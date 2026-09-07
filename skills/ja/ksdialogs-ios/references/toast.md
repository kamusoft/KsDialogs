# Toast を表示する

Toast は fire-and-forget の通知である。表示はタッチを奪わず、受理された表示ごとに独立した timer で消える。

`Toast.shared` と、DI で `KsToast` として注入した実体は同じレジストリ・同じ一括設定を指す。

## `show` を選ぶ

経路は 4 つで、組み込みのメッセージ Toast を出すか、登録済みの custom content を出すか、content の factory をその場で渡すか、ViewModel の型を渡して生成から登録側に任せるかである。組み込みのメッセージ Toast の経路には content の解決が無いため throw しない。

`duration` はミリ秒で、`placement` を渡すと content に添付された `DialogPlacement` をまるごと置換する ([レイアウト](layout.md))。

**Toast に閉じる操作は無い**。`hide` に相当する API も、表示中のメッセージを差し替える API も持たない。消滅の契機は duration の経過だけである ([Loading](loading.md) との違い)。

| シグネチャ | 何をする | いつ選ぶ | 必要な登録 |
|---|---|---|---|
| `show(message:duration:placement:)` | 組み込みのメッセージ Toast を表示する | 文言だけ伝えるとき | 不要 |
| `show(message:duration:)` / `show(message:)` | 上の省略形 | 配置や表示時間を既定に委ねるとき | 不要 |
| `show(_ viewModel:duration:placement:)` — `throws` | 登録済みの custom content を表示する | 独自の見た目を出すとき | Toast の View factory |
| `show(_ viewModel:duration:)` / `show(_ viewModel:)` | 上の省略形 | 配置や表示時間を既定に委ねるとき | 同上 |
| `show(_ viewModel:duration:placement:factory:)` — SwiftUI / `UIView` の 2 overload | content の factory をその場で渡す。レジストリは読まず、変えもしない | 1 回だけ使う content | 不要 |
| `show(_ viewModel:factory:)` | 上の省略形 (表示時間も配置も既定) | 同上で既定のまま出すとき | 不要 |
| `show(_ viewModelType:duration:placement:configure:)` — `throws` | ViewModel の型だけを渡し、登録済みの ViewModel factory が作ったインスタンスを `configure` してから表示する | ViewModel の生成も登録側に任せるとき | Toast の View factory + ViewModel factory ([ViewModel](view-models.md)) |
| `show(_ viewModelType:)` / `show(_ viewModelType:configure:)` / `show(_ viewModelType:duration:)` / `show(_ viewModelType:duration:configure:)` / `show(_ viewModelType:duration:placement:)` | 上の省略形 | `configure`・表示時間・配置を省くとき | 同上 |

以下の例では、「message Toast を表示する」が 1 行目、「custom content を登録して表示する」が 4 行目 (`show(_ viewModel:duration:)`)、「登録せずに custom content を表示する」が 5 行目の SwiftUI 版、「型を渡して custom content を表示する」が `show(_ viewModelType:duration:configure:)` に当たる。残る行は次の「各 overload の最小例」に挙げる。

## 各 overload の最小例

以下の節のサンプルに現れない形を 1 例ずつ挙げる。`StatusToastViewModel` と呼び出し元の `toasts` プロパティは、いずれも以下の節で定義しているものである。

組み込みのメッセージ Toast に文言と `duration` だけを渡す形 (配置は既定)。

```swift
toasts.show(message: "Saved", duration: 2000)
```

組み込みのメッセージ Toast に文言だけを渡す形 (`duration` も配置も既定)。

```swift
toasts.show(message: "Saved")
```

登録済みの custom content に `duration` と配置の両方を渡す形。

```swift
try toasts.show(
    StatusToastViewModel(message: "Synchronized"),
    duration: 1800,
    placement: DialogPlacement(verticalAlignment: .start)
)
```

登録済みの custom content を ViewModel だけで表示する形 (`duration` も配置も既定)。

```swift
try toasts.show(StatusToastViewModel(message: "Synchronized"))
```

インライン factory に `duration` と配置を渡し、factory が `UIView` を返す UIKit 版。

```swift
try toasts.show(
    StatusToastViewModel(message: "Uploaded"),
    duration: 1200,
    placement: DialogPlacement(verticalAlignment: .start),
    factory: { (viewModel: StatusToastViewModel) -> UIView in
        let label = UILabel()
        label.text = viewModel.message
        return label
    }
)
```

インライン factory を ViewModel と一緒に渡すだけの形 (SwiftUI content、`duration` も配置も既定)。

```swift
try toasts.show(StatusToastViewModel(message: "Uploaded")) { viewModel in
    Text(viewModel.message)
        .padding()
}
```

