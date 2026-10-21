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

## iOS の SwiftUI の中身で安全領域を受け取る

iOS では、Dialog・Loading・Toast の SwiftUI の中身はライブラリの器の中で `UIHostingController` に載せて表示され、`Dialog.shared.kmp` で登録した中身も同じ載せ方を通る。中身は、器とシステムの領域 (window の safe area の外側で、ステータスバー・ホームインジケータ等が占める帯) の重なりを、SwiftUI 標準の安全領域として受け取る。受け取るのは次の表の軸だけである。安全領域を避ける (既定) か `.ignoresSafeArea(.container)` で無視するかは、SwiftUI の標準の書き方で選ぶ。

| 基準領域 | その軸の配置 | その軸の辺で受け取る安全領域 |
|---|---|---|
| `.window` | 端 — `.start` / `.end`、または `proportionalWidth` / `proportionalHeight` を伴わない `.fill` | 器とシステムの領域の重なり |
| `.window` | 中央 — `.center`、または比率を伴う `.fill` | 0 |
| `.visibleArea` (既定)・`.currentPage` | どれでも (移動量でシステムの領域に重ねても) | 0 |

- 届くのはシステムの領域だけで、キーボードの領域は含まない
- 表示中に window の寸法や window の safe area が変わると、受け取る値も新しい重なりに追従する
- 例: 縦長の 393 × 852 の window (safe area は上 59・下 34) で、基準領域を `.window`、垂直の配置を `.start` (水平は既定の `.center`) にした中身は、上 59 を受け取り、ほかの辺は 0 になる。両軸を `.fill` にすると上 59・下 34 を受け取る

UIKit の中身 (`UIView`) は UIKit の伝播で、配置や基準領域を問わず重なりを safe area として受け取る。両者の見え方が分かれるのは、`.window` で中央に置いた中身がシステムの領域に重なるときと、`.visibleArea`・`.currentPage` の中身を移動量でシステムの領域へ重ねたときである。

### window 全体を覆い、本文は安全領域の内側に置く

`.window` で両軸を `.fill` にした中身は、SwiftUI の画面と同じ書き方で、地色だけを window の端まで広げ、本文を安全領域の内側に置ける。ここでの `WelcomeViewModel` は、`title` プロパティを持つ共有の `DialogViewModel<Boolean>` である。

```swift
Dialog.shared.kmp.register(WelcomeViewModel.self) { viewModel, notifier in
    VStack(spacing: 16) {
        Text(viewModel.title)
        Button("Continue") { notifier.complete(true) }
    }
    .foregroundStyle(.white)
    .frame(maxWidth: .infinity, maxHeight: .infinity)
    .background(Color.black.ignoresSafeArea())
    .ksDialogOptions(DialogOptions(layoutArea: .window))
    .ksDialogPlacement(DialogPlacement(horizontalAlignment: .fill, verticalAlignment: .fill))
}
```

### `GeometryReader` で重なりを読む

中身の中で `GeometryReader` から読む `GeometryProxy.safeAreaInsets` は、読む View の位置で決まる。安全領域の内側に置かれた、大きさを中身に合わせた `Text` から読むと 0 になる。重なりを読みたいときは、器いっぱいに広がる View (一番外側の `GeometryReader` など) から読む。読める位置と大きさは、配置が落ち着いた後に描かれた位置と一致する。後で述べる収まらない中身のそろえを判定し直している途中では、画面に描かれない途中の位置が 1 回届くことがあるので、最後に届いた値を使う。次の例は、ホームインジケータの無い端末のように下端に何も重ならないときだけ、下に余白を取る。

```swift
Dialog.shared.kmp.register(WelcomeViewModel.self) { viewModel, notifier in
    GeometryReader { proxy in
        VStack(spacing: 16) {
            Text(viewModel.title)
            Button("Continue") { notifier.complete(true) }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding(.bottom, proxy.safeAreaInsets.bottom > 0 ? 0 : 24)
    }
    .foregroundStyle(.white)
    .background(Color.black.ignoresSafeArea())
    .ksDialogOptions(DialogOptions(layoutArea: .window))
    .ksDialogPlacement(DialogPlacement(horizontalAlignment: .fill, verticalAlignment: .fill))
}
```

### 端に付けた中身の大きさを確保する

受け取った安全領域で器は大きくならない。器が求める大きさは、安全領域を受け取らないときと同じ中身の理想の大きさなので、器の大きさと位置は変わらず、端に付けた中身は器の内側で安全領域を避ける。そのぶん使える大きさは重なりの分だけ減る。例の window で上端に付けた理想の高さ 100 の縮められる中身 (提案された大きさまで縮む中身) は、器が y 0・高さ 100 のまま、中身は y 59 から高さ 41 に置かれる。折り返す長い本文も縮められる中身なので、同じく使える高さが 59 減り、本文全体は表示しきれない。次のどれかで避ける。

