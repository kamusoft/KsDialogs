# 出入りの演出を content に添付する

Dialog の出入りの演出は `DialogTransition` にまとめ、content (factory が返す表示物) に添付して渡す。`show` の引数で渡す経路はない。

添付の書き方は content の種類で決まる。

| content | 添付の書き方 |
|---|---|
| SwiftUI の `View` | `.ksDialogTransition(_:)` modifier |
| UIKit の `UIView` | `ksDialogTransition` プロパティ |

## preset を選ぶ

`DialogTransition` の static メソッドが、`presentation` / `dismissal` / `overlayDuration` の 3 つとも埋まった値を返す。

| preset | 署名 | 演出 |
|---|---|---|
| fade | `DialogTransition.fade(duration:easing:)` | 透明度で出入りする |
| slide | `DialogTransition.slide(from:duration:easing:)` | `from` の辺から滑り込み、同じ辺へ滑り出す |
| zoom | `DialogTransition.zoom(duration:easing:)` | 少し縮んだ状態から等倍へ広がり、同じ倍率へ縮んで消える |
| none | `DialogTransition.none()` | content 側は無演出 |

| 引数 | 型 | 既定 | 意味 |
|---|---|---|---|
| `from` | `DialogTransitionEdge` | なし (slide でのみ必須) | 滑り込み・滑り出しの辺 |
| `duration` | `TimeInterval` (秒) | `0.25` | 片道の時間 |
| `easing` | `any UITimingCurveProvider` | `.standard` (加速して減速する曲線) | 時間に対する進み方 |

| `from` に渡す値 | 向き |
|---|---|
| `.top` / `.bottom` | 物理方向のまま変わらない |
| `.leading` / `.trailing` | レイアウト方向に追随する (右から左へ読む環境では左右が入れ替わる) |

## 既定の挙動と細則

- **未添付時** — content と背後の覆いが 0.25 秒でクロスフェードする
- **`none()`** — content だけを即時化する。覆いは既定どおりフェードする
- **並行実行** — content の演出と覆いのフェードは並行に走り、表示・退出・結果の受け取りはいずれも両方の完了を待つ
- **片側だけの添付** — `presentation` と `dismissal` は片方だけ渡してよい。渡さなかった側は器の既定のクロスフェードになり、渡した側は既定と混ざらず完全に置き換わる
- **成立しない `duration`** — 0・負値・NaN・無限大では演出を省いて最終状態へ直ちに飛ぶ
- **失敗の扱い** — hook が投げた失敗は器が吸収し、Dialog の結果には影響しない
- **完了待ち** — 器はタイムアウトを設けず hook の完了を待つため、完了しない hook は Dialog の撤去と結果の受け渡しを止める
- **採用時点** — 添付値が使われるのは初回のネイティブレイアウトパスが完了した時点である。表示中の Dialog で添付を変えても現在の表示には反映されない
- **呼ぶ場所** — preset factory は MainActor 上で呼ぶ。SwiftUI の `body` と `UIView` の初期化、`register` の factory はいずれも MainActor なので通常は意識しなくてよい

## SwiftUI content に添付して show する

以下は content のルートに preset を添付する例である。`ConfirmViewModel` と `ItemScreenModel` は [Dialog](dialogs.md) で定義したものである。

```swift
import SwiftUI
import KsDialogs

struct ConfirmSheetView: View {
    let viewModel: ConfirmViewModel
    let notifier: DialogNotifier<Bool>

    var body: some View {
        VStack(spacing: 16) {
            Text(viewModel.message)
            Button("OK") { notifier.complete(true) }
        }
        .padding(20)
        .background(.regularMaterial, in: .rect(cornerRadius: 16))
        .ksDialogTransition(.slide(from: .bottom, duration: 0.3))
    }
}
```

この content をアプリ起動時に登録する。

```swift
@main
struct MyApp: App {
    init() {
        Dialog.shared.registry.register(ConfirmViewModel.self) { viewModel, notifier in
            ConfirmSheetView(viewModel: viewModel, notifier: notifier)
        }
    }

    var body: some Scene {
        WindowGroup {
            ItemScreen()
        }
    }
}
```

呼び出し元は演出について何も書かない。`show` すると content が下から 0.3 秒で滑り込み、閉じるときは同じ辺へ滑り出す。

```swift
extension ItemScreenModel {
    func confirmDelete() async throws {
        let result = try await dialogs.show(ConfirmViewModel(message: "Delete this item?"))
        status = result == .completed(true) ? "Deleted" : "Kept"
    }
}
```

