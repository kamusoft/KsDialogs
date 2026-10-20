# History: phase-2-kmp-swift-int-result-guard

## 2026-10-20: 「使えない指定」の実現方法 (未決・停止報告)
フェーズ担当 (ksn-autopilot) が Swift 6.4 で実験した ([artifacts/swift-int-overload-experiment.md](artifacts/swift-int-overload-experiment.md))。`Int` 専用のオーバーロードに `@available(*, unavailable, message:)` を付けても、使えるジェネリック版が選ばれてエラーにならない (同一ファイル・別モジュールとも)。型検査器が使用不可の宣言への参照に罰点を付け、それが具体性の比較より先に効くため。obsoleted・プラットフォーム限定・将来版限定・使用不可の conformance・ジェネリック版の disfavored も同じで、message 付きで選ばれるのは deprecated (警告) だけだった。

相談役 (ksn-advise、出典: advisor (counterpart / codex)) も同じ結論: 負の制約 (`Result != Int`) は Swift に無く、公開コンパイラの順位付けでも固定スコアの比較が具体性の比較より先に行われる。次善は deprecated の警告 + 実行時の拒否だが、完了条件 6 の「コンパイルエラー」を満たさない。見落としとして 2 点が挙がった: ジェネリックなラッパー越しに呼ぶと警告も曖昧さも迂回されるので、実行時の拒否はジェネリック経路にも要ること。`register` は throw しないので、実行時の拒否の形 (異常終了・`throws` 化・失敗を保持して後で報告) を別に決める必要があること。

検討した案: (a) 結果型をプロトコルに絞る — `Int` 以外の任意の型 (Swift から show するときの struct・クラス) にも準拠が要り、承認された破壊的変更の範囲を越える。(b) 警告 + 実行時の拒否 — 完了条件 6 を満たさない。(c) 曖昧さのエラー — コンパイルエラーにはなるが Int32 / Int64 を案内せず、公開面に意味のないオーバーロードが増える。どれも委任状 (完了条件の変更不可・任せない判断「公開 API の破壊的変更」) の範囲で決められないため、停止報告にした。

## 2026-10-20: 対象の入口・UInt・エラー文の言語・既存テストの書き換え
自律決定 (ksn-autopilot)。対象は `result:` を取る 6 つの入口 (register 4 種・`notifier(for:result:)`・`show(_:result:placement:)`)。`UInt` も Kotlin の `UInt` / `ULong` (Swift では `UInt32` / `UInt64`) との取り違えを起こすが、弾くと承認された破壊的変更 (`Int` だけ) の範囲を越えるのでロードマップへ申し送る。エラー文は handbook の規約で英語。`[MB-KM-04]` の `Int.self` 2 か所は `Int64` に置き換え、食い違いを確かめる意図を保つ。
