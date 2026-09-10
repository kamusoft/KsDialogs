# レビュー結果: add-release-workflow (001 回目)

**日付**: 2026-09-10
**判定**: CHANGES_REQUESTED

## サマリー

publish の順序・同じ version での再実行・secrets と権限の範囲は、デルタスペックの Requirement を 1 行ずつ突き合わせた範囲でいずれも実体と一致していた。翻案元 (KsSettingsView) の初回レビューが見つけた 4 つの欠陥 (`maven-metadata-local.xml` を含む同一性比較 / drop 後の ID が artifact に残る / コマンド置換の終了ステータスを捨てる tag 検査 / upload 後・ID 保存前の取りこぼし) はいずれも写していない。

一方で、翻案の過程で失敗経路の artifact 保存にあった**ガードが落ちている**。翻案元は「拾い直した ID がある」「drop で消せた」ときだけ deployment ID の artifact を書き換えるが、本実装は 3 step すべてを無条件の `if: failure()` にしたため、外部状態の判定や artifact の download より前に落ちた attempt が、前の attempt の引き継ぎ用 ID を空で上書きする。Requirement「Maven Central の 2 枠の deployment」の「同じ実行の再実行から枠ごとに参照できる形で保存する」が成立しない経路が残っている。

## 照合した規約

| 文書 | 適用のきっかけ |
|---|---|
| handbook cross/comment-policy.md | always (workflow / スクリプト / csproj のコメント) |
| handbook cross/verification-ci.md | `.github/workflows/` を変えるとき |
| handbook cross/ci-script-deletion.md | `scripts/**` `.github/workflows/**` のスクリプトを作る・翻案する・レビューするとき |
| handbook cross/local-development-setup.md | 手元での自己テストの実行 |
| handbook cross/release-procedure.md | 本変更で新設された手順書そのもの |
| decisions cross/ADR-0016 (accepted) / 0017 (accepted) / 0022 (accepted) / 0009 (accepted) | ブランチモデル・検証 CI の構成・lint の検査集合・lockstep |
| decisions cross/ADR-0024 (**proposed**) | publish の順序と README 置換。proposed のため、これを根拠にした CHANGES_REQUESTED は出していない |
| lessons code-review.md L-001 / lessons process.md L-001・L-002 | 証跡の判別力・姉妹面の照合・互換主張の範囲 |

## 実行した検証

- `scripts/release/*.sh --selftest` 7 本と `set-readme-version.py --selftest`: 全件「失敗なし」。`set-readme-version.py` は実物 14 ファイル 26 行を確定して置換・検査まで通ることを確認
- `actionlint`: `release.yml` に対する指摘は shellcheck の info / style 3 件のみ (SC2012 × 2、SC2001 × 1)。error 0 件
- `scripts/{local-path-lint,identity-lint,comment-policy-lint,readme-example-lint}.py`: いずれも exit 0
- `python3 scripts/local-path-lint.py --paths ...` / `identity-lint.py --paths ...` が実在し exit 0 で通ること (publish job が `develop` 反映前に掛ける lint の疎通)
- `verify-consumer-{ios,android,maui,kmp}.yml` の `workflow_call` 入力 (`mode` / `version` / `artifact`) と `permissions: contents: read` を release.yml の呼び出し側と突き合わせ
- KMP の 5 publication の artifactId を `verification/kmp/check-dependencies.py:45-49` と突き合わせ、`check-signatures.sh` / `wait-for-registries.sh` の列挙と一致することを確認
- 翻案元 `../KsSettingsView/kasane/changes/archive/2026-09-04-add-release-workflow/review-001.md` / `review-002.md` の指摘 4 件を本実装に照合 (すべて解消済みであることを確認)

## 指摘事項

### [🟠 Major] 失敗経路の 3 step が無条件で、早期に落ちた再実行が deployment ID の引き継ぎを空で消す

**該当箇所**: `.github/workflows/release.yml:1232`, `.github/workflows/release.yml:1262`, `.github/workflows/release.yml:1290-1299`

**問題点**:

