# scout 調査: 移植元 (AiForms.Maui.Dialogs) の公開 API 表面 + KsAppKMP ADR-0006 (2026-08-13)

ksn-scout による調査要約。論点「公開 API 形状」「結果通知」「DI 差し込み」「View 再利用」「レイアウト計算」「既存資産取り込み」の議論素材。パスは移植元リポジトリ内の相対パス。

## 1. 公開 API エントリポイント

3種すべて `Lazy<T>` + `static Instance` の同一パターン。`Dialog.Instance` / `Loading.Instance` / `Toast.Instance` がインターフェース (`IDialog` / `ILoading` / `IToast`) を返す (Dialog/Dialog.cs:5-6 ほか)。

- **IDialog** (Dialog/IDialog.cs:3-16): 8メソッド — `ShowAsync<TView>(object viewModel = null)` / `ShowAsync(DialogView view, object vm)` / `ShowAsync(object viewModel)` / `ShowFromModelAsync<TViewModel>()` / `ShowFromModelAsync<TViewModel,TParameter>(TParameter)` / `ShowResultAsync<TResult>(object vm)` / `ShowResultAsync<TView,TResult>(object vm = null)` / `ShowResultFromModelAsync<TViewModel,TParameter,TResult>(TParameter)` + `Create<TView>()` / `Create(DialogView, object)`
- **ILoading** (Loading/ILoading.cs:3-18): `Show(string message = null, bool isCurrentScope = false)` / `Hide()` / `SetMessage(string)` / `StartAsync(Func<IProgress<double>,Task> action, ...)` + `Create` 系4種
- **IToast** (Toast/IToast.cs:3-9): `void Show<TView>(object viewModel = null)` のみ。`Toast` クラスは `[Obsolete("Toast will be deprecated.")]` (Toast/Toast.cs:5)

README 例 (README.md:144-165, 333-343):

```csharp
var ret = await Dialog.Instance.ShowAsync<MyDialogView>(new{Title="Hello"});
var reusableDialog = Dialog.Instance.Create<MyDialogView>();
ret = await reusableDialog.ShowAsync();   // 何度でも呼べる
reusableDialog.Dispose();
var r = await Dialog.Instance.ShowResultAsync<MyDialogView,MyResult>(new{Title="Hello"});
await Loading.Instance.StartAsync(async progress => { ...; progress.Report(0.5); });
```

## 2. 結果通知 (DialogNotifier)

DialogView が自分で `DialogNotifier` を生成・公開し、View 側が `Complete()` / `Complete<T>(result)` / `Cancel()` を叩く。ReusableDialog が内部イベント (`Completed` / `Canceled`) を購読して `TaskCompletionSource` を解決 — async/await 対応あり。

- 公開 IF (Dialog/DialogNotifier.cs:3-8): `void Complete(); void Cancel(); void Complete<T>(T result);`
- DialogView がコンストラクタで自動生成し `BindableProperty` (OneWayToSource) として公開 (Dialog/DialogView.cs:50-72)。VM 側は `DialogNotifier="{Binding Notifier}"` で受ける (README:686-707)
- 結果橋渡し (Dialog/ReusableDialog.iOS.cs:141-211): TCS + 購読/解除、`ShowResultAsync<TResult>` は `(TResult)e.Result` キャスト、キャンセル時 `default`
- 外側タップは `IsCanceledOnTouchOutside` → `Cancel()` (同:213-219)
- **設計上の粗**: 結果が `object` 経由キャストで型安全でない。`Complete()` と `Complete<T>()` が同一イベントに合流するため `ShowResultAsync<T>` 中の `Complete()` でキャスト例外の可能性 (実コード確認、テスト裏取りなし)

## 3. DI 差し込み (SetIocConfig)

DI コンテナ非依存の「関数2つ」注入。コンテナ参照は持たない。

```csharp
public static void SetIocConfig(Func<Type,Type> viewTypeGetter, Func<Type,object> viewResolver = null)
```

- ViewTypeGetter: ViewModel Type → View Type。Resolver: Type → インスタンス (View にも VM にも使用) (Configurations.cs:13-25)
- 使用箇所 (Dialog/Dialog.cs:33-62): 見つからなければ `KeyNotFoundException`。`ShowFromModelAsync<TViewModel>` は VM もコンテナ解決 → コンストラクタインジェクション可
- パラメータ受け渡し: `IDialogViewModel<T>.DialogInitializeAsync(T parameter)`、後始末: `IDialogViewModelDestroy.Destroy()` (IDialogViewModel.cs:4-12)
- Prism 連携例あり (README.md:189-193)
- **既知の粗**: `Resolve = viewResolver;` に null チェックがなく、省略時にデフォルトを null で上書き (Configurations.cs:23)

