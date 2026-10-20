# KMP の Swift 面で Int の結果型を防ぐ

Dialog の KMP 向け Swift 面で、結果型に `Int` を指定するとコンパイルエラーになるようにし、`Int32` / `Int64` を案内する。

## 論点
- 「使えない指定」の実現方法 (オーナーの判断待ち。下の小節)
- 確かめ方: 負のコンパイル検査 (`#if KSDIALOGS_NEGATIVE_CHECK_*` と `build-for-testing`) の追加と、`kasane/handbook/cross/test-execution.md` の表への追記。実現方法が決まるまで決めない (警告に落とすなら負のコンパイル検査は使えない)

### 実現方法の調べた結果 (2026-10-20)

実験 ([artifacts/swift-int-overload-experiment.md](artifacts/swift-int-overload-experiment.md)) で、`Int` 専用のオーバーロードに使用不可の印を付けても、使えるジェネリック版が選ばれてエラーにならないと分かった。印の変種 (obsoleted・プラットフォーム限定・将来版限定・使用不可の conformance・ジェネリック版の disfavored) もすべて同じで、message 付きで選ばれるのは deprecated の警告だけだった。

相談役 (ksn-advise、counterpart) も、今の呼び出しの形と、任意の `Sendable` な結果型を受け入れる契約を保ったまま `Int` だけを案内付きのコンパイルエラーにする、サポートされた仕組みは無いと結論した。

残る案はどれも委任状の範囲で決められないため、停止報告でオーナーに判断を求めた。

| 案 | 満たせる範囲 | 委任状で決められない理由 |
|---|---|---|
| 警告 + 実行時の拒否 | `Int` 以外の型はそのまま通る | 完了条件 6 (コンパイルエラー) を満たさない |
| 結果型をプロトコルに絞る | `Int` をコンパイルエラーにできる | `Int` 以外の任意の型にも準拠が要り、承認された破壊的変更を越える |
| 曖昧さのエラー | `Int` をコンパイルエラーにできる | 診断が Int32 / Int64 を案内しない (完了条件 6) |

## 決定事項
- Dialog の KMP 向け Swift 面で、結果型に `Int` を渡す指定をコンパイルエラーにする。エラー文では、Kotlin の `Int` には `Int32`、`Long` には `Int64` を使うよう案内する
- kmp/ADR-0004 の方向の内側の変更で、可逆で局所的なため ADR にしない
- `Long` の結果を `Int` で返していたコードはコンパイルできなくなる。`main` に入れる PR の `## Changes` に `- breaking:` として書く
- 蒸留時に反映: `kasane/concepts/kmp/api/ios-host-integration.md` (と必要なら `dialog-surface.md`) — Kotlin と Swift の結果型の対応 (`Int` ↔ `Int32`、`Long` ↔ `Int64`。`Int` は指定できない)
- 蒸留時に反映: 同じ文書 — 結果型の食い違いが型付きの失敗になるのは Swift から show したときだけで、共有コードからの show では検出されないこと
- `skills/` への反映は実装に含めない (蒸留の後に docs-refresh で追従する)
- 自律決定 (ksn-autopilot、根拠: 優先順 2 — `kasane/handbook/cross/diagnostic-message-language.md`): 診断・エラー文は英語で書く

### 対象の入口

自律決定 (ksn-autopilot、根拠: 優先順 1 — 委任状の方針と agenda の論点)。`ios/Sources/KsDialogs/Kmp/KsDialogsKmp.swift` の、`result:` を取る `register` 4 種・`notifier(for:result:)`・`show(_:result:placement:)` の 6 つ。`result:` を省略する真偽値の入口は対象外 (`Int` を指定する余地が無い)。

### 弾くのは Int だけ

自律決定 (ksn-autopilot、根拠: 優先順 1 — 委任状の方針は `Int` の指定を弾く変更だけを承認済みの破壊的変更とし、それ以外の公開 API の破壊的変更は任せない判断)。Kotlin の `UInt` / `ULong` は Swift では `UInt32` / `UInt64` になるので、Swift の `UInt` も同じ取り違えを起こす。ただし `UInt` を弾くと承認の範囲を越えるため、このフェーズに入れずロードマップへの申し送りにする。

### 既存テストの書き換え

自律決定 (ksn-autopilot、根拠: 優先順 4 — 触るファイルが少なく、テストの意図を保つ)。`ios/Tests/KsDialogsTests/KsDialogsKmpModelBindingTests.swift` の `[MB-KM-04]` で `result: Int.self` を使う 2 か所は `Int64` に置き換え、期待するエラーの型名の文字列 (`"Int"`) も `"Int64"` に合わせる。「show を始めた時点の結果型 (`String`) と、あとから登録した別の結果型の食い違い」を確かめる意図は、別の型であれば保たれる。

## TODO
- [ ] 論点の解消 (実現方法はオーナーの判断待ち)
- [ ] ksn-propose で変更提案を起こす
