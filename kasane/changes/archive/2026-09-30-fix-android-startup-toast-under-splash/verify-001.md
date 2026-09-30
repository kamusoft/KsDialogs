# 一致検証: fix-android-startup-toast-under-splash (001 回目)

**日付**: 2026-09-30
**判定**: VALID

## サマリー

- デルタスペック 4 能力に Requirement は 8 件、Scenario は 34 件ある。すべてについて、実装とテスト (または証跡) との対応を確かめた。❌ は 0 件
- CA-SA-10 は ⚠️ とした。測り方・端末・範囲の変更が deviation.md に合意済みで、その変更を踏まえた証跡が合格ラインを満たしている
- 足場 (proposal / design / specs) は、基点 HEAD から書き換えられていない。逆流なし
- 作業ツリーにある変更は、どれも Scenario か deviation.md の項 (付随修正を含む) に対応する。記録の無い乖離は無い
- テストと lint は、この検証の中で実行した (下の「テスト実行」)。失敗は 0 件
- review-002 の Minor 2 への対応として足された 2 件のテストは、求められたとおりの退行で落ちる。作業用のコピーに退行を入れて実際に落ちることを確かめた (「review-002 Minor 2 の対応の確認」)
- 次の 2 点は、判定を変えないが記録の整合として残す (「観察事項」)
  - tasks 7.5 (端末の削除) が未完了
  - tasks.md 7 章に、deviation で足した実測の場面の項目が無い

## 検査の範囲

- 対象: 作業ツリーの未コミットの変更すべて (基点 HEAD cbfbf00 との `git diff HEAD` の 42 ファイルと、未追跡ファイル)
- 照合の基準: `specs/` の 4 能力、`tasks.md`、`deviation.md`、`evidence/before-fix-measurement.md`・`evidence/after-fix-measurement.md`
- deviation.md の各項は、合意済みの差分として扱った

## 対応表

パスはリポジトリルートからの相対パス。略記は次のとおり。

- `A/` = `android/ksdialogs-core/src/`
- `I/` = `ios/`

### toast-contract

