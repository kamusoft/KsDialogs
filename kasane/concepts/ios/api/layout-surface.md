---
type: concept
title: iOS のレイアウト公開面
description: iOS Native (Swift) でダイアログの大きさと位置を指定するときの公開名と署名 — 属性の型とプロパティ・UIKit の添付プロパティと SwiftUI の modifier・show 引数での置き場所指定・表示中のページの教え方 (既定の探し方・関数の登録・SwiftUI の modifier)・論理単位と色型
tags: [ios, layout, api, surface]
timestamp: 2026-09-30
---

# iOS のレイアウト公開面

この文書を読むと、iOS Native (Swift) でダイアログの大きさ・位置・背後の覆い・外側タップの扱いを指定するときに書く型名とプロパティ名、UIKit / SwiftUI での添付の書き方、基準領域「表示中のページ」のページをライブラリへ教える方法、コード例が分かる。

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
- [iOS の Dialog 公開面](dialog-surface.md) — 登録・表示・結果の受け取りの公開面
- [iOS のトランジション公開面](transition-surface.md) — 出入りの演出の添付面とフックの型
