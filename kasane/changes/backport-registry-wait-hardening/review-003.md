# レビュー結果: backport-registry-wait-hardening (003 回目)

**日付**: 2026-09-13
**判定**: CHANGES_REQUESTED

## サマリー

review-002 が名指しした Major は解消した。`cmd_wait_published` に単一の誤実装を与える写しを 16 通り作って
実測したところ、**停止 (待機が終わらない) するものは 1 つも無い**。review-002 の 4 種
(`PUBLISHED` を外さない / 枠ごとに待ち切る / `FAILED` 非終端 / `NOT_FOUND` 非終端) はいずれも数秒で NG になる。
根治の `""` 分岐が本番経路に悪影響を与えないことも、状態照会が 500 を返す模擬で確認した (到達しない)。

一方、その根治が**既存の検査の判別力を食っている**。公開待ちの上限判定 (`deadline`) を丸ごと外しても
自己テストは緑で通る。上限を名乗る 2 つの検査 (「上限を過ぎれば失敗する」「2 件とも未公開のまま上限を
過ぎれば失敗する」) が、いま上限とは無関係な理由 (台本切れ → `""` → 失敗) で通っているためで、
**今回の修正前はこの誤実装は停止として表れていた** (赤) のが、緑に変わった。加えて、根治の `""` 分岐
そのものを消しても緑のままで、deviation が「構造的に満たすための変更」と位置づけた機構が無検査になっている。
どちらも遡及案 (計 4 行) を作って、正しい実装では緑・誤実装では NG になることを実測した。

## 照合した規約

- `kasane/handbook/cross/comment-policy.md` (always) — 許容参照・禁止類型・そのファイルだけを読む人にとって正であること
- `kasane/handbook/cross/verification-ci.md` (`.github/workflows/` を変えるとき)
- `kasane/handbook/cross/ci-script-deletion.md` (`scripts/**` `.github/workflows/**` を作る・翻案するとき)
- `kasane/handbook/cross/release-procedure.md` (release workflow を触るとき)
- `kasane/lessons/code-review.md` L-001 (受け入れ条件の判別力 — 「その機構を丸ごと外しても同じ観測が
  得られないか」を問う)。本レビューの主眼はこの観点そのもの
- `kasane/decisions/cross/0026` (本 change が起票、`status: proposed`) / `0022` / `0020`

review-002 で ✅ とした項目 (時間予算の式・step 順序・ADR-0026 の整合・`wait-for-registries.sh` の健全性) は
再検証の対象外とし、今回の修正が壊していないかだけを確認した (下記「前回 ✅ を壊していないこと」)。

## ビルド・テストの実行結果

| 実行 | 結果 |
|---|---|
| `scripts/release/*.sh --selftest` 8 本 | 全件 exit 0 (合計 14 秒。`central-portal.sh` 5 秒) |
| `python3 scripts/release/check-time-budget.py --selftest` / 本検査 | 15 件 / 失敗なし、exit 0 (publish 112/150、wait-for-registries 52/60) |
| `python3 scripts/release/check-publish-step-order.py --selftest` / 本検査 | 8 件 / 失敗なし、exit 0 |
| `python3 scripts/comment-policy-lint.py` | exit 0 (禁止 0 件 / 検査対象 1004 ファイル) |
| `python3 scripts/local-path-lint.py` / `identity-lint.py` | 両方 exit 0 |

## 誤実装の網羅スイープ (本レビューで実施)

実物の `scripts/release/central-portal.sh` を写し、**1 箇所だけ**書き換えて `--selftest` を走らせた
(上限 100 秒。`rc=124` が停止)。review-002 の 4 種に加えて 12 種を新たに与えた。

**停止として表れたものは 1 件も無い** (単一の誤実装では)。

