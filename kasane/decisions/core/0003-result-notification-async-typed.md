---
id: 0003
title: 結果通知は async 単発 + 型付き結果とし、completed / cancelled を型で区別する
status: accepted
date: 2026-08-13
---

## Context

移植元の結果通知は DialogView が自動生成する DialogNotifier (`Complete()` / `Complete<T>(result)` / `Cancel()`) を内部イベント経由で TaskCompletionSource が購読し、`await ShowAsync()` を解決する設計 (async/await 対応済み)。ただし2つの粗がある: (1) 結果が `object` 経由の `(TResult)e.Result` キャストで型安全でなく、`ShowResultAsync<T>` 中に引数なし `Complete()` を呼ぶとキャスト例外の可能性がある。(2) キャンセル時は `default(TResult)` が返り「キャンセル」と「結果が default 値」を区別できない。

DialogNotifier は MAUI の BindableProperty (OneWayToSource) に依存しており、Native / KMP 版では通知経路の再設計が必要。

## Decision

core 契約として、show は「**completed(結果) または cancelled を1回返す非同期操作**」とする:

- 結果型は表示する ViewModel が宣言し (`DialogViewModel<R>` 相当)、show はその宣言から結果型を導出する。通知役 (DialogNotifier 相当) も同じ結果型に束縛する — object キャストを排除する
- completed / cancelled は型で区別する (enum / sealed) — `default` 値ハックを排除する
- 各形態への写像: Swift = `async` 関数 + enum、Kotlin / KMP commonMain = `suspend` 関数 + sealed class (共有層 Presenter が `val r = dialogs.show(...)` と待てる)、MAUI = `Task<T>` (原典踏襲、戻りは型付き結果に洗練)
- コールバック (completion handler) は公開 API の一級市民にしない。KMP→Swift の ObjC 互換面 (cross/ADR-0002) の最下層にのみ、Swift async からの機械変換として存在する

## Alternatives Considered

- **コールバックを公開 API とする案** — 却下。全形態で async / suspend が標準イディオムの現在、公開面をコールバックにする理由がない。ObjC 互換面の内部変換としてのみ残す
- **Flow / Observable ストリームとする案** — 却下。「1表示 = 1結果」のダイアログ意味論にストリームは過剰で、利用側に collect / 購読解除の負担を課す
- **結果型を show 呼び出し側の型パラメータで指定する案** — 却下。View factory のレジストリは ViewModel の型しか見ないため、同じ ViewModel を異なる結果型で show でき、報告型と受取型の不一致をコンパイル時に防げない

## Consequences

- 正: 各形態のイディオム (suspend / async / Task) に自然に写り、呼び出し側が1行で結果を待てる
- 正: 原典の型安全性の粗 (object キャスト・cancel = default) を契約レベルで解消する
- 負: 通知役を結果型に束縛するため、原典の「単一の DialogNotifier 型を全ダイアログで使い回す」より型設計が複雑になる
- 負: 結果型は ViewModel 型に1対1で結び付くため、同じ表示内容で異なる結果型を返し分けるには ViewModel 型を分ける必要がある
- 負: Swift async → ObjC completion handler → Kotlin suspend の変換層が KMP iOS 経路に必要になる

出典: kasane/roadmaps/library-foundation/phases/phase-1-architecture-research/history.md (2026-08-13: 結果通知方式) / artifacts/scout-origin-api-surface.md (DialogNotifier の実装詳細)

現行照合: 2026-08-15 確認。ios/Sources/KsDialogs/Presentation/KsDialogs.swift の `show<ViewModel: DialogViewModel>(_:) -> DialogResult<ViewModel.Result>` と ios/Sources/KsDialogs/Contract/DialogResult.swift の `case completed(Value) / cancelled` が、ViewModel 宣言由来の型付き結果と completed / cancelled の型区別を実装している。判定: 維持
