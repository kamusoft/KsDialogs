---
type: concept
title: KMP 利用者の iOS ホスト統合
description: KMP 共有モジュールから KsDialogs を使う iOS アプリの依存経路と初回統合手順、Swift 側の登録入口と結果型 (Kotlin の整数型との対応)、および Sample の合成 Swift package 再生成手順
tags: [kmp, ios, swiftpm, integration, distribution]
timestamp: 2026-10-04
---

# KMP 利用者の iOS ホスト統合

この文書を読むと、KMP 共有モジュールが KsDialogs の Maven artifact を消費するとき、iOS アプリ側にどの依存が推移し、何を手で追加する必要があるかが分かる。Kotlin Gradle Plugin の Swift package 連携は Alpha であり、ここでは static framework を前提とする。

## 依存の全体像

共有モジュールが Maven 依存 `jp.kamusoft:ksdialogs-kmp` を 1 点追加すると、KMP artifact の発行 metadata が KsDialogs の Swift package 参照を消費者へ伝える。消費者の `build.gradle.kts` に `swiftPMDependencies` を再宣言する必要はない。

```text
共有モジュール
  └─ Maven: jp.kamusoft:ksdialogs-kmp
       ├─ KMP facade と cinterop klib
       └─ 発行 metadata の Swift package 参照
            └─ KotlinMultiplatformLinkedPackage

iOS アプリ
  ├─ 共有モジュールの static framework
  ├─ KotlinMultiplatformLinkedPackage
  └─ SwiftPM: KsDialogs-SPM / product KsDialogs
```

KMP artifact は Kotlin コードから Swift 実装へつながる参照を持つが、static framework 自体には Swift の実体を含めない。`KotlinMultiplatformLinkedPackage` は、発行 metadata が示す Swift package を Xcode の依存グラフへ追加し、この未解決参照を iOS アプリのリンク時に満たすための合成 package である。

一方、Swift 側で View を登録する型付き入口 (`Dialog.shared.kmp` / `Loading.shared.kmp` / `Toast.shared.kmp`) をアプリのソースから参照するため、iOS アプリは `https://github.com/kamusoft/KsDialogs-SPM` の product `KsDialogs` を Package Dependencies に 1 点追加する。同じ package identity は SwiftPM が 1 つにまとめるため、合成 package と直接参照で Swift 実体が二重化しない。

## 初回の統合手順

前提として、共有モジュールの iOS framework を Xcode project からビルド・リンクできる標準の KMP iOS 連携を済ませておく。

1. 共有モジュールの `commonMain` に Maven 依存 `jp.kamusoft:ksdialogs-kmp` を 1 点追加する。
2. KMP project のルートで、Xcode project のパスを環境変数 `XCODEPROJ_PATH` に渡して `integrateLinkagePackage` を 1 回実行する。
3. Xcode の Package Dependencies に `https://github.com/kamusoft/KsDialogs-SPM` を追加し、product `KsDialogs` をアプリ target にリンクする。

消費者プロジェクトでの手順 2 は、共有モジュール名を `shared` とすると次の形になる。

```bash
XCODEPROJ_PATH="$PWD/iosApp/MyApp.xcodeproj" \
  ./gradlew :shared:integrateLinkagePackage
```

生成された `KotlinMultiplatformLinkedPackage/` は Xcode project が参照するため VCS に含める。以後の依存変更では Gradle build が合成 package を更新する。

## 発行 metadata の Swift 参照は version で決まる

KMP artifact の発行 metadata に載る Swift package 参照は、`kmp/ksdialogs-kmp/build.gradle.kts` が artifact の version から導出する。専用の切替スイッチは無い ([cross/ADR-0008](../../../decisions/cross/0008-distribution-model-standard-channels.md))。

| version | Swift 参照 | 用途 |
|---|---|---|
| `-SNAPSHOT` (カタログの開発既定値) | `localSwiftPackage(../ios)` — monorepo 内の `ios/` をそのまま指す | 日常開発。`../ios` のライブ編集が即座に効き、Sample の合成 package も変わらない |
| リリース版 (`-Pversion=` で注入) | `swiftPackage(url, exact(<version>))` — 配信リポジトリ `KsDialogs-SPM` の同じ version の tag | 公開 artifact。exact は version と同じ値で、上書きできない |

リモート参照の URL だけは Gradle プロパティ `ksdialogs.swiftPackageUrl` で上書きでき、公開前の検証はスナップショットを同期して commit + tag したローカル clone の `file://` URL を渡して行う (SNAPSHOT では参照に URL が無いため無視される)。metadata の deployment target `17.0` は参照の種別によらず載る。

