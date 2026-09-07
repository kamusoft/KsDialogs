---
id: 0018
title: notifier は show 時に VM へサイドテーブルで紐付け、VM 契約は参照型限定とする
status: accepted
date: 2026-08-24
---

## Context

登録 factory の基本形は `(vm, notifier) → View` の2引数形で、notifier は型消去 factory を呼ぶ瞬間に生成され View にだけ渡る。一方 MAUI には原典水準の1行登録 `.RegisterForDialog<TView, TViewModel>()` (View 型 + VM 型のみ・factory なし) を利用者向け主経路として提供する要件がある — C# は部分的型引数推論を持たず、素の factory 登録では原典比の書き味後退を回復できないため。factory を書かない登録では notifier を View に手渡す場所がないため、notifier を VM 側から引ける仕組みが前提になる (原典の BindingContext 方式が先例)。

VM 契約は4形態とも中身ゼロのマーカー (iOS のみ `associatedtype Result = Bool`) であり、合意済みの API スケッチは「VM 定義は中身ゼロの1行」という書き味を到達イメージとして描いている。この書き味を壊さずに notifier を届ける方法が本決定の主眼。

## Decision

**show 時にライブラリが notifier を VM に紐付け、View / VM は `vm.notifier` (拡張プロパティ相当) で参照する。** 紐付けの実体はライブラリ管理のサイドテーブル (弱参照・インスタンス同一性キー) とし、VM 契約にはメンバーを追加しない。

- VM 契約は**参照型 (class) 限定に狭める** — サイドテーブルがインスタンス同一性を要求するため (Swift は protocol の AnyObject 制約化。Kotlin / C# は実態としてクラスのみ)
- factory の `(vm) → View` 形を非破壊追加できる。既存の `(vm, notifier) → View` 形は低水準 API として残る
- 結果型の安全性は「公開面は型付き・内部はキャスト」— 既存の型消去 factory 設計 (DialogViewFactory) と同水準
- 同一 VM インスタンスの並行 show は構成ミスとして失敗させる (core/ADR-0004 の登録漏れと同じ扱い)。show 毎回生成モデル (core/ADR-0005) により正常系では発生しない
- KMP 共有層 (commonMain) での表現 (共有 VM から `vm.notifier` をどう見せるか) は本決定の範囲外とし、別途決める

## Alternatives Considered

- **A. VM 契約に settable な notifier プロパティを追加** — 却下。コンパイル時の型安全は最良だが、全 VM にプロパティ実装1行が必須になり「中身ゼロの1行 VM 定義」が崩れる。書き味を優先した
- **B. ライブラリ基底クラス (継ぐと notifier が生える)** — 主経路としては却下。単一継承を消費し、Android で VM が androidx ViewModel 等の基底を継ぐ構成と衝突する。ただしサイドテーブル方式と排他ではなく、任意の糖衣として後日追加できる

## Consequences

- 正: VM 定義の書き味 (中身ゼロの1行) を維持したまま notifier を VM 経由で引けるようになり、1行登録要件の前提が成立する
- 正: 利用者の VM が他の基底クラスを自由に継げる
- 正: 変更が notifier 生成箇所 (型消去 factory 内) の近傍に閉じ、レジストリ・show の公開シグネチャに波及しない
- 負: VM 契約が参照型限定になる (現状の利用実態では実害ゼロ — iOS の VM 準拠型はテスト・サンプル含め全て final class であることを 2026-08-24 に確認)
- 負: `vm.notifier` の内部は型消去キャストであり、コンパイル時保証は A 案より弱い
- 負: 弱参照サイドテーブルの実装は形態ごとの言語機構に依存する (C#: ConditionalWeakTable / Kotlin: 同一性キーの弱参照マップ / Swift: class 制約前提の弱参照表)。Kotlin では equals ベースの WeakHashMap をそのまま使えない (data class VM の等価衝突) 点に実装上の注意が要る

出典: kasane/roadmaps/library-foundation/phases/phase-6-model-binding-di/history.md (2026-08-24: notifier の VM 注入方式) / kasane/roadmaps/library-foundation/phases/phase-6-model-binding-di/artifacts/api-sketch-final-form.md (到達イメージ) / kasane/roadmaps/library-foundation/phases/phase-6-model-binding-di/agenda.md (1行登録の必須要件)
