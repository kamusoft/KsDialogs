# レビュー結果: fix-android-instrumented-toast-back-loading-coalescing (1 回目)

**日付**: 2026-09-06
**判定**: APPROVED

## サマリー

androidTest 2 ファイルの観測方法の修正と handbook の実測値更新。どちらの修正も本体コードの実装と突き合わせて原因分析どおりであることを確認でき (`LoadingContainer.runDismissal` の実効値未固定の分岐、予測型バックで `onBackPressed` が呼ばれない経路)、Scenario の前提をゆるめるのではなく成立させる向きの変更になっている。ライブラリ本体は無改変。

指摘は Minor 3 件・Suggestion 2 件で、いずれも修正の正しさではなく**記録の正確さと後始末**に関するもの。判定を妨げるものはない。

## 照合した規約

| 文書 | 適用のきっかけ |
|---|---|
| kasane/handbook/cross/comment-policy.md | 常時 (コメント構文を持つファイルを書くとき) |
| kasane/handbook/cross/test-execution.md | テストの実行・結果の報告・変更の完了判定 (instrumented ルートの件数・skip・Scenario ID 網羅) |
| kasane/handbook/cross/runtime-behavior-verification.md | 実行時挙動 (演出のタイミング・OS の戻る機構) が絡む不具合の修正と完了判定 |

android ドメインの handbook は未整備 (規約なし)。`kasane/lessons/code-review.md` は存在しない (重点観点・指摘しないことの指定なし)。`kasane/lessons/process.md` の L-001 (姉妹面照合) を適用した。

## 実行した検証 (レビュアー独立)

対象 2 クラス (`ToastSystemInputTests` / `LoadingCoalescingTests`、17 本) を、接続中の 4 台のうち 3 台で実行:

| API | 種別 | 結果 |
|---|---|---|
| 29 | エミュレータ | 18/18 完了 (1 skipped) / 0 failed |
| 33 | 実機 | 本レビューでは未実行 (`evidence/after-full-suite-3-devices.txt` の全件実行が 305 / 0 failures を記録) |
| 35 | エミュレータ | 17/17 / 0 failed (**証跡に無い API レベル**) |
| 36 | 実機 | 17/17 / 0 failed |

加えて、修正前に 2/2 で失敗していた API 29 で `LoadingCoalescingTests` を 3 回反復し、15 本すべて成功 (3 回とも)。

handbook の全件実行表のうち、本 diff が触る android/ の 2 ルートを絞り込みなしで実行した (残る ios / kmp / maui / MAUI 互換面の 4 ルートは diff が 1 行も触れないため未実行 — 本レビューは実行していないルートについて成功を主張しない):

| ルート | 端末 | 結果 |
|---|---|---|
| android/ (instrumented) | API 36 実機 | `:ksdialogs` 268 (skipped 1) + `:ksdialogs-compose` 37 = **305 tests / 0 failures / 0 errors** |
| android/ (instrumented) | API 29 エミュレータ | `:ksdialogs` 268 (skipped 6) + `:ksdialogs-compose` 37 = **305 tests / 0 failures / 0 errors** |
| android/ | (JVM) | **67 tests / 0 failures / 0 errors** (`verifyNoDeclarativeUiDependency` を含む) |

件数・failures・skipped のいずれも `kasane/handbook/cross/test-execution.md:23` の更新後の記述と一致する (API 30 以上で skipped 1・API 29 で 6)。API 30 以上の skip が 1 本である根拠も確認した — androidTest の API レベル依存の `assumeTrue` はすべて Android 11 (API 30) を境にしており (`android/ksdialogs/src/androidTest/kotlin/jp/kamusoft/ksdialogs/DialogSystemBarsTests.kt:139` の `< R` と `:304` の `>= R`、`ToastSystemInputTests.kt:44` の `>= R`)、API 30〜32 で内訳が変わる境界は無い。

## 確認した観点 (指摘に至らなかったもの)

**戻るの二重計数がないこと** — `OnBackInvokedDispatcher` は登録済みコールバックのうち最上位の 1 本だけを呼ぶ機構で、コールバックが呼ばれた場合 `onBackPressed` は呼ばれない。逆に予測型バックが無効な環境では登録が届かず従来の `onBackPressed` 経路だけが動く。したがって `ToastInputTestActivity` の 2 経路が同時に発火する経路は無く、コメント (`android/ksdialogs/src/androidTest/kotlin/jp/kamusoft/ksdialogs/support/ToastInputTestActivity.kt:24`) の主張は成立する。実測でも、二重計数があれば `android/ksdialogs/src/androidTest/kotlin/jp/kamusoft/ksdialogs/ToastSystemInputTests.kt:88` の待ち (`== before + 1`) か `同ファイル:104` の `assertEquals` のどちらかが必ず落ちるところ、4 台すべてで成功した。

