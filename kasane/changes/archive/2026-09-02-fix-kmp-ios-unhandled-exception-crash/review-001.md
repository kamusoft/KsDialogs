# レビュー結果: fix-kmp-ios-unhandled-exception-crash (001 回目)

**日付**: 2026-09-02
**判定**: CHANGES_REQUESTED

## サマリー

2つの修正のうち、`SamplePresenter` への `@Throws` 付与は合意済みスコープどおりで、対象の suspend 関数 10 本を漏れなく覆っており、機構の確認 (強制失敗の A/B) も証跡付きで残っている — こちらは問題ない。もう一方の「提示先 (key window) の準備完了待ち」は、判定条件がライブラリの提示可否より**厳密に弱い** (前面アクティブなシーンへの限定が抜けている) ため、狙った競合をすり抜ける経路が残っており、決定事項の記述 (「前面シーンの key window」) とも実装のコメント (「前面のシーンに」) とも食い違っている。あわせて、この change が直した abort と**同型の欠陥がライブラリの公開面 (`KsLoading` / `KsToast`) に残っている**ことを確認した — exploration の切り分け結論「ライブラリ本体の欠陥ではない」は `KsDialogs.show` にしか当てはまらない。

### 実行した検証

| 対象 | コマンド | 結果 |
|---|---|---|
| KMP Sample 共有コード | `samples/kmp` で `./gradlew :shared:compileKotlinIosSimulatorArm64 :shared:compileAndroidMain` | BUILD SUCCESSFUL |
| KMP iOS Sample アプリ | `samples/kmp/iosApp` で `xcodebuild build -scheme KsDialogsSampleKmp -destination 'platform=iOS Simulator,name=iPhone 17 Pro'` | BUILD SUCCEEDED |
| KMP ライブラリ (無変更だが依存先) | `kmp` で `./gradlew allTests` | 96 tests / 0 failures (iosSimulatorArm64 49 + androidHostTest 47。handbook の実測値と一致) |
| 動作確認 | `--demo basic-dialog` のコールド起動 3 回 (terminate → 1.5s → launch) | 3 回とも Basic Dialog が正常表示、abort なし |
| lint | `local-path-lint.py --paths <本 change の変更ファイル + evidence>` / `comment-policy-lint.py samples/kmp` / `identity-lint.py` | いずれも 0 件 |

`samples/kmp/shared` にはテストソースが無く (`src/commonMain` のみ)、Sample を対象にした自動テストは存在しない。作業ツリーには本 change と無関係な `scripts/` の未コミット変更があり、`local-path-lint.py` の全体実行はその自己テスト用フィクスチャに当たる — 本 change の成果物は上記のとおり clean。

## 照合した規約

- `kasane/handbook/cross/comment-policy.md` (always) — 新規コメントに change / フェーズの裸参照なし、ファイル単独で意味が通る。lint も 0 件
- `kasane/handbook/cross/sample-parity.md` (`samples/**` を触る) — 撮影支援機構は一致要件の枠外、かつ外部から見える契約 (引数キー名・デモ ID・不正値の倒れ方) は無変更。`samples/ios` を揃えない判断は決定事項に記録済みで、規約違反にはあたらない
- `kasane/handbook/cross/runtime-behavior-verification.md` (実行時挙動が絡む不具合の調査・完了判定) — `@Throws` 側は満たしている。提示先待ち側は未充足 (指摘 4)
- `kasane/handbook/cross/test-execution.md` (テスト実行・完了判定) — kmp ルートの全件実行コマンドと件数確認を実施
- `kasane/decisions/kmp/0001-swift-interop-plain-suspend.md` — 素の suspend 直接公開は維持されており、`@Throws` の付与で ObjC シグネチャは変わらない (生成ヘッダで確認) ため ADR の範囲内

## 指摘事項

### [🟠 Major] 提示先の判定条件がライブラリより弱く、待ちが空振りしうる

**該当箇所**: `samples/kmp/iosApp/KsDialogsSampleKmp/SampleMenuScreen.swift:135-141`

