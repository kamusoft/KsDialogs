---
type: concept
title: MAUI の DI 連携と登録糖衣 (RegisterForDialog / RegisterForLoading / RegisterForToast と fallback resolver)
description: MAUI 限定の登録・解決の公開契約 — 1行登録 RegisterForDialog / RegisterForLoading / RegisterForToast の配線 (View / VM factory の自動登録と BindingContext の同一性)・AddKsDialogs と fallback resolver の解決順序 (明示 → fallback → 失敗。Dialog 限定)・fallback 設定の合成と持続・構成ミスの失敗の種類
tags: [maui, di, registration, fallback, contract]
timestamp: 2026-09-08
---

# MAUI の DI 連携と登録糖衣 (RegisterForDialog / RegisterForLoading / RegisterForToast と fallback resolver)

この文書は、MAUI (C#) だけが持つ登録・解決の糖衣の公開契約を定める。読むと、`RegisterForDialog` (と Loading / Toast 版の `RegisterForLoading` / `RegisterForToast`) の1行で何が配線されるか、`AddKsDialogs` の fallback resolver が未登録の型をどの順序で解決するか、設定がどこに載っていつまで効くか、構成ミスがどう失敗するかが分かる。

先に core の [ViewModel 主導の呼び出しのルール](../../core/api/model-binding-semantics.md) (型指定 show・VM factory・`vm.Notifier`) と [登録と表示の呼び出し面のルール](../../core/api/registration-show-semantics.md) (register / show の基本形) を読むと分かりやすい。この文書はその MAUI 固有の上乗せだけを扱う。

**この文書が正であり、実装はここに合わせる**。根拠決定は [maui/ADR-0005](../../../decisions/maui/0005-fallback-resolver-sugar.md) (fallback resolver の採用と static 差し込み口の廃止)・[core/ADR-0021](../../../decisions/core/0021-vm-factory-registry-resolution.md) (VM factory による解決)・[core/ADR-0035](../../../decisions/core/0035-loading-toast-typed-show-vm-factory.md) (Loading / Toast のレジストリへの VM factory スロット追加)。

この糖衣は **MAUI 限定**である。Native (Swift / Kotlin) / KMP には持ち込まない — Swift / Kotlin はクロージャの型推論で factory 登録が既に1行なのに対し、C# は部分的型引数推論を持たないため、低水準の `Register` では型引数とラムダの両方を書かされて原典 (移植元 AiForms.Maui.Dialogs) 比で書き味が後退する。その回復がこの糖衣の存在理由であり、Native にはその後退がない。

前提となる**レジストリ**の構造: レジストリは ViewModel の型をキーに、**View factory と VM factory の2スロット** (キーごとに埋まる2つの登録枠) を持つ対応表で、Dialog / Loading / Toast の3つが独立に存在する (公開型 `DialogViewRegistry` / `LoadingViewRegistry` / `ToastViewRegistry`。既定の実体は各 `Shared`)。View factory は表示のたびに中身の View を作る関数、VM factory は ViewModel の型だけを渡す**型指定 show** のために ViewModel を作る関数である。

## 全体像

MAUI アプリの `MauiProgram` では、次の1チェーンで「登録 + DI 連携 + 型指定 show の有効化」まで済む:

```csharp
builder.Services
    .AddKsDialogs(o => o.UseViewModelFallback())                 // 初期化サービスの登録 + fallback 設定 (o は KsDialogsOptions。省略可)
    .RegisterForDialog<ConfirmDialogView, ConfirmViewModel>()    // 1行登録 (チェーン可能)
    .RegisterForLoading<UploadLoadingView, UploadViewModel>()    // Loading 版 (Loading 専用レジストリに載る)
    .RegisterForToast<NoticeToastView, NoticeViewModel>();       // Toast 版 (Toast 専用レジストリに載る)
```

`Register` / `RegisterViewModel` (factory を自分で書く形) は低水準 API として残っており、糖衣を使わない構成も従来どおり成立する。

## 1行登録 (`RegisterForDialog<TView, TViewModel>`)

`IServiceCollection` 拡張。1回の呼び出しでレジストリの2スロット (View factory / VM factory) を両方配線し、サービス登録も済ませる。

### View factory の配線

show のたびに TView を生成する。生成は `ActivatorUtilities.CreateInstance` で行い (コンストラクタ依存はサービスから解決される)、**現在 show されている VM インスタンスを明示引数として渡す** — TView のコンストラクタが TViewModel を受ける構成なら show 対象の VM がそのまま注入され、**コンストラクタに渡る VM・BindingContext・show 対象は同一インスタンス、VM の追加生成はゼロ**になる。生成した View には BindingContext = 現在の VM が設定される。

この生成に失敗した場合 (コンストラクタが要求する依存がサービスに無い等) は、`DialogException.ViewCreationFailed` として報告する。元の失敗は `InnerException` にそのまま残り、組み立てようとした View と ViewModel の型名を `ViewTypeName` / `ViewModelTypeName` で読める。「factory が登録されていない」とは原因も直し方も違うため、既存の `ViewFactoryNotRegistered` には寄せない。包むのはライブラリ自身が View を組み立てるこの経路だけで、利用者が書いた factory (`Register` / インライン show) や fallback resolver が投げた例外は包まずそのまま届く (resolver は利用者コードで、View の型名も知り得ない)。Dialog / Loading では show (start) がこの例外で失敗し、iOS / Android の実機経路でもユニットテストの fake gateway 経路でも同じ型が届く (Android 側の経路は [core/ADR-0036](../../../decisions/core/0036-maui-android-content-supply-symmetry.md))。Toast は Show が戻り値を持たないため呼び出し元へは返さず、既存の受理後の失敗モデル (警告 + その 1 枚だけ破棄、後続は継続) のまま、警告に `ViewCreationFailed` が原因として残る。

### VM factory の配線

`IServiceProvider.GetRequiredService<TViewModel>()` で解決する factory を登録する。これにより型指定 show (`ShowAsync<TViewModel>(configure)`。configure は表示前に生成した VM を設定する省略可のコールバック) が追加手数ゼロで有効になる。

### サービス自動登録

TView / TViewModel を **TryAdd・transient** で `IServiceCollection` に登録する (show 毎回生成の使い捨てモデル — [core/ADR-0005](../../../decisions/core/0005-no-view-reuse-mechanism.md) — と整合)。利用者が同じ型を先に登録していればそちらが尊重される。ただし **TView のサービス登録は表示時の View 生成には使われない** (生成は常に上記のコンストラクタ経由) — TView 登録の意味はコンストラクタ依存の解決までである。

### カスタム結果型と `AddKsDialogs` の省略

カスタム結果型の VM には対の `RegisterForDialog<TView, TViewModel, TResult>()` を使う。ViewModel 契約 `IDialogViewModel<TResult>` ([ViewModel 主導の呼び出しのルール](../../core/api/model-binding-semantics.md)) に対して、C# では「何らかの `IDialogViewModel<>` 実装」という制約が書けないため、既存 `Register` と同じ2形分岐になる。

`AddKsDialogs` を呼ばず `RegisterForDialog` 単独でも動く (初期化サービスの登録は冪等に行われる)。

### Loading / Toast の 1行登録 (`RegisterForLoading` / `RegisterForToast`)

`RegisterForLoading<TView, TViewModel>()` / `RegisterForToast<TView, TViewModel>()` は `RegisterForDialog` と同じ配線 (View factory の生成規則と BindingContext の設定・サービスから引く VM factory・TryAdd transient のサービス自動登録・初期化サービスの冪等登録) を、それぞれ Loading 専用レジストリ / Toast 専用レジストリに対して行う ([core/ADR-0035](../../../decisions/core/0035-loading-toast-typed-show-vm-factory.md))。この1行だけで、インスタンス渡しの表示と型指定 show / start ([MAUI の Loading 公開面](loading-surface.md) / [MAUI の Toast 公開面](toast-surface.md)) の両方が使える。3 つのレジストリは独立しているため、同じ ViewModel 型を Dialog / Loading / Toast へ別々の View で登録できる。

Dialog 版との違いは 2 点: 結果型の型引数を取る 3 型引数版は無い (Loading / Toast は結果を返さない)。fallback resolver は効かない (次節。Loading / Toast は明示登録か1行登録のみで、未登録は即失敗する)。

## fallback resolver (`AddKsDialogs(options)`)

型ごとの明示登録なしで規約ベースの一括解決 (VM 名 → View 名の命名規約など) をしたいアプリ向けの差し込み口。**Dialog レジストリだけの機構**で、Loading / Toast のレジストリには適用されない。**View の fallback と VM の fallback は明示的に別スロット**で、どちらも options で設定して初めて有効になる (未設定 = 無効):

| スロット | 設定 API | 意味 |
|---|---|---|
| View fallback | `options.UseViewFallback(Func<Type, IServiceProvider, View?> resolver)` | 未登録の VM の**型**と provider を受けて View インスタンスを作る関数 (null を返す = 解決不能)。fallback が返した View にも**ライブラリが BindingContext = 現在の VM インスタンスを設定する** (関数自身は型しか受け取らなくてよい) |
| VM fallback | `options.UseViewModelFallback()` (引数なし = 既定実装「サービス (`IServiceProvider.GetService`) から解決」) または `UseViewModelFallback(Func<Type, IServiceProvider, object?> resolver)` | 型指定 show で VM factory が未登録のときの解決 |

### 解決順序 (仕様)

**明示登録 → fallback → 構成ミスとして失敗** (`DialogException.ViewFactoryNotRegistered`。`DialogException` の入れ子クラスで種別を表す — 後述の「構成ミスの失敗の種類」。cancelled 等の結果に化けない)。明示登録が常に勝ち、判定は View / VM のスロットごとに独立に行われる (明示 VM factory + View fallback のような組み合わせも成立する)。static な設定差し込み口は存在しない — 原典 (移植元 AiForms.Maui.Dialogs) の static 一括設定 API `SetIocConfig` にあった「後勝ちで **null が既存設定を潰す**」粗を構造ごと消した形である (経緯は [maui/ADR-0005](../../../decisions/maui/0005-fallback-resolver-sugar.md))。

fallback が解決できないことは呼んでみるまで分からないため、この失敗が観察されるのは View 生成前・提示処理に入った直後である (View は生成されない)。

### 設定の合成と持続

`AddKsDialogs` の再呼び出しは **null でないスロットだけを合成する** — `UseViewFallback` 設定後に引数なしの `AddKsDialogs()` が来ても設定は消えず、View / VM を別々の呼び出しで設定しても両方残る。明示登録の「スロット単位の後勝ち」と同じ意味論であり、null スロットは上書きしないため原典の「null が既存設定を潰す」粗は再現しない (同じ側のスロットを2回設定した場合は後の設定が勝つ)。

設定は process 内で共有される Dialog レジストリの既定の実体 (`DialogViewRegistry.Shared`。公開型) に載り、**公開 API から解除する手段はない** (process 内で持続する)。1つのテストプロセスで複数の host を組み立てる利用者のテストでは、先の検証の fallback が次へ漏れることに注意。

#### provider ホルダと初期化サービス

`AddKsDialogs` は `IServiceProvider` をアプリ起動時に保持する初期化サービス (`IMauiInitializeService` 相当。MAUI 標準の初期化機構) を登録する。この登録は `RegisterForDialog` / `RegisterForLoading` / `RegisterForToast` も冪等に行うため、fallback を使わないアプリでは `AddKsDialogs` の呼び出し自体を省ける — `AddKsDialogs` を明示的に呼ぶ意味は fallback などの options 設定である。

ホルダは**最後に初期化されたアプリの provider** を保持し、複数 MauiApp の並存はサポート外 (動作は最後に初期化された provider に依存する)。provider が要る解決 (DI 由来の VM factory・VM fallback) を provider 確立前に呼ぶと `DialogException.ServiceProviderUnavailable` で失敗する (Loading / Toast の1行登録が配線した VM factory も同じ)。

## 構成ミスの失敗の種類

いずれも `DialogException` の入れ子クラスの例外 (faulted Task。Toast は同期 throw) であり、cancelled 等の結果に化けない ([結果通知のルール](../../core/api/result-notification-semantics.md) の原則)。Dialog / Loading / Toast のどの経路でも同じ例外型で返る:

| 失敗 | 意味 |
|---|---|
| `ViewFactoryNotRegistered` | 明示登録も fallback も View を解決できない |
| `ViewModelFactoryNotRegistered` | 型指定 show で VM factory が未登録 (Dialog では VM fallback も無効。Loading / Toast には fallback が無い) |
| `ViewModelAlreadyShowing` | 同一 VM インスタンスの並行 show |
| `ValueTypeViewModel` | 値型 (struct) の VM が show に渡された — 登録・型指定 show はコンパイル時 (`where TViewModel : class`) に弾かれるため、実行時にここへ来るのはインスタンス渡し show だけ |
| `ServiceProviderUnavailable` | DI 解決が要る経路を provider 確立前に呼んだ |
| `ViewCreationFailed` | 1 行登録が結び付けた View をライブラリが組み立てられなかった (元の失敗は `InnerException`)。利用者コードが投げた例外は包まれない。Toast では呼び出し元へ返らず警告に原因として残る |

## 保証すること

- **1行登録した TView のコンストラクタ・BindingContext・show 対象の VM は同一インスタンスで、VM の生成は show 1回につき1回を超えない**。ここが崩れると「コンストラクタで受けた VM と画面にバインドされた VM が別物」という発見困難な不整合になる
- **明示登録は fallback より常に優先される**。fallback の導入が既存の明示登録の挙動を変えることはない
- **`AddKsDialogs` の再呼び出しが設定済みの fallback を黙って消すことはない** (非 null スロットの合成)
- **fallback 経由でも core 契約 (VM への結果報告口 `vm.Notifier` の供給・結果通知・値型 VM の拒否) は同一に働く**。fallback が返した View にも BindingContext として現在の VM が入るため、View はそこから `vm.Notifier` を引いて結果を報告できる

## してはいけないこと

- **static な一括設定口を追加しない**。後勝ち・null 上書きの粗を構造的に排除した決定 (maui/ADR-0005) に反する
- **fallback を「登録の代わり」として常用しない**。fallback が返す View は明示登録の型検査を通らないため、規約ミス (名前不一致・型不整合) の検出が実行時まで遅れる。個別に書ける登録は `RegisterForDialog` で書く
- **TView をサービス登録すれば表示時の生成に使われる、と説明しない**。生成は常に「現在 VM を明示引数に渡すコンストラクタ経由」であり、サービス登録の意味は依存解決までである
- **値型 VM の拒否をどれかの show 経路だけ免除しない** (core の [ViewModel 主導の呼び出しのルール](../../core/api/model-binding-semantics.md) の禁止事項と同じ)

## 用語

- **1行登録** — `RegisterForDialog<TView, TViewModel>()` と、その Loading / Toast 版 `RegisterForLoading` / `RegisterForToast`。View factory + VM factory + サービス自動登録を1呼び出しで配線する糖衣
- **fallback resolver** — 明示レジストリで解決できなかったときだけ呼ばれる一括解決関数 (View 用 / VM 用の2スロット。Dialog レジストリ限定)
- **provider ホルダ** — `AddKsDialogs` が登録する初期化サービスが、起動時の `IServiceProvider` をライブラリ内に保持する仕組み (公開型ではない)
- **スロット** — レジストリの1つのキー (ViewModel 型) に対して埋まる登録枠。明示登録の View factory / VM factory と、fallback の View 用 / VM 用の2組がある
- **低水準 API** — factory を自分で書く `Register` / `RegisterViewModel` と、インスタンス渡し show / インライン show。糖衣なしでも全機能に到達できる面
- **インスタンス渡し show / インライン show** — VM のインスタンスを渡す show / 登録せず factory をその場で渡す show ([登録と表示の呼び出し面のルール](../../core/api/registration-show-semantics.md))

## 関連

### MAUI の公開面

- [MAUI の Dialog 公開面](dialog-surface.md) — 登録・表示・結果の受け取りの公開名と署名
- [MAUI のレイアウト公開面](layout-surface.md) — 添付プロパティと属性の型
- [MAUI のトランジション公開面](transition-surface.md) — 演出の添付と型
- [MAUI の Loading 公開面](loading-surface.md) — Loading の公開名・型指定 show / start と `RegisterForLoading` の 1 行登録
- [MAUI の Toast 公開面](toast-surface.md) — Toast の公開名・型指定 Show と `RegisterForToast` の 1 行登録

### core の契約と決定記録

- [ViewModel 主導の呼び出しのルール](../../core/api/model-binding-semantics.md) — 型指定 show・`vm.Notifier`・参照型限定 (core 契約)
- [登録と表示の呼び出し面のルール](../../core/api/registration-show-semantics.md) — register / show の基本形
- [結果通知のルール](../../core/api/result-notification-semantics.md) — 構成ミスは失敗で返す原則
- [maui/ADR-0005](../../../decisions/maui/0005-fallback-resolver-sugar.md) — 決定 (fallback resolver の採用・解決順序の仕様化)
- [maui/ADR-0001](../../../decisions/maui/0001-call-scoped-bridge.md) — 決定 (MAUI レジストリを C# 層に持つ構成)
- [core/ADR-0021](../../../decisions/core/0021-vm-factory-registry-resolution.md) — 決定 (VM factory による解決)
- [core/ADR-0005](../../../decisions/core/0005-no-view-reuse-mechanism.md) — 決定 (show 毎回生成の使い捨てモデル。transient 登録の根拠)
- [core/ADR-0035](../../../decisions/core/0035-loading-toast-typed-show-vm-factory.md) — 決定 (Loading / Toast のレジストリへの VM factory スロット追加。`RegisterForLoading` / `RegisterForToast` の VM factory 配線の根拠)
