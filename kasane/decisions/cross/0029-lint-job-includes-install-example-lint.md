---
id: 0029
title: 検証 CI の lint job にインストール例の契約の検査を加え、12 検査とする (0026 を一部改訂)
status: proposed
date: 2026-09-13
amends: [cross/0026]
---

## Context

インストール例が具体 version を持たなくなり、リリースはその行を書き換えなくなった (cross/ADR-0027)。置換機構が持っていた歯止め — release の validate 段でインストール例と入力 version の一致を検査する step — も、書き換える対象が無くなったため撤去した。

その結果、契約 (プレースホルダ・SwiftPM の `exact:`・最新リリースへの案内・英日の同一構成) が崩れても気づく機会がどこにも無くなる。崩れ方は静かで、具体 version が 1 行だけ書き戻されても、Skill が 1 つ英語版にだけ増えても、平時のビルドとテストは緑のまま通る。露見するのは利用者が例を写して依存解決に失敗したときになる。

cross/ADR-0026 は lint job を 11 検査に固定しており、検査を無断で足すことはできない。同 ADR は前提として「`set-readme-version.py` は後続の変更で撤去する予定のため対象に含めない」と書いていた。その撤去は cross/ADR-0027 の決定として済み、同スクリプトはリポジトリに存在しない。

前提: lint job は `ubuntu-24.04` で `timeout-minutes: 10`。`scripts/install-example-lint.py` は本検査・自己テストとも手元実測で 1 秒未満、ネットワークに出ない。

## Decision

cross/ADR-0026 の決定のうち「リリース用スクリプトの自己テスト・待ちの時間予算の検査・publish の step 順序の検査を加えた 11 検査」を、これに **インストール例の契約の検査 (`scripts/install-example-lint.py`) を加えた 12 検査**へ置き換える。他の決定 (各検査の対象・数え方・実行の形) は維持する。

| 要素 | 決定 |
|---|---|
| 検査 | `scripts/install-example-lint.py` が、対象表 (`TARGET_FILES`) の各ファイルについて 4 契約を検査し、あわせて対象表と `skills/{en,ja}/` の実構成を突き合わせる。突合の範囲は入口の `SKILL.md` に限らず、インストール宣言を持つ文書すべて |
| 数え方 | 本検査と自己テスト (`--selftest`) の 1 対で 1 検査と数える (cross/ADR-0020・0021・0022・0026 と同じ数え方)。自己テストを本検査より先に同じ step で走らせる |
| `build-release-notes.py` の自己テスト | 既存の「リリース用スクリプトの自己テスト」1 検査に含め、数を増やさない (cross/ADR-0026 が `scripts/release/` の自己テストを 1 検査と数えているのに揃える) |
| 検査の限界 | 走査はコードブロック内に限る。散文の宣言は値の検査の対象外で、**検出 0 件は適合の証明にならない**。この限界は検査の側 (`PROSE_DECLARATIONS`) と handbook `cross/install-examples.md` の両方に記録し、記録が実体を失わないよう、一覧の各組がコードブロックの外に実在することだけは検査する |
| 対象外 | `scripts/release/set-readme-version.py` (cross/ADR-0027 の決定により撤去済み。cross/ADR-0026 の「後続の変更で撤去する予定」はこれで解消した) |

## Alternatives Considered

- **lint job へ接続せず、契約の維持を人のレビューに委ねる** — 却下。リリースが書き換えなくなったことで機械の歯止めが 1 つも無くなる。具体 version の書き戻しも英日のずれも平時は緑のまま通る
- **本検査と自己テストを別々の 2 検査として数え、13 検査とする** — 却下。同種の検査を 1 つと数える既存の数え方 (cross/ADR-0020 以降) に反し、数え方が検査ごとに割れる
- **`build-release-notes.py --selftest` を別の 1 検査として数え、13 検査とする** — 却下。`scripts/release/` の自己テストを 1 検査と数える cross/ADR-0026 の決定に反し、スクリプトの増減のたびに ADR の改訂が要る
- **release の側にも契約の検査を残す** — 却下。リリースはインストール例を読み書きしなくなったため、release に検査を置く根拠が無い。日常 CI で落ちていれば、その状態のまま `main` へ入ることはない

## Consequences

- 正: 契約の崩れが `develop` への push のたびに検出される (release を待たない)
- 正: 対象表と実構成の突合により、インストール宣言を持つ Skill や補助文書が表に登録されないまま増えた状態も検出される (宣言を 1 つも持たない Skill は突合の対象に入らないため検出されない。英日で非対称な Skill は宣言の有無によらず検出される)
- 負: lint job の検査が 1 つ増える (所要は 1 秒未満で `timeout-minutes: 10` に対しては無視できる)
- 負: 対象表は手で維持する。Skill を足すたびに英日 2 行の登録が要る (宣言を持つ文書の登録漏れは突合が落とす)
- 負: 散文の宣言は値の検査の外に残り (実在するかだけを見る)、適合は人が読んで判定する

## Revisit When

- インストール例の契約そのものが変わったとき (cross/ADR-0027 を見直すとき)
- 走査をコードブロック外へ広げる手段が見つかり、散文まで機械で検査できるようになったとき
- lint job の所要が `timeout-minutes: 10` に対して余裕を失ったとき

---
出典: kasane/changes/install-examples-and-release-notes/proposal.md (What Changes 3・既存 ADR との関係) / 同 design.md (Decision 3・9) / 同 specs/install-examples/spec.md (Requirement「インストール例の契約の検査」)
関連: cross/ADR-0026 (lint job の検査の集合。本決定で 11 → 12 検査に置き換え) / cross/ADR-0027 (インストール例は具体 version を持たない — 検査が守る契約) / cross/ADR-0020 (自己テストを本検査と同じ step で先に走らせる理屈と数え方)
