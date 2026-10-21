# セカンドオピニオン: fix-ios-swiftui-edge-content-placement (code-001)
**相方**: codex / **label**: so-code-fix-ios-swiftui-edge-content-placement-001 / **日付**: 2026-10-21 / **対象**: HEAD (29a94a6) に対する作業ツリーの差分 (ios/Sources/KsDialogs/・ios/Tests/KsDialogsTests/・change 配下の evidence/)
---
# レビュー結果: fix-ios-swiftui-edge-content-placement (001 回目)

**日付**: 2026-10-21  
**判定**: CHANGES_REQUESTED  
**保存先**: `kasane/changes/fix-ios-swiftui-edge-content-placement/review-001.md`

## サマリー

端の物理方向への変換、常設の frame による中身の同一性の保持、幅の上限拡大への追従は、仕様と整合しています。一方、理想サイズが変わらない内容更新では、はみ出し判定が更新されない欠陥があります。

静的レビューのみを実施しました。ビルド・テスト結果は提示されたホスト側の結果と `evidence/implementation-verification.md` に基づき、再実行していません。

## 照合した規約

- `kasane/handbook/cross/comment-policy.md` — 本体・テストのコメント。
- `kasane/handbook/cross/test-execution.md` — 実行範囲・件数と Scenario ID 網羅の報告。
- `kasane/handbook/cross/runtime-behavior-verification.md` — 修正前後の検出力と描画位置の証跡。
- `kasane/handbook/cross/ci-flaky-test-policy.md` — 状態変化の観測、MainActor 上での読み取り、テストの無効化。
- `kasane/handbook/cross/aiforms-origin-reference.md` — 挙動差調査への適用範囲。
- accepted の ios/ADR-0002、cross/ADR-0002。ios/ADR-0003 は proposed として扱いました。
- `kasane/lessons/code-review.md` の検出力・実行範囲の確認規律。

ロードしたスキル: `ksn-review`、`swift-ui-impl-skill`。

## 確認した観点

- デルタスペック、探索の決定事項、tasks、deviation と実装の対応。
- 足場アーティファクトの凍結と変更範囲。
- start / end と fill・中央の区別、比率サイズ、左右の書字方向。
- 安全領域の受け渡し、器の矩形、描画位置と geometry の一致。
- はみ出し判定の保持・無効化条件と表示中の内容更新。
- 幅の記憶の解除、回転後の拡大・縮小、同寸法での安定性。
- SwiftUI の構造的同一性、共有状態の MainActor 隔離、参照の所有関係。
- UIView の目印による描画位置の検査と、修正機構を無効化した際の検出力。

実行範囲の照合結果:

| ビルドルート | 提示された結果・対象範囲 |
|---|---|
| ios/ | Simulator 419 tests / 69 suites passed |
| android/ | 今回の指定検証範囲外 |
| android/ instrumented | 今回の指定検証範囲外 |
| kmp/ | iOS 86 tests / 0 failures、Android host 83 tests / 0 failures |
| maui/ | 今回の指定検証範囲外 |
| maui/android/native/ | 今回の指定検証範囲外 |
| maui/macios/native/ | 今回の指定検証範囲外 |

Scenario ID 網羅はホスト側で PB-IS 35/35、未網羅なしと報告されています。

## 指摘事項

### [🟠 Major] 理想サイズが同じまま縮められる範囲が変わると、はみ出しを再判定しない

**該当箇所**: `ios/Sources/KsDialogs/SwiftUI/DialogSwiftUIContentView.swift:193`

**問題点**:  
`OverflowInputs` は器の寸法・理想サイズ・安全領域・付いた辺だけを比較しています。しかし、これらが同じでも、中身の最小サイズが変われば、安全領域の内側に収まるかどうかは変わります。

既存のテスト用モデルで、次の状態変化を作れます。

```swift
// 変更前: 理想の高さ100、提案された高さ66まで縮む。
let model = SwiftUISafeAreaProbeModel(idealHeight: 100)

// 表示後の変更: 理想の高さ100のまま、最小の高さを80にする。
model.minHeight = 80
```

窓全体基準の下端配置では、器の高さ100・下の安全領域34なので、変更前の中身は高さ66に収まります。変更後は最低80を必要とし、内側の辺にそろえる処理が必要です。

ところが、無制約で測る理想サイズは変更前後とも200×100です。器の寸法・安全領域・配置も変わらないため、193行目で処理が終了し、`overflowAlignment.vertical` は nil のままになります。`contentPreferredSizeDidChange()` が呼ばれても、同じ理想サイズを保存するだけで、この判定を無効化しません。

これは静的な制御フローからの指摘で、この条件の実行確認はしていません。既存の PB-IS-23 は最初から最小高さ80、PB-IS-25 は理想高さの変更、PB-IS-33 は安全領域の変更を伴うため、この経路を覆っていません。

**推奨修正**:  
理想サイズが同じでも、制約下で必要とするサイズが変わった内容更新を検出して、はみ出し判定を無効化してください。包み自身の変更による再計測とは区別し、判定の往復を防ぐ必要があります。

上記の「最小高さ nil → 80」を、理想高さ100・器の矩形・安全領域を固定したまま行う回帰テストを追加してください。下端配置では、変更後の描画位置が y752・高さ80に落ち着くことを UIView の目印で確認します。この再判定が欠ける実装では、内側の辺へのそろえが有効にならず、テストが失敗する条件になります。

## アクションプラン

1. 理想サイズ以外の内容変化でも、必要なはみ出し再判定が行われるようにする。
2. 理想サイズを維持した最小高さの変更を回帰テストで固定する。
3. 修正後、指定された ios/・kmp/ の全件と Scenario ID 網羅を再確認する。

**指摘件数**: Critical 0 / Major 1 / Minor 0 / Suggestion 0



## 突き合わせ結果

ホスト側: review-001.md (NEEDS_DISCUSSION、Critical 0 / Major 0 / Minor 2 / Suggestion 2)、verify-001.md (VALID)。

- 確定 (1): 理想の大きさが同じまま縮められる範囲だけが変わると (最小の高さを表示中に足すなど)、はみ出しを判定し直さない。双方一致。重要度は相方の Major を採る (高い方)。ホスト側は作業ツリーの写しで再現済み (理想 200 × 100 に表示中に最小の高さ 100 を足すと、下端で中身の下端 y 735・上端で y 29.7。直したかったずれと同じ並び方)。ホスト側は「切り替わりの箇条と tasks 1.3 が挙げる判定の入力に入っていない」ため spec の扱いの問題とした → オーナーに諮る (NEEDS_DISCUSSION)
- 採用・降格・未解決: なし (相方の指摘は上の 1 件のみ)
