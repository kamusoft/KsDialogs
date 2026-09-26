# レビュー結果: define-loading-action-thread (003 回目)

**日付**: 2026-09-25
**判定**: APPROVED (末尾の「追記: 指摘への対応の確認」で更新。初回の判定は CHANGES_REQUESTED)

## サマリー

今回見たのは、review-002 の APPROVED の後に入った付随修正 (handbook cross/test-execution.md の件数表・但し書き・skip の説明・負の検査の表の android/ 5 行と表の前の説明文・lint 違反 1 件の言い回し) と、その記録 (deviation.md 末尾 2 項・evidence/test-run-summary.md の追記) である。件数表 7 行と skip の説明は、実測の記録と端末・Gradle が残した結果 XML に一致した。android/ の 5 行も、自分で 1 本回した結果と一致した。

一方で、表の前の説明文から「オーバーロードされた呼び出し面では 2 件出る」という一般則を消したのは、android/ でしか測っていないのに言い過ぎている。kmp/ の overload された `show` では今も 2 件出る (自分で実測した)。また、表を直した結果、同じ期待診断を書いた android/ の負の検査ソースの doc コメントとも食い違うようになった。どちらも値が分かっている食い違いであり、この change の付随修正で直すべきと判断した。

## 照合した規約

- cross/test-execution.md — テストを実行するとき・テスト結果を報告するとき・変更の完了を判定するとき (今回のレビュー対象そのもの)
- cross/verification-ci.md — 同じ件数を別の場所で言っている文の照合のため
- cross/comment-policy.md (always) — 今回の範囲にソースの変更は無いため照合の対象外。負の検査ソースの doc コメントは指摘 2 の文脈で読んだ
- ksn-core references/handbook.md (frontmatter の `timestamp` の意味・更新履歴の置き場) と references/doc-structure.md (構造 lint)

## 確認したこと

| 観点 | 照合先 | 結果 |
|---|---|---|
| 件数表 ios/ 286 | evidence 6.1 の表 (Swift Testing 286 / 51 suites) | 一致 |
| 件数表 android/ 68 | `ksdialogs-core/build/test-results/testDebugUnitTest/` の結果 XML (16 ファイル合計 68 / failures 0) | 一致 |
| 件数表 android/ (instrumented) 344 = 304 + 40 (API 33 実機) | `androidTest-results/connected/debug/` の結果 XML: `:ksdialogs-core` tests 304 / failures 0 / skipped 1、`:ksdialogs` tests 40 / failures 0。どちらも Android 13 端末 | 一致 |
| 件数表 kmp/ 166 = 85 + 81 | `iosSimulatorArm64Test` の XML 合計 85、`testAndroidHostTest` の XML 合計 81。failures 0 | 一致 |
| 件数表 maui/ 172・maui/android/native/ 34・maui/macios/native/ 7 / 4 suites | evidence 6.1 の表 (maui/android/native/ は結果 XML の合計 34 でも一致) | 一致 |
| 但し書き「2026-09-25 に全 7 ルートを実測、instrumented は API 33 の 1 台で全件成功」 | evidence「最終コードでの全件実行」の表 (失敗 0) | 一致。ios/・maui/・maui/macios/native/ の 3 ルートは報告経路を直す前の作業ツリーで測った値だが、直したのは Android の Kotlin だけで、この 3 ルートには効かない。主張の範囲に収まっている |
| API 30 以上で skip される 1 本 = `PB_SB_04` | `DialogSystemBarsTests.kt` の `assumeTrue(SDK_INT < R)` と結果 XML の skipped 要素 | 一致。修正前の「IME の出し入れ」は向きが逆だったので、正しく直っている |
| API 29 で skip される 6 本 | `assumeTrue(SDK_INT >= R)` は `DialogSystemBarsTests` の 5 本 (`assumeInsetsControllerPath`) と `ToastSystemInputTests` の IME の 1 本 | 数は合う。言い回しは提案 1 を参照 |
| 「API 依存の `assumeTrue` はすべて API 30 を境」 | androidTest 内の `assumeTrue` の全箇所 | 一致 (API によらない 1 箇所はシステムバーが観測できるかどうかの前提確認) |
| android/ 負の検査 `showTransition` | 自分で実行: `./gradlew :api-surface-check:compileDebugKotlin -Pksdialogs.negativeCheck.showTransition --rerun-tasks` | ビルドは失敗。`e:` は `None of the following candidates is applicable:` の 1 件だけ。候補は 3 本で、どれも `transition` を取らない。evidence・handbook と一致 |
| doc-structure lint | `python3 scripts/doc-structure-lint.py --verbose` | test-execution.md の違反は 0 件になった。言い回しを直した項も元の意味を保っている |
| 付随修正の同梱条件 | ksn-core「付随修正」・lessons inbox `test-count-table-must-follow-in-change` | 件数表の追随は lessons のルール文どおりの同梱であり、スコープ外の指摘はしない |

