# Sample 通しと演出の実機確認 — 証跡 (iOS / Android)

tasks 8.4 の iOS / Android 分の記録 (2026-08-21)。MAUI / KMP の2ルートは別グループが担当するため、
このディレクトリには2ルート分だけがある。

対象は samples デルタスペックの Requirement「トランジションデモ」(PB-SM-01〜03) と、
ui/brief.md の文言表・調整面の規範表。

## 環境

| ルート | 端末 | ビルドと起動 |
|---|---|---|
| iOS | iPhone 17 Simulator (iOS 26.5 / UDID `<uuid>`) | `xcodebuild -project samples/ios/KsDialogsSample.xcodeproj -scheme KsDialogsSample -destination 'platform=iOS Simulator,id=F899B356-…' -derivedDataPath DerivedData CODE_SIGNING_ALLOWED=NO build` → `simctl install` / `simctl launch jp.kamusoft.ksdialogs.samples.ios` |
| Android | Pixel 6a 実機 (API 36 / serial `2A141JEGR18112`)。API 33 実機 (Pixel 4a / `0B261JEC216142`) でも起動確認 | `cd samples/android && ./gradlew :app:assembleDebug` → `adb install -r` / `am start -n jp.kamusoft.ksdialogs.samples.android/.MainActivity` |

## 通した操作

両ルートとも同じ順で操作した。

1. メニューの6項目目 `Transition Dialog` を押してデモ画面へ入る
2. 初期状態を確認する (プリセット `Fade` / 時間 `250 ms` / イージング `Standard`)
3. `表示` → ダイアログの `OK` → 結果 `結果: completed(true)` がデモ画面とメニューの両方に出る
4. `None` を選ぶ → 時間・イージングが無効表示になり、値 (250 ms / Standard) は保たれる
5. `Custom Hook` を選ぶ → 同じく無効表示。`表示` → `キャンセル` → `結果: cancelled`
6. `Slide Up` + 時間 600 ms で `表示` → `キャンセル` (演出を目視できる長さにして撮影)
7. メニューへ戻り、既存デモ (`Basic Dialog`・`Layout Dialog` のパネル) が変わらず動くことを確認

## 演出が動いていることの証跡 (連写)

アニメーションは数百ミリ秒で終わるため、`xcrun simctl io … screenshot` /
`adb exec-out screencap` を間隔を空けずに連写し、変化のあったコマを左から順に並べた。
**厳密な中間フレーム比較はしていない** — 「動いていること」と「方向・対象が契約どおりであること」が
見えることを目的にしている。

| ファイル | 見えるもの |
|---|---|
| `ios-01-slideup-presentation.png` | 下辺の外から中身が滑り込み、覆いが同時に濃くなっていく (600 ms)。中身と覆いが並行に進む |
| `ios-02-slideup-dismissal.png` | 中身が下辺へ滑り出し、覆いが薄れる。**中身が消えたコマで初めて** `結果: cancelled` が出る (配送は撤去の後) |
| `ios-03-custom-presentation.png` | 自作フックの入場。調整部が無効表示のまま (Custom Hook は調整値を使わない) |
| `ios-04-custom-dismissal.png` | 自作フックの退場を透明度の途中で捉えたコマ (中身が半透明・覆いも薄れている) |
| `android-01-slideup-presentation.png` | 下辺から中身が持ち上がる途中のコマ + 覆いが薄い状態 |
| `android-02-slideup-dismissal.png` | 中身が下辺へ向かって下がる途中のコマ。直前の `結果: completed(true)` が下がっている間は据え置かれ、**中身が消えたコマで** `結果: cancelled` へ変わる (配送は撤去の後) |
| `android-03-custom-presentation.png` | 自作フックの入場 (覆いがまだ薄いコマ)。調整部は無効表示 |
| `android-04-custom-dismissal.png` | 自作フックの退場と、その後に出た `結果: completed(true)` |
| `android-08-fade-presentation-midframe.png` | 既定側のプリセット `Fade` (250 ms) の入場を偶然捉えた1コマ (中身がほぼ透明) |

連写の間隔は撮影コマンドの往復時間 (iOS で概ね 0.3〜0.5 秒、Android で 0.3 秒前後) が下限になるため、
250〜300 ms の演出では中間コマが取れないことがある。そのため観察用に 600 ms を使ったのが 1〜2 の組で、
自作フック (時間は演出側に固定) は取れたコマだけを載せている。

## 動的モックとの突き合わせ

`ui/mock/mock-preset-motion.html` の動作イメージと見比べた結果:

- 中身と覆いが**別レイヤ**で、覆いのフェードが中身の演出を巻き込まない — 一致 (連写のどのコマでも
  中身の透明度と覆いの濃さが別々に動いている)
- スライドは**入った辺と同じ辺へ**出る — 一致 (`Slide Up` が下から入り下へ出る)
- 覆いのフェード時間が本体の演出に揃う (プリセットは同じ duration) — 一致 (600 ms の組で、覆いの
  濃さの変化が中身の移動と同じ長さで進む)

**動的モックは参考であり受け入れ基準ではない。** 受け入れ基準は dialog-contract の Scenario
(順序と完了待ち) で、そちらは本体側のテストが固定している。ここで見ているのは
「Sample から使ったときに、契約どおりの見え方になるか」だけである。

## 既存デモの退行確認

| ファイル | 内容 |
|---|---|
| `ios-06-basic-dialog-regression.png` / `android-06-basic-dialog-regression.png` | `Basic Dialog` が従来どおり出る (既定の演出がクロスフェードに変わっている点は本体の仕様) |
| `ios-07-layout-panel-regression.png` / `android-07-layout-panel-regression.png` | `Layout Dialog` の属性調整パネルが従来どおり開き、初期値 (Center / Center / 0 / 0 / ON) も変わっていない |

iOS はデモ画面を2枚目の全画面カバーとして足したため、Layout パネルが従来どおり開くことを
特に確認している (7 の画像)。

## 設定操作の証跡

| ファイル | 内容 |
|---|---|
| `ios-05-settings-slideup-600ms.png` | スライダを右端へ動かして `600 ms` になった状態 (途中で `590 ms` を経由しており、10 ms 刻みで動くことも確認済み) |
| `android-05-settings-slideup-600ms.png` | 同上 (`Slide Up` 選択 + 600 ms + 直前の `結果: cancelled`) |
| `android-09-api33-panel.png` | API 33 実機でのデモ画面。`SeekBar` の下限を API 26 以降の API に頼らず目盛りで持っているため、最低対象 OS でも初期値 250 ms が正しく出る |

