# レビュー結果: fix-android-container-system-bar-appearance (003 回目)

**日付**: 2026-09-27
**判定**: APPROVED
**一致検証 (M 級のため兼務)**: VALID

## サマリー

前回の指摘 2 件 (review-002 の Minor と、second-opinion-code-002 の Minor) は、どちらも解消している。

- 明暗の読み取り: API 35 以上では、テーマが明るい地向けにしているビットを「OS の値」と「旧来のフラグ」の AND で読むようにした。前回の推奨 a のとおり。判定力のあるテスト (L-001) も 1 件足した
- Toast の後始末: 表示と計時を丸ごと捨てるようにした

deviation.md の 3 項目め (明暗の読み取り規則) は書き直されており、実装の分岐・ビット演算・KDoc と 1 対 1 で照合できた。本体に足された `ToastCoordinator.discardAll()` は、公開 API に出ず、既存の撤去の経路をそのまま再利用しているので妥当と判断した。

Critical / Major / Minor は無い。新しい指摘は Suggestion 1 件だけで、記録の網羅に関するもの。deviation の「受け入れる取りこぼし」が代表例しか挙げておらず、同じ仕組みで起きる取りこぼしの一群を名指ししていない。蒸留で ADR-0039 の Consequences に書くときに扱えばよく、コードの修正は要らない。

## 照合した規約

- comment-policy.md (常時) — 今回変わったコメントを節ごとに照合した (許容参照・禁止参照・禁止記述類型・公開 doc コメント)
  - 対象: `hostAppearance` の KDoc・`discardAll` の KDoc・新しいテストの KDoc・`SystemBarsTestActivity.Appearance` の新しい値
  - 外部識別子は `core/ADR-0039` だけで、どれも `internal` の型かテストコードの中にある
  - `comment-policy-lint.py` の結果は 1136 ファイル中で禁止 0 件
- ci-flaky-test-policy.md (適用のきっかけ: 状態遷移を観測するテスト) — 次の節を照合した
  - 前提の置き方: 新しいテストは API 35 以上に `assumeTrue` で絞っている。これは API レベルの前提の表現で、skip の印ではない
  - 正規の印以外の無効化手段: 無い。`ci-skip-lint.py` の結果は印 0 件
  - 後始末: `withdraw` は判定を弱めない
- test-execution.md (適用のきっかけ: テストの実行・結果の報告) — 次の 3 点を照合した
  - Swift Testing の件数は 2 系統ある
  - 絞り込み実行のあとは件数が 1 以上であることを見る
  - 「API レベルで走る / 走らない Scenario」
- lessons/code-review.md の L-001 — 新しい PB-SB-09 のテーマのテストと、指摘 1 の扱いに適用した
- lessons/process.md の L-003 — deviation.md の全項目を実装と照合した (下の表)

## テスト実行

- レビュアーが実行した iOS: 対象 3 スイート (`DialogStatusBarAppearanceTests`・`LoadingStatusBarAppearanceTests`・`ToastStatusBarAppearanceTests`) を専用シミュレータ (id=166DB6E4-…) で実行した
  - `Test run with 6 tests in 3 suites passed`。XCTest の `Executed 0` は想定どおり (Swift Testing のみ)
  - iOS の差分はサイクル 2 から変わっていない
- レビュアーが実行した lint: `comment-policy-lint.py`・`identity-lint.py`・`local-path-lint.py`・`ci-skip-lint.py`・`secret-scan.sh`・`scenario-id-coverage.py` の既定 / `--require-mirror` / `--selftest` を回し、すべて通過した
  - 既定の実行で出る「見出しに無い ID」(PB-SB-07 など) は、デルタの本文にある相互参照で、未網羅ではない
- 回していないもの: Android の Gradle。バックグラウンドの再実行と build ディレクトリを取り合うため、コンテキストパッケージの制約に従った。Android の結果は `evidence/test-run-summary.md` の値を採った
  - API 35 / 31 は全件 0 failures。skip は、API 35 が PB_SB_04 の 1 件、API 31 が PB_SB_04 とテーマのテスト 2 本の計 3 件
  - API 36 はシステムバー関連の 21 件 × 3 回連続成功
  - JVM は 75 件
