---
scope: impl
kind: pain
severity: normal
count: 1
first-seen: 2026-09-10
last-seen: 2026-09-10
evidence:
  - add-release-workflow (翻案元 KsSettingsView の publish job は失敗経路の 3 step (deployment ID の拾い直し / drop / artifact への書き戻し) を「拾い直した ID がある」「drop で消せた」ときだけ走らせる `if:` のガードを持っていた。Maven Central の deployment を Android / KMP の 2 枠へ一般化して写す際にこのガードが落ち、3 step とも無条件の `if: failure()` になった。外部状態の判定や artifact の download より前に落ちた attempt が、前の attempt の引き継ぎ用 ID を空で上書きし、次の attempt が保留 deployment をもう 1 件作る経路が開いた。review-001 Major と second-opinion-code-001 Major の双方が検出)
---

## ルール文

翻案元の step / 関数を「2 枠化」「複数 ID 化」のように一般化して写すときは、翻案元の条件式 (`if:` のガード・output の有無判定・early return) を 1 つずつ列挙し、一般化後の各出現箇所に同じ条件が残っているか (または集約した 1 つの条件で同じ経路を塞いでいるか) を表で突き合わせる。落とした条件には落とした理由を実装報告に書く。守れたかは、翻案元と翻案先の条件式の対応表が実装報告か evidence にあることから判定する。

## 経緯

- 2026-09-10 add-release-workflow: 修正は「引き継ぎ ID の読み込みを fallible な download より前へ移し、失敗経路 3 step を `failure() && <引き継ぎを読み込み済み>` で守る」形で解消し、ガードを外した写しで同じ検査が 7 件とも NG になる証跡 (evidence/deployment-id-handoff-guard.txt) を添えた。翻案元 review-001 の Major「失敗経路で drop した ID が artifact に残る」の裏返しにあたる欠陥で、翻案元のレビュー指摘を「解消済みか」だけで照合し、そのガード自体が翻案先で保たれているかを見なかった。近縁: [[translated-norm-needs-local-basis-and-fact-check]] (翻案元の規範・前提の確認) — こちらは規範ではなく機構の条件式の話
