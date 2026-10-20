# 一致検証: guard-kmp-swift-int-result-type (001 回目)

**日付**: 2026-10-20
**判定**: INVALID

❌ は 1 件で、実装期間中の足場 (`proposal.md`) の書き換えが deviation.md に記録されていないこと (逆流)。デルタスペックの Requirement・Scenario・Side Effects はすべて ✅ 一致。

## 対応表

実装は `ios/Sources/KsDialogs/Kmp/KsDialogsKmp.swift`、テストは `ios/Tests/KsDialogsTests/KsDialogsKmpKotlinIntegerResultTests.swift` (以下 `KotlinIntegerResultTests`)。

| Requirement / Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| Requirement: Swift の Int / UInt を結果型に指定したときの案内 | 12 個の `@available(*, deprecated, message:)` オーバーロード (宣言 `KsDialogsKmp.swift:78,95,133,150,188,206,242,260,322,338,418,436`)。中身は private の `store` (`:272`)・`boundNotifier` (`:346`)・`presentTyped` (`:446`) へ委ねる | 下の IR-KM-01・02 | ✅ 一致 |
| [IR-KM-01] Int を指定した 6 入口の呼び出しは案内付きの警告でコンパイルできる | 上の `Int` 用 6 個。message は `Int32` (Kotlin `Int`) / `Int64` (Kotlin `Long`) を案内する英文 | `KotlinIntegerResultTests:35-85` (`[IR-KM-01]`)。THEN の警告は `evidence/deprecation-warnings.md` の表 1〜6 (6 入口すべてに警告、ビルド成功)。AND の登録は `declaredResultType == Int` と結果型を省略した show の登録由来の失敗で、show・notifier は `resultTypeMismatch(expected: "Int32 or Int64", actual: "Int")` で確認 | ✅ 一致 |
| [IR-KM-02] UInt を指定した 6 入口の呼び出しは案内付きの警告でコンパイルできる | 上の `UInt` 用 6 個。message は `UInt32` (Kotlin `UInt`) / `UInt64` (Kotlin `ULong`) を案内する英文 | `KotlinIntegerResultTests:87-137` (`[IR-KM-02]`)。警告は `evidence/deprecation-warnings.md` の表 7〜12。期待値: `resultTypeMismatch(expected: "UInt32 or UInt64", actual: "UInt")` | ✅ 一致 |
| Requirement: 案内 — Side Effects (共有の View factory レジストリ: 更新) | deprecated の `register` 4 種 × 2 は `store` → `registry.register` だけを行い、ジェネリックな `register` と同じ書き込み。deprecated の `show` / `notifier` は throw の前に状態を変えない | IR-KM-01・02 の `declaredResultType` の確認 | ✅ 一致 (列挙の範囲内) |
| Requirement: Int / UInt の結果型での show と報告口の取り出しは型付きの失敗になる | show: `presentTyped` が指定した型 → 呼んだ時点の登録の型の順に `kotlinIntegerMismatch` (`:503-511`) で判定し、表示の前に throw (`:453-459`)。結果型を省略した show も `Bool.self` でジェネリックな入口を通る (`:383-388`)。notifier: `boundNotifier` が表示中の判定より先に throw (`:351-353`)。`register` は判定しない | 下の IR-KM-03〜06 | ✅ 一致 |
| Requirement: show と報告口の失敗 — Requires (show の指定と登録が Int / UInt でない) | 同上の show の判定 | 範囲外の Scenario: IR-KM-03 (指定)・IR-KM-04 (登録) | ✅ 一致 |
| Requirement: show と報告口の失敗 — Requires (notifier の指定が Int / UInt でない) | 同上の notifier の判定 | 範囲外の Scenario: IR-KM-06 | ✅ 一致 |
| Requirement: show と報告口の失敗 — Side Effects (なし) | 判定はレジストリの読み出しだけで、throw の前に書き込み・表示・紐付けをしない | IR-KM-03〜05 で factory 呼び出し 0 回・提示コンテナ 0、IR-KM-06 で表示中のダイアログが残る | ✅ 一致 |
| [IR-KM-03] show に Int / UInt を指定すると表示せずに失敗する | `presentTyped` の指定した型の判定 | `KotlinIntegerResultTests:141-171` (`[IR-KM-03]`)。GIVEN の `Int64` 登録と真偽値登録の両方で `Int.self` / `UInt.self` を指定し、THEN の失敗の値、AND の factory 呼び出し 0 回と提示コンテナ 0 を確認 | ✅ 一致 |
| [IR-KM-04] Int / UInt で登録した共有 VM の show は、指定した結果型によらず表示せずに失敗する | `presentTyped` の登録された型の判定 (`??` で指定した型の判定のあと) | `KotlinIntegerResultTests:173-209` (`[IR-KM-04]`)。結果型の省略と `Int64` の指定で登録型の失敗、2 つ目の AND の `Int` 登録 × `UInt` 指定 → `uintMismatch`・`UInt` 登録 × `Int` 指定 → `intMismatch`、factory 呼び出し 0 回と提示コンテナ 0 | ✅ 一致 |
| [IR-KM-05] ジェネリックなラッパー越しの Int / UInt の指定も同じに失敗する | ジェネリックな `show` / `notifier` も同じ委ね先を通る (`:400-406`、`:306-312`) | `KotlinIntegerResultTests:232-258` (`[IR-KM-05]`)。型引数で受けるラッパー 2 つ (`:215-230`) で show と notifier が直接指定と同じ失敗、show は factory 呼び出し 0 回・提示コンテナ 0。ラッパー内が deprecated の入口に解決されていないことは `evidence/deprecation-warnings.md`「補足の観察」(警告なし) | ✅ 一致 |
| [IR-KM-06] notifier(for:result:) に Int / UInt を指定すると、表示中でも登録の型によらず失敗する | `boundNotifier` の先頭の判定 (`:351-353`) | `KotlinIntegerResultTests:262-294` (`[IR-KM-06]`)。`String` で登録して表示中の VM と未表示の VM の両方で `Int.self` / `UInt.self` が失敗 (nil にならない)。AND の、ダイアログが閉じずに `String` の報告口で `completed("入力")` を返すことも確認 | ✅ 一致 |
| [IR-KM-07] Kotlin の整数型に対応する Swift の型は今までどおり使える | 判定は `Int` / `UInt` の型の同一性だけで、`Int32` / `Int64` / `UInt32` / `UInt64` は判定に当たらない | `KotlinIntegerResultTests:310-373` (`[IR-KM-07]`)。4 型それぞれで登録・show し、`completed(-7)` / `completed(Int64.max)` / `completed(7)` / `completed(UInt64.max)`。THEN の「警告も無く」は `evidence/deprecation-warnings.md`「補足の観察」(IR-KM-07 の呼び出しに警告なし) | ✅ 一致 |

