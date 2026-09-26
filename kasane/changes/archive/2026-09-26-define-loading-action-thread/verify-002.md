# 一致検証: define-loading-action-thread (002 回目)

**日付**: 2026-09-25
**判定**: VALID (verify-001 の ❌-1 は解消。24 Scenario は ✅ 23・⚠️ deviation 記録済み 1 のまま、❌ 0)

## サマリー

- 今回確かめたのは次の 2 点
  - verify-001 の ❌-1 (tasks 6.1 が閉じておらず、最終のコードで全件成功を確かめられない) が解消したか
  - verify-001 の後に変わったもの (deviation.md・evidence/test-run-summary.md・review-002.md・tasks.md) が、対応表の判定を変えないか
- ❌-1 は解消した
  - `evidence/test-run-summary.md` の「最終コードでの全件実行」節に、最終コードでの instrumented 全件の結果が記録されている。`:ksdialogs-core` 304 件中 303 成功・失敗 0・skip 1、`:ksdialogs` 40 件中 40 成功
  - 端末に残った結果 XML が、この記録と一致することを確かめた (下の「❌-1 の解消の確認」)
  - tasks.md の 6.1 はチェック済みで、実態と合っている
- 実装コードは verify-001 の時点から変わっていない
  - `git diff HEAD --stat` の追跡差分 (42 ファイル) と未追跡の新規ファイルの顔ぶれは、verify-001 の検証範囲と同じ
  - change ディレクトリの外の差分ファイルは、最後の更新が `LoadingCoordinator.kt` / `LoadingProgressTests.kt` の修正 (review-001 への対応)。これは verify-001 の出力より前で、それ以降に更新されたのは change ディレクトリ内の記録だけ
  - 対応表の行番号は抜き取りで確かめ、ずれは無かった
- verify-001 の後の記録の変化は、対応表の判定を変えない (下の「verify-001 以降の変化の影響」)

## ❌-1 の解消の確認

| 確認したこと | 結果 |
|---|---|
| tasks.md の 6.1 | チェック済み。tasks.md の HEAD からの差分はチェックの変化だけで、未チェックは 0 件 |
| 最終コードでの `:ksdialogs-core` instrumented 全件 | 端末に残った結果 XML (`android/ksdialogs-core/build/outputs/androidTest-results/connected/debug/`)<br>- 実行時刻は、実装コードの最後の更新より後<br>- `tests="304" failures="0" errors="0" skipped="1"`、終了コード 0<br>- `LoadingActionThreadTests` 9 件・`LoadingProgressTests` 8 件を含む<br>- review-001 で足した `進捗の受け口が失敗しても以後の報告と終了は受理され次の開始が戻る` も含む<br>- 失敗 C で落ちていた `ToastMultiDisplayTests.TS_MX_05` は成功<br>記録 (API 33 実機 1 台) と一致する |
| 最終コードでの `:ksdialogs` instrumented 全件 | 結果 XML は `tests="40" failures="0" errors="0" skipped="0"`。記録と一致 |
| Android の修正を巻き込むルートの回し直し | 結果 XML (どれも実装コードの最後の更新より後) が記録の件数と一致<br>- android/ unit: 68 件・失敗 0<br>- kmp/: 166 件・失敗 0<br>- maui/android/native/: 34 件・失敗 0 |
| 回し直していないルート (ios/・maui/ の `dotnet test`・maui/macios/native/) | 報告経路の修正は Android の Kotlin だけで、これらのルートのソースに触れていない<br>- ios/・maui/ の差分ファイルは、どれも Android の修正より前に更新されたまま<br>- maui/ の `dotnet test` は、verify-001 で同じコードに対して 172 / 172 成功を確かめている<br>- ios/ (286)・maui/macios/native/ (7) は、ホスト側の記録を事実として受け取った |
| 残っていた失敗 A・C の扱い | 最終コードの全件 (API 33) に失敗は無い<br>- A (API 36 実機の UiAutomation 接続、5 件) は最終コードの全件に含まれていない端末の問題。記録は「端末側の問題と見られる」とし、変更前のコードとの比較 (API 36 実機 1 台、ログは証跡に無い) の範囲も正直に書いている。完了条件から外す判断は、6.1 の根拠として記録済み<br>- C は今回の全件で成功した |

