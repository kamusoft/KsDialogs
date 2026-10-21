# レビュー結果: fix-ios-swiftui-edge-content-placement (002 回目)

**日付**: 2026-10-21
**判定**: CHANGES_REQUESTED

## サマリー

前回の指摘 4 件はすべて解消した。縮みやすさだけが変わった中身の判定し直しは、オーナー決定 (deviation.md の 2 行目) のとおり実装されている。中身に重ねた UIView で置き直しを捉え、包みが効いていない軸だけを測り直す。回帰テストもあり、この仕組みを止めるとそのテストだけが落ちることを確かめた。全件テストも通る。

差し戻しの理由は 1 つで、証跡に Simulator の UDID が生のまま残っている。この値は commit 時の検査 (`.githooks/pre-commit` → `scripts/git-gate-lint.py --staged`) で止まるので、このままでは commit できない。直し方はプレースホルダへの置き換え 1 か所で済む。もう 1 件、優先度の低い判断事項がある。前回の指摘の逆向き (縮まない → 縮められる) で、器が窓の反対側のシステムの領域まで届く中身に限り、中身が内側の辺の安全領域に入り込んだまま残る。

## 照合した規約

- `kasane/handbook/cross/comment-policy.md` (always): 追加・変更したコメント全件 (`DialogSwiftUIContentLayoutReader.swift` と `DialogSwiftUIContentView.swift` の再検査を含む)
- `kasane/handbook/cross/test-execution.md`: テストを実行し、件数を確かめるため。全件実行表とは行ごとに照合した (lessons L-002)
- `kasane/handbook/cross/ci-flaky-test-policy.md`: `ios/Tests/**` に状態遷移を観測する回帰テストが増えたため。観測の待ち方と、正規の印以外の無効化手段が無いかを見た
- `kasane/decisions/ios/0002-swiftui-content-receives-standard-safe-area.md` (accepted)・`kasane/decisions/ios/0003-swiftui-content-overflowing-safe-area-aligns-to-inner-edge.md` (proposed のため、適合の根拠にだけ使う)
- `kasane/lessons/code-review.md` の L-001 と L-002
- 証跡の識別子: ksn-core `references/evidence.md` (`scripts/identity-lint.py` で確認)

ロードしたスキル: swift-ui-impl-skill

## 前回 (review-001) の指摘の解消

| 前回の指摘 | 状態 | 確認したこと |
|---|---|---|
| 🟡 縮みやすさだけが変わったとき、はみ出しの判定がやり直されない | 解消 (オーナー決定 B。deviation.md 2 行目) | `ios/Sources/KsDialogs/SwiftUI/DialogSwiftUIContentLayoutReader.swift` が中身の置き直しを捉える。受け手は `ios/Sources/KsDialogs/SwiftUI/DialogSwiftUIContentView.swift:278` の `hostedContentDidResize` で、`:226` の `recheckUnalignedAxes` が包みの効いていない軸だけを測り直す。回帰テスト `settlesAfterOnlyShrinkableRangeChanges` (`ios/Tests/KsDialogsTests/DialogSwiftUIInnerEdgeAlignmentTests.swift:316-349`) で固定している。検出力 (L-001) も確かめた。作業ツリーの複製で `hostedContentDidResize` を何もしない形にし、同じスイートを回すと、この回帰テストの 2 ケースだけが `settled` と描かれた位置の期待で落ち、ほかの 10 テストは通った |
| 🟡 証跡の件数表に、ios/ を取り込むルートの行が無い | 解消 | `evidence/implementation-verification.md:7-16` に全件実行表の 7 行と網羅検査がそろう |
| 🔵 変わっていない包みの指定へ書き込んでいる | 解消 | `DialogSwiftUIContentView.swift:257-273` は値が違うときだけ書き、反映する |
| 🔵 [PB-IS-22] の右から左を、表示した後に切り替えている | 解消 | `DialogSwiftUIInnerEdgeAlignmentTests.swift:69-118` が「最初から右から左」と「表示後に切り替え」の両方を回す。`ios/Tests/KsDialogsTests/Support/DialogLayoutMeasurement.swift` の `layoutDirection` 引数は、器を載せる前に窓へ設定する |

## 確認した観点

- 仕様充足
  - 全 Scenario の充足: `verify-002.md` の対応表
  - tasks.md: 虚偽のチェックは無い
  - 足場: proposal / specs / exploration / decisions の書き換えは無い (`git diff HEAD` で確認)
  - deviation.md の 2 行目: 合意済みの差分として扱った。実装はその記述 (理想の大きさが変わらないまま縮められる範囲だけが変わったときも判定し直し、内側の辺にそろえる・Scenario ID は足さず回帰テストで固定する) と一致する
  - `[付随修正]` の行は無い
