# Proposal: adopt-docs-refresh

## Why

利用者向けドキュメントを Agent Skills (`skills/{en,ja}/<name>/`) として提供し、`kasane/concepts/` とコード・テストへ追従させる道具 docs-refresh を持つ形を、姉妹ライブラリ KsSettingsView から踏襲することがロードマップ package-distribution で決まっている (phase-1 agenda の踏襲決定。翻案元は `../KsSettingsView/kasane/decisions/cross/0022-user-docs-as-agent-skills.md` と `../KsSettingsView/kasane/changes/archive/2026-08-26-retarget-docs-refresh-to-skills/`)。KsDialogs には `.agents/` も `skills/` も docs-refresh の登録もまだ無く、Skill 本文の初期生成 (phase-2) を始める前に、追従の道具と規約記述を KsDialogs の concepts・ビルドルート・識別子に向けて据えておく必要がある。

KsSettingsView の docs-refresh は、検査スクリプト 8 本が `kasane/concepts` / `skills/{en,ja}` / `skills/.manifest.json` の構造規約だけに依存し、プロジェクト固有値 (Skill 名・ビルドファイルのパス・識別子・廃止 API) は SKILL.md と prompt 2 本に集中している。したがって本 change は「一式のコピー + SKILL.md / prompt の固有値差し替え + 規約記述の更新」で完結する。

## What Changes

- **docs-refresh 一式の配置**: `../KsSettingsView/.agents/skills/docs-refresh/` (SKILL.md + `references/prompt-skill.md` / `prompt-readme.md` + `scripts/` 8 本。翻案元は KsSettingsView の commit `33ce94e` (2026-09-02) 時点のスナップショットに固定する) を `.agents/skills/docs-refresh/` へコピーし、`.claude/skills/docs-refresh` を symlink で結ぶ。scripts/ は無改変 (固有値を持たない)
- **SKILL.md / prompt の KsDialogs 向け差し替え** (phase-1 agenda の決定事項 5 件を反映):
  - 追従対象を **5 Skill × 2 言語** (`ksdialogs-{ios,android,maui,kmp,aiforms-migration}`) + README 4 枚 (`skills/README.md` / `README_ja.md`、ルート `README.md` / `README_ja.md`) に定義する。KMP Skill は 1 本で共有コード側・Android ホスト側・iOS ホスト側を扱い、ホスト側のレシピは `references/` で振り分ける。移行 Skill は独立 (`SKILL.md` + `references/api-mapping.md`)
  - 移行 Skill の源泉は新 API 側の concepts のみ。旧 API 側 (AiForms.Maui.Dialogs) は追従対象外であることを明記する
  - `excluded` の初期値 (`cross/reference/reference-repositories.md`) と、`architecture/` カテゴリの concept を既定で除外候補として扱う規則 (3c 発火時に理由つきで確定、自動除外はしない) を 3c 節に書く
  - コード正の機械チェック 3d の取得元を KsDialogs の 4 ビルドルートに合わせた 4 行 (version catalog / Gradle wrapper 2 本 / `ios/Package.swift` / MAUI csproj の TFM・OS 下限・MAUI 本体下限) に置き換え、KMP ルートが android の version catalog を共有している前提 (`kmp/settings.gradle.kts`) の確認行を添える
  - 整合性チェック 6-⑤ (旧名残 grep) を KsDialogs の閉世界性検査に置き換える — リポジトリ内部用語 (`kasane/`・ADR 番号)、利用者向けでない機械面 (`KsDialogsInteropBridge` / `KsDialogsInteropResultType`。どちらも現存する public ABI だが KMP cinterop 委譲専用で、kmp/ADR-0004 により利用者向け導線から退いている) の漏れ、Skill ルートの外へ出る相対リンク (skills/ 配下のみ適用) を検出する。配布座標の URL (SwiftPM package URL・`metadata.source`) は識別子であって参照ではないため機械検査の対象にせず、文書としての外部参照の禁止は生成プロンプトの規約とレビューで担保する。6-⑧ (表記ゆれ grep) は cross/ADR-0005 (公開識別子の写像表) と android/ADR-0001 (`ksdialogs-compose`) の 2 本を正典として導いたパターンに置き換える。KsSettingsView の `docs/` / `openspec` 参照検査は KsDialogs に該当ディレクトリがないため持ち込まない
  - 3e の仕分け基準の参照先 (KsSettingsView の handbook `user-skill-api-listing.md`) は KsDialogs に無いため、phase-2 で起こす旨の注記に置き換える
  - ADR リンク (cross/ADR-0022) は KsDialogs に翻案 ADR が未起票のため、ロードマップ phase-1 の決定事項への参照 (archive 後の解決規則つき) に置き換える。蒸留時に ADR を起票したらリンクを差し替える
  - prompt 2 本の固有名 (プロジェクト名・変更理由の例・規約⑤の `docs/` / openspec) を KsDialogs 向け (閉世界性: `kasane/` 内部文書・ADR 番号・機械面への参照を書かない) に置き換える
