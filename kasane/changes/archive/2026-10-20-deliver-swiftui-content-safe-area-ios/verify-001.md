# 一致検証: deliver-swiftui-content-safe-area-ios (001 回目)

**日付**: 2026-10-20
**判定**: INVALID

19 の Scenario はすべて実装とテストで一致した (PB-IS-15 は deviation で合意済み)。ただし Requirement 本文の 2 つの約束 (要求サイズの変化への追従、既定の基準領域で器の矩形が今と同じ) が、幅の変化について守られていない。どちらも deviation.md に記録が無い。詳細と実測は `review-001.md` の Major の指摘。

## 対応表

### Requirement: 窓全体基準で端の軸に付けた SwiftUI の中身は、器と安全領域の重なりを標準の安全領域として受け取る

| Requirement / Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 本文: 端の軸の各辺で重なりを標準の安全領域として受け取る | `ios/Sources/KsDialogs/SwiftUI/DialogSwiftUIHost.swift:26` (`.container` を渡す)・`ios/Sources/KsDialogs/SwiftUI/DialogSwiftUIHost.swift:57` (渡さない辺をルートで無視)・`ios/Sources/KsDialogs/Layout/DialogLayoutApplier.swift:93`・`ios/Sources/KsDialogs/Layout/DialogLayoutApplier.swift:271`-`293` (軸の判定)・`ios/Sources/KsDialogs/SwiftUI/DialogSwiftUIContentView.swift:70`-`80` | 下の Scenario | ✅ 一致 |
| 本文: KMP の Swift 面から登録した SwiftUI の中身も同じく受け取る | 同じホストを通る: `ios/Sources/KsDialogs/Kmp/DialogViewFactory+Kmp.swift:34`・`:63`、`ios/Sources/KsDialogs/Kmp/KsLoadingKmp.swift:108`、`ios/Sources/KsDialogs/Kmp/KsToastKmp.swift:105` | Scenario なし (構造上、上の Scenario と同じ経路) | ✅ 一致 |
| [PB-IS-01] 上端の Dialog は上の重なりを受け取る | 同上 | `ios/Tests/KsDialogsTests/DialogSwiftUISafeAreaTests.swift:26` (期待値: 上 59・下 0・左右 0) | ✅ 一致 |
| [PB-IS-02] 下端の Dialog は下の重なりを受け取る | 同上 | `ios/Tests/KsDialogsTests/DialogSwiftUISafeAreaTests.swift:35` (上 0・下 34・左右 0) | ✅ 一致 |
| [PB-IS-03] 全面の Dialog は両辺の重なりを受け取る | 同上 (比率なしの fill は端の軸) | `ios/Tests/KsDialogsTests/DialogSwiftUISafeAreaTests.swift:44` (上 59・下 34・左右 0) | ✅ 一致 |
| [PB-IS-04] 中央の Dialog は重なりがあっても受け取らない | `ios/Sources/KsDialogs/Layout/DialogLayoutApplier.swift:290` (`.center` は渡さない) | `ios/Tests/KsDialogsTests/DialogSwiftUISafeAreaTests.swift:105` (履歴の全件が 0) | ✅ 一致 |
| [PB-IS-05] 上端の Loading は重なりを受け取る | 器の部品を共有 (`DialogLayoutApplier`) | `ios/Tests/KsDialogsTests/LoadingSwiftUISafeAreaTests.swift:12` (上 59・下 0) | ✅ 一致 |
| [PB-IS-06] 下端の Toast は重なりを受け取る | 同上 | `ios/Tests/KsDialogsTests/ToastSwiftUISafeAreaTests.swift:17` (上 0・下 34) | ✅ 一致 |
| [PB-IS-07] 窓が変わると新しい重なりになる | `ios/Sources/KsDialogs/SwiftUI/DialogSwiftUIContentView.swift:82`-`86` と UIKit の safe area の伝播 | `ios/Tests/KsDialogsTests/DialogSwiftUISafeAreaTests.swift:56` (前: 上 59・右 0、後: 上 0・右 59) | ✅ 一致 |
| [PB-IS-08] キーボードの領域は届けない | `ios/Sources/KsDialogs/SwiftUI/DialogSwiftUIHost.swift:26` | `ios/Tests/KsDialogsTests/DialogSwiftUISafeAreaTests.swift:73` (`safeAreaRegions == .container`、`.keyboard` を含まない) | ✅ 一致 |
| [PB-IS-09] 比率サイズを伴う全面の軸は中央の軸 | 解の位置が `.center` になる軸は渡さない (`ios/Sources/KsDialogs/Layout/DialogLayoutApplier.swift:290`) | `ios/Tests/KsDialogsTests/DialogSwiftUISafeAreaTests.swift:85` (上下に重なる矩形を確かめたうえで履歴の全件が 0) | ✅ 一致 |
| Side Effects (なし) | 状態変更はホストと器の View の表示上の状態 (ルートで無視する辺・要求サイズ) だけで、操作の後に残る永続データ・他の能力が読む状態・外への送信は無い | — | ✅ 一致 |