## 未確認 / 申し送り

- MAUI / KMP の2ルートは別グループの担当。4ルート一致の確認 (PB-SM-03) は4ルート揃った時点で行う
- `Custom Hook` の固定時間 300 ms と移動量 80pt (Android は 80dp) は brief に規定がなく、
  実装で決めた値。4ルート一致させるため ui/brief.md に記録した

---

# Sample 通しと演出の実機確認 — 証跡 (MAUI / KMP)

tasks 8.4 の MAUI / KMP 分の記録 (2026-08-21)。上の節 (iOS / Android) とは別グループの担当分で、
同じ Requirement「トランジションデモ」(PB-SM-01〜03) と ui/brief.md の文言表・調整面の規範表が対象。

**MAUI iOS だけは通しが成立しなかった。** 演出を添付したダイアログを表示した瞬間にアプリが落ちる
(下の「MAUI iOS の中断」)。原因は Sample ではなく MAUI の iOS ブリッジ側にあり、Sample 側では回避できない。

## 環境

| ルート | 端末 | ビルドと起動 |
|---|---|---|
| MAUI Android | Pixel 6a 実機 (API 36 / serial `2A141JEGR18112`) | `dotnet build -f net10.0-android -p:EmbedAssembliesIntoApk=true` → `adb install -r` / `am start -n jp.kamusoft.ksdialogs.samples.maui/crc648e69508f1bc815cb.MainActivity` |
| MAUI iOS | iPhone 17 Simulator (iOS 26.5 / UDID `F899B356-…`) | `DEVELOPER_DIR=/Applications/Xcode-26.1.1.app/Contents/Developer dotnet build -f net10.0-ios` → `simctl install` / `simctl launch jp.kamusoft.ksdialogs.samples.maui` |
| KMP Android | 同 Pixel 6a 実機 | `cd samples/kmp && ./gradlew :androidApp:assembleDebug` → `adb install -r` / `am start -n jp.kamusoft.ksdialogs.samples.kmp.android/.MainActivity` |
| KMP iOS | 同 iPhone 17 Simulator | `xcodebuild -project samples/kmp/iosApp/KsDialogsSampleKmp.xcodeproj -scheme KsDialogsSampleKmp …` → `simctl install` / `simctl launch jp.kamusoft.ksdialogs.samples.kmp.ios` |

`DEVELOPER_DIR`: .NET for iOS 26.1.10502 は Xcode 26.1 を要求するため、既定の Xcode 26.5 のままでは
ビルドが拒否される。KMP iosApp 側は既定の Xcode 26.5 で通る。

## 通した操作 (MAUI Android / KMP Android / KMP iOS)

3ルートとも同じ順で操作した。

1. メニューの6項目目 `Transition Dialog` を押してデモ画面へ入る
2. 初期状態を確認する (プリセット `Fade` / 時間 `250 ms` / イージング `Standard`)
3. `表示` → ダイアログの `OK` → 結果 `結果: completed(true)` がデモ画面とメニューの両方に出る
4. `None` を選ぶ → 時間・イージングが無効表示になり、値 (250 ms / Standard) は保たれる
5. `Slide Up` + 時間 600 ms で `表示` → `OK` / `キャンセル` (演出を目視できる長さにして連写)
6. `Custom Hook` を選ぶ → 同じく無効表示。`表示` → 結果が返る
7. メニューへ戻り、既存デモ (`Basic Dialog`・`Layout Dialog` のパネル) が変わらず動くことを確認

## 演出が動いていることの証跡 (連写)

`adb exec-out screencap` / `xcrun simctl io … screenshot` を間隔を空けずに連写し、変化のあったコマを
左から順に並べた。**厳密な中間フレーム比較はしていない** — 「動いていること」と「方向・対象が
契約どおりであること」が見えることを目的にしている。

| ファイル | 見えるもの |
|---|---|
| `maui-android-01-slideup-presentation.png` | 下辺の外から中身が滑り込み、覆いが同時に濃くなる (600 ms) |
| `maui-android-02-slideup-dismissal.png` | 中身が下辺へ滑り出す。**結果表示は中身が消える前に切り替わっている** (下の所見) |
| `maui-android-03-fade-dismissal-early-delivery.png` | Fade 600 ms の退場。中身がまだ半透明で見えているコマで `結果: completed(true)` が出ている |
| `maui-android-04-zoom-presentation.png` | 0.8 倍から等倍へ広がる途中のコマ (中身が小さく半透明) |
| `maui-android-05-none-presentation.png` | 中身は最初から等倍・不透明 (演出なし)。覆いだけがフェード中のコマ |
| `maui-android-06-settings-slideup-600ms.png` | スライダ右端で `600 ms`。10 ms 刻みで動く |
| `maui-android-09-custom-presentation.png` | 自作フックの入場を透明度と位置の途中で捉えたコマ (調整部は無効表示のまま。閉じると `結果: completed(true)`) |
| `kmp-android-01-slideup-presentation.png` | 下辺から中身が持ち上がる途中のコマ + 覆いが薄い状態 |
| `kmp-android-02-slideup-dismissal.png` | 中身が下辺へ下がる途中のコマ。直前の `結果: completed(true)` は据え置かれ、**中身が消えたコマで** `結果: cancelled` へ変わる |
| `kmp-android-03-custom-presentation.png` | 自作フックの入場。調整部が無効表示のまま |
| `kmp-ios-01-slideup-presentation.png` | 下辺の外から中身が現れ、覆いが濃くなっていく 4 コマ |
| `kmp-ios-02-slideup-dismissal.png` | 中身が下辺へ滑り出し、**中身が消えたコマの次で**結果表示が切り替わる |
| `kmp-ios-03-custom-presentation.png` | 自作フックの入場を透明度の途中で捉えたコマ (調整部は無効表示) |

連写の間隔は撮影コマンドの往復時間 (Android で 0.3 秒前後、iOS Simulator で 0.36 秒前後) が下限になるため、
250〜300 ms の演出では中間コマが取れないことがある。観察用に 600 ms を使ったのがスライド系の組で、
自作フック (時間は演出側に固定の 300 ms) は取れたコマだけを載せている。

## 動的モックとの突き合わせ (KMP 2ルート・MAUI Android)

`ui/mock/mock-preset-motion.html` の動作イメージと見比べた結果:

- 中身と覆いが**別レイヤ**で、覆いのフェードが中身の演出を巻き込まない — 一致
- スライドは**入った辺と同じ辺へ**出る — 一致 (`Slide Up` が下から入り下へ出る)
- 覆いのフェード時間が本体の演出に揃う — 一致 (600 ms の組で、覆いの濃さの変化が中身の移動と同じ長さで進む)
- `None` は中身側だけ無演出で、覆いはフェードする — 一致 (`maui-android-05`)

**動的モックは参考であり受け入れ基準ではない。**

## 所見1: MAUI iOS の中断 (Sample では回避できない)

演出を添付したダイアログを `表示` で出すと、**プリセットの種類によらず** (`None` でも) アプリが
SIGSEGV で落ちる。`Basic Dialog` など演出を添付しないデモは正常に動く
(`maui-ios-02-basic-dialog-regression.png`)。

- 落ちたところ: `maui-ios-01-transition-crash.png` (ホーム画面に戻っている)
- ログ: `maui-ios-transition-crash.log` (`simctl launch --console-pty` の出力)

managed stacktrace の要点:

```
at System.Action:wrapper_aot_native
at KsDialogs.DialogSingleCompletion:Complete
at KsDialogs.PlatformDialogGateway:RunOnUiThread
…
at KsDialogs.DialogTransitionRunner:Start / :Run
at <>c__DisplayClass14_0:<ToRunner>b__0
```

native 側は `KsDialogsMauiBridge.MauiDialogContent.installTransition(presentation:dismissal:overlayDuration:)`
のクロージャの中で落ちている。つまり **C# 側が Swift から受け取った完了通知の block を呼んだ瞬間**に落ちる。

Sample がしているのは公開 API どおりの添付 (`Dialog.SetTransition(view, transition)`) だけで、
落ちる位置は本体 (`maui/KsDialogs.Maui/Platforms/iOS/PlatformDialogGateway.cs` の `ToRunner` が返す
`(_, completion) => runner.Run(completion)` と `maui/macios/KsDialogs.Binding.iOS/ApiDefinition.cs` の
`installTransitionWithPresentation:dismissal:overlayDuration:` の宣言) にある。
同じ経路を通る MAUI Android は正常に動くため、iOS binding 側の block 引数の扱い
(`Action<UIView, Action>` の内側の `Action` = native から渡ってくる block) が疑わしい。

**このため MAUI iOS の 6 状態のうち 3 つ (ダイアログ表示・デモ画面の結果・メニューの結果) は撮れていない。**

## 所見2: MAUI Android の結果配送のタイミング

契約 (transition-semantics の「ラッチと配送」) は「dismissal とオーバーレイ消滅の完了 → 撤去 → 配送」と定めており、
Native 2ルートと KMP 2ルートはそのとおりに見える (中身が消えたコマで初めて結果表示が変わる)。

**MAUI Android だけは、中身がまだ画面に見えている間に結果表示が切り替わる。**
`maui-android-03-fade-dismissal-early-delivery.png` では、中身が半透明で残っているコマで
すでに `結果: completed(true)` が出ている (`maui-android-02` の Slide Up でも同様)。

Sample 側は `await Dialog.Instance.ShowAsync(viewModel)` の戻りで結果表示を更新しているだけなので、
`ShowAsync` が退出処理の完了前に返っていることになる。これも本体側の所見として申し送る。

## 既存デモの退行確認

| ファイル | 内容 |
|---|---|
| `maui-android-07-basic-dialog-regression.png` / `maui-ios-02-basic-dialog-regression.png` / `kmp-android-05-basic-dialog-regression.png` / `kmp-ios-05-basic-dialog-regression.png` | `Basic Dialog` が従来どおり出る |
| `maui-android-08-layout-panel-regression.png` / `kmp-android-06-layout-panel-regression.png` / `kmp-ios-06-layout-panel-regression.png` | `Layout Dialog` の属性調整パネルが従来どおり開き、初期値 (Center / Center / 0 / 0 / ON) も変わっていない |

MAUI iOS の Layout パネルは、Transition の中断の切り分け中にアプリが落ちる経路と混ざるのを避けるため
未撮影 (Basic Dialog の退行確認までは取れている)。

## 未確認 / 申し送り

- **MAUI iOS の通しは未完了** (所見1)。本体の iOS ブリッジが直ったあとに、6 状態の撮影と
  Fade / Slide / Zoom / None の連写をやり直す必要がある
- **MAUI Android の結果配送のタイミング** (所見2) は本体側の判断待ち
- 4ルート一致の確認 (PB-SM-03) は、MAUI iOS が動くようになってから改めて行う

---

# MAUI 2件の不具合修正 — 再現解消の確認 (2026-08-21)

上の「所見1」(MAUI iOS の SIGSEGV) と「所見2」(MAUI Android の早すぎる配送) を修正したあとの確認記録。
実行時挙動の検証規約 (cross/conventions) に従い、**修正前に症状を再現した手順と同じ手順**で解消を確認した。

