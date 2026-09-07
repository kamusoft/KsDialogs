# Dialog を表示する

class の ViewModel に結果型を宣言し、表示ごとに新しいコンテンツを作る factory を登録してから `DialogResult` を待つ。値を伴う完了とキャンセルは `DialogResult.Completed` と `DialogResult.Cancelled` で分岐する。結果型が `Boolean` の ViewModel には別名の `SimpleDialogViewModel`、独自の結果型には `DialogViewModel<R>` を使う。

コンテンツは Jetpack Compose でも Android View でも書ける。登録先はどちらも同じ `DialogViewRegistry` で、`registerCompose` で入れても `register` で入れても `show` の呼び方は変わらない。

factory は show のたびに呼ばれ、その show の結果を完了またはキャンセルする `DialogNotifier<R>` を受け取る。ViewModel だけを受け取る factory を使う場合は、ViewModel 自身が拡張プロパティ `notifier` から報告する ([ViewModel](view-models.md))。最初の報告だけが結果を確定し、以後の報告は何もしない。

`Cancelled` になるのは、notifier のキャンセル報告、覆いへの外側タップ、戻るボタン、Dialog を載せていた画面の破棄である。

## `show` を選ぶ

`KsDialog` (`Dialog.instance` と DI で注入したもののどちらも同じ実体) は `show` を 3 つの overload で公開し、`ksdialogs-compose` artifact が拡張関数 `showCompose` を加える。選ぶ軸は 2 つで、ViewModel をインスタンスで渡すか型だけ渡すか、そしてコンテンツを登録済みの factory に任せるかその場で渡すかである。どの入口も省略可能な `placement` を取り、渡すとコンテンツに添付された配置をまるごと置換する ([レイアウト](layout.md))。戻り値は `DialogResult<R>` で、`R` は ViewModel の宣言から決まる。

| シグネチャ | 何をする | いつ選ぶ | 必要な登録 |
|---|---|---|---|
| `show(viewModel: DialogViewModel<R>, placement: DialogPlacement? = null)` | 作った ViewModel インスタンスを渡し、登録済みの factory がコンテンツを作る | 呼び出し元で ViewModel を組み立てる (コンストラクタ引数を渡す) とき | View factory (`register` / `registerCompose`) |
| `show(viewModel: VM, placement: DialogPlacement? = null, factory: Context.(VM, DialogNotifier<R>) -> View)` | ViewModel インスタンスと View factory の両方をその場で渡す。レジストリは読まず、変えもしない | 1 回だけ使う View コンテンツ | 不要 |
| `show(viewModelClass: KClass<VM>, placement: DialogPlacement? = null, configure: (suspend (VM) -> Unit)? = null)` | ViewModel の型だけを渡し、登録済みの ViewModel factory が作ったインスタンスを `configure` してから表示する | ViewModel の生成をライブラリに任せ、表示の直前に状態を整えるとき | View factory + ViewModel factory (`registerViewModel`。[ViewModel](view-models.md)) |
| `showCompose(viewModel: VM, placement: DialogPlacement? = null, content: @Composable (VM, DialogNotifier<R>) -> Unit)` | ViewModel インスタンスと Compose コンテンツの両方をその場で渡す。レジストリは読まず、変えもしない | 1 回だけ使う Compose コンテンツ | 不要 (`jp.kamusoft.ksdialogs.compose` から import) |

Dialog を閉じる入口は無く、`hide` に当たる API も無い。表示が終わるのは上に挙げた結果確定のときだけである。

以下の例では、「登録して呼び出す」の `show(ConfirmViewModel(...))` が 1 行目、「登録せずに View コンテンツを表示する」が 2 行目、「Boolean 以外の結果型を返す」の `show(ItemEditViewModel::class)` が 3 行目、「登録せずに Compose コンテンツを表示する」が 4 行目に当たる。

## ViewModel を宣言する

ViewModel は結果型を宣言し、表示に必要な値を持つ。結果の報告をコンテンツ側の `notifier` に任せるなら、ViewModel には値の宣言だけがあればよい。

```kotlin
import jp.kamusoft.ksdialogs.SimpleDialogViewModel

class ConfirmViewModel(val message: String) : SimpleDialogViewModel
```

## コンテンツを書く

覆いと配置はライブラリの器が受け持つため、コンテンツにはカード自体だけを書く。Compose では `@Composable` 関数として書く。

```kotlin
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import jp.kamusoft.ksdialogs.DialogNotifier

@Composable
fun ConfirmContent(viewModel: ConfirmViewModel, notifier: DialogNotifier<Boolean>) {
    Column {
        Text(viewModel.message)
        Button(onClick = { notifier.complete(true) }) { Text("OK") }
        Button(onClick = notifier::cancel) { Text("Cancel") }
    }
}
```

View で書くなら、`Context` から組み立てた `View` を返す。コンストラクタの形を `(Context, VM, DialogNotifier<R>)` にしておくと、登録の lambda がそのまま 1 行になる。

