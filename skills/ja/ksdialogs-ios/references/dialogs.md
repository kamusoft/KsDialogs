# Dialog を表示する

class の ViewModel に結果型を宣言し、表示ごとに新しい content を生成する factory を登録してから `DialogResult` を await する。`Result` の宣言を省略すると結果は `Bool` になり、別の結果型は `typealias Result` で宣言する。

content は SwiftUI の View でも UIKit の `UIView` でも書ける。登録も表示も同じ名前の overload で受け、factory の戻り値の型だけが違う。

以下の例は ViewModel・View・登録・呼び出し元の 4 つに分けている。

## `show` を選ぶ

`Dialog.shared` と、DI で `KsDialog` として注入した実体は同じレジストリと同じ表示状態を指す。`show` の overload を選ぶ軸は 3 つで、ViewModel をインスタンスで渡すか型だけ渡すか、content を登録済みの factory に任せるかその場で渡すか、そしてこの表示だけ配置を上書きするかである。

どの overload も `async throws` で `DialogResult<ViewModel.Result>` を返す。`placement` を渡すと、content に添付された `DialogPlacement` をまるごと置換する ([レイアウト](layout.md))。

| シグネチャ | 何をする | いつ選ぶ | 必要な登録 |
|---|---|---|---|
| `show(_ viewModel: ViewModel)` | 作った ViewModel インスタンスを渡し、登録済みの View factory が content を作る | 呼び出し元で ViewModel を組み立てる (init に値を渡す) とき | View factory |
| `show(_ viewModel: ViewModel, placement: DialogPlacement?)` | 上に配置の上書きを足した形 | この表示だけ配置を変えるとき | View factory |
| `show(_ viewModel: ViewModel, factory: (ViewModel, DialogNotifier<Result>) -> some View)` | ViewModel と SwiftUI content の factory を同時に渡す。レジストリは読まず、変えもしない | 1 回だけ使う SwiftUI content | 不要 |
| `show(_ viewModel: ViewModel, factory: (ViewModel, DialogNotifier<Result>) -> UIView)` | 同じ形の UIKit 版。factory の戻り値の型でこの overload が選ばれる | 1 回だけ使う UIKit content | 不要 |
| `show(_ viewModel:placement:factory:)` (SwiftUI / UIKit の 2 overload) | 上の 2 つに配置の上書きを足した形 | 同上で配置も変えるとき | 不要 |
| `show(_ viewModelType: ViewModel.Type)` | ViewModel の型だけを渡し、登録済みの ViewModel factory が作ったインスタンスを表示する | ViewModel の生成も登録側に任せるとき | View factory + ViewModel factory ([ViewModel](view-models.md)) |
| `show(_ viewModelType: ViewModel.Type, configure: (ViewModel) async throws -> Void)` | 型を渡したうえで、生成したインスタンスを `configure` で整えてから表示する | 表示前に値を入れるとき。`configure` の完了を待ってから content を作る | 同上 |
| `show(_ viewModelType:placement:)` / `show(_ viewModelType:placement:configure:)` | 型渡しに配置の上書きを足した形 | 同上で配置も変えるとき | 同上 |

表では省いているが、登録した factory もインライン factory も `throws` にできる。factory が投げた失敗はそのまま `show` の失敗になり、Dialog は提示されない (Loading も同じで、Toast だけは呼び出し元へ返らない)。

以下の例では、「登録して呼び出す」の `show(ConfirmViewModel(message:))` が 1 行目、「`Bool` 以外の結果型を返す」の `show(ItemEditViewModel.self) { ... }` が 7 行目、「登録せずに content を表示する」の factory 直渡しが 3 行目に当たる。残る行は次の「各 overload の最小例」に挙げる。型を渡す show で必要な ViewModel factory の登録は [ViewModel](view-models.md) にもある。

## 各 overload の最小例

以下の節のサンプルに現れない形を 1 例ずつ挙げる。`ConfirmViewModel` / `ItemEditViewModel` と呼び出し元の `dialogs` プロパティは、いずれも以下の節で定義しているものである。

インスタンスを渡し、この表示だけ配置を上書きする形。

```swift
let result = try await dialogs.show(
    ConfirmViewModel(message: "Delete this item?"),
    placement: DialogPlacement(verticalAlignment: .start)
)
```

インライン factory が `UIView` を返す UIKit 版。

```swift
let result = try await dialogs.show(ConfirmViewModel(message: "Delete this item?")) { viewModel, notifier in
    UIButton(primaryAction: UIAction(title: viewModel.message) { _ in notifier.complete(true) })
}
```

インライン factory に配置の上書きを足した形 (SwiftUI content)。

```swift
let result = try await dialogs.show(
    ConfirmViewModel(message: "Delete this item?"),
    placement: DialogPlacement(verticalAlignment: .end)
) { viewModel, notifier in
    Button(viewModel.message) { notifier.complete(true) }
}
```

同じ形で factory が `UIView` を返す UIKit 版。

```swift
let result = try await dialogs.show(
    ConfirmViewModel(message: "Delete this item?"),
    placement: DialogPlacement(verticalAlignment: .end)
) { viewModel, notifier in
    UIButton(primaryAction: UIAction(title: viewModel.message) { _ in notifier.complete(true) })
}
```