Sample のコードは変更していない。修正はどちらも MAUI の本体側 (iOS binding 定義と C# の配送点) にある。

## 環境

上の MAUI / KMP の節と同じ。MAUI iOS は iPhone 17 Simulator (iOS 26.5 / UDID `F899B356-…`)、
MAUI Android は Pixel 6a 実機 (API 36 / serial `2A141JEGR18112`)。ビルドと起動のコマンドも同じ。

## 所見1 (MAUI iOS の SIGSEGV) の解消

再現手順は所見1と同一 — `Transition Dialog` → プリセットを選ぶ → `表示`。

| ファイル | 見えるもの |
|---|---|
| `maui-ios-10-transition-presets-fixed.png` | `Fade` / `None` / `Custom Hook` の3種でダイアログが出ている (左から順)。プリセットの種類によらず落ちていた症状が出ない |
| `maui-ios-12-basic-dialog-regression-fixed.png` | 演出を添付しない `Basic Dialog` も従来どおり (`キャンセル` で `結果: cancelled`) |
| `maui-ios-transition-fixed.log` | `simctl launch --console-pty` の出力。同じ操作を通しても `Native Crash Reporting` / `SIGSEGV` の節が 1 つも出ない (修正前は `maui-ios-transition-crash.log`) |

3種とも `OK` / `キャンセル` まで通し、結果 (`completed(true)` / `cancelled`) がデモ画面に出るところまで確認した。
`Layout Dialog` の属性調整パネルも従来どおり開き、初期値 (Center / Center / 0 / 0 / ON) は変わっていない。

## 所見2 (結果配送のタイミング) の解消

再現手順は所見2と同一 — `Fade` (MAUI Android) / `Slide Up` (MAUI iOS) を 600 ms にして `表示` → `OK` を連写。
**直前の結果を `cancelled` にしてから `OK` で閉じ**、`cancelled` → `completed(true)` の切り替わりが
どのコマで起きるかを見た (同じ結果を続けると遷移が読めないため)。

| ファイル | 見えるもの |
|---|---|
| `maui-android-10-fade-dismissal-fixed.png` | 左から: ダイアログ表示中 (`結果: cancelled`) → **中身が半透明で残っているコマでも `結果: cancelled` のまま** → 中身が消えたコマで `結果: completed(true)` → 静止。修正前の `maui-android-03-fade-dismissal-early-delivery.png` は、この2コマ目にあたる状態で既に `completed(true)` が出ていた |
| `maui-ios-11-slideup-dismissal-fixed.png` | 同じ見方で MAUI iOS。中身と覆いが残っている2コマは `結果: cancelled` のまま、中身が消えたコマで `completed(true)` に変わる |

これで4ルートとも「中身が消えたコマで初めて結果表示が切り替わる」で揃った。

## 未確認 / 申し送り

- **Sample 8.3 / 8.4 の MAUI iOS 分の撮影** (6 状態を `ui/verification/maui-ios-*.png` へ、
  Fade / Slide / Zoom / None の連写) は別の担当へ引き継ぐ。ここで撮ったのは再現解消の確認に必要な最小限
- 4ルート一致の確認 (PB-SM-03) も、上の撮影が揃った時点で行う

---

# MAUI iOS の撮り直しと 4ルート一致の確認 (2026-08-21)

上の「MAUI 2件の不具合修正」で本体が直ったあとに、申し送られていた
**MAUI iOS の 8.3 (6状態) と 8.4 (演出の実機確認)** を実施した記録。

Sample のコードは変更していない。本体も変更していない (読み取りのみ)。

## 環境

| 項目 | 値 |
|---|---|
| 端末 | iPhone 17 Simulator (iOS 26.5 / UDID `<uuid>`) |
| ビルド | `DEVELOPER_DIR=/Applications/Xcode-26.1.1.app/Contents/Developer dotnet build -f net10.0-ios` (0 警告 / 0 エラー) |
| 起動 | `simctl install …/KsDialogs.Sample.Maui.app` → `simctl launch jp.kamusoft.ksdialogs.samples.maui` |
| 撮影 | `xcrun simctl io … screenshot` を scratchpad へ撮ってから証跡ディレクトリへ複写 |

連写の間隔は撮影コマンドの往復時間が下限で、実測 **0.27 秒/コマ**。600 ms の演出なら
中間コマが 2〜3 枚取れる。観察用のプリセットはすべて 600 ms にしてある
(`maui-ios-29-settings-slideup-600ms.png`)。

## 8.3 の 3 状態 (`ui/verification/`)

未撮影だった 3 状態を撮り、既存の 3 状態と合わせて 6 状態が揃った。

| ファイル | 内容 |
|---|---|
| `maui-ios-transition-dialog.png` | `Fade` / 250 ms / `Standard` の初期状態で `表示` したダイアログ |
| `maui-ios-transition-panel-result.png` | `OK` で閉じた後のデモ画面 (`結果: completed(true)`) |
| `maui-ios-menu-result.png` | `キャンセル` で閉じてメニューへ戻った状態 (`結果: cancelled`) |

Native iOS の同名スクリーンショットと並べて 4 観点で照合した結果は `ui/brief.md` の
「照合結果」に記録した (**1周で収束**、乖離による修正は 0 件)。

## 8.4 の連写証跡

**直前の結果を別の値にしてから閉じ**、どのコマで結果表示が切り替わるかを見ている
(同じ結果を続けると遷移が読めないため)。

| ファイル | 見えるもの |
|---|---|
| `maui-ios-20-fade-presentation.png` | 中身がほぼ透明 → 半透明 → 不透明へ濃くなり、覆いも並行して濃くなる (600 ms)。**契約どおり** |
| `maui-ios-21-fade-dismissal.png` | 中身が半透明で残るコマは `結果: cancelled` のまま、**中身が消えたコマの次で** `結果: completed(true)` に変わる。**契約どおり** |
| `maui-ios-22-slideup-presentation.png` | 覆いが濃くなっていく 3 コマ。**中身は最初のコマから最終位置・不透明のまま動かない** (所見3) |
| `maui-ios-23-slideup-dismissal.png` | 覆いが薄れていく 3 コマ。**中身は最終位置・不透明のまま留まり、最後に消える** (所見3) |
| `maui-ios-24-zoom-presentation.png` | 中身の透明度は変わるが、**0.8 倍から等倍へ広がる動きが出ない** (所見3) |
| `maui-ios-25-none-presentation.png` | 中身は最初から等倍・不透明 (演出なし)、覆いだけがフェードする。**契約どおり** |
| `maui-ios-26-none-dismissal.png` | 1 回目の連写。覆いが消えたコマで中身がまだ残り、結果が既に切り替わっている (所見4) |
| `maui-ios-30-none-dismissal-second-run.png` | 2 回目の連写。中身と覆いが同じコマで消え、そのコマで結果が切り替わる (所見4 は再現せず) |
| `maui-ios-27-custom-presentation.png` | 自作フックの入場。**透明度は変わるが上方向 80pt の移動が出ない**。調整部は無効表示のまま (所見3) |
| `maui-ios-28-layout-panel-regression.png` | `Layout Dialog` の属性調整パネルが従来どおり開き、初期値 (Center / Center / 0 / 0 / ON) も変わっていない |
| `maui-ios-29-settings-slideup-600ms.png` | スライダを右端へ動かして `600 ms` になった状態 |

## 動的モックとの突き合わせ

`ui/mock/mock-preset-motion.html` の動作イメージと見比べた結果:

| 項目 | MAUI iOS |
|---|---|
| 中身と覆いが別レイヤで、覆いのフェードが中身の演出を巻き込まない | 一致 (Fade / Custom で中身の透明度と覆いの濃さが別々に動く) |
| 覆いのフェード時間が本体の演出に揃う | 一致 (600 ms の組で覆いの変化が 600 ms かけて進む) |
| `None` は中身側だけ無演出で、覆いはフェードする | 一致 |
| スライドは入った辺と同じ辺へ出る | **不一致** — 中身がそもそも動かない (所見3) |
| `Zoom` は 0.8 倍から等倍へ | **不一致** — 中身が拡大縮小しない (所見3) |
| 自作フックの上方向 80pt 移動 | **不一致** — フェードだけが出る (所見3) |

**動的モックは参考であり受け入れ基準ではない。** ただし所見3 は
`ui/brief.md` の「調整面の規範表」(方向写像) と samples デルタスペックの PB-SM-01 が
求める観察結果そのものに届いていないため、規範表側の不一致として記録する。

## 所見3: MAUI iOS で中身の移動・拡大縮小が効かない (未解決)

**症状** — MAUI iOS では、演出のうち **透明度だけが動き、位置と大きさが動かない**。

| プリセット | 中身の透明度 | 中身の位置・大きさ |
|---|---|---|
| `Fade` | 動く | (対象外) |
| `Slide Up` (他 3 方向も同様) | 動かない | **動かない** (本来は辺の外から滑り込む) |
| `Zoom` | 動く | **動かない** (本来は 0.8 倍から等倍) |
| `Custom Hook` | 動く | **動かない** (本来は上方向 80pt) |
| `None` | (対象外) | (対象外) |

**測り方** — 中身の白いカードと `OK` ボタンの外接矩形をコマごとに画素で測り、
覆いの濃さから割り出した演出の進み具合と突き合わせた。決定的なコマは次の 2 つ。

- `Slide Up` 入場: 覆いの進み **2%** の時点 (演出の開始直後) で、カードは既に最終位置
  (`OK` ボタンの外接矩形 y=1334..1481) かつ不透明。600 ms かけて滑り込むなら、
  この時点では画面下辺の外にいるはずである
- `Slide Up` 退場: 覆いの進み 8% → 71% の 2 コマとも、カードは最終位置のまま不透明で
  1 画素も動かず、その次のコマで消える

同じ操作を 2 回繰り返しても同じ結果になった (`slideup-present` / `slideup-present2`)。

**Sample 側ではない** — Sample は公開 API のプリセットを選んで渡しているだけで、
同じ Sample コードで動く MAUI Android は中身が動く (`maui-android-01-slideup-presentation.png`)。
Native iOS も動く (`ios-01-slideup-presentation.png` で下辺から持ち上がる 5 コマ)。
**MAUI iOS だけの症状**である。

**見立て (未検証の仮説)** — MAUI のプリセットは
`maui/KsDialogs.Maui/Contract/DialogTransition.cs` で MAUI の `VisualElement` に対して
`FadeTo` / `TranslateTo` / `ScaleTo` を掛けている。iOS では `Opacity` は
プラットフォーム View の `alpha` に落ちるため効くが、`TranslationX/Y` と `Scale` は
プラットフォーム View の transform に落ちるため、器 (`ios/` の DialogLayoutHost) が
レイアウトパスで中身の `frame` を置き直すと打ち消される、という筋が考えられる。
Android は `TranslationY` がレイアウトと独立に効くため影響を受けない。
**本体側の担当範囲のため、ここでは調べただけで手を入れていない。**

なお `DialogTransitionRunner` がフックへ渡すのは MAUI の `VisualElement` であり、
Swift 側 (`MauiDialogContent.swift`) が渡してくる `hostView` は使われていない。
契約 (design Decision「統一形 (ホスト View)」) との対応を含めて本体側の判断が要る。

## 所見4: `None` 退場で配送順が疑わしいコマ (再現せず)

`None` の退場を 1 回目に連写したとき、**覆いが消えたコマで中身がまだ残り、
結果表示が既に `completed(true)` へ切り替わっている**コマが 1 枚だけ撮れた
(`maui-ios-26-none-dismissal.png` の 2 コマ目)。契約 (「撤去 → 配送」) の順と食い違う。

同じ操作を 2 回目に連写したときは、中身と覆いが同じコマで消え、そのコマで結果が
切り替わった (`maui-ios-30-none-dismissal-second-run.png`) ため、**再現しなかった**。
連写の間隔 (0.27 秒) が `None` の覆いのフェード (既定 250 ms) と同程度で、
撮れるコマが 1 枚しかないことも効いている。1 コマの観測だけで契約違反とは断じられないため、
**申し送りとして残す** (所見3 の修正で `None` 経路の順序も見直されるなら、そのときに一緒に確認したい)。

## 既存デモの退行確認

| ファイル | 内容 |
|---|---|
| `maui-ios-12-basic-dialog-regression-fixed.png` (既出) | `Basic Dialog` が従来どおり出る |
| `maui-ios-28-layout-panel-regression.png` | `Layout Dialog` の属性調整パネルが従来どおり開き、初期値も変わっていない。前回未撮影だった分をここで埋めた |

## 4ルート一致の自己確認

`ui/verification/` の 6 状態 × 4ルートが揃った。文言と構造は 4 ルートで一致している。

| 状態 | Native iOS | Native Android | MAUI iOS | MAUI Android | KMP iOS | KMP Android |
|---|---|---|---|---|---|---|
| `menu-initial` | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| `transition-panel-initial` | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| `transition-panel-adjust-disabled` | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| `transition-dialog` | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| `transition-panel-result` | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| `menu-result` | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |

(Native / KMP は iOS・Android の 2 端末で 1 ルート。表は端末ごとに列を割ってある)

### PB-SM-01〜03 の受け入れと証跡の対応

| Scenario | 受け入れの中身 | 証跡 | 判定 |
|---|---|---|---|
| PB-SM-01 プリセットを選んで表示できる | 選択したプリセットの演出で表示・閉鎖され、結果が結果エリアに出る。`None` 選択時は時間・イージングが無効表示 | 4ルートの `transition-panel-initial` / `transition-panel-adjust-disabled` / `transition-dialog` / `transition-panel-result`、および `*-slideup-presentation` / `*-zoom-presentation` / `*-none-presentation` の連写 | **MAUI iOS のみ未達** — 選択・調整・無効表示・結果表示は満たすが、`Slide` 系と `Zoom` の演出が観察できない (所見3)。他 3 ルートは満たす |
| PB-SM-02 カスタムフックの実演 | 自作フックによる演出で表示・閉鎖され、結果が表示に反映される | `ios-03/04`・`android-03/04`・`maui-android-09`・`kmp-android-03`・`kmp-ios-03`・`maui-ios-27` | **MAUI iOS のみ部分** — 結果は反映され、フェードは出るが、上方向 80pt の移動が出ない (所見3)。他 3 ルートは満たす |
| PB-SM-03 4形態で同一デモが動く | 同じデモ項目・同じ操作で同等の観察結果になる | 上の 6 状態 × 4ルートの表、および各ルートの「通した操作」 | **部分** — デモ項目・操作・文言・静止した見た目は 4 形態で同一。**演出の見え方だけが MAUI iOS で揃わない** (所見3) |

## 未確認 / 申し送り

- **所見3 (MAUI iOS で中身の移動・拡大縮小が効かない) は未解決。** 本体 (`maui/KsDialogs.Maui`) の
  担当範囲のため手を入れていない。直ったあとに `maui-ios-22/23/24/27` を撮り直せば
  PB-SM-01〜03 が 4 ルートで揃う
- **所見4 (`None` 退場の配送順)** は 1 コマだけの観測で再現せず。所見3 の修正時に併せて確認したい
- tasks 8.3 は完了。**8.2 と 8.4 は未完のまま**残した (理由は上の受け入れ表)

---

# 所見3 (MAUI iOS で中身の移動・拡大縮小が効かない) の修正と解消確認 (2026-08-21)

上の「MAUI iOS の撮り直しと 4ルート一致の確認」で残った**所見3**の修正記録。
実行時挙動の検証規約 (cross/conventions) に従い、**修正前に実環境で症状と原因を観測し、
同じ手順で解消を確認**した。Sample のコードは変更していない。

## 環境

| 項目 | 値 |
|---|---|
| MAUI iOS | iPhone 17 Simulator (iOS 26.5 / UDID `F899B356-…`)。`DEVELOPER_DIR=/Applications/Xcode-26.1.1.app/Contents/Developer dotnet build -f net10.0-ios` |
| MAUI Android (退行確認) | Pixel 6a 実機 (API 36 / serial `2A141JEGR18112`)。`dotnet build -f net10.0-android -p:EmbedAssembliesIntoApk=true` |
| 撮影 | `xcrun simctl io … screenshot` / `adb exec-out screencap -p` の連写 (実測 0.27 秒/コマ)。観察用のプリセットは 600 ms |

## 真因 (実機で観測して確定)

**MAUI の iOS 実装は、MAUI の要素ツリーに載っていない (親を持たない) View に
transform を反映しない。** 該当は `Microsoft.Maui.Platform.TransformationExtensions.UpdateTransformation`
で、`Frame` の幅・高さが正かつ **`view.Parent != null`** のときだけ `layer.Transform` を書く。
`Opacity` は別の写し口 (`UpdateOpacity` → `UIView.Alpha`) を通り、この条件を持たない。
これが「透明度だけ動いて位置と大きさが動かない」の形と一致する。

ダイアログの中身は ViewModel ごとの factory が新規生成し、器へ渡すのはその platform view だけなので、
これまで **MAUI 側の親を持たないまま**提示していた。Android の写し口は `View.TranslationX` を直接書き、
この条件を持たないため症状が出なかった。

観測 (一時的な計測コードを入れて撮り、原因確定後に取り除いた。証跡 `maui-ios-transform-probe.log`):

| | 修正前 | 修正後 |
|---|---|---|
| 中身の親 | `null` | `DialogContentHost` |
| MAUI の `TranslationY` (Slide Up 600 ms) | 484 → 460 → 382 → 222 → 81 → 25 → 2 → 0 (**動いている**) | 484 → 456 → 374 → 245 → 93 → 22 → 1 → 0 |
| platform view の `layer.transform` の平行移動 | **(0, 0) のまま**動かない | `TranslationY` と同じ値を追随 |
| Zoom の `layer.transform` の倍率 | **1.00 のまま** | 0.80 → 0.81 → 0.85 → 0.92 → 0.97 → 1.00 |

つまり **MAUI 側のアニメーションは最初から正しく動いており、その値が platform view へ届いていなかった**。
コンテキストで挙がっていた仮説 (器のレイアウトパスが `frame` を置き直して transform を打ち消す) は
**誤り**で、器は `frame` を置き直していない (`center` / `bounds` を使う MAUI の arrange も transform を壊さない)。

## 修正

`maui/KsDialogs.Maui/Internals/DialogContentHost.cs` (新規、internal) — 中身だけを論理上の子として抱える親。
提示 1 回分の入れ物 `DialogPresentationContent` がこの親を持ち、中身を MAUI の要素ツリーへ載せてから
器へ渡す。親はスタイルも BindingContext も持たないため、中身の見た目と結び付きには影響しない。

プリセットは design Decision 3 のまま C# 側の `TranslateTo` / `FadeTo` / `ScaleTo` で、
カスタムフックと同じ 1 経路を通る。公開 API の増減はない。

## 解消確認 (再現時と同一手順)

`Transition Dialog` → プリセットを選ぶ → 時間 600 ms → `表示` → 連写、閉じて連写。

| ファイル | 見えるもの |
|---|---|
| `maui-ios-40-slideup-presentation-fixed.png` | 下辺の外から中身が滑り込む 3 コマ + 静止。修正前 (`maui-ios-22`) は 1 コマ目から最終位置だった |
| `maui-ios-41-slideup-dismissal-fixed.png` | 中身が下辺へ滑り出す 3 コマ。**中身が残っているコマでは結果表示が変わらず**、消えたコマで切り替わる |
| `maui-ios-42-zoom-presentation-fixed.png` | 0.8 倍から等倍へ広がる途中のコマ (中身が小さく半透明)。修正前 (`maui-ios-24`) は透明度だけだった |
| `maui-ios-43-custom-presentation-fixed.png` | 自作フックの入場。**上方向 80pt の移動**が出ている (左から: 出る前 → 移動途中 → 静止)。修正前 (`maui-ios-27`) はフェードだけだった |
| `maui-ios-44-layout-panel-regression-fixed.png` | `Layout Dialog` が従来どおり中央に出る (パネルの初期値 Center / Center / 0 / 0 / ON も変わらず) |
| `maui-android-11-slideup-regression.png` | MAUI Android の `Slide Up` 600 ms。入場で下辺から滑り込み、退場で下辺へ滑り出し、消えたコマで `completed(true)` になる。**退行なし** |

動的モックとの突き合わせで **不一致**だった 3 項目 (スライドの方向・Zoom の倍率・自作フックの移動) は、
いずれも一致に変わった。

## 所見4 (`None` 退場の配送順) の再確認

所見3 の修正後に `None` の退場を 3 回試し、連写でコマを取れたのは 2 回。
**2 回とも中身と覆いが同じコマで消え、そのコマで結果表示が切り替わった** (`maui-ios-45-none-dismissal-recheck.png`。
左 2 コマが 1 回目、右 2 コマが 3 回目)。所見4 の症状は**再現しなかった**。
連写の間隔 (0.27 秒) が `None` の覆いのフェード (既定 250 ms) と同程度で取れるコマが少ないという事情は
変わらないため、断定はできないが、追加の手当ては要らないと判断した。

## テスト

| 対象 | 結果 |
|---|---|
| `cd maui && dotnet test` | 62 件成功 (回帰用に 1 件追加。`PB_MA_05_TheContentIsCarriedInsideTheElementTree`) |
| `cd maui/android/native && ./gradlew :ksdialogs-maui-bridge:test --rerun-tasks` | 15 件成功 |
| `python3 scripts/comment-policy-lint.py` | 禁止 0 件 (540 ファイル) |

---

# MAUI iOS の最終確認 — 方向写像・イージング写像・動的モック突き合わせ (2026-08-21)

tasks 8.2 の MAUI iOS 分 (方向写像・イージング写像・None / Custom Hook の調整無効化) と、
8.4 の MAUI iOS 分 (動的モックの動作イメージとの突き合わせ) を仕上げた記録。

直前に本体の MAUI 側で `FadeTo` / `TranslateTo` / `ScaleTo` を `*Async` へ改名している
(警告除去のみで挙動は同一)。**その本体で Sample を作り直してから**撮り直した。
Sample のコードも本体のコードも、この作業では変更していない (読み取りのみ)。

## 環境

| 項目 | 値 |
|---|---|
| 端末 | iPhone 17 Simulator (iOS 26.5 / UDID `<uuid>`) |
| ビルド | `DEVELOPER_DIR=/Applications/Xcode-26.1.1.app/Contents/Developer dotnet build -f net10.0-ios` (0 警告 / 0 エラー) |
| 起動 | `simctl install …/KsDialogs.Sample.Maui.app` → `simctl launch jp.kamusoft.ksdialogs.samples.maui` |
| 撮影 | `xcrun simctl io … screenshot` の連写。実測 0.27 秒/コマ。観察用のプリセットはすべて 600 ms |

## 読み取り方 (計測の手口)

コマの並びだけでは「どこまで進んだ時点のコマか」が分からないため、次の 2 つを画素から測って添えた。

- **覆いの進み具合** — 中身が決してかからない位置 (画面右端・カードより下、画素 x=1140..1170 / y=2080..2110) の
  白地の暗さ。覆いが最大のとき 255 → 153 になるので、その 102 段階を 0〜100 % として読む。
  覆いは常にライブラリが標準カーブで駆動するため、**プリセットやイージングを変えても同じ時計**になる
- **中身の位置と大きさ** — 覆いが掛かった地の白は 255 未満になるので、**純白 (>=252) の画素の外接矩形**が
  カードそのものになる。透明度が動くプリセット (Zoom / Custom Hook) では純白にならないため、
  中央帯の最大輝度から 3 段以内の画素を明部として測った

カードの静止位置は画素で y=1164..1542 / x=195..1011。`Slide Up` の送り出し量は
`SlideOffset` の定義 (面の高さ − カードの上端) から 486 pt = 1458 px で、開始位置の上端は画面下端 (2622) にあたる。

## 8.2 方向写像 (`Slide` 4 方向)

600 ms / `Standard` で、入場と退場の両方を連写した。**入った辺と同じ辺へ出る**ことも同時に見ている。

| プリセット | 規範表 | 入場のコマ (覆い / カード) | 退場のコマ | 判定 | 証跡 |
|---|---|---|---|---|---|
| `Slide Up` | `from: bottom` | 覆い 16 % で上端 2467 (静止位置 1164 より下 = 下辺の側) | 覆い 45 % で上端 2091 → 下辺へ | 一致 | `maui-ios-60-slideup-direction.png`、既出の `maui-ios-40/41-*-fixed.png` |
| `Slide Down` | `from: top` | 覆い 60 % で上端 696 (静止位置より上 = 上辺の側) | 覆い 55 % で上端 423 → 上辺へ | 一致 | `maui-ios-50-slidedown-direction.png` |
| `Slide Start` | `from: leading (START)` | 覆い 65 % で右端 759 (静止 1009 より左 = 左辺の側。LTR) | 覆い 55 % で右端 521 → 左辺へ | 一致 | `maui-ios-51-slidestart-direction.png` |
| `Slide End` | `from: trailing (END)` | 覆い 28 % で左端 993 (静止 195 より右 = 右辺の側) | 覆い 84 % → 19 % で左端 304 → 1114 と右辺へ | 一致 | `maui-ios-52-slideend-direction.png` |

## 8.2 イージング写像

`Slide Up` / 600 ms を固定して、4 つのイージングで入場を連写した。覆いを時計にして
「その時点でカードがどこまで進んでいるか」を比べている (証跡 `maui-ios-53-easing-comparison.png`)。

覆いは標準カーブ固定なので、覆いの % から時間 t を逆算でき、各イージングの理論値と突き合わせられる。

| イージング | 写像 (Sample) | コマ | 覆い | t (逆算) | 位置の進み (実測) | 同 (理論) |
|---|---|---|---|---|---|---|
| `Accelerate` | `Easing.CubicIn` | 1 | 19.6 % | 0.37 | 3.4 % | 4.9 % |
| `Accelerate` | 〃 | 2 | 84.3 % | 0.66 | 40.8 % | 28.8 % |
| `Standard` | `Easing.CubicInOut` | 1 | 15.7 % | 0.34 | 10.6 % | 15.7 % |
| `Standard` | 〃 | 2 | 76.5 % | 0.61 | 87.7 % | 76.5 % |
| `Linear` | `Easing.Linear` | 1 | 15.7 % | 0.34 | 29.0 % | 34.0 % |
| `Linear` | 〃 | 2 | 87.3 % | 0.68 | 76.2 % | 68.3 % |
| `Decelerate` | `Easing.CubicOut` | 1 | 54.9 % | 0.52 | 91.1 % | 88.7 % |

**進み方が変わることは明確に見える。** 前半 (t<0.5) は `Decelerate` > `Linear` > `Standard` > `Accelerate`、
後半は `Decelerate` > `Standard` > `Linear` > `Accelerate` の順に先行し、これは 4 つの曲線の形そのものである。
特に覆いが 8 割方進んだ時点で、`Accelerate` はまだ 4 割しか動いていないのに `Standard` は 9 割近い。

実測が理論よりやや先行する傾向があるが、連写 1 コマの取得に 0.27 秒かかることと、
覆い (ネイティブ側が駆動) と中身 (MAUI 側が駆動) の開始時刻が厳密には揃わないことで説明がつく。
**厳密な数値一致は目的ではなく、写像が入れ替わっていないことを見るのが目的**なので、この差は問題にしない。

## 8.2 `None` / `Custom Hook` の調整無効化

証跡 `maui-ios-55-adjust-disabled.png`。

| 観察 | 結果 |
|---|---|
| `None` を選ぶと `時間` / スライダ / `イージング` / 4 チップが薄く (不透明度 0.4) なる | 一致 |
| 薄い状態のまま `Linear` を押し、スライダを左端まで引いても値が変わらない (600 ms / `Standard` のまま) | 一致 (操作不可・値は保持) |
| `Custom Hook` でも同じ無効表示になり、値が保持される | 一致 |

## `Fade` の再確認 (改名後の本体で)

`Fade` は方向も倍率も持たないため上の 2 つの表には出てこないが、改名後の本体で撮り直した
(証跡 `maui-ios-59-fade-in-out.png`)。入場は覆い 80 % の時点で中身の輝度 247 (半透明) → 255 (不透明)、
退場は覆い 40 % の時点で 227 と薄れており、**中身が見えているコマでは結果表示が直前の値のまま**である。
以前の記録 (`maui-ios-20/21`) と同じ見え方で、退行はない。

## 8.4 動的モック (`ui/mock/mock-preset-motion.html`) との突き合わせ

| 動的モックが見せる動作イメージ | MAUI iOS | 証跡 |
|---|---|---|
| スライドは入った辺と同じ辺へ出る | 一致 (4 方向すべて) | `maui-ios-50/51/52`、`maui-ios-40/41-*-fixed.png` |
| `Zoom` は 0.8 倍から等倍へ、退場は 0.8 倍へ | 一致 (入場は幅 690 = 0.85 倍のコマ → 812 = 1.00 倍。退場は 0.96 倍 → 0.94 倍と縮みながら薄れる) | `maui-ios-54-zoom-in-out.png` |
| `Custom Hook` は上方向 80pt 移動 + フェード、退場はフェード | 一致 (入場は静止位置の 51pt 下から薄い状態で上がってくる。退場は位置を変えずに薄れるだけ) | `maui-ios-57-custom-hook-motion.png`、既出の `maui-ios-43-*-fixed.png` |
| `None` は中身の待ちなし、覆いはフェード | 一致 (覆いが 28 % のコマで中身は既に静止位置・不透明) | `maui-ios-56-none-order.png` |
| 中身が消えたコマで初めて結果表示が切り替わる | 一致 (全プリセットで、中身が見えているコマは直前の結果のまま) | 上記すべて。特に `maui-ios-56-none-order.png` |

**`Zoom` の退場だけは 0.8 倍に到達した瞬間のコマが撮れていない** — 到達と同時に中身が撤去されるため、
0.27 秒間隔の連写では原理的に取りにくい。2 回試して撮れたのは 0.96 倍と 0.94 倍のコマで、
どちらも「縮みながら薄れる」向きは一致している。入場側で 0.8 倍から始まることが撮れているため、
倍率の写像そのものは確認できたと判断した。

**動的モックは参考であり受け入れ基準ではない。** 受け入れ基準は dialog-contract の Scenario (順序と完了待ち) で、
そちらは本体側のテストが固定している。

## 所見4 (`None` 退場の配送順) の 3 度目の確認

`maui-ios-56-none-order.png` の 4 コマ目は、**覆いがほぼ消えたのに中身がまだ見えているコマ**で、
そこでは結果表示が**直前の値のまま**である。次のコマで中身が消え、そこで結果が切り替わる。
所見4 で 1 度だけ撮れた「中身が残っているのに結果が切り替わっている」コマは、今回も再現しなかった。
**追加の手当ては不要と判断する** (所見3 の修正後の再確認と合わせて 3 度目)。

## 既存デモの退行確認 (改名後の本体で)

証跡 `maui-ios-58-regression-after-rename.png`。`Basic Dialog` が従来どおり出て、
`Layout Dialog` の属性調整パネルの初期値 (Center / Center / 0 / 0 / ON) も変わっていない。
Transition デモ画面の初期状態 (`Fade` / 250 ms / `Standard`) も規範表どおり。

## PB-SM-01〜03 の受け入れと証跡の対応 (確定版)

前節「MAUI iOS の撮り直しと 4ルート一致の確認」の同名の表は、所見3 が未解決だった時点のもので、
**この表が置き換える**。

| Scenario | 受け入れの中身 | 証跡 | 判定 |
|---|---|---|---|
| PB-SM-01 プリセットを選んで表示できる | 選択したプリセットの演出 (時間・イージングは規範表の写像どおり) で表示・閉鎖され、結果が結果エリアに出る。`None` 選択時は時間・イージングが無効表示 | 4ルートの 6 状態 (`ui/verification/`)、Native / KMP の連写、MAUI Android の `maui-android-01/04/05/10/11`、MAUI iOS の `maui-ios-25/40/41/42` と `maui-ios-50〜56`・`maui-ios-59` | **満たす (4ルート)** — 方向写像 4 方向・イージング写像 4 種・`None` の無効表示まで MAUI iOS で確認済み |
| PB-SM-02 カスタムフックの実演 | 自作フックによる演出で表示・閉鎖され、結果が表示に反映される | `ios-03/04`・`android-03/04`・`maui-android-09`・`kmp-android-03`・`kmp-ios-03`・`maui-ios-43-*-fixed` と `maui-ios-55/57` | **満たす (4ルート)** — MAUI iOS の上方向 80pt 移動 + フェード (入場) とフェードのみ (退場) を確認済み |
| PB-SM-03 4形態で同一デモが動く | 同じデモ項目・同じ操作で同等の観察結果になる | 6 状態 × 4ルートの表、各ルートの「通した操作」、および本節の動的モック突き合わせ | **満たす** — デモ項目・操作・文言・静止した見た目に加えて、**演出の見え方も 4 形態で揃った** |

## 未確認 / 申し送り

- `Zoom` 退場の 0.8 倍到達コマは連写の間隔の都合で未取得 (上記のとおり、写像は入場側で確認済み)
- 実測が理論よりやや先行する件は、覆いと中身の駆動元が別 (ネイティブ / MAUI) であることによる開始時刻の
  ずれと考えているが、**数値としては追い込んでいない**。契約 (順序と完了待ち) は本体側のテストが固定しており、
  ここで見ているのは見え方なので、追い込む必要はないと判断した
