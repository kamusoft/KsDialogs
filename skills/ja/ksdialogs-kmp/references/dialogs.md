# 共有コードから Dialog を表示する

Dialog の呼び出しは共有コード (`commonMain`) に書き、中身の View は各 host が登録する。共有コードに書くのは 3 つ — 結果型を宣言した ViewModel の class、`show` の呼び出し、返ってきた `DialogResult` の分岐である。

共有コードには、Native にある真偽値の省略形 (結果型を書かない ViewModel の別名) が無い。ViewModel の宣言では結果型を常に明示し、真偽値なら `DialogViewModel<Boolean>` と書く。呼び出し側では結果型を書かない — `show` が返す `DialogResult<R>` の型は ViewModel の宣言から決まる。

## 結果とキャンセルの返り方

`DialogResult` は sealed interface で、`Completed` は値を持ち `Cancelled` は値を持たない。1 回の `show` につきどちらかがちょうど 1 回返る。

`show` は任意のスレッドから呼べ、提示先の指定も要らない。出す先の画面がまだ無ければ、失敗せずに画面が現れるのを待ってから表示する (後述の「出す先の画面がまだ無いときに呼ぶ」)。戻るのは退出の演出が終わって器が撤去された後なので、結果が届いた時点で Dialog はもう画面にない — 演出を何も添付していなくても、内蔵のクロスフェードの分だけ戻りが遅れる。

| 状況 | 共有コードから見えるもの |
|---|---|
| host の content が完了を報告した | 値を持つ `DialogResult.Completed` |
| content からのキャンセル・Dialog の外側のタップ・Android の戻るボタン | `DialogResult.Cancelled` |
| 報告の前に OS が器を外した、または中身を作った後に OS が器を載せられなかった | `DialogResult.Cancelled` |
| 呼び出し元のコルーチンをキャンセルした (画面を待っている間も表示中も) | `CancellationException`。Dialog は閉じ、内部では cancelled で確定する |
| ViewModel class が未登録・同じ instance が表示中または画面を待っている | `DialogException` |

外側タップでのキャンセルは既定で有効で、無効にしたい場合は host 側で添付する option に書く ([レイアウト](layout.md))。

## `show` を選ぶ

軸は 1 つで、ViewModel の instance を自分で作って渡すか、class だけを渡して生成をライブラリに任せるかである。`Dialog.instance` から呼んでも `KsDialog` として注入したものから呼んでも、同じレジストリと同じ表示に届く。

| シグネチャ | 何をする | いつ選ぶ | 必要な登録 |
|---|---|---|---|
| `suspend fun <R> show(viewModel: DialogViewModel<R>, placement: DialogPlacement? = null): DialogResult<R>` | 登録済みの content を表示し、結果が確定するまで待つ | 呼び出し側が ViewModel を組み立てるとき | その ViewModel class の content ([Android host](android-host.md) / [iOS host](ios-host.md)) |
| `suspend fun <R, VM : DialogViewModel<R>> show(viewModelClass: KClass<VM>, placement: DialogPlacement? = null, configure: (suspend (VM) -> Unit)? = null): DialogResult<R>` | 登録済みの ViewModel factory で instance を作り、`configure` の完了後に表示する | 組み立てをライブラリに任せるとき | 上に加えて共有コードの ViewModel factory ([ViewModel](view-models.md)) |

`placement` はこの呼び出しだけの置き場所で、渡すと content に添付された配置をまるごと置換する ([レイアウト](layout.md))。インライン factory の `show` は共有コードに無く、Native host 側の面にある。class を渡す `show` は共有 Kotlin コード専用で、Swift からは見えない ([iOS host](ios-host.md))。Loading と Toast の入口は [Loading](loading.md) と [Toast](toast.md) にそれぞれの選択表がある。

`Dialog.instance.registry` は、どの入口から取得しても同じレジストリを共有する。共有コードから登録できるのは ViewModel factory (`registerViewModel`) だけで、content の型が OS ごとに違うため content の登録 API は持たない ([ViewModel](view-models.md))。

## ViewModel を宣言する

ViewModel は結果型を宣言するだけの class で、共有 module に置く。この class がそのまま各 host のレジストリのキーになる。