**問題点**: ライブラリが提示可否に使う条件は `ios/Sources/KsDialogs/Presentation/ApplicationKeyWindowProvider.swift:24-28` の `selectKeyWindow` で、**前面でアクティブなシーン** (`activationState == .foregroundActive`) に絞ってから key window を選ぶ。一方 Sample の `hasPresentationHost` は `UIApplication.shared.connectedScenes` を活性状態で絞らずに走査するため、述語としてライブラリより厳密に弱い — Sample が「準備できた」と判定してもライブラリの `canPresent` (`UIKitDialogPresentationSurface.swift:12-15` → `topmostViewController()`) は false でありうる。

コールド起動では window が key になるのはシーン接続時 (`.foregroundInactive` の段階) で、`.foregroundActive` になるのはその後である。`.task` はこの間に発火するため、「key window はあるがシーンはまだアクティブでない」は起動シーケンス上の例外的状態ではなく通常の通過点であり、この change が塞ごうとした競合そのものがすり抜ける。関数の doc コメント (「前面のシーンに」) と決定事項 (「前面シーンの key window に rootViewController がある」) はどちらも活性シーンへの限定を述べており、実装だけがそれを欠いている。

なお、この経路を実機で再現して観測したわけではない (指摘 4 参照)。指摘は両者のソースの述語比較に基づく。

**推奨修正**: `hasPresentationHost` の判定に `windowScene.activationState == .foregroundActive` を加え、ライブラリの `selectKeyWindow` と同じ述語にする。あわせて、条件を二重に持つことによる将来のずれを防ぐため、コメントで照合先の型名 (`ApplicationKeyWindowProvider` / `UIKitDialogPresentationSurface.canPresent`) を名指しする (リポジトリ内のコード識別子への参照はコメント規約で許容されている)。

### [🟠 Major] 同型の abort がライブラリの公開面 (`KsLoading` / `KsToast`) に残っている

**該当箇所**: `kmp/ksdialogs-kmp/src/commonMain/kotlin/jp/kamusoft/ksdialogs/kmp/KsLoading.kt:28,40,47,54,67,83` / `kmp/ksdialogs-kmp/src/commonMain/kotlin/jp/kamusoft/ksdialogs/kmp/KsToast.kt:37,50`

**問題点**: KMP ライブラリ全体で `@Throws` が付いているのは `KsDialogs.show` と `DialogGateway.show` の 2 箇所だけで、`KsLoading` の suspend 5 本と `KsToast.show` 2 本には無い。いずれも doc に「未登録の ViewModel 型は構成ミスとして `DialogException` になる」と明記され、実装も投げる (`kmp/ksdialogs-kmp/src/iosMain/kotlin/jp/kamusoft/ksdialogs/kmp/IosToastGateway.kt:37` が `throw DialogException(...)`)。生成された ObjC ヘッダにも Kotlin/Native 自身の注記が出ている —

> `@note This method converts instances of CancellationException to errors. Other uncaught Kotlin exceptions are fatal.` (`KsLoading.hide` の宣言に付随)

つまり Swift 消費者が `Loading.instance.hide()` や `Toast.instance.show(viewModel:)` を直接呼び、未登録 ViewModel や提示先不在で失敗すると、本 change が Sample で潰したのと**同じ経路で abort する**。exploration の切り分け「ライブラリの契約は公開面では守られており、Sample が自分の export 面で同じ宣言を欠いていた」「ライブラリ本体の欠陥ではない」は `KsDialogs.show` にしか当てはまらず、この結論のまま change を閉じると、一般公開予定のライブラリに同型の欠陥が残ったままになる。

**推奨修正**: 本 change のスコープ (Sample に閉じる・公開面は据え置き) を超えるため、ここでは直さず**起票をオーナーに諮る**。判断材料として: suspend 関数 (`KsLoading` 5 本) は `@Throws` の有無で ObjC シグネチャが変わらない (`completionHandler:(void (^)(NSError * _Nullable))` のまま。今回の生成ヘッダで確認) ため公開面の形は不変だが、非 suspend の `KsToast.show` は `error:` 引数が増える公開面変更になる — この非対称が判断の分かれ目になる。

