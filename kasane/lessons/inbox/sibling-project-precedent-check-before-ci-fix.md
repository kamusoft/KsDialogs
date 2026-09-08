---
scope: process
kind: success
severity: normal
count: 1
first-seen: 2026-09-08
last-seen: 2026-09-08
evidence:
  - add-verification-ci (CI の ios job が 2 回連続で落ちたとき、オーナーの提案で対処を選ぶ前に姉妹プロジェクト KsSettingsView の handbook / lessons / archive を scout に照合させた。同型の事例 4 件 (いずれもテスト側の待機条件の是正で解決、CI 側のノブは未使用) と待ち補助の既定値・延長却下の経緯が返り、今回の失敗の形 (待ちの上限を大きく超える・落ちるテストが回ごとに入れ替わる) がそれらと違うことを根拠に「CI だけ直列化」を選べた)
---

## ルール文

同型の仕組み (CI・ビルド・テスト基盤) を先に運用している姉妹プロジェクトが `../<リポジトリ名>/` にあるとき、その仕組みで起きた失敗の対処を選ぶ前に、姉妹側の handbook / lessons (inbox 含む) / `changes/archive/` の exploration を読み取り専用の調査 (scout) で照合し、「同型の事例があるか・どう対処したか・今回の失敗の形が同じか」の 3 点を要約で受け取る。同じなら対処を翻案し、違うなら違いを根拠に書いてから別の対処を選ぶ。守れたかは、判断依頼または deviation に姉妹側の該当パスと「同じ / 違う」の判定が書かれていることで判定する。

## 経緯

- 2026-09-08 add-verification-ci: 手元では通る iOS テストがランナーで落ちた。姉妹側の 4 事例は待機条件の問題で超過が数秒だったが、今回は 28〜57 秒の飢餓で別物。照合したことで「待ち延長」「待機条件の是正」を根拠なく選ばずに済んだ。近縁: [[translated-norm-needs-local-basis-and-fact-check]] (翻案元の前提を翻案先で裏取りする)
