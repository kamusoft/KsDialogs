# セカンドオピニオン: fix-android-startup-toast-under-splash (spec-001)
**相方**: codex / **label**: so-spec-fix-android-startup-toast-under-splash / **日付**: 2026-09-29 / **対象**: 提案一式 (proposal.md / design.md / specs/ / tasks.md)
---
## 提案レビュー結果

**判定: NEEDS_DISCUSSION**（Major 5件、Minor 1件）。期限の開始条件と提示先の状態遷移に、実装者だけでは解決できない仕様上の穴があります。以下を決めてから実装に進むべきです。

### 指摘事項

1. **🟠 Major — iOS の一部のシーンが背面に移っただけで計時を始めうる**  
   **該当箇所:** [design.md:72](kasane/changes/fix-android-startup-toast-under-splash/design.md:72)、[ios-native/spec.md:13](kasane/changes/fix-android-startup-toast-under-splash/specs/ios-native/spec.md:13)  
   **問題点:** 前面の定義は「該当シーンが1つ以上」ですが、離脱の合図は個々の `didEnterBackgroundNotification` です。複数シーンのうち1つが背面に移っても、別のシーンが前面なら Toast の期限を確定してはいけません。  
   **推奨修正:** 通知後に全シーンの前面状態を読み直し、前面シーンがゼロになった場合だけ期限を確定する要件と Scenario を追加してください。

2. **🟠 Major — Android の作り直し中に「前面の待ち」が途切れる**  
   **該当箇所:** [design.md:66](kasane/changes/fix-android-startup-toast-under-splash/design.md:66)、[android-native/spec.md:56](kasane/changes/fix-android-startup-toast-under-splash/specs/android-native/spec.md:56)、[toast-contract/spec.md:95](kasane/changes/fix-android-startup-toast-under-splash/specs/toast-contract/spec.md:95)  
   **問題点:** 提案は Activity の「作り直し」を前面の待ちに含めますが、旧 Activity の stop から新 Activity の create まで集合が空になると、設計上は背面離脱として期限が始まります。新画面の描画が遅ければ、対象の Toast が表示前に満了します。  
   **推奨修正:** この間を背面として扱うのか、作り直し中として待ちを維持するのかを決め、旧画面の stop から新画面の描画までを通す Scenario を設けてください。

3. **🟠 Major — 「受理時の状態」と「開始処理時の状態」が食い違う**  
   **該当箇所:** [toast-contract/spec.md:93](kasane/changes/fix-android-startup-toast-under-splash/specs/toast-contract/spec.md:93)、[design.md:78](kasane/changes/fix-android-startup-toast-under-splash/design.md:78)、[ADR-0043:32](kasane/decisions/core/0043-toast-duration-from-visible-when-accepted-in-foreground-without-host.md:32)  
   **問題点:** ADR と要件は「前面の待ちの間に受理された Toast」を対象とします。一方、設計は UI スレッドで開始処理が走る時点に分類します。show の後、UI スレッドが処理する前にアプリが背面へ移ると、どちらの時間モデルになるか記述が矛盾します。  
   **推奨修正:** 分類の基準時点を一つに統一し、受理から開始処理までに前面状態が変わる Scenario を追加してください。

4. **🟠 Major — 描画通知の遅延処理が stop 後の印を復活させうる**  
   **該当箇所:** [design.md:41](kasane/changes/fix-android-startup-toast-under-splash/design.md:41)、[android-native/spec.md:35](kasane/changes/fix-android-startup-toast-under-splash/specs/android-native/spec.md:35)  
   **問題点:** `OnDrawListener` から `View.post` で印を立てるまでに Activity が stop すると、stop で消した印を古い callback が再び立てられます。次の start・resume で、今回の描画を待たず提示先になりえます。既存の PB-HA-06 はこの順序を検証しません。  
   **推奨修正:** stop・破棄時に観測を無効化し、遅延 callback が同じ観測世代か確認する条件と、この順序の Scenario を追加してください。

