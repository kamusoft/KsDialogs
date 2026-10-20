# Proposal: guard-kmp-swift-int-result-type

## Why

Dialog の KMP 向け Swift 面 (`Dialog.shared.kmp`) は、結果型を `result:` 引数で受け取る。共有コードの ViewModel が Kotlin の `Int` / `Long` / `UInt` / `ULong` を結果型にしているとき、Swift から見える型は `Int32` / `Int64` / `UInt32` / `UInt64` だが、Swift で自然に書く `Int.self` / `UInt.self` もそのままコンパイルが通る。食い違いに気づくのは、結果が共有コード側で Kotlin の型へ戻されるとき (`ClassCastException`) で、ColorAnalyzer の KMP 移植で実際に踏んだ (`../ColorAnalyzer/kasane/roadmaps/kmp-migration/autopilot-log.md`)。

Swift には、ジェネリックな入口を保ったまま `Int` だけを案内付きのコンパイルエラーにする仕組みが無い (`kasane/roadmaps/ios-safe-area-and-kmp-int-result/phases/phase-2-kmp-swift-int-result-guard/artifacts/swift-int-overload-experiment.md`)。オーナーの判断 (委任状 `kasane/roadmaps/ios-safe-area-and-kmp-int-result/charter.md`「保留の判断」) で、コンパイル時の警告による案内と、show 時点の型付きの失敗の組み合わせにする。

## What Changes

- `result:` を取る 6 つの入口 (`register` 4 種・`notifier(for:result:)`・`show(_:result:placement:)`) に、結果型が `Int` 用と `UInt` 用の deprecated のオーバーロードを足す (12 個)。呼び出しはコンパイルでき、英語の警告が `Int32` / `Int64` (`UInt32` / `UInt64`) を案内する。オーバーロードの中身は今のジェネリックな入口と同じに働く
- show は、指定した結果型か、共有 VM の型に登録された結果型が `Int` / `UInt` なら、表示せずに `KsDialogsKmpError.resultTypeMismatch` を throw する。`expected` に使うべき型 (`"Int32 or Int64"` / `"UInt32 or UInt64"`)、`actual` に指定された型 (`"Int"` / `"UInt"`) が入る
- `notifier(for:result:)` は、`Int` / `UInt` の指定を表示中かどうか・登録の結果型との比較より先に、同じ失敗にする
- `register` は throw しないまま、今までどおり登録する
- 判定はジェネリックなラッパー越しの呼び出し (deprecated のオーバーロードを通らない) にも掛かる
- 既存テスト `[MB-KM-04]` の `Int.self` を `Int64.self` に置き換える (意図は「別の結果型との食い違い」で、型が別なら保たれる)

影響する能力: kmp-facade

- 蒸留時に反映: concepts `kasane/concepts/kmp/api/ios-host-integration.md` (と必要なら `dialog-surface.md`) — Kotlin と Swift の結果型の対応 (`Int` ↔ `Int32`、`Long` ↔ `Int64`、`UInt` ↔ `UInt32`、`ULong` ↔ `UInt64`)。Swift の `Int` / `UInt` を指定すると deprecated の警告が出て、show と報告口の取り出しが `resultTypeMismatch` で失敗すること。「失敗の型」の表の `resultTypeMismatch` の行に、この場合を足す
- 蒸留時に反映: 同じ文書 — 結果型の食い違いが型付きの失敗になるのは Swift から show したときだけで、共有コードからの show では検出されないこと

## Non-Goals

- 共有コードからの show (互換面 `KsDialogsInteropBridge` を通る経路) の判定は変えない — 互換面は Kotlin とつなぐ ABI で、ここを変えるのは別の設計判断を要する。完了条件 7 もこの経路では検出されないことを記述する前提にしている
- `register` を throw させる・登録の時点で異常終了させる — オーナーの判断で採らない (委任状「保留の判断」)
- 案内付きのコンパイルエラー・負のコンパイル検査 (`#if KSDIALOGS_NEGATIVE_CHECK_*`)・`kasane/handbook/cross/test-execution.md` の表への追記 — オーナーの判断で使わない (Swift に仕組みが無いため)
- `Loading.shared.kmp` / `Toast.shared.kmp` — 結果型を取る入口が無い (別の能力の面でもある)
- `skills/` と README の更新 — 蒸留の後に docs-refresh で追従する (委任状の禁止事項)
- `samples/` の変更 — ロードマップの非ゴール。`samples/` に `Int.self` / `UInt.self` を結果型にしている箇所は無い (2026-10-20 確認)

## Impact

- 破壊的変更あり (委任状の方針で承認済み)。`Int` / `UInt` を結果型にしていた Swift のコードは、コンパイル時に警告が出て、show と `notifier(for:result:)` が型付きの失敗になる。それ以外の結果型の利用者には影響しない
- 公開エラー型の case は増えない (kmp/ADR-0004 の「case 集合を広げない」に従う)
- 公開面に deprecated のオーバーロードが 12 個増える。ObjC には出ない (Swift のジェネリックな面だけ)
- リスク: 同名オーバーロードの増加で、既存の呼び出し (結果型を省略した真偽値の入口、`String` などの結果型) の解決が変わらないこと。正の検査 (既存のテストのビルド) で確かめる

`main` に入れる PR の `## Changes` に書く案 (英文):

```
- breaking: On iOS, `Dialog.shared.kmp` no longer accepts Swift `Int` or `UInt` as a result type. Passing `Int.self` or `UInt.self` to `result:` of `register`, `notifier(for:result:)`, or `show(_:result:placement:)` now produces a deprecation warning, and `show` and `notifier(for:result:)` throw `KsDialogsKmpError.resultTypeMismatch` without presenting the dialog. Use `Int32` / `Int64` for Kotlin `Int` / `Long`, and `UInt32` / `UInt64` for Kotlin `UInt` / `ULong`.
```

## 級: M

1 能力 (kmp-facade) の中での公開 API の小変更 (オーバーロードの追加と、特定の結果型での show の失敗化)。ADR を新設しない (kmp/ADR-0004 の方向の内側)。

domain: kmp
roadmap: ios-safe-area-and-kmp-int-result/phase-2-kmp-swift-int-result-guard
