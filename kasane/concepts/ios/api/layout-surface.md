---
type: concept
title: iOS のレイアウト公開面
description: iOS Native (Swift) でダイアログの大きさと位置を指定するときの公開名と署名 — 属性の型とプロパティ・UIKit の添付プロパティと SwiftUI の modifier・show 引数での置き場所指定・表示中のページの教え方 (既定の探し方・関数の登録・SwiftUI の modifier)・SwiftUI の中身から見える安全領域・論理単位と色型
tags: [ios, layout, api, surface]
timestamp: 2026-10-21
---

# iOS のレイアウト公開面

この文書を読むと、iOS Native (Swift) でダイアログの大きさ・位置・背後の覆い・外側タップの扱いを指定するときに書く型名とプロパティ名、UIKit / SwiftUI での添付の書き方、基準領域「表示中のページ」のページをライブラリへ教える方法、器に載せた SwiftUI の中身から安全領域がどう見えるか、コード例が分かる。

**この文書は iOS の公開面 (名前・署名・コード例・framework 固有の注意) だけを扱い、挙動の契約は core が正である。** 対応する契約は [レイアウトのルール](../../core/api/layout-semantics.md) で、属性の意味・既定値・優先順位・rect の決まり方はそちらを読む。

## 属性の型

| 用途 | 書く名前 |
|---|---|
| 静的メタ属性 (基準領域・余白・比率・覆い・外側タップ) | `DialogOptions` |
| 置き場所 (整列と移動量) | `DialogPlacement` |
| 基準領域の選択肢 | `DialogLayoutArea` (`.window` / `.visibleArea` / `.currentPage`) |
| 配置の選択肢 | `DialogAlignment` (`.start` / `.center` / `.end` / `.fill`) |
| 4 辺の余白 | `DialogEdgeInsets` (`init(top:left:bottom:right:)` と全辺同値の `init(all:)`、`.zero`) |

`DialogOptions` / `DialogPlacement` / `DialogEdgeInsets` は `struct`、`DialogLayoutArea` / `DialogAlignment` は `enum` である。`DialogOptions` と `DialogPlacement` はすべてのイニシャライザ引数に既定値があり、`DialogEdgeInsets` は 4 辺を明示するか `init(all:)` / `.zero` を使う。数値は `Double` (論理単位 pt)。

### `DialogOptions` のプロパティ

| プロパティ | 型 |
|---|---|
| `layoutArea` | `DialogLayoutArea` |
| `dialogMargin` | `DialogEdgeInsets` |
| `proportionalWidth` / `proportionalHeight` | `Double` |
| `overlayColor` | `UIColor` |
| `isCanceledOnTouchOutside` | `Bool` |

### `DialogPlacement` のプロパティ

`horizontalAlignment` / `verticalAlignment` (`DialogAlignment`) と `offsetX` / `offsetY` (`Double`)。

## UIKit での添付

中身になる `UIView` の extension プロパティに設定する。設定しなかったほう (nil) は供給なしとして扱う (優先順位は core が定める)。

| プロパティ | 型 |
|---|---|
| `ksDialogOptions` | `DialogOptions?` |
| `ksDialogPlacement` | `DialogPlacement?` |

```swift
// content は registry へ登録する中身の View (登録の書き方は Dialog 公開面)
let content = ConfirmContentView()
content.ksDialogOptions = DialogOptions(
    layoutArea: .window,
    dialogMargin: DialogEdgeInsets(all: 16),
    proportionalWidth: 0.9,
    isCanceledOnTouchOutside: false
)
content.ksDialogPlacement = DialogPlacement(verticalAlignment: .end, offsetY: -24)
```

## SwiftUI での添付

中身の body ルートに modifier を付ける。

| modifier | 渡す型 |
|---|---|
| `.ksDialogOptions(...)` | `DialogOptions` |
| `.ksDialogPlacement(...)` | `DialogPlacement` |

```swift
Dialog.shared.registry.register(ConfirmViewModel.self) { viewModel, notifier in
    // notifier は結果 (completed / cancelled) を返す口 (Dialog 公開面)
    ConfirmContent(message: viewModel.message, notifier: notifier)
        .ksDialogOptions(DialogOptions(proportionalWidth: 0.8))
        .ksDialogPlacement(DialogPlacement(verticalAlignment: .end))
}
```

