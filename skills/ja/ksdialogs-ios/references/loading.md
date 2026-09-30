# Loading を表示する

Loading は処理中の操作をブロックする表示で、Dialog や Toast より手前に出続け、ユーザー操作では閉じられない。

`Loading.shared` と、DI で `KsLoading` として注入した実体は同じ表示状態を指す。並行する利用はプロセス内の 1 表示へ合流し、content は最初の開始のものが維持される。

## `show` / `start` / `hide` を選ぶ

表示の開き方は 2 通りある。`show` で開いた表示を閉じるのは `hide` だけで、`start` は処理の開始と完了に表示の寿命を合わせるので自分で閉じる必要がない。進捗を報告できるのは `start` だけである。

`placement` を渡すと content に添付された `DialogPlacement` をまるごと置換する ([レイアウト](layout.md))。

| シグネチャ | 何をする | いつ選ぶ | 必要な登録 |
|---|---|---|---|
| `show(message:placement:)` — `async` | 内蔵 Loading を表示し、合流を 1 件開始する | メッセージだけ出して自分で閉じるとき | 不要 |
| `show(message:)` / `show()` | 上の省略形 (配置は content の既定に委ねる) | 配置を変えないとき | 不要 |
| `show(_ viewModel:placement:)` — `async throws` | 登録済みの custom content を表示する | 独自の見た目を出すとき | Loading の View factory |
| `show(_ viewModel:)` | 上の省略形 | 配置を変えないとき | 同上 |
| `show(_ viewModel:placement:factory:)` — SwiftUI / `UIView` の 2 overload | content の factory をその場で渡す。レジストリは読まず、変えもしない | 1 回だけ使う content | 不要 |
| `show(_ viewModel:factory:)` | 上の省略形 | 配置を変えないとき | 不要 |
| `show(_ viewModelType:placement:configure:)` — `async throws` | ViewModel の型だけを渡し、登録済みの ViewModel factory が作ったインスタンスを `configure` してから表示する | ViewModel の生成も登録側に任せるとき | View factory + ViewModel factory ([ViewModel](view-models.md)) |
| `show(_ viewModelType:)` / `show(_ viewModelType:configure:)` / `show(_ viewModelType:placement:)` | 上の省略形 | `configure` や配置を省くとき | 同上 |
| `hide()` — `async` | 表示を閉じる。合流数によらず即座に閉じ、走行中の処理には干渉しない | `show` で開いた表示を閉じるとき | — |
| `setMessage(_:)` — `async` | 表示中のメッセージを差し替える | 内蔵 Loading の表示中に文言を変えるとき (非表示中と custom content の表示中は何も起こらない) | — |
| `start(message:placement:_:)` — `async throws` | 内蔵 Loading を出したまま処理を実行し、その戻り値を返す | 表示の寿命を処理に合わせるとき | 不要 |
| `start(message:_:)` / `start(_:)` (処理だけを渡す形) | 上の省略形 | メッセージや配置を既定に委ねるとき | 不要 |
| `start(_ viewModel:placement:_:)` — `async throws` | 登録済みの custom content で同じことをする | 独自の見た目で処理を包むとき | Loading の View factory |
| `start(_ viewModel:_:)` | 上の省略形 | 配置を変えないとき | 同上 |
| `start(_ viewModel:placement:factory:_:)` — SwiftUI / `UIView` の 2 overload | インライン factory で同じことをする | 1 回だけ使う content で処理を包むとき | 不要 |
| `start(_ viewModel:factory:_:)` | 上の省略形 | 配置を変えないとき | 不要 |
| `start(_ viewModelType:placement:configure:_:)` — `async throws` | 型を渡して同じことをする。`configure` の完了後に処理を実行する | 生成も登録側に任せて処理を包むとき | View factory + ViewModel factory ([ViewModel](view-models.md)) |
| `start(_ viewModelType:_:)` / `start(_ viewModelType:configure:_:)` / `start(_ viewModelType:placement:_:)` | 上の省略形 | `configure` や配置を省くとき | 同上 |

以下の例では、「命令形で表示して閉じる」が `show(message:)` と `hide()` と `setMessage(_:)`、「処理の間だけ表示する」が `start(message:_:)`、「custom content を登録して表示する」が `start(_:_:)`、「型を渡して custom content を表示する」が `start(_ viewModelType:configure:_:)` に当たる。残る行は次の「各 overload の最小例」に挙げる。

