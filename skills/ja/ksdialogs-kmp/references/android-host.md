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

KMP artifact は Compose 用の extension を引き込まない。Android アプリへ追加したうえで、各 registry の `registerCompose` overload を使う。`register` と別名なのは、`@Composable` 付きの関数型と通常の関数型を同名で並べると呼び出し側の型推論が曖昧になるためである。

```kotlin
dependencies {
    implementation("jp.kamusoft:ksdialogs-compose:<version>")
    implementation("androidx.compose.foundation:foundation:1.8.1")
}
```

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

登録済みの content の表示は、factory がどちらの技術で書かれていても同じ `show` / `start` を通る。属性は composable の中で `KsDialogAttributes` を宣言して供給する。これも `ksdialogs-compose` に入っている。[レイアウト](layout.md) と [トランジション](transitions.md) を読む。

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

`configure` は Dialog と Loading では中断関数として書け、Toast では `show` が fire-and-forget なので同期のみになる。DI コンテナが組み立てる ViewModel は ViewModel factory の中身として書く。ライブラリはコンテナを知らない。共有コードにも同じ形の `show(VM::class)` があるが、引く ViewModel factory の表は別なので、共有コードから表示するなら共有コードで登録する ([ViewModel](view-models.md))。iOS host の Swift 入口にはこの形が無い。

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

`Loading.instance.options` は Dialog の添付で使うのと同じ `DialogOptions` を受け取り、内蔵 Loading content に添付の口が無い代わりになる。外側タップの項目は設定しても無効のままである。`defaultPlacement` は全 Toast に効くアプリ既定の配置で、アプリ自身のボトムバーを避けるための逃げ道になる。`progressFormat` は書式文字列ではなく `(String?, Double?) -> String` の関数である。

## 構成ミスの失敗

`show` は `DialogResult.Completed` か `DialogResult.Cancelled` を返す。構成ミスでは結果を返さず投げ、型が種別を表す。

| 状況 | 例外 |
|---|---|
| その class の View factory が未登録 (Dialog / Loading / Toast のいずれも) | `DialogException.ViewFactoryNotRegistered` |
| 提示できる画面が無い | `DialogException.PresentationHostUnavailable` |
| 型指定 show で ViewModel factory が未登録 (Dialog / Loading / Toast のいずれも) | `DialogException.ViewModelFactoryNotRegistered` |
| 同じ ViewModel instance が既に表示中 | `DialogException.ViewModelAlreadyShowing` |
| value class を ViewModel にした | `DialogException.ValueClassViewModel` |

型指定 show で ViewModel factory や `configure` が投げた例外は、Dialog と Loading では表示に進まずに呼び出し元へ伝わり、型指定の `start` は処理を実行しない。Toast がこの形で返せるのは ViewModel factory の未登録だけである。`show` が既に戻っているため factory と `configure` の例外は呼び出し元へ返せず、警告を記録に残してその 1 枚だけが破棄される。

呼び出し元のコルーチンをキャンセルすると `CancellationException` が伝播する。Dialog は退出の演出と撤去を最後まで完遂する。

## Android 専用の入口

次は Android の面にだけあり共有コードに対応物が無いので、Android 固有の UI でだけ使う:

| 入口 | 何ができるか |
|---|---|
| `show(viewModel, placement, factory)` と `showCompose(viewModel, placement, content)` | registry を変えずに content をその場で渡して表示する。`Loading` には `startCompose` もある |
| `SimpleDialogViewModel` | `DialogViewModel<Boolean>` の別名。共有コードでは型引数を明示して書く |
