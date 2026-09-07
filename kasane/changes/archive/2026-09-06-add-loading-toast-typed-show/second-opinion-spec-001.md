# セカンドオピニオン: add-loading-toast-typed-show (spec-001)
**相方**: codex / **label**: so-spec-add-loading-toast-typed-show / **日付**: 2026-09-06 / **対象**: kasane/changes/add-loading-toast-typed-show/ の proposal.md / specs/ / tasks.md (提案一式)
---
# レビュー結果: add-loading-toast-typed-show

**日付**: 2026-09-06  
**判定**: NEEDS_DISCUSSION  
**件数**: Critical 0 / Major 7 / Minor 2 / Suggestion 0

静的レビューのみ実施し、ビルド・テスト・ファイル変更は行っていません。

## 照合した規約

- `comment-policy.md`（常時）
- `test-execution.md`（検証計画・完了判定）
- `sample-parity.md`（Sample 変更）
- `aiforms-origin-reference.md`（Loading の既存契約）
- Swift / Kotlin / C# / MAUI の実装・レビュー規律

## 指摘事項

### [🟠 Major] Toast の「同期失敗・UI スレッド実行・任意スレッド呼び出し」が同時には成立しない

**該当箇所**: `specs/toast-contract/spec.md:18`、`specs/toast-contract/spec.md:20`、`specs/ios-native/spec.md:9`、`specs/android-native/spec.md:9`、`specs/maui-binding/spec.md:9`

**問題点**: 新仕様は次の3条件を同時に要求しています。

- `show` は同期 fire-and-forget
- VM factory/configure は MainActor/Main dispatcher/UI スレッドで実行
- factory/configure の例外は呼び出し時点で同期伝播

一方、既存契約では任意スレッドから呼び出せます（`ios/Sources/KsDialogs/Presentation/KsToast.swift:13`、`android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/KsToast.kt:16`、`maui/KsDialogs.Maui/Presentation/IKsToast.cs:18`）。現行実装も UI 処理を後段へキュー投入しています（`ios/Sources/KsDialogs/Presentation/ToastCoordinator.swift:91`、`android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/ToastCoordinator.kt:96`）。

バックグラウンドスレッドから同期例外を返すには、UI スレッドへ同期 dispatch して呼び出し元をブロックする必要があります。これは現在の非同期受理モデルと異なり、デッドロック、再入、受理順序の変化を持ち込みます。単なる実装詳細ではなく公開契約の選択です。

**推奨修正**: 次のいずれを正とするか決定し、Requirement と Scenario に固定してください。

1. UI スレッド保証を維持し、型指定 Toast だけ async/suspend にする。
2. 同期 API を維持し、VM factory/configure は呼び出しスレッドで行う。
3. 同期 UI-thread hop を採用し、ブロッキング・再入・main/background 双方の意味を契約化する。
4. UI キュー上の失敗を受理後失敗へ分類し、同期伝播を諦める。

少なくとも main/background 呼び出し、configure 例外、再入呼び出しを検証する Scenario が必要です。

### [🟠 Major] Loading の合流時に VM を生成するか、誰へ進捗を送るかが未定義

**該当箇所**: `specs/loading-contract/spec.md:9`、`specs/loading-contract/spec.md:16`、`specs/loading-contract/spec.md:23`、`specs/loading-contract/spec.md:50`、`specs/loading-contract/spec.md:57`

**問題点**: 型指定経路は毎回「VM 生成 → configure → 進捗受け口紐付け → View 生成」としていますが、既存 Loading 契約は重なった利用を1表示へ合流し、最初の開始の中身を維持します（`kasane/concepts/core/api/loading-semantics.md:44`、`kasane/concepts/core/api/loading-semantics.md:48`）。

現行 coordinator も、合流時には新しい View を生成せず登録だけを検証します（`ios/Sources/KsDialogs/Presentation/LoadingCoordinator.swift:120`、`android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/LoadingCoordinator.kt:181`）。そのため、表示中に型指定 start が合流した場合について次が決まっていません。

- 新しい VM factory/configure を呼ぶのか。
- 生成した VM を表示しないまま破棄するのか。
- action の進捗は最初の表示の VM と新しく生成した VMのどちらへ送るのか。
- configure が失敗した合流を合流数に含めるのか。
- 非同期 configure の中断中に別の開始が先に表示を確定した場合、どちらを「最初」とするのか。

LD-TY-08 の「生成された VM が進捗を受け取る」は、合流時の既存契約と衝突し得ます。

**推奨修正**: 「新規表示世代」と「既存世代への合流」を分けて規定してください。factory/configure の呼出回数、表示 VM、進捗転送先、失敗時の合流数を明記し、次を Scenario 化してください。

- 既存の built-in/custom Loading へ型指定 start が合流する。
- 非同期 configure 中に別の開始が到着する。
- configure 中の再登録。
- 合流側の factory/configure が失敗する。

### [🟠 Major] C# の型指定 start 署名が定義されておらず、既存呼び出しを壊す候補も残っている

