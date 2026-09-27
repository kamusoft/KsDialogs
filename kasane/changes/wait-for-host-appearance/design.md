# Design: wait-for-host-appearance

## Context

方向は探索で決まっている ([exploration.md](exploration.md)・[proposal.md](proposal.md))。

- core/ADR-0039 (proposed): 提示先が無いまま呼ばれた Dialog・Loading・Toast は、全形態で失敗せず提示先の出現を待つ。待ちの上限は各機能の寿命に任せる
- maui/ADR-0006 (proposed): MAUI の Dialog の show に呼び出し元の打ち切りを足す

この design は、それを各形態でどう実現するかを決める。実現経路の調査 (2026-09-27、ksn-scout 2 本) で分かった現状は次のとおり。

- **iOS の提示先の供給**: 提示先を選ぶ規則は `ApplicationKeyWindowProvider.selectKeyWindow` (`ios/Sources/KsDialogs/Presentation/ApplicationKeyWindowProvider.swift:23-28`)。規則を持つ provider の protocol `DialogKeyWindowProvider` (`DialogKeyWindowProvider.swift:6-8`) は `keyWindow` の 1 要件だけで、3 機能の提示面 (Dialog は `UIKitDialogPresentationSurface.swift`、Loading・Toast は各 `*PresentationSurface.swift` の `KeyWindow*PresentationSurface`) がこれを使う。テストは**提示面ごと**差し替えて提示先の有無を切り替える (`DialogTestPresentationSurface`・`LoadingTestPresentationSurface`・`ToastTestPresentationSurface`・`LoadingStartFailureTests.swift:127` の `HostReadHookSurface`)
- **iOS で購読している通知**: `ToastCoordinator.swift:226-229` の `UIWindow.didBecomeKeyNotification` だけ。シーンの通知はどこも購読していない。最低 OS は iOS 17 (`ios/Package.swift`) なので、シーンの通知 (iOS 13 以降) は制約なく使える
- **Android の購読口**: Loading・Toast の提示面は `observeHostChange` を持ち、`ResumedActivityTracker` の入れ替わり購読を包む (`android/ksdialogs-core/src/main/kotlin/jp/kamusoft/ksdialogs/LoadingPresentationSurface.kt:26,40-51`・`ToastPresentationSurface.kt:25,39-50`)。通知は UI スレッドで、登録順に届く (`ResumedActivityTracker.kt:13,30,76-78`)。resume と destroy で通知し、pause では通知しない。Dialog の提示面 (`DialogPresentationSurface.kt:12-18`) はこの口を持たない
- **Dialog の show の流れ** (両 Native とも全入口が 1 か所に集まる)
  - iOS `DialogPresenter.swift`: レジストリ探索 (`:17-19`) → 提示先の判定 (`:38-40`) → 結果の通り道 (`:42`) → notifier の紐付け (`:48-55`、解除は `defer` `:58`) → 中身の生成 (`:60-62`) → 提示 (`:81-89`)。Task のキャンセル (`withTaskCancellationHandler` `:81-94`) は提示の部分だけを囲む
  - Android `DialogPresenter.kt`: factory 解決 (`:64`) → 提示先の判定 (`:66-68`) → 紐付け (`:75-77`) → 提示と View の生成 (`:79-85`) → 結果待ち (`:86-94`、打ち切りは `invokeOnCancellation`) → 解除 (`:95-99`)
  - 型指定 show の VM factory と configure は、どちらも Presenter より前に走る (iOS `Dialog.swift:84-88`、Android `Dialog.kt:56-62`)
- **中身の生成の時点**
  - iOS の Toast は受理のたびに中身を作り (`ToastCoordinator.swift:147`)、型指定経路の VM factory と configure もここで走る (`:277-280`)
  - iOS の Loading は開始時に中身を作り (`LoadingCoordinator.swift:129`)、失敗は開始の失敗になる
  - Android は、Toast も Loading も提示先を確保してから中身を作る。Loading で提示先が無いまま始まった表示は、提示先が現れた時点で初めて作る。そこで失敗したら警告を残して表示だけを諦める (`LoadingCoordinator.kt:452-466`)
- **MAUI**
  - Dialog の gateway は、ブリッジを呼ぶ前に MAUI の画面の文脈を解決し、取れなければ失敗する (`maui/KsDialogs.Maui/Platforms/iOS/PlatformDialogGateway.cs:33-34`、Android 同)
  - Loading・Toast の gateway は、Native から呼ばれる中身の供給の中で文脈を解決する (Android Loading `:140-141`、Toast `:75-76`、iOS Loading `:145-146`、Toast `:48-49`)
  - 文脈の解決 (`PlatformDialogContent.ResolvePresentationTarget`) は、「Native の提示先に対応する MAUI の画面」を探す。見つからなければ「文脈を持つ最初の画面」に落とす
