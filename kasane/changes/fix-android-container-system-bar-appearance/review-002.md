# レビュー結果: fix-android-container-system-bar-appearance (002 回目)

**日付**: 2026-09-27
**判定**: APPROVED
**一致検証 (M 級のため兼務)**: VALID

## サマリー

前回の突き合わせ (second-opinion-code-001.md 末尾) で採用・確定した 5 件は、すべて解消している。内訳は Major 2 件 (旧来のフラグの OR 読み、Toast の表示期限)、Minor 1 件、同梱の Suggestion 2 件。deviation.md に足された 2 項目 (選択肢 B・選択肢 a) も、実装と 1 対 1 で照合できた。Critical / Major は無い。

新しい指摘は Minor 1 件だけ。選択肢 B は API 35 以降で「テーマの明暗」と「コードでの明示」を見分けるが、「テーマの明るい地向けを、旧来のフラグを外して暗い地向けにした提示先」では器が明るい地向けになる。これは修正前から同じ結果で、この change で入った退行ではない。ただし `hostAppearance` の KDoc は「API 35 以降は OS の値を正とすればよい」と読める書き方で、この穴に触れていない。直すか記録するかを、蒸留の前に決めてほしい (後述)。

## 照合した規約

- comment-policy.md (常時) — 追加・変更したコメントを節ごとに照合した (許容参照・禁止参照・禁止記述類型・公開 doc コメント)。追加分に出てくる外部識別子は `core/ADR-0039` だけで、どれも `internal` の型・テストコードの中にある。`comment-policy-lint.py` の結果は禁止 0 件
- ci-flaky-test-policy.md (適用のきっかけ: `android/**/src/androidTest/**`・`ios/Tests/**` の、状態遷移を観測するテスト) — 次の節ごとに照合した
  - 終端状態の合意: `hideHostBars` と PB-SB-11 の `awaitHostOwnsStatusBar`
  - 「起きない」ことを履歴で見る: `BarVisibilityRecorder`
  - 正規の印以外の無効化手段: 無い。`assumeTrue` は API レベルの前提の表現なので対象外。`ci-skip-lint.py` の結果は印 0 件
  - 前提の置き方: `ImmersiveModeConfirmation.whileSuppressed` は判定を弱めずに OS の割り込みだけを外す前提の整え方で、skip には当たらない
- test-execution.md (適用のきっかけ: テストの実行・結果の報告) — 次の 3 点を照合した
  - Swift Testing の件数は 2 系統ある (`Test run with N tests` 行と `Executed` 行)
  - 絞り込み実行のあとは件数が 1 以上であることを見る
  - 「API レベルで走る / 走らない Scenario」の節
- lessons/code-review.md の L-001 (推奨する検証手順が、証明したい命題の真偽で結果が分かれるか) — 指摘 1 の推奨テストと、新しい PB-SB-09 のテーマのテストに適用した

## テスト実行

- レビュアーが実行したもの
  - iOS の対象 3 スイート (`DialogStatusBarAppearanceTests`・`LoadingStatusBarAppearanceTests`・`ToastStatusBarAppearanceTests`) を専用シミュレータ (id=166DB6E4-…) で実行した。`Test run with 6 tests in 3 suites passed` で、対象は PB-IA-03・PB-SB-08・LD-SB-01/02・TS-SB-01/02
  - lint: `comment-policy-lint.py` (禁止 0 件)・`identity-lint.py`・`local-path-lint.py`・`ci-skip-lint.py` (印 0 件)・`scenario-id-coverage.py` の既定 / `--require-mirror` / `--selftest` を回し、すべて通過した。既定の実行で出る「見出しに無い ID」の警告は、デルタの本文にある相互参照 (PB-SB-01 など) で、未網羅ではない
- 回していないもの: Android の Gradle。バックグラウンドの再実行と build ディレクトリを取り合うため、コンテキストパッケージの制約に従った。Android の結果は、オーケストレーターが確認した客観的事実を採った
  - API 35: :ksdialogs-core 360 / 0 failures / 1 skipped、:ksdialogs 52 / 0
  - API 31: :ksdialogs-core 360 / 0 / 2 skipped、:ksdialogs 52 / 0
  - API 36: システムバー関連の 5 クラス 20 件 × 3 回連続成功
  - 新しい PB-SB-09 のテーマのテストは、OR の実装に戻すと API 35 / 36 で `expected:<0> but was:<24>` で落ちる
  - iOS の全件: 321 / 0
