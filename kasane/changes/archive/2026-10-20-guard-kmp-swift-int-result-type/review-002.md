# レビュー結果: guard-kmp-swift-int-result-type (002 回目)

**日付**: 2026-10-20
**判定**: APPROVED

## サマリー

前回 (review-001.md) の唯一の Minor (優先度高) だった「実装期間中の proposal.md の Impact の書き換えが deviation.md に無い」は、deviation.md:4 に乖離の行として記録され、合意済みの差分になった。前回の Suggestion (完了報告の実行範囲) も evidence/test-run-summary.md:64-73 で、レビュアーの再実行分と未実行のルートを挙げる形で反映されている。コードは前回から変わっておらず (宣言の行番号も review-001 と一致)、新鮮な文脈で読み直しても直すべき点は無い。ios/ の全件と Scenario ID の網羅をこのレビューで再実行し、すべて通った。

## 照合した規約

- `kasane/handbook/cross/comment-policy.md` (always) — 追加した doc コメント・行コメント (公開 doc コメントに ADR ID・change 名が無く、private 側の委ね先のコメントが単独で読める)
- `kasane/handbook/cross/test-execution.md` — 全件実行の表 (L-002 で行ごとに突き合わせ) と「公開 API 形状の検証」の節 (正の検査は非 `@testable` のファイルで行う。下の Suggestion のきっかけ)
- `kasane/handbook/cross/diagnostic-message-language.md` — `@available` の message 12 個と `resultTypeMismatch` の `expected` / `actual` (英語のみ)
- 決定: kmp/ADR-0004 (型付きジェネリックな糖衣・型不一致は型付きエラー・公開エラー型の case 集合を広げない)
- 規律: ksn-core `references/change-scope.md` (足場の凍結と deviation・deviation.md の行の形)
- lessons: `kasane/lessons/code-review.md` (L-001 / L-002)、`kasane/lessons/process.md` (L-002 — deviation.md:4 の根拠として引かれているので本文を確認した)

ロードしたスキル: swift-ui-impl-skill / kotlin-impl-skill (パッケージで指定。kmp/ のコードはこの diff に無いので、kotlin-impl-skill の観点は当てたが該当なし)

## 確認した観点

**前回指摘の解消確認**

| 前回の指摘 | 今回の状態 | 根拠 |
|---|---|---|
| 🟡 Minor (優先度高) proposal.md の Impact の書き換えが未記録 | 解消 | deviation.md:4 に、提案のコミット (f98ed41) の文面 → 書き換え後の文面・根拠 (`kasane/lessons/process.md` L-002)・理由 (実証範囲に絞る、Requirement・Scenario・Non-Goals は不変)・日付がある。`git diff HEAD -- proposal.md` の差分 (proposal.md:34 の 1 行) と内容が 1 対 1 で一致する |
| 🔵 Suggestion 完了報告の実行範囲 | 反映済み | evidence/test-run-summary.md:64-73 にレビュアーの再実行 (ios/・maui/macios/native/) と未実行の 4 ルートとその理由がある |

**仕様充足**
- [x] Requirement「Swift の Int / UInt を結果型に指定したときの案内」: 12 個の deprecated のオーバーロード (`ios/Sources/KsDialogs/Kmp/KsDialogsKmp.swift:78,95,133,150,188,206,242,260,322,338,418,436`)。中身は private の `store` (:272)・`boundNotifier` (:346)・`presentTyped` (:446) へ委ね、自己再帰しない
- [x] Requirement「Int / UInt の結果型での show と報告口の取り出しは型付きの失敗になる」: `presentTyped` は指定した型 → 呼んだ時点の登録の型の順に判定し、表示の前に throw。`boundNotifier` は表示中の判定より先に throw。結果型を省略した show は `show(viewModel, result: Bool.self, ...)` (:387) でジェネリックな入口を通る
- [x] tasks.md: 全 15 タスクのチェックが実装・テスト・証跡と一致 (虚偽チェックなし)
- [x] 足場の書き換え: `proposal.md` の差分は deviation.md:4 に記録済み (合意済みの差分)。`specs/kmp-facade/spec.md` は HEAD から無変更。`tasks.md` はチェック欄だけ
- [x] 無断の仕様逸脱: なし。deviation.md の行は蒸留送り 1 行 (:3) と乖離 1 行 (:4) で、`[付随修正]` の行は無い。diff 中の Scenario に対応しない変更は tasks 2.1 の `[MB-KM-04]` の置き換えだけ

