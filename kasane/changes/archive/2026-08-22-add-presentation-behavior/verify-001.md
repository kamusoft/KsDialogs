# 一致検証: add-presentation-behavior (verify-001)

- **判定: VALID**
- 検証日: 2026-08-22
- 対象: `kasane/changes/add-presentation-behavior/specs/` の 6 capability / 17 Requirement / 61 Scenario と、HEAD `66ea298` に対する未コミットの作業ツリー全体
- 検証者: ksn-verifier (独立文脈。実装者の経緯報告は受け取っていない)
- 集計: **✅ 55 / ⚠️ 6 / ❌ 0**

---

## 1. テスト実行 (すべて自分で実行した)

| ルート | コマンド | 実測 | 結果 |
|---|---|---|---|
| ios/ | `xcodebuild test -scheme KsDialogs -destination 'platform=iOS Simulator,id=3B42B268-…'` (iPhone 17 / iOS 26.0) | **130 tests / 25 suites** | 0 failures (`** TEST SUCCEEDED **`) |
| android/ (unit) | `./gradlew test --rerun-tasks` | **50 tests** (TEST-*.xml の合算) | 0 failures |
| android/ (instrumented) | `ANDROID_SERIAL=0B261JEC216142 ./gradlew connectedDebugAndroidTest` (Pixel 4a / API 33) | **138 tests** (`:ksdialogs` 105 + `:ksdialogs-compose` 33) | 0 failures / skip 1 (`PB_SB_04` は `assumeTrue(SDK < R)`) |
| kmp/ | `./gradlew allTests --rerun-tasks` | **51 tests** (iosSimulatorArm64 29 + androidHostTest 22) | 0 failures |
| maui/ | `dotnet test` | **62 tests** | 0 failures |
| maui/android/native/ | `./gradlew :ksdialogs-maui-bridge:test --rerun-tasks` | **15 tests** | 0 failures |

いずれも件数まで確認した (test-execution.md の「終了コードだけでは検証にならない」に従う)。

### 負の検査 (本変更で追加された 10 本を全数実行)

`test-execution.md` の方式 (1 回のビルドに入る誤りは 1 つだけ / 成功したら検証は失敗) で 1 本ずつ回した。
**10 本すべてが期待どおりビルド失敗し、診断も一致**した。

| ルート | フラグ | 実測した診断 |
|---|---|---|
| ios/ | `KSDIALOGS_NEGATIVE_CHECK_SHOW_TRANSITION` | `extra argument 'transition' in call` |
| ios/ | `KSDIALOGS_NEGATIVE_CHECK_OPTIONS_TRANSITION` | `value of type 'DialogOptions' has no member 'transition'` |
| ios/ | `KSDIALOGS_NEGATIVE_CHECK_NONE_ARGUMENT` | `argument passed to call that takes no arguments` |
| ios/ | `KSDIALOGS_NEGATIVE_CHECK_DEFAULT_DURATION` | `'defaultDuration' is inaccessible due to 'internal' protection level` |
| android/ | `ksdialogs.negativeCheck.showTransition` | `None of the following candidates is applicable:` + `No parameter with name 'transition' found.` (show がオーバーロードされているため 2 件) |
| android/ | `ksdialogs.negativeCheck.optionsTransition` | `Unresolved reference 'transition'.` |
| android/ | `ksdialogs.negativeCheck.noneArguments` | `Too many arguments for 'fun none(): DialogTransition'.` |
| maui/ | `KsDialogsNegativeCheckShowTransition` | CS1739 (`ShowAsync` に `transition` パラメーターがない) |
| maui/ | `KsDialogsNegativeCheckOptionsTransition` | CS0117 (`Dialog` に `GetTransitionDuration` がない) |
| maui/ | `KsDialogsNegativeCheckNoneArguments` | CS1501 (引数 1 個の `None` オーバーロードがない) |

既存分 (iOS 4 / Android 4 / KMP 3 / MAUI 6) は今回未実行 (本変更の追加ではないため。正の検査は上表の既定実行に含まれており全ルート green)。
本変更後の負の検査は **iOS 8 / Android 7 / KMP 3 / MAUI 9 = 27 本**である
(コンテキストの「MAUI 7」は 9 本の誤りと思われる。フラグ定義は `maui/KsDialogs.Maui.ApiSurfaceCheck/KsDialogs.Maui.ApiSurfaceCheck.csproj` が正)。

