# 検証: add-native-distribution

**日付**: 2026-09-08
**対象**: commit `aa71409` (提案) から現在の作業ツリーまでの差分 (実装本体 `654a41f` + 未コミットの tasks.md チェック更新と `evidence/spm-https-resolution.txt`)
**デルタスペック**: `specs/android-maven-distribution/spec.md` / `specs/spm-distribution/spec.md` (Requirement 8 / Scenario 22)
**判定**: **VALID** (❌ 0 件)

---

## 対応表

### android-maven-distribution

#### Requirement: Android module の座標と構成

| Scenario | 実装 | テスト / 担保 | 状態 |
|---|---|---|---|
| 改名後の aar 生成 | `android/settings.gradle.kts:30,34` (include `:ksdialogs-core` / `:ksdialogs`)、`android/ksdialogs-core/build.gradle.kts:30` (namespace `jp.kamusoft.ksdialogs`)、`android/ksdialogs/build.gradle.kts:30` (namespace `jp.kamusoft.ksdialogs.compose`) | 検証側で `./gradlew :ksdialogs-core:assembleRelease :ksdialogs:assembleRelease` を実行 → BUILD SUCCESSFUL。`ksdialogs-core-release.aar` (309884 B) / `ksdialogs-release.aar` (25061 B) を出力。クラス構成は `evidence/publish-to-maven-local.txt` (`jp/kamusoft/ksdialogs` 212 / `jp/kamusoft/ksdialogs/compose` 12) | ✅ 一致 |
| ユニットテストと API 形状検査の無改変実行 | 同上 + `android/ksdialogs-core/build.gradle.kts:152-153` (`verifyNoDeclarativeUiDependency` を `test` に結線)、`:145-146` (`check` に結線)、`android/api-surface-check/build.gradle.kts:54,56` (project 参照の差し替え) | 検証側で `./gradlew test --rerun-tasks` → BUILD SUCCESSFUL、`:ksdialogs-core` 68 tests / 0 failures (改名前の実測 68 = `kasane/handbook/cross/test-execution.md` の記録と一致)、`:ksdialogs-core:verifyNoDeclarativeUiDependency` がタスクグラフに乗ることをログで確認。`:ksdialogs` / `:api-surface-check` は `NO-SOURCE` (改名前と同じ配置)。`:api-surface-check` 肯定ケースは `assembleDebug` 成功、否定ケースは 15 プロパティすべてを個別ビルドして全件コンパイルエラーで失敗 (下の「否定ケース全 15 件」) | ✅ 一致 |
| instrumented test の無改変実行 | 同上 | `evidence/instrumented-test-counts.txt` — `:ksdialogs-core` 294 / `:ksdialogs` 39、failures 0。実行環境は API 36 のエミュレータではなく実機 (`deviation.md` にオーナー合意済みで記録)。高コストのため検証側では再実行せず証跡で代替 | ⚠️ deviation 記録済み |

`:api-surface-check` は発行対象に含まれない (下の「api-surface-check の非公開」)。Kotlin パッケージ・AGP namespace は無変更 (上記 build.gradle.kts の namespace 値が改名前と同一)。

**否定ケース全 15 件** (検証側で `./gradlew :api-surface-check:assembleDebug -Pksdialogs.negativeCheck.<name>` を 1 つずつ実行。全件 exit 1):

