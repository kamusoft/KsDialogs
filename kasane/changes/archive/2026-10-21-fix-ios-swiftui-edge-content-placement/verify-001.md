# 一致検証: fix-ios-swiftui-edge-content-placement (001 回目)

**日付**: 2026-10-21
**判定**: VALID

デルタスペック `specs/ios-native/spec.md` の全 Requirement / Scenario を、HEAD (29a94a6) に対する作業ツリーの差分 (未追跡の新規ファイルを含む) と突き合わせた。テストの位置はテスト関数名 (Scenario ID を含む)。

## 対応表

### MODIFIED: 窓全体基準で端の軸に付けた SwiftUI の中身は、器と安全領域の重なりを標準の安全領域として受け取る

| Requirement / Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| [PB-IS-01] 上端の Dialog は上の重なりを受け取る | `ios/Sources/KsDialogs/Layout/DialogLayoutApplier.swift:273` (`relaySafeArea`)・`ios/Sources/KsDialogs/SwiftUI/DialogSwiftUIContentView.swift:106` | `ios/Tests/KsDialogsTests/DialogSwiftUISafeAreaTests.swift` の `[PB-IS-01]` (変更なし、通過) | ✅ 一致 |
| [PB-IS-02] 下端の Dialog は下の重なりを受け取る | 同上 | 同 `[PB-IS-02]` | ✅ 一致 |
| [PB-IS-03] 全面は両辺の重なりを受け取る | 同上 (fill は `anchoredEdge` が nil。`DialogLayoutApplier.swift:302`) | 同 `[PB-IS-03]` | ✅ 一致 |
| [PB-IS-04] 中央は受け取らない | `DialogLayoutApplier.swift:296` (`relaysSafeArea` の `.center`) | 同 `[PB-IS-04]` | ✅ 一致 |
| [PB-IS-05] Loading の端は受け取る | 3 器が同じ `DialogLayoutApplier` を使う (`ios/Sources/KsDialogs/Presentation/LoadingContainerViewController.swift:101`) | `ios/Tests/KsDialogsTests/LoadingSwiftUISafeAreaTests.swift` の `[PB-IS-05]` | ✅ 一致 |
| [PB-IS-06] Toast の端は受け取る | `ios/Sources/KsDialogs/Presentation/ToastContainerViewController.swift:79` | `ios/Tests/KsDialogsTests/ToastSwiftUISafeAreaTests.swift` の `[PB-IS-06]` | ✅ 一致 |
| [PB-IS-07] 窓が変わると新しい重なりになる | `DialogSwiftUIContentView.swift:138` (`safeAreaInsetsDidChange`) | `DialogSwiftUISafeAreaTests` の `[PB-IS-07]` | ✅ 一致 |
| [PB-IS-08] キーボードの領域は届けない | `ios/Sources/KsDialogs/SwiftUI/DialogSwiftUIHost.swift:32` (`safeAreaRegions = .container`) | 同 `[PB-IS-08]` | ✅ 一致 |
| [PB-IS-09] 比率を伴う全面は中央の軸 | `ios/Sources/KsDialogs/Layout/DialogLayoutResolver.swift` (比率 fill の位置は center) | 同 `[PB-IS-09]` | ✅ 一致 |
| 収まらない片側の端の軸は安全領域 0 (新しい箇条) | `DialogSwiftUIHost.swift:78-92` (包みの frame と内側の辺の `ignoresSafeArea`) | `ios/Tests/KsDialogsTests/DialogSwiftUIInnerEdgeAlignmentTests.swift` の `[PB-IS-24]` (固定サイズの中身で外側の安全領域 0) | ✅ 一致 |
| — Side Effects (なし) | 状態変更は器・中身の View 内部の保持値 (`anchoredHorizontalEdge` 等) だけで、永続化・外部送信・他の能力が読む状態は無い | — | ✅ 一致 |

### MODIFIED: 安全領域を受け渡しても、器の大きさと位置は変わらない

