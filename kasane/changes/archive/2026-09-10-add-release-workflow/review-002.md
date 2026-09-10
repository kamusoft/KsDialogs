# レビュー結果: add-release-workflow (002 回目)

**日付**: 2026-09-10
**判定**: APPROVED

## サマリー

前回サイクルで採用された 6 件 (Critical 1 / Major 3 / Minor 2) はいずれも解消している。とくに再実行の続行可否は、`run_attempt` だけの判定から「外部状態が 1 つも無いことを自分で確認した試行だけが印を残す」形へ置き換わり、拒否された run の再実行が resume に化ける経路が閉じた。deployment ID の引き継ぎ・`skip-all` 後の後処理・nuget.org 照会の fail-open・`develop` 反映の warn 化・`automaticRelease` の証跡も、いずれも実体で確認できた。

修正による回帰は、publish の順序・secrets と権限・dry-run の遮断・failure 経路の相互作用を追った範囲では見つからなかった。`skip-all` の再実行でも後処理 2 step を走らせる変更が「完了済みの古い run を `Re-run all jobs` したとき `develop` のインストール例を旧 version へ書き戻しうる」という新しい面を作るが、`validate` の monorepo tag 照合がこの状況の大半を先に塞ぐため Suggestion にとどめる。

## 照合した規約

| 文書 | 適用のきっかけ |
|---|---|
| handbook cross/comment-policy.md | always (workflow / スクリプト / csproj のコメント) |
| handbook cross/verification-ci.md | `.github/workflows/` を変えるとき |
| handbook cross/ci-script-deletion.md | `scripts/**` `.github/workflows/**` のスクリプトを作る・翻案するとき |
| handbook cross/local-development-setup.md | 手元での自己テストの実行 |
| handbook cross/release-procedure.md | 本変更で新設された手順書そのもの |
| decisions cross/ADR-0009 / 0016 / 0017 / 0022 (いずれも accepted) | lockstep の単一 version・ブランチモデル・検証 CI の構成・lint の検査集合 |
| decisions cross/ADR-0024 (**proposed**) | publish の順序と README 置換。proposed のため、これを根拠にした CHANGES_REQUESTED は出していない |
| lessons code-review.md L-001 / lessons process.md L-001・L-002 | 証跡の判別力・姉妹面の照合・互換主張の範囲 |
| skills github-workflow-skill / kotlin-impl-skill | workflow の構造・Gradle Kotlin DSL の変更 |

## 実行した検証

- `scripts/release/*.sh --selftest` 8 本 (新設 `check-nuget-version.sh` を含む) と `set-readme-version.py --selftest`: 全件「失敗なし」。`set-readme-version.py` は実物 14 ファイル 26 行を確定して置換・検査まで通る
- `actionlint .github/workflows/release.yml`: error 0 件。shellcheck の info / style 3 件のみ (SC2012 × 2 @ `:347` `:566`、SC2001 × 1 @ `:1245`) で、`evidence/deployment-id-handoff-guard.txt` の記載と一致
- `scripts/{local-path-lint,identity-lint,comment-policy-lint,readme-example-lint}.py`: いずれも exit 0
- `.github/workflows/release.yml` の publish job の step 順序・`if` 条件・`set -e` 下での終了ステータスの伝播を全 step 読み取り
- `check-resume-eligibility.sh` / `check-nuget-version.sh` / `central-portal.sh` (`cmd_published` / `cmd_drop`) の分岐を、release.yml の呼び出し側の `case` と 1 対 1 で突き合わせ
- デルタスペック `specs/release-workflow/spec.md` の Requirement「同じ version での再実行」「Maven Central の 2 枠の deployment」「README と Skill の version 置換」、`specs/maui-nuget-distribution/spec.md` の Requirement「発行版の nupkg 名の検査」を実体と照合
- `evidence/` 7 本のうち、今回の争点にあたる `deployment-id-handoff-guard.txt` / `gradle-publish-tasks.txt` / `central-portal-upload-probe.txt` / `release-script-selftests.txt` を判別力の観点で読む
- `tasks.md` の `[x]` を成果物と証跡で照合 (1.1 は `evidence/central-portal-upload-probe.txt`、2.5 は handbook `local-development-setup.md` の新設節、5.3b は `check-resume-eligibility.sh` の自己テスト)。虚偽のチェックは見つからなかった
- `scripts/release/__pycache__/` は `.gitignore:71` に該当し追跡されないことを確認

## 前回指摘の解消状況

### (a) 🔴 Critical `run_attempt >= 2` だけでは外部状態の出所を証明できない → **解消**