- 未完了: kmp・maui・MAUI 互換面 Android の再実行 (tasks 6.1)。コンテキストパッケージのとおり指摘しない
  - 所見: `evidence/test-run-summary.md` は見出しで「修正サイクル 3 の後」と言い、kmp / maui の行も載っている。再実行が終わったら、これらの行が今回の実行の値であることを確かめてから 6.1 をチェックすること

## 前回の指摘の解消確認

| 前回の指摘 (出典) | 対応 | 確認 |
|---|---|---|
| [Minor] API 35 以降で、テーマの明るい地向けを旧来のフラグの代入で外した提示先では、器が明るい地向けになる (review-002 指摘 1) | 推奨 a を採った。<br>・`DialogWindowSystemBars.kt:178-184`: API 35 以上では、テーマのビットに入っていないビットを `(reported or legacy)`、テーマのビットを `reported and legacy` で読む<br>・KDoc の `:153-171` を実態に合わせて書き直し、受け入れる取りこぼしを 2 つ明記した<br>・テスト `DialogSystemBarAppearanceTests.kt:140-170`: API 35 以上に絞る。前提として、旧来のフラグが 0 であることと、OS がテーマ由来の明るい地向けを返していることの 2 つを確かめる<br>・提示先の状態は `SystemBarsTestActivity.kt:48-53,106-110` | ✅ 解消。<br>L-001: サイクル 2 の実装 (テーマのビットは OS の値だけ) に戻すと、API 35 / 36 で `expected:<0> but was:<24>` で落ち、今の実装では通る (オーケストレーターの確認を採った)。<br>android-35 の sources でも裏を取った: `InsetsController.getSystemBarsAppearance` は明示の無いビットに `mAppearanceFromResource` を返す。`ViewRootImpl.adjustLayoutParamsForCompatibility` は明示の無いビットの見えを旧来のフラグから作る。`PhoneWindow` はテーマの明暗を旧来のフラグにも立てる。<br>サイクル 2 で足したテスト (`:102-132`、コードで暗い地向けを明示) も `0 and 1 = 0` で引き続き成り立つ |
| [Minor] Android テストで器を外した後も、Toast の計時と表示が続き、終わった画面を握り得る (second-opinion-code-002) | `ToastCoordinator.kt:229-237` に `discardAll()` を足した。`ToastSystemBarsTests.kt:246-257` の `withdraw` がこれを UI スレッドで呼び、`displayCount == 0` を確かめる。3 つのテストすべての `finally` で使っている (`:95,153,203`) | ✅ 解消。器・表示・計時がそろって消え、それを確認するアサーションもある。本体への追加としての妥当性は次の節 |
| [Suggestion] test-execution.md の API 境界の記述 (review-001 からの持ち越し。蒸留へ申し送り) | 実装の対象外 | — (蒸留で扱う。今回、API 35 境界のテストが 2 本になった) |

## 本体に足された `ToastCoordinator.discardAll()` の妥当性

| 観点 | 確認 |
|---|---|
| 公開 API に出ないか | ✅ `ToastCoordinator` は `internal class` なので、`discardAll` はモジュールの外 (`:ksdialogs`・MAUI の橋渡し・KMP) から見えない。同じクラスには、テストと内部からの読み取り用の `displayCount`・`presentedContainers` などが既に並んでいる。テストから使う入口を internal に置く作法は、既存の型と一致する |
| 既存の表示/撤去の経路と矛盾しないか | ✅ 中身は既存の `discard` (計時の取り消し → 器を演出なしで外す → 表示リストから外して参照を手放す) を、表示中のすべてに回すだけ。新しい状態遷移は持ち込んでいない。<br>・最後の 1 枚が消えると、`removeDisplay` が提示先の購読も外す<br>・期限による撤去 (`finish`) の最中に呼ばれた場合: `timerJob.cancel()` で `finish` のコルーチンが止まり、`discard` の側で撤去が完結する<br>・`removeDisplay` は 2 回呼ばれても害が無い<br>・受理済みでまだ UI スレッドに届いていない表示は対象外 (KDoc の「表示中のすべて」と一致する)。テストでは表示状態を待ってから呼ぶので影響しない<br>・テストは既定のシングルトンではなく、ハーネス専用の coordinator に対して呼んでいるので、ほかのテストの状態に触れない |
| 同梱として収まるか | ✅ スコープ外の不具合を直したものではなく、tasks 3.5 のテスト (TS_SB_*) を成り立たせるための支えなので、`[付随修正]` の記録は要らない。仮に同梱条件で測っても、次のとおりすべて満たす<br>① 本務で触る能力 (Toast) の中<br>② 公開 API・スキーマ・ADR に触れない<br>③ 1 ファイル・10 行で、新しい抽象は無い<br>④ `withdraw` のアサーションで担保されている<br>⑤ ユーザーの選択を要する分岐が無い |

