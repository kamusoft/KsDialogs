# kmp ルートの登録経路デモの通し (共有コードの型指定 show) — 証跡

tasks 3.2 の記録 (2026-09-07)。対象は samples デルタスペックの Requirement
「kmp ルートの登録経路デモを共有コードの型指定 show にする」
(Scenario PB-KS-01 / LD-KS-01 / TS-KS-01)。

判定するのは「共有コード (`SamplePresenter`) の呼び出しを VM インスタンス渡しから
型指定 show + configure に差し替えても、画面に出るもの (ダイアログの文言・カスタム View・
進捗・Toast 2 枚・結果表示) が従来と変わらないこと」。文言の正は Sample パリティ規約の文言表
(`ViewModel から表示しています` / `結果: completed(true)` / `カスタムローディング` /
`結果: 完了` / `カスタムトースト` / `インライントースト`)。

iOS の通しは、共有コードの ViewModel factory が作った Kotlin オブジェクトが実 framework 越しに
Swift 側レジストリで解決されることの確認を兼ねる (`iosSimulatorArm64Test` では実提示が起きないため)。

## 環境

| 組 | 実行 OS / 端末 | ビルドと投入 |
|---|---|---|
| android | Android エミュレータ (1080x2340) | `samples/kmp` の `:androidApp:assembleDebug` → `adb install -r` / `am start` |
| ios | iPhone 17 Simulator (iOS 26.5)。この通し専用に boot した (起動中の他の Simulator は流用しない) | `samples/kmp/iosApp` を xcodebuild → `simctl install` / `simctl launch jp.kamusoft.ksdialogs.samples.kmp.ios` |

## 通した操作

デモの自動再生と刻み間隔は撮影支援の起動引数で作った (ソースの一時改変はしていない)。

```
# android
adb -s <android-serial> shell am force-stop <package> ; sleep 1 ; \
adb -s <android-serial> shell am start -n <package>/.MainActivity --es demo model-dialog
adb -s <android-serial> exec-out screencap -p > <一時パス>
adb -s <android-serial> shell input tap <OK の描画中心>
# custom-loading は --es loading-step-interval-ms 2000 を添えて起動、custom-toast は引数なし

# ios
xcrun simctl terminate <uuid> <bundle-id> ; sleep 2 ; \
xcrun simctl launch <uuid> <bundle-id> --demo model-dialog
xcrun simctl io <uuid> screenshot <一時パス>
# custom-loading は --loading-step-interval-ms 2000 を添えて起動、custom-toast は引数なし
```

1. `model-dialog` を自動再生し、表示中のダイアログを撮ってから `OK` を押し、結果表示を撮る
2. アプリを終了してから `custom-loading` を自動再生し、進捗の途中と完了後の結果表示を撮る
3. アプリを終了してから `custom-toast` を自動再生し、2 枚が重なって出ているところと、
   duration 満了で 2 枚とも消えた後を撮る

結果表示は縦の余白が足りず、完了直後のコマでは `直近の結果` の見出しだけが見えて値の行が
画面外にあるため、android の 02・04 はメニューを少し上へスクロールしてから撮っている
(archive の通しにも記録がある既存の見え方で、本変更の前後で変わらない)。ios は無スクロールで値まで見える。

Toast は表示時間が短く、コールド起動の遅さと重なって撮影の待ちを固定値にすると撮り逃すため、
起動後 1 秒間隔で連写して 2 枚が出ているコマと消えた後のコマを選んだ。

## 観察結果

| 観察点 | android | ios |
|---|---|---|
| PB-KS-01 `ViewModel から表示しています` のダイアログが出る (configure が入れた文言) | ✓ (01) | ✓ (01) |
| PB-KS-01 `OK` で閉じ、結果表示が `結果: completed(true)` になる | ✓ (02) | ✓ (02) |
| LD-KS-01 カスタム View の見出し `カスタムローディング` + 帯 + 百分率が出る (実撮は android が 50%、ios が 75%) | ✓ (03) | ✓ (03) |
| LD-KS-01 完了で表示が消え `結果: 完了` になる | ✓ (04) | ✓ (04) |
| TS-KS-01 `カスタムトースト` (登録経路) が中央寄りに ✓ バッジ付きで出る | ✓ (05) | ✓ (05) |
| TS-KS-01 `インライントースト` (インライン経路・OS 側実装のまま) が下寄せで同時に出て 2 枚が重なる | ✓ (05) | ✓ (05) |
| TS-KS-01 時間経過で 2 枚とも消える | ✓ (06) | ✓ (06) |

**PB-KS-01 / LD-KS-01 / TS-KS-01 の判定: 成立。** 2 組とも、差し替え前の見え方
(ダイアログの文言と結果・カスタム View の構成・2 枚の重なり・結果表示) と同じものが出た。

ios の 01〜06 は実 framework 越しの通しであり、共有コードのレジストリに登録した
ViewModel factory の生成物が Swift 側レジストリの View factory で解決されている
(解決に失敗すれば中身が出ず構成ミスで倒れる)。

観察できた差は次の 1 つで、本体の仕様差ではない:

- **android の 02・04・05・06 で状態バーが写っていない**: タップ・完了・自動再生の直後に
  OS が状態バーを隠した状態のまま撮れたもので、加工ではない。01・03 には時刻が写っている

## 撮影・保存の規律

- android はエミュレータのため無加工。写っているのは時刻・Wi-Fi・電波・電池の標準表示だけで、
  個人を特定する通知アイコンは無い
- ios はシミュレータのため無加工。写っているのは時刻・Wi-Fi・電池の標準表示のみ
- 保存後に 12 枚の md5 を取り、重複が無いことを確認した。説明文は保存した実体を開いて
  突き合わせて書いた (android-04 / android-06 / ios-02 / ios-04 / ios-06 は保存先のファイルを直接開き、
  残りは保存元と同一バイト列であることを md5 で確認した)

## ファイル

`<組>-<連番>-<状態>.png`。連番の意味は 2 組で共通:

| 連番 | 状態 |
|---|---|
| 01 | Model Dialog 表示中 — `ViewModel から表示しています` + `キャンセル` / `OK` |
| 02 | Model Dialog 完了 — メニューに戻り `結果: completed(true)` |
| 03 | Custom Loading 表示中 — `カスタムローディング` + 帯 + 百分率 (android は `50%`、ios は `75%`) |
| 04 | Custom Loading 完了 — メニューに戻り `結果: 完了` |
| 05 | Custom Toast — `カスタムトースト` と `インライントースト` の 2 枚が重なって表示 |
| 06 | Custom Toast の満了後 — 2 枚とも消えたメニュー |

組の接頭辞は `android` / `ios`。