```kotlin
import android.content.Context
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import jp.kamusoft.ksdialogs.DialogNotifier

class ConfirmCardView(
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
        addView(
            Button(context).apply {
                text = "Cancel"
                setOnClickListener { notifier.cancel() }
            },
        )
    }
}
```

## 登録して呼び出す

登録は起動時に 1 回だけ済ませる。`Application.onCreate` なら、アプリ内のどこから show しても登録済みの状態になる。

```kotlin
import android.app.Application
import jp.kamusoft.ksdialogs.Dialog
import jp.kamusoft.ksdialogs.compose.registerCompose

class SampleApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Dialog.instance.registry.registerCompose(ConfirmViewModel::class) { viewModel, notifier ->
            ConfirmContent(viewModel, notifier)
        }
    }
}
```

View コンテンツなら、同じ場所で `registerCompose` の代わりに `register` を呼ぶ。

```kotlin
Dialog.instance.registry.register(ConfirmViewModel::class, ::ConfirmCardView)
```

呼び出し側は `Dialog.instance` を使うか、`KsDialog` として注入したものを使う。注入する実体は `Dialog.instance` でも新しい `Dialog()` でもよく、どちらもプロセス内の同じレジストリと同じ状態を指す。`show` は suspend 関数なので、Activity からは `lifecycleScope` で呼ぶ。

```kotlin
import android.os.Bundle
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import jp.kamusoft.ksdialogs.Dialog
import jp.kamusoft.ksdialogs.DialogResult
import kotlinx.coroutines.launch

class ItemActivity : ComponentActivity() {
    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        statusText = TextView(this)
        setContentView(statusText)
    }

    fun onDeleteClicked() {
        lifecycleScope.launch {
            val result = Dialog.instance.show(ConfirmViewModel("Delete this item?"))
            statusText.text = when {
                result is DialogResult.Completed && result.value -> "Deleted"
                else -> "Kept"
            }
        }
    }
}
```

## Boolean 以外の結果型を返す

`DialogViewModel<R>` の `R` には data class でも `String` でも任意の型を置ける。`notifier` と `DialogResult` の型はその宣言から決まるので、別の型で報告するコードはコンパイルできない。

View factory と ViewModel factory の両方を登録しておくと、ViewModel の型だけを渡す `show` が使える。生成した ViewModel は `configure` で整えてから表示される。

```kotlin
import jp.kamusoft.ksdialogs.DialogViewModel
import jp.kamusoft.ksdialogs.notifier

data class ItemEdit(val name: String, val quantity: Int)

class ItemEditViewModel : DialogViewModel<ItemEdit> {
    var name = ""
    var quantity = 0

    fun save() {
        notifier?.complete(ItemEdit(name, quantity))
    }

    fun cancel() {
        notifier?.cancel()
    }
}
```

コンテンツは `ItemEditViewModel` だけを受け取る形で登録し、報告は ViewModel の `save` / `cancel` に任せる。`ItemEditContent` は `ConfirmContent` と同じ形で利用者が書く composable である。

```kotlin
Dialog.instance.registry.registerCompose(ItemEditViewModel::class) { viewModel ->
    ItemEditContent(viewModel)
}
Dialog.instance.registry.registerViewModel(ItemEditViewModel::class, ::ItemEditViewModel)
```

Composable の画面から呼ぶ場合は、`KsDialog` を受け取る画面の ViewModel に呼び出しを置き、結果を state として公開する。

```kotlin
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import jp.kamusoft.ksdialogs.DialogResult
import jp.kamusoft.ksdialogs.KsDialog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ItemScreenViewModel(private val dialogs: KsDialog) : ViewModel() {
    private val mutableStatus = MutableStateFlow("")
    val status = mutableStatus.asStateFlow()

    fun onEditClicked() {
        viewModelScope.launch {
            val result = dialogs.show(ItemEditViewModel::class) { viewModel ->
                viewModel.name = "Apple"
            }
            mutableStatus.value = when (result) {
                is DialogResult.Completed -> "${result.value.name} x${result.value.quantity}"
                DialogResult.Cancelled -> "Cancelled"
            }
        }
    }
}
```

## 登録せずに Compose コンテンツを表示する

一度だけ使う Dialog では Compose コンテンツを `showCompose` へ直接渡す。この経路は同じ ViewModel 型の登録があっても使わず、レジストリのエントリを追加・置換・削除しない。

```kotlin
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import jp.kamusoft.ksdialogs.Dialog
import jp.kamusoft.ksdialogs.DialogResult
import jp.kamusoft.ksdialogs.SimpleDialogViewModel
import jp.kamusoft.ksdialogs.compose.showCompose

class InlineConfirmViewModel(val label: String) : SimpleDialogViewModel

suspend fun showInlineConfirm(): DialogResult<Boolean> =
    Dialog.instance.showCompose(InlineConfirmViewModel("Continue")) { viewModel, notifier ->
        Button(onClick = { notifier.complete(true) }) {
            Text(viewModel.label)
        }
    }
```

