# 検証: add-maui-nuget-distribution (verify-001)

- 対象: HEAD (`c844740`) に対する未コミットの作業ツリー全体
- デルタスペック: `specs/maui-binding/spec.md` (Requirement 3 / Scenario 8)、`specs/maui-nuget-distribution/spec.md` (Requirement 7 / Scenario 15) — 合計 **Requirement 10 / Scenario 23**
- 合意済み差分: `deviation.md` (2 件)
- **判定: INVALID** (❌ 1 件 — 実装の欠落ではなく、証跡に記録した Android 互換面のテスト件数が実測と食い違う)

---

## 1. 対応表: specs/maui-binding/spec.md

### Requirement: View 生成失敗の構成ミスとしての報告

実装の主体: `maui/KsDialogs.Maui/Contract/DialogException.cs:59` (`ViewCreationFailed`。`ViewTypeName` / `ViewModelTypeName` / InnerException を公開)、`maui/KsDialogs.Maui/Hosting/KsDialogsServiceCollectionExtensions.cs:213` (`CreateView` が `ActivatorUtilities` の生成だけを try で包み、`catch (DialogException)` は素通し、それ以外を `ViewCreationFailed` に包む)。`DialogServiceProvider.Require()` は try の外にあるため `ServiceProviderUnavailable` が先に立つ既存経路は不変。

| Scenario | 実装 | テスト / 証跡 | 状態 |
|---|---|---|---|
| [MB-MA-11] 依存を解決できない View は ViewCreationFailed で失敗する | `maui/KsDialogs.Maui/Hosting/KsDialogsServiceCollectionExtensions.cs:228`、1 行登録 3 経路の呼び出し側 (同 `:123` / `:162` / `:187`) | `maui/KsDialogs.Maui.Tests/DialogDependencyInjectionTests.cs:109` (Dialog)、`maui/KsDialogs.Maui.Tests/LoadingDependencyInjectionTests.cs:92` (Loading)。両プロパティ・InnerException の型と内容・`gateway.CreatedViews` が空であることまで assert | ✅ 一致 |
| [MB-MA-12] Toast の 1 行登録の生成失敗は警告に残して 1 枚だけ破棄する | 同上 (`RegisterForToast` 側は `BridgeContentSupply.CreateOrDiscard` の既存経路へ合流) | `maui/KsDialogs.Maui.Tests/ToastDependencyInjectionTests.cs:76`。警告 1 件・`ViewCreationFailed` と元の依存名が警告に含まれること・破棄 1 枚・後続 Toast の表示を assert | ✅ 一致 |
| [MB-MA-13] 利用者コードの例外は包まれない | `maui/KsDialogs.Maui/Hosting/KsDialogsServiceCollectionExtensions.cs:213` (包むのは 1 行登録の `CreateView` のみ。`Register` の factory と `UseViewFallback` の resolver は経路が別) | `maui/KsDialogs.Maui.Tests/DialogDependencyInjectionTests.cs:142`。factory / resolver の独自例外がそのまま届くことと、resolver が null を返した場合の `ViewFactoryNotRegistered` を assert | ✅ 一致 |
| 公開例外面の compile 検査 | 同上 (`ViewCreationFailed` と両プロパティが public) | `maui/KsDialogs.Maui.ApiSurfaceCheck/DialogExceptionApiSurfaceChecks.cs` (新規)。`KsDialogs.Maui.Tests` が ProjectReference で巻き込むため `dotnet test` のビルドが検査を兼ねる (`maui/KsDialogs.Maui.Tests/KsDialogs.Maui.Tests.csproj:20`)。本検証で再実行しビルド成功を確認 | ✅ 一致 |
| [MB-MA-14] Android の実機経路でも同じ型が届く (修正前後の A/B) | 上記 + `maui/KsDialogs.Maui/Platforms/Android/PlatformDialogGateway.cs:64,95` | 自動テストなし (`scripts/scenario-id-coverage.py` の `DEFAULT_ALLOW_MISSING` に登録済み — `scripts/scenario-id-coverage.py:114`)。証跡 `evidence/view-creation-failure/` の 3 枚。本検証で 3 枚を開いて内容を確認 — `01-android-before.png` は `type=System.InvalidOperationException` / `inner=(none)` / `view=(none)` / `vm=(none)`、`02-android-after.png` と `03-ios-after.png` は `type=KsDialogs.DialogException+ViewCreationFailed` / `inner=System.InvalidOperationException` / `view=…ModelDialogCardView` / `vm=…ModelDialogViewModel`。README の記述と一致 | ✅ 一致 |

