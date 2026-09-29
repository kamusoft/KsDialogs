# maui-binding デルタ (wait-for-host-appearance)

MAUI は挙動を Native へパススルーする (core/ADR-0009)。Dialog・Loading・Toast の提示先の出現待ちは Native が行う。本書は、MAUI の Dialog の呼び出し元の打ち切り (maui/ADR-0006、design Decision 6) と、MAUI 自前の提示先の判定の扱い (design Decision 7) の差分。Scenario ID は `PB-MC-<NN>` (MAUI の打ち切り) と `PB-MH-<NN>` (MAUI の提示先の判定)。

実現経路
- 公開面: `IKsDialog` の `ShowAsync` 7 本 (`maui/KsDialogs.Maui/Presentation/IKsDialog.cs:39,67,89,116,133,148,161`) と実装 (`Presentation/Dialog.cs`・`Internals/DialogPresenter.cs:77-113`)。gateway の呼び出しは `DialogPresenter.cs:105` の 1 か所
- gateway: `IDialogGateway.PresentAsync` (`Internals/DialogGateway.cs:29`)、両 OS の `Platforms/*/PlatformDialogGateway.cs`
- ブリッジ: iOS `maui/macios/native/KsDialogsMauiBridge/MauiDialogBridge.swift:43-73` (Task を保持して打ち切る。先例は `ios/Sources/KsDialogs/Interop/KsDialogsInteropShowHandle.swift`)、Android `maui/android/native/ksdialogs-maui-bridge/.../MauiDialogBridge.kt:47-57,77-94` (Job を保持して打ち切る)。ハンドルは両 OS の `MauiDialogPresentation`
- 素の .NET: `Internals/HostlessDialogGateway.cs`

## ADDED Requirements

### Requirement: Dialog の show は呼び出し元の打ち切りを受け付ける

`IKsDialog` の `ShowAsync` のすべての入口 (7 本) は、末尾に `CancellationToken cancellationToken = default` を持つ SHALL。省略できるので、今の呼び出しはソースのまま通る。

呼び出し元が打ち切ったときの振る舞いは次のとおりとする SHALL。

- 提示先の出現を待っている間なら、Dialog は一度も表示されない。表示中なら Dialog を閉じる。どちらも、内部の結果は cancelled で確定する
- `ShowAsync` は `OperationCanceledException` を投げる (結果の `Cancelled` は返さない)
- 呼び出しの時点で打ち切り済みなら、登録の解決・VM の生成・Native の呼び出しをせずに、`OperationCanceledException` を投げる

打ち切りは、C# の gateway からブリッジのハンドルの打ち切りへ中継し、Native の打ち切り (iOS は Task のキャンセル、Android はコルーチンの打ち切り) で止める SHALL。gateway がハンドルを得る前に打ち切られた場合は、打ち切りを取りこぼさない SHALL (ハンドルを得る前ならブリッジを呼ばずに終え、得た直後ならすぐに打ち切る)。ブリッジは打ち切りを閉鎖通知の cancelled として C# 側へ知らせる。Android のブリッジは、通知してからキャンセルを投げ直す。

待っている間に結果が確定した場合 (VM の報告) は、ブリッジは中身を作らずに Native の show を止め、Dialog を一度も表示しない SHALL。

#### Scenario: [PB-MC-01] 公開面の正の compile 検査
- **GIVEN** 利用者コードの立場の compile 検査ソース (`maui/KsDialogs.Maui.ApiSurfaceCheck`)
- **WHEN** `ShowAsync` の 7 本を、token なし / 位置引数の token / 名前付き引数 `cancellationToken:` で記述する
- **THEN** すべてコンパイルが通る。既存の名前付き引数の負の検査 (`options:` / `transition:`) は、今までどおりコンパイルエラーになる

#### Scenario: [PB-MC-02] 待っている間に打ち切ると、表示されずに OperationCanceledException になる
- **GIVEN** 提示先の出現を待っている状態を再現する gateway と、登録済みの VM
- **WHEN** `ShowAsync` を token 付きで呼び、待っている間に token を打ち切る
- **THEN** gateway に打ち切りが中継され、`ShowAsync` は `OperationCanceledException` を投げる。中身の View は作られない

#### Scenario: [PB-MC-03] 表示中に打ち切ると、Dialog が閉じて OperationCanceledException になる
- **GIVEN** 表示中の状態を再現する gateway と、登録済みの VM
- **WHEN** `ShowAsync` を token 付きで呼び、表示中に token を打ち切る
- **THEN** gateway に打ち切りが中継されて Dialog が閉じ、`ShowAsync` は `OperationCanceledException` を投げる

#### Scenario: [PB-MC-04] 呼び出しの時点で打ち切り済みなら、何もせずに OperationCanceledException になる
- **GIVEN** 打ち切り済みの token
- **WHEN** インスタンス渡しと型指定の `ShowAsync` をそれぞれ呼ぶ
- **THEN** どちらも `OperationCanceledException` を投げる。VM factory・configure・gateway はどれも呼ばれない

