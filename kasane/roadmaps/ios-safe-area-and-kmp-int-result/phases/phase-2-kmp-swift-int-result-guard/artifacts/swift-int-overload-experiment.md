# 実験: Int 専用の使用不可オーバーロードはジェネリック版より選ばれるか

2026-10-20、フェーズ担当 (ksn-autopilot) が実施。Apple Swift 6.4 (swiftlang-6.4.0.34.1)、`swiftc -typecheck`。同一ファイルと、別モジュール (`-emit-module` したライブラリを `import` するクライアント) の両方で確かめた。

## 問い

`result: R.Type` を取るジェネリックなメソッドに、`result: Int.Type` を取る使用不可の印付きオーバーロードを足したとき、呼び出し側の `result: Int.self` は印付きの方に解決されて、印の message 付きのコンパイルエラーになるか。

## 基本形

```swift
final class Surface {
    func register<V: AnyObject, R: Sendable>(_ v: V.Type, result: R.Type, factory: @escaping (V, Notifier<R>) -> Int) {}
    @available(*, unavailable, message: "Use Int32 for Kotlin Int, Int64 for Kotlin Long")
    func register<V: AnyObject>(_ v: V.Type, result: Int.Type, factory: @escaping (V, Notifier<Int>) -> Int) { fatalError() }
}
Surface().register(VM.self, result: Int.self) { _, _ in 0 }
```

結果: **診断 0 件でコンパイルが通る** (同一ファイル・別モジュールとも)。`async throws` の `show` 形でも同じ。型検査器は使用不可の宣言を参照する解に罰点を付け、その比較が「具体型のほうが特殊」という比較より先に効くため、使えるジェネリック版が選ばれる。

## 変種と結果

| 変種 | 結果 |
|---|---|
| 使用不可の印 + ジェネリック版に `@_disfavoredOverload` | 診断なし (ジェネリック版が選ばれる) |
| `@available(swift, obsoleted: 1.0, message:)` | 診断なし |
| `@available(macOS, unavailable, message:)` (プラットフォーム限定) | 診断なし |
| `@available(macOS, introduced: 9999, message:)` (将来版でのみ使える)。ジェネリック版を disfavored にしても | 診断なし |
| `protocol P: Sendable` + `@available(*, unavailable) extension Int: P` + `<R: P>` 版 | 診断なし |
| 同上で conformance を `@available(macOS 9999, *)` | 診断なし |
| Int 版に `@available(*, deprecated, message:)` | Int 版が選ばれ、message 付きの**警告** (エラーではない) |
| Int 版 (async) に `@_unavailableFromAsync(message:)` | 宣言そのものがエラー (async 関数には付けられない) |
| Int 版 (同期) に `@_unavailableFromAsync(message:)` を async 文脈から呼ぶ | 選ばれるが、Swift 5 言語モードでは警告、Swift 6 で初めてエラー。同期文脈からの呼び出しは素通り |
| 既定値つきの別ラベル引数で区別した Int 版を 2 つ | `ambiguous use of '<名前>'` の**エラー**。ただし診断は候補の列挙だけで、Int32 / Int64 を案内しない |

## 結論

ジェネリック版が `Int` で使える限り、使用不可の印 (unavailable / obsoleted / プラットフォーム限定 / 将来版限定 / 使用不可の conformance) はどれもジェネリック版に負け、エラーにならない。message 付きで選ばれるのは deprecated (警告) だけである。Swift には「`Int` 以外」を表す負の制約が無いので、ジェネリック版を `Int` でだけ使えなくすることもできない。
