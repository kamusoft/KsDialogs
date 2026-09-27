# レビュー結果: fix-android-container-system-bar-appearance (001 回目)

**日付**: 2026-09-27
**判定**: NEEDS_DISCUSSION
**一致検証 (M 級のため兼務)**: VALID

## サマリー

Loading / Toast の器への引き継ぎの追加、明暗の読み取りへの旧来のフラグの追加、作法 0 を写さない扱い、テストの組み立て直し (比べる相手は、テストが提示先に与えた指定から組み立てた期待値。提示先の前提を先に確かめる。PB-SB-11 は終端条件の合意を待つ) は、デルタスペックと tasks どおりに実装されている。Scenario の対応表に欠落は無い。

ただ、明暗の読み取りで旧来のフラグを OR で足す実装 (`hostAppearance`) には問題がある。テーマ (`windowLightStatusBar`) が付けた旧来のフラグと、WindowInsetsController で明示した「暗い地向け」が食い違う提示先では、OS は旧来のフラグを無視する。この実装は逆に器を明るい地向けにしてしまう。修正前は Dialog が正しく扱えていたケースなので、この change で入る退行になる。API 30〜34 では公開 API だけでは見分けがつかず、どちらかのケースを捨てる設計判断が要る。このため判定は NEEDS_DISCUSSION とする。

## 照合した規約

- comment-policy.md (常時) — 追加・変更したコメントを節ごとに照合した (許容参照・禁止参照・禁止記述類型・公開 doc コメント)。`--advisory` で出る要確認は、テストクラスと `internal object` のメンバーへの誤検知で、ライブラリの公開面ではない
- ci-flaky-test-policy.md (適用のきっかけ: `android/**/src/androidTest/**`・`ios/Tests/**` の、状態遷移を観測するテスト) — 終端状態の合意を待つ (PB-SB-11 の `awaitHostOwnsStatusBar`・`hideHostBars`)、「起きない」を履歴で確かめる (`BarVisibilityRecorder`)、正規の印以外の無効化手段が無い (`assumeTrue` は API レベルの前提の表現で、対象外)。`ci-skip-lint.py` も確認した: 印 0 件
- test-execution.md (適用のきっかけ: テストの実行・結果の報告) — 「API レベルで走る / 走らない Scenario」の節と照合した (後述の Suggestion 5)
- lessons/code-review.md の L-001 (推奨する検証手順が、証明したい命題の真偽で結果が分かれるか) — 指摘 1 の推奨テストに適用した

## テスト実行

- レビュアーが実行したもの: iOS の対象 3 スイート (`DialogStatusBarAppearanceTests`・`LoadingStatusBarAppearanceTests`・`ToastStatusBarAppearanceTests`) を専用シミュレータで実行し、6 tests 全件成功 (PB-IA-03・PB-SB-08・LD-SB-01/02・TS-SB-01/02)。`scenario-id-coverage.py --require-mirror`・`comment-policy-lint.py`・`ci-skip-lint.py`・`local-path-lint.py`・`identity-lint.py` もすべて通過
- Android の Gradle は、kmp / maui のビルドと build ディレクトリを取り合うため回していない (コンテキストパッケージの制約)。Android の結果は、オーケストレーターが確認した客観的事実を採った: JVM 75 件 0 失敗、instrumented は API 35 / API 31 とも :ksdialogs-core 359 件 0 失敗 1 skip (PB_SB_04) と :ksdialogs 52 件 0 失敗、API 36 はシステムバー関連の 5 クラスで 19 件 0 失敗 1 skip。修正前の本体で新テストが落ちること (API 35 で 9 件中 7 件、API 31 で 9 件中 8 件) も `evidence/impl-probe-appearance-behavior.txt` に残っている

## 指摘事項

### [🟠 Major] 旧来のフラグを OR で足すと、テーマの旧来のフラグと WindowInsetsController の明示的な「暗い地向け」が食い違う提示先で、器が明るい地向けになる

