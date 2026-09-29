# 自動再生の起動位置を揃えた後の実測 (align-sample-autoplay-start)

## 環境と手順

- 環境: API 36 (google_apis、arm64) の Android エミュレータ (pixel_7 の定義、1080x2400) と、iPhone Air / iOS 26.5 の Simulator。どちらもこの実測のために新しく作り、実測後に削除した。4 Sample とも Debug ビルド (MAUI Android は `EmbedAssembliesIntoApk=true`。後述)
- 対象: `samples/android` (以下 android)、`samples/kmp/androidApp` (以下 kmp (Android))、`samples/maui` の Android (MAUI Android) と iOS (MAUI iOS)
- ライブラリ: wait-for-host-appearance の実装後の作業ツリー (提示先の出現待ちを持つ)
- 起動: iOS は `xcrun simctl terminate` → 1.5 秒待ち → `xcrun simctl launch <bundle-id> --demo <デモID> --loading-step-interval-ms 5000`。Android は `am force-stop` → 1.5 秒待ち → `am start -n <package>/<activity> --es demo <デモID> --es loading-step-interval-ms 5000`
- 撮影 (起動コマンドの発行からの経過):
  - Dialog・Loading: 約 1.5 秒の待ちを挟んで 5 コマ
  - Toast: 待ちを挟まずに連続で撮る。1 回目の実測は 10 コマ、2 回目以降は 20 コマ。Android の撮影は 1 コマに 0.3〜数秒かかる (エミュレータの負荷で変わる) ため、コマの間隔は一定ではない
- 判定: 全コマを縮小して並べた一覧を目視し、指定デモの「起動直後の状態」(sample-parity「安定デモ ID」表) が出たかを見た。`custom-toast` は登録経路 (「カスタムトースト」) とインライン経路 (「インライントースト」) を分けて数えた。落ちたかどうかは、最後の撮影後にプロセスが残っているか (iOS は `launchctl list`、Android は `pidof`) と Android の crash バッファが空であることで判定した
- 表の「出た / 試行」は、起動直後の状態が 1 コマ以上に写り、落ちなかった回数 / 起動回数。消えかけ (半透明) で 1 コマだけ写った回は「出た」に数え、括弧で内数を示す
- 実測の間、Mac 全体の負荷が大きく変動した (1 回目の実測の開始時は load average が 100 前後、2 回目・直す前の比較は 6〜12)。Android の初回描画までの時間はこの負荷に強く引きずられる

## 1 回目の実測 (全 5 デモ、変更後)

> 注記: この節と 2 回目の節の「変更後」、およびそれらの画像 (1 回目の Android 系の Dialog・Loading と `refire-*` を含む) は、Android 系 3 ルートの待ちを外した途中の版で撮った。最終版は deviation.md のとおり Android 系で待ちを戻しており (3 回目の節)、android と kmp (Android) の実行時の動きは直す前と同じ `post`、MAUI Android は直す前と同じく UI スレッドの次の周回へ回す (`Dispatch` を `DispatchAsync` にした)。MAUI iOS の行は最終版と同じ形。作り直しで再発火しないことの守り (`savedInstanceState` の判定と 1 回限りの取り出し) は、最終版でも変わらない。

| Sample | basic-dialog | default-loading | custom-loading | default-toast | custom-toast (登録 / インライン) |
|---|---|---|---|---|---|
| android | 3 / 3 | 3 / 3 | 3 / 3 | **0 / 5** | 5 / 5 / 5 / 5 |
| kmp (Android) | 3 / 3 | 3 / 3 | 3 / 3 | **2 / 5** | 5 / 5 / **3 / 5** |
| MAUI Android | 3 / 3 | 3 / 3 | 3 / 3 | 4 / 5 (消えかけ 1) | **4 / 5** / **1 / 5** |
| MAUI iOS | 3 / 3 | 3 / 3 | 3 / 3 | 5 / 5 | 5 / 5 / 5 / 5 |

- Dialog・Loading は 4 ルートとも全回で出た。落ちた回は無い
- MAUI iOS は Toast も全回で出た (起動画面「.NET」の後、メニューが出た最初のコマから 2〜4 コマ続けて写る)
- Android の 3 ルートは Toast に出ない回がある (太字)。出た回も、メニューが最初に写ったコマの 1〜2 コマだけで、消えかけで写る回が多い
- MAUI Android の最初の実測は、Debug ビルドの高速配備 (アセンブリを APK の外に置く) の APK を `adb install` で入れたため、起動直後に `No assemblies found ... Fast Deployment` で落ちた (19 回すべて)。`EmbedAssembliesIntoApk=true` でビルドし直して全デモを撮り直したものを上表に載せた。Sample の変更とは無関係

