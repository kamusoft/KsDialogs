# Delta Spec: android-native (rename-current-page-marker)

対象: Compose の composable を表示中のページとして名乗らせる印の公開名。挙動 (台帳規則、登録済みの provider との優先順位、配置と離脱への追随) は変えない。

現行の宣言 (確認先):
- 印: `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/compose/KsDialogCurrentPage.kt` (`public fun Modifier.ksDialogCurrentPage(): Modifier`、パッケージ `jp.kamusoft.ksdialogs.compose`、座標 `jp.kamusoft:ksdialogs`)。同じファイルの要素がデバッグ用の表示名 (`InspectorInfo` の `name = "ksDialogCurrentPage"`) を持つ
- 印を名指す診断メッセージ: `android/ksdialogs-core/src/main/kotlin/jp/kamusoft/ksdialogs/DialogCurrentPageLedger.kt` (`DialogCurrentPageLookup.NotFound` の文言)
- 印を名指す説明コメント: `android/ksdialogs-core/src/main/kotlin/jp/kamusoft/ksdialogs/DialogCurrentPage.kt`・`DialogLayoutArea.kt`
- 公開面の正の検査: `android/api-surface-check/src/main/kotlin/jp/kamusoft/ksdialogs/apicheck/DialogCurrentPageApiSurfaceChecks.kt` (`./gradlew test` で `:api-surface-check:compileDebugKotlin` がコンパイルする)
- 挙動のテスト: `android/ksdialogs/src/androidTest/kotlin/jp/kamusoft/ksdialogs/compose/ComposeCurrentPageTests.kt` と `support/CurrentPageComposeTestActivity.kt` (instrumented)

## ADDED Requirements

### Requirement: 表示中のページの印の名前 (Android)

Android 実装は、Compose の composable を表示中のページとして名乗らせる `Modifier` の拡張関数を `markAsDialogCurrentPage()` の名前で公開すること (SHALL)。旧名 `ksDialogCurrentPage()` は deprecated 別名を含めて残さないこと (SHALL)。台帳規則、登録済みの provider との優先順位、配置と離脱への追随は改名前と変えないこと (SHALL)。ライブラリの診断メッセージ、説明コメント、デバッグ用の表示名が印を名指すときは新しい名前を使うこと (SHALL)。

#### Scenario: 新しい名前で印を付けられる
- **GIVEN** 利用者と同じ可視性の境界にある公開面の検証 (friend path を持たない `api-surface-check`)
- **WHEN** 利用者の `Modifier` に `markAsDialogCurrentPage()` を連ねるコードを `./gradlew test` でコンパイルする
- **THEN** コンパイルが通り、戻り値を他の `Modifier` と連ねられる

#### Scenario: 挙動のテストが名前の追随だけで通る
- **GIVEN** 改名前に列挙した、Compose の印を使う表示中のページのテスト (`ComposeCurrentPageTests`) のテスト名の一覧
- **WHEN** 改名後に `kasane/handbook/cross/test-execution.md` の android/ の手順で JVM テストと instrumented テストを実行する
- **THEN** すべて成功し、一覧のテストは 1 件も欠けずに成功として instrumented テストの結果に現れる

#### Scenario: 差分は旧名から新名への置き換えだけ
- **GIVEN** 改名前後の android/ (main・androidTest・api-surface-check)
- **WHEN** 差分を見比べる
- **THEN** 変わったのは、`ksDialogCurrentPage` を `markAsDialogCurrentPage` に置き換えただけの行と、印を定義するファイルの改名だけであり、削除だけの行は無い。台帳の診断メッセージ (`DialogCurrentPageLookup.NotFound` の文言) は改名前と同じ英文のまま `markAsDialogCurrentPage()` を名指し、デバッグ用の表示名は `markAsDialogCurrentPage` である

#### Scenario: 旧名が現行のソースに残らない
- **GIVEN** 改名後の android/ (main・test・androidTest・api-surface-check)
- **WHEN** `ksDialogCurrentPage` を大文字小文字を区別して検索する
- **THEN** 0 件である (診断メッセージ、説明コメント、デバッグ用の表示名を含む)
