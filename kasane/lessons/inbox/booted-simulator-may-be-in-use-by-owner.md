---
scope: impl
kind: pain
severity: normal
count: 2
first-seen: 2026-09-02
last-seen: 2026-09-27
evidence:
  - fix-kmp-ios-unhandled-exception-crash (再現ループの実行先に Booted 状態の iPhone 17 を選んだところ、オーナーが別セッションで使用中で、terminate / launch の反復が相手の作業を壊す直前に止められた)
  - fix-maui-android-dialog-content-size (実配置テストホストを adb devices に出ていたエミュレータで起動したところ、別セッションが同じエミュレータで instrumented テストを実行中で、提示先の取り合いで自分の位置のシナリオが落ち、相手のテスト結果にも影響した可能性がある)
---

## ルール文

iOS Simulator / Android エミュレータで再現・撮影・テストを回すとき、`simctl list devices` の Booted や `adb devices` に出ている端末を「空いている」とみなして使わない — 自分で別のデバイス / AVD を起動して使い、終わったら shutdown / 終了する。起動済みの端末はオーナーや別セッションが使用中の可能性があり、terminate / launch / 提示の反復は相手の作業を破壊する。委譲するときはコンテキストパッケージにこの約束を両 OS ぶん書く。

## 経緯

- 2026-09-02 fix-kmp-ios-unhandled-exception-crash: オーナーの指摘で発覚。以後 iPhone 17 Pro を自前で boot して使用
- 2026-09-27 fix-maui-android-dialog-content-size: 実装ワーカーの報告で発覚。コンテキストパッケージに iOS 側の約束 (起動中の Simulator を流用しない) だけを書き、Android は「adb で操作してよい」としか書かなかった。ワーカーは途中で自前の AVD に切り替えた
