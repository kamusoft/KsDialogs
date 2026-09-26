---
type: concept
title: Android のレイアウト公開面
description: Android Native (Kotlin) でダイアログの大きさと位置を指定するときの公開名と署名 — 属性の型とプロパティ・従来 View 系の拡張プロパティと Compose の宣言・show 引数での置き場所指定・表示中のページの教え方 (Compose の modifier・View を返す関数の登録・候補になるウィンドウ)・論理単位と色の表現
tags: [android, layout, api, surface]
timestamp: 2026-09-27
---

# Android のレイアウト公開面

この文書を読むと、Android Native (Kotlin) でダイアログの大きさ・位置・背後の覆い・外側タップの扱いを指定するときに書く型名とプロパティ名、従来 View 系 / Compose での添付の書き方、基準領域「表示中のページ」のページをライブラリへ教える方法、コード例が分かる。

**この文書は Android の公開面 (名前・署名・コード例・framework 固有の注意) だけを扱い、挙動の契約は core が正である。** 対応する契約は [レイアウトのルール](../../core/api/layout-semantics.md) で、属性の意味・既定値・優先順位・rect の決まり方はそちらを読む。

## 属性の型

| 用途 | 書く名前 |
|---|---|
| 静的メタ属性 (基準領域・余白・比率・覆い・外側タップ) | `DialogOptions` |
| 置き場所 (整列と移動量) | `DialogPlacement` |
| 基準領域の選択肢 | `DialogLayoutArea` (`WINDOW` / `VISIBLE_AREA` / `CURRENT_PAGE`) |
| 配置の選択肢 | `DialogAlignment` (`START` / `CENTER` / `END` / `FILL`) |
| 4 辺の余白 | `DialogEdgeInsets` (4 辺を個別に取るコンストラクタと全辺同値の `DialogEdgeInsets(all)`、`DialogEdgeInsets.ZERO`) |

`DialogOptions` / `DialogPlacement` / `DialogEdgeInsets` は `data class`、`DialogLayoutArea` / `DialogAlignment` は `enum class` である。`DialogOptions` と `DialogPlacement` はコンストラクタ引数にすべて既定値があり、`DialogEdgeInsets` は 4 辺を明示するか `DialogEdgeInsets(all)` / `DialogEdgeInsets.ZERO` を使う。数値は `Double` (論理単位 dp)。

### `DialogOptions` のプロパティ

| プロパティ | 型 |
|---|---|
| `layoutArea` | `DialogLayoutArea` |
| `dialogMargin` | `DialogEdgeInsets` |
| `proportionalWidth` / `proportionalHeight` | `Double` |
| `overlayColor` | `Int` (`@ColorInt` の ARGB 32bit) |
| `isCanceledOnTouchOutside` | `Boolean` |

### `DialogPlacement` のプロパティ

`horizontalAlignment` / `verticalAlignment` (`DialogAlignment`) と `offsetX` / `offsetY` (`Double`)。

## 従来 View 系での添付

中身になる `View` の拡張プロパティに設定する。設定しなかったほう (null) は供給なしとして扱う (優先順位は core が定める)。

| プロパティ | 型 |
|---|---|
| `ksDialogOptions` | `DialogOptions?` |
| `ksDialogPlacement` | `DialogPlacement?` |

```kotlin
// content は registry へ登録する中身の View (登録の書き方は Dialog 公開面)
val content = ConfirmContentView(context)
content.ksDialogOptions = DialogOptions(
    layoutArea = DialogLayoutArea.WINDOW,
    dialogMargin = DialogEdgeInsets(all = 16.0),
    proportionalWidth = 0.9,
    isCanceledOnTouchOutside = false,
)
content.ksDialogPlacement = DialogPlacement(verticalAlignment = DialogAlignment.END, offsetY = -24.0)
```

拡張プロパティの実体は View のタグなので、View を作った直後に設定しておけばよい。

## Compose での添付

