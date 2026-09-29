# Design: fix-android-startup-toast-under-splash

## Context

Android の提示先は resumed な Activity で、起動直後は最初の描画と起動画面の退場より前にそろう。Toast の表示時間は受理の時点から数える。このため、起動直後に受け付けた短い Toast は、起動画面の下で表示時間を使い切る (proposal「Why」)。

探索と提案作成で、次の 2 つを決めた:

- [core/ADR-0044](../../decisions/core/0044-android-host-is-drawn-resumed-activity.md): Android の提示先は resumed で、かつ描画された Activity とし、Dialog・Loading・Toast で揃える
- [core/ADR-0043](../../decisions/core/0043-toast-duration-from-visible-when-accepted-in-foreground-without-host.md): アプリが前面にいるのに提示先が無い間 (以下「前面の待ち」) に受け付けた Toast は、提示先に載った時点から表示時間を数える

現状のコード (2026-09-29 の調査。ksn-scout 2 本):

| 項目 | Android | iOS |
|---|---|---|
| 提示先の判定 | `ResumedActivityTracker` (resume で立て、pause・破棄で下ろす)。3 機能の提示面がこれを読む (`ActivityDialogPresentationSurface.kt:18-19`・`LoadingPresentationSurface.kt:40-52`・`ToastPresentationSurface.kt:39-51`) | `ApplicationKeyWindowProvider.swift:7-17,70-75` (前面でアクティブなシーンの key window)。3 機能ともこの供給元を読む |
| 提示先の出現の合図 | 追跡役の入れ替わりの通知 (`ResumedActivityTracker.kt:59-62,89-93`)。pause で下ろしたときは通知しない | `UIWindow.didBecomeKeyNotification` と `UIScene.didActivateNotification` (`ApplicationKeyWindowProvider.swift:26-47`)。購読は待ちがある間だけ |
| 追跡の開始 | ContentProvider の `onCreate` で自動 (`KsDialogsInstaller.kt:16-20`) | 起動時に自動で動き出す仕組みは無い |
| Toast の期限 | 受理時に `SystemClock.elapsedRealtime()` から決め、`ToastDisplay.kt:59` の val に固定 (`ToastCoordinator.kt:101`) | 受理時に `ContinuousClock.now + duration` を決め、`ToastDisplay.swift:27-28` の let に固定 (`ToastCoordinator.swift:99`) |
| 時計の差し替え口 | 無い | 無い |
| テストの提示先の作り方 | unit は素の JVM で `Activity()` を置き、追跡役の通知を直接呼ぶ (Robolectric なし)。instrumented は偽の提示面か `hidesPresentationHost` | 偽の key window 供給元と、合図を手で送る口 (`Support/DialogTestKeyWindowProvider.swift`・`DialogTestHostAppearanceSignal.swift`) |

KMP は両 OS とも Native の Dialog・Loading・Toast を包むだけで、Activity もシーンも参照しない。MAUI は提示先の判定と Toast の計時を Native に任せる。ただし MAUI Android は、中身を作る画面の文脈を MAUI 側で選ぶ経路を別に持つ (`maui/KsDialogs.Maui/Platforms/Android/PlatformDialogContent.cs:144-156`、`ActivityStateManager.Default.GetCurrentActivity()`)。この経路は、Native が提示先を確保した後に中身を作る文脈を決めるもので、提示先の判定ではない。

## Goals / Non-Goals

Goals:
- Android で、resumed だが未描画の画面には Dialog・Loading・Toast を載せない
- 前面の待ちの間に受け付けた Toast は、載った時点から数えて表示される。Android の起動直後は、起動画面が退いた後に表示時間の 2/3 以上が見える。背面で受け付けた Toast の扱いは変えない
- Android 系 3 ルートの Sample から「最初の描画の後に再生する」待ちを外しても、起動直後の Toast が見える

Non-Goals: proposal の Non-Goals のとおり。

## Decisions

### Decision 1: Android の提示先の「描画された」を追跡役の中で持つ

**採用案:**
- `ResumedActivityTracker` に、Activity ごとの「描画済み」の印を持たせる。提示先の供給 (`ResumedActivityProvider.resumedActivity`、`ResumedActivityProvider.kt:10-13`) は、最後に resume した Activity が描画済みのときだけ、その Activity を返す
- 描画の合図:
  - `onActivityStarted` で、その Activity の decorView に `ViewTreeObserver.OnDrawListener` を張る
  - 最初の描画を受けたら、次のメッセージ周回 (`View.post`) で印を立てて listener を外し、入れ替わりの購読者へ通知する (`notifyResumedChange`)
  - post を挟むのは、`OnDrawListener` の中では listener の追加・削除ができないため
  - 観測には世代を持たせる。stop・破棄で世代を進め、描画を受けてから印を立てるまでの間に stop・破棄した Activity では、遅れて走った処理が印を立てないようにする (古い描画で、次の start 後の描画を待たずに提示先になるのを防ぐ)
