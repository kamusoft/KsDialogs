# レビュー結果: wait-for-host-appearance (002 回目)

**日付**: 2026-09-28
**判定**: APPROVED

## サマリー

修正サイクル 1 で対応すると決めた 4 件は、すべて解消されている。iOS の提示面は完了通知に「載せられたか」を持たせ、載せられなかった show を器の消失と同じ経路 (`handleHostLost`) で cancelled に確定させてから列の番を返す。Android の提示面は提示先が得られなければ中身を作らずに cancelled を届ける。待っている登録経路の Toast は受理の時点で解決した factory を持つ。MAUI の中身の供給は 6 つの gateway すべてで文脈を先に解決する。Android の公開 KDoc の ADR ID も外れている。deviation.md の「design Decision 3 (提示面が器を載せられなかった場合)」の項は、今の実装と 1 対 1 で照合できる。

修正で入った変更に退行は見当たらない。残った指摘は、iOS の提示面の「提示機構が受け付けなかった」判定の保険 (Suggestion 1 件) だけ。ビルド・テストはオーケストレーター実施分 (全形態で失敗 0) を前提とした。このレビューでは、静的に実行できる検査 (`scripts/scenario-id-coverage.py --require-mirror`・`scripts/comment-policy-lint.py`・診断文言の日本語リテラルの grep) を再実行して、違反 0 件を確かめた。

## 照合した規約

- handbook cross/comment-policy.md (always) — 公開 doc コメントの節は、この change で足した行を全形態の本体 (ios/Sources・android/ksdialogs-core/src/main・kmp commonMain・maui/KsDialogs.Maui・両 MAUI ブリッジ) で抜き出して照合した。ADR ID を含む追加行は、どれも非公開メンバーのコメントか実装側のコメントにある
- handbook cross/diagnostic-message-language.md — Android の `DialogException.PresentationHostUnavailable` の削除。検査の grep は 0 件
- handbook cross/test-execution.md — テスト結果の報告と完了判定 (オーケストレーター実施分の件数を前提にした)
- handbook cross/ci-flaky-test-policy.md — 実物の提示機構の上で状態遷移を観測するテストの追加 (`MauiBridgePresentationRefusalTests.swift`・`DialogPresentationRefusalTests.swift`)。skip の印と許可リストの追加は無い
- lessons/code-review.md の重点観点 L-001 (下の Suggestion で推奨する確かめ方にも適用した)

## 前回指摘の解消確認表