#### Scenario: [PB-MC-08] gateway がハンドルを得る前に打ち切られても、取りこぼさない
- **GIVEN** UI スレッドへ移る途中 (ハンドルを得る前) で止められる gateway と、登録済みの VM
- **WHEN** `ShowAsync` を token 付きで呼び、gateway がハンドルを得る前に token を打ち切ってから、gateway を進める
- **THEN** ブリッジは呼ばれない (または呼ばれた直後に打ち切られる)。Dialog は表示されず、`ShowAsync` は `OperationCanceledException` を投げる

#### Scenario: [PB-MC-05] Android のブリッジは、打ち切りを cancelled の閉鎖通知にしてから投げ直す
- **GIVEN** Android のブリッジで、Native の show を走らせているハンドル
- **WHEN** ハンドルで打ち切る
- **THEN** 閉鎖通知の cancelled が 1 回届き、そのあとキャンセルが伝播する。「提示先が無い」の通知は存在しない

#### Scenario: [PB-MC-06] iOS のブリッジは、打ち切りを cancelled の閉鎖通知として届ける
- **GIVEN** iOS のブリッジ (テストホスト上) で、Native の show を走らせているハンドル
- **WHEN** ハンドルで打ち切る
- **THEN** 閉鎖通知の cancelled が 1 回届く。閉鎖の種別に「提示先が無い」は存在しない

#### Scenario: [PB-MC-07] 中身を作る前にブリッジを閉じると、一度も表示されない
- **GIVEN** 中身の生成をまだ求められていない (提示先を待っている) ブリッジのハンドル
- **WHEN** ハンドルで閉じる (結果の確定による)
- **THEN** 中身の供給は呼ばれず、Dialog は表示されない

### Requirement: MAUI は提示先を事前に判定せず、中身の供給の時点で画面の文脈を解決する

Dialog の gateway は、ブリッジを呼ぶ前に MAUI の画面の文脈を解決しない SHALL。MAUI の画面の文脈は、Native が呼ぶ中身の供給の中で、その時点の Native の提示先に対応する画面から解決する (Loading・Toast と同じ形)。

中身の供給で画面の文脈が取れなかった場合は、中身の生成の失敗として、理由の文言つきの `InvalidOperationException` を扱う SHALL (Dialog は show の失敗、Loading は開始の失敗または表示の諦め、Toast はその 1 枚の破棄)。`DialogException.PresentationHostUnavailable` は使わない。

`DialogException.PresentationHostUnavailable` は、提示の仕組みそのものを持たない環境 (Native を持たない素の .NET) での Dialog の show の失敗にだけ使う SHALL。

#### Scenario: [PB-MH-01] 素の .NET では、Dialog の show がその場で失敗する
- **GIVEN** Native を持たない素の .NET (`HostlessDialogGateway`)
- **WHEN** 登録済みの VM で `ShowAsync` を呼ぶ
- **THEN** `DialogException.PresentationHostUnavailable` で失敗し、中身の View は作られない

#### Scenario: [PB-MH-02] 中身の供給で画面の文脈が取れなければ、InvalidOperationException の失敗になる
- **GIVEN** Native の提示先はあるが、対応する MAUI の画面の文脈が取れない状態
- **WHEN** Native が中身の供給を呼ぶ
- **THEN** 理由の文言つきの `InvalidOperationException` が中身の生成の失敗として扱われ、`DialogException.PresentationHostUnavailable` は使われない (Native の提示先の確保後には到達させる手段が無い防御のため、コードレビューで受け入れる Scenario。`scripts/scenario-id-coverage.py` の除外表に理由付きで登録する)

## MODIFIED Requirements

### Requirement: Android の managed/native 境界での失敗の受け止め

Android の Dialog / Loading の中身の供給は、iOS と同じく managed 側で失敗を値 (中身なし) に変えて互換面へ返し、元の失敗を呼び出し 1 回分の預かり口に退避したうえで、互換面からの失敗の通知を受け取った時点で元の失敗を呼び出し元へそのまま投げ直す SHALL (core/ADR-0033)。互換面 (Kotlin) の Dialog / Loading の中身の供給は Toast と同じく中身なしを返せ、中身なしは既存の失敗経路 (Dialog は閉鎖通知の失敗、Loading は完了通知の失敗) に合流する SHALL。生の例外が境界を越えない SHALL。

#### Scenario: [MB-MA-15] Kotlin 互換面は中身なしを失敗経路へ合流させる

- **GIVEN** 中身なし (null) を返す供給元を渡した Dialog / Loading の互換面
- **WHEN** 提示を要求する
- **THEN** Dialog は閉鎖通知の失敗、Loading は完了通知の失敗として呼び出し側へ通知され、例外は互換面の外へ漏れない

#### Scenario: [MB-MA-16] 既存の失敗経路は変わらない

- **GIVEN** 提示の仕組みを持たない環境 (素の .NET) と、利用者操作でキャンセルされる状態
- **WHEN** それぞれ show する
- **THEN** 前者は `DialogException.PresentationHostUnavailable`、後者は `Cancelled` の結果として従来どおり届く
