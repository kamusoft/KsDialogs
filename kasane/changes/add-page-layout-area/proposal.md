# Proposal: add-page-layout-area

## Why

AiForms.Maui.Dialogs から KsDialogs.Maui へ移行した消費者 (ColorAnalyzer) で、両軸 End に置くメニューがタブを持つ画面でタブバーに重なった (知らせ: `../ColorAnalyzer/kasane/outbox/KsDialogs/2026-09-25-page-location-layout-area.md`)。原典の `UseCurrentPageLocation=true` は表示中ページの矩形 (ナビバー・タブバーの内側) が基準だったのに対し、KsDialogs の基準領域は `window` / `visibleArea` の 2 値で、`visibleArea` (window − システム insets) はアプリのタブバーを含む。移行スキルの対応表もこの 2 値へ写しているため、タブ・ナビバーを持つ画面では原典と同じ位置にならない。

器はページ構造を知らない設計 (core/ADR-0030・0032) を Dialog にそのまま当てると、利用側がページごとにタブバーの高さを測って offset で寄せることになる。呼び出しは共有層 (KMP commonMain / ViewModel) から行うのが基本なので、show ごとに基準 View を渡す形も採れない。探索 (exploration.md) で、基準領域に 3 つ目の値「表示中のページ」を足し、器は登録された現在ページ provider から矩形を得る形に決めた (core/ADR-0038、proposed)。

## What Changes

- **dialog-contract (core)**: `layoutArea` に 3 つ目の値 `currentPage` を足す。基準矩形は「表示中ページの矩形のうち、そのページ自身の safe area / システム insets の内側」で、水平・垂直の両軸に効く。器は登録された現在ページ provider から矩形を得て窓座標の 4 辺 inset に変換し、既存の rect 決定手順 (基準 rect R → 有効領域 A) にそのまま流す。取得元の優先順位は modifier の台帳 > 登録 provider > 既定 provider で、上位が空なら下位へ進む。候補は提示先と同じ window / Activity に属するものに限る。すべて辿っても未解決 (provider が矩形を返せない・ページが未描画) のときは `visibleArea` と同じ結果。共通ケース表 `core/layout-spec/cases.json` に任意の基準矩形を表すスキーマを足し、`currentPage` のケースを追加する
- **ios-native**: `DialogLayoutArea.currentPage`。既定 provider を内蔵 (key window の VC 階層を presented → navigation の top → tab の selected と先端まで走査した VC の view の `safeAreaLayoutGuide.layoutFrame`)。走査は KsDialogs 自身の器を通り抜けて提示元へ戻る。既定 provider の保証範囲は UIKit のコンテナまでで、SwiftUI の `TabView` / `NavigationStack` (root が `UIHostingController`) には保証を及ぼさない。UIKit の上書きは `UIView?` を返す provider を一度登録する口。SwiftUI は `.ksDialogCurrentPage()` view modifier が正規の経路 (attach 中のものを台帳に持ち、窓外は除外・入れ子は内側・それ以外は最後に配置されたものが勝ち・detach で残りへ戻る)。既存の `DialogKeyWindowProvider` / `DialogLayoutApplier` (bounds + safeAreaInsets → resolver) の経路に inset の供給元として差し込む
- **android-native**: `DialogLayoutArea.CURRENT_PAGE`。既定 provider は持たない (登録制)。従来 View 向けにページの `View?` を返す provider の登録口、Compose 向けに `Modifier.ksDialogCurrentPage()` (Modifier.Node の attach / detach で台帳を管理、`boundsInWindow()` を返す。規則は iOS の modifier と同じ)。矩形はページ view の矩形 ∩ 可視領域。器 (`android.app.Dialog` の別ウィンドウ) と Activity ウィンドウは画面座標を共通原点にして突き合わせ (`getLocationOnScreen`)、 `DialogLayoutHost.visibleAreaInsets` の経路へ流す
- **maui-binding**: `DialogLayoutArea.CurrentPage` + bridge 2 つ (`KSDMauiDialogLayoutArea` / `MauiDialogLayoutArea`) と写像の拡張。既定 provider は MAUI 層で持つ: `ResolveMauiContext()` のホスト `Window` から ModalStack の先頭 (無ければ `Window.Page`) を起点に `Shell → CurrentPage` / `FlyoutPage → Detail` / `TabbedPage → CurrentPage` / `NavigationPage → CurrentPage` を容れ物でなくなるまで降り、先端ページの `Handler.PlatformView` を Native の View provider へ流す (両 OS 同じ 1 本)。利用者の上書きは `Page` / `VisualElement` を返す provider。パススルーテストと、TabbedPage を持つテストホストでの実配置の確認
- **テスト**: ケース表の追加ケースを iOS / Android の実 frame 検証に載せる。provider の登録・未登録・未解決のフォールバック、modifier の台帳規則 (最後に配置・detach で戻る・入れ子・窓外除外)、iOS 既定 provider の VC 走査 (navigation / tab / presented)、MAUI 既定 provider の辿り (Shell / Tabbed+Navigation / Flyout / モーダル) を Scenario で固定する
- **samples (UI 変更)**: 4 ルートの Layout Dialog デモ画面 (属性調整パネル) にボトムタブバーを付け (「パネル」と説明文だけの 2 つ目のタブ)、LayoutArea のトグルを 3 択 (window / visibleArea / currentPage) のセグメントに変える。End/End + currentPage でタブバーを避け、visibleArea でタブバーに重なる様子を 4 ルートで並べて見られるようにする。Sample が検証装置 (handbook/cross/sample-parity.md) なので本 change に含める。見た目は `ui/` (brief + mock 承認) で決め、文言表 (sample-parity) にタブ名とセグメント名を足す
- **concepts**: `concepts/core/api/layout-semantics.md` の「基準領域」節と各 platform の `layout-surface.md` の追随は蒸留時 (ksn-distill) に行う。core/ADR-0038 は実装に埋め込まれた時点で accepted へ昇格し、ADR-0008 Decision 3 への注記 (基準の再定義) は 0038 の Consequences で扱う