**該当箇所**: `android/ksdialogs-core/src/main/kotlin/jp/kamusoft/ksdialogs/DialogWindowSystemBars.kt:152-166` (`hostAppearance`)

**問題点**:
OS は、アプリが WindowInsetsController で明暗を指定したウィンドウ (appearance controlled) では、旧来のフラグ (`SYSTEM_UI_FLAG_LIGHT_STATUS_BAR` / `SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR`) を無視する。SDK の sources で確認した根拠は次のとおり。
- `ViewRootImpl.adjustLayoutParamsForCompatibility`: API 31 では `PRIVATE_FLAG_APPEARANCE_CONTROLLED` が立っていないときだけ、API 35 ではビットごとの `appearanceControlled` が立っていないビットだけ、旧来のフラグから明暗を作る
- `PhoneWindow`: テーマの `windowLightStatusBar` / `windowLightNavigationBar` を、API 30〜35 のどれでも旧来のフラグとして decor に立てる (API 35 は `setSystemBarsAppearanceFromResource` と並べて、「旧来の方法でも読めるように」立て続ける)

このため、次の提示先では実際の見えは暗い地向け (白いアイコン) なのに、`hostAppearance` は `windowSystemUiVisibility` に残ったテーマ由来のフラグを OR する。結果、器を明るい地向け (黒いアイコン) にしてしまう。
- テーマが `windowLightStatusBar=true` の画面で、プラットフォームの `window.insetsController.setSystemBarsAppearance(0, APPEARANCE_LIGHT_STATUS_BARS)` を呼んで暗い地向けにしている (暗い画面・動画や画像のビューアなど)

API 35 では `getSystemBarsAppearance()` が `(host & controlled) | (fromResource & ~controlled)` を返すので、このビットは 0 になる。旧来のフラグは立っているので、OR で明るい地向けになる。API 31〜33 も、controlled ならアプリが指定した 0 を返し、同じ結果になる。

修正前の Dialog は OS の返す値 (0) だけを写していたので、このケースを正しく保っていた。つまりこの change で入る退行で、ADR-0039 の「器は提示先のシステムバーの指定を変えない」に反する。androidx の `WindowInsetsControllerCompat` は明暗を変えるときに旧来のフラグも立て外しするので、androidx 経由で指定するアプリは影響を受けない。影響を受けるのは、プラットフォームの API を直接使うアプリになる。proposal の「リスク」にもこのケースは書かれていない。今のテストもこのケースを含まないので、退行は検出されない。

公開 API だけでは完全には解けない。API 31〜33 の `getSystemBarsAppearance()` は、controlled でないウィンドウにも 0 を返す。このため「テーマが明るい地向けで、コードでは何も指定していない (旧来のフラグが効く。よくあるケース)」と「テーマが明るい地向けで、コードで暗い地向けを明示した (旧来のフラグは無視される)」を見分けられない。今の実装は前者 (よくあるケース) を取っており、API 30〜34 ではその判断に一理ある。一方 API 35 以降は、テーマ由来の明暗が `fromResource` として OS の返す値に含まれるので、見分けがつく (下の選択肢 B)。

**推奨修正 (選択肢。設計判断が要るのでオーナーに諮る)**:
- **A. 現状の読み取りのまま、捨てるケースを記録する**: コードのコメント (`hostAppearance` の KDoc) と、proposal Impact のリスク相当の記録 (deviation.md、または蒸留時の ADR-0039 Consequences) に、「テーマの旧来のフラグと WindowInsetsController の明示的な暗い地向けが食い違う提示先では、器が明るい地向けになる」を残す
- **B. API 35 以降だけ見分ける (推奨)**: 提示先のテーマの `android.R.attr.windowLightStatusBar` / `windowLightNavigationBar` を読み、テーマが立てているビットは、旧来のフラグから足さない。API 35 以降ではテーマ由来の明暗が OS の返す値に含まれるので、OS の値を正とする。コードで `setSystemUiVisibility` した旧来のフラグ (PB-SB-09 の本来の対象) は、テーマに無いビットなので従来どおり足される。API 30〜34 は A と同じく記録して受け入れる
- **C. 非公開の属性 (`WindowManager.LayoutParams.insetsFlags` / `privateFlags`) を reflection で読む**: 全 API で正確に見分けられるが、非公開 API への依存になるので推奨しない

