---
from: KsDialogs
to: KsSettingsView
kind: change
date: 2026-09-10
source: kasane/changes/archive/2026-09-10-add-release-workflow/
---

# release workflow の翻案で見つかった不具合 2 件と、逆流を検討してほしい改良 5 件

## 何が起きたか / 何を変えたか

KsSettingsView の `release.yml` / `scripts/release/` / `.github/release.yml` / handbook release-procedure を KsDialogs へ翻案し、初回リリース `0.1.0-beta.1` まで到達した (change add-release-workflow と、初回リリース後の S 級 fix-release-published-wait)。翻案の過程で翻案元にもある疑いの高い不具合 2 件と、翻案先で足した改良 5 件がある。

### 不具合の疑い (翻案元にもあるもの)

| 項目 | 内容 | KsDialogs 側の対応 |
|---|---|---|
| `check-signatures.sh` の自己テストが無言で終わる | `$( ... \|\| true )` の形の中で `exit` するため、失敗がサブシェルを終えるだけで自己テスト全体の結果に反映されない | `scripts/release/check-signatures.sh` の自己テストを書き直した |
| `.github/release.yml` が参照するラベルが未作成 | Release ノートの分類に使うラベル (`breaking` / `feature` / `fix` / `docs` / `kasane` / `ci`) がリポジトリに無いと分類が効かない | `gh label create` で 6 件を作成した (tasks 4.7) |

### 改良 (逆流を検討してほしいもの)

| 項目 | 内容 | 参照 |
|---|---|---|
| README / Skill の version 置換を workflow が行う | package-maui job が pack の前に作業木で置換 (nupkg 同梱 README が実値に)、publish job が成功後に `develop` へ置換 commit を lint (ローカル絶対パス・個体情報・README 最小例) 付きで push。リリース PR の手作業と validate の `--check` を廃止 | `scripts/release/set-readme-version.py`、`release.yml` の `Set install example version` / `Update install examples on develop`、cross/ADR-0024 (KsDialogs) |
| 再実行の続行判定を印 (marker) で行う | `run_attempt` だけでは拒否された run の再実行が resume に化け、別 commit の binary に tag を打てる (相方レビューの Critical)。外部状態が無いことを確認した試行だけが version・commit・run id の印を artifact に残し、印が一致する再試行だけ続きを埋める | `scripts/release/check-resume-eligibility.sh` (自己テスト 38 件) |
| nuget.org の外部状態照会を fail-closed に | `curl ... \|\| true` は通信失敗・5xx・不正 JSON を「未公開」と読む。200 / 404 / それ以外を分け、判定不能は失敗にする | `scripts/release/check-nuget-version.sh` |
| 失敗経路の deployment ID 書き戻しをガードする | 引き継ぎ ID の読み込みを fallible な artifact download より前に置き、失敗経路 3 step を「引き継ぎを読み込み済み」の条件で守る (早期失敗の attempt が ID を空で上書きしない) | `release.yml` の `Prepare deployment id files` の `ready` output |
| Maven Central の公開待ちを release 要求の後に 1 本で | 翻案元の上限 30 分 (実測 11 分が根拠) は Central の同期 (KsDialogs の初回で約 60 分) に足りないことがある。release を要求してから公開待ちを 1 本でまとめ、上限は専用変数 `KSR_PUBLISHED_TIMEOUT_SECONDS` (既定 90 分) に寄せた | `scripts/release/central-portal.sh wait-published` (複数 ID)、fix-release-published-wait |

そのほか: monorepo tag を完了印にすると tag 直後に Release 作成が失敗した run を再実行できない (相方レビュー Major) ため、tag がある再実行はレジストリへの publish だけを skip し、Release 作成 (既存なら触らない) とインストール例の反映は走らせる形にした。

## 相手に関係する理由

`overlap` の `.github` と `scripts` に当たる。`release.yml` と `scripts/release/` は KsSettingsView から「コピー + 固有値の差し替え」で写したもので、同じ構造・同じ判定を持つ。

## 提案する対応

判断は KsSettingsView 側に委ねる。不具合の疑い 2 件は確認だけでも価値がある。改良 5 件は KsDialogs 側の `scripts/release/` と `release.yml` を再び写す形で逆流できる (KMP 枠は KsSettingsView に無いので、枠は Android 1 つとして読み替える)。
