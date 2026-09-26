# Exploration: fix-ios-sample-demo-autoplay

## 課題 / 動機

iOS Sample (`samples/ios/`) を撮影支援の起動引数 `--demo <デモID>` で起動すると、自動再生が正しく働かない。手でメニューをタップすれば正常に出る。

- `default-loading` / `custom-loading`: 処理は走って結果表示は変わるのに、Loading が一度も画面に出ない
- `basic-dialog`: `showBasicDialog` の `assertionFailure` で落ちる (提示先を取れていないと見られる)
- 変更前 (define-loading-action-thread 着手前) のビルドでも同じで、この change が原因ではない
- 観測環境: iPhone Air / iOS 26.5 の Simulator

撮影支援の起動引数は Sample の撮影手順 (`kasane/config.yaml` の `ui.screenshot`、`kasane/handbook/cross/sample-parity.md`「撮影支援の起動引数」節) の前提で、iOS で効かないと証跡の撮影がタップ頼みになる。

発見の文脈: define-loading-action-thread の tasks 5.4 / 5.5 (Sample の通しの撮影) で、自動再生では Loading が撮れず、メニューのタップで撮り直したとき。

## 検討した選択肢 (却下案と理由を含む)

## 決定事項

## ADR 候補 (作成済み: なし / 未起票: なし)

## 未決の論点

**未探索 (簡易起票)**。分かっている疑問点:

- 起動直後の提示先の解決が、`SampleMenuScreen.autoPlay` の `Task.yield()` 1 回では足りていない可能性 (実装ワーカーの見立て。未確認)
- Loading は提示先が無いとき黙って流しているように見える。これが契約どおりの挙動か (ライブラリ側の確認対象か) は未確認
- KMP の iOS Sample (`samples/kmp/iosApp/`) でも同じ症状が出るかは未確認
- いつから効かなくなったか (撮影支援の起動引数を入れた change の時点で iOS 26.5 相当の環境で確かめていたか) は未確認

## UI 素材 (ui/references/ の一覧と注釈)

なし

## 変更級の推奨: 未判定
