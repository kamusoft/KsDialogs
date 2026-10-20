# レビュー結果: deliver-swiftui-content-safe-area-ios (001 回目)

**日付**: 2026-10-20
**判定**: CHANGES_REQUESTED

## サマリー

安全領域の受け渡し (端の軸だけに渡す、キーボードを含めない、ルートで渡さない辺を無視させる) と、要求サイズから安全領域を引き戻す計算は、19 の Scenario を満たしている。ios/ の全件 391 件と MAUI iOS 互換面 17 件はどちらも成功した。ただ、要求サイズの求め方を変えたことで、表示中に中身の幅が広がっても器が追従しなくなった。この退行は既定の基準領域 (可視領域) でも起き、変更前のコードとの比較で再現した。Requirement「安全領域を受け渡しても、器の大きさと位置は変わらない」と「窓全体以外の基準領域では…受け取らない」(器の矩形は今と同じ) に反するため、変更を求める。

## 照合した規約

- `kasane/handbook/cross/comment-policy.md` (always)
- `kasane/handbook/cross/test-execution.md` (テストの実行・テスト結果の報告・変更の完了判定)
- `kasane/handbook/cross/ci-flaky-test-policy.md` (Simulator 上で状態遷移を観測するテストを書くとき。「観測は終端状態の合意を待つ」「「起きない」ことの確認には履歴を使う」の節)
- `kasane/lessons/code-review.md` の重点観点 L-001 (推奨する受け入れ条件が、機構の有無で結果の分かれるものか)・L-002 (全件実行の主張を全件実行表と行ごとに突き合わせる)
- ADR: ios/ADR-0002 (proposed — 方向の確認に使い、指摘の根拠にはしていない)・core/ADR-0011・core/ADR-0009

ロードしたスキル: ksn-review / ksn-verify / swift-ui-impl-skill

## 確認した観点

- 仕様充足: Requirement と 19 の Scenario の対応 (対応表は `verify-001.md`)。tasks.md のチェックと実装の対応。足場 (proposal / specs) が 62b4981 から書き換えられていないこと (diff に出るのは tasks.md のチェックだけ)。deviation.md の PB-IS-15 の乖離は合意済みとして扱った
- テスト: 自分で全件を回した (下の実行表)。Scenario ごとのテストの存在と、期待値が THEN と合うか。「受け取らない」を履歴で見ているか (`expectNoInsetsEver` は届いた全履歴を見ている — 適合)
- 設計品質: accepted ADR との衝突 (なし)。過剰設計 (なし — 新しい面は `DialogContentSafeAreaRelay` と `DialogSwiftUISafeAreaEdges` の 2 つで、どちらも用途がはっきりしている)。コメントが単独で読めるか (読める。作業文書への参照なし)。入力検証・機密情報 (該当なし)
- 汎用の品質: 要求サイズの計算の境界 (幅 0 の初回・幅が広がる / 狭まる変化・高さの変化)。KVO の監視とホストの解放 (既存の「全閉鎖経路でホストが解放される」テストが通る)。メインスレッドの前提 (`MainActor.assumeIsolated` は `preferredContentSize` の更新がメインで起きる前提で妥当)
- SwiftUI (swift-ui-impl-skill): `@Observable` + `@MainActor` の共有状態 (適合)、1 ファイル 1 型 (新しい型はそれぞれ別ファイル — 適合)、body にロジックを置かない (適合)

### テストの実行表 (L-002)

| ビルドルート | 実行 | 件数と結果 |
|---|---|---|
| ios/ | 自分で実行 (Simulator `<udid>`、`-collect-test-diagnostics never`) | Swift Testing `Test run with 391 tests in 64 suites passed` / XCTest `Executed 0 tests` / `** TEST SUCCEEDED **` |
| android/ | 未実行 | 対象外 — この change は ios/ だけを触る |
| android/ (instrumented) | 未実行 | 対象外 (同上) |
| kmp/ | 自分では未実行 | 実装側の記録 `evidence/test-run-summary.md` で 169 件 / failures 0 |
| maui/ | 未実行 | 対象外 — MAUI の中身は UIView で、変わった SwiftUI のホストを通らない |
| maui/android/native/ | 未実行 | 対象外 (Android) |
| maui/macios/native/ | 自分で実行 (同じ Simulator) | Swift Testing `Test run with 17 tests in 7 suites passed` / `** TEST SUCCEEDED **` — `ios/` をローカルパッケージとして取り込むため、器のレイアウト部品 (`DialogLayoutApplier`) の変更の影響を確かめた |
| Scenario ID 網羅 | 自分で実行 | `PB-IS 19/19`、`結果: 未網羅なし` |

## 指摘事項

### 🟠 Major 表示中に中身の幅が広がっても、器が追従しない (既定の基準領域でも起きる退行)

**該当箇所**: `ios/Sources/KsDialogs/SwiftUI/DialogSwiftUIContentView.swift:52`-`68` (とくに `:59`-`62` の幅の提案)

**問題点**:
`intrinsicContentSize` は、今の幅 (`bounds.width`) を幅の提案にしてホストへ大きさを問い合わせる。大きさが可変の SwiftUI の中身は、幅を提案されると提案より広い幅を答えない。そのため、一度レイアウトされた後は、要求サイズの幅が今の幅を超えて広がらない。幅の変化は高さの変化に化ける (文が 1 語ずつ折り返して縦に伸びる)。

