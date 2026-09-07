# ios-native デルタスペック

iOS Native (Swift) 形態での dialog-contract の貫通。契約の意味論は [dialog-contract](../dialog-contract/spec.md) に従う。

## ADDED Requirements

### Requirement: Swift 公開 API での貫通

iOS Native は Swift の `async throws` 関数として show を公開し、enum の型付き結果を返す SHALL (throw は構成エラー用チャネル)。レジストリ登録は Swift のメタタイプをキーとして受け付ける SHALL。

#### Scenario: Swift から show して結果を受け取る
- **GIVEN** Swift アプリが VM 型に View factory を登録している
- **WHEN** show を await で呼び出し、表示されたダイアログを完了操作で閉じる
- **THEN** await の戻り値として completed(結果値) が正しい型で得られる

### Requirement: KMP 委譲向け互換面の提供

iOS Native の show・レジストリ操作は、KMP facade からの委譲呼び出しが可能な `@objc` 互換面を持つ SHALL (kmp/ADR-0002 の委譲先)。互換面は非ジェネリックな型消去輸送表現 (completed の値 + completed / cancelled / error の判別) で受け渡し、内部用として公開 API と分離される SHALL (design Decision 12)。結果の exactly-once 保証は Native 側 DialogNotifier が持ち、互換面はそれを素通しする SHALL。

#### Scenario: ObjC 互換面経由の呼び出しが同一レジストリに到達する
- **GIVEN** `@objc` 互換面経由で VM キーと View factory を登録している
- **WHEN** 同じキーで show の委譲呼び出しを行う
- **THEN** 登録済み factory が解決され、Swift 直接利用と同じ挙動でダイアログが表示される

#### Scenario: 互換面経由でも結果は1回だけ届く
- **GIVEN** 互換面経由の show でダイアログが表示され、結果が確定している
- **WHEN** 続けて完了またはキャンセルの報告が発生する
- **THEN** 互換面の completion は追加で呼ばれず、最初の結果のみが届いている
