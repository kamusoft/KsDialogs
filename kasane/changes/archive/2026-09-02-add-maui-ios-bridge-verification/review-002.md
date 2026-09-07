# レビュー結果: add-maui-ios-bridge-verification (002 回目)

**日付**: 2026-09-02
**判定**: APPROVED

## サマリー

修正サイクル 1 の再確認。review-001 の Minor 2 件・Suggestion 1 件と second-opinion-code-001 の採用 2 件は**すべて解消**しており、同じ指摘の残存はない。新設の `BridgeTestHostWindow` による取り付け履歴の観測は、production の提示先解決規則 (`ios/Sources/KsDialogs/Presentation/ApplicationKeyWindowProvider.swift:23-28`) と同じ規則で選んだ window に対して行われ、観測できないときは値の不在として `#require` で弾く形になっているため、偽陰性を「取り付けなし」に読み替える経路は塞がっている。レビュー環境で bridge 標的を再実行して `Test run with 6 tests in 3 suites passed` / `** TEST SUCCEEDED **`、framework 単独ビルドが `** BUILD SUCCEEDED **` かつログにテスト標的・ホストアプリが 0 件であること、lint 3 本と scenario-id-coverage が全 exit 0 であることを実測で確認した。新規の指摘は Suggestion 2 件のみで、いずれも今回の成立には影響しない (今後テストを積むときの土台の耐性)。

## 照合した規約

| 文書 | 適用のきっかけ |
|---|---|
| handbook/cross/comment-policy.md | 常時 (always)。新規 Swift 3 ファイル・xcscheme に doc / ブロックコメントを追加している |
| handbook/cross/test-execution.md | テストの実行・件数の報告・完了判定を行っている |
| handbook/cross/runtime-behavior-verification.md | 器の取り付けという実行時にしか現れない事象を、退行を作った A/B で完了判定している |
| kasane/lessons/process.md (L-001) | 姉妹面照合と「修正前に fail すること」。今回の追加観測 (取り付け履歴) についても検出力の再確認が `verification/bridge-test-target.md:69-98` に取ってある |
| kasane/lessons/code-review.md | 不在 (重点観点・指摘しないことの指定なし) |

参照した決定: core/ADR-0033 (中身の供給失敗の届き方)、core/ADR-0016 (Scenario ID とテスト名)、maui/ADR-0003 (SDK 内部ターゲットへの依存)、cross/ADR-0006 (ソースツリー参照の開発構成)。

規約適合の判定:

- **comment-policy**: `python3 scripts/comment-policy-lint.py` = 禁止 0 件 (919 ファイル、前回から +2)。機械検査は規約より狭いため新規 3 ファイル (`BridgeTestHostWindow.swift` / `BridgeTestSharedHost.swift` / 差し替えた `SceneDelegate.swift`) を手で読み直した — 外部参照は無く、履歴記述・仕様構文キーワード・作業文書の参照も無い。`BridgeTestHost.swift:1-2` の `@testable import` の理由コメントも自己完結している
- **test-execution**: 件数の 2 系統 (Swift Testing 行と XCTest 行) の読み分けが `verification/bridge-test-target.md:17-26` に明記され、単体指定の `()` も同 `:96-98` で守られている。完了判定は全件実行 (絞り込みなし) で取っている
- **runtime-behavior-verification**: 「取り付いた直後に撤去される」退行は互換面側から作れないため、テストへ一時的に数行を入れて旧観測が素通りし新観測だけが落ちることを確かめており (`verification/bridge-test-target.md:82-98`)、緑になったことではなく落ちることを見て検出力を判定している

## 検証 (ksn-verify 兼務): Scenario 対応表

