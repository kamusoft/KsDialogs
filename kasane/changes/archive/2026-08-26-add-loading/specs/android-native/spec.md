# android-native デルタ (add-loading)

dialog-contract の挙動 Scenario (LD-CO/AT/WN/PR/ST/CV/TR) は同名テストで全量検証する (core/ADR-0016)。レイアウト共通ケース表は Loading の器でも全量回す (design Decision 5)。本書は Kotlin 公開面に固有の差分のみ。

## ADDED Requirements

### Requirement: Kotlin 公開面の Loading

契約 `interface KsLoading` + 既定シングルトン `Loading.instance` を追加する (SHALL — core/ADR-0002)。スコープ形はジェネリクス (`suspend fun <T> start(message, placement, action): T`) で、action は進捗報告の関数型 (`(Double) -> Unit` 相当) を受け取る。スタイルは `LoadingStyle` 値オブジェクトをシングルトンの設定プロパティで受け、進捗フォーマットは関数型 (`(String?, Double?) -> String`)。カスタム View の登録は従来 View 系 (本体) と Compose 系 (`ksdialogs-compose` モジュール) の技術別オーバーロード (結果報告口なしの factory)。進捗受け口は interface (`onProgress(Double)` 相当) で、VM の任意実装。既定ローディング用の設定プロパティ (`style` / `options` — options は既存 `DialogOptions` を再利用) を `Loading.instance` に持つ。呼び出しは任意スレッドから可能で、内部で main へ直列化する (Dialog 面と同じ規則)。器は Loading 専用の全画面透過 Window (design Decision 4 — 既存 Dialog 器より手前・多段意味論に不参加)。本体モジュールの Compose 非依存 (android/ADR-0001) は Loading 追加後も維持する。

#### Scenario: [LD-AN-01] 公開面の正の compile 検査
- **GIVEN** 利用者コードの立場の compile 検査ソース (api-surface-check)
- **WHEN** show / hide / setMessage / 値を返す start (進捗報告つき)・placement 引数・LoadingStyle の一括設定 (フォーマット関数含む)・従来 View 系 / Compose 系の登録・インライン factory 表示・進捗受け口 interface の実装を記述する
- **THEN** すべてコンパイルが通る

#### Scenario: [LD-AN-02] 本体モジュールは Loading 追加後も Compose に依存しない
- **GIVEN** Loading 一式を追加した本体モジュール (`ksdialogs`)
- **WHEN** 既存の依存グラフ走査検査 (本体の Compose 非依存検査) を実行する
- **THEN** 検査が通る (Compose 系の登録面は `ksdialogs-compose` 側にのみ存在する)