## verify-001 以降の変化の影響

| 変わったもの | 内容 | 判定への影響 |
|---|---|---|
| deviation.md (design Decision 3 の項) | 受理を 1 件ずつ `scope` (`SupervisorJob`) 直下の子として起動し、受け口の失敗が列と区切りを止めないこと、失敗は握りつぶさず未捕捉例外ハンドラへ出すこと、固定するテストとして `LoadingProgressTests` の閉じ込めのテストが追記された | 無し。LD-TH-07 は引き続き「⚠️ deviation 記録済み」。verify-001 の付記 1 で「deviation.md の項が閉じ込めに触れていない」とした点が解消し、`LoadingProgressTests.kt:300` の追加テストが deviation 側から辿れるようになった |
| evidence/test-run-summary.md | 次の 3 点が変わった<br>- 「最終コードでの全件実行」節を追加<br>- 失敗 A に変更前のコードとの比較を追記<br>- skip 1 件の説明を訂正 (IME ではなく `DialogSystemBarsTests.PB_SB_04`) | ❌-1 の解消 (上記)。Scenario の対応は変わらない |
| review-002.md | APPROVED。Suggestion 2 件 (失敗 A の裏づけ・deviation の閉じ込めの記述) は、追記で解消を確認済み | 無し。実装コードと足場に差分が無いことも、review-002 の補足と一致する |
| tasks.md | 6.1 のチェック | ❌-1 の解消 |

## 対応表

実装コードが verify-001 の時点から変わっていないため、実装・テストの位置は verify-001 の対応表と同じ。各行の詳細 (入口ごとの行番号) は verify-001 を参照。

### loading-contract

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| [LD-TH-01] 既定で、UI スレッドから呼んでも UI スレッドで始まる | iOS `ios/Sources/KsDialogs/Presentation/Loading.swift:230` (`runScope`、action の型が `@MainActor`)<br>Android `android/ksdialogs-core/src/main/kotlin/jp/kamusoft/ksdialogs/Loading.kt:172` `:180` | `ios/Tests/KsDialogsTests/LoadingActionThreadTests.swift:26`<br>`android/ksdialogs-core/src/androidTest/kotlin/jp/kamusoft/ksdialogs/LoadingActionThreadTests.kt:54` | ✅ 一致 |
| [LD-TH-02] 既定で、UI スレッド外から呼んでも UI スレッドで始まる | 同上 | iOS `:40` / Android `:69` | ✅ 一致 |
| [LD-TH-03] UI スレッド外の指定で、UI スレッドから呼んでも UI スレッド外で始まる | iOS 利用者の `@concurrent`<br>Android `Loading.kt:172` (`BACKGROUND -> Dispatchers.Default`) | iOS `:61` / Android `:84` | ✅ 一致 |
| [LD-TH-04] UI スレッド外の指定で、UI スレッド外から呼んでも UI スレッド外で始まる | 同上 | iOS `:75` / Android `:102` | ✅ 一致 |
| [LD-TH-05] カスタム View の入口でも、既定は UI スレッドで始まる | iOS `Loading.swift` の各入口 → `runScope`<br>Android `Loading.kt` の `start` 4 本 → `runScope` | iOS `:98` / Android `:121` | ✅ 一致 |
| [LD-TH-06] カスタム View の入口でも、UI スレッド外の指定が効く | 同上 | iOS `:118` / Android `:137` | ✅ 一致 |
| [LD-TH-07] UI スレッド外で始まった action からの進捗報告が届く | iOS `LoadingReportQueue`<br>Android `android/ksdialogs-core/src/main/kotlin/jp/kamusoft/ksdialogs/LoadingCoordinator.kt:279` `:290` `:317`、`Loading.kt:201` | iOS `:155`<br>Android `:155` (40 回反復)・`:195` (既定の指定の順序)<br>閉じ込め: `android/ksdialogs-core/src/androidTest/kotlin/jp/kamusoft/ksdialogs/LoadingProgressTests.kt:300` | ⚠️ deviation 記録済み (design Decision 3 の項。閉じ込めも今回の追記で記録済み)。挙動は Scenario どおり |
| [LD-TH-08] どちらの指定でも、失敗は伝播して表示が閉じる | 両 `runScope` の catch → `endUseAfterPendingReports` | iOS `:183` / Android `:227` | ✅ 一致 |

