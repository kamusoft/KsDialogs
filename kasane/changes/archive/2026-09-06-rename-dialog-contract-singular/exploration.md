# Exploration: rename-dialog-contract-singular

## 課題 / 動機

Dialog の契約型名だけが複数形 (`KsDialogs` / MAUI `IKsDialogs`) で、Loading (`KsLoading`) / Toast (`KsToast`) の「Ks + 機能名 (単数)」と非対称になっている。

経緯の調査結果 (2026-09-06):
- 08-13 core/ADR-0002 は契約を「`IDialog` 相当」とだけ書き、具体名は未定
- 08-14 21:08 phase-3 の core concepts 初版でも契約名は未記載
- 08-14 22:53 phase-4 (縦串) 議論の kmp/ADR-0002 本文に `interface KsDialogs` が既定事実として初出。命名の選択肢・理由の記述なし
- 08-15 縦串の1コミット (768f0d4) で 4 形態の契約 (`protocol KsDialogs` / `interface KsDialogs` / `IKsDialogs`) が同時に実装
- 別名 (`IDialog` / `DialogProtocol` 等) が使われた形跡は git に無し。08-14 の議論セッションはセッション記録に未収載
- 推定: 既定エントリ class に原典名 `Dialog` を先に割り当てたため、契約にモジュール名 (製品名) を流用した
- オーナー確認: エージェント主導で見逃したもので、意図した複数形ではない。非対称は直す

## 検討した選択肢 (却下案と理由を含む)

- **A. Dialog 側を `KsDialog` / `IKsDialog` に単数化 (採用)** — Loading / Toast の先例 2 件に揃う。既存の単数系識別子は Compose の `KsDialogAttributes` のみで衝突なし
- **B. Loading / Toast を複数形にする** — 却下。英語として不自然
- **C. 現状維持で経緯だけ記録** — 却下。一般公開後に直すコストが上がる

## 決定事項

- 契約型名の規則: 全形態で「Ks + 機能名 (単数)」。MAUI は `I` 接頭辞
- `KsDialogs` → `KsDialog`、`IKsDialogs` → `IKsDialog`。旧名は残さない
- 製品名としての `KsDialogs` (モジュール・パッケージ・namespace・NuGet ID、cross/ADR-0005) は不変
- docs-refresh の禁止トークン lint (`.agents/skills/docs-refresh/SKILL.md` の `KsDialog([^sA-Za-z0-9_]|$)`) は前提が逆転するため改修する
- **skills/ (en/ja) は今回に限り docs-refresh を通さず直接修正する** (オーナー指示 2026-09-06)。ただし進行中の proofread-user-skills-ja が skills/ に未コミット変更を持つため、そのコミット後に重ねる
- ADR (記録) の過去本文は触らず、core/ADR-0002 の現行照合に改名を追記する

## ADR 候補

- 作成済み: core/ADR-0034 (proposed) — 契約の型名は「Ks + 機能名 (単数)」で揃え、Dialog の契約を改名する

## 未決の論点

- MAUI 内部の関連名 (例: `KsDialogsMauiGateway` 等、契約名を含む internal 型) をどこまで追随させるか — propose で棚卸し
- 影響ファイルの概数 (型名参照): iOS 少数 / Android 28 行 / KMP 66 行 / MAUI 最多 / samples 20 行、concepts 17 行 / handbook 12 行 / README 6 行 / skills 116 行 (2026-09-06 概算、propose で精査)

## UI 素材

なし (UI 変更なし)

## 変更級の推奨: M

公開 API の破壊的改名が 4 形態 + lint + docs 一式に跨る。挙動変更なし・UI なしのため S でも成立するが、迷ったら 1 段上で M (オーナー確定 2026-09-06)。