英語メッセージの固定は `maui/KsDialogs.Maui.Tests/DiagnosticMessageTests.cs:20` (`DM_MA_01`) に `ViewCreationFailed` の文言・両プロパティ・InnerException の同一性を追加して担保。

### Requirement: Android の managed/native 境界での失敗の受け止め

実装の主体: managed 側 `maui/KsDialogs.Maui/Platforms/Android/PlatformDialogGateway.cs:64,95` と `.../PlatformLoadingGateway.cs:130,186` (`BridgeContentSupply.CreateOrFail` + `BridgeContentFailure` で失敗を値に変えて預かり、`OnFailed` / `OnFailure` で預かった元例外を優先して投げ直す。`HideAsync` は供給が無いため預かり口 null)。互換面 (Kotlin) 側 `maui/android/native/.../MauiDialogContentProvider.kt:171` と `.../MauiLoadingContent.kt:13` で戻り値を nullable 化し、`.../MauiDialogBridge.kt:120` / `.../MauiLoadingBridge.kt:247` で null を `error(...)` により既存の失敗経路へ合流。

| Scenario | 実装 | テスト / 証跡 | 状態 |
|---|---|---|---|
| [MB-MA-15] Kotlin 互換面は中身なしを失敗経路へ合流させる | `maui/android/native/ksdialogs-maui-bridge/src/main/kotlin/jp/kamusoft/ksdialogs/maui/MauiDialogBridge.kt:120`、`.../MauiLoadingBridge.kt:247` | `maui/android/native/ksdialogs-maui-bridge/src/test/kotlin/jp/kamusoft/ksdialogs/maui/MauiDialogLoadingContentSupplyTests.kt:59,82` (新規)。`reportClosure` / `reportLoadingCompletion` 経由で通知がちょうど 1 回・`failed:` / `failure:` になること、呼び出し自体が例外で落ちないこと (= 非漏出) を assert | ✅ 一致 |
| [MB-MA-16] 既存の失敗経路は変わらない | `PresentationHostUnavailable` / `Cancelled` の経路に変更なし | `maui/KsDialogs.Maui.Tests/DialogDependencyInjectionTests.cs:169` | ✅ 一致 |

補足: MB-MA-15 のテストを書くために `MauiDialogViewModel` / `MauiLoadingViewModel` を `private` → `internal` に変えている (`MauiDialogBridge.kt:106` / `MauiLoadingBridge.kt:233`)。モジュール内可視性の変更であり、互換面の public API は変わらない。

### Requirement: Android binding の生成出力

| Scenario | 実装 | テスト / 証跡 | 状態 |
|---|---|---|---|
| BG8401 の不在と static メンバーの可用性 | `maui/android/KsDialogs.Binding.Android/Transforms/Metadata.xml:30-42` (4 型の `.Companion` を `attr visibility=private` + 同名 static フィールドを `remove-node` のハイブリッド) | 証跡 `evidence/binding-warnings/README.md`。本検証で `dotnet build -c Release --no-incremental` を再実行 — **BG8401 は 0 件** (警告 30 件の内訳は BG8605 40 / BG8606 4 / BG8A00 16 の出現行)。facade の `net10.0-android` は `dotnet test` のビルドで成功し、`MauiDialogBridge.Shared` 等の参照が解決 | ⚠️ deviation 記録済み |

`deviation.md` 1 件目 (design Decision 5 の「両方を remove-node」→ ハイブリッド、BG8A00 の受容) に合致。spec の受け入れ条件 (BG8401 の不在・facade のビルド成功) は満たしている。

---

## 2. 対応表: specs/maui-nuget-distribution/spec.md

### Requirement: パッケージの共通メタデータと版の宣言元

実装の主体: `maui/Directory.Build.props` (新規。Authors / Company / Copyright / MIT / URL 2 種 / icon / `Version` 既定 `0.0.0-dev` / `IsPackable` 既定 false / SourceLink / `SymbolPackageFormat=snupkg`)、`maui/Directory.Build.targets` (新規。同梱 targets の import、icon の同梱、自 assembly 用 aar の検査と除去)。

