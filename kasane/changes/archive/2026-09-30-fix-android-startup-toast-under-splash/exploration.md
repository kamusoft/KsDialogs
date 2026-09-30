# Exploration: fix-android-startup-toast-under-splash

(簡易起票 2026-09-29。align-sample-autoplay-start の実機での確認で発見)

## 課題 / 動機

Android で、アプリの起動直後 (最初の画面の表示時の処理) に表示時間の短い Toast を出すと、画面に見えないまま終わる回が多い。

- 観測: Sample の自動再生から Sample 側の待ち (`post` / `Dispatch` で受理を最初の描画の後まで遅らせる処理) を外すと、Android 系 3 ルートで起動直後の Toast が出ない回が増えた。kmp (Android) の default-toast は待ちありで 5/5 → 待ちなしで 1/5、MAUI Android の登録経路の custom-toast は 5/5 → 1/5、Android のインライン経路の custom-toast は 3/5 → 0/5。Dialog と Loading は 4 ルートとも全回で表示され、iOS 系では起きていない。実測は `kasane/changes/archive/2026-09-29-align-sample-autoplay-start/evidence/autoplay-measurement.md`
- 見立て (未検証): Toast の表示時間は受理の時点から数える (core の toast-semantics、wait-for-host-appearance の TS-HW-01)。Android の提示先は resumed な Activity で、これは最初の描画と起動画面の退場より前にそろう。そのため Toast は起動画面の下で表示時間を使い切る。表示時間の短い Toast ほど、起動から最初の描画までが長い構成 (MAUI) ほど出ない
- Sample 側の待ちでも隠しきれない: 待ちを戻した版 (受理を最初の描画の後まで遅らせる) でも、android の default-toast (1500 ms) は 3/5 で、直す前の版も 3/5 だった。出なかった 2 回は起動から最初の描画までが 4.8 秒・6.9 秒と長い回で、最初の描画から起動画面の退場までの間に表示時間が尽きると見ている (同じ evidence の「3 回目の実測」節)
- 利用者への影響: Sample に限らず、利用者のアプリが Android の起動直後に Toast を出すと見えずに終わりうる。エラーにもならないので気づきにくい

## 考えられる方向 (簡易起票時点)

- Android の提示先の条件を、resumed に加えて最初の描画 / window のフォーカスまで含める
- Toast の表示時間を、受理の時点ではなく画面に載った (または見える) 時点から数える
- どちらも core/ADR-0041 と toast-contract (TS-HW-01「受理時点から数えた duration」) に手が入り、全形態の揃えに関わる。iOS の提示先の条件 (前面でアクティブなシーンの key window) との対称も見る

## 検討した選択肢 (却下案と理由を含む)

### 見立て: 片方だけでは直らない (2026-09-29 探索)

起動直後の Android の順は「受理 (最初の画面の表示時の処理) → resumed (今の提示先の条件) → 最初の描画 → 起動画面の退場 (ここで初めて見える)」。表示時間は受理時点から数える (toast-semantics「duration の時間モデル」・TS-HW-01。Android は受理時に期限を決める `ToastCoordinator`) ので、見える時間 = 表示時間 −(受理から起動画面の退場まで)。

- 提示先の条件を「見えている画面」に強めるだけ: 式は変わらない。起動画面の下で消える表示が「表示されずに破棄」(TS-HW-03) に変わるだけで、見える Toast は増えない
- 数え方を「画面に載った時点から」に変えるだけ: 今の Android では載る時点 = resumed = 起動画面の下なので、見える前に時間を使う
- → 両方が要る

### 論点 1: Toast の表示時間をいつから数えるか

| 軸 | ア 今のまま受理時点から | イ 全 Toast を画面に出た時点から | ウ 準備中に受け付けたものだけ見えた時点から | エ 提示先が無い間は時間を止める |
|---|---|---|---|---|
| 起動直後の Toast が見えるか | 増えない (Sample の待ちを外せない) | 表示時間いっぱい | 表示時間いっぱい | 表示時間いっぱい |
| 背面にいる間に呼んだ Toast | 今のまま (背面でも時間が進み、戻る頃には期限切れで出ない) | 戻ったときにまとめて出る (古い通知が溜まる) | 今のまま | 戻ったときに残り時間ぶん出る (古い通知が出る) |
| 規則のわかりやすさ | 今のまま | 最も単純 | 「準備中」の判定が 1 つ増える | 「止まる」条件の説明が要る |
| 契約・ADR の改訂範囲 | なし | 大 (Toast も上限なしで待つ。ADR-0041 の Toast の寿命、TS-HW-01・03) | 中 (ADR-0041 の Toast の寿命に準備中の例外、TS-HW-01・03) | 中〜大 (「背面の間も時間が進む」を捨てる) |

