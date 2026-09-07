# 一致検証: add-kmp-typed-show (001 回目)

**日付**: 2026-09-07
**判定**: VALID

デルタスペック 2 本 (kmp-facade / samples) の全 Requirement / Scenario を実装とテストに突き合わせた。❌ は 0 件。tasks.md の虚偽チェックなし、足場 (proposal / specs / exploration) の逆流なし、テストは全件成功。deviation.md は存在せず、対応表の全行が ✅ のため未記録乖離もない。

## 実行した検証

| 検証 | コマンド | 結果 |
|---|---|---|
| kmp 全件 | `cd kmp && ./gradlew allTests --rerun-tasks` | BUILD SUCCESSFUL。**147 tests / 0 failures / 0 skipped** (iosSimulatorArm64Test 73 + testAndroidHostTest 74) |
| 階層化 metadata compile | `cd kmp && ./gradlew :ksdialogs-kmp:testAndroidHostTest :ksdialogs-kmp:compileIosMainKotlinMetadata :ksdialogs-kmp:compileCommonMainKotlinMetadata --rerun-tasks` | BUILD SUCCESSFUL (lessons/inbox の `kmp-completion-must-run-hierarchical-metadata-compile` に対応) |
| consumer 側 metadata + Android Sample | `cd samples/kmp && ./gradlew :shared:compileCommonMainKotlinMetadata :androidApp:assembleDebug --rerun-tasks` | BUILD SUCCESSFUL |
| 負の compile 検査 (置換分) | `cd kmp && ./gradlew :api-surface-check:compileKotlinIosSimulatorArm64 -Pksdialogs.negativeCheck.toastRegistration --rerun-tasks` | 期待どおり BUILD FAILED。診断は `Unresolved reference 'register'.` 1 件 (handbook の表と一致) |
| 負の compile 検査 (新設分) | 同上 `-Pksdialogs.negativeCheck.loadingRegistration` | 期待どおり BUILD FAILED。診断は `Unresolved reference 'register'.` 1 件 (同上) |
| Scenario ID 網羅 | `python3 scripts/scenario-id-coverage.py` | 「未網羅なし」(exit 0)。PB-KS-01 / LD-KS-01 / TS-KS-01 は除外 ID として理由つきで表示される |
| 標準 lint | `scripts/comment-policy-lint.py` / `local-path-lint.py` / `identity-lint.py` / `doc-structure-lint.py --paths kasane/handbook/cross/test-execution.md` | いずれも違反 0 件 |

ios/ と android/ の回帰実行は行っていない (diff が `ios/` `android/` のファイルを 1 つも含まないため。tasks 4.2 の「回帰の有無の確認」に相当)。

## 対応表 — specs/kmp-facade/spec.md

### Requirement: 共有コードのレジストリの VM factory 登録口 (ADDED)

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| PB-KT-01 既定エントリと契約 interface 経由で VM factory のレジストリを共有する | `kmp/ksdialogs-kmp/src/commonMain/kotlin/jp/kamusoft/ksdialogs/kmp/DialogViewRegistry.kt:26` / `LoadingViewRegistry.kt:26` / `ToastViewRegistry.kt:26` (公開の登録口)、`DialogGateway.kt:53` / `LoadingGateway.kt:47` / `ToastGateway.kt:26` (レジストリの供給元)、`Loading.android.kt:9` / `Toast.android.kt:9` / `Loading.ios.kt:14` / `Toast.ios.kt:14` (既定エントリの単一インスタンス) | `kmp/ksdialogs-kmp/src/commonTest/kotlin/jp/kamusoft/ksdialogs/kmp/TypedShowTests.kt`「PB-KT-01 …共有する」「PB-KT-01 …どの入口から取っても同じもの」、`LoadingTypedShowTests.kt`「PB-KT-01 …」、`ToastTypedShowTests.kt`「PB-KT-01 …」 | ✅ 一致 |
| PB-KT-02 VM factory の再登録は後勝ちで、表示中の show には影響しない | `ViewModelFactoryStore.kt:20` (put は CAS で後勝ち)、`:34` (create は 1 回の load で解決) | `TypedShowTests.kt`「PB-KT-02 …」(非同期 configure 中に再登録し、進行中は最初の産物・次回は新しい産物であることを確認) | ✅ 一致 |
| PB-KT-11 Android で Native 側に登録した VM factory は共有コードの型指定 show から見えない | `DialogGateway.kt:70` / `LoadingGateway.kt:102` / `ToastGateway.kt:40` がいずれも共有コードの表だけを引く | `kmp/ksdialogs-kmp/src/androidHostTest/kotlin/jp/kamusoft/ksdialogs/kmp/TypedShowRegistryScopeTests.kt` の PB-KT-11 3 本 (Dialog / Loading show+start / Toast)。共有コードのメッセージ完全一致 + `cause == null` で「Native の失敗の載せ替えでない」ことまで見ている | ✅ 一致 |
| PB-KT-12 並行する再登録の途中でも解決は原子的なスナップショットになる | `ViewModelFactoryStore.kt:17` (不変 Map を `AtomicReference` で保持)、`:20-26` (CAS ループでの置換) | `TypedShowTests.kt`「PB-KT-12 …」(`Dispatchers.Default` 上で 200 回の再登録と 200 回の show を並走させ、全 show が登録済みいずれかの産物を得ることを確認) | ✅ 一致 |

