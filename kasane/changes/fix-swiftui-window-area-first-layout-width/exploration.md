# Exploration: fix-swiftui-window-area-first-layout-width

## 課題 / 動機

ロードマップ `ios-safe-area-and-kmp-int-result` の phase-1 (change `deliver-swiftui-content-safe-area-ios`) の実装中に見つかった (2026-10-20、実装ワーカーの実測)。

- iOS で、基準領域を窓全体 (`layoutArea: .window`) にした SwiftUI の中身は、添付値 (`ksDialogOptions`) が届く前の最初のレイアウトを既定の可視領域で行う
- 折り返す本文のように幅で高さが決まる中身は、その最初のレイアウトで可視領域の幅 (横長の窓で 734) を上限に大きさが決まり、窓全体の幅 (852) まで広がらないまま止まる
- 変更前のコードでも同じ (器の幅 733.33)。phase-1 の change が生んだものではない。phase-1 では Scenario [PB-IS-15] の期待値をこの値に合わせ、deviation に記録した (`kasane/changes/deliver-swiftui-content-safe-area-ios/deviation.md`)
- 同じ根と見られる症状がもう 1 つある。理想の幅が窓より広い中身 (理想の幅 600) を可視領域で縦長に出す。そのあと横長に変えても、器は 393 のまま広がらない。中央に置いた長い本文も、横長にして 389 のまま。どれも変更前のコード (62b4981) で同じで、phase-1 の修正サイクル 2 の実測で確かめた

## 検討した選択肢 (却下案と理由を含む)

## 決定事項

## ADR 候補 (作成済み: ADR-NNNN / 未起票: ...)

## 未決の論点
- 未探索 (簡易起票)
- 添付値が届いた後に、内容サイズに委ねた軸の大きさを有効領域の新しい上限で測り直すべきか (UIView の中身との差の有無も確かめる)

## UI 素材 (ui/references/ の一覧と注釈)

## 変更級の推奨: 未判定
