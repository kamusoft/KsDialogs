# Exploration: harden-docs-refresh-targets-read

## 課題 / 動機

姉妹リポジトリ KsSettingsView からの知らせ (`../KsSettingsView/kasane/outbox/KsDialogs/2026-09-16-link-resolution-targets-path-collision.md`、kind: bug)。

- 向こうの docs-refresh で、内部リンク解決検査が既定パス `/tmp/docs-refresh-targets.txt` に残っていた KsDialogs 側の対象一覧を読み、KsDialogs の Skill 66 件を `MISSING` と並べた (偽陽性)。KsDialogs 側が採っている対策 (`DOCS_REFRESH_TARGETS` 対応 + リポジトリ別のファイル名) は正しかった、という報告
- 向こうが入れて、こちらに無いのは 1 点だけ: 一覧ファイルが読めないときに理由と対処を標準エラーへ出して exit 1 で終わるガード
- こちらの `.agents/skills/docs-refresh/scripts/link-resolution-check.py:22` は素の `open(targets_path)` のため、`DOCS_REFRESH_TARGETS` に誤ったパスを渡すとトレースバックで落ちる。黙った誤検査にはならず、実害は小さい

発見の文脈: 2026-09-26 の ksn-drift クイック (relations の未処理の知らせ) をオーナー判断で簡易起票。

## 検討した選択肢 (却下案と理由を含む)

## 決定事項

## ADR 候補 (作成済み: なし / 未起票: なし)

## 未決の論点

**未探索 (簡易起票)**。分かっている疑問点:

- ガードを取り込むか (知らせは「取り込まない判断でも構わない」としている)
- 同じ一覧を読むほかのスクリプト (docs-refresh 6-⑤ / ⑦ / ⑧ の検査) にも同じガードを入れるか
- 既定値 `/tmp/docs-refresh-targets.txt` を残すか (向こうは後方互換のため共通の既定を残した)
- `.agents/skills/docs-refresh/` の変更は docs-refresh 自体の改修であり、skills/ と README 群の追従更新ではない。変更経路 (通常の change) で扱ってよいかの確認

## UI 素材 (ui/references/ の一覧と注釈)

なし

## 変更級の推奨: 未判定 (暫定 S — スクリプト 1〜数本の入力検査に閉じるなら)
