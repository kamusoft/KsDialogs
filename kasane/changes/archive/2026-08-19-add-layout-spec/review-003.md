# レビュー結果: add-layout-spec (003 回目)

**日付**: 2026-08-19
**判定**: CHANGES_REQUESTED

## サマリー

review-002 / second-opinion-code-002 の指摘は、Major 2件・Minor 4件・Suggestion 2件のすべてがコード側で解消されている。とくに**スナップショット時点**は「読み取りを初回レイアウトパスへ寄せる」案 (推奨修正 1) が3経路すべてで採られ、iOS は `viewWillLayoutSubviews` での読み直し + `prepareForPresentation` での収束固定、Android は `DialogLayoutSnapshot` + `onMeasure` 毎回読み + `OnPreDrawListener` での固定、MAUI は `DialogPresentationContent.FreezeAttributes` + 両 platform gateway の写し直しで実装され、「完了前の変更は採用・完了後は無視」の対テストが iOS / Android / MAUI の3経路に入った。**公開 API 検査**も実利用者境界 (android / kmp / maui は friend path を持たない新規モジュール、iOS は非 `@testable` ファイル) へ移り、負の検査は禁止形状ごとに16本へ分割された — 本レビューで**16本すべてを個別に実行し、期待どおり1本ずつ別々の診断で失敗すること**を実測した。全6ビルドルートのテストも green で、退行は見当たらない。

一方で、その16本と新モジュール群を**どう回すかがどこにも書かれていない**。規約 `concepts/cross/conventions/test-execution.md` は撤去済みの旧フラグを指したままで、書かれているコマンドを実行すると**何も検証しないまま BUILD SUCCESSFUL になる** (実測済み)。同規約が自ら防ごうとしている「黙って空振りする範囲」に、本変更が新たな穴を1つ開けた形になるため Major として起票する。

### 実行した検証

| ルート | コマンド | 結果 |
|---|---|---|
| ios/ | `xcodebuild test -scheme KsDialogs -destination 'platform=iOS Simulator,id=76EB1CA1-…'` | 58 tests / 14 suites passed (XCTest 側 0 件) |
| android/ | `./gradlew test --rerun-tasks` | 40 tests / 0 failures |
| android/ (instrumented) | `./gradlew connectedDebugAndroidTest` (Pixel 4a - 13 実機) | 62 tests / 0 failed |
| kmp/ | `./gradlew allTests --rerun-tasks` | 48 tests / 0 failures (iosSimulatorArm64 26 + androidHostTest 22) |
| maui/ | `dotnet test` | 28 tests / 0 failures |
| maui/android/native/ | `./gradlew :ksdialogs-maui-bridge:test --rerun-tasks` | 9 tests / 0 failures |
| 負の検査 (禁止形状別) | ios 4本 / android 4本 / kmp 3本 / maui 5本 = **16本** | 全本が個別に BUILD FAILED。診断はソース中の「期待する診断」と一致 (後述) |
| 正の検査のビルド到達 | android `./gradlew test` → `:api-surface-check:compileDebugKotlin` 実行 / kmp `./gradlew allTests` → `:api-surface-check:compileKotlinIosSimulatorArm64` 実行 / maui `dotnet test` → `KsDialogs.Maui.ApiSurfaceCheck` ビルド / ios は既定のテストビルドに同梱 | 4ルートとも既定の実行に含まれることを確認 |
| コメント規約 lint | `python3 scripts/comment-policy-lint.py` | 禁止 0 件 / 373 ファイル |
| ケース表の同一性 | `core/layout-spec/cases.json` ↔ `specs/dialog-contract/layout-cases.json` | JSON として完全一致 (19ケース / tolerance 1.0) |
| 足場の凍結 | `git diff 5af1a1b -- kasane/changes/add-layout-spec/` | proposal / design / specs / layout-cases.json は無改変。差分は tasks.md・ui/brief.md・verification 証跡のみ |

負の検査16本の実測診断 (すべて1ビルド1エラー):

- ios: `has no member 'proportionalWidth'` / `extra argument 'options' in call` / `cannot assign value of type 'DialogResult<Bool>' to type 'DialogResult<String>'` / `cannot convert value of type 'String' to expected argument type 'Bool'`
- android: `Unresolved reference 'proportionalWidth'` / `No parameter with name 'options' found.` / `Return type mismatch` / `Argument type mismatch: actual type is 'String'`
- kmp: `Unresolved reference 'DialogOptions'` / `No parameter with name 'options' found.` / `Return type mismatch`
- maui: CS1061 / CS1739 / CS0029 / CS1503 / CS0311