5. **🟠 Major — 「表示時間いっぱい見える」の受け入れ基準が定まっていない**  
   **該当箇所:** [proposal.md:41](kasane/changes/fix-android-startup-toast-under-splash/proposal.md:41)、[design.md:184](kasane/changes/fix-android-startup-toast-under-splash/design.md:184)、[tasks.md:55](kasane/changes/fix-android-startup-toast-under-splash/tasks.md:55)  
   **問題点:** 提案の到達点は表示時間いっぱい見えることですが、設計は最初の描画から起動画面の退場までの時間を消費しうると認めています。実測で「目立つ場合」に相談する条件にも数値や判定方法がありません。CA-SA-10 の「5回とも写る」だけでは、見える時間が足りるか判定できません。  
   **推奨修正:** 起動画面退場後に見える時間の許容差、測定開始・終了コマ、基準を満たさなかった場合の扱いを実装前に定めてください。

6. **🟡 Minor — 実装コメントの ADR 表記が規約に不足する**  
   **該当箇所:** [tasks.md:25](kasane/changes/fix-android-startup-toast-under-splash/tasks.md:25)、[tasks.md:39](kasane/changes/fix-android-startup-toast-under-splash/tasks.md:39)  
   **問題点:** タスクの `ADR-0043`・`ADR-0044` は、[コメント規約](kasane/handbook/cross/comment-policy.md:21)が定めるドメイン付き ID ではありません。ID だけのコメントも単独では実装理由を説明しません。  
   **推奨修正:** 非公開の実装コメントで理由を現在形で説明し、参照が必要なら `core/ADR-0043`・`core/ADR-0044` を使うようタスクを明確にしてください。

**照合した規約:** コメント、テスト実行、状態遷移テスト、実行時挙動の検証、Sample パリティ。提案・既存仕様・コードの静的レビューのみを行い、ビルドとテストは実行していません。ファイルは変更していません。

## 突き合わせ結果

ホスト側の自己レビュー (ksn-propose Step 8、2 周) は、6 件のどれも検出していなかった (自己レビューで出した所見は、前面の待ちの Toast が表示時間で終わらなくなることの記録だけ)。6 件とも相方のみの指摘として、根拠で採否を決めた。

| # | 重要度 | 採否 | 根拠 | 反映先 |
|---|---|---|---|---|
| 1 | Major | 採用 | 前面の定義 (前面のシーンが 1 つ以上) と、シーンごとの背面の通知が食い違う。複数シーンで誤って数え始める具体的な筋書きがある | design Decision 2、ios-native (合図は前面を読み直してから届ける、PB-HI-07 の前提を変更、PB-HI-09 を追加)、tasks 4.1・4.3 |
| 2 | Major | 採用 | 構成の変更による作り直しでは、旧 Activity の stop・破棄と新 Activity の作成が同じメッセージの中で続く。追跡役は通知を同期に届けるので、その間に集合が空になって背面と判定される | design Decision 2、android-native (背面の確定は次のメッセージ周回、PB-HA-09 の前提を変更、PB-HA-11 を追加)、tasks 2.3・2.4 |
| 3 | Major | 採用 | 要件は「前面の待ちの間に受理された」、design は開始処理の時点で判定しており、show から開始処理までの間に状態が変わったときの扱いが書かれていない | design Decision 2、toast-contract (判定の時点を開始処理の手番と明記し、間で変わったら開始処理の時点に従う)。決定的に作れる場面ではないため、Scenario は足さない |
| 4 | Major | 採用 | 描画を受けてから post で印を立てるまでの間の stop が、stop の後に印を復活させる。具体的な順序がある | design Decision 1 (観測の世代)、android-native (印になる前の stop では印にしない、PB-HA-10 を追加)、tasks 2.2・2.4 |
| 5 | Major | 採用 (オーナー判断 2026-09-29) | 「表示時間いっぱい見える」を判定する数値と測り方が無い。数値の基準を置くかどうかはオーナーの判断として提示し、オーナーが数値の合格ライン (起動画面が退いた後に表示時間の 2/3 以上が見える) を採用した | design Decision 7 (画面の録画で測る・合格ライン・割ったら止める)・samples (CA-SA-10)・tasks 1.2・7.4・proposal・core/ADR-0044 の Revisit When |
| 6 | Minor | 採用 | `kasane/handbook/cross/comment-policy.md` の「ADR ID」は、ドメインを定義したプロジェクトでは `<domain>/ADR-NNNN` の形を求める。規約に根拠があり、好みの域ではない | design Decision 1・2、tasks 2.6・4.4 |

件数: 確定 0 / 採用 6 / 降格 0 / 未解決 0
