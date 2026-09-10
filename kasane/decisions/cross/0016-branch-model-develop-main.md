---
id: 0016
title: ブランチは develop / main の 2 本とし、develop へ直 push、main はリリース候補だけが PR で入り release は main からのみ起動する
status: accepted
date: 2026-09-07
amended-by: [0024, 0025]
---

## Context

KsDialogs は git remote なし・CI なし・`main` 1 本への直コミットで開発してきた。public 化と配信 (検証 CI・release workflow) の整備にあたり、ブランチの役割を決める必要があった。

姉妹ライブラリ KsSettingsView は `develop` / `main` の 2 本で運用し、検証 CI のトリガーをブランチの役割で分け (`../KsSettingsView/kasane/decisions/cross/0028-ci-triggers-by-branch-role.md`)、本番のリリースは `main` からのみ起動する (`../KsSettingsView/kasane/decisions/cross/0020-release-dispatch-tag-last-version-injection.md`)。KsDialogs の検証 CI と release workflow はこの 2 本の workflow・手順書を「コピー + 固有値の差し替え」で逆流させる計画で、ブランチモデルが違えば読み替えが要る。

前提: 開発者は 1 人で、外部からの pull request は受け付けない (cross/ADR-0013)。リリースは version 入力の手動起動で行う (cross/ADR-0009 の lockstep 配信)。

## Decision

- ブランチは **`develop` と `main` の 2 本**にする。既定ブランチは `develop`
- 日常のコミットはトピックブランチを切らず **`develop` へ直 push** する (現行の直コミット運用と手数は同じ)
- **`main` の先端は「最新リリース、またはリリース進行中のリリース候補」**とし、`develop` からの pull request だけが入る。README の version 置換はこのリリース PR の中の commit として行う
- **本番の release workflow は `main` からのみ起動**する。消費者検証 (dry-run) は `main` 宛ての pull request で走らせ、リリース前に必ず 1 回通す
- public 化の initial commit は `develop` に置き、`main` は初回リリースの PR で作る (KsSettingsView と同じ順序)

## Alternatives Considered

**`main` 1 本のまま運用する** — 却下。

- リリース前に PR を切る手間は消えるが、任意の commit から release を起動できてしまい「リリース対象 commit が一意に定まらない」という翻案元 ADR-0020 の却下理由がそのまま残る
- 消費者検証の自動実行の場が無くなり、dry-run を手で回す運用になる
- CI トリガー・起動ブランチ制限・release 手順書を 1 本用に書き換える逆流コストが、PR 1 本の手間を上回る

## Consequences

- 正: KsSettingsView の `ci.yml` / `release.yml` / release-procedure を読み替えなしで逆流できる
- 正: `main` の先端がリリース対象として一意に定まり、消費者検証がリリース前に必ず走る
- 負: リリースのたびに `develop` → `main` の pull request を 1 本切ってマージする作業が増える
- 負: `develop` の検証 CI は直 push に対する事後検証になる (壊れた commit が `develop` に載り得る)

## Revisit When

- 開発体制が変わって複数人や外部からの pull request を `develop` で受けるようになったとき
- リリース頻度が上がり、リリース PR の運用が負担になったとき

---
出典: kasane/roadmaps/package-distribution/phases/phase-3-public-readiness/history.md (2026-09-07: ブランチモデル) / 同 agenda.md 決定事項「ブランチモデルは `develop` / `main` の 2 本を踏襲する」 / ../KsSettingsView/kasane/decisions/cross/0020-release-dispatch-tag-last-version-injection.md (却下案「`develop` から起動し `main` は作らない」) / ../KsSettingsView/kasane/decisions/cross/0028-ci-triggers-by-branch-role.md
関連: cross/ADR-0017 (このブランチの役割に合わせた検証 CI のトリガーと保護設定)
