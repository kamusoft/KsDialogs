# レビュー結果: fix-maui-android-dialog-content-size (001 回目)

**日付**: 2026-09-27
**判定**: APPROVED

## サマリー

Android の MAUI 層に iOS の `DialogContentView` と同じ役割の包みを置き、MAUI の測り方で中身の大きさを決めるという合意スコープどおりの修正になっている。測定 (制約つき / 制約なし)・arrange・添付の写しの節目・演出の結び付け・切り取りの扱いは iOS の姉妹面と対応しており、採用時点 (core/ADR-0015) の意味も修正前から変わっていない。大きさのシナリオは修正前の Android で ContentView ルートの 3 件だけが FAIL する A/B を取れており、症状を区別できる。残る指摘は、比率指定・fill の軸 (EXACTLY) で起きる見え方の変化が証跡に無いことと、テストの判定条件を強められる余地の 2 件 (いずれも Minor)。

## 照合した規約

- comment-policy.md (always) — 追加コメントの参照形式 (`core/ADR-0015` のみ)・記述類型を照合。`scripts/comment-policy-lint.py --summary` は禁止 0 件
- test-execution.md (テストを実行するとき・完了を判定するとき) — MAUI の 3 実行のうち `dotnet test` と Android 互換面を実行
- runtime-behavior-verification.md (表示の演出・OS の提示機構が絡む不具合の修正を完了判定するとき) — 修正前の再現・同一手順での解消・`evidence/` への証跡の 3 点を照合

## 実行したビルドとテスト

| 実行 | 結果 |
|---|---|
| `dotnet build maui/KsDialogs.Maui/KsDialogs.Maui.csproj -f net10.0-android` | 成功 (警告 0) |
| `dotnet build maui/KsDialogs.Maui.PlacementHost/...csproj -f net10.0-android` | 成功 (警告 0) |
| `dotnet build maui/KsDialogs.Maui.PlacementHost/...csproj -f net10.0-ios` | 成功 (警告 0) |
| `dotnet test maui/KsDialogs.Maui.Tests` | 197 件合格 / 失敗 0 / skip 0 |
| `./gradlew :ksdialogs-maui-bridge:test --rerun-tasks` (maui/android/native) | 38 件合格 / 失敗 0 / skip 0。worktree に `local.properties` が無いため `ANDROID_HOME` を環境変数で渡して実行 (ファイルは作っていない) |
| iOS 互換面 (`xcodebuild test`) | 未実行。この diff は iOS 互換面・MAUI の iOS 側のコードに触れていない。起動中の Simulator を流用しない運用のため、完了判定で回す場合はオーケストレーター側で別デバイスを boot して回すこと |
| `scripts/local-path-lint.py` / `scripts/identity-lint.py` | どちらも exit 0 (evidence/ を含む) |

実配置テストホストの実機再実行はしていない (証跡とコードで判定)。

## 確認した観点

