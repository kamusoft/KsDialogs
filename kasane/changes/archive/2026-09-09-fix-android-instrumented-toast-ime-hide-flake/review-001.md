# レビュー結果: fix-android-instrumented-toast-ime-hide-flake (001 回目)

**日付**: 2026-09-09
**判定**: CHANGES_REQUESTED

## サマリー

exploration.md の決定 (C) のうち B (観測の設置 + 一時的な反復 workflow) が、合意されたスコープどおりに実装されている。既定呼び出しの挙動 (成否判定・件数検査) は変わらず、emulator-runner の `script` が 1 行ずつ別プロセスへ渡る制約への対処 (スクリプトファイル化) も action の実装と一致しており、終了コードの受け渡しと後始末は正しい。action の SHA 固定も 3 本すべて実物と一致した。
一方で、一時 workflow のヘッダコメントに作業文書 (`kasane/changes/**/exploration.md`) のパス参照があり、コメント規約 (`always`) の「禁止する参照」に新規で抵触している。`.yml` は comment-policy lint の検査対象外 (実測: 検査対象 0 ファイル) で機械検査が効かず、レビューが唯一の関門になる箇所のため Major とした。あわせて、観測が空振りしても気づけない (Minor)・`runs` の範囲検証に素通り経路がある (Minor) を指摘する。

## 照合した規約

- `handbook/cross/comment-policy.md` (always) — ソースコメント規約
- `handbook/cross/verification-ci.md` (きっかけ: `.github/workflows/` を変えるとき・CI の失敗の切り分け)
- `handbook/cross/ci-script-deletion.md` (きっかけ: `.github/workflows/**` のスクリプトを作る・レビューするとき) — 本変更に削除操作は無く、抵触なし
- `lessons/process.md` (L-001 / L-002) — 姉妹面の照合対象なし、互換の主張なし
- `lessons/code-review.md` は不在 (重点観点・指摘しないことの指定なし)
- `kasane/config.yaml` の `skills.code-review` / `domain-skills.cross` は空 (ドメイン実装スキルの適用なし)

## 実行した検査

change に本体・テストのソース変更は無く (`git status` は workflow 2 本のみ)、ビルド / テストの結果は本変更で変わらない。代わりに以下を実行した。

| 検査 | 結果 |
|---|---|
| `actionlint` (2 本) | 指摘 0 |
| YAML パース (`yaml.safe_load`、ci.yml 含む 3 本) | OK |
| heredoc 本体を抽出して `bash -n` / `shellcheck -s bash` | 指摘 0 (YAML のブロックスカラーの字下げ除去後もヒアドキュメント終端は行頭に来る) |
| `comment-policy-lint.py --advisory` / `local-path-lint.py` / `identity-lint.py` | いずれも exit 0 (ただし comment-policy は `.yml` を検査対象にしていない — 後述 M-1) |
| action の SHA 照合 (`gh api .../git/ref/tags/...`) | `actions/upload-artifact` v7.0.1 = `043fb46…`、`actions/checkout` v7.0.1 = `3d3c42e…`、`ReactiveCircus/android-emulator-runner` v2.38.0 (annotated tag を解決) = `a421e43…` — 3 本ともコメントの版と一致 |
| emulator-runner v2.38.0 の実装確認 (`src/main.ts` / `src/script-parser.ts`) | `parseScript` が改行で分割し、各行を `exec.exec('sh', ['-c', script], { env: {...process.env, …} })` で実行。**(a)** コメントの「行ごとに別々の `sh -c`」は正しい **(b)** 配列形式のため `bash "${RUNNER_TEMP}/…"` の引用符は壊れない **(c)** step の `env:` (`KS_OBSERVATION_DIR`) はスクリプトへ継承される **(d)** `working-directory` は `process.chdir` で効き `./gradlew` は `android/` 基準になる |
| 再利用 workflow の job 名の実測 (直近の ci.yml run の `/jobs`) | `android-instrumented / verify` の形。matrix 呼び出しでは `repeat (N) / verify` になるため `startswith("repeat ")` で拾える |
| `runs/{id}/attempts/{n}/jobs` + `--paginate --jq` の疎通 | 実リポジトリで実行し、summary が期待する 1 行 1 JSON が得られることを確認 |
| 既定呼び出し (ci.yml → `with:` なし) | `artifact-suffix` は `required: false` + `default: ''` で成立。成果物名・件数検査・成否判定は従来どおり |
| 反復時の成果物名の一意性 | `-${{ matrix.index }}` で run 内一意 |
| `concurrency` | `repeat-android-instrumented-<ref>` は ci.yml の `ci-CI-<ref>` と別 group、`cancel-in-progress: false`。検証 CI を打ち切らない |
| 起動条件 | `workflow_dispatch` のみ。`push` / `pull_request` は持たず、ci.yml からも呼ばれていない。既定ブランチが `develop` (= commit 先) なので手動起動は成立する |
| 削除条件の明記 | ヘッダに「署名が取れた時点、または 16 run 回しても失敗が出なかった時点で削除」と記載あり。`runs` の上限 16 とも整合 |