| Requirement / Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| **MODIFIED: Toast の公開面と fire-and-forget** (時間モデル・失敗モデルの「提示環境の不在」) | 受理時刻を記録し、期限は開始処理で決める: `A/main/kotlin/jp/kamusoft/ksdialogs/ToastCoordinator.kt:97-110`・`:122-128`、`A/main/.../ToastDisplay.kt:64-80`。iOS は `I/Sources/KsDialogs/Presentation/ToastCoordinator.swift:99-113`・`:139-170`、`ToastDisplay.swift` | 下の TS-CO-01〜08 | ✅ |
| [TS-CO-01] show で表示され duration 経過で自動的に消える | 同上 (既存の経路) | Android `A/androidTest/.../ToastContractTests.kt:52`、iOS `I/Tests/KsDialogsTests/ToastContractTests.swift:27` | ✅ |
| [TS-CO-02] duration 省略時は ToastStyle の既定が使われる | 既存の経路 | Android `ToastContractTests.kt:64`、iOS `ToastContractTests.swift:47` | ✅ |
| [TS-CO-03] 0 以下の duration は style の既定に丸められる | 既存の経路 | Android `ToastContractTests.kt:88`、iOS `ToastContractTests.swift:63`・`:80` | ✅ |
| [TS-CO-04] 未登録の ViewModel 型は fail-fast | 既存の経路 | Android `ToastContractTests.kt:107`、iOS `ToastContractTests.swift:91` | ✅ |
| [TS-CO-05] インライン経路はレジストリの状態を変えない | 既存の経路 | Android `ToastContractTests.kt:122`、iOS `ToastContractTests.swift:107` | ✅ |
| [TS-CO-06] Native 内の異なる入口が同じレジストリと style を共有する | 既存の経路 | Android `ToastContractTests.kt:153`、iOS `ToastContractTests.swift:136` | ✅ |
| [TS-CO-07] 受理後の factory 失敗は破棄と資源解放 | 既存の経路 (開始処理の分岐の後も破棄の扱いは同じ) | Android `ToastContractTests.kt:174`、iOS `ToastContractTests.swift:164`・`:185`・`:207` | ✅ |
| [TS-CO-08] 計時は受理時点から進み、入りの途中でも duration 到達で出へ移る (前提を「提示先がある状態」に直した) | 提示先があれば受理時点で期限を決める: Android `ToastCoordinator.kt:124-128`、iOS `ToastCoordinator.swift:155-158` | Android `ToastContractTests.kt:194` (既定のハーネスは提示先あり)、iOS `ToastContractTests.swift:496` | ✅ |
| **MODIFIED: Toast の中身は取り付けの時点で作る** | 期限の決まった表示だけを期限切れと判定し、中身は取り付けの時点で作る: Android `ToastCoordinator.kt:222-225`、iOS `ToastCoordinator.swift` の `attachIfPossible` | 下の TS-HW-01〜03 | ✅ |
| [TS-HW-01] 背面で受理された Toast は、提示先が現れた時点で表示され、受理時点から数えた duration で消える | 同上 | Android `ToastContractTests.kt:238` (`isAppInForeground = false`)、iOS `ToastContractTests.swift:229` | ✅ |
| [TS-HW-02] 提示先が現れないまま満了した Toast は、中身が作られずに破棄される | 同上 | Android `ToastContractTests.kt:272`、iOS `ToastContractTests.swift:267` (どちらも背面で受理) | ✅ |
| [TS-HW-03] 期限を過ぎた保留表示は、提示先の出現が期限の処理より先でも表示されない | 同上 | Android `ToastContractTests.kt:304`、iOS `ToastContractTests.swift:298` (どちらも背面で受理) | ✅ |
| **ADDED: 前面の待ちの間に受け付けた Toast は、提示先に載った時点から計時する** | 各処理と実装の位置は次のとおり<br>- 開始処理で前面の待ちを判定する: Android `ToastCoordinator.kt:124-128`、iOS `ToastCoordinator.swift:155-158`<br>- 取り付けの成功で期限を決める: Android `ToastCoordinator.kt:216-217`、iOS `ToastCoordinator.swift:231-232`<br>- 前面を離れた通知で期限を決める: Android `ToastCoordinator.kt:320-323`、iOS `ToastCoordinator.swift` の `fixDeadlinesOnForegroundDeparture`<br>- 一度決めた期限は動かさない: `ToastDisplay.fixDeadline` | 下の TS-HW-04〜06。作り直しをまたぐ場合は PB-HA-11 (instrumented) | ✅ |
| [TS-HW-04] 前面の待ちの間に受理された Toast は、duration より長く待っても破棄されず、載った時点から数えた duration で消える | 同上 | Android `ToastContractTests.kt:337`、iOS `ToastContractTests.swift:353` | ✅ |
| [TS-HW-05] 前面の待ちのまま背面へ下がった Toast は、下がった時点から数え始める | 同上 | Android `ToastContractTests.kt:371`、iOS `ToastContractTests.swift:383` | ✅ |
| [TS-HW-06] 背面へ下がった後、期限の前に提示先が現れたら、下がった時点から数えた期限まで表示される | 同上 | Android `ToastContractTests.kt:399`、iOS `ToastContractTests.swift:413` | ✅ |

### android-native

