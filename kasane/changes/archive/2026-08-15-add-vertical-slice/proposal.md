# Proposal: add-vertical-slice

## Why

3形態 (Native / MAUI / KMP) を最初から掲げる本プロジェクトの最大リスクは「後から形態を載せたら core 契約が合わなかった」であり、発見が遅いほど覆すコストが跳ね上がる。phase-2 の scaffold は4形態のビルドルートという器のみで、公開 API の実体はゼロ。シンプルな Dialog 1本を core 契約 + iOS Native + Android Native + MAUI binding + KMP の全形態に貫通させ、アーキテクチャリスク (レジストリキー同一性・Swift async 変換・binding ビルド連携) をこの1本で潰す (roadmap 前提の縦串スライス戦略)。

## What Changes

題材は「最小カスタム View ダイアログ」(View = ラベル1個 + OK/Cancel、VM = メッセージ文字列1個、結果 = completed(単純値) / cancelled)。影響する能力:

- **dialog-contract (core)**: `KsDialogs` 契約 + `DialogResult` 型付き結果 (core/ADR-0002・0003) + VM 型キー → View factory レジストリ契約 (core/ADR-0004) の初実装
- **ios-native**: Swift 実装 — レジストリ実体 + show/dismiss + `@objc` 互換面 (cross/ADR-0002)
- **android-native**: Kotlin 実装 — レジストリ実体 + show/dismiss
- **maui-binding**: 使い捨て Bridge + C# 層レジストリ + internal gateway seam (maui/ADR-0001・0002)。iOS は標準 XcodeProject アイテム、Android は標準 AndroidGradleProject を実測 (失敗時 gradlew Exec フォールバック)
- **kmp-facade**: commonMain 契約 + 既定 singleton エントリの expect/actual + Native lib への全委譲 (kmp/ADR-0001・0002)
- **samples**: 集約 samples/ 4ルート新設 (cross/ADR-0006)、parity 準拠の「Basic Dialog」1項目 (cross/ADR-0007)
- **共通仕様テストの器の初版**: シナリオ表 (ID + 期待値 + OS 差記録欄) + 同名テスト規約。初版シナリオは多段表示 (a)〜(d)
- スモークテスト用マーカー BuildProbe (4ルート) の削除 (phase-2 申し送り)

## Non-Goals

- **View 直接渡しの show 系統** (原典 `ShowAsync<TView>` 相当。レジストリ・VM 登録不要の起動モード。Native 利用の主流になり得る) — phase-5。縦串のリスク経路 (レジストリ・KMP 境界) を踏まないため縦串には含めないが、design Decision 1 の型原則 (結果型は表示物側が宣言) の View 版として非破壊追加できることを design で担保する
- Dialog 全機能の移植 (レイアウト属性セット・styling・多段表示の完全対応) — phase-5
- DI コンテナ連携の糖衣 (SetIocConfig 相当) — phase-6
- Loading / Toast — phase-7 / phase-8
- UI 自動テストインフラの整備 — phase-5 以降 (受け入れは手動確認 + 結果経路の自動テストのみ)
- パッケージング・配布基盤 — ロードマップ非ゴール

## Impact

- 破壊的変更なし (公開 API の初導入。既存消費者ゼロ)
- BuildProbe 削除は internal のため影響なし
- リスク: (1) KMP iOS のレジストリキー同一性と Swift async 変換の粗 — フォールバック決定済み (kmp/ADR-0001、崩れた場合は kmp/ADR-0002 見直し) (2) Android 標準ビルドアイテムの成立が未実測 — gradlew Exec フォールバック確保済み (3) KMP deployment target の非保証フラグ依存 (phase-2 申し送り、本変更で正統手段を再確認)

## 級: L

全5ドメイン横断 + アーキテクチャ土台の確立 (覆すコスト高) のため。

domain: cross
roadmap: library-foundation/phase-4-vertical-slice