### ios-native

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| Requirement 本文 (13 入口の action の型・`@concurrent`・doc コメント) | `ios/Sources/KsDialogs/Presentation/KsLoading.swift` (protocol 5 本・extension 8 本・doc コメント)、`Loading.swift` の実装 5 本 | (下の 2 本) | ✅ 一致 |
| [LD-HI-01] 公開面の正の compile 検査 | 同上 | `ios/Tests/KsDialogsTests/LoadingApiSurfaceCompileChecks.swift:248` `:292` `:335` | ✅ 一致 |
| [LD-HI-02] isolation の指定が無い関数を名前で渡すと UI スレッド外で始まる | 言語規則 | `LoadingActionThreadTests.swift:207` `:267` | ✅ 一致 |

### android-native

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| Requirement 本文 (enum・`start` 4 本・`startCompose`・既定値 `MAIN`) | `android/ksdialogs-core/src/main/kotlin/jp/kamusoft/ksdialogs/LoadingActionThread.kt:10`、`KsLoading.kt:140` `:159` `:180` `:204`、`android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/compose/ComposeLoadingShow.kt:50` | (下の 2 本) | ✅ 一致 |
| [LD-HA-01] 公開面の正の compile 検査 | 同上 | `android/api-surface-check/src/main/kotlin/jp/kamusoft/ksdialogs/apicheck/LoadingApiSurfaceChecks.kt:199` | ✅ 一致 |
| [LD-HA-02] Compose の `startCompose` でも指定が効く | `ComposeLoadingShow.kt:50` | `android/ksdialogs/src/androidTest/kotlin/jp/kamusoft/ksdialogs/compose/ComposeLoadingActionThreadTests.kt:37` (最終コードの `:ksdialogs` 全件 40 件に含まれて成功) | ✅ 一致 |

### maui-binding

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| Requirement 本文 (enum・`StartAsync` 10 本・振り分け・Hostless) | `maui/KsDialogs.Maui/Contract/LoadingActionThread.cs:15`、`maui/KsDialogs.Maui/Presentation/IKsLoading.cs`、`maui/KsDialogs.Maui/Presentation/Loading.cs`、`maui/KsDialogs.Maui/Internals/LoadingGateway.cs`、`maui/KsDialogs.Maui/Internals/LoadingActionRunner.cs:76-77` `:100` | (下の 6 本) | ✅ 一致 |
| [LD-HM-01] 正の compile 検査とオーバーロードの束縛 | 同上 | `maui/KsDialogs.Maui.ApiSurfaceCheck/LoadingActionThreadApiSurfaceChecks.cs:20-195` | ✅ 一致 |
| [LD-HM-02] 既定では、UI スレッドで呼ぶ口を通って始まる | `LoadingActionRunner.cs:76` | `maui/KsDialogs.Maui.Tests/LoadingActionThreadTests.cs:25` | ✅ 一致 |
| [LD-HM-03] `Background` では口を通らずに始まる | `LoadingActionRunner.cs:77` | `LoadingActionThreadTests.cs:56` | ✅ 一致 |
| [LD-HM-04] どちらの経路でも、Runner の順序の保証が保たれる | `LoadingActionRunner.cs:63-91` | `LoadingActionThreadTests.cs:99` | ✅ 一致 |
| [LD-HM-05] 10 本の入口から、指定が gateway まで届く | `Loading.cs` → `ILoadingGateway.RunAsync` | `LoadingActionThreadTests.cs:162` `:185` | ✅ 一致 |
| [LD-HM-06] UI スレッドを持たない環境では、その場で実行する | `maui/KsDialogs.Maui/Internals/HostlessLoadingGateway.cs:30` | `LoadingActionThreadTests.cs:225` `:255` | ✅ 一致 |