- 印を下ろすのは `onActivityStopped` と `onActivityDestroyed` だけにする。pause では下ろさない。resumed でなくなるので、提示先の参照は今までどおり pause で下ろす
- `onActivityResumed` の時点で印が立っていなければ、decorView の `invalidate()` で描画を促す (取りこぼしの保険)
- 描画の観測は、差し替えられる口の後ろに置く (名前は実装で決める)
  - 既定の実装は、上記の ViewTreeObserver を使う
  - unit テストは、偽の実装で「描画された」を手で送る (素の JVM の `Activity()` は window を持たない)
- 3 機能の提示面・Dialog の待ちの列 (`DialogHostWaitQueue.kt`)・Loading と Toast の載せ直し (`LoadingCoordinator.onHostChanged`・`ToastCoordinator.onHostChanged` `:279-302`) には手を入れない。印を立てたときの通知で、これまでどおり動く
- 実装箇所のコメントは、理由を現在形で説明する。決定を参照するときは `core/ADR-0044` の形で書く (`kasane/handbook/cross/comment-policy.md`)

**理由:**
- 提示先の供給の中に閉じれば、3 機能・待ちの列・載せ直しが、そのまま新しい条件に従う (core/ADR-0044)
- 起動画面の延長 (core-splashscreen の延長条件) は、描画の直前 (PreDraw) で描画を止める仕組みである。`invalidate()` で描画を促しても、延長を破らない

**代替案:**
- **A: `OnPreDrawListener` を合図にする** — 却下。起動画面の延長中も PreDraw は来るので、延長中に載ってしまう
- **B: `ViewTreeObserver.registerFrameCommitCallback` (API 29 以上) でフレームの確定まで待つ** — 却下。API 24〜28 の分岐が要る。得られる差は 1 フレームほどで、起動画面の退場の演出 (数百 ms と推測) に比べて小さい
- **C: 印を pause で下ろす** — 却下。pause だけからの復帰 (透過画面や権限ダイアログが閉じたとき) では再描画が起きないことがあり、提示先が現れないまま Dialog が返らなくなる
- **D: ウィンドウのフォーカスを合図にする** — 却下 (core/ADR-0044 の却下案)

### Decision 2: 前面の待ちを「アプリが前面にいるのに提示先が無い間」として、今の状態だけで判定する

**採用案:**
- 前面の待ちは「アプリが前面にいる (画面が背面に下がっていない) のに、提示先が無い間」とする
  - 画面を開いている途中 (コールド起動・プロセスが残ったままの再起動・画面の切り替え・背面から戻る途中・作り直し) を含む
  - 割り込み (システムの許可ダイアログ・コントロールセンターなど) で提示先を一時的に失っている間も含む
- Android の「前面」は、作成済みで、まだ stop も破棄もされていない Activity が 1 つ以上あること
  - 追跡役が `onActivityCreated`・`onActivityStarted` で集合に入れ、`onActivityStopped`・`onActivityDestroyed` で外す
  - 集合が空になったら (背面に下がったら)、入れ替わりの購読者へ通知する。ただし背面の確定は、stop・破棄の通知の直後ではなく、次のメッセージ周回で集合が空のままのときに行う。構成の変更による作り直しでは、旧 Activity の stop・破棄と新 Activity の作成が同じメッセージの中で続くので、この間を背面とみなさない
  - コールド起動の `onCreate` の間は「作成済み・未 start」なので前面に入る。Activity が 1 つも作られていない間 (`Application.onCreate` など) は背面とみなす
- iOS の「前面」は、activationState が foregroundActive か foregroundInactive のシーンが 1 つ以上あること
  - 判定は、提示先の規則と同じ供給元 (`ApplicationKeyWindowProvider`) に置く。シーンの写し (`DialogWindowSceneSnapshot.swift`) に前面かどうかを足す
  - シーンが 1 つもつながっていない間 (アプリの初期化処理など) は背面とみなす
- 「前面を離れた」の合図
  - Android は上記の集合が空になったときの通知
  - iOS は `UIScene.didEnterBackgroundNotification`。提示先の出現の合図 (`hostAppearanceNotifications`) とは別の口として、同じ供給元に足す。購読は前面の待ちの Toast がある間だけ張る (出現の合図と同じ作法)
  - iOS の合図は、通知を受けた時点で前面かどうかを読み直し、前面のシーンが 1 つも無くなっていたときだけ届ける。複数のシーンのうち 1 つが背面へ入っても、別のシーンが前面なら前面を離れたとみなさない
