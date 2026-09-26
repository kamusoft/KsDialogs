# kmp-facade デルタ (define-loading-action-thread)

Scenario ID は `LD-HK-<NN>`。検証の分担は kmp/ADR-0002 に従う。
- commonTest: 共有コードの委譲面を、Test gateway に差し替えて確かめる
- androidHostTest / iosTest: 各 OS の gateway の契約を確かめる

実現経路:
- commonMain の `KsLoading` (`kmp/ksdialogs-kmp/src/commonMain/kotlin/jp/kamusoft/ksdialogs/kmp/KsLoading.kt:103-147`) と `GatewayKsLoading` / `LoadingGateway` (`LoadingGateway.kt:26-102`) は、指定を gateway へ渡すだけにする
- 切り替えは各 OS の gateway に置く
  - Android: `AndroidLoadingGateway` (`kmp/ksdialogs-kmp/src/androidMain/kotlin/jp/kamusoft/ksdialogs/kmp/AndroidLoadingGateway.kt:30-40`) が、Native の `LoadingActionThread` に写して Native の `start` へ渡す
  - iOS: `IosLoadingGateway.runScope` (`kmp/ksdialogs-kmp/src/iosMain/kotlin/jp/kamusoft/ksdialogs/kmp/IosLoadingGateway.kt:116`) の action 呼び出しを、`withContext(Dispatchers.Main)` または `withContext(Dispatchers.Default)` で包む。`kotlinx-coroutines-core` は iosMain にだけある (`kmp/ksdialogs-kmp/build.gradle.kts:177`)
- commonMain は UI スレッドを知らないまま (kmp/ADR-0006)。ADR-0006 の UI スレッドの扱いは VM factory / configure に限った話で、action とは別 (design Decision 3)

## ADDED Requirements

### Requirement: 共有コードの `LoadingActionThread` 引数

commonMain に `enum class LoadingActionThread { MAIN, BACKGROUND }` を公開する (SHALL)。`DialogAlignment` と同じく KMP が自前で持ち、Android の gateway で Native の型に写す。

`KsLoading` の `start` 3 本に、`actionThread: LoadingActionThread = LoadingActionThread.MAIN` を action の直前に足す。action は、`MAIN` なら UI スレッドで始まり、`BACKGROUND` なら UI スレッド外で始まる。どちらも呼び出し元のコルーチン文脈に関係なく保証する。

既定値があるので、今の呼び出しはソースのまま通る。

Swift 向けに書き出される `start` にも同じ引数が現れる。Objective-C 経由の書き出しでは既定値が消えるため、Swift から呼ぶ場合は `actionThread` を明示で渡す。型指定版は、今までどおり `@HiddenFromObjC` で Swift から隠す。

#### Scenario: [LD-HK-01] 3 本の入口から、指定が gateway まで届く
- **GIVEN** 受け取った指定を記録する Test gateway を持つ共有コードの `KsLoading`
- **WHEN** 3 本の `start` を、指定なしと `LoadingActionThread.BACKGROUND` で呼ぶ
- **THEN** 指定なしでは `MAIN`、`BACKGROUND` を渡したときは `BACKGROUND` が gateway に届く

#### Scenario: [LD-HK-02] Android の gateway が、Native の型に写して渡す
- **GIVEN** Android の gateway と、受け取った指定を記録する Native の `KsLoading` の偽物
- **WHEN** gateway に `MAIN` と `BACKGROUND` を渡して開始する
- **THEN** Native には、それぞれ Native の `LoadingActionThread.MAIN` と `LoadingActionThread.BACKGROUND` が渡る

#### Scenario: [LD-HK-03] iOS の gateway は、既定で action を UI スレッドで始める
- **GIVEN** iOS の gateway
- **WHEN** UI スレッド外のコルーチン文脈から、`MAIN` で開始する
- **THEN** action の本体の最初の文は UI スレッドで実行される

#### Scenario: [LD-HK-04] iOS の gateway は、`BACKGROUND` で action を UI スレッド外で始める
- **GIVEN** iOS の gateway
- **WHEN** UI スレッドから、`BACKGROUND` で開始する
- **THEN** action の本体の最初の文は UI スレッド外で実行される
