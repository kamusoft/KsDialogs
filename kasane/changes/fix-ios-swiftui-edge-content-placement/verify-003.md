# 一致検証: fix-ios-swiftui-edge-content-placement (003 回目)

**日付**: 2026-10-21
**判定**: VALID

## 検証の範囲

- 突き合わせた相手: デルタスペック `specs/ios-native/spec.md` の全 Requirement / Scenario
- 実装: HEAD (29a94a6) に対する作業ツリーの差分。未追跡の新規ファイルと、修正サイクル 1・2 の変更を含む
- deviation.md: 2 行目 (縮められる範囲だけが変わって収まらなくなる向き) と 3 行目 (その逆向き) は、合意済みの差分として扱った
- テストの位置: Scenario ID を含むテスト名で示す

## 対応表

### MODIFIED: 窓全体基準で端の軸に付けた SwiftUI の中身は、器と安全領域の重なりを標準の安全領域として受け取る

| Requirement / Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| [PB-IS-01] 上端の Dialog は上の重なりを受け取る | `ios/Sources/KsDialogs/Layout/DialogLayoutApplier.swift:273` (`relaySafeArea`)・`ios/Sources/KsDialogs/SwiftUI/DialogSwiftUIContentView.swift:112` | `ios/Tests/KsDialogsTests/DialogSwiftUISafeAreaTests.swift` の `[PB-IS-01]` | ✅ 一致 |
| [PB-IS-02] 下端の Dialog は下の重なりを受け取る | 同上 | 同 `[PB-IS-02]` | ✅ 一致 |
| [PB-IS-03] 全面は両辺の重なりを受け取る | 同上 (fill の軸では `anchoredEdge` が nil を返す。`DialogLayoutApplier.swift:302-303`) | 同 `[PB-IS-03]` | ✅ 一致 |
| [PB-IS-04] 中央は受け取らない | `DialogLayoutApplier.swift:288-296` (`relaysSafeArea` の `.center`) | 同 `[PB-IS-04]` | ✅ 一致 |
| [PB-IS-05] Loading の端は受け取る | 3 つの器は同じ `DialogLayoutApplier` を使う | `ios/Tests/KsDialogsTests/LoadingSwiftUISafeAreaTests.swift` の `[PB-IS-05]` | ✅ 一致 |
| [PB-IS-06] Toast の端は受け取る | 同上 | `ios/Tests/KsDialogsTests/ToastSwiftUISafeAreaTests.swift` の `[PB-IS-06]` | ✅ 一致 |
| [PB-IS-07] 窓が変わると新しい重なりになる | `DialogSwiftUIContentView.swift:144` (`safeAreaInsetsDidChange`) | `DialogSwiftUISafeAreaTests` の `[PB-IS-07]` | ✅ 一致 |
| [PB-IS-08] キーボードの領域は届けない | `ios/Sources/KsDialogs/SwiftUI/DialogSwiftUIHost.swift:33` (`safeAreaRegions = .container`) | 同 `[PB-IS-08]` | ✅ 一致 |
| [PB-IS-09] 比率を伴う全面は中央の軸 | `ios/Sources/KsDialogs/Layout/DialogLayoutResolver.swift:90-96` (比率を伴う fill の位置は center) | 同 `[PB-IS-09]` | ✅ 一致 |
| 箇条: 収まらない片側の端の軸では、その軸の安全領域は 0 | `DialogSwiftUIHost.swift:84-97` (包みの frame と、内側の辺の `ignoresSafeArea`) | `ios/Tests/KsDialogsTests/DialogSwiftUIInnerEdgeAlignmentTests.swift` の `[PB-IS-24]` (固定サイズの中身では、一番外側が受け取る安全領域が 0) | ✅ 一致 |
| Side Effects (なし) | 状態変更は、器と中身の View の内部の保持値に限られる。永続化・外部への送信・他の能力が読む状態はない | — | ✅ 一致 |

### MODIFIED: 安全領域を受け渡しても、器の大きさと位置は変わらない

| Requirement / Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| [PB-IS-10] 端の器は要求サイズのまま (描かれた位置で測る) | `DialogSwiftUIContentView.swift:84-110` (要求サイズから安全領域を引き戻す) | `DialogSwiftUISafeAreaTests` の `[PB-IS-10]` (UIView の目印で測る。期待値: 上端 y 59 h 41、下端 y 752 h 66) | ✅ 一致 |
| [PB-IS-11] 安全領域を無視する中身は今と同じ | 同上 | 同 `[PB-IS-11]` | ✅ 一致 |
| [PB-IS-12] 収まらない固定サイズの中身は器いっぱい | `DialogSwiftUIContentView.swift:194-226` (はみ出しの判定)・`DialogSwiftUIHost.swift:84-97` | 同 `[PB-IS-12]` (4 通り。器と描かれた位置を 1 秒間測る) | ✅ 一致 |
| [PB-IS-13] 中央の中身は膨らまない | 変更なし | 同 `[PB-IS-13]` | ✅ 一致 |
| [PB-IS-14] 要求サイズの変化に器が追従する | `DialogSwiftUIContentView.swift:304` (`contentPreferredSizeDidChange`) | 同 `[PB-IS-14]` | ✅ 一致 |
| [PB-IS-15] 折り返す本文は窓の幅 852 まで広がり、左右 59 を受け取る | `DialogLayoutApplier.swift:332-334` (上限の通知)・`DialogSwiftUIContentView.swift:135-142` | 同 `[PB-IS-15]` (期待値: 幅 852 ±1.0、左右 59) | ✅ 一致 |
| [PB-IS-16] UIView の中身は従来どおり | UIView の経路は変更なし | 同 `[PB-IS-16]` | ✅ 一致 |
| Side Effects (なし) | 同上 | — | ✅ 一致 |

