# レビュー結果: backport-registry-wait-hardening (002 回目)

**日付**: 2026-09-13
**判定**: CHANGES_REQUESTED

## サマリー

review-001 の Minor 1 件 (ci.yml のコメント) と Suggestion 2 件 (実行権限・申し送り) は解消を確認した。
Major についても、review-001 が名指しした誤実装 (`PUBLISHED` の枠を `remaining` から外さない) は
停止から失敗へ変わっている (実測 23 秒 / NG 8 件)。

一方で Major は**解消していない**。対策が「成功を期待する 4 検査に上限を与える」という形に限定された結果、
`cmd_wait_published` を呼ぶ検査のうち上限を持たない 3 件 (いずれも失敗を期待する検査) が残り、
そこで停止する誤実装が**別に 2 種類**見つかった。しかも停止するのは
「FAILED を終端として扱わない」「NOT_FOUND を終端として扱わない」という、
まさにその検査自身が捕まえるべき誤実装である。1 種類の誤実装を塞いだが、同型の穴は塞がっていない。

## 照合した規約

- `kasane/handbook/cross/comment-policy.md` (always) — コメントはそのファイルだけを読む人にとって正であること
- `kasane/handbook/cross/verification-ci.md` (`.github/workflows/` を変えるとき)
- `kasane/handbook/cross/ci-script-deletion.md` (`scripts/**` `.github/workflows/**` を作る・翻案するとき)
- `kasane/handbook/cross/release-procedure.md` (release workflow を触るとき)
- `kasane/decisions/cross/0026` (本 change が起票、`status: proposed`) / `0022` / `0020`

review-001 で ✅ とした項目 (待ち対象 10 件・Maven 2 枠・時間予算の式・観点 3/4/5・ADR-0026 の整合) は
再検証の対象外とし、今回の修正がそれらを壊していないかだけを確認した (下記「前回 ✅ を壊していないこと」)。

## ビルド・テストの実行結果

| 実行 | 結果 |
|---|---|
| `scripts/release/*.sh --selftest` 8 本 | 全件 exit 0 (合計 15 秒) |
| `python3 scripts/release/check-time-budget.py --selftest` / 本検査 | exit 0 / exit 0 (publish 112/150、wait-for-registries 52/60) |
| `python3 scripts/release/check-publish-step-order.py --selftest` / 本検査 | exit 0 / exit 0 |
| `python3 scripts/comment-policy-lint.py` | exit 0 (禁止 0 件 / 検査対象 1004 ファイル) |
| `python3 scripts/local-path-lint.py` / `identity-lint.py` | 両方 exit 0 |

正しい実装に対しては全件緑。落ちるのは誤実装を与えたときだけ (下記の実測表)。

## 指摘事項

### [🟠 Major] 自己テストが停止する誤実装が**まだ**残っている (対策が成功期待の検査にしか掛かっていない)

**該当箇所**:
`scripts/release/central-portal.sh:544` (「存在しない deployment の公開待ちは失敗する」) /
`:564` (「FAILED になれば失敗する」) / `:600` (「2 件のうち 1 件が FAILED なら失敗する」)。
あわせて `scripts/release/central-portal.sh:553-556` (今回追加されたコメント)。

**問題点**:

今回の修正は `KSR_PUBLISHED_TIMEOUT_SECONDS=5` を**成功を期待する 4 検査**
(`:559` `:574` `:586` `:614`) に与えた。しかし停止が起きる条件は「成功を期待するか」ではなく
「`cmd_wait_published` が上限なしで回りうるか」であり、失敗を期待する 3 検査も同じ条件を満たす。

原因は今回のコメントが書いているとおりで、`cmd_wait_published` の中の
`state="$(deployment_state "${id}")"` はコマンド置換のため、`deployment_state` の `fail` が
サブシェルだけを終わらせて外側の `while` に戻る。`state` が空のまま `*)` に落ち、その ID は
`remaining` に残り続ける。検査自体が `if ( ... )` の条件で呼ばれているので `set -e` も働かない。
上限を与えていない検査は既定 90 分 (`KSR_PUBLISHED_TIMEOUT_SECONDS:-5400`) を
`KSR_POLL_INTERVAL_SECONDS=0` で空回りする。lint job は `timeout-minutes: 10` なので、
job ごと打ち切られてどの検査が落ちたかの出力が残らない — Requirement が禁じている
「待機が終わらない形」そのもの。

実測 (すべて現在の成果物に対して、誤実装を 1 箇所だけ与えた写しで実行。90 秒で強制終了):