- 判定する時点は、Toast の表示の開始処理 (UI スレッドの手番。Android `ToastCoordinator.beginDisplay` `:116`、iOS `ToastCoordinator.beginDisplay` `:129`) とする。呼び出しスレッドの時点では判定しない。契約の「前面の待ちの間に受理された」の「受理された時点の状態」は、この開始処理の時点の状態を指す (toast-contract デルタに明記する)。show を呼んでから開始処理までの間に前面・背面が変わったら、開始処理の時点の状態に従う
- 実装箇所のコメントは、理由を現在形で説明する。決定を参照するときは `core/ADR-0043` の形で書く

**理由:**
- 前面・背面だけで分ければ、状態の履歴が要らず、今の状態だけで決まる
- iOS のライブラリは起動時に動き出せない。利用者が最初に呼ぶ時点では、起動の途中を知らせる通知が過ぎていることが多いので、履歴に頼ると判定に推定が混じる
- 両 OS を「前面にいるのに提示先が無い間」という同じ規則で言える
- 前面・提示先の状態は UI スレッドで変わる。同じ手番で判定すれば、判定と載せる処理が食い違わない

**代替案:**
- **A: 画面を開いている途中に限り、割り込みの最中は含めない** (探索の論点 5 で一度採った範囲) — 却下 (オーナー判断 2026-09-29)。起動の途中と割り込みを見分けるには状態の履歴が要るが、iOS では最初の呼び出しより前の履歴が取れない。割り込みの下で Toast が見えないまま終わることも残る
- **B: 呼び出しスレッドで、受理した時点に判定する** — 却下。状態は UI スレッドで変わるので、呼び出しスレッドで読んだ判定と、UI スレッドで載せる処理が食い違いうる

### Decision 3: Toast の期限を「未確定」にでき、載った時点か前面を離れた時点で決める

**採用案:**
- 受理の時刻は、今までどおり呼び出しの時点で記録する
- 表示の開始処理で、次のように期限を決める
  - 前面の待ちなら、期限を未確定のまま保留する
  - そうでなければ、期限 = 受理の時刻 + duration (今までどおり)
- 未確定の表示の期限は、次のどちらかの時点で決め、その時点からタイマーを起動する
  - 提示先に載った時点 (Android `attachIfPossible` の取り付け成功 `:184`、iOS `attachIfPossible` `:168-197` の取り付け成功): 期限 = 載った時点 + duration
  - 載る前に前面を離れた合図が来た時点: 期限 = その時点 + duration。そのあと期限の前に提示先が現れれば、その期限まで表示する
- 期限切れの判定 (Android `:117`・`:150`・`hasReachedDeadline` `:189-190`、iOS `:136`・`:170`) は、期限が決まった表示にだけ行う
- 期限は、一度決めたら動かさない。作り直しや載せ直しをまたいでも、残り時間は巻き戻らない (今までどおり)
- 期限を持つ型 (Android `ToastDisplay.kt:59` の val、iOS `ToastDisplay.swift:27-28` の let) は、「未確定」を表せる形にする

**理由:**
- core/ADR-0044 のもとで、載った時点 = Android で画面が見えた時点になる。iOS の提示先 (前面でアクティブなシーンの key window) も、見えている画面である
- 「一度決めた期限は動かさない」を保てば、作り直しと多重表示の既存の規則 (TS-AN-03 ほか) に手が入らない

**代替案:**
- **A: 前面の待ちの間は期限を決めたうえで、待っている間だけ時計を止める** — 却下。止める・進めるを繰り返す状態を持つことになる。載った時点で決める方が、「一度決めた期限は動かさない」をそのまま保てる

### Decision 4: 時計は差し替えず、数え始めは実時間の待ちで確かめる

**採用案:**
- 時計の差し替え口は足さない
- 数え始めのテストは、実時間の待ちで組む。既存の TS-HW-01〜03 と同じ作法で、`kasane/handbook/cross/ci-flaky-test-policy.md` の節「実時間の期限を持つ表示を観察するテスト」に従う
  - duration より長く提示先の無い時間を置き、そのあと提示先を出す
  - 表示されることと、載った時点からおよそ duration で消えることを確かめる

**理由:** 既存の作法で観察でき、両 OS の coordinator と表示の型に注入口を足さずに済む

**代替案:**
- **A: 両 OS に時計の差し替え口を足す** — 却下。注入口が要る型が両 OS で 2 つずつあり、この変更の検証には既存の作法で足りる

