# 一致検証: rename-dialog-contract-singular (001 回目)

**日付**: 2026-09-06
**判定**: VALID

対象デルタスペック: `specs/dialog-contract/spec.md`、`specs/user-docs/spec.md`
deviation.md: **不在** (合意済みの乖離なし)。対応表に ⚠️ は現れない。

## 対応表: dialog-contract

### Requirement: 契約型名の規則

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 3 機能の契約名が同じ規則で読める | `ios/Sources/KsDialogs/Presentation/KsDialog.swift:7` / `KsLoading.swift` / `KsToast.swift`、`android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/KsDialog.kt:12` / `KsLoading.kt:15` / `KsToast.kt:18`、`kmp/ksdialogs-kmp/src/commonMain/kotlin/jp/kamusoft/ksdialogs/kmp/KsDialog.kt:12` / `KsLoading.kt:19` / `KsToast.kt:24`、`maui/KsDialogs.Maui/Presentation/IKsDialog.cs:14` / `IKsLoading.cs:24` / `IKsToast.cs:24` | `ios/Tests/KsDialogsTests/DialogContractNameCompileChecks.swift:20` (3 契約型を並べて解決する正のコンパイル検証・既定ビルド同梱)。android / kmp / maui は既定ビルドに乗る `api-surface-check` の Dialog / Loading / Toast 各 checks が同じ 3 名を型名で参照 | ✅ 一致 |

### Requirement: Dialog 契約の改名

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 旧名では解決できない | 4 形態の rename (staged): `ios/Sources/KsDialogs/Presentation/KsDialog.swift`、`android/.../KsDialog.kt`、`kmp/.../kmp/KsDialog.kt`、`maui/KsDialogs.Maui/Presentation/IKsDialog.cs`。typealias / deprecated 別名なし (grep 0 件) | `ios/Tests/KsDialogsTests/DialogContractNameCompileChecks.swift:30`、`android/api-surface-check/src/negativeCheckLegacyContractName/kotlin/jp/kamusoft/ksdialogs/apicheck/RejectsLegacyContractName.kt:13`、`kmp/api-surface-check/src/negativeCheckLegacyContractName/kotlin/jp/kamusoft/ksdialogs/kmp/apicheck/RejectsLegacyContractName.kt:13`、`maui/KsDialogs.Maui.ApiSurfaceCheck/NegativeChecks/RejectsLegacyContractName.cs:11`。フラグ登録は `android/api-surface-check/build.gradle.kts:76` / `kmp/api-surface-check/build.gradle.kts:59` / `maui/KsDialogs.Maui.ApiSurfaceCheck/KsDialogs.Maui.ApiSurfaceCheck.csproj:101`。**4 本を 1 本ずつ実行して全て型未解決でビルド失敗を確認** (診断は `kasane/handbook/cross/test-execution.md` の追記行と一致) | ✅ 一致 |
| 既定エントリと自前構築の注入が同じレジストリを共有する (iOS / Android / MAUI) | `ios/Sources/KsDialogs/Presentation/Dialog.swift:8`、`android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/Dialog.kt:15`、`maui/KsDialogs.Maui/Presentation/Dialog.cs:17` | `ios/Tests/KsDialogsTests/DialogRegistryTests.swift:117`、`android/ksdialogs/src/test/kotlin/jp/kamusoft/ksdialogs/DialogRegistryTests.kt:100`、`maui/KsDialogs.Maui.Tests/DialogRegistryTests.cs:109` / `DialogDependencyInjectionTests.cs`。いずれも名前の追随のみで全件成功 | ✅ 一致 |
| 既定エントリの注入と fake の差し替えができる (KMP) | `kmp/ksdialogs-kmp/src/commonMain/kotlin/jp/kamusoft/ksdialogs/kmp/Dialog.kt:9` (`expect object`)、`.../androidMain/.../Dialog.android.kt:10`、`.../iosMain/.../Dialog.ios.kt:14` | `kmp/ksdialogs-kmp/src/commonTest/kotlin/jp/kamusoft/ksdialogs/kmp/DialogEntryPointTests.kt:17` (`val viaContract: KsDialog = Dialog.instance`)、`.../FakeDialogsSubstitutionTests.kt:20,30` (`FakeKsDialog` 注入)、`.../androidHostTest/.../AndroidDialogGatewayContractTests.kt:28,79` (Native 実体への委譲)、`.../iosTest/.../IosDialogGatewayCancellationTests.kt` | ✅ 一致 |
| 契約名を冠する実装と fake も追随する | `kmp/ksdialogs-kmp/src/commonMain/kotlin/jp/kamusoft/ksdialogs/kmp/DialogGateway.kt:45` (`GatewayKsDialog`)、`kmp/ksdialogs-kmp/src/commonTest/kotlin/jp/kamusoft/ksdialogs/kmp/support/FakeKsDialog.kt:15` (ファイル名も追随) | 上記 KMP テスト群が新名でコンパイル・実行。残存 grep で `GatewayKsDialogs` / `FakeKsDialogs` は 0 件 | ✅ 一致 |
| 製品名由来の識別子は変わらない | 改名なし。`KsDialogsOptions` / `KsDialogsKmp` / `AddKsDialogs` / `KsDialogsInterop*` / `KsDialogsMauiBridge` / `KsDialogsInstaller` / `KsDialogsInitializer` の 7 種すべて現存。Swift モジュール `KsDialogs`・Kotlin パッケージ `jp.kamusoft.ksdialogs(.kmp)`・C# namespace `KsDialogs`・NuGet ID `KsDialogs.Maui` も不変 | 4 ルートの既定ビルドが成立していること自体が担保。加えて diff の削除行に製品名識別子の改名が 1 件も無いことを確認 | ✅ 一致 |
| 4 形態のライブラリテストが通る | — | ios 251 tests / 47 suites、android 67 tests / 0 failures、kmp 96 tests / 0 failures (iosSimulatorArm64 49 + androidHostTest 47)、maui `dotnet test` 133 / 0 + Android 互換面 30 / 0 + iOS 互換面 6 tests / 3 suites。既定ビルドに乗る正の API 形状検査 (`api-surface-check` / `ApiSurfaceCheck`) も新名で成立 | ✅ 一致 |
| 4 Sample がビルドできる | — | `samples/ios` (xcodebuild) ✅ / `samples/android` (`:app:assembleDebug`) ✅ / `samples/maui` (`net10.0-android` + `net10.0-ios`) ✅ / `samples/kmp` (`:androidApp:assembleDebug` + iosApp xcodebuild) ✅ — 5 ビルドすべて成功 | ✅ 一致 |

