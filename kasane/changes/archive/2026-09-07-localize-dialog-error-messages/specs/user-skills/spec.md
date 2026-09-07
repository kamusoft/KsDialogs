# user-skills デルタ (localize-dialog-error-messages)

利用者向け Agent Skills の「構成ミスの失敗」診断表 (en / ja × iOS / Android / MAUI / KMP × dialogs / loading / toast = 24 ファイル、各言語 12) のメッセージ列を、各形態のデルタの対応表にある英語文言へ置き換える。列構成・節の並び・コードブロックは変えない。本デルタの Scenario は挙動系ではないため安定 ID を持たず (core/ADR-0016 の対象外)、受け入れは ksn-verify の対応表と検査 script で行う。`skills/.manifest.json` は触らない (蒸留完了後の docs-refresh が書く)。

## ADDED Requirements

### Requirement: Skills の診断表は実装の英語文言を引用する

`skills/{en,ja}/ksdialogs-{ios,android,maui,kmp}/references/{dialogs,loading,toast}.md` の診断表のメッセージ列は、対応する形態のデルタスペックの対応表の英語文言と一致する (SHALL)。型名の埋め込みは en / ja とも `{TypeName}` のプレースホルダで書き (現行の `{型名}` を置き換える)、メッセージ列のセルは両言語で byte 一致させる。診断表の周辺で個別のメッセージを本文に引用している箇所 (KMP の dialogs.md の iOS 補足など) も同じ英語文言にする。診断表の直後に「メッセージは現在の実装値で、安定 API ではない (契約は case / 例外型と throw 条件)」の 1 文を en / ja で添える。

#### Scenario: 診断表のメッセージ列が対応表の英語文言と一致する
- **GIVEN** 24 ファイルの診断表の各行
- **WHEN** メッセージ列を、その行の例外 (case) に対応するデルタスペックの英語文言と突き合わせる
- **THEN** `{TypeName}` プレースホルダを除いて完全一致し、en と ja で同じ行のメッセージ列が byte 一致する

#### Scenario: Skills に日本語の例外メッセージの引用が残らない
- **GIVEN** `skills/` 配下の全ファイル
- **WHEN** 各形態のデルタの対応表にある現行 (ja) 文言を検索する
- **THEN** 該当は 0 件

#### Scenario: en / ja の parity 検査と lint を通る
- **GIVEN** 更新後の `skills/`
- **WHEN** docs-refresh の parity 検査 (`code-block-parity-check.py` / `heading-parity-check.py` / `link-resolution-check.py` / `frontmatter-check.py`) と `scripts/local-path-lint.py` / `scripts/identity-lint.py` を走らせる
- **THEN** すべて差分ゼロ・検出ゼロで終わる
