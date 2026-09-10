# レビュー結果: fix-release-published-wait (001 回目)

**日付**: 2026-09-10
**判定**: CHANGES_REQUESTED

## サマリー

`wait-published` の複数 ID 化と step の組み替え自体は素直で、bash 3.2 (macOS ランナーの `/bin/bash`) でも空配列展開に触れない書き方になっており、`if` の畳み方 (要求 step の skip / 失敗の両方で待ち step が skip される) も正しい。一方で、**PUBLISHED 待ちの上限を引き上げたのは 3 箇所ある待ちのうち 1 箇所だけ**で、この change が直そうとした「上限 30 分では Central の同期 (実測 60 分) に足りない」という原因が、リリース手順が唯一の復旧手段として定める再実行の経路 (`central-resume.sh` の `wait-published` 動作) にそのまま残っている。あわせて、複数 ID の自己テストが「直列に待つ実装」と結果を分けられない (コメントの主張と実挙動が食い違う) 点と、job timeout の根拠コメントが自分の数え上げと合わない点を挙げる。

### 実行した検証

| 検査 | 結果 |
|---|---|
| `bash scripts/release/central-portal.sh --selftest` | exit 0 / OK 58 件 / NG 0 件 |
| `actionlint .github/workflows/release.yml` | error 0 件 (info 2 / style 1 は変更前と同一・同内容) |
| `shellcheck scripts/release/central-portal.sh` | exit 0 |
| `local-path-lint.py` / `identity-lint.py` / `doc-structure-lint.py` / `comment-policy-lint.py` | すべて exit 0 (doc-structure の「注意」は既存文書のみ) |
| `/bin/bash --version` | 3.2.57 — 新規コードは空配列の `"${arr[@]}"` 展開を早期 return で回避しており、この版で通ることを実行で確認 |

## 照合した規約

- `kasane/handbook/cross/comment-policy.md` (always) — 新規コメントに作業文書パス・change 識別子・ローカル通番の参照なし。適合
- `kasane/handbook/cross/verification-ci.md` (`.github/workflows/` を変えるとき) — lint job の 8 検査に `scripts/release/**` の自己テストは含まれない (Suggestion 1 参照)
- `kasane/handbook/cross/release-procedure.md` (リリース手順・再実行。本 change の変更対象)
- `kasane/handbook/cross/ci-script-deletion.md` (`scripts/**` / `.github/workflows/**`) — 削除操作の追加なし。非該当
- 参照した決定: cross/ADR-0024 (proposed)。順 6「Maven release 2 件 (Android → KMP) → published 待ち」と新しい step 構成は矛盾しない
- 参照した spec: `kasane/changes/add-release-workflow/specs/release-workflow/spec.md`「Maven Central の 2 枠の deployment」「同じ version での再実行」

---

## 指摘事項

### [🟠 Major] PUBLISHED 待ちの上限引き上げが再実行の経路に届いておらず、元の失敗原因がそのまま残る

**該当箇所**: `.github/workflows/release.yml:912`、`.github/workflows/release.yml:1062` (引き上げたのは `:1213` の 1 箇所のみ)

**問題点**:

publish job には PUBLISHED を待つ箇所が 3 つある。

| 待ちの箇所 | 上限 | 発火条件 |
|---|---|---|
| `Wait for Maven Central deployments to be published` (`:1208-1219`) | 5400 秒 (90 分) | この attempt で release を要求した枠 |
| `Publish Android to Central Portal` の `wait-published` 分岐 (`:911-913`) | **既定の 1800 秒 (30 分)** | 再実行で Android 枠が PUBLISHING のとき |
| `Publish KMP to Central Portal` の `wait-published` 分岐 (`:1061-1063`) | **既定の 1800 秒 (30 分)** | 再実行で KMP 枠が PUBLISHING のとき |

`KSR_POLL_TIMEOUT_SECONDS` はリポジトリ全体で `:1213` の 1 箇所にしか無く (`grep` で確認)、後者 2 つは `central-portal.sh:277` の既定 1800 を使う。exploration が記録した実測は Android 枠 約 60 分 / KMP 枠 27 分 29 秒 なので、**この change が「30 分では足りない」と判断した根拠がそのまま当てはまる経路が 2 つ残っている**。