## 2 回目の実測と、直す前との比較 (Toast 2 デモ、Android 3 ルート)

Toast だけが出ない回を持つため、Android の 3 ルートで Toast の 2 デモを 5 回ずつ撮り直し (変更後)、続けて同じ条件で**直す前の Sample** (再生を `post` / `Dispatch` で後ろへ回す版) も撮った。直す前の版は、3 ファイルを一時的に HEAD の内容へ戻してビルドした APK で撮り、撮影後に作業中の内容へ戻して md5 の一致を確かめた (直す前の版の APK は変更後の版で上書きして入れ直した)。

| Sample | 版 | default-toast | custom-toast 登録経路 | custom-toast インライン経路 | 起動から初回描画まで (`Displayed`) |
|---|---|---|---|---|---|
| android | 変更後 (`onCreate` から直接) | 3 / 5 (消えかけ 1) | 4 / 5 (消えかけ 2) | **0 / 5** | (取り損ね) |
| android | 直す前 (`post`) | 3 / 5 | 4 / 5 (消えかけ 1) | 3 / 5 | 1.8〜2.7 秒 (1 回だけ 30.6 秒) |
| kmp (Android) | 変更後 (`onCreate` から直接) | **1 / 5** | 4 / 5 | **2 / 5** | (取り損ね) |
| kmp (Android) | 直す前 (`post`) | 5 / 5 | 5 / 5 | 5 / 5 | 1.6〜2.4 秒 |
| MAUI Android | 変更後 (`OnAppearing` から直接) | 3 / 5 (消えかけ 1) | **1 / 5** | **0 / 5** | 7.2〜11.2 秒 (4 回ぶん) |
| MAUI Android | 直す前 (`OnAppearing` から `Dispatch`) | 2 / 5 | 5 / 5 | 3 / 5 | 4.5〜7.2 秒 |

- `Displayed` は `ActivityTaskManager` の「Displayed ... +<時間>」(Activity の起動から最初の画面の描画まで)。変更後の android・kmp (Android) の回は、抽出の書式を誤って値を取り損ねた
- MAUI Android の変更後 default-toast の 1 回目は、起動の完了が遅れて OS の「応答なし」ダイアログが出た (`am_anr` の理由は `failed to complete startup`。プロセスの起動処理が期限内に終わらなかったもの)。この回は Toast が写らず、上表では出なかった回に数えた
- kmp (Android) は直す前の版が 3 列とも全回で出て、変更後は出ない回が多い。MAUI Android の custom-toast も同じ傾向。android は直す前の版でも default-toast に出ない回があり、差ははっきりしない

## 出ない回の再現の条件 (見立て)

Sample の修正で逃げずに、観測から読み取れる条件を書く。

- 起きる Sample: Android の 3 ルート (android・kmp (Android)・MAUI Android)。iOS 系 (MAUI iOS、および wait-for-host-appearance の実測の ios・kmp (iOS)) では起きていない
- 起きるデモ: Toast だけ。表示時間の短い Toast ほど出ない — 既定の表示時間 (内蔵既定 1500 ms) の default-toast と、2000 ms のインライン経路で目立ち、3000 ms の登録経路では少ない。寿命を持たない Dialog と、処理の間出続ける Loading では起きない
- 起きる時機: 起動直後。メニューの最初の描画 (`Displayed`) までに 1.6〜11 秒かかる状態で、その時間が長いほど出ない回が増える (Mac の負荷が大きかった 1 回目の実測で android の default-toast が 0 / 5)
- 仕組みの見立て: Toast の表示時間は受理の時点から数える (wait-for-host-appearance の toast-contract の Scenario「受理時点から数えた duration の到達で消える」)。Android の提示先は再開した Activity で、Activity の再開は最初の描画と起動画面 (スプラッシュ) の退場より前に来る。そのため Toast は「提示先が現れた」扱いで起動画面の下に置かれ、利用者に見える前に表示時間の大半を使い切る。自動再生を `onCreate` (MAUI は `OnAppearing` の同期部分) から呼ぶと、受理がさらに早まり (直す前の `post` は最初の描画の後、`Dispatch` は次の周回まで受理を遅らせていた)、見えるまでの間に消費する時間が延びる
- 手動でメニューをタップした場合は、画面が描画された後に受理されるため、この条件に当たらない

