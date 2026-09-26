# 完了確認の実行記録 (tasks 6.1〜6.3)

実行日: 2026-09-25。コマンドと件数の得方は handbook cross/test-execution.md の表に従った。iOS Simulator は起動中のものを使わず、別の機種を起動して使い、終了後に停止した。Gradle のビルドは逐次で回した。

## 6.1 全ルートの全件実行

| ビルドルート | 実行 | 成功 | 失敗 | skip | 備考 |
|---|---|---|---|---|---|
| ios/ | `xcodebuild test -scheme KsDialogs` (Simulator) | 286 | 0 | 0 | Swift Testing 286 件 / 51 suites。XCTest 側は `Executed 0 tests` |
| android/ (unit + api-surface-check) | `./gradlew test --rerun-tasks` | 68 | 0 | 0 | `verifyNoDeclarativeUiDependency` と `:api-surface-check:compileDebugKotlin` を含む |
| android/ instrumented `:ksdialogs-core` (API 36 実機) | `./gradlew connectedDebugAndroidTest` | 296 | 5 | 1 | 全 302 件。失敗 5 件は既知の UiAutomation 接続エラー (下記 A) |
| android/ instrumented `:ksdialogs` (API 36 実機) | 同上 | 40 | 0 | 0 | LD-HA-02 を含む |
| android/ instrumented `:ksdialogs-core` (API 33 実機) | 同上 | 299 | 2 | 1 | 全 302 件。失敗 2 件は下記 B・C |
| android/ instrumented `:ksdialogs` (API 33 実機) | 同上 | 40 | 0 | 0 | |
| kmp/ | `./gradlew allTests --rerun-tasks` | 166 | 0 | 0 | iosSimulatorArm64 85 + androidHostTest 81 |
| maui/ | `dotnet test` | 172 | 0 | 0 | |
| maui/android/native/ | `./gradlew :ksdialogs-maui-bridge:test --rerun-tasks` | 34 | 0 | 0 | |
| maui/macios/native/ | `xcodebuild test -scheme KsDialogsMauiBridge` (Simulator) | 7 | 0 | 0 | Swift Testing 7 件 / 4 suites |

skip の各 1 件は API 30 以上でだけ判定できる IME の出し入れの `assumeTrue` (test-execution の「API レベルで走る / 走らない Scenario」)。API 29 の端末は今回つないでいないため、旧経路側の実行は無い。

上の表は完了確認の 1 回目の全件実行の件数。失敗 B を受けて Android の報告経路を直した後 (deviation.md の design Decision 3 の項)、さらにレビュー (review-001) の指摘で受理ループの失敗の閉じ込めを足した後に、Android の関係するルートを回し直した。最終の件数は次のとおり (API 31 エミュレータ)。

| ビルドルート | 実行 | 成功 | 失敗 | 備考 |
|---|---|---|---|---|
| android/ (unit + api-surface-check) | `./gradlew test --rerun-tasks` | 68 | 0 | |
| android/ instrumented `:ksdialogs-core` の Loading 系 10 クラス | `connectedDebugAndroidTest` (クラス指定) | 111 | 0 | 新しい順序テスト 1 本と、受け口の失敗を閉じ込めるテスト 1 本を含む |
| android/ instrumented `:ksdialogs` | `connectedDebugAndroidTest` | 40 | 0 | |
| maui/android/native/ | `./gradlew :ksdialogs-maui-bridge:test --rerun-tasks` | 34 | 0 | |

`:ksdialogs-core` の全件は修正後に回し直していない。修正は Loading の報告経路 (`LoadingCoordinator` / `Loading`) に閉じており、Loading 系 10 クラスで回し直した。残る失敗は下記 A (端末側の問題と見られる。変更前のコードとの比較は A の切り分けの最後の項) と C (この change と無関係のテスト補助の競合) で、どちらもこの change の範囲の外。

### 参考: 表示 API の負のコンパイル検査 (6.1 の表の外)

`KsLoading` の署名に触れたため、Loading の表示 API の負の検査を回した。いずれもビルドは失敗し、禁止形状は弾かれている。

| ルート | フラグ | 結果 |
|---|---|---|
| ios/ | `KSDIALOGS_NEGATIVE_CHECK_LOADING_SHOW_OPTIONS` | `extra argument 'options' in call` (handbook どおり) |
| ios/ | `KSDIALOGS_NEGATIVE_CHECK_LOADING_SHOW_STYLE` | `extra argument 'style' in call` (handbook どおり) |
| android/ | `ksdialogs.negativeCheck.loadingShowOptions` | `None of the following candidates is applicable:` の 1 件だけ (handbook は `No parameter with name 'options' found.` を加えた 2 件と記載)。候補の一覧に `options` を取る `show` は無い |
| maui/ | `KsDialogsNegativeCheckLoadingShowOptions` | CS1739 (handbook どおり) |

