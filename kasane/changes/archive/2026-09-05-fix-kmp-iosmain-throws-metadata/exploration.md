# Exploration: fix-kmp-iosmain-throws-metadata

## 課題 / 動機

`rollout-user-docs` の KMP Skill 最終レビュー中に、Kotlin 2.4.10 の階層化 source set で iOS metadata を再コンパイルすると、公開 interface と iOS 実装 override の `@Throws` filter 不一致によりビルドが失敗する事象を発見した。

- `kmp/` で `./gradlew :ksdialogs-kmp:compileIosMainKotlinMetadata --rerun-tasks` を実行すると、`IosLoadingGateway.kt` 2 箇所と `IosToastGateway.kt` 1 箇所で `Member overrides different '@Throws' filter` が発生する
- `samples/kmp/` で `./gradlew :shared:compileCommonMainKotlinMetadata --rerun-tasks` を実行しても、included build 内の同じ task・同じ3件で失敗する
- 対照として `:ksdialogs-kmp:compileKotlinIosSimulatorArm64 --rerun-tasks`、Android Sample の compile、KMP iOS Sample の framework link は成功しており、`iosMain` metadata/commonization 経路に限定して再現する
- 発見時の詳細証跡は `../rollout-user-docs/review-012.md` にある

### 探索で確定した事実 (2026-09-05)

- **ソース上の不一致は無い**。`kmp/ksdialogs-kmp/src/commonMain/kotlin/jp/kamusoft/ksdialogs/kmp/KsLoading.kt` (`show(viewModel)` / `start(viewModel)`) と `KsToast.kt` (`show(viewModel)`) の `@Throws` と、`iosMain/.../IosLoadingGateway.kt` / `IosToastGateway.kt` の override の `@Throws` は完全に同一
- **切り分け実験** (iosMain に一時ファイルを置いて `compileIosMainKotlinMetadata --rerun-tasks`、実験後に削除):
  - iosMain 内で閉じた interface + 同じ `@Throws` の override → 通る
  - commonMain の `KsToast` を iosMain で override し `@Throws` を書かない → 通る
  - commonMain の interface を iosMain で override し同じ `@Throws` を書く (現行) → 失敗
