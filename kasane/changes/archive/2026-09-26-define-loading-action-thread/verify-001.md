# 一致検証: define-loading-action-thread (001 回目)

**日付**: 2026-09-25
**判定**: INVALID (❌ 1 件。Scenario の対応はすべて一致しており、残っているのは完了確認 tasks 6.1 が閉じていないことだけ)

## サマリー

- 検証の範囲は、コミット b0657a9 からの作業ツリーの差分 (追跡の変更と未追跡の新規ファイル)
- デルタスペック 6 能力の 24 Scenario は、実装・テストとも全件一致 (✅ 24)
  - LD-TH 8・LD-HI 2・LD-HA 2・LD-HM 6・LD-HK 4・LD-HS 2
  - LD-HS-01・02 は、Sample の通しの証跡で受け入れた
- deviation.md の 3 項 (付随修正 2・design Decision 3 の乖離 1) は、合意済みの差分として扱った
- 足場 (proposal / design / specs) の逆流は無い
- tasks.md に虚偽のチェックは無い
- ❌ は 1 件。tasks 6.1 (全ルートの全件実行) が未チェックで、実態もそれに合っている。Android の報告経路を直した後、`:ksdialogs-core` の instrumented 全件を回し直しておらず、同じルートに範囲外の既知の失敗 (A 5 件・C 1 件) も残っている。完了の条件である「テスト成功」を、最終のコードで全件について確かめられない

## 対応表

### loading-contract (挙動。iOS / Android の同名テストで全量を検証)

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| [LD-TH-01] 既定で、UI スレッドから呼んでも UI スレッドで始まる | iOS: `ios/Sources/KsDialogs/Presentation/Loading.swift:105` (action の型が `@MainActor`)、`:230` (`runScope`)<br>Android: `android/ksdialogs-core/src/main/kotlin/jp/kamusoft/ksdialogs/Loading.kt:172` (`runScope`)、`:180` (`MAIN -> Dispatchers.Main.immediate`) | `ios/Tests/KsDialogsTests/LoadingActionThreadTests.swift:26`<br>`android/ksdialogs-core/src/androidTest/kotlin/jp/kamusoft/ksdialogs/LoadingActionThreadTests.kt:54` | ✅ 一致 |
| [LD-TH-02] 既定で、UI スレッド外から呼んでも UI スレッドで始まる | 同上 | iOS `LoadingActionThreadTests.swift:40` (`Task.detached` から呼ぶ)<br>Android `LoadingActionThreadTests.kt:69` (`Dispatchers.Default` から呼ぶ) | ✅ 一致 |
| [LD-TH-03] UI スレッド外の指定で、UI スレッドから呼んでも UI スレッド外で始まる | iOS: 利用者の `@concurrent` (言語規則)<br>Android: `Loading.kt:172` の `BACKGROUND -> Dispatchers.Default` | iOS `LoadingActionThreadTests.swift:61`<br>Android `LoadingActionThreadTests.kt:84` | ✅ 一致 |
| [LD-TH-04] UI スレッド外の指定で、UI スレッド外から呼んでも UI スレッド外で始まる | 同上 | iOS `LoadingActionThreadTests.swift:75`<br>Android `LoadingActionThreadTests.kt:102` | ✅ 一致 |
| [LD-TH-05] カスタム View の入口でも、既定は UI スレッドで始まる | iOS: `Loading.swift:114` / `:128` / `:142` / `:156` の各入口 → `runScope`<br>Android: `Loading.kt` の `start` 4 本 → `runScope` | iOS `LoadingActionThreadTests.swift:98`: インスタンス渡し・インライン UIKit・インライン SwiftUI・型指定の 4 入口<br>Android `LoadingActionThreadTests.kt:121`: インスタンス渡し・インライン・型指定の 3 入口。どちらも UI スレッド外から呼ぶ | ✅ 一致 |
| [LD-TH-06] カスタム View の入口でも、UI スレッド外の指定が効く | 同上 | iOS `LoadingActionThreadTests.swift:118`<br>Android `LoadingActionThreadTests.kt:137`。どちらも UI スレッドから呼ぶ | ✅ 一致 |
| [LD-TH-07] UI スレッド外で始まった action からの進捗報告が届く | iOS: 既存の `LoadingReportQueue` (`Loading.swift:230` 以降)<br>Android: `android/ksdialogs-core/src/main/kotlin/jp/kamusoft/ksdialogs/LoadingCoordinator.kt:279` (`report` が列に積む)、`:290` (`awaitAcceptedReports`)、`:317` (受理の列)、`Loading.kt:201` (`endUseAfterPendingReports`) | iOS `LoadingActionThreadTests.swift:155` (8 回反復)<br>Android `LoadingActionThreadTests.kt:155` (40 回反復) | ⚠️ deviation 記録済み (Android の報告経路の作り直し = deviation.md の design Decision 3 の項)。挙動は Scenario どおり |
| [LD-TH-08] どちらの指定でも、失敗は伝播して表示が閉じる | iOS `runScope` / Android `runScope` の catch → `endUseAfterPendingReports` | iOS `LoadingActionThreadTests.swift:183`<br>Android `LoadingActionThreadTests.kt:227`。どちらも伝播・合流数 0・非表示を確認 | ✅ 一致 |

