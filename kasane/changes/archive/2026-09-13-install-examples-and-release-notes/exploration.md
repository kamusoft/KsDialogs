# Exploration: install-examples-and-release-notes

## 課題 / 動機

姉妹リポジトリ KsSettingsView から 2 通の知らせを受け取った。
本 change は利用者向け契約の側 — インストール例の version 表記と GitHub Release のノート — を翻案する。

- `../KsSettingsView/kasane/outbox/KsDialogs/2026-09-12-install-examples-and-release-notes-decisions.md` (kind: decision)
- `../KsSettingsView/kasane/outbox/KsDialogs/2026-09-12-release-mechanism-hardening-and-notes-rework.md` (kind: change)
- 向こうの実装: `../KsSettingsView/kasane/changes/archive/2026-09-12-install-examples-and-release-notes/`

知らせが名指ししているとおり、こちらは「workflow が README を書く」方式を実装した直後で、向こうの決定は
その真逆に進んでいる。こちらの現状:

- `scripts/release/set-readme-version.py` (711 行) が 18 ファイル・22 行のインストール例 version を置換する。
  呼び出しは 2 箇所 — `.github/workflows/release.yml:398` (package-maui の pack 前、作業木で NuGet 同梱 README 向け) と
  同 `:1312` (publish 後、`develop` 先端へ書き戻して push)
- `.github/release.yml` (30 行) がラベル分類を定義し、`gh release create --generate-notes` が読む。
  分類ラベル 4 件 (`breaking` / `feature` / `fix` / `docs`) と除外ラベル 2 件 (`kasane` / `ci`) はリポジトリに実在する
- GitHub Release の prerelease 印は version にハイフンが含まれるかで機械判定している
- `AGENTS.md:13` (および CLAUDE.md) に「インストール例の version の置換だけは release workflow が行う」例外規定がある

向こうが挙げた問題はこちらの構成にそのまま当てはまる。書き戻し先が `develop` だけなので、
**既定ブランチ `main` が常に 1 リリースぶん古い version を示し続ける** (こちらは 2026-09-12 に既定ブランチを
`main` へ切り替えたばかりで、GitHub で README を読む利用者が見るのは `main`)。
ラベル分類も、集約 pull request だけが `main` に入る運用では分類できる粒度が集約単位にしかならない。

## 検討した選択肢 (却下案と理由を含む)

KsSettingsView 側で cross/ADR-0029 / ADR-0030 として決着済みのため、本 change では選択肢の比較を行わない
(オーナー指示)。前提として引き継ぐ却下案は次の 2 つ:

- **`main` へ直接書き戻す案** — branch protection の pull request 必須と必須 status check を CI にバイパスさせることになる
  (管理者 PAT か bypass actor の登録)。得られる結果は手作業と同じ
- **`develop` だけに書き戻す案 (= こちらの現状)** — protection には触れないが、既定ブランチが 1 リリースぶん
  古い version を示し続ける。手作業方式より正確さが下がる

プレースホルダを `X.Y.Z` にする案も却下済み (それ自体が version の形をしているため埋め忘れが露見しない)。
採るのは `{version}` — version として解決できない形にするのが要点で、埋め忘れは依存解決の失敗として必ず露見する。

## 決定事項

### 翻案の方針 (KsSettingsView 最終形の踏襲)

- **インストール例から具体 version を外す** — 18 ファイル・22 行を `{version}` にし、最新版の案内は
  GitHub Releases (`https://github.com/kamusoft/KsDialogs/releases/latest`) に委ねる。
  SwiftPM は `exact:` を維持する (`from:` は上限が次メジャーまで開くので固定にならない)
- **置換機構を撤去する** — `scripts/release/set-readme-version.py` (711 行)、`release.yml` の呼び出し 2 箇所
  (`:398` の pack 前 / `:1290-1348` の `Update install examples on develop` step)、
  validate 段の一致検査 step、`AGENTS.md:13` (+ CLAUDE.md) の例外規定
- **`scripts/install-example-lint.py` を新設** — プレースホルダ / SwiftPM が `exact:` / 最新版案内 /
  英日同一構成の 4 契約を日常 CI で検査。対象は検査側が明示表 (`TARGET_FILES`) で持ち、
  `skills/{en,ja}/*/SKILL.md` の実構成とも突合する
- **GitHub Release の prerelease 印を廃止し `--latest` を明示する** — `/releases/latest` は prerelease を除外するため、
  正式版が無い間 404 を返す。印を外すだけでは最新の選別が自動判定に委ねられる
- **Release ノートを `main` 宛て pull request 本文の `## Changes` から組み立てる** —
  `scripts/release/build-release-notes.py` を新設し、`.github/release.yml` は廃止。
  収集と検査は validate 段で一度だけ行い artifact で publish へ渡す (検査を通した本文と公開されるノートを同一にするため)。
  `.github/pull_request_template.md` に `## Changes` 雛形を置く
