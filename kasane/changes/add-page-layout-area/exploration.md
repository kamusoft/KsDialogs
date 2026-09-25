# Exploration: add-page-layout-area

## 課題 / 動機

ColorAnalyzer からの知らせ (`../ColorAnalyzer/kasane/outbox/KsDialogs/2026-09-25-page-location-layout-area.md`、kind: change) を起点とする。

ColorAnalyzer は AiForms.Maui.Dialogs から KsDialogs.Maui へ移行した。移行後、右下に出すメニュー (水平・垂直とも End 配置。AiForms では `UseCurrentPageLocation=True`) が、タブを持つ画面でタブバーに重なるようになった。

- ずれの大きさ (ColorAnalyzer の実機計測): iOS (iPhone 11) で約 49pt、Android (Pixel 6a) で約 56dp。どちらもタブバーの高さに一致する
- AiForms の `UseCurrentPageLocation=True` は、表示中のページの矩形を基準にしていた (知らせによる)。この矩形はタブバー・ナビゲーションバーの内側で終わる
  - iOS: `DialogHelpers.GetCurrentPageRect` がアクティブページの PlatformView の Bounds を使う
  - Android: `ReusableDialog.Android.cs` がページ位置から余白を計算する
- KsDialogs の `DialogLayoutArea.VisibleArea` は「ウィンドウから insets (システムバー等) を除いた領域」で、タブバーを含む (`kasane/concepts/core/api/layout-semantics.md`)
- 移行スキル `ksdialogs-aiforms-migration` の `references/api-mapping.md` は、`UseCurrentPageLocation=true` の写し先を `VisibleArea` としている。このため、タブバーやナビゲーションバーを持つ画面では同じ位置にならない

ColorAnalyzer のオーナーの希望は次の 2 つ。

- 基準領域に「表示中のページの矩形」を選べるようにしてほしい (MAUI 面なら `DialogLayoutArea` に 3 つ目の値を足す等)
- 移行スキルの対応表の写し先を、足した値に直してほしい。足すまでの間は「`VisibleArea` とは位置が一致しない」旨の注記を入れてほしい

ColorAnalyzer は、足されるまで `VisibleArea` のまま (タブバーに重なる状態で) 置き、足された版を取り込むときに切り替える。

## 検討した選択肢 (却下案と理由を含む)

論点 A (基準の与え方) — 2026-09-25:

- 案 1「呼び出し側が show ごとに基準 View / 矩形を渡す」 — 却下: 共有層 (KMP commonMain / VM) から呼ぶのが基本で、渡しようがない (オーナー指摘)
- 案 2「器が表示中のページを自力で見つける」 — 部分採用: iOS (VC 階層走査) / MAUI (`Window.Page` から Shell / NavigationStack / ModalStack) は確実に辿れる。Android は OS 標準の画面遷移機構がなく (Jetpack Navigation はライブラリ、Compose の NavHost は View を持たない)、自力では定義できない
- 案 3「足さない (アプリが margin / offset で寄せる)」 — 却下: ColorAnalyzer のオーナーが採らない判断
- **採用: アプリが「現在ページを返す関数 (provider)」を起動時に一度登録する** (オーナー提案)。既存の `DialogKeyWindowProvider` / `ResumedActivityProvider` や `ToastStyle.defaultPlacement` と同型の受け口

未登録時の既定 — 3 案から **「iOS / MAUI は既定 provider 内蔵、Android Native は登録制 (未登録なら可視領域)」** を採用。却下: 全形態登録必須 (iOS / MAUI 利用者にも登録を強いる)、Android にも既定 (NavHostFragment / Activity content — Compose で当たらず content はボトムナビを含む)

## 決定事項