- 未完了: JVM・kmp・maui・MAUI 互換面 Android は再実行中 (tasks 6.1 未チェック)。これが 0 failures で終わることが完了の条件になる

## 前回の指摘の解消確認

| 前回の指摘 (出典・採否) | 対応 | 確認 |
|---|---|---|
| [Major] 旧来のフラグの OR 読みが、テーマの明るい地向け + WindowInsetsController での暗い地向けの明示が食い違う提示先で退行を入れる (ホスト・確定、要判断 → 選択肢 B) | `DialogWindowSystemBars.kt:164-174`: API 35 以上では、テーマの属性 (`themeLightBars`、`:190-212`) が立てたビットを旧来のフラグから引く。API 30〜34 で受け入れるケースは KDoc の `:153-162` に書いた。テストは `DialogSystemBarAppearanceTests.kt:102-132` (API 35 以上に `assumeTrue`。前提として「テーマ由来の旧来のフラグが残っている」「コードで暗い地向けを明示している」の 2 つを確かめてから比べる) | ✅ 解消。L-001: OR の実装では落ち (`expected:<0> but was:<24>`)、B では通るので、命題の真偽で結果が分かれる。SDK の sources (android-35 の `InsetsController.getSystemBarsAppearance`・`PhoneWindow`) でも、テーマ由来の明暗が OS の返す値に含まれることを確かめた。ただし B が扱わない隣のケースが残る (指摘 1) |
| [Major] Toast の表示期限が観測の待機上限より短い (相方・採用) | Android: `ToastSystemBarsTests.kt:275-285` で、期限を「表示待ちの上限 + 作り直しの見込み + 載せ替え待ちの上限 + バーの観測」の 2 倍 (62 秒) にした。判定の前後に `assertStillShown` (`:238-244`) を置いた。後始末は `withdraw` (`:247-249`)。iOS: `ToastStatusBarAppearanceTests.swift:23-26` で期限を 30 秒にした。`:77-92` で、判定の前に「期限前で表示中」を `#require` で確かめ、期限切れを別の失敗として報告する | ✅ 解消。期限切れと退行を別の失敗文で区別できる。器を残さない後始末もある。Loading は期限を持たないので対象外 (`LoadingStatusBarAppearanceTests.swift:36` に明記) |
| [Minor] 作法 0 のコメントが Android 11 について不正確 (ホスト・確定) | `DialogWindowSystemBars.kt:114-119`: 「0 を返す」範囲 (Android 11〜13) と、「ずれる」範囲 (既定が別の値の Android 12 以降) を分けて書き、Android 11 は結果が変わらないと明記した | ✅ 解消。`evidence/impl-probe-appearance-behavior.txt` の「API 30 は 0 が既定そのものなのでずれない」と一致する |
| [Suggestion・同梱] PB_SB_07 の最初のアサーションが、提示先から OS が返す値と比べている (ホスト・確定) | `DialogSystemBarsTests.kt:249-252`: アサーションを消して「表示時の器の作法を控える」だけにし、その理由 (器が OS の既定を採ることは PB-SB-10 がウィンドウ管理の値で確かめている) をコメントに残した | ✅ 解消。追随しないことを見る本体のアサーションは残っている |
| [Suggestion・同梱] iOS テストの最後の `#expect` が失敗し得ない (ホスト・確定) | 3 ファイルとも、提示元の定数の override を読む `#expect` を削除した。前提は冒頭の `#require` に残した | ✅ 解消 |
| [Suggestion] test-execution.md の API 30 境界の記述 (ホスト・蒸留へ申し送り) | 実装の対象外 | — (蒸留で扱う) |
| [Suggestion] 引き継ぎの入口が 2 通り (ホスト・見送り) | 見送り | — |

## deviation.md の照合 (lessons/process.md L-003)

