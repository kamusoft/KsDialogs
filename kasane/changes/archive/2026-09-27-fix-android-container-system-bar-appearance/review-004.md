# レビュー結果: fix-android-container-system-bar-appearance (004 回目)

**日付**: 2026-09-27
**判定**: APPROVED
**一致検証 (M 級のため兼務)**: VALID

## サマリー

前回の指摘は 2 件で、どちらも解消している。
- review-003 の Suggestion: deviation.md の 3 項目めに、取りこぼしの仕組みが書き足された
- second-opinion-code-003 の Minor: iOS の Toast テストの後始末が、表示と期限の計時を残していた。iOS の coordinator に `discardAll()` を足し、既存の `discard` が器も外すようにしたことで解消した

iOS 本体の追加は 2 つ (`ToastCoordinator.discardAll()` と `ToastContainerViewController.removeImmediately()`)。どちらも internal の型の中にあり、公開 API には出ない。既存の `discard` の呼び出し元は、器がまだ無いときにしか呼ばないので、足した 1 行は既存の経路の挙動を変えない。期限切れの撤去 (`finish`) の途中で呼ばれても、撤去が 2 回効くことは無い。Android 側の `discardAll()` とも形がそろっている。

Critical / Major / Minor / Suggestion の新しい指摘は無い。

## 照合した規約

- comment-policy.md (常時): 今回変わったコメントを節ごとに照合した (許容参照・禁止参照・禁止記述類型・公開 doc コメント)
  - 対象: `discardAll` / `discard` / `removeImmediately` の doc コメントと、テストの doc コメント・行コメント
  - 外部識別子は含まない。時間軸の記述も、デルタスペックの構文キーワードも無い
  - どれも internal の型か、テストコードの中にある
  - `comment-policy-lint.py` の結果: 1136 ファイル中、禁止 0 件
- ci-flaky-test-policy.md (適用のきっかけ: 状態遷移を観測するテスト): 次の 2 点を照合した
  - 後始末: 判定を弱めていない。後始末のアサーション (`displayCount == 0`・器が外れていること) は増えた
  - 無効化: 正規の印以外の手段は無い。`ci-skip-lint.py` の結果は印 0 件
- test-execution.md (適用のきっかけ: テストの実行・結果の報告): 次の 2 点を照合した
  - Swift Testing と XCTest の 2 系統の件数を分けて見た
  - 絞り込み実行のあとに、件数が 1 以上であることを確かめた
- lessons/code-review.md の L-001: 新しい後始末のアサーションが、修正の有無で結果が分かれるかを確かめた (下の「前回の指摘の解消確認」の表)
- lessons/process.md の L-003: deviation.md の全項目を実装と照合した (下の表)
- lessons/process.md の L-001 (姉妹面): 後始末の口を、iOS と Android で照合した (下の「iOS 本体の追加の妥当性」の表)

## テスト実行

- iOS の全件 (レビュアーが実行): 専用シミュレータ (id=166DB6E4-…、ksn-sbfix-ios) で `xcodebuild test -scheme KsDialogs` を実行した
  - Swift Testing: `Test run with 321 tests in 56 suites passed`
  - XCTest: `Executed 0` で、想定どおり
  - `evidence/test-run-summary.md` の値と一致する
- iOS の反復 (レビュアーが実行): `-only-testing:KsDialogsTests/ToastStatusBarAppearanceTests -test-iterations 5` を実行した
  - `Test run with 10 tests in 5 suites passed` (2 件 × 5 回)
- lint (レビュアーが実行): 次をすべて通過した
  - `comment-policy-lint.py`・`identity-lint.py`・`local-path-lint.py`・`ci-skip-lint.py`・`secret-scan.sh`
  - `scenario-id-coverage.py` の `--require-mirror` と `--selftest`
- 回していないもの: Android・kmp・maui
  - Android: エミュレータがもう無い。サイクル 3 の後は変わっていないので、`evidence/test-run-summary.md` の値を採った
  - kmp・maui: 同じく `evidence/test-run-summary.md` の値を採った。kmp の iOS (85 件) と MAUI 互換面の iOS (9 件) は、iOS の修正の後にも再実行されたと記録されている。iOS 本体に触れたのでこの 2 つには影響し得るが、そのどちらも再実行済みになっている

## 前回の指摘の解消確認