| Scenario | 実装 | テスト / 証跡 | 状態 |
|---|---|---|---|
| nuspec のメタデータと Version の既定・注入 | `maui/Directory.Build.props`、`maui/KsDialogs.Maui/KsDialogs.Maui.csproj:17-21` (`Version` 直書きを削除し pack メタデータへ置換) | 証跡 `evidence/pack-verification/README.md`。本検証で facade を `-p:Version=0.1.0-alpha.1` で pack し直し、nuspec に authors `kamusoft` / license expression MIT / icon / projectUrl / repository / `<readme>README.md</readme>` が入り `.snupkg` が併産されることを確認。binding (Android) の nuspec に readme が無いことも確認 | ✅ 一致 |
| pack 対象の限定 | `maui/Directory.Build.props` の `IsPackable` 既定 false | 証跡 `evidence/pack-verification/README.md`。本検証で `-getProperty:IsPackable` を再取得し `KsDialogs.Maui.Tests` / `KsDialogs.Maui.ApiSurfaceCheck` とも `false` | ✅ 一致 |
| MAUI 本体の版の引き下げ後のビルドとテスト | `maui/Directory.Packages.props:17` (10.0.70 → 10.0.20)、`samples/maui/KsDialogs.Sample.Maui/KsDialogs.Sample.Maui.csproj:24` (`MauiVersion` 10.0.20) | 本検証で `maui/KsDialogs.Maui/obj/project.assets.json` (net10.0 / net10.0-android / net10.0-ios) と `samples/maui/KsDialogs.Sample.Maui/obj/project.assets.json` (全 target) の解決版がすべて `Microsoft.Maui.Controls/10.0.20` であることを確認。`cd maui && dotnet test` を再実行し 160 件成功 / 失敗 0。Sample の両 OS ビルドは `evidence/sample-walkthrough/README.md` の観測環境節 | ✅ 一致 |

### Requirement: 3 パッケージの構成と内容

実装の主体: `maui/KsDialogs.Maui/KsDialogs.Maui.csproj:17-21`、`maui/android/KsDialogs.Binding.Android/KsDialogs.Binding.Android.csproj:24-25`、`maui/macios/KsDialogs.Binding.iOS/KsDialogs.Binding.iOS.csproj:24-25`、`maui/Directory.Build.targets` の `KsCheckGeneratedAarEntries` / `KsExcludeGeneratedAarFromPackage`。

| Scenario | 実装 | テスト / 証跡 | 状態 |
|---|---|---|---|
| 3 パッケージのローカル pack | 上記 csproj 3 件 | 証跡 `evidence/pack-verification/README.md`。本検証で facade を pack し直し、nuspec の TFM group が `net10.0` = `Microsoft.Maui.Controls 10.0.20` のみ、`net10.0-android36.0` / `net10.0-ios26.0` にそれぞれ binding を同一 version (`0.1.0-alpha.1`) で持つことを確認。同梱物は nuspec / README.md / buildTransitive 2 件 / icon.png / lib の dll 3 件 (+ android の xml) のみで、`.aar` / `.xcframework` / `KsDialogs.Binding.*.dll` / `KsDialogs.Maui.aar` は無し | ⚠️ deviation 記録済み |
| binding パッケージの同梱物と説明 | 上記 binding 2 件の csproj | 証跡 `evidence/pack-verification/README.md`。本検証で Android binding を pack し直し、`lib/net10.0-android36.0/` に Gradle 由来の `ksdialogs-core-release.aar` / `ksdialogs-maui-bridge-release.aar` の 2 本だけ (自 assembly 用 aar なし)、依存は `Xamarin.Kotlin.StdLib 2.4.0.1` / `Xamarin.KotlinX.Coroutines.Android 1.11.0.1`、description に "do not reference this package directly" を確認。iOS binding の xcframework 両スライスと facade description の "AiForms.Maui.Dialogs" は証跡の記録で受け入れ (facade description は本検証の nuspec 実測でも確認) | ⚠️ deviation 記録済み |

`deviation.md` 2 件目 (自 assembly 用 aar が KsDialogs では生成されず、除去後処理は `Exists` 不成立で走らない。spec の「nupkg に含まれない」は同梱物の実測で満たす) に合致。

### Requirement: 消費者からの導入

