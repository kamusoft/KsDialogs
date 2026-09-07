# レビュー結果: expand-api-surface (001 回目)

**日付**: 2026-08-19
**判定**: CHANGES_REQUESTED

## サマリー

5ドメイン横断の L 級変更として、デルタスペック 6 本の Requirement / Scenario はいずれも実装とテストで裏付けられており、全6ビルドルートを実測で回して 0 failures を確認した (下記「実行した検証」)。内部表現の1本化 (`DialogContent` / `erasedDialogViewFactory` / `DialogViewFactories.Erase`) で登録経路とインライン経路を同じ変換に載せた設計は design Decision 1・5 に忠実で、非干渉 Scenario も4形態すべてで個別にテスト化されている。concepts の新規文書 (`registration-show-semantics.md`) と `handoff-distill.md` の申し送りも精度が高い。

一方で、design Decision 7 の核心 (本体 `ksdialogs` が Compose に依存しないこと) を固定するために新設した検査タスクが、**プロジェクトが文書化しているどの検証ルートからも起動されない**状態になっている。これは本プロジェクトが `concepts/cross/conventions/test-execution.md` で「黙って空振りする検証」を第一級の落とし穴として扱っていることに正面から抵触し、かつ申し送り先 (蒸留) では build 定義を直せないため放置が確定する。この1点を Major として差し戻す。

## 指摘事項

### [🟠 Major] Compose 非依存の固定検査がどの検証ルートからも走らない

**該当箇所**: `android/ksdialogs/build.gradle.kts:86-133` (`verifyNoDeclarativeUiDependency` と `tasks.named("check") { dependsOn(...) }`)

**問題点**:
`specs/android-native/spec.md` の Scenario「本体は Compose 非依存のまま」に対応する検査として依存グラフ走査タスクを新設しているが、フックしているのは `check` だけである。実測で確認した:

```
$ ./gradlew :ksdialogs:test --dry-run | grep verifyNoDeclarativeUiDependency
→ ヒットなし (test のタスクグラフに入っていない)
$ ./gradlew :ksdialogs:verifyNoDeclarativeUiDependency
→ BUILD SUCCESSFUL (単独で回せば正しく通る)
```

`concepts/cross/conventions/test-execution.md` が定める android/ の全件実行コマンドは `./gradlew test --rerun-tasks` であり、この検査は含まれない。同規約に新タスクの記載もない。リポジトリに CI ワークフローは存在しない (`.github/workflows` なし) ため、**この検査を起動する経路は現時点でどこにも文書化されていない**。

結果として、将来 `:ksdialogs` に compose 系依存が混入しても、規約どおりの検証はすべて緑のまま通る。Decision 7 の目的 (MAUI Android がバインディング経由で本体を取り込む経路に compose-ui を推移させない) を守る唯一の機械的な歯止めが、実質無効化された状態で残る。

`handoff-distill.md` の 4 節はこの点を「追記を検討すること」として蒸留へ申し送っているが、**ksn-distill が触るのは長命層のドキュメントであって build 定義ではない**ため、タスクの結線はこの申し送りでは解決しない (規約への追記だけが行われ、コード側の穴は残る)。

**推奨修正**: 次のいずれかで、規約どおりの実行に検査が乗るようにする。
- `android/ksdialogs/build.gradle.kts` で `tasks.named("test") { dependsOn(verifyNoDeclarativeUiDependency) }` を追加する (`check` への結線はそのままでよい)
- または、`concepts/cross/conventions/test-execution.md` の android/ の行を `./gradlew test verifyNoDeclarativeUiDependency` 相当へ更新し、「公開 API 形状の検証」と同格の節として実行方法・期待結果 (成功が期待結果) を明記する — 規約更新は本変更の tasks 4.1 で既に同ファイルに手を入れている以上、蒸留待ちにする理由がない

### [🟡 Minor] test-execution.md の件数表が 3 行だけ古いまま残っている

**該当箇所**: `kasane/concepts/cross/conventions/test-execution.md:17-24`

