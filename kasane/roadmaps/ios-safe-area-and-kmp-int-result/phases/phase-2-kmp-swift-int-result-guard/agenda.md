# KMP の Swift 面で Int の結果型を防ぐ

Dialog の KMP 向け Swift 面で、結果型に `Int` / `UInt` を指定するとコンパイル時の警告が `Int32` / `Int64` (`UInt` なら `UInt32` / `UInt64`) を案内し、show が型付きの失敗になるようにする。

## 論点

(なし。2026-10-20 にオーナーの判断 (委任状「保留の判断」) で保留を解消し、残った論点を自律決定した)

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

保留 (ksn-autopilot 2026-10-20) は、オーナーの判断 (2026-10-20、委任状「保留の判断」) で解消した: 案 A を採り、`UInt` も同じ扱いにし、完了条件 6 を「警告 + show 時の型付きの失敗」に書き直した。負のコンパイル検査と `kasane/handbook/cross/test-execution.md` の表への追記は使わない。

## 決定事項

- Dialog の KMP 向け Swift 面で、結果型に `Int` / `UInt` を渡す指定には deprecated の専用オーバーロードで英語の警告を出し、`Int32` / `Int64` (`UInt` なら `UInt32` / `UInt64`) を案内する。show は表示せずに `KsDialogsKmpError` の型付きの失敗になる。`register` は throw しないまま登録し、登録の時点で異常終了させない (2026-10-20 オーナー判断、委任状「保留の判断」。当初の「コンパイルエラーにする」はこれで置き換えた)
- 確かめ方: 警告文はビルドのログに出たことを change の `evidence/` に記録し、実行時の失敗はテストで確かめる。負のコンパイル検査 (`#if KSDIALOGS_NEGATIVE_CHECK_*`) と `kasane/handbook/cross/test-execution.md` の表への追記は使わない (2026-10-20 オーナー判断、委任状「保留の判断」)
- kmp/ADR-0004 の方向の内側の変更で、可逆で局所的なため ADR にしない
- `Int` / `UInt` を結果型にしていたコードの show は型付きの失敗になる。`main` に入れる PR の `## Changes` に `- breaking:` として書く (英文の案は change に残す — 委任状の方針)
- 蒸留時に反映: `kasane/concepts/kmp/api/ios-host-integration.md` (と必要なら `dialog-surface.md`) — Kotlin と Swift の結果型の対応 (`Int` ↔ `Int32`、`Long` ↔ `Int64`、`UInt` ↔ `UInt32`、`ULong` ↔ `UInt64`。Swift の `Int` / `UInt` を指定すると警告が出て show が失敗する)
- 蒸留時に反映: 同じ文書 — 結果型の食い違いが型付きの失敗になるのは Swift から show したときだけで、共有コードからの show では検出されないこと
- `skills/` への反映は実装に含めない (蒸留の後に docs-refresh で追従する)
- 自律決定 (ksn-autopilot、根拠: 優先順 2 — `kasane/handbook/cross/diagnostic-message-language.md`): 警告文・エラー文は英語で書く

### 対象の入口

自律決定 (ksn-autopilot、根拠: 優先順 1 — 委任状の方針と agenda の論点)。`ios/Sources/KsDialogs/Kmp/KsDialogsKmp.swift` の、`result:` を取る `register` 4 種・`notifier(for:result:)`・`show(_:result:placement:)` の 6 つ。`result:` を省略する真偽値の入口は対象外 (`Int` を指定する余地が無い)。6 つの入口それぞれに `Int` 用と `UInt` 用の deprecated のオーバーロードを置く (12 個)。

### UInt も同じ扱いにする

2026-10-20 オーナー判断 (委任状「保留の判断」)。Kotlin の `UInt` / `ULong` は Swift では `UInt32` / `UInt64` になるので、Swift の `UInt` も同じ取り違えを起こす。警告で `UInt32` / `UInt64` を案内し、show を型付きの失敗にする。前の担当の「弾くのは Int だけ」(承認の範囲を越えるため申し送り) はこの判断で置き換えた。

### 失敗の形

