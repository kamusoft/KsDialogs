# Android 透明オーバーレイの実環境確認 (tasks 7.2)

対象要求: android-native「透明オーバーレイの正式対応 (Android)」/ Scenario「透明時も配置規則が保たれる」

## 環境

| 項目 | 値 |
|---|---|
| 実施日 | 2026-08-19 |
| 端末 | Android 実機 Pixel 6a (serial 2A141JEGR18112)、1080x2400 px / density 420 (2.625x) |
| ステータスバー高 | 132 px (`dumpsys window` の `InsetsSource ... type=statusBars frame=[0,0][1080,132]`) |
| アプリ | samples/android (`jp.kamusoft.ksdialogs.samples.android`)、Layout Dialog |
| 配備 | `cd samples/android && ANDROID_SERIAL=<serial> ./gradlew installDebug` |

## 手順 (2条件とも同一)

1. `adb shell am force-stop` → `am start -n .../.MainActivity` でアプリを起動し直す
2. メニューの `Layout Dialog` をタップしてパネルを開く (パネルは既定値のまま = Center / Offset 0 / visible area ON)
3. **表示前**を撮影 (`adb exec-out screencap -p`)
4. `Show` をタップ
5. **表示中**を撮影
6. 画面上端から 132 px (ステータスバー領域) の平均輝度 (ITU-R BT.601 の Y) を算出し、表示前後で比較する

覆いの色は Sample の公開面から調整できないため、透明条件は
`samples/android/.../SampleDialogRegistration.kt` の Layout Dialog 登録に
`overlayColor = Color.TRANSPARENT` を**一時的に**足して観測した。観測後に撤去し、
`samples/` 配下 138 ファイルの SHA-256 一致で原本無改変を確認したうえで、原本ビルドを端末へ再配備した。

## 実測

| 条件 | 表示前 | 表示中 | 差 | 証跡 |
|---|---|---|---|---|
| 覆い = 既定 (黒 40%) | 253.95 | 156.39 | **-97.55** | `01-default-overlay-before.png` → `02-default-overlay-during.png` |
| 覆い = 透明 | 253.95 | 253.95 | **±0.00** | `03-transparent-overlay-before.png` → `04-transparent-overlay-during.png` |

既定の覆いでは同じ測り方でステータスバーが 97.55 暗くなる。すなわち**測定手段は差を検出できている** (陰性対照)。
その手段で透明の覆いを測ると差が完全に 0 であり、ステータスバーは暗転していない。

配置についても、透明条件の表示中スクリーンショット (`04-transparent-overlay-during.png`) で
中身 (`レイアウト確認` + `キャンセル` / `OK`) が既定値どおり水平中央・可視領域の垂直中央に出ており、
覆いの色によって配置規則が変わる分岐は入っていない。

## 結論

Scenario「透明時も配置規則が保たれる」は満たしている。ステータスバーは暗転せず (差 0.00)、
かつ配置は覆いが既定のときと同じ規則に従う。
instrumented test `DialogTransparentOverlayTests` (3件、Pixel 6a で green) と同じ結論を、
実機の画素で裏取りした形になる。