添付は View の階層を子から親へ遡って合流するため、入れ子の内側と外側の両方に同じ属性を添付した場合は**外側 (より上位の View) が勝つ**。添付は初回表示までに評価される位置に書く。

中身に属性を付ける名前は、UIKit の extension プロパティも SwiftUI の modifier も `ks` で始まる同じ綴りである。OS の型 (`UIView`) に後から足す名前を他のライブラリや OS 自身のメンバーとぶつけないための接頭辞で、SwiftUI の modifier は UIKit のプロパティと綴りを揃えている。表示中のページの印 (`markAsDialogCurrentPage()`) は中身への添付ではなく、対になる UIKit のプロパティも無いので `ks` を付けない ([core/ADR-0045](../../../decisions/core/0045-ks-prefix-limited-to-view-attachments.md))。

## show 引数での置き場所指定

`show(_:placement:)` の `placement:` に `DialogPlacement` を渡すと、添付された置き場所をオブジェクトまるごと置換する。静的メタ属性を渡す引数は無い。

```swift
let result = try await Dialog.shared.show(
    ConfirmViewModel(message: "削除しますか?"),
    placement: DialogPlacement(horizontalAlignment: .fill)
)
```

## 表示中のページの教え方

基準領域に `.currentPage` を選んだときのページは、次の表の 3 つの取得元に、表の上から順に問い合わせて決まる (上位が候補を持たないときに下位へ進む条件は core の「基準領域」節が定める)。どの取得元でも、基準になるのはページの View の safe area の内側 (`safeAreaLayoutGuide.layoutFrame`) と可視領域の共通部分である。

| 順 | 取得元 | 書く名前 |
|---|---|---|
| 1 | SwiftUI の modifier | `View.markAsDialogCurrentPage()` |
| 2 | UIKit 向けの関数の登録 | `DialogCurrentPage.provider` (`(@MainActor () throws -> UIView?)?`) |
| 3 | 既定の探し方 (内蔵) | なし (登録不要) |

### 既定の探し方 (UIKit のコンテナまで)

何も登録しなくても、ライブラリはダイアログを出す window の view controller 階層を辿ってページを決める。root から present の連なりを先端まで進み (閉じる途中の画面から先は辿らない)、そこから `UINavigationController` の `topViewController` と `UITabBarController` の `selectedViewController` へ、コンテナでなくなるまで降りた先の view controller の view がページになる。連なりの途中にある KsDialogs 自身の器 (Dialog / Loading / Toast のコンテナ) はページに選ばず通り抜けるので、ダイアログを重ねて出しても 2 つ目の基準は背後の画面のままになる。

保証するのは UIKit のコンテナまでである。root が `UIHostingController` の SwiftUI の画面では、`TabView` / `NavigationStack` の内側まで降りず、hosting controller の view がページになる。その safe area は SwiftUI のバーを含んだままなので、modifier を付けずに既定の探し方に任せると、結果はほぼ可視領域と同じになる (タブバーを避けない)。SwiftUI の画面は次の modifier で教える。

### SwiftUI: `markAsDialogCurrentPage()`

`TabView` / `NavigationStack` で組んだ画面では、各画面の中身の枠 (バーの内側) に 1 回付けるのが正規の経路である。付けた View は画面に載っている間だけ候補になり、関数の登録と既定の探し方より優先される。

```swift
TabView {
    NavigationStack {
        OrdersScreen()
            .markAsDialogCurrentPage()   // ナビゲーションバーとタブバーの内側が基準になる
            .navigationTitle("Orders")
    }
    .tabItem { Label("Orders", systemImage: "list.bullet") }

    SettingsScreen()
        .markAsDialogCurrentPage()
        .tabItem { Label("Settings", systemImage: "gearshape") }
}
```

候補が複数あるときの決め方 (入れ子は内側・それ以外は最後に画面に載ったもの) は core の「modifier の台帳」の規則に従う。iOS ではさらに、ウィンドウに載っていても画面に表示されていない印 (印自身かウィンドウまでの祖先が `isHidden`、または `alpha` が 0.01 未満) を外す。`TabView` はタブの切り替えの間、去るタブの画面を `alpha` 0 へ向けてアニメーションさせながらウィンドウに残すため、これを外さないと去るタブの印が選ばれ得る。判定にはアニメーションの最終値 (model layer の `alpha`) を使い、画面に見えている途中の値 (presentation layer) は見ない。

### UIKit: `DialogCurrentPage.provider`

