# maui-binding デルタスペック (add-layout-spec)

改訂履歴: 2026-08-18 全面改訂 — 供給機構を VM 契約から添付プロパティ + Show placement 引数へ (core/ADR-0015)。当たり領域要件は初版から維持 (実装・証跡完了済み)。

## ADDED Requirements

### Requirement: メタ属性の供給とパススルー (MAUI)

MAUI 公開面は `DialogOptions` / `DialogPlacement` を添付プロパティ (View 定義側) と Show の placement 引数で供給できること (SHALL)。MAUI 層は値を束ねて Native 実装へ無変換で引き渡し (色は ARGB 32bit 整数)、レイアウト計算を MAUI 層で再実装しないこと。実効値の優先順位は dialog-contract に従うこと。

#### Scenario: 添付プロパティがネイティブへ届く
- **GIVEN** XAML の添付プロパティで options と placement を設定した View
- **WHEN** show する
- **THEN** Native 側が受け取った実効値は添付プロパティで設定した値と一致する

#### Scenario: Show の placement 引数が添付に勝つ
- **GIVEN** 添付プロパティで placement を設定した View と、Show の placement 引数
- **WHEN** show する
- **THEN** Native 側が受け取る placement は Show 引数の値である

### Requirement: 当たり領域は描画領域と一致する (MAUI)

ダイアログ内の操作要素は、描画上の領域へのタップで反応すること (SHALL)。

#### Scenario: 描画中心のタップが反応する
- **GIVEN** ボタンを含むダイアログを表示した状態
- **WHEN** ボタンの描画上の中心をタップする
- **THEN** ボタンのハンドラが発火する (phase-4 記録の「中心で反応せず上端寄りで反応」事象が解消している)
