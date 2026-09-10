# Loading を表示する

Loading は処理中の操作をブロックする表示で、`KsLoading` (`Loading.instance` と DI で注入したもののどちらも同じ実体) から呼ぶ。組み込みの既定 Loading をそのまま出すか、自分で書いたコンテンツを ViewModel 型に紐付けて出すかの 2 通りがある。

同時に走る利用はプロセス単位の 1 つの表示へ合流し、コンテンツは最初の開始のものが維持される。Loading では外側タップによるキャンセルは無効のままである。

## `show` と `start` を選ぶ

`KsLoading` は表示を開いて自分で閉じる `show` と、表示の生存期間を処理に対応させる `start` を持ち、どちらも複数の overload を公開する。Compose 系 artifact `jp.kamusoft:ksdialogs` が拡張関数 `showCompose` と `startCompose` を加える。選ぶ軸は、コンテンツを組み込みの既定にするか自分で書くか、自分で書いたコンテンツを登録済みの factory に任せるかその場で渡すか、そして ViewModel をインスタンスで渡すか型だけ渡すかである。どの入口も省略可能な `placement` を取る ([レイアウト](layout.md))。

| シグネチャ | 何をする | いつ選ぶ | 必要な登録 |
|---|---|---|---|
| `show(message: String? = null, placement: DialogPlacement? = null)` | 組み込みの既定 Loading を表示し、合流を 1 件開始する | 文言だけ出して自分で閉じるとき | 不要 |
| `show(viewModel: LoadingViewModel, placement: DialogPlacement? = null)` | 登録済みの factory が作ったコンテンツを表示する | 見た目を自分で書き、繰り返し使うとき | View factory (`register` / `registerCompose`) |
| `show(viewModelClass: KClass<VM>, placement: DialogPlacement? = null, configure: (suspend (VM) -> Unit)? = null)` | ViewModel の型だけを渡し、登録済みの ViewModel factory が作ったインスタンスを `configure` してから表示する | ViewModel の生成をライブラリに任せ、表示の直前に状態を整えるとき | View factory + ViewModel factory (`registerViewModel`) |
| `show(viewModel: VM, placement: DialogPlacement? = null, factory: Context.(VM) -> View)` | ViewModel インスタンスと View factory の両方をその場で渡す。レジストリは読まず、変えもしない | 1 回だけ使う View コンテンツ | 不要 |
| `showCompose(viewModel: VM, placement: DialogPlacement? = null, content: @Composable (VM) -> Unit)` | ViewModel インスタンスと Compose コンテンツの両方をその場で渡す。レジストリは読まず、変えもしない | 1 回だけ使う Compose コンテンツ | 不要 (`jp.kamusoft.ksdialogs.compose` から import) |
| `start(message: String? = null, placement: DialogPlacement? = null, action: suspend ((Double) -> Unit) -> T)` | 既定 Loading を出したまま `action` を実行し、その戻り値を返す | 表示の開始と終了を処理に合わせるとき | 不要 |
| `start(viewModel: LoadingViewModel, placement: DialogPlacement? = null, action: suspend ((Double) -> Unit) -> T)` | 登録済みのコンテンツを出したまま `action` を実行する | 同上で、見た目を自分で書くとき | View factory |
| `start(viewModelClass: KClass<VM>, placement: DialogPlacement? = null, configure: (suspend (VM) -> Unit)? = null, action: suspend ((Double) -> Unit) -> T)` | 型から作らせた ViewModel のコンテンツを出したまま `action` を実行する | 同上で、ViewModel の生成もライブラリに任せるとき | View factory + ViewModel factory |
| `start(viewModel: VM, placement: DialogPlacement? = null, factory: Context.(VM) -> View, action: suspend ((Double) -> Unit) -> T)` | ViewModel・View factory・処理をその場で渡す | 1 回だけ使う View コンテンツで処理を包むとき | 不要 |
| `startCompose(viewModel: VM, placement: DialogPlacement? = null, content: @Composable (VM) -> Unit, action: suspend ((Double) -> Unit) -> T)` | ViewModel・Compose コンテンツ・処理をその場で渡す | 1 回だけ使う Compose コンテンツで処理を包むとき | 不要 (compose artifact) |
| `hide()` | 表示を閉じる。合流数によらず即座に閉じ、走行中の処理には干渉しない | `show` で開いた表示を閉じるとき | — |
| `setMessage(message: String?)` | 表示中のメッセージを差し替える | 既定 Loading の進み具合を伝えるとき | — |