#### android/ の「show がオーバーロードされているため 2 件出る」行の実測 (付随修正)

実行日: 2026-09-25。handbook が同じ前提 (show のオーバーロードで候補不適合と引数名不明の 2 件が出る) で書いていた android/ の 5 本を、最終コードの作業ツリーで handbook の手順 (`./gradlew :api-surface-check:compileDebugKotlin -P<フラグ> --rerun-tasks`) により 1 本ずつ逐次に回した。5 本ともビルドは失敗し、診断 (`e:` 行) はどれも 1 件だった。

| フラグ | 診断の件数 | 出た診断 | 候補の一覧に出た `show` |
|---|---|---|---|
| `ksdialogs.negativeCheck.loadingShowOptions` | 1 | `None of the following candidates is applicable:` | 4 本。どれも `options` を取らない |
| `ksdialogs.negativeCheck.showOptions` | 1 | `None of the following candidates is applicable:` | 3 本。どれも `options` を取らない |
| `ksdialogs.negativeCheck.showTransition` | 1 | `None of the following candidates is applicable:` | 3 本。どれも `transition` を取らない |
| `ksdialogs.negativeCheck.loadingShowStyle` | 1 | `None of the following candidates is applicable:` | 4 本。どれも `style` を取らない |
| `ksdialogs.negativeCheck.toastShowStyle` | 1 | `None of the following candidates is applicable:` | 4 本。どれも `style` を取らない |

- 5 本とも `No parameter with name '...' found.` は出ない
- 変更前との切り分け: この change の前のコード (コミット b0657a9 の `android/` と `core/`) を作業ツリーの外へ複製し、`loadingShowOptions` を同じ手順で 1 回回した。診断は同じく `None of the following candidates is applicable:` の 1 件だけで、候補の一覧も同じ 4 本だった。1 件になったのはこの change (`KsLoading.start` / `Loading` への `actionThread` 引数の追加) によるものではなく、change の前からそうだった。複製は実行後に削除した
- handbook (cross/test-execution.md の負の検査の表) の android/ の上記 5 行を、実測どおり 1 件の記述に直した。`loadingShowOptions` の行の「同上」も、他の行に依存しない記述にした

### 失敗の内訳

**A. API 36 実機の 5 件 (既知の 5 件と一致)**

| テスト | 例外 |
|---|---|
| `DialogTransparentOverlayTests.既定の覆いではステータスバー領域も覆いの色で暗くなる` | `IllegalStateException: UiAutomationService ... already registered!` |
| `DialogTransparentOverlayTests.覆いが透明ならステータスバーの明るさは表示前後で変わらない` | `IllegalStateException: Not connected!` |
| `ToastMultiDisplayTests.TS_MX_01_多重起動はすべて表示され起動順に重なる` | `IllegalStateException: Not connected!` |
| `ToastMultiDisplayTests.TS_MX_05_Loading_は起動順によらず_Toast_より前面` | `IllegalStateException: Not connected!` |
| `ToastSystemInputTests.Toast_表示中でも戻るとホームが通る` | `IllegalStateException: UiAutomation not connected` (`performGlobalAction`) |

切り分け:

- 同じ 3 クラスだけを API 36 実機で回し直すと、同じ 5 件が同じ例外で落ちる (12 件中 5 件)。`already registered!` に出る登録済みのサービス代理は、全件実行のときと同じ識別値だった — テストの実行をまたいで同じ UiAutomation の登録が端末側に残っている
- その端末には、shell 権限の `app_process` として別ツールのデバイスサーバー (`com.mobilenext.mobilecli.DeviceServer`) が常駐していた。UiAutomation の接続はこれに握られていると見られる (停止はしていない — 他の作業のものかもしれないため)
- 同じ 3 クラスを API 33 実機で回すと 12 件すべて成功した
- 変更前のコードとの比較: Android の実装 (tasks 2) の作業中に、変更前の android/ を作業用の場所へ複製し、API 36 実機で `:ksdialogs-core` の instrumented を回した。同じ 5 件が同じ例外 (`already registered!` / `Not connected!`) で落ちた。この比較の実行は API 36 実機の 1 台だけで、API 33 / 31 では変更前のコードを回していない。ログはこの証跡に保存していない (実装ワーカーの報告による記録)

