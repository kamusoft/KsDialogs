# deprecation 警告の証跡 (IR-KM-01・02)

2026-10-20 採取。Xcode 27.0 (27A266a) / Apple Swift 6.4 (swiftlang-6.4.0.34.1)。iOS Simulator (iPhone 17 Pro / iOS 26.0, `<udid>`)。

採取元: `ios/` の全件テスト `xcodebuild test -scheme KsDialogs -destination 'platform=iOS Simulator,id=<udid>' -collect-test-diagnostics never` のビルドログ (全文は手元保管)。`scripts/log-sanitize.py` を通した (置換対象なし) うえで、`[#DeprecatedDeclaration]` の行のうち診断位置 (`<file>:<line>:<col>`) の付いた行を位置で重複除去して抜き出した。パスはリポジトリ相対 (`ios/` 起点) に直してある。

## 12 入口の対応表

位置はすべて `ios/Tests/KsDialogsTests/KsDialogsKmpKotlinIntegerResultTests.swift`。register の 4 種は警告の宣言名がどれも `register(_:result:factory:)` になるため、呼び出し側の factory の形 (引数の数・戻り値が `UIView` か SwiftUI の `View` か) で入口を特定している — その形と `Int.Type` / `UInt.Type` を受け付ける非推奨の入口は 1 つずつしかない。

| # | 入口 | 結果型 | 警告の出た位置 | 案内 |
|---|---|---|---|---|
| 1 | `register(_:result:factory:)` — `(ViewModel, DialogNotifier<Int>) -> UIView` | `Int` | `:41:13` | Int32 (Kotlin Int) / Int64 (Kotlin Long) |
| 2 | `register(_:result:factory:)` — `(ViewModel, DialogNotifier<Int>) -> some View` | `Int` | `:51:13` | 同上 |
| 3 | `register(_:result:factory:)` — `(ViewModel) -> UIView` | `Int` | `:60:13` | 同上 |
| 4 | `register(_:result:factory:)` — `(ViewModel) -> some View` | `Int` | `:69:13` | 同上 |
| 5 | `show(_:result:placement:)` | `Int` | `:79:31` | 同上 |
| 6 | `notifier(for:result:)` | `Int` | `:82:25` | 同上 |
| 7 | `register(_:result:factory:)` — `(ViewModel, DialogNotifier<UInt>) -> UIView` | `UInt` | `:93:13` | UInt32 (Kotlin UInt) / UInt64 (Kotlin ULong) |
| 8 | `register(_:result:factory:)` — `(ViewModel, DialogNotifier<UInt>) -> some View` | `UInt` | `:103:13` | 同上 |
| 9 | `register(_:result:factory:)` — `(ViewModel) -> UIView` | `UInt` | `:112:13` | 同上 |
| 10 | `register(_:result:factory:)` — `(ViewModel) -> some View` | `UInt` | `:121:13` | 同上 |
| 11 | `show(_:result:placement:)` | `UInt` | `:131:31` | 同上 |
| 12 | `notifier(for:result:)` | `UInt` | `:134:25` | 同上 |

12 入口すべてに警告が出た。ビルドは成功 (`** TEST SUCCEEDED **`)。

## 補足の観察

- 同じファイルの IR-KM-03・04・06 の直接呼び出し (`:152` `:155` `:163` `:166` `:179` `:190` `:193` `:204` `:283` `:286`) にも同じ文面の警告が出ている
- IR-KM-05 のジェネリックなラッパーの中 (型引数 `Result` を `result:` に渡す呼び出し) と、IR-KM-07 の `Int32` / `Int64` / `UInt32` / `UInt64` の呼び出しには警告が出ていない (非推奨の入口に解決されていない)
- この文面の deprecation 警告は、上のファイル以外 (既存テスト・`ios/Sources/`) には 1 件も出ていない。`[MB-KM-04]` は `Int64` に置き換えたため警告が出ない

## 抜粋 (12 入口分)

