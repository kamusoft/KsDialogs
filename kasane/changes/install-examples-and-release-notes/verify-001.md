# verify-001: install-examples-and-release-notes

- 検証日: 2026-09-13
- 対象: `specs/install-examples/spec.md` (ADDED 2 Requirement / 9 Scenario)、`specs/release-workflow/spec.md` (MODIFIED 1 / ADDED 2 / REMOVED 1、26 Scenario)
- 判定: **INVALID** (❌ 1 件 — 実装の欠落ではなく未実行のオーナー操作)

同じ作業木に同居する change `backport-registry-wait-hardening` の変更
(`scripts/release/central-portal.sh` / `wait-for-registries.sh` / `check-time-budget.py` /
`check-publish-step-order.py` / `kasane/decisions/cross/0026-*.md`) は検証対象外とした。
`.github/workflows/release.yml` と `.github/workflows/ci.yml` は両 change が触るため、本 change に属する
差分だけを見ている。

---

## 1. install-examples

### Requirement: インストール例の version 表記

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 埋めずに使うと依存解決が失敗する | `README.md` / `README_ja.md` / `skills/` 14 ファイルの計 24 行 (コードブロック内) + 散文 4 行が `{version}`。具体 version の残存なし (grep で 0 件) | `scripts/install-example-lint.py` の `check_files` プレースホルダ検査、`--selftest` [プレースホルダ] 2 ケース。**解決失敗そのものは未検証** (tasks 10.6 未実行) | ✅ 一致 |
| 最新版の案内から目的の版に着地できる | 対象 16 ファイルすべてに `https://github.com/kamusoft/KsDialogs/releases/latest` (`grep -rl` で 16/16) | `install-example-lint.py:check_files` の案内検査、`--selftest` [最新リリースへの案内] | ⚠️ 実装は完了。ただし着地先の成立は tasks 1.1 の完了が前提 (下記 ❌ 参照) |
| リリースしてもインストール例は変わらない | `.github/workflows/release.yml` から置換呼び出し 2 箇所 (package-maui の `Set install example version`、publish の `Update install examples on develop`) を削除。validate 段に一致検査 step は無い (HEAD にも存在せず、残っていたのは「ここでは検査しない」旨のコメントのみで、これも削除済み) | なし (workflow 実行時にしか観測できない) | ✅ 一致 (静的確認) |
| 配布物に同梱される README も具体 version を持たない | package-maui job の pack 前の置換 step 削除。`maui/KsDialogs.Maui/KsDialogs.Maui.csproj:22` の `<PackageReadmeFile>README.md</PackageReadmeFile>` は据え置きで、同梱されるのはプレースホルダのままの README | なし (pack 実行時にしか観測できない) | ✅ 一致 (静的確認) |

### Requirement: インストール例の契約の検査

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 具体 version への逆戻りを検出する | `scripts/install-example-lint.py` の `check_files` (プレースホルダ比較、`{relative}:{行番号}` 付きで出力) | `--selftest` [プレースホルダ] — SwiftPM / Maven 2 ケース。ファイル・行・実際の値の出力まで確認 | ✅ 一致 |
| SwiftPM の宣言が `exact:` でなくなったことを検出する | 同 `find_swiftpm` が `from`/`exact` を `keyword` として捕り、`check_files` が `exact` 以外を違反にする。1 行形と複数行形の両方に対応 | `--selftest` [SwiftPM の解決方法] | ✅ 一致 |
| 検査対象に登録されていない文書を検出する | `check_structure` + `declaring_documents` (`skills/{en,ja}/` を `os.walk` で走査し、**散文の宣言も数える**) + `registered_documents` (`TARGET_FILES` 由来) の双方向差分 | `--selftest` [対象表と実構成の突合] 4 ケース — 未登録 Skill / 未登録 `references/` 文書 / 実在しない Skill / 宣言を失った文書 | ✅ 一致 |
| 英日の構成のずれを検出する | `check_structure` の `actual_skills` 差分と `declaring` 差分、`check_language_parity` の種別別本数比較 | `--selftest` [英日の構成] 2 ケース (片言語だけの Skill 追加 / 片言語だけの宣言追加) | ✅ 一致 |
| 散文の記述は検査を通る | `code_block_flags` がフェンスで内外を判定し、`check_files` は `in_code` の Occurrence だけを見る。限界は module docstring (27〜30 行目)・`PROSE_DECLARATIONS` (4 行)・成功メッセージ・`kasane/handbook/cross/install-examples.md`「機械検査」節に記録 | `--selftest` [散文は見ない] (コードブロック外の `ksdialogs-core:1.2.3` で exit 0) | ✅ 一致 |

