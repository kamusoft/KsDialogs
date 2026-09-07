---
id: 0003
title: Swift 向け KMP 登録 API は Swift パッケージ側に置き、klib に公開面を持たない
status: accepted
date: 2026-08-17
---

## Context

KMP 利用アプリで共有コード (Kotlin commonMain) の ViewModel に対する View factory を登録するのは、View (UIView) の実装を持つアプリの Swift 側である。この Swift 向け登録 API をどの配布物に置くかが API 表面設計を左右する。

配布モデル検討の PoC (cross/ADR-0008 の検証) で確定した構造的事実:

- KMP 消費者はライブラリの framework を受け取らず、Maven の klib から**自分の framework を自分でビルドする**。klib 側に置いた Kotlin API を Swift から見せるには、消費者が自分の framework 設定に `export(...)` を書く必要がある
- 消費者アプリは登録のためにすでに iOS Native の Swift パッケージを直接リンクしている (cross/ADR-0008 の「アプリ側 SwiftPM 1点」)

現状の登録入口は、純 Swift 向けの型付き API (`DialogViewRegistry.register<ViewModel>`) と、KMP cinterop 委譲用の機械面 (`KsDialogsInteropBridge.registerViewFactory`、ObjC 互換・型消去) の2つ。verify-001 ❌3 で KMP iOS Sample が後者の機械面を利用者コードとして直接使っている deviation が検出されている。

## Decision

- KMP 利用者向けの公開登録 API は **iOS Native の Swift パッケージ側**に置く。klib (commonMain / iosMain) には Swift 向けの公開登録面を持たない
- `KsDialogsInteropBridge` は KMP cinterop 委譲専用の機械面として残し、利用者向け API とはしない。利用者向け公開 API は機械面を包む形で Swift パッケージ内に別途設計する
- API の具体的な形 (名前・シグネチャ) は kmp/ADR-0004 で確定する。KMP iOS Sample の機械面直接利用は新 API へ差し替える

## Alternatives Considered

- **klib (KMP) 側に登録 API を置く**: 却下。消費者の framework に `export(...)` 設定を強要して手数と framework 肥大を押し付け、ObjC 経由でジェネリクスが劣化する。kmp/ADR-0002 の「レジストリ実体は Native lib 側・KMP は薄い契約」も崩れる
- **Swift パッケージと klib の両方に置く**: 却下。同一レジストリへの入口が2つになり、登録先の混乱と二重メンテを生む

## Consequences

- 正: 消費者は追加のビルド設定なしで、すでにリンクしている Swift パッケージから登録できる
- 正: 登録の入口が Swift 側1つに定まり、kmp/ADR-0002 のレジストリ一意性がそのまま保たれる
- 正: API を Swift らしい形 (closure・命名・型) で設計できる
- 負: 結果型が自己申告になる制約 (kmp/ADR-0002 の既知の負) は置き場所に依らず残る
- 負: KMP iOS 利用者にとって Swift パッケージのリンクが必須である構造 (cross/ADR-0008) を前提とする。配布単位を見直す場合は本 ADR も連動して見直す

出典: kasane/roadmaps/library-foundation/phases/phase-10-packaging-model/history.md (2026-08-17: 論点E)
