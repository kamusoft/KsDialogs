# 共有コードから Toast を表示する

Toast は非対話の通知で、duration が経つと自分で消える。呼び出しは共有コード (`commonMain`) に書き、組み込みの非対話メッセージ Toast に文言を載せるか、各 host に登録した content を表示する。`show` は同期で戻り値を持たず、任意のスレッドから呼べる。受理した呼び出しは host 側が UI スレッド上で順に処理する。

## `show` を選ぶ

軸は 2 つで、内蔵の見た目に文言を載せるか登録した content を表示するか、そして content を表示する場合に ViewModel の instance を渡すか class だけを渡すかである。閉じる入口は無い — Toast が消える契機は duration の経過だけである。`Toast.instance` から呼んでも `KsToast` として注入したものから呼んでも、同じレジストリと同じ重なりの管理に届く ([ViewModel](view-models.md))。

| シグネチャ | 何をする | いつ選ぶ | 必要な登録 |
|---|---|---|---|
| `fun show(message: String, durationMs: Int? = null, placement: DialogPlacement? = null)` | 組み込みの非対話メッセージ Toast に文言を載せて表示する | 文言だけを伝えるとき | 不要 |
| `fun show(viewModel: ToastViewModel, durationMs: Int? = null, placement: DialogPlacement? = null)` | 登録済みの content を表示する | 見た目を自分で作るとき | その ViewModel class の content |
| `fun <VM : ToastViewModel> show(viewModelClass: KClass<VM>, durationMs: Int? = null, placement: DialogPlacement? = null, configure: ((VM) -> Unit)? = null)` | 登録済みの ViewModel factory で instance を作り、`configure` の後に表示する | 同上で、組み立てをライブラリに任せるとき | 上に加えて共有コードの ViewModel factory |

`durationMs` を省略すると host 側で設定された既定 duration になり、`placement` を省略すると host 側のアプリ既定配置、それも無ければ契約の既定値になる ([レイアウト](layout.md))。class を渡す形は共有コードに登録した ViewModel factory が instance を作るもので、共有 Kotlin コード専用であり Swift からは見えない ([ViewModel](view-models.md))。`show` が同期なので `configure` も同期の関数で、中断関数にはできない。Dialog と Loading の入口は [Dialog](dialogs.md) と [Loading](loading.md) にそれぞれの選択表がある。

## 文言だけの Toast を出す

```kotlin
package com.example.shared

import jp.kamusoft.ksdialogs.kmp.DialogAlignment
import jp.kamusoft.ksdialogs.kmp.DialogPlacement
import jp.kamusoft.ksdialogs.kmp.KsToast
import jp.kamusoft.ksdialogs.kmp.Toast

class ItemEditor(
    private val repository: ItemRepository,
    private val toast: KsToast = Toast.instance,
) {
    suspend fun save(item: Item) {
        repository.save(item)
        toast.show(
            message = "Saved",
            durationMs = 2000,
            placement = DialogPlacement(
                verticalAlignment = DialogAlignment.END,
                offsetY = -100.0,
            ),
        )
    }
}
```

## duration の決まり

`durationMs` はミリ秒の `Int?` である。共有コードから渡せるよう、プラットフォーム固有の時間型を使わない。計時は単調時計で行い、アプリが背面にある間も進む。上限のクランプは無い。0 以下の値は例外にせず警告ログを出して host 側の既定へ丸める。

数え始めは、表示を始める処理の時点の状態で決まる。起動直後や許可ダイアログの最中に出した Toast が、利用者に見えないまま duration を使い切らないためである。

| 表示を始める時点の状態 | 数え始め |
|---|---|
| 提示先がある | 受理した時点 |
| アプリが背面にいる | 受理した時点。提示先が現れないまま満了すると、表示されずに破棄される |
| アプリが前面にいるのに提示先が無い (起動中の画面・許可ダイアログの最中など) | 提示先に載った時点。載る前にアプリが背面へ下がったら、下がった時点 |

一度決まった期限は、画面の作り直し・載せ替え・背面からの復帰をまたいでも巻き戻らない。前面にいるのに提示先が現れない間 (起動画面を延ばし続けるなど) は期限が決まらず、表示を待ち続ける。Android の起動直後は、起動画面が退場する演出の分だけ起動画面の下で数えるので、見えている時間はその分 duration より短い。

文言の内容は制限しない。空文字は内容が空のまま表示され、長文は複数行に折り返す。組み込みの Toast は自分の中身に全辺 24 の余白を持ち、変える口は無い。この余白は配置をどう渡しても効くので、長文でも画面の左右の端まで広がらない。カスタム content の余白は Dialog と同じく既定が全辺 0 で、host が content に添付して変える ([レイアウト](layout.md))。

## 登録済みの custom content を表示する

`commonMain` で `ToastViewModel` を実装した class を宣言する。この class に対する content は、`show` を呼ぶより前に各 host の起動経路で 1 回登録する。Android は `Toast.instance.registry.register(StatusToastViewModel::class) { … }`、iOS は `Toast.shared.kmp.register(StatusToastViewModel.self) { … }` を使う。完動レシピは [Android host](android-host.md) と [iOS host](ios-host.md) にある。

```kotlin
package com.example.shared

import jp.kamusoft.ksdialogs.kmp.ToastViewModel

class StatusToastViewModel(var message: String = "") : ToastViewModel
```

```kotlin
package com.example.shared

import jp.kamusoft.ksdialogs.kmp.DialogException
import jp.kamusoft.ksdialogs.kmp.KsToast
import jp.kamusoft.ksdialogs.kmp.Toast

class SyncStatusReporter(private val toast: KsToast = Toast.instance) {
    @Throws(DialogException::class)
    fun notifyStatus(message: String) {
        toast.show(
            viewModel = StatusToastViewModel(message),
            durationMs = 1800,
        )
    }
}
```

