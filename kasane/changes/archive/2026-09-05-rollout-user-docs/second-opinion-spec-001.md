# セカンドオピニオン: rollout-user-docs (spec-001)
**相方**: codex / **label**: so-spec-rollout-user-docs / **日付**: 2026-09-04 / **対象**: kasane/changes/rollout-user-docs/ の proposal.md / design.md / specs/ 3 本 / tasks.md / ui/brief.md (実装前の提案一式)
---
# レビュー結果: rollout-user-docs（スペックレビュー）

**日付**: 2026-09-04  
**判定**: **NEEDS_DISCUSSION**  
**結果ファイル**: 未作成（read-only 制約に従い、本応答のみ）

## サマリー

変更の目的と大枠は明確ですが、既存 docs-refresh 契約との衝突、利用者下限の根拠不足、レビュー完了条件の穴があります。このまま実装すると、生成時には正しくても次回 docs-refresh で退行する可能性があるため、実装開始前に仕様側の修正が必要です。

指摘数: **Critical 0 / Major 6 / Minor 3 / Suggestion 0**

## 照合した規約

- `ksn-review` 独立レビュー規律
- ksn-core デルタスペック・handbook・パス規約
- `test-execution.md`
- `sample-parity.md`
- `local-development-setup.md`
- `aiforms-origin-reference.md`
- cross/ADR-0011
- adopt-docs-refresh の既存デルタスペック
- package-distribution roadmap / phase-2 agenda
- 現行 API concepts、KMP/Gradle 構成、翻案元 KsSettingsView

## 指摘事項

### [🟠 Major] Toast の移行方針が既存 docs-refresh 契約と衝突する

**該当箇所**: `specs/user-skills/spec.md:71`、`.agents/skills/docs-refresh/SKILL.md:30`、`kasane/changes/archive/2026-09-04-adopt-docs-refresh/specs/docs-refresh/spec.md:47`、`tasks.md:55`

**問題点**: 新仕様は旧 `Toast.Instance.Show(message)` に新 Toast の message 入口を対応付けます。一方、既存契約と現行 docs-refresh は「移植元で Obsolete だった Toast は対応先なし」と規定しています。tasks 7.3 は 3e 注記と README 言及しか更新しないため、この矛盾が残ります。将来の docs-refresh が新しい対応表を旧方針へ戻す危険があります。

**推奨修正**: docs-refresh の既存 Requirement を `MODIFIED` し、「message 入口は移行案内あり、旧 View 登録は直接対応なし」と具体化してください。現行 SKILL.md の追従対象説明も tasks 7.3 の更新対象へ追加し、同内容を検査する Scenario を設けてください。

### [🟠 Major] `api-mapping.md` が「全 references は完動コードのレシピ」という要件を満たせない

**該当箇所**: `specs/user-skills/spec.md:29`、`specs/user-skills/spec.md:71`、`kasane/decisions/cross/0011-user-docs-as-agent-skills.md:31`

**問題点**: user-skills は全 `references/` ファイルを「自然言語見出し＋リード文＋完動コード」のレシピと要求しています。しかし `api-mapping.md` は旧 API と新 API の対応表であり、ADR-0011 も「MAUI Skill のレシピ形式とは別の読み物」と明記しています。どちらを満たすべきか実装者が判断できません。

**推奨修正**: `api-mapping.md` をレシピ形式の明示的な例外にしてください。レシピ形式の Scenario も platform Skill の references のみに限定し、移行 Skill は対応表固有の構造で検査してください。

### [🟠 Major] 利用側 Kotlin 最低版の根拠が存在しない

**該当箇所**: `specs/repository-docs/spec.md:39`、`design.md:102`、`.agents/skills/docs-refresh/SKILL.md:195`

**問題点**: README に「利用側の Kotlin 最小版」を載せる一方、3d の取得元は version catalog のビルド使用版だけです。現行の `android/gradle/libs.versions.toml:6` が示す Kotlin 2.4.10 は消費者最低版を証明しません。KMP 範囲は phase-7 待ちですが、Android Native 側の最低版についても決定・検証方法がありません。翻案元でもビルド版 2.4.10 と利用側下限 2.3 は別値です。