| Scenario | 実装 | テスト / 実測記録 | 判定 |
|---|---|---|---|
| BV-MA-01 Dialog の中身なし供給 | `maui/macios/native/KsDialogsMauiBridge/MauiDialogViewModel.swift:32-35` (nil で `contentUnavailable` を投げる) / `MauiDialogBridge.swift:23,44` (失敗を閉鎖の通知へ) | `maui/macios/native/KsDialogsMauiBridgeTests/MauiBridgeContentSupplyTests.swift:21-41` | VALID |
| BV-MA-02 Loading (表示形) | `maui/macios/native/KsDialogsMauiBridge/MauiLoadingBridge.swift:63-81, 222-225` | 同 `:43-63` (取り付け履歴で「一度も取り付かない」を見る) | VALID |
| BV-MA-07 Loading (スコープ形) | 同 `:83-118` | 同 `:65-94` (処理の未実行 + 取り付け履歴) | VALID |
| BV-MA-03 Toast | `maui/macios/native/KsDialogsMauiBridge/MauiToastBridge.swift:41-73, 75-78` | 同 `:96-139` (settle 待ち + 履歴 2 枚 + 現存 2 枚 + 同一性) | VALID |
| BV-MA-04 変更後の追随 | `maui/macios/KsDialogs.Binding.iOS/KsDialogs.Binding.iOS.csproj:86-118` / `samples/maui/KsDialogs.Sample.Maui/KsDialogs.Sample.Maui.csproj:51-79` | `verification/incremental-build.md` A5 (シンボル 0→2) / B1・B2 (手当てなしで症状再現) / D1・E4 (Binding 側の切り分け) | VALID |
| BV-MA-05 未変更時のスキップ | 同上 | `verification/incremental-build.md` A2 / A6 と `:96-111`。資源パッケージ再生成の 1 ビルドおきの再実行は `deviation.md` に合意済みの差分として記録済み | VALID (deviation 適用) |
| BV-MA-06 テスト標的の全件実行 | `maui/macios/native/KsDialogsMauiBridge.xcodeproj/project.pbxproj` (host / test 2 target・`TEST_HOST` / `BUNDLE_LOADER`) / `xcshareddata/xcschemes/KsDialogsMauiBridge.xcscheme:32-49` | `verification/bridge-test-target.md` + 本レビューでの再実行 | VALID |

Scenario ID の網羅: `python3 scripts/scenario-id-coverage.py` = 「未網羅なし」(BV-MA 4/7・除外 3 件)、`--selftest` 全件 OK。spec 冒頭の取り決めどおり BV-MA-01/02/03/07 は除外に入らず必須網羅として通っている。

本レビューでの実測:

| 観測 | 値 |
|---|---|
| `xcodebuild test -project KsDialogsMauiBridge.xcodeproj -scheme KsDialogsMauiBridge -destination 'platform=iOS Simulator,name=iPhone 17'` | `Test run with 6 tests in 3 suites passed` / XCTest 側 `Executed 0 tests, with 0 failures` / `** TEST SUCCEEDED **` (exit 0) |
| `xcodebuild build -project KsDialogsMauiBridge.xcodeproj -scheme KsDialogsMauiBridge -destination 'generic/platform=iOS Simulator'` | `** BUILD SUCCEEDED **`。ログ中の `TestHost` / `KsDialogsMauiBridgeTests` は **0 件** (配布物のビルドに混ざらない) |
| `local-path-lint.py` / `identity-lint.py` / `comment-policy-lint.py` | すべて exit 0 (禁止 0 件 / 919 ファイル) |
| `scenario-id-coverage.py` (既定 / `--selftest`) | 未網羅なし / 全件 OK |

## 前回指摘の解消状況 (収束判定)

