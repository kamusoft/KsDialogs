# セカンドオピニオン: localize-dialog-error-messages (spec-001)
**相方**: codex / **label**: so-spec-localize-dialog-error-messages / **日付**: 2026-09-07 / **対象**: kasane/changes/localize-dialog-error-messages/ の proposal.md / specs/ (5 capability) / tasks.md (提案一式)
---
# 独立スペックレビュー結果

Critical 0件、Major 5件、Minor 1件です。静的レビューのみ実施し、ビルド・テスト・ファイル更新は行っていません。

## 照合した規約・決定

- `cross/ADR-0015` — 診断文言は英語固定
- `cross/test-execution.md` — 仕様とテストの対応検査
- `cross/user-skill-writing-style.md` — 診断表、en/ja 同期、英語表現
- `cross/user-skill-api-listing.md`
- `cross/docs-refresh-timing.md`
- ksn-core のデルタスペック・ドメイン・パス規約
- iOS / Android / MAUI / KMP の Dialog 公開面 concept

## 指摘事項

### [🟠 Major] メッセージ文字列が公開互換契約なのか決まっていない

**該当箇所**: `proposal.md:33`、`specs/ios-native/spec.md:9`、`specs/ios-native/spec.md:23`、`specs/user-skills/spec.md:9`

**問題点**: proposal は「ライブラリはメッセージ文字列を契約として保証していない」としています。一方、各 spec は全文を `SHALL` で固定し、完全一致テストを要求し、利用者向け Skills にも正確な文字列を掲載します。この状態では、将来の文言改善が破壊的変更なのか、単なる非互換性のない修正なのか判断できません。

**推奨修正**: 次のどちらかを仕様で明示してください。

- 文言を公開互換契約にするなら、Impact と互換性方針を修正する。
- 今回のリリースでの実装値は完全一致で検証するが、将来互換は保証しないなら、proposal と Skills に「現在の診断文言であり安定 API ではない」と明記する。

### [🟠 Major] 「iOS と Android で同じ文言」の意味が仕様間で矛盾している

**該当箇所**: `proposal.md:16`、`specs/ios-native/spec.md:35`、`specs/ios-native/spec.md:43`、`specs/ios-native/spec.md:50`、`specs/android-native/spec.md:26`、`specs/android-native/spec.md:31`、`specs/android-native/spec.md:33`

**問題点**: proposal と ADR は対応ログを同じ英語文言にするとしていますが、次は一致しません。

- フック失敗: iOS は本文に `: {error}` を含み、Android は例外を `Log.w` の throwable 引数で別渡しする。
- Toast content 生成失敗も、iOS だけ本文に `{error}` を含む。
- proposal が対応対象に挙げる「添付値未達」は Android の表に対応行がない。
- Android の共通 `DialogLayoutHost` は Dialog / Loading / Toast で同じ「Dialog attachments」文言を使う一方、iOS は機能別文言になっている。

「同じ」が基礎文だけを指すのか、最終的に表示されるログ全体を指すのか判定できません。

**推奨修正**: 対応ペアを明示した表を設け、比較単位を「本文テンプレート」「動的なエラー説明」「throwable」のどれにするか決定してください。対応物がない項目は同一化対象から外してください。

### [🟠 Major] DM-KM-01 の実装タスクでは Scenario を検証できない

**該当箇所**: `specs/kmp-facade/spec.md:17`、`tasks.md:22`、`kmp/ksdialogs-kmp/src/iosTest/kotlin/jp/kamusoft/ksdialogs/kmp/InteropBridgeContractTests.kt:87`

**問題点**: DM-KM-01 は、未登録と提示先不在の両方について、`Dialog.instance.show` から共有コードの `DialogException.message` へ届くことを要求しています。しかし tasks が更新対象とする3 assertionのうち2件は、Native bridge の `KSDInteropDialogResult.error.localizedDescription` を直接検査しています。共有 `DialogException` を検査するのは提示先不在の1件だけです。未登録経路の既存テストは同ファイルの118–125行にありますが、例外型しか確認しておらず message assertion がありません。

