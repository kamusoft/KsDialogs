# content にレイアウトを添付する

Dialog の大きさ・位置・背後の覆い・外側タップの扱いは、content (factory が返す表示物) に添付して渡す。添付しなかった項目は既定値になる。

添付する値は 2 つある。`DialogOptions` はその content の性質として決まる静的な設定で、添付でしか渡せない。`DialogPlacement` は置き場所で、添付に加えて `show` の引数でも渡せる。

## 添付できる属性

| 添付する値 | プロパティ | 決めるもの | 既定値 |
|---|---|---|---|
| `DialogOptions` | `layoutArea`: `DialogLayoutArea` (`.window` / `.visibleArea`) | サイズと位置を計算する基準領域 | `.visibleArea` |
| | `dialogMargin`: `DialogEdgeInsets` | 基準領域の各辺から控除する余白 | 全辺 24 |
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
- **余白** — `dialogMargin` は負の辺だけ 0 に、非有限値の辺は既定の 24 に丸める
- **比率と `.fill`** — 同じ軸では比率サイズが `.fill` に勝ち、そのとき `.fill` は中央配置として働く
- **クランプ** — サイズは `dialogMargin` を控除した領域に収まるよう切り詰める。offset は切り詰めないので、意図的に画面外へ押し出せる
- **基準領域** — `.visibleArea` が控除するのは UIKit の safe area insets である
- **再配置** — 画面の回転・ウィンドウのリサイズ・システムバーの出入りでは同じ実効値のまま位置を計算し直す。ソフトキーボードでは動かない

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