**該当箇所**: `specs/maui-binding/spec.md:9`、`specs/maui-binding/spec.md:11`、`proposal.md:33`、`tasks.md:19`、`tasks.md:20`

**問題点**: Show の署名は記載されていますが、型指定 StartAsync は「値なし / 値あり × 同期 / 非同期 configure」としか書かれておらず、型引数、引数順、既定値が未定です。これでは最重要リスクであるオーバーロード解決を提案レビューできません。

既存 API は `StartAsync<T>` を処理の戻り値型に使用しています（`maui/KsDialogs.Maui/Presentation/IKsLoading.cs:110`、`:122`、`:133`、`:144`、`:159`、`:177`）。新しい `StartAsync<TViewModel>` と型引数1個で競合するため、具体的な呼び出し構文が必要です。

また、提案済みの Show 署名でも、たとえば次は既存インライン版と新しい型指定版の両方が候補になり、ソース互換性を壊します。

```csharp
loading.ShowAsync<MyViewModel>(null, null);
toast.Show<MyViewModel>(null, null);
```

これは `proposal.md:31` の「破壊的変更なし」と一致しません。

**推奨修正**: 全 StartAsync 署名を仕様へ列挙し、以下を含む compile matrix と期待する静的戻り値型を定義してください。

- 値なし / 値あり
- configure なし / Action / Func<Task>
- async lambda が `async void` 側へ束縛されないこと
- placement 指定
- 既存インスタンス/インライン版
- 明示的な `null`
- 値あり版で指定すべき型引数の個数

「曖昧なら実装中に止める」ではなく、公開 API 形状を提案段階で確定する必要があります。

### [🟠 Major] Swift の VM factory は現行 Dialog と同型にすると例外を表現できない

**該当箇所**: `proposal.md:14`、`specs/ios-native/spec.md:3`、`specs/ios-native/spec.md:9`

**問題点**: proposal は VM factory の例外伝播を保証していますが、「Dialog と同じ」現行登録は非 throwing closure です（`ios/Sources/KsDialogs/Registry/DialogViewRegistry.swift:80`、`ios/Sources/KsDialogs/Registry/DialogViewModelFactory.swift:6`）。そのまま複製すると、Swift 利用者は失敗する VM factory を登録できず、契約を表現できません。

さらに LD-TY/TS-TY の Scenario は configure 失敗しか扱っておらず、VM factory 失敗を検証できません。

**推奨修正**: Swift の新しい登録を `@MainActor @Sendable () throws -> ViewModel` とするのか、VM factory は非 throwing として core 契約を狭めるのか決定してください。前者なら、型消去 factory も throwing にし、Loading/Toast 双方へ factory 例外の Scenario を追加してください。

### [🟠 Major] 「呼び出し時点のエントリスナップショット」を検証する Scenario がない

**該当箇所**: `specs/loading-contract/spec.md:9`、`specs/toast-contract/spec.md:9`、`specs/loading-contract/spec.md:11`、`specs/toast-contract/spec.md:11`

**問題点**: 現在の再登録 Scenario は「再登録後の次回 show」または「既に表示済みの View」を見るだけで、show が取得した entry を View 生成まで保持することを検証できません。

特に iOS Toast の現行コードは、呼び出し時に factory の存在を検査した後（`ios/Sources/KsDialogs/Presentation/ToastCoordinator.swift:96`）、生の request をキューへ積み（`:104`）、UI キュー上でレジストリを再解決しています（`:272`）。これは今回の実装で同じ誤りが入り得る具体的な経路です。

**推奨修正**: UI キューまたは非同期 configure を停止し、型指定 show の呼び出し後かつ View 生成前に両スロットを再登録しても、最初に取得した VM/View factory の組が使われる Scenario を Loading/Toast の両方へ追加してください。

### [🟠 Major] MAUI の責務である進捗・duration・placement のパススルーが検証対象から除外されている

**該当箇所**: `specs/maui-binding/spec.md:3`、`tasks.md:22`、`specs/loading-contract/spec.md:50`、`specs/toast-contract/spec.md:42`

**問題点**: MAUI 検証対象から LD-TY-08 と TS-TY-06 が除外されています。しかし次は Native の責務ではなく、C# facade が生成した VMと引数を正しく request へ載せる責務です。

- 生成 VM が `ILoadingProgressReceiver` として渡ること  
  (`maui/KsDialogs.Maui/Internals/LoadingPresenter.cs:60`)
- typed Toast の duration/placement が request に保持されること  
  (`maui/KsDialogs.Maui/Internals/ToastPresenter.cs:63`)

Native テストだけでは、C# typed overload がこれらを落としても検出できません。また Loading の placement は Requirement に含まれる一方、専用 Scenario 自体がありません。

**推奨修正**: MAUI facade テストへ LD-TY-08、TS-TY-06、Loading の placement パススルーを追加してください。値だけでなく、生成した VM インスタンスと進捗 receiver が同一であることも確認すべきです。

### [🟠 Major] tasks.md の完了検証がプロジェクト規約と KMP 互換リスクを満たさない

**該当箇所**: `tasks.md:26`、`tasks.md:29`、`tasks.md:30`