### kmp-facade

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| Requirement 本文 (enum・`start` 3 本・既定値・ObjC 書き出し・`@HiddenFromObjC`) | `kmp/ksdialogs-kmp/src/commonMain/kotlin/jp/kamusoft/ksdialogs/kmp/LoadingActionThread.kt:12`、`KsLoading.kt:125` `:148` `:175` `:180`、`LoadingGateway.kt:94` `:102` `:110`、`evidence/kmp-objc-header-action-thread.txt` | `kmp/ksdialogs-kmp/src/iosTest/kotlin/jp/kamusoft/ksdialogs/kmp/ObjCApiSurfaceTests.kt:51`、`kmp/api-surface-check/src/commonMain/kotlin/jp/kamusoft/ksdialogs/kmp/apicheck/LoadingApiSurfaceChecks.kt:104` | ✅ 一致 |
| [LD-HK-01] 3 本の入口から、指定が gateway まで届く | `LoadingGateway.kt:94` `:102` `:110` | `kmp/ksdialogs-kmp/src/commonTest/kotlin/jp/kamusoft/ksdialogs/kmp/LoadingActionThreadDelegationTests.kt:19` `:38` | ✅ 一致 |
| [LD-HK-02] Android の gateway が、Native の型に写して渡す | `kmp/ksdialogs-kmp/src/androidMain/kotlin/jp/kamusoft/ksdialogs/kmp/AndroidLoadingGateway.kt:40` `:53` `:72` | `kmp/ksdialogs-kmp/src/androidHostTest/kotlin/jp/kamusoft/ksdialogs/kmp/AndroidLoadingGatewayContractTests.kt:170` | ✅ 一致 |
| [LD-HK-03] iOS の gateway は、既定で UI スレッドで始める | `kmp/ksdialogs-kmp/src/iosMain/kotlin/jp/kamusoft/ksdialogs/kmp/IosLoadingGateway.kt:136` `:158` | `kmp/ksdialogs-kmp/src/iosTest/kotlin/jp/kamusoft/ksdialogs/kmp/IosLoadingActionThreadTests.kt:46` (`:87`) | ✅ 一致 |
| [LD-HK-04] iOS の gateway は、`BACKGROUND` で UI スレッド外で始める | 同上 | `IosLoadingActionThreadTests.kt:62` (`:100`) | ✅ 一致 |

### samples (Sample 通しの証跡で受け入れる。`scripts/scenario-id-coverage.py` に除外を登録済み)

| Scenario | 実装 | 証跡 | 状態 |
|---|---|---|---|
| Requirement: Default Loading の action の中から結果表示を更新する | `samples/ios/KsDialogsSample/SampleMenuModel.swift:185`、`samples/android/app/src/main/kotlin/jp/kamusoft/ksdialogs/samples/android/MainActivity.kt:191`、`samples/maui/KsDialogs.Sample.Maui/SampleMenuPage.xaml.cs:269`、`samples/kmp/shared/src/commonMain/kotlin/jp/kamusoft/ksdialogs/samples/kmp/SamplePresenter.kt:220`、4 ルートの `SampleText` | (下の LD-HS-02) | ✅ 一致 |
| [LD-HS-02] 処理中に、結果表示が action の中から更新される | 同上 | `evidence/sample-{ios,android,kmp-ios,kmp-android,maui-ios,maui-android}-default-loading-*.png`、`evidence/maui-ios-deployed-binary-check.txt`、`evidence/maui-ios-before-change-default-loading.log`、`evidence/sample-loading-capture-notes.txt` | ✅ 一致 |
| Requirement: iOS Sample のスコープ形を新しい既定に合わせる | `SampleMenuModel.swift` (前提コメントと値の先取りの削除) | (下の LD-HS-01) | ✅ 一致 |
| [LD-HS-01] iOS の Default Loading / Custom Loading の通し | 同上 | `evidence/sample-ios-default-loading-*.png`、`evidence/sample-ios-custom-loading-*.png` | ✅ 一致 |