**推奨修正**: Android/KMP それぞれについて、最低版の決定元と互換性検証方法を定義してください。phase-7 待ちなら具体的な暫定値を推測せず、「未確定」とする受け入れ基準へ変更する選択肢があります。3d がビルド版と消費者下限を別々に検査できる形にしてください。

### [🟠 Major] frontmatter の機械検査が Requirement を検証していない

**該当箇所**: `specs/user-skills/spec.md:19`、`specs/user-skills/spec.md:23`、`tasks.md:38`、`.agents/skills/docs-refresh/scripts/frontmatter-check.py:56`

**問題点**: Requirement は `license`、`metadata.source`、ja description の英語キーワードまで必須とします。しかし既存スクリプトが必須確認するのは `name` と `description` だけで、`license`、`metadata.source`、英語キーワードの欠落は成功扱いになります。tasks 5.1 はこのスクリプトだけで Requirement を満たした扱いです。

**推奨修正**: 次のいずれかを仕様で選んでください。

- 既存 docs-refresh 契約も変更して `frontmatter-check.py` を強化する
- 初期生成専用の追加静的検査を tasks 5.1 に定義する

少なくとも全必須フィールドの存在と `metadata.source` の期待値は機械判定してください。キーワード選定をレビュー判断にする場合は Scenario から機械検査扱いを外してください。

### [🟠 Major] KMP ホスト文書の責務と manifest の源泉割当が不足している

**該当箇所**: `design.md:28`、`design.md:67`、`design.md:73`、`design.md:74`

**問題点**: `android-host.md` / `ios-host.md` をホスト側登録レシピとしていますが、Loading・Toast concepts は KMP の `loading.md` / `toast.md` にしか割り当てられていません。現行 iOS 公開面には KMP 用の Loading/Toast 登録 APIも存在します（`ios/Sources/KsDialogs/Kmp/KsLoadingKmp.swift:34`、`ios/Sources/KsDialogs/Kmp/KsToastKmp.swift:33`）。ホスト登録をどちらの文書へ置くか未確定であり、現行の網羅検査は「concept がどこか一つに載る」ことしか見ないため、個別 target の源泉漏れを検出できません。

**推奨修正**: Dialog・Loading・Toast のホスト登録を、6機能別 references と host references のどちらへ置くか明記してください。host references に置く場合は loading/toast concepts をその `targets` に加え、ファイル単位の源泉完全性を独立レビューの明示的な合格条件にしてください。

### [🟠 Major] 独立レビューが APPROVED であることを完了条件にしていない

**該当箇所**: `specs/user-skills/spec.md:133`、`specs/user-skills/spec.md:139`、`tasks.md:45`、`tasks.md:48`

**問題点**: 完了条件は「指摘反映済み」に留まり、`ksn-review` の判定が `APPROVED` であることを要求していません。tasks でも一度レビューした後、修正と機械検査だけを行い、再レビューしません。`CHANGES_REQUESTED` の Major を不完全に修正しても完了扱いできてしまいます。

**推奨修正**: 各独立レビューについて最終判定 `APPROVED` を必須化し、修正が入った場合の再レビューを tasks に追加してください。初見レビューも、指摘反映で対象本文が大きく変わった場合の再実施条件を決めてください。

### [🟡 Minor] 初期 manifest の組み立て手順が既存スクリプトと噛み合わない

**該当箇所**: `tasks.md:22`、`.agents/skills/docs-refresh/scripts/planned-manifest.py:27`

**問題点**: tasks 2.6 は `planned-manifest.py` で初期 manifest 草案を組むとしていますが、同スクリプトは既存の `skills/.manifest.json` を必ず読みます。初期生成時にはそのファイルが存在しません。

**推奨修正**: 初期 manifest を独立生成する手順を定義するか、最初に bootstrap manifest を書いてからスクリプトを使う順序と一時ファイルの扱いを明記してください。

### [🟡 Minor] 削除済み README の Scenario が未解決リンクを許してしまう

**該当箇所**: `specs/repository-docs/spec.md:18`

**問題点**: THEN が「解決可能な参照が存在しない」なので、削除先を指す未解決リンクは条件を満たしてしまいます。Requirement と tasks 7.4 が求めるのは参照・言及そのもののゼロ件です。

**推奨修正**: 「該当パスへの Markdown リンクまたは文字列言及が存在しない」と変更し、未解決リンクも失敗させてください。