`Recover deployment ids from upload logs` / `Drop pending deployments` / `Store deployment ids after cleanup` の 3 step は条件が `if: failure()` だけで、publish job の**どの step で落ちても**走る。一方 `Download previous deployment ids` (`release.yml:743`) と `Prepare deployment id files` (`release.yml:753`) は `steps.state.outputs.publish-needed == 'true'` で守られ、しかも artifact の download 群より後ろに置かれている。

このため、次の順で引き継ぎが失われる。

1. attempt 1 が Android を upload し、`Upload deployment ids (android)` が artifact `central-deployment-id` に実 ID を保存する。その後 KMP 側で失敗し、drop できない状態 (VALIDATING など) だったため ID が artifact に残る
2. attempt 2 が `Re-check external state` (`release.yml:607`。`central-portal.sh published` が 200/404 以外を返せば exit 1)、`Select Xcode` (`release.yml:561`)、`Download Android artifacts` (`release.yml:678`。artifact は `retention-days: 7` なので期限切れで失敗しうる)、`Verify package names` (`release.yml:695`) のいずれかで落ちる
3. `Download previous deployment ids` は skip されているので `${RUNNER_TEMP}/deployment` は空。`Recover deployment ids from upload logs` が `mkdir -p` して 2 枠のファイルを**空で作り**、`Drop pending deployments` は空なので何もせず、`Store deployment ids after cleanup` が `overwrite: true` でその空のディレクトリを artifact に上書きする
4. attempt 3 は両枠とも `NONE` → `upload` と判定し、Portal に保留 deployment を**もう 1 件**作る。`central-resume.sh:40-41` 自身が「同じ version で保留が 2 件並ぶと release の対象が決まらない」と書いている状態になる

`release.yml:1291-1292` のコメント「失敗経路では直前の 2 step が 2 枠のファイルを必ず作り直しているので、ディレクトリの実在を条件に足す必要はない」は、まさにこの上書きを正当化してしまっている (ファイルは必ず作られるが、中身が空でも作られる)。

翻案元は同じ 3 step を `if: failure() && steps.recover-deployment.outputs.deployment-id != ''` / `... && (steps.maven.outputs.deployment-id || steps.recover-deployment.outputs.deployment-id) != ''` / `... && steps.drop-deployment.outputs.cleared == 'true'` で守っており (`../KsSettingsView/.github/workflows/release.yml` の該当 step)、2 枠化にあたってこのガードが落ちている。翻案元 review-001 の Major「失敗経路で drop した deployment ID が artifact に残り、再実行を塞ぐ」の裏返しの欠陥にあたる。

デルタスペック `specs/release-workflow/spec.md` の Requirement「Maven Central の 2 枠の deployment」の「両枠の deployment ID は upload の時点で得られなければ失敗し、**同じ実行の再実行から枠ごとに参照できる形で保存する** SHALL」と、Scenario「release の応答が失われても再実行で整合する」が、この経路では成立しない。

**推奨修正**: 次のいずれか (併用が望ましい)。

- 3 step を `if: failure() && steps.state.outputs.publish-needed == 'true'` にし、さらに `Download previous deployment ids` と `Prepare deployment id files` を `Re-check external state` の直後 (artifact の download 群より**前**) へ移す。こうすると「publish に進むと決めた attempt は必ず引き継ぎを読み込んでいる」が保証され、失敗経路が読み込み済みの状態だけを書き戻す
- または翻案元と同じく、`Store deployment ids after cleanup` を「拾い直した ID がある」または「drop で実際に消せた」ときだけ走らせる (`Drop pending deployments` に `cleared` 相当の output を持たせる)

なお、この防御が効かない場合の最終的な受け皿 (Central Portal の deployment 一覧で人が拾う) は `Summarize` と handbook `kasane/handbook/cross/release-procedure.md:175-181` にあるので、被害は「自動の引き継ぎが切れる」までにとどまる。

### [🟡 Minor] `develop` 反映 step の未保護のコマンドが、publish 完了後の release 全体を失敗させうる

**該当箇所**: `.github/workflows/release.yml:1191-1192`, `.github/workflows/release.yml:1219-1221`

**問題点**:

`Update install examples on develop` は `warn()` で「失敗しても警告にとどめる」経路を持つが、保護されているのは置換スクリプト・lint・`rebase`・`push` の 4 つだけで、`git fetch --quiet origin develop` / `git worktree add --quiet --detach "${work}" origin/develop` / `git -C "${work}" config` / `git -C "${work}" commit -a` は素通しになっている。この step は monorepo の tag と GitHub Release を作り終えた**後**にあるため、ここで想定外の失敗が出ると、4 形態すべてが公開済みなのに publish job が failure になり、`Drop pending deployments` (無害) が走ったうえで `wait-for-registries` と smoke 4 本が `needs: publish` により skip される。design Decision 4 と Requirement「README と Skill の version 置換」の意図 (「release を失敗にせず警告として報告する」) から外れる。

具体的に危ういのは `origin/develop` の解決である。直前の行は `git fetch --quiet origin develop` で、これが確実に更新するのは `FETCH_HEAD` であって remote-tracking ref `refs/remotes/origin/develop` ではない (checkout action が `remote.origin.fetch` に何を書くかに依存する)。後段の `rebase` は `FETCH_HEAD` を使っており、同じ step の中で参照の取り方が揃っていない。

**推奨修正**: `git worktree add` の commit-ish を `FETCH_HEAD` にして直前の fetch だけに依存させ、あわせて step 本体を関数か `{ ... } || warn "..."` で囲んで、この step のどの失敗も警告に落ちるようにする (公開が終わった後に release を赤くする理由が無い)。

### [🟡 Minor] `automaticRelease = false` の証跡が、指定の有無で結果が分かれない

**該当箇所**: `evidence/gradle-publish-tasks.txt` の「4. automaticRelease = false の効き」、`kasane/changes/add-release-workflow/tasks.md:24` (3.0)

**問題点**:

証跡は `./gradlew publishToMavenCentral -PmavenCentralAutomaticPublishing=true --dry-run` の出力に `enableAutomatic` が 0 件であることを根拠にしている。しかし `automaticRelease` は task の**プロパティ**であって task 名ではなく、`publishToMavenCentral` の task graph は自動 release の有無で変わらない。つまり `publishToMavenCentral(automaticRelease = false)` を書かなくても同じ観測 (一致 0 件) が得られ、この検査には「指定が効いているか」を判別する力が無い。lessons code-review L-001 (「その機構を丸ごと外しても同じ観測が得られないか」) に当たる。

コード変更 (`android/build.gradle.kts` / `kmp/ksdialogs-kmp/build.gradle.kts` の明示引数) 自体は plugin の API 契約から正しいので、実装の欠陥ではなく証跡の欠陥である。

**推奨修正**: 証跡を判別力のある形へ差し替えるか、判別できないことを明記する。判別できる代替としては、(a) plugin の API 契約 (引数を渡すと Gradle プロパティを読まない) を根拠として引く、(b) 実 upload を伴う tasks 1.1 で「upload 後に deployment が USER_MANAGED / VALIDATED で止まる」ことを確認したうえでそれを 3.0 の根拠にする、のいずれか。tasks 1.1 は未実施なので、現状は (a) に寄せて「task graph では判別できないため plugin の API 契約に依拠する」と書くのが正確。

### [🟡 Minor] 初回リリース後、`<version>` プレースホルダを説明する散文が置換結果と矛盾する

**該当箇所**: `README.md:54`, `README.md:74`, `README_ja.md:54`, `README_ja.md:74`, `skills/en/ksdialogs-maui/SKILL.md:30`, `skills/ja/ksdialogs-maui/SKILL.md:30`, `skills/en/ksdialogs-android/SKILL.md:31`, `skills/ja/ksdialogs-android/SKILL.md:31`, `skills/en/ksdialogs-kmp/SKILL.md:43`, `skills/ja/ksdialogs-kmp/SKILL.md:43`, `skills/en/ksdialogs-aiforms-migration/SKILL.md:47`, `skills/ja/ksdialogs-aiforms-migration/SKILL.md:47`

**問題点**:

`set-readme-version.py` が置き換えるのはコードブロック / 座標の行だけで、その周囲にある説明文は対象外である。現在の説明文は「package は NuGet にまだ公開していないため、下の `<version>` は入手した package の version を指す」「以下の `<version>` を release version に置き換える」「prerelease では `<version>` を `X.Y.Z-beta.N` などの正確な tag に置き換えます」といった、プレースホルダが残っていることを前提にした文になっている。

publish job は成功後に自動で `develop` へ置換 commit を push するため、初回リリースが通った瞬間から `develop` の README と Skill は「公開していない」と書きながら実 version を載せた状態になる。docs-refresh 2 回目は `kasane/changes/add-release-workflow/tasks.md:62` (7.4) で蒸留への申し送りになっているが、その間 (公開 → 蒸留) は利用者向け文書が矛盾したまま残り、`AGENTS.md` に足した「手で書き換えない」との組み合わせで、誰の担当かも読み取りにくい。

**推奨修正**: 初回リリースの完了条件 (群 7) に「散文の `<version>` 前提と『まだ公開していない』の記述を落とす」を明示的に含めるか、リリース PR (handbook `kasane/handbook/cross/release-procedure.md:129-134`) の docs-refresh の項に「初回だけは散文の未公開表現も外す」を 1 行足す。

### [🔵 Suggestion] `scripts/release/` の自己テストが CI に載らない

**該当箇所**: `kasane/changes/add-release-workflow/tasks.md:20` (2.5)、`kasane/handbook/cross/local-development-setup.md` の「リリース用スクリプトの自己テストを回す」

**問題点**: 2.5 の判断 (lint job に載せず handbook に手元手順を書く) 自体は妥当だが、`set-readme-version.py` の `TARGET_FILES` と正規表現は `skills/**` と README の編集で腐り、腐ったことに気づけるのは次のリリースの `package-maui` (失敗はするので安全側) か、誰かが手元で自己テストを回したときだけになる。実物のファイルを使う自己テスト (`set-readme-version.py --selftest` の「実物のファイルに対する疎通」) は数秒で終わり、`docs-refresh` や skills の改稿と同じ PR で壊れたことがすぐ分かる。

**推奨修正**: 蒸留の申し送りに「lint job へ `scripts/release/` の自己テストを足すか (cross/ADR-0022 の検査集合の一部改訂)」を再掲するか、少なくとも handbook `user-skill-writing-style.md` / `docs-refresh-timing.md` 側から「インストール例の行を触ったら `set-readme-version.py --selftest`」への導線を張る。

## 確認して問題がなかった観点