### Requirement: 共有コードの型指定 show (Dialog / Loading / Toast) (ADDED)

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| PB-KT-03 Dialog の型指定 show が生成 → configure → 結果の一連で動く | `KsDialog.kt:56` (契約)、`DialogGateway.kt:70-78` (前段) | `TypedShowTests.kt`「PB-KT-03 …」(configure の状態・placement・宣言結果型の completed を確認) | ✅ 一致 |
| PB-KT-04 非同期 configure の完了まで委譲面に渡らない | `DialogGateway.kt:72-74` (configure を await してから `show(viewModel, …)`) | `TypedShowTests.kt`「PB-KT-04 …」(`runCurrent()` 時点で委譲面が空、gate 解放後に 1 件) | ✅ 一致 |
| PB-KT-05 VM factory 未登録の型指定 show は構成ミスとして失敗する | `ViewModelFactoryStore.kt:35-38` (`DialogException`) | `TypedShowTests.kt`「PB-KT-05 …」/ `LoadingTypedShowTests.kt`「PB-KT-05 …」(show と start の両方) / `ToastTypedShowTests.kt`「PB-KT-05 …」 | ✅ 一致 |
| PB-KT-06 VM factory と configure の失敗は提示に進まず伝播する | `DialogGateway.kt:71-73` / `LoadingGateway.kt:102-109` / `ToastGateway.kt:44-46` (いずれも生成・configure の後で初めて委譲面を呼ぶ) | `TypedShowTests.kt`「PB-KT-06 VM factory の失敗…」「PB-KT-06 configure の失敗とキャンセル…」(`CancellationException` を含む)、`ToastTypedShowTests.kt`「TS-KT-01 …」(Toast の同期伝播) | ✅ 一致 (Loading の例外伝播は未検証 — review-001 の Minor 3) |
| PB-KT-07 VM factory と configure は呼び出し元の文脈で実行される | `DialogGateway.kt:71-72` (dispatcher への hop なし) | `TypedShowTests.kt`「PB-KT-07 …」(dispatch 回数を数える `CountingDispatcher` と configure 内の `ContinuationInterceptor` で判定) | ✅ 一致 |
| PB-KT-13 VM factory の生成物の型が登録キーと違えば型不一致として失敗する | `ViewModelFactoryStore.kt:42-49` | `TypedShowTests.kt` / `LoadingTypedShowTests.kt` / `ToastTypedShowTests.kt` の各「PB-KT-13 …」(3 機能とも、失敗説明に生成物の型が出ることと委譲面が呼ばれないことを確認) | ✅ 一致 |
| PB-KT-14 型指定 show は Swift / ObjC から見えない | `KsDialog.kt:55` / `KsLoading.kt:70`・`:145` / `KsToast.kt:84` の `@HiddenFromObjC`、`kmp/ksdialogs-kmp/build.gradle.kts:83-93` (ヘッダを作らせて場所を渡す配線) | `kmp/ksdialogs-kmp/src/iosTest/kotlin/jp/kamusoft/ksdialogs/kmp/ObjCApiSurfaceTests.kt`「PB-KT-14 …」。生成ヘッダを本検証でも確認 — `showViewModelClass:` / `startViewModelClass:` の宣言は 0 件、実例渡し show と `registry` は存在 | ✅ 一致 |
| PB-KT-08 configure 省略の型指定 show は VM factory の生成物をそのまま渡す | `DialogGateway.kt:72` (`configure?.invoke`) | `TypedShowTests.kt`「PB-KT-08 …」(`assertSame` で同一インスタンスを確認) | ✅ 一致 |
| LD-KT-02 Loading の型指定 show / start が既存のインスタンス渡し経路に流れる | `LoadingGateway.kt:61-65`・`:90-95`・`:102-109` | `LoadingTypedShowTests.kt`「LD-KT-02 …」(configure 済み VM・placement 2 件・start の戻り値・スコープ形の委譲回数) | ✅ 一致 |
| TS-KT-01 Toast の型指定 show が同期に委譲面へ流れ、失敗は同期に伝播する | `ToastGateway.kt:40-48` (suspend でない前段) | `ToastTypedShowTests.kt`「TS-KT-01 …」(VM・durationMs・placement の受け渡しと、configure 例外の同期伝播 + 委譲面が呼ばれないこと) | ✅ 一致 |
| PB-KT-09 両 OS の gateway が型指定 show の VM をインスタンス渡しと同じ経路で Native へ渡す | Android: `AndroidDialogGateway.kt` / `AndroidLoadingGateway.kt` / `AndroidToastGateway.kt` (Native のインスタンス渡し API のみを呼ぶ)。iOS: `IosDialogGateway.kt` / `IosLoadingGateway.kt` / `IosToastGateway.kt` (互換面 bridge へ) | androidHostTest: `AndroidDialogGatewayContractTests.kt`「PB-KT-09 …」/ `AndroidLoadingGatewayContractTests.kt`「PB-KT-09 …」/ `AndroidToastGatewayContractTests.kt`「PB-KT-09 …」(Native の型指定 API を使うと double が `UnsupportedOperationException` を投げる形で経路を固定)。iosTest: `InteropBridgeContractTests.kt`「PB-KT-09 …」(提示先不在の失敗で解決成立を判定) / `InteropLoadingBridgeContractTests.kt`「PB-KT-09 …」(進捗が生成 VM へ届く) / `InteropToastBridgeContractTests.kt`「PB-KT-09 …」(View factory へ渡ったのが生成 VM そのもの) | ✅ 一致 |

