# Tasks: add-kmp-typed-show

先に読む: handbook/cross の comment-policy (常時)・test-execution (kmp の `allTests`・api-surface-check・scenario-id-coverage)・sample-parity (samples を触るとき)・local-development-setup (kmp の composite build と Sample の iOS 合成 package)。lessons/impl.md も読む。

## 1. commonMain の契約とレジストリ
- [x] 1.1 `DialogViewRegistry` に `registerViewModel` を追加し、`LoadingViewRegistry` / `ToastViewRegistry` (interface) と `KsLoading.registry` / `KsToast.registry` を新設する。VM factory 表 (KClass → factory、後勝ち・スナップショット) を commonMain の内部クラスとして実装する (→ Requirement: 共有コードのレジストリの VM factory 登録口)
- [x] 1.2 `KsDialog` / `KsLoading` / `KsToast` に型指定 show (と Loading の型指定 start) を追加し、`@HiddenFromObjC` を付ける (`@Throws` は宣言しない)。VM factory の生成物の実行時クラスが `viewModelClass` と一致しなければ `DialogException` で失敗させる。実装は「VM factory → configure → 既存のインスタンス渡し show」を呼び出し元の文脈で行う commonMain の共通の前段として書き、Dialog は `GatewayKsDialog`、Loading / Toast は新設の commonMain デコレータ `GatewayKsLoading` / `GatewayKsToast` (各 actual の gateway を internal interface として委譲先にする) に置く。レジストリの VM factory 表は任意スレッドからの登録・解決に対して原子的なスナップショットを返す実装にする (→ Requirement: 共有コードの型指定 show)
- [x] 1.3 androidMain / iosMain の actual に `registry` の供給を足す (VM factory 表は commonMain の実体を返す。Android の `AndroidDialogViewRegistry` は Native ハンドルの同一性を保ったまま VM factory 表を持つ) (→ Requirement: 共有コードのレジストリの VM factory 登録口)

## 2. テスト
- [x] 2.1 commonTest: `TypedShowTests.kt` 系 (PB-KT-01〜08・12・13・LD-KT-02・TS-KT-01) を、本番の前段 (`GatewayKsDialog` / `GatewayKsLoading` / `GatewayKsToast`) に Test gateway を差し込んで書く。既存の `FakeKsLoading` / `FakeKsToast` (利用者の Fake の例) には `registry` の実装を足す (→ 各 Requirement)
- [x] 2.2 PB-KT-14: 生成 framework の ObjC ヘッダに型指定 show が現れないことを iosTest または Sample の iOS ルートの Swift compile 検査で固定する
- [x] 2.3 androidHostTest: PB-KT-11 (Native 側登録は見えない — Dialog / Loading / Toast の 3 機能。Native の `LoadingViewRegistry` / `ToastViewRegistry` の `registerViewModel` は実装済み)・PB-KT-09 (Android 委譲) / iosTest: PB-KT-09 (実 bridge に `object_getClass` で登録して解決を確認) (→ Requirement: 共有コードの型指定 show)
- [x] 2.4 `api-surface-check` に型指定 show の正の compile 検査を追加し (PB-KT-10)、負検査 `RejectsToastRegistration` を「View factory 登録 API が見えない」検査に置き換える (Loading 版も追加。build.gradle.kts の負検査定義と期待診断を更新) (TS-KT-02)。handbook/cross/test-execution.md の負検査表 (`toastRegistration` 行の期待診断の置き換え・Loading 行の追加・「負の検査 61 本」の本数) は同じ change の中で更新する (→ Requirement: 共有コードの公開面の compile 検査)

## 3. samples/kmp (sample-parity 準拠)
- [x] 3.1 `SamplePresenter` の `Model Dialog` / `Custom Loading` / `Custom Toast` の登録経路を型指定 show に差し替え、VM factory を共有コードで登録する (→ Requirement: kmp ルートの登録経路デモを共有コードの型指定 show にする)
- [x] 3.2 kmp ルート (Android / iOS) で手動通しを行い verification/ に証跡を残す (PB-KS-01 / LD-KS-01 / TS-KS-01)。iOS は実 framework 越しに VM factory の生成物が Swift 側レジストリで解決されることの確認を兼ねる。scenario-id-coverage の除外 ID に 3 件を理由つきで登録する

## 4. 仕上げ
- [x] 4.1 test-execution の規約どおり kmp `allTests` (iosSimulatorArm64Test + testAndroidHostTest) と api-surface-check を通し、scenario-id-coverage で「未網羅なし」まで確認する。全件実行の実測値で handbook/cross/test-execution.md の件数表の kmp 行 (現行 96 tests = iosSimulatorArm64 49 + androidHostTest 47) を実測日つきで更新する (lessons inbox: test-count-table-must-follow-in-change)
- [x] 4.2 ios/ と android/ のテストが影響を受けていないことを確認する (本 change は Native の公開面に触れないため、回帰の有無の確認のみ)
