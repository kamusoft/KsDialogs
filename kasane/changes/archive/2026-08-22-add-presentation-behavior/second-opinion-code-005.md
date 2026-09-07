# セカンドオピニオン: add-presentation-behavior (code-005)
**相方**: codex (セッション `so-code-add-presentation-behavior-r4` への追送、器 ksn-reviewer) / **日付**: 2026-08-22 / **対象**: `.presenting` 遷移の窓の修正 (review-004 Minor / second-opinion-code-004 Major 確定)。入力はホスト側 review-005 と同一
---
# レビュー結果: add-presentation-behavior（5回目）

**日付**: 2026-08-22  
**判定**: **APPROVED**

## サマリー

前回の Major は解消されています。`.attached` から `.presenting` への遷移、結果確定の再確認、presentation 開始が同一の MainActor 同期区間にまとまり、提示開始前と提示開始後の閉鎖境界が design Decision 10 と一致しました。

追加2テストも問題の窓を一度も `await` せず再現しており、修正前の誤ったフック実行を有意に検出します。コード上の退行や新たな Critical / Major はありません。

## 指摘事項

### [🟡 Minor] deviation.md の実装メモが修正前の状態遷移を説明している

**該当箇所**: `kasane/changes/add-presentation-behavior/deviation.md:13`

**問題点**: 現在の記述は「`beginPresentation` 側は状態遷移とタスク起動だけを行う」としていますが、修正後の `beginPresentation()` は Task の作成だけを行い、`.presenting` への遷移は Task 本体のガード通過後に行います。また、ガードも「取り消し済み」だけでなく、状態と結果確定済みの確認を含みます。

このまま蒸留すると、今回解消した状態遷移の境界が誤って記録される可能性があります。

**推奨修正**: 次の現在形へ更新してください。

> `beginPresentation` は進行 Task の起動だけを行う。Task 本体は、取り消し・`.attached` 状態・結果未確定を最初の中断点より前に確認し、通過後に `.presenting` へ進めて中身の表示と presentation フックを開始する。

コード変更は不要です。

## 確認結果

- `DialogContainerViewController.swift:421-429` は、ガードから状態遷移、`runPresentationPhase()` 呼び出しまで中断点がありません。ガード通過後の閉鎖は presenting、通過前の閉鎖は attached として線形化できます。
- ガード失敗時の `finishRemoval()` は、attached 側ですでに撤去要求済みの場合も既存の多重実行防止で安全です。
- PB-TR-05 / 23 / 28 の提示開始後の経路は維持されています。
- 追加2テストは、呼び出し元キャンセルと通常報告の両方について、presentation / dismissal フック非実行と配送結果まで検査しています。
- `waitUntilPresentationCompletes` への変更は、既定トランジションの alpha や途中の transform に依存せず、最終位置でのヒットテストを行うため、既存2テストを弱めていません。
- `runPresentationPhase()` の成立条件コメントは実装と一致し、単独でも意図を理解できます。
- 覆いの `async let` と FIFO 依存テストは、制約・実測・後続候補への記録を踏まえ、今回の承認を妨げる問題とは評価しません。
- attached 中の外側タップは design との差分ですが、`deviation.md` に記録済みです。今回 `.attached` の窓が明確な1ターンになったことで直接呼び出しでは観察しやすくなりましたが、通常の実入力では到達しにくく、結果もフックなしの cancelled 撤去へ収束します。合意済み差分として妥当です。

## アクションプラン

1. 蒸留前に `deviation.md:13` の実装メモだけ現在の状態遷移へ更新する。
2. コードおよびテストの追加修正は不要。

ホスト実施の iOS 136 tests / 25 suites、3連続実行、lint、Scenario ID 検査、MAUI bridge ビルド結果を確認済み証跡として扱いました。依頼どおり再実行・ファイル書き込みは行っていません。


## 突き合わせ結果 (ホスト review-005.md との照合、2026-08-22)

| # | 指摘 | ホスト (review-005) | 相方 (codex) | 採否 | 根拠 |
|---|---|---|---|---|---|
| 1 | 4 回目 #1 (`.presenting` 遷移の窓) / #4 (成立条件コメント) / #6 (待ち条件) | 解消 | 解消 | **確定 (解消)** | 双方一致。遷移表との一致・報告到達順の冪等性・新規 2 本の検出力・退行なしを双方が確認 |
| 2 | ガード第 3 条件 `!resultChannel.isResultSettled` を単独で殺す変異をどのテストも検出しない (`observeResultChannel()` 未登録で settle する 1 本で検出可) | 🟡 Minor | — | 採用 (ホストのみ) | テスト 1 本の追加 (小スコープ)、確認はレビュアーに追記依頼 |
| 3 | 外側タップの deviation が `.created` を含まず、厳格化のコスト (既存テスト 4 本以上) の記録が無い | 🟡 Minor | — (合意済み差分として妥当と評価) | 採用 (文書) | オーケストレーターが deviation.md を更新 |
| 4 | deviation.md の実装メモが修正前の遷移を説明 | — | 🟡 Minor | 採用 (文書) | 更新済み (review-005 は更新後の内容で判断) |
| 5 | 「提示前の確定」を `scheduleImmediateRemoval()` と新設ガードの 2 機構が担う (片方だけ直す保守ハザード) | 🔵 Suggestion | — | 申し送り | handoff-distill.md へ (一本化は後続) |

- 判定: ホスト APPROVED / 相方 APPROVED。降格 0 / 未解決 0
- 再オープン分 (オーナー差し戻しの入場ちらつき) のレビューサイクル: 2 周 (review-004 → 005) で収束
