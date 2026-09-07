# kmp-facade デルタ (add-presentation-behavior)

## ADDED Requirements

### Requirement: Kotlin 経路の呼び出し元キャンセル追随 (iOS gateway)

KMP 共有コードからの suspend show (iOS gateway 経由) は、呼び出し元コルーチンのキャンセル時に機械面の show ハンドルを通じて当該ダイアログだけを閉じる (SHALL)。呼び出し元はコルーチン規約どおり `CancellationException` を観察し、内部の結果は cancelled で確定する (結果通知契約 基本ルール4・kmp/ADR-0005 の意味論)。

#### Scenario: [PB-KC-01] コルーチンキャンセルでダイアログが閉じ、呼び出し元は CancellationException を観察する
- **GIVEN** KMP 共有コードから iOS 上で show を開始し表示中
- **WHEN** show を待つコルーチンをキャンセルする
- **THEN** 当該ダイアログは閉じ、他の表示中ダイアログには影響せず、呼び出し元には `CancellationException` が伝播する

#### Scenario: [PB-KC-02] 提示開始前のキャンセルを取りこぼさない
- **GIVEN** KMP 共有コードから show を開始した直後 (提示完了前)
- **WHEN** コルーチンをキャンセルする
- **THEN** presentation / dismissal フックとも実行されず、ダイアログは表示されないか表示された場合も演出なしで直ちに閉じ、呼び出し元には `CancellationException` が伝播する

### Requirement: KMP 登録コンテンツへのトランジション添付

KMP 経由で登録・表示されるコンテンツ (Swift 向け型付き面・Kotlin 経路とも) でも、Native 側で添付した DialogTransition が機能する (SHALL)。KMP 共有コードの公開 API は増やさない (kmp/ADR-0002)。Swift 向け面からの show は、呼び出し元 Task のキャンセル時に `.cancelled` を返す既存挙動 (kmp/ADR-0005) を維持する。

#### Scenario: [PB-KC-03] KMP 登録コンテンツの添付が機能する
- **GIVEN** KMP の登録面で登録したコンテンツに Native 側で DialogTransition を添付する
- **WHEN** KMP 共有コードから show で表示する
- **THEN** presentation フックが呼ばれ、閉鎖時は dismissal フックの完了後に結果が配送される