- **MAUI のブリッジ**: 両 OS とも、Native の show を走らせた Task / Job を捨てて、閉じるだけのハンドル (`MauiDialogPresentation`) を返す。外から打ち切る手段は無い
  - Android: `maui/android/native/ksdialogs-maui-bridge/.../MauiDialogBridge.kt:47-57`。`CancellationException` は通知せずに投げ直す (`:89-90`)
  - iOS: `maui/macios/native/KsDialogsMauiBridge/MauiDialogBridge.swift:43-73`
  - 先例として、KMP の iOS には Task を保持して打ち切れるハンドル (`ios/Sources/KsDialogs/Interop/KsDialogsInteropShowHandle.swift`) がある
- **MAUI の打ち切りの先例**: 公開 API に `CancellationToken` を受けるものは無い。core の結果通知の契約では、呼び出し元の打ち切りの見え方が iOS (cancelled を返す) と Android・KMP (キャンセルの通知が伝播する) で分かれ、MAUI は「経路なし」(`kasane/concepts/core/api/result-notification-semantics.md:41-48,84-90`)
- **実アプリの起動順を再現する自動テストの手段は無い**
  - iOS は SwiftPM のテストだけで、ホストアプリも UI テストも無い。KMP の iosTest も提示先が常に無い (`kasane/handbook/cross/test-execution.md:121-123`)
  - 起動直後の画面が「非アクティブ」で始まり、あとから「アクティブ」が届くことは、前回の実測で分かっている (`kasane/changes/archive/2026-09-27-fix-ios-sample-demo-autoplay/evidence/autoplay-measurement.md`)。key 化の通知とアクティブ化の相対的な順序は未実測

## Goals / Non-Goals

**Goals**
- 3 機能とも、提示先が無い間は待ち、現れたら表示する。各機能の寿命 (Toast の表示時間、Loading の処理、Dialog の呼び出し元の待ち) が先に尽きたら、表示しない
- iOS の待ちの合図を、提示先の条件と同じ場所で持ち、3 機能で共用する
- iOS の中身の生成の時点を Android に揃え、MAUI の中身の供給が提示先の確保後に呼ばれるようにする
- MAUI の Dialog を、呼び出し元が打ち切れるようにする
- iOS・Android の「提示先が無い」エラー型を削除する

**Non-Goals**: proposal.md の Non-Goals のとおり。加えて、iOS の Loading・Toast の提示先の入れ替わり (別の window への載せ直し) は扱わない。iOS の window は回転で作り直されず、Android のような載せ直しの必要が確認されていない。

## Decisions

### Decision 1: iOS の待ちの合図は提示面の口として持ち、提示先を選ぶ規則の隣で 2 つの通知を見る

**採用案:**
- `DialogKeyWindowProvider` に「提示先が現れたかもしれない」ことの購読を足し、`ApplicationKeyWindowProvider` で実装する
  - 購読するのは `UIWindow.didBecomeKeyNotification` と `UIScene.didActivateNotification` の 2 つ
  - 購読者に知らせるのは合図だけ。受け取った側が `keyWindow` (提示先を選ぶ規則) をもう一度読んで判断する
- 3 機能の提示面の protocol (`DialogPresentationSurface`・`LoadingPresentationSurface`・`ToastPresentationSurface`) に同じ購読の口を足し、provider へ中継する。形は Android の `observeHostChange` (購読を返し、解除できる) に揃える
- `ToastCoordinator` の `NotificationCenter` の直接購読 (`:224-235`) は外し、この口に置き換える
- テスト用の提示面は、この口から合図を発火できるようにする。既存テストの「通知を手で post する」形 (`ToastContractTests.swift:235-238`) は、この発火に置き換える

**理由:**
- 今回の不具合の原因は、合図 (key 化の通知だけ) と提示先の条件 (前面でアクティブなシーンの key window) が別の場所にあってずれたことにある。合図を規則の隣に置けば、条件を変えるときに合図も一緒に目に入る
- 合図だけを送って判定を受け取り側に任せるので、通知の順序 (key 化が先か、アクティブ化が先か) に依存しない。未実測の順序が見立てと違っても、この設計は変わらない
- 口を提示面に置くのは、テストが提示面の単位で差し替えているため。provider だけに置くと、Presenter や Coordinator から届かず、テスト用の提示面からも発火できない。Android も口を提示面に置いている

