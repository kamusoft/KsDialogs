# android-native デルタスペック

Android Native (Kotlin) 形態での dialog-contract の貫通。契約の意味論は [dialog-contract](../dialog-contract/spec.md) に従う。

## ADDED Requirements

### Requirement: Kotlin 公開 API での貫通

Android Native は Kotlin の suspend 関数として show を公開し、sealed class の型付き結果を返す SHALL。レジストリ登録は Kotlin のクラス参照をキーとして受け付ける SHALL。

#### Scenario: Kotlin から show して結果を受け取る
- **GIVEN** Android アプリが VM 型に View factory を登録している
- **WHEN** show を suspend 呼び出しし、表示されたダイアログを完了操作で閉じる
- **THEN** 戻り値として completed(結果値) が正しい型で得られる

### Requirement: 戻るボタンによるキャンセル (通常時)

キーボードが表示されていない通常時、Android の戻るボタン操作は表示中の手前のダイアログに対するキャンセル操作として扱われる SHALL ([結果通知のルール](../../../../concepts/core/api/result-notification-semantics.md) のキャンセル操作一覧)。キーボード表示中の戻るボタンの扱い (移植元は無視) は本変更では規定せず、調査ケース MD-d として実挙動を記録する (期待値の確定は記録を見て行う)。

#### Scenario: 戻るボタンで cancelled が返る (キーボード非表示時)
- **GIVEN** キーボードが表示されていない状態でダイアログが表示されている
- **WHEN** 戻るボタンを押す
- **THEN** show の呼び出しは cancelled で完了する
