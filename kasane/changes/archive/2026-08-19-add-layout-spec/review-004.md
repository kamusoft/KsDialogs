# レビュー結果: add-layout-spec (004 回目)

**日付**: 2026-08-19
**判定**: APPROVED

## サマリー

修正サイクル3の対象だった6件 — 相方 round 2 の Major 2件 (iOS Native の window 搭載前固定 / MAUI iOS の固定値未転送)、review-003 の Major 1件 (test-execution.md の実態乖離)・Minor 2件・Suggestion 1件 — は**すべて解消**している。とくにスナップショット時点は、iOS Native が `DialogContainerRootView.didMoveToWindow` を起点に「画面に載ったあとのパスを走らせ切ってから固定」へ移り、`prepareForPresentation` は提示前サイズ確定のための暫定パスだけを残す形になった。両者は両立しており、新テストが「提示時点では暫定値 (0,0) / 画面上の初回パスで届いた End/End が採用」を同一テスト内で押さえている。MAUI は `DialogAttributeSnapshotRelay` で 暫定転送 → Arrange → Freeze → **固定値の再転送** の順に是正され、iOS 側は `Window is not null` のときだけ固定するようになった。

`test-execution.md` は**書かれたコマンドを本レビューで全数実行し、そのまま機能することを確認**した — 負の検査16本はすべて個別に BUILD FAILED になり、表に書かれた診断1件と一致した。正の検査が既定実行に含まれることも4ルートで確認し、実測件数6行はすべて表と一致した (ios 59 / android 40 / instrumented 62 / kmp 48 / maui 31 / maui-bridge 9)。付随修正2件 (ObjC 互換面の `applyAttributes` インスタンス化 / 正規化済み `DialogLayout` による NaN 変更検知の空回り解消) も妥当で、後者は Swift の `Double` 等値が NaN で偽になる問題を「非有限値を畳み込んだ後の型で比較する」ことで構造的に潰しており、Kotlin 側 (data class の `Double.compare` 意味論で NaN 同値) との整合も取れている。

新規指摘は Minor 1件・Suggestion 2件で、いずれも実害のない記述・生成物のずれ。Critical / Major はない。

### 実行した検証

| ルート | コマンド (規約に書かれたまま) | 結果 |
|---|---|---|
| ios/ | `xcodebuild test -scheme KsDialogs -destination 'platform=iOS Simulator,…'` | **59 tests / 14 suites passed** (表と一致) |
| android/ | `./gradlew test --rerun-tasks` | **40 tests / 0 failures** (表と一致) |
| android/ (instrumented) | `ANDROID_SERIAL=0B261JEC216142 ./gradlew connectedDebugAndroidTest` (Pixel 4a - 13 実機) | **62 tests / 0 failed** (表と一致) |
| kmp/ | `./gradlew allTests --rerun-tasks` | **48 tests / 0 failures** (iosSimulatorArm64 26 + androidHostTest 22。表と一致) |
| maui/ | `dotnet test` | **31 tests / 0 failures** (表と一致。relay 3本の増分を含む) |
| maui/android/native/ | `./gradlew :ksdialogs-maui-bridge:test --rerun-tasks` | **9 tests / 0 failures** (表と一致) |
| 負の検査 16本 | ios 4 / android 4 / kmp 3 / maui 5 を**1本ずつ**規約記載のコマンドで実行 | **全16本が BUILD FAILED**。診断はすべて表の記述と一致 (下記) |
| 正の検査 (既定実行に含まれるか) | android `test` → `:api-surface-check:compileDebugKotlin` 実行を実測 / kmp `allTests --dry-run` に `:api-surface-check:compileKotlinIosSimulatorArm64` が含まれることを実測 / maui テスト csproj が `KsDialogs.Maui.ApiSurfaceCheck` を ProjectReference / ios は非 `@testable` 検査ファイルがテストビルドに同梱 | 4ルートとも規約の記述どおり |
| 旧フラグの残置 | `grep -rn "negativeCompileCheck\|NEGATIVE_COMPILE_CHECK\|KsDialogsNegativeCompileCheck"` | 現行の規約・コードには 0 件 (ヒットは archive 済み変更の記録と後述 Suggestion 3 のみ) |
| コメント規約 lint | `python3 scripts/comment-policy-lint.py` | 禁止 0 件 / 375 ファイル |
| ケース表の同一性 | `core/layout-spec/cases.json` ↔ `specs/dialog-contract/layout-cases.json` | JSON として完全一致 (19ケース / tolerance 1.0) |
| 足場の凍結 | `git diff 5af1a1b -- proposal.md design.md specs/` | 差分なし (1バイトも変わっていない) |