| 出所 | 指摘 | 状況 |
|---|---|---|
| review-001 Minor 1 | BV-MA-03 が待ち条件と同じ式で確かめており settle 待ちが無い | **解消**。`MauiBridgeContentSupplyTests.swift:121-125` に `settleTimeout` の待ちが入り、他の 3 本と形が揃った。さらに履歴 (`attached.count == 2`) が加わり、途中の並びを捉えた場合でも 3 枚目が確実に露見する |
| review-001 Minor 2 | BV-MA-05 の字面と実測の食い違いが deviation.md に無い | **解消**。`deviation.md` に乖離として 1 行記録され、理由と実測の出どころ (`verification/incremental-build.md`) が付いている。足場 (`specs/maui-binding/spec.md`) は凍結のまま |
| review-001 Suggestion | `_FrameworkNativeReference` が実バイナリである前提が記録に無い | **解消**。`verification/incremental-build.md:30-36` に前提と、SDK 更新でディレクトリに転んだときの倒れ方まで明記された |
| second-opinion Minor 2 | 器の瞬間的な取り付けを検出できない | **解消**。`BridgeTestHostWindow` が `didAddSubview` で履歴を控え、`BridgeTestOverlayObserver.attachedEver` が baseline を除いた履歴を返す。BV-MA-02/07 は `attached.isEmpty`、BV-MA-03 は `attached.count == 2` を見る形へ変わった |
| second-opinion Minor 3 | 共有ホスト状態の後片付け失敗が見逃される | **解消**。`BridgeTestSharedHost.run` が本体と片付けを組み立て、成功時は片付けの完了を `#require` で検証する。片付け待ちの戻り値を捨てている箇所は残っていない |

**同じ指摘の残存はない。**

## 指摘事項

### [🔵 Suggestion] 本体が失敗したときだけ、片付けの失敗が痕跡を残さず捨てられる

**該当箇所**: `maui/macios/native/KsDialogsMauiBridgeTests/Support/BridgeTestSharedHost.swift:22-26`

**問題点**: 本体が投げた経路では `_ = await cleanup()` として戻り値を捨てている。元の失敗を隠さない (片付けの失敗で上書きしない) 判断自体は正しいが、結果として「本体が失敗し、かつ片付けにも失敗した」ときだけ共有の提示先が汚れたまま無記録で次のテストへ渡る。second-opinion で採用した指摘 (順序依存の失敗に化ける) が、成功経路からは消えたものの失敗経路には残っている形になる。実害は退行が既にある実行に限られ、今回の緑には影響しない。

**推奨修正**: catch 側で `cleanup()` が false を返したときに `Issue.record` 相当で「共有の提示先が片付かなかった」ことを記録し、その上で元の失敗を投げ直す。元の失敗は throw のまま残るので隠蔽は起きず、後続テストの巻き添えの出どころが 1 行で分かるようになる。

### [🔵 Suggestion] 取り付け履歴が共有され、観測を 2 つ重ねると先の履歴が消える前提が書かれていない

**該当箇所**: `maui/macios/native/KsDialogsMauiBridgeTests/Support/BridgeTestOverlayObserver.swift:17-22` / `maui/macios/native/KsDialogsMauiBridgeTestHost/BridgeTestHostWindow.swift:18-21`

**問題点**: 履歴は提示先の window が 1 つだけ持ち、`BridgeTestOverlayObserver` は init で無条件に `resetAddedSubviewHistory()` を呼ぶ。したがって 1 つのテストの中で観測を 2 つ作る、あるいは要求を出した後に観測を作り直すと、先に控えた取り付けが黙って消え、`attachedEver` が空になって**偽陰性側**へ倒れる (テストは緑のまま退行を見逃す)。今のテスト 4 本はいずれも「テスト 1 本につき観測 1 つを要求の前に作る」形で成立しており、現時点で不具合はない。ただし本 change の Non-Goals は残りの結び目のテストを後続 change で積むと宣言しており、この土台はそのまま使われる。

**推奨修正**: `BridgeTestOverlayObserver` の doc コメントに「観測は 1 つのテストにつき 1 つだけ、要求を出す前に作る (履歴は提示先の window が 1 つだけ持つため、作り直すと先の履歴が消える)」旨を 1 行足す。型の側で守らせるなら、履歴を観測ごとの通し番号で切る (reset ではなく開始位置を控える) 形も採れる。

## 確認したが指摘に至らなかった観点

