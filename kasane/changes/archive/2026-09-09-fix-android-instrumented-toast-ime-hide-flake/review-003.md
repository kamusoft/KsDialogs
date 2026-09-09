# レビュー結果: fix-android-instrumented-toast-ime-hide-flake (003 回目)

**日付**: 2026-09-09
**判定**: APPROVED

## サマリー

前回 (002) の Minor 1 件と Suggestion 3 件 (見送り 2 件は申し送り扱い) はすべて取り込まれており、実物で 1 件ずつ解消を確認した。焦点だった生存確認は固定 `sleep` + 非空判定から「端末に印を打って届くのを待つ」形に置き換わり、偽陽性の経路が消えている。`adb shell log -p i -t <タグ>` は toybox `log` の受理形 (既定の書き込み先は main バッファ) で、`-G` を `main,system,events` に絞っても採取は `-b all` のままなので印は拾える。ポーリングは `set -euo pipefail` 配下でも `grep -q` の非ゼロで落ちないことを 3 モードの模擬実行で実測した。
スコープは決定事項どおりで、触っているのは workflow 2 本だけ (`git status` で本体・テストに変更なし)。件数検査 step は無改変のまま `if: always()` で残り、検証 CI の保証 (件数を成否判定に含める) は維持されている。Critical / Major / Minor なし。残るのは優先度の低い Suggestion 3 件で、いずれも反復の結論に影響しない。

## 照合した規約

- `handbook/cross/comment-policy.md` (always) — ソースコメント規約。`.yml` は機械検査の対象外のため、禁止参照 (作業文書パス・裸の変更識別子・ローカル通番・デルタスペック構文キーワード・履歴記述) を両ファイルで手で再走査し 0 件
- `handbook/cross/verification-ci.md` (きっかけ: `.github/workflows/` を変えるとき) — android-instrumented job の保証 (Emulator 1 台・API 36 固定・件数検査を成否判定に含める) を節ごとに照合。追加 step はすべて件数検査 step の前に挿入され、いずれも `if: always()` なので検査は従来どおり走る
- `handbook/cross/ci-script-deletion.md` (きっかけ: `.github/workflows/**` のスクリプトを作る・レビューするとき) — 本変更に削除操作なし。抵触なし
- `handbook/cross/runtime-behavior-verification.md` (きっかけ: IME / OS 提示機構が絡む不具合の調査) — 本変更は観測の設置のみで、完了判定を主張していないため衝突なし
- `lessons/spec-review.md` (L-001〜L-003) — spec-review scope。本変更に spec / design はなく、翻案元の規範を持ち込む記述もない
- `lessons/code-review.md` は不在 (重点観点・指摘しないことの指定なし)
- `kasane/config.yaml` の `skills.code-review` / `domain-skills` に cross ドメインの割り当てなし

## 前回指摘の解消確認