| 与えた誤実装 | 結果 |
|---|---|
| `PUBLISHED` の枠を `remaining` から外さない | NG 8 件 / 5 秒 |
| 枠ごとに待ち切る (`*)` の後に `break`) | NG 4 件 / 5 秒 |
| `FAILED` 非終端 (分岐を落とす) | NG 2 件 / 4 秒 |
| `NOT_FOUND` 非終端 (分岐を落とす) | NG 1 件 / 5 秒 |
| 先頭 ID しか見ない (`pending=("$1")`) | NG 6 件 / 1 秒 |
| `remaining` を毎周リセットしない | NG 8 件 / 5 秒 |
| 公開待ちの上限を検証待ちと同じ変数にする | NG 1 件 / 2 秒 |
| `deployment_state` が 404 で空を返す | NG 3 件 / 2 秒 |
| 残りが 0 件でなくても成功で返す (`-eq` → `-ge`) | NG 8 件 / 1 秒 |
| `PUBLISHING` も公開済みと見なす | NG 8 件 / 1 秒 |
| **公開待ちの上限判定 (`deadline`) を丸ごと外す** | **NG 0 件 / exit 0 (緑)** ← Major |
| **根治の `""` 分岐を外す** | **NG 0 件 / exit 0 (緑)** ← Minor-1 |
| 上限の単位を取り違える (`SECONDS + timeout * 60`) | NG 0 件 / exit 0 (緑) ← Suggestion |
| 台本切れを「戻り値 1」へ戻す | NG 0 件 / exit 0 (緑) ← Minor-2 の材料 |
| 状態照会の非 200 を失敗にしない (`deployment_state`) | NG 0 件 / exit 0 (本 change の変更箇所外) |
| 空の `deploymentState` を通す (`parse_deployment_state`) | NG 0 件 / exit 0 (本 change の変更箇所外) |

2 箇所を同時に壊した場合のみ停止が現れる (参考):

| 与えた誤実装 (2 箇所) | 結果 |
|---|---|
| 根治の `""` 分岐を外す + `FAILED` 非終端 | NG 2 件 / 12 秒 |
| 根治の `""` 分岐を外す + `NOT_FOUND` 非終端 | NG 1 件 / 6 秒 |
| **根治の `""` 分岐を外す + 上限判定を外す** | **100 秒でも終わらず停止** |

最後の行が Major の根拠。上限判定の誤実装は、根治が入る前は停止 (赤) として表れていたが、
根治が入ったことで**緑**に変わった。

## 指摘事項

### [🟠 Major] 公開待ちの上限判定を外しても自己テストが緑になる (上限を名乗る 2 検査が判別力を失った)

**該当箇所**: `scripts/release/central-portal.sh:577-579` (「上限を過ぎれば失敗する」) /
`:615-617` (「2 件とも未公開のまま上限を過ぎれば失敗する」)。対象の実装は `:319-321` の `deadline` 判定。

**問題点**:

`cmd_wait_published` から上限判定

```
        if [ "${SECONDS}" -ge "${deadline}" ]; then
            fail "公開を待ちきれなかった (上限 ${timeout} 秒、最後の状態 ${states})"
        fi
```

を丸ごと外した写しで `--selftest` を走らせると、**NG 0 件 / exit 0 / 2 秒**で通る。
上限を名乗る 2 つの検査は「失敗したか」しか見ておらず、その失敗は上限ではなく台本切れ
(`000` → `deployment_state` が `fail` → `""` → 今回追加の `""` 分岐) で起きている。
どちらの検査も、上限の機構を丸ごと外しても同じ観測が得られる (`lessons/code-review.md` L-001)。

上限は飾りではない。`scripts/release/check-time-budget.py` が読む `KSR_PUBLISHED_TIMEOUT_SECONDS:-5400` は
`release.yml` の `timeout-minutes: 150` と突き合わされており、ADR-0026 の決定もこの関係に乗っている。
定数は検査されるが、**定数を使う挙動が検査されない**状態になっている。上限判定が壊れた publish は
150 分の job timeout まで居座り、`wait-for-registries` 以降を道連れにする。

なお、これは今回の修正が持ち込んだ後退である。根治が入る前 (「`""` 分岐を外す + 上限判定を外す」) では
同じ誤実装が 100 秒でも終わらない停止として表れていた。停止よりは緑の方が静かで危険が大きい。

**推奨修正**: 上限の 2 検査に照会回数の表明を添える (この change が `:544` `:564` で既に採った形と同じ)。
上限が効いていれば 1 巡で止まるため、台本切れまで回る誤実装と分かれる。

```
    check "$([ "$(calls | wc -l | tr -d ' ')" = "1" ] && echo 0 || echo 1)" \
        "上限で止まるので 1 巡で終わる" "$(calls)"
```

(2 件の側は `= "2"`)。実測で効果を確認済み:

| 写し | 結果 |
|---|---|
| 遡及案 2 行を追加 (正しい実装) | 失敗なし / 2 秒 (緑のまま) |
| 同上 + 上限判定を外す | **NG 2 件 / 2 秒** (検出できる) |

