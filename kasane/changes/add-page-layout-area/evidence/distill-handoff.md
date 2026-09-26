# 完了確認の実行記録と蒸留への申し送り (tasks 6.1 / 6.2)

実行日: 2026-09-26。tasks 6.1 時点の最終コードの作業ツリー (tasks 1〜5 と deviation.md の 5 項がすべて入った状態) で回した。コマンドと件数の得方は handbook cross/test-execution.md と cross/verification-ci.md に従った。iOS は作業用の Simulator (iPhone 17 Pro / iOS 26.5) を起動して使い、終わったら停止した。Android は共用の端末 (起動中のエミュレータ・実機 2 台) を使わず、専用の AVD (Small_Phone、API 35、ウィンドウなしで起動) を起動して `ANDROID_SERIAL` で 1 台に絞り、終わったら停止した。Gradle のルートは逐次で回した。

## 6.1 全ルートの全件実行

| ビルドルート | 実行 | 全件 | 失敗 | skip | 備考 |
|---|---|---|---|---|---|
| ios/ | `xcodebuild test -scheme KsDialogs -parallel-testing-enabled NO` (Simulator、スイート直列) | 316 | 0 | 0 | Swift Testing 316 件 / 54 suites。XCTest 側は `Executed 0 tests`。追加の 3 suites (「現在ページの台帳の規則」「基準領域「表示中のページ」 (UIKit)」「同 (SwiftUI の modifier)」) とケース表 3 suites が結果に現れる。公開 API の正の検査 (`DialogCurrentPageCompileChecks.swift`) はこのビルドに同梱 |
| android/ (unit + api-surface-check) | `./gradlew test --rerun-tasks` | 75 | 0 | 0 | `DialogCurrentPageSelectionTests` 7 件を含む。`verifyNoDeclarativeUiDependency` と `:api-surface-check:compileDebugKotlin` が走った |
| android/ instrumented `:ksdialogs-core` | `./gradlew connectedDebugAndroidTest` (専用 AVD、API 35) | 350 | 1 | 1 | `DialogCurrentPageTests` 12 件・`DialogCurrentPageLedgerTests` 4 件を含む。失敗 1 件は下記 A (変更前のコードでも落ちる)。skip は `DialogSystemBarsTests.PB_SB_04_旧経路でも非表示状態が維持される` (API 29 でだけ判定できる) |
| android/ instrumented `:ksdialogs` | `./gradlew :ksdialogs:connectedDebugAndroidTest` (同上) | 52 | 0 | 0 | `:ksdialogs-core` の失敗でタスクが止まったため単独で回した。`ComposeCurrentPageTests` 7 件を含む |
| kmp/ | `./gradlew allTests compileCommonMainKotlinMetadata compileIosMainKotlinMetadata --rerun-tasks` | 166 | 0 | 0 | iosSimulatorArm64 85 + androidHostTest 81。階層化 source set の metadata compile (CI の kmp job と同じ 2 タスク) と `:api-surface-check:compileKotlinIosSimulatorArm64` も成功 |
| maui/ | `dotnet test` | 192 | 0 | 0 | `KsDialogs.Maui.ApiSurfaceCheck` のビルド (`DialogCurrentPageApiSurfaceChecks.cs` を含む) を巻き込んで成功 |
| maui/android/native/ | `./gradlew :ksdialogs-maui-bridge:test --rerun-tasks` | 38 | 0 | 0 | `MauiDialogCurrentPageRegistrationTests`・`MauiDialogLayoutPassthroughTests` を含む |
| maui/macios/native/ | `xcodebuild test -scheme KsDialogsMauiBridge -parallel-testing-enabled NO` (Simulator) | 9 | 0 | 0 | Swift Testing 9 件 / 5 suites |

### MAUI の実配置テストホスト (`maui/KsDialogs.Maui.PlacementHost`)

