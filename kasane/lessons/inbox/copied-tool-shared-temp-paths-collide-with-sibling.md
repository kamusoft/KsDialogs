---
scope: spec-review
kind: pain
severity: normal
count: 1
first-seen: 2026-09-04
last-seen: 2026-09-04
evidence:
  - adopt-docs-refresh (デルタスペックが検査スクリプト 8 本を「翻案元と byte 一致・無改変」と契約したが、スクリプトは `/tmp/docs-refresh-*` の固定ファイル名を使っており、同じ道具を持つ姉妹リポジトリ KsSettingsView と同じマシンで動かすと予定 manifest・対象一覧を読み違える。review-002 の Suggestion をオーナーが採用し、`/tmp` パスのプロジェクト名入りと `link-resolution-check.py` の環境変数化を deviation として実装した)
---

## ルール文

姉妹プロジェクトから道具 (スクリプト・hook・スキル) を「無改変で写す」と提案・デルタスペックに書くとき、その道具がプロセスの外に持つ共有資源 — `/tmp` 等の固定ファイル名・環境変数の既定値・グローバル設定・ポート — を `grep -n -E "/tmp/|os.environ|getenv"` の最小確認で列挙し、両プロジェクトが同じマシンで動いたときに衝突しないことを確かめてから「無改変」を契約にする。衝突する資源があれば、プロジェクト名を含めた固有化を差し替え対象に含める (byte 一致の契約は衝突しない部分に限る)。

## 経緯

- 2026-09-04 adopt-docs-refresh: 翻案元の `/tmp/docs-refresh-manifest-planned.json` の残骸が実際に検証機に残っており、環境変数の付け忘れで姉妹リポジトリの状態を「適合」と読み違える経路が実在した。近縁: [[translated-norm-needs-local-basis-and-fact-check]] (翻案時の根拠・現状確認) / [[tool-premise-decision-needs-existence-probe]] (ツール前提の最小プローブ)