## 各 overload の最小例

以下の節のサンプルに現れない形を 1 例ずつ挙げる。`ProgressLoadingViewModel` と呼び出し元の `loading` プロパティは、いずれも以下の節で定義しているものである。

内蔵 Loading にメッセージと配置の両方を渡す形。

```swift
await loading.show(message: "Connecting", placement: DialogPlacement(verticalAlignment: .start))
```

内蔵 Loading をメッセージも配置も省略して表示する形。

```swift
await loading.show()
```

登録済みの custom content を、配置を省略して表示する形。

```swift
try await loading.show(ProgressLoadingViewModel())
```

登録済みの custom content に配置の上書きを足した形。

```swift
try await loading.show(ProgressLoadingViewModel(), placement: DialogPlacement(verticalAlignment: .start))
```

インライン factory で SwiftUI content を表示する形 (配置は省略)。

```swift
try await loading.show(ProgressLoadingViewModel()) { viewModel in
    ProgressView(value: viewModel.progress)
}
```

同じ形で factory が `UIView` を返す UIKit 版。

```swift
try await loading.show(ProgressLoadingViewModel()) { _ in
    UIActivityIndicatorView(style: .large)
}
```

インライン factory に配置の上書きを足した形 (SwiftUI content)。

```swift
try await loading.show(
    ProgressLoadingViewModel(),
    placement: DialogPlacement(verticalAlignment: .start),
    factory: { viewModel in ProgressView(value: viewModel.progress) }
)
```

同じ形で factory が `UIView` を返す UIKit 版。

```swift
try await loading.show(
    ProgressLoadingViewModel(),
    placement: DialogPlacement(verticalAlignment: .start),
    factory: { _ in UIActivityIndicatorView(style: .large) }
)
```

内蔵 Loading のスコープ形で、メッセージも配置も省略して処理だけを渡す形。

```swift
let data = try await loading.start { report in
    report(1)
    return Data()
}
```

内蔵 Loading のスコープ形にメッセージと配置の両方を渡す形。

```swift
let data = try await loading.start(
    message: "Downloading",
    placement: DialogPlacement(verticalAlignment: .start)
) { report in
    report(1)
    return Data()
}
```

登録済みの custom content のスコープ形に配置の上書きを足した形。

```swift
try await loading.start(
    ProgressLoadingViewModel(),
    placement: DialogPlacement(verticalAlignment: .start)
) { report in
    report(1)
}
```

インライン factory のスコープ形 (SwiftUI content、配置は省略)。

```swift
try await loading.start(
    ProgressLoadingViewModel(),
    factory: { viewModel in ProgressView(value: viewModel.progress) }
) { report in
    report(1)
}
```

同じ形で factory が `UIView` を返す UIKit 版。

```swift
try await loading.start(
    ProgressLoadingViewModel(),
    factory: { _ in UIActivityIndicatorView(style: .large) }
) { report in
    report(1)
}
```

インライン factory のスコープ形に配置の上書きを足した形 (SwiftUI content)。

```swift
try await loading.start(
    ProgressLoadingViewModel(),
    placement: DialogPlacement(verticalAlignment: .start),
    factory: { viewModel in ProgressView(value: viewModel.progress) }
) { report in
    report(1)
}
```

同じ形で factory が `UIView` を返す UIKit 版。

```swift
try await loading.start(
    ProgressLoadingViewModel(),
    placement: DialogPlacement(verticalAlignment: .start),
    factory: { _ in UIActivityIndicatorView(style: .large) }
) { report in
    report(1)
}
```

ViewModel の型を渡し、`configure` も配置も省く形。

```swift
try await loading.show(ProgressLoadingViewModel.self)
```

型を渡し、`configure` で状態を整えてから表示する形 (配置は省略)。

```swift
try await loading.show(ProgressLoadingViewModel.self) { viewModel in
    viewModel.message = "Connecting"
}
```

型渡しに配置の上書きを足した形 (`configure` は省略)。

```swift
try await loading.show(ProgressLoadingViewModel.self, placement: DialogPlacement(verticalAlignment: .start))
```

