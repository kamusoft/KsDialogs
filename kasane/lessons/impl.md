---
scope: impl
timestamp: 2026-09-29
---

# lessons: impl

- [L-001] 証跡 (撮影画像・生成物の抜粋・ログ) の説明文 (手順・内訳・コマ見出し・「観測できない」という限界の記述) は、記憶や意図した手順からではなく**保存した実体そのものを開き直して**書く。保存時に各コマの実体 (どの画面・どの結果表示・どの向きか) と説明文を 1 コマずつ突き合わせ、md5 の重複検査を行う。別ルート・別端末・別観測回を名乗る画像が同一画素になっていたら、同一である旨の開示では裏付けにならない — 識別要素 (ステータスバーの時計・観測回が分かる画面差) を残して撮り直す (個人情報は通知アイコンだけを落とす等で避ける)。意図と違う操作をしてしまった場合は撮り直すか、実体の側に合わせて記録を訂正する (意図した手順のまま書かない)。守れたかは、証跡ディレクトリ内の md5 の相異と、説明文の各行が対応する実体ファイルを指せることから判定する。([経緯](details/evidence-notes-must-match-captured-artifacts.md)) (昇格: 2026-09-04、出典: add-sample-capture-automation / add-kmp-loading-toast-throws / fix-sample-android-back-and-rotation)
- [L-002] iOS Simulator / Android エミュレータ・実機と、それらの上で動かすプロセスは、自分がこの作業のために作った・起動したものだけを操作する。起動中の端末 (`simctl list devices` の Booted・`adb devices` の一覧) を「空いている」とみなして使わず、作業専用の端末を作って使い、作業後に削除する。止めるときは、自分で起動したプロセスの PID、自分で作った Simulator の UDID・エミュレータのシリアルを指定し、`pkill -f`・`killall` などの名前やパターンの一致では止めない。委譲するときはコンテキストパッケージにこの約束を両 OS ぶん書く。守れたかは、作業報告に使った端末が作業専用として作成・削除されたことと、止めた対象が PID・UDID・シリアルで書かれていることから判定する。([経緯](details/touch-only-own-devices-and-processes.md)) (昇格: 2026-09-29、出典: fix-kmp-ios-unhandled-exception-crash / fix-maui-android-dialog-content-size / wait-for-host-appearance)