**対象の明示**: `TARGET_FILES` は README 2 枚 + `skills/` 14 ファイルを種別別の期待本数つきで持ち、走査に代替していない (デルタスペックの「検査する側が明示的に持つ」を満たす)。

---

## 2. release-workflow

### MODIFIED Requirement: tag と GitHub Release

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| beta の版でも最新リリースとして参照できる | `.github/workflows/release.yml` `Push monorepo tag` (monorepo tag、既存なら同一 commit を確認して skip) と `Create GitHub Release`。prerelease の `case` 分岐を削除し `gh release create --title --notes-file --latest`。配信リポジトリの tag は KMP 発行前の既存 step が作る | なし (workflow 実行時にしか観測できない) | ⚠️ 自動検証不能 (step の記述は静的に確認済み) |
| 既に発行済みの Release も最新の選別に乗る | tasks 1.1 (オーナー操作として保留)。`gh release list` は `0.1.0-beta.1` が **Pre-release のまま**であることを示す | なし | ❌ 未実行 |
| 確定したノートが無ければ Release を作らない | 同 step の `notes="${RUNNER_TEMP}/release-notes/notes.md"` / `[ ! -s "${notes}" ]` → `::error::` + `exit 1`。取得元の `Download release notes` step は publish の要否で分岐させていない (レジストリ公開済みの再実行でも本文が揃う) | なし (workflow 実行時にしか観測できない) | ⚠️ 自動検証不能 (step の記述は静的に確認済み) |

### ADDED Requirement: Release ノートの内容