| 前回の指摘 | 対応 | 確認 |
|---|---|---|
| 🟡 生存確認の偽陽性 (共有 CI が赤くなる) | 固定 `sleep 3` + 非空判定を撤去し、専用タグの印を打って最大 15 秒ポーリング (`verify-android-instrumented.yml:155-180`) | 偽陽性の源だった「一定時間で非空」は消えた。`adb shell log -p i -t TAG MESSAGE` は toybox `log` の受理形 (`-p` は先頭一致で `i` = INFO、既定の書き込み先は main)。`logcat` は既定でリング先頭から読むので、印が採取開始より先に出ても取りこぼさない。書き分けも 2 系統 (`kill -0` が落ちた → 「adb の障害」/ 時間切れ → 「印が届かない」) に分かれている |
| 🔵 `-G` の範囲と実容量の記録 | `-b main,system,events -G 64M` に絞り、直後に `adb logcat -b all -g` を `environment.txt` へ追記 (`:139-143`) | 拡大要求と採取範囲の齟齬なし — 採取は `-b all` のままで、狙いの 3 バッファだけ拡大し、実際に確保された容量は全バッファ分が素性に残る (拒否された回と巻き戻った回を後から区別できる)。順序も 拡大 → `-g` 記録 → `-c` 消去 → 採取開始 で正しい |
| 🔵 upload の空 path | `if: always() && env.KS_OBSERVATION_DIR != ''` (`:218`) | 準備 step より前で落ちた回は `env.KS_OBSERVATION_DIR` が null。GitHub の式は型不一致を数値に寄せるため null と `''` はともに 0 に落ち、`!=` は false → step は skip される (期待どおり)。`env` コンテキストは step の `if` で参照可能で、actionlint も通る |
| 🔵 集計の分母 | `failure` / `timed_out` だけを失敗に数え、それ以外を `unfinished` として別枠表示 (`repeat-android-instrumented.yml:136-138`) | 混在データ (success / failure / cancelled / skipped / timed_out / None) を投入して実行し、失敗 2・未完了 3 に正しく分かれることを確認。`None` (進行中) を `unfinished` に含めるのは防御としては妥当 — `summary` は `needs: repeat` なので実際には出ないが、出た場合に失敗へ数えないほうが安全 |
| (見送り) 恒久ヘッダの調査中記述 | 未対応 (決着時に書き直す申し送り) | `verify-android-instrumented.yml:11-14` は現状のまま。観測を常設している理由の説明として現時点では機能しており、規約違反ではない |
| (見送り) 公開成果物の確認事項 | 採取内容は変更なし | `-b all` の採取範囲は前回判定のまま。今回 `-G` を絞ったのは容量だけで、成果物に載る種類は変わらない |

## 実行した検査

本変更にソースの変更はなく (`git status` は workflow 2 本のみ)、ビルド / テスト結果は変わらない。代わりに以下を実行した。

| 検査 | 結果 |
|---|---|
| `actionlint` (対象 2 本) | 指摘 0 (exit 0) |
| YAML パース + heredoc 抽出 → `bash -n` / `shellcheck -s bash` (準備 step の外側・`SCRIPT` 本体) | いずれも指摘 0。ブロックスカラーの字下げ除去後も `SCRIPT` 終端が行頭に来る |
| 印の到達待ちループを 3 モードで模擬実行 (正常 / 採取プロセス即死 / 印が来ない) | 正常は 2 秒で `PROCEED`、即死は「adb の障害」で exit 1、無音は 15 秒で「印が届かない」で exit 1。`set -euo pipefail` 下でも `grep -q` の非ゼロ・`kill -0` の非ゼロは `if` 条件のため落ちない。`SECONDS` は `bash` 起動 (`script: bash …`) なので `set -u` でも未定義にならない |
| summary の Python を抽出して混在データで実行 | 上表のとおり。数値順ソート (`repeat (N) / verify`) も維持 |
| コメント規約の手動走査 (禁止参照・履歴記述・仕様構文キーワード) | 両ファイルで 0 件。ローカル絶対パス (`/Users/` `/Volumes/` `/home/…`) も 0 件 |
| `local-path-lint.py` / `identity-lint.py` / `comment-policy-lint.py --advisory` | exit 0 / exit 0 / 禁止 0 件 (要確認 450 件はすべて本変更の外の既存分) |
| 既定呼び出しの不変 (`ci.yml:141`) | `uses:` のみで `with:` なし。`artifact-suffix` は `required: false` / `default: ''` で成立し、成果物名・件数検査・成否判定は従来どおり |
| concurrency group の衝突 | `ci-…` (cancel-in-progress: true) と `repeat-android-instrumented-…` (false) で別群。反復が通常の検証 CI を打ち切らない |
| 件数検査 step の無改変 | `Verify executed test count` は diff に含まれず `if: always()` のまま。追加 step はすべてその前に入るため、検証 CI の保証は維持 |
| 決定事項との照合 | (1) 観測 (logcat / 失敗時 dumpsys / 結果 XML / 環境の素性) ✔ (2) `workflow_dispatch` の反復 workflow (matrix・既定 8・上限 16・削除条件明記・probe 用の一時テストを持ち込まない) ✔ (3) 本体 (ToastContainer) 無改変 ✔ |