## deviation.md の照合 (lessons/process.md L-003)

| 項目 | 実装・証跡との対応 | 照合 |
|---|---|---|
| 1. 実行端末 (この change 専用の AVD と iOS シミュレータ) | コードの差分は無い。`evidence/test-run-summary.md` の端末名 (ksn_sbfix_api35 / 31 / 36、ksn-sbfix-ios) と、レビュアーが使ったシミュレータ id が一致する。API 36 の AVD は記録に無いが、CI と同じ API での追加の確認なので、tasks の手順から外れる差分ではない | ✅ |
| 2. PB-SB-10 の API の絞り方 (`assumeTrue(SDK_INT >= 31)`、`bhv=` で観測) | `DialogSystemBarAppearanceTests.kt:179-203` (今回は行番号だけ移った)、`support/SystemBarsObservation.windowManagerBehavior` | ✅ |
| 3. 明暗の読み取り規則 (今回書き直した項) | 条文ごとに照合した。<br>・「API 30〜34 は OR」→ `DialogWindowSystemBars.kt:178-180` (`SDK_INT < VANILLA_ICE_CREAM` なら `reported or legacyAppearance`。API 30 未満は呼び出し元の分岐 `:78` で来ない)<br>・「テーマが明るい地向けにしていないビットは、どちらかが立っていれば明るい地向け」→ `:182` の `(reported or legacyAppearance) and themeBits.inv()`<br>・「テーマが明るい地向けにしているビットは、両方が立っているときだけ」→ `:183` の `reported and legacyAppearance and themeBits`<br>・テーマの見分け → `:203-223` (`windowLightStatusBar` / `windowLightNavigationBar` を読み、`recycle` まで閉じる)<br>・受け入れる取りこぼし 2 つ → KDoc の `:164-166` (API 35 以上) と `:167-171` (API 30〜34)<br>・テストの 2 本 → 提示先の 2 状態 (`LIGHT_THEME_DARK_BY_CONTROLLER` / `LIGHT_THEME_LEGACY_FLAGS_CLEARED`) と対応する | ✅ 記録どおり。記録に無い分岐は無い。ただし取りこぼしの列挙は代表例にとどまる (指摘 1) |
| 4. `immersive_mode_confirmations` の一時切り替え (LD_SB_02・TS_SB_02・PB_SB_01〜03・06) | `whileSuppressed` を使っているのは `DialogSystemBarsTests.kt:56,95,123,217`・`LoadingSystemBarsTests.kt:103`・`ToastSystemBarsTests.kt:104` で、記録の範囲とちょうど一致する。今回の変更はここに触れていない。オーケストレーターの確認では、3 台ともテストの前後で値が同じだった | ✅ |

## 指摘事項

### [🔵 Suggestion] 「受け入れる取りこぼし」を、代表例ではなく仕組みで書く (蒸留時に ADR-0039 の Consequences へ)

**該当箇所**: `deviation.md` の 3 項目め (「受け入れる取りこぼし:」以下)、`android/ksdialogs-core/src/main/kotlin/jp/kamusoft/ksdialogs/DialogWindowSystemBars.kt:159-160,164-171`

**問題点**:
今の規則が読み違えるのは、次の 1 つの仕組みで起きる場合全体になる。「OS が旧来のフラグを見えに使わなくなったビットでも、器は旧来のフラグを明るい地向けとして足す」。deviation と KDoc は、この一群のうち 1 つずつしか挙げていない。

