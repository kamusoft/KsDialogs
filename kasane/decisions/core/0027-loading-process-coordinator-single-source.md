---
id: 0027
title: プロセス内 Loading coordinator を状態の唯一の正とし、全入口が委譲する
status: accepted
date: 2026-08-25
---

## Context

Loading の公開面には複数の入口がある: 既定シングルトン、契約 interface から構築した実装インスタンス (DI 用 — ADR-0002)、MAUI bridge、KMP actual。単一表示への合流 (ADR-0024) は、これらの入口をまたいで成立しなければならない。また並行する報告の「最新の報告が勝つ (後勝ち)」の「最新」を実装非依存に定義する必要がある。

## Decision

1 OS プロセス内に **Loading coordinator** (合流カウント・世代・表示中コンテンツ・最新メッセージ / 進捗の保持者。iOS `LoadingCoordinator` / Android `LoadingCoordinator`) を1つ置き、すべての入口がそこへ委譲する。

- 状態操作 (開始・終了・hide・メッセージ・進捗) は UI スレッド (main) 上で**受理順に直列化**し、「後勝ち」は coordinator の受理順で定義する
- 呼び出し面は任意スレッドから呼べる (内部で main へ移す — Dialog 面と同じ)
- 進捗受け口 (VM) とフォーマット関数の呼び出しも UI スレッド上で行う

## Alternatives Considered

- **各入口 (シングルトン / bridge / KMP actual) が独立に状態を持つ** — 却下: 入口をまたぐ利用で同一 OS 上に複数の Loading が表示され、単一表示・合流の契約 (ADR-0024) が成立しない
- **スレッド規則を実装任せにする** — 却下: 並行報告の「最新」が OS・形態ごとにぶれ、Scenario テストの期待値が固定できない

## Consequences

- 正: どの入口から使っても1つの表示に合流し、単一表示の契約が形態の境界を越えて成立する
- 正: 「後勝ち」が受理順という観察可能な定義を持ち、Scenario テストの期待値を固定できる
- 負: coordinator がプロセスグローバルな共有状態になり、テストは状態の分離・後始末に配慮が要る
- 負: 将来の入口追加 (新しい互換面等) は必ず coordinator への委譲として実装する制約を受ける

出典: kasane/changes/archive/2026-08-26-add-loading/design.md (Decision 8) / kasane/changes/archive/2026-08-26-add-loading/second-opinion-spec-001.md (Major: 複数入口の状態共有・並行報告の直列化)