**代替案:**
- **A: 各機能が `NotificationCenter` を直接購読する (今の Toast の形)** — 却下。合図と条件がまた別々の場所に散り、今回と同じずれを作りうる
- **B: 口を provider だけに置く** — 却下。Presenter・Coordinator は提示面しか持たず、テストの差し替え単位とも合わない
- **C: 待っている間、短い間隔で提示先を確かめ続ける** — 却下 (探索で却下済み)。確認が走り続け、ずれを直さず覆い隠す
- **D: 非アクティブでも前面のシーンなら提示先にする** — 却下 (探索で却下済み)。提示先の定義が変わり、Dialog にも波及する

### Decision 2: 中身は提示先を確保してから作る (iOS の Toast・Loading を Android に揃える)

**採用案:**
- **Toast (iOS)**: 中身は、取り付けの時点で作る。提示先があれば受理と同じターン、無ければ提示先が現れた時点になる。期限を過ぎた表示は、中身を作らずに捨てる。型指定経路の VM factory と configure も取り付けの時点で走る。つまり提示先が現れないまま満了した表示では、一度も呼ばれない (Android と同じ)
- **Loading (iOS)**
  - 開始時点で提示先があれば、今と同じく開始時点で中身を作る。失敗は開始の失敗になる
  - 開始時点で提示先が無ければ、中身の指定 (と、型指定・インスタンス渡しの VM) だけを控えて待つ。提示先が現れた時点で表示が続いていれば中身を作り、入りの演出から表示する
  - そこで中身の生成に失敗したら、警告を残して表示だけを諦める。合流状態は残り、処理はそのまま続く。呼び出し元へは返さない (Android `LoadingCoordinator.kt:452-466` と同じ)
  - 進捗の転送先 (VM の進捗受け口) は開始時点で控えるので、表示の前でも進捗は VM へ届く
- **Dialog**: Decision 3 のとおり、待ちの後に View を作る (両 Native とも、今も提示先の判定の後に作っている)

**理由:**
- MAUI の中身の供給は、MAUI の画面の文脈を必要とする。iOS が受理・開始の時点で作ると、MAUI の画面ができる前の呼び出しで供給が失敗する。そうなると Loading は開始が失敗し、Toast は捨てられる。どちらも core/ADR-0039 の「失敗せず待つ」に反する
- 提示先の確保後に作れば、MAUI の画面の文脈は用意されている見込みが高い。Native の提示先は、MAUI が画面ごとに作る Native の window (iOS) や Activity (Android) で、MAUI はそれを作る時点で画面に文脈を持たせる。ただし、この見込みは MAUI の内部の順序に依存し、リポジトリの中では確かめられない。そこで tasks 9.3 で MAUI の Sample の起動直後を実測する。見込みが外れた場合は、Decision 7 の失敗 (`InvalidOperationException`) になる
- Android はすでにこの形で、iOS を揃えると「中身の生成の時点」の承認済みの差 (`kasane/concepts/core/api/toast-semantics.md:63`) が消える
- 提示先が無いまま始まった Loading の生成失敗を「表示だけを諦める」とすることは、core/ADR-0033 (Loading の失敗の合流先 = 開始の失敗) の一部を置き換える。core/ADR-0040 (proposed、ADR-0033 の amends) に記録した
- Loading で提示先があるときの開始時の失敗を保つので、構成ミスは今までどおり呼び出し元に返る。変わるのは、提示先が無いまま始まった表示だけ

**代替案:**
- **A: iOS は受理・開始の時点で作ったまま、MAUI の失敗は許す** — 却下。MAUI の画面ができる前の呼び出しで Loading の開始が失敗し、Toast が捨てられる。失敗の型が「提示先が無い」になり、MAUI のエラー型を素の .NET に限る決定 (探索の決定事項) とも合わない
- **B: MAUI 側で画面の文脈が取れるまで待ってから Native を呼ぶ** — 却下。Native の待ちと MAUI の待ちが二重になる。Toast の計時や Loading の処理の開始が MAUI の待ちに引きずられ、Native の契約と食い違う

### Decision 3: Dialog は紐付けの後、中身の生成の前で待ち、待ちは 3 つのどれかで明ける

**採用案:**
- 両 Native の Presenter の「提示先の判定」の位置で待つ。ただし待ちは、notifier の紐付けの後に置く
  - iOS: 判定 (`DialogPresenter.swift:38-40`) を紐付け (`:48-55`) の後ろへ移し、待ちに置き換える
  - Android: 判定 (`DialogPresenter.kt:66-68`) を紐付け (`:75-77`) の後ろへ移し、待ちに置き換える
