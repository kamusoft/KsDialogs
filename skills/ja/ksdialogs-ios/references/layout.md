# content にレイアウトを添付する

Dialog の大きさ・位置・背後の覆い・外側タップの扱いは、content (factory が返す表示物) に添付して渡す。添付しなかった項目は既定値になる。

添付する値は 2 つある。`DialogOptions` はその content の性質として決まる静的な設定で、添付でしか渡せない。`DialogPlacement` は置き場所で、添付に加えて `show` の引数でも渡せる。

## 添付できる属性

| 添付する値 | プロパティ | 決めるもの | 既定値 |
|---|---|---|---|
| `DialogOptions` | `layoutArea`: `DialogLayoutArea` (`.window` / `.visibleArea` / `.currentPage`) | サイズと位置を計算する基準領域 | `.visibleArea` |
| | `dialogMargin`: `DialogEdgeInsets` | 基準領域の各辺から控除する余白 | 全辺 0 |
| | `proportionalWidth` / `proportionalHeight`: `Double` | その軸の基準領域に対する比率 | `-1` (未指定) |
| | `overlayColor`: `UIColor` | Dialog の背後を覆う色 | 黒 40% |
| | `isCanceledOnTouchOutside`: `Bool` | 外側タップでキャンセルするか | `true` |
| `DialogPlacement` | `horizontalAlignment` / `verticalAlignment`: `DialogAlignment` (`.start` / `.center` / `.end` / `.fill`) | その軸の配置 | `.center` |
| | `offsetX` / `offsetY`: `Double` | 配置を決めた後の平行移動 | `0` |

`DialogAlignment` の `.start` と `.end` は物理方向 (水平軸なら左と右、垂直軸なら上と下) で、書字方向には追随しない。

`DialogEdgeInsets` は 4 辺を明示する `init(top:left:bottom:right:)`、全辺同値の `init(all:)`、`.zero` で作る。`DialogOptions` と `DialogPlacement` は全引数に既定値があるので、変えたい項目だけを書けばよい。数値の単位は pt で、正の `offsetX` は右、正の `offsetY` は下へ動かす。

添付の書き方は content の種類で決まる。

| content | `DialogOptions` | `DialogPlacement` |
|---|---|---|
| SwiftUI の `View` | `.ksDialogOptions(_:)` modifier | `.ksDialogPlacement(_:)` modifier |
| UIKit の `UIView` | `ksDialogOptions` プロパティ | `ksDialogPlacement` プロパティ |

## 値の決まり方

- **採用時点** — 実効値は初回のネイティブレイアウトパスが完了した時点の添付値である。それより後に添付を書き換えても表示中の Dialog は変わらない。SwiftUI では初回表示までに評価される場所へ添付を書く
- **`show` 引数の優先** — `show` に渡した `placement` は、添付済みの `DialogPlacement` をオブジェクトまるごと置換する。フィールド単位では合成しないため、添付した `offsetY` は placement を渡した時点で使われなくなる。`DialogOptions` に対応する `show` 引数はない
- **入れ子** — SwiftUI の添付は子から親へ遡って合流するので、内側と外側の両方に同じ属性を添付した場合は外側が勝つ
- **比率** — 0 以下と非有限値は未指定として扱い、1 を超える値は 1 に丸める。未指定の軸のサイズは content が決める
- **余白** — `dialogMargin` は負の辺と非有限値の辺を、辺ごとに 0 に丸める。既定の 0 のままだと `.end` で寄せた Dialog は基準領域の下端に接するので、隙間を空けたいときは余白を渡す
- **比率と `.fill`** — 同じ軸では比率サイズが `.fill` に勝ち、そのとき `.fill` は中央配置として働く
- **クランプ** — サイズは `dialogMargin` を控除した領域に収まるよう切り詰める。offset は切り詰めないので、意図的に画面外へ押し出せる
- **基準領域** — `.visibleArea` が控除するのは UIKit の safe area insets である。`.currentPage` は画面に出ているページを基準にする (後述の「表示中のページを基準に配置する」)
- **content 側の制約** — 計算した rect は Auto Layout の制約で反映する。上限と位置の制約は必須なので、content の側で何をしても守られる。比率・`.fill` で決めたサイズの制約は優先度 999 で、content の固有サイズには勝つが、content が必須 (1000) の幅・高さ制約を持つとそちらが勝つ。`frame` を直接代入しても Auto Layout に上書きされる
- **再配置** — 画面の回転・ウィンドウのリサイズ・システムバーの出入りでは同じ実効値のまま位置を計算し直す。ソフトキーボードでは動かない
- **content に委ねた軸の追従** — 比率も `.fill` も指定しない軸 (大きさを content に委ねた軸) は、表示中に `dialogMargin` を控除した領域が広がると content の理想の大きさまで広がり、狭まると新しい領域で頭打ちになる。SwiftUI と `UIView` のどちらの content でも同じである。例: 縦長で幅 393 に頭打ちになっていた理想の幅 600 の content は、横長にすると 600 になり、縦長に戻すと 393 に戻る