## 画像

| ファイル | 内容 |
|---|---|
| `android-basic-dialog.png` | android、basic-dialog の 2 回目 (起動から約 8.7 秒後)。ダイアログが出ている |
| `android-default-loading.png` | android、default-loading の 1 回目 (約 10.8 秒後)。既定 Loading が出ている |
| `android-custom-loading.png` | android、custom-loading の 1 回目 (約 12.1 秒後)。カスタム Loading が出ている |
| `android-default-toast.png` | android、2 回目の実測の default-toast の 1 回目。メニューの最初のコマで「Hello Toast!」が出ている |
| `android-custom-toast.png` | android、2 回目の実測の custom-toast の 3 回目。登録経路の「カスタムトースト」だけが出ていて、インライン経路は写っていない |
| `kmp-android-basic-dialog.png` | kmp (Android)、basic-dialog の 2 回目。ダイアログが出ている |
| `kmp-android-default-loading.png` | kmp (Android)、default-loading の 1 回目。既定 Loading が出ている |
| `kmp-android-custom-loading.png` | kmp (Android)、custom-loading の 1 回目。カスタム Loading が出ている |
| `kmp-android-default-toast.png` | kmp (Android)、2 回目の実測の default-toast の 4 回目。「Hello Toast!」が出ている (5 回中この回だけ) |
| `kmp-android-default-toast-missing.png` | kmp (Android)、2 回目の実測の default-toast の 2 回目。メニューが出た最初のコマで、Toast は写っていない (以降のコマにも写らない) |
| `kmp-android-custom-toast.png` | kmp (Android)、2 回目の実測の custom-toast の 5 回目。登録経路とインライン経路の 2 枚が出ている |
| `maui-android-basic-dialog.png` | MAUI Android、basic-dialog の 2 回目。ダイアログが出ている |
| `maui-android-default-loading.png` | MAUI Android、default-loading の 2 回目。既定 Loading が出ている |
| `maui-android-custom-loading.png` | MAUI Android、custom-loading の 1 回目。カスタム Loading が出ている |
| `maui-android-default-toast.png` | MAUI Android、2 回目の実測の default-toast の 3 回目。起動画面が退いた最初のコマで「Hello Toast!」が出ている |
| `maui-android-custom-toast.png` | MAUI Android、2 回目の実測の custom-toast の 3 回目。登録経路だけが出ている (5 回中この回だけ) |
| `maui-android-custom-toast-missing.png` | MAUI Android、2 回目の実測の custom-toast の 1 回目。起動画面が退いた最初のコマで、Toast は写っていない |
| `maui-ios-basic-dialog.png` | MAUI iOS、basic-dialog の 1 回目 (約 3.8 秒後)。ダイアログが出ている |
| `maui-ios-default-loading.png` | MAUI iOS、default-loading の 1 回目 (約 3.7 秒後)。既定 Loading が出ている |
| `maui-ios-custom-loading.png` | MAUI iOS、custom-loading の 1 回目 (約 5.4 秒後)。カスタム Loading が出ている |
| `maui-ios-default-toast.png` | MAUI iOS、default-toast の 1 回目 (約 2.3 秒後)。「Hello Toast!」が出ている |
| `maui-ios-custom-toast.png` | MAUI iOS、custom-toast の 1 回目 (約 2.6 秒後)。2 枚が出ている |
| `refire-1-launched.png` | 再発火の確認 (下節)。android を basic-dialog で起動した後。ダイアログが出ている |
| `refire-2-rotated.png` | 同。`OK` で閉じてから横向きへ回した後。ダイアログは出ていない |
| `refire-3-recreated.png` | 同。文字の大きさの設定を変えて Activity を作り直させた後。文字が大きくなり、ダイアログは出ていない |

## 画面・Activity の作り直しで再発火しないこと (android)

