# Exploration: fix-maui-android-dialog-content-size

## 課題 / 動機

MAUI の Android で、ダイアログの中身に置いた View の大きさ指定 (`WidthRequest` / `HeightRequest`) が効かず、中の子要素の大きさまで縮んで表示される疑いがある。

- 観測: 中身に `WidthRequest=120`・`HeightRequest=80` の ContentView (中に Label 1 つ) を置いたダイアログが、iOS では 120×80pt で出るのに、Android では Label の大きさ (約 36×19dp) で出た
- 観測環境: Android エミュレータ (emulator-5554)、MAUI の実配置テストホスト `maui/KsDialogs.Maui.PlacementHost` のプローブ (`PlacementProbe.cs`) の 1 構成
- 観測は 1 件だけで、原因は未調査

発見の文脈: add-page-layout-area の tasks 4.4 (MAUI の実配置テスト)。この change は基準領域の経路だけに触れ、中身の測り方には触れていない。配置の判定は矩形の位置で見るため、この change の合否には影響しない。

## 検討した選択肢 (却下案と理由を含む)

## 決定事項

## ADR 候補 (作成済み: なし / 未起票: なし)

## 未決の論点

**未探索 (簡易起票)**。分かっている疑問点:

- MAUI の View を Native の器へ渡すときの測り方 (Android の `PlatformDialogContent` 周辺) で、`WidthRequest` / `HeightRequest` が測定の制約に反映されているか。iOS との経路差はどこか
- ダイアログの sizing 指定 (比率・固定) がある場合とない場合 (中身に合わせる) で挙動が違うか。layout 契約 (concepts core/api/layout-semantics) の「中身に合わせる」の規則との関係
- Sample (samples/maui) の既存ダイアログでも同じ縮みが出ているか。消費者検証 (verification/) で見落としていないか
- 既存の MAUI テストで中身の大きさを区別できるものがあるか

## UI 素材 (ui/references/ の一覧と注釈)

なし

## 変更級の推奨: 未判定 (暫定 S — 測り方の経路の修正で閉じるなら)
