# 実測: 実際の安全領域を持つ窓での SwiftUI の中身の安全領域と器の矩形 (2026-10-20、修正サイクル 2 の後に取り直し)

tasks.md 3.1 の記録。テスト用の窓 (`DialogLayoutTestWindow` の安全領域の上書き) ではなく、ノッチのある機種の Simulator が窓に与える実際の安全領域で、窓全体基準の上端・下端に付けた SwiftUI の中身を測った。修正サイクル 2 で要求サイズの求め方を直した後のコードで取り直し、窓の幅を超える長さの折り返す本文の条件を足した。

## 条件

- 端末: iPhone 17 Pro / iOS 26.0 の Simulator (`<udid>`)。`xcodebuild test -only-testing:` で使い捨ての測定テスト 1 本 (9 条件の引数つき) を実行し、`Test run with 1 test in 1 suite passed` を確認した。測定テストはコミットしていない (測り終えて削除した)
- 窓: `UIWindow(frame: UIScreen.main.bounds)` に Dialog の器を root として載せた。テストの実行体はシーンを 1 つも持たない (`connectedScenes` が 0) ため、シーンに載せた窓は作れない。シーンを持たない窓でも、安全領域は端末の実際の値 (402 × 874、上 62 / 下 34 / 左右 0) になった。上書きは一切していない
- 中身は 2 種類
  - 大きさが可変で理想の大きさ 200 × 100 を求める SwiftUI の中身 (`SafeAreaProbeSwiftUIContent`)。安全領域と矩形 (window 座標) は中身の一番外側の View で記録した。「無視あり」は一番外側に `.ignoresSafeArea(.container)` を付けたもの
  - 窓の幅を超える長さの英文 1 つの `Text` (`MutableSwiftUIContent` の `.text`)。この中身は安全領域を記録しないので、ホストの View が受け取った safe area (`hostSA`) を記録した
- 器の矩形は実効値の固定後 1 秒間 (50 ms 間隔で 20 回) 測った。全条件で 1 秒間の値は 1 種類だけだった

## 結果

| 基準領域・配置 | 中身 | 器の矩形 (x, y, 幅, 高さ) | 中身の矩形 | 安全領域 (中身が受け取った最後の値 / ホストの safe area) |
|---|---|---|---|---|
| 窓全体・上端 | 可変・無視なし | 101, 0, 200, 100 | 101, 62, 200, 38 | 上 62・下 0 |
| 窓全体・下端 | 可変・無視なし | 101, 774, 200, 100 | 101, 774, 200, 66 | 上 0・下 34 |
| 窓全体・上端 | 可変・無視あり | 101, 0, 200, 100 | 101, 0, 200, 100 | 0 (無視の内側で記録) |
| 窓全体・下端 | 可変・無視あり | 101, 774, 200, 100 | 101, 774, 200, 100 | 0 (同上) |
| 可視領域 (既定)・上端 | 可変 | 101, 62, 200, 100 | 101, 62, 200, 100 | 0 |
| 可視領域 (既定)・下端 | 可変 | 101, 740, 200, 100 | 101, 740, 200, 100 | 0 |
| 窓全体・上端 | 長い本文 | 6.67, 0, 389, 130.33 | — | ホスト: 上 62 |
| 窓全体・下端 | 長い本文 | 6.67, 743.67, 389, 130.33 | — | ホスト: 下 34 |
| 可視領域 (既定)・上端 | 長い本文 | 6.67, 62, 389, 130.33 | — | ホスト: 0 |

## 読み取れること

- 窓全体基準で端に付けた中身は、実際の窓でも器と安全領域の重なり (上端なら上 62、下端なら下 34) を受け取り、器の内側で安全領域を避けた (上端は y 62 から 38、下端は y 774 から 66)
- 器の矩形は要求サイズ (高さ 100) のままで、上端 y 0・下端 y 774 (874 − 100) から動かなかった
- 安全領域を無視する中身は器の矩形いっぱいに置かれた
- 既定の可視領域では、器は安全領域の内側に置かれ (上端 y 62・下端 y 740)、中身は安全領域を受け取らなかった
- 窓の幅を超える長い本文は、窓全体の上端・下端でも可視領域の上端と同じ 389 × 130.33 になった (1 行の高さに潰れない)。ホストは上端なら上 62、下端なら下 34 の安全領域を受け取っている

## 生ログの抜粋 (`scripts/log-sanitize.py` を通したもの。置換対象なし)

