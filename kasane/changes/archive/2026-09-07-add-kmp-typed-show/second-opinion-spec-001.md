# セカンドオピニオン: add-kmp-typed-show (spec-001)
**相方**: codex / **label**: so-spec-add-kmp-typed-show / **日付**: 2026-09-06 / **対象**: kasane/changes/add-kmp-typed-show/ の proposal.md / specs/ / tasks.md (提案一式)
---
## サマリー

設計判断が未確定の箇所と、現行コードでは仕様どおり成立しない経路があります。特に、型指定時の `KClass` キー消失、core 契約との実行文脈不整合、Swift 境界の例外契約は実装方法を先に決め直す必要があります。

静的レビューのみ実施し、ビルド・テスト・ファイル書き込みは行っていません。

## 指摘事項

### [🟠 Major] VM factory の登録キーがインスタンス渡し時に失われる

**該当箇所**: `specs/kmp-facade/spec.md:30`

**問題点**: 仕様は `viewModelClass: KClass<VM>` で factory を解決した後、生成物を既存のインスタンス渡し show に流します。しかし Native 側のインスタンス渡し経路は、渡されたオブジェクトの実行時クラスで View factory を再解決します。

- Android: `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/DialogPresenter.kt:28`
- iOS Dialog: `ios/Sources/KsDialogs/Presentation/DialogPresenter.swift:16`
- iOS Loading: `ios/Sources/KsDialogs/Kmp/KmpLoadingViewModel.swift:40`
- iOS Toast: `ios/Sources/KsDialogs/Kmp/KmpToastViewModel.swift:28`

したがって、合法な登録である次の形が壊れます。

```kotlin
registry.registerViewModel(BaseViewModel::class) { DerivedViewModel() }
show(BaseViewModel::class)
```

Native の型指定 show は要求された型のエントリから View factory もスナップショットするため、この構成でも動きます。KMP 案だけは `DerivedViewModel::class` で再解決し、未登録になります。「インスタンス渡しと同じ経路なので Native と同じ」という `proposal.md:13` の説明も成立しません。

**推奨修正**: 次のどちらを契約として決定し、Scenario を追加してください。

- factory の生成物の実行時クラスが登録キーと完全一致しなければ、明示的な型不一致として失敗させる。
- 要求された `KClass` に対応する View factory を Native 提示まで保持する。その場合は現行の「橋渡し変更なし」を再検討する。

### [🟠 Major] 呼び出し元文脈での実行が現行 core 契約と矛盾する

**該当箇所**: `proposal.md:14`

**問題点**: 提案は VM factory/configure を呼び出し元文脈で実行すると定めています。一方、現行契約は「VM factory と configure は View factory と同じ UI スレッド保証」と明記しています。

- `kasane/concepts/core/api/model-binding-semantics.md:69`
- `kasane/concepts/core/api/model-binding-semantics.md:72`

`kmp/ADR-0006` は commonMain で生成・configure することは決めていますが、UI スレッド保証を外す決定までは記録していません。また提案は影響能力を `kmp-facade` と `samples` に限定しており、core 契約の変更として扱っていません。

**推奨修正**: KMP だけを caller-context とする例外を正式に core 契約へ導入するか、既存どおり UI スレッドへ移すかを決定してください。前者なら ADR と Impact、蒸留対象にこの契約差を明記してください。

### [🟠 Major] `@Throws` が factory/configure の任意例外を Swift 境界で運べない

**該当箇所**: `specs/kmp-facade/spec.md:32`

**問題点**: 仕様は factory/configure が投げた「その例外」を伝播するとしながら、宣言するのは `DialogException` と `CancellationException` だけです。

