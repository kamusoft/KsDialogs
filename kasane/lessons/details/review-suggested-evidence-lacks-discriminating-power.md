---
scope: code-review
kind: pain
severity: normal
count: 3
first-seen: 2026-09-02
last-seen: 2026-09-10
evidence:
  - fix-kmp-ios-unhandled-exception-crash (review-001 Minor 2 が「提示先不在を起動後 N ms 強制する A/B」を推奨したが、強制はライブラリ側の判定にしか効かず、Sample の待ちは UIApplication を独立に見るため待ちの効果を識別できない手順だった。review-002 (同じ規律・別文脈) が推奨の不成立を認め、実装側が代替した「同じ述語で窓の実在を直接測る」実測を妥当と判定)
  - add-kmp-maven-distribution (提案段階。spec の Scenario「SNAPSHOT の Central 発行拒否」の受け入れ条件が「Central 向けタスクが失敗する」だけで、ガード未実装でも認証情報不足・ネットワーク制限で同じ失敗が出て成立してしまう形だった。同じく `.module` の受け入れ条件が存在確認だけで variant / 依存の欠落を検出できなかった。ホストのスペックレビューは通し、相方 second-opinion-spec-001 が Major 2 件で検出。SNAPSHOT 固有の診断文言と `.module` の内容検査を条件に足して解消)
  - add-consumer-verification (提案段階。KMP 固有の依存検査 (5 publication・core の版・metadata の URL / exact / deployment target・2 つの Package.swift の URL 一致・pin の一意性) を独自解析で行う設計に、検査自体が壊れて空振りしても正ケースだけでは検出できない状態が残っていた。ホストのスペックレビューは通し、相方 second-opinion-spec-001 #7 が Major で検出。`--selftest` (負の入力 5 種) と tasks 5.2 (i) を足して解消)
---

## ルール文

レビュー (コードレビュー・スペックレビュー) で証跡の取り方 (A/B・強制再現) や Scenario の受け入れ条件を推奨・承認するときは、その手順が「証明したい命題の真偽で結果が分かれるか」を先に確かめて書く — 具体的には、操作する箇所 (強制する判定) と観測する箇所 (修正が見ている述語) が同じ入力を見ているかを追う。分かれない手順を推奨すると、実装側がそれに従って無意味な証跡を積むか、代替の妥当性を巡って 1 サイクル余計に使う。Scenario の受け入れ条件なら「その機構を丸ごと外しても同じ観測が得られないか」を問い、機構固有の印 (診断文言・生成物の内容) を条件に入れる。

## 経緯

- 2026-09-02 fix-kmp-ios-unhandled-exception-crash: レビュアーはライブラリ側の判定を偽装する案を出したが、Sample の待ちはライブラリを経由せず同じ OS 状態を直接見る構造だった。実装側が構造を説明して代替を取り、次のレビューで承認された
- 2026-09-09 add-kmp-maven-distribution: 提案段階の同型。受け入れ条件が「失敗する」「ファイルが存在する」で止まり、機構の有無で結果が分かれなかった。scope は spec-review 寄りだが、命題の真偽で結果が分かれるかを問う規律は同じなので同一パターンとして数える
- 2026-09-10 add-consumer-verification: 提案段階の同型 3 件目。検査スクリプトの受け入れ条件が「正ケースで通る」だけで、検査対象が空でも・パーサーが行を取り違えても成功する形だった。機構固有の負の入力 (欠落・別版・不一致・0 件と 2 件) で失敗することを条件に含める必要があった
