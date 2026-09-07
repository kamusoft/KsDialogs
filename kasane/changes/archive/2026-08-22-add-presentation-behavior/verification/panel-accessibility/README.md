# トランジションデモ画面 操作部の読み上げ対応 — accessibility tree 検査の証跡

`Transition Dialog` のデモ画面 (演出を選ぶパネル) にある操作部へ与えた読み上げ用の名前・役割・状態を、
4ルートそれぞれの accessibility tree を実機/シミュレータから取り出して確認した記録 (2026-08-21)。

規約は `kasane/concepts/cross/conventions/sample-parity.md` の「パネル操作部の読み上げ」節、
先例は `kasane/changes/archive/2026-08-19-expand-api-surface/verification/panel-accessibility/`
(Layout Dialog の属性調整パネル) で、取得方法・ファイル形式・読み方をそのまま踏襲している。

対象の操作部は本変更で新設された次のもの:
戻る記号 `‹`、プリセットのチップ8個、`時間` のスライダ、イージングのチップ4個、`表示` ボタン。

## 取得方法

| ルート | 取得方法 |
|---|---|
| ios | XCUITest (`XCUIApplication(bundleIdentifier:)` で起動 → `app.debugDescription`) を使い捨てのプローブ用 Xcode プロジェクトから実行。Sample 側には何も足していない |
| kmp (iosApp) | 同上 (bundle id だけ差し替え) |
| maui (iOS) | 同上 (bundle id だけ差し替え) |
| android | Pixel 6a 実機 (API 36 / serial `2A141JEGR18112`、1080x2400) で `adb shell uiautomator dump` |
| maui (Android) | 同上 |
| kmp (androidApp) | 同上 |

iOS 系は3ルートとも iPhone 17 Simulator (iOS 26.5 / UDID `<uuid>`) の
**導入済みアプリへ後から取り付ける**形で撮っており、Sample の再ビルドはしていない。
プローブは UI テスト用ターゲット1つだけの使い捨てプロジェクト (対象アプリを持たない構成) で、
`TEST_RUNNER_AX_BUNDLE_ID` で対象の bundle id を渡し、メニューの `Transition Dialog` を押して
デモ画面に入ってから2状態の `debugDescription` を出力する。証跡はその出力を切り出したもの。

各ルートとも2状態を取得している。

- **initial**: デモ画面を開いた直後 (プリセット `Fade` / 時間 `250 ms` / イージング `Standard`)
- **changed**: 時間のスライダを右端 (`600 ms`) へ動かしてから `None` を選んだ状態
  (選択が移り、調整部が無効になり、時間の値は保たれる)

## 確認できたこと

### 名前と役割

| 操作部 | ios / kmp-ios / maui-ios | android / maui-android / kmp-android |
|---|---|---|
| 戻る記号 | `Button`、label `戻る` | `android.widget.Button`、content-desc `戻る` |
| プリセットの選択肢 (8個) | `Button`、label `Fade` / `Slide Up` / `Slide Down` / `Slide Start` / `Slide End` / `Zoom` / `None` / `Custom Hook` | `android.widget.Button`、content-desc に同じ8件 |
| 時間のスライダ | `Slider`、label `時間` | `android.widget.SeekBar`、content-desc `時間` |
| イージングの選択肢 (4個) | `Button`、label `Standard` / `Linear` / `Accelerate` / `Decelerate` | `android.widget.Button`、content-desc に同じ4件 |
| 表示操作 | `Button`、label `表示` | `android.widget.Button`、text `表示` |

読み上げ名は6形態とも画面の文言と同一で、**12個の選択肢に同じ文言のものはない**。
そのため Layout パネルの `Start` / `Center` / `End` のような複合名 (`Horizontal Start`) は必要にならない
(規約は「同名になる選択肢だけ」を複合名の対象としている)。

### 状態と現在値

| 状態 | ios / kmp-ios | maui-ios | android / maui-android / kmp-android |
|---|---|---|---|
| 選択肢の選択状態 | `Selected` 特性 (initial は `Fade` + `Standard`、changed は `None` + `Standard`) | 同左 | `selected="true"` (同左) |
| スライダの現在値 | `value: 250 ms` → `value: 600 ms` | `value: 30%` → `value: 100%` | 標準の `SeekBar` の役割が持つ (下記「取得方法の限界」) |
| 調整部の無効状態 | スライダ・イージングチップとも `Disabled` | **スライダだけ `Disabled`。イージングチップに付かない** (下記の所見1) | `enabled="false"` (時間の行・スライダ・イージングチップすべて) |
| 無効中も値が保たれること | `600 ms` / `Standard` が残る | 同左 | `text="600 ms"` と `selected="true"` が残る |

## 規約との照合

`sample-parity.md`「パネル操作部の読み上げ」の各項を6形態で照合した表。

