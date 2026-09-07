# セカンドオピニオン: add-loading (code-002)
**相方**: codex / **label**: so-code-add-loading / **日付**: 2026-08-26 / **対象**: 修正サイクル2周目 (相方指摘 Major 3件への修正) の確認
---
# 判定: CHANGES_REQUESTED

| 前回指摘 | 判定 |
|---|---|
| 1. Android factory 失敗後の状態残留 | 新たな懸念 |
| 2. Android token 配送時のキャンセル競合 | 解消 |
| 3. MAUI 最終進捗の喪失 | 未解消（Android は解消、iOS は残存） |

## [Major / 新たな懸念] 遅延 factory 失敗が正常終了へ変換される

**該当箇所:** `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/LoadingCoordinator.kt:367`

**確認結果:** 通常開始時の factory 例外については、`rollbackFailedStart()` が利用数、器、コンテンツ、host 購読を除去しており、前回指摘した状態汚染は解消しています。テストも後続表示の成立まで確認できています。

一方、提示先復帰時の factory 例外は `abandonContent()` で握り潰されます。action はそのまま完了し、`start` も成功として戻るため、`dialog-contract/spec.md:19` の「View factory の失敗は action を実行せず失敗として伝播」に反します。現在の `deviation.md` にもこの差分はありません。

また `catch (contentFailure: Throwable)` は `OutOfMemoryError` 等の致命的な JVM `Error` まで抑止します。

**推奨修正:** 初期提示先がないカスタム表示では後から factory を実行しない、または遅延失敗を呼び出し元へ配送できる所有構造にしてください。遅延表示を維持して現在の成功扱いを仕様とするなら、実装修正ではなくオーナー判断と deviation が必要です。抑止対象は少なくとも `Exception` に限定し、致命的な `Error` は再送出してください。

## [解消] Android token 配送時のキャンセル競合

**該当箇所:** `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/LoadingCoordinator.kt:152`

`acquireUse()` で確定した token を `AtomicReference` に保持し、`withContext` の復帰時に `CancellationException` が発生した場合は `NonCancellable` で `endUse()` を完了してから再送出しています。

状態 commit から token 保存までに新しい suspension pointもなく、旧世代化や合流が並行しても `generation` とカウントで正しく処理されます。新規テストも問題の競合位置で決定的にキャンセルし、action 未実行・利用数ゼロ・器撤去を確認しています。

この指摘は解消です。

## [Major / 未解消] iOS では最終進捗が依然として終了処理に追い越され得る

**該当箇所:** `ios/Sources/KsDialogs/Presentation/Loading.swift:166`、`maui/macios/native/KsDialogsMauiBridge/MauiLoadingBridge.swift:149`

`DirectProgress` により C# の `Progress<double>` が持っていた非同期 dispatch は除去され、C# delegate までの順序は保証されました。Android は `Dispatchers.Main.immediate` 経路により、その場で Native coordinator まで受理されるため解消と判断できます。

しかし iOS の報告口は、受け取った値を依然として別の `Task { @MainActor in ... }` へ投入します。その直後に action の completion が continuation を再開し、`Loading.runScope()` が `endUse()` を呼びます。Swift actor の別 Task 間には FIFO 保証がないため、次の順序が残ります。

```text
DirectProgress.Report
→ Swift の report block 呼び出し
→ MainActor Task を予約
→ action completion
→ endUse で activeCount = 0
→ 予約した report が実行され、旧報告として破棄
```

追加された `LoadingActionRunnerTests` は C# delegate までの同期性しか検証しておらず、Android bridge のテストにも「最終報告が Native の終了前に受理された」ことを見る統合ケースはありません。iOS Native の直接利用にも同じ競合が残ります。

**推奨修正:** iOS の報告 Task を追跡し、action 完了時に発行済み報告をすべて drain してから `endUse()` してください。iOS Native と MAUI iOS の双方に、`report(1.0)` の直後に action が戻るテストを追加し、カスタム VM が `1.0` を受け取った後に `start` が完了することを確認してください。

ホスト提示の Android instrumented 221件、MAUI 110件、bridge 25件の通過結果は受領し、再実行していません。今回の修正範囲外である前回 Minor は再判定対象から除外しました。

---

## 突き合わせ結果 (2026-08-26, ksn-orchestrator)

ホスト側 review-002.md (NEEDS_DISCUSSION) との突き合わせ。

| 指摘 | 採否 | 処理 |
|---|---|---|
| 遅延 factory 失敗が正常終了へ変換される (新たな懸念) | **採用** → オーナー判断へ | spec 未定義コーナーとしてオーナーに提示し「表示なしで完走」で決定 (deviation 記録)。Exception 限定 + 警告ログ + Error 伝播テストを実装 |
| Throwable 捕捉が広すぎる | **採用** (Minor) | 遅延経路を Exception に限定。通常経路は rollback 後再送出のため Throwable 維持 (理由をコメント化) |
| iOS の最終進捗が終了に追い越されうる (未解消) | **採用** (Major) — 自前のコード確認で裏取り | LoadingReportQueue (直列鎖 + drain) で機構化。実測では旧実装でも再現せず、言語仕様上の保証の明示化という位置づけ |
| Android キャンセル競合 | 解消判定で一致 | — |
| MAUI 最終進捗 (C# 区間) | 解消判定で一致 (iOS Native 区間の残りは上記で対処) | — |
