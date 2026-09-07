# 原典 Loading 仕様サーベイ (2026-08-25)

phase-7-loading の議論の土台として ksn-scout が移植元を調査した結果。パスは断りがない限り `../AiForms.Maui.Dialogs/` からの相対 (ローカル位置は concepts cross「参考リポジトリの在り処」で解決)。

## 1. 公開 API 全体像

- 単一エントリ `Loading.Instance` (Lazy シングルトン、`ILoading`) — `AiForms.Maui.Dialogs/Loading/Loading.cs:7-8`
- `ILoading` = 既定ローディング用の `Show / Hide / SetMessage / StartAsync` + カスタム View 用ファクトリ `Create<TView>() / Create(LoadingView, vm) / Create(object vm) / CreateFromModel<TVM>()`。DEBUG のみ `Dispose()` — `Loading/ILoading.cs:3-18`
- `Create*` は `IReusableLoading` (`Show / Hide / StartAsync` + `IDisposable`) を返す。生成物は Dispose まで何度でも再利用可 — `Loading/IReusableLoading.cs:3-8`、README.md:433
- `Create(object viewModel)` / `CreateFromModel<TVM>()` は `Configurations.ViewTypeGetter` / `Resolve` (DI) 経由で VM→View を解決し、見つからなければ `KeyNotFoundException` — `Loading/Loading.iOS.cs:30-42`、`Configurations.cs:13-24`
- README 構成: `## Loading` (README.md:302) → 概要/スクショ → `### Show default Loading Dialog` (314) → `### Create custom loading view` (345) → `### Show a custom loading dialog` (413)。API リファレンス側は `## ILoading` (502) / `## IReusableLoading` (530) / `## LoadingConfig` (547) / `## LoadingView` (697)
- net (非プラットフォーム) ターゲットは全メソッド `NotImplementedException` — `Loading/Loading.Net.cs`

## 2. LoadingConfig (既定ローディング専用。カスタム View では未使用)

`Loading/LoadingConfig.cs:5-18` / 説明は README.md:547-578

| プロパティ | 既定 | 意味 |
|---|---|---|
| OffsetX / OffsetY | 0 | インジケータ・メッセージの中心からの相対オフセット |
| IndicatorColor | White | スピナー色 |
| FontSize | 14 | メッセージ文字サイズ |
| FontColor | White | メッセージ文字色 |
| OverlayColor | RGB(0,0,0) | オーバーレイ背景色 |
| Opacity | 0.6 | 全体不透明度 (0-1) |
| DefaultMessage | null | message 未指定時の既定文言 |
| ProgressMessageFormat | `"{0}\n{1:P0}"` | {0}=message, {1}=progress |
| IsReusable | false | false なら Hide のたびにインスタンス破棄、true なら使い回す |

- 適用タイミングは初回 `Initialize()` の一度きり (`OnceInitializeAction`) なので、表示後の config 差し替えは効かない — `Loading/DefaultLoading.iOS.cs:18,119-121`、`Loading/DefaultLoading.Android.cs:23,130-132`

## 3. 進捗通知

- `Task StartAsync(Func<IProgress<double>,Task> action, string message = null, bool isCurrentScope = false)` (既定) / `Task StartAsync(Func<IProgress<double>,Task> action, bool isCurrentScope = false)` (カスタム)
- 内部で `Progress<double>` を作り `ProgressChanged` を購読 → action に渡す → action 完了後に Hide。Hide 時に購読解除して null 化 — `Loading/DefaultLoading.iOS.cs:34-45,67-74`、`Loading/ReusableLoading.iOS.cs:46-58,87-98`、`Loading/ReusableLoading.Android.cs:63-72,91-97`
- 既定版の受け口: `string.Format(config.ProgressMessageFormat, message, progress)` でラベル更新。`progress < 0` (= `SetMessage`) のときは書式なしで message のみ — `Loading/DefaultLoading.iOS.cs:92-117`、`Loading/DefaultLoading.Android.cs:99-128`
- カスタム版の受け口: `LoadingView.Progress` (double の BindableProperty) に代入するだけ。View 側は自分自身への Binding + StringFormat で表現する — `Loading/LoadingView.cs:5-18`、`Loading/ReusableLoading.iOS.cs:110-116`、README.md:406,411
- 既定版の `SetMessage` は表示中インスタンスがある場合のみ有効 (`_defaultInstance != null`) — `Loading/Loading.iOS.cs:71-77`

## 4. 単一性の担保

