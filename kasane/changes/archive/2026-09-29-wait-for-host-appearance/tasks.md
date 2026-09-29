# Tasks: wait-for-host-appearance

対応の表記: (→ <能力>: <Requirement 名の要点> / <Scenario ID>)。design の Decision 番号は (D<N>)。

テストの実行手順は `kasane/handbook/cross/test-execution.md` に従う。Scenario ID はテストの宣言 (名前・直前の属性) に含める (core/ADR-0016)。

## 1. Sample の待ちを外し、直す前の再現を撮る

- [x] 1.1 iOS Sample (`samples/ios/KsDialogsSample/SampleMenuScreen.swift:93-99`) と KMP の iOS Sample (`samples/kmp/iosApp/KsDialogsSampleKmp/SampleMenuScreen.swift:94` 付近) の自動再生から、シーンの状態を待つ処理を外し、最初の画面の表示時の処理から始める形に戻す。1 回限りの取り出しと、再生中のデモを画面の状態の変化で打ち切らない形は保つ。コメントの「ライブラリは…より前に再生すると失敗する」の説明も外す (→ samples: iOS 系 Sample の自動再生は最初の画面の表示時に始める / CA-SA-09)
- [x] 1.2 **直す前の実測** (ライブラリはまだ変えない): 1.1 の Sample 2 つで `basic-dialog`・`default-loading`・`custom-loading`・`default-toast` を自動再生し、3 機能が出ない (Dialog は失敗する) ことを、前回の手順 (`kasane/changes/archive/2026-09-27-fix-ios-sample-demo-autoplay/evidence/autoplay-measurement.md`) で撮る。証跡は `evidence/` に置く (D10)
- [x] 1.3 一時的な診断出力で、起動直後の「window が key になった通知」「シーンがアクティブになった通知」「提示先の条件が満たされた時点」の順序を観測する。シーンが一時的に非アクティブになる場面 (システムの許可ダイアログなど) の最中と前後も観測する。観測が終わったら診断出力を取り除く。**2 つの通知のどちらも来ないまま提示先の条件が満たされる場面が見つかったら、実装に進まずに報告する** (D1 の前提が崩れるため) (D10)

## 2. iOS: 提示先の出現の合図 (D1)

- [x] 2.1 `DialogKeyWindowProvider` に合図の購読 (購読を返し、解除できる形) を足し、`ApplicationKeyWindowProvider` で `UIWindow.didBecomeKeyNotification` と `UIScene.didActivateNotification` から合図を送る。テスト用の `DialogTestKeyWindowProvider` にも発火口を足す (→ ios-native: 提示先の出現の合図は、提示先を選ぶ規則の隣で 2 つの通知から作る)
- [x] 2.2 3 機能の提示面の protocol (`DialogPresentationSurface`・`LoadingPresentationSurface`・`ToastPresentationSurface`) に同じ口を足して provider へ中継する。テスト用の提示面 4 つ (`DialogTestPresentationSurface`・`LoadingTestPresentationSurface`・`ToastTestPresentationSurface`・`LoadingStartFailureTests.swift` の `HostReadHookSurface`) に、合図を発火する口を足す (→ 同上)
- [x] 2.3 テスト: PB-HI-01 (2 つの通知で合図が届く・解除後は届かない)、PB-HI-02 (条件を満たさない合図では待ち続け、満たした合図で 3 機能が表示される)、PB-HI-03 (待っている表示が無くなると購読が解除される) (→ 同上 / PB-HI-01〜03)

## 3. iOS: Toast (D1・D2)

- [x] 3.1 `ToastCoordinator` の `NotificationCenter` の直接購読 (`ToastCoordinator.swift:224-235`) を外し、提示面の口で待つ。中身の生成 (型指定経路の VM factory・configure を含む) を、受理の時点から取り付けの時点へ移す (`ToastDisplay` は中身ではなく中身の指定を持つ)。期限を過ぎた表示は中身を作らずに捨てる (→ toast-contract: Toast の中身は取り付けの時点で作る)
- [x] 3.2 テスト: TS-HW-01〜03。`ToastContractTests.swift:224`・`:244`・`:255` の 3 本を ID つきに置き換え、通知を手で送る形を提示面の発火に置き換える。TS-HW-02 は型指定経路で VM factory・configure・View factory が呼ばれないことまで確かめる (→ toast-contract / TS-HW-01〜03)

## 4. iOS: Loading (D1・D2)

