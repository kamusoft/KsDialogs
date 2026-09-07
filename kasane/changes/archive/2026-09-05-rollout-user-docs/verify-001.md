# 一致検証結果: rollout-user-docs (001 回目)

**日付**: 2026-09-05
**判定**: VALID (条件つき — 下の「残務」1 件は成果物の記帳更新のみ)
**検証範囲**: `kasane/changes/rollout-user-docs/specs/` の 3 本 (user-skills / docs-refresh / repository-docs) の全 Requirement / Scenario と、commit `53914cc` から作業ツリー (未コミット変更を含む) までの実装差分

## 検証の方法

- 「テスト」列には docs-refresh の検査スクリプト群・lint・README ↔ Skill のコード一致検査・Issue Forms の静的検査を対応付けた。実行は `.agents/skills/docs-refresh/SKILL.md` の 6-①〜6-⑧ と 3d / 3e、`tasks.md` 5 節の手順に従った。
- 予定 manifest はディスクの `skills/.manifest.json` と同一 (判断 JSON なし) として `scripts/planned-manifest.py` / `scripts/targets-list.py` で生成し、検査対象 70 ファイル (Skill 66 + README 4) を得た。
- `deviation.md` の全項目は合意済みの差分として扱い、該当 Scenario は ⚠️ とした。

### 実行した検査と結果

| 検査 | コマンド | 結果 |
|---|---|---|
| 6-① concepts 網羅 | `scripts/concepts-coverage-check.py` (予定 manifest) | `concepts coverage OK` |
| 6-② en/ja 節構成一致 | `scripts/heading-parity-check.py` | `en/ja heading structure OK` |
| 6-③ コードブロック byte 一致 | `scripts/code-block-parity-check.py` | `code blocks byte-identical` |
| 6-④ frontmatter | `scripts/frontmatter-check.py` | `frontmatter OK` |
| 6-⑤ 閉世界性・機械面 | `kasane/` / `ADR-NNNN` / Interop 名の grep + Skill ルート外リンク判定 | 違反 0 件 / `relative links stay inside each Skill root` |
| 6-⑥ 内部リンク解決 | `scripts/link-resolution-check.py` | `All internal links resolve` |
| 6-⑦ identity / local-path lint | `scripts/local-path-lint.py` / `scripts/identity-lint.py` (対象 70 件・リポジトリ全体の双方) | いずれも exit 0 |
| 6-⑧ 配信識別子の表記ゆれ | SKILL.md 6-⑧ の grep パターン | 一致 0 件 |
| 3d ツール最低バージョン | version catalog / wrapper 2 本 / `ios/Package.swift` / MAUI csproj を実読して README 表と突合 | 全 4 行一致 (下表参照) |
| 3e API 名網羅 | `scripts/api-coverage-check.py` + 除外リスト照合 | 報告トークンは全件 `kasane/handbook/cross/user-skill-api-listing.md` の除外リストに存在 (未分類 0 件) |
| 構造 lint | `scripts/doc-structure-lint.py --paths <本 change で変更した長命文書 6 本>` | `構造 lint: 違反なし` |
| README ↔ Skill 最小コード | 4 形態のコードブロック抽出 diff | 4 組とも byte 一致 |
| Issue Forms 静的検査 | YAML parse・必須項目・Platform 7 択・`blank_issues_enabled` | 全項目適合 |
| 移植元 API 網羅 | 移植元 README の API Reference から抽出した公開名 45 件を `api-mapping.md` (en / ja) に照合 | 未掲載 0 件 |

3d の突合値: AGP 9.3.0 / Kotlin 2.4.10 / minSdk 24 / compileSdk 36 (`android/gradle/libs.versions.toml`)、Gradle 9.7.0 (android・kmp の wrapper 2 本とも同値)、Swift tools 6.3 / iOS 17 (`ios/Package.swift`)、`net10.0;net10.0-ios;net10.0-android` / iOS 17.0 / Android 24.0 / Microsoft.Maui.Controls 10.0.1 (`maui/KsDialogs.Maui/KsDialogs.Maui.csproj`)。`kmp/settings.gradle.kts` の `versionCatalogs` が android のカタログを共有している前提も確認済み。

## 対応表