ビルドとテスト: 今回の範囲はドキュメントと証跡だけで、実装は review-001 / 002 で確認済み。全件の再実行はせず、結果 XML の照合と負の検査 2 本 (android/ 1 本・kmp/ 1 本) の実行で確かめた。

## 指摘事項

### 🟡 Minor (優先度高) オーバーロードの一般則を消したことで kmp/ の実態と食い違った

**該当箇所**: `kasane/handbook/cross/test-execution.md:188`、同 `:241` (kmp/ `showOptions` の行)

**問題点**: 説明文から「呼び出し面がオーバーロードされていると候補不適合の行が先行して 2 件出る」を消し、「android/ で `show` のようにオーバーロードされた呼び出し面に誤った引数名を渡したときは、候補不適合の 1 件だけが出る」に置き換えた。しかし測ったのは android/ の 5 本だけで、消した一般則は kmp/ では今も成り立っている。kmp/ の `KsDialog.show` も 2 本にオーバーロードされており、自分で次を実行した。

`cd kmp && ./gradlew :api-surface-check:compileKotlinIosSimulatorArm64 -Pksdialogs.negativeCheck.showOptions --rerun-tasks`

ビルドは失敗し、診断は `None of the following candidates is applicable:` (`KClass` を取る候補の型不一致の説明つき) と `No parameter with name 'options' found.` の 2 件だった。

- kmp/ の行は `No parameter with name 'options' found.` の 1 件だけを書いている。説明文は「原則 1 件、2 件になるのは型を import と使用の両方で書いたときで、その場合は表に両方書く」と読めるので、kmp/ で 2 件出る理由がどこにも書かれていない。kmp/ の行が片方しか書いていないのは修正前からだが、修正前は説明文の一般則がこの 2 件を説明していた
- 説明文が 1 件になる条件として挙げた「オーバーロードされた呼び出し面」は、条件として足りない。同じくオーバーロードされた kmp/ の `show` では 2 件出る。分かれ目は、誤った引数名の前に型の合う候補がいくつ残るかにあるように見える (android/ は `viewModel` を取る候補が複数あり、kmp/ は型の合う候補が 1 本だけ)。ただし、この仕組みは測っていない

**推奨修正**:
- kmp/ `showOptions` の行を、実測した 2 件 (`None of the following candidates is applicable:` と `No parameter with name 'options' found.`) を書く形に直し、実測日を添える
- 説明文の「オーバーロード」の文は、測ったことだけに絞る。例: 「android/ の `show` 系 5 本は候補不適合の 1 件だけ、kmp/ の `show` は候補不適合と引数名不明の 2 件 (どちらも 2026-09-25 実測)」。仕組みの説明 (オーバーロードだから 1 件になる) は書かない
- deviation.md の該当項 (負の検査の表と説明文) に kmp/ の行も含めた旨を書き足し、evidence/test-run-summary.md に kmp/ 1 本の実測を加える

### 🟡 Minor (優先度高) android/ の負の検査ソースの doc コメントが、直した handbook と食い違っている

**該当箇所**:
- `android/api-surface-check/src/negativeCheckLoadingShowOptions/kotlin/jp/kamusoft/ksdialogs/apicheck/RejectsOptionsArgumentOnLoadingShow.kt:13-14` (この change で文言を直したファイル)
- `android/api-surface-check/src/negativeCheckLoadingShowStyle/kotlin/jp/kamusoft/ksdialogs/apicheck/RejectsStyleArgumentOnLoadingShow.kt:12-13`
- `android/api-surface-check/src/negativeCheckToastShowStyle/kotlin/jp/kamusoft/ksdialogs/apicheck/RejectsStyleArgumentOnToastShow.kt:12-13`
- `android/api-surface-check/src/negativeCheckShowOptions/kotlin/jp/kamusoft/ksdialogs/apicheck/RejectsOptionsArgumentOnShow.kt:12`