`check-resume-eligibility.sh` に marker の読み書き (`marker-write` / `marker-state`) と 4 入力の判定 (`decide <試行回数> <tag> <外部状態> <marker>`) が入り、`release.yml` は `Download eligibility marker` (`:610`) → `Re-check external state` (`:622`) → `Upload eligibility marker` (`:720`) の順で回している。

- **外部状態ありで拒否された run の再実行が止まること**: 試行 1 は `fail-new-dispatch` (`check-resume-eligibility.sh:113-118`) で、印は書かれない (`release.yml:707-711` は `decision = proceed` のときだけ `marker-write`)。`decision="$(... decide ...)"` は代入形なので errexit が step を落とし、後続の `Upload eligibility marker` も走らない。その run を再実行すると試行 2 / 外部 present / 印 absent → `fail-no-marker` (`:126-131`)。自己テストの「[印と判定をつないだ経路]」がこの 2 段を明示的に通している (`check-resume-eligibility.sh:284-294`)
- **正規 run の再実行が resume すること**: 試行 1 が外部 none で `proceed` → marker を書いて upload → 部分 publish 後に失敗 → 再実行は marker `match` + 外部 present で `resume` (`:120-125`)。`marker-state` は version / commit / run id の 3 つを完全一致で見るため、別 run・別 commit から拾った印では `mismatch` になる (自己テスト `:303-311`)
- **印の書き込み条件 `proceed` の安全性**: `proceed` は「tag absent かつ外部状態 none」でしか返らない (`:95-111`)。この時点で取り消せない外部書き込みは 1 件も行われていないので、「この run が無の状態から入った」という主張は真である。逆に、印を書いた後に外部書き込みへ進む前で落ちた attempt (`Upload eligibility marker` 自体の失敗を含む) は、次の attempt でも外部 none のまま `proceed` に戻るだけで、resume を不当に塞がない。配信リポジトリの tag push (`:1011`) が KMP 発行・NuGet push・Maven release のすべてより前にあるため、「外部状態 none なのに publish が進んでいた」状態も作れない

spec `specs/release-workflow/spec.md:150`「部分 publish の続行は同じ run の再試行に限り」に対して、実装は run 単位よりさらに厳しい (印を残した試行の系列のみ) が、spec が成功させたい経路を塞いではいない。

### (b) 🟠 Major 失敗経路の 3 step が引き継ぎを空で上書きする → **解消**

`Download previous deployment ids` (`:737`) と `Prepare deployment id files` (`:750`) が `Download Android artifacts` (`:763`) / `Download MAUI packages` (`:770`) より前へ移り、失敗経路の 3 step (`:1309` / `:1339` / `:1370`) はいずれも `failure() && steps.deployment-ids.outputs.ready == 'true'` になった。`ready` は `Prepare deployment id files` が最後に出す output なので、引き継ぎの download が落ちた attempt では空のまま → 3 step が丸ごと skip され、artifact は前の attempt の内容を保つ。

証跡 `evidence/deployment-id-handoff-guard.txt` は判別力を満たしている。同じ release.yml から「ガードを外し、引き継ぎ読み込みを download の後ろへ戻した」写しを機械生成し、同じ検査が 7 件とも NG になることを示している (lessons code-review L-001 の「機構を丸ごと外しても同じ観測が得られないか」に答えている)。検査が構造 (順序とガードの実在) までしか示さないことも本文が明記していて、主張の範囲が証拠の範囲を超えていない (lessons process L-002)。

### (c) 🟠 Major `skip-all` が Release 作成と `develop` 反映まで skip する → **解消**

`Create GitHub Release` (`:1219`) と `Update install examples on develop` (`:1243`) から `if:` が外れ、`Push monorepo tag` (`:1200`) までが `publish-needed == 'true'` に限定された。

- `skip-all` (monorepo tag が起動 commit にある) では publish 群がすべて skip され、この 2 step だけが走る。tag は定義上存在するので `gh release create` は成立する
- dry-run では publish job 自体が `if: ${{ !inputs['dry-run'] }}` (`:535`) で skip されるため、2 step とも走らない
- 冪等性: `gh release view` が通れば「本文は更新しない」で `exit 0` (`:1224-1227`)。README 側は `git diff --quiet` で差分なしなら commit しない (`:1268-1271`)
- publish 経路で途中失敗した場合、この 2 step は既定の `success()` 判定により skip される (tag を打つ前に Release を作る経路は生じない)

`check-resume-eligibility.sh:46-49` の冒頭コメントと handbook `release-procedure.md:174` の表も同じ意味に揃っていて、deviation.md の 5 件目 (完了印の扱い) と矛盾しない。

### (d) 🟠 Major nuget.org の照会が fail-open → **解消**

`scripts/release/check-nuget-version.sh` が新設され、200 / 404 / それ以外を明示的に分けている。

