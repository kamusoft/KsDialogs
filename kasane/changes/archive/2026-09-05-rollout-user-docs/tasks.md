# Tasks: rollout-user-docs

順序依存は design.md「Migration Plan」のとおり。グループ 1 は Skill 生成の源泉を先に作る。`samples/` README の廃止 (グループ 7) は検収通過後に置く (生成の素材として使い終えるまで残す)。

## 1. 長命層の準備 (生成の前提)

- [x] 1.1 `kasane/concepts/kmp/api/ios-host-integration.md` を新設する (type: description。内容と出典は design Decision 4。Sample での再生成手順の節を含む)。`kasane/concepts/kmp/index.md` を新設し `kasane/concepts/index.md` の kmp 行と `kasane/concepts/log.md` を更新する (→ Requirement: 開発者向け知識の所在 / 源泉 concept の追加)
- [x] 1.2 `kasane/handbook/cross/user-skill-api-listing.md` を新設する (kind: rule。翻案元 `../KsSettingsView/kasane/handbook/cross/user-skill-api-listing.md` の構成を KsDialogs へ翻案。除外リストは「6.3 で初期化」の空表で置く)。`kasane/handbook/cross/index.md` に登載し log に記録する (→ Requirement: 利用者向け Skill の API 掲載基準)
- [x] 1.3 `kasane/handbook/cross/sample-parity.md` に「撮影支援の起動引数」節を追加する (`samples/README.md` の同名節を規範として書き直す: キー 2 つ・外部表現・アプリ識別子・安定デモ ID 14 件)。「関連」節の `samples/README.md` 行を削除する (→ Requirement: 開発者向け知識の所在)
- [x] 1.4 `kasane/handbook/cross/local-development-setup.md` に「Sample のビルドと実行」節を追加する (冒頭表 + ルート別小節: 参照方式とその理由・起動コマンド。出所は design Decision 2 の B') (→ Requirement: 開発者向け知識の所在)
- [x] 1.5 廃止対象 5 本の全節を design Decision 2 の対応表と突き合わせ、A 分類 (捨てる) が既出であることを 1 件ずつ確認する。既出でない差分があれば行き先の文書へ補う (→ Requirement: 開発者向け知識の所在)

## 2. skills/ 10 部の生成 (Skill 単位 fan-out、最大 3 並列、各ワーカーが en / ja ペアを同時生成)

各ワーカーへ渡すもの: 源泉 concepts (design Decision 3 の割当)・実装コード / テストの所在・design Decision 1 (references 構成)・`.agents/skills/docs-refresh/references/prompt-skill.md` の内容規約・user-skills スペックの該当 Requirement。報告に「ファイル別の源泉 concepts マップ」「drift 所見」を含めさせる。

- [x] 2.1 `ksdialogs-ios` en / ja (SKILL.md + references 6 本) (→ Requirement: Skill 一式の構成 / Skill 本文の構成 / 閉世界性 / 生成の内容規約 / 翻訳ロックステップ)
- [x] 2.2 `ksdialogs-android` en / ja (同 6 本) (→ 同上)
- [x] 2.3 `ksdialogs-maui` en / ja (同 6 本 + di-registration.md) (→ 同上)
- [x] 2.4 `ksdialogs-kmp` en / ja (同 6 本 + android-host.md + ios-host.md。ホスト側 2 本に Dialog / Loading / Toast の登録を置き、機能別 6 本には置かない。最小コードの `@Throws`・iOS ホスト側 Setup の前提 1 + 手順 3 を含む) (→ 同上 + KMP Skill の共有コード側と iOS ホスト側)
- [x] 2.5 `ksdialogs-aiforms-migration` en / ja (SKILL.md + api-mapping.md。移植元のローカル clone を `reference-repositories.md` で解決して旧公開 API を一度だけ書き起こす。Toast 節 2 行) (→ Requirement: 移行 Skill の対応表)
- [x] 2.6 bootstrap manifest (`version` 3・`concepts` 空・`targets` 空・`excluded` に reference-repositories・`readmes` 4 枚) を `skills/.manifest.json` に書き、ワーカー報告の源泉マップを `DOCS_REFRESH_DECISIONS` の `addTargets` として `planned-manifest.py` で重ねて草案を作り、`concepts-coverage-check.py` で網羅を確認する (→ Requirement: manifest 初期版)

## 3. 索引・スクリーンショット・ルート README

- [x] 3.1 `skills/README.md` + `skills/README_ja.md` (3 要素のみ、Skill 一覧 5 行) (→ Requirement: 索引 README)
- [x] 3.2 Sample のデモ駆動モードで iOS シミュレータ (`basic-dialog` / `default-loading` / `toast-stack` 等) と Android エミュレータから候補を撮り `ui/references/` に置く。オーナーに提示して採用を選び `ui/brief.md` の承認欄に記録し、採用 6 枚を `assets/{ios,android}-{dialog,loading,toast}.png` へ配置する (→ Requirement: スクリーンショットの提示)
- [x] 3.3 英語 `README.md` を新規作成する (節構成・対応プラットフォーム表 (3d 取得元から実読。Kotlin 最小版は値を書かず「確定前」)・インストール 4 形態・最小コード例 4 形態を各 SKILL.md から byte コピー・Skills 導線・リポジトリ構成表・貢献・ライセンス。冒頭に配信準備中 1 行、API 安定性は常設。画像は `assets/` の絶対 URL でブランチ `main`) (→ Requirement: ルート README の節構成 / 対応プラットフォーム表 / インストールと最小コード例 / スクリーンショットの提示 / 配信準備中の状態表記 / 貢献方針の表明)
- [x] 3.4 日本語 `README_ja.md` を同一構成で作成する (→ Requirement: 英日 README の翻訳ロックステップ、および 3.3 と同じ Requirement 群)

## 4. `.github/` 一式

- [x] 4.1 `bug_report.yml` / `feature_request.yml` / `question.yml` / `config.yml` を翻案元から写し、製品名と Platform 7 択を差し替える (→ Requirement: Issue テンプレートの必須項目)
- [x] 4.2 `CONTRIBUTING.md` / `CONTRIBUTING_ja.md` を翻案元から写し、製品名と 4 形態の表現を差し替え相互リンクする (→ Requirement: 貢献方針の表明)

## 5. 機械検査 (レビュー 1 層目)

- [x] 5.1 docs-refresh の検査 8 本を草案 manifest で実行する (6-①〜6-⑧: concepts 網羅 / 節構成一致 / コードブロック byte 一致 (README 言語ペア含む) / frontmatter / 閉世界性・機械面 / 内部リンク / identity-lint / 表記ゆれ)。あわせて 3d (対応プラットフォーム表と各 SKILL.md の Setup) と 3e (API 名網羅、報告のみ) を実行し、追加静的検査として 10 部の frontmatter に `license` があり `metadata.source` が本リポジトリの URL であることを確認する (→ Requirement: レビュー 4 層 / frontmatter の標準準拠 / 閉世界性 / 翻訳ロックステップ / manifest 初期版 / 対応プラットフォーム表 / 英日 README の翻訳ロックステップ)
- [x] 5.2 README ↔ Skill の最小コード例 4 組と座標の一致を検査する (コードブロック抽出の diff、`grep` で座標) (→ Requirement: インストールと最小コード例)
- [x] 5.3 Issue Forms の静的検査 (YAML parse・必須項目・7 択の同一性・`blank_issues_enabled`) (→ Requirement: Issue テンプレートの必須項目)
- [x] 5.4 `scripts/local-path-lint.py` / `identity-lint.py` / `doc-structure-lint.py` をリポジトリ全体で通す (→ Requirement: 閉世界性 / 開発者向け知識の所在)

## 6. レビュー 2〜4 層目

- [x] 6.1 独立レビュー (ksn-review): Skill 単位 5 本 + README 群・`.github`・長命層で 1 本。源泉 concepts との内容整合 (targets の各 concept を 1 件ずつ)・ファイル単位の源泉完全性・構成規約・en / ja 等価性・ja 版 description の英語キーワード (→ Requirement: レビュー 4 層 / frontmatter の標準準拠)
- [x] 6.2 初見レビュー: concepts もコードも読んでいない新鮮なエージェントに Skill 本文だけを渡す (5 Skill、ja 版) (→ Requirement: レビュー 4 層)
- [x] 6.3 3e の報告に対するオーナー判断で `user-skill-api-listing.md` の除外リストを初期化し、掲載する分は Skill を修正する (→ Requirement: 利用者向け Skill の API 掲載基準)
- [x] 6.4 指摘の反映と機械検査 (5.1〜5.4) の再実行。修正が入った Skill は独立レビューを再実施して最終判定 APPROVED を得る。SKILL.md の節が増減した Skill は初見レビューも再実施する (→ Requirement: レビュー 4 層)
- [x] 6.5 オーナー目視検収 (少なくとも ja 5 部) — 指摘は反映または簡易起票で処理 (→ Requirement: レビュー 4 層)

レビュー最終状態: iOS `review-009.md`、Android `review-013.md`、MAUI `review-015.md`、AiForms migration `review-020.md`、cross/docs `review-023.md` は APPROVED。クロスモデル最終レビューは `second-opinion-code-001.md` に保存し、文書指摘は反映または簡易起票済み。KMP の `review-021.md` が CHANGES_REQUESTED だった唯一の理由である `iosMain` の metadata compile 不具合は、別 change `fix-kmp-iosmain-throws-metadata` で修正・取り込み済み (アーカイブ済み)。その蒸留で源泉 concept `kmp/api/ios-host-integration.md` に増えた注意点を `skills/{en,ja}/ksdialogs-kmp/references/ios-host.md` の `@Throws` 節へ反映し、`skills/.manifest.json` の該当 concept ハッシュ・`generatedAt`・`lastUpdatedFiles` を現状へ更新した (SKILL.md は変更なし = 節の増減なし。初見レビューの再実施は不要)。機械検査 5.1〜5.4 は再実行して全件合格。KMP Skill に修正が入ったため KMP の独立レビューを再実施し、`review-024.md` で APPROVED (metadata compile 2 系統と `allTests` 96 件の成功をレビュアーが実測)。全 Skill が APPROVED となり 6.4 完了。L 級の `ksn-verify` は `verify-001.md` で VALID (欠落 0、deviation 記録済み 3 Scenario)。 `review-024.md` の Minor 1 (注意点の適用範囲に `KsDialogs` が含まれない、正本は源泉 concept 側) はオーナー判断で `generalize-kmp-iosmain-throws-note` に簡易起票し、本 change では反映しない。Minor 2 (deviation.md の 9.1 項の旧状態) は反映済み。

## 7. `samples/` README の廃止と参照付け替え (検収通過後)

- [x] 7.1 `samples/README.md` と `samples/{ios,android,maui,kmp}/README.md` を `trash` で削除する (→ Requirement: README の所在)
- [x] 7.2 cross/ADR-0010 の Decision 節の「正は samples/README.md」を handbook の節へ書き換える (本文修正、オーナー許可済み)。`kasane/config.yaml` `ui.screenshot` のポインタ行を差し替える。最終レビューで判明した cross/ADR-0006・0007 は accepted 本文・過去記録を保持し、2026-09-05 の現行照合 footer で廃止後の handbook を示す (→ Requirement: 開発者向け知識の所在)
- [x] 7.3 `.agents/skills/docs-refresh/SKILL.md` を更新する: 3e 暫定注記を handbook 規約への参照に差し替え、追従対象の表の移行 Skill 行を「message 入口は移行案内あり・View 登録は対応先なし」に書き換え、`samples/` 配下 README への言及を現状 (廃止済み) に合わせる (→ Requirement: API 掲載基準の参照 / 追従対象の README 群 / 追従対象の規範)
- [x] 7.4 残存検査: 公開ドキュメント面の `README*.md` が 5 枚であること。承認済み spec の全文検索 Scenario は履歴記録を含むため達成不能であり、deviation に従って `kasane/changes/`、`kasane/roadmaps/`、`kasane/concepts/log.md`、`kasane/lessons/inbox/`、accepted ADR-0006・0007 の既存本文・過去 footer を履歴として除外し、active なリンク・文字列言及が 0 件であることを検査する (未解決リンクも失敗) (→ Requirement: README の所在)

## 8. manifest 確定と最終検査

- [x] 8.1 `skills/.manifest.json` の最終書き出し (concept ハッシュをグループ 7 完了後の状態で再計算、11 本、`readmes` 4 枚、スキーマ検証) (→ Requirement: manifest 初期版 / 源泉 concept の追加)
- [x] 8.2 機械検査一式 (5.1〜5.4) の最終実行 (→ Requirement: レビュー 4 層)
- [x] 8.3 docs-refresh を `--readme-only` で起動し、停止せず 4 枚を対象に差分なしで完了することを確認する (→ Requirement: 追従対象の README 群)

## 9. 完了判定

- [x] 9.1 テスト実行規約の全ビルドルート全件実行は、本 change が製品コード・テストに触れないため合意済み例外 (proposal Impact) として省略し、その旨を deviation.md に記録する
- [x] 9.2 phase-2 agenda の決定事項 8 節 + 踏襲 5 件を 1 件ずつ成果物と照合し、反映先を列挙する

### 9.2 phase-2 agenda 照合結果

| 区分 | 決定 | 反映先 |
|---|---|---|
| 踏襲 1 | README はルート英日 2 枚、platform / Sample README は利用者の入口にしない | `README.md`、`README_ja.md`、`skills/README.md`、`skills/README_ja.md`。`samples/` 配下 5 README は廃止 |
| 踏襲 2 | README はインストール座標だけ、手順は Skills、最小コードは逐語一致 | ルート README 2 枚の Installation / Minimal examples、`skills/{en,ja}/ksdialogs-{ios,android,maui,kmp}/SKILL.md`。task 5.2 / 8.2 で 4 組一致 |
| 踏襲 3 | 初回リリース前の状態表記は README 冒頭 1 行 | `README.md`、`README_ja.md` の先頭 |
| 踏襲 4 | 外部貢献は Issue、PR は collaborators only、Issue Forms は英語、CONTRIBUTING は英日 | ルート README 2 枚の Contributing、`.github/CONTRIBUTING.md`、`.github/CONTRIBUTING_ja.md`、`.github/ISSUE_TEMPLATE/` 4 ファイル |
| 踏襲 5 | Skill 単位 fan-out と 4 層レビュー | `skills/{en,ja}/` 5 Skill、`review-*.md`、`novice-review-001.md`、task 6.5 のオーナー承認 |
| 固有 1 | 既定 singleton と DI 注入の使い分け | 5 Skill の `SKILL.md` 冒頭。具体 DI は `ksdialogs-maui/references/di-registration.md` と `ksdialogs-kmp/SKILL.md` |
| 固有 2 | 対応プラットフォーム表は最小 OS + build toolchain の 3 列 | ルート README 2 枚の Supported platforms と注記 |
| 固有 3 | iOS / Android × Dialog / Loading / Toast の 6 枚 | `assets/` 6 枚、ルート README 2 枚の Screenshots、`ui/brief.md` の採用記録 |
| 固有 4 | 移行 Skill の Toast は message 代替と View 登録の対応先なし | `skills/{en,ja}/ksdialogs-aiforms-migration/references/api-mapping.md`。原典差は `deviation.md` に記録 |
| 固有 5 | KMP の `@Throws` は最小コード + 本文 3 行、詳細は reference | `skills/{en,ja}/ksdialogs-kmp/SKILL.md` と `references/ios-host.md` |
| 固有 6 | KMP iOS Setup は前提 1 + 手順 3、源泉 concept を持つ | `kasane/concepts/kmp/api/ios-host-integration.md`、`skills/{en,ja}/ksdialogs-kmp/SKILL.md` と `references/ios-host.md` |
| 固有 7 | `samples/` README 5 本を廃止し、正を handbook / concepts へ移送 | `kasane/handbook/cross/sample-parity.md`、`local-development-setup.md`、`kasane/concepts/kmp/api/ios-host-integration.md`、cross/ADR-0010、`kasane/config.yaml`。旧 5 本は `trash` で廃止 |
| 固有 8 | Issue Forms の Platform は形態 × host OS の 7 択 | `.github/ISSUE_TEMPLATE/bug_report.yml` と `question.yml`。提案 form と config を含む静的検査は task 5.3 / 8.2 で合格 |

Reduce Motion は「取り扱わず利用者向け文書にも記載しない」という非成果物の決定である。`README*.md` と `skills/**` に記載がないことを照合した。
