# iOS の器の中の SwiftUI に安全領域を届ける

iOS の器に載せた SwiftUI の中身 (Dialog・Loading・Toast) に、器と安全領域の重なりを SwiftUI 標準の安全領域として届ける (ios/ADR-0002)。

## 論点
(なし — すべて決定事項へ移した)

## 決定事項
- 方向は ios/ADR-0002 (proposed): 器と安全領域の重なりを SwiftUI 標準の安全領域として届ける。UIView の中身の扱いと、器の配置・サイズの規則は変えない
- 既定の基準領域 (可視領域) では重なりが 0 で、見た目は変わらない。変わるのは、窓全体基準で端に付けた中身だけ
- Sample へのデモの追加はしない (ロードマップの非ゴール)。自動テストと Simulator での実測で確かめる
- 蒸留時に反映: `kasane/concepts/ios/api/` (レイアウトの公開面) — 器の中の SwiftUI の中身から見える安全領域の意味
- 蒸留時に反映: ios/ADR-0002 の出典の行を、このフェーズの change に直す
### 器の大きさの決め方 (サイズ計算の実測の結論)

自律決定 (ksn-autopilot、根拠: 1・委任状の見た目の基準と、停止報告への答え (自律判断ログ [決定]、統合ブランチ 7bc76bf))。ホストに安全領域をそのまま渡すと、要求サイズに重なりが足されて上端・下端の器が大きくなり、中央の重なる中身は窓まで膨らんだ。そこで次の形にする。実測は `kasane/changes/deliver-swiftui-content-safe-area-ios/evidence/safe-area-sizing-probe.md`。

- 器へ伝える要求サイズは、安全領域が 0 のときの大きさ (ホストが求めるサイズから、ホストが受け取っている安全領域を引いた値)。受け取った重なりは器の大きさに戻らない
- 基準領域が窓全体のときだけ、位置が端に固定される軸 (start / end と、比率サイズを伴わない fill) が器と安全領域の重なりを受け取る。中央に固定される軸と、窓全体以外の基準領域 (移動量でシステムの領域に重なる場合を含む) では今と同じく 0 (提案の相方レビューの指摘を受けて範囲を確定)
- 結果: 器の大きさと位置はどの配置でも今と同じ。窓全体基準で端に付けた中身だけが、器の内側で安全領域を避けて寄り、使える大きさが重なりの分だけ減る (UIView の中身と同じ)。無視する指定の中身は今と同じ
- ADR 候補: ios/ADR-0002 の範囲を「端の軸だけに渡し、要求サイズには重なりを入れない」に書き直す (proposed のため蒸留時に反映)

### そのほかの決定

- 自律決定 (ksn-autopilot、根拠: 1・委任状の見た目の基準、2・core/ADR-0011): 届けるのはシステムの領域 (ステータスバー・ホームインジケータ等) だけで、キーボードの領域は含めない。含めると既定の可視領域でもキーボードを避けて中身が動き、UIView の中身とも揃わない
- 自律決定 (ksn-autopilot、根拠: 2・core/ADR-0011): UIView の中身には器と安全領域の重なりがすでに届いていることを実測で確かめた (上端 59・下端 34・中央 0)。UIView の中身の扱いは変えず、SwiftUI の中身をこれに揃える
- 自律決定 (ksn-autopilot、根拠: 4): テストはテスト用の窓 (安全領域の上書き) で測る。上書きした値がホストの View まで届くことを実測で確かめたため、実際の機種の安全領域には頼らない
- 自律決定 (ksn-autopilot、根拠: 1・委任状の禁止事項、4): 共通ケース表 (`core/layout-spec/cases.json`) には足さず、iOS 固有のテストに留める。表に足すと android/・maui/ の読み手にも手が入る
- 自律決定 (ksn-autopilot、根拠: 1・ロードマップの非ゴール): Android の中身にはウィンドウ全体の insets が位置に関係なく届くとコードから読めた (iOS は重なり)。差は簡易起票 `align-android-content-insets-meaning` に留める

## TODO
- [x] 論点の解消
- [x] ksn-propose で変更提案を起こす (change: deliver-swiftui-content-safe-area-ios)

## 実装結果 (2026-10-20 反映)

change `deliver-swiftui-content-safe-area-ios` (`kasane/changes/archive/2026-10-20-deliver-swiftui-content-safe-area-ios/`) で、決定事項「器の大きさの決め方」の形のまま実装した。ios/ADR-0002 は範囲と Consequences をこの形に書き直して accepted にし、`kasane/concepts/ios/api/layout-surface.md` に節「SwiftUI の中身から見える安全領域」を足した。仕様との違いは [PB-IS-15] の THEN の幅 (852 ではなく、安全領域を受け取らないときと同じ値) の 1 件で、change の deviation.md に記録してある。

申し送り:

| 事柄 | 受け皿 |
|---|---|
| Android の中身には位置に関係なく window 全体の insets が届き、iOS と意味が違う | 簡易起票 `align-android-content-insets-meaning` (ロードマップの非ゴール。独立した change で扱う) |
| 窓全体基準の SwiftUI の中身は最初の配置を可視領域で測り、幅が窓まで広がらない (変更前からの挙動) | 簡易起票 `fix-swiftui-window-area-first-layout-width` (独立した change で扱う) |
| review-003 の任意の Suggestion (窓全体・上端の長い本文で窓を変える回帰テスト) | 見送り (自律決定、根拠: 4。今のコードは変更前と同じ値で、期待値が上の起票の修正で変わる見込みがある) |
| `kasane/concepts/kmp/api/ios-host-integration.md` の「UIView と SwiftUI のどちらでも観察できる挙動は同じ」は、中央の軸と窓全体以外の基準領域の安全領域では成り立たない | → phase-2-kmp-swift-int-result-guard の担当へ渡す (ksn-autopilot)。同じ文書を phase-2 の蒸留が書き直すため、この蒸留では触らない |
| 利用者向けの `skills/` と README の追従 | 見送り (ロードマップの非ゴール。蒸留の後に docs-refresh で追従する) |
