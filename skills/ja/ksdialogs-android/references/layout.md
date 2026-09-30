# レイアウトを添付する

Dialog の大きさ・位置・背後の覆い・外側タップの扱いは、コンテンツに添付して指定する。この文書は添付できる属性と値の決まり方、Compose と View それぞれの添付の書き方、表示中のページ (画面のバーの内側) を基準にした配置を扱う。

## 添付する 2 つの値

| 値 | 供給するもの | 渡せる経路 |
|---|---|---|
| `DialogOptions` | 大きさ・基準領域・覆い・外側タップの扱い | 添付だけ |
| `DialogPlacement` | 置き場所 (配置と移動量) | 添付と `show` の引数 |

どちらも `data class` で、コンストラクタ引数にはすべて既定値がある。数値は `Double` の dp であり、ピクセルではない。

### `DialogOptions` のプロパティ

| プロパティ | 型 | 供給するもの | 既定値 |
|---|---|---|---|
| `layoutArea` | `DialogLayoutArea` (`WINDOW` / `VISIBLE_AREA` / `CURRENT_PAGE`) | 大きさと位置の計算の基準になる領域 | `VISIBLE_AREA` |
| `dialogMargin` | `DialogEdgeInsets` | 基準領域の各辺から控除する余白 | 全辺 0 |
| `proportionalWidth` / `proportionalHeight` | `Double` | その軸の基準領域に対する比率 | `-1.0` (未指定) |
| `overlayColor` | `Int` (`@ColorInt` の ARGB 32bit) | Dialog の背後を覆う色 | `0x66000000` (黒 40%) |
| `isCanceledOnTouchOutside` | `Boolean` | 外側タップでキャンセルするか | `true` |

`DialogEdgeInsets` は 4 辺を `top`、`left`、`bottom`、`right` の順に取る。全辺が同じ値なら `DialogEdgeInsets(24.0)`、余白なしなら `DialogEdgeInsets.ZERO` と書ける。

`DialogLayoutArea` の値は次の基準領域を選ぶ。どれを選んでも水平・垂直の両軸に効く。

| 値 | 基準になる領域 |
|---|---|
| `WINDOW` | Dialog を載せるウィンドウの全体 |
| `VISIBLE_AREA` | ウィンドウからシステムバーなどが占める幅を除いた領域 |
| `CURRENT_PAGE` | アプリが教えた表示中のページの矩形のうち、可視領域と重なる部分 (「表示中のページを基準にする」) |

### `DialogPlacement` のプロパティ

| プロパティ | 型 | 供給するもの | 既定値 |
|---|---|---|---|
| `horizontalAlignment` / `verticalAlignment` | `DialogAlignment` (`START` / `CENTER` / `END` / `FILL`) | その軸の配置 | `CENTER` |
| `offsetX` / `offsetY` | `Double` | 配置後の平行移動。正の `offsetX` は右、正の `offsetY` は下 | `0.0` |

`DialogAlignment` の `START` と `END` は物理方向 (水平軸なら左と右、垂直軸なら上と下) で、書字方向には追随しない。

## 値の決まり方

- **比率** — `0` より大きく `1` 以下の値はそのまま使い、`1` を超える値は `1` に丸める。ゼロ・負数・非有限値は未指定になる
- **余白** — 負の辺も非有限値の辺も、その辺だけ `0` に丸める
- **移動量** — 非有限値は `0` になる
- **大きさの優先順位** — 比率 > `FILL` 配置 > コンテンツ自身の大きさ。1 つの軸では比率が `FILL` に勝ち、そのとき `FILL` は中央配置として働く
- **上限** — 比率の基準は余白を控除する前の領域、大きさの上限は控除した後の領域である。offset はこの上限で切り詰めないため、コンテンツを画面の外へ押し出せる
- **外側タップ** — `isCanceledOnTouchOutside` が `true` なら外形の外側のタップで `Cancelled` になる。`false` ならタップは何も起こさず、背後の画面へ透過もしない。`overlayColor` の値とは独立に働く

## いつの値が使われるか

実効値は、初回のネイティブレイアウトパスが終わった時点で添付されていた値である。

- 表示中に添付値を書き換えても、出ている Dialog には反映されない
- ウィンドウの寸法やシステムバーの insets が変わったときは、その実効値のまま再配置される
- ソフトキーボードの出入りでは動かない
- `CURRENT_PAGE` のページの矩形は、表示の開始時と、この再配置のたびに取り直す。画面遷移で表示中のページが変わっただけでは再配置しない
- 器は表示先の画面のシステムバーの指定 (アイコンの明暗・バーの表示/非表示) を変えない。`overlayColor` に透明を指定しても同じである
- `show` に渡した `placement` は、添付された `DialogPlacement` をオブジェクトごと置き換える。フィールド単位では合成しない
- `DialogOptions` を `show` の引数で渡す経路はない

## Compose コンテンツへ添付する

