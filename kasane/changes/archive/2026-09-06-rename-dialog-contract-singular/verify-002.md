# 一致検証: rename-dialog-contract-singular (002 回目・追加検証)

**日付**: 2026-09-06
**判定**: INVALID (❌ 1 件。**本 change 起因ではない既存の環境依存の失敗**。見立ては下記「❌ の見立て」)

## この検証の位置づけ

`verify-001.md` は dialog-contract spec の Scenario「4 形態のライブラリテストが通る」を、`kasane/handbook/cross/test-execution.md` の全件実行表のうち **android/ (instrumented) ルートを実行せずに** ✅ とした。同 handbook はこのルート (`./gradlew connectedDebugAndroidTest`、実測 305 tests) を独立の行として持ち、「実 View のレイアウト・実入力の注入・実ウィンドウの見えを測るテストはここにしかない。`./gradlew test` では 1 件も走らない」と定めている。

本書は**この 1 ルートだけを実行した裏取り**である。他の Scenario の対応表は再作成しない — dialog-contract 7 Scenario / user-docs 3 Scenario の対応と、ios / android (JVM) / kmp / maui の実行結果、負のコンパイル検証、Sample ビルド、追加検査 (虚偽チェック・逆流・未記録乖離) は `verify-001.md` を参照のこと。本書が更新するのは Scenario「4 形態のライブラリテストが通る」の 1 行だけである。

## 実行環境

handbook の「API レベルで走る / 走らない Scenario」に従い、**API 29 のエミュレータ (旧経路用)** と **API 30 以上の端末**の両方で回した。端末は `ANDROID_SERIAL` で 1 台ずつ絞った (別用途で起動中の `emulator-5554` / AVD `ksn_custcell_api35` は使用していない)。API 29 エミュレータ (`ksn_api29`) は本検証のために boot し、検証後に `adb emu kill` で終了した。

| 端末 | API | 位置づけ |
|---|---|---|
| `ksn_api29` (エミュレータ) | 29 | 旧経路 (`systemUiVisibility`) 側 |
| Pixel 6a (実機) | 36 | API 30 以上側 |
| Pixel 4a (実機) | 33 | 失敗の切り分け用 (該当 2 クラスのみ) |

## 件数 (端末 × モジュール)

結果 XML (`ksdialogs/build/outputs/androidTest-results/connected/debug/TEST-*.xml` と `ksdialogs-compose/` の同位置) の `tests` / `failures` / `skipped` 属性で確認した。

| 端末 (API) | モジュール | tests | failures | skipped |
|---|---|---|---|---|
| `ksn_api29` (29) | `:ksdialogs` | 268 | **1** | 6 |
| `ksn_api29` (29) | `:ksdialogs-compose` | 37 | 0 | 0 |
| `ksn_api29` (29) | **合計** | **305** | **1** | 6 |
| Pixel 6a (36) | `:ksdialogs` (1 回目) | 268 | **1** | 1 |
| Pixel 6a (36) | `:ksdialogs-compose` | 37 | 0 | 0 |
| Pixel 6a (36) | `:ksdialogs` (2 回目) | 268 | **2** | 1 |
| Pixel 6a (36) | **合計** (2 回目基準) | **305** | **2** | 1 |

- 総数 305 (`:ksdialogs` 268 + `:ksdialogs-compose` 37) は handbook の実測値と一致する
- Pixel 6a の `:ksdialogs` は 2 回回した (1 回目の全件実行後に単体再実行で XML を上書きしてしまったため、証跡の取り直しを兼ねて再実行)。1 回目と 2 回目で失敗の本数が違う (後述)
- `:ksdialogs-compose` は両端末とも 37 / 0 failures / 0 skipped

### skipped の内訳と handbook の記述との整合

| 端末 (API) | skipped | 内訳 |
|---|---|---|
| Pixel 6a (36) | 1 | `DialogSystemBarsTests.PB_SB_04_旧経路でも非表示状態が維持される` (`systemUiVisibility` の経路は Android 11 未満にしかない) |
| `ksn_api29` (29) | 6 | `DialogSystemBarsTests` の `WindowInsetsController` 経路 5 本 (`PB_SB_01` / `PB_SB_02` / `PB_SB_03` / `PB_SB_06` / `PB_SB_07`) + `ToastSystemInputTests.Toast_表示中でも_IME_を出し入れできる` (IME の可視状態を読めるのは API 30 以降) |

handbook の「`:ksdialogs` 268 (skipped 1)」は API 30 以上側の値で、Pixel 6a の実測と一致する。API 29 側では新経路 5 本が skip される代わりに **旧経路の `PB_SB_04` が実際に走る**ため、2 台の組み合わせで `DialogSystemBarsTests` の全 Scenario が 1 度は実行されている。handbook の「全件を実行したと言えるのは対象 API レベルをそれぞれ 1 台ずつ回したとき」という記述と整合する。

## failure の内容

