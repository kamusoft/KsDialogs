# Design: define-loading-action-thread

## Context

Loading のスコープ形の action がどのスレッドで始まるかを、4 形態の共通の契約として定める。方針は [core/ADR-0037](../../decisions/core/0037-loading-action-starts-on-ui-thread.md) (proposed) で確定済み。既定は UI スレッドで始まり、指定すると UI スレッド外で始まる。どちらも、始まるスレッドを保証する。

今の入口と、action を最後に呼ぶ場所 (2026-09-25 コード確認):

| 形態 | 公開入口 | action を呼ぶ場所 | 今始まるスレッド |
|---|---|---|---|
| iOS Native | `KsLoading` の protocol 要件 5 本 + extension の省略形 8 本 (`ios/Sources/KsDialogs/Presentation/KsLoading.swift:89-133`) | `Loading.runScope` (`ios/Sources/KsDialogs/Presentation/Loading.swift:226`、呼び出しは `:237`) | 常にメインスレッド外 |
| Android Native | `KsLoading.start` 4 本 (`android/ksdialogs-core/.../KsLoading.kt:120-170`) + Compose の `startCompose` (`android/ksdialogs/.../compose/ComposeLoadingShow.kt:41`) | `Loading.runScope` (`android/ksdialogs-core/.../Loading.kt:163`、呼び出しは `:170`) | 呼び出し元の文脈 |
| MAUI | `IKsLoading.StartAsync` 10 本 (`maui/KsDialogs.Maui/Presentation/IKsLoading.cs:153-302`) | `LoadingActionRunner.RunAsync` (`maui/KsDialogs.Maui/Internals/LoadingActionRunner.cs:36`) と `HostlessLoadingGateway.RunAsync` (`maui/KsDialogs.Maui/Internals/HostlessLoadingGateway.cs:30`) | iOS はメインスレッド外、Android は UI スレッド |
| KMP commonMain | `KsLoading.start` 3 本 (`kmp/ksdialogs-kmp/src/commonMain/.../KsLoading.kt:103-147`) | Android: Native の `start` へ委譲 (`AndroidLoadingGateway.kt:30-40`) / iOS: `IosLoadingGateway.runScope` (`IosLoadingGateway.kt:116`、呼び出しは `:123`) | 呼び出し元の文脈 |

Swift 向けの KMP 面 (`ios/Sources/KsDialogs/Kmp/KsLoadingKmp.swift`) にスコープ形は無い。

## Goals / Non-Goals

**Goals**
- 全形態・全入口で、action が始まるスレッドを 2 つの値 (UI スレッド / UI スレッド外) で保証する。既定は UI スレッド
- どちらのスレッドで始まるかを、呼び出しの 1 行から読めるようにする
- 形態ごとに、両方の値をテストで確かめる

**Non-Goals**
- proposal.md の Non-Goals のとおり。周辺操作のスレッド規約、任意の dispatcher / executor への一般化、KMP の VM factory / configure、`skills/` の追随は扱わない

## Decisions

### Decision 1: Swift は action の型を `@MainActor` にし、UI スレッド外は利用者の `@concurrent` で表す

**採用案:**

`KsLoading` の protocol 要件 5 本と `Loading` の実装、extension の省略形 8 本の action の型を、次のように変える。

```swift
// 変更前
_ action: @Sendable (@Sendable @escaping (Double) -> Void) async throws -> T
// 変更後
_ action: @MainActor @Sendable (@Sendable @escaping (Double) -> Void) async throws -> T
```

入口は増やさない。利用者の書き方は次のとおり。

```swift
// 既定: UI スレッドで始まる。中で UIKit に await なしで触れる
try await Loading.instance.start { report in
    imageView.image = resized
}
// UI スレッド外で始まる
try await Loading.instance.start { @concurrent report in
    try await heavyWork(report)
}
```