## 追加検査

- [x] **tasks.md**: 全 15 タスクがチェック済みで、対応表・diff・証跡と一致する。虚偽チェックなし
  - 2.1 (`[MB-KM-04]` の `Int.self` → `Int64.self`、期待値 `"Int"` → `"Int64"`): `ios/Tests/KsDialogsTests/KsDialogsKmpModelBindingTests.swift:100-112` で実施
  - 3.1・3.2・3.3: `evidence/deprecation-warnings.md` (12 入口の表)・`evidence/test-run-summary.md` (件数・IR-KM の結果行・診断文言の grep) で実施。Simulator の識別子は `<udid>` で書かれている
- [ ] **逆流検査**: ❌ — `proposal.md` (足場) が実装期間中に書き換えられている (下の ❌ 1)。`specs/kmp-facade/spec.md` は HEAD (`f98ed41`、提案のコミット) から変わっていない。`tasks.md` の差分はチェック欄 `[ ]` → `[x]` だけ
- [x] **未記録乖離**: 対応表に ❌ は無い。❌ 1 は対応表の外 (足場の書き換え)
- [x] **Side Effects (逆向きの検査)**: 2 つの Requirement とも、実装の状態変更は列挙に収まっている (対応表の Side Effects の行)
- [x] **付随修正**: deviation.md に `[付随修正]` の行は無い。diff にあって Scenario に対応しない変更は `[MB-KM-04]` の置き換えだけで、tasks 2.1 に書かれた作業
- [x] **UI 変更**: なし (ui/brief.md 不要)
- [x] **テストの全件成功** (検証の中で再実行した):
  - ios/: `xcodebuild test -scheme KsDialogs` (レーンの Simulator、`-collect-test-diagnostics never`) → Swift Testing 379 tests / 62 suites passed、XCTest 0 tests、`** TEST SUCCEEDED **`。IR-KM-01〜07 と `[MB-KM-04]` 2 本が passed で結果に出ている
  - maui/macios/native/ (`ios/` を local package として取り込む): 17 tests / 7 suites passed、`** TEST SUCCEEDED **`
  - `python3 scripts/scenario-id-coverage.py` → `IR-KM 7/7`、未網羅なし
  - kmp/: 再実行はしていない。実装側の `./gradlew allTests --rerun-tasks` の 169 tests / 0 failures (`evidence/test-run-summary.md`) を採った。kmp/ のコードは diff に無い

## ❌ の一覧と見立て

### ❌ 1 実装期間中の proposal.md の書き換え (未記録)

- **内容**: `proposal.md:34` の Impact の 1 項目目。「それ以外の結果型の利用者には影響しない」が、「影響は無い見込みで、実証は ios/ の既存テスト … のビルドと全件実行、IR-KM-07 に限る。実証しない面: 利用者側で 6 つの入口を関数参照として取り出す書き方 …、ABI …」に変わっている (`git diff HEAD -- kasane/changes/guard-kmp-swift-int-result-type/proposal.md`)。deviation.md に対応する行が無い
- **影響**: Requirement・Scenario・Non-Goals・What Changes は変わっておらず、挙動の契約には影響しない (実証していない主張を狭めた書き換え)
- **見立て**: 実装を直す必要はない。次のどちらかを記録の主体 (ksn-orchestrator / フェーズ担当) が選ぶ — (a) `proposal.md` を HEAD の文面に戻し、足した内容は証跡か deviation.md へ移す、(b) deviation.md に乖離の行で記録し、合意済みの差分にする。内容が正確さを増す方向の書き換えなので、(b) で合意するのが自然。決定はしない
