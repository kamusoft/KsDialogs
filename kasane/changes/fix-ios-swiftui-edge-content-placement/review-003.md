# レビュー結果: fix-ios-swiftui-edge-content-placement (003 回目)

**日付**: 2026-10-21
**判定**: APPROVED

## サマリー

前回 (review-002) の指摘 2 件は解消した。証跡の UDID はプレースホルダになり、identity-lint の検出は 0 件になった。縮みやすさだけが戻って収まる状態になる向きは、deviation.md の 3 行目のとおり実装で直した (前回の推奨修正 B)。中身が置き直された知らせを受けたら、そろえが効いている軸も含めて包みを外して測り直す。回帰テストも足した。

全件テストを作業専用の Simulator で回し直し、通ることを確かめた。新しい回帰テストに検出力があることも確かめた。そろえが効いている軸を測り直さない形に複製で戻すと、このテストの 2 ケースだけが落ちる。

Critical / Major はない。低優先の Suggestion が 1 件ある。そろえが効いたまま中身が置き直されると、中身から読む geometry に、描かれない途中の位置が 1 回届く。

## 照合した規約

- `kasane/handbook/cross/comment-policy.md` (always): 修正サイクル 2 で変わったコメント (`ios/Sources/KsDialogs/SwiftUI/DialogSwiftUIContentView.swift:184-193`・`:250-253`) と、新規ファイル全体を照合した
- `kasane/handbook/cross/test-execution.md`: テストを実行し、件数を確かめるため。全件実行表とは行ごとに照合した (lessons L-002)
- `kasane/handbook/cross/ci-flaky-test-policy.md`: `ios/Tests/**` に状態遷移を観測する回帰テストが 1 本増えたため。待ち方 (`waitUntilDrawn` で条件まで待つ) を見た。正規の印以外の無効化手段がないことも見た
- `kasane/decisions/ios/0002-swiftui-content-receives-standard-safe-area.md` (accepted)
- `kasane/decisions/ios/0003-swiftui-content-overflowing-safe-area-aligns-to-inner-edge.md` (proposed のため、適合の根拠にだけ使う)
- `kasane/lessons/code-review.md` の L-001 と L-002
- 証跡の識別子とローカルパス: ksn-core の `references/evidence.md` と `references/paths.md`。`scripts/identity-lint.py` と `scripts/local-path-lint.py` で確かめた

ロードしたスキル: swift-ui-impl-skill

## 前回 (review-002) の指摘の解消

| 前回の指摘 | 状態 | 確認したこと |
|---|---|---|
| 🟡 証跡に Simulator の UDID が生のまま残っている | 解消 | `evidence/implementation-verification.md:5` は `<uuid>` に置き換わった。`python3 scripts/identity-lint.py --paths kasane/changes/fix-ios-swiftui-edge-content-placement` は検出 0 件 (exit 0) |
| 🟡 縮まない → 縮められるへ縮みやすさだけが変わると、内側の辺の安全領域に入り込んだまま残る | 解消 (推奨修正 B。deviation.md 3 行目) | `ios/Sources/KsDialogs/SwiftUI/DialogSwiftUIContentView.swift:254-259` の `hostedContentDidResize` が、包みが効いているかによらず測り直しを予約する。`:194-216` の `updateOverflowAlignment` は、測り直しのとき包みを外して測り、付け直す。回帰テストは `ios/Tests/KsDialogsTests/DialogSwiftUIInnerEdgeAlignmentTests.swift:351-404` の `settlesBackAfterShrinkableRangeReturns`。前回報告した 2 ケース (下端 y 0 h 818・上端 y 59 h 793) が、y 59 h 759 に落ち着くことを確かめる |

### 検出力の確認 (L-001)

`ios/` を作業ツリーの外に複製し、`hostedContentDidResize` に「そろえが効いていない片側の端の軸があるときだけ受け取る」条件を足して、前の形に戻した。そのうえで `DialogSwiftUIInnerEdgeAlignmentTests` を回した。結果は次のとおり。

- `settlesBackAfterShrinkableRangeReturns` の 2 ケース (下端・上端) だけが落ちた。落ちた期待は、`settled`、描かれた位置、一番外側と内側の子から読む位置、受け取る安全領域
- 同じスイートのほかの 11 テストは通った

このテストは、測り直しの仕組みがあるかないかで結果が分かれる。作業ツリーのファイルは変えていない。