**検出力が落ちていないこと** — 戻るは focus を持つウィンドウの dispatcher へ配送される。Toast の器が focus を奪う退行が入れば戻るは Activity のコールバックへ届かず、カウントが増えないか (旧来どおり失敗)、器側で既定の戻るが走って Activity が破棄される (修正前と同じ NPE) かのどちらかになる。Scenario が守る性質は維持されている。

**LD_CO_13 が Scenario の意図を検証できていること** — `LoadingContainer` の撤去は実効値が固定済みのときだけ出の演出を走らせる (`android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/LoadingContainer.kt:121`)。`SHOWN` は提示フェーズ完走後にしか立たないので、`SHOWN` を待ってから閉じることで「出の途中」が実際に成立する。観測対象を coordinator の `isDismissing` から器の `DISMISSING` へ移した点も、器の `DISMISSING` は coordinator の撤去進行中を含意するため観測が**狭くなる**方向で、要求を弱めていない。`containerState` は `@Volatile` (`android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/DialogContainer.kt:62`) で別スレッドからのポーリングに耐える。待ち方は `android/ksdialogs/src/androidTest/kotlin/jp/kamusoft/ksdialogs/DialogTransitionAttachmentTests.kt:49` / `:71` の慣例と同形。

**L-001 (姉妹面照合)** — `waitUntil { coordinator.isDismissing }` と同じ待ち方をしている姉妹面は iOS の `ios/Tests/KsDialogsTests/LoadingCoalescingTests.swift:329` (同じ LD-CO-13) だけで、器側にも同じ「実効値が固まる前に閉じると演出を飛ばす」分岐がある (`ios/Sources/KsDialogs/Presentation/LoadingContainerViewController.swift:164`)。ただし iOS は取り付け時に `hostView.layoutIfNeeded()` を同期で回して実効値を固定する (`同ファイル:153` と `loadView` の `onAttachedToWindow`) ため、`show()` が戻った時点で固定済みであり、Android のような「固定前に閉じる」窓が開かない。**姉妹面に同じ穴は無い**と判断した (exploration の初版にあった同趣旨の未決の論点は改訂で消えており、答えが記録されていないため、蒸留時のためにここに残す)。

**実行時挙動の検証規約の 3 条件** — 修正前の再現 2 台 (`evidence/before-pixel6a-api36.txt` / `evidence/before-emulator-api29.txt`)、修正後の同一コマンドでの解消 (`evidence/after-pixel6a-api36.txt`)、証跡の change 配下への保存、いずれも満たしている。証跡はシリアルがプレースホルダ化され抜粋のみ・全文は手元保管の旨も記されており、evidence 規約に適合。

**lint / 検査** — `scripts/comment-policy-lint.py` 禁止 0 件 (本 diff の 2 ファイルに新規の要確認も無し。`android/ksdialogs/src/androidTest/kotlin/jp/kamusoft/ksdialogs/LoadingCoalescingTests.kt:29` の要確認はクラス KDoc の既存項目で本 diff の対象外)、`local-path-lint.py` / `identity-lint.py` 違反 0 件、`doc-structure-lint.py` で `kasane/handbook/cross/test-execution.md` の指摘無し、`scenario-id-coverage.py` の未網羅 60 件はすべて進行中の 2 提案由来で本変更と無関係 (LD-CO-13 の ID はテスト名に残存)。

## 指摘事項

### [🟡 Minor] `LoadingCoordinator.isDismissing` が誰からも読まれなくなる

**該当箇所**: `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/LoadingCoordinator.kt:126`

**問題点**: この修正で LD_CO_13 が `isDismissing` を使わなくなった結果、`isDismissing` の参照はリポジトリ全体 (本体・テスト・compose・maui 互換面) で 0 件になる。宣言は「観察 (テストと内部からの読み取り)」節にあり、読み手が「テストが使っているはず」と誤読する状態で残る。iOS 側の同名プロパティは `ios/Tests/KsDialogsTests/LoadingCoalescingTests.swift:329` から今も読まれており、両ミラーで参照状況がずれる。

**推奨修正**: 本 change では**直さない** — exploration の決定事項「ライブラリ本体は触らない」に反するため。撤去するか観察面として意図的に残すかを別の起票 (簡易起票で足りる) に送り、判断をオーナーへ回す。