### Requirement: 安全領域を受け渡しても、器の大きさと位置は変わらない

| Requirement / Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 本文: 要求サイズは安全領域を受け取らないときと同じ値 (用語「要求サイズ: 制約を付けずに求めた中身の理想の大きさ」) | `ios/Sources/KsDialogs/SwiftUI/DialogSwiftUIContentView.swift:52`-`68` | 下の Scenario | ❌ 乖離 — 幅を今の幅 (`bounds.width`) で提案して求めるため、一度置かれた後は理想の幅ではなく今の幅で制約された値になる (下の行) |
| 本文: 中身の要求サイズが表示中に変わったら、器はその新しい大きさに追従する | `ios/Sources/KsDialogs/SwiftUI/DialogSwiftUIContentView.swift:90`-`97`・`:122`-`126` (高さは追従する) | `ios/Tests/KsDialogsTests/DialogSwiftUISafeAreaTests.swift:200` は高さだけ | ❌ 欠落 — 幅が広がる変化に追従しない。理想の幅 200 → 300 で器は 200 のまま、文言を長くすると幅 39.67 のまま高さ 322.67 に伸びる (変更前のコードでは 300、250.67 × 36.33)。見立て: **実装を直すべき** (spec の約束どおりで、変更前は満たしていた挙動の退行) |
| 本文: UIView の中身の扱いは変えない | relay の面を持たない中身には何も渡さない (`ios/Sources/KsDialogs/Layout/DialogLayoutApplier.swift:275`) | 下の PB-IS-16 | ✅ 一致 |
| [PB-IS-10] 端に付けた中身の器は要求サイズのまま | `ios/Sources/KsDialogs/SwiftUI/DialogSwiftUIContentView.swift:52`-`68` (安全領域の引き戻し) | `ios/Tests/KsDialogsTests/DialogSwiftUISafeAreaTests.swift:116` (1 秒間、器: 上端 y 0・高さ 100 / 下端 y 752・高さ 100、中身: 上端 y 59・高さ 41 / 下端 y 752・高さ 66) | ✅ 一致 |
| [PB-IS-11] 安全領域を無視する中身は今と同じ位置と大きさ | 同上 | `ios/Tests/KsDialogsTests/DialogSwiftUISafeAreaTests.swift:139` (器と中身がどちらも y 0 / 752・高さ 100) | ✅ 一致 |
| [PB-IS-12] 収まらない固定サイズの中身でも器の矩形は変わらない | 同上 | `ios/Tests/KsDialogsTests/DialogSwiftUISafeAreaTests.swift:163` (上端 / 下端 × 無視あり / なしの 4 条件、1 秒間) | ✅ 一致 |
| [PB-IS-13] 中央に置いた中身は膨らまない | 中央の軸は渡さない + 引き戻し | `ios/Tests/KsDialogsTests/DialogSwiftUISafeAreaTests.swift:189` (1 秒間 y 26・高さ 800) | ✅ 一致 |
| [PB-IS-14] 要求サイズが変わると器が追従する | `ios/Sources/KsDialogs/SwiftUI/DialogSwiftUIContentView.swift:90`-`97`・`:122`-`126` | `ios/Tests/KsDialogsTests/DialogSwiftUISafeAreaTests.swift:200` (高さ 160、上 59 のまま) | ✅ 一致 (Scenario の条件は高さの変化だけ。幅の欠落は上の本文の行) |
| [PB-IS-15] 横の安全領域で折り返す本文でも器の矩形は落ち着く | `ios/Sources/KsDialogs/SwiftUI/DialogSwiftUIContentView.swift:59`-`66` | `ios/Tests/KsDialogsTests/DialogSwiftUISafeAreaTests.swift:223` (1 秒間不変・幅 ≤ 852・左 59・右 0) | ⚠️ deviation 記録済み (幅は 852 ではなく受け取らないときと同じ値。テストは `≤ 852` しか見ておらず合意した判定より弱い — `review-001.md` の Minor) |
| [PB-IS-16] UIView の中身は今と同じく重なりを受け取る | 変更なし (relay の面を持たない) | `ios/Tests/KsDialogsTests/DialogSwiftUISafeAreaTests.swift:241` (上 59・下 0・高さ 100) | ✅ 一致 |
| Side Effects (なし) | 永続データ・他の能力が読む状態・外への送信なし | — | ✅ 一致 |

