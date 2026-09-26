# レビュー結果: add-page-layout-area (002 回目)

**日付**: 2026-09-26
**判定**: CHANGES_REQUESTED

## サマリー

修正対象の 2 件はどちらも解消している。1 件目 (MAUI 層の取得元判定が Native の棄却条件より緩い) では、両 OS の `Judge` が Native と同じ条件・同じ順で判定するようになった。単体テストと実配置テストホストの新しいシナリオ (空の矩形・器の中の要素) で固定されている。2 件目 (落ち着き待ち) では、共通の待ちを両モジュールで共有できる置き場へ移し、新しい待ちは全部そこを通る。全ルートを回し直し、失敗は 0 件だった。

一方で、変更範囲を見直すと、前回まで見落とされていた問題が 1 件あった。MAUI iOS 層は、ページが決まらないときの逃げ道として提示先の `UIWindow` そのものを Native へ返す。ところが `UIWindow.window` は nil なので、Native はこれを「提示先の window に載っていない」として外す。その結果、spec が MAUI では使わないとしている Native 内蔵の view controller 走査へ進んでしまう (実機のプロセスで確認した)。1 件目と同じ型の不整合で、しかも spec の SHALL に反する。これを検出するはずのテストも、この差を見分けられない。そのため Major とし、判定は CHANGES_REQUESTED にする。

## 照合した規約

- comment-policy.md (always): `scripts/comment-policy-lint.py` で禁止 0 件 (検査対象 1132 ファイル)
- test-execution.md (テストの実行・完了判定): 下の「テスト実行」の各ルートを実行した。instrumented は 2 つのモジュールの結果 XML を合算
- ci-flaky-test-policy.md (instrumented で状態遷移を観測するテスト): 「観測は終端状態の合意を待つ」「共通プリミティブの面ごとの置き場」「時間切れの説明文に単調時計付きの観測履歴」の 3 節を照合した (解消確認表 2)
- diagnostic-message-language.md (警告ログを足す・変える): MAUI 層の新しい理由文 (`has an empty area.` / `is in a window of a KsDialogs container.` 等) は英語で、Native の同じ状況の文と揃っている
- 関連する決定: core/ADR-0038 (proposed。Decision 3 の「MAUI = MAUI 層でページ木を辿る」は指摘 1 の論点)
- lessons/code-review.md L-001: 推奨修正には、検査を足すたびに「機構が無いと何が変わるか」を添えた

## テスト実行

作業用 Simulator「KsDialogs-add-page-layout-area」と、未起動だった AVD `Small_Phone` (API 35、`emulator-5680` としてウィンドウなしで起動) を使った。終わったら両方とも停止した。起動中の端末は使っていない。Gradle は逐次で回した。

| ルート | 実行 | 全件 | 失敗 | skip |
|---|---|---|---|---|
| maui/ | `dotnet test` | 197 | 0 | 0 |
| android/ unit | `./gradlew test --rerun-tasks` | 75 | 0 | 0 |
| android/ instrumented `:ksdialogs-core` | `connectedDebugAndroidTest` | 350 | 0 | 1 (`PB_SB_04`、API 29 専用) |
| android/ instrumented `:ksdialogs` | `:ksdialogs:connectedDebugAndroidTest` | 52 | 0 | 0 |
| MAUI 実配置テストホスト iOS | `simctl launch --console-pty` | 8 シナリオ | 0 | — |
| MAUI 実配置テストホスト Android | logcat `KSDPLACEMENT` | 9 シナリオ | 0 | — |

- `dotnet test` は `net10.0` / `net10.0-ios` / `net10.0-android` の 3 つで `KsDialogs.Maui` をビルドした。`PlatformCurrentPage.cs` の両 OS 版 (`Resource.Id.ksdialogs_container_window` を初めて参照する箇所を含む) はコンパイルが通る。`DialogCurrentPageLocatorTests` は 23 件で、新しい `RejectedRegisteredElementFallsBackToTheDefaultPage` 4 ケースが結果に現れる
- instrumented の結果 XML には、`DialogCurrentPageTests`・`DialogCurrentPageLedgerTests`・`ComposeCurrentPageTests` が現れる。移設した共通の待ちを使う既存テスト (`LoadingAttributeTests`・`ImeSettleWaiting` の利用側) も含め、全件成功した。前回の実行記録で間欠失敗していた `DialogTransparentOverlayTests` も、今回は成功した
- 実配置テストホストの新しいシナリオ `provider-empty-falls-back` (両 OS) と `provider-container-falls-back` (Android) は PASS
- 1 回目の instrumented の実行は、起動直後の AVD の画面が消灯していたため `PresentationHostUnavailable` で大量に落ちた。止めたつもりの実行が Gradle デーモン上で続いていて、2 つの実行が同じ端末で重なった。画面を起こしてから 1 実行ずつ回し直した結果が上表である (環境の問題で、実装とは無関係)
- 補足 (指摘ではない): `scripts/ci-skip-lint.py` は、今の作業ツリーでは `FileNotFoundError` で異常終了する。旧 `android/ksdialogs-core/src/androidTest/.../support/InstrumentedStateSettling.kt` の削除が index に載っておらず、`git ls-files --cached` がまだこのパスを返すためである。コミット時に削除も一緒に stage すれば解消する (削除を stage し忘れると CI の lint job がここで落ちる)

