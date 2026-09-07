# ios-native デルタ (add-loading)

dialog-contract の挙動 Scenario (LD-CO/AT/WN/PR/ST/CV/TR) は同名テストで全量検証する (core/ADR-0016)。レイアウト共通ケース表は Loading の器でも全量回す (design Decision 5)。本書は Swift 公開面に固有の差分のみ。

## ADDED Requirements

### Requirement: Swift 公開面の Loading

契約 protocol `KsLoading` + 既定シングルトン `Loading.shared` を追加する (SHALL — core/ADR-0002)。スコープ形はジェネリクス (`start<T>(message:placement:_ action:) async throws -> T`) で、action は進捗報告クロージャ (`(Double) -> Void` 相当) を受け取る。スタイルは `LoadingStyle` 値オブジェクトをシングルトンの設定プロパティで受け、進捗フォーマットはクロージャ (`(String?, Double?) -> String`)。カスタム View の登録は UIKit / SwiftUI の技術別オーバーロード (結果報告口なしの factory)。進捗受け口は protocol (`onProgress(Double)` 相当) で、VM の任意準拠。呼び出し面は任意スレッドから呼べ、内部で MainActor へ直列化する (Dialog 面と同じ規則)。進捗受け口とフォーマットクロージャは MainActor 上で呼ばれる。既定ローディング用の設定プロパティ (`style` / `options` — options は既存 `DialogOptions` を再利用) を `Loading.shared` に持つ。

#### Scenario: [LD-IO-01] 公開面の正の compile 検査
- **GIVEN** 利用者コードの立場の compile 検査ソース
- **WHEN** show / hide / setMessage / 値を返す start (進捗報告つき)・placement 引数・LoadingStyle の一括設定 (フォーマットクロージャ含む)・UIKit / SwiftUI 両系統の登録・インライン factory 表示・進捗受け口 protocol の準拠を記述する
- **THEN** すべてコンパイルが通る

#### Scenario: [LD-IO-02] SwiftUI 登録のカスタム Loading が UIKit 登録と同じに働く
- **GIVEN** SwiftUI 系統の factory で登録した進捗受け口つき VM 型
- **WHEN** 表示し、スコープ形の処理が進捗を報告し、hide する
- **THEN** 表示・進捗転送・撤去の観察可能挙動が UIKit 登録と一致する
