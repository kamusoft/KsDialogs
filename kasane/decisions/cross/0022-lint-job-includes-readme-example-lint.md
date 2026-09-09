---
id: 0022
title: 検証 CI の lint job に README 最小例と消費者ソースの一致検査を加え、8 検査とする (0021 を一部改訂)
status: proposed
date: 2026-09-09
amends: [cross/0021]
---

## Context

phase-8 (consumer-verification) で、配布物を利用者と同じ経路で参照する消費者プロジェクト `verification/` (iOS / Android / MAUI / KMP) を新設し、ルート README (英語) の「Minimal examples」節の 4 コードブロックを消費者のソースとして逐語で同梱する (agenda 決定 4)。README の例と消費者のソースのどちらか一方だけが変わると「README に載っている例はビルドされている」が崩れるため、完全一致を機械で検査する必要がある。姉妹ライブラリ KsSettingsView は同じ検査 (`scripts/readme-example-lint.py`) を lint job に置いている。

cross/ADR-0021 (cross/ADR-0020・0017 の系譜) は lint job を 7 検査に固定しており、検査を無断で足すことはできない。消費者検証 4 job は `main` 宛て pull_request でだけ起動する (cross/ADR-0017 のトリガーの決定、phase-4 の決定事項) ため、一致検査を消費者 job の中に置くと `develop` への push では検査されない。

前提: lint job は `ubuntu-24.04` で数十秒、一致検査は README 1 枚と 4 ファイルの文字列比較で所要は無視できる。README の最小例の更新は docs-refresh 経由で行われる (cross/ADR-0012、AGENTS の運用宣言)。

## Decision

cross/ADR-0021 の決定のうち「検証 CI の lint job に許可リスト検査を加えて 7 検査とする」を、これに **README 最小例と消費者ソースの一致検査 (`scripts/readme-example-lint.py`、自己テスト付き) を加えた 8 検査**へ置き換える。他の決定 (CI 限定 skip の統制・正規の印・CI の判定・lint の違反条件) は維持する。

| 要素 | 決定 |
|---|---|
| 検査対象 | ルート README (英語) の「Minimal examples」節の 4 コードブロック (iOS / Android / MAUI / KMP) と、`verification/` 配下の対応する 4 ソースファイルの完全一致 (末尾の改行まで)。対応表 (小見出し・fence 言語・ファイル) はスクリプト冒頭に持つ |
| 違反 | 不一致・対応するコードブロックの不在・同じ小見出しと言語のブロックの重複 |
| 対象外 | `README_ja` (英日同期は docs-refresh の責務)、README に無い KMP のホスト側の登録コード、`skills/` の例 |
| 実行 | lint job で自己テストを本検査より先に同じ step で走らせる (cross/ADR-0020・0021 と同じ理屈)。`develop` への push のたびに落とす |
| 追随の運用 | README の最小例が変わるのは docs-refresh 経由で、lint が赤になったら docs-refresh の依頼者が消費者側のソースも直す |

## Alternatives Considered

- **一致検査を消費者検証 4 job の中で行い、lint job には足さない** — 却下。消費者 job は `main` 宛て PR でしか起動せず、README と消費者の不一致に気づくのがリリース直前になる。macOS 3 job で同じ文字列比較を重複して回す
- **README の例をビルド対象にせず、`skills/` との文字列一致だけで担保する (phase-2 までの状態)** — 却下。例がビルドできるかは検査されない (翻案元の proposal Why と同じ理由)
- **`README_ja` も検査対象にする** — 却下。英日の同期は docs-refresh の責務で、消費者に 2 言語分のソースを持つ意味がない

## Consequences

- 正: README の最小例 4 つが、`develop` への push のたびにビルド対象と一致していることが検査される
- 正: 例の変更が消費者側に追随していない状態で `main` へ進めない
- 負: lint job の検査が 1 つ増え、README の最小例を変える docs-refresh は消費者側のソースの更新を伴う
- 負: 対応表を持つため、README の節見出しや消費者のファイル配置を変えるときはスクリプトの追随を要する

## Revisit When

- README の最小例の節の構成が変わったとき (ブロックの追加・削除。KMP のホスト側の例を載せる判断は phase-9 の docs-refresh の論点)
- 消費者検証のトリガーが変わり、`develop` への push でも消費者 job が回るようになったとき

---
出典: kasane/roadmaps/package-distribution/phases/phase-8-consumer-verification/agenda.md (決定 4) / kasane/changes/add-consumer-verification/design.md (ADR 候補) / kasane/changes/add-consumer-verification/second-opinion-spec-001.md (#2)
関連: cross/ADR-0021 (lint job の検査の集合。本決定で 7 → 8 検査に置き換え) / cross/ADR-0017 (検証 CI の構成とトリガー) / cross/ADR-0012 (README はルート 2 枚)
