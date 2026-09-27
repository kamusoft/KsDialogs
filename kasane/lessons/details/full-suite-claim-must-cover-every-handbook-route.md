# 全件実行の主張を handbook の全件実行表と行ごとに突き合わせる — 経緯

出典 change: add-loading-toast-typed-show / rename-dialog-contract-singular / fix-android-container-system-bar-appearance

## 昇格前のルール文

実装報告が「テスト全件成功」「全ルート実行」を主張していたら、レビューはその主張を `kasane/handbook/cross/test-execution.md` の全件実行表と**行ごとに**突き合わせる — 表の各ビルドルート (JVM とは別に走る instrumented ルートを含む) に対応する件数・failures・skipped が報告に載っているかを確認し、載っていないルートがあれば「未実行」として指摘する。表にあるルートの結果が 1 行でも欠けた報告を全件成功として承認しない。守れたかは、review の実行表と handbook の全件実行表の行数が一致することから判定する。

## 経緯

- 2026-09-06 add-loading-toast-typed-show: 初回ゲートは規約どおり 3 台で回したが、修正サイクルの再実行で指揮側が「1 台以上で可」と緩めた。ルートの表は 1 周目だけでなく修正サイクルの再実行にも同じ行数で適用される — 指揮側が再実行の指示を書く時点で表の行を落とさないことも同じルールの範囲

- 2026-09-06 rename-dialog-contract-singular: 実装・レビュー・検証の 3 者が同じ見落とし (`./gradlew test` を Android の全件とみなす) をしており、ホスト側の多層チェックでは検出できなかった。handbook は「`./gradlew test` では instrumented が 1 件も走らない」と明記済みで、規約の側に不足はなく、報告を表と突き合わせる動作が欠けていた

- 2026-09-27 fix-android-container-system-bar-appearance: 担当範囲を Native 1 ルートに絞ったワーカーが、tasks の「全ルート」項目を自分の範囲だけで閉じた。完了主張と表の突き合わせは、レビューだけでなく tasks のチェックを受け取る指揮側にも要る