## 指摘事項

### [🔵 Suggestion] 印を打てなかった回が「採取経路が通っていない」に畳まれる

**該当箇所**: `.github/workflows/verify-android-instrumented.yml:161`

**問題点**: `adb shell log …  || true` で送出の失敗を握り潰しているため、`log` の実行自体が失敗した回 (コマンド不在・`adb shell` の一時失敗) も、時間切れ側のメッセージ「端末に打った印が 15 秒以内に logcat の採取ファイルへ届かない (採取経路が通っていない)」で報告される。前回の指摘は原因を書き分けることが趣旨だったので、3 つ目の原因が 2 つ目に混ざる形が残っている。`adb shell` は shell protocol v2 で端末側コマンドの終了コードを返すため、拾えば区別できる。

**推奨修正**: `|| true` を、送出に失敗したことを記録する形 (`|| probe_sent=0` 等) にし、時間切れ時のメッセージを「印を打てなかった」と「印が届かない」で分ける。落ち方は変わらないので優先度は低い。

### [🔵 Suggestion] 集計の見出しの分母に未完了が含まれたまま

**該当箇所**: `.github/workflows/repeat-android-instrumented.yml:138`

**問題点**: 見出しは `len(repeated)` を分母にしており、未完了・打ち切りの回も含む。読み手に「分母から除いて読む」と促してはいるが、この数字は頻度の実測値として後から引用されるもので、見出しだけが独り歩きすると失敗率を過小に見積もる (例: cancelled 3 回混在で「8 回中 2 回」= 25% に見えるが、実効は 5 回中 2 回 = 40%)。

**推奨修正**: 見出しを実効の分母 (`len(repeated) - len(unfinished)`) で出し、除いた回数を括弧で添える。表は現状のまま全回を出せばよい。

### [🔵 Suggestion] 集計の Python が落ちると run が赤くなる (コメントの意図と不一致)

**該当箇所**: `.github/workflows/repeat-android-instrumented.yml:117`

**問題点**: job のヘッダコメントは「集計自体は観測の付帯物なので、取得に失敗しても run を赤くしない」と述べており、`gh api` の失敗はそのとおり `exit 0` で吸収される。一方その後の `python3 - "$jobs_json"` は `set -e` 配下の素の実行なので、想定外の入力で例外が出れば step が落ちて summary job が赤くなる。実害は小さい (`--jq` の出力が壊れる筋は薄い) が、コメントが述べる保証と実際の挙動がずれている。

**推奨修正**: `python3 … || echo "集計に失敗した (job 一覧を見て判断する)" >> "$summary_file"` の形にするか、コメント側を「job 一覧の取得に失敗しても」と限定する。

## アクションプラン

1. Suggestion 3 件はいずれも反復の結論に影響しない。反復を回す前に入れるなら 1 件目 (印の書き分け) が最も安いが、当てなくてよい
2. 前回のアクションプラン 2 番 (試走 1 回) は未消化のはず — commit 後に `runs: 1` で dispatch し、`logcat.txt` に `ImeTracker` / `InsetsController` の行が入っていること、`environment.txt` の `-g` が拡大後の容量を示していることを見てから 8〜16 run へ進む
3. 申し送り (決着時): `verify-android-instrumented.yml:11-14` のヘッダを恒久の文面 (なぜ instrumented job だけ logcat を常設するのか) へ書き直す。一時 workflow の削除と同じタイミングで行う
4. 申し送り (運用): 採取を実機や手元環境へ広げるときは公開成果物としての判断を取り直す。logcat を `evidence/` へ抜粋するときは `scripts/log-sanitize.py` を通す
