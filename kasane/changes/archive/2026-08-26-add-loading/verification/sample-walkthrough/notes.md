# 4ルートのパリティ通し — 証跡

tasks 6.3 の記録 (2026-08-26)。対象は samples デルタスペックの Requirement
「Default Loading デモ項目」「Custom Loading デモ項目」(Scenario LD-SA-01 / LD-SA-02 / LD-SA-03)。

4ルート (ios / android / maui / kmp) × 実行 OS の**6組**で、同じ順で同じ操作を通した。

## 環境

| ルート | 実行 OS / 端末 | ビルドと投入 |
|---|---|---|
| ios | iPhone 17 Simulator | `samples/ios` を xcodebuild → `simctl install` / `simctl launch jp.kamusoft.ksdialogs.samples.ios` |
| android | Pixel 4a 実機 (1080x2340) | `samples/android` の `:app:assembleDebug` → `adb install -r` / `am start` |
| maui (iOS) | iPhone 17 Simulator | `samples/maui/KsDialogs.Sample.Maui` を `dotnet build -f net10.0-ios` (`DEVELOPER_DIR` はワークロードが要求する Xcode 26.1 を指す) → `simctl install` / `simctl launch` |
| maui (Android) | Pixel 4a 実機 | `dotnet build -f net10.0-android -p:EmbedAssembliesIntoApk=true` → `adb uninstall` してから `adb install -r` (README の Fast Deployment 残骸対策) |
| kmp (iOS) | iPhone 17 Simulator | `samples/kmp/iosApp` を xcodebuild (Run Script phase が shared framework を link) → `simctl install` / `simctl launch` |
| kmp (Android) | Pixel 4a 実機 | `samples/kmp` の `:androidApp:assembleDebug` → `adb install -r` / `am start` |

## 通した操作 (6組すべて同一)

1. メニューの初期状態を撮る (結果表示エリアはまだ出ていない)
2. `Default Loading` を押す → 表示中のコマを撮る (開始メッセージ + 進捗、途中更新後のメッセージ + 進捗)
3. 完了後の結果表示を撮る
4. `Basic Dialog` を出して外側タップでキャンセルし、直近の結果を `結果: cancelled` にする
   (Default / Custom はどちらも完了時の結果表示が `結果: 完了` で同じため、遷移が起きたことを見えるようにする — config `ui.screenshot` の共通規律)
5. `Custom Loading` を押す → カスタム View と進捗の反映を撮る (背後に `結果: cancelled` が残っている)
6. 完了後の結果表示 `結果: 完了` を撮る

## 観察結果 (パリティ)

| 観察点 | ios | android | maui (iOS) | maui (Android) | kmp (iOS) | kmp (Android) |
|---|---|---|---|---|---|---|
| メニュー項目 `Default Loading` / `Custom Loading` の並び (7件の後ろに2件) | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| 開始メッセージ `Loading...` + 進捗の百分率 | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| 途中でメッセージが `Soon...` へ変わる | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| 進捗が段階的に上がる (0 / 25 / 50 / 75 / 100 %) | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| 完了で表示が消え `結果: 完了` になる | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| カスタム View の見出し `カスタムローディング` + 帯 + 百分率 | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| 進捗が VM 経由でカスタム View に反映される | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| Custom も完了で消え `結果: 完了` になる | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |

**LD-SA-03 の判定: 成立。** 6組で文言・表示内容・結果表示が一致し、差は実装経路
(ios / android = 手動登録、maui = Loading 版1行登録の DI チェーン、kmp = 共有 VM 型 + OS 側登録) だけだった。

観察できた差は次の2つで、いずれも本体の仕様差ではない:

- **既定ローディングの回し物の形**: iOS はスポーク状 (`UIActivityIndicatorView` 相当)、Android は円弧
  (`ProgressBar` 相当)。OS 標準の不定進捗表示をそのまま使う設計 (mock 案A) の帰結であり、
  ルート間 (同じ OS の ios/maui/kmp どうし) では一致している
- **覆いの外側の地の色**: MAUI Android だけ、メニュー画面の上端がテンプレート由来の帯色になる。
  これは Loading とは無関係の既存の見た目で、本変更の前後で変わっていない

## 撮影・保存の規律

- Android は実機のため、保存前に**ステータスバーの帯 (上端 132 px / MAUI は 140 px) を切り落とした**。
  通知アイコン等の個人要素を含まないことを保存後に開いて確認済み。切り落とし以外の加工はしていない
- iOS はシミュレータのため無加工。写り込んでいるのは時刻・Wi-Fi・電池の標準表示のみ
- 進捗のコマは連写 (`simctl io screenshot` / `adb exec-out screencap` の繰り返し) から変化のあったコマを選んだ。
  iOS Simulator は1コマあたり 1.4 秒ほどかかるため、拾えた進捗値がルートごとに違う
  (0% / 25% / 50% / 75% のいずれか)。**進捗値そのものの網羅は自動テスト (LD-PR-01〜06) が受け持つ**ので、
  ここでは「更新されること」と「メッセージが差し替わること」が見えれば足りるものとした

## ファイル

`<ルート>-<連番>-<状態>.png`。連番の意味は6組で共通:

| 連番 | 状態 |
|---|---|
| 01 | メニュー初期状態 (結果表示なし) |
| 02 | Default Loading 表示中 — `Loading...` + 進捗 |
| 03 | Default Loading 表示中 — `Soon...` + 進捗 (メッセージ更新後) |
| 04 | Default Loading 完了 — `結果: 完了` |
| 05 | Custom Loading の直前 — `結果: cancelled` |
| 06 | Custom Loading 表示中 — `カスタムローディング` + 帯 + 百分率 |
| 07 | Custom Loading 完了 — `結果: 完了` |

ルートの接頭辞は `ios` / `android` / `maui-ios` / `maui-android` / `kmp-ios` / `kmp-android`。

## 関連

- ダイアログ表示中の Loading が最前面になること (Scenario LD-AT-04) の実提示確認は
  [../loading-front/notes.md](../loading-front/notes.md)
