# 多段表示の系列挙動 (Android) — 検証の証跡

tasks 1.3 (Android の PB-MD-04 / PB-MD-05) の確認記録 (2026-08-21)。
対象は dialog-contract デルタスペックの Requirement「多段表示の系列挙動の固定」。
期待値の正は `kasane/concepts/core/api/multi-display-semantics.md` の OS 差分表。

## 何を確認したか

### 1. 契約ロジックの JVM テスト (PB-MD-01〜04)

既存の 4 本を温存したまま、テスト名へ ID を 1 対 1 で付与した (改名のみ・期待値不変)。
実装は `android/ksdialogs/src/test/kotlin/jp/kamusoft/ksdialogs/DialogMultiDisplayTests.kt`。

```
cd android
./gradlew test --rerun-tasks
```

結果: **50 tests / 0 failures** (`ksdialogs/build/test-results/testDebugUnitTest/TEST-*.xml`)。

| Scenario | テスト名 (JVM) |
|---|---|
| PB-MD-01 | `PB_MD_01_結果確定で自分のダイアログだけが閉じる` |
| PB-MD-02 | `PB_MD_02_2枚重ねて上から順に閉じる` |
| PB-MD-03 | `PB_MD_03_重ね出し中の外側タップは手前のみに届く` |
| PB-MD-04 | `PB_MD_04_下の段を先に閉じたときの挙動` |

### 2. 実ウィンドウでの系列テスト (PB-MD-04 / PB-MD-05)

器を実際のウィンドウへ載せ、段ごとに別の観測用フックを添付して、どちらの段の演出が動いたかを
取り違えない形で観察している。実装は
`android/ksdialogs/src/androidTest/kotlin/jp/kamusoft/ksdialogs/DialogMultiDisplayPresentationTests.kt`。

```
cd android
ANDROID_SERIAL=<serial> ./gradlew :ksdialogs:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=jp.kamusoft.ksdialogs.DialogMultiDisplayPresentationTests
```

結果: **2 tests / 0 failures** (実機 Pixel 6a / API 36 と Pixel 4a / API 33 の両方。`connected-multi-display.log`)。

| Scenario | テスト名 (instrumented) | 観察していること |
|---|---|---|
| PB-MD-04 | `PB_MD_04_下の段を先に閉じても上の段は残り後の報告で確定する` | 下の段だけが退出の演出を通って撤去され、上の段は SHOWN のままウィンドウ上に残る (退出フック 0 回・確定した結果は 1 件)。その後の報告で上の段が通常どおり completed になる |
| PB-MD-05 | `PB_MD_05_器が画面から外れると各_show_が_cancelled_で1回だけ確定する` | 提示先の画面を実際に破棄 (`scenario.moveToState(DESTROYED)`) し、両段の show が cancelled で確定・退出の演出は両段とも 0 回・確定後に報告しても結果が増えない |

iOS の同名ミラー (`ios/Tests/KsDialogsTests/DialogMultiDisplayPresentationTests.swift`) では PB-MD-04 の
THEN が「上の段も cancelled で確定」となり、差分表どおりに割れている。

## OS 差分表との照合

| | 見え方 | 上の show の結果 |
|---|---|---|
| 差分表 (Android) | 下だけが閉じ、上は表示されたまま残る | そのまま操作でき、後から通常どおり completed で返る |
| 実測 (本テスト) | 下の段のみ REMOVED、上の段は SHOWN でウィンドウ上に残る | その後の報告で `Completed(true)` |

一致。差分表の追記・変更は不要。

## 補足

PB-MD-05 の「器消失」は `RecordingDialogPresentationSurface.simulateHostLoss` ではなく
**実際の Activity 破棄**で作っている (`ActivityScenario` の状態遷移)。Android では画面の破棄が
本番と同じ `ActivityDestroyObserver` 経由で全段に届くため、系列としての観察に適している。
単一ダイアログでの器消失は PB-TR-09 / 12 / 23 が別途押さえているので、ここでは重ならない観点
(2 段重なった状態での一斉確定と「ちょうど 1 回」) だけを見ている。

## ファイル

| ファイル | 内容 |
|---|---|
| `connected-multi-display.log` | PB-MD-04 / PB-MD-05 の実行ログ (Pixel 6a / API 36 と Pixel 4a / API 33) |
| `unit-multi-display.log` | PB-MD-01〜04 (JVM) を含む `./gradlew test --rerun-tasks` の実行ログ |