- iOS: `LoadingBase.IsRunning` フラグ。`Loading.Show()` は `DefaultInstance.IsRunning` なら即 return、`ReusableLoading.Show()` も `IsRunning` で return — `Loading/LoadingBase.iOS.cs:12`、`Loading/Loading.iOS.cs:46-49`、`Loading/ReusableLoading.iOS.cs:62-64`
- iOS の `StartAsync` は先頭で `WaitDismiss()`: 250ms×5 回まで前回の非表示完了を待ち、1.25 秒たっても IsRunning が下りなければ**何もせず return** (action すら実行されない) — `Loading/LoadingBase.iOS.cs:77-88`、`Loading/DefaultLoading.iOS.cs:36-39`、`Loading/ReusableLoading.iOS.cs:48-51`
- Android: 固定タグ `Loading.LoadingDialogTag = "LoadingDialog"` で FragmentManager を検索する `IsRunning()`。`StartAsync` は `WaitDialogDestroy()` で既存 Fragment の `DestroyTcs` 完了 + 100ms 待ってから進む (iOS と違い**待った上で必ず表示する**) — `Loading/Loading.Android.cs:14,59-65,97-117`、`Loading/LoadingBase.Android.cs:54-74`
- Hide: iOS は 0.25 秒のアルファアニメ後に `IsRunning=false`。Android は 250ms のアルファアニメ + `IsDialogShownTcs` (ダイアログ生成完了) を待ってから `DismissAllowingStateLoss()` — 極短時間処理でのクラッシュ/フリーズ回避のため — `Loading/DefaultLoading.iOS.cs:67-85`、`Loading/DefaultLoading.Android.cs:62-97`、`Loading/ReusableLoading.Android.cs:91-124`
- `Loading.Hide()` は `IsReusable=false` なら既定インスタンスを破棄して null 化 — `Loading/Loading.iOS.cs:61-69`、`Loading/Loading.Android.cs:77-87`、`Loading/DefaultLoading.Android.cs:92-95`
- ユーザー操作では閉じられない (`Cancelable=false` / `SetCanceledOnTouchOutside(false)`) — `Native/Android/LoadingPlatformDialog.cs:33-35`、README.md:304

## 5. 表示スコープ (isCurrentScope)

- iOS のみ有効。false = KeyWindow に、true = 現在の RootViewController.View にオーバーレイを addSubview し、4 辺を制約で貼る — `Loading/LoadingBase.iOS.cs:49-75`、README.md:511 (「This only works on iOS.」)
- 表示ごとに前回値と比較し、変わっていれば制約を張り直す — `Loading/DefaultLoading.iOS.cs:55-60`、`Loading/ReusableLoading.iOS.cs:75-80`
- Android は引数を受け取るが**完全に無視**。常に全画面透明 DialogFragment (`CreateFullScreenTransparentDialog`, MatchParent) — `Loading/ReusableLoading.Android.cs:56-61,74-89`、`Loading/DefaultLoading.Android.cs:42-60`、`Native/Android/DialogHelpers.cs:126-137`

## 6. アニメーション

- 既定ローディング: 表示・非表示とも 0.25 秒 (Android 250ms) のオーバーレイ・アルファのみ。iOS は `Alpha → config.Opacity`、Android は初期化時に `ContentView.Alpha = config.Opacity` を置いて 0 へフェード。差し替え不可 — `Loading/DefaultLoading.iOS.cs:64,76-84`、`Loading/DefaultLoading.Android.cs:71-74,135`
- カスタム View: 同じアルファフェードに加えて `ExtraView.RunPresentationAnimation()` / `RunDismissalAnimation()` / `Destroy()` (いずれも空の virtual) をオーバーライドして自由に演出 — `ExtraView.cs:167-169`、README.md:376-391
- 呼び出し位置: iOS は `ShowInner` 内で直接、Android は DialogFragment の `OnStart()` で呼ぶ (生成タイミングの差)。Dismissal はいずれも Hide 内 — `Loading/ReusableLoading.iOS.cs:82`、`Native/Android/LoadingPlatformDialog.cs:50-55`、`Loading/ReusableLoading.Android.cs:104`
- iOS の Hide には `_loadingView.Progress = 0d` へのリセットがコメントアウトで残っている (再利用時に前回進捗が残る) — `Loading/ReusableLoading.iOS.cs:94-97`

## 7. カスタム View 版と Dialog 機構の実装共有

