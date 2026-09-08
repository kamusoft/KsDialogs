# MAUI の 3 テストルートの全件実行 (tasks 6.3)

Requirement「MAUI Sample の両 OS 通しと MAUI テストルートの完了判定」/ Scenario「3 テストルートの全件実行」の証跡。
実行日: 2026-09-08。本変更の全実装 (グループ 1〜5) が済んだ状態で、絞り込みなしの全件実行を 3 ルートとも行った (handbook `cross/test-execution.md`)。

## 結果

| ルート | コマンド | 実行件数 | 失敗 |
|---|---|---|---|
| facade | `cd maui && dotnet test` | 160 tests | 0 |
| Android 互換面 | `cd maui/android/native && ./gradlew :ksdialogs-maui-bridge:test --rerun-tasks` | 34 tests | 0 |
| iOS 互換面 | `cd maui/macios/native && xcodebuild test -project KsDialogsMauiBridge.xcodeproj -scheme KsDialogsMauiBridge -destination 'platform=iOS Simulator,name=iPhone 17'` | 7 tests / 4 suites (Swift Testing)、XCTest 側は `Executed 0 tests` | 0 |

出力の抜粋は [test-routes.txt](test-routes.txt) (`scripts/log-sanitize.py` を通過、置換対象なし)。

## 件数の内訳と期待値との突き合わせ

- **facade 160 件**: 変更前 155 件 (handbook `cross/test-execution.md` の 2026-09-07 実測) + 本変更で追加した 5 件 = 160。期待値どおり
- **Android 互換面 34 件**: 変更前 31 件 + 本変更で追加した MB-MA-15 の 2 件 + レビュー指摘 (second-opinion-code-001) で追加した DM-MA-04 の 2 件目 (Dialog / Loading の文言完全一致) 1 件 = 34。初回計測 (修正サイクル前) は 33 件で、修正サイクル後に再計測して更新 (2026-09-08)。件数は Gradle のコンソール出力には出ないため、`ksdialogs-maui-bridge/build/test-results/testDebugUnitTest/*.xml` の `tests` / `failures` / `errors` / `skipped` 属性を合算して得た (tests=34 / failures+errors=0 / skipped=0)
- **iOS 互換面 7 件**: 本変更では互換面 (Swift) にテストを追加していないため、変更前と同じ 7 件。実測して記録した

## 実行時の注意 (実測メモ)

- `dotnet test` は Native 2 実装 (Xcode / Gradle) のビルドを巻き込む。今回も Gradle の `:ksdialogs-maui-bridge:assembleRelease` などが同じログに現れる
- binding の生成時に `BG8A00` 警告が 8 件出るが、これは deviation.md に記録済みの合意事項 (`.Companion` 入れ子型を `attr visibility=private`、static フィールドのみ `remove-node` にしたハイブリッドの副作用) であり、テストの失敗ではない
- iOS 互換面の 1 回目の実行は `Failed to prepare device 'iPhone 17' for impending launch. (Invalid connectionUUID specified.)` で `TEST FAILED` になった。テストの失敗ではなくシミュレータ準備の失敗で、直前まで別のシミュレータを起動・終了していたことが原因と見られる。`xcrun simctl shutdown all` を実行してから再実行したところ 7 件すべて成功した。上表は再実行の結果