### 検査スクリプト

| 検査 | 結果 |
|---|---|
| `python3 scripts/scenario-id-coverage.py` | 仕様 61 ID / テスト検出 58 ID / 除外 3 (PB-SM-01〜03) — **未網羅なし** |
| `python3 scripts/scenario-id-coverage.py --require-mirror` | **対象領域の ID はすべて iOS / Android 双方にある** |
| `python3 scripts/scenario-id-coverage.py --selftest` | 全件 OK (検査自体が空振りしていないことを確認) |
| `python3 scripts/comment-policy-lint.py` | 禁止 0 件 / 540 ファイル |

---

## 2. 対応表

`実装` は Requirement 単位の主たる置き場、`テスト` は Scenario ID を含むテストの置き場。
状態: ✅ 一致 / ⚠️ deviation 記録済み / ❌ 欠落・乖離。

### 2.1 dialog-contract

#### Requirement: トランジションの添付 (ADDED)

実装: `ios/Sources/KsDialogs/Contract/DialogTransition.swift`・`Contract/UIViewDialogAttributes.swift:28`・`SwiftUI/DialogAttributeAttachment.swift:68`・`Presentation/DialogContainerViewController.swift:410,448` / `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/DialogTransition.kt`・`ViewDialogAttributes.kt:36`・`DialogContainer.kt:258,278,329` / `android/ksdialogs-compose/src/main/kotlin/jp/kamusoft/ksdialogs/compose/KsDialogAttributes.kt:33`

| Scenario | テスト | 状態 |
|---|---|---|
| PB-TR-01 presentation フックが1回・ホスト View・UI スレッド | `ios/Tests/KsDialogsTests/DialogTransitionTests.swift` / `android/ksdialogs/src/androidTest/.../DialogTransitionTests.kt` (呼び出し回数・`hostView` 同一性・`isOnMainThread`・`isOnWindow`・レイアウト済みまで検査) | ⚠️ |
| PB-TR-02 片側だけの添付では未指定側に既定 | 同上2本 | ✅ |
| PB-TR-03 添付なしでは両フックとも呼ばれない | 同上2本 | ✅ |
| PB-TR-04 表示後の添付変更は退出に影響しない | 同上2本 (凍結済みの組が使われ、書き換え後は 0 回) | ✅ |

⚠️ PB-TR-01: deviation.md「オーナー確認待ち (暫定採用)」の **design Decision 10 (attached 中のコンテンツ非表示)** — iOS は凍結時点で alpha を同期復帰し、presentation フックは直後の MainActor ホップで開始する (design の字面では「フック開始と同時に表示」)。Scenario の THEN (フックが 1 回・ホスト View・UI スレッド・ウィンドウ上・レイアウト済み・表示状態) は満たしており、差分は design に対するもの。**オーナー確認待ちのため暫定**。Android は design どおり (deviation.md 実装メモ)。

#### Requirement: 退出の開始条件と直列化 (ADDED)

実装: `ios/Sources/KsDialogs/Contract/DialogDismissalOrigin.swift`・`Presentation/DialogContainerViewController.swift`・`Presentation/DialogContainerState.swift` / `android/…/DialogDismissalOrigin.kt`・`DialogContainer.kt`・`DialogContainerState.kt`