`hide` に overload は無く、`start` で開いた表示は `action` の完了・失敗で自動的に閉じるため `hide` を呼ぶ必要はない。

以下の例では、「既定 Loading を表示して更新する」が 1 行目と 11・12 行目、「Loading のスコープ内で処理を実行する」が 6 行目、「登録して呼び出す」が 7 行目と 2 行目、「型から ViewModel を生成して表示する」が 3 行目と 8 行目、「登録せずにカスタムコンテンツを表示する」が 9 行目と 10 行目、および 4・5 行目に当たる。

## 既定 Loading を表示して更新する

命令形の制御には `show`、`setMessage`、`hide` を使う。`hide` は実行中の処理をキャンセルせずにその表示を閉じる。

```kotlin
import jp.kamusoft.ksdialogs.Loading

suspend fun synchronize() {
    Loading.instance.show("Connecting")
    try {
        Loading.instance.setMessage("Downloading")
        performSynchronization()
    } finally {
        Loading.instance.hide()
    }
}

suspend fun performSynchronization() {}
```

## Loading のスコープ内で処理を実行する

`start` を使い、表示の生存期間と suspend 処理を対応させる。報告した進捗は `0.0..1.0` に丸められる。別の利用と表示が合流しても処理は実行され、処理の値または失敗が呼び出し元へ返る。

```kotlin
import jp.kamusoft.ksdialogs.Loading

suspend fun download(): ByteArray =
    Loading.instance.start(message = "Downloading") { report ->
        report(0.25)
        val data = fetchData()
        report(1.0)
        data
    }

suspend fun fetchData(): ByteArray = byteArrayOf()
```

## 既定 Loading を設定する

次の表示が始まる前に `style` と `options` を設定する。器はどちらも各表示の開始時に読むため、表示中の変更は次の表示から効く。style は indicator と message、options は器の layout と overlay を制御する。`LoadingStyle` は data class なので `copy` で一部だけ差し替えられ、`progressFormat` の内蔵既定は `LoadingStyle.DEFAULT_PROGRESS_FORMAT` である。

```kotlin
import android.graphics.Color
import jp.kamusoft.ksdialogs.DialogEdgeInsets
import jp.kamusoft.ksdialogs.DialogOptions
import jp.kamusoft.ksdialogs.Loading
import jp.kamusoft.ksdialogs.LoadingStyle

fun configureLoading() {
    Loading.instance.style = LoadingStyle(
        indicatorColor = Color.CYAN,
        messageFontSize = 16.0,
        messageColor = Color.WHITE,
        defaultMessage = "Working",
        progressFormat = { message, progress ->
            if (progress == null) message.orEmpty() else "${message.orEmpty()} ${(progress * 100).toInt()}%"
        },
    )
    Loading.instance.options = DialogOptions(
        dialogMargin = DialogEdgeInsets(32.0),
        overlayColor = 0x80000000.toInt(),
    )
}
```

## ViewModel を宣言する

自分で書くコンテンツは class の `LoadingViewModel` を型キーにする。`start` が報告した進捗を UI へ届けたい ViewModel は `LoadingProgressReceiver` を実装し、受け取った値を state として公開する。実装しない ViewModel には進捗が転送されないだけで、表示そのものは変わらない。

```kotlin
import jp.kamusoft.ksdialogs.LoadingProgressReceiver
import jp.kamusoft.ksdialogs.LoadingViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class ProgressLoadingViewModel : LoadingViewModel, LoadingProgressReceiver {
    private val mutableProgress = MutableStateFlow(0.0)
    val progress = mutableProgress.asStateFlow()

    override fun onProgress(progress: Double) {
        mutableProgress.value = progress
    }
}
```