## SwiftUI content に添付する

以下は content のルートに 2 つの modifier を付けて、画面下端に張り付く幅いっぱいのカードにする例である。

```swift
import SwiftUI
import UIKit
import KsDialogs

struct BottomSheetConfirmView: View {
    let viewModel: ConfirmViewModel
    let notifier: DialogNotifier<Bool>

    var body: some View {
        VStack(spacing: 16) {
            Text(viewModel.message)
            Button("OK") { notifier.complete(true) }
        }
        .padding(20)
        .background(.regularMaterial, in: .rect(cornerRadius: 16))
        .ksDialogOptions(
            DialogOptions(
                layoutArea: .visibleArea,
                dialogMargin: DialogEdgeInsets(all: 16),
                proportionalWidth: 1,
                overlayColor: UIColor.black.withAlphaComponent(0.4)
            )
        )
        .ksDialogPlacement(
            DialogPlacement(horizontalAlignment: .fill, verticalAlignment: .end, offsetY: -12)
        )
    }
}
```

## UIKit content に添付する

以下は content の `UIView` 自身の初期化時に添付する例である。ウィンドウ全体を基準に幅を 9 割へ絞り、外側タップでは閉じないようにしている。

```swift
import UIKit
import KsDialogs

final class ConfirmPanelView: UIView {
    init(message: String, notifier: DialogNotifier<Bool>) {
        super.init(frame: .zero)
        backgroundColor = .secondarySystemBackground
        ksDialogOptions = DialogOptions(
            layoutArea: .window,
            dialogMargin: DialogEdgeInsets(top: 24, left: 20, bottom: 32, right: 20),
            proportionalWidth: 0.9,
            overlayColor: UIColor.black.withAlphaComponent(0.3),
            isCanceledOnTouchOutside: false
        )
        ksDialogPlacement = DialogPlacement(verticalAlignment: .end)

        let button = UIButton(primaryAction: UIAction(title: message) { _ in notifier.complete(true) })
        button.translatesAutoresizingMaskIntoConstraints = false
        addSubview(button)
        NSLayoutConstraint.activate([
            button.topAnchor.constraint(equalTo: topAnchor, constant: 20),
            button.bottomAnchor.constraint(equalTo: bottomAnchor, constant: -20),
            button.leadingAnchor.constraint(equalTo: leadingAnchor, constant: 20),
            button.trailingAnchor.constraint(equalTo: trailingAnchor, constant: -20)
        ])
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) is not supported")
    }
}
```

添付した `UIView` は、SwiftUI の content と同じ `register` で登録する。

```swift
Dialog.shared.registry.register(ConfirmViewModel.self) { viewModel, notifier in
    ConfirmPanelView(message: viewModel.message, notifier: notifier)
}
```

## 1 回の show だけ配置を上書きする

以下は content 側の添付はそのままに、この呼び出しだけ Dialog を画面上部へ出す例である。`ConfirmViewModel` と `ItemScreenModel` は [Dialog](dialogs.md) で定義したものである。

```swift
extension ItemScreenModel {
    func confirmDeleteAtTop() async throws {
        let result = try await dialogs.show(
            ConfirmViewModel(message: "Delete this item?"),
            placement: DialogPlacement(verticalAlignment: .start, offsetY: 20)
        )
        status = result == .completed(true) ? "Deleted" : "Kept"
    }
}
```

## 表示中のページを基準に配置する