| プロパティ | 診断 (先頭 1 件) |
|---|---|
| vmAttribute | `RejectsLayoutAttributeOnViewModel.kt:11:66 Unresolved reference 'proportionalWidth'.` |
| showOptions | `RejectsOptionsArgumentOnShow.kt:16:17 None of the following candidates is applicable:` |
| resultType | `RejectsMismatchedResultType.kt:15:9 Return type mismatch: expected 'DialogResult<String>', actual 'DialogResult<Boolean>'.` |
| notifierValue | `RejectsMismatchedNotifierValue.kt:16:31 Argument type mismatch: actual type is 'String', but 'Boolean' was expected.` |
| showTransition | `RejectsTransitionArgumentOnShow.kt:15:17 None of the following candidates is applicable:` |
| optionsTransition | `RejectsTransitionOnDialogOptions.kt:12:61 Unresolved reference 'transition'.` |
| noneArguments | `RejectsArgumentsOnNonePreset.kt:13:66 Too many arguments for 'fun none(): DialogTransition'.` |
| loadingShowStyle | `RejectsStyleArgumentOnLoadingShow.kt:17:17 None of the following candidates is applicable:` |
| loadingShowOptions | `RejectsOptionsArgumentOnLoadingShow.kt:17:17 None of the following candidates is applicable:` |
| toastHide | `RejectsHideOnToast.kt:14:15 Unresolved reference 'hide'.` |
| toastShowResult | `RejectsResultOfToastShow.kt:14:30 Initializer type mismatch: expected 'String', actual 'Unit'.` |
| toastShowStyle | `RejectsStyleArgumentOnToastShow.kt:17:15 None of the following candidates is applicable:` |
| toastOptions | `RejectsOptionsPropertyOnToast.kt:16:15 Unresolved reference 'options'.` |
| toastComposeFromCore | `RejectsComposeToastApiFromCoreModule.kt:5:30 Unresolved reference 'showCompose'.` |
| legacyContractName | `RejectsLegacyContractName.kt:3:30 Unresolved reference 'KsDialogs'.` |

「改名前と同じ診断」は、否定検証のソース 15 本が本 change で無改変 (唯一の変更は `RejectsComposeToastApiFromCoreModule.kt` の doc コメント 2 行で、診断が指す 5 行目より下) であることと、上表の診断がすべて元のファイル名・行・メッセージを保っていることから成立する。

#### Requirement: version の単一ソースと注入

| Scenario | 実装 | テスト / 担保 | 状態 |
|---|---|---|---|
| 注入なしの開発既定値 | `android/build.gradle.kts:29-45` (導出式)、`:48-49` (subprojects の group / version)、`android/gradle/libs.versions.toml` の `ksdialogs = "0.1.0-SNAPSHOT"` | 検証側でローカル Maven リポジトリの発行物を直接検査 — `jp/kamusoft/ksdialogs-core/0.1.0-SNAPSHOT/` と `jp/kamusoft/ksdialogs/0.1.0-SNAPSHOT/` が存在。`evidence/version-injection.txt` 節 1 | ✅ 一致 |
| リリース version の注入 | 同上 | `evidence/version-injection.txt` 節 2 — `-Pversion=0.1.0-beta.1` で両 artifact の座標 version が `0.1.0-beta.1`、`ksdialogs` POM の `ksdialogs-core` 依存も同版。高コスト (publishToMavenLocal の再実行) のため証跡で代替 | ✅ 一致 |
| 不正な注入値の拒否 | `android/build.gradle.kts:34` (`releaseVersionPattern`)、`:39-44` (`require` と文言) | `evidence/version-injection.txt` 節 3 — 空文字 / `v1.0.0` / `1.0.0-pre` / 末尾空白の 4 値が設定段階で BUILD FAILED、`1.0.0-beta.1` は成功。許容形式を示すメッセージあり | ✅ 一致 |
| SNAPSHOT の Central 発行拒否 | `android/build.gradle.kts:127-137` (タスク名述語 + `doFirst` の例外) | `evidence/snapshot-central-publish-guard.txt` — Central 向け 7 エントリポイントすべてでガード発火、除外した `dropMavenCentralDeployment` は発火せず、`publishToMavenLocal` は成功。検証側でも発行物が `publishToMavenLocal` 経由で存在することを確認 | ✅ 一致 |

#### Requirement: Maven 発行物の座標と内容

