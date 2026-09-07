# maui-binding デルタ (add-loading)

MAUI はレイアウト計算・合流管理を再実装せず、属性・呼び出しを Native へ渡すパススルーとする (core/ADR-0001)。挙動の全量検証は Native 側。本書は C# 公開面と bridge 経路の差分のみ。

## ADDED Requirements

### Requirement: C# 公開面の Loading

契約 `IKsLoading` + 既定シングルトン `Loading.Instance` を追加する (SHALL — core/ADR-0002)。呼び出し面は `ShowAsync(message, placement)` / `HideAsync()` / `SetMessage(message)` / `StartAsync` (`Func<IProgress<double>, Task>` と値を返す `Func<IProgress<double>, Task<T>>` の両形)。スタイルは `LoadingStyle` (進捗フォーマットは `Func<string?, double?, string>`) をシングルトンの設定プロパティで受ける。カスタム View の登録は Dialog と同型の登録 API と、DI 連携の1行登録糖衣 (`RegisterForLoading<TView, TViewModel>` 相当 — maui/ADR-0005 の Loading 版) を持つ。器メタ属性の添付は既存の添付プロパティ (`ksd:Dialog.*`) をそのまま使い、既定ローディング用の設定プロパティ (`Style` / `Options` — Options は既存 DialogOptions を再利用) を `Loading.Instance` に持つ。進捗受け口は interface で VM の任意実装。完了時点は dialog-contract の契約に従う (`HideAsync` / 合流最後の `StartAsync` は撤去完了まで待つ)。MAUI 層は合流状態を持たず、すべて Native coordinator へ委譲する (design Decision 8)。

#### Scenario: [LD-MA-01] 公開面の正の compile 検査
- **GIVEN** 利用者コードの立場の compile 検査ソース (ApiSurfaceCheck)
- **WHEN** ShowAsync / HideAsync / SetMessage / 両形の StartAsync・placement 引数・LoadingStyle の一括設定・登録 API と DI 糖衣・進捗受け口 interface の実装を記述する
- **THEN** すべてコンパイルが通る

#### Scenario: [LD-MA-02] DI 糖衣で登録したカスタム Loading が表示される
- **GIVEN** `AddKsDialogs` + Loading 版1行登録の DI チェーン構成
- **WHEN** 登録した VM 型のインスタンスで Loading を表示する
- **THEN** DI 解決された View がカスタム Loading として表示される

#### Scenario: [LD-MA-03] 属性・スタイル・進捗が値のまま Native へ届く
- **GIVEN** placement 引数と LoadingStyle を設定した MAUI 呼び出し
- **WHEN** 表示し、処理が進捗を報告する
- **THEN** placement の各値・スタイルの各値・進捗値が無変換で Native 側に到達する (パススルー検証)
