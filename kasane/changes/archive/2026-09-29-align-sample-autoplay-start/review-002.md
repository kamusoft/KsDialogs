# レビュー結果: align-sample-autoplay-start (002 回目)

**日付**: 2026-09-29
**判定**: APPROVED

## サマリー

review-001 の指摘 2 件への対応で直された `evidence/autoplay-measurement.md` を全文読み直した。Minor (1 回目の実測の「変更後」が途中版だと書かれていない) は、1 回目の節の冒頭の注記で解消している。Suggestion (`post` の時点の書き方の揺れ) は「最初の描画の後」に揃っていて、解消している。注記の内容は、deviation.md と今の 3 つの Sample のコードに照らして事実と合う。他の節とも食い違いはない。残るのは、任意の Suggestion 1 件だけである。

## 照合した規約

- comment-policy.md (always): 今回の修正は証跡だけで、コードは変わっていない。このため照合は review-001 の結果を引き継ぐ
- sample-parity.md (`samples/` を触るとき): 修正された証跡の記述が、撮影支援機構の外部契約 (1 回限り・再生成で繰り返さない) の説明と矛盾しないかだけを見た
- 前提の確認として、3 つの Sample のファイルの更新時刻が review-001 より前であることを確かめた。行の位置 (`menuView.post` が android の `:102`・kmp の `:105`、MAUI の `#if ANDROID` が `:95-103`) も review-001 の時点と同じで、コードは変わっていない。このためビルドとテストは再実行していない。`scripts/local-path-lint.py`・`scripts/identity-lint.py` を evidence に掛け、検出は 0 件だった

## 解消確認表

| review-001 の指摘 | 重要度 | 状態 | 確認した内容 |
|---|---|---|---|
| 1 回目の実測の「変更後」が、最終の実装ではない途中版を指している | 🟡 Minor | 解消 | 1 回目の節の冒頭 (`evidence/autoplay-measurement.md:18`) に注記がある。対象は 1 回目と 2 回目の節の「変更後」、1 回目の Android 系の Dialog・Loading の画像、`refire-*` で、いずれも待ちを外した途中版で撮ったと明記した。あわせて、最終版の実行時の動きが直す前と同じであること、MAUI iOS の行が最終版と同じ形であること、再発火の守りが最終版でも変わらないことも書かれている。推奨のうち「3 回目の節に根拠を足す」「再発火の節に版を添える」は、節を分けず、この注記 1 か所にまとめる形で反映された。誤読のおそれは、表の手前で読み手が注記に当たるので解消している (根拠の書き方の残りは下の Suggestion) |
| `post` の時点の書き方が evidence の中で揃っていない | 🔵 Suggestion | 解消 | `:56` が「直す前の `post` は最初の描画の後」に改まり、`:97`・`:99`・`:114` とコードのコメントの「最初の描画の後」に揃った。「最初のレイアウトの後」は evidence から消えている |

## 注記の事実確認

`evidence/autoplay-measurement.md:18` の各主張を、deviation.md と今のコードに照らした。

- **Android 系の途中版は待ちを外していた**: 2 回目の表の版の欄 (`:38`・`:40`・`:42`) は、「`onCreate` から直接」「`OnAppearing` から直接」となっている。deviation.md の「exploration では 3 ルートとも待ちを外す → オーナー判断により Android 系は待ちを残す」とも合う
- **android と kmp (Android) の実行時の動きは直す前と同じ `post`**: HEAD との差分は、2 ファイルともコメント行の追加と書き換えだけである。`menuView.post { play(demo) }` (`samples/android/app/src/main/kotlin/jp/kamusoft/ksdialogs/samples/android/MainActivity.kt:102`・`samples/kmp/androidApp/src/main/kotlin/jp/kamusoft/ksdialogs/samples/kmp/android/MainActivity.kt:105`) と、その呼び出し条件は変わっていない。事実と合う
- **MAUI Android は直す前と同じく次の周回へ回す (`Dispatch` を `DispatchAsync` にした)**: `samples/maui/KsDialogs.Sample.Maui/SampleMenuPage.xaml.cs:95-100` は、Android のときだけ `Dispatcher.DispatchAsync` を使う。これは内部で `Dispatch` に積むので、受理を遅らせる機構は直す前と同じである。`ConsumeDemo()` が待ちの前に同期で呼ばれる順序も、直す前から変わっていない。事実と合う
- **MAUI iOS の行は最終版と同じ形**: `#else` の枝 (`:102`) は `OnAppearing` から待たずに `await PlayAsync(demo)` を呼ぶ。対象の TFM は Android と iOS の 2 つなので、iOS はこの枝に当たる。3 回目の節の「iOS の形は 1 回目の実測から変えていない」(`:99`) とも合う
- **再発火の守りは最終版でも変わらない**: android は `savedInstanceState == null` の判定と、プロセス単位の `autoPlayConsumed` で守っている。kmp (Android) は `savedInstanceState == null` の判定と、共有コードの `SampleCaptureAutoPlay.consumeDemo` で守っている。どちらの行も今回の差分に含まれない。再発火の確認は android で行っており (`:89` 以降)、`:94` の説明「`savedInstanceState` が渡るため分岐に入らず、1 回限りの取り出しも消費済み」とも合う

## 他の節との整合

- 1 回目の見出し (`:16`) の「変更後」は残っているが、直後の注記で範囲が示されている。2 回目の表 (`:38`・`:40`・`:42`) の版の欄も、もともと「直接」の版と明記されている。注記と食い違いはない
- 注記の「それらの画像」は、画像の表 (`:61-87`) の Android 系の Dialog・Loading (1 回目)、Toast (2 回目の「変更後」)、`refire-*` を指す。3 回目の画像 (`:117-126`) は別の表に分かれている。どの画像がどの版かを取り違えるおそれはない
- 3 回目の節の「直す前 (2 回目の比較)」の行 (`:107`・`:109`・`:111`) は、2 回目の表の直す前の行 (`:39`・`:41`・`:43`) と数値が一致する
- `:114` の「最初の描画の後に回しても、描画から起動画面の退場までの間に表示時間が尽きる回は残る」は、`:56` の見立て (再開は最初の描画と起動画面の退場より前に来る) と矛盾しない

## 指摘事項

### 🔵 Suggestion 最終版の Dialog・Loading を撮り直さなくてよい理由が、推論に任されている

**該当箇所**: `evidence/autoplay-measurement.md:18`、`evidence/autoplay-measurement.md:99`

**問題点**:
- 注記は「最終版の実行時の動きは直す前と同じ」という事実を書いている。ただし、この change で撮った Dialog・Loading は途中版のものだけで、直す前の版の Dialog・Loading は撮っていない
- そのため、最終版の Android 系で Dialog・Loading が出ることの根拠は、読み手の推論に任されている。推論の中身は次の 2 つである
  - 直す前の版は、これまで使われてきた形そのものである
  - 寿命を持たない Dialog と、処理の間出続ける Loading は、受理を最初の描画の後へ遅らせても出なくなる理由がない
- `:99` の「Dialog・Loading と MAUI iOS は撮り直していない」から `:18` の注記への参照もない

**推奨修正 (任意)**:
- `:99` の末尾に、「理由は 1 回目の節の注記のとおり。寿命を持たない Dialog・Loading は、受理を最初の描画の後へ遅らせても出方が変わらない」程度の 1 文を足す
- 足さなくても誤読のおそれは解消しているので、このままでもよい

## アクションプラン

1. (Suggestion・任意) 3 回目の節の「撮り直していない」に、撮り直さなくてよい理由を 1 文添える