| 項目 | 実装・証跡との対応 | 照合 |
|---|---|---|
| 実行端末 (この change 専用の AVD 2 台と iOS シミュレータを使い、作業後に削除) | コードの差分は無い。テスト実行の事実 (専用 AVD の API 35 / 31、専用シミュレータ) と食い違わない | ✅ |
| PB-SB-10 の API の絞り方 (`assumeTrue(SDK_INT >= 31)`、作法は `dumpsys window windows` の `bhv=` で観測) | `DialogSystemBarAppearanceTests.kt:141-165`、`SystemBarsObservation.windowManagerBehavior` (`support/SystemBarsObservation.kt:148-155`)。LD_SB_01 / TS_SB_01 の作法の確認も同じ境界 (`LoadingSystemBarsTests.kt:85`・`ToastSystemBarsTests.kt:85`) | ✅ |
| 選択肢 B: API 35 以上はテーマの属性を読み、テーマが立てたビットは旧来のフラグとして足さない。API 30〜34 の食い違いは受け入れてコメントに残す | 分岐は `DialogWindowSystemBars.kt:168-172`、テーマの読み取りは `:190-212` (`obtainStyledAttributes` を `recycle` まで閉じている)、受け入れの記述は KDoc の `:158-162`。テストは `DialogSystemBarAppearanceTests.kt:102-132`、提示先の状態は `SystemBarsTestActivity.kt:41-46,60-63,93-96`、テーマは `androidTest/res/values-v27/themes.xml` | ✅ 記録どおり。記録に無い挙動の追加は無い |
| 選択肢 a: バーを隠すテストの間だけ `immersive_mode_confirmations` を `confirmed` にし、失敗時を含めて元へ戻す。未設定なら削除、値あり (空文字を含む) なら content provider への挿入で書き戻す。適用範囲は LD_SB_02・TS_SB_02・PB_SB_01〜03・06 | `support/ImmersiveModeConfirmation.kt:31-67` (空白を含む元の値は書き換える前に失敗させる `:45-47`)。適用箇所は `DialogSystemBarsTests.kt:56,95,123,217`・`LoadingSystemBarsTests.kt:103`・`ToastSystemBarsTests.kt:104` で、記録の範囲とちょうど一致する | ✅ 過不足なし。バーを隠す操作はほかに PB_SB_04 (API 29 以下の旧経路。`DialogSystemBarsTests.kt:449-451`) にしかなく、API 30 以上では `assumeTrue` で走らない。API 29 以下は対象外のオーナー判断に沿う |

## 指摘事項

### [🟡 Minor] API 35 以降で、テーマの明るい地向けを旧来のフラグの操作で外した提示先では、器が明るい地向けになる (修正前から同じ)

**該当箇所**: `android/ksdialogs-core/src/main/kotlin/jp/kamusoft/ksdialogs/DialogWindowSystemBars.kt:156-157,168-173` (`hostAppearance` とその KDoc)

**問題点**:
android-35 の SDK の sources で確かめた OS の挙動は次のとおり。
- `ViewRootImpl.adjustLayoutParamsForCompatibility`: `appearanceControlled` が立っていないビット (コードで明暗を明示していないビット) では、実際の明暗を旧来のフラグ (`systemUiVisibility`) から作る
- `InsetsController.getSystemBarsAppearance`: 明示していないビットには、テーマ由来の値 (`mAppearanceFromResource`) を返す

つまり、テーマが明るい地向けで、コードが `decorView.systemUiVisibility` を代入し直して明るいフラグを外した提示先はこうなる。旧来の edge-to-edge の書き方で、`systemUiVisibility = SYSTEM_UI_FLAG_LAYOUT_STABLE or SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN` と代入する形がこれに当たる。
- 実際の見え: 暗い地向け (白いアイコン)
- OS が返す値: 明るい地向け
- 今の実装: テーマのビットを旧来のフラグから引いたうえで、OS の値 (明るい地向け) を OR する。結果は明るい地向け

器を出すと、提示先の白いアイコンが黒に変わる。

API 30〜33 では、OS は明示していないウィンドウに 0 を返すので、今の実装は 0 | 0 = 0 で正しい。修正前の Dialog も API 35 では OS の値 (明るい地向け) を写していたので、この change で入った退行ではない。

一方で、MODIFIED Requirement「システムバー表示状態の引き継ぎ」は「提示先の指定のしかたによらず引き継ぐ」「旧来のフラグだけで指定している場合も、その指定を引き継ぐ」と書いており、このケースはその文面に当たる。KDoc の「Android 15 以上: テーマ由来の明暗は OS の返す値に含まれる (明示があれば明示が優先される) ので、テーマが立てたビットは旧来のフラグとして足さない」も、テーマ由来の値が旧来のフラグで上書きされ得ることに触れていない。このため、選択肢 B で API 35 以降は正しく引き継げると読める。前回のレビュー (review-001 の選択肢 B の説明) もこのケースを挙げておらず、オーナーの判断材料から抜けていた。

