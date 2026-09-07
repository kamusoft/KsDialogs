# ViewModel から結果を報告する

Dialog の結果は、表示中の ViewModel が拡張プロパティ `Notifier` から報告する。この文書は `Notifier` の読み方と、ViewModel の型だけを渡す `ShowAsync` の使い方を扱う。

## `Notifier` を読む

`Notifier` は表示中だけ値を持つ。ViewModel 自身の中からは `this.Notifier` と書き、`?.` で呼ぶ。

| 項目 | 内容 |
|---|---|
| 型 | ViewModel が宣言した結果型の `DialogNotifier<TResult>` |
| show の前後 | `null` になる (表示中だけ値を返す) |
| 紐付く show 経路 | instance 渡し・インライン factory・型指定のすべて |
| 2 引数 factory との関係 | ViewModel から読んだ notifier と factory 引数の notifier は同じ配送先を指す |

以下は真偽値を結果とする ViewModel で、`Accept` が完了を、`Cancel` がキャンセルを報告する。

```csharp
using KsDialogs;
using Microsoft.Maui.Controls;

namespace MyApp;

public sealed class ConfirmViewModel : IDialogViewModel
{
    public string Message { get; set; } = string.Empty;

    public void Accept() => this.Notifier?.Complete(true);

    public void Cancel() => this.Notifier?.Cancel();
}
```

## ViewModel は class にする

`Notifier` の紐付けは instance の同一性で引くため、ViewModel は参照型でなければならない。

- 登録と型指定 show には `where TViewModel : class` 制約があり、値型の ViewModel はそこでコンパイルできない
- interface を受ける instance 渡しの show だけは実行時に検査し、`DialogException.ValueTypeViewModel` で拒否する

## 同じ instance を重ねて表示しない

1 つの instance が持てる notifier の紐付けは 1 つだけである。同じ instance を並行して表示すると `DialogException.ViewModelAlreadyShowing` で失敗する。同じ型でも別 instance なら独立して重なる。

## 型から ViewModel を生成して configure する

型指定 `ShowAsync` は ViewModel の型だけを受け取り、登録済みの ViewModel factory が作った instance を `configure` してから表示する。

### 必要な登録

レジストリのエントリは独立した 2 slot を持つ。

| slot | 入れる API | 使う show |
|---|---|---|
| View factory | `Register` | instance 渡しと型指定の両方 |
| ViewModel factory | `RegisterViewModel` | 型指定だけ |

型指定 `ShowAsync` は両方を必要とする。再登録は触れた slot だけを置換し、もう片方は残る。解決は show を呼んだ時点のスナップショットなので、表示中に再登録しても出ている Dialog には影響しない。明示した ViewModel factory と fallback ([DI 登録](di-registration.md)) のどちらでも型を解決できない場合、show は提示前に `DialogException.ViewModelFactoryNotRegistered` で失敗する。

### 生成から表示までの順序

順序は次に固定されている。

1. ViewModel の生成
2. `configure` の完了
3. notifier の紐付け
4. content View の生成

したがって configure は content factory が状態を読むより前に完了する。ViewModel factory や `configure` が投げた失敗は cancelled になるのではなく、呼び出し元へ伝播する。

以下は結果型に `string` を宣言した ViewModel を 2 slot とも登録し、型指定 `ShowAsync` の `configure` で `Prompt` を入れてから表示する例である。

```csharp
using KsDialogs;
using Microsoft.Maui.Controls;

namespace MyApp;

public sealed class ProfileViewModel : IDialogViewModel<string>
{
    public string Prompt { get; set; } = string.Empty;
}

public static class ProfileDialogs
{
    public static void Register()
    {
        Dialog.Instance.Registry.Register<ProfileViewModel, string>(viewModel =>
            new Button
            {
                Text = viewModel.Prompt,
                Command = new Command(() => viewModel.Notifier?.Complete("accepted")),
            });
        Dialog.Instance.Registry.RegisterViewModel<ProfileViewModel, string>(() => new ProfileViewModel());
    }

    public static Task<DialogResult<string>> ShowAsync() =>
        Dialog.Instance.ShowAsync<ProfileViewModel, string>(viewModel =>
        {
            viewModel.Prompt = "Continue?";
        });
}
```

## 表示前に非同期で configure する

`configure` には同期の `Action<TViewModel>` と非同期の `Func<TViewModel, Task>` の 2 通りがある。content factory を呼ぶ前に初期状態をロードする場合は非同期 overload を使う。`ShowAsync` を呼ぶ前に両方の factory を一度登録すると、content factory は configure 完了後の状態を読み取る。

以下は真偽値を結果とする ViewModel で、非同期の `configure` が `Name` を入れてから content View を作る例である。

```csharp
using KsDialogs;
using Microsoft.Maui.Controls;

namespace MyApp;

public sealed class AccountViewModel : IDialogViewModel
{
    public string Name { get; set; } = string.Empty;
}

public static class AccountDialogs
{
    public static void Register()
    {
        Dialog.Instance.Registry.Register<AccountViewModel>(viewModel =>
            new Button
            {
                Text = viewModel.Name,
                Command = new Command(() => viewModel.Notifier?.Complete(true)),
            });
        Dialog.Instance.Registry.RegisterViewModel<AccountViewModel>(() => new AccountViewModel());
    }

    public static Task<DialogResult<bool>> ShowAsync() =>
        Dialog.Instance.ShowAsync<AccountViewModel>(async viewModel =>
        {
            await Task.Yield();
            viewModel.Name = "Ada";
        });
}
```

## Loading と Toast でも同じ形で使える

型を渡す表示は Dialog 専用ではなく、Loading と Toast にも同じ形で用意されている。共通するのは、ViewModel factory で解決すること、解決が show を呼んだ時点のスナップショットであること、「生成 → `configure` の完了 → content View の生成 → 表示」の順序、そして ViewModel factory 未登録が `DialogException.ViewModelFactoryNotRegistered` になることである。Loading には action を渡すスコープ形の型指定版 `StartAsync<TViewModel>` もある。

機能ごとに違うのは次の点である。

| 機能 | `configure` | content View の生成前に挟まる紐付け | ViewModel factory と `configure` の失敗 |
|---|---|---|---|
| Dialog | 同期と非同期 | notifier | 表示に進まず呼び出し元へ伝播する |
| Loading | 同期と非同期 | 進捗受け口。合流の判定は `configure` の完了後で、合流側になった呼び出しの ViewModel は表示に使われない | 表示にも合流にも進まず伝播し、`StartAsync` の action も実行されない |
| Toast | 同期のみ | なし | `Show` は既に戻っているため伝播せず、警告を残してその 1 枚だけが破棄される |

fallback 解決が効くのは Dialog レジストリだけで、Loading と Toast は明示登録か 1 行登録だけを見る ([DI 登録](di-registration.md))。書き方は [Loading](loading.md) と [Toast](toast.md) にある。

呼ぶ側から見ればこれは追加であり、instance を渡す show・インライン factory・2 引数 factory の登録はそのまま使える。ただし `IKsDialog` / `IKsLoading` / `IKsToast` を自分で実装している型 (テスト用の代用実装や adapter) は、契約に増えたメンバーを実装しないと再コンパイルが通らない。