| Scenario | テスト | 状態 |
|---|---|---|
| PB-TR-05 presentation 中の閉鎖信号は完走後に退出 | iOS/Android `DialogTransitionTests` (イベント列 started→finished(presentation)→started→finished(dismissal) の完全一致) | ✅ |
| PB-TR-06 複数の閉鎖信号でも dismissal は1回 | 同上2本 | ✅ |
| PB-TR-07 外側タップでも dismissal フックが実行 | 同上2本 (cancelled 配送 + 1 回) | ✅ |
| PB-TR-08 呼び出し元キャンセルでも dismissal フック実行・形態別観察 | 同上2本 (Swift は `.cancelled` 戻り、Kotlin は `CancellationException`) | ✅ |
| PB-TR-09 OS 発の器消失ではフック非実行で即 cancelled | 同上2本 (`dismissal` 0 回・`removed`・ホスト解放) | ✅ |
| PB-TR-19 提示開始前の報告は演出なし | 同上2本 (`probe.events.isEmpty`) | ✅ |
| PB-TR-20 提示開始前の呼び出し元キャンセルは演出なし | 同上2本 (`created` 段階で cancel) | ✅ |
| PB-TR-21 none 直後の閉鎖は覆いの出現完了を待つ | 同上2本 (報告時点で `presenting`) | ✅ |
| PB-TR-28 presentation 中のキャンセルは presentation をキャンセルして退出 | 同上2本 (`cancelled(presentation)` あり・`finished` なし・dismissal 1 回) | ✅ |
| PB-TR-29 退出中のキャンセルは dismissal をキャンセルして脱出 | 同上2本 (ラッチ済み completed が届かないことも検査) | ✅ |
| PB-TR-22 退出中の入力は無視される | 同上2本 (iOS: `isUserInteractionEnabled == false` + `hitTest == nil` / Android: `dispatchTouchEvent` を器が飲む。両方とも最初の報告値が配送) | ✅ |

Requirement 本文の「デバッグビルドで一定時間を超えるフックに警告 (打ち切りなし)」は Scenario を持たないが実装あり — `DialogContainerViewController.swift:447,463` / `DialogContainer.kt:327,346`。

#### Requirement: 結果のラッチと配送 (ADDED)

実装: `ios/Sources/KsDialogs/Contract/DialogResultChannel.swift`・`Presentation/DialogContainerViewController.swift` / `android/…/DialogResultChannel.kt`・`DialogContainer.kt` / `maui/KsDialogs.Maui/Contract/DialogResultChannel.cs`

| Scenario | テスト | 状態 |
|---|---|---|
| PB-TR-10 show は dismissal 完了と撤去より先に返らない | iOS/Android `DialogTransitionTests` (iOS は撤去完了通知の保留まで分離して検査) + `maui/KsDialogs.Maui.Tests/DialogPresentationDeliveryTests.cs` | ✅ |
| PB-TR-11 退出中の二重報告はラッチ済み値 | 同上3本 | ✅ |
| PB-TR-12 退出中の OS 発器消失はフックをキャンセルして即配送 | iOS/Android `DialogTransitionTests` (completed のまま・cancelled にならない) | ✅ |
| PB-TR-23 提示中の OS 発器消失は cancelled | 同上2本 | ✅ |
| PB-TR-13 添付なしでも配送は撤去後 | iOS/Android `DialogTransitionTests` + `DialogPresentationDeliveryTests.cs` | ✅ |

#### Requirement: フックの失敗 (ADDED)

実装: `DialogContainerViewController.swift:448` (`runHook` が throw を吸収) / `DialogContainer.kt:329`

| Scenario | テスト | 状態 |
|---|---|---|
| PB-TR-14 presentation 失敗でも表示継続 | iOS/Android `DialogTransitionTests` (`shown` へ進む・Android は `Failed` イベントも検査) | ✅ |
| PB-TR-15 dismissal 失敗でも撤去と配送 | 同上2本 | ✅ |

#### Requirement: フックの完了は利用者の責務 (前提条件と脱出口) (ADDED)

| Scenario | テスト | 状態 |
|---|---|---|
| PB-TR-24 終了しないフックは呼び出し元キャンセルで脱出 | iOS/Android `DialogTransitionTests` (Android は `neverEndingHook` を使用) | ✅ |
| PB-TR-25 終了しないフックは OS 発器消失で脱出 | 同上2本 | ✅ |

#### Requirement: トランジションのプリセット (ADDED)

実装: `ios/Sources/KsDialogs/Contract/DialogTransition.swift:55,84,114,143`・`DialogTransitionEdge.swift`・`UITimingCurveProviderStandard.swift` / `android/…/DialogTransition.kt:46,68,98,126`・`DialogTransitionEdge.kt` / `maui/KsDialogs.Maui/Contract/DialogTransition.cs`

