# 共有 ViewModel を書き、結果を受け取る

共有コードに書くのは ViewModel の class と `show` の呼び出しだけで、結果を報告するのは host 側の content である。この文書は ViewModel の書き方、表示の入口の受け取り方、結果が返ってくるまでの流れを扱う。

## ViewModel は class で書く

`DialogViewModel<R>`・`LoadingViewModel`・`ToastViewModel` はメンバーを持たない目印で、class そのものがレジストリの登録キーになる。

- 宣言は class で書く。値型はコピーで instance の同一性を失うため結果の紐付けを持てず、表示に至る前に拒否される
- 結果型は Dialog の `DialogViewModel<R>` の型引数だけが宣言する。Loading と Toast は結果を返さない
- 配置・大きさ・覆い・演出は ViewModel に持たせない。それらは host 側の content の定義に属する ([レイアウト](layout.md) / [トランジション](transitions.md))
- ライフサイクルのメンバーは無い。初期化はコンストラクタで行い、後始末は `show` の呼び出しが戻ってから書く

## 表示の入口をコンストラクタで受け取る

呼び出し元は `KsDialog`・`KsLoading`・`KsToast` を受け取り、既定値を各既定エントリにしておく。通常は何も渡さずに済み、テストでは fake を渡して共有ロジックだけを検証できる。本番の Native レジストリを置換・二重化する必要はない。契約を自分で実装する差し替えでは、`registry` と class を渡す `show` も実装することになる ([iOS host](ios-host.md))。

どの入口から使っても、レジストリ・Loading の表示・Toast の表示リストはプロセス内で 1 つずつを共有する。

次は、[Dialog](dialogs.md) の呼び出し元に Loading と Toast を足した形である。

```kotlin
package com.example.shared

import jp.kamusoft.ksdialogs.kmp.Dialog
import jp.kamusoft.ksdialogs.kmp.DialogException
import jp.kamusoft.ksdialogs.kmp.DialogResult
import jp.kamusoft.ksdialogs.kmp.KsDialog
import jp.kamusoft.ksdialogs.kmp.KsLoading
import jp.kamusoft.ksdialogs.kmp.KsToast
import jp.kamusoft.ksdialogs.kmp.Loading
import jp.kamusoft.ksdialogs.kmp.Toast
import kotlin.coroutines.cancellation.CancellationException

class ItemListViewModel(
    private val repository: ItemRepository,
    private val dialogs: KsDialog = Dialog.instance,
    private val loading: KsLoading = Loading.instance,
    private val toast: KsToast = Toast.instance,
) {
    @Throws(DialogException::class, CancellationException::class)
    suspend fun deleteItem(itemName: String) {
        val result = dialogs.show(DeleteViewModel(itemName))
        if (result !is DialogResult.Completed || !result.value) return
        loading.start(message = "Deleting") { repository.delete(itemName) }
        toast.show("Deleted")
    }
}
```

## content が結果を報告する

共有の `DialogViewModel<R>` は結果型を宣言するだけで、報告口そのものは保持しない。ライブラリが表示のたびに報告口をその instance へ紐付け、結果が呼び出し元へ渡った時点で外す。

| 項目 | 内容 |
|---|---|
| 型 | ViewModel が宣言した結果型の `DialogNotifier<R>` |
| 読める期間 | 表示中だけ。`show` の前と、結果または例外が呼び出し元へ渡った後は空になる |
| 紐付けの単位 | ViewModel の instance 1 つにつき 1 本。同じ instance を並行して表示すると構成ミスとして失敗する |
| 報告の方法 | `complete(value)` で値を返し、`cancel()` でキャンセルを報告する |
| 2 回目以降の報告 | 最初の報告だけが有効で、以降は何もしない |

報告口の取り出し方は host ごとに違う。

| host | content が報告口を得る方法 |
|---|---|
| Android | 2 引数 factory が `DialogNotifier<R>` を受け取るか、1 引数 factory から `viewModel.notifier` を読む |
| iOS | content が `Dialog.shared.kmp.notifier(for:)` を呼ぶ。真偽値以外の結果では `notifier(for:result:)` を使う |

## 必要な登録

`show` や `start` を呼ぶより前に、その ViewModel class に対する content を各 host の起動経路で 1 回登録する。登録先は Dialog / Loading / Toast で独立していて、Dialog に登録した class が Loading や Toast に登録されることはない。

content の登録 API は host 側のもので、共有コードには無い (共有コードが登録できるのは ViewModel の作り方だけである — 次の節)。Android は Android Native のレジストリ (`jp.kamusoft.ksdialogs` の `Dialog` / `Loading` / `Toast`)、iOS は各 facade の `kmp` 入口を使う。

| 表示するもの | Android の登録 (Android Native) | iOS の登録 |
|---|---|---|
| Dialog | `Dialog.instance.registry.register(VM::class) { … }` / `registerCompose(VM::class) { … }` | `Dialog.shared.kmp.register(VM.self) { … }` |
| カスタム Loading | `Loading.instance.registry.register(VM::class) { … }` / `registerCompose(VM::class) { … }` | `Loading.shared.kmp.register(VM.self) { … }` |
| カスタム Toast | `Toast.instance.registry.register(VM::class) { … }` / `registerCompose(VM::class) { … }` | `Toast.shared.kmp.register(VM.self) { … }` |

