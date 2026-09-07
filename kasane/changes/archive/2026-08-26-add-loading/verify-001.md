# 一致検証: add-loading (001 回目)

**日付**: 2026-08-26
**判定**: **VALID**

対象は `specs/` 6 能力 (dialog-contract / ios-native / android-native / maui-binding / kmp-facade / samples) の
全 Requirement 12 件 / 全 Scenario 43 件。実装は作業ツリーの未コミット変更全体 (modified 47 / untracked 150)。
`deviation.md` の全 13 項目と `ui/brief.md` の合意済み差分 (オーナー最終承認済み) は違反として扱わない。

すべての Requirement が ADDED のため、MODIFIED / REMOVED の検査対象はない。

---

## 1. dialog-contract

### Requirement: Loading の公開面と合流

実装の据え付け: iOS `ios/Sources/KsDialogs/Presentation/KsLoading.swift:10` (契約 protocol) +
`ios/Sources/KsDialogs/Presentation/Loading.swift:11` (既定シングルトン `shared`、`Loading()` も
`LoadingCoordinator.shared` へ委譲 — `Loading.swift:23`) + `ios/Sources/KsDialogs/Presentation/LoadingCoordinator.swift:27`
(状態の唯一の正)。Android `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/KsLoading.kt:15` +
`android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/Loading.kt:17`(`:126` = `instance`) +
`android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/LoadingCoordinator.kt:50`。

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| [LD-CO-01] show で表示され hide で消える | `ios/Sources/KsDialogs/Presentation/LoadingCoordinator.swift:114` / `:160`、`android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/LoadingCoordinator.kt:153` / `:240` | `ios/Tests/KsDialogsTests/LoadingCoalescingTests.swift:16`、`android/ksdialogs/src/androidTest/kotlin/jp/kamusoft/ksdialogs/LoadingCoalescingTests.kt:44` | ✅ 一致 |
| [LD-CO-02] スコープ形は処理完了で自動的に閉じ、処理の戻り値を返す | `ios/Sources/KsDialogs/Presentation/Loading.swift:160` (runScope)、`android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/Loading.kt:108` | `ios/Tests/KsDialogsTests/LoadingCoalescingTests.swift:35`、`android/ksdialogs/src/androidTest/kotlin/jp/kamusoft/ksdialogs/LoadingCoalescingTests.kt:61` | ✅ 一致 |
| [LD-CO-03] 重なったスコープ形は1つの表示に合流し、最後の完了で閉じる | `ios/Sources/KsDialogs/Presentation/LoadingCoordinator.swift:132`-`:139` / `:152`、`android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/LoadingCoordinator.kt:175` / `:228` | `ios/Tests/KsDialogsTests/LoadingCoalescingTests.swift:55`、`android/ksdialogs/src/androidTest/kotlin/jp/kamusoft/ksdialogs/LoadingCoalescingTests.kt:81` | ✅ 一致 |
| [LD-CO-04] 表示中でも渡した処理は必ず実行される | `ios/Sources/KsDialogs/Presentation/LoadingCoordinator.swift:132`、`android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/LoadingCoordinator.kt:175` | `ios/Tests/KsDialogsTests/LoadingCoalescingTests.swift:82`、`android/ksdialogs/src/androidTest/kotlin/jp/kamusoft/ksdialogs/LoadingCoalescingTests.kt:106` | ✅ 一致 |
| [LD-CO-05] 処理の失敗は合流1件の終了として数え、呼び出し元へ伝播する | `ios/Sources/KsDialogs/Presentation/Loading.swift:176` (catch 経路でも `endUse`)、`android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/Loading.kt:108` | `ios/Tests/KsDialogsTests/LoadingCoalescingTests.swift:95`、`android/ksdialogs/src/androidTest/kotlin/jp/kamusoft/ksdialogs/LoadingCoalescingTests.kt:118` | ✅ 一致 |
| [LD-CO-06] hide は合流数によらず即閉じ、走行中の処理は継続する | `ios/Sources/KsDialogs/Presentation/LoadingCoordinator.swift:160` (世代を進めて締め出す)、`android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/LoadingCoordinator.kt:240` | `ios/Tests/KsDialogsTests/LoadingCoalescingTests.swift:110`、`android/ksdialogs/src/androidTest/kotlin/jp/kamusoft/ksdialogs/LoadingCoalescingTests.kt:131` | ✅ 一致 |
| [LD-CO-07] メッセージは後勝ち | `ios/Sources/KsDialogs/Presentation/LoadingCoordinator.swift:135`、`android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/LoadingCoordinator.kt:175` | `ios/Tests/KsDialogsTests/LoadingCoalescingTests.swift:138`、`android/ksdialogs/src/androidTest/kotlin/jp/kamusoft/ksdialogs/LoadingCoalescingTests.kt:160` | ✅ 一致 |
| [LD-CO-08] setMessage は表示中のみ有効で合流に関与しない | `ios/Sources/KsDialogs/Presentation/LoadingCoordinator.swift:169`、`android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/LoadingCoordinator.kt:251` | `ios/Tests/KsDialogsTests/LoadingCoalescingTests.swift:153`、`android/ksdialogs/src/androidTest/kotlin/jp/kamusoft/ksdialogs/LoadingCoalescingTests.kt:174` | ✅ 一致 |
| [LD-CO-09] コンテンツは最初の開始が決める | `ios/Sources/KsDialogs/Presentation/LoadingCoordinator.swift:132` (合流時は `validateContentRequest` のみ)、`android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/LoadingCoordinator.kt:175` | `ios/Tests/KsDialogsTests/LoadingCoalescingTests.swift:176`、`android/ksdialogs/src/androidTest/kotlin/jp/kamusoft/ksdialogs/LoadingCoalescingTests.kt:196` | ✅ 一致 |
| [LD-CO-10] hide 後の新しい表示は旧世代の完了で閉じない | `ios/Sources/KsDialogs/Presentation/LoadingCoordinator.swift:152` (`token.generation == generation` の門)、`android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/LoadingCoordinator.kt:228` | `ios/Tests/KsDialogsTests/LoadingCoalescingTests.swift:214`、`android/ksdialogs/src/androidTest/kotlin/jp/kamusoft/ksdialogs/LoadingCoalescingTests.kt:230` | ✅ 一致 |
| [LD-CO-11] 旧世代の遅延進捗・メッセージは新しい表示に届かない | `ios/Sources/KsDialogs/Presentation/LoadingCoordinator.swift:176` (進捗のみ世代で遮断)、`android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/LoadingCoordinator.kt:268` | `ios/Tests/KsDialogsTests/LoadingCoalescingTests.swift:248`、`android/ksdialogs/src/androidTest/kotlin/jp/kamusoft/ksdialogs/LoadingCoalescingTests.kt:265` | ⚠️ deviation 記録済み (遮断は進捗のみ。`setMessage` は世代に紐づかないグローバル操作 — `deviation.md` 1行目) |
| [LD-CO-12] hide と最終 start は撤去完了後に戻る | `ios/Sources/KsDialogs/Presentation/LoadingCoordinator.swift:204` (`finishDisplay` が撤去完了まで待つ)、`android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/LoadingCoordinator.kt:406` | `ios/Tests/KsDialogsTests/LoadingCoalescingTests.swift:291`、`android/ksdialogs/src/androidTest/kotlin/jp/kamusoft/ksdialogs/LoadingCoalescingTests.kt:309` | ✅ 一致 |
| [LD-CO-13] 出の途中の新しい開始は出の完了後に新世代として表示される | `ios/Sources/KsDialogs/Presentation/LoadingCoordinator.swift:119` (`waitForPendingDismissal`)、`android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/LoadingCoordinator.kt:388` | `ios/Tests/KsDialogsTests/LoadingCoalescingTests.swift:319`、`android/ksdialogs/src/androidTest/kotlin/jp/kamusoft/ksdialogs/LoadingCoalescingTests.kt:329` | ✅ 一致 |
| [LD-CO-14] 提示環境が無くても action は実行される | `ios/Sources/KsDialogs/Presentation/LoadingCoordinator.swift:196` (取り付け先が無ければ器を作らず合流だけ成立)、`android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/LoadingCoordinator.kt:292` | `ios/Tests/KsDialogsTests/LoadingCoalescingTests.swift:344`、`android/ksdialogs/src/androidTest/kotlin/jp/kamusoft/ksdialogs/LoadingCoalescingTests.kt:354` | ✅ 一致 |
| [LD-CO-15] 異なる入口からの利用は1つの表示に合流する | `ios/Sources/KsDialogs/Presentation/Loading.swift:23` (`Loading()` → `.shared` coordinator)、`android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/Loading.kt:22` | `ios/Tests/KsDialogsTests/LoadingCoalescingTests.swift:356`、`android/ksdialogs/src/androidTest/kotlin/jp/kamusoft/ksdialogs/LoadingCoalescingTests.kt:365` | ✅ 一致 |

