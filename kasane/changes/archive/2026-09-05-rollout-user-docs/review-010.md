# レビュー結果: rollout-user-docs (010 回目)

**日付**: 2026-09-05
**判定**: CHANGES_REQUESTED

## サマリー

Android Skill の英日 14 ファイルは、8 本の core API concept、Android 公開実装・テスト・Sample、accepted Android ADR と突き合わせた範囲で、API 署名・Setup・挙動・構成・翻訳ロックステップ・閉世界性に問題はなかった。特に View-only と Compose の配布境界、Compose artifact から core への推移依存、BOM と Compose 1.8.1 の対応、Toast の `showCompose`、`proportionalHeight` を含む比率サイズを確認した。ただし、予定 manifest の Android ルート `SKILL.md` に対する源泉割当が本文の実依存を覆っておらず、ファイル単位の源泉完全性を満たさない Major が 1 件ある。

## 照合した規約

- `kasane/handbook/cross/user-skill-api-listing.md`（`skills/**` の API 掲載基準と 3e exact 分類）
- `kasane/handbook/cross/test-execution.md`（テスト実行。全ビルドルート省略は `proposal.md` の合意済み例外を適用）
- `kasane/handbook/cross/local-development-setup.md`（Android build root と Sample の実行条件）
- `kasane/changes/rollout-user-docs/specs/user-skills/spec.md`（Skill 構成、閉世界性、源泉完全性、英日等価性）
- `kasane/decisions/android/0001-compose-api-separate-module.md`（accepted。core の Compose 非依存と Compose artifact の分離）
- `kotlin-impl-skill` / `jetpack-compose-impl-skill`（Kotlin・Compose の API 使用、ライフサイクル、キャンセル、安全な composable の観点）

## 指摘事項

### [🟠 Major] Android ルート `SKILL.md` の予定 manifest が本文の全源泉を追跡しない

**該当箇所**: `design.md:67`、`design.md:78`、`skills/en/ksdialogs-android/SKILL.md:12`、`skills/en/ksdialogs-android/SKILL.md:18`、`skills/en/ksdialogs-android/SKILL.md:77`

**問題点**: Decision 3 の初期割当では `ksdialogs-android/SKILL.md` の源泉が `core/api/registration-show-semantics.md` だけである。一方、実ファイルは冒頭と能力マップ、recipe 振り分けで、型付き結果・多段表示・ViewModel binding・layout・transition・Loading・Toast の契約と公開 API を明示している。この内容は registration-show だけではなく、core/api の 8 concept 全てに直接依拠する。設計自身も、初期値はワーカーのファイル別源泉マップで最終確定し、各ファイルが依拠する concept を全て載せることを独立レビューの合格条件としている。現在のディスク manifest は予定どおり bootstrap の `targets: {}` のため、これを上書きする別の現行予定 manifest もレビュー対象内には存在せず、Decision 3 の割当のままでは Loading や Toast などの concept だけが変わった際にルート能力マップが追従対象へ入らない。

**推奨修正**: 予定 manifest を構成するファイル別源泉マップで、`ksdialogs-android/SKILL.md` に次の 8 本を全て割り当てる。references 6 本の既存割当はそのまま維持し、修正後の予定 manifest で source completeness と機械検査を再実行する。

- `core/api/registration-show-semantics.md`
- `core/api/result-notification-semantics.md`
- `core/api/multi-display-semantics.md`
- `core/api/model-binding-semantics.md`
- `core/api/layout-semantics.md`
- `core/api/transition-semantics.md`
- `core/api/loading-semantics.md`
- `core/api/toast-semantics.md`

## 確認結果

### source・API・挙動

- 8 concept 全件を読み、Android Skill の各記述を公開実装、API surface check、unit / instrumented tests、Android Sample の利用コードと突き合わせた。references の源泉割当は `dialogs` = registration/result/multi、`view-models` = model、`layout`、`transitions`、`loading`、`toast` の各 1 本で内容と一致する。
- View-only の依存は core 1 点、Compose は `ksdialogs-compose` 1 点で core が推移する構成であり、accepted Android ADR と `api(project(":ksdialogs"))` に一致する。`:ksdialogs:verifyNoDeclarativeUiDependency` も成功した。
- Kotlin 2.4.10、minSdk 24、Compose 1.8.1、Lifecycle 2.8.7、Coroutines 1.11.0 は version catalog と一致する。Compose BOM 2025.05.00 のローカル解決済み POMでも Foundation / runtime / ui が 1.8.1 であることを確認した。
- `Toast.instance.showCompose(...)` は `KsToast.showCompose` の実シグネチャ（非 suspend、`durationMs`、`placement`、Compose content）と一致する。`DialogOptions.proportionalHeight` は `proportionalWidth` と同じ正規化規則を説明し、コード例にも掲載されている。
- Loading の imperative / scoped / registered Compose 経路、Toast の message / registered / View inline / Compose inline 経路、Dialog の registered / inline / multi-display、ViewModel notifier / typed show、layout の優先順位、transition の post-dismissal result を確認し、concept と矛盾する記述はなかった。

### 3e exact 分類

監査用 manifest で core 8 concept を Android Skill に集約して `api-coverage-check.py` を実行し、報告された全 token を handbook の Android 行へ exact match で分類した。未分類は 0 件。

- 内部層: `LoadingCoordinator`
- 対象 Skill 外・機械検査由来: `DialogException.ValueTypeViewModel`、`ValueTypeViewModel`、`IDialogViewModel`、`Dismissal`、`Presentation`、`viewModelAlreadyShowing`、`viewModelFactoryNotRegistered`、`TimeSpan`、`TimeSpan.MaxValue`、`AnyObject`、`TimeInterval`、`UIHostingController`、`UITimingCurveProvider`、`UIView`、`Dialog.Instance`、`Dialog.shared`、`IKsDialogs`、`IKsLoading`、`IKsToast`、`Loading.Instance`、`Loading.shared`、`ProportionalWidth`、`RegisterForDialog`、`ShowAsync`、`ShowResultAsync`、`Toast.Instance`、`Toast.shared`、`UseCurrentPageLocation`、`C05`、`C19`、`approvedBy`、`approvedDiff`
- 機械的に導出できる名前: `AbstractComposeView`、`Easing`、`LazyColumn`

### 機械検査・限定 compile

- 8 concept を実依存どおり割り当てた監査用 manifest: `concepts coverage OK`
- `heading-parity-check.py`: `en/ja heading structure OK`
- `code-block-parity-check.py`: `code blocks byte-identical`
- `frontmatter-check.py`: `frontmatter OK`
- `link-resolution-check.py`: `All internal links resolve`
- Android Skill 14 ファイルへの local-path / identity lint、閉世界性・機械面 grep: 違反 0 件
- `./gradlew :ksdialogs:compileDebugKotlin :ksdialogs-compose:compileDebugKotlin :api-surface-check:compileDebugKotlin :ksdialogs:verifyNoDeclarativeUiDependency`: 成功
- Sample build root の `./gradlew :app:compileDebugKotlin`: 成功
- 全ビルドルートの全件実行は、製品コード・テストを変更しない本 change に対する `proposal.md` の合意済み例外に従い省略した。

## アクションプラン

1. Android ルート `SKILL.md` の予定 manifest 源泉を core 8 concept 全てへ補正する。
2. 補正した予定 manifest で source completeness と docs-refresh 機械検査を再実行する。
3. Android Skill を再レビューし、Major が解消したことを確認する。
