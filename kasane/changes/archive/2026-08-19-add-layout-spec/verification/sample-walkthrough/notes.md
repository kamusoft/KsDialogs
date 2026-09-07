# パリティ準拠 Sample の通し確認 (tasks 7.3)

4ルート (ios / android / kmp / maui) の Sample を実機・Simulator で実際に操作し、
デルタスペックの Scenario が実環境で成り立つことを確認した記録。

## 環境

| ルート | 実行環境 | アプリ | ビルド |
|---|---|---|---|
| ios | iOS Simulator iPhone 17 Pro (76EB1CA1-…、402x874 pt @3x) | `jp.kamusoft.ksdialogs.samples.ios` | `xcodebuild -project samples/ios/KsDialogsSample.xcodeproj -scheme KsDialogsSample` |
| kmp | 同上 | `jp.kamusoft.ksdialogs.samples.kmp.ios` | `xcodebuild -project samples/kmp/iosApp/KsDialogsSampleKmp.xcodeproj -scheme KsDialogsSampleKmp` |
| maui | 同上 | `jp.kamusoft.ksdialogs.samples.maui` | `dotnet build -f net10.0-ios -p:RuntimeIdentifier=iossimulator-arm64 -p:ValidateXcodeVersion=false` |
| android | Android 実機 Pixel 6a (2A141JEGR18112、1080x2400 px / density 420 = 2.625x、411.4x914.3 dp) | `jp.kamusoft.ksdialogs.samples.android` | `cd samples/android && ./gradlew installDebug` |

実施日 2026-08-19。すべて本変更適用後のツリーからビルドし直したものを配備して操作した。

## 確認1: パネル操作 → 配置反映

→ samples Requirement「レイアウトデモ項目 (属性調整パネル)」/ Scenario「属性を変えて表示すると反映される」「パネルの初期値は属性の既定値」

**パネル初期値** (`*-01`/`*-02`): 4ルートとも Horizontal = Center / Vertical = Center / OffsetX = 0 / OffsetY = 0 /
Use visible area = ON。契約の既定値と一致する。結果表示エリアは無い。

**Horizontal / Vertical の変更** (`*-03-dialog-end-end.png`): End / End にして Show すると、
4ルートとも右下寄せで表示された。

**Use visible area トグル** (`*-05-visiblearea-on.png` → `*-06-visiblearea-off.png`)。
Horizontal / Vertical = Start に固定し、トグルだけを切り替えて撮った 2 枚のカード上端を画素から実測:

| ルート | ON (可視領域基準) | OFF (window 基準) | 差 | 理論値 |
|---|---|---|---|---|
| ios | 87.0 pt | 25.0 pt | 62.0 pt | safe area 上端 62 pt |
| kmp | 87.0 pt | 25.0 pt | 62.0 pt | 同上 |
| maui | 86.0 pt | 24.0 pt | 62.0 pt | 同上 |
| android | 74.3 dp | 24.0 dp | 50.3 dp | ステータスバー 132 px ÷ 2.625 = 50.3 dp |

(iOS 系の絶対値は角丸の内側で測っているため真の上端 86 / 24 pt より 1 pt 大きい。差は角丸の影響を受けない)
OFF 側はどのルートも `dialogMargin` の 24 のみ、ON 側はそれにシステムバー分が上乗せされており、
基準領域の切り替えが実際に効いている。

**Offset 入力** (`*-07-offsety-*.png`): 数値欄へ入力して Show:

| ルート | 配置 | 入力 | カード上端 | 内訳 |
|---|---|---|---|---|
| ios | Center / Center | OffsetY 200 | 590.0 pt | 中央配置 + 200 |
| kmp | Start / Start | OffsetY 200 | 286.0 pt | 86 + 200 |
| maui | Start / Start | OffsetY 20 | 106.0 pt | 86 + 20 |
| android | Start / Start | OffsetY 200 | 274.3 dp | 74.3 + 200 |

