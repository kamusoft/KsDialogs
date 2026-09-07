# Tasks: localize-dialog-error-messages

文言はすべて各 capability のデルタスペックの対応表 (現行 ja → 変更後 en) が唯一の源。タスクで新しい訳語を作らない。対応表に無い日本語リテラルを見つけたら、表に足すのではなく deviation.md に記録して報告する。

## 1. iOS Native (specs/ios-native)

- [x] 1.1 `DialogError` 7 case と `KsDialogsKmpError` 2 case の `errorDescription` を対応表の英語に置き換える (→ Requirement: iOS の失敗型メッセージは英語固定)
- [x] 1.2 `init?(coder:)` の `fatalError` 6 件と `Logger.warning` 11 件を対応表の英語に置き換える (→ Requirement: iOS の到達不能 init の文言と警告ログは英語固定)
- [x] 1.3 テスト追加: `[DM-IO-01]` 全 case の `errorDescription` 完全一致、`[DM-IO-02]` `KsDialogsKmpError` 2 case の完全一致 (ios/Tests、Swift Testing)
- [x] 1.4 `ios/Sources/` の日本語文字列リテラル grep が 0 件 (→ Scenario: DM-IO-03)

## 2. Android Native (specs/android-native)

- [x] 2.1 `DialogException` 5 サブクラスの message を対応表の英語に置き換える (→ Requirement: Android の失敗型メッセージは英語固定)
- [x] 2.2 `Log.w` 7 件を対応表の英語に置き換える (→ Requirement: Android の警告ログは英語固定)
- [x] 2.3 テスト追加: `[DM-AN-01]` 5 サブクラスの `message` 完全一致 (android/ksdialogs/src/test、JVM)
- [x] 2.4 `android/ksdialogs/src/main/` と `android/ksdialogs-compose/src/main/` の日本語文字列リテラル grep が 0 件 (→ Scenario: DM-AN-02)

## 3. KMP (specs/kmp-facade)

- [x] 3.1 `IosDialogGateway` / `IosLoadingGateway` の定数 3 本を対応表の英語に置き換える (→ Requirement: KMP iOS ホストの gateway 固有メッセージは英語固定)
- [x] 3.2 文言依存の既存テストを英語文言へ追随し ID を付与: `InteropBridgeContractTests` の互換面直検査 (部分一致 2 箇所) → `[DM-KM-01]`、同ファイルの既定エントリ show (提示先なし、部分一致 1 箇所) → `[DM-KM-03]`、`AndroidDialogGatewayContractTests` の完全一致 1 箇所 → `[DM-KM-02]` (既存テストの期待値だけを変える。構造は変えない) (→ Scenario: DM-KM-01 / DM-KM-02 / DM-KM-03)
- [x] 3.2b `InteropBridgeContractTests` の未登録 ViewModel の show テストに、共有 `DialogException` の `message` が `No View factory is registered for ViewModel type` を含む assertion を追加し `[DM-KM-03]` に含める (→ Scenario: DM-KM-03)
- [x] 3.3 `kmp/ksdialogs-kmp/src/{commonMain,androidMain,iosMain}/` の日本語文字列リテラル grep が 0 件 (→ Scenario: DM-KM-04)

## 4. MAUI (specs/maui-binding)

- [x] 4.1 `DialogException` 6 入れ子型の message を対応表の英語に置き換える (→ Requirement: MAUI の失敗型メッセージは英語固定)
- [x] 4.2 `ToastGateway` / `LoadingGateway` / `Platforms/{Android,iOS}/Platform{Dialog,Loading}Gateway` の例外文言 6 件を対応表の英語に置き換える (→ Requirement: MAUI の gateway と bridge が投げる例外文言は英語固定)
- [x] 4.3 `MauiDialogBridgeError` (iOS bridge) 2 case と `MauiToastBridge` (Android bridge) の `error()` を対応表の英語に置き換える (→ 同上)
- [x] 4.4 `DialogTransitionRunner` / `BridgeContentSupply` の警告書式と部品 5 件を対応表の英語に置き換える (→ Requirement: MAUI の警告ログは英語固定)
- [x] 4.5 テスト追加: `[DM-MA-01]` 6 入れ子型の `Message` 完全一致、`[DM-MA-02]` 既定の中身生成経路の `InvalidOperationException` 文言 (maui/KsDialogs.Maui.Tests)
- [x] 4.6 テスト追加: `[DM-MA-03]` `MauiDialogBridgeError` 2 case の `errorDescription` (maui/macios/native の bridge テスト)、`[DM-MA-04]` `MauiToastBridge` の `IllegalStateException` 文言 (maui/android/native の bridge テスト)
- [x] 4.7 `maui/KsDialogs.Maui/`・`maui/macios/native/KsDialogsMauiBridge/`・`maui/android/native/ksdialogs-maui-bridge/src/main/` の日本語文字列リテラル grep が 0 件 (→ Scenario: DM-MA-05)
- [x] 4.8 MAUI iOS 面のビルドが通ることを確認する (bridge の Swift 変更が binding に追随する。`kasane/handbook/cross/local-development-setup.md` の Xcode 整合手順に従う。環境都合で通せない場合は deviation に記録して報告)

