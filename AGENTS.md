# KsDialogs

AiForms.Maui.Dialogs の Native + MAUI + KMP 対応リブランドプロジェクト。ダイアログをいつでも呼び出せる UI ライブラリ。

## 開発ハーネス

このプロジェクトは Kasane (ksn-*) で運用する。
- 探索: ksn-explore / 提案: ksn-propose / 実装: ksn-orchestrator / 蒸留: ksn-distill / 棚卸し: ksn-drift
- 規約: ~/.claude/skills/ksn-core/ (SKILL.md + references/)、プロジェクト設定: kasane/config.yaml
- 他の SDD 系スキル (openspec-* 等) はこのプロジェクトでは使用しない
- `skills/` は利用者向けドキュメント (Agent Skills) であり、エージェントは開発時の知識参照先にしない
- `skills/` と README 群の継続的な追従更新は docs-refresh 経由のみ (自動発動禁止)。初期生成・構成の見直しは承認済み change の実装として行う。スキル本体は `.agents/skills/docs-refresh/SKILL.md`
- 例外: インストール例の version の置換だけは release workflow が行う (docs-refresh を経ない)。手で書き換えない