| Scenario | 実装 | テスト / 証跡 | 状態 |
|---|---|---|---|
| ローカルフィードからの restore と Release ビルド | 上記の pack 設定一式 | 自動テストなし (一時消費者プロジェクトでの実測)。証跡 `evidence/consumer-verification/README.md` の 1.〜2. — restore 警告 0 件 (`NU1605` / `NU1608` / `NU1107` を `WarningsAsErrors`)、`Microsoft.Maui.Controls` 全 TFM で 10.0.20、binding 2 件が facade と同 version で推移解決、`.nupkg.metadata` の source が 3 件ともローカルフィード、Android (trimming + R8 + AOT) / iOS Release ビルド成功と成果物中の facade / binding アセンブリ、`XA4301` 0 件 | ✅ 一致 |
| パッケージ経由の起動確認 | 同上 | 証跡 `evidence/consumer-verification/` の png 6 枚。本検証で `android-consumer-dialog-shown.png` (覆いの上に白いカード。"Delivered through the NuGet package." と Cancel / OK) と `ios-consumer-home-result-completed.png` (`Result: completed (True)`) を開いて README の記述と一致することを確認 | ✅ 一致 |

### Requirement: 最低 OS 版のビルド時ガード

実装の主体: `maui/KsDialogs.Maui/buildTransitive/KsDialogs.Maui.props` (定数 `KsDialogsMinAndroidApi=24` / `KsDialogsMinIOSVersion=17.0`)、`maui/KsDialogs.Maui/buildTransitive/KsDialogs.Maui.targets:15-17` (`BeforeTargets="CoreCompile"`、`TargetFramework` 非空 かつ platform が android / ios のときだけ動く Condition。比較は `VersionLessThan`)、facade csproj の `None Include="buildTransitive/..."` による同梱 (`maui/KsDialogs.Maui/KsDialogs.Maui.csproj:28-29`)、`maui/Directory.Build.props` からの import でリポジトリ内にも同じ資産を適用。

| Scenario | 実装 | テスト / 証跡 | 状態 |
|---|---|---|---|
| 要件未満の利用者アプリ | `maui/KsDialogs.Maui/buildTransitive/KsDialogs.Maui.targets:26` | 証跡 `evidence/consumer-verification/README.md` の 4-a / 4-b / 4-c。android=21 / ios=15.0 で `KSDLG0001` の文面つきエラー、診断の出所が利用者側に展開された `buildTransitive/KsDialogs.Maui.targets`、`-p:KsDialogsMinAndroidApi=1` の対照実験でガードを黙らせると `XAAMM0000` が出る (= ガードが manifest merger より先)、未設定では Android が SDK 既定 21.0 で同じエラー / iOS は SDK 既定 26.5 で通過 | ✅ 一致 |
| 要件を満たす利用者アプリとリポジトリ内のビルド | 同上 | 証跡 `evidence/pack-verification/README.md` 末尾 (facade 3 TFM / binding 2 件 / Sample 両 OS のビルドログに `KSDLG` の出現なし) と `evidence/consumer-verification/README.md` の 2.。本検証の `dotnet test` / binding Release ビルド / facade pack のログにも `KSDLG` は出ていない | ✅ 一致 |
| 非 platform TFM と outer build ではガードが動かない | `.../KsDialogs.Maui.targets:17` の Condition | 証跡 `evidence/consumer-verification/README.md` の 5-a / 5-b。`net10.0` クラスライブラリ・outer build・`net10.0` inner build で診断 0 件、`net10.0-android` の inner build でのみ `KSDLG0001` | ✅ 一致 |
| 要件の宣言元の一致 | `maui/KsDialogs.Maui/KsDialogs.Maui.csproj:34,38`、`maui/android/KsDialogs.Binding.Android/KsDialogs.Binding.Android.csproj:14`、`maui/macios/KsDialogs.Binding.iOS/KsDialogs.Binding.iOS.csproj:13` (すべて `$(KsDialogsMinAndroidApi)` / `$(KsDialogsMinIOSVersion)` 参照) | 証跡 `evidence/pack-verification/README.md` の評価値表 (Android 24.0 / iOS 17.0)。本検証で `git grep SupportedOSPlatformVersion -- 'maui/**'` を実行し、`maui/` 配下の 4 か所すべてが定数参照で数値の直書きが無いことを確認 | ✅ 一致 |

### Requirement: package README の表示