### specs/user-skills/spec.md

| Requirement / Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| Skill 一式の構成 / ディレクトリ構成の検査 | `skills/{en,ja}/ksdialogs-{ios,android,maui,kmp,aiforms-migration}/` | ファイル列挙 (en / ja 同一構成、references 6 / 6 / 7 / 8 / 1 本、規定外ファイル 0) | ✅ 一致 |
| frontmatter の標準準拠 / frontmatter の機械検査 | 10 部の `SKILL.md` frontmatter | 6-④ `frontmatter OK` + 追加静的検査 (`license` 10/10、`metadata.source` 10/10 が `https://github.com/kamusoft/KsDialogs`) | ✅ 一致 |
| frontmatter の標準準拠 / ja 版 description の英語キーワード | `skills/ja/*/SKILL.md` の `description` | 目視確認 (製品名 `KsDialogs` / API 名 `Dialog`・`Loading`・`Toast` / platform 名 `SwiftUI`・`Jetpack Compose`・`.NET MAUI`・`Kotlin Multiplatform` を併記) | ✅ 一致 |
| Skill 本文の構成 / 本文構成と両入口の説明 | platform Skill 4 本の `SKILL.md` | 見出し列挙 (概念説明 → 能力マップ → Setup → 最小例 → レシピ振り分け)、両入口 1 段落の目視、最小コードが既定エントリのみ、iOS / Android references に DI レシピ無し (`di-registration.md` 不在 + DI 語彙 grep 0 件) | ✅ 一致 |
| Skill 本文の構成 / レシピ形式 | platform Skill 4 本の `references/` | 各節が「やりたいこと見出し + リード文 + 完動コード」であることの抽出確認 | ✅ 一致 |
| Skill 本文の構成 / 対応表の形 | `skills/{en,ja}/ksdialogs-aiforms-migration/references/api-mapping.md` | 内容クラス 10 節、各行が旧メンバー / 対応先 (または対応先なし) / 代替手段の 3 列 | ✅ 一致 |
| 閉世界性 / 閉世界性の機械検査 | Skill 10 部 | 6-⑤ (違反 0)、6-⑥ (`All internal links resolve`)、追加で change-id / `kasane` / `openspec` / `samples/` の grep 0 件 | ✅ 一致 |
| KMP Skill の共有コード側と iOS ホスト側 / `@Throws` の最小コードと注意書き | `skills/{en,ja}/ksdialogs-kmp/SKILL.md` | 最小コードに `@Throws(DialogException::class, CancellationException::class)`、Setup iOS host に前提 1 + 手順 3 (Maven 1 点 / `integrateLinkagePackage` を xcodeproj パス指定で 1 回・生成物を VCS へ / Xcode に `KsDialogs-SPM` 1 点)、`@Throws` 3 点 (suspend・非 suspend とも必要 / 失敗しうる VM 経路のみ・message 経路は宣言しない / 無いと NSError 変換されずプロセス終了)、SwiftPM 再宣言の禁止を明記 | ✅ 一致 |
| KMP Skill の共有コード側と iOS ホスト側 / 統合手順の源泉 | `skills/.manifest.json` | `ksdialogs-kmp/SKILL.md` と `ksdialogs-kmp/references/ios-host.md` の `targets` 双方に `kmp/api/ios-host-integration.md` | ✅ 一致 |
| 移行 Skill の対応表 / 旧公開 API の網羅 | `api-mapping.md` (en / ja) | 移植元 README API Reference の公開名 45 件を照合 → 未掲載 0 件。`IReusableLoading.Hide()` は実署名 `Task Hide()` を掲載し README の記載差を注記 | ⚠️ deviation 記録済み (deviation.md 2 行目) |
| 移行 Skill の対応表 / Toast 節の内容 | `api-mapping.md` の「Replace the obsolete Toast surface」節 | 節内容の確認 (新 message 入口 `Toast.Instance.Show(message, durationMs, placement)` を挙動差 3 点 — 上限クランプなし・多重は重なる・完全非対話でタッチ素通し — 付きで案内 / `IToast.Show<TView>()` と具象 View 経路は対応先なしで `ksdialogs-maui` Skill へ誘導 / throws 化の記載 0 件) | ⚠️ deviation 記録済み (deviation.md 1 行目) |
| 生成の内容規約 / コード例のコメント規約 | Skill 10 部の全コードブロック | コメント行抽出 → 0 件 (非 ASCII コメントも 0 件)。ローカル絶対パスは 6-⑦ で 0 件。源泉マップは manifest `targets` 33 キーとして反映済み | ✅ 一致 |
| 翻訳ロックステップ / ロックステップの機械検査 | en / ja 全ペア | 6-② / 6-③ 違反 0 件 | ✅ 一致 |
| manifest 初期版 / 網羅不変条件の検査 | `skills/.manifest.json` | 6-① `concepts coverage OK` (`excluded` は `cross/reference/reference-repositories.md` の 1 本のみ) | ✅ 一致 |
| manifest 初期版 / スキーマ準拠とハッシュの最終状態一致 | `skills/.manifest.json` | 必須キー 5 種あり・`version` 3・`concepts` 11 件がディスクの concept 集合と過不足なく一致・SHA-256 再計算で不一致 0 件・`targets` 33 キーが en / ja 双方に実在し Skill ファイル 33 本と 1:1・`readmes` 4 枚・aiforms の源泉は `core/api/*` + `maui/api/di-registration.md` のみ | ✅ 一致 |
| 索引 README / 索引の構成検査 | `skills/README.md` / `skills/README_ja.md` | 節構成 (一覧 / コピー手順 / 片言語) の 3 要素のみ、Skill 一覧 5 行、`.agents/skills/` を第一候補・Claude Code は `.claude/skills/` | ✅ 一致 |
| レビュー 4 層 / 4 層の通過 | `review-*.md` / `novice-review-001.md` / `tasks.md` 6 節 | ①機械検査は上表のとおり全件合格 ②独立レビューの最終判定は iOS `review-009` / Android `review-013` / MAUI `review-015` / migration `review-020` / cross・docs `review-023` / KMP `review-024` がいずれも APPROVED (`review-021` の CHANGES_REQUESTED は `review-024` が後継として解消) ③初見レビュー `novice-review-001` APPROVED、KMP `SKILL.md` は `308c4a1` 以降無変更のため再実施条件に当たらず ④オーナー検収は tasks 6.5 完了 | ✅ 一致 |

