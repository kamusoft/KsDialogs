# Android ホスト統合

共有 ViewModel の契約は Android Native のものの typealias なので、Android host は Native API をそのまま使い、KMP 専用の入口を必要としない。content は `Dialog.instance.registry` (型は `DialogViewRegistry`)、`Loading.instance.registry` (型は `LoadingViewRegistry`)、`Toast.instance.registry` (型は `ToastViewRegistry`) へ登録する。3 つは独立していて、Dialog に登録した class が Loading や Toast に登録されることはない。

## View の content を登録する

Android アプリの起動経路に置く。次で使う共有 ViewModel の class は機能別レシピで定義している。

```kotlin
import android.widget.Button
import android.widget.TextView
import jp.kamusoft.ksdialogs.Dialog
import jp.kamusoft.ksdialogs.DialogTransition
import jp.kamusoft.ksdialogs.Loading
import jp.kamusoft.ksdialogs.Toast
import jp.kamusoft.ksdialogs.ksDialogTransition
import jp.kamusoft.ksdialogs.notifier

object DialogHostRegistration {
    fun register() {
        Dialog.instance.registry.register(DeleteViewModel::class) { viewModel, notifier ->
            Button(this).apply {
                text = "Delete ${viewModel.itemName}"
                setOnClickListener { notifier.complete(true) }
                ksDialogTransition = DialogTransition.fade()
            }
        }

        Dialog.instance.registry.register(ChoiceViewModel::class) { viewModel ->
            Button(this).apply {
                text = viewModel.title
                setOnClickListener { viewModel.notifier?.complete("accepted") }
            }
        }

        Loading.instance.registry.register(UploadLoadingViewModel::class) {
            TextView(this).apply { text = "Uploading" }
        }

        Toast.instance.registry.register(StatusToastViewModel::class) { viewModel ->
            TextView(this).apply { text = viewModel.message }
        }
    }
}
```

factory の receiver は提示先画面の `Context` で、表示のたびに呼ばれるため content は毎回新しく作られる。Dialog の factory は 2 つ目の引数で `DialogNotifier<R>` を受け取るか、`viewModel.notifier` を読む。この紐付けは factory の実行より前に済んでおり、表示していない間は null になる。Loading と Toast の factory は ViewModel だけを受け取る。返した View は自分でレイアウトと演出の添付を持つ。View のコンストラクタが `(Context, VM)` なら、コンストラクタ参照をそのまま 1 引数 factory にできる。

## Compose の content を登録する

KMP artifact は Android View 系 artifact `jp.kamusoft:ksdialogs-core` を推移的に運ぶが、Compose 用の extension は引き込まない。Compose 系 artifact `jp.kamusoft:ksdialogs` (これが View 系本体も連れてくる) を Android アプリへ追加したうえで、各 registry の `registerCompose` overload を使う。`register` と別名なのは、`@Composable` 付きの関数型と通常の関数型を同名で並べると呼び出し側の型推論が曖昧になるためである。

```kotlin
dependencies {
    implementation("jp.kamusoft:ksdialogs:{version}")
    implementation("androidx.compose.foundation:foundation:1.8.1")
}
```

