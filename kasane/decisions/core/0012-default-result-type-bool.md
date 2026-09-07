---
id: 0012
title: 結果型は Bool を既定とし、カスタム結果型は宣言した VM だけが持つ
status: accepted
date: 2026-08-17
---

## Context

core/ADR-0003 で結果通知は型付き (`DialogResult<TResult>`) と決定済み。だが利用実態は「戻り値は true / false で使うことがほとんどで、一部カスタム型を使いたいニーズ」(オーナー) であり、全 VM に結果型の宣言を課す現状は、多数派の bool 利用に少数派向けの儀式 (MAUI の型引数2つ明示・結果型の宣言) を強要していた。

## Decision

VM が結果型を宣言しなければ **bool 扱い**とする。形態別の表現:

- **iOS (Swift)**: `protocol DialogViewModel: Sendable { associatedtype Result: Sendable = Bool }` — デフォルト associatedtype。Result 宣言なしの VM は Bool になり、register の notifier は自動で `DialogNotifier<Bool>` に型付く
- **Android (Kotlin)**: 同名の型引数違い宣言が言語上不可のため、bool 既定の顔として `typealias SimpleDialogViewModel = DialogViewModel<Boolean>` を用意する。register は従来から型境界推論のため変更不要
- **MAUI (C#)**: 非ジェネリック `IDialogViewModel : IDialogViewModel<bool>` を追加し、VM 単型引数の `Register<TViewModel>` オーバーロード (factory は `DialogNotifier<bool>` 固定) を設ける。カスタム型用の2型引数版と共存し、オーバーロード解決が曖昧にならないことを実験で確認済み
- **KMP Swift 面 (kmp/ADR-0004)**: `result:` 引数を省略した場合 Bool とするオーバーロードを追加
- show の戻りは bool 既定 VM なら `DialogResult<bool>` に自動で型付く

なお notifier **引数そのもの**の省略は本決定の範囲外 — View が自前生成した notifier は show の結果配線と繋がらないため成立せず、「notifier を VM に注入する」設計 (原典 BindingContext 方式の先例) として後続の ViewModel ライフサイクル設計 (model-binding / DI 連携) に織り込んだ。

## Alternatives Considered

- **全 VM に結果型宣言を課す現状維持** — 却下。多数派の bool 利用に低頻度形の儀式を強要する
- **View コンストラクタでの notifier 自前生成による省略** (オーナー案) — 不成立。自前 new した notifier はどの show とも結線されず、結果が届かない。VM 注入設計 (後続の ViewModel ライフサイクル設計) へ

## Consequences

- 正: bool 利用の登録・VM 宣言から型の儀式が消える (MAUI は型引数1つ、Swift / Kotlin は宣言なし)
- 正: カスタム結果型の経路は従来のまま非破壊で共存する
- 負: VM 契約が「bool 既定の顔」と「明示宣言の顔」の2面になり、ドキュメントでの説明責任が増える
- 負: Kotlin は言語制約により typealias の別名が1つ増える

出典: kasane/roadmaps/library-foundation/phases/phase-5-2-api-surface/history.md (2026-08-17: 既定結果型 Bool と notifier 省略の行き先)
