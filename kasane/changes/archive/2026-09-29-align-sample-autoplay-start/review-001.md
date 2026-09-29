# レビュー結果: align-sample-autoplay-start (001 回目)

**日付**: 2026-09-29
**判定**: APPROVED

## サマリー

実装は deviation.md の決定と 1 対 1 で合っている。Android 系 3 ルートは最初の描画の後に再生する待ちを残し、説明を本当の理由 (起動画面の下で Toast の表示時間を使い切らないため) に書き換えている。android と kmp (Android) は実行時の動きが e2d4ac9 と同じで、コメントだけの差分である。MAUI は `#if ANDROID` で待ちを Android に限り、iOS では待たない。「提示先の出現を待つのはライブラリの役目」という説明の立場は、iOS 系 2 ルートの `SampleMenuScreen.swift` と揃っている。1 回限りの自動再生、作り直しで再発火しないこと、再生中のデモを画面の状態の変化で打ち切らないことも保たれている。指摘は証跡の読みやすさに関する Minor 1 件と Suggestion 1 件だけである。

## 照合した規約

- comment-policy.md (always): 許容参照、禁止参照 (作業文書のパス・change 名・通番)、禁止する記述類型 (履歴記述・構文キーワード)、公開 doc コメント、書き換えの 3 類型を節ごとに照合した。3 ファイルとも非公開メンバーへのコメントで、外部の ID に頼らず単独で読める。履歴の記述もない。`scripts/comment-policy-lint.py --advisory` で 3 ファイルの検出は 0 件
- sample-parity.md (`samples/` を触るとき): 撮影支援機構の節 (機構そのものの外部契約を 4 ルートで揃える)、撮影支援の起動引数 (1 回限り・再生成で繰り返さない)、自動再生はタップと同じ入口を使うこと、「してはいけないこと」(片側だけの改善・観測用の一時改変の戻し漏れ) を照合した。Android と iOS で待ちの有無が異なるが、これは deviation.md で合意済みの差分である。起動引数・安定 ID・倒れ方という外部契約は変わっていない。直す前の版を撮るための一時的な差し戻しは、evidence に md5 の一致確認の記録がある
- 関連する決定: core/ADR-0041 (proposed。ライブラリが提示先の出現を待つ)。コメントの立場はこの ADR と矛盾しない

## 確認した観点

- **deviation との一致**:
  - android と kmp (Android) は `menuView.post { play(demo) }` を残している (`samples/android/app/src/main/kotlin/jp/kamusoft/ksdialogs/samples/android/MainActivity.kt:102`・`samples/kmp/androidApp/src/main/kotlin/jp/kamusoft/ksdialogs/samples/kmp/android/MainActivity.kt:105`)
  - MAUI は Android だけ `Dispatcher.DispatchAsync` で待ち、それ以外は `await PlayAsync(demo)` を直接呼ぶ (`samples/maui/KsDialogs.Sample.Maui/SampleMenuPage.xaml.cs:95-102`)。対象の TFM は `net10.0-android;net10.0-ios` の 2 つなので、`#else` は iOS だけに当たる
- **1 回限り・再発火しないこと**:
  - android は `savedInstanceState == null` と、プロセス単位の `autoPlayConsumed` の二重の守りがある
  - kmp は `savedInstanceState == null` と、共有コードの `SampleCaptureAutoPlay.consumeDemo` (プロセス単位の `isConsumed`) で守っている
  - MAUI の `ConsumeDemo()` は待ちの前に同期で消費される。そのため、Dispatch を待つ間に `OnAppearing` が再び呼ばれても、二重には再生されない
  - evidence の作り直しの確認 (`font_scale` の変更による 2 回の再生成) とも合う
- **再生中のデモを打ち切らないこと**: MAUI の `PlayAsync` はページのライフサイクルに結び付いた取り消しを持たない。Kotlin 側は実行時の動きが変わっていない
- **MAUI の `async void` と例外**:
  - 直す前は `Dispatcher.Dispatch(async () => ...)` の async ラムダ、つまり実質 `async void` で例外を表面化させていた
  - 変更後は `DispatchAsync(Func<Task>)` が例外を Task に載せ、`async void AutoPlay` の `await` で UI の SynchronizationContext に投げ直す。メニュー項目のタップハンドラ (`async void On...Selected`) と同じ倒れ方で、既存コメント (`SampleMenuPage.xaml.cs:92-93`) の意図どおりである
  - 例外の握りつぶしはない
