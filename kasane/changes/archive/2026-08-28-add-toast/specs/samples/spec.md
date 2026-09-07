# samples デルタ (add-toast)

パリティ規約 (cross concepts sample-parity) 準拠。Sample 専用 Scenario は scenario-id-coverage の allow-missing に登録する (機械検証は手動通し + verification 証跡)。デモ駆動モード (cross/ADR-0010) の安定デモ ID を5件追加し、samples/README.md の一覧を更新する。

## ADDED Requirements

### Requirement: Default Toast デモ項目

4ルートにデモ項目 `Default Toast` を追加する (SHALL)。文言 (SampleText 追加分): デモ項目の文言 `Default Toast`、表示メッセージ `Hello Toast!`。デモの内容: タップで show(message) を呼び、デフォルト View の Toast が契約既定の配置に表示され、既定 duration の経過で自動的に消える。

#### Scenario: [TS-SA-01] Default Toast の通し
- **GIVEN** メニューを表示した Sample
- **WHEN** `Default Toast` をタップする
- **THEN** `Hello Toast!` のデフォルト View Toast が表示され、時間経過で自動的に消える

### Requirement: Custom Toast デモ項目

4ルートにデモ項目 `Custom Toast` を追加する (SHALL)。文言 (SampleText 追加分): デモ項目の文言 `Custom Toast`、登録経路の View 内文言 `カスタムトースト`、インライン経路の View 内文言 `インライントースト`。デモの内容: タップで登録経路のカスタム Toast とインライン経路のカスタム Toast を続けて表示し、両方が重なって表示されたのち各自の duration で消える。実装経路はルートの主流儀に従う (ios / android: 手動登録 / maui: DI チェーンの1行登録 / kmp: 共有 VM 型 + OS 側登録)。

#### Scenario: [TS-SA-02] Custom Toast の通し
- **GIVEN** メニューを表示した Sample
- **WHEN** `Custom Toast` をタップする
- **THEN** `カスタムトースト` (登録経路) と `インライントースト` (インライン経路) の2枚が重なって表示され、時間経過で消える

### Requirement: Toast Stack デモ項目

4ルートにデモ項目 `Toast Stack` を追加する (SHALL)。文言 (SampleText 追加分): デモ項目の文言 `Toast Stack`、表示メッセージ `Toast 1` / `Toast 2` / 長文メッセージ `Toast 3: 長いメッセージは複数行に折り返され、コンテンツの高さが確保されることを確認する`。デモの内容: タップで3枚の Toast を duration をずらして連続表示する。3枚は placement の上方向オフセットを少しずつ変えて表示し (同一配置では同座標に完全に重なって視認できないため — 視認用のずらしであり、placement 上書きの実演も兼ねる)、それぞれが自分の duration で独立に消える様子を見せる。3枚目の長文で、デフォルト View が複数行に折り返されて高さがコンテンツに追随することをあわせて検証する。

#### Scenario: [TS-SA-03] Toast Stack の通し
- **GIVEN** メニューを表示した Sample
- **WHEN** `Toast Stack` をタップする
- **THEN** 3枚が起動順に重なって表示され (3枚目の長文は複数行に折り返されて高さが伸びる)、duration の短いものから順に独立して消える

### Requirement: Toast Placement デモ項目

4ルートにデモ項目 `Toast Placement` を追加する (SHALL)。文言 (SampleText 追加分): デモ項目の文言 `Toast Placement`、表示メッセージ `Placed Toast`。デモの内容: タップで契約既定と異なる placement (上部中央) と長めの duration を指定して表示し、show 引数による配置上書きを見せる。

#### Scenario: [TS-SA-04] Toast Placement の通し
- **GIVEN** メニューを表示した Sample
- **WHEN** `Toast Placement` をタップする
- **THEN** `Placed Toast` が指定した配置 (上部中央) に表示され、指定 duration の経過で消える

### Requirement: Toast Overlap デモ項目 (機能間多重起動)

4ルートにデモ項目 `Toast Overlap` を追加する (SHALL)。文言 (SampleText 追加分): デモ項目の文言 `Toast Overlap`、表示メッセージ `Overlap Toast`、完了時の結果表示 `結果: 完了`。デモの内容は操作者に依存しない固定の時系列で自動進行する: タップで duration 10000ms の Toast を表示 → 直後に Dialog を表示し 2000ms 後にデモが自動で閉じる (Toast と Dialog の共存) → 続けて Loading を 2000ms 表示して自動終了する (Loading が Toast より前面に出る — core/ADR-0030 の順序規則の目視検証)。時系列上 Loading 終了時点で Toast は必ず残っており、duration 満了で消えたのち結果表示エリアに `結果: 完了` を出す。

#### Scenario: [TS-SA-05] Toast Overlap の通し
- **GIVEN** メニューを表示した Sample
- **WHEN** `Toast Overlap` をタップし完了まで待つ
- **THEN** Toast 表示中に Dialog が共存し、Loading は Toast より前面に表示され、Loading 終了後も Toast が残り、Toast の消滅後に `結果: 完了` が表示される

### Requirement: 4ルートパリティ

4ルートで上記5デモが同一に動く (SHALL — cross/ADR-0007)。

#### Scenario: [TS-SA-06] 4ルートで同一デモが動く
- **GIVEN** 4ルートの Sample
- **WHEN** それぞれで5デモを通す
- **THEN** メニュー文言・表示内容・重なり・結果表示が4ルートで一致する (差は実装経路のみ)
