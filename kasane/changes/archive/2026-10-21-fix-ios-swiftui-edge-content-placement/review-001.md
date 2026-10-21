# レビュー結果: fix-ios-swiftui-edge-content-placement (001 回目)

**日付**: 2026-10-21
**判定**: NEEDS_DISCUSSION

## サマリー

実装は tasks.md の設計 (包みを常に挟み、はみ出す軸でだけ効かせる・物理の向きで内側の辺を決める・包みを外した状態で判定して保つ・前に知らされた上限より広がったときだけ幅を求め直す) どおりで、デルタスペックの全 Scenario がテストで固定され、全件が通る。テストは描かれた位置を UIView の目印で測っており、`onGeometryChange` だけに頼らない。幅の上限の判定を「毎回求め直す」形に壊すと [PB-IS-29]・[PB-IS-34] が落ちることも確かめた。

判断を要する点が 1 つある。理想の大きさを変えずに縮みやすさだけが表示中に変わると (縮められる → 縮まない)、はみ出しの判定がやり直されず、今回直した「重なりの半分だけずれる」並び方が残る。Requirement の切り替わりの箇条はきっかけを「理想の大きさ・安全領域の変化」に絞っており、実装はそれを満たすため、spec の範囲の問題として判断を求める。

## 照合した規約

- `kasane/handbook/cross/comment-policy.md` (always) — 追加・変更したコメント全件
- `kasane/handbook/cross/test-execution.md` — テストの実行と件数の確認、全件実行表との行ごとの照合 (lessons L-002)
- `kasane/handbook/cross/ci-flaky-test-policy.md` — `ios/Tests/**` に状態遷移を観測するテストを足しているため (観測の待ち方・正規の印以外の無効化手段の有無)
- `kasane/decisions/ios/0002-swiftui-content-receives-standard-safe-area.md` (accepted)・`kasane/decisions/ios/0003-swiftui-content-overflowing-safe-area-aligns-to-inner-edge.md` (proposed。決定ではないので適合の根拠にだけ使う)
- `kasane/lessons/code-review.md` の L-001・L-002

ロードしたスキル: swift-ui-impl-skill

## 確認した観点

- 仕様充足: 全 Scenario の充足 (`verify-001.md` の対応表)・tasks.md の虚偽チェックなし・足場 (proposal / specs / exploration / decisions) の書き換えなし (`git diff HEAD` で確認)・deviation.md 外の逸脱なし・`[付随修正]` の行なし
- テスト: 自分で再実行した結果は下表。Scenario 対応のテストはすべて存在し (`scripts/scenario-id-coverage.py` で `未網羅なし`)、言い訳コメントでの実質スキップ・`.disabled` 等の無効化は無い
- テストの検出力 (L-001): 実装報告に、本体の 2 つの仕組みを止めると該当 Scenario が落ちる記録がある。加えて、上限の通知を「知らされるたびに求め直す」形 (探索で却下した形に近い) に壊した複製で `ContentWidthLimitFollowTests` を回し、[PB-IS-29]・[PB-IS-34] が落ちることを確かめた。[PB-IS-34] は求め直しすぎの見張りとして効いている
- 設計: ios/ADR-0002 (器の大きさを変えない・端の軸の重なりを届ける) に反しない。公開 API の変更なし (`DialogAnchoredEdge`・`DialogContentWidthLimitReceiver`・`DialogSwiftUIOverflowAlignment` はすべて internal)。オーバーエンジニアリングなし
- コメント: 作業文書・タスク番号・履歴記述への依存なし。`python3 scripts/comment-policy-lint.py` は禁止 0 件、要確認 (advisory) にも変更ファイルは出ない
- SwiftUI (swift-ui-impl-skill): `@Observable` + `@MainActor` の共有状態、`ObservableObject` 不使用、`AnyView` 不使用、包みの差し込み・取り外しで構造的同一性を崩さない (常に同じ `frame` を置く)
- 汎用品質: 境界 (器の大きさ 0 のときは判定しない)・並行 (すべて MainActor)・リソース (KVO の監視は既存のまま)・性能 (判定は入力が変わったときだけで、レイアウトのたびの問い合わせは増えない)

### 全件実行表との照合 (L-002)

| ビルドルート | 実行 | 件数 |
|---|---|---|
| ios/ | レビューで再実行 (作業専用の iPhone 17 Pro / iOS 26.0 の Simulator。終了後に削除) | `Test run with 419 tests in 69 suites passed` / XCTest `Executed 0 tests` |
| android/ | 未実行 (この変更は `ios/` だけを変え、android/ は `ios/` を取り込まない) | — |
| android/ (instrumented) | 未実行 (同上) | — |
| kmp/ | 実装報告の値 (`iosSimulatorArm64Test` が `localSwiftPackage("../ios")` で取り込む) | iosSimulatorArm64Test 86 / testAndroidHostTest 83、失敗 0 |
| maui/ | レビューで実行 (`maui/macios/native` 経由で `ios/` を取り込む xcframework のビルドを巻き込む) | 合格 206 / 失敗 0 / スキップ 0 |
| maui/android/native/ | 未実行 (`ios/` を取り込まない) | — |
| maui/macios/native/ | レビューで実行 (`XCLocalSwiftPackageReference "../../../ios"`) | `Test run with 17 tests in 7 suites passed` / XCTest `Executed 0 tests` |
| Scenario ID の網羅 | レビューで実行 | `結果: 未網羅なし` |

## 指摘事項

### 🟡 Minor (判断を要する) 縮みやすさだけが変わったとき、はみ出しの判定がやり直されない

**該当箇所**: `ios/Sources/KsDialogs/SwiftUI/DialogSwiftUIContentView.swift:170-193`