### [🟡 Minor] 「6枚は同一の Sample ビルド」は4ルート構成では成立しない

**該当箇所**: `ui/brief.md:29`

**問題点**: iOS と Android は独立したビルドルートなので、6枚を同一ビルドから撮ることはできません。受け入れ条件として文字どおりには達成不能です。

**推奨修正**: 「同一リポジトリ revision、各 platform で同一構成・同一画面向きのビルド」と定義し直してください。

## アクションプラン

1. Toast 移行方針を既存 docs-refresh 契約まで含めて一本化する。
2. `api-mapping.md` の構造と KMP host references の責務境界を確定する。
3. 利用側 Kotlin 下限の決定元・暫定表現・検証方法を定める。
4. frontmatter 検査とレビューゲートを、実際に合否を判定できる形へ強化する。
5. Minor 3件を修正後、再度スペックレビューを行う。

静的レビューのため、ビルド・テスト・ファイル書き込みは実施していません。


## 突き合わせ結果 (2026-09-04、ホスト側自己レビュー 2 周との照合)

ホスト側の自己レビューは 2 周とも整合性チェックリストを通過し指摘なし。相方の 9 件はすべて「相方のみ」で、根拠 (該当箇所の実在・実害シナリオ) を実物で確認したうえで採否を決めた。

| # | 指摘 | 採否 | 根拠の確認と反映先 |
|---|---|---|---|
| 1 | Toast 移行方針が既存 docs-refresh 契約と衝突 | **採用** | `.agents/skills/docs-refresh/SKILL.md:30` と adopt-docs-refresh spec「追従対象の規範」に「対応先なしと扱う」が実在。docs-refresh spec に同 Requirement の MODIFIED (全文) と Scenario「Toast の移行方針の一致」を追加、tasks 7.3 で SKILL.md の行を書き換える |
| 2 | api-mapping.md がレシピ形式要件を満たせない | **採用** | cross/ADR-0011 が「レシピ形式とは別の読み物」と明記。user-skills spec のレシピ形式を platform Skill に限定し、対応表の形の Scenario を追加 |
| 3 | 利用側 Kotlin 最小版の根拠が無い | **採用** | 3d の取得元はビルド使用版のみで消費者下限の決定元が無い。repository-docs spec / design D5 を「Kotlin 最小版は値を推測せず確定前と明記、minSdk / compileSdk / MAUI 本体下限は取得元から機械的に読む」へ変更 (agenda 決定「暫定値の印」の具体化) |
| 4 | frontmatter 検査が Requirement を検証していない | **採用** | `frontmatter-check.py:60` の必須確認は `name` / `description` のみ。追加静的検査 (`license` / `metadata.source`) を tasks 5.1 に定義し、英語キーワードは独立レビューの確認項目へ移した |
| 5 | KMP ホスト文書の責務と源泉割当の不足 | **採用** | `ios/Sources/KsDialogs/Kmp/KsLoadingKmp.swift` / `KsToastKmp.swift` に KMP 用登録 API が実在。design D1 でホスト側 2 本の責務を Dialog / Loading / Toast の登録に確定、D3 の targets に loading / toast を追加、ファイル単位の源泉完全性を独立レビューの合格条件に |
| 6 | 独立レビュー APPROVED を完了条件にしていない | **採用** | user-skills spec「レビュー 4 層」と tasks 6.4 に APPROVED 必須・再レビュー・初見レビューの再実施条件を追加 |
| 7 | 初期 manifest の手順がスクリプトと噛み合わない (Minor) | **採用** | `planned-manifest.py:27` が `skills/.manifest.json` を無条件に読む。bootstrap manifest を先に書く手順を design D3 と tasks 2.6 に明記 |
| 8 | 削除済み README の Scenario が未解決リンクを許す (Minor) | **採用** | repository-docs spec の THEN を「リンクも文字列言及も存在しない」に変更、tasks 7.4 も同様 |
| 9 | 「同一の Sample ビルド」が 4 ルートでは成立しない (Minor) | **採用** | ui/brief.md を「同一 revision から各 platform で同一構成でビルド」に変更 |

採用 9 / 降格 0 / 未解決 0。相方の判定 NEEDS_DISCUSSION はこれらの反映で解消 (設計判断の追加は不要で、既存決定の具体化に収まる)。