| 前回の指摘 (出典) | 対応 | 確認 |
|---|---|---|
| [Minor] iOS の Toast テストの後始末が器を `dismiss()` するだけで、coordinator の表示と期限の計時を最大 30 秒残す (second-opinion-code-003) | 次の 3 か所を変えた。<br>・`ios/Sources/KsDialogs/Presentation/ToastCoordinator.swift:217-221`: `discardAll()` を足した<br>・`ToastCoordinator.swift:228`: 既存の `discard` が、取り付き済みの器を `removeImmediately()` で外すようにした<br>・`ios/Tests/KsDialogsTests/ToastStatusBarAppearanceTests.swift:64,83-85`: 観測の後に `discardAll()` を呼び、`displayCount == 0` と器が外れたこと (`container.view.window == nil`) を確かめる。途中で打ち切られた回も `defer` で同じ後始末を通る | ✅ 解消。<br>L-001 の照合 (アサーションが修正の有無で分かれるか):<br>・`:228` の 1 行を外すと、表示は消えるが器は取り付け先に残り、`:85` が落ちる<br>・`discardAll` を呼ばないと `:84` が落ちる<br>後始末の失敗は、`:88-91` の期限の判定より前にアサーションとして報告される。このため、ステータスバーの判定の失敗とは区別して読める |
| [Suggestion] 「受け入れる取りこぼし」を代表例ではなく仕組みで書く (review-003) | `deviation.md` の 3 項目めに「受け入れる取りこぼしの仕組み」が足された。OS が旧来のフラグを見えに使わなくなる単位 (API 31 / 33 はウィンドウ単位、API 35 はビット単位) と、器の読み取り規則とのずれとして書かれ、例も添えてある | ✅ deviation 側は解消。<br>`DialogWindowSystemBars.kt:159-160` の KDoc に同じ趣旨の 1 文を足す作業は残っている。review-003 で「蒸留後の手入れでもよい」とした部分なので、指摘にはせず、ADR-0039 の Consequences と一緒に蒸留へ申し送る |
| [Suggestion] test-execution.md の API 境界の記述 (review-001 からの持ち越し) | 実装の対象外 | — (蒸留で扱う) |

## iOS 本体の追加の妥当性

| 観点 | 確認 |
|---|---|
| 公開 API に出ないか | ✅ `ToastCoordinator` と `ToastContainerViewController` はどちらも `public` の無い `final class` (internal) で、足したメソッドにも修飾子は無い。<br>・モジュールの外 (MAUI の橋渡しの `maui/macios/native`・KMP) からは見えない。リポジトリ全体を検索しても、参照はテストの 1 ファイルだけ<br>・同じクラスには、テストと内部から読むための `displayCount`・`presentedContainers` が既に並んでいる。テストから使う入口を internal に置く作法は、既存の型と一致する |
| 既存の `discard` の経路を変えないか | ✅ `discard` の既存の呼び出し元は `attachIfPossible` の期限切れの分岐 (`ToastCoordinator.swift:177-179`) だけ。ここは `:176` の `display.container == nil` を満たしたときにしか来ないので、`:228` の `container?.removeImmediately()` は何もしない。既存の経路の挙動は変わらない |
| 期限切れの撤去 (`finish`) と重なったとき | ✅ `finish` が `await container.dismiss()` の途中 (出の演出中) に `discardAll()` が走った場合を追った。<br>・`discard` は次の順に処理する: `isFinishing` を立てる → `timerTask` を取り消す → `removeImmediately()` で器を外す → 参照を手放す → 表示リストから外す<br>・その後 `dismiss()` の続きが走っても、何も起きない。`releaseContentHost()` は `contentHost` が nil なので何もせず、`removeFromSuperview()` もすでに外れた View に対しては何もしない<br>・`finish` の続きの処理 (`releaseResources()`・`removeAll`・`stopWaitingForHostIfSatisfied()`) は、2 回呼ばれても害が無い<br>・`finish` が先に終わっていれば、表示リストは空なので、`discardAll()` は何もしない<br>doc コメントの「撤去は 1 回分しか効かない」(`:215-216`)、「残りの処理は外し終えた器に対して何もしない」(`ToastContainerViewController.swift:159-160`) と一致する |
| 入りの演出・添付値待ちと重なったとき | ✅ `removeImmediately()` は `isRemoved` を立てて `presentationTask` を取り消す。このため、外した後に固定の処理や入りの演出が動き出すことは無い。<br>・添付値待ちの `Task` (`:196`) は `isRemoved` を見て抜ける<br>・`settleLayoutSnapshot()` (`:213`) も `isRemoved` を見る<br>・`beginPresentation` の `Task` (`:268`) も、取り消しと `isRemoved` の両方を見る<br>`removeImmediately()` の後に `dismiss()` が呼ばれても、`:145` のガードで何もしない |
| 走査中にリストを書き換えても安全か | ✅ `for display in displays` は配列の値のコピーを回すので、`discard` の中の `removeAll` と干渉しない。Android 側の `displays.toList()` と同じ効果になる |
| Android の同じ指摘への対応と釣り合うか | ✅ 形がそろっている。<br>・Android の `discardAll()` (`android/ksdialogs-core/src/main/kotlin/jp/kamusoft/ksdialogs/ToastCoordinator.kt:235-237`) は、既存の `discard` を表示中のすべてに回すだけ。その `discard` はもともと器を外していた (`detachForReattach`)<br>・iOS の `discard` は器を外していなかったので、`:228` の 1 行でそろえた<br>・後始末で確かめる内容も同じ: Android は `ToastSystemBarsTests.kt:246-257` の `withdraw` で `displayCount == 0`、iOS はそれに加えて器が外れたことも確かめる<br>・呼ぶスレッドの約束: Android は KDoc で「UI スレッドで呼ぶ」と書き、iOS は `@MainActor` の型で強制される |
| 同梱として収まるか | ✅ tasks 4.3 (TS-SB-01 / TS-SB-02 の iOS テスト) を支えるためのもので、スコープ外の不具合を直したものではない。review-003 の Android 側と同じ理由で、`[付随修正]` の記録は要らない。仮に同梱条件で測っても、次のとおりすべて満たす<br>① Toast の能力の中<br>② 公開 API・スキーマ・ADR に触れない<br>③ 2 ファイル・約 20 行で、新しい型も抽象も無い<br>④ テストのアサーションで担保されている<br>⑤ ユーザーの選択を要する分岐が無い |
| Swift | ✅ 次を確かめた。<br>・新しい型を足していない (1 ファイル 1 型は保たれている)<br>・強制アンラップ・`try!` を使っていない<br>・`@MainActor` の型の中の同期メソッドなので、データ競合の心配は無い<br>・テストも `@MainActor` の suite から呼んでいる |