```kotlin
package com.example.shared

import jp.kamusoft.ksdialogs.kmp.DialogViewModel

class DeleteViewModel(var itemName: String = "") : DialogViewModel<Boolean>
```

## 各 host で content を登録する

`show` を呼ぶより前に、この class に対する content を各 host の起動経路で 1 回登録する。Android は `Dialog.instance.registry.register(DeleteViewModel::class) { … }`、iOS は `Dialog.shared.kmp.register(DeleteViewModel.self) { … }` を使う。完動レシピは [Android host](android-host.md) と [iOS host](ios-host.md) にある。

## 共有コードから呼び出す

呼び出し元は共有の ViewModel や use case の class に置き、表示の入口はコンストラクタで受け取る。既定値を `Dialog.instance` にしておけば、通常は何も渡さずに済み、テストでは差し替えたものを渡せる ([ViewModel](view-models.md))。

```kotlin
package com.example.shared

import jp.kamusoft.ksdialogs.kmp.Dialog
import jp.kamusoft.ksdialogs.kmp.DialogException
import jp.kamusoft.ksdialogs.kmp.DialogResult
import jp.kamusoft.ksdialogs.kmp.KsDialog
import kotlin.coroutines.cancellation.CancellationException

class ItemListViewModel(
    private val repository: ItemRepository,
    private val dialogs: KsDialog = Dialog.instance,
) {
    @Throws(DialogException::class, CancellationException::class)
    suspend fun deleteItem(itemName: String) {
        when (val result = dialogs.show(DeleteViewModel(itemName))) {
            is DialogResult.Completed -> if (result.value) repository.delete(itemName)
            DialogResult.Cancelled -> Unit
        }
    }
}
```

この関数を Swift から呼ぶなら `@Throws` が要る。宣言が無いと Kotlin/Native は例外を `NSError` に変換せず、`suspend` の関数では未処理例外でプロセスが終了する ([iOS host](ios-host.md))。

## 真偽値以外の結果型を返す

結果型は ViewModel が宣言した型がそのまま `DialogResult<R>` になる。型引数に制約は無いので、真偽値以外の型も宣言できる。

```kotlin
package com.example.shared

import jp.kamusoft.ksdialogs.kmp.DialogViewModel

class ChoiceViewModel(val title: String) : DialogViewModel<String>
```

```kotlin
package com.example.shared

import jp.kamusoft.ksdialogs.kmp.Dialog
import jp.kamusoft.ksdialogs.kmp.DialogException
import jp.kamusoft.ksdialogs.kmp.DialogResult
import jp.kamusoft.ksdialogs.kmp.KsDialog
import kotlin.coroutines.cancellation.CancellationException

class PlanPicker(private val dialogs: KsDialog = Dialog.instance) {
    @Throws(DialogException::class, CancellationException::class)
    suspend fun pickPlan(): String? =
        when (val result = dialogs.show(ChoiceViewModel("Choose a plan"))) {
            is DialogResult.Completed -> result.value
            DialogResult.Cancelled -> null
        }
}
```

host 側も同じ型で報告する。Android の factory が受け取る `DialogNotifier<R>` は宣言から型が決まるため、違う型で報告する書き方はコンパイルできない。iOS は結果型が Swift から見えないので、登録と `notifier` の呼び出しで `result:` に同じ型を渡す — 食い違いは結果を戻す時点で失敗として現れる ([iOS host](ios-host.md))。

## Dialog を独立して重ねて表示する

呼び出しごとに別の ViewModel instance を使う。重なっても結果は独立し、後から表示した Dialog が手前になってユーザー操作を受ける。同じ instance を同時に 2 回表示しようとすると失敗する — 結果の紐付けが instance ごとに 1 本だからである。

覆われた下段の Dialog をユーザーが UI から閉じることはできない。次の platform 差が生じるのは、アプリコードが下段 content の notifier を保持し、下段の結果を先に報告した場合だけである。Android は下段だけを閉じ、上段は表示中のまま残って後で自身の completed または cancelled result を返す。iOS は提示の連なりから両方を外し、上段 `show` は `DialogResult.Cancelled` を返す。上段の container は OS によってすでに外れているため、その dismissal hook は実行されない。

