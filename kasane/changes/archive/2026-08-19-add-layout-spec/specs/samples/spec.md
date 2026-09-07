# samples デルタスペック (add-layout-spec)

改訂履歴: 2026-08-18 供給機構の変更に伴う注記 — パネルの配置・Offset は show の placement 引数、LayoutArea は factory 内での View 添付 (options) で実現する (core/ADR-0015。show 毎回生成モデルのため添付は表示ごとに再構成できる)。要件・Scenario・文言・承認モックは変更なし。2026-08-17 相方レビュー採用指摘 + オーナー指示 (原典サンプルの属性調整パネル方式) の反映。

## ADDED Requirements

### Requirement: レイアウトデモ項目 (属性調整パネル)

samples の 4 ルートすべてに、レイアウト属性を調整してからダイアログを表示できるデモ項目をパリティ準拠 (cross/ADR-0007) で追加すること (SHALL)。パネルで調整できる属性は最低限、水平・垂直配置 (Start / Center / End の択一)、OffsetX / OffsetY (数値)、LayoutArea (visibleArea / window の切替) とする。画面構成・文言の実物は ui/ (brief.md の文言表 + 承認モック) を正とする。

#### Scenario: 属性を変えて表示すると反映される
- **GIVEN** レイアウトデモのパネルで垂直配置を End に変更した
- **WHEN** 表示操作を行う
- **THEN** ダイアログは下端寄せで表示され、閉じると結果が既存デモと同じ形式で表示される

#### Scenario: パネルの初期値は属性の既定値
- **GIVEN** レイアウトデモのパネルを開いた直後
- **WHEN** 各項目の選択状態を見る
- **THEN** 配置は Center / Offset は 0 / LayoutArea は visibleArea (契約の既定値と一致) になっている

### Requirement: 結果表示エリアの表示条件

メニュー画面の結果表示エリアは、一度も結果が出ていない初期状態では表示せず、最初の結果確定以降に表示すること (SHALL)。(phase-4 実装の挙動を契約として追認するもの)

#### Scenario: 初期状態では結果エリアが無い
- **GIVEN** アプリを起動した直後 (どのデモも実行していない)
- **WHEN** メニュー画面を見る
- **THEN** 結果表示エリアは存在しない

#### Scenario: 結果確定後に表示される
- **GIVEN** いずれかのデモでダイアログを完了またはキャンセルした
- **WHEN** メニュー画面に戻る
- **THEN** 結果表示エリアに直近の結果が表示される