## コンテンツを書く

コンテンツは進捗の state を lifecycle-aware に読む。Compose では `collectAsStateWithLifecycle` を使う。

```kotlin
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ProgressLoadingContent(viewModel: ProgressLoadingViewModel) {
    val progress = viewModel.progress.collectAsStateWithLifecycle()
    CircularProgressIndicator(progress = { progress.value.toFloat() })
}
```

演出を差し替えられるのは自分で書いたコンテンツだけで、コンテンツへ `DialogTransition` を添付して指定する ([トランジション](transitions.md))。

## 登録して呼び出す

Loading のレジストリ (`Loading.instance.registry`。型は `LoadingViewRegistry`) は Dialog のものとは独立しているので、登録もこちらへ行う。起動時に 1 回だけ済ませる。

```kotlin
import android.app.Application
import jp.kamusoft.ksdialogs.Loading
import jp.kamusoft.ksdialogs.compose.registerCompose

class SampleApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Loading.instance.registry.registerCompose(ProgressLoadingViewModel::class) { viewModel ->
            ProgressLoadingContent(viewModel)
        }
    }
}
```

呼び出し側は `Loading.instance` を使うか、`KsLoading` として注入したものを使う。どちらもプロセス内の同じレジストリと同じ状態を指す。画面の ViewModel から呼ぶ場合は `viewModelScope` に載せる。

```kotlin
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import jp.kamusoft.ksdialogs.KsLoading
import kotlinx.coroutines.launch

class SyncScreenViewModel(private val loading: KsLoading) : ViewModel() {
    fun onSyncClicked() {
        viewModelScope.launch {
            loading.start(ProgressLoadingViewModel()) { report ->
                report(0.5)
                performSynchronization()
                report(1.0)
            }
        }
    }
}
```

処理を包まず命令形で開くなら、同じ登録を使う `show` に ViewModel インスタンスだけを渡す。閉じるのは `hide` である。

```kotlin
loading.show(ProgressLoadingViewModel())
```

## 型から ViewModel を生成して表示する

ViewModel の生成もライブラリに任せるなら、View factory と同じレジストリへ ViewModel factory も登録し、`show` / `start` にはクラス参照を渡す。引数なしのコンストラクタならその参照をそのまま factory にできる。再登録は触れたスロットだけを置き換えるので、View factory の登録はそのまま残る ([ViewModel](view-models.md))。

```kotlin
Loading.instance.registry.registerViewModel(ProgressLoadingViewModel::class, ::ProgressLoadingViewModel)
```

呼び出し側はインスタンスを組み立てず、型と `configure` を渡す。順序は「ViewModel の生成 → `configure` の完了 → 進捗の受け口の紐付け → コンテンツの生成 → 表示」に固定されているので、`configure` が入れた状態はコンテンツの初期化から読める。`configure` は `suspend` として書けるので、表示の前に初期状態を読み込むこともできる。

```kotlin
import jp.kamusoft.ksdialogs.Loading

suspend fun uploadWithTypedLoading(): Int =
    Loading.instance.start(
        ProgressLoadingViewModel::class,
        configure = { viewModel -> viewModel.onProgress(0.0) },
    ) { report ->
        report(1.0)
        1
    }
```

処理を包まず命令形で開くなら、同じ型を `show` に渡す。末尾の lambda が `configure` になる。

```kotlin
Loading.instance.show(ProgressLoadingViewModel::class) { viewModel -> viewModel.onProgress(0.0) }
```

`configure` が完了してから、インスタンスを渡す経路とまったく同じ合流判定に入る。`configure` の途中で別の利用が先に表示を確定していればこの呼び出しは合流側になり、生成した ViewModel は表示に使われず、コンテンツも作られない。ViewModel factory や `configure` が失敗した場合は、表示にも合流にも進まずに呼び出し元へ伝播し、`start` では処理も実行されない。

## 登録せずにカスタムコンテンツを表示する