- 待ちが明けるのは次のどれか。明けたら、それぞれ次のように進む
  - **提示先が現れた**: 提示先の条件を読み直す。満たしていれば中身を作って提示する。満たしていなければ待ちに戻る
  - **呼び出し元が打ち切った**: 一度も表示せずに終える。iOS は `.cancelled` を返し、Android はキャンセルの通知が伝播する (結果通知の既存ルール「呼び出し元の打ち切り」)
  - **待っている間に結果が確定した** (VM が notifier で報告した): 表示せずに、その結果を返す
- 待っている間も紐付けは有効なので、同じ VM インスタンスの再 show は、今までどおり「表示中」として失敗する
- レジストリの探索 (未登録の失敗) と、型指定 show の VM factory・configure は、今までどおり待ちより前に走る

**理由:**
- 呼び出し元から見ると、待っている Dialog は「表示中の show」と同じく呼び出しの途中にある。紐付けの後に待てば、同じ VM の二重 show の検出が、提示先の有無に左右されない
- 紐付けの後に待つと、VM が表示の前に報告できてしまう。そこで「結果の確定」も明ける条件に入れて、表示されないまま結果が返るようにする。紐付けの前に待つと、表示前の報告は紐付いていない notifier に届き、その後で表示された Dialog が閉じられなくなる
- 中身 (View) は待ちの後に作るので、Decision 2 の「提示先の確保後に作る」が Dialog でも成り立つ (MAUI の中身の供給も待ちの後に呼ばれる)
- core/ADR-0018 (notifier は show 時にサイドテーブルで紐付ける) の「show 時」の範囲に待ちが入るだけで、紐付けの仕組みは変わらない

**代替案:**
- **A: 紐付けの前で待つ** — 却下。待っている間の同じ VM の再 show が検出されず、明けたあとに片方が「表示中」で失敗する。検出の時点が提示先の有無で変わる。表示前の VM の報告も行き場を失う
- **B: 型指定 show の VM 生成と configure も待ちの後に回す** — 却下。VM の生成は呼び出しの文脈 (UI スレッドの受理順) に属し、提示先とは関係が無い。未登録の失敗が待ちの前に返る今の順序とも揃わなくなる

### Decision 4: 待っている Dialog は、Presenter が呼んだ順の列で持ち、1 枚ずつ明ける

**採用案:**
- Presenter が、提示先を待っている Dialog の列を、呼んだ順に持つ。提示面の口の購読は、列が空でない間だけ 1 本張る
- 合図を受けたら、提示先の条件を読み直す。満たしていれば、列の先頭の 1 枚だけを明ける
- 明けた 1 枚の提示が終わったら、条件をもう一度読み、満たしていれば次の 1 枚を明ける。列が空になるまで続ける
  - 「提示が終わった」は、iOS では提示面の提示の完了通知 (UIKit の present の完了。提示面の `present` に完了通知を足す)、Android では Dialog のウィンドウを追加した後とする
- 打ち切りや結果の確定で終わった 1 枚は、順番を待たずにその場で列から外す
- 列に待っている Dialog があるうちに新しく show が呼ばれたら、提示先があってもその後ろに並ぶ (先に待っている Dialog を追い越さない)
- 結果として、後から呼んだものが手前に重なる (多段表示の契約「後から出したものが手前」)

**理由:**
- 表示の順番を、実行環境のスケジューリング (Swift の MainActor での再開順、Android の通知順と UI スレッドへの配送順) に任せず、列で明示する。そのため、提示の順序を設計の上で保証できる (second-opinion-spec-001 Major 3 の指摘で見直し)
- 前の 1 枚の提示の完了を待ってから次を明けるので、UIKit の提示の途中に次の提示を重ねない
- 多段表示の契約と、提示先がある場合の重ね出しと同じ見え方になる。列は OS に渡す前の待ちで、渡したあとの重なりは OS に任せたままなので、core/ADR-0006 (スタックを管理しない) の決定とは衝突しない (探索で確認済み)

**代替案:**
- **A: 最後に呼んだ 1 枚だけを出し、それより前のものは cancelled で終える** — 却下。利用者の操作なしに結果が確定し、「各 show は独立に結果を返す」に反する
- **B: 前の 1 枚が閉じてから次を出す** — 却下。提示先がある場合の重ね出しと見え方が変わる。ライブラリが重なりを管理することになり、core/ADR-0006 の却下案 (明示スタック管理) に近づく
- **C: 列を持たず、各 Dialog が合図を受けた順に自分で待ちを明ける** — 却下。表示の順番が実行環境のスケジューリングに依存し、設計の上で保証できない。UIKit の提示の途中に次の提示を重ねうる