- 基準領域に 3 つ目の値「表示中のページ (currentPage)」を足す。器は登録された現在ページ provider から矩形を得て、窓座標の inset に変換して既存の計算に流す
- provider の戻りの原始型は窓座標の矩形。各形態に View を返す糖衣を用意する (Compose は測った矩形)
- 効く軸は水平・垂直の両方 (原典の垂直のみは踏襲しない)
- 実装面は iOS Native / Android Native / MAUI の 3 面。KMP は commonMain を触らない
- 既定 provider: iOS / MAUI は内蔵、Android Native は登録制。未解決時は可視領域へ落とす
- 登録口は形態ごとに 1 つ (2026-09-25): Compose = `Modifier` (付けた composable の `boundsInWindow()` を自動登録。provider は書かない)、Android View = provider (ページの `View` を返す。ライブラリが `getLocationInWindow` で矩形へ)、iOS / MAUI = 既定 provider 内蔵 (任意で provider 上書き。iOS は `UIView` / `UIViewController` を返す糖衣)。「矩形を返す provider」は内部の共通型で、利用者向けには公開しない
- Compose の Modifier の規則: attach 中のものを台帳に持つ / 窓と重ならない矩形は候補から外す / 候補が入れ子なら内側 (小さい方) が勝つ / それ以外は最後に配置されたものが勝つ / detach で台帳から外し残りへ戻る / 台帳が空なら可視領域。既定の案内は「Scaffold の content 枠 (NavHost の外側) に 1 回付ける」。付けていないページでは可視領域と同じ結果になる。付けたページを残したまま別の composable を上に重ねる構成では残った矩形が使われ続ける (ドキュメントで content 枠 1 箇所を案内)
- 基準になる矩形は「ページの矩形のうち、そのページ自身の safe area の内側」(2026-09-26)。UIKit 標準ではページ view がバーの下まで伸びるため、view の矩形ではなく `safeAreaLayoutGuide.layoutFrame` を窓座標へ変換する (MAUI のページ view はバーの内側で終わり safe area の bottom が 0 なので結果は同じ)。Android はページ view の矩形 ∩ 可視領域 (edge-to-edge でもシステムバーを含まない)。全画面のページなら可視領域と一致する
- iOS の登録口 (2026-09-26): 既定 provider = key window の VC 階層を presented → navigation の top → tab の selected と先端まで走査した VC の view (SwiftUI の NavigationStack / TabView も内部は UIKit コンテナなので先端の UIHostingController に届く)。UIKit の上書きは `UIView?` を返す provider 1 種類 (VC を持つ人は `vc.view`)。SwiftUI は `.ksDialogCurrentPage()` view modifier を初版に含める (Compose と同じ台帳規則・実装共有。既定 provider が届かない構成の逃げ道)
- MAUI の既定 provider (2026-09-26): MAUI 層で MAUI のページ木を辿り、先端ページの `Handler.PlatformView` を Native の「View を返す provider」へ流す (Android Native に既定が無いため MAUI 層で持つ。両 OS 同じ 1 本)。辿り方: `ResolveMauiContext()` のホスト `Window` から、ModalStack の先頭があればそれ、無ければ `Window.Page` を起点に、`Shell → CurrentPage` / `FlyoutPage → Detail` / `TabbedPage → CurrentPage` / `NavigationPage → CurrentPage` を容れ物でなくなるまで再帰。Shell の有無を問わない。`Handler` が無ければ未解決 → 可視領域。利用者の上書きは `Page` / `VisualElement` を返す provider で、同じ経路へ流す。原典 (`GetActivePage`) との差はモーダル優先と Shell 対応の 2 点
- ADR-0030・0032 の「器はページ構造を知らない」は Dialog の基準領域だけ例外 (器は探さず教えてもらう)。Toast の自動検知却下は据え置き

## ADR 候補 (作成済み: core/ADR-0038 (proposed) / 未起票: ADR-0008 Decision 3 への注記は 0038 の Consequences で扱う)

## 未決の論点

### 現状の把握 (2026-09-25 探索で確認済み)

原典 (`../AiForms.Maui.Dialogs/`) と KsDialogs の実物を読んで確認した事実。

- 原典の `UseCurrentPageLocation=true` の基準は**表示中ページの PlatformView の矩形** (iOS: `Native/iOS/DialogHelpers.cs` の `GetCurrentPageRect` が Handler の PlatformView.Bounds を ContainerView 座標へ変換。Android: `Native/Android/DialogHelpers.cs` の `CalcWindowPadding` が PlatformView の `GetGlobalVisibleRect` から上下 padding を算出)。ステータスバー・ナビゲーションバー・タブバーの内側で終わる。知らせの記述は正しい
- 効くのは両 OS とも**垂直だけ**。水平は常にウィンドウ基準
- 表示中ページの取得は `Application.Current.MainPage.GetActivePage()` (`PageExtensions.cs`)。FlyoutPage → Detail / TabbedPage → CurrentPage / NavigationPage → CurrentPage を再帰。**Shell とモーダルスタックは扱っていない** (Shell だと Shell 全体の矩形になる)
- core/ADR-0008 の前提: 当時の調査記録 (`kasane/roadmaps/archive/2026-09-04-library-foundation/phases/phase-5-1-layout-spec/artifacts/scout-origin-layout-attributes.md`) は「現在ページ領域・垂直のみ」と正確に書いている。「visibleArea = システムバー除外領域」は同 phase の history.md (2026-08-17) でオーナーが基準を定義し直した結果で、ADR-0008 はこれを「半端実装を正す」乖離としてしか書いておらず、**基準そのものを変えた (タブバー・ナビバーを含むようになった) ことを意図的乖離として明記していない**。ADR-0008 の改訂候補 (accepted なので注記 / 本 change の ADR からの参照で扱う)
- KsDialogs の基準 rect R の計算は 4 形態とも「窓の bounds + 4 辺の insets → R」の OS 非依存の純関数 (iOS `ios/Sources/KsDialogs/Layout/DialogLayoutResolver.swift`、Android `android/ksdialogs-core/.../DialogLayoutResolver.kt`)。insets の取得は iOS が `containerView.safeAreaInsets`、Android が `rootWindowInsets` の systemBars (差し替え可能な関数引数)。**3 つ目の基準は「窓から見た 4 辺の inset」に変換して渡せば計算は変えずに済む**
- 足りないもの: (a) 器が呼び出し側の View / ページへの参照を受け取る経路 (iOS の show は viewModel / placement / factory のみ、Android は resumed Activity のみ、MAUI は `Window.Handler.MauiContext` まで)、(b) ページの矩形 → 窓座標の inset へ変換する処理。Android の器は Activity とは別ウィンドウ (`android.app.Dialog`) なので座標変換が要る
- MAUI 層は `ResolveMauiContext()` (`maui/KsDialogs.Maui/Platforms/*/PlatformDialogContent.cs`) で MAUI の `Window` を得ている。`Window.Page` → Shell.CurrentPage / NavigationStack / ModalStack と辿れば表示中ページに届く見込み (未実装)。View → 窓座標の変換は `Contract/DialogTransition.cs` (スライド演出の計測) に前例がある
- 公開 enum は iOS `DialogLayoutArea`、Android `DialogLayoutArea`、MAUI `DialogLayoutArea` + bridge 2 つ + iOS binding の最低 5 箇所に波及。**KMP は commonMain / iosMain / androidMain のどこにも layoutArea を持たない** (ホスト側で登録した Native View に添付する形でだけ効く)
- 共通ケース表 `core/layout-spec/cases.json` は `screen` + `insets` + `layoutArea: "window" | "visibleArea"` で、任意の矩形は表せない (スキーマ拡張が要る)。MAUI / KMP にはケース表テストがない
- Dialog には Toast の `ToastStyle.defaultPlacement` にあたる「アプリ既定」の受け口がない (Loading にはアプリ既定の options がある)

