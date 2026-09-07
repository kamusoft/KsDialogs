---
scope: code-review
kind: pain
severity: normal
count: 2
first-seen: 2026-08-14
last-seen: 2026-08-19
evidence:
  - add-monorepo-scaffold (review-001 が BuildProbe の public 露出を見逃し、相方 second-opinion-code-001 が Major で検出。修正サイクル1周が発生)
  - expand-api-surface (review-001 が KsDialogsKmpError の宣言表外 case を見逃し、相方 second-opinion-code-001 が Major で検出。修正サイクル1周が発生)
---

## ルール文

ライブラリの変更をレビューするとき、追加・変更された型やシンボルが配布成果物 (aar / NuGet / SwiftPM product) の公開 API 面をどう変えるかを必ず確認する。可視性 (意図しない public) と形状 (凍結済みの宣言表・公開 enum の case 集合・シグネチャ) の両方を見る。

## 経緯

- 2026-08-14 add-monorepo-scaffold: スモークテスト用マーカー (BuildProbe) が Kotlin object / C# public class として公開 API に混入。ホストレビューは APPROVED としたが、相方 (codex) が「後で削除すると破壊的 API 変更になる」と Major 指摘し、internal 化 + InternalsVisibleTo の修正が発生した
- 2026-08-19 expand-api-surface: `KsDialogsKmpError` に design.md Decision 6 の凍結済み宣言表にない case が2つ追加されていた。ホストレビューは「宣言レベルの形 (引数・型引数・result ラベル・別名) はすべて一致」と判定して enum の case 集合に触れず、相方 (codex) が「公開 enum の case 追加は利用者の網羅的 switch に影響する」と Major 指摘。宣言表どおりの2 case へ収束させる修正が発生した

## 観測されている傾向

2件とも**ホスト側は「一致している」と判定した上での見逃し**であり、見落としの型は「シグネチャは照合したが、可視性や case 集合といった別軸の API 面を照合対象に含めていない」。公開 API 面の照合を「宣言表の行と実装の行を突き合わせる」作業として捉えると取りこぼす軸がある。
