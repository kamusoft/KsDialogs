# 一致検証結果: add-layout-spec (002 回目)

**日付**: 2026-08-19
**判定**: **VALID**
**検証対象**: commit `5af1a1b` 以降の working tree 全変更 (未コミット・追跡外を含む)
**デルタスペック**: `specs/dialog-contract/spec.md` (7 Requirement / 16 Scenario)・`specs/ios-native/spec.md` (3/4)・`specs/android-native/spec.md` (4/5)・`specs/maui-binding/spec.md` (2/3)・`specs/kmp-facade/spec.md` (1/3)・`specs/samples/spec.md` (2/4) — **計 19 Requirement / 35 Scenario**
**合意済み差分**: `deviation.md` の2件 (パネル画面の戻る導線 A 案 / パネル内結果表示) は違反として扱わない

> 注: 本変更に `verify-001.md` は存在しない (初版実装は commit `5af1a1b` に取り込まれたまま単独の verify 文書を持たない)。本書が本変更で最初の一致検証記録であり、番号は依頼どおり `002` を用いる。

---

## 1. 対応表

凡例: ✅ 一致 / ⚠️ deviation 記録済み / ❌ 欠落・乖離

### 1.1 dialog-contract

| Requirement / Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| **R: メタ属性セットと既定値** | iOS `ios/Sources/KsDialogs/Contract/DialogOptions.swift:13-50` / `DialogPlacement.swift:9-32`、Android `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/DialogOptions.kt:24-31` / `DialogPlacement.kt:18-23`、MAUI `maui/KsDialogs.Maui/Internals/DialogOptions.cs:24-52` (internal) / `maui/KsDialogs.Maui/Contract/DialogPlacement.cs:16-28`、KMP `kmp/ksdialogs-kmp/src/commonMain/kotlin/jp/kamusoft/ksdialogs/kmp/DialogPlacement.kt:19-24` (placement のみ公開) | 下記 2 Scenario | ✅ |
| ├ S: 既存コードの互換性 | 属性は全フィールド既定値付きの追加。VM 契約は空 (`ios/.../Contract/DialogViewModel.swift:11-13`、`android/.../DialogViewModel.kt:16`) | iOS `DialogLayoutAttributeDefaultsTests.swift:35-53` (C19 実測) / `DialogAttributeCompileChecks.swift:37-39`、Android instrumented `DialogLayoutAttributeDefaultsTests.kt:49` / unit `DialogAttributeDefaultsTests.kt:38`、API 形状 4ルート正の検査 (`acceptsShowWithoutPlacement`)、負の検査 `*vmAttribute` (VM に属性を生やせない) | ✅ |
| ├ S: 無効値の正規化 | iOS `Layout/DialogLayout.swift:23-63` (比率 `:37-40` / offset `:43-45` / margin `:48-60`)、Android `DialogLayout.kt:49-69` | iOS `DialogAttributeSupplyTests.swift:242-261`、Android unit `DialogAttributeDefaultsTests.kt:65,75,85` + instrumented `DialogAttributeSupplyTests.kt:117`、MAUI 無変換確認 `DialogLayoutPassthroughTests.cs:104` / `MauiDialogLayoutPassthroughTests.kt:75` | ✅ |
| **R: 属性の供給と優先順位** | 合成点: iOS `Presentation/DialogContainerViewController.swift:183-188`、Android `DialogLayoutSnapshot.kt:61-64`、MAUI `Internals/DialogGateway.cs:94-96` | 下記 5 Scenario | ✅ |
| ├ S: 添付だけで供給される | iOS `Contract/UIViewDialogAttributes.swift:10-41`、Android `ViewDialogAttributes.kt:12-28` (+ `res/values/ids.xml:4-5`)、MAUI `Presentation/DialogAttachedProperties.cs:28-96,222-241` | iOS `DialogAttributeSupplyTests.swift:21-39`、Android `DialogAttributeSupplyTests.kt:43,59`、MAUI `DialogLayoutPassthroughTests.cs:25,46` | ✅ |
| ├ S: show 引数の placement が添付に勝つ | iOS `DialogContainerViewController.swift:185`、Android `DialogLayoutSnapshot.kt:63`、MAUI `DialogGateway.cs:95` (`showPlacement ?? AttachedPlacement`) | iOS `DialogAttributeSupplyTests.swift:70-86`、Android `DialogAttributeSupplyTests.kt:72`、MAUI `DialogLayoutPassthroughTests.cs:121` | ✅ |
| ├ S: show placement はオブジェクト単位で置換する | 同上 (`??` によるオブジェクト置換。フィールド合成なし) | iOS `DialogAttributeSupplyTests.swift:88-109`、Android `DialogAttributeSupplyTests.kt:90`、MAUI `DialogLayoutPassthroughTests.cs:140` | ✅ |
| ├ S: 初回レイアウト完了後の添付変更は反映されない | iOS `DialogContainerViewController.swift:13-17,137-165,170-180` (`didMoveToWindow` 起点・収束上限4・`isLayoutSnapshotFrozen`)、Android `DialogLayoutSnapshot.kt:16-72` + `DialogLayoutHost.kt:53-92,149`、MAUI `Internals/DialogAttributeSnapshotRelay.cs:20-48` + `Platforms/*/PlatformDialogGateway.cs` | iOS `DialogAttributeSupplyTests.swift:111-145,147-202,204-240`、Android `DialogAttributeSupplyTests.kt:143,182`、MAUI `DialogLayoutPassthroughTests.cs:166,188` / `DialogAttributeSnapshotRelayTests.cs:23,44,76`。実環境: `verification/maui-snapshot-wiring/notes.md` | ✅ |
| ├ S: 何も供給しなければ既定値 (C19) | 既定インスタンスへのフォールバック (上記合成点) | iOS `DialogLayoutAttributeDefaultsTests.swift:27-33,35-53`、Android instrumented `DialogLayoutAttributeDefaultsTests.kt:28,49` / unit `DialogAttributeDefaultsTests.kt:49`、MAUI `DialogLayoutPassthroughTests.cs:85` / `MauiDialogLayoutPassthroughTests.kt:69` | ✅ |
| **R: 軸別レイアウト規則** | iOS `Layout/DialogLayoutResolver.swift:56-101`、Android `DialogLayoutResolver.kt:27-100` + `DialogAxisLayout.kt:11,27-34` + クランプ適用 `DialogLayoutHost.kt:133-141` | 下記 4 Scenario (すべてケース表全量検証に内包) | ✅ |
| ├ S: 比率と Fill の競合 (C05) | 比率基準 = R (控除前)、Fill 基準 = A、Fill 敗北軸は Center — iOS `:71-85`、Android `:81-100` | iOS `DialogLayoutCaseTableTests.swift:17-41` (C05)、Android `DialogLayoutCaseTableTests.kt:30` (C05) | ✅ |
| ├ S: 非対称 Margin の中心 (C09) | 辺ごとの控除 — iOS `:65-66`、Android `:73-74` | 同上 (C09) | ✅ |
| ├ S: Offset の座標系は一定・領域外を許容 (C07) | anchor 各枝で `+ offset`、クランプなし — iOS `:86-94`、Android `DialogAxisLayout.kt:27-34` | 同上 (C07) | ✅ |
| ├ S: visibleArea 基準は両軸に効く (C11) | iOS `DialogLayoutResolver.swift:18-24`、Android `DialogLayoutResolver.kt:27-30` | 同上 (C11) + Android `DialogPresentedWindowTests.kt:39,59,79`。実環境: `verification/sample-walkthrough/notes.md` の visibleArea トグル実測 (4ルート) | ✅ |
| **R: 共通ケース表への適合** | 正の配置 `core/layout-spec/cases.json` — 凍結版 `specs/dialog-contract/layout-cases.json` と**バイト等価 (正規化 JSON 差分 0 行 / 19 ケース / tolerance 1.0)**。`approvedDiff` エントリ 0 件。参照は写しを作らず直参照 (Android `android/ksdialogs/build.gradle.kts:34` の asset ディレクトリ、iOS `Support/DialogLayoutCaseLoader.swift:18-25` のリポジトリルート解決) | iOS `DialogLayoutCaseTableTests.swift:17-41` (19 件パラメタライズ・実 frame)、Android `DialogLayoutCaseTableTests.kt:30,42` (19 × 2 = 38 件・実 View) | ✅ |
| └ S: 同一ケースが両 Native 実装で同じ期待 rect | 両実装が同一 JSON を読み、期待値分岐なし | 上記 (iOS 19 / Android 38 いずれも green) | ✅ |
| **R: 提示前サイズ確定** | iOS `DialogContainerViewController.swift:126-130` (`prepareForPresentation`) ← `DialogPresenter.swift:34`、Android `DialogContentHolder.kt:30-57` + `DialogLayoutHost.kt:98` | — | ✅ |
| └ S: 初期状態で伸びた内容が初期表示に反映される | 同上 | iOS `DialogPresentationSizingTests.swift:10-22,24-50`、Android `DialogPresentationSizingTests.kt:27,39` | ✅ |
| **R: 透明オーバーレイはシステムバーの見えを変えない** | Android `DialogWindowSystemBars.kt:34-53,62-74` + `DialogContainer.kt:67-79` (背景透明・`FLAG_DIM_BEHIND` クリア・`MATCH_PARENT`)。提示構成の切り替え分岐なし | — | ✅ |
| └ S: 透明オーバーレイ表示中のステータスバー | 同上 | Android instrumented `DialogTransparentOverlayTests.kt:33,48,65` (輝度実測 + 検出力の陰性対照)。実環境証跡: `verification/android-transparent-overlay/notes.md` (Pixel 6a、既定 −97.55 / 透明 ±0.00) | ✅ |
| **R: 外側タップキャンセル** | iOS `DialogContainerViewController.swift:219-239`、Android `DialogContainer.kt:57-64,88-93,130-131` (`setCanceledOnTouchOutside(false)` で OS 既定を使わず自前処理)。背後非透過: Host の clickable 消費 + 全画面 Window + `DialogContentHolder.kt:26` | 下記 2 Scenario | ✅ |
| ├ S: 既定では外側タップでキャンセルされる | 同上 (既定 true) | iOS `DialogOutsideTapTests.swift:17,33,48,69,127`、Android `DialogOutsideTapTests.kt:47,61,75` (実タッチ注入)。実環境: `verification/sample-walkthrough/notes.md` 確認3 (4ルート) | ✅ |
| └ S: false なら外側タップは無反応でモーダル性を保つ | 同上 (早期 return + タッチ消費) | iOS `DialogOutsideTapTests.swift:98-125`、Android `DialogOutsideTapTests.kt:95` (`backgroundTaps == 0` を検査) | ✅ |