- 404 → exit 1 (未公開)、200 かつ版一覧に完全一致 → exit 0、200 だが JSON として読めない / `versions` が配列でない / 本文が無い → exit 2、curl が応答を得られない (空 status・`000`) / 5xx / 429 → exit 2 (`check-nuget-version.sh:79-115`)
- 呼び出し側 (`release.yml:669-681`) は `found=0; ... || found=$?` の代入形で受け、`0` → 外部状態あり、`1` → 未公開、`*` → `::error::` + `exit 1`。exit 2 が「未公開」に読み替えられる経路は無い
- 版比較は完全一致 + 小文字正規化で、`1.0.1` の照会が `1.0.10` に当たらないことを自己テストが持つ (`:175`)
- 引数不正も `exit 2` (`:215-218`) なので、呼び出し側では判定不能 = 失敗に倒れる (安全側)

Maven 側 (`central-portal.sh:324-343` の `cmd_published`) も同じ 0 / 1 / 2 の契約で、`release.yml:652-663` が同型に扱っている。2 つの安全ゲートの形が揃っている。

### (e) 🟡 Minor `develop` 反映 step の未保護コマンド → **解消**

`git worktree add` の commit-ish が `FETCH_HEAD` になり (`:1260`)、直前の `git fetch origin develop` (`:1258`) だけに依存する形へ揃った。後段の `rebase FETCH_HEAD` (`:1296`) と参照の取り方が一致している。

step 内の外部コマンドはすべて `|| warn`・`if ! (...)` のいずれかで受けており (`:1258` `:1260` `:1265` `:1273` `:1282` `:1289` `:1292` `:1294` `:1296` `:1298`)、`warn()` は `::warning::` + `$GITHUB_OUTPUT` への記録の後 `exit 0` で抜ける。`git diff --quiet` (`:1268`) は `if` 条件なので errexit の対象外で、git 自体のエラー (exit 128) は次行の `changed=...` が warn で拾う。公開後に release を赤くする経路は残っていない。

### (f) 🟡 Minor `automaticRelease = false` の証跡に判別力が無い → **解消**

`evidence/gradle-publish-tasks.txt` の 4 節が、作業木の外に作った probe プロジェクトでの A/B へ差し替わった。同じ plugin 版・同じ `-PmavenCentralAutomaticPublishing=true` に対して、(a) 引数なしでは task graph に `enableAutomaticMavenCentralPublishing` が出る / (b) `automaticRelease = false` では出ない、と観測が分かれる。「機構を外しても同じ観測になるか」に答えており L-001 を満たす。

補強として `evidence/central-portal-upload-probe.txt` に実 upload の `Uploaded bundle to Central Portal as USER_MANAGED, deployment id: <uuid>` が残っており、前回推奨した代替 (b) の裏取りも実際に取れている。

### 前回「問題なし」とした観点の再確認 (回帰なし)

- **publish の順序**: スナップショット commit push (`:851`) → Android upload (`:881`) と wait-validated (`:992`) → 配信 tag push (`:1011`) → KMP 発行 + upload (`:1033`) と wait-validated (`:1133`) → NuGet push (`:1158`) → Maven release Android (`:1177`) → KMP (`:1188`) → monorepo tag (`:1199`) → Release (`:1219`) → `develop` 反映 (`:1243`)。spec の 8 段と一致し、修正で入れ替わっていない
- **secrets と権限の範囲**: workflow 既定 `permissions: contents: read` (`:40-41`)、publish だけが `environment: release` + `contents: write` + `id-token: write` (`:542-547`)。`secrets: inherit` は再利用可能 workflow の呼び出し 8 箇所すべてに無い。新設の marker artifact は資格情報を含まず (version / commit / run id のみ)、`Upload eligibility marker` は追加の権限を要求しない
- **failure 経路の相互作用**: `Drop pending deployments` は `central-portal.sh drop` が削除不能状態で 0 を返す (`central-portal.sh:301-314`) ため、片枠の状態で errexit が止まって他枠を取りこぼす経路は無い。`Store deployment ids after cleanup` の条件は `failure()` を含むので、`Drop` が通信失敗で落ちても書き戻しは走る
- **`skip-all` 時の failure 経路**: `Prepare deployment id files` が skip されて `ready` が空になるため、後処理だけが残った再実行で failure が起きても deployment artifact に触らない
- **`Verify package names` の 2 回**: package-maui (`:425`) と publish の download 直後 (`:780`)。deviation.md の 4 件目 (「push 直前」→「download 直後」) に沿う
- **tasks.md**: 新たに `[x]` になった項目 (1.1〜1.5 / 2.x / 3.x / 4.x / 5.1 / 5.2 / 5.3b / 6.2〜6.4) はいずれも成果物または `evidence/` に対応物がある。dispatch を要する 5.3〜5.5 と 6.1、群 7 は未チェックのまま