失敗モデル (構成ミスの fail-fast) は LD-CV-04 で、状態の共有と直列化は LD-CO-15 と
`ios/Sources/KsDialogs/Presentation/LoadingCoordinator.swift:26` (`@MainActor`) /
`android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/LoadingCoordinator.kt:64` (`Dispatchers.Main.immediate`) で押さえられている。

### Requirement: 器メタ属性の適用

実装の据え付け: iOS `ios/Sources/KsDialogs/Presentation/LoadingContainerViewController.swift:39` (器) +
`ios/Sources/KsDialogs/Layout/DialogLayoutApplier.swift` (Dialog 器から切り出した共有部品 — tasks 1.2)。
Android `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/LoadingContainer.kt:37` (専用の全画面透過 Window) +
`android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/DialogLayoutHost.kt` (tasks 1.3)。
既定ローディング用の設定プロパティは `ios/Sources/KsDialogs/Presentation/LoadingSettings.swift` /
`android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/LoadingSettings.kt`。

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| [LD-AT-01] placement 引数で既定ローディングの配置が変わる | `ios/Sources/KsDialogs/Presentation/LoadingCoordinator.swift:197`、`android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/LoadingCoordinator.kt:317` | `ios/Tests/KsDialogsTests/LoadingAttributeTests.swift:18`、`android/ksdialogs/src/androidTest/kotlin/jp/kamusoft/ksdialogs/LoadingAttributeTests.kt:56` | ✅ 一致 |
| [LD-AT-02] カスタム View の添付属性が Dialog と同じ優先順位で効く | `ios/Sources/KsDialogs/Presentation/LoadingContainerViewController.swift:248` (`composedLayout`)、`android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/LoadingContainer.kt:37` | `ios/Tests/KsDialogsTests/LoadingAttributeTests.swift:47`、`android/ksdialogs/src/androidTest/kotlin/jp/kamusoft/ksdialogs/LoadingAttributeTests.kt:84` | ✅ 一致 |
| [LD-AT-03] 外側タップで閉じず、背後にも透過しない | `ios/Sources/KsDialogs/Presentation/LoadingContainerViewController.swift:111` (器の root がタップを飲む)、`android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/LoadingContainer.kt:90` | `ios/Tests/KsDialogsTests/LoadingAttributeTests.swift:86`、`android/ksdialogs/src/androidTest/kotlin/jp/kamusoft/ksdialogs/LoadingAttributeTests.kt:120` | ✅ 一致 |
| [LD-AT-04] ダイアログ表示中の Loading は最前面で入力を遮る | iOS: key window 直貼り `ios/Sources/KsDialogs/Presentation/LoadingContainerViewController.swift:146` / `ios/Sources/KsDialogs/Presentation/LoadingPresentationSurface.swift`。Android: Loading 専用 Window `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/LoadingContainer.kt:37` | `ios/Tests/KsDialogsTests/LoadingAttributeTests.swift:116`、`android/ksdialogs/src/androidTest/kotlin/jp/kamusoft/ksdialogs/LoadingAttributeTests.kt:144` (実タップ注入) + 実提示証跡 `verification/loading-front/notes.md` (両 OS 4枚ずつ) | ✅ 一致 (spec の「実提示での検証 — 両 OS」を証跡が満たす) |
| [LD-AT-05] 設定プロパティの options 変更が次の表示から効く | `ios/Sources/KsDialogs/Presentation/LoadingSettings.swift`、`android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/LoadingSettings.kt` (各表示の開始時に読む — `LoadingCoordinator.swift:122` / `LoadingCoordinator.kt:192`) | `ios/Tests/KsDialogsTests/LoadingAttributeTests.swift:164`、`android/ksdialogs/src/androidTest/kotlin/jp/kamusoft/ksdialogs/LoadingAttributeTests.kt:174` | ✅ 一致 |
| [LD-WN-01] 画面の変化をまたいで表示が継続し再配置される | `ios/Sources/KsDialogs/Presentation/LoadingContainerViewController.swift:258` (`updateLayoutForCurrentBounds`)、Android は Activity 再生成をまたぐ再取り付け `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/LoadingCoordinator.kt:333` / `:350` + `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/ResumedActivityTracker.kt` | `ios/Tests/KsDialogsTests/LoadingAttributeTests.swift:189`、`android/ksdialogs/src/androidTest/kotlin/jp/kamusoft/ksdialogs/LoadingAttributeTests.kt:200` | ✅ 一致 |