- [x] 4.1 `LoadingCoordinator` で、View factory の登録の解決は開始時点で行ったうえで (未登録は開始の失敗)、開始時点で提示先が無ければ中身の指定と VM (進捗の受け口) だけを控え、提示面の口で待つ。提示先が現れた時点で表示が続いていれば中身を作って入りのフックから表示し、生成に失敗したら警告ログを残して表示だけを諦める。合流の最後の終了と hide で待ちを解除する。開始時点で提示先があれば、今と同じく開始時点で作り、失敗は開始の失敗にする (→ loading-contract: 提示先が無いまま始まった Loading の表示)
- [x] 4.2 テスト: LD-HW-01〜07。`LoadingCoalescingTests.swift:390` の LD-CO-14 は、action が実行されることの検査を残し、「表示されない」の期待を「提示先が現れるまでは表示されない」に見直す (→ loading-contract / LD-HW-01〜07)

## 5. iOS: Dialog (D3・D4・D8)

- [x] 5.1 `DialogPresenter` の提示先の判定 (`DialogPresenter.swift:38-40`) を、notifier の紐付け (`:48-55`) の後ろへ移して待ちに置き換える。待ちは、提示先の出現 (条件を読み直す)・Task のキャンセル・結果の確定のどれかで明ける。明けた直後にキャンセル済みでないかを確かめてから中身を作る。待っている Dialog は Presenter が呼んだ順の列で持ち、先頭の 1 枚ずつ明ける。次の 1 枚は、前の 1 枚の提示の完了通知 (提示面の `present` に足す) の後に明ける。列があるうちの新しい show は後ろに並ぶ (D4) (→ dialog-contract: 呼び出しコンテキストの契約 / 提示先の出現を待つ Dialog の結末)
- [x] 5.2 `DialogError.presentationHostUnavailable` を削除し、診断文言と doc コメントを直す。`KsDialogsKmpError.swift:34` の理由の欠けた失敗のフォールバックを別の値に置き換え、テストで固定する。MAUI の iOS ブリッジの参照 (`maui/macios/native/KsDialogsMauiBridge/MauiDialogBridge.swift:66-67`) は 8.3 と同時に直す (→ ios-native: 提示先が無いことを表す失敗は公開 API に無い / iOS の失敗型メッセージは英語固定)
- [x] 5.3 テスト: PB-HW-01〜08 (iOS)、PB-HI-04 (`DialogError` を網羅する `switch`)、DM-IO-01 (6 case)。`DialogCallContextTests.swift:57` (即失敗) を PB-HW-01 に、`KsDialogsKmpFacadeTests.swift:240-249` を待ちと打ち切りの検査に置き換える (→ dialog-contract・ios-native / PB-HW-01〜08・PB-HI-04・DM-IO-01)
- [x] 5.4 **実 UIKit での連続提示の確認** (design Risks): 待っていた 2 枚が、実 UIKit の上で順に表示され、後の 1 枚が手前に重なることを確かめる。手段は実装時に選ぶ (候補: MAUI の iOS ブリッジのテストホスト `maui/macios/native/KsDialogsMauiBridgeTestHost` の実 UIKit 上のテスト)。確かめた手段と結果を `evidence/` に残す (→ dialog-contract / PB-HW-06)

## 6. Android (D3・D4・D5・D8)

- [x] 6.1 `DialogPresentationSurface` に `observeHostChange` を足し、`ActivityDialogPresentationSurface` のコンストラクタに `changeObserver: ResumedActivityChangeObserver = ResumedActivityTracker.shared` を足す。提示の時点で提示先が無くても `DialogException` を投げない。テスト用の提示面 (`src/test/.../support/DialogTestPresentationSurface.kt`・`src/androidTest/.../support/RecordingDialogPresentationSurface.kt`) に出現の発火口を足す (→ android-native: Dialog の提示面は、Loading・Toast と同じ入れ替わりの購読口を持つ)
- [x] 6.2 `DialogPresenter.presentWithFactory` の提示先の判定 (`DialogPresenter.kt:66-68`) を、紐付け (`:75-77`) の後ろへ移して、取り消し可能な待ちに置き換える。明け方と列の持ち方は 5.1 と同じ (次の 1 枚は、前の 1 枚の Dialog のウィンドウを追加した後に明ける) (→ dialog-contract: 呼び出しコンテキストの契約 / 提示先の出現を待つ Dialog の結末)
- [x] 6.3 `DialogException.PresentationHostUnavailable` を削除し、KDoc (`DialogPresenter.kt:18,38` ほか) を直す。MAUI の Android ブリッジの参照は 8.4 と同時に直す (→ android-native: 提示先が無いことを表す例外は公開 API に無い / Android の失敗型メッセージは英語固定)
- [x] 6.4 テスト: PB-HW-01〜08 (Android)、PB-HA-01〜03、DM-AN-01 (4 サブクラス)。`DialogCallContextTests.kt:45`・`DialogInlineShowTests.kt:192` (即失敗) を待ちの検査に置き換える (→ dialog-contract・android-native / PB-HW-01〜08・PB-HA-01〜03・DM-AN-01)
- [x] 6.5 Android の Loading・Toast に、LD-HW-01〜07・TS-HW-01〜03 の同名テストを置く。実装は変えない見込み。テストが通らない (Android の今の挙動が契約と違う) 場合は、実装を変える前に報告する (→ loading-contract・toast-contract / LD-HW-01〜07・TS-HW-01〜03)