リリース版の発行は、発行の副作用として本体側の合成 Swift マニフェスト 2 本 (`kmp/.swiftpm-locks/default/swiftImport/subpackages/` 配下の `Package.swift`) を、そのとき解決した Swift 参照の URL へ書き換える。`file://` の上書き付きで手元から発行すると追跡ファイルにローカル絶対パスが残るため、消費者検証のフィード準備は発行前に 2 本が未変更であることを検査し、成否によらず復元する ([消費者検証](../../cross/architecture/consumer-verification.md))。

リリース版の iOS publication (cinterop klib 付き) は、Kotlin Gradle Plugin が発行時に SwiftPM パッケージを解決するため、配信リポジトリに同版の tag が実在しないと発行できない。release workflow はこの制約から配信リポジトリの tag を push した後に KMP を発行する ([release workflow](../../cross/architecture/release-workflow.md)、cross/ADR-0024)。

SNAPSHOT のまま Maven local へ発行した成果物には発行者の絶対パスが載り、同一マシンでしか解決できない。SNAPSHOT の消費はリポジトリ内 Sample の composite build が担い、リポジトリ外での検証はリリース版の version を注入して行う。

消費者側の Kotlin Gradle Plugin は本ライブラリと同じ minor (2.4.x) をサポートし、動作確認済みの版はリポジトリのバージョンカタログ (`android/gradle/libs.versions.toml` の `kotlin`) が固定する値である。SwiftPM import は Alpha 機能で消費側の最低版と metadata 形式の互換に公式の記述が無いため、これより広い範囲は約束しない。

Xcode 27.0 で Swift パッケージの取り込みをビルドするには、Kotlin 2.4.20 以上が要る。2.4.10 では取り込みのタスク (`convertSyntheticImportProjectIntoDefFile…`) が `clang: error: unknown argument` で落ちる (上流の不具合 KT-87196。2.4.20 で修正)。2.4.20 で作った klib の `abi_version` は 2.4.0 で、2.4.10 のときと同じである (2026-10-04 実測)。

## Swift 側で登録するもの

KMP facade は View factory のレジストリ実体を持たず、iOS Native ライブラリのレジストリへ委譲する (共有コードが持つのは ViewModel factory の表だけ — [KMP の Dialog 公開面](dialog-surface.md))。iOS アプリは共有コードで定義した ViewModel 型をキーに、Dialog / Loading / Toast の View を Swift 側で登録する。

Swift 向けの入口に型指定 show (ViewModel の型だけを渡す表示) は無い。共有コードの型指定 show と ViewModel factory の登録口は共有 Kotlin コード専用で、framework の ObjC ヘッダには現れない。iOS ホストから共有 VM を出すときは、Kotlin の VM を Swift 側で作ってインスタンス渡しの `show` を呼ぶ。

| 機能 | Swift 側の入口 | 登録するもの |
|---|---|---|
| Dialog | `Dialog.shared.kmp` | 共有 ViewModel 型から dialog content を作る factory |
| Loading | `Loading.shared.kmp` | 共有 ViewModel 型から loading content を作る factory |
| Toast | `Toast.shared.kmp` | 共有 ViewModel 型から toast content を作る factory |

この登録は、純 Native 利用と KMP 利用が同じ iOS Native レジストリを共有するために必要である。共有コード側の show と結果通知の契約は [登録と表示の呼び出し面](../../core/api/registration-show-semantics.md) と [結果通知](../../core/api/result-notification-semantics.md) を参照する。

登録はアプリの起動時に MainActor 上で 1 回行う。次は 3 機能の最小形で、`SharedConfirmViewModel` などの ViewModel 型と `ConfirmContent` などの View は消費者アプリ側の型である。Dialog の factory は ViewModel と結果報告口を受け取り、Loading / Toast の factory は ViewModel を受け取って UIKit または SwiftUI の View を返す。

```swift
@MainActor
func registerKmpViews() {
    Dialog.shared.kmp.register(SharedConfirmViewModel.self) { viewModel, notifier in
        ConfirmContent(viewModel: viewModel, notifier: notifier)
    }
    Loading.shared.kmp.register(SharedLoadingViewModel.self) { viewModel in
        LoadingContent(viewModel: viewModel)
    }
    Toast.shared.kmp.register(SharedToastViewModel.self) { viewModel in
        ToastContent(viewModel: viewModel)
    }
}
```