### Decision 5: Android の Dialog の提示面に、Loading・Toast と同じ入れ替わりの購読口を足す

**採用案:**
- `DialogPresentationSurface` に `observeHostChange` を足す
- `ActivityDialogPresentationSurface` のコンストラクタに `changeObserver: ResumedActivityChangeObserver = ResumedActivityTracker.shared` を足す (Loading・Toast と同じ既定引数)
- 提示の時点で提示先が無ければ、例外を投げない。Presenter が待つので、そこに来るのは提示先がある場合だけになる。来てしまった場合は、内部の不整合として扱う
- テスト用の提示面 (`src/test/.../support/DialogTestPresentationSurface.kt`、`src/androidTest/.../support/RecordingDialogPresentationSurface.kt`) に、提示先の出現を発火する口を足す

**理由:** Loading・Toast ですでに使っている仕組みで、通知のスレッドと順序の性質 (UI スレッド・登録順) も確かめてある。新しい購読の仕組みを作らない。

**代替案:**
- **A: Presenter が `ResumedActivityTracker` を直接購読する** — 却下。提示面を差し替えるテストから発火できない。Loading・Toast と形が揃わない

### Decision 6: MAUI の打ち切りは全入口の末尾の `CancellationToken` で受け、打ち切られたら `OperationCanceledException` を投げる

**採用案:**
- `IKsDialog` の `ShowAsync` 7 本 (`maui/KsDialogs.Maui/Presentation/IKsDialog.cs:39,67,89,116,133,148,161`) の末尾に `CancellationToken cancellationToken = default` を足す
- 呼び出し元が打ち切った場合の振る舞い
  - 待っている間なら、一度も表示しない。表示中なら Dialog を閉じる。どちらも、内部の結果は cancelled で確定する
  - `ShowAsync` は `OperationCanceledException` を投げる
  - 呼び出しの時点で打ち切り済みなら、Dialog を作らずに投げる
- 中継
  - C# の gateway で token に登録し、ブリッジのハンドルの打ち切りを呼ぶ
  - gateway は UI スレッドへ移ってからハンドルを得る。その間に打ち切られた場合は、打ち切りを覚えておく。ハンドルを得る前ならブリッジを呼ばずに終え、得た直後ならすぐにハンドルを打ち切る。打ち切りの登録と、ハンドルの受け渡しは 1 か所で直列にする (second-opinion-spec-001 Minor の指摘)
  - ブリッジのハンドル (`MauiDialogPresentation`) に打ち切りを足す
    - iOS: show を走らせた Task を保持し、`cancel()` で打ち切る。Native は `.cancelled` を返すので、閉鎖通知は cancelled で届く。ObjC への公開と、binding の定義 (`maui/macios/KsDialogs.Binding.iOS/ApiDefinition.cs`) にも足す
    - Android: Job を保持し、`cancel()` で打ち切る。打ち切られたときは、閉鎖通知の cancelled を送ってからキャンセルを投げ直す。今は通知せずに投げ直すので (`MauiDialogBridge.kt:89-90`)、C# 側の完了が宙に浮く
  - KMP の iOS の打ち切りハンドル (`KsDialogsInteropShowHandle.swift`) と同じ形にする
- 待っている間に結果が確定した場合 (Decision 3) は、C# 側からはブリッジの「閉じる」(`MauiDialogPresentation.dismiss()`) になる。今の「閉じる」は、中身の紐付け前に呼ばれると印だけを立て、紐付けた時点ですぐ閉じるので、一度だけ表示されうる。そこで、紐付け前に呼ばれた「閉じる」は、中身を作らずに Native の show を止める形に変える (両 OS のブリッジ)

**理由:**
- `CancellationToken` を受ける .NET の非同期メソッドは、打ち切られたら `OperationCanceledException` を投げるのが慣習。core の見え方の表でも、Android・KMP は「キャンセルの通知が伝播する」側で、MAUI をこちらに揃える
- 省略可能な末尾の引数なので、今の呼び出しはソースのまま通る。名前付き引数の負の検査 (`options:` / `transition:`) とも衝突しない

**代替案:**
- **A: 打ち切られたら `Cancelled` の結果を返す (iOS と同じ見え方)** — 却下。token を受ける .NET の非同期メソッドの慣習と違い、呼び出し元の `catch (OperationCanceledException)` や `Task.WhenAny` などの組み合わせが働かない
- **B: 打ち切りのための別の入口 (例: 打ち切り可能な表示オブジェクトを返す) を足す** — 却下。入口が倍になり、ほかの形態 (呼び出しの打ち切りで止める) とも形が揃わない

