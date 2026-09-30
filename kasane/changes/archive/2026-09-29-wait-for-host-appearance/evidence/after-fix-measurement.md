# 直した後の実測と、許可ダイアログを閉じた後の順序の観測 (wait-for-host-appearance、tasks 9.3・1.3 の残り)

## 環境と手順

- 環境: iPhone Air / iOS 26.5 の Simulator と、API 36 (google_apis、arm64) の Android エミュレータ。どちらもこの実測のために新しく作り、実測後に削除した。Xcode 26.5、.NET SDK は repo 直下の `global.json` のとおり。4 Sample とも Debug ビルド
- ライブラリ: この change の実装後の作業ツリー (tasks 2〜8 の実装を含む)。Sample は tasks 1.1 の変更後 (自動再生は最初の画面の表示時に始め、シーンの状態を待たない)
- MAUI のビルドが変更後の互換面を積んでいることの確認 (cross/runtime-behavior-verification の「MAUI iOS の互換面を直した後」):
  - iOS: 配備した .app の実行ファイルに、Native の提示先待ちの型 `DialogHostWaitReservation` を引数に持つ `DialogPresenter.present` の mangled 名が載っている (`nm -a`)
  - Android: APK の dex に `observeHostChange` があり、削除した `onPresentationHostUnavailable` が無い (`strings -a`)
- 起動: 1.2 と同じ。iOS は `xcrun simctl terminate` → 1.5 秒待ち → `xcrun simctl launch <bundle-id> --demo <デモID> --loading-step-interval-ms 5000`。Android は `am force-stop` → 1.5 秒待ち → `am start -n <package>/<activity> --es demo <デモID> --es loading-step-interval-ms 5000`
- 撮影 (起動コマンドの発行からの経過):
  - Dialog・Loading: 約 1.5 秒の待ちを挟んで 4 コマ (ios) / 5 コマ (kmp (iOS)・MAUI iOS・MAUI Android)。初回は 0.6〜4.6 秒後、最後は 6.0〜13.7 秒後
  - Toast: 待ちを挟まずに 8 コマ (ios) / 10 コマ (kmp (iOS)・MAUI iOS・MAUI Android)。初回は 0.6〜1.2 秒後、最後は 2.3〜7.3 秒後
- 判定: 全コマを縮小して並べた一覧を目視し、指定デモの「起動直後の状態」(sample-parity「安定デモ ID」表) が出たかを見た。`custom-toast` は 2 枚 (登録経路の「カスタムトースト」とインライン経路の「インライントースト」) が両方出たことを見た。落ちたかどうかは、最後の撮影後にプロセスが残っているか (iOS は `launchctl list`、Android は `pidof` と crash バッファが空であること) と、最後のコマが Sample の画面であることで判定した
- 表の「出た / 試行」は、起動直後の状態が出て落ちなかった回数 / 起動回数

## 9.3 直した後の実測 (1.2 と並べる)

| Sample | basic-dialog | default-loading | custom-loading | default-toast | custom-toast |
|---|---|---|---|---|---|
| ios (直す前・1.2) | 1 / 3 | 0 / 3 | 0 / 3 | 2 / 8 | (撮っていない) |
| **ios (直した後)** | **3 / 3** | **3 / 3** | **3 / 3** | **5 / 5** | **5 / 5** |
| kmp (iOS) (直す前・1.2) | 1 / 3 | 1 / 3 | 0 / 3 | 0 / 5 | (撮っていない) |
| **kmp (iOS) (直した後)** | **3 / 3** | **3 / 3** | **3 / 3** | **5 / 5** | **5 / 5** |
| **MAUI iOS (直した後)** | **3 / 3** | **3 / 3** | **3 / 3** | **5 / 5** | **5 / 5** |
| **MAUI Android (直した後)** | **3 / 3** | **3 / 3** | **3 / 3** | **5 / 5** | **5 / 5** |

- 直す前に出なかった回があったデモ (ios・kmp (iOS) の 4 デモすべて) は、直した後は全回で出た。落ちた回は無い (直す前は basic-dialog で 4 回落ちていた)
- Toast は初回のコマ (起動から 0.6〜1.2 秒後) か、その数コマ後から写り、各回とも複数コマで写った。直す前の ios で見られた「インストール直後の 1・2 回目だけ出る」ような偏りは無い
- MAUI (iOS・Android) では、5 デモとも中身 (Dialog の中身・既定 Loading・カスタム Loading・Toast 2 種) が表示され、中身の供給の失敗 (表示されない・落ちる) は無かった (design Decision 2・7 の見込みどおり)。MAUI は起動画面 (「.NET」の表示) の間に自動再生が呼ばれ、Sample の画面が出た後に表示された
- 直す前の 1.2 は ios の Toast を 8 回撮ったが、今回は 5 回とした

