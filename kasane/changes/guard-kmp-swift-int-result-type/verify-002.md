# 一致検証: guard-kmp-swift-int-result-type (002 回目)

**日付**: 2026-10-20
**判定**: VALID

前回 (verify-001.md) の ❌ 1 (実装期間中の `proposal.md` の書き換えが未記録) は、deviation.md:4 に乖離の行として記録されたので ⚠️ deviation 記録済みに変わった。デルタスペックの Requirement・Scenario・Side Effects はすべて ✅ 一致のまま。コードは前回から変わっていない (宣言の行番号が verify-001 と一致)。

## 対応表

実装は `ios/Sources/KsDialogs/Kmp/KsDialogsKmp.swift`、テストは `ios/Tests/KsDialogsTests/KsDialogsKmpKotlinIntegerResultTests.swift` (以下 `KotlinIntegerResultTests`)。

| Requirement / Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| Requirement: Swift の Int / UInt を結果型に指定したときの案内 | 12 個の `@available(*, deprecated, message:)` オーバーロード (宣言 `KsDialogsKmp.swift:78,95,133,150,188,206,242,260,322,338,418,436`)。中身は private の `store` (`:272`)・`boundNotifier` (`:346`)・`presentTyped` (`:446`) へ委ねる | 下の IR-KM-01・02 | ✅ 一致 |
| [IR-KM-01] Int を指定した 6 入口の呼び出しは案内付きの警告でコンパイルできる | 上の `Int` 用 6 個。message は `Int32` (Kotlin `Int`) / `Int64` (Kotlin `Long`) を案内する英文 | `KotlinIntegerResultTests:35-85`。THEN の警告は `evidence/deprecation-warnings.md` の表 1〜6 と、この検証のビルドのログ (41・51・60・69・79・82 行に警告、ビルド成功)。AND は `declaredResultType == Int` と登録由来の show の失敗、show・notifier の失敗。期待値: `resultTypeMismatch(expected: "Int32 or Int64", actual: "Int")` | ✅ 一致 |
| [IR-KM-02] UInt を指定した 6 入口の呼び出しは案内付きの警告でコンパイルできる | 上の `UInt` 用 6 個。message は `UInt32` (Kotlin `UInt`) / `UInt64` (Kotlin `ULong`) を案内する英文 | `KotlinIntegerResultTests:87-137`。警告は `evidence/deprecation-warnings.md` の表 7〜12 と、この検証のビルドのログ (93・103・112・121・131・134 行)。期待値: `resultTypeMismatch(expected: "UInt32 or UInt64", actual: "UInt")` | ✅ 一致 |
| Requirement: 案内 — Side Effects (共有の View factory レジストリ: 更新) | deprecated の `register` 8 個は `store` → `registry.register` だけ。deprecated の `show` / `notifier` は throw の前に状態を変えない | IR-KM-01・02 の `declaredResultType` の確認 | ✅ 一致 (列挙の範囲内) |
| Requirement: Int / UInt の結果型での show と報告口の取り出しは型付きの失敗になる | show: `presentTyped` が指定した型 → 呼んだ時点の登録の型の順に `kotlinIntegerMismatch` (`:503-511`) で判定し、表示の前に throw。結果型を省略した show は `:387` でジェネリックな入口を通る。notifier: `boundNotifier` が表示中の判定より先に throw。`register` は判定しない | 下の IR-KM-03〜06 | ✅ 一致 |
| Requirement: show と報告口の失敗 — Requires (show の指定と登録が Int / UInt でない) | 同上の show の判定 | 範囲外の Scenario: IR-KM-03 (指定)・IR-KM-04 (登録) | ✅ 一致 |
| Requirement: show と報告口の失敗 — Requires (notifier の指定が Int / UInt でない) | 同上の notifier の判定 | 範囲外の Scenario: IR-KM-06 | ✅ 一致 |
| Requirement: show と報告口の失敗 — Side Effects (なし) | 判定はレジストリの読み出しだけで、throw の前に書き込み・表示・紐付けをしない | IR-KM-03〜05 で factory 呼び出し 0 回・提示コンテナ 0、IR-KM-06 で表示中のダイアログが残る | ✅ 一致 |
| [IR-KM-03] show に Int / UInt を指定すると表示せずに失敗する | `presentTyped` の指定した型の判定 | `KotlinIntegerResultTests:141-171`。`Int64` 登録と真偽値登録の両方で `Int.self` / `UInt.self` を指定し、失敗の値、factory 呼び出し 0 回、提示コンテナ 0 | ✅ 一致 |
| [IR-KM-04] Int / UInt で登録した共有 VM の show は、指定した結果型によらず表示せずに失敗する | `presentTyped` の登録された型の判定 (`??` で指定した型の判定のあと) | `KotlinIntegerResultTests:173-209`。結果型の省略と `Int64` の指定で登録型の失敗。`Int` 登録 × `UInt` 指定 → `expected: "UInt32 or UInt64", actual: "UInt"`、`UInt` 登録 × `Int` 指定 → `expected: "Int32 or Int64", actual: "Int"`。factory 0 回・提示コンテナ 0 | ✅ 一致 |
| [IR-KM-05] ジェネリックなラッパー越しの Int / UInt の指定も同じに失敗する | ジェネリックな `show` / `notifier` も同じ委ね先を通る | `KotlinIntegerResultTests:232-258` (ラッパーは `:215-230`)。show と notifier が直接指定と同じ失敗、show は factory 0 回・提示コンテナ 0。ラッパー内 (220・229 行) に警告が出ていないこと (= deprecated の入口に解決されていないこと) をこの検証のビルドでも確認 | ✅ 一致 |
| [IR-KM-06] notifier(for:result:) に Int / UInt を指定すると、表示中でも登録の型によらず失敗する | `boundNotifier` の先頭の判定 | `KotlinIntegerResultTests:262-294`。`String` 登録の表示中 VM と未表示 VM の両方で `Int.self` / `UInt.self` が失敗 (nil にならない)。ダイアログが閉じず、`String` の報告口で `completed("入力")` を返す | ✅ 一致 |
| [IR-KM-07] Kotlin の整数型に対応する Swift の型は今までどおり使える | 判定は `Int` / `UInt` の型の同一性だけ | `KotlinIntegerResultTests:310-373`。期待値: `completed(-7)` / `completed(Int64.max)` / `completed(7)` / `completed(UInt64.max)`。「警告も無く」は 310-373 行に警告が出ていないことをこの検証のビルドで確認 | ✅ 一致 |
| proposal.md Impact 1 項目め (足場の書き換え) | — (挙動の契約に関わらない文面の変更) | — | ⚠️ deviation 記録済み (deviation.md:4) |