これは安全領域を受け取るかどうかに関係なく、SwiftUI の中身すべてに起きる。既定の可視領域でも起きる。変更前のコード (62b4981) と今の作業ツリーを、スクラッチに複製した同じ使い捨てのテストで比べた (コミットしていない)。縦長の窓 393 × 852 (安全領域 上 59・下 34)。

| 中身・条件 | 変更前 (62b4981) の器の矩形 | 今の器の矩形 |
|---|---|---|
| `Text` の文言を「OK」から「This is a much longer message」に変える。既定の可視領域・中央 | 39.67 × 36.33 → **250.67 × 36.33** | 39.67 × 36.33 → **39.67 × 322.67** |
| 同じ。窓全体・中央 | → 250.67 × 36.33 | → 39.67 × 322.67 |
| 同じ。窓全体・上端 | → 250.67 × 36.33 | → 39.67 × 322.67 |
| `Color.clear.frame(idealWidth:idealHeight:)` の理想の幅を 200 から 300 に変える (既定・窓全体中央・窓全体上端の 3 条件) | 200 × 100 → **300 × 100** | 200 × 100 → **200 × 100** (追従しない) |

食い違う契約:
- デルタスペックの用語「要求サイズ: …今と同じく、制約を付けずに求めた中身の理想の大きさ」。今は幅が今の幅で制約されている
- Requirement「安全領域を受け渡しても、器の大きさと位置は変わらない」の「中身の要求サイズが表示中に変わったら、器はその新しい大きさに追従する」。[PB-IS-14] は高さだけを変えるので、この退行を拾わない (`ios/Tests/KsDialogsTests/DialogSwiftUISafeAreaTests.swift:200`-`221`)
- Requirement「窓全体以外の基準領域では、SwiftUI の中身は安全領域を受け取らない」の「器の矩形も中身の位置も今と同じ」と、proposal の「既定の基準領域 (可視領域) では見た目は変わらない」。既定の可視領域で、文言が長くなる Dialog・Loading の中身 (進捗の文言を出すカスタム Loading、KMP から登録した SwiftUI の中身など) の見た目が変わる

**推奨修正**:
幅は提案なし (理想の幅) で求め、高さだけを「実際に置かれる幅」で求め直す形にする。たとえば、提案なしで理想の幅を得て、高さはその幅か今の幅のうち器が実際に許す方で問い合わせ、レイアウト後の幅で高さを測り直す。こうすれば変更前の追従が戻り、[PB-IS-15] のような折り返す本文の高さも幅に合わせて求まる。直し方は実装側に任せる。安全領域の引き戻し (`horizontalInsets` / `verticalInsets` を足して引く) はそのまま使える。

あわせて、幅が広がる変化のテストを足す。既定の可視領域と窓全体・端の両方で、中身の理想の幅を表示中に 200 から 300 に変え、器の幅が 300 になることを確かめる。
- 機構が無いと何が変わるか (L-001): 変更前のコード (今の幅を提案しない求め方) ではこのテストは通り (300)、今のコードでは落ちる (200)。上の比較で実測済みで、修正の有無で結果が分かれる

### 🟡 Minor [PB-IS-15] のテストが、deviation で合意した判定より弱い

**該当箇所**: `ios/Tests/KsDialogsTests/DialogSwiftUISafeAreaTests.swift:237`

**問題点**: deviation.md は [PB-IS-15] を「幅は窓の幅を超えず、安全領域を受け取らないときと同じ値 (この条件では 733.33)」で判定すると合意している。しかしテストは `first.width <= 852 + 0.5` しか見ていない。安全領域の引き戻しが横で崩れて幅が 733.33 から動いても、852 以下である限りこのテストは通る。

**推奨修正**: 同じ本文を安全領域を受け取らない条件 (たとえば一番外側で安全領域を無視する指定) で測った幅と一致することを確かめる。733.33 を直に書くと、起票済みの `fix-swiftui-window-area-first-layout-width` を直したときにテストが壊れるので避ける。
- 機構が無いと何が変わるか: 横の引き戻し (`intrinsicContentSize` の `horizontalInsets` を幅の提案に足して答えから引く処理) を外すと、受け取る / 受け取らない条件の幅が食い違い、このテストが落ちる。今のテストは落ちない

### 🔵 Suggestion テスト実行の記録に MAUI iOS 互換面の行を足す

**該当箇所**: `evidence/test-run-summary.md`

**問題点**: `maui/macios/native/` は `ios/` をローカルパッケージとして取り込むため、器のレイアウト部品 (`ios/Sources/KsDialogs/Layout/DialogLayoutApplier.swift`) の変更はこのルートにも届く。記録には ios/・kmp/・Scenario ID 網羅の 3 行しかない。tasks.md 2.5 が求める範囲は満たしており、このレビューで回して 17 件成功を確かめたので、実害は無い。

**推奨修正**: 修正サイクルの再実行で `maui/macios/native/` の行 (件数と結果) も記録に残す。残りのルート (android 系・maui/) は「対象外」と理由を書く。

## アクションプラン

1. 🟠 `DialogSwiftUIContentView.intrinsicContentSize` の幅の求め方を直し、表示中に幅が広がる変化に器が追従するようにする。既定の可視領域と窓全体・端の両方で、幅が広がる回帰テストを足す
2. 🟡 [PB-IS-15] のテストを、安全領域を受け取らない条件の幅と一致することを確かめる判定に強める
3. 🔵 再実行の記録に `maui/macios/native/` の行を足し、ios/ の全件・kmp/・Scenario ID 網羅と合わせて回し直す
