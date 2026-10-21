# セカンドオピニオン: fix-ios-swiftui-edge-content-placement (code-002)
**相方**: codex / **label**: so-code-fix-ios-swiftui-edge-content-placement-002 / **日付**: 2026-10-21 / **対象**: HEAD (29a94a6) に対する作業ツリーの差分 (ios/Sources/KsDialogs/・ios/Tests/KsDialogsTests/・change 配下の evidence/)
---
# レビュー結果: fix-ios-swiftui-edge-content-placement (002 回目)

**日付**: 2026-10-21  
**判定**: CHANGES_REQUESTED  
**件数**: Critical 0 / Major 1 / Minor 1 / Suggestion 0  
**保存先**: `kasane/changes/fix-ios-swiftui-edge-content-placement/review-002.md`（本応答を全文として返却。ファイル未作成）

## サマリー

前回の Major 指摘にあった、理想サイズを変えずに最小高さを増やして新たにはみ出すケースは、サイズ変更通知と回帰テストによって解消されています。ただし、逆に最小サイズを緩めて収まる状態へ戻した場合、既存のそろえが解除されない問題が残っています。

## 照合した規約

- `kasane/handbook/cross/comment-policy.md`：Swift 本体・テストのコメント。
- `kasane/handbook/cross/test-execution.md`：実行ルートと件数報告。
- `kasane/handbook/cross/runtime-behavior-verification.md`：表示位置の検証と証跡。
- `kasane/handbook/cross/ci-flaky-test-policy.md`：状態変化の待ち合わせ・継続観測・MainActor 上の読み取り。
- `kasane/handbook/cross/aiforms-origin-reference.md`：移植元との挙動差の位置付け。
- ksn-core の `change-scope.md`・`delta-spec.md`・`paths.md`・`evidence.md`。
- ios/ADR-0002。ios/ADR-0003 は proposed として扱い、決定済み規約には数えていない。
- `kasane/lessons/code-review.md` の重点観点。

ロードしたスキル: `ksn-review`、`swift-ui-impl-skill`。

## 確認した観点

- proposal・デルタスペック・探索の決定事項・deviation の整合。
- 端の配置と fill・中央・比率サイズの区別、物理方向と RTL。
- 安全領域の受け渡し、器の矩形、描画と geometry の一致。
- はみ出し判定の保持・無効化条件と、表示中の状態変化。
- 幅の上限通知、拡大時の記憶破棄、同一上限での安定性。
- SwiftUI の構造的同一性、Observation、MainActor、弱参照。
- テストの観測対象と検出力、チェック済みタスクとの対応。
- 合意済み deviation は違反として扱っていない。

ビルド・テストは指定どおり再実行していない。ホスト提供の結果と `evidence/implementation-verification.md` を照合した。

| ルート | 報告結果 |
|---|---|
| ios/ | 420 tests / 69 suites passed |
| android/ | 対象外と記録 |
| android/ instrumented | 対象外と記録 |
| kmp/ | iOS 86 / Android host 83、各 0 failures |
| maui/ | 206 passed / 0 failed |
| maui/android/native/ | 対象外と記録 |
| maui/macios/native/ | 17 tests / 7 suites passed |

Scenario ID は PB-IS 35/35、未網羅なし。追加の最小高さ変更テストには、通知経路を無効化すると失敗する確認が記録されている。撮影画像では、帯の地色が窓の下端まで続いていることを確認した。

## 指摘事項

### [🟠 Major] 最小サイズを緩めて収まる状態へ戻しても、そろえと安全領域の無視が残る

**該当箇所**: `ios/Sources/KsDialogs/SwiftUI/DialogSwiftUIContentView.swift:278`  
**関連箇所**: `ios/Sources/KsDialogs/SwiftUI/DialogSwiftUIContentView.swift:231`、`ios/Sources/KsDialogs/SwiftUI/DialogSwiftUIHost.swift:94`

