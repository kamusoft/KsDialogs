# レビュー結果: expand-api-surface (002 回目)

**日付**: 2026-08-19
**判定**: APPROVED

## サマリー

1周目 (`review-001.md` の Major 1・Minor 3・Suggestion 3 と `second-opinion-code-001.md` の相方 Major 1) の**全6件が成果物側で解消されている**ことを、diff とアーティファクトで裏を取って確認した。とくに争点だった2件 — KMP Swift 公開エラー型の宣言表への収束と、Compose 非依存検査の起動経路 — は、それぞれ「失敗理由が落ちていないこと」「検査が空振りしていないこと」を独自の実測で確かめた (下記「実行した検証」)。全6ビルドルートを再実行して 0 failures、件数も規約表の記載と一致する。

修正による退行は検出できなかった。`KsDialogsKmpError` の2 case への縮約は情報を落とさず (宣言表外の失敗は公開型 `DialogError` がそのまま届き、専用テストで固定されている)、ViewTree owner の復帰は破棄後にのみ走って組み立ての dispose 順序を乱さず、`test` への結線は他ルート (kmp の composite build・maui の `dotnet test` が巻き込む gradle ビルド・maui bridge 単体) のいずれのタスクグラフにも入らない。

残る指摘は Minor 2 件 (いずれも長命層の追随・オーナー確認で、実装コードの修正を要しない) と Suggestion 2 件。

## 前回指摘の解消状況

| # | 指摘 (出所) | 状況 | 確認方法 |
|---|---|---|---|
| 1 | `KsDialogsKmpError` を宣言表の2 case へ (相方 Major) | **解消** | `ios/Sources/KsDialogs/Kmp/KsDialogsKmpError.swift` は `notRegistered` / `resultTypeMismatch` の2 case。宣言表外の失敗は `publicError(from:)` が公開型 `DialogError` のまま素通しし、`KsDialogsKmpFacadeTests.showRejectsMissingPresentationHost` が `DialogError.presentationHostUnavailable` の到達を固定。`KmpApiSurfaceCompileChecks.handlesEveryKmpError` に既定枝なしの網羅 `switch` を追加しており、case が増えれば検査がコンパイルできなくなる (相方の推奨した形状検査) |
| 2 | `verifyNoDeclarativeUiDependency` を `test` に結線 (review-001 Major) | **解消** | `android/ksdialogs/build.gradle.kts:141-143` で `test` にも結線。`./gradlew test --dry-run` のタスクグラフに `:ksdialogs:verifyNoDeclarativeUiDependency` が入ることと、`test --rerun-tasks` の実行ログに当該タスクが現れることを本レビューで実測。`test-execution.md` に「本体の Compose 非依存の検証」節を新設 (成功が期待結果であることを明記) |
| 3 | 件数表を実測値へ (review-001 Minor) | **解消** | ios 90/19・android 50・instrumented 94 (62+32)・maui 44 へ更新済み。本レビューの再実測と全行一致。instrumented の確認先が2モジュールに分かれた点も追記済み |
| 4 | Compose ホストの画面破棄経路の dispose 検証 (review-001 Minor) | **解消** | `ComposeDialogContentTests.画面破棄で閉じたときに組み立てが破棄される` を追加。Activity を finish して cancelled 確定と dispose の両方を assert。実機で 32/32 green |
| 5 | ui/brief.md と tasks 6.5 の食い違い (review-001 Minor) | **食い違いは解消・承認は未取得** | tasks 6.5 に「照合作業は完了。オーナーの最終承認は未取得」と明記され、2ファイルの記述は一致した。承認ゲート自体はオーナー行為待ち (下記 Minor 2) |
| 6 | Suggestion 3件 | **3件とも反映** | (a) `DialogComposeContentView.restoreDisplacedOwners()` で据えた owner 2種を復帰 (b) `DialogExpandedApiSurfaceChecks.acceptsInlineShowWithDeclaredResultType` が `ConsumerTextDialogViewModel` / `DialogResult<String>` になり iOS・MAUI と同水準に (c) iOS containment は `DialogSwiftUIHost.makeContent` と `DialogContainerViewController.viewDidLoad` の双方に順序の意図をコメントで明示 |

## 指摘事項

### [🟡 Minor] concepts の index / log が test-execution.md の更新に追随していない

**該当箇所**: `kasane/concepts/cross/index.md:12` / `kasane/concepts/log.md` (末尾)

**問題点**:
本変更は `concepts/cross/conventions/test-execution.md` を2回書き換えている (負の検査 16→17 本・件数表・「本体の Compose 非依存の検証」節の新設) が、次の2点が追随していない。

