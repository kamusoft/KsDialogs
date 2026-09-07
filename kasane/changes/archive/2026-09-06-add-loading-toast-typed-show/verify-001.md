# 検証結果: add-loading-toast-typed-show (001 回目)

**日付**: 2026-09-06
**判定**: VALID

デルタスペック 6 面の全 Requirement / Scenario (40 件) について、実装とテストの対応を突き合わせた。❌ は 0 件、deviation.md は不在で未記録乖離も検出していない。tasks.md の 15 タスクはすべて対応表の実体を伴っており虚偽チェックなし、足場 (proposal / specs) の逆流もなし。

## 実装アンカー

対応表では次の記号で実装位置を示す (すべてリポジトリ相対)。

| 記号 | 実装位置 |
|---|---|
| **RL-i** | `ios/Sources/KsDialogs/Registry/LoadingViewRegistry.swift:55` (`register(_:viewModel:)`) / `:81` (`entry(forKey:)`)、`ios/Sources/KsDialogs/Registry/LoadingRegistryEntry.swift`、`ios/Sources/KsDialogs/Registry/LoadingViewModelFactory.swift` |
| **RL-a** | `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/LoadingViewRegistry.kt:49` (`registerViewModel`) / `:78` (`entry`)、`android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/LoadingRegistryEntry.kt`、`LoadingViewModelFactory.kt` |
| **RL-m** | `maui/KsDialogs.Maui/Registry/LoadingViewRegistry.cs:62` (`RegisterViewModel`) / `:79` (`StoreViewModelFactory`) / `:88` (`Entry`)、`maui/KsDialogs.Maui/Registry/LoadingRegistryEntry.cs` |
| **RT-i** | `ios/Sources/KsDialogs/Registry/ToastViewRegistry.swift:56` / `:82`、`ios/Sources/KsDialogs/Registry/ToastRegistryEntry.swift`、`ToastViewModelFactory.swift` |
| **RT-a** | `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/ToastViewRegistry.kt:48` / `:77`、`android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/ToastRegistryEntry.kt`、`ToastViewModelFactory.kt` |
| **RT-m** | `maui/KsDialogs.Maui/Registry/ToastViewRegistry.cs:59` / `:76` / `:85`、`maui/KsDialogs.Maui/Registry/ToastRegistryEntry.cs` |
| **SL-i** | 契約 `ios/Sources/KsDialogs/Presentation/KsLoading.swift:69` (show) / `:127` (start)、実装 `ios/Sources/KsDialogs/Presentation/Loading.swift:86` / `:152` / `:167` (`beginTypedUse`) |
| **SL-a** | 契約 `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/KsLoading.kt:89` (show) / `:170` (start)、実装 `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/Loading.kt:57` / `:101` / `:118` (`beginTypedUse`) |
| **SL-m** | 契約 `maui/KsDialogs.Maui/Presentation/IKsLoading.cs:78-135` / `:202-320`、実装 `maui/KsDialogs.Maui/Presentation/Loading.cs:259` (`ShowTypedAsync`) / `:276`・`:295` (`StartTypedAsync`) / `:319` (`ResolveTypedAsync`)、`maui/KsDialogs.Maui/Internals/LoadingPresenter.cs:41` (`ResolveTyped`) |
| **ST-i** | 契約 `ios/Sources/KsDialogs/Presentation/KsToast.swift:71`、実装 `ios/Sources/KsDialogs/Presentation/Toast.swift:85`、要求 `ios/Sources/KsDialogs/Presentation/ToastContentRequest.swift` (`.typed`)、受理後 `ios/Sources/KsDialogs/Presentation/ToastCoordinator.swift:280` |
| **ST-a** | 契約 `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/KsToast.kt:96`、実装 `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/Toast.kt:53`、要求 `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/ToastDisplay.kt:33` (`ToastContentRequest.Typed`)、受理後 `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/ToastCoordinator.kt:193` (`createContent`) |
| **ST-m** | 契約 `maui/KsDialogs.Maui/Presentation/IKsToast.cs:107`、実装 `maui/KsDialogs.Maui/Presentation/Toast.cs:97`、`maui/KsDialogs.Maui/Internals/ToastPresenter.cs:47` (`ResolveTyped`) |
| **DI-m** | `maui/KsDialogs.Maui/Hosting/KsDialogsServiceCollectionExtensions.cs:111`・`:124` (`RegisterForLoading`) / `:150`・`:163` (`RegisterForToast`) |
| **SMP** | `samples/ios/KsDialogsSample/SampleLoadingRegistration.swift`・`SampleToastRegistration.swift`・`SampleMenuModel.swift:210`・`:240`、`samples/android/app/src/main/kotlin/jp/kamusoft/ksdialogs/samples/android/SampleLoadingRegistration.kt`・`SampleToastRegistration.kt`・`MainActivity.kt:209`・`:238`、`samples/maui/KsDialogs.Sample.Maui/SampleMenuPage.xaml.cs:293`・`:326` |