実装は `scripts/release/build-release-notes.py` (689 行、`collect` / `render` / `--selftest`) と
`.github/workflows/release.yml` の validate 段 4 step (`Decide release notes scope` / `Build release notes` /
`Upload release notes` / `Skip release notes`)、publish 段 1 step (`Download release notes`)。
テスト欄の「自己テスト」は `python3 scripts/release/build-release-notes.py --selftest` (37 観測点、全 OK) の個別ケース名。

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 利用者向けの変更が種別ごとに並ぶ | `KINDS` (breaking / feature / fix / docs の定義順) と `render_notes` の `grouped` | 自己テスト「種別ごとにまとまり、定義順に並ぶ」「項目の無い種別の見出しは出さない」。加えて手元で `render` を実行し、4 種別が定義順・空見出しなしで出ることを確認 | ✅ 一致 |
| 利用者向けでない変更は載らない | `NONE_MARKER = "- none"` と `parse_items` (`none_written` で項目 0 を許す) | 自己テスト「`- none` だけの pull request からは項目が載らない」「pull request テンプレートの未編集状態を受理する」 | ✅ 一致 |
| 複数の pull request 分が連結される | `collect_pulls` の `found: dict[int, dict]` による pull request 単位の重複除去、`render_notes` の `(#{number})` 付与 | 自己テスト「複数 pull request 分が連結される」「出所の pull request 番号が付く」「同じ pull request は 1 度だけ数える」「異なる pull request の同じ文面は両方残る」 | ✅ 一致 |
| Release を持たない tag は起点にならない | `select_base` が `api.paged("/repos/{repo}/releases")` の結果だけを候補にする (tag 一覧を見ない) | 自己テスト「Release を持たない tag は起点にならない」 | ✅ 一致 |
| 今回の tag がある再実行でも対象が変わらない | `select_base` の `tag == version` で除外 | 自己テスト「今回の version の tag は起点にならない」 | ✅ 一致 |
| draft の Release は起点にならない | `select_base` の `release.get("draft")` で除外 | 自己テスト「draft の Release は起点にならない」 | ✅ 一致 |
| 対象 commit の祖先でない Release は起点にならない | `first_parent_chain` で作った `distance` に無い commit を除外し、距離最小を選ぶ | 自己テスト「対象 commit の祖先でない Release は起点にならない」「直前の公開済み Release が起点になる」 | ✅ 一致 |
| 初回のリリースでは履歴の最初から集める | `select_base` が `None` を返したとき `commits_in_range(None, sha)` が `git rev-list sha` を使う | 自己テスト「公開済み Release が無ければ起点も無い」「起点が無ければ履歴の最初から集める」。記載不備での停止は解析側の自己テストが担う | ✅ 一致 |
| 開発ブランチ宛ての pull request は対象にならない | `TARGET_BASE_BRANCH = "main"` と `collect_pulls` の base 判定 | 自己テスト「開発ブランチ宛ての pull request は対象にならない」 | ✅ 一致 |
| 記載の無い pull request があれば publish の前に止まる | `extract_section` の `NotesError`、`parse_pulls` が `#<番号>:` を前置。実行位置は validate job の `Build release notes` (publish は `needs: validate`) | 自己テスト「セクションが無いと失敗する」「失敗の原因が pull request 番号で分かる」 | ✅ 一致 |
| 認識できない記載があれば止まる | `parse_items` — 見出し重複 / 空セクション / 未知の種別 / 非 `- ` 行 / 地の文 / 空の説明 / `- none` の併記をすべて `NotesError`。黙って読み飛ばすのは空行のみ | 自己テスト「見出しが 2 つあると失敗する」「空のセクションは失敗する」「不正な種別は失敗する」「認識できない非空行は失敗する」「地の文は失敗する」「空の説明は失敗する」「`- none` と項目の共存は失敗する」 | ✅ 一致 |
| 検査の後に本文が編集されても公開内容が変わらない | validate の `Upload release notes` (`if-no-files-found: error` / `overwrite: true`) → publish の `Download release notes`。publish 側に pull request 本文の読み直しは無い | なし (workflow 実行時にしか観測できない) | ⚠️ 自動検証不能 (step の記述は静的に確認済み) |
| 開発ブランチからのリハーサルでは対象を集めない | `Decide release notes scope` の条件を 1 箇所で評価し `collect` 出力に渡す。`dry-run == true` かつ ref ≠ `refs/heads/main` で `collect=false`。`Skip release notes` がその旨をログに残す | なし (workflow 実行時にしか観測できない) | ⚠️ 自動検証不能 (条件式は静的に確認済み) |
| main からのリハーサルでは実際の経路を通る | 同条件で `collect=true` (収集・検査・整形・artifact 保存まで本番と同じ)。publish job は `if: ${{ !inputs['dry-run'] }}` で走らないため Release だけが作られない | なし。tasks 10.5 (`main` からの dry-run 起動) 未実行 | ⚠️ 自動検証不能 (条件式は静的に確認済み) |
| 初回のリリースでは比較の位置を含めない | `render_notes` の `base_tag` が偽のとき `This is the first release.` | 自己テスト「初回のリリースでは比較の位置を含めない」+ 手元 `render` 実行で確認 | ✅ 一致 |
| 対象が無いときも本文が決まる | `render_notes` の `No user-facing changes in this release.` と空見出しの抑止 | 自己テスト「対象が 0 件でも本文が決まる」+ 手元 `render` 実行 (対象 0 件) で確認 | ✅ 一致 |
| ノートの組み立てが単独で検査できる | `render` サブコマンド (API も git も使わない。`--pulls` の JSON だけを入力に取る) | 自己テスト全 19 観測点が `render_notes` / `parse_items` を直接叩く。加えて **tasks 10.3 を本検証で実施** — pull request テンプレート + 模擬本文 2 件で期待どおりのノートを得た | ✅ 一致 |

**ページ送り**: `GitHubApi.paged` が上限未満のページに達するまで辿る。自己テスト「ページ送りで全件を取る」「最後のページまで辿る」「2 ページ目の項目がノートに載る」「上限ちょうどのページの次を空で受けて終わる」「上限ちょうどのページの後も次を要求する」で本体実装を通して検査している (tasks 5.6 の「写しではなく本体を呼ぶこと」を満たす)。

**権限と履歴**: validate job に `permissions: {contents: read, pull-requests: read}` を両方列挙。checkout は `fetch-depth: 0` で全履歴と tag を取るため `rev-list --first-parent` と tag 解決が成立する。