**推奨修正**: 未登録の `Dialog.instance.show` が返す `DialogException.message` にも英語文言の assertion を追加してください。bridge の直接検査と facade の素通し検査は、別 Scenario ID に分けるのが明確です。

### [🟠 Major] Scenario ID 網羅検査が7つの受け入れ条件をゲートしない

**該当箇所**: `specs/ios-native/spec.md:53`、`specs/android-native/spec.md:38`、`specs/kmp-facade/spec.md:22`、`specs/maui-binding/spec.md:66`、`specs/user-skills/spec.md:11`、`scripts/scenario-id-coverage.py:273`、`scripts/scenario-id-coverage.py:703`、`tasks.md:46`

**問題点**: 日本語リテラル検査4件と user-skills の3 Scenario、合計7件に ID がありません。特に user-skills spec は ID が1件もないため、スクリプトの実装上はファイル全体が「管理対象外」になります。ID付き spec 内の無ID Scenarioも警告だけで、終了コードは0のままです。

したがって tasks 6.2 の「終了コード0」は、これら7条件を一切満たさなくても通ります。MAUI の4つの Platform gateway fallback とKMP固有定数も、最終的には grep と目視だけに依存しています。

**推奨修正**: 全Scenarioに安定IDを付けてください。自動テストにできない静的検査は、理由付き除外として `scenario-id-coverage.py` に登録し、検証コマンドと期待結果を tasks に明記してください。到達可能な fallback 経路には直接テストを追加してください。

### [🟠 Major] 英語文言に不自然・不明瞭な表現が残っている

**該当箇所**: `specs/ios-native/spec.md:45`、`specs/maui-binding/spec.md:31`、`specs/maui-binding/spec.md:63`、`specs/maui-binding/spec.md:64`、`specs/user-skills/spec.md:9`

**問題点**: 主目的である英語品質として、次の表現は公開用診断文として不自然です。

- `fixing to the values of layout pass {N}` — “fixing to” の用法が不自然。
- `a default-View Toast` — ハイフンと大文字の組み合わせが英語として不自然。
- `This call returns as a failure` — 非慣用的で、通常は `This call fails`。
- `No attachment values arrived` — `were received` の方が自然。
- `This instance of ViewModel type {T} is already showing` — ViewModel 自身が何かを表示しているようにも読める。
- en Skills でもプレースホルダを `{型名}` のままにする指定になっており、英語利用者向け文言に日本語が残る。

**推奨修正**: 英語表を一括で校正し、例えば次のように直してください。

- `...; using the values from layout pass {N}.`
- `The Native library owns the content for the default Toast.`
- `This call fails.`
- `No attachment values were received ...`
- `This ViewModel instance of type {T} is already being shown.`
- en は `{type name}`、ja は `{型名}` とし、検査では両者を同じプレースホルダとして正規化する。

### [🟡 Minor] Skills の対象ファイル数が12件と24件で食い違う

**該当箇所**: `proposal.md:18`、`proposal.md:34`、`specs/user-skills/spec.md:3`、`specs/user-skills/spec.md:12`、`tasks.md:38`

**問題点**: `en / ja × 4形態 × 3機能` は合計24ファイルです。tasks は ja 12件と en 12件を別々に更新するため24件を意図していますが、proposal・spec・Scenario は12件と記載しています。実装者が片言語だけを対象数として扱う余地があります。

**推奨修正**: 全文を「24ファイル（各言語12ファイル）」へ統一してください。

## アクションプラン

1. 文言を公開互換契約にするか決定する。
2. 形態間で「同じ」とする比較単位と対応ペアを確定する。
3. 英語対応表を校正し、en のプレースホルダを英語化する。
4. DM-KM-01 の facade 経路テストを補完する。
5. 全ScenarioへIDを付け、静的検査の除外理由と実行方法を固定する。
6. Skills の対象数を24ファイルへ訂正して再レビューする。

