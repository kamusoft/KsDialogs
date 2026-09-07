# セカンドオピニオン: expand-api-surface (spec-001)
**相方**: codex / **日付**: 2026-08-17 / **対象**: 提案一式 (proposal / design / specs 6本 / tasks / ui-brief)
---
# レビュー結果: expand-api-surface

**判定**: `NEEDS_DISCUSSION`  
**件数**: Critical 0 / Major 8 / Minor 2 / Suggestion 0

## サマリー

ADR の方向性は概ね反映されていますが、公開 API の具体形、宣言的 UI ホストのライフサイクル、KMP Swift 面のキャンセル／可視性に実装側では解決できない仕様上の穴があります。このまま実装すると、実装者ごとに異なる API が成立するほか、Compose の実行時失敗や既存 MAUI 呼び出しのコンパイル退行を受け入れテストが検出できません。

### [🟠 Major] 公開 API の名前・シグネチャが確定していない

**該当箇所**: [design.md:22](kasane/changes/archive/2026-08-19-expand-api-surface/design.md:22)、[ios-native/spec.md:23](kasane/changes/archive/2026-08-19-expand-api-surface/specs/ios-native/spec.md:23)、[android-native/spec.md:23](kasane/changes/archive/2026-08-19-expand-api-surface/specs/android-native/spec.md:23)、[maui-binding/spec.md:19](kasane/changes/archive/2026-08-19-expand-api-surface/specs/maui-binding/spec.md:19)、[kmp-facade/spec.md:5](kasane/changes/archive/2026-08-19-expand-api-surface/specs/kmp-facade/spec.md:5)

**問題点**: 「UIView 版・SwiftUI 版」「View 版・Compose 版」「factory を渡す ShowAsync」までしか決まっていません。特に以下が未確定です。

- Android のインライン Compose 版が `show` か `showCompose` か。同名衝突を避けて登録を `registerCompose` にした理由は、インライン show にも当てはまります。
- 新しい show が `KsDialogs` / `IKsDialogs` 契約にも追加されるか、具象 singleton だけか。
- MAUI の型引数、factory 型、bool 版とカスタム結果版のオーバーロード集合。
- Swift の `@MainActor` / `@Sendable` / `@ViewBuilder` 制約。
- KMP facade の公開エラー型・戻り値型・正確なメソッド名。

公開 API 表面を確定する変更なのに、互換性のない複数実装が同じ Scenario を満たせます。

**推奨修正**: 各形態についてコンパイル可能な宣言レベルの API 表を spec に追加してください。契約 interface/protocol、具象型、レジストリのどこに属するかも明記し、全オーバーロードに利用コード形式の compile Scenario を設けてください。

### [🟠 Major] 「結果型を宣言しない VM」の共通契約が KMP と一致しない

**該当箇所**: [dialog-contract/spec.md:5](kasane/changes/archive/2026-08-19-expand-api-surface/specs/dialog-contract/spec.md:5)、[kmp-facade/spec.md:14](kasane/changes/archive/2026-08-19-expand-api-surface/specs/kmp-facade/spec.md:14)、[DialogViewModel.kt:15](kmp/ksdialogs-kmp/src/commonMain/kotlin/jp/kamusoft/ksdialogs/kmp/DialogViewModel.kt:15)

**問題点**: core spec は「結果型を宣言しない VM は真偽値」と全形態共通に読めますが、KMP commonMain の VM は引き続き `DialogViewModel<R>` であり、`Boolean` の宣言を省略できません。KMP で省略できるのは Swift 登録 API の `result:` です。また Android も型引数自体のデフォルトではなく、別名の bool 用インターフェースを選ぶ方式です。

**推奨修正**: 共通契約を「各公開面に bool 用の省略形を提供する」に直し、形態別の意味を列挙してください。KMP は「共有 VM は `DialogViewModel<Boolean>` のまま、Swift 登録／show の `result:` のみ省略可能」と明示する必要があります。

### [🟠 Major] Compose の配布単位が Open Question のまま実装へ送られている

