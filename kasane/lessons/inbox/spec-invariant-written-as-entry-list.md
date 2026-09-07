---
scope: spec-review
kind: pain
severity: normal
count: 2
first-seen: 2026-08-25
last-seen: 2026-09-02
evidence:
  - add-model-binding-di (android-native spec が「value class の VM は登録および型指定 show の時点で拒否する」と拒否点を2つに列挙し、列挙外のインライン show・インスタンス渡し show が検査なしで提示まで到達する抜け穴になった (相方レビューが Major で検出)。maui-binding spec も「ジェネリック制約に class を含めコンパイル時に拒否する」と強制手段を限定して書き、class 制約が書けない interface 受けのインスタンス渡し show + View fallback 構成の経路を数え落とした。どちらも修正は全 show 経路が通る共通提示入口での検査への統一で、spec が最初から「全 show 経路で拒否する (強制手段は形態別)」と不変条件として書いていれば列挙漏れは構造的に起きなかった)
  - add-maui-ios-bridge-verification (maui-binding spec の Loading Scenario BV-MA-02 が「開始を要求する」とだけ書き、tasks も 1 本しか置いていなかったため、互換面の 2 入口 (`showContent:completion:` = beginUse / `startContent:action:completion:` = runScope) のどちらを固定するかが未定で、片方だけの検証で Scenario を満たした扱いにできた。相方 spec-review が Major で検出し、BV-MA-02 (表示形) と新設 BV-MA-07 (スコープ形) に分割して両入口を固定)
---

## ルール文

デルタスペックの Requirement が「〜な入力を拒否する」「〜を保証する」という不変条件を特定の入口・強制手段の列挙 (登録と型指定 show で / コンパイル時に) で書いていたら、レビューは公開面の全経路 (インライン show・インスタンス渡し show・fallback 構成・ブリッジ経由) を数え上げて列挙と突き合わせ、漏れる経路があれば「全経路に適用し、強制手段は経路ごとに定める」形へ書き直させる。不変条件の適用範囲と強制手段の列挙は別のものとして書く。

## 経緯

- 2026-09-02 add-maui-ios-bridge-verification: 列挙が過剰だった前回とは逆に、入口を 1 つも名指ししない書き方で経路を数え落とした。どちらも「公開面の全経路を数え上げて Scenario と突き合わせる」を提案段階で行っていれば防げた同型 (蒸留時に照合して count 2)
- 2026-08-25 add-model-binding-di: VM 契約の参照型限定 (core/ADR-0018) は契約全体に及ぶ不変条件なのに、Android / MAUI の両 spec が拒否点・強制手段の列挙として書いたため、列挙外の経路が抜け穴として実装に残った。実装フェーズで相方レビュー→姉妹面照合→オーナー裁定の3段を経て全経路検査へ収束したが、spec の書き方の問題として提案フェーズで防げた
