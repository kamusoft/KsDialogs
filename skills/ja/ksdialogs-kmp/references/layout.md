# 配置と大きさを決める

器の見え方を決める属性は 2 種類ある。共有コードから渡せるのは置き場所だけで、それ以外は host が content に添付する。

| 属性 | 何を決めるか | どこから供給するか |
|---|---|---|
| 置き場所 (`DialogPlacement`) | 配置と移動量 | 共有コードの `show` 引数、または host の content への添付 |
| 静的 option (`DialogOptions`) | 基準領域・余白・比率・覆い・外側タップ | host の content への添付だけ |

Loading と Toast も同じ属性を使う。Loading は同じ option を受け取るが外側タップは常にブロックし、Toast は置き場所だけを受け取る。

## 共有コードから置き場所を渡す

`show` に `DialogPlacement` を渡すと、その呼び出しの間だけ置き場所が決まる。渡した値は content に添付された置き場所を**オブジェクトまるごと置き換える**もので、フィールド単位では合成しない。message だけを渡す Loading・Toast の呼び出しでは、host が決めたアプリ既定の配置を置き換える。

| プロパティ | 型 | 既定値 | 何を決めるか |
|---|---|---|---|
| `horizontalAlignment` | `DialogAlignment` | `CENTER` | 水平方向の寄せ |
| `verticalAlignment` | `DialogAlignment` | `CENTER` | 垂直方向の寄せ |
| `offsetX` | `Double` | `0.0` | 配置を決めた後の水平移動量。正の値で右へ動く |
| `offsetY` | `Double` | `0.0` | 配置を決めた後の垂直移動量。正の値で下へ動く |

- `DialogAlignment` は `START` / `CENTER` / `END` / `FILL` の 4 つ。`START` と `END` は物理方向 (水平軸なら左と右、垂直軸なら上と下) で、書字方向には追随しない
- `FILL` は位置だけでなく大きさも基準領域いっぱいに広げる。その軸に比率が指定されているときは大きさの決め方として採用されず、位置は `CENTER` として扱う
- offset は最後に加算され、画面内へクランプされない。意図して画面外へ押し出せる
- 長さは論理単位で、iOS は pt、Android は dp になる。非有限値は 0 に丸められる

次は、[Dialog](dialogs.md) の呼び出し元に置き場所を足して、削除の確認 Dialog を画面下端へ寄せる例である。`DeleteViewModel` の宣言と host での登録も同じ文書にある。

```kotlin
package com.example.shared

import jp.kamusoft.ksdialogs.kmp.Dialog
import jp.kamusoft.ksdialogs.kmp.DialogAlignment
import jp.kamusoft.ksdialogs.kmp.DialogException
import jp.kamusoft.ksdialogs.kmp.DialogPlacement
import jp.kamusoft.ksdialogs.kmp.DialogResult
import jp.kamusoft.ksdialogs.kmp.KsDialog
import kotlin.coroutines.cancellation.CancellationException

class ItemListViewModel(
    private val repository: ItemRepository,
    private val dialogs: KsDialog = Dialog.instance,
) {
    @Throws(DialogException::class, CancellationException::class)
    suspend fun deleteItem(itemName: String) {
        val result = dialogs.show(
            viewModel = DeleteViewModel(itemName),
            placement = DialogPlacement(
                horizontalAlignment = DialogAlignment.FILL,
                verticalAlignment = DialogAlignment.END,
                offsetY = -24.0,
            ),
        )
        if (result is DialogResult.Completed && result.value) repository.delete(itemName)
    }
}
```

## host で静的 option を添付する

基準領域から外側タップまでは content の性質なので、commonMain は `DialogOptions` を公開しない。host が組み立てた content に添付する。

