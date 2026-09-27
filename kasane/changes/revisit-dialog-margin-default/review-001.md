# レビュー結果: revisit-dialog-margin-default (001 回目)

**日付**: 2026-09-27
**判定**: APPROVED

## サマリー

余白の契約既定値を全辺 0 にする変更は、3 形態の契約既定値、MAUI の 2 つの橋渡し、Android の `DEFAULT_MARGIN` の二重定義の解消、共通ケース表 C19/C23/C24/C26 の再導出 (規則から検算して一致) まで漏れなく入っている。既定 Toast の余白 24 は両 Native の `ToastDefaultContentView` が自分に添付する形で実装した。これは deviation に記録済みで、どの配置経路でも効くことを TS-AT-04〜07 が両 Native で固定している。Sample 4 ルートの `Margin` 行は、承認モック・文言表・読み上げの記録と一致している。
全形態のテストをレビュー側で再実行し、変更に起因する失敗が無いことを確かめた。Critical / Major は無い。指摘は、Sample コメントの履歴記述 (規約違反の Minor)、テスト件数の証跡が change に残っていないこと (Minor)、小さな改善提案 2 件にとどまる。

## 照合した規約

- comment-policy.md (always)
- test-execution.md (テストの実行・結果の報告・完了判定)
- sample-parity.md (`samples/` を触るとき)。同文書の「パネルが出すダイアログの余白 = 全辺 0」の行は、proposal の Impact どおり蒸留時に書き換える予定の箇所として扱った
- verification-ci.md (iOS の並列実行で出た時間切れの切り分け — スイート直列の記述)
- diagnostic-message-language.md (照合したが、診断文言の追加・変更は無し)
- lessons/code-review.md の重点観点 L-001 (TS-AT-05 の受け入れ条件が「余白の添付が無いと結果が変わるか」を満たしているか — 下の「確認した観点」)

## レビュー側の実行結果 (2026-09-27)

| ルート | 件数 | 結果 |
|---|---|---|
| ios/ (新規 iPhone 17 Simulator・iOS 26.5、既定の並列) | Swift Testing 321 tests | 3 件失敗 (TS-IO-02 / TS-TR-01 / TS-CO-01。どれも `waitUntilPresenting` の 44 秒時間切れで、この change の差分外の Toast スイート)。同じ 4 スイートを単独で再実行すると 23 tests すべて成功。直列実行 (`-parallel-testing-enabled NO`) では xctest の再起動を 1 回挟んで 230 tests 成功。エミュレータ 5 台と Gradle が並走していた負荷による時間切れと判断した (verification-ci.md の記述と同じ型)。変更で足した・直したテスト (TS-AT-04〜07、既定ローディングの末尾寄せ、LD-AT-01/02、無効値の正規化、C23) は並列の実行ですべて成功 |
| android/ `./gradlew test --rerun-tasks` | 75 tests / 0 failures | 成功 |
| android/ instrumented (新規に起動した read-only の API 35 AVD) | ksdialogs-core 356 tests / 0 failures / 1 skipped (API 30 以上で skip される旧経路の 1 件)。ksdialogs (Compose) 52 tests | 1 回目は起動直後のランチャーの ANR ダイアログが前面にあり、入力の注入で 2 件失敗 (環境要因)。ANR を閉じた 2 回目は 1 件失敗 (ComposeCurrentPageTests の最初の 1 件が、配置が落ち着かないまま 10 秒で時間切れ。`content=[0,0][0,0] laidOut=false`)。同クラスを単独で再実行すると 7/7 成功。1 回目では同じテストが成功している |
| kmp/ `./gradlew allTests --rerun-tasks` | iosSimulatorArm64Test 85 / testAndroidHostTest 81、0 failures | 成功 |
| maui/ `dotnet test` | 197 / 0 failures | 成功 |
| maui/android/native `:ksdialogs-maui-bridge:test --rerun-tasks` | 38 / 0 failures | 成功 |
| maui/macios/native (bridge) | 9 tests | 成功 |
| `scripts/scenario-id-coverage.py` (`--require-mirror` 含む) | — | 未網羅なし、両 Native のミラー OK |
| `comment-policy-lint.py` / `local-path-lint.py` / `identity-lint.py` / `doc-structure-lint.py` | — | 禁止 0 件 / exit 0 |

