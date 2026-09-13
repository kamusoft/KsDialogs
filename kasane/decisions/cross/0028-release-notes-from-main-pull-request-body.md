---
id: 0028
title: Release ノートは `main` 宛て pull request 本文の `## Changes` から組み立て、収集と検査は validate 段で一度だけ行う
status: accepted
date: 2026-09-13
---

## Context

Release ノートは `.github/release.yml` のラベル分類と `gh release create --generate-notes` で作っていた。ラベル自体はリポジトリに実在するが、**`main` へ入るのは `develop` からの集約 pull request だけ**という運用 (cross/ADR-0016) では、分類できる粒度が集約の単位にしかならない。集約 pull request に除外ラベルを付ければ、その中の利用者向けの変更ごとノートから消える。

一方で、利用者向けの変更を「人が書いたもの」に限るなら、書く位置は pull request 本文しかない。範囲の決め方と検査の位置は、不可逆な公開との前後関係で決まる — 公開の後に Release 作成だけが失敗する経路を残さないことが要件になる。

前提: リリースは `main` からのみ起動し (cross/ADR-0016)、publish は取り消せない順で直列に進む (cross/ADR-0024)。`dry-run` では publish job そのものが走らない。

## Decision

Release ノートは、範囲に入る `main` 宛て pull request の本文に書かれた `## Changes` セクションから組み立てる。GitHub がラベル等から推測した分類は使わない。`.github/release.yml` は廃止し、組み立ては `scripts/release/build-release-notes.py` が行う。

| 要素 | 決定 |
|---|---|
| 起点 | 「今回の version でない / draft でなく公開済み / その tag が `main` の first-parent 上で対象 commit の祖先」をすべて満たす Release のうち、対象 commit にもっとも近いものの tag。該当が無ければ履歴の最初から |
| 対象 | 起点と対象 commit の間の commit に紐づく pull request のうち、**base が `main`** のものだけ。pull request 番号で重複を除き、マージ順に並べる |
| 入力の文法 | `## Changes` はちょうど 1 つ。範囲の空行でない行はすべて `- <種別>: <説明>` または `- none`。種別は breaking / feature / fix / docs の 4 つ。説明は非空。`- none` は単独のみ。空セクションは失敗 |
| 認識できない入力 | 失敗させる。読み飛ばさない、「その他」に落とさない |
| 確定の位置 | 収集・検査・整形は validate 段で一度だけ行い、確定した本文を成果物として publish へ渡す。publish は pull request 本文を読み直さない。確定した本文が無ければ Release を作らずに失敗する |
| 権限 | validate の `permissions` に `contents: read` と `pull-requests: read` の**両方**を書き切る (job レベルの `permissions` は列挙しなかった権限を `none` にする) |
| リハーサル | `main` から起動した `dry-run` は収集・検査・整形・受け渡しまで本番と同じ経路を通る (Release だけ作らない)。`main` 以外からの起動では収集も検査も行わない |
| 出力 | 種別ごとにまとめ、項目の無い種別の見出しは出さない。起点があれば前回の版との比較の位置を、無ければ最初のリリースであることを末尾に置く。定型文言は英語 |

記入の書式の正は `.github/pull_request_template.md`、手順の正は handbook `cross/release-procedure.md` が持つ。

## Alternatives Considered

- **ラベル分類を続ける** — 却下。集約 pull request 運用では分類の粒度が集約の単位にしかならず、除外ラベルを付けると中の利用者向けの変更ごと消える
- **起点を「直前の tag」にする** — 却下。Release を持たない tag を起点にしうる。version 順で選ぶ案も、version 順は履歴の前後と一致しない
- **起点を Release の作成日時で決める** — 却下。対象 commit の祖先でない別枝の Release を選びうる
- **範囲を日時で絞る** — 却下。境界が commit ではなく時刻になり、再実行や tag の打ち直しで結果が変わる
- **base を限定せずに pull request を集める** — 却下。commit から pull request を引く API は base を問わないため、`develop` 宛ての個別 pull request が集約 pull request の記載と二重に載る
- **起動時に人がノート本文を貼る** — 却下。複数の pull request から集める以上、手作業の集約は現実的でない
- **認識できない行を読み飛ばす / 不正な種別を「その他」に落とす** — 却下。いずれも静かに通り、項目が消えたことに誰も気づかない
- **収集と組み立てを publish の段で行う** — 却下。**不可逆な公開の後に Release 作成だけが失敗する**経路が残る。両方の段で取得して一致を検査する案も、一致しなかったときに publish 後で止まる問題は同じ
- **どのブランチでも一律に `dry-run` の収集を省く** — 却下。API 認証・ページ送り・commit と pull request の関連付け・成果物の受け渡しという中心の経路が、本番まで一度も実行されない

## Consequences

- 正: Release ノートに載るのは、人が利用者向けと判断して書いた項目だけになる
- 正: 記載の不備で止まる位置が validate 段に固定され、公開物には何も起きていない状態で直せる
- 正: 検査を通した時点の本文がそのまま Release になる (publish までの間に本文が編集されても公開内容が変わらない)
- 負: `## Changes` の記載が必須になり、記載を欠く pull request が範囲にあると release が止まる
- 負: pull request 本文は後から編集でき、ノートは release 実行時点の本文を読む。マージ済みでも本文を直せば反映される (直し忘れも反映される)
- 負: 組み立ての正しさは自己テストでしか確かめられない (`dry-run` では Release を作らない)。`render` サブコマンドで手元から再現できる形にして補う

## Revisit When

- `main` への取り込みが集約 pull request 以外の形 (個別 pull request の直接マージ) を採るようになったとき
- 種別 4 つで足りなくなったとき、または記載の書式を変えたくなったとき
- リリースの起動ブランチの決定 (cross/ADR-0016) が変わったとき

---
出典: kasane/changes/archive/2026-09-13-install-examples-and-release-notes/proposal.md (Why・What Changes 5) / 同 design.md (Decision 4・5・6・7・8) / ../KsSettingsView/kasane/outbox/KsDialogs/2026-09-12-install-examples-and-release-notes-decisions.md (sibling の決定)
関連: cross/ADR-0016 (ブランチモデルとリリースの起動ブランチ) / cross/ADR-0024 (publish の順序 — 公開が取り消せない前提) / cross/ADR-0027 (インストール例と最新リリースの指定)