いずれも入力値がそのまま移動量として乗っている。
(maui だけ 20 なのは、Entry のキャレットが末尾に付かず 200 になってしまうため。値が異なるだけで検証内容は同じ)

## MAUI iOS の再取得 (2026-08-19)

`maui-05-visiblearea-on.png` / `maui-06-visiblearea-off.png` / `maui-07-offsety-20.png` の 3 枚は、
サイクル3の修正 (互換面のクラスメソッドが静的リンクで消えて MAUI iOS が提示のたびに落ちていた不具合の解消と、
実効値のスナップショット境界の是正) を反映したビルドで撮り直した。手順・判定基準は上と同じ
(iPhone 17 Pro Simulator、`dotnet build -f net10.0-ios -p:RuntimeIdentifier=iossimulator-arm64 -p:ValidateXcodeVersion=false`)。

カード上端は 3 枚とも画素から実測し、修正前の記録と 1 px の差もなく一致した
(ON 258 px = 86.0 pt / OFF 72 px = 24.0 pt / OffsetY 20 は 318 px = 106.0 pt)。
差 62.0 pt は safe area 上端、106.0 pt は 86 + 20 で、いずれも理論値どおり。
上の表の maui 行の絶対値 (86.7 / 24.7) は角丸の内側で測った旧値だったので、
外形の上端で測り直した 86.0 / 24.0 に改めた (差 62.0 pt は変わらない)。

`maui-07` の OffsetY 欄の表示は、旧記録の `020` から `20` になった (入力操作の違いだけで、移動量は同じ 20)。

## 確認2: 結果経路と、初期非表示 → 結果表示の遷移

→ samples Requirement「結果表示エリアの表示条件」/ Scenario「初期状態では結果エリアが無い」「結果確定後に表示される」
→ deviation.md のパネル内結果表示 (合意済み差分)

- **初期状態**: `*-01-menu-initial.png` (メニュー) と `*-02-panel-initial.png` (パネル) に結果エリアは無い — 4ルート一致
- **パネル内の結果表示**: 外側タップの直後 `*-04-outside-tap-cancelled.png` に `直近の結果` / `結果: cancelled` が現れる (無 → 有の遷移) — 4ルート一致
- **メニューの結果表示**: パネルで `OK` を押して `結果: completed(true)` にしてから戻ると、
  メニューにも `直近の結果` / `結果: completed(true)` が出る (`*-08-menu-result-completed.png`) — 4ルート一致
- 遷移として撮るため、いずれも直前に別の結果 (cancelled) にしてから操作している

## 確認3: 外側タップキャンセルの既定挙動

→ dialog-contract / ios-native / android-native の Requirement「外側タップキャンセル」

既定設定 (isCanceledOnTouchOutside = true) のまま、カードの外側 (覆いの領域) をタップした:

| ルート | タップ点 | 結果 |
|---|---|---|
| ios | (100, 550) pt | 閉じて `結果: cancelled` |
| kmp | (100, 550) pt | 閉じて `結果: cancelled` |
| maui | (100, 550) pt | 閉じて `結果: cancelled` |
| android | (300, 1400) px | 閉じて `結果: cancelled` |

証跡は各ルートの `*-04-outside-tap-cancelled.png`。

## 確認4: MAUI iOS の描画中心タップ

→ maui-binding Requirement「当たり領域は描画領域と一致する (MAUI)」

`maui-hit-test/notes.md` の「新経路での再確認」節に記録した (証跡も同ディレクトリ)。

## 備考

- iOS Simulator の `UISwitch` は操作ツールの単発 tap では反応せず、短いドラッグ (touch_path) で切り替わった。
  実機の指操作を模した入力の差であり、アプリ側の不具合ではない (同じトグルは Android では tap で切り替わる)
- 観測のためのアプリ改変は行っていない (この節の確認はすべて Sample の公開操作だけで完結する)