**問題点**:

`hostedContentDidResize()` は、まだそろえを効かせていない軸がある場合だけ再判定を要求する。再判定側も未適用の軸しか調べないため、一度適用したそろえは、理想サイズ・器・安全領域・付いた辺が変わらない限り解除されない。

影響が明確になる確認ケースは、既存の `SafeAreaProbeSwiftUIContent` で作れる。

1. 縦長の窓、窓全体基準、垂直 `.end`。
2. `idealHeight: 1000, minHeight: 1000` の中身を表示する。器は高さ 852 で頭打ちになり、そろえが適用される。
3. 表示中に `minHeight` だけを `nil` に戻す。

理想高さは 1000、器の高さは 852 のままなので `OverflowInputs` は変わらない。垂直軸にはすでにそろえがあり、サイズ変更通知も再判定を要求しない。その結果、下端配置の包みに付けた `.ignoresSafeArea(.top)` が残る。

収まる中身は上 59・下 34 を避けた `y: 59, height: 759` に戻るべきだが、残った包みは上の安全領域まで使用する。これは「収まる中身の並び方は今のまま」という契約を満たさない。位置の数値はコードからの予測であり、本レビューでは実行確認していない。

追加された回帰テストは最小高さを増やす方向だけを検証しており、この逆方向を検出しない。

**推奨修正**:

中身自身のサイズ変更では、適用済みの軸も判定を無効化し、包みを効かせない状態で測り直せるようにする。包み自身による変更通知の抑止と、同一パス内の反映は維持する。

上記の最小高さ解除を回帰テストに追加し、描画矩形・geometry・受け取る安全領域が、最初から縮められる状態で表示した場合と一致することを確認する。

### [🟡 Minor] 証跡本文に Simulator UDID の実値が残っている

**該当箇所**: `evidence/implementation-verification.md:5`

**問題点**:

Simulator UDID が実値で記載されている。ksn-core の `references/evidence.md` は、本文へ手書きする値も含め、UUID を `<uuid>` に置換するよう定めている。Simulator を削除済みであることによる例外はない。

**推奨修正**:

UDID を `<uuid>` に置換する。検証条件の説明には機種・OS・修正サイクルの記録で足りる。

## アクションプラン

1. 最小サイズを緩めた場合にも、適用済みのそろえを再判定できるよう修正する。
2. 器が窓いっぱいになるケースで、最小高さの追加・解除を往復する回帰テストを追加する。再判定経路を無効化したときに解除側の検査が失敗することも確認する。
3. 証跡の UDID を置換する。
4. 修正後、影響するルートの全件テストと Scenario ID 網羅検査を再実行する。



## 突き合わせ結果

ホスト側: review-002.md (CHANGES_REQUESTED、Minor 2)、verify-002.md (VALID)。前回の確定指摘 (縮められる範囲だけが変わって収まらなくなるケース) は双方とも解消を確認。

- 確定 (1): 縮められる範囲だけが変わって収まる状態へ戻ったとき (表示中に最小の高さを外す)、効いたそろえが外れない。双方一致。重要度は相方の Major を採る。ホスト側は作業ツリーの写しで再現済み (理想・最小とも高さ 1000 から最小を外すと、下端に付けた中身が y 0・高さ 818、上端で y 59・高さ 793。最初から縮められる中身なら y 59・高さ 759)。Requirement の「収まる中身の並び方は…のまま (安全領域の内側に置かれる)」に反する。ホスト側はオーナー判断を求めたが、前サイクルのオーナー決定 (deviation.md 2 行目、本文の約束は無条件) の逆向きに当たるため、同じ決定の範囲で直す (deviation.md 3 行目)
- 確定 (2): 証跡 `evidence/implementation-verification.md:5` に Simulator の UDID の実値が残る。双方一致 (Minor)。ホスト側は identity lint で commit が止まることも指摘
- 採用・降格・未解決: なし
