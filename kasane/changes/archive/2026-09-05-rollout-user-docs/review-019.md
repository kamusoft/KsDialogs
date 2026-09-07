# レビュー結果: rollout-user-docs（019 回目）

**日付**: 2026-09-05
**対象**: `skills/{en,ja}/ksdialogs-kmp/**`、planned manifest、関連 concepts / handbook、KMP・Android・iOS の公開実装・tests・Sample
**判定**: **CHANGES_REQUESTED**

## サマリー

KMP Skill の英日各9ファイルは、planned manifest の source 対応、全 target concept、構成・frontmatter、英日等価性、閉世界性、3e API 候補の exact 分類、公開 API・Setup・挙動との対応に適合している。文書自体に新たな不整合は検出しなかった。

一方、Skill が案内する KMP iOS consumer 経路の基礎となる `iosMain` metadata compile は現行でも失敗する。library 直下だけでなく、現行 Sample の通常 consumer 経路からも同じ `@Throws` override 不一致3件を再現した。別 change の簡易起票は存在するが、根本原因・修正形・回帰検査が未決で、成功証跡や本 change の明示的な免除判断にはなっていない。したがって現時点では Major 1件、`CHANGES_REQUESTED` とする。

## 照合した規約・正本

- `ksn-review`、`ksn-core` と必須 references
- `kotlin-impl-skill` と必須 references、`jetpack-compose-impl-skill`
- `docs-refresh` と利用者向け Skill の内容・検査規約
- `rollout-user-docs` の proposal、design、tasks、deviation、全 delta specs、UI brief
- `kasane/concepts/core/api/**` の8 concept、`kasane/concepts/kmp/api/ios-host-integration.md`
- 関連 handbook と accepted Android / KMP ADR
- `/tmp/docs-refresh-ksdialogs-manifest-planned.json`
- `skills/{en,ja}/ksdialogs-kmp/**` 全18ファイル
- KMP / Android / iOS host の公開実装、tests、Sample
- 呼び出し元から渡された `review-012.md` と、スコープ外不具合の簡易起票 `fix-kmp-iosmain-throws-metadata/exploration.md`

実装者の報告は判定根拠に用いず、文書・実装・検査を現行作業ツリーから再確認した。

## 指摘事項

### [Major] 通常の KMP consumer metadata compile が `@Throws` override 不一致で失敗する

library 直下で、キャッシュを使わず階層化 source set の metadata task を実行した。

```text
cd kmp
./gradlew :ksdialogs-kmp:compileIosMainKotlinMetadata --rerun-tasks
# BUILD FAILED
```

現行 Sample から通常の shared consumer metadata 経路も実行した。

```text
cd samples/kmp
./gradlew :shared:compileCommonMainKotlinMetadata --rerun-tasks
# BUILD FAILED
```

両方とも included KMP build の `compileIosMainKotlinMetadata` で、次の同じ3件を報告した。

```text
IosLoadingGateway.kt:34:5 Member overrides different '@Throws' filter from 'interface KsLoading : Any'.
IosLoadingGateway.kt:57:5 Member overrides different '@Throws' filter from 'interface KsLoading : Any'.
IosToastGateway.kt:30:5 Member overrides different '@Throws' filter from 'interface KsToast : Any'.
```

対応する公開 interface と失敗箇所は次のとおりである。

- `kmp/ksdialogs-kmp/src/commonMain/kotlin/jp/kamusoft/ksdialogs/kmp/KsLoading.kt:43` と `IosLoadingGateway.kt:34` — custom Loading `show`
- `kmp/ksdialogs-kmp/src/commonMain/kotlin/jp/kamusoft/ksdialogs/kmp/KsLoading.kt:88` と `IosLoadingGateway.kt:57` — custom Loading `start`
- `kmp/ksdialogs-kmp/src/commonMain/kotlin/jp/kamusoft/ksdialogs/kmp/KsToast.kt:51` と `IosToastGateway.kt:30` — custom Toast `show`

Skill は `skills/en/ksdialogs-kmp/SKILL.md:66` と `references/ios-host.md:106` 以降、日本語版の対応箇所で Swift 境界の `@Throws` と route table を案内する。表は commonMain の公開宣言と一致しており文書誤記ではないが、これらの API を含む通常 consumer metadata 経路が現行 Kotlin 2.4.10 構成で成立しない。

失敗範囲の対照として、次の target / Android consumer compile は fresh に成功した。

```text
cd kmp
./gradlew :ksdialogs-kmp:compileKotlinIosSimulatorArm64 --rerun-tasks
# BUILD SUCCESSFUL

cd samples/kmp
./gradlew :androidApp:compileDebugKotlin --rerun-tasks
# BUILD SUCCESSFUL
```