独自のコンテナで画面を切り替えているなど、既定の探し方では届かないアプリは、表示中のページの View を返す関数を一度登録する。`nil` を代入すると既定の探し方に戻る。表示開始時点で登録されていた関数が、メインスレッドで、その表示の開始時と、表示中に window の寸法や window の safe area が変わったときに呼ばれる (表示中に登録を差し替えても、そのダイアログには古い関数が使われ続ける)。

```swift
DialogCurrentPage.provider = { [weak container] in
    container?.visibleChild?.view   // 表示中の子画面の view を返す
}
```

関数が `nil` を返す・エラーを投げる・返した View がダイアログを出す window に載っていない・返した View の safe area の内側が空・window の外にある、のいずれかなら、既定の探し方の結果を使う。

## SwiftUI の中身から見える安全領域

器 (Dialog・Loading・Toast の中身を載せて表示する、ライブラリのコンテナ) に載せた SwiftUI の中身は、決まった条件の軸でだけ、器とシステムの領域の重なりを SwiftUI 標準の安全領域として受け取る ([ios/ADR-0002](../../../decisions/ios/0002-swiftui-content-receives-standard-safe-area.md))。システムの領域は window の safe area の外側 (ステータスバー・ホームインジケータ等が占める帯) で、重なりは器の矩形がそこへはみ出している幅を辺ごとに測ったものである。中身は器の中で `UIHostingController` に載せて表示され、KMP から Swift 側の登録口で登録した SwiftUI の中身も同じ載せ方を通る。安全領域を避ける (既定) か `.ignoresSafeArea(.container)` で無視するかは、SwiftUI の標準の書き方で選ぶ。

### 受け取る条件

軸ごとに、基準領域と、その軸の位置の決まり方で決まる。

| 基準領域 | その軸の位置 (配置) | その軸の辺で受け取る安全領域 |
|---|---|---|
| `.window` | 端 — `.start` / `.end`、比率 (`proportionalWidth` / `proportionalHeight`) を伴わない `.fill` | 器とシステムの領域の重なり |
| `.window` | 中央 — `.center`、比率を伴う `.fill` | 0 |
| `.visibleArea` (既定)・`.currentPage` | どれでも (移動量でシステムの領域に重なっても) | 0 |

- 届くのはシステムの領域 (UIKit の safe area にあたるもの) だけで、キーボードの領域は含まない
- 表示中に window の寸法や window の safe area が変わると、受け取る値も新しい重なりになる
- 例: 縦長の window 393 × 852 (safe area は上 59・下 34・左右 0) で、基準領域を `.window`、垂直の配置を `.start` (水平は既定の `.center`) にした中身は、上 59・下 0・左右 0 を受け取る。両軸を `.fill` にした中身は上 59・下 34 を受け取る

UIView で書いた中身は UIKit の伝播で、配置や基準領域を問わず器とシステムの領域の重なりを safe area として受け取る。SwiftUI の中身との差が出るのは、`.window` で中央に置いた中身がシステムの領域に重なるときと、`.visibleArea`・`.currentPage` で移動量によりシステムの領域に重ねたときである。中央の軸に渡さないのは、渡すと中央に置いた中身が安全領域を避けて器の中でずれるためで、ios/ADR-0002 で退けた案にあたる。

### 器の大きさは安全領域で変わらない

器へ伝える要求サイズ (中身が器に求める大きさで、器はこれを基に自分の大きさを決める) は、中身が安全領域を受け取らないときと同じ値 (中身の理想の大きさ) で、受け取った重なりは器の大きさに入らない。そのため器の大きさと位置はどの配置でも安全領域の受け渡しで変わらず、端に付けた中身は器の内側で安全領域を避ける。器が要求サイズと違う幅で置いた (基準領域から余白を除いた有効領域の幅で頭打ちにした・`.fill` で広げた) ときは、置かれた幅で折り返した高さを求め直す。表示中に中身の理想の大きさ (文言など) が変わると、器はその新しい大きさに追従する。

比率も `.fill` も指定しない軸 (大きさを中身に委ねた軸) は、表示中に有効領域が前より広がると、中身の理想の大きさから求め直して新しい上限まで広がり、狭まると新しい有効領域で頭打ちになる。広がる場面は、window の回転・window の寸法の変化と、SwiftUI の中身に添付した基準領域 (`ksDialogOptions(DialogOptions(layoutArea: .window))`) が最初の配置の後に届いて、有効領域が可視領域から window 全体になったときである。SwiftUI と UIView のどちらの中身でも同じに振る舞う。例: 縦長で幅 393 に頭打ちになっていた理想の幅 600 の中身は、横長にすると 600 になり、縦長に戻すと 393 に戻る。