| Requirement / Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| [PB-IS-10] 端の器は要求サイズのまま (描かれた位置で測る) | `DialogSwiftUIContentView.swift:78-104` (要求サイズから安全領域を引き戻す) | `DialogSwiftUISafeAreaTests` の `[PB-IS-10]` (UIView の目印で描かれた位置を測る形に変更済み。期待値: 上端 y 59 h 41 / 下端 y 752 h 66) | ✅ 一致 |
| [PB-IS-11] 無視する中身は今と同じ | 同上 | 同 `[PB-IS-11]` | ✅ 一致 |
| [PB-IS-12] 収まらない固定サイズは器いっぱい | `DialogSwiftUIContentView.swift:184-214` (はみ出しの判定)・`DialogSwiftUIHost.swift:78-92` | 同 `[PB-IS-12]` (4 通り、器と描かれた位置を 1 秒間) | ✅ 一致 |
| [PB-IS-13] 中央は膨らまない | 変更なし | 同 `[PB-IS-13]` | ✅ 一致 |
| [PB-IS-14] 要求サイズの変化に追従 | `DialogSwiftUIContentView.swift:259` (`contentPreferredSizeDidChange`) | 同 `[PB-IS-14]` | ✅ 一致 |
| [PB-IS-15] 折り返す本文は窓の幅 852 まで広がる・左右 59 | `DialogLayoutApplier.swift:332` (上限の通知)・`DialogSwiftUIContentView.swift:129` | 同 `[PB-IS-15]` (期待値: 幅 852 ±1.0、左右 59) | ✅ 一致 |
| [PB-IS-16] UIView の中身は従来どおり | UIView の経路は変更なし | 同 `[PB-IS-16]` | ✅ 一致 |
| — Side Effects (なし) | 同上 | — | ✅ 一致 |

### ADDED: 窓全体基準で片側の端に付けた SwiftUI の中身が安全領域の内側に収まらないときは、内側の辺にそろって器いっぱいに置かれる

| Requirement / Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| [PB-IS-20] 下端の縮まない中身は器いっぱい (3 通り) | `DialogSwiftUIContentView.swift:184-214`・`DialogSwiftUIHost.swift:78-92` | `DialogSwiftUIInnerEdgeAlignmentTests` の `[PB-IS-20]` (帯・地色の両目印) | ✅ 一致 |
| [PB-IS-21] 上端の縮まない中身は器いっぱい (2 通り) | 同上 | 同 `[PB-IS-21]` | ✅ 一致 |
| [PB-IS-22] 左右の端・書字方向によらない (4 通り) | `DialogSwiftUIHost.swift:134-142` (物理の向きを leading / trailing へ変換) | 同 `[PB-IS-22]` (期待値: 右端 x 652 / 左端 x 0) | ✅ 一致 |
| [PB-IS-23] 一部だけ縮む中身は縮んで内側の辺にそろう | 同 `[PB-IS-20]` の実装 | 同 `[PB-IS-23]` (上端 y 20 h 80 / 下端 y 752 h 80) | ✅ 一致 |
| [PB-IS-24] 読める位置は描かれた位置と一致 | 包みを常に置き、はみ出す軸でだけ効かせる (`DialogSwiftUIHost.swift:66-92`) | 同 `[PB-IS-24]` (一番外側・内側の子の両方と安全領域) | ✅ 一致 |
| [PB-IS-25] 大きさが変わって収まらなくなっても器いっぱいに落ち着く | `DialogSwiftUIContentView.swift:193-213` (入力変化で判定し直し、同じパスで反映) | 同 `[PB-IS-25]` (5 ms ごと 200 パス) | ✅ 一致 |
| [PB-IS-32] 比率サイズの下端も内側の辺にそろう | `DialogLayoutApplier.swift:302` (比率の有無を問わず start / end を渡す) | 同 `[PB-IS-32]` (器 y 766.8 h 85.2 / 中身 y 766.8 h 60) | ✅ 一致 |
| [PB-IS-33] 安全領域だけの変化でも新しい状態に落ち着く | `OverflowInputs.safeAreaInsets` (`DialogSwiftUIContentView.swift:173`) | 同 `[PB-IS-33]` | ✅ 一致 |
| [PB-IS-35] 器より大きい中身は付いた側へはみ出す | 包みの `min 0 / max ∞` と内側の辺の配置 | 同 `[PB-IS-35]` (下端 y 0 h 1000 / 上端 y -148 h 1000、読める位置も同じ) | ✅ 一致 |
| [PB-IS-26] Loading・Toast の下端の縮まない中身 | 3 器共通の `DialogLayoutApplier` とホスト | `LoadingSwiftUISafeAreaTests`・`ToastSwiftUISafeAreaTests` の `[PB-IS-26]` | ✅ 一致 |
| [PB-IS-27] 全面・可視領域では並び方を変えない | `DialogLayoutApplier.swift:302-303` (fill・窓全体以外は nil) | `DialogSwiftUIInnerEdgeAlignmentTests` の `[PB-IS-27]` (全面 y 388.5 / 可視領域 y 59・y 718) | ✅ 一致 |
| — Side Effects (なし) | 状態変更はホストのルートの包みの指定 (`DialogSwiftUIOverflowAlignment`) と判定の保持値だけで、View 内部に閉じる | — | ✅ 一致 |

