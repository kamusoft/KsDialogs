# 属性調整パネル操作部の読み上げ対応 — accessibility tree 検査の証跡

Layout Dialog の属性調整パネルにある操作部へ与えた読み上げ用の名前・役割・状態を、
4ルートそれぞれの accessibility tree を実機/シミュレータから取り出して確認した記録 (2026-08-19)。

## 取得方法

| ルート | 取得方法 |
|---|---|
| ios | XCUITest (`XCUIApplication(bundleIdentifier:)` で起動 → `app.debugDescription`) を使い捨てのプローブ用 Xcode プロジェクトから実行。Sample 側には何も足していない |
| kmp (iosApp) | 同上 (bundle id だけ差し替え) |
| android | 実機 (Pixel 系, Android 16, 1080x2340) で `adb shell uiautomator dump` |
| maui | Android 側は `adb shell uiautomator dump`、iOS 側は ios と同じ XCUITest プローブ (`DEVELOPER_DIR` を .NET for iOS が要求する Xcode に向ければ net10.0-ios もビルドできる) |
| kmp (androidApp) | 同上 |

各ルートとも2状態を取得している。

- initial: パネルを開いた直後 (中央配置・移動量 0・基準領域 ON)
- changed: Horizontal を End、Vertical を Start、基準領域トグルを OFF、OffsetX に値を入れた状態

## 確認できたこと

**名前と役割 (Scenario: パネル操作部が読み上げで識別できる)**

| 操作部 | ios / kmp-ios | android / maui / kmp-android |
|---|---|---|
| 配置の選択肢 | `Button`、label `Horizontal Start` ほか計6件 | `android.widget.Button`、content-desc `Horizontal Start` ほか計6件 |
| 移動量欄 | `TextField`、label `OffsetX` / `OffsetY` | `android.widget.EditText`、content-desc (maui は読み上げ文字列) に `OffsetX` / `OffsetY` |
| 基準領域トグル | `Switch`、label `Use visible area` | `android.widget.Switch`、content-desc `Use visible area` |
| 表示操作 | `Button`、label `Show` | `android.widget.Button`、text `Show` |
| 戻る | `Button`、label `戻る` | `android.widget.Button`、content-desc `戻る` |

同じ文言の選択肢 (Start / Center / End) は、どのルートでも所属行の文言との複合名で一意に識別できる。

**状態と現在値 (Scenario: 操作部の状態が読み上げに含まれる)**

| 状態 | ios / kmp-ios | android / maui / kmp-android |
|---|---|---|
| 選択肢の選択状態 | `Selected` 特性 (initial は Center、changed は Horizontal End / Vertical Start) | `selected="true"` (同上) |
| トグルの ON/OFF | `value: 1` → `value: 0` | `checked="true"` → `checked="false"` |
| 移動量欄の現在値 | `value: 0` → 入力後の値 | `text="0"` → `text="24"` (maui は読み上げ文字列が `OffsetX, 24`) |

## ファイル

| ファイル | 内容 |
|---|---|
| `ios-xcuitest-dump.txt` | ios ルートの accessibility 階層 (initial / changed) |
| `kmp-ios-xcuitest-dump.txt` | kmp ルート iosApp の accessibility 階層 (initial / changed) |
| `android-uiautomator-initial.xml` / `-changed.xml` | android ルートのノードツリー |
| `maui-android-uiautomator-initial.xml` / `-changed.xml` | maui ルート (Android) のノードツリー |
| `maui-ios-xcuitest-dump.txt` | maui ルート (iOS) の accessibility 階層 (initial / changed) |
| `maui-ios-panel.png` | maui ルート (iOS) のパネル外観 |
| `kmp-android-uiautomator-initial.xml` / `-changed.xml` | kmp ルート androidApp のノードツリー |
| `android-panel-changed.png` | 読み上げ対応の前後で見た目が変わらないことの確認用 (changed 状態の画面) |