deviation.md の2点 (パネルの戻る導線・パネル内結果表示) は合意済み差分として扱い、指摘に含めていない。スナップショット時点については deviation を追加せず実装側を契約へ寄せる判断が採られており、`layout-semantics.md:84-92` との整合が取れているため追加の記録は不要と判断した。

## 前回指摘の解消状況

| # | 出典 / 重要度 | 指摘 | 状況 |
|---|---|---|---|
| 1 | 双方 / 🟠 Major | 添付のスナップショット時点が契約より早い (器の構築時に読んでいる) | **解消**。推奨修正 1 (初回レイアウトパスへ寄せる) を3経路で実施。対テストも3経路に追加 |
| 2 | 相方 / 🟠 Major | 公開 API のコンパイル検査が部分的な契約違反を見逃せる | **解消**。実利用者境界へ移設 + 禁止形状別16本へ分割。16本すべて個別に実測確認 |
| 3 | ホスト / 🟡 Minor | iOS の外側タップが実タップ経路で検証されていない | **解消**。`isOutsideTap(touching:)` を hitTest 経由で検証する1本と、内側タップ非キャンセルを iOS / Android 双方に追加 |
| 4 | ホスト / 🟡 Minor | KMP Android の視覚照合が未実施のまま tasks 6.3 完了扱い | **解消**。Pixel 6a で6状態を追撮 (`ui/verification/kmp-android-*.png`) し、brief.md の照合結果に記録。乖離0件 |
| 5 | ホスト / 🟡 Minor | iOS 負のコンパイル検査が並列ビルドで空振りする | **解消 (構造的に)**。1ビルド1誤りへ分割したため空振りが起こらない。ただし手順が規約に反映されていない → 下記 Major 1 |
| 6 | ホスト / 🟡 Minor | test-execution.md の件数乖離・instrumented 手順不在 | **未解消**。加えて本変更で負の検査のコマンド自体が無効化された → 下記 Major 1 に統合 |
| 7 | ホスト / 🔵 Suggestion | MAUI `DialogOptions` が internal である根拠を残す | **解消**。`Internals/DialogOptions.cs:14-18` に理由を明記 |
| 8 | ホスト / 🔵 Suggestion | iOS 互換面の既定値にパリティテストがない | **解消 (より強い形で)**。`MauiDialogAttributes.swift:31-61` が既定値を書き写さず `DialogOptions()` / `DialogPlacement()` から引くようになり、二重定義そのものが消えた |
| 9 | 相方 / 🟡 Minor | 戻る導線のアクセシブルな名前・役割が不足 | **解消**。4ルートに名前「戻る」と役割を付与。Android 系は起動の道も開き、見た目不変を5形態で撮り直して確認 (brief.md) |

## 指摘事項

### [🟠 Major] 新しい検査機構が規約に反映されておらず、規約どおりのコマンドが黙って成功する

**該当箇所**:
- `kasane/concepts/cross/conventions/test-execution.md:78-89` (負のコンパイル検証の節)
- 同 `:6, 15, 17-23` (timestamp と実測件数の表)

**問題点**:
本変更は負のコンパイル検査の機構を全面的に置き換えた — 旧フラグ (`-DKSDIALOGS_NEGATIVE_COMPILE_CHECK` / `-Pksdialogs.negativeCompileCheck` / `-p:KsDialogsNegativeCompileCheck=true`) と旧ソースセット (`src/testNegativeCompileCheck` 等) は削除され、禁止形状別のフラグ16本と新モジュール (`android/api-surface-check` / `kmp/api-surface-check` / `maui/KsDialogs.Maui.ApiSurfaceCheck`) に置き換わっている。しかし規約の表は旧フラグのままで、**新しいフラグ名・モジュール・実行方法は kasane/ のどこにも書かれていない** (`grep -rn "api-surface-check\|negativeCheck" kasane/` は 0 件)。

その結果、規約に書かれたコマンドは現在すべて「検証ゼロで成功」する。実測:

```
$ cd android && ./gradlew compileDebugUnitTestKotlin -Pksdialogs.negativeCompileCheck
BUILD SUCCESSFUL in 915ms
$ cd maui && dotnet build KsDialogs.Maui.Tests -p:KsDialogsNegativeCompileCheck=true
    0 エラー
```

同規約は「**成功したら検証は失敗**である」と自ら定めているため、この状態では読んだ人が「負の検査を回した (そして失敗した = 何かがおかしい)」と誤読するか、あるいは成功を見て検証済みと判断する。どちらに転んでも、16本の検査は1本も走らない。review-002 の Minor 5 (件数乖離・instrumented 手順不在) が未解消のまま残っていることと合わせ、この規約は現在の実態から二重に離れている。

なお実装側は正しく、16本すべてが個別に効くことは本レビューで実測済みである。問題は**手順の記録が実装に追随していない**点に限られる。

**推奨修正**: `test-execution.md` を実測で更新する。最低限:
1. 負の検査の表を、ルート × 禁止形状の16行 (またはルート4行 + フラグ一覧) へ差し替え、各行に期待する診断を書く。新モジュールを回す対象 (`:api-surface-check:compileDebugKotlin` / `:api-surface-check:compileKotlinIosSimulatorArm64` / `dotnet build KsDialogs.Maui.ApiSurfaceCheck`) も明記する
2. **正の検査が既定の実行に含まれる**ことを1行足す (android `./gradlew test`・kmp `./gradlew allTests`・maui `dotnet test`・ios のテストビルドがそれぞれ当該モジュール/ファイルをコンパイルする。本レビューで4ルートとも確認済み)
3. `android/ (instrumented)` 行 (`./gradlew connectedDebugAndroidTest`、実機/エミュレータ接続と `adb devices` 確認が前提、結果は `ksdialogs/build/outputs/androidTest-results/connected/` の XML) を追加する。本変更の受け入れのうち「ケース表の全量検証 (実 View)」「外側タップ (Android)」「透明オーバーレイ」「スナップショット時点 (Android)」はこの 62 件にしか無く、`./gradlew test` だけでは1件も走らない
4. 件数を実測値へ更新し timestamp を改める: ios 58 / android 40 / android instrumented 62 / kmp 48 / maui 28 / maui-bridge 9

concepts の更新なので蒸留フェーズへ送る選択肢もあるが、**旧コマンドが黙って成功する状態を残したままアーカイブすると、次に負の検査を回す人が確実に空振りする**ため、本サイクル内での対処を推奨する。

### [🟡 Minor] MAUI 経路のスナップショット配線だけが自動検証も実環境確認も持たない

**該当箇所**:
- `maui/KsDialogs.Maui/Platforms/Android/PlatformDialogGateway.cs:146-169` (`ObserveFirstLayoutPass`)
- `maui/KsDialogs.Maui/Platforms/iOS/PlatformDialogGateway.cs:199-222` (`LayoutSubviews` / `RefreshAttachedAttributes`)
- `maui/KsDialogs.Maui.Tests/DialogLayoutPassthroughTests.cs:166-205`

**問題点**:
iOS Native と Android Native は「View 生成後・初回レイアウトパス完了前に届いた添付が採用される」ことを、レイアウトパスの中で添付し直す専用の中身 (`LateAttachingContentView`) を使って実 rect で検証している。MAUI 側の対応テストは `DialogPresentationContent` という値オブジェクト単体の検証で、採用時点は `delegated.FreezeAttributes()` を**テストが直接呼んで**再現している。つまり、実際に採用時点を決める配線 — Android は `ViewAttachedToWindow` / `LayoutChange` の2フック、iOS は `DialogContentView.LayoutSubviews` — は1行も実行されない。

読んだ限り配線の順序は正しい (Android は measure → layout → `LayoutChange` で写し直し → `OnPreDraw` で `hasPendingChange()` を拾って1パス回し直す、iOS は `LayoutSubviews` の写し直しを器の収束ループが拾う)。ただし、この順序が崩れても — たとえば `Refresh()` の呼び出しを落としても、`FreezeAttributes()` を `Refresh()` の前に動かしても — MAUI の28件も Native の全テストも green のままである。`verification/sample-walkthrough/notes.md` の確認1〜4 も、添付が生成時から変わらない経路しか通っていないため回帰保護にならない。

