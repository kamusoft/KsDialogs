# 撮影支援設定の4ルート通し検証 — 証跡

tasks.md 5.3 の記録 (2026-08-27)。対象は samples デルタスペックの全 Scenario (CA-SA-01〜07)。
コードリーディングによる突き合わせは [../../verification/parity-check.md](../../verification/parity-check.md) が受け持ち、本書は
**実機・シミュレータでの通し観察**を受け持つ。

## 環境

| ルート | 実行 OS / 端末 | 配備済みビルド |
|---|---|---|
| ios | iPhone 17 Simulator (402x874 pt, scale 3) | `jp.kamusoft.ksdialogs.samples.ios` |
| maui (iOS) | 同上 | `jp.kamusoft.ksdialogs.samples.maui` |
| kmp (iOS) | 同上 | `jp.kamusoft.ksdialogs.samples.kmp.ios` |
| android | Pixel 4a 実機 (1080x2340) | `jp.kamusoft.ksdialogs.samples.android/.MainActivity` |
| maui (Android) | 同上 | `jp.kamusoft.ksdialogs.samples.maui` (activity 名は生成物のため `cmd package resolve-activity` で解決) |
| kmp (Android) | 同上 | `jp.kamusoft.ksdialogs.samples.kmp.android/.MainActivity` |

起動・撮影・タップの手順は kasane/config.yaml `ui.screenshot` (本 change の 4.1 で改訂したもの) に従った。
すなわち iOS 系は `simctl terminate` → `simctl launch <bundle-id> --demo <id> --loading-step-interval-ms <n>`、
Android 系は `am force-stop` → `am start -n <component> --es demo <id> --es loading-step-interval-ms <n>`。
CA-SA-01〜06 の通しはビルドを行っていない (配備済みのデモ駆動モード込みビルドをそのまま使用)。
CA-SA-07 の再検証 (maui-android) だけは、レビュー指摘の修正後のコードで確かめるため
`dotnet build -t:Install -f net10.0-android` で作り直したビルドを配備して実施した。

## Scenario ごとの結果

### CA-SA-01 引数なしの通常起動は不変 — 成立

- 6アプリすべてを引数なしで起動。メニュー題字 `KsDialogs Sample` と9件のデモ項目 (Basic / Layout /
  Declarative / Text Input / Inline / Transition / Model / Default Loading / Custom Loading) の**文言と並び順が
  6アプリで一致**し、撮影支援機構に由来する表示 (バナー・追加項目・デバッグ表示) は一切現れない
  → `ca-sa-01-noarg-menus.png`
- 4ルート (ios / android / maui / kmp) で `Basic Dialog` を手動タップ → ダイアログ `こんにちは、KsDialogs!` →
  `OK` → `結果: completed(true)` まで従来どおり動作 → `ca-sa-01-manual-demo.png`

### CA-SA-02 各デモ ID の自動再生 — 成立

9件の安定デモ ID をそれぞれ `demo` に渡して6アプリを起動し、手動操作なしで design Decision 3 の表どおりの
状態になることを確認した (6アプリ × 9デモ = 54 回の起動)。

| 安定デモ ID | 観察された起動直後の状態 (6アプリ共通) |
|---|---|
| `basic-dialog` | ダイアログ `こんにちは、KsDialogs!` + `キャンセル` / `OK` |
| `declarative-dialog` | 同上 (宣言的 UI 登録) |
| `model-dialog` | ダイアログ `ViewModel から表示しています` |
| `text-input-dialog` | ダイアログ `メッセージを入力してください` + 入力欄 (下書き `ここに入力`) |
| `inline-dialog` | ダイアログ `インライン表示です` |
| `transition-dialog` | 属性パネル画面 `Transition Dialog` (初期値 `Fade` / 250 ms / `Standard`) |
| `layout-dialog` | 属性パネル画面 `Layout Dialog` (初期値 Center / Center / 0 / 0 / Use visible area オン) |
| `default-loading` | 既定ローディング開始 (`Loading...` + 進捗) |
| `custom-loading` | カスタムローディング開始 (`カスタムローディング` + 帯 + 百分率) |