テストファイルは対応表で次の略称を使う。

| 略称 | テストファイル |
|---|---|
| **LT-i** | `ios/Tests/KsDialogsTests/LoadingTypedShowTests.swift` |
| **LT-a** | `android/ksdialogs/src/androidTest/kotlin/jp/kamusoft/ksdialogs/LoadingTypedShowTests.kt` |
| **LT-m** | `maui/KsDialogs.Maui.Tests/LoadingTypedShowTests.cs` |
| **TT-i** | `ios/Tests/KsDialogsTests/ToastTypedShowTests.swift` |
| **TT-a** | `android/ksdialogs/src/androidTest/kotlin/jp/kamusoft/ksdialogs/ToastTypedShowTests.kt` |
| **TT-m** | `maui/KsDialogs.Maui.Tests/ToastTypedShowTests.cs` |
| **CT-a** | `android/ksdialogs-compose/src/androidTest/kotlin/jp/kamusoft/ksdialogs/compose/ComposeTypedShowTests.kt` |

## 対応表

### specs/loading-contract/spec.md

Requirement「Loading レジストリの VM factory スロット」(ADDED)

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| LD-TY-01 VM factory の再登録は View factory を保持する | RL-i / RL-a / RL-m | LT-i:55、LT-a:56 (MAUI はパススルー検証のため spec の責務列挙に無し) | ✅ 一致 |
| LD-TY-02 表示中の再登録は出ている Loading に影響しない | RL-i / RL-a、SL-i / SL-a | LT-i:82、LT-a:86 | ✅ 一致 |

Requirement「Loading の型指定 show」(ADDED)

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| LD-TY-03 生成 → configure → 表示の一連 | SL-i / SL-a / SL-m | LT-i:124、LT-a:118、LT-m:24 | ✅ 一致 |
| LD-TY-04 非同期 configure の完了まで View 生成が始まらない | SL-i / SL-a / SL-m | LT-i:143、LT-a:135、LT-m:51 | ✅ 一致 |
| LD-TY-05 VM factory 未登録は構成ミスとして失敗 | SL-i:172 系 / SL-a / LoadingPresenter.cs:46 | LT-i:172、LT-a:165、LT-m:86 | ✅ 一致 |
| LD-TY-06 configure の失敗は提示に進まず伝播 | SL-i / SL-a / SL-m | LT-i:189、LT-a:184、LT-m:109 | ✅ 一致 |
| LD-TY-07 configure 省略は生成物をそのまま表示 | SL-i / SL-a / SL-m | LT-i:212、LT-a:202、LT-m:139 | ✅ 一致 |
| LD-TY-08 生成した VM にも進捗が転送される | SL-i / SL-a / SL-m | LT-i:226、LT-a:215、LT-m:160 (MAUI は進捗受け口の同一性) | ✅ 一致 |
| LD-TY-14 表示中の型指定 start は合流し View factory を呼ばない | SL-i / SL-a | LT-i:366、LT-a:356 | ✅ 一致 |
| LD-TY-15 非同期 configure 中の別開始で合流側になる | SL-i / SL-a | LT-i:409、LT-a:394 | ✅ 一致 |

