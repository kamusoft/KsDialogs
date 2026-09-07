---
id: 0006
title: 多段表示は OS の提示機構への委譲とし、契約は観察可能な意味論のみを規定する
status: accepted
date: 2026-08-13
---

## Context

移植元は「ダイアログの上にダイアログ」の多段表示を、明示的なスタック管理コードなしで実現している — iOS は ViewController の presentation、Android は Dialog ウィンドウという OS の提示機構への委譲による。ロードマップの前提として、挙動としての多段表示は継承が合意済み。core/ADR-0001 により Native 2実装が土台であり、各 OS の提示機構が本業として多段管理を担う。

## Decision

ライブラリはスタックを管理せず、OS の提示機構への委譲を踏襲する。core 契約 (仕様) には観察可能な意味論のみを規定する:

1. ダイアログ表示中でも show を呼べる (多段可)
2. 各 show は独立に結果を返す非同期操作である (core/ADR-0003 の意味論が多段でも成立する)
3. 後から出たダイアログが手前に重なり、閉じる順序は各 OS の提示機構に従う
4. ライブラリは「今何段出ているか」の状態を持たない・公開しない
5. 下の段を先に閉じたときに**どの段が画面から消えるか**は保証しない — OS の提示機構により異なる。保証するのは意味論2 (各 show の結果がちょうど1回返ること) のみで、見え方の差は OS 差として仕様に明記する
6. 結果報告を経ずに器が OS 側の都合で画面から外れた場合、未確定の show は cancelled で確定する — OS へ委譲する以上「閉じたのに結果が返らない」経路が生じうるため、意味論2 をこの規則で担保する

OS 間の挙動差は共通仕様テストで記録し、仕様側に明記して吸収する。

## Alternatives Considered

- **明示スタック管理を契約化する案** — 却下。OS の提示機構と二重管理になり喧嘩のリスクを負う。厳密な z-order 制御や「全部閉じる」等の拡張は原典に存在せず、必要になれば非破壊の追加機能として後から導入できる

## Consequences

- 正: 原典と同じ構図で挙動継承が素直に成立する
- 正: ライブラリが状態を持たず、Native 実装・KMP 層とも薄く保てる
- 負: OS 差 (提示アニメーション・閉じる順序の細部等) が挙動に漏れうる — 共通仕様テストでの挙動差の記録が必須になる
- 負: 下の段を先に閉じたときの見え方が OS ごとに割れる (iOS は提示の連なりごと外れて上下とも消え、Android は下の段だけが閉じる)。両 OS 共通に約束できるのは「各 show の結果がちょうど1回返ること」までで、段の見え方は platform 依存として利用者に開示することになる
- 負: OS 側の都合で器だけが消える経路 (提示関係の解除・画面の破棄・ウィンドウの取り外し) を各 Native 実装が個別に検知する必要があり、検知漏れは show の宙吊りとして現れる
- 負: 「全部閉じる」等のスタック横断操作を将来入れる場合、OS 委譲の枠内で設計する制約を負う

出典: kasane/roadmaps/library-foundation/phases/phase-1-architecture-research/history.md (2026-08-13: 多段表示の core 契約表現) / kasane/roadmaps/library-foundation/roadmap.md (前提: 移植元の実装知識)

現行照合: 2026-08-15 確認。ios/Sources/KsDialogs/Presentation/UIKitDialogPresentationSurface.swift と android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/ActivityDialogPresentationSurface.kt が OS の提示機構へ委譲し、段数の状態を持たない。器の消失検知は DialogContainerViewController.swift / ActivityDestroyObserver.kt が担う。判定: 維持
