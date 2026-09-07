# maui-binding デルタ (add-presentation-behavior)

## ADDED Requirements

### Requirement: トランジション添付面 (MAUI)

MAUI ライブラリは、添付プロパティ `Dialog.TransitionProperty` (`Dialog.SetTransition` / `GetTransition`、code-behind 供給) で DialogTransition を添付できる (SHALL)。フックは `Func<VisualElement, Task>` で UI スレッドで開始され、コンテンツの MAUI View が渡る。添付はブリッジの完了コールバック型の実行口を通じてネイティブへパススルーされ、演出の駆動と完了待ちはネイティブ側の契約 (dialog-contract) に従う。MAUI の `Task<T>` は CancellationToken を受けないため、呼び出し元キャンセルの経路は存在しない。公開 API の形は design Decision 3 の表に従う。

#### Scenario: [PB-MA-01] 添付プロパティでの添付がネイティブへパススルーされる
- **GIVEN** ContentView 派生のコンテンツに `Dialog.SetTransition` で DialogTransition を設定する
- **WHEN** show で表示する
- **THEN** presentation フックがコンテンツの MAUI View を引数に UI スレッドで呼ばれる

#### Scenario: [PB-MA-02] 退出フックの完了待ちが MAUI 経由でも成立する
- **GIVEN** 完了までの間を制御できる dismissal フックを添付した MAUI コンテンツを表示中
- **WHEN** 結果を報告して閉じる
- **THEN** フックの Task 完了後に show の Task が結果で完了する

#### Scenario: [PB-MA-03] フックの Task が fault しても結果は配送される
- **GIVEN** 例外で fault する dismissal フックを添付した MAUI コンテンツを表示中
- **WHEN** 結果を completed で報告する
- **THEN** 器は撤去され、show の Task は completed で完了する (faulted にならない)

#### Scenario: [PB-MA-04] フックの完了通知は多重に届かない
- **GIVEN** dismissal フックを添付した MAUI コンテンツを表示中
- **WHEN** 結果を報告し、フックの Task が完了する
- **THEN** ネイティブ側の完了口はちょうど1回だけ呼ばれる

### Requirement: プリセットの MAUI 表現

プリセット factory (Fade / Slide / Zoom / None) は `TimeSpan?` と `Easing?` で引数を受け取り、MAUI のアニメーション API で実装され、カスタムフックと同じブリッジ経路を通る (SHALL)。

#### Scenario: [PB-MA-05] プリセット添付がネイティブ経路で機能する
- **GIVEN** Slide プリセットを添付した MAUI コンテンツ
- **WHEN** show で表示し、閉じる
- **THEN** 表示・閉鎖ともプリセット経路で完了し、結果が配送される
