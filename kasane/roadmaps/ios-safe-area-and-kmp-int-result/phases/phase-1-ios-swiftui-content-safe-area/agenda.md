# iOS の器の中の SwiftUI に安全領域を届ける

iOS の器に載せた SwiftUI の中身 (Dialog・Loading・Toast) に、器と安全領域の重なりを SwiftUI 標準の安全領域として届ける (ios/ADR-0002)。

## 論点
- サイズ計算の実測: 端に付く配置 (fill・start / end) × 窓全体基準 × Dialog・Loading・Toast で、中身の要求サイズと最終の大きさが安定するか (膨らみすぎ・行ったり来たりが無いか)。崩れたら B 案 (安全領域は 0 のまま、重なりを独自の値で渡す) に倒し、ios/ADR-0002 を書き直す
- 届ける領域の範囲: キーボードの領域も含めるか、システムの領域 (ステータスバー・ホームインジケータ) だけに限るか
- UIView の中身に、器と安全領域の重なりが実際に届いているかの実測 (core/ADR-0011 との一致の確認)
- テストの測り方: テスト用の窓 (`ios/Tests/KsDialogsTests/Support/DialogLayoutTestWindow.swift`) は安全領域を getter の上書きで擬似的に作っている。これがホストの中まで届くか。届かなければ、ノッチのある機種の Simulator の実際の窓で測る
- 共通ケース表 (`core/layout-spec/cases.json`) に SwiftUI の中身のケースを足すか、iOS 固有のテストに留めるか
- Android の器の中の Compose の中身から、システムバーの insets がどう見えるかの調査 (変更はしない。差があれば起票に留める)

## 決定事項
- 方向は ios/ADR-0002 (proposed): 器と安全領域の重なりを SwiftUI 標準の安全領域として届ける。UIView の中身の扱いと、器の配置・サイズの規則は変えない
- 既定の基準領域 (可視領域) では重なりが 0 で、見た目は変わらない。変わるのは、窓全体基準で端に付けた中身だけ
- Sample へのデモの追加はしない (ロードマップの非ゴール)。自動テストと Simulator での実測で確かめる
- 蒸留時に反映: `kasane/concepts/ios/api/` (レイアウトの公開面) — 器の中の SwiftUI の中身から見える安全領域の意味
- 蒸留時に反映: ios/ADR-0002 の出典の行を、このフェーズの change に直す

## TODO
- [ ] 論点の解消
- [ ] ksn-propose で変更提案を起こす