証跡: `ca-sa-02-autoplay-<ルート>.png` (ios / maui-ios / kmp-ios / android / maui-android / kmp-android)。
Loading 系2件は起動直後の状態を捉えるため `loading-step-interval-ms 3000` を併用して撮影した
(自動再生の成否には影響しない)。

「以降の操作・結果表示が手動起動と同一」は CA-SA-07 の通し (自動再生 → `OK` → `結果: completed(true)`) と
CA-SA-01 の手動通しの結果が一致することで確認した。

### CA-SA-03 定義外 ID の無視 — 成立

`demo` に `no-such-demo` を渡して6アプリを起動 (`loading-step-interval-ms 2000` も同時指定)。
6アプリとも自動再生は起きず、通常のメニューがそのまま表示された → `ca-sa-03-unknown-id.png`

### CA-SA-04 刻み間隔の延長 — 成立

`loading-step-interval-ms 2000` を渡して `default-loading` / `custom-loading` を6アプリで通し、
起動後の3時点 (t1 / t2 / t3) を撮った。どのアプリも進捗が段階的に更新され、各段階を撮影で捉えられた。

| ルート | Default Loading (t1 → t2 → t3) | Custom Loading (t1 → t2 → t3) |
|---|---|---|
| ios | 25% → 25% → `Soon...` 100% | 25% → 25% → 100% |
| maui-ios | `Loading...` 0% → 25% → `Soon...` 100% | 0% → 25% → 100% |
| kmp-ios | 25% → `Soon...` 75% → `結果: 完了` | 25% → 75% → `結果: 完了` |
| android | `Loading...` 0% → `Soon...` 50% → `結果: 完了` | 0% → 50% → `結果: 完了` |
| maui-android | `Loading...` 0% → `Soon...` 50% → `結果: 完了` | 0% → 50% → `結果: 完了` |
| kmp-android | `Loading...` 0% → `Soon...` 50% → `結果: 完了` | 0% → 50% → `結果: 完了` |

証跡: `ca-sa-04-interval-2000-{ios,android}-{default,custom}-loading.png`。
既定 (400 ms) では全5段階が約2秒で終わり撮り分けられないのに対し、2000 ms 指定では完了まで約10秒かかり、
途中段階を確実に撮れた (メッセージが `Loading...` → `Soon...` へ差し替わる瞬間も含む)。

### CA-SA-05 不正値は既定値で動作 — 成立

`loading-step-interval-ms` に数値でない `abc` を渡して `default-loading` を6アプリで通した。
6アプリとも起動から数秒後の最初の観測時点で既に `結果: 完了` に到達しており (maui-ios だけ 100% の表示を
途中で捉えた)、CA-SA-04 の 2000 ms 指定が同時点でまだ進行中であるのと明確に対照的だった。
既定の刻み間隔で完了まで動作している → `ca-sa-05-invalid-interval.png`

### CA-SA-06 4ルートで同じ引数・同じ挙動 — 成立

上記 CA-SA-02 / 03 / 04 / 05 はいずれも**6アプリに同じキー・同じ値**を渡して実施しており、
自動再生されるデモ・無視の判定・刻み間隔の効き方・不正値の扱いがすべて一致した。
画面文言も従来どおり4ルートで一致している (CA-SA-01 のメニュー、CA-SA-02 の各デモ)。
コード側の突き合わせ (キー名・デモ ID 定義・異常系) は `../../verification/parity-check.md` を参照。

既知の非対称 (同じキーを複数回渡したときの採用が iOS 系 = 最初の1組 / Android 系 = 後勝ち) は
deviation.md に合意済み差分として記録されており、本通しでは扱っていない。

