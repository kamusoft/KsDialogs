# Proposal: install-examples-and-release-notes

## Why

sibling の KsSettingsView から 2 通の知らせを受け取った。こちらが送った知らせを受けて向こうが
リリース機構を改修し、その過程で**利用者から見える契約を 2 つ組み替える決定**に至っている
(`../KsSettingsView/kasane/outbox/KsDialogs/2026-09-12-install-examples-and-release-notes-decisions.md`)。
知らせが名指しするとおり、こちらは「workflow が README を書く」方式を実装した直後で、向こうの決定はその真逆に進む。

### インストール例の version 同期が、書き戻し先のどこにも収まらない

`scripts/release/set-readme-version.py` (711 行) が 16 ファイル・28 行のインストール例 version を置換し、
publish 後に `develop` の先端へ commit して push する。しかしこちらは 2026-09-12 に既定ブランチを `main` へ
切り替えた ([cross/ADR-0025](../../decisions/cross/0025-default-branch-main.md))。GitHub で README を読む利用者が見るのは
`main` であり、**そこには常に 1 リリースぶん古い version が表示され続ける**。手作業で書く方式より正確さが下がる。

`main` へ直接書き戻す案は、branch protection の pull request 必須と必須 status check を CI にバイパスさせることになり、
得られる結果は手作業と同じ。維持しているもの (置換スクリプト 711 行・validate の一致検査 step・handbook の手順・
`AGENTS.md` の例外規定) と、得ているもの (「README の行をそのままコピーできる」) が釣り合っていない。

### Release ノートのラベル分類が、取り込みの単位と噛み合わない

`.github/release.yml` が 6 ラベルで分類を定義し、`gh release create --generate-notes` が読む。
ラベル自体はリポジトリに実在するが、**集約 pull request だけが `main` に入る運用では分類できる粒度が集約の単位でしかない**。
集約 pull request に除外ラベルを付ければ、その中の利用者向け変更ごとノートから消える。

### prerelease 印が「最新リリース」の案内先を壊す

GitHub Release は version にハイフンが含まれるかで prerelease 印を機械判定している。
`/releases/latest` は prerelease を除外するため、正式版が無い間 404 を返す。
インストール例から version を外して最新版の案内に委ねるなら、この案内先が解決しなければ成り立たない。

## What Changes

影響する能力: **install-examples** (新設) と **release-workflow**。

1. **インストール例から具体 version を外す** — 16 ファイル・28 行を `{version}` にし、最新版の案内を
   GitHub Releases に委ねる。SwiftPM は `exact:` を維持する
2. **置換機構を撤去する** — `scripts/release/set-readme-version.py`、`release.yml` の呼び出し 2 箇所
   (package-maui の pack 前 / publish 後の `develop` 書き戻し)、validate の一致検査 step、
   `AGENTS.md` (と `CLAUDE.md`) の例外規定
3. **`scripts/install-example-lint.py` を新設する** — プレースホルダ・SwiftPM の `exact:`・最新版案内・
   英日同一構成の 4 契約を日常 CI で検査する。対象は検査側が明示表で持ち、実構成とも突き合わせる
4. **GitHub Release の prerelease 印を廃止し `--latest` を明示する**
5. **Release ノートを `main` 宛て pull request 本文の `## Changes` から組み立てる** —
   `scripts/release/build-release-notes.py` を新設し、`.github/release.yml` は廃止する。
   収集と検査は validate 段で一度だけ行い、artifact で publish へ渡す
6. **リリース用スキルを置く** — `.agents/skills/release/`。handbook のリリース手順を実行時に読んで従う薄い層
7. **handbook を整える** — `cross/install-examples.md` を新設し、`cross/release-procedure.md` から
   version 置換手順を落として `## Changes` の記入を加える

## Non-Goals

- **publish の待機と時間予算の堅牢化**: `kasane/changes/backport-registry-wait-hardening/` で別に実施する
  (publish 内部の挙動で、利用者から見える契約に触れないため)
- **既存の分類ラベル 6 件の削除**: `.github/release.yml` の廃止でラベルは用途を失うが、ラベル自体は
  GitHub 上のデータでリポジトリのファイルではない。残っていても害がなく、削除は別の判断
- **`kasane/concepts/cross/architecture/release-workflow.md` の追随**: 長命層の更新は蒸留 (ksn-distill) の責務。
  本 change の申し送りに記録する
- **`scripts/readme-example-lint.py` との統合**: README 最小例と `verification/` ソースの完全一致を見るもので、
  インストール例の契約とは対象が異なる。共存させる

## Impact

- **破壊的変更**: 利用者向けドキュメントの契約が変わる。インストール例をそのまま写した消費者は
  `{version}` を自分で埋める必要がある (埋め忘れは依存解決の失敗として必ず露見する)
- 影響範囲:
  - README 2 枚と `skills/` 14 ファイル (計 28 行) — 利用者が読む公開物
  - `.github/workflows/release.yml` (validate 段に 4 step 追加・1 step 削除、publish 段に 1 step 追加・
    Release 作成 step の置換・`Update install examples on develop` step の削除)
  - `.github/workflows/ci.yml` (lint job)、`.github/release.yml` (削除)、`.github/pull_request_template.md` (新設)
  - `scripts/release/set-readme-version.py` (削除 711 行)、`scripts/install-example-lint.py` (新設)、
    `scripts/release/build-release-notes.py` (新設)
  - `AGENTS.md` / `CLAUDE.md` の例外規定、handbook 2 本、`.agents/skills/release/` (新設)
- **リスク**: Release ノートの組み立ては `dry-run` では Release を作らないため、経路全体を実地で確かめられるのは
  本番のリリースだけ。解析・整形・対象の選び方は自己テストで検査する。
  `## Changes` の必須化により、記載を欠く pull request が範囲にあると release が validate 段で止まる
- **既存 ADR との関係**: 新規 2 本を起票する (下記)。
  [cross/ADR-0022](../../decisions/cross/0022-lint-job-includes-readme-example-lint.md) の系譜
  (lint job の検査の集合) にも検査が 1 つ増えるため、amends が要る —
  先行する change backport-registry-wait-hardening が cross/ADR-0026 (11 検査) を起票済みなので、
  本 change はそれをさらに amends する

## ADR 候補

1. **インストール例は具体 version を持たない** — 置換機構ごと撤去した判断。書き戻し先が収まらない理由
   (branch protection のバイパス / 既定ブランチの古さ)、プレースホルダを `{version}` にした理由
2. **Release ノートは `main` 宛て pull request 本文の `## Changes` から組み立てる** — ラベル分類が
   集約 pull request 運用と噛み合わない理由、収集を validate 段で一度だけ行う理由
3. cross/ADR-0026 の amends (lint job の検査の集合に install-example-lint を加える)

## 級: L

判定材料: 触る能力が 2 つ (install-examples 新設 + release-workflow)。利用者が読むインストール手順の契約を変える
ため公開面に触れる。新規スクリプト 2 本 + 711 行のスクリプト撤去 + workflow の validate / publish 両段の組み替え +
handbook 新設 + Agent Skill 新設 + PR テンプレート新設 + ADR 3 本。翻案元も L 級で実装している。

domain: cross