**問題点**:
本変更で maui の行だけ 31 → 44 に更新されているが、同じ表の ios / android / android (instrumented) は旧値のままである。実測との差:

| 行 | 規約の記載 | 本レビューでの実測 |
|---|---|---|
| ios/ | 59 tests / 14 suites | **90 tests / 19 suites** |
| android/ | 40 tests | **50 tests** |
| android/ (instrumented) | 62 tests | **93 tests** (`:ksdialogs` 62 + `:ksdialogs-compose` 31) |

この表は「テストが 1 件も走らなくてもコマンドは成功し得る」ことへの唯一の歯止めとして置かれており (同ファイル冒頭)、値が古いと歯止めとして機能しない。かつ同ファイルは「テスト構成が育って実態が変わったら本規約を実測で更新する」と自ら定めている。1 行だけ更新して残りを据え置くと、表全体の鮮度が読み手から判断できなくなる点が特に問題である。

`handoff-distill.md` の 4 節に正しい実測値が揃っており蒸留での反映が予定されているため Minor に留めるが、**同じ表の 1 セルを本変更で書き換えている以上、残り 3 セルを同時に直すのが自然**である。また instrumented の件数確認先が 2 モジュールに分かれた点 (`ksdialogs-compose/build/outputs/androidTest-results/...`) も同節に追記が要る。

**推奨修正**: 上表の 3 行を実測値に更新する (`handoff-distill.md` の値と一致する)。

### [🟡 Minor] Compose ホストの「画面破棄」経路の破棄テストがない (iOS には対応テストがある)

**該当箇所**: `android/ksdialogs-compose/src/androidTest/kotlin/jp/kamusoft/ksdialogs/compose/ComposeDialogContentTests.kt:165-186`

**問題点**:
`specs/android-native/spec.md` の Requirement「Compose ホストの lifecycle と破棄 (Android)」は全閉鎖経路として **完了 / キャンセル / 呼び出し元キャンセル / 画面破棄** の4つを挙げているが、テストは前3つだけである (`完了で閉じたときに組み立てが破棄される` / `キャンセルで閉じたときに…` / `呼び出し元キャンセルで閉じたときに…`)。

iOS 側は同じ契約に対して `器が破棄されるとホストも一緒に解放される` (`ios/Tests/KsDialogsTests/DialogSwiftUIContentTests.swift`) を持っており、両 Native で検証範囲が非対称になっている。Android の画面破棄経路は `ActivityDialogPresentationSurface` の `destroyObserver.observeDestroy(activity) { container.closeOnHostDestroyed() }` を通り、他3経路とは別の入口なので、`disposeComposition` への合流は別途確かめる価値がある。

なお当該 Requirement の Scenario 本文は WHEN に3経路しか書いていないため、Scenario 単位では充足している。Requirement 本文との差という位置づけで Minor とする。

**推奨修正**: `ComposeDialogContentTests` に画面破棄 (提示先 Activity の破棄) 経路の dispose 検証を1件足す。テスト用 Activity (`ComposeDialogTestActivity`) がすでにあるため、器を出したまま Activity を finish する形で追加できる。

### [🟡 Minor] ui/brief.md が「オーナーの最終承認は未取得」のまま tasks 6.5 が完了印になっている

**該当箇所**: `kasane/changes/expand-api-surface/ui/brief.md` (「実装時の追記 (2026-08-19)」→「照合結果」節) / `kasane/changes/expand-api-surface/tasks.md:44`

**問題点**:
`ui/brief.md` は「approved.png と構造・トークン・状態・意図の4観点で照合した。2周で収束。**オーナーの最終承認は未取得**」と記録しているのに対し、tasks.md の 6.5「mock との視覚照合 (approved.png 基準)」は `[x]` になっている。ksn-core の ui/ 規約 (`references/ui-artifacts.md`) は brief.md に「verification/ の各画像と approved.png を照合し YYYY-MM-DD 最終承認」を記録することを求めており、証跡側は「未承認」と読める状態で完了印だけが付いている。