### Requirement: 共有コードの公開面の compile 検査 (ADDED)

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| TS-KT-02 共有コードのレジストリに View factory の登録 API が無い (負の compile 検査) | `kmp/api-surface-check/build.gradle.kts:57` (`loadingRegistration` の配線)、`kmp/api-surface-check/src/negativeCheckToastRegistration/kotlin/jp/kamusoft/ksdialogs/kmp/apicheck/RejectsToastRegistration.kt:14`、`kmp/api-surface-check/src/negativeCheckLoadingRegistration/kotlin/jp/kamusoft/ksdialogs/kmp/apicheck/RejectsLoadingRegistration.kt:14` | 本検証で 2 本を個別に実行し、それぞれ `Unresolved reference 'register'.` 1 件で失敗することを実測。`kasane/handbook/cross/test-execution.md:243-244` の期待診断表と一致 | ✅ 一致 |
| PB-KT-10 共有コード公開面の正の compile 検査 | `kmp/api-surface-check/src/commonMain/kotlin/jp/kamusoft/ksdialogs/kmp/apicheck/DialogApiSurfaceChecks.kt:39-60` / `LoadingApiSurfaceChecks.kt:71-103` / `ToastApiSurfaceChecks.kt:56-76` (ラムダ / コンストラクタ参照の登録・configure あり / なし・suspend configure・placement / durationMs・型指定 start) | `allTests` に含まれる `:api-surface-check:compileKotlinIosSimulatorArm64` が本検証で成功 | ✅ 一致 |

