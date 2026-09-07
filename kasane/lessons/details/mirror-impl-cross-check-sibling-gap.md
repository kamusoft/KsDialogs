---
scope: process
kind: success
severity: normal
count: 5
first-seen: 2026-08-21
last-seen: 2026-09-02
evidence:
  - add-toast (review-003 が、iOS で塞いだ MAUI factory 例外境界の姉妹面として Android を照合し、「Android は既存の受け皿あり・修正不要」という同梱判断が Toast には誤りであることを実証的に検出 — .NET for Android の `JavaProxyThrowable` は `java.lang.Error` 派生で、Toast の受け皿 `catch (Exception)` だけを素通りする (Loading / Dialog の `catch (Throwable)` は捕まる)。javap と生成バインディングの実物確認まで行われ、iOS と対称の修正 (BridgeContentSupply + Kotlin nullable 化) に繋がった)
  - add-presentation-behavior (Android ワーカーが状態機械の実装中に「表示中の呼び出し元キャンセルで settle 通知と専用ハンドラが二重に届き、後者が『もともと退出中 = 脱出口』と誤判定する」穴を踏んで塞ぎ、報告で「iOS も同じ二重ハンドラ構造で同じ穴が残っている可能性」と示唆。オーケストレーターが読み取り専用の調査を委譲して照合したところ iOS に同じ穴が実在 (PB-TR-08 は即完了フックのため検出力なし) — レビュー前に修正し、テストの検出力も強化できた)
  - add-model-binding-di (相方レビューが Android のインライン show に value class 拒否の迂回穴を確定させた後、修正確認の review-002 が「Android で塞いだ穴の姉妹面」として MAUI の同じ箇所を照合し、fallback 構成下のインスタンス渡し show で値型 VM が提示まで到達する同型の穴を検出。オーナー裁定で共通提示入口の実行時検査として対称化された)
  - add-loading (iOS が「最終進捗が終了に追い越されない」保証を `LoadingReportQueue` で機構化 + 契約テスト2本で固定した後、review-003 が Android の同じ箇所を照合し、同じ保証が `Dispatchers.Main.immediate` というディスパッチャ選択の1行だけに依存しテストが無い — 1行を `Main` に替えても既存 LD-PR 系6本は落ちない — ことを検出 (Minor)。逆方向の対称化も同 change 内で実施: Android 側のみにあった開始失敗系の回帰ガードを iOS へ `LoadingStartFailureTests` としてミラー追加)
  - add-kmp-loading-toast-throws (起点は前 change の review-001 が KMP 公開面の姉妹面 `KsLoading` / `KsToast` に `@Throws` 欠落を検出したこと。本 change の探索では、Sample 側で前回 suspend 10 本に付けた `@Throws` の姉妹面として非 suspend の Toast 関数 4 本を照合し、登録経路の 1 本に同型の穴 (非 suspend + 宣言なし = 例外を一切伝播せず abort) を検出して同梱修正。照合の問いは「同じ失敗経路 (登録経路の DialogException) を通るか」に絞った)
---

## ルール文

同名ミラー方式 (複数プラットフォームが同じ Scenario を各自の実装で満たす) の実装で、「設計の遷移表・契約の字面からは見えない穴」が片方のプラットフォームで見つかったら (実装ワーカーの報告でもレビュー指摘でも)、レビューの完了を待たず、他方のプラットフォームの同じ箇所を読み取り専用の調査で照合する。照合の問いは「同じ入力経路 (通知の本数・到着順) と同じ状態分岐が存在するか」「既存テストがその経路を区別できるか (即完了のダブルで素通りしないか)」の2点に絞る。穴があれば小スコープの修正として委譲し、テストの検出力 (修正前に fail すること) を報告に含めさせる。

## 経緯

- 2026-09-02 add-kmp-loading-toast-throws: 姉妹面が「他プラットフォーム」ではなく「同じ境界の別の関数群 (suspend ↔ 非 suspend、ライブラリ ↔ Sample)」でも同じ型。前 change のレビューが公開面の欠落を拾い、その探索が Sample の非 suspend 群を拾った — 境界越えの規則 (Kotlin/Native の @Throws) が同じなら、同じ規則に従うべき関数群はすべて照合対象になる

- 2026-08-21 add-presentation-behavior: 先行した iOS 実装は PB-TR 29 本が green だったが、Android ワーカーの報告を起点に照合したら shown 状態からの呼び出し元キャンセルで dismissal フックが走る前に撤去・配送される穴が見つかった。ミラー実装は「同じ設計から独立に書く」ため、片方が踏んだ穴は高確率で他方にもあり、かつ同じ設計から書いたテストは同じ盲点を持つ。レビュアーの新鮮な目に頼るより、実装者同士の報告を突き合わせる方が早く確実だった
- 2026-08-25 add-model-binding-di: Android の value class 拒否迂回 (インライン show) を塞いだ修正の確認レビューが、姉妹面の照合として MAUI を見て同型の穴 (interface 受けのインスタンス渡し show + View fallback で値型 VM が提示到達) を検出。起点が実装ワーカーの報告ではなくレビュー指摘でも、照合の有効性は同じだった
- 2026-08-26 add-loading: 対象が「穴」ではなく「片側だけ機構化された保証」でも同じ型 — 片方のプラットフォームで保証をわざわざ機構とテストで固めたなら、他方で同じ保証が何に依存しているか (偶然の1行か・テストで固定されているか) を照合する。ミラー方式では、保証の出所の非対称もテスト検出力の非対称も両方が照合対象になる
- 2026-08-28 add-toast: 「他方にも受け皿がある」という**成立の主張そのもの**も照合対象になる型。iOS の穴を塞いだ際の「Android は既存の catch で受かるので修正不要」という判断は、Loading / Dialog (catch Throwable) には正しく Toast (catch Exception) には誤りだった。言語境界の例外変換 (`JavaProxyThrowable` = `Error` 派生) という OS 固有事実が判断を面ごとに反転させており、受け皿の catch 型を1面ずつ実物で確認しない限り「既存の受け皿あり」は成立の証明にならない

- 2026-09-02: success 閾値 5 到達、オーナー承認により `lessons/process.md` の L-001 へ昇格 (add-kmp-loading-toast-throws の蒸留時)。昇格時にルール文の「他方のプラットフォーム」を「姉妹面 (同じ設計・同じ境界規則から独立に書かれた面)」へ一般化した。本ファイルは経緯の保存用