B を選ぶなら、次のテストを足す。提示先に旧来のフラグを立てたうえで、WindowInsetsController で `setSystemBarsAppearance(0, LIGHT_BARS)` を明示し、器の明暗が 0 であることを確かめる (`SystemBarsTestActivity.Appearance` に値を 1 つ足せば作れる)。L-001 の照合: 今の OR の実装ではこのテストは API 35 で落ち、B の実装では通る。つまり命題 (テーマ由来と明示の食い違いで明示を採る) の真偽で結果が分かれる。A を選ぶ場合、このテストは API 30〜34 で必ず落ちるので置かない (置くなら API 35 以降に `assumeTrue` で絞る)。

### [🟡 Minor] 作法 0 を写さない理由のコメントが Android 11 について不正確

**該当箇所**: `android/ksdialogs-core/src/main/kotlin/jp/kamusoft/ksdialogs/DialogWindowSystemBars.kt:114-118`

**問題点**: 「少なくとも Android 11〜13 は、指定の無いウィンドウに 0 を返す」に続けて、「そのまま写すと…提示先の既定の作法 (スワイプで出す) とずれる」と書いている。しかし Android 11 (API 30) の既定の作法は 0 (触れたら出す) そのもので、ずれは起きない (`evidence/impl-probe-appearance-behavior.txt` の「API 30 は 0 が既定そのものなのでずれない」と食い違う)。このファイルだけを読む人が、API 30 にも補正が要ると読み違える。
**推奨修正**: 「0 を返す」の範囲 (Android 11〜13) と、「ずれる」の範囲 (既定の作法が別の値として定義された Android 12〜13) を分けて書く。例: 「Android 11〜13 は指定の無いウィンドウに 0 を返す。Android 12 以降は既定の作法が 0 とは別の値なので、そのまま写すと…ずれる。Android 11 は 0 が既定そのものなので、写さなくても結果は変わらない」。

### [🔵 Suggestion] 既存の PB_SB_07 の最初のアサーションが、提示先から OS が返す値と比べている

**該当箇所**: `android/ksdialogs-core/src/androidTest/kotlin/jp/kamusoft/ksdialogs/DialogSystemBarsTests.kt:237-238` (diff の外。MODIFIED Requirement の既存 Scenario)
**問題点**: `assertEquals("提示先の既定の作法が引き継がれていない", readHostBehavior(), adopted)` は、android-native デルタの「テストの比べ方」が避けるとした形 (提示先から OS が返す値と比べる) のまま残っている。API 31〜33 では、作法を指定していない提示先も器もアプリから読むと 0 なので、たまたま一致して通っているだけで、「器が OS の既定になった」ことは確かめていない (それは PB-SB-10 が dumpsys で確かめている)。追随しないことを見る本体 (2 つ目のアサーション) は有効。
**推奨修正**: 必須ではない。揃えるなら、最初のアサーションを「前提: 表示時の器の作法を控える」だけにするか、PB-SB-10 と同じくウィンドウ管理の値 (`bhv=`) で比べる。

### [🔵 Suggestion] iOS テストの最後の `#expect` は、失敗し得ない

**該当箇所**: `ios/Tests/KsDialogsTests/DialogStatusBarAppearanceTests.swift` (PB-SB-08 の最後の `#expect`)・`ios/Tests/KsDialogsTests/LoadingStatusBarAppearanceTests.swift:24,34`・`ios/Tests/KsDialogsTests/ToastStatusBarAppearanceTests.swift:27,37`
**問題点**: `presenter.preferredStatusBarStyle == .darkContent` と `presenter.prefersStatusBarHidden` は、テスト用の画面が定数を返す override なので、器が何をしても変わらない。判定力を持つのは、見えを決める画面をたどる観測 (`StatusBarOwnershipObservation` と `modalPresentationCapturesStatusBarAppearance` / `childForStatusBarStyle`) のほうで、この `#expect` は検証しているように読めるだけになっている。
**推奨修正**: 削除するか、「前提の再確認」であることが分かる書き方にする (前提は冒頭の `#require` で確かめ済み)。

