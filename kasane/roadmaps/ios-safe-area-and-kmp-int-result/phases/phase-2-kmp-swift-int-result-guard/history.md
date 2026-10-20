# History: phase-2-kmp-swift-int-result-guard

## 2026-10-20: 「使えない指定」の実現方法 (未決・停止報告)
フェーズ担当 (ksn-autopilot) が Swift 6.4 で実験した ([artifacts/swift-int-overload-experiment.md](artifacts/swift-int-overload-experiment.md))。`Int` 専用のオーバーロードに `@available(*, unavailable, message:)` を付けても、使えるジェネリック版が選ばれてエラーにならない (同一ファイル・別モジュールとも)。型検査器が使用不可の宣言への参照に罰点を付け、それが具体性の比較より先に効くため。obsoleted・プラットフォーム限定・将来版限定・使用不可の conformance・ジェネリック版の disfavored も同じで、message 付きで選ばれるのは deprecated (警告) だけだった。

相談役 (ksn-advise、出典: advisor (counterpart / codex)) も同じ結論: 負の制約 (`Result != Int`) は Swift に無く、公開コンパイラの順位付けでも固定スコアの比較が具体性の比較より先に行われる。次善は deprecated の警告 + 実行時の拒否だが、完了条件 6 の「コンパイルエラー」を満たさない。見落としとして 2 点が挙がった: ジェネリックなラッパー越しに呼ぶと警告も曖昧さも迂回されるので、実行時の拒否はジェネリック経路にも要ること。`register` は throw しないので、実行時の拒否の形 (異常終了・`throws` 化・失敗を保持して後で報告) を別に決める必要があること。

検討した案: (a) 結果型をプロトコルに絞る — `Int` 以外の任意の型 (Swift から show するときの struct・クラス) にも準拠が要り、承認された破壊的変更の範囲を越える。(b) 警告 + 実行時の拒否 — 完了条件 6 を満たさない。(c) 曖昧さのエラー — コンパイルエラーにはなるが Int32 / Int64 を案内せず、公開面に意味のないオーバーロードが増える。どれも委任状 (完了条件の変更不可・任せない判断「公開 API の破壊的変更」) の範囲で決められないため、停止報告にした。

## 2026-10-20: 対象の入口・UInt・エラー文の言語・既存テストの書き換え
自律決定 (ksn-autopilot)。対象は `result:` を取る 6 つの入口 (register 4 種・`notifier(for:result:)`・`show(_:result:placement:)`)。`UInt` も Kotlin の `UInt` / `ULong` (Swift では `UInt32` / `UInt64`) との取り違えを起こすが、弾くと承認された破壊的変更 (`Int` だけ) の範囲を越えるのでロードマップへ申し送る。エラー文は handbook の規約で英語。`[MB-KM-04]` の `Int.self` 2 か所は `Int64` に置き換え、食い違いを確かめる意図を保つ。

## 2026-10-20: 保留の解消 (オーナー判断) と残った論点の自律決定
オーナーが停止報告の案 A〜D から案 A を選んだ (委任状「保留の判断」)。`Int` / `UInt` を指定すると deprecated の専用オーバーロードが `Int32` / `Int64` (`UInt32` / `UInt64`) を案内する英語の警告を出し、show はジェネリック経路を含めて `KsDialogsKmpError` の型付きの失敗になる。`register` は throw しないまま。`UInt` も同じ扱いにする (前の担当の申し送りへの答え)。完了条件 6 は書き直され、負のコンパイル検査と test-execution.md の表への追記は使わないことになった。

再開したフェーズ担当 (ksn-autopilot) が、残った設計を委任状の判断の優先順で決めた。失敗の形は新しい case を足さず `resultTypeMismatch` を使い、`expected` に使うべき型 (`Int32 or Int64`)・`actual` に指定された型 (`Int`) を入れてエラー文を案内にする (kmp/ADR-0004 の case 集合を広げない決定に従う)。show は、指定した結果型か共有 VM の型に登録された結果型が `Int` / `UInt` なら表示せずに throw する。`notifier(for:result:)` も `Int` / `UInt` の指定を他の判定より先に失敗にする。共有コードからの show (互換面) は変えない (完了条件 7 の「検出されない」の記述と、互換面が Kotlin とつなぐ ABI であることから)。`[MB-KM-04]` の `Int.self` を `Int64` に置き換える決定は残す。
