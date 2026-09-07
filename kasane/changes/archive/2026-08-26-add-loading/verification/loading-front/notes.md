# ダイアログ表示中の Loading が最前面になること — 実提示の証跡

tasks 6.3 の補完記録 (2026-08-26)。対象は dialog-contract デルタスペックの
Scenario **[LD-AT-04] ダイアログ表示中の Loading は最前面で入力を遮る** (「実提示での検証 — 両 OS」)。

## なぜ Sample で確かめる必要があったか

iOS 側の自動テスト `ios/Tests/KsDialogsTests/LoadingAttributeTests.swift` の
`LD_AT_04_loadingIsFrontmostOverPresentedDialog` は、window scene を持たないテスト実行環境では
UIKit の提示遷移が完走しないため、ダイアログの View を本番の全画面提示と同じ位置 (window の subview) へ
**直接載せる**置き方で代用している。そのため「本番の提示経路を通したダイアログの上に Loading が載るか」は
テストの中では観察できない。Android 側は instrumented テスト (実タップ注入) で担保済みだが、
両 OS の見え方を並べる意味でこちらでも同じ通しを撮った。

## 一時コードについて

Sample には「ダイアログを出したうえで Loading を重ねる」導線が無いため、**一時的に**
`Default Loading` の起動処理を次のように書き換えて確認し、撮影後に元へ戻した:

- ローディングを始める前に `Basic Dialog` を表示する (待たずに投げる)
- 1200 ms 待ってからローディングを開始する
- 観察と操作の時間を作るため、進捗の刻み間隔を 400 ms から 3000 ms へ伸ばす

対象は `samples/ios/KsDialogsSample/SampleMenuModel.swift` と
`samples/android/app/src/main/kotlin/jp/kamusoft/ksdialogs/samples/android/MainActivity.kt` の2ファイル。
**撮影後に両ファイルを元の内容へ戻し、チェックサム (md5) が一時改変前と一致することを確認した。**
戻したあとに両 Sample を再ビルドして通常の挙動 (ダイアログは出ず、400 ms 刻みで完了して `結果: 完了`)
に戻っていることも確認済み。

## 通した操作 (両 OS 同一)

1. 直近の結果を `結果: cancelled` にしておく (Loading の完了で入る `結果: 完了` と区別するため)
2. `Default Loading` を押す → ダイアログが出て、その上に Loading が重なる
3. **Loading 表示中にダイアログの `OK` ボタンの位置を突く**
4. Loading の完了を待つ
5. もう一度**同じ座標**で `OK` を突く

## 観察結果

| 観察点 | iOS (iPhone 17 Simulator) | Android (Pixel 4a 実機) |
|---|---|---|
| Loading がダイアログより手前に描かれる (回し物と文字がカードの上に出て、カードが覆いで暗くなる) | `ios-01-loading-over-dialog.png` | `android-01-loading-over-dialog.png` |
| 表示中に `OK` を突いても Loading は前面のまま、結果表示も変わらない | `ios-02-tap-on-dialog-blocked.png` | `android-02-tap-on-dialog-blocked.png` |
| Loading が消えた後もダイアログは残っており、結果は Loading 由来の `結果: 完了` (= `OK` は届いていなかった) | `ios-03-after-hide-dialog-remains.png` | `android-03-after-hide-dialog-remains.png` |
| 同じ座標を突くと今度はダイアログが応じ、`結果: completed(true)` になる | `ios-04-after-hide-tap-completed.png` | `android-04-after-hide-tap-completed.png` |

**判定: 両 OS で LD-AT-04 が実提示でも成立。** 3枚目が要になっている — 遮られていなければ
`OK` でダイアログが閉じて `結果: completed(true)` になるはずのところ、ダイアログは残ったままで
結果は Loading の `結果: 完了` だった。4枚目で同じ座標が効くことを見せているので、
「座標が外れていた」possibility は排除できている。

## 撮影の限界として残ること

- iOS の2枚目は、1コマの撮影に 1.4 秒ほどかかる関係で**同じ手順を別に一度通したときのコマ**を使っている
  (結果表示エリアの値がその回のもの = `結果: completed(true)` になっている)。
  「`OK` を突いた直後も Loading が前面にある」ことを示す目的には足りるが、
  1・3・4枚目と連続した一回の通しではない。Android の2枚目は同一の通しの中のコマ
  (`結果: cancelled` が背後に見えている)
- Android は実機のため、保存前にステータスバーの帯 (上端 132 px) を切り落としている。
  切り落とし以外の加工はしていない