中身の composable の冒頭で `KsDialogAttributes(options = ..., placement = ...)` を宣言する (引数はすべて省略可で、省略した引数は供給なしとして扱う)。

```kotlin
Dialog.instance.registry.registerCompose(ConfirmViewModel::class) { viewModel, notifier ->
    // notifier は結果 (completed / cancelled) を返す口 (Dialog 公開面)
    KsDialogAttributes(
        options = DialogOptions(proportionalWidth = 0.8),
        placement = DialogPlacement(verticalAlignment = DialogAlignment.END),
    )
    ConfirmContent(viewModel.message, notifier)
}
```

`KsDialogAttributes` は**初回の組み立てで通る位置に書く**。`LazyColumn` のような遅延評価されるスコープの中に書いた宣言は初回の組み立てで実行されず、その表示には効かない。ダイアログの中身以外で呼び出しても何も起こらない。

`KsDialogAttributes` は Compose 系の配布物 `jp.kamusoft:ksdialogs` (Gradle module `:ksdialogs`) に入っており、これを依存に追加した消費者だけが使える。

## show 引数での置き場所指定

`show(viewModel, placement = ...)` に `DialogPlacement` を渡すと、添付された置き場所をオブジェクトまるごと置換する。静的メタ属性を渡す引数は無い。

```kotlin
val result = Dialog.instance.show(
    ConfirmViewModel("削除しますか?"),
    placement = DialogPlacement(horizontalAlignment = DialogAlignment.FILL),
)
```

## 表示中のページの教え方

Android は表示中のページを探す既定の仕組みを持たない (「ページ」が OS の概念に無いため)。`DialogLayoutArea.CURRENT_PAGE` を選ぶときは、アプリが次のどちらかでページを教える。両方あれば上が優先され、上が候補を持たないときに下へ進む (条件は core の「基準領域」節)。どちらからも得られなければ `VISIBLE_AREA` と同じ結果になり、理由が警告ログ (タグ `KsDialogs`、`The current page could not be resolved, so the visible area is used instead.` で始まる文) に出る。

| 順 | 取得元 | 書く名前 | 配布物 |
|---|---|---|---|
| 1 | Compose の modifier | `Modifier.ksDialogCurrentPage()` (`jp.kamusoft.ksdialogs.compose`) | `jp.kamusoft:ksdialogs` |
| 2 | 従来 View 向けの関数の登録 | `DialogCurrentPage.provider` (`(() -> View?)?`) | `jp.kamusoft:ksdialogs-core` |

基準になるのは、教えた composable / View の矩形と可視領域の共通部分である。iOS の safe area にあたるものは使わないので、バーを含む画面全体ではなく**バーの内側の枠** (中身の領域) を教える。

### Compose: `Modifier.ksDialogCurrentPage()`

`Scaffold` を使う画面では、content 枠 (topBar / bottomBar の内側) に 1 回付ける。付けた composable は画面に載っている間だけ候補になり、画面遷移などで組み立てから外れると候補から外れる。

```kotlin
Scaffold(
    topBar = { TopAppBar(title = { Text("Orders") }) },
    bottomBar = { AppBottomNavigation() },
) { innerPadding ->
    OrdersScreen(
        modifier = Modifier
            .padding(innerPadding)
            .fillMaxSize()
            .ksDialogCurrentPage(),   // バーの内側が基準になる
    )
}
```

候補が複数あるときの決め方 (入れ子は内側・それ以外は最後に画面に載ったもの) は core の「modifier の台帳」の規則に従う。`Crossfade` / `AnimatedContent` / Navigation Compose のフェード遷移では、切り替えの間は去る画面の印も台帳に残るため、その間に出したダイアログは去る画面を基準にし得る (iOS のような表示されていない印を外す判定は持たない。条件分岐でタブを切り替える画面では起きない。core の「まだ決めていないこと」)。

### 従来 View: `DialogCurrentPage.provider`