### Decision 5: 「resumed だが未描画」を、unit は差し替え口で、instrumented は描画を止める試験用 Activity で作る

**採用案:**
- Android unit: Decision 1 の差し替え口で「描画された」を手で送る。追跡役のテスト (`ResumedActivityTrackerTests`) と Dialog の待ちのテスト (`DialogActivityHostWaitTests`) に足す
- Android instrumented: androidTest に、最初の描画を止める試験用 Activity を置く
  - decorView に「描画しない」を返す `OnPreDrawListener` を置き、テストが解除するまで描画を止める (起動画面の延長と同じ仕組み)
  - これで、3 機能の「resumed だが未描画の間は載らず、描画で載る」を、実物の描画で確かめる
- Toast の前面の待ちのテスト
  - 偽の提示面 (Android `support/ToastTestPresentationSurface.kt`、iOS `Support/ToastTestPresentationSurface.swift`) に、前面かどうかと前面を離れた合図を持たせる
  - これに合わせて、両 OS の Toast の提示面の口に「前面かどうか」と「前面を離れた」の購読を足す
- iOS: 偽の key window 供給元に、前面かどうかと、前面を離れた合図を手で送る口を足す

**理由:**
- 素の JVM では描画を起こせない
- 描画を止める仕組みは、利用者が起動画面を延ばすときと同じなので、実際の延長中の振る舞いも同時に確かめられる

**代替案:**
- **A: Robolectric を入れて unit で描画を起こす** — 却下。テストの基盤が増える。実物の描画は instrumented で確かめられる

### Decision 6: Android 系 3 ルートの Sample から、最初の描画の後へ回す待ちを外す

**採用案:**
- 次の 3 か所の待ちを外し、最初の画面の表示時の処理から再生する。待ちの理由を書いたコメントも外す
  - `samples/android/app/src/main/kotlin/jp/kamusoft/ksdialogs/samples/android/MainActivity.kt:102` の `menuView.post`
  - `samples/kmp/androidApp/src/main/kotlin/jp/kamusoft/ksdialogs/samples/kmp/android/MainActivity.kt:105` の `menuView.post`
  - `samples/maui/KsDialogs.Sample.Maui/SampleMenuPage.xaml.cs:95-103` の `#if ANDROID` の `Dispatcher.DispatchAsync`
- 作り直しで再発火しない守り (`savedInstanceState` の判定と、1 回限りの取り出し) は変えない

**理由:** ライブラリが「見えてから載せ、見えてから数える」ようになるので、Sample が提示先の条件をなぞる必要が無くなる。4 ルートの自動再生の入口が揃う (handbook/cross/sample-parity.md)

**代替案:**
- **A: Sample の待ちを残す** — 却下。オーナー指示のスコープ (proposal「What Changes」の Sample)。待ちを残すと、ライブラリの修正を Sample で確かめられない

### Decision 7: 直す前と直した後を、同じ端末・同じ手順で撮る

**採用案:**
- 直す前: 今のライブラリに、Decision 6 の待ちを外した Sample を載せて撮る。直した後: 同じ端末・同じ手順で撮る。手順は `kasane/config.yaml` の `ui.screenshot`。端末は専用に作り、作業後に削除する
- Android エミュレータ (API 36):
  - Android 系 3 ルートで、`default-toast` と `custom-toast` を、コールド起動とプロセスが残ったままの再起動でそれぞれ 5 回
  - Dialog・Loading の代表 (`basic-dialog`・`default-loading`) を 3 回
  - Toast の見えている時間を、画面の録画 (`adb shell screenrecord`) から測る。起動画面が退いたコマ (Toast が見え始めたコマ) から Toast が消えたコマまでの時間を読む。スクリーンショットの連写は 1 コマに 0.3〜数秒かかり粗すぎるので使わない。コマの時刻の取り出しは、macOS 標準の動画の仕組み (AVFoundation) を使う小さなスクリプトで行う (ffmpeg は入っていない)
  - 合格ライン (オーナー判断 2026-09-29): 起動画面が退いた後に Toast が見えている時間が、表示時間の 2/3 以上 (既定の 1500 ms なら 1000 ms 以上)。Android 系 3 ルートの Toast の 2 デモで、全回満たすこと。割る回が 1 回でも出たら、実装を止めてオーナーに諮る (core/ADR-0044 の見直しのきっかけ)
- iOS Simulator:
  - 3 ルート (`ios`・KMP iOS・MAUI iOS) の起動直後の Toast を撮る
  - 割り込みの最中 (許可ダイアログ) に受け付けた Toast が、割り込みの後に表示されることを撮る。手順は `kasane/handbook/cross/runtime-behavior-verification.md` の小節「iOS Simulator でシーンの状態を観測するとき」