```kotlin
package com.example.shared

import jp.kamusoft.ksdialogs.kmp.Dialog
import jp.kamusoft.ksdialogs.kmp.DialogException
import jp.kamusoft.ksdialogs.kmp.DialogResult
import jp.kamusoft.ksdialogs.kmp.KsDialog
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class ChoicePairPicker(private val dialogs: KsDialog = Dialog.instance) {
    @Throws(DialogException::class, CancellationException::class)
    suspend fun pickBoth(): Pair<DialogResult<String>, DialogResult<String>> = coroutineScope {
        val first = async { dialogs.show(ChoiceViewModel("First")) }
        val second = async { dialogs.show(ChoiceViewModel("Second")) }
        first.await() to second.await()
    }
}
```

host の content が最初に報告した完了またはキャンセルだけが有効で、以降の報告は何もしない。ライブラリは重なりの枚数を数えず、API としても公開しない。

## 出す先の画面がまだ無いときに呼ぶ

出す先の画面 (提示先) が無いときの `show` は失敗しない。提示先が現れるのを待ってから content を作り、表示する。起動直後に最初の画面の表示処理から呼んだとき、アプリが背面にいる間に呼んだとき、システムの許可ダイアログが出ている間に呼んだときがこれに当たる。

| host | 提示先 |
|---|---|
| iOS | 前面でアクティブなシーンの key window |
| Android | resumed で、かつ描画された Activity。起動画面の下で開いている途中の画面には出さず、Activity が破棄されただけでは出さずに次の Activity を待つ |

待ちに上限は無い。待ちが終わるのは、提示先が現れて表示に進んだとき、呼び出し元のコルーチンがキャンセルされたとき (一度も表示せずに `CancellationException` が伝播する)、待っている間にアプリが報告口で結果を報告したとき (表示せずにその結果を返す) である。画面を持たない処理やバックグラウンドの処理など、提示先が現れる見込みの無い場所から呼ぶ `show` は返らないので、打ち切れるようにしておく。長く背面にいた後に、古い文脈の Dialog が表示されることもある。

次は、待ちに期限を付けて、期限までに出せなければ「復元しない」として扱う例である。

```kotlin
package com.example.shared

import jp.kamusoft.ksdialogs.kmp.Dialog
import jp.kamusoft.ksdialogs.kmp.DialogException
import jp.kamusoft.ksdialogs.kmp.DialogResult
import jp.kamusoft.ksdialogs.kmp.KsDialog
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.withTimeoutOrNull

class DraftRecovery(private val dialogs: KsDialog = Dialog.instance) {
    @Throws(DialogException::class, CancellationException::class)
    suspend fun askToRestore(): Boolean {
        val result = withTimeoutOrNull(30_000) {
            dialogs.show(DeleteViewModel("Unsaved draft"))
        }
        return result is DialogResult.Completed && result.value
    }
}
```

待っている Dialog が複数あるときは、呼んだ順に並べて 1 枚ずつ載せる。前の 1 枚が画面に載り終えてから次を載せるので、後から呼んだものが手前に重なる。待っている Dialog があるうちに呼んだ `show` は、提示先があってもその後ろに並ぶ。並ぶ順が呼んだ順になるのは、同じ UI スレッドから続けて呼んだ場合だけである。

| 呼び方 | 並ぶ順 |
|---|---|
| 同じ UI スレッドから続けて呼んだ `show` | 呼んだ順 |
| UI スレッド以外から呼んだ `show` | 保証しない。UI スレッドに着いた順 |
| `configure` が中断する class 指定の `show` | 保証しない。`configure` を終えた順 |

待っている間も、報告口はその instance へ紐付いている。同じ instance をもう一度 `show` すると、まだ表示されていなくても `DialogException` になる。View factory の未登録も、提示先の有無にかかわらず待たずに `DialogException` になる。

## 各メソッドの最小例

選択表のうち instance を渡す `show` は、上の節の例がそのまま最小例になっている。ここには既存の例に現れない引数の形を置く。

`placement` を渡す形。この呼び出しの間だけ、content に添付された配置をまるごと置き換える。

```kotlin
dialogs.show(DeleteViewModel("Report"), DialogPlacement(verticalAlignment = DialogAlignment.END))
```