SDK の sources で確かめた OS の挙動は次のとおり。
- API 31 / 33: `ViewRootInsetsControllerHost.setSystemBarsAppearance` は、渡した mask によらず、ウィンドウに `PRIVATE_FLAG_APPEARANCE_CONTROLLED` を立てる
- API 31 / 33: `adjustLayoutParamsForCompatibility` は、このフラグが立ったウィンドウでは旧来のフラグを見えに反映しない (ウィンドウ単位)
- API 35: 同じ判定がビット単位になった (`appearanceControlled` の各ビット)

このため、次の提示先は、どちらも「明示が勝って見えは暗い地向け、器は明るい地向け」になる。
- API 35 以上: テーマが明るい地向けにしていないビットで、コードが旧来の明るいフラグを立て、プラットフォームの WindowInsetsController で暗い地向けを明示した提示先。`:182` が `0 or 1 = 1` と読む
- API 30〜34: プラットフォームの WindowInsetsController で明暗を明示したあとは、テーマ由来でもコード由来でも、残っている旧来の明るいフラグは見えに効かない。deviation に書かれた「テーマが明るい地向けで、controller から暗い地向けを明示した提示先」は、この一例

どれも旧来の API とプラットフォームの API を食い違う向きに併用した画面に限られ、影響は小さい。修正前 (Dialog のみ) にも同じ食い違いはあった。androidx の WindowInsetsControllerCompat 経由の切り替えは、明示と旧来のフラグを同じ向きにそろえるので、切り替えたビットについてはこの形にならない。

一方で KDoc の `:159-160` (「テーマ由来の値は無いので、OS の値と旧来のフラグのどちらかが立っていれば明るい地向けと読む」) は、明示と旧来のフラグが食い違う場合に触れていない。今の書き方だと、API 35 以上の読み違いは `:164-166` の 1 ケースだけだと読める。

**推奨修正**: コードの変更は要らない。蒸留で ADR-0039 を accepted にするときに、Consequences の取りこぼしを「プラットフォームの WindowInsetsController での明示と、旧来の明るいフラグが食い違う提示先では、器は明るい地向けに倒れることがある (API 30〜34 はウィンドウ単位、API 35 以上はテーマ外のビット単位)」という仕組みの形で書く。今の 2 つのケースは、その例として添える。KDoc の `:159-160` にも同じ趣旨の 1 文を足すと、記録と実装の照合が閉じる。これは蒸留後の手入れでもよい。

L-001 の照合: この指摘は証跡や受け入れ条件を推奨しないので、判定力の問いは当たらない。取りこぼしの存在は、上の sources の 2 箇所 (明示のフラグの立て方と、旧来のフラグの反映条件) だけから導ける。テストは推奨しない。

## 一致検証 (デルタスペックの対応表)

パスは、特記の無いものは `android/ksdialogs-core/src/main/kotlin/jp/kamusoft/ksdialogs/` (実装) と `android/ksdialogs-core/src/androidTest/kotlin/jp/kamusoft/ksdialogs/` (テスト) からの相対。

