# 4ルートのパリティ通し — 証跡 (tasks 6.3)

2026-08-27 実施。対象は samples デルタスペックの Requirement 5件 (Scenario TS-SA-01〜06) と、
実提示でしか判定できない TS-MX-05 (Loading 前面) / TS-NM-01 (タッチ素通し)。

判定の様式:
- **Loading 前面規則 (TS-MX-05)** は「Loading 表示中のフレーム」で判定する
  (Loading の覆いの下に Toast が沈んで見えることが前面である証拠)
- **タッチ素通し (TS-NM-01)** は「背後要素の状態変化」で判定する
  (Toast の面をタップし、その真下のメニュー行が反応したか・Toast が消えていないかを操作前後で撮る)
- Toast は時間で消えるため、進捗と消滅の撮り分けは連写のコマから選ぶ

## 環境

| ルート | 実行 OS / 端末 | ビルドと投入 |
|---|---|---|
| ios | iPhone 17 Simulator | `samples/ios` を xcodebuild → `simctl install` / `simctl launch jp.kamusoft.ksdialogs.samples.ios` |
| android | Pixel 4a 実機 (1080x2340) | `samples/android` の `:app:assembleDebug` → `adb install -r` / `am start` |
| maui (Android) | Pixel 4a 実機 | `samples/maui/KsDialogs.Sample.Maui` を `dotnet build -f net10.0-android -p:EmbedAssembliesIntoApk=true` → `adb uninstall` してから `adb install -r` |
| maui (iOS) | iPhone 17 Simulator | `samples/maui/KsDialogs.Sample.Maui` を `MD_APPLE_SDK_ROOT=/Applications/Xcode-26.1.1.app dotnet build -f net10.0-ios -p:RuntimeIdentifier=iossimulator-arm64` → `simctl install` / `simctl launch jp.kamusoft.ksdialogs.samples.maui` |
| kmp (Android) | Pixel 4a 実機 | `samples/kmp` の `:androidApp:assembleDebug` → `adb install -r` / `am start` |
| kmp (iOS) | iPhone 17 Simulator | `samples/kmp/iosApp` を xcodebuild → `simctl install` / `simctl launch` |

結果表示の見切れ (後述) の切り分けだけ、android ルートを **Pixel 6a 実機 (1080x2400)** でも通した。

**maui (iOS) の「未実施 (環境要因)」は誤診だった (2026-08-27 に判明・実施済みへ改めた)**:
初回の通しでは「.NET for iOS のワークロードが要求する Xcode と導入済み Xcode が食い違うため
ビルドが通らない」と記録したが、実際には Xcode 26.1.1 が `/Applications/Xcode-26.1.1.app` に
導入済みで、**環境変数 `MD_APPLE_SDK_ROOT` にその Xcode を指定すればビルドは通る**。
`xcode-select` などのシステム設定は変更していない。同日中に iPhone 17 Simulator で通しを実施し、
下の結果表の maui (iOS) 列を実測で埋めた。

## 使用したコマンド (代表)

デモの起動は samples/README.md「撮影のための起動引数」の安定デモ ID で行う。

```
# maui(iOS) のビルドと投入 (MD_APPLE_SDK_ROOT が必須)
MD_APPLE_SDK_ROOT=/Applications/Xcode-26.1.1.app \
  dotnet build samples/maui/KsDialogs.Sample.Maui/KsDialogs.Sample.Maui.csproj \
  -f net10.0-ios -p:RuntimeIdentifier=iossimulator-arm64
xcrun simctl install <UDID> \
  samples/maui/KsDialogs.Sample.Maui/bin/Debug/net10.0-ios/iossimulator-arm64/KsDialogs.Sample.Maui.app

# iOS / maui(iOS) / kmp(iOS)
xcrun simctl terminate <UDID> <bundle-id> ; sleep 2 ; \
xcrun simctl launch <UDID> <bundle-id> --demo default-toast
xcrun simctl io <UDID> screenshot <一時パス>        # 連写して変化のあったコマを選ぶ

# Android / maui(Android) / kmp(Android)
adb -s <serial> shell am force-stop <package> ; sleep 1 ; \
adb -s <serial> shell am start -n <package>/<activity> --es demo default-toast
adb -s <serial> exec-out screencap -p > <一時パス>
adb -s <serial> shell input tap <x> <y>             # TS-NM-01 と、自動再生が撮影に間に合わない場合の起動
```