### [🔵 Suggestion] 蒸留時に、test-execution.md の「API 依存の assumeTrue はすべて API 30 を境にしている」を更新する

**該当箇所**: `kasane/handbook/cross/test-execution.md:84` (実装の変更対象ではない)
**問題点**: PB_SB_10 と、LD_SB_01 / TS_SB_01 の作法の確認は、API 31 (`Build.VERSION_CODES.S`) を境にしている。handbook の記述と食い違う。tasks.md 7 の申し送り (「PB-SB-10 の API の絞り方を足すか」) で扱える範囲なので、実装の修正は要らない。
**推奨修正**: 蒸留で handbook を更新するときに、この 1 文も直す。

### [🔵 Suggestion] Dialog と Loading / Toast で、引き継ぎを呼ぶ仕組みが 2 通りある

**該当箇所**: `android/ksdialogs-core/src/main/kotlin/jp/kamusoft/ksdialogs/DialogContainer.kt:99-103`・`DialogWindowSystemBars.kt:95-106`
**問題点**: Dialog は、取り外しの受け皿を兼ねる常駐のリスナーから、載るたびに `inheritSystemBarState` を呼ぶ。Loading / Toast は、新しく足した `inheritSystemBarStateWhenAttached` (1 回で外れるリスナー) を使う。Loading / Toast の器は載せ直しのたびに作り直されるので (`LoadingCoordinator.bringToFrontWithoutPresentation`・`onHostChanged`)、1 回で外れる形で挙動上の問題は無い。ただ、「3 つの器が同じ引き継ぎを通す」(`DialogWindowSystemBars` の KDoc) を守る入口が 2 つになっている。
**推奨修正**: 任意。器を増やすときに入口を 1 つにしておくと、写し漏れ (ADR-0039 の Consequences にある負の面) を防ぎやすい。今回の範囲では対応不要。

## 一致検証 (デルタスペックの対応表)