**旧 Scenario との関係**: 置き換え対象の負検査が持っていた ID `TS-KM-03` (archive の add-toast の kmp-facade spec) は、`negativeCheckToastStyleType` / `ToastStyleProperty` / `ToastHide` / `ToastShowResult` の 4 本が引き続き名乗っており、網羅検査は成立している。「レジストリのハンドルを持たない」面だけが本 change の spec で明示的に置き換えられている (削除ではない)。

## 対応表 — specs/samples/spec.md

### Requirement: kmp ルートの登録経路デモを共有コードの型指定 show にする (ADDED)

| Scenario | 実装 | テスト / 証跡 | 状態 |
|---|---|---|---|
| PB-KS-01 Model Dialog の通し (kmp・型指定経路) | `samples/kmp/shared/src/commonMain/kotlin/jp/kamusoft/ksdialogs/samples/kmp/SamplePresenter.kt:50-56` (VM factory の登録) / `:138-140` (型指定 show + configure)、`ModelDialogViewModel.kt` (無引数生成 + 可変 message) | `kasane/changes/add-kmp-typed-show/verification/sample-walkthrough/`: `android-01`/`02`、`ios-01`/`02` と `notes.md`。除外 ID 登録は `scripts/scenario-id-coverage.py:99` | ✅ 一致 |
| LD-KS-01 Custom Loading の通し (kmp・型指定経路) | `SamplePresenter.kt:238` (`loading.start(CustomLoadingViewModel::class) { … }`) | `android-03`/`04`、`ios-03`/`04` | ✅ 一致 |
| TS-KS-01 Custom Toast の通し (kmp・型指定経路) | `SamplePresenter.kt:271-275` (`viewModelClass` + `configure`)、`CustomToastViewModel.kt` | `android-05`/`06`、`ios-05`/`06` | ✅ 一致 |

証跡は 12 枚すべて md5 が相異し、notes.md の各行が実体ファイルを指せることを本検証でも抜き取り確認した (android-01 / android-03 / android-04 / ios-05 を実際に開き、文言・進捗率・2 枚の重なり・結果表示が記述どおりであることを確認)。文言は sample-parity 規約の文言表と一致し、デモ項目・安定デモ ID の追加はない。

## 追加検査

| 検査 | 結果 |
|---|---|
| tasks.md の全タスク完了 / 虚偽チェックの有無 | 1.1〜4.2 の 9 件すべて `[x]`。対応表と突き合わせて未実装のチェックは無い。4.1 が求める件数表の更新 (`kasane/handbook/cross/test-execution.md`) と 2.4 が求める負検査表の更新も diff に含まれ、実測値と一致する |
| 逆流検査 (足場の書き換え) | `git diff HEAD -- kasane/changes/add-kmp-typed-show/proposal.md specs exploration.md` が空。書き換わっているのは tasks.md のチェックボックスのみ |
| 未記録乖離 | deviation.md は存在しない。対応表に ❌ が無く、diff 中で Scenario に紐づかない変更 (build.gradle.kts のヘッダ配線・scenario-id-coverage の除外 ID・handbook の件数表と負検査表・api-surface-check の Consumer VM を可変化する調整) はいずれも tasks の該当項目が明示している範囲内のため、未記録乖離なし |
| 付随修正 | deviation.md に `[付随修正]` 行なし。診断すべき付随修正も見つからない |
| UI 変更 | ui/ アーティファクトを持たない change (見た目の変更なし)。sample の観察可能な挙動が不変であることは証跡で確認済み |
| テスト全件成功 | 上記「実行した検証」のとおり実行して確認 |

## 判定

**VALID** — 全 Scenario が ✅ 一致。❌ 0 件。虚偽チェックなし、逆流なし、テスト全件成功。

なお品質面の所見 (公開 ObjC 面への `registerViewModel` の露出、Loading の例外伝播テストの欠落など) は一致検証の対象外のため `review-001.md` に分けて記載した。