| Scenario | 実装 | テスト / 証跡 | 状態 |
|---|---|---|---|
| README の同梱とリンク参照 | `maui/KsDialogs.Maui/KsDialogs.Maui.csproj:21,26` (`PackageReadmeFile` + ルート README の `None Include`)、`README.md` / `README_ja.md` の参照を絶対 URL 化 | 証跡 `evidence/readme-links/README.md` (改訂前 各 12 件 → 改訂後 0 件、一意 20 URL がすべて 200、nupkg ルートの README.md)。本検証で抽出スクリプトを再実行し **両 README とも総参照 19 / 非 HTTP(S) 0 件**、pack し直した nupkg のルートに `README.md` があり nuspec の `<readme>` が指していることを確認 | ✅ 一致 |

### Requirement: MAUI Sample の両 OS 通しと MAUI テストルートの完了判定

| Scenario | 実装 | テスト / 証跡 | 状態 |
|---|---|---|---|
| 両 OS の全デモ項目 | Sample のソース改変なし (状態は起動引数で作る) | 証跡 `evidence/sample-walkthrough/README.md` + `ios/` 25 枚 / `android/` 25 枚。本検証で枚数 (各 25) と md5 重複 0 件を確認し、`android/and-07-layout-dialog-result.png` を開いて README の記述 (属性調整パネルの既定値 + 「結果: cancelled」への変化) と一致することを確認。安定デモ ID 14 件 × 両 OS が項目別に対応づけられており、`transition-dialog` / `layout-dialog` の 1 回表示と Dialog 系の閉鎖後の結果変化も記録されている | ✅ 一致 |
| 3 テストルートの全件実行 | — | 証跡 `evidence/test-routes/README.md` + `test-routes.txt`。**Android 互換面の記録件数が実測と食い違う (下記 3.)** | ❌ 乖離 |

### Requirement: MAUI 本体の版の追随ルール

| Scenario | 実装 | テスト / 証跡 | 状態 |
|---|---|---|---|
| 規範文書の記載 | `kasane/handbook/cross/local-development-setup.md:77-78` | 本検証で本文を確認 — 「workload set を上げるときは props と Sample の `MauiVersion` を同梱版に合わせる」ルールと、同梱版の確かめ方 (`$(MauiVersion)` の既定値を `-getProperty:MauiVersion` で評価、2026-09-08 実測 10.0.20) が書かれている | ✅ 一致 |

---

## 3. ❌ の詳細

### ❌-1: Android 互換面のテスト件数の記録が実測と食い違う

- **Scenario**: 「3 テストルートの全件実行」(`specs/maui-nuget-distribution/spec.md:107`)
- **記録**: `evidence/test-routes/README.md` と `evidence/test-routes/test-routes.txt` が `tests=33`、内訳を「変更前 31 件 + 本変更で追加した MB-MA-15 の 2 件 = 33。期待値どおり」と書いている
- **実測 (本検証)**: `cd maui/android/native && ./gradlew :ksdialogs-maui-bridge:test --rerun-tasks` を再実行し、`ksdialogs-maui-bridge/build/test-results/testDebugUnitTest/*.xml` を集計すると **tests=34 / failures=0 / errors=0 / skipped=0**
- **食い違いの中身**: 本変更は Kotlin 側にテストを 3 件追加している — MB-MA-15 の 2 件 (`MauiDialogLoadingContentSupplyTests.kt:59,82`) に加え、`MauiBridgeDiagnosticMessageTests.kt:33` に `DM-MA-04` の 2 件目 (Dialog / Loading の中身なしの文言) がある。証跡の内訳はこの 1 件を数えていない。クラス別の実測は `MauiBridgeDiagnosticMessageTests` 2 / `MauiDialogClosureReportTests` 6 / `MauiDialogLayoutPassthroughTests` 4 / `MauiDialogLoadingContentSupplyTests` 2 / `MauiDialogTransitionRunnerTests` 5 / `MauiLoadingCompletionReportTests` 4 / `MauiLoadingPassthroughTests` 6 / `MauiToastContentSupplyTests` 1 / `MauiToastPassthroughTests` 4 = 34
- **影響範囲**: 実装・テストの中身に欠落はない (失敗 0・全件成功は再現した)。壊れているのは記録された件数と内訳だけ
- **見立て**: **証跡の修正**で解消する種類 (実装を直す話でも deviation として合意する話でもない)。`evidence/test-routes/README.md` の表と内訳、`evidence/test-routes/test-routes.txt` の「JUnit XML 集計」行を 34 に改め、内訳に `DM-MA-04` の 1 件を足す。ハンドブック `cross/test-execution.md` の実測表 (maui/ 155 件・maui/android/native 31 件、実測日 2026-09-07) も現状 155/31 のままなので、蒸留時に 160/34 へ更新するのが自然