`--require-mirror` で、LD-TH の 8 本が iOS / Android の双方にあることを確かめた (後述)。

### ios-native

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| Requirement 本文 (13 入口の action の型・`@concurrent`・doc コメント) | `ios/Sources/KsDialogs/Presentation/KsLoading.swift`: protocol 5 本 (`:107` `:118` `:129` `:140` `:154`)、extension 8 本 (`:177` `:186` `:195` `:221` `:231` `:261` `:271` `:281`)。doc コメント `:89-102`。`Loading.swift` の実装 5 本と `runScope:230`。スレッド指定の引数は足していない | (下の 2 本) | ✅ 一致 |
| [LD-HI-01] 公開面の正の compile 検査 | 同上 | `ios/Tests/KsDialogsTests/LoadingApiSurfaceCompileChecks.swift` に 13 入口 × 3 通り<br>- `:248`: MainActor の状態に `await` なしで触れる<br>- `:292`: `@concurrent`<br>- `:335`: `await MainActor.run`<br>テストターゲットのビルドに含まれる (ios 286 件成功) | ✅ 一致 |
| [LD-HI-02] isolation の指定が無い関数を名前で渡すと UI スレッド外で始まる | 言語規則 (Loading 側に切り替えの処理は無い。`Loading.swift:226-229` のコメント) | `LoadingActionThreadTests.swift:207` + `:267` の nonisolated 関数 | ✅ 一致 |

### android-native

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| Requirement 本文 (enum・`start` 4 本・`startCompose`・既定値 `MAIN`・action の直前) | `android/ksdialogs-core/src/main/kotlin/jp/kamusoft/ksdialogs/LoadingActionThread.kt:10`<br>`android/ksdialogs-core/src/main/kotlin/jp/kamusoft/ksdialogs/KsLoading.kt:140` `:159` `:180` `:204`<br>`android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/compose/ComposeLoadingShow.kt:50` | (下の 2 本) | ✅ 一致 |
| [LD-HA-01] 公開面の正の compile 検査 | 同上 | `android/api-surface-check/src/main/kotlin/jp/kamusoft/ksdialogs/apicheck/LoadingApiSurfaceChecks.kt:199`。4 本 + `startCompose` を、省略 / `MAIN` / `BACKGROUND` と trailing lambda で書く。`:api-surface-check:compileDebugKotlin` は `./gradlew test` に含まれ、68 件成功 | ✅ 一致 |
| [LD-HA-02] Compose の `startCompose` でも指定が効く | `ComposeLoadingShow.kt:50` → `actionThread = actionThread` | `android/ksdialogs/src/androidTest/kotlin/jp/kamusoft/ksdialogs/compose/ComposeLoadingActionThreadTests.kt:37`。`Dispatchers.Default` から、指定なし → UI スレッド、`BACKGROUND` → UI スレッド外 | ✅ 一致 |