**理由:**
- 最小のコードでコンパイルと実行を確かめた (Swift 6.3.2、`-swift-version 6`。2026-09-25、探索の scratchpad で実施)
  - 既定の入口にその場で書いたクロージャは、UI スレッドから呼んでもバックグラウンドから呼んでも UI スレッドで始まる。中で MainActor に属する状態に `await` なしで触れる
  - `@concurrent` を付けたクロージャは、どちらから呼んでも UI スレッド外で始まる。中で MainActor に属する状態に触るとコンパイルエラーになる (型で分かる)
  - 今の iOS テストの書き方 (action の中で `await MainActor.run { … }`) は、そのままコンパイルできて動く
- Swift では isolation が型に載るので、「どちらのスレッドで始まるか」が型と呼び出しの 1 行の両方に出る。間違えるとコンパイラが止める
- 切り替えのための実行時の処理が要らない。`runScope` は `@MainActor` の関数を呼ぶだけで、UI スレッドへ移すのは言語の規則が行う

**言語規則による例外:** isolation の指定が無い async 関数を名前で渡すと (`start(work)`)、その関数自身の isolation が優先され、UI スレッド外で始まる (同じ確認で実測)。型の上でも UI スレッドではないと分かるため、中で UIKit に触るとコンパイルエラーになる。この例外は契約と公開 doc コメントに書く。

**代替案:**
- **P1: UI スレッド外用の入口を別に足す** (既定の入口は `@MainActor`、別の引数を持つ入口は isolation の指定なし) — 却下。動作は正しい (同じ確認で実測) が、protocol 要件が 5 本から 10 本に倍増し、extension の省略形も増える。`@concurrent` で同じことが表せる
- **P2: action の型を `nonisolated(nonsending)` の 1 つにし、実行時のフラグでライブラリが呼ぶ場所を切り替える** — 却下。実行時のスレッドは正しいが、UI スレッドで始まる場合でも、中で MainActor に属する状態に触るとコンパイルエラーになる (同じ確認で実測)。型の上では UI スレッドだと分からないので、「中で UIKit に触れる」という狙いが果たせない
- **Kotlin / C# と同じ列挙型の引数を Swift にも足す** — 却下。引数の値で action の型の isolation を変えられないため、P1 か P2 のどちらかになる

### Decision 2: Kotlin と C# は列挙型 `LoadingActionThread` の引数で表す

**採用案:**

| 形態 | 型 | 引数 | 置き場所 |
|---|---|---|---|
| Android Native (`ksdialogs-core`) | `enum class LoadingActionThread { MAIN, BACKGROUND }` | `actionThread: LoadingActionThread = LoadingActionThread.MAIN` | 4 本の `start` と `startCompose` の、action (trailing lambda) の直前 |
| KMP commonMain | 同じ綴りの enum を commonMain に持つ (`DialogAlignment` と同じく KMP が自前で持ち、gateway で Native の型に写す) | 同上 | 3 本の `start` の action の直前 |
| MAUI | `public enum LoadingActionThread { Main, Background }` | `LoadingActionThread actionThread = LoadingActionThread.Main` | 10 本の `StartAsync` の最後 (既存の `placement` の後) |