なお `DM-MA-04` の追加テスト自体は乖離としない — `DM-MA-04` は archive 済み変更 (`kasane/changes/archive/2026-09-07-localize-dialog-error-messages/specs/maui-binding/spec.md:49`) の既存 Scenario であり、本変更が新設した `error("The MAUI side could not create the presentation content.")` の 2 か所を既存 Requirement の網羅に追随させたもの。ただし件数の内訳には数える必要がある。

---

## 4. 追加検査

| 検査 | 結果 |
|---|---|
| tasks.md の全タスク完了 | 全 26 タスクが `[x]`。対応表と突き合わせて**虚偽チェックなし** (1.1〜1.7 / 2.1〜2.5 / 3.1〜3.4 / 4.1 / 5.1〜5.6 / 6.1〜6.3 のすべてに実装またはツリー内の実物・証跡が対応する)。`assets/icon.png` は 300×300 PNG で実在 |
| 逆流検査 (足場の書き換え) | **なし**。`proposal.md` / `design.md` / `specs/` は `1762a7f` (propose 段階) 以降 commit も未コミット変更も無し。`git status` で change 配下の変更は `tasks.md` のみ。その diff もチェックボックスの `[ ]` → `[x]` だけで本文の書き換えは 0 行 |
| 未記録乖離 | ❌-1 のみ (実装の乖離ではなく証跡の記録誤り)。`deviation.md` の 2 件は対応表で ⚠️ として突き合わせ済み |
| 付随修正 | `deviation.md` に `[付随修正]` 行なし。diff で Scenario に直接対応しない変更は (a) `MauiDialogViewModel` / `MauiLoadingViewModel` の `private` → `internal` (MB-MA-15 のテスト可視性)、(b) `DM_MA_01` の 7 型化 (`ViewCreationFailed` の文言固定)、(c) `DM-MA-04` テストの 2 件目 — いずれも本変更の Requirement / 既存 Requirement の網羅であり、スコープ外の修正ではない |
| UI 変更 | 本変更に `ui/` は無い (パッケージング + 例外面。Sample の UI は無改変) — 対象外 |
| テスト全件実行 | **本検証で再実行**: facade `cd maui && dotnet test` → 発見 160 / 成功 160 / 失敗 0。Android 互換面 `./gradlew :ksdialogs-maui-bridge:test --rerun-tasks` → 34 / 失敗 0。iOS 互換面は Swift 側に本変更の差分が無いため証跡 `evidence/test-routes/test-routes.txt` の記録 (7 tests / 4 suites、失敗 0) で受け入れ |
| Scenario ID 網羅 | `python3 scripts/scenario-id-coverage.py` → 「結果: 未網羅なし」(exit 0)。`MB-MA-14` が理由付きで `DEFAULT_ALLOW_MISSING` に入っている (`scripts/scenario-id-coverage.py:114`)。ID を持たない Scenario 2 件 (「公開例外面の compile 検査」「BG8401 の不在と static メンバーの可用性」) は警告として出るが、両者とも ID 体系を持たない検証 (compile / ビルド警告) であり本変更の設計どおり |

## 5. 注記 (判定には数えないが蒸留で拾うとよい点)

- `evidence/pack-verification/README.md` の pack 対象限定の節に「`-getProperty:IsPackable` はいずれも `false` (`maui/Directory.Build.props` の既定値のまま)」とあるが、実際は `maui/KsDialogs.Maui.Tests/KsDialogs.Maui.Tests.csproj:9` と `maui/KsDialogs.Maui.ApiSurfaceCheck/KsDialogs.Maui.ApiSurfaceCheck.csproj:18` が明示的に `false` を持っている。Scenario の THEN (nupkg を生成しない) は満たすため乖離にはしないが、括弧の説明は事実と異なる
- `kasane/handbook/cross/test-execution.md` の実測表が maui/ 155 件・maui/android/native 31 件のまま (実測日 2026-09-07)。同文書は「テスト構成が育って実態が変わったら本規約を実測で更新する」と定めており、本変更で 160 / 34 になった