### maui-binding

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| Requirement 本文 (enum・`StartAsync` 10 本の最後・振り分け・Hostless) | enum: `maui/KsDialogs.Maui/Contract/LoadingActionThread.cs:15`<br>`maui/KsDialogs.Maui/Presentation/IKsLoading.cs` の 10 本 (`:165`〜`:359`。既定値 `Main` 10 か所)<br>引き回し: `maui/KsDialogs.Maui/Presentation/Loading.cs:133` `:158` `:187`、`maui/KsDialogs.Maui/Internals/LoadingGateway.cs` (`ILoadingGateway.RunAsync`)、`Platforms/{Android,iOS}/PlatformLoadingGateway.cs`<br>振り分け: `maui/KsDialogs.Maui/Internals/LoadingActionRunner.cs:76-77`<br>本番の口: `:100` (`MainThread.InvokeOnMainThreadAsync`) | (下の 6 本) | ✅ 一致 |
| [LD-HM-01] 正の compile 検査とオーバーロードの束縛 | 同上 | `maui/KsDialogs.Maui.ApiSurfaceCheck/LoadingActionThreadApiSurfaceChecks.cs:20-195`。10 本 × 3 通りの戻り値を `Task` / `Task<int>` の変数で受ける。テストプロジェクトが参照してビルドする | ✅ 一致 |
| [LD-HM-02] 既定では、UI スレッドで呼ぶ口を通って始まる | `LoadingActionRunner.cs:76` | `maui/KsDialogs.Maui.Tests/LoadingActionThreadTests.cs:25` (`DedicatedUiThread` の偽物) | ✅ 一致 |
| [LD-HM-03] `Background` では口を通らずに始まる | `LoadingActionRunner.cs:77` (`Task.Run`) | `LoadingActionThreadTests.cs:56` | ✅ 一致 |
| [LD-HM-04] どちらの経路でも、Runner の順序の保証が保たれる | `LoadingActionRunner.cs:63-91` (`DirectProgress` と `finally` の完了通知) | `LoadingActionThreadTests.cs:99` (Main / Background × 成功 / 失敗 の 4 ケース) | ✅ 一致 |
| [LD-HM-05] 10 本の入口から、指定が gateway まで届く | `Loading.cs` → `ILoadingGateway.RunAsync` | `LoadingActionThreadTests.cs:162` (指定なし → Main × 10)、`:185` (Background × 10) | ✅ 一致 |
| [LD-HM-06] UI スレッドを持たない環境では、その場で実行する | `maui/KsDialogs.Maui/Internals/HostlessLoadingGateway.cs:30` | `LoadingActionThreadTests.cs:225` (戻り値)、`:255` (失敗)。どちらも Main / Background | ✅ 一致 |

### kmp-facade

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| Requirement 本文 (enum・`start` 3 本・既定値・ObjC 書き出し・型指定の `@HiddenFromObjC`) | enum: `kmp/ksdialogs-kmp/src/commonMain/kotlin/jp/kamusoft/ksdialogs/kmp/LoadingActionThread.kt:12`<br>`start` 3 本: `kmp/ksdialogs-kmp/src/commonMain/kotlin/jp/kamusoft/ksdialogs/kmp/KsLoading.kt:125` `:148` `:180`。型指定の `@HiddenFromObjC` は `:175`<br>委譲: `LoadingGateway.kt:94` `:102` `:110`<br>ヘッダ: `evidence/kmp-objc-header-action-thread.txt` | `kmp/ksdialogs-kmp/src/iosTest/kotlin/jp/kamusoft/ksdialogs/kmp/ObjCApiSurfaceTests.kt:51` (ヘッダに `actionThread` と型が現れる)<br>`kmp/api-surface-check/.../LoadingApiSurfaceChecks.kt:104` | ✅ 一致 |
| [LD-HK-01] 3 本の入口から、指定が gateway まで届く | `LoadingGateway.kt:94` `:102` `:110` | `kmp/ksdialogs-kmp/src/commonTest/kotlin/jp/kamusoft/ksdialogs/kmp/LoadingActionThreadDelegationTests.kt:19` (指定なし → MAIN)、`:38` (BACKGROUND)。どちらも Test gateway | ✅ 一致 |
| [LD-HK-02] Android の gateway が、Native の型に写して渡す | `kmp/ksdialogs-kmp/src/androidMain/kotlin/jp/kamusoft/ksdialogs/kmp/AndroidLoadingGateway.kt:40` `:53` `:72` | `kmp/ksdialogs-kmp/src/androidHostTest/kotlin/jp/kamusoft/ksdialogs/kmp/AndroidLoadingGatewayContractTests.kt:170` | ✅ 一致 |
| [LD-HK-03] iOS の gateway は、既定で UI スレッドで始める | `kmp/ksdialogs-kmp/src/iosMain/kotlin/jp/kamusoft/ksdialogs/kmp/IosLoadingGateway.kt:136`、`:158` (`MAIN -> Dispatchers.Main.immediate`) | `kmp/ksdialogs-kmp/src/iosTest/kotlin/jp/kamusoft/ksdialogs/kmp/IosLoadingActionThreadTests.kt:46` (UI スレッド外から。Scenario の WHEN)。逆向きの `:87` もある | ✅ 一致 |
| [LD-HK-04] iOS の gateway は、`BACKGROUND` で UI スレッド外で始める | 同上 (`BACKGROUND -> Dispatchers.Default`) | `IosLoadingActionThreadTests.kt:62` (UI スレッドから。Scenario の WHEN)。逆向きの `:100` もある | ✅ 一致 |