## deviation.md の照合 (lessons/process.md L-003)

今回のサイクルは iOS のテストの後始末だけを動かした。deviation.md に書かれた仕組み (順序・待ち・復元・分岐の位置) には触れていない。

| 項目 | 実装・証跡との対応 | 照合 |
|---|---|---|
| 1. 実行端末 (この change 専用の AVD と iOS シミュレータ) | `evidence/test-run-summary.md` に書かれた端末名 (ksn_sbfix_api35 / 31 / 36、ksn-sbfix-ios) と、レビュアーが使ったシミュレータ (ksn-sbfix-ios、id=166DB6E4-…) が一致する。Android のエミュレータはすでに消されていて、「作業後に削除する」と一致する。シミュレータは、この review のためにまだ残っている | ✅ |
| 2. PB-SB-10 の API の絞り方 (`assumeTrue(SDK_INT >= 31)`、`bhv=` で観測) | `DialogSystemBarAppearanceTests.kt:179` と `support/SystemBarsObservation.kt`。サイクル 3 から変わっていない | ✅ |
| 3. 明暗の読み取り規則と、受け入れる取りこぼし (今回、仕組みの記述が足された) | 条文ごとに照合した。<br>・「API 30〜34 は OR」→ `DialogWindowSystemBars.kt:178-180`<br>・「テーマが明るい地向けにしていないビットは、どちらかが立っていれば明るい地向け」→ `:182`<br>・「テーマが明るい地向けにしているビットは、両方が立っているときだけ」→ `:183`<br>・足された仕組みの例 (「API 35 以上で、テーマ外のビットに旧来の明るいフラグを立てつつ、WindowInsetsController で暗い地向けを明示した提示先」) は、`:182` で `0 or 1 = 1` になり、器は明るい地向けと読む。記録どおりの取りこぼしになる<br>・代表例の 2 つは、KDoc の `:164-166` と `:167-171` にある | ✅ 記録どおり。<br>記録には「取りこぼしは `hostAppearance` の KDoc に残す」とある。KDoc にあるのは代表例の 2 つで、仕組みの 1 文はまだ無い。これは review-003 で蒸留後に回してよいとした部分なので、申し送りにとどめる |
| 4. `immersive_mode_confirmations` の一時切り替え | `whileSuppressed` を使う範囲 (`DialogSystemBarsTests.kt`・`LoadingSystemBarsTests.kt`・`ToastSystemBarsTests.kt`) は、サイクル 3 から変わっていない | ✅ |

## 指摘事項

なし。

## 一致検証 (デルタスペックの対応表)