| 与えた誤実装 | 結果 |
|---|---|
| `PUBLISHED` の枠を `remaining` から外さない (review-001 が名指ししたもの) | 23 秒で NG 8 件 — **解消済み** |
| 枠ごとに待ち切る (直列化) | 6 秒で NG 1 件 (「1 ループで全枠を照会する」) — 検出できる |
| **`FAILED` を終端として扱わない** (`FAILED)` 分岐を落とす) | **120 秒でも終わらず `:564` で停止** (NG 0 件) |
| **`NOT_FOUND` を終端として扱わない** (`"${DEPLOYMENT_NOT_FOUND}")` 分岐を落とす) | **60 秒でも終わらず `:544` で停止** (NG 0 件) |

停止した 2 件は、いずれも停止した検査自身が捕まえる対象の誤実装である
(`:564` の名前は「FAILED になれば失敗する」、`:544` は「存在しない deployment の公開待ちは失敗する」)。
検査が守ろうとしている性質を壊すと、その検査が落ちる代わりに固まる。

また `:553-556` のコメントは「公開待ちを回す検査は、**成功を期待するものにも**短い上限を与える」と
書いており、実際には上限を持たない検査が 3 件ある。同じコメントの後半が理由として挙げているのは
「期待が『失敗』の検査は `if` の条件で呼ぶため set -e が働かず、台本が尽きた照会は未知の状態として
待機対象に残る」という、まさに**失敗期待の検査に当てはまる**説明で、規則と理由が噛み合っていない。
以後の検査追加でも同じ穴が空く (`comment-policy.md`: コメントはそのファイルだけを読む人にとって正であること)。

**推奨修正**:

1. 自己テスト内の `cmd_wait_published` 呼び出し**全件**に短い `KSR_PUBLISHED_TIMEOUT_SECONDS` を与える
   (残り 3 件は `:544` `:564` `:600`)。コメントも「成功を期待するものにも」ではなく
   「公開待ちを回す検査にはすべて短い上限を与える」に直す (`wait-for-registries.sh:676-679` の
   「巡回を回す検査には必ず短い上限を与える」と同じ趣旨・同じ言い回しに揃う)
2. 上限を足すだけだと、失敗期待の検査が「上限で失敗した」だけでも通ってしまい判別力を失う。
   終端状態の検査には照会回数の表明を添える (`:600` は既に「FAILED を見つけた時点で止まる」を持つ。
   `:544` と `:564` には無い)

実測で効果を確認済み:

| 写し | 結果 |
|---|---|
| 3 件に `KSR_PUBLISHED_TIMEOUT_SECONDS=5` を追加 (正しい実装) | 5 秒 / 失敗なし (緑のまま) |
| 同上 + 「`FAILED` 非終端」誤実装 | 13 秒で NG 1 件 (停止せず失敗) |
| 同上 + 「`NOT_FOUND` 非終端」誤実装 | 10 秒で **NG 0 件・exit 0** (停止はしないが**検出できない**) |
| 上に照会回数の表明 2 件を追加 (正しい実装) | 5 秒 / 失敗なし |
| 同上 + 「`FAILED` 非終端」誤実装 | 13 秒で NG 2 件 |
| 同上 + 「`NOT_FOUND` 非終端」誤実装 | 10 秒で NG 1 件 |

3. (代替案として) 根治は自己テスト側ではなく `cmd_wait_published` 側にもある。
   `deployment_state` が返せなかった (空の) 状態を `*)` で待機対象に残さず即座に失敗にすれば、
   上限の有無に関わらず停止しない。本番でも「状態を照会できない ID を 90 分待ち続ける」挙動が
   消える利点がある。どちらを採るかは実装側の判断だが、自己テスト側だけで塞ぐ場合は
   「検査を足すたびに上限を忘れない」という運用に依存し続ける点を認識した上で選ぶこと。

---

### [🟡 Minor・低優先度] `handover-android` / `handover-kmp` には依然として消費者が無い (前回 Minor-2 の半分)

**該当箇所**: `.github/workflows/release.yml:785` / `:787`

**問題点**: 集約の `handover` は `Summarize` の `KS_HANDOVER` (`:1485`) が読むようになり、
`Summarize` が `if: always()` (`:1454`) なので、引き継ぎの読み込みが失敗した実行では値が空になって
`未実行` と表示される。spec の「引き継ぎを読み込んだかどうかと、引き継ぎが存在したかどうかが
実行の出力から区別できる」はこれで実際に満たされた (前回の指摘の主眼は解消)。

一方、枠ごとの `handover-android` / `handover-kmp` は今も参照されていない
(`steps.deployment-ids.outputs` の参照は `ready` 3 箇所と `handover` 1 箇所のみ)。
spec は枠ごとの粒度を求めておらず、枠ごとの有無は `${slot} 枠の引き継ぎ: '...'` のログ行で既に分かる。
ksn-core の「スコープが要求しない拡張ポイント」に当たる。