- `cross/index.md` の1行説明が `公開 API 形状の検査 (正/負16本) の回し方` のままで、本数が実態 (17本) と食い違う。新設節 (依存グラフ検査) も index からは見えない
- `concepts/log.md` に本更新のエントリがない。本変更のタスク 1.1 (`registration-show-semantics.md` 新設) のエントリは書かれているため、同じ変更の中で片方だけ記録がない状態になっている

ksn-core の concepts 規約は「どの経路でも価値 lint・機密 lint・**index/log 更新は共通**」と定めており、直前の同ファイル更新 (add-layout-spec のレビュー指摘修正) では log にエントリを残し `cross/index.md の1行説明も更新` している。index の1行説明はワーカーが規約のロード要否を判定する唯一の手掛かりで、本数はこの規約自身が「黙って空振りする検証」への歯止めとして持っている値であるため、ここだけ古いと歯止めの意味が薄れる。

`handoff-distill.md` の 4 節は index 行の食い違いを蒸留へ申し送っているが (log の欠落には触れていない)、同じ変更で `concepts/index.md` / `core/index.md` / `log.md` を実際に手入れしている以上、蒸留待ちにする理由は薄い。

**推奨修正**: `cross/index.md` の1行説明を「正/負17本」+ 依存グラフ検査の範囲が読み取れる表現に更新し、`log.md` に test-execution.md 更新のエントリを追記する。蒸留へ回す方針を採る場合は、handoff の 4 節に log エントリの欠落も併記して取りこぼしを防ぐこと。

### [🟡 Minor] tasks 6.5 の承認ゲートがオーナー行為待ちのまま完了印になっている (継続)

**該当箇所**: `kasane/changes/expand-api-surface/tasks.md:44` / `ui/brief.md`「照合結果」節

**問題点**:
review-001 の指摘した「2ファイルの記述の食い違い」は、6.5 に「オーナーの最終承認は未取得」と明記することで解消された。ただし ksn-core の ui/ 規約が brief.md に求める「`verification/` の各画像と approved.png を照合し YYYY-MM-DD 最終承認」の**日付つき承認記録は依然として無い**まま、チェックは `[x]` である。実装側で決めきれない事項なので実装の不備ではないが、アーカイブ (蒸留) の前に閉じるべきゲートが1つ開いたまま残っている。

**推奨修正**: オーナーの最終承認を取得して brief.md に日付つきで記録する。判断はオーナーに属するため、実装側で `[x]` の意味を再解釈しないこと。

### [🔵 Suggestion] ViewTree owner の復帰処理に観測点がない

**該当箇所**: `android/ksdialogs-compose/src/main/kotlin/jp/kamusoft/ksdialogs/compose/DialogComposeContentView.kt:105-129`

`onDetachedFromWindow` で据えた owner 2種を元へ戻す処理が入り、コメントも「1枚が1つのウィンドウ」という前提に依存しないためと明記していて意図は明快である。順序 (`disposeComposition()` → 復帰 → `DESTROYED`) も安全で、既存の破棄テスト4本が緑であることから退行も起きていない。

一方でこの復帰そのものを観測するテストは無く、前提が崩れたとき (根を共有する構成になったとき) に効くはずの保険が、黙って外れても誰も気づかない。`ComposeDialogContentTests` に「閉じたあと、ウィンドウの根の `findViewTreeLifecycleOwner()` が据える前の値へ戻っている」ことを見る assertion を1件足しておくと、保険が保険として残る。

### [🔵 Suggestion] instrumented テストが共有レジストリを跨いで状態を残す

**該当箇所**: `android/ksdialogs-compose/src/androidTest/kotlin/jp/kamusoft/ksdialogs/compose/ComposeDialogContentTests.kt` 全体

`DialogViewRegistry` は登録解除の口を持たない設計 (公開面を広げない判断) のため、テストは `DialogViewRegistry.shared` に登録したまま次のテストへ進む。現状は「未登録であること」を assert する `登録せずに渡した_Compose_コンテンツは登録済みにしない` が使う `ComposeInlineTestDialogViewModel` をどのテストも登録していないため成立しているが、この成立条件はファイル全体を読まないと分からず、将来同じ型を登録するテストが1本増えるだけで順序依存で落ちる。

「未登録である」ことを見るテストは専用の VM 型を使う (型名でその意図が読める名前にする) か、その制約をテストクラスのドキュメントコメントに明記しておくと、将来の追記で崩れない。

## 実行した検証

全ビルドルートを再実測した (すべて 0 failures)。件数はコンソールまたは結果 XML から確認している。

