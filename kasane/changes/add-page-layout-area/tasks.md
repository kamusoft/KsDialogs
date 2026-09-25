# Tasks: add-page-layout-area

## 1. core (ケース表)

- [ ] 1.1 `core/layout-spec/cases.json` に入力 `pageArea` (窓座標の矩形) を足し、`layoutArea: "currentPage"` の追加ケース (下部帯を除いたページで End/End・ページ基準の比率サイズ・全画面ページ = 可視領域一致・safe area の内側を採る例) を機械導出の期待値つきで追加する。既存ケースの期待値は変えない (→ Requirement: 共通ケース表の拡張)

## 2. iOS Native

- [ ] 2.1 `DialogLayoutArea.currentPage` の追加 + 現在ページ provider の内部型 (窓座標の矩形を返す) と `DialogLayoutApplier` (bounds + safeAreaInsets → resolver) への inset 供給の差し込み。未解決時の可視領域フォールバックと診断ログ (→ Requirement: 基準領域「表示中のページ」 / 現在ページは登録された provider から得る / 未解決時は可視領域へ落ちる)
- [ ] 2.2 既定 provider (提示先 window の VC 階層走査: presented → navigation top → tab selected、KsDialogs の器のコンテナ VC は通り抜けて提示元へ戻る、先端 view の `safeAreaLayoutGuide.layoutFrame`) + `UIView?` を返す provider の登録口。取得元の優先順位 (台帳 > 登録 > 既定、上位が空なら下位へ) と提示先 window との同一性判定 (→ Requirement: currentPage と既定 provider (iOS) / provider の上書き (iOS) / 現在ページは登録された provider から得る)
- [ ] 2.3 SwiftUI の `.ksDialogCurrentPage()` modifier と台帳 (attach 中 / 窓外除外 / 入れ子は内側 / 最後に配置 / detach で戻る)。台帳は Compose 側と同じ規則の文書化コメントを持つ (→ Requirement: SwiftUI の modifier (iOS) / modifier の台帳規則)
- [ ] 2.4 テスト: tab + navigation + presented のテストホストでの実配置、表示中の器を通り抜ける (2 枚目のダイアログ)、SwiftUI TabView / NavigationStack のホストで modifier、別ウィンドウの View の除外、取得元の優先順位 (台帳 > 登録 > 既定・空なら下位へ)、上書き provider の優先と解除、modifier の台帳規則 (1 つ / 遷移で消える / 戻る / 入れ子 / 窓外)、未解決 3 種 (未登録相当・nil・例外)、回転での問い合わせ直し (PB-WN-01 の関門)、登録差し替えは次の表示から (→ 上記 Requirement の各 Scenario)
- [ ] 2.5 ケース表ローダーに `pageArea` を足し、ページの safe area を再現するテスト用ホストで追加ケースの実 frame 検証を通す。既存ケースの結果不変を確認 (→ Requirement: ケース表の追加ケースへの適合 (iOS))
- [ ] 2.6 公開 API 形状のコンパイル検査 (非 `@testable` のファイル) に enum 値・登録口・modifier を足す

## 3. Android Native

- [ ] 3.1 `DialogLayoutArea.CURRENT_PAGE` の追加 + provider の内部型と `DialogLayoutHost.visibleAreaInsets` の経路への inset 供給の差し込み。Activity ウィンドウ → 器のウィンドウの変換は**画面座標を共通原点**にする (ページ View は `getLocationOnScreen`、器はその root の `getLocationOnScreen`、Compose の `boundsInWindow()` には Activity の decorView の画面上の位置を足す)。候補は提示先 Activity のウィンドウに属するものに限る。ページ矩形 ∩ 可視領域。未解決時の可視領域フォールバックと診断ログ (→ Requirement: CURRENT_PAGE と provider の登録 (Android) / 未解決時は可視領域へ落ちる)
- [ ] 3.2 `View?` を返す provider の登録口 (既定なし) (→ Requirement: CURRENT_PAGE と provider の登録 (Android))
- [ ] 3.3 Compose の `Modifier.ksDialogCurrentPage()` (Modifier.Node の attach / detach で台帳、`boundsInWindow()`) と台帳規則。台帳が provider に優先 (→ Requirement: Compose の modifier (Android) / modifier の台帳規則)
- [ ] 3.4 instrumented テスト: 下部バー + ページ領域 View のテストホストでの実配置、器と Activity のウィンドウ原点が違うケース、未登録 (可視領域 + 診断ログ)、edge-to-edge、取得元の優先順位 (台帳 > 登録、空なら下位へ)、Scaffold content 枠の modifier、遷移で離脱、入れ子、null / 例外の provider、回転での問い合わせ直し (→ 上記 Requirement の各 Scenario)
- [ ] 3.5 ケース表ローダーに `pageArea` を足し、Activity 側にページ領域 View を置くホストで追加ケースの実 View 検証を通す。既存ケースの結果不変を確認 (→ Requirement: ケース表の追加ケースへの適合 (Android))
- [ ] 3.6 `api-surface-check` に enum 値・登録口・modifier を足す

