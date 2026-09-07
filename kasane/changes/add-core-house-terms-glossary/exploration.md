# Exploration: add-core-house-terms-glossary

## 課題 / 動機

core 契約 (`kasane/concepts/core/api/*.md`) は、platform 非依存で書くために **house 用語** — 面 (公開面)・器 (ダイアログを載せる表示コンテナ)・中身 (content View)・スロット (登録先: View factory / ViewModel factory) — を使い、各 concept の冒頭で都度説明している。split-concepts-platform-surface で core から platform 固有の識別子を外した結果、この用語への依存が強まった一方、「用語 → 各形態 (iOS Native / Android Native / MAUI / KMP の 3 側) の実体」の対応表がどこにもない。読み手 (エージェント・利用者向け Skill の生成ワーカー) は concept ごとに説明を読み直すことになり、KMP の 3 側の説明が繰り返し要る。

発見の文脈: split-concepts-platform-surface の独立レビュー (review-002 / review-003 のレビュアー所見) と、docs-refresh による Skill 再生成ワーカーの報告。

## 検討した選択肢 (却下案と理由を含む)

## 決定事項

## ADR 候補 (作成済み: なし / 未起票: なし)

なし (知識の整理であり決定ではない)。cross/ADR-0014 の「core = 契約 / platform = 公開面」の振り分けと `kasane/concepts/rules.md` の共通概念名の判定が前提。

## 未決の論点

**未探索 (簡易起票)**。分かっている疑問点:

- 置き場所: `core/architecture/` に 1 枚 (契約ではなく読み方の補助) か、`core/index.md` の冒頭節か、`concepts/rules.md` の用語節か。docs-refresh の manifest では Skill の源泉に載せない (利用者向けには翻訳して届く) 前提でよいか
- 用語の確定一覧: 面 / 器 / 中身 / スロット のほかに「添付」「供給点」「合流」「世代」(loading) など core が使う語をどこまで載せるか
- 対応表の列: iOS Native / Android Native (View / Compose) / MAUI / KMP (commonMain / Android ホスト / Swift 向け公開面) の分け方
- ksn-concept (知識直接入力モード) で行うのが自然 (変更フローではなく決まり事の整理)。その場合はこの change を ksn-concept の入力にして閉じる

## UI 素材 (ui/references/ の一覧と注釈)

なし

## 変更級の推奨: 未判定

暫定: 文書 1 枚の追加と各 concept 冒頭の説明の参照化で **S 級** (ksn-concept 経路の候補)。