## 実現経路と確認先 (lessons spec-review L-001 / L-002)

- iOS: inset の供給元は `ios/Sources/KsDialogs/Layout/DialogLayoutApplier.swift` (bounds + `safeAreaInsets` → `DialogLayoutResolver`)、提示先の走査は `Presentation/UIKitDialogPresentationSurface.swift` (`keyWindowProvider` → presented 連鎖)、SwiftUI の既存 modifier は `SwiftUI/DialogAttributeAttachment.swift`。`safeAreaLayoutGuide.layoutFrame` は UIKit 標準
- Android: inset の供給元は `android/ksdialogs-core/.../DialogLayoutHost.kt` の `visibleAreaInsets` (差し替え可能な関数引数)、Activity は `ResumedActivityProvider.kt`、Compose の既存添付は `android/ksdialogs/.../compose/KsDialogAttributes.kt`。Compose は 1.8.1 (`android/gradle/libs.versions.toml`) で `Modifier.Node` の attach / detach と `LayoutCoordinates.boundsInWindow()` が使える。`getLocationInWindow` は `DialogAttributeSupplyTests.kt` 等に前例
- MAUI: ホスト `Window` は `maui/KsDialogs.Maui/Platforms/{iOS,Android}/PlatformDialogContent.cs` の `ResolveMauiContext()` が既に選んでいる。`Window.Page` / `Navigation.ModalStack` / `Shell.CurrentPage` / `FlyoutPage.Detail` / `TabbedPage.CurrentPage` / `NavigationPage.CurrentPage` / `Handler.PlatformView` は MAUI の公開 API。View → 窓座標の変換は `Contract/DialogTransition.cs` (`MeasureSlideGeometry`) に前例。enum の写像は同 `PlatformDialogContent.cs` と bridge (`maui/macios/native/KsDialogsMauiBridge/MauiDialogAttributes.swift`、`maui/android/native/.../MauiDialogAttributes.kt`)
- 共通ケース表: `core/layout-spec/cases.json` (入力は `screen` + `insets`。`pageArea` を足す)。ローダーは iOS `DialogLayoutCaseTableTests.swift` / Android `support/DialogLayoutMeasurement.kt`