| Scenario | テスト | 状態 |
|---|---|---|
| PB-TR-16 プリセットで差し替わり覆いの時間も揃う | iOS/Android `DialogTransitionTests` (`resolvedOverlayDuration == duration`) | ⚠️ |
| PB-TR-17 none は中身側の待ちなし・覆いは既定 | 同上2本 (`defaultDuration` / 250ms) | ⚠️ |
| PB-TR-18 duration 0 は即完了 | 同上2本 | ⚠️ |
| PB-TR-26 負の duration は即完了 | 同上2本 | ⚠️ |
| PB-TR-27 範囲外 duration は即完了 | iOS (`.nan` / `±.infinity` の 3 ケース) / Android (`Duration.INFINITE`)。MAUI の `TimeSpan.MaxValue` / `uint.MaxValue` 超過は `DialogTransitionPassthroughTests.cs` の `UnusableDurations` が別途カバー | ⚠️ |

⚠️ 上記5件: deviation.md「オーナー確認待ち (暫定採用)」の **design Decision 3 (iOS プリセット factory のシグネチャ)** — iOS の `fade` / `slide` / `zoom` / `none` に `@MainActor` が付いている (design の表・完全シグネチャには isolation 注釈なし)。引数名・順・既定値・戻り型・`none()` 無引数は design どおりで、spec 本文の THEN には影響しない。**オーナー確認待ちのため暫定**。

#### Requirement: 多段表示の系列挙動の固定 (ADDED)

実装: 既存の提示機構 (`UIKitDialogPresentationSurface.swift` / `ActivityDialogPresentationSurface.kt`) + 本変更の状態機械。期待値の正は `kasane/concepts/core/api/multi-display-semantics.md` の差分表 (`PB-MD-01`〜`05` の参照あり)。

| Scenario | テスト | 状態 |
|---|---|---|
| PB-MD-01 結果確定で自分のダイアログだけ閉じる | `ios/Tests/KsDialogsTests/DialogMultiDisplayTests.swift` / `android/ksdialogs/src/test/.../DialogMultiDisplayTests.kt` | ✅ |
| PB-MD-02 2枚重ねて上から順に閉じる | 同上2本 | ✅ |
| PB-MD-03 重ね出し中の外側タップは手前のみ | 同上2本 | ✅ |
| PB-MD-04 下の段を先に閉じたとき (OS 差分表に従う) | 上記2本 + 実提示版 `DialogMultiDisplayPresentationTests.swift` / `.kt`。iOS = 上段 cancelled・dismissal 0 回、Android = 上段は `SHOWN` のまま残り後の報告で completed。**差分表と一致** | ✅ |
| PB-MD-05 器消失時の cancelled 確定 | `DialogMultiDisplayPresentationTests.swift` / `.kt` (Android は実 Activity を `DESTROYED` へ。確定後の追加報告で結果が増えないことまで検査) | ✅ |

#### Requirement: 表示中のウィンドウ寸法変化への追随 (ADDED)

実装: 既存のレイアウト再計算機構 (`DialogLayoutResolver.kt` / iOS 器のレイアウトパス) + 凍結スナップショットの維持

| Scenario | テスト | 状態 |
|---|---|---|
| PB-WN-01 回転後も配置規則が新しい寸法で成立 | `ios/…/DialogWindowChangeTests.swift` / `android/…/androidTest/.../DialogWindowChangeTests.kt` (両者とも同一の期待矩形。凍結後の添付書き換えが効かないことと `isLayoutSnapshotFrozen` も検査) | ✅ |
| PB-WN-02 寸法のみの変化に追随 | 同上2本 (幅 390→320) | ✅ |
| PB-WN-03 インセットのみの変化に追随 | 同上2本 (visibleArea 基準) | ✅ |

証跡: `verification/window-change-ios/`・`verification/window-change-android/` (実機/シミュレータの回転前後スクリーンショット + ログ)。

### 2.2 ios-native

#### Requirement: トランジション添付面 (iOS) (ADDED)

