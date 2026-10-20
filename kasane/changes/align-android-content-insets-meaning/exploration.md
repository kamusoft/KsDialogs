# Exploration: align-android-content-insets-meaning

## 課題 / 動機

ロードマップ `ios-safe-area-and-kmp-int-result` の phase-1 (iOS の器の中の SwiftUI に安全領域を届ける) の論点「Android の器の中の Compose の中身から、システムバーの insets がどう見えるか」の調査で見つかった差 (2026-10-20、コードの読みによる。実機では未実測)。Android 側の変更はこのロードマップの非ゴールのため起票に留める。

- Android の器 (Dialog・Loading・Toast) は全画面の別ウィンドウで、edge-to-edge にしてある (`android/ksdialogs-core/src/main/kotlin/jp/kamusoft/ksdialogs/DialogWindowSystemBars.kt`)。器の View (`DialogLayoutHost.kt`・`DialogContentHolder.kt`) は insets を消費も加工もしないので、中身にはウィンドウ全体のシステムバーの insets がそのまま届くと読める
- その値は中身の位置に関係しない。画面中央に置いた中身にもステータスバーの高さがまるごと届く。View の中身と Compose の中身 (`DialogComposeContentView.kt`) で差は見当たらない
- iOS では、UIView の中身に「中身の矩形と安全領域の重なり」が届く (phase-1 の実測で確認、`kasane/changes/deliver-swiftui-content-safe-area-ios/evidence/safe-area-sizing-probe.md`)。中央に置いた中身では 0 になる
- そのため、中身から見えるシステムの領域の意味が両 OS で違う。共有コードの中身を書く KMP の利用者や、両 OS で同じ中身を書く利用者が、OS ごとに違う読み方を強いられる可能性がある
- 中身から insets を読むテストは Android に無い (器のウィンドウに insets が届くことは `DialogPresentedWindowTests` が確かめている)

## 検討した選択肢 (却下案と理由を含む)

## 決定事項

## ADR 候補 (作成済み: ADR-NNNN / 未起票: ...)

## 未決の論点
- 未探索 (簡易起票)
- 差をそのまま仕様として文書に書くか、Android の中身に届く insets を中身の矩形との重なりに揃えるか
- Compose の `WindowInsets` が Activity 側の edge-to-edge 設定に左右されないことの実機での確認

## UI 素材 (ui/references/ の一覧と注釈)

## 変更級の推奨: 未判定