**推奨修正 (どちらか)**:
- **a. 直す**: API 35 以上では、テーマのビットについては「OS の値」と「旧来のフラグ」の AND を採り、テーマに無いビットについては今のまま OR を採る。明示のあるビットは OS の値が明示どおりになるので、前回のケース (テーマは明るい地向け、コードで暗い地向けを明示) は 0 AND 1 = 0 のまま通る。取りこぼすのは、プラットフォームの API で明るい地向けを明示しながら、旧来のフラグも外した提示先だけになる。androidx の `WindowInsetsControllerCompat` は両方をそろえて切り替えるので、この形にはならない。テストは `SystemBarsTestActivity.Appearance` に「テーマで明るい地向けにし、`systemUiVisibility` の代入で明るいフラグを外す (WindowInsetsController は呼ばない)」を 1 つ足して作る。前提として、提示先の旧来のフラグが 0 であることと、API 35 以上では OS の値が明るい地向けであることを確かめる。そのうえで、器の明暗が 0 であることを見る
  - L-001 の照合: 今の実装では API 35 / 36 で明るい地向けになって落ち、a の実装では通る。命題 (テーマの明暗を旧来のフラグで外した指定を採る) の真偽で結果が分かれる。API 31 ではどちらの実装でも通るので、`assumeTrue` を API 35 以上に絞るか、全 API で回して「API 31 は判定力なし」と KDoc に書く
- **b. 記録して受け入れる**: deviation.md の選択肢 B の項 (または蒸留時の ADR-0039 Consequences) に、このケースで器が明るい地向けになる (修正前と同じ) ことを足す。あわせて `hostAppearance` の KDoc の Android 15 以上の段落を、「テーマ由来のビットは OS の値を採る。旧来のフラグの操作でテーマの明暗を外した画面は取りこぼす」と正確にする

退行ではなく、影響するのは非推奨の `systemUiVisibility` を代入し直している画面に限られる。このため判定は止めない。ただし、この件は Requirement の文面に当たり、KDoc も実態と食い違っている。a / b のどちらにするかを蒸留の前に決めることを勧める。

## 一致検証 (デルタスペックの対応表)

パスは、特記の無いものは `android/ksdialogs-core/src/main/kotlin/jp/kamusoft/ksdialogs/` (実装) と `android/ksdialogs-core/src/androidTest/kotlin/jp/kamusoft/ksdialogs/` (テスト) からの相対。

