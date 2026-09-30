# Proposal: fix-android-startup-toast-under-splash

## Why

Android で、最初の画面の表示時の処理から表示時間の短い Toast を出すと、画面に見えないまま終わる回が多い。エラーにもならないので、利用者は気づきにくい。

原因は 2 つが重なっている:

- Android の提示先は resumed な Activity である。起動直後は「resumed → 最初の描画 → 起動画面の退場」の順に進むので、Toast の器は起動画面の下に載る
- Toast の表示時間は受理の時点から数える (core の Toast の契約の時間モデル)

そのため、見える時間は「表示時間 −(受理から起動画面の退場まで)」になり、起動が遅い構成ほど短くなって、0 以下なら一度も見えない。どちらか片方だけを直しても、見える Toast は増えない (exploration.md「見立て: 片方だけでは直らない」)。

Sample の自動再生での実測 (API 36 のエミュレータ、`kasane/changes/archive/2026-09-29-align-sample-autoplay-start/evidence/autoplay-measurement.md`):
- Android 系 3 ルートから Sample 側の待ち (受理を最初の描画の後まで遅らせる処理) を外すと、起動直後の Toast が出ない回が増えた。例: KMP Android の既定 Toast は 5 回中 5 回 → 1 回
- 待ちを戻しても、表示時間 1500 ms の既定 Toast は 5 回中 3 回しか出なかった
- Dialog・Loading は全回表示された。iOS 系では起きていない

そのため Android 系 3 ルートの Sample には、今も「最初の描画の後に再生する」待ちが残っている。これは iOS 系と揃っていない。

決定は探索と提案作成で確定済み。経緯は [exploration.md](exploration.md) と [design.md](design.md) の Decision 2。

- [core/ADR-0043](../../decisions/core/0043-toast-duration-from-visible-when-accepted-in-foreground-without-host.md) (proposed、core/ADR-0041 の amends): アプリが前面にいるのに提示先が無い間に受け付けた Toast は、画面が利用者に見えた時点から表示時間を数える
- [core/ADR-0044](../../decisions/core/0044-android-host-is-drawn-resumed-activity.md) (proposed): Android の提示先は resumed で、かつ描画された Activity とし、Dialog・Loading・Toast の 3 機能で揃える

## What Changes

- **契約 (core)**
  - 提示先 (Android): 「resumed な Activity」から「resumed で、かつ描画された Activity」に変える。Dialog・Loading・Toast で共通。iOS の提示先 (前面でアクティブなシーンの key window) は変えない
  - Toast の表示時間: アプリが前面にいるのに提示先が無い間 (以下「前面の待ち」) に受け付けた Toast は、提示先に載った (画面が見えた) 時点から数える。アプリが背面にいる間に受け付けた Toast は、今までどおり受理の時点から実時間で数える。前面の待ちのまま提示先に載らずに背面へ下がったときは、下がった時点から数え始める
  - 前面の待ち: 画面を開いている途中 (コールド起動・プロセスが残ったままの再起動・画面の切り替え・背面から戻る途中・作り直し) と、割り込み (システムの許可ダイアログ・コントロールセンターなど) で提示先を一時的に失っている間の両方を含む。探索では「画面を開いている途中」に限っていたが、iOS のライブラリが起動時に動き出せず、起動の途中と割り込みを見分けられないため、提案作成で範囲を広げた (オーナー判断 2026-09-29、design Decision 2)
- **Android Native**
  - 提示先の追跡に「描画された」の条件を足す。印は Activity ごとに持ち、stop と破棄でだけ下ろす (pause だけからの復帰では再描画が起きないことがあるため)
  - 前面の判定 (作成済みで stop していない Activity があるか) を足し、前面の待ちの間に受け付けた Toast の期限を、受理時ではなく取り付けの時点で決める
- **iOS Native**: 前面の判定 (前面のシーンがあるか) と、前面を離れた合図を足し、前面の待ちの間に受け付けた Toast の期限を取り付けの時点で決める。提示先の判定と、提示先の出現の合図は変えない
- **KMP・MAUI**: 提示先の判定と Toast の計時は各 OS の Native に任せているので、振る舞いは Native に従う (実装の変更は無い見込み)。ただし MAUI Android は、中身を作る画面の文脈を MAUI 側で現在の Activity から選ぶ経路を別に持つ (`PlatformDialogContent`)。Native が確保した提示先と同じ画面を指すことを、実測で確かめる
- **テスト**
  - Android: 「resumed だが未描画」の間は 3 機能とも載らず、描画で載ること。pause だけからの復帰・背面からの復帰・作り直しで提示先を取りこぼさないこと
  - 両 OS: 前面の待ちの間に受け付けた Toast は載った時点から数え、背面で受け付けた Toast は受理時点から数えること。前面の待ちのまま背面へ下がった Toast は、下がった時点から数えること
