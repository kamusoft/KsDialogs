---
scope: process
kind: success
severity: normal
count: 3
first-seen: 2026-09-07
last-seen: 2026-09-29
evidence:
  - add-kmp-typed-show (実装中に main で localize-dialog-error-messages が cross/ADR-0015 (診断文言は英語固定) を accepted にして実装・マージした。add-kmp-typed-show の spec は文言の言語を定めておらず、共有コードが新設した診断文言 2 件は日本語のまま review-003 APPROVED に達していた。オーナー指示で main 取り込み時に 2 件を英語化し、文言 assertion 11 件を追随させ、合流後に kmp allTests 151 / 0 failures と日本語リテラルの静的 grep 0 行を確認、deviation.md に記録して review-004 (合流分の限定レビュー) で APPROVED。ADR-0015 違反を main に持ち込まなかった)
  - fix-android-container-system-bar-appearance (develop への合流の直前に、分岐後の develop のコミットを列挙して、並行して入った revisit-dialog-margin-default が core/ADR-0039 (余白の既定値 0) を accepted にしていたことを検出。自 change も探索で core/ADR-0039 (器はシステムバーの指定を変えない) を起票・蒸留で accepted にしていたため、先に入った側を残して自分の ADR を 0040 へ振り直し、このブランチで足した参照 23 行だけを機械的に置換した。決定内容どうしの矛盾は無し。合流分は番号の振り直しと index・log の結合だけで、lint 7 種・Android JVM 75 / 0・Android / iOS のテストビルドで確認した (限定レビューは実施せず))
  - wait-for-host-appearance / align-sample-autoplay-start (develop への合流で、分岐の後に develop が accepted にした core/ADR-0039 (余白の既定値 0)・core/ADR-0040 (器は提示先のシステムバーの指定を変えない) と、このブランチの core/ADR-0039・0040 (提示先の出現待ち・中身は提示先を確保してから作る) の番号がぶつかった。こちらを 0041・0042 に振り直し、このブランチ側の参照 64 行だけを直した。develop 側の ADR への反しは無かったが、develop が足した iOS のテストがこのブランチで変えた提示面の呼び出しの形に合わずコンパイルエラーになり、合流時に直した)
---

## ルール文

change を main に合流させるとき (合流の直前)、着手後に `kasane/decisions/` で accepted になった ADR を `git log` (decisions/ 配下の追加・status 変更) で列挙し、自 change の成果物 (新設した文言・API・テスト・handbook の追記) がその決定に反していないかを 1 件ずつ確かめる。反していれば合流時に追随し、deviation.md に「合流時に ADR-NNNN へ追随」として記録して、合流分だけを対象にした限定レビューを受ける。守れたかは、合流コミットの本文または deviation.md に確認した ADR ID が書かれていることから判定する。

## 経緯

- 2026-09-07 add-kmp-typed-show: 並行して進んだ change が長命層の決定を先に確定させたケース。自 change の spec とレビューは合流前の時点では正しく、合流の瞬間にだけ検査点がある。ADR-0015 は静的 grep (`DM-KM-04`) で機械的に検出できたため、合流後の再実行で違反ゼロを確認できた
- 2026-09-27 fix-android-container-system-bar-appearance: 決定の内容ではなく ADR の番号が衝突した。番号は探索時に採番されるので、並行する change が同じ domain で ADR を起票すると合流の瞬間まで衝突が見えない。合流時の列挙は決定の中身の照合だけでなく、番号の重複の確認にも効く
- 2026-09-29 wait-for-host-appearance: 同じ型の別の現れとして、ADR の番号そのものがぶつかった。合流の直前の列挙で、番号の重なりも確かめる (`git log <分岐点>..<合流先> -- kasane/decisions/` の追加ファイル名と、自分の ADR の番号を突き合わせる)
