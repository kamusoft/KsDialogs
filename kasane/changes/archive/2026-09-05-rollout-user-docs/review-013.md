# レビュー結果: rollout-user-docs (013 回目)

**日付**: 2026-09-05
**判定**: APPROVED

## サマリー

Android Skill の authoritative planned manifest を直接確認した結果、ルート `SKILL.md` は core 8 concept 全件を持ち、references 6 本も各ファイルの内容が依拠する source を過不足なく持っていた。前回 review-010 の Major は、task 8 前の bootstrap `skills/.manifest.json` を予定 source map の証拠として扱った証拠選択ミスであり、実際の予定 manifest には存在しない。文書・実装に変更はないため、API・Setup・挙動・英日等価性・閉世界性についての前回の適合結果と、今回再実行した機械検査・限定 compile を合わせ、最終判定を APPROVED とする。

## 照合した規約

- `kasane/handbook/cross/user-skill-api-listing.md`（`skills/**` の API 掲載基準と 3e exact 分類）
- `kasane/handbook/cross/test-execution.md`（テスト実行。全ビルドルート省略は `proposal.md` の合意済み例外を適用）
- `kasane/handbook/cross/local-development-setup.md`（Android build root と Sample の実行条件）
- `specs/user-skills/spec.md`（manifest、ファイル単位の源泉完全性、Skill 構成、英日等価性、閉世界性）
- `kasane/decisions/android/0001-compose-api-separate-module.md`（accepted。core の Compose 非依存と Compose artifact の分離）
- `kotlin-impl-skill`（Android 公開 API と限定 compile の Kotlin 観点）

## 指摘事項

なし。

## 再判定の証拠

### authoritative planned manifest

task 8 前のディスク上の `skills/.manifest.json` は `targets: {}` の bootstrap であり、この段階では予定どおりである。実装・6.4 レビューで使う authoritative planned manifest を `DOCS_REFRESH_MANIFEST` として直接読み、Android の全 target を次のとおり確認した。

| target | source concepts |
|---|---|
| `ksdialogs-android/SKILL.md` | `registration-show`、`result-notification`、`multi-display`、`model-binding`、`layout`、`transition`、`loading`、`toast` の core 8 本全件 |
| `ksdialogs-android/references/dialogs.md` | `registration-show`、`result-notification`、`multi-display` |
| `ksdialogs-android/references/view-models.md` | `model-binding` |
| `ksdialogs-android/references/layout.md` | `layout` |
| `ksdialogs-android/references/transitions.md` | `transition` |
| `ksdialogs-android/references/loading.md` | `loading` |
| `ksdialogs-android/references/toast.md` | `toast` |

`jq -e` による期待配列との exact 比較は `true` だった。manifest の Android target 7 キーと `skills/en/ksdialogs-android/` に実在する Markdown 7 ファイルも集合として一致し、source concept は全て実在した。各ファイルの本文も再度 source map と突き合わせ、ルートの能力マップ・recipe 振り分けは 8 concept、各 reference は上表の担当 concept で完全に説明できるため、ファイル単位の源泉完全性を満たす。

### 機械検査

authoritative planned manifest を入力として再実行した。

- `concepts-coverage-check.py`: `concepts coverage OK`
- `heading-parity-check.py`: `en/ja heading structure OK`
- `code-block-parity-check.py`: `code blocks byte-identical`
- `frontmatter-check.py`: `frontmatter OK`
- `link-resolution-check.py`: `All internal links resolve`
- `api-coverage-check.py`: Android の報告 token は `kasane/handbook/cross/user-skill-api-listing.md` の現行 Android 分類に全て exact match し、未分類 0 件

### API・Setup・限定 compile

- View-only の core 1 点と Compose の `ksdialogs-compose` 1 点から core が推移する構成、Compose BOM 2025.05.00 と Compose 1.8.1、Kotlin 2.4.10、minSdk 24、Lifecycle 2.8.7、Coroutines 1.11.0 の一致を維持している。
- Toast の View inline / Compose inline (`Toast.instance.showCompose`) と `proportionalHeight` を含む layout、Dialog / Loading / Toast の公開 API・挙動は、前回の公開実装・tests・Sampleとの全件突合後から文書・実装に変更がない。
- Android build root の `./gradlew :ksdialogs:compileDebugKotlin :ksdialogs-compose:compileDebugKotlin :api-surface-check:compileDebugKotlin :ksdialogs:verifyNoDeclarativeUiDependency`: 成功。
- Android Sample build root の `./gradlew :app:compileDebugKotlin`: 成功。
- 全ビルドルートの全件実行は、製品コード・テストを変更しない本 change に対する `proposal.md` の合意済み例外に従い省略した。

## アクションプラン

なし。Android Skill は次のゲートへ進められる。