Requirement「Loading の型指定 start」(ADDED)

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| LD-TY-09 型指定 start が戻り値を返し合流 1 件を対で数える | SL-i / SL-a / SL-m | LT-i:256、LT-a:235、LT-m:188 | ✅ 一致 |
| LD-TY-10 VM factory 未登録は処理を実行しない | SL-i / SL-a / SL-m | LT-i:280、LT-a:252、LT-m:210 | ✅ 一致 |
| LD-TY-11 置き場所引数が提示に渡る | SL-i / SL-a / SL-m | LT-i:301、LT-a:271、LT-m:235 | ✅ 一致 |
| LD-TY-12 呼び出し時点のエントリで View まで作る | RL-i:81 / RL-a:78 / RL-m:88、SL-* | LT-i:334、LT-a:299、LT-m:257 | ✅ 一致 |
| LD-TY-13 VM factory の失敗は提示に進まず伝播 | SL-a / SL-m | LT-a:331、LT-m:295。**iOS は対象外** — spec が「Swift の VM factory は Dialog と同じく失敗を表明できないため対象外」と明示 | ✅ 一致 |

### specs/toast-contract/spec.md

Requirement「Toast レジストリの VM factory スロット」(ADDED)

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| TS-TY-01 VM factory の再登録は View factory を保持する | RT-i / RT-a / RT-m | TT-i:61、TT-a:55 | ✅ 一致 |

Requirement「Toast の型指定 show」(ADDED)

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| TS-TY-09 任意スレッドから呼べ VM factory と configure は UI スレッド | ST-i / ST-a | TT-i:222、TT-a:254 | ✅ 一致 |
| TS-TY-02 生成 → configure → 表示の一連 | ST-i / ST-a / ST-m | TT-i:90、TT-a:88、TT-m:23 | ✅ 一致 |
| TS-TY-03 VM factory 未登録は呼び出し時点で同期に失敗 | `ios/Sources/KsDialogs/Presentation/Toast.swift:93`、`android/.../Toast.kt:62`、`maui/KsDialogs.Maui/Internals/ToastPresenter.cs:56` | TT-i:114、TT-a:107、TT-m:50 | ✅ 一致 |
| TS-TY-04 configure の失敗は受理後の失敗として 1 枚だけ破棄 | `ios/.../ToastCoordinator.swift:280`、`android/.../ToastCoordinator.kt:162-168`・`:230`、ST-m | TT-i:131、TT-a:125、TT-m:73 | ✅ 一致 |
| TS-TY-05 configure 省略は生成物をそのまま表示 | ST-i / ST-a / ST-m | TT-i:158、TT-a:159、TT-m:107 | ✅ 一致 |
| TS-TY-06 duration と置き場所の引数が効く | ST-i / ST-a / ST-m | TT-i:171、TT-a:173、TT-m:128 | ✅ 一致 |
| TS-TY-07 呼び出し時点のエントリで View まで作る | RT-i:82 / RT-a:77 / RT-m:85、`android/.../ToastCoordinator.kt:303` (受理後にレジストリを引き直さない) | TT-i:195、TT-a:191、TT-m:151 | ✅ 一致 |
| TS-TY-08 VM factory の失敗は受理後の失敗として 1 枚だけ破棄 | `android/.../ToastCoordinator.kt:162-168`、ST-m | TT-a:221、TT-m:188。**iOS は対象外** (spec の明示どおり) | ✅ 一致 |

### specs/ios-native/spec.md

Requirement「Swift 公開面の Loading / Toast 型指定 show」(ADDED)

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| LD-YI-01 Loading 公開面の正の compile 検査 | RL-i、SL-i | `ios/Tests/KsDialogsTests/LoadingApiSurfaceCompileChecks.swift:168`・`:178`・`:199` | ✅ 一致 |
| TS-YI-01 Toast 公開面の正の compile 検査 | RT-i、ST-i | `ios/Tests/KsDialogsTests/ToastApiSurfaceCompileChecks.swift:146`・`:156` | ✅ 一致 |
| LD-YI-02 SwiftUI 登録でも型指定 show が同じに働く | RL-i:40 (SwiftUI 登録面) + SL-i | LT-i:447 | ✅ 一致 |
| TS-YI-02 SwiftUI 登録でも Toast の型指定 show が同じに働く | RT-i:41 + ST-i | TT-i:256 | ✅ 一致 |

### specs/android-native/spec.md

