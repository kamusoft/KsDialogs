# Toast を表示する

Toast は結果を返さない fire-and-forget の通知で、`KsToast` (`Toast.instance` と DI で注入したもののどちらも同じ実体) から呼ぶ。組み込みのメッセージ Toast をそのまま出すか、自分で書いたコンテンツを ViewModel 型に紐付けて出すかの 2 通りがある。

受理された各表示は指定された duration と placement、自身の timer を持ち、他の Toast と重ねられる。Toast はタッチを奪わず、覆いの色や外側タップの受け口も持たない。Loading と同時に見えるときは Loading が前面に残る。

## `show` を選ぶ

`KsToast` は `show` を複数の overload で公開し、Compose 系 artifact `jp.kamusoft:ksdialogs` が拡張関数 `showCompose` を加える。選ぶ軸は、コンテンツを組み込みのメッセージ Toast にするか自分で書くか、自分で書いたコンテンツを登録済みの factory に任せるかその場で渡すか、そして ViewModel をインスタンスで渡すか型だけ渡すかである。どの入口も省略可能な `durationMs` と `placement` を取る。

| シグネチャ | 何をする | いつ選ぶ | 必要な登録 |
|---|---|---|---|
| `show(message: String, durationMs: Int? = null, placement: DialogPlacement? = null)` | 組み込みのメッセージ Toast に文言を載せて表示する | 文言だけ伝えるとき | 不要 |
| `show(viewModel: ToastViewModel, durationMs: Int? = null, placement: DialogPlacement? = null)` | 登録済みの factory が作ったコンテンツを表示する | 見た目を自分で書き、繰り返し使うとき | View factory (`register` / `registerCompose`) |
| `show(viewModelClass: KClass<VM>, durationMs: Int? = null, placement: DialogPlacement? = null, configure: ((VM) -> Unit)? = null)` | ViewModel の型だけを渡し、登録済みの ViewModel factory が作ったインスタンスを `configure` してから表示する | ViewModel の生成をライブラリに任せ、表示する値を呼び出しごとに入れるとき | View factory + ViewModel factory (`registerViewModel`) |
| `show(viewModel: VM, durationMs: Int? = null, placement: DialogPlacement? = null, factory: Context.(VM) -> View)` | ViewModel インスタンスと View factory の両方をその場で渡す。レジストリは読まず、変えもしない | 1 回だけ使う View コンテンツ | 不要 |
| `showCompose(viewModel: VM, durationMs: Int? = null, placement: DialogPlacement? = null, content: @Composable (VM) -> Unit)` | ViewModel インスタンスと Compose コンテンツの両方をその場で渡す。レジストリは読まず、変えもしない | 1 回だけ使う Compose コンテンツ | 不要 (`jp.kamusoft.ksdialogs.compose` から import) |

Toast は結果も `hide` も持たない。表示は duration の経過で自動的に消える。`configure` は同期の関数だけで、`suspend` としては書けない (`show` 自体が戻り値を持たない同期の呼び出しであるため)。

以下の例では、「組み込みのメッセージ Toast を表示する」が 1 行目、「登録して呼び出す」が 2 行目、「型から ViewModel を生成して表示する」が 3 行目、「登録せずにカスタムコンテンツを表示する」が 4 行目と 5 行目に当たる。

## 組み込みのメッセージ Toast を表示する

`durationMs` はミリ秒で、省略するか 0 以下を渡すと `ToastStyle.defaultDuration` を使い、それも 0 以下なら `ToastStyle.BUILTIN_DEFAULT_DURATION` (1500) へ丸める (0 以下を渡した場合と `defaultDuration` が 0 以下の場合は警告ログを残す。省略した場合は残さない)。placement を省略するとアプリ既定、さらに無ければライブラリ既定 (可視領域の下部中央から上方向へ論理単位 80) に配置される。

```kotlin
import jp.kamusoft.ksdialogs.DialogAlignment
import jp.kamusoft.ksdialogs.DialogPlacement
import jp.kamusoft.ksdialogs.Toast

fun notifySaved() {
    Toast.instance.show(
        message = "Saved",
        durationMs = 2000,
        placement = DialogPlacement(
            verticalAlignment = DialogAlignment.END,
            offsetY = -100.0,
        ),
    )
}
```

## Toast の既定値を設定する