起動すると全シナリオを順に実行し、1 シナリオ 1 行を `KSDPLACEMENT|…` で出す。iOS は `dotnet build -f net10.0-ios -p:RuntimeIdentifier=iossimulator-arm64` → `simctl install` → `simctl launch --console-pty`、Android は `dotnet build -f net10.0-android` → `adb install` → 起動 → logcat の DOTNET タグで読んだ。

| OS | 端末 | 結果 |
|---|---|---|
| iOS | Simulator (iPhone 17 Pro / iOS 26.5) | `SUMMARY|passed=7|failed=0` |
| Android | 専用 AVD (API 35) | `SUMMARY|passed=7|failed=0` |

シナリオは両 OS とも `tabbed-navigation` / `shell` / `modal` / `modal-navigation` / `flyout` / `plain-content-page` / `provider-override` の 7 本で、すべて PASS。抜粋 (Android):

```
KSDPLACEMENT|Android|tabbed-navigation|PASS|dialog=(598,1034)-(672,1072)|area=(0,160)-(720,1120)|visible=(0,48)-(720,1232)|barBelowPage=True
KSDPLACEMENT|Android|modal-navigation|PASS|dialog=(598,1146)-(672,1184)|area=(0,160)-(720,1232)|visible=(0,48)-(720,1232)|barAbovePage=True|startDialog=(48,208)-(122,246)|startPlaced=True|underneath=(0,48)-(720,1120)|distinct=True
KSDPLACEMENT|Android|plain-content-page|PASS|dialog=(598,1146)-(672,1184)|area=(0,48)-(720,1232)|visible=(0,48)-(720,1232)|visibleAreaDialog=(598,1146)-(672,1184)|sameAsVisibleArea=True
KSDPLACEMENT|Android|SUMMARY|passed=7|failed=0
```

### レビュー修正後の再実行 (review-002 / review-003)

上の表は tasks 6.1 の時点の記録。レビュー指摘の修正 (MAUI 層の取得元判定・instrumented の待ち・MAUI iOS の未解決時の扱い) の後、独立レビュアーが最終コードで再実行した結果は次のとおり (詳細は review-002.md / review-003.md のテスト実行節)。

| 対象 | 全件 | 失敗 | 備考 |
|---|---|---|---|
| maui/ `dotnet test` | 197 | 0 | 192 → 197 件。`RejectedRegisteredElementFallsBackToTheDefaultPage` 4 ケースを含む |
| maui/macios/native/ bridge | 9 | 0 | `presentationWindowAsPageMatchesVisibleArea` を `rootViewAsPageMatchesVisibleArea` に組み直し |
| MAUI 実配置テストホスト iOS | 8 シナリオ | 0 | `provider-empty-falls-back` を追加 (iOS は FlyoutPage + NavigationPage 構成) |
| MAUI 実配置テストホスト Android | 9 シナリオ | 0 | `provider-empty-falls-back` / `provider-container-falls-back` を追加 (review-002 時点) |
| android/ unit / instrumented `:ksdialogs-core` / `:ksdialogs` | 75 / 350 / 52 | 0 / 0 / 0 | review-002 時点。専用 AVD (API 35) で下記 A も含め失敗なし。以後 Android のソースは変わっていない |

### Sample 4 ルートのビルド

| Sample | 実行 | 結果 |
|---|---|---|
| samples/ios | `xcodebuild -project KsDialogsSample.xcodeproj -scheme KsDialogsSample … build` | BUILD SUCCEEDED |
| samples/android | `./gradlew :app:assembleDebug` | BUILD SUCCESSFUL |
| samples/maui | `dotnet build -f net10.0-ios -p:RuntimeIdentifier=iossimulator-arm64` / `dotnet build -f net10.0-android` | 両方成功 (警告 0・エラー 0) |
| samples/kmp | `./gradlew :androidApp:assembleDebug` / `iosApp` の `xcodebuild … build` | 両方成功 |

### lint (CI の lint job が回すもののうち、この change に関係するもの)