同じ形で factory が `UIView` を返す UIKit 版。

```swift
try toasts.show(StatusToastViewModel(message: "Uploaded")) { (viewModel: StatusToastViewModel) -> UIView in
    let label = UILabel()
    label.text = viewModel.message
    return label
}
```

ViewModel の型を渡し、`configure`・表示時間・配置をすべて省く形。

```swift
try toasts.show(StatusToastViewModel.self)
```

型を渡し、`configure` で状態を整えてから表示する形 (表示時間も配置も既定)。

```swift
try toasts.show(StatusToastViewModel.self) { viewModel in
    viewModel.message = "Synchronized"
}
```

型渡しに `duration` を足した形 (`configure` は省略)。

```swift
try toasts.show(StatusToastViewModel.self, duration: 1800)
```

型渡しに `duration` と配置を足した形 (`configure` は省略)。

```swift
try toasts.show(
    StatusToastViewModel.self,
    duration: 1800,
    placement: DialogPlacement(verticalAlignment: .start)
)
```

型渡しに `duration`・配置・`configure` をすべて渡す形。

```swift
try toasts.show(
    StatusToastViewModel.self,
    duration: 1800,
    placement: DialogPlacement(verticalAlignment: .start),
    configure: { viewModel in viewModel.message = "Synchronized" }
)
```

## message Toast を表示する

`duration` を省略するか非正値を渡した場合、現在の `Toast.shared.style.defaultDuration` が正ならその値を使い、それも非正なら `ToastStyle.builtinDefaultDuration` へフォールバックする。

`placement` を省略した場合は `Toast.shared.style.defaultPlacement`、それも無ければライブラリ既定 (可視領域の下部中央 + 上方向へ 80 pt) になる。

この経路は throw しないので、View から直接呼べる。

```swift
import SwiftUI
import KsDialogs

struct SaveButton: View {
    var body: some View {
        Button("Save") {
            Toast.shared.show(
                message: "Saved",
                duration: 2000,
                placement: DialogPlacement(verticalAlignment: .end, offsetY: -100)
            )
        }
    }
}
```

## Toast の既定値を設定する

`ToastStyle` は表示の受理時に読まれるため、変更は次の Toast から効く。アプリ起動時に一度設定しておくのが基本形になる。

視覚項目 (`backgroundColor`、`textColor`、`fontSize`、`cornerRadius`) は内蔵 View にだけ効き、`backgroundColor` の既定は `ToastStyle.builtinBackgroundColor`。`defaultDuration` と `defaultPlacement` は custom content の既定値にもなる。

```swift
import SwiftUI
import UIKit
import KsDialogs

@main
struct MyApp: App {
    init() {
        Toast.shared.style = ToastStyle(
            backgroundColor: .darkGray,
            textColor: .white,
            fontSize: 16,
            cornerRadius: 22,
            defaultDuration: ToastStyle.builtinDefaultDuration,
            defaultPlacement: DialogPlacement(verticalAlignment: .end, offsetY: -120)
        )
    }

    var body: some Scene {
        WindowGroup {
            NoticeScreen()
        }
    }
}
```

## custom content を登録して表示する

custom Toast のレジストリは `Toast.shared.registry` (`ToastViewRegistry`) で、Dialog / Loading のものとは独立している。content の ViewModel は class の `ToastViewModel` にする。

custom content には `DialogTransition` を添付できる。内蔵 View にはこの口が無い ([トランジション](transitions.md))。

```swift
import SwiftUI
import KsDialogs

@MainActor
final class StatusToastViewModel: ToastViewModel {
    var message: String

    init(message: String) {
        self.message = message
    }
}

struct StatusToastView: View {
    let viewModel: StatusToastViewModel

    var body: some View {
        Label(viewModel.message, systemImage: "checkmark.circle.fill")
            .padding()
            .background(.regularMaterial)
            .clipShape(.capsule)
            .ksDialogTransition(.slide(from: .bottom))
    }
}
```

登録は既定値の設定と同じ場所 (`App` の `init`) に足す。

```swift
Toast.shared.registry.register(StatusToastViewModel.self) { viewModel in
    StatusToastView(viewModel: viewModel)
}
```

呼び出し元では `Toast.shared` を直接使ってもよいし、`KsToast` として注入したものを使ってもよい。

```swift
import Observation
import KsDialogs

@MainActor
@Observable
final class NoticeScreenModel {
    private let toasts: any KsToast

    init(toasts: any KsToast = Toast.shared) {
        self.toasts = toasts
    }

    func notifySynchronized() throws {
        try toasts.show(StatusToastViewModel(message: "Synchronized"), duration: 1800)
    }
}
```

## 登録せずに custom content を表示する

