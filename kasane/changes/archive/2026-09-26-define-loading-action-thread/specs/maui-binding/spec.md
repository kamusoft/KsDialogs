# maui-binding デルタ (define-loading-action-thread)

MAUI は挙動を Native へパススルーする (core/ADR-0009)。ただし action の実行は C# の facade が行うため、action が始まるスレッドの切り替えは managed 側の `LoadingActionRunner` が担う。`net10.0` のユニットテストでは UI スレッドへの移送が素通しになる (`maui/KsDialogs.Maui/Internals/DialogPresenter.cs:125-130`) ので、UI スレッドで呼ぶ口を偽物に差し替えて振り分けを確かめる (design Decision 5)。

実現経路: `LoadingActionRunner.RunAsync` (`maui/KsDialogs.Maui/Internals/LoadingActionRunner.cs:36`) の action 呼び出しを、`Main` なら UI スレッドで呼ぶ口 (本番は `MainThread.InvokeOnMainThreadAsync`。型指定の configure と同じ配管) 経由に、`Background` なら `Task.Run` 経由にする。Runner を通らない `HostlessLoadingGateway.RunAsync` (`maui/KsDialogs.Maui/Internals/HostlessLoadingGateway.cs:30`) は、UI スレッドを持たない素の .NET でだけ使われるため、指定を受け取っても切り替えない。指定は `Loading` → `ILoadingGateway.RunAsync` → 各 gateway → Runner と引き回す。

## ADDED Requirements

### Requirement: C# の `LoadingActionThread` 引数と action の振り分け

`public enum LoadingActionThread { Main, Background }` を公開する (SHALL)。

`IKsLoading` の `StartAsync` 10 本の最後 (既存の `placement` の後) に、`LoadingActionThread actionThread = LoadingActionThread.Main` を足す。`Main` は UI スレッドで始まり、`Background` は UI スレッド外で始まる。

既定値があるので、今の呼び出しはソースのまま通る。オーバーロードの束縛も変わらない。

UI スレッドを持たない実行環境 (iOS / Android の実装を持たない素の .NET。表示先の無い `HostlessLoadingGateway` が使われる) では、指定に関係なく、action をその場で実行する (今の動きのまま)。この環境には UI スレッドが存在せず、表示も成立しないため、指定の意味が無い。

`Main` では action を UI スレッドで呼ぶ口を通して呼び、`Background` ではスレッドプールで呼ぶ。どちらの経路でも Runner の順序の保証は保つ。
- 進捗の報告を、呼ばれたスレッドのまま同期で転送する
- 成否によらず、終了をちょうど 1 回伝える
- 失敗を預けて、呼び出し元へ返す

#### Scenario: [LD-HM-01] 公開面の正の compile 検査とオーバーロード束縛
- **GIVEN** 利用者コードの立場の compile 検査ソース
- **WHEN** 10 本の `StartAsync` を、`actionThread` なし / `LoadingActionThread.Main` / `LoadingActionThread.Background` (名前付き引数) で記述する
- **THEN** すべてコンパイルが通り、各呼び出しの静的な戻り値型 (`Task` / `Task<T>`) が、`actionThread` を足す前と同じオーバーロードのものになる

#### Scenario: [LD-HM-02] 既定では、UI スレッドで呼ぶ口を通って action が始まる
- **GIVEN** UI スレッドで呼ぶ口を、専用スレッドで動く偽物に差し替えた Runner
- **WHEN** 指定なし (`Main`) で action を走らせる
- **THEN** action は偽物の口を通り、偽物の専用スレッドで始まる

#### Scenario: [LD-HM-03] `Background` では、UI スレッドで呼ぶ口を通らずに action が始まる
- **GIVEN** UI スレッドで呼ぶ口を、専用スレッドで動く偽物に差し替えた Runner
- **WHEN** 偽物の専用スレッドから、`Background` で action を走らせる
- **THEN** action は偽物の口を通らず、偽物の専用スレッドとは別のスレッドで始まる

#### Scenario: [LD-HM-04] どちらの経路でも、Runner の順序の保証が保たれる
- **GIVEN** 進捗を報告してから完了する action と、例外を投げる action
- **WHEN** それぞれを `Main` と `Background` で走らせる
- **THEN** どの組み合わせでも、報告は完了通知より先に同期で転送され、終了はちょうど 1 回伝えられ、例外は失敗として預けられる

#### Scenario: [LD-HM-05] 10 本の入口から、指定が gateway まで届く
- **GIVEN** 受け取った指定を記録する gateway の偽物を持つ `Loading`
- **WHEN** 10 本の `StartAsync` を、指定なしと `Background` で呼ぶ
- **THEN** 指定なしでは `Main`、`Background` を渡したときは `Background` が gateway に届く

#### Scenario: [LD-HM-06] UI スレッドを持たない環境では、指定に関係なくその場で実行する
- **GIVEN** 表示先の無い gateway (`HostlessLoadingGateway`)
- **WHEN** 呼び出し元のスレッドから、`Main` と `Background` のそれぞれで action を走らせる
- **THEN** どちらの場合も、action は呼び出し元のスレッドでそのまま始まり、戻り値と失敗は契約どおり返る
