---
type: concept
title: iOS の Dialog 公開面
description: iOS Native (Swift) からダイアログを使うときの公開名と署名 — 既定エントリと登録の入口・結果型の省略形・UIKit / SwiftUI の中身の書き分け・SwiftUI の添付 modifier・インライン show・結果報告口の取得・構成ミスのエラー種別・型指定 show
tags: [ios, dialog, api, surface]
timestamp: 2026-09-06
---

# iOS の Dialog 公開面

この文書を読むと、iOS Native (Swift) からダイアログを登録・表示するときに書く名前と署名、コード例、UIKit / SwiftUI 固有の注意が分かる。

**この文書は iOS の公開面 (名前・署名・コード例・framework 固有の注意) だけを扱い、挙動の契約は core が正である。** 対応する契約は次の 4 本で、「何が起きるか」はそちらを読む:

- [登録と表示の呼び出し面のルール](../../core/api/registration-show-semantics.md) — register / show の基本形・結果型の省略形・中身の技術・インライン show
- [結果通知のルール](../../core/api/result-notification-semantics.md) — show が返すもの・キャンセル・構成ミス
- [多段表示のルール](../../core/api/multi-display-semantics.md) — 重ね出しの保証
- [ViewModel 主導の呼び出しのルール](../../core/api/model-binding-semantics.md) — 結果報告口の VM 供給・型指定 show・参照型限定

## 登録と表示の入口

| 用途 | 書く名前 |
|---|---|
| 既定の表示エントリ | `Dialog.shared` |
| DI で注入する表示契約 | `KsDialog` (実体は `Dialog`) |
| 登録の入口 | `Dialog.shared.registry` (実体は `DialogViewRegistry.shared`) |
| 表示 | `show(_:placement:)` (`async throws`) |

`Dialog()` を自分で作って `KsDialog` として注入しても、既定と同じ `DialogViewRegistry.shared` を共有する。

## 結果型の省略形

`DialogViewModel` は `associatedtype Result` に既定値 `Bool` を持つ。したがって `Result` を書かない ViewModel はそれだけで真偽値の結果になり、show は `DialogResult<Bool>` を返す。カスタム結果型は `typealias Result = String` のように宣言する。

```swift
// Result を宣言しないので、この ViewModel の結果は Bool
final class ConfirmViewModel: DialogViewModel {
    let message: String
    init(message: String) { self.message = message }
}
```

## UIKit と SwiftUI の書き分け

中身は `UIView` を返す factory でも SwiftUI の View を返す factory でも書ける。**どちらも同名の `register(_:factory:)` / `show(_:placement:factory:)` のオーバーロード**で受ける — Swift はクロージャの戻り値型でオーバーロードを解決できるため、別名を用意する必要がない。

SwiftUI の中身は内部で `UIHostingController` に載せて UIKit の中身と同じ表現に集約される。このホストは、ダイアログが閉じるすべての経路 (完了 / キャンセル / 呼び出し元キャンセル / 画面破棄) で破棄される。

```swift
Dialog.shared.registry.register(ConfirmViewModel.self) { viewModel, notifier in
    ConfirmContent(message: viewModel.message, notifier: notifier)   // SwiftUI の View をそのまま返す
        .ksDialogPlacement(DialogPlacement(verticalAlignment: .end))
}

let result = try await Dialog.shared.show(ConfirmViewModel(message: "削除しますか?"))
// result は DialogResult<Bool>
```

## SwiftUI での属性の添付

中身の body ルートに modifier を付ける。

| modifier | 渡す型 | 供給するもの |
|---|---|---|
| `.ksDialogOptions(...)` | `DialogOptions` | 器 (ダイアログを画面に載せる表示コンテナ) の静的な属性 — 大きさ・基準領域 (大きさと位置の基準になる矩形)・背後の覆い・外側タップの扱い |
| `.ksDialogPlacement(...)` | `DialogPlacement` | 置き場所 (整列と移動量) |
| `.ksDialogTransition(...)` | `DialogTransition` | 出入りの演出 |