**該当箇所**: [design.md:61](kasane/changes/archive/2026-08-19-expand-api-surface/design.md:61)、[tasks.md:18](kasane/changes/archive/2026-08-19-expand-api-surface/tasks.md:18)、[build.gradle.kts:59](android/ksdialogs/build.gradle.kts:59)

**問題点**: `ksdialogs` 本体へ Compose 依存を加えるか、`ksdialogs-compose` を新設するかは、公開 artifact、import、依存の推移性、KMP Android からの利用方法を変える設計判断です。実装開始後に選ぶと、凍結済み spec と配布モデルを変更する必要があります。

**推奨修正**: 実装前に配布単位を確定してください。本体に含める場合は必要な Compose artifact と `api`/`implementation` 境界を、分割する場合は Maven 座標、消費者が追加する依存、KMP からの公開方法を proposal/spec/tasks に反映してください。

### [🟠 Major] SwiftUI／Compose ホストの所有・破棄・サイズ契約がない

**該当箇所**: [design.md:14](kasane/changes/archive/2026-08-19-expand-api-surface/design.md:14)、[dialog-contract/spec.md:19](kasane/changes/archive/2026-08-19-expand-api-surface/specs/dialog-contract/spec.md:19)、[proposal.md:26](kasane/changes/archive/2026-08-19-expand-api-surface/proposal.md:26)、[DialogContainerViewController.swift:14](ios/Sources/KsDialogs/Presentation/DialogContainerViewController.swift:14)、[DialogContainer.kt:19](android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/DialogContainer.kt:19)

**問題点**: Requirement は「破棄も同一」としますが、その意味と Scenario がありません。