## 7. KMP (D9)

- [x] 7.1 iosTest の `InteropBridgeContractTests.kt` の DM-KM-01 (`:78`)・DM-KM-03 (`:103`)・PB-KT-09 (`:133`) を、「待っていることを確かめてから打ち切り、Cancelled / キャンセルの通知で終わる」判定に書き直す。同じ手口の `InteropPlacementTransportTests.kt:52-66` も確かめて、必要なら直す (→ kmp-facade: KMP iOS ホストの gateway 固有メッセージは英語固定 / 共有コードの型指定 show / DM-KM-01・DM-KM-03・PB-KT-09)
- [x] 7.2 androidHostTest の `AndroidDialogGatewayContractTests.kt` の DM-KM-02 (`:157-178`)・PB-KT-09 と、`KmpViewModelSupplyTests.kt` の MB-KM-02 (`:34-65`) を同じ手で書き直す。DM-KM-02 の未登録の側は、`ViewFactoryNotRegistered` の文言と cause で確かめる (→ kmp-facade: KMP Android ホストは Native の英語文言を素通しする / DM-KM-02・PB-KT-09・MB-KM-02)
- [x] 7.3 テスト: PB-KC-04 (両 OS。待っている Dialog を共有コードで打ち切ると、表示されずにキャンセルが伝播し、iOS では互換面のハンドルの打ち切りが呼ばれる) (→ kmp-facade: 共有コードから、待っている Dialog を打ち切れる / PB-KC-04)
- [x] 7.4 提示先の無いテストランナーで Toast・Loading の factory が呼ばれることを判定材料にしている iosTest 4 本 (`InteropToastBridgeContractTests.kt:48` の TS-KM-02・`:83` の PB-KT-09、`InteropLoadingBridgeContractTests.kt:70` の LD-KM-03・`:113` の PB-KT-09) を、design Decision 9 の判定材料で書き直す。Toast は互換面の show の同期の結果 (未登録はエラー、登録済みはエラーなし)、Loading は開始が失敗しないことと表示の前の進捗が VM の受け口へ届くことで判定する (→ kmp-facade: 共有コードの型指定 show ほか既存の TS-KM-02・LD-KM-03 / TS-KM-02・LD-KM-03・PB-KT-09)

## 8. MAUI (D6・D7・D8)