**テスト**
- [x] 全件実行 (下の表)。IR-KM-01〜07 と書き換えた `[MB-KM-04]` 2 本が passed で結果に出ている
- [x] Scenario とテストの対応: IR-KM-01〜07 が `ios/Tests/KsDialogsTests/KsDialogsKmpKotlinIntegerResultTests.swift` のテスト 1 本ずつに対応
- [x] 証跡の判別力 (L-001): 登録の確認は直前に `String` で登録し直してから `declaredResultType` を見る (`KsDialogsKmpKotlinIntegerResultTests.swift:26-31`) ので、登録が何もしなければ落ちる。IR-KM-05 は判定が deprecated のオーバーロードの中にしか無い実装なら throw せず表示へ進むので分かれる
- [x] 警告の出方 (このレビューのビルドで再確認): deprecation 警告はすべて `KsDialogsKmpKotlinIntegerResultTests.swift` の直接呼び出しの行 (41・51・60・69・79・82・93・103・112・121・131・134・152・155・163・166・179・190・193・204・283・286 行) に出ており、ジェネリックなラッパー内 (220・229 行)・IR-KM-07 の呼び出し・非 `@testable` の `ios/Tests/KsDialogsTests/KmpApiSurfaceCompileChecks.swift` には出ていない。後者は、利用者と同じ可視性から見た既存の呼び出し (結果型の省略・`String`) の解決が変わっていないことの正の検査にもなっている
- [x] 手抜き: 言い訳コメントでのスキップなし。`Int` 登録 × `UInt` 指定・`UInt` 登録 × `Int` 指定の組、表示中・未表示の VM の両方、factory 呼び出し回数と提示コンテナ数の両方を見ている

**設計品質**
- [x] kmp/ADR-0004: 公開エラー型に case を足していない
- [x] オーバーエンジニアリング: 判定は private static 関数 1 つ (:503-511)、委ね先 3 つは再帰回避に要る最小構成
- [x] コメント: 公開 doc コメントは機能と契約だけで、外部文書の ID に依存しない
- [x] 判定は `DialogDeclaredResultType` の同一性で行い、名前の文字列に頼らない
- [x] 並行性・入力検証・機密情報: 既存の経路と同じで、新たな問題なし
- [x] swift-ui-impl-skill の観点 (Swift 言語と Concurrency・コード衛生): `@MainActor` の付け方は対応するジェネリックな入口と揃っている。強制アンラップ・`try!` なし

### 全件実行の表との突き合わせ (L-002)

`kasane/handbook/cross/test-execution.md` の全件実行の表 7 行と、表の外の網羅検査:

| ビルドルート | 実行 | 件数 / 結果 | 備考 |
|---|---|---|---|
| ios/ | 実装側 + このレビューで再実行 | Swift Testing 379 tests / 62 suites passed、XCTest 0 tests、`** TEST SUCCEEDED **` | レーンの Simulator、`-collect-test-diagnostics never` |
| android/ | 未実行 | — | この diff は `android/` が読むファイルに触れていない |
| android/ (instrumented) | 未実行 | — | 同上 |
| kmp/ | 実装側 | 169 tests / 0 failures (`iosSimulatorArm64Test` 86・`testAndroidHostTest` 83) | evidence/test-run-summary.md:37-47。kmp/ のコードは diff に無く、前回からコードも変わっていないので再実行していない |
| maui/ | 未実行 | — | .NET 側は `ios/Sources/` を直接コンパイルしない |
| maui/android/native/ | 未実行 | — | `ios/` に依存しない |
| maui/macios/native/ | review-001 で実行 | 17 tests / 7 suites passed、`** TEST SUCCEEDED **` | コードは前回から変わっていないので再実行していない (evidence/test-run-summary.md:71) |
| (網羅検査) `scripts/scenario-id-coverage.py` | 実装側 + このレビューで再実行 | `IR-KM 7/7`・`MB-KM 4/4`・`結果: 未網羅なし` | — |

未実行の 4 行はこの diff のファイルをコンパイルしないルートで、完了判定の妨げにしない。完了報告では「全ルートで成功」ではなく実行したルートを挙げて書く (前回の Suggestion どおり、証跡はすでにその形になっている)。

## 指摘事項

### 🔵 Suggestion 非推奨のオーバーロードの公開可視性は、非 `@testable` の検査では押さえていない
**該当箇所**: `ios/Tests/KsDialogsTests/KsDialogsKmpKotlinIntegerResultTests.swift:6`
**問題点**: IR-KM-01・02 の「利用者のビルドで警告が出る」の証跡は `@testable import KsDialogs` のファイルから採っている。`kasane/handbook/cross/test-execution.md`「公開 API 形状の検証」は、公開面の正の検査を非 `@testable` のファイル (`ios/Tests/KsDialogsTests/KmpApiSurfaceCompileChecks.swift`) で行う決まりで、将来オーバーロードが誤って `internal` になっても、今のテストは警告付きで通り続ける (利用者側ではジェネリックな入口に解決されて警告が消え、実行時の失敗だけが残る)。今のコードは 12 個すべて `public` と明記されているので、現時点の不具合ではない。
**推奨修正**: 任意。対応するなら、非 `@testable` のファイルに `Int` / `UInt` で 1 入口ずつ呼ぶ検査を足す。ただし既定のビルドに deprecation 警告が恒常的に増えるという代償があり、今の change のスコープ (tasks) にも無いので、ここで直すより蒸留時か別の簡易起票で扱うのが妥当。

## アクションプラン

1. (Suggestion・任意) 非推奨のオーバーロードの公開可視性の検査を、非 `@testable` の面で持つかどうかを蒸留時に判断する
