# Exploration: revisit-dialog-margin-default

## 課題 / 動機

ダイアログの余白 `dialogMargin` の既定値 (全辺 24) を 0 に見直したい (オーナー要望、2026-09-26)。

- 既定値が 0 なら、配置 (Start / End) と基準領域 (window / visibleArea / currentPage) の結果がそのまま基準の端に接して見え、利用者が挙動を理解しやすい
- 発見の文脈: add-page-layout-area の Sample 撮影証跡を見たオーナーが、カードが基準領域の端から 24 離れていて「ぱっと見検証しにくい」と指摘。同 change では Layout Dialog パネルのダイアログだけ dialogMargin を全辺 0 に指定した (`kasane/changes/add-page-layout-area/deviation.md` 最終項。archive 後は `archive/*-add-page-layout-area/`)

現状:

- 既定値は 3 面とも全辺 24: iOS `ios/Sources/KsDialogs/Contract/DialogOptions.swift` / Android `android/ksdialogs-core/src/main/kotlin/jp/kamusoft/ksdialogs/DialogOptions.kt` / MAUI `maui/KsDialogs.Maui/Contract/DialogOptions.cs`
- 決定の記録: core/ADR-0008 Decision 3「既定値の乖離3件」で、dialogMargin = 全辺 24 (原典 AiForms は 0) を「属性を何も指定しなければ今までどおりに出る」ためのオーナー判断として維持している。concepts `core/api/layout-semantics.md` の既定値表と「原典と既定値が異なる」節も同じ内容

## 検討した選択肢 (却下案と理由を含む)

## 決定事項

## ADR 候補 (作成済み: なし / 未起票: core/ADR-0008 Decision 3 の改訂 — dialogMargin の既定値を原典と同じ 0 に戻すなら、乖離 3 件のうち 1 件が解消する)

## 未決の論点

**未探索 (簡易起票)**。分かっている疑問点:

- 既定を 0 にしたとき、何も指定しないダイアログが画面の端 (可視領域の端) に接して出る。中央配置の既定では見た目の差は小さいが、fill や比率サイズ 1.0・大きな内容では端に貼り付く。利用者が余白を付けたいときの指定の手間とのバランス
- 適用範囲: Dialog だけか、同じ器メタ属性を持つ Loading (core/ADR-0022) にも及ぶか。Toast の既定配置 (core/ADR-0032 のボトムバー回避オフセット) との関係
- 非有限値の規則 (layout-semantics「NaN の辺は既定の 24 に戻り、負の辺は 0 になる」) の書き換え。既定が 0 になると NaN と負の扱いが同じ結果になる
- 共通ケース表 `core/layout-spec/cases.json` の既定値前提のケースと期待値、各 platform の既定値テスト・layout-surface の記述・skills (docs-refresh 経由) への波及
- 公開前の既定値変更として扱えるか (互換の扱い、リリースノート)。移行スキル (AiForms からの移行) では原典と同じ既定になるので対応表の注記が減る

## UI 素材 (ui/references/ の一覧と注釈)

なし

## 変更級の推奨: 未判定 (暫定 S〜M — 既定値の変更自体は小さいが、ADR 改訂・ケース表・3 面のテストと concepts への波及がある)