### specs/docs-refresh/spec.md

| Requirement / Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 追従対象の規範 (MODIFIED) / 追従対象の特定 | `.agents/skills/docs-refresh/SKILL.md` 「追従対象」節 | 表の 5 Skill 名・KMP の references 振り分け・移行 Skill の `SKILL.md` + `api-mapping.md`・README 4 枚を確認。`kssettingsview` の grep 一致 0 件 | ✅ 一致 |
| 追従対象の規範 (MODIFIED) / Toast の移行方針の一致 | 同 SKILL.md の移行 Skill 行と `api-mapping.md` の Toast 節 | 両者とも「message 入口は移行案内あり・View 登録は対応先なし」で一致。「Toast は対応先なし」とだけ書いた記述は SKILL.md に無し | ✅ 一致 (下の所見 1 参照) |
| 追従対象の README 群 / 追従対象の列挙 | `skills/.manifest.json` の `readmes` | `skills/README.md` / `skills/README_ja.md` / `README.md` / `README_ja.md` の 4 枚のみ。`android/layout-case-fixtures/README.md` は不在 | ✅ 一致 |
| 追従対象の README 群 / 初期生成後の起動 | `skills/.manifest.json` + `.agents/skills/docs-refresh/SKILL.md` | 停止条件 (不在 / parse 不能 / `version`≠3 / 必須キー欠落) をいずれも満たさないこと、器 `ksn-implementer` が配置済みであること、`DOCS_REFRESH_README_ONLY=1` の対象一覧が 4 枚になること、README 言語ペアの 6-② / 6-③ が差分 0 で通ることを確認 | ✅ 一致 |
| API 掲載基準の参照 / 注記の差し替え | `.agents/skills/docs-refresh/SKILL.md` 3e 節 | `kasane/handbook/cross/user-skill-api-listing.md` への参照あり、`phase-2` の暫定注記は grep 一致 0 件 | ✅ 一致 |
| 源泉 concept の追加 / 新設 concept の追従 | `skills/.manifest.json` | `concepts` に `kmp/api/ios-host-integration.md` のハッシュあり (再計算一致)、`ksdialogs-kmp/SKILL.md` と `ksdialogs-kmp/references/ios-host.md` の源泉に含まれる | ✅ 一致 |

