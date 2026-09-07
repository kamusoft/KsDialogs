# レビュー結果: fix-sample-android-back-and-rotation (002 回目)

**日付**: 2026-09-04
**判定**: APPROVED

## サマリー

`review-001.md` の Minor 2 件に対する証跡側の修正を確認した。Minor 1 (kmp-a36 の項目 3 の遷移先) は README の補足で説明が入り**解消**、Minor 2 (06 / 07 が android の 04 / 05 とバイト同一) は撮り直しで 4 枚とも別ファイルになり**解消**した。Suggestion のうち Declarative Dialog の記録 (項目 7) と端末設定の変更前の値も README に入っている。実装コードは前回から一切変わっておらず (両 Sample の `assembleDebug` が up-to-date で BUILD SUCCESSFUL)、新規の指摘は README の記述精度に関する低優先度の Minor 1 件のみ。

## 実行した検証

| 対象 | 手段 | 結果 |
|---|---|---|
| 実装差分の不変 | `git diff --stat` / 実装 7 ファイルの mtime | 差分の顔ぶれ・行数とも review-001 時点と同一。7 ファイルすべて mtime が `review-001.md` (14:49) より前 (最新 14:27)。変わったのは `verification/` (14:54〜14:55) だけ |
| ビルド | `samples/android` で `:app:assembleDebug` / `samples/kmp` で `:androidApp:assembleDebug` (`--offline`) | どちらも BUILD SUCCESSFUL。android は 72 タスク全 up-to-date で、review-001 がビルドした成果物と同一であることが裏取りできた |
| 画像の同一性 | `md5 *.png` | 9 枚すべて相異 (前回同一だった 04/06・05/07 が別ハッシュに) |
| 画像の実体 | 04 / 06 / 07 を目視 + PIL で画素比較 | 06 / 07 は 1080px (ステータスバー込み)、04 / 05 は 1005px (帯を落とし済み)。06 は 14:53、07 は 14:54 の時計を持つ |
| 個人情報 | 06 / 07 を目視 | 通知アイコン・アカウント名・端末名・キャリア名・Wi-Fi SSID・位置情報の写り込みなし。写っているのはミュート/通知オフ・電波・Wi-Fi 強度・電池 (07 は 79% 充電中) のみ。個人特定にはつながらない |
| 標準 lint | `scripts/local-path-lint.py` / `scripts/identity-lint.py` を `--paths verification/README.md` で実行 | どちらも rc=0 (違反 0 件) |
| 端末シリアル | README・ログの読み取り | `<DEVICE-A33>` / `<DEVICE-A36>` のプレースホルダのみ。生の serial なし |

## 照合した規約

- `kasane/handbook/cross/comment-policy.md` (always) — 今回の差分はソースを含まないため対象コードなし。README も配布物ではなく作業証跡
- `kasane/handbook/cross/runtime-behavior-verification.md` (実行時挙動の不具合修正の完了判定) — 3 条件 (修正前の再現 `android-a36-before.log` / 修正後の同一手順での解消 / 証跡の change 配下保管) は今回の差し替え後も維持されている
- `kasane/handbook/cross/test-execution.md` (完了判定) — 実装不変の裏取りとしてビルドのみ再実行。テスト実行結果は review-001 の記録が有効
- `kasane/handbook/cross/sample-parity.md` (`samples/**` の証跡) — 06 / 07 の本体画素が 04 / 05 と一致するのはパリティ規約が成り立っている状態そのもので、規約違反ではない
- `kasane/lessons/` — `code-review.md` は存在しない (`process.md` / `spec-review.md` のみ)。「指摘しないこと」の登録なし

## 前回指摘への対応状況

| # | review-001 の指摘 | 対応状況 |
|---|---|---|
| 1 | [🟡 Minor] kmp-a36 の項目 3 だけ遷移先が違い README に説明がない | **解消** |
| 2 | [🟡 Minor] 06 / 07 が 04 / 05 とバイト同一 | **解消** (残件は下記 Minor へ) |
| 3 | [🔵 Suggestion] Declarative Dialog が結果表に無い | **解消** |
| 4 | [🔵 Suggestion] `handleOnBackPressed` に else が無い | **未対応** (実装不変。任意対応のため判定に影響させない) |
| 5 | [🔵 Suggestion] 端末設定の原状復帰が事後に検証できない | **解消** |
| 6 | [🔵 Suggestion] 回転手順を撮影レシピへ / `samples/README.md` と sample-parity の追随 | **未対応 (想定どおり)** — 蒸留での作業として申し送り済み |

### 1 (Minor): 解消

README「結果」の補足に項目 3 の段が新設され、「終了」の判定根拠 (Sample の Activity が前面から消えたこと)、遷移先が端末に残っていたタスクの並びで決まること、`kmp-a36.log` だけ android Sample が前面化した理由が書かれている。ログの実体 (`kmp-a36.log` の「戻る 2 回目」= `jp.kamusoft.ksdialogs.samples.android/.MainActivity`、他 3 本は `NexusLauncherActivity`) と README の記述が一致することを確認した。証跡だけを読む立場からも結果表の「終了」を裏付けられる。