| プロパティ | 既定値 | 何を決めるか |
|---|---|---|
| `layoutArea` | 可視領域 | 基準 rect の選択。ウィンドウ全体・ウィンドウからシステムの insets を除いた可視領域・表示中のページのいずれか。水平・垂直の両軸に効く |
| `dialogMargin` | 全辺 0 | 基準 rect から控除する余白。最大サイズを決めるほか、先頭寄せ・末尾寄せでも端から離す。負の辺と非有限の辺は 0 になる |
| `proportionalWidth` / `proportionalHeight` | 未指定 (`-1`) | その軸の基準 rect に対する比率。`0` 以下は未指定、`1` を超える値は `1` に丸める |
| `overlayColor` | 黒 40% | Dialog の背後を覆う層の色 |
| `isCanceledOnTouchOutside` | `true` | 外側タップでキャンセルするか。無効のとき、タップは背後の画面へも届かない |

余白の既定は全辺 0 なので、配置は基準 rect の端にそのまま接する。端から離したいときは `dialogMargin` を添付する。内蔵 Toast だけは自分の中身に全辺 24 の余白を持つ ([Toast](toast.md))。

綴りは host ごとに違う。

| 型 | Android | iOS |
|---|---|---|
| 基準領域 | `DialogLayoutArea.WINDOW` / `DialogLayoutArea.VISIBLE_AREA` / `DialogLayoutArea.CURRENT_PAGE` | `DialogLayoutArea.window` / `DialogLayoutArea.visibleArea` / `DialogLayoutArea.currentPage` |
| 配置 | `DialogAlignment.START` / `CENTER` / `END` / `FILL` | `DialogAlignment.start` / `.center` / `.end` / `.fill` |
| 4 辺の余白 | `DialogEdgeInsets(top, left, bottom, right)`, `DialogEdgeInsets(all)`, `DialogEdgeInsets.ZERO` | `DialogEdgeInsets(top:left:bottom:right:)`, `DialogEdgeInsets(all:)`, `DialogEdgeInsets.zero` |
| 覆いの色 | ARGB 32bit の `Int` | `UIColor` |
| 従来 View 系への添付 | 拡張プロパティ `ksDialogOptions` / `ksDialogPlacement` | extension プロパティ `ksDialogOptions` / `ksDialogPlacement` |
| 宣言的 UI への添付 | `KsDialogAttributes(options = …, placement = …)` | modifier `.ksDialogOptions(…)` / `.ksDialogPlacement(…)` |

登録そのものの書き方は [Android host](android-host.md) と [iOS host](ios-host.md) にある。

### Android host

従来の View 系では、content の拡張プロパティに設定する。

```kotlin
val content = DeleteContentView(this, viewModel, notifier)
content.ksDialogOptions = DialogOptions(
    layoutArea = DialogLayoutArea.WINDOW,
    dialogMargin = DialogEdgeInsets(all = 16.0),
    proportionalWidth = 0.9,
    proportionalHeight = 0.6,
    overlayColor = 0x66000000,
    isCanceledOnTouchOutside = false,
)
content.ksDialogPlacement = DialogPlacement(verticalAlignment = DialogAlignment.END, offsetY = -24.0)
```

Compose の content では、composable の冒頭で `KsDialogAttributes` を宣言する。

```kotlin
Dialog.instance.registry.registerCompose(DeleteViewModel::class) { viewModel, notifier ->
    KsDialogAttributes(
        options = DialogOptions(layoutArea = DialogLayoutArea.VISIBLE_AREA, proportionalWidth = 0.8),
        placement = DialogPlacement(verticalAlignment = DialogAlignment.END),
    )
    DeleteContent(viewModel.itemName, notifier)
}
```

### iOS host

UIKit の content では、view の extension プロパティに設定する。

```swift
let content = DeleteContentView(itemName: viewModel.itemName, notifier: notifier)
content.ksDialogOptions = DialogOptions(
    layoutArea: .window,
    dialogMargin: DialogEdgeInsets(all: 16),
    proportionalWidth: 0.9,
    proportionalHeight: 0.6,
    overlayColor: UIColor.black.withAlphaComponent(0.6),
    isCanceledOnTouchOutside: false
)
content.ksDialogPlacement = DialogPlacement(verticalAlignment: .end, offsetY: -24)
```