## 対応表: user-docs

### Requirement: 文書の契約型名が実装と一致する

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 現行文書に旧名の型参照が残らない | `kasane/concepts/core/api/registration-show-semantics.md`、`kasane/concepts/{ios,android,kmp,maui}/api/dialog-surface.md`、`kasane/concepts/kmp/api/ios-host-integration.md`、`skills/{en,ja}/` 34 ファイル。README 2 枚・handbook には契約型としての旧名が元から無く変更不要 | 全 occurrence の列挙 → allowlist 除外で契約型を指す旧名 **0 件**。除外した用法は下の一覧 | ✅ 一致 |
| skills の en / ja が同じ名前を掲載する | `skills/en/**` / `skills/ja/**` | `KsDialog` / `IKsDialog` の出現数が Skill 単位で一致 (ios 5/5、android 8/8、kmp 22/22、maui 12/12、aiforms-migration 11/11)。docs-refresh 6-② 節構成一致・6-③ コードブロック byte 一致も OK | ✅ 一致 |

### Requirement: 禁止トークン lint が改名後の正しい名前を弾かない

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 単数形の契約名が lint を通り、残した禁止パターンは検出される | `.agents/skills/docs-refresh/SKILL.md:509` (パターンから `KsDialog([^sA-Za-z0-9_]|$)` を撤去)、`:516` (読み方の注記を書き換え) | scratchpad の一時 fixture で許可綴り (`KsDialog` / `IKsDialog` / `KsDialogs` / `KsDialogAttributes`) 検出 **0 件**、残した禁止パターンの例 **15 行すべて検出**。`skills/` + README の検査対象 70 ファイルで検出 **0 件**。参考: 撤去前のパターンを同じ対象へ当てると 114 件の偽陽性 | ✅ 一致 |

### 旧名の残存として除外した用法 (allowlist)

いずれも製品名・配布識別子・機械面の正当な用法 (cross/ADR-0005 の写像表どおり)。