### 画像

各デモの 1 回目から 1 コマずつ選んだ。

| ファイル | 内容 |
|---|---|
| `after-ios-basic-dialog.png` | ios、basic-dialog の 1 回目 (起動から約 10.4 秒後)。ダイアログが出ている |
| `after-ios-default-loading.png` | ios、default-loading の 1 回目 (約 6.1 秒後)。既定 Loading が出ている |
| `after-ios-custom-loading.png` | ios、custom-loading の 1 回目 (約 6.0 秒後)。カスタム Loading が出ている |
| `after-ios-default-toast.png` | ios、default-toast の 1 回目 (約 1.2 秒後)。「Hello Toast!」が出ている |
| `after-ios-custom-toast.png` | ios、custom-toast の 1 回目 (約 1.1 秒後)。2 枚が出ている |
| `after-kmp-basic-dialog.png` | kmp (iOS)、basic-dialog の 1 回目 (約 8.1 秒後)。ダイアログが出ている |
| `after-kmp-default-loading.png` | kmp (iOS)、default-loading の 1 回目 (約 8.2 秒後)。既定 Loading が出ている |
| `after-kmp-custom-loading.png` | kmp (iOS)、custom-loading の 1 回目 (約 13.7 秒後)。カスタム Loading が出ている |
| `after-kmp-default-toast.png` | kmp (iOS)、default-toast の 1 回目 (約 1.4 秒後)。「Hello Toast!」が出ている |
| `after-kmp-custom-toast.png` | kmp (iOS)、custom-toast の 1 回目 (約 1.1 秒後)。2 枚が出ている |
| `after-maui-ios-basic-dialog.png` | MAUI iOS、basic-dialog の 1 回目 (約 10.1 秒後)。ダイアログが出ている |
| `after-maui-ios-default-loading.png` | MAUI iOS、default-loading の 1 回目 (約 7.6 秒後)。既定 Loading が出ている |
| `after-maui-ios-custom-loading.png` | MAUI iOS、custom-loading の 1 回目 (約 7.7 秒後)。カスタム Loading が出ている |
| `after-maui-ios-default-toast.png` | MAUI iOS、default-toast の 1 回目 (約 2.9 秒後)。「Hello Toast!」が出ている |
| `after-maui-ios-custom-toast.png` | MAUI iOS、custom-toast の 1 回目 (約 2.8 秒後)。2 枚が出ている |
| `after-maui-android-basic-dialog.png` | MAUI Android、basic-dialog の 1 回目 (約 9.0 秒後)。ダイアログが出ている |
| `after-maui-android-default-loading.png` | MAUI Android、default-loading の 1 回目 (約 9.0 秒後)。既定 Loading が出ている |
| `after-maui-android-custom-loading.png` | MAUI Android、custom-loading の 1 回目 (約 8.1 秒後)。カスタム Loading が出ている |
| `after-maui-android-default-toast.png` | MAUI Android、default-toast の 1 回目 (約 3.2 秒後)。「Hello Toast!」が出ている |
| `after-maui-android-custom-toast.png` | MAUI Android、custom-toast の 1 回目 (約 3.1 秒後)。2 枚が出ている |

## 1.3 の残り: 許可ダイアログを閉じた後の順序と、その最中に呼んだ 3 機能

ios Sample の App の初期化に一時的な診断出力を入れた (1.3 と同じ観測項目。通知の許可の要求と、下の「呼び出し」を起動引数で切り替える)。観測後に取り除き、`KsDialogsSampleApp.swift` が診断出力を入れる前とチェックサムで一致することを確かめた。kmp (iOS) では観測していない。

- 許可ダイアログを出す手段: 1.3 と同じく、App の初期化で通知の許可を求める
- 呼び出し: 許可ダイアログでシーンが非アクティブになった通知の 0.5 秒後に、Toast (duration 60000 ms)・Loading (スコープ形。処理は 120 秒待つ)・Dialog (Basic Dialog と同じ ViewModel) のどれか 1 つを呼ぶ
- 閉じる操作: シミュレータ制御ツールのタップで、許可ダイアログの「許可」を押した
- 各回とも Simulator を shutdown / boot して Sample を入れ直してから起動した (許可ダイアログが前のプロセスから残らないようにするため。下の「期待と違った点」)

