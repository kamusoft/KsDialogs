# 表示中のウィンドウ変化への追随 (iOS) — 検証の証跡

tasks 1.4 の iOS 分 (PB-WN-01〜03) の確認記録 (2026-08-21)。
対象は dialog-contract デルタスペックの Requirement「表示中のウィンドウ寸法変化への追随」。

環境: iPhone 17 Simulator (iOS 26.5 / UDID `<uuid>`)、
Sample は `samples/ios` の KsDialogsSample (bundle id `jp.kamusoft.ksdialogs.samples.ios`)。

## 何を確認したか

### 1. Scenario テスト (実 UIKit のレイアウト機構で実行)

3本とも Simulator 上で実 `UIWindow` に器を載せ、実効値を固定したあとでウィンドウの寸法・
可視領域の余白を変えて、再配置後の中身の矩形を実測している。実装は
`ios/Tests/KsDialogsTests/DialogWindowChangeTests.swift`。

```
cd ios
xcodebuild test -scheme KsDialogs -destination 'platform=iOS Simulator,name=iPhone 17' \
  -only-testing:KsDialogsTests/DialogWindowChangeTests
```

結果: **3 tests / 1 suite / 0 failures** (`xcodebuild-window-change.log`)。

| Scenario | テスト名 | 変えたもの | 期待した矩形 (pt) |
|---|---|---|---|
| PB-WN-01 | `[PB-WN-01] 回転後も配置規則が新しい寸法で成立する` | 390x844 / insets 上59 下34 → 844x390 / insets 下21 左右59 | (97.5, 340.625, 195, 187.75) → (240.5, 138.375, 363, 92.25) |
| PB-WN-02 | `[PB-WN-02] ウィンドウ寸法のみの変化に追随する` | 幅 390 → 320 (余白は据え置き) | (80, 340.625, 160, 187.75) |
| PB-WN-03 | `[PB-WN-03] 可視領域インセットのみの変化に追随する` | 余白 上59 下34 → 全辺 0 (寸法は据え置き) | (97.5, 316.5, 195, 211) |

期待値は属性 (基準領域 visibleArea / 全辺 24 の余白 / 比率 幅 0.5・高さ 0.25 / 中央配置) と
新しい寸法・余白から、レイアウトのルールの手順で手計算したもの。許容差は共通ケース表と同じ 1.0 pt。

PB-WN-01 では、実効値の固定後に添付を別の値へ書き換えてから回転させ、**再配置が固定済みの
属性で行われる** (スナップショット凍結が解けない) ことも同じテストで確かめている。

### 2. 実 Sample での表示 (縦向き)

Sample の Layout Dialog で Horizontal = End / Vertical = End / Use visible area = ON にして
表示した状態が `02-portrait-dialog.png`。設定画面は `01-portrait-settings.png`。
ダイアログが可視領域の右下 (ステータスバー・ホームインジケータを避けた領域から 24 pt 内側) に
寄っており、位置がウィンドウ寸法と可視領域から導かれていることが見える。

## 3. 実 Sample を回転させた後の表示 (横向き)

2 の状態 (ダイアログ表示中) のまま Simulator を Device > Rotate Right で横向きにし、
`xcrun simctl io <uuid> screenshot` で撮影したのが
`03-landscape-dialog.png` (2026-08-21。回転操作はオーナーが Simulator.app で実施、撮影は simctl)。
simctl の出力はデバイス本来の画素の向き (1206x2622) で保存されるため、`03-landscape-dialog-raw.png` を
原本として残し、`03-landscape-dialog.png` は `sips` で回転して横向きに直した版。

観察: 回転後もダイアログは可視領域の右下に再配置されている。右端からの距離は約 88 pt
(Dynamic Island 側の安全領域 59 pt + 余白 24 pt)、下端からの距離は約 44 pt (ホームインジケータ側の
インセット 21 pt + 余白 24 pt) で、縦向き (右端から 24 pt・下端から 34 + 24 pt) とは異なる位置に
なっている — 位置が回転後のウィンドウ寸法と可視領域から導き直されていることが見える (PB-WN-01 の
実 Sample での裏取り)。ダイアログの中身 (文言・ボタン) と結果通知の経路は回転前後で変わっていない。

回転の手段について: `xcrun simctl` に回転サブコマンドは無く、シミュレータ制御ツールの操作も
tap / swipe / text / button のみのため、回転操作自体は Simulator.app 上で人手 (Command + ← / →) が要る。
Android の回転は `adb shell settings put system user_rotation <0-3>` で機械的に行える (Android 側の証跡を参照)。

## ファイル

| ファイル | 内容 |
|---|---|
| `01-portrait-settings.png` | Layout Dialog の設定 (End / End / visible area) |
| `02-portrait-dialog.png` | 縦向きで表示したダイアログ (可視領域の右下に配置) |
| `03-landscape-dialog-raw.png` | 横向きに回転した後のダイアログ (simctl 出力の原本、画素は縦向き) |
| `03-landscape-dialog.png` | 同上を横向きに直した版 (可視領域の右下に再配置) |
| `xcodebuild-window-change.log` | PB-WN-01〜03 の実行ログ |