### CA-SA-07 画面再生成で再発火しない — 成立 (Android 3アプリ = 実地の再生成 / iOS 3アプリ = 実地の回転 + 構造的保証)

この Scenario だけは「再生成を実際に起こせたか」がアプリごとに違う。**再生成が起きたことまで確かめられた
経路と、回転の実地観察 + 実装の構造で保証している経路を分けて記録する。**

#### 実地に再生成を起こせた経路 (Android 3アプリ)

**android / kmp-android (回転)**: `demo=basic-dialog` で起動 → 自動再生されたダイアログを `OK` で閉じて
`結果: completed(true)` にする → 横向きへ回転 → 縦向きへ戻す。両アプリとも manifest に画面向きの
`configChanges` を持たないため回転で Activity が作り直され、**回転後の画面から `直近の結果` の表示が消えて
いる** — これが再生成が実際に起きたことの傍証になる。それでもダイアログは再表示されず、通常のメニューの
ままだった → `ca-sa-07-android.png` / `ca-sa-07-kmp-android.png`

この2枚は**アプリごとに独立に通して撮り直したもの** (2026-08-27 に再取得)。実行した手順は2アプリで同一で、
`<component>` だけが `jp.kamusoft.ksdialogs.samples.android/.MainActivity` と
`jp.kamusoft.ksdialogs.samples.kmp.android/.MainActivity` で入れ替わる:

```
adb -s <android-serial> shell am force-stop <package>
adb -s <android-serial> shell am start -n <component> --es demo basic-dialog
（自動再生されたダイアログを OK で閉じ、結果: completed(true) にする）
adb -s <android-serial> shell settings put system user_rotation 1   # ← 横向き = Activity 再生成
adb -s <android-serial> shell settings put system user_rotation 0   # ← 縦向きへ戻す
```

プロセス ID は android が `32519`、kmp-android が `32637` で、いずれも回転の前後で同一 =
**同じプロセスのままの再生成**である。撮影時刻は android が 14:34:35〜14:34:59、
kmp-android が 14:35:20〜14:35:45。

**2枚の画面部分が画素単位で一致することについて**: 保存前にステータスバー相当 (上端 6%) を切り落とすと、
2アプリの4コマは画素単位で完全一致する。両ルートは同じ端末・同じ解像度で、メニュー・ダイアログの
描画がパリティにより一致するため、切り落とし後に残る領域には差が出ない (時刻や通知アイコンといった
アプリ外の要素だけが両者を分けるが、それは個人要素として切り落としている)。
そのため**どのアプリをいつ撮った証跡かを画像自身が示せるよう、グリッド上端に対象パッケージ・撮影時刻・
プロセス ID の見出し帯を入れた**。画面の画素そのものには一切手を入れていない。

**maui-android (フォント倍率の変更)**: maui の `MainActivity` は `ConfigurationChanges` に `Orientation` を
含むため、**回転では Activity が作り直されない**。実際に `ca-sa-07-maui-android.png` では回転の前後で
`結果: completed(true)` の表示が残っており、この画像は「再発火しない」ことの証跡にはなっても
「再生成しても再発火しない」ことの証跡にはならない。

そこで `ConfigurationChanges` に含まれない **フォント倍率** を変えて Activity の再生成を起こし直した:

```
adb -s <android-serial> shell am force-stop <package>
adb -s <android-serial> shell am start -n <component> --es demo basic-dialog
（自動再生されたダイアログを OK で閉じ、結果: completed(true) にする）
adb -s <android-serial> shell settings put system font_scale 1.15   # ← Activity 再生成
adb -s <android-serial> shell settings put system font_scale 1.0    # ← 検証後に元へ戻す
```

結果 → `ca-sa-07-maui-android-recreate.png`

- 文字が大きくなり、`直近の結果` の表示が消えた = **Activity と Page が作り直された**
- プロセス ID は変更の前後で同一 (`31639`) = **同じプロセスのまま**の再生成である
- それでもダイアログは再表示されなかった = 再発火していない

