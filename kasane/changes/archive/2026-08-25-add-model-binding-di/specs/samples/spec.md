# samples デルタ (add-model-binding-di)

パリティ規約 (cross concepts sample-parity) 準拠。実装経路の差のみを持つデモ項目は Declarative Dialog の先例に倣う。

## ADDED Requirements

### Requirement: Model Dialog デモ項目

4ルートにデモ項目 `Model Dialog` を追加する (SHALL)。文言 (SampleText 追加分): デモ項目の文言 `Model Dialog`、ダイアログのメッセージ `ViewModel から表示しています`。操作と結果表示は既存書式 (`OK` = completed(true) / `キャンセル` = cancelled、結果表示エリアは既存)。実装経路はルートごとに本変更の主経路を使う — ios / android: VM factory 手動登録 + 型指定 show (configure でメッセージを設定) / maui: `RegisterForDialog` + 型指定 show (configure) / kmp: 共有 VM のインスタンス渡し show (commonMain の呼び出し面は現状維持のため。差は実装経路のみで画面は同一)。全ルートの View は VM 供給経由 (`vm.notifier` 相当) で結果を報告する。

#### Scenario: [MB-SM-01] Model Dialog の完了経路
- **GIVEN** メニューを表示した Sample
- **WHEN** `Model Dialog` を開き `OK` を押す
- **THEN** ダイアログに `ViewModel から表示しています` が表示されており、閉じた後に `結果: completed(true)` が表示される

#### Scenario: [MB-SM-02] 4ルートで同一デモが動く
- **GIVEN** 4ルートの Sample
- **WHEN** それぞれで `Model Dialog` の完了経路とキャンセル経路を通す
- **THEN** メニュー文言・ダイアログ内容・結果表示が4ルートで一致する (差は実装経路のみ)

### Requirement: MAUI ルートのコンテナ連携構成

MAUI ルートの Sample は、登録を `AddKsDialogs` + `RegisterForDialog` の DI チェーン構成で行う参考実装とする (SHALL)。Model Dialog は 1行登録経由で解決される。他ルートは直接登録 (手動) のままとし、コンテナ連携の見本は MAUI ルートが担う。

#### Scenario: [MB-SM-03] MAUI ルートが DI チェーン構成で全デモを通す
- **GIVEN** DI チェーン構成に変更した MAUI Sample
- **WHEN** 既存の全デモ項目と Model Dialog を通す
- **THEN** 既存デモの挙動は変わらず、Model Dialog が 1行登録経由で動く