`KsDialogAttributes` をコンテンツの冒頭で宣言する。`LazyColumn` のような遅延評価されるスコープの中にだけ書いた宣言は初回の組み立てで実行されず、その表示には効かない。`KsDialogAttributes` は Compose 系 artifact `jp.kamusoft:ksdialogs` に入っている。

以下は画面下端に幅いっぱいで貼り付くシート風のコンテンツで、余白・比率・覆い・配置をまとめて添付している。

```kotlin
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import jp.kamusoft.ksdialogs.DialogAlignment
import jp.kamusoft.ksdialogs.DialogEdgeInsets
import jp.kamusoft.ksdialogs.DialogNotifier
import jp.kamusoft.ksdialogs.DialogOptions
import jp.kamusoft.ksdialogs.DialogPlacement
import jp.kamusoft.ksdialogs.compose.KsDialogAttributes

@Composable
fun SheetConfirmContent(viewModel: ConfirmViewModel, notifier: DialogNotifier<Boolean>) {
    KsDialogAttributes(
        options = DialogOptions(
            dialogMargin = DialogEdgeInsets(16.0),
            proportionalWidth = 1.0,
            proportionalHeight = 0.8,
            isCanceledOnTouchOutside = true,
        ),
        placement = DialogPlacement(
            horizontalAlignment = DialogAlignment.FILL,
            verticalAlignment = DialogAlignment.END,
            offsetY = -12.0,
        ),
    )
    Column {
        Text(viewModel.message)
        Button(onClick = { notifier.complete(true) }) { Text("OK") }
    }
}
```

## View コンテンツへ添付する

`View` の拡張プロパティ `ksDialogOptions` と `ksDialogPlacement` に設定する。値の実体は View のタグなので、View を組み立てるときに設定しておけばよい。

以下は同じシート風のコンテンツを View で書いた例で、基準領域をウィンドウ全体に変え、辺ごとに違う余白を与えている。

```kotlin
import android.content.Context
import android.graphics.Color
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import jp.kamusoft.ksdialogs.DialogAlignment
import jp.kamusoft.ksdialogs.DialogEdgeInsets
import jp.kamusoft.ksdialogs.DialogLayoutArea
import jp.kamusoft.ksdialogs.DialogNotifier
import jp.kamusoft.ksdialogs.DialogOptions
import jp.kamusoft.ksdialogs.DialogPlacement
import jp.kamusoft.ksdialogs.ksDialogOptions
import jp.kamusoft.ksdialogs.ksDialogPlacement

class SheetConfirmCardView(
    context: Context,
    viewModel: ConfirmViewModel,
    notifier: DialogNotifier<Boolean>,
) : LinearLayout(context) {
    init {
        orientation = VERTICAL
        addView(TextView(context).apply { text = viewModel.message })
        addView(
            Button(context).apply {
                text = "OK"
                setOnClickListener { notifier.complete(true) }
            },
        )
        ksDialogOptions = DialogOptions(
            layoutArea = DialogLayoutArea.WINDOW,
            dialogMargin = DialogEdgeInsets(24.0, 20.0, 32.0, 20.0),
            proportionalWidth = 0.9,
            overlayColor = Color.argb(77, 0, 0, 0),
            isCanceledOnTouchOutside = false,
        )
        ksDialogPlacement = DialogPlacement(verticalAlignment = DialogAlignment.END)
    }
}
```

## 登録する

添付はコンテンツ側の記述なので、登録の書き方は属性を持たないコンテンツと変わらない。

```kotlin
import android.app.Application
import jp.kamusoft.ksdialogs.Dialog
import jp.kamusoft.ksdialogs.compose.registerCompose

class SampleApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Dialog.instance.registry.registerCompose(ConfirmViewModel::class) { viewModel, notifier ->
            SheetConfirmContent(viewModel, notifier)
        }
    }
}
```

View コンテンツなら、同じ場所で `register` を呼ぶ。

```kotlin
Dialog.instance.registry.register(ConfirmViewModel::class, ::SheetConfirmCardView)
```

## 1 回の表示だけ置き場所を変える

`show` に `placement` を渡すと、その呼び出しだけ添付された置き場所を差し替えられる。添付した `DialogOptions` はそのまま効く。

以下は登録済みのコンテンツを、画面上端寄りに 20 dp 下げて表示する例である。

```kotlin
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import jp.kamusoft.ksdialogs.Dialog
import jp.kamusoft.ksdialogs.DialogAlignment
import jp.kamusoft.ksdialogs.DialogPlacement
import kotlinx.coroutines.launch

class ItemActivity : ComponentActivity() {
    fun onDeleteClicked() {
        lifecycleScope.launch {
            Dialog.instance.show(
                ConfirmViewModel("Delete this item?"),
                placement = DialogPlacement(
                    horizontalAlignment = DialogAlignment.CENTER,
                    verticalAlignment = DialogAlignment.START,
                    offsetY = 20.0,
                ),
            )
        }
    }
}
```

## 表示中のページを基準にする

