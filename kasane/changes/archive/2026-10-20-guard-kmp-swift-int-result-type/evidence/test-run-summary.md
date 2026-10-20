# 全件実行の証跡

2026-10-20 採取。Xcode 27.0 (27A266a) / Apple Swift 6.4 (swiftlang-6.4.0.34.1)。iOS Simulator (iPhone 17 Pro / iOS 26.0, `<udid>`)。ログの全文は手元保管で、以下は `scripts/log-sanitize.py` を通した (置換対象なし) 抜粋。

## ios/

コマンド (`ios/` で実行): `xcodebuild test -scheme KsDialogs -destination 'platform=iOS Simulator,id=<udid>' -collect-test-diagnostics never`

| 系統 | 件数 |
|---|---|
| Swift Testing | 379 tests / 62 suites / 全件 passed |
| XCTest | 0 tests (このルートのテストはすべて Swift Testing 製) |
| 合計 | 379 tests / 0 failures |

```
	 Executed 0 tests, with 0 failures (0 unexpected) in 0.000 (0.001) seconds
✔ Test run with 379 tests in 62 suites passed after 18.541 seconds.
** TEST SUCCEEDED **
```

この変更で足した・書き換えたテストの結果行:

```
✔ Test "[IR-KM-01] Int を指定した register 4 種は登録し、show と notifier(for:result:) は型付きの失敗になる" passed after 0.832 seconds.
✔ Test "[IR-KM-02] UInt を指定した register 4 種は登録し、show と notifier(for:result:) は型付きの失敗になる" passed after 0.116 seconds.
✔ Test "[IR-KM-03] show に Int / UInt を指定すると表示せずに失敗する" passed after 0.046 seconds.
✔ Test "[IR-KM-04] Int / UInt で登録した共有 VM の show は、指定した結果型によらず表示せずに失敗する" passed after 0.073 seconds.
✔ Test "[IR-KM-05] ジェネリックなラッパー越しの Int / UInt の指定も同じに失敗する" passed after 0.022 seconds.
✔ Test "[IR-KM-06] notifier(for:result:) に Int / UInt を指定すると、表示中でも登録の型によらず失敗する" passed after 0.543 seconds.
✔ Test "[IR-KM-07] Kotlin の整数型に対応する Swift の型は今までどおり使える" passed after 2.059 seconds.
✔ Test "[MB-KM-04] 登録時の結果型と違う型の指定は空ではなく型付きの失敗になる" passed after 0.149 seconds.
✔ Test "[MB-KM-04] 表示中の再登録は出ているダイアログの結果型判定を変えない" passed after 0.572 seconds.
```

IR-KM-01〜07 は `ios/Tests/KsDialogsTests/KsDialogsKmpKotlinIntegerResultTests.swift`、書き換えた `[MB-KM-04]` は `ios/Tests/KsDialogsTests/KsDialogsKmpModelBindingTests.swift`。deprecation 警告の行は [deprecation-warnings.md](deprecation-warnings.md)。

## kmp/

コマンド (`kmp/` で実行): `./gradlew allTests --rerun-tasks` → `BUILD SUCCESSFUL in 47s`

| ターゲット (`kmp/ksdialogs-kmp/build/test-results/<ターゲット>/TEST-*.xml`) | ファイル数 | tests | failures | errors | skipped |
|---|---|---|---|---|---|
| `iosSimulatorArm64Test` | 20 | 86 | 0 | 0 | 0 |
| `testAndroidHostTest` | 18 | 83 | 0 | 0 | 0 |
| 合計 | 38 | 169 | 0 | 0 | 0 |

この変更は kmp/ のコードに触れていない (Swift 面だけの変更)。回帰がないことの確認として回した。

## Scenario ID の網羅

コマンド (リポジトリルート): `python3 scripts/scenario-id-coverage.py`

```
  OK  IR-KM  7/7
  OK  MB-KM  4/4
  合計: 341/383 (除外 42 件)
結果: 未網羅なし
```

## 診断文言の静的 grep

`kasane/handbook/cross/diagnostic-message-language.md`「検査」の grep をリポジトリルートで実行し、出力が空 (grep の終了コード 1 = 一致なし) であることを確かめた。この変更で足した診断文言 (`@available` の message と `resultTypeMismatch` の `expected` / `actual`) は英語のみ。

## レビュアーの再実行 (review-001、2026-10-20)

独立レビュー (review-001.md) のレビュアーが同じ Simulator (`<udid>`) で再実行した。

| ルート | 結果 |
|---|---|
| ios/ | Swift Testing 379 tests / 62 suites passed、XCTest 0、`** TEST SUCCEEDED **` |
| maui/macios/native/ (ios/ のパッケージを取り込む) | 17 tests / 7 suites passed、`** TEST SUCCEEDED **` |

実行していないルート: android/・android/ (instrumented)・maui/ (`dotnet test`)・maui/android/native/。どれもこの change のファイル (ios/Sources/KsDialogs/Kmp/・ios/Tests/) をコンパイルしない。

## 統合ブランチ取り込み後の全件実行 (2026-10-20)

統合ブランチ (phase-1 の実装 `ios/Sources/KsDialogs/SwiftUI/`・`Layout/` を含む) を取り込んだあと、フェーズ担当が同じ Simulator (`<udid>`) で回した。衝突は無く、この change のファイルは変わっていない。

| ルート | 結果 |
|---|---|
| ios/ | Swift Testing 402 tests / 67 suites passed、XCTest `Executed 0 tests`、`** TEST SUCCEEDED **` |
| kmp/ (`allTests --rerun-tasks`) | BUILD SUCCESSFUL。iosSimulatorArm64Test 20 ファイル・86 tests、testAndroidHostTest 18 ファイル・83 tests、failures・errors・skipped は 0 |
| `scripts/scenario-id-coverage.py` | `IR-KM 7/7`・未網羅なし (合計 360/402、除外 42 件) |

この文面の deprecation 警告は、取り込み後も `ios/Tests/KsDialogsTests/KsDialogsKmpKotlinIntegerResultTests.swift` の直接呼び出しの行 (22 か所) にだけ出ている。
