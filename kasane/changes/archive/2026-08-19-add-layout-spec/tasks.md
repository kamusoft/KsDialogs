# Tasks: add-layout-spec

改訂履歴: 2026-08-18 全面改訂 — core/ADR-0014・0015 の反映 (供給機構の置換・属性取捨・isCanceledOnTouchOutside)。初版実装済み資産のうちエンジン・検証基盤・透明オーバーレイ・hit-test 解明は温存し、VM 経路を撤去・置換する (初版タスクのチェック状態は改訂によりリセット)。2026-08-17 初版。

## 1. core 仕様とケース表の配線

- [x] 1.1 `concepts/core/api/layout-semantics.md` の改訂 — DialogOptions / DialogPlacement の写像表・供給と優先順位・軸別規則の簡素化 (比率 > Fill > 内容)・isCanceledOnTouchOutside 規則。廃止属性の記述を除去。「まだ決めていないこと」節はクランプ時の中身の見え方のみに更新 (proportional = 0 は「0 以下は未指定」で確定済み) (→ Requirement: メタ属性セットと既定値 / 属性の供給と優先順位 / 軸別レイアウト規則 / 外側タップキャンセル)
- [x] 1.2 改訂版 layout-cases.json (19ケース) を `core/layout-spec/cases.json` へ再配置 (期待値の変更は deviation 扱い) (→ Requirement: 共通ケース表への適合)

## 2. iOS Native

- [x] 2.1 VM 経路の撤去 (`DialogLayoutProviding` と VM 契約への継承・既定実装の削除) + `DialogOptions` / `DialogPlacement` 型 + `UIView` 添付 extension プロパティ + show の placement 引数 (→ Requirement: メタ属性の供給機構 (iOS)、dialog-contract: 既存コードの互換性)
- [x] 2.2 器の添付読み取り (スナップショット = 初回レイアウトパス完了時点) とレイアウト実装 (温存エンジン) の接続改修 + 供給・優先順位のテスト (添付のみ / show 置換 [オブジェクト単位] / 既定値 / 無効値正規化 / レイアウト完了後の添付変更不反映) + 公開 API 形状のコンパイル検査 (→ Requirement: 属性の供給と優先順位 / メタ属性セットと既定値)
- [x] 2.3 ケース表ローダー・全量検証テストの改訂 (19ケース対応、テスト Support の属性宣言を添付方式へ書き換え) (→ Scenario: ケース表の全量検証が通る (実 frame) / 提示前サイズ確定 (iOS))
- [x] 2.4 isCanceledOnTouchOutside の実装とテスト (既定 true の外側タップ = cancelled / false のモーダル維持) (→ Requirement: 外側タップキャンセル (iOS))

## 3. Android Native

- [x] 3.1 VM 経路の撤去 + `DialogOptions` / `DialogPlacement` 型 + `View` 添付 extension プロパティ + show の placement 引数 (→ Requirement: メタ属性の供給機構 (Android)、dialog-contract: 既存コードの互換性)
- [x] 3.2 器の添付読み取り (スナップショット = 初回レイアウトパス完了時点) とレイアウト実装の接続改修 + 供給・優先順位のテスト (添付のみ / show 置換 [オブジェクト単位] / 既定値 / 無効値正規化 / レイアウト完了後の添付変更不反映) + 公開 API 形状のコンパイル検査 (→ Requirement: 属性の供給と優先順位 / メタ属性セットと既定値)
- [x] 3.3 ケース表ローダー・instrumented 全量検証テストの改訂 (19ケース、添付方式へ書き換え)。透明オーバーレイ対応 (実装済み) と基準領域テストが改修後も green であることを含む (→ Scenario: ケース表の全量検証が通る (実 View) / 透明時も配置規則が保たれる)
- [x] 3.4 isCanceledOnTouchOutside の実装と instrumented テスト (→ Requirement: 外側タップキャンセル (Android))

## 4. MAUI binding

- [x] 4.1 `IDialogLayoutProviding` の撤去 → 添付プロパティ (`ksd:Dialog.*` スカラー10個、内部で2オブジェクトに束ねる) + Show の placement 引数 + DTO 束ね写像の改修 (isCanceledOnTouchOutside を含む options 輸送) + パススルーテスト改訂 + 公開 API 形状のコンパイル検査 (→ Requirement: メタ属性の供給とパススルー (MAUI))
- [x] 4.2 当たり判定事象の原因究明と解消 — **初版で完了済み** (不具合非実在の解明、証跡: verification/maui-hit-test/)。改修後のリグレッション確認は 7.3 (→ Requirement: 当たり領域は描画領域と一致する (MAUI))

## 5. KMP facade

- [x] 5.1 commonMain へ `DialogPlacement` (具象 data class) を公開 (`DialogOptions` は公開しない) + show の placement 引数 (Android actual = Native 呼び出し / iOS actual = interop 面への placement DTO 配管を新設) + パススルーテスト (androidHost / iosSimulator) + 既存互換と公開 API 形状のコンパイル検査 (→ Requirement: 共有コードからの placement 指定 (KMP))

## 6. samples と UI

- [x] 6.1 モック改訂の反映 (タップ領域・surface-variant・初期非表示) — 4ルート (→ Requirement: 結果表示エリアの表示条件)
- [x] 6.2 レイアウトデモ (属性調整パネル) の追加 — 4ルート パリティ準拠、文言は ui/brief.md の文言表が正。配置・Offset は show の placement 引数、LayoutArea は factory 内の View 添付で実現 (→ Requirement: レイアウトデモ項目 (属性調整パネル))
- [x] 6.3 mock との視覚照合 (approved.png / approved-layout-panel.png 基準、4ルートのスクリーンショット突き合わせ)
- [x] 6.4 `concepts/cross/conventions/sample-parity.md` のデモ項目・文言表を Layout Dialog 分で更新 (ui/brief.md の文言表と一致させる) (→ Requirement: レイアウトデモ項目 (属性調整パネル))

## 7. 検証 (実環境の個別確認 — design Decision 7 の2層目)

- [x] 7.1 全ビルドルートのビルドと全件テスト通過
- [x] 7.2 Android 透明オーバーレイ: 表示前後のシステムバーを同一条件で撮影比較し証跡を残す (→ Scenario: 透明オーバーレイ表示中のステータスバー)
- [x] 7.3 パリティ準拠の Sample 通し (手動確認: パネル操作 → 配置反映・結果経路・初期非表示 → 結果表示の遷移・外側タップキャンセルの既定挙動・MAUI iOS の描画中心タップ再確認)