各型が持つプロパティの意味は、レイアウトとトランジションの公開面が定める (下記「関連」)。

入れ子の内側と外側の両方に同じ属性を添付した場合は外側 (より上位の View) が勝つ。添付は初回表示までに評価される位置に書く。

## インライン show

登録せずに factory を `show` へ直接渡す。UIKit / SwiftUI のどちらの中身でも書け、`placement:` も渡せる。

```swift
let result: DialogResult<Bool> = try await Dialog.shared.show(
    ConfirmViewModel(message: "削除しますか?")
) { viewModel, notifier in
    Button(viewModel.message) { notifier.complete(true) }
}
```

## 結果報告口の取得

`DialogViewModel` の extension プロパティ `vm.notifier` で、show 中の ViewModel から `DialogNotifier` を引ける。型は ViewModel が宣言した `Result` 型 (宣言結果型) に固定され、show の前後は nil になる。

```swift
final class ConfirmViewModel: DialogViewModel {
    func tapOK() { notifier?.complete(true) }   // Result 未宣言なので DialogNotifier<Bool>
}
```

## 参照型限定の強制

`DialogViewModel` は `AnyObject` 制約を持つ protocol なので、struct を準拠させようとした時点でコンパイルエラーになる。実行時検査に落ちる経路はない。

## 型指定 show と VM factory

型指定 show は `Dialog.shared.show(ConfirmViewModel.self) { vm in ... }` の形で、configure は `async throws` でも書ける。VM factory の登録は View factory と同じ `register` のオーバーロード (`register(_:viewModel:)`) で行う。1引数 factory (`register(_:)` に ViewModel だけを取るクロージャを渡す形) も使える。

次は前節までとは独立した例で、configure で書き換えるプロパティを `var` で宣言している。

```swift
final class ConfirmViewModel: DialogViewModel {
    var message = ""                              // configure から書き換えるので var
}

Dialog.shared.registry.register(ConfirmViewModel.self) { vm in
    ConfirmContent(message: vm.message)           // 1引数 factory。報告口は中身側で vm.notifier から引く
}
Dialog.shared.registry.register(ConfirmViewModel.self, viewModel: { ConfirmViewModel() })

let result = try await Dialog.shared.show(ConfirmViewModel.self) { vm in
    vm.message = "削除しますか?"                    // configure: View 生成前に必ず完了する
}
```

## 失敗とキャンセルの形

show は `async throws` で、結果は `DialogResult` の enum (`case completed(Value)` / `case cancelled`) として返る。

構成ミスは結果ではなく throw で届き、種別は例外型の階層ではなく `DialogError` という enum の case で表す。

| 事象 | `DialogError` の case |
|---|---|
| View factory 未登録 | `viewFactoryNotRegistered` |
| 提示先の画面が無い | `presentationHostUnavailable` |
| VM factory 未登録 (型指定 show) | `viewModelFactoryNotRegistered` |
| 同一 ViewModel インスタンスの並行 show | `viewModelAlreadyShowing` |
| 登録済み factory が ViewModel の実際の型を受け取れない | `viewFactoryTypeMismatch` |
| 登録済み VM factory が要求された型の ViewModel を作らない | `viewModelFactoryTypeMismatch` |
| 報告された結果値が ViewModel の宣言結果型へ復元できない | `resultTypeMismatch` |

下 3 つは型消去輸送 (KMP 入口・型指定 show) での不整合で、純 Swift の登録・表示だけを使う限り通常は起きない。

呼び出し元の Task をキャンセルしたときは、show は throw せず結果として `cancelled` を返す。

## 関連

- [iOS のレイアウト公開面](layout-surface.md) — 属性の添付面と型
- [iOS のトランジション公開面](transition-surface.md) — 演出の添付面とフックの型
- [KMP 利用者の iOS ホスト統合](../../kmp/api/ios-host-integration.md) — 同じレジストリを共有する KMP 向けの型付き入口
