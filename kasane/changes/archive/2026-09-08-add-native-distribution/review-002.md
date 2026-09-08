# レビュー結果: add-native-distribution (002 回目)

**日付**: 2026-09-08
**判定**: APPROVED

## サマリー

前回のホスト側レビュー (Minor 3 / Suggestion 4) と相方レビューの採用分 (Major 1 / Minor 1) は、引き継ぎ事項として出した Suggestion 1 件 (本 change の Non-Goals) を除いてすべて解消している。とりわけ後始末の失敗検出は、削除コマンドの終了コードではなく削除後に照会した tag 一覧の実体で判定し、照会自体の終了コードを個別に検査する形へ組み替わっており、着手フラグを mutation より前に立てることで push の更新窓も塞がっている。ログの取りこぼし対策も process substitution から自己再実行 + パイプラインへ変わり、ネットワークに出ない fixture の実行でログ末尾 (後始末の記録と「結果:」行) が残ることを確認した。

新規の指摘は Critical / Major なし。前回の Minor を塞ぐために足した事前検証の `git fetch` が、後始末の同一性判定と順序で噛み合っておらず、**検証が成功した実行を偽の失敗として記録し得る** 経路が 1 つ残る (Minor、fixture で再現済み)。ただし配信リポジトリの作業コピーは現在 tag を 1 本も持たないため、目前の task 7 でこの経路に入る見込みは低い。ほかは Suggestion 1 件。

## 実行した検証 (レビュー側で再実行)

| 対象 | 結果 |
|---|---|
| `android/` `./gradlew test --rerun-tasks` | BUILD SUCCESSFUL / 68 tests・0 failures (handbook の基準値と一致)。`:ksdialogs-core:verifyNoDeclarativeUiDependency` がタスクグラフに乗って実行されたことをログで確認 |
| `scripts/spm-snapshot/sync-snapshot-test.sh` | 全判定 ok / 失敗 0 |
| `scripts/local-path-lint.py` / `identity-lint.py` / `comment-policy-lint.py` | いずれも違反 0 (comment-policy は 966 ファイル検査 / 禁止 0) |
| `scripts/spm-snapshot/verify-https-resolution.sh` | `bash -n` 通過。ネットワークに出ない fixture (一時 bare リポジトリを origin、`xcodebuild` はスタブ、origin URL 検査 1 行だけを差し替えたコピー) で全経路を実行 |
| 改名の内容同一性 | `android/ksdialogs-core/src/**` と `android/ksdialogs/src/**` の全追跡ファイルの blob を HEAD の `android/ksdialogs/**` / `android/ksdialogs-compose/**` と照合し、**全件が byte 同一** (改名に紛れたソース改変が無いことの確認) |
| 検証 CI の残存検査 | `.github/workflows/` に旧 module 名・旧パスの残存なし (`verify-android.yml` / `verify-android-instrumented.yml` / `verify-maui.yml` は module 名を含まない汎用タスクで参照しており追随不要) |
| lint job の新 step の CI 移植性 | `sync-snapshot-test.sh` は `git -c user.name=… -c user.email=…` で identity を明示し、ハッシュツールを `shasum` / `sha256sum` で分岐している。global git identity の無い `ubuntu-24.04` ランナーで落ちない |
| `git fetch` の tag 自動追従 | 一時リポジトリで再現確認 (下記 Minor の根拠) |

instrumented test・`ios/` の `xcodebuild test`・MAUI binding のビルド・samples 2 ルートの `assembleDebug`・`publishToMavenLocal` 系の検算は evidence/ と review-001 の実施済み結果で代替した。task 7.1〜7.3 (実リモートの push・検証 tag・後始末) は未実施として扱い、対応 Scenario の未検証は指摘対象にしていない。

## 照合した規約

