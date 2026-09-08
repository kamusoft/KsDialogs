# MAUI Sample の両 OS 通し (tasks 6.1 / 6.2)

Requirement「MAUI Sample の両 OS 通しと MAUI テストルートの完了判定」/ Scenario「両 OS の全デモ項目」の証跡。
観測日: 2026-09-08。`Microsoft.Maui.Controls` 10.0.20 (Sample の `MauiVersion` も 10.0.20) でビルドした `samples/maui` を、iOS Simulator と Android エミュレータの両方で、handbook `cross/sample-parity.md` の安定デモ ID 14 件すべてについてデモ駆動モードの起動引数で通した。

説明文は保存した画像を 1 枚ずつ開き直して書いた (lessons impl L-001)。`ios/` `android/` それぞれ 25 枚、md5 の重複は 0 件。

## 観測環境

| | iOS | Android |
|---|---|---|
| 端末 | iPhone 17 Pro (iOS 26.5) の Simulator。観測のために新規に boot し、終了後 shutdown した (起動中のものは流用していない) | AVD `Pixel_6` (system image android-31、1080x2400)。接続中の実機 2 台には触れていない |
| ビルド | `dotnet build -f net10.0-ios -p:RuntimeIdentifier=iossimulator-arm64` | `dotnet build -f net10.0-android -p:EmbedAssembliesIntoApk=true` |
| 起動 | `xcrun simctl terminate` → 1.5 秒待ち → `xcrun simctl launch <UDID> jp.kamusoft.ksdialogs.samples.maui --demo <ID>` | `adb -s <serial> shell am force-stop` → `am start -n <package>/<activity> --es demo <ID>` |
| 撮影 | `xcrun simctl io <UDID> screenshot` | `adb -s <serial> exec-out screencap -p` |
| 操作 | シミュレータ操作ツールの tap / swipe (device point) | `adb shell input tap` / `input swipe` (物理ピクセル) |

Sample のソースは撮影のために一切改変していない。状態はすべて起動引数 (`demo` / `loading-step-interval-ms`) で作った。回転を要する項目は 14 件の中に無いため、オーナー操作が必要な未観測項目は無い。

## ビルド前に踏んだ落とし穴 (実測メモ)

グループ 2 で `MauiVersion` を 10.0.70 → 10.0.20 に下げた後、`bin/` `obj/` を残したままの増分ビルドで作った iOS アプリは、ダイアログを表示した瞬間に SIGABRT で落ちた。管理例外は
`Could not create an native instance of the type 'Microsoft.Maui.Platform.MauiCALayerAutosizeToSuperLayerBehavior': the native class hasn't been loaded.`
で、`PlatformDialogContent.Create` の `ToPlatform` 内で起きる。`bin/` `obj/` を捨ててビルドし直すと再現しなくなり、以降 14 件すべてが通った。**版を下げた後の Sample は増分ビルドを信用せず、`bin/` `obj/` を捨ててからビルドする**。ライブラリ側の不具合ではなく、静的レジストラを含む生成物が古い版のまま残ることによるもの。

## iOS の項目別証跡 (`ios/`)