ViewModel の class を渡す形。共有コードに登録した ViewModel factory が instance を作る。`configure` は最後の引数なので、渡すなら末尾のラムダで書ける。これは `suspend` なので、中で待つ処理も書ける ([ViewModel](view-models.md))。

```kotlin
dialogs.show(DeleteViewModel::class)
dialogs.show(DeleteViewModel::class) { viewModel -> viewModel.itemName = "Report" }
```

## 構成ミスの失敗を扱う

構成ミスは `Cancelled` を返さず `DialogException` を投げる。登録漏れをユーザーのキャンセルと取り違えないためであり、この場合 content は生成も表示もされない。共有コードの `DialogException` はサブクラスを持たないため、原因はメッセージからしか読めない — Native ライブラリで起きた失敗はその説明文がそのまま届き、class を渡す `show` の失敗は共有コードが説明文を組み立てる。

| 状況 | メッセージ | 対処 |
|---|---|---|
| その ViewModel class の content が host に未登録 | `No View factory is registered for ViewModel type {TypeName}.` | 起動時に各 host で登録する ([Android host](android-host.md) / [iOS host](ios-host.md)) |
| class を渡したが共有コードに ViewModel factory が未登録 | `No ViewModel factory is registered for ViewModel type {TypeName}.` | 起動時に `registry.registerViewModel` で登録する ([ViewModel](view-models.md)) |
| ViewModel factory が登録キーと違う class の instance を返した | `The registered ViewModel factory does not produce ViewModel type {TypeName}. It produced {TypeName} instead. A ViewModel factory must return a ViewModel of the same class as its registration key.` | factory の戻り値を登録キーと同じ class にする |
| 同じ ViewModel instance を重ねて表示した | `This ViewModel instance of type {TypeName} is already being shown.` | `show` ごとに新しい instance を作る |
| 報告された結果値を宣言結果型へ戻せない (iOS) | `The result value type does not match (expected: {TypeName} / actual: {TypeName}).` | 登録と `notifier` の `result:` に ViewModel の宣言と同じ型を渡す |
| ViewModel を value class で宣言した (Android) | `ViewModel type {TypeName} is a value class and cannot be used as a ViewModel.` | ViewModel を class で書く |

表のメッセージは現在の実装が返す値であり、安定した API ではない (変わらないのは例外型と throw される条件であり、文言は予告なく変わりうる)。

iOS ではこのほかに、登録済み factory がその ViewModel を受け取れないときの `The registered View factory cannot accept ViewModel type {TypeName}.`、結果が届かなかったときの `No Dialog result was delivered.` と `Failed to show the Dialog.` がメッセージになる。

ViewModel factory と `configure` が投げた例外は `DialogException` に包まれない。表示に進まないまま、その例外がそのまま呼び出し元へ伝わる。

たとえば Android の起動経路で `DeleteViewModel` だけを登録し、`ChoiceViewModel` の登録を書き忘れると、`ChoiceViewModel` の `show` は content を解決できずに失敗する。

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

呼び出し元では `DialogException` を `catch` できる。構成ミスは実行時に直せるものではないので、ログに残したうえで再 throw し、開発中に気づける形にする。`CancellationException` は別の型なのでこの `catch` には入らず、コルーチンのキャンセルはそのまま伝播する。

```kotlin
package com.example.shared

import jp.kamusoft.ksdialogs.kmp.Dialog
import jp.kamusoft.ksdialogs.kmp.DialogException
import jp.kamusoft.ksdialogs.kmp.DialogResult
import jp.kamusoft.ksdialogs.kmp.KsDialog
import kotlin.coroutines.cancellation.CancellationException

class PlanPicker(private val dialogs: KsDialog = Dialog.instance) {
    @Throws(DialogException::class, CancellationException::class)
    suspend fun pickPlan(): String? =
        try {
            when (val result = dialogs.show(ChoiceViewModel("Choose a plan"))) {
                is DialogResult.Completed -> result.value
                DialogResult.Cancelled -> null
            }
        } catch (failure: DialogException) {
            println("Dialog content is not registered: ${failure.message}")
            throw failure
        }
}
```