### 1.2 ios-native

| Requirement / Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| **R: メタ属性の供給機構 (iOS)** | `Contract/DialogOptions.swift` / `DialogPlacement.swift` / `UIViewDialogAttributes.swift:10-41`、show 引数 `Presentation/KsDialogs.swift:17-29` → `Dialog.swift:23-32` → `DialogPresenter.swift:10-32`。VM 契約は属性なし | — | ✅ |
| └ S: View 添付と show 引数の合成 | `DialogContainerViewController.swift:183-188` | `DialogAttributeSupplyTests.swift:41-68`。API 形状: `DialogAttributeCompileChecks.swift:42-54` (正) / 負の検査 `KSDIALOGS_NEGATIVE_CHECK_VM_ATTRIBUTE`・`..._SHOW_OPTIONS` | ✅ |
| **R: レイアウト規則の実装とケース表全量適合 (iOS)** | `Layout/DialogLayoutResolver.swift:56-101` + AutoLayout 反映 `DialogContainerViewController.swift:241-341` | — | ✅ |
| ├ S: ケース表の全量検証が通る (実 frame) | 実測ハーネス `Tests/.../Support/DialogLayoutMeasurement.swift:18-65` (実 window 搭載 → `layoutIfNeeded` → `contentView.frame`)。rect 計算関数の単体検証ではない | `DialogLayoutCaseTableTests.swift:10-41` (19 ケース green) | ✅ |
| └ S: 提示前サイズ確定 (iOS) | `DialogContainerViewController.swift:126-130` | `DialogPresentationSizingTests.swift:10-22,24-50` | ✅ |
| **R: 外側タップキャンセル (iOS)** | `DialogContainerViewController.swift:219-239` | `DialogOutsideTapTests.swift` 6 件 | ✅ |
| └ S: 外側タップの結果経路 | 同上 (`resultChannel` へ cancelled) | `DialogOutsideTapTests.swift:17-31` | ✅ |

