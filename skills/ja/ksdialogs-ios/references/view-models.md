# ViewModel から結果を報告する

Dialog の結果は、表示中の ViewModel が `notifier` プロパティから報告する。この文書は `notifier` の読み方と、ViewModel の型だけを渡す `show` の使い方を扱う。型を渡す `show` は Loading と Toast にも同じ形で用意されており、機能ごとの違いは最後の節にまとめてある。

## `notifier` を読む

`notifier` は表示中だけ値を持つ。`?.` で呼べば、表示していない間の報告は何も起こさない。

| 項目 | 内容 |
|---|---|
| 型 | ViewModel が宣言した `Result` の `DialogNotifier<Result>` |
| `show` の前後 | `nil` (表示中だけ値を返す) |
| 付く時点 | `show` が content を生成する直前 |
| 外れる時点 | 結果が呼び出し元へ渡る前。完了・キャンセル・失敗のどの終わり方でも外れる |
| factory 引数の notifier との関係 | 同じ配送先を指す |
| 報告の回数 | 最初の 1 回だけが結果を確定させ、以後の報告は何もしない |

以下は文字列を結果とする ViewModel で、`save()` が完了を、`close()` がキャンセルを報告する。content からは `@Bindable` で状態を編集する。

```swift
import Observation
import SwiftUI
import KsDialogs

@MainActor
@Observable
final class EditorViewModel: DialogViewModel {
    typealias Result = String
    var text = ""

    func save() {
        notifier?.complete(text)
    }

    func close() {
        notifier?.cancel()
    }
}

struct EditorView: View {
    @Bindable var viewModel: EditorViewModel

    var body: some View {
        VStack {
            TextField("Name", text: $viewModel.text)
            Button("Save") { viewModel.save() }
            Button("Cancel") { viewModel.close() }
        }
        .padding()
    }
}
```

## ViewModel は class にする

- `DialogViewModel` に準拠できるのは `Sendable` な class だけである。notifier をインスタンスの同一性で引くため、struct は準拠できない
- 1 つのインスタンスに結び付く notifier は 1 本だけである。同じインスタンスを重ねて `show` すると、2 回目が `DialogError.viewModelAlreadyShowing(viewModelType:)` を投げる。最初の表示は影響を受けない
- 重ねて出したいときは、表示ごとに新しいインスタンスを作る。同じ型でも別インスタンスなら独立して重なる

## 型から ViewModel を生成して configure する

型を渡す `show` は ViewModel の型だけを受け取り、登録済みの ViewModel factory が作ったインスタンスを `configure` してから表示する。呼び出し元が ViewModel を組み立てずに済む。

### 必要な登録

レジストリのエントリは独立した 2 つの slot を持つ。

| slot | 登録する API | 使う show |
|---|---|---|
| View factory | `registry.register(_:factory:)` | インスタンス渡しと型渡しの両方 |
| ViewModel factory | `registry.register(_:viewModel:)` | 型渡しだけ |

- 型を渡す `show` は両方を必要とする。ViewModel factory が無ければ、content を作る前に `DialogError.viewModelFactoryNotRegistered(viewModelType:)` を投げる。起動時の登録漏れはこのエラーで気づける
- 同じ slot への再登録は後勝ちで置き換わり、もう片方の slot は残る
- 解決するのは `show` を呼んだ時点の登録内容なので、表示中に登録し直しても出ている Dialog は変わらない

### 生成から表示までの手順

順序は次に固定されている。

1. ViewModel factory がインスタンスを生成する
2. `configure` が完了する
3. notifier が紐付く
4. View factory が content を生成する
5. Dialog を表示する

したがって `configure` で入れた値は、content が状態を読むより前に必ず入っている。生成と `configure` は MainActor で実行される。ViewModel factory と `configure` が投げた失敗はキャンセルにならず、表示へ進まずにそのまま呼び出し元へ伝わる。

以下は上の `EditorViewModel` を 2 つの slot とも登録する例である。登録はアプリ起動時に 1 回だけ行う。

```swift
@main
struct MyApp: App {
    init() {
        Dialog.shared.registry.register(EditorViewModel.self) { viewModel in
            EditorView(viewModel: viewModel)
        }
        Dialog.shared.registry.register(EditorViewModel.self, viewModel: {
            EditorViewModel()
        })
    }

    var body: some Scene {
        WindowGroup {
            ItemScreen()
        }
    }
}
```

呼び出し元は型を渡し、`configure` で初期値を入れてから表示する。`ItemScreenModel` は [Dialog](dialogs.md) で定義したものである。

```swift
extension ItemScreenModel {
    func renameItem() async throws {
        let result = try await dialogs.show(EditorViewModel.self) { viewModel in
            viewModel.text = "Apple"
        }
        if case .completed(let name) = result {
            status = name
        }
    }
}
```

`configure` は `async throws` なので、表示前に非同期で初期状態を読み込むこともできる。その完了を待ってから content が作られる。

```swift
extension ItemScreenModel {
    func editLoadedItem() async throws {
        let result = try await dialogs.show(EditorViewModel.self) { viewModel in
            viewModel.text = try await self.loadCurrentName()
        }
        if case .completed(let name) = result {
            status = name
        }
    }

    func loadCurrentName() async throws -> String { "Apple" }
}
```

## Loading と Toast でも型を渡せる

型を渡す `show` は Dialog 専用の経路ではなく、Loading と Toast にも同じ形で用意されている。Loading にはスコープ形の型渡し `start` もある。3 つのレジストリは互いに独立していて、同じ ViewModel 型を別々に登録できるが、slot の登録 API の綴りはどれも `register(_:factory:)` と `register(_:viewModel:)` である。

3 機能で共通なのは、ViewModel factory がインスタンスを生成すること・解決が呼び出し時点のスナップショットであること・「生成 → `configure` → content の生成 → 表示」の順序・生成と `configure` が MainActor で実行されること・ViewModel factory の未登録が構成ミスとして失敗することである。違うのは次の 3 点になる。

| 機能 | `configure` | content の生成前に挟まる紐付け | ViewModel factory と `configure` の失敗 |
|---|---|---|---|
| Dialog | `async throws` | notifier | 表示へ進まず、キャンセルにならずに呼び出し元へ伝わる |
| Loading (`show` / `start`) | `async throws` | 進捗の受け口 | 表示へ進まず呼び出し元へ伝わり、合流にも数えない。型渡しの `start` は処理も実行しない |
| Toast | 同期 (`throws` は書ける) | なし | `show` は既に戻っているため返らず、その表示 1 枚だけが破棄される |

書き方と例は [Loading](loading.md) と [Toast](toast.md) にある。
