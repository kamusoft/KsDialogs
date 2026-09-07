---
scope: process
kind: pain
severity: normal
count: 3
first-seen: 2026-08-28
last-seen: 2026-09-06
evidence:
  - add-loading-toast-typed-show (proposal の Impact が「破壊的変更なし: 公開 API の追加のみ」と書いたが、型指定 show / start は 3 形態とも契約 protocol / interface の抽象メンバーとして増え、契約を自前で実装する型 (利用者のテストダブル・adapter) は再準拠が要る。相方 second-opinion-code-001 が Major として指摘し、オーナーは Dialog の先例と同型として現状維持を選んだうえで「非破壊 = 呼ぶ側の互換」を deviation.md と concepts に明文化。主張自体は残ったが、範囲が書かれていなかったため判断依頼 1 往復が発生した)
  - add-toast (deviation.md が iOS factory 契約の throws 化を「ソース互換 (samples 無改変ビルドで実証)」と記載したが、samples が実証したのは呼び出し側の互換だけで、公開 protocol `KsLoading` / `KsDialogs` に外部で準拠していた型は再準拠が必要 (Swift の witness マッチングは引数位置の関数型に反変の緩和を持たない — review-003 が最小再現で確認)。主張が実証の範囲より広く、リリースノートに書くべき破壊的変更を取りこぼす risk があった。オーナー承認のうえ deviation.md を「呼び出し側のソース互換」へ限定する表記に修正)
  - fix-android-instrumented-toast-back-loading-coalescing (evidence の注記が「予測型バックは API 36 固有」と、実測 (API 29 で従来経路・API 36 で失敗) の範囲を超えて一般化していた。review-001 Minor が targetSdk 35 以上では API 35 端末から既定有効になり得ると指摘。API 35 は修正前の証跡に無く、注記を実測の範囲 (予測型バックが有効でない環境では従来経路) へ限定する表現に修正)
---

## ルール文

「互換」「破壊的変更なし」を成果物 (deviation.md・proposal・リリースノート) に書くときは、主張の範囲を実証した範囲に限定して書く — 何で実証したか (無改変ビルド・テスト・最小再現) と、その実証が**カバーしない面** (公開 protocol / interface への外部準拠・ABI・シリアライズ形式など) を分けて明記する。実証していない面が壊れうるなら、互換の主張ではなく破壊の記録として書く。

## 経緯

- 2026-08-28 add-toast: 呼び出し側 (クロージャを渡す側) と準拠側 (protocol を実装する側) で互換性の成立条件が異なるのに、「ソース互換」の一語でまとめて主張していた。一般公開前で実害はなかったが、蒸留・リリースノート起草はこの記録を正として拾うため、記録時点で範囲を限定しておかないと下流で破壊的変更が消える
- 2026-09-06 add-loading-toast-typed-show: 提案段階の「破壊的変更なし」に範囲 (呼ぶ側 / 実装する側) が添えられておらず、実装後に相方レビューが「実装する側が壊れる」を Major に上げた。範囲を proposal に書いておけば、指摘は Suggestion 止まりで済んだ (code-review 側の同型は additive-contract-member-follows-accepted-precedent に捕捉)
- 2026-09-06 fix-android-instrumented-toast-back-loading-coalescing: 互換の主張ではなく原因の一般化 (「API 36 固有」) の形で同じ取りこぼしが出た。証跡は蒸留後も残る記録なので、原因の説明も測った端末・条件の範囲で書き、測っていない API レベルへの断定を含めない