### 型付き入口の署名 (Dialog)

共有 VM は iOS Native の ViewModel 契約に準拠せず結果型を宣言しないため、この面では**結果型を `result:` ラベルの引数で受け取る**。`result:` を省略すると真偽値 (`Bool`) になる。

| 書き方 | 意味 |
|---|---|
| `register(SharedConfirmViewModel.self) { viewModel, notifier in … }` | 結果型は `Bool` |
| `register(SharedConfirmViewModel.self, result: String.self) { … }` | 結果型を明示する |
| `show(viewModel, placement:)` / `show(viewModel, result:placement:)` | 表示 (`async throws`。閉じるまで待って `DialogResult<結果型>` を返す)。`result:` の省略時は `Bool` |
| `notifier(for:)` / `notifier(for:result:)` | 表示中の共有 VM から結果報告口を取り出す (報告口を受け取らない factory で書いた中身が、VM から結果を返すときに使う) |

中身は UIKit (`UIView` を返す factory) と SwiftUI (View を返す factory) の**同名オーバーロード**で書ける。どちらにも、ViewModel と報告口を受け取る 2 引数の factory と、ViewModel だけを受け取る 1 引数の factory がある。`result:` を取る `register` は、この UIKit / SwiftUI × 2 引数 / 1 引数の 4 種である。

どちらで書いても表示・結果・失敗の挙動は同じだが、器 (中身を載せて表示するライブラリのコンテナ) の中から見える安全領域だけは違う。UIView の中身は、配置や基準領域を問わず、器とシステムの領域 (ステータスバー・ホームインジケータ等の帯) の重なりを safe area として受け取る。SwiftUI の中身がその重なりを受け取るのは、基準領域 (`DialogOptions.layoutArea`) が窓全体 (`.window`) で、その軸 (水平・垂直) の配置 (`DialogPlacement` の `horizontalAlignment` / `verticalAlignment`) が端 (`.start` / `.end` など) のときだけである。配置が中央 (`.center` など) の軸と、窓全体以外の基準領域 (`.visibleArea`・`.currentPage`) では 0 を受け取る。器がシステムの領域に重なると、この 2 つの場合で両者の見え方が分かれる (条件の表は [iOS のレイアウト公開面](../../ios/api/layout-surface.md) の「SwiftUI の中身から見える安全領域」、決定は [ios/ADR-0002](../../../decisions/ios/0002-swiftui-content-receives-standard-safe-area.md))。

`notifier(for:result:)` は show 外では nil を返し、登録時の結果型と違う型を `result:` に渡した場合は nil ではなく型付きの失敗になる — 「表示していないから空」と「結果型を取り違えている」を取り違えないためである。表示中に同じ VM 型を別の結果型で登録し直しても、判定に使う結果型は、その show を始めた時点の登録で固定される。

`register` に渡した結果型と共有コードの ViewModel が宣言している結果型は同じでなければならない。Swift から show したときは、食い違いは報告された結果値を `result:` の型として取り出す時点で型付きの失敗として現れる ([kmp/ADR-0004](../../../decisions/kmp/0004-swift-facing-typed-generic-facade.md))。

共有コードからの show (`Dialog.instance.show(...)`。Swift の型付き入口を通らない) では、食い違いは検出されない。共有コード側は、Swift で登録した結果型を確かめずに結果値をそのまま ViewModel の結果型として受け取るため、食い違いは共有コードが値を Kotlin の型として使う時点の `ClassCastException` で初めて表に出る。下の小節の `Int` / `UInt` の判定もこの経路には掛からず、Swift の `Int` で登録した VM も表示まで進んで、同じ `ClassCastException` で表に出る。

#### Kotlin の整数型を結果型にするとき

共有コードの ViewModel が Kotlin の整数型を結果型にしているとき、Swift から見えるのは幅の決まった整数型で、`result:` にはその型を渡す。

| Kotlin (共有 VM の結果型) | Swift で `result:` に渡す型 |
|---|---|
| `Int` | `Int32` |
| `Long` | `Int64` |
| `UInt` | `UInt32` |
| `ULong` | `UInt64` |

Swift の `Int` / `UInt` はどの Kotlin の整数型とも対応しない。`result:` を取る 6 つの入口 (上の `register` 4 種・`notifier(for:result:)`・`show(_:result:placement:)`) に `Int.self` / `UInt.self` を渡すと、コンパイルは通るが、使うべき型 (`Int` なら `Int32` / `Int64`、`UInt` なら `UInt32` / `UInt64`) を案内する英語の deprecated 警告が出る。Swift にはジェネリックな入口を保ったまま `Int` だけを案内付きのコンパイルエラーにする仕組みが無いため、警告と、次の実行時の失敗を組み合わせている。

