# レビュー結果: add-loading (003 回目)

**日付**: 2026-08-26
**判定**: APPROVED

## サマリー

review-002 の Major (iOS 全件実行の不安定さ) は環境掃除後の実測で解消を確認した — 5 回連続で **212 tests / 38 suites が green** (12.1〜14.1 秒)。review-002 の Minor 2 件と相方 2 周目の指摘 (最終進捗が終了に追い越されない保証の機構化・遅延生成失敗の捕捉範囲) も、いずれも根本を機構で押さえた修正で解消しており、全 6 ルートが 0 failures、Scenario 網羅・ミラー・3 種の lint もすべて通っている。新しい欠陥・契約変更の持ち込みは見つからなかった。

指摘は優先度の低い Minor 1 件 (Android 側にミラーの契約テストが無い) と、review-002 から持ち越しの Suggestion 2 件のみで、いずれも完了を妨げない。

## 実行した検証

| ビルドルート | コマンド | 結果 |
|---|---|---|
| ios/ | `xcodebuild test -scheme KsDialogs -destination 'platform=iOS Simulator,name=iPhone 17'` (5 回反復) | **212 tests / 38 suites — 5 回とも green** (12.4 / 12.2 / 12.8 / 12.3 / 14.1 秒) |
| android/ | `./gradlew test --rerun-tasks` | **67 / 0 failures** |
| android/ (instrumented, Pixel 4a) | `ANDROID_SERIAL=<Pixel 4a> ./gradlew connectedDebugAndroidTest` | `:ksdialogs` **187** (skipped 1) + `:ksdialogs-compose` **35** = **222 / 0 failures** |
| kmp/ | `./gradlew allTests --rerun-tasks` | iosSimulatorArm64 42 + androidHostTest 38 = **80 / 0 failures** |
| maui/ | `dotnet test` | **110 / 0 failures** |
| maui/android/native/ | `./gradlew :ksdialogs-maui-bridge:test --rerun-tasks` | **25 / 0 failures** |

- 件数の増分は本サイクルの追加分と一致する — ios 206→212 (`LoadingReportQueueTests` 4 + 契約固定 2)、instrumented `:ksdialogs` 186→187 (致命的 Error 伝播 1)
- `android/` 系の Gradle は review-002 の罠 (build ディレクトリの取り合い) を避けて逐次実行した
- `python3 scripts/scenario-id-coverage.py` → 141/150 (除外 9 件)・未網羅なし。`--require-mirror` → 対象領域は両 Native に揃っている。`--selftest` → 全件 OK
- `scripts/local-path-lint.py` / `scripts/identity-lint.py` / `scripts/comment-policy-lint.py` → いずれも 0 件 (comment-policy は 739 ファイル検査)
- 負の検査: 本サイクルで公開面の差分が無いため、新規 2 本のみ実測して確認した — `ksdialogs.negativeCheck.loadingShowStyle` / `.loadingShowOptions` はどちらも `No parameter with name 'style' / 'options' found.` で BUILD FAILED (期待どおり)。残る 35 本は review-001 の実測を引き継ぐ
- 足場アーティファクト: `specs/` 6 ファイル・`proposal.md` / `design.md` は未変更 (git 上も未 modified)。`tasks.md` はチェックの反転のみで、虚偽のチェックは見つからなかった

## 前サイクルの指摘に対する確認

| # | 出典 | 指摘 | 状態 |
|---|---|---|---|
| 1 | review-002 Major | iOS の全件実行が安定して green にならない (検証機の CPU 汚染) | **解消**。掃除後の 5 回反復がすべて green。所要も 100→12 秒台に戻り、review-002 の原因分析 (負荷起因) と整合する |
| 2 | 相方 2 周目 | 最終進捗が終了に追い越されうる余地が残る (受理ごとに独立した仕事を投げる形) | **解消**。`LoadingReportQueue` で受理を鎖に載せ、`drain()` で終了前に待ち切る。機構の単体テスト 4 本 + 端から端までの契約テスト 2 本 |
| 3 | review-002 Minor | Android 遅延生成失敗の握り潰しが無記録 / 捕捉範囲が広すぎる | **解消**。`catch (contentFailure: Exception)` へ限定し `Log.w` を追加。致命的 Error の伝播を instrumented 1 本で固定し、判断は `deviation.md` に記録済み |
| 4 | review-002 Minor | 再帰 post の打ち切りが KMP 側 Sample に入っていない | **解消**。`samples/kmp/androidApp/.../CustomLoadingCardView.kt` と `samples/android/.../CustomLoadingCardView.kt` の差分は package 宣言・import・KDoc の 1 行のみになった (`diff` で確認) |
| 5 | review-002 Suggestion | 蒸留への申し送り (concepts の陳腐化) | **未対応** (下記 Suggestion) |
| 6 | review-002 Suggestion | Swift 互換面にテストターゲットが無い事情の記録 | **未対応** (下記 Suggestion) |

