---
scope: spec-review
kind: pain
severity: normal
count: 4
first-seen: 2026-08-18
last-seen: 2026-08-27
evidence:
  - add-loading (design Decision 4 当初案が Android の Loading 器を「Activity decorView への直貼り」と確定したが、既存の Dialog 器 `DialogContainer` は `android.app.Dialog` = Activity と別の Window に表示されるため、decorView の子はその背面になり「ダイアログより手前・背後の操作遮断」を満たせない。相方スペックレビュー second-opinion-spec-001 の Critical C1 が提案段階で検出し、専用の全画面透過 Window 方式へ再設計。既存コードの表示階層 (どの Window に出るか) との突き合わせを design 確定前に行っていれば自力で検出できた)
  - add-layout-spec (Group 5 KMP facade が spec 不備で実装停止。design Decision 6「属性は VM 契約のオプションメンバ・VM 経由は既存輸送で成立する唯一の案」が KMP では二重に不成立 — Kotlin の expect interface は既定実装を持てず既存 VM 互換 Scenario と両立する公開形が存在しない (言語制約)、iOS Native の読み取り点は純 Swift protocol キャスト1箇所で ObjC interop 面にレイアウト型が露出しておらず Kotlin/Native から原理的に到達不能 (配管不在)。相方スペックレビュー 18指摘を通過した後の実装フェーズで発覚)
  - add-presentation-behavior (design Decision 3 が iOS プリセット factory の完全シグネチャを `static func fade(duration:easing: any UITimingCurveProvider = .standard)` (isolation 注釈なし) で確定したが、Swift 6 言語モードでは `UICubicTimingParameters.init(animationCurve:)` が MainActor 隔離のため nonisolated な既定引数として書けず (コンパイルエラー、回避すると恒久警告)、実装フェーズで factory を `@MainActor` にする乖離が発生。相方スペックレビュー4ラウンド通過後の発覚。既定引数のコンパイルプローブ1本で提案時に検出できた)
  - add-sample-capture-automation (design Decision 1 が異常系「同じキーが複数回現れたら最初の1組を採用」を両 OS 共通と確定したが、Android の Intent extra は `--es demo a --es demo b` が adb / Bundle 段階で 1 値 (後勝ち) に畳まれ、アプリ側から重複を観測できず「最初の1組」が原理的に実装不能。実装フェーズで発覚し deviation 記録へ。Intent extra の重複挙動は adb 実行1回で提案時に確認できた)
  - add-sample-capture-automation (同一 change 内の別事例、count 据え置き: design が capture-tooling の「待ち→タップ→撮影を1コマンドで (CA-CT-01)」を6アプリ共通と確定したが、iOS シミュレータへ座標タップを CLI から注入する手段の特定を行っておらず、config.yaml の従来メモが挙げる「シミュレータ操作ツール」はエージェント専用 MCP で Python スクリプトから呼べない。simctl 単体に入力注入は無く、実装フェーズで停止。オーナーが「なぜ設計段階で分からなかったのか」と指摘。タップ手段の実在確認は simctl help の一読で提案時に検出できた)
  - add-presentation-behavior (同一 change 内の別事例、count 据え置き: design Decision 5-1「配送は dismissal 完了 + 覆いの消滅 + 撤去の後」が MAUI 経路でどう実現されるか (ブリッジの閉鎖通知 = 撤去後の合図を C# が待つ) を Decision 8 が繋いでおらず、両 PlatformDialogGateway が旧来のラッチ待ち (`ResultChannel.Result`) のまま残った。ユニットテスト (PB-MA-02) は継ぎ目の完了変換だけを見ていて素通りし、Sample 通しの連写で発覚。iOS binding の `Action<UIView, Action>` が ObjC block を C 関数ポインタとして生成し SIGSEGV する件も同根 — 「ブリッジで block を受ける」という実現経路の型表現を design が確認していなかった)
---

## ルール文

複数形態 (プラットフォーム・言語境界) に共通の公開契約を design の Decision として確定するとき、形態ごとに「その契約を言語機構と既存 interop 面で実現する経路」を特定して Decision の根拠に書く (既存配管なら該当ファイル・API 名の明示、新規機構なら最小プローブのコンパイル確認)。実現経路を示せない形態がある場合、その形態の Requirement/Scenario を spec に入れず、設計論点として proposal の Open Questions か Non-Goals に置く。

## 経緯

- 2026-08-18 add-layout-spec: Decision 6 は「既存の VM 輸送経路がそのままパススルー経路になる」を採用理由にしたが、これは iOS/Android Native と MAUI (専用 ObjC 互換面を新設して成立) では真で、KMP では偽だった。expect interface の既定実装不可は Kotlin 言語仕様、iOS interop 面の型露出は生成ヘッダの実測で確認できる事実で、いずれも提案フェーズにコンパイルプローブ1本で検証可能だった。結果として実装フェーズで停止し、ADR 級の再設計判断がオーナーに差し戻された
- 2026-08-21 add-presentation-behavior: Decision 3 は「api-surface-check の期待値を一意にするため完全シグネチャを固定する」を狙いにしたが、固定したシグネチャ自体が Swift 6 の actor 隔離規則で成立しなかった。完全シグネチャを design に書くときは、その形のダミー宣言 (既定引数を含む) をターゲットの言語モードでコンパイルして通ることを確認してから確定する
- 2026-08-25 add-loading: 実現経路の不成立が言語機構ではなく**既存実装の表示階層** (既存 Dialog が別 Window に出る事実) にあった事例。今回は実装フェーズ前の相方スペックレビューが検出したため手戻りは提案改訂で済んだが、パターンとしては同型 — 「その OS でその配置が要件 (手前に出る) を満たすか」の突き合わせを design が行っていなかった

## 昇格の記録

- 2026-08-26: pain 閾値 3 到達、オーナー承認により `lessons/spec-review.md` の L-001 へ昇格 (add-loading の蒸留時)。本ファイルは経緯の保存用
