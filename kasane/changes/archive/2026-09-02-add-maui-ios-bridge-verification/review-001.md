# レビュー結果: add-maui-ios-bridge-verification (001 回目)

**日付**: 2026-09-02
**判定**: APPROVED

## サマリー

デルタスペックの 7 Scenario すべてに実装・テストまたは実測記録が対応しており、いずれも VALID と判定した。bridge テスト標的はレビュー環境で再実行して `Test run with 6 tests in 3 suites passed` を確認し、ホストアプリとテスト標的が binding の xcframework 生成へ混入しないこと (scheme の BuildAction が framework 単独であること・framework 単独ビルドのログにテスト標的が現れないこと) も実測で確認した。ビルド連携の 2 か所は SDK の該当 target / item を実物の targets ファイルで照合し、コメントの主張 (資源パッケージの中身は変わらない・`_ComputeLinkNativeExecutableInputs` の後で入力補正が効く) が正しいこと、`_AddKsBridgeBinaryToBindingResourceInputs` が実際に 2 スライスの Mach-O を `_FileNativeReference` へ足していることを診断ビルドで確認した。指摘は Minor 2 件・Suggestion 1 件で、いずれも実装の正しさではなく検出力と記録の網羅にかかるもの。

## 照合した規約

| 文書 | 適用のきっかけ |
|---|---|
| handbook/cross/comment-policy.md | 常時 (always)。Swift / csproj / xcscheme のコメントを追加している |
| handbook/cross/test-execution.md | テストの実行・件数の報告・完了判定を行っている |
| handbook/cross/runtime-behavior-verification.md | 症状 (古いネイティブ成果物が配備される) が実行時成果物にしか現れない事象で、修正前後の A/B を完了判定に使っている |
| kasane/lessons/process.md (L-001) | 姉妹面の照合。本変更は MAUI Android の同役テスト (`maui/android/native/ksdialogs-maui-bridge/src/test/**/MauiToastContentSupplyTests.kt` 等) の iOS 側ミラーを新設するもので、観測点を Android 側より 1 段強く取っている旨が tasks 2.4 に明記されている |
| kasane/lessons/code-review.md | 不在 (重点観点・指摘しないことの指定なし) |

参照した決定: maui/ADR-0003 (SDK 内部ターゲットへの依存)、cross/ADR-0006 (ソースツリー参照の開発構成)、core/ADR-0033 (中身の供給失敗の届き方)、core/ADR-0016 (Scenario ID とテスト名)。

規約適合の判定:

- comment-policy: `python3 scripts/comment-policy-lint.py` = 禁止 0 件 (917 ファイル)。新規ファイルを手で読み直しても、外部参照は ADR の ID 形式 (`core/ADR-0033` / `maui/ADR-0003` / `cross/ADR-0006`) と リポジトリ内コード識別子のみで、作業文書のパス・変更 ID・`SHALL` 等の仕様構文キーワードは無い。テスト名の `[BV-MA-01]` は core/ADR-0016 の Scenario ID 規約に基づく宣言であってコメントではない
- test-execution: 件数の読み方 (Swift Testing 行を読む・XCTest 行は 0 件に見える) が `verification/bridge-test-target.md` に明記され、0 件の偽 green を排除している。handbook の件数表そのものの更新は proposal で蒸留へ申し送られており、値は `verification/` に揃っている
- runtime-behavior-verification: `verification/incremental-build.md` の B1 / B2 が「手当てを外すと症状が再現し、入れると解消する」A/B になっており、証跡が change 配下にある

## 検証 (ksn-verify 兼務): Scenario 対応表

| Scenario | 実装 | テスト / 実測記録 | 判定 |
|---|---|---|---|
| BV-MA-01 Dialog の中身なし供給 | `maui/macios/native/KsDialogsMauiBridge/MauiDialogBridge.swift:66-70` (失敗を閉鎖の通知へ) / `MauiDialogViewModel.swift` (nil で失敗を投げる) | `maui/macios/native/KsDialogsMauiBridgeTests/MauiBridgeContentSupplyTests.swift:21-41` | VALID |
| BV-MA-02 Loading (表示形) | `maui/macios/native/KsDialogsMauiBridge/MauiLoadingBridge.swift:62-72` | 同 `:43-62` | VALID |
| BV-MA-07 Loading (スコープ形) | 同 `:82-97` | 同 `:64-92` | VALID |
| BV-MA-03 Toast | `maui/macios/native/KsDialogsMauiBridge/MauiToastBridge.swift:40-53, 74-81` | 同 `:94-128` | VALID (指摘 1 の注記付き) |
| BV-MA-04 変更後の追随 | `maui/macios/KsDialogs.Binding.iOS/KsDialogs.Binding.iOS.csproj:111-118` / `samples/maui/KsDialogs.Sample.Maui/KsDialogs.Sample.Maui.csproj:52-77` | `verification/incremental-build.md` A5 (シンボル 0→2)・B1/B2 (手当てなしでスキップ・据え置き)・D1/E4 (Binding 側の切り分け) | VALID |
| BV-MA-05 未変更時のスキップ | 同上 | `verification/incremental-build.md` A2 / A6 (Xcode・資源パッケージ・ネイティブリンクともスキップ、2〜4 秒台) | VALID (指摘 2 の注記付き) |
| BV-MA-06 テスト標的の全件実行 | `maui/macios/native/KsDialogsMauiBridge.xcodeproj/project.pbxproj` / 同 `xcshareddata/xcschemes/KsDialogsMauiBridge.xcscheme:36-47` | `verification/bridge-test-target.md` + 本レビューでの再実行 (`Test run with 6 tests in 3 suites passed` / `Executed 0 tests` は XCTest 側 / `** TEST SUCCEEDED **`) | VALID |

