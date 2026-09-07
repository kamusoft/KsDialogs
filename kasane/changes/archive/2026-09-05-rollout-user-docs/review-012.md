# レビュー結果: rollout-user-docs（012 回目）

**日付**: 2026-09-05
**対象**: `skills/{en,ja}/ksdialogs-kmp/**`、KMP/Android/iOS host の公開実装・tests・Sample、呼び出し元指定の planned manifest
**判定**: **CHANGES_REQUESTED**

## サマリー

KMP Skill の英日各9ファイルは、planned manifest の source 対応、KMP に必要な8件の core concept、API/Setup/挙動、3e の exact 分類、英日構造、閉世界性の各検査に適合している。`host notifier`、Android の3種の `registerCompose` と `showCompose`、iOS linkage、multi-display のプラットフォーム差、Swift 境界の `@Throws` ルート表も、公開実装・tests・Sample と一致する。

ただし、現行 Sample の通常 consumer 経路から KMP ライブラリの `iosMain` metadata を再コンパイルすると、公開 interface と iOS 実装 override の `@Throws` filter 不一致3件で失敗する。直接の iOS target compile と Android consumer compile は成功するため、target 単体の確認だけでは見落とす。Skill が案内する iOS consumer 統合を現行構成で完遂できない build failure であり、Major と判定する。

## 照合した規約・正本

- `ksn-review`、`ksn-core`（handbook / delta-spec / config / paths）
- `kotlin-impl-skill`（null safety / DSL / coroutines / testing / hygiene）
- `rollout-user-docs` の proposal、design、tasks、deviation、UI brief、delta specs
- `kasane/concepts/core/api/**` の8 concept、`kasane/concepts/kmp/api/ios-host-integration.md`
- 関連 handbook と accepted Android/KMP ADR
- 呼び出し元指定の planned manifest `docs-refresh-ksdialogs-manifest-planned.json`
- `skills/{en,ja}/ksdialogs-kmp/**` 全18ファイル
- KMP/Android/iOS host の公開実装、tests、Sample

既存の KMP review および実装報告は参照していない。repository の bootstrap `skills/.manifest.json` は task 8 前の予定状態であるため、判定根拠に使用していない。

## 指摘事項

### [Major] 通常の KMP consumer metadata compile が `@Throws` override 不一致で失敗する

現行 Sample から、キャッシュを使わずに通常の shared consumer metadata 経路を実行した。

```text
cd samples/kmp
./gradlew :shared:compileCommonMainKotlinMetadata --rerun-tasks
```

結果は `BUILD FAILED` で、included KMP build の `:kmp:ksdialogs-kmp:compileIosMainKotlinMetadata` が次の3件を報告した。

```text
IosLoadingGateway.kt:34:5 Member overrides different '@Throws' filter from 'interface KsLoading : Any'.
IosLoadingGateway.kt:57:5 Member overrides different '@Throws' filter from 'interface KsLoading : Any'.
IosToastGateway.kt:30:5 Member overrides different '@Throws' filter from 'interface KsToast : Any'.
```

該当する公開 interface は以下である。

- `kmp/ksdialogs-kmp/src/commonMain/kotlin/jp/kamusoft/ksdialogs/kmp/KsLoading.kt:43` — custom Loading `show`
- `kmp/ksdialogs-kmp/src/commonMain/kotlin/jp/kamusoft/ksdialogs/kmp/KsLoading.kt:88` — custom Loading `start`
- `kmp/ksdialogs-kmp/src/commonMain/kotlin/jp/kamusoft/ksdialogs/kmp/KsToast.kt:51` — custom Toast `show`

失敗した iOS override は以下である。

- `kmp/ksdialogs-kmp/src/iosMain/kotlin/jp/kamusoft/ksdialogs/kmp/IosLoadingGateway.kt:34`
- `kmp/ksdialogs-kmp/src/iosMain/kotlin/jp/kamusoft/ksdialogs/kmp/IosLoadingGateway.kt:57`
- `kmp/ksdialogs-kmp/src/iosMain/kotlin/jp/kamusoft/ksdialogs/kmp/IosToastGateway.kt:30`