レイアウト共通ケース表の Loading 器での全量実測 (tasks 2.3 / 3.3) は
`ios/Tests/KsDialogsTests/LoadingLayoutCaseTableTests.swift:12` と
`android/ksdialogs/src/androidTest/kotlin/jp/kamusoft/ksdialogs/LoadingLayoutCaseTableTests.kt:23` にある
(どちらも `core/layout-spec/cases.json` を読み、isCanceledOnTouchOutside は「常に無効」に読み替え)。

### Requirement: 進捗通知

実装の据え付け: `ios/Sources/KsDialogs/Presentation/LoadingCoordinator.swift:176` (`report`) +
`ios/Sources/KsDialogs/Presentation/LoadingReportQueue.swift` (受理順の直列化) +
`ios/Sources/KsDialogs/Contract/LoadingProgressReceiver.swift:7`。
Android `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/LoadingCoordinator.kt:264` / `:268` +
`android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/LoadingProgressReceiver.kt:10`。

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| [LD-PR-01] 進捗報告で既定ローディングの表示が更新される | `ios/Sources/KsDialogs/Presentation/LoadingCoordinator.swift:237` (`refreshBuiltinText`)、`android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/LoadingCoordinator.kt:268` | `ios/Tests/KsDialogsTests/LoadingProgressTests.swift:16`、`android/ksdialogs/src/androidTest/kotlin/jp/kamusoft/ksdialogs/LoadingProgressTests.kt:39` | ✅ 一致 |
| [LD-PR-02] 未報告のあいだはメッセージのみが表示される | `ios/Sources/KsDialogs/Presentation/LoadingCoordinator.swift:240` (`progressFormat(message, nil)`)、Android 同 `:268` | `ios/Tests/KsDialogsTests/LoadingProgressTests.swift:40`、`android/ksdialogs/src/androidTest/kotlin/jp/kamusoft/ksdialogs/LoadingProgressTests.kt:66` | ✅ 一致 |
| [LD-PR-03] 合流中は最新の報告が表示される | `ios/Sources/KsDialogs/Presentation/LoadingCoordinator.swift:181` (`latestProgress` 後勝ち)、`android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/LoadingCoordinator.kt:276` | `ios/Tests/KsDialogsTests/LoadingProgressTests.swift:59`、`android/ksdialogs/src/androidTest/kotlin/jp/kamusoft/ksdialogs/LoadingProgressTests.kt:88` | ✅ 一致 |
| [LD-PR-04] 範囲外・非有限の報告値の扱い | `ios/Sources/KsDialogs/Presentation/LoadingCoordinator.swift:179`-`:180` (非有限は無視・0〜1 にクランプ)、`android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/LoadingCoordinator.kt:276` | `ios/Tests/KsDialogsTests/LoadingProgressTests.swift:99`、`android/ksdialogs/src/androidTest/kotlin/jp/kamusoft/ksdialogs/LoadingProgressTests.kt:139` | ✅ 一致 |
| [LD-PR-05] 進捗受け口を実装した VM のカスタム View に進捗が転送される | `ios/Sources/KsDialogs/Presentation/LoadingCoordinator.swift:183`、`android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/LoadingCoordinator.kt:268` | `ios/Tests/KsDialogsTests/LoadingProgressTests.swift:134` / `:201`、`android/ksdialogs/src/androidTest/kotlin/jp/kamusoft/ksdialogs/LoadingProgressTests.kt:179` | ✅ 一致 |
| [LD-PR-06] 受け口未実装の VM では転送されない | `ios/Sources/KsDialogs/Presentation/LoadingCoordinator.swift:183` (`as?` の任意準拠)、Android 同 | `ios/Tests/KsDialogsTests/LoadingProgressTests.swift:170`、`android/ksdialogs/src/androidTest/kotlin/jp/kamusoft/ksdialogs/LoadingProgressTests.kt:222` | ✅ 一致 |

### Requirement: 既定ローディングのスタイル