`layoutArea: .currentPage` を選ぶと、基準領域は画面に出ているページから、そのページ自身のバー (ナビゲーションバー・タブバー) を除いた内側になる。`.end` で寄せた Dialog はタブバーの上に出て、比率サイズもタブバーを除いた高さに対する割合になる。使われるのは、ページの View の safe area の内側 (`safeAreaLayoutGuide.layoutFrame`) と可視領域の共通部分である。

ライブラリはページを次の 3 つの取得元に上から順に問い合わせ、使えるページを持たない取得元は飛ばして次へ進む。

| 順 | 取得元 | 書くもの |
|---|---|---|
| 1 | ページとして印を付けた SwiftUI の View | `View.markAsDialogCurrentPage()` |
| 2 | UIKit 向けに登録する関数 | `DialogCurrentPage.provider` |
| 3 | 内蔵の既定の探し方 | なし |

既定の探し方は、Dialog を出す window の view controller を辿る。present の連なりを先端まで進み、そこから `UINavigationController` の `topViewController` と `UITabBarController` の `selectedViewController` へ降りる。これらのコンテナで組んだ UIKit のアプリは、何も書かなくてよい。SwiftUI の内側までは降りないので、root が SwiftUI の画面で印を付けていないと、結果は `.visibleArea` とほぼ同じになる (タブバーを避けない)。

どの取得元からもページが得られないときは、`.visibleArea` と同じ結果で表示する。`show` は失敗せず、`The current page could not be resolved, so the visible area is used instead.` で始まる英語の警告ログが出る。

### 基準領域に表示中のページを選ぶ

以下は表示中のページの下端に幅いっぱいで出す content で、次のレシピで使う。

```swift
import SwiftUI
import KsDialogs

struct PageBottomConfirmView: View {
    let viewModel: ConfirmViewModel
    let notifier: DialogNotifier<Bool>

    var body: some View {
        Button(viewModel.message) { notifier.complete(true) }
            .padding(20)
            .frame(maxWidth: .infinity)
            .background(.regularMaterial, in: .rect(cornerRadius: 16))
            .ksDialogOptions(
                DialogOptions(
                    layoutArea: .currentPage,
                    dialogMargin: DialogEdgeInsets(all: 16),
                    proportionalWidth: 1
                )
            )
            .ksDialogPlacement(DialogPlacement(verticalAlignment: .end))
    }
}
```

### SwiftUI のページに印を付ける

`TabView` / `NavigationStack` で組んだ画面では、各画面の中身 (バーの内側) に `markAsDialogCurrentPage()` を 1 回付ける。印を付けた View は画面に載っている間だけ候補になり、登録した関数と既定の探し方より優先される。画面に印が複数あるときは、入れ子なら内側が、それ以外は最後に画面に載ったものが選ばれる。非表示の印 (自身か祖先が `isHidden`、または `alpha` が 0.01 未満) は外すので、タブの切り替え中に去っていくタブの画面は選ばれない。

`ConfirmViewModel` は [Dialog](dialogs.md) で定義したものである。

```swift
import SwiftUI
import KsDialogs

struct MainTabs: View {
    var body: some View {
        TabView {
            NavigationStack {
                OrdersScreen()
                    .markAsDialogCurrentPage()
                    .navigationTitle("Orders")
            }
            .tabItem { Label("Orders", systemImage: "list.bullet") }

            SettingsScreen()
                .markAsDialogCurrentPage()
                .tabItem { Label("Settings", systemImage: "gearshape") }
        }
    }
}

struct OrdersScreen: View {
    @State private var status = ""

    var body: some View {
        VStack(spacing: 16) {
            Text(status)
            Button("Archive") {
                Task {
                    let result = try await Dialog.shared.show(
                        ConfirmViewModel(message: "Archive this order?")
                    ) { viewModel, notifier in
                        PageBottomConfirmView(viewModel: viewModel, notifier: notifier)
                    }
                    status = result == .completed(true) ? "Archived" : "Kept"
                }
            }
        }
    }
}

struct SettingsScreen: View {
    var body: some View {
        Text("Settings")
    }
}
```

### 独自の UIKit コンテナのページを登録する