## 指摘事項

### [🟠 Major] 一時 workflow のコメントが作業文書のパスを参照している

**該当箇所**: `.github/workflows/repeat-android-instrumented.yml:10-12`

**問題点**: ヘッダに `判断の材料と経緯は kasane/changes/fix-android-instrumented-toast-ime-hide-flake/exploration.md が持つ。` とある。コメント規約 (`handbook/cross/comment-policy.md`、`always`) の「禁止する参照」は、**`kasane/` 配下の作業文書のパス**を明示的に挙げている。理由もそのまま当てはまり、この change は蒸留時に `kasane/changes/archive/YYYY-MM-DD-…/` へ移るため、書かれたパスはその時点で指す先を失う。
`comment-policy-lint.py` の既定拡張子 (`.swift .kt .kts .cs .java .xml .xaml .axml .gradle .pro`) に `.yml` は無く、`config.yaml` の `lint.comment-policy.ext` も空のため、この 2 ファイルは機械検査の対象外 (`--paths` 指定でも「検査対象 0 ファイル」)。規約本文は「リポジトリ内でコメント構文を持つ全ソースファイル」を対象とし、検査範囲は規約より狭いと明記しているので、判定はレビューが行う。

**推奨修正**: 参照句を削除する (規約の「書き換え時の判断基準」1. 定型句型)。目的・削除条件・probe を持ち込まない理由はすでにヘッダ内で自己完結しており、参照句を落としても文意は壊れない。経緯を残したい場合も、ADR ではない探索メモへのパス参照は取れないため、コメント内で自己完結する 1 文に書き直す。

### [🟡 Minor] logcat の採取が始まったことを確認していない (空振りに気づけない)

**該当箇所**: `.github/workflows/verify-android-instrumented.yml:138-139`

**問題点**: `adb logcat -v threadtime > … 2>&1 &` は背景に落としたきり、生きているかを確認していない。adb が即座に落ちた場合 (端末未接続・`ANDROID_SERIAL` の食い違い・adb server の再起動) でも `logcat.txt` にはエラー 1 行だけが入り、gradle はそのまま走って job は通常どおり成否を返す。この変更の目的は「失敗した回の時系列を取ること」であり、採取が空振りしていても分かるのは 8〜16 run を回し終えて成果物を開いた後になる。1 回あたり Emulator 1 台 × 最大 30 分を消費するので、空振りの検出は早いほど価値が高い。

**推奨修正**: logcat を起こした直後に短く待ってから `kill -0 "$logcat_pid"` とファイルサイズ (行が流れているか) を確認し、採れていなければその場で `::error::` を出して終了する。gradle を回す前に落とす形なら、無駄な 30 分を使わずに済む。

### [🟡 Minor] `runs` の範囲検証に素通り経路がある

**該当箇所**: `.github/workflows/repeat-android-instrumented.yml:51-64`

**問題点**: `case` で数字以外を弾いた後に `[ "${KS_RUNS}" -lt 1 ] || [ "${KS_RUNS}" -gt 16 ]` で範囲を見ているが、`intmax_t` に収まらない桁数の入力は数字だけで構成されるため `case` を通り、`[` は「integer expression expected」で **終了コード 2** を返す。`if` は真偽としてしか見ないので両方 2 → 偽 → 検証を通過し、`seq 1 <巨大値> | paste -sd, -` がコマンド置換の中で走り続ける。手元で実測 (`[ 99999999999999999999 -lt 1 ]` → status 2、続く `seq` が停止せず) して確認した。
`timeout-minutes: 5` があるので run は 5 分で赤くなり、matrix は生成されないため「ランナーを食い潰す」までは至らない。ただしコメントが述べる歯止めの意図 (手が滑った入力を弾く) は満たしておらず、`plan` job のメモリを食う経路が残る。三桁の入力 (`100` など) は正しく弾かれる。

**推奨修正**: 桁数を先に縛る (`case` を `''|*[!0-9]*|??? *` ではなく、`1` `2` … `16` の列挙、または `[0-9]|1[0-6]` 相当のパターン照合) か、`workflow_dispatch` の `type: choice` で選択肢を固定する。数値比較に到達する前に値域を確定させる形が確実。

### [🟡 Minor] 失敗時 dumpsys の採取時点をコメントが誤読させる

**該当箇所**: `.github/workflows/verify-android-instrumented.yml:144-148`

