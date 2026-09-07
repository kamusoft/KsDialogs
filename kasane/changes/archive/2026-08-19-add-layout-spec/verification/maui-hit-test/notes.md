# MAUI iOS 当たり判定の実環境確認 (tasks 4.2)

対象要求: maui-binding「当たり領域は描画領域と一致する (MAUI)」/ Scenario「描画中心のタップが反応する」

## 環境

| 項目 | 値 |
|---|---|
| 実施日 | 2026-08-18 |
| 端末 | iOS Simulator iPhone 17 Pro (UDID <uuid>)、1206x2622 px / 402x874 pt (@3x) |
| アプリ | samples/maui (`jp.kamusoft.ksdialogs.samples.maui`)、Basic Dialog |
| ビルド | `dotnet build -f net10.0-ios -p:RuntimeIdentifier=iossimulator-arm64 -p:ValidateXcodeVersion=false` |

`ValidateXcodeVersion=false` は Xcode 26.5 と .NET for iOS 26.1 の要求バージョン差を通すためのビルド時フラグで、ソースには影響しない。

## 手順

1. 撮影: `xcrun simctl io <UDID> screenshot <path>` (scratchpad へ撮って証跡ディレクトリへ cp)
2. 描画領域の実測: スクリーンショットの画素から OK ボタンの塗り (#2563EB) とキャンセルボタンの塗り (#F3F4F6) の bbox を取り、3 で割って pt へ換算する
3. タップ: シミュレータ操作ツール (座標は pt、原点は左上)
4. 判定: タップ後のスクリーンショットに #2563EB の画素が残っていればダイアログは開いたまま (無反応)、消えていれば閉じた。結果表示の遷移も併せて撮る (直前に別の結果にしてから操作する)

## 実測 (現在のビルド = 本変更適用後)

描画領域 (pt): カード x 65.0–336.7 / y 391.0–510.7、OK ボタン x 206.0–316.7 / y 453.0–490.7 (中心 261.3, 471.8)、キャンセルボタン x 85.0–195.7 / y 453.0–490.7 (中心 140.3, 471.8)

| タップ点 (pt) | 位置づけ | 結果 |
|---|---|---|
| (261, 472) | OK の描画中心 | 反応する (`結果: completed(true)` へ遷移) |
| (261, 480) | phase-4 記録が無反応とした点 | 反応する |
| (261, 455) | OK の上端から 2pt 内側 | 反応する |
| (261, 488) | OK の下端から 2.7pt 内側 | 反応する |
| (315, 455) | OK の右上隅の内側 | 反応する |
| (208, 489) | OK の左下隅の内側 | 反応する |
| (140, 472) | キャンセルの描画中心 | 反応する (`結果: cancelled` へ遷移) |
| (193, 489) | キャンセルの右下隅の内側 | 反応する (`結果: cancelled` へ遷移) |
| (261, 500) | カード内・ボタンの描画領域の外 (下の余白) | 反応しない (陰性対照。判定手段が空振りしていないことの確認) |

証跡: `current-01-prior-result-cancelled.png` (直前の結果を cancelled にした状態) → `current-02-dialog-shown.png` (表示) → `current-03-tap-drawn-center-completed.png` (描画中心タップ後、completed(true) へ遷移) / `current-04-tap-corner-cancelled.png` (キャンセルの隅タップで cancelled へ遷移)

## 原因

phase-4 の記録 (描画中心 (261,480) で無反応、上端寄り (280,470) / (140,470) で反応) を、本変更適用前のツリー (git archive HEAD) を同じ手順でビルド・実行して再現し、原因を特定した。

本変更適用前の描画領域 (pt): カード y 377.0–496.7 (ウィンドウ中央 437 を中心とする)、OK ボタン y 439.0–476.7 (中心 261.3, **457.8**)

| タップ点 (pt) | 結果 |
|---|---|
| (261, 480) | 反応しない (`baseline-03-tap-y480-no-reaction.png`) |
| (261, 458) = 実測した描画中心 | 反応する (`baseline-04-tap-drawn-center-completed.png`) |

適用前のビルドでも**描画領域の中心をタップすれば反応する**。無反応だった (261,480) は OK ボタンの描画下端 476.7 より 3.3pt 下、すなわちカードの下余白であり、描画領域の外だった。つまり phase-4 の事象は当たり判定の不具合ではなく、**描画中心の座標の取り違え**である (実際の中心は y=458 であり、480 ではない)。「上端寄りで反応した」のは、その点が描画領域の内側だったため。

本変更適用後は基準領域が可視領域になったためカードが約 14pt 下がり (OK ボタンの描画中心 y が 457.8 → 471.8)、記録の (261,480) も描画領域の内側に入って反応するようになっている。

## 新経路での再確認 (tasks 7.3)

供給機構が VM 契約から添付プロパティ + Show 引数へ置き換わった (core/ADR-0015) ため、
上の実測を新しいツリー・新しい経路で撮り直した (2026-08-19、同じ iPhone 17 Pro)。

まず OK ボタンの描画領域を画素から測り直したところ **pt bbox x 206.0–316.7 / y 450.0–493.7、中心 (261.3, 471.8)** で、
改修前の実測 (261.3, 471.8) と一致した。その描画中心をタップして反応を確認した:

| 段階 | 操作 | 画面 | 証跡 |
|---|---|---|---|
| 1 | Basic Dialog を開いてキャンセルの描画中心 (140, 472) をタップ | `結果: cancelled` | `current-07-prior-result-cancelled.png` |
| 2 | Basic Dialog を再表示 | ダイアログ表示 | `current-08-dialog-shown.png` |
| 3 | OK の描画中心 (261, 472) をタップ | `結果: completed(true)` へ遷移 | `current-09-tap-drawn-center-completed.png` |

Layout Dialog 側でも同様に、画素から測った OK の描画中心 (196, 186) のタップで `completed(true)` へ遷移した
(証跡は `../sample-walkthrough/maui-08-menu-result-completed.png` 前後)。改修による退行はない。

## 併せて実施: 属性の輸送の実環境確認 (tasks 4.1 の裏取り — 新経路で撮り直し)

自動テストが覆うのは MAUI facade から委譲面までと、Android 互換面から ViewModel までの2区間で、
C# ↔ Native の境界 (iOS = ObjC binding / Android = Java binding) を越える部分は実環境でしか観測できない。
そこで **samples/maui の `BasicDialogCardView.xaml` の根要素へ一時的に添付プロパティを書いて** (新しい供給経路)
両 OS で観測し、観測後に撤去した (撤去は `samples/` 配下 138 ファイルの SHA-256 一致で確認済み。
原本ビルドを両端末へ再配備済み)。

指定値 (XAML の `ksd:Dialog.*`): ProportionalWidth 0.5 / HorizontalAlignment Start / VerticalAlignment Start /
OffsetY 20 / OverlayColor `#8000FF00` (緑 50%)。
※ 改訂で廃止された BorderWidth / BorderColor は指定していない (公開面に存在しない)。

| 観測 | iOS Simulator iPhone 17 Pro (402pt 幅 / safe area 上端 62) | Android 実機 Pixel 6a (411.4dp 幅 / ステータスバー 50.3dp) |
|---|---|---|
| 中身の上端 | 106.0 pt (= 62 + dialogMargin 24 + offsetY 20) | 94.5 dp (= 50.3 + 24 + 20) |
| 外形の幅 | 201 pt (= 0.5 × 402)。中身 (272 要求) が外形をはみ出し左端が画面外へクリップされ、右端 260.0 pt = 外形中心 124.5 ± 136 と一致 | 205.3 dp (実測 24.0–229.3 dp、= 0.5 × 411.4 = 205.7) |
| 外形の左端 | (中身のはみ出しで直接は見えない。上の右端から逆算して 24) | 24.0 dp (= dialogMargin) |
| 覆い | 白地の上で (127,255,127) = 緑 50% (sRGB そのもの) | 緑の半透明 (背後のメニュー行が透ける)。生値 (186,253,165) |

証跡: `current-05-attribute-transport-ios.png` / `current-06-attribute-transport-android.png`

比率・配置・Offset・Margin・覆い (ARGB) が値を保ったまま Native 実装まで届いていることを確認した。
中身がクランプ後の外形からはみ出して描かれる点 (iOS) は、契約が未規定としている「クランプ時の中身の見え方」にあたる。

**覆いの色の数値について**: Pixel 6a の `screencap` は Display P3 (sRGB transfer) の ICC プロファイル付きで出力されるため、
生の 8bit 値は sRGB と直接比較できない。同じ画面の `#2563EB` は (54,98,227) として出ており、
これは sRGB → 当該プロファイル変換の計算値と完全一致する (=変換モデルは正しい)。
一方、覆いの (186,253,165) は sRGB 合成値 `#7FFF7F` の変換値 (160,252,142) とは一致しない
— 合成が行われる色空間の違いによるものと見られる。**Android 側は「緑の半透明が出ている」という定性的確認に留め、
数値一致の主張はしない** (iOS 側は sRGB で厳密一致しており、輸送そのものは押さえられている)。

## 結論

- Scenario「描画中心のタップが反応する」は満たしている。描画領域は中心・四隅の内側 (端から 2〜3pt) まで反応し、描画領域の外側で試した点 (下余白の (261,500)) は反応しない
- 当たり判定を直すためのコード変更は行っていない (直すべき不具合が存在しなかったため)。phase-4 記録の「当たり領域が描画より狭い / 上へずれている疑い」は、上の再現で否定された