- **規約記述の更新**: AGENTS.md (CLAUDE.md はその写し) に Kasane 運用宣言として「`skills/` は利用者向け派生物で知識参照先にしない」「追従更新は docs-refresh 経由のみ・自動発動禁止・初期生成は承認済み change」の 2 行を足す (ツール手順は書かない)。`kasane/config.yaml` の `context` に同趣旨を 1 文足し、`lint.identity.scope` に `skills` を追加、`lint.comment-policy.exclude` の `docs` (KsDialogs に存在しない) を `skills` に置き換える

影響する能力: docs-refresh (利用者向けドキュメントの追従更新) のみ。

## Non-Goals

- **`skills/` 一式と `skills/.manifest.json` 初期版の生成、`skills/README.md` / `README_ja.md` の執筆**: 初期生成は配置判断・レシピ設計という創作を含み、phase-2 (docs-rollout) の変更フローが承認つきで担う。本 change 完了から phase-2 完了までの間、docs-refresh は manifest 不在で停止案内のみを返す (合意済みのギャップ、KsSettingsView と同じ)
- **3e の仕分け基準 (利用者向け Skill の API 掲載基準) の handbook 起こし**: 掲載除外リストは Skill 本文と一緒に決まる規約であり、phase-2 で KsDialogs 固有の内容として起こす (規約の新設は実装タスクに入れず変更提案の中で諮る — ksn-propose の規律)
- **翻案 ADR (KsSettingsView cross/ADR-0022 相当) の起票**: roadmap.md の前提どおり蒸留時に判断する。ADR 候補として下記 Impact に申し送る
- **ルート README への Skills 導線・インストール手順**: phase-2 の責務
- **Kasane ハーネス側スキルの変更**: ロードマップの非ゴール

## Impact

- 製品コード・公開 API への影響なし。変更対象は `.agents/` 配下のスキル文書・スクリプト、`.claude/skills/` の symlink、AGENTS.md / CLAUDE.md / `kasane/config.yaml` の規約記述のみ
- `lint.identity.scope` に `skills` を足しても、`skills/` は phase-2 まで存在しないため現時点の lint 結果は変わらない
- テスト実行規約 (`kasane/handbook/cross/test-execution.md`) は完了判定に全ビルドルートの全件実行を求めるが、本 change は製品コード・テストに触れない (変更対象はスキル文書・スクリプト・規約記述のみ)。全件実行の省略を**合意済み例外**として扱う (オーナー確認済み 2026-09-04。相方レビュー second-opinion-spec-001 の指摘 6)
- ADR 候補 (蒸留時): 「利用者向けドキュメントは Agent Skills として提供し docs-refresh で追従する」を KsSettingsView cross/ADR-0022 の翻案として起票し、KsDialogs 固有の決定 (KMP Skill 1 本・移行 Skill 独立・移行 Skill の源泉は新 API 側のみ・excluded の既定) を含める。出典は phase-1 agenda / history

## 級: M

1 能力 (docs-refresh) 内で製品コード無変更だが、一式のコピーに加えて KsDialogs 固有の差し替え (追従対象・3d・6-⑤・6-⑧) と規約記述の更新を含み、検証 (スクリプトの実走・取得元の実読) が要るため S ではない。

domain: cross
roadmap: package-distribution/phase-1-skills-foundation
