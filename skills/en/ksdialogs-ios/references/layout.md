# Attach layout to content

The Dialog's size, position, the overlay behind it, and the handling of outside taps are passed by attaching them to the content (the presentable the factory returns). Anything not attached takes its default.

There are two values to attach. `DialogOptions` is the static configuration determined by the nature of that content, and can only be passed by attachment. `DialogPlacement` is where it sits, and can be passed both by attachment and as a `show` argument.

## Attachable attributes

| Attached value | Property | What it decides | Default |
|---|---|---|---|
| `DialogOptions` | `layoutArea`: `DialogLayoutArea` (`.window` / `.visibleArea` / `.currentPage`) | The reference area used to compute size and position | `.visibleArea` |
| | `dialogMargin`: `DialogEdgeInsets` | The margin subtracted from each edge of the reference area | 0 on every edge |
| | `proportionalWidth` / `proportionalHeight`: `Double` | The ratio against the reference area on that axis | `-1` (unspecified) |
| | `overlayColor`: `UIColor` | The color covering the area behind the Dialog | Black at 40% |
| | `isCanceledOnTouchOutside`: `Bool` | Whether an outside tap cancels | `true` |
| `DialogPlacement` | `horizontalAlignment` / `verticalAlignment`: `DialogAlignment` (`.start` / `.center` / `.end` / `.fill`) | The alignment on that axis | `.center` |
| | `offsetX` / `offsetY`: `Double` | The translation applied after alignment | `0` |

`.start` and `.end` in `DialogAlignment` are physical directions (left and right on the horizontal axis, top and bottom on the vertical one) and do not follow the writing direction.

`DialogEdgeInsets` is built with `init(top:left:bottom:right:)` for four explicit edges, `init(all:)` for one value on every edge, or `.zero`. Every argument of `DialogOptions` and `DialogPlacement` has a default, so write only the items you want to change. Numbers are in pt; a positive `offsetX` moves right and a positive `offsetY` moves down.

How you attach depends on the kind of content.

| Content | `DialogOptions` | `DialogPlacement` |
|---|---|---|
| SwiftUI `View` | `.ksDialogOptions(_:)` modifier | `.ksDialogPlacement(_:)` modifier |
| UIKit `UIView` | `ksDialogOptions` property | `ksDialogPlacement` property |

## How the values are settled

- **When they are adopted** — the effective values are the attached values as of the completion of the first native layout pass. Rewriting an attachment after that does not change a Dialog already on screen. In SwiftUI, write the attachment somewhere that is evaluated before the first presentation
- **`show` argument wins** — a `placement` passed to `show` replaces the attached `DialogPlacement` as a whole object. It is not merged field by field, so an attached `offsetY` stops being used the moment a placement is passed. There is no `show` argument corresponding to `DialogOptions`
- **Nesting** — SwiftUI attachments merge from child up to parent, so when the same attribute is attached both inside and outside, the outer one wins
- **Ratios** — values of 0 or less and non-finite values are treated as unspecified, and values above 1 are clamped to 1. The size on an unspecified axis is decided by the content
- **Margins** — `dialogMargin` clamps negative edges and non-finite edges to 0, one edge at a time. With the default of 0, an `.end`-aligned Dialog touches the bottom edge of the reference area, so give it a margin when you want a gap
- **Ratios and `.fill`** — on the same axis a proportional size beats `.fill`, and `.fill` then acts as center alignment
- **Clamping** — the size is trimmed to fit the area left after subtracting `dialogMargin`. Offsets are not trimmed, so content can be pushed off screen deliberately
- **Reference area** — what `.visibleArea` subtracts is the UIKit safe area insets. `.currentPage` uses the page on screen (see "Place relative to the current page" below)
- **Content constraints** — the library applies the computed rect with Auto Layout constraints. The upper bound and the position are required constraints, so the content cannot break them. A size decided by a ratio or `.fill` has priority 999: it beats the content's intrinsic size, but a required (1000) width or height constraint on the content wins. Assigning `frame` directly is overwritten by Auto Layout
- **Re-layout** — on screen rotation, window resizing, and system bars appearing or disappearing, the position is recomputed from the same effective values. It does not move for the software keyboard

## Attach to SwiftUI content

The following puts two modifiers on the content's root to make a full-width card stuck to the bottom of the screen.

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

## Attach to UIKit content

The following attaches during the initialization of the content `UIView` itself. It narrows the width to 90% of the whole window and does not close on an outside tap.

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

A `UIView` with attachments is registered with the same `register` as SwiftUI content.

```swift
Dialog.shared.registry.register(ConfirmViewModel.self) { viewModel, notifier in
    ConfirmPanelView(message: viewModel.message, notifier: notifier)
}
```

## Override placement for one show

The following leaves the content-side attachment as it is and presents the Dialog at the top of the screen for this call alone. `ConfirmViewModel` and `ItemScreenModel` are the ones defined in [Dialog](dialogs.md).

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

## Place relative to the current page

With `layoutArea: .currentPage`, the reference area is the page on screen, minus that page's own bars (navigation bar and tab bar). An `.end`-aligned Dialog then sits above the tab bar, and a proportional size is a ratio of the height without the tab bar. The area used is the intersection of the page view's safe area (`safeAreaLayoutGuide.layoutFrame`) and the visible area.

The library asks three sources for the page, in this order, and moves to the next one when a source has no usable page.

| Order | Source | What you write |
|---|---|---|
| 1 | A SwiftUI view marked as the page | `View.markAsDialogCurrentPage()` |
| 2 | A function you register for UIKit | `DialogCurrentPage.provider` |
| 3 | The built-in lookup | Nothing |

The built-in lookup follows the view controllers of the window that shows the Dialog: it walks the presented chain to its tip, then descends into the `topViewController` of a `UINavigationController` and the `selectedViewController` of a `UITabBarController`. A UIKit app built from those containers needs no extra code. The lookup does not descend into SwiftUI, so when the root is a SwiftUI screen, the result without a marker is almost the same as `.visibleArea` (it does not avoid the tab bar).

When no source yields a page, the Dialog is shown with the same result as `.visibleArea`. The show does not fail, and a warning in English that begins with `The current page could not be resolved, so the visible area is used instead.` is logged.

### Choose the current page as the reference area

The following content is attached to the bottom of the current page with full width. It is used by the next recipe.

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

### Mark SwiftUI pages

In a screen built with `TabView` / `NavigationStack`, attach `markAsDialogCurrentPage()` once to the content of each screen (inside its bars). A marked view is a candidate only while it is on screen, and it wins over the registered function and the built-in lookup. When several marked views are on screen, the inner one wins if they are nested; otherwise the one that came on screen last wins. Marked views that are hidden (the view or an ancestor has `isHidden`, or its `alpha` is below 0.01) are skipped, so the page of the tab being left during a tab switch is not chosen.

`ConfirmViewModel` is the one defined in [Dialog](dialogs.md).

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

### Register the page of a custom UIKit container

When the page is switched by a container of your own that the built-in lookup cannot reach, register once a function that returns the view of the page on screen. The following container keeps one child view controller on screen and exposes its view.

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

Register the function where the container is created. `OrdersViewController` and `SettingsViewController` stand for the app's own view controllers.

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

The function runs on the main thread when a Dialog starts showing and again when the window size or the window safe area changes while it is shown. A Dialog keeps using the function that was registered when it started, so a replacement takes effect from the next show. Assign `nil` to go back to the built-in lookup. When the function returns `nil`, throws, or returns a view that is not in the window showing the Dialog, has an empty safe area, or lies outside the window, the built-in lookup is used instead.