オーナーが **ウ** を採用 (2026-09-29)。ア・イ・エの却下理由は上表のとおり。

### 論点 2 の材料: Android で「見えた」を知る合図 (ksn-scout 調査 2026-09-29)

- 起動画面 (starting window) の除去はアプリのプロセスに通知されない。ライブラリから観測できる最も近い合図は、decorView の実際の描画 (`ViewTreeObserver.OnDrawListener`)。フレームの確定 (`registerFrameCommitCallback` (API 29+) か次の `Choreographer` フレーム) を足すのが現実的
- `OnPreDrawListener` は使えない: core-splashscreen の `setKeepOnScreenCondition` が PreDraw で描画を止める仕組みで、延長中でも PreDraw は来る。`OnDrawListener` は延長中は来ず、延長が終わってから来る (望ましい)
- フォーカスは主条件にしない: マルチウィンドウ / 分割画面 (resumed でもフォーカスは 1 つ)・通知シェード・ライブラリ自身の Dialog や利用者の AlertDialog / focusable な PopupWindow が前面にあるときに、誤って「提示先なし」になる
- `onTopResumedActivityChanged` / `onEnterAnimationComplete` は ActivityLifecycleCallbacks から観測できない。`Window.Callback` の差し替えは前例 (square/curtains) があるが、AppCompat や計測 SDK との重ね包みで壊れやすい。`splashScreen.setOnExitAnimationListener` は利用者専用 (listener は 1 つで上書きになる)
- ラッチ案: Activity のインスタンス単位で、最初の実際の描画で立てる。pause だけからの復帰 (透過 Activity・権限ダイアログ・分割画面) では再描画が起きないことがあるので、resume 時に `invalidate()` で描画を誘発するか、stop / destroy でだけ下ろす。`OnDrawListener` の中では listener の追加・削除ができないので post してから外す
- 残る先走り: 最初の描画で立てても、起動画面の除去と API 31+ の退場アニメーションのぶん (数百 ms と推測、未実測) はまだ先に進んでしまう。器 (Dialog) 自身の描画が除去の引き金になり得るかは未確認。実測で詰める

## 決定事項

- 論点 1 (2026-09-29): 画面の準備中に受け付けた Toast は、画面が利用者に見えた時点から表示時間を数える。準備中以外 (背面にいる間など) に受け付けた Toast は従来どおり受理時点から実時間で数える。全形態で揃える (core/ADR-0043)
- 蒸留時に反映: core/api/toast-semantics.md — 「duration の時間モデル」と失敗モデルの「提示環境の不在」行 (TS-HW-01・03) を、準備中の例外を含む形に書き換える
- 蒸留時に反映: core/ADR-0041 — frontmatter に `amended-by: 0043`、index 行に「一部改訂: 0043」
- 論点 2 (2026-09-29): Android の提示先の条件を「resumed」から「resumed で、かつ描画された画面」に変える (見えてから載せる)。iOS の提示先 (前面でアクティブなシーンの key window) と同じ「利用者に見えている画面」の意味に揃え、ADR-0043 の「見えた時点から数える」を「載った時点から数える」と一致させる。却下: 提示先は resumed のまま準備中の Toast の数え始めだけを描画に合わせる案 (載る合図と数え始めの合図が 2 本に分かれ、入りの演出が起動画面の下で終わる)。ADR は論点 3 (適用範囲) の後に起票する
  - 合図は画面の実際の描画 (フレームの確定まで) を前提にする。フォーカスは誤判定の場面が多いので使わない。一定時間の上乗せは値の根拠を置きにくいので採らない。起動画面の退場アニメーション分の先走りが実測で目立てば再相談
- 論点 3 (2026-09-29): Dialog・Loading も同じ条件 (resumed で、かつ描画された画面) で待つ。追跡役の定義ごと変え、Android の提示先を 3 機能で 1 つに保つ (core/ADR-0044)。却下: Toast だけ描画を待つ案 (範囲・被害は小さいが、Android の提示先の定義が機能で 2 通りに分かれる)
  - 最大のリスクは描画の合図の取りこぼしで Dialog が出ないまま返らないこと。描画済みの印は Activity ごとに持ち、stop・破棄でだけ下ろす (pause だけからの復帰では再描画が無いことがあるため)。pause だけからの復帰・背面からの復帰・作り直しをテストで押さえる