### ADDED Requirement: リリース手順の定型実行

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 記載の下書きが得られる | `.agents/skills/release/SKILL.md`「`## Changes` の下書きを作る」節 (`git log --no-merges --reverse origin/main..origin/develop`、利用者から見える変化だけ、`- <種別>: <説明>`、書式の正は `.github/pull_request_template.md`) | なし (人手運用) | ✅ 一致 |
| 先行する pull request の変更が下書きに混ざらない | 同節の範囲指定が `origin/main..origin/develop` (前回リリース以降の全変更ではない)。理由も同節に明記。handbook 段 2 も同じ範囲を書く | なし | ✅ 一致 |
| 事前確認が実行される | `kasane/handbook/cross/release-procedure.md`「1. 事前確認」(`gh run list --branch develop --workflow=ci.yml`、失敗なら先へ進まない) + SKILL.md「進め方」2 の到達状態報告 | なし | ✅ 一致 |
| 各段が順に実行できる | handbook が 5 段 (事前確認 / リリース PR / 起動 / 見守り / 公開後の確認) をそれぞれ「到達状態」つきで持つ。SKILL.md が段ごとの報告形 (段・到達状態・根拠・次にすること) を持つ | なし | ✅ 一致 |
| 失敗したときに手順書の該当箇所が示される | SKILL.md「失敗を検出したとき」(位置の特定 → handbook の**その位置に対応する**記述を引用 → 該当なしなら明示 → 再実行の判断は人)。handbook「失敗したとき」を validate 段と publish 段に分割し、validate 段には `render` での手元確認手段も置いた | なし | ✅ 一致 |
| 手順を変えたときに道具を直さずに済む | SKILL.md は段の順序・節の並び・コマンドを持たず、起動のたびに handbook を先頭から読む旨を明記。自分で持つのは下書き手順と判断の境界の 2 つだけ | なし (構造の確認) | ✅ 一致 |

`.claude/skills/release` → `../../.agents/skills/release` の symlink を確認 (tasks 7.3、docs-refresh と同じ形)。

### REMOVED Requirement: README と Skill の version 置換

| 撤去対象 | 確認結果 | 状態 |
|---|---|---|
| `scripts/release/set-readme-version.py` (711 行) | 削除済み (`git status` で ` D`) | ✅ |
| validate 段の一致検査 step | 存在しない。HEAD の validate 段にも検査 step は無く、残っていたのは「ここでは検査しない」旨のコメントのみ。そのコメントも削除済み | ✅ |
| package-maui job の pack 前の置換呼び出し | `Set install example version` step を削除済み | ✅ |
| publish job の `develop` への書き戻し | `Update install examples on develop` step (約 60 行) を削除済み。`Summarize` の `KS_README_WARNING` 行と env も削除済み | ✅ |
| `AGENTS.md` の例外規定 | 該当 1 行を削除済み。`CLAUDE.md` は HEAD にも同文を持たない (grep 0 件) ため対応不要 | ✅ |
| `.github/release.yml` (ラベル分類) | 削除済み。`--generate-notes` / `--prerelease` の参照も workflow に残っていない (grep 0 件) | ✅ |

`set-readme-version` の残存参照は `kasane/roadmaps/archive/` (履歴)、`kasane/concepts/log.md` (append-only の記録)、
`kasane/decisions/cross/0026-*.md` (別 change の ADR。0029 が amends で解消)、`.claude/worktrees/` (別作業木) のみで、
いずれも実行経路ではない。`kasane/concepts/cross/architecture/release-workflow.md:114` は置換機構を現在形で記述したままだが、
tasks.md の「蒸留への申し送り」に実装タスクではない項目として明記されており、本検証の対象外とする。

---

## 3. 追加検査

### tasks.md の整合

- 虚偽チェックなし。チェック済みの 2.1〜9.4 はすべて対応する実装を確認できた。
- 未チェックは 1.1 と 10.1〜10.6 で、状態表示と実態が一致している。
- **本検証で 10.1 / 10.2 / 10.3 / 10.4 を実施し、いずれも成功** (下記)。10.5 (`main` からの dry-run 起動) と 10.6 (消費者での解決確認) は未実施。
- tasks.md 冒頭の実施順制約のうち「グループ 1 (印の解除) は 3.4 (案内の追加) より前」が守られていない (下記 ❌)。冒頭の制約文は「グループ 4 の印の解除は 1 の案内追加より前」と group 番号が入れ替わった書き方になっており、1.1 本文の記述 (「README に最新版の案内を書く前に行う」) が正。

### 逆流検査

change ディレクトリは未 commit のため git 履歴が無く、mtime で確認した。

| ファイル | mtime |
|---|---|
| `proposal.md` | 09-13 15:54 |
| `design.md` | 09-13 15:56 |
| `specs/install-examples/spec.md` | 09-13 15:56 |
| `specs/release-workflow/spec.md` | 09-13 15:57 |
| 実装ファイル (`scripts/install-example-lint.py` 〜 `kasane/handbook/cross/install-examples.md`) | 09-13 17:22〜17:38 |
| `tasks.md` / `deviation.md` | 09-13 17:44 / 17:45 |

