# システムバー表示状態の引き継ぎ (Android) — 検証の証跡

tasks 5.1 / 5.2 (PB-SB-01〜07) の確認記録 (2026-08-21)。
対象は android-native デルタスペックの Requirement「システムバー表示状態の引き継ぎ」。
受け入れ方式は design Decision 9 の表に従う。

環境:

| 役割 | 端末 | API |
|---|---|---|
| API 30 以上の実機 | Pixel 6a (`2A141JEGR18112`) | 36 |
| API 30 以上の実機 (2台目) | Pixel 4a (`0B261JEC216142`) | 33 |
| API 24〜29 の旧経路 | エミュレータ `ksn_api29` (`emulator-5554`) | 29 |

## 何を確認したか

### 1. Scenario テスト

実装は `android/ksdialogs/src/androidTest/kotlin/jp/kamusoft/ksdialogs/DialogSystemBarsTests.kt`。

```
cd android
ANDROID_SERIAL=<serial> ./gradlew :ksdialogs:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=jp.kamusoft.ksdialogs.DialogSystemBarsTests
```

結果 (`connected-system-bars.log`):

| 端末 | 実行 | スキップ | 失敗 |
|---|---|---|---|
| Pixel 6a / API 36 | 7 | 1 (PB-SB-04) | 0 |
| Pixel 4a / API 33 | 7 | 1 (PB-SB-04) | 0 |
| ksn_api29 / API 29 | 7 | 5 (PB-SB-01/02/03/06/07) | 0 |

API レベルで意味を持たない Scenario は `assumeTrue` でスキップされる。
**PB-SB-04 は API 29 エミュレータで実際に実行して green** (API 30 以上ではスキップ)。

| Scenario | テスト名 (instrumented) | 実行した API |
|---|---|---|
| PB-SB-01 | `PB_SB_01_全システムバー非表示の画面でダイアログを出してもバーが再出現しない` | 33 / 36 |
| PB-SB-02 | `PB_SB_02_ステータスバーのみ非表示の画面を引き継ぐ` | 33 / 36 |
| PB-SB-03 | `PB_SB_03_ナビゲーションバーのみ非表示の画面を引き継ぐ` | 33 / 36 |
| PB-SB-04 | `PB_SB_04_旧経路でも非表示状態が維持される` | 29 |
| PB-SB-05 | `PB_SB_05_通常表示の画面では従来どおり` | 29 / 33 / 36 |
| PB-SB-06 | `PB_SB_06_表示後の提示先の可視状態の変更には追随しない` | 33 / 36 |
| PB-SB-07 | `PB_SB_07_表示後の提示先の_behavior_の変更には追随しない` | 33 / 36 |

観察の取り方: 「バーが再出現していないか」は**提示先のウィンドウに届く insets** で見る (バーが本当に
出れば提示先の可視領域が狭まって insets に現れる)。あわせてダイアログのウィンドウ自身の
可視状態・`systemBarsBehavior` も読み、引き継ぎがダイアログ側に入っていることを確かめている。
API 29 の旧経路は systemUiVisibility の丸ごとのコピーが経路なので、観察対象もフラグ (要求値と
システムが実際に適用した値の両方) になる。

### 2. 検査が空振りしていないことの確認 (A/B)

引き継ぎを外すとテストが落ちることを、実装を一時的に無効化して確かめた (2026-08-21 実測):

- API 30 以上の可視状態・behavior のコピーを外す → **PB-SB-01 / 02 / 03 が失敗** (Pixel 6a / API 36)
- 旧経路の systemUiVisibility のコピーを外す → **PB-SB-04 が失敗** (ksn_api29 / API 29)

確認後に実装は元へ戻してある。

### 3. 実機・実エミュレータの見え (スクリーンショット)

テストは計測用の引数 `ksdialogsEvidence` を与えたときだけ、ダイアログ表示中の画面を撮って保存する
(通常の実行では撮らない)。撮影は `adb shell am instrument` で直接実行して取り出した
(Gradle 経由だと実行後にテスト APK ごと保存先が消えるため)。

```
cd android
./gradlew :ksdialogs:assembleDebugAndroidTest
adb -s <serial> install -r -t ksdialogs/build/outputs/apk/androidTest/debug/ksdialogs-debug-androidTest.apk
adb -s <serial> shell am instrument -w \
  -e class jp.kamusoft.ksdialogs.DialogSystemBarsTests -e ksdialogsEvidence 1 \
  jp.kamusoft.ksdialogs.test/androidx.test.runner.AndroidJUnitRunner
adb -s <serial> pull /sdcard/Android/data/jp.kamusoft.ksdialogs.test/files/evidence/
```

画面上端 120px と下端 120px の色数を数えた実測 (ダイアログの覆いは一様色なので、バーが出ていれば
時計やアイコンで色数が増える):

| 画像 | 端末 | 上端の色数 | 下端の色数 | 読み取り |
|---|---|---|---|---|
| `PB-SB-01-all-bars-hidden.png` | Pixel 6a / API 36 | 1 | 1 | 両バーとも出ていない |
| `PB-SB-02-status-bar-hidden.png` | Pixel 6a / API 36 | 1 | 4 | ステータスバーだけ消え、ジェスチャーバーは残る |
| `PB-SB-03-navigation-bar-hidden.png` | Pixel 6a / API 36 | 54 | 1 | ステータスバーは残り、ナビゲーションバーだけ消える |
| `PB-SB-04-legacy-bars-hidden.png` | ksn_api29 / API 29 | 1 | 1 | 旧経路でも両バーとも出ていない |

いずれもダイアログ (白いカード「システムバーの確認」) が覆いの上に見えている状態での撮影。

API 29 の撮影では、システムの immersive 確認オーバーレイ (「Viewing full screen」) が画面上部を
覆って見えを隠したため、`settings put secure immersive_mode_confirmations confirmed` で抑止して
撮り直した。**観測後に `settings delete secure immersive_mode_confirmations` で既定へ戻してある**。

## ファイル

| ファイル | 内容 |
|---|---|
| `PB-SB-01-all-bars-hidden.png` | 全バー非表示の画面でダイアログを表示 (Pixel 6a / API 36) |
| `PB-SB-02-status-bar-hidden.png` | ステータスバーのみ非表示 (Pixel 6a / API 36) |
| `PB-SB-03-navigation-bar-hidden.png` | ナビゲーションバーのみ非表示 (Pixel 6a / API 36) |
| `PB-SB-04-legacy-bars-hidden.png` | 旧経路での非表示維持 (ksn_api29 / API 29) |
| `connected-system-bars.log` | PB-SB-01〜07 の実行ログ (3端末) |