照合作業そのものは 4ルート分の `ui/verification/` 19 枚 + 合意済み妥協1件の記録があり実施済みなので、**タスクの実体が虚偽なわけではない**が、承認ゲートの状態が2つのファイルで食い違う。

**推奨修正**: オーナーの最終承認を取得して brief.md に日付つきで記録するか、6.5 のチェックを「照合完了・承認待ち」と読める状態 (未チェック、または承認待ちの明記) に整える。判断はオーナー確認を要するため実装側で決めきらないこと。

### [🔵 Suggestion] Compose ホストが root view の ViewTree owner を書き換えたまま戻さない

**該当箇所**: `android/ksdialogs-compose/src/main/kotlin/jp/kamusoft/ksdialogs/compose/DialogComposeContentView.kt:80-96`

`onAttachedToWindow` で `rootView.setViewTreeLifecycleOwner(this)` / `setViewTreeSavedStateRegistryOwner(this)` を行っているが、`onDetachedFromWindow` では自分の `viewTreeObserver` からリスナを外すだけで、root view に据えた owner は残る。owner は直後に `DESTROYED` へ遷移するため、同じ decor に何かが後から載る構成になると壊れた owner を拾うことになる。

現状は `DialogContainer` が show 1回につき 1 つの `android.app.Dialog` ウィンドウを持つ (「1枚が1つのウィンドウ」) ため実害はなく、この前提はコメントにも書かれている。ただしこの前提が崩れたときに現れる不具合は追いにくいので、`onDetachedFromWindow` で `rootView.setViewTreeLifecycleOwner(null)` 相当を戻すか、前提への依存をコメントで明示しておくと安全側になる。

### [🔵 Suggestion] Android の正の API 形状検査で「カスタム結果型のインライン show」が実質的に検査されていない

**該当箇所**: `android/api-surface-check/src/main/kotlin/jp/kamusoft/ksdialogs/apicheck/DialogExpandedApiSurfaceChecks.kt:55-57`

`acceptsInlineShowWithDeclaredResultType` は「結果型を明示した ViewModel でも、インライン show の結果はその型で返る」という説明だが、使っている `ConsumerDialogViewModel` の結果型は `Boolean` であり、戻り値注釈も `DialogResult<Boolean>` である。真偽値の顔を使う直前の検査 (`acceptsInlineShow`) と型的な差がなく、非真偽値の結果型が型付きで返ることを固定できていない。

iOS (`DialogApiSurfaceCompileChecks.swift` の `ConsumerTextDialogViewModel`) と MAUI (`AcceptsInlineShowWithDeclaredResultType` で `DialogResult<string>`) は文字列結果で検査しており、Android だけ弱い。文字列結果の ViewModel を1つ足して `DialogResult<String>` を返す形にすると3形態でそろう。

### [🔵 Suggestion] iOS の SwiftUI ホストを child containment に組み込む順序が Apple の推奨手順と前後している

**該当箇所**: `ios/Sources/KsDialogs/SwiftUI/DialogSwiftUIHost.swift:24` / `ios/Sources/KsDialogs/Presentation/DialogContainerViewController.swift:128-134`

`DialogSwiftUIHost.makeContent` の時点で `contentView.embed(hostingController.view)` を済ませており、`addChild(contentHost)` が呼ばれるのはその後 (`viewDidLoad`) になる。Apple の containment の手順は addChild → view の追加 → didMove の順で、ここは view の追加だけが先行している。

`addChild` の時点では hosting controller の view は未接続のサブツリーにあり、`contentView` が器の view 階層へ入るのは同じ `viewDidLoad` の中なので、出現ライフサイクルは実際には通る (テスト `SwiftUI の中身はホストごと器に組み込まれる` / `全閉鎖経路でホストが解放される` も通っている)。動作上の問題は観測されなかったが、design Decision 8 が「Apple のガイダンスに反する」ことを代替案 A の却下理由に挙げている以上、手順もガイダンスどおりに寄せておくと意図が読み取りやすい。

## 実行した検証

全ビルドルートを実測で回した (すべて 0 failures)。件数は結果 XML / ログから確認している。

