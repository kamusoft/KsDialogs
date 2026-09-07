# samples デルタ (add-loading)

パリティ規約 (cross concepts sample-parity) 準拠。Sample 専用 Scenario は scenario-id-coverage の allow-missing に登録する (機械検証は手動通し + verification 証跡)。

## ADDED Requirements

### Requirement: Default Loading デモ項目

4ルートにデモ項目 `Default Loading` を追加する (SHALL)。文言 (SampleText 追加分): デモ項目の文言 `Default Loading`、開始メッセージ `Loading...`、途中更新メッセージ `Soon...`、完了時の結果表示 `結果: 完了`。デモの内容: タップでスコープ形 start を開始し、処理が進捗を 0 から 1 まで段階的に報告し (進捗表示が既定フォーマットで更新される)、途中で setMessage により `Soon...` へ更新し、完了後に表示が消えて結果表示エリアに `結果: 完了` を出す。原典 Sample の既定ローディングデモの構成を踏襲する。

#### Scenario: [LD-SA-01] Default Loading の通し
- **GIVEN** メニューを表示した Sample
- **WHEN** `Default Loading` をタップし完了まで待つ
- **THEN** 表示中に `Loading...` と進捗表示の更新・`Soon...` への更新が観察でき、完了後に表示が消えて `結果: 完了` が表示される

### Requirement: Custom Loading デモ項目

4ルートにデモ項目 `Custom Loading` を追加する (SHALL)。文言 (SampleText 追加分): デモ項目の文言 `Custom Loading`、カスタム View 内の文言 `カスタムローディング`、完了時の結果表示 `結果: 完了`。デモの内容: 進捗受け口 interface を実装した VM とカスタム View (進捗を表示に反映する) を登録し、タップでスコープ形 start を開始、進捗が VM 経由でカスタム View に反映され、完了後に表示が消えて `結果: 完了` を出す。実装経路はルートの主流儀に従う (ios / android: 手動登録 / maui: Loading 版1行登録 (DI チェーン) / kmp: 共有 VM 型 + OS 側登録)。

#### Scenario: [LD-SA-02] Custom Loading の通し
- **GIVEN** メニューを表示した Sample
- **WHEN** `Custom Loading` をタップし完了まで待つ
- **THEN** カスタム View に `カスタムローディング` と進捗の反映が観察でき、完了後に表示が消えて `結果: 完了` が表示される

#### Scenario: [LD-SA-03] 4ルートで同一デモが動く
- **GIVEN** 4ルートの Sample
- **WHEN** それぞれで `Default Loading` と `Custom Loading` を通す
- **THEN** メニュー文言・表示内容・結果表示が4ルートで一致する (差は実装経路のみ)