Scenario ID の網羅: `python3 scripts/scenario-id-coverage.py` = 「未網羅なし」(BV-MA 4/7 網羅・除外 3 件)、`--selftest` 全件 OK。spec 冒頭の取り決めどおり BV-MA-01/02/03/07 は除外に入れず必須網羅として通っている。

補足 (対応表の読み方):

- BV-MA-03 の THEN の「受理は失敗せず」は、`MauiToastBridge.show` が戻り値を持たず `try?` で受けるため公開面の形として構造的に成立しており、テストの観測点ではない。テストが観測しているのは残る 2 つ (その 1 枚だけが破棄される・表示中の別の Toast が残る)
- BV-MA-04 / 05 は spec 冒頭の取り決めどおり `verification/` の実測記録で受け入れた。記録は実行コマンド・観測点 (3 つ)・一時変更の中身・シンボルの見え方まで再現可能な形で残っており、A/B が付いている点も含めて受け入れに足る

## 指摘事項

### [🟡 Minor] BV-MA-03 だけ「器が増えない」を待ち条件と同じ式で確かめており、settle 待ちが無い

**該当箇所**: `maui/macios/native/KsDialogsMauiBridgeTests/MauiBridgeContentSupplyTests.swift:114-123`

**問題点**: `try #require(await BridgeTestWaiting.waitUntil { overlays.added.count == 2 })` の直後に `#expect(overlays.added.count == 2)` を置いているが、これは待ち条件と同一の式であり、追加の検出力を持たない。受理は `ToastAcceptanceQueue` で直列化され、鎖の各段に `await previous?.value` の中断点があるため、「中身なしの 1 枚も器を作る」退行時の枚数は 1 → 2 (失敗分) → 3 と遷移する。5 ms 間隔のポーリングがその中間の 2 を捉えた場合、`added.first === firstContainer` も成立するため require も expect も通り、テストが green になる余地がある。`verification/bridge-test-target.md` の検出力確認では実際に fail しており実害は観測されていないが、成立がタイミング依存である点は他の 3 本 (`:39-40` `:60-61` `:90-91` が `settleTimeout` で「後から追加で届かない」を見ている) と非対称。

**推奨修正**: 枚数が 2 に達した後に `Self.settleTimeout` 相当の待ちを 1 つ挟み、その上で `overlays.added.count == 2` と `added.first === firstContainer` を見る (他の 3 本と同じ形にする)。退行時は settle 後に 3 枚となり確実に fail する。

### [🟡 Minor] BV-MA-05 の字面と実測 (資源パッケージが 1 ビルドおきに再実行) の食い違いが deviation.md に無い

**該当箇所**: `verification/incremental-build.md:98-103` / `specs/maui-binding/spec.md` の Scenario BV-MA-05

**問題点**: Scenario BV-MA-05 の THEN は「binding の資源パッケージ再生成と Sample のネイティブリンクは実行されず (up-to-date でスキップ)」と両方のスキップを求めているが、実測では資源パッケージだけが変更の有無によらず 1 ビルドおきに実行される。本レビューでも binding 単体ビルドで `IncrementalClean` が `KsDialogs.Binding.iOS.resources.stamp` を削除し、次のビルドで `出力ファイル "…resources.stamp" は存在しません` として実行される様子を再現できたので、記録の分析 (SDK 既定であり本変更が持ち込んだものではない) は正しい。Requirement 本文の「従来どおりスキップされる」「毎回の強制再生成にしない」の趣旨も満たしている。問題は記録の置き場で、`deviation.md` には付随修正 1 件しか無く、この食い違いは `verification/` の注意書きにしか無い。ksn-core の意味論では「deviation.md に記録済みの乖離だけが合意済みの差分」であるため、このままだと後続の verify / 蒸留が Scenario の字面と実測の差を未記録の乖離として扱う余地が残る。

**推奨修正**: `deviation.md` に spec からの乖離として 1 行残す (足場である spec.md は凍結のまま触らない)。例: 「BV-MA-05 の THEN のうち資源パッケージ再生成のスキップは成立しない。SDK が出力 stamp を `FileWrites` に載せず IncrementalClean が消すため、手当ての有無によらず 1 ビルドおきに実行される (実測: verification/incremental-build.md)。ネイティブリンクのスキップは成立する」。spec の字面のほうを直すべきと考える場合は実装では解決できないためオーナー判断へ回す。

### [🔵 Suggestion] Sample 側フィルタが「実バイナリであること」までは縛れていない