負の検査16本の実測診断 (すべて1ビルド1エラー、規約の「期待する診断」列と一致):

- ios: `has no member 'proportionalWidth'` / `extra argument 'options' in call` / `cannot assign value of type 'DialogResult<ConsumerDialogViewModel.Result>' (aka 'DialogResult<Bool>') to type 'DialogResult<String>'` / `cannot convert value of type 'String' to expected argument type 'ConsumerDialogViewModel.Result' (aka 'Bool')`
- android: `Unresolved reference 'proportionalWidth'.` / `No parameter with name 'options' found.` / `Return type mismatch: expected 'DialogResult<String>', actual 'DialogResult<Boolean>'.` / `Argument type mismatch: actual type is 'String', but 'Boolean' was expected.`
- kmp: `Unresolved reference 'DialogOptions'.` / `No parameter with name 'options' found.` / `Return type mismatch: …`
- maui: CS1061 / CS1739 / CS0029 / CS1503 / CS0311

deviation.md の2点 (パネルの戻る導線・パネル内結果表示) は合意済み差分として扱い、指摘に含めていない。

## 前回指摘の解消状況

| # | 出典 / 重要度 | 指摘 | 状況 |
|---|---|---|---|
| 1 | 相方 round 2 / 🟠 Major | iOS Native: `prepareForPresentation()` 内で `settleLayoutSnapshot()` → `present()` の順のため window 搭載前に固定している | **解消**。`DialogContainerRootView` (`DialogContainerViewController.swift:9-18`) の `didMoveToWindow` を起点に `settleLayoutSnapshotOnScreen()` (`:137-143`) が画面上のパスを走らせ切ってから固定する。`prepareForPresentation` (`:126-130`) は暫定パスのみで固定しない。テストは本番同順 (`DialogPresenter.swift:34-35` = prepare → present、`DialogTestPresentationSurface.present` が window 搭載) になり、`DialogAttributeSupplyTests.swift:147-202` が「提示時点は暫定 (0,0) / 画面上の初回パスで届いた End/End が採用 / 外側タップも届いた false に従う」を1本で押さえている。提示前サイズ確定との両立は同テストの `frameAtPresentation` 検査と `DialogPresentationSizingTests.swift:10,24` の2本で保たれている |
| 2 | 相方 round 2 / 🟠 Major | MAUI iOS: 転送 → Arrange → 固定 の順のため、Arrange 中の変更が Native 添付面へ届かない | **解消**。`DialogAttributeSnapshotRelay` (新規) が `TransferBeforeLayout()` / `FreezeAndTransfer()` の2口に分かれ、後者が**固定した値を必ず転送し直す** (`DialogAttributeSnapshotRelay.cs:39-48`)。iOS は `DialogContentView.LayoutSubviews` (`Platforms/iOS/PlatformDialogGateway.cs:210-222`) で 暫定転送 → `base` → `Arrange` → `Window is not null` のときだけ `FreezeAndTransfer`。Android も同じ relay を `ViewAttachedToWindow` / `LayoutChange` に接続 (`Platforms/Android/PlatformDialogGateway.cs:146-160`)。自動テスト3本 (`DialogAttributeSnapshotRelayTests.cs`) が段取りを固定し、実環境確認は `verification/maui-snapshot-wiring/` に iOS / Android 各4〜6枚の証跡付きで残っている |
| 3 | review-003 / 🟠 Major | 新しい検査機構が規約に反映されておらず、規約どおりのコマンドが黙って成功する | **解消**。`test-execution.md` に「公開 API 形状の検証」節が新設され、正の検査4ルート・負の検査16本 (ルート × フラグ × 期待する診断) ・ルートごとのコマンド形・`android/ (instrumented)` の行と節・実測件数6行が入った。**本レビューで書かれたコマンドを全数実行し、そのまま機能することを実測**。旧フラグの記述は現行 concepts から消えている |
| 4 | review-003 / 🟡 Minor | MAUI 経路のスナップショット配線だけが自動検証も実環境確認も持たない | **解消**。推奨修正1と2の両方が実施された — 実機/シミュレータへ一時的な供給点 (overlayColor 赤 50% / isCanceledOnTouchOutside=false) を差し込んで採用を目視確認し証跡化、撤去後の通常挙動も撮り直し。加えて `残る限界` 節に「回帰保護は静的レビューと本記録が担保」と明記されている。Android で MAUI の配置が View まで下りてこないことを logcat で実測したうえで供給点を選び直した記録もあり、確認の質は高い |
| 5 | review-003 / 🟡 Minor | iOS の正の API 検査だけ、利用者側で宣言した ViewModel を通っていない | **解消**。`DialogAttributeCompileChecks.swift:12-20` に非 `@testable` な `ConsumerDialogViewModel` を宣言し、`DialogTypedResultCompileChecks.swift` も同じ型を使うようになった。併用している `DialogTestContentView` は素の `UIView` サブクラス (ライブラリ内部シンボルに依存しない) なので、検査の境界は崩れていない。4ルートで検査の強度がそろった |
| 6 | review-003 / 🔵 Suggestion | 収束回数の上限に達したときに何も残らない | **解消**。iOS は `os.Logger` の warning (`DialogContainerViewController.swift:157-163`)、Android は `Log.w` (`DialogLayoutHost.kt:64-71`)。どちらも「採用値が契約の定める時点の値とは限らない」ことまでコメントで説明している |

