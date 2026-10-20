# Tasks: guard-kmp-swift-int-result-type

対応の表記: (→ <Requirement 名の要点> / <Scenario ID>)。テストの実行手順は `kasane/handbook/cross/test-execution.md` に従い、Scenario ID はテストの宣言 (Swift Testing の表示名) に含める (core/ADR-0016)。

## 1. Swift 面の判定と deprecated のオーバーロード (`ios/Sources/KsDialogs/Kmp/`)

- [x] 1.1 `KsDialogsKmp.swift` に、結果型が `Int` / `UInt` かを判定し、`KsDialogsKmpError.resultTypeMismatch(expected: "Int32 or Int64", actual: "Int")` / `(expected: "UInt32 or UInt64", actual: "UInt")` を作る内部の判定を置く。判定は型の同一性で行う (`Int.self` / `UInt.self`。`DialogDeclaredResultType` と同じく型そのもので比べる) (→ show と報告口の取り出しは型付きの失敗 / IR-KM-03〜06)
- [x] 1.2 `show(_:result:placement:)` の先頭で、指定した結果型を 1.1 で判定し、当たらなければ共有 VM の型にその時点で登録されている factory の結果型 (`declaredResultType`) を判定し、当たれば表示を始めずに throw する。指定した結果型を先に判定する (両方当たれば指定した型の失敗)。登録が無い・`declaredResultType` が nil のときは今までどおり進める (→ 同上 / IR-KM-03・04)
- [x] 1.3 `notifier(for:result:)` の先頭で、指定した結果型を 1.1 で判定し、当たれば表示中かどうか・登録の結果型との比較より先に throw する (→ 同上 / IR-KM-06)
- [x] 1.4 6 つの入口それぞれに、結果型が `Int` 用と `UInt` 用の `@available(*, deprecated, message: "...")` のオーバーロードを足す (12 個)。署名は対応するジェネリックな入口の `Result` を `Int` / `UInt` に置き換えたもの (actor 分離・`async throws`・`@ViewBuilder`・`placement` の既定値を含めて揃える)。中身はジェネリックな入口と同じ処理へ委ねる。**同じ名前のジェネリックな入口を `result: Int.self` で呼び直すと、同じ deprecated のオーバーロードが選ばれて自分自身を呼ぶ** ので、委ねる先は private な実装 (ジェネリックな入口もそれを呼ぶ) にする。警告文は英語で、`Int` は Kotlin `Int` → `Int32`・`Long` → `Int64`、`UInt` は Kotlin `UInt` → `UInt32`・`ULong` → `UInt64` を案内し、show が `KsDialogsKmpError.resultTypeMismatch` で失敗することを添える (`kasane/handbook/cross/diagnostic-message-language.md`)。`@available` の message は文字列リテラルしか取れないので、`Int` 用 6 個・`UInt` 用 6 個にそれぞれ同じ文面を書く (→ Int / UInt を結果型に指定したときの案内 / IR-KM-01・02)
- [x] 1.5 doc コメント (日本語、`kasane/handbook/cross/comment-policy.md`) に、Kotlin の整数型と Swift の型の対応と、`Int` / `UInt` が警告と show の失敗になることを書く。`KsDialogsKmpError.resultTypeMismatch` の doc コメントにもこの場合を足す

## 2. テスト (`ios/Tests/KsDialogsTests/`)

- [x] 2.1 既存の `KsDialogsKmpModelBindingTests.swift` の `[MB-KM-04]` で `result: Int.self` を使う 2 か所 (登録と `notifier(for:result:)`) を `Int64.self` に置き換え、期待する `expected` / `actual` の `"Int"` を `"Int64"` に合わせる (→ 既存の意図の保持)
- [x] 2.2 IR-KM-01・02: 6 つの入口を `Int.self` / `UInt.self` で直接呼ぶテスト。直接呼ぶことでビルドのログに deprecation 警告が出る (警告の確認は 3.1 の証跡)。実行時は `register` 4 種が throw せずに登録されること (4 種それぞれの登録の直後に、レジストリの `declaredResultType` が `Int` / `UInt` であること、または結果型を省略した show (= `Bool` の指定) が `actual: "Int"` / `"UInt"` の失敗になること — 登録に由来する失敗 — で確かめる。`show(result: Int.self)` の失敗は登録の有無によらず起きるので、登録の確認には使わない)、`show` と `notifier(for:result:)` が `resultTypeMismatch` を throw することを見る (→ IR-KM-01・02)
- [x] 2.3 IR-KM-03・04: show が表示せずに失敗すること。View factory が呼ばれないことと、提示されたコンテナが 0 のままであることを見る。04 は結果型を省略した show と `Int64` を指定した show の両方と、`Int` 登録 × `UInt` 指定・`UInt` 登録 × `Int` 指定の組 (指定した結果型の失敗になる) (→ IR-KM-03・04)
- [x] 2.4 IR-KM-05: テストの中に結果型を型引数で受け取るジェネリックな関数を置き、`Int` / `UInt` で呼んで show と `notifier(for:result:)` の失敗を見る (deprecated のオーバーロードを通らない経路) (→ IR-KM-05)
- [x] 2.5 IR-KM-06: `String` で登録して表示中の VM と未表示の VM で `notifier(for:result: Int.self)` / `UInt.self` が失敗し、表示中のダイアログは `String` の報告口で completed にできること (→ IR-KM-06)
- [x] 2.6 IR-KM-07: `Int32` / `Int64` / `UInt32` / `UInt64` で登録・show して completed の値が返ること (→ IR-KM-07)

## 3. 証跡と全件実行

- [x] 3.1 `ios/` の全件テストのログから、2.2 で足した呼び出しに出た deprecation 警告の行 (12 入口分 = 6 入口 × `Int` / `UInt`) を抜き出し、`evidence/deprecation-warnings.md` に残す。ログのローカル絶対パスはリポジトリ相対に直す (`kasane/` 配下に絶対パスを書かない)。12 の入口すべてに警告が出たことを表で示す
- [x] 3.2 全件実行: `cd ios && xcodebuild test -scheme KsDialogs -destination 'platform=iOS Simulator,id=<レーンの Simulator>' -collect-test-diagnostics never`、`cd kmp && ./gradlew allTests --rerun-tasks`、`python3 scripts/scenario-id-coverage.py`。件数 (Swift Testing の `Test run with N tests`、kmp の `TEST-*.xml` の件数) と、IR-KM-01〜07 のテストが結果に現れていることを `evidence/test-run-summary.md` に残す。Simulator の識別子は書かない (`<udid>` と書く)
- [x] 3.3 診断文言の静的 grep (`kasane/handbook/cross/diagnostic-message-language.md`「検査」) の出力が空であることを確かめ、3.2 の証跡に添える
