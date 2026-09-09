# レビュー結果: fix-android-instrumented-toast-ime-hide-flake (002 回目)

**日付**: 2026-09-09
**判定**: APPROVED

## サマリー

前回の指摘 8 件 (Major 1 / Minor 3 / Suggestion 4) はすべて取り込まれており、実物で 1 件ずつ解消を確認した。Major (作業文書パスの参照) は参照句が消えてヘッダが自己完結しており、`runs` の値域は `case` のパターンだけで確定して 14 種の入力で全数確認したところ想定どおりに通す / 弾く。数値順ソートも 16 run の名前で確認した。観測ディレクトリを `$GITHUB_ENV` 経由で渡す形も、emulator-runner v2.38.0 の実装 (`exec.exec('sh', ['-c', script], { env: { ...process.env, … } })`) と `env` コンテキストの両方に届くことを確認した。
スコープも決定事項どおりで、触っているのは workflow 2 本だけ (テストの硬化・本体には手が入っていない)。Critical / Major はない。
残る指摘は Minor 1 件 — logcat の生存確認に入れた「3 秒後に非空」の条件が、端末が静かだった回に偽陽性を出しうる形で、しかもこれは**一時 workflow ではなく共有の検証 CI 側**に入っている (develop への push で毎回走る) ため、成立しない回は通常の CI が赤くなる。落ち方は速くメッセージも明快なので回す前提を壊すほどではないが、判定条件は硬くできる。

## 照合した規約

- `handbook/cross/comment-policy.md` (always) — ソースコメント規約。禁止参照 (作業文書パス・裸の変更識別子・ローカル通番) を両ファイルで再走査し 0 件
- `handbook/cross/verification-ci.md` (きっかけ: `.github/workflows/` を変えるとき・CI の失敗の切り分け) — android-instrumented job の保証 (件数検査を成否判定に含める) が維持されているかを節ごとに照合
- `handbook/cross/ci-script-deletion.md` (きっかけ: `.github/workflows/**` のスクリプトを作る・レビューするとき) — 本変更に削除操作はなく、抵触なし
- `handbook/cross/runtime-behavior-verification.md` (きっかけ: IME / OS 提示機構が絡む不具合の調査) — 本変更は観測の設置のみで完了判定を主張していないため、判定手順との衝突なし
- `lessons/process.md` (L-001 / L-002) — 姉妹面の照合対象なし。「互換」「破壊的変更なし」の主張は成果物に無い (既定呼び出しの不変は下表の実測として書く)
- `lessons/code-review.md` は不在 (重点観点・指摘しないことの指定なし)
- `kasane/config.yaml` の `skills.code-review` / `domain-skills.cross` は空 (ドメイン実装スキルの適用なし)

## 前回指摘の解消確認

| 前回の指摘 | 対応 | 確認 |
|---|---|---|
| 🟠 作業文書パスの参照 | ヘッダから参照句を削除 | `kasane/` `changes/` `exploration` `review-NNN` 等を両ファイルで grep して 0 件。残るヘッダは目的・削除条件・probe を持ち込まない理由で自己完結 |
| 🟡 logcat の空振り | `sleep 3` → `kill -0` + 非空判定 → `::error::` で exit 1 (gradle の前) | 位置は gradle 実行の前で正しい。`kill -0` は**有効** — bash は背景の子を SIGCHLD で回収するため、即座に落ちた子は 3 秒後に `kill -0` が失敗する (手元で模擬して実測)。ただし非空判定側に偽陽性が残る (後述 Minor) |
| 🟡 `runs` の値域 | `case "[1-9]｜1[0-6]"` だけで確定、数値比較を撤去 | `0 / 1 / 8 / 16 / 17 / 100 / 空 / 08 / " 8" / 20 桁 / "1;ls" / -1 / "1 6" / abc` の 14 種を実行し、通るのは 1・8・16 のみ。20 桁の入力も `seq` に到達せず弾かれる (前回の素通り経路は閉じた) |
| 🟡 dumpsys のコメント | 「suite 全件が終わった後の最終状態であり、失敗の瞬間ではない」「失敗の瞬間は logcat の時系列から読む」を明記 | 実態と一致。読む順序 (logcat が主・dumpsys が補助) も書かれている |
| 🔵 `-b all` | `-G` / `-c` / 採取の 3 か所を `-b all` に変更、コメントも events バッファに言及する形へ | 3 か所とも揃っている。ただし `-G` の効き方に注意点あり (後述 Suggestion) |
| 🔵 観測ディレクトリの一元化 | 準備 step で `$GITHUB_ENV` に 1 回定義、以降は `env.KS_OBSERVATION_DIR` | 値がスクリプトと upload の**両方**に届くことを確認 (下表)。重複定義は解消 |
| 🔵 system image の grep | 座標 grep を止めて `system-images;` 行を全部残す形へ | `api-level` / `target` / `arch` を変えても静かに空にならない |
| 🔵 summary の並び | 括弧内の数字で数値順ソート、取り出せない名前は末尾 | 16 run 分の名前で実行し 1→16 の順を確認 |