### ADDED: 窓全体基準で片側の端に付けた SwiftUI の中身が安全領域の内側に収まらないときは、内側の辺にそろって器いっぱいに置かれる

| Requirement / Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| [PB-IS-20] 下端の縮まない中身は器いっぱい (3 通り) | `DialogSwiftUIContentView.swift:194-226`・`DialogSwiftUIHost.swift:84-97` | `DialogSwiftUIInnerEdgeAlignmentTests` の `[PB-IS-20]` (帯と地色の両方に目印) | ✅ 一致 |
| [PB-IS-21] 上端の縮まない中身は器いっぱい (2 通り) | 同上 | 同 `[PB-IS-21]` | ✅ 一致 |
| [PB-IS-22] 左右の端で、書字方向に左右されない | `DialogSwiftUIHost.swift:140-148` (物理の向きを leading / trailing へ直す) | 同 `[PB-IS-22]` (6 ケース: 最初から右から左 / 表示後に切り替え。期待値: 右端 x 652、左端 x 0) | ✅ 一致 |
| [PB-IS-23] 一部だけ縮む中身は、縮んで内側の辺にそろう | [PB-IS-20] と同じ実装 | 同 `[PB-IS-23]` (上端 y 20 h 80、下端 y 752 h 80) | ✅ 一致 |
| [PB-IS-24] 読める位置は描かれた位置と一致する | 包みを常に置き、はみ出す軸でだけ効かせる (`DialogSwiftUIHost.swift:69-97`) | 同 `[PB-IS-24]` (一番外側と内側の子の両方を見る。受け取る安全領域も見る) | ✅ 一致 |
| [PB-IS-25] 大きさが変わって収まらなくなっても器いっぱいに落ち着く | `DialogSwiftUIContentView.swift:194-216` (入力が変わったら判定し直し、同じパスで反映する) | 同 `[PB-IS-25]` (5 ms ごとに 200 パス) | ✅ 一致 |
| [PB-IS-32] 比率サイズの下端も内側の辺にそろう | `DialogLayoutApplier.swift:302-313` (比率の有無を問わず start / end を渡す) | 同 `[PB-IS-32]` (器 y 766.8 h 85.2、中身 y 766.8 h 60) | ✅ 一致 |
| [PB-IS-33] 安全領域だけの変化でも新しい状態に落ち着く | `OverflowInputs.safeAreaInsets` (`DialogSwiftUIContentView.swift:179`) | 同 `[PB-IS-33]` | ✅ 一致 |
| [PB-IS-35] 器より大きい中身は付いた側へはみ出す | 包みの `min 0 / max ∞` と、内側の辺への配置 (`DialogSwiftUIHost.swift:84-97`) | 同 `[PB-IS-35]` (下端 y 0 h 1000、上端 y -148 h 1000。読める位置も同じ) | ✅ 一致 |
| [PB-IS-26] Loading・Toast の下端の縮まない中身 | 3 つの器は、共通の `DialogLayoutApplier` とホストを使う | `LoadingSwiftUISafeAreaTests`・`ToastSwiftUISafeAreaTests` の `[PB-IS-26]` | ✅ 一致 |
| [PB-IS-27] 全面と可視領域では並び方を変えない | `DialogLayoutApplier.swift:302-303` (fill と、窓全体以外の基準領域では nil) | `DialogSwiftUIInnerEdgeAlignmentTests` の `[PB-IS-27]` (全面 y 388.5、可視領域 y 59 / y 718) | ✅ 一致 |
| 切り替わりの箇条の拡張: 縮められる範囲だけが変わって収まらなくなっても、判定し直してそろえる (deviation.md 2 行目) | `ios/Sources/KsDialogs/SwiftUI/DialogSwiftUIContentLayoutReader.swift` (置き直しの検知)・`DialogSwiftUIContentView.swift:194-216`・`:254-259` | `DialogSwiftUIInnerEdgeAlignmentTests` の `settlesAfterOnlyShrinkableRangeChanges` (下端は y 752 h 100、上端は y 0 h 100 に落ち着き、200 パス動かない) | ⚠️ deviation 記録済み |
| 同上の逆向き: 縮められる範囲だけが変わって収まる状態へ戻ったら、そろえを外して安全領域の内側に置く (deviation.md 3 行目) | `DialogSwiftUIContentView.swift:254-259` (そろえが効いている軸でも測り直しを予約する)・`:205-215` (包みを外して測り、付け直す) | `DialogSwiftUIInnerEdgeAlignmentTests` の `settlesBackAfterShrinkableRangeReturns`。最初・最小の高さを外した後とも y 59 h 759 で、200 パス動かない。読める位置と受け取る安全領域 (上 59・下 34) も見る | ⚠️ deviation 記録済み |
| Side Effects (なし) | 状態変更は View の内部に閉じる。中身は、ホストのルートの包みの指定 (`DialogSwiftUIOverflowAlignment`)、判定の保持値 (`overflowInputs`・`needsOverflowRecheck`)、目印の View の `lastSize` | — | ✅ 一致 |