| Scenario | 実装 | テスト / 担保 | 状態 |
|---|---|---|---|
| ローカル発行での発行物検証 | `android/build.gradle.kts:59-66` (`AndroidSingleVariantLibrary` / `SourcesJar.Sources()` / `JavadocJar.Empty()`)、`:76-100` (POM 共通部)、`android/ksdialogs-core/build.gradle.kts:19-28` と `android/ksdialogs/build.gradle.kts:19-29` (name / description) | 検証側でローカル Maven リポジトリの発行物を直接検査 — 両座標に aar・sources jar・javadoc jar・POM・`.module` の 5 点。javadoc jar は `META-INF/MANIFEST.MF` のみ (2 エントリ、25 B) で空。sources jar は `jp/kamusoft/ksdialogs/**` の `.kt` 74 本 (core) / `jp/kamusoft/ksdialogs/compose/**` の `.kt` 9 本。POM に MIT License・scm・developer `kamusoft`・inceptionYear 2026・url と module ごとの name (`KsDialogs Core` / `KsDialogs`) / description を確認 | ✅ 一致 |
| 発行メタデータの依存スコープ | 各 module の `api` / `implementation` 宣言 (`android/ksdialogs-core/build.gradle.kts:79,83`、`android/ksdialogs/build.gradle.kts:70-77`) | 検証側で POM と `.module` を直接パース。compile = `androidx.annotation:annotation` (core) / `jp.kamusoft:ksdialogs-core` + `androidx.compose.runtime:runtime` (compose) — いずれも Requirement の列挙と一致 (kotlin-stdlib は対象外)。runtime = 上記 + `kotlinx-coroutines-android` (core) / `androidx.compose.ui:ui` + `androidx.lifecycle:lifecycle-runtime` + `androidx.savedstate:savedstate` (compose)。テスト専用ライブラリ (junit / androidx.test / coroutines-test / kotlin-test / robolectric) は POM・`.module` のいずれにも 0 件 | ✅ 一致 |
| release aar の公開シグネチャ検算 | 同上 | `evidence/release-aar-public-signature-scan.txt` — javap 走査で列挙された外部の非プラットフォーム型は `androidx.annotation.ColorInt` / `androidx.compose.runtime.Composable` `Composer` `ProvidableCompositionLocal` / `org.jetbrains.annotations.*` のみで、すべて compile スコープ (または stdlib の推移分) に存在。`implementation` のみの依存の型は公開面に 0 件 | ✅ 一致 |
| api-surface-check の非公開 | `android/api-surface-check/build.gradle.kts:12-16` (`mavenPublish` プラグイン未適用)、`android/build.gradle.kts:54` (`plugins.withId` で発行 module にだけ共通設定) | `evidence/snapshot-central-publish-guard.txt` 節 3 — `:api-surface-check:tasks --group publishing` が `No tasks`、`--all` の publish 系は AGP の `prepareLintJarForPublish` のみ | ✅ 一致 |

#### Requirement: 発行物の署名

| Scenario | 実装 | テスト / 担保 | 状態 |
|---|---|---|---|
| 鍵なしのローカル発行 | `android/build.gradle.kts:109-111` (`setRequired(providers.gradleProperty("signingInMemoryKey").isPresent)`) | 検証側でローカル Maven リポジトリの両座標を走査し `.asc` 0 件を確認。`evidence/publish-to-maven-local.txt` の「署名 — 鍵なしのローカル発行」で `signMavenPublication SKIPPED` + BUILD SUCCESSFUL | ✅ 一致 |
| 鍵ありのローカル発行 | 同上 + `android/build.gradle.kts:73` (`signAllPublications()`) | `evidence/publish-to-maven-local.txt` の「署名 — 鍵ありのローカル発行」 — 一時生成鍵で両 module の全 publication に `.asc` 計 10 件。鍵は破棄済み。鍵の再生成は高コスト・かつ秘密の取り回しを伴うため検証側では再実行せず証跡で代替 (`android/ksdialogs-core/build/outputs/aar/` に当時の `.asc` がビルド出力として残っていることも確認) | ✅ 一致 |

#### Requirement: monorepo 内消費者の座標追随

