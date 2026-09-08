---
scope: spec-review
kind: pain
severity: normal
count: 1
first-seen: 2026-09-08
last-seen: 2026-09-08
evidence:
  - add-native-distribution (相方スペックレビューが、翻案元から持ち込む同期スクリプトの `rm -rf` を全体ルール「削除は trash」との衝突として Major に挙げ、ホスト側も採用した。オーナー裁定「CI はルール外なので rm -rf でよい」で、ルールの適用範囲は人の操作環境であり CI ランナーで動くスクリプトは対象外と確定した)
---

## ルール文

利用者の全体ルール (CLAUDE.md / AGENTS.md の「削除は trash」のような操作規律) を、成果物のコード (CI ランナーや消費者環境で動くスクリプト・ビルドファイル) に適用して指摘・修正するときは、そのルールが誰の操作を縛るものか (人・エージェントの対話操作か、成果物の実行環境か) を先に判定し、実行環境にそのツールが無い場合は指摘ではなく「適用範囲の確認」として起こす。守れたかは、指摘本文に「このルールの適用範囲は〜」の判定が書かれているか、または成果物側の規約 (handbook) に適用範囲が明記されていることから判定する。

## 経緯

- 2026-09-08 add-native-distribution: 翻案元 KsSettingsView の `sync-snapshot.sh` は `.git/` 以外を `rm -rf` で除去する。`trash` は CI ランナーに無く、release workflow から呼ぶ経路が成立しない。オーナー裁定を design Decision 1 に記録し、以後は CI スクリプト内の削除は `rm -rf` でよいこととした。近縁: [[translated-norm-needs-local-basis-and-fact-check]] (翻案時の規範は根拠と現状を確かめてから採録する)
