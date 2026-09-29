# レビュー結果: wait-for-host-appearance (003 回目)

**日付**: 2026-09-28
**判定**: APPROVED

## サマリー

修正サイクル 2 で対応すると決めた 2 件は、どちらも解消されている。iOS の提示面は、遷移の完了の見張りを登録できなかったとき (`animate(alongsideTransition:completion:)` が false) に、その場で提示関係を確かめ直して載せられなかったと報告する。「載せられなかった」と報告した後に提示が遅れて結ばれた場合は、確定済みの結果を変えずに器を提示元から閉じる。deviation.md の「design Decision 3」の項は、追記分を含めて今の実装と 1 対 1 で照合できる。

修正で入った変更 (latch の失敗報告の記録・遷移の見張りの差し替え口・遅れて結ばれた器の撤去) に、退行や新しい穴は見当たらない。指摘は 0 件。ビルド・テストはオーケストレーター実施分 (iOS `xcodebuild test` 354 件・MAUI iOS ブリッジ 17 件・KMP iosSimulatorArm64Test 86 件、いずれも失敗 0。修正サイクル 2 は iOS の提示面とそのテストだけを変えた) を前提にした。このレビューでは、静的に実行できる検査 (`scripts/comment-policy-lint.py --summary`・`scripts/scenario-id-coverage.py --require-mirror`) を再実行して、違反 0 件・未網羅なしを確かめた。

## 照合した規約

- handbook cross/comment-policy.md (always) — 修正サイクル 2 で触ったファイル (`ios/Sources/KsDialogs/Presentation/UIKitDialogPresentationSurface.swift`・`DialogPresentationCompletionLatch.swift`、`ios/Tests/KsDialogsTests/UIKitDialogPresentationSurfaceTests.swift`・`DialogPresentationRefusalTests.swift`・`Support/LateBindingPresentingViewController.swift`、`maui/macios/native/KsDialogsMauiBridgeTests/MauiBridgePresentationRefusalTests.swift`・`Support/BridgeTestLateBindingViewController.swift`) のコメントを節ごとに照合した。許容参照は非公開の実装側コメントにある `core/ADR-0017` だけ。作業文書の参照・ローカル通番・履歴記述・仕様構文キーワードは無い。追加した型・メンバーはすべて internal (テスト側は test target) で、公開 doc コメントの節の対象は無い
- handbook cross/ci-flaky-test-policy.md — 実物の提示機構の上で状態遷移を観測するテスト (`MauiBridgePresentationRefusalTests.swift` の遅れて結ばれる場合) の追加。待ちは `BridgeTestWaiting.waitUntil` の条件待ちで、skip の印・許可リストの追加は無い
- handbook cross/test-execution.md — テスト結果の報告と完了判定 (オーケストレーター実施分の件数を前提にした)
- handbook cross/diagnostic-message-language.md — 修正サイクル 2 で診断文言の追加・変更は無い (該当なしを確認)
- lessons/code-review.md の重点観点 L-001 — 追加テストが「機構を外すと結果が分かれるか」を下の解消確認表で確かめた

## 前回指摘の解消確認表

