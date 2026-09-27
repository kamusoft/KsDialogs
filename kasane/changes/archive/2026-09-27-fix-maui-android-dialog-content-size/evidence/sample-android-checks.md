# MAUI Sample (Android) での修正後の操作確認

中身の受け渡しに包みを挟んだあとも、タップ・Toast のタッチ素通し・MAUI 側の演出が保たれていることを、修正後のライブラリで組んだ `samples/maui` を Android エミュレータ (API 31、1080x2400 px) で動かして確かめた。Sample のソースは変更していない (起動は `--es demo <デモID>` と `adb -s <android-serial> shell input tap`)。

| 確認 | 手順 | 観測 | 実体 |
|---|---|---|---|
| Dialog の中身のタップ | `basic-dialog` で起動 → ダイアログの `OK` をタップ | ダイアログが閉じ、メニュー下の直近の結果が `結果: completed(true)` になった | 画像なし (観測は本文のみ) |
| Toast のタッチ素通し | 通常起動 → `Custom Toast` をタップ → カスタムトーストとインライントーストが出ている間に、背後の `Basic Dialog` の行をタップ | 2 枚の Toast が出たまま、背後の画面で Basic Dialog のダイアログ (`こんにちは、KsDialogs!`) が開いた。Toast の上ではなく Toast の外の行をタップしている。Toast の器はウィンドウ単位でタッチを受けない設定のため、包みの有無はこの経路に関与しない | `sample-android-toast-tap-through.png` (覆いの下に Toast 2 枚が暗く見え、手前に Basic Dialog が出ている) |
| MAUI 側の演出 (包みが中身を切り取らない) | `transition-dialog` で起動 → `Custom Hook` を選び `表示` をタップ → 約 0.3 秒後に撮影 | 出現の途中で、中身 (`トランジションのデモです` のダイアログ) が最終位置より下にずれて半透明のまま描かれている。包みの枠の外へ動いた部分も欠けずに見える。約 2.5 秒後には最終位置で不透明に落ち着いた (後者の画像は証跡に置いていない) | `sample-android-custom-hook-midframe.png` |

時間スライダーの操作も試みたが、画像では 250 ms のままで、演出時間の変更は効いていない (観測の目的には影響しない)。
