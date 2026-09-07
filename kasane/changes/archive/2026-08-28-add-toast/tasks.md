# Tasks: add-toast

## 1. 共通仕様と契約の下ごしらえ

- [x] 1.1 レイアウト共通ケース表 (`core/layout-spec/cases.json`) に Toast の契約既定配置 (可視領域下部中央 + 既定オフセット 80) のケースを追加 (→ Requirement: 配置属性と ToastStyle)
- [x] 1.2 scenario-id-coverage に TS 系 ID を登録し、Sample 専用 Scenario (TS-SA) を allow-missing に登録 (→ Requirement: 4ルートパリティ)

## 2. iOS native

- [x] 2.1 契約型: `KsToast` protocol・`ToastStyle`・`ToastViewModel`・`ToastViewRegistry` (→ Requirement: Swift 公開面の Toast / Toast の公開面と fire-and-forget)
- [x] 2.2 器: `ToastContainerViewController` — Loading の器の非モーダル派生 (ヒットテスト素通しのルート View・覆いなし・共有部品 `DialogLayoutApplier` / `DialogTransitionRunner`) (→ Requirement: 完全非対話と非モーダル / 出入りの演出の適用)
- [x] 2.3 `ToastCoordinator` — 表示リスト・duration タイマー・直列化・Loading 前面維持 (`insertSubview(belowSubview:)`) (→ Requirement: 多重表示と表示の継続)
- [x] 2.4 `ToastDefaultContentView` — ピルの内蔵コンテンツ (背景自前描画)・ToastStyle 反映 (→ Requirement: Toast の公開面と fire-and-forget)
- [x] 2.5 `Toast.shared` エントリと UIKit / SwiftUI 登録・インライン factory (→ Requirement: Swift 公開面の Toast)
- [x] 2.6 テスト: TS-CO / NM / MX / AT / TR / AC の同名テスト + TS-IO (正・負の compile 検査・SwiftUI 同等) + レイアウト共通ケース表の Toast 適合 (→ 全 TS Scenario)
- [x] 2.7 デフォルト View の accessibility: announce 発行・フォーカス非移動 (→ Requirement: 支援技術への通知)

## 3. Android native

- [x] 3.1 契約型: `KsToast`・`ToastStyle`・`ToastViewModel`・`ToastViewRegistry` (→ Requirement: Kotlin 公開面の Toast)
- [x] 3.2 器: `ToastContainer` — 全画面透過 Window + FLAG_NOT_FOCUSABLE / FLAG_NOT_TOUCHABLE・覆いなし・共有部品 (→ Requirement: 完全非対話と非モーダル / 出入りの演出の適用)
- [x] 3.3 `ToastCoordinator` — 表示リスト・タイマー・Activity 再生成の起動順再取り付け・Loading 前面維持 (LoadingCoordinator への演出なし再前面化依頼) (→ Requirement: 多重表示と表示の継続)
- [x] 3.4 `ToastDefaultContentView` — ピルの内蔵コンテンツ・ToastStyle 反映 (→ Requirement: Toast の公開面と fire-and-forget)
- [x] 3.5 `Toast` エントリと従来 View 系登録・インライン factory + ksdialogs-compose の Compose 系オーバーロード (→ Requirement: Kotlin 公開面の Toast)
- [x] 3.6 テスト: TS-CO / NM / MX / AT / TR / AC の同名テスト + TS-AN (正・負の compile 検査・Compose 同等・再生成の多重再取り付け) + レイアウト共通ケース表の Toast 適合 (→ 全 TS Scenario)
- [x] 3.7 実機確認: タッチ素通し Window と IME・システムジェスチャの干渉検証 — Toast 表示中に IME を出し入れし、戻る・ホームのジェスチャが通ることを操作前後の状態で判定 (design Risks) (→ Requirement: 完全非対話と非モーダル)
- [x] 3.8 デフォルト View の accessibility: announce 発行 (TalkBack イベント)・フォーカス非移動 (→ Requirement: 支援技術への通知)

## 4. MAUI binding

- [x] 4.1 C# 公開面: `IKsToast`・静的 `Toast`・`ToastStyle`・C# 層レジストリ (DI チェーン1行含む)・インライン factory (→ Requirement: C# 公開面の Toast と bridge 委譲)
- [x] 4.2 bridge (Swift / Kotlin): Toast 面の増設 — message 入口・互換面専用 Toast 用 VM 型の Native 登録・カスタム View ホスティング・style 反映 (maui/ADR-0001 構造) (→ 同上)
- [x] 4.3 テスト: TS-MA (正・負の compile 検査・パススルー検証) (→ 同上)

## 5. KMP facade

- [x] 5.1 commonMain 契約: `KsToast`・既定エントリ (duration は ms 整数) (→ Requirement: commonMain 契約の Toast と Native 委譲)
- [x] 5.2 gateway (Android / iOS) と Swift パッケージ側の登録 API (→ 同上)
- [x] 5.3 テスト: TS-KM (正・負の compile 検査・型キー解決のパススルー検証) (→ 同上)

## 6. Samples (4ルート)

- [x] 6.1 デモ5項目 (`Default Toast` / `Custom Toast` / `Toast Stack` / `Toast Placement` / `Toast Overlap`) を4ルートに追加 (→ Requirement: 各デモ項目)
- [x] 6.2 デモ駆動モードの安定デモ ID 5件を追加し samples/README.md を更新 (→ Requirement: 4ルートパリティ)
- [x] 6.3 4ルート通し + 撮影証跡 (verification/) — Toast Overlap は固定時系列で自動進行し、Loading 前面規則は Loading 表示中のフレームで、タッチ素通しは背後要素の状態変化で判定する (→ TS-SA-01〜06, TS-MX-05, TS-NM-01)

## 8. スコープ追加 (オーナー決定 2026-08-27 — deviation.md 参照)

- [x] 8.1 MAUI iOS の Loading / Dialog の factory 例外境界修正 (BridgeContentSupply 共通化・nullable 境界・既存失敗契約への合流・iOS native factory 契約の throws 化)
- [x] 8.2 Android Toast provider の例外境界 (CreateOrDiscard 化・Kotlin bridge の null 受け) — review-003 指摘
- [x] 8.3 Loading / Dialog の元例外の退避と再送 (BridgeContentFailure) — review-003 / second-opinion-code-003 双方一致指摘
- [x] 8.4 修正後バイナリでの MAUI 実機/シミュレータ証跡の更新 — review-004 指摘 (maui (Android) `custom-toast` / maui (iOS) `custom-toast`・`basic-dialog` を再ビルド後に通し、`ui/verification/sample-walkthrough.md`「2周目修正 (例外境界) 後の再確認」に記録)
- [x] 8.5 失敗系の実機観測 (A 方式 — オーナー決定 2026-08-28): Sample へ throwing factory を一時追加し、MAUI iOS の Toast / Loading / Dialog と MAUI Android の Toast で「クラッシュせず既存失敗契約に流れ後続継続」を撮影。一時編集は copy 方式で復元し git diff で確認

## 7. UI 照合

- [x] 7.1 デフォルト View (ピル) の実装スクリーンショットと承認済み mock (ui/mock/approved.png) の視覚照合 (→ Requirement: Toast の公開面と fire-and-forget)