Kotlin/Native では、`@Throws` に列挙された型またはそのサブクラス以外が Swift/ObjC 境界へ達するとプロセス終了になります。非 suspend 関数では未宣言例外を伝播できません。[Kotlin公式ドキュメント](https://kotlinlang.org/docs/native-objc-interop.html?section=posts)

例えば factory が `IllegalStateException` を投げると、共通 Kotlin 内では伝播しても Swift から直接呼ばれた場合の契約は成立しません。正の Kotlin compile 検査だけでは `@Throws` の Swift 表現も実行時挙動も検証できません。

**推奨修正**: 次のいずれかを決定してください。

- `Throwable` 相当まで `@Throws` に含める。
- 非キャンセル例外を `DialogException` に正規化する。
- Swift 向け API ではないなら `@HiddenFromObjC` で新オーバーロードを非公開にする。

併せて、生成された Swift 面の compile 検査または Swift 境界を実際に越える例外テストを受け入れ基準に加えてください。

### [🟠 Major] value class ViewModel の拒否契約が抜けている

**該当箇所**: `specs/kmp-facade/spec.md:11`

**問題点**: core 契約は ViewModel を参照型に限定し、すべての show 経路で値型が提示に至らないことを要求しています。

- `kasane/concepts/core/api/model-binding-semantics.md:44`
- `kasane/concepts/core/api/model-binding-semantics.md:48`
- `kasane/concepts/core/api/model-binding-semantics.md:99`

Kotlin の型制約だけでは `value class : DialogViewModel<R>` を排除できません。新しい commonMain レジストリは value class の `KClass` と factory を受理できる一方、提案・spec・tasks のいずれにも拒否点や失敗型がありません。Android Native の既存検査へ到達するまで遅延させると、iOS と失敗点・失敗理由が一致しません。

**推奨修正**: Dialog／Loading／Toast の登録時および型指定 show で value class をどう拒否するかを仕様化してください。commonMain だけで判定できない場合は expect/actual 検査を設け、3機能それぞれの実行時 Scenario を追加してください。

### [🟠 Major] 「別 change と独立」という主張と必須テストが矛盾する

**該当箇所**: `proposal.md:25`

**問題点**: 提案は `add-loading-toast-typed-show` と独立であるとしていますが、LD-KT-01 は Android Native の Loading レジストリへ VM factory を登録することを前提としています。

- `specs/kmp-facade/spec.md:23`
- `tasks.md:12`

現行 `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/LoadingViewRegistry.kt:28` には View factory 登録しかなく、`registerViewModel` は別 change の変更です。この change を単独で実装すると、必須テスト自体が記述できません。

**推奨修正**: 独立性を維持するなら、すでに Native VM factory を持つ Dialog で分離性を検証してください。Loading 固有の検査が必要なら change 間依存と実装順を proposal/tasks に明記してください。

### [🟠 Major] Loading／Toast の commonTest が本番の型指定実装を検証しない

**該当箇所**: `tasks.md:11`

**問題点**: Dialog の実装は commonMain の `GatewayKsDialogs` にあるため commonTest で本番コードを通せます。一方、Loading／Toast は現在、`AndroidLoadingGateway`／`IosLoadingGateway` と `AndroidToastGateway`／`IosToastGateway` が直接 interface を実装しています。

tasks は commonTest の `FakeKsLoading`／`FakeKsToast` に新 API を実装して Scenario を検証するとしていますが、それでは Fake の複製実装だけが通り、実際の4つの gateway が factory解決・configure・例外伝播を誤っていても green になります。

**推奨修正**: Loading／Toast にも commonMain の decorator/gateway または共通 resolver を設け、本番とテストが同じ前段を通る構造にしてください。構造を変えない場合は、PB-KT-01〜08相当を Android/iOS双方の実 gateway テストへ展開してください。

### [🟠 Major] PB-KT-09 の iOS 検査方法では ObjC クラスキー同一性を証明できない

**該当箇所**: `specs/kmp-facade/spec.md:74`

**問題点**: Scenario は「生成物の ObjC クラスが Swift レジストリのキーになる」ことを期待していますが、記載された互換面の差し替えは Kotlin オブジェクトを受け取るだけで、Swift 側の `type(of:)` とレジストリ検索を通りません。

現行の実際のキー解決は次にあります。

- `ios/Sources/KsDialogs/Kmp/KmpLoadingViewModel.swift:40`
- `ios/Sources/KsDialogs/Kmp/KmpToastViewModel.swift:28`
- `ios/Sources/KsDialogs/Presentation/DialogPresenter.swift:16`

**推奨修正**: `object_getClass` で実 bridge に View factory を登録し、common factory が生成したオブジェクトを `KSDInterop*Bridge` へ渡して、登録済み factory が引かれるところまで検証してください。Sample の手動確認は実提示の補完とし、キー同一性の唯一の判定手段にしないでください。

### [🟠 Major] 既存の Toast 負コンパイル検査が新 API と正面衝突する

**該当箇所**: `tasks.md:13`

**問題点**: `KsToast.registry` の追加後、既存の負検査は成功してしまいます。

- `kmp/api-surface-check/src/negativeCheckToastRegistration/kotlin/jp/kamusoft/ksdialogs/kmp/apicheck/RejectsToastRegistration.kt:14`
- `kmp/api-surface-check/build.gradle.kts:56`
- `kasane/handbook/cross/test-execution.md:235`

tasks は正の compile 検査追加しか挙げていないため、負検査とテスト実行規約が「registry は存在してはならない」という旧契約のまま残ります。通常の `allTests` ではこの負検査は起動されないため、完了タスクも検出できません。

**推奨修正**: この負検査を削除するのではなく、「View factory 登録 API は依然として見えない」ことを検査する形へ置き換えてください。build定義、期待診断、handbookの負検査一覧・件数も更新対象へ加えてください。

### [🟠 Major] commonMain レジストリの並行アクセス契約がない

**該当箇所**: `specs/kmp-facade/spec.md:11`

**問題点**: 再登録の後勝ちと呼び出し時スナップショットは定義されていますが、登録とshowが異なるスレッドから並行した場合の原子性・可視性が決まっていません。現行公開面は任意スレッド呼び出しを保証し、Nativeレジストリはいずれもロックで保護しています。

- `kmp/ksdialogs-kmp/src/commonMain/kotlin/jp/kamusoft/ksdialogs/kmp/KsLoading.kt:13`
- `kmp/ksdialogs-kmp/src/commonMain/kotlin/jp/kamusoft/ksdialogs/kmp/KsToast.kt:16`
- `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/DialogViewRegistry.kt:13`
- `ios/Sources/KsDialogs/Registry/DialogViewRegistry.swift:10`

commonMain の通常の `MutableMap` で実装すると、後勝ち・スナップショットの双方を保証できず、Kotlin/Native ではデータ競合になります。

**推奨修正**: 3レジストリについて「登録と解決は任意スレッドから行え、1回の解決は原子的なスナップショット」と明記し、並行再登録と解決を観察するテストを加えてください。同期手段をcommonMainだけで持つのか、expect/actualにするのかも設計対象です。

## アクションプラン

1. 登録キーとfactory生成物の実行時型が異なる場合の意味論を決める。
2. caller-context、value class、Swift例外境界、並行アクセスを仕様・ADRへ反映する。
3. 別changeへの依存を解消するか、依存関係を明示する。
4. Loading／Toastの本番前段を直接検証できる構造にする。
5. 実bridgeによるiOSキー同一性検査とSwift例外境界検査を追加する。
6. 既存のToast負コンパイル検査とhandbookを新しい公開面へ更新する。

総合判定: NEEDS_DISCUSSION

## 突き合わせ結果 (2026-09-06)

ホスト側の自己レビュー (2 周、指摘なし) との突き合わせ。採否は ksn-second-opinion の規則による。

| 相方の指摘 | 採否 | 扱い |
|---|---|---|
| VM factory の登録キーがインスタンス渡し時に失われる (サブクラスを返す factory) | **採用** (設計判断 → オーナー決定) | 生成物の実行時クラスと登録キーの一致を要求し、違えば型不一致で失敗 (PB-KT-13、kmp/ADR-0006 追記) |
| 呼び出し元文脈での実行が core 契約と矛盾 | **採用** (記録の不足) | KMP 限定の例外として kmp/ADR-0006 に追記、proposal に core concept の追随を明記 |
| `@Throws` で任意例外を Swift 境界に運べない | **採用** (設計判断 → オーナー決定) | 型指定 show を `@HiddenFromObjC` で隠す (PB-KT-14、kmp/ADR-0006 追記) |
| value class の拒否契約が無い | **一部採用** | 共有コードでは判定できないため各 OS の既存判定に委ねると明記。新たに開く穴ではない (インスタンス渡しと同じ) ので Scenario は追加しない |
| 「別 change と独立」と LD-KT-01 が矛盾 | **採用** | Dialog の Native 登録口で検証する PB-KT-11 に置き換え |
| Loading / Toast の commonTest が本番を検証しない | **採用** | commonMain デコレータ (`GatewayKsLoading` / `GatewayKsToast`) を新設し本番の前段を Test gateway で検証 |
| PB-KT-09 の iOS 検査がキー同一性を証明しない | **採用** | 実 bridge に `object_getClass` で登録する既存手段に変更 |
| 既存の Toast 負検査と衝突 | **採用** | 「View factory 登録 API が見えない」検査に置き換え (TS-KT-02)、handbook の一覧は蒸留時に追随 |
| レジストリの並行アクセス契約が無い | **採用** | 任意スレッド・原子的スナップショットを明記 (PB-KT-12) |

未解決: なし。