**問題点**: tasks は iOS / Android JVM / MAUI の3実行と「KMP のビルド」だけです。一方、`test-execution.md` は Android instrumented、KMP `allTests`、MAUI Android/iOS 互換面を独立した完了経路として定めています（`kasane/handbook/cross/test-execution.md:19`、`:99`、`:128`、`:153`）。

今回変更される registry の内部 API は、KMP iOS が直接使用しています（`ios/Sources/KsDialogs/Kmp/KmpLoadingViewModel.swift:41`、`ios/Sources/KsDialogs/Kmp/KmpToastViewModel.swift:29`）。コンパイルだけでは factory slot の保持や解決結果を確認できません。

Sample も3ルートしか通さない計画ですが、spec 自身は4ルートの観察結果を同一に保つとしています（`specs/samples/spec.md:9`）。パリティ規約も4ルートを検証装置としています。

**推奨修正**: 少なくとも以下を tasks の完了ゲートへ入れてください。

- Android JVM + instrumented
- KMP `allTests`
- MAUI `dotnet test`
- MAUI Android/iOS 互換面テスト
- scenario-id-coverage `--require-mirror`
- 4ルートの Sample 比較、または未変更 KMP ルートについて再利用できる基準証跡の明記

### [🟡 Minor] Toast の SwiftUI / Compose 登録との組み合わせが未検証

**該当箇所**: `specs/ios-native/spec.md:21`、`specs/android-native/spec.md:21`、`tasks.md:9`、`tasks.md:15`

**問題点**: 宣言的 UI 登録との組み合わせ Scenario は Loading だけです。Toast は別レジストリ・別 factory 型・別 coordinator を通るため、Loading のテストでは代替できません。

**推奨修正**: Toast にも SwiftUI 登録 + typed show、`registerCompose` + typed show の Scenario を追加してください。

### [🟡 Minor] Android の value class 拒否は Loading 側が網羅されていない

**該当箇所**: `specs/android-native/spec.md:9`、`specs/android-native/spec.md:26`、`tasks.md:15`

**問題点**: Requirement は Loading/Toast の登録時・型指定 show 時の双方を拒否するとしていますが、Scenario と task は Toast だけです。

**推奨修正**: Loading の `registerViewModel` と typed show/start でも `ValueClassViewModel` になることを明示的に検証してください。

## アクションプラン

1. Toast の同期/UI-thread/任意スレッド契約について設計判断を確定する。
2. Loading の合流時と async configure 中の受理規則を確定する。
3. C# の全公開署名と互換性 compile matrix を仕様へ記載する。
4. Swift VM factory の throwing 契約を確定する。
5. スナップショット、MAUI パススルー、宣言的 UI、value class の Scenario を補う。
6. tasks.md の実行経路をプロジェクトの完了規約へ合わせる。

総合判定: NEEDS_DISCUSSION

## 突き合わせ結果 (2026-09-06)

ホスト側の自己レビュー (2 周、指摘なし) との突き合わせ。採否は ksn-second-opinion の規則による。

| 相方の指摘 | 採否 | 扱い |
|---|---|---|
| Toast の同期失敗 / UI スレッド / 任意スレッドが両立しない | **採用** (設計判断 → オーナー決定) | VM factory 未登録は呼び出し時点の同期失敗、VM factory / configure の例外は「受理後の失敗」に分類 (toast-contract TS-TY-04・08・09、core/ADR-0035 追記) |
| Loading の合流時に VM 生成 / 進捗転送先が未定義 | **採用** (設計判断 → オーナー決定) | 生成・configure 後にインスタンス渡しと同じ合流判定 (loading-contract の合流節、LD-TY-08 条件付け、LD-TY-14・15 追加) |
| C# の型指定 start 署名が未定義 | **採用** | maui-binding に署名表と compile matrix (LD-YM-01 / TS-YM-01) |
| 〃 のうち「null リテラル呼び出しの曖昧化は破壊的変更」 | **降格** | Dialog 型指定 show と既存インライン版の間でも同じ曖昧さがあり、意味のある呼び出しではない。互換性の対象外と spec に明記 |
| Swift の VM factory が非 throwing で例外を表現できない | **採用** | Dialog と同じ非 throwing と明記 (ios-native)。VM factory 失敗の Scenario (LD-TY-13 / TS-TY-08) は失敗を表明できる形態のみ |
| スナップショット解決の Scenario がない | **採用** | LD-TY-12 / TS-TY-07 追加 |
| MAUI のパススルー (進捗受け口 / duration / placement) が検証対象外 | **採用** | maui-binding の検証対象に LD-TY-08・11 / TS-TY-06 を追加 |
| tasks の完了ゲートが規約を満たさない | **採用** | tasks 5.1 / 5.2 / 4.2 を test-execution・sample-parity に合わせて改訂 |
| Toast の SwiftUI / Compose 組み合わせ未検証 (Minor) | **採用** | TS-YI-02 / TS-YA-03 追加 |
| Android の value class 拒否が Loading 側未網羅 (Minor) | **採用** | LD-YA-03 追加 |

未解決: なし。