## 確認した観点

- 仕様充足
  - 全 Scenario の充足は `verify-003.md` の対応表のとおり
  - tasks.md: 変わったのはチェックだけで、本文は変わっていない (HEAD の本文の `[ ]` を `[x]` にしたものと一致する)。虚偽のチェックはない
  - 足場: proposal / specs / exploration / `kasane/decisions/` の書き換えはない (`git diff HEAD` で確認)
  - deviation.md: 2・3 行目は合意済みの差分として扱った。実装はその記述と一致する。2 行目は収まらなくなる向き、3 行目は収まる状態へ戻る向きで、どちらも Scenario ID は足さず、回帰テストで固定している
  - `[付随修正]` の行はない
- テスト
  - 自分で再実行した結果は、下の表のとおり
  - Scenario 対応のテストはすべてある (`scripts/scenario-id-coverage.py` で `PB-IS 35/35`・`結果: 未網羅なし`)
  - 言い訳コメントで実質スキップしたテストや、`.disabled` などの無効化はない
  - 新しい回帰テストは、最初の状態・そろえた状態・戻した状態を順に条件待ちで確かめる。最後は 5 ms ごとのパスで 1 秒間、動かないことも見る。境界は「器が有効領域で頭打ちになり、内側の辺がシステムの領域に重なる」場合で、前回の指摘が起きた条件そのものである
- 設計
  - ios/ADR-0002 に反しない。器の大きさと位置は変えない。新しいテストも、器が 96.5, 0, 200 × 852 のまま動かないことを見ている
  - 公開 API は増えていない
  - 判定の入力 (`OverflowInputs`) が変わらない測り直しは、置き直しの知らせを受けたときだけ起きる。包み自身の変化による知らせは、`isApplyingOverflowAlignment` で受け取らない
  - 行ったり来たりしない理由: 測り直しの結果は、包みを外した状態の測定だけで決まり、包みの有無に左右されない。同じ入力なら同じ結果になる。実測でも、描かれた位置は各パスで 1 つの値に落ち着いた (下の指摘 1 の表)
- 汎用品質
  - 並行性: すべて MainActor で動く
  - 参照: `onContentResize` は `contentView` を weak に取る (`ios/Sources/KsDialogs/SwiftUI/DialogSwiftUIHost.swift:22`)
  - 性能: そろえが効いている中身が置き直されるたびに、ホストのレイアウトを同じパスで 2 回 (外す・付け直す) 走らせるようになった。対象は、窓全体基準で片側の端に付けた SwiftUI の中身に限られる。proposal の Impact (はみ出していない間の問い合わせ 1 回の増加) の範囲を少し超えるが、中身の外形が変わったときだけなので実害は小さいと見た
  - 早期 return: `updateOverflowAlignment` の冒頭で測り直しの予約を下ろしてから guard する (`:195-197`)。大きさ 0 のパスで予約が消えても、次に大きさが付いたパスでは入力が前と違うため判定し直す。取りこぼしはない
- コメント
  - 修正サイクル 2 で変わったコメントは、外部の作業文書・タスク番号・履歴に依存せず、単独で読める
  - `python3 scripts/comment-policy-lint.py` は禁止 0 件 (検査対象 1215 ファイル)
- SwiftUI (swift-ui-impl-skill)
  - `@Observable` と `@MainActor` を使っている。`AnyView` は使っていない
  - 包みは常に同じ `frame` を置くので、構造的同一性が崩れない
  - 修正サイクル 2 は SwiftUI 側のコードを変えていない
- 証跡: `scripts/identity-lint.py` (change 配下) と `scripts/local-path-lint.py` (change 配下と `ios/`) は検出 0 件。証跡の記述 (`evidence/implementation-verification.md:36-46`) は、上の検出力の確認と同じ結果を報告している

### 全件実行表との照合 (L-002)