| 規約の項 | ios | kmp-ios | maui-ios | android | maui-android | kmp-android |
|---|---|---|---|---|---|---|
| 読み上げ名は画面の文言と同一 | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| 同名の選択肢は複合名 | 該当なし (同名の選択肢が存在しない) | 該当なし | 該当なし | 該当なし | 該当なし | 該当なし |
| 役割 (押せるものはボタン / スライダはスライダ) | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| 選択状態が OS 標準の状態機構に載る | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| 現在値が読み上げ名への埋め込みでなく状態として載る | ✅ | ✅ | ✅ (百分率) | ✅ (標準 SeekBar) | ✅ (標準 SeekBar) | ✅ (標準 SeekBar) |
| 戻る記号に名前とボタンの役割 | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |

**規約の明文 (名前・役割・選択状態・現在値) は6形態すべてで満たしている。**
以下は規約の列挙には無いが、ルート間で差が出た点。

### 所見1: maui-ios だけ、調整部の無効状態が読み上げに乗らない (要判断・未修正)

changed 状態で、他の5形態はイージングチップにも無効状態が付く
(iOS 系は `Disabled`、Android 系は `enabled="false"`) のに対し、
**maui-ios だけはスライダにしか付かない**。

```
-- ios (changed)                        -- maui-ios (changed)
Slider,  label: '時間', value: 600 ms, Disabled   Slider,  label: '時間', value: 100%, Disabled
Button,  label: 'Standard', Selected, Disabled    Button,  label: 'Standard', Selected
Button,  label: 'Linear', Disabled                Button,  label: 'Linear'
Button,  label: 'Accelerate', Disabled            Button,  label: 'Accelerate'
Button,  label: 'Decelerate', Disabled            Button,  label: 'Decelerate'
```

**操作不可であること自体は maui-ios でも成立している。** 無効中に `Linear` を押す補助プローブを
別に流したところ、押しても選択は `Standard` のまま動かなかった (画面の見た目も
`ui/verification/maui-ios-transition-panel-adjust-disabled.png` のとおり薄くなっている)。
乗っていないのは**スクリーンリーダーへの通知だけ**で、VoiceOver の利用者には
「押せるボタン」に見えたまま押しても何も起きない状態になる。

原因は Sample の書き方ではなく MAUI の `IsEnabled` 伝播で、`AdjustBlock` の無効化が
直下の `Slider` の platform view には届く一方、`ContentView` (`EasingChipsHost`) を挟んだ
チップの `UIButton` までは届かないため。合わせるなら、選択状態を platform 側へ直付けしている
`SampleChipsView.ApplySelectedState` (`samples/maui/KsDialogs.Sample.Maui/SampleChipsView.cs`) と
同じ形で、無効状態も platform 側へ直付けする追加が要る。

規約が状態として列挙しているのは「選択状態・ON/OFF・現在値」までで無効状態は含まないため、
**規約違反とは断定していない**。4ルート一致の観点で直すかどうかはオーケストレーターの判断に委ねる
(本作業では修正していない)。

### 所見2: スライダの現在値の読み上げ表現がルートで異なる

ios / kmp-ios は `accessibilityValue` を与えているため `250 ms` / `600 ms` と読み、
maui-ios は OS 既定の百分率 (`30%` / `100%`)、Android 系は標準 `SeekBar` の値になる。

どれも「OS 標準の状態機構に載せる (読み上げ名への埋め込みで代用しない)」は満たしており、
先例でも移動量欄の読み上げ文字列がルートで違うこと (`OffsetX, 24`) をそのまま記録している。
**規約違反ではないが、表現を揃えたいなら maui-ios にも `SemanticProperties` 相当の値付けが要る。**

### 取得方法の限界: Android のスライダの現在値

`uiautomator dump` はノードの `RangeInfo` (スライダの現在値・範囲) を書き出さないため、
Android 系3形態の XML には値そのものが現れない。証跡から読めるのは
「標準の `android.widget.SeekBar` の役割であること」「読み上げ名が `時間` であること」
「隣接する表示用の `TextView` が `250 ms` → `600 ms` に変わり、無効中も保たれること」まで。
値は標準ウィジェットが持つ機構にそのまま乗っているため、実装側の追加は不要と判断した。

## ファイル

| ファイル | 内容 |
|---|---|
| `ios-xcuitest-dump.txt` | ios ルートの accessibility 階層 (initial / changed) |
| `kmp-ios-xcuitest-dump.txt` | kmp ルート iosApp の accessibility 階層 (initial / changed) |
| `maui-ios-xcuitest-dump.txt` | maui ルート (iOS) の accessibility 階層 (initial / changed) |
| `android-uiautomator-initial.xml` / `-changed.xml` | android ルートのノードツリー |
| `maui-android-uiautomator-initial.xml` / `-changed.xml` | maui ルート (Android) のノードツリー |
| `kmp-android-uiautomator-initial.xml` / `-changed.xml` | kmp ルート androidApp のノードツリー |

見た目 (無効表示の薄さ・選択チップの塗り) は本証跡ではなく
`kasane/changes/add-presentation-behavior/ui/verification/` の
`*-transition-panel-initial.png` / `*-transition-panel-adjust-disabled.png` 側にある。
読み上げ対応は見た目を変えないため、この2組は同じ画面を別の側面から撮ったものになる。
