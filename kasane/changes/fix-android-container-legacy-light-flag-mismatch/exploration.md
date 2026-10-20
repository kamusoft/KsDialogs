# Exploration: fix-android-container-legacy-light-flag-mismatch

## 課題 / 動機

### 事象

- 利用側 KsApp.Core が KsDialogs.Maui を 0.1.0-beta.2 から 0.1.0-beta.3 に上げたところ、Android (エミュレータ Pixel_API_36、Android 16) で、Dialog・Loading・Toast が出ている間、ステータスバーの時計とアイコンが白から暗い色になり、暗い覆いの上でほとんど読めなくなった
- 閉じると白に戻る。毎回再現する。iOS では起きない
- beta.2 では、Dialog の表示中も白のままだった
- 画像と記録 (KsApp 側の証跡):
  - `../KsApp/kasane/changes/adopt-ios-scene-lifecycle/evidence/dialog-android-statusbar-before.png` (beta.2)
  - `../KsApp/kasane/changes/adopt-ios-scene-lifecycle/evidence/dialog-android-statusbar-after.png` (beta.3)
  - `../KsApp/kasane/changes/adopt-ios-scene-lifecycle/evidence/dialog-results-after.txt`

### KsApp 側の調査で分かっていること

コードの読みによるもので、端末での裏取りはしていない。「推定」と付けた項目は未確認。

- 関係する変更は commit f3b51c6 (change fix-android-container-system-bar-appearance、`kasane/decisions/core/0040-containers-keep-host-system-bar-settings.md`)。器が提示先の明暗を読むとき、提示先の旧来のフラグ (DecorView の LIGHT_STATUS_BAR / LIGHT_NAVIGATION_BAR) も足して読むようになった (`android/ksdialogs-core/src/main/kotlin/jp/kamusoft/ksdialogs/DialogWindowSystemBars.kt` の `hostAppearance()`)。Loading / Toast の器も同じ引き継ぎを通すようになった
- 提示先 (MAUI の Activity) は、旧来のフラグでは明るい地向けが立ったまま、プラットフォームの InsetsController では暗い地向け (白アイコン) が明示されている、という食い違った状態とみられる (推定)
  - 旧来のフラグを立てているのは MAUI 本体 (推定)
  - 暗い地向けを明示しているのは KsApp.Core (`../KsApp/KsApp.Core/Src/Platforms/Android/StatusBarThemeApplier.cs` の `ApplyMode`。API 30 以上では InsetsController だけを使い、旧来のフラグに触れない。確認済み)
- API 35 以上では、提示先は明示したビットで白アイコンになるが、器は旧来のフラグを拾って暗いアイコンになる
- この形は core/ADR-0040 の Consequences に、承知のうえで受け入れた取りこぼしとして記録されている。KsDialogs 側の確認の記録は、明るい地 + 暗いアイコンの提示先だけで、暗い地 + 白アイコンの提示先や MAUI の提示先で確かめた記録は見つからなかった
- 出る条件 (推定): Android 15 以上・ライト外観・提示先が暗いステータスバー色。ダーク外観では出ない見込み
- KsDialogs.Maui に、表示中のシステムバーの明暗を利用側から指定する公開面は無い

### 発見の文脈

- KsApp の change adopt-ios-scene-lifecycle の実装で、KsDialogs.Maui の更新の前後を比べて見つかった
- KsApp 側ではこの変更では直さず、KsDialogs 側に起票すると決めた (オーナー指示 2026-10-04)
- KsApp 側の議論の出典: `../KsApp/kasane/changes/adopt-ios-scene-lifecycle/` の `deviation.md`・`review-001.md`・`second-opinion-code-001.md`・`evidence/`

## 検討した選択肢 (却下案と理由を含む)

## 決定事項

## ADR 候補 (作成済み: ADR-NNNN / 未起票: ...)

## 未決の論点

未探索 (簡易起票)。

- 端末での裏取り: 提示先の旧来のフラグと `systemBarsAppearance` の実際の値、旧来のフラグを立てている主体
- 直す場所と方法。次のどれにするか
  - KsDialogs 側で読み取りの規則を変える (core/ADR-0040 の見直しになる)
  - MAUI の提示先向けの扱いを足す
  - 利用側 (KsApp.Core) が明暗の指定を揃える前提にして、文書に書く
- KsApp 側でも直せる見当がある (`ApplyMode` で旧来のフラグも揃える)。どちらで直すか、両方か

## UI 素材 (ui/references/ の一覧と注釈)

## 変更級の推奨: S / M / L (理由)

未判定。