| Scenario | 実装 | テスト / 担保 | 状態 |
|---|---|---|---|
| Sample のビルドとソース参照 | `samples/android/app/build.gradle.kts:50` (直接依存 1 行)、`samples/android/settings.gradle.kts:46,48` (置換 2 本)、`samples/kmp/androidApp/build.gradle.kts:51`、`samples/kmp/settings.gradle.kts:51,53` | `evidence/consumer-follow-up.txt` 節 1〜3 — 両 Sample の `assembleDebug` 成功、`dependencyInsight` で `jp.kamusoft:ksdialogs` → `project ':android:ksdialogs'`・本体は推移的に `project ':android:ksdialogs-core'`、置換の実効を APK の dex に入れた目印で確認。Sample のビルドは高コストのため証跡で代替 | ✅ 一致 |
| KMP と MAUI bridge のビルド | `kmp/ksdialogs-kmp/build.gradle.kts:69`、`kmp/settings.gradle.kts:37`、`maui/android/native/ksdialogs-maui-bridge/build.gradle.kts:49`、`maui/android/native/settings.gradle.kts:37` | 検証側で `kmp/` の `./gradlew allTests --rerun-tasks` を実行 → BUILD SUCCESSFUL、151 tests / 0 failures (iosSimulatorArm64 75 + androidHostTest 76)。bridge の `assembleRelease` と依存解決先は `evidence/consumer-follow-up.txt` 節 5 (`jp.kamusoft:ksdialogs-core` → `project ':android:ksdialogs-core'`、Compose 系は依存グラフに現れない) | ✅ 一致 |
| MAUI binding のビルド | `maui/android/KsDialogs.Binding.Android/KsDialogs.Binding.Android.csproj:39,58,60,73` (aar パス・ソース入力・gradlew task 名) | `evidence/consumer-follow-up.txt` 節 6 — aar を削除してから `dotnet build` し 0 エラー、同梱 aar の `package="jp.kamusoft.ksdialogs"` (View 系本体) を確認。MAUI binding のビルドは高コストのため検証側では再実行せず証跡で代替 | ✅ 一致 |

### spm-distribution

#### Requirement: スナップショット同期スクリプト

実装: `scripts/spm-snapshot/sync-snapshot.sh` (検証 1〜4 = `:87-131`、`.git` 以外の除去 = `:141-147`、ホワイトリスト 5 点配置 = `:149-153`、git 操作なし)。
テスト: `scripts/spm-snapshot/sync-snapshot-test.sh` を検証側で実行 → exit 0、判定 41 件すべて ok / FAIL 0 件。

| Scenario | テスト | 状態 |
|---|---|---|
| ホワイトリスト 5 点の配置 | `sync-snapshot-test.sh:117-136` (8 判定。5 点の存在・種別・内容一致) | ✅ 一致 |
| 列挙外ファイルの混入防止 | `:140-152` (隠しファイル・ディレクトリ内部の残骸を含む) | ✅ 一致 |
| 冪等性 | `:156-166` (ファイル一覧と全ファイル内容のハッシュ) | ✅ 一致 |
| 同期先の誤指定の拒否 | `:170-235` (git top-level でない / サブディレクトリ / origin 不一致 / 名前を含む別 URL / monorepo 自身 / monorepo の祖先 / 祖先判定ロジック単体 5 件) | ✅ 一致 |
| コピー元不足時の無変更 | `:239-248` | ✅ 一致 |
| git 非操作 | `:252-274` (`.git` 保持・HEAD・commit 数・tag・index・remote 設定の不変、結果が未コミットの working tree に残ること) | ✅ 一致 |

CI 結線: `.github/workflows/ci.yml:256-257` の lint job に `bash scripts/spm-snapshot/sync-snapshot-test.sh` を追加、`kasane/handbook/cross/verification-ci.md:24` の lint 行を追随済み。

#### Requirement: 誘導 README の整合

| Scenario | 実装 | テスト / 担保 | 状態 |
|---|---|---|---|
| 初回 sync 後の README | `scripts/spm-snapshot/README.template.md`、配置は `sync-snapshot.sh:153` | 検証側で `diff -u ../KsDialogs-SPM/README.md scripts/spm-snapshot/README.template.md` → 差分なし (exit 0)。テンプレートの内容が配置されることは `sync-snapshot-test.sh:129-130` が担保 | ✅ 一致 |

