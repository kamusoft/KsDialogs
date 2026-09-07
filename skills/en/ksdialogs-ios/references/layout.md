# Attach layout to content

The Dialog's size, position, the overlay behind it, and the handling of outside taps are passed by attaching them to the content (the presentable the factory returns). Anything not attached takes its default.

There are two values to attach. `DialogOptions` is the static configuration determined by the nature of that content, and can only be passed by attachment. `DialogPlacement` is where it sits, and can be passed both by attachment and as a `show` argument.

## Attachable attributes

| Attached value | Property | What it decides | Default |
|---|---|---|---|
| `DialogOptions` | `layoutArea`: `DialogLayoutArea` (`.window` / `.visibleArea`) | The reference area used to compute size and position | `.visibleArea` |
| | `dialogMargin`: `DialogEdgeInsets` | The margin subtracted from each edge of the reference area | 24 on every edge |
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
- **Margins** — `dialogMargin` clamps only negative edges to 0, and non-finite edges to the default of 24
- **Ratios and `.fill`** — on the same axis a proportional size beats `.fill`, and `.fill` then acts as center alignment
- **Clamping** — the size is trimmed to fit the area left after subtracting `dialogMargin`. Offsets are not trimmed, so content can be pushed off screen deliberately
- **Reference area** — what `.visibleArea` subtracts is the UIKit safe area insets
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