**未達ではなく回帰保護の穴**であり、MAUI の platform コードに自動テストの土台が無い (本リポジトリは device test を持たない) ことも承知のうえでの指摘である。

**推奨修正**: 次のいずれか。
1. MAUI Sample のパネルに、Show の直後 (レイアウトパスの最中) に添付プロパティを1つ書き換える一時的な経路を差し込み、その値が採用されることを iOS / Android の実機で1回だけ撮って `verification/` に残す
2. 実施しないなら、`deviation.md` ではなく検証記録の側に「MAUI 経路のスナップショット配線は静的レビューのみ」と限界を明記し、expand-api-surface へ申し送る

### [🟡 Minor] iOS の正の API 検査だけ、利用者側で宣言した ViewModel を通っていない

**該当箇所**: `ios/Tests/KsDialogsTests/DialogAttributeCompileChecks.swift:6, 24, 30`、`ios/Tests/KsDialogsTests/Support/BasicTestDialogViewModel.swift:1`

**問題点**:
android / kmp / maui の api-surface-check は、いずれも検査モジュールの中で `ConsumerDialogViewModel` を**自分で宣言**している。そのため「利用者が自分の ViewModel 型を宣言して show に渡せる」ことまでが公開面だけで証明される。

iOS の検査ファイルは `import KsDialogs` (非 `@testable`) にした点は正しいが、渡している `BasicTestDialogViewModel` は `@testable import KsDialogs` を持つ Support ファイルで宣言されている。conformance の宣言が friend 可視性の下にあるため、**利用者側から `DialogViewModel` に適合する型を宣言できること**は iOS だけ証明されていない。相方が Major 2 で挙げた「friend 可視性を通る検証」の残りがここに1点残っている。

実害の可能性は低い (公開 API の制約に internal 型を使えばライブラリ自体がコンパイルできない) が、4ルートで検査の強度をそろえる意味では安価に埋まる。

**推奨修正**: `DialogAttributeCompileChecks.swift` の中に、他3ルートと同じ `ConsumerDialogViewModel` (非 `@testable` ファイル側での `DialogViewModel` 適合) を1つ宣言し、正の検査をそれで書き直す。`DialogTypedResultCompileChecks.swift` も同じ型を使えばよい。

### [🔵 Suggestion] 収束回数の上限に達したときに何も残らない

**該当箇所**: `ios/Sources/KsDialogs/Presentation/DialogContainerViewController.swift:12-14, 120-129`、`android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/DialogLayoutHost.kt:43, 52-67, 134-141`

読むたびに値が変わり続ける供給元に対してレイアウトを止めない上限 (どちらも 4) を置いた判断は妥当で、コメントも意図をよく説明している。ただし上限に達した場合、採用される実効値は「4パス目の値」であって契約が定める「初回レイアウトパス完了時点の値」とは限らず、しかもその事実は外から一切分からない (iOS は `isLayoutSnapshotFrozen` を立てるだけ、Android は `remainingConvergencePasses` を使い切るだけ)。

現実に起こるのは供給元の実装不良のときだけなので実害はないが、上限に達したことだけでも `assertionFailure` / `Log.w` 相当で1回知らせておくと、宣言的 UI の供給機構を足す expand-api-surface で「なぜか添付が効かない」を追うときの手掛かりになる。

## アクションプラン

1. **🟠 Major (test-execution.md)** — 負の検査16本と新モジュールの回し方、正の検査が既定実行に含まれること、instrumented 行、実測件数の4点を規約へ反映する。蒸留へ送る場合も、旧フラグの記述だけは先に消す
2. **🟡 Minor (MAUI スナップショット配線)** — 実機で1回確認して証跡を残すか、限界を明記して申し送る
3. **🟡 Minor (iOS 正の検査の ViewModel)** — 非 `@testable` ファイル側に `ConsumerDialogViewModel` を1つ宣言して他3ルートとそろえる
4. **🔵 Suggestion (収束上限)** — 任意

## 確認して問題がなかった観点