### [🟡 Minor] handbook の「222 → 305」修正が合意スコープ外で、deviation に記録がない

**該当箇所**: `kasane/handbook/cross/test-execution.md:80`

**問題点**: exploration の決定事項は「android/ (instrumented) 行の実測値を更新する」であり、`件数表の 222 は 1 台分` (別節の陳腐化した数値) の修正はその外にある。修正内容そのものは明らかに正しく、ksn-core の付随修正の同梱条件 (①同じファイル ②公開 API に触れない ③局所 ④テスト不要 ⑤判断不要) を満たすが、**同梱した付随修正は deviation.md に記録する**のが条件で、本 change には deviation.md が無い。

**推奨修正**: `deviation.md` を作り `[付随修正]` として 1 行記録する (箇所・理由・同梱条件を満たす根拠)。

### [🟡 Minor] 証跡の注記「予測型バックは API 36 固有」が実構成と合わない

**該当箇所**: `evidence/before-emulator-api29.txt:3`

**問題点**: テスト APK の targetSdk は 36 で、androidTest の manifest に `enableOnBackInvokedCallback` の宣言は無い (`android/ksdialogs/src/androidTest/AndroidManifest.xml`)。予測型バックの既定有効化は targetSdk 35 以上のアプリで API 35 の端末から始まるため、「API 36 固有」は成立しない見込みで、少なくとも実測の裏付けが無い。実際に API 35 のエミュレータが接続中だが、修正前・修正後とも証跡には現れていない。証跡は蒸留後も残る記録なので、誤った一般化が残ると次に同種の症状を見たときの切り分けを誤らせる。

**推奨修正**: 注記を実測に即した表現へ直す (例: 「API 29 では 2 件とも成功。予測型バックの経路は API 33 以上でのみ登録され、この構成では API 35 / 36 の端末で有効」)。断定を残したいなら API 35 で修正前の再現を 1 回取る。なお**修正後**の API 35 での成功はこのレビューで確認済み (17/17) なので、修正の妥当性そのものは揺らがない。

### [🔵 Suggestion] 新設した 2 つの待ちに assert のメッセージが無い

**該当箇所**: `android/ksdialogs/src/androidTest/kotlin/jp/kamusoft/ksdialogs/LoadingCoalescingTests.kt:336` / `:345`

**問題点**: どちらも `assertTrue(waitUntil { ... })` の形で、時間切れ時に出るのは素の `java.lang.AssertionError` だけになる。修正前の失敗 (`evidence/before-pixel6a-api36.txt`) がまさに素の AssertionError で、どちらの待ちが切れたかを行番号から逆引きする必要があった。この change 自体が「落ちたときに何が起きたか分からない」ことの解消なので、同じ状態を再生産しないほうがよい。兄弟テストにはメッセージ付きの `assertTrue` (`android/ksdialogs/src/androidTest/kotlin/jp/kamusoft/ksdialogs/ToastSystemInputTests.kt:87` 等) の慣例もある。

**推奨修正**: それぞれに 1 行のメッセージを添える (例: 「入りの演出が終わらない」「出の演出が始まらない」)。

### [🔵 Suggestion] handbook の実測値セルが記録の二重化を生んでいる

**該当箇所**: `kasane/handbook/cross/test-execution.md:23` (と `:17`)

**問題点**: 1 セルに件数・モジュール内訳・API レベル別の skip 数・実測日・端末構成が入り、他の行の 3〜5 倍の長さになっている。skip の内訳は同じ文書の「API レベルで走る / 走らない Scenario」節が扱う話題で、数値を 2 か所に置くと片方だけ陳腐化する。また `:17` の「コマンドと件数は 2026-08-28 に実測した (MAUI iOS 互換面の行だけは … 2026-09-02 の実測)」が、この行だけ 2026-09-06 になったことで実態と食い違う。

**推奨修正**: セルは件数と内訳までに留め、skip の内訳と端末構成は後続節へ寄せる。`:17` の但し書きに android/ (instrumented) 行を足す。

## アクションプラン

1. `deviation.md` に「222 → 305」の付随修正を 1 行記録する (Minor 2)
2. `evidence/before-emulator-api29.txt` の注記を実測に即した表現へ直す (Minor 3)
3. `LoadingCoordinator.isDismissing` の扱いを簡易起票としてオーナーへ回す (Minor 1 — 本 change では直さない)
4. 余力があれば assert メッセージの追加と handbook のセル整理 (Suggestion 1 / 2)