型渡しに配置と `configure` の両方を足した形。

```swift
try await loading.show(
    ProgressLoadingViewModel.self,
    placement: DialogPlacement(verticalAlignment: .start),
    configure: { viewModel in viewModel.message = "Connecting" }
)
```

型渡しのスコープ形で、`configure` も配置も省く形。

```swift
try await loading.start(ProgressLoadingViewModel.self) { report in
    report(1)
}
```

型渡しのスコープ形に配置の上書きを足した形。

```swift
try await loading.start(
    ProgressLoadingViewModel.self,
    placement: DialogPlacement(verticalAlignment: .start)
) { report in
    report(1)
}
```

型渡しのスコープ形に配置と `configure` の両方を足した形。

```swift
try await loading.start(
    ProgressLoadingViewModel.self,
    placement: DialogPlacement(verticalAlignment: .start),
    configure: { viewModel in viewModel.message = "Uploading" }
) { report in
    report(1)
}
```

## 命令形で表示して閉じる

`show` と `hide` は対で使う。`hide` は走行中の処理をキャンセルせず、現在の表示を閉じて、器の撤去が終わってから戻る。

```swift
import Observation
import KsDialogs

@MainActor
@Observable
final class SyncScreenModel {
    private let loading: any KsLoading

    private(set) var status = ""

    init(loading: any KsLoading = Loading.shared) {
        self.loading = loading
    }

    func synchronize() async {
        await loading.show(message: "Connecting")
        await loading.setMessage("Downloading")
        await downloadItems()
        await loading.hide()
        status = "Synchronized"
    }

    private func downloadItems() async {}
}
```

## 処理の間だけ表示する

`start` は表示の寿命と処理を対にする。処理の戻り値はそのまま呼び出し元へ返り、処理が投げた失敗も表示を閉じたうえで伝播する。報告した進捗は `0...1` に丸められる。

```swift
import Foundation
import KsDialogs

extension SyncScreenModel {
    func downloadReport() async throws -> Data {
        try await loading.start(message: "Downloading") { report in
            report(0.25)
            let data = try await fetchReport()
            report(1)
            return data
        }
    }

    private func fetchReport() async throws -> Data {
        Data()
    }
}
```

## スコープ形の処理が始まるスレッドを選ぶ

`start` に渡す処理の型は、どの `start` でも `@MainActor @Sendable (@Sendable @escaping (Double) -> Void) async throws -> T` である。そのため、その場で書いたクロージャはどのスレッドから `start` を呼んでもメインスレッドで始まり、中で UIKit に `await` なしで触れる。重い処理をメインスレッド外で始めたいときは、クロージャに `@concurrent` を付ける。その場合、中で MainActor の状態に `await` なしで触るとコンパイルエラーになる。スレッドを選ぶ引数は無く、処理の isolation で決まる。

メインスレッドで始まった処理は、`await` の後もメインスレッドで再開する。処理の中で `await MainActor.run { ... }` を使う書き方もそのまま動く。重い同期処理をメインスレッドのまま書くとメインスレッドを塞ぎ、終わるまで進捗やメッセージの更新が画面に出ない。

```swift
import UIKit
import KsDialogs

extension SyncScreenModel {
    func applyPhoto(_ photo: UIImage, to imageView: UIImageView) async throws {
        try await loading.start(message: "Applying") { report in
            imageView.image = photo
            report(1)
        }
    }

    func buildIndex() async throws -> Int {
        try await loading.start(message: "Indexing") { @concurrent report in
            let count = (1...1_000_000).reduce(0) { $0 + $1 % 7 }
            report(1)
            return count
        }
    }
}
```

関数を名前で渡す (`start(work)`) と、引数の型の `@MainActor` ではなく、その関数自身の isolation が優先される。

| 名前で渡した関数 | 始まるスレッド |
|---|---|
| `@MainActor` | メインスレッド |
| `@concurrent` | メインスレッド外 |
| isolation の指定が無い `async` 関数で、利用者のモジュールの `NonisolatedNonsendingByDefault` が無効 (Swift 6 の既定) | メインスレッド外 |
| isolation の指定が無い `async` 関数で、利用者のモジュールの `NonisolatedNonsendingByDefault` が有効 (Xcode 26 の新規プロジェクトの既定) | メインスレッド (呼び出し元の isolation を引き継ぐ) |