- テスト
  - 自分で再実行した結果は下の表のとおり
  - Scenario 対応のテストはすべてある (`scripts/scenario-id-coverage.py` で `PB-IS 35/35`・`未網羅なし`)
  - 言い訳コメントで実質スキップしたテストや、`.disabled` 等の無効化は無い
- テストの検出力 (L-001): 上の表のとおり、回帰テストは置き直しを捉える仕組みの有無で結果が分かれる
- 設計
  - ios/ADR-0002 に反しない (器の大きさと位置は変えない)
  - 公開 API は増えていない (`DialogSwiftUIContentLayoutReader` は internal)
  - 置き直しの知らせを使うのは、包みが効いていない軸があるときだけ (`DialogSwiftUIContentView.swift:278-285`)。包みを反映している最中の知らせは受け取らない (`isApplyingOverflowAlignment`)。測り直しでは包みを付けるだけで外さないため、付けたり外したりを繰り返さない
- 汎用品質
  - 並行性: すべて MainActor で動く
  - 参照: 目印の UIView の `onResize` は、`DialogSwiftUIHost.swift:22` で `contentView` を weak に取る
  - 性能: 問い合わせが増えるのは、包みが効いていない軸がある中身が置き直されたときだけ
  - 当たり判定とアクセシビリティ: 目印は `isUserInteractionEnabled = false` で、ラベルを持たない
- コメント
  - 作業文書、タスク番号、履歴を語る記述への依存は無い
  - `python3 scripts/comment-policy-lint.py` は禁止 0 件。要確認 (advisory) に、この変更が触ったファイルは出ない
- SwiftUI (swift-ui-impl-skill)
  - `@Observable` と `@MainActor` を使っている
  - `AnyView` は使っていない
  - 包みは常に同じ `frame` を置くので、構造的同一性が崩れない
  - `ReaderView` は入れ子の型だが、既存コードに同じ形の前例がある (`DialogAxisLayout.Position`・`DialogNotifierBindings.Binding`)
- 証跡: `scripts/identity-lint.py` と `scripts/local-path-lint.py` を change 配下と `ios/` に掛けた。下の指摘 1 のとおり

### 全件実行表との照合 (L-002)

| ビルドルート | 実行 | 件数 |
|---|---|---|
| ios/ | レビューで再実行した。端末は作業専用の iPhone 17 Pro / iOS 26.0 の Simulator で、終了後に削除した | `Test run with 420 tests in 69 suites passed` / XCTest `Executed 0 tests` |
| android/ | 未実行 (この変更は `ios/` だけを変え、android/ は `ios/` を取り込まない) | — |
| android/ (instrumented) | 未実行 (同上) | — |
| kmp/ | 実装報告の値を採った (`iosSimulatorArm64Test` が `localSwiftPackage("../ios")` で取り込む) | iosSimulatorArm64Test 86 / testAndroidHostTest 83、失敗 0 |
| maui/ | レビューで再実行した (`maui/macios/native` 経由で、`ios/` を取り込む xcframework のビルドを巻き込む) | 合格 206 / 失敗 0 / スキップ 0 |
| maui/android/native/ | 未実行 (`ios/` を取り込まない) | — |
| maui/macios/native/ | レビューで再実行した (`XCLocalSwiftPackageReference "../../../ios"`) | `Test run with 17 tests in 7 suites passed` / XCTest `Executed 0 tests` |
| Scenario ID の網羅 | レビューで再実行した | `結果: 未網羅なし` (`PB-IS 35/35`) |

## 指摘事項

### 🟡 Minor (優先度高) 証跡に Simulator の UDID が生のまま残っていて、commit 時の検査で止まる

**該当箇所**: `evidence/implementation-verification.md:5`

**問題点**: 「修正サイクル 1 の端末は `ksn-impl-edge-fix1` (UDID `…`、作業後に `simctl delete` で削除)」の括弧内に、端末の UDID がそのまま書かれている。`python3 scripts/identity-lint.py --paths kasane/changes/fix-ios-swiftui-edge-content-placement` は、この行を `uuid` として検出する。`kasane/config.yaml` の `lint.identity.scope` は `kasane` を含む。このため、`.githooks/pre-commit` (`scripts/git-gate-lint.py --staged`) と、Bash の commit を見る hook は、このファイルを含む commit を止める。端末は削除済みで実害は小さいが、このままでは change を commit できない。同じ表のコマンド列はすでに `id=<uuid>` と書いており、そこと表記も食い違う。

