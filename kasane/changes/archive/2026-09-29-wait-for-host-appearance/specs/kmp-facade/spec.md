# kmp-facade デルタ (wait-for-host-appearance)

KMP の Dialog・Loading・Toast は、両 OS の Native の待ちに従う (Native へ委譲するため、KMP 側に待ちの仕組みは足さない)。本書は、提示先の不在を判定材料にしていた既存 Scenario の書き直しと、共有コードからの打ち切りの追加分。検証の分担は kmp/ADR-0002 に従う (androidHostTest / iosTest は各 OS の gateway の契約)。

iosTest (`iosSimulatorArm64Test`) と androidHostTest には提示先が無い (`kasane/handbook/cross/test-execution.md:121-123`)。登録済みの ViewModel の show は、これまでは提示先の不在で必ず失敗したが、これからは失敗せずに待つ。そこで、「待っていること」と「打ち切りで cancelled / キャンセルの通知になること」を、登録済みの factory を引き当てた印にする (design Decision 9)。未登録の ViewModel は、今までどおり待たずに失敗する。

実現経路
- iOS: 互換面の show のハンドル `KsDialogsInteropShowHandle.cancel()` (`ios/Sources/KsDialogs/Interop/KsDialogsInteropShowHandle.swift`) が Presenter の Task を打ち切る。共有コードのコルーチンの打ち切りは `IosDialogGateway` の `invokeOnCancellation` (`kmp/ksdialogs-kmp/src/iosMain/kotlin/jp/kamusoft/ksdialogs/kmp/IosDialogGateway.kt:106-127`) からこのハンドルへ届く
- Android: `AndroidDialogGateway` (`kmp/ksdialogs-kmp/src/androidMain/kotlin/jp/kamusoft/ksdialogs/kmp/AndroidDialogGateway.kt:23-35`) は Native の suspend にそのまま乗るので、コルーチンの打ち切りが Native の待ちに届く

## ADDED Requirements

### Requirement: 共有コードから、待っている Dialog を打ち切れる

提示先を待っている Dialog を、共有コードのコルーチンの打ち切りで止められる SHALL。止めた Dialog は一度も表示されず、キャンセルの通知が呼び出し元へ伝播する (結果通知の「呼び出し元の打ち切り」の KMP の見え方と同じ)。

#### Scenario: [PB-KC-04] 待っている Dialog を共有コードで打ち切ると、表示されずにキャンセルが伝播する
- **GIVEN** 提示先の無いホスト (androidHostTest / iosTest) で、登録済みの ViewModel を `Dialog.instance.show` で表示しようとしているコルーチン
- **WHEN** show が失敗も完了もしていないことを確かめてから、そのコルーチンを打ち切る
- **THEN** 呼び出し元にキャンセルの通知が伝播し、`DialogException` にはならない。iOS では互換面のハンドルの打ち切りが呼ばれている

## MODIFIED Requirements

### Requirement: KMP iOS ホストの gateway 固有メッセージは英語固定

iOS ホスト側 gateway が Native の説明を得られない場合に `DialogException` の `message` として使う定数は、次の英語文言とする (SHALL)。

| 定数 | 文言 (en) |
|---|---|
| `IosDialogGateway.MISSING_RESULT_MESSAGE` | `No Dialog result was delivered.` |
| `IosDialogGateway.UNKNOWN_FAILURE_MESSAGE` | `Failed to show the Dialog.` |
| `IosLoadingGateway.UNKNOWN_FAILURE_MESSAGE` | `Failed to show the Loading.` |

#### Scenario: [DM-KM-01] iOS 互換面の結果に Native の英語文言が載り、登録済みの show は待つ
- **GIVEN** iOS ホストで View factory 未登録の ViewModel と、提示先の無い状態で登録済みの ViewModel
- **WHEN** それぞれ互換面 (bridge) 経由で show を呼ぶ。登録済みの側は、結果が届いていないことを確かめてから、互換面のハンドルで打ち切る
- **THEN** 未登録の側は、失敗の結果の `error.localizedDescription` が ios-native デルタの英語文言 (`No View factory is registered for ViewModel type`) を含む。登録済みの側は失敗の結果にならず、打ち切りのあとに cancelled の結果が届く