デモの自動再生は1プロセス1回だけのため、撮り直しは毎回アプリを終了してから起動し直した。
KMP (iOS) と KMP / MAUI の Android は起動が遅く、既定 duration 1500 ms の `Default Toast` が
起動アニメーション中に消えてしまうことがある。その場合は通常起動してからメニュー行を
タップした (自動再生と同じ入口を呼ぶため、観察対象は変わらない)。

## ルート × デモの結果

| Scenario / 観察点 | ios | android | maui (Android) | maui (iOS) | kmp (Android) | kmp (iOS) |
|---|---|---|---|---|---|---|
| メニューに Toast 5項目が Loading の後ろへ並ぶ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| TS-SA-01 `Default Toast` — `Hello Toast!` が契約既定配置に出て時間で消える | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| TS-SA-02 `Custom Toast` — `カスタムトースト` (登録) + `インライントースト` (インライン) の2枚が重なる | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| TS-SA-03 `Toast Stack` — 3枚が起動順に重なり、3枚目の長文が複数行に折り返して高さが伸びる | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| TS-SA-03 duration の短いものから独立に消える (Toast 1 が先に消え 2・3 が残る) | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| TS-SA-04 `Toast Placement` — `Placed Toast` が上部中央に出る (配置上書き) | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| TS-SA-05 `Toast Overlap` — Toast 表示中に Dialog が共存する | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| TS-MX-05 Loading 表示中のフレームで Toast が Loading の覆いの下にある | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| TS-SA-05 Loading 終了後も Toast が残っている | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| TS-SA-05 Toast 消滅後に `結果: 完了` が出る | ✓ | ✓ (注) | ✓ (注) | ✓ (注) | ✓ (注) | ✓ |
| TS-NM-01 Toast の面をタップすると背後のメニュー行が反応し、Toast は消えない | ✓ | ✓ | (未実施) | (未実施) | (未実施) | (未実施) |

`(未実施)` は他ルートで成立を確認済みのため省いたもの。環境要因による未実施はもう無い
(maui (iOS) は 2026-08-27 に実施済み)。

**TS-SA-06 (4ルートで同一デモが動く) の判定: 成立**。maui (iOS) を含めた**4ルート6実行経路**で
メニュー文言 (`Default Toast` / `Custom Toast` / `Toast Stack` / `Toast Placement` / `Toast Overlap`)・
表示内容・重なり・結果表示が一致した。残る差は下の「観察できたルート間の差」に挙げた実装経路由来の
ものだけで、契約が保証しない範囲に収まる。

### (注) 結果表示が端末の高さによって見切れていた件 — メニューのスクロール化で解消 (2026-08-27)

**初回の通しで観測した現象**: Android の3ルートを **Pixel 4a (1080x2340)** で通したとき、
`Toast Overlap` 完了後に結果表示エリアの見出し `直近の結果` までは出るが、値の `結果: 完了` が
**画面の下端より下へ押し出されて見えない** (uiautomator の階層ダンプでも値の TextView が現れない)。
同じ APK を **Pixel 6a (1080x2400)** で通すと収まって見えた
(`android-13-overlap-result-pixel6a.png` — 修正前に切り分けで撮ったもの)。
原因は Sample のメニューがスクロールしない縦積みで、デモ項目が 9 → 14 に増えた分だけ
可視領域を超えていたこと (**Toast 本体の挙動ではなく Sample 画面の問題**)。