### ADDED: 大きさを中身に委ねた軸は、有効領域の上限が広がると新しい上限まで広がる

| Requirement / Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| [PB-IS-28] 理想の幅 600 は横長で 600、縦長に戻して 393 | `DialogLayoutApplier.swift:332`・`DialogSwiftUIContentView.swift:129-136` | `ios/Tests/KsDialogsTests/ContentWidthLimitFollowTests.swift` の `[PB-IS-28]` | ✅ 一致 |
| [PB-IS-29] 中央の長い本文は横長で 734 | 同上 | 同 `[PB-IS-29]` | ✅ 一致 |
| [PB-IS-34] 同じ寸法の置き直しでは変わらない | 前に知らされた上限との比較 (`DialogSwiftUIContentView.swift:132`) | 同 `[PB-IS-34]` | ✅ 一致 |
| [PB-IS-30] UIView の中身も横長で 734 | 本体の変更なしで成り立つ (タスク 2.2 の確認) | 同 `[PB-IS-30]` | ✅ 一致 |
| [PB-IS-31] Loading・Toast も横長で 734 | 3 器共通の `DialogLayoutApplier` | 同 `[PB-IS-31]` | ✅ 一致 |
| — Side Effects (なし) | 状態変更は中身の View の `notifiedWidthLimit`・`proposedWidth` だけ | — | ✅ 一致 |

## 追加検査

- [x] tasks.md: 1.1〜3.2 がすべてチェック済み。対応表と突き合わせて虚偽チェックは無い。3.2 の証跡は `evidence/bottom-confirm-window-end-after.png` と `evidence/implementation-verification.md` にあり、帯の下端が窓の下端に付くことを画像でも確かめた
- [x] 逆流検査: `git diff HEAD` で proposal.md・specs/・exploration.md・`kasane/decisions/` に差分なし。change 配下で変わったのは tasks.md のチェックだけ
- [x] 未記録乖離: ❌ なし。deviation.md の 1 行は蒸留送り (器より大きい中身のはみ出しを concepts へ反映) で、乖離ではない
- [x] Side Effects: 4 Requirement とも「なし」。実装の状態変更は View 内部の保持値に閉じ、列挙外の状態変更は無い
- [x] 付随修正: deviation.md に `[付随修正]` の行は無く、diff にも Scenario に対応しない本体の変更は無い (`DialogAxisLayout.isFilled` はタスク 1.1 の fill と start の区別)
- [x] UI 変更: proposal の Non-Goals で brief・モックを作らないと合意済み。見た目の基準 `ui/references/original-maui-bottom-confirm.png` と照合した証跡がある
- [x] テスト: レビューで自分で実行した (作業専用の Simulator で、終了後に削除)
  - ios/: `Test run with 419 tests in 69 suites passed` / XCTest `Executed 0 tests`
  - maui/macios/native/: `Test run with 17 tests in 7 suites passed`
  - maui/: `dotnet test` 合格 206 / 失敗 0
  - `python3 scripts/scenario-id-coverage.py`: `結果: 未網羅なし`
  - kmp/ `allTests` は実装報告の値 (iosSimulatorArm64Test 86 / testAndroidHostTest 83、失敗 0) を採用

## 所見 (判定に影響しない)

Scenario に現れない場合として、理想の大きさを変えずに縮みやすさだけが表示中に変わった中身 (最小の高さを足すなど) では、はみ出しの判定がやり直されず、縮まない中身が安全領域の内側の中央 (下端 y 735 / 上端 y 29.7) に残る。Requirement の切り替わりの箇条が挙げるきっかけ (理想の大きさ・安全領域) は実装が覆っており、どの Scenario にも当たらないため対応表では ❌ にしていない。扱いは `review-001.md` の指摘 1 で判断を求める。
