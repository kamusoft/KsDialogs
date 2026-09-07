---
scope: code-review
kind: pain
severity: normal
count: 2
first-seen: 2026-08-21
last-seen: 2026-08-26
evidence:
  - add-presentation-behavior (review-001 が iOS `DialogContainerViewController.finishRemoval()` の「`onDismissRequest` 直後に配送」を見逃し、相方 second-opinion-code-001 が Major で検出。実 surface の `dismiss(animated: false)` は completion を受けず撤去完了を確認していないが、テスト用 surface が `dismiss` 内で同期的に除去するため PB-TR-10 / PB-TR-13 が通っていた。修正サイクル 1 周が発生)
  - add-loading (review-001 が MAUI の進捗パススルーを見逃し、相方 second-opinion-code-001 が Major で検出。`Progress<double>` の `Report()` は捕捉した SynchronizationContext / ThreadPool へ**非同期 dispatch** する一方、action 完了通知は同期呼び出しのため、`progress.Report(1.0); return;` という通常の形で最終進捗が完了通知に追い越され、Native 側が終了後の報告として捨てる。修正は同期転送する専用 IProgress 実装 (`LoadingActionRunner`) + 順序を固定する C# テスト 5 本。iOS 側の同型の余地も相方 2 周目の指摘で `LoadingReportQueue` (受理の鎖 + drain) として機構化された)
---

## ルール文

「X の完了後に Y する」を契約にした経路 (撤去後の配送・アニメーション完了後の解放など) をレビューするときは、本番の X がコールバック / completion / async のどれで完了を知らせるかを実装 (OS API の呼び出し箇所) で確認し、テストダブルの X が同期的に完了している場合は「本番では非同期になる境界を Scenario テストが区別できていない」として指摘する。判定には、ダブルに「要求」と「完了」を別々に進められる操作があるかを見る。

## 経緯

- 2026-08-21 add-presentation-behavior: ホスト側レビューは状態機械の多重進入とテストの手抜き (即完了ダブル) を重点的に見て「素通しなし」と判定したが、確認したのはフックのダブルであって提示面 (surface) のダブルではなかった。契約の「撤去」を担う OS API 側の完了境界まで視線が届いていなかった
- 2026-08-26 add-loading: 隠れ方の変種 — テストダブルではなく、標準型 (`Progress<T>`) の「呼び出しは同期・配送は非同期」という仕様が境界を隠した。「X の後に Y」(報告の後に終了) の契約に対し、X を運ぶ機構の配送タイミングを言語/フレームワーク仕様まで降りて確認する必要があるのは同じ
