# 共有コードから Loading を出す

Loading は処理の間だけ画面全体の操作をブロックする表示で、プロセス全体で 1 つしか出ない。同時に使われた分は 1 つの表示へ合流し、各処理はいずれも実行される。呼び出しは共有コード (`commonMain`) に書き、内蔵の見た目をそのまま使うか、各 host に登録した content を使う。

Loading はユーザー操作では閉じない。外側タップは閉じる契機にならず、背後の画面へも届かない。処理を中断できるようにしたい場合は、キャンセル操作を持つ Dialog を使う ([Dialog](dialogs.md))。Loading はまた、起動順によらずどの Dialog よりも、どの Toast よりも前面に出る。

## `start` と `show` を選ぶ

軸は 3 つで、表示の範囲を処理に合わせるか自分で開始と終了を書くか、内蔵の見た目を使うか登録した content を使うか、そして content を使う場合に ViewModel の instance を渡すか class だけを渡すかである。`Loading.instance` から呼んでも `KsLoading` として注入したものから呼んでも、同じ表示と同じ合流状態に届く ([ViewModel](view-models.md))。どれも `suspend` 関数で、任意のスレッドから呼べる。

| シグネチャ | 何をする | いつ選ぶ | 必要な登録 |
|---|---|---|---|
| `suspend fun <T> start(message: String? = null, placement: DialogPlacement? = null, action: suspend ((Double) -> Unit) -> T): T` | 内蔵 Loading を出したまま処理を実行し、戻り値を返す | 処理の範囲がそのまま表示の範囲になるとき | 不要 |
| `suspend fun <T> start(viewModel: LoadingViewModel, placement: DialogPlacement? = null, action: suspend ((Double) -> Unit) -> T): T` | 登録済みの content を出したまま処理を実行する | 同上で、見た目を自分で作るとき | その ViewModel class の content |
| `suspend fun <VM : LoadingViewModel, T> start(viewModelClass: KClass<VM>, placement: DialogPlacement? = null, configure: (suspend (VM) -> Unit)? = null, action: suspend ((Double) -> Unit) -> T): T` | 登録済みの ViewModel factory で instance を作り、`configure` の完了後に上と同じことをする | 同上で、組み立てをライブラリに任せるとき | 上に加えて共有コードの ViewModel factory |
| `suspend fun show(message: String? = null, placement: DialogPlacement? = null)` | 内蔵 Loading を出し、合流 1 件を開始する | 開始と終了が別の場所にあるとき | 不要 |
| `suspend fun show(viewModel: LoadingViewModel, placement: DialogPlacement? = null)` | 登録済みの content を出し、合流 1 件を開始する | 同上で、見た目を自分で作るとき | その ViewModel class の content |
| `suspend fun <VM : LoadingViewModel> show(viewModelClass: KClass<VM>, placement: DialogPlacement? = null, configure: (suspend (VM) -> Unit)? = null)` | 登録済みの ViewModel factory で instance を作り、`configure` の完了後に上と同じことをする | 同上で、組み立てをライブラリに任せるとき | 上に加えて共有コードの ViewModel factory |
| `suspend fun setMessage(message: String?)` | 表示中の内蔵 Loading の文言を差し替える | 処理の途中で状況を伝えるとき | 不要 |
| `suspend fun hide()` | 表示を閉じる | `show` で開始した表示を終えるとき | 不要 |

`start` に対応する終了は無い — 処理の完了が終了である。`show` に対応する終了は `hide` だけで、合流数によらず即座に閉じる。`message` を省略すると host 側で設定された既定メッセージになり、`placement` を省略すると契約の既定値になる ([レイアウト](layout.md))。class を渡す形は共有コードに登録した ViewModel factory が instance を作るもので、共有 Kotlin コード専用であり Swift からは見えない ([ViewModel](view-models.md))。Dialog と Toast の入口は [Dialog](dialogs.md) と [Toast](toast.md) にそれぞれの選択表がある。

## 処理の間だけ表示する

`start` は合流 1 件を開始し、処理を実行し、成功・失敗・キャンセルのいずれでもその 1 件を終了する。合流数を 0 にした呼び出しは、入力をブロックする器が撤去されてから戻る。

