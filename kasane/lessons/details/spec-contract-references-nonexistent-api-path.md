---
scope: spec-review
kind: pain
severity: normal
count: 3
first-seen: 2026-08-26
last-seen: 2026-09-05
evidence:
  - split-concepts-platform-surface (design Decision 2 が core に残す共通概念名の候補として `DialogOptions` / `LoadingStyle` / `ToastStyle` / `notifier` を「4 形態同綴り」の前提で列挙したが、KMP commonMain はこれらを公開しておらず現行 API と矛盾。相方スペックレビュー second-opinion-spec-001 Major 2 で検出し、一覧を「Skill 範囲すべての公開面に存在することをコードで確認した名前 (実装時に確定)」へ改訂。実装フェーズの確定一覧では `DialogException` / `LayoutArea` も条件を満たさなかった)
  - rollout-user-docs (user-skills spec の Scenario「Toast 節の内容」が、移行 Skill の対応表に旧 `Toast.Instance.Show(message)` → 新 Toast の message 入口の行を要求したが、指定された移植元 clone の README・公開ソース・全 Git refs に旧 message overload は存在しなかった。オーナー裁定で「対応先なし + 新 message 入口は移行後の代替」に倒して deviation 記録。agenda の決定を spec に写す時点で移植元の公開 API 形状と突き合わせていれば契約に載らなかった)
  - add-loading (dialog-contract spec の Scenario LD-CO-11 が「旧世代の処理が報告する進捗**・メッセージ**は新しい表示に届かない」と書いたが、公開 API 形状にはメッセージを「処理に紐づけて」報告する経路が存在しない — メッセージ更新は世代に紐づかないグローバル操作 setMessage のみ。実装フェーズでオーナー裁定により遮断対象は進捗のみとなり deviation 記録。spec 起こしの時点で「その報告は誰がどの口から行うのか」を API 形状と突き合わせていれば、契約に載らなかった)
---

## ルール文

デルタスペックの Scenario が「〜からの報告・〜に紐づく操作を遮断する / 区別する」という契約を書くとき、その報告・操作を行う経路 (誰が・どの引数・どの口で) が同じ spec の公開 API 形状に実在するかを突き合わせる。経路の無い主体を持つ契約は、経路を API に足すか、契約から落とす (グローバル操作ならグローバル操作として書く) かに倒してから spec を確定する。

## 経緯

- 2026-08-26 add-loading: 進捗は「処理に渡された報告口」経由なので世代に紐づけられるが、メッセージ更新は世代と無関係な公開操作しか無かった。対称に見える2つ (進捗・メッセージ) を同じ文で遮断対象にしたことで、実在しない経路への契約が生まれた。実装フェーズの発覚だったため deviation + オーナー裁定のコストが掛かった
- 2026-09-05 rollout-user-docs: 遮断契約ではなく移行対応表だが同型 — spec が「実在するはず」と扱った API (移植元の旧 Toast message overload) が公開 API 形状に無かった。別リポジトリの API を対応付ける Scenario でも、写す前に移植元の公開面で実在を確かめる
- 2026-09-05 split-concepts-platform-surface: 遮断契約でも対応表でもなく design の判定基準の例示だが同型 — 「全形態に存在する」と扱った名前を、形態ごとの公開面 (特に KMP commonMain) で確かめずに列挙した。閾値 3 到達