実装の据え付け: `ios/Sources/KsDialogs/Contract/LoadingStyle.swift:11` (値オブジェクト、`:49` に既定フォーマット) +
`ios/Sources/KsDialogs/Presentation/LoadingDefaultContentView.swift`。
Android `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/LoadingStyle.kt:23` (`:37` 既定フォーマット) +
`android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/LoadingDefaultContentView.kt`。
一括設定の口はシングルトンの `style` プロパティのみ (`ios/Sources/KsDialogs/Presentation/KsLoading.swift:15` /
`android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/KsLoading.kt:20`)。表示 API にスタイル引数は無い (負の検査で固定)。

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| [LD-ST-01] スタイル変更は次の表示から効く | `ios/Sources/KsDialogs/Presentation/LoadingCoordinator.swift:122` / `:126` (開始時に読んで `displayedStyle` に固定)、`android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/LoadingCoordinator.kt:192` | `ios/Tests/KsDialogsTests/LoadingStyleTests.swift:14`、`android/ksdialogs/src/androidTest/kotlin/jp/kamusoft/ksdialogs/LoadingStyleTests.kt:36` | ✅ 一致 |
| [LD-ST-02] メッセージ未指定なら既定メッセージが表示される | `ios/Sources/KsDialogs/Presentation/LoadingCoordinator.swift:239` (`latestMessage ?? displayedStyle.defaultMessage`)、Android 同等 | `ios/Tests/KsDialogsTests/LoadingStyleTests.swift:52`、`android/ksdialogs/src/androidTest/kotlin/jp/kamusoft/ksdialogs/LoadingStyleTests.kt:73` | ✅ 一致 |
| [LD-ST-03] フォーマット関数の差し替えが進捗表示に反映される | `ios/Sources/KsDialogs/Presentation/LoadingCoordinator.swift:240`、`android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/LoadingStyle.kt:28` | `ios/Tests/KsDialogsTests/LoadingStyleTests.swift:67`、`android/ksdialogs/src/androidTest/kotlin/jp/kamusoft/ksdialogs/LoadingStyleTests.kt:91` | ✅ 一致 |

既定値と見えの確定 (白・14・bold・行間 8・間隔 20) は `ui/brief.md` の追記に記録され、
`ui/verification/` の照合画像 4 枚 (iOS / Android × 状態2) でオーナー最終承認済み。

### Requirement: カスタム View 版の登録と表示

実装の据え付け: `ios/Sources/KsDialogs/Registry/LoadingViewRegistry.swift:11` (Dialog と独立、`:23` UIKit / `:37` SwiftUI) +
`ios/Sources/KsDialogs/Registry/LoadingViewFactory.swift`。
Android `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/LoadingViewRegistry.kt:14` (`:28` 従来 View 系) +
`android/ksdialogs-compose/src/main/kotlin/jp/kamusoft/ksdialogs/compose/ComposeLoadingRegistration.kt` (Compose 系)。

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| [LD-CV-01] 登録済み VM でカスタム Loading が表示される | `ios/Sources/KsDialogs/Registry/LoadingViewRegistry.swift:23`、`android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/LoadingViewRegistry.kt:28` | `ios/Tests/KsDialogsTests/LoadingCustomViewTests.swift:18`、`android/ksdialogs/src/androidTest/kotlin/jp/kamusoft/ksdialogs/LoadingCustomViewTests.kt:47` | ✅ 一致 |
| [LD-CV-02] 宣言的 UI 系の登録でも同じに働く | `ios/Sources/KsDialogs/Registry/LoadingViewRegistry.swift:37` (SwiftUI)、`android/ksdialogs-compose/src/main/kotlin/jp/kamusoft/ksdialogs/compose/ComposeLoadingRegistration.kt` (Compose) | `ios/Tests/KsDialogsTests/LoadingCustomViewTests.swift:42`、`android/ksdialogs-compose/src/androidTest/kotlin/jp/kamusoft/ksdialogs/compose/ComposeLoadingCustomViewTests.kt:47` | ✅ 一致 |
| [LD-CV-03] インライン factory 表示はレジストリを変えない | `ios/Sources/KsDialogs/Presentation/Loading.swift:145` (`beginInlineUse`)、`android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/Loading.kt:92` | `ios/Tests/KsDialogsTests/LoadingCustomViewTests.swift:84`、`android/ksdialogs/src/androidTest/kotlin/jp/kamusoft/ksdialogs/LoadingCustomViewTests.kt:71` | ✅ 一致 |
| [LD-CV-04] 未登録 VM の表示は構成ミスとして失敗し action は実行されない | `ios/Sources/KsDialogs/Presentation/LoadingCoordinator.swift:293` / `:298` (`beginUse` の前段で throw)、`android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/LoadingCoordinator.kt:193` / `:217` | `ios/Tests/KsDialogsTests/LoadingCustomViewTests.swift:116`、`android/ksdialogs/src/androidTest/kotlin/jp/kamusoft/ksdialogs/LoadingCustomViewTests.kt:101` | ✅ 一致 |
| [LD-CV-05] Dialog と Loading のレジストリは独立している | `ios/Sources/KsDialogs/Registry/LoadingViewRegistry.swift:13` (Dialog とは別の `shared`)、`android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/LoadingViewRegistry.kt:50` | `ios/Tests/KsDialogsTests/LoadingCustomViewTests.swift:137`、`android/ksdialogs/src/androidTest/kotlin/jp/kamusoft/ksdialogs/LoadingCustomViewTests.kt:126` | ✅ 一致 |
| [LD-CV-06] View は表示のたびに生成される | `ios/Sources/KsDialogs/Presentation/LoadingCoordinator.swift:276` (`makeCustomContent` を開始のたびに呼ぶ)、`android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/LoadingCoordinator.kt:298` | `ios/Tests/KsDialogsTests/LoadingCustomViewTests.swift:182`、`android/ksdialogs/src/androidTest/kotlin/jp/kamusoft/ksdialogs/LoadingCustomViewTests.kt:176` | ✅ 一致 |

VM 契約の参照型限定は `ios/Sources/KsDialogs/Contract/LoadingViewModel.swift:9` /
`android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/LoadingViewModel.kt` と負の検査で担保されている。

### Requirement: 出入りの演出