| 文書 | 適用のきっかけ |
|---|---|
| `kasane/handbook/cross/comment-policy.md` | always (ビルドスクリプト・シェルのコメントを書き換えている / ADR 参照の書き換えが指摘の対象だった) |
| `kasane/handbook/cross/test-execution.md` | テストを実行し完了を判定する / 本文書自体が diff の対象 |
| `kasane/handbook/cross/verification-ci.md` | `.github/workflows/ci.yml` を変更している / 本文書自体が diff の対象 |
| `kasane/handbook/cross/local-development-setup.md` | Gradle ルートのビルドと Sample の参照方式 / 本文書自体が diff の対象 |
| `kasane/handbook/cross/diagnostic-message-language.md` | 検査対象パスの追随 (本文書自体が diff の対象) |
| `kasane/lessons/process.md` | L-001 / L-002 |

`sample-parity.md` は前回と同じ理由 (変更は依存宣言と置換設定だけでデモ項目・文言・色トークン・OS 操作への反応・撮影引数のいずれにも当たらない) で適用外と判定した。`kasane/lessons/code-review.md` は存在しないため、追加の「指摘しないこと」はない。

参照した決定: cross/ADR-0002 / 0004 / 0005 / 0006 / 0015 / 0016 / 0017 / 0018、core/ADR-0009、android/ADR-0001 (いずれも accepted)。cross/ADR-0008 / 0009 / 0019 は `proposed` のため判定根拠にしていない (所見も無し)。

適用したスキル: kotlin-impl-skill (Gradle Kotlin DSL)、github-workflow-skill (`ci.yml` の差分)、maui-native-binding-skill / csharp-impl-skill (binding csproj)、swift-ui-impl-skill (消費者コードの参照のみ)。Kotlin 言語層の指摘は無し。

## 前回指摘の解消状況

### ホスト側 `review-001.md`

| # | 指摘 | 状態 | 根拠 |
|---|---|---|---|
| 1 | [Minor] 検証用 tag を打つ前にスナップショットの remote 到達を確かめていない | **解消** | `scripts/spm-snapshot/verify-https-resolution.sh:147-170`。`fetch` → 既定ブランチの `ls-remote --symref` 照会 → tracking ref の存在確認 → `merge-base --is-ancestor HEAD origin/<既定>` の 4 段で、いずれも満たさなければ tag を作らず終了する。fixture で「HEAD は origin/main に含まれる」まで到達することを確認 |
| 2 | [Minor] instrumented test の Scenario GIVEN の置き換えが成果物に残っていない | **解消** | `deviation.md` 2 行目に実行環境の置き換え・理由・エミュレータ条件を CI が担保する旨を記録。`evidence/instrumented-test-counts.txt` に `<testsuites>` の集計行と `<testcase>` 実数 (294 / 39・failure 0) を出典付きで保存。lessons L-002 の求める「実証手段と対象外の面の併記」を満たす |
| 3 | [Minor] `diagnostic-message-language.md` の `timestamp` 未更新 | **解消** | `kasane/handbook/cross/diagnostic-message-language.md:9` が `2026-09-08` |
| 4 | [Suggestion] tag 名の存在確認が固定文字列比較でない | **解消** | `verify-https-resolution.sh:139` / `:222` / `:239` の 3 箇所すべて `grep -qxF` |
| 5 | [Suggestion] ログ出力が process substitution 越しで末尾を取りこぼす | **解消** | `verify-https-resolution.sh:66-87`。環境変数で見分ける自己再実行に変え、`… | tee -a` のパイプラインで shell が `tee` の終了を待つ形にした上で `PIPESTATUS[0]` を本体の終了コードにしている。fixture 実行で生成したログの末尾に後始末の全出力と「結果: 失敗 (exit 1)」「ログ: …」が残ることを確認 |
| 6 | [Suggestion] コメントの「鍵が漏れて」が二義的 | **解消** | `android/build.gradle.kts:105` が「本番発行で鍵の指定が抜けていても」 |
| 7 | [Suggestion] 旧座標が残る利用者向け成果物を docs-refresh へ通す | **未解消 (本 change 外の引き継ぎ)** | `skills/{en,ja}/ksdialogs-android/**` と `skills/{en,ja}/ksdialogs-kmp/references/android-host.md` に旧座標が 12 箇所、`README.md` / `README_ja.md` に各 2 箇所残る。proposal の Non-Goals どおりで違反ではない。`kasane/roadmaps/package-distribution/phases/phase-9-release-workflow/agenda.md:10` に「初回リリース前の docs-refresh 依頼のタイミング」の論点は既にあるが、**旧座標の残存を塞ぐことが目的だと分かる記述にはなっていない**。下の「アクションプラン」に引き継ぎとして再掲する |