有効領域の大きさが変わらないまま器が配置し直されるとき (同じ寸法の window での再レイアウトなど) は求め直さない。折り返す本文は上限よりわずかに狭い幅 (上限 734 に対して 733.33 など) で折り返しが決まることがあり、配置し直すたびに求め直すと器の幅が動き続けるためである。

### 端に付けた中身の使える大きさ

端に付けた中身は器を大きくせずに安全領域を避けるので、使える大きさは重なりの分だけ減る。393 × 852 の例の window で上端に付けた理想の高さ 100 の縮められる中身 (提案された大きさまで縮む中身) は、器が y 0・高さ 100 のまま、中身は y 59 から高さ 41 に置かれる (座標は window 基準)。window の幅を超える長い本文 (折り返す `Text`) も同じで、器の高さは安全領域を受け取らないときと同じまま、本文に使える高さだけが 59 減るので、本文全体は表示しきれない (折り返す本文は縮められる中身なので、次の「収まらない中身の並び方」には当たらない)。避け方は次のどれかである。

| 避け方 | 結果 |
|---|---|
| 基準領域を既定の `.visibleArea` にする | 器が safe area の内側に置かれ、重なりは 0。中身は器いっぱいを使える |
| 中身の一番外側に `.ignoresSafeArea(.container)` を付ける | 中身は器の矩形いっぱいに置かれる。システムの領域を避けるのは中身の責任になる |
| `dialogMargin` で器を window の端から離す | 器とシステムの領域の重なりが離した分だけ減り、受け取る値も減る |

window 全体を覆う中身 (両軸 `.fill`) では、SwiftUI の画面と同じ書き方で、地色だけを window の端まで広げ、本文を安全領域の内側に置ける (`ExtractViewModel` / `ExtractContent` は利用側の型)。

```swift
Dialog.shared.registry.register(ExtractViewModel.self) { viewModel, notifier in
    ExtractContent(viewModel: viewModel, notifier: notifier)
        .frame(maxWidth: .infinity, maxHeight: .infinity)   // 本文は安全領域の内側いっぱい
        .background(Color.black.ignoresSafeArea())          // 地色だけ window の端まで
        .ksDialogOptions(DialogOptions(layoutArea: .window))
        .ksDialogPlacement(DialogPlacement(horizontalAlignment: .fill, verticalAlignment: .fill))
}
```

### 収まらない中身の並び方

`.window` で片側の端に付けた軸 (`.start` / `.end`。比率の有無を問わない) で、中身が器の中の安全領域の内側に収まらないときは、その軸で、器が付いた端と反対側の辺 (以下「内側の辺」。下端に付けたなら上の辺、上端なら下の辺、左端なら右の辺、右端なら左の辺) にそろえて置かれる ([ios/ADR-0003](../../../decisions/ios/0003-swiftui-content-overflowing-safe-area-aligns-to-inner-edge.md))。そろえた軸では中身は安全領域を受け取らない (0)。収まらない中身とは、縮められる限り縮めても安全領域の内側の大きさに入らない中身である。固定サイズの指定や、文言とボタンを並べた `VStack` のように縮められない部品を並べたものと、最小の大きさが安全領域の内側より大きい中身が当たる。こうした中身は、SwiftUI が安全領域の内側の中央に置くと重なりの半分だけ内側へずれる。そのずれを KsDialogs が起こさないようにしている。器の矩形は変わらない。393 × 852 の例の window では次のように置かれる。

| 中身 | 上端に付けたとき | 下端に付けたとき |
|---|---|---|
| 縮まない高さ 100 (安全領域を無視する指定の有無を問わない) | 器いっぱい (y 0・高さ 100) | 器いっぱい (y 752・高さ 100) |
| 理想の高さ 100・最小の高さ 80 | 縮んだうえで下の辺にそろう (y 20・高さ 80) | 縮んだうえで上の辺にそろう (y 752・高さ 80) |
| 縮まない高さ 1000 (器は window の高さ 852 で頭打ち) | 下の辺にそろい、window の上端の外へ 148 はみ出す (y -148) | 上の辺にそろい、window の下端の外へ 148 はみ出す (y 0) |

