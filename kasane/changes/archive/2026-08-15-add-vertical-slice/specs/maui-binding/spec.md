# maui-binding デルタスペック

.NET MAUI 形態での dialog-contract の貫通。契約の意味論は [dialog-contract](../dialog-contract/spec.md) に従う。構造判断は maui/ADR-0001 (使い捨て Bridge・C# 層レジストリ)・maui/ADR-0002 (gateway seam) が正。

## ADDED Requirements

### Requirement: MAUI 公開 API での貫通

MAUI は `Task` を返す形で show を公開し、型付き結果 (completed / cancelled) を返す SHALL。MAUI 側レジストリは VM 型をキーに MAUI View の factory を受け付け、show 時に MAUI View を platform view へ実体化して Native 実装で表示する SHALL。

#### Scenario: C# から show して結果を受け取る
- **GIVEN** MAUI アプリが VM 型に MAUI View の factory を登録している
- **WHEN** show を await し、表示されたダイアログを完了操作で閉じる
- **THEN** Task の結果として completed(結果値) が正しい型で得られる

#### Scenario: MAUI View がダイアログとして表示される
- **GIVEN** VM 型に MAUI View の factory が登録されている
- **WHEN** その VM で show を呼び出す
- **THEN** factory が生成した MAUI View の内容が iOS / Android のダイアログとして画面に表示される

### Requirement: 結果経路の platform 非依存検証

MAUI facade の結果経路 (レジストリ解決・completed / cancelled の型変換) は、platform 実装なしで検証可能である SHALL (maui/ADR-0002 の gateway seam による)。

#### Scenario: fake 実装で結果経路をユニットテストできる
- **GIVEN** Bridge の fake 実装が completed(結果値) または cancelled を返すよう構成されている
- **WHEN** 素の net10.0 ターゲットのユニットテストから show を呼び出す
- **THEN** 呼び出し元は正しい型・値の結果を受け取り、シミュレータなしでテストが完了する