## 解消確認表

| # | 指摘 (出典) | 結果 | 根拠 |
|---|---|---|---|
| 1 | MAUI 層の取得元判定が Native の棄却条件より緩い。空の矩形や (Android) 器の中の要素を返す上書き provider が、MAUI の既定ページへ進まない (second-opinion-code-001 採用 Major) | **解消** | `DialogCurrentPageLocator.ToHostedPlatformView` (`maui/KsDialogs.Maui/Internals/DialogCurrentPageLocator.cs:140`) が platform 側の `judge` の結果で次の取得元へ進む。Android の `Judge` (`maui/KsDialogs.Maui/Platforms/Android/PlatformCurrentPage.cs:81`) は Native の `DialogCurrentPageGeometry.isInPresentingActivity` / `pageRect` と同じ順 (取り付け → 根の一致 → 器の印 → token → 空 → 重なり) で判定する。iOS の `Judge` (`maui/KsDialogs.Maui/Platforms/iOS/PlatformCurrentPage.cs:76`) は Native の `DialogCurrentPageGeometry.pageRect` と同じ順 (window の一致 → safe area の非有限・空 → 重なり) で判定する。単体テスト `RejectedRegisteredElementFallsBackToTheDefaultPage` 4 ケースは、`judge` の結果を無視する実装に戻すと既定ページが選ばれずに失敗する形になっている。実配置では、Android の `provider-container-falls-back` が器の印を読み違えると成立しない (印が読めなければ、同じ Activity の token を持つ器のウィンドウが通り、Native が外して可視領域へ落ち、`barBelowPage` が偽になる)。今回 PASS したので、`Resource.Id` と `Java.Lang.Boolean` のタグの読み方が実機で機能していることも確かめられた |
| 2 | 新しい instrumented テストの落ち着き待ちが共通プリミティブを使っていない (review-001 Minor 2) | **解消** | `InstrumentedStateSettling` / `StateHistory` を内容を変えずに `android/layout-case-fixtures/kotlin/.../support/` へ移した (旧ファイルとの diff は空)。両モジュールの androidTest に既に足してある共有ソースなので、`:ksdialogs` からも参照できる。`DialogCurrentPageStage.awaitObservedSettled` (`android/ksdialogs-core/src/androidTest/kotlin/jp/kamusoft/ksdialogs/support/DialogCurrentPageStage.kt:170`) と `ComposeCurrentPageTests.awaitSettledOrFail` (`android/ksdialogs/src/androidTest/kotlin/jp/kamusoft/ksdialogs/compose/ComposeCurrentPageTests.kt:311`) は、どちらも `awaitSettled` を通る。変化した時点の観測を `StateHistory` に積み、時間切れの説明文に待ちの上限と履歴を添える。handbook は置き場のパスではなく名前だけを挙げているので、移設で handbook の記述は古くならない。`layout-case-fixtures/README.md` に、移した理由の段落が足してある |

### deviation.md との照合