- **根本原因は Kotlin 側の既知の回帰バグ** [KT-88548](https://youtrack.jetbrains.com/issue/KT-88548) (State: Fixed、affected: 2.3.0 / **2.4.10** / 2.5.0-Beta1、修正版: **2.5.0-Beta1**、2.4.x へのバックポート記録なし)。native の metadata compile task は Metadata CLI ではなく Native CLI で走るため、KT-85036 で分離した「metadata compile では走らせない checker」の除外が `FirNativeThrowsChecker` (`INCOMPATIBLE_THROWS_OVERRIDE`) に効いておらず、commonMain interface の `@Throws` と同一 filter を native 系 source set の override が持つと false positive になる。発火条件 3 つ (JVM 系 target あり / native target 2 つ以上 / interface が commonMain) を本プロジェクトは全て満たす。KT-84192 (2.3.20-RC で修正) の再発
- `CancellationException` の有無は無関係 (Toast 側は `DialogException` のみでも落ちる)
- Android 側の override (`androidMain/.../AndroidLoadingGateway.kt` / `AndroidToastGateway.kt`) は JVM のため `@Throws` 一致検査が無く、影響なし
- 通常 consumer への露出: `:shared:compileCommonMainKotlinMetadata` は Sample の `build` / `assemble` (allMetadataJar) と IDE import で走る。Android アプリの assemble と iOS framework link だけでは走らないため、target 単体の確認では見落とす

## 検討した選択肢 (却下案と理由を含む)

**論点 1: 修正形**

- **A. iosMain の override 3 箇所から `@Throws` を外し、commonMain の interface 側の宣言はそのまま残す** — 採用。KT-88548 に記載の公式回避策。stdlib の `kotlin.Throws` ドキュメントは「この annotation を持つ、または**継承する**関数は ObjC では NSError 産出メソッド、Swift では throws メソッドとして表現される」と明記しており、加えて Swift 利用者が触るのは commonMain の interface (protocol) 側で、実装クラスは `internal` で export されない。よって公開契約 (Swift の NSError) は不変。切り分け実験で通ることを確認済み。変更は 3 行で可逆
- B. Kotlin を 2.5.0-Beta1 以降へ上げる — 却下 (本 change では)。恒久解だが Beta 採用の可否と toolchain 全体 (AGP / Android / Sample) の整合を巻き込む。package-distribution の toolchain 版の論点として申し送る
- C. 実装クラスを iosMain から各 target source set (iosArm64Main / iosSimulatorArm64Main / iosX64Main) へ複製する — 却下。metadata compile の対象から外れるはずだが未検証で、3 重の重複コードが残る
- D. `@Suppress("INCOMPATIBLE_THROWS_OVERRIDE")` で抑える — 却下。エラー診断が抑制できるか未検証で、A が確実に通る以上選ぶ理由が無い
- E. interface 側から `@Throws` を外して実装側に残す — 却下。Swift に見える protocol が throws でなくなり公開契約が壊れる (add-kmp-loading-toast-throws の決定を覆す)
- F. metadata compile task の無効化・skip 系フラグ — 却下。metadata klib は consumer (included build / IDE) が参照する成果物で、公開ライブラリでは潰せない

**論点 2: 回帰検査の扱い**

- **2-a. 修正 + 手動の `--rerun-tasks` 実行証跡だけにし、通常の build / check 経路への組み込みは phase-4 (verification-ci) へ申し送る** — 採用。S 級に収まる
- 2-b. 本 change で Gradle の build / check 経路に組み込む — 却下。Gradle 配線が入り M 級寄りになる。CI 構築のフェーズでまとめて扱う方が筋がよい

## 決定事項

- 2026-09-05: **iosMain の override 3 箇所 (`IosLoadingGateway.show(viewModel)` / `IosLoadingGateway.start(viewModel)` / `IosToastGateway.show(viewModel)`) から `@Throws` を外す。commonMain の `KsLoading` / `KsToast` の宣言は維持する** (オーナー確定、案 A)
- 2026-09-05: **回帰検査は手動実行の証跡のみ。build / check 経路への組み込みは phase-4 (verification-ci) へ申し送る** (オーナー確定、案 2-a)
- 実装時の留意:
  - override 側に「commonMain の宣言を継承する。KT-88548 (2.4.10 の metadata compile false positive、2.5.0 で修正) の回避のため書かない」旨を参照付きコメントで残し、2.5.0 採用時に戻せる状態にする。Android 側の override は触らなくてよい (対称性のためコメントを揃えるかは実装時判断)
  - 検証: `kmp/` の `:ksdialogs-kmp:compileIosMainKotlinMetadata --rerun-tasks` と `samples/kmp/` の `:shared:compileCommonMainKotlinMetadata --rerun-tasks` が成功すること、および `compileKotlinIosSimulatorArm64` と KMP iOS Sample の framework link が引き続き成功することを `evidence/` に残す
  - 公開契約の裏取り: framework の生成 ObjC ヘッダで `KsLoading` / `KsToast` protocol の VM 経路が `error:` 付きのまま (変化なし) であることを確認する
  - kmp の既存テスト (commonTest 51 件) を実行する
- 申し送り:
  - package-distribution phase-4 (verification-ci): KMP の検証 CI に `compileIosMainKotlinMetadata` 系 (階層化 source set の metadata compile) を含める。target 単体の compile だけでは本件を見落とす
  - package-distribution の toolchain 版の論点: Kotlin 2.5.0 以降へ上げたときに本回避を撤回して override 側に `@Throws` を戻せる (戻さなくても契約は同じ)
  - `rollout-user-docs` review-012 の Major: 本 change の完了後に KMP Skill を再レビューする
  - `rollout-user-docs` の KMP Skill 再レビュー時: 同じ false positive は利用者が iosMain で `KsLoading` / `KsToast` を実装 (テスト差し替え等) する場合にも当たるため、注意書きの要否を判断する (review-001 Suggestion)
  - 蒸留時: 撤回条件 (Kotlin 2.5.0 で override へ `@Throws` を戻せる) が短命層にしか無いので、kmp/ADR-0001 の現行照合か concepts へ 1 行残す (review-001 Suggestion)

## ADR 候補 (作成済み: なし / 未起票: なし)

kmp/ADR-0001 (素の suspend 直接公開・`@Throws` で NSError 化) の契約は不変で、Kotlin の既知バグに対する実装上の回避にすぎない。ADR 級の判断は含まない。蒸留時は kmp/ADR-0001 の現行照合か concepts の該当箇所に「実装側は継承に頼る (KT-88548)」の 1 行を足す程度でよい

## 未決の論点

- なし (探索で全件解消)

## UI 素材 (ui/references/ の一覧と注釈)

なし

## 変更級の推奨: S

理由: iosMain の 2 ファイル・3 行の削除と参照コメント。公開 API 変更なし (Swift の NSError 契約は interface の宣言を継承して不変)、触る能力は KMP の Loading / Toast 委譲面のみ、UI なし、可逆 (2.5.0 採用時に戻せる)。回帰検査は手動証跡のみで Gradle 配線を伴わない。独立レビューは必須 (S 級の規律)