## 実行した検査

change に本体・テストのソース変更は無く (`git status` は workflow 2 本のみ)、ビルド / テスト結果は本変更で変わらない。代わりに以下を実行した。

| 検査 | 結果 |
|---|---|
| `actionlint` (対象 2 本 + ci.yml) | 本変更の 2 本は指摘 0。ci.yml の SC2001 (style) は本変更の外の既存分 |
| YAML パース (3 本) | OK。job 構成は `verify` / `plan`+`repeat`+`summary` |
| heredoc 本体を抽出して `bash -n` / `shellcheck -s bash` (3 本) | 指摘 0。YAML ブロックスカラーの字下げ除去後も `SCRIPT` / `PY` の終端は行頭に来る |
| summary の Python を抽出して `py_compile` + 実データ投入 | OK。job 名に matrix 値が付く形 (`repeat (N) / verify`) で 16 件を 1→16 に整列。付かない形 (`repeat / verify` が N 件) でも件数の集計は正しく、表が同名で並ぶだけに縮退する |
| `runs` の値域を 14 種の入力で全数実行 | 上表のとおり 1・8・16 のみ通過 |
| `kill -0` の有効性を模擬実行 | 即終了した背景の子は bash が回収するため 3 秒後の `kill -0` が失敗し、死亡を検出できる。一方 `-s` は adb のエラー行 (`2>&1` で同じファイルへ入る) で非空になるため単独では死亡を検出しない — 2 条件の `||` で補い合う設計になっている |
| emulator-runner v2.38.0 の実装確認 (`src/main.ts` を SHA 指定で取得) | `for (const script of scripts) await exec.exec('sh', ['-c', script], { env: { ...process.env, EMULATOR_PORT, ANDROID_SERIAL } })`。**(a)** 行ごとに別プロセスというコメントは正しい **(b)** `$GITHUB_ENV` で入れた `KS_OBSERVATION_DIR` は `process.env` 経由でスクリプトに届く **(c)** `ANDROID_SERIAL` が入るので `adb` は Emulator を指す |
| `env` コンテキストの到達 (upload の `path`) | step の `with:` で `env` コンテキストは参照可能で、直前の step が `$GITHUB_ENV` に書いた値を含む。スクリプト側は `set -u` 下の `${KS_OBSERVATION_DIR}` なので、届かなければ unbound variable で即座に赤くなる (静かに別の場所へ書く経路は無い) |
| `actions/upload-artifact` v7.0.1 の `path` 仕様 | `required: true`。値が空だと step 自体が失敗する (準備 step より前で落ちた回の挙動 — 後述 Suggestion) |
| action の SHA (3 本) | 前回照合と同一のまま、`upload-artifact` の追加分も同じ `043fb46…` (v7.0.1) を使用 |
| 既定呼び出し (ci.yml → `with:` なし) | ci.yml:141 は `uses:` のみで `with:` を持たない。`artifact-suffix` は `required: false` / `default: ''` で成立し、成果物名・件数検査・成否判定は従来どおり |
| 決定事項との照合 | (1) 観測 (logcat / 失敗時 dumpsys / 結果 XML / 環境の素性) ✔ (2) `workflow_dispatch` の反復 workflow (matrix・既定 8・上限 16・削除条件明記) ✔ テストの硬化と本体は未着手 ✔。`-v time` → `-b all -v threadtime` は決定の趣旨 (ImeTracker を読む) を満たす強化で、逸脱ではない |
| `comment-policy-lint.py --advisory` / `local-path-lint.py` / `identity-lint.py` | いずれも exit 0 (禁止 0 件)。`.yml` は comment-policy の検査対象外のままなので、規約判定は本レビューで行った |