## Non-Goals

- **KMP commonMain への `layoutArea` の公開**: `DialogPlacement` は配置だけを持ち、器のメタ属性はホスト側の Native 添付で効く現状を維持する (オーナー決定)。器のメタ属性一式を共有層へ出すかは別の設計論 (ADR-0038 Revisit When)。Native 側の対応で KMP のホスト添付からも `currentPage` は効く
- **Sample の新しいデモ項目 (専用のタブ画面)**: 既存の Layout Dialog パネルにタブバーを足す方 (A 案) を採ったため作らない。ナビゲーションバーを持つ画面の実演も同じ理由で持たない (タブバーで基準領域の差は見える)
- **移行スキル (`skills/{ja,en}/ksdialogs-aiforms-migration`) の対応表更新**: `skills/` の更新は docs-refresh 経由のみ (CLAUDE.md)。実装後に `true → CurrentPage` (provider の説明つき) へ一括で直す。暫定注記は入れない (オーナー決定)
- **Loading / Toast への `currentPage` の適用**: Loading は既存のアプリ既定 options に `layoutArea` を含むため値を足せば指定自体は通るが、Toast の既定配置 (ADR-0032 のボトムバー回避オフセット) との整合は別論点。本 change の Scenario は Dialog に限り、Loading / Toast での挙動は「値を渡せば Dialog と同じ規則で解決する」以上を固定しない
- **表示中に画面遷移してページが変わったときの再配置**: 現契約の再配置トリガーは窓寸法と insets の変化だけ (layout-semantics「表示中に画面のほうが変わったとき」)。ページの変化を新しいトリガーに加えるかは、モーダルな Dialog の下でページが変わる場面自体が稀なため保留 (探索の未決)
- **`fix-layout-contract-gaps` の 3 件** (非有限値の適用順・添付の時点・添付競合): 別 change。本 change が触る「基準領域」節とは重ならない

## Impact

- 破壊的変更なし。enum の値と登録口の追加のみで、既定 (`visibleArea`) と既存指定の結果は変わらない。ケース表の既存ケースの期待値も変えない
- 公開 API の追加: enum 値 1 つ (3 面 + MAUI bridge 2 つ + iOS binding)、provider の登録口 (iOS / Android / MAUI)、modifier 2 つ (SwiftUI / Compose)
- Sample: Layout Dialog パネルの承認済みモック (add-layout-spec の approved-layout-panel.png) を本 change の承認モックで置き換える。文言表の追加行は 4 ルート一斉
- accepted ADR との関係: ADR-0030・0032 の「器はページ構造を知らない」に Dialog の基準領域だけ例外を設ける (器は探さず教えてもらう)。Toast の自動検知の却下は据え置き。この整理は ADR-0038 (proposed) に記録済み
- 相方スペックレビュー (second-opinion-spec-001.md) の指摘 6 件を採用済み: 器自身を現在ページに選ばない走査、SwiftUI は modifier を正規経路にする (Sample の iOS 系も modifier)、Android は画面座標を共通原点にする、取得元の優先順位、提示先ウィンドウとの同一性、MAUI の提示先 Window の配管
- リスク: (1) iOS 既定 provider の VC 走査は UIKit コンテナまでの保証で、SwiftUI / MAUI Shell の内部構造には依存させない (SwiftUI は modifier、MAUI は MAUI 層の provider) (2) Android の器は別ウィンドウで、Activity ウィンドウとの原点差 (マルチウィンドウ・cutout) の変換を誤ると全体がずれる — instrumented テストで Activity 側の View から得た矩形と器の配置を突き合わせる (3) Android Native は登録しないと `currentPage` が `visibleArea` と同じになる — 診断ログ (handbook/cross/diagnostic-message-language.md の言語規約) で未登録を知らせる

## 級: M

能力は 1 つ (レイアウト契約の基準領域) だが core 契約 + 3 面の実装と独立 verify が要り、公開 API に追加が入るため。

domain: cross