`DialogLayoutArea.CURRENT_PAGE` を選ぶと、画面のトップバーやボトムバーを除いたページの内側を基準に大きさと位置が決まる。タブバーを持つ画面で末尾寄せにすれば、Dialog の下端はタブバーの上端から `dialogMargin` だけ上に出て、比率サイズもタブバーを除いた高さに対する割合になる。

Android には OS としての「ページ」が無いので、どこがページかをアプリが教える。教え方は 2 つあり、両方あるときは上が優先され、上に候補が無いときに下を使う。

| 順 | UI 技術 | 教え方 | 配布物 |
|---|---|---|---|
| 1 | Compose | ページの枠に `Modifier.markAsDialogCurrentPage()` を付ける | `jp.kamusoft:ksdialogs` |
| 2 | Android View | ページの View を返す関数を `DialogCurrentPage.provider` に登録する | `jp.kamusoft:ksdialogs-core` |

どちらからもページが得られないときは、表示を失敗させずに `VISIBLE_AREA` と同じ結果で表示し、理由をタグ `KsDialogs` の警告ログに出す。基準になるのは教えた矩形そのもの (と可視領域の共通部分) なので、バーを含む画面全体ではなく、バーの内側の枠を教える。

### Compose の画面に印を付ける

`Scaffold` を使う画面では、content 枠 (`innerPadding` の内側) に 1 回付ける。付けた composable は画面に載っている間だけ候補になり、画面遷移などで組み立てから外れると候補から外れる。

```kotlin
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import jp.kamusoft.ksdialogs.compose.markAsDialogCurrentPage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen() {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Orders") }) },
        bottomBar = { NavigationBar {} },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .markAsDialogCurrentPage(),
        ) {
            Text("Order list")
        }
    }
}
```

候補が複数あるときは、矩形が入れ子なら内側を、それ以外は最後に画面に載ったものを採る。`Crossfade`・`AnimatedContent`・Navigation Compose のフェード遷移では、切り替えの間は去る画面の印も候補に残るため、その間に出した Dialog は去る画面を基準にすることがある。条件分岐で画面を切り替える構成ではこれは起きない。

### View の画面でページを返す関数を登録する

ページの View を返す関数を `DialogCurrentPage.provider` に代入する。`null` を代入すると登録を解除する。関数は UI スレッドで、各表示の開始時と、表示中にウィンドウの寸法やシステムバーの幅が変わったときに呼ばれる。表示中に差し替えた登録は次の表示から効く。

```kotlin
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.activity.ComponentActivity
import jp.kamusoft.ksdialogs.DialogCurrentPage

class OrdersActivity : ComponentActivity() {
    private lateinit var pageContainer: FrameLayout
    private val pageProvider: () -> View? = { pageContainer }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pageContainer = FrameLayout(this)
        val bottomBar = LinearLayout(this)
        setContentView(
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                addView(pageContainer, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
                addView(bottomBar, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 160))
            },
        )
    }

    override fun onResume() {
        super.onResume()
        DialogCurrentPage.provider = pageProvider
    }

    override fun onDestroy() {
        if (DialogCurrentPage.provider === pageProvider) {
            DialogCurrentPage.provider = null
        }
        super.onDestroy()
    }
}
```

関数が `null` を返した・例外を投げた・返した View の矩形が空だった・返した View が Dialog を出す Activity のウィンドウ (同じ Activity で出したモーダル・ダイアログのウィンドウを含む) に載っていない、のいずれかなら、ページは得られなかった扱いになる。

### コンテンツ側で基準領域を選ぶ

基準領域の指定は他の `DialogOptions` と同じくコンテンツに添付する。以下はページの下端に幅いっぱいで出すシート風のコンテンツである。

```kotlin
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import jp.kamusoft.ksdialogs.DialogAlignment
import jp.kamusoft.ksdialogs.DialogEdgeInsets
import jp.kamusoft.ksdialogs.DialogLayoutArea
import jp.kamusoft.ksdialogs.DialogNotifier
import jp.kamusoft.ksdialogs.DialogOptions
import jp.kamusoft.ksdialogs.DialogPlacement
import jp.kamusoft.ksdialogs.compose.KsDialogAttributes

@Composable
fun PageSheetContent(viewModel: ConfirmViewModel, notifier: DialogNotifier<Boolean>) {
    KsDialogAttributes(
        options = DialogOptions(
            layoutArea = DialogLayoutArea.CURRENT_PAGE,
            dialogMargin = DialogEdgeInsets(16.0),
            proportionalWidth = 1.0,
        ),
        placement = DialogPlacement(verticalAlignment = DialogAlignment.END),
    )
    Column {
        Text(viewModel.message)
        Button(onClick = { notifier.complete(true) }) { Text("OK") }
    }
}
```

`Loading.instance.options` でも `CURRENT_PAGE` を選べ、Dialog と同じ規則でページを解決するが、Loading と Toast でこの値を選んだときの見え方は今後変わりうる。