| Requirement / Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| android-native MODIFIED: システムバー表示状態の引き継ぎ (3 つの器、載った時点で 1 回) | `DialogContainer.kt:100-103`・`LoadingContainer.kt:101`・`ToastContainer.kt:101` → `DialogWindowSystemBars.kt:74-129` | 下の各 Scenario | ✅ 一致 |
| 〃 載せ替え先の画面から写す | `DialogWindowSystemBars.kt:95-106` (載った時点で提示先を求める) | `LoadingSystemBarsTests.kt:165`・`ToastSystemBarsTests.kt:165` (作り直しの前後で異なる明暗を与える) | ✅ 一致 |
| 〃 Toast はフォーカスを取らず、写しても提示先のバーは変わらない | `ToastContainer.kt:101` | `ToastSystemBarsTests.kt:101` (TS_SB_02、提示先の見えを履歴で確認) | ✅ 一致 |
| [PB-SB-01] 全バー非表示を引き継ぐ (API 30+) | `DialogWindowSystemBars.kt:120-128` | `DialogSystemBarsTests.kt:53` | ✅ 一致 (既存。確認ウィンドウは選択肢 a で外す) |
| [PB-SB-02] ステータスバーのみ非表示 | 同上 | `DialogSystemBarsTests.kt:92` | ✅ 一致 |
| [PB-SB-03] ナビゲーションバーのみ非表示 | 同上 | `DialogSystemBarsTests.kt:120` | ✅ 一致 |
| [PB-SB-04] 旧経路 (API 24〜29) | `DialogWindowSystemBars.kt:80-84` | `DialogSystemBarsTests.kt:148` | ✅ 一致 (テストはある。API 29 の端末は proposal の Non-Goals と tasks 6.1 のとおり未実行) |
| [PB-SB-05] 通常表示では従来どおり | `DialogWindowSystemBars.kt:126-128` | `DialogSystemBarsTests.kt:183` | ✅ 一致 |
| [PB-SB-06] 可視状態の変更に追随しない | `DialogContainer.kt:100-103` (載った時点だけ) | `DialogSystemBarsTests.kt:214` | ✅ 一致 |
| [PB-SB-07] behavior の変更に追随しない | 同上 | `DialogSystemBarsTests.kt:244` (前回の Suggestion を反映し、表示時の値を控えるだけにした) | ✅ 一致 |
| [PB-SB-09] 旧来のフラグだけで指定した明暗を引き継ぐ (API 30+) | `DialogWindowSystemBars.kt:113,164-188` | `DialogSystemBarAppearanceTests.kt:65` (旧来のフラグだけ)・`:103` (テーマと明示が食い違う提示先、API 35 以上) | ✅ 一致 (API 35 以降のテーマの扱いは ⚠️ deviation 記録済み (選択肢 B)。隣のケースは指摘 1) |
| [PB-SB-10] 作法を指定していない画面では OS の既定 (API 30+) | `DialogWindowSystemBars.kt:120-123` | `DialogSystemBarAppearanceTests.kt:141` | ⚠️ deviation 記録済み (API の絞り方と観測方法) |
| android-native ADDED: 透明な覆いでステータスバーが暗くならない / [PB-SB-11] | `DialogWindowSystemBars.kt:40-59` (既存の作り) | `DialogTransparentOverlayTests.kt:60` (明るい地向けの前提と、終端条件 3 つの合意。成り立たなければ測らずに失敗させる。既定の覆いの裏取りも同じ前提) | ✅ 一致 |
| dialog-contract ADDED: 非干渉 (Dialog) / [PB-SB-08] Android | `DialogContainer.kt:102`・`DialogWindowSystemBars.kt:113` | `DialogSystemBarAppearanceTests.kt:36` (ナビゲーションバーの明暗も確認) | ✅ 一致 |
| 〃 [PB-SB-08] iOS | 変更なし | `ios/Tests/KsDialogsTests/DialogStatusBarAppearanceTests.swift:48` | ✅ 一致 (レビュアー実行で成功) |
| loading-contract ADDED: 非干渉 (Loading) / [LD-SB-01] Android | `LoadingContainer.kt:101` | `LoadingSystemBarsTests.kt:48` (明暗・可視状態・作法)。載せ替えは `:165` | ✅ 一致 |
| 〃 [LD-SB-01] iOS | 変更なし (`ios/Sources/KsDialogs/Presentation/LoadingPresentationSurface.swift` の `KeyWindowLoadingPresentationSurface`) | `ios/Tests/KsDialogsTests/LoadingStatusBarAppearanceTests.swift:17` | ✅ 一致 (レビュアー実行で成功) |
| 〃 [LD-SB-02] Android | `LoadingContainer.kt:101` | `LoadingSystemBarsTests.kt:100` (提示先の見えの履歴・器側の可視状態・作法) | ✅ 一致 |
| 〃 [LD-SB-02] iOS | 変更なし | `ios/Tests/KsDialogsTests/LoadingStatusBarAppearanceTests.swift:25` | ✅ 一致 (レビュアー実行で成功) |
| toast-contract ADDED: 非干渉 (Toast) / [TS-SB-01] Android | `ToastContainer.kt:101` | `ToastSystemBarsTests.kt:47`。載せ替えは `:165` | ✅ 一致 |
| 〃 [TS-SB-01] iOS | 変更なし (`ios/Sources/KsDialogs/Presentation/ToastPresentationSurface.swift` の `KeyWindowToastPresentationSurface`) | `ios/Tests/KsDialogsTests/ToastStatusBarAppearanceTests.swift:28` | ✅ 一致 (レビュアー実行で成功) |
| 〃 [TS-SB-02] Android | `ToastContainer.kt:101` | `ToastSystemBarsTests.kt:101` | ✅ 一致 |
| 〃 [TS-SB-02] iOS | 変更なし | `ios/Tests/KsDialogsTests/ToastStatusBarAppearanceTests.swift:36` | ✅ 一致 (レビュアー実行で成功) |