## 指摘事項

### [🔵 Suggestion] 完了済みの古い run を `Re-run all jobs` すると、`develop` のインストール例が旧 version へ戻りうる

**該当箇所**: `.github/workflows/release.yml:1243`

**問題点**:

(c) の修正で `Update install examples on develop` が `skip-all` の再実行でも走るようになった。これは「tag を打った直後に落ちた run を完了させる」ために必要な変更だが、副作用として **既に全工程が完了した run を後から `Re-run all jobs` した場合にも走る**。

その再実行では `GITHUB_SHA` が当時の commit なので `Verify monorepo tag` (`:129`) と `Re-check external state` (`:622`) は `same-commit` で通り、`skip-all` として Release 作成 (既存なので no-op) の後に README 置換が走る。このとき `develop` の先端が既に新しい version を指していれば、`set-readme-version.py` は無条件に引数の version へ置換するため、`develop` のインストール例が**古い version へ書き戻されて push される**。

現実にこれが起きるには「main が進んでいない状態で develop の版表記だけが先へ進んでいる」必要があり、`develop` の版表記を書くのはこの workflow だけ (AGENTS.md の新しい行) なので、通常のリリース運用では成立しにくい。ただし、次のリリース (または人の手) までインストール例が旧 version のまま残るという後戻りは、`warn` にも要約にも現れない (置換自体は成功するため)。

**推奨修正**: 蒸留の申し送りか handbook `release-procedure.md` の「再実行」節に、「完了した run の `Re-run all jobs` は使わない (インストール例が旧 version へ戻る)」を 1 行足す。実装で塞ぐなら、`Update install examples on develop` を「置換前の `develop` の版が入力 version 以下のときだけ commit する」形にする案があるが、比較のために SemVer の順序を workflow に持ち込むことになるので、費用対効果は手順書側のほうが良いと考える。

## 確認して問題がなかった観点

- **足場アーティファクト**: `proposal.md` / `design.md` / `specs/**` は未変更 (`git status` は `tasks.md` のみ M)。実装中の書き換えは無い
- **deviation.md の 5 件**: いずれも合意済みとして扱い、違反として指摘していない。5 件目 (完了印の扱い) は実装 (`release.yml:1216-1218` のコメント・`check-resume-eligibility.sh:46-49`) と handbook (`release-procedure.md:174`) の 3 箇所で同じ意味に揃っている
- **付随修正**: `PlatformDialogContent.cs` の `<param>` タグ移動は、3.1 で XML ドキュメント生成を有効にしたことで初めて出た CS1572 の解消で、公開面・挙動に触れない。ksn-core の同梱条件に収まる
- **コメント規約**: 新設・改訂したコメント (workflow・`check-nuget-version.sh`・`check-resume-eligibility.sh`・csproj 3 本・Gradle 2 本) に作業文書のパス・変更識別子・ローカル通番・デルタスペック構文キーワードは無い。外部参照は `cross/ADR-0009` / `cross/ADR-0024` の ID 形式のみ。`comment-policy-lint.py` も 0 件。コメントは単独で読めており、ADR の ID だけに説明を委ねている箇所は無い
- **CI で動くスクリプトの削除**: 新設 2 本の `rm -f` / `rm -rf` はいずれも `mktemp` / `mktemp -d` の後片付けで、handbook `ci-script-deletion.md` の範囲
- **Gradle 変更 (kotlin-impl-skill の観点)**: `publishToMavenCentral(automaticRelease = false)` は名前付き引数で意図が読め、android / kmp の 2 ファイルで同じ書き方・同じ趣旨のコメントに揃っている。副作用や状態を持ち込まない宣言の変更で、DSL の慣用から外れていない
- **handbook の追随**: `cross/index.md` に `release-procedure.md` の行、`local-development-setup.md` に自己テストの節と `applies-when.tasks` の追加、`verification-ci.md` に release からの呼び出しの 1 段落。3 本とも「適用のきっかけ」と本文が対応している
- **`.gitignore`**: `scripts/release/__pycache__/` は `.gitignore:71` の `__pycache__/` に該当し、コミット対象に混ざらない

## アクションプラン

1. **[Suggestion]** `release-procedure.md` の再実行節に「完了した run の `Re-run all jobs` は使わない」を足す (蒸留の申し送りでも可)
2. 前回サイクルで降格した 2 件 (初回リリース後の散文の `<version>` 前提 / `scripts/release/` 自己テストの CI 化) は、それぞれ docs-refresh 2 回目と cross/ADR-0022 の一部改訂として蒸留へ申し送るという判断が有効なままであることを確認した。本レビューでは再掲しない