| 検査 | 結果 |
|---|---|
| `scenario-id-coverage.py` / `--require-mirror` / `--selftest` | 未網羅なし / 対象領域の ID はすべて iOS / Android の双方にある / 全件 OK |
| `comment-policy-lint.py` | 禁止 0 件 (検査対象 1131 ファイル) |
| `local-path-lint.py` / `identity-lint.py` | 違反なし / 違反なし (この申し送りファイルを書いた後にも回した) |
| `ci-skip-lint.py --selftest` と本検査 | 自己テスト OK / 印 0 件・違反なし |
| `readme-example-lint.py --selftest` と本検査 | 失敗なし / 最小例 4 件が一致 |
| `install-example-lint.py --selftest` と本検査 | 失敗なし / 16 ファイル 24 行が契約を満たす |
| `spm-snapshot/sync-snapshot-test.sh` | すべて成功 |

`doc-structure-lint.py` (CI の lint job には無い) は 35 ファイル 219 件の指摘で終了コード 1 だが、この change が触った文書 (`handbook/cross/sample-parity.md` を含む) は指摘に入っていない。

### 失敗と環境要因の切り分け

**既知の 5 件 (UiAutomation の接続) は専用 AVD では出なかった — 環境要因として確定**

前回、共用の端末で「UiAutomationService already registered」→「Not connected」で落ちた 5 件 (`DialogTransparentOverlayTests` 2 件・`ToastMultiDisplayTests` TS_MX_01 / TS_MX_05・`ToastSystemInputTests.Toast_表示中でも戻るとホームが通る`) は、専用 AVD の全件実行で `already registered` / `Not connected` の例外を 1 件も出さなかった (結果 XML に該当文字列 0 件)。このうち 4 件は成功し、残る `DialogTransparentOverlayTests` の 1 件は別の理由で落ちた (下記 A)。UiAutomation の接続が取れない現象は、他のツールが UiAutomation を握っている共用の端末に固有のもので、この change の実装とは無関係と判断した。起動直後の専用 AVD の `dumpsys accessibility` は `Bound services:{}` だった。

**A. `DialogTransparentOverlayTests.覆いが透明ならステータスバーの明るさは表示前後で変わらない` (API 35 の AVD) — 変更前のコードでも落ちる**

- 失敗: `AssertionError: 透明の覆いでステータスバーの明るさが変わっている (表示前 246.7… / 表示中 255.0)`
- 同じクラスだけを最終コードで 3 回回した: 3 回中 2 回失敗 (同じ形。表示中の値はいつも 255.0)
- 変更前のコード (コミット c4936a8 の `android/` と `core/` を作業ツリーの外へ取り出したもの) で同じクラスを同じ AVD で 5 回回した: 5 回中 4 回失敗 (同じ形)。取り出したコードは実行後に削除した
- 画面の見え: テスト実行中に画面を撮ると、透明の覆いのダイアログが出ている間はステータスバーのアイコン (時刻・電波・電池) が白地の上から消え、帯が一様に白 (輝度 255) になる。アイコンが白で描かれて白地に溶けていると見られる。ダイアログが閉じると元の濃いアイコンに戻る
- 判断: この change の実装によるものではない (変更前も同じ頻度で落ちる)。API 35 で KsDialogs の器のウィンドウが前面に来たときにステータスバーのアイコンの明暗が画面側の指定を引き継がない、という既存の挙動に見える。deviation.md の付随修正 2 件目 (Sample の API 35 でステータスバーのアイコンが白地に白で見えなかった) と同じ系統の可能性がある。CI (API 36) と前回の API 33 実機では再現していない。スコープ外の発見として下に記録した

**API 29 (旧経路) は未実行**: handbook が挙げる API 29 のエミュレータ (`ksn_api29`) はこの環境に AVD として存在しなかった (API 29 のシステムイメージはある)。API 29 でだけ判定できる `PB_SB_04` は上記のとおり skip。

### 6.1 をチェックした根拠

