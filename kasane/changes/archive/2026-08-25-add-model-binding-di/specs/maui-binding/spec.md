# maui-binding デルタ (add-model-binding-di)

MAUI は C# 側に独立した registry / presenter を持つため (maui/ADR-0001)、dialog-contract の挙動 (MB-NI-*/MB-TS-*) のうち MAUI 実装に載るものは MAUI テストでも同名検証する。本書は MAUI 公開面と DI 糖衣の差分。

## ADDED Requirements

### Requirement: C# 公開面の VM 供給と型指定呼び出し

VM からの notifier 取得は拡張メンバー (`vm.Notifier` 相当) で提供し、宣言結果型で型付けされる (SHALL)。登録は VM 引数のみの factory (`Func<TViewModel, View>`) を追加し、従来の2引数 factory は低水準 API として残る。VM factory の登録 API と型指定 show を追加する。型指定 show は既存 Register と同型の**2型引数形** — bool 省略形 `ShowAsync<TViewModel>(configure)` (TViewModel : IDialogViewModel) とカスタム結果型 `ShowAsync<TViewModel, TResult>(configure)` — とし、同期 / 非同期 configure・省略可。型指定 show・登録・notifier 取得のジェネリック制約には `class` を含め、値型 VM をコンパイル時に拒否する (参照型限定の C# 表現)。VM factory と configure は UI スレッドで実行される。型指定 show の各オーバーロードは、既存のインスタンス渡し show・インライン show と同名 (ShowAsync) のまま曖昧さなく解決される (core/ADR-0020)。

#### Scenario: [MB-MA-01] 公開面の正の compile 検査
- **GIVEN** 利用者コードの立場の compile 検査ソース
- **WHEN** 1引数 factory 登録・VM factory 登録・`vm.Notifier` の宣言結果型での参照・型指定 show の2型引数形 (bool 省略形 / カスタム結果型・configure あり / なし・非同期 configure)・既存のインスタンス渡し / インライン show を同一ソース内に記述する
- **THEN** すべてコンパイルが通り、オーバーロードが意図した経路に解決される

#### Scenario: [MB-MA-02] 意図しない形式は negative compile check で拒否される
- **GIVEN** negative check ソース
- **WHEN** 契約に準拠しない型での型指定 show・結果型の不一致な notifier 参照・struct の VM による登録と型指定 show を記述する
- **THEN** コンパイルが拒否される (struct は class 制約違反として)

### Requirement: 1行登録 (RegisterForDialog)

IServiceCollection 拡張 `RegisterForDialog<TView, TViewModel>()` は、View factory と VM factory の両方をレジストリに配線し、チェーン可能である (SHALL)。TView / TViewModel は TryAdd・transient でサービス自動登録し、利用者の既存登録を尊重する。TView の生成は現在の VM インスタンスを明示引数に渡して行う — TView のコンストラクタが TViewModel を受ける場合は show 対象の VM がそのまま注入され、コンストラクタ側と BindingContext は同一インスタンス・VM の生成は1回である。素の Register (factory 形) は低水準 API として残る。

#### Scenario: [MB-MA-03] 1行登録したペアがインスタンス渡し show で表示される
- **GIVEN** `RegisterForDialog<TView, TViewModel>()` だけを登録したアプリ構成 (TView / TViewModel の個別サービス登録なし)
- **WHEN** VM インスタンスを show し、View が VM 経由の notifier で報告する
- **THEN** TView が BindingContext = VM で表示され、報告した結果が返る

#### Scenario: [MB-MA-04] 1行登録だけで型指定 show が DI 解決の VM で動く
- **GIVEN** コンストラクタ依存 (サービス登録済み) を持つ TViewModel を `RegisterForDialog` した構成
- **WHEN** 型指定 show を configure 付きで呼ぶ
- **THEN** 依存が注入された VM が生成され、configure 適用後に表示され、結果が返る

#### Scenario: [MB-MA-09] VM をコンストラクタで受ける TView に同一インスタンスが1回だけ届く
- **GIVEN** TViewModel をコンストラクタで受け取る TView を `RegisterForDialog` した構成
- **WHEN** VM インスタンスを show する
- **THEN** TView のコンストラクタに渡る VM と BindingContext と show 対象は同一インスタンスで、VM の生成は発生しない (show に渡した1つだけ)

### Requirement: fallback resolver

`AddKsDialogs(options)` で View fallback (未登録 VM 型 → View、null = 解決不能) と VM fallback (既定実装はサービスから解決) を設定できる (SHALL)。解決順序は「明示レジストリ → fallback → 構成ミスとして失敗」であり、明示登録が常に優先される。fallback の判定は View / VM のスロットごとに独立して適用される。View fallback が返した View にはライブラリが BindingContext = 現在の VM を設定する。fallback が null を返した場合・未設定の場合は構成ミスとして失敗する (cancelled 等の結果に化けない)。static な設定差し込み口は提供しない。

#### Scenario: [MB-MA-05] 明示登録が fallback より優先される
- **GIVEN** ある VM 型を明示登録し、同じ型も解決できる View fallback を設定した構成
- **WHEN** その VM を show する
- **THEN** 明示登録の View が表示され、fallback は呼ばれない

#### Scenario: [MB-MA-06] 未登録 VM 型が View fallback で解決され表示される
- **GIVEN** View fallback (命名規約で View を生成する) を設定し、明示登録のない VM 型
- **WHEN** その VM を show し、View が VM 経由の notifier で報告する
- **THEN** fallback が生成した View が表示され、報告した結果が返る

#### Scenario: [MB-MA-07] fallback が解決できない場合は構成ミスとして失敗する
- **GIVEN** View fallback が null を返す VM 型
- **WHEN** その VM を show する
- **THEN** 構成ミスとして失敗する (cancelled 等の結果に化けない)

#### Scenario: [MB-MA-08] VM fallback の既定実装で型指定 show が動く
- **GIVEN** `AddKsDialogs` で VM fallback の既定実装 (サービスから解決) を有効にし、View は明示登録・VM factory は未登録の VM 型 (サービス登録済み)
- **WHEN** 型指定 show を呼ぶ
- **THEN** サービスから解決された VM で表示され、結果が返る

#### Scenario: [MB-MA-10] 明示 VM factory と View fallback の組み合わせが成立する
- **GIVEN** VM factory は明示登録・View は未登録で、View fallback を設定した VM 型
- **WHEN** 型指定 show を呼び、fallback が生成した View が VM 経由の notifier で報告する
- **THEN** 明示 VM factory の VM が fallback View (BindingContext = その VM) で表示され、報告した結果が返る