**推奨修正**: 2 つの output を落とす (ログ行は残す)。残すなら読む step を決めてから残す。

---

### [🔵 Suggestion] 見送った `env:` 上書きの前提が、どこにも記録されていない

**該当箇所**: `kasane/changes/backport-registry-wait-hardening/tasks.md` の「蒸留への申し送り」

**問題点**: `release.yml` に `KSR_` で始まる記述が 1 件も無いことは確認した (grep 0 件) ので、
検査を変えない判断自体は妥当。ただし review-001 の Suggestion はその場合の受け皿として
「蒸留の申し送りに『予算の検査が読むのは既定値リテラルのみ』という前提として残す」を挙げており、
申し送りにはこの行が入っていない。前提が残らないまま将来 workflow 側で上書きを入れると、
検査は既定値を読んだまま緑で通る (`check-time-budget.py:84-98`)。
`KSR_POLL_TIMEOUT_SECONDS` が `central-portal.sh` の検証待ちと `wait-for-registries.sh` の反映待ちで
名前を共有している点も同じ前提に乗っている。

**推奨修正**: 申し送りに 1 行足す。

## 前回の指摘のうち解消を確認したもの

| review-001 の指摘 | 状態 |
|---|---|
| Minor-1 (`ci.yml` の `set-readme-version.py` 除外理由) | ✅ 解消。`.github/workflows/ci.yml:324-327` が「撤去が決まっているため載せない」になり、ADR-0026 の「対象外」行・tasks 4.1 と一致 |
| Minor-2 (`handover` 系 output に消費者が無い) | △ 半分解消 (上記 Minor)。集約の `handover` は `Summarize` が消費するようになった |
| Suggestion (実行権限が不揃い) | ✅ 解消。`check-publish-step-order.py` が 755 になり `scripts/release/` の全 11 本が 755 で揃った。`scripts/release/__pycache__` も無い |
| Suggestion (申し送りに `verification-ci.md` が漏れ) | ✅ 解消。`verification-ci.md` に加えて `ci-script-deletion.md` も追加されている |
| Suggestion (`env:` 上書き) | 見送り。前提の記録が無い (上記 Suggestion) |

## 前回 ✅ を壊していないこと

- 時間予算の検査は本検査・自己テストとも exit 0 で、出力は spec の 2 つの表と全項一致 (90/5/1/16 余裕 38、45/2/5 余裕 8)。
  `release.yml` の内訳コメント追加は数値を動かしていない
- step 順序の検査は本検査・自己テストとも exit 0。`Prepare deployment id files` への output 追加は
  step の並びを変えていない (`Download previous deployment ids` → `Prepare deployment id files` → `Download Android artifacts`)
- `wait-for-registries.sh` の自己テストは今回も同じ観点で再確認した。巡回を回す 3 検査すべてが
  `KSR_POLL_TIMEOUT_SECONDS=5` を持ち、台本切れの印 (`not_exhausted`) まで見ている。
  新たに 3 種類の誤実装を与えたが、いずれも停止せず失敗した:

  | 与えた誤実装 | 結果 |
  |---|---|
  | 期限を過ぎても照会を続ける (`can_start_probe` の break を外す) | 14 秒で NG 10 件 |
  | 反映済みでも完了と見なさない (`has_unreflected` が常に真) | 12 秒で NG 2 件 |
  | 未照会を未反映に畳み込む (初期値を `STATE_PENDING` に) | 4 秒で NG 8 件 |

  `central-portal.sh` が倣うべき形はこの `wait-for-registries.sh` 側にある
- `comment-policy-lint.py` / `local-path-lint.py` / `identity-lint.py` は全件緑
- ADR-0026 (`status: proposed`) の記述と `ci.yml` の step 構成は引き続き一致 (8 → 11 検査)

## アクションプラン

1. **Major**: `cmd_wait_published` を呼ぶ残り 3 検査 (`:544` `:564` `:600`) にも短い上限を与え、
   `:553-556` のコメントを「回す検査すべて」に直す。あわせて `:544` `:564` に照会回数の表明を足す
   (上限だけでは `NOT_FOUND` 非終端の誤実装を検出できないことを実測で確認済み)。
   または `cmd_wait_published` 側で「状態を取得できなかった ID」を待機対象に残さない形にする
2. **Minor**: `handover-android` / `handover-kmp` を落とす
3. **Suggestion**: 申し送りに「予算の検査が読むのは既定値リテラルのみ」を足す
4. tasks 6.3 (`develop` への push で lint job) と 6.4 (`dry-run` の release) は引き続き未了。
   6.1 / 6.2 は本レビューで実行して通ることを確認した