### 2 (Minor): 解消

4 枚とも md5 が相異し、06 / 07 は通知アイコンだけを落として時計・電波・電池を残した状態で撮り直されている (1080px = 帯込み、04 / 05 は 1005px = 帯を落とし済み)。06 は 14:53、07 は 14:54 と、04 / 05 (14:36 に保存) とは別の観測回であることが画像自体から読める。「別名の 2 枚が実は同じファイル」という取り違えの余地は消えた。撮影に使った `cmd statusbar send-disable-flag notification-icons` とその復帰も手順・設定表に記録されている。

### 3 (Suggestion): 解消

結果表に項目 7「修正後: Declarative Dialog (Compose 登録経路) が表示される」が追加され、android / kmp の API 36 列が「表示」、API 33 列が `—`。補足で出所 (`../review-001.md` の実測)・理由 (ダイアログのウィンドウ根が自前で owner を据え直す)・画像証跡を残していないことまで開示されている。

### 5 (Suggestion): 解消

「手順」節に変更前 / 観測時 / 復帰後の 3 列を持つ設定表が入り、API 33 の `accelerometer_rotation` が元から 0 だったこと (= 戻し漏れではないこと) が読めるようになった。ステータスバーの disable flag も同じ表に載っている。

## 指摘事項

### [🟡 Minor・低優先度] README の「どちらのルートの観測かが読める」が画像の実力を超えている

**該当箇所**: `verification/README.md`「証跡」節の「そのため 4 枚はいずれも別ファイルで、時計と端末の画面サイズからどちらの端末・どちらのルートの観測かが読める」

**問題点**: 画素で照合したところ、06 の上端 75px (ステータスバー) を落とすと 04 と**完全一致** (差分 0 バイト / 7,236,000)、07 と 05 も同様に完全一致だった。つまり画像から読めるのは「端末 (画面幅 2400 / 2340)」と「別の観測回であること (時計)」までで、**ルート (android / kmp) の別は画像からは判別できない**。README 自身が末尾で「どのルートを起動した観測かは、いずれの場面も同じ場面の `.log` が併せて持つ」と正しく書いており、証跡の組としては成立しているが、その 2 文が食い違っている。証跡文書は後から読む人 (蒸留・別変更のレビュー) が根拠として引く前提なので、読める範囲を過大に書くと次の人が画像だけでルートを判定できると誤認する。

**推奨修正**: 当該 1 文を「時計と画面サイズから別の観測回・別端末であることが読める。ルートの別は同じ場面の `.log` が持つ」の趣旨に直す。1 行の書き換えで足り、画像の撮り直しは不要。

## 確認した観点 (指摘に至らなかったもの)

- **個人情報**: 06 / 07 を実際に開いて確認。ステータスバーに残っているのはミュート (06) / 通知オフ (07)・電波強度・Wi-Fi 強度・電池残量で、キャリア名も SSID もアカウント名も端末名も出ていない。07 の「79%」は電池残量であり個体特定にはつながらない。画面内容は Sample のメニューとダイアログのみ
- **ローカル絶対パス・シリアル**: README 本文のパス表記は change 相対 (`../review-001.md`) とリポジトリ相対 (`samples/README.md` 等) のみ。端末はプレースホルダ。`local-path-lint.py` / `identity-lint.py` とも違反 0 件
- **証跡表と実ファイルの対応**: 表に挙がる 9 枚 + 5 本のログがすべて実在し、余剰ファイルもない
- **実装への波及**: 今回の差分は `verification/` のみ。`MainActivity` (2 ルート)・`AndroidManifest.xml` (2 ルート)・`build.gradle.kts` (2 ルート)・`libs.versions.toml` に新たな差分はなく、ビルドも 2 ルートとも up-to-date で成功
- **Suggestion 4 (else 欠落) の未対応**: 実装不変を確認済み。任意対応の Suggestion であり、パネルの開閉経路が 4 本に閉じている現状で到達しないという前回の分析も変わっていないため、判定には影響させない

## アクションプラン

1. (Minor・低優先度) `verification/README.md` の「どちらのルートの観測かが読める」の 1 文を、ルートの別はログが持つ旨に書き換える。アーカイブ前に直せばよく、本判定を保留する理由にはしない
2. (任意・申し送り) review-001 の Suggestion 4 (`handleOnBackPressed` の else 欠落) は未対応のまま。対応するなら 2 ルート同形で
3. (蒸留で) review-001 の Suggestion 6 — `kasane/config.yaml` の `ui.screenshot` に回転手順、`samples/README.md` と `sample-parity.md` に「OS 操作への反応」を追記
