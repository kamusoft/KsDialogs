# レビュー結果: fix-release-published-wait (002 回目)

**日付**: 2026-09-10
**判定**: APPROVED

## サマリー

前回の Major (再実行経路の公開待ちが 30 分のまま・枠ごとに直列) は、推奨した 2 案のうち筋の良い方 (待ちを後段の 1 本へ寄せる) で解消している。上限は `central-portal.sh` 側の専用変数 `KSR_PUBLISHED_TIMEOUT_SECONDS` (既定 5400) に寄り、`release.yml` から `KSR_POLL_TIMEOUT_SECONDS` は消えたので、workflow の指定漏れで 30 分に落ちる経路は無い。Minor 2 件も解消しており、うち自己テストの識別力は**手元で誤実装の写しを作って再現**し、直列版が新しい観測点でだけ NG になることを確かめた。残るのは記録 (deviation) の 1 件と自己テストの逆対照の見せ方 1 件で、いずれも優先度は低い。

### 実行した検証

| 検査 | 結果 |
|---|---|
| `/bin/bash scripts/release/central-portal.sh --selftest` | exit 0 / NG 0 件 (このマシンの `bash` は 3.2.57 のみ = macOS ランナーと同じ版) |
| `scripts/release/*.sh --selftest` 8 本 | すべて exit 0 / NG 0 件 |
| `actionlint .github/workflows/release.yml` | error 0 件 (info 2 / style 1 は変更前と同一) |
| `shellcheck scripts/release/central-portal.sh` | exit 0 |
| `local-path-lint.py` / `identity-lint.py` / `comment-policy-lint.py` (`--advisory` 含む) | exit 0。変更 3 ファイルに指摘なし |
| `doc-structure-lint.py` | exit 1 だが指摘 219 件はすべて今回触っていない既存文書 (`release-procedure.md` の指摘は 0 件) |
| 誤実装の写しによる A/B (下記 3 種) | 直列版・共有上限版がそれぞれ想定どおり検査を割る (詳細は Minor 1 の項) |

## 照合した規約

- `kasane/handbook/cross/comment-policy.md` (always) — 新規コメントに作業文書パス・change 識別子・ローカル通番・デルタスペック構文キーワードなし。「初回リリース 2026-09-10 の実測で〜」はアーカイブ文書に依存しない自己完結の根拠記述で、禁止する履歴記述 (過去仕様の説明・進捗ログ) には当たらないと判定
- `kasane/handbook/cross/verification-ci.md` (`.github/workflows/` を変えるとき) — lint job の 8 検査は変更なし。`scripts/release/**` の自己テストは相変わらず CI に載らない (Suggestion 1)
- `kasane/handbook/cross/release-procedure.md` (リリース手順・再実行。本 change の変更対象)
- `kasane/handbook/cross/ci-script-deletion.md` (`scripts/**` / `.github/workflows/**`) — 削除操作の追加なし。非該当
- 参照した決定: cross/ADR-0024 (proposed。順 6「Maven release 2 件 (Android → KMP) → published 待ち」と矛盾しない)
- 参照した spec: `kasane/changes/add-release-workflow/specs/release-workflow/spec.md`「Maven Central の 2 枠の deployment」「同じ version での再実行」

---

## 前回指摘の解消状況

### [🟠 Major] 上限引き上げが再実行の経路に届いていない → **解消**

推奨案 2 (待ちを後段の 1 本へ寄せる) が採られている。確認した点:

| 確認項目 | 結果 |
|---|---|
| 引き継ぎ枠が step 内で待たず output を出す | `.github/workflows/release.yml:918-921` / `:1073-1076` で `wait-published` 分岐は `pending_published` を立てるだけ。`:982` / `:1135` で `pending-published-id` を出力 |
| 待ち step が要求分と引き継ぎ分をまとめて待つ | `:1226-1244`。`ids` に `released-deployment-ids` + 2 枠の `pending-published-id` を並べ、語の分割で `wait-published` の可変長引数に開く。`cmd_wait_published` は 1 ループで全 ID を照会するので、再実行でも待ちは 1 本・上限も 1 つ |
| 待ち step の `if` | `:1227-1230` の「要求 / 引き継ぎのいずれかが 1 つでもある」で正しい。skip された step の output は空文字なので、`maven-release` が skip / 失敗したときは (状態関数の暗黙の `success()` と併せて) 待ちも skip される |
| 上限が指定漏れで 30 分に落ちないか | `KSR_POLL_TIMEOUT_SECONDS` は `release.yml` から消え (repo 全体を grep して確認)、`wait-published` は `scripts/release/central-portal.sh:283` の `KSR_PUBLISHED_TIMEOUT_SECONDS:-5400` だけを見る。呼び出し側が何も渡さなくても 90 分。`wait-validated` は従来どおり `KSR_POLL_TIMEOUT_SECONDS:-1800` (`:239`) で、両者は独立 |
| spec の契約 (release は NuGet push の後) との整合 | 待ちの位置が「NuGet push → release 要求 → 公開待ち」になったことで、再実行経路も初回経路と同じ順序に揃った。spec の「release は nuget.org への push の後に」と矛盾しない |