| # | 出典 | 指摘 | 状態 | 確認した箇所と根拠 |
|---|---|---|---|---|
| 1 | second-opinion-code-002 Major (突き合わせで Major に確定) + review-002 Suggestion (1) | UIKit の遷移の完了を見張る登録 (`animate(alongsideTransition:completion:)`) の戻り値を見ていない。登録できず提示も拒否されると show と列の番が残る | 解消 | `ios/Sources/KsDialogs/Presentation/UIKitDialogPresentationSurface.swift:75-84` が登録の戻り値 `isObserving` を受け、false で提示関係が結ばれていなければ、その場で `latch.report(false)` を呼ぶ。登録は `:7-10` の `TransitionCompletionObservation` として差し替えられ、既定の `:89-96` は `animate(alongsideTransition:completion:)` の戻り値をそのまま返す。false の報告は `DialogPresenter.swift:125-131` で `slot?.finish()` と `container.handleHostLost()` へ進み、cancelled (origin は hostLost) で確定して列の番を返す。テスト: `ios/Tests/KsDialogsTests/UIKitDialogPresentationSurfaceTests.swift:146-173` (遷移の最中に登録が false を返すと、登録が 1 回試みられ、提示関係は無く、`[false]` がちょうど 1 回届く。`:82-84` の分岐を外すと結果は `[]` になるので分かれる)、`DialogPresentationRefusalTests.swift:81-124` (列から明けた A・B がともに cancelled で戻り、列が空になり、紐付けも外れる。分岐を外すと A が戻らず B も列に残るので、`results.count == 2` の待ちが時間切れになって分かれる) |
| 2 | review-002 Suggestion (2) (突き合わせで同じ箇所の保険として対応に確定) | 「載せられなかった」と報告した後に提示が遅れて結ばれると、片付け済みの器が画面に残る | 解消 | `DialogPresentationCompletionLatch.swift:10-11,21` が最初の報告が false だったかを `hasReportedFailure` に残す。`UIKitDialogPresentationSurface.swift:54-65` の提示機構の完了通知は、失敗を報告済みなら `container.presentingViewController?.dismiss(animated: false)` で器を提示元から閉じ、`latch.report(true)` は呼ばない (呼んでも latch が捨てるので、確定済みの結果は変わらない)。閉じた後の器の後始末も二重にならない: 器は `handleHostLost` → `finishRemoval(waitsForHostRemoval: false)` → `completeRemoval` で `.removed` になっており、閉鎖で走る `DialogContainerViewController.swift:316` の `viewDidDisappear` の判定は `isRemovalRequested` が立っているので `completeRemoval` へ進み、`didCompleteRemoval` のガードで何もしない。画面に載った時点の `beginLifecycle` (`:341`) も `.created` 以外を素通りするので、出現の演出も走らない。テスト: `UIKitDialogPresentationSurfaceTests.swift:175-200` (提示を預かって後から結ぶ提示元で、結んだ後に提示元へアニメーションなしの閉鎖がちょうど 1 回依頼され、報告は `[false]` のまま。`:57-62` の分岐を外すと閉鎖の依頼は `[]` になるので分かれる)、`DialogPresentationRefusalTests.swift:126-152` (Dialog の show として cancelled で戻った後に結ばれても結果は変わらず、紐付けも外れたまま)、`maui/macios/native/KsDialogsMauiBridgeTests/MauiBridgePresentationRefusalTests.swift:107-153` (実物の提示機構の上で、遅れて結ばれた器が画面から実際に外れ、閉鎖の通知は cancelled の 1 回だけ。分岐を外すと器が残り `presentedViewController == nil` の待ちが時間切れになるので分かれる) |
| — | review-001 Suggestion 1 | MAUI の両ブリッジで、中身の供給開始直後の閉じると打ち切りの競合 | 見送り (合意済み) | 突き合わせ結果のとおり再指摘しない |

review-002 の解消確認表で「解消」とした 4 件 (提示面が器を載せられなかった show の結末・待っている登録経路の Toast の factory・Android の公開 KDoc の ADR ID・MAUI の中身の供給の順) は、修正サイクル 2 で該当箇所が変わっていない (修正サイクル 2 で更新されたのは上記の iOS の提示面・latch・テストと deviation.md だけ) ので、解消のまま。

## deviation.md の「design Decision 3」の項と実装の照合

| deviation の記述 | 実装 | 一致 |
|---|---|---|
| 載せられなかった場合 = 提示先を再取得できない / UIKit が提示を拒否する | `UIKitDialogPresentationSurface.swift:47-51` (再取得できない)、`:66-72` (提示関係も遷移も無い = 拒否) | 一致 |
| 結果通知のルール 4 (a) の「器の消失」と同じく cancelled で確定し、列の番を返す | iOS は `DialogPresenter.swift:127,131` (`slot?.finish()` → `handleHostLost`。origin は hostLost)、Android は review-002 で照合済みの `ActivityDialogPresentationSurface.kt`・`DialogPresenter.kt` (修正サイクル 2 で変更なし) | 一致 |
| iOS は提示面の完了通知に「載せられたか」を持たせる | `DialogPresentationSurface.swift:9,24-30` の `DialogPresentationCompletion` の `didPresent` | 一致 |
| 提示の直後に提示関係が無ければ、遷移中は遷移の完了通知で、遷移が無ければその場で判定する | `UIKitDialogPresentationSurface.swift:67` (直後に結ばれていれば判定しない)、`:68-72` (遷移が無ければその場で false)、`:75-79` (遷移の完了通知の時点で結ばれていなければ false) | 一致 |
| Android は提示先が得られなければ中身を作らずに cancelled を届ける handle を返す | review-002 で照合済み (修正サイクル 2 で変更なし) | 一致 |
| 遷移の完了の見張りを提示機構が受け付けなかった (`animate(alongsideTransition:completion:)` が false) 場合は、その場で提示関係を確かめ直し、結ばれていなければ載せられなかったとして cancelled に確定させる (結ばれていれば提示機構の完了通知を待つ) | `UIKitDialogPresentationSurface.swift:82-84`。結ばれていれば何もせず、`:54-65` の提示機構の完了通知が `report(true)` を流す | 一致 |
| 載せられなかったと知らせた後に提示機構が遅れて提示を結んだ場合は、器の確定済みの結果は変えずに、器を提示元から閉じて画面から外す | `UIKitDialogPresentationSurface.swift:57-61` と `DialogPresentationCompletionLatch.swift:11,21` | 一致 |