### [🟡 Minor] 待ちループがキャンセルを握り潰す

**該当箇所**: `samples/kmp/iosApp/KsDialogsSampleKmp/SampleMenuScreen.swift:129-132`

**問題点**: `try? await Task.sleep(...)` はキャンセル由来の `CancellationError` も飲み込む。`.task` が (画面離脱などで) キャンセルされると、以降 `Task.sleep` は即座に throw し続けるため、ループは待たずに残り回数を空回りしたうえで抜け、キャンセル済みにもかかわらず自動再生 (`model.autoPlay` 等) へ進む。実害は Sample の範囲では小さいが、意図しない挙動である。

**推奨修正**: `do { try await Task.sleep(...) } catch { return }` にするか、ループ先頭で `if Task.isCancelled { return }` を見る。

### [🟡 Minor] 提示先待ちの効果に識別力のある証跡がない

**該当箇所**: `kasane/changes/fix-kmp-ios-unhandled-exception-crash/exploration.md` (再現の試行 / 機構の確認の節)

**問題点**: `@Throws` 側は「強制失敗による段階 1 / 段階 2」の A/B と、段階 1 のクラッシュログが `evidence/` に残っており `runtime-behavior-verification.md` の 1〜3 を満たしている。一方、提示先待ちについては修正前の同一手順で症状が出ることを示せていない — 修正前ビルドもコールド起動 25/25 で正常だったため、「修正後のコールド起動 10 回で正常」は待ちが効いたことを何も区別しない (待ちを外しても同じ結果になる)。この状態では、指摘 1 のような述語のずれが残っていても検証で気づけない。

**推奨修正**: `@Throws` 検証に使った「提示先不在を強制する一時変更」を「起動後 N ミリ秒だけ提示先不在を強制する」形にすれば、待ちの有無で abort / 正常表示が分かれる A/B が取れる。指摘 1 の修正後にこの手順で確認し、証跡を `evidence/` に残すことを推奨する。

### [🔵 Suggestion] `@MainActor` の付け方が不揃い

**該当箇所**: `samples/kmp/iosApp/KsDialogsSampleKmp/SampleMenuScreen.swift:127,136`

**問題点**: `SampleMenuScreen` は `View` 準拠により型全体が `@MainActor` 推論されるため、`waitUntilPresentationHostIsReady` の `@MainActor` は冗長である。一方で同じく MainActor 隔離された API (`UIApplication.shared`) を触る `hasPresentationHost` には付いていない。どちらかに揃っていないと、読み手に「片方だけ隔離要求がある」と誤読させる。

**推奨修正**: 両方から外す (型の推論に委ねる) か、両方に付ける。同ファイル・同ルートの既存コード (`SampleMenuModel` は型に `@MainActor`) の書き方に合わせる。

### [🔵 Suggestion] 判定基準となる決定事項が実装と同じコミットで書かれている

**該当箇所**: コミット `fd4c376` (`kasane/changes/fix-kmp-ios-unhandled-exception-crash/exploration.md`)

**問題点**: 提示先待ちの条件・刻み・上限を定める決定事項 3 件が、その実装コミットと同一コミットで exploration.md に追記されている。S 級では exploration.md が合意済みスコープの正であり、レビューはこれを判定基準に使うため、実装と同時に書かれた記録は基準としての強さが落ちる (実際、今回はその決定文と実装が食い違っている — 指摘 1)。

**推奨修正**: 記録の書き換えは指示しない。次回以降、決定の確定と実装は別コミットに分けることを推奨する。

## アクションプラン

1. **指摘 1** を修正する (`activationState == .foregroundActive` の追加 + 照合先を名指しするコメント)。これが本 change の主目的である競合の封じ込めそのものに関わる
2. **指摘 3** を修正する (キャンセルの尊重)。1 と同じファイル・同じ関数で一度に直せる
3. **指摘 4** の A/B を取り、証跡を `evidence/` に残す。1 の修正が効いたことの確認も兼ねる
4. **指摘 2** をオーナーに諮る (別 change の起票 / 見送りの判断)。本 change では直さない
5. 指摘 5・6 は任意
