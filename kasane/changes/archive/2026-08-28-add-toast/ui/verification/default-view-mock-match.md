# デフォルト View (ピル) とモックの視覚照合 — 証跡 (tasks 7.1)

2026-08-27 実施。承認済みモック `ui/mock/approved.png` (案A: OS Toast 準拠ピル) と
`ui/mock/approved-sample-custom.png` (Sample の Custom Toast デモ View) を「見た目の正」として、
実装のスクリーンショットと照合した。照合は**構造・トークン・意図**の3点で見る (ピクセル一致は追わない)。

## 照合に使った実装スクリーンショット

| 観点 | 画像 |
|---|---|
| 1行メッセージ (ピル) | `ios-01-default-toast.png` / `android-01-default-toast.png` / `maui-android-01-default-toast.png` / `maui-ios-01-default-toast.png` / `kmp-ios-01-default-toast.png` / `kmp-android-01-default-toast.png` |
| 複数行メッセージ + 多重の重なり | `ios-04-toast-stack.png` / `android-04-toast-stack.png` / `maui-android-04-toast-stack.png` / `maui-ios-04-toast-stack.png` / `kmp-ios-04-toast-stack.png` / `kmp-android-04-toast-stack.png` |
| Sample の Custom Toast デモ View | `ios-03-custom-toast.png` / `android-03-custom-toast.png` / `maui-android-03-custom-toast.png` / `maui-ios-03-custom-toast.png` / `kmp-ios-03-custom-toast.png` / `kmp-android-03-custom-toast.png` |

`maui-ios-*` は 2026-08-27 に追加した (iOS ビルドが通らないとした初回の記録が誤診だったため。
経緯は `sample-walkthrough.md`)。ピル形状・地色の透け方・白 14 の中央寄せ・落ち影・複数行時に
高さだけ伸びる形・最大幅のいずれも他ルートと同じ見えで、approved.png と一致している。

## デフォルト View の照合結果

**判定: 一致 (2周で収束)。** 初回照合では一致と記録したが、review-001 が Android の落ち影欠落 (と本記録の誤り) を指摘。実装追加 + 再撮影後の再照合で、影を含め全観点一致 (下表と※注)。

| 観点 | モック | 実装 | 判定 |
|---|---|---|---|
| 構造 | 単一のピル。中身はテキストのみ・中央寄せ・覆いなし | 同じ (背景はデフォルト View が自分で描く) | ✓ |
| 配置 | 可視領域の下部中央 + 上方向オフセット 80 | 同じ (契約既定。`Toast Placement` で上部中央への上書きも確認) | ✓ |
| 1行時の形 | 角丸 22 でピルに見える | 同じ | ✓ |
| 複数行時の形 | 角丸長方形として高さだけ伸びる (卵形にしない) | 同じ (`Toast Stack` の3枚目) | ✓ |
| 最大幅 | 80% | 同じ | ✓ |
| 地色 | 半透明のダークグレー | 同じ (背後のメニュー行が透けている) | ✓ |
| 文字 | 白・14・中央寄せ・行間 1.5 | 同じ | ✓ |
| 影 | 弱い落ち影 | 同じ (iOS = layer の影、Android = 地色の輪郭に沿った elevation の影) | ✓ (※) |
| 多重の重なり | 起動順に重なる (図の縦ずらしは図示のためで、実際は同一配置なら同座標) | 同じ (`Toast Stack` は placement を変えて視認用にずらしている — デモ側の指定) | ✓ |

※ 影の行は当初、Android 実装に落ち影が無いまま「同じ ✓」と記録していた (review-001 の Minor 指摘)。
2026-08-27 の修正で Android 側にも同等の落ち影を入れ、この記述を実態に合わせた。
上表の `android-01-default-toast.png` / `maui-android-01-default-toast.png` /
`kmp-android-01-default-toast.png` は**この修正の後に撮り直したもの**で、ピルの輪郭に沿った
落ち影が写っている (Pixel 4a 実機・`--es demo default-toast`)。撮り直し後に approved.png と
再照合し、影を含めて一致していることを確認した (2026-08-27)。

既定値はブリーフの「実装の既定値の指標」表と実装の定数が一致している (コード側の確認):

| 項目 | 指標 | iOS | Android |
|---|---|---|---|
| 背景色 | #323232 92% | `ToastStyle.builtinBackgroundColor` = RGB 0x32/0x32/0x32・alpha 0.92 | `ToastStyle.BUILTIN_BACKGROUND_COLOR` = `0xEB323232` (0xEB = 92.2%) |
| 文字色 | 白 | `.white` | `Color.WHITE` |
| フォントサイズ | 14 | `fontSize: Double = 14` | `fontSize: Double = 14.0` |
| 角丸半径 | 22 (固定) | `cornerRadius: Double = 22` | `cornerRadius: Double = 22.0` |
| 既定 duration | 1500ms | `builtinDefaultDuration = 1500` | `BUILTIN_DEFAULT_DURATION = 1500` |
| アプリ既定配置 | なし (nil) | `defaultPlacement` 既定 nil | `defaultPlacement: DialogPlacement? = null` |
| 余白 | 11 × 22 | `verticalPadding = 11` / `horizontalPadding = 22` | `VERTICAL_PADDING_DP = 11f` / `HORIZONTAL_PADDING_DP = 22f` |
| 最大幅 | 80% | `maxWidthRatio = 0.8` | `MAX_WIDTH_RATIO = 0.8f` |
| 行間 | 1.5 | `lineHeightRatio = 1.5` | `LINE_HEIGHT_RATIO = 1.5f` |
| 落ち影 | 弱い落ち影 (モックは `0 2px 10px rgba(0,0,0,.25)`) | `shadowOpacity = 0.25` / `shadowRadius = 5` / `shadowOffset = (0, 2)` | `ELEVATION_DP = 5f` + `ViewOutlineProvider.BACKGROUND` |

## Sample の Custom Toast デモ View の照合結果

**判定: 一致 (乖離なし)。**

| 観点 | モック | 実装 | 判定 |
|---|---|---|---|
| 登録経路 (上) | SampleTheme primary 地 + on-primary 文字 + 丸バッジ | 同じ (`カスタムトースト` + チェックの丸バッジ) | ✓ |
| インライン経路 (下) | surface-variant 地 + on-surface 文字 + divider 枠 | 同じ (`インライントースト`) | ✓ |
| 2枚の重なり | 上下2段で同時に見える | 同じ (4ルートすべて) | ✓ |

## 合意済みの妥協

なし (プラットフォーム制約による妥協は発生していない)。

## トークン候補

なし。デフォルト View の値は ToastStyle の既定として実装側に閉じており、
Sample のデモ View は既存の SampleTheme トークンだけを参照している。

## 関連

- モック: `ui/mock/default-pill.html` (`ui/mock/approved.png`) / `ui/mock/sample-custom.html` (`ui/mock/approved-sample-custom.png`)
- 既定値の指標: `ui/brief.md`「実装の既定値の指標」
- 実装: `ios/Sources/KsDialogs/Presentation/ToastDefaultContentView.swift`、
  `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/ToastDefaultContentView.kt`