ViewModel の型だけを渡し、`configure` も配置も省略する形。

```swift
let result = try await dialogs.show(ItemEditViewModel.self)
```

型渡しに配置の上書きだけを足した形 (`configure` なし)。

```swift
let result = try await dialogs.show(
    ItemEditViewModel.self,
    placement: DialogPlacement(verticalAlignment: .start)
)
```

型渡しに配置の上書きと `configure` の両方を渡す形。

```swift
let result = try await dialogs.show(
    ItemEditViewModel.self,
    placement: DialogPlacement(verticalAlignment: .start)
) { viewModel in
    viewModel.name = "Apple"
}
```

## ViewModel を宣言する

ViewModel は結果型と、content が読む値を持つ。`Result` を宣言していないので、この ViewModel の結果は `Bool` になる。

```swift
import KsDialogs

final class ConfirmViewModel: DialogViewModel {
    let message: String

    init(message: String) {
        self.message = message
    }
}
```

## SwiftUI で content を書く

覆いと配置はライブラリの器が受け持つため、View にはカード自体だけを書く。結果は factory から渡される `DialogNotifier` へ報告する。最初の報告だけが結果を確定させ、以後の報告は何もしない。結果が呼び出し元へ届くのは、退出の演出と器の撤去が終わったあとである。

```swift
import SwiftUI
import KsDialogs

struct ConfirmView: View {
    let viewModel: ConfirmViewModel
    let notifier: DialogNotifier<Bool>

    var body: some View {
        VStack(spacing: 16) {
            Text(viewModel.message)
                .multilineTextAlignment(.center)

            HStack(spacing: 10) {
                Button("Cancel") { notifier.cancel() }
                    .frame(maxWidth: .infinity, minHeight: 44)
                Button("OK") { notifier.complete(true) }
                    .frame(maxWidth: .infinity, minHeight: 44)
            }
        }
        .padding(20)
        .frame(width: 272)
        .background(.regularMaterial, in: .rect(cornerRadius: 16))
    }
}
```

UIKit で書く場合は、factory が `UIView` を返すようにする。register も show も名前は同じで、レジストリも 1 つである ([レイアウト](layout.md) に `UIView` の content の例がある)。

## 登録して呼び出す

`register` はアプリ起動時に 1 回だけ呼ぶ。SwiftUI アプリでは `App` の `init` が置き場所になる。

```swift
import SwiftUI
import KsDialogs

@main
struct MyApp: App {
    init() {
        Dialog.shared.registry.register(ConfirmViewModel.self) { viewModel, notifier in
            ConfirmView(viewModel: viewModel, notifier: notifier)
        }
    }

    var body: some Scene {
        WindowGroup {
            ItemScreen()
        }
    }
}
```

呼び出し元は `Dialog.shared` を直接使ってもよいし、`KsDialog` として注入したものを使ってもよい。

```swift
import Observation
import SwiftUI
import KsDialogs

@MainActor
@Observable
final class ItemScreenModel {
    private let dialogs: any KsDialog

    private(set) var status = ""

    init(dialogs: any KsDialog = Dialog.shared) {
        self.dialogs = dialogs
    }

    func deleteItem() async {
        do {
            switch try await dialogs.show(ConfirmViewModel(message: "Delete this item?")) {
            case .completed(let accepted):
                status = accepted ? "Deleted" : "Kept"
            case .cancelled:
                status = "Kept"
            }
        } catch {
            status = "Failed: \(error.localizedDescription)"
        }
    }
}

struct ItemScreen: View {
    @State private var model = ItemScreenModel()

    var body: some View {
        VStack(spacing: 16) {
            Text(model.status)
            Button("Delete") {
                Task { await model.deleteItem() }
            }
        }
    }
}
```

`show` はキャンセル用の引数を取らない。`.cancelled` になるのは、notifier のキャンセル報告、外側タップ (既定で有効)、Dialog を載せていた画面の破棄、そして `show` を await している Task のキャンセルである。

## `Bool` 以外の結果型を返す

`typealias Result` には `Sendable` な任意の型を置ける。`notifier` と `DialogResult` の型はその宣言から決まるので、別の型で報告する書き方はコンパイルできない。

次の ViewModel は content から書き換える値を `var` で持ち、結果の報告も `notifier` プロパティから自分で行う。この形では View factory が ViewModel だけを受け取る 1 引数の overload になる ([ViewModel](view-models.md))。

```swift
import Observation
import KsDialogs

struct ItemEdit: Sendable {
    let name: String
    let quantity: Int
}

@MainActor
@Observable
final class ItemEditViewModel: DialogViewModel {
    typealias Result = ItemEdit

    var name = ""
    var quantity = 0

    func save() {
        notifier?.complete(ItemEdit(name: name, quantity: quantity))
    }

    func cancel() {
        notifier?.cancel()
    }
}
```

View factory と ViewModel factory を両方登録すると、型を渡す show も使えるようになる。View は `@Bindable var viewModel: ItemEditViewModel` を受け取り、`save()` / `cancel()` を呼ぶ SwiftUI View である。