### 相方レビュー `second-opinion-code-001.md` (突き合わせで採用された分)

| # | 指摘 | 状態 | 根拠 |
|---|---|---|---|
| A | [Major] tag 削除失敗を検出できず、remote tag が残っても成功扱いになり得る | **解消** | ① 着手フラグ `tag_creation_started` / `tag_push_started` を mutation の**前**に立てる (`verify-https-resolution.sh:267` / `:269`)。push が終了コードを返さずに中断されても後始末が削除に回る。② 成否判定を削除コマンドの終了コードから切り離し、削除後に照会した tag 一覧の実体で判定 (`:209-246`)。③ 照会自体の終了コードを個別に検査し (`:216` / `:233`)、照会できない場合は「消えたことを確認できていない」として `cleanup_verified=no` に倒す。`set -euo pipefail` の `pipefail` は `cleanup` の `set +e` で解除されないため、`git ls-remote \| awk \| sort` のパイプライン先頭の失敗が `$?` に伝播する (実行前が 0 件のときに照会失敗を成功と誤読する相方の指摘した経路が塞がっている)。④ 成功系・remote に tag が残る系・照会不能系のいずれでも `exit_code` が 0 なら 1 に引き上げる (`:250-252`) |
| B | [Minor] コメントが proposed / 改訂前 ADR を確定根拠として参照 | **解消** | `android/ksdialogs-core/build.gradle.kts:16` / `android/ksdialogs/build.gradle.kts:16` の `cross/ADR-0005` 参照は自己完結した写像の現在形説明に置き換わり、`android/build.gradle.kts` の version 導出コメントからも `cross/ADR-0009` 参照が消えた。差分に残る ADR 参照は cross/0002・0004・0005・0006、core/0009、android/0001 のみで、**すべて accepted**。`android/build.gradle.kts:46` の `cross/ADR-0005` は groupId `jp.kamusoft` への参照で、ADR-0019 が改訂を提案しているのは Android の artifactId 写像だけなので、参照先は accepted かつ未改訂の記述に当たる (comment-policy「確定した設計判断への参照」に適合) |

## 指摘事項

### [🟡 Minor] 事前検証で足した `git fetch` が local tag を増やし、後始末の同一性判定が偽の失敗を出す

**該当箇所**: `scripts/spm-snapshot/verify-https-resolution.sh:144` と `scripts/spm-snapshot/verify-https-resolution.sh:153` (判定は `:242`)

**問題点**: local tag のベースライン `tags_local_before` を取るのは 144 行目、`git fetch --quiet origin` は 153 行目で、**ベースラインを取った後に fetch している**。git の既定の tag 自動追従により、`git fetch origin` は取得したオブジェクトが指す remote の tag を local に作る (本レビューで一時リポジトリを作って再現確認済み: fetch 前 0 件 → fetch 後 1 件)。そのため「remote に居るが作業コピーにはまだ無い tag」が 1 本でもあると、後始末の `tags_local_before != tags_local_after` (242 行目) が成立し、検証も後始末も正しく終わった実行が

```
エラー: local の tag 一覧が実行前と一致しません。手で確認してください
結果: 失敗 (exit 1)
```

