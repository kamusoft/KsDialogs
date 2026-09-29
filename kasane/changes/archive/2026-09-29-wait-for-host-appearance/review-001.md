# レビュー結果: wait-for-host-appearance (001 回目)

**日付**: 2026-09-28
**判定**: APPROVED

## サマリー

提示先の出現待ちを、Dialog・Loading・Toast の 3 機能と 4 形態 (iOS / Android / KMP / MAUI) でデルタスペックどおりに実装している。iOS の合図は提示先を選ぶ規則の隣 (`ApplicationKeyWindowProvider`) で 2 つの通知から作られ、3 機能の提示面の口に集約された。Dialog の待ちは紐付けの後・中身の生成の前に置かれ、打ち切り・待ち中の確定・列の順序 (呼んだ順・追い越さない・前の 1 枚の提示完了後に次を明ける) が両 Native で同じ形で実装されている。MAUI の `CancellationToken` の中継 (`DialogCallerCancellation`) も、ハンドル取得前後の取りこぼしを 1 か所のロックで直列にしていて堅い。

Critical / Major は無い。指摘は、アプリ全体で共有する列が UIKit の提示拒否で止まりうる点 (Minor)、公開 KDoc への ADR ID の追加 (Minor)、防御経路・競合経路の小さな改善 (Suggestion 2 件) にとどまる。ビルド・テストはオーケストレーター実施分 (evidence/test-run-summary.md。全形態で失敗 0) を前提とし、静的に実行できる検査 (`scripts/scenario-id-coverage.py --require-mirror`・`scripts/comment-policy-lint.py`・診断文言の日本語リテラルの grep) はこのレビューでも再実行して違反 0 件を確かめた。

## 照合した規約

- handbook cross/comment-policy.md (always)
- handbook cross/diagnostic-message-language.md — ライブラリ本体の失敗型の case 削除・例外文言・警告ログの追加 (iOS `KsDialogsKmpMissingFailureReason`、iOS Loading の警告、Android `checkNotNull` の文言、MAUI `DialogPresentationContext.UnavailableMessage`)
- handbook cross/ci-flaky-test-policy.md — Simulator / instrumented 上で状態遷移を観測するテストの追加と、iOS の並列実行の間欠失敗の切り分け ([付随修正])
- handbook cross/test-execution.md — テスト結果の報告と完了判定 (evidence/test-run-summary.md の照合)
- handbook cross/runtime-behavior-verification.md — 起動順・多段表示のタイミングが絡む不具合の完了判定 (evidence/before-fix-measurement.md・after-fix-measurement.md・ios-dialog-sequential-presentation.md の照合)
- handbook cross/sample-parity.md — `samples/ios`・`samples/kmp/iosApp` の自動再生の入口の変更
- lessons/code-review.md の重点観点 L-001 (KMP の「待っていること」の判定が、未登録の即時失敗と登録済みの待ちで結果が分かれるか)

## 確認した観点 (指摘なし)

