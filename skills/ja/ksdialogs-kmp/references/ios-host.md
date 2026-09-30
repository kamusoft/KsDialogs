# iOS ホスト統合

## 前提と3つの Setup 手順

前提: iOS target を持つ Kotlin Multiplatform 共有 module と、その framework をすでにビルド・リンクできる Xcode のアプリ project。

1. 共有 module の commonMain dependencies に `api("jp.kamusoft:ksdialogs-kmp:{version}")` を追加する。
2. `XCODEPROJ_PATH` を指定して `integrateLinkagePackage` を1回実行し、生成された `KotlinMultiplatformLinkedPackage/` directory を project と一緒に commit する。
3. Xcode の Package Dependencies に `https://github.com/kamusoft/KsDialogs-SPM` を同じ version の exact 指定で追加し、`KsDialogs` product をアプリ target に link する。

手順 1 の `{version}` はプレースホルダで、使う version に置き換える (そのままでは依存解決に失敗する)。現在の version は、常に最新のリリースへ解決される [latest release](https://github.com/kamusoft/KsDialogs/releases/latest) のページで確認できる。

```bash
XCODEPROJ_PATH="$PWD/iosApp/MyApp.xcodeproj" \
  ./gradlew :shared:integrateLinkagePackage
```

Maven 依存は発行 metadata に Swift package の参照を持ち、生成された package がその参照を Xcode の依存グラフへ渡すことで、共有 framework の未解決の Swift シンボルがリンクする。手順3の直接追加はそれとは別で、アプリのソースから登録 API を呼ぶために要る。同じ package identity は SwiftPM が 1 つにまとめるため、Swift の実体が二重化することはない。

発行 metadata が指す Swift package の版は Maven artifact と同じ version へ exact で固定される。公開された version には Swift package のリポジトリにも同じ version の tag が必ずあるため、両方に同じ 1 つの version を書き、2 つは一緒に上げる。

手順2は初回だけ行う integration である。その後は通常の Gradle build が依存の変更を生成済み package へ反映する。Xcode が参照する統合物なので clone 直後にも必要になり、VCS に含める。

## ホストコンテンツを登録する

共有 ViewModel は iOS Native の ViewModel 契約に準拠しないため、登録は KMP 入口の `Dialog.shared.kmp`・`Loading.shared.kmp`・`Toast.shared.kmp` を通る。書き込み先は純 Native の登録と同じレジストリである。生成された共有 framework は module 名で import する。次の例では `Shared` としている。

```swift
import KsDialogs
import Shared
import UIKit

enum DialogHostRegistration {
    @MainActor
    static func register() {
        Dialog.shared.kmp.register(DeleteViewModel.self) { viewModel, notifier in
            var configuration = UIButton.Configuration.filled()
            configuration.title = "Delete \(viewModel.itemName)"
            let view = UIButton(
                configuration: configuration,
                primaryAction: UIAction { _ in notifier.complete(true) }
            )
            view.ksDialogTransition = DialogTransition.fade()
            return view
        }

        Loading.shared.kmp.register(UploadLoadingViewModel.self) { _ in
            let view = UILabel()
            view.text = "Uploading"
            return view
        }

        Toast.shared.kmp.register(StatusToastViewModel.self) { viewModel in
            let view = UILabel()
            view.text = viewModel.message
            return view
        }
    }
}
```

`DialogHostRegistration.register()` は、共有コードが何かを表示するより前に main actor のアプリ起動経路から1回呼ぶ。同じ class を登録し直すと factory が置き換わり、factory は表示のたびに呼ばれる。UIKit と SwiftUI の factory は同名で戻り値の型だけが違い、呼び出し側から観察できる挙動はどちらも同じである。

この Swift 向けの入口に、ViewModel の class を渡す表示は無い。共有コードの class を渡す `show` と `registerViewModel` は共有 Kotlin コード専用で、生成される framework の ObjC ヘッダにも現れない ([ViewModel](view-models.md))。Swift から共有 ViewModel を出すときは、Kotlin の ViewModel を Swift 側で作って instance を渡す `show` を呼ぶ。

## 結果型を指定して notifier を読む

共有 ViewModel は結果型を Kotlin 側で宣言していて Swift からは見えないため、この入口は結果型を引数で受け取り、省略すると `Bool` になる。共有の `DialogViewModel<R>` が宣言しているのと同じ型を渡す。食い違いは結果を復元する時点で型付きの失敗として現れる。

| 書き方 | 意味 |
|---|---|
| `register(DeleteViewModel.self) { viewModel, notifier in … }` | 結果型は `Bool` |
| `register(ChoiceViewModel.self, result: String.self) { … }` | 結果型を明示する |
| `show(viewModel, placement:)` / `show(viewModel, result:placement:)` | Swift から表示する。`result:` の省略時は `Bool` |
| `notifier(for:)` / `notifier(for:result:)` | 表示中の共有 ViewModel から結果報告口を取り出す |

1引数 factory は自分で報告口を読む。

```swift
import KsDialogs
import Shared
import UIKit

Dialog.shared.kmp.register(ChoiceViewModel.self, result: String.self) { viewModel in
    let notifier = try? Dialog.shared.kmp.notifier(
        for: viewModel,
        result: String.self
    )
    let view = UIButton(type: .system)
    view.setTitle(viewModel.title, for: .normal)
    view.addAction(
        UIAction { _ in notifier?.complete("accepted") },
        for: .touchUpInside
    )
    return view
}
```

`notifier(for:result:)` は表示していないとき `nil` を返し、指定した結果型が登録時と違うときは throw する。「表示していない」と「型を取り違えている」を混同しないためである。`try?` はその失敗を `nil` に潰すため、型が食い違うとこの button は結果を報告できなくなる。表示中の判定に使う結果型は、その show を始めた時点の登録で固定される。

## Swift 側の表示が投げる失敗を見分ける

KMP 入口の表示は Swift の throwing なので、構成ミスは結果に化けずに `catch` へ届く。届く型は 2 つある。共有 ViewModel の紐付けと結果型にまつわる失敗は `KsDialogsKmpError` で、Dialog の入口だけがこの型に写し替える。それ以外の失敗と、Loading / Toast の入口の失敗は、ライブラリ共通の `DialogError` のまま届く。どちらも Swift の入口が直接投げるもので、次節の「Kotlin の exception が `NSError` として届く」経路とは別である。

| 失敗 | 投げる入口 | 原因と対処 |
|---|---|---|
| `KsDialogsKmpError.notRegistered(viewModelType:)` | `Dialog.shared.kmp.show` | 共有 ViewModel の class に content を登録していない。起動経路の登録を先に走らせる |
| `KsDialogsKmpError.resultTypeMismatch(expected:actual:)` | `Dialog.shared.kmp.show`, `notifier(for:result:)` | 指定した結果型が登録時のものと食い違う。`result:` を共有 `DialogViewModel<R>` の型に合わせる |
| `DialogError.viewFactoryNotRegistered(viewModelType:)` | `Loading.shared.kmp.show`, `Toast.shared.kmp.show` | 共有 ViewModel の class に content を登録していない。Loading と Toast の入口はこの失敗を写し替えず、そのまま投げる |
| `DialogError.viewModelAlreadyShowing(viewModelType:)` | `Dialog.shared.kmp.show` | 表示中と同じ ViewModel インスタンスを重ねて show した。呼び出しごとに新しいインスタンスを作る。先に出ているダイアログは影響を受けない |

Dialog の入口は前の 2 つを `KsDialogsKmpError` として投げ、残りは `DialogError` のまま投げる。写し替えの対象を増やさないため、種類ごとに分けたい場合は 2 つの `catch` を並べる。

出す先の画面が無いことは失敗にならない。iOS の提示先は前面でアクティブなシーンの key window で、それが現れるまで `show` は待つ。待ちに上限は無いので、画面が現れない場所から呼ぶ場合は呼び出し元の Task をキャンセルできるようにする。キャンセルすると、待っている間でも表示中でも `show` は `.cancelled` を返す。

```swift
import KsDialogs
import Shared

@MainActor
func confirmDeleteFromSwift() async -> Bool {
    do {
        switch try await Dialog.shared.kmp.show(DeleteViewModel(itemName: "Report")) {
        case .completed(let value):
            return value
        case .cancelled:
            return false
        }
    } catch let error as KsDialogsKmpError {
        assertionFailure("The registration or the result type does not match: \(error)")
        return false
    } catch {
        assertionFailure("Could not present the dialog: \(error)")
        return false
    }
}
```

## 表示中のページを教える

content に `DialogLayoutArea.currentPage` を添付した Dialog は、表示中のページを基準に配置される ([レイアウト](layout.md))。ページは次の順に問い合わせて決まり、上が候補を持たないときに下へ進む。どこからも得られなければ `.visibleArea` と同じ結果になり、理由が英語の警告ログに出る。

| 順 | 取得元 | 書く名前 |
|---|---|---|
| 1 | SwiftUI の modifier | `View.markAsDialogCurrentPage()` |
| 2 | UIKit 向けの関数の登録 | `DialogCurrentPage.provider` (`(@MainActor () throws -> UIView?)?`) |
| 3 | 既定の探し方 | なし (登録不要) |

どの取得元でも、基準になるのはページの View の safe area の内側と可視領域の共通部分なので、バーの下まで伸びた View でもバーは除かれる。

### 既定の探し方に任せる

何も登録しなくても、ライブラリは Dialog を出す window の view controller を辿り、present された画面・`UINavigationController` の先頭・`UITabBarController` の選択中のタブまで降りた先をページにする。重ねて出した Dialog の器は通り抜けるので、2 枚目の基準も背後の画面のままである。届くのは UIKit のコンテナまでで、root が SwiftUI の画面では `TabView` や `NavigationStack` の内側まで降りず、タブバーを避けない。

### SwiftUI の画面で印を付ける

各画面の中身の枠に 1 回付ける。付けた View は画面に載っている間だけ候補になり、関数の登録と既定の探し方より優先される。候補が複数あるときは、入れ子なら内側、それ以外は最後に画面に載ったものが選ばれ、画面に表示されていないもの (`TabView` の切り替え中に去るタブなど) は外れる。

```swift
import KsDialogs
import SwiftUI

struct RootView: View {
    var body: some View {
        TabView {
            NavigationStack {
                OrdersScreen()
                    .markAsDialogCurrentPage()
                    .navigationTitle("Orders")
            }
            .tabItem { Label("Orders", systemImage: "list.bullet") }

            SettingsScreen()
                .markAsDialogCurrentPage()
                .tabItem { Label("Settings", systemImage: "gearshape") }
        }
    }
}
```

### UIKit の独自コンテナで関数を登録する

既定の探し方が届かない独自のコンテナで画面を切り替えている場合は、表示中のページの View を返す関数を一度登録する。`nil` を代入すると既定の探し方に戻る。関数はメインスレッドで、各表示の開始時と、表示中に window の寸法や safe area が変わったときに呼ばれ、登録の差し替えは次の表示から効く。

```swift
import KsDialogs
import UIKit

final class PagerViewController: UIViewController {
    private var visibleChild: UIViewController?

    override func viewDidLoad() {
        super.viewDidLoad()
        DialogCurrentPage.provider = { [weak self] in
            self?.visibleChild?.view
        }
    }
}
```

関数が `nil` を返す・エラーを投げる・返した View が Dialog を出す window に載っていない・safe area の内側が空・window の外にある、のいずれかなら、既定の探し方の結果を使う。

## Swift boundary に `@Throws` を付ける

Swift から呼ぶ共有 function で exception が外へ出る可能性がある場合は、suspend か non-suspend かを問わず `@Throws` が必要である。annotation がないと Kotlin exception は `NSError` に変換されず、suspend の function では未処理例外で process が終了し、non-suspend の function では Swift 側へ何も伝わらない。

library は各経路が報告できる failure だけを宣言している。message 専用の Loading と Toast route は何も宣言しない。

| 共有 library call | 宣言済み exception |
|---|---|
| `Dialog.instance.show(viewModel)` | `DialogException`, `CancellationException` |
| `Loading.instance.show(viewModel)` | `DialogException`, `CancellationException` |
| `Loading.instance.start(viewModel, action)` | `DialogException`, `CancellationException` |
| `Toast.instance.show(viewModel)` | `DialogException` |
| `Loading.instance.show(message)`, `Loading.instance.start(message, action)`, `Toast.instance.show(message)` | なし |

class を渡す `show` と `start` は Swift から見えないため、この表にも `@Throws` にも現れない。Swift から `Loading.instance.start` を直接呼ぶときは、`actionThread` を明示で渡す。Kotlin の既定値は Swift へ書き出すと消えるためで、共有コードの wrapper から呼べば既定値が使える ([Loading](loading.md))。

自分の exported wrapper が外へ出す可能性のある exception は、それぞれその wrapper に annotation する。

共有 module に置く exported wrapper の例である。`Dialog.instance.show(viewModel)` を包む suspend の wrapper には表の 2 つを annotation し、message 専用の Toast 経路だけを包む wrapper には何も付けない。

```kotlin
package com.example.shared

import jp.kamusoft.ksdialogs.kmp.Dialog
import jp.kamusoft.ksdialogs.kmp.DialogException
import jp.kamusoft.ksdialogs.kmp.DialogResult
import jp.kamusoft.ksdialogs.kmp.KsDialog
import jp.kamusoft.ksdialogs.kmp.KsToast
import jp.kamusoft.ksdialogs.kmp.Toast
import kotlin.coroutines.cancellation.CancellationException

class DeletePresenter(
    private val dialogs: KsDialog = Dialog.instance,
    private val toast: KsToast = Toast.instance,
) {
    @Throws(DialogException::class, CancellationException::class)
    suspend fun confirmDelete(itemName: String): String =
        when (val result = dialogs.show(DeleteViewModel(itemName))) {
            is DialogResult.Completed -> if (result.value) "Deleted" else "Kept"
            DialogResult.Cancelled -> "Cancelled"
        }

    fun notifyDeleted(itemName: String) {
        toast.show("Deleted $itemName")
    }
}
```

その wrapper を Swift から呼ぶ例である。annotation を付けた `confirmDelete` は Swift の throwing function になるので `try await` で呼び、失敗は `NSError` として `catch` に届く。annotation のない `notifyDeleted` は `try` なしで呼ぶ。

```swift
import Shared

@MainActor
func confirmDelete(itemName: String) async -> String? {
    let presenter = DeletePresenter()
    do {
        let outcome = try await presenter.confirmDelete(itemName: itemName)
        presenter.notifyDeleted(itemName: itemName)
        return outcome
    } catch {
        assertionFailure("Could not confirm the delete: \(error)")
        return nil
    }
}
```

共有 module の `iosMain` で、commonMain の interface が `@Throws` を宣言したメンバを override する場合 (テスト用の差し替えなどで `KsDialog`、`KsLoading`、`KsToast` を実装するとき)、override 側に `@Throws` を書き直さない。override は interface の宣言を継承するため Swift 側の throws 契約は変わらず、書き直すと Kotlin 2.4.x では native 系の中間 source set の metadata compile が失敗する (Kotlin 2.5.0 で修正済み)。これはいずれの契約でも同じで、ジェネリックを持つ `KsDialog.show` の override でも失敗する。`commonMain` に書いた override は対象外で、書いても通る。

`iosMain` に置く差し替えの例である。`KsToast.show(viewModel, …)` は `@Throws(DialogException::class)` を宣言しているが、override 側には annotation を書かない。契約には `registry` と class を渡す `show` もあるため、差し替えでは ViewModel factory の表も自分で用意する。

```kotlin
package com.example.shared

import jp.kamusoft.ksdialogs.kmp.DialogPlacement
import jp.kamusoft.ksdialogs.kmp.KsToast
import jp.kamusoft.ksdialogs.kmp.ToastViewModel
import jp.kamusoft.ksdialogs.kmp.ToastViewRegistry
import kotlin.reflect.KClass

class RecordingKsToast : KsToast {
    val shown: MutableList<ToastViewModel> = mutableListOf()

    private val factories: MutableMap<KClass<*>, () -> ToastViewModel> = mutableMapOf()

    override val registry: ToastViewRegistry = object : ToastViewRegistry {
        override fun <VM : ToastViewModel> registerViewModel(
            viewModelClass: KClass<VM>,
            factory: () -> VM,
        ) {
            factories[viewModelClass] = factory
        }
    }

    override fun show(message: String, durationMs: Int?, placement: DialogPlacement?) = Unit

    override fun show(viewModel: ToastViewModel, durationMs: Int?, placement: DialogPlacement?) {
        shown += viewModel
    }

    override fun <VM : ToastViewModel> show(
        viewModelClass: KClass<VM>,
        durationMs: Int?,
        placement: DialogPlacement?,
        configure: ((VM) -> Unit)?,
    ) {
        @Suppress("UNCHECKED_CAST")
        val viewModel = factories.getValue(viewModelClass)() as VM
        configure?.invoke(viewModel)
        show(viewModel, durationMs, placement)
    }
}
```

## 機能別レシピへ進む

- 返した content に添付する option と placement、表示中のページを基準にする配置は [レイアウト](layout.md)。
- 出入りの演出は [トランジション](transitions.md)。
- 共有コード側は [Dialog](dialogs.md)・[Loading](loading.md)・[Toast](toast.md)。