実装: `Contract/UIViewDialogAttributes.swift:28` (`ksDialogTransition`)・`SwiftUI/DialogAttributeAttachment.swift:68` (`dialogTransition(_:)`)・`Contract/DialogTransition.swift` (Hook 型 `@MainActor @Sendable (UIView) async throws -> Void`)

| Scenario | テスト | 状態 |
|---|---|---|
| PB-IA-01 UIView への添付が器で採用される | `ios/Tests/KsDialogsTests/DialogTransitionAttachmentTests.swift` | ✅ |
| PB-IA-02 SwiftUI modifier での添付が採用される | 同上 (`hostView is DialogSwiftUIContentView` まで検査) | ✅ |

公開面は正の api-surface 検査 `DialogApiSurfaceCompileChecks.swift:137,153,161,176` が固定 (テストビルドに同梱)。

#### Requirement: ステータスバー表示状態の非干渉 (iOS) (ADDED)

実装: `Presentation/DialogContainerViewController.swift` (`modalPresentationCapturesStatusBarAppearance` を既定 false のまま・`childForStatusBarHidden` を持たない)

| Scenario | テスト | 状態 |
|---|---|---|
| PB-IA-03 ステータスバー非表示の画面で再出現しない | `ios/Tests/KsDialogsTests/DialogStatusBarAppearanceTests.swift` | ✅ |

備考: THEN「非表示のまま維持される」を、UIKit が見えを尋ねる先が提示元のままであること (制御を奪わない機構) で検査している。画素の観察ではないが、非全画面提示でステータスバーの見えを決める条件そのものを突いており、空振りしない。

### 2.3 android-native

#### Requirement: トランジション添付面 (Android) (ADDED)

実装: `ViewDialogAttributes.kt:36` (`View.ksDialogTransition`)・`compose/KsDialogAttributes.kt:33` (`transition` 引数)・`DialogContainer.kt` (Main で開始・`NonCancellable` 相当の完遂)

| Scenario | テスト | 状態 |
|---|---|---|
| PB-AA-01 View 拡張プロパティでの添付が採用される | `android/ksdialogs/src/androidTest/.../DialogTransitionAttachmentTests.kt` | ✅ |
| PB-AA-02 Compose 属性宣言での添付が採用される | `android/ksdialogs-compose/src/androidTest/.../ComposeDialogTransitionTests.kt` (`DialogComposeContentView` が渡ること・Main スレッド開始を検査) | ✅ |
| PB-AA-03 呼び出し元キャンセル後も退出処理が完遂される | `DialogTransitionAttachmentTests.kt` (`CancellationException` 伝播 → 門が開くまで `DISMISSING` → 完遂 → `REMOVED`) | ✅ |

#### Requirement: システムバー表示状態の引き継ぎ (ADDED)

実装: `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/DialogWindowSystemBars.kt:72` (`inheritSystemBarState`。API 30+ は可視状態 + `systemBarsBehavior`、API 24〜29 は `systemUiVisibility` 丸ごとコピー)

| Scenario | テスト | 状態 |
|---|---|---|
| PB-SB-01 全バー非表示 (API 30+) | `android/ksdialogs/src/androidTest/.../DialogSystemBarsTests.kt` (API 33 / 36 で実行) | ✅ |
| PB-SB-02 status のみ非表示 (API 30+) | 同上 (API 33 / 36) | ✅ |
| PB-SB-03 navigation のみ非表示 (API 30+) | 同上 (API 33 / 36) | ✅ |
| PB-SB-04 旧経路 (API 24〜29) | 同上 (API 30+ では `assumeTrue` でスキップ。**API 29 エミュレータ `ksn_api29` で実行して green** — 証跡 `verification/system-bars-android/connected-system-bars.log` + `PB-SB-04-legacy-bars-hidden.png`) | ✅ |
| PB-SB-05 通常表示では従来どおり | 同上 (API 29 / 33 / 36) | ✅ |
| PB-SB-06 表示後の可視状態変更に追随しない | 同上 (API 33 / 36) | ✅ |
| PB-SB-07 表示後の behavior 変更に追随しない | 同上 (API 33 / 36) | ✅ |