### 1.3 android-native

| Requirement / Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| **R: メタ属性の供給機構 (Android)** | `DialogOptions.kt` / `DialogPlacement.kt` / `ViewDialogAttributes.kt:12-28` (+ `res/values/ids.xml`)、show 引数 `KsDialogs.kt:23-26` → `Dialog.kt:17-31` → `DialogPresenter.kt:21-43` → `DialogPresentationSurface.kt:27-31` → `ActivityDialogPresentationSurface.kt:19-24` → `DialogContainer.kt:34`。VM 契約は属性なし | — | ✅ |
| └ S: View 添付と show 引数の合成 | `DialogLayoutSnapshot.kt:61-64` | instrumented `DialogAttributeSupplyTests.kt:219` (実 show 経由) / `:43`。API 形状: `android/api-surface-check/src/main/.../DialogApiSurfaceChecks.kt:29-64` (正) / 負の検査 4 フラグ | ✅ |
| **R: レイアウト規則の実装とケース表全量適合 (Android)** | `DialogLayoutResolver.kt:27-100` + `DialogAxisLayout.kt` + `DialogLayoutHost.kt:98-141` | — | ✅ |
| ├ S: ケース表の全量検証が通る (実 View) | 実測ハーネス `androidTest/.../support/DialogLayoutMeasurement.kt:66-139` (実 `DialogLayoutHost` に screen を与え、insets はケース入力で注入、`OnGlobalLayoutListener` 後に `contentHolder` の実 rect を dp 換算) | instrumented `DialogLayoutCaseTableTests.kt:30,42` (19 × 2 = 38 件 green)。ローダーは `approvedDiff` の `reason`/`approvedBy` 欠落を読込時に AssertionError にする (`DialogLayoutCaseLoader.kt:80-92`) | ✅ |
| └ S: 提示前サイズ確定 (Android) | `DialogContentHolder.kt:30-57` | `DialogPresentationSizingTests.kt:27,39` | ✅ |
| **R: 透明オーバーレイの正式対応 (Android)** | `DialogWindowSystemBars.kt:27-91` + `DialogContainer.kt:46-49,67-79`。提示構成切り替えハック (原典方式) なし | — | ✅ |
| └ S: 透明時も配置規則が保たれる | 覆いの色は Host 背景として適用 (`DialogLayoutHost.kt:99-102`)。色による配置分岐なし | `DialogLayoutCaseTableTests.kt:42` (19 ケースを透明覆いで再走査し同一 rect) + `DialogTransparentOverlayTests.kt:33,48,65`。実環境: `verification/android-transparent-overlay/notes.md` | ✅ |
| **R: 外側タップキャンセル (Android)** | `DialogContainer.kt:57-64,88-93,130-131` | `DialogOutsideTapTests.kt` 4 件 | ✅ |
| └ S: 外側タップの結果経路 | 同上 | `DialogOutsideTapTests.kt:47` (suspend show が cancelled で返る) | ✅ |