| Requirement / Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| **MODIFIED: Dialog の提示面は、Loading・Toast と同じ入れ替わりの購読口を持つ** (提示先の定義を「resumed で、かつ描画された Activity」に) | 提示面は追跡役の供給を読む: `A/main/.../ActivityDialogPresentationSurface.kt:18-19`・`:48-51`。供給は描画済みのときだけ提示先を持つ: `A/main/.../ResumedActivityTracker.kt:65-66`・`:191-198` | 下の PB-HA-01・02 | ✅ |
| [PB-HA-01] Activity が resume して描画された時点で、待っていた Dialog が表示される | 同上。描画を受けたら通知する: `ResumedActivityTracker.kt:181-188` | `A/test/.../DialogActivityHostWaitTests.kt:80` | ✅ |
| [PB-HA-02] Activity が破棄されただけでは表示されず、次の Activity が resume して描画された時点で表示される | 同上 | `DialogActivityHostWaitTests.kt:110` | ✅ |
| **ADDED: Android の提示先は、resumed で、かつ描画された Activity** | 各処理と実装の位置は次のとおり<br>- 描画の観測: `A/main/.../ActivityDrawObserver.kt` (既定は decorView の `OnDrawListener`)<br>- 描画済みの印: `ResumedActivityTracker.kt:160-188`<br>- 世代で古い描画を捨てる: `:163`・`:183`<br>- stop・破棄で印を下ろし、pause では下ろさない: `:123-139`<br>- resume で描画を促す: `:108-121`<br>- 描画済みになったら通知する: `:186-187` | 下の PB-HA-04〜06・10 | ✅ |
| [PB-HA-04] resumed だが未描画の Activity には、Dialog・Loading・Toast のどれも載らず、描画の後に載る | 同上 | instrumented `A/androidTest/.../DrawnHostPresentationTests.kt:40` (試験用 Activity `support/FirstDrawBlockingTestActivity.kt`) | ✅ |
| [PB-HA-05] pause だけから復帰した Activity は、再描画を待たずに提示先に戻る | `ResumedActivityTracker.kt:123-127` (pause では印を残す) | `A/test/.../ResumedActivityTrackerTests.kt:195` | ✅ |
| [PB-HA-06] stop した Activity は、start の後に描画されるまで提示先にならない | `ResumedActivityTracker.kt:129-133`・`:103-106` | `ResumedActivityTrackerTests.kt:209` | ✅ |
| [PB-HA-10] 描画が印になる前に stop した Activity は、その描画では提示先にならない | 世代の照合: `ResumedActivityTracker.kt:181-184` | `ResumedActivityTrackerTests.kt:281` | ✅ |
| **ADDED: Android の前面の判定** | 各処理と実装の位置は次のとおり<br>- 作成・start で集合に入れる: `ResumedActivityTracker.kt:98-106`・`:200-203`<br>- stop・破棄で外し、背面の確定を次の周回へ回す: `:212-220`<br>- 読み口: `AppForegroundProvider` (`A/main/.../ResumedActivityProvider.kt`) | 下の PB-HA-07〜09・11 | ✅ |
| [PB-HA-07] 作成の通知だけが届いた Activity があれば、前面の待ちと判定される | 同上 | `ResumedActivityTrackerTests.kt:227` | ✅ |
| [PB-HA-08] 描画済みの Activity が pause しただけなら、前面のまま提示先を失う | 同上 | `ResumedActivityTrackerTests.kt:238` | ✅ |
| [PB-HA-09] 前面の Activity がすべて stop すると背面になり、通知が届く | 同上 | `ResumedActivityTrackerTests.kt:250` | ✅ |
| [PB-HA-11] 作り直しの間は背面と判定されず、前面の待ちの Toast は数え始めない | 同上。加えて Toast の期限は、前面を離れた通知でだけ決まる (`ToastCoordinator.kt:320-323`) | 追跡役の判定は unit `ResumedActivityTrackerTests.kt:301`。Toast が新しい画面の描画の後に載り、そこから数えることは instrumented `DrawnHostPresentationTests.kt:113` | ✅ |

### ios-native