### 論点

- **A. 基準の与え方**: 決着 (決定事項・ADR-0038)
- **B. 効く軸**: 決着 — 水平・垂直の両軸に効かせる (オーナー確認 2026-09-25。Flyout の Detail など横幅が狭まるケースは原典と挙動が変わることを承知の上)
- **C. どの形態に持たせるか**: 決着 — 契約は core (全形態)。実装は iOS Native / Android Native / MAUI の 3 面。KMP は commonMain を触らず (`DialogPlacement` は配置だけを持ち、器のメタ属性はホスト側の Native 添付で効く現状を維持)、Native 側の対応で効かせる (オーナー確認 2026-09-25)
- **D. ADR-0008 の改訂**: 決着 — accepted の本文は触らず、ADR-0038 の Consequences で「基準の再定義」を記録し Decision 3 への注記とする
- **E. 移行スキルの対応表**: 決着 — 暫定注記は入れない。実装後に docs-refresh で `true → CurrentPage` (provider の説明つき) へ一括で直す (オーナー確認 2026-09-25)。ColorAnalyzer への返事は outbox の知らせで返す (蒸留時)
- `fix-layout-contract-gaps` (layout 契約の既存の穴 3 件) とは性質が違うため別の change とした。同じ `layout-semantics.md` に触れるが、本 change は「基準領域」節への追記が中心で穴 3 件 (非有限値・添付の時点・添付競合) とは節が重ならない。本 change を先に進め、gaps 側は着手時に本 change の差分を取り込む
- propose / 実装に送る細目 (探索では決めない): provider・modifier の公開名と置き場所 (iOS / Android / MAUI)、Android の Activity ウィンドウ → Dialog ウィンドウの座標変換 (原点差。マルチウィンドウ・cutout のとき。`DialogTransition.cs` の前例)、`cases.json` のスキーマ拡張 (任意の矩形を rect で表すか inset で表すか)、未解決時の細部 (provider が例外を投げたとき・矩形が空のとき・窓の外のとき)、表示中に画面遷移してページが変わったときに再配置するか (現契約の再配置トリガーは窓寸法と insets の変化だけ)

## UI 素材 (ui/references/ の一覧と注釈)

## 変更級の推奨: M (オーナー確定 2026-09-25。探索は継続中)

- 触る能力: 1 つ (レイアウト契約の基準領域) だが、core 契約 + iOS Native + Android Native + MAUI (enum・binding・bridge 2 つ・写像) の 3 面に跨る
- 公開 API 変更: あり (enum の値 1 つ + provider の登録口 3 面)。追加のみで既存指定は壊れない
- 可逆性: 値と登録口を足すだけで、既定 (可視領域) は変えない。撤回は API 削除になるが実装は独立
- UI: なし (配置の計算のみ。ケース表で固定)
- テスト: `cases.json` のスキーマ拡張と iOS / Android のケース表テスト追加、MAUI の受け渡しテスト
- L にしない理由: 能力は 1 つで、ロードマップに分けるほどの独立 change は無い
