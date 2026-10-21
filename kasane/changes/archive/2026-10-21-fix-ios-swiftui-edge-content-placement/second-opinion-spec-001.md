# セカンドオピニオン: fix-ios-swiftui-edge-content-placement (spec-001)
**相方**: codex / **label**: so-spec-fix-ios-swiftui-edge-content-placement-001 / **日付**: 2026-10-21 / **対象**: 提案一式 (proposal.md / specs/ios-native/spec.md / tasks.md)
---
判定は **修正要求** です。実装前に解消すべき Major が 4 件あります。

1. **Major — 比率サイズを伴う start / end の扱いが、仕様とタスクで異なる**  
   該当箇所: [tasks.md:11](kasane/changes/fix-ios-swiftui-edge-content-placement/tasks.md:11)、[spec.md:8](kasane/changes/fix-ios-swiftui-edge-content-placement/specs/ios-native/spec.md:8)  
   仕様は start / end を「片側の端の軸」と定義し、比率サイズを除外していません。一方、タスク 1.1 は内容サイズに委ねた軸だけを対象とし、比率サイズを明示的に除外しています。現行の `DialogLayoutResolver` でも、比率サイズと start / end は併用できます。このままでは、仕様が保証する配置の一部が未実装になります。  
   **推奨修正:** 比率サイズを伴う start / end を対象に含めるか、仕様・提案・ADR の対象範囲をそろえて除外してください。いずれの場合も、その条件の Scenario を追加してください。

2. **Major — はみ出し判定の保持条件では、安全領域だけ変わったときに判定が古くなる**  
   該当箇所: [tasks.md:13](kasane/changes/fix-ios-swiftui-edge-content-placement/tasks.md:13)、[spec.md:32](kasane/changes/fix-ios-swiftui-edge-content-placement/specs/ios-native/spec.md:32)  
   タスクは、器か理想サイズが変わるまで判定を保持します。しかし、窓全体基準では、器と理想サイズが同じまま窓の安全領域だけ変わり、収まる／収まらないが切り替わります。例えば高さ 100・最小高さ 80 の中身では、下の重なりが 34 なら収まらず、0 なら収まります。保持した判定では、仕様が約束する安全領域の更新と並び方に追従できません。  
   **推奨修正:** 重なり・対象軸・付いた側も判定の無効化条件に含め、器と理想サイズを固定して安全領域だけを変更する往復 Scenario を追加してください。

3. **Major — 上限と記憶した幅を比較する条件では、同じ上限で求め直しを繰り返す**  
   該当箇所: [tasks.md:20](kasane/changes/fix-ios-swiftui-edge-content-placement/tasks.md:20)、[spec.md:191](kasane/changes/fix-ios-swiftui-edge-content-placement/specs/ios-native/spec.md:191)  
   「上限が覚えている幅より広がったら」という条件は、上限自体の拡大を判定していません。探索には、上限 734 に対して本文の幅が 733.33 になる例が記録されています。この場合、同じ上限の通知でも条件が成立し、幅の破棄と再計測を繰り返し得ます。「有効領域が変わらない配置の値は変えない」という契約とも衝突します。  
   **推奨修正:** 前回通知した有効領域の上限と、新しい上限を比較する形にしてください。上限よりわずかに狭い要求幅で落ち着いた状態に、同じ上限を繰り返し通知する回帰 Scenario も必要です。

4. **Major — 器より大きい縮まない中身では「矩形が同じ」という保証を満たせない**  
   該当箇所: [spec.md:135](kasane/changes/fix-ios-swiftui-edge-content-placement/specs/ios-native/spec.md:135)、[spec.md:9](kasane/changes/fix-ios-swiftui-edge-content-placement/specs/ios-native/spec.md:9)  
   器は有効領域で頭打ちになりますが、縮まない中身の大きさには上限がありません。例えば高さ 1,000 の固定 View を高さ 852 の窓に置くと、器と中身を同じ矩形にはできません。クリップするのか、内側の辺を一致させて反対側へはみ出させるのかが未定義です。既存 Scenario は器以下のサイズしか扱っておらず、この矛盾を検出できません。  
   **推奨修正:** 「器いっぱい」を寸法の一致と辺の一致に分け、器を超える中身の描画・クリップ・geometry の期待値を明記してください。上下または左右の端で、器より大きい固定 View の Scenario を追加してください。

`Side Effects: なし` は今回の表示・配置変更の範囲では妥当です。実装タスクに decisions・handbook・concepts の書き換えもありません。静的レビューのみ実施し、ファイル変更・ビルド・テストは行っていません。


## 突き合わせ結果

ホスト側の自己レビュー (ksn-propose Step 8 の 2 周) では、どれも検出していなかった。

- 採用 (1): 比率サイズを伴う start / end が対象から漏れていた。レイアウトの解 (`ios/Sources/KsDialogs/Layout/DialogLayoutResolver.swift` の `resolveAxis`) では、比率サイズと start / end を併用できる。ios/ADR-0002 も安全領域を届ける (`DialogLayoutApplier.relaysSafeArea` は位置だけで判定)。そのため、同じずれが起きる。用語「片側の端の軸」を「比率サイズの有無を問わない」と明記し、tasks 1.1 を直し、[PB-IS-32] を足した
- 採用 (2): はみ出しの判定を保つ条件に、安全領域の変化が無かった。条件に重なり・そろえる軸と辺を足し (tasks 1.3)、Requirement の箇条に安全領域の変化を足し、[PB-IS-33] を足した
- 採用 (3): 上限の拡大を「覚えた幅との比較」で判定すると、733.33 / 734 の差で同じ上限でも求め直しを繰り返す。判定を「前に知らされた上限との比較」にし (tasks 2.1)、Requirement に「上限が変わらない置き直しでは求め直さない」を足し、[PB-IS-34] を足した
- 採用 (4): 器 (有効領域で頭打ち) より大きい縮まない中身の並び方が未定義だった。オーナーに提示し、Q (内側の辺にそろえ、付いた側へはみ出させる) に決まった (2026-10-21)。Requirement の箇条を直し、[PB-IS-35] を足した