| 項 | 内容 | 実装との照合 |
|---|---|---|
| 1 | Android の台帳の出入り口と印の型を `@KsDialogsInternalApi` (`RequiresOptIn` level ERROR) つきの public にする | 一致。`KsDialogsInternalApi.kt` は `Level.ERROR`。`DialogCurrentPageLedger` / `DialogCurrentPageMarker` に注釈があり、`:ksdialogs` の modifier だけが `@OptIn` している |
| 2 | Android の「同じ Activity のウィンドウ」の解釈 (token の同一性・器の印で除外)。MAUI 層も同じ条件 (token・器の印・空・重ならない) で判定し、外れたら次の取得元へ進む | 一致。Native は `DialogCurrentPageSource.kt:99`・`DialogContainerWindowMark.kt` と、器 3 種の `markAsKsDialogsContainerWindow()` 呼び出し。MAUI は上の解消確認 1 のとおり。追記部分 (MAUI 層の判定) も今の実装と 1 対 1 で対応する |
| 3 | iOS の台帳で、画面に表示されていない印 (`isHidden` / `alpha` < 0.01) を除外する | 一致。`ios/Sources/KsDialogs/CurrentPage/DialogCurrentPageLedger.swift:84` |
| 4 | [付随修正] Sample Android 2 ルートの移動量欄 | 一致。両ルートの `SampleOffsetField.kt:47-48` |
| 5 | [付随修正] Sample Android 2 ルートのステータスバー | 一致。両ルートの `MainActivity.kt` で、API 35 以上のときだけ `isAppearanceLightStatusBars = true` |

## 指摘事項

### 🟠 Major 1. MAUI iOS の「ページが決まらないとき」の逃げ道が Native に外され、内蔵の view controller 走査へ進む
**該当箇所**: `maui/KsDialogs.Maui/Platforms/iOS/PlatformCurrentPage.cs:65` (`return PlatformDialogContent.FindPresentationHost() ?? hostWindow;`)、`ios/Sources/KsDialogs/CurrentPage/DialogCurrentPageGeometry.swift:17`、`maui/macios/native/KsDialogsMauiBridgeTests/MauiBridgeCurrentPageTests.swift:54`

**問題点**:
- MAUI 層でページが決まらない (未描画・提示先の画面が決まらない・既定ページが `Judge` で外れた) とき、iOS 版は提示先の `UIWindow` そのものを Native の登録口へ返す。コメントには「window の safe area の内側は可視領域と一致するため、基準は可視領域と同じになる」とある
- しかし Native の `DialogCurrentPageGeometry.pageRect` は最初に `view.window === window` を確かめる。`UIWindow` の `window` プロパティは nil を返すので、ここで「not in the window presenting the dialog」として外れる
- 実測で確かめた: 作業用 Simulator で起動中の実配置テストホストに lldb で接続し、前面アクティブなシーンに属する window について `[window window]` → `nil`、`[window windowScene]` → `UISceneActivationStateForegroundActive` を得た。scene に載らない `UIWindow` を単体で作った場合も nil だった
- Native の取得元は「台帳 > 登録 > 既定」の順なので、登録口が外れると `DialogDefaultCurrentPageSource` (UIKit の view controller 階層の走査) へ進む
- これは maui-binding の「iOS Native の既定 provider (VC 走査) は MAUI では使わず、MAUI 層の結果を優先すること」と「先端ページの `Handler` が無い (未描画) ときは未解決として dialog-contract の規則に従うこと」(= 可視領域) に反する。VC 走査が何を選ぶかは MAUI の root VC の内部構造次第で、ADR-0038 がリスクとして避けた「MAUI の内部構造への依存」に当たる
- さらに MAUI 層は `The current page could not be resolved, so the visible area is used instead.` を出すが、実際には可視領域になるとは限らない。診断が実際の挙動と食い違う
- 仕組みは今回の採用 Major (MAUI 層が Native に受け付けられないものを渡し、意図した逃げ道を経ない) と同じ型である
- これを固定するはずの bridge テスト `presentationWindowAsPageMatchesVisibleArea` は差を見分けられない。テスト用ホストの root VC は UIKit のコンテナ (presented / navigation / tab) を持たず、VC 走査が選ぶ root の view の safe area は可視領域と同じになる。window が外れて VC 走査へ進んでも、可視領域として扱われても、同じ配置になって通る

**推奨修正**:
- Native が受け付け、safe area の内側が可視領域と一致する View を返す。候補は提示先 window の `RootViewController.View` で、window を満たし、`window` が提示先 window になる。併せてコメントと、`evidence/distill-handoff.md` の「OS 差: iOS は … 提示先 window を返して `visibleArea` と同じ結果にする」を実装に合わせる
- bridge テストは、VC 走査なら可視領域と違う結果になるホスト構成に変える。例: テスト中だけ root を `UINavigationController` (ナビゲーションバーあり) にする、あるいは root VC に `additionalSafeAreaInsets` を持つ子のナビゲーションを置く。そのうえで「逃げ道の View を登録すると可視領域と一致する」を確かめる。機構が無いと何が変わるか: 今の実装 (window を返す) では Native が外して VC 走査へ進み、ナビゲーションバーの下を基準にするので、このテストは失敗する。修正後は root の view が受け付けられて可視領域と一致し、成功する。今のホスト構成のままでは、どちらの実装でも成功する
- 配置の結果だけでなく、Native の診断 (`currentPageDiagnostics` 相当) が出ないこと、または Native が VC 走査の結果を使っていないことを観測に足すと、さらに確実になる