1. `--es demo basic-dialog` で起動し、ダイアログが出たことを撮った (`refire-1-launched.png`)
2. `OK` をタップして閉じた (結果表示は `結果: completed(true)`)
3. `settings put system user_rotation 1` で横向きへ、`0` で縦向きへ戻した。android の `MainActivity` は `configChanges` に向きを持つため Activity は作り直されない。どちらの後もダイアログは出なかった (`refire-2-rotated.png`)
4. 回転では作り直しが起きないため、`configChanges` に含まれない文字の大きさの設定 (`settings put system font_scale 1.15`、続けて `1.0`) を変えて Activity を 2 回作り直させた。イベントログに、同じプロセスのまま `wm_relaunch_resume_activity` → `wm_on_destroy_called` → `wm_on_create_called` が 2 組記録された。どちらの後もダイアログは出なかった (`refire-3-recreated.png`)。作り直しでは `savedInstanceState` が渡るため自動再生の分岐に入らず、プロセスで 1 回限りの取り出しも消費済みである
5. 作り直しの後は、直近の結果の表示が消えてメニューだけになる。これは直す前の Sample から同じ (結果表示を作り直しで引き継がない作り) で、この change の範囲外

## 3 回目の実測: Android 系に最初の描画の後の待ちを戻した後 (Toast 3 デモ)

オーナー判断 (deviation.md) を受けて、Android 系 3 ルートは再生を後ろへ回す Sample 側の待ちを戻した。android と kmp (Android) は `onCreate` から受け付けて `menuView.post` で最初の描画の後に再生する。MAUI は Android のときだけ `Dispatcher.DispatchAsync` で UI スレッドの次の周回へ回し、iOS は `OnAppearing` から待たずに再生する (iOS の形は 1 回目の実測から変えていない)。Dialog・Loading と MAUI iOS は撮り直していない。

- 環境: 1・2 回目と同じ定義で作り直した API 36 のエミュレータ (1・2 回目の端末は削除済み)。作成直後の初回起動の遅さを除くため、各 Sample を 1 回ずつ起動してから撮った (数えていない)
- 手順・判定は 2 回目と同じ (Toast は 20 コマ連続)。Mac の負荷は load average 6〜7

| Sample | 版 | default-toast | custom-toast 登録経路 | custom-toast インライン経路 | 起動から初回描画まで (`Displayed`) |
|---|---|---|---|---|---|
| android | 待ちを戻した後 (3 回目) | 3 / 5 | 5 / 5 | 5 / 5 | custom-toast は 1.8〜2.1 秒、default-toast は 2.0〜6.9 秒 |
| android | 直す前 (2 回目の比較) | 3 / 5 | 4 / 5 (消えかけ 1) | 3 / 5 | 1.8〜2.7 秒 |
| kmp (Android) | 待ちを戻した後 (3 回目) | 5 / 5 | 5 / 5 | 5 / 5 | 1.8〜2.9 秒 |
| kmp (Android) | 直す前 (2 回目の比較) | 5 / 5 | 5 / 5 | 5 / 5 | 1.6〜2.4 秒 |
| MAUI Android | 待ちを戻した後 (3 回目) | 4 / 5 (消えかけ 1) | 5 / 5 | 5 / 5 | 3.8〜8.0 秒 |
| MAUI Android | 直す前 (2 回目の比較) | 2 / 5 | 5 / 5 | 3 / 5 | 4.5〜7.2 秒 |

- 3 ルートとも、直す前の版と同じかそれ以上の回数で出た。落ちた回・応答なしの回は無い
- android の default-toast で出なかった 2 回は、初回描画までが 4.8 秒・6.9 秒と長かった回である (出た 3 回は 2.0〜3.4 秒)。直す前の版でも default-toast は 3 / 5 で、最初の描画の後に回しても、描画から起動画面の退場までの間に 1500 ms の表示時間が尽きる回は残る
- android と kmp (Android) の起動直後のコマは、ステータスバーがまだ描かれておらず、両 Sample の画面が同じ文言・同じ色のため、別ルートのコマが同一画素になることがある。下の画像は、このディレクトリ内で md5 が重ならないコマを選んだ

### 3 回目の画像

| ファイル | 内容 |
|---|---|
| `third-android-default-toast.png` | android、default-toast の 1 回目。「Hello Toast!」が出ている |
| `third-android-custom-toast.png` | android、custom-toast の 1 回目。登録経路とインライン経路の 2 枚が出ている |
| `third-kmp-android-default-toast.png` | kmp (Android)、default-toast の 2 回目。「Hello Toast!」が出ている |
| `third-kmp-android-custom-toast.png` | kmp (Android)、custom-toast の 1 回目。登録経路とインライン経路の 2 枚が出ている |
| `third-maui-android-default-toast.png` | MAUI Android、default-toast の 2 回目。起動画面が退いた最初のコマで「Hello Toast!」が出ている |
| `third-maui-android-custom-toast.png` | MAUI Android、custom-toast の 1 回目。登録経路とインライン経路の 2 枚が出ている |