実装の据え付け: 添付スロットは Dialog と同じ `DialogTransition`。実行部は Dialog 器から切り出した
`ios/Sources/KsDialogs/Presentation/DialogTransitionRunner.swift` /
`android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/DialogTransitionRunner.kt` (tasks 1.1 / 1.3) を
Loading 器 (`ios/Sources/KsDialogs/Presentation/LoadingContainerViewController.swift:267` /
`android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/LoadingContainer.kt:176`) が使う。

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| [LD-TR-01] カスタム View の添付演出フックが実行される | `ios/Sources/KsDialogs/Presentation/LoadingContainerViewController.swift:86` (`resolvedTransition`) / `:158` (`dismiss`)、`android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/LoadingContainer.kt:114` | `ios/Tests/KsDialogsTests/LoadingTransitionTests.swift:17`、`android/ksdialogs/src/androidTest/kotlin/jp/kamusoft/ksdialogs/LoadingTransitionTests.kt:45` | ✅ 一致 |
| [LD-TR-02] 未添付なら既定のトランジションで出入りする | 同上 (未添付時はライブラリ既定へフォールバック) | `ios/Tests/KsDialogsTests/LoadingTransitionTests.swift:55`、`android/ksdialogs/src/androidTest/kotlin/jp/kamusoft/ksdialogs/LoadingTransitionTests.kt:94` | ✅ 一致 |
| [LD-TR-03] 覆いは別レイヤでフェードする | `ios/Sources/KsDialogs/Presentation/LoadingContainerViewController.swift:81` (`overlayView`)、`android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/LoadingContainer.kt:66` | `ios/Tests/KsDialogsTests/LoadingTransitionTests.swift:89`、`android/ksdialogs/src/androidTest/kotlin/jp/kamusoft/ksdialogs/LoadingTransitionTests.kt:133` | ✅ 一致 |

---

## 2. ios-native

### Requirement: Swift 公開面の Loading

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| [LD-IO-01] 公開面の正の compile 検査 | `ios/Sources/KsDialogs/Presentation/KsLoading.swift:10`〜`:180`、`ios/Sources/KsDialogs/Presentation/Loading.swift:11`、`ios/Sources/KsDialogs/Contract/LoadingStyle.swift:11`、`ios/Sources/KsDialogs/Registry/LoadingViewRegistry.swift:23` / `:37`、`ios/Sources/KsDialogs/Contract/LoadingProgressReceiver.swift:7` | `ios/Tests/KsDialogsTests/LoadingApiSurfaceCompileChecks.swift:52`〜`:168` (8 箇所で show / hide / setMessage / 値返し start + 進捗 / placement / LoadingStyle 一括設定 / UIKit・SwiftUI 登録 / インライン / 受け口準拠を網羅) | ✅ 一致 |
| [LD-IO-02] SwiftUI 登録のカスタム Loading が UIKit 登録と同じに働く | `ios/Sources/KsDialogs/Registry/LoadingViewRegistry.swift:37` | `ios/Tests/KsDialogsTests/LoadingCustomViewTests.swift:206` | ✅ 一致 |

---

## 3. android-native

### Requirement: Kotlin 公開面の Loading

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| [LD-AN-01] 公開面の正の compile 検査 | `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/KsLoading.kt:15`、`android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/Loading.kt:17`、`android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/LoadingStyle.kt:23`、`android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/LoadingViewRegistry.kt:28`、`android/ksdialogs-compose/src/main/kotlin/jp/kamusoft/ksdialogs/compose/ComposeLoadingRegistration.kt` | `android/api-surface-check/src/main/kotlin/jp/kamusoft/ksdialogs/apicheck/LoadingApiSurfaceChecks.kt:46`〜`:151` (9 箇所) | ✅ 一致 |
| [LD-AN-02] 本体モジュールは Loading 追加後も Compose に依存しない | Compose 系の登録・表示は `android/ksdialogs-compose/src/main/kotlin/jp/kamusoft/ksdialogs/compose/ComposeLoadingRegistration.kt` / `ComposeLoadingShow.kt` にのみ存在 | `android/ksdialogs/src/test/kotlin/jp/kamusoft/ksdialogs/LoadingModuleBoundaryTests.kt:16` (依存グラフ走査) | ✅ 一致 |

器が Loading 専用の全画面透過 Window であることは
`android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/LoadingContainer.kt:37`、
Activity 再生成時の再取り付けは `LoadingCoordinator.kt:333` + `ResumedActivityTracker.kt` (deviation の付随修正)。

---

## 4. maui-binding

### Requirement: C# 公開面の Loading

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| [LD-MA-01] 公開面の正の compile 検査 | `maui/KsDialogs.Maui/Presentation/IKsLoading.cs:24` (`:51` ShowAsync / `:90` HideAsync / `:98` SetMessage / `:110`・`:122` 両形の StartAsync)、`maui/KsDialogs.Maui/Presentation/Loading.cs:19` (`:45` Instance / `:51` Style / `:63` Options)、`maui/KsDialogs.Maui/Contract/LoadingStyle.cs`、`maui/KsDialogs.Maui/Registry/LoadingViewRegistry.cs`、`maui/KsDialogs.Maui/Contract/LoadingProgressReceiver.cs` | `maui/KsDialogs.Maui.ApiSurfaceCheck/DialogLoadingApiSurfaceChecks.cs:33`〜`:136` (11 箇所) | ⚠️ deviation 記録済み (`DialogOptions` を public 化 — `maui/KsDialogs.Maui/Internals/DialogOptions.cs`、`deviation.md` 4行目) |
| [LD-MA-02] DI 糖衣で登録したカスタム Loading が表示される | `maui/KsDialogs.Maui/Hosting/KsDialogsServiceCollectionExtensions.cs:110` (`RegisterForLoading<TView, TViewModel>`) | `maui/KsDialogs.Maui.Tests/LoadingDependencyInjectionTests.cs:26` / `:47` / `:71` | ✅ 一致 |
| [LD-MA-03] 属性・スタイル・進捗が値のまま Native へ届く | `maui/KsDialogs.Maui/Internals/LoadingGateway.cs`、`maui/KsDialogs.Maui/Internals/LoadingActionRunner.cs`、`maui/KsDialogs.Maui/Platforms/Android/PlatformLoadingGateway.cs` / `maui/KsDialogs.Maui/Platforms/iOS/PlatformLoadingGateway.cs`、bridge は `maui/android/native/ksdialogs-maui-bridge/src/main/kotlin/jp/kamusoft/ksdialogs/maui/MauiLoadingBridge.kt` / `maui/macios/native/KsDialogsMauiBridge/MauiLoadingBridge.swift` | `maui/KsDialogs.Maui.Tests/LoadingPassthroughTests.cs:25`〜`:169` (placement / style / options / 進捗 / 受け口の供給)、`maui/KsDialogs.Maui.Tests/LoadingActionRunnerTests.cs:22`〜`:131` (終了と最終報告の順序)、`maui/android/native/ksdialogs-maui-bridge/src/test/kotlin/jp/kamusoft/ksdialogs/maui/MauiLoadingPassthroughTests.kt` / `MauiLoadingCompletionReportTests.kt` | ⚠️ deviation 記録済み (Swift bridge の写しに自動テストが無い — `deviation.md` 11行目の「制約の記録」) |