| ルート | コマンド | 結果 |
|---|---|---|
| ios/ | `xcodebuild test -scheme KsDialogs -destination 'platform=iOS Simulator,name=iPhone 17 Pro'` | **90 tests / 19 suites / 0 failures** (`** TEST SUCCEEDED **`) |
| android/ | `./gradlew test --rerun-tasks` | **50 tests / 0 failures** |
| android/ (instrumented) | `./gradlew connectedDebugAndroidTest` (実機 1 台) | **93 tests / 0 failures** (`:ksdialogs` 62 + `:ksdialogs-compose` 31) |
| kmp/ | `./gradlew allTests --rerun-tasks` | **48 tests / 0 failures** (iosSimulatorArm64 26 + androidHostTest 22) |
| maui/ | `dotnet test` | **44 tests / 0 failures** |
| maui/android/native/ | `./gradlew :ksdialogs-maui-bridge:test --rerun-tasks` | **9 tests / 0 failures** |
| samples/android | `./gradlew assembleDebug` | 成功 |
| samples/kmp | `./gradlew :androidApp:assembleDebug` | 成功 |

追加で確認したもの:

- 新設の負のコンパイル検査 `KsDialogsNegativeCheckSimpleRegister` を単独実行し、規約表どおり **CS0311** (`ConsumerTextDialogViewModel` → `IDialogViewModel` の変換なし) 1 件で失敗することを確認 (期待結果どおりの失敗)
- `:ksdialogs:verifyNoDeclarativeUiDependency` は単独実行では成功 (= 現時点で本体に Compose 依存はない)。ただし Major 指摘のとおり、規約のコマンドからは起動されない
- `samples/` 配下に `KsDialogsInteropBridge` への参照が 1 件も残っていないこと (specs/samples「KMP iOS Sample の公開 API 化」/ verify-001 ❌3 の解消) を grep で確認
- 足場アーティファクトの凍結: `proposal.md` / `design.md` / `specs/**` は未変更。`tasks.md` はチェック更新のみ、`ui/brief.md` は ui/ 規約が求める照合結果の追記のみ (逆流修正なし)
- `deviation.md` は存在しない = 無断の乖離は記録上ゼロ。design Decision 6 の宣言表と実装の対応を全形態で突き合わせ、宣言レベルの形 (引数・型引数・result ラベル・別名) はすべて一致していることを確認した (KMP Swift 面が `Dialog.shared.kmp` / `KsDialogsKmp` の下に置かれている点は、design が「名前は仮」と明記しているため乖離としない)
- concepts への追随: `core/api/registration-show-semantics.md` の新設と `layout-semantics.md` / `core/index.md` / `index.md` / `log.md` の更新が tasks 1.1 の指示どおり (添付の面の表への SwiftUI / Compose 行追加、「宣言的 UI 向けの添付イディオムはまだ提供していない」の削除) に行われていることを確認
- コメント規約: 新規・変更コードのコメントは ADR ID を根拠として添えつつ日本語で理由を自足説明しており、ID だけに依存した記述は見当たらない。TODO / FIXME / デバッグ出力の残置もなし

## アクションプラン

1. **[Major]** `verifyNoDeclarativeUiDependency` を `test` に結線するか、`test-execution.md` の android/ ルートへ実行方法として明記する — 蒸留へ回すと build 定義は直らないため、本変更で閉じること
2. **[Minor]** `test-execution.md` の件数表の ios / android / android (instrumented) の 3 行を実測値 (90 / 50 / 93) に更新する。instrumented の件数確認先が 2 モジュールに分かれた点も併記する
3. **[Minor]** ui/brief.md と tasks 6.5 の承認状態の食い違いを解消する (オーナー確認を要する — 実装側で決めきらない)
4. **[Minor]** `ComposeDialogContentTests` に画面破棄経路の dispose 検証を追加する
5. **[Suggestion]** 3件 (Compose ホストの owner 復帰 / Android 正検査のカスタム結果型 / iOS containment の順序) — 任意