| 用法 | 例 |
|---|---|
| Swift モジュール / SwiftPM product | `import KsDialogs`、`ios/Package.swift` の `name: "KsDialogs"`、`-scheme KsDialogs` |
| Kotlin パッケージ / Maven 座標 | `jp.kamusoft.ksdialogs`、`jp.kamusoft.ksdialogs.kmp`、`jp.kamusoft:ksdialogs-*` |
| C# namespace / NuGet ID / プロジェクト名 | `namespace KsDialogs`、`using KsDialogs;`、`KsDialogs.Maui`、`KsDialogs.Maui.Tests`、`KsDialogs.Binding.*` |
| 製品名接頭辞の識別子 | `AddKsDialogs`、`KsDialogsOptions`、`KsDialogsInterop*`、`KsDialogsMauiBridge`、`KsDialogsInstaller`、`KsDialogsInitializer`、`KsDialogsKmp*` |
| リポジトリ名・製品としての言及 | `github.com/kamusoft/KsDialogs(-SPM)`、README 冒頭の製品説明、`KsDialogs Sample`、`こんにちは、KsDialogs!` |
| ターゲット・テスト名 | `ios/Tests/KsDialogsTests/`、`KsDialogsSample(.xcodeproj)`、`KsDialogsSampleKmp` |
| 経緯・履歴の記述 (spec が明示的に許容) | `kasane/decisions/core/0002-public-api-shape.md` の旧照合行、`kasane/decisions/core/0034-*.md` 本文、`kasane/concepts/log.md` の過去行 |
| 負の検査の診断文言 (改名の**結果**として旧名が現れる) | `kasane/handbook/cross/test-execution.md:214`「`cannot find type 'KsDialogs' in scope`」、`:229` / `:240`「`Unresolved reference 'KsDialogs'.`」、`:257`「`IKsDialogs` が見つからない」、および 4 本の負の検査ソース本体 |

## 追加検査

| 項目 | 結果 |
|---|---|
| tasks.md の全タスク完了 | 5 節 15 タスクすべて `[x]`。**虚偽チェックなし** — 1.1〜1.5 / 2.1〜2.5 / 3.1〜3.5 / 4.1〜4.2 / 5.1 のすべてを上記の対応表・実行結果と突き合わせて成果物の実在を確認した |
| 逆流検査 (足場の書き換え) | `git diff --stat kasane/changes/` は `tasks.md` のみ 18 行。その内容はチェックボックスの `[ ]` → `[x]` だけ。`proposal.md` / `specs/dialog-contract/spec.md` / `specs/user-docs/spec.md` / `exploration.md` / `second-opinion-spec-001.md` に差分なし。**逆流なし** |
| 未記録乖離 | 対応表に ❌ なし。diff にあって Scenario に対応しない変更は `kasane/concepts/log.md` への 1 行追記と `kasane/decisions/core/0002-public-api-shape.md` の照合行更新のみで、いずれも tasks 3.1 / 5.1 が明示的に指示した範囲。**未記録乖離なし** |
| 付随修正 | deviation.md 不在。`[付随修正]` に相当する範囲外の修正は diff に見当たらない |
| UI 変更 | 本 change に `ui/` は無く該当なし (挙動・見た目の変更なし) |
| テストが全件成功 | すべて自分で実行。ios 251 / android 67 / kmp 96 / maui 133 + 30 + 6 のいずれも failures 0。※ ios の 1 回目のみ `[TS-IO-02]` (Toast・本 change の対象外) が cold boot 直後の Simulator でタイムアウトし 1 件失敗したが、単体再実行と 2 回目の全件実行 (251/251) で成功。環境起因のフレークとして扱い、判定には影響させない (review-001.md に所見として記録) |
| 負の検査の巻き添え確認 | 改名で本文が変わった既存の負の検査 10 本 (ios 3 / android 3 / kmp 2 / maui 5 のうち改名の影響を受ける 13 フラグ) を個別に実行し、**すべて改名前と同じ診断**で失敗することを確認。別の理由で失敗して「効いた」と読み違える事故はない |
| handbook の本数更新 | `kasane/handbook/cross/test-execution.md` の負の検査が 55 本 → 59 本へ更新され、4 行の追記が実際のフラグ・診断と一致 |

## 判定

**VALID** — 全 10 Scenario (dialog-contract 7 + user-docs 3) が ✅ 一致。❌ **0 件**。虚偽チェックなし、逆流なし、未記録乖離なし、テスト全件成功。