MAUI 層が合流状態を持たないこと (design Decision 8) は `maui/KsDialogs.Maui/Internals/LoadingGateway.cs` が
Native gateway へ委譲するだけの構造であること、および `LoadingFacadeTests.cs` の契約テスト群で確認できる。

---

## 5. kmp-facade

### Requirement: 共有コードからの Loading 呼び出し

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| [LD-KM-01] 共有コードの start が両 OS で表示・進捗・終了まで到達する | `kmp/ksdialogs-kmp/src/commonMain/kotlin/jp/kamusoft/ksdialogs/kmp/KsLoading.kt:17` (`:28` show / `:47` hide / `:54` setMessage / `:67`・`:83` start) + `kmp/ksdialogs-kmp/src/commonMain/kotlin/jp/kamusoft/ksdialogs/kmp/Loading.kt:10` (`expect object`)、actual は `kmp/ksdialogs-kmp/src/androidMain/kotlin/jp/kamusoft/ksdialogs/kmp/AndroidLoadingGateway.kt` / `kmp/ksdialogs-kmp/src/iosMain/kotlin/jp/kamusoft/ksdialogs/kmp/IosLoadingGateway.kt` (iOS は `ios/Sources/KsDialogs/Interop/KsDialogsInteropLoadingBridge.swift` 経由) | `kmp/ksdialogs-kmp/src/commonTest/kotlin/jp/kamusoft/ksdialogs/kmp/LoadingSharedLayerCallTests.kt:25` / `:37`、`kmp/ksdialogs-kmp/src/androidHostTest/kotlin/jp/kamusoft/ksdialogs/kmp/AndroidLoadingGatewayContractTests.kt:127`、`kmp/ksdialogs-kmp/src/iosTest/kotlin/jp/kamusoft/ksdialogs/kmp/InteropLoadingBridgeContractTests.kt:47` / `:61`、`ios/Tests/KsDialogsTests/KsLoadingKmpTests.swift:21` | ✅ 一致 |
| [LD-KM-02] commonMain の公開面にスタイル型が存在しない | commonMain には `LoadingStyle` / options 型を置いていない (`kmp/ksdialogs-kmp/src/commonMain/kotlin/jp/kamusoft/ksdialogs/kmp/` に該当ファイルなし) | 正: `kmp/api-surface-check/src/commonMain/kotlin/jp/kamusoft/ksdialogs/kmp/apicheck/LoadingApiSurfaceChecks.kt:43` / `:49` / `:56`。負: `kmp/api-surface-check/src/negativeCheckLoadingStyleType/` / `negativeCheckLoadingStyleProperty/` (`kmp/api-surface-check/build.gradle.kts:52`-`:53` で配線) | ✅ 一致 |

### Requirement: 共有 VM によるカスタム Loading

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| [LD-KM-03] 共有 VM のカスタム Loading が両 OS で通る | `kmp/ksdialogs-kmp/src/commonMain/kotlin/jp/kamusoft/ksdialogs/kmp/KsLoading.kt:40` / `:83` (VM 受けの show / start)、`kmp/ksdialogs-kmp/src/commonMain/kotlin/jp/kamusoft/ksdialogs/kmp/LoadingViewModel.kt:12` / `LoadingProgressReceiver.kt:13` (expect)、Android は `kmp/ksdialogs-kmp/src/androidMain/.../LoadingViewModel.android.kt` の typealias、iOS は `ios/Sources/KsDialogs/Kmp/KsLoadingKmp.swift` + `ios/Sources/KsDialogs/Kmp/KmpLoadingViewModel.swift` | `kmp/ksdialogs-kmp/src/commonTest/kotlin/jp/kamusoft/ksdialogs/kmp/LoadingSharedLayerCallTests.kt:48` / `:60`、`kmp/ksdialogs-kmp/src/androidHostTest/kotlin/jp/kamusoft/ksdialogs/kmp/AndroidLoadingGatewayContractTests.kt:142` / `:153` / `:191`、`kmp/ksdialogs-kmp/src/iosTest/kotlin/jp/kamusoft/ksdialogs/kmp/InteropLoadingBridgeContractTests.kt:70` / `:91`、`ios/Tests/KsDialogsTests/KsLoadingKmpTests.swift:38` / `:76` | ✅ 一致 |
| [LD-KM-04] iOS Swift パッケージ KMP 面の Loading 登録の compile 検査 | `ios/Sources/KsDialogs/Kmp/KsLoadingKmp.swift` (型付き facade、`Loading.shared.kmp` で到達 — `ios/Sources/KsDialogs/Presentation/Loading.swift:20`) | `ios/Tests/KsDialogsTests/KmpLoadingApiSurfaceCompileChecks.swift:29`〜`:70` (6 箇所) | ✅ 一致 |

