---
id: 0007
title: レイアウト計算は観察可能な規則を core 仕様として1本化し、実装は各 OS のレイアウト機構に委ねる
status: accepted
date: 2026-08-13
---

## Context

移植元のレイアウトは、属性が `ExtraView` 共通基底 (ProportionalWidth/Height・Vertical/HorizontalLayoutAlignment・OffsetX/Y・CornerRadius・Border・DialogMargin 等) に集約される一方、Measure アルゴリズムは iOS / Android でほぼ行対応のコピー (本質差はデバイスサイズ取得のみ) で、計算が共通層・iOS・Android の3箇所に散在している — 移植時の主要な負債。この計算を3形態 (Native 2実装 + MAUI/KMP ラッパー) でどう共通化するかが論点。

## Decision

共通化の対象を計算コードではなく**観察可能な規則**とする:

- **core 仕様 (concepts) に1本化するもの**: 属性セット (原典踏襲の命名) と、サイズ・配置の決定規則 — 優先順位とクランプ規則 (Proportional 指定 = 親に対する比率 / Fill = 画面幅からマージン控除 / 未指定 = 内容サイズを画面内にクランプ)。原典の重複コードの中身をルール文として抽出する
- **実装は各 OS のレイアウト機構で自由**: iOS = AutoLayout / constraints、Android = LayoutParams / Compose modifier 等で規則を満たせばよい。原典の手動 Measure は MAUI 仮想 View 都合の実装であり、同型移植を要求しない
- **一貫性の担保**: 共通仕様テスト (同じ属性入力 → 期待されるサイズ・位置) を3形態に同型で課す。Sample パリティ規約と同じ発想
- 属性の取捨 (どの属性を残すか) は後続の仕様化作業で確定する (その後 core/ADR-0014 で確定済み)

## Alternatives Considered

- **計算ロジックを共有コード化する案** — 却下。共有 core の再導入であり core/ADR-0001 と矛盾する (MAUI は共有コードを消費できず、純 Swift 利用者に Kotlin ランタイムを背負わせる)
- **原典アルゴリズムの同型移植を規範化する案** — 却下。iOS / Android コピペ構図の負債をそのまま継承し、各 OS のレイアウト機構を活かせない

## Consequences

- 正: 規則1本が正となり、原典の「3箇所散在」の負債を構造的に解消する
- 正: 各 OS がネイティブのレイアウト機構を素直に使え、実装が簡潔になる
- 負: 挙動一貫性が共通仕様テストの整備に依存する — テストがなければ規則は絵に描いた餅 (Sample パリティ規約と同じ「実物を並べて突き合わせる」仕組みで担保する)
- 負: 規則文の曖昧さがそのまま実装差になるため、仕様の記述精度に対する要求が高い

出典: kasane/roadmaps/library-foundation/phases/phase-1-architecture-research/history.md (2026-08-13: レイアウト計算の共通仕様化) / artifacts/scout-origin-api-surface.md (ExtraView 属性一覧と Measure 重複の実測)
