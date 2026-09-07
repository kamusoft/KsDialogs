# Verify 001: add-toast

判定: **VALID**

デルタスペック 6能力・Scenario 30件 (TS-CO 8 / TS-NM 2 / TS-MX 5 / TS-AT 3 / TS-TR 2 / TS-AC 1 /
TS-IO 3 / TS-AN 4 / TS-MA 3 / TS-KM 3 / TS-SA 6 — 実数 40) をすべて実装・テストへ突き合わせた。
❌ は 0 件。虚偽チェックなし、足場の逆流なし、テスト全件成功。

- 検証日: 2026-08-28
- 対象: 作業ツリー全体 (HEAD `5d446e2` は提案アーティファクトのみ。実装は未コミット)
- 合意済み差分は `deviation.md` を正として扱った (⚠️ 行)

---

## 1. dialog-contract (specs/dialog-contract/spec.md)

### Requirement: Toast の公開面と fire-and-forget (ADDED)

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| TS-CO-01 show で表示され duration 経過で自動的に消える | `ios/Sources/KsDialogs/Presentation/ToastCoordinator.swift:91` (accept) / `:133` (beginDisplay) / `:163` (期限タイマー)、`android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/ToastCoordinator.kt:96` / `:116` / `:133` | `ios/Tests/KsDialogsTests/ToastContractTests.swift:24`、`android/ksdialogs/src/androidTest/kotlin/jp/kamusoft/ksdialogs/ToastContractTests.kt:47` | ✅ |
| TS-CO-02 duration 省略時は ToastStyle の既定 | `ToastCoordinator.swift:117` / `ToastCoordinator.kt:323` (effectiveDuration)、`ios/Sources/KsDialogs/Contract/ToastStyle.swift:34`・`android/.../ToastStyle.kt:35` (defaultDuration) | `ToastContractTests.swift:44`、`ToastContractTests.kt:59` | ✅ |
| TS-CO-03 0 以下の duration は style の既定に丸め | `ToastCoordinator.swift:117-125` / `ToastCoordinator.kt:323-335` (警告ログ + 内蔵既定 1500 への二段丸め) | `ToastContractTests.swift:59` / `:77` (内蔵既定への丸め)、`ToastContractTests.kt:83` | ✅ |
| TS-CO-04 未登録の ViewModel 型は fail-fast | `ToastCoordinator.swift:96` / `:293` (accept 内で同期 throw)、`ToastCoordinator.kt:97` / `:294` (resolveFactory) | `ToastContractTests.swift:88`、`ToastContractTests.kt:102` | ✅ |
| TS-CO-05 インライン経路はレジストリの状態を変えない | `ToastCoordinator.swift:274` (`.inline` はレジストリ非経由)、`ToastCoordinator.kt:297`、`ios/Sources/KsDialogs/Presentation/Toast.swift:86` (acceptInline) | `ToastContractTests.swift:104`、`ToastContractTests.kt:117` | ✅ |
| TS-CO-06 異なる入口が同じレジストリと style を共有 | `Toast.swift:12` / `:22` (`Toast()` → `.shared` coordinator)、`ToastCoordinator.swift:18`、`ToastCoordinator.kt:312` (companion `shared`) | `ToastContractTests.swift:133`、`ToastContractTests.kt:148` | ✅ |
| TS-CO-07 受理後の factory 失敗は破棄と資源解放 | `ToastCoordinator.swift:145-153` (catch → 警告 + 破棄) / `:213` (discard)、`ToastCoordinator.kt:162-168` / `:221` | `ToastContractTests.swift:161` / `:182` / `:204`、`ToastContractTests.kt:169` | ✅ |
| TS-CO-08 計時は受理時点から進み、入りの途中でも出へ移る | `ToastCoordinator.swift:102` (`ContinuousClock` の deadline) / `:163`、`ToastCoordinator.kt:101` (`SystemClock.elapsedRealtime`) / `:133`、撤去はフック完了後 (`ToastContainerViewController.swift:144`、`ToastContainer.kt` runDismissal) | `ToastContractTests.swift:288`、`ToastContractTests.kt:189` | ✅ |

Requirement 本文の追加条項 (Scenario 外) も実装を確認した:

- 提示環境の不在で受理は失敗せず、満了した保留表示は表示されずに破棄 →
  `ToastCoordinator.swift:175-193` / `ToastCoordinator.kt:146-186`。
  テスト `ToastContractTests.swift:224` / `:244` / `:255`、`ToastContractTests.kt:232` / `:252` / `:266`
- hide / メッセージ更新 / スコープ形 / 進捗口を持たない → 負の compile 検査 (TS-IO-03 / TS-AN-04 / TS-MA-03 / TS-KM-03)
- 上限クランプなし → `effectiveDuration` に上限分岐が無いことをコードで確認

### Requirement: 完全非対話と非モーダル (ADDED)

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| TS-NM-01 Toast の真下の要素が操作できる | `ios/Sources/KsDialogs/Presentation/ToastContainerRootView.swift:31` (`hitTest` が常に nil)、`android/.../ToastContainer.kt:92` (`FLAG_NOT_FOCUSABLE` \| `FLAG_NOT_TOUCHABLE`) | `ios/Tests/KsDialogsTests/ToastNonModalTests.swift:17`、`android/.../ToastNonModalTests.kt:41`、実提示は `ui/verification/sample-walkthrough.md` の連番 11 / 12 (ios / android) | ✅ |
| TS-NM-02 カスタム View 内の対話部品は反応しない | 同上 (器のルート面が子ごと当たり判定を持たない) | `ToastNonModalTests.swift:41`、`ToastNonModalTests.kt:59` | ✅ |

補助: `ToastNonModalTests.kt:87` がウィンドウのフラグ自体も固定。`ToastContainer.kt:77` で
`setCancelable(false)` / `setCanceledOnTouchOutside(false)`。
Android の IME・システムジェスチャ干渉は `android/.../ToastSystemInputTests.kt:42` / `:72` と
`evidence/toast-system-input-android.md` (tasks 3.7)。

### Requirement: 多重表示と表示の継続 (ADDED)

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| TS-MX-01 多重起動はすべて表示され起動順に重なる | `ToastCoordinator.swift:37` (displays は起動順) / `:192`、`ToastCoordinator.kt:58` / `:123` | `ios/Tests/KsDialogsTests/ToastMultiDisplayTests.swift:18`、`android/.../ToastMultiDisplayTests.kt:49` | ✅ |
| TS-MX-02 各表示は自分の duration で独立に消える | 表示ごとの `deadline` / タイマー (`ToastDisplay.swift`、`ToastDisplay.kt`) | `ToastMultiDisplayTests.swift:41`、`ToastMultiDisplayTests.kt:75` | ✅ |
| TS-MX-03 ページ遷移をまたいで表示が継続 | 器は提示スタックに参加せず key window / 独立ウィンドウへ直付け (`ToastCoordinator.swift:181`、`ToastContainer.kt`) | `ToastMultiDisplayTests.swift:61`、`ToastMultiDisplayTests.kt:91` | ✅ |
| TS-MX-04 回転しても継続し残り時間は維持 | `ToastContainerViewController.swift:230` (凍結済み実効値での再配置)、`ToastCoordinator.kt:260` (器を使い捨てて再取り付け・deadline は不変) | `ToastMultiDisplayTests.swift:82`、`ToastMultiDisplayTests.kt:120` | ✅ |
| TS-MX-05 Loading は起動順によらず Toast より前面 | `ToastCoordinator.swift:192` / `:196` (`insertSubview(below:)`)、`ToastCoordinator.kt:43` / `:127` / `:281` → `LoadingCoordinator.kt:342` (`bringToFrontWithoutPresentation`) | `ToastMultiDisplayTests.swift:111`、`ToastMultiDisplayTests.kt:146` (+ `:211` 既定配線)、実提示は `ui/verification/sample-walkthrough.md` 連番 08 (6実行経路) | ✅ |

「同一実効配置なら同座標に重なる (自動ずらしをしない)」は器が位置を加工しないこと
(`ToastContainerViewController.swift:234`、`ToastLayoutSnapshot.kt:32`) で確認。