**問題点**: 検査ソース冒頭の「期待する診断」は handbook の表と同じ事実を書いた 2 か所目である。直したのは handbook の側だけなので、3 本はまだ「`None of the following candidates is applicable:` と `No parameter with name '...' found.` (show がオーバーロードされているため 2 件出る)」と書いている。`showOptions` の 1 本は「期待する診断: No parameter with name 'options' found.」で、実測ではこの診断自体が出ない。検査を回した人がソースのコメントを期待値として読むと、「期待した診断が出ていない = 別の誤りで失敗した」と取り違える。`RejectsOptionsArgumentOnLoadingShow.kt` はこの change で同じ doc コメントの別の行を直しており、食い違いが同じコメントの中に残っている。

**推奨修正**: 4 本の「期待する診断」を handbook の各行と同じ内容 (候補不適合の 1 件だけ) に揃え、deviation.md の付随修正の項に含める。付随修正の目安 (3 ファイル以内) を 1 本超えるが、どれもコメントだけの変更で設計判断を含まない。この change で扱わないのであれば、deviation.md に「検査ソースのコメントは別途」と理由をつけて記録する。`negativeCheckShowTransition` のソースは期待する診断を書いていないので対象外。

### 🟡 Minor frontmatter の `timestamp` が古いまま

**該当箇所**: `kasane/handbook/cross/test-execution.md:8`

**問題点**: 件数表・skip の説明・負の検査の表を 2026-09-25 の実測で確かめ直したのに、`timestamp` (最終検証日。ksn-core references/handbook.md は「確認した日に更新する」と定める) は 2026-09-09 のままである。ksn-drift の棚卸しで、この文書が 9-09 以降確かめられていないように見える。

**推奨修正**: `timestamp: 2026-09-25` に更新する。文書全体を確かめ直したわけではないので、更新を蒸留に回すならその旨を deviation.md に残す。

### 🔵 Suggestion API 29 で skip される 6 本の言い回し

**該当箇所**: `kasane/handbook/cross/test-execution.md:84`

**問題点**: 「API 29 で 6 (IME の出し入れなど、API 30 以上でだけ判定できるもの)」とある。実際には 6 本のうち 5 本が `WindowInsetsController` 経路のシステムバーのテスト (`DialogSystemBarsTests` の `PB_SB_01/02/03/06/07`) で、IME は 1 本だけである。「など」で間違いにはならないが、代表として挙げる例が少数側に寄っている。

**推奨修正**: 「`WindowInsetsController` 経路のシステムバー 5 本と IME の出し入れ 1 本」のように内訳で書く。

### 🔵 Suggestion 同じ件数を言っている文が verification-ci.md に古いまま残っている

**該当箇所**: `kasane/handbook/cross/verification-ci.md:57`

**問題点**: 「手元 (12 論理 CPU 級) では既定の並列で 277 件が安定する」とあり、件数表 (ios/ 286) と食い違って見える。実測した日の記録としては正しいが、日付が無いので今の件数として読まれる。

**推奨修正**: 件数を外して「手元では既定の並列で全件が安定する」にするか、実測日 (2026-09-08) を添える。今回の ios/ の実行 (286 件、並列、成功) は 1 回だけなので、「安定する」の根拠として 286 に置き換えるのは避ける。

### 🔵 Suggestion deviation.md の「HEAD でも同じだった」の範囲

**該当箇所**: `deviation.md` 末尾から 2 項目 (負の検査の表の付随修正)

**問題点**: 「実測ではどれも候補不適合の 1 件だけで、HEAD (b0657a9) のコードでも同じだった」は、5 本とも HEAD で確かめたように読める。evidence/test-run-summary.md によると、HEAD のコードで回したのは `loadingShowOptions` の 1 本だけである。

**推奨修正**: 「HEAD のコードでも `loadingShowOptions` で同じだった」のように、確かめた範囲に合わせる。

## アクションプラン

