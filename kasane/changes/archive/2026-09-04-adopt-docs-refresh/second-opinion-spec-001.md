# セカンドオピニオン: adopt-docs-refresh (spec-001)
**相方**: codex / **label**: so-spec-adopt-docs-refresh / **日付**: 2026-09-04 / **対象**: 提案一式 (proposal.md / specs/docs-refresh/spec.md / tasks.md)
---
# レビュー結果: adopt-docs-refresh

**日付**: 2026-09-04  
**判定**: NEEDS_DISCUSSION

## サマリー

Critical はありませんが、Major 6件があります。特に、閉世界性を機械的に保証できないこと、現存 ABI 型の誤分類、識別子正典の不足、翻案元依存で契約が自己完結していないことは、実装前に仕様判断が必要です。

静的スペックレビューのため、ビルド・テストは実行していません。

## 照合した規約

- `ksn-core` のデルタスペック、設定解決、handbook 読み込み、パス記述規約
- `kasane/handbook/cross/comment-policy.md`（always）
- `kasane/handbook/cross/test-execution.md`（完了判定）
- `kasane/lessons/spec-review.md` L-001
- `kasane/decisions/cross/0005-public-identifier-mapping.md`
- `kasane/decisions/android/0001-compose-api-separate-module.md`
- `kasane/decisions/kmp/0004-swift-facing-typed-generic-facade.md`

## 指摘事項

### [🟠 Major] 閉世界性の決定を検査できず、README への適用範囲も曖昧

**該当箇所**: `kasane/roadmaps/package-distribution/phases/phase-1-skills-foundation/agenda.md:16`、`kasane/changes/adopt-docs-refresh/specs/docs-refresh/spec.md:103`、`../KsSettingsView/.agents/skills/docs-refresh/scripts/link-resolution-check.py:31`

**問題点**: agenda は「Skill 外のファイル・URLを参照しない」と決定していますが、spec の検査対象は `kasane/`、ADR、機械面の名前だけです。コピー対象の `link-resolution-check.py` は外部 URL を明示的に無視し、リポジトリ内に実在すれば別 Skill やルートファイルへのリンクも許します。そのため、閉世界性に違反する Skill が全検査を通過できます。

また6-⑤は README も含む対象一覧を使うため、単純に URL 禁止を加えると、README に必要な配布先リンクまで禁止する可能性があります。

**推奨修正**: 閉世界性を `skills/{en,ja}/` のみに適用すると明記し、次を検出する Scenario とタスクを追加してください。

- `http:` / `https:` 等の外部 URL
- 現在の Skill ルートから外へ出る相対リンク
- 別 Skill への参照

その実装には、scripts 無改変という制約を撤回してリンク検査を拡張するか、Skill 専用の追加検査を設けるかの判断が必要です。

### [🟠 Major] 現存する cinterop ABI 型を「廃止済み」と誤分類している

**該当箇所**: `kasane/changes/adopt-docs-refresh/proposal.md:17`、`kasane/changes/adopt-docs-refresh/tasks.md:16`、`ios/Sources/KsDialogs/Interop/KsDialogsInteropResultType.swift:9`、`ios/Sources/KsDialogs/Interop/KsDialogsInteropBridge.swift:39`

**問題点**: `KsDialogsInteropResultType` は廃止済みではありません。現在も public な KMP cinterop 委譲面であり、`KsDialogsInteropBridge.registerViewFactory` の引数として使用されています。廃止されたのは利用者による手動申告経路であって、ABI 型そのものではありません。

翻案元 Guardrails の「廃止概念を復活させない」をこの型へ置換すると、必要な機械面を削除・非公開化すべきものと誤解させ、`kasane/decisions/kmp/0004-swift-facing-typed-generic-facade.md:20` の境界判断と衝突します。

**推奨修正**: `KsDialogsInteropBridge` と `KsDialogsInteropResultType` を「現存する public ABI だが利用者向けではない機械面」と定義してください。利用者向け生成物からの漏れは検出しつつ、「廃止済み」「復活させない」という表現は使わないでください。

### [🟠 Major] 識別子検査の正典に `ksdialogs-compose` が存在しない

**該当箇所**: `kasane/changes/adopt-docs-refresh/specs/docs-refresh/spec.md:121`、`kasane/decisions/cross/0005-public-identifier-mapping.md:16`、`kasane/decisions/android/0001-compose-api-separate-module.md:14`

**問題点**: spec は6-⑧の正を cross/ADR-0005だけとしていますが、この ADR の表には `jp.kamusoft:ksdialogs-compose` と `.compose` パッケージがありません。Compose artifact は後発の android/ADR-0001で決定されています。したがって、spec に列挙された正しい識別子すべてを「cross/ADR-0005から導く」ことはできません。