---

### [🟡 Minor] 根治の `""` 分岐が無検査 — 消しても自己テストが緑のまま通る

**該当箇所**: `scripts/release/central-portal.sh:304-308`

**問題点**:

deviation.md は `""` 分岐を「Scenario『待ちの誤実装が停止ではなく失敗として表れる』を**構造的に**
満たすための変更」と位置づけている。ところがこの分岐を丸ごと外した写しは NG 0 件 / exit 0 で通る
(スイープ表)。分岐にはコメントで「本番では到達しない」とも読める説明が付いているため、
将来「死んだ分岐」として落とされる筋は十分にある。落とした瞬間、上限判定を外す誤実装は再び停止に戻る
(2 箇所同時の表の 3 行目が実測)。構造的な保証を名乗るなら、その構造が消えたことに気づく手当が要る。

**推奨修正**: 既存の「台本が尽きても待機が終わらなくなるのではなく失敗する」(`:633-635`) に
照会回数の表明を 1 つ添える。答えの返らない照会は待たずに止まるので、照会は 2 件で終わる。

```
    check "$([ "$(calls | wc -l | tr -d ' ')" = "2" ] && echo 0 || echo 1)" \
        "答えの返らない照会は待たずに止まる (上限まで空回りしない)" "$(calls)"
```

実測:

| 写し | 結果 |
|---|---|
| 遡及案 1 行を追加 (正しい実装) | 失敗なし / 2 秒 |
| 同上 + `""` 分岐を外す | **NG 1 件 / 2 秒** |

---

### [🟡 Minor] `[wait-published]` 冒頭のコメントの理由が、現在のコードと噛み合っていない

**該当箇所**: `scripts/release/central-portal.sh:560-564`

**問題点**:

コメントは「終端の状態を終端として扱わない誤った実装を与えると待機が止まらない。上限を既定 (90 分) の
ままにすると、その誤りは検査の失敗ではなく自己テストの停止として表れる」を、短い上限を与える理由として
書いている。この説明は根治が入る前の状態のものだった。現在は `""` 分岐があるため、**短い上限を全部
取り去っても**終端の取りこぼしは停止しない — 実測 (失敗を期待する 3 検査から `KSR_PUBLISHED_TIMEOUT_SECONDS`
を外した写し): 正しい実装で緑、`FAILED` 非終端で NG 2 件 / 2 秒、`NOT_FOUND` 非終端で NG 1 件 / 2 秒。

短い上限を残すこと自体は二重の守りとして妥当だが、理由が現状と食い違ったままだと、
このファイルだけを読む人が「停止を防いでいるのは上限だ」と読み、`""` 分岐の方を安全に消せると判断する
(上の Minor と同じ穴につながる)。`comment-policy.md`「そのファイルだけを読んでいる人にとって意味が通る」。

**推奨修正**: 二層であることを書く。「答えの返らない照会は `cmd_wait_published` 側が待たずに止める。
短い上限はその外側の保険で、上限に達したことだけでは終端の取りこぼしと区別がつかないため、
照会回数の表明も添える」といった形。同じ理由づけが deviation.md の `[自己テストの構造]` にもあるので、
蒸留のときに揃える。

---

### [🔵 Suggestion] 上限の単位を取り違える誤実装 (秒 → 分) も緑で通る

**該当箇所**: `scripts/release/central-portal.sh:284` (`local deadline=$(( SECONDS + timeout ))`)

`SECONDS + timeout * 60` に変えた写しは NG 0 件 / exit 0 で通る (上限 0 の検査は 0 のまま効き、
上限 5 の検査は台本切れが先に来るため)。90 分の待ちが 90 時間になる誤りで、本番では job timeout が
受け止めるが、自己テストは何も言わない。Major の遡及案 (照会回数の表明) を入れてもこの種は残る。
本 change のスコープが要求する話ではないので、入れるなら蒸留の申し送り扱いでよい。

---

### [🔵 Suggestion・本 change の変更箇所外] `deployment_state` / `parse_deployment_state` の 2 検査も判別力が弱い

`deployment_state` から非 200 の `fail` を落としても、`parse_deployment_state` が空の `deploymentState` を
通すようにしても、自己テストは緑で通る (いずれも本文が JSON として解釈できないか、`""` 分岐が
受け止めるため)。「200 以外の状態照会で失敗する」という検査は、機構を外しても同じ観測になる。
本 change 以前からある構造なので指摘にとどめる。

