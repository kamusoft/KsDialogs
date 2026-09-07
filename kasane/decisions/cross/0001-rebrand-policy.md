---
id: 0001
title: リブランド方針 — Native 主・互換 shim なし・独立ブランド
status: accepted
date: 2026-08-13
---

## Context

KsDialogs は AiForms.Maui.Dialogs のコンセプトを継承し、Native (Swift / Kotlin) + MAUI + KMP の3形態で使えるダイアログライブラリとしてリビルドする。移植元は MAUI 専用ライブラリであり、そのままでは純ネイティブ・KMP から使えない。同 AiForms シリーズのリブランド先例として KsSettingsView cross/ADR-0017 (Native ベースへの移植・リファイン) がある。

## Decision

以下の3原則をリブランド方針とする:

1. **Native 主** — Swift / Kotlin の Native 実装が主役。API 設計は各言語のイディオムを優先する。MAUI / KMP は Native を包む副形態とする
2. **互換 shim なし** — AiForms.Maui.Dialogs との API 互換レイヤー (旧 API 呼び出しを新実装へ橋渡しする層) は提供しない。乗り換えは KsDialogs の新 API での書き直しとする
3. **独立ブランド** — 移植元からは仕様と実装パターンのみ継承する独立ライブラリとする。対応する概念は原典の命名・使い心地を踏襲するが、互換は約束しない

KsSettingsView cross/ADR-0017 と同型の判断であり、その翻案として位置づける。

## Alternatives Considered

- **互換 shim を提供する案** — 却下。shim は旧 API の形 (MAUI 依存の設計・癖) を永久に背負い込み、Native 主での設計仕切り直しに旧 API の形が裏口から侵入する。概念・命名を継承するため移行コストはもともと小さく、shim の便益が保守コストに見合わない

## Consequences

- 正: 各言語のイディオムを優先した API 設計の自由度を確保できる
- 正: MAUI の将来性リスク (終息時の共倒れ) から独立する
- 正: KsSettingsView と同型の方針のため、先例の規約・知識 (cross 規約・パリティ規約) を再利用できる
- 負: 既存 AiForms.Maui.Dialogs 利用者はコード書き直しなしに乗り換えられない
- 負: 仕様継承のため、移植完了までは移植元 README (仕様の一次情報源) との二重参照コストを負う

出典: kasane/roadmaps/library-foundation/phases/phase-1-architecture-research/history.md (2026-08-13: リブランド方針 ADR の明文化) / KsSettingsView cross/ADR-0017 (翻案元)