このため環境全体や SwiftPM linkage の一律失敗ではなく、`iosMain` metadata/commonization 経路に限定した再現性のある不整合である。

`kasane/changes/fix-kmp-iosmain-throws-metadata/exploration.md:18` 以降は根本原因・修正形・回帰検査をすべて「未決の論点」とし、変更級も未判定である。別 change として追跡すること自体は適切だが、起票だけでは壊れた通常 consumer 経路を回復せず、本 change に対する成功条件の変更や明示的な deviation にもならない。

proposal の合意済み例外は「製品コード・tests 無変更につき、全ビルドルートの全件実行を省略する」という範囲である。この例外に従って全 suite は実行していないが、実利用経路を直接検証する限定 compile の失敗まで成功扱いにする根拠にはできない。

**必要な対応**: 別 change で public interface と iOS gateway override の `@Throws` filter 不一致を解消し、library の `compileIosMainKotlinMetadata` と Sample consumer の `:shared:compileCommonMainKotlinMetadata` を `--rerun-tasks` 付きでともに成功させる。通常 build / test で退行を検出できる回帰検査を確定した後、KMP Skill を再レビューする。

## 検証証跡

### Source completeness / 構造

- planned manifest の KMP target は root 1件 + reference 8件で、英日双方の実ファイル集合と一致した。
- root は core API 8 concept 全件と KMP iOS host integration concept、各 reference は実際に依拠する担当 concept を過不足なく source に持つ。`ios-host.md` は core 7件 + KMP iOS host、`android-host.md` は担当する core 7件を持つ。
- `name`、`description`、`license: MIT`、`metadata.language`、`metadata.source` を含む frontmatter は規約に適合した。ja description は `KsDialogs`、`Kotlin Multiplatform`、`KMP`、`commonMain`、`Dialog`、`Loading`、`Toast`、`Android`、`iOS`、`View` を併記する。
- 英日で見出し構造が一致し、全コードブロックは byte-identical、本文の意味も等価だった。
- internal link は全件解決し、Skill root 外への相対 link、ローカル絶対 path、`kasane/`、ADR ID、内部 bridge 型などの利用者向け閉世界性違反は検出しなかった。
- planned manifest を指定した docs-refresh 3e の KMP 候補50件は、handbook の KMP exact exclusion 行に全件一致した。対象 Skill 外・内部層・機械的導出・低頻度 API の分類に未仕分け候補はない。

### API / Setup / 挙動

- Kotlin 2.4.10、Gradle 9.7.0、Android API 24、iOS 17、Swift tools 6.3 と、Maven Central 未配信・planned coordinate・通常 consumer 代替手順未合意という Setup を build 定義と配信状態の記述に照合した。
- singleton と注入可能 interface、Dialog / Loading / Toast の独立 registry、typed result と notifier identity、layout、transition、multi-display、Loading、Toast を concepts・公開実装・tests と照合した。
- Android host の Native View registration、全3 registry の `registerCompose` / `showCompose`、Compose 依存の扱いを公開実装・Sample と照合した。
- iOS host の `Dialog.shared.kmp` / `Loading.shared.kmp` / `Toast.shared.kmp`、UIKit / SwiftUI registration、Dialog notifier、linked package の生成・VCS 管理・direct SwiftPM product を公開実装・build 定義・Sample project と照合した。
- `@Throws` の4 view-model route と message route の非付与を commonMain 公開宣言に照合した。文書の route table は宣言どおりだが、上記 metadata compile failure が残る。

### 機械検査・実行範囲

- planned manifest に対する concepts coverage、en/ja heading parity、code-block parity、frontmatter、closed-world、link resolution、identity、表記ゆれの各検査は成功した。
- KMP API coverage report と handbook exact exclusion の突き合わせは50件すべて一致した。
- `:ksdialogs-kmp:compileKotlinIosSimulatorArm64 --rerun-tasks` と Sample の `:androidApp:compileDebugKotlin --rerun-tasks` は成功した。
- `:ksdialogs-kmp:compileIosMainKotlinMetadata --rerun-tasks` と Sample の `:shared:compileCommonMainKotlinMetadata --rerun-tasks` は同じ3件で失敗した。
- 製品コード・tests に差分がないことを確認し、proposal の合意済み例外に従って全 build root / 全 test suite は実行していない。

## アクションプラン

1. `fix-kmp-iosmain-throws-metadata` で `KsLoading` / `KsToast` と iOS gateway override の `@Throws` filter 不一致を解消する。
2. library と Sample consumer の両 metadata compile を fresh に成功させ、通常の回帰検査へ固定する。
3. KMP Skill の独立レビューを再実施する。

**指摘件数**: Major 1件 / Minor 0件