### 🔵 Suggestion 2. iOS の `Judge` の判定そのものは、iOS の実配置シナリオでは見分けがつかない可能性がある
**該当箇所**: `maui/KsDialogs.Maui.PlacementHost/PlacementScenario.cs:144` (`ProviderEmptyFallsBackAsync`)
**問題点**: 共通部分 (`DialogCurrentPageLocator`) が判定の結果で次へ進むことは、単体テストで固定されている。一方、iOS の `Judge` が空の safe area を実際に `EmptySafeArea` と判定しているかは、iOS の `provider-empty-falls-back` シナリオでしか見ていない。ところが iOS では、MAUI 側で外さずに Native へ渡しても、Native が空として外したあとに VC 走査へ進む。MAUI の `TabbedPage` の構成次第では、VC 走査が既定ページと同じ矩形 (タブバーの上まで) を選び、同じ PASS になり得る (Android のシナリオは可視領域へ落ちるので見分けがつく)。今回の実行で PASS したことは、iOS の `Judge` が機能している証拠としては弱い。
**推奨修正**: 指摘 1 を直すと、iOS で MAUI 層がページを決めきれなかったときは可視領域になる。そのため iOS のシナリオも、`Judge` を外すと失敗する形に近づく。そのうえで、シナリオの判定に「ページ領域が可視領域と違う」ことを足すとよい (`ExpectsBarBelowPage` がそれに当たるかを確認する)。機構が無いと何が変わるか: `Judge` が常に受け付ける実装なら、Native の VC 走査の結果によっては同じ PASS になり得る。指摘 1 の修正後は、Native が可視領域へ落ちて `barBelowPage` が偽になるので失敗する。優先度は低い。

## 確認した観点

- 修正の回帰: 移設した `InstrumentedStateSettling` / `StateHistory` を使う既存テストは、全件成功した。移設先は `internal` のまま各モジュールの androidTest に別々にコンパイルされるので、重複定義は起きない (同名の定義はリポジトリ内に 1 つだけ)。README の「公開 API だけで書ける補助に限る」にも収まっている (ライブラリの内部に触れていない)
- 待ちの妥当性: `DialogCurrentPageStage` は外形の不変を条件に含めなくなった。ただし既定の出現演出は `fade` (位置を動かさない) で、外形の条件を持つ待ち (`assertContentRectSettles`) は条件そのものを 48ms 続けて求めるので、問題ない
- 過剰な実装: `DialogCurrentPageRejection` は Native の外す理由と 1 対 1 で、未知の値には例外を投げる。余計な抽象は増えていない。テスト用の `StubPlatformView` は外す理由を 1 つ持つだけで小さい
- 移設で壊れる参照: handbook・スクリプト・ビルド定義で旧パスを参照しているのは、`ci-skip-lint.py` が `git ls-files` 経由で拾う 1 か所だけで、削除を stage すれば解消する (テスト実行の補足を参照)
- 変更範囲全体の見直し: `DialogLayoutHost` の問い合わせ直し、Native の Android / iOS の判定、Compose の台帳、MAUI の辿り方と提示先の解決、実配置テストホストのシナリオを読み直した。指摘 1 以外に Critical / Major は無い。相方レビューの降格指摘 (2 回目の問い合わせが未解決のとき直前の結果を保つ) は、突き合わせ結果の根拠 (1 回目で spec どおり決まる・一時的な null でちらつくおそれ) に異論なし
- review-001 の Minor 1・Suggestion 3〜5 (実配置ホストの自動実行、オプトインの負の検査、Toast gateway、material3 の版) は、修正対象外の扱いのまま (本レビューでも蒸留・後続に回す判断を妨げない)

## アクションプラン

1. (本 change 内・必須) Major 1: MAUI iOS の逃げ道を Native が受け付ける View (提示先 window の root VC の view) にし、bridge テストを VC 走査と見分けられる構成に直す。コメントと distill-handoff.md の該当記述を実装に合わせる
2. (任意) Suggestion 2: 1 の修正後に、iOS の `provider-empty-falls-back` が `Judge` の有無で結果が分かれることを確かめる
3. (コミット時) 旧 `InstrumentedStateSettling.kt` の削除を stage して、`ci-skip-lint.py` が通ることを確かめる
