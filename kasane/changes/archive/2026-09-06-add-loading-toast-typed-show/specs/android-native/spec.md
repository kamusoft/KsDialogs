# android-native デルタ (add-loading-toast-typed-show)

loading-contract / toast-contract の挙動 Scenario (LD-TY-* / TS-TY-*) は同名テストで全量検証する (core/ADR-0016)。本書は Kotlin 公開面に固有の差分のみ。実現経路: Dialog の型指定 show (`android/ksdialogs/.../KsDialog.kt` の `show(viewModelClass:, placement:, configure:)`) と VM factory 登録 (`DialogViewRegistry.kt` の `registerViewModel`) と同じ言語機構 (`KClass<VM>` 引数 + suspend configure) を `KsLoading` / `KsToast` と `LoadingViewRegistry` / `ToastViewRegistry` に適用する。

## ADDED Requirements

### Requirement: Kotlin 公開面の Loading / Toast 型指定 show

`LoadingViewRegistry` / `ToastViewRegistry` に `registerViewModel(viewModelClass, factory)` を追加する (SHALL)。`KsLoading` に型指定 show (`show(viewModelClass, placement, configure)`、suspend configure) と型指定 start (`start(viewModelClass, placement, configure, action)`) を、`KsToast` に型指定 show (`show(viewModelClass, durationMs, placement, configure)`、同期 configure) を追加する。VM factory と configure は Main dispatcher で実行される。value class の VM は登録および型指定 show の時点で構成ミスとして拒否する (Dialog と同じ)。VM factory 未登録は既存の `DialogException.ViewModelFactoryNotRegistered` で失敗する。

#### Scenario: [LD-YA-01] Loading 公開面の正の compile 検査
- **GIVEN** 利用者コードの立場の compile 検査ソース
- **WHEN** VM factory 登録 (ラムダ / コンストラクタ参照)・型指定 show (configure あり / なし・suspend configure・placement あり)・型指定 start を記述する
- **THEN** すべてコンパイルが通る

#### Scenario: [TS-YA-01] Toast 公開面の正の compile 検査
- **GIVEN** 利用者コードの立場の compile 検査ソース
- **WHEN** VM factory 登録・型指定 show (configure あり / なし・durationMs あり・placement あり) を記述する
- **THEN** すべてコンパイルが通る

#### Scenario: [LD-YA-02] Compose 登録でも型指定 show が同じに働く
- **GIVEN** `registerCompose` で中身を登録し VM factory を登録した Loading の VM 型
- **WHEN** configure 付きで型指定 show を呼ぶ
- **THEN** configure で設定した状態が表示に反映される (Compose 面に追加 API は不要)

#### Scenario: [TS-YA-02] value class の VM は型指定 show で拒否される
- **GIVEN** Toast の VM 契約に準拠した value class
- **WHEN** その型の VM factory を登録する、または型指定 show を呼ぶ
- **THEN** 構成ミスとして失敗する

#### Scenario: [TS-YA-03] Compose 登録でも Toast の型指定 show が同じに働く
- **GIVEN** `registerCompose` で中身を登録し VM factory を登録した Toast の VM 型
- **WHEN** configure 付きで型指定 show を呼ぶ
- **THEN** configure で設定した状態が表示に反映される (Compose 面に追加 API は不要)

#### Scenario: [LD-YA-03] value class の VM は Loading の登録と型指定 show / start で拒否される
- **GIVEN** Loading の VM 契約に準拠した value class
- **WHEN** その型の VM factory を登録する、または型指定 show / start を呼ぶ
- **THEN** 構成ミスとして失敗する
