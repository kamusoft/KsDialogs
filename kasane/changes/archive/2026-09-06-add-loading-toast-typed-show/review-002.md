# レビュー結果: add-loading-toast-typed-show (002 回目)

**日付**: 2026-09-06
**判定**: APPROVED

## サマリー

review-001 の Minor 2 件と、second-opinion-code-001 の突き合わせで確定した Minor 1 件 (ホスト Suggestion 1 と同じ箇所) の修正を、修正 diff の 5 ファイルだけを対象に新鮮な目で確認した。3 件とも指摘どおりに直っており、直し方が新しい問題を持ち込んでいる形跡はない。Android の Toast 型指定経路は `ToastDisplay.typedViewModel` に生成 VM を預けるようになり、`ToastDisplay` の kdoc が宣言する不変条件 (「撤去が完了するまでここが握る」) は通常 / インライン / 型指定の 3 経路と、完了・破棄・提示先入れ替わりのすべての終わり方で成立する。Compose の `TS_YA_03` は同モジュールの既存テストと同じ 600 ms + 取り外し待ちの作法にそろった。

Critical / Major は無い。新たな指摘は Minor 2 件 (いずれも本体コードではなく完了ゲートと handbook の追随) と Suggestion 1 件。ソース側の修正は完成していると判断する。

## 照合した規約

- `kasane/handbook/cross/comment-policy.md` (always — 全ソースファイル。今回は公開 doc コメントの ADR ID の節を再照合)
- `kasane/handbook/cross/test-execution.md` (完了ゲート・全件実行の定義・件数表の更新義務・scenario-id-coverage)
- `kasane/handbook/cross/sample-parity.md` — 修正 diff が samples に触れていないため今回は再照合のみ (差分なし)
- `kasane/handbook/cross/runtime-behavior-verification.md` / `local-development-setup.md` / `aiforms-origin-reference.md` / `user-skill-api-listing.md` / `user-skill-writing-style.md` — 適用外 (不具合調査・環境構築・未移植機能・`skills/**` のいずれにも当たらない)
- ADR: core/ADR-0030 (1 Toast 1 器)・0033 (受理後の失敗)・0035 (本 change の決定)
- lessons: `kasane/lessons/code-review.md` は不在 (重点観点・指摘しないことの適用なし)。`process.md` の姉妹面照合の観点を Minor 1 の修正確認に適用した

## 実行した検査 (本セッション)

- `android/` の Kotlin コンパイル (`:ksdialogs:compileDebugKotlin` / `:ksdialogs:compileDebugAndroidTestKotlin` / `:ksdialogs-compose:compileDebugAndroidTestKotlin`) → exit 0
- `python3 scripts/comment-policy-lint.py --advisory` → 禁止 0 件 / 要確認 451 件 (修正前の 453 件から 2 件減。減ったのは Android の 2 レジストリで、その 2 ファイルは検出一覧から消えている)
- `python3 scripts/local-path-lint.py` / `identity-lint.py` → いずれも 0 件
- `python3 scripts/scenario-id-coverage.py --require-mirror` → 本 change の ID は未網羅 0・両 Native ミラー OK

テストの再実行は行っていない (コンテキストパッケージ提示の実測値を前提にした)。

## 確定指摘の修正確認

### Minor 1 (Android Toast の型指定経路が生成 VM を保持しない) — 修正済み

`android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/ToastDisplay.kt:80-88` に `var typedViewModel: ToastViewModel?` が入り (解放は同ファイル `:101`)、`android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/ToastCoordinator.kt:206-207` で `prepare()` の戻り値を預けてから `createView` に渡すようになった。全経路で不変条件が成立することを次のとおり確認した:

- **保持の開始**: 型指定経路で VM が存在しうるのは `createContent` が `prepare()` を呼んだ瞬間以降だけで、その直後に `display` へ預けている。生成されたのに誰も握っていない窓は無い
- **二重生成なし**: `attachIfPossible` の `if (display.contentView == null)` ガードにより `createContent` は成功後に再入しない。提示先の入れ替わりで載せ直しても `prepare()` (= 利用者の configure) は再実行されない
- **解放**: 終わり方は `finish` (期限到達) と `discard` (期限切れでの破棄 / 中身生成の失敗) の 2 つで、どちらも `removeDisplay` → `releaseResources()` を通り `typedViewModel = null` になる。`ToastCoordinator.kt:206-207` で VM を預けた直後に `createView` が投げた場合も `discard` 側に落ちて解放される
- **例外経路**: `beginDisplay` の期限先行チェックで表示リストに載らずに捨てられる場合は、そもそも `createContent` を通らないので VM が存在しない
- **姉妹面**: iOS は `ios/Sources/KsDialogs/Presentation/ToastCoordinator.swift:294` の `ToastResolvedContent(content:viewModel:)` から `ToastDisplay(viewModel:)` へ渡して同じ期間握る。Android がこれにそろった

`typedViewModel` は書くだけで読まないフィールドだが、kdoc がその意図 (参照の寿命を他経路とそろえるためだけに置いている) を単独で説明しており、外部文書の ID に依存していない。前 change の `isDismissing` 残置と同じ扱いで問題ないと判断する。

### Minor 2 (Compose テストの後始末) — 修正済み

`android/ksdialogs-compose/src/androidTest/kotlin/jp/kamusoft/ksdialogs/compose/ComposeTypedShowTests.kt:132` の `DURATION_MILLIS` が 4000 → 600 になり、同ファイル `:102-110` で表題の観測後に「取り付け済みの中身を掴む → 取り外されるまで待つ」の 2 段待ちが入った。同モジュールの `android/ksdialogs-compose/src/androidTest/kotlin/jp/kamusoft/ksdialogs/compose/ComposeToastCustomViewTests.kt:102-110` と同じ形・同じ定数で、待ちの上限は共有の `PRESENTATION_TIMEOUT_MILLIS` (10 秒) を使う。共有シングルトンへ 4 秒の表示を残したまま抜ける形は解消しており、後続の Toast 系テストへの混入経路は無くなった。