## 各メソッドの最小例

選択表のうち文言を渡す形と instance を渡す形は、上の節に例がある。ここには既存の例に現れない引数の形を置く。

`durationMs` と `placement` を省略して、host 側の既定 duration と既定配置に任せる形。

```kotlin
toast.show("Saved")
```

`placement` を渡す形。カスタム content の側にも同じ引数があり、content に添付された配置を差し替える。

```kotlin
toast.show(StatusToastViewModel("Synced"), placement = DialogPlacement(verticalAlignment = DialogAlignment.END))
```

ViewModel の class を渡す形。共有コードに登録した ViewModel factory が instance を作り、`configure` を渡せば表示の前にそれが走る。Dialog や Loading と違い、この `configure` は `suspend` ではないので中で待てない ([ViewModel](view-models.md))。

```kotlin
toast.show(StatusToastViewModel::class)
toast.show(StatusToastViewModel::class) { viewModel -> viewModel.message = "Exported" }
```

## Toast がしないこと

- `hide`・結果・進捗・スコープ形を持たない。消える契機は duration の経過だけである。
- 対話しない。Toast へのタッチは背後のページへ素通しされ、カスタム content の中に置いた部品も反応しない。ボタンの要る通知は Dialog で作る ([Dialog](dialogs.md))。
- 順番待ちも置き換えもしない。同時に出した Toast はすべて表示され、受理順に重なって後のものが手前になり、それぞれ自分のタイマーで消える。同じ実効配置の Toast は位置をずらさずに重なる。
- ページに紐づかない。ページ遷移をまたいで表示は続き、回転やリサイズでは再配置されるが残り duration は巻き戻らない。載っている画面が破棄されたときと、描画済みの別の提示先が現れたときだけ別の画面へ載せ替わる。
- 提示先の画面のシステムバーの指定 (アイコンの明暗・表示/非表示) を変えない。

Loading は常に Toast より前面に出る。Dialog と Toast の前後関係は保証しない。

## 構成ミスの失敗を扱う

message 経路は構成ミスの例外を持たない。失敗するのは、登録済み content を使う ViewModel 経路だけである。

| 状況 | メッセージ | 対処 |
|---|---|---|
| その ViewModel class の content が host に未登録 | `No View factory is registered for ViewModel type {TypeName}.` | 起動時に各 host で登録する ([Android host](android-host.md) / [iOS host](ios-host.md)) |
| class を渡したが共有コードに ViewModel factory が未登録 | `No ViewModel factory is registered for ViewModel type {TypeName}.` | 起動時に `registry.registerViewModel` で登録する ([ViewModel](view-models.md)) |
| ViewModel factory が登録キーと違う class の instance を返した | `The registered ViewModel factory does not produce ViewModel type {TypeName}. It produced {TypeName} instead. A ViewModel factory must return a ViewModel of the same class as its registration key.` | factory の戻り値を登録キーと同じ class にする |

表のメッセージは現在の実装が返す値であり、安定した API ではない (変わらないのは例外型と throw される条件であり、文言は予告なく変わりうる)。

class を渡す形では、ViewModel factory と `configure` は受理の前に呼び出しスレッドで走る。だから factory と `configure` が投げた例外も、受理後の失敗にはならずに `show` から同期で呼び出し元へ届く。後で満了して表示されずに捨てられた Toast でも、この 2 つは呼ばれている。

未登録の class は受理される前に同期で `DialogException` を投げ、表示は行われない。iOS ではこのほかに、登録済み factory がその ViewModel を受け取れないときの `The registered View factory cannot accept ViewModel type {TypeName}.` がメッセージになる。

受理された後の失敗 — host の factory が投げた場合や器の取り付けに失敗した場合 — は `show` が既に戻っているため呼び出し元へ返せない。その 1 枚だけが警告ログとともに破棄され、資源は解放される。ほかの表示や後続の呼び出しには影響しない。提示先がまだ無い場合は出現を待ち、host の content は提示先に載せる時点で作られる。載る前に duration が満了した Toast は、content を作らずに破棄される (満了の決まりは「duration の決まり」)。

たとえば Android の起動経路が Dialog の content だけを登録し、`Toast.instance.registry` へ何も登録していないと、`StatusToastViewModel` を渡した `show` は content を解決できずに失敗する。

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

呼び出し元では `DialogException` を `catch` できる。構成ミスは実行時に直せるものではないので、ログに残したうえで再 throw する。

```kotlin
package com.example.shared

import jp.kamusoft.ksdialogs.kmp.DialogException
import jp.kamusoft.ksdialogs.kmp.KsToast
import jp.kamusoft.ksdialogs.kmp.Toast

class SyncStatusReporter(private val toast: KsToast = Toast.instance) {
    @Throws(DialogException::class)
    fun notifyStatus(message: String) {
        try {
            toast.show(
                viewModel = StatusToastViewModel(message),
                durationMs = 1800,
            )
        } catch (failure: DialogException) {
            println("Toast content is not registered: ${failure.message}")
            throw failure
        }
    }
}
```

## host 側に残るもの

共有コードが制御するのは message・duration・配置と、ViewModel の作り方である。見た目の style、アプリ既定配置、カスタム content の登録、演出の添付は Native host 側にある。共有コードは content の型に触れないため、`Toast.instance.registry` が持つのは ViewModel factory の登録だけで、インライン factory の経路は無い。その経路は Android の面に Android 固有の UI 向けとしてある。[Android host](android-host.md) と [iOS host](ios-host.md) を読む。