- **iOS の姉妹面との対応 (lessons/process.md L-001)**: 測定は両面とも `IView.Measure` (制約なしの軸は無限大) で、Android は `MeasureSpec.ToDouble` が UNSPECIFIED を無限大、それ以外を dp に換算する。arrange は両面とも包みの領域全体へ `IView.Arrange`。添付の写しは両面とも包み自身 (互換面の `view`) が対象。演出フックへ渡すのは両面とも中身の MAUI View。固定の条件は iOS が `Window is not null`、Android が `IsAttachedToWindow` で対応している。差分は `TransferBeforeLayout` の呼び所 (iOS は各 `LayoutSubviews`、Android は attach 時の 1 回) だが、これは修正前の Android (`ViewAttachedToWindow`) と同じで、この change が持ち込んだ差ではない
- **採用時点 (core/ADR-0015) の意味**: 修正前は中身の platform view の `ViewAttachedToWindow` で暫定の写し、`LayoutChange` で固定だった。修正後は包みの `OnAttachedToWindow` と `OnLayout` (中身の `Arrange` の後)。Android の `View.layout` は `onLayout` と `OnLayoutChangeListener` を同じ条件で呼ぶため、節目は同じ。attach 前の layout で固定しない条件が加わった分、「画面に載せたあとの初回パス」に修正前より厳密に揃っている。器は写し先 (`bridgeContent.view` = 包み) から添付を読み (`MauiDialogBridge.kt` / `MauiLoadingBridge.kt` / `MauiToastBridge.kt` の `applyAttributes(content.view, ...)`)、演出も `installTransition` で同じ `view` に載るため、包みへ差し替えても整合する。C# の event 購読 (lambda) が override になり、購読の解除漏れの懸念も無くなった
- **タップ・Toast の素通し**: 包みは clickable を立てない素の `ViewGroup` で、タッチを中身へ流す。Dialog / Loading のタップの遮りは Native の `DialogContentHolder` (`isClickable = true`) が持ち、Toast はウィンドウ単位の `FLAG_NOT_TOUCHABLE` なので包みは経路に関与しない。証跡の `sample-android-toast-tap-through.png` は記述どおり (覆いの下に Toast 2 枚、手前に Basic Dialog)
- **演出で中身が外へ動くときの切り取り**: 包みが `SetClipChildren(false)` を持ち、親の `DialogContentHolder`・`DialogLayoutHost` も `clipChildren = false` なので、中身が包みの外へ出ても切り取られない。`sample-android-custom-hook-midframe.png` は出現途中の半透明の中身が欠けずに描かれていて、記述と一致する
- **測定条件の換算**: dp→px は MAUI の `ToPixels` (切り上げ) を `int` へ、px→dp は `FromPixels`。AT_MOST は `ResolveSize` で上限へ、EXACTLY は指定の大きさに揃う。Native の器 (`contentHolderMeasureSpec` と `DialogContentHolder.contentMeasureSpec`) は AT_MOST か EXACTLY しか渡さないので、UNSPECIFIED の経路は現状通らない
- **比率指定 (EXACTLY) の軸**: 外形 (rect) は器が決めた大きさのまま (契約どおり)。包みの中でルートがどう配置されるかは MAUI の `ComputeFrame` に従う。これは iOS (`Arrange(Bounds)`) と同じ → 指摘 1
- **大きさのシナリオの検出力 (lessons/code-review.md L-001)**: 修正前の Android では ContentView ルートの 3 件が `shown=25.5x18.7` で FAIL、Grid 3 件と最小値の 1 件は PASS (探索で予測した対照どおり)。修正後は 16/16 PASS。iOS も 15/15 PASS。「包みが無いと何が変わるか」が ContentView 3 件の合否で分かれるため、修正の機構を区別できている → 強化の余地は指摘 2
- **既存の位置シナリオ**: 修正後も 9 件すべて PASS。中身の外形が 95x49 px (約 36x19 dp) から 315x210 px (= 120x80 dp) に戻っており、端の位置で判定するため合否は変わらない (探索の見込みどおり)。iOS に `provider-container-falls-back` が無いのは `PlacementScenario.cs` の `#if ANDROID` によるもので、証跡の件数 (Android 16 / iOS 15) と一致する
- **Native の器・Kotlin 互換面・公開 API**: `android/` と `maui/android/native/` には差分なし。変更は `internal static class` 内の private 入れ子型と private メソッドの削除だけで、公開 API は変わっていない
- **コメント**: 包みの remarks は現在形の自己完結した説明で、外部参照は `core/ADR-0015` だけ (許容形式)。公開 doc コメントへの ADR 混入なし
- **付随修正・deviation**: deviation.md は無い。diff は合意スコープ (Android の包みと実配置テストホストの大きさシナリオ・ホストの説明の拡張) の範囲に収まっている

## 指摘事項

### 🟡 Minor 比率指定・fill の軸で、明示サイズを持つルートの見え方が Android で変わるのに証跡が無い

**該当箇所**: `maui/KsDialogs.Maui/Platforms/Android/PlatformDialogContent.cs:233-240`
**問題点**: 器が大きさを決めた軸 (ProportionalWidth / Height、または Fill 配置) では、包みは EXACTLY の大きさになり、中身の `Arrange` は領域全体で呼ばれる。MAUI の `ComputeFrame` は、明示サイズ (`WidthRequest` 等) を持つルートを宣言した大きさで領域の中央に置く。そのため修正後の Android では、たとえば `ProportionalWidth=0.8` と `WidthRequest=160` の ContentView ルートを組み合わせると、ルート (背景) は 160 のまま中央に置かれ、両脇は透明になる。修正前はネイティブの EXACTLY がそのまま platform view に渡り、ルートは領域いっぱいに広がっていた。