### Requirement: 窓全体以外の基準領域では、SwiftUI の中身は安全領域を受け取らない

| Requirement / Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 本文: 安全領域は 0 | `ios/Sources/KsDialogs/Layout/DialogLayoutApplier.swift:286` (窓全体以外は渡さない) | 下の Scenario | ✅ 一致 |
| 本文: 器の矩形も中身の位置も今と同じ | `ios/Sources/KsDialogs/SwiftUI/DialogSwiftUIContentView.swift:52`-`68` (既定の基準領域でも同じ求め方を通る) | 下の Scenario (静的な大きさだけ) | ❌ 乖離 — 既定の可視領域でも、表示中に中身の幅が広がると器の矩形が変更前と変わる (上の追従の行と同じ原因・同じ実測)。見立て: **実装を直すべき** |
| [PB-IS-17] 既定の基準領域で端に付けた Dialog は受け取らない | 同上 | `ios/Tests/KsDialogsTests/DialogSwiftUISafeAreaTests.swift:260` (履歴の全件が 0、器: 上端 y 59 / 下端 y 718・高さ 100) | ✅ 一致 |
| [PB-IS-18] 移動量で重なっても受け取らない | 同上 | `ios/Tests/KsDialogsTests/DialogSwiftUISafeAreaTests.swift:281` (履歴の全件が 0、器 y 29・高さ 100) | ✅ 一致 |
| [PB-IS-19] 既定の基準領域の Toast は受け取らない | 同上 | `ios/Tests/KsDialogsTests/ToastSwiftUISafeAreaTests.swift:31` (履歴の全件が 0) | ✅ 一致 |
| Side Effects (なし) | 永続データ・他の能力が読む状態・外への送信なし | — | ✅ 一致 |

## 追加検査

- [x] テスト: ios/ の全件を自分で実行した。Swift Testing `Test run with 391 tests in 64 suites passed` / XCTest `Executed 0 tests` / `** TEST SUCCEEDED **` (Simulator `<udid>`)。足した 19 件 (引数つきを含む) はすべて結果に現れる。`maui/macios/native/` も 17 件成功。`python3 scripts/scenario-id-coverage.py` は `PB-IS 19/19`・`結果: 未網羅なし`。kmp/ の 169 件は実装側の記録 (`evidence/test-run-summary.md`) で確認し、自分では回していない
- [ ] tasks.md: 全タスクにチェックがある。1.2「中身の要求サイズが表示中に変わったら器が追従するようにする」は高さの変化にしか効いておらず、幅について未達のままチェックされている (❌ — 上の対応表の追従の行と同じ欠落)
- [x] 逆流検査: 62b4981 以降のコミットは無く、作業ツリーの diff で `kasane/changes/deliver-swiftui-content-safe-area-ios/` 配下に出る変更は tasks.md のチェックだけ。proposal.md・specs/ は書き換えられていない
- [ ] 未記録乖離: 2 件 (要求サイズの変化への追従 (幅)・既定の基準領域で器の矩形が今と同じ (幅の変化))。同じ原因 (`ios/Sources/KsDialogs/SwiftUI/DialogSwiftUIContentView.swift:59`-`62` で今の幅を幅の提案にしている)
- [x] Side Effects: 3 つの Requirement とも「なし」。実装の状態変更は表示中の View の状態に閉じ、残る変更・外への送信は無い
- [x] 付随修正: deviation.md に `[付随修正]` の行は無い。diff にも Scenario に対応しない製品コードの変更は無い (`DialogSwiftUIRoot` を private から internal にしたのは [PB-IS-08] のテストでホストの型を引くため)
- [x] UI 変更: proposal の Non-Goals で `ui/` を作らないと合意済み (委任状の見た目の基準は ios/ADR-0002 の挙動)。見た目の確認は `evidence/real-window-safe-area.md` の実測

## ❌ の見立て

| ❌ | 見立て |
|---|---|
| 要求サイズの変化への追従 (幅) | 実装を直すべき。spec の約束どおりの挙動で、変更前のコードは満たしていた |
| 既定の基準領域で器の矩形が今と同じ (幅の変化) | 実装を直すべき。上と同じ原因で、上を直せば解消する見込み |
| tasks.md 1.2 のチェック | 上の修正で実態がチェックに追いつく |