| ルート | コマンド | 結果 |
|---|---|---|
| ios/ | `xcodebuild test -scheme KsDialogs -destination 'platform=iOS Simulator,name=iPhone 17 Pro'` | **90 tests / 19 suites / 0 failures** (`** TEST SUCCEEDED **`) |
| android/ | `./gradlew test --rerun-tasks` | **50 tests / 0 failures** (結果 XML 12 本の合算) |
| android/ (instrumented) | `./gradlew connectedDebugAndroidTest` (実機 Pixel 4a) | **94 tests / 0 failures** (`:ksdialogs` 62 + `:ksdialogs-compose` 32)。`:ksdialogs` 側は別プロセスの gradle と証跡が競合したため単独で再実行し 62/62 green を再確認 |
| kmp/ | `./gradlew allTests --rerun-tasks` | **48 tests / 0 failures** (iosSimulatorArm64 26 + androidHostTest 22) |
| maui/ | `dotnet test` | **44 tests / 0 failures** (`合格: 44、合計: 44`) |
| maui/android/native/ | `./gradlew :ksdialogs-maui-bridge:test --rerun-tasks` | **9 tests / 0 failures** |

修正の実効性・退行の有無について追加で確認したもの:

- **Compose 非依存検査が空振りしていないこと**: リポジトリのファイルを一切変更せず、init script (`-I`) で `:ksdialogs` にだけ `androidx.compose.ui:ui` を注入して `verifyNoDeclarativeUiDependency` を実行 → 規約に書かれたとおりの診断 (`debugCompileClasspath に Compose 系 artifact が混ざっています: …`) で **FAILED**。直接依存だけでなく `androidx.compose.runtime:*` 等の推移的混入も列挙されており、走査が実効していることを確認した。素の状態では成功する
- **`test` への結線の副作用**: `:ksdialogs:test` のタスクグラフにのみ追加されており、kmp の `allTests`・`dotnet test` が巻き込む maui 側 gradle ビルド (`assembleRelease` 系)・`:ksdialogs-maui-bridge:test` のいずれも当該タスクを引かない (各実行のログで確認)。`check` への結線も残っている
- **エラー情報の欠落がないこと**: `KsDialogsKmpError.publicError(from:)` は宣言表の2判別に当たるものだけを写し替え、それ以外は受け取ったエラーをそのまま返す (nil のときだけ提示不能として補完する)。提示先不在の経路が `DialogError.presentationHostUnavailable` として届くことは iOS テストで固定されている。`showFailed` / 旧 `presentationHostUnavailable` case への参照はリポジトリ内 (samples 含む) に残っていない — sample の Swift 側は総称 `catch` のみで、case 集合の縮小によるコンパイル影響もない
- **owner 復帰による退行の有無**: 復帰は `disposeComposition()` の後・`DESTROYED` 遷移の前に1回だけ走り、再 attach 時は据え直される。全閉鎖経路4本の破棄テスト (完了 / キャンセル / 呼び出し元キャンセル / 画面破棄) が実機で緑
- **負のコンパイル検査の記載更新**: 規約表が「オーバーロードのため2件出る」と書き換えられた `ksdialogs.negativeCheck.showOptions` を単独実行し、`None of the following candidates is applicable:` と `No parameter with name 'options' found.` の2件でビルドが失敗することを確認 (期待結果どおりの失敗)
- **足場アーティファクトの凍結**: `proposal.md` / `design.md` / `specs/**` は本サイクルでも未変更。`changes/` 配下の変更は `tasks.md` (チェックと 6.5 の注記) と `ui/brief.md` (照合結果の追記) のみ。`deviation.md` は存在せず、無断の乖離は記録上ゼロ
- **宣言表 (design Decision 6) との対応**: KMP Swift 面の登録4形・show 2形・エラー型が宣言表と一致することを `KmpApiSurfaceCompileChecks` と実装で突き合わせ、1周目に見落とされた enum の case 集合まで含めて一致を確認した
- **Decision 10 (preference 解決の3終了状態)**: 同期解決 / 追加パス1回 (`maxAttributeSupplyWaitPasses = 1`) / 上限到達時の既定値 + 警告ログが `DialogContainerViewController.scheduleAttributeSupplyWait()` に揃っており、spec-002 で追加された規定と一致

## アクションプラン

1. **[Minor]** `cross/index.md` の1行説明 (負の検査 17 本・依存グラフ検査) と `concepts/log.md` のエントリを追随させる — 蒸留へ回す場合は handoff の 4 節に log の欠落も併記する
2. **[Minor]** ui/brief.md へオーナーの最終承認を日付つきで記録する (オーナー行為。アーカイブ前のゲート)
3. **[Suggestion]** ViewTree owner 復帰の観測点を1件足す / 「未登録である」系テストの前提を型名かコメントで明示する — いずれも任意