Requirement「Kotlin 公開面の Loading / Toast 型指定 show」(ADDED)

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| LD-YA-01 Loading 公開面の正の compile 検査 | RL-a、SL-a | `android/api-surface-check/src/main/kotlin/jp/kamusoft/ksdialogs/apicheck/LoadingApiSurfaceChecks.kt:155`・`:163`・`:179` | ✅ 一致 |
| TS-YA-01 Toast 公開面の正の compile 検査 | RT-a、ST-a | `android/api-surface-check/src/main/kotlin/jp/kamusoft/ksdialogs/apicheck/ToastApiSurfaceChecks.kt:116`・`:124` | ✅ 一致 |
| LD-YA-02 Compose 登録でも型指定 show が同じに働く | RL-a + `android/ksdialogs-compose/` の `registerCompose` (追加 API なし) | CT-a:55 | ✅ 一致 |
| TS-YA-02 value class の VM は Toast の型指定 show で拒否される | `android/.../ToastViewRegistry.kt:53`、`android/.../Toast.kt:59` (`requireReferenceTypeViewModel`) | TT-a:288 | ✅ 一致 |
| TS-YA-03 Compose 登録でも Toast の型指定 show が同じに働く | RT-a + `registerCompose` | CT-a:78 | ✅ 一致 |
| LD-YA-03 value class の VM は Loading の登録と型指定 show / start で拒否 | `android/.../LoadingViewRegistry.kt:54`、`android/.../Loading.kt:123` | LT-a:437 | ✅ 一致 |

### specs/maui-binding/spec.md

Requirement「C# 公開面の Loading / Toast 型指定 show」(ADDED)

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| LD-YM-01 Loading 公開面の正の compile 検査とオーバーロード束縛 | RL-m、SL-m (`IKsLoading.cs` の署名表どおり 6 本) | `maui/KsDialogs.Maui.ApiSurfaceCheck/LoadingTypedShowApiSurfaceChecks.cs:43`〜`:196` (11 本。既存のメッセージ入口・インスタンス渡し・インライン factory 版の非退行も含む) | ✅ 一致 |
| TS-YM-01 Toast 公開面の正の compile 検査とオーバーロード束縛 | RT-m、ST-m (`IKsToast.cs:107` の 1 本) | `maui/KsDialogs.Maui.ApiSurfaceCheck/ToastTypedShowApiSurfaceChecks.cs:25`〜`:83` (8 本) | ✅ 一致 |

Requirement「1 行登録の VM factory 自動配線 (Loading / Toast)」(ADDED)

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| LD-YM-02 1 行登録だけで Loading の型指定 show が有効になる | DI-m (`:111`・`:124`) | `maui/KsDialogs.Maui.Tests/LoadingDependencyInjectionTests.cs:91` | ✅ 一致 |
| TS-YM-02 1 行登録だけで Toast の型指定 show が有効になる | DI-m (`:150`・`:163`) | `maui/KsDialogs.Maui.Tests/ToastDependencyInjectionTests.cs:70` | ✅ 一致 |

### specs/samples/spec.md

Requirement「Custom Loading / Custom Toast デモの登録経路を型指定 show にする」(ADDED)

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| LD-YS-01 Custom Loading の通し (型指定経路) | SMP (ios / android は手動登録に VM factory 登録を追加、maui は 1 行登録のまま) | 自動テスト無し (scenario-id-coverage の除外 ID に登録済み — `scripts/scenario-id-coverage.py:95`)。証跡: `verification/sample-walkthrough/notes.md` の観察結果表と `<組>-01〜03` の PNG 12 枚 (ios / android / maui-ios / maui-android の 4 組) | ✅ 一致 |
| TS-YS-01 Custom Toast の通し (型指定経路) | SMP | 同上。証跡: `verification/sample-walkthrough/notes.md` と `<組>-04` の PNG 4 枚 | ✅ 一致 |

samples の証跡は本セッションで notes.md の記述と PNG の一覧・命名規則の一致を確認した。kmp ルートは spec が対象外と明示しており、notes.md は archive の add-loading / add-toast の通しを基準として引用している (再撮影しない旨も spec に明示)。

## 追加検査

### tasks.md

全 15 タスクが `[x]`。対応表と突き合わせて**虚偽チェックなし**。1.4 / 2.4 / 3.5 が列挙する Scenario 範囲 (iOS の LD-TY-13・TS-TY-08 対象外、MAUI は facade 責務分のみ) は spec の記述と一致しており、対応表の欠落もこの範囲どおり。3.3 の「compile 検査を最初に書く」は成果物からは順序を検証できないが、検査そのもの (LD-YM-01 / TS-YM-01) は存在する。

### 逆流検査