---

## 6. samples

Sample 専用 Scenario (LD-SA-*) は `scripts/scenario-id-coverage.py:72`-`:74` の allow-missing に登録され
(tasks 7.1)、`verification/` の実機・シミュレータ証跡が対応物になる。

| Scenario | 実装 | 証跡 | 状態 |
|---|---|---|---|
| [LD-SA-01] Default Loading の通し | 文言: `samples/ios/KsDialogsSample/SampleText.swift:36`-`:38`・`:128` / `samples/android/app/src/main/kotlin/jp/kamusoft/ksdialogs/samples/android/SampleText.kt:56`-`:62`・`:161` / `samples/kmp/shared/src/commonMain/kotlin/jp/kamusoft/ksdialogs/samples/kmp/SampleText.kt:56`-`:62`・`:161` / `samples/maui/KsDialogs.Sample.Maui/SampleText.cs:55`-`:61`・`:160`。導線: `samples/ios/KsDialogsSample/SampleMenuModel.swift:122` / `samples/android/.../MainActivity.kt` / `samples/kmp/shared/.../SamplePresenter.kt:125` / `samples/maui/KsDialogs.Sample.Maui/SampleMenuPage.xaml.cs:97` | `verification/sample-walkthrough/notes.md` + `*-02`〜`*-04` (6 組) | ✅ 一致 |
| [LD-SA-02] Custom Loading の通し | VM / View / 登録: `samples/ios/KsDialogsSample/CustomLoadingViewModel.swift` + `CustomLoadingCard.swift` + `SampleLoadingRegistration.swift` / `samples/android/app/src/main/kotlin/jp/kamusoft/ksdialogs/samples/android/CustomLoadingViewModel.kt` + `CustomLoadingCardView.kt` + `SampleLoadingRegistration.kt` / `samples/maui/KsDialogs.Sample.Maui/CustomLoadingViewModel.cs` + `CustomLoadingCardView.xaml` (DI 1行登録は `samples/maui/KsDialogs.Sample.Maui/MauiProgram.cs`) / `samples/kmp/shared/src/commonMain/kotlin/jp/kamusoft/ksdialogs/samples/kmp/CustomLoadingViewModel.kt` + 各 OS 側 `SampleLoadingRegistration` | `verification/sample-walkthrough/notes.md` + `*-06`〜`*-07` (6 組)、モック照合は `ui/verification/*-sample-custom-*.png` と `ui/brief.md` | ✅ 一致 |
| [LD-SA-03] 4ルートで同一デモが動く | 上記4ルート分 | `verification/sample-walkthrough/notes.md` の観察表 (8 観察点 × 6 組すべて ✓) | ✅ 一致 |

---

## 7. 追加検査

### tasks.md の虚偽チェック

全 23 タスクが `[x]`。対応表と突き合わせて、実体の無いチェックは見つからなかった。

- 1.1〜1.4 (共有部品の切り出し): `ios/Sources/KsDialogs/Presentation/DialogTransitionRunner.swift`、`ios/Sources/KsDialogs/Layout/DialogLayoutApplier.swift`、`android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/DialogTransitionRunner.kt`、`android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/DialogLayoutHost.kt` が実在し、既存テスト (PB / MB 系) が全通過
- 2.1〜2.8 / 3.1〜3.8: 上記 1〜3 節の対応表がすべて実体を指す
- 4.1〜4.3 / 5.1〜5.4 / 6.1〜6.3: 同 4〜6 節
- 7.1: `scripts/scenario-id-coverage.py:81` の `MIRROR_AREAS` が `(prefix, 領域)` の組になっており、`--selftest` に「同じ領域名でも接頭辞が対象外なら検査しない」の判定が入っている
- 7.2: 負の検査 4 本を新設 (`android/api-surface-check/build.gradle.kts:69`-`:70`、`kmp/api-surface-check/build.gradle.kts:52`-`:53`、`maui/KsDialogs.Maui.ApiSurfaceCheck/KsDialogs.Maui.ApiSurfaceCheck.csproj:77`・`:81`)

### 逆流検査 (足場アーティファクトの書き換え)

- `git log -- kasane/changes/add-loading/` は起票コミット `c93795f` の 1 件のみ
- 作業ツリーの差分は `tasks.md` と `ui/brief.md` の 2 ファイルだけ。`proposal.md` / `design.md` / `specs/` 6 ファイルは無変更
- `tasks.md` の差分は行を正規化すると全行が 1:1 で対応し、変わっているのはチェックボックスのみ (本文の書き換えなし)
- `ui/brief.md` は追記のみ (113 行、削除 0)。内容は照合結果・確定した既定値・オーナー最終承認の記録で、承認モックの記録 (案A 採用) は起票時のまま

**逆流なし。**

### 未記録乖離の洗い出し

対応表に ❌ は無い。Loading 面以外に触れている差分をすべて洗い、`deviation.md` または tasks の
先行リファクタリング (1.1〜1.4) / 検査基盤 (7.1〜7.2) / samples Requirement のいずれかに帰属することを確認した。

