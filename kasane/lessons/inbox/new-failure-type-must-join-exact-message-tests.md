---
scope: impl
kind: pain
severity: normal
count: 1
first-seen: 2026-09-08
last-seen: 2026-09-08
evidence:
  - add-maui-nuget-distribution (失敗種別 `DialogException.ViewCreationFailed` を 7 種目として新設し英語文言も書いたが、失敗型ごとの完全一致テスト `DM_MA_01` (handbook cross/diagnostic-message-language.md「検査」節が英語文言の正しさを担うと定める系列) は「全入れ子型 6 種」のまま新型を含めず、Kotlin 互換面の新規テスト MB-MA-15 も `failed:` / `failure:` の接頭辞だけを見ていた。また MB-MA-11 は InnerException の非 null しか見ず spec の「DI の解決失敗の例外」を識別できなかった。ホスト review-001 は見逃し、相方 second-opinion-code-001 が Minor (優先度高) で検出、修正サイクルで完全一致と InnerException の同一性まで固定した)
---

## ルール文

失敗種別 (例外の入れ子型・error の case・診断 ID) を新設または追加したら、同じ change の中で、その系列の失敗型を網羅する完全一致テスト (MAUI `DM-MA-01` / iOS `DM-IO-*` / Android `DM-AN-*` の系列と、互換面の文言テスト) に新しい型のメッセージ・公開プロパティ・元例外の保持 (`Is.SameAs` 等の同一性) を加える。新しい失敗を検証する Scenario テストは「非 null」「接頭辞一致」ではなく、元例外の同一性か型 + 識別できる内容で assert する。守れたかは、diff に網羅テストの該当行 (種数のコメントを含む) の更新が含まれることと、新設型を名指しした完全一致 assertion が存在することから判定する。

## 経緯

- 2026-09-08 add-maui-nuget-distribution: 網羅テストは「N 種」を doc コメントに持っていたが、種数を数える assertion は無く、新型を足しても落ちなかった。文言の誤記が素通りする状態で review-001 も検出できず、相方が検出した。近縁: [[test-count-table-must-follow-in-change]] (テストが育ったら件数表を同じ change で更新する) / [[review-suggested-evidence-lacks-discriminating-power]] (識別力のない証跡)