### Requirement: 配置属性と ToastStyle (ADDED)

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| TS-AT-01 placement 引数で配置が変わる | `ToastContainerViewController.swift:234`、`ToastLayoutSnapshot.kt:32` | `ios/Tests/KsDialogsTests/ToastAttributeTests.swift:18`、`android/.../ToastAttributeTests.kt:43` | ✅ |
| TS-AT-02 優先順は show 引数 > 添付 > style 既定 > 契約既定 | 上記 + `ToastCoordinator.swift:103` / `ToastCoordinator.kt:107` (style 既定 → `ToastPlacementDefault`)、`ios/Sources/KsDialogs/Contract/ToastPlacementDefault.swift:13`、`android/.../ToastPlacementDefault.kt:11` | `ToastAttributeTests.swift:46`、`ToastAttributeTests.kt:59` | ✅ |
| TS-AT-03 style の変更は次の表示から効く | style は受理時に読み切る (`ToastCoordinator.swift:99`、`ToastCoordinator.kt:98`) | `ToastAttributeTests.swift:107`、`ToastAttributeTests.kt:96` | ✅ |

Requirement 本文の追加条項:

- 視覚項目はデフォルト View のみ / 既定値項目は全 Toast → `ToastStyle.swift:16-`、`ToastStyle.kt:31-`、
  テスト `ToastAttributeTests.swift:128`、`ToastAttributeTests.kt:122`
- 空文字はそのまま・長文は折り返し・スクロール/省略なし →
  `ToastAttributeTests.swift:143`、`ToastAttributeTests.kt:171`
- 器メタ属性 (overlayColor / isCanceledOnTouchOutside) を持たない → 負の検査 `toastOptions` (両 OS で失敗を実測)
- 共通ケース表が Toast の器でも適合 (契約既定配置 C23 を追加) → `core/layout-spec/cases.json:56` (C23)、
  `ios/Tests/KsDialogsTests/ToastLayoutCaseTableTests.swift:16` / `:43`、
  `android/.../ToastLayoutCaseTableTests.kt:35` / `:47` (20ケース × 2 観点を実測で通過)

### Requirement: 出入りの演出の適用 (ADDED)

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| TS-TR-01 カスタム View の演出フックが両局面で呼ばれる | `ToastContainerViewController.swift:80` / `:212` / `:255` / `:151` (共有 `DialogTransitionRunner`)、`android/.../ToastContainer.kt:44` | `ios/Tests/KsDialogsTests/ToastTransitionTests.swift:15`、`android/.../ToastTransitionTests.kt:41` | ✅ |
| TS-TR-02 デフォルト View は器の既定演出で出入りする | 同上 (添付なしは器の既定演出へ) | `ToastTransitionTests.swift:46`、`ToastTransitionTests.kt:87` | ✅ |

デフォルト View に演出選択の口を設けていないことは公開面 (`KsToast.swift` / `KsToast.kt`) で確認。

### Requirement: 支援技術への通知 (ADDED)

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| TS-AC-01 デフォルト View は announce しフォーカスを奪わない | `ios/Sources/KsDialogs/Presentation/ToastDefaultContentView.swift:110` (`.announcement`)、`android/.../ToastDefaultContentView.kt:83` (`TYPE_ANNOUNCEMENT`)、通知口は `ToastAccessibilityAnnouncer.swift` / `.kt` | `ios/Tests/KsDialogsTests/ToastAccessibilityTests.swift:15` / `:37` (カスタム View では器が無関与)、`android/.../ToastAccessibilityTests.kt:38` / `:65` / `:88` | ⚠️ deviation 記録済み (Android 既定通知口の `AccessibilityManager.isEnabled` ガード追加 = 付随修正。`deviation.md:5`) |

---

## 2. ios-native (specs/ios-native/spec.md)

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| TS-IO-01 公開面の正の compile 検査 | `ios/Sources/KsDialogs/Presentation/KsToast.swift:14` (protocol) / `:62` (省略形 extension)、`ios/Sources/KsDialogs/Presentation/Toast.swift:12` (`Toast.shared`)、`ios/Sources/KsDialogs/Registry/ToastViewRegistry.swift`・`ToastViewFactory.swift` (UIKit / SwiftUI 両系統)、`ios/Sources/KsDialogs/Contract/ToastStyle.swift` | `ios/Tests/KsDialogsTests/ToastApiSurfaceCompileChecks.swift:45-140` (既定ビルドでコンパイル。本検証で `xcodebuild test` 成功) | ⚠️ deviation 記録済み (インライン factory の `throws` 化 = `deviation.md:3`) |
| TS-IO-02 SwiftUI 登録が UIKit 登録と同じに働く | `ToastViewFactory.swift` の SwiftUI オーバーロード、`Toast.swift:71` | `ios/Tests/KsDialogsTests/ToastSwiftUIContentTests.swift:17` | ✅ |
| TS-IO-03 公開面の負の compile 検査 | 該当 API が存在しないこと | `ToastApiSurfaceCompileChecks.swift:144` / `:152` / `:161` / `:169`。**本検証で4フラグすべて別ビルドし、期待どおりコンパイルエラーを実測** (下記「テスト実行」) | ✅ |

