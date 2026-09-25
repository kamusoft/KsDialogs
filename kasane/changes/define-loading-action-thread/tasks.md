# Tasks: define-loading-action-thread

作業の前に handbook cross/ の `always` の文書 (comment-policy) と、担当範囲に当たる文書 (test-execution・sample-parity・diagnostic-message-language) を本文まで読む。android/ 系の Gradle ビルドは同時に走らせない (test-execution)。

## 1. iOS Native

- [ ] 1.1 `KsLoading` の protocol 要件 5 本・extension の省略形 8 本・`Loading` の実装 5 本・`runScope` の action の型に `@MainActor` を付ける (→ Requirement: Swift の action の型と、UI スレッド外で始める指定)
- [ ] 1.2 公開 doc コメントに、既定は UI スレッドで始まること、`@concurrent` で UI スレッド外になること、isolation を持つ関数を名前で渡すとその isolation が優先されることを書く (→ 同上)
- [ ] 1.3 契約の挙動テスト LD-TH-01〜08 を Swift Testing で書く (`ios/Tests/KsDialogsTests/`、判定は `Support/DialogTestThread.swift` の `isMainThread()`) (→ Requirement: スコープ形の action が始まるスレッド)
- [ ] 1.4 `LoadingApiSurfaceCompileChecks.swift` に LD-HI-01 の書き方を足す (→ Scenario: LD-HI-01)
- [ ] 1.5 関数を名前で渡す例外のテスト LD-HI-02 を書く (→ Scenario: LD-HI-02)
- [ ] 1.6 既存のテストのうち、action の中で `MainActor.run` を使っているものは、そのまま通ることを確かめる (直さなくてよい)

## 2. Android Native

- [ ] 2.1 `ksdialogs-core` に `enum class LoadingActionThread { MAIN, BACKGROUND }` を足す (→ Requirement: Kotlin の `LoadingActionThread` 引数)
- [ ] 2.2 `KsLoading` の `start` 4 本と `Loading` の実装、Compose の `startCompose` に `actionThread` 引数を足す (→ 同上)
- [ ] 2.3 `Loading.runScope` の action 呼び出しを、指定に応じて `withContext(Dispatchers.Main.immediate)` / `withContext(Dispatchers.Default)` で包む (→ Requirement: スコープ形の action が始まるスレッド)
- [ ] 2.4 KDoc に既定と指定の意味を書く (→ 同上)
- [ ] 2.5 契約の挙動テスト LD-TH-01〜08 を instrumented で書く (`android/ksdialogs-core/src/androidTest/`) (→ Requirement: スコープ形の action が始まるスレッド)
- [ ] 2.6 `android/api-surface-check` に LD-HA-01 の正の compile 検査を足す。負の検査 `RejectsOptionsArgumentOnLoadingShow` の説明で「引数で渡せるのは placement だけ」という前提になっている箇所があれば、`actionThread` を踏まえた文言に直す (→ Scenario: LD-HA-01)
- [ ] 2.7 Compose の LD-HA-02 を instrumented で書く (`android/ksdialogs/src/androidTest/`) (→ Scenario: LD-HA-02)

## 3. MAUI

