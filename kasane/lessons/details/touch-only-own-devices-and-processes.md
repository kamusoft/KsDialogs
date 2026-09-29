---
scope: impl
kind: pain
severity: normal
count: 3
first-seen: 2026-09-02
last-seen: 2026-09-28
evidence:
  - fix-kmp-ios-unhandled-exception-crash (再現ループの実行先に Booted 状態の iPhone 17 を選んだところ、オーナーが別セッションで使用中で、terminate / launch の反復が相手の作業を壊す直前に止められた。旧 inbox: booted-simulator-may-be-in-use-by-owner)
  - fix-maui-android-dialog-content-size (実配置テストホストを adb devices に出ていたエミュレータで起動したところ、別セッションが同じエミュレータで instrumented テストを実行中で、提示先の取り合いで自分の位置のシナリオが落ち、相手のテスト結果にも影響した可能性がある。旧 inbox: 同上)
  - wait-for-host-appearance (全形態テストのワーカーが、実配置テストホストの後片付けで `pkill -f "simctl launch --console-pty"` を実行し、同じ形で起動していた別セッションのプロセスも止めた可能性がある。その後、既存のエミュレータが adb の一覧から消えていた (因果は未確定)。旧 inbox: cleanup-must-target-own-processes-not-pattern)
---

## ルール文

iOS Simulator / Android エミュレータ・実機と、それらの上で動かすプロセスは、自分がこの作業のために作った・起動したものだけを操作する。起動中の端末 (`simctl list devices` の Booted・`adb devices` の一覧) を「空いている」とみなして使わず、作業専用の端末を作って使い、作業後に削除する。止めるときは、自分で起動したプロセスの PID、自分で作った Simulator の UDID・エミュレータのシリアルを指定し、`pkill -f`・`killall` などの名前やパターンの一致では止めない。委譲するときはコンテキストパッケージにこの約束を両 OS ぶん書く。守れたかは、作業報告に使った端末が作業専用として作成・削除されたことと、止めた対象が PID・UDID・シリアルで書かれていることから判定する。

## 経緯

- 2026-09-02 fix-kmp-ios-unhandled-exception-crash: オーナーの指摘で発覚。以後 iPhone 17 Pro を自前で boot して使用
- 2026-09-27 fix-maui-android-dialog-content-size: 実装ワーカーの報告で発覚。コンテキストパッケージに iOS 側の約束だけを書き、Android は「adb で操作してよい」としか書かなかった
- 2026-09-28 wait-for-host-appearance: 端末は専用に作っていたが、後片付けのプロセスの停止がパターン一致で、自分の作業の外まで届いた。入口は違うが、「別のセッションが使っている端末・プロセスに触って相手の作業を壊す」という同じ根っことして統合した (オーナー承認 2026-09-29)。パターン一致の停止は機械で止められるので、hook の設定案をあわせて提示した