- **Sample**: Android 系 3 ルート (`samples/android`・`samples/kmp/androidApp`・`samples/maui` の Android) の「最初の描画の後に再生する」待ちを外し、iOS 系と同じく最初の画面の表示時の処理から再生する。デモ項目は変えない
- **実測**: 直す前と直した後を、Android エミュレータで撮る。場面は、コールド起動・プロセスが残ったままの再起動・画面の切り替え・背面からの復帰・作り直し。Android 系 3 ルートの Sample で、待ちを外しても起動直後の Toast が見えることを確かめる。合格ラインは、起動画面が退いた後に Toast が見えている時間が表示時間の 2/3 以上 (既定の 1500 ms なら 1000 ms 以上) を全回で満たすこと。見えている時間は画面の録画から測る (オーナー判断 2026-09-29)。iOS Simulator では、起動直後と割り込みの最中 (許可ダイアログ) に受け付けた Toast を確かめる
- **蒸留時に反映**:
  - concepts core/api/toast-semantics.md — 「duration の時間モデル」と、失敗モデルの「提示環境の不在」の行 (TS-HW-01・03) を前面の待ちの扱いを含む形へ。「中身を作る時点」の提示先の定義。「保証すること」の結末 (3)「提示先が現れないまま満了」に、前面の待ちの表示は提示先が現れるか背面へ下がるまで満了しないことを足す
  - concepts core/api/loading-semantics.md・result-notification-semantics.md — 提示先の定義 (Android)
  - concepts android/api/dialog-surface.md・toast-surface.md ほか — 「resumed な Activity」の記述
  - handbook cross/sample-parity.md — 自動再生の節から Android 系 3 ルートの例外 (最初の描画の後へ回す) を外し、提示先の定義を追随する
  - core/ADR-0043・0044 — 実装の結果に合わせて本文を見直してから昇格する。ADR-0043 の昇格と同時に、core/ADR-0041 の frontmatter に `amended-by: 0043` を足し、index の ADR-0041 の行に「一部改訂: 0043」を書く

影響する能力: toast-contract・android-native・ios-native・samples (dialog-contract・loading-contract は、提示先の定義を参照するだけで要件の本文は変わらない。Android での振る舞いの変化は android-native で扱う)

## Non-Goals

- **iOS の提示先の定義の変更** — iOS の提示先 (前面でアクティブなシーンの key window) は既に「利用者に見えている画面」であり、core/ADR-0044 の範囲外
- **Dialog・Loading の寿命の変更** — どちらも表示時間を持たず、見えないまま終わる問題は起きない (core/ADR-0043 の範囲外)
- **起動画面の退場の演出の分を埋める、一定時間の待ち** — core/ADR-0044 で却下済み (値の根拠を置きにくい)。実測で合格ライン (表示時間の 2/3) を割れば、止めて再相談する
- **API 29 以下に固有の挙動の実測と修正** — オーナー判断 (2026-09-26) で扱わない。minSdk (24) でビルドと動作が成り立つことだけは保つ
- **利用者向け Skill (`skills/`) の追随** — docs-refresh 経由の手順で行う (CLAUDE.md の運用)

## Impact

- **公開 API の変更は無い見込み**。振る舞いの変更だけで、いずれも 0.1.0-beta の範囲
  - Android の Dialog・Loading・Toast: 起動直後・画面の切り替え直後は、以前より遅れて (最初の描画の後に) 出る
  - 両 OS の Toast: 起動直後や割り込みの最中 (前面の待ち) に受け付けたものは、以前より遅れて出て、起動画面や割り込みが退いた後に表示時間の大半が見える
- **リスク**
  - 描画の合図を取りこぼすと、Dialog は上限なく待つので、出ないまま返らない。Loading・Toast は出ないまま終わる。描画が起きない復帰 (pause だけからの復帰) を、テストと実測で押さえる
  - 描画は起動画面の退場より前に来るので、退場の演出の分だけ、なお起動画面の下で載ることがある (数百 ms と推測、未実測)
  - iOS で、シーンがつながる前 (アプリの初期化処理など) に受け付けた Toast は、前面にいると判定できず受理時点から数える。シーンの状態の移り方 (起動時・復帰時・割り込み時) は、まだ実測していない
  - MAUI Android の最初の描画が、MAUI の画面の中身ができる前の空の画面になる構成があるかは、まだ確かめていない
  - MAUI Android が中身を作る画面の文脈 (MAUI 側の現在の Activity) と、Native の新しい提示先 (描画された Activity) が、画面の切り替えの途中で食い違う可能性は理屈上ある。まだ確かめていない
  - 表示時間の計時は、Android・iOS とも時計を差し替える口が無く、テストは実時間で測っている。数え始めの時点を確かめるテストは、実時間の待ちで組むことになる
  - Android の instrumented テストで「resumed だが未描画」の状態を作る手段は、design で決める

## 級: L

Dialog・Loading・Toast の 3 機能の提示先の条件と、Toast の時間モデルを全形態で変える。公開済みベータの振る舞いが変わり、失敗したときの被害が大きい (オーナー確定 2026-09-29)。

domain: cross
