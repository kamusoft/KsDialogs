# セカンドオピニオン: add-loading (code-003)
**相方**: codex / **label**: so-code-add-loading / **日付**: 2026-08-26 / **対象**: 修正サイクル3周目 (iOS 進捗直列鎖・遅延生成失敗の仕様化) の確認
---
## 判定: APPROVED

### 1. iOS の最終進捗競合 — 解消

[LoadingReportQueue.swift](/Volumes/<VOLUME>/Projects/kamusoft/KsDialogs/.claude/worktrees/kasane-loading-changes-181bd9/ios/Sources/KsDialogs/Presentation/LoadingReportQueue.swift) により、進捗処理が直前の処理を待つ直列鎖になっています。

- `runScope` は成功・例外の両経路で `drain()` 後に `endUse` を実行
- KMP bridge は `report` と `endUse` を同じキューへ投入
- MAUI bridge も `runScope` 経由で同じ保証を獲得
- 実環境の MainActor FIFO 挙動に依存せず、コード上の順序保証になっている
- 機構テストと Native/KMP の契約テストが元の競合条件を適切に固定している

新たな懸念はありません。

### 2. Android の遅延 factory 失敗 — 解消

[deviation.md](/Volumes/<VOLUME>/Projects/kamusoft/KsDialogs/.claude/worktrees/kasane-loading-changes-181bd9/kasane/changes/add-loading/deviation.md) に「表示なしで action を完走させる」方針が明文化され、実装も一致しています。

- 通常の `Exception` のみを捕捉して表示を断念
- `Log.w` により障害を観測可能
- `Error` は捕捉せず伝播
- action の所有状態や終了時クリーンアップを維持
- 両方の失敗分類が回帰テストで固定されている

合意済み deviation として妥当であり、新たな懸念はありません。

指摘件数: Critical 0 / Major 0 / Minor 0 / Suggestion 0

ホスト側の実行結果（iOS 212件×5回、Android instrumented 222件）を前提とした静的再レビューです。ファイルへの書き込みは行っていません。

---

## 突き合わせ結果 (2026-08-26, ksn-orchestrator)

相方 APPROVED (指摘 0 件)。ホスト側 review-003 の判定と合わせて最終確定する。
