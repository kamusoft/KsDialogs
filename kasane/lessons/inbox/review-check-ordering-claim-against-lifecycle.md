---
scope: code-review
kind: pain
severity: normal
count: 1
first-seen: 2026-09-09
last-seen: 2026-09-09
evidence:
  - add-kmp-maven-distribution (spec Requirement が SNAPSHOT ガードに「ネットワークアクセスや認証より前に」失敗することを求めていたのに、実装はタスクの `doFirst` に置かれ、Gradle は Central リポジトリの認証情報を task graph 確定時に解決するため認証情報の無い環境ではガードに届かなかった。evidence/snapshot-central-publish-guard.txt 自体が「ダミー認証情報なしではガードに到達しない」と書いていたが、ホスト review-001 は通し、相方 second-opinion-code-001 が Major で検出。設定段階の要求タスク名照合 + task graph 確定時の 2 段に修正し、翻案元 android/ も同じ形へ揃えた)
---

## ルール文

spec や design が「X より前に失敗する / 動く」という順序の要求を持つとき、レビューはその実装が置かれたフック (ビルドツールなら設定段階・task graph 確定時・タスク実行時の `doFirst` など) が要求の順序で実際に発火するかを、ビルドツールのライフサイクルと、要求の「前」にあたる条件を欠いた環境 (認証情報なし・オフライン) での実行証跡で確かめる。証跡の本文に「到達しない」「先に落ちる」の記述があれば、順序の要求が満たされていない印として指摘する。

## 経緯

- 2026-09-09 add-kmp-maven-distribution: 翻案元 (android/) と同じ `doFirst` 方式を写したことで、順序の要求を実装の形ではなく先例の踏襲で満たしたと判定していた。証跡には不成立の観測が書かれていたが、レビューは証跡の存在で確認済みと扱い本文の含意を読まなかった