しかもこの 2 経路は、`release-procedure.md` が唯一の復旧手段として定める「同じ version で失敗した job から再実行」で必ず通る道であり、spec の Scenario「release の応答が失われても再実行で整合する」が指す経路そのものである。さらに悪いことに、この 2 つは step が別なので**枠ごとに直列に**待つ (Android 枠の待ちが終わるまで KMP の step に進まない) — 本 change が「足し算になるから」と言って解消した直列待ちが、再実行時には最大 30 分 × 2 で復活する。

再実行がこの経路を通る現実的な引き金:
- 新設の待ち step が 90 分で時間切れになった後の再実行 (2 枠とも PUBLISHING)
- `Release Maven Central deployments` が Android の release だけ受理して KMP の release で失敗した場合 (Android は PUBLISHING のまま残り、drop もできない)。旧実装では Android は release + 待ちを 1 step で行っていたため PUBLISHED まで進んでいた可能性があり、**この change はむしろこの経路に入る確率を上げている**
- job timeout (150 分) やランナー障害で待ちの途中に落ちた場合

**推奨修正**: 次のいずれか。

1. 最小: `:882` と `:1034` の 2 step の `env` に `KSR_POLL_TIMEOUT_SECONDS: "5400"` を足し、なぜ 90 分なのかを 1 箇所 (例えば job の env か `central-portal.sh` の既定) に集約する。直列に 2 本並ぶことは job timeout の根拠コメント (Minor 2) にも反映する
2. より筋が良い: 再実行で PUBLISHING だった枠を **その場で待たずに** 後段の待ち step へ回す。`maven-android` / `maven-kmp` が `wait-published` 動作のとき `release-needed=false` のまま `already-publishing=true` のような output を追加で出し、`Wait for ...` step の待機対象を「今回 release した枠 + 既に PUBLISHING の枠」にする。再実行でも待ちが 1 本にまとまり、上限も 1 箇所で済む

どちらを採るにせよ、`release-procedure.md:173` の「Maven の release」行と `:145` の「上限 90 分」の記述が、再実行時の待ちにも同じ上限が効くことと読める形に揃っていること。

---

### [🟡 Minor] 複数 ID の自己テストが「直列に待つ実装」と結果を分けられず、コメントの主張が成り立たない

**該当箇所**: `scripts/release/central-portal.sh:553-564`

**問題点**:

`:554-555` のコメントは「先に公開された枠は待機対象から外れるので、3 ループ目の照会は残った枠だけになる (**直列に待つ実装なら 2 枠目の照会が 1 ループ目に現れず、この呼び出し順にならない**)」と書いているが、これは成り立たない。

台本 (`PUBLISHING, PUBLISHING, PUBLISHED, PUBLISHING, PUBLISHED`) に対する照会順は次のとおりで、**呼び出し件数も 4 件目の ID も一致する**。

| 実装 | 照会順 | 件数 | 4 件目 |
|---|---|---|---|
| 現行 (1 ループで全 ID) | aaa, bbb, aaa, bbb, bbb | 5 | bbb |
| 直列 (ID ごとに公開まで待ち切る) | aaa, aaa, aaa, bbb, bbb | 5 | bbb |

実際に `cmd_wait_published` を直列版 (`for id in "$@"; do while :; do ... done; done`) へ差し替えた写しで `--selftest` を回したところ、**新規 6 件がすべて OK になった** (手元で実行済み。evidence の「逆対照」は `("$1")` へ戻した版 = 先頭 ID しか見ない実装が対象で、直列版は対照に入っていない)。

つまり `:561-562`「公開済みの ID は照会しなくなる」と `:563-564`「残った枠だけを照会し続ける」は、**この change が入れた並行待ちの有無で結果が分かれない**。lessons `code-review` L-001 の「操作する箇所と観測する箇所が同じ入力を見ているか」「その機構を丸ごと外しても同じ観測が得られないか」に抵触する。

(念のため: `:566-572` の FAILED 系 2 件はコメントどおり「先頭の ID しか見ない実装」と結果を分ける。問題があるのは上の 2 件とそのコメント。)

**推奨修正**: 並行と直列を分ける観測点は **2 件目の照会**。次のいずれかを足し、コメントを実際に分かれる根拠に書き直す。

```bash
    check "$(contains "$(calls | sed -n 2p)" "id=bbb")" \
        "1 ループで全枠を照会する (直列なら 2 件目も aaa になる)" "$(calls | sed -n 2p)"
```

「公開済みの ID は照会しなくなる」を残すなら、件数だけでなく **3 ループ目に aaa が現れないこと** (`calls | sed -n 5p` が `id=bbb`) を条件にしたほうが、毎ループ全件を照会し続ける実装との差が明示的になる。