パスは、特記の無いものは次のディレクトリからの相対。
- 実装: `android/ksdialogs-core/src/main/kotlin/jp/kamusoft/ksdialogs/`
- テスト: `android/ksdialogs-core/src/androidTest/kotlin/jp/kamusoft/ksdialogs/`

Android の実装とテストはサイクル 3 から変わっていない。行番号はこの review で取り直した。

| Requirement / Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| android-native MODIFIED: システムバー表示状態の引き継ぎ (3 つの器、載った時点で 1 回) | `DialogContainer.kt:102`・`LoadingContainer.kt:101`・`ToastContainer.kt:101` → `DialogWindowSystemBars.kt:74-129` | 下の各 Scenario | ✅ 一致 |
| 〃 載せ替え先の画面から写す | `DialogWindowSystemBars.kt:95-106` | `LoadingSystemBarsTests.kt:165`・`ToastSystemBarsTests.kt:165` | ✅ 一致 |
| 〃 Toast はフォーカスを取らず、写しても提示先のバーは変わらない | `ToastContainer.kt:101` | `ToastSystemBarsTests.kt:101` | ✅ 一致 |
| [PB-SB-01] 全バー非表示を引き継ぐ | `DialogWindowSystemBars.kt:124-128` | `DialogSystemBarsTests.kt:53` | ✅ 一致 |
| [PB-SB-02] ステータスバーのみ非表示 | 同上 | `DialogSystemBarsTests.kt:92` | ✅ 一致 |
| [PB-SB-03] ナビゲーションバーのみ非表示 | 同上 | `DialogSystemBarsTests.kt:120` | ✅ 一致 |
| [PB-SB-04] 旧経路 (API 24〜29) | `DialogWindowSystemBars.kt:80-84` | `DialogSystemBarsTests.kt:148` | ✅ 一致。テストはあるが、API 29 の端末では回していない (proposal の Non-Goals と tasks 6.1 のとおり) |
| [PB-SB-05] 通常表示では従来どおり | `DialogWindowSystemBars.kt:126-128` | `DialogSystemBarsTests.kt:183` | ✅ 一致 |
| [PB-SB-06] 可視状態の変更に追随しない | `DialogContainer.kt:100-103` | `DialogSystemBarsTests.kt:214` | ✅ 一致 |
| [PB-SB-07] behavior の変更に追随しない | 同上 | `DialogSystemBarsTests.kt:244` | ✅ 一致 |
| [PB-SB-09] 旧来のフラグだけで指定した明暗を引き継ぐ | `DialogWindowSystemBars.kt:113,174-184` | `DialogSystemBarAppearanceTests.kt:65`・`:103`・`:141` | ✅ 一致。API による読み分けは ⚠️ deviation 記録済み (3 項目め) |
| [PB-SB-10] 作法を指定していない画面では OS の既定 | `DialogWindowSystemBars.kt:120-123` | `DialogSystemBarAppearanceTests.kt:179` | ⚠️ deviation 記録済み (2 項目め) |
| android-native ADDED: 透明な覆いでステータスバーが暗くならない / [PB-SB-11] | `DialogWindowSystemBars.kt:40-59` | `DialogTransparentOverlayTests.kt:60` | ✅ 一致 |
| dialog-contract ADDED: 非干渉 (Dialog) / [PB-SB-08] Android | `DialogContainer.kt:102`・`DialogWindowSystemBars.kt:113` | `DialogSystemBarAppearanceTests.kt:36` | ✅ 一致 |
| 〃 [PB-SB-08] iOS | 変更なし | `ios/Tests/KsDialogsTests/DialogStatusBarAppearanceTests.swift:48` | ✅ 一致 (レビュアーの実行で成功) |
| loading-contract ADDED: 非干渉 (Loading) / [LD-SB-01] Android | `LoadingContainer.kt:101` | `LoadingSystemBarsTests.kt:48`、載せ替えは `:165` | ✅ 一致 |
| 〃 [LD-SB-01] iOS | 変更なし | `ios/Tests/KsDialogsTests/LoadingStatusBarAppearanceTests.swift:17` | ✅ 一致 (レビュアーの実行で成功) |
| 〃 [LD-SB-02] Android | `LoadingContainer.kt:101` | `LoadingSystemBarsTests.kt:100` | ✅ 一致 |
| 〃 [LD-SB-02] iOS | 変更なし | `ios/Tests/KsDialogsTests/LoadingStatusBarAppearanceTests.swift:25` | ✅ 一致 (レビュアーの実行で成功) |
| toast-contract ADDED: 非干渉 (Toast) / [TS-SB-01] Android | `ToastContainer.kt:101` | `ToastSystemBarsTests.kt:47`、載せ替えは `:165` | ✅ 一致 |
| 〃 [TS-SB-01] iOS | 器の非干渉は変更なし。テストの後始末の口として `ToastCoordinator.discardAll()` を追加 | `ios/Tests/KsDialogsTests/ToastStatusBarAppearanceTests.swift:28` | ✅ 一致 (レビュアーの実行で成功、5 回の反復も成功) |
| 〃 [TS-SB-02] Android | `ToastContainer.kt:101` | `ToastSystemBarsTests.kt:101` | ✅ 一致 |
| 〃 [TS-SB-02] iOS | 同上 | `ios/Tests/KsDialogsTests/ToastStatusBarAppearanceTests.swift:36` | ✅ 一致 (レビュアーの実行で成功、5 回の反復も成功) |

