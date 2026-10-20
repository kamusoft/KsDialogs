# セカンドオピニオン: deliver-swiftui-content-safe-area-ios (spec-001)
**相方**: codex / **label**: so-spec-deliver-swiftui-content-safe-area-ios-001 / **日付**: 2026-10-20 / **対象**: 提案一式 (proposal.md・specs/ios-native/spec.md・tasks.md)
---
実装前に修正すべき Major が 4 件、Minor が 1 件あります。

1. **Major — `kasane/changes/deliver-swiftui-content-safe-area-ios/specs/ios-native/spec.md:66`**
   
   `ignoresSafeArea` を使う中身の位置・大きさを無条件に保証していますが、実測では下端の固定高さ 100 の中身が y 752 → 735 に移動しています（`evidence/safe-area-sizing-probe.md:64,75`）。Scenario PB-IS-10 は可変サイズだけなので、この既知の不一致を検出しません。proposal.md:30 の互換性の説明も同じ問題があります。
   
   **推奨修正:** 固定サイズの例外を契約と影響説明に明記し、再現 Scenario を追加してください。無条件の保証を維持するなら、実測した方式では未達であることを明示し、解決方法を確定する必要があります。

2. **Major — `kasane/changes/deliver-swiftui-content-safe-area-ios/specs/ios-native/spec.md:7`**
   
   `fill` を常に「端の軸」と定義していますが、既存コードでは比率サイズが指定された軸の `fill` は中央配置になります（`ios/Sources/KsDialogs/Layout/DialogLayoutResolver.swift:89`）。一方、tasks.md:6 は「レイアウトの解」から判定するため、この入力では spec が要求する受け渡しと実装方針が食い違います。
   
   **推奨修正:** 判定対象を指定された alignment と解決後の配置のどちらにするか決め、定義・タスクを統一してください。比率サイズ＋`fill` で安全領域に重なる Scenario も必要です。

3. **Major — `kasane/changes/deliver-swiftui-content-safe-area-ios/specs/ios-native/spec.md:100`**
   
   「可視領域なら常に安全領域 0」は既存の offset と両立しません。例えば上の安全領域 59、可視領域基準、上端配置、`offsetY = -30` なら中身は y 29 に移り、上側に 30 重なります。既存コードは有限の負 offset を受け付け、そのまま配置へ加算します（`DialogLayout.swift:54`、`DialogLayoutResolver.swift:94`）。この場合、Requirement 1 は 30、Requirement 3 は 0 を要求します。
   
   **推奨修正:** 既定の保証を「offset などによってシステム領域に重ならない場合」に限定し、可視領域基準でも重なった場合の値を Scenario で固定してください。

4. **Major — `kasane/changes/deliver-swiftui-content-safe-area-ios/specs/ios-native/spec.md:64`**
   
   「安全領域が 0 のときの要求サイズ」という比較条件が未定義です。実装根拠はホストの要求サイズから inset を引く方式ですが、実測対象は固定サイズ・理想サイズです。幅に応じて折り返す本文や `GeometryReader.safeAreaInsets` に応じて内容を変える View では、要求サイズの差が inset の単純加算になる保証がありません。PB-IS-12 の高さ 100 → 160 だけでは、この一般的な保証を検証できません。
   
   **推奨修正:** 比較時の提案幅・高さと測定対象を定義してください。少なくとも、横方向の安全領域で折り返す本文について、器の寸法と表示中の更新を確認する Scenario を追加し、単純減算方式が契約を満たすか確かめる必要があります。

5. **Minor — `kasane/changes/deliver-swiftui-content-safe-area-ios/specs/ios-native/spec.md:74`**
   
   PB-IS-09・10・14 は y 752／718 などを期待しますが、窓の高さ 852 が GIVEN にありません。また PB-IS-01・02 などは中身の高さを指定せず反対側の inset を 0 としています。背の高い中身なら反対側にも重なるため、記載した前提だけでは期待値が一意になりません。
   
   **推奨修正:** 共通の試験条件として窓の寸法、中身の要求サイズ、余白・offset・比率指定の有無を定義し、各 Scenario から参照してください。

`Side Effects: なし` は今回の画面表示・配置の変更について妥当です。Requires の必須漏れや、実装ワーカーに decisions／concepts 等を書き換えさせるタスクは見当たりません。ビルド・テストは実行していません。

## 突き合わせ結果

ホスト側の自己レビュー (ksn-propose Step 8 の 1 周目) はチェックリストを通過していた。相方の指摘はどれも該当箇所と実害の筋書きが具体的なので、5 件とも採用して提案に反映した (確定 0 / 採用 5 / 降格 0 / 未解決 0)。

- 採用 1 (Major、無視する固定サイズの中身が下端で動く): 無視する中身の保証を「大きさが可変のもの」に限った。固定サイズで安全領域に収まらない中身は、器の矩形だけを保証する ([PB-IS-12])。並び方は SwiftUI 標準のホストに従う、と Requirement と proposal の Impact に書いた
- 採用 2 (Major、比率サイズを伴う fill は解決後に中央になる): 端の軸 / 中央の軸の定義を、レイアウトの解の位置で決める形にした。比率サイズの fill の Scenario を足した ([PB-IS-09])
- 採用 3 (Major、可視領域でも負の移動量で重なる): 受け渡しを基準領域が窓全体のときに限った。窓全体以外では、移動量で重なっても 0 とした ([PB-IS-18])。委任状の見た目の基準 (窓全体基準で端に付けた中身だけが寄る) と一致する
- 採用 4 (Major、要求サイズの比較条件が未定義): 用語に「要求サイズ」を定義した。横の安全領域で折り返す本文の Scenario を足した ([PB-IS-15])
- 採用 5 (Minor、共通の試験条件が無い): 窓の寸法・安全領域・中身の既定の大きさ・余白と移動量の扱いを「共通の試験条件」にまとめた