| 避け方 | 結果 |
|---|---|
| 基準領域を既定の `.visibleArea` のままにする | 器が safe area の内側に置かれて重なりは 0 になり、中身は器いっぱいを使える |
| 中身の一番外側に `.ignoresSafeArea(.container)` を付ける | 中身は器の矩形いっぱいに置かれる。システムの領域を避けるのは中身の責任になる |
| `dialogMargin` で器を window の端から離す | 離した分だけ重なりが減り、受け取る値も減る |

器が求めた大きさと違う幅で置かれた (基準領域から余白を除いた幅で頭打ちになった・`.fill` で広げた) ときは、置かれた幅で折り返した高さを求め直す。表示中に中身の理想の大きさ (文言など) が変わると、器はその新しい大きさに追従する。

比率も `.fill` も指定しない軸 (大きさを中身に委ねた軸) は、表示中に基準領域から余白を除いた幅が前より広がると、中身の理想の大きさから求め直して新しい上限まで広がり、狭まると新しい幅で頭打ちになる。広がるのは、window の回転・window の寸法の変化と、SwiftUI の中身に添付した `.window` が最初の配置の後に届いて、基準が可視領域から window 全体になったときである。SwiftUI と UIView のどちらの中身でも同じに振る舞う。例: 縦長で幅 393 に頭打ちになっていた理想の幅 600 の中身は、横長にすると 600 になり、縦長に戻すと 393 に戻る。その幅が変わらないまま配置し直されるとき (同じ寸法の window での再レイアウトなど) は求め直さない。

### 安全領域の内側に収まらない中身の置かれ方

`.window` で片側の端 (`.start` / `.end`。比率の有無を問わない) に付けた軸で、中身が器の中の安全領域の内側に収まらないときは、その軸で器が付いた端と反対側の辺 (内側の辺。下端に付けたなら上の辺、上端なら下の辺、左端なら右の辺、右端なら左の辺) にそろえて置かれ、その軸では安全領域を受け取らない (0)。収まらない中身とは、縮められる限り縮めても安全領域の内側に入らない中身で、`.frame(width:height:)` などの固定サイズの指定、文言とボタンを並べた `VStack` のように縮められない部品を並べたもの、最小の大きさが安全領域の内側より大きいものが当たる。器の矩形は変わらない。例の window では次のように置かれる。

| 中身 | 上端に付けたとき | 下端に付けたとき |
|---|---|---|
| 縮まない高さ 100 (`.ignoresSafeArea` の有無を問わない) | 器いっぱい (y 0・高さ 100) | 器いっぱい (y 752・高さ 100) |
| 理想の高さ 100・最小の高さ 80 | 縮んだうえで下の辺にそろう (y 20・高さ 80) | 縮んだうえで上の辺にそろう (y 752・高さ 80) |
| 縮まない高さ 1000 (器は window の高さ 852 で頭打ち) | 下の辺にそろい、window の上端の外へ 148 はみ出す (y -148) | 上の辺にそろい、window の下端の外へ 148 はみ出す (y 0) |

- 左右の辺は物理の向きで決まり、右から左に書く言語でも同じ辺になる
- 両軸 `.fill`・中央の軸・`.window` 以外の基準領域では並び方を変えず、収まらない中身は SwiftUI の標準どおり安全領域の内側の中央に置かれる。UIKit の中身 (`UIView`) も対象外である
- 収まるかどうかは表示中も判定し直す。器の大きさ・中身の理想の大きさや最小の大きさ・window の安全領域が変わると、器と中身の位置は新しい状態で落ち着く

## 添付が効く時点

- 添付は初回表示までに評価される場所に書く
- Compose では、`LazyColumn` の item のように遅延評価されるスコープの中に置いた `KsDialogAttributes` は初回の組み立てで実行されず、その表示には効かない
- SwiftUI の添付は子から親へ遡って合流するため、同じ属性を二重に添付した場合は外側が勝つ
- 実効値は初回のネイティブレイアウトパスで凍結される。その後に添付を書き換えても、表示中の Dialog は動かない
- 回転やウィンドウ寸法・insets の変化では、凍結済みの値のままレイアウトをやり直す。表示中のページを選んでいれば、このときページの矩形を取り直す (ページの教え方の差し替えは次の表示から効く)
- 表示中に画面遷移で表示中のページが変わっても、それだけでは Dialog は動かない
- ソフトキーボードの開閉では Dialog は動かない