1. handbook の説明文と kmp/ `showOptions` の行を、kmp/ の実測 (2 件) に合わせて直す。evidence に kmp/ 1 本の実測を足し、deviation.md の項に kmp/ も含める (指摘 1)
2. android/ の負の検査ソース 4 本の「期待する診断」の doc コメントを handbook と揃えるか、揃えない理由を deviation.md に記録する (指摘 2)
3. `test-execution.md` の `timestamp` を更新する (指摘 3)
4. 余裕があれば提案 3 件に対応する (API 29 の skip の内訳・verification-ci.md の 277・deviation の「HEAD でも同じ」の範囲)
5. 蒸留では、今回の handbook の変更を concepts/log.md に記録する (handbook/index.md が更新履歴の置き場としている場所)

## 追記: 指摘への対応の確認

**日付**: 2026-09-25
**更新後の判定**: APPROVED

対応後の作業ツリーで、handbook・検査ソース・deviation.md・evidence/test-run-summary.md の差分を読んで確かめた。

| 指摘 | 対応 | 確認結果 |
|---|---|---|
| 指摘 1 (kmp/ の件数と説明文) | `test-execution.md:241` の kmp/ `showOptions` の行を「`None of the following candidates is applicable:` と `No parameter with name 'options' found.` の 2 件 (2026-09-25 実測)」に直した。`:188` の説明文は「誤った引数名を `show` に渡す検査は、ルートによって件数が違う — android/ の `show` 系 5 本は 1 件、kmp/ の `showOptions` は 2 件 (どちらも 2026-09-25 実測)」に直した。evidence の末尾に kmp/ 1 本の実測を追記し、deviation.md の項に kmp/ を含めた | 満たす。行の内容は、このレビューで kmp/ を実測した結果と一致する。説明文は実測した事実だけを述べ、オーバーロードを原因とする説明は消えている。「原則 1 件」の文とも矛盾しない |
| 指摘 2 (検査ソースの doc コメント) | android/ の 4 本 (`RejectsOptionsArgumentOnShow` / `RejectsOptionsArgumentOnLoadingShow` / `RejectsStyleArgumentOnLoadingShow` / `RejectsStyleArgumentOnToastShow`) を「候補不適合の 1 件だけ (引数名不明の診断は出ない。2026-09-25 実測)」に、kmp/ の `RejectsOptionsArgumentOnShow` を 2 件に揃えた。deviation.md の項に含めた | 満たす。5 本とも handbook の各行と同じ内容になった。差分はコメントだけで、コードは変わっていない。`comment-policy-lint.py --advisory --paths` (5 ファイル) は禁止 0 件。要確認 5 件は、どれも以前からある ADR 参照の行 (8 行目付近) で、今回の対応が足した行ではない。検査ソースは配布物ではなく、従来からの書き方の範囲にある。実測日の注記は計測の条件であり、禁止類型の進捗ログ・履歴記述には当たらないと判断した |
| 指摘 3 (`timestamp`) | `timestamp: 2026-09-25` に更新 | 満たす |
| 提案 1 (API 29 の 6 本) | `:84` を「`WindowInsetsController` 経路のシステムバー 5 本と IME の出し入れ 1 本。どちらも API 30 以上でだけ判定できる。2026-09-06 実測」に直した | 満たす。`assumeTrue(SDK_INT >= R)` の内訳 (`DialogSystemBarsTests` の 5 本・`ToastSystemInputTests` の 1 本) と一致する |
| 提案 2 (verification-ci.md の 277) | `verification-ci.md:57` を「全件 (2026-09-08 実測で 277 件)」にした | 満たす。286 に置き換えず実測日を添えた形なので、1 回の実行を「安定する」の根拠にしていない |
| 提案 3 (deviation の「HEAD でも同じ」) | 「HEAD (b0657a9) のコードでも `loadingShowOptions` で同じだった」に絞った | 満たす。evidence の切り分けの記録 (HEAD で回したのは 1 本) と一致する |

あわせて確かめたこと:

- `doc-structure-lint.py --verbose` で、`test-execution.md`・`verification-ci.md` の違反は 0 件
- deviation.md の付随修正の項は、同梱の中身 (handbook の表と説明文、検査ソース 5 本のコメント) と理由を記録している。検査ソースは付随修正の目安 (3 ファイル以内) を超えるが、どれもコメントだけで、設計判断もユーザーが選ぶ分岐も含まない。指摘 2 の推奨どおりの範囲に収まっている
- 新しい指摘は無い

アクションプランで挙げた蒸留時の concepts/log.md への記録 (5 番目) は、蒸留の作業として残る。