既定の探し方が届かない独自のコンテナで画面を切り替えているときは、表示中のページの View を返す関数を一度登録する。以下のコンテナは子の view controller を 1 つだけ画面に出し、その View を公開する。

```swift
import UIKit
import KsDialogs

final class PagerViewController: UIViewController {
    private let pages: [UIViewController]
    private var visiblePage: UIViewController?

    init(pages: [UIViewController]) {
        self.pages = pages
        super.init(nibName: nil, bundle: nil)
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) is not supported")
    }

    var visiblePageView: UIView? {
        visiblePage?.view
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        showPage(at: 0)
    }

    func showPage(at index: Int) {
        let page = pages[index]
        if let visiblePage {
            visiblePage.willMove(toParent: nil)
            visiblePage.view.removeFromSuperview()
            visiblePage.removeFromParent()
        }
        addChild(page)
        page.view.frame = view.bounds
        page.view.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        view.addSubview(page.view)
        page.didMove(toParent: self)
        visiblePage = page
    }
}
```

関数はコンテナを作る場所で登録する。`OrdersViewController` と `SettingsViewController` はアプリ自身の view controller を表す。

```swift
import UIKit
import KsDialogs

final class SceneDelegate: UIResponder, UIWindowSceneDelegate {
    var window: UIWindow?

    func scene(
        _ scene: UIScene,
        willConnectTo session: UISceneSession,
        options connectionOptions: UIScene.ConnectionOptions
    ) {
        guard let windowScene = scene as? UIWindowScene else { return }
        let pager = PagerViewController(pages: [OrdersViewController(), SettingsViewController()])
        DialogCurrentPage.provider = { [weak pager] in
            pager?.visiblePageView
        }

        let window = UIWindow(windowScene: windowScene)
        window.rootViewController = pager
        window.makeKeyAndVisible()
        self.window = window
    }
}
```

関数はメインスレッドで、Dialog の表示の開始時と、表示中に window の寸法や window の safe area が変わったときに呼ばれる。Dialog は表示を始めた時点で登録されていた関数を使い続けるので、差し替えは次の `show` から効く。`nil` を代入すると既定の探し方に戻る。関数が `nil` を返す・エラーを投げる・返した View が Dialog を出す window に載っていない・safe area の内側が空・window の外にある、のいずれかなら、既定の探し方の結果を使う。

## SwiftUI content にシステムの領域を避けさせる

Dialog・Loading・Toast の SwiftUI content は、決まった条件の軸でだけ、器 (content を載せて表示するライブラリのコンテナ) とシステムの領域の重なりを SwiftUI 標準の safe area として受け取る。システムの領域は window の safe area の外側 (ステータスバー・ホームインジケータ等が占める帯) で、重なりは器の矩形がそこへはみ出している幅を辺ごとに測ったものである。safe area を避ける (SwiftUI の既定) か `.ignoresSafeArea(.container)` で無視するかは、SwiftUI の標準の書き方で選ぶ。

| `layoutArea` | その軸の配置 | その軸の辺で受け取る safe area |
|---|---|---|
| `.window` | `.start` / `.end`、または `proportionalWidth` / `proportionalHeight` を伴わない `.fill` | 器とシステムの領域の重なり |
| `.window` | `.center`、または比率を伴う `.fill` | 0 |
| `.visibleArea` (既定) / `.currentPage` | どれでも (offset で器がシステムの領域に重なっても) | 0 |

- 届くのはシステムの領域だけで、キーボードの領域は含まない
- 表示中に window の寸法や window の safe area が変わると、受け取る値も新しい重なりになる
- 例: safe area が上 59・下 34 の縦長の window 393 × 852 で、`.window` と垂直の `.start` を指定した content は上 59・それ以外 0 を受け取る。両軸を `.fill` にすると上 59・下 34 を受け取る

`UIView` で書いた content は UIKit の伝播で、配置や基準領域を問わず重なりを UIKit の safe area として受け取る。

器の大きさは safe area で変わらない。器は content の理想の大きさのままで、端に付けた content は器の内側で safe area を避ける。そのため使える大きさは重なりの分だけ減る。上の window で理想の高さ 100 の content を上端に付けると、器は y 0・高さ 100 のまま、content は y 59 から高さ 41 に置かれ、折り返す長い本文は収まらなくなる。困るときは次のどれかを選ぶ。