```
ZZREAL kind=window-top scenes=0 window=(0.0, 0.0, 402.0, 874.0) windowSA=UIEdgeInsets(top: 62.0, left: 0.0, bottom: 34.0, right: 0.0) container=(101.0, 0.0, 200.0, 100.0) distinctFrames1s=1 hostSA=UIEdgeInsets(top: 62.0, left: 0.0, bottom: 0.0, right: 0.0) contentFrame=Optional((101.0, 62.0, 200.0, 38.0)) contentInsets=t0.0 l0.0 b0.0 r0.0 → t62.0 l0.0 b0.0 r0.0
ZZREAL kind=window-bottom scenes=0 window=(0.0, 0.0, 402.0, 874.0) windowSA=UIEdgeInsets(top: 62.0, left: 0.0, bottom: 34.0, right: 0.0) container=(101.0, 774.0, 200.0, 100.0) distinctFrames1s=1 hostSA=UIEdgeInsets(top: 0.0, left: 0.0, bottom: 34.0, right: 0.0) contentFrame=Optional((101.0, 774.0, 200.0, 66.0)) contentInsets=t0.0 l0.0 b0.0 r0.0 → t0.0 l0.0 b34.0 r0.0
ZZREAL kind=window-top-ignores scenes=0 window=(0.0, 0.0, 402.0, 874.0) windowSA=UIEdgeInsets(top: 62.0, left: 0.0, bottom: 34.0, right: 0.0) container=(101.0, 0.0, 200.0, 100.0) distinctFrames1s=1 hostSA=UIEdgeInsets(top: 62.0, left: 0.0, bottom: 0.0, right: 0.0) contentFrame=Optional((101.0, 0.0, 200.0, 100.0)) contentInsets=t0.0 l0.0 b0.0 r0.0
ZZREAL kind=window-bottom-ignores scenes=0 window=(0.0, 0.0, 402.0, 874.0) windowSA=UIEdgeInsets(top: 62.0, left: 0.0, bottom: 34.0, right: 0.0) container=(101.0, 774.0, 200.0, 100.0) distinctFrames1s=1 hostSA=UIEdgeInsets(top: 0.0, left: 0.0, bottom: 34.0, right: 0.0) contentFrame=Optional((101.0, 774.0, 200.0, 100.0)) contentInsets=t0.0 l0.0 b0.0 r0.0
ZZREAL kind=visible-top scenes=0 window=(0.0, 0.0, 402.0, 874.0) windowSA=UIEdgeInsets(top: 62.0, left: 0.0, bottom: 34.0, right: 0.0) container=(101.0, 62.0, 200.0, 100.0) distinctFrames1s=1 hostSA=UIEdgeInsets(top: 0.0, left: 0.0, bottom: 0.0, right: 0.0) contentFrame=Optional((101.0, 62.0, 200.0, 100.0)) contentInsets=t0.0 l0.0 b0.0 r0.0
ZZREAL kind=visible-bottom scenes=0 window=(0.0, 0.0, 402.0, 874.0) windowSA=UIEdgeInsets(top: 62.0, left: 0.0, bottom: 34.0, right: 0.0) container=(101.0, 740.0, 200.0, 100.0) distinctFrames1s=1 hostSA=UIEdgeInsets(top: 0.0, left: 0.0, bottom: 0.0, right: 0.0) contentFrame=Optional((101.0, 740.0, 200.0, 100.0)) contentInsets=t0.0 l0.0 b0.0 r0.0
ZZREAL kind=text-window-top scenes=0 window=(0.0, 0.0, 402.0, 874.0) windowSA=UIEdgeInsets(top: 62.0, left: 0.0, bottom: 34.0, right: 0.0) container=(6.666666666666657, 0.0, 389.0, 130.33333333333334) distinctFrames1s=1 hostSA=UIEdgeInsets(top: 62.0, left: 0.0, bottom: 0.0, right: 0.0) contentFrame=nil contentInsets=
ZZREAL kind=text-window-bottom scenes=0 window=(0.0, 0.0, 402.0, 874.0) windowSA=UIEdgeInsets(top: 62.0, left: 0.0, bottom: 34.0, right: 0.0) container=(6.666666666666657, 743.6666666666666, 389.0, 130.33333333333334) distinctFrames1s=1 hostSA=UIEdgeInsets(top: 0.0, left: 0.0, bottom: 34.0, right: 0.0) contentFrame=nil contentInsets=
ZZREAL kind=text-visible-top scenes=0 window=(0.0, 0.0, 402.0, 874.0) windowSA=UIEdgeInsets(top: 62.0, left: 0.0, bottom: 34.0, right: 0.0) container=(6.666666666666657, 62.0, 389.0, 130.33333333333334) distinctFrames1s=1 hostSA=UIEdgeInsets(top: 0.0, left: 0.0, bottom: 0.0, right: 0.0) contentFrame=nil contentInsets=
✔ Test run with 1 test in 1 suite passed after 10.365 seconds.
```

`contentInsets` は届いた順の履歴 (`→` で区切る)。最初の 0 は、器が中身へ重なりを渡すと決める前 (添付値が届く前に既定の可視領域で置かれている間を含む) の値。長い本文の行の `contentFrame=nil contentInsets=` は、その中身が記録をしないため。