- **仕様充足**: PB-HW-01〜08 (両 Native)・PB-HI-01〜04・PB-HA-01〜03・LD-HW-01〜07 (両 Native)・TS-HW-01〜03 (両 Native)・PB-KC-04 (両 OS)・DM-KM-01〜03・PB-KT-09・PB-MC-01〜08・PB-MH-01・DM-IO-01・DM-AN-01 のテストが宣言に ID つきで存在する。`--require-mirror` も通る。CA-SA-08/09・PB-MH-02 の除外登録は理由つき
- **tasks.md**: 差分はチェックボックスの `[x]` 化だけで、足場の本文は書き換えられていない。各タスクの実装・テスト・証跡 (1.2・1.3・5.4・9.2・9.3) は実在する
- **deviation.md**: 列をアプリ全体で 1 つにする判断、`nonisolated(nonsending)` と順番札・`@_spi(KsDialogsBridge)`、configure 中断時の順序、[付随修正] (Toast スイートの MainActor 混雑待ち) は合意済みとして扱った。[付随修正] は evidence/ios-parallel-toast-flake.md で「失敗件数 = 期限切れで捨てられた受理の件数」を全回で示しており、待ち時間の延長ではなく観測に基づく前提の修正になっている (ci-flaky-test-policy の「観測を直す」に適合)。skip の印・許可リストの追加は無い
- **打ち切りと確定の競合**: iOS は `withTaskCancellationHandler` の onCancel で結果チャネルを取り消しとして確定させ、確定の観察 (`observeSettlement`) から列を外す。明けた直後に `isResultSettled` を見て番を返すので、確定と明けの入れ違いでも中身は作られない。Android は `CompletableDeferred` の先着 1 回で同じ競合を処理し、`advance` が確定済みの先頭を読み飛ばす
- **列の番の返却**: 両 Native とも、中身の生成失敗・打ち切り・確定の全経路で `slot.finish()` が `defer` / `finally` に置かれている。明けた後に提示先が消えた場合は同じ札 (Android は先頭) で待ち直す
- **MAUI の打ち切り**: 呼び出し時点で打ち切り済みなら解決・VM 生成・configure・gateway のどれも走らない (`Dialog.ShowTypedAsync`・`DialogPresenter.PresentCoreAsync` の先頭)。ハンドル取得前の打ち切りはブリッジを呼ばずに cancelled で終え、取得中の打ち切りは取得直後に中継する。テスト用 gateway も本番の `DialogCallerCancellation` を通している
- **診断文言**: 追加・変更した文言は英語で、日本語リテラルの grep は 0 件。iOS と Android の Loading の警告は本文テンプレートが同じ
- **L-001**: KMP の `cancelWhileWaitingForHost` は「未登録は即時に失敗する / 登録済みは 500 ms 回しても結果も失敗も返らない」の違いで判定しており、View factory の解決の機構を外すと結果が分かれる (判定力がある)
- **実環境の証跡**: 直す前 (ios・kmp の 4 デモで出ない回がある) と直した後 (4 Sample × 5 デモで全回表示) が同じ手順で並び、MAUI の中身の供給の見込み (design Decision 2・7) も MAUI の iOS・Android の両 Sample で確かめている。許可ダイアログを閉じた後に、2 つの通知のどちらも来ないまま条件を満たす場面が無いことも観測済み

## 指摘事項

### 🟡 Minor アプリ全体で共有する iOS の列が、UIKit の提示拒否で止まりうる

**該当箇所**: `ios/Sources/KsDialogs/Presentation/UIKitDialogPresentationSurface.swift:32-48`、`ios/Sources/KsDialogs/Presentation/DialogPresenter.swift:124-127`

**問題点**: 列から明けた 1 枚は、提示面の `present` の完了通知 (UIKit の `present(_:animated:completion:)` の completion) で番を返す。`topmostViewController()` は閉じる途中の ViewController の手前で辿るのをやめるので、その提示元は「提示中」のままであり、UIKit はこの提示を拒否して completion を呼ばない (警告ログだけを出す)。この 1 枚の show が返らないのはこの change の前からの振る舞いだが、今回は番 (`presentingSlot`) が返らないため、`DialogHostWaitQueue.application` を共有するアプリ中のすべての後続の show (インスタンス渡し・KMP 互換面・MAUI ブリッジ) が、提示先があっても `waitForTurn` の早道に入れず列に並び続ける。呼び出し元がその 1 枚を打ち切れば解けるが、利用者からは「全ダイアログが出なくなった」と見える。起きるのは、待っていた Dialog が明けた瞬間に利用者側のモーダルが閉じる途中だった場合などに限られる。

**推奨修正**: 提示面の `present` で、UIKit が提示を受け付けなかったことを検出して `completion` を呼ぶ。たとえば `topmost.present(container, animated: false)` の直後に `container.presentingViewController == nil` なら拒否されたとみなして `completion()` を呼ぶ (受け付けた場合は非アニメーションの提示でも同期に設定される)。あわせて、拒否された 1 枚をどう終えるか (cancelled で確定させるか、提示先の出現を待ち直すか) を決めると、この change の前からある「返らない show」も解ける。後者は振る舞いの変更になるので、別 change に切り出してもよい。

### 🟡 Minor Android の公開 KDoc に ADR ID を足している

**該当箇所**: `android/ksdialogs-core/src/main/kotlin/jp/kamusoft/ksdialogs/KsDialog.kt:23`