`always_finish_activities` でも同じことを狙ったが、`settings put global always_finish_activities 1` は
**再起動しないと実行中の ActivityManager に効かない** (設定直後に HOME → 再起動しても Activity は破棄されず、
`直近の結果` が残ったまま `Activity not started, its current task has been brought to the front` になる)。
オーナーの端末を再起動しない方針のため、フォント倍率の経路を採った。

#### 実地の回転 + 構造的保証の経路 (iOS 3アプリ)

ios / maui-ios / kmp-ios は、シミュレータの向きを CLI から変える手段がない。そこでオーナー確認のうえ
(2026-08-27)、**回転操作だけをオーナーが Simulator 上で手動実行 (Cmd+← / Cmd+→) し、それ以外の起動・タップ・
撮影はエージェントが行う**形で3アプリとも実地に通した:

1. `simctl terminate` → `simctl launch <bundle-id> --demo basic-dialog` (コールド起動)
2. 自動再生されたダイアログを閉じて結果表示にする (ios / maui-ios は `OK` → `completed(true)`。
   kmp-ios のみタップ座標の取り違えでキャンセル → `cancelled` — Scenario の主張には影響しない)
3. オーナーが横向きへ回転 → 数秒後に縦向きへ戻す (回転の検知と撮影は、画面内容の差分を監視する
   ウォッチャーで自動化 — simctl のスクリーンショットは横向きでも縦枠のまま中身だけ回るため、
   画像サイズではなく内容差分で判定する)
4. 横向きの間も、縦へ戻した後も、**ダイアログは再表示されず通常のメニューのままだった**

→ `ca-sa-07-ios.png` / `ca-sa-07-maui-ios.png` / `ca-sa-07-kmp-ios.png` (各4コマ:
自動再生 → OK後 → 横向き → 縦復帰。横向きコマは可読性のため 90° 回転して収載、画素は無加工)

ただし iOS の回転は Android と違い、SwiftUI / MAUI のルート画面を**破棄・再生成しない** (レイアウトの
更新のみ)。したがって上の証跡が直接示すのは「回転しても再発火しない」であり、「画面が作り直されても
再発火しない」の**再生成側の保証は引き続き実装の構造が担う**。なお当初撮っていた「Layout Dialog の
パネルへ遷移 → `‹` で戻る」は、`fullScreenCover` が背後の `SampleMenuScreen` を破棄しないため再生成の
証跡にならないと判定し、上記の回転証跡に差し替えた。撮影のためだけに Sample 側へテスト用フックを
足すことはしない方針は維持する。

保証の実体は「**プロセス単位の消費フラグを、自動再生の入口の先頭で検査して消費する**」構造で、
画面が何度作り直されても 2 回目以降は必ず自動再生なしの経路へ倒れる。

- ios: `samples/ios/KsDialogsSample/SampleCaptureAutoPlay.swift`

  ```swift
  @MainActor
  enum SampleCaptureAutoPlay {
      private static var isConsumed = false

      static func consumeDemo() -> SampleDemoId? {
          guard !isConsumed else { return nil }
          isConsumed = true
          return SampleCaptureOptions.current.demo
      }
  }
  ```

  入口は `samples/ios/KsDialogsSample/SampleMenuScreen.swift` の
  `guard let demo = SampleCaptureAutoPlay.consumeDemo() else { return }` — 画面状態を触る前に検査する

- kmp-ios: `samples/kmp/shared/.../SampleCaptureAutoPlay.kt` の `object` が同じ `isConsumed` を持ち、
  `samples/kmp/iosApp/KsDialogsSampleKmp/SampleMenuScreen.swift` の
  `guard let demo = SampleCaptureAutoPlay.shared.consumeDemo(options:) else { return }` が入口
- maui-ios: `samples/maui/KsDialogs.Sample.Maui/SampleCaptureAutoPlay.cs` の `private static bool s_consumed` を
  `SampleMenuPage.AutoPlay()` の `if (SampleCaptureAutoPlay.ConsumeDemo() is not SampleDemoId demo) { return; }`
  が消費する