**対応**: オーナー判断 (2026-08-27「スクロール同梱」・`deviation.md` に記録) により、
4ルートのメニューを**見出しを固定し、その下 (デモ項目の一覧 + 結果表示エリア) を
スクロールできる容器に入れる**構成へ変更した。既にある `Transition Dialog` のデモ画面と同じ構成で、
容器は Android 系 = `ScrollView`、MAUI = `ScrollView` (`Grid RowDefinitions="Auto,Auto,*"` の行3)、
iOS 系 = `ScrollView`。

**再確認 (2026-08-27・修正後)**:

| ルート | 端末 | 結果 |
|---|---|---|
| android | Pixel 4a (1080x2340) | 下端まで送ると `直近の結果` / `結果: 完了` が完全に見える (`android-10-overlap-result.png`) |
| maui (Android) | Pixel 4a | 同上 (`maui-android-10-overlap-result.png`) |
| kmp (Android) | Pixel 4a | 同上 (`kmp-android-10-overlap-result.png`) |
| ios | iPhone 17 Simulator | 14 項目 + 結果表示がスクロールなしで収まり、`結果: 完了` まで見える (`ios-10-overlap-result.png`) |
| kmp (iOS) | iPhone 17 Simulator | 同上 (`kmp-ios-10-overlap-result.png`) |
| maui (iOS) | iPhone 17 Simulator | 一覧を下端まで送ると `直近の結果` / `結果: 完了` が完全に見える (`maui-ios-10-overlap-result.png`)。MAUI は iOS 系でも行の高さが大きく、14 項目 + 結果表示がスクロールなしでは収まらないため、Android 系と同じく送ってから撮った |

したがって TS-SA-05 / TS-SA-06 の結果表示は、短い端末を含めて**4ルート6実行経路すべて**で確認できる。
連番 10 の画像は5つの実行経路をこの修正後に撮り直し、maui (iOS) は 2026-08-27 の
再通しで新たに撮った。

## 観察できたルート間の差 (仕様差ではない)

- **Dialog と Toast の前後関係**: iOS 系3経路 (ios / maui (iOS) / kmp (iOS)) では Toast が
  Dialog の覆いの下に沈んで見え、Android 系3経路では覆いの上に出る。dialog-contract デルタスペックは
  「Dialog と Toast の前後関係は契約で保証しない (core/ADR-0006 の線)」としており、規約どおりの差
- **既定ローディングの回し物の形**: iOS はスポーク状、Android は円弧。OS 標準の不定進捗表示を
  そのまま使う設計の帰結 (add-loading の通しでも同じ差が記録されている)
- **覆いの外側の地の色**: MAUI Android だけ画面上端がテンプレート由来の帯色になる。
  本変更の前後で変わっていない既存の見た目

## 撮影・保存の規律

- Android は実機のため、保存前に**上端のステータスバーを切り落とした** (Pixel 4a は 136 px、Pixel 6a は 140 px)。
  切り落とし以外の加工はしていない。保存後にすべて開き、通知・アカウント名・端末名・位置情報などの
  個人要素が写っていないことを確認済み
- iOS はシミュレータのため無加工。写り込んでいるのは時刻・Wi-Fi・電池の標準表示のみ
- 連写の中間コマは残していない (各状態につき1枚だけを本ディレクトリに置いた)

## ファイル

`<ルート>-<連番>-<状態>.png`。ルートの接頭辞は `ios` / `android` / `maui-android` /
`maui-ios` / `kmp-android` / `kmp-ios`。連番の意味はルート間で共通:

| 連番 | 状態 |
|---|---|
| 01 | `Default Toast` 表示中 (`Hello Toast!`) |
| 02 | `Default Toast` 消滅後 |
| 03 | `Custom Toast` — カスタム View 2枚の重なり |
| 04 | `Toast Stack` — 3枚 (3枚目は複数行) |
| 05 | `Toast Stack` — Toast 1 が消え 2・3 が残る |
| 06 | `Toast Placement` — 上部中央 |
| 07 | `Toast Overlap` — Toast と Dialog の共存 |
| 08 | `Toast Overlap` — Loading 表示中 (Toast は覆いの下) |
| 09 | `Toast Overlap` — Loading 終了後も Toast が残る |
| 10 | `Toast Overlap` — 完了後の結果表示 (Android 系と maui (iOS) は一覧を下端まで送った状態) |
| 11 | TS-NM-01 操作前 (Toast がメニュー行に重なっている) |
| 12 | TS-NM-01 操作後 (背後の行が反応して画面が遷移し、Toast は残っている) |
| 13 | (android のみ) **スクロール化の修正前**に、見切れの切り分けとして Pixel 6a で撮った結果表示 |

2026-08-27 の修正 (Android のデフォルト View の落ち影・メニューのスクロール化) を受けて、
連番 01 の Android 系3枚と連番 10 の5枚を撮り直した。撮り直しも同じ端末・同じ起動引数で行い、
Pixel 4a のステータスバー切り落としの規律も同じ。

`kmp-android-02` は未取得 (連写が消滅後のコマを跨がなかった。消滅そのものは
`kmp-android-01` と他ルートの 02 で確認済み)。

`maui-ios-*` は 2026-08-27 の再通しで撮った (連番 01〜10 の10枚)。連番 11 / 12 (TS-NM-01) は
maui (Android) / kmp の2ルートと同じく省いた (素通しは ios / android の2ルートで成立済み)。
撮影は単発の `simctl io screenshot` が遅く短命な Toast を撮り逃すため、時間差をつけた
並列連写でコマを集め、状態ごとに1枚だけを残している。

## 2周目修正 (例外境界) 後の再確認 — MAUI 2経路 (2026-08-28)

review-004 の Minor 指摘 (MAUI の binding / 互換面を書き換えたのに、実アプリを通した証跡が
2周目修正より前で止まっている) への対応。**修正後のソースからビルドし直した Sample** で、
今回の修正が触った経路の既存デモを通して成功系が不変であることを確認した。

対象にした修正は次の2つ:

- **MAUI Android の Toast 中身供給の防護** — C# 層のレジストリ → Android Toast provider →
  Kotlin bridge。`CreateContent()` の戻りを nullable 化した経路の**成功系**
- **MAUI iOS の例外境界と notifier の attach 順序** — factory の `throws` 化と、
  `MauiDialogPresentation` へ notifier を結ぶ位置の移動。Dialog 提示の**正常系と結末通知**

### ビルド (いずれも修正後のソースから。修正の最終更新は 2026-08-28 00:43〜00:46)

| ルート | コマンド | 時刻 / 結果 |
|---|---|---|
| maui (Android) | `dotnet build samples/maui/KsDialogs.Sample.Maui/KsDialogs.Sample.Maui.csproj -f net10.0-android -p:EmbedAssembliesIntoApk=true` | 2026-08-28 01:15:13 開始 → 01:17 成功 (エラー 0。警告は binding の既知 BG8401 のみ)。生成 APK は同 01:17 |
| maui (iOS) | `MD_APPLE_SDK_ROOT=/Applications/Xcode-26.1.1.app dotnet build samples/maui/KsDialogs.Sample.Maui/KsDialogs.Sample.Maui.csproj -f net10.0-ios -p:RuntimeIdentifier=iossimulator-arm64` | 2026-08-28 01:17:18 → 01:17:30 成功 (**警告 0 / エラー 0**)。app bundle 01:17、同梱の `KsDialogs.Maui.dll` 00:46、リンクされる `KsDialogsMauiBridgeiOS.xcframework` 00:46 — いずれも修正 (00:43) より後 |

