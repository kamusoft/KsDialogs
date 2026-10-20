# Exploration: docs-refresh-scribe-unfilled-report

## 課題 / 動機

docs-refresh のサブエージェントの器を `ksn-implementer` から `ksn-scribe` に差し替えた change `switch-docs-refresh-worker-to-scribe` の独立レビュー (`kasane/changes/switch-docs-refresh-worker-to-scribe/review-001.md`、Suggestion) で見つかった空白。オーナー判断で簡易起票。

- 器 `ksn-scribe` の定義 (3 項) は「テンプレート・書式・既存資料から埋められない箇所は自分で決めずに未記入のまま残し、その箇所と理由を報告に列挙する」と定める。`ksn-implementer` にはこの規定が無かった
- docs-refresh 側には未記入を受け取る経路が無い:
  - `.agents/skills/docs-refresh/references/prompt-skill.md:72` / `references/prompt-readme.md:69` の報告項目は「変更要約と drift 所見」だけ
  - `.agents/skills/docs-refresh/SKILL.md` Step 5 はバッチが返れば進捗リストを完了にマークする
  - 同 Step 8 (完了サマリ) にも未記入を挙げる欄が無い
- そのため scribe が一部を未記入で返しても、メインが拾えず完了扱いにする余地がある。Step 4 の承認は書き込み前の方針の承認なので、ここでは捕まらない
- 姉妹リポジトリ KsSettingsView の docs-refresh にも同じ空白がある (対の起票: `../KsSettingsView/kasane/changes/docs-refresh-scribe-unfilled-report/`)

## 検討した選択肢 (却下案と理由を含む)

## 決定事項

## ADR 候補 (作成済み: なし / 未起票: なし)

## 未決の論点

**未探索 (簡易起票)**。分かっている疑問点:

- テンプレートの報告項目に「未記入の箇所と理由」を足すか
- 未記入があったバッチを Step 5 で完了にせず保留として扱うか、メインからの再委譲・ユーザーへの確認のどちらで埋めるか
- Step 8 の完了サマリに未記入の一覧を載せるか
- manifest のハッシュ更新 (Step 7) を、未記入が残る Skill についても行うか (行うと次回の実行で要追従に上がらない)
- KsSettingsView 側と同じ形で揃えるか

## UI 素材 (ui/references/ の一覧と注釈)

## 変更級の推奨: 未判定 (暫定 S — docs-refresh のテンプレートと SKILL.md の手順の追記で収まる見込み)