値の綴りは各言語の既存 enum に合わせる (Kotlin は `DialogLayoutArea.WINDOW` と同じ大文字のスネークケース、C# はパスカルケース)。

**理由:**
- Kotlin と C# は関数の型にスレッドを載せられないので、引数で表す
- 呼び出し側に `LoadingActionThread.BACKGROUND` と書かれるので、どちらのスレッドで始まるかが 1 行で読める
- 既定値を持つ引数にするので、今の呼び出しはソースのまま通る
- core/ADR-0015 (show の引数で渡せる器のメタ属性は placement のみ) と core/ADR-0023 (表示 API にスタイル引数を設けない) とはぶつからない。両 ADR が限っているのは器の属性 (配置・見た目) で、`actionThread` は器の属性ではなく、action をどう実行するかの指定である。Android の負の compile 検査 (`RejectsOptionsArgumentOnLoadingShow`) の説明にこの前提の文言があれば、tasks 2.6 で直す

**代替案:**
- **真偽値の引数 (例: `runOnUiThread = true`)** — 却下。呼び出し側に `false` と書かれても意味が読めない。探索で「真偽値か列挙か」を提案段階の論点として残していたもの
- **nullable の列挙型で、null を既定の意味にする** (`placement: DialogPlacement? = null` と同じ慣習) — 却下。`placement` の null は「スタイルの既定に従う」という意味を持つが、このスレッドの指定には「スタイル側の既定」が無い。null の意味を読む手間だけが増える
- **`CoroutineDispatcher` / `TaskScheduler` を渡せる形** — 却下。ADR-0037 は 2 つの値を保証する形に決めており、それ以上の需要は確認できていない (proposal Non-Goals)

### Decision 3: 切り替えは action を最後に呼ぶ 1 か所に置き、既存の配管を使う

**採用案:**

| 形態 | UI スレッド | UI スレッド外 | 使う配管 |
|---|---|---|---|
| iOS Native | 実行時の処理なし (`@MainActor` の関数を呼ぶと言語が移す) | 実行時の処理なし (`@concurrent` の関数を呼ぶと言語が移す) | Swift の isolation |
| Android Native | `runScope` の `action(report)` を `withContext(Dispatchers.Main.immediate)` で包む | `withContext(Dispatchers.Default)` で包む | 型指定の生成で使っている `withContext(Dispatchers.Main.immediate)` (`Loading.kt:132`) と同じ |
| KMP Android | `AndroidLoadingGateway` が `LoadingActionThread` を Native の enum に写して渡す | 同上 | Native の実装に任せる |
| KMP iOS | `IosLoadingGateway.runScope` の action 呼び出しを `withContext(Dispatchers.Main)` で包む | `withContext(Dispatchers.Default)` で包む | iosMain にだけある `kotlinx-coroutines-core` (`kmp/ksdialogs-kmp/build.gradle.kts:177`) |
| MAUI (両 OS) | `LoadingActionRunner.RunAsync` の `await action(...)` を、UI スレッドで呼ぶ口を通して呼ぶ (本番は `MainThread.InvokeOnMainThreadAsync`) | `Task.Run(() => action(...))` で呼ぶ | 型指定の configure で使っている `DialogPresenter.OnUiThreadAsync` (`DialogPresenter.cs:125-130`) と同じ移送 |

- KMP の commonMain は UI スレッドを知らないままにする。切り替えを各 OS の gateway に置くことで、kmp/ADR-0006 の論拠 (共有コードは UI スレッドの概念を持たない・最薄のファサード・Fake 差し替えのテスト容易性) に触れない。ADR-0006 の UI スレッドの扱いは VM factory / configure に限った話で、action とは別と整理する
- MAUI は managed 側の `LoadingActionRunner` で切り替えるので、両 OS の互換面 (Swift / Kotlin のブリッジ) がどのスレッドで C# の callback を呼ぶかに左右されない。`HostlessLoadingGateway` (Runner を通らない経路) は、UI スレッドを持たない素の .NET でだけ使われるので、指定を受け取っても切り替えず、今どおりその場で実行する (契約の範囲は UI スレッドを持つ環境だけ — loading-contract デルタ)。指定は `Loading` → `ILoadingGateway.RunAsync` → 各 gateway → Runner と引き回す
- MAUI の「UI スレッドで呼ぶ口」は Runner に差し替え可能な形で渡す。`net10.0` のテストでは、専用スレッドで動く偽物に差し替え、どちらの口を通ったかを確かめる (Decision 5)

**理由:** 各形態で action を呼ぶ場所が 1 か所しかなく、報告と完了の順序の保証 (`runScope` の `queue.drain()`、Runner の `finally`) もそこにある。切り替えをその内側に入れれば、順序の保証をそのまま保てる。

**代替案:**
- **KMP の commonMain (`GatewayKsLoading`) で切り替える** — 却下。commonMain に coroutines の依存と UI スレッドの概念を持ち込むことになり、kmp/ADR-0006 の論拠とぶつかる。Android は Native に委譲しているので、commonMain で切り替えると 2 重になる
- **MAUI の両 OS のブリッジ (Swift / Kotlin) で切り替える** — 却下。C# の action を呼ぶスレッドを 2 か所で合わせることになる。Runner なら 1 か所で両 OS に効く

### Decision 4: 契約が保証するのは「始まるスレッド」で、await の後は各言語の規則に従う

**採用案:** 契約は「action の本体の最初の文がどのスレッドで実行されるか」を保証する。action の中で await した後にどこへ戻るかは、各言語の規則に従い、ライブラリは追加の保証をしない。

| 形態 | UI スレッドで始まった action の await の後 |
|---|---|
| Swift | クロージャの isolation (`@MainActor`) のまま。UI スレッドに戻る |
| Kotlin | `withContext` の dispatcher (`Main`) のまま。UI スレッドに戻る |
| C# | UI スレッドの同期コンテキストへ戻る。ただし利用者が `ConfigureAwait(false)` を書いた場合は戻らない |

**理由:** await の後の戻り先は、利用者のコード (C# の `ConfigureAwait` など) で変えられる。ライブラリが保証できるのは、自分が action を呼ぶ時点のスレッドだけである。

**代替案:**
- **action の本体全体が UI スレッドで動くことまで保証する** — 却下。C# では利用者の `ConfigureAwait(false)` を止められず、保証できない

### Decision 5: テストの分担

**採用案:**

| 形態 | 確かめ方 | 置き場所 |
|---|---|---|
| core 契約 (`LD-TH-*`) | Native 2 実装の同名テストで全量を検証する (core/ADR-0016) | iOS: Swift Testing (`ios/Tests/KsDialogsTests/`、判定は `Support/DialogTestThread.swift` の `isMainThread()`)。Android: instrumented (`android/ksdialogs-core/src/androidTest/`、判定は `Looper.myLooper() === Looper.getMainLooper()`) |
| iOS の公開面 (`LD-HI-*`) | 正の compile 検査 (`ios/Tests/KsDialogsTests/LoadingApiSurfaceCompileChecks.swift`) に、既定のクロージャで MainActor の状態に触る書き方と `@concurrent` の書き方を足す | 同左 |
| Android の公開面 (`LD-HA-*`) | 正の compile 検査 (`android/api-surface-check`) に引数ありと引数なしを足す。Compose の `startCompose` の指定は instrumented で確かめる | 同左と `android/ksdialogs/src/androidTest/` |
| MAUI (`LD-HM-*`) | `net10.0` の Runner テストで、UI スレッドで呼ぶ口の偽物 (専用スレッドで動く) に差し替え、既定はその口を通って偽物のスレッドで始まること、`Background` は口を通らずスレッドプールで始まることを確かめる。10 本の入口から指定が Runner まで届くことは facade テストで確かめる | `maui/KsDialogs.Maui.Tests/` (`LoadingActionRunnerTests` など) |
| KMP (`LD-HK-*`) | commonTest: 指定が gateway まで届くこと。androidHostTest: Android の gateway が Native の enum に写して渡すこと。iosTest: iOS の gateway が UI スレッド / UI スレッド外で action を始めること (`support/MainLoopPump.kt` の `runPumpingMainLoop`) | `kmp/ksdialogs-kmp/src/{commonTest,androidHostTest,iosTest}/` |

**理由:** `net10.0` では UI スレッドへの移送が素通しになる (`DialogPresenter.OnUiThreadAsync` の `#else` 分岐) ため、MAUI は振り分けの正しさを偽物で確かめる。本物の `MainThread.InvokeOnMainThreadAsync` は、型指定の configure で既に使っている配管である。

### Decision 6: MAUI の実機での確認は、Sample の Default Loading のデモで行う

**採用案:** 4 ルートの Sample の `Default Loading` のデモで、action の最初の文から結果表示を `結果: 処理中` に直接更新する (UI スレッドへ明示的に移す書き方は使わない)。Simulator / 実機で処理中の画面を撮り、verification の証跡に残す (samples デルタ LD-HS-02)。

**理由:**
- 変更前の MAUI iOS では、この更新が Debug 構成で UIKit のスレッド検査に掛かって失敗する。変更後は通る。元の不具合を実際の MAUI ホストでそのまま確かめられる (second-opinion-spec-001 の指摘 3)
- 新しい既定の使い方の見本になる
- sample-parity に従い、4 ルートを同じ変更にする。追加する文言は 1 つだけで、デモ項目は増やさない

**代替案:**
- **`net10.0` の偽物による振り分けの確認 (Decision 5) と、既存の配管 (`MainThread.InvokeOnMainThreadAsync`) の実績で足りるとする** — 却下。本物の UI スレッドを通らないため、元の不具合が直ったことを確かめられない

UI スレッド外の指定 (`Background`) は実機では確かめない。`Task.Run` で呼ぶだけの単純な経路で、振り分けは Decision 5 のテストで確かめる。

## Risks / Trade-offs

- **既定の動きが変わる** (proposal Impact)。UI に触らない重い処理を action に同期で書いている利用者は、既定のままだと UI スレッドを塞ぐ。0.1.0-beta の範囲なので破壊的変更として出し、リリースノートで案内する (Migration Plan)
- **Swift の関数を名前で渡す例外** (Decision 1)。契約と公開 doc コメントに書く
- **MAUI は本物の UI スレッドをテストで確かめられない** (Decision 5)。実機での確認手段は Open Questions に残す
- **KMP iOS の `Dispatchers.Main`**: Darwin のメインキューで動く。iosTest の `runPumpingMainLoop` で確かめる。`Dispatchers.Main.immediate` を使うか (UI スレッドから呼ばれたときに dispatch を省くか) は、iosTest で確かめてから実装で決める
- **MAUI の iOS 互換面 (Swift ブリッジ)** は Native の `start` を呼んでいるので、Decision 1 の型の変更に合わせてコンパイルが通るよう直す。C# の callback のスレッドは Runner が決めるため、ブリッジのクロージャに `@MainActor` / `@concurrent` のどちらを付けても契約には影響しない。UI スレッドを無駄に経由しないよう、`@concurrent` を付けて今の動きを保つ
- **MAUI の Android 互換面 (Kotlin ブリッジ)** は Native の `start` を指定なしで呼ぶ。既定の `MAIN` は、ブリッジの今の動き (scope が `Dispatchers.Main.immediate`) と同じなので直さない。C# の callback のスレッドは、iOS と同じく Runner が決める
- **KMP を Swift から直接呼ぶ利用者**は、Objective-C 経由の書き出しで既定値が消えるため、`actionThread` を明示で渡す必要がある (proposal Impact)

## Migration Plan

- 破壊的変更として、次のリリースのリリースノートに書く (handbook cross/release-procedure.md の `## Changes`)
  - 既定の動きが変わる形態と、UI スレッド外で始める指定の書き方 (Swift は `@concurrent`、Kotlin / C# は `LoadingActionThread`)
- AiForms からの移行案内 (`ksdialogs-aiforms-migration`) と `ksdialogs-maui` の loading.md は、蒸留後に docs-refresh で追随させる (proposal Non-Goals)
- ColorAnalyzer の回避策 (`ShowAsync` / `HideAsync` の明示) は、この変更を取り込めば `StartAsync` へ戻せる。完成を知らせるかは蒸留時に判断する (ColorAnalyzer は `relations` に載せていないため、自動の確認は出ない)

## Open Questions

- なし (MAUI の実機での確認方法は Decision 6 で確定)

## ADR 候補

- Decision 1・2 (指定の表し方) は core/ADR-0037 (proposed) の Decision に取り込み済み。蒸留時に、実装とテストの結果で accepted にするかを判断する
- Decision 3 のうち「KMP の action の切り替えは各 OS の gateway に置き、kmp/ADR-0006 の UI スレッドの扱いは VM factory / configure に限る」は、kmp/ADR-0006 の適用範囲の明確化にあたる。蒸留時に、ADR-0006 への注記か ADR-0037 の Consequences の更新として残す (ADR-0037 の「kmp/ADR-0006 と両立するか確かめる必要がある」の項を解消するため)
- Decision 4・5 は局所的な設計なので、コードとテストに任せる