### 付随修正2件の妥当性

- **`applyAttributes` のインスタンスメソッド化** (`MauiDialogContent.swift:37-45`) — 妥当。ObjC のクラスメソッドがアプリへの静的リンク後に metaclass の method list から消える問題は `otool -oV` で実測されており (`maui-snapshot-wiring/notes.md:66-70`)、インスタンス操作への変更で構造的に回避できる。理由が doc comment に自然文で残っているため、将来クラスメソッドへ戻す誘惑を止められる。Android 側が `@JvmStatic` の companion のままなのは非対称だが、JNI 境界には同じ問題がなく、両者とも同じ意味 (中身の View の添付面へ写す) を保っているので実害はない
- **NaN 変更検知の空回り解消** — 妥当。`DialogContainerViewController.refreshLayoutSnapshotIfChanged()` (`:170-180`) と `hasPendingSupplyChange` (`:191-193`) が比較するのは正規化済みの `DialogLayout` で、`DialogLayout.swift:37-60` が非有限値を `nil` / `0` / 既定値へ畳み込むため NaN は型の中に残らない。「同じ値を読み直しても等しくならない → 収束ループが上限まで回って警告を出す」空回りが構造的に起きない。この意図は `DialogLayout.swift:8-9` のコメントに書かれている。Android は逆に**丸める前**の `Supply` で比較する設計 (`DialogLayoutSnapshot.kt:66-72`) だが、Kotlin の data class 等値は `Double.compare` 意味論で NaN 同士を等しいと判定するため空回りしない。両者の選択の違いは言語の等値意味論の差に根ざしており、どちらもコメントに理由がある

### 退行の有無

`DialogContainerViewController` / `DialogLayoutHost` / 両 platform gateway を中心に確認し、退行は見当たらなかった。

- `loadView()` のカスタム root view 化で `viewDidLoad` 以降の組み立て・外側タップの `UIGestureRecognizerDelegate` 経路は変わっていない。`didMoveToWindow` は再搭載でも呼ばれ得るが `isLayoutSnapshotFrozen` が二重固定を防ぐ
- 固定するのは**属性**だけで、解決後の rect は `updateContentLayout()` (`:307-321`) が `bounds` / `safeAreaInsets` の変化ごとに計算し直す。したがって `didMoveToWindow` 時点の frame が最終値でなくても、回転・インセット変化への追随は保たれる
- MAUI の `DialogPresentationContent` は生成時点で固定しない (`DialogGateway.cs:72-97`)。`FreezeAttributes()` は `??=` で1度きり、`Attributes` は固定後に固定値を返すため、relay の2回目以降の呼び出しは無害
- relay のイベント購読 (Android の `ViewAttachedToWindow` / `LayoutChange`) は解除していないが、購読先が提示1回分の platform view なのでライフタイムは器と同じ。固定後は早期 return するだけで、恒常的なコストにならない
- `DialogAttributeSupplyTests` の既存6本 (添付のみ / show 置換 / オブジェクト単位置換 / 完了後の無視 / 無効値正規化) は順序変更後も期待値を変えずに green。ケース表19件の全量検証 (ios 実 frame / android 実 View) も green

## 指摘事項

### [🟡 Minor] レイアウト計測ヘルパーの順序と説明が、直したはずの本番順序と食い違う

**該当箇所**: `ios/Tests/KsDialogsTests/Support/DialogLayoutMeasurement.swift:39-46`

**問題点**:
このヘルパーは `window.rootViewController = container` で器を先に window へ載せ、**そのあとで** `prepareForPresentation(inBounds:)` を呼ぶ。実効値の固定は `didMoveToWindow` を起点に走るため、実際には `rootViewController` を代入した瞬間に済んでおり、後続の `prepareForPresentation` はスナップショットに関与しない。ところがコメントは次のように書いている:

```
// 提示前の1回のレイアウトパスは提示処理と同じ手順で進める。
// 実効値のスナップショットもこのパスの完了時点で固定される (core/ADR-0015)。
```

- 「提示処理と同じ手順」は成り立っていない。本番は `prepareForPresentation` → `present` (= window 搭載) の順で、このヘルパーはその**逆順**である
- 「このパスの完了時点で固定される」も実装と一致しない。固定するのは `prepareForPresentation` のパスではなく、window 搭載を起点に走るパスである

相方 round 2 が指摘した穴 (「追加テストは window 搭載 → prepare の逆順で穴を検出できない」) の本体は、本番同順のハーネスを使う新テスト `attachmentChangeDuringFirstOnScreenLayoutPassIsAdopted` で塞がれているため**機能面の未達ではない**。ただしこのヘルパーは `DialogLayoutCaseTableTests` を含む多くのレイアウトテストの土台であり、そこに「本番と逆順で、しかも逆順であることを打ち消す説明」が残っていると、次にこの周辺を触る人が 3 サイクルかけて直した順序を読み違える。ADR ID を引いている分だけ誤誘導の力が強い。

**推奨修正**: 次のいずれか。

1. ヘルパーの順序を本番にそろえる — `DialogContainerViewController` を作る → `prepareForPresentation(inBounds:)` → `window.rootViewController = container` / `window.isHidden = false` の順にし、コメントを「本番と同じ順で、window に載せた時点のパス完了で固定される」に改める
2. 順序を変えないなら、コメントを実態に書き換える — 「このヘルパーは rect の実測が目的で、器を window に載せた時点で固定される。提示処理と同じ順序が要る検証は `DialogTestPresentationSurface` を使うハーネス側で行う」旨を明記し、`prepareForPresentation` の呼び出しが何のために残っているのか (内容サイズの確定手順を本番と合わせる) を書く

### [🔵 Suggestion] KMP の SwiftPM ロック生成物が VCS 上で不整合になっている

**該当箇所**: `kmp/.swiftpm-locks/default/swiftImport/Package.swift`、`kmp/.swiftpm-locks/default/swiftImport/subpackages/`

`:api-surface-check` モジュールの追加に伴い、追跡対象の `Package.swift` が3つのサブパッケージを参照するようになった:

```
.package(path: "subpackages/_api-surface-check"),
.package(path: "subpackages/_ksdialogs-kmp"),
.package(path: "subpackages/_ksdialogs_kmp")
```

このうち `_api-surface-check` と `_ksdialogs_kmp` は**追跡対象外** (`git status` で `??`) で、追跡されているのは旧名の `_ksdialogs-kmp` だけである。つまり追跡ファイルが VCS に無いパスを参照している状態で、変更前は 1 対 1 で整合していた。加えて `_ksdialogs-kmp` (ハイフン) と `_ksdialogs_kmp` (アンダースコア) が並存しており、前者は現行のモジュール名と対応しない残骸に見える。

実害は小さい — 本レビュー中の `./gradlew allTests` で3つとも再生成される (全ファイルの mtime が更新された) ことを確認したので、Gradle を通す限り自己修復する。ただし「生成物を追跡する」という現状の運用 (phase-10 論点F の決定) を保つなら、追跡範囲が生成結果と食い違ったままアーカイブされることになる。

**推奨修正**: 蒸留フェーズでよい。新しい2サブパッケージを追跡対象に加えて旧 `_ksdialogs-kmp` を整理するか、`.swiftpm-locks/default/swiftImport/subpackages/` ごと `.gitignore` へ回して「毎ビルドで再生成される」ことを1行残すか、どちらかに寄せる。

### [🔵 Suggestion] 次の変更 (expand-api-surface) の spec が、撤去済みの検査方式を参照している

**該当箇所**: `kasane/changes/expand-api-surface/specs/maui-binding/spec.md:16`

> **WHEN** negative compile check (既存の NegativeCompileChecks 方式) を書く

ここが指す「既存の NegativeCompileChecks 方式」(`maui/KsDialogs.Maui.Tests/NegativeCompileChecks/` + `-p:KsDialogsNegativeCompileCheck=true`) は本変更で撤去され、`KsDialogs.Maui.ApiSurfaceCheck` + 禁止形状別フラグへ置き換わっている。本変更の足場ではないため**ここでの修正対象ではない**が、そのまま実装フェーズに入ると存在しない方式を探すことになる。

