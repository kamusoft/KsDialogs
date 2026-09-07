---
scope: process
kind: pain
severity: normal
count: 1
first-seen: 2026-09-06
last-seen: 2026-09-06
evidence:
  - proofread-user-skills-ja (ja → en 同期を platform 別に 4 ワーカーへ並列で分けたところ、同じ「覆い」を ios ワーカーは "scrim"、android ワーカーは "overlay" と訳し、references のリンクラベルも android は [Dialog] 単数に統一・kmp は既存の [Dialogs] を維持と割れた。確定前にオーナー判断で overlay / [Dialog] に統一する追加の 1 周を要した)
---

## ルール文

同じ文書群の書き直し・翻訳を複数のワーカーへ並列に分けるとき、コンテキストパッケージに「用語 → 採用する語」の対応表 (既に決まっている語と、決まっていない語の決め方) を含める。分けた後で訳語や表記が割れていたら、統一する語をオーナーに諮ってから 1 体で横断して揃え、その語を handbook の該当規約へ書く。

## 経緯

- 2026-09-06 proofread-user-skills-ja: ja の校正はオーナーとの対話で語が決まっていったが、en 同期の fan-out 時点では「Dialog / built-in message Toast / typed 接頭語の除去」だけを渡し、「覆い」やリンクラベルの単複は各ワーカーの判断に任せた。並列ワーカーは互いの出力を見ないため、判断が分かれる語は事前に固定するしかない。統一後の語は handbook/cross/user-skill-writing-style.md に記録した