- **`@testable import KsDialogsMauiBridgeTestHost` の構成**: テスト標的がホストアプリを `@testable` で取り込むのは、Host Application を持つユニットテスト標的の標準構成の範囲。依存の向きはテスト → ホスト → (無し) の一方向で、ホストアプリ側は bridge も ios/ の Swift パッケージも参照しておらず (`project.pbxproj` の TestHost target は Sources 1 phase のみ)、配布物のビルドにも混ざらない (framework 単独ビルドで実測 0 件)。`@testable` が効くのは `ENABLE_TESTABILITY = YES` の Debug だけだが、scheme の TestAction は `buildConfiguration = "Debug"` に固定されており、その依存関係は `verification/bridge-test-target.md:110-112` に記録済み
- **履歴観測の偽陽性**: `attachedEver` は baseline (観測開始時点の顔ぶれ) を除くため、提示機構が作って居座る中間 View は数えない。BV-MA-02/07 の `attached.isEmpty` は「key window に何も足されない」という強い述語だが、観測窓はそのテストの要求から assert までに限られ、直列化と片付けで前のテストの残りが入り込まない構造になっている。再実行でも 4 本とも緑
- **履歴観測の偽陰性**: 「今の顔ぶれ」は「観測開始後に一度でも取り付いたか」の部分集合なので、旧観測 (`added`) を履歴に置き換えたことによる検出力の低下はない。履歴を持つ window が提示先として解決できないときは `attachedEver` が nil になり `#require` で落ちるため、観測不能を「取り付けなし」と読み替える経路も無い。残る穴は「器が key でない別 window に載る」退行だけで、これは互換面が提示先を key window として解決する限り作れない
- **観測点と production の一致**: `BridgeTestHost.keyWindow` の絞り込み (前面アクティブなシーン → `isKeyWindow`) は `ApplicationKeyWindowProvider.selectKeyWindow` (`ios/Sources/KsDialogs/Presentation/ApplicationKeyWindowProvider.swift:23-28`) と同じ規則。テストが覗く window と互換面が提示先に選ぶ window がずれていない
- **片付けが元の失敗を隠さないか**: `BridgeTestSharedHost.run` は catch で片付けてから元のエラーを `throw` し直しており、失敗の差し替えは起きない (上の Suggestion は隠蔽ではなく無記録の話)
- **BV-MA-01 が提示 handle を捨てている点** (`MauiBridgeContentSupplyTests.swift:27`): 緑の経路ではダイアログが提示されないため共有状態は汚れない。退行時にだけ閉じないダイアログが残り提示先 probe を巻き添えにするが、その巻き添えは検出力の記録 (`verification/bridge-test-target.md:66-67`) に明示されており、退行が既にある実行でのみ起きる。指摘に足る実害ではないと判断した
- **静的 framework と Swift パッケージの二重リンク**: テスト標的は `KsDialogsMauiBridge.framework` (`MACH_O_TYPE = staticlib`) をリンクしつつ `KsDialogs` パッケージ product も別に持つ。静的 framework は依存を取り込まないため、この二重指定は重複ではなく最終消費者側での解決として必要なもの。リンクは重複シンボルなしで通っている
- **付随修正の同梱条件**: `deviation.md` の 1 件 (テスト標的・ホストアプリのソースを `_AdjustKsBridgeXcodeProjectInputs` から除外) は前回同様、本務で触る同じ csproj・2 行・公開 API と ADR に触れない・touch 実測 (`verification/incremental-build.md:156-164`) で担保。今回の差分でも増えていない
- **足場の凍結**: `tasks.md` の差分はチェックボックスのみ (本文の書き換えなし)。`proposal.md` / `specs/maui-binding/spec.md` は未変更。`verification/presentation-host-probe.md` は追記のみで既存節を書き換えていない
- **BV-MA-05 の乖離**: `deviation.md` 記録済みの合意済み差分として扱い、違反として指摘していない (ksn-core のデルタスペック意味論)

## アクションプラン

1. (Suggestion) `BridgeTestSharedHost.run` の catch 側で片付けの失敗を記録する — 元の失敗は throw のまま
2. (Suggestion) `BridgeTestOverlayObserver` に「1 テスト 1 観測・要求の前に作る」前提を 1 行残す

どちらも本変更の成立には影響しない。2 は後続 change で bridge のテストを積み増す前に済ませておくと、土台の使い方を読み違えた偽陰性を予防できる。1 と併せて蒸留前に入れてもよいし、次の bridge テストの change に回してもよい。