#### Requirement: 配信リポジトリの https 解決

| Scenario | 実装 | テスト / 担保 | 状態 |
|---|---|---|---|
| 実リモートからの依存解決とビルド | `scripts/spm-snapshot/verify-https-resolution.sh:97-174` (事前検証)、`:268-274` (tag 作成と push)、`:276-327` (一時消費者パッケージと iOS Simulator 向けビルド) | `evidence/spm-https-resolution.txt` — 配信リポジトリ `kamusoft/KsDialogs-SPM` のスナップショット (49fc0a8) に tag `0.1.0-alpha.1` を打ち、https URL + exact 指定で解決して `DialogOptions` / `DialogAlignment` を参照する消費者が `** BUILD SUCCEEDED **`。`Package.resolved` の pin が配信リポジトリの当該 revision を指す。実リモートへの再実行は制約により禁止のため証跡で代替 | ✅ 一致 |
| 検証用 tag の後始末 (成功時) | `:176-266` (`trap cleanup EXIT`。着手フラグを mutation の前に立て、削除後に照会した tag 一覧の実体で判定) | `evidence/spm-https-resolution.txt` の後始末節 — remote / local とも実行後の tag 一覧が実行前と同一 (どちらも空)。検証側で `../KsDialogs-SPM` の tag 数が 0 であることを直接確認 | ✅ 一致 |
| 検証用 tag の後始末 (失敗時) | 同上 (`:191-208` の削除、`:213-256` の実体照会による判定と exit code の引き上げ) | 実リモートでは再現していない。修正サイクルで scratch の fixture (一時 bare リポジトリを origin、`xcodebuild` はスタブ、origin URL 検査 1 行だけを差し替えたコピー) により、消費者ビルド失敗・remote 削除拒否・照会不能の経路で後始末が走ることを確認済み (`review-002.md:19`、`:62` 項 A、`:120-127` の対照表 — 失敗条件でも remote に検証用 tag が残らず「結果: 失敗 (exit 1)」がログ末尾に記録される)。この fixture は commit されていない | ✅ 一致 (注記あり) |

注記: 失敗時の後始末は Scenario の THEN (後始末が走る / tag が remote と作業コピーの両方から削除される / 失敗の記録だけが残る) を fixture 実行で満たしていることが確認できるため ✅ とした。ただし担保は使い捨て fixture の実行記録だけで、リポジトリに残る回帰検出はない。`sync-snapshot.sh` が `sync-snapshot-test.sh` を持つのに対し `verify-https-resolution.sh` は自動テストを持たない (この非対称は本 change の Requirement の範囲外)。

#### Requirement: monorepo 内の iOS 消費者の維持

| Scenario | 実装 | テスト / 担保 | 状態 |
|---|---|---|---|
| iOS package のテスト維持 | 無変更 (`ios/`) | 検証側で `git diff aa71409 -- ios/` が空であることを確認 (`ios/Package.swift` 無変更)。`evidence/spm-sync-and-ios-consumers.txt` 節 4 — 専用に boot した Simulator で `xcodebuild test`、277 tests / 50 suites すべて成功。高コストのため検証側では再実行せず証跡で代替 | ✅ 一致 |
| iOS Sample と binding のビルド | 無変更 (`samples/ios` / `maui/macios/native`) | `evidence/spm-sync-and-ios-consumers.txt` 節 5 — 両プロジェクトとも `** BUILD SUCCEEDED **`、ログに `ios/Sources/KsDialogs/` のコンパイル行があり Local Swift Package 参照が維持されている。高コストのため証跡で代替 | ✅ 一致 |

---

## 追加検査