## 指摘事項

### [🟡 Minor] logcat 生存確認の「3 秒後に非空」が偽陽性を出しうる (共有の検証 CI が赤くなる)

**該当箇所**: `.github/workflows/verify-android-instrumented.yml:151-156`

**問題点**: 判定は `! kill -0 "$logcat_pid" || [ ! -s "$observation_dir/logcat.txt" ]` の 2 条件で、後者が「3 秒以内に 1 バイトも書かれていなければ失敗」を意味する。直前に `adb logcat -b all -c` でバッファを空にしているので、ファイルに最初のバイトが乗るのは**その後に新しいログが 1 行出て、かつ端末側 logcat がパイプへ吐いた**時点であり、3 秒以内に必ず起きることは何にも保証されていない (端末が静かな窓に当たる・端末側の出力が塊で溜まる)。成立しなかった回は `::error::` + exit 1 で step が落ち、この判定は一時 workflow ではなく**共有の再利用 workflow** に入っているため、develop への push で回る通常の CI がそのまま赤くなる。メッセージ (「adb が落ちたか、行が流れていない」) も 2 つの原因を畳んでいるので、当たった人は adb の障害を疑って時間を使う。
なお死亡の検出は `kill -0` 側で足りている (実測。bash が背景の子を回収するため 3 秒後には ESRCH になる) ので、非空判定は「採取経路が端まで通っているか」を見るための追加条件という位置づけになる。

**推奨修正**: 端末側に印を打ってからそれが届くのを待つ形にすると、偽陽性が消えて経路の確認も強くなる。logcat を起こした直後に `adb shell log -p i -t <専用タグ> <印>` を 1 行入れ、`logcat.txt` にその印が現れるまで短い間隔で最大 10〜15 秒ポーリングする (現れたら続行、時間切れなら `::error::`)。固定の `sleep 3` と `-s` を、印の到達待ちに置き換える形。あわせて失敗メッセージを「adb が落ちた」と「印が届かない」で書き分けると、当たった回の切り分けが要らなくなる。

### [🔵 Suggestion] `-G 64M` はバッファごとに掛かり、実際のリング容量が成果物に残らない

**該当箇所**: `.github/workflows/verify-android-instrumented.yml:136`

**問題点**: `-G` は選択されたバッファそれぞれに掛かるため、`-b all -G 64M` は全バッファに 64M を要求する形になる。狙い (main / system / events で失敗の直前を残す) に対して要求が広く、Emulator の RAM を握る logd 側の footprint が読めない。今回の対象は**タイミング依存の間欠失敗**なので、観測の設置自体が端末のメモリ圧を変えると、測っている現象を動かしうる。
もう一つ実利のある問題として、採取後の成果物から「ログが押し出されたかどうか」を判定する材料が無い。`-G` が拒否された回 (`|| echo` で握り潰される) と、通ったが容量が足りずに巻き戻った回を、後から区別できない。

**推奨修正**: 要求の範囲を狙いに合わせて絞る (`-b main,system,events -G 64M` にする、または `-b all` のまま容量を落とす)。あわせて `-G` の直後に `adb logcat -b all -g` を実行して `environment.txt` に追記すると、その回の実際のリング容量が成果物に残り、後から「失敗の窓が保持されていたか」を判定できる。

### [🔵 Suggestion] 準備 step より前で落ちた回に upload が別の理由で失敗する

**該当箇所**: `.github/workflows/verify-android-instrumented.yml:190-199`