### ADDED: 大きさを中身に委ねた軸は、有効領域の上限が広がると新しい上限まで広がる

| Requirement / Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| [PB-IS-28] 理想の幅 600 は、横長で 600、縦長に戻して 393 | `DialogLayoutApplier.swift:332-334`・`DialogSwiftUIContentView.swift:135-142` | `ios/Tests/KsDialogsTests/ContentWidthLimitFollowTests.swift` の `[PB-IS-28]` | ✅ 一致 |
| [PB-IS-29] 中央の長い本文は、横長で 734 | 同上 | 同 `[PB-IS-29]` | ✅ 一致 |
| [PB-IS-34] 同じ寸法での置き直しでは変わらない | 前に知らされた上限と比べる (`DialogSwiftUIContentView.swift:138`) | 同 `[PB-IS-34]` | ✅ 一致 |
| [PB-IS-30] UIView の中身も、横長で 734 | 本体を変えずに成り立つ (タスク 2.2 で確認) | 同 `[PB-IS-30]` | ✅ 一致 |
| [PB-IS-31] Loading・Toast も、横長で 734 | 3 つの器は共通の `DialogLayoutApplier` を使う | 同 `[PB-IS-31]` | ✅ 一致 |
| Side Effects (なし) | 状態変更は、中身の View の `notifiedWidthLimit` と `proposedWidth` に限られる | — | ✅ 一致 |

## 追加検査

- [x] tasks.md: 1.1〜3.2 はすべてチェック済み。対応表と突き合わせ、虚偽のチェックはない。HEAD からの差分は `[ ]` → `[x]` だけで、本文は変わっていない。3.2 の証跡は、`evidence/bottom-confirm-window-end-after.png` と `evidence/implementation-verification.md:50-57` にある
- [x] 逆流検査: `git diff HEAD` で、`proposal.md`・`specs/`・`exploration.md`・`kasane/decisions/` に差分がないことを確かめた
- [x] 未記録の乖離: ❌ はない。deviation.md の行の内訳は次のとおり
  - 1 行目: 蒸留送り
  - 2・3 行目: 乖離。対応表では ⚠️ にした。3 行目は修正サイクル 2 の実装 (`hostedContentDidResize` の条件を外したこと) と一致する
- [x] Side Effects: 4 つの Requirement は、どれも「なし」。実装の状態変更は View の内部の保持値に閉じる。修正サイクル 2 は保持値を増やしていない (`needsOverflowRecheck` の立つ条件を広げただけ)
- [x] 付随修正: deviation.md に `[付随修正]` の行はない。diff にも、Scenario にも deviation にも対応しない本体の変更はない
- [x] UI 変更: brief とモックは作らないことに、proposal の Non-Goals で合意済み。見た目の基準 `ui/references/original-maui-bottom-confirm.png` と照合した証跡がある
- [x] テスト: レビューで実行した。作業専用の Simulator を使い、終了後に削除した
  - ios/: `Test run with 421 tests in 69 suites passed` / XCTest `Executed 0 tests`。修正サイクル 2 で足した `settlesBackAfterShrinkableRangeReturns` (2 ケース) が結果に現れ、通っている
  - maui/macios/native/: `Test run with 17 tests in 7 suites passed`
  - `python3 scripts/scenario-id-coverage.py`: `結果: 未網羅なし` (`PB-IS 35/35`)
  - kmp/ の `allTests` (iosSimulatorArm64Test 86 / testAndroidHostTest 83、失敗 0) と maui/ の `dotnet test` (合格 206 / 失敗 0) は、実装報告の値を採った

## 所見 (判定に影響しない)

- そろえが効いたまま中身が置き直されると、中身の `onGeometryChange` に、包みを外した途中の位置が 1 回届く。描かれた位置は動かず、最後に届く値は描かれた位置と一致する。このため [PB-IS-24] と、Requirement の「読める位置は描かれた位置と一致する」の落ち着いた状態は満たす。扱いは `review-003.md` の Suggestion を参照