### Decision 7: MAUI の gateway は、提示先を事前に判定せず、中身の供給の時点で画面の文脈を解決する

**採用案:**
- Dialog の gateway の事前判定 (`PlatformDialogGateway.cs:33-34`、両 OS) を外す。MAUI の画面の文脈は、Native が呼ぶ中身の供給の中で解決する (Loading・Toast と同じ形)
- 中身の供給で文脈が取れなかった場合は、中身の生成の失敗として扱う
  - 投げる例外は、理由の文言つきの `InvalidOperationException` にする (`DialogException.PresentationHostUnavailable` は使わない)
  - Native の提示先を確保した後なので、本来は起きない。KsDialogs の Activity の追跡と MAUI の `ActivityStateManager` がずれた場合などの防御にあたる
  - Loading・Toast の供給の中の同じ判定 (Android Loading `:140-141`、Toast `:75-76`、iOS Loading `:145-146`、Toast `:48-49`) も、この扱いに揃える
- 素の .NET (`HostlessDialogGateway`) は、今までどおり `DialogException.PresentationHostUnavailable` で即座に失敗する

**理由:**
- 事前判定があると、MAUI の画面ができる前の呼び出しが、Native の待ちに届く前に失敗する
- 事前判定で捕まえた文脈を供給まで持ち回ると、待ちの後に別の画面になった場合に古い文脈を使う
- 供給の時点で解決すれば、Native の提示先に対応する画面の文脈が取れる
- `DialogException.PresentationHostUnavailable` を「提示の仕組みそのものが無い環境」(素の .NET) に限る決定 (探索の決定事項) を守る

**代替案:**
- **A: 事前判定を残し、失敗の代わりに MAUI の画面ができるまで待つ** — 却下。Decision 2 の代替案 B と同じく、待ちが二重になる
- **B: 供給の失敗にも `PresentationHostUnavailable` を使う** — 却下。探索で、この型を素の .NET の失敗に限ると決めている

### Decision 8: 「提示先が無い」の失敗の運搬路を、Native から MAUI のブリッジまでまとめて外す

