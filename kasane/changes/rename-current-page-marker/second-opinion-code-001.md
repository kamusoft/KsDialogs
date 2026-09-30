# セカンドオピニオン: rename-current-page-marker (code-001)
**相方**: codex / **label**: so-code-rename-current-page-marker / **日付**: 2026-09-30 / **対象**: 077da71 (HEAD) に対する作業ツリーの未コミット差分 (改名先の未追跡ファイル android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/compose/DialogCurrentPageModifier.kt を含む)
---
# レビュー結果: rename-current-page-marker

**判定: CHANGES_REQUESTED**（Major 1 件）

公開 API、診断文言、Compose の inspector 名、テスト、Sample の呼び出しは新名に揃っています。`ios/`・`android/`・`samples/` の変更 23 ファイルを HEAD と照合し、改名先ファイルを含めて旧名から新名への置換だけであること、各ソースに旧名が残らないことを確認しました。ビルド・テストは依頼に従い再実行していません。

## 照合した規約

ソースコメント規約（常時）、テスト実行規約（完了判定）、Sample パリティ規約（Sample 変更）、診断文言の言語規約（文言変更）、ローカル開発環境の準備（Sample ビルド）。

## 指摘事項

### 🟠 Major — Android の全件実行に必要な API 29 の結果がない

**該当箇所**: [evidence/test-results.md:3](kasane/changes/rename-current-page-marker/evidence/test-results.md:3)、[tasks.md:17](kasane/changes/rename-current-page-marker/tasks.md:17)

**問題点**: instrumented テストの証跡は API 36 の 1 台分で、`PB_SB_04` は skip されています。[テスト実行規約](kasane/handbook/cross/test-execution.md:84) は、Android の「全件実行」を判定するには API 29 と API 30 以上をそれぞれ実行するよう定めています。変更対象の Compose テスト 7 件が成功したことは確認できますが、tasks 2.4 の全件実行は現証跡だけでは完了と判定できません。

**推奨修正**: API 29 でも `connectedDebugAndroidTest` を実行し、両モジュールの件数・失敗・skip と `PB_SB_04` の結果を evidence に追加してください。API 36 で skip された機構が API 29 で実行されたことを確認できれば、この指摘は解消します。

**アクションプラン**: API 29 の結果を補った後、tasks 2.4 の完了判定を更新する。コード自体への修正指摘はありません。

## 突き合わせ結果

突き合わせの相手はホスト側の ksn-reviewer (review-001.md: APPROVED / verify-001.md: VALID、指摘 0 件)。

| # | 指摘 | 採否 | 理由 |
|---|---|---|---|
| 1 | Android の全件実行に API 29 の結果がない (handbook の test-execution.md は API 29 と API 30 以上を 1 台ずつ回すと定める) | 降格 | 相方のみ。API 29 以下 (minSdk 24〜29) は実測の対象から外すオーナー判断 (2026-09-26) がある。加えて本変更は Compose の印の名前・診断文言・コメントの置き換えだけで API レベルで分かれる経路に触れず、API 36 で skip されたのは本変更と無関係の既知の `PB_SB_04` だけ。デルタスペックの Scenario (改名前に列挙した `ComposeCurrentPageTests` 7 件が欠けずに成功) は API 36 の結果で満たされる。コードへの修正指摘はない |

確定 0 件 / 採用 0 件 / 降格 1 件 / 未解決 0 件。修正サイクルは回さない。
所見: handbook の test-execution.md の「全件実行は API 29 と API 30 以上を 1 台ずつ」は、API 29 以下を実測対象から外すオーナー判断と食い違ったまま残っている。