deviation の各文に対応する実装があり、deviation に書かれていない振る舞いの追加も無い (遷移の見張りの差し替え口 `observeTransitionCompletion` は internal の初期化引数で、既定は提示機構の API をそのまま呼ぶだけなので、振る舞いの追加ではない)。

## 確認した観点 (指摘なし)

- **latch の失敗報告の記録**: `hasReportedFailure` は最初の報告でだけ書かれ、2 回目以降の `report` は `completion` が nil なので何も変えない。`completion` を呼ぶ前に書くので、`completion` の中から再入しても読み違えない。直接 `completion(false)` を呼ぶ「提示先を再取得できない」経路 (`:47-51`) は提示機構へ渡していないので、latch を持たなくても遅れて結ばれることは無い
- **遷移の見張りの差し替え口**: 型 `TransitionCompletionObservation` は `@MainActor` の閉包で、提示面 (`Sendable`) の `let` に保持しても隔離は崩れない。本番の生成箇所 (`Dialog.swift:22`・`KsDialogsKmp.swift:24`・`KsDialogsInteropBridge.swift:25`) はどれも既定値を使う。見張りの閉包が同期に `handler` を呼んでから false を返す実装であっても、latch が 1 回にまとめるので二重の報告にならない
- **遅れて結ばれた器の撤去の副作用**: 器の View は提示の前に `prepareForPresentation` で読み込み済みで、宣言的 UI のホストは `completeRemoval` の `releaseContentHost` で外れている。載った時点の `settleLayoutSnapshotOnScreen` はレイアウトを走らせるだけで、`beginLifecycle` のガード (`.created` のみ) により出現の演出・フックは走らない。閉鎖は完了通知の中で即座に依頼されるので、覆いが入力を吸い続けることも無い
- **撤去の対象の取り違え**: `container.presentingViewController?.dismiss(animated: false)` は、器を提示した画面から器以降を閉じる。遅れて結ばれてから完了通知が届くまでのごく短い間に別の show が器を提示先に選ぶと、その show の器も一緒に閉じられるが、その器は `viewDidDisappear` → `handleHostLost` で cancelled として確定・配送されるので、戻らない show や画面に残る器にはならない。遅れて結ばれること自体が実測では観測されていない保険の経路なので、指摘にはしない
- **テストの識別力 (L-001)**: 追加した 3 系統のテスト (見張れない場合・遅れて結ばれる場合の提示面単体・Dialog の show・MAUI の実物の提示機構) は、いずれも追加した分岐を外すと観測が変わる (解消確認表の各行に記載)。テスト用の提示元 (`LateBindingPresentingViewController`・`BridgeTestLateBindingViewController`) は `present` を預かるだけで、判定の述語 (`presentingViewController` と完了通知) は本物の UIKit の値を読むので、操作と観測が同じ入力を見ている
- **足場の凍結**: `git diff e2d4ac9 -- kasane/changes/wait-for-host-appearance/` は tasks.md のチェックボックス 33 件だけで、proposal / design / specs の書き換えは無い
- **仕様充足**: `scripts/scenario-id-coverage.py --require-mirror` は未網羅なし。`scripts/comment-policy-lint.py --summary` は禁止 0 件

## 指摘事項

なし。

## アクションプラン

なし (このまま次の段階へ進めてよい)。