8 ルート + MAUI の実配置テストホスト両 OS + Sample 4 ルートのビルドが、tasks 6.1 時点の最終コードで成功した (レビュー修正後の再実行は上の「レビュー修正後の再実行」節)。唯一の失敗 (A) は変更前のコードでも同じ頻度で落ちるので、この change とは無関係と判断した。件数はどのルートも 0 ではなく、この change で足したテスト (上表の備考に挙げたクラス・suite) が結果に現れている。

## 6.2 蒸留への申し送り

### concepts の追随点

**`concepts/core/api/layout-semantics.md` の「基準領域」節**

- 選択肢の表を 3 行にする: ウィンドウ全体 / 可視領域 (既定) / 表示中のページ (`currentPage`)。「将来 3 つ目の基準が必要になっても…足せる」の一文は、実際に足した記述へ置き換える
- `currentPage` の意味: 表示中ページの矩形のうち、そのページ自身の safe area / システム insets の内側。水平・垂直の両軸に効く。器は矩形を窓座標の 4 辺 inset に変換し、既存の rect 決定手順 (基準 rect R → 有効領域 A) にそのまま流す (計算部は変わらない)
- 取得元の優先順位: modifier の台帳 > 登録 provider > 既定 provider。上位が空 (未登録・null・例外・提示先と別のウィンドウ・矩形が空) なら下位へ進む。すべて空なら `visibleArea` と同じ結果になり、診断ログ (英語。例 `The current page could not be resolved, so the visible area is used instead.`) を出す
- 候補は提示先と同じウィンドウに属するものに限る。Android の「同じウィンドウ」は **提示先 Activity が持つウィンドウ** (メインウィンドウと、同じ Activity で出したモーダル・ダイアログのウィンドウ) と解釈し、KsDialogs 自身の器 (Dialog / Loading / Toast) のウィンドウは除く (deviation.md 2 項目。MAUI Android のモーダルページが Activity とは別のダイアログウィンドウに載るため)
- modifier の台帳規則: 配置中のものだけ・提示先ウィンドウ外は除外・入れ子は内側・それ以外は最後に配置されたもの・外れたら残りで決め直す。**iOS だけ**「画面に表示されていない印 (印自身かウィンドウまでの祖先に `isHidden`、または `alpha` < 0.01) も除外」が加わる (deviation.md 3 項目。SwiftUI `TabView` の切替演出中に去るタブの View が残るため)。Compose は `when` の切替なら同じ状態は起きない
- 再配置のトリガーは従来どおり (窓寸法と insets の変化)。そのたびに provider に問い合わせ直す。表示中のページ遷移は再配置のトリガーにしない (proposal Non-Goals)。登録の差し替えは次の表示から効く
- Loading / Toast への適用は「値を渡せば Dialog と同じ規則で解決する」以上を固定していない (proposal Non-Goals)
- 「形態別の公開面」節の表にも `currentPage` の 3 面の名前を足す

**`concepts/ios/api/layout-surface.md`**

- `DialogLayoutArea` の値に `.currentPage` を足す
- 上書きの登録口: `DialogCurrentPage.provider: (@MainActor () throws -> UIView?)?` (`nil` 代入で既定へ戻る)。基準は返した View の `safeAreaLayoutGuide.layoutFrame`
- SwiftUI の modifier: `View.ksDialogCurrentPage()`。`TabView` / `NavigationStack` の画面はこちらが正規経路 (各画面の中身の枠に 1 回付ける)
- 既定 provider: 提示先 window の VC 階層を presented → `UINavigationController` の top → `UITabBarController` の selected と先端まで辿り、先端 VC の view の safe area の内側を使う。KsDialogs の器のコンテナ VC は通り抜けて提示元へ戻る。dismiss 中の presented は辿らない。保証範囲は UIKit のコンテナまで (root が `UIHostingController` の SwiftUI 画面には及ばない)
- 台帳の非表示除外 (deviation 3) はここに書く