`Toast.shared` は `style` / `registry` を持ち `options` 相当を持たない (`Toast.swift:31-38`、負の検査 `TOAST_OPTIONS`)。

---

## 3. android-native (specs/android-native/spec.md)

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| TS-AN-01 公開面の正の compile 検査 | `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/KsToast.kt:18`、`Toast.kt`、`ToastViewRegistry.kt`、`ToastStyle.kt` | `android/api-surface-check/src/main/kotlin/jp/kamusoft/ksdialogs/apicheck/ToastApiSurfaceChecks.kt` (`:api-surface-check:assembleDebug` 成功) | ✅ |
| TS-AN-02 Compose 登録が従来 View 登録と同じに働く | `android/ksdialogs-compose/src/main/kotlin/jp/kamusoft/ksdialogs/compose/ComposeToastRegistration.kt`・`ComposeToastShow.kt` (本体は Compose 非依存を維持) | `android/ksdialogs-compose/src/androidTest/kotlin/jp/kamusoft/ksdialogs/compose/ComposeToastCustomViewTests.kt:51` | ⚠️ deviation 記録済み (観察テストの待ち条件追加 = 付随修正。`deviation.md:10`) |
| TS-AN-03 Activity 再生成で多重 Toast が起動順のまま再取り付け | `android/.../ToastCoordinator.kt:260` (onHostChanged)、`ToastContainer.kt` (器の使い捨て) | `android/.../ToastActivityRecreationTests.kt:36` | ✅ |
| TS-AN-04 公開面の負の compile 検査 | 該当 API が存在しないこと | `android/api-surface-check/src/negativeCheckToast{Hide,ShowResult,ShowStyle,Options,ComposeFromCore}/` (`build.gradle.kts:71-75` で配線)。**本検証で5プロパティすべてビルドし、期待どおりコンパイルエラーを実測** | ✅ |

器は非フォーカス・タッチ素通しの全画面透過 Window (`ToastContainer.kt:92-94`) で design Decision 3・4 と一致。

---