1 回だけ使う content は factory を `show` へ直接渡す。この経路はレジストリを追加も置換も削除もせず、同じ ViewModel 型の登録済み factory も使わない。SwiftUI と UIKit のどちらの factory も渡せる。

```swift
extension NoticeScreenModel {
    func notifyUploaded() throws {
        try toasts.show(
            StatusToastViewModel(message: "Uploaded"),
            duration: 1200,
            placement: DialogPlacement(verticalAlignment: .start),
            factory: { viewModel in
                Text(viewModel.message)
                    .padding()
                    .background(.thinMaterial)
            }
        )
    }
}
```

## 型を渡して custom content を表示する

ViewModel の型だけを渡す `show` は、登録済みの ViewModel factory が作ったインスタンスを `configure` してから表示する。Dialog / Loading の型渡しと同じ形である ([ViewModel](view-models.md))。

レジストリのエントリは View factory と ViewModel factory の 2 つの slot を持ち、この経路は両方を必要とする。ViewModel factory の登録は content の登録と同じ場所に足す。

```swift
Toast.shared.registry.register(StatusToastViewModel.self, viewModel: {
    StatusToastViewModel(message: "")
})
```

`configure` は同期で、`throws` にはできるが `async` にはできない。`show` が戻り値を持たない同期呼び出しであるためで、その完了を待つ口は無い。

ViewModel factory と `configure` は MainActor で、受理順に実行される。

```swift
extension NoticeScreenModel {
    func notifyFromRegisteredViewModel() throws {
        try toasts.show(StatusToastViewModel.self, duration: 1800) { viewModel in
            viewModel.message = "Synchronized"
        }
    }
}
```

`configure` と ViewModel factory が投げた失敗は `show` の呼び出し元へ返らない。`show` が同期に戻ったあとに実行されるため、警告ログを残してその表示 1 枚だけが破棄される。この経路が呼び出し時点で throw するのは、slot が未登録のときだけである。

解決するのは `show` を呼んだ時点の登録内容なので、表示中に登録し直しても出ている Toast は変わらない。

## 構成ミスの失敗を扱う

Toast で起こりうる構成ミスは content と ViewModel の解決で、登録経路で ViewModel 型が未登録なら `show` の呼び出し時点で同期的に throw し、表示は行われない。型を渡す経路も同じで、ViewModel factory の未登録が View factory の未登録より先に判定される。

受理されたあとの factory の失敗は呼び出し元へ返せないため、その表示 1 枚を破棄するだけになる。型を渡す経路の `configure` の失敗と、型消去をまたぐ受け渡し (KMP の入口) での不整合 (`viewFactoryTypeMismatch` / `viewModelFactoryTypeMismatch`) もこの経路になり、警告ログが出るだけで `show` からは catch できない。型の不整合は Swift だけで登録・表示している限り通常は起きない。組み込みのメッセージ Toast の経路 (`show(message:)`) にはどちらの解決も無いため、失敗しない。

| `DialogError` の case | メッセージ (`localizedDescription`) | 原因と対処 |
|---|---|---|
| `viewFactoryNotRegistered(viewModelType:)` | `No View factory is registered for ViewModel type {TypeName}.` | その ViewModel 型の content を解決できない。起動時に `Toast.shared.registry.register(_:factory:)` を呼ぶ (Dialog / Loading のレジストリとは別物である) |
| `viewModelFactoryNotRegistered(viewModelType:)` | `No ViewModel factory is registered for ViewModel type {TypeName}.` | 型を渡す `show` に ViewModel factory がない。起動時に `Toast.shared.registry.register(_:viewModel:)` を呼ぶ |

表のメッセージは現在の実装が返す値であり、安定した API ではない (変わらないのは case と throw される条件であり、文言は予告なく変わりうる)。

たとえば起動時に既定値だけ設定して `register` を書かないと、`StatusToastViewModel` の content を解決できず `viewFactoryNotRegistered` になる。

```swift
@main
struct MyApp: App {
    init() {
        Toast.shared.style = ToastStyle(defaultDuration: 2000)
    }

    var body: some Scene {
        WindowGroup {
            NoticeScreen()
        }
    }
}
```

呼び出し元ではこの case を `catch` でき、`viewModelType` から解決できなかった ViewModel の型名を読める。Toast は通知でしかないので画面に失敗を出す先が無く、構成ミスは開発中に気づける形にしておく。

```swift
extension NoticeScreenModel {
    func notifySynchronizedSafely() {
        do {
            try notifySynchronized()
        } catch DialogError.viewFactoryNotRegistered(let viewModelType) {
            assertionFailure("Register toast content for \(viewModelType) during startup")
        } catch {
            assertionFailure("Toast failed: \(error.localizedDescription)")
        }
    }
}
```
