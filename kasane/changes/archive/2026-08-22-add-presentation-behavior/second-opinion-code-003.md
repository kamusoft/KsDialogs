# セカンドオピニオン: add-presentation-behavior (code-003)
**相方**: codex (同一セッションへの追送、器 ksn-reviewer) / **日付**: 2026-08-22 / **対象**: 修正サイクル 2 周目 (iOS: 配送口の切り離し・removed の時点・UIKit 本命経路のテスト) 後の作業ツリー。入力はホスト側 review-003 と同一
---
# 再レビュー結果: add-presentation-behavior

**判定: APPROVED**

前回の Critical / Major はすべて解消しています。新しい配送 coordinator、撤去状態の修正、UIKit completion の結線にブロッキングな退行は見つかりませんでした。残る指摘は文書上の Minor 2件のみです。

## 解消状況

| 項目 | 判定 |
|---|---|
| 器の解放による配送口消失 | **解消** |
| `.removed` の設定時点 | **解消** |
| UIKit completion の本命経路 | **解消** |
| 申し送り件数 | **一部未反映** |
| MAUI iOS 無効状態 | **降格妥当・今回の違反ではない** |

`DialogOutcomeDelivery` は destination と outcome の到着順を吸収し、`isDelivered` により二重配送も防いでいます。deinit が coordinator を MainActor Taskへ強く引き継ぐため、器の解放と同時に配送口が失われる問題もありません。公開 API の増加もありません。

テストを直接器の解放へ組み替えた判断も妥当です。`show` の async フレームが器を保持する構成では故障条件を確実に作れませんが、`deliveryOutlivesContainerRelease` は問題となった「器が先に解放される順序」を直接かつ決定的に再現しています。既存のPBテストがPresenterとの結線を、追加テストがdeinit時の寿命を分担しており、責務分割も適切です。

## 指摘事項

### [🟡 Minor] handoff-distill.md の最終iOS件数がまだ130件になっている

**該当箇所**: [handoff-distill.md:5](<kasane/changes/archive/2026-08-22-add-presentation-behavior/handoff-distill.md:5>)、[handoff-distill.md:30](<kasane/changes/archive/2026-08-22-add-presentation-behavior/handoff-distill.md:30>)

**問題点**: 負の検査8本、MAUI 9本、合計27本は正しく直っていますが、iOSの通常テスト件数は両箇所とも130件のままです。今回提示された最終実測は133 testsです。

**推奨修正**: 蒸留時に古い値を採用しないよう、最終値を `133 tests / 25 suites` へ更新してください。

### [🟡 Minor] viewDidDisappear fallback の説明が同一段落内で矛盾している

**該当箇所**: [deviation.md:22](<kasane/changes/archive/2026-08-22-add-presentation-behavior/deviation.md:22>)

**問題点**: 実測状況を「completionが届かず、`presentingViewController` も解けない」と記録した直後に、fallbackは `presentingViewController == nil` を確認するため completion未着時に機能すると説明しています。現在の実装では、提示関係が残る実測ケースにはfallbackは作用しません。

実装側のnil確認は妥当です。提示関係が残ったまま配送すると、次のダイアログ提示との競合を再導入するためです。問題は説明だけです。

**推奨修正**: 「viewDidDisappear fallbackは、View消失後に提示関係まで解けたがcompletionだけ届かない場合に限る。シーンなしランナーの提示関係が残るケースはテストsurfaceで代替する」と記録を修正してください。

## MAUI iOSの無効状態について

今回の降格に同意します。`sample-parity.md` が明示的に列挙する状態は選択状態・ON/OFF・現在値であり、無効状態までは現在の凍結契約に含まれていません。VoiceOver品質として改善価値はありますが、別のS級UI微調整としてオーナー判断へ送る扱いが適切です。

## サマリー

- Critical: 0
- Major: 0
- Minor: 2
- Suggestion: 0

ホスト提示の `133 tests / 0 failures` 等を根拠として扱い、ビルド・テストは再実行していません。ファイルへの書き込みも行っていません。


## 突き合わせ結果 (ホスト review-003.md との照合、2026-08-22)

| # | 指摘 | ホスト (review-003) | 相方 (codex) | 採否 | 根拠 |
|---|---|---|---|---|---|
| 1 | 2 周目 #1 (配送口の寿命) / #2 (`removed` の時点) / #3 (UIKit 本命経路) | 解消 | 解消 | **確定 (解消)** | 双方一致。設計判断 (a) 三点併用 (b) `[weak self]` の非対称 (c) テスト組み替え も双方が妥当と評価 |
| 2 | `DialogContainerViewController.swift` の `dismissRequest { [weak self] … }` に理由コメントが無い (揃える方向に直すと #1 が静かに再発) | 🟡 M-1 | — | 採用 (ホスト) | オーケストレーターが直接コメントを追加 (コメントのみ)、確認はレビュアーに追記依頼 |
| 3 | `DialogOutcomeDelivery.setDestination` の `guard !isDelivered` が届け先を黙って捨てる前提が明文化されていない | 🔵 S-3 | — | 採用 (コメント) | 同上、前提条件をコメントで明文化 |
| 4 | 撤去要求〜完了通知の窓での解放を直接固定するテストが無い (機構同一のため任意) | 🔵 S-2 | — | 申し送り | handoff-distill.md へ |
| 5 | handoff-distill.md の iOS 件数 130 → 133 | — | 🟡 Minor | 確定 (修正済み) | 相方の読み取り後に更新済み。現在は両箇所とも 133 |
| 6 | deviation.md の `viewDidDisappear` fallback の説明が同一段落で矛盾 | — | 🟡 Minor | 採用 (文書) | オーケストレーターが訂正済み (fallback は提示関係まで解けて completion だけ届かない場合に限る。シーンなしランナーのケースはテスト用提示面で代替) |
| 7 | MAUI iOS の無効状態 (2 周目 #4 の降格) | — | 降格妥当・今回の違反ではない | 確定 (降格) | 相方も同意。オーナー判断 (S 級 UI 微調整) へ |

- 判定: ホスト APPROVED / 相方 APPROVED。採用 (相方のみ): 1 件 (#6、文書) / 降格: 0 件 (新規) / 未解決: 0 件
- レビューサイクル: 3 周 (上限 3) で収束。同一指摘の 2 周連続残存なし