**推奨修正**: expand-api-surface の実装に着手する前に、この行を新方式 (`KsDialogs.Maui.ApiSurfaceCheck` + `KsDialogsNegativeCheck*` フラグ、規約は `cross/conventions/test-execution.md`) へ読み替える旨を申し送る。

## アクションプラン

1. **🟡 Minor (レイアウト計測ヘルパー)** — コメントを実態に合わせる。順序自体を本番にそろえられるなら、そのほうが誤読の芽を残さない
2. **🔵 Suggestion (SwiftPM ロック生成物)** — 蒸留フェーズで追跡方針を1つに寄せる
3. **🔵 Suggestion (expand-api-surface の spec)** — 次の変更への申し送り

いずれも本変更の受け入れを妨げない。1 は次の着手時にまとめて片付けてもよい。

## 確認して問題がなかった観点

- **スナップショット時点 (3経路)**: iOS は `didMoveToWindow` → `setNeedsLayout` + `layoutIfNeeded` → 収束ループ → `freeze`、Android は `onAttachedToWindow` で `OnPreDrawListener` を張り、`onMeasure` の読み直しと `hasPendingChange()` の突き合わせで収束させてから `freeze`、MAUI は relay が 暫定転送 → Arrange → 固定 + 再転送。3経路とも固定は**最初の描画より前**に済み、契約の「提示後の再適用は違反」を踏んでいない。収束上限 (どちらも 4) に達した場合も、実装不良として1回だけ記録が残る
- **提示前サイズ確定との両立**: `DialogTestPresentationSurface` が `present` の入口 (window 搭載前) の中身の rect を `contentFramesAtPresentation` に記録し、`DialogPresentationSizingTests` の2本と新テストの `frameAtPresentation` 検査がそこを見ている。「固定を後ろへ動かしたら提示時に大きさが決まっていない」という取り違えは、この2軸の同時検査で防がれている
- **公開 API 検査の境界と個別性**: android / kmp は `implementation(project(...))` (friend path なし)、maui は `InternalsVisibleTo` の外の別プロジェクト、ios は非 `@testable` な `import KsDialogs` + 検査ファイル側で宣言した `ConsumerDialogViewModel`。負の検査はソースセット / `Compile Include` / `#if` で禁止形状ごとに分離され、1ビルドに1誤りしか入らない。16本すべてを個別に実行し、**それぞれ別々の診断1件**で失敗することを確認した。フラグ名は3つのビルド定義および iOS 検査ファイルの doc comment と規約の表が完全に一致している
- **規約の実用性**: `test-execution.md` に書かれたコマンドを写経して実行し、6ルートの件数・16本の負検査・4ルートの正検査すべてが記述どおりに再現した。`android/ (instrumented)` 節の「`adb devices` を先に確認」「複数台なら `ANDROID_SERIAL` で絞る」も実際に必要になった (本環境は2台接続) ため、記述が実務に即している
- **足場の凍結**: proposal / design / specs / layout-cases.json は commit 5af1a1b 以降 1バイトも変わっていない。変更アーティファクト側の差分は tasks.md のチェック・ui/brief.md への追記・証跡ファイルのみ
- **tasks.md の虚偽チェック**: 1.1〜7.3 の全項目に対応する実装・テスト・証跡を再確認した。7.1 (全ルート green) は本レビューで実測。虚偽なし
- **サンプルの後始末**: 実環境確認のために `LayoutDialogCardView` へ差し込んだ一時的な供給点は完全に撤去されている (`samples/maui/KsDialogs.Sample.Maui/LayoutDialogCardView.xaml.cs` は 24 行の素の code-behind)。撤去後の通常挙動も両 platform で撮り直されている
- **旧経路の残骸**: 旧フラグ (`-DKSDIALOGS_NEGATIVE_COMPILE_CHECK` / `-Pksdialogs.negativeCompileCheck` / `-p:KsDialogsNegativeCompileCheck`) と旧ソースセットは現行のコード・concepts から消えている。ヒットするのは archive 済み変更の記録 (歴史なので正しい) と、上記 Suggestion 3 の1行のみ
- **コメント規約**: 機械検査 0 件 / 375 ファイル。サイクル3で足された説明 (`DialogContainerRootView` の役割・`prepareForPresentation` が固定しない理由・relay が渡し直す理由・`applyAttributes` をインスタンス化した理由・`DialogLayout` を等値比較に使う理由) はいずれも ADR ID に加えて「なぜそうするか」を自然文で書いており、単独で読める。唯一の例外が上記 Minor の1箇所
- **concepts の追随記録**: `concepts/log.md` に今回の規約更新 (何が実態と離れていたか・何を足したか・実測値) が1行で残り、`cross/index.md` の1行説明も更新されている