## 追加検査

- [x] **tasks.md の全件完了**: 1.1〜6.3 がすべてチェック済み (未チェック 0)
- [x] **tasks.md の虚偽チェック**: 無い。6.1 のチェックは、上の結果 XML と test-run-summary.md の記録に実体がある。それ以外のチェックは verify-001 で確認済みで、その後の変化はチェック以外に無い
- [x] **逆流検査**: proposal / design / specs に差分は無い (`git diff HEAD` で空)。change ディレクトリ内で追跡されているファイルの変更は tasks.md のチェックだけ
- [x] **未記録の乖離**: 無い。verify-001 で「deviation.md が直接は触れていない」とした受理ループの失敗の閉じ込め (`LoadingProgressTests` の追加テスト) は、今回の追記で deviation.md の Decision 3 の項に記録された
- [x] **付随修正**: deviation.md の `[付随修正]` 2 件は変わっておらず、対応表の対象外 (記録済み)
- [x] **UI 変更**: `ui/` は無い。Sample の観察できる変化は samples デルタの Scenario と証跡で見た
- [x] **テストの全件成功**: 最終コードの全ルートで失敗 0 (上の「❌-1 の解消の確認」)

## テストの実行

| 実行 | 結果 |
|---|---|
| `python3 scripts/scenario-id-coverage.py` (この検証で実行) | 終了コード 0、未網羅なし。LD-TH 8/8・LD-HI 2/2・LD-HA 2/2・LD-HM 6/6・LD-HK 4/4・LD-HS 0/2 (除外 2) |
| `python3 scripts/scenario-id-coverage.py --require-mirror` (この検証で実行) | 終了コード 0。「対象領域の ID はすべて iOS / Android の双方にあります」。LD-TH 8/8 |
| ビルド・端末テスト | 依頼どおり再実行していない。最終コードの結果は、端末と Gradle が残した結果 XML で件数と失敗 0 を照合した (android instrumented 2 モジュール・android unit・kmp・maui/android/native)。ios/・maui/macios/native/ はホスト側の記録を事実として受け取った。maui/ の `dotnet test` は verify-001 で同じコードに対して実行済み (172 / 172) |

## ❌ の一覧と見立て

無し。

## 付記 (判定に影響しない)

1. 最終コードの Android instrumented の全件は、API 33 実機 1 台で回している。handbook cross/test-execution.md の「API レベルで走る / 走らない Scenario」は、対象の API レベルを 1 台ずつ回して初めて全件と言える、としている。この change では API 29 の端末をつないでおらず、API 30 以上で skip される `DialogSystemBarsTests.PB_SB_04_旧経路でも非表示状態が維持される` の 1 本は、どの回でも実行されていない
   - このテストはダイアログのシステムバーの旧経路 (`systemUiVisibility`) を見るもので、Loading には触れない (テストのファイルに Loading の参照は無い)
   - この change の差分 (Loading の action のスレッドと報告経路) の影響を受けないため、❌ とはしない
   - 記録 (test-run-summary.md の 6.1 節) も、API 29 を回していないことを書いている
2. 失敗 A (API 36 実機の 5 件) を完了条件から外した根拠は、変更前のコードとの比較が API 36 実機 1 台で、ログが証跡に無い (実装ワーカーの報告による記録) ことを含めて、test-run-summary.md に正直に書かれている。review-002 の追記もこの書きぶりで解消としている。蒸留の際にこの端末の問題を handbook 側 (test-execution の既知事項) へ移すかは、呼び出し元の判断