SwiftUI の content では modifier で添付する。

```swift
Dialog.shared.kmp.register(DeleteViewModel.self) { viewModel, notifier in
    DeleteContent(itemName: viewModel.itemName, notifier: notifier)
        .ksDialogOptions(DialogOptions(layoutArea: .visibleArea, proportionalWidth: 0.8))
        .ksDialogPlacement(DialogPlacement(verticalAlignment: .end))
}
```

## 表示中のページを基準にする

基準領域に表示中のページを選ぶと、いま画面に出ているページのうちナビゲーションバーやタブバーを除いた内側 (と可視領域の共通部分) が基準 rect になる。タブバーのある画面で末尾寄せにすれば、Dialog はタブバーの上端から `dialogMargin` だけ上に出て、比率サイズもタブバーを除いた高さに対する割合になる。

共有コードには基準領域を選ぶ口もページを教える口も無い。選択は host が content に添付し、ページは各 host の口で教える。

| host | 選び方 | ページの教え方 |
|---|---|---|
| Android | `DialogLayoutArea.CURRENT_PAGE` | Compose は `Modifier.markAsDialogCurrentPage()`、従来 View は `DialogCurrentPage.provider`。どちらも無ければページは得られない |
| iOS | `DialogLayoutArea.currentPage` | SwiftUI は `markAsDialogCurrentPage()`、UIKit は `DialogCurrentPage.provider`。何もしなくても UIKit のナビゲーション・タブのコンテナまでは既定で辿る |

ページが得られないときも表示は失敗せず、可視領域を選んだときと同じ結果になる (理由は英語の警告ログに出る)。

Android の添付は選択肢を変えるだけでよい。

```kotlin
Dialog.instance.registry.register(DeleteViewModel::class) { viewModel, notifier ->
    DeleteContentView(this, viewModel, notifier).apply {
        ksDialogOptions = DialogOptions(
            layoutArea = DialogLayoutArea.CURRENT_PAGE,
            dialogMargin = DialogEdgeInsets(all = 16.0),
        )
        ksDialogPlacement = DialogPlacement(verticalAlignment = DialogAlignment.END)
    }
}
```

iOS の添付も同じである。

```swift
Dialog.shared.kmp.register(DeleteViewModel.self) { viewModel, notifier in
    DeleteContent(itemName: viewModel.itemName, notifier: notifier)
        .ksDialogOptions(DialogOptions(layoutArea: .currentPage, dialogMargin: DialogEdgeInsets(all: 16)))
        .ksDialogPlacement(DialogPlacement(verticalAlignment: .end))
}
```

ページの教え方の完動レシピは [Android host](android-host.md) と [iOS host](ios-host.md) にある。Loading と Toast の content にも同じ値を添付でき、Dialog と同じ規則でページを解決するが、それ以上の挙動 (Toast のアプリ既定配置との整合など) は決まっていない。

## 添付が効く時点

- 添付は初回表示までに評価される場所に書く
- Compose では、`LazyColumn` の item のように遅延評価されるスコープの中に置いた `KsDialogAttributes` は初回の組み立てで実行されず、その表示には効かない
- SwiftUI の添付は子から親へ遡って合流するため、同じ属性を二重に添付した場合は外側が勝つ
- 実効値は初回のネイティブレイアウトパスで凍結される。その後に添付を書き換えても、表示中の Dialog は動かない
- 回転やウィンドウ寸法・insets の変化では、凍結済みの値のままレイアウトをやり直す。表示中のページを選んでいれば、このときページの矩形を取り直す (ページの教え方の差し替えは次の表示から効く)
- 表示中に画面遷移で表示中のページが変わっても、それだけでは Dialog は動かない
- ソフトキーボードの開閉では Dialog は動かない
