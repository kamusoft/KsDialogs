# Tasks: fix-android-startup-toast-under-splash

各タスクの Requirement は specs/ の要件名。テスト名には Scenario ID を含める (core/ADR-0016)。端末は専用に作り、作業後に削除する (既存・起動中の Simulator / AVD は使わない)。

## 1. 直す前の実測 (design Decision 7)

- [x] 1.1 専用の Android エミュレータ (API 36) と iOS Simulator を作る
- [x] 1.2 Android 系 3 ルートの Sample から design Decision 6 の待ちを外した版を、今のライブラリでビルドして撮る (→ Requirement: Android の提示先は、resumed で、かつ描画された Activity / 前面の待ちの間に受け付けた Toast は、提示先に載った時点から計時する)
  - `default-toast`・`custom-toast` を、コールド起動とプロセスが残ったままの再起動で各 5 回
  - `basic-dialog`・`default-loading` を 3 回
  - Toast の見えている時間を、画面の録画から測る (起動画面が退いたコマから Toast が消えたコマまで。測り方は design Decision 7)
- [x] 1.3 iOS Simulator で、割り込みの最中 (許可ダイアログ) に受け付けた Toast の今の振る舞いを撮る。手順は `kasane/handbook/cross/runtime-behavior-verification.md` の小節「iOS Simulator でシーンの状態を観測するとき」。Sample に手を入れずに場面を作れなければ、作業を止めて報告する (→ Requirement: 前面の待ちの間に受け付けた Toast は、提示先に載った時点から計時する)
- [x] 1.4 手順・環境・結果を `evidence/` に記録する (生ログは sanitize した抜粋だけ)

## 2. Android: 提示先と前面の判定 (design Decision 1・2・5)

- [x] 2.1 描画の観測を差し替えられる口と、既定の実装を足す。既定は decorView の `OnDrawListener` で最初の描画を受け、次のメッセージ周回で知らせる (→ Requirement: Android の提示先は、resumed で、かつ描画された Activity)
- [x] 2.2 `ResumedActivityTracker` に描画済みの印を足し、提示先の供給を「resumed で、かつ描画済み」にする (→ Requirement: Android の提示先は、resumed で、かつ描画された Activity / Dialog の提示面は、Loading・Toast と同じ入れ替わりの購読口を持つ)
  - 印は Activity ごとに持つ。start で観測を張り、stop・破棄で下ろす。観測に世代を持たせ、stop・破棄の後に遅れて走った処理が印を立てないようにする
  - resume の時点で未描画なら、描画を促す
  - 描画済みになった時点で、入れ替わりの購読者へ通知する
- [x] 2.3 `ResumedActivityTracker` に前面の判定を足す。作成・start で集合に入れ、stop・破棄で外す。背面の確定は次のメッセージ周回で集合が空のままのときに行い、通知する (作り直しの間を背面とみなさない) (→ Requirement: Android の前面の判定)
- [x] 2.4 unit テスト: PB-HA-05・06・07・08・09・10・11 を足す。PB-HA-01・02 と `ResumedActivityTrackerTests` の既存テストを、描画の条件に合わせて直す (→ Requirement: Android の提示先は、resumed で、かつ描画された Activity / Android の前面の判定 / Dialog の提示面は、Loading・Toast と同じ入れ替わりの購読口を持つ)
- [x] 2.5 instrumented テスト: androidTest に、最初の描画を止める試験用 Activity (描画の直前で止め、テストが解除する) を置き、PB-HA-04 を足す (Dialog・Loading・Toast の 3 つ) (→ Requirement: Android の提示先は、resumed で、かつ描画された Activity)
- [x] 2.6 実装箇所のコメントで理由を現在形で説明し、決定の参照は `core/ADR-0044`・`core/ADR-0043` の形で書く (`kasane/handbook/cross/comment-policy.md`)

## 3. Android: Toast の期限 (design Decision 3・5)

