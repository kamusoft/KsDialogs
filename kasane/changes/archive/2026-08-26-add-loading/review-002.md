# レビュー結果: add-loading (002 回目)

**日付**: 2026-08-26
**判定**: NEEDS_DISCUSSION

## サマリー

review-001 と second-opinion-code-001 の指摘は、**採用された 8 件すべてが実装で解消している**。相方由来の 3 Major (Android factory 例外の rollback / prompt cancellation での閉じ残り / MAUI 最終進捗の喪失) はいずれも根本を直した修正で、決定的なテスト (Android instrumented 3 本・C# 5 本) が付き、修正が新しい契約変更を持ち込んでいないことを diff とコード読解で確認した。MAUI 互換面の Loading テスト (Kotlin 10 本) も追加され、review-001 の Minor はすべて解消している。

一方で **iOS の全件実行は依然として安定して green にならない** (7 回中 3 回失敗)。ただし失敗はすべて `DialogTestWaiting.waitUntil` (5 秒) のタイムアウトで、失敗するテストは本変更と無関係なもの (phase-6 由来の互換面テスト・`PB-TR-03`・SwiftUI 中身の提示ほか) に散っており、**HEAD (本変更なし) でも同じ形で失敗する** (7 回中 1 回)。原因は検証機に居座る CPU 負荷プロセス群で、実装では解決できない。この 1 点だけを理由に NEEDS_DISCUSSION とし、他は低優先度の Minor / Suggestion にとどまる。

## 実行した検証

| ビルドルート | コマンド | 結果 |
|---|---|---|
| ios/ | `xcodebuild test -scheme KsDialogs -destination 'platform=iOS Simulator,name=iPhone 17'` (7 回反復) | **206 tests / 37 suites — 4 回 green / 3 回 失敗** (失敗はすべて `waitUntil` タイムアウト) |
| android/ | `./gradlew test --rerun-tasks` | 67 / 0 failures |
| android/ (instrumented, Pixel 4a) | `ANDROID_SERIAL=<Pixel 4a> ./gradlew connectedDebugAndroidTest` | `:ksdialogs` 186 (skipped 1) + `:ksdialogs-compose` 35 = **221 / 0 failures** |
| kmp/ | `./gradlew allTests --rerun-tasks` | iosSimulatorArm64 42 + androidHostTest 38 = **80 / 0 failures** |
| maui/ | `dotnet test` | **110 / 0 failures** (review-001 時 105 → `LoadingActionRunnerTests` 5 本追加) |
| maui/android/native/ | `./gradlew :ksdialogs-maui-bridge:test --rerun-tasks` | **25 / 0 failures** (review-001 時 15 → Loading 10 本追加) |

- `python3 scripts/scenario-id-coverage.py` → 未網羅なし。`--require-mirror` → 対象領域は両 Native に揃っている。`--selftest` → 全件 OK (新規の (prefix, 領域) ケース 2 本を含む)
- `scripts/local-path-lint.py` / `scripts/identity-lint.py` / `scripts/comment-policy-lint.py` → いずれも 0 件 (comment-policy は 737 ファイル検査)
- 足場アーティファクト: `specs/` 6 ファイル・`proposal.md` / `design.md` は未変更 (変更は `tasks.md` のチェック反転と `ui/brief.md` のみ)
- API 形状の正負検査 (37 本) は本修正サイクルで差分が無いため、review-001 の実測結果を引き継いだ

## review-001 / second-opinion-code-001 の指摘に対する確認

| # | 出典 | 指摘 | 状態 |
|---|---|---|---|
| 1 | 相方 Major | Android coordinator: factory 例外で利用状態が残る + 復帰経路の lifecycle クラッシュ | **解消**。`resolveFactory` を状態確定より前に置き、`startDisplay()` は `rollbackFailedStart()` 付きの try で囲まれた。復帰経路 (`onHostChanged`) は `abandonContent()` で諦めを控え、通知元へ投げ返さない。instrumented 2 本が固定 |
| 2 | 相方 Major | Android beginUse: prompt cancellation で身分証が失われ閉じ残る | **解消**。`AtomicReference` で確定した身分証を控え、`CancellationException` 経路で `withContext(NonCancellable) { endUse(token) }` へ回す。`HostReadHookSurface` で決定的に競合を起こすテスト 1 本 |
| 3 | 相方 Major | MAUI: `Progress<double>` の非同期 dispatch で最終進捗が失われる | **解消**。`LoadingActionRunner` の `DirectProgress` が同期転送し、完了通知は `finally` で必ず報告の後になる。C# 5 本が順序・スレッド・回数を固定 |
| 4 | 相方 Minor | `scripts/scenario-id-coverage.py` のコメントが規約違反 | **解消**。変更識別子と Decision 通番を落とし、自己完結した説明になった (comment-policy lint 0 件) |
| 5 | ホスト Major | `PB-TR-23` 不安定で全件実行が green にならない | **当該テストは解消**。`waitUntilCancelled` で 4 箇所を待ち化し、7 回の反復で `PB-TR-23` / `PB-TR-12` / `PB-TR-28` / `PB-TR-29` はいずれも 1 度も失敗しなかった。ただし全件実行の green 自体は別要因で未達 (下記 Major) |
| 6 | ホスト Minor | MAUI 互換面の Loading 経路にテストが無い | **解消**。`MauiLoadingPassthroughTests` 6 本 + `MauiLoadingCompletionReportTests` 4 本。色の項目ごとに違う値を与えて取り違えを見る作りになっている |
| 7 | ホスト Minor | `waitForPendingDismissal` のコメントが条件と食い違う | **解消**。3 行のコメントが実際の分岐 (null / 別 Job / 同一 Job) と一致している |
| 8 | ホスト Minor | 回転再取り付けで前の画面の Context を持ち越す | **解消**。`onHostChanged` の KDoc にトレードオフが明記された |
| 9 | ホスト Suggestion | `samples/maui/README.md` の追記が未記録 | **解消** (`deviation.md` 7 件目) |
| 10 | ホスト Suggestion | `LoadingSettings` の反映順の注記 | **解消** (`LoadingSettings.cs` の `<remarks>` 第 3 段落) |
| 11 | ホスト Suggestion | Sample の `render` 再帰 post | **片側のみ解消** (下記 Minor) |
| 12 | ホスト Suggestion | 蒸留への申し送り | **未対応** (下記 Suggestion) |

## 指摘事項

### [🟠 Major] iOS の全件実行が安定して green にならない — ただし原因は検証機の CPU 汚染で、実装では解決できない

**該当箇所**: 検証環境 (`ios/` のテストコードそのものではない)

**問題点**:

`xcodebuild test` を 7 回反復した結果、**4 回 green / 3 回 失敗**だった。

| 回 | 結果 | 所要 | 失敗したテスト |
|---|---|---|---|
| 1 | 失敗 | 100.8s | (ログを末尾のみ保存したため未特定。`出入りの演出` suite) |
| 2〜4 | green | 34.1s / 31.1s / 18.3s | — |
| 5 | 失敗 | 41.5s | `KsDialogsInteropBridgeTests.swift:69`「互換面経由でも結果は1回だけ届く」/ `KsDialogsKmpModelBindingTests.swift:161` |
| 6 | green | 17.0s | — |
| 7 | 失敗 | 47.3s | `KsDialogsInteropBridgeTests.swift:113`「nil を包んだ結果値の報告も失敗として返る」/ `DialogTransitionTests.swift:57`「[PB-TR-03] 添付なしでは両フックとも呼ばれず既定のトランジションで表示・閉鎖される」 |

失敗はすべて `DialogTestWaiting.waitUntil`(既定 5 秒) のタイムアウトであり、`Expectation failed: await ... waitUntil { ... }` の形をしている。失敗するテストは回ごとに入れ替わり、**本変更が触っていない領域 (phase-6 由来の互換面テスト・SwiftUI 中身の提示・レジストリ表示) にも散っている**。

同じ手順を **HEAD (本変更なしの一時 worktree) でも 7 回**回した:

| 対象 | 全件実行の結果 | 備考 |
|---|---|---|
| 本変更の作業ツリー | 3 失敗 / 7 回 | 206 tests / 37 suites |
| HEAD | 1 失敗 / 7 回 | 158 tests / 28 suites。失敗回は 7 テストが同時に `waitForPresentedContainers` / `appeared` でタイムアウト |

HEAD 側の失敗も**同じ形 (待ちのタイムアウトが複数テストで同時多発)** であり、本変更に固有の回帰ではない。

原因として、**検証機に約 5.5 時間前から居座る CPU 負荷プロセス群**を実測した — `yes` が 10 本、それぞれ約 90% CPU を消費し、親プロセスは init (孤児化している)。この状態で load average は 1 分 61 / 5 分 193 / 15 分 205 に達しており、Swift Testing が suite を並列実行する iOS のテストにとって 5 秒の待ちは十分な余裕を持たない。**review-001 の実測 (同日 17 時台) もこの汚染下で行われている**ため、当時の「`PB-TR-23` が 9 回中 3 回失敗」という数値も同じ疑いがかかる。

作業ツリー側の失敗率が HEAD より高いのは、テスト本数が 158 → 206・suite 数が 28 → 37 と増えて並列圧が上がったことで説明がつく (本変更が待ちの構造を壊した証拠は得られなかった)。実際、review-001 が名指しした 4 テスト (`PB-TR-12` / `-23` / `-28` / `-29`) は 7 回とも 1 度も失敗しておらず、待ち化の修正は目的を果たしている。

**この指摘は実装では解決できない**ため、判定を NEEDS_DISCUSSION とした。

**選択肢**:

- (a) **推奨** — 検証機の孤児プロセス (`yes` × 10) を落としてから `ios/` の全件実行を数回回し直し、green が安定することを確認して完了とする。プロセスの停止はレビュアーの権限外なのでオーナーの判断が要る
- (b) 掃除後も失敗が残るなら、`DialogTestWaiting.waitUntil` の既定タイムアウト (5 秒) が現在のテスト規模に対して短すぎる可能性を疑い、テスト基盤の改善を別 change として起票する (本変更のスコープ外)
- (c) 本変更の完了判定からは外し、`deviation.md` に「iOS 全件実行の不安定さは検証機の負荷に起因し、HEAD でも再現する。別途対処」と経緯を残す

いずれにせよ、**「全件実行が green」を無記録のまま飛ばさない**ことが要件である点は review-001 と変わらない。

---

### [🟡 Minor] Sample の再帰 post の打ち切りが、同じ内容を持つ KMP 側 Sample に入っていない

**該当箇所**: `samples/kmp/androidApp/src/main/kotlin/jp/kamusoft/ksdialogs/samples/kmp/android/CustomLoadingCardView.kt:107`

**問題点**:

review-001 の Suggestion を受けて `samples/android/app/src/main/kotlin/jp/kamusoft/ksdialogs/samples/android/CustomLoadingCardView.kt` は `addOnLayoutChangeListener` で 1 回だけ待つ形に直っているが、**同じ役割・ほぼ同じ内容の KMP 側 Sample には元の再帰 post が残っている**:

```kotlin
val trackWidth = progressTrack.width
if (trackWidth == 0) {
    // 初回はまだ配置されていないため、幅が決まってから塗り直す
    progressTrack.post { render(progress) }
    return
}
```

帯の幅が最後まで 0 のままになる経路では、表示が閉じるまで毎フレーム再投函が続く。実害は Sample に閉じるが、cross conventions の Sample パリティ規約が「各プラットフォームで idiomatic なサンプルではなく一字一句同一を採る」と定めており、Android 系 2 ルートの実装が分かれたままなのは規約の意図に反する (差が出たときに本体の仕様差か Sample の書き方の差かを切り分けられなくなる)。

**推奨修正**: `samples/android` 側と同じ `addOnLayoutChangeListener` の形へ揃える (`fillProgressBar` の切り出しを含めて同一にする)。

---

### [🟡 Minor] 提示先復帰時の factory 失敗を、記録を残さず握り潰している

**該当箇所**: `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/LoadingCoordinator.kt:367-373`

**問題点**:

```kotlin
try {
    createContent(host)
} catch (contentFailure: Throwable) {
    // 生成できない中身を次の入れ替わりで作り直しても同じ失敗を繰り返すため、諦めを控える
    abandonContent()
    return
}
```

投げ返さない判断そのものは妥当 (通知元は Activity ライフサイクルのコールバックであり、利用者アプリを巻き込むわけにいかない) で、コメントもトレードオフを説明している。しかし **例外の内容がどこにも残らない**。この経路に落ちた利用者から見えるのは「ローディングを出したのに何も表示されないまま処理だけ走る」で、原因を突き止める手掛かりが 1 つも無い。

本リポジトリには同型の先例がある — `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/DialogTransitionRunner.kt:140` は利用者コード (演出フック) の失敗を握り潰す際に `Log.w(LOG_TAG, "ダイアログの${phase}フックが失敗しました。", failure)` を残しており、iOS 側も `Logger` の warning で同じことをしている。Loading のこの経路だけが無記録なのは一貫していない。

なお、提示先が最初から在る経路では factory の失敗はそのまま呼び出し元へ伝播する (fail-fast) のに対し、提示先が後から現れる経路だけ握り潰しになるという非対称も生まれている。デルタスペックは利用者 factory 自身が投げる場合を規定していない (`LD-CV-04` は未登録 VM のみ) ため仕様違反ではないが、契約の空白を埋める挙動判断であり `deviation.md` に痕跡が無い。

**推奨修正**: `Log.w` で理由を残す (既存の `LOG_TAG` の作法に合わせる)。あわせて `deviation.md` に「提示先の復帰時の中身の生成失敗は、通知元 (Activity ライフサイクル) を巻き込まないよう投げ返さず、表示だけを諦める」を 1 行足す。

---

### [🔵 Suggestion] 蒸留への申し送り (concepts の陳腐化) が未記録のまま

**該当箇所**: `kasane/concepts/cross/conventions/test-execution.md` / `kasane/concepts/cross/conventions/sample-parity.md`

**問題点**: review-001 の Suggestion (件数表と負の検査フラグの更新) は本サイクルでも記録が足されていない。review-001.md 自体が change 配下に残るので蒸留時に読まれる見込みはあるが、対象がもう 1 件増えている:

- `test-execution.md` の件数表: ios 158→206 / android 66→67 / instrumented 139→221 / kmp 57→80 / maui 88→110 / bridge 15→25 / 負の検査 31→37 本
- `sample-parity.md` の文言表: 「現在のデモ項目は 7 件」のまま。本変更で `Default Loading` / `Custom Loading` の 2 件が増え、`Loading...` / `Soon...` / `カスタムローディング` / `結果: 完了` 等の文言も表に無い

**推奨修正**: 実装側で今すぐ直す必要はない (concepts の追随は `ksn-distill` の責務)。両ファイルの更新対象を 1 か所にまとめて申し送りとして残すこと。

---

### [🔵 Suggestion] Swift 互換面に同型テストを置けない事情が記録されていない

**該当箇所**: `maui/macios/native/KsDialogsMauiBridge/` (テストターゲットが存在しない)

**問題点**: review-001 の Minor は「Swift 側に同型のものを置けない事情があるなら、その旨を記録に残す」を求めていた。実際 `maui/macios/native/` にはテストターゲットが 1 つも無く、Dialog 面 (`MauiDialogClosureReportTests` 等) も Kotlin 側だけという既存の構図と同じであるため妥当な帰結だが、`deviation.md` にも証跡にもその一言が無い。読み手は「片側だけ手を抜いた」と誤読しうる。

**推奨修正**: `deviation.md` の `LoadingActionRunner` の項に「Swift 互換面にはテストターゲットが存在せず (Dialog 面も同じ)、両 OS が共有する順序保証は C# 側の `LoadingActionRunnerTests` で担保する」を追記する。

---

### [🔵 Suggestion] `android/` と `maui/android/native/` の Gradle 実行は同時に走らせられない

**該当箇所**: `kasane/concepts/cross/conventions/test-execution.md` (テスト実行手順)

**問題点**: 本レビューで `android/` の `./gradlew test` と `maui/android/native/` の `./gradlew :ksdialogs-maui-bridge:test` を並行実行したところ、後者が `:android:ksdialogs:bundleLibCompileToJarDebug` で失敗した (両者が `android/ksdialogs/build/` を共有し、クラスファイルを取り合う)。逐次に回し直すと 25 / 0 で通る。テスト自体の欠陥ではないが、規約が「黙って空振りする範囲」を列挙している文書である以上、この罠も同じ場所に置く価値がある。

**推奨修正**: 蒸留時に `test-execution.md` へ「`maui/android/native/` は `android/` の複合ビルドを含むため、`android/` のタスクと同時に実行しない」を追記する。

## アクションプラン

1. **[Major]** iOS の全件実行 — 検証機の孤児 CPU 負荷プロセス (`yes` × 10) の停止をオーナーに諮り、掃除後に `ios/` を数回回し直して green の安定を確認する。掃除後も残るなら (b) / (c) のいずれかを選び、選んだ方を `deviation.md` に記録する
2. **[Minor]** `samples/kmp/androidApp/.../CustomLoadingCardView.kt` の再帰 post を `samples/android` 側と同じ形へ揃える
3. **[Minor]** `LoadingCoordinator.onHostChanged` の握り潰しに `Log.w` を足し、挙動判断を `deviation.md` に 1 行残す
4. **[Suggestion]** 蒸留への申し送り (`test-execution.md` の件数・負の検査フラグ、`sample-parity.md` のデモ項目表、Gradle 同時実行の罠) をまとめて残す
5. **[Suggestion]** Swift 互換面にテストターゲットが無い事情を `deviation.md` に追記する

## 確認した観点 (指摘に至らなかったもの)

- **factory 失敗の rollback の網羅性**: `rollbackFailedStart()` は合流数・器・購読・控えた中身・最新値をすべて戻し、世代だけを進めたままにしている。走行中の旧世代の身分証が後から `endUse` しても世代照合で弾かれ、失敗した世代へ後続が合流する経路は見つからなかった。`resolveFactory` (未登録 VM) は状態確定より前で失敗するため `LD-CV-04` の fail-fast は保たれている
- **prompt cancellation 修正の副作用**: `show()` 系は身分証を捨てる API だが、受理直後の取り消しでは `endUse` が走って表示が巻き戻る。`show` → `hide` の対で使う限り `hide()` は `activeCount == 0 && container == null` で早期に戻るため閉じ残り・二重撤去には落ちない。合流中 (activeCount ≥ 2) の取り消しでも 1 件だけ減る。`withContext(NonCancellable)` は外側の dispatcher を引き継ぐだけで、`endUse` 内の `Main.immediate` への切り替えを通っても取り消し不能性は保たれる
- **iOS 側の同型経路**: `LoadingCoordinator.beginUse` は `makeContent` を状態確定より**前**に呼ぶため factory 失敗で状態が残らず、提示先の遅延生成の経路も持たない。Swift の取り消しは協調的で受理後に身分証が失われないため、Android と同じ穴は構造上生じない — `LoadingStartFailureTests.swift` の 2 本がこれを固定している
- **進捗と完了の順序保証 (端から端まで)**: `DirectProgress` の同期転送により、報告は必ず `completion()` より先に発行される。その先も Android は `Handler.post` 同士 (`scope.launch` / `continuation.resume`) の FIFO、iOS は MainActor へ積む順序で保たれる。報告口はもともと任意スレッドから呼べる契約 (`MauiLoadingProgressReport` / Swift の `@Sendable` 報告口) なので、同期転送でスレッド制約に反することもない
- **`LoadingActionRunner` の同梱条件**: 触れたのは新規 1 + gateway 2 = 3 ファイル、公開面は internal のまま、テスト 5 本付き。採用済み Major の修正そのものであり、ksn-core の同梱条件を超えていない
- **切り出しの挙動不変性 (再確認)**: `DialogTransitionRunner.runPresentationPhase(revealContent:)` は「覆いのフェードを仕込む → 中身を見せる → フックを直接 await」という同期区間の順序を移送前と同一に保っている (`revealContent` は非 async のクロージャなので中断点が入らない)。器側の `resolvedTransition` / `resolvedOverlayDuration` / `overlayView` は runner への委譲プロパティに置き換わっただけで、値の意味は変わっていない
- **足場アーティファクトの改変**: `specs/` 6 ファイルと `proposal.md` / `design.md` は未変更。`tasks.md` はチェックの反転のみで、虚偽のチェックは見つからなかった
- **合意済み差分**: `deviation.md` 10 件 (付随修正 6 件を含む) と `ui/brief.md` の合意済み差分は違反として扱っていない。付随修正はいずれも本務と同じ能力内・公開面に触れない・テストで担保されている
- **コメント規約**: 新規・修正されたコメントに変更識別子・議論通番・`kasane/` パスの参照は無い (`comment-policy-lint.py` 737 ファイル 0 件)。`core/ADR-00NN` 形式の参照だけが使われている
- **`scenario-id-coverage.py` の (prefix, 領域) 化**: `evaluate_mirror` が `(prefix_of(id), area_of(id))` で判定するようになり、自己テストに「同じ領域名でも接頭辞が対象外なら検査しない」「LD 系の挙動領域は両 Native にあれば通る」の 2 ケースが足された。`--require-mirror` 実行でも取りこぼしなし
- **セキュリティ / 機微情報**: 修正差分に認証・ネットワーク・永続化の面は無い。3 種の lint はすべて 0 件