| Requirement / Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| **ADDED: iOS の前面の判定と、前面を離れた合図は、提示先の規則と同じ供給元に置く** | 各処理と実装の位置は次のとおり<br>- シーンの写しに前面かどうかを持たせる: `I/Sources/KsDialogs/Presentation/DialogWindowSceneSnapshot.swift`<br>- 前面の判定と前面を離れた合図を、同じ供給元に置く: `ApplicationKeyWindowProvider.swift` (`isAppInForeground`・`observeForegroundDeparture`。通知は `UIScene.didEnterBackgroundNotification` で、受けた時点で前面を読み直す)<br>- 口の宣言: `DialogKeyWindowProvider.swift`・`ToastPresentationSurface.swift`<br>- 購読は前面の待ちがある間だけ張る: `ToastCoordinator.swift` の `startWaitingForForegroundDeparture`・`stopWaitingIfSatisfied` | 下の PB-HI-05〜09 | ✅ |
| [PB-HI-05] 前面でアクティブでないシーンだけがあるとき、前面の待ちと判定される | 同上 | `I/Tests/KsDialogsTests/DialogHostAppearanceTests.swift:186` | ✅ |
| [PB-HI-06] 背面のシーンだけがあるとき、またはシーンが無いときは、背面と判定される | 同上 | `DialogHostAppearanceTests.swift:199` | ✅ |
| [PB-HI-07] 唯一の前面のシーンが背面へ入った通知で、前面を離れた合図が届く | 同上 | `DialogHostAppearanceTests.swift:217` (出現の合図の通知では届かないこと・UI スレッドで届くこと・解除後に届かないことも確かめている) | ✅ |
| [PB-HI-09] 別のシーンが前面に残っていれば、1 つのシーンが背面へ入っても合図は届かない | 同上 | `DialogHostAppearanceTests.swift:253` | ✅ |
| [PB-HI-08] 前面の待ちの Toast が無くなれば、前面を離れた合図の購読を解除する | 同上 | `DialogHostAppearanceTests.swift:275` | ✅ |

### samples

| Requirement / Scenario | 実装 | テスト / 証跡 | 状態 |
|---|---|---|---|
| **ADDED: Android 系 Sample の自動再生も、最初の画面の表示時に始める** | 3 ルートとも後ろへ回す処理を外した (下の CA-SA-11)。次の 3 つは変えていない<br>- 1 回限りの取り出し<br>- 作り直しでの再発火の守り (`savedInstanceState == null`・`autoPlayConsumed`・`ConsumeDemo`)<br>- 入口 (`play` / `PlayAsync`) | CA-SA-10 (証跡)・CA-SA-11 (コードレビュー)。`scripts/scenario-id-coverage.py:85-87`・`:130-131` に、除外の理由つきで登録してある | ✅ |
| [CA-SA-10] 起動直後の自動再生で、指定デモの起動直後の状態が表示される (手で通して証跡を残す Scenario) | 同上 | `evidence/after-fix-measurement.md` の「実機」節と「7.4 まとめ」。直す前は `evidence/before-fix-measurement.md`。内訳は下の「CA-SA-10 の照合」 | ⚠️ deviation 記録済み |
| [CA-SA-11] 自動再生の処理が、最初の描画の後や次の周回へ回さない (コードレビューで受け入れる Scenario) | 3 ルートとも後ろへ回す処理が無い<br>- `samples/android/app/src/main/kotlin/jp/kamusoft/ksdialogs/samples/android/MainActivity.kt:92-98` (`menuView.post` を外して `play(demo)` を直接呼ぶ)<br>- `samples/kmp/androidApp/src/main/kotlin/jp/kamusoft/ksdialogs/samples/kmp/android/MainActivity.kt:96-101` (同じ)<br>- `samples/maui/KsDialogs.Sample.Maui/SampleMenuPage.xaml.cs:86-96` (`#if ANDROID` の `Dispatcher.DispatchAsync` を外し、`OnAppearing` (`:77-81`) からそのまま `await PlayAsync(demo)`)<br>3 ファイルとも、`post`・`postDelayed`・`DispatchAsync`・描画を待つ listener は残っていない (grep で確認)。提示先の条件をなぞる処理も無い | ソースを読んで照合 | ✅ |