`{version}` はプレースホルダで、使う version に置き換える (そのままでは依存解決に失敗する)。現在の version は、常に最新のリリースへ解決される [latest release](https://github.com/kamusoft/KsDialogs/releases/latest) のページで確認できる。

```kotlin
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.Modifier
import jp.kamusoft.ksdialogs.Dialog
import jp.kamusoft.ksdialogs.Loading
import jp.kamusoft.ksdialogs.Toast
import jp.kamusoft.ksdialogs.compose.registerCompose
import jp.kamusoft.ksdialogs.notifier

object ComposeHostRegistration {
    fun register() {
        Dialog.instance.registry.registerCompose(ChoiceViewModel::class) { viewModel ->
            BasicText(
                text = viewModel.title,
                modifier = Modifier.clickable {
                    viewModel.notifier?.complete("accepted")
                },
            )
        }

        Loading.instance.registry.registerCompose(UploadLoadingViewModel::class) {
            BasicText("Uploading")
        }

        Toast.instance.registry.registerCompose(StatusToastViewModel::class) { viewModel ->
            BasicText(viewModel.message)
        }
    }
}
```

登録済みの content の表示は、factory がどちらの技術で書かれていても同じ `show` / `start` を通る。属性は composable の中で `KsDialogAttributes` を宣言して供給する。これも `jp.kamusoft:ksdialogs` に入っている。[レイアウト](layout.md) と [トランジション](transitions.md) を読む。

## 起動時に登録を呼ぶ

```kotlin
import android.app.Application

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        DialogHostRegistration.register()
    }
}
```

commonMain の `show` や `start` を呼ぶ前に 1 回登録する。登録は thread-safe で、同じ class を登録し直すとそのスロットの factory が置き換わる。アプリで Compose の content を使う場合は、同じ場所で `ComposeHostRegistration.register()` を呼ぶ。

## ViewModel factory を登録して型で表示する

レジストリのエントリは ViewModel class ごとに 2 つのスロットを持つ。どの経路でも使う View factory と、ライブラリ自身に ViewModel を作らせるための ViewModel factory である。両方のスロットが埋まっていれば、Android の UI コードは instance の代わりに class を渡せる。ここで埋めるのは Android Native のレジストリのスロットで、共有コードが持つ ViewModel factory の表とは別である ([ViewModel](view-models.md))。ライブラリは ViewModel を生成し、`configure` を完了させてから View factory を呼ぶので、content は `configure` が書いた状態を必ず読める。登録し直したスロットだけが置き換わり、解決は呼び出し時点のエントリのスナップショットで行うため、表示中に登録し直しても出ているものは変わらない。

`configure` が書き込む状態を共有 ViewModel に持たせる。

```kotlin
package com.example.shared

import jp.kamusoft.ksdialogs.kmp.LoadingProgressReceiver
import jp.kamusoft.ksdialogs.kmp.LoadingViewModel

class ImportLoadingViewModel : LoadingViewModel, LoadingProgressReceiver {
    var title = ""
    var progress = 0.0
        private set

    override fun onProgress(progress: Double) {
        this.progress = progress
    }
}
```

Android の起動経路で 2 つのスロットを埋め、class を渡して表示する。`Loading` にはスコープ形の `start(VM::class)` もある。Dialog は `Dialog.instance.show(VM::class)`、Toast は `Toast.instance.show(VM::class)` が同じ形になる。Loading では進捗の受け口の紐付けが `configure` の後・View factory の前に入るため、この経路で生成した ViewModel もインスタンスを渡したときと同じに進捗を受け取る。

```kotlin
import android.widget.TextView
import jp.kamusoft.ksdialogs.Loading

object ImportLoadingRegistration {
    fun register() {
        Loading.instance.registry.register(ImportLoadingViewModel::class) { viewModel ->
            TextView(this).apply { text = viewModel.title }
        }
        Loading.instance.registry.registerViewModel(ImportLoadingViewModel::class, ::ImportLoadingViewModel)
    }
}

suspend fun importLibrary(source: ImportSource): Int =
    Loading.instance.start(ImportLoadingViewModel::class, configure = { viewModel ->
        viewModel.title = "Importing"
    }) { report ->
        source.importAll(onProgress = report)
    }
```

Android Native の `start` にも、共有コードと同じ `actionThread` 引数 (`LoadingActionThread`) がある。Android Native の型指定 `start` では、ViewModel factory と `configure` は指定に関係なく UI スレッドで走る。`configure` は Dialog と Loading では中断関数として書け、Toast では `show` が fire-and-forget なので同期のみになる。DI コンテナが組み立てる ViewModel は ViewModel factory の中身として書く。ライブラリはコンテナを知らない。共有コードにも同じ形の `show(VM::class)` があるが、引く ViewModel factory の表は別なので、共有コードから表示するなら共有コードで登録する ([ViewModel](view-models.md))。iOS host の Swift 入口にはこの形が無い。

## 表示中のページを教える

content に `DialogLayoutArea.CURRENT_PAGE` を添付した Dialog は、表示中のページを基準に配置される ([レイアウト](layout.md))。Android には OS の概念としてのページが無く、ライブラリは自分ではページを探さないので、アプリが教える。教え方は 2 つあり、両方あれば上が優先される。どちらからも得られなければ `VISIBLE_AREA` と同じ結果になり、理由が警告ログ (タグ `KsDialogs`) に出る。

| 順 | 画面の作り | 書く名前 | 配布物 |
|---|---|---|---|
| 1 | Compose | `Modifier.markAsDialogCurrentPage()` | `jp.kamusoft:ksdialogs` |
| 2 | 従来 View | `DialogCurrentPage.provider` (`(() -> View?)?`) | `jp.kamusoft:ksdialogs-core` |

基準になるのは、教えた composable / View の矩形と可視領域の共通部分である。バーを含む画面全体ではなく、バーの内側の枠 (中身の領域) を教える。

### Compose の画面で印を付ける

中身の枠に 1 回付ける。付けた composable は画面に載っている間だけ候補になり、組み立てから外れると候補から外れる。候補が複数あるときは、入れ子なら内側、それ以外は最後に画面に載ったものが選ばれる。

```kotlin
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import jp.kamusoft.ksdialogs.compose.markAsDialogCurrentPage

@Composable
fun PageWithBottomBar(
    bottomBar: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .markAsDialogCurrentPage(),
        ) {
            content()
        }
        bottomBar()
    }
}
```

`Crossfade` や `AnimatedContent`、Navigation Compose のフェード遷移では、切り替えの間は去る画面の印も候補に残るため、その間に出した Dialog は去る画面を基準にしうる。

### 従来 View の画面で関数を登録する

表示中のページの View を返す関数を一度登録する。`null` を代入すると登録を解除する。関数は UI スレッドで、各表示の開始時と、表示中にウィンドウの寸法やシステムバーの幅が変わったときに呼ばれる。登録の差し替えは次の表示から効く。

```kotlin
import android.app.Activity
import android.os.Bundle
import android.widget.FrameLayout
import jp.kamusoft.ksdialogs.DialogCurrentPage

class MainActivity : Activity() {
    private lateinit var pageContainer: FrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        pageContainer = findViewById(R.id.page_container)
        DialogCurrentPage.provider = { pageContainer }
    }

    override fun onDestroy() {
        DialogCurrentPage.provider = null
        super.onDestroy()
    }
}
```

関数が `null` を返す・例外を投げる・返した View が Dialog を出す Activity のウィンドウに載っていない・矩形が空、のいずれかなら、ページは得られなかった扱いになる。同じ Activity で出したモーダル・ダイアログのウィンドウに載った View は候補にできる。

## 内蔵 Loading と Toast の見た目を設定する

色は共有コードの境界を渡らないため、内蔵の見た目は Android 側の入口で設定する。どちらの style も `data class` なので `copy(...)` で 1 項目ずつ差し替えられる。器は各表示の開始時にこれを読むので、変更は次の表示から効き、表示中のものには効かない。

```kotlin
Loading.instance.style = Loading.instance.style.copy(
    indicatorColor = Color.WHITE,
    messageFontSize = 16.0,
    messageColor = Color.WHITE,
    defaultMessage = "Working",
    progressFormat = LoadingStyle.DEFAULT_PROGRESS_FORMAT,
)
Loading.instance.options = DialogOptions(proportionalWidth = 0.5)

Toast.instance.style = Toast.instance.style.copy(
    backgroundColor = ToastStyle.BUILTIN_BACKGROUND_COLOR,
    textColor = Color.WHITE,
    fontSize = 16.0,
    cornerRadius = 20.0,
    defaultDuration = ToastStyle.BUILTIN_DEFAULT_DURATION,
    defaultPlacement = DialogPlacement(verticalAlignment = DialogAlignment.END, offsetY = -120.0),
)
```

`Loading.instance.options` は Dialog の添付で使うのと同じ `DialogOptions` を受け取り、内蔵 Loading content に添付の口が無い代わりになる。外側タップの項目は設定しても無効のままである。`defaultPlacement` は全 Toast に効くアプリ既定の配置で、アプリ自身のボトムバーを避けるための逃げ道になる。組み込みの Toast は自分の中身に全辺 24 の余白を持つので、`defaultPlacement` で動かしても画面の端には貼り付かない。内蔵 Loading と Toast の器は、表示中も提示先の Activity のシステムバーの指定 (アイコンの明暗・表示/非表示) を引き継いで変えない。`progressFormat` は書式文字列ではなく `(String?, Double?) -> String` の関数である。

## 構成ミスの失敗

`show` は `DialogResult.Completed` か `DialogResult.Cancelled` を返す。構成ミスでは結果を返さず投げ、型が種別を表す。

| 状況 | 例外 |
|---|---|
| その class の View factory が未登録 (Dialog / Loading / Toast のいずれも) | `DialogException.ViewFactoryNotRegistered` |
| 型指定 show で ViewModel factory が未登録 (Dialog / Loading / Toast のいずれも) | `DialogException.ViewModelFactoryNotRegistered` |
| 同じ ViewModel instance が既に表示中 | `DialogException.ViewModelAlreadyShowing` |
| value class を ViewModel にした | `DialogException.ValueClassViewModel` |

型指定 show で ViewModel factory や `configure` が投げた例外は、Dialog と Loading では表示に進まずに呼び出し元へ伝わり、型指定の `start` は処理を実行しない。Toast がこの形で返せるのは ViewModel factory の未登録だけである。`show` が既に戻っているため factory と `configure` の例外は呼び出し元へ返せず、警告を記録に残してその 1 枚だけが破棄される。

出す先の画面が無いことは失敗にならない。Android の提示先は resumed で、かつ描画された Activity で、それが現れるまで Dialog は待ち、Loading は処理を続けながら待ち、Toast は期限の範囲で待つ。起動画面の下で開いている途中の画面には出さない ([Dialog](dialogs.md) / [Loading](loading.md) / [Toast](toast.md))。

呼び出し元のコルーチンをキャンセルすると、画面を待っている間でも表示中でも `CancellationException` が伝播する。表示中の Dialog は退出の演出と撤去を最後まで完遂する。

## Android 専用の入口

次は Android の面にだけあり共有コードに対応物が無いので、Android 固有の UI でだけ使う:

| 入口 | 何ができるか |
|---|---|
| `show(viewModel, placement, factory)` と `showCompose(viewModel, placement, content)` | registry を変えずに content をその場で渡して表示する。`Loading` には `startCompose` もある |
| `SimpleDialogViewModel` | `DialogViewModel<Boolean>` の別名。共有コードでは型引数を明示して書く |