```
Tests/KsDialogsTests/KsDialogsKmpKotlinIntegerResultTests.swift:41:13: warning: 'register(_:result:factory:)' is deprecated: Swift Int does not correspond to a Kotlin integer type. Use Int32 for Kotlin Int or Int64 for Kotlin Long. show and notifier(for:result:) throw KsDialogsKmpError.resultTypeMismatch for Int. [#DeprecatedDeclaration]
Tests/KsDialogsTests/KsDialogsKmpKotlinIntegerResultTests.swift:51:13: warning: 'register(_:result:factory:)' is deprecated: Swift Int does not correspond to a Kotlin integer type. Use Int32 for Kotlin Int or Int64 for Kotlin Long. show and notifier(for:result:) throw KsDialogsKmpError.resultTypeMismatch for Int. [#DeprecatedDeclaration]
Tests/KsDialogsTests/KsDialogsKmpKotlinIntegerResultTests.swift:60:13: warning: 'register(_:result:factory:)' is deprecated: Swift Int does not correspond to a Kotlin integer type. Use Int32 for Kotlin Int or Int64 for Kotlin Long. show and notifier(for:result:) throw KsDialogsKmpError.resultTypeMismatch for Int. [#DeprecatedDeclaration]
Tests/KsDialogsTests/KsDialogsKmpKotlinIntegerResultTests.swift:69:13: warning: 'register(_:result:factory:)' is deprecated: Swift Int does not correspond to a Kotlin integer type. Use Int32 for Kotlin Int or Int64 for Kotlin Long. show and notifier(for:result:) throw KsDialogsKmpError.resultTypeMismatch for Int. [#DeprecatedDeclaration]
Tests/KsDialogsTests/KsDialogsKmpKotlinIntegerResultTests.swift:79:31: warning: 'show(_:result:placement:)' is deprecated: Swift Int does not correspond to a Kotlin integer type. Use Int32 for Kotlin Int or Int64 for Kotlin Long. show and notifier(for:result:) throw KsDialogsKmpError.resultTypeMismatch for Int. [#DeprecatedDeclaration]
Tests/KsDialogsTests/KsDialogsKmpKotlinIntegerResultTests.swift:82:25: warning: 'notifier(for:result:)' is deprecated: Swift Int does not correspond to a Kotlin integer type. Use Int32 for Kotlin Int or Int64 for Kotlin Long. show and notifier(for:result:) throw KsDialogsKmpError.resultTypeMismatch for Int. [#DeprecatedDeclaration]
Tests/KsDialogsTests/KsDialogsKmpKotlinIntegerResultTests.swift:93:13: warning: 'register(_:result:factory:)' is deprecated: Swift UInt does not correspond to a Kotlin integer type. Use UInt32 for Kotlin UInt or UInt64 for Kotlin ULong. show and notifier(for:result:) throw KsDialogsKmpError.resultTypeMismatch for UInt. [#DeprecatedDeclaration]
Tests/KsDialogsTests/KsDialogsKmpKotlinIntegerResultTests.swift:103:13: warning: 'register(_:result:factory:)' is deprecated: Swift UInt does not correspond to a Kotlin integer type. Use UInt32 for Kotlin UInt or UInt64 for Kotlin ULong. show and notifier(for:result:) throw KsDialogsKmpError.resultTypeMismatch for UInt. [#DeprecatedDeclaration]
Tests/KsDialogsTests/KsDialogsKmpKotlinIntegerResultTests.swift:112:13: warning: 'register(_:result:factory:)' is deprecated: Swift UInt does not correspond to a Kotlin integer type. Use UInt32 for Kotlin UInt or UInt64 for Kotlin ULong. show and notifier(for:result:) throw KsDialogsKmpError.resultTypeMismatch for UInt. [#DeprecatedDeclaration]
Tests/KsDialogsTests/KsDialogsKmpKotlinIntegerResultTests.swift:121:13: warning: 'register(_:result:factory:)' is deprecated: Swift UInt does not correspond to a Kotlin integer type. Use UInt32 for Kotlin UInt or UInt64 for Kotlin ULong. show and notifier(for:result:) throw KsDialogsKmpError.resultTypeMismatch for UInt. [#DeprecatedDeclaration]
Tests/KsDialogsTests/KsDialogsKmpKotlinIntegerResultTests.swift:131:31: warning: 'show(_:result:placement:)' is deprecated: Swift UInt does not correspond to a Kotlin integer type. Use UInt32 for Kotlin UInt or UInt64 for Kotlin ULong. show and notifier(for:result:) throw KsDialogsKmpError.resultTypeMismatch for UInt. [#DeprecatedDeclaration]
Tests/KsDialogsTests/KsDialogsKmpKotlinIntegerResultTests.swift:134:25: warning: 'notifier(for:result:)' is deprecated: Swift UInt does not correspond to a Kotlin integer type. Use UInt32 for Kotlin UInt or UInt64 for Kotlin ULong. show and notifier(for:result:) throw KsDialogsKmpError.resultTypeMismatch for UInt. [#DeprecatedDeclaration]
```