## 4. MAUI binding

- [ ] 4.1 `DialogLayoutArea.CurrentPage` + bridge の enum (`KSDMauiDialogLayoutArea` / `MauiDialogLayoutArea`) + iOS binding + 写像 (`PlatformDialogContent` の両 OS) の拡張。パススルーテスト (→ Requirement: CurrentPage のパススルー (MAUI))
- [ ] 4.2 提示先の `Window` と `IMauiContext` を一組で返す解決経路を足す (現行 `ResolveMauiContext()` は context だけを返し、`Application.Current.Windows` から選び直すと提示先と違う Window を選び得る)。その `Window` から MAUI 層の既定 provider (ModalStack 先頭 → `Window.Page` 起点、Shell / FlyoutPage / TabbedPage / NavigationPage を降りる) と、先端ページの `Handler.PlatformView` を Native の View provider へ渡す bridge 配線 (iOS の VC 走査より MAUI 層を優先)。未描画は未解決 (→ Requirement: MAUI 層の既定 provider)
- [ ] 4.3 `Page` / `VisualElement` を返す provider の登録口 (MAUI 層の既定より優先) (→ Requirement: provider の上書き (MAUI))
- [ ] 4.4 テスト: TabbedPage+NavigationPage / Shell / モーダル / FlyoutPage / 素の ContentPage のテストホストで両 OS の実配置、上書き provider (→ 上記 Requirement の各 Scenario)。`KsDialogs.Maui.ApiSurfaceCheck` に enum 値と登録口を足す

## 5. Sample (UI)

- [ ] 5.1 ui/brief.md の文言表と承認モックに従い、4 ルートの Layout Dialog パネル画面を改訂する: タブバー (`Panel` / `Info`、アイコン + 文字) の追加、パネルのタブのタイトル帯を OS 標準のナビゲーションバー (戻る `‹` と `Show` をバーに) へ、`Info` タブはタイトルバーなしのページ (説明文 + `Show`)、基準領域の行を 3 択セグメント (Window / Visible area / Current page、初期は Visible area) へ。iOS Native と KMP (iOS) は各タブの content 枠に `.ksDialogCurrentPage()`、Android Native と KMP (Android) は各タブの content 枠に Compose の modifier (Scaffold の topBar / bottomBar の外側)、MAUI は MAUI 層の既定 provider (→ Requirement: レイアウトデモ項目 (属性調整パネル))
- [ ] 5.2 パネル操作部の読み上げ (名前・役割・状態) を sample-parity の規約どおり 4 ルートに与える (セグメントとタブ)
- [ ] 5.3 mock との視覚照合 (approved.png 基準、4 ルート × 6 状態: パネル初期 / Current page + End/End / Visible area + End/End / Current page + Start/Start / Info タブ / Info タブで Current page + Start/Start) を verification/ に保存
- [ ] 5.4 `kasane/handbook/cross/sample-parity.md` の文言表 (基準領域の行・タブ名・Info タブの文言と表示操作) と「Layout Dialog の属性調整パネル」節 (タイトル帯がナビゲーションバーになる・タブ構成) を ui/brief.md と一致させる

## 6. 検証と申し送り

- [ ] 6.1 4 形態の test-execution 規約どおりのテスト実行 (iOS: スイート直列 / Android: instrumented / MAUI / KMP のホスト) と件数の確認
- [ ] 6.2 蒸留への申し送りを evidence に残す: `concepts/core/api/layout-semantics.md`「基準領域」節と各 platform の `layout-surface.md` の追随点、core/ADR-0038 の accepted 昇格と ADR-0008 Decision 3 への注記、ColorAnalyzer への返事 (outbox)、docs-refresh で移行スキルの対応表を `true → CurrentPage` に直す件