| 入口 | `Int` / `UInt` を使ったときの実行時の挙動 |
|---|---|
| `register` 4 種 | 失敗せずに登録する |
| `show(_:result:placement:)` (結果型を省略した `show(_:placement:)` を含む) | 指定した結果型か、show を呼んだ時点でその共有 VM の型に登録されている結果型が `Int` / `UInt` なら、表示せず (View factory を呼ばず) に `resultTypeMismatch` を throw する。両方が当たると指定した結果型の失敗になる |
| `notifier(for:result:)` | 指定した結果型が `Int` / `UInt` なら、表示中かどうかや登録の結果型との比較より先に `resultTypeMismatch` を throw する (show 外でも nil にならない) |

失敗の `expected` には使うべき型 (`"Int32 or Int64"` / `"UInt32 or UInt64"`)、`actual` には当たった型 (`"Int"` / `"UInt"`) が入るので、エラー文がそのまま案内になる。結果型を型引数で受け取る利用者側のジェネリックな関数越しに呼ぶと警告は出ないが、実行時の判定は同じに掛かる。警告と実行時の失敗の組み合わせにした経緯は `kasane/changes/archive/2026-10-20-guard-kmp-swift-int-result-type/proposal.md` にある。

#### 失敗の型 (Dialog 入口だけが写し替える)

Dialog の型付き入口は、この面に固有の失敗だけを `KsDialogsKmpError` へ写し替えて throw する。判別は共有 VM の紐付けと結果型の食い違いに限り、失敗の種類が増えてもこの面は広げない ([kmp/ADR-0004](../../../decisions/kmp/0004-swift-facing-typed-generic-facade.md))。

| 判別 | いつ起きるか |
|---|---|
| `KsDialogsKmpError.notRegistered(viewModelType:)` | 共有 VM の型に View factory が登録されていない |
| `KsDialogsKmpError.resultTypeMismatch(expected:actual:)` | 結果値の型が、登録・show で指定した結果型 (省略時は真偽値) と一致しない。または、show・`notifier(for:result:)` に指定した結果型か、show する共有 VM の型に登録された結果型が Swift の `Int` / `UInt` である (このときは表示する前に失敗する — 上の「Kotlin の整数型を結果型にするとき」) |

写し替えの対象はこの 2 つで、報告口が二重に取られた・View factory が ViewModel の型を受け取れないなど**共有コード経路に固有でない失敗は `DialogError` のまま届く**。出す先の画面が無いことは失敗にならず、画面が現れるのを待つ ([iOS の Dialog 公開面](../../ios/api/dialog-surface.md))。利用者は必要に応じて両方を捕まえる。

### Loading / Toast の Swift 側入口と見た目の設定

`Loading.shared.kmp` / `Toast.shared.kmp` には登録に加えて、共有 ViewModel を渡す表示もある。見た目と器メタ属性の設定は KMP 専用入口ではなく iOS Native の設定プロパティで行う (共有コード側にこの面が無いため — [KMP の Loading 公開面](loading-surface.md) / [KMP の Toast 公開面](toast-surface.md))。

| 機能 | 表示 | 見た目・器メタ属性の設定 |
|---|---|---|
| Loading | `show(viewModel, placement:)` (`async throws`。終了は `Loading.shared.hide()`) | `Loading.shared.style` / `Loading.shared.options` ([iOS の Loading 公開面](../../ios/api/loading-surface.md)) |
| Toast | `show(viewModel, duration:placement:)` (`throws`。`duration:` はミリ秒で、共有コードの `durationMs` と同じ値) | `Toast.shared.style` ([iOS の Toast 公開面](../../ios/api/toast-surface.md)) |

未登録の共有 ViewModel 型はどちらも構成ミスとして `DialogError.viewFactoryNotRegistered` を投げ、表示は行われない。Loading / Toast の入口は Dialog と違って `KsDialogsKmpError` への写し替えを行わないため、失敗は `DialogError` のまま届く。

## Sample で合成 package を再生成する

`samples/kmp/iosApp` は、共有モジュールの static framework、`KotlinMultiplatformLinkedPackage`、登録 API 用の KsDialogs Swift package の 3 点をリンクする。合成 package を作り直すときはリポジトリルートから次を実行する。