- **`.agents/skills/release/` を新設** — handbook のリリース手順を実行時に読んで従う薄い層 (手順は書き写さない)。
  docs-refresh と同じ形で `.claude/skills/release` symlink を張る
- **handbook**: `kasane/handbook/cross/install-examples.md` を新設 (kind: rule)、
  `cross/release-procedure.md` から version 置換手順を削除し `## Changes` 記入を追加、`index.md` の cross 行要約を更新

### KsDialogs 固有の差分 (向こうと違う点)

- **対象が 16 ファイル・28 行** (向こうは 10 ファイル・14 行。2026-09-13 に実測)。
  Maven 座標が 3 種 (core / compose / kmp) あり、KMP の Skill が複数ファイルに宣言を持つため。
  内訳: `README.md` / `README_ja.md` 各 5 行 (SwiftPM / Maven core / Maven compose / NuGet / Maven kmp)、
  `skills/{en,ja}/ksdialogs-ios/SKILL.md` 各 1、`ksdialogs-android/SKILL.md` 各 2、
  `ksdialogs-kmp/SKILL.md` 各 2、`ksdialogs-kmp/references/android-host.md` 各 1、
  `ksdialogs-kmp/references/ios-host.md` 各 1、`ksdialogs-maui/SKILL.md` 各 1、
  `ksdialogs-aiforms-migration/SKILL.md` 各 1
- **インストール宣言が `SKILL.md` の外にもある** — `ksdialogs-kmp/references/android-host.md` と
  同 `references/ios-host.md`。向こうの lint は対象表も構造の突合も `skills/{en,ja}/*/SKILL.md` だけを見るため、
  対象表と実構成の突合をこの 2 ファイルまで広げる必要がある
- **散文中の version が 4 行ある** — `skills/{en,ja}/ksdialogs-kmp/SKILL.md:69` と
  `skills/{en,ja}/ksdialogs-kmp/references/ios-host.md:7`。いずれもコードブロックで示した依存の再掲なので
  `{version}` 化する。向こうの lint はコードブロック内だけを走査するためこの 4 行は検査対象外になるが、
  「検出 0 件は適合の証明にならない (散文は検査対象外)」は向こうの handbook にも明記された既知の限界で、同じ扱いとする
- **`set-readme-version.py` の呼び出しが 2 箇所** — 向こうには無い `release.yml:398` (package-maui の pack 前) がある。
  NuGet パッケージ同梱 README も `{version}` のままにする (向こうの Scenario「同梱 README も同じ」に一致)
- **分類ラベルが既に存在する** — 向こうは「ラベルは作らない」だが、こちらは 6 件が実在する。
  `.github/release.yml` を廃止すると `breaking` / `feature` / `fix` / `docs` は用途を失う。
  ラベル自体は GitHub 上のデータでリポジトリのファイルではないため、本 change では削除しない (放置しても害がない)
- **`scripts/readme-example-lint.py` (366 行) が別に存在する** — README 最小例と `verification/` ソースの
  完全一致を検査するもので、install-example-lint とは対象が異なる。共存させる
- **`verification/` の version は変数展開** (`exact: "${KSV_VERSION}"`) のため対象外

## ADR 候補 (作成済み: なし / 未起票: 2 本)

向こうの cross/ADR-0029 / ADR-0030 に相当する決定をこちらでも起票する
(両者は独立したリポジトリで、決定の記録はそれぞれが持つ)。

- **インストール例は具体 version を持たない** — 置換機構ごと撤去した判断、書き戻し先が収まらない理由、
  プレースホルダを `{version}` にした理由
- **Release ノートは `main` 宛て pull request 本文の `## Changes` から組み立てる** — ラベル分類が
  集約 pull request 運用と噛み合わない理由、収集を validate 段で一度だけ行う理由

あわせて cross/ADR-0022 (lint job の検査の集合) の amends が必要 (install-example-lint の追加)。
change backport-registry-wait-hardening でも同じ ADR に触れるため、起票の重複を実装時に調整する。

## 未決の論点

- 既存の分類ラベル 6 件を GitHub 上で削除するか (本 change では放置と決定。将来の整理対象)
- cross/ADR-0022 の amends を 2 つの change のどちらで起票するか

## UI 素材 (ui/references/ の一覧と注釈)

なし

## 変更級の推奨: L

判定材料: 触る能力は install-examples (新設) と release-workflow の 2 つ。
利用者が読むインストール手順の契約を変えるため利用者向けの公開面に触れる。
新規スクリプト 2 本 + 711 行のスクリプト撤去 + workflow の validate / publish 両段の組み替え +
handbook 新設 + Agent Skill 新設 + PR テンプレート新設。向こうも L 級で実装している。
