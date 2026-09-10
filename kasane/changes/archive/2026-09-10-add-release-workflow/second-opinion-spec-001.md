# セカンドオピニオン: add-release-workflow (spec-001)
**相方**: codex / **label**: so-spec-add-release-workflow / **日付**: 2026-09-10 / **対象**: kasane/changes/add-release-workflow/ の proposal / design / specs / tasks (提案一式)
---
# レビュー結果: add-release-workflow

**日付**: 2026-09-10  
**判定**: **NEEDS_DISCUSSION**

## サマリー

Critical 0件、Major 6件、Minor 4件です。

公開済み成果物と source commit の対応、SPM tag の扱い、`develop` 更新時の CI 保証に、実装前に解消すべき設計上の穴があります。現状のまま実装へ進むべきではありません。

静的レビューのため、ビルド・テスト・ファイル書き込みは行っていません。

## 照合した規約

- `kasane/handbook/cross/verification-ci.md`
- `kasane/handbook/cross/ci-script-deletion.md`
- `kasane/lessons/code-review.md` L-001
- cross/ADR-0008、0009、0016、0017、0022、0024
- `kasane/concepts/cross/architecture/consumer-verification.md`
- 現行 workflow、Gradle 発行設定、KsSettingsView の翻案元一式

## 指摘事項

### [🟠 Major] accepted ADR-0016 と README 更新方式が衝突している

**該当箇所**: `proposal.md:40`、`design.md:49-58`

**問題点**: proposal は ADR-0016 と「衝突なし」としていますが、accepted の `kasane/decisions/cross/0016-branch-model-develop-main.md:20` は README の version 置換をリリース PR 内で行うと明記しています。workflow がリリース後に `develop` へ直接 commit する方式はこれを変更します。ADR-0024 はまだ proposed なので、accepted ADR を上書きする根拠になりません。

**推奨修正**: 実装前に ADR-0016 を正式に amend するか、置換をリリース PR 内へ戻してください。proposal の「衝突なし」も結果に合わせて修正が必要です。

### [🟠 Major] version 置換 commit が accepted の CI 保証を迂回する

**該当箇所**: `design.md:51-53`、`specs/release-workflow/spec.md:163-183`