左右の辺は物理の向きで決まり、右から左に書く言語でも同じ辺になる。両軸 `.fill`・中央の軸・`.window` 以外の基準領域では並び方を変えず、収まらない中身は SwiftUI の標準どおり安全領域の内側の中央に置かれる。UIView の中身も対象外である。

収まるかどうかは表示中も追い続ける。器の大きさ・中身の理想の大きさ・window の安全領域が変わったときと、理想の大きさは同じまま中身の最小の大きさだけが変わったとき (最小の大きさを足す・外す) に判定し直し、器と中身の位置は新しい状態で落ち着く (位置の変化が止まり、行ったり来たりしない)。

今の挙動の制約として、そろえたあとの中身の大きさが変わらない形で最小の大きさを外したときは、判定し直されずにそろえが残ることがある (コードの読みによる。実測はしていない)。例は、理想の高さ 1000 の中身に表示中に最小の高さ 800 を付けてから外す場合である。器は window の高さ 852 で頭打ちになり、安全領域の内側 759 に 800 は入らないのでそろえが効く。そろえた中身は上の安全領域まで使えるので高さ 818 に置かれ、最小の高さを外しても 818 のまま変わらないため、判定し直すきっかけが来ない。

### `GeometryReader` で読む値

中身の中で `GeometryReader` から読む `GeometryProxy.safeAreaInsets` は、読む View の位置で決まる。安全領域の内側に置かれた、大きさを中身に合わせた `Text` から読むと 0 になる。重なりを読みたいときは、器いっぱいに広がる View (一番外側の `GeometryReader` など) から読む。

中身から読める位置と大きさ (`GeometryReader`・`onGeometryChange`) は、落ち着いた後に描かれた位置と一致する。収まらない中身を内側の辺へそろえるかを判定し直している途中では、画面に描かれない途中の位置が 1 回届くことがあるので、最後に届いた値を使う。

## framework 固有の注意

- **覆いの色は `UIColor`** で渡す。既定は黒 40% (`UIColor(white: 0, alpha: 0.4)` に相当)
- **可視領域は safe area** である。`DialogLayoutArea.visibleArea` を選んだときに控除される insets は UIKit の safe area insets にあたる
- **表示中のページを指定してもページが得られないとき**は `.visibleArea` と同じ結果になり、理由を英語の警告ログ (`The current page could not be resolved, so the visible area is used instead.` で始まる文) に出す
- **数値の単位は pt**。Android の dp と同じ論理単位の役割だが、値の意味は各 OS の論理座標系に従う
- **レイアウトは Auto Layout の制約で反映する**。計算は iOS 側が実 frame として行い、器が中身の View に制約を張る (次の小節)

### 中身の側の制約との関係

上限 (有効領域による頭打ち) と位置の制約は必須なので、中身の側で何をしても守られる。比率・fill で決めたサイズの制約は優先度 999 で、中身の固有サイズの主張には勝つが、中身が必須 (1000) の幅・高さ制約を持つとそちらが勝つ。frame を直接代入しても Auto Layout に上書きされる。

## 関連

- [レイアウトのルール](../../core/api/layout-semantics.md) — 属性の意味・既定値・優先順位・rect の決まり方 (契約の記述はこちら)
- [core/ADR-0038](../../../decisions/core/0038-current-page-layout-area-via-registered-provider.md) — 決定 (表示中のページは器が探さず、登録された取得元から得る)
- [core/ADR-0045](../../../decisions/core/0045-ks-prefix-limited-to-view-attachments.md) — 決定 (接頭辞 `ks` は OS の View 型に後付けする属性とその SwiftUI の対に限る)
- [ios/ADR-0002](../../../decisions/ios/0002-swiftui-content-receives-standard-safe-area.md) — 決定 (SwiftUI の中身には、窓全体基準で端に付けた軸の安全領域との重なりを、器の大きさを変えずに標準の安全領域として届ける)
- [ios/ADR-0003](../../../decisions/ios/0003-swiftui-content-overflowing-safe-area-aligns-to-inner-edge.md) — 決定 (端に付けた SwiftUI の中身が安全領域の内側に収まらないときは、器の内側の辺にそろえて器いっぱいに置く)
- [iOS の Dialog 公開面](dialog-surface.md) — 登録・表示・結果の受け取りの公開面
- [iOS のトランジション公開面](transition-surface.md) — 出入りの演出の添付面とフックの型