### 1.4 maui-binding

| Requirement / Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| **R: メタ属性の供給とパススルー (MAUI)** | 添付プロパティ 10 個 `Presentation/DialogAttachedProperties.cs:28-96` (Get/Set `:102-216`)、2 オブジェクトへの束ね `:222-241`、Show 引数 `Presentation/IKsDialogs.cs:37-39` → `Dialog.cs:45-53` → `Internals/DialogPresenter.cs:24-41` → `Internals/DialogGateway.cs:37-96`。options 引数は非提供。旧 `IDialogLayoutProviding` / `DialogLayoutAttributes` / `MauiDialogLayoutAttributes.{kt,swift}` は**コード上 0 件** (残存は kasane ドキュメントの言及のみ) | — | ✅ |
| ├ S: 添付プロパティがネイティブへ届く | 無変換写像 (色は ARGB 32bit int `Internals/DialogOptions.cs:51`): Android `Platforms/Android/PlatformDialogGateway.cs:55-97`、iOS `Platforms/iOS/PlatformDialogGateway.cs:98-140`。互換面 Kotlin `maui/android/native/.../MauiDialogAttributes.kt`、Swift `maui/macios/native/KsDialogsMauiBridge/MauiDialogAttributes.swift` | C# 側委譲面まで: `DialogLayoutPassthroughTests.cs:25,46,68,85,104`。Kotlin 互換面 → Native: `MauiDialogLayoutPassthroughTests.kt:22,49,69,75`。C#↔Native 境界の実環境証跡: `verification/maui-hit-test/notes.md:73-102` (`current-05-attribute-transport-ios.png` / `current-06-attribute-transport-android.png`) と `verification/maui-snapshot-wiring/notes.md` | ✅ |
| └ S: Show の placement 引数が添付に勝つ | `Internals/DialogGateway.cs:94-96` | `DialogLayoutPassthroughTests.cs:121,140`。API 形状: `maui/KsDialogs.Maui.ApiSurfaceCheck/DialogApiSurfaceChecks.cs:24-79` (正) / 負の検査 5 フラグ | ✅ |
| **R: 当たり領域は描画領域と一致する (MAUI)** | **実装変更なし** — 原因究明の結果「不具合非実在」(phase-4 記録は描画中心座標の取り違え)。`verification/maui-hit-test/notes.md:41-54,104-107` | — | ✅ |
| └ S: 描画中心のタップが反応する | 同上 | 実環境証跡: `baseline-0{1..4}` (適用前再現) / `current-0{1..4}` (適用後) / `current-0{7..9}` (新経路での再確認・tasks 7.3)。描画中心 (261.3, 471.8) で completed、陰性対照 (261,500) は無反応 | ✅ |