## 登録せずに View コンテンツを表示する

一度だけ使うコンテンツは View factory を `show` へ直接渡す。factory lambda は Android の `Context` receiver を持つため、lambda 内の `this` はその `Context` である。以降の View factory レシピも同じ receiver を使う。この経路はレジストリのエントリを追加・置換・削除しない。

```kotlin
import android.widget.Button
import jp.kamusoft.ksdialogs.Dialog
import jp.kamusoft.ksdialogs.DialogResult
import jp.kamusoft.ksdialogs.SimpleDialogViewModel

class NoticeViewModel(val text: String) : SimpleDialogViewModel

suspend fun showNotice(): DialogResult<Boolean> =
    Dialog.instance.show(NoticeViewModel("Ready")) { viewModel, notifier ->
        Button(this).apply {
            text = viewModel.text
            setOnClickListener { notifier.complete(true) }
        }
    }
```

## 独立した Dialog を重ねる

別々の ViewModel インスタンスで別々の `show` を開始する。後から表示した Dialog が前面に出て各呼び出しは自身の結果を保つ。Android では下の Dialog へ先に結果を報告すると下だけが閉じ、上の Dialog はそのまま操作できる。これはアプリ側が下の報告口を保持して先に報告した場合にだけ起きる — ユーザー操作 (完了・キャンセル・外側タップ・戻るボタン) は常に手前の 1 枚にしか届かない。

```kotlin
import jp.kamusoft.ksdialogs.Dialog
import jp.kamusoft.ksdialogs.DialogResult
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

suspend fun showTwoDialogs(): Pair<DialogResult<Boolean>, DialogResult<Boolean>> = coroutineScope {
    val first = async { Dialog.instance.show(ConfirmViewModel("First")) }
    val second = async { Dialog.instance.show(ConfirmViewModel("Second")) }
    first.await() to second.await()
}
```

## 構成ミスの失敗を扱う

構成ミスは `Cancelled` を返さず、入れ子クラスの `DialogException` で `show` を失敗させる。登録漏れを利用者のキャンセルと取り違えないためであり、この場合コンテンツは生成も表示もされない。呼び出し元の coroutine をキャンセルした場合はこれとは別で、`CancellationException` が伝播し、退出の演出と器の撤去は最後まで完遂される。

`PresentationHostUnavailable` 以外の例外は、対象の ViewModel 型名を `viewModelTypeName` から読める。

| 例外 | メッセージ | 原因と対処 |
|---|---|---|
| `DialogException.ViewFactoryNotRegistered` | `No View factory is registered for ViewModel type {TypeName}.` | その ViewModel 型のコンテンツを解決できない。`Dialog.instance.registry` へ `register` または `registerCompose` を呼ぶ |
| `DialogException.ViewModelFactoryNotRegistered` | `No ViewModel factory is registered for ViewModel type {TypeName}.` | 型を渡す `show` に ViewModel factory がない。`registerViewModel` を呼ぶ ([ViewModel](view-models.md)) |
| `DialogException.ViewModelAlreadyShowing` | `This ViewModel instance of type {TypeName} is already being shown.` | 同じ ViewModel インスタンスを既に表示している。重ねる show ごとに新しいインスタンスを作る |
| `DialogException.ValueClassViewModel` | `ViewModel type {TypeName} is a value class and cannot be used as a ViewModel.` | value class を ViewModel にした。ViewModel を class にする |
| `DialogException.PresentationHostUnavailable` | `No screen is available to present the Dialog.` | resumed な Activity がない。最初の画面が resumed になってから show する。キューイングはせず即座に失敗する |

表のメッセージは現在の実装が返す値であり、安定した API ではない (変わらないのは例外型と throw される条件であり、文言は予告なく変わりうる)。

たとえば起動時の登録を書き忘れたまま `ConfirmViewModel` を show すると、コンテンツを解決できず `DialogException.ViewFactoryNotRegistered` になる。

```kotlin
import android.app.Application

class SampleApplication : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
```

呼び出し元では入れ子の例外型でそのまま `catch` でき、`viewModelTypeName` から解決できなかった ViewModel の型名を読める。構成ミスは実行時に直せるものではないので、ログに残したうえで再 throw し、開発中に気づける形にする。

```kotlin
import android.util.Log
import jp.kamusoft.ksdialogs.Dialog
import jp.kamusoft.ksdialogs.DialogException
import jp.kamusoft.ksdialogs.DialogResult

suspend fun confirmDeletion(): DialogResult<Boolean> =
    try {
        Dialog.instance.show(ConfirmViewModel("Delete this item?"))
    } catch (exception: DialogException.ViewFactoryNotRegistered) {
        Log.e("MyApp", "Dialog view factory is not registered for ${exception.viewModelTypeName}.")
        throw exception
    }
```

`viewModelTypeName` を持たない `PresentationHostUnavailable` も含めてまとめて扱いたい場合は、基底型の `DialogException` で `catch` する。