どちらの設定でも、その関数は `@MainActor` と宣言されていないので、中で MainActor の状態 (UIKit を含む) に `await` なしで触るとコンパイルエラーになる。

## 画面が出る前に始める

出す先の画面 (前面でアクティブなシーンの key window) が無くても、Loading は処理を実行しながら画面が現れるのを待つ。`start` は処理の結果を通常どおり返す。

- 画面が現れた時点で表示が続いていれば、そこで content を作り、入りの演出から表示する
- 画面が現れる前に表示が終われば (最後の `start` の終了、または `hide`)、何も表示せず、待ちもやめる
- 処理の終わり際に画面が現れると、Loading が短い間だけ表示されて閉じることがある

content を作る時点は、開始時点に画面があったかで分かれる。画面があれば factory は開始時点に呼ばれ、投げた失敗は開始の失敗として呼び出し元へ返る (処理は実行しない)。画面が無ければ factory は画面が現れた時点に呼ばれ、投げた失敗は呼び出し元へ返らない。警告ログを残して表示だけを諦め、処理はそのまま続いて結果を返す。登録漏れの判定と ViewModel の進捗の受け口の紐付けは開始時点に行うので、表示の前に報告した進捗も受け口へ届く。

## 内蔵 Loading の見た目を設定する

`style` と `options` は表示の開始時に読まれるため、変更は次の表示から効く。アプリ起動時に一度設定しておくのが基本形になる。

`LoadingStyle` は indicator とメッセージを制御する。`progressFormat` は `LoadingStyle.ProgressFormat` の closure で、既定は `LoadingStyle.defaultProgressFormat`。`options` は `DialogOptions` で、`isCanceledOnTouchOutside` は Loading では効かないままになる。

```swift
import SwiftUI
import UIKit
import KsDialogs

@main
struct MyApp: App {
    init() {
        Loading.shared.style = LoadingStyle(
            indicatorColor: .systemBlue,
            messageFontSize: 16,
            messageColor: .white,
            defaultMessage: "Working",
            progressFormat: { message, progress in
                guard let progress else { return message ?? "" }
                return "\(message ?? "") \(Int(progress * 100))%"
            }
        )
        Loading.shared.options = DialogOptions(
            dialogMargin: DialogEdgeInsets(all: 32),
            overlayColor: UIColor.black.withAlphaComponent(0.5)
        )
    }

    var body: some Scene {
        WindowGroup {
            SyncScreen()
        }
    }
}
```

## custom content を登録して表示する

custom Loading のレジストリは `Loading.shared.registry` (`LoadingViewRegistry`) で、Dialog のものとは独立している。content の ViewModel は class の `LoadingViewModel` にする。

`start` の進捗を受け取るモデルは `LoadingProgressReceiver` も実装する。実装しないモデルへは転送されないだけで、誤りにはならない。

```swift
import Observation
import SwiftUI
import KsDialogs

@MainActor
@Observable
final class ProgressLoadingViewModel: LoadingViewModel, LoadingProgressReceiver {
    var message = ""
    var progress = 0.0

    func onProgress(_ progress: Double) {
        self.progress = progress
    }
}

struct ProgressLoadingView: View {
    let viewModel: ProgressLoadingViewModel

    var body: some View {
        ProgressView(value: viewModel.progress) {
            Text(viewModel.message)
        }
        .padding()
        .ksDialogTransition(.fade())
    }
}
```

登録は見た目の設定と同じ場所 (`App` の `init`) に足す。

```swift
Loading.shared.registry.register(ProgressLoadingViewModel.self) { viewModel in
    ProgressLoadingView(viewModel: viewModel)
}
```

呼び出し元は内蔵 Loading と同じ形で、ViewModel を渡すだけである。

```swift
extension SyncScreenModel {
    func uploadItems() async throws {
        try await loading.start(ProgressLoadingViewModel()) { report in
            report(0.5)
            try await upload()
        }
    }

    private func upload() async throws {}
}
```

`show` と `start` には、レジストリを変えないインライン factory も渡せる。

## 型を渡して custom content を表示する

ViewModel の型だけを渡す `show` / `start` は、登録済みの ViewModel factory が作ったインスタンスを `configure` してから表示する。呼び出し元が ViewModel を組み立てずに済み、Dialog の型渡しと同じ形である ([ViewModel](view-models.md))。

