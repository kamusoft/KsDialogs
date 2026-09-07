---
scope: code-review
kind: pain
severity: normal
count: 1
first-seen: 2026-09-02
last-seen: 2026-09-02
evidence:
  - add-maui-ios-bridge-verification (相方 code-review Major が、Scenario BV-MA-05 の「資源パッケージ再生成もスキップ」の字面に対し、.NET for iOS SDK の既定挙動 (スキップしたビルドで出力 stamp が `FileWrites` に載らず IncrementalClean に消され 1 ビルドおきに再実行) を Binding 側の MSBuild 配線で塞いで毎回スキップにせよと処方した。ホスト review-001 は同じ差を Minor とし deviation.md への記録を処方。オーナーは「SDK 内部への依存を増やしてまで守らない」と判断し deviation 記録に倒した — 再実行は同じ内容の資源パッケージを作り直すだけで、症状 (古いバイナリの配備) にもビルド時間にも影響しない)
---

## ルール文

Scenario の字面と実測の差が SDK / ツールチェインの既定挙動に由来し、Requirement の趣旨 (症状の再発防止・ビルド時間) に影響しないとき、レビューはその字面を満たすために SDK 内部への依存を増やす修正を処方しない: 代わりに deviation.md への記録 (趣旨を満たしている根拠・SDK 既定であることの A/B 実測・実測の出どころ) を処方し、字面を直すか手当てを足すかの判断はオーナーへ回す。重要度は「趣旨への影響」で付け、字面の不一致だけで Major にしない。

## 経緯

- 2026-09-02 add-maui-ios-bridge-verification: ホストと相方の両レビューが同じ差を検出したが、処方が割れた (記録 vs 配線修正)。オーナー判断は記録側で、ADR-0003 の Consequences「SDK 内部ターゲットへの依存を抱える」を増やす方向を避けた。相方の指摘は「趣旨への影響がない差」を仕様違反として Major 扱いしており、修正すると依存が 1 か所増えるだけで利用者の得るものがなかった