| # | テスト | 失敗する端末 | 症状 |
|---|---|---|---|
| F1 | `jp.kamusoft.ksdialogs.ToastSystemInputTests.Toast_表示中でも戻るとホームが通る` | Pixel 6a (36) のみ。2 回の全件実行 + 単体再実行の**計 3 回すべて失敗** | `java.lang.NullPointerException: Cannot run onActivity since Activity has been destroyed already` (`ActivityScenario.onActivity`)。`GLOBAL_ACTION_BACK` の後に `currentActivity()` を読む箇所で落ちる — 戻る操作で提示先 Activity が破棄される API 36 の挙動差 |
| F2 | `jp.kamusoft.ksdialogs.LoadingCoalescingTests.LD_CO_13_出の途中の新しい開始は出の完了後に新世代として表示される` | `ksn_api29` (29) で全件・単体の**2 回とも失敗**。Pixel 6a (36) では 1 回目 成功 / 2 回目 失敗の**間欠** | `java.lang.AssertionError` — `android/ksdialogs/src/androidTest/kotlin/jp/kamusoft/ksdialogs/LoadingCoalescingTests.kt:338` の `assertTrue(InstrumentedDialogWaiting.waitUntil { harness.coordinator.isDismissing })`。出の演出が観測される前に完了してしまう時間依存の競合 |

### 切り分け: 本 change 起因ではない

| 観点 | 確認結果 |
|---|---|
| 失敗した 2 テストは本 change の変更対象か | **対象外**。`git status --porcelain` / `git diff HEAD` の双方で `android/ksdialogs/src/androidTest/` に差分は 1 件もない (`ToastSystemInputTests.kt` の最終更新は `add-toast` の実装コミット) |
| Android 本体で本 change が触ったファイル | `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/Dialog.kt` / `DialogViewRegistry.kt` / `KsDialogs.kt → KsDialog.kt` (rename) と `android/ksdialogs-compose/src/main/kotlin/jp/kamusoft/ksdialogs/compose/ComposeDialogShow.kt` の 4 つのみ。差分はいずれも `KsDialogs` → `KsDialog` の識別子置換 (宣言 1 行・レシーバ 1 行・KDoc 参照 2 行) で、挙動に触る変更を含まない |
| Loading / Toast のコードは変わったか | Android では**変わっていない**。リポジトリ全体で Loading / Toast 名を持つ変更ファイルは `maui/KsDialogs.Maui.Tests/LoadingFacadeTests.cs` の 1 行 (`IKsDialogs` → `IKsDialog`) だけで、Android には及ばない |
| 失敗した 2 テストは Dialog の契約型に触れるか | 触れない。両クラスとも `KsDialog` / `KsDialogs` を参照しない |
| 同じビルドで他の API レベルではどうか | Pixel 4a (API 33) で該当 2 クラス (17 tests) を実行し **0 failures**。F1 は API 29 / 33 で成功、F2 は API 33 で成功 |

以上より、2 件の failure は **改名によって生じた退行ではなく、端末 / API レベルに依存する既存の失敗** (F1 は API 36 の戻る操作の挙動差、F2 は演出の観測タイミングの競合) である。

### handbook との差 (drift 所見)

`kasane/handbook/cross/test-execution.md` の android/ (instrumented) 行は「305 tests / 0 failures」(2026-08-28 実測) を掲げるが、**現在の端末構成では failures 0 が再現しない**。件数 305 と skip の設計は一致しているため、乖離しているのは failures の実測値だけである。本書は検証記録であり handbook は書き換えていない (足場・長命層の書き換えは本ワーカーの範囲外)。

## 対応表の更新 (1 Scenario のみ)

| Requirement / Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| Dialog 契約の改名 / 4 形態のライブラリテストが通る (**android instrumented ルート**) | — | `ksn_api29` (API 29) 305 tests / 1 failure / 6 skipped、Pixel 6a (API 36) 305 tests / 1〜2 failures / 1 skipped。失敗 2 件はいずれも Dialog の契約に触れない Loading / Toast のテストで、本 change の差分の外にある | ❌ 乖離 (未記録) |

- 他の 9 Scenario (dialog-contract 6 + user-docs 3) と Sample ビルド・負のコンパイル検証・追加検査は `verify-001.md` のとおりで、本書は変更しない
- `deviation.md` は依然として不在のため、この ❌ は**未記録乖離**として扱う

## ❌ の見立て

**実装を直すべきではない**と見る。改名の差分は識別子置換のみで、失敗した 2 テストはその差分の外にあり、同じビルドが API 33 では両方成功する。したがって「改名が壊した」という読みは成立しない。

取りうる収束は次のいずれかで、**決定は呼び出し元とユーザーの判断**である。

| 案 | 内容 |
|---|---|
| A | `deviation.md` に「android instrumented の 2 件 (F1 / F2) は本 change 起因でない既存の環境依存の失敗」として記録し、本 change はこの差分を抱えたまま完了とする |
| B | A に加えて、F1 (API 36 の戻る挙動差) と F2 (演出観測の競合) を別 change として起票する。F2 は Pixel 6a でも間欠的に落ちるため、端末を問わない不安定さを持つ |
| C | handbook の android/ (instrumented) 行の実測値 (305 tests / 0 failures) を現在の端末構成で取り直す — ただし handbook の更新は長命層の変更であり、本 change のスコープ外 |

## 判定

**INVALID** — Scenario「4 形態のライブラリテストが通る」を android/ (instrumented) ルートまで含めて見ると、`ksn_api29` (API 29) と Pixel 6a (API 36) の双方で failure が残り、`deviation.md` に記録がない。❌ は 1 件。虚偽チェック・逆流・他 Scenario の乖離は `verify-001.md` の検査どおり無し (本検証では足場・実装のいずれも変更していない)。

ただし ❌ の実体は**本 change 起因ではない既存の環境依存の失敗 2 件**であり、改名そのものの一致性 (契約型名・旧名の解決不能・製品名識別子の据え置き・文書と lint の追随) には問題が見つかっていない。