| Requirement / Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| android-native MODIFIED: システムバー表示状態の引き継ぎ — Dialog / Loading / Toast の器が引き継ぐ | `DialogContainer.kt:102`・`LoadingContainer.kt:101`・`ToastContainer.kt:101`・`DialogWindowSystemBars.kt:74-128` | 下記の各 Scenario | ✅ 一致 |
| 〃 表示時の 1 回で追随しない (Loading / Toast も) | `DialogWindowSystemBars.kt:95-106` (載った時点で 1 回。外れるリスナー) | Dialog は PB-SB-06 / 07。Loading / Toast は載せ替えのテスト (`LoadingSystemBarsTests.kt:161`・`ToastSystemBarsTests.kt:148`) で、作り直し後の画面から読むことを確かめている | ✅ 一致 |
| 〃 Toast はフォーカスを取らず、写しても提示先のバーは変わらない | `ToastContainer.kt:101` | `ToastSystemBarsTests.kt:94` (TS_SB_02、提示先の見えを履歴で確認) | ✅ 一致 |
| [PB-SB-01] 全バー非表示を引き継ぐ (API 30+) | `DialogWindowSystemBars.kt:119-127` | `DialogSystemBarsTests.kt:52` | ✅ 一致 (既存。API 31 / 35 / 36 で成功) |
| [PB-SB-02] ステータスバーのみ非表示 | 同上 | `DialogSystemBarsTests.kt:88` | ✅ 一致 (既存) |
| [PB-SB-03] ナビゲーションバーのみ非表示 | 同上 | `DialogSystemBarsTests.kt:113` | ✅ 一致 (既存) |
| [PB-SB-04] 旧経路 (API 24〜29) | `DialogWindowSystemBars.kt:80-84` | `DialogSystemBarsTests.kt:138` | ✅ 一致 (テストは存在。API 29 の端末は proposal の Non-Goals と tasks 6.1 のとおり未実行) |
| [PB-SB-05] 通常表示では従来どおり | `DialogWindowSystemBars.kt:125-127` | `DialogSystemBarsTests.kt:173` | ✅ 一致 (既存) |
| [PB-SB-06] 可視状態の変更に追随しない | `DialogContainer.kt:99-103` (載った時点だけ) | `DialogSystemBarsTests.kt:204` | ✅ 一致 (既存) |
| [PB-SB-07] behavior の変更に追随しない | 同上 | `DialogSystemBarsTests.kt:232` | ✅ 一致 (既存。比べ方は Suggestion 参照) |
| [PB-SB-09] 旧来のフラグだけで指定した明暗を引き継ぐ (API 30+) | `DialogWindowSystemBars.kt:113,152-166` | `DialogSystemBarAppearanceTests.kt:65` | ✅ 一致 (食い違うケースの退行は指摘 1。Scenario 自体は満たしている) |
| [PB-SB-10] 作法を指定していない画面では OS の既定 (API 30+) | `DialogWindowSystemBars.kt:119-122` | `DialogSystemBarAppearanceTests.kt:101` (`assumeTrue(SDK >= S)`、dumpsys の `bhv=` で判定) | ⚠️ deviation 記録済み (API の絞り方と観測方法) |
| android-native ADDED: 透明な覆いでステータスバーが暗くならない / [PB-SB-11] | `DialogWindowSystemBars.kt:40-59` (既存の作り) | `DialogTransparentOverlayTests.kt:60` (明るい地向けの前提と、終端条件 3 つの合意を待ってから測る。成り立たなければ失敗させる)。同じ前提を既定の覆いの裏取りにも適用 | ✅ 一致 |
| dialog-contract ADDED: 非干渉 (Dialog) / [PB-SB-08] Android | `DialogContainer.kt:102`・`DialogWindowSystemBars.kt:113` | `DialogSystemBarAppearanceTests.kt:36` (ナビゲーションバーの明暗も確認) | ✅ 一致 |
| 〃 [PB-SB-08] iOS | 変更なし (`ios/Sources/KsDialogs/Presentation/DialogContainerViewController.swift:136` の overFullScreen で、制御を奪わない) | `ios/Tests/KsDialogsTests/DialogStatusBarAppearanceTests.swift:49` | ✅ 一致 |
| loading-contract ADDED: 非干渉 (Loading) / [LD-SB-01] Android | `LoadingContainer.kt:101` | `LoadingSystemBarsTests.kt:47` (明暗・可視状態・作法)。載せ替えは `:161` | ✅ 一致 |
| 〃 [LD-SB-01] iOS | 変更なし (`ios/Sources/KsDialogs/Presentation/LoadingPresentationSurface.swift:16`) | `ios/Tests/KsDialogsTests/LoadingStatusBarAppearanceTests.swift:18` | ✅ 一致 |
| 〃 [LD-SB-02] Android | `LoadingContainer.kt:101` | `LoadingSystemBarsTests.kt:99` (提示先の見えの履歴・器側の可視状態・作法) | ✅ 一致 |
| 〃 [LD-SB-02] iOS | 変更なし | `ios/Tests/KsDialogsTests/LoadingStatusBarAppearanceTests.swift:28` | ✅ 一致 |
| toast-contract ADDED: 非干渉 (Toast) / [TS-SB-01] Android | `ToastContainer.kt:101` | `ToastSystemBarsTests.kt:46`。載せ替えは `:148` | ✅ 一致 |
| 〃 [TS-SB-01] iOS | 変更なし (`ios/Sources/KsDialogs/Presentation/ToastPresentationSurface.swift:16`) | `ios/Tests/KsDialogsTests/ToastStatusBarAppearanceTests.swift:21` | ✅ 一致 |
| 〃 [TS-SB-02] Android | `ToastContainer.kt:101` | `ToastSystemBarsTests.kt:94` | ✅ 一致 |
| 〃 [TS-SB-02] iOS | 変更なし | `ios/Tests/KsDialogsTests/ToastStatusBarAppearanceTests.swift:31` | ✅ 一致 |