- View 階層: `LoadingView : ExtraView`、`DialogView : ExtraView` の兄弟。共通なのは ExtraView (配置・サイズ・角丸・枠・アニメ virtual) だけで、Loading 側は `Progress` と `OverlayColor` を追加 — `Loading/LoadingView.cs:3`、`Dialog/DialogView.cs:3`、README.md:581
- クラス構成: `LoadingBase` (プラットフォーム別) を `DefaultLoading` と `ReusableLoading` が継承。`LoadingBase` はオーバーレイ/ContentView の生成・破棄、Progress 保持、単一性チェックを持つ — `Loading/LoadingBase.iOS.cs`、`Loading/LoadingBase.Android.cs`
- `ReusableLoading` は `DialogHelpers.CreateNewHandler` / `Measure` / `SetLayoutAlignment` (iOS) / `GetGravity` / `SetOffsetMargin` (Android) を Dialog と共有。角丸・枠の適用も同型 — `Loading/ReusableLoading.iOS.cs:130-155`、`Loading/ReusableLoading.Android.cs:141-176`
- Android は Loading 専用 DialogFragment `LoadingPlatformDialog` + 専用ペイロード `LoadingDialogPayload` を持つ (Dialog 側は `ExtraPlatformDialog` + `ExtraDialogPayload`)。DefaultLoading と ReusableLoading は**同一の `LoadingPlatformDialog` インスタンス**を `Loading` から共有 — `Native/Android/LoadingPlatformDialog.cs`、`Loading/Loading.Android.cs:8,18,33`
- `ReusableLoading` の Dispose は View の `Destroy()` / BindingContext 解除 / Handler の DisconnectHandler をまとめて行う — `Loading/ReusableLoading.iOS.cs:24-44`、`Loading/ReusableLoading.Android.cs:29-54`

## 8. Sample での使われ方

- 既定: `Configurations.LoadingConfig = new LoadingConfig { DefaultMessage = "Loading...", IsReusable = true }` を置き、`StartAsync` 内で `progress.Report` と途中の `SetMessage("Soon...")` を実演。`isCurrentScope` を毎回トグルして両スコープを試す — `Sample/ViewModels/MainPageViewModel.cs:46-70`
- カスタム: `using var customLoading = Loading.Instance.Create<MyIndicatorView>(匿名 VM)` → `StartAsync` (using で自動 Dispose) — `Sample/ViewModels/MainPageViewModel.cs:75-91`
- VM 起点: `Create(new VmLoadingViewModel())` と `CreateFromModel<VmLoadingViewModel>()` の 2 経路。Prism の `registry.RegisterDialog<VmLoading, VmLoadingViewModel>()` + `SetIocConfig` で解決 — `Sample/ViewModels/VmTestViewModel.cs:46-62`、`Sample/MauiProgram.cs:32,37-42`
- `Sample/Views/Dialogs/VmLoading.xaml:12` が `AutoRotateForIOS="False"` を指定しているが、このプロパティを読んでいるのは Dialog 側 (`Dialog/ReusableDialog.iOS.cs:67` → `ContentViewController`) だけで、Loading の iOS 実装は ViewController を作らず KeyWindow に addSubview するため**事実上ノーオペ** (grep 全件で Loading 側の参照ゼロ)。既定値は true (`ExtraView.cs:141-152`)
- 他に `Sample/ViewModels/AutoTestViewModel.cs:150-212` (Show/Hide/StartAsync/Dispose の自動テスト)、`Sample/ViewModels/ManualTestViewModel.cs:357-368`

## 9. Dialog と Loading の分岐点

- API レベルで完全分離: `Dialog.Instance` (`IDialog`) と `Loading.Instance` (`ILoading`) は別シングルトン、共有するのは `ExtraView` と `Configurations` / `DialogHelpers` の補助関数のみ
- iOS: **既定ローディングは Dialog 機構を一切通らない**。UIViewController も PresentationController も使わず、`UIView` (OverlayView) に `UIActivityIndicatorView` + `UILabel` を直接組んで KeyWindow / 現ページ View に貼るだけ — `Loading/DefaultLoading.iOS.cs:119-153`。カスタム版も同じオーバーレイ方式で、中に MAUI View の Handler を載せるだけ (`ReusableLoading.iOS.cs:118-156`)。一方 Dialog は `ContentViewController` + `DialogPresentationController` で present する
- Android: 両者とも DialogFragment だが**クラスもタグも別系統** (`LoadingPlatformDialog` / タグ "LoadingDialog" vs `ExtraPlatformDialog`)。Loading 側はキーリスナー・キーボード対応・OverlayColor 透過時の gravity 調整を持たず、`Cancelable=false` 固定 — `Native/Android/LoadingPlatformDialog.cs:17-48` vs `Native/Android/ExtraPlatformDialog.cs:29-57`
- 分岐のもう一段: 既定 (`DefaultLoading`) はネイティブ部品を直接組み立て、カスタム (`ReusableLoading`) は `LoadingView` → Handler 生成 → Measure/Arrange という Dialog 寄りの経路。LoadingConfig が効くのは前者だけ (README.md:552)

## 調査で否定された想定

- `IsRegisterLoadingDialogViews` は原典に存在しない (全文 grep でヒット無し)
- Windows / Mac Catalyst 等の非モバイルは `Loading.Net.cs` で未実装のため、そこでの仕様は存在しない
