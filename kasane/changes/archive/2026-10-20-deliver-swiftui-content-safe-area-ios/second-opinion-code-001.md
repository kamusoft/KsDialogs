# セカンドオピニオン: deliver-swiftui-content-safe-area-ios (code-001)
**相方**: codex / **label**: so-code-deliver-swiftui-content-safe-area-ios-001 / **日付**: 2026-10-20 / **対象**: 62b4981 から作業ツリーまで (ios/Sources/KsDialogs/SwiftUI/・Layout/、ios/Tests/KsDialogsTests/)
---
# レビュー結果: deliver-swiftui-content-safe-area-ios (001 回目)

**日付**: 2026-10-20  
**判定**: CHANGES_REQUESTED  
**保存先**: `kasane/changes/deliver-swiftui-content-safe-area-ios/review-001.md`

## サマリー

安全領域を渡す軸の選択と、Dialog・Loading・Toast への共通適用は仕様に沿っています。一方、要求サイズの測定が現在の配置幅に依存するため、表示中の理想幅の変更に追従できない経路があります。指摘は Major 1 件、Minor 1 件です。

静的レビューのみ実施しました。ビルド・テストの成功はホスト提供の証跡に基づき、再実行していません。

## 照合した規約

- `kasane/handbook/cross/comment-policy.md`：変更・追加したソースとテストのコメント。
- `kasane/handbook/cross/test-execution.md`：対象ルートの全件実行報告と件数。
- `kasane/handbook/cross/ci-flaky-test-policy.md`：SwiftUI の更新を待つ観測、MainActor 上の状態参照、テストの無効化。
- `kasane/handbook/cross/runtime-behavior-verification.md`：Simulator の実際の安全領域を使った実測証跡。
- core/ADR-0007・0009・0010・0011：レイアウト規則、実 frame の検証、UIView／SwiftUI の対応とホスティング。

ロードしたスキル: `ksn-review`、`swift-ui-impl-skill`

ios/ADR-0002 は proposed として参照し、単独の違反判定根拠にはしていません。

## 確認した観点

- 窓全体基準の端の軸への伝播、中央・比率付き fill・可視領域での抑制。
- キーボード領域を除外するホスト設定。
- 器の矩形、固定サイズ、`ignoresSafeArea`、窓の寸法・安全領域の変更。
- 動的な要求サイズ変更と、その通知から再測定までの経路。
- UIView 経路への影響、公開 API の追加有無、参照所有と MainActor 隔離。
- Scenario ID とテスト本体の検証内容。
- 足場アーティファクトの凍結と tasks の完了報告。
- PB-IS-15 の deviation は合意済み差分として扱い、幅 852 を要求していません。

提供された実行証跡は次のとおりです。

| 対象 | 結果 |
|---|---|
| ios/ Simulator | Swift Testing 391 tests passed、XCTest 0 tests / 0 failures |
| kmp/ allTests | 169 tests / 0 failures / 0 errors / 0 skipped |
| Scenario ID 網羅 | PB-IS 19/19、未網羅なし |

## 指摘事項

### [🟠 Major] 現在の幅を測定に戻すため、可変コンテンツの理想幅変更に追従できない

**該当箇所**: `ios/Sources/KsDialogs/SwiftUI/DialogSwiftUIContentView.swift:60`

**問題点**: `bounds.width` が正になると、それ以降の `sizeThatFits` は常に現在の配置幅を提案します。提案幅を受け入れる可変 View では、内容が求める新しい理想幅を取得できません。

例えば、追加された `SafeAreaProbeSwiftUIContent` の可変サイズ経路は次の形です。

```swift
Color.clear.frame(
    idealWidth: model.idealWidth,
    idealHeight: model.idealHeight
)
```

幅 200 で表示した後に `idealWidth` を 280 または 120 に変更しても、測定には幅 200 が提案されます。この View は提案された幅を使うため、返される幅も 200 に留まります。`preferredContentSize` の変更通知で再測定しても同じ計算になります。

これは「中身の要求サイズが表示中に変わったら、器はその新しい大きさに追従する」という Requirement を満たしません。現在の PB-IS-14 は `idealHeight` だけを変更しており、この経路を検出できません。既定の可視領域でも起こる計算です。

**推奨修正**: 幅の理想値の取得と、利用可能な幅に応じた折り返し高さの測定を分け、現在の配置幅だけで要求幅が固定されないようにしてください。単純に両軸を無制約に戻すのではなく、PB-IS-15 の安定性も維持する必要があります。

回帰テストには、同じ表示中に `idealWidth` を `200 → 280 → 120` と変更し、器の幅と配置が追従するケースを追加してください。現実装では幅 200 に留まるため、問題の有無を区別できます。

### [🟡 Minor] PB-IS-15 が合意後の「安全領域なしと同じ矩形」を検証していない

**該当箇所**: `ios/Tests/KsDialogsTests/DialogSwiftUISafeAreaTests.swift:237`

**問題点**: このテストが検証する幅の条件は「852 以下」と「1 秒間一定」だけです。deviation で維持した「安全領域を受け取らない場合と同じ値」という条件を確認していません。

例えば幅が誤って 600 に縮んでも、一定の矩形で左 59・右 0 を受け取れば成功します。安全領域の補正で幅や折り返し高さが変わる退行を検出できません。

**推奨修正**: 合意済みの基準矩形との比較、または同条件で安全領域を渡さないホストとの矩形比較を追加してください。幅 852 への変更は不要です。比較があれば、安全領域の補正によって矩形が変わった場合に失敗します。

## アクションプラン

1. 要求幅を現在の配置幅から独立して更新できるようにし、理想幅の拡大・縮小を検証する。
2. PB-IS-15 に、安全領域を受け取らない場合との矩形一致の検証を追加する。
3. 修正後に ios/ 全件、kmp/ allTests、Scenario ID 網羅検査を再実行し、証跡を更新する。

## 突き合わせ結果

確定 2 / 採用 0 / 降格 0 / 未解決 0。

- 確定 (Major): 要求サイズの測定が今の配置幅を提案するため、表示中に中身の理想の幅が変わっても器が追従しない。ホスト側 review-001.md の Major と同じ指摘で、ホスト側は変更前のコードとの比較で、既定の可視領域でも起きる退行であることを実測している
- 確定 (Minor): [PB-IS-15] のテストが、deviation で合意した「安全領域を受け取らないときと同じ幅」を確かめていない。ホスト側 review-001.md の Minor と同じ
