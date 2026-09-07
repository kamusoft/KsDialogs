# user-skills-manifest (利用者向け Skills の源泉と manifest)

## MODIFIED Requirements

### Requirement: manifest 初期版

`skills/.manifest.json` を docs-refresh SKILL.md の「manifest v3 の構造 (規範)」に従って書き出す SHALL。manifest が扱う concept の集合は `kasane/concepts/` 配下の `*.md` から `index.md` / `log.md` / `rules.md` を除いたもの (本 change 完了時点で 30 本: core/api 8 + core/architecture 1 + ios/api 5 + android/api 5 + maui/api 6 + kmp/api 4 + cross/reference 1) とする SHALL。`concepts` のキー集合はこの集合と過不足なく一致し、各値は本 change の全タスク完了時点のファイル内容の SHA-256 と一致する SHALL。`targets` は design.md Decision 6 の完成形の表 (33 キー) と一致する SHALL。platform Skill の `references/` 各ファイルは対応する core 契約と自 platform の同名 `*-surface.md` を源泉とし、KMP の `layout.md` / `transitions.md` は `kmp/api/dialog-surface.md` と `ios/api/` `android/api/` の同名 surface を、`view-models.md` は現行の core 4 本と `kmp/api/` の 3 surface を、`android-host.md` は `android/api/` 5 本を、`ios-host.md` は `kmp/api/ios-host-integration.md` を源泉に含める SHALL。生成された全 Skill ファイルが言語抜き相対パスでちょうど 1 キーとして存在する SHALL。`excluded` は `cross/reference/reference-repositories.md` と `core/architecture/layout-case-table.md` の 2 本 (理由文字列つき) とし、他の全 concept は `targets` のいずれかの値に現れる SHALL (網羅不変条件)。`ksdialogs-aiforms-migration` 配下のファイルの源泉は新 API 側の concepts のうち対応表が触れるもの (core/api + `maui/api/` 6 本) のみとする SHALL (cross/ADR-0011 の基準の適用)。`readmes` は `skills/README.md` / `skills/README_ja.md` / `README.md` / `README_ja.md` の 4 枚とする SHALL。

#### Scenario: 網羅不変条件の検査

- **GIVEN** 書き出された manifest と `kasane/concepts/` の全 concept ファイル
- **WHEN** 網羅検査 (docs-refresh 6-①) を実行する
- **THEN** 未参照かつ未除外の concept が 0 件

#### Scenario: スキーマ準拠とハッシュの最終状態一致

- **GIVEN** 全タスク完了後の作業ツリーと書き出された manifest
- **WHEN** 必須キー・不変条件を検証し、concept 集合の SHA-256 を再計算して `concepts` と突き合わせる
- **THEN** すべて満たされ、キー集合・ハッシュ値ともに過不足なく一致する

#### Scenario: 源泉が他 platform の公開面を含まない

- **GIVEN** 書き出された manifest の `targets`
- **WHEN** 33 キーの源泉を design.md Decision 6 の表と突き合わせる
- **THEN** 全キーが表と一致し、ios / android / maui の Skill に他 platform の `api/` パスが含まれず、kmp は `android-host.md` / `layout.md` / `transitions.md` に限って `android/api/` `ios/api/` の surface を含む

## ADDED Requirements

### Requirement: 源泉の再構成に伴う再生成

manifest の書き換え後、docs-refresh を `--all` で起動して 5 Skill × en/ja と README 4 枚を再生成する SHALL。Skill の本数・各 Skill の references の構成 (ファイル名と本数) を再生成で変更することを禁止する (SHALL NOT)。再生成後は docs-refresh の整合性チェック (concept coverage / heading parity / code-block parity / frontmatter / internal links / 閉世界性 / 識別子表記ゆれ / ツール最低バージョン) と identity / local-path lint をすべて通過する SHALL。

#### Scenario: 構成の不変

- **GIVEN** 再生成前の `skills/{en,ja}/` のファイル一覧と再生成後の一覧
- **WHEN** 言語抜き相対パスの集合を比較する
- **THEN** 過不足なく一致する (33 ファイル × 2 言語)

#### Scenario: 整合性チェックの通過

- **GIVEN** 再生成後の `skills/` と README 4 枚
- **WHEN** docs-refresh Step 6 の検査一式と lint を実行する
- **THEN** すべて成功する

### Requirement: API 名網羅検査の候補から他 platform 名が消える

`verification/forbidden-tokens.json` に Skill 範囲ごと (ios / android / maui / kmp / aiforms-migration) の禁止トークン集合を固定する SHALL。初期値は分割前の除外リストで基準が「対象 Skill 外・機械検査由来」の行の名前をその platform 列ごとに拾ったものとし、新設した他 platform の surface concept の識別子のうち自 platform の公開面に無いものを加える SHALL。再生成後の Skill (en / ja) と API 名網羅検査の候補のどちらにも、当該 Skill 範囲の禁止トークンが出現しない SHALL。検査の候補は仕分け後に未分類が 0 件である SHALL。

#### Scenario: Skill 本文への負の検査

- **GIVEN** 再生成後の `skills/{en,ja}/ksdialogs-<p>/**` と禁止トークン集合
- **WHEN** 各 Skill 範囲のファイル全文を当該集合と突き合わせる
- **THEN** 一致が 0 件 (5 Skill 範囲すべて)

#### Scenario: 検査候補への負の検査

- **GIVEN** 再生成後の manifest と `skills/ja/`
- **WHEN** `api-coverage-check.py` を実行し、各 Skill 範囲の候補トークンを禁止集合と突き合わせる
- **THEN** 一致が 0 件

#### Scenario: 仕分けの完了

- **GIVEN** 同上の候補
- **WHEN** 各候補を「掲載漏れ (Skill を修正)」「実判断の除外 (旧リストの理由を引き継ぐ)」「非 API token」に仕分ける
- **THEN** 未分類が 0 件で、掲載漏れは Skill 修正後の再実行で候補から消えている
