# kmp-facade デルタ (add-kmp-typed-show)

Scenario ID は `<機能>-KT-<NN>` (`PB-KT` = Dialog、`LD-KT` = Loading、`TS-KT` = Toast の KMP 型指定 show)。挙動は commonTest (Fake / Test gateway による委譲面の差し替え) で検証し、Native への委譲は androidHostTest / iosTest の gateway 契約テストで検証する (kmp/ADR-0002 の検証分担)。公開面は `api-surface-check` の compile 検査で固定する。

実現経路: 型指定 show は commonMain の**共通の前段**に置く — Dialog は既存の `GatewayKsDialog` (`kmp/ksdialogs-kmp/src/commonMain/.../DialogGateway.kt`)、Loading / Toast にも同型の commonMain デコレータ (`GatewayKsLoading` / `GatewayKsToast`) を新設し、各 actual (`AndroidLoadingGateway` / `IosLoadingGateway` 等) はその委譲先 (internal gateway) になる。これにより本番の型指定経路 (VM factory 解決・configure・例外の扱い) が commonTest の Test gateway で検証できる (Fake の複製実装で検証しない)。VM factory 表は commonMain の内部クラスが持ち、既定エントリ (`Dialog.instance` / `Loading.instance` / `Toast.instance`) ごとに 1 個。Native の互換面 (`KSDInterop*Bridge`) と Android の typealias 委譲は変更しない。

## ADDED Requirements

### Requirement: 共有コードのレジストリの VM factory 登録口

`DialogViewRegistry` に `registerViewModel(viewModelClass: KClass<VM>, factory: () -> VM)` を追加し、`KsLoading` / `KsToast` に同型の `LoadingViewRegistry` / `ToastViewRegistry` (commonMain の interface) を `registry` プロパティで公開する (SHALL)。VM factory の実体は共有コード側が持ち、既定エントリと契約 interface 経由のどちらから取得しても同じレジストリが返る。同じ型への再登録は後勝ち。型指定 show の解決は呼び出し時点のスナップショットによる。View factory の登録口はこの面に無い (各 OS 側のまま)。**登録と解決は任意のスレッドから行え、1 回の解決は原子的なスナップショット**である (並行する再登録が解決の途中で混ざらない。同期手段は実装の選択)。value class の VM の拒否は共有コードでは判定できない (共通の reflection に無い) ため、各 OS の Native 経路の既存の判定に委ねる (Android はインスタンス渡し show の時点で拒否される — 既存の MB-AN-03。iOS は既存のインスタンス渡し show と同じ)。Android では共有 VM の `KClass` が Native の型と同一であり、Native 側の登録口 (Native レジストリの VM factory) に登録したものは共有コードの型指定 show から**見えない** (共有コードのレジストリが正)。

#### Scenario: [PB-KT-01] 既定エントリと契約 interface 経由で VM factory のレジストリを共有する
- **GIVEN** `Dialog.instance` と、それを `KsDialog` 型で受けた参照
- **WHEN** 片方の `registry` に VM factory を登録し、もう片方で型指定 show を呼ぶ
- **THEN** 登録した VM factory で VM が生成される (Loading / Toast も同様)

#### Scenario: [PB-KT-02] VM factory の再登録は後勝ちで、表示中の show には影響しない
- **GIVEN** VM factory を登録した VM 型と、完了を外から制御できる非同期 configure
- **WHEN** 型指定 show を呼び、configure の完了前に同じ型の VM factory を再登録し、そのあと configure を完了させる
- **THEN** 進行中の show は最初の VM factory の生成物で表示され、次の型指定 show から新しい VM factory が使われる

#### Scenario: [PB-KT-11] Android で Native 側に登録した VM factory は共有コードの型指定 show から見えない
- **GIVEN** 共有 Dialog / Loading / Toast VM 型の VM factory を Android Native の `DialogViewRegistry` / `LoadingViewRegistry` / `ToastViewRegistry` の `registerViewModel` にだけ登録した状態 (androidHostTest)
- **WHEN** 共有コードの型指定 show を呼ぶ
- **THEN** 3 機能とも「VM factory 未登録」の構成ミスとして `DialogException` で失敗する (Native の登録は共有コードのレジストリに影響しない)

#### Scenario: [PB-KT-12] 並行する再登録の途中でも解決は原子的なスナップショットになる
- **GIVEN** VM factory を登録した VM 型
- **WHEN** 別スレッドから再登録を繰り返しながら型指定 show を繰り返す
- **THEN** 各 show は登録済みのいずれかの VM factory の生成物を得て、未登録の失敗や混ざった状態にならない

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
- **THEN** Android では Native の `show(viewModel)` に生成物が渡り、iOS では生成物が Swift 側レジストリで解決される (未登録の失敗ではなく、提示先不在の失敗で終わる — `iosSimulatorArm64Test` では実提示が起きないため)。実提示は kmp Sample の iOS ルートで確認する (samples spec)

### Requirement: 共有コードの公開面の compile 検査

`api-surface-check` に、VM factory 登録 (ラムダ / コンストラクタ参照)・3 機能の型指定 show (configure あり / なし・suspend configure・placement / durationMs あり)・型指定 start を利用者の可視性で記述した正の検査を追加する (SHALL)。結果型を省略した Dialog の型指定 show (`DialogViewModel` に既定の型引数が無い) は書けない (既存の「結果型は省略できない」規則のまま)。

既存の負検査 `RejectsToastRegistration` (`KsToast.registry` が存在しないことを検査) は本 change で前提が変わるため、「共有コードの Toast / Loading レジストリに **View factory の登録 API が見えない**」ことを検査する形に置き換える (削除しない)。

#### Scenario: [TS-KT-02] 共有コードのレジストリに View factory の登録 API が無い (負の compile 検査)
- **GIVEN** `api-surface-check` の負検査ソース
- **WHEN** `KsToast.registry` / `KsLoading.registry` に対して View を返す factory の登録を記述する
- **THEN** コンパイルが失敗する (期待診断は既存の負検査と同じ仕組みで固定する)

#### Scenario: [PB-KT-10] 共有コード公開面の正の compile 検査
- **GIVEN** `api-surface-check` の commonMain ソース
- **WHEN** 上記の呼び出しを記述する
- **THEN** すべてコンパイルが通る