```kotlin
package com.example.shared

import jp.kamusoft.ksdialogs.kmp.KsLoading
import jp.kamusoft.ksdialogs.kmp.Loading
import kotlin.coroutines.cancellation.CancellationException

class ReportDownloader(
    private val client: ReportClient,
    private val loading: KsLoading = Loading.instance,
) {
    @Throws(CancellationException::class)
    suspend fun download(): ByteArray =
        loading.start(message = "Downloading") { report ->
            report(0.25)
            val data = client.fetch()
            report(1.0)
            data
        }
}
```

進捗の報告口は `(Double) -> Unit` の関数で、任意のスレッドから呼べる。`0`〜`1` の範囲外は丸められ、最新の報告が勝ち、逐次列を受け取る API は無い。表示できる画面が無い場合、表示は行われないが処理は実行され、`start` は通常どおり値を返す。

## 開始と終了を自分で書く

lifetime を別に制御する場合は `show`・`setMessage`・`hide` を使う。`show` は操作のブロックが有効になった時点で戻り、入りの演出の完了は待たない。`hide` は走行中の処理をキャンセルしない — 世代を終わらせるので、その後に始まった利用は新しい世代に属し、古い表示を閉じることも遅れて届いた進捗を受け取ることもない。`setMessage` は合流に関与せず、内蔵 Loading を表示している間だけ効く (非表示中とカスタム content の表示中は何も起こらない)。

```kotlin
package com.example.shared

import jp.kamusoft.ksdialogs.kmp.KsLoading
import jp.kamusoft.ksdialogs.kmp.Loading
import kotlin.coroutines.cancellation.CancellationException

class LibrarySynchronizer(
    private val client: ReportClient,
    private val loading: KsLoading = Loading.instance,
) {
    @Throws(CancellationException::class)
    suspend fun synchronize() {
        loading.show("Connecting")
        try {
            loading.setMessage("Synchronizing")
            client.synchronize()
        } finally {
            loading.hide()
        }
    }
}
```

## custom content で進捗を受け取る

`commonMain` で `LoadingViewModel` を実装した class を宣言し、content に進捗を追わせたい場合は `LoadingProgressReceiver` も実装する。その ViewModel を表示している間、ライブラリは報告を UI スレッド上で `onProgress` へ転送する。実装していなければ転送されない。

```kotlin
package com.example.shared

import jp.kamusoft.ksdialogs.kmp.LoadingProgressReceiver
import jp.kamusoft.ksdialogs.kmp.LoadingViewModel

class UploadLoadingViewModel : LoadingViewModel, LoadingProgressReceiver {
    var title = ""
    var progress = 0.0
        private set

    override fun onProgress(progress: Double) {
        this.progress = progress
    }
}
```

この class に対する content は、`start` や `show` を呼ぶより前に各 host の起動経路で 1 回登録する。Android は `Loading.instance.registry.register(UploadLoadingViewModel::class) { … }`、iOS は `Loading.shared.kmp.register(UploadLoadingViewModel.self) { … }` を使う。完動レシピは [Android host](android-host.md) と [iOS host](ios-host.md) にある。

```kotlin
package com.example.shared

import jp.kamusoft.ksdialogs.kmp.DialogException
import jp.kamusoft.ksdialogs.kmp.KsLoading
import jp.kamusoft.ksdialogs.kmp.Loading
import kotlin.coroutines.cancellation.CancellationException

class PhotoUploader(
    private val client: ReportClient,
    private val loading: KsLoading = Loading.instance,
) {
    @Throws(DialogException::class, CancellationException::class)
    suspend fun upload() {
        loading.start(UploadLoadingViewModel()) { report ->
            report(0.5)
            client.send()
            report(1.0)
        }
    }
}
```

## 各メソッドの最小例

選択表のうち `start(message)`・`start(viewModel)`・`show(message)`・`setMessage`・`hide` は上の節の例が最小例になっている。残る形をここに置く。

登録済み content を出して合流 1 件を開始する `show`。`start` と違って処理の範囲に紐づかないため、終了は `hide` を自分で書く。

```kotlin
loading.show(UploadLoadingViewModel())
```

`placement` を渡す形。`start` と `show` のどの入口にも同じ引数があり、この表示の配置を差し替える。

```kotlin
loading.show(message = "Downloading", placement = DialogPlacement(verticalAlignment = DialogAlignment.END))
```

```kotlin
loading.start(UploadLoadingViewModel(), DialogPlacement(offsetY = -100.0)) { report -> report(1.0) }
```

ViewModel の class を渡す `show`。共有コードに登録した ViewModel factory が instance を作る。`configure` は最後の引数なので、渡すなら末尾のラムダで書ける ([ViewModel](view-models.md))。