## 5. 利用者向け Skills (specs/user-skills)

- [x] 5.1 `skills/ja/ksdialogs-{ios,android,maui,kmp}/references/{dialogs,loading,toast}.md` (12 ファイル) の診断表のメッセージ列と本文中の個別引用を対応表の英語に置き換え、型名のプレースホルダを `{TypeName}` にし、診断表の直後に「メッセージは現在の実装値で、安定 API ではない」の 1 文を添える (→ Requirement: Skills の診断表は実装の英語文言を引用する)
- [x] 5.2 `skills/en/` の同 12 ファイルを ja と同じ英語文言 (メッセージ列は byte 一致) と同じ 1 文 (英訳) にする (列構成・見出し・コードブロックは変えない。`kasane/handbook/cross/user-skill-writing-style.md` の ja → en 同期の到達状態) (→ 同上)
- [x] 5.3 `skills/` に対応表の現行 (ja) 文言が残らない grep が 0 件 (→ Scenario: Skills に日本語の例外メッセージの引用が残らない)
- [x] 5.4 docs-refresh の parity 検査 4 本と `scripts/local-path-lint.py` / `scripts/identity-lint.py` を通し差分ゼロ (→ Scenario: en / ja の parity 検査と lint を通る)。`skills/.manifest.json` は触らない

## 6. 完了判定 (`kasane/handbook/cross/test-execution.md`)

- [x] 6.1 全件実行と件数の記録: ios (Simulator) / android `./gradlew test --rerun-tasks` / android instrumented (1 台以上) / kmp `./gradlew allTests --rerun-tasks` / maui `dotnet test` / maui android bridge / maui macios bridge (android 系の Gradle は逐次)
- [x] 6.0 `scripts/scenario-id-coverage.py` の `DEFAULT_ALLOW_MISSING` に DM-IO-03 / DM-AN-02 / DM-KM-04 / DM-MA-05 を「ライブラリ本体に日本語の文字列リテラルが残らないことを静的 grep で受け入れる Scenario」の理由付きで登録する (先頭コメントの理由列挙にも追記)
- [x] 6.2 `python3 scripts/scenario-id-coverage.py` が終了コード 0 (DM-IO-01/02・DM-AN-01・DM-KM-01/02/03・DM-MA-01〜04 がテスト宣言に含まれ、除外登録した 4 ID は理由付きで除外される)
- [x] 6.3 `scripts/comment-policy-lint.py` を通す (触ったソースのコメントは日本語のまま。文言の置き換えで doc comment を英語化しない)
- [x] 6.4 ライブラリ本体全体の日本語文字列リテラル grep が 0 件であることを、実行コマンドと出力ごと `verification/` に記録する (DM-IO-03 / DM-AN-02 / DM-KM-04 / DM-MA-05 の受け入れ証跡)。コマンド:
  `grep -rn --include='*.swift' --include='*.kt' --include='*.cs' -E '"[^"]*[ぁ-んァ-ヶ一-龠][^"]*"' ios/Sources android/ksdialogs android/ksdialogs-compose kmp/ksdialogs-kmp/src maui/KsDialogs.Maui maui/macios/native maui/android/native | grep -v '/build/' | grep -v -E '^[^:]+:[0-9]+:\s*(//|///|\*|/\*)' | grep -v -E '/src/(test|androidTest|commonTest|iosTest|androidHostTest)/' | grep -v 'Tests/'` → 期待結果: 出力なし