### specs/repository-docs/spec.md

| Requirement / Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| README の所在 / 公開ドキュメント面の README 集合 | `README.md` / `README_ja.md` / `skills/README.md` / `skills/README_ja.md` / `android/layout-case-fixtures/README.md`、`samples/` 配下 5 本は削除済み | 公開面の `README*.md` 列挙 (ビルド生成物・SwiftPM checkout 除外) → 上記 5 枚のみ | ✅ 一致 |
| README の所在 / 廃止した README への参照の解消 | 参照付け替え済みの `kasane/decisions/cross/0010-*.md` / `kasane/config.yaml` / `kasane/handbook/cross/sample-parity.md` | active な文書の全文検索。残存は accepted な cross/ADR-0006・0007 の既存本文と過去 footer のみ (履歴として保持) | ⚠️ deviation 記録済み (deviation.md 最終行) |
| ルート README の節構成 / 開発者向け手順の不在 | `README.md` / `README_ja.md` | ビルドコマンド・SDK 設定手順・ビルドルート表の不在、`samples/` 配下文書へのリンク 0 件 (リポジトリ構成の `samples/` 行はリンク無しの 1 行説明) | ✅ 一致 |
| ルート README の節構成 / 節の順序と Skills 導線 | 同上 | 見出し順 (状態表記 → 概要と主な特徴 → スクリーンショット → 対応プラットフォーム → インストール → 最小コード例 → Agent Skills → リポジトリ構成 → 貢献 → ライセンス) を en / ja とも確認。en は `skills/README.md`、ja は `skills/README_ja.md` を指す | ✅ 一致 |
| 対応プラットフォーム表 / 3d 取得元との一致 | `README.md` / `README_ja.md` の対応プラットフォーム節 | 3d の取得元 4 行を実読して突合 → 全値一致 (上の 3d 突合値) | ✅ 一致 |
| 対応プラットフォーム表 / 注記の内容 | 同節の直下の注記 | minSdk 24 / compileSdk 36 / Microsoft.Maui.Controls 10.0.1 の記載、ビルド版と利用側最小の区別、Kotlin 最小版は「確定前 (初回リリースまでに確定)」で推測値なし、SwiftPM 連携が Alpha である旨 | ✅ 一致 |
| インストールと最小コード例 / 導入手順の委譲 | `README.md` / `README_ja.md` のインストール節 | 4 形態の依存宣言と prerelease 指定のみ。IDE 操作・module 構成・要件表は Agent Skills へ委譲する案内。未配信を理由とした代替手順は無し | ✅ 一致 |
| インストールと最小コード例 / 最小コード例の一致 | 最小コード例 4 例 ↔ `ksdialogs-{ios,android,maui,kmp}/SKILL.md` の最小コードブロック | コードブロック抽出 diff → 4 組とも byte 一致 | ✅ 一致 |
| インストールと最小コード例 / 座標の文書間一致 | インストール節 ↔ 各 `SKILL.md` の Setup | SwiftPM `https://github.com/kamusoft/KsDialogs-SPM` + product `KsDialogs` / Maven `jp.kamusoft:ksdialogs`・`ksdialogs-compose` / NuGet `KsDialogs.Maui` `0.1.0` / Maven `jp.kamusoft:ksdialogs-kmp` + iOS の SwiftPM 1 点 — いずれも同値 | ✅ 一致 |
| スクリーンショットの提示 / 6 枚の組み合わせの網羅 | `assets/{ios,android}-{dialog,loading,toast}.png` (6 枚) | ファイル存在確認と、en / ja README が同一の raw URL を参照し キャプションのみ言語別であることの確認。MAUI / KMP は画像を置かず文で同一画面である旨を記載 | ✅ 一致 |
| スクリーンショットの提示 / 端末固有情報の不在 | 同 6 枚 | 6 枚すべてを原寸で目視 → ステータスバー領域が含まれず、キャリア名・時刻・バッテリー残量の表示なし。撮影経路のうち iOS Toast のみ実メニュー操作 | ⚠️ deviation 記録済み (deviation.md 3 行目 = 撮影経路。端末固有情報の不在自体は ✅) |
| 配信準備中の状態表記 / 解除箇所の単一性 | `README.md:3` / `README_ja.md:3` | 未配信・準備中を示す表現の grep → 冒頭 1 箇所のみ。インストール節には無し。API 安定性 (0.x) の記述は概要節に常設として分離 | ✅ 一致 |
| 英日 README の翻訳ロックステップ / 構成の一致 | `README.md` / `README_ja.md` | 6-② / 6-③ (README 言語ペア) 違反 0 件 | ✅ 一致 |
| 開発者向け知識の所在 / 撮影用の起動引数の到達可能性 | `kasane/handbook/cross/sample-parity.md` 「撮影支援の起動引数」節 | キー 2 つ (`demo` / `loading-step-interval-ms`)、安定デモ ID 14 件、4 ルートのアプリ識別子、iOS / Android の外部表現をすべて確認 | ✅ 一致 |
| 開発者向け知識の所在 / Sample のビルド手順の到達可能性 | `kasane/handbook/cross/local-development-setup.md` 「Sample のビルドと実行」節 | 冒頭表 + 4 ルート小節に参照方式 (理由つき) と起動コマンドが揃っていることを確認 | ✅ 一致 |
| 開発者向け知識の所在 / KMP iOS 統合の到達可能性 | `kasane/concepts/kmp/api/ios-host-integration.md` | 「初回の統合手順」(Maven 依存 1 点・合成パッケージ生成・登録 API 用 SwiftPM 1 点) と「Sample で合成 package を再生成する」節を確認 | ✅ 一致 |
| 開発者向け知識の所在 / 参照元の付け替え | `kasane/decisions/cross/0010-*.md` / `kasane/config.yaml` / `kasane/handbook/cross/sample-parity.md` | 3 ファイルとも `samples/README` への言及 0 件、ADR-0010 と config は `kasane/handbook/cross/sample-parity.md` の該当節を指す。sample-parity.md の「関連」節の該当行は削除済み | ✅ 一致 |
| 利用者向け Skill の API 掲載基準 / 3e の仕分け | `kasane/handbook/cross/user-skill-api-listing.md` | 3e の全報告トークンを除外リストへ機械照合 → 未分類 0 件。各行に基準 (低頻度 / 内部層 / 機械的導出 / 可視性引き下げ候補 / 対象 Skill 外) が付与済み。方針・してはいけないこと・コメント規約の各節も存在 | ✅ 一致 |
| 貢献方針の表明 / README だけを読む人への到達 | `README.md` / `README_ja.md` の貢献節、`.github/CONTRIBUTING.md` / `.github/CONTRIBUTING_ja.md` | README 側に PR 不受理・Issue で受ける・テンプレート利用の案内と CONTRIBUTING へのリンク。CONTRIBUTING 2 枚は方針の理由と Issue の書き方を持ち相互リンク済み | ✅ 一致 |
| Issue テンプレートの必須項目 / Forms の静的検査 | `.github/ISSUE_TEMPLATE/` 4 ファイル | YAML parse 成功。bug_report = Version / Platform / 再現手順 / 実際の挙動 / 期待した挙動、feature_request = 解決したい課題 / 現状の困りごと / 検討した代替案、question = Version / Platform / 試したこと / 参照した Skill・README 節 が全件 `required: true`。Platform dropdown は 2 本で同一の 7 択かつ単一選択。`blank_issues_enabled: false`。3 本とも冒頭に英日いずれでも書いてよい旨の案内 | ✅ 一致 |