**`concepts/android/api/layout-surface.md`**

- `DialogLayoutArea.CURRENT_PAGE` を足す。既定 provider は持たない (登録制)。登録しなければ `VISIBLE_AREA` と同じで、診断ログを出す
- 従来 View の登録口: `DialogCurrentPage.provider: (() -> View?)?` (`jp.kamusoft:ksdialogs-core`、UI スレッドで呼ばれる、`null` 代入で解除)
- Compose の modifier: `Modifier.ksDialogCurrentPage()` (`jp.kamusoft.ksdialogs.compose`、`jp.kamusoft:ksdialogs`)。`Scaffold` の content 枠 (topBar / bottomBar の外側) に付ける。台帳は登録 provider に優先する
- 矩形はページ View の矩形 ∩ 可視領域。器 (別ウィンドウ) と Activity ウィンドウの突き合わせは画面座標を共通原点にする (View は `getLocationOnScreen`、Compose は `boundsInWindow()` に Activity の decorView の画面上の位置を足す)
- 台帳の出入り口 `DialogCurrentPageLedger` と印の型 `DialogCurrentPageMarker` は `@KsDialogsInternalApi` (`@RequiresOptIn(level = ERROR)`) つきの public (deviation.md 1 項目。Compose の modifier が別モジュールにあり Kotlin の internal が届かないため)。利用者向けの登録口ではなく、オプトインしないとコンパイルエラーになることを明記する
- 同じウィンドウの解釈 (deviation 2) はここにも書く

**`concepts/maui/api/layout-surface.md`**

- `DialogLayoutArea.CurrentPage` を足す (bridge の `KSDMauiDialogLayoutArea` / `MauiDialogLayoutArea`、iOS binding まで写像済み)
- 既定 provider は MAUI 層にある (両 OS 同じ辿り方): 提示先の `Window` と `IMauiContext` を一組で解決し、その `Window` の ModalStack の先頭 (無ければ `Window.Page`) を起点に `Shell → CurrentPage` / `FlyoutPage → Detail` / `TabbedPage → CurrentPage` / `NavigationPage → CurrentPage` を容れ物でなくなるまで降り、先端ページの `Handler.PlatformView` を Native の登録口へ流す
- 上書き: `DialogCurrentPage.Provider: Func<VisualElement?>?` (`Page` でも要素でもよい。MAUI 層の既定より優先。`null` 代入で既定へ戻る)。null・例外・未描画・提示先ウィンドウ外なら既定へ進む
- OS 差: iOS は MAUI 層で決まらなければ Native 内蔵の VC 走査へは進まず、safe area の内側が可視領域を覆う View (提示先 window の root から present の連なりを辿った VC の view のうち最初に該当するもの。通常は root の view、全画面モーダル中はモーダルの入れ物の view) を返して `visibleArea` と同じ結果にする (MAUI 側で診断を出す)。window そのものは `UIView.window` が nil のため Native に外され VC 走査へ進むので返さない。Android は null を返し、Native 実装が診断つきで可視領域へ落とす
- 未描画 (`Handler` が無い) ページは未解決扱い

### core/ADR-0038 と ADR-0008

- core/ADR-0038 (proposed) を accepted へ昇格する。Android の `@KsDialogsInternalApi` と同じウィンドウの解釈、iOS の台帳の非表示除外 (deviation 1〜3) は、ADR の本文 (Decision) に足すか concepts だけに置くかを蒸留時に決める
- core/ADR-0008 の Decision 3 に注記を足す: 「visibleArea = システムバー除外領域」は原典の基準 (表示中ページの領域) からの再定義であり、ページ領域は ADR-0038 で 3 つ目の値 `currentPage` として復活した

### ColorAnalyzer への返事 (outbox) の要点

