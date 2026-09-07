# maui-binding デルタ (add-maui-ios-bridge-verification)

MAUI iOS の互換面 (bridge) を「直したあとに確かめる」ための契約。ID 領域は `BV` (bridge verification)、形態は MA (MAUI)。BV-MA-01〜03 (および 07) は bridge の自動テストが受け止め、テスト名に ID を含めて scenario-id-coverage の必須網羅にする (除外しない)。除外 ID として登録するのは BV-MA-04〜05 (ビルドの追随というビルド成果物の観察) と BV-MA-06 (テスト標的そのものの実行可能性) だけで、これらは `verification/` の実測記録で受け入れる。

## ADDED Requirements

### Requirement: 中身なしの供給は互換面の失敗として呼び出し側へ届く

MAUI 側から供給された中身が nil だったとき、互換面は表示を試みず、その提示 1 回を互換面の失敗 (`contentUnavailable`) として扱う (SHALL — core/ADR-0033 の iOS 側)。届き方は面ごとの契約に従う: Dialog は閉鎖の通知にエラーとしてちょうど 1 回、Loading は表示形 (`show`) とスコープ形 (`start`) の両入口とも完了の通知に失敗の理由を載せて 1 回 (スコープ形では MAUI 側の処理も実行されない)、Toast は受理を失敗させず、その 1 枚だけを破棄して他の表示に影響させない。この経路は bridge の自動テストで固定され、Sample の実機観測だけに依存しない (SHALL)。

#### Scenario: [BV-MA-01] Dialog の中身なし供給
- **GIVEN** 提示先が確保できる状態の Dialog 互換面
- **WHEN** 中身の供給関数が nil を返す提示を要求する
- **THEN** ダイアログは提示されず、閉鎖の通知が互換面の失敗 (`contentUnavailable`) としてちょうど 1 回届く

#### Scenario: [BV-MA-02] Loading (表示形) の中身なし供給
- **GIVEN** 提示先が確保できる状態の Loading 互換面
- **WHEN** 中身の供給関数が nil を返す表示形の開始 (`show`) を要求する
- **THEN** Loading は表示されず、完了の通知に互換面の失敗 (`contentUnavailable`) が理由として載って 1 回届く

#### Scenario: [BV-MA-07] Loading (スコープ形) の中身なし供給
- **GIVEN** 提示先が確保できる状態の Loading 互換面
- **WHEN** 中身の供給関数が nil を返すスコープ形の開始 (`start`) を、MAUI 側の処理を添えて要求する
- **THEN** Loading は表示されず、MAUI 側の処理は実行されず、完了の通知に互換面の失敗 (`contentUnavailable`) が理由として載って 1 回届く

#### Scenario: [BV-MA-03] Toast の中身なし供給
- **GIVEN** 提示先が確保できる状態の Toast 互換面で、別の Toast が表示されている
- **WHEN** 中身の供給関数が nil を返す表示を要求する
- **THEN** 受理は失敗せず、その 1 枚だけが表示されずに破棄され、表示中の別の Toast は影響を受けない

### Requirement: bridge の更新は Sample の iOS ビルドに追随する

bridge の Swift ソースだけを変更して Sample の iOS をインクリメンタルビルドしたとき、配備される .app のネイティブ実行ファイルは変更後の bridge を含む (SHALL)。bridge に変更が無いインクリメンタルビルドでは、binding の資源パッケージ再生成と Sample のネイティブリンクは従来どおりスキップされる (SHALL — 毎回の強制再生成にしない)。この追随はモノレポの ProjectReference 構成のための手当てであり、NuGet 経由の利用者の構成には現れない。

#### Scenario: [BV-MA-04] bridge 変更後のインクリメンタルビルドで新しいバイナリが配備される
- **GIVEN** Sample の iOS を一度ビルド済みで、その後 bridge の Swift ソースだけを (観察可能なシンボルが変わる形で) 変更した
- **WHEN** 成果物を捨てずに Sample の iOS をインクリメンタルビルドする
- **THEN** binding の資源パッケージが再生成され、Sample のネイティブリンクが再実行され、できた .app の実行ファイルに変更後のシンボルが含まれる

#### Scenario: [BV-MA-05] bridge 未変更のインクリメンタルビルドは再リンクしない
- **GIVEN** Sample の iOS を一度ビルド済みで、bridge にも Sample にも変更が無い
- **WHEN** Sample の iOS をインクリメンタルビルドする
- **THEN** binding の資源パッケージ再生成と Sample のネイティブリンクは実行されず (up-to-date でスキップ)、ビルドは成功する

### Requirement: bridge のテスト標的は ios/ と同じ方式で全件実行できる

bridge の Xcode プロジェクトは Simulator 上で走るテスト標的を持ち、ios/ と同じく `xcodebuild test -scheme <scheme> -destination 'platform=iOS Simulator,...'` の 1 コマンドで全件を実行でき、実行件数が出力から読み取れる (SHALL)。テスト名は Scenario ID を含み、scenario-id-coverage の走査対象に入る (SHALL)。

#### Scenario: [BV-MA-06] テスト標的の全件実行
- **GIVEN** bridge の Xcode プロジェクトと利用可能な iOS Simulator
- **WHEN** scheme を指定して `xcodebuild test` を実行する
- **THEN** BV-MA-01〜03・07 のテストを含む全件が実行され、出力から実行件数と失敗数が読み取れる