自律決定 (ksn-autopilot、根拠: 優先順 1 — 委任状「保留の判断」の「結果型の食い違い相当」と、優先順 2 — kmp/ADR-0004 の「公開エラー型の case 集合を広げない」)。新しい case を足さず、既存の `KsDialogsKmpError.resultTypeMismatch(expected:actual:)` を使う。`expected` には使うべき型 (`"Int32 or Int64"` / `"UInt32 or UInt64"`)、`actual` には指定された型 (`"Int"` / `"UInt"`) を入れ、エラー文 (`The result value type does not match (expected: Int32 or Int64 / actual: Int).`) がそのまま案内になるようにする。

### show の時点で失敗にする範囲

自律決定 (ksn-autopilot、根拠: 優先順 1 — 委任状「保留の判断」の「ジェネリック経路を含めて show の時点で」「register は throw しないまま」と、優先順 4 — 触るファイルが 1 つで済む)。

| 入口 | 失敗にする条件 | 起きること |
|---|---|---|
| `show(_:result:placement:)` (省略形の `show(_:placement:)` を含む) | 指定した結果型が `Int` / `UInt`、または共有 VM の型に登録された結果型が `Int` / `UInt` | 表示せず (View factory を呼ばず)、その場で throw する |
| `notifier(for:result:)` | 指定した結果型が `Int` / `UInt` | 表示中かどうかや登録の結果型との比較より先に throw する |
| `register` 4 種 | (失敗にしない) | 今までどおり登録する。その型の show が上の行で失敗する |

show では指定した結果型を先に判定し、登録された結果型は show を呼んだ時点の登録で判定する (提案の相方レビューの指摘で足した。show のあと表示までの間の再登録は既存の結果復元時の照合に任せる)。ジェネリックなラッパー越しの呼び出し (deprecated のオーバーロードを通らない) も同じ判定に掛かる。共有コードからの show (互換面 `KsDialogsInteropBridge` を通る経路) は変えない — 完了条件 7 が「共有コードからの show では検出されない」と記述することを前提にしており、互換面は Kotlin とつなぐ ABI なので触る範囲を広げない。

### 既存テストの書き換え

自律決定 (ksn-autopilot、根拠: 優先順 4 — 触るファイルが少なく、テストの意図を保つ)。`ios/Tests/KsDialogsTests/KsDialogsKmpModelBindingTests.swift` の `[MB-KM-04]` で `result: Int.self` を使う 2 か所は `Int64` に置き換え、期待するエラーの型名の文字列 (`"Int"`) も `"Int64"` に合わせる。「show を始めた時点の結果型 (`String`) と、あとから登録した別の結果型の食い違い」を確かめる意図は、別の型であれば保たれる。

## TODO
- [x] 論点の解消 (2026-10-20 オーナー判断で解消)
- [x] ksn-propose で変更提案を起こす (`kasane/changes/guard-kmp-swift-int-result-type/`)

## 実装結果 (2026-10-20 反映)

`kasane/changes/archive/2026-10-20-guard-kmp-swift-int-result-type/` (M 級、review-002 APPROVED・verify-002 VALID)。決定事項どおり、`result:` を取る 6 入口に `Int` 用・`UInt` 用の deprecated のオーバーロード 12 個を置き、show と `notifier(for:result:)` を `resultTypeMismatch` の失敗にした。共有コードからの show は変えていない。deviation は 2 件で、phase-1 由来の「UIView と SwiftUI で観察できる挙動は同じ」の注記 (蒸留で `kasane/concepts/kmp/api/ios-host-integration.md` に反映済み) と、proposal.md の Impact の互換の主張を実証した範囲に絞った書き換え。`main` に入れる PR の `- breaking:` の英文の案は archive 先の proposal.md の Impact にある。ios/Sources の公開 doc コメントに残る ADR ID は、実装中に `kasane/changes/strip-adr-ids-from-ios-public-docs/` へ簡易起票した。

申し送り:
- review-002 の任意の Suggestion (deprecated の 12 入口が public のままであることを、非 `@testable` の検査ファイルで押さえる) — 見送り: 既定のビルドに deprecation 警告が恒常的に増える代償があり、今の 12 個は `public` と明記されている (フェーズ担当の判断、蒸留担当も同じ判断で閉じる)
- `skills/` と README への追従 — 見送り: このロードマップの非ゴール (roadmap.md「非ゴール」。蒸留の後に docs-refresh で追従する)