として記録される。fixture (remote に既存 tag `9.9.9-other` を 1 本置いた状態) で end-to-end に再現した — 検証用 tag の作成・push・消費者ビルド・remote/local からの削除はすべて成功しているのに、スクリプトは失敗を返し、実際には残っていない tag を手で探せと促す。task 7.3 はこのログを evidence/ に保存する手順なので、証跡に「失敗」が残ることになる。

失敗側に倒れる (偽の成功ではない) ため実害は限定的で、配信リポジトリの作業コピーは現在 local tag 0 件・配信リポジトリも phase-3 の状態なので、目前の task 7 でこの経路に入る見込みは低い。ただしスクリプトはリポジトリに残り、design Risks では phase-9 で CI から呼ぶ想定になっている。**初回リリースの tag が打たれた後は、作業コピーが tag を取り込んでいない状態で走らせるたびにこれが起きる**。

なお `git fetch` が local tag を増やすこと自体、ヘッダーの手順説明 (12-16 行目) が挙げる git 操作 (tag の作成・push・削除) に含まれておらず、作業コピーへの副作用が文書化されていない。

**推奨修正**: 事前検証の `fetch` を、ベースライン 2 本 (`tags_remote_before` / `tags_local_before`) を取るより**前**へ移す。remote 到達の検証 (156-170 行目) はベースライン取得の後に置いたままでよい。あわせてヘッダーの手順説明に「事前検証で `git fetch` を行うため、remote にあって作業コピーに無い tag は取り込まれる」を 1 行足すと、後始末が「この手順が作った tag だけを消す」ことと副作用の関係が読み手に閉じる。

### [🔵 Suggestion] `VERIFY_HTTPS_RESOLUTION_LOGGING` が外から立っていると診断不能なエラーになる

**該当箇所**: `scripts/spm-snapshot/verify-https-resolution.sh:71` と `scripts/spm-snapshot/verify-https-resolution.sh:89`

**問題点**: ログ用の自己再実行は環境変数の有無で見分けるが、引数の個数チェック (58 行目) は 2 個でも 3 個でも通す。何らかの理由でこの環境変数が呼び出し元の環境に残っていて引数 2 個で実行すると、ラッパーを飛ばして 89 行目の `readonly LOG_PATH="$3"` に入り、`set -u` により `$3: unbound variable` で落ちる。使い方も原因も分からないメッセージになる。発生確率は低いが、この変数は本体側の内部フラグであって利用者の入力ではないので、外から立っていたときの振る舞いは決めておきたい。

**推奨修正**: 89 行目を `LOG_PATH="${3:-}"` にして空なら `fail` で「内部フラグが外部から設定されている」旨を出す、または 71 行目の条件を「環境変数があり、かつ引数が 3 個」にして、それ以外はラッパー経路へ落とす。

## 確認したが指摘に至らなかった観点

- **中断時の後始末**: ヘッダー 30-35 行目が主張する「終了コードを返さずに中断された場合でも削除を試みる」は、着手フラグの前倒しで push の更新窓については成立している。加えて端末の Ctrl+C 相当 (プロセスグループへの SIGINT) を実測したところ、`trap cleanup EXIT` は `exit_code=130` を見て後始末を走らせ「結果: 失敗」を記録する。`trap` に `INT` / `TERM` を明示していない点は実挙動として問題にならないため指摘しない
- **後始末の tag 一覧比較が第三者の push でも失敗する**: 意図した保守的な倒し方 (「手で確認してください」) と読めるため指摘しない
- **`rm -rf` の使用** (`verify-https-resolution.sh:206`): 対象は `mktemp -d` で作った一時ディレクトリで、スクリプト内の削除に関するオーナー裁定の範囲内
- **旧座標 `ksdialogs-compose` の残存** (skills 12 箇所・README 4 箇所・concepts 5 枚・ADR 4 本・roadmap 系): すべて proposal の Non-Goals (docs-refresh / 蒸留 / ksn-roadmap の責務) に明記された範囲で、本 change の違反ではない
- **`deviation.md` の `[付随修正]`**: 座標リネームに伴うコメント 3 箇所 + doc コメント 2 ファイルの字面置換で、値・コードは不変。同梱条件の範囲内