表示中のページの View を返す関数を一度登録する。`null` を代入すると登録を解除する。表示開始時点で登録されていた関数が、UI スレッドで、その表示の開始時と、表示中にウィンドウの寸法やシステムバーの幅が変わったときに呼ばれる (表示中に登録を差し替えても、そのダイアログには古い関数が使われ続ける)。

```kotlin
DialogCurrentPage.provider = {
    activity.findViewById<View>(R.id.page_container)   // バーの内側のページ領域
}
```

関数が `null` を返す・例外を投げる・返した View が次の小節のウィンドウに載っていない・返した View の矩形が空・ダイアログを出す Activity のメインウィンドウと重ならない、のいずれかなら、ページは得られなかった扱いになる。

### 候補になるウィンドウと座標の突き合わせ

候補にするのは、ダイアログを出す Activity が持つウィンドウ (メインウィンドウと、同じ Activity で出したモーダル・ダイアログのウィンドウ) に載った View / composable である。同じ Activity かは、各ウィンドウの根の View が持つ `WindowManager.LayoutParams.token` が Activity のものと同じかで判定する。KsDialogs 自身の器 (Dialog / Loading / Toast) のウィンドウは、同じ Activity に属していても除く (器のウィンドウを見分ける目印として、器が decor view に付けるタグ `ksdialogs_container_window` で判定する)。別の Activity のウィンドウと、親ウィンドウの token を持つ PopupWindow などのパネルは候補にならない。

器は Activity とは別のウィンドウ (`android.app.Dialog`) に載るため、ページと器は**画面座標を共通の原点**にして突き合わせる。View は `getLocationOnScreen` で、Compose の枠は `boundsInWindow()` に枠が載っているウィンドウの根の画面上の位置を足して画面座標にし、器の側は、器のウィンドウの中でダイアログを配置する View (内部の `DialogLayoutHost`) 自身の画面上の位置を引いて、その View の座標へ写す。ウィンドウごとに原点が違っても (モーダルのウィンドウ・マルチウィンドウ)、同じ場所を指す。

### 台帳の内部口 (`@KsDialogsInternalApi`)

Compose の modifier は `:ksdialogs` にあり、台帳の本体は `:ksdialogs-core` にある。Kotlin の `internal` はモジュールをまたげないため、台帳の出入り口 `DialogCurrentPageLedger` と印の型 `DialogCurrentPageMarker` は public だが、`@KsDialogsInternalApi` (`@RequiresOptIn(level = ERROR)`) が付いている。**アプリ向けの登録口ではなく**、オプトインしないまま使うとコンパイルエラーになる。予告なく変わるので、アプリは上の 2 つの登録口だけを使う。

## framework 固有の注意

- **覆いの色は ARGB 32bit の `Int`** で渡す (`@ColorInt`)。既定は `0x66000000` (黒 40%)
- **可視領域はシステムバーを除外した領域**である。`DialogLayoutArea.VISIBLE_AREA` を選んだときに控除される insets はシステムバーなどが占める幅にあたる
- **数値の単位は dp**。器は dp で受け取った値を実 View の px へ変換して配置するため、`Double` の値をピクセルとして渡さない
- レイアウト計算は Android 側が実 View の rect として行う。中身の `View` に `LayoutParams` を設定しても、器が決めた外形 rect は変わらない

## 関連

- [レイアウトのルール](../../core/api/layout-semantics.md) — 属性の意味・既定値・優先順位・rect の決まり方 (契約の記述はこちら)
- [core/ADR-0038](../../../decisions/core/0038-current-page-layout-area-via-registered-provider.md) — 決定 (表示中のページは器が探さず、登録された取得元から得る。Android は既定の探し方を持たない)
- [Android の Dialog 公開面](dialog-surface.md) — 登録・表示・結果の受け取りの公開面
- [Android のトランジション公開面](transition-surface.md) — 出入りの演出の添付面とフックの型