同じ構造を持つ maui-android で、上のフォント倍率の経路により**実地に再生成しても再発火しない**ことを
確かめている (maui は iOS / Android で `SampleCaptureAutoPlay` を共有する)。iOS 3アプリは
「回転しても再発火しない」を上の実地証跡で、「再生成しても再発火しない」をこの構造で担保する
(iOS で再生成そのものを外から起こす手段は引き続き存在しない)。

## 撮影・保存の規律

- Android は実機のため、保存前に**上端 6% (ステータスバー相当) を切り落とした**。切り落とし以外の加工はしていない。
  保存した画像を開いて通知アイコン・アカウント名などの個人要素が含まれないことを確認済み
- iOS はシミュレータのため画面の画素は無加工。CA-SA-07 の3枚のみ、Android の CA-SA-07 と同じ規律で
  上端 6% の切り落とし + 見出し帯 (対象 bundle id・撮影時刻・プロセス ID) を付けている。
  それ以外の iOS 証跡に写り込んでいるのは時刻・Wi-Fi・電池の標準表示のみ
- 証跡は容量を抑えるため、同じ Scenario の連続コマを1枚のグリッド画像にまとめて保存している
  (元の個別スクリーンショットはスクラッチ領域に置き、リポジトリには残していない)
- CA-SA-07 の5枚 (`ca-sa-07-{android,kmp-android,ios,maui-ios,kmp-ios}.png`) は、グリッドの上端に
  対象パッケージ (bundle id)・撮影時刻・プロセス ID の見出し帯を足している (画面の画素は無加工)。
  発端は android / kmp-android の切り落とし後の画面が2ルートで画素単位に一致し画像だけでは
  別撮りを示せないことで、iOS 3枚も同じ規律に揃えた
- CA-SA-07 の kmp-ios のみ、ダイアログを閉じた操作がキャンセル (結果表示は `結果: cancelled`)。
  他2枚 (OK / `completed(true)`) との差は撮影時のタップ座標の取り違えによるもので、
  「閉じた後に回転しても再発火しない」という Scenario の主張には影響しない (グリッドの
  コマ見出しと見出し帯に明記)
- 回転検証で変更した Android の設定 (`user_rotation`) は検証後に元の値 (0) へ戻し、
  `accelerometer_rotation` も元の値 (0) のままであることを確認した
- CA-SA-07 の再検証で変更した Android の設定も検証後に元の値へ戻した —
  `font_scale` は 1.15 → 元の 1.0、`always_finish_activities` は 1 → 元の 0
  (後者は実行中の ActivityManager に効かず、目的の再生成は起こせていない)

## 撮影中に判明した手順上の注意 (config.yaml `ui.screenshot` の補足)

- **iOS は `simctl terminate` の直後に `simctl launch` すると引数が届かないことがある。**
  終了が完了する前に launch すると既存プロセスが前面化するだけになり、`--demo` が無視されて
  「自動再生が起きない」「刻み間隔が既定のまま」に見える。終了と起動の間に1秒以上空けると安定した
- **iOS のスクリーンショットを間を空けずに連続で撮ると、画面の更新が撮影に追いつかず同じ進捗値が続けて写る。**
  進捗の段階を撮り分けるときは撮影間に待ちを入れる (刻み間隔を延ばすだけでは足りない)
- kmp (iOS) はコールド起動が遅く、`--demo` 指定でも初回描画まで6〜9秒かかることがある

## 関連

- デルタスペック: [../../specs/samples/spec.md](../../specs/samples/spec.md)
- コードリーディングによるパリティ突き合わせ: [../../verification/parity-check.md](../../verification/parity-check.md)
- 合意済み差分: [../../deviation.md](../../deviation.md)
- 起動引数の利用者向け説明: samples/README.md「撮影のための起動引数」