## アクションプラン

1. **[Minor] `verify-https-resolution.sh` の `fetch` をベースライン取得より前へ移す** — task 7.1〜7.3 に着手する前に。ヘッダーの副作用 1 行も同じ編集で
2. **[Suggestion] `LOG_PATH="${3:-}"` 化などで内部フラグの外部設定に備える** — 1 と同じ編集サイクルでまとめると安い
3. **[引き継ぎ] `skills/` 12 箇所と README 2 枚の旧座標を docs-refresh で追随させる** — 本 change の外。`phase-9-release-workflow` の agenda に「旧座標 (`jp.kamusoft:ksdialogs-compose` / 本体としての `jp.kamusoft:ksdialogs`) の残存を潰す」ことが目的だと分かる形で TODO を積む

---

## 再確認 (修正後)

**日付**: 2026-09-08
**対象**: 本レビューの Minor (事前検証の `git fetch` が local tag を増やす) / Suggestion (`VERIFY_HTTPS_RESOLUTION_LOGGING` が外から立っていると `$3` 未定義で落ちる) と、相方レビュー `second-opinion-code-002.md` の新規 Minor (version 導出式コメントが保証範囲を超える) への修正
**結論**: 3 点とも解消。新たな問題は見つからなかった。判定は **APPROVED** のまま据え置く。

### 1. `--no-tags` で偽失敗の経路が塞がったか — **解消**

`scripts/spm-snapshot/verify-https-resolution.sh:155-158`。ベースライン取得 (`:139` / `:146`) と fetch (`:157`) の順序は元のままで、fetch 側に `--no-tags` を足して tag の自動追従だけを止める形になっている。ベースラインを動かさない分、事前検証と後始末の役割分担 (「実行前の一覧を取ってから remote 到達を確かめる」) が読み手にそのまま残るので、指摘時に挙げた「fetch を前へ移す」案より素直。

ネットワークに出ない fixture (一時ディレクトリに bare リポジトリを作って origin にし、別クローンから remote 側にだけ tag `9.9.9-other` を push、`xcodebuild` はスタブ、origin URL 検査の case に fixture のパスを 1 行足したコピー) で、修正前後を同一条件で走らせて対照した。

| 条件 | local tag (実行後) | 終了 | ログ末尾 |
|---|---|---|---|
| `fetch --quiet origin` (修正前の再現) | `9.9.9-other` が増える | exit 1 | 「local の tag 一覧が実行前と一致しません」/「結果: 失敗 (exit 1)」 |
| `fetch --quiet --no-tags origin` (修正後) | 空のまま | exit 0 | 「tag 一覧は実行前と同一」/「結果: 成功」 |

remote の tag 一覧は両方とも実行前後で `refs/tags/9.9.9-other` のみ (検証用 tag は残っていない)。指摘した「検証も後始末も正しく終わった実行が失敗として記録される」経路が、実際に成功を返すようになったことを end-to-end で確認した。

`--no-tags` の効き方も個別に確かめた。作業コピー側に `remote.origin.tagOpt = --tags` が設定されていてもコマンドラインの `--no-tags` が優先し、local tag は増えない。増えるのは `remote.origin.fetch` に `+refs/tags/*:refs/tags/*` を明示追加した場合だけで、これは `git clone` で作った作業コピーの既定構成には現れない。

### 2. remote 到達確認が壊れていないか — **壊れていない**

同じ fixture で 4 段すべての成立を確認した。

