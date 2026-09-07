# レビュー結果: add-presentation-behavior (001 回目)

**日付**: 2026-08-21
**判定**: CHANGES_REQUESTED

## サマリー

全ビルドルート (ios 129 / android unit 50 / android instrumented 138 (Pixel 6a・API 36、`assumeTrue` スキップ 1) / kmp 51 / maui 62 / maui bridge 15) と新規の負のコンパイル検査 9 本、comment-policy lint (禁止 0)、Scenario ID 網羅 (58/61、PB-SM 3 件は既定除外) をすべて自分で回して green を確認した。状態機械・ラッチと配送・脱出口はデルタスペックの 29 本の PB-TR Scenario が両 Native で実テストされており (iOS は実 UIWindow へ載せ、Android は実機の instrumented)、門つきフック・失敗フック・完了しないフックを使った本物の契約テストで、即完了ダブルによる素通しは見当たらない。MAUI ブリッジの completion も両側 (Native `MauiDialogSingleCompletion` / C# `DialogSingleCompletion`) で 1 回に切り詰められており、寿命も中身と揃っている。

指摘は Major 1 件のみで、実装の欠陥ではなく**公開 API 表面の非対称**である — iOS だけ design Decision 3 の完全シグネチャに無い `public static let defaultDuration` が露出しており、Android / MAUI は同じ定数を internal に保っている。一般公開予定のライブラリで API を凍結する前に直しておきたい種類のずれなので CHANGES_REQUESTED とした。修正は 1 キーワードで済む。残りは Minor 2 / Suggestion 2。

## 確認した観点 (指摘に至らなかったもの)

- **仕様充足**: 61 Scenario のうち自動検査対象 58 件がテスト名の ID で突合でき、tasks.md 36 件に虚偽チェックなし (1.5 のスクリプト・5.2 の実機証跡・8.4 の連写証跡まで実在を確認)。足場 (proposal / design / specs) の書き換えなし
- **deviation.md**: 記録済みの 2 件 (iOS factory の `@MainActor` 化 / iOS の attached 中の alpha 同期復帰) 以外に無断の仕様逸脱は見つからなかった。実装メモ 8 件も実物と一致
- **状態機械の並行性**: `beginDismissal` / `finishRemoval` / `handleSettled` / `handleCallerCancellation` の多重進入はすべて状態ガードで潰されている。`DialogResultChannel` のラッチは両 OS ともロック下で 1 回きり、`cancelFromCaller` が確定済みでも観察者だけ呼ぶ形も PB-TR-29 の要求どおり。Kotlin の `runHook` が `CancellationException` を再送出しても、子の CancellationException は親 (`coroutineScope`) を巻き込まないため `finishRemoval` に到達する — 宙吊り経路は作られていない
- **公開面**: Android で新規に増えた public は `View.ksDialogTransition` と `DialogTransition` / `DialogTransitionEdge` のみ、MAUI も `DialogTransition` / `DialogTransitionEdge` / `Dialog.TransitionProperty` (Get/Set) のみで design 表と一致。`DialogContainerState` / `DialogDismissalOrigin` / `DialogTransitionAnimator` / `DialogContentHost` / `DialogPresentationCompletion` / `DialogTransitionRunner` はいずれも internal
- **負の検査**: 新規 9 本 (iOS / Android / MAUI × show 引数・DialogOptions・none 引数) を 1 本ずつ回し、すべて期待どおりの診断でビルド失敗することを確認した
- **Sample パリティ**: 文言 21 項目が 4 ルートで一致、`Custom Hook` の実装値 (80 / 300ms / easeInOut 相当) も 4 ルート一致。色は全ルート SampleTheme トークン参照で生値なし。チップの選択状態も OS 標準の機構 (`View.isSelected` / `.isSelected` trait / platform 直付け) に載っている
- **`_bridgeContent` を介した iOS 側の強参照の輪**は本変更以前から存在するもので、diff の範囲外と判断した

## 指摘事項

### [🟠 Major] iOS だけ `DialogTransition.defaultDuration` が公開されている (design Decision 3 の完全シグネチャ外)

**該当箇所**: `ios/Sources/KsDialogs/Contract/DialogTransition.swift:50-52`

```swift
public extension DialogTransition {
    /// プリセットが引数を省略したときの時間。
    static let defaultDuration: TimeInterval = 0.25
```

**問題点**:

- design Decision 3 は「公開 API は下表で固定する」と宣言し、`api-surface-check の期待値` として完全シグネチャを列挙している。その一覧に `defaultDuration` は無く、design は既定値を `duration: TimeInterval = 0.25` とリテラルで書いている
- 同じ役割の定数を Android は `internal val DEFAULT_DURATION` (`android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/DialogTransition.kt:35`)、MAUI は `internal static readonly TimeSpan DefaultDuration` (`maui/KsDialogs.Maui/Contract/DialogTransition.cs:35`) として internal に保っている。**iOS だけが非対称**であり、意図した設計判断なら deviation.md に載るはずだが記録がない
- 利用箇所は自モジュール内と `@testable import` なテスト 1 箇所 (`ios/Tests/KsDialogsTests/DialogTransitionTests.swift:473`) だけで、公開である必要がない。非 `@testable` な api-surface-check 側 (`DialogApiSurfaceCompileChecks.swift` / `DialogAttributeCompileChecks.swift`) からは参照していない
- KsDialogs は一般公開予定であり、いったん公開した定数を後から internal 化するのは破壊変更になる。`kasane/lessons/inbox/review-check-public-api-surface.md` が観測パターンとして数えている「公開 API 表面の見逃し」と同型

**推奨修正**: どちらか。

1. `public extension` の中で当該メンバーだけ明示的に `internal static let defaultDuration` にする (非 `@inlinable` な public 関数の既定引数式は internal シンボルを参照できるため、`fade` / `slide` / `zoom` / `none` の既定引数はそのままで通る)
2. design の字面どおり `duration: TimeInterval = 0.25` に戻し、定数を internal な別 extension へ移す

あわせて、`DialogApiSurfaceCompileChecks.swift` 側に「プリセットは引数省略でも作れる」正の検査はあるが公開定数の不在を固定する手段がないため、この種のずれを次回も見逃さないなら 3 形態の完全シグネチャを 1 箇所に並べた突合表 (または負の検査) を検討したい (今回の必須ではない)。

### [🟡 Minor] handoff-distill.md に記録された maui の実測件数が現在の実測とずれている

**該当箇所**: `kasane/changes/add-presentation-behavior/handoff-distill.md:24`

**問題点**: 「実測件数の更新 (MAUI 不具合修正後): maui `dotnet test` 61 / `maui/android/native` 15」とあるが、本レビューで `cd maui && dotnet test` を回した実測は **62** (`失敗: 0、合格: 62、スキップ: 0、合計: 62`)。`maui/android/native` の 15 は一致。この数値は蒸留で `kasane/concepts/cross/conventions/test-execution.md` の件数表へ写される前提で書かれている申し送りなので、ずれたまま写されると規約側が最初から不正確になる。

なお test-execution.md 本体 (件数表・「負の検査 17 本」・`kasane/concepts/cross/index.md` の「正/負17本」) が未更新であること自体は、handoff-distill.md:5・12・15 で新フラグ 9 本と期待診断・各ルートの実測件数を含めて蒸留へ明示的にルーティングされているため、本変更の欠落とは見なさない。

**推奨修正**: handoff-distill.md:24 の maui 件数を 62 へ訂正する (蒸留時に実測し直す運用なら、その旨を1行足すだけでもよい)。

### [🟡 Minor] 新規パネル操作部の読み上げについて、accessibility tree の 4 ルート証跡がない

**該当箇所**: `kasane/changes/add-presentation-behavior/verification/` / `ui/verification/` (証跡の不在)、規約は `kasane/concepts/cross/conventions/sample-parity.md:86-93`

**問題点**: samples デルタスペックの Requirement「トランジションデモ」は「操作部の読み上げは既存のパネル読み上げ規約に従う (SHALL)」と定め、その規約は「検証は accessibility tree の検査 (自動検査または実機スクリーンリーダー) で行い、**4ルートの証跡を残す**」まで含む。本変更で新設された操作部 (チップ 8 個・時間スライダ・イージングチップ 4 個・戻る記号) について、`verification/` にも `ui/verification/` にも accessibility tree の証跡がない。`ui/brief.md` の照合結果も構造/トークン/状態/意図の 4 観点 (見た目) までで、読み上げには触れていない。

実装側は 4 ルートとも確認できており正しい (Android `contentDescription` + `SampleButtonRoleDelegate` + `View.isSelected`、iOS `.accessibilityLabel` + `.accessibilityAddTraits(.isSelected)` + `.accessibilityValue`、MAUI `SemanticProperties.SetDescription` + platform 直付けの選択状態)。欠けているのは**証跡だけ**のため Minor とした。

**推奨修正**: 4 ルートで accessibility tree を撮る (Android は `uiautomator dump` / Accessibility Scanner、iOS は Accessibility Inspector、MAUI は各 platform 同様) か、実機スクリーンリーダーの読み上げ記録を `ui/verification/` へ追加する。実施しないなら、なぜ今回は先行 change の証跡で足りるのかを brief に 1 行残す。

### [🔵 Suggestion] フック未完了の警告の有効条件が iOS と Android で非対称

**該当箇所**: `ios/Sources/KsDialogs/Presentation/DialogContainerViewController.swift:451-463` / `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/DialogContainer.kt:453-456`

**問題点**: Android は**提示先アプリ**の `ApplicationInfo.FLAG_DEBUGGABLE` を見て警告するのに対し、iOS は `#if DEBUG` — つまり**ライブラリ自身のビルド構成**で決まる。現在の SPM ソース配布では利用者の Debug ビルドでライブラリも DEBUG で組まれるので実害はないが、将来プリビルドの xcframework を配る形になると、利用者が Debug でも警告が一切出なくなる。design Decision 5-8 の「デバッグビルドでは警告ログを出す」の「デバッグ」が誰のビルドかで割れる。

**推奨修正**: 現状維持で構わないが、`#if DEBUG` の直上に「ライブラリのビルド構成で決まる (ソース配布前提)」ことを 1 行残すか、配布形態が変わったら見直す旨を申し送りに足しておくと、次に触る人が非対称に気づける。

### [🔵 Suggestion] Android だけ「ミリ秒に落とすと 0 になる duration」が成立しない値に含まれる

**該当箇所**: `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/DialogTransitionAnimator.kt:24-25` / `ios/Sources/KsDialogs/Presentation/DialogTransitionAnimator.swift:13-15`

**問題点**: Android の `isAnimatable` は `isFinite() && isPositive() && inWholeMilliseconds > 0` で、0.5 ミリ秒のような正の微小値も即完了扱いになる。iOS は `duration.isFinite && duration > 0` なので同じ値で 0.5 ミリ秒のアニメーションを組む (MAUI は `(uint)TotalMilliseconds` が 0 になり実質即完了)。`concepts/core/api/transition-semantics.md:106` の成立しない値の列挙は「0・負値・NaN・無限大、および形態のアニメーション API が扱えない大きさ (MAUI の `TimeSpan` では…)」で、Android のこの条件だけ明示されていない。

実害はほぼ無く、Android 側のコメントには理由が書かれているので、直すなら実装ではなく文書側。

**推奨修正**: transition-semantics.md:106 の括弧書きに「Android では `kotlin.time.Duration` をミリ秒に落とすと 0 になる長さ」を並記する (蒸留時でよい)。

## アクションプラン

1. **[Major]** `ios/Sources/KsDialogs/Contract/DialogTransition.swift:52` の `defaultDuration` を internal 化するか、design の字面どおりリテラル既定へ戻す。Android / MAUI との対称性を取り戻す
2. **[Minor]** `handoff-distill.md:24` の maui 実測件数を 62 へ訂正
3. **[Minor]** 新規パネル操作部の accessibility tree 証跡を 4 ルート分追加する (または brief に免除理由を記録)
4. **[Suggestion]** iOS 警告の `#if DEBUG` の意味をコメント/申し送りに明示
5. **[Suggestion]** transition-semantics.md の「成立しない duration」に Android のミリ秒条件を並記 (蒸留時)

## 実行結果 (本レビューでの実測)

| ルート | コマンド | 結果 |
|---|---|---|
| ios/ | `xcodebuild test -scheme KsDialogs -destination 'platform=iOS Simulator,name=iPhone 17'` | 129 tests / 25 suites / 0 failures (TEST SUCCEEDED) |
| android/ | `./gradlew test --rerun-tasks` | 50 / 0 (xml 集計) |
| android/ instrumented | `ANDROID_SERIAL=2A141JEGR18112 ./gradlew connectedDebugAndroidTest` | 138 / 0 / skipped 1 (Pixel 6a・API 36) |
| kmp/ | `./gradlew allTests --rerun-tasks` | 51 / 0 |
| maui/ | `dotnet test` | 62 / 0 |
| maui/android/native | `./gradlew :ksdialogs-maui-bridge:test --rerun-tasks` | 15 / 0 |
| 負の検査 (新規 9 本) | iOS 3 フラグ / Android 3 フラグ / MAUI 3 フラグ | 全件が期待診断でビルド失敗 |
| lint | `python3 scripts/comment-policy-lint.py` | 禁止 0 件 / 540 ファイル |
| 網羅 | `python3 scripts/scenario-id-coverage.py` | 58/61 (除外 3)・未網羅なし |

## 補足 (指摘ではない)

`ui/brief.md` の「**オーナーの最終承認: 未取得**」は本レビューの対象外のゲートとして残っている。静止画の 4 観点照合 (4ルート × 6状態) と演出の実機確認は揃っており、before/after の提示待ちの状態と読める。