`kasane/changes/add-loading-toast-typed-show/specs/` と `proposal.md` の最終更新は commit 36500f8 (実装着手前の改名追随) で、以後の変更なし。作業ツリーで変更されている change 配下のファイルは `tasks.md` のみで、差分は 15 行すべてが `- [ ]` → `- [x]` のチェックのみ。**逆流なし**。

### deviation.md と未記録乖離

deviation.md は不在 (乖離の記録なし)。対応表に ❌ が無いため未記録の欠落・乖離は無い。Scenario に直接対応しない diff は次の 4 群で、いずれも Requirement の実装・検証に必要な範囲に収まると判定した (付随修正としての記録も不要):

1. `kmp/ksdialogs-kmp/src/androidHostTest/kotlin/jp/kamusoft/ksdialogs/kmp/AndroidLoadingGatewayContractTests.kt:112` / `AndroidToastGatewayContractTests.kt:68` — 委譲面の test double に新メンバーの stub を追加。契約 (`KsLoading` / `KsToast`) に抽象メンバーが増えたことの直接の帰結
2. `maui/KsDialogs.Maui.Tests/Support/RecordingTraceListener.cs` (新規) と `maui/KsDialogs.Maui.Tests/BridgeContentSupplyTests.cs` の該当クラス削除 — TS-TY-04 / TS-TY-08 の警告検証で使うため既存の入れ子クラスを Support へ移動しただけ (挙動不変)
3. `ios/Tests/KsDialogsTests/Support/` と `android/ksdialogs/src/androidTest/.../support/` の観測用ヘルパ追加・拡張 — Scenario テストの成立に必要
4. `scripts/scenario-id-coverage.py:95-96` に LD-YS-01 / TS-YS-01 の除外 ID を理由つきで追加 — tasks 4.2 が明示的に指示

**残論点 (verify の判定には影響しない)**: 1 の stub 追加が示すとおり、型指定 API は 3 形態とも公開 protocol / interface の抽象メンバーとして増えており、既存の準拠型 (利用者の fake / adapter) は再コンパイル時に実装の追加を要求される。これは second-opinion-code-001 の Major として提起され、突き合わせで「Dialog の型指定 show (core/ADR-0019〜0021) と同型」を理由に降格された論点である。デルタスペックは既定実装 (protocol extension / default interface method) を要求していないため spec との乖離ではなく ❌ にしないが、`proposal.md:31` の「破壊的変更なし」の解釈としてオーナーの確認対象として残っている。

### UI 変更

`ui/` アーティファクトは無い (本 change は公開 API の追加で、画面の見た目は変えない)。samples の見た目が変わらないことが要件であり、`verification/sample-walkthrough/notes.md` がその確認結果を記録している。承認モックのゲートは適用外。

### テストの実行

本セッションでは全ルートの再実行は行っていない (コンテキストパッケージの制約による)。自分で回したのは次の 4 つ:

- `android/` の Kotlin コンパイル (`:ksdialogs:compileDebugKotlin` / 両モジュールの `compileDebugAndroidTestKotlin`) → exit 0
- `python3 scripts/scenario-id-coverage.py --require-mirror` → 本 change の ID は未網羅 0・両 Native ミラー OK (残る未網羅 20 件はすべて別 change `add-kmp-typed-show` の ID)
- `python3 scripts/local-path-lint.py` / `identity-lint.py` → 0 件
- `python3 scripts/comment-policy-lint.py --advisory` → 禁止 0 件

件数はパッケージ提示の実測値を前提にした: ios 275 / android JVM 67 / android instrumented 333 × 2 台 (API 33 / 36) / kmp 96 / maui 153 + 30 + 6。`kasane/handbook/cross/test-execution.md:19-27` の 7 ビルドルートはすべて実行済みで、全件成功。

**注記**: android instrumented は API 33 / 36 の 2 台のみで、`kasane/handbook/cross/test-execution.md:80` が定める「対象 API レベルをそれぞれ1台ずつ」を文字どおりには満たしていない (API 29 でしか走らない `PB_SB_04` が未実行)。未実行の 1 本は Dialog の旧経路システムバーで、本 change の Scenario とは無関係のため対応表の判定には影響しない。詳細は `review-002.md` の Minor 1 を参照。

## 判定

**VALID** — 全 40 Scenario が「✅ 一致」。虚偽チェックなし、逆流なし、未記録乖離なし、実行された範囲のテストは全件成功。