- **スナップショット時点の実装 (3経路)**: iOS は `viewWillLayoutSubviews` で読み直し + `prepareForPresentation` の収束ループで固定 (`isLayoutSnapshotFrozen` 以降は読まない)。Android は `DialogLayoutSnapshot` が `onMeasure` のたびに読み直し、`OnPreDrawListener` が `hasPendingChange()` を見てパスを回し直してから `freeze()`。MAUI は `DialogPresentationContent` が読むたびに合成し、platform gateway がレイアウトの節目で写し直してから `FreezeAttributes()`。いずれも**固定は提示より前**に済み、契約の「提示後の再適用は違反」を踏んでいない
- **完了前 / 完了後の対テスト**: iOS `DialogAttributeSupplyTests.swift:111-183` (LateAttachingContentView で End/End が採用され、外側タップの扱いも false 側に従う / 完了後の変更は rect も操作も動かさない)、Android `DialogAttributeSupplyTests.kt:142-216` (静的に End/End を添付した参照の器と rect が一致することで答え合わせ)、MAUI `DialogLayoutPassthroughTests.cs:163-205`。3経路とも「構築時に固定していれば落ちる」期待値になっている
- **公開 API 検査の境界**: android / kmp は `implementation(project(...))` (friend path なし)、maui は別プロジェクト (`InternalsVisibleTo` の外)、ios は非 `@testable` の `import KsDialogs`。負の検査は禁止形状ごとにソースセット / `Compile Include` / `#if` フラグで分離され、1ビルドに1誤りしか入らない。16本すべてを個別に実行し、それぞれ**別々の診断1件**で失敗することを確認した
- **iOS 外側タップ**: `DialogOutsideTapTests.swift:48-96` が実座標 → `hitTest` → `isOutsideTap(touching:)` の経路を通し、覆いの上の点と中身の上の点で真偽が分かれることを検証。述語を反転させれば落ちる。内側タップ非キャンセルは iOS / Android 双方に1本ずつあり、Android は実入力注入 (`DialogTouchInjection.tap`) 経由
- **戻る導線の a11y**: 4ルートとも名前「戻る」を付与。iOS 系は SwiftUI の `Button` + `accessibilityLabel`、Android 系は `contentDescription` + `AccessibilityDelegate` で `className` をボタンへ差し替え、MAUI は platform 側で名前・種別に加え `ActionClick` まで開いている (`TapGestureRecognizer` が platform view を押せる状態にしないための補い。コメントに理由あり)。見た目不変は5形態の撮り直しと画素差分で確認済み
- **KMP Android の補完照合**: 6状態を追撮し、`approved-layout-panel.png` / `approved.png` と Android Native ルートの同状態に照合。画素差分で時計以外の差が無いことまで記録されており、tasks 6.3 の `[x]` は現在は正当
- **互換面の既定値**: iOS 側は Native の `DialogOptions()` / `DialogPlacement()` から引く形になり二重定義が解消。Android 側は書き写しのままだが `MauiDialogLayoutPassthroughTests.kt` のパリティテストで守られている。どちらの経路でも既定値のズレは検出できる
- **旧経路の残骸**: `DialogLayoutProviding` / `IDialogLayoutProviding` / `MauiDialogLayoutAttributes` / `LayoutTestDialogViewModel` / 旧 `*NegativeCompileCheck*` ソースセットはコードから完全に消えている (ヒットするのは ADR 本文と過去のレビュー記録のみ)
- **オブジェクト単位置換・正規化・ケース表**: 4ルートすべてに置換のテスト (フィールド合成なら落ちる期待値) があり、正規化 (比率・Offset・Margin) は iOS / Android で同一ロジック・同一期待 rect (72, 0, 280, 800)、MAUI は「丸めずに渡す」を別テストで固定。ケース表19件は凍結版と JSON レベルで完全一致し、`approvedDiff` は1件も使われていない
- **足場の凍結**: proposal / design / specs / layout-cases.json は commit 5af1a1b 以降 1バイトも変わっていない。変更アーティファクト側の差分は tasks.md のチェックと ui/brief.md への追記 (ui-artifacts.md が brief.md に書くよう定めている内容) と証跡ファイルのみ
- **tasks.md の虚偽チェック**: 1.1〜7.3 のすべてについて対応する実装・テスト・証跡を再確認した。虚偽なし
- **コメント規約**: 機械検査 0 件 / 373 ファイル。新規追加分 (収束ループ・スナップショット面・api-surface-check の build.gradle 冒頭) はいずれも ADR ID に加えて「なぜそうするか」を自然文で説明しており、ID だけに依存した説明になっていない