証跡には **A/B 確認** (引き継ぎ実装を一時的に外すと PB-SB-01/02/03 と PB-SB-04 が落ちること) が記録されており、検査が空振りしていないことが示されている。

### 2.4 maui-binding

#### Requirement: トランジション添付面 (MAUI) (ADDED)

実装: `maui/KsDialogs.Maui/Presentation/DialogAttachedProperties.cs:106,235,241` (`TransitionProperty` / `GetTransition` / `SetTransition`)・`maui/KsDialogs.Maui/Internals/DialogTransitionRunner.cs`・`Internals/DialogContentHost.cs` / ブリッジ `maui/macios/native/KsDialogsMauiBridge/MauiDialogSingleCompletion.swift`・`MauiDialogTransitionAwaiter.swift` / `maui/android/native/ksdialogs-maui-bridge/src/main/kotlin/.../MauiDialogTransition.kt`

| Scenario | テスト | 状態 |
|---|---|---|
| PB-MA-01 添付プロパティがネイティブへパススルー | `maui/KsDialogs.Maui.Tests/DialogTransitionPassthroughTests.cs` (実効値の同一性・中身の View が渡る・UI スレッド開始・未添付なら runner を作らない・overlayDuration の未指定伝播) | ✅ |
| PB-MA-02 退出フックの完了待ちが成立 | 同上 (門が開くまで完了 0 回 → 1 回) | ✅ |
| PB-MA-03 fault しても結果は配送 | 同上 (fault / その場 throw / cancel の 3 形とも完了 1 回) | ✅ |
| PB-MA-04 完了通知は多重に届かない | 同上 (`DialogTransitionRunner` と `DialogSingleCompletion` の両方で 1 回) + `maui/android/native` の「閉鎖の通知はちょうど1回だけ届く」6 件 | ✅ |

#### Requirement: プリセットの MAUI 表現 (ADDED)

実装: `maui/KsDialogs.Maui/Contract/DialogTransition.cs` (`Fade` / `Slide` / `Zoom` / `None`、`*Async` 系 MAUI アニメーション API)

| Scenario | テスト | 状態 |
|---|---|---|
| PB-MA-05 プリセット添付がネイティブ経路で機能する | `DialogTransitionPassthroughTests.cs` 5 本 (同じ実行口を通る・4 種とも両フックと overlayDuration を持つ・overlayDuration がブリッジへ渡る・成立しない時間は演出なしで即完了・中身が要素ツリーに載る) | ✅ |

備考: `TranslateTo` / `FadeTo` / `ScaleTo` → `*Async` への改名は deviation.md 実装メモに記録済み (MAUI 10 で旧名が CS0618。挙動同一・公開 API 影響なし)。

### 2.5 kmp-facade

#### Requirement: Kotlin 経路の呼び出し元キャンセル追随 (iOS gateway) (ADDED)

実装: `kmp/ksdialogs-kmp/src/iosMain/kotlin/jp/kamusoft/ksdialogs/kmp/IosDialogGateway.kt:66-80,122-126` (`IosDialogShowCancellation` を `invokeOnCancellation` から引く)

| Scenario | テスト | 状態 |
|---|---|---|
| PB-KC-01 キャンセルで当該ダイアログが閉じ CancellationException | `kmp/…/iosTest/.../IosDialogGatewayCancellationTests.kt` (当該 show の取り消し 1 回・他の show 0 回・例外伝播) + 実提示の閉鎖は `ios/Tests/KsDialogsTests/KsDialogsKmpCancellationTests.swift` | ✅ |
| PB-KC-02 提示開始前のキャンセルを取りこぼさない | 同 Kotlin テスト 2 本 (提示前の取り消し到達 / キャンセル後に届いた結果は配送されない) | ✅ |

備考: PB-KC-01 の THEN のうち「ダイアログが閉じ」を Kotlin 層では「取り消し操作がちょうど 1 回届く」に読み替えている件は deviation.md 実装メモに記録済み (KMP の iOS テスト実行体は `UIApplicationMain` を通らず key window が無く実提示に到達できない)。実際に閉じることは Swift 側テストが担保しており、層別は design Decision 7 と整合する。証跡 `verification/kmp-cancellation/README.md`。

