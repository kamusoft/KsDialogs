# Custom Loading / Custom Toast の通し (型指定経路) — 証跡

tasks 4.2 の記録 (2026-09-06)。対象は samples デルタスペックの Requirement
「Custom Loading / Custom Toast デモの登録経路を型指定 show にする」
(Scenario LD-YS-01 / TS-YS-01)。

判定するのは「登録経路の呼び出しを VM インスタンス渡しから型指定 show に差し替えても、
画面に出るもの (カスタム View・進捗・文言・結果表示) が従来と変わらないこと」。
文言の正は Sample パリティ規約の文言表 (`カスタムローディング` / `カスタムトースト` /
`インライントースト` / `結果: 完了`)。

## 環境

3 ルート (ios / android / maui) を通した。maui は実行 OS を 2 つ持つため、iOS と Android の
両方で通している (計 4 組)。

| 組 | 実行 OS / 端末 | ビルドと投入 |
|---|---|---|
| ios | iPhone 17 Pro Simulator (iOS 26)。この通し専用に boot し、終了後 shutdown した | `samples/ios` を xcodebuild → `simctl install` / `simctl launch jp.kamusoft.ksdialogs.samples.ios` |
| android | Android エミュレータ (Android 15 / 1080x2340) | `samples/android` の `:app:assembleDebug` → `adb install -r` / `am start` |
| maui (iOS) | 同じ iPhone 17 Pro Simulator | `dotnet build -f net10.0-ios -p:RuntimeIdentifier=iossimulator-arm64 -p:ValidateXcodeVersion=false` → `simctl install` / `simctl launch jp.kamusoft.ksdialogs.samples.maui` |
| maui (Android) | 同じ Android エミュレータ | `dotnet build -f net10.0-android -p:EmbedAssembliesIntoApk=true` → `adb uninstall` してから `adb install -r` / `am start` |

kmp ルートは本 change の対象外 (インスタンス渡しのまま) のため再撮影せず、基準は
[archive の add-loading の通し](../../../archive/2026-08-26-add-loading/verification/sample-walkthrough/notes.md) と
[archive の add-toast の通し](../../../archive/2026-08-28-add-toast/ui/verification/sample-walkthrough.md)
に記録された観察結果を参照する。

## 通した操作

デモの自動再生と刻み間隔は撮影支援の起動引数で作った (ソースの一時改変はしていない)。

```
# ios / maui (iOS)
xcrun simctl terminate <UDID> <bundle-id> ; sleep 2 ; \
xcrun simctl launch <UDID> <bundle-id> --demo custom-loading --loading-step-interval-ms 2500
xcrun simctl io <UDID> screenshot <一時パス>

xcrun simctl terminate <UDID> <bundle-id> ; sleep 2 ; \
xcrun simctl launch <UDID> <bundle-id> --demo custom-toast

# android / maui (Android)
adb -s <serial> shell am force-stop <package> ; sleep 1 ; \
adb -s <serial> shell am start -n <package>/<activity> \
  --es demo custom-loading --es loading-step-interval-ms 2500
adb -s <serial> exec-out screencap -p > <一時パス>
```

1. `custom-loading` を自動再生して、進捗の途中を 2 コマ撮る
2. 完了後の結果表示 `結果: 完了` を撮る
3. アプリを終了してから `custom-toast` を自動再生し、2 枚が重なって出ているところを撮る

maui (Android) だけは起動が遅く、刻み間隔を 4000 ms に延ばしても最初のコマが 0% になり、
Toast は自動再生の 2 枚が撮影前に消えてしまった。Toast は通常起動してからメニューの
`Custom Toast` 行をタップして撮り直した (自動再生と同じ入口を呼ぶので観察対象は変わらない)。

## 観察結果

| 観察点 | ios | android | maui (iOS) | maui (Android) |
|---|---|---|---|---|
| LD-YS-01 カスタム View の見出し `カスタムローディング` + 帯 + 百分率が出る | ✓ | ✓ | ✓ | ✓ |
| LD-YS-01 進捗が段階的に上がる (実撮は ios / android / maui (iOS) が 25% → 50%、maui (Android) が 0% → 50%) | ✓ | ✓ | ✓ | ✓ |
| LD-YS-01 完了で表示が消え `結果: 完了` になる | ✓ | ✓ | ✓ | ✓ |
| TS-YS-01 `カスタムトースト` (登録経路) が中央寄りの帯に ✓ バッジ付きで出る | ✓ | ✓ | ✓ | ✓ |
| TS-YS-01 `インライントースト` (インライン経路) が下寄せで同時に出て 2 枚が重なる | ✓ | ✓ | ✓ | ✓ |

**LD-YS-01 / TS-YS-01 の判定: 成立。** 4 組とも、差し替え前の archive の通しに記録された見え方
(カスタム View の構成・文言・2 枚の重なり・結果表示) と同じものが出た。
kmp ルートは呼び出しを変えていないため、archive の基準がそのまま現在の見え方であり、
4 ルートの見た目は同一である。

観察できた差は次の 3 つで、いずれも本体の仕様差ではない:

- **結果表示の見切れ**: android / maui (iOS) / maui (Android) は縦の余白が足りず、完了直後のコマでは
  `直近の結果` の見出しだけが見えて値の行が画面外にある。メニューを少し上へスクロールしてから
  撮り直して `結果: 完了` を確認した (03 のコマはスクロール後)。ios は無スクロールで値まで見える。
  archive の add-loading の通しでも同じ見切れが記録されている既存の見え方で、本変更の前後で変わらない
- **maui (iOS) の状態バー左上の `◀ KsDialogs Sample`**: `simctl launch` で直前のアプリから
  切り替えた OS の表示であり、Sample の画面ではない
- **Android 系のコマで状態バーが写っていないものがある** (android の 03・04、maui (Android) の 03・04):
  完了・タップ後に OS が状態バーを隠した状態のまま撮れたもので、加工ではない

## 撮影・保存の規律

- Android はエミュレータのため無加工。写っているのは時刻・Wi-Fi・電波・電池の標準表示だけで、
  個人を特定する通知アイコンは無い
- iOS はシミュレータのため無加工。写っているのは時刻・Wi-Fi・電池の標準表示のみ
- 保存後に 16 枚すべてを開き直して説明文と突き合わせ、md5 に重複が無いことを確認した
  (ios と maui (iOS)、android と maui (Android) の同じ番号のコマは似て見えるが、
  状態バーの表示・行の位置・画素が異なる)

## ファイル

`<組>-<連番>-<状態>.png`。連番の意味は 4 組で共通:

| 連番 | 状態 |
|---|---|
| 01 | Custom Loading 表示中 — `カスタムローディング` + 帯 + 百分率 (maui (Android) のみ `0%` で帯が空、他は `25%`) |
| 02 | Custom Loading 表示中 — 帯が伸びて `50%` |
| 03 | Custom Loading 完了 — メニューに戻り `結果: 完了` |
| 04 | Custom Toast — `カスタムトースト` と `インライントースト` の 2 枚が重なって表示 |

組の接頭辞は `ios` / `android` / `maui-ios` / `maui-android`。
