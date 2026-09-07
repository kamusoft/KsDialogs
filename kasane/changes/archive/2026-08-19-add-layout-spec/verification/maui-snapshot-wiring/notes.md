# MAUI 経路のスナップショット配線の実環境確認 (2026-08-19)

MAUI の添付が「画面に載せたあとの初回レイアウトパスの中で書き換わっても採用される」ことを、
iOS Simulator と Android 実機で 1 回ずつ確認した記録。自動テストは値オブジェクトと段取り
(`DialogAttributeSnapshotRelay`) までしか届かず、platform 側のフック接続は実機でしか通らないため。

## 環境

| 形態 | 端末 | ビルド |
|---|---|---|
| MAUI iOS | iPhone 17 Pro Simulator (<uuid> / iOS 26.0) | `dotnet build -f net10.0-ios -p:RuntimeIdentifier=iossimulator-arm64 -p:ValidateXcodeVersion=false` |
| MAUI Android | Pixel 4a - 13 (0B261JEC216142) | `dotnet build -f net10.0-android -t:Run` |

`ValidateXcodeVersion=false` は Xcode 26.5 と .NET for iOS 26.1 の要求バージョン差を通すためのビルド時フラグで、ソースには影響しない。

## 差し込んだ一時的な供給点 (確認後に撤去済み)

`LayoutDialogCardView` に、画面に載せたあとの初回レイアウトパスの中で 2 つの添付を書き換える
差し込みを一時的に置いた。どちらも **options 側**の属性なので、Show の引数 (placement) には影響されず、
中身への添付だけが供給経路になる。

- `Dialog.SetOverlayColor` → 赤 50% (見た目で判別できる)
- `Dialog.SetIsCanceledOnTouchOutside` → false (操作で判別できる)

供給の差し込み点は platform で異なる:

- **iOS**: `ArrangeOverride` の中で、platform view が window に載っているときだけ書き換える。
  提示より前の暫定パスでは window が無いので書き換わらず、**画面に載せたあとのパスでだけ**供給される
- **Android**: MAUI の配置がこの View まで下りてこない (`MeasureOverride` / `ArrangeOverride` /
  `OnSizeAllocated` のいずれも呼ばれないことを logcat で実測) ため、platform view の
  `LayoutChange` を供給点にした。この購読は委譲面の購読より先に登録されるので、
  委譲面が実効値を固定する直前に書き換えが届く

## 確認結果

| # | 形態 | 観察 | 証跡 |
|---|---|---|---|
| 1 | iOS | 覆いが赤で出た = レイアウトパスの中で届いた overlayColor が器へ渡っている | `ios-01-probe-overlay-red.png` |
| 2 | iOS | 外側をタップしても閉じない = 同じく届いた isCanceledOnTouchOutside=false が効いている | `ios-02-probe-outside-tap-not-closed.png` |
| 3 | Android | 覆いが赤で出た | `android-01-probe-overlay-red.png` |
| 4 | Android | 外側をタップしても閉じない | `android-02-probe-outside-tap-not-closed.png` |

いずれも「委譲面が固定した値を Native の添付面へ渡し直す」段取りが働かなければ既定の覆い (黒 40%) の
まま出て、外側タップで cancelled になる。実際、Android で供給点を取り違えていた試行 (書き換えが
1 度も走らなかった回) では覆いは既定色で、外側タップでそのまま閉じた。

差し込みを撤去したあとの通常の挙動も、両形態で撮り直して確認した (覆いは既定の黒 40%、
外側タップで cancelled)。

| # | 形態 | 観察 | 証跡 |
|---|---|---|---|
| 5 | iOS | 差し込み撤去後は既定の覆いで出る | `ios-03-after-probe-removed-default.png` |
| 6 | iOS | 外側タップで cancelled になる | `ios-04-after-probe-removed-outside-cancelled.png` |
| 7 | Android | 差し込み撤去後は既定の覆いで出る | `android-03-after-probe-removed-default.png` |
| 8 | Android | 外側タップで cancelled になる | `android-04-after-probe-removed-outside-cancelled.png` |
| 9 | iOS | パネルで End / End を選ぶと右下に出る (置き場所の輸送) | `ios-05-after-probe-removed-end-end.png` |
| 10 | iOS | OK で completed(true) が返る | `ios-06-after-probe-removed-completed.png` |

9・10 を撮り直したのは、下の不具合により MAUI iOS が提示のたびに落ちていた期間があり、
`verification/sample-walkthrough/` の MAUI iOS 分がその不具合より前の記録になっているためである。
撮り直しは基本経路 (表示・置き場所・外側タップ・完了) に絞ってあり、
LayoutArea トグルと Offset の 2 項目は撮り直していない。

## この確認で見つけて直した不具合

- **MAUI iOS が提示のたびに落ちていた** — 互換面の `KSDMauiDialogContent.applyAttributes` は
  ObjC のクラスメソッドとして公開していたが、アプリへの静的リンク後のバイナリにそのメソッドが
  残らず (`otool -oV` で metaclass の method list が空になることを確認)、実行時に
  `unrecognized selector sent to class` で異常終了していた。インスタンスの操作へ変更して解消。
  自動テストは net10.0 のみで platform の実体を通らないため、この経路は実機でしか出ない

## 残る限界

- 差し込みは確認後に撤去したため、この経路の回帰保護は自動テストには無い。
  自動化できているのは `DialogAttributeSnapshotRelayTests` (暫定転送 → 固定 → 渡し直しの段取り) までで、
  platform のフック (iOS の `LayoutSubviews`、Android の `ViewAttachedToWindow` / `LayoutChange`) を
  どこに繋ぐかは静的レビューと本記録が担保している