#### Scenario: [DM-KM-03] iOS ホストで共有コードの DialogException に Native の英語文言が素通しで届き、登録済みの show は待つ
- **GIVEN** iOS ホストで View factory 未登録の ViewModel と、提示先の無い状態で登録済みの ViewModel
- **WHEN** それぞれ `Dialog.instance.show` を呼ぶ。登録済みの側は、失敗も完了もしていないことを確かめてから、コルーチンを打ち切る
- **THEN** 未登録の側は、投げられた共有コードの `DialogException` の `message` が上と同じ英語文言を含む。登録済みの側は `DialogException` にならず、キャンセルの通知が伝播する

#### Scenario: [DM-KM-04] KMP 共有コードのメイン側に日本語の文字列リテラルが残らない
- **GIVEN** `kmp/ksdialogs-kmp/src/{commonMain,androidMain,iosMain}/` 配下の Kotlin ソース (コメント行を除く)
- **WHEN** 日本語を含む文字列リテラルを検索する
- **THEN** 該当行は 0 件で、上表の英語文言が 3 定数のリテラルと一致している (静的 grep とレビューで受け入れる Scenario。`scripts/scenario-id-coverage.py` の除外表に理由付きで登録する。Native の説明が得られない経路は既存テストに器が無いため自動テストの対象にしない)

### Requirement: KMP Android ホストは Native の英語文言を素通しする

Android ホスト側 gateway は Native の `DialogException` の `message` を共有コードの `DialogException` の `message` にそのまま転記する (SHALL、現行どおり)。

#### Scenario: [DM-KM-02] Android ホストで Native の英語文言が DialogException に届き、登録済みの show は待つ
- **GIVEN** Android ホストで View factory 未登録の ViewModel と、提示先の無い状態で登録済みの ViewModel
- **WHEN** それぞれ `Dialog.instance.show` を呼ぶ。登録済みの側は、失敗も完了もしていないことを確かめてから、コルーチンを打ち切る
- **THEN** 未登録の側は、`DialogException` の `message` が `No View factory is registered for ViewModel type {T}.` と完全一致し、`cause` が Native の `DialogException.ViewFactoryNotRegistered` である。登録済みの側は `DialogException` にならず、キャンセルの通知が伝播する

### Requirement: 共有コードの型指定 show (Dialog / Loading / Toast)

`KsDialog` に `show(viewModelClass: KClass<VM>, placement, configure: (suspend (VM) -> Unit)?)`、`KsLoading` に同形の `show` と `start(viewModelClass, placement, configure, action)`、`KsToast` に `show(viewModelClass, durationMs, placement, configure: ((VM) -> Unit)?)` を追加する (SHALL)。動詞は show 1 本。結果型・置き場所・duration・合流・計時の意味はインスタンス渡し show と同じで、Dialog の結果は VM が宣言した結果型で返る。

解決の流れは「VM factory で生成 → configure 完了 → 既存のインスタンス渡し show」で、**VM factory と configure は呼び出し元の文脈 (suspend 関数は呼び出し元のコルーチン文脈、Toast の同期 show は呼び出しスレッド) で実行し、UI スレッドへは移さない**。VM factory 未登録は「VM factory 未登録」の構成ミスとして `DialogException` で失敗し、表示は行われない。VM factory / configure の例外 (キャンセル含む) は提示に進まず呼び出し元へ伝播する — **Toast も同期に伝播する** (Native の「受理後の失敗」分類は共有コードには適用しない)。**VM factory の生成物の実行時クラスは登録キー (`viewModelClass`) と一致していなければならない**。一致しない場合 (サブクラスを返す factory 等) は、Native 側で実行時クラスによる View factory の再解決が未登録に化けるのを防ぐため、共有コード側で「型不一致」の構成ミスとして `DialogException` で失敗させる (提示に進まない)。

**型指定 show / start のオーバーロードは ObjC / Swift から隠す** (`@HiddenFromObjC`)。共有 Kotlin コード専用の面であり (Swift 向け KMP 面の型指定 show は設けない — kmp/ADR-0006)、`KClass` 引数は Swift から扱えず、VM factory / configure の任意の例外は `@Throws` で Swift 境界に運べないため。したがってこれらのオーバーロードには `@Throws` を宣言しない (Kotlin 内では例外がそのまま伝播する)。Swift から見える面 (インスタンス渡し show・`registry` プロパティ) は従来どおり。

#### Scenario: [PB-KT-03] Dialog の型指定 show が生成 → configure → 結果の一連で動く
- **GIVEN** VM factory を登録した結果型付きの共有 VM 型と、委譲面の差し替え (Test gateway)
- **WHEN** configure で状態を設定して型指定 show を呼び、委譲面が completed を返す
- **THEN** 委譲面には configure の状態が入った VM が渡り、宣言結果型の completed が返る