- 論点 5 (2026-09-29): 「画面の準備中」は、画面を開いている途中すべてとする (ADR-0043 の Decision の範囲内の場合分けなので ADR にしない)。Android はコールド起動・プロセスが残ったままの再起動・画面の切り替え (次の画面の表示時の処理)・背面から戻る途中・作り直しの途中を含む。iOS は起動と背面から戻る途中 (シーンがアクティブになる前) を含む (iOS は画面の切り替えで key window が変わらないので、切り替えの途中は生じない)。背面にいて開いている途中の画面が無い間は準備中に含めない (受理時点から数える)。却下: 起動のときだけ (プロセスが残ったままの再起動でも Android 12 以降は起動画面が出て、同じ問題が残る)
  - **提案作成で範囲を広げた (2026-09-29、オーナー判断)**: iOS のライブラリは起動時に動き出す仕組みを持たず、シーンの通知も表示を待つ間だけ購読するので、最初の呼び出しの時点で「起動の途中」と「割り込み」を見分ける履歴が無い。範囲を「アプリが前面にいるのに提示先が無い間」(割り込みの最中を含む) へ広げ、両 OS とも今の状態だけで判定する。ADR-0043 の本文とタイトルを改訂した (proposed のため直接改訂)。詳細は design.md Decision 2
  - ア・イ共通の扱い: 開いている途中で画面が見えないまま背面に行った (ホームに戻った等) ときは、背面に行った時点から数え始める。準備中の Toast が待ち続けて、戻ったときに古い通知として出るのを防ぐ
- 蒸留時に反映: core/api の提示先の記述 (toast-semantics・loading-semantics・result-notification-semantics、android/api の dialog-surface・toast-surface など「resumed な Activity」と書いている箇所) と handbook/cross/sample-parity.md の自動再生の節 — Android の提示先を「resumed で、かつ描画された Activity」へ。sample-parity の Android 系 3 ルートの例外 (最初の描画の後へ回す) は Sample の修正と合わせて削除

## ADR 候補

- 作成済み: core/ADR-0043 (proposed。0041 の amends)
- 作成済み: core/ADR-0044 (proposed。Android の提示先の条件)

## 未決の論点

- ADR-0041 の前提要確認: 「提示先が現れた = 利用者に見えている」が Android では成り立っていない (resumed は起動画面の下)。論点 1 は ADR-0043 (amends) で扱った。提示先の定義そのもの (0041 では範囲外) は論点 2・3 で扱い ADR-0044 に起票した (0041 の改訂ではなく関連)

## スコープに含めるもの (オーナー指示 2026-09-29)

- ライブラリの振る舞いの見直し
- **Sample の修正**: Android 系 3 ルート (`samples/android`・`samples/kmp/androidApp`・`samples/maui` の Android) に残した「最初の描画の後に再生する」Sample 側の待ちを外し、iOS 系と同じく最初の画面の表示時の処理から呼ぶ形に 4 ルートを揃える (align-sample-autoplay-start で見送った部分)。外した後に Android 系で起動直後の Toast が表示されることを実機で確かめる

## UI 素材

なし (見た目は変えない。変わるのは器を載せる時点と表示時間の数え始め)

## 変更級の推奨: L (オーナー確定 2026-09-29)

- 複数能力の横断: Android の提示先の条件は Dialog・Loading・Toast の 3 機能に効く。Toast の表示時間の数え方は iOS Native・Android Native の両方の実装に手が入る (KMP・MAUI は Native に委譲するので検証のみ)
- 契約の変更: core の Toast の契約 (時間モデル・TS-HW-01・03)、結果通知・Loading の契約の提示先の記述。ADR 2 件 (0043 は accepted の 0041 の amends、0044 は提示先の定義)
- 覆すコストが高い: 公開済みベータ (`0.1.0-beta.1` / `0.1.0-beta.2`) の振る舞いが 3 機能とも変わる。描画の合図を取りこぼすと Dialog が返らない (上限なしの待ち) ので、失敗の被害が大きい
- Sample の修正 (Android 系 3 ルート) と、実機での起動直後の実測 (コールド起動・再起動・切り替え・復帰・作り直し) を含む
- 先例: 同じ規模の wait-for-host-appearance は L 級 (domain: cross)
- 1 変更に収まる: 提示先の条件と数え方は、両方そろって初めて直る組なので分けない

domain: cross