レジストリのエントリは View factory と ViewModel factory の 2 つの slot を持ち、型を渡す経路は両方を必要とする。ViewModel factory の登録は content の登録と同じ場所に足す。

```swift
Loading.shared.registry.register(ProgressLoadingViewModel.self, viewModel: {
    ProgressLoadingViewModel()
})
```

`configure` は `async throws` で、その完了を待ってから進捗の受け口が紐付き、content が作られる。したがって `configure` で入れた値は content が読むより前に入っている。ViewModel factory と `configure` が投げた失敗は表示へ進まず、合流にも数えず、そのまま呼び出し元へ伝わる。型を渡す `start` ではこのとき処理も実行されない。

ViewModel factory と `configure` は MainActor で実行されるので、中で UIKit の API をそのまま呼べる。

```swift
extension SyncScreenModel {
    func uploadFromRegisteredViewModel() async throws {
        try await loading.start(ProgressLoadingViewModel.self, configure: { viewModel in
            viewModel.message = "Uploading"
        }) { report in
            report(0.5)
            try await upload()
        }
    }
}
```

合流の判定に入るのは `configure` の完了後である。非同期の `configure` を待っている間に別の開始が表示を確定していれば、この呼び出しは合流側になり、生成した ViewModel は表示に使われない (content は最初の開始のものが維持される)。

解決するのは `show` / `start` を呼んだ時点の登録内容なので、表示中に登録し直しても、出ている Loading の content も進捗の転送先も変わらない。

## 構成ミスの失敗を扱う

Loading で起こりうる構成ミスは content と ViewModel の解決で、失敗した場合は表示も処理の実行も行われない (fail-fast)。内蔵 Loading の経路 (`show(message:)` / `start(message:_:)`) にはどちらの解決も無いため、失敗しない。型を渡す経路では、ViewModel factory の未登録が View factory の未登録より先に判定される。

| `DialogError` の case | メッセージ (`localizedDescription`) | 原因と対処 |
|---|---|---|
| `viewFactoryNotRegistered(viewModelType:)` | `No View factory is registered for ViewModel type {TypeName}.` | その ViewModel 型の content を解決できない。起動時に `Loading.shared.registry.register(_:factory:)` を呼ぶ (Dialog / Toast のレジストリとは別物である) |
| `viewModelFactoryNotRegistered(viewModelType:)` | `No ViewModel factory is registered for ViewModel type {TypeName}.` | 型を渡す `show` / `start` に ViewModel factory がない。起動時に `Loading.shared.registry.register(_:viewModel:)` を呼ぶ |
| `viewFactoryTypeMismatch(viewModelType:)` | `The registered View factory cannot accept ViewModel type {TypeName}.` | 型消去をまたぐ受け渡し (KMP の入口) での不整合。Swift だけで登録・表示している限り通常は起きない |
| `viewModelFactoryTypeMismatch(viewModelType:)` | `The registered ViewModel factory does not produce ViewModel type {TypeName}.` | 同じく型消去をまたぐ受け渡しでの不整合で、型を渡す経路だけで起こりうる |

表のメッセージは現在の実装が返す値であり、安定した API ではない (変わらないのは case と throw される条件であり、文言は予告なく変わりうる)。

たとえば起動時に見た目だけ設定して `register` を書かないと、`ProgressLoadingViewModel` の content を解決できず `viewFactoryNotRegistered` になる。

```swift
@main
struct MyApp: App {
    init() {
        Loading.shared.style = LoadingStyle(defaultMessage: "Working")
    }

    var body: some Scene {
        WindowGroup {
            SyncScreen()
        }
    }
}
```

呼び出し元では case ごとに `catch` でき、`viewModelType` から解決できなかった ViewModel の型名を読める。構成ミスは実行時に直せるものではないので、開発中に気づける形にしておく。

```swift
extension SyncScreenModel {
    func uploadItemsSafely() async {
        do {
            try await uploadItems()
        } catch DialogError.viewFactoryNotRegistered(let viewModelType) {
            assertionFailure("Register loading content for \(viewModelType) during startup")
        } catch {
            status = "Failed"
        }
    }
}
```
