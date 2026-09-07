---
id: 0021
title: 型指定呼び出しの VM 解決はレジストリ登録の VM factory で行い、暗黙の既定コンストラクタ生成は採らない
status: accepted
date: 2026-08-24
---

## Context

型指定呼び出し (ライブラリが VM を生成し configure クロージャで初期化する経路 — core/ADR-0019・0020) では、VM インスタンスの解決元をライブラリが持つ必要がある。移植元は「DI 解決フック (viewResolver) があればそれ、なければ既定コンストラクタで生成 (Activator)」の2段だが、この形は C# のリフレクション前提であり、リフレクションを持たない Swift では「既定コンストラクタ」段を同型に表現できない。

一方、View の解決は既に「VM 型キー → View factory の明示レジストリ」(core/ADR-0004) で確立しており、リフレクション不要・全形態同型・登録漏れは構成ミスとして失敗、という原則がある。また MAUI には1行登録 `.RegisterForDialog<TView, TViewModel>()` を利用者向け主経路とする要件があり、この糖衣は IServiceCollection のチェーン上にあるため DI コンテナへのアクセスを自然に持つ。

## Decision

**レジストリに VM 型キー → VM factory を登録できるようにし (View factory と対)、型指定呼び出しの VM 解決はこの factory で行う。暗黙の既定コンストラクタ生成は採らない。**

- VM factory の登録は View factory と同じ明示レジストリに載せ、全形態 (iOS / Android / MAUI / KMP) で同型の「登録が正」モデルとする
- MAUI の1行登録は View factory と併せて「VM を DI コンテナから引く factory」を自動配線する — 利用者は1行で型指定呼び出しまで有効になる
- VM factory 未登録の型指定呼び出しは構成ミスとして失敗させる (core/ADR-0004 の登録漏れと同じ扱い)。暗黙の new で隠さない
- DI コンテナ連携 (Koin・手動 DI 等) は VM factory の中身 (`{ get() }` 等) として表現し、core 契約にコンテナ固有の知識を持ち込まない (core/ADR-0004 の「コンテナ別 adapter は将来のオプション」を踏襲)

## Alternatives Considered

- **DI 解決フックのみ (フック未設定は失敗)** — 却下。Native には「DI コンテナ」の標準が存在せず、形態ごとに解決フックの定義と設置手順が必要になり、同型性が崩れる
- **移植元同等の2段 (DI フック → 既定コンストラクタ)** — 却下。Swift はリフレクション不足で既定コンストラクタ段を同型表現できず、形態間で挙動が割れる。また暗黙の既定コンストラクタ生成は「DI に登録し忘れて依存が注入されていない VM が黙って生まれる」事故の温床で、構成ミスを失敗させる方針 (core/ADR-0004) と不整合

## Consequences

- 正: VM 解決が View 解決と同じ明示レジストリの原則に載り、全形態でリフレクション不要・同型・登録が正のモデルに統一される
- 正: MAUI 主経路 (1行登録) の利用者は追加手数ゼロで型指定呼び出しを得る
- 正: どの DI コンテナでも (コンテナなしでも) VM factory の中身として連携でき、core 契約が汚れない
- 負: Native で型指定呼び出しを使うには VM factory の明示登録が1行増える (インスタンス渡し show だけなら不要)
- 負: レジストリが View factory と VM factory の2種を持つことになり、内部表現と登録 API の面が増える
- 負: 移植元の「設定なしでも型指定呼び出しが動く」手軽さからの意図的乖離であり、移行者向けの説明が必要

出典: kasane/roadmaps/library-foundation/phases/phase-6-model-binding-di/history.md (2026-08-24: 型指定呼び出しの VM 解決元) / AiForms.Maui.Dialogs リポジトリ: Dialog/Dialog.cs・Configurations (2段解決の原典実装)