**推奨修正**: UDID を `<uuid>` に置き換える (`python3 scripts/log-sanitize.py <file>` でもよい)。修正後の形は「修正サイクル 1 の端末は `ksn-impl-edge-fix1` (UDID `<uuid>`、作業後に `simctl delete` で削除)」。直したら `python3 scripts/identity-lint.py --paths kasane/changes/fix-ios-swiftui-edge-content-placement` で検出 0 件を確かめる。

### 🟡 Minor (優先度低・判断を要する) 縮まない → 縮められるへ縮みやすさだけが変わると、器が反対側のシステムの領域まで届く中身は、内側の辺の安全領域に入り込んだまま残る

**該当箇所**: `ios/Sources/KsDialogs/SwiftUI/DialogSwiftUIContentView.swift:191-193`・`:226-241` (包みは測り直しで付けるだけで、外すのは判定の入力が変わったときだけ)

**問題点**: 前回の指摘 1 の逆向きにあたる。理想の大きさを変えずに、縮まない中身が縮められる中身へ変わったとき、`OverflowInputs` は変わらない。そのため包みは効いたまま残る。包みは内側の辺の安全領域を無視する (`ios/Sources/KsDialogs/SwiftUI/DialogSwiftUIHost.swift:94-97`)。器の内側の辺がシステムの領域に重ならない普通の大きさでは、この無視は 0 の無視になる。結果として包みを外したときと同じ位置になり、[PB-IS-25] の後半 (y 692 h 126) もこの経路で通っている。

一方、器が有効領域で頭打ちになり、反対側のシステムの領域まで届くと結果が変わる。縮められるようになった中身が、内側の辺の安全領域に入り込んで置かれる。

レビューで作業ツリーの複製に一時テストを足して確かめた。条件は、縦長の窓・窓全体基準・理想の高さ 1000 で、最小の高さ 1000 (縮まない) で表示した後、表示中に最小の高さを外す (理想の大きさは変えない)。比べる相手は、同じ縮められる中身を最初から出した場合である。

| 付いた側 | 外す前の描かれた位置 | 外した後の描かれた位置 | 最初から縮められる中身で出した位置 |
|---|---|---|---|
| 下端 (器 y 0 h 852) | y 0 h 1000 | **y 0 h 818** (上のステータスバーの領域 0〜59 に入る) | y 59 h 759 |
| 上端 (器 y 0 h 852) | y -148 h 1000 | **y 59 h 793** (下のホームインジケータの領域 818〜852 に入る) | y 59 h 759 |

これは Requirement「窓全体基準で片側の端に付けた…器いっぱいに置かれる」の箇条「収まる中身は…安全領域の内側に置かれる」に反する。ただし、spec の切り替わりの箇条が挙げるきっかけ (理想の大きさ・安全領域の変化) にも、deviation.md の 2 行目 (収まらなくなる向きの判定し直し) にも当たらない。成り立つ条件も、器が窓の反対側のシステムの領域まで届く大きさの中身で、表示中に縮みやすさだけを広げたときに限られる。

**推奨修正** (どちらにするかはオーナーの判断):
- A. 既知の限界として受け入れる。deviation.md に「縮みやすさだけが広がった向き (縮まない → 縮められる) では判定し直さない。器が反対側のシステムの領域まで届く中身に限り、内側の辺の安全領域に入り込んだまま残る」旨を記録し、蒸留で `kasane/concepts/ios/api/layout-surface.md` の保証に条件として書く
- B. 実装で直す。包みが効いている軸でも置き直しを受け取ったら、入力が変わったときと同じく「包みを外して測り、付け直す」を同じパスで行う (`updateOverflowAlignment` の入力変化の経路を使う)。包みの付け外しで起きる置き直しは `isApplyingOverflowAlignment` で受け取らないため、行ったり来たりは起きにくい見込みだが、[PB-IS-25]・[PB-IS-33] と同じ 5 ms ごとの測りで跳ねないことを確かめる必要がある。回帰テストは上の表の 2 ケースで足りる。このテストは包みを外す仕組みが無ければ「外した後」の列の位置になって落ちるので、仕組みの有無で結果が分かれる

## アクションプラン

1. `evidence/implementation-verification.md:5` の UDID をプレースホルダにし、identity-lint で検出 0 件を確かめる (commit の前提)
2. 指摘 2 の扱い (A: 既知の限界として記録 / B: 実装で直す) をオーナーが決める。A なら deviation.md に 1 行足し、蒸留送りに条件を足せば済む