- 知らせ `2026-09-25-page-location-layout-area.md` への返事。希望 1 (基準領域に表示中のページを足す) は `DialogLayoutArea.CurrentPage` として入った。水平・垂直の両軸に効く (原典は垂直だけ)
- MAUI では MAUI 層の既定 provider が Shell / TabbedPage / NavigationPage / FlyoutPage / モーダルを辿るので、標準のページ構成なら登録なしで `LayoutArea = CurrentPage` を指定するだけでタブバー・ナビゲーションバーの内側が基準になる。独自の切り替えを使う画面だけ `DialogCurrentPage.Provider` を登録する
- 両 OS の実配置 (TabbedPage + NavigationPage・Shell・モーダル・FlyoutPage) をテストホストで確かめた (上記)
- 取り込める版はこの change を含むリリース以降 (まだ公開していない)
- 希望 2 (移行スキルの対応表) は docs-refresh で `true → CurrentPage` に直す。暫定の注記は入れない (オーナー決定 2026-09-25)

### docs-refresh で直す件

移行スキル `skills/{ja,en}/ksdialogs-aiforms-migration` の対応表で、`UseCurrentPageLocation=true` の写し先を `VisibleArea` から `CurrentPage` へ直す (MAUI の既定 provider で登録不要なこと、独自構成では `DialogCurrentPage.Provider` を登録することの説明つき)。あわせて各形態の利用者向け Skill (`ksdialogs-ios` / `-android` / `-maui` / `-kmp`) のレイアウトの記述に `currentPage` と登録口・modifier が要るかを docs-refresh の網羅検査で仕分ける。

### handbook 追記の候補 (確認事項)

- **MAUI 実配置テストホストの実行方法を test-execution.md に載せるか**: `maui/KsDialogs.Maui.PlacementHost` は `dotnet test` の対象外で、両 OS でアプリとして起動して `KSDPLACEMENT|…|SUMMARY|passed=N|failed=M` を読む (iOS は `simctl launch --console-pty`、Android は logcat の DOTNET タグ)。載せないと、次に MAUI の配置を触る人がこのルートを回さない
- **ホストなしの iOS テストの制約**: ホストアプリなしの iOS テスト (ios/ の標的) では、SwiftUI `TabView` の選択を変えても View の差し替えも切替の演出も起きない。タブ切替をまたぐ台帳の挙動 (deviation 3 の状態) はテストでは再現できず、Sample の通しで確かめた。test-execution.md の「黙って空振りする範囲」に足す候補
- **API 29 のエミュレータ**: handbook が前提にする `ksn_api29` がこの環境に無かった。手元の完了判定に API 29 を残すなら、AVD の作り方を local-development-setup.md に書くか、前提の記述を直す
- **sample-parity.md の「画面の構成」表** (review-004 Suggestion 1): Layout Dialog パネルが出すダイアログの dialogMargin は全辺 0 (deviation.md 最終項のオーナー指示) を表に足す。今の規約では 1 ルートだけ既定値 (24) に戻っても検出できない。既定値そのものの見直しは起票済み (`revisit-dialog-margin-default`)

### スコープ外の発見 (見送り・起票済みの記録)

- **Android Compose の遷移演出中に去る画面が基準に選ばれ得る**: `Crossfade` / `AnimatedContent` / Navigation Compose のフェード遷移では、演出中に去る画面の印が台帳に残り、入れ子や最後に配置の規則でそちらが選ばれ得る (iOS の deviation 3 と同種)。オーナー判断で見送り (2026-09-26)
- **MAUI Android のダイアログの中身のサイズ**: 起票済み (`fix-maui-android-dialog-content-size`)
- **MAUI Android Sample の移動量欄の縦位置 (API 35)**: 未起票の記録
- **MAUI Android Sample のステータスバーの色**: 未起票の記録
- **(今回の発見) API 35 で透明の覆いのダイアログを出すとステータスバーのアイコンが白地に溶ける**: 上記 A。変更前のコードでも同じ頻度で落ちるテストがあり、この change とは無関係。起票済み (`fix-android-api35-overlay-status-bar-icons`、2026-09-26)