- **コメントの内容**:
  - 次の 3 点は、Android のライフサイクル (resume が最初の traversal より前) と evidence の実測に照らして妥当である: 受理の時点から数える表示時間、起動画面が退く前にそろう提示先、最初の描画の後へ回す待ち
  - 「提示先が起動画面の退場の後にそろうようになれば、この待ちは要らない」は、fix-android-startup-toast-under-splash のスコープと合う
  - kmp の「取り出しは 1 回限りなので、画面の再生成では再生しない」は事実と合っている
- **証跡の表と画像**:
  - 表の数と画像の説明を突き合わせた。「5 回中この回だけ」や「登録経路だけ」の記述は、表の 1 / 5 や 0 / 5 と整合している
  - 4 枚の画像を目視した (`android-custom-toast.png`・`kmp-android-default-toast-missing.png`・`third-maui-android-custom-toast.png`・`refire-3-recreated.png`)。いずれも説明どおりで、個人情報は写っていない
- **ビルドと lint**: 依頼元が実施済みで成功 (静的レビューのため、端末は使っていない)。comment-policy・local-path・identity の lint を手元で再実行し、検出はなかった

## 指摘事項

### 🟡 Minor 1 回目の実測の「変更後」が、最終の実装ではない途中版を指している

**該当箇所**: `evidence/autoplay-measurement.md:16`、`evidence/autoplay-measurement.md:61-66`・`:72-74`、`evidence/autoplay-measurement.md:87-93`

**問題点**:
- 1 回目の実測の見出しは「変更後」だが、Android 3 ルートの行は待ちを外した途中版 (`onCreate` / `OnAppearing` から直接呼ぶ版) で撮ったものである。オーナー判断 (deviation.md) の後、この版は最終の実装ではなくなった
- 同じ版で撮ったものには、Dialog・Loading の画像 (`android-*`・`kmp-android-*`・`maui-android-*` の dialog / loading) と、再発火の確認 (`refire-*`) もある
- 3 回目の節には「Dialog・Loading と MAUI iOS は撮り直していない」とある。しかし、最終版の Android 系で Dialog・Loading・再発火の確認がどの根拠で成り立つのかは書かれていない
- 最終版の android と kmp (Android) の Kotlin は、実行時の動きが e2d4ac9 と同じである (コメントだけの差分)。MAUI Android は直す前と同じ `Dispatch` の待ちである。どちらも本文からは読み取れない
- 蒸留時やアーカイブ後に読んだ人が、1 回目の表を最終版の実測だと誤読するおそれがある

**推奨修正**:
- 1 回目の節の冒頭か見出しに、「Android 系の行は、待ちを外した途中版の実測 (最終の実装ではない)」と注記する
- 3 回目の節に次の 2 点を 1〜2 文で足す
  - 最終版の android と kmp (Android) はコメントだけの変更で、実行時の動きは直す前と同じ。MAUI Android の待ちも直す前の `Dispatch` と同じ機構である
  - このため、Dialog・Loading と作り直しの確認は、途中版と直す前の版の結果で足りると判断した
- 再発火の確認の節 (`:87`) にも、どの版で行ったかを添える

### 🔵 Suggestion `post` の時点の書き方が evidence の中で揃っていない

**該当箇所**: `evidence/autoplay-measurement.md:54`、`evidence/autoplay-measurement.md:97`

**問題点**:
- 同じ `menuView.post` について、`:54` は「直す前の `post` は最初のレイアウトの後」、`:97` とコードのコメントは「最初の描画の後」と書いている
- `View.post` は、アタッチ前に積むと最初の traversal でハンドラへ回される。そのため、実際にはその traversal (描画の発行を含む) の後に走るのがふつうである。ただし、描画が見送られる経路もあり、画面に見えたことまでは保証しない
- 3 回目の実測でも、android の default-toast は 3 / 5 にとどまる。待ちは完全な対策ではない

**推奨修正**:
- evidence の 2 か所の書き方をどちらかに揃える (deviation.md に合わせるなら「最初の描画の後」)
- コードのコメントは deviation.md の言い回しどおりなので、変えなくてよい。必要なら「間に合わない回を減らす」程度の含みを持たせる選択肢もある (任意)

## アクションプラン

1. (Minor) evidence の 1 回目の実測と再発火の確認に、途中版で撮ったことを注記する。3 回目の節に、最終版の Android 系で Dialog・Loading・再発火の確認が成り立つ根拠 (実行時の動きが直す前と同じ) を足す
2. (Suggestion・任意) evidence の中で、`post` の時点の書き方を揃える
