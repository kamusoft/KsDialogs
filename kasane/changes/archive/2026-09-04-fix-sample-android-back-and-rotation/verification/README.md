# 実機確認: fix-sample-android-back-and-rotation

Android Sample (`samples/android`) と KMP Sample の Android アプリ (`samples/kmp/androidApp`) について、
戻る操作の移行 (`ComponentActivity` + `OnBackPressedCallback`) と回転対応 (`android:configChanges`) を
実機で確認した記録。実施日 2026-09-04。

## 環境

| 記号 | 端末 | Android / API | 画面 |
|---|---|---|---|
| `<DEVICE-A33>` | Pixel 4a | 13 / 33 | 1080x2340 |
| `<DEVICE-A36>` | Pixel 6a | 16 / 36 | 1080x2400 |

- 対象ビルド: `samples/android` の `:app:assembleDebug` と `samples/kmp` の `:androidApp:assembleDebug` (どちらも debug)
- package / activity: `jp.kamusoft.ksdialogs.samples.android/.MainActivity` と `jp.kamusoft.ksdialogs.samples.kmp.android/.MainActivity`

## 手順

デモの状態は Sample の起動引数 (`--es demo <安定デモ ID>`) で作る (`samples/README.md`「撮影のための起動引数」)。

```
adb -s <serial> shell am force-stop <package>
adb -s <serial> shell am start -n <package>/.MainActivity --es demo <layout-dialog | basic-dialog>
adb -s <serial> shell input keyevent 4                     # 戻る
adb -s <serial> shell settings put system accelerometer_rotation 0
adb -s <serial> shell settings put system user_rotation 1  # 横向きへ。観測後は元の値に戻す
adb -s <serial> shell cmd statusbar send-disable-flag notification-icons  # 通知アイコンを隠す。観測後は none で復帰
adb -s <serial> exec-out screencap -p > <保存先.png>
```

画面の遷移先は `adb shell dumpsys activity activities` の `topResumedActivity` で判定した。

観測のために変えた端末設定は、変更前の値へ戻してある:

| 端末 | 設定 | 変更前 | 観測時 | 復帰後 |
|---|---|---|---|---|
| `<DEVICE-A33>` | `system accelerometer_rotation` | 0 | 0 | 0 |
| `<DEVICE-A33>` | `system user_rotation` | 0 | 1 | 0 |
| `<DEVICE-A36>` | `system accelerometer_rotation` | 1 | 0 | 1 |
| `<DEVICE-A36>` | `system user_rotation` | 0 | 1 | 0 |
| 両端末 | 状態バーの `notification-icons` (disable flag) | 既定 (none) | disable | none |

## 結果

| # | 確認項目 | android / API 33 | android / API 36 | kmp / API 33 | kmp / API 36 |
|---|---|---|---|---|---|
| 1 | 修正前: パネルを開いて戻る → メニューへ戻らない | — | 戻らない (アプリが終了) | — | — |
| 2 | 修正後: パネルを開いて戻る → メニューへ戻る | 戻る | 戻る | 戻る | 戻る |
| 3 | 修正後: パネルを閉じた状態で戻る → アプリが終了する | 終了 | 終了 | 終了 | 終了 |
| 4 | 修正後: ダイアログ表示中に戻る → `結果: cancelled` | cancelled | cancelled | cancelled | cancelled |
| 5 | 修正後: ダイアログ表示中に回転 → ダイアログが残り再配置される | 残る | 残る | 残る | 残る |
| 6 | 修正後: パネルを開いた状態で回転 → パネルが崩れない | 崩れない | 崩れない | 崩れない | 崩れない |
| 7 | 修正後: Declarative Dialog (Compose 登録経路) が表示される | — | 表示 | — | 表示 |

補足:

- 項目 1 は探索メモの推定 (targetSdk 36 では予測型戻りが既定で有効になり `onBackPressed` が呼ばれない) の裏取り。
  修正前のビルドでは、パネルを開いた状態の戻るで `topResumedActivity` がランチャーへ移り、メニューへは戻らなかった
- 項目 4 は判定の取り違えを避けるため、直前に `OK` で `結果: completed(true)` にしてから戻るを押し、
  `結果: cancelled` への遷移まで撮っている。この経路は本体の器が受けるもので、今回の変更で挙動は変わっていない
- 項目 3 の「終了」は、戻るで Sample の Activity が前面から消えたことで判定している。遷移先は端末に残っていた
  タスクの並びで決まるため 4 本すべてがランチャーになるとは限らず、`kmp-a36.log` だけは android Sample の
  タスクが同じ端末に残っていたため、kmp Sample の終了後に前面化したのが android Sample になっている
  (他の 3 本はランチャー)。どちらも「kmp / android の Sample 自身は終了した」という主張の裏付けとしては同じ
- 項目 5・6 は回転後も Activity が再生成されず (`configChanges` が効いている)、View も崩れず再レイアウトされた。
  `onConfigurationChanged` の追加対応は要らなかった
- 項目 7 は本変更のレビュー (`../review-001.md`) の実測。API 36 実機の 2 ルート (android / kmp) で
  `--es demo declarative-dialog` を起動し、どちらも Declarative Dialog が正常に描画されることを確認した。
  `ComponentActivity` 化による影響はない (ダイアログのウィンドウ根は自前で owner を据え直すため提示元の基底クラスに依存しない)。
  API 33 は踏んでいないため `—`。画像の証跡は残していない

## 証跡

| ファイル | 内容 |
|---|---|
| `android-a36-before.log` | 項目 1 (修正前・API 36) の遷移先の記録 |
| `android-a33.log` / `android-a36.log` / `kmp-a33.log` / `kmp-a36.log` | 項目 2〜6 を通しで実行したときの遷移先の記録 (ルートの別はこのログの package 名が持つ) |
| `01-android-a36-back-to-menu.png` | 項目 2: パネルから戻ってメニューに居ること |
| `02-android-a36-dialog-completed.png` / `03-android-a36-dialog-back-cancelled.png` | 項目 4: `completed(true)` → 戻る → `cancelled` の遷移 |
| `04-android-a36-dialog-landscape.png` / `05-android-a33-dialog-landscape.png` / `06-kmp-a36-dialog-landscape.png` / `07-kmp-a33-dialog-landscape.png` | 項目 5: 回転後もダイアログが残り、横向きの寸法で中央に再配置されていること |
| `08-android-a36-panel-landscape.png` / `09-kmp-a33-panel-landscape.png` | 項目 6: 回転後もパネルが崩れないこと |

`01`〜`05` / `08` / `09` は、通知アイコンが写り込まないようステータスバーの帯を落としてある。
このうち `09` は kmp ルートだが、android 側に同じ場面 (項目 6・API 33) の画像が無いため取り違えは起きない。

kmp ルートの回転証跡 (`06` / `07`) は、帯を落とすと android の同一場面と画素まで一致してしまい
(Sample パリティ規約どおり 2 ルートの描画が同一で、差が帯の中だけだったため) 画像として区別できなかったため、
`adb shell cmd statusbar send-disable-flag notification-icons` で通知アイコンだけを消し、
時計・電波・電池を残した状態で撮り直した (観測後に `send-disable-flag none` で復帰済み)。
そのため 4 枚はいずれも別ファイルで、時計と端末の画面サイズからどちらの端末・どの観測回かが読める。ルートの別は画像からは読めず、対応する `.log` の package 名が持つ (画面描画は 2 ルートで同一)。
撮影後に画像を目視し、通知・アカウント名・端末名・Wi-Fi 名の写り込みが無いことを確認した。

どのルートを起動した観測かは、いずれの場面も同じ場面の `.log` が併せて持つ。