### 1.5 kmp-facade

| Requirement / Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| **R: 共有コードからの placement 指定 (KMP)** | `commonMain/.../DialogPlacement.kt:19-24` (public data class) + `DialogAlignment.kt:9-25`。`DialogOptions` は KMP 公開面に**存在しない** (`kmp/ksdialogs-kmp/src/**` で 0 件、負の検査ソースのみが参照)。show 引数 `KsDialogs.kt:29-32` (`placement: DialogPlacement? = null`) → `DialogGateway.kt:21-24,50-54` | — | ✅ |
| ├ S: 共有コードの placement が両 OS のネイティブへ届く | Android actual `AndroidDialogGateway.kt:27,42-57` (Native 型へ 1:1)、iOS actual `IosDialogGateway.kt:44,79-95` → `ios/Sources/KsDialogs/Interop/KsDialogsInteropPlacement.swift:6-73` → `KsDialogsInteropBridge.swift:59-75` | commonTest `DialogPlacementSupplyTests.kt:20,48`、androidHostTest `AndroidDialogGatewayContractTests.kt:72,97`、iosTest `InteropPlacementTransportTests.kt:28,53,70`、Swift 側 `KsDialogsInteropBridgeTests.swift:161-198` | ✅ |
| ├ S: placement 省略時は添付と既定値がそのまま効く | `null` を null のまま Native へ (safe call)。添付は各 OS の View 定義側 | `DialogPlacementSupplyTests.kt:36,61`、`AndroidDialogGatewayContractTests.kt:114`、`InteropPlacementTransportTests.kt:43` | ✅ |
| └ S: 既存の共有 VM の互換性 | VM を包み直さずそのまま委譲 (`AndroidDialogGateway.kt:27`) | `AndroidDialogGatewayContractTests.kt:53,63,126,135,159`、`InteropBridgeContractTests.kt` 6 件、`DialogResultRouteTests` / `FakeDialogsSubstitutionTests` / `DialogEntryPointTests` (無改変で green)。API 形状: `kmp/api-surface-check/src/commonMain/.../DialogApiSurfaceChecks.kt:21-33` (正) / 負の検査 3 フラグ | ✅ |

### 1.6 samples

