# KMP の Swift 面で Int の結果型を防ぐ

Dialog の KMP 向け Swift 面で、結果型に `Int` を指定するとコンパイルエラーになるようにし、`Int32` / `Int64` を案内する。

## 論点
- 「使えない指定」の実現方法: `Int` 専用のオーバーロードに使用不可の印を付ける形などで、Swift がジェネリック版よりそちらを選ぶかを実験で確かめる
- 対象の入口: `ios/Sources/KsDialogs/Kmp/KsDialogsKmp.swift` の、`result:` を取る `register` 4 種・`notifier(for:result:)`・`show(_:result:placement:)`
- `UInt` など、ほかに踏みやすい型があるか
- エラー文の文言 (ios/Sources の診断文言は英語固定の規約に従う見込み)
- 確かめ方: 負のコンパイル検査 (`#if KSDIALOGS_NEGATIVE_CHECK_*` と `build-for-testing`) の追加と、`kasane/handbook/cross/test-execution.md` の表への追記
- 既存テスト `ios/Tests/KsDialogsTests/KsDialogsKmpModelBindingTests.swift` の、`result: Int.self` を使う 2 か所の書き換え (結果型の食い違いを確かめる意図を保つ)

## 決定事項
- Dialog の KMP 向け Swift 面で、結果型に `Int` を渡す指定をコンパイルエラーにする。エラー文では、Kotlin の `Int` には `Int32`、`Long` には `Int64` を使うよう案内する
- kmp/ADR-0004 の方向の内側の変更で、可逆で局所的なため ADR にしない
- `Long` の結果を `Int` で返していたコードはコンパイルできなくなる。`main` に入れる PR の `## Changes` に `- breaking:` として書く
- 蒸留時に反映: `kasane/concepts/kmp/api/ios-host-integration.md` (と必要なら `dialog-surface.md`) — Kotlin と Swift の結果型の対応 (`Int` ↔ `Int32`、`Long` ↔ `Int64`。`Int` は指定できない)
- 蒸留時に反映: 同じ文書 — 結果型の食い違いが型付きの失敗になるのは Swift から show したときだけで、共有コードからの show では検出されないこと
- `skills/` への反映は実装に含めない (蒸留の後に docs-refresh で追従する)

## TODO
- [ ] 論点の解消
- [ ] ksn-propose で変更提案を起こす
