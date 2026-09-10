---
scope: spec-review
kind: pain
severity: normal
count: 2
first-seen: 2026-08-27
last-seen: 2026-09-10
evidence:
  - add-sample-capture-automation (ADR cross/0010 起票時、撮影スクリプトの座標タップが CLI から注入できることを前提に「単一入口スクリプト + 宣言的操作列」を採用したが、実装フェーズで simctl 単体に入力注入がなく、従来手順メモの「シミュレータ操作ツール」はエージェント専用 MCP でスクリプトから呼べないと判明。外部ツール導入・XCUITest ランナー同梱はオーナー判断で不採用となり、スクリプト部分を取り下げ (proposal 2026-08-27 改訂))
  - add-release-workflow (相方スペックレビューの Major を取り込む際、maui-nuget-distribution の Scenario に「binding 2 件の nupkg には `.xml` が無い」と、csproj の `GenerateDocumentationFile=false` で SDK の生成を止められる前提を確かめずに書いた。実装で .NET Android の binding 用 targets が csproj の後に `DocumentationFile` を設定し直すと判明し、Android binding では満たせない。オーナー判断で受け入れ deviation 記録 (2026-09-10))
---

## ルール文

探索・提案の Decision が外部ツール・CLI 手段・撮影/操作手段の存在を前提にするとき、その手段が実行環境から実際に呼び出せることを最小プローブ (コマンドの存在とヘルプ・当該サブコマンドの受理確認) で確かめてから Decision に固定する。エージェント専用ツール (MCP) はスクリプト・CI から呼べない前提で数える。確かめられない手段に依存する部分は Decision に入れず、proposal の Open Questions か Non-Goals に置く。

## 経緯

- 2026-08-27 add-sample-capture-automation: 昇格済み L-001 (複数形態に共通の公開契約の実現経路プローブ) と同じ型だが、対象が公開契約ではなくリポジトリツールの決定だったため L-001 のスコープ外で発火しなかった。ツール前提の決定にも同じ「実現経路の最小プローブ」が要る、として別パターンで捕捉 (オーナー確定)
- 2026-09-10 add-release-workflow: 対象が外部ツールではなく SDK 設定の効き方 (csproj の指定が binding 用 targets に上書きされる) だったが型は同じ — spec の Scenario が toolchain の挙動を前提にするなら、1 回 pack して確かめてから固定する。翻案元 KsSettingsView は同じ `.xml` を証跡に記録しつつ spec には書いていなかったので、翻案元の証跡を読んでいれば防げた (lessons spec-review L-003 の適用範囲)