| 差分 | 帰属 |
|---|---|
| `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/ResumedActivityProvider.kt` / `ResumedActivityTracker.kt` | `deviation.md` [付随修正] 2行目 |
| `maui/KsDialogs.Maui/Platforms/{Android,iOS}/PlatformDialogGateway.cs` + 新規 `PlatformDialogContent.cs` | `deviation.md` [付随修正] 3行目 |
| `maui/KsDialogs.Maui/Internals/DialogOptions.cs` (public 化) | `deviation.md` 4行目 |
| `samples/maui/README.md` | `deviation.md` [付随修正] 5行目 |
| `ios/Tests/KsDialogsTests/DialogTransitionTests.swift` | `deviation.md` [付随修正] 6行目・12行目 |
| `ios/Tests/KsDialogsTests/LoadingStartFailureTests.swift` (新規) | `deviation.md` [付随修正] 7行目 |
| `maui/KsDialogs.Maui/Internals/LoadingActionRunner.cs` (新規) | `deviation.md` [付随修正] 9行目 |
| `ios/Sources/KsDialogs/Presentation/LoadingReportQueue.swift` (新規) | `deviation.md` [付随修正] 13行目 |
| `ios/Sources/KsDialogs/Presentation/DialogContainerViewController.swift`、`ios/Sources/KsDialogs/Layout/DialogLayout.swift`、`android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/{DialogContainer,DialogLayoutHost,DialogLayoutSnapshot}.kt` + 新規 Runner / Applier | tasks 1.1〜1.3 (先行リファクタリング。design Decision 4) |
| `scripts/scenario-id-coverage.py`、`android/api-surface-check/build.gradle.kts`、`kmp/api-surface-check/build.gradle.kts`、`maui/KsDialogs.Maui.ApiSurfaceCheck/*.csproj` | tasks 7.1 / 7.2 |
| `maui/macios/KsDialogs.Binding.iOS/ApiDefinition.cs`、`maui/KsDialogs.Maui/Hosting/KsDialogsServiceCollectionExtensions.cs` | tasks 4.1 / 4.2 (LD-MA-01 / LD-MA-02) |
| `samples/README.md`、`samples/{android,ios,kmp}/README.md`、各ルートの `SampleText` / メニュー / 起動配線 | samples Requirement (2 デモ項目の追加そのもの) |

**未記録乖離なし。**

### UI 変更の記録

- `ui/brief.md` に承認モックの記録あり — 既定ローディングは `mock/default-plain.html` (案A) を `approved.png` として
  2026-08-25 オーナー承認、Sample の Custom Loading View は `mock/sample-custom.html` を
  `approved-sample-custom.png` として同日承認 (起票時のまま無変更)
- 合意済み妥協も記録済み — 既定ローディング: 行間 8pt / bold (オーナー指示、最終承認取得済み)、
  Android の OS 標準大インジケータの寸法差、Sample カスタムカードの影なし。いずれも `ui/brief.md` に
  4 観点の照合表と一緒に残っている
- 照合画像は `ui/verification/` に 8 枚 (iOS / Android × 既定・カスタム × 状態2)。個人要素なしの確認と
  Android のステータスバー切り落としも記録されている

### テストの実行

すべて本検証で実測した (前サイクルの結果の引き写しではない)。

| ビルドルート | コマンド | 結果 |
|---|---|---|
| ios/ | `xcodebuild test -scheme KsDialogs -destination 'platform=iOS Simulator,name=iPhone 17'` | **212 passed / 0 failed** (`** TEST SUCCEEDED **`、23.9 秒) |
| android/ (unit) | `./gradlew test --rerun-tasks` | **67 / 0 failures** (BUILD SUCCESSFUL) |
| android/ (instrumented, Pixel 4a) | `ANDROID_SERIAL=<Pixel 4a> ./gradlew connectedDebugAndroidTest` | `:ksdialogs` **188 (skipped 1)** + `:ksdialogs-compose` **35** = **223 / 0 failures** (6分17秒) |
| kmp/ | `./gradlew allTests --rerun-tasks` | iosSimulatorArm64 42 + androidHostTest 38 = **80 / 0 failures** |
| maui/ | `dotnet test` | **110 passed / 0 failed / 0 skipped** |
| maui/android/native/ | `./gradlew :ksdialogs-maui-bridge:test --rerun-tasks` | **25 / 0 failures** |

合計 **713 件 / 失敗 0**。`android/` 系の Gradle は build ディレクトリの取り合いを避けて逐次実行した。
件数は review-003 の実測と一致する (instrumented の 188 は skipped 1 を含む数え方の違い)。

補助検査:

- `python3 scripts/scenario-id-coverage.py` → 141/150 (除外 9 件)、未網羅なし
- `python3 scripts/scenario-id-coverage.py --require-mirror` → 対象領域の ID はすべて iOS / Android の双方にある
- `python3 scripts/scenario-id-coverage.py --selftest` → 全件 OK
- `scripts/local-path-lint.py` / `scripts/identity-lint.py` / `scripts/comment-policy-lint.py` → いずれも 0 件 (comment-policy は 739 ファイル)
- 負の検査 (本変更の新設 4 本のうち MAUI の 2 本を実測): `KsDialogsNegativeCheckLoadingShowStyle` / `...LoadingShowOptions` はどちらも `CS1739` (`'style'` / `'options'` という名前のパラメーターがありません) でビルド失敗 — 期待どおり

---

## 8. 判定

**VALID**

- 全 43 Scenario が「✅ 一致」または「⚠️ deviation 記録済み」(⚠️ は 3 件 — LD-CO-11 / LD-MA-01 / LD-MA-03、いずれも `deviation.md` に合意済み)
- ❌ (未記録の欠落・乖離) は 0 件
- tasks.md の虚偽チェックなし (全 23 タスクに実体あり)
- 足場アーティファクト (proposal / design / specs) への逆流なし
- 6 ルート 713 件のテストが全通過 (失敗 0)。Scenario 網羅・両 Native ミラー・3 種の lint・負の検査もすべて通過
- UI 変更の承認モックと合意済み妥協が `ui/brief.md` に記録済み (オーナー最終承認取得済み)

### 参考: 判定に影響しない観察

- Scenario **LD-CO-11** の題名は「旧世代の遅延進捗・**メッセージ**は新しい表示に届かない」のままだが、
  実装とテストが遮断するのは進捗のみ。`deviation.md` 1行目で合意済みのため違反としないが、
  蒸留 (`ksn-distill`) で concepts へ写す際は spec の文言ではなく deviation の側を正として書くのが安全
- `verification/loading-front/notes.md` が自ら記す限界 (iOS の 2 枚目が別の通しのコマ) は証跡側で
  開示済みであり、LD-AT-04 は両 OS の自動テスト (`LoadingAttributeTests`) でも押さえられている