追加検査:
- tasks.md: チェック済みの 1.1〜5.2・6.3 には、どれも対応する実装・テスト・証跡がある。虚偽チェックは無い。未チェックの 6.1 (再実行の結果待ち) と 6.2 (撮影済みでチェック待ち) は、コンテキストパッケージのとおり指摘しない。なお 6.2 の証跡 `evidence/after-sample-default-loading.png` と `after-sample-default-toast.png` を見た。Loading / Toast の表示中もステータスバーのアイコンは暗い色で、個人を特定する情報は写っていない
- 逆流: proposal.md・specs/ の 4 本・ADR-0039 は、HEAD (d1396b0) から変更されていない。tasks.md の差分はチェックボックスの 21 行だけ
- 未記録の乖離: なし。API の絞り方・専用端末・選択肢 B・選択肢 a は deviation.md に記録済みで、実装と 1 対 1 で照合できた (上の表)
- 付随修正: `[付随修正]` の行は無い。Scenario に対応しない差分は、`DialogContainer.kt:100` のコメント 1 行 (ADR-0039 の参照) だけで、tasks.md 冒頭の指示の範囲に収まる
- 網羅検査: `MIRROR_AREAS` に `("LD", "SB")` と `("TS", "SB")` が入り、`--require-mirror` が通る。PB-SB-08 は両 Native に揃っている (dialog-contract デルタが「レビューで確かめる」とした点)

一致検証の判定は VALID。全 Scenario が「✅ 一致」か「⚠️ deviation 記録済み」で、虚偽チェック・逆流・テスト失敗は無い。指摘 1 は Scenario に書かれていないケースについて、Requirement の本文と食い違うというもの。前回と同じ扱いで、レビューの指摘として出す。

## 確認した観点 (指摘に至らなかったもの)

- 選択肢 B が既存のケースを壊していないか
  - テーマも旧来のフラグも使わない明示 (PB-SB-08): テーマのビットが 0 なので、OS の値がそのまま採られる
  - 旧来のフラグだけ (PB-SB-09): テーマが明るい地向けでない提示先なので、引く対象が無い
  - テーマだけで明るい地向け: OS の値に含まれる
  - androidx 経由の暗い地向け: OS の値 0 と、旧来のフラグ 0
  - 4 つとも期待どおりになる
- `themeLightBars` は `hostWindow.context` (Activity) のテーマを読む。`TypedArray` は `finally` で `recycle` している。API 35 未満では呼ばれない
- `ImmersiveModeConfirmation`
  - 端末の設定をテストの間だけ書き換える前提の整え方で、判定は弱めていない
  - 元へ戻せない値 (空白を含む) は書き換える前に失敗させる
  - 失敗時も `finally` で戻す
  - 戻した結果が 3 台で元と同じだったことは、オーケストレーターの確認を採った
  - 設定を書き換えたままプロセスが落ちると元へ戻らない。これは instrumented の一般的な限界で、専用端末の運用 (作業後に削除) の範囲に収まる
- Toast の期限: Android の 62 秒は、待ちの上限の合計 (31 秒) の 2 倍。後始末 (`withdraw`) で器を残さない。iOS の 30 秒は、取り付け待ちと落ち着き待ちの上限 (計 10 秒) を大きく超える。期限切れの回は `#require` で別の失敗として報告し、判定の前に器が外れるのを待つ
- 載せ替えのテストの判定力: 作り直し前は明暗なし、作り直し後だけに明るい地向けを与えている。前の器の値や作り直し前の画面を読む実装では落ちる (L-001)
- Kotlin
  - `!!` を使っていない
  - テストのコルーチン (`watcher`) は `runBlocking` の子で起動し、`cancelAndJoin` で終える。途中で失敗しても親の取り消しで止まる
  - 本体の変更に、リソースリークと性能の問題は無い (テーマの読み取りは表示時の 1 回だけ)
- Swift
  - 1 ファイル 1 型
  - `@MainActor` の付け方は適切
  - 強制アンラップは無い
  - `Duration` と `ContinuousClock` で経過を測っている

## アクションプラン

1. **指摘 1 (Minor)**: a (API 35 以上ではテーマのビットを AND で読み、テストを 1 件足す) か、b (deviation.md の選択肢 B の項に記録し、KDoc を正確にする) かをオーナーに諮り、蒸留の前に反映する。判定は止めない
2. tasks 6.1: JVM・kmp・maui・MAUI 互換面 Android の再実行が 0 failures で終わったことを確かめてからチェックする。6.2 のチェックも付ける
3. 蒸留の申し送り (前回からの持ち越し): test-execution.md の「API 依存の assumeTrue はすべて API 30 を境にしている」を更新する。今回の change で、API 31 境界 (PB_SB_10 と、LD/TS_SB_01 の作法の確認) と API 35 境界 (PB-SB-09 のテーマのテスト) の assumeTrue が増えている