英語 Skill は `skills/en/ksdialogs-kmp/SKILL.md:66` と `references/ios-host.md:108` 以降、日本語 Skill は対応する同じ位置で Swift 境界の `@Throws` とルート表を案内している。表自体は commonMain の公開宣言と一致するが、通常 consumer が通る階層化 source set の metadata compile が失敗するため、利用者はその API を含む iOS consumer 統合を現行 Kotlin 2.4.10 構成で完了できない。

対照として、次の fresh compile は成功した。

```text
cd kmp
./gradlew :ksdialogs-kmp:compileKotlinIosSimulatorArm64 --rerun-tasks
# BUILD SUCCESSFUL

cd samples/kmp
./gradlew :androidApp:compileDebugKotlin --rerun-tasks
# BUILD SUCCESSFUL
```

また、library 直下でも次の階層化 metadata task は同じ3件で失敗した。

```text
cd kmp
./gradlew :ksdialogs-kmp:compileIosMainKotlinMetadata --rerun-tasks
# BUILD FAILED
```

したがって環境全体や SwiftPM linkage の一律失敗ではなく、`iosMain` metadata/commonization 経路に固有の再現性ある不整合である。

**必要な対応**: Kotlin 2.4.10 で public interface と iosMain override の `@Throws` filter が一致するよう実装を修正し、library 直下の `compileIosMainKotlinMetadata` と Sample consumer の `:shared:compileCommonMainKotlinMetadata` を、ともに `--rerun-tasks` 付きで成功させる。その後 KMP Skill を再レビューする。

## 検証証跡

### Source completeness / 構造

- planned manifest の KMP target は root 1件 + reference 8件で、英日双方の実ファイル集合と完全一致した。
- root source は8 core concept 全件に加えて KMP iOS host integration concept を含む。
- 各 reference の source は担当内容と一致した。`ios-host.md` は7 core concept + KMP iOS host integration、`android-host.md` はAndroid host に必要な7 core concept を持つ。
- internal link は全件解決し、Skill root 外への相対 link はない。
- frontmatter、見出し構造、コードブロックは英日で一致し、コードブロックは byte-identical だった。
- KMP API 候補50件は handbook の exact exclusion row に全件一致し、internal interop / target skill outside / mechanically derivable の3e分類から漏れなかった。
- `kasane/`、ADR ID、内部 bridge 型、ローカル絶対 path などの provenance/internal detail は Skill 本文に流出していない。

### API / Setup / 挙動

- KMP 2.4.10、Gradle 9.7.0、Android minSdk 24、iOS 17、Swift tools 6.3 を build 定義と照合した。
- singleton と注入可能 interface、全3 registry の共有、Android native factory の `Context` receiver、`viewModel.notifier` を実装・tests と照合した。
- Android の Dialog / Loading / Toast の `registerCompose` と inline factory の `showCompose` を公開実装・Sample と照合した。
- iOS の `Dialog.shared.kmp` / `Loading.shared.kmp` / `Toast.shared.kmp`、UIKit/SwiftUI registration、Dialog notifier を公開実装・Sample と照合した。
- shared static framework、composite linked package、typed registration 用の direct KsDialogs package という linkage と、1回限りの integrate task、VCS、Gradle で SwiftPM dependency を再宣言しない手順を build 設定・Sample project と照合した。
- `@Throws` の4 view-model route と message route の非付与を commonMain 公開宣言と照合した。
- multi-display は、保持済み下段 notifier で Android は下段のみ閉じて上段を維持し、iOS は鎖を両方外して上段を cancel する差を concepts・両 host tests と照合した。
- layout、Loading、Toast、transition の記述を concepts・公開実装・tests と照合した。

### 実行範囲

- 上記の限定 fresh compile を実行した。
- 全 build root / 全 test suite は、proposal に明記された製品コード・tests 無変更時の免除に従い実行していない。

## アクションプラン

1. `KsLoading` / `KsToast` と iOS gateway override の `@Throws` filter 不一致を修正する。
2. `:ksdialogs-kmp:compileIosMainKotlinMetadata --rerun-tasks` を成功させる。
3. Sample から `:shared:compileCommonMainKotlinMetadata --rerun-tasks` を成功させ、通常 consumer 経路の回復を確認する。
4. KMP Skill の fresh review を再実施する。