レビュー用に作った Simulator は削除し、AVD は終了した。

## 指摘事項

### 🟡 Minor: Sample の新規コメントに履歴記述 (「旧既定値」) がある
**該当箇所**: `samples/ios/KsDialogsSample/SampleMarginChoice.swift:3`、`samples/android/app/src/main/kotlin/jp/kamusoft/ksdialogs/samples/android/SampleMarginChoice.kt:6`、`samples/kmp/androidApp/src/main/kotlin/jp/kamusoft/ksdialogs/samples/kmp/android/SampleMarginChoice.kt:8`、`samples/kmp/iosApp/KsDialogsSampleKmp/SampleMarginChoice.swift:5`、`samples/maui/KsDialogs.Sample.Maui/SampleMarginSegmentsView.xaml.cs:6`
**問題点**: 「24 は旧既定値と既定 Toast の余白」の「旧既定値」は、アーカイブされた過去の仕様の説明にあたる。comment-policy.md の「禁止する記述類型 — アーカイブされた過去仕様の説明」(書き換え類型の「履歴記述型」) に当たる。lint は履歴記述を advisory でしか拾わないため、機械検査では出ない。
**推奨修正**: 現在の仕様だけで書く。例: 「0 は契約の既定値、24 は既定 Toast のデフォルト View が自分に持つ余白と同じ値」。5 ファイルとも同じ文言にそろえる (Sample パリティ)。

### 🟡 Minor: テストの実行件数が change の証跡に残っていない
**該当箇所**: `tasks.md` 4.6 (チェック済み)、`evidence/` が無い
**問題点**: test-execution.md は「実行した件数は、その変更の証跡 (`kasane/changes/<id>/evidence/`) に残す」と定めている。完了判定では、ルートごとの件数が 0 でないことと、足したテストが結果に現れていることを確かめる。4.6 はチェック済みだが、change 内に件数の記録が無い。ui/verification と brief.md にあるのは Sample の照合だけである。今回はレビュー側で全ルートを再実行して上表のとおり確かめたので、実害は小さい。
**推奨修正**: `evidence/` にルートごとの件数 (上表の値でよい) と、足したテスト名が結果に現れたことを記録する。

### 🔵 Suggestion: MAUI の `DialogMargin` の XML doc と宣言の間に行コメントが挟まっている
**該当箇所**: `maui/KsDialogs.Maui/Contract/DialogOptions.cs:29-31`
**問題点**: `/// <summary>` の直後、プロパティの前に `// 余白の既定は全辺 0 (core/ADR-0039)。` がある。ビルド警告 (CS1587) は出ず、doc も効いている。ただ、iOS と Android はこの根拠コメントを初期化子の直前に置いており、並びがそろっていない。
**推奨修正**: 行コメントを `<summary>` の前に移すか、プロパティ行の行末に置く。

### 🔵 Suggestion: Android の `ToastDefaultContentView` の companion を `internal` に広げる必要がない
**該当箇所**: `android/ksdialogs-core/src/main/kotlin/jp/kamusoft/ksdialogs/ToastDefaultContentView.kt:131-138`
**問題点**: companion を `private` から `internal` に変え、定数を個別に `private` にした。広げた `attachedOptions` はクラス外から参照されていない (テストは 24 を自前の定数で持つ)。iOS 側も `static let` (internal) なので実害はないが、公開範囲を広げる理由がコードから読めない。
**推奨修正**: companion を `private companion object` に戻し、`attachedOptions` も private にする。テストから参照する意図があるなら、現状のままでよい。

## 確認した観点 (指摘なし)

