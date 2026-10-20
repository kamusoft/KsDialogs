# 自律実行の報告: ios-safe-area-and-kmp-int-result (2026-10-20)

起点: `develop` の `f396a3e` / 統合ブランチ: `autopilot/ios-safe-area-and-kmp-int-result` / 止めた理由: 全フェーズ完了
フェーズ: 完了 2 / 保留 0 / 追加 0 (修正フェーズ 0) / ksn-conductor の起動 5 回 (自律判断ログの `[起動]` の行数。事前点検の確かめ役 2 体は数えない) / 予算の上限 6 回

phase-2 は自律実行の途中で一度保留になった。オーナーが同じ実行の中で保留を判断し (委任状の「保留の判断」)、完了条件 6・7 と方針を改めて委任状の承認を取り直してから、同じブランチの続きで再開した。

## 完了条件

統合ブランチの先頭 (`f2b0a04`) で確かめた。

| # | 条件 | 判定 | 根拠 |
|---|---|---|---|
| 1 | 2 フェーズがどちらも蒸留済み | 満たす | `roadmap.md` の 2 行とも `completed`、Change 列が `changes/archive/2026-10-20-deliver-swiftui-content-safe-area-ios/` と `changes/archive/2026-10-20-guard-kmp-swift-int-result-type/` |
| 2 | 統合ブランチで ios/ の全件テストが通る | 満たす | `xcodebuild test -scheme KsDialogs` が成功し、`Test run with 402 tests in 67 suites passed` (起点 372 件) |
| 3 | 統合ブランチで kmp/ の全件テストが通る | 満たす | `./gradlew allTests --rerun-tasks` が `BUILD SUCCESSFUL`。`test-results/` の XML の合計 169 件 (iosSimulatorArm64Test 86 / testAndroidHostTest 83)、失敗 0 |
| 4 | 仕様の Scenario ID がテスト名に網羅されている | 満たす | `python3 scripts/scenario-id-coverage.py` が「結果: 未網羅なし」で成功 |
| 5 | 窓全体基準で端に付けた SwiftUI の中身から見える安全領域が重なりに等しく、既定の可視領域では 0 | 満たす | PB-IS-01〜19 のテスト 19 件が 2 の結果に現れて通っている。Simulator の実際の窓での実測は `changes/archive/2026-10-20-deliver-swiftui-content-safe-area-ios/evidence/real-window-safe-area.md` |
| 6 | (2026-10-20 改訂) `Int` / `UInt` を指定すると `Int32` / `Int64` (`UInt32` / `UInt64`) を案内する英語の deprecated 警告が出て、show すると `KsDialogsKmpError` の型付きの失敗になる | 満たす | 12 入口の警告の記録が `changes/archive/2026-10-20-guard-kmp-swift-int-result-type/evidence/deprecation-warnings.md`。実行時の失敗を確かめる IR-KM-01〜07 のテスト 7 件が 2 の結果に現れて通っている |
| 7 | concepts が新しい挙動と型の対応を記述している | 満たす | `concepts/ios/api/layout-surface.md` の「SwiftUI の中身から見える安全領域」。`concepts/kmp/api/ios-host-integration.md` の「Kotlin の整数型を結果型にするとき」(型の対応表と `UInt32` / `UInt64`) と、共有コードからの show では食い違いが検出されない記述 (122 行) |
| 8 | ios/ADR-0002 が実装と一致している | 満たす | `status: accepted`。本文は「標準の安全領域として届ける」方向で、範囲を窓全体基準の端の軸に絞った形。`現行照合:` が実装のファイルとテストを指す (自動承認 — 下の「確かめてほしい自動の決定」) |

## フェーズごとの結果