## 4. View 再利用機構

- **IReusableDialog** (Dialog/IReusableDialog.cs:3-7): `IDisposable` + `Task<bool> ShowAsync()` + `Task<TResult> ShowResultAsync<TResult>()`。`Dialog.Instance.ShowAsync<T>()` は内部で `using var dlg = Create<TView>(vm)` = 1回きり使い捨て。`Create` 経由なら Dispose まで何度でも Show 可
- **OnceInitializeAction**: 遅延1回初期化フック。コンストラクタでアクション代入 → `ShowAsync()` 冒頭で `Invoke()` → `Initialize()` 末尾で null 代入 (2回目以降 no-op)。Handler 生成・CornerRadius/Border 適用・Measure→Arrange・VC 構築を遅延実行 (ReusableDialog.iOS.cs:40-77, 139, 178)。Loading にも同型があるが iOS 版は `Action<bool>` とシグネチャが割れている (LoadingBase.iOS.cs:15 vs Android:15)
- **View 側ライフサイクル**: `DialogView.SetUp()` / `TearDown()` (Show ごと前後)、`ExtraView.RunPresentationAnimation()` / `RunDismissalAnimation()` / `Destroy()`。アニメーションは 250ms 想定 (README:620)

## 5. レイアウト属性と Measure 重複

**ExtraView** = DialogView / ToastView / LoadingView の共通基底 (ExtraView.cs:6)。属性一覧:

| プロパティ | 型 | 既定値 |
|---|---|---|
| ProportionalWidth / ProportionalHeight | double | -1 |
| VerticalLayoutAlignment / HorizontalLayoutAlignment | LayoutAlignment | Center |
| OffsetX / OffsetY | int | 0 |
| CornerRadius | float | 0 |
| BorderColor / BorderWidth | Color / double | Transparent / 0 |
| AutoRotateForIOS | bool | true |
| DialogMargin | Thickness | default |

派生固有: DialogView = `IsCanceledOnTouchOutside`(既定 true) / `OverlayColor` / `UseCurrentPageLocation` / `DialogNotifier`。LoadingView = `Progress` / `OverlayColor`(既定 rgba(0,0,0,0.2))。ToastView = `Duration`(既定 1500ms)。Loading の既定設定は別系統の `LoadingConfig` (LoadingConfig.cs)。

**Measure の重複**: Native/iOS/DialogHelpers.cs:52-122 と Native/Android/DialogHelpers.cs:139-209 が**ほぼ行対応のコピー** (日本語コメントまで同一)。本質的な差はデバイスサイズ取得のみ。ロジック: Proportional 指定 → 比率 / Fill → 画面幅-マージン / 未指定 → Infinity で Measure して maxHeight クランプ。Alignment の実座標反映はプラットフォーム別 (Android: LayoutAlignmentExtensions + SetOffsetMargin、iOS: DialogPresentationController)。`ExtraView.OnPropertyChanged` も自前 Measure→Arrange を実行しており、**レイアウト計算が共通層・iOS・Android の3箇所に散在** — 移植時の主要な負債。

## 6. KsAppKMP ADR-0006 (ハイブリッド取り込み型)

`KsAppKMP core/ADR-0006` (accepted, 2026-08-03) — inherit-ksapp-core-api-principles

- **原則系は本 ADR で踏襲を宣言 (即時)**: 標準 API の並置原則 (KsApp ADR-0005 由来: 標準 API を置換・ラップせず独立抽象として並置) + デフォルト ON・明示オプトアウト原則 (KsApp ADR-0007 由来)
- **機構系は機能吸収時にオンデマンド再決定**: 対応する出典 ADR/concepts を読み、新文脈で設計し直して新 ADR に出典リンクを残す。一括移植はしない
- 分割軸: プラットフォーム非依存の原則系 vs 旧環境の仕組みに密結合した機構系
- 却下案: 一括移植 (翻訳コスト + 移植した瞬間から乖離開始) / オンデマンドのみ (原則不在で API 形状を決めることになる)
- 負の受容: 吸収のたびに参照の一手間、原則だけでは機械的に決まらず都度判断

## 未確認事項

- ReusableDialog.Android.cs の ShowAsync 実装は iOS と同型と推測 (行単位未読)。Loading / Toast の実装本体も未読
- KsApp 側 ADR-0005/0007 の原文は未読 (ADR-0006 内の要約引用のみ)