| Requirement / Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| android-native MODIFIED: システムバー表示状態の引き継ぎ (3 つの器、載った時点で 1 回) | `DialogContainer.kt:100-103`・`LoadingContainer.kt:101`・`ToastContainer.kt:101` → `DialogWindowSystemBars.kt:74-129` | 下の各 Scenario | ✅ 一致 |
| 〃 載せ替え先の画面から写す | `DialogWindowSystemBars.kt:95-106` | `LoadingSystemBarsTests.kt:165`・`ToastSystemBarsTests.kt:165` | ✅ 一致 |
| 〃 Toast はフォーカスを取らず、写しても提示先のバーは変わらない | `ToastContainer.kt:101` | `ToastSystemBarsTests.kt:101` | ✅ 一致 |
| [PB-SB-01] 全バー非表示を引き継ぐ (API 30+) | `DialogWindowSystemBars.kt:124-128` | `DialogSystemBarsTests.kt:53` | ✅ 一致 |
| [PB-SB-02] ステータスバーのみ非表示 | 同上 | `DialogSystemBarsTests.kt:92` | ✅ 一致 |
| [PB-SB-03] ナビゲーションバーのみ非表示 | 同上 | `DialogSystemBarsTests.kt:120` | ✅ 一致 |
| [PB-SB-04] 旧経路 (API 24〜29) | `DialogWindowSystemBars.kt:80-84` | `DialogSystemBarsTests.kt:148` | ✅ 一致 (テストはある。API 29 の端末は、proposal の Non-Goals と tasks 6.1 のとおり未実行) |
| [PB-SB-05] 通常表示では従来どおり | `DialogWindowSystemBars.kt:126-128` | `DialogSystemBarsTests.kt:183` | ✅ 一致 |
| [PB-SB-06] 可視状態の変更に追随しない | `DialogContainer.kt:100-103` | `DialogSystemBarsTests.kt:214` | ✅ 一致 |
| [PB-SB-07] behavior の変更に追随しない | 同上 | `DialogSystemBarsTests.kt:244` | ✅ 一致 |
| [PB-SB-09] 旧来のフラグだけで指定した明暗を引き継ぐ (API 30+) | `DialogWindowSystemBars.kt:113,174-199` | `DialogSystemBarAppearanceTests.kt:65` (旧来のフラグだけ)・`:103` (テーマ + コードで暗い地向けを明示、API 35 以上)・`:141` (テーマ + 旧来のフラグの代入で外す、API 35 以上) | ✅ 一致 (API による読み分けは ⚠️ deviation 記録済み (3 項目め)) |
| [PB-SB-10] 作法を指定していない画面では OS の既定 (API 30+) | `DialogWindowSystemBars.kt:120-123` | `DialogSystemBarAppearanceTests.kt:179` | ⚠️ deviation 記録済み (2 項目め) |
| android-native ADDED: 透明な覆いでステータスバーが暗くならない / [PB-SB-11] | `DialogWindowSystemBars.kt:40-59` | `DialogTransparentOverlayTests.kt:60` | ✅ 一致 |
| dialog-contract ADDED: 非干渉 (Dialog) / [PB-SB-08] Android | `DialogContainer.kt:102`・`DialogWindowSystemBars.kt:113` | `DialogSystemBarAppearanceTests.kt:36` | ✅ 一致 |
| 〃 [PB-SB-08] iOS | 変更なし | `ios/Tests/KsDialogsTests/DialogStatusBarAppearanceTests.swift:48` | ✅ 一致 (レビュアー実行で成功) |
| loading-contract ADDED: 非干渉 (Loading) / [LD-SB-01] Android | `LoadingContainer.kt:101` | `LoadingSystemBarsTests.kt:48`、載せ替えは `:165` | ✅ 一致 |
| 〃 [LD-SB-01] iOS | 変更なし | `ios/Tests/KsDialogsTests/LoadingStatusBarAppearanceTests.swift:17` | ✅ 一致 (レビュアー実行で成功) |
| 〃 [LD-SB-02] Android | `LoadingContainer.kt:101` | `LoadingSystemBarsTests.kt:100` | ✅ 一致 |
| 〃 [LD-SB-02] iOS | 変更なし | `ios/Tests/KsDialogsTests/LoadingStatusBarAppearanceTests.swift:25` | ✅ 一致 (レビュアー実行で成功) |
| toast-contract ADDED: 非干渉 (Toast) / [TS-SB-01] Android | `ToastContainer.kt:101` | `ToastSystemBarsTests.kt:47`、載せ替えは `:165` | ✅ 一致 |
| 〃 [TS-SB-01] iOS | 変更なし | `ios/Tests/KsDialogsTests/ToastStatusBarAppearanceTests.swift:28` | ✅ 一致 (レビュアー実行で成功) |
| 〃 [TS-SB-02] Android | `ToastContainer.kt:101` | `ToastSystemBarsTests.kt:101` | ✅ 一致 |
| 〃 [TS-SB-02] iOS | 変更なし | `ios/Tests/KsDialogsTests/ToastStatusBarAppearanceTests.swift:36` | ✅ 一致 (レビュアー実行で成功) |

