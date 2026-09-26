# Exploration: fix-android-api35-overlay-status-bar-icons

## 課題 / 動機

Android の API 35 で、透明な覆いのダイアログを出している間、ステータスバーのアイコン (時刻・電波・電池) が白地に溶けて見えなくなる。

- 観測: instrumented テスト `DialogTransparentOverlayTests.覆いが透明ならステータスバーの明るさは表示前後で変わらない` が API 35 の AVD で間欠的に失敗する (`表示前 246.7… / 表示中 255.0`)。表示中の値はいつも 255.0
- 画面を撮ると、ダイアログが出ている間はステータスバーの帯が一様に白 (輝度 255) になり、閉じると元の濃いアイコンに戻る。アイコンが白で描かれて白地に溶けていると見られる
- 変更前のコード (コミット c4936a8) でも同じ AVD で 5 回中 4 回失敗する。以前からある挙動
- CI (API 36) と API 33 の実機では再現していない
- 観測環境: 専用 AVD (Small_Phone、API 35)

発見の文脈: add-page-layout-area の tasks 6.1 (全ルートの全件実行)。この change は覆いとシステムバーの扱いに触れていない。記録は `kasane/changes/archive/2026-09-27-add-page-layout-area/evidence/distill-handoff.md` の失敗 A。

## 検討した選択肢 (却下案と理由を含む)

## 決定事項

## ADR 候補 (作成済み: なし / 未起票: なし)

## 未決の論点

**未探索 (簡易起票)**。分かっている疑問点:

- ライブラリ本体の不具合か、テストの前提の問題か。KsDialogs の器のウィンドウ (`android.app.Dialog`) が前面に来たとき、ステータスバーのアイコンの明暗 (`isAppearanceLightStatusBars`) を画面側の指定から引き継いでいないように見える
- API 35 だけで起きる理由。API 35 の edge-to-edge 強制 (targetSdk 35) やシステムバーの外観の既定の変化との関係
- add-page-layout-area の付随修正 (Sample Android の `MainActivity.kt` で API 35 以上のときだけ `isAppearanceLightStatusBars = true`) と同じ系統か。Sample 側で直したものが、ライブラリの器でも必要になるのか
- 間欠的に通る (5 回中 1 回) 理由。器のウィンドウの外観が決まるタイミングとの関係
- 既存のシステムバーの契約 (`DialogSystemBarsTests` 系・core の system bars の規則) との関係

## UI 素材 (ui/references/ の一覧と注釈)

なし

## 変更級の推奨: 未判定 (暫定 S — 器のウィンドウの外観の引き継ぎで閉じるなら)