**該当箇所**: `samples/maui/KsDialogs.Sample.Maui/KsDialogs.Sample.Maui.csproj:73-75`

**問題点**: `Condition="'%(Filename)' == 'KsDialogsMauiBridge'"` は、`_FrameworkNativeReference` の項目が実バイナリ (`…/KsDialogsMauiBridge.framework/KsDialogsMauiBridge`) でもディレクトリ (`…/KsDialogsMauiBridge.framework`) でも同じく真になる。「変わったときだけ再リンクする」という本手当ての性質は、SDK がこの item を実バイナリとして解決していることに依存しているが (SDK 側は `@(NativeReference -> '%(Identity)/%(Filename)')` と `ResolveNativeReferences` の出力)、コメントと `verification/incremental-build.md` は「自分がディレクトリではなく実バイナリを足す」ことは書いていても、「`_FrameworkNativeReference` の項目自体が実バイナリである」という前提は明示していない。SDK 更新でここがディレクトリに変わると、条件は通ったまま毎回再リンク (ビルドが遅くなるだけで成功する) へ静かに転ぶ。

**推奨修正**: `verification/incremental-build.md` の「依存した SDK の内部要素」の表か注に、`_FrameworkNativeReference` が framework 内の実バイナリのパスであること (それゆえディレクトリ判定にならないこと) を 1 行足す。SDK 更新時の見直しで確認すべき点が特定できる。条件式そのものへ `'%(Extension)' == ''` を足す案は、外れたときに「毎回再リンク」ではなく「古いバイナリが配備される」= 本変更が塞いだ症状の再発側へ倒れるため採らないほうがよい。

## 確認したが指摘に至らなかった観点

- **ホストアプリ・テスト標的の混入**: scheme の BuildActionEntries は framework 1 件のみ (`:15-30`)。`xcodebuild build -scheme KsDialogsMauiBridge -destination 'generic/platform=iOS Simulator'` のログに `TestHost` / `KsDialogsMauiBridgeTests` は 1 件も現れない。テスト標的側は `PBXTargetDependency` で framework とホストアプリに依存する向きだけを持ち、逆向きの依存は無い
- **付随修正の同梱条件**: `deviation.md` の 1 件 (`_AdjustKsBridgeXcodeProjectInputs` からテスト標的・ホストアプリのソースを除外) は、本務で触る同じ csproj・2 行・公開 API と ADR に触れない・`verification/incremental-build.md:148-156` の touch 実測で担保、と ksn-core の同梱条件をすべて満たす。スコープ膨張は無い
- **足場の凍結**: `tasks.md` の差分はチェックボックスのみで本文の書き換えは無い。`proposal.md` / `specs/` は未変更
- **SDK 内部要素の照合**: `_CreateBindingResourcePackage` の Inputs に `@(_FileNativeReference)` があり、タスク `CreateBindingResourcePackage` の入力は `@(NativeReference)` であること (= 資源パッケージの中身は変わらない)、`_LinkNativeExecutable` が `_ComputeLinkNativeExecutableInputs` を DependsOnTargets に持つこと (= AfterTargets の補正が入力判定に間に合う) を SDK の targets で確認した。診断ビルドで `_AddKsBridgeBinaryToBindingResourceInputs` が `_CreateBindingResourcePackage` の依存として走り、xcframework の 2 スライス分の Mach-O を `_FileNativeReference` に追加していることも確認済み
- **テスト間の状態の分離**: 外側 suite が `.serialized` で、Toast / Loading の器は `BridgeTestOverlayObserver` が観測開始時点の顔ぶれを控える形になっており、提示機構が残す中間 View と切り分けられている。提示先の probe テストは末尾で `dismiss` と撤去の完了まで待つため、後続テストの `presentedViewController == nil` を汚さない
- **失敗の判別**: `BridgeTestFailure` が内部型を参照せず domain の接尾辞と説明文で照合している点は、`MauiDialogBridgeError` が internal な enum (code はケースの並び順に依存する) であることを踏まえると妥当。`unsupportedResult` とも区別できている
- **成果物のパス・個体特定値**: `python3 scripts/local-path-lint.py` / `identity-lint.py` とも exit 0。標準 lint は追跡済みファイルを走査するため、未追跡の新規ファイル (テストのソース・`verification/` の 2 ファイル・`deviation.md`) を手で grep したが、ローカル絶対パス・Simulator の UDID・個人を特定する値は無い

## アクションプラン

1. (Minor) `deviation.md` に BV-MA-05 の資源パッケージ再生成についての乖離を 1 行追記する — 記録だけで、コードの変更は不要
2. (Minor) BV-MA-03 のテストに settle 待ちを挟み、他の 3 本と検出力を揃える
3. (Suggestion) `verification/incremental-build.md` に `_FrameworkNativeReference` が実バイナリである前提を 1 行足す

1 と 3 は蒸留前に済ませておくと、後続の verify・蒸留・SDK 更新時の見直しで迷いが出ない。2 は本変更の範囲で直せるが、優先度は低く、次に bridge のテストを積む change に回してもよい。