- **仕様充足**: dialog-contract の 2 Requirement / 10 Scenario と samples の 1 Requirement / 3 Scenario は、実装とテスト (Sample は照合記録) で満たされている (対応表は verify-001.md)。足場 (proposal / specs / exploration) は HEAD から変わっていない。tasks の 5.4 以外はチェック済みで、実装と対応が取れる (2.1 / 2.2 は deviation の 1 件目で差し替え)
- **deviation の 1 対 1 照合**: 1 件目 (添付箇所を `ToastDefaultContentView` の init に移した) は `ios/Sources/KsDialogs/Presentation/ToastDefaultContentView.swift:21,56` と `android/.../ToastDefaultContentView.kt:48,138` に一致する。どの経路から Toast を作っても、ビルトインの分岐はこの View を作るので余白が付く。`ToastCoordinator` を含め、`ksDialogOptions` を上書きする箇所はない。2 件目 (iOS の TS-AT-05 の許容差) は `ios/Tests/KsDialogsTests/ToastAttributeTests.swift:158-187` の許容差「1 字幅の半分 + 1」に一致し、Android は `DP_TOLERANCE` (1dp) でちょうど 24 を判定している
- **lessons L-001 (受け入れ条件の弁別力)**: TS-AT-05 は、可視領域を左右 100 ずつ狭めて余白の制約が最大幅 (80%) より先に効くようにしてある。余白の添付が無ければ、両 Native ともピルの端は 100 / 290 (iOS) または 100 / 300 (Android) に来て、期待の 124 ± 許容差から外れる。機構を外すと結果が変わる条件になっている。TS-AT-06 は show 引数の経路と ToastStyle の経路の両方を測っている
- **堅牢性**: 非有限値の辺は既定値 0 に戻る。Android は二重定義をやめて `DialogOptions().dialogMargin` から引くので、iOS (`DialogLayout.swift:75`) と同じ導出になった。MAUI iOS の橋渡しは `contractDefaults` から、Android の橋渡しは直書きの 0 から引く。Android の橋渡しは既存のテスト (`MauiDialogOptions().toDialogOptions() == DialogOptions()`) で契約既定値との一致が固定されている
- **ケース表**: C19 (60, 320)、C23 (60, 510)、C24 (120, 530)、C26 (120, 590) を規則から再導出し、すべて一致した
- **テストの手抜き**: 既定値の変更で弁別力を失ったテスト (currentPage 系の `DEFAULT_MARGIN_DP = 0`) はあるが、余白を控除すること自体は明示の余白を持つケース表 (C18 など) が引き続き固定している。言い訳コメントでスキップしたテストはない
- **公開 doc コメント**: 3 形態の `DialogOptions` の doc は「既定は全辺 0。負の辺と非有限値の辺は、その辺だけ 0 に丸める」と書き換えられている。ADR ID は公開 doc には入れず、非公開の行コメントに置いてある
- **Sample**: 4 ルートとも、パネルの状態から ViewModel を経て `SampleDialogRegistration` の添付へ流れている。Panel / Info の両タブが同じ表示関数を通るので、タブを切り替えても値が保たれる。文言は `SampleText` に置き、選択肢と読み上げ名 (`Margin 0/24/48`) は brief の文言表と一致している。スクリーンショット 2 枚 (Android の余白 24、MAUI iOS の余白 0) を開き、承認モックの A-2 / A-3 と構造が一致することを確かめた

## アクションプラン

1. (Minor) 5 ファイルの `SampleMarginChoice` / `SampleMarginSegmentsView` の doc コメントから「旧既定値」を外し、現在形の説明にする
2. (Minor) `evidence/` にテストの実行件数を記録する (レビュー側の再実行の値を転記してよい)
3. (Suggestion) MAUI の `DialogMargin` の根拠コメントの位置をそろえる / Android の companion の公開範囲を戻す
4. (蒸留時の申し送り) sample-parity.md の「パネルが出すダイアログの余白」の行と文言表・読み上げ節を書き換える (proposal の Impact 記載どおり)。core/ADR-0039 を accepted にし、0008 に amended-by を付ける