| # | 安定デモ ID | ファイル | 実体 |
|---|---|---|---|
| 1 | `basic-dialog` | `ios-01-basic-dialog-shown.png` | メニュー上にダイアログ。本文「こんにちは、KsDialogs!」、ボタン「キャンセル」「OK」 |
| | | `ios-01-basic-dialog-result.png` | OK をタップして閉じた後、末尾までスクロール。「直近の結果 / 結果: completed(true)」が現れている |
| 2 | `declarative-dialog` | `ios-02-declarative-dialog-shown.png` | 同じ本文「こんにちは、KsDialogs!」のダイアログ (`SampleText.DeclarativeDialogMessage` は Basic と同値) |
| | | `ios-02-declarative-dialog-result.png` | 閉じた後の「結果: completed(true)」 |
| 3 | `model-dialog` | `ios-03-model-dialog-shown.png` | 本文「ViewModel から表示しています」のダイアログ |
| | | `ios-03-model-dialog-result.png` | 閉じた後の「結果: completed(true)」 |
| 4 | `text-input-dialog` | `ios-04-text-input-dialog-shown.png` | 本文「メッセージを入力してください」と、プレースホルダ「ここに入力」の入力欄を持つダイアログ |
| | | `ios-04-text-input-dialog-typed.png` | 入力欄に `Hello` を入力した状態 |
| | | `ios-04-text-input-dialog-result.png` | OK で閉じた後の「結果: completed("Hello")」 |
| 5 | `inline-dialog` | `ios-05-inline-dialog-shown.png` | 本文「インライン表示です」のダイアログ |
| | | `ios-05-inline-dialog-result.png` | 閉じた後の「結果: completed(true)」 |
| 6 | `transition-dialog` | `ios-06-transition-dialog-panel.png` | 起動直後のデモ画面。プリセット 8 件 (Fade 選択済み)、時間 250 ms、イージング 4 件 (Standard 選択済み)、下端に「表示」 |
| | | `ios-06-transition-dialog-shown.png` | 「表示」を 1 回タップして出したダイアログ。本文「トランジションのデモです」 |
| | | `ios-06-transition-dialog-result.png` | OK で閉じた後、デモ画面の結果表示が「結果: completed(true)」に変化 |
| 7 | `layout-dialog` | `ios-07-layout-dialog-panel.png` | 起動直後の属性調整パネル。Horizontal / Vertical とも Center、OffsetX / OffsetY が 0、Use visible area が on、右上に「Show」 |
| | | `ios-07-layout-dialog-shown.png` | 「Show」を 1 回タップして出したダイアログ。本文「レイアウト確認」 |
| | | `ios-07-layout-dialog-result.png` | 「キャンセル」で閉じた後、パネルの結果表示が「結果: cancelled」に変化 |
| 8 | `default-loading` | `ios-08-default-loading-shown.png` | 起動直後に Loading 開始。スピナーと「Loading...」「25%」(`loading-step-interval-ms 3000`) |
| 9 | `custom-loading` | `ios-09-custom-loading-shown.png` | カスタム Loading。「カスタムローディング」の見出し・プログレスバー・「25%」 |
| 10 | `default-toast` | `ios-10-default-toast-shown.png` | 「Hello Toast!」の Toast を 1 枚表示 |
| 11 | `custom-toast` | `ios-11-custom-toast-shown.png` | カスタム Toast 2 枚。チェック印つきの青い「カスタムトースト」と、薄い背景の「インライントースト」 |
| 12 | `toast-stack` | `ios-12-toast-stack-shown.png` | Toast 3 枚が積み上がった状態。上から「Toast 3: 長いメッセージは…」(3 行折り返し)・「Toast 2」・「Toast 1」 |
| 13 | `toast-placement` | `ios-13-toast-placement-shown.png` | 「Placed Toast」が画面上部中央に表示 |
| 14 | `toast-overlap` | `ios-14-toast-overlap-shown.png` | 時系列の開始。Toast「Overlap Toast」が出ている上にダイアログ「こんにちは、KsDialogs!」が重なっている |
| | | `ios-14-toast-overlap-result.png` | 時系列 (Toast → Dialog → Loading) が終わった後、末尾までスクロールして「結果: 完了」 |

## Android の項目別証跡 (`android/`)

