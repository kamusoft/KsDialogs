---
scope: process
timestamp: 2026-09-06
---

# lessons: process

- [L-001] 同じ設計・同じ境界規則から独立に書かれた**姉妹面** (別プラットフォームの同名ミラー実装、同じ言語境界を渡る別の関数群 — suspend と非 suspend・ライブラリと Sample — など) のどこか一面で「設計の遷移表・契約の字面からは見えない穴」が見つかったら (実装ワーカーの報告でもレビュー指摘でも)、レビューの完了を待たず、他の姉妹面の同じ箇所を読み取り専用の調査で照合する。照合の問いは「同じ入力経路 (通知の本数・到着順・失敗経路) と同じ状態分岐が存在するか」「既存テストがその経路を区別できるか (即完了のダブルで素通りしないか)」の2点に絞る。「他方には既存の受け皿がある」という成立の主張も照合対象で、受け皿の型 (catch する例外型・宣言の有無) を一面ずつ実物で確認する。穴があれば小スコープの修正として委譲し、テストの検出力 (修正前に fail すること) を報告に含めさせる。([経緯](details/mirror-impl-cross-check-sibling-gap.md)) (昇格: 2026-09-02、出典: add-presentation-behavior / add-model-binding-di / add-loading / add-toast / add-kmp-loading-toast-throws)
- [L-002] 「互換」「破壊的変更なし」を成果物 (proposal・deviation.md・証跡の注記・リリースノート) に書くときは、主張の範囲を実証した範囲に限定して書く — 何で実証したか (無改変ビルド・テスト・最小再現) と、その実証が**カバーしない面** (公開 protocol / interface への外部準拠・ABI・シリアライズ形式・測っていない API レベルなど) を分けて明記する。実証していない面が壊れうるなら、互換の主張ではなく破壊の記録として書く。守れたかは、当該の主張の直後に実証手段と対象外の面が書かれていることから判定する。([経緯](details/compat-claim-scope-must-match-evidence.md)) (昇格: 2026-09-06、出典: add-toast / fix-android-instrumented-toast-back-loading-coalescing / add-loading-toast-typed-show)