**推奨修正**: 正典を少なくとも cross/ADR-0005とandroid/ADR-0001の組にしてください。各 Maven 座標・Kotlin package・NuGet IDについて、正例と誤例を明示した fixture を用意してください。

### [🟠 Major] 取り込む契約が翻案元参照に依存し、KsDialogs側で自己完結していない

**該当箇所**: `kasane/changes/adopt-docs-refresh/specs/docs-refresh/spec.md:3`、`kasane/changes/adopt-docs-refresh/tasks.md:5`、`../KsSettingsView/.agents/skills/docs-refresh/SKILL.md:184`

**問題点**: manifest、承認ゲート、実行フラグ、部分承認、manifest 更新などを「翻案元と同一」として再定義していません。一方、現在のコピー元 SKILL.md には、archive spec に現れない3e API網羅検査、固定の `ksn-implementer` 起動条件、予定manifest処理なども含まれます。

どの時点の何を契約として採用するのかが一意でなく、SKILL.md の差し替え中に既存挙動を壊しても、tasks 5.1〜5.7では十分に検出できません。隣接リポジトリの将来変更によって受け入れ基準が動く問題もあります。

**推奨修正**: 次のいずれかで契約を固定してください。

- 採用する Requirement / Scenario をKsDialogs側デルタスペックへ展開する
- コピー元の確定commitまたは内容hashと、許可する差分を固定する

少なくとも manifest検証、承認前無変更、部分承認、`--all`、`--readme-only`、中断時の再検出、委譲不能時停止は受け入れ項目に含める必要があります。

### [🟠 Major] 3dの将来差分Scenarioを検証せず、KMPの共有catalog前提も監視しない

**該当箇所**: `kasane/changes/adopt-docs-refresh/specs/docs-refresh/spec.md:75`、`kasane/changes/adopt-docs-refresh/tasks.md:34`、`kmp/settings.gradle.kts:29`

**問題点**: task 5.3は現時点で値が空でないことしか確認しません。spec が要求するAGP変更、wrapper不一致、MAUI版変更から更新対象を導くScenarioは検証されません。

また「KMPはAndroidのcatalogを共有する」という前提は現在 `kmp/settings.gradle.kts:32` で成立していますが、docs-refreshの監視対象ではありません。将来KMPが別catalogへ移っても、Android側の値をKMPの正として読み続けます。

**推奨修正**: `kmp/settings.gradle.kts` のcatalog参照を機械チェック対象に加え、共有が外れた場合は停止または差異報告する契約を追加してください。各取得元の変更から、期待するREADME／Skillが要追従になることを正負fixtureで確認してください。

### [🟠 Major] project handbookが要求する完了テストがtasksにない

**該当箇所**: `kasane/changes/adopt-docs-refresh/tasks.md:30`、`kasane/handbook/cross/test-execution.md:15`

**問題点**: handbook は変更の完了判定に、絞り込みなしの全件実行と実行件数の確認を要求しています。tasksにはdocs-refresh用fixtureとlintしかなく、iOS／Android／KMP／MAUIの全件実行がありません。このままでは実装が成功しても、プロジェクト規約上の完了条件を満たせません。

**推奨修正**: handbook所定の全ビルドルート実行と件数確認をtasksへ追加してください。docs-only変更では不要とするなら、暗黙に省略せず、規約側の適用条件または本changeの合意済み例外として先に確定してください。

### [🟡 Minor] manifest異常系の受け入れ検証が不在ケースだけ

**該当箇所**: `kasane/changes/adopt-docs-refresh/specs/docs-refresh/spec.md:23`、`kasane/changes/adopt-docs-refresh/tasks.md:33`

**問題点**: Requirement は「不在・破損JSON・version不一致」を同列に扱いますが、Scenarioとtaskはmanifest不在しか確認しません。破損やversion不一致時に初期生成へ進む退行を検出できません。

**推奨修正**: 不在、JSON破損、version 2、必須キー欠落、型不正を表形式fixtureで検証し、全ケースで生成物が不変であることを確認してください。

### [🟡 Minor] コピー元固有語の残留検査が部分的

**該当箇所**: `kasane/changes/adopt-docs-refresh/specs/docs-refresh/spec.md:37`、`kasane/changes/adopt-docs-refresh/tasks.md:18`

**問題点**: spec は追従対象節の `kssettingsview-*` だけを禁止し、task 2.9も一部の旧API名と `docs/` 前提に限定されています。コピー元にはプロジェクト名、Skill名、ADR番号、phase番号、パス例が多数あるため、別節に残った誤参照が実行手順を誤らせる可能性があります。

**推奨修正**: SKILL.mdとprompt 2本の全体を対象に、`KsSettingsView`、`kssettingsview`、旧ADR／phase、旧パスを検査してください。意図的な翻案元表記が必要なら、許容箇所を明示的に限定してください。