## 前回の指摘のうち解消を確認したもの

| review-002 の指摘 | 状態 |
|---|---|
| Major (自己テストが停止する誤実装が残っている) | ✅ 解消。名指しの 4 種を含む単一の誤実装 16 種で停止は 0 件 (スイープ表)。ただし上限判定の 1 種が停止から緑へ変わった (上記 Major) |
| Minor (`handover-android` / `handover-kmp` に消費者が無い) | ✅ 解消。2 つの output は削除され、`steps.deployment-ids.outputs` の参照は `ready` 3 箇所 (`.github/workflows/release.yml:1381` `:1411` `:1442`) と `handover` 1 箇所 (`:1484`) のみ。枠ごとの内訳は `:782` のログ行に残る |
| Suggestion (`env:` 上書きの前提が記録されていない) | ✅ 解消。`tasks.md` の申し送りに「時間予算の検査はスクリプト側の定数リテラルだけを読み、workflow の `env:` による上書きは見ない」の 1 行が入った |

## production の変更 (`""` 分岐) が本番経路に与える影響

**影響なし**を実測で確認した。`deployment_state` (`scripts/release/central-portal.sh:208-226`) は
404 なら `NOT_FOUND`、非 200 なら `fail`、`deploymentState` を取り出せなければ `fail` で、
空文字を返す経路を持たない。`fail` はコマンド置換のサブシェルを終わらせるが、`set -e` (`:68`) の下では
`state="$(deployment_state "${id}")"` の代入自体が失敗ステータスになり、`case` に入る前にスクリプトが終わる。

実測 (`http_request` を「常に HTTP 500」に差し替えた写しで本番のサブコマンドを起動。ネットワークには出ない):

```
$ MAVEN_CENTRAL_USERNAME=... MAVEN_CENTRAL_PASSWORD=... bash <写し> wait-published aaa bbb
::error::deployment の状態を照会できない (HTTP 500): aaa
exit=1
```

`""` 分岐の文言は出ておらず、従来と同じ診断・同じ終了コードで止まる。`""` 分岐が効くのは
`set -e` の効かない文脈から呼ばれたとき (= 自己テスト) だけ。deviation.md の記述と一致する。

## 前回 ✅ を壊していないこと

- 時間予算: 自己テスト 15 件緑、本検査 exit 0。出力は spec の 2 つの表と全項一致
  (90 / 5 / 1 / 16、余裕 38、上限 150。45 / 2 / 5、余裕 8、上限 60)。自己テストに追加された
  `KSR_PUBLISHED_TIMEOUT_SECONDS=5` の行は、定数の読み取りが `:-` 形に限定されているため
  (`scripts/release/check-time-budget.py:147-149`)「値が割れている」に当たらない
- step 順序: 自己テスト 8 件緑、本検査 exit 0
  (`Download previous deployment ids` → `Prepare deployment id files` → `Download Android artifacts`)。
  `handover-android` / `handover-kmp` の削除は step の並びを動かしていない
- `wait-for-registries.sh` は今回も変更なし (自己テスト exit 0)。ADR-0026 と `.github/workflows/ci.yml` も変更なし
- `comment-policy-lint.py` / `local-path-lint.py` / `identity-lint.py` は全件緑。今回追加のコメントに
  禁止参照 (作業文書のパス・レビュー通番・デルタスペック構文キーワード) は無い
- 足場 (`specs/` `proposal.md`) は今回も更新されていない (更新されたのは `deviation.md` / `tasks.md` と実装のみ)

## アクションプラン

1. **Major**: 上限の 2 検査 (`scripts/release/central-portal.sh:577-579` / `:615-617`) に照会回数の表明を足す
   (計 2 行、実測済み)
2. **Minor**: 「台本が尽きても…」の検査 (`:633-635`) に照会回数の表明を足し、`""` 分岐の削除を検出できるようにする
   (1 行、実測済み)
3. **Minor**: `:560-564` のコメントを二層 (`""` 分岐が主・短い上限が保険) の説明に直す。
   deviation.md の `[自己テストの構造]` の理由も蒸留で揃える
4. **Suggestion**: 上限の単位取り違えと `deployment_state` 系の判別力は、蒸留の申し送りに回すか見送る
5. tasks 6.3 (`develop` への push で lint job) と 6.4 (`dry-run` の release) は引き続き未了。
   6.1 / 6.2 は本レビューで実行して通ることを確認した
