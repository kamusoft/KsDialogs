# セカンドオピニオン: add-maui-nuget-distribution (code-001)
**相方**: codex / **label**: so-code-add-maui-nuget-distribution / **日付**: 2026-09-08 / **対象**: HEAD (c844740) に対する未コミットの作業ツリー全体 (maui/ / samples/maui / README 2 枚 / scripts/scenario-id-coverage.py / kasane/handbook/cross/local-development-setup.md / kasane/changes/add-maui-nuget-distribution/)
---
CHANGES_REQUESTED

## サマリー

配布構成、Android の managed/native 境界、`ViewCreationFailed` の伝播は仕様と整合しています。提示された全テスト成功結果も確認材料としました。

ただし、新しい失敗契約を固定するテストが診断文言規約を満たしていません。優先度の高い Minor のため修正を要求します。

件数: Critical 0 / Major 0 / Minor 2 / Suggestion 0

## 照合した規約

- `cross/comment-policy.md`（always）
- `cross/test-execution.md`
- `cross/diagnostic-message-language.md`
- `cross/runtime-behavior-verification.md`
- `cross/sample-parity.md`
- `cross/local-development-setup.md`
- `cross/ci-script-deletion.md`
- `kasane/lessons/impl.md`
- `kasane/lessons/process.md`
- ksn-core の delta-spec / paths / evidence 規約
- 関連する accepted ADR と MAUI/core concepts

## 指摘事項

### [🟡 Minor・優先度高] 新規失敗契約のテストが診断文言と元例外を固定していない

**該当箇所**: `maui/KsDialogs.Maui/Contract/DialogException.cs:59`  
**該当箇所**: `maui/KsDialogs.Maui.Tests/DiagnosticMessageTests.cs:17`  
**該当箇所**: `maui/KsDialogs.Maui.Tests/DialogDependencyInjectionTests.cs:126`  
**該当箇所**: `maui/KsDialogs.Maui.Tests/LoadingDependencyInjectionTests.cs:108`  
**該当箇所**: `maui/android/native/ksdialogs-maui-bridge/src/test/kotlin/jp/kamusoft/ksdialogs/maui/MauiDialogLoadingContentSupplyTests.kt:73`

**問題点**: `DiagnosticMessageTests` は「全入れ子型6種」としたまま、追加された7つ目の `ViewCreationFailed.Message` を完全一致で検査していません。また Kotlin 側の追加テストは `failed:` / `failure:` の接頭辞しか見ないため、Dialog / Loading の新しい英語文言が誤記されても成功します。これは `diagnostic-message-language.md` の失敗型ごとの完全一致検査に反します。

さらに MB-MA-11 は `InnerException` が非 null であることしか確認せず、DI の元例外が保持されたという Scenario を識別できません。

**推奨修正**:

- 既存の `DM_MA_01` に `ViewCreationFailed` の完全一致メッセージ、両型名、元例外の保持を追加する。
- Android bridge の診断テストで Dialog / Loading 両方のメッセージを完全一致で確認する。
- MB-MA-11 では `InnerException` の型・メッセージ、または投入した例外との同一性まで検査する。

### [🟡 Minor] Sample 証跡の14項目の内訳が計数と一致しない

**該当箇所**: `kasane/changes/add-maui-nuget-distribution/evidence/sample-walkthrough/README.md:88`

**問題点**: 「14件」の内訳として記載された `8 + 1 + 1 + 2 + 4` は16件になります。また `transition-dialog` と `layout-dialog` の起動直後はダイアログではなく、それぞれデモ画面と属性調整パネルです。個別表と画像は揃っていますが、判定サマリーが保存実体の分類と一致していません。

**推奨修正**: 起動直後の排他的な分類を、例えば「直接ダイアログ5件 / デモ画面1件 / 属性調整パネル1件 / Loading 2件 / Toast 4件 / overlap時系列1件 = 14件」と訂正してください。追加操作で観測したダイアログ数は別文に分けるのが明確です。

## アクションプラン

1. 新規3文言と `InnerException` の検出力を既存テストへ追加する。
2. Sample 証跡の内訳を訂正する。
3. facade 160件、Android互換面33件を再実行し、件数と失敗0を更新確認する。


## 突き合わせ結果 (ホスト review-001.md との照合、2026-09-08)

| 指摘 | 出典 | 採否 | 根拠 |
|---|---|---|---|
| 新規失敗契約のテストが診断文言と元例外を固定していない (`DM_MA_01` に `ViewCreationFailed` が無い / Kotlin の MB-MA-15 が接頭辞のみ / MB-MA-11 が InnerException の非 null のみ) | 相方のみ | **採用** (Minor) | handbook `cross/diagnostic-message-language.md` 「検査」節が「英語文言が意図どおりであることは失敗型ごとの完全一致テスト (DM-MA-01〜04 系列) が担う」と定める。spec MB-MA-11 は「InnerException が DI の解決失敗の例外」を要求するが現行 assertion では識別できない。該当箇所・実害 (文言の誤記が素通り) とも具体的 |
| Sample 証跡の判定サマリーの内訳 (8 + 1 + 1 + 2 + 4) が 14 件にならない | 相方のみ | **採用** (Minor) | `evidence/sample-walkthrough/README.md` の判定節で実測。lessons impl L-001 (説明文は保存実体と一致させる) に反する |
| core/ADR-0033 の Alternatives / Consequences が古いまま残る | ホストのみ | 確定 (Minor、蒸留時) | 実装変更なし。ksn-distill への申し送り |
| facade nupkg の XML ドキュメントが android TFM のみ | ホストのみ | 確定 (Minor、phase-8/9) | spec 違反ではない。初版発行前に方針を明示 |
| `KSDLG0001` が diagnostic-message-language の grep 範囲外 | ホストのみ | 確定 (Minor、蒸留時) | handbook の対象範囲の改訂 |
| aar 検査ターゲットの一度きりの発火確認 | ホストのみ | 確定 (Suggestion) | 任意。deviation.md 記載済み |
| MAUI の `0.0.0-dev` 発行ガード | ホストのみ | 確定 (Suggestion、phase-9) | 申し送り |

未解決: なし。採用 2 件は修正サイクル (review-002 で独立確認) へ。