message だけを渡す Loading と Toast は内蔵 content を使うため、登録は要らない。完動レシピは [Android host](android-host.md) と [iOS host](ios-host.md) にある。

## ViewModel factory を登録して型で表示する

レジストリのエントリは ViewModel class ごとに 2 つのスロットを持つ。中身の View を作る View factory と、ViewModel 自身を作る ViewModel factory である。View factory は各 host が登録し、ViewModel factory は共有コードが `registry.registerViewModel` で登録する。両方が埋まっていれば、共有コードは instance の代わりに class を渡して表示できる。Dialog の `show`、Toast の `show`、Loading の `show` と `start` にこの形がある。

ライブラリは ViewModel を作り、`configure` を完了させてから content を作る。だから content は `configure` が書いた状態を必ず読める。ViewModel factory と `configure` は呼び出し元の文脈でそのまま走り、UI スレッドへは移らない — UI スレッドが要る処理は host 側の View factory に置く。

登録し直すと該当のスロットだけが置き換わり、解決は呼び出し時点のスナップショットで行われるので、表示中に登録し直しても出ているものは変わらない。factory は登録キーと同じ class の instance を返す。サブクラスを返すと content を引けないため、型不一致の構成ミスとして失敗する。

共有コードの ViewModel factory の表は host のレジストリとは別である。Android Native の `registerViewModel` へ登録したものは共有コードから class を渡す `show` には見えないので、共有コードから表示するなら共有コードで登録する ([Android host](android-host.md))。この形は共有 Kotlin コード専用で、iOS host の Swift 入口には無い ([iOS host](ios-host.md))。

`configure` が書き込む状態を共有 ViewModel に持たせる。

```kotlin
package com.example.shared

import jp.kamusoft.ksdialogs.kmp.DialogViewModel

class ExportViewModel : DialogViewModel<Boolean> {
    var itemName = ""
}
```

Dialog・Loading・Toast の登録と呼び出しをまとめた例である。`UploadLoadingViewModel` は [Loading](loading.md)、`StatusToastViewModel` は [Toast](toast.md) で宣言している。`Loading` のスコープ形では末尾に渡すのが `action` なので、`configure` は名前付き引数で渡す。

```kotlin
package com.example.shared

import jp.kamusoft.ksdialogs.kmp.Dialog
import jp.kamusoft.ksdialogs.kmp.DialogException
import jp.kamusoft.ksdialogs.kmp.DialogResult
import jp.kamusoft.ksdialogs.kmp.KsDialog
import jp.kamusoft.ksdialogs.kmp.KsLoading
import jp.kamusoft.ksdialogs.kmp.KsToast
import jp.kamusoft.ksdialogs.kmp.Loading
import jp.kamusoft.ksdialogs.kmp.Toast
import kotlin.coroutines.cancellation.CancellationException

class ExportPresenter(
    private val dialogs: KsDialog = Dialog.instance,
    private val loading: KsLoading = Loading.instance,
    private val toast: KsToast = Toast.instance,
) {
    init {
        dialogs.registry.registerViewModel(ExportViewModel::class, ::ExportViewModel)
        loading.registry.registerViewModel(UploadLoadingViewModel::class, ::UploadLoadingViewModel)
        toast.registry.registerViewModel(StatusToastViewModel::class) { StatusToastViewModel("Exported") }
    }

    @Throws(DialogException::class, CancellationException::class)
    suspend fun exportItem(itemName: String) {
        val result = dialogs.show(ExportViewModel::class) { viewModel ->
            viewModel.itemName = itemName
        }
        if (result !is DialogResult.Completed || !result.value) return
        loading.start(UploadLoadingViewModel::class) { report -> report(1.0) }
        toast.show(StatusToastViewModel::class)
    }
}
```

ViewModel factory が未登録のとき、factory が登録キーと違う class を返したときは、表示に進まずに `DialogException` になる ([Dialog](dialogs.md) / [Loading](loading.md) / [Toast](toast.md))。factory と `configure` が投げた例外は包まれず、そのまま呼び出し元へ伝わる。

## 生成から結果までの順序

1. 呼び出し元が ViewModel の instance を作り、`show` に渡す
2. ライブラリが報告口をその instance へ紐付ける
3. host の View factory が呼ばれ、content が作られる (この時点で報告口はもう読める)
4. content が表示され、出現の演出が走る
5. content が `complete` か `cancel` で結果を報告する。外側タップ・Android の戻るボタンでもキャンセルが確定する
6. 閉鎖の演出が終わって器が撤去され、`show` が `DialogResult` を返す。報告口の紐付けはここで外れる

報告口の紐付けは、正常な結果配送だけでなく、表示に失敗した経路でも必ず外れる。だから失敗した直後でも同じ instance をもう一度表示できる。