- iOS の既存 container は `UIView` だけを所有します。`UIHostingController.view` だけを渡す実装では、hosting controller の保持・child containment・appearance lifecycle が保証されません。Apple も `UIHostingController` は通常の ViewController として提示または child に埋め込むよう案内しています。[Apple UIHostingController](https://developer.apple.com/documentation/swiftui/uihostingcontroller/)
- Android の既存 container は `android.app.Dialog` です。`ComposeView` は `LifecycleOwner` と `SavedStateRegistryOwner` を伝播する View tree を要求します。[Android ComposeView API](https://developer.android.com/reference/kotlin/androidx/compose/ui/platform/ComposeView)
- add-layout-spec のケース表適合は proposal のリスク欄にしかなく、SHALL/Scenario/tasks に落ちていません。

**推奨修正**: ホストについて次を受け入れ条件にしてください。

- SwiftUI host controller の保持・containmentと、閉鎖時の解放。
- Compose の lifecycle/saved-state owner、composition disposal。
- completed、cancelled、host destruction、呼び出し元キャンセルの全閉鎖経路。
- 宣言的 UI 経由でも add-layout-spec のサイズケース表を満たすこと。

### [🟠 Major] KMP Swift の直接 show がキャンセル契約を満たせない

**該当箇所**: [kmp-facade/spec.md:19](kasane/changes/archive/2026-08-19-expand-api-surface/specs/kmp-facade/spec.md:19)、[result-notification-semantics.md:24](kasane/concepts/core/api/result-notification-semantics.md:24)、[IosDialogGateway.kt:31](kmp/ksdialogs-kmp/src/iosMain/kotlin/jp/kamusoft/ksdialogs/kmp/IosDialogGateway.kt:31)、[KsDialogsInteropBridge.swift:53](ios/Sources/KsDialogs/Interop/KsDialogsInteropBridge.swift:53)

**問題点**: core 契約は呼び出し元 Task／コルーチンのキャンセル時にダイアログを閉じて `cancelled` に確定するとしています。一方、現行 iOS KMP gateway は「キャンセルされても表示を残す」と明記され、bridge に cancel 操作や show handle がありません。新しい Swift `kmpShow` が completion bridge を async 化するだけでは契約違反が残ります。

**推奨修正**: bridge に show 単位のキャンセル手段を設計し、Swift Task キャンセルが当該ダイアログだけを閉じ、結果を一度だけ確定する Scenario と task を追加してください。

### [🟠 Major] 「機械面の内部化」と KMP cinterop の ABI 要件が矛盾している

**該当箇所**: [kmp-facade/spec.md:37](kasane/changes/archive/2026-08-19-expand-api-surface/specs/kmp-facade/spec.md:37)、[tasks.md:32](kasane/changes/archive/2026-08-19-expand-api-surface/tasks.md:32)、[KsDialogsInteropBridge.swift:13](ios/Sources/KsDialogs/Interop/KsDialogsInteropBridge.swift:13)

**問題点**: ADR は機械面を KMP cinterop 用として残す決定ですが、task は「アクセスレベルの内部化」を要求しています。現在の `@objc public` class/method は KMP からリンクするための ABI 面です。Swift の `internal` へ落とすと、KMP 側の import／リンクが成立しなくなる可能性があります。一方、Scenario は「Sample が直接参照しない」だけなので、アクセスレベル変更の成否を判定できません。

**推奨修正**: 「ABI 上は公開だが利用者向け API ではない」と「Swift access level を下げる」を区別してください。前者なら documentation/SPI/命名上の扱いと、KMP cinterop から引き続き利用可能であることを明記してください。

### [🟠 Major] インライン show の非干渉 Scenario が空レジストリしか検証しない

**該当箇所**: [dialog-contract/spec.md:28](kasane/changes/archive/2026-08-19-expand-api-surface/specs/dialog-contract/spec.md:28)、[design.md:45](kasane/changes/archive/2026-08-19-expand-api-surface/design.md:45)

**問題点**: 現 Scenario は「未登録 VM がインライン show 後も未登録」だけです。一時登録して後で削除する実装でも通りますが、同型の既存登録を消す、並行インライン show が互いの factory を奪う、といった design が避けたい故障を検出できません。

**推奨修正**: 次の Scenario を追加してください。

- 同じ VM 型に既存 factory が登録済みでも、インライン factory がその表示だけで使われ、既存登録は前後で不変。
- 同じ VM 型のインライン show を並行実行しても、それぞれの factory／notifier／結果が独立する。

### [🟠 Major] MAUI の「破壊的変更なし」を既存呼び出し形式で検証していない

**該当箇所**: [proposal.md:24](kasane/changes/archive/2026-08-19-expand-api-surface/proposal.md:24)、[maui-binding/spec.md:5](kasane/changes/archive/2026-08-19-expand-api-surface/specs/maui-binding/spec.md:5)、[DialogRegistryTests.cs:21](maui/KsDialogs.Maui.Tests/DialogRegistryTests.cs:21)、[SampleDialogRegistration.cs:12](samples/maui/KsDialogs.Sample.Maui/SampleDialogRegistration.cs:12)

**問題点**: 新しい1型引数版は、bool VM に対して既存2型引数版と実効的に同じ delegate 型になります。spec は明示1型引数と明示2型引数しか検証せず、現行テストで使われる「明示型付きラムダから全型引数を推論する呼び出し」が曖昧にならないことを保証していません。

**推奨修正**: 現在リポジトリに存在する全形式を compile Scenario として固定してください。

- `Register<TViewModel, bool>(...)`
- `Register<TViewModel>(...)`
- `Register((TViewModel vm, DialogNotifier<bool> notifier) => ...)`
- カスタム結果型の推論呼び出し
- 意図した場合のみ失敗する negative compile check

### [🟡 Minor] 新しい Sample の正確な文言と結果形式が長命規約に存在しない

**該当箇所**: [samples/spec.md:5](kasane/changes/archive/2026-08-19-expand-api-surface/specs/samples/spec.md:5)、[ui/brief.md:5](kasane/changes/archive/2026-08-19-expand-api-surface/ui/brief.md:5)、[sample-parity.md:38](kasane/concepts/cross/conventions/sample-parity.md:38)

**問題点**: パリティ規約は「画面文言の全体」を表として固定していますが、現在は Basic Dialog だけです。新しい3項目について、メニュー名以外のメッセージ、初期入力値、文字列結果の表記が spec から一意に決まりません。

**推奨修正**: 少なくとも変更足場内に4ルート共通の文言表を設け、実装後の蒸留で `sample-parity.md` に反映する対象として tasks に明記してください。

### [🟡 Minor] MAUI の「登録2スタイル提示」が仕様に対応していない

**該当箇所**: [tasks.md:25](kasane/changes/archive/2026-08-19-expand-api-surface/tasks.md:25)

**問題点**: 「型引数明示／明示型付きラムダ」のコード例コメントは proposal の変更内容や samples spec の Requirement/Scenario に対応していません。完了判定が task の文言だけに依存します。

**推奨修正**: 利用例として必須なら samples spec に静的検査可能な Scenario を追加してください。必須でなければ task から外し、phase-9 のドキュメント作業へ送ってください。

## アクションプラン

1. 各言語の公開シグネチャと Compose 配布単位を確定する。
2. bool 省略形の適用範囲、KMP bridge の ABI 上の可視性、キャンセル経路を修正する。
3. SwiftUI／Compose ホストの lifecycle・破棄・サイズ Scenario を追加する。
4. インライン show の既存登録／並行実行非干渉と、MAUI 全既存呼び出し形式の compile Scenario を追加する。
5. Sample の文言表と登録例の受け入れ基準を整える。

指定どおり、ファイル変更・ビルド・テスト実行は行っていません。

## 突き合わせ結果 (2026-08-17 ホスト側判定)

ホスト側自己レビュー (2周・チェックリスト型) はいずれも検出していなかった。採否は根拠で判定:

| # | 指摘 | 採否 | 根拠 |
|---|---|---|---|
| 1 | 公開 API の名前・シグネチャ未確定 | **採用** | 宣言レベルの API 表 + 全オーバーロードの compile Scenario を spec に追加 |
| 2 | bool 既定の共通契約が KMP commonMain と不一致 | **採用** | core Requirement を「各公開面に bool 用の省略形を提供」に修正し、形態別の意味 (KMP は Swift 面の result: 省略のみ、共有 VM は DialogViewModel<Boolean> のまま) を列挙 |
| 3 | Compose 配布単位が Open Question のまま実装送り | **採用 (解決済み)** | オーナー判断 (2026-08-17): 別モジュール ksdialogs-compose に確定 (design Decision 7)。cross/ADR-0008 追記は蒸留へ申し送り |
| 4 | SwiftUI/Compose ホストの所有・破棄・サイズ契約なし | **採用** | UIHostingController の containment / ComposeView の lifecycle owner / 全閉鎖経路 / ケース表適合を受け入れ条件に追加 |
| 5 | KMP Swift 直接 show がキャンセル契約 (concepts) を満たせない | **採用** | bridge に show 単位キャンセル手段を設計し Scenario / task を追加 |
| 6 | 機械面内部化と cinterop ABI の矛盾 | **採用** | 「ABI 上は公開・利用者向けではない (doc/命名)」へ言い換え、KMP から利用可能なことを明記 |
| 7 | インライン非干渉 Scenario が空レジストリのみ | **採用** | 既存登録との共存・並行インライン show の独立の Scenario を追加 |
| 8 | MAUI 既存呼び出し全形式の compile 検証なし | **採用** | 4形式 + negative compile check を Scenario 化 |
| 9 (Minor) | Sample 新項目の文言表が無い | **採用** | change 足場内に4ルート共通の文言表を設け、蒸留での sample-parity.md 反映を tasks に明記 |
| 10 (Minor) | 登録2スタイル提示が spec 非対応 | **採用** | samples spec に静的検査可能な Requirement として追加 (phase-5-2 決定「両スタイルを Sample で示す」の反映) |

降格: なし / 未解決: なし (全件反映済み。#3 はオーナー判断で別モジュールに確定)