## 指摘事項

### [🟡 Minor] 「報告の直後に終了しても最終進捗が追い越されない」の回帰ガードが iOS 側にしか無い

**該当箇所**: `android/ksdialogs/src/androidTest/kotlin/jp/kamusoft/ksdialogs/LoadingProgressTests.kt` (LD-PR-01〜06 のみ)

**問題点**:

本サイクルで iOS には、この保証を端から端まで押さえる契約テストが 2 本入った (`ios/Tests/KsDialogsTests/LoadingProgressTests.swift` の「報告の直後に処理が戻っても最終進捗が終了に追い越されない」と `KsLoadingKmpTests.swift` の互換面版)。Android 側に同趣旨のテストは無い。

Android が現状この保証を満たしていることは確認した — `LoadingCoordinator.report` の `scope` は `Dispatchers.Main.immediate` で組まれており (`LoadingCoordinator.kt:64-66`)、`endUse` も `withContext(Dispatchers.Main.immediate)` (同 `:228`) であるため、別スレッドからの報告は Handler の FIFO で、主スレッドからの報告は dispatch 省略の即時実行で、いずれも終了より先に受理される。つまり**保証の出所はディスパッチャの選択という 1 行**にある。

にもかかわらず、その 1 行を `Dispatchers.Main` (immediate なし) に替えるだけで順序は静かに崩れ、既存の LD-PR 系 6 本はどれも落ちない (どれも報告と終了を競わせていない)。本変更が iOS でわざわざ機構化した性質が、ミラー側では検査されないまま「たまたま成立している」状態で残る。

なお Scenario ID を持たないテストのため `scripts/scenario-id-coverage.py --require-mirror` は通っており、ミラー規約への違反ではない。

**推奨修正**: iOS の 2 本と同型の instrumented テストを 1 本足す (進捗受け口つき VM のカスタム Loading で `report(0.5)` / `report(1.0)` を呼んで即座に action を抜け、受け口の受領列が `[0.5, 1.0]` であることを見る)。優先度は低く、本変更の完了を妨げるものではない。

---

### [🔵 Suggestion] 蒸留への申し送り (concepts の陳腐化) が 3 サイクル通じて未記録

**該当箇所**: `kasane/concepts/cross/conventions/test-execution.md` / `kasane/concepts/cross/conventions/sample-parity.md`

**問題点**: review-001 / review-002 で挙げた申し送りが `deviation.md` にも他の成果物にも入っていない。両 review ファイルは change 配下に残るため蒸留時に読まれる見込みはあるが、本サイクルの実測でさらに数値が動いた:

- `test-execution.md` の件数表: ios 158→**212** / android 66→**67** / instrumented 139→**222** / kmp 57→**80** / maui 88→**110** / bridge 15→**25** / 負の検査 31→**37** 本
- `test-execution.md` の罠: 「`maui/android/native/` は `android/` の複合ビルドを含むため、`android/` のタスクと同時に実行しない」(review-002 で実測、本サイクルも逐次で回避)
- `sample-parity.md` の文言表: 「現在のデモ項目は 7 件」のまま。`Default Loading` / `Custom Loading` の 2 件と付随する文言が未反映

**推奨修正**: 実装側で今すぐ直す必要はない (concepts の追随は `ksn-distill` の責務)。上記 3 点を 1 か所にまとめて申し送りとして残すこと。

---

### [🔵 Suggestion] Swift 互換面に同型テストを置けない事情が記録されていない

**該当箇所**: `maui/macios/native/KsDialogsMauiBridge/` (テストターゲットが存在しない)

**問題点**: review-002 の Suggestion がそのまま残っている。`maui/macios/native/` にはテストターゲットが 1 つも無く、Dialog 面も Kotlin 側だけという既存の構図と同じであるため妥当な帰結だが、`deviation.md` にその一言が無いままである。読み手は「片側だけ手を抜いた」と誤読しうる。

**推奨修正**: `deviation.md` の `LoadingActionRunner` の項に「Swift 互換面にはテストターゲットが存在せず (Dialog 面も同じ)、両 OS が共有する順序保証は C# 側の `LoadingActionRunnerTests` で担保する」を追記する。

## アクションプラン

