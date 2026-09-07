# Android: タッチ素通し Window と IME・システムジェスチャの干渉確認

Android native (tasks 3.7) の実機確認の証跡。取得日 2026-08-27。

## 環境

- エミュレータ 1台 (API 35)・縦向き・ジェスチャナビゲーション
- 実行: `adb -s <android-serial> shell am instrument -w -e class jp.kamusoft.ksdialogs.ToastSystemInputTests -e ksdialogsEvidence 1 jp.kamusoft.ksdialogs.test/androidx.test.runner.AndroidJUnitRunner`
- 結果: `OK (2 tests)`
- 同じ 2 件は実機 (API 33) でも成功する (`connectedDebugAndroidTest` の全件実行に含まれる)

判定は `ToastSystemInputTests` が機械的に行い、画像は「そのときどう見えたか」の記録である。

## 判定した内容

| 観点 | 判定の方法 | 結果 |
|---|---|---|
| IME を出せる | Toast を出す前と後で、入力欄にフォーカスして IME を出し、ウィンドウの insets の ime が可視になるまで待つ | 前後とも可視になる |
| IME を引っ込められる | 同じく insets の ime が不可視になるまで待つ | 前後とも不可視になる |
| IME が Toast を消さない | IME の出し入れの前後で器が取り付いたままか | 取り付いたまま |
| 入力欄がフォーカスを失わない | IME 表示中に入力欄の `hasFocus()` | true (Toast のウィンドウはフォーカスを奪わない) |
| 戻るが画面へ届く | Toast 表示中に戻るを起こし、画面の戻るの受け口が呼ばれた回数が 1 増えるまで待つ | 1 増える |
| 戻るが Toast を消さない | 戻るの後で器が取り付いたままか | 取り付いたまま |
| ホームが通る | Toast 表示中にホームを起こし、画面が前面から外れた回数が 1 増えるまで待つ | 1 増える |
| ホームで戻るが二重に届かない | ホームの後の戻るの回数 | 変わらない |

戻る・ホームは、実機のジェスチャと同じ入口 (システムが受け取る全体操作) を計測から起こしている。

## 画像

| ファイル | 状態 |
|---|---|
| [toast-ime-idle.png](toast-ime-idle.png) | Toast 表示中・IME なし |
| [toast-ime-shown.png](toast-ime-shown.png) | Toast 表示中・IME 表示中 |
| [toast-ime-hidden.png](toast-ime-hidden.png) | Toast 表示中・IME を引っ込めた後 |
| [toast-gesture-before.png](toast-gesture-before.png) | 戻るを起こす前 |
| [toast-back-delivered.png](toast-back-delivered.png) | 戻るが画面へ届いた後 (Toast は残っている) |

- IME のウィンドウは画面の撮影に写らない (別ウィンドウのため)。IME が出ているかどうかの判定は
  上表のとおり insets で行っており、画像は「Toast が消えていないこと」の記録として読む
- ホーム後の画面はランチャーになるため撮影していない (判定は上表の回数で行っている)
- 画像に個人・端末を特定する要素が写っていないことを保存前に確認済み