## UIKit content に添付する

以下は content の `UIView` 自身の初期化時に演出を添付する例である。登録と呼び出しは SwiftUI content のときと変わらない。

```swift
import UIKit
import KsDialogs

final class ConfirmPanelView: UIView {
    init(message: String, notifier: DialogNotifier<Bool>) {
        super.init(frame: .zero)
        backgroundColor = .secondarySystemBackground
        ksDialogTransition = .zoom(duration: 0.2)

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

## custom hook を渡す

`DialogTransition(presentation:dismissal:overlayDuration:)` は 3 引数とも省略できる。hook の型はどちらも `DialogTransition.Hook` = `@MainActor @Sendable (UIView) async throws -> Void` で、レイアウト済みの host `UIView` を受け取り、演出が終わってから戻る。host `UIView` は SwiftUI の content ではそれを包む View になり、背後の覆いは兄弟のレイヤなので hook の対象にならない。

以下は登録の factory で演出を組み立て、content へ渡して添付する例である。入場は下から持ち上げながら現れ、退場は透明度だけで消える。

```swift
@main
struct MyApp: App {
    init() {
        Dialog.shared.registry.register(ConfirmViewModel.self) { viewModel, notifier in
            ConfirmSheetView(
                viewModel: viewModel,
                notifier: notifier,
                transition: DialogTransition(
                    presentation: { hostView in
                        hostView.alpha = 0
                        hostView.transform = CGAffineTransform(translationX: 0, y: 24)
                        await withCheckedContinuation { continuation in
                            UIView.animate(
                                withDuration: 0.2,
                                animations: {
                                    hostView.alpha = 1
                                    hostView.transform = .identity
                                },
                                completion: { _ in continuation.resume() }
                            )
                        }
                    },
                    dismissal: { hostView in
                        await withCheckedContinuation { continuation in
                            UIView.animate(
                                withDuration: 0.2,
                                animations: { hostView.alpha = 0 },
                                completion: { _ in continuation.resume() }
                            )
                        }
                    },
                    overlayDuration: 0.2
                )
            )
        }
    }

    var body: some Scene {
        WindowGroup {
            ItemScreen()
        }
    }
}
```

content 側は受け取った演出をそのまま添付する。

```swift
struct ConfirmSheetView: View {
    let viewModel: ConfirmViewModel
    let notifier: DialogNotifier<Bool>
    let transition: DialogTransition

    var body: some View {
        VStack(spacing: 16) {
            Text(viewModel.message)
            Button("OK") { notifier.complete(true) }
        }
        .padding(20)
        .background(.regularMaterial, in: .rect(cornerRadius: 16))
        .ksDialogTransition(transition)
    }
}
```

これで `show(ConfirmViewModel(message:))` の表示時に `presentation` が、閉じるときに `dismissal` が呼ばれ、覆いは 0.2 秒でフェードする。呼び出し元は preset のときから変わらない。

## preset と custom hook を組み合わせる

preset が返した値から片側の hook だけを取り出し、自作の hook と一緒に渡せる。以下は入場を zoom preset のまま、退場だけ自作にする例である。

```swift
Dialog.shared.registry.register(ConfirmViewModel.self) { viewModel, notifier in
    let zoomIn = DialogTransition.zoom(duration: 0.2)
    return ConfirmSheetView(
        viewModel: viewModel,
        notifier: notifier,
        transition: DialogTransition(
            presentation: zoomIn.presentation,
            dismissal: { hostView in
                await withCheckedContinuation { continuation in
                    UIView.animate(
                        withDuration: 0.14,
                        animations: { hostView.alpha = 0 },
                        completion: { _ in continuation.resume() }
                    )
                }
            },
            overlayDuration: zoomIn.overlayDuration
        )
    )
}
```

覆いの時間だけを preset に揃えたい場合は `zoomIn.overlayDuration` を、閉鎖だけを preset にしたい場合は `zoomIn.dismissal` を同じように渡す。

## 退出を待ってから次へ進む

await した結果は退出後の値として扱う。最初の報告で結果が確定するが、`show` が戻るのは dismissal hook・覆いのフェード・器の撤去がすべて完了した後である。以下は Dialog が画面から消えてから次の処理へ進む例である。

```swift
extension ItemScreenModel {
    func confirmThenRefresh() async throws {
        let result = try await dialogs.show(ConfirmViewModel(message: "Refresh now?"))
        if case .completed(true) = result {
            await reloadItems()
        }
    }

    func reloadItems() async {}
}
```