**問題点**: `path: ${{ env.KS_OBSERVATION_DIR }}` は準備 step (`:91`) が走って初めて値を持つ。checkout / JDK / cache / KVM / SDK 位置のいずれかで落ちた回でも `if: always()` により upload が走り、`path` が空のまま `required: true` の入力に渡って step 自体が「Input required and not supplied: path」で失敗する。job は既に赤いので実害は小さいが、本来の失敗と無関係な赤い step が 1 つ増え、原因を読む人の目を逸らす。

**推奨修正**: 条件を `if: always() && env.KS_OBSERVATION_DIR != ''` にする (結果 XML 側の upload は既に固定パスなのでそのままでよい)。

### [🔵 Suggestion] 集計が cancelled / skipped も「失敗」に数える

**該当箇所**: `.github/workflows/repeat-android-instrumented.yml:135`

**問題点**: `conclusion not in ("success", None)` なので、`cancelled` (run の手動キャンセル・concurrency での打ち切り) や `skipped` が失敗として数えられる。この workflow の産物は「N 回中 M 回が失敗」という**頻度の実測値**であり、探索の判断 (16 run 回して出なければ引き上げる) がその数字に乗るため、キャンセルが混ざった回の数字をそのまま引用すると頻度を過大に見積もる。表には結論が出るので読めば分かるが、見出しの数字が独り歩きしやすい。

**推奨修正**: `failure` / `timed_out` を失敗として数え、`cancelled` / `skipped` は別枠 (「うち未完了 K 回」) で出す。

### [🔵 Suggestion] 恒久ファイル側に調査中の状況説明が残る

**該当箇所**: `.github/workflows/verify-android-instrumented.yml:11-14`

**問題点**: 一時 workflow は決着時に削除されるが、`verify-android-instrumented.yml` は残る。そのヘッダに「手元 (arm64) では自然再現も強制再現も得られておらず、失敗した回の ImeTracker / InsetsController の時系列が唯一の判別材料になる」という**調査中の状況**が書かれており、決着後は現在形として成立しない記述になる (コメント規約が禁じる履歴記述に寄っていく)。現時点では観測を常設している理由の説明として機能しているので違反ではない。

**推奨修正**: 決着 (署名の取得または引き上げ) の時点で、恒久的に残す文面 — なぜ instrumented job だけ logcat を常設するのか — に書き直す。この 1 行を申し送りに入れておくと、蒸留の時に取り残されない。

### [🔵 Suggestion] 公開成果物の確認事項 (前回分の更新)

**該当箇所**: `.github/workflows/verify-android-instrumented.yml:145`

**問題点**: 指摘ではなく確認事項の更新。前回は既定バッファ (`main,system,crash`) 前提で「個体情報・個人情報・秘密は含まれない」と判定したが、`-b all` により `radio` / `events` / `kernel` まで採取範囲が広がった。Emulator の `radio` に出る加入者識別子の類は system image の合成値、`events` はアプリの遷移イベント、`kernel` は Emulator カーネルの dmesg で、いずれも公開リポジトリの成果物として問題になる値を持たない。判定は前回と同じ (公開されて問題なし)。

**推奨修正**: 採取内容は変更不要。運用として (1) 採取対象を実機や手元環境へ広げるときは同じ判断を取り直す、(2) この logcat を `evidence/` へ抜粋するときは生ログのまま貼らず `scripts/log-sanitize.py` を通す、を守る。

## アクションプラン

1. **Minor (生存確認の偽陽性)** — 印の到達待ちへ置き換える。共有の検証 CI に入る hard fail なので、反復を回す前に直すのが安い
2. **試走を 1 回挟む** — `-b all` の採取と生存確認が CI のイメージで成立するかは、走らせるまで確かめられない。commit 後は `runs: 1` で 1 回だけ dispatch して、成果物の `logcat.txt` に `ImeTracker` / `InsetsController` の行が実際に入っていることを見てから 8〜16 run へ進む (署名の取れない採取で 16 run を使い切らないため)
3. **Suggestion (`-G` の範囲と `-g` の記録)** — 試走の前に入れておくと、1 回目の成果物で容量の妥当性まで判定できる
4. 残る Suggestion 3 件 (upload の条件・集計の内訳・恒久ヘッダの文面) は反復の結論に影響しない。恒久ヘッダの文面だけは決着時の申し送りに残す