ログの抜粋は `host-appearance-after-alert.log`。

### 閉じた後の順序 (4 回とも同じ)

| 経過 (Toast の回) | 事象 | 提示先の条件 | シーンの状態 |
|---|---|---|---|
| 480 ms | 許可ダイアログが出て、シーンが非アクティブになる通知 | 満たさない (通知の 2 ms 後に見張りが検出) | 前面・非アクティブ |
| 983 ms | Toast を呼ぶ | 満たさない | 前面・非アクティブ |
| 15061 ms | 「許可」のタップ後、許可の結果が届く | 満たさない | 前面・非アクティブ |
| 15488 ms | **シーンがアクティブになった通知** | **満たす** (通知の受け取り時点で既に満たしている) | 前面・アクティブ |

- 閉じた後に条件を満たすのは、4 回ともシーンがアクティブになった通知と同じ時点だった (許可の結果が届いてから約 0.4 秒後)。window が key になった通知は来ない (許可ダイアログの間も window の key は外れていない)。2 つの通知のどちらも来ないまま条件を満たす場面は無かった (design Decision 1 の前提は崩れていない)
- 許可ダイアログが出ている間、条件は満たさないまま変わらなかった (約 10〜15 秒)
- 回 1 は、閉じたのがタップではない (下の「期待と違った点」) が、閉じた後の順序は回 2〜4 と同じだった

### 最中に呼んだ 3 機能

| 呼んだ機能 | 閉じた後 | 画像 |
|---|---|---|
| Toast | 閉じた約 2 秒後の画面に「Hello Toast!」が出ている | `order-after-alert-toast.png` |
| Loading | 閉じた約 2 秒後の画面に既定 Loading (「Loading...」) が出ている | `order-after-alert-loading.png` |
| Dialog | 閉じた約 2 秒後の画面にダイアログが出ている。呼び出しは返っていない (結果待ち) | `order-after-alert-dialog.png` |

- 許可ダイアログが出ている間に呼んだ Loading は、許可ダイアログが出ている間は出ていない (`order-alert-during-loading.png`。予備観測の回で、Loading を呼んだ後、許可ダイアログだけが写っている。この回の診断出力もログの末尾に参考として残した)。Toast・Dialog の回は、下の理由で許可ダイアログが出ている間の画面を撮っていない
- Toast は受理の時点から寿命を数えるので、許可ダイアログが duration より長く出ていれば表示されずに捨てられる (toast-contract のとおり)。今回は duration を 60000 ms にして観測した

## 期待と違った点

- **タップしていないのに「許可」の結果が届き、許可ダイアログが消えることがあった**: 予備観測の 3 回 (Toast・Loading・Dialog を呼んだ回) と本観測の回 1 (機能を呼ばない回) で、操作なしで起動から 1.7〜10.1 秒後に `granted=true` が届いた。そのうち 2 回は `xcrun simctl io screenshot` の直後、1 回は連続撮影の最後のコマの後、1 回 (Dialog の予備観測) は撮影より前だった。一方、撮影しなかった回 2〜4 ではタップするまで約 10〜15 秒間、機能を呼ばずに撮影もしなかった別の予備観測では 20 秒以上、許可ダイアログが残った。原因は調べていない (機能を呼ばない回 1 でも起きたので、ライブラリの呼び出しによるものではないと見ている)。そのため、閉じる操作の観測 (回 2〜4) では許可ダイアログが出ている間の撮影をせず、閉じたのがタップであることを、タップするまで結果が届かなかったことで確かめた
- **前のプロセスの許可ダイアログが残る**: Sample を terminate して入れ直しただけでは許可ダイアログがシステム側に残り、次に起動したプロセスのシーンが一度もアクティブにならなかった (1.3 の観測と同じ)。その状態で「許可」をタップすると、次のプロセスには `granted=false` が届き、続いてシーンがアクティブになる通知と同時に条件を満たした。本観測は毎回 Simulator を再起動して避けた
- 直した後の実測で、期待と違う結果 (出ない回・落ちる回・MAUI の中身の供給の失敗) は無かった
