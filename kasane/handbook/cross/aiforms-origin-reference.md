---
kind: rule
applies-when:
  always: false
  tasks: [未移植の Dialog / Loading 機能の実装, Dialog / Loading の不具合・挙動差の調査]
title: 移植元 AiForms.Maui.Dialogs の参照
description: 移植元 (AiForms.Maui.Dialogs) を参照すべき場面と仕様の正の序列を定める時限規約。適用対象は Dialog / Loading、廃止は延期中
timestamp: 2026-09-26
---

# 移植元 AiForms.Maui.Dialogs の参照

この文書は、KsDialogs の移植元である AiForms.Maui.Dialogs を、どんなときに・どう参照すべきかのルールを定める。読むと、未移植機能の実装や不具合調査で「移植前の正」をどこで確認すればよいかが分かる。**時限規約**であり、廃止の要否は棚卸しのたびに見直す (「時限性」の節)。

## 成り立ちと目的

KsDialogs は .NET MAUI 用ライブラリ **AiForms.Maui.Dialogs** のコンセプトを継承し、Native (Swift / Kotlin) + MAUI + KMP で使えるダイアログライブラリとしてリビルドする独立ブランドである ([cross/ADR-0001](../../decisions/cross/0001-rebrand-policy.md))。ベタ移植はしない — 概念・API 形状・レイアウト計算アルゴリズムを継承し、実装は各形態で書き直す。

移植前の仕様・挙動の正は移植元にしか存在しない。移植元を参照せずに実装・調査すると、仕様の取り違え (意図された挙動を「バグ」と誤認する、移植時の退行を「仕様」と誤認する) が起こる。

## 参照先と仕様の正の序列

移植元は AiForms.Maui.Dialogs の1リポジトリ (upstream: `github.com/muak/AiForms.Maui.Dialogs`)。ローカルでは `../AiForms.Maui.Dialogs` で参照する (リポジトリ群が同じ親ディレクトリに clone されている前提)。

1. **README (711行)** — API リファレンスを含む仕様の一次情報源。まず README で仕様を確認する
2. **コード (本体約3,900行)** — レイアウト計算・アニメーションのタイミングなど README に書かれない詳細の補完。README と実装が食い違う場合、挙動の正はコード

## 参照するルール

- **未移植機能を実装するとき**: README の該当節と移植元の該当実装を読み、仕様・挙動を確認してから設計する
- **不具合・挙動差を調査するとき**: 移植元の同機能の挙動と突き合わせ、意図された仕様なのか移植時の退行なのかを判別する
- upstream で不具合修正が入っている可能性があるため、不具合調査では移植元の最新コミットに同種の修正がないかも確認する

## 適用範囲

- **対象: Dialog / Loading** — 移植元の仕様を継承する機能
- **対象外: Toast** — 原典で Obsolete のため仕様を継承せず、新実装で復活させた。Toast の仕様は [Toast の意味論](../../concepts/core/api/toast-semantics.md) が記述し、移植元は参照しない

## 時限性

当初は Dialog / Loading の移植完了 (library-foundation ロードマップ phase-7) で廃止する予定だった。phase-7 は 2026-08-26 に完了したが、2026-09-26 の ksn-drift の棚卸しでオーナーが廃止を延期した。移植完了後も、利用者アプリの移行 (AiForms.Maui.Dialogs → KsDialogs) で見つかる挙動差の調査に原典の挙動が使われている ([core/ADR-0038](../../decisions/core/0038-current-page-layout-area-via-registered-provider.md) の Context)。

廃止の要否は ksn-drift の棚卸しのたびに見直す。廃止するときはこの規約を削除し (成り立ちの記録は cross/ADR-0001 が持ち続ける)、[handbook cross の index.md](index.md) と [concepts の log.md](../../concepts/log.md) に廃止を記録する。concepts の core 契約のうち「移植元」「原典」の語をこの規約へのリンクで定義している文書 (layout / result-notification / multi-display / registration-show / transition の各意味論) は、同じ作業で定義の置き場を付け替える。

`timestamp` は移植状況と参照先を確認した日。

## してはいけないこと

- 移植元ソースを KsDialogs 内へコピー配置しない — upstream から再取得でき、スナップショットはすぐ腐る (KsSettingsView 版規約の判断を踏襲)
- 移植元との互換 shim (AiForms API をそのまま受け付ける互換層) を提供しない — 仕様と実装パターンだけを継承する独立ブランドであり、互換層は API 制約への引きずられを生む ([cross/ADR-0001](../../decisions/cross/0001-rebrand-policy.md))
- ローカルの絶対パスをこの文書や他の ADR / concepts / handbook に書かない — `../AiForms.Maui.Dialogs` の形で参照する

## 関連

- [cross/ADR-0001](../../decisions/cross/0001-rebrand-policy.md) — リブランド方針 (成り立ちの恒久記録)