投入は従来と同じ手順 (Android: `adb uninstall` してから `adb install -r` / iOS: `xcrun simctl install`)。
`xcode-select` などのシステム設定は変更していない。

### 通したデモと判定

| ルート / 端末 | デモ | 観察 | 判定 |
|---|---|---|---|
| maui (Android) / Pixel 4a | `custom-toast` | 登録経路の `カスタムトースト` (青のピル + チェック) とインライン経路の `インライントースト` が上下2段に重なって表示され、それぞれの duration の経過で消えた。消滅後もメニューは通常表示のままでアプリは生存 (連写の後半コマがすべて同一のメニュー画面) | ✓ 従来どおり (`maui-android-03-custom-toast.png` と一致) |
| maui (iOS) / iPhone 17 Simulator | `custom-toast` | 同上。2枚の重なり・文言・配置とも従来と同じ | ✓ 従来どおり |
| maui (iOS) / iPhone 17 Simulator | `basic-dialog` | 覆いの上にダイアログが出て `こんにちは、KsDialogs!` + `キャンセル` / `OK` を表示。`OK` をタップすると閉じ、一覧を下端まで送ると `直近の結果` / `結果: completed(true)` が出た | ✓ 提示・結末通知とも従来どおり (attach 順序の変更で通知が落ちないことを実提示で確認) |

`custom-toast` の**失敗系** (中身の作り手が例外を投げるときの1枚破棄) は、Sample に該当デモが無いため
このセクションでは通していない (ここで確認したのは成功系の不変性)。失敗系は下の
「失敗系の実機観測 (A 方式・2026-08-28)」で一時デモを足して観測している。

### 撮り直し / 追加した証跡

| ファイル | 扱い |
|---|---|
| `maui-android-03-custom-toast.png` | 差し替え (2026-08-28 撮影)。Pixel 4a・ステータスバー切り落としの規律は従来と同じ (1080x2204) |
| `maui-ios-03-custom-toast.png` | 差し替え (2026-08-28 撮影) |
| `maui-ios-basic-dialog-after-fix.png` | 追加。`basic-dialog` のダイアログ提示中 |
| `maui-ios-basic-dialog-result-after-fix.png` | 追加。`OK` 後の `結果: completed(true)` |

Dialog 系の2枚は Toast の連番 (01〜13) の体系に属さないため、連番を振らず用途で名付けている。

## 失敗系の実機観測 (A 方式・2026-08-28)

review-004 の Minor 指摘② と deviation.md の「失敗系の実機観測 (オーナー決定)」への対応。
**Sample へ「失敗する factory」を一時的に足して** factory 例外境界 (2周目修正) の失敗系を
実機/シミュレータで観測した。一時編集は撮影後に copy で書き戻して復元してある。

### 一時変更の内容 (撮影後に復元済み)

変更したのは `samples/maui/KsDialogs.Sample.Maui/SampleMenuPage.xaml.cs` の 1 ファイルだけで、
ライブラリ本体・bridge には触れていない。

| デモ | 一時変更 |
|---|---|
| `custom-toast` | インライン経路の View factory を `throw new InvalidOperationException(...)` に差し替え (登録経路の1枚はそのまま)。失敗の 5 秒後に既定 Toast を 1 枚出して後続の正常系を見る |
| `custom-loading` | 登録済み ViewModel を渡す形からインライン factory の overload に替え、その factory を throw に。`StartAsync` を try/catch で囲み、失敗を結果表示へ出す。失敗の 2 秒後に `Default Loading` を実行して後続の正常系を見る |
| `inline-dialog` | インライン経路の View factory を throw に。`ShowAsync` を try/catch で囲み、失敗を結果表示へ出す。失敗の 2 秒後に `Basic Dialog` を出して後続の正常系を見る |

### ビルド