外形 (rect) は契約どおりで、配置はコード上 iOS と同じなので、不具合ではない。ただし Android では見え方が変わるのに、探索の記録にも証跡にも無く、実機で両 OS が揃うことも確かめられていない。コンテキストパッケージの重点観点にある「比率指定 (EXACTLY) の軸での挙動」は、現状ではコード読解でしか裏付けられていない。

**推奨修正**: 比率指定 (または Fill) を付けて明示サイズのルートを出す構成を 1 つ、両 OS で走らせる。実配置テストホストのシナリオとして足すか、手動の観測を証跡に足す。記録するのは外形とルートの矩形。外形は比率どおり、ルートは宣言サイズで中央、という形で両 OS が揃うことを残す。

区別できる根拠: 包みが無い修正前の Android ではルートが外形の幅いっぱいになるので、ルートの幅で修正前後が分かれる。あわせて、この見え方の変化 (iOS に揃った) を evidence かアーカイブ時の記録へ一文残すと、蒸留で拾える。

### 🟡 Minor 大きさのシナリオがルートの矩形しか判定せず、外形とルートの Width / Height を合否に入れていない

**該当箇所**: `maui/KsDialogs.Maui.PlacementHost/PlacementRunner.cs:176-183`
**問題点**: 判定しているのはルートの platform view の矩形だけで、ダイアログの外形 (包み / 器) の大きさは見ていない。包みが「測定では大きい値を返し、arrange では中身を宣言サイズで中央に置く」形に壊れても (たとえば AT_MOST で上限をそのまま返す)、ルートは 160x100 のままなので、大きさのシナリオは PASS してしまう。この形の退行は、いまは位置のシナリオ (End / End 寄せの端の位置) が偶然拾う構造になっている。

また、この修正で届くようになったルートの `Width` / `Height` は `rootBounds` としてログに出るだけで、合否に入っていない。修正前の証跡では Grid ルートも含めて全件 `rootBounds=-1x-1` だった。これを合否に入れれば、併発していた症状 (ルートに配置が届かない) も検出できる。

**推奨修正**: 次のどちらか (または両方) を足す。
- 大きさのシナリオも End / End に寄せて出し、ルートの右端・下端が期待する領域の端から余白分内側にあることを判定する。これなら包みの内部構造に依存せずに、外形がルートより大きい退行を検出できる
- `content.Width` / `content.Height` が期待値と一致することを合否に入れる。修正前のビルドでは Grid 3 件と最小値の 1 件も FAIL に変わるので、併発症状を区別できる

### 🔵 Suggestion Toast が一度も表示されなかったとき、次のシナリオと重なり得る

**該当箇所**: `maui/KsDialogs.Maui.PlacementHost/PlacementRunner.cs:220-221`
**問題点**: 閉じる操作の `UntilDetachedAsync` は、`ContentRect` が最初から null (測定の時間切れで表示を捕まえられなかった場合など) だと即座に戻る。遅れて出た Toast が次の Toast シナリオの測定と重なり得る。現状の証跡では起きていない。
**推奨修正**: Toast の閉じる操作では、少なくとも `ToastDurationMs` と退出の演出の時間が過ぎるまで待ってから、画面から外れるのを待つ。

## 所見 (指摘ではない)

- `sample-android-checks.md` の末尾にある「時間スライダーの変更が効いていない」は Sample ページ側の操作 (adb の tap でスライダーを動かせたか) の話で、包みの経路とは関係しない。修正前から同じかどうかは記録されていない。気になるなら別途確かめる程度でよい
- `OnMeasure` は、MAUI 側が無限大の大きさを返し、かつ測定条件が UNSPECIFIED のときに `int` へのキャストが飽和する。ただし現状の器は UNSPECIFIED を渡さないため、実害の経路は無い

## アクションプラン

1. (Minor) 比率指定 / Fill の軸で明示サイズのルートを出す構成を両 OS で観測し、外形とルートの矩形を証跡に残す。Android の見え方が変わった (iOS に揃った) ことを一文記録する
2. (Minor) 大きさのシナリオの合否に、外形の検出 (End / End 寄せの端の判定) とルートの `Width` / `Height` を足す
3. (Suggestion) Toast シナリオの閉じ待ちに、表示時間ぶんの下限を入れる
4. 完了判定の前に iOS 互換面のテストを、別に boot したデバイスで回す (handbook test-execution の 3 実行)
