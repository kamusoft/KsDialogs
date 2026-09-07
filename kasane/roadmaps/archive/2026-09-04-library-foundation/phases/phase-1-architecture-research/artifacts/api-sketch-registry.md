# API スケッチ: VM 型キー → View factory レジストリ (2026-08-13)

論点「DI 差し込み方式」の合意時に提示した書き味スケッチ。**命名・細部は仮** — 正式な API 設計は phase-4 (vertical-slice) の提案で確定する。core/ADR-0004 の補助資料。

## ① KMP — 共有層 Presenter から呼ぶ (この形態の核心シナリオ)

```kotlin
// commonMain — ライブラリ利用者の共有 ViewModel
class ConfirmDeleteViewModel(val itemName: String)

// commonMain — 共有層の Presenter。契約 interface を注入 (テストでは fake に差し替え)
class ItemPresenter(private val dialogs: KsDialogs) {
    suspend fun onDeleteRequested(item: Item) {
        // 結果通知 (core/ADR-0003): suspend + sealed で結果が返る
        when (val r = dialogs.show<ConfirmDeleteViewModel, Boolean>(ConfirmDeleteViewModel(item.name))) {
            is DialogResult.Completed -> if (r.value) repository.delete(item)
            is DialogResult.Cancelled -> Unit
        }
    }
}
```

```kotlin
// Android アプリの起動時 (androidMain / Application.onCreate)
KsDialogs.registry.register(ConfirmDeleteViewModel::class) { vm ->
    ConfirmDeleteDialogView(vm)   // Kotlin Native 実装の View (Compose でも OK)
}
```

```swift
// iOS アプリの起動時 (Swift) — 同じ共有 VM に Swift Native の View を紐付け
KsDialogs.registry.register(ConfirmDeleteViewModel.self) { vm in
    ConfirmDeleteDialogView(viewModel: vm)
}
```

共有 Presenter は View の存在を知らず「VM を投げたら結果が返る」だけ。どの View が出るかは各 OS 側の登録が決める — 「共有 ViewModel と Native View の紐付け」の実体。

## ② 純 Swift ネイティブ利用 (KMP なし)

```swift
// 登録 (起動時)
KsDialogs.registry.register(SettingsViewModel.self) { SettingsDialogView(viewModel: $0) }

// 呼び出し — リフレクション不要、メタタイプがキー
let result = await KsDialogs.shared.show(SettingsViewModel(), expecting: Bool.self)
switch result {
case .completed(let ok): save(ok)
case .cancelled: break
}

// View 型を直接指定する呼び方 (原典の ShowAsync<TView> 相当) なら登録不要
let r = await KsDialogs.shared.show(SettingsDialogView.self)
```

## ③ MAUI — 素のレジストリ + 原典互換の糖衣

```csharp
// 素のレジストリ
KsDialogs.Registry.Register<ConfirmDeleteViewModel>(vm => new ConfirmDeleteDialogView(vm));

// 原典互換の糖衣 — SetIocConfig 相当。Prism 等のコンテナ連携は従来どおり
// (内部では「全 VM 型をこの関数ペアで解決する」一括委譲としてレジストリに載る)
KsDialogs.SetIocConfig(
    viewTypeGetter: vmType => registry.GetViewType(vmType),
    viewResolver: type => container.Resolve(type));    // 原典の null 上書きの粗は修正
```

## ④ テストでの差し替え (公開 API 形状 core/ADR-0002 の両対応が効く)

```kotlin
// Presenter の単体テスト — ダイアログを実際に出さず「OK が押された」ことにする
class FakeDialogs : KsDialogs {
    override suspend fun <VM : Any, R> show(vm: VM): DialogResult<R> =
        DialogResult.Completed(true as R)
}
val presenter = ItemPresenter(FakeDialogs())
```

## 疎通確認メモ

①の iOS 登録が成立するのは「commonMain の VM クラスが ObjC クラスとして Swift から見える」ためで、クラス同一性がキーとして機能する (`@objc` 互換面と同じ仕組み)。**phase-4 縦串スライスで最優先の疎通確認ポイント。**