## 追加検査

| 項目 | 結果 |
|---|---|
| tasks.md の完了状況 | **6.4 が未チェックのまま**だが、その残務 (KMP の独立レビュー再実施と APPROVED 取得) は本検証の実施中に `review-024.md` (APPROVED) として成立した。他 34 件はチェック済みで、対応表と突き合わせて虚偽チェック (未実装なのにチェック済み) は検出されなかった (8.3 は実行ログを直接確認できないため、停止条件の不成立・器の配置・`--readme-only` 対象 4 枚・README 言語ペア検査の差分 0 を再現して代替確認した) |
| 逆流検査 | `proposal.md` / `design.md` / `specs/` の 3 本は `53914cc` から差分 0。`ui/brief.md` のみ差分があるが、内容は tasks 3.2 が求める採用スクリーンショットの承認記録の追記であり足場の書き換えではない |
| 未記録乖離 | 下の「未対応項目」1 件のみ。`deviation.md` の `[付随修正]` 3 行 (docs-refresh SKILL.md / prompt-readme.md、`core/api/toast-semantics.md`、cross/ADR-0006・0007 の footer) は差分と一致しており、Requirement 非対応の変更で記録漏れのものは無かった |
| 検証範囲外の差分 | `kmp/ksdialogs-kmp/src/iosMain/.../IosLoadingGateway.kt` / `IosToastGateway.kt`、`kasane/decisions/kmp/0001-*.md`、`kasane/lessons/inbox/kmp-completion-must-run-hierarchical-metadata-compile.md`、`kasane/changes/archive/2026-09-05-fix-kmp-iosmain-throws-metadata/`、`kasane/roadmaps/package-distribution/phases/phase-4-verification-ci/agenda.md`、`kasane/changes/refine-docs-refresh-api-token-extraction/exploration.md` は別 change (`fix-kmp-iosmain-throws-metadata` および簡易起票) の成果物であり、本 change の検証対象外として扱った |
| UI 変更の記録 | `ui/brief.md` に承認日 2026-09-05・撮影 revision・採用 6 枚の配置先表・比較条件があり、iOS Toast の撮影経路の妥協が本文と `deviation.md` の双方に記録済み |
| テストの実行 | 本 change は製品コード・テストに触れないため全ビルドルートの全 suite は deviation 9.1 で省略済み。代わりに上表の検査一式を今回すべて実行し、全件合格を確認した |