```kotlin
loading.show(UploadLoadingViewModel::class)
loading.show(UploadLoadingViewModel::class) { viewModel -> viewModel.title = "Uploading" }
```

class を渡すスコープ形。末尾に渡すのは `action` で、`configure` は名前付き引数で渡す。

```kotlin
loading.start(UploadLoadingViewModel::class) { report -> report(1.0) }
loading.start(
    UploadLoadingViewModel::class,
    configure = { viewModel -> viewModel.title = "Uploading" },
) { report -> report(1.0) }
```

## 構成ミスの失敗を扱う

内蔵 Loading の経路 (`show(message)`・`start(message)`・`setMessage`・`hide`) は構成ミスの例外を持たない。失敗するのは、登録済み content を使う ViewModel 経路だけである。

| 状況 | メッセージ | 対処 |
|---|---|---|
| その ViewModel class の content が host に未登録 | `No View factory is registered for ViewModel type {TypeName}.` | 起動時に各 host で登録する ([Android host](android-host.md) / [iOS host](ios-host.md)) |
| class を渡したが共有コードに ViewModel factory が未登録 | `No ViewModel factory is registered for ViewModel type {TypeName}.` | 起動時に `registry.registerViewModel` で登録する ([ViewModel](view-models.md)) |
| ViewModel factory が登録キーと違う class の instance を返した | `The registered ViewModel factory does not produce ViewModel type {TypeName}. It produced {TypeName} instead. A ViewModel factory must return a ViewModel of the same class as its registration key.` | factory の戻り値を登録キーと同じ class にする |

表のメッセージは現在の実装が返す値であり、安定した API ではない (変わらないのは例外型と throw される条件であり、文言は予告なく変わりうる)。

ViewModel factory と `configure` が投げた例外は `DialogException` に包まれず、そのまま呼び出し元へ伝わる。class を渡すスコープ形では `action` も実行されない。

未登録の class は処理を始める前に `DialogException` で失敗するので、表示も処理も行われない。合流に加わろうとした呼び出しも同じで、先に表示されている Loading は影響を受けない。iOS ではこのほかに、登録済み factory がその ViewModel を受け取れないときの `The registered View factory cannot accept ViewModel type {TypeName}.` と、host 側から結果もエラーも返らなかったときの `Failed to show the Loading.` がメッセージになる。

たとえば Android の起動経路が Dialog の content だけを登録し、`Loading.instance.registry` へ何も登録していないと、`UploadLoadingViewModel` を渡した `start` は content を解決できずに失敗する。

```kotlin
import android.widget.Button
import jp.kamusoft.ksdialogs.Dialog

object DialogHostRegistration {
    fun register() {
        Dialog.instance.registry.register(DeleteViewModel::class) { viewModel, notifier ->
            Button(this).apply {
                text = "Delete ${viewModel.itemName}"
                setOnClickListener { notifier.complete(true) }
            }
        }
    }
}
```

呼び出し元では `DialogException` を `catch` できる。構成ミスは実行時に直せるものではないので、ログに残したうえで再 throw する。`CancellationException` は別の型なのでこの `catch` には入らず、コルーチンのキャンセルはそのまま伝播する。

```kotlin
package com.example.shared

import jp.kamusoft.ksdialogs.kmp.DialogException
import jp.kamusoft.ksdialogs.kmp.KsLoading
import jp.kamusoft.ksdialogs.kmp.Loading
import kotlin.coroutines.cancellation.CancellationException

class PhotoUploader(
    private val client: ReportClient,
    private val loading: KsLoading = Loading.instance,
) {
    @Throws(DialogException::class, CancellationException::class)
    suspend fun upload() {
        try {
            loading.start(UploadLoadingViewModel()) { report ->
                report(0.5)
                client.send()
                report(1.0)
            }
        } catch (failure: DialogException) {
            println("Loading content is not registered: ${failure.message}")
            throw failure
        }
    }
}
```

## host 側に残るもの

共有コードが制御するのは message・配置・報告する進捗と、ViewModel の作り方である。見た目に関わるものは Native host 側にある — 内蔵インジケータの style、器の option、カスタム content の登録、演出の添付である。共有コードは content の型に触れないため、`Loading.instance.registry` が持つのは ViewModel factory の登録だけで、インライン factory の経路は無い。その経路は Android の面に Android 固有の UI 向けとしてある。[Android host](android-host.md) と [iOS host](ios-host.md) を読む。