## 判定

**NEEDS_DISCUSSION**


---

## 突き合わせ結果 (2026-09-07、ホスト側自己レビュー 2 周との突き合わせ)

| # | 相方の指摘 | 採否 | 根拠と反映先 |
|---|---|---|---|
| 1 | Major: メッセージ文字列が互換契約か決まっていない | **採用** (相方のみ・根拠強: proposal と spec の SHALL / 完全一致 Scenario が読み手に契約と映る) | 「文言は互換契約ではない。完全一致 Scenario は今回の置き換えの受け入れ基準」を proposal Impact と 4 形態の spec 前文に明記。Skills の診断表に「現在の実装値で安定 API ではない」の 1 文を添える (user-skills spec) |
| 2 | Major: 「iOS と Android で同じ文言」の比較単位が不明・対応物が無い項目を含む | **採用** (相方のみ・根拠強: 添付値未達は iOS のみ、Android の `DialogLayoutHost` は 3 器共用で機能名を持たないことを実装で確認) | 比較単位を「動的な値の前の本文テンプレート」と定義し、動的なエラー説明の渡し方は OS のログ慣行に従うと proposal と両 spec に明記。添付値未達と収束警告を同一化対象から外し、Android の収束警告は機能名を含めない文言に変更 |
| 3 | Major: DM-KM-01 の 3 assertion のうち 2 件は互換面直検査で共有 `DialogException` を見ていない | **採用** (相方のみ・根拠強: `InteropBridgeContractTests.kt` L87-100 は `KSDInteropDialogResult.error.localizedDescription`、未登録経路 L118-125 は例外型のみ) | DM-KM-01 を互換面直検査に限定し、共有コード素通しを DM-KM-03 として分離。未登録経路の message assertion 追加を tasks 3.2b に追加 |
| 4 | Major: 静的 grep 検査 4 件と user-skills 3 Scenario に ID が無く coverage script のゲートにならない | **一部採用** | grep 検査 4 件に DM-IO-03 / DM-AN-02 / DM-KM-04 / DM-MA-05 を付与し、既存の除外機構 (`DEFAULT_ALLOW_MISSING`、PB-SM / BV-MA の先例) に理由付きで登録するタスク (6.0) と、コマンド・期待結果つきの証跡記録 (6.4) を追加。user-skills の 3 Scenario は **降格**: 挙動系ではなく core/ADR-0016 の ID の対象外で、先例 (rollout-user-docs / rename-dialog-contract-singular の user-docs / user-skills spec) も ID を持たない。受け入れは ksn-verify と検査 script。MAUI Platform gateway フォールバックへの直接テスト追加も **降格**: `dotnet test` の器は facade のみで platform コードに乗らない |
| 5 | Major: 英語文言の不自然さ・en のプレースホルダが日本語 | **採用** (相方のみ・根拠強: 公開文言の品質は本 change の主目的) | `fixing to` → `using the values from`、`No attachment values arrived` → `were received`、`a default-View Toast` → `The Native library owns the content of the default Toast.`、`This call returns as a failure` → `This call fails`、`already showing` → `This ViewModel instance of type {T} is already being shown.` を全 spec に反映。プレースホルダは en / ja とも `{TypeName}` に統一 (相方案の言語別 + 正規化より、メッセージ列を byte 一致させる方が parity 検査と整合するため簡素化) |
| 6 | Minor: Skills の対象数 12 と 24 の食い違い | **採用** | proposal / user-skills spec / tasks を「24 ファイル (各言語 12)」に統一 |

未解決: なし。相方の判定 NEEDS_DISCUSSION は上記 6 件の反映で解消 (設計判断を要した #1 は proposal の既存方針「文言は契約でない」に揃えて確定し、オーナーへの提示事項として報告する)。