- `git fetch --quiet --no-tags origin` の後も `refs/remotes/origin/main` は remote の最新 commit へ更新される (`--no-tags` は tag の自動追従だけを止め、`+refs/heads/*:refs/remotes/origin/*` の refspec には影響しない)
- `ls-remote --symref origin HEAD` は `refs/heads/main` を返し、`DEFAULT_BRANCH` / `DEFAULT_BRANCH_TRACKING` が解決される
- tracking ref の存在確認 (`:168`) が通る
- `merge-base --is-ancestor HEAD refs/remotes/origin/main` は、push 済みの HEAD で成立し、local に未 push の commit を積むと不成立になる (弾く側も確認)

つまりこの事前検証が本来止めたかったケース (スナップショット本体が push されていない状態) は、`--no-tags` を足した後も引き続き止まる。

### 3. 新たな問題が入っていないか — **入っていない**

3 ファイルとも修正は局所で、本レビューの指摘行番号が `+2` (内部フラグの検査 2 行) と `+4` (加えて `--no-tags` の説明 2 行) だけずれており、他の編集が紛れていないことと整合する (`:144`→`:146`、`:153`→`:157`、`:242`→`:246`)。

- **内部フラグの検査** (`verify-https-resolution.sh:89-90`): 引数の個数を数える形になっており、想定した経路をすべて実測した。フラグあり + 引数 2 個 → 「内部フラグ … 解除してから実行します」で exit 1 (`$3: unbound variable` は出ない)、フラグあり + 引数 3 個 → 本体を直接実行、フラグ無し + 引数 2 個 → ラッパー経由で一時ディレクトリにログ、フラグが空文字 → ラッパー経路 (`-z` 判定と整合)、引数 1 個 / 4 個 → 使い方表示。ラッパー側のログ出力先ディレクトリ検査も従来どおり効く
- **`--no-tags` のコメント** (`:155-156`): 「なぜ止めるか」(後始末の同一性判定が偽の失敗を出す) まで書かれており、そのファイルだけで意味が閉じる。作業文書・変更識別子・proposed ADR への参照は無い
- **version 導出式のコメント** (`android/build.gradle.kts:26-28`): 保証範囲が「注入値・tag・Android の 2 artifact」に限定され、KMP は「`kmp/` が同じ式で自分の version と本体依存版を導出するようになれば」という条件節に移った。現状は `kmp/ksdialogs-kmp/build.gradle.kts:13` が `version = "0.1.0"` を直書きし、`:69` の本体依存もカタログ値を使っているので、条件節が名指しする 2 点 (自分の version と本体依存版) は事実と一致する。言い回しを弱めただけでなく、何が欠けているかが読んで分かる形になっている
- 機械検査: `bash -n` 通過、`local-path-lint.py` / `identity-lint.py` / `comment-policy-lint.py` いずれも違反 0 (comment-policy は 966 ファイル / 禁止 0)、`sync-snapshot-test.sh` 全判定 ok、`android/` の `./gradlew help` 成功 (コメントのみの変更だが設定評価の健全性を確認)

### 確認したが指摘に至らなかった点

- **ヘッダーに fetch の副作用を書き足していない**: 指摘時に併せて薦めた 1 行だが、`--no-tags` によって作業コピーの tag は増えなくなり、残る副作用は remote-tracking ref の更新とオブジェクトの取得だけになった。「remote 到達を確かめる」という手順の説明から自然に読める範囲なので、書き足さない選択で問題ない
- **フラグあり + 引数 3 個での直接実行**: 検査を素通りして本体が走り、末尾の「ログ: …」が実在しないパスを指し得る。ただしこの経路はラッパー自身の再実行が使う正規の入口で、指摘の狙い (診断不能なクラッシュを避ける) は満たされている
- **`android/build.gradle.kts:26` の「tag」**: この 1 ファイルが tag を扱うわけではないが、注入値の出どころを示す導出式の目的説明として読める。修正前から在る表現で、今回の限定によって主語 (Android の 2 artifact) が明確になったため指摘しない
