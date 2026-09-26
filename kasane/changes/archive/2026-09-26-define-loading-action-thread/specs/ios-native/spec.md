# ios-native デルタ (define-loading-action-thread)

loading-contract の挙動 Scenario (LD-TH-*) は同名テストで全量を検証する (core/ADR-0016)。本書は Swift の公開面に固有の差分のみ。

実現経路: `KsLoading` の protocol 要件 5 本 (`ios/Sources/KsDialogs/Presentation/KsLoading.swift:89-133`)、extension の省略形 8 本、`Loading` の実装 (`ios/Sources/KsDialogs/Presentation/Loading.swift:102-163`) と `runScope` (`:226`) の action の型に `@MainActor` を付ける。UI スレッドへ移すのは Swift の isolation の規則が行う。最小のコードでのコンパイルと実行の確認は design Decision 1 に記録した。

## ADDED Requirements

### Requirement: Swift の action の型と、UI スレッド外で始める指定

スコープ形のすべての入口の action の型は `@MainActor @Sendable (@Sendable @escaping (Double) -> Void) async throws -> T` である (SHALL)。その場で書いたクロージャは UI スレッドで始まり、中で MainActor に属する状態 (UIKit を含む) に `await` なしで触れる。

UI スレッド外で始めるには、利用者がクロージャに `@concurrent` を付ける。スコープ形に、スレッドを指定する引数は足さない。

isolation を自分で持つ関数を名前で渡した場合は、その関数の isolation が優先される (Swift の言語規則)。たとえば、isolation の指定が無い async 関数は UI スレッド外で始まる。この規則は公開 doc コメントに書く。

#### Scenario: [LD-HI-01] 公開面の正の compile 検査
- **GIVEN** 利用者コードの立場の compile 検査ソース
- **WHEN** 13 本の入口のそれぞれに、次の 3 つの書き方で action を渡す: 中で MainActor に属する状態に `await` なしで触れるクロージャ / `@concurrent` を付けたクロージャ / 中で `await MainActor.run { … }` を使うクロージャ (変更前の書き方)
- **THEN** すべてコンパイルが通る

#### Scenario: [LD-HI-02] isolation の指定が無い関数を名前で渡すと UI スレッド外で始まる
- **GIVEN** isolation の指定が無い async 関数
- **WHEN** その関数を既定ローディングのスコープ形に名前で渡し、UI スレッドから開始する
- **THEN** 関数の本体の最初の文は UI スレッド外で実行される