### samples (Sample 通しの証跡で受け入れる。`scripts/scenario-id-coverage.py:118-119` に除外を登録済み)

| Scenario | 実装 | 証跡 | 状態 |
|---|---|---|---|
| Requirement: Default Loading の action の中から結果表示を更新する (文言は 4 ルートで同じ値) | action の最初の文で `結果: 処理中` を書く。明示的な UI スレッドへの移送は無い<br>- `samples/ios/KsDialogsSample/SampleMenuModel.swift:185`<br>- `samples/android/app/src/main/kotlin/jp/kamusoft/ksdialogs/samples/android/MainActivity.kt:191`<br>- `samples/maui/KsDialogs.Sample.Maui/SampleMenuPage.xaml.cs:269`<br>- `samples/kmp/shared/src/commonMain/kotlin/jp/kamusoft/ksdialogs/samples/kmp/SamplePresenter.kt:220`<br>`SampleText` の 4 ルートに同じ値 | (下の LD-HS-02) | ✅ 一致 |
| [LD-HS-02] 処理中に、結果表示が action の中から更新される | 同上 | 各ルートに `processing-start` / `processing-soon` / `completed` の 3 枚<br>- `evidence/sample-ios-default-loading-*.png`<br>- `evidence/sample-android-default-loading-*.png`<br>- `evidence/sample-kmp-ios-default-loading-*.png`<br>- `evidence/sample-kmp-android-default-loading-*.png`<br>- `evidence/sample-maui-ios-default-loading-*.png`<br>- `evidence/sample-maui-android-default-loading-*.png`<br>MAUI iOS は Debug 構成。配備物の確認は `evidence/maui-ios-deployed-binary-check.txt`、変更前に落ちる対照は `evidence/maui-ios-before-change-default-loading.log`。撮影手順は `evidence/sample-loading-capture-notes.txt` | ✅ 一致 (MAUI iOS の処理中の画像を開き、表示中に `結果: 処理中` が出ていることを確認) |
| Requirement: iOS Sample のスコープ形を新しい既定に合わせる | `SampleMenuModel.swift`: 「MainActor の外で動く」前提のコメント 2 か所と、値の先取り 2 か所を削除 (`runDefaultLoading` / `runCustomLoading:206`) | (下の LD-HS-01) | ✅ 一致 |
| [LD-HS-01] iOS の Default Loading / Custom Loading の通し | 同上 | `evidence/sample-ios-default-loading-*.png` (メッセージの差し替え「Soon...」を含む)<br>`evidence/sample-ios-custom-loading-progress-25.png` / `-progress-75.png` / `-completed.png` (75% の画像を開いて確認) | ✅ 一致 |

## 追加検査

- [x] **tasks.md の虚偽チェック**: 無い。1.1〜5.5・6.2・6.3 のチェックは、どれも上の対応表・証跡・lint の記録に実体がある。6.1 は未チェックで、実態と合っている
- [ ] **tasks.md の全件完了**: 6.1 が未完 (❌-1)
- [x] **逆流検査**: proposal / design / specs に差分は無い (`git diff HEAD` は空)。change ディレクトリ内の追跡の変更は tasks.md のチェックだけ
- [x] **未記録の乖離**: 無い。Scenario に当たらない差分は次のとおりで、どれも記録済み、または Requirement の範囲内
  - deviation.md の付随修正 2 件:
    - `maui/KsDialogs.Maui.ApiSurfaceCheck/NegativeChecks/RejectsOptionsArgumentOnLoadingShow.cs` の説明文
    - `scripts/scenario-id-coverage.py:140` の `("LD", "TH")`
  - Android の説明文 `android/api-surface-check/src/negativeCheckLoadingShowOptions/.../RejectsOptionsArgumentOnLoadingShow.kt` は tasks 2.6 の本務
  - `LoadingCoordinator` の列の作り直しと、`LoadingActionThreadTests.kt:195` の順序テストは、deviation.md の design Decision 3 の項
  - `android/ksdialogs-core/src/androidTest/kotlin/jp/kamusoft/ksdialogs/LoadingProgressTests.kt` の「受け口が失敗しても以後の報告と終了は受理される」テストは、同じ作り直しで入った受理ループの失敗の閉じ込めを固定する (review-001 の Suggestion への対応)。挙動は変更前 (報告ごとに別のコルーチン) の性質を保つもので、新しい契約ではない。deviation.md の項は閉じ込めに直接は触れておらず、`evidence/test-run-summary.md` にある (付記 1)
  - MAUI の iOS 互換面 `maui/macios/native/KsDialogsMauiBridge/MauiLoadingBridge.swift:151` `:160` の `@concurrent` は tasks 3.5 (design Risks)
  - KMP Sample の `SamplePresenter.autoPlay` / `runDefaultLoading` に `showInterimResult` 引数を足した変更は、共有 Presenter が画面を持たないことから来る LD-HS-02 の実現手段。デモ項目・デモ ID は増えていない
  - KMP iOS の gateway の `Dispatchers.Main.immediate` は、tasks 4.3 が iosTest で決めるとした選択肢の範囲内