- **publish の順序** (Requirement「publish の順序」): スナップショット commit push (`release.yml:791`) → Android upload (`:821`) と wait-validated (`:932`) → 配信 tag push (`:951`) → KMP 発行 + upload (`:973`) と wait-validated (`:1073`) → NuGet push (`:1098`) → Maven release Android (`:1117`) → KMP (`:1128`) → monorepo tag (`:1139`) → Release (`:1156`) → `develop` 反映 (`:1177`)。spec の 8 段と 1 対 1 で一致
- **release 直前の VALIDATED 再確認**: workflow の step には無いが `central-portal.sh:255-256` (`cmd_release`) が VALIDATED 以外を失敗させるため、Requirement「Maven Central の 2 枠の deployment」の「枠ごとに VALIDATED を再確認してから」は満たされている
- **署名検査の位置**: Android は upload の前 (`release.yml:883`)、KMP も upload の前 (`:1036`)。Android の署名検査は配信リポジトリの tag push (`:951`) より前にあり、Scenario「署名鍵が渡っていなければ upload しない」の「配信リポジトリの tag push も実行されない」を満たす
- **同一性比較**: `compare-maven-artifacts.sh:18-24,72-73` が `.asc` / checksum 4 種 / `maven-metadata*.xml` を除外し、アーカイブはエントリ名 + 内容ハッシュで比較。翻案元 review-001 の Critical (`maven-metadata-local.xml` の `<lastUpdated>` で毎回失敗) を写していない
- **コマンド置換の終了ステータス**: 配信リポジトリ tag の検査 3 箇所 (`release.yml:154`, `:816`, `:958`) がいずれも代入形で、`case` / `if` の条件部に置いていない。翻案元 review-002 の Major / Minor を写していない
- **同じ version での再実行**: `check-resume-eligibility.sh` の判定 (attempt 1 + 外部状態あり + monorepo tag 無し → `fail-new-dispatch`、`other-commit` → `fail-tag-mismatch`、`same-commit` → `skip-all`) が Requirement の字面と一致し、`release.yml:662-672` は代入形なので exit 1 が step を落とす。自己テストが 12 組合せを網羅
- **secrets と権限の範囲**: workflow 既定 `permissions: contents: read` (`:39-40`)、publish だけが `environment: release` + `contents: write` + `id-token: write` (`:538-543`)。`secrets: inherit` は全 8 箇所の再利用可能 workflow 呼び出しに無し。`verify-*.yml` / `verify-consumer-*.yml` 側も `permissions: contents: read` を自前で宣言
- **version 形式の検査**: `release.yml:108-109` の正規表現を spec Scenario の 6 種 (`1.0` / `v1.0.0` / `1.0.0-pre.1` / `1.0.0-SNAPSHOT` / `01.0.0` / `1.0.0-beta.01`) で追い、すべて弾かれることを確認。checkout より前に置かれている
- **dry-run の遮断**: `publish` / `wait-for-registries` / `smoke-*` の 6 job すべてに `if: ${{ !inputs['dry-run'] }}`。`Set install example version` は package 段の作業木にしか触れず commit しない
- **反映待ちの対象**: `wait-for-registries.sh:38-53` が Maven 7 + NuGet 3 の計 10 件、間隔 30 秒・上限 2700 秒 (45 分)。KMP の 5 publication の artifactId は `verification/kmp/check-dependencies.py` と一致
- **`readme-example-lint.py` との共存**: README の最小例 4 つは `verification/` のソースと完全一致する利用コードで version 文字列を含まないため、`set-readme-version.py` の置換で lint が落ちることはない (実物で確認)
- **CI で動くスクリプトの削除**: `scripts/release/` 内の `rm -rf` は handbook `ci-script-deletion.md` に従う範囲。いずれも `mktemp -d` の後片付けで、引数の誤指定で回復困難な消失に至る構造にはなっていない
- **コメント規約**: workflow / スクリプト / csproj のコメントに作業文書のパス・変更識別子・ローカル通番・デルタスペック構文キーワードは無く、外部参照は `cross/ADR-0009` / `cross/ADR-0024` の ID 形式に限られている。`comment-policy-lint.py` も 0 件
- **必須 status check 10 件の実在**: `.github/workflows/ci.yml` の `pull_request` では本体検証 5 job が `github.event_name == 'pull_request'` で常に起動し、消費者検証 4 job も同条件、`lint` は無条件。handbook `release-procedure.md:47-60` の 10 件と名前 (`<job> / verify`) が対応する
- **deviation.md 記載の 3 件** (Android binding の `.xml` / `git worktree` 化 / publish の nupkg 名検査を download 直後へ) は合意済みとして扱い、指摘していない。3 件目については「取り消せない操作 (配信リポジトリへの push) より前」という置き方のほうが spec の意図に沿うと判断する

## アクションプラン

1. **[Major]** `release.yml` の失敗経路 3 step (`:1232` / `:1262` / `:1290`) にガードを足し、`Download previous deployment ids` と `Prepare deployment id files` を `Re-check external state` の直後へ前倒しする
2. **[Minor]** `Update install examples on develop` の commit-ish を `FETCH_HEAD` にし、step 全体を warn へ落とす形にする (`:1191-1221`)
3. **[Minor]** `evidence/gradle-publish-tasks.txt` の 4 節を、判別力のある根拠 (plugin の API 契約、または tasks 1.1 の実 upload) へ書き直す
4. **[Minor]** 初回リリースの完了条件か handbook のリリース PR 節に、散文の `<version>` 前提と「まだ公開していない」の除去を明示する
5. **[Suggestion]** `scripts/release/` 自己テストの CI 化 (または skills 改稿からの導線) を蒸留の申し送りに残す