| ルート | コマンド | 結果 |
|---|---|---|
| maui (iOS) | `MD_APPLE_SDK_ROOT=/Applications/Xcode-26.1.1.app dotnet build samples/maui/KsDialogs.Sample.Maui/KsDialogs.Sample.Maui.csproj -f net10.0-ios -p:RuntimeIdentifier=iossimulator-arm64` → `xcrun simctl install` | 2026-08-28 01:24 成功 (警告 0 / エラー 0)。リンクされる `KsDialogsMauiBridgeiOS.xcframework` は 00:46 生成 = 2周目修正 (00:43) より後 |
| maui (Android) | `dotnet build ... -f net10.0-android -p:EmbedAssembliesIntoApk=true` → `adb -s <Pixel 4a> install -r` | 2026-08-28 01:26 成功 (警告 0 / エラー 0) |

`xcode-select` などのシステム設定は変更していない。撮影後は**復元したソースから両ルートを
再ビルドして端末へ入れ直し**、一時ビルドが端末に残らないようにした。

### 観測結果

| ケース | 期待 (修正後の契約) | 観測 | 判定 |
|---|---|---|---|
| maui (Android) / Pixel 4a: Toast factory 例外 | プロセスは落ちず、その1枚だけ表示されない。登録経路のもう1枚と後続の Toast は正常表示 | 登録経路の `カスタムトースト` (青のピル) だけが出て、インライン経路の1枚は出ない (`maui-android-failure-toast.png`)。logcat に `W KsDialogs: Toast の中身を作れませんでした。この表示を破棄します。` + `IllegalStateException: MAUI 側が表示の中身を作れませんでした。` が 1 回。その後の既定 Toast `Hello Toast!` は正常表示 (`maui-android-failure-toast-next.png`)。プロセスは生存 (同一 pid) | ✓ 期待どおり |
| maui (iOS) / iPhone 17 Simulator: Toast factory 例外 | 同上 | 登録経路の1枚だけが出てインライン経路は出ない (`maui-ios-failure-toast.png`)。5 秒後の既定 Toast `Hello Toast!` も正常表示 (`maui-ios-failure-toast-next.png`)。プロセスは生存 (`launchctl list` で同一 pid を確認) | ✓ 期待どおり |
| maui (iOS): Loading factory 例外 | プロセスは落ちず、Loading の呼び出しが失敗として返る | **プロセスがクラッシュ** (SIGSEGV / EXC_BAD_ACCESS `KERN_INVALID_ADDRESS at 0x10`)。起動 1.7 秒後にホーム画面へ戻る (`maui-ios-failure-loading-crash.png`)。クラッシュレポートの faulting frame は `MauiLoadingViewModel.makeContentView()` ← `closure #1 in MauiLoadingBridge.init()` ← `LoadingCoordinator.makeCustomContent` | ✗ **期待と不一致** |
| maui (iOS): Dialog factory 例外 | プロセスは落ちず、提示されずに失敗として返る | **プロセスがクラッシュ** (同じく SIGSEGV / `KERN_INVALID_ADDRESS at 0x10`)。faulting frame は `MauiDialogViewModel.makeContentView()` ← `closure #1 in MauiDialogBridge.init()` ← `DialogPresenter.present` | ✗ **期待と不一致** |

観測できた手掛かり (原因の断定はしていない):

- 落ちる 2 経路も落ちない Toast も、Swift 側は同じ形の `guard let content = contentProvider() else { throw ... }` を
  持ち、C# 側は同じ `BridgeContentSupply` で例外を握って `null` を返している。にもかかわらず
  Dialog / Loading だけが `makeContentView()` 内で nil 参照相当のアドレス (`0x10`) を触って落ちている
- 使ったアプリにリンクされている xcframework は 2周目修正より後 (00:46) の生成で、**修正の入った
  バイナリで落ちている**
- Android は 3 面とも Kotlin 側で null を受けており、Toast の失敗は警告 1 行と 1 枚破棄で収まっている

この2件は**期待と異なる挙動**のため、一時変更を復元したうえでここで観測を止めている
(修正は行っていない)。tasks 8.5 は未完了のまま。