## 4. maui-binding (specs/maui-binding/spec.md)

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| TS-MA-01 公開面の正の compile 検査 | `maui/KsDialogs.Maui/Presentation/IKsToast.cs:24`・`Toast.cs`、`maui/KsDialogs.Maui/Contract/ToastStyle.cs`、`maui/KsDialogs.Maui/Registry/ToastViewRegistry.cs` (C# 層レジストリ)、`maui/KsDialogs.Maui/Hosting/KsDialogsServiceCollectionExtensions.cs:145` (`RegisterForToast` = DI チェーン1行) | `maui/KsDialogs.Maui.ApiSurfaceCheck/DialogToastApiSurfaceChecks.cs` (既定ビルド成功: 警告0/エラー0) | ✅ |
| TS-MA-02 MAUI 経由の表示が Native 直接呼び出しと同じに観察される | `maui/KsDialogs.Maui/Internals/ToastGateway.cs`・`ToastPresenter.cs`・`ToastSettings.cs`、`maui/KsDialogs.Maui/Platforms/{Android,iOS}/PlatformToastGateway.cs`、bridge `maui/macios/native/KsDialogsMauiBridge/MauiToast{Bridge,Content,Style}.swift`・`maui/android/native/ksdialogs-maui-bridge/src/main/kotlin/jp/kamusoft/ksdialogs/maui/MauiToast{Bridge,Content,Style}.kt`、`maui/macios/KsDialogs.Binding.iOS/ApiDefinition.cs` | `maui/KsDialogs.Maui.Tests/ToastPassthroughTests.cs:21/50/71/104/122`、`ToastFacadeTests.cs:21/41/57/64`、`ToastDependencyInjectionTests.cs:25/46`、`maui/android/native/.../MauiToastPassthroughTests.kt`、実提示は `ui/verification/sample-walkthrough.md` (6実行経路) | ✅ |
| TS-MA-03 公開面の負の compile 検査 | 該当 API が存在しないこと | `maui/KsDialogs.Maui.ApiSurfaceCheck/NegativeChecks/Rejects{HideOnToast,AwaitOnToastShow,ReturnValueOnToastShow,StyleArgumentOnToastShow}.cs` (`.csproj:85-100` で配線)。**本検証で4プロパティすべてビルドし、CS1061 / CS4008 / CS0029 / CS1739 を実測** | ✅ |

互換面専用 VM 型を Native 共有レジストリへ1つだけ登録し利用者 VM 型は入れない構造
(`ToastPresenter.cs` / `MauiToastContent`) を確認 — maui/ADR-0001 と一致。
例外境界 (`Internals/BridgeContentSupply.cs`) は ⚠️ deviation 記録済み (`deviation.md:7`・`:8`)。

---

## 5. kmp-facade (specs/kmp-facade/spec.md)

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| TS-KM-01 共有コードからの呼び出し面の compile 検査 | `kmp/ksdialogs-kmp/src/commonMain/kotlin/jp/kamusoft/ksdialogs/kmp/KsToast.kt:24`・`Toast.kt`・`ToastViewModel.kt` (色・factory 登録は commonMain に無い) | `kmp/api-surface-check/src/commonMain/kotlin/jp/kamusoft/ksdialogs/kmp/apicheck/ToastApiSurfaceChecks.kt` (`:api-surface-check:assemble` 成功) | ✅ |
| TS-KM-02 型キー表示が OS 側登録の factory で解決される | `kmp/.../androidMain/.../AndroidToastGateway.kt`・`Toast.android.kt`、`kmp/.../iosMain/.../IosToastGateway.kt`・`Toast.ios.kt`、Swift 側登録 `ios/Sources/KsDialogs/Kmp/KsToastKmp.swift`・`KsDialogsInteropToastBridge.swift` | `kmp/.../androidHostTest/.../AndroidToastGatewayContractTests.kt`、`kmp/.../iosTest/.../InteropToastBridgeContractTests.kt`、`ios/Tests/KsDialogsTests/KsToastKmpTests.swift:24/43/56/73`、`kmp/.../commonTest/.../ToastSharedLayerCallTests.kt` | ✅ |
| TS-KM-03 commonMain の負の compile 検査 | 該当 API が commonMain に無いこと | `kmp/api-surface-check/src/negativeCheckToast{StyleType,StyleProperty,Registration,Hide,ShowResult}/` (`build.gradle.kts:54-58` で配線)。**本検証で5プロパティすべてビルドし、期待どおりコンパイルエラーを実測** | ✅ |

duration がミリ秒整数で commonMain から指定できることは `KsToast.kt:37` / `:50` で確認。

---

## 6. samples (specs/samples/spec.md)

TS-SA-01〜06 は `scripts/scenario-id-coverage.py:84-89` の allow-missing 登録により
手動通し + 撮影証跡で受け入れる (spec 本文の規定どおり)。

| Scenario | 実装 (4ルート) | 証跡 | 状態 |
|---|---|---|---|
| TS-SA-01 Default Toast の通し | `samples/android/app/src/main/kotlin/jp/kamusoft/ksdialogs/samples/android/MainActivity.kt` (showDefaultToast)、`samples/ios/KsDialogsSample/SampleMenuModel.swift:230`、`samples/kmp/shared/src/commonMain/kotlin/jp/kamusoft/ksdialogs/samples/kmp/SamplePresenter.kt:230`、`samples/maui/KsDialogs.Sample.Maui/SampleMenuPage.xaml.cs:112` | `ui/verification/sample-walkthrough.md` 結果表 (6実行経路 ✓)、`ui/verification/*-01/02-default-toast*.png` | ✅ |
| TS-SA-02 Custom Toast の通し | 各ルートの `CustomToast*` / `InlineToast*` / `SampleToastRegistration*` (kmp は共有 VM `samples/kmp/shared/.../CustomToastViewModel.kt` + OS 側登録、maui は DI 1行登録) | 同上 + `*-03-custom-toast.png` | ✅ |
| TS-SA-03 Toast Stack の通し | 各ルート showToastStack (3枚・duration ずらし・placement ずらし・3枚目長文) | 同上 + `*-04-toast-stack.png` / `*-05-toast-stack-first-gone.png` | ✅ |
| TS-SA-04 Toast Placement の通し | 各ルート showToastPlacement (上部中央・duration 3000) | 同上 + `*-06-toast-placement.png` | ✅ |
| TS-SA-05 Toast Overlap の通し | 各ルート runToastOverlap (Toast 10000ms → Dialog 2000ms 自動閉 → Loading 2000ms 自動終了 → `結果: 完了`)。定数は4ルートで一致 | 同上 + `*-07`〜`*-10` | ⚠️ deviation 記録済み (小画面での結果表示のためメニューをスクロール化。`deviation.md:6`。4ルート6経路すべてでスクロール容器を確認) |
| TS-SA-06 4ルートで同一デモが動く | `SampleText`(4ルート) の文言が spec 指定と完全一致 (`Default Toast` / `Custom Toast` / `カスタムトースト` / `インライントースト` / `Toast 1` / `Toast 2` / 長文 `Toast 3: …` / `Toast Placement` / `Placed Toast` / `Toast Overlap` / `Overlap Toast` / `結果: 完了`)、`SampleDemoId` に安定 ID 5件、`samples/README.md` 更新 (9→14件) | `ui/verification/sample-walkthrough.md`「TS-SA-06 の判定: 成立」 | ✅ |

---

## 7. 追加検査

### tasks.md の照合 (虚偽チェック)

全24タスク (1.1〜1.2 / 2.1〜2.7 / 3.1〜3.8 / 4.1〜4.3 / 5.1〜5.3 / 6.1〜6.3 / 7.1 / 8.1〜8.5) が
チェック済み。対応表と突き合わせ、**未実装のままチェックされたものは無い**。個別確認:

- 1.1 C23 追加 → `core/layout-spec/cases.json:56` (comment も19→20ケースへ更新)
- 1.2 TS 系 ID 登録 → `scripts/scenario-id-coverage.py:84-89` (allow-missing) / `:112-117` (MIRROR_AREAS)
- 3.7 実機確認 → `evidence/toast-system-input-android.md` + `ToastSystemInputTests.kt` (2件成功を再実測)
- 6.3 4ルート通し → `ui/verification/sample-walkthrough.md` + 証跡 PNG 60枚超
- 7.1 UI 照合 → `ui/verification/default-view-mock-match.md` (2周で収束・影の記録誤りも訂正済み)
- 8.5 失敗系の実機観測 → walkthrough の「失敗系の実機観測 (A 方式)」+「クラッシュ2件の追試」。
  途中経過に「tasks 8.5 は未完了のまま」の記述が残るが、同じ文書の追試節で原因特定 (Sample 側の
  古いネイティブリンク成果物) と解消確認まで到達しており、チェック済みは妥当

### 逆流検査 (足場の凍結)

`git status` / `git log` で確認。`proposal.md` / `design.md` / `specs/**` は **HEAD (`5d446e2`) から
一切変更されていない** (作業ツリーに差分なし)。変更があるのは `tasks.md` (チェックのみ + 8章追加) と
`ui/brief.md` (末尾に「視覚照合結果」を追記 — ksn-ui の照合結果記録として想定される追記であり、
要件・値の書き換えは無い)。**逆流なし**。

### 未記録乖離の洗い出し

対応表に ❌ は無い。Scenario に対応しない差分についても deviation.md の記録と突き合わせた:

| 差分 | 扱い |
|---|---|
| iOS の Loading / Dialog factory の `throws` 化 (`ios/Sources/KsDialogs/Registry/{Dialog,Loading}ViewFactory.swift` ほか) | `deviation.md:7`・`:8` に記録済み |
| MAUI iOS の例外境界 (`maui/KsDialogs.Maui/Internals/BridgeContentSupply.cs`、`Platforms/iOS/Platform{Dialog,Loading}Gateway.cs`、`maui/macios/native/**`) | 同上 |
| Sample メニューのスクロール化 (4ルート6経路) | `deviation.md:6` に記録済み |
| Android 既定通知口の `isEnabled` ガード | `deviation.md:5` に `[付随修正]` として記録済み |
| Compose Toast 観察テストの待ち条件 | `deviation.md:10` に `[付随修正]` として記録済み |
| `android/.../LoadingCoordinator.kt:342`・`LoadingContainer.kt:136`・`DialogTransitionRunner.kt:76` の追加 | TS-MX-05 (Loading 前面規則) の実装に必要な追加であり Requirement 配下。乖離ではない |
| `android/ksdialogs/src/androidTest/AndroidManifest.xml` | tasks 3.7 のテスト用 Activity 登録。乖離ではない |
| `kasane/lessons/inbox/**` | ksn-lesson の捕捉。仕様対象外 |

**未記録乖離: 0 件。**

観察 (乖離ではないが記録の精度に関するもの):
`deviation.md:8` は「KMP / Interop 面は非 throwing のまま無改変」と書くが、
`ios/Sources/KsDialogs/Kmp/KmpLoadingViewModel.swift:47` は内部の呼び出し側に `try` が入っている。
KMP 面の**公開シグネチャ**は非 throwing のまま (`KsLoadingKmp.swift` 等は無改変) なので
記述の意図とは一致しており、契約上の乖離ではない。

### UI 変更の確認

- 承認モックの記録: `ui/brief.md`「視覚照合結果」+ `ui/mock/approved.png` / `approved-sample-custom.png`
- 照合結果: `ui/verification/default-view-mock-match.md` (構造・トークン・意図の3点。既定値の指標表と
  実装定数が一致)
- 合意済み妥協: 「なし」と明記

### テストの実行 (本検証で実測)

| 対象 | コマンド | 結果 |
|---|---|---|
| Scenario ID 網羅 (ミラー含む) | `python3 scripts/scenario-id-coverage.py --require-mirror` | 175/197 (除外22) / ミラー OK / **未網羅なし** |
| iOS Native | `xcodebuild test -scheme KsDialogs` (iPhone 17 Simulator) | **251 tests / 47 suites 成功** (Toast 系 45 件を xcresult で個別確認) |
| iOS 負の compile 検査 (TS-IO-03) | `xcodebuild build-for-testing` × 4 フラグ | 4件すべて期待どおり失敗 (`has no member 'hide'` / `cannot convert value of type '()'` / `extra argument 'style'` / `has no member 'options'`) |
| Android Native | `:ksdialogs:testDebugUnitTest` + `:ksdialogs:connectedDebugAndroidTest` + `:ksdialogs-compose:connectedDebugAndroidTest` (Pixel 4a / API 33) | **372 tests / 0 failures** (Toast 系 78 件を XML で個別確認) |
| Android 負の compile 検査 (TS-AN-04) | `:api-surface-check:compileDebugKotlin` × 5 プロパティ | 5件すべて期待どおり失敗 |
| Android 正の compile 検査 (TS-AN-01) | `:api-surface-check:assembleDebug` | 成功 |
| KMP | `:ksdialogs-kmp:allTests --rerun-tasks` (androidHostTest + iosSimulatorArm64Test) | **96 tests / 0 failures** |
| KMP 負の compile 検査 (TS-KM-03) | `:api-surface-check:compileCommonMainKotlinMetadata` × 5 プロパティ | 5件すべて期待どおり失敗 |
| MAUI (C#) | `dotnet test KsDialogs.Maui.Tests` | **133 tests / 0 failures** |
| MAUI 負の compile 検査 (TS-MA-03) | `dotnet build KsDialogs.Maui.ApiSurfaceCheck` × 4 プロパティ | 4件すべて期待どおり失敗 (CS1061 / CS4008 / CS0029 / CS1739) |
| MAUI 正の compile 検査 (TS-MA-01) | `dotnet build KsDialogs.Maui.ApiSurfaceCheck` | 成功 (警告0 / エラー0) |
| MAUI Android bridge (Kotlin) | `:ksdialogs-maui-bridge:testDebugUnitTest --rerun-tasks` | **30 tests / 0 failures** |

合計 882 件の自動テストが成功、18 件の負の compile 検査がすべて期待どおり失敗した。

---

## 判定

**VALID**

- 全 Requirement / Scenario が「✅ 一致」または「⚠️ deviation 記録済み」
- tasks.md の虚偽チェックなし
- 足場アーティファクト (proposal / design / specs) の逆流なし
- 未記録乖離 0 件
- テストは全プラットフォームで実行し全件成功、負の compile 検査も全件が期待どおり失敗

アーカイブへ進める状態にある。