- [x] 3.1 Toast の提示面の口に「前面かどうか」を足し、前面を離れた通知を今の入れ替わりの購読で受ける。偽の提示面 (`support/ToastTestPresentationSurface.kt`) にも足す (→ Requirement: 前面の待ちの間に受け付けた Toast は、提示先に載った時点から計時する)
- [x] 3.2 `ToastDisplay` の期限を「未確定」を持てる形にする。`ToastCoordinator` の表示の開始処理で前面の待ちを判定し、取り付けの成功か前面を離れた通知の時点で期限を決める。期限切れの判定は、期限が決まった表示にだけ行う (→ Requirement: 前面の待ちの間に受け付けた Toast は、提示先に載った時点から計時する / Toast の公開面と fire-and-forget)
- [x] 3.3 instrumented テスト: TS-HW-04・05・06 を足す。TS-HW-01・02・03 を「背面で受理」の前提に直す。TS-CO-08 を「提示先がある状態」の前提に直す (→ Requirement: 前面の待ちの間に受け付けた Toast は、提示先に載った時点から計時する / Toast の中身は取り付けの時点で作る / Toast の公開面と fire-and-forget)
- [x] 3.4 既存の Toast のテスト (作り直しの TS-AN-03、多重表示ほか) がそのまま通ることを確かめる

## 4. iOS: 前面の判定と Toast の期限 (design Decision 2・3・5)

- [x] 4.1 シーンの写し (`DialogWindowSceneSnapshot`) に前面かどうかを足し、`ApplicationKeyWindowProvider` に前面の判定と前面を離れた合図 (`UIScene.didEnterBackgroundNotification`) を足す。合図は通知の時点で前面を読み直し、前面のシーンが無くなったときだけ届ける。購読は前面の待ちの Toast がある間だけ (→ Requirement: iOS の前面の判定と、前面を離れた合図は、提示先の規則と同じ供給元に置く)
- [x] 4.2 `ToastDisplay` の期限を「未確定」を持てる形にする。`ToastCoordinator` の表示の開始処理で前面の待ちを判定し、取り付けの成功か前面を離れた合図の時点で期限を決める (→ Requirement: 前面の待ちの間に受け付けた Toast は、提示先に載った時点から計時する / Toast の公開面と fire-and-forget)
- [x] 4.3 テスト: 偽の供給元と偽の Toast の提示面に、前面かどうかと前面を離れた合図を手で送る口を足す。PB-HI-05・06・07・08・09、TS-HW-04・05・06 を足す。TS-HW-01・02・03 と TS-CO-08 の前提を直す (→ Requirement: iOS の前面の判定と、前面を離れた合図は、提示先の規則と同じ供給元に置く / 前面の待ちの間に受け付けた Toast は、提示先に載った時点から計時する / Toast の中身は取り付けの時点で作る / Toast の公開面と fire-and-forget)
- [x] 4.4 実装箇所のコメントで理由を現在形で説明し、決定の参照は `core/ADR-0043` の形で書く

## 5. KMP・MAUI

- [x] 5.1 KMP と MAUI の既存テストを走らせ、通ることを確かめる。Native の口の変更で KMP・MAUI の偽の面やブリッジの追随が要れば直す (振る舞いの変更は無い見込み)

## 6. Sample (design Decision 6)

- [x] 6.1 Android 系 3 ルートの待ちと、待ちの理由のコメントを外す。作り直しで再発火しない守りは変えない (→ Requirement: Android 系 Sample の自動再生も、最初の画面の表示時に始める — CA-SA-11)
- [x] 6.2 `scripts/scenario-id-coverage.py` の除外 ID に CA-SA-10・11 を、理由つきで登録する

## 7. 直した後の実測 (design Decision 7)

- [x] 7.1 1.2 と同じ端末・同じ手順で撮る。直す前と並べて記録する (→ Requirement: Android 系 Sample の自動再生も、最初の画面の表示時に始める — CA-SA-10 / Android の提示先は、resumed で、かつ描画された Activity)
- [x] 7.2 iOS 3 ルート (`ios`・KMP iOS・MAUI iOS) の起動直後の Toast と、割り込みの最中に受け付けた Toast を撮る (→ Requirement: 前面の待ちの間に受け付けた Toast は、提示先に載った時点から計時する)
- [x] 7.3 MAUI Android で、起動直後のカスタム Toast とカスタム Dialog が正しい画面の文脈で表示されることを確かめる。食い違いが見つかったら、作業を止めて報告する
- [x] 7.4 Toast の見えている時間 (起動画面が退いた後) と表示時間の比を `evidence/` にまとめる。合格ライン (表示時間の 2/3 以上を全回) を割る回が 1 回でも出たら、作業を止めてオーナーに諮る (core/ADR-0044 の Revisit When)
- [x] 7.5 1.1 で作った端末を削除する

## 8. 仕上げ

- [x] 8.1 全形態の全件テストと lint を、`kasane/handbook/cross/test-execution.md` の手順で通す (scenario-id-coverage を含む)