次の表示が受理される前に `ToastStyle` を設定する。見た目のフィールドは組み込みのメッセージ Toast に効き、`defaultDuration` と `defaultPlacement` は自分で書いたコンテンツにも既定値を供給する。`ToastStyle` は data class なので `copy` で一部だけ差し替えられ、内蔵値は `ToastStyle.BUILTIN_BACKGROUND_COLOR` と `ToastStyle.BUILTIN_DEFAULT_DURATION` で参照できる。渡せる器の属性は placement だけである。

```kotlin
import android.graphics.Color
import jp.kamusoft.ksdialogs.DialogAlignment
import jp.kamusoft.ksdialogs.DialogPlacement
import jp.kamusoft.ksdialogs.Toast
import jp.kamusoft.ksdialogs.ToastStyle

fun configureToast() {
    Toast.instance.style = ToastStyle(
        backgroundColor = Color.DKGRAY,
        textColor = Color.WHITE,
        fontSize = 16.0,
        cornerRadius = 22.0,
        defaultDuration = ToastStyle.BUILTIN_DEFAULT_DURATION,
        defaultPlacement = DialogPlacement(
            verticalAlignment = DialogAlignment.END,
            offsetY = -120.0,
        ),
    )
}
```

## ViewModel を宣言する

自分で書くコンテンツは class の `ToastViewModel` を型キーにする。Toast は結果も進捗も持たないため、ViewModel が担うのは表示する値の運搬とレジストリの型キーだけである。

```kotlin
import jp.kamusoft.ksdialogs.ToastViewModel

class StatusToastViewModel(val message: String) : ToastViewModel
```

## コンテンツを書く

コンテンツには通知の見た目だけを書く。演出を差し替えるときは `DialogTransition` を添付する ([トランジション](transitions.md))。

```kotlin
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import jp.kamusoft.ksdialogs.DialogTransition
import jp.kamusoft.ksdialogs.compose.KsDialogAttributes

@Composable
fun StatusToastContent(viewModel: StatusToastViewModel) {
    KsDialogAttributes(transition = DialogTransition.fade())
    Text(viewModel.message)
}
```

## 登録して呼び出す

Toast のレジストリ (`Toast.instance.registry`。型は `ToastViewRegistry`) は Dialog / Loading のものとは独立しているので、登録もこちらへ行う。起動時に 1 回だけ済ませる。

```kotlin
import android.app.Application
import jp.kamusoft.ksdialogs.Toast
import jp.kamusoft.ksdialogs.compose.registerCompose

class SampleApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Toast.instance.registry.registerCompose(StatusToastViewModel::class) { viewModel ->
            StatusToastContent(viewModel)
        }
    }
}
```

呼び出し側は `Toast.instance` を使うか、`KsToast` として注入したものを使う。どちらもプロセス内の同じレジストリと同じ状態を指す。`show` は suspend 関数ではないので、Activity でも画面の ViewModel でもそのまま呼べる。

```kotlin
import androidx.lifecycle.ViewModel
import jp.kamusoft.ksdialogs.KsToast

class SyncScreenViewModel(private val toast: KsToast) : ViewModel() {
    fun onSynchronized() {
        toast.show(StatusToastViewModel("Synchronized"), durationMs = 1800)
    }
}
```

## 型から ViewModel を生成して表示する

呼び出しごとに載せる値だけが変わるコンテンツでは、ViewModel の生成もライブラリに任せられる。型キーの ViewModel には引数なしのコンストラクタを持たせ、値は `configure` で入れる。

```kotlin
import jp.kamusoft.ksdialogs.ToastViewModel

class NoticeToastViewModel : ToastViewModel {
    var message = ""
}
```

View factory と ViewModel factory は同じレジストリの別々のスロットに入る。再登録は触れたスロットだけを置き換えるので、もう片方は残る ([ViewModel](view-models.md))。

```kotlin
import android.app.Application
import androidx.compose.material3.Text
import jp.kamusoft.ksdialogs.Toast
import jp.kamusoft.ksdialogs.compose.registerCompose

class NoticeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Toast.instance.registry.registerCompose(NoticeToastViewModel::class) { viewModel ->
            Text(viewModel.message)
        }
        Toast.instance.registry.registerViewModel(NoticeToastViewModel::class, ::NoticeToastViewModel)
    }
}
```

呼び出し側はインスタンスを組み立てず、型と `configure` を渡す。順序は「ViewModel の生成 → `configure` の完了 → コンテンツの生成 → 表示」に固定されているので、`configure` が入れた値はコンテンツの初期化から読める。解決は `show` を呼んだ時点のスナップショットで、表示中の再登録は出ている Toast を変えない。