```bash
XCODEPROJ_PATH="$PWD/samples/kmp/iosApp/KsDialogsSampleKmp.xcodeproj" \
  ./samples/kmp/gradlew -p samples/kmp :shared:integrateLinkagePackage
```

生成先は `samples/kmp/iosApp/KotlinMultiplatformLinkedPackage/` であり、Xcode project の参照先なので VCS に含める。

Kotlin の版を上げたあとは合成 package を作り直し、増えたファイルも commit する。Kotlin 2.4.20 からは `include/module.modulemap` が加わった。これが無い checkout では、Sample の iOS の 1 回目のビルドが「Synthetic project regenerated」で止まり、2 回目は通る (2026-10-04 実測)。`kmp/.swiftpm-locks/default/swiftImport/` 配下の合成 package も同じ扱いにする。

## 保証すること

- 共有モジュールの利用者は KsDialogs の Swift package 依存を Gradle 側へ再宣言せず、Maven 依存 1 点から推移的に受け取れる。
- iOS アプリへ直接追加する SwiftPM 依存は、Swift 側の登録 API をソースから参照するための 1 点だけである。
- KMP の static framework と iOS Native ライブラリは View factory のレジストリを共有し、純 Native と KMP の View の登録先が二重化しない。

## してはいけないこと

- 公開 artifact の発行 metadata に `localSwiftPackage` のローカルパスを残さない。消費者環境ではそのパスを解決できない。リリース版の version を注入した発行ではビルドファイルの導出が local 参照を選ばないため、これを崩すのは導出の分岐を書き換えたときだけである。
- `KotlinMultiplatformLinkedPackage/` を生成物として無視しない。Xcode project が参照する統合物であり、clone 後にも必要になる。
- KMP 側に View factory の別のレジストリ実体を作らない。iOS Native と KMP の登録キーが分裂する (共有コード側にあるのは ViewModel factory の表だけで、View の登録は Swift 側の 1 か所)。
- 共有モジュールの iosMain で commonMain の interface が `@Throws` を宣言したメンバを override するとき (`KsDialog` / `KsLoading` / `KsToast` の差し替え等)、override に `@Throws` を書かない。Kotlin 2.4.x では metadata compile が失敗する (次段落)。

`@Throws` は interface の宣言を継承するので、override に書かなくても Swift 側の throws 契約は保たれる。書くと Kotlin 2.4.x の native 系の中間 source set の metadata compile が、同一の filter を「異なる filter」と誤検出して失敗する (https://youtrack.jetbrains.com/issue/KT-88548、2.5.0 で修正)。これはいずれの契約を実装しても同じで、ジェネリックを持つ `KsDialog.show` の override でも失敗する。commonMain の override は対象外で、`@Throws` を書いても通る。

## 関連

- [KMP の Dialog 公開面](dialog-surface.md) — 共有コードの呼び出し面と、iOS ホスト側の型付き入口の位置づけ
- [KMP の Loading 公開面](loading-surface.md) — 共有コードの Loading 操作と、登録が各 OS 側にあること
- [KMP の Toast 公開面](toast-surface.md) — 共有コードの Toast 操作と、登録が各 OS 側にあること
- [消費者検証](../../cross/architecture/consumer-verification.md) — 配布物を利用者と同じ経路で解決する消費者プロジェクトと、KMP の dry-run の組み立て (合成 version・tag 付きローカル clone・smoke 形 fixture)
- [cross/ADR-0008](../../../decisions/cross/0008-distribution-model-standard-channels.md) — 4 形態の配布単位と `KsDialogs-SPM`、KMP の Swift 参照を version から導出する決定と Kotlin サポート範囲
- [cross/ADR-0009](../../../decisions/cross/0009-lockstep-single-version.md) — lockstep 単一バージョンと、KMP artifact の version・本体依存版・exact の共通の入力になる版の導出式
- [kmp/ADR-0002](../../../decisions/kmp/0002-thin-facade-native-registry.md) — static framework と View レジストリの Native 委譲
- [kmp/ADR-0006](../../../decisions/kmp/0006-common-vm-factory-typed-show.md) — ViewModel factory の表は共有コード側に持ち、Swift 向け面に型指定 show を設けない決定
- [登録と表示の呼び出し面](../../core/api/registration-show-semantics.md) — ViewModel 登録と show の共通契約
- `kasane/roadmaps/archive/2026-09-04-library-foundation/phases/phase-10-packaging-model/artifacts/poc-swiftpm-remote-distribution.md` — Maven 依存、発行 metadata、合成 package、package identity の実測記録
