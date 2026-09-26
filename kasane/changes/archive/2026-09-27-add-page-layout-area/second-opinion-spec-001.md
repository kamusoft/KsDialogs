# セカンドオピニオン: add-page-layout-area (spec-001)
**相方**: codex / **label**: so-spec-add-page-layout-area / **日付**: 2026-09-26 / **対象**: 提案一式 (proposal.md / specs/*/spec.md / tasks.md / ui/brief.md)
---
# 独立スペックレビュー: add-page-layout-area

提案資料、現行のレイアウト契約、指定された ADR、ケース表、関連する提示・レイアウト実装を静的に照合しました。**現状の仕様には、iOS のページ選択、Android の座標変換、provider 間の優先順位に実装前に決めるべき問題があります。** ユーザー指定に従い、ビルド・テストとファイルへの書き込みは行っていません。

## 指摘事項

### 🟠 Major — iOS の既定 provider がダイアログ自身を現在ページとして選び得る

**該当箇所**: `specs/ios-native/spec.md:7`  
**問題点**: 現行の提示処理は、提示先の `presentedViewController` の先端にダイアログの器を追加します（`ios/Sources/KsDialogs/Presentation/UIKitDialogPresentationSurface.swift:23-27,50-57`）。提案の走査を表示後に行うと、その器自身、または重ねて表示中の別の器へ到達し、背後のページの safe area を取得できません。  
**推奨修正**: ライブラリの器を走査対象から除くか、提示元ページを表示ごとに保持するかを契約で決め、単独表示と重ね表示の Scenario を追加してください。

### 🟠 Major — SwiftUI の既定 provider によるバー除外が、指定された走査経路では保証されない

**該当箇所**: `specs/ios-native/spec.md:19-22`、`specs/samples/spec.md:7`  
**問題点**: 走査対象は UIKit の presented／navigation／tab の各 controller です。一方、iOS Sample は `WindowGroup` の SwiftUI 画面から始まります（`samples/ios/KsDialogsSample/KsDialogsSampleApp.swift:12-15`）。`TabView` と `NavigationStack` の内側のページ矩形に、この走査だけで必ず到達する根拠がありません。Sample も既定 provider のみを指定しているため、4 ルートの位置検証が詰まる可能性があります。  
**推奨修正**: 対象の SwiftUI 構成で実際に得られる VC と safe area を先に確認してください。到達を保証できなければ、SwiftUI Sample では `.ksDialogCurrentPage()` を使う契約に改め、既定 provider の保証範囲を限定してください。

### 🟠 Major — Android の別ウィンドウ間座標変換に共通の原点が定義されていない

**該当箇所**: `tasks.md:18`、`specs/android-native/spec.md:7`  
**問題点**: 指定された `getLocationInWindow` は、Activity 側と Dialog 側でそれぞれ別のウィンドウを原点にします。両者の値を差し引くだけではウィンドウ間の原点差を得られず、提案が挙げるマルチウィンドウ等でずれます。既存の同 API の使用箇所は同一ウィンドウ内の変換です（`android/ksdialogs-core/src/main/kotlin/jp/kamusoft/ksdialogs/DialogTransition.kt:139-140`）。  
**推奨修正**: 両 View を共通座標系で測って Dialog root へ変換する手順と、異なるウィンドウ原点を持つ受け入れケースを明記してください。Compose の `boundsInWindow()` も同じ変換条件に含めてください。

### 🟠 Major — 明示 provider と modifier の優先順位が未定義

**該当箇所**: `specs/dialog-contract/spec.md:36`、`specs/ios-native/spec.md:26,40`  
**問題点**: iOS では登録 provider と SwiftUI modifier の双方が既定 provider より優先しますが、双方が同時に有効な場合の勝者が決まっていません。実装順によって利用者の明示指定が無視され得ます。  
**推奨修正**: 全取得元の優先順位と、上位の取得元が `nil`・空矩形を返した場合に下位へ進むかを定め、競合 Scenario を追加してください。

### 🟠 Major — 「表示中のページ」の候補に提示ウィンドウとの同一性条件がない

**該当箇所**: `specs/dialog-contract/spec.md:55,74`  
**問題点**: View が「窓に載っている」ことと、今回ダイアログを出す窓に載っていることは別です。modifier 台帳も矩形が窓と重なるかだけで選ぶため、複数ウィンドウで座標が重なれば別ウィンドウの候補を選べます。  
**推奨修正**: provider の View と modifier 候補は提示先と同じ window／Activity に属する、と規定してください。別ウィンドウの候補を除外する Scenario も必要です。

### 🟡 Minor — MAUI の提示先 Window を得る配管が tasks に特定されていない

**該当箇所**: `tasks.md:28`  
**問題点**: 提案は「`ResolveMauiContext()` のホスト `Window`」からページを辿りますが、現行メソッドが返すのは `IMauiContext` だけです（`maui/KsDialogs.Maui/Platforms/Android/PlatformDialogContent.cs:114-124`、iOS も同型）。別途 `Application.Current.Windows` から選び直すと、提示先と異なる Window を選ぶ余地があります。  
**推奨修正**: 提示先の Window と context を一組として渡す経路を tasks に明記してください。

## アクションプラン

まず iOS の提示中の走査対象、SwiftUI で保証できる取得経路、Android の共通座標系を確定してください。その上で provider の優先順位と提示ウィンドウの同一性をデルタスペックに追加し、各問題を識別できる Scenario にしてください。ADR-0038 は *proposed* として参照し、判定根拠にはしていません。

**総合判定: NEEDS_DISCUSSION**


## 突き合わせ結果 (ホスト側の自己レビューとの照合、2026-09-26)

ホスト側の自己レビュー (ksn-propose Step 8 の 2 周) では以下のいずれも検出していない。全件「相方のみ + 根拠強 (該当箇所特定・実害シナリオあり)」として**採用**し、提案に反映した。

| # | 指摘 | 採否 | 反映先 |
|---|---|---|---|
| 1 | iOS の既定 provider がダイアログの器自身 (presented の先端) を現在ページに選ぶ | 採用 | ios-native: 走査はライブラリの器 (Dialog / Loading / Toast のコンテナ VC) を除外し、重ね表示の Scenario を追加 |
| 2 | SwiftUI の TabView / NavigationStack に UIKit 走査だけで届く根拠がない (root は UIHostingController) | 採用 | ios-native: 既定 provider の保証範囲を UIKit コンテナ (presented / navigation / tab) に限定し、SwiftUI は `.ksDialogCurrentPage()` を正規の経路にする。samples / brief: iOS Native と KMP (iOS) の Sample は modifier を使う |
| 3 | Android の別ウィンドウ間の座標変換に共通原点がない (`getLocationInWindow` は各ウィンドウ原点) | 採用 | android-native: 共通原点を画面座標 (`getLocationOnScreen` / Activity ウィンドウの画面上の位置) と規定し、原点が異なるケースの Scenario を追加。tasks 3.1 を修正 |
| 4 | 登録 provider と modifier の優先順位が未定義 | 採用 | dialog-contract: 取得元の優先順位 (modifier 台帳 > 登録 provider > 既定 provider) と、上位が候補なし / null / 例外なら下位へ進む規則を追加。競合 Scenario を追加 |
| 5 | 候補が提示先と同じウィンドウに属する条件がない | 採用 | dialog-contract: provider の View と modifier 候補は提示先と同じ window / Activity に属するものに限る規則を追加。ios-native に別ウィンドウ除外の Scenario |
| 6 | MAUI の提示先 Window を得る配管が tasks に無い (`ResolveMauiContext()` は `IMauiContext` しか返さない) | 採用 (Minor) | tasks 4.2: 提示先 Window と context を一組で返す経路の追加を明記 |

未解決: なし。総合判定 NEEDS_DISCUSSION は上記反映で解消したとホスト側が判断 (設計判断の変更を伴う #2 はオーナーへ報告)。
