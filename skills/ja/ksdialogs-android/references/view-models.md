# ViewModel から結果を報告する

Dialog の結果は、表示中の ViewModel が拡張プロパティ `notifier` から報告できる。この文書は `notifier` の読み方と、ViewModel の型だけを渡す `show` の使い方を扱う。

## `notifier` を読む

`notifier` は表示中だけ値を持つ。ViewModel 自身の中からは `notifier` と書き、`?.` で呼ぶ。

| 項目 | 内容 |
|---|---|
| 型 | ViewModel が宣言した結果型の `DialogNotifier<R>` |
| show の前後 | `null` になる (表示中だけ値を返す) |
| 取り除かれる時点 | 完了・キャンセル・失敗のどの終端でも取り除かれる |
| 紐付く show の経路 | インスタンス渡し・インライン factory・型指定のすべて |
| 2 引数 factory との関係 | ViewModel から読んだ notifier と factory 引数の notifier は同じ配送先を指す |

以下は真偽値を結果とする ViewModel で、`accept` が完了を、`cancel` がキャンセルを報告する。

```kotlin
import jp.kamusoft.ksdialogs.SimpleDialogViewModel
import jp.kamusoft.ksdialogs.notifier

class ConfirmViewModel(val message: String) : SimpleDialogViewModel {
    fun accept() {
        notifier?.complete(true)
    }

    fun cancel() {
        notifier?.cancel()
    }
}
```

報告を ViewModel に任せるなら、コンテンツは ViewModel だけを受け取る形で登録できる。

```kotlin
import android.app.Application
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import jp.kamusoft.ksdialogs.Dialog
import jp.kamusoft.ksdialogs.compose.registerCompose

class SampleApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Dialog.instance.registry.registerCompose(ConfirmViewModel::class) { viewModel ->
            Button(onClick = viewModel::accept) { Text(viewModel.message) }
        }
    }
}
```

View コンテンツも同じ 1 引数の形で `register` に渡せる。コンストラクタが `(Context, VM)` の View なら、コンストラクタ参照をそのまま factory にできる。

## ViewModel は class にする

`notifier` の紐付けはインスタンスの同一性で引くため、ViewModel は class でなければならない。

- value class は boxing のたびに同一性が失われるので扱えない。登録の時点と、インライン factory を含むすべての表示の時点で `DialogException.ValueClassViewModel` として拒否する
- 等価比較 (`equals`) が一致する別インスタンスは互いに干渉しない。それぞれが自分の `notifier` を持つ

## 同じインスタンスを重ねて表示しない

1 つのインスタンスが持てる紐付けは 1 つだけである。

- 同じインスタンスを並行して表示すると `DialogException.ViewModelAlreadyShowing` で失敗する
- 失敗しても先に表示中の Dialog には影響しない
- 同じ型でも別インスタンスなら独立して重なる ([Dialog](dialogs.md) の「独立した Dialog を重ねる」)
- 紐付けは終端のたびに外れるので、失敗した直後でも同じインスタンスを表示し直せる

## 型から ViewModel を生成して設定する

型指定 `show` は ViewModel の型だけを受け取り、登録済みの ViewModel factory が作ったインスタンスを `configure` してから表示する。

### 必要な登録

レジストリのエントリは独立した 2 つのスロットを持つ。

| スロット | 入れる API | 使う show |
|---|---|---|
| View factory | `register` / `registerCompose` | インスタンス渡しと型指定の両方 |
| ViewModel factory | `registerViewModel` | 型指定だけ |

- 型指定 `show` は両方を必要とする。ViewModel factory が無ければ、コンテンツを作る前に `DialogException.ViewModelFactoryNotRegistered` で失敗する
- 再登録は触れたスロットだけを置き換え、もう片方は残る
- 解決は `show` を呼んだ時点のスナップショットなので、表示中に再登録しても出ている Dialog には影響しない
- DI コンテナから ViewModel を取るときは、`registerViewModel` の factory の中でコンテナを呼ぶ

### 生成から表示までの順序

順序は次に固定されている。