| ビルドルート | 実行 | 件数 |
|---|---|---|
| ios/ | レビューで再実行した。端末は作業専用の iPhone 17 Pro / iOS 26.0 の Simulator で、終了後に削除した | `Test run with 421 tests in 69 suites passed` / XCTest `Executed 0 tests` |
| android/ | 未実行 (この変更は `ios/` だけを変える。android/ は `ios/` を取り込まない) | — |
| android/ (instrumented) | 未実行 (同上) | — |
| kmp/ | 実装報告の値を採った (`iosSimulatorArm64Test` が `localSwiftPackage("../ios")` で取り込む) | iosSimulatorArm64Test 86 / testAndroidHostTest 83、失敗 0 |
| maui/ | 実装報告の値を採った (修正サイクル 2 の差分は `DialogSwiftUIContentView` の private な処理だけで、MAUI の iOS 互換面から見える口は変わらない) | 合格 206 / 失敗 0 / スキップ 0 |
| maui/android/native/ | 未実行 (`ios/` を取り込まない) | — |
| maui/macios/native/ | レビューで再実行した (`XCLocalSwiftPackageReference "../../../ios"` で、修正サイクル 2 の `ios/` をビルドに巻き込む) | `Test run with 17 tests in 7 suites passed` / XCTest `Executed 0 tests` |
| Scenario ID の網羅 | レビューで再実行した | `結果: 未網羅なし` (`PB-IS 35/35`) |

## 指摘事項

### 🔵 Suggestion そろえが効いたまま中身が置き直されると、中身から読む geometry に、描かれない途中の位置が 1 回届く

**該当箇所**: `ios/Sources/KsDialogs/SwiftUI/DialogSwiftUIContentView.swift:205-215` (測り直しで包みを外して反映し、付け直す)・`:254-259` (修正サイクル 2 で、そろえが効いている軸でも測り直すようにした)

**問題点**: 測り直しは、包みを外した状態をホストへ反映してから測り、付け直す。描画には出ないが、中身の `onGeometryChange` には、包みを外した途中の位置が 1 回届く。修正サイクル 1 まで、これは判定の入力 (器の大きさ・理想の大きさ・安全領域・付いた側) が変わったときだけ起きていた。修正サイクル 2 からは、そろえが効いたまま中身が置き直されるたびに起きる。

複製に一時テストを足して確かめた。条件は、縦長の窓・窓全体基準・理想 200 × 100・最小の高さ 100 (そろえが効く) で表示し、表示中に最小の高さを 90 にするもの。収まらないままなので、そろえは効いたままになる。5 ms ごとに 200 パス測った結果は次のとおり。

| 付いた側 | 描かれた位置 (全パスの異なる値) | 一番外側の `onGeometryChange` が受け取った順 |
|---|---|---|
| 下端 | y 752 h 90 の 1 つだけ | y 752 → **y 740** → y 752 |
| 上端 | y 10 h 90 の 1 つだけ | y 10 → **y 34.5** → y 10 |

太字の値は、包みを外したときの位置 (安全領域の内側の中央) で、描かれることはない。最後に届く値は描かれた位置と一致する。このため、Scenario [PB-IS-24] (最後に記録された値で判定する) と、Requirement の「読める位置は描かれた位置と一致する」の落ち着いた状態の約束は満たしている。

扱いを Suggestion にしたのは、次の理由による。

- 途中の値が届くのは、tasks.md 1.3 が定めた形 (包みが効いていない状態で測る) に元からある性質である。修正サイクル 2 は、それが起きる機会を増やしただけである
- 描かれた位置は動かない
- 実害が出るのは、届いた geometry で自分の大きさを変える中身に限られる

**推奨修正**: 実装の変更は求めない。蒸留で `kasane/concepts/ios/api/layout-surface.md` の保証を書くとき、「中身から読める位置は、落ち着いた後に描かれた位置と一致する。そろえを判定し直す間に、描かれない途中の値が 1 回届くことがある」と、落ち着いた後の約束として書く。必要なら deviation.md に蒸留送りの行を足す。

## 所見 (判定に影響しない)

- deviation.md の 3 行目は、オーナーに改めて諮らず、記録の主体が 2 行目の決定の範囲として扱ったと明記している。中身は、Requirement 本文の「収まる中身は安全領域の内側に置かれる」を満たす向きの変更で、spec から離れる変更ではない。パッケージの指示に従い、合意済みの差分として扱った。オーナーレビューで、この行を一度確かめてもらうのがよい
- 修正サイクル 2 の実行中に、change 配下へ `second-opinion-code-003.md` が増えた。独立性を保つため読んでいない

## アクションプラン

1. (任意) 指摘 1 の内容を、蒸留時の `kasane/concepts/ios/api/layout-surface.md` の書き直しに含める。必要なら deviation.md に蒸留送りの行を足す
2. オーナーレビューで deviation.md の 3 行目を確かめる