---

### [🟡 Minor] job timeout の根拠コメントが、自分が数え上げた最悪ケースを下回っている

**該当箇所**: `.github/workflows/release.yml:548-553`

**問題点**:

コメントは「検証待ちは最悪で**枠ごとに** 2 本 (30 分 → 30 分) 並び、公開待ちは 2 枠まとめて 1 本 90 分なので、本体の作業を足しても収まる予算にする」と書いて `timeout-minutes: 150` を正当化しているが、この数え上げどおりなら 30 × 2 本 × 2 枠 + 90 = **210 分**で、本体の作業を足す前に 150 を超える。加えて、この列挙には再実行時の `wait-published` (30 分 × 2 枠、Major 1) が入っていない。

exploration の決定は「前段の upload / 検証 / NuGet 約 10 分 + KMP 再ビルド + 待ち 90 分」という**実測ベース**の見積りで 150 分としており、これはオーナーの決定として妥当。問題は、コメントが実測ベースの予算を「最悪ケースが収まる」と書いてしまっていること (コメント単独で読んだときに誤った安心を与える)。

**推奨修正**: 「最悪ケースが収まる」ではなく、実測を根拠にした予算であることと、上限が効くのはどの待ちかを書く。例:

```
    # 既定の 360 分は長すぎる。Android と KMP の発行と Central Portal の待ちを含む
    # 直列の publish に対する上限として置く。実測 (検証待ちは数分、公開待ちは遅い枠で
    # 約 60 分) に本体の作業 (upload / 検証 / NuGet 約 10 分 + KMP 再ビルド) と
    # 公開待ちの上限 90 分を足した予算。待ちがすべて上限まで伸びる最悪ケースはこれを
    # 超えるが、その場合は job timeout で止めて再実行に回す (deployment ID は
    # upload の直後に保存済みで、次の attempt が状態を見て続きを行う)。
```

---

### [🔵 Suggestion] `scripts/release/**` の自己テストが CI で回らない

**該当箇所**: `.github/workflows/ci.yml:305-321` (lint job)

**問題点**: lint job の 8 検査に含まれるスクリプト自己テストは `ci-skip-lint.py` / `readme-example-lint.py` / `sync-snapshot-test.sh` の 3 本で、`scripts/release/*.sh --selftest` は入っていない。今回追加した複数 ID の分岐は本番で年に数回しか通らず、退行を見張るのは自己テストだけなので、手元実行のみでは印が残らない。

本 change が作り込んだ問題ではない (`add-release-workflow` の時点からの状態) ため Suggestion に留める。足すなら lint job で `for f in scripts/release/*.sh; do bash "$f" --selftest; done` の 1 step。ネットワークへ出ない (`--selftest` は HTTP 送信関数をモックへ差し替える) ので lint job で完結する。別 change として起票するのが妥当と考える。

---

### [🔵 Suggestion] 複数 ID の待ちで FAILED を見つけたとき、他の枠の状態が記録に残らない

**該当箇所**: `scripts/release/central-portal.sh:288-291`

**問題点**: `for` ループの中で `fail` するため、FAILED を見つけた時点で残りの ID は照会されずに終わる。運用上、片方が FAILED のときにもう一方が PUBLISHING (drop 不可・次の attempt が引き継ぐ) なのか VALIDATED (drop される) なのかは復旧の判断材料になるが、この step のログには出ない。Summarize は ID を出すだけで状態は出さない。

**推奨修正**: 必須ではない。残すなら、ループ内では FAILED / NOT_FOUND を記録だけして、ループを抜けてから全枠の最終状態を含めて `fail` する形にする。`release-procedure.md` の「失敗したとき」に Portal 一覧で両枠を見る導線が既にあるので、現状のままでも運用は成立する。

---

## アクションプラン

1. **Major 1** — 再実行経路の `wait-published` に 90 分の上限を効かせる (最小案) か、待ちを後段の 1 本へ寄せる (推奨案)。あわせて `release-procedure.md` の記述を揃える
2. **Minor 2** — 自己テストの観測点を「2 件目の照会」に変え、コメントの主張を実際に分かれる根拠へ書き直す
3. **Minor 3** — job timeout の根拠コメントを実測ベースの予算として書き直す
4. Suggestion 2 件 — 別 change として起票するか見送るかをオーナー判断で