1. ViewModel factory が ViewModel を生成する (Main dispatcher)
2. `configure` が完了する (`suspend` として書ける)
3. `notifier` が紐付く
4. View factory がコンテンツを生成する
5. Dialog を表示する

したがって `configure` が入れた状態は、コンテンツの初期化から必ず読める。ViewModel factory や `configure` が投げた失敗は `Cancelled` にはならず、表示へ進まずに呼び出し元へ伝播する。

以下は結果型に `String` を宣言した ViewModel を 2 つのスロットとも登録し、型指定 `show` の `configure` で `prompt` を入れてから表示する例である。

```kotlin
import jp.kamusoft.ksdialogs.DialogViewModel
import jp.kamusoft.ksdialogs.notifier

class ProfileViewModel : DialogViewModel<String> {
    var prompt = ""

    fun save(name: String) {
        notifier?.complete(name)
    }
}
```

```kotlin
import android.app.Application
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import jp.kamusoft.ksdialogs.Dialog
import jp.kamusoft.ksdialogs.compose.registerCompose

class SampleApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Dialog.instance.registry.registerCompose(ProfileViewModel::class) { viewModel ->
            Button(onClick = { viewModel.save("Ada") }) { Text(viewModel.prompt) }
        }
        Dialog.instance.registry.registerViewModel(ProfileViewModel::class, ::ProfileViewModel)
    }
}
```

呼び出し元は ViewModel のインスタンスを組み立てず、型と `configure` だけを渡す。

```kotlin
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import jp.kamusoft.ksdialogs.DialogResult
import jp.kamusoft.ksdialogs.KsDialog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileScreenViewModel(private val dialogs: KsDialog) : ViewModel() {
    private val mutableStatus = MutableStateFlow("")
    val status = mutableStatus.asStateFlow()

    fun onEditClicked() {
        viewModelScope.launch {
            val result = dialogs.show(ProfileViewModel::class) { viewModel ->
                viewModel.prompt = "Continue?"
            }
            mutableStatus.value = when (result) {
                is DialogResult.Completed -> result.value
                DialogResult.Cancelled -> "Cancelled"
            }
        }
    }
}
```

`configure` は `suspend` として書けるので、表示の前に初期状態を読み込むこともできる。コンテンツが作られるのはその完了後である。

## Loading と Toast も同じ形で呼べる

型指定 `show` は Dialog 専用の経路ではない。Loading には型指定 `show` と型指定 `start` が、Toast には型指定 `show` があり、いずれも「登録した ViewModel factory が生成 → `configure` の完了 → コンテンツの生成 → 表示」の順序と、呼び出し時点のスナップショットによる解決を Dialog と共有する。レジストリは 3 種類とも独立していて、それぞれの `registerViewModel` に登録する。書き方は [Loading](loading.md) と [Toast](toast.md) のレシピにある。

機能の性質から来る違いは次のとおりである。

| 機能 | `configure` | コンテンツ生成の前に挟まる紐付け | ViewModel factory・`configure` が投げた失敗 |
|---|---|---|---|
| Dialog | `suspend` として書ける | `notifier` | 表示へ進まずに呼び出し元へ伝播する |
| Loading (`show` / `start`) | `suspend` として書ける | 進捗の受け口。合流の判定は `configure` の完了後で、合流側になった呼び出しの ViewModel は表示に使われない | 表示にも合流にも進まずに呼び出し元へ伝播し、`start` では処理も実行されない |
| Toast | 同期のみ (`show` が戻り値を持たない同期の呼び出しであるため) | なし | `show` は既に戻っているため呼び出し元へ返らず、ログを残してその表示 1 枚だけが破棄される |

型指定 `show` は呼ぶ側にとっては追加であり、インスタンス渡しの `show`・インライン factory・2 引数 factory の登録はそのまま使える。ただし `KsDialog` / `KsLoading` / `KsToast` を自分で実装した型 (テストダブルや adapter) には型指定 `show` のメンバーが増えるため、再コンパイル時にその実装が必要になる。