- MAUI Android: 起動直後のカスタム Toast とカスタム Dialog が、正しい画面の文脈で表示されることを確かめる
- 記録は `evidence/` に残す

**理由:** 前回の実測は Mac の負荷の変動が大きかった。同じ条件で前後を比べないと、差が負荷のせいか直したせいか分からない

**代替案:**
- **A: 前回 (align-sample-autoplay-start) の「待ちなし」の実測を直す前として使う** — 却下。端末も負荷も違う
- **B: 数値の合格ラインを置かず、見えている時間を記録してオーナーが判断する** — 却下 (オーナー判断 2026-09-29)。「見えているが短い」を実装の中で止められず、core/ADR-0044 の見直しの条件も数字で判定できない

## Risks / Trade-offs

- **描画の合図の取りこぼし**: 取りこぼすと、Dialog は上限なく待つので、出ないまま返らない。次の 3 つで塞ぐ
  - 印は stop・破棄でだけ下ろす
  - resume 時に描画を促す
  - pause だけからの復帰・背面からの復帰・作り直しをテストする
- **描画しない Activity**: 描画しない Activity (`Theme.NoDisplay` の中継用の画面など) は提示先にならない。そこで受け付けた Toast は、次に描画された画面に載るまで前面の待ちになる。見えていない画面に載せないという意味で正しい振る舞いだが、中継の画面の後に画面が開かなければ、前面を離れるまで待つ
- **起動画面の退場の演出の分の先走り**: 描画は起動画面の退場より前に来る。退場の演出の分 (数百 ms と推測) は、まだ起動画面の下で数えてしまう。Decision 7 で録画から測り、合格ライン (表示時間の 2/3) を割れば止めて再相談する (core/ADR-0044 の Revisit When)
- **iOS でシーンがつながる前に受け付けた Toast**: 背面とみなされ、受理の時点から数える。最初の画面の表示時の処理は、シーンがつながった後に走るので、この変更の目的の場面には当たらない
- **MAUI Android の中身の文脈**: 中身を作る画面の文脈 (MAUI 側の現在の Activity) が、画面の切り替えの途中で、Native の新しい提示先 (描画された Activity) と食い違う可能性が理屈上ある。Decision 7 で確かめる。食い違いが見つかったら、この change の中で扱うかをオーナーに諮る
- **前面の待ちの Toast は表示時間では終わらない**: Toast の契約の「保証すること」は、受理された表示が必ず結末 (表示されて消える・受理後の失敗・満了して破棄) に到達すると定める。前面の待ちの表示は、提示先が現れるか背面へ下がるまで期限が決まらないので、前面にいるのに提示先が現れない間 (起動画面を延ばし続けるなど) は表示リストに残る。結末に到達することは変わらないが、到達までの時間に表示時間という上限が無くなる (core/ADR-0043 の負の帰結)。契約の文は蒸留時に直す
- **公開済みベータの振る舞いの変更**: Android では起動直後・画面の切り替え直後に 3 機能が以前より遅れて出る。両 OS で、前面の待ちの間に受け付けた Toast は遅れて出て長く表示される

## Migration Plan

公開 API は変わらない。振る舞いの変更は、次の配布のリリースノートで利用者に知らせる (リリース手順の `## Changes` の下書き)。

## Open Questions

- 起動画面の退場の演出の分の先走りが、実測でどれくらいか (Decision 7)
- MAUI Android の最初の描画が、MAUI の画面の中身ができる前の空の画面になる構成があるか (あれば、中身の無い画面に載ることになる。Decision 7 で確かめる)

## ADR 候補

- Decision 1 → [core/ADR-0044](../../decisions/core/0044-android-host-is-drawn-resumed-activity.md) (proposed 起票済み)「Android の提示先は resumed で、かつ描画された Activity とし、Dialog・Loading・Toast の 3 機能で揃える」。3 機能の境界を越え、将来の提示先の判定を制約する
- Decision 2・3 → [core/ADR-0043](../../decisions/core/0043-toast-duration-from-visible-when-accepted-in-foreground-without-host.md) (proposed 起票済み。Decision 2 の範囲に合わせて 2026-09-29 に本文を改訂)「アプリが前面にいるのに提示先が無い間に受け付けた Toast は、画面が利用者に見えた時点から表示時間を数える」。全形態の Toast の契約を変え、accepted の core/ADR-0041 を一部改訂する
- Decision 4〜7 は変更の中の設計・検証の判断で、選別基準に当たらない