- [x] **付随修正**: deviation.md の `[付随修正]` 2 件は対応表の対象外 (記録済み)
- [x] **UI 変更**: `ui/` は無い。Sample の変更は結果表示の文言の追加だけで、観察できる状態遷移は samples デルタの Scenario と証跡で見た
- [ ] **テストの全件成功**: 最終のコードで全件成功を確かめられないルートが残る (❌-1)

## テストの実行

| 実行 | 結果 |
|---|---|
| `python3 scripts/scenario-id-coverage.py` (この検証で実行) | 終了コード 0、未網羅なし。LD-TH 8/8・LD-HI 2/2・LD-HA 2/2・LD-HM 6/6・LD-HK 4/4・LD-HS 0/2 (除外 2)。警告はすべて archive 済み change のもの |
| `python3 scripts/scenario-id-coverage.py --require-mirror` (この検証で実行) | 終了コード 0 |
| `dotnet test maui/KsDialogs.Maui.Tests` (この検証で実行) | 172 / 172 成功 (ApiSurfaceCheck のビルドを含む)。`LoadingActionThread` で絞ると 12 / 12 成功 |
| ios / android / android instrumented / kmp / MAUI 互換面 2 種 | 実行していない (依頼の範囲外)。ホスト側の記録 `evidence/test-run-summary.md` を事実として受け取った。数え方は下の ❌-1 |

## ❌ の一覧と見立て

### ❌-1 完了確認 (tasks 6.1) が閉じておらず、最終のコードで Android instrumented の全件成功を確かめられない

- 事実 (`evidence/test-run-summary.md`)
  - 1 回目の全件実行の後、`LoadingCoordinator` / `Loading` の報告経路を 2 回直している (失敗 B の修正と、review-001 の閉じ込め)
  - 修正後に回し直したのは Android の unit (68)・`:ksdialogs-core` の Loading 系 10 クラス (111)・`:ksdialogs` (40)・MAUI の Android 互換面 (34) だけで、`:ksdialogs-core` の全 302 件は回し直していない
  - 1 回目の全件実行では、範囲外と判断された失敗が残っている
    - A: API 36 実機の UiAutomation 接続。5 件。変更前のコードでも同じ例外で落ちる
    - C: API 33 実機の `ToastMultiDisplayTests.TS_MX_05`。1 件。テスト補助の競合で、回し直しでは成功
  - C の `TS_MX_05` は Loading を出すテストだが、Loading 系 10 クラスの回し直しには含まれていない
- 見立て: **実装の修正は不要。完了確認を閉じる側の作業** (deviation に記録する類いではない)
  - 修正後のコードで `:ksdialogs-core` の instrumented 全件を 1 回回す。A が出ない端末 (API 33 実機など) を使えば、A の切り分けと混ざらない
  - 残った失敗が A・C と一致することを test-run-summary.md に記録し、6.1 をチェックする
  - A・C をこの change の完了条件から外すかは、呼び出し元とオーナーが決める (handbook cross/test-execution.md の完了判定)。外すと決め、全件の再実行を省く場合も、その判断を記録すれば VALID の条件を満たす見込み

## 付記 (判定に影響しない)

1. deviation.md の design Decision 3 の項は、列の作り直しと LD-TH-07 の固定を記録している。受理ループの失敗の閉じ込め (1 件の受け口の失敗が以後の報告と区切りを止めない) には触れていない。蒸留で「なぜ報告は列と区切りで、1 件ずつ子コルーチンで受理するのか」を拾うなら、この項に 1 文足すと辿りやすい
2. ios-native デルタの例示「isolation の指定が無い async 関数は UI スレッド外で始まる」は、利用者のモジュールで `NonisolatedNonsendingByDefault` を有効にしていない場合に限って成り立つ (review-001 の Suggestion と同じ)。LD-HI-02 のテストは、この前提をテスト側のコメント (`LoadingActionThreadTests.swift:262-266`) に書いたうえで、デルタの書き方どおりに検証している。一致の判定には影響しない