排他性も見た。`release_needed` と `pending_published` は同じ `case` の別の枝でしか立たず、`upload` 側の枝は `pending_published` を空のままにするので、`ids` に同じ ID が二重に並ぶ経路は無い。

### [🟡 Minor] 複数 ID 自己テストの識別力 → **解消**

観測点が 2 件目の照会 (`scripts/release/central-portal.sh:575-576`) に移り、コメントも「直列なら 2 件目も aaa になる / 照会の総数と最後の ID は直列実装でも一致するため件数や末尾では分けられない」という**実際に分かれる根拠**に書き直されている。

手元で `cmd_wait_published` を差し替えた写しを 3 種作って A/B した (実装ファイルには触らず scratchpad の複製で実行):

| 誤実装 | 結果 |
|---|---|
| 直列版 (`for id; do while :; done; done`) | **NG 1 件 = `1 ループで全枠を照会する`**。他の 6 件は OK。前回「分かれない」と指摘した `公開済みの枠は次のループで照会しない` / `残った枠だけを照会し続ける` は依然 OK のままだが、新しい観測点が独立して割っている |
| 上限を検証待ちと同じ変数から取る版 | 新設の `検証待ちの上限は公開待ちに効かない` (`:561-562`) が割る (evidence 4 節と一致)。実装だけ差し替えた私の写しでは、その手前の `上限を過ぎれば失敗する` で先に落ちた (無限ループ。Suggestion 1 参照) |
| 公開済みを外さず毎ループ全件を照会する版 | 台本を使い切って終わらず、判定行が出ない (evidence 3 節の自認と一致。Suggestion 1 参照) |

### [🟡 Minor] job timeout の根拠コメント → **解消**

`.github/workflows/release.yml:548-555` は実測ベースの予算 (本体 10 分 + KMP 再ビルド 6 分 + 公開待ちの上限 90 分 = 106 分 ≤ 150) として書き直され、最悪ケースが収まらないことと、その場合に何が起きるか (job timeout で止めて再実行。deployment ID は upload 直後に artifact 保存済み) が明記された。数え上げと値の矛盾は無くなっている。再実行時の待ちも同じ 1 本になったので、列挙から漏れていた 30 分 × 2 枠も消えた。

---

## 回帰の確認

| 経路 | 確認結果 |
|---|---|
| dry-run | publish job は job レベルの `if: ${{ !inputs['dry-run'] }}` で丸ごと skip。`wait-published` の呼び出しは publish job 内の 1 箇所だけ (grep 済み) なので dry-run では走らない |
| 両枠 PUBLISHED 済みの再実行 | 2 枠とも `skip-all` → `release-needed` も `pending-published-id` も空 → release step も待ち step も skip。`Push monorepo tag` は `publish-needed` で継続 |
| 片方だけ release が要る再実行 | `Release Maven Central deployments` (`:1194-1216`) が該当枠だけ release し、`released` にその ID だけを積む。待ちも 1 枠分 |
| 両枠が引き継ぎ (PUBLISHING) の再実行 | release step は skip、待ち step だけが 2 枠を 1 本で待つ (上限 90 分)。前回指摘の「30 分 × 2 の直列」は消えた |
| release step が Android だけ受理して KMP で失敗 | `set -euo pipefail` により output を書く前に落ち、待ち step は skip。失敗経路で Android (PUBLISHING) は drop されず ID が残り、KMP (VALIDATED) は drop されて ID が消える。次の attempt が Android を引き継ぎ枠・KMP を再 upload として処理する形で整合する |
| 失敗経路 (drop / ID 保存) との相互作用 | 引き継ぎ枠は `deployment_id` を保持したまま `:979` の `printf` で ID ファイルへ書き戻すので、`Upload deployment ids (android/kmp)` の `deployment-id != ''` も従来どおり満たす。`Drop pending deployments` / `Store deployment ids after cleanup` の条件 (`failure() && deployment-ids.ready == 'true'`) は変更されていない |
| bash 3.2 互換 | `${remaining[@]}` の展開は空配列にならない経路 (件数 0 なら手前で `return 0`) にあり、`pending` は常に 1 件以上。実行でも `/bin/bash` 3.2.57 で全件 OK |

---

## 指摘事項

### [🟡 Minor] 再実行時の「PUBLISHING なら PUBLISHED を待つ」の位置が変わったことが、どの成果物にも記録されていない

**該当箇所**: `.github/workflows/release.yml:918-921` / `:1073-1076`、`kasane/changes/fix-release-published-wait/exploration.md` (deviation.md は未作成)

**問題点**:

exploration の選択肢 B は「**再実行の分岐 (枠ごとの状態照会) は変わらない**。step 2 つの組み替えで済む」と書かれており、spec「Maven Central の 2 枠の deployment」も「PUBLISHING なら PUBLISHED になるまで待って release を skip」を upload 前の分岐として並べている。今回の実装は状態照会そのものは変えていないが、**その分岐が行う待ちの位置を NuGet push より後ろへ移した**ので、記録された合意スコープの字面からは一段はみ出している。

挙動としての帰結が 1 つある。再実行で「一方が PUBLISHING・他方が再 upload」になる組み合わせ (初回 attempt が Android を release した後に失敗し、後始末で VALIDATED の KMP が drop された場合に到達する) では、**Android の PUBLISHED を確認する前に KMP の release という取り消せない操作が走る**。旧実装ではこの確認が先だった。ただし初回経路は「2 枠を release してからまとめて待つ」形をオーナーが決定済みで、今回の変更は再実行経路をそれに揃えただけなので、露出そのものは合意済みの設計と同じ性質である。lessons `process` L-002 の趣旨 (主張の範囲を実証した範囲に限る) からも、この差は「変わらない」と書いたまま流さないほうがよい。

**推奨修正**: `deviation.md` に 1 項目として残す — ①exploration が「変わらない」とした再実行の分岐について、待ちの位置だけを後段へ移したこと ②spec の当該文は「PUBLISHING の枠は release を送り直さず公開の完了を待つ」という**動作の指定**として読み、待つ時点は 2 枠の release 要求の後であること ③上の組み合わせでの順序の帰結。exploration が「待ちの並行化は蒸留時に concepts 側で記述する」としているので、蒸留がこの 3 点も拾える形にしておけば足りる。足場 (exploration.md) 側は書き換えない。

---

### [🔵 Suggestion] 自己テストの逆対照のうち 2 種が「NG」ではなく「終わらない」で表れる

**該当箇所**: `scripts/release/central-portal.sh:565-582`、`kasane/changes/fix-release-published-wait/evidence/wait-published-multi-id-selftests.txt` 3 節

**問題点**: 「公開済みを外さず毎ループ全件を照会する」実装で自己テストを回すと、台本が尽きた後の `http_request` が失敗を返し、その戻りが状態不明として `remaining` に積み直されるため、`KSR_POLL_INTERVAL_SECONDS=0` の下で無限ループになる。evidence 3 節はこれを自認しているが、結果として `公開済みの枠は次のループで照会しない` (`:579-580`) と `残った枠だけを照会し続ける` (`:581-582`) は**その誤実装に対して判定行を出さない** (CI に載せた場合は赤ではなく hang になる)。手元でも 20 秒で打ち切って再現した。

**推奨修正**: 必須ではない。揃えるなら台本を 1 件伸ばす (`PUBLISHING, PUBLISHING, PUBLISHED, PUBLISHING, PUBLISHED, PUBLISHED`) だけでよい。正しい実装は 6 件目を消費しないので `残った枠だけを照会し続ける` の期待値 5 はそのまま使え、毎ループ全件を照会する実装は 3 ループ目で終了して照会 6 件・5 件目が `aaa` になり、2 つの検査が NG として出る。

---

### [🔵 Suggestion] (前回から継続・未対応) `scripts/release/**` の自己テストが CI で回らない

**該当箇所**: `.github/workflows/ci.yml` の lint job

**問題点**: 前回 Suggestion のまま。今回追加した複数 ID の分岐と上限の分離は、退行を見張るのが自己テストだけで、その自己テストが CI に載っていない。本 change が作った問題ではないので判定には影響させない。

**推奨修正**: 別 change として起票するか見送るかのオーナー判断。足すなら lint job に `for f in scripts/release/*.sh; do bash "$f" --selftest; done` の 1 step (ネットワークへ出ない)。上の Suggestion の台本延長を先に入れておくと、誤実装が hang ではなく赤で出る。

---

### [🔵 Suggestion] (前回から継続・未対応) 複数 ID の待ちで FAILED を見つけたとき、他の枠の状態が記録に残らない

**該当箇所**: `scripts/release/central-portal.sh:294-300`

**問題点**: 前回 Suggestion のまま。`for` の中で `fail` するため、FAILED を見つけた時点で残りの枠は照会されない。`release-procedure.md` に Portal 一覧を見る導線 (今回「表示名では見分けられない・区別は Summarize の deployment ID」の 1 文が加わって、むしろ辿りやすくなった) があるので、現状のままでも運用は成立する。

---

## アクションプラン

1. **Minor 1** — `deviation.md` に再実行経路の待ちの位置の変更 (と順序の帰結) を 1 項目として記録する。蒸留で concepts へ渡す材料になる
2. Suggestion 3 件 — 自己テストの台本延長 (1 行) は本 change に同梱してもよい大きさ。CI への自己テスト追加と FAILED 時の全枠状態出力は別 change / 見送りのオーナー判断
