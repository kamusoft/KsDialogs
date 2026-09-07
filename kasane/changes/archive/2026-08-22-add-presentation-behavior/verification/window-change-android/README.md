# 表示中のウィンドウ変化への追随 (Android) — 検証の証跡

tasks 1.4 の Android 分 (PB-WN-01〜03) の確認記録 (2026-08-21)。
対象は dialog-contract デルタスペックの Requirement「表示中のウィンドウ寸法変化への追随」。

環境: 実機 Pixel 6a / API 36 (`2A141JEGR18112`) と Pixel 4a / API 33 (`0B261JEC216142`)。
Sample は `samples/android` の KsDialogs Sample (applicationId `jp.kamusoft.ksdialogs.samples.android`)。

## 何を確認したか

### 1. Scenario テスト (実 Android のレイアウト機構で実行)

3本とも実機上で器を実際のレイアウトパスに載せ、実効値を固定したあとでウィンドウの寸法・
可視領域の余白を変えて、再配置後の中身の矩形を実測している。実装は
`android/ksdialogs/src/androidTest/kotlin/jp/kamusoft/ksdialogs/DialogWindowChangeTests.kt`
(舞台は `support/DialogWindowGeometryStage.kt`)。

```
cd android
ANDROID_SERIAL=<serial> ./gradlew :ksdialogs:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=jp.kamusoft.ksdialogs.DialogWindowChangeTests
```

結果: **3 tests / 0 failures** (両実機。`connected-window-change.log`)。

| Scenario | テスト名 (instrumented) | 変えたもの | 期待した矩形 (dp) |
|---|---|---|---|
| PB-WN-01 | `PB_WN_01_回転後も配置規則が新しい寸法で成立する` | 390x844 / 余白 上59 下34 → 844x390 / 余白 下21 左右59 | (97.5, 340.625, 195, 187.75) → (240.5, 138.375, 363, 92.25) |
| PB-WN-02 | `PB_WN_02_ウィンドウ寸法のみの変化に追随する` | 幅 390 → 320 (余白は据え置き) | (80, 340.625, 160, 187.75) |
| PB-WN-03 | `PB_WN_03_可視領域インセットのみの変化に追随する` | 余白 上59 下34 → 全辺 0 (寸法は据え置き) | (97.5, 316.5, 195, 211) |

期待値は属性 (基準領域 visibleArea / 全辺 24 の余白 / 比率 幅 0.5・高さ 0.25 / 中央配置) と
新しい寸法・余白から、軸別レイアウト規則の手順で手計算したもの。許容差は共通ケース表と同じ 1.0 dp。
iOS の同名ミラー (`ios/Tests/KsDialogsTests/DialogWindowChangeTests.swift`) と同じ入力・同じ期待値で、
値まで一致している。

PB-WN-01 では、実効値の固定後に添付を別の値へ書き換えてから寸法・余白を変え、**再配置が固定済みの
属性で行われる** (スナップショット凍結が解けない) ことも同じテストで確かめている。

変化が実際に効いていることは期待値自身が担保している — 3本とも変化前の矩形とは異なる値を期待して
おり、変化が反映されなければ失敗する。

### 2. 実 Sample での表示 (縦向き)

Layout Dialog で Horizontal = End / Vertical = End / Use visible area = ON にして表示した状態が
`02-portrait-dialog.png` (設定は `01-portrait-settings.png`)。ダイアログは可視領域の右下 — ステータスバーと
ジェスチャーバーを避けた領域から余白 24dp 内側 — に寄っている。

### 3. 実 Sample を回転させたときの観察 (重要な Android 差)

2 の状態 (ダイアログ表示中) で `adb shell settings put system user_rotation 1` により横向きへ回転させた
結果が `03-landscape-after-rotation.png` — **ダイアログは閉じ、Sample はメニュー画面に戻っている**。

これは Sample の `MainActivity` が `android:configChanges` を宣言しておらず、回転で Activity が
作り直されるため。画面の破棄は器消失の経路に入り、show は cancelled で確定する (結果通知のルール
基本ルール4)。iOS の Simulator では提示元が作り直されないためダイアログが残り、証跡の見え方が
OS 間で異なる。**PB-WN が扱う「表示したまま寸法が変わる」状況は、Android では回転そのものではなく
`configChanges` を宣言した画面・マルチウィンドウのリサイズ・インセットの変化で起きる**。

そのため実 Sample での裏取りは「横向きの寸法・余白から位置が導かれること」を、回転後に
同じ設定 (End / End / visible area) で出し直して確かめた (`04-landscape-dialog.png`)。
ダイアログは横向きの可視領域の右下に置かれ、端からの距離は縦向きとは異なる — 位置が
その時点のウィンドウ寸法と可視領域から導かれていることが見える。

観測後、端末の回転設定は既定へ戻した (`user_rotation 0` / `accelerometer_rotation 1`)。

## ファイル

| ファイル | 内容 |
|---|---|
| `01-portrait-settings.png` | Layout Dialog の設定 (End / End / visible area) |
| `02-portrait-dialog.png` | 縦向きで表示したダイアログ (可視領域の右下に配置) |
| `03-landscape-after-rotation.png` | 表示中に回転させた直後 (Activity 再生成でダイアログは閉じ、メニューへ戻る) |
| `04-landscape-dialog.png` | 横向きで出し直したダイアログ (横向きの可視領域の右下に配置) |
| `connected-window-change.log` | PB-WN-01〜03 の実行ログ (Pixel 6a / API 36 と Pixel 4a / API 33) |