**採用案:**
- 公開 API から削除する: iOS `DialogError.presentationHostUnavailable` (`ios/Sources/KsDialogs/Contract/DialogError.swift:9,29-30`)、Android `DialogException.PresentationHostUnavailable` (`DialogException.kt:55-57`)
- 失敗を運んでいた内部・輸送層の経路も外す
  - MAUI の iOS ブリッジの閉鎖種別 `presentationHostUnavailable` (`maui/macios/native/KsDialogsMauiBridge/MauiDialogClosure.swift:11`、C# `StructsAndEnums.cs:16`、受け取り `PlatformDialogGateway.cs:75-76`)
  - MAUI の Android ブリッジの `MauiDialogClosureListener.onPresentationHostUnavailable` (`MauiDialogClosureListener.kt:20`、送り `MauiDialogBridge.kt:87-88`、受け取り `PlatformDialogGateway.cs:94-95`)
- KMP 向けの Swift の型付き入口で、理由の欠けた結果のフォールバックに使っている箇所 (`ios/Sources/KsDialogs/Kmp/KsDialogsKmpError.swift:34`) は、別の値に置き換える。値は実装時に確定し、テストで固定する
- 診断文言の検査 (`DiagnosticMessageTests.swift:21` の DM-IO-01、Android `DialogExceptionMessageTests.kt:45-49` の DM-AN-01) から、該当の行を外す
- MAUI の `DialogException.PresentationHostUnavailable` は残し、doc コメントを「この環境には提示の仕組みが無い (素の .NET)」の意味に直す

**理由:** Native が報告しなくなった失敗の通り道を残すと、どこからも届かない分岐が残る。輸送層 (ブリッジと binding) は利用者向けの面ではないので、公開 API の削除と同じ change でまとめて外す。

**代替案:**
- **A: ブリッジの閉鎖種別は残す (将来の再利用に備える)** — 却下。届かない分岐と、それを検査できないテストが残る。必要になれば足せる

### Decision 9: 「即失敗」を判定材料にしていたテストは、打ち切りと二重 show で判定し直す

**採用案:**
- KMP の DM-KM-01〜03・PB-KT-09 (iosTest `InteropBridgeContractTests.kt:78-150`)、DM-KM-02・MB-KM-02 (androidHostTest `AndroidDialogGatewayContractTests.kt:157-178`・`KmpViewModelSupplyTests.kt:34-65`) は、「提示先が無いので必ず失敗する」ことを、登録済みの factory を引き当てた印にしている
- 待つ方式では、登録済みの側が返らなくなる。そこで次の手で判定し直す
  - 登録済み: show を始めて、失敗せずに待っていることを確かめてから打ち切る。iOS は互換面のハンドルの `cancel()` で Cancelled、Android はコルーチンの打ち切りでキャンセルの通知
  - 未登録: 今までどおり、待ちの前にすぐ失敗する
  - 解決の成否は、この 2 つの違いで判定する
- 型指定 show の VM factory が呼ばれたこと (PB-KT-09・MB-KM-02) は、待ちの前に走るので、今までどおり確かめられる
- 提示先の無いテストランナーで Toast・Loading の factory が呼ばれることを判定材料にしている 4 本は、Decision 2 で factory が呼ばれなくなるので、判定材料を次のように置き換える (second-opinion-spec-001 Major 4 の指摘で、実装時の判断から design の決定に上げた)
  - iosTest `InteropToastBridgeContractTests.kt:48` の TS-KM-02 と `:83` の PB-KT-09 (Toast): 共有 VM の型キーで Native のレジストリを引き当てたことを、互換面の Toast の show の同期の結果で判定する。互換面の show は、未登録なら同期にエラーを返し (`ios/Sources/KsDialogs/Interop/KsDialogsInteropToastBridge.swift:76-93`)、登録済みならエラーを返さない
  - iosTest `InteropLoadingBridgeContractTests.kt:70` の LD-KM-03 と `:113` の PB-KT-09 (Loading): 開始が失敗しないこと (未登録は開始の時点で失敗する。loading-contract デルタ) と、表示の前に報告した進捗が VM の受け口へ届くこと (LD-HW-05) で判定する
  - 引き当てた factory から中身が作られること、中身に共有 VM そのものが渡ることは、KMP の iosTest では確かめない。Native のテスト (TS-HW-01・LD-HW-01) と、KMP の iOS Sample の `custom-toast`・`custom-loading` の自動再生 (tasks 9.3) で確かめる。KMP の gateway は VM を包み直さずに渡す (`IosLoadingGateway.kt` の説明) ので、この分担で落ちる検査は無い見込み
- MAUI の Android ブリッジの `MauiDialogClosureReportTests.kt:115-135` (「提示先不在はそれと分かる形で通知される」) は削除する。同じファイルの `:105-113` (「コルーチンのキャンセルは通知に変換されずに伝播する」) は、Decision 6 の「通知してから投げ直す」に合わせて改訂する

**理由:** 検査したいのは「登録済みの factory を引き当てたか」で、「提示先が無いと失敗するか」ではない。待つ方式でも、打ち切りと未登録の失敗の違いで同じことを判定できる。

**代替案:**
- **A: これらのテストの中で、提示先を用意する** — 却下。KMP の iosTest と androidHostTest には `UIApplicationMain` もホストの Activity も無い。互換面から提示面を差し替える口も無い (Native の `Dialog` の面を渡すコンストラクタも、`ResumedActivityTracker` も internal)
- **B: 互換面にテスト用の提示面の差し替え口を足す** — 却下。共有コードのテストから使うには ObjC に公開する必要があり、出荷する framework にテスト専用の面が載る

### Decision 10: 起動順の実測は一時的な診断出力で行い、回帰の観測点は Sample の自動再生にする

**採用案:**
- **直す前の実測**: 前回の実測の手順 (`simctl launch --demo` と連続撮影) で、起動直後の呼び出しで 3 機能が出ないことを確かめる。あわせて、key 化の通知・シーンのアクティブ化・提示先の条件が満たされた時点の順序を観測する
  - 順序は、一時的な診断出力で観測し、観測後に取り除く。前回の change と同じ手順
  - シーンが一時的に非アクティブになる場面 (システムの許可ダイアログなど) も、同じ方法で観測する
  - 今の iOS Sample と KMP の iOS Sample は「シーンがアクティブになってから再生」する待ちを持つ (`samples/ios/KsDialogsSample/SampleMenuScreen.swift:93-99`、`samples/kmp/iosApp/KsDialogsSampleKmp/SampleMenuScreen.swift:94`)。この待ちを、ライブラリを直す前に外す (Sample の変更はこの change に含める。オーナー判断 2026-09-27)。外した Sample は、直す前は前回の実測と同じく 3 機能が出ない状態を再現し、直した後は回帰の観測点になる。ソースを一時改変せずに撮れる
- **直した後の確認**: 同じ手順で、3 機能が表示されることを確かめる
- **自動テスト**: 待ちと解除は、テスト用の提示面から合図を発火して単体テストで確かめる (Decision 1)。実アプリの起動順そのものは自動テストにできない (Context)

**理由:** 実アプリの起動順を再現する自動テストの手段が無い。そのため、前回と同じ半自動の手順が最も確実で、証跡の形も揃う。

**代替案:**
- **A: iOS のテストホストアプリや UI テストの target を新設する** — 却下。この change の規模に対して、配線と CI の追加が大きい。単体テストで待ちの仕組みを押さえ、実アプリの起動順は実測で押さえれば足りる

## Risks / Trade-offs

- **UIKit の連続提示**: Decision 4 で、前の 1 枚の提示の完了を待ってから次を明けるので、UIKit の提示の途中に次を重ねることは設計の上で起きない。実 UIKit で 2 枚が順に重なることは、tasks 5.4 で確かめる
- **KMP の iosTest の検査範囲の縮小**: Decision 9 で、Toast・Loading の factory が共有 VM で呼ばれることを iosTest では確かめなくなる。Native のテストと KMP の iOS Sample の実測で補う
- **見立ての外れ**: iOS の起動順とシーンの一時的な非アクティブの順序は未実測。ただし Decision 1 は順序に依存しない。見立てが外れて、2 つの通知のどちらも来ない状況が見つかったら、design に戻る
- **Loading の失敗の見え方の変化 (iOS)**: 提示先が無いまま始まった表示の中身の生成の失敗は、開始の失敗ではなく、警告と表示の諦めになる。Android と同じだが、iOS の利用者から見ると、その場合だけ失敗が返らなくなる
- **Toast の VM factory が呼ばれない場合が増える (iOS)**: 提示先が現れないまま満了した表示では、型指定経路の VM factory と configure が一度も呼ばれない。Android と同じで、承認済みの差が消える
- **テストの作り直しの量**: KMP の 11 本 (Dialog 7 本・Toast と Loading 4 本)、MAUI ブリッジの 2 本、各 Native の即失敗・保留のテスト
- **返らない Dialog**: 画面が現れない場所から呼んだ Dialog は返らない (core/ADR-0039 の負の帰結)。呼び出し元の打ち切りで防ぐ

## Migration Plan

- 利用者のコードの移行
  - `DialogError.presentationHostUnavailable` / `DialogException.PresentationHostUnavailable` (iOS・Android) を参照している箇所を消す。Dialog は失敗しなくなるので、代わりの処理は要らない
  - 返らない Dialog を避けたい呼び出しには、打ち切りを付ける (iOS は Task のキャンセル、Android・KMP はコルーチンの打ち切り、MAUI は `CancellationToken`)
- リリースの案内: 次のリリースの `## Changes` に、破壊的変更 (型の削除・Dialog が待つようになること・iOS の Loading・Toast が遅れて出ること) と、MAUI の `CancellationToken` の追加を書く。下書きは release スキルが作る
- データや設定の移行は無い

## Open Questions

なし。

- 解決済み: Sample の自動再生の待ちを外すか → 外す (オーナー判断 2026-09-27)。最初の画面の表示時の処理から呼ぶ形に戻し、Decision 10 の再現と回帰の観測点にする

## ADR 候補

- **core/ADR-0039** (proposed、起票済み): 提示先が無いまま呼ばれた Dialog・Loading・Toast は、全形態で失敗せず提示先の出現を待ち、待ちの上限は各機能の寿命に任せる — Decision 2〜5・7 の前提。蒸留時に、実装の結果に合わせて見直してから昇格する
- **maui/ADR-0006** (proposed、起票済み): MAUI の Dialog の show に呼び出し元の打ち切りを足し、待っている Dialog と表示中の Dialog をコードから止められるようにする — Decision 6。打ち切りの見え方 (`OperationCanceledException`) は、ADR が設計に委ねた範囲の決定で、ADR の本文には写さない
- **core/ADR-0040** (proposed、起票済み。core/ADR-0033 の amends): Toast・Loading の中身は全形態で提示先を確保してから作り、提示先が無いまま始まった Loading の生成失敗は表示だけを諦める — Decision 2。提案レビュー (second-opinion-spec-001 Major 1) で ADR-0033 との衝突が指摘され、オーナーが改訂を選んだ (2026-09-27)。蒸留時に昇格し、ADR-0033 に `amended-by: 0040` と index の「一部改訂」を書く