**B. API 33 実機: `LoadingActionThreadTests.LD_TH_07_UI_スレッド外で始まった_action_からの進捗報告が届く` (この change で足したテスト)**

- 失敗: `AssertionError: 7 回目: 報告は報告した順に届き、最後の報告が終了に追い越されない expected:<[0.25, 0.5, 1.0]> but was:<[]>`
- 同じクラスだけを 10 回ずつ回し直した: API 33 実機 10/10 成功、API 31 エミュレータ 8/10 成功 (失敗 2 回はどちらも `2 回目` / `3 回目` の反復で、受け取った進捗が空 `[]`)
- 落ち方はいつも「3 件の報告がすべて届かない」で、一部だけが欠けた回は無い。報告の受理は合流数が 0 になった後だと捨てられるため、終了が 3 件の報告より先に受理されている
- 実装の見立て: `Loading.endUseAfterPendingReports` は `withContext(NonCancellable + Dispatchers.Main)` で「UI スレッドの列の末尾へ積み直す」ことを前提にしている。しかし呼び出し元がすでに `Dispatchers.Main` の上にいると、`withContext` は同じ dispatcher なら dispatch を省く (同じ `ContinuationInterceptor` の fast path)。さらに UI スレッド外の action が呼び出し元の中断より先に完了すると、`withContext(Dispatchers.Default)` は再 dispatch なしで UI スレッドへ戻る。この 2 つが重なった回は、先に積まれた報告より前に終了がその場で受理される。テストは `withContext(Dispatchers.Main)` から呼んでいるため、この組み合わせに当たる
- 手元で落ちる失敗であり、ci-flaky-test-policy の CI 限定 skip の候補にはならない (実装側で直す対象)
- **その後の修正 (解消済み)**: 上の見立ては、この時点の途中の実装 (`endUseAfterPendingReports` を `withContext(NonCancellable + Dispatchers.Main)` で受理する形) についてのもの。見立てを確かめたうえで、`LoadingCoordinator.report` を「呼んだその場で 1 本の列に積み、UI スレッド上の 1 つの受理コルーチンが順に受理する」形に直し、終了の直前に同じ列の末尾へ区切りを積んで待つ形にした (deviation.md の design Decision 3 の項)。検出力のため LD-TH-07 の 1 回あたりの試行数を 8 から 40 に増やし、既定の指定で UI スレッド外から報告してすぐ戻る経路のテストを足した
- **再実行の結果**
  - 途中の実装 + 同じテスト: API 31 エミュレータで LD-TH-07 は 20 回中 7 回失敗、既定の指定の順序テストは 20 回中 5 回失敗 (検出力の確認)
  - 修正後: LD-TH-07 と既定の指定の順序テストは、API 31 エミュレータ・API 33 実機・API 36 実機でそれぞれ 50 回連続成功
  - 受理ループの失敗の閉じ込め (review-001 の指摘) の後: API 31 エミュレータで両テストとも 20 回連続成功

**C. API 33 実機: `ToastMultiDisplayTests.TS_MX_05_Loading_は起動順によらず_Toast_より前面`**

- 失敗: `ConcurrentModificationException` (`ToastCoordinator.getPresentedContainers` ← `ToastTestHarness.getContainers` ← `waitUntilPresenting`)
- テストの待ち合わせ (`InstrumentedDialogWaiting.waitUntil`) が `runBlocking` のスレッドから Toast の状態の列を読み、UI スレッドが同じ列を書き換えている間に走査が重なった。この change で触れていないテスト補助と Toast の実装の間の競合
- 同じクラスを API 33 実機で回し直した回は成功した (12 件中 12 件)

### 最終コードでの全件実行

実行日: 2026-09-25。B の修正 (報告経路の 2 回の修正) とテスト 2 本の追加 (`LoadingActionThreadTests` の既定の指定の順序テスト、`LoadingProgressTests` の受け口の失敗を閉じ込めるテスト) が入った作業ツリーで、`:ksdialogs-core` と `:ksdialogs` の instrumented 全件を API 33 実機 1 台で 1 回ずつ回した (`./gradlew :ksdialogs-core:connectedDebugAndroidTest :ksdialogs:connectedDebugAndroidTest`、端末を 1 台に絞って実行)。