## CA-SA-10 の照合

Scenario の THEN を要素に分け、deviation.md の合意を当てて照合した。

| THEN の要素 | 合意済みの変更 (deviation.md) | 証跡 | 結果 |
|---|---|---|---|
| 3 ルート × 4 デモで、指定デモの起動直後の状態が出る | 端末をエミュレータから実機 (API 36) に変えた (2026-09-30「直した後の実測の端末」) | `basic-dialog`・`default-loading` を 3 ルートで各 3 回、計 18 回とも写っている。画像は `after-pixel-maui-basic-dialog.png` など | 満たす |
| Toast 2 デモを「コールド起動とプロセスが残ったままの再起動のどちらでも」5 回 | プロセスが残ったままの再起動は android ルートだけにする (kmp・MAUI は Sample を変えずに場面を作れない。2026-09-29 の 1 項目め) | 次の 50 回<br>- android: コールドとプロセスが残ったままの再起動で、各 5 回 × 2 デモ<br>- kmp・MAUI: コールドで、各 5 回 × 2 デモ | 満たす |
| 5 回とも、起動画面が退いた後の画面に写る | 起動画面が退いたコマの定義を、フェードが終わり切ったコマとした (2026-09-29)。3 コマ以下の不一致を無視する補いも足した (2026-09-30) | 50 回・60 枚とも、退いたコマで既に見えている (実機の「取り付け → フェードの終わり → 見え始め」の表と、第 2 段) | 満たす |
| 見えている時間が表示時間の 2/3 以上 | 同上。判定は実機の結果で行い、エミュレータの 2 回分は参考として残す (2026-09-30) | 比は 0.87〜0.96 で、全回が 2/3 以上 (「7.4 まとめ」) | 満たす |
| プロセスは落ちない | — | 落ちた回は無い (各節) | 満たす |

補足:

- 第 1 段 (android ルートの Toast 20 回) は、一時的な診断出力を入れたライブラリで撮っている
  - この扱いは deviation.md の 2026-09-30 の 2 項 (撮り直しの (c) と、実機でも同じ設定で撮るとした項) に記録済み
  - 戻したことは、SHA-256 の一致で確かめられている
  - 第 2 段 (kmp・MAUI の Toast、3 ルートの Dialog・Loading) は、戻した後のライブラリで撮っている
- android ルートの default-toast のコールド起動の 1 回目は無効として、6 回目で代えている (`after-fix-measurement.md:159-161`)。理由は次のとおり
  - 撮影中に端末で最近のアプリの画面が開き、Sample が背面へ下がった
  - そのため Scenario の WHEN (起動直後から連続で撮影する) の条件を満たしていない回にあたる。Scenario との乖離ではない
  - ただし除外の判断は deviation.md に記録されていない。追跡性のため、蒸留までに deviation.md へ 1 行足すことを勧める (判定には影響しない)

## review-002 Minor 2 の対応の確認

**結論: 満たしている。**

### 足されたテスト

`android/ksdialogs-core/src/test/kotlin/jp/kamusoft/ksdialogs/AttachedHostRetentionTests.kt` の 2 件。

- `:148`「破棄されていない画面に載った Loading は、描画済みの別の画面が現れた時点でその画面へ移る」
- `:175`「破棄されていない画面に載った Toast は、描画済みの別の画面が現れた時点でその画面へ移る」

### 実物でつないでいるか

Minor 2 の求めは「器を残す判定を実物でつなぐ」こと。次のとおり、判定の経路はすべて実物である。

