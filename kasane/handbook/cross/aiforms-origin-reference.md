---
kind: rule
applies-when:
  always: false
  tasks: [未移植の Dialog / Loading 機能の実装, Dialog / Loading の不具合・挙動差の調査]
title: 移植元 AiForms.Maui.Dialogs の参照
description: 移植元 (AiForms.Maui.Dialogs) を参照すべき場面と仕様の正の序列を定める時限規約。適用対象は Dialog / Loading、移植完了で廃止
timestamp: 2026-08-14
---

# 移植元 AiForms.Maui.Dialogs の参照

この文書は、KsDialogs の移植元である AiForms.Maui.Dialogs を、どんなときに・どう参照すべきかのルールを定める。読むと、未移植機能の実装や不具合調査で「移植前の正」をどこで確認すればよいかが分かる。**Dialog / Loading の移植が完全に終わるまでの時限規約**である。

## 成り立ちと目的

KsDialogs は .NET MAUI 用ライブラリ **AiForms.Maui.Dialogs** のコンセプトを継承し、Native (Swift / Kotlin) + MAUI + KMP で使えるダイアログライブラリとしてリビルドする独立ブランドである ([cross/ADR-0001](../../decisions/cross/0001-rebrand-policy.md))。ベタ移植はしない — 概念・API 形状・レイアウト計算アルゴリズムを継承し、実装は各形態で書き直す。

移植前の仕様・挙動の正は移植元にしか存在しない。移植元を参照せずに実装・調査すると、仕様の取り違え (意図された挙動を「バグ」と誤認する、移植時の退行を「仕様」と誤認する) が起こる。

## 参照先と仕様の正の序列

移植元は AiForms.Maui.Dialogs の1リポジトリ (upstream: `github.com/muak/AiForms.Maui.Dialogs`)。ローカルパスは [参考リポジトリの在り処](../../concepts/cross/reference/reference-repositories.md) の対応表で解決する — 本文へのパス直書きは同規約で禁止されている。

1. **README (711行)** — API リファレンスを含む仕様の一次情報源。まず README で仕様を確認する
2. **コード (本体約3,900行)** — レイアウト計算・アニメーションのタイミングなど README に書かれない詳細の補完。README と実装が食い違う場合、挙動の正はコード

## 参照するルール

- **未移植機能を実装するとき**: README の該当節と移植元の該当実装を読み、仕様・挙動を確認してから設計する
- **不具合・挙動差を調査するとき**: 移植元の同機能の挙動と突き合わせ、意図された仕様なのか移植時の退行なのかを判別する
- upstream で不具合修正が入っている可能性があるため、不具合調査では移植元の最新コミットに同種の修正がないかも確認する

## 適用範囲

- **対象: Dialog / Loading** — 移植元の仕様を継承する機能
- **対象外: Toast** — 原典で Obsolete のため仕様を継承せず、新実装で復活させる (library-foundation ロードマップの非ゴール)。Toast の仕様の正は本規約の範囲外で、phase-8-toast-rebuild で新たに定める

## 時限性

Dialog / Loading の移植完了 (library-foundation ロードマップ phase-7 完了想定) で役目を終える。終了時はこの規約を削除し (成り立ちの記録は cross/ADR-0001 が持ち続ける)、[handbook cross の index.md](index.md) と [concepts の log.md](../../concepts/log.md) に廃止を記録する。`timestamp` は移植状況と参照先を確認した日。高腐食 (移植の進行とともに古くなりやすい) だが高価値の規約として、ksn-drift の重点棚卸し対象とする。

## してはいけないこと

- 移植元ソースを KsDialogs 内へコピー配置しない — upstream から再取得でき、スナップショットはすぐ腐る (KsSettingsView 版規約の判断を踏襲)
- 移植元との互換 shim (AiForms API をそのまま受け付ける互換層) を提供しない — 仕様と実装パターンだけを継承する独立ブランドであり、互換層は API 制約への引きずられを生む ([cross/ADR-0001](../../decisions/cross/0001-rebrand-policy.md))
- ローカルパスをこの文書や他の ADR / concepts / handbook に直書きしない — [参考リポジトリの在り処](../../concepts/cross/reference/reference-repositories.md) に集約

## 関連

- [参考リポジトリの在り処](../../concepts/cross/reference/reference-repositories.md) — ローカルパスの解決先
- [cross/ADR-0001](../../decisions/cross/0001-rebrand-policy.md) — リブランド方針 (成り立ちの恒久記録)
