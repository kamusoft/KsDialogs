---
id: 0030
title: release は publish の後にインストール例を `develop` へ書き戻さず、publish の順序を 7 段で終える (0024 を一部改訂)
status: accepted
date: 2026-09-13
amends: [cross/0024]
---

## Context

cross/ADR-0024 は publish の順序を 8 段で定め、最後の 1 段 (順 8) を「README 2 枚と利用者向け Skill のインストール例の version を置き換えた commit を、lint を掛けてから `develop` へ push する」に充てていた。同 ADR の「tag と README の扱い」節の後半も、この書き戻しを前提に「インストール例の version は release workflow が書く」「置き換えの commit は `GITHUB_TOKEN` の push のため検証 CI を起動しないので、publish job が push の前に検証 CI の lint job と同じ検査を掛ける」と定めていた。

cross/ADR-0027 により、インストール例は具体的な version を持たなくなった。version の位置はプレースホルダで、最新版の案内は GitHub Releases に委ねる。書き換える対象そのものが無くなったため、順 8 と、それを支える「tag と README の扱い」後半の記述は、指す先を失っている。

cross/ADR-0024 は accepted であり、`.github/workflows/release.yml` の冒頭コメントと handbook `cross/release-procedure.md` の双方から現行の決定として参照されている。撤去を記録しないままにすると、0024 を開いた読み手が順 8 と「置換は workflow が行う」を現行として読む。

前提: cross/ADR-0027 (インストール例は具体 version を持たない) と cross/ADR-0029 (契約の検査は日常の検証 CI が行う) が同時に入る。

## Decision

cross/ADR-0024 の決定のうち **インストール例の書き戻し (publish の順序表の 順 8 と、「tag と README の扱い」節の後半) を「release はインストール例を読み書きせず、publish は順 7 (monorepo tag + GitHub Release) で終える」へ置き換える**。他の決定 (起動と version・段構成・publish の順序 1〜7・再実行の範囲・配信リポジトリの tag が取り消せない操作より前に生まれる例外と monorepo の tag の扱い) は維持する。

| 要素 | 決定 |
|---|---|
| publish の順序 | 順 1〜7 のまま。最後の段は monorepo tag + GitHub Release で、publish はそこで終わる |
| インストール例 | release workflow は読まない・書かない。MAUI の pack 前の作業木での置換も、publish 成功後の `develop` への commit も行わない。配布物に同梱される README もプレースホルダのまま |
| publish job の lint | 書き戻しが無くなったため、push の前に検証 CI の lint job と同じ検査を掛ける step も持たない。契約の検査は `develop` への push ごとの lint job が行う (cross/ADR-0029) |
| `develop` への書き込み | release workflow は `develop` へ commit しない。リリースがリポジトリへ書き戻す経路は 1 つも残らない |

cross/ADR-0024 が cross/ADR-0016 の「README の version 置換はリリース PR の中の commit として行う」を置き換えていた件は、置換そのものが cross/ADR-0027 で無くなったため、どちらの形も現行ではない (インストール例に version を書く手順は存在しない)。

## Alternatives Considered

- **0024 の本文を書き換えて順 8 を消す** — 却下。accepted の ADR は記録であり、決定の置き換えは新しい ADR の `amends` で示す (ksn-core の改訂の型)。本文の直接改訂は proposed の ADR に限る
- **cross/ADR-0027 の footer の `関連:` 行に「本決定で置換そのものを撤去」と書くだけで済ませる** — 却下。`関連:` は置き換えずに補う場合の型で、accepted の決定の一部を撤去したことが `amends` / `amended-by` / index のどこにも現れない。0024 を開いた読み手は順 8 を現行として読む
- **0024 全体を supersede する** — 却下。撤去したのは順 8 と付随する 1 節だけで、起動・段構成・順 1〜7・再実行の範囲・tag の扱いはそのまま効いている。全体を置き換えると、維持している決定の出所が失われる

## Consequences

- 正: publish が外部へ書き込む先が配信先だけになり、リポジトリへの書き戻しに由来する失敗経路 (置換・lint・git push の競合) が publish から消える
- 正: 0024 を開いた読み手が、現行でない順 8 を現行として読むことがなくなる
- 負: cross/ADR-0024 の順序表は 8 段のまま残り、順 8 が現行でないことは `amended-by` を辿らないと分からない
- 負: インストール例の正しさを担保するのは日常の検証 CI だけになり、リリースの側に歯止めが無くなる (cross/ADR-0029 の Consequences と同じ)

## Revisit When

- インストール例が再び具体 version を持つようになったとき (cross/ADR-0027 を見直すとき)
- release workflow がリポジトリへ何かを書き戻す必要が生じたとき

---
出典: kasane/changes/archive/2026-09-13-install-examples-and-release-notes/proposal.md (What Changes 1・2) / 同 design.md (Decision 1・Migration Plan) / kasane/changes/archive/2026-09-13-install-examples-and-release-notes/review-001.md (accepted ADR との衝突の指摘)
関連: cross/ADR-0024 (publish の順序と tag の扱い。本決定で順 8 と「tag と README の扱い」後半を置き換え) / cross/ADR-0027 (インストール例は具体 version を持たない — 撤去の根拠) / cross/ADR-0029 (契約の検査を日常の lint job へ移す) / cross/ADR-0016 (ブランチモデル。README 置換の扱いは対象そのものが無くなった)