> この時点の記録はここまで。上の ✗ 2 件は後述の「クラッシュ2件の追試 (2026-08-28)」で
> **原因特定と解消の確認まで済んでいる** (この節の記述はそのときの観測として残してある)。

### 復元の確認

- 一時変更は撮影前に scratchpad へ copy でバックアップし、撮影後に copy で書き戻した
  (`git checkout` は使っていない)。書き戻し後の SHA-1 はバックアップと一致
- `git diff --stat -- samples/` は撮影前後で完全一致 (27 files changed, 1631 insertions(+), 297 deletions(-))。
  証跡 PNG 以外に残差はない

### 追加した証跡

| ファイル | 中身 |
|---|---|
| `maui-android-failure-toast.png` | Pixel 4a。インライン経路が破棄され登録経路の1枚だけが出ている |
| `maui-android-failure-toast-next.png` | 同上の後続。既定 Toast が正常表示 |
| `maui-ios-failure-toast.png` | iPhone 17 Simulator。同じく1枚だけ |
| `maui-ios-failure-toast-next.png` | 同上の後続。既定 Toast が正常表示 |
| `maui-ios-failure-loading-crash.png` | Loading の factory 例外でアプリが落ちてホーム画面に戻った状態 |
| `maui-ios-failure-dialog-crash.png` | Dialog の factory 例外で同じく落ちた状態 |

## クラッシュ2件の追試 (2026-08-28)

上の観測で ✗ になった maui (iOS) の Loading / Dialog について、原因を特定してから同じ手順で
撮り直した。**ライブラリ・bridge のソースは 1 行も変えていない** — 落ちていたのは
Sample アプリが古い bridge バイナリを抱えたままだったためで、リンクをやり直したら解消した。

### 原因 (古いネイティブリンク成果物)

クラッシュしたアプリの実行ファイルを読むと、3 面のうち **Toast だけが修正後の形**だった:

| 面 | クラッシュしたアプリ内のシンボル | 意味 |
|---|---|---|
| Toast | `ToastViewModel.makeContentView()` … `yKF` | throws あり = 2周目修正後 |
| Loading | `LoadingViewModel.makeContentView()` … `yF` | throws なし = **2周目修正前** |
| Dialog | `DialogViewModel.makeContentView()` … `yF` | throws なし = **2周目修正前** |

クラッシュレポートの faulting アドレス (image + `0xf148`) を逆アセンブルすると、そこは
修正前の実装が「供給された中身の属性を nil 検査なしに読む」命令で、読みに行く先が
`nil + 0x10` になっていた。`KERN_INVALID_ADDRESS at 0x10` はこの命令のものである。

なぜ 3 面で食い違ったか:

- .NET for iOS の**ネイティブ実行ファイルのリンク結果**は Sample の `obj/.../nativelibraries/` に
  キャッシュされる。この成果物は 2周目修正より前 (前日 23:44) のもので、以後の
  `dotnet build` はこれをそのまま `.app` へ写していた
- xcframework 自体は修正後 (00:46) に再生成されていたが、**リンクはやり直されていない**。
  Swift の修正だけでは binding の C# アセンブリが変わらないため、リンク工程が
  最新と判断されたまま素通りした形
- Toast は 2周目修正の対象外 (23:44 の時点ですでに修正後の形) だったため、
  同じ古いバイナリの中でも 1 面だけ期待どおりに動いていた。これが「Toast だけ無事」の正体

### 追試の手順

Sample の iOS 向けビルド成果物 (`samples/maui/KsDialogs.Sample.Maui/{obj,bin}/Debug/net10.0-ios`) を
捨ててから再ビルドし、リンクをやり直した。再ビルド後のアプリでは 3 面とも throws ありの
シンボルになっていることをリンク結果で確認してから、上と同じ一時編集 (A 方式) を当てて撮影した。

