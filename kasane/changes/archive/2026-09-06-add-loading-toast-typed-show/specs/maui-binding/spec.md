# maui-binding デルタ (add-loading-toast-typed-show)

MAUI は挙動を Native へパススルーする (core/ADR-0009)。loading-contract / toast-contract の Scenario のうち、C# facade が完結させる責務 — VM 生成・configure・失敗の扱い (LD-TY-03〜07・09・10・12・13 / TS-TY-02〜05・07・08) と、生成した VM・引数を要求に正しく載せるパススルー (LD-TY-08: 生成 VM が進捗受け口として同一インスタンスで渡ること / LD-TY-11: 置き場所 / TS-TY-06: duration と置き場所) — は C# の facade テストで検証し、提示・合流・計時の挙動は Native 側の検証に委ねる。実現経路: Dialog の型指定 show (`maui/KsDialogs.Maui/Presentation/IKsDialog.cs` の `ShowAsync<TViewModel, TResult>`) と `DialogViewRegistry.RegisterViewModel`、1 行登録 `RegisterForDialog` の VM factory 自動配線 (`Hosting/KsDialogsServiceCollectionExtensions.cs` の `RegisterPair`) と同じ機構を Loading / Toast に適用する。

## ADDED Requirements

### Requirement: C# 公開面の Loading / Toast 型指定 show

`LoadingViewRegistry` / `ToastViewRegistry` に `RegisterViewModel<TViewModel>(Func<TViewModel> factory)` を追加する (SHALL)。VM factory と configure は UI スレッドで実行される。VM factory 未登録は既存の `DialogException.ViewModelFactoryNotRegistered` で失敗する。fallback resolver は Loading / Toast には適用しない (明示登録のみ)。

公開署名 (Dialog の型指定 show と同じ「configure が先・placement が後」の並び。既存の `StartAsync` と同じく action が先頭):

| 面 | 署名 | 型引数の制約 |
|---|---|---|
| `IKsLoading` | `Task ShowAsync<TViewModel>(Action<TViewModel>? configure = null, DialogPlacement? placement = null)` | `TViewModel : class, ILoadingViewModel` |
| `IKsLoading` | `Task ShowAsync<TViewModel>(Func<TViewModel, Task> configure, DialogPlacement? placement = null)` | 同上 |
| `IKsLoading` | `Task StartAsync<TViewModel>(Func<IProgress<double>, Task> action, Action<TViewModel>? configure = null, DialogPlacement? placement = null)` | 同上 |
| `IKsLoading` | `Task StartAsync<TViewModel>(Func<IProgress<double>, Task> action, Func<TViewModel, Task> configure, DialogPlacement? placement = null)` | 同上 |
| `IKsLoading` | `Task<T> StartAsync<TViewModel, T>(Func<IProgress<double>, Task<T>> action, Action<TViewModel>? configure = null, DialogPlacement? placement = null)` | 同上 |
| `IKsLoading` | `Task<T> StartAsync<TViewModel, T>(Func<IProgress<double>, Task<T>> action, Func<TViewModel, Task> configure, DialogPlacement? placement = null)` | 同上 |
| `IKsToast` | `void Show<TViewModel>(Action<TViewModel>? configure = null, int? durationMs = null, DialogPlacement? placement = null)` | `TViewModel : class, IToastViewModel` |

既存の型引数 1 個のオーバーロード (`StartAsync<T>(Func<IProgress<double>, Task<T>> …)`・インライン factory 版 `ShowAsync<TViewModel>(TViewModel, Func<TViewModel, View>, …)` / `Show<TViewModel>(TViewModel, Func<…>, …)`) と並ぶため、実引数の型で一意に束縛されることを compile 検査で固定する。型引数の制約 (`ILoadingViewModel` / `IToastViewModel`) は C# 7.3 以降のオーバーロード解決で候補の絞り込みに使われる。**引数をすべて `null` リテラルで渡す呼び出し** (`ShowAsync<VM>(null, null)` 等) は Dialog の型指定 show と既存インライン版の間でも既に曖昧であり、意味のある呼び出しではないため互換性の対象にしない。

#### Scenario: [LD-YM-01] Loading 公開面の正の compile 検査とオーバーロード束縛
- **GIVEN** 利用者コードの立場の compile 検査ソース
- **WHEN** 次の組み合わせを記述する: VM factory 登録 / 型指定 show (configure なし・`Action` configure・`Func<TViewModel, Task>` configure (async lambda)・placement 指定) / 型指定 start (値なし・値あり × configure なし・`Action`・`Func<Task>`) / 既存のメッセージ入口 start (`StartAsync<T>` の値あり) / 既存のインスタンス渡し show・start / 既存のインライン factory 版
- **THEN** すべてコンパイルが通り、各呼び出しの静的な戻り値型 (`Task` / `Task<T>`) が意図したオーバーロードのものになり、async lambda の configure が `Func<TViewModel, Task>` 側に束縛される

#### Scenario: [TS-YM-01] Toast 公開面の正の compile 検査とオーバーロード束縛
- **GIVEN** 利用者コードの立場の compile 検査ソース
- **WHEN** VM factory 登録 / 型指定 show (configure なし・`Action` configure・durationMs 指定・placement 指定) / 既存のメッセージ入口・インスタンス渡し・インライン factory 版を並べて記述する
- **THEN** すべてコンパイルが通り、各呼び出しが意図したオーバーロードに束縛される

### Requirement: 1 行登録の VM factory 自動配線 (Loading / Toast)

`RegisterForLoading<TView, TViewModel>()` / `RegisterForToast<TView, TViewModel>()` は、View factory に加えて「VM をサービスから引く VM factory」を該当レジストリに自動配線する (SHALL)。配線内容と provider 未確立時の失敗 (`ServiceProviderUnavailable`) は `RegisterForDialog` と同じ。

#### Scenario: [LD-YM-02] 1 行登録だけで Loading の型指定 show が有効になる
- **GIVEN** `RegisterForLoading<TView, TViewModel>()` だけを書いた host
- **WHEN** 型指定 show を呼ぶ
- **THEN** サービスから解決された VM で View が作られ、表示の要求が Native へ渡る

#### Scenario: [TS-YM-02] 1 行登録だけで Toast の型指定 show が有効になる
- **GIVEN** `RegisterForToast<TView, TViewModel>()` だけを書いた host
- **WHEN** 型指定 show を呼ぶ
- **THEN** サービスから解決された VM で View が作られ、表示の要求が Native へ渡る