#### Requirement: KMP 登録コンテンツへのトランジション添付 (ADDED)

実装: KMP 共有コードの公開 API 追加なし (kmp/ADR-0002 維持)。添付面は Native 側 (`UIView.ksDialogTransition`)。

| Scenario | テスト | 状態 |
|---|---|---|
| PB-KC-03 KMP 登録コンテンツの添付が機能する | `ios/Tests/KsDialogsTests/KsDialogsKmpTransitionTests.swift` 2 本 (型付き面 `dialogs.kmp.show` と共有コード経路 `KsDialogsInteropBridge` の両方。dismissal フック完了前は結果が届かないことも検査) | ✅ |

### 2.6 samples

#### Requirement: トランジションデモ (ADDED)

実装 (4形態):
- iOS `samples/ios/KsDialogsSample/` (`SampleTransitionPanelScreen.swift`・`TransitionDialogViewModel.swift`・`SampleCustomTransition.swift` 他)
- Android `samples/android/app/src/main/kotlin/jp/kamusoft/ksdialogs/samples/android/` (`SampleTransitionPanelView.kt`・`TransitionDialogCardView.kt`・`SampleCustomTransition.kt` 他)
- MAUI `samples/maui/KsDialogs.Sample.Maui/` (`SampleTransitionPanelPage.xaml(.cs)`・`TransitionDialogCardView.xaml(.cs)`・`SampleCustomTransition.cs` 他)
- KMP `samples/kmp/shared/src/commonMain/.../TransitionDialogViewModel.kt`・`SampleTransitionPreset.kt` + `iosApp/` / `androidApp/` の各パネル

| Scenario | テスト / 証跡 | 状態 |
|---|---|---|
| PB-SM-01 プリセットを選んで表示できる | 自動テスト対象外 (coverage スクリプトの明示的除外)。`verification/sample-walkthrough/README.md` の**確定版**受け入れ表 = 「満たす (4ルート)」。方向写像 4 方向・イージング写像 4 種・None の無効表示まで実機で確認 | ✅ |
| PB-SM-02 カスタムフックの実演 | 同上 = 「満たす (4ルート)」(MAUI iOS の 80pt 移動 + フェードの解消確認済み) | ✅ |
| PB-SM-03 4形態で同一デモが動く | 同上 = 「満たす」(デモ項目・操作・文言・静止した見た目に加えて演出の見え方も 4 形態で一致) | ✅ |

備考: README には所見3 (MAUI iOS で transform が効かない) が未解決だった時点の受け入れ表も残っているが、末尾の「確定版」表が置き換えると明記されており、修正 (`DialogContentHost`) と解消確認の記録も揃っている。文言表・調整面の規範表との一致と、`ui/verification/` の 6 状態 × 4ルート照合は `ui/brief.md` の照合結果表 (4観点すべて「一致」、1周で収束、乖離修正 0 件) が担保する。

---

## 3. 追加検査

### 3.1 tasks.md の虚偽チェック

36 タスクすべて `[x]`。対応表と突き合わせた結果、**未実装のままチェックされたタスクはない**。

- 1.1〜1.5: ID 付与・実提示版 MD テスト・網羅スクリプト・WN テスト — すべて実物を確認
- 2.1〜2.8: `DialogDismissalOrigin` / 状態機械 / 自前駆動 / 脱出口 / 警告ログ / `NonCancellable` 相当 — すべて実物を確認。2.8 の退行確認は全ルート green で裏付け
- 3.1〜3.5: 添付面・プリセット・api-surface 検査 (正・負とも実行して確認)
- 4.1〜4.5: ブリッジ実行口 (`MauiDialogSingleCompletion.swift` / `MauiDialogTransition.kt`)・C# アダプタ・テスト
- 5.1〜5.3: `DialogWindowSystemBars.inheritSystemBarState` と API 別テスト・証跡
- 6.1〜6.2: `IosDialogGateway` の `invokeOnCancellation` 追随とテスト
- 7.1〜7.4: `concepts/core/api/transition-semantics.md` (新設・30KB)・`result-notification-semantics.md`・`multi-display-semantics.md`・`layout-semantics.md` の更新を確認
- 8.1〜8.4: 4形態のデモ実装・視覚照合・実機証跡

