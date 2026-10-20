# KMP の Swift 面で Int の結果型を防ぐ

Dialog の KMP 向け Swift 面で、結果型に `Int` を指定するとコンパイルエラーになるようにし、`Int32` / `Int64` を案内する。

## 論点
- 保留 (ksn-autopilot 2026-10-20): `Int` を案内付きのコンパイルエラーにする仕組みが Swift に無い。完了条件 6 を改めるか、公開 API の破壊的変更を広げるかをオーナーが決める (下の小節の案 A〜D)
- 「使えない指定」の実現方法 (オーナーの判断待ち。下の小節)
- 確かめ方: 負のコンパイル検査 (`#if KSDIALOGS_NEGATIVE_CHECK_*` と `build-for-testing`) の追加と、`kasane/handbook/cross/test-execution.md` の表への追記。実現方法が決まるまで決めない (警告に落とすなら負のコンパイル検査は使えない)

### 実現方法の調べた結果 (2026-10-20)

実験 ([artifacts/swift-int-overload-experiment.md](artifacts/swift-int-overload-experiment.md)) で、`Int` 専用のオーバーロードに使用不可の印を付けても、使えるジェネリック版が選ばれてエラーにならないと分かった。印の変種 (obsoleted・プラットフォーム限定・将来版限定・使用不可の conformance・ジェネリック版の disfavored) もすべて同じで、message 付きで選ばれるのは deprecated の警告だけだった。

相談役 (ksn-advise、counterpart) も、今の呼び出しの形と、任意の `Sendable` な結果型を受け入れる契約を保ったまま `Int` だけを案内付きのコンパイルエラーにする、サポートされた仕組みは無いと結論した。

残る案はどれも委任状の範囲で決められないため、停止報告でオーナーに判断を求め、保留になった。

| 案 | 選んだときに起きること | 委任状で決められない理由 |
|---|---|---|
| A: 警告 + 実行時の拒否 | `Int` を指定すると Int32 / Int64 を案内する英語の警告が出て、実行すると失敗する。`Int` 以外の型の利用者には影響しない | 完了条件 6 (コンパイルエラー) を改める必要がある |
| B: 結果型をプロトコルに絞る | `Int` は案内付きのコンパイルエラーになる。Swift から show するときの独自の struct・クラスなど、`Int` 以外の型にも準拠を足す必要が出る | 承認された破壊的変更 (`Int` だけ) を越える (任せない判断) |
| C: 曖昧さのエラー | `Int` はコンパイルエラーになるが、診断は候補の一覧だけで Int32 / Int64 を案内しない。公開面に意味のないオーバーロードが増える | 完了条件 6 の後半 (案内する診断) を満たさない |
| D: フェーズを取り下げる | `Int` の指定は今までどおり通り、食い違いは Swift から show したときだけ実行時の型付きの失敗になる | 完了条件 6 と 7 の後半を外す必要がある |

推奨は A。`Int` 以外の利用者を壊さずに済む案はこれだけで、実行時の拒否はジェネリックなラッパー越しの呼び出しにも効く。拒否の形は、show の時点で型付きの失敗 (`KsDialogsKmpError.resultTypeMismatch` 相当) を投げる形を推す。`register` が throw しない今の契約を変えずに済むため。登録の時点で異常終了させる形もあるが、利用者のアプリを落とすので推さない。

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
