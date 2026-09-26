# android-native デルタ (define-loading-action-thread)

loading-contract の挙動 Scenario (LD-TH-*) は同名テストで全量を検証する (core/ADR-0016)。本書は Kotlin の公開面に固有の差分のみ。

実現経路: `Loading.runScope` (`android/ksdialogs-core/src/main/kotlin/jp/kamusoft/ksdialogs/Loading.kt:163`) の `action(report)` を、指定に応じて `withContext(Dispatchers.Main.immediate)` または `withContext(Dispatchers.Default)` で包む。同じ配管は型指定の生成 (`Loading.kt:132`) で使っている。

## ADDED Requirements

### Requirement: Kotlin の `LoadingActionThread` 引数

`ksdialogs-core` に `enum class LoadingActionThread { MAIN, BACKGROUND }` を公開する (SHALL)。

`KsLoading` の `start` 4 本と Compose の `startCompose` に、`actionThread: LoadingActionThread = LoadingActionThread.MAIN` を action (trailing lambda) の直前に足す。`MAIN` は UI スレッドで始まり、`BACKGROUND` は UI スレッド外で始まる。

既定値があるので、引数を書かない今の呼び出しはソースのまま通る。

#### Scenario: [LD-HA-01] 公開面の正の compile 検査
- **GIVEN** 利用者コードの立場の compile 検査ソース (`android/api-surface-check`)
- **WHEN** `start` 4 本と `startCompose` を、`actionThread` なし / `LoadingActionThread.MAIN` / `LoadingActionThread.BACKGROUND` で trailing lambda と組み合わせて記述する
- **THEN** すべてコンパイルが通る

#### Scenario: [LD-HA-02] Compose の `startCompose` でも指定が効く
- **GIVEN** Compose の中身を渡す `startCompose`
- **WHEN** 指定なしと `LoadingActionThread.BACKGROUND` のそれぞれで、`Dispatchers.Default` から開始する
- **THEN** 指定なしでは action が UI スレッドで始まり、`BACKGROUND` では UI スレッド外で始まる