| モジュール | 端末 | 全件 | 成功 | 失敗 | skip |
|---|---|---|---|---|---|
| `:ksdialogs-core` | API 33 実機 | 304 | 303 | 0 | 1 |
| `:ksdialogs` | API 33 実機 | 40 | 40 | 0 | 0 |

- `:ksdialogs-core` の全件は、前回の 302 件から追加 2 本の分だけ増えた 304 件。結果 XML で、`LoadingActionThreadTests` 9 件・`LoadingProgressTests` 8 件がすべて実行され、すべて成功していることを確かめた
- 失敗は無い。前回 API 33 で出た B (`LD_TH_07`) と C (`TS_MX_05` の `ConcurrentModificationException`) は、この回では出なかった
- skip の 1 件は `DialogSystemBarsTests.PB_SB_04_旧経路でも非表示状態が維持される` (旧経路で、API 29 でだけ判定できる)。上の表の注記で「IME の出し入れ」としたのは誤りで、API 30 以上の端末で skip されるのはこの 1 本 (API 36 の全件実行の skip も同じ条件)
- Android の実装を巻き込む Gradle のルートも、同じ作業ツリーで逐次に回し直した。結果は上の表と同じ件数で、失敗は無い: android/ `./gradlew test --rerun-tasks` 68 件、kmp/ `./gradlew allTests --rerun-tasks` 166 件 (iosSimulatorArm64 85 + androidHostTest 81)、maui/android/native/ `:ksdialogs-maui-bridge:test --rerun-tasks` 34 件。ios/・maui/ の `dotnet test`・maui/macios/native/ は、報告経路の修正 (Android の Kotlin だけ) の影響を受けないので回し直していない
- 6.1 をチェックした根拠: 7 ルートすべてで、最終コードでの失敗は 0 件。Android instrumented の最終コードでの全件は API 33 実機の 1 台分。API 36 実機で出る A の 5 件 (UiAutomation の接続) は端末側の常駐ツールによるもので、変更前のコードでも同じ 5 件が落ちている。API 33 では同じテストが成功しているので、この change とは無関係と判断した

## 6.2 仕様とテストの対応の検査

| 実行 | 結果 |
|---|---|
| `python3 scripts/scenario-id-coverage.py --selftest` | 全件 OK |
| `python3 scripts/scenario-id-coverage.py` | 未網羅なし (終了コード 0)。LD-TH 8/8・LD-HI 2/2・LD-HA 2/2・LD-HM 6/6・LD-HK 4/4・LD-HS 0/2 (除外 2 件、Sample 通しの証跡で受け入れる) |
| `python3 scripts/scenario-id-coverage.py --require-mirror` | 未網羅なし (終了コード 0)。LD-TH は iOS / Android の双方にある |

警告 (ID の無い Scenario 見出し 2 件・見出しに無い相互参照 6 件) は、どれも archive 済みの change の仕様で、この change のものではない。

## 6.3 標準 lint

変更したファイル (追跡差分と未追跡の新規 77 件) を対象にした。

| 検査 | 結果 |
|---|---|
| `comment-policy-lint.py --selftest` / `--paths` | 自己テスト OK / 禁止 0 件 (検査対象 49 ファイル) |
| `local-path-lint.py --selftest` / `--paths` | 自己テスト OK / 違反なし |
| `identity-lint.py --selftest` / `--paths` | 自己テスト OK / 違反なし |
| `ci-skip-lint.py --selftest` / 本検査 | 自己テスト OK / 印 0 件・違反なし |

`comment-policy-lint.py --advisory` の要確認 (公開 doc コメント内の ADR 参照) のうち、この change で書いた行に当たるのは次の 2 件。どちらもテスト側のソースで、配布物ではない。コードレビューで判定する類型。

- `android/api-surface-check/src/negativeCheckLoadingShowOptions/.../RejectsOptionsArgumentOnLoadingShow.kt` 8 行目 (ADR 参照そのものは変更前から在り、同じ行の文言を直した)
- `android/ksdialogs-core/src/androidTest/.../LoadingActionThreadTests.kt` 27 行目 (テストクラスの doc コメント)

## 追記: kmp/ の負の検査 `showOptions` の実測 (review-003)

review-003 のレビュアーが、今の作業ツリーで `cd kmp && ./gradlew :api-surface-check:compileKotlinIosSimulatorArm64 -Pksdialogs.negativeCheck.showOptions --rerun-tasks` を回した。ビルドは失敗し、診断は `None of the following candidates is applicable:` と `No parameter with name 'options' found.` の 2 件 (2026-09-25)。handbook の kmp/ の行 (1 件と記載) を 2 件に直した。
