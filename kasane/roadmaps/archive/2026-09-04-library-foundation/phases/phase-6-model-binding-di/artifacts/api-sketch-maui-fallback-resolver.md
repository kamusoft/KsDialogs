# API スケッチ: MAUI fallback resolver (SetIocConfig 相当の後継、2026-08-24)

論点③-2 の決定 (C 案採用) 時点の完成系イメージ。**名前はすべて仮 — spec 化で確定する。**

前提となる決定: core/ADR-0004 (明示レジストリ + MAUI 糖衣の存置)・core/ADR-0018 (notifier の VM 注入)・core/ADR-0021 (VM factory のレジストリ解決)・maui/ADR-0005 (本スケッチの決定)。

## 設定側 — MauiProgram.cs

```csharp
builder.Services
    .AddKsDialogs(options =>
    {
        // ① View の一括解決規約 (原典 SetIocConfig の viewTypeGetter + viewResolver 相当)
        //    未登録の VM 型が来たときだけ呼ばれる。null を返したら「解決できない」
        options.UseViewFallback((vmType, sp) =>
        {
            var viewTypeName = vmType.FullName!.Replace("ViewModel", "Dialog");
            var viewType = vmType.Assembly.GetType(viewTypeName);
            return viewType is null
                ? null
                : (View)ActivatorUtilities.CreateInstance(sp, viewType);
        });

        // ② VM の一括解決 (型指定呼び出しで VM factory 未登録のとき)
        //    既定実装を用意: IServiceProvider から引くだけ
        options.UseViewModelFallback();   // = (vmType, sp) => sp.GetService(vmType)
    })
    // 明示登録は従来どおり書ける。解決は常にこちらが最優先
    .RegisterForDialog<OKDialog, OKDialogViewModel>();
```

## 利用側 — 呼び出し面は変わらない

```csharp
// 明示登録なしでも、命名規約 (XxxViewModel → XxxDialog) で解決される
var result = await Dialog.Instance.ShowAsync(new ConfirmViewModel());

// 型指定呼び出しも VM が DI に登録されていれば動く (②の fallback 経由)
var input = await Dialog.Instance.ShowAsync<TextInputViewModel>(vm => vm.Placeholder = "名前");
```

## 解決順序 (レジストリの中で閉じる)

```
show の解決:  ① 明示レジストリ (Register / RegisterForDialog)
                    ↓ 未登録なら
              ② fallback resolver (設定されていれば)
                    ↓ null または未設定なら
              ③ 構成ミスとして失敗 (例外。cancelled に化けない — ADR-0004 の原則)
```

## 原典との対応 (移行者向け)

| 原典 | C 案 |
|---|---|
| `Dialog.SetIocConfig(viewTypeGetter, viewResolver)` — static・後勝ち・null 上書きの粗 | `AddKsDialogs(options => ...)` — DI チェーン上の一箇所設定。static 差し込み口が存在せず上書き事故が構造的に起きない |
| viewResolver が View と VM の解決を暗黙に兼務 | View fallback (①) と VM fallback (②) を明示分離 |
| 個別登録との優先順位が不明瞭 | 明示登録 → fallback → 失敗の順を仕様として規定 |