- 追跡役は実物の `ResumedActivityTracker` を使う。描画と次の周回だけを手で送る (`support/TrackerTestDriver.kt`)
- 提示面は既定の `ActivityLoadingPresentationSurface` / `ActivityToastPresentationSurface` を使う (`:201-221`)。追跡役を `retainsAttachment` の供給元として渡している
- coordinator も実物の `LoadingCoordinator` / `ToastCoordinator` を使う
- そのため、`retainsAttachment` は既定の false ではなく、追跡役の判定 (`ResumedActivityTracker.kt:71-72`) を通る。素の JVM の `Activity.isDestroyed` は、`isReturnDefaultValues = true` (`android/ksdialogs-core/build.gradle.kts:60`) により false になる

### 手順が推奨修正と合っているか

review-002 の推奨修正の 1〜5 と、テストの手順を照らし合わせた。

1. A を開いて描画し、器を載せる: `driver.launchAndDraw()` の後に show している
2. B を開く (A は pause だけ、B は未描画): `onActivityPaused(previous)` の後に `driver.launch()`
3. 器が A に付いたままであること: `assertSame(attached, …)` で確かめている (`:161`・`:188`)
   - この時点の通知は、B の resume によるもの。新しい提示先は null で、A は破棄されていないので、器を残す分岐 (`AttachedHostRetention.kt:35`) を通る
4. B を描画する: `drawObserver.draw(next)`
5. 器が入れ替わったこと: `assertNotNull` と `assertNotSame(attached, moved)` で確かめている (`:169-170`・`:196-197`)
   - 推奨修正では「可能なら」とされていた「載っている先が B であること」は、確かめていない
   - ただし、器が入れ替わるのは `newHost != null` の分岐で外して新しい提示先に載せ直すときに限る。そのため、Minor 2 の求める退行の検出には足りる

4 の後、同じブロックで `onActivityStopped(previous)` も送っている。追跡役は stop では入れ替わりの通知を送らない (`ResumedActivityTracker.kt:129-133`)。前面の Activity (B) が残っているので、背面の確定も起きない。そのため、この追加の通知が判定の結果を変えることはない。

### 退行で落ちるか (実際に確かめた)

作業ツリーは変えずに、作業用のコピー (scratchpad) に android/ と core/ を写した。そのうえで、`AttachedHostRetention.kt:34` を review-002 が挙げた退行の形に変えて、`AttachedHostRetentionTests` を実行した。

- 変更: `newHost != null -> true` を `newHost != null -> !retainsAttachment(attachedHost)` にした
- 結果: 6 件中 2 件が失敗した。落ちたのは、足された 2 件だけである
  - どちらも `前の画面の器のまま残っている ==> expected: not same`
  - Loading は `LoadingContainer`、Toast は `ToastContainer` で落ちた
- 既存の 4 件 (背面へ下がって戻る・破棄で外れる) は成功した。つまり、この退行を検出できるのは、足された 2 件だけである

作業ツリー (変更なし) では、同じ 6 件がすべて成功する (下の「テスト実行」)。

## 追加検査

| 検査 | 結果 |
|---|---|
| tasks.md の虚偽チェック | なし。チェック済みの 1.1〜7.4・8.1 は、どれも対応表の実装・テスト・証跡と対応する。7.5 は未チェックで、実際に未完了 (観察事項 1) |
| 逆流 (足場の書き換え) | なし。`git diff HEAD` で `proposal.md`・`design.md`・`specs/` に差分が無い。この change の差分は `tasks.md` のチェックだけ。基点 HEAD は足場を作ったコミットそのもの。`kasane/decisions/` (core/ADR-0043・0044) にも差分が無い |
| 未記録の乖離 | なし。対応表に ❌ は無い |
| 付随修正 | deviation.md の `[付随修正]` 3 項は、どれも diff と一致する<br>- `support/FirstDrawBlockingTestActivity.kt` の破棄時の後始末と `recreateAndWait()` を足した<br>- iOS `KsToast.swift` の公開 doc を直した<br>- Android・KMP・MAUI の Toast の公開 doc を直した<br>ほかに、Scenario に直接対応しない次の差分も、deviation.md の該当項に記録がある<br>- `KsDialog.kt`・`ActivityDialogPresentationSurface.kt`・`DialogHostWaitQueue.kt`・`LoadingPresentationSurface.kt` の説明の追随 (review-001 Minor への対応の項)<br>- `AttachedHostRetention.kt` と、Loading・Toast の `retainsAttachment` (design Decision 1 の B の 2 項)<br>- 描画の観測の張り直し (描画の取りこぼしの保険の項) |
| UI 変更 | 該当なし (`ui/` を持たない change) |
| テスト | すべて成功 (下の表) |