```swift
Dialog.shared.registry.register(ItemEditViewModel.self) { viewModel in
    ItemEditView(viewModel: viewModel)
}
Dialog.shared.registry.register(ItemEditViewModel.self, viewModel: {
    ItemEditViewModel()
})
```

```swift
extension ItemScreenModel {
    func editItem() async throws {
        let result = try await dialogs.show(ItemEditViewModel.self) { viewModel in
            viewModel.name = "Apple"
        }
        if case .completed(let edit) = result {
            status = "\(edit.name) x\(edit.quantity)"
        }
    }
}
```

## 登録せずに content を表示する

1 回だけ使う content は factory を `show` へ直接渡す。この経路はレジストリを追加も置換も削除もせず、同じ ViewModel 型の登録済み factory も使わない。SwiftUI と UIKit のどちらの factory も渡せる。

```swift
import SwiftUI
import KsDialogs

final class NoticeViewModel: DialogViewModel {
    let text: String

    init(text: String) {
        self.text = text
    }
}
```

```swift
extension ItemScreenModel {
    func showNotice() async throws {
        let result = try await dialogs.show(NoticeViewModel(text: "Ready")) { viewModel, notifier in
            Button(viewModel.text) { notifier.complete(true) }
                .padding()
        }
        status = result == .completed(true) ? "Read" : "Dismissed"
    }
}
```

## 独立した Dialog を重ねて表示する

並行する show には別々の ViewModel インスタンスを使う。後から出した Dialog が手前になり、通常のユーザー入力はその手前の Dialog だけに届く。

アプリコードが下段の notifier を保持して先に完了させると、iOS は提示の連なり全体を外すため上下とも消える。下段の show は報告された値を返し、上段の show は `.cancelled` を返す。OS が上段の器を外すため、上段の dismissal hook は実行されない。

```swift
extension ItemScreenModel {
    func showTwoDialogs() async throws {
        async let first = dialogs.show(ConfirmViewModel(message: "First"))
        async let second = dialogs.show(ConfirmViewModel(message: "Second"))
        let results = try await (first, second)
        status = "\(results.0) / \(results.1)"
    }
}
```

## 構成ミスの失敗を扱う

構成ミスは `.cancelled` を返さず `DialogError` を throw する。登録漏れをエンドユーザーのキャンセルと取り違えないためであり、この場合 content は生成も表示もされない。

`show` を await している Task のキャンセルはこれとは別で、Dialog は閉じ、`show` は throw せず `.cancelled` を返す。

| `DialogError` の case | メッセージ (`localizedDescription`) | 原因と対処 |
|---|---|---|
| `viewFactoryNotRegistered(viewModelType:)` | `No View factory is registered for ViewModel type {TypeName}.` | その ViewModel 型の content を解決できない。起動時に `register(_:factory:)` を呼ぶ |
| `presentationHostUnavailable` | `No screen is available to present the Dialog.` | 提示できる画面がない。最初の画面が出たあとに show する。キューイングはせず即座に失敗する |
| `viewModelFactoryNotRegistered(viewModelType:)` | `No ViewModel factory is registered for ViewModel type {TypeName}.` | 型を渡す show に ViewModel factory がない。`register(_:viewModel:)` を呼ぶ ([ViewModel](view-models.md)) |
| `viewModelAlreadyShowing(viewModelType:)` | `This ViewModel instance of type {TypeName} is already being shown.` | 同じインスタンスを重ねて show した。重ねる表示ごとに新しいインスタンスを作る |

表のメッセージは現在の実装が返す値であり、安定した API ではない (変わらないのは case と throw される条件であり、文言は予告なく変わりうる)。

残る `viewFactoryTypeMismatch` / `viewModelFactoryTypeMismatch` / `resultTypeMismatch` は型消去をまたぐ受け渡し (KMP の入口・型を渡す show) での不整合で、Swift だけで登録・表示している限り通常は起きない。

たとえば起動時に `register` を呼ばないまま `ConfirmViewModel` を show すると、`ConfirmView` を解決できず `viewFactoryNotRegistered` になる。

```swift
@main
struct MyApp: App {
    var body: some Scene {
        WindowGroup {
            ItemScreen()
        }
    }
}
```

呼び出し元では case ごとに `catch` でき、`viewModelType` から解決できなかった ViewModel の型名を読める。構成ミスは実行時に直せるものではないので、開発中に気づける形にしたうえで、利用者には失敗として見せる。上の `deleteItem` の `catch` を case ごとに分けるとこうなる。

```swift
func deleteItem() async {
    do {
        switch try await dialogs.show(ConfirmViewModel(message: "Delete this item?")) {
        case .completed(let accepted):
            status = accepted ? "Deleted" : "Kept"
        case .cancelled:
            status = "Kept"
        }
    } catch DialogError.viewFactoryNotRegistered(let viewModelType) {
        assertionFailure("Register content for \(viewModelType) during startup")
        status = "Failed"
    } catch {
        status = "Failed"
    }
}
```

case を問わずまとめて扱いたい場合は `catch let error as DialogError` で受ける。