| 避け方 | 結果 |
|---|---|
| `layoutArea` を既定の `.visibleArea` のままにする | 器が safe area の内側に置かれ、重なりは 0。content は器いっぱいを使える |
| content の一番外側の View に `.ignoresSafeArea(.container)` を付ける | content は器の矩形いっぱいに置かれる。システムの領域を避けるのは content の責任になる |
| `dialogMargin` で器を window の端から離す | 重なりが離した分だけ減り、受け取る値も減る |

`.window` で片側の端に付けた軸 (`.start` / `.end`。比率の有無を問わない) では、縮められる限り縮めても器の中の safe area の内側に収まらない content (`.frame(width:height:)` で大きさを固定したものや、文言とボタンを並べた縮まない `VStack` など) は、器が付いた端と反対側の辺にそろえて器いっぱいに置かれる。下端に付けたなら上の辺、上端なら下の辺、左端なら右の辺、右端なら左の辺で、右から左に書く言語でも同じ辺になる。そろえた軸では content は safe area を受け取らない。上の window で縮まない高さ 100 の content を下端に付けると、y 752・高さ 100 の器いっぱいに置かれ、ホームインジケータの帯に重なる。システムの領域を避けさせたいときは、端に付ける軸の大きさを縮められるようにしておく。

収まるかどうかは表示中も判定し直される。両軸 `.fill`・`.center` の軸・`.window` 以外の基準領域と、`UIView` の content ではこのそろえは行われず、収まらない SwiftUI content は SwiftUI の標準どおり safe area の内側の中央に置かれる。

### window 全体を覆い、本文は safe area の内側に置く

両軸を `.fill` にした content では、SwiftUI の画面と同じ書き方で、地色だけを window の端まで広げ、本文を safe area の内側に置ける。

```swift
import SwiftUI
import KsDialogs

final class NoticeViewModel: DialogViewModel {
    let message: String

    init(message: String) {
        self.message = message
    }
}

struct FullScreenNoticeView: View {
    let viewModel: NoticeViewModel
    let notifier: DialogNotifier<Bool>

    var body: some View {
        VStack(spacing: 24) {
            Text(viewModel.message)
                .foregroundStyle(.white)
            Button("Close") { notifier.complete(true) }
                .buttonStyle(.borderedProminent)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color.black.ignoresSafeArea())
        .ksDialogOptions(DialogOptions(layoutArea: .window))
        .ksDialogPlacement(DialogPlacement(horizontalAlignment: .fill, verticalAlignment: .fill))
    }
}
```

登録はほかの content と同じである。

```swift
Dialog.shared.registry.register(NoticeViewModel.self) { viewModel, notifier in
    FullScreenNoticeView(viewModel: viewModel, notifier: notifier)
}
```

### `GeometryReader` で重なりを読む

`GeometryReader` から読む `GeometryProxy.safeAreaInsets` は、読む View の位置で決まる。safe area の内側に置かれた、大きさを中身に合わせた `Text` から読むと 0 になる。重なりを読みたいときは、一番外側の `GeometryReader` のように器いっぱいに広がる View から読む。収まらない content を内側の辺へそろえるかを判定し直している途中では、画面に描かれない途中の位置と大きさが 1 回届くことがあるので、最後に届いた値を使う。

```swift
import SwiftUI
import KsDialogs

struct InsetAwareNoticeView: View {
    let viewModel: NoticeViewModel
    let notifier: DialogNotifier<Bool>

    var body: some View {
        GeometryReader { proxy in
            VStack(spacing: 16) {
                Text(viewModel.message)
                Text("Top \(Int(proxy.safeAreaInsets.top)) pt / Bottom \(Int(proxy.safeAreaInsets.bottom)) pt")
                    .font(.footnote)
                Button("Close") { notifier.complete(true) }
            }
            .frame(width: proxy.size.width, height: proxy.size.height)
        }
        .background(Color(uiColor: .systemBackground).ignoresSafeArea())
        .ksDialogOptions(DialogOptions(layoutArea: .window))
        .ksDialogPlacement(DialogPlacement(horizontalAlignment: .fill, verticalAlignment: .fill))
    }
}
```