| # | 出典 | 指摘 | 状態 | 確認した箇所と根拠 |
|---|---|---|---|---|
| 1 | review-001 Minor 1 + second-opinion-code-001 Major 1 (突き合わせで Major に確定) | 提示面が器を載せられなかった show が終わらず、共有の列も止まる | 解消 | iOS: `ios/Sources/KsDialogs/Presentation/DialogPresentationSurface.swift:7-9,24-30` で完了通知 `DialogPresentationCompletion` が `didPresent` を持ち、「ちょうど 1 回呼ぶ」が要件になった。`UIKitDialogPresentationSurface.swift:36-40` は提示先を取り直せなければ `completion(false)`。`:49-62` は提示直後に提示関係が無ければ、遷移が無いときはその場で、遷移中なら遷移の完了通知で false を報告する。二重の報告は `DialogPresentationCompletionLatch.swift` が先着 1 回にする。`DialogPresenter.swift:125-133` は完了通知で `slot?.finish()` を呼んでから、false なら `container.handleHostLost()` で cancelled (origin は hostLost) に確定させる。`handleHostLost` → `finishRemoval(waitsForHostRemoval: false)` → 提示面の `dismiss` が「提示関係が無い」ので即時に完了 → `completeRemoval` で配送、と止まらずに進む。待っている間に VM の報告や呼び出し元の打ち切りで確定済みの場合も、器は `.created` のまま `handleSettled`・`handleCallerCancellation` を素通りし、`handleHostLost` の確定は先着で負けて、確定済みの結果が配送される。Android: `ActivityDialogPresentationSurface.kt:22-23,38-46` は提示先が得られなければ中身を作らずに `HOST_LOST` で確定させ、確定済みの結果を同期に届ける handle を返す。`DialogPresenter.kt:94-105` は戻り直後に `slot?.finish()` を呼ぶ。テスト: `ios/Tests/KsDialogsTests/DialogPresentationRefusalTests.swift` (提示機構が受け付けない場合に、A・B ともに cancelled で戻り、列が空になり、紐付けも外れる / factory の中で提示先が消えた場合に、A が cancelled で戻り、B は列で待ち続け、提示先が戻ると明ける)、`maui/macios/native/KsDialogsMauiBridgeTests/MauiBridgePresentationRefusalTests.swift` (実物の閉鎖遷移の最中に呼んだ 1 枚目は持ち越されて表示され、取り消されない / 同じ提示元からの 2 枚目は cancelled で閉じる)、`android/.../ActivityDialogPresentationSurfaceTests.kt:80-102` (中身は作られず、cancelled が 1 回届き、origin は `HOST_LOST`) |
| 2 | second-opinion-code-001 Major 2 | 待っている登録経路の Toast が、再登録後の factory で表示される | 解消 | `ios/Sources/KsDialogs/Presentation/ToastCoordinator.swift:94-95` の `accept` が `resolveFactoryIfNeeded` で `.registered` を `.resolved(viewModel:factory:)` (`ToastContentRequest.swift:12-14`) に置き換え、取り付けの時点の `makeContent` (`:275-278`) はレジストリを引き直さない。未登録なら受理の時点で同期に失敗する契約も保たれる。テスト: `ToastContractTests.swift` の「提示先を待っている間に View factory を登録し直しても、受理の時点の登録で表示される」が、受理時の factory が 1 回・再登録後の factory が 0 回であることを見る (解決の機構を外すと再登録後の factory が呼ばれるので、結果が分かれる) |
| 3 | review-001 Minor 2 | Android の公開 KDoc (`KsDialog.show`) に ADR ID | 解消 | `android/ksdialogs-core/src/main/kotlin/jp/kamusoft/ksdialogs/KsDialog.kt:21-25` の追加文に ADR ID は無い。この change で足した公開 doc コメントの行にも、全形態を通じて ADR ID は無い (上の「照合した規約」を参照) |
| 4 | review-001 Suggestion 2 | MAUI の中身の供給で、文脈の解決より先に利用者の factory が走る | 解消 | 両 OS の `PlatformDialogContent.CreateInPresentationContext` (`maui/KsDialogs.Maui/Platforms/iOS/PlatformDialogContent.cs:37-41`・`Platforms/Android/PlatformDialogContent.cs:36-40`) が、`RequirePresentationContext()` を先に呼んでから `createContent()` を呼ぶ。`Create` は private になり、Dialog・Loading・Toast の 6 つの gateway はすべてこの入口を通る (`Platforms/iOS/PlatformDialogGateway.cs:39`・`PlatformLoadingGateway.cs:142`・`PlatformToastGateway.cs:45`・`Platforms/Android/PlatformDialogGateway.cs:72`・`PlatformLoadingGateway.cs:137`・`PlatformToastGateway.cs:72`)。到達させる手段の無い防御経路なので、テストが無いのは妥当 |
| — | review-001 Suggestion 1 | MAUI の両ブリッジで、中身の供給開始直後の閉じると打ち切りの競合 | 見送り (合意済み) | 突き合わせ結果のとおり再指摘しない |

## deviation.md の「design Decision 3」の項と実装の照合

| deviation の記述 | 実装 | 一致 |
|---|---|---|
| 載せられなかった場合 = 提示先を再取得できない / UIKit が提示を拒否する | `UIKitDialogPresentationSurface.swift:36-40` (再取得できない)、`:49-62` (拒否) | 一致 |
| 結果通知のルール 4 (a) の「器の消失」と同じく cancelled で確定する | iOS は `handleHostLost` (`DialogContainerViewController.swift:499-505`。origin は hostLost)、Android は `settle(Cancelled, HOST_LOST)`。どちらも「未確定なら」で、確定済みの結果はそのまま届く (ルール 4 の「未確定なら cancelled」と同じ) | 一致 |
| 列の番を返す | iOS は `DialogPresenter.swift:127`、Android は `DialogPresenter.kt:102` | 一致 |
| iOS は提示面の完了通知に「載せられたか」を持たせる | `DialogPresentationCompletion` の `didPresent` | 一致 |
| 提示の直後に提示関係が無ければ、遷移中は遷移の完了通知で、遷移が無ければその場で判定する | `UIKitDialogPresentationSurface.swift:50-62` | 一致 |
| Android は提示先が得られなければ中身を作らずに cancelled を届ける handle を返す (`IllegalStateException` から改める) | `ActivityDialogPresentationSurface.kt:22-23,38-46`。`checkNotNull` の例外は無くなった | 一致 |

