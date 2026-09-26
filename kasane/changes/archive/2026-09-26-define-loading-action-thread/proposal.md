# Proposal: define-loading-action-thread

## Why

Loading のスコープ形 (処理 action を渡すと、Loading を出して action を await し、終わったら閉じる入口) で、action がどのスレッドで始まるかが契約に定められていない。そのため、形態ごとに動きが食い違っている。

- iOS Native / MAUI iOS: 常にメインスレッド外で始まる
- Android Native / KMP: 呼び出し元の文脈で始まる
- MAUI Android: 常に UI スレッドで始まる

AiForms.Maui.Dialogs から移行した利用者のアプリ (ColorAnalyzer) では、同じコードが Android では成功し、iOS 実機でだけ action 内の UIKit 呼び出しが `UIKitThreadAccessException` になった。スコープ形はほぼ UI スレッドから呼ばれるので、利用者が困っているのは、action の中が UI スレッドかどうかが書く側から分からないことである。

決定は探索で確定済み: 既定は UI スレッドで始まり、指定すると UI スレッド外で始まる。どちらの値でも、始まるスレッドを保証する — [core/ADR-0037](../../decisions/core/0037-loading-action-starts-on-ui-thread.md) (proposed)。経緯は [exploration.md](exploration.md)。

## What Changes

- **契約**: スコープ形の action は、既定で UI スレッドで始まる。「UI スレッド外で始める」を指定すると、UI スレッド外で始まる。どちらも呼び出し元のスレッドに関係なく保証する。全入口 (既定ローディング・インスタンス渡し・インライン・型指定) と全形態で同じ
  - 範囲の定め: 処理自身が実行スレッドを型で宣言している場合 (Swift の isolation) は、その宣言が優先される。UI スレッドを持たない実行環境 (MAUI の素の .NET) では、指定に関係なくその場で実行する
- **指定の表し方は、形態の言語機構に合わせる**
  - iOS Native: action の型を `@MainActor` にする (5 本の protocol 要件と実装)。UI スレッド外で始めるには、利用者がクロージャに `@concurrent` を付ける。入口は増やさない (最小のコードでコンパイルと実行を確認済み — design Decision 1)
  - Android Native / KMP commonMain: 各入口に `actionThread: LoadingActionThread = LoadingActionThread.MAIN` を足す (`BACKGROUND` で UI スレッド外)。Compose の `startCompose` も同じ
  - MAUI: 10 本の `StartAsync` に `LoadingActionThread actionThread = LoadingActionThread.Main` を足す (`Background` で UI スレッド外)
- **切り替えの場所**: 各形態で action を最後に呼ぶ 1 か所 (`runScope` / `LoadingActionRunner`) に置く
  - MAUI は managed 側の `LoadingActionRunner` で切り替えるので、iOS と Android の両方に同じ仕組みで効く
  - KMP は、Android では Native へ指定を渡し、iOS では iosMain の gateway で切り替える (commonMain は UI スレッドを知らないまま — kmp/ADR-0006)
- **テスト**: 全形態で「既定は UI スレッドで始まる」「指定すると UI スレッド外で始まる」を、UI スレッドからの呼び出しとバックグラウンドからの呼び出しの両方で確かめる
- **Sample**: sample-parity に従う
  - 4 ルートの `Default Loading` のデモで、action の最初の文から結果表示を `結果: 処理中` に直接更新する。新しい既定の使い方の見本で、MAUI iOS の元の不具合を実機で確かめる観測点を兼ねる (design Decision 6)
  - iOS の Sample には「処理は MainActor の外で動く」前提のコメントとコードがあるので、新しい既定に合わせて直す
- **ADR-0037 の文言の改訂**: 「入口にフラグを持たせる」を「UI スレッド外で始める指定を持たせる。表し方は形態の言語機構に合わせる」に直す (proposed のため本文を書き換える)
- **概念文書・規約の追随** (蒸留時): core/api/loading-semantics.md (公開面の構成・保証すること)、ios / android / maui / kmp の loading-surface.md (署名と注意)、handbook cross/sample-parity.md の文言の表 (`結果: 処理中` の追加)

影響する能力: loading-contract (core 契約)・ios-native・android-native・maui-binding・kmp-facade・samples

## Non-Goals

- **action の中から呼ぶ周辺操作 (show / hide / メッセージ更新 / 進捗報告) のスレッド規約** — 変えない。全形態ですでに「任意スレッドから呼べる」契約と実装になっている
- **任意の dispatcher / executor を渡せる形への一般化** — 設計判断が要る。ADR-0037 は「始まるスレッドを 2 値で保証する」形に決めており、それ以上の需要は確認できていない
- **KMP の VM factory / configure のスレッド** — 変えない。kmp/ADR-0006 の決定の範囲で、action とは別の話
- **Swift 向けの KMP 面 (`Loading.shared.kmp`)** — スコープ形が無いため対象外
- **利用者向け Skill (`skills/`) の追随** — docs-refresh 経由の手順で行う (CLAUDE.md の運用。`ksdialogs-maui` の loading.md と移行スキルの対応表が対象)

## Impact

- **破壊的変更あり (既定の動きが変わる)**。公開 API はすべて 0.1.0-beta の範囲
  - iOS Native / MAUI iOS: action が、メインスレッド外ではなく UI スレッドで始まるようになる
  - Android Native / KMP: バックグラウンドから呼んだ場合も、UI スレッドで始まるようになる (UI スレッドから呼ぶ場合は今と同じ)
  - MAUI Android: 変わらない
- **iOS Native のソース互換**
  - その場で書いたクロージャは、そのままコンパイルできる。今の iOS テストの書き方 (中で `MainActor.run`) も同じ
  - isolation の指定が無い async 関数を名前で渡した場合は、その関数自身の isolation が優先され、UI スレッド外で始まる (Swift の言語規則)。その関数は型の上でも UI スレッドではないと分かるため、中で UIKit に触るとコンパイルエラーになる
- **KMP を Swift から直接呼ぶ場合のソース互換**: Kotlin の既定値つき引数は、Objective-C 経由で Swift に書き出すと既定値が消える。Swift から KMP の `start` (インスタンス渡し版・既定ローディング版) を呼んでいる利用者は、`actionThread` を明示で渡す必要がある。リポジトリ内の KMP Sample は commonMain から呼んでいるため、影響しない
- **リスク**
  - UI に触らない重い処理を action に同期で書いている利用者は、既定のままだと UI スレッドを塞ぐ。移行の案内で「UI スレッド外で始める指定」を示す
  - MAUI の `dotnet test` (`net10.0`) では UI スレッドの切り替えが素通しになり、本物では確かめられない。MAUI の Android / iOS の互換面の実行で確かめる (handbook cross/test-execution.md)
  - 公開面の compile 検査 (`api-surface-check`) の中に「引数で渡せるのは placement だけ」という前提の箇所があり、直す必要がある

## 級: L

公開 API と Loading の既定の動きを 4 形態にわたって変える破壊的変更で、形態ごとの実現方法 (Swift の isolation、KMP の gateway) を design で先に決める必要がある (オーナー確定 2026-09-25)。

domain: cross