| # | 安定デモ ID | ファイル | 実体 |
|---|---|---|---|
| 1 | `basic-dialog` | `and-01-basic-dialog-shown.png` | メニュー上にダイアログ。本文「こんにちは、KsDialogs!」、ボタン「キャンセル」「OK」 |
| | | `and-01-basic-dialog-result.png` | OK で閉じた後、末尾までスクロールして「直近の結果 / 結果: completed(true)」 |
| 2 | `declarative-dialog` | `and-02-declarative-dialog-shown.png` | 同じ本文のダイアログ |
| | | `and-02-declarative-dialog-result.png` | 閉じた後の「結果: completed(true)」 |
| 3 | `model-dialog` | `and-03-model-dialog-shown.png` | 本文「ViewModel から表示しています」のダイアログ |
| | | `and-03-model-dialog-result.png` | 閉じた後の「結果: completed(true)」 |
| 4 | `text-input-dialog` | `and-04-text-input-dialog-shown.png` | 本文「メッセージを入力してください」と入力欄 (プレースホルダ「ここに入力」) |
| | | `and-04-text-input-dialog-typed.png` | `Hello` を入力し、ソフトキーボードを閉じた状態 (入力中の下線が消えて確定済み) |
| | | `and-04-text-input-dialog-result.png` | OK で閉じた後の「結果: completed("Hello")」 |
| 5 | `inline-dialog` | `and-05-inline-dialog-shown.png` | 本文「インライン表示です」のダイアログ |
| | | `and-05-inline-dialog-result.png` | 閉じた後の「結果: completed(true)」 |
| 6 | `transition-dialog` | `and-06-transition-dialog-panel.png` | 起動直後のデモ画面。iOS と同じ構成・文言 (Fade / 250 ms / Standard が既定) |
| | | `and-06-transition-dialog-shown.png` | 「表示」を 1 回タップして出したダイアログ。本文「トランジションのデモです」 |
| | | `and-06-transition-dialog-result.png` | OK で閉じた後、デモ画面の結果表示が「結果: completed(true)」に変化 |
| 7 | `layout-dialog` | `and-07-layout-dialog-panel.png` | 起動直後の属性調整パネル。iOS と同じ既定値 |
| | | `and-07-layout-dialog-shown.png` | 「Show」を 1 回タップして出したダイアログ。本文「レイアウト確認」 |
| | | `and-07-layout-dialog-result.png` | 「キャンセル」で閉じた後、パネルの結果表示が「結果: cancelled」に変化 |
| 8 | `default-loading` | `and-08-default-loading-shown.png` | Loading 開始。スピナーと「Loading...」「25%」 |
| 9 | `custom-loading` | `and-09-custom-loading-shown.png` | カスタム Loading。「カスタムローディング」・プログレスバー・「50%」(撮影時点が iOS より 1 段進んでいる) |
| 10 | `default-toast` | `and-10-default-toast-shown.png` | 「Hello Toast!」の Toast を 1 枚表示 (フェードイン途中で、背景がわずかに明るい) |
| 11 | `custom-toast` | `and-11-custom-toast-shown.png` | カスタム Toast 2 枚。「カスタムトースト」(青・チェック印) と「インライントースト」 |
| 12 | `toast-stack` | `and-12-toast-stack-shown.png` | Toast 3 枚。上から「Toast 3: 長いメッセージは…」(2 行折り返し)・「Toast 2」・「Toast 1」 |
| 13 | `toast-placement` | `and-13-toast-placement-shown.png` | 「Placed Toast」が画面上部中央に表示 |
| 14 | `toast-overlap` | `and-14-toast-overlap-shown.png` | 時系列の開始。Toast「Overlap Toast」の上にダイアログ「こんにちは、KsDialogs!」が重なっている |
| | | `and-14-toast-overlap-result.png` | 時系列が終わった後の「結果: 完了」 |

## 判定

- 両 OS とも 14 件すべてが sample-parity 規約の「起動直後の状態」を示した (ダイアログ表示 5 件 / デモ画面 1 件 / 属性調整パネル 1 件 / Loading 開始 2 件 / Toast 表示 4 件 / 時系列の開始 1 件 — 内訳は同規約の表のとおり)
- `transition-dialog` / `layout-dialog` は起動直後にダイアログを出す項目ではない。両 OS とも、開いた画面から追加操作でダイアログを 1 回ずつ表示できた (上の内訳には数えていない)
- 結果を返す Dialog 系 (`basic` / `declarative` / `model` / `text-input` / `inline` / `transition` / `layout` / `toast-overlap`) は、両 OS とも閉じたときに結果表示が変化した。結果表示エリアは一度もダイアログを閉じていない間は非表示 (`IsVisible=False`) なので、非表示 → 表示 + 値の出現が遷移の証跡になる
- MAUI 本体 10.0.20 での回帰は見つからなかった (Android 側は引き下げ後の初回通し)

## 観測時に分かったこと (仕様の理解として)

- `toast-overlap` の Dialog は 2 秒で自動的に閉じる (`ToastOverlapDialogDurationMs`)。iOS の `ios-14-...-shown.png` を撮った後に OK をタップしたが、この項目の結果表示「結果: 完了」は時系列そのものの完了によるもので、タップの有無に依らない。Android では何も操作せずに同じ結果になった
- Android では Toast の表示が起動から 7 秒前後で始まる (エミュレータのコールド起動が遅い)。固定待ち時間では撮り逃すため、Toast 4 件は起動後に連続撮影して Toast が写っているコマを採用した。採用したコマ以外は証跡に残していない