- [ ] 3.1 `LoadingActionThread` enum を足し、`IKsLoading` / `Loading` の `StartAsync` 10 本に `actionThread` 引数を足す (→ Requirement: C# の `LoadingActionThread` 引数と action の振り分け)
- [ ] 3.2 指定を `ILoadingGateway.RunAsync` → 各 Platform の gateway → `LoadingActionRunner` まで引き回す (→ 同上)
- [ ] 3.3 `LoadingActionRunner` に「UI スレッドで呼ぶ口」の差し替え口を設け、`Main` はその口 (本番は `MainThread.InvokeOnMainThreadAsync`)、`Background` は `Task.Run` で action を呼ぶ。順序の保証 (同期の報告転送・終了をちょうど 1 回・失敗を預ける) を保つ (→ 同上)
- [ ] 3.4 `HostlessLoadingGateway.RunAsync` は指定を受け取るが切り替えず、今どおりその場で実行する (→ Scenario: LD-HM-06)
- [ ] 3.5 iOS 互換面 (`maui/macios/native/KsDialogsMauiBridge/MauiLoadingBridge.swift`) を 1.1 の型の変更に合わせ、`start` に渡すクロージャに `@concurrent` を付ける (design Risks)
- [ ] 3.6 XML ドキュメントコメントに既定と指定の意味を書く (→ Requirement: C# の `LoadingActionThread` 引数と action の振り分け)
- [ ] 3.7 LD-HM-01〜06 のテストを書く (`maui/KsDialogs.Maui.Tests/`) (→ Scenario: LD-HM-01〜06)

## 4. KMP

- [ ] 4.1 commonMain に `enum class LoadingActionThread { MAIN, BACKGROUND }` を足し、`KsLoading` の `start` 3 本・`GatewayKsLoading`・`LoadingGateway` に指定を通す (→ Requirement: 共有コードの `LoadingActionThread` 引数)
- [ ] 4.2 `AndroidLoadingGateway` で Native の `LoadingActionThread` に写して渡す (→ Scenario: LD-HK-02)
- [ ] 4.3 `IosLoadingGateway.runScope` の action 呼び出しを、`withContext(Dispatchers.Main)` / `withContext(Dispatchers.Default)` で包む。`Dispatchers.Main.immediate` にするかは、iosTest で確かめてから決める (design Risks) (→ Scenario: LD-HK-03・04)
- [ ] 4.4 LD-HK-01 (commonTest)・LD-HK-02 (androidHostTest)・LD-HK-03・04 (iosTest、`support/MainLoopPump.kt` の `runPumpingMainLoop`) を書く。commonTest の Fake (`FakeKsLoading` など) を新しい署名に合わせる (→ Scenario: LD-HK-01〜04)
- [ ] 4.5 KMP の iOS フレームワークを生成し、書き出された Objective-C ヘッダの `start` に `actionThread` 引数が現れることを確かめて、verification の証跡に残す (proposal Impact の「KMP を Swift から直接呼ぶ場合のソース互換」の確認。second-opinion-spec-001 の指摘 4)

## 5. Sample

- [ ] 5.1 iOS Sample (`samples/ios/KsDialogsSample/SampleMenuModel.swift`) から、「処理は MainActor の外で動く」前提のコメントと値の先取りを外す (→ Requirement: iOS Sample のスコープ形を新しい既定に合わせる)
- [ ] 5.2 4 ルートの `SampleText` に `結果: 処理中` を同じ値で足し、`Default Loading` の action の最初の文で結果表示を直接更新する (UI スレッドへ明示的に移す書き方は使わない) (→ Requirement: Default Loading のデモで、action の中から結果表示を更新する)
- [ ] 5.3 LD-HS-01・LD-HS-02 を `scripts/scenario-id-coverage.py` の除外 ID に理由付きで登録する (→ Scenario: LD-HS-01・02)
- [ ] 5.4 iOS Sample を Simulator で動かし、`Default Loading` と `Custom Loading` の通しを verification の証跡として残す (撮影手順は config.yaml の `ui.screenshot`) (→ Scenario: LD-HS-01)
- [ ] 5.5 4 ルート (MAUI は iOS と Android の両方) で、`Default Loading` の処理中に `結果: 処理中` が出て落ちないこと、完了で `結果: 完了` になることを、刻み間隔を延ばす起動引数 (`loading-step-interval-ms`) で撮って verification の証跡に残す。MAUI iOS は Debug 構成で確かめる。可能なら、変更前の MAUI iOS で同じ操作が失敗することも記録する (→ Scenario: LD-HS-02)

## 6. 完了の確認

- [ ] 6.1 全ルートの全件実行を handbook cross/test-execution.md の表どおりに回し、ルートごとに件数を記録する (ios / android / android instrumented / kmp / maui の `dotnet test` / MAUI の Android 互換面 / MAUI の iOS 互換面)
- [ ] 6.2 `python3 scripts/scenario-id-coverage.py` で未網羅が無いことを確かめる (LD-TH は `--require-mirror` でも確かめる)
- [ ] 6.3 標準 lint (comment-policy など) を通す