deviation の各文に対応する実装があり、deviation に書かれていない振る舞いの追加も無い。

## 確認した観点 (指摘なし)

- **修正で入った変更の退行**: iOS の完了通知の引数の追加で、テスト用の提示面 (`DialogTestPresentationSurface` ほか) も新しい要件に合わせてあり、完了通知を握って後から流す機能 (`holdsPresentationCompletion`) も残っている。`slot.finish()` は冪等 (`DialogHostWaitSlot.swift:15-19`) なので、完了通知での返却と `defer` での返却が重なっても列は 1 回しか進まない
- **Android の cancelled の届け方**: `notPresented` の handle は `onDelivery` の中で同期に handler を呼ぶ。`suspendCancellableCoroutine` のブロックの中で `resume` されるので、呼び出し元は即座に戻り、`finally` で紐付けが外れる。器を作らないので、撤去と配送の順序 (core/ADR-0017) を崩す経路も無い
- **Toast の受理時の解決**: 中身の生成は取り付けの時点に移ったが、提示先があれば受理と同じターンで取り付けるので、表示までの時点は変わらない。取り付けの時点の生成失敗は `discard` で 1 枚だけを捨て、期限の仕事も張らない (`ToastCoordinator.swift:150-151` のコメントとガード)。`attachPendingDisplays` の走査中に `discard` が配列を書き換えても、`for` は走査開始時の配列の写しを回すので安全
- **MAUI の供給の並び**: 文脈の解決に失敗したときの例外型 (`InvalidOperationException`) と文言は変わらず、利用者の factory だけが走らなくなった
- **仕様充足・tasks.md**: tasks.md の差分はチェックボックスの `[x]` 化 33 件だけで、本文の書き換えは無い。`--require-mirror` は未網羅なし
- **診断文言**: 追加・変更した文言は英語で、日本語リテラルの grep は 0 件

## 指摘事項

### 🔵 Suggestion iOS の提示面で、「提示機構が受け付けなかった」判定に保険が無い

**該当箇所**: `ios/Sources/KsDialogs/Presentation/UIKitDialogPresentationSurface.swift:43-62`

**問題点**: 拒否の判定は「提示の直後に提示関係が無い」「遷移の完了通知の時点で提示関係が無い」という UIKit の観測上の振る舞いに頼っている (deviation にあるとおり、MAUI の iOS ブリッジのテストホストで実測済み)。この前提から外れた場合の保険が 2 つの方向で無い。

1. `coordinator.animate(alongsideTransition:completion:)` の戻り値を見ていない。この API は、アニメーションを遷移に載せられなかったときに false を返す。そのとき completion が呼ばれる保証は無い。呼ばれなければ、拒否された提示の完了通知はどちらからも届かず、修正前と同じく show が終わらず、共有の列も止まる
2. false を報告した後に、提示機構が遅れて提示を結んだ場合 (判定の誤り)。器は `handleHostLost` で撤去と配送まで済んだ `.removed` の状態のまま、画面に残る。覆いが入力を吸うので、利用者からは「画面が操作できなくなった」と見える。latch は 2 回目の報告を捨てるので、この状態に気づく手段が無い

どちらも、今確かめられている遷移 (閉じる途中の画面の閉鎖) では起きない。起きうるのは、確かめていない種類の遷移の最中に明けた場合に限られる。

**推奨修正**: (1) `animate(alongsideTransition:completion:)` が false を返したら、その場で (または次の main loop の回で) 提示関係を見て判定する。(2) UIKit の提示の完了通知で、latch が既に false を報告済みなら、その器を提示元から閉じる (たとえば latch に「報告済みの値」を読める口を足し、`report(true)` が捨てられたときに `container.presentingViewController?.dismiss(animated: false)` を呼ぶ)。確かめるなら、(2) は「提示の完了通知を遅らせて false の判定の後に流す」テスト用の提示面で、器が提示の連なりから外れることを見る。保険を外すと器が残るので、結果が分かれる。優先度は低く、別 change でもよい。

## アクションプラン

1. (Suggestion・任意) iOS の提示面で、`animate(alongsideTransition:completion:)` が false を返した場合の判定と、false を報告した後に遅れて結ばれた提示を閉じる保険を足す