| ルート | コマンド | 結果 |
|---|---|---|
| maui (iOS) | 上記 2 ディレクトリを破棄 → `MD_APPLE_SDK_ROOT=/Applications/Xcode-26.1.1.app dotnet build samples/maui/KsDialogs.Sample.Maui/KsDialogs.Sample.Maui.csproj -f net10.0-ios -p:RuntimeIdentifier=iossimulator-arm64` → `xcrun simctl install` | 2026-08-28 01:44 成功 (警告 0 / エラー 0) |

### 観測結果 (追試)

| ケース | 期待 (修正後の契約) | 観測 | 判定 |
|---|---|---|---|
| maui (iOS): Loading factory 例外 | プロセスは落ちず、Loading の呼び出しが失敗として返る | クラッシュせず、Loading は提示されないまま結果表示が `失敗: InvalidOperationException` になる (`maui-ios-failure-loading.png`)。元の例外の型がそのまま返っている。2 秒後の `Default Loading` は正常に出て 100% まで進み (`maui-ios-failure-loading-next.png`)、`結果: 完了` に到達。プロセスは生存 (同一 pid) | ✓ **修正後: 解消** |
| maui (iOS): Dialog factory 例外 | プロセスは落ちず、提示されずに失敗として返る | クラッシュせず、提示は起きないまま結果表示が `失敗: InvalidOperationException` になる (`maui-ios-failure-dialog.png`)。2 秒後の `Basic Dialog` は正常に提示される (`maui-ios-failure-dialog-next.png`)。プロセスは生存 (同一 pid) | ✓ **修正後: 解消** |
| maui (iOS): Toast factory 例外 (再確認) | その1枚だけ表示されない | 登録経路の `カスタムトースト` だけが出てインライン経路は出ない。5 秒後の既定 Toast `Hello Toast!` も正常表示。プロセスは生存 | ✓ 前回と同じ |

### 復元の確認 (追試)

- 一時編集は `samples/maui/KsDialogs.Sample.Maui/SampleMenuPage.xaml.cs` の 1 ファイルのみ。
  撮影前に scratchpad へ copy でバックアップし、撮影後に copy で書き戻した (`git checkout` は使っていない)
- 書き戻し後の SHA-1 はバックアップと一致。`git diff --stat -- samples/` も撮影前と完全一致
  (27 files changed, 1631 insertions(+), 297 deletions(-))
- 復元したソースから再ビルドしてシミュレータへ入れ直し、一時ビルドが端末に残らないようにした

### 追加した証跡 (追試)

| ファイル | 中身 |
|---|---|
| `maui-ios-failure-loading.png` | Loading の factory 例外。クラッシュせず `失敗: InvalidOperationException` が結果表示に出ている |
| `maui-ios-failure-loading-next.png` | 同上の後続。`Default Loading` が正常に進捗 100% まで動いている |
| `maui-ios-failure-dialog.png` | Dialog の factory 例外。クラッシュせず `失敗: InvalidOperationException` が結果表示に出ている |
| `maui-ios-failure-dialog-next.png` | 同上の後続。`Basic Dialog` が正常に提示されている |

`-crash.png` の 2 枚は原因調査時の記録として残してある (解消前の状態)。

### この追試から分かる撮影上の注意

**Sample の iOS ビルドは、bridge の Swift だけを直したときリンクをやり直さない。**
bridge を直した後に Sample で挙動を見るときは、`samples/maui/KsDialogs.Sample.Maui/obj` と
`bin` の `Debug/net10.0-ios` を捨ててからビルドする。実際に入れ替わったかは、
できた `.app` の実行ファイルに含まれる bridge のシンボルで確かめられる。

## 関連

- デルタスペック: `specs/samples/spec.md` (TS-SA-01〜06)、`specs/dialog-contract/spec.md` (TS-MX-05 / TS-NM-01)
- 起動引数の正: samples/README.md「撮影のための起動引数」