## 未対応項目 (❌)

なし。全 Requirement / Scenario が「✅ 一致」または「⚠️ deviation 記録済み」である。

## 残務 (成果物の記帳更新 — 判定を左右しない)

### 1. `tasks.md` 6.4 のチェックとレビュー最終状態の記述

- **事実**: 検証開始時点で `tasks.md` の 6.4 は未チェックで、末尾の「レビュー最終状態」に「KMP の `review-021.md` が CHANGES_REQUESTED」「6.4 の残りは KMP の独立レビュー再実施と APPROVED 取得」と書かれていた。本検証の実施中に KMP を対象とした再レビューが `review-024.md` (APPROVED) として成立し、Scenario「4 層の通過」が要求する「独立レビューの最終判定が全件 APPROVED」は満たされた。
- **見立て**: 実装・仕様の乖離ではなく記帳の遅れである。6.4 を `[x]` にし、「レビュー最終状態」の一文を KMP = `review-024.md` APPROVED へ更新すれば整う。deviation として合意する筋の項目ではない。

## 所見 (❌ ではない指摘)

1. **Toast の前提の表現差**: `.agents/skills/docs-refresh/SKILL.md` の追従対象の表は「旧 Toast の message 入口は新 API の移行案内を持ち」と書き、旧 message 入口の実在を前提とする言い回しになっている。一方 `api-mapping.md` は deviation のとおり「旧 message overload は存在せず、新 message 入口は移行後の代替」と扱う。Scenario「Toast の移行方針の一致」が要求する 2 点 (message 入口は移行案内あり / View 登録は対応先なし) は両者とも満たすため ✅ としたが、前提の書きぶりは揃っていない。docs-refresh SKILL.md 側の 1 行を deviation の事実に合わせて書き直すと、次回以降の追従判断で誤読しない。

2. **`--readme-only` の実起動は未確認**: tasks 8.3 の完了は、停止条件の不成立・器の配置・対象 4 枚・README 言語ペア検査の差分 0 という**構成要素の再現**で代替確認した。docs-refresh の全体フローはサブエージェント委譲を伴うため、読み取り専用の本検証では実起動していない。

3. **検証中に成果物が増えた**: 本検証の実行中に `review-024.md` が追加された。上の対応表・残務はこれを反映済みだが、`tasks.md` の記述は検証開始時点のまま (残務 1)。