パスは、特記の無いものは `android/ksdialogs-core/src/main/kotlin/jp/kamusoft/ksdialogs/` と `android/ksdialogs-core/src/androidTest/kotlin/jp/kamusoft/ksdialogs/` からの相対。

追加検査:
- tasks.md: チェック済みの 1.1〜5.2・6.3 は、どれも対応する実装・テスト・証跡がある。1.3 / 1.4 の実測は `evidence/impl-probe-appearance-behavior.txt`、2.3 は載せ替えのテスト 2 件、6.3 はレビュアーの lint 実行で確認した。虚偽チェックは無い。未チェックの 6.1 / 6.2 は実施中 (コンテキストパッケージのとおり、指摘しない)
- 逆流: proposal.md と specs/ 配下 4 本は HEAD (d1396b0) から変更されていない (`git status` / `git diff HEAD`)。tasks.md の差分はチェックボックスだけ
- 未記録の乖離: なし。PB-SB-10 の API の絞り方と、専用端末の使用は deviation.md に記録済み
- 付随修正: deviation.md に `[付随修正]` の行は無い。diff の中で Scenario に対応しない変更は、`DialogContainer.kt` のコメント 1 行 (ADR-0039 への参照の追加) だけで、tasks.md 冒頭の「該当箇所に ADR-0039 のコメントを残す」の範囲に収まる
- 網羅検査: `MIRROR_AREAS` に `("LD", "SB")` と `("TS", "SB")` が入り、`--require-mirror` が通る。PB-SB-08 は両 Native に揃っている (dialog-contract デルタが「レビューで確かめる」とした点)

一致検証の判定は VALID。全 Scenario が「✅ 一致」か「⚠️ deviation 記録済み」で、虚偽チェック・逆流・テスト失敗は無い。指摘 1 は Scenario に書かれていないケース (テーマの旧来のフラグと明示的な指定が食い違う提示先) についての Requirement 本文 (「提示先の画面のシステムバーの指定を変えない」) との食い違いなので、一致検証ではなくレビューの指摘として扱う。

## 確認した観点 (指摘に至らなかったもの)

- 載せ替えの経路: Loading の前面化 (`bringToFrontWithoutPresentation`) と提示先の入れ替わり (`onHostChanged`) は、どちらも器を作り直す。このため、1 回で外れるリスナーでも載せ替え先から写す
- ラムダの中の `context` は `Window.getContext()` に解決されるが、Dialog のテーマのラッパーからたどって同じ Activity に届くので、結果は同じ
- Toast はフォーカスを取らないので可視状態を写しても効かないが、害も無いことを TS_SB_02 が提示先の見えの履歴で確かめている
- Kotlin: `!!` を使っていない。コルーチンは、テストの監視を `runBlocking` の子で起動し、`cancelAndJoin` で終えている (失敗時は親の取り消しで止まる)。リソースリークは無い。本体の変更に性能の問題は無い
- Swift: 1 ファイル 1 型、`@MainActor` の付け方、強制アンラップが無いこと
- テストの前提確認: 各テストが、比べる前に提示先の指定を `assertEquals` / `#require` で確かめている (白地に白いアイコン同士のような、差の出ない比較で通らない)

## アクションプラン

1. **(要判断) 指摘 1**: 選択肢 A / B / C をオーナーに諮る。推奨は B (API 35 以降はテーマの属性で見分け、API 30〜34 は記録して受け入れる)。B なら、食い違うケースのテストを API 35 以降に絞って足す。A なら、コメントと記録を足す
2. 指摘 2 (Minor): 作法のコメントの API の範囲を直す
3. Suggestion 3〜6 は任意。5 は蒸留時の申し送りに含める