追加検査:
- tasks.md: 1.1〜6.3 がすべてチェック済み。どのタスクにも、対応する実装・テスト・証跡がある。虚偽チェックは無い
  - 6.1: `evidence/test-run-summary.md` に、handbook の全ルート (Android の JVM と instrumented 2 モジュール × API 35 / 31、iOS、kmp の 2 ターゲット、maui の `dotnet test` と互換面 2 つ) の件数がそろっている。「API 31 と API 35 で全件実行、API 29 は対象外」の呼び方と、PB_SB_04 が未実行であることも書かれている。iOS の値 (321 件) は、レビュアーが実行した結果と一致する
  - 6.2: `evidence/after-sample-{menu,basic-dialog,default-loading,default-toast,custom-loading}.png` がある
- 逆流: proposal.md・specs/ の 4 本・`kasane/decisions/` は、HEAD (d1396b0) から変更されていない (`git diff HEAD --stat` が空)。tasks.md の差分は、チェックボックスの行だけ
- 未記録の乖離: なし。今回の iOS 本体の追加はテストの支えで、Scenario の挙動を変えない (上の「同梱として収まるか」)
- 付随修正: `[付随修正]` の行は無い。Scenario に対応しない本体の差分は次の 3 つで、どれも記録は要らないと判断した
  - `DialogContainer.kt:100` のコメント
  - Android の `ToastCoordinator.discardAll()` (review-003 で判断済み)
  - iOS の `discardAll()` / `removeImmediately()` (今回)
- 網羅検査: `--require-mirror` が通る

一致検証の判定は VALID。全 Scenario が「✅ 一致」か「⚠️ deviation 記録済み」で、虚偽チェック・逆流・テスト失敗は無い。

## 確認した観点 (指摘に至らなかったもの)

- テストの `discardAll()` の 2 回呼び出し (`:83` と `defer` の `:64`): 2 回目は、表示リストが空なので何もしない。`defer` は逆順に走るので、`discardAll()` が先、`window.isHidden = true` が後になる。器を外してから window を隠す順序で問題ない
- 期限の判定 (`:88-91`) を後始末の後に置いたこと: 判定に使う `elapsed` と `stillDisplayed` は、後始末の前 (`:79-80`) に取っている。そのため、後始末が判定の入力を変えることは無い
- 利用者の出の演出のフックが完了しない場合: `finish` はフックを待ち続けるが、`discard` は器を外して表示を捨てる。残るのは完了しないフックを待つ `Task` だけ。これは既存の「完了しないフックは撤去させない」性質 (`DialogTransitionRunner.swift:117`) の範囲で、しかも後始末の経路は公開の入口から呼ばれない。問題にしない
- iOS の doc コメントは、Android の KDoc と趣旨がそろっている (公開の入口から呼ばれないこと・検証の後始末に使うこと・期限まで残ると参照を握り続けること)。iOS のほうは、`finish` と重なったときの振る舞いも書いている。Android の `discard` はコルーチンの取り消しで `finish` の続きが走らないので、同じ記述は要らない

## アクションプラン

1. 蒸留に進む。申し送りは次の 3 つ
   - ADR-0039 の Consequences に、取りこぼしを仕組みの形で書く (review-003 の Suggestion。deviation.md の 3 項目めが元になる)
   - `DialogWindowSystemBars.kt:159-160` の KDoc に、同じ趣旨の 1 文を足す (蒸留後の手入れでよい)
   - test-execution.md の API 境界の記述を更新する (API 31 境界の PB_SB_10・LD/TS_SB_01 と、API 35 境界の PB-SB-09 のテーマのテスト 2 本)
2. 作業後、専用シミュレータ ksn-sbfix-ios を消す (deviation.md の 1 項目めのとおり)
