# Live Summary: switch-docs-refresh-worker-to-scribe

## 最終状態 (何がどうなったか)
docs-refresh スキルがサブエージェントを起動するときに指定する器を `ksn-implementer` から `ksn-scribe` に差し替えた。姉妹リポジトリ KsSettingsView の `.agents/skills/docs-refresh/` commit `458aa23` と同じ差し替えで、器名以外の文言は変えていない。

## 採用値と根拠 (却下試行の要点)
- 採用値: 器名 `ksn-scribe` (器定義ファイルのパスは `~/.claude/agents/ksn-scribe.md` / `.claude/agents/ksn-scribe.md` / `~/.codex/agents/ksn-scribe.toml`)
- 根拠: オーナーの指示 (KsSettingsView の docs-refresh と同じ器にする)。試行は 1 回で採用、却下試行なし

## 触ったファイル
- `.agents/skills/docs-refresh/SKILL.md` (53, 67, 72, 269, 280, 561 行の器名)
- `.agents/skills/docs-refresh/references/prompt-skill.md` (3 行目)
- `.agents/skills/docs-refresh/references/prompt-readme.md` (3 行目)

## 決定事項 / ADR 候補
- 決定: docs-refresh のサブエージェントの器は `ksn-scribe`
- ADR 候補: なし (スキル内に器名を直書きで固定する既存方針の範囲内の値の差し替え)