一度だけ使うコンテンツは、View factory を `show` / `start` へ直接渡すか、Compose コンテンツなら `showCompose` / `startCompose` を呼ぶ。どちらもレジストリを読まず、変更もしない。factory lambda は Android の `Context` receiver を持つため、lambda 内の `this` はその `Context` である。

```kotlin
import android.widget.ProgressBar
import jp.kamusoft.ksdialogs.Loading
import jp.kamusoft.ksdialogs.LoadingViewModel

class InlineLoadingViewModel : LoadingViewModel

suspend fun importFile(): Int =
    Loading.instance.start(
        viewModel = InlineLoadingViewModel(),
        factory = { ProgressBar(this) },
    ) { report ->
        report(1.0)
        1
    }
```

```kotlin
import androidx.compose.material3.CircularProgressIndicator
import jp.kamusoft.ksdialogs.Loading
import jp.kamusoft.ksdialogs.LoadingViewModel
import jp.kamusoft.ksdialogs.compose.startCompose

class InlineComposeLoadingViewModel : LoadingViewModel

suspend fun exportFile(): Int =
    Loading.instance.startCompose(
        viewModel = InlineComposeLoadingViewModel(),
        content = { CircularProgressIndicator() },
    ) { report ->
        report(1.0)
        1
    }
```

処理を包まず命令形で開くなら、同じ ViewModel と factory を `show` へ渡す。末尾の lambda が `factory` になる。

```kotlin
Loading.instance.show(InlineLoadingViewModel()) { ProgressBar(this) }
```

Compose コンテンツも同じで、`showCompose` の末尾の lambda が `content` になる (`jp.kamusoft.ksdialogs.compose.showCompose` を import する)。

```kotlin
Loading.instance.showCompose(InlineComposeLoadingViewModel()) { CircularProgressIndicator() }
```

## 構成ミスの失敗を扱う

構成ミスは表示を出さずに `DialogException` を投げる。`start` では処理も実行されない (fail-fast)。組み込みの既定 Loading を出す `show` / `start` は登録もコンテンツも要らないため、これらの失敗はカスタムコンテンツの経路でだけ起こる。

| 例外 | メッセージ | 原因と対処 |
|---|---|---|
| `DialogException.ViewFactoryNotRegistered` | `No View factory is registered for ViewModel type {TypeName}.` | 登録経路と型指定経路の `show` / `start` に対応する factory がない。`Loading.instance.registry` へ `register` または `registerCompose` を呼ぶか、factory をその場で渡す経路に切り替える |
| `DialogException.ViewModelFactoryNotRegistered` | `No ViewModel factory is registered for ViewModel type {TypeName}.` | 型を渡す `show` / `start` に ViewModel factory がない。`registerViewModel` を呼ぶ |
| `DialogException.ValueClassViewModel` | `ViewModel type {TypeName} is a value class and cannot be used as a ViewModel.` | value class を `LoadingViewModel` にした。ViewModel を class にする。この検査は表示の時点だけでなく `register` / `registerCompose` / `registerViewModel` の時点でも走る |

表のメッセージは現在の実装が返す値であり、安定した API ではない (変わらないのは例外型と throw される条件であり、文言は予告なく変わりうる)。

どちらの例外も `viewModelTypeName` から対象の ViewModel 型名を読める。たとえば起動時の登録を書き忘れたまま `ProgressLoadingViewModel` で `start` を呼ぶと、`DialogException.ViewFactoryNotRegistered` になる。

```kotlin
import android.app.Application

class SampleApplication : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
```

呼び出し元では入れ子の例外型でそのまま `catch` できる。構成ミスは実行時に直せるものではないので、ログに残したうえで再 throw し、開発中に気づける形にする。

```kotlin
import android.util.Log
import jp.kamusoft.ksdialogs.DialogException
import jp.kamusoft.ksdialogs.Loading

suspend fun runCustomLoading() {
    try {
        Loading.instance.start(ProgressLoadingViewModel()) { report ->
            report(0.5)
        }
    } catch (exception: DialogException.ViewFactoryNotRegistered) {
        Log.e("MyApp", "Loading view factory is not registered for ${exception.viewModelTypeName}.")
        throw exception
    }
}
```
