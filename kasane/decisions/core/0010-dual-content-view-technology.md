---
id: 0010
title: ダイアログコンテンツは従来 View 系 (Android.View / UIView) と宣言的 UI 系 (Compose / SwiftUI) の両対応を必須とする
status: accepted
date: 2026-08-17
---

## Context

Sample UI の議論で「samples Android の素の View 採用 (Compose 移行の要否)」を扱った際、オーナーが Sample の技術選択を超えたライブラリ本体の要件として宣言した。ダイアログのコンテンツ View をどの UI 技術で書けるかは、登録 API (VM 型キー → View factory、core/ADR-0004) が受け付ける型とネイティブ実装のホスティング機構を規定する契約級の決定である。

## Decision

- **Compose / SwiftUI でダイアログを書ける対応は必須**: Android は Jetpack Compose、iOS は SwiftUI でコンテンツを記述できること
- **Android.View / UIView 対応も必須**: 従来 View 系の対応がないと MAUI と連携できないため、宣言的 UI 系への一本化はしない
- 参考実装: KsSettingsView (同 AiForms シリーズリブランドの先例)
- 登録 API・ホスティングの具体設計は登録 API の決定 (core/ADR-0011) に委ねる

## Alternatives Considered

- **素の View のみ継続し、Compose 対応の要否判断を先送りする案** (アシスタント提案) — 却下。オーナー判断で Compose / SwiftUI 対応は必須要件であり、要否判断の先送り対象ではない
- **宣言的 UI 系への一本化** — 却下。従来 View 系がないと MAUI と連携できない

## Consequences

- 正: ネイティブ消費者 (Compose / SwiftUI 世代) とラッパー消費者 (MAUI) の双方が自然な技術でコンテンツを書ける
- 負: 登録 API とホスティングが2系統×2OS になり、API 表面・テスト・Sample のマトリクスが広がる (設計は core/ADR-0011)

出典: kasane/roadmaps/library-foundation/phases/phase-5-1-layout-spec/history.md (2026-08-17: Sample UI の未決4件)