足場 (proposal / design / specs) は実装開始 (17:22) より前で止まっており、**実装期間中の書き換えは無い**。

### テストの実行

lint job 相当の検査を手元で全数実行し、すべて exit 0。

| コマンド | 結果 |
|---|---|
| `python3 scripts/install-example-lint.py` | 0 (16 ファイル 24 行が契約を満たす) |
| `python3 scripts/install-example-lint.py --selftest` | 0 (19 観測点、失敗なし) |
| `python3 scripts/release/build-release-notes.py --selftest` | 0 (37 観測点、失敗なし) |
| `python3 scripts/local-path-lint.py` (+ `--selftest`) | 0 |
| `python3 scripts/identity-lint.py` (+ `--selftest`) | 0 |
| `python3 scripts/readme-example-lint.py` (+ `--selftest`) | 0 |
| `python3 scripts/release/check-time-budget.py` (+ `--selftest`) | 0 |
| `python3 scripts/release/check-publish-step-order.py` (+ `--selftest`) | 0 (tasks 6.8 — download step が 1 本増えても追随できている) |

tasks 10.3 の手元確認: pull request テンプレート未編集 (#10) + 模擬本文 2 件 (#11 / #12) を `render` に与え、
(a) 種別が breaking → feature → fix → docs の順に並ぶ、(b) 項目の無い種別の見出しが出ない、
(c) テンプレート由来の `- none` から項目が載らない、(d) 異なる pull request の同一文面が両方残る、
(e) `--base-tag` ありで `**Full Changelog**`、無しで `This is the first release.`、
(f) 対象 0 件で `No user-facing changes in this release.` をいずれも確認した。

### 未記録乖離

deviation.md の「乖離: なし」と対応表は矛盾しない。`[付随修正]` 4 件 (`local-development-setup.md` /
`verification-ci.md` / `handbook/index.md` / `release-procedure.md` の注意削除) はいずれも diff に実在し、記録と一致する。
Scenario に対応しない差分で未記録のものは見つからなかった。

### UI 変更

なし (該当なし)。

---

## 4. 判定

**INVALID** — ❌ 1 件。

### ❌ Scenario: 既に発行済みの Release も最新の選別に乗る (release-workflow / MODIFIED: tag と GitHub Release)

- **事実**: `gh release list --repo kamusoft/KsDialogs` が `0.1.0-beta.1` を **Pre-release** と表示する。tasks 1.1 は未チェックで、印は解除されていない。
- **波及**: install-examples の Scenario「最新版の案内から目的の版に着地できる」が現状では成立しない。公開済みの Release が prerelease 印しか持たない間、`https://github.com/kamusoft/KsDialogs/releases/latest` は解決先を持たない。README 2 枚と Skill 14 ファイルに案内を書いた変更 (tasks 3.4) は完了しているため、**この状態のまま `main` へ入れると 16 ファイルの案内が着地しない窓が開く**。tasks 1.1 本文が「README に最新版の案内を書く前に行う」と定めている実施順が、現在の作業木では逆転している。
- **見立て**: 実装を直す話でも deviation として合意する話でもない。**オーナーが `main` へのマージ前に 1.1 を実行する**のが筋 (`gh release edit 0.1.0-beta.1 --prerelease=false --latest` 相当)。公開物を変える操作なので保留されていること自体は妥当で、解消後に本 Scenario と「最新版の案内から目的の版に着地できる」が同時に ✅ になる。

### ⚠️ 自動検証不能 (5 件 — 判定を妨げない)

workflow の実行時にしか観測できないもの。step・条件式の記述は静的に確認済み。

- beta の版でも最新リリースとして参照できる (`--latest` 指定、tag 2 本)
- 確定したノートが無ければ Release を作らない (`[ ! -s notes ]` → exit 1)
- 検査の後に本文が編集されても公開内容が変わらない (artifact 経由の受け渡し)
- 開発ブランチからのリハーサルでは対象を集めない
- main からのリハーサルでは実際の経路を通る (tasks 10.5 未実行 — `main` から dry-run を 1 回回すと解消する)

### 残りの未実行タスク

- **tasks 10.5** — `main` からの dry-run 起動。上記 ⚠️ 2 件を実行で裏付けられる。
- **tasks 10.6** — 消費者で `{version}` を埋めた解決の確認。Scenario「埋めずに使うと依存解決が失敗する」の裏面にあたる。

これらは ❌ に数えていない (実装・記述はいずれも確認済みで、乖離の証拠が無いため)。