| Requirement / Scenario | 実装 (4ルート) | 検証 | 状態 |
|---|---|---|---|
| **R: レイアウトデモ項目 (属性調整パネル)** | ios `SampleLayoutPanelScreen.swift:12-55` / android `SampleLayoutPanelView.kt` / kmp `SampleLayoutPanelScreen.swift`・`SampleLayoutPanelView.kt` / maui `SampleLayoutPanelPage.xaml(.cs)`。調整項目は水平・垂直配置 (Start/Center/End)・OffsetX/Y・LayoutArea トグルの5種。配置と Offset は **show の placement 引数**、LayoutArea は **factory 内の View 添付 (options)** で実現 (ios `SampleDialogRegistration.swift:25-27` / android `SampleDialogRegistration.kt:33-39` / kmp `SampleDialogRegistration.{swift,kt}` / maui `SampleDialogRegistration.cs:27-29`) — tasks 6.2 の指定どおり | `verification/sample-walkthrough/notes.md` 確認1 (4ルート実機・Simulator 実測)、`ui/verification/*.png` (4ルート × 6状態)、文言の正 `concepts/cross/conventions/sample-parity.md` 更新済み (tasks 6.4) | ✅ |
| ├ S: 属性を変えて表示すると反映される | 上記 + 結果表示は既存デモと同形式 | `*-03-dialog-end-end.png` (4ルートとも右下寄せ)、`*-05/06` (visibleArea トグルの画素実測: iOS 系 62.0pt / Android 50.3dp)、`*-07` (Offset 実測) | ✅ |
| └ S: パネルの初期値は属性の既定値 | 配置 Center (ios `SampleLayoutPanelModel.swift:11-12` / android `SampleAlignmentSegmentsView.kt:16` / kmp 同 / maui `SampleAlignmentSegmentsView.xaml.cs:17`)、Offset 0、visibleArea ON | `*-01/02-panel-initial.png` (4ルートとも Center/Center/0/0/ON。契約既定値と一致) | ✅ |
| **R: 結果表示エリアの表示条件** | メニュー側の初期非表示: ios `SampleMenuModel.swift:11`+`SampleMenuScreen.swift:20-22` / android `SampleMenuView.kt:107` / kmp 同 / maui `SampleMenuPage.xaml:56-57`。**パネル側にも結果表示を追加** (deviation.md 2件目) | `verification/sample-walkthrough/notes.md` 確認2 | ⚠️ deviation 記録済み |
| ├ S: 初期状態では結果エリアが無い | 上記 (GONE / nil / IsVisible=False) | `*-01-menu-initial.png`・`*-02-panel-initial.png` (4ルート一致) | ✅ |
| └ S: 結果確定後に表示される | メニュー側の Scenario は従来どおり維持 (パネルからの伝播: ios `SampleLayoutPanelScreen.swift:57-61` / android `MainActivity.kt:77-78` / maui `SampleLayoutPanelPage.xaml.cs:246-248`) | `*-08-menu-result-completed.png` (4ルート一致) | ✅ |
| (参考) パネル画面の戻る導線 `‹` | ios `SampleLayoutPanelHeader.swift:12-22` / android `SampleLayoutPanelView.kt:101-115`+`MainActivity.kt:36-42,63` / kmp 同 / maui `SampleLayoutPanelPage.xaml:15-26`。読み上げ名「戻る」+ ボタン役割を4ルートに付与 (`ui/brief.md`) | `verification/sample-walkthrough/notes.md`、`ui/brief.md` 照合結果 | ⚠️ deviation 記録済み (A 案) |

---

## 2. 追加検査

### 2.1 tasks.md の虚偽チェック

`git diff 5af1a1b -- tasks.md` は**チェックボックスの状態変化のみ**で、タスク本文の書き換えはない。全 16 タスクが `[x]`。対応表と突き合わせた結果、**未実装なのにチェック済みのタスクは 0 件**:

| タスク | 対応表での裏付け |
|---|---|
| 1.1 layout-semantics.md 改訂 | `concepts/core/api/layout-semantics.md` 全面改訂済み (2 値オブジェクト・供給と優先順位・スナップショット規則・「比率 > Fill > 内容」・外側タップ節を確認。廃止属性の記述なし。「まだ決めていないこと」は `:205-207` でクランプ時の見え方のみ) |
| 1.2 ケース表の再配置 | `core/layout-spec/cases.json` が凍結版と**正規化 JSON 差分 0 行**。期待値の変更なし = deviation 不要 |
| 2.1〜2.4 / 3.1〜3.4 | §1.2 / §1.3 の全行 ✅ |
| 4.1 / 4.2 | §1.4 の全行 ✅ (4.2 は初版完了・7.3 でリグレッション再確認済み) |
| 5.1 | §1.5 の全行 ✅ |
| 6.1〜6.4 | §1.6 + `ui/brief.md` 照合結果節 + `sample-parity.md` の Layout Dialog 追記 |
| 7.1 | §2.4 で全ルート再実行し全件 green を確認 |
| 7.2 | `verification/android-transparent-overlay/` (notes + 4 枚) |
| 7.3 | `verification/sample-walkthrough/` (notes + 4ルート × 8 枚) |

### 2.2 逆流検査 (足場の凍結)

```
git diff --stat 5af1a1b -- kasane/changes/add-layout-spec/proposal.md \
                            kasane/changes/add-layout-spec/design.md \
                            kasane/changes/add-layout-spec/specs/
→ 出力なし
```

**proposal / design / デルタスペック (layout-cases.json を含む) は実装期間中に一切書き換えられていない。逆流なし。**
変更されている change 配下の文書は `tasks.md` (チェックのみ)・`ui/brief.md` (実装時合意事項と照合結果の追記 = 記録用途)・`verification/maui-hit-test/notes.md` (再確認の追記) のみで、いずれも契約の書き換えではない。

### 2.3 UI 変更の検査