| 検査 | 結果 |
|---|---|
| tasks.md 全タスク完了 | 22 タスクすべて `[x]`。`git diff aa71409 -- tasks.md` はチェックボックスの `[ ]`→`[x]` のみで、本文の書き換えなし |
| 虚偽チェックの有無 | なし。対応表と突き合わせて未実装のチェックは見つからなかった。特に検証側で自力再現した項目 (1.1 / 1.2 の unit + api-surface-check 全否定ケース / 2.1 / 3.1〜3.5 の発行物 / 4.1 / 5.2 の kmp / 6.1〜6.4 / 7.3 の tag 不在 / 7.4 の `ios/` 無変更) はすべて記述どおり |
| 逆流検査 | `git diff aa71409 -- proposal.md design.md specs` の出力は 0 バイト。足場アーティファクトの書き換えなし |
| 未記録乖離 | なし。❌ が 0 件のため該当なし |
| 付随修正 | `deviation.md` の `[付随修正]` 1 行が diff の該当箇所 (`android/gradle/libs.versions.toml` のコメント 3 箇所、`android/api-surface-check/src/main/.../ToastApiSurfaceChecks.kt` 1 行、`.../negativeCheckToastComposeFromCore/.../RejectsComposeToastApiFromCoreModule.kt` 2 行) を網羅。いずれも字面の置換で値・コードは不変 |
| UI 変更 | なし (`ui/` アーティファクトを持たない change) |
| テスト全件成功 | `android/` `./gradlew test --rerun-tasks` = 68 / 0 failures (BUILD SUCCESSFUL)、`kmp/` `./gradlew allTests --rerun-tasks` = 151 / 0 failures (BUILD SUCCESSFUL)、`scripts/spm-snapshot/sync-snapshot-test.sh` = 41 判定すべて ok (exit 0)。加えて CI lint job の 4 スクリプト (`scripts/local-path-lint.py` / `identity-lint.py` / `comment-policy-lint.py` / `scenario-id-coverage.py`) をすべて exit 0 で確認 |

### 証跡で代替した高コスト検証

コンテキストパッケージの許可に従い、次は再実行せず `evidence/` の記録を担保とした: instrumented test (実機 294 / 39)、`ios/` の `xcodebuild test` (277 件)、`samples/ios` と maui iOS binding のビルド、MAUI binding の `dotnet build`、両 Sample の `assembleDebug`、`-Pversion=` 注入ありの `publishToMavenLocal`、署名鍵ありの `publishToMavenLocal`、`verify-https-resolution.sh` の実リモート実行 (制約により実行禁止)。ローカル発行の**発行物そのもの**は再発行せずローカル Maven リポジトリに残る実体を直接検査した (POM / `.module` / jar の中身)。

---

## 観測 (Requirement の範囲外・❌ ではない)

1. **利用者向け文書に旧座標が残る** — `README.md:66,70` / `README_ja.md:66,70` と `skills/{en,ja}/` 計 14 行に `jp.kamusoft:ksdialogs-compose` (および本体としての `jp.kamusoft:ksdialogs`) が残る。デルタスペックの「monorepo 内消費者の座標追随」は samples / kmp / maui bridge / MAUI binding だけを列挙しており対象外。プロジェクトの `CLAUDE.md` は `skills/` と README 群の追従を docs-refresh 経由に限定しており、`review-002.md` のアクションプラン 3 が引き継ぎとして記録済み。本 change ではその受け皿として `.agents/skills/docs-refresh/SKILL.md:495,516,518` の識別子表と配布座標の説明が新座標へ更新されている。
2. **concepts に旧座標が残る** — `kasane/concepts/android/api/` の dialog-surface.md:44 / layout-surface.md:81 / loading-surface.md:51 / toast-surface.md:51 / transition-surface.md:70 の計 5 行。長命層の追随は ksn-distill の責務で、本 change はまだ蒸留前。蒸留時の追随対象として記録しておく (`kasane/concepts/log.md` の既存行は append-only の履歴なので変更対象外)。

---

## 判定

**VALID** — 全 22 Scenario が「✅ 一致」(21 件) または「⚠️ deviation 記録済み」(1 件、instrumented test の実機実行)。❌ 0 件。tasks.md に虚偽のチェックなし、足場アーティファクトの逆流なし、未記録乖離なし、実行したテストはすべて成功。