sample-walkthrough README の中間節に「8.2 と 8.4 は未完のまま残した」という記述があるが、その後の 2 節 (所見3 の修正と解消確認 / MAUI iOS の最終確認) で両方が仕上がっており、確定版の受け入れ表で closed。tasks の `[x]` と矛盾しない。

### 3.2 逆流検査 (足場の書き換え)

`git status` / `git diff` (読み取りのみ) で確認:

- `specs/`・`proposal.md`・`design.md`: **変更なし** (HEAD 66ea298 のまま)。逆流なし
- `tasks.md`: 変更あり = チェックの付与のみ (正常)
- `ui/brief.md`: 変更あり = **純粋な追記のみ** (`@@ -54,3 +54,133 @@`、既存行の書き換えゼロ)。追記内容は実装で決めた値・モックにない要素の出典・4観点の照合結果・残差・オーナー最終承認の状態であり、ksn-ui の記録先として正しい。規範表 (見た目の正) には手が入っていない

### 3.3 UI 変更の検査

- `ui/brief.md` に承認モックの記録あり: 静的 = 案B (`mock/approved.png`、2026-08-21 オーナー承認)、動的 = `mock/mock-preset-motion.html` (2026-08-21 オーナー最終承認)。**モック承認ゲートは実装前に閉じている**
- **合意済み妥協: なし** (brief に明記)。残差 3 件 (チップ高さの OS 下限・無効表示の濃さ・戻る記号の余白) はプラットフォームの流儀による差として記録され、いずれも既存 Sample の流儀を踏襲したもの
- オーナーの最終承認 (実装スクリーンショット) は brief 上「未取得 — 4ルート揃った時点でまとめて行う想定」。**これは検証の ❌ ではない** (ksn-verify の判定軸ではなく、オーナーレビューの段取り)。呼び出し元へ申し送る

### 3.4 未記録の乖離

**なし。** deviation.md に記録のない乖離は見つからなかった。

---

## 4. 所見 (判定には影響しない申し送り)

1. **`concepts/cross/conventions/test-execution.md` の実測件数が古い** — 2026-08-19 時点の値 (ios 90 / instrumented 94 / kmp 48 / maui 44 / bridge 9 / 負の検査 17 本) のままで、実測 (ios 130 / instrumented 138 / kmp 51 / maui 62 / bridge 15 / 負の検査 27 本) と乖離している。同文書は「テスト構成が育って実態が変わったら本規約を実測で更新する」と自ら定めており、**蒸留フェーズでの更新対象**。本変更の tasks に更新項目がないため未記録乖離とはしないが、そのままアーカイブすると次の実行者が件数を取り違える
2. **`verification/kmp-cancellation/README.md` の「ios/ の全件実行も 129 tests」** — 現時点の実測は 130 tests。証跡を書いた後にテストが 1 本増えたための鮮度差と読める (`maui`/`ios` の他の証跡も同様に途中断面)。証跡としての結論 (0 failures) は変わらない
3. **オーナーの UI 最終承認が未取得** (3.3 参照)。4ルート揃った現在、before/after の提示ができる状態
4. **コンテキストの負の検査件数「MAUI 7」は 9 が正**。フラグ定義 (csproj) を正として本書に記録した

---

## 5. 判定

**VALID**

- 61 Scenario すべてに実装とテスト (または合意済みの実機証跡) が対応し、GIVEN/WHEN/THEN がテストの検査内容として表現されている
- ❌ (実装なし / テストなし / 期待値不一致 / 未記録の乖離) は **0 件**
- ⚠️ 6 件はいずれも deviation.md の「オーナー確認待ち (暫定採用)」2 項目に由来し、**spec 本文の THEN には影響しない** (design.md の字面に対する差分)。オーナーが却下した場合は実装修正へ戻す前提で暫定採用されている
- tasks.md の虚偽チェックなし、足場の逆流なし、全ルートのテストが実行件数つきで green、負の検査 (新規 10 本) も期待どおり失敗