- 承認モックの記録: `ui/brief.md:41-43` に `approved.png` (2026-08-17 承認) と `approved-layout-panel.png` (2026-08-17 承認) の記載あり
- 合意済み妥協の記録: `ui/brief.md` 「実装時の合意事項 (モックとの差分)」節に 6 件 (OS 標準スイッチ・カード幅 240・MAUI のキーボード種別・パネル内結果の地色・タップ領域・戻る導線の読み上げ属性)。オーナー指示由来の 2 件は `deviation.md` が正と明記
- 視覚照合: `ui/brief.md` 「照合結果」節に 4ルート × 6状態 + KMP Android 追撮 + MAUI Android 追撮 + 読み上げ属性追加後の再撮影の記録。証跡 `ui/verification/` 32 枚
- 残件 (仕様不一致ではない): 同節に「**オーナーによる最終承認は未取得**」と明記されている。UI の最終承認ゲートは本検証の範囲外

### 2.4 テスト実行 (本検証で実測)

`concepts/cross/conventions/test-execution.md` (2026-08-19 更新版) に書かれたコマンドをそのまま実行:

| ルート | コマンド | 実測 | 規約の記載 |
|---|---|---|---|
| ios/ | `xcodebuild test -scheme KsDialogs -destination 'platform=iOS Simulator,name=iPhone 17 Pro'` | **59 tests / 14 suites passed**, `** TEST SUCCEEDED **` | 59 / 14 一致 |
| android/ | `./gradlew test --rerun-tasks` | **40 tests / 0 failures** (10 クラス) | 40 / 0 一致 |
| android/ (instrumented) | `ANDROID_SERIAL=0B261JEC216142 ./gradlew connectedDebugAndroidTest` | **62 tests / 0 failures** | 62 / 0 一致 |
| kmp/ | `./gradlew allTests --rerun-tasks` | **48 tests / 0 failures** (iosSimulatorArm64 26 + androidHostTest 22) | 48 一致 |
| maui/ | `dotnet test` | **31 合格 / 0 失敗** | 31 / 0 一致 |
| maui/android/native/ | `./gradlew :ksdialogs-maui-bridge:test --rerun-tasks` | **9 tests / 0 failures** | 9 / 0 一致 |

instrumented の内訳 (62件): `DialogLayoutCaseTableTests` 38 (19 ケース × 2)・`DialogAttributeSupplyTests` 8・`DialogOutsideTapTests` 4・`DialogLayoutAttributeDefaultsTests` 3・`DialogPresentedWindowTests` 3・`DialogTransparentOverlayTests` 3・`DialogPresentationSizingTests` 2・`DialogLayoutCaseTableLoadingTests` 1。`adb devices` に実機 2 台 (Pixel 6a `2A141JEGR18112` / Pixel 4a `0B261JEC216142`) が device 状態であることを事前確認済み。

**公開 API 形状の負の検査 16 本**も 1 本ずつ個別に実行し、**全 16 本がビルド失敗 (= 期待結果) となり、規約表の診断と一致**することを確認した:

- android 4 本 (`vmAttribute` / `showOptions` / `resultType` / `notifierValue`) — 例: `Unresolved reference 'proportionalWidth'.`
- kmp 3 本 (`optionsType` / `showOptions` / `resultType`) — 例: `Unresolved reference 'DialogOptions'.`
- maui 5 本 — CS1061 / CS1739 / CS0029 / CS1503 / CS0311 をそれぞれ 1 件
- ios 4 本 — `value of type 'ConsumerDialogViewModel' has no member 'proportionalWidth'` ほか

正の検査は上記の既定実行に含まれる (android `:api-surface-check:compileDebugKotlin` / kmp `:api-surface-check:compileKotlinIosSimulatorArm64` / maui `KsDialogs.Maui.ApiSurfaceCheck` / ios テストビルド同梱) ため、各ルート green をもって通過。

なお **sample アプリのビルド・配備は本検証では再実行していない** (テストではないため)。`verification/sample-walkthrough/notes.md` に 2026-08-19 付で 4ルートすべてを本変更適用後のツリーからビルドして配備した記録があり、これを証跡として採用した。

### 2.5 旧経路の残骸検査

| 検索語 | ソースコードのヒット |
|---|---|
| `DialogLayoutProviding` (iOS/Android) | **0 件** (kasane ドキュメントの言及のみ) |
| `IDialogLayoutProviding` / `DialogLayoutAttributes` / `MauiDialogLayoutAttributes` (MAUI) | **0 件** (同上) |
| VM 契約への属性 | `DialogViewModel.{swift,kt,cs}` にメンバーなし。負の検査 `*vmAttribute` 4 ルートで機械的に固定 |
| samples の一時的な観測用差し込み | **残存なし** (透明 overlay 観測・snapshot 配線観測・属性輸送観測の 3 種すべて撤去済み。notes 側に SHA-256 一致での確認記録あり) |