**問題点**: 設計は「`GITHUB_TOKEN` の push は他 workflow を起動しない」ことを意図的に利用しています。しかし ADR-0017 と handbook は `develop` push ごとに lint を実行すると定め、ADR-0022 は README 一致検査も毎回実行するとしています。したがって、この commit だけは secret scan、identity lint、README lint 等を一切通りません。GitHub 公式にも、`GITHUB_TOKEN` による push は通常、新しい workflow run を起動しないと明記されています。[GitHub Docs](https://docs.github.com/en/actions/how-tos/write-workflows/choose-when-workflows-run/trigger-a-workflow)

**推奨修正**: GitHub App/PAT など CI を起動する資格情報で push するか、push 後に `workflow_dispatch`／`repository_dispatch` で当該 commit の CI を明示起動し、成功まで待つ契約を追加してください。後者は現行 `ci.yml` の変更が必要なため、「検証 CI は変更しない」という Non-Goal も見直す必要があります。

### [🟠 Major] 公開 SPM tag を「取り消せる操作」とみなせない

**該当箇所**: `proposal.md:5`、`proposal.md:37`、`specs/release-workflow/spec.md:59-75`

**問題点**: ゴールは「tag は publish 全成功後にのみ生まれる」ですが、仕様では SPM tag を KMP upload より前に公開し、失敗時にも残します。さらに放棄時は tag を削除するとしていますが、remote ref を削除しても既に clone／解決された tag は回収できません。Native iOS だけが一時的に公開済みになるため、厳密な lockstep や「取り消せる順」とは一致しません。ADR-0024 の `Consequences:53` も同じ矛盾を含みます。

**推奨修正**: 次のどちらを契約にするか決めてください。

- ゴールの「tag」を monorepo tag に限定し、SPM tag は先行公開される例外と明記する。公開後の tag は削除・再利用せず、放棄版として扱う。
- SPM tag も全成功後に限定するなら、KMP 発行前検証の仕組みを再設計する。

少なくとも「KMP 発行失敗後に version を放棄する」Scenario と、利用者から見える状態を追加すべきです。

### [🟠 Major] 再実行時に公開物と source commit の同一性を保証できない

**該当箇所**: `specs/release-workflow/spec.md:144-160`

**問題点**: 冪等性の判定は、SPM tag のツリー一致、代表 POM の存在、NuGet の `--skip-duplicate` に依存しています。前回の別 commit が一部を公開した後、同じ version を新しい `main` commit から再度 dispatch すると、既存成果物を skip しつつ新しい commit に monorepo tag を打てます。iOS のスナップショットが偶然同じなら SPM 照合でも検出できません。結果として、tag が示す source と Maven／NuGet の binary が静かに食い違います。

**推奨修正**: 部分 publish の続行は同一 workflow run の `run_attempt > 1` に限定してください。新規 run の初回 attempt で SPM tag または公開成果物だけが存在する場合は失敗させるのが安全です。必要なら source SHA／run ID を外部状態または release-state artifact に保存し、再開時に一致を検証する Requirement と Scenario を追加してください。

### [🟠 Major] concurrency は3件以上の dispatch をすべて保持しない

**該当箇所**: `specs/release-workflow/spec.md:33-44`、`tasks.md:28`

**問題点**: workflow-level の `concurrency.group: release` と `cancel-in-progress: false` は、既定では「実行中1件＋pending 1件」です。3件目が来ると、古い pending run は置き換えられてキャンセルされます。そのため「release workflow の実行はすべて直列化され、打ち切られない」という契約は成立しません。またロックは publish 開始時ではなく workflow 全体の開始時に取得されます。[GitHub concurrency documentation](https://docs.github.com/en/actions/how-tos/write-workflows/choose-when-workflows-run/control-workflow-concurrency)

**推奨修正**: 全 dispatch を保持するなら `queue: max` を明記してください。pending の置き換えを許容するなら、Requirement と Scenario をその挙動へ狭め、どの run がキャンセルされ得るかを記述してください。

### [🟠 Major] 中核となる復旧契約の検証計画が命題を判別できない

**該当箇所**: `tasks.md:7`、`tasks.md:44`

**問題点**:

- `--dry-run`／`--offline` では Central upload が発生しないため、upload 後にだけ出る `deployment id:` の抽出可否を確認できません。Vanniktech 0.37.0 が ID をログ出力するのは実 upload 後です。[Vanniktech changelog](https://vanniktech.github.io/gradle-maven-publish-plugin/changelog/)、[Sonatype Publisher API](https://central.sonatype.org/publish/publish-portal-api/)
- 部分 publish／再実行 Scenario は、初回リリースで偶然失敗した場合だけ実測し、失敗しなければ未確認の deviation にするとしています。これでは最重要の状態分岐を一度も通さず完了できます。

これは `kasane/lessons/code-review.md` L-001 の「機構を外しても同じ観測になる検証」に該当します。

**推奨修正**: KsSettingsView の spike と同様に、公開しない USER_MANAGED deployment を実 uploadして ID 抽出・状態照会・drop を確認するか、抽出器と状態機械をスクリプトへ分離して、各状態をモックした自己テストを必須化してください。再実行の自然発生待ちは受け入れ条件にしないでください。

### [🟡 Minor] Maven Central の手動 release が設定で固定されていない

**該当箇所**: `design.md:17-21`、`design.md:29-33`

**問題点**: 現行の Android／KMP はともに引数なしの `publishToMavenCentral()` です。Vanniktech 0.37.0 は `automaticRelease` の既定値を Gradle property から解決するため、`mavenCentralAutomaticPublishing=true` が入ると NuGet より前に自動公開され、設計順序が崩れます。[Vanniktech 0.37.0 source](https://github.com/vanniktech/gradle-maven-publish-plugin/blob/0.37.0/plugin/src/main/kotlin/com/vanniktech/maven/publish/MavenPublishBaseExtension.kt)

**推奨修正**: Android と KMP の両方で `publishToMavenCentral(automaticRelease = false)` を明示し、その変更と task graph の確認を tasks に追加してください。

### [🟡 Minor] KMP の反映待ちが5 publicationのうちrootしか見ない

**該当箇所**: `design.md:71-75`、`specs/release-workflow/spec.md:185-196`

**問題点**: KMP 消費者は root に加えて Android と iOS 3 target の publication を解決しますが、反映待ちは `ksdialogs-kmp` root POM だけです。root が先に取得可能になった場合、smoke が target publication の未反映で失敗し、「反映待ち」としての判別力がありません。

**推奨修正**: `ksdialogs-kmp`、`ksdialogs-kmp-android`、iOS 3 publication の全5 POMを待機対象にしてください。

### [🟡 Minor] version 正規表現が成果物間で一致していない

**該当箇所**: `proposal.md:11`

**問題点**: proposal と ADR-0024 の `^[0-9]+...` は `01.0.0` や `1.0.0-beta.01` を許しますが、design とデルタスペックはこれらを拒否します。

**推奨修正**: proposal と ADR-0024 も、design の先頭ゼロを拒否する正規表現へ統一してください。

### [🟡 Minor] KMP 署名検査の対象ファイル集合が決まっていない

**該当箇所**: `tasks.md:16`

**問題点**: 翻案元の `check-signatures.sh` は `aar`、`pom`、`jar`、`module` だけを列挙します。一方、KMP publication には `.klib`、`-cinterop-*.klib`、SwiftPM metadata JSON、tooling metadata JSON があります。「5 publication のレイアウトに対応」だけでは、どこまで `.asc` の対を要求するか判定できません。

**推奨修正**: KMP の発行対象拡張子と必須 publication 名を明記し、`.klib` または metadata の署名を1件欠かせた負ケースで検査が失敗することを受け入れ条件に加えてください。

## アクションプラン

1. ADR-0016、CI 保証、SPM tag の意味を先に再決定する。
2. 再実行を同一 run／同一 SHA に束縛し、部分公開時の新規 dispatch を定義する。
3. concurrency と Central manual release を設定として固定する。
4. Central 状態機械・署名・5 publication の検証計画を、強制失敗で判別できる形へ変更する。
5. proposal／design／spec／tasks の正規表現と用語を統一してから再レビューする。


## 突き合わせ結果 (2026-09-10)

ホスト側の自己レビュー (ksn-propose Step 8、2 周) は整合性チェックリストを通過していたが、上位層 (accepted ADR) との衝突と GitHub Actions の仕様上の制約を見落としていた。相方のみの指摘で根拠が強いものを採用した。

| # | 指摘 | 採否 | 反映 |
|---|---|---|---|
| Major 1 | accepted の cross/ADR-0016 (README 置換はリリース PR 内) と R4 が衝突 | **採用** (ホスト側の見逃し) | cross/ADR-0024 に `amends: [cross/0016]` と置換範囲の 1 文、proposal Impact と design Decision 4 の理由を修正 |
| Major 2 | `GITHUB_TOKEN` の push は検証 CI を起動せず lint を迂回 | **採用** | publish job が push 前に lint 3 種 (local-path / identity / README 最小例) を掛ける契約を spec / design / tasks 4.5 / ADR-0024 に追加。PAT 案は却下として design 代替案 D に記録 |
| Major 3 | 公開 SPM tag は削除しても回収できず「取り消せる操作」ではない | **採用** (オーナー判断 2026-09-10: 案 A) | ゴールの「tag」は monorepo tag を指す例外扱いを proposal / design / ADR-0024 に明記、放棄 version は欠番 (Requirement「同じ version での再実行」に契約と Scenario)。ゴール文の読み替えは蒸留時に ksn-roadmap |
| Major 4 | 別 commit からの新規 dispatch で binary と source が食い違う | **採用** | 部分 publish の続行を同じ run の再試行 (`run_attempt` ≥ 2) に限定。Requirement「同じ version での再実行」に契約と Scenario を追加、design Decision 7 / tasks 4.5 / 5.3b / ADR-0024 に反映 |
| Major 5 | concurrency は 3 件目で古い待ちを置き換える | **採用 (契約を狭める)** | spec「段の構成と順序」の記述と Scenario を実際の挙動 (待ちは最新 1 件) に合わせた |
| Major 6 | 復旧契約の検証計画が判別力を持たない (L-001) | **採用** | tasks 1.1 を Central Portal への実 upload (USER_MANAGED、drop で回収) に変更、状態分岐を `central-resume.sh` に切り出して `--selftest` 6 状態 (2.1b)、5.5 の「自然発生待ち」を撤回 |
| Minor 1 | `automaticRelease` が明示されていない | **採用** | tasks 3.0 (android/ と kmp/ に `automaticRelease = false`)、design Decision 2 |
| Minor 2 | KMP の反映待ちが root POM だけ | **採用** | 反映待ちを Android 2 + KMP 5 publication + NuGet 3 の 10 件に (spec / design Decision 6 / tasks 2.3) |
| Minor 3 | version 正規表現が proposal / ADR-0024 で緩い | **採用** | 先頭ゼロを拒否する形に統一 |
| Minor 4 | KMP の署名検査の対象集合が未定 | **採用** | spec「署名の生成確認」に拡張子集合と負ケース Scenario、tasks 2.2 に自己テスト |

採用 10 / 未解決 0 / 降格 0。
