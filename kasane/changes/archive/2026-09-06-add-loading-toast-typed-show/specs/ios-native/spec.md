# ios-native デルタ (add-loading-toast-typed-show)

loading-contract / toast-contract の挙動 Scenario (LD-TY-* / TS-TY-*) は同名テストで全量検証する (core/ADR-0016)。本書は Swift 公開面に固有の差分のみ。実現経路: Dialog の型指定 show (`ios/Sources/KsDialogs/Presentation/KsDialog.swift` の `show(_:placement:configure:)`) と VM factory 登録 (`Registry/DialogViewRegistry.swift` の `register(_:viewModel:)`) と同じ言語機構 (メタタイプ引数 + `@MainActor @Sendable` クロージャ) を `KsLoading` / `KsToast` と `LoadingViewRegistry` / `ToastViewRegistry` に適用する。

## ADDED Requirements

### Requirement: Swift 公開面の Loading / Toast 型指定 show

`LoadingViewRegistry` / `ToastViewRegistry` に VM factory の登録 (VM 型 + `viewModel:` ラベルの factory。Dialog レジストリと同じ綴り) を追加する (SHALL)。`KsLoading` に型指定 show (`show(VM.self, placement:, configure:)`、非同期 configure 対応) と型指定 start (`start(VM.self, placement:, configure:, action)`) を、`KsToast` に型指定 show (`show(VM.self, duration:, placement:, configure:)`、同期 configure) を追加し、Dialog と同じ省略形の extension (configure なし / placement なし / duration なし) を用意する。VM factory の closure は Dialog の VM factory と同じく **非 throwing** (`@MainActor @Sendable () -> VM`) で、Swift では失敗の表明は configure (throws) が担う。VM factory と configure は MainActor で実行される。VM factory 未登録は既存の `DialogError.viewModelFactoryNotRegistered` で失敗する。

#### Scenario: [LD-YI-01] Loading 公開面の正の compile 検査
- **GIVEN** 利用者コードの立場の compile 検査ソース
- **WHEN** VM factory 登録・型指定 show (configure あり / なし・非同期 configure・placement あり)・型指定 start を記述する
- **THEN** すべてコンパイルが通る

#### Scenario: [TS-YI-01] Toast 公開面の正の compile 検査
- **GIVEN** 利用者コードの立場の compile 検査ソース
- **WHEN** VM factory 登録・型指定 show (configure あり / なし・duration あり・placement あり) を記述する
- **THEN** すべてコンパイルが通る

#### Scenario: [LD-YI-02] SwiftUI 登録でも型指定 show が同じに働く
- **GIVEN** SwiftUI の中身の factory と VM factory を登録した Loading の VM 型
- **WHEN** configure 付きで型指定 show を呼ぶ
- **THEN** configure で設定した状態が表示に反映される

#### Scenario: [TS-YI-02] SwiftUI 登録でも Toast の型指定 show が同じに働く
- **GIVEN** SwiftUI の中身の factory と VM factory を登録した Toast の VM 型
- **WHEN** configure 付きで型指定 show を呼ぶ
- **THEN** configure で設定した状態が表示に反映される
