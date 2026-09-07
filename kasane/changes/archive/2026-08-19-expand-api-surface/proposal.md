# Proposal: expand-api-surface

改訂履歴: 2026-08-19 add-layout-spec 完了分の反映 (phase-5-2 agenda 決定) — SwiftUI / Compose 添付 DSL のスコープ追加 (core/ADR-0015 の申し送り)・属性調整パネル操作部の読み上げ対応の追加。同日、相方スペックレビュー (second-opinion-spec-002) の採用指摘を design / specs / tasks に反映。

## Why

phase-5-2 の議論で API 表面の決定が完了した — 宣言的 UI (Compose / SwiftUI) の一級市民化 (core/ADR-0010・0011)、既定結果型 Bool (core/ADR-0012)、インライン factory show (core/ADR-0013)、Swift 向け KMP 面の型付き公開 (kmp/ADR-0003・0004)。現状は factory 戻り値が従来 View 系1本で宣言的 UI の受け口が無く、KMP iOS Sample は内部機械面 (`KsDialogsInteropBridge`) を直接使う乖離 (add-vertical-slice verify-001 ❌3) を抱えたまま。これらの決定を実装に落とし、利用者向け API 表面を完成させる。

## What Changes

- **dialog-contract (core)**: 既定結果型 Bool・技術別登録オーバーロード・インライン factory show の契約化・添付 DSL の供給契約同一性 (優先順位・スナップショットはホスティング経由でも不変)
- **ios-native**: SwiftUI 登録オーバーロード (内部 `UIHostingController`)・デフォルト associatedtype `Result = Bool`・インライン show (UIView / SwiftUI 両形)・SwiftUI 添付 DSL (`.ksDialogOptions(...)` / `.ksDialogPlacement(...)` modifier — core/ADR-0015 の申し送り)
- **android-native**: Compose 登録 (内部 `ComposeView`)・bool 既定の typealias 顔・インライン show (View / Compose 両形)・Compose 添付 DSL (`KsDialogAttributes(options, placement)` — core/ADR-0015 の申し送り)。Compose 系 API は新モジュール **`ksdialogs-compose`** に分離し、本体は Compose 非依存を維持 (2026-08-17 オーナー判断。cross/ADR-0008 への追記を蒸留へ申し送り)
- **maui-binding**: 非ジェネリック `IDialogViewModel` + VM 単型引数 `Register` オーバーロード・インライン show・登録2スタイルの Sample 内提示
- **kmp-facade**: Swift 向け型付き公開面 (ジェネリック糖衣登録 + 型付き kmpShow + 型不一致の型付きエラー + `result:` 省略 = Bool + SwiftUI 受け口)。機械面は内部専用へ戻す
- **samples**: 宣言的 UI デモ・カスタム結果型デモ (テキスト入力)・インライン show デモの追加 (パリティ準拠) + KMP iOS Sample の新 API 差し替え (consumer 境界違反の解消) + 属性調整パネル内操作部の読み上げ対応 (4ルート一斉 — add-layout-spec 残課題の引き取り、phase-5-2 agenda 決定 2026-08-19)

## Non-Goals

- 原典水準の1行登録・DI 糖衣・notifier の VM 注入 (phase-6 必須要件として申し送り済み)
- レイアウト属性の意味論・ケース表・従来 View 系の添付供給 (add-layout-spec で確定・実装済み — 本変更が実装するのは宣言的 UI の添付 DSL による供給面のみ)、提示挙動の拡充 (phase-5-3)
- 利用者向けドキュメント整備 (phase-9。Sample 内のコード例提示まで)

## Impact

- 破壊的変更なし (すべて追加。既存の (vm, notifier) → View factory と2型引数 Register は不変)
- samples のメニュー・mock は add-layout-spec と共有 — **実装順序は add-layout-spec → 本変更** (改訂モックを前提にデモ項目を足す)。add-layout-spec は 2026-08-19 完了・アーカイブ済みで前提充足
- リスク: (1) SwiftUI / Compose ホスティングの内容サイズ測定が add-layout-spec のレイアウト規則と整合する必要 — ケース表適合はホスティング経由でも要求され、**添付 DSL で供給した属性値の適合も含む** (2) Android 配布物が `ksdialogs-compose` の分だけ増え、lockstep バージョン (cross/ADR-0009) の管理対象になる。※ Compose 系 API の公開名は design Decision 2 で別名 (`registerCompose` / `showCompose`) に確定済み

## 級: L

5ドメイン横断 + 公開 API 表面の確定 (覆すコスト高) のため。

domain: cross
roadmap: library-foundation/phase-5-2-api-surface
