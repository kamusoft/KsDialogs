# kmp-facade デルタ (guard-kmp-swift-int-result-type)

対象は Dialog の KMP 向け Swift 面 (`Dialog.shared.kmp`) の、結果型を `result:` で受け取る 6 つの入口 (`register` 4 種・`notifier(for:result:)`・`show(_:result:placement:)`)。共有コードからの show (互換面) は変えない (proposal の Non-Goals)。

## ADDED Requirements

### Requirement: Swift の Int / UInt を結果型に指定したときの案内

6 つの入口は、結果型に Swift の `Int` / `UInt` を指定した呼び出しを、コンパイルエラーにせず、使うべき型を案内する英語の deprecation 警告付きで受け付ける (SHALL)。案内は、`Int` なら Kotlin の `Int` には `Int32`・`Long` には `Int64` を、`UInt` なら Kotlin の `UInt` には `UInt32`・`ULong` には `UInt64` を使うことを示す。警告の出る呼び出しの実行時の挙動は、同じ入口のジェネリックな呼び出しと同じである (`register` は登録し、`show` と `notifier(for:result:)` は下の Requirement のとおり失敗する)。

**Side Effects**:
- 共有の View factory レジストリ: 更新 (`register` の呼び出しが、ジェネリックな `register` と同じに共有 VM の型へ factory を登録する)

#### Scenario: [IR-KM-01] Int を指定した 6 入口の呼び出しは案内付きの警告でコンパイルできる
- **GIVEN** 6 つの入口それぞれを `result: Int.self` で呼ぶ Swift のコード
- **WHEN** そのコードをビルドする
- **THEN** ビルドは成功し、呼び出しごとに `Int32` (Kotlin `Int`) と `Int64` (Kotlin `Long`) を案内する英語の deprecation 警告がビルドのログに出る
- **AND** 実行すると `register` は失敗せずに登録し、`show` と `notifier(for:result:)` は型付きの失敗になる

#### Scenario: [IR-KM-02] UInt を指定した 6 入口の呼び出しは案内付きの警告でコンパイルできる
- **GIVEN** 6 つの入口それぞれを `result: UInt.self` で呼ぶ Swift のコード
- **WHEN** そのコードをビルドする
- **THEN** ビルドは成功し、呼び出しごとに `UInt32` (Kotlin `UInt`) と `UInt64` (Kotlin `ULong`) を案内する英語の deprecation 警告がビルドのログに出る
- **AND** 実行すると `register` は失敗せずに登録し、`show` と `notifier(for:result:)` は型付きの失敗になる

### Requirement: Int / UInt の結果型での show と報告口の取り出しは型付きの失敗になる

`show(_:result:placement:)` (結果型を省略した `show(_:placement:)` を含む) は、指定した結果型が `Int` / `UInt` のとき、または共有 VM の型に登録された結果型が `Int` / `UInt` のとき、ダイアログを表示せず (View factory を呼ばず) に `KsDialogsKmpError.resultTypeMismatch` を throw する (SHALL)。`notifier(for:result:)` は、指定した結果型が `Int` / `UInt` のとき、表示中かどうかと登録の結果型との比較より先に同じ失敗を throw する (SHALL)。どちらも、deprecated のオーバーロードを通らないジェネリックな呼び出しにも同じに掛かる。失敗の `expected` には使うべき型 (`Int` なら `"Int32 or Int64"`、`UInt` なら `"UInt32 or UInt64"`)、`actual` には判定に当たった型 (`"Int"` / `"UInt"`) が入る。show では指定した結果型を先に判定し、指定した結果型が `Int` / `UInt` でないときだけ登録された結果型を判定する (指定と登録の両方が当たれば、失敗の内容は指定した結果型で決まる)。登録された結果型は show を呼んだ時点の登録で判定する — show を呼んだあと表示までの間に同じ VM 型が登録し直された場合は、この判定ではなく既存の結果型の照合 (結果を復元する時点の型付きの失敗) に任せる。`register` はこの判定で失敗しない。

**Requires**:
- `show` に指定する結果型と、表示する共有 VM の型に登録された結果型が、Swift の `Int` / `UInt` でないこと
- `notifier(for:result:)` に指定する結果型が Swift の `Int` / `UInt` でないこと

**Side Effects**: なし

#### Scenario: [IR-KM-03] show に Int / UInt を指定すると表示せずに失敗する
- **GIVEN** 共有 VM の型を `Int64` (または真偽値) で登録した構成
- **WHEN** その VM を `result: Int.self` で show する (`UInt.self` でも同じ)
- **THEN** show は `KsDialogsKmpError.resultTypeMismatch(expected: "Int32 or Int64", actual: "Int")` (`UInt` なら `expected: "UInt32 or UInt64", actual: "UInt"`) を throw する
- **AND** ダイアログは表示されず、View factory は呼ばれない

#### Scenario: [IR-KM-04] Int / UInt で登録した共有 VM の show は、指定した結果型によらず表示せずに失敗する
- **GIVEN** 共有 VM の型を `result: Int.self` (または `UInt.self`) で登録した構成 (登録は失敗しない)
- **WHEN** その VM を、結果型を省略して、または `Int64` などの別の結果型を指定して show する
- **THEN** show は `KsDialogsKmpError.resultTypeMismatch(expected: "Int32 or Int64", actual: "Int")` (`UInt` なら `expected: "UInt32 or UInt64", actual: "UInt"`) を throw する
- **AND** ダイアログは表示されず、View factory は呼ばれない
- **AND** `Int` で登録した VM を `result: UInt.self` で show したときは、指定した結果型が先に判定されて `expected: "UInt32 or UInt64", actual: "UInt"` になる (`UInt` で登録して `Int.self` で show したときは `expected: "Int32 or Int64", actual: "Int"`)

#### Scenario: [IR-KM-05] ジェネリックなラッパー越しの Int / UInt の指定も同じに失敗する
- **GIVEN** 結果型を型引数で受け取り、そのまま `show(_:result:placement:)` / `notifier(for:result:)` へ渡す利用者側のジェネリックな関数
- **WHEN** その関数を結果型 `Int` (または `UInt`) で呼ぶ
- **THEN** show と `notifier(for:result:)` は、直接 `Int.self` (`UInt.self`) を指定したときと同じ `resultTypeMismatch` を throw し、show はダイアログを表示しない

#### Scenario: [IR-KM-06] notifier(for:result:) に Int / UInt を指定すると、表示中でも登録の型によらず失敗する
- **GIVEN** 共有 VM の型を `String` で登録して表示中の VM と、表示していない同じ型の VM
- **WHEN** どちらの VM にも `notifier(for:result: Int.self)` (または `UInt.self`) を呼ぶ
- **THEN** どちらも nil ではなく `resultTypeMismatch(expected: "Int32 or Int64", actual: "Int")` (`UInt` なら `expected: "UInt32 or UInt64", actual: "UInt"`) を throw する
- **AND** 表示中のダイアログは閉じず、登録した結果型 (`String`) で引いた報告口から結果を返せる

#### Scenario: [IR-KM-07] Kotlin の整数型に対応する Swift の型は今までどおり使える
- **GIVEN** 共有 VM の型を `Int32` / `Int64` / `UInt32` / `UInt64` のいずれかで登録した構成
- **WHEN** 同じ結果型を指定して show し、中身が報告口で値を返す
- **THEN** show は警告も失敗も無く、その値の completed で終わる
