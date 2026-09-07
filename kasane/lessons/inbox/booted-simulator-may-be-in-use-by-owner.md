---
scope: impl
kind: pain
severity: normal
count: 1
first-seen: 2026-09-02
last-seen: 2026-09-02
evidence:
  - fix-kmp-ios-unhandled-exception-crash (再現ループの実行先に Booted 状態の iPhone 17 を選んだところ、オーナーが別セッションで使用中で、terminate / launch の反復が相手の作業を壊す直前に止められた)
---

## ルール文

iOS Simulator で再現・撮影・テストを回すとき、`simctl list devices` で Booted になっているデバイスを「空いている」とみなして使わない — 同じ OS の別デバイスを自分で `simctl boot` して使い、終わったら shutdown する。Booted はオーナーや別セッションが使用中の可能性があり、terminate / launch / shutdown の反復は相手の作業を破壊する。

## 経緯

- 2026-09-02 fix-kmp-ios-unhandled-exception-crash: オーナーの指摘で発覚。以後 iPhone 17 Pro を自前で boot して使用