### 相方 Minor / ホスト Suggestion 1 (公開 doc の ADR ID) — 修正済み (Android 分)

`android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/LoadingViewRegistry.kt:7-17` と `ToastViewRegistry.kt:7-17` の class doc から ADR ID が消え、2 スロット・スロット単位の後勝ち・独立レジストリという利用者向けの契約だけが残った。comment-policy lint の検出一覧からこの 2 ファイルが消えていることも確認した。

## 指摘事項

### [🟡 Minor] android instrumented の全件実行が handbook の「全件」の定義を満たしていない

**該当箇所**: `kasane/handbook/cross/test-execution.md:80` (「全件を実行したと言えるのは**対象 API レベルをそれぞれ1台ずつ回したとき**」) と `tasks.md:29` (5.1 の完了ゲート)

**問題点**: 最終状態の instrumented 実測は API 33 / 36 の 2 台 (各 333 tests) で、API 29 の台が回っていない。handbook は API 依存の `assumeTrue` が API 30 を境に両向きにあることを明記しており、API 30 以上では 1 本 (`android/ksdialogs/src/androidTest/kotlin/jp/kamusoft/ksdialogs/DialogSystemBarsTests.kt:138` の `PB_SB_04_旧経路でも非表示状態が維持される`。`systemUiVisibility` の旧経路は Android 11 未満にしかない) が skip される。API 33 / 36 だけの実行では、この 1 本はどの台でも実行されていない。直前の change (fix-android-instrumented-toast-back-loading-coalescing) は API 29 / 33 / 36 の 3 台で回して規約どおりに閉じており、本 change はその作法から後退している。

実害の見込みは薄い — 未実行の 1 本は Dialog の旧経路システムバーの検証で、本 change の diff は Dialog 面 (`DialogViewRegistry` / `KsDialog` / `DialogContainer`) に一切触れていない。それでも「完了ゲートを全部通す」と宣言した tasks 5.1 の文言は文字どおりには満たされていない。

**推奨修正**: `ksn_api29` エミュレータで `./gradlew connectedDebugAndroidTest` を 1 回通して件数を記録すれば閉じる (残る 2 台の結果はそのまま使える)。回さずに進めるなら、「本 change は Dialog 面に触れないため API 29 の台を省いた」ことをオーナー判断として deviation.md に記録する。

### [🟡 Minor] handbook の実測件数表が本 change の増分に追随していない

**該当箇所**: `kasane/handbook/cross/test-execution.md:19-27` の件数表

**問題点**: 本 change でテスト構成が大きく育っている (ios 251 → 275、android instrumented 305 → 333、maui 133 → 153) のに、表は 2026-08-28 実測のままで更新されていない (`kasane/handbook/` に差分なし)。同じ節の `test-execution.md:17` が「テスト構成が育って実態が変わったら本規約を実測で更新する」と自分で定めており、先例 (rename-dialog-contract-singular は負の検査表を 55 → 59、fix-android-instrumented は instrumented 行を実装コミットで更新) もそろって実装側で追随させている。表は完了判定で件数を突き合わせる正なので、放置すると次の change が古い基準で「件数が合わない」を判断することになる。

**推奨修正**: 蒸留 (ksn-distill) で `test-execution.md` の ios / android (instrumented) / maui の 3 行を本 change の実測へ更新する。前項の API 29 を回すなら、その結果もあわせて反映する。

### [🔵 Suggestion] 公開 doc からの ADR ID 除去が、この change が書き換えた iOS の同じ行に及んでいない

**該当箇所**: `ios/Sources/KsDialogs/Registry/LoadingViewRegistry.swift:6-7` と `ios/Sources/KsDialogs/Registry/ToastViewRegistry.swift:6-7`

**問題点**: review-001 の Suggestion 1 は「触った行の方向をそろえる」を提案し、その後 MAUI (削除) と Android (削除) はそろった。一方 iOS の 2 レジストリの class doc は、**本 change の diff がこの行自体を書き換えている** (「View factory を引く」→「View factory と ViewModel factory を引く」に改め、2 行に折り返した) にもかかわらず `(core/ADR-0025)` / `(core/ADR-0029)` を残している。結果として、同じ change の中で同じ役割の公開 class doc が 3 形態のうち iOS だけ逆向きになった。なお second-opinion-code-001 の突き合わせ表にある「iOS の既存行は未変更」は事実と食い違う (行は書き換わっており、ADR ID だけが残っている)。

comment-policy lint は advisory 止まりでリポジトリ全体の既存債務でもあるため、この change に閉じた違反として扱う必要はない。

**推奨修正**: 方向は既に「触った公開 doc からは ADR ID を落とす」で 2 形態ぶん決まっているので、iOS の 2 行も同じにそろえるのが素直 (削除だけで文意は壊れない)。この change に同梱しない選択も妥当で、その場合は蒸留か ksn-drift の棚卸しへ回す。

## アクションプラン

1. (推奨) API 29 の台で instrumented を 1 回通して完了ゲートを文字どおり閉じる。回さない判断ならその旨を deviation.md に記録する
2. 蒸留で `kasane/handbook/cross/test-execution.md` の件数表を本 change の実測 (ios 275 / instrumented 333 / maui 153) へ更新する
3. (任意) iOS の 2 レジストリの class doc から ADR ID を落として 3 形態の方向をそろえる。同梱しないなら drift へ回す