## 追加検査

- [x] **tasks.md**: 全 15 タスクがチェック済みで、対応表・diff・証跡と一致する。虚偽チェックなし (2.1 は `ios/Tests/KsDialogsTests/KsDialogsKmpModelBindingTests.swift:100-112` で実施、3.1〜3.3 は evidence/ の 2 ファイルで実施)
- [x] **逆流検査**: `git diff HEAD` で足場を確認した。`specs/kmp-facade/spec.md` は HEAD (`f98ed41`) から無変更。`tasks.md` はチェック欄 `[ ]` → `[x]` だけ。`proposal.md` は proposal.md:34 の 1 行が書き換わっており、deviation.md:4 の記録 (元の文面・書き換え後の文面・根拠・理由・日付) と 1 対 1 で一致するので、合意済みの差分として扱う
- [x] **未記録乖離**: なし
- [x] **Side Effects (逆向きの検査)**: 2 つの Requirement とも、実装の状態変更は列挙に収まっている
- [x] **付随修正**: deviation.md に `[付随修正]` の行は無い。diff にあって Scenario に対応しない変更は tasks 2.1 の `[MB-KM-04]` の置き換えだけで、tasks に書かれた作業
- [x] **UI 変更**: なし (ui/brief.md 不要)
- [x] **テストの全件成功** (この検証の中で再実行した):
  - ios/: `xcodebuild test -scheme KsDialogs` (レーンの Simulator、`-collect-test-diagnostics never`) → Swift Testing 379 tests / 62 suites passed、XCTest 0 tests、`** TEST SUCCEEDED **`。IR-KM-01〜07 と `[MB-KM-04]` 2 本が passed
  - `python3 scripts/scenario-id-coverage.py` → `IR-KM 7/7`・`MB-KM 4/4`、`結果: 未網羅なし`
  - kmp/: 再実行していない。kmp/ のコードは diff に無く、実装側の `./gradlew allTests --rerun-tasks` の 169 tests / 0 failures (evidence/test-run-summary.md:37-47) を採った
  - maui/macios/native/: 再実行していない。コードは前回から変わっておらず、review-001 の再実行 (17 tests / 7 suites passed、evidence/test-run-summary.md:71) を採った

## ❌ の一覧

なし。