## テスト実行 (この検証で実行)

| ビルドルート | コマンド | 結果 |
|---|---|---|
| ios/ | `xcodebuild test -scheme KsDialogs` (この change 専用の Simulator を UDID 指定) | Swift Testing: 372 tests / 61 suites 成功。XCTest: `Executed 0 tests`。PB-HI-05〜09・TS-HW-01〜06・TS-CO-08 が結果に出ていて、成功 |
| android/ (JVM) | `./gradlew :ksdialogs-core:testDebugUnitTest --rerun-tasks` | 109 tests / 0 failures / 0 skipped。`AttachedHostRetentionTests` の 6 件を含む |
| android/ (instrumented、API 36) | `ANDROID_SERIAL=<この change 専用の AVD> ./gradlew :ksdialogs-core:connectedDebugAndroidTest` | 383 tests / 0 failures / 1 skipped<br>- skip は PB-SB-04 で、API レベルによる既存の skip<br>- PB-HA-04・PB-HA-11・TS-HW-01〜06・TS-CO-08 が結果に出ていて、成功<br>- 実行中の Mac の load average は 9〜18 |
| lint | `scenario-id-coverage.py` / `--require-mirror`・`ci-skip-lint.py`・`local-path-lint.py`・`identity-lint.py`・`doc-structure-lint.py`・`comment-policy-lint.py` | どれも終了コード 0<br>- scenario-id-coverage: 未網羅なし<br>- `--require-mirror`: 対象領域の ID はすべて両 Native にある<br>- ci-skip-lint: 印 0 件<br>- comment-policy: 禁止 0 件<br>- doc-structure は既存文書の大きさの助言だけ (この change の範囲外) |

次のルートは再実行していない。

- `android/ :ksdialogs` (宣言的 UI) の instrumented・kmp・maui・MAUI 互換面の Android / iOS
- 理由: この change の差分のうちこれらに関わるのは、KMP・MAUI の公開 doc コメント 2 行ずつだけ。Native 側の新しい口は既定の実装を持つ (`retainsAttachment`) か、main と androidTest の中でしか実装されていない (review-002 で確認済み)
- tasks 8.1 の報告 (`:ksdialogs` 52 / kmp 86 + 83 / maui 206 / maui android native 41 / maui macios native 17、いずれも失敗 0) を採った

## 観察事項 (判定には影響しない)

1. **tasks 7.5 (1.1 で作った端末の削除) が未完了**
   - この検証でも専用の AVD と Simulator を使った。使った後は、シリアル / UDID を指定して停止した (削除はしていない)
   - 削除は、この検証の後に呼び出し元が行う
2. **tasks.md 7 章に、deviation で足した実測の場面「Loading の表示中に背面へ下がって戻る」の項目が無い** (review-002 Minor 1 が残っている)
   - 実測自体は行われている。記録は `evidence/after-fix-measurement.md` の「Loading の表示中に背面へ下がって戻る (android、3 回)」節と `after-pixel-return-loading.png`。3 回とも覆いの無いコマは無かった
   - tasks.md は凍結される足場ではない。後から追えるように、7 章に項目を足してチェックを付けることを勧める
3. **CA-SA-10 の実測で、無効として代えた 1 回の記録**
   - 上の「CA-SA-10 の照合」の補足のとおり。deviation.md への 1 行の追記を勧める