**問題点**: 公開 interface `KsDialog.show` の KDoc に、今回の追加文として `(core/ADR-0039)` が入っている。comment-policy.md の「公開メンバーの doc コメント」は、ADR ID を含む内部用語を公開 doc コメントに書かないと定めている。同じ節の iOS (`ios/Sources/KsDialogs/Presentation/KsDialog.swift`)・KMP・MAUI の公開 doc コメントの追加文は ADR ID を含んでおらず、Android だけが食い違っている。同じ KDoc に前からある `(core/ADR-0015)` はこの change の範囲外の既存債務なので、ここでは問わない。

**推奨修正**: 追加した 1 行から `(core/ADR-0039)` を外す。設計根拠を残したい場合は、内部の `DialogPresenter.kt` のコメントに既に書かれているので、それで足りる。

### 🔵 Suggestion MAUI ブリッジの「閉じる」が中身の生成中に届くと、show を打ち切ってしまう

**該当箇所**: `maui/android/native/ksdialogs-maui-bridge/src/main/kotlin/jp/kamusoft/ksdialogs/maui/MauiDialogPresentation.kt:24-40,68-70`、`maui/macios/native/KsDialogsMauiBridge/MauiDialogPresentation.swift:22-35,61-65`

**問題点**: `dismiss()` は「報告口がまだ結び付いていない」ことを「中身を作る前」とみなして Job / Task を打ち切る。C# 側の `Dismiss` は VM の報告を受けた任意のスレッドから届くため、UI スレッドで `canSupplyContent()` が true を返してから `attach` するまでの間に届くと、中身を作り始めた show まで打ち切られる。`attach` が閉鎖要求を取り消して報告口で閉じるので C# が受け取る結果は VM の報告のまま (C# の `Settle` は先着優先) だが、閉鎖の種別は cancelled になり、Android では `suspendCancellableCoroutine` が打ち切りで即座に抜けるため、閉鎖の通知 (C# の配送の合図) が器の撤去より前に届く (core/ADR-0017 の「配送は撤去の後」から外れる)。VM が表示の直前に別スレッドから報告したときだけ起きる狭い競合で、結果の値は壊れない。

**推奨修正**: `canSupplyContent()` を「中身の供給を始めた」印を同じロックの中で立てる形にし、`dismiss()` はその印が無いときだけ Job / Task を打ち切る (印があれば閉鎖要求だけを残し、`attach` に任せる)。両 OS のブリッジで同じ形にする。

### 🔵 Suggestion MAUI の中身の供給で、画面の文脈の解決より先に利用者の factory が走る

**該当箇所**: `maui/KsDialogs.Maui/Platforms/iOS/PlatformDialogGateway.cs:39-41`、`maui/KsDialogs.Maui/Platforms/Android/PlatformDialogGateway.cs:72-75` (Loading・Toast の gateway の `RequirePresentationContext()` の呼び出し箇所も同じ並び)

**問題点**: `PlatformDialogContent.Create(request.CreateContent(), PlatformDialogContent.RequirePresentationContext())` は引数を左から評価するので、利用者の View factory が走ってから文脈を解決する。文脈が取れない防御経路 (PB-MH-02) では、作った View を捨てて `InvalidOperationException` になる。Dialog の gateway は今回、事前判定 (文脈を先に解決し、取れなければ View を作らない) からこの並びに変わった。Loading・Toast はこの change の前から同じ並び。

**推奨修正**: 文脈を先にローカル変数へ解決してから `request.CreateContent()` を呼ぶ。防御経路で利用者の factory (と、その中での notifier の読み出しなどの副作用) を走らせずに済む。到達させる手段が無い経路なので優先度は低い。

## アクションプラン

1. (Minor) `KsDialog.kt:23` の公開 KDoc から `(core/ADR-0039)` を外す — 1 行の修正
2. (Minor) iOS の提示面で UIKit の提示拒否を検出して `completion` を呼び、列の番が返るようにする。拒否された 1 枚の終え方を決めるところまでは、別 change に切り出してもよい
3. (Suggestion) 両 OS の MAUI ブリッジで、中身の供給を始めた後の `dismiss()` が Job / Task を打ち切らないようにする
4. (Suggestion) MAUI の中身の供給で、画面の文脈を利用者の factory より先に解決する