**問題点**: はみ出しの判定は `OverflowInputs` (器の大きさ・`lastIdealSize`・ホストの safe area・付いた側) が変わったときだけやり直す。`lastIdealSize` はホストの `preferredContentSize` の変化 (= 理想の大きさの変化) でしか更新されないため、理想の大きさを変えずに最小の大きさ・縮みやすさだけが表示中に変わった中身では、判定が古いまま残る。

レビューで作業ツリーの複製に一時テストを足して確かめた (縦長の窓・窓全体基準・理想 200 × 100 の縮められる中身を表示し、表示中に最小の高さを 100 にする):

| 付いた側 | 変える前の描かれた位置 | 変えた後の描かれた位置 | Requirement が求める位置 |
|---|---|---|---|
| 下端 (器 y 752 h 100) | y 752 h 66 | y 735 h 100 | y 752 h 100 |
| 上端 (器 y 0 h 100) | y 59 h 41 | y 29.7 h 100 | y 0 h 100 |

この変更で直した「重なりの半分だけ内側へずれる」並び方そのものが残る。逆向き (縮まない → 縮められる、[PB-IS-25] の後半) は、包みが効いたままでも描かれた位置・一番外側と内側の子から読める位置がすべて y 692 h 126 (上端なら y 59 h 101) で一致し、問題は無かった。

Requirement「…内側の辺にそろって器いっぱいに置かれる」の本文 (縮まない中身は器いっぱいに置かれる) には反するが、切り替わりの箇条はきっかけを「中身の理想の大きさ、または中身が受け取る安全領域」と挙げ、tasks.md 1.3 も判定を保つ入力をこの 4 つに定めている。実装は spec と tasks のとおりで、穴は spec が挙げたきっかけの外にある。

**推奨修正** (どちらにするかはオーナーの判断):
- A. 既知の限界として受け入れる。deviation.md に「切り替わりのきっかけは理想の大きさと安全領域の変化に限り、縮みやすさだけの変化では判定し直さない」旨を記録し、蒸留で `kasane/concepts/ios/api/layout-surface.md` の保証にその条件を書く。理想の大きさが同時に変わる通常の変化 (文言・Dynamic Type・部品の出し入れ) は覆われている
- B. 実装で直す。包みが効いていない間に限り、ホスト側のレイアウトの更新 (SwiftUI 内部の変化) を契機に、安全領域の内側の大きさを提案した答えを測り直して判定する (proposal の Impact が想定した「はみ出していない間は大きさの問い合わせが 1 回増える」形に近い)。spec に Scenario を足すかは propose 側の判断になる

### 🟡 Minor 証跡の件数表に、`ios/` を取り込むルートの行が無い

**該当箇所**: `kasane/changes/fix-ios-swiftui-edge-content-placement/evidence/implementation-verification.md:5-11`

**問題点**: 件数表は ios/・kmp/・網羅検査の 3 行で、`ios/` を Swift package として取り込む `maui/macios/native/` と、その xcframework のビルドを巻き込む `maui/` (`dotnet test`) の行が無い。handbook の全件実行表と行ごとに照合すると、この 2 行は「未実行」にあたる。レビューで両方を実行し成功を確かめたので、退行の懸念は無い。

**推奨修正**: 件数表に上の「全件実行表との照合」の maui/・maui/macios/native/ の行を足し、android/ 系 3 行は「`ios/` を取り込まないため対象外」と書き添える。

### 🔵 Suggestion 判定の入力が変わるたびに、変わっていない包みの指定へ書き込んでいる

**該当箇所**: `ios/Sources/KsDialogs/SwiftUI/DialogSwiftUIContentView.swift:195-198`

**問題点**: 付いた側が無い中身 (中央・全面・窓全体以外) でも、入力が変わるたびに `overflowAlignment.horizontal = nil` / `vertical = nil` を書く。`@Observable` は同じ値の代入でも変更を通知するため、ホストのルートの `body` が評価し直される。入力が変わる頻度は低く実害は小さい。

**推奨修正**: 今の値と違うときだけ書く。
```swift
// before
overflowAlignment.horizontal = nil
overflowAlignment.vertical = nil
// after
if overflowAlignment.horizontal != nil { overflowAlignment.horizontal = nil }
if overflowAlignment.vertical != nil { overflowAlignment.vertical = nil }
```

### 🔵 Suggestion [PB-IS-22] の右から左は、表示した後に書字方向を切り替えている

**該当箇所**: `ios/Tests/KsDialogsTests/DialogSwiftUIInnerEdgeAlignmentTests.swift:93-96`

**問題点**: GIVEN は「窓の書字方向を…2 通りで用意する」で、表示の時点から右から左の窓を想定している。テストは左から右で表示した後に `traitOverrides.layoutDirection` を切り替えており、表示中の切り替えは確かめているが、最初から右から左で出したときの経路 (最初の判定) は通っていない。ルートは `@Environment(\.layoutDirection)` を読むだけなので実害は考えにくい。

**推奨修正**: 窓を作った直後 (表示の前) に書字方向を設定する引数を `DialogSwiftUIMeasurement.layoutInWindow` に足すか、今の形のまま「表示中の切り替え」を確かめていることをテストの説明に書く。

## アクションプラン

1. 指摘 1 の扱い (A: 既知の限界として記録 / B: 実装で直す) をオーナーが決める。A なら deviation.md への記録と蒸留送りの行で完了。B なら実装と、必要なら spec の扱いを決める
2. `evidence/implementation-verification.md` の件数表に maui/・maui/macios/native/ の行を足す (値は上表)
3. (任意) Suggestion 2 件
