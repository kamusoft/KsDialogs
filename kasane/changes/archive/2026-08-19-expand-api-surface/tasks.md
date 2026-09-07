# Tasks: expand-api-surface

前提: add-layout-spec の実装完了後に着手する (samples メニュー・mock の共有、および宣言的 UI のケース表適合が add-layout-spec の器に依存するため)。→ 2026-08-19 完了・アーカイブ済みで充足。

改訂履歴: 2026-08-17 相方レビュー採用指摘の反映 — Compose 別モジュール (design Decision 7)・ホスト lifecycle (Decision 8)・KMP キャンセル (Decision 9)・compile 検査の明記。
2026-08-19 add-layout-spec 完了分の反映 — 添付 DSL タスク (2.4・3.4)・パネル読み上げ対応 (6.4) を追加。
2026-08-19 相方スペックレビュー (spec-002) 採用指摘の反映 — layout-semantics 更新 (1.1)・MAUI 負検査の規約組み込み (4.1)・読み上げ検査の証跡 (6.4)。同レビューで実機確認済みと判明したため、いったん追加した MAUI スナップショット配線の実機確認タスクは撤回 (証跡: `changes/archive/2026-08-19-add-layout-spec/verification/maui-snapshot-wiring/`、review-004 #4 で解消判定済み — agenda の申し送りが古かった)。

## 1. core 契約

- [x] 1.1 真偽値省略形・技術別登録の挙動同一性・インライン show の concepts 契約文書化 (`concepts/core/api/` 配下)。`layout-semantics.md` の追随を含む — 添付の面の表へ SwiftUI / Compose 行を追加し、「宣言的 UI 向けの添付イディオムはまだ提供していない」記述を削除する (→ Requirement: 真偽値結果の省略形 / 技術別登録の挙動同一性 / インライン factory show)

## 2. iOS Native

- [x] 2.1 デフォルト associatedtype `Result = Bool` の導入と宣言表 compile 検査 (→ Requirement: Result のデフォルト (iOS)、Scenario: 宣言表どおりの利用コードがコンパイルできる)
- [x] 2.2 SwiftUI 登録オーバーロード + child containment ホスティング (design Decision 1・3・8) とテスト (全閉鎖経路の解放検証を含む) (→ Requirement: SwiftUI 登録オーバーロード (iOS) / SwiftUI ホストの所有と破棄 (iOS))
- [x] 2.3 インライン show (UIView / SwiftUI) とテスト — 非干渉 (既存登録共存・並行独立) を含む (→ Requirement: インライン show (iOS)、design Decision 5)
- [x] 2.4 SwiftUI 添付 DSL (`.ksDialogOptions` / `.ksDialogPlacement`、PreferenceKey + 到達待ち収束、design Decision 10) とテスト — スナップショット・外側勝ち・ケース表適合を含む (→ Requirement: SwiftUI 添付 DSL (iOS))

## 3. Android Native

- [x] 3.1 bool 既定 typealias の導入と宣言表 compile 検査 (→ Requirement: bool 既定の VM 契約の顔 (Android))
- [x] 3.2 **`ksdialogs-compose` モジュール新設** (design Decision 7): registerCompose / showCompose + ComposeView ホスティング (lifecycle owner 伝播・全閉鎖経路 dispose、design Decision 8)。本体の Compose 非依存を依存グラフ検査で固定 (→ Requirement: Compose 登録 (Android) / Compose ホストの lifecycle と破棄 (Android))
- [x] 3.3 インライン show (View 版は本体・Compose 版は compose モジュール) とテスト — 非干渉を含む (→ Requirement: インライン show (Android)、design Decision 5)
- [x] 3.4 Compose 添付 DSL (`KsDialogAttributes`、SideEffect 書き込み + doOnPreDraw 読み取り、Lazy 制約のドキュメント契約明記、design Decision 11) とテスト — スナップショット・ケース表適合を含む (→ Requirement: Compose 添付 DSL (Android))

## 4. MAUI binding

- [x] 4.1 非ジェネリック `IDialogViewModel` + VM 単型引数 Register + 4形式 compile 検査 + negative compile check — 新設フラグの単独実行で期待した診断1件で失敗することを確認し、`concepts/cross/conventions/test-execution.md` の負検査表・件数 (現行16本) を更新する (→ Requirement: bool 既定の登録 (MAUI))
- [x] 4.2 インライン ShowAsync とテスト — 非干渉を含む (→ Requirement: インライン show (MAUI))

## 5. KMP facade (Swift 向け公開面)

- [x] 5.1 型付き登録 (ジェネリック糖衣、result: 省略 = Bool、UIView / SwiftUI 両形) と宣言表 compile 検査 — SwiftUI 添付 DSL が KMP 経路でも有効なことの検証を含む (→ Requirement: Swift 向け型付き登録 (KMP) / 宣言表どおりの Swift 公開面)
- [x] 5.2 型付き show とテスト — Swift から await して型付き結果 (→ Requirement: Swift からの型付き show (KMP))
- [x] 5.3 show 単位キャンセルハンドルの機械面追加と、Task キャンセルでの閉鎖 + cancelled 1回確定のテスト (design Decision 9) (→ Requirement: 呼び出し元キャンセルでの閉鎖 (KMP Swift 面))
- [x] 5.4 型不一致の型付きエラー throw とテスト (design Decision 4) (→ Requirement: 型不一致の型付きエラー (KMP))
- [x] 5.5 機械面のドキュメントコメント整備 (「cinterop 委譲専用」。access level は public 維持) + cinterop 委譲の回帰確認 (→ Requirement: 機械面の利用者非公開 (ABI は公開のまま))

## 6. samples と UI

- [x] 6.1 新デモ項目3種 (宣言的 UI / テキスト入力 / インライン) — 4ルート パリティ準拠、文言は ui/brief.md の文言表が正 (→ Requirement: API 表面の新デモ項目)
- [x] 6.2 KMP iOS Sample の登録コードを新 API へ差し替え (→ Requirement: KMP iOS Sample の公開 API 化)
- [x] 6.3 MAUI Sample の登録2スタイル併用 (→ Requirement: MAUI Sample の登録2スタイル提示)
- [x] 6.4 属性調整パネル操作部の読み上げ対応 — 4ルート一斉、名前 + 役割 + 状態、同名選択肢は複合名 (行文言 + 選択肢文言)。accessibility tree 検査または実機スクリーンリーダー確認の証跡を4ルート分残す (→ Requirement: 属性調整パネル操作部の読み上げ対応)
- [x] 6.5 mock との視覚照合 (approved.png 基準。読み上げ対応は非視覚のため対象外 — 検証は 6.4 の accessibility 検査で行う) — 照合作業は完了 (ui/verification/ に4ルート分の証跡、2周で収束)。**2026-08-19 オーナー最終承認**

## 7. 検証

- [x] 7.1 全ビルドルートのビルドと全件テスト通過
- [x] 7.2 パリティ準拠の Sample 通し (手動確認: 新デモ3種 × 完了/キャンセル経路 + Swift Task キャンセルの閉鎖)
- [x] 7.3 蒸留への申し送り記録: cross/ADR-0008 への ksdialogs-compose 追記、sample-parity 規約への文言表反映とパネル操作部の読み上げ規約反映