```kotlin
import jp.kamusoft.ksdialogs.Toast

fun notifySynchronized() {
    Toast.instance.show(NoticeToastViewModel::class, durationMs = 2000) { viewModel ->
        viewModel.message = "Synchronized"
    }
}
```

Android では ViewModel の生成も `configure` も、コンテンツの生成と同じく提示先 (resumed な Activity) を確保してから走る。提示先が現れないまま duration が満了した表示では、どちらも呼ばれないまま破棄される。

## 登録せずにカスタムコンテンツを表示する

一度だけ使うコンテンツは View factory を `show` へ直接渡すか、Compose コンテンツなら `showCompose` を呼ぶ。どちらもレジストリを読まず、変更もしない。factory lambda は Android の `Context` receiver を持つため、lambda 内の `this` はその `Context` である。

```kotlin
import android.widget.TextView
import jp.kamusoft.ksdialogs.DialogAlignment
import jp.kamusoft.ksdialogs.DialogPlacement
import jp.kamusoft.ksdialogs.Toast
import jp.kamusoft.ksdialogs.ToastViewModel

class InlineToastViewModel(val message: String) : ToastViewModel

fun notifyInline() {
    Toast.instance.show(
        viewModel = InlineToastViewModel("Uploaded"),
        durationMs = 1200,
        placement = DialogPlacement(verticalAlignment = DialogAlignment.START),
    ) { viewModel ->
        TextView(this).apply { text = viewModel.message }
    }
}
```

```kotlin
import androidx.compose.material3.Text
import jp.kamusoft.ksdialogs.DialogAlignment
import jp.kamusoft.ksdialogs.DialogPlacement
import jp.kamusoft.ksdialogs.Toast
import jp.kamusoft.ksdialogs.ToastViewModel
import jp.kamusoft.ksdialogs.compose.showCompose

class InlineComposeToastViewModel(val message: String) : ToastViewModel

fun notifyInlineCompose() {
    Toast.instance.showCompose(
        viewModel = InlineComposeToastViewModel("Uploaded"),
        durationMs = 1200,
        placement = DialogPlacement(verticalAlignment = DialogAlignment.START),
    ) { viewModel ->
        Text(viewModel.message)
    }
}
```

## 構成ミスの失敗を扱う

構成ミスは表示を出さず、呼び出し時点で同期に `DialogException` を投げる。組み込みのメッセージ Toast を出す `show` は登録もコンテンツも要らないため、これらの失敗は自分で書いたコンテンツの経路でだけ起こる。受理された後に content factory が失敗した場合はこれとは別で、ログに残したうえでその表示だけが破棄される。型指定経路の ViewModel factory と `configure` が投げた例外も同じ扱いで、`show` は既に戻っているため呼び出し元へは返らない。

| 例外 | メッセージ | 原因と対処 |
|---|---|---|
| `DialogException.ViewFactoryNotRegistered` | `No View factory is registered for ViewModel type {TypeName}.` | 登録経路と型指定経路の `show` に対応する factory がない。`Toast.instance.registry` へ `register` または `registerCompose` を呼ぶか、factory をその場で渡す経路に切り替える |
| `DialogException.ViewModelFactoryNotRegistered` | `No ViewModel factory is registered for ViewModel type {TypeName}.` | 型を渡す `show` に ViewModel factory がない。`registerViewModel` を呼ぶ |
| `DialogException.ValueClassViewModel` | `ViewModel type {TypeName} is a value class and cannot be used as a ViewModel.` | value class を `ToastViewModel` にした。ViewModel を class にする。この検査は表示の時点だけでなく `register` / `registerCompose` / `registerViewModel` の時点でも走る |

表のメッセージは現在の実装が返す値であり、安定した API ではない (変わらないのは例外型と throw される条件であり、文言は予告なく変わりうる)。

どちらの例外も `viewModelTypeName` から対象の ViewModel 型名を読める。たとえば起動時の登録を書き忘れたまま `StatusToastViewModel` を show すると、`DialogException.ViewFactoryNotRegistered` になる。

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
import jp.kamusoft.ksdialogs.Toast

fun notifySynchronization() {
    try {
        Toast.instance.show(StatusToastViewModel("Synchronized"), durationMs = 1800)
    } catch (exception: DialogException.ViewFactoryNotRegistered) {
        Log.e("MyApp", "Toast view factory is not registered for ${exception.viewModelTypeName}.")
        throw exception
    }
}
```