1. **[Minor]** Android に「報告の直後に終了しても最終進捗が追い越されない」の instrumented テストを 1 本足す (優先度低。完了を妨げない)
2. **[Suggestion]** 蒸留への申し送り (件数表・Gradle 同時実行の罠・Sample デモ項目表) をまとめて残す
3. **[Suggestion]** Swift 互換面にテストターゲットが無い事情を `deviation.md` に追記する

## 確認した観点 (指摘に至らなかったもの)

- **`LoadingReportQueue` の順序保証**: `enqueue` は錠の下で末尾を付け替え、新しい仕事は必ず直前の仕事の完了を待つ。`drain()` は末尾だけを待てば鎖全体を待ち切れる。積む側が任意スレッドでも順序は enqueue の呼び出し順に固定される。錠の中で `Task` を作るが、その仕事は錠に触れないためデッドロックしない
- **取り消し下での `drain()`**: 積まれるのは非構造の `Task` で、`Task {}` は呼び出し元の取り消しを引き継がない。呼び出し元が取り消された `catch` 経路でも受理は完走し、`drain()` は必ず戻る (`Task<Void, Never>.value` は取り消しで throw しない)。取り消し時に終了を数え損ねる経路は見つからなかった
- **2 つの待ち行列の使い分け**: `Loading.runScope` はスコープごとに新しい行列を持ち終了は行列の外で呼ぶ / 互換面 (`KsDialogsInteropLoadingBridge`) は共有の行列に報告と終了の両方を載せる。この非対称は妥当で、`IosLoadingGateway.runScope` が「報告は fire-and-forget、終了は completion 待ち」という別々の呼び出しで来るため、互換面では両者を同じ鎖に載せないと順序が決まらない。逆に `runScope` 側でスコープごとに分けているのは、終了が撤去の完了を待つ間に別の報告が頭で詰まるのを避ける形になっている
- **互換面の他の操作 (`hide` / `setMessage`) が鎖に載っていないこと**: どちらも共有コード側 (`IosLoadingGateway`) で completion を待つ suspend 呼び出しであり、連続呼び出しは本質的に直列になる。行列に載せる必要は無く、後勝ちが崩れる経路は見つからなかった
- **契約変更の有無**: 互換面の終了通知が発行済みの報告の後に届くようになった点だけが観察可能な差分で、`deviation.md` 13 行目に記録済み。デルタスペックは「最新は状態直列化の受理順」としか定めておらず (`specs/dialog-contract/spec.md:21` / `:134`)、本サイクルの変更はその契約を**強める**方向で、緩める箇所は無い
- **Android の遅延生成失敗の扱い**: `catch (contentFailure: Exception)` へ限定し `Log.w(LOG_TAG, …)` を残す形は、`DialogTransitionRunner.kt:140` の既存の作法 (同じ `LOG_TAG` "KsDialogs"・同じ Log.w) と一致している。Error 伝播時に `abandonContent()` を呼ばないため次の入れ替わりで再試行されるが、これは「隠さず伝える」判断と整合する。器・取り付け先の控えは try の前に落としてあり、状態は破綻しない
- **Sample パリティ**: KMP 側と android 側の `CustomLoadingCardView.kt` は package / import / KDoc 1 行以外が完全一致。幅が 0 のまま複数回 `render` が呼ばれると `OnLayoutChangeListener` が複数登録されるが、登録順に発火して最後 (= 最新の進捗) が勝つため見えは正しく、両ルートで同一である
- **PB-TR-21 の出現待ち**: `DialogTransition.none()` でも覆いのフェードは走るため `.presenting` は既定の覆い時間だけ持続し、5 ms 間隔・5 秒上限の待ちが取りこぼす窓は無い。追加した待ちが本来の検出対象 (「覆いの出現中はまだ退出しない」) を覆い隠していないことも、直後の `#expect` が `.presenting` を主張する形から確認した。テスト専用の変更で本体挙動には触れていない
- **付随修正の同梱条件**: 本サイクルの追加は iOS 新規 1 (internal) + `Loading.swift` / 互換面 1 の書き換え + テスト、Android は `LoadingCoordinator.kt` の catch 1 箇所 + テスト 1 本、Sample 1 ファイル、テスト専用 1 箇所。公開面に触れず、いずれもテストで担保されており、ksn-core の同梱条件を超えていない
- **合意済み差分**: `deviation.md` 11 件 (付随修正 7 件を含む) と `ui/brief.md` の追記 (照合結果とオーナー指示の 2 点) は違反として扱っていない
- **セキュリティ / 機微情報**: 本サイクルの差分に認証・ネットワーク・永続化の面は無い。3 種の lint はすべて 0 件