#### Scenario: [PB-KT-04] 非同期 configure の完了まで委譲面に渡らない
- **GIVEN** 完了を外から制御できる非同期 configure
- **WHEN** 型指定 show を呼び、configure が完了する前に委譲面の呼び出し有無を観察する
- **THEN** 完了前は委譲面が呼ばれず、完了後に呼ばれる

#### Scenario: [PB-KT-05] VM factory 未登録の型指定 show は構成ミスとして失敗する
- **GIVEN** VM factory を登録していない共有 VM 型
- **WHEN** 型指定 show を呼ぶ
- **THEN** `DialogException` で失敗し、委譲面は呼ばれない (Loading / Toast も同様)

#### Scenario: [PB-KT-06] VM factory と configure の失敗は提示に進まず伝播する
- **GIVEN** 例外を投げる VM factory、または例外を投げる configure
- **WHEN** 型指定 show を呼ぶ
- **THEN** その例外が呼び出し元へ伝播し、委譲面は呼ばれない (キャンセルも同様に伝播する)

#### Scenario: [PB-KT-07] VM factory と configure は呼び出し元の文脈で実行される
- **GIVEN** VM factory と configure の実行スレッド / コルーチン文脈を記録する仕掛け
- **WHEN** 任意のディスパッチャ上から型指定 show を呼ぶ
- **THEN** 両方が呼び出し元と同じ文脈で実行される (Main dispatcher への hop が起きない)

#### Scenario: [PB-KT-13] VM factory の生成物の型が登録キーと違えば型不一致として失敗する
- **GIVEN** `registerViewModel(Base::class) { Derived() }` のように、登録キーのサブクラスを返す VM factory
- **WHEN** `show(Base::class)` を呼ぶ
- **THEN** 「型不一致」の構成ミスとして `DialogException` で失敗し、委譲面は呼ばれない (Loading / Toast も同様)

#### Scenario: [PB-KT-14] 型指定 show は Swift / ObjC から見えない
- **GIVEN** 生成された iOS framework の ObjC ヘッダ (または Swift 側の compile 検査)
- **WHEN** `KsDialog` / `KsLoading` / `KsToast` の型指定 show / start を Swift から参照する
- **THEN** 参照できない (インスタンス渡し show と `registry` は参照できる)

#### Scenario: [PB-KT-08] configure 省略の型指定 show は VM factory の生成物をそのまま渡す
- **GIVEN** VM factory を登録した共有 VM 型
- **WHEN** configure なしで型指定 show を呼ぶ
- **THEN** VM factory の生成物がそのまま委譲面に渡る

#### Scenario: [LD-KT-02] Loading の型指定 show / start が既存のインスタンス渡し経路に流れる
- **GIVEN** VM factory を登録した共有 Loading VM 型と、委譲面の差し替え
- **WHEN** configure 付きで型指定 show と型指定 start を呼ぶ
- **THEN** 委譲面のインスタンス渡し show / start に configure 済みの VM と置き場所が渡り、start は処理の戻り値をそのまま返す

#### Scenario: [TS-KT-01] Toast の型指定 show が同期に委譲面へ流れ、失敗は同期に伝播する
- **GIVEN** VM factory を登録した共有 Toast VM 型と、委譲面の差し替え
- **WHEN** configure・durationMs・placement 付きで型指定 show を呼ぶ / 例外を投げる configure で呼ぶ
- **THEN** 前者は委譲面のインスタンス渡し show に configure 済みの VM・durationMs・placement が渡り、後者はその例外が呼び出し元へ同期に伝播して委譲面は呼ばれない

#### Scenario: [PB-KT-09] 両 OS の gateway が型指定 show の VM をインスタンス渡しと同じ経路で Native へ渡す
- **GIVEN** VM factory を登録した共有 VM 型 (androidHostTest は Native レジストリに View factory を登録 / iosTest は**実際の互換面 bridge** に `object_getClass` で得た ObjC クラスをキーに View factory を登録する — 既存の `InteropBridgeContractTests` の probe 登録と同じ手段)
- **WHEN** 共有コードの型指定 show を呼ぶ
- **THEN** Android では Native の `show(viewModel)` に生成物が渡り、iOS では生成物が Swift 側レジストリで解決される。どちらも未登録の失敗にならず、提示先の出現を待つ (提示先の無いテストランナーでは実提示が起きないため、待っていることを確かめてから打ち切り、キャンセルで終わることを確かめる)。実提示は kmp Sample の iOS ルートで確認する (samples spec)