| フェーズ | change (級) | レビュー / verify | 蒸留 | 備考 |
|---|---|---|---|---|
| phase-1-ios-swiftui-content-safe-area | deliver-swiftui-content-safe-area-ios (M) | review-003 APPROVED (修正サイクル 2 回) / verify-003 VALID。相方の提案レビュー 1 回・コードレビュー 3 回 | 済 (`6950a85`。ios/ADR-0002 を accepted) | 素直な A 案で器の大きさが崩れて停止報告 → 見た目の基準を満たす形を 1 回だけ探させて解消。マージ `841671b` |
| phase-2-kmp-swift-int-result-guard | guard-kmp-swift-int-result-type (M) | review-002 APPROVED / verify-002 VALID。相方の提案レビュー 1 回・コードレビュー 1 回 | 済 (`7e94d6c`。ADR なし) | 案内付きのコンパイルエラーにする仕組みが Swift に無いと分かって保留 → オーナーの判断 (案 A + `UInt`) で再開。マージ `1e95c38` |

## オーナーに判断してほしいこと

なし (保留・未達は無い)。

## 確かめてほしい自動の決定 (覆すコストの高い順)

| 決定 | フェーズ | 根拠 | 覆すコスト | 記録 |
|---|---|---|---|---|
| ios/ADR-0002 の範囲を「窓全体基準で位置が端に固定される軸だけ・キーボードは含めない・要求サイズに重なりを入れない」に書き直して自動承認した | phase-1 | 委任状の方針・完了条件 8・見た目の基準 | 高 (ADR) | `kasane/decisions/ios/0002-swiftui-content-receives-standard-safe-area.md` |
| 要求サイズは安全領域を受け取らないときと同じ値にし、安全領域を渡すのは窓全体基準で端に固定される軸だけにした (どの配置でも器の矩形は今と同じ) | phase-1 | 判断の優先順 1 (見た目の基準と停止報告への答え) | 中 (1 フェーズの設計) | phase-1 の agenda.md・`evidence/safe-area-sizing-probe.md` |
| `Int` / `UInt` の失敗は新しい case を足さず `resultTypeMismatch` で表し、`expected` に使うべき型 (`"Int32 or Int64"` / `"UInt32 or UInt64"`)、`actual` に指定された型を入れる | phase-2 | 判断の優先順 1 (保留の判断)・2 (kmp/ADR-0004 の case 集合を広げない) | 中 (公開エラーの値の中身) | phase-2 の agenda.md・spec |
| (オーナーの判断の記録) phase-2 の保留を案 A + `UInt` で判断し、完了条件 6・7・方針とロードマップのゴールを改めて承認を取り直した | phase-2 | オーナーの判断 | 中 | `charter.md`「保留の判断」・`history.md` |

- 覆すコストが低の決定: 19 件 (自律判断ログの `[決定]`・`[改訂]`)
- 自動承認した ADR: ios/ADR-0002 — iOS の器に載せた SwiftUI の中身には、窓全体基準で端に付けた軸の安全領域との重なりを、器の大きさを変えずに標準の安全領域として届ける (footer に「自動承認: ksn-autopilot、オーナー未確認」)
- 自動照合した画面: なし (承認済みモックは使わず、見た目の基準は ADR-0002 の挙動。Simulator での実測は数値の記録)
- research の ADR 候補 (起票していない): なし

## 次にやること

- 統合ブランチをレビューし、`main` へマージする。PR の `## Changes` に書く `- breaking:` の英文の案は `changes/archive/2026-10-20-guard-kmp-swift-int-result-type/proposal.md` の Impact にある
- `skills/` と README の追従: docs-refresh で行う (ロードマップの非ゴール)
- 昇格待ちの教訓: `tool-premise-decision-needs-existence-probe` (spec-review, pain, count 3。案: `lessons/spec-review.md` の L-004。ksn-autopilot の委任の準備にも掛かるので ksn-evolve の候補) → ksn-lesson の昇格モード
- 関連リポジトリへの起票の候補: なし。ColorAnalyzer 側の回避策 (`WindowSafeArea.swift`・`Int32` の書き方) の撤去は別リポジトリの非ゴール
- 簡易起票した change: `align-android-content-insets-meaning`・`fix-swiftui-window-area-first-layout-width`・`strip-adr-ids-from-ios-public-docs` → ksn-sweep / ksn-explore
- 文書の手入れ: `concepts/kmp/api/ios-host-integration.md` の散文が 9994 字で分割の目安の直前 (次に育つとき「Swift 側で登録するもの」を分ける)
- ロードマップのアーカイブ: 全フェーズ終端なので ksn-roadmap のアーカイブができる