追加検査:
- tasks.md: チェック済みの 1.1〜5.2・6.3 には、どれも対応する実装・テスト・証跡がある。虚偽チェックは無い。未チェックの 6.1 / 6.2 は、コンテキストパッケージのとおり指摘しない
- 逆流: proposal.md・specs/ の 4 本・`kasane/decisions/` は、HEAD (d1396b0) から変更されていない (`git diff HEAD --stat` が空)。tasks.md の差分はチェックボックスの行だけ
- 未記録の乖離: なし。今回の読み取り規則の変更は、deviation.md の 3 項目めに書き直されている
- 付随修正: `[付随修正]` の行は無い。Scenario に対応しない本体の差分は 2 つあり、どちらも記録は要らないと判断した
  - `DialogContainer.kt:100` のコメント 1 行: tasks.md 冒頭の指示の範囲
  - `ToastCoordinator.discardAll()`: tasks 3.5 のテストの支え (上の節)
- 網羅検査: `--require-mirror` が通る

一致検証の判定は VALID。全 Scenario が「✅ 一致」か「⚠️ deviation 記録済み」で、虚偽チェック・逆流・テスト失敗は無い。

## 確認した観点 (指摘に至らなかったもの)

- AND の規則が、よくある提示先を壊していないか (API 35 以上)
  - テーマで明るい地向けにし、コードでは何もしない (Material 系の Light テーマの既定): OS の値 1、旧来のフラグ 1 で、明るい地向けになる
  - この読みは、`PhoneWindow` がテーマの明暗を旧来のフラグにも立てることに依存している。android-35 の `PhoneWindow` は、旧来の読み手のためにあえて立てている (sources のコメントにそう書いてある)
  - `:103` のテストが前提として「テーマ由来の旧来のフラグが残っている」ことを API 35 / 36 で確かめている。将来の OS がこれをやめたら、CI の API 36 でこの前提が落ちて気付ける
  - androidx の WindowInsetsControllerCompat で暗い地向けにする: 0 と 0 で、暗い地向けになる
  - 旧来のフラグの代入で外す: 1 と 0 で、暗い地向けになる (今回の修正の対象)
  - テーマ外のビットを旧来のフラグだけで明るい地向けにする (PB-SB-09 の基本形): `0 or 1` で、明るい地向けになる
- `themeLightBars` について
  - `hostWindow.context` のテーマを読む。これは `PhoneWindow` がテーマの明暗を読むのと同じ Context。夜間モードなどの構成の切り替えは、`obtainStyledAttributes` が今の構成で解決する
  - `TypedArray` は `finally` で `recycle` している。呼ばれるのは API 35 以上で表示するときの 1 回だけなので、性能上の懸念は無い
- 新しい提示先の状態 `LIGHT_THEME_LEGACY_FLAGS_CLEARED` は、`setTheme` を `super.onCreate` より前に呼んでいる。装飾が作られる前にテーマが効くので、テストの前提 (OS がテーマ由来の値を返す) が成り立つ。`values/` と `values-v27/` のテーマの分け方 (明暗の属性は API 27 以上だけに置く) も正しい
- `withdraw` の `finally` 内のアサーションが本来の失敗を上書きするのは、`discardAll` が表示を消せなかった場合に限られる。その場合は後始末の失敗として別に報告すべき事象なので、問題にしない
- Kotlin
  - `!!` を使っていない
  - `discardAll` は `displays.toList()` の複製を回すので、`discard` の中でリストを変更しても安全
  - ビット演算は `Int` の `and` / `or` / `inv()` で、マスク (`APPEARANCE_MASK`) の外のビットは `setSystemBarsAppearance` の mask で落ちる
- Swift: 今回の変更は無い。サイクル 2 の確認 (1 ファイル 1 型、`@MainActor`、強制アンラップ無し) から変わっていない

## アクションプラン

1. tasks 6.1: kmp・maui・MAUI 互換面 Android の再実行が 0 failures で終わったことを確かめ、`evidence/test-run-summary.md` のそれらの行が今回の実行の値であることを確認してからチェックする。6.2 もチェックする
2. 蒸留: 指摘 1 (Suggestion) を ADR-0039 の Consequences に反映する (取りこぼしを仕組みの形で書く)。あわせて、持ち越しの test-execution.md の API 境界の記述を更新する (API 31 境界の PB_SB_10・LD/TS_SB_01 の作法の確認と、API 35 境界の PB-SB-09 のテーマのテスト 2 本)