**問題点**: コメントは「失敗した回だけ、Emulator が生きているうちに IME とウィンドウの状態を残す」と書いているが、実際に採るのは `connectedDebugAndroidTest` が**全件を終えた後**である。目的の失敗 (`ToastSystemInputTests` の IME hide) は suite の途中で起き、その後も 200 件以上のテストが走って Activity は破棄・再生成されるため、`dumpsys input_method` / `dumpsys window` が写すのは「失敗した瞬間の状態」ではなく「suite 最後のテストが終わった後の状態」になる。採取そのものは exploration.md の決定どおりで逸脱ではないが、コメントを読んだ人が「失敗時のスナップショット」として解釈すると診断を誤る。

**推奨修正**: コメントを実態に合わせる (「suite 終了後の最終状態であり、失敗の瞬間の状態ではない。失敗の瞬間は logcat の時系列から読む」)。判別材料としての位置づけは logcat 側にある旨を 1 文添えると、成果物を開いた人が読む順序を誤らない。

### [🔵 Suggestion] 「フィルタは掛けない」と既定バッファの範囲がずれている

**該当箇所**: `.github/workflows/verify-android-instrumented.yml:134-138`

**問題点**: `adb logcat` は `-b` 無指定だと `main,system,crash` だけを読む。`events` / `kernel` / `radio` は落ちるので、「フィルタは掛けない」という記述より実際の採取範囲は狭い。`ImeTracker` / `InsetsController` / `WindowManager` は system / main に出るので当面の判別には足りるが、入力方式まわりのイベントログ (`events` バッファ) は今回の署名探しでも参照候補になる。

**推奨修正**: `-b all` を検討する (サイズは増える) か、既定バッファのみを読んでいることをコメントに明記して、期待される範囲を実態に合わせる。

### [🔵 Suggestion] 観測ディレクトリと system image 座標の値が複数箇所に散っている

**該当箇所**: `.github/workflows/verify-android-instrumented.yml:97,161,179` / 同 `123-125`

**問題点**: `${{ runner.temp }}/ime-observation` が 3 箇所 (+ スクリプト内の変数) に重複しており、片方だけ変えると upload が空になる。また環境素性の採取で `grep 'system-images;android-36;google_apis;x86_64'` と書いているが、これは同ファイルの `api-level` / `target` / `arch` の値を二重に持っている形で、将来 API レベルや arch を上げると grep だけ取り残されて「(sdkmanager から revision を取得できなかった)」に静かに倒れる。

**推奨修正**: 観測ディレクトリは job 単位の `env:` に 1 か所で定義して各所から参照する。system image は座標で grep せず `--list_installed` の出力から `system-images;` 行をすべて残す形にすると、設定変更に追随する。

### [🔵 Suggestion] 反復サマリの並びが辞書順になる

**該当箇所**: `.github/workflows/repeat-android-instrumented.yml:142`

**問題点**: `sorted(repeated, key=lambda r: r["name"])` は `repeat (1)` → `repeat (10)` → `repeat (11)` → `repeat (2)` の順に並ぶ。既定 8 run では顕在化しないが、上限の 16 run で回すときに読み違えやすい。

**推奨修正**: 名前から括弧内の数字を取り出して数値でソートする (取り出せない行は末尾に回す)。

### [🔵 Suggestion] 成果物は公開ダウンロード可能である点の確認

**該当箇所**: `.github/workflows/verify-android-instrumented.yml:171-187`

**問題点**: このリポジトリは既に public であり (`gh repo view` で確認)、run の成果物はリポジトリを閲覧できる誰でもダウンロードできる。今回採る内容 (Emulator の logcat・`dumpsys`・`getprop`・ランナーの SDK パス・emulator の版) を確認したかぎり個体情報・個人情報・秘密は含まれず、公開されても問題ない。指摘ではなく確認事項として記録する。

**推奨修正**: 現状の採取内容は変更不要。ただし (1) 採取対象を実機や手元環境へ広げるときは同じ判断を取り直すこと、(2) この logcat を `evidence/` へ抜粋するときは生ログのまま貼らず `scripts/log-sanitize.py` を通すこと、を運用として守る。

## アクションプラン

1. **M-1** — `repeat-android-instrumented.yml` のヘッダから作業文書パスの参照句を削除する (1 行。規約違反の解消)
2. **Minor (logcat 空振り検出)** — gradle を回す前に採取が生きているか確認して、駄目なら即失敗させる (8〜16 run を無駄にしない)
3. **Minor (`runs` の検証)** — 値域を数値比較の前に確定させる形へ変える
4. **Minor (dumpsys のコメント)** — 採取時点を実態に合わせて書き直す
5. Suggestion 4 件は反復を回す前に取り込めるものだけ取り込み、残りは一時 workflow の削除とともに消える前提で見送ってよい