- [x] 8.1 `IKsDialog` の `ShowAsync` 7 本と実装 (`Presentation/Dialog.cs`・`Internals/DialogPresenter.cs`) に `CancellationToken cancellationToken = default` を末尾に足し、gateway まで引き回す (`IDialogGateway.PresentAsync` の引数か `DialogPresentationRequest` に載せる)。呼び出しの時点で打ち切り済みなら、登録の解決・VM の生成・gateway の呼び出しをせずに `OperationCanceledException` を投げる。打ち切りで終わった show は `OperationCanceledException` を投げる (→ maui-binding: Dialog の show は呼び出し元の打ち切りを受け付ける)
- [x] 8.2 両 OS の `PlatformDialogGateway.cs` で、token の打ち切りをブリッジのハンドルの打ち切りへ中継する。UI スレッドへ移ってハンドルを得る前に打ち切られた場合は、ブリッジを呼ばずに終えるか、得た直後に打ち切る (打ち切りの登録とハンドルの受け渡しを 1 か所で直列にする)。事前判定 (`:33-34`) を外し、MAUI の画面の文脈は中身の供給の中で解決する。Dialog・Loading・Toast の供給の中で文脈が取れない場合は、理由の文言つきの `InvalidOperationException` を中身の生成の失敗として扱う (両 OS の Loading `:140-141` / `:145-146`、Toast `:75-76` / `:48-49`) (→ maui-binding: MAUI は提示先を事前に判定せず、中身の供給の時点で画面の文脈を解決する)
- [x] 8.3 iOS のブリッジ: Native の show の Task を保持し、`MauiDialogPresentation` に打ち切りを足す (ObjC 公開と `maui/macios/KsDialogs.Binding.iOS/ApiDefinition.cs` の定義)。閉鎖種別から `presentationHostUnavailable` を外す (`MauiDialogClosure.swift:11`・`StructsAndEnums.cs:16`・`PlatformDialogGateway.cs:75-76`)。中身の紐付け前の「閉じる」は、中身を作らずに Native の show を止める (→ maui-binding: 同上 / PB-MC-06・PB-MC-07)
- [x] 8.4 Android のブリッジ: Native の show の Job を保持し、`MauiDialogPresentation` に打ち切りを足す。打ち切られたら閉鎖通知の cancelled を送ってから投げ直す (`MauiDialogBridge.kt:89-90`)。`MauiDialogClosureListener.onPresentationHostUnavailable` を外す (`MauiDialogClosureListener.kt:20`・`MauiDialogBridge.kt:87-88`・`PlatformDialogGateway.cs:94-95`)。中身の紐付け前の「閉じる」は、中身を作らずに Native の show を止める (→ maui-binding: 同上 / PB-MC-05・PB-MC-07)
- [x] 8.5 `DialogException.PresentationHostUnavailable` (`maui/KsDialogs.Maui/Contract/DialogException.cs:129-132`) と `HostlessDialogGateway` の doc コメントを、「提示の仕組みを持たない環境 (素の .NET)」の意味に直す (→ maui-binding: 同上 / PB-MH-01)
- [x] 8.6 テスト: PB-MC-01 (`maui/KsDialogs.Maui.ApiSurfaceCheck` の正の compile 検査)、PB-MC-02〜04・PB-MC-08 (managed。待ち・表示中・ハンドル取得前を再現する gateway の差し替え)、PB-MC-05・PB-MC-07 (Android ブリッジの単体テスト)、PB-MC-06・PB-MC-07 (iOS ブリッジのテストホスト)、PB-MH-01 (`DialogCallContextTests.cs:69` に ID を付ける)、MB-MA-16 の文言。Android ブリッジの `MauiDialogClosureReportTests.kt:115-135` (提示先不在の通知) を削除し、`:105-113` を「通知してから投げ直す」に改訂する。listener の実装 (`MauiDialogClosureReportTests.kt:43`・`MauiDialogLoadingContentSupplyTests.kt:35`) を直す (→ maui-binding / PB-MC-01〜08・PB-MH-01・MB-MA-16)

## 9. 仕上げ

- [x] 9.1 `scripts/scenario-id-coverage.py` を更新する
  - 既定の除外表 (`DEFAULT_ALLOW_MISSING`) に、CA-SA-08・CA-SA-09 (Sample、手で通す)・PB-MH-02 (到達させる手段が無い防御、コードレビューで受け入れ) を理由付きで登録する
  - 両 Native に同じ ID のテストを置く領域 (`MIRROR_AREAS`) に、`("PB", "HW")`・`("LD", "HW")`・`("TS", "HW")` を足す。iOS 専用の PB-HI と Android 専用の PB-HA は足さない
- [x] 9.2 全形態のテストを実行する: iOS (Simulator の `xcodebuild test`。`swift test` は UIKit のガード下のテストを実行しないので完了判定に使わない)、Android (unit・instrumented)、KMP (androidHostTest・iosTest)、MAUI (`dotnet test`・両 OS のブリッジのテスト・実配置テストホスト)。iOS は Swift Testing と XCTest の両方の件数行を合算し、今回の Scenario ID (PB-HW・PB-HI・LD-HW・TS-HW) のテストが実行に現れていることを確かめる。結果の要約を `evidence/` に残す (手順は `kasane/handbook/cross/test-execution.md`)
- [x] 9.3 **直した後の実測**: 1.2 と同じ手順で、iOS Sample と KMP の iOS Sample の 4 デモと `custom-toast` の起動直後の状態が表示されることを撮る。MAUI の Sample (iOS・Android) でも同じ 5 デモを自動再生し、表示されること (MAUI の中身の供給が失敗しないこと。design Decision 2・7 の見込みの確認) を撮る。1.3 で観測した一時的な非アクティブの場面でも、Toast・Loading・Dialog が表示されることを確かめる。証跡は `evidence/` に置く (→ samples / CA-SA-08)
- [x] 9.4 標準 lint (`scripts/local-path-lint.py`・`scripts/identity-lint.py` ほか) と `scripts/scenario-id-coverage.py` を通す