---

## 3. 未記録乖離

**なし (0 件)。**

対応表に ❌ は 1 件もない。⚠️ の 2 件はいずれも `deviation.md` に記録済みの合意済み差分である。

---

## 4. 参考所見 (判定に影響しない observation)

一致検証の判定には影響しないが、次に触る人のために記録する。いずれもデルタスペックの Requirement / Scenario に対する不一致ではない。

1. **`ios/Tests/KsDialogsTests/Support/DialogLayoutMeasurement.swift:43-45` のコメントは実装と一致している。**
   コメントの 2 つの主張を実装で確認した — (a)「実効値のスナップショットは window 搭載時の初回 on-screen パス完了で固定済み (didMoveToWindow 起点)」: `window.rootViewController = container` (`:39`) の時点で `DialogContainerRootView.didMoveToWindow` (`DialogContainerViewController.swift:13-17`) → `settleLayoutSnapshotOnScreen()` (`:137-143`) → `settleLayoutSnapshot()` (`:149-165`) が走り `isLayoutSnapshotFrozen = true` (`:164`) になる。コメント位置 (`:43`) はその後方であり記述どおり。(b)「`prepareForPresentation` は暫定サイズ計算のみで固定に関与しない」: 当該メソッド本体 (`DialogContainerViewController.swift:126-130`) は `loadViewIfNeeded` / `view.frame = bounds` / `layoutIfNeeded` の 3 行のみで固定コードを含まない。**両主張とも実態と一致**。
   ただし review-004 の推奨修正 2 が併せて求めていた「このヘルパーの呼び出し順序は本番 (`prepareForPresentation` → `present`) と逆である」旨と「提示処理と同じ順序が要る検証は別ハーネスで行う」旨は、現コメントには書かれていない。本番同順の検証は `DialogAttributeSupplyTests.swift:147-202` (`attachmentChangeDuringFirstOnScreenLayoutPassIsAdopted`) と `Support/DialogTestPresentationSurface.swift` が担っており機能面の穴はない。

2. **MAUI の C#↔Native 境界は自動テストで跨げていない。** `DialogLayoutPassthroughTests.cs` の観測点は C# 側委譲面 (`DialogPresentationContent`) まで、`MauiDialogLayoutPassthroughTests.kt` は Kotlin 互換面から Native まで。iOS 側の C#↔Swift 区間は `verification/maui-hit-test/notes.md:73-102` と `verification/maui-snapshot-wiring/notes.md` の実環境証跡が担保しており、この限界は両テストファイル冒頭コメントと後者の「残る限界」節に明記されている。Scenario「添付プロパティがネイティブへ届く」は 2 層のテスト + 実環境証跡の連鎖で充足していると判定した。

3. **ケース表の `requirements` フィールドはローダーに読み捨てられている** (Android `DialogLayoutCaseLoader.kt`、iOS `Support/DialogLayoutCase.swift` の復号型にフィールドなし)。デルタスペックは「ケース ID ↔ Requirement の対応表を内包」することのみを求めており、機械利用は要求していないため一致。

4. **Android のケース表全量検証は属性を View 添付のみで供給している** (`DialogLayoutCaseTableTests.kt:34-35`)。ケース表の `attributes` は「供給合成後の実効値」と定義されており供給経路は問われないため一致。show 引数経路は `DialogAttributeSupplyTests.kt` が個別に担保。

5. **`kmp/.swiftpm-locks/` の生成物が VCS 上で不整合** (review-004 の Suggestion として既出、未対応)。デルタスペックの Requirement ではないため判定外。蒸留フェーズでの追跡方針決定を要する。

6. **`ui/brief.md` に「オーナーによる最終承認は未取得」の記載**がある。視覚照合 (tasks 6.3) は完了しているが、UI の最終承認そのものはオーナーの判断待ちであり、アーカイブ前に確認が要る。

---

## 5. 判定

**VALID**

- 全 19 Requirement / 35 Scenario が「✅ 一致」または「⚠️ deviation 記録済み」
- 未記録乖離 0 件
- tasks.md の虚偽チェック 0 件
- 足場アーティファクト (proposal / design / specs) の逆流 0 件
- 全 6 ビルドルートのテストを本検証で再実行し全件 green (59 / 40 / 62 / 48 / 31 / 9)。公開 API 形状の負の検査 16 本もすべて期待どおりの診断で失敗することを個別に確認