## アクションプラン

1. 閉世界性の対象範囲と検査方式を決定する。
2. KMP機械面を「現存ABI」として仕様・tasksの用語を修正する。
3. 識別子正典を複数ADRへ広げる。
4. 翻案元から継承する契約をKsDialogs側で固定する。
5. 3dの差分fixtureとKMP catalog共有検査を追加する。
6. handbook所定の全件テストを行うか、例外を合意する。
7. manifest異常系とコピー元固有語の検査を補強する。

指摘件数: Critical 0 / Major 6 / Minor 2 / Suggestion 0。  
以上から、現時点の判定は **NEEDS_DISCUSSION** です。


## 突き合わせ結果 (2026-09-04)

ホスト側の自己レビュー (2 周) は指摘なしのため、全件が「相方のみ」の指摘。根拠の強さで採否を判定した。

| # | 指摘 | 採否 | 根拠・反映先 |
|---|---|---|---|
| 1 | 閉世界性を検査できない・README への適用範囲が曖昧 | **採用 (範囲限定)** | KsSettingsView の Skill 実物に配布座標 URL (SwiftPM package URL・`metadata.source`) と同一 Skill 内の `../SKILL.md` リンクが正当に存在するため、機械検査は「Skill ルートの外へ解決される相対リンク」(skills/ のみ、README 対象外) に絞り、外部 URL の禁止は生成プロンプトとレビューに委ねる。scripts/ は無改変のまま SKILL.md 内の断片で実装。spec「閉世界性と機械面の漏れ検査」③ + Scenario、tasks 2.7 / 5.4 |
| 2 | `KsDialogsInteropResultType` を「廃止済み」と誤分類 | **採用** | `ios/Sources/KsDialogs/Interop/KsDialogsInteropResultType.swift` は現存する public ABI (KMP cinterop 委譲専用)。kmp/ADR-0004 で廃止されたのは手動 enum 申告経路であって型ではない。proposal / spec / tasks 2.7 の表現を「現存する public ABI だが利用者向け導線に載せない」に修正 |
| 3 | 識別子の正典に `ksdialogs-compose` が無い | **採用** | android/ADR-0001 が `jp.kamusoft:ksdialogs-compose` を定義。正典を cross/ADR-0005 + android/ADR-0001 に拡張し、fixture に 2 ADR 分の正例・誤例を用意 (spec「配信識別子の表記ゆれ検査」、tasks 2.8 / 5.4) |
| 4 | 翻案元参照に依存し契約が自己完結していない | **採用** | 翻案元を KsSettingsView commit `33ce94e` (2026-09-02、作業ツリー clean) に固定し、継承する契約項目 (manifest 検証・承認前無変更・Skill 単位委譲と器固定・実行フラグ・3e 報告のみ・予定 manifest・最後に書く規律と旧ハッシュ保持) を Requirement 本文と Scenario「翻案元の契約の保持」に展開 (tasks 1.1 / 5.7) |
| 5 | 3d の差分 Scenario を検証せず、KMP の catalog 共有前提を監視しない | **後半採用 / 前半降格** | 後半: `kmp/settings.gradle.kts` の `from()` 参照を 3d の前提確認行として追加し、外れたら報告して停止 (spec Scenario「KMP の catalog 共有が外れた」、tasks 2.5 / 5.3)。前半: 3d はスクリプトではなく SKILL.md の手順 (散文) のため fixture 実走ではなく「取得元 → 突合先の対応が本文にある」ことの照読で担保 (tasks 5.3) |
| 6 | handbook (テスト実行規約) の全件実行が tasks に無い | **オーナー判断へ** | 本 change は製品コード・テストに触れないため「合意済み例外」として proposal Impact と tasks 6.1 に明記。例外の可否はハンドオフ時にオーナーへ確認する |
| 7 | manifest 異常系の検証が不在ケースだけ | **降格 → 照読** | manifest 検証 (Step 2) は SKILL.md の手順であり scripts は version を見ないため fixture では検証できない。4 ケースの列挙を照読で確認 (spec Scenario「異常な manifest でも書き換えない」、tasks 5.2) |
| 8 | 翻案元固有語の残留検査が部分的 | **採用** | tasks 2.9 を SKILL.md + prompt 2 本の全文 grep (プロジェクト名・Skill 名・旧 ADR / phase 番号・旧 handbook パス・`docs/` / `openspec`) に拡張し、許容箇所を冒頭の翻案元出典 1 箇所に限定 |

採用 5 (うち範囲限定 1・部分 1) / 降格 2 / オーナー判断 1 / 未解決 0。反映後の spec は 9 Requirement / 22 Scenario、tasks は 23 件。
