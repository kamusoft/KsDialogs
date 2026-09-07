# Exploration: add-layout-spec 再設計 (属性の置き場)

実装フェーズ中の design Decision 6 差し戻し (2026-08-18) を受けた再探索の記録。元の探索は phase-5-1 の agenda/history、初版提案は proposal.md / design.md を参照。

## 課題 / 動機

- design Decision 6 (レイアウト属性は VM 契約のオプションメンバ) がオーナー差し戻し: VM に UI 要素が入り責務が曖昧・原典から乖離・VM を使わない呼び出し (core/ADR-0013 インライン show) に運び手がない
- 同時に KMP facade の実装が spec 不備で停止: expect interface の既定実装不可 (Kotlin 言語制約) + iOS interop 配管不在の二重ブロッカーで Decision 6 は KMP で実装不能
- Decision 6 は相方スペックレビュー指摘対応の一括改訂で入っており、ADR 級論点として単独議論されていなかった (lessons/inbox に2件捕捉済み)

## 検討した選択肢 (却下案と理由)

- 属性の分類前の一枚岩の置き場議論 → オーナー指摘で論点0 (分類) を先行。「レイアウト属性」括りにスタイル・UI 属性・機能要素が混在していた
- 契約の形: 単一 DialogLayout 型 → 却下 (静動混在で誤用が型上可能)。VM 契約 (旧 Decision 6) → 却下 (上記)。詳細は core/ADR-0015 の Alternatives
- 供給点: 登録/show の引数 → 却下 (レジスター肥大)。View 側属性のみ → 却下 (共有コードの動的配置と属性調整パネルが不成立)

## 決定事項

1. **論点0 (分類と取捨)**: 器にしか実現できないメタ要素だけを契約に残す。残10属性 = 静的メタ6 (layoutArea / dialogMargin / proportionalW/H / overlayColor / isCanceledOnTouchOutside) + 動的メタ4 (alignment×2 / offset×2)。廃止5属性 = width / height / cornerRadius / borderWidth / borderColor。isCanceledOnTouchOutside は要件漏れとして本 change に編入 → core/ADR-0014
2. **論点1 (契約の形)**: 静動2分割の不変値オブジェクト `DialogOptions` / `DialogPlacement` → core/ADR-0015
3. **論点2 (供給点)**: コンテンツ定義への添付 (UIKit assoc object / Android setTag / SwiftUI body ルート modifier / Compose 属性宣言 composable / MAUI 添付プロパティ) + placement のみ show 引数上書き。優先順位 show > 添付 > 既定。読み取り境界 = 初回レイアウトパス完了まで → core/ADR-0015
4. **クロスプラットフォーム姿勢**: 3形態を無理に同型化しない。MAUI は Native 写像で層内完結、KMP は特別処理を許容 (実際は interop への placement DTO 1本に縮小)

## ADR 候補

- 作成済み (proposed): core/ADR-0014 (属性の取捨原則)、core/ADR-0015 (供給機構)
- ADR-0008 の Border 内側描画統一 (Decision 3) は属性廃止により無効化 — 0014 に記載済み

## 未決の論点 (提案改訂へ申し送り)

- クランプで rect が内容サイズより小さい場合の中身の見え方 (クリップ/縮小/スクロール) — 初版から未規定のまま維持でよいか
- proportional = 0 の扱い (有効域 0〜1 の境界値、サイズ 0 か未指定か) — 属性が残るため論点も残る
- isCanceledOnTouchOutside の規則詳細 (ADR-0008 Decision 7 で「操作挙動として別論点」とされていたもの) — 本 change へ編入したため spec 化が必要
- Android immersive 時のシステムバー表示状態の引き継ぎ (Group 3 実装申し送り)
- SwiftUI modifier / Compose 属性宣言の値運搬が「初回レイアウトパスまでに器へ届く」ことの実現可能性プローブ (提案フェーズで実施 — lessons/inbox/design-decision-needs-platform-feasibility-probe.md の適用)

## 実装済み資産への影響 (提案改訂の入力)

- 温存: ケース表の枠組み・Decision 5 アルゴリズムの骨格 (基準領域・クランプ・anchor・offset)・提示前サイズ確定・Android 基準領域修正/透明オーバーレイ対応・MAUI hit-test 解明・MAUI の DTO 配管パターン
- 改訂: ケース表の明示サイズ/描画系ケース、サイズ優先順位 (比率 > Fill > 内容 へ簡素化)、layout-semantics.md、各形態 specs
- 撤去・置換: 各形態の `DialogLayoutProviding` 系 (VM 経路) → 添付機構へ

## 変更級の推奨: L (再確認)

5ドメイン横断 + 公開契約の確定は変わらず。提案アーティファクトの改訂 (proposal / design / specs / tasks) を ksn-propose で行う。
