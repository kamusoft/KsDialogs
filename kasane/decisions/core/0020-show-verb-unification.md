---
id: 0020
title: 表示 API の動詞は show 1本とし、呼び出し経路は引数の形で表現する
status: accepted
date: 2026-08-24
---

## Context

移植元の表示 API は後付けの積み重ねで命名の統一性が崩れている: `ShowAsync` / `ShowResultAsync` / `ShowFromModelAsync` / `ShowResultFromModelAsync`。分裂の次元は2つあり、「結果型の有無」(Show / ShowResult) と「呼び出し経路の接尾辞」(FromModel) である。

KsDialogs では前者は既に消滅している — 結果型は VM が宣言し戻り値は常に `DialogResult<T>` (core/ADR-0012) のため、動詞を分ける理由がない。また現状実装は、インスタンス渡し show とインライン factory show (core/ADR-0013) を既に `show` (C# は `ShowAsync`) のオーバーロードに集約している。残っていたのは、型指定呼び出し (ライブラリが VM を解決し configure クロージャで初期化する経路 — core/ADR-0019) の命名である。

命名ポリシー (core/ADR-0002) は「原典命名が非対称な箇所は対称性を優先して改める」と定めており、本決定はその表示 API への具体適用にあたる。

## Decision

**表示 API の動詞は show 1本 (C# は言語慣習の Async 接尾辞を付けた ShowAsync) に統一し、呼び出し経路の違いは引数の形だけで表現する。経路を表す接尾辞・別動詞は導入しない。**

- インスタンス渡し: `show(vm)` / 型指定 + configure: `show(VM 型) { vm -> ... }` / インライン factory: `show(vm) { vm, notifier -> View }` — いずれも同じ動詞のオーバーロード
- 結果型による動詞分裂 (ShowResult 相当) は導入しない (core/ADR-0012 により不要)
- 将来 show 系の経路を追加する場合も、原則として同じ動詞のオーバーロードとして設計する
- C# のオーバーロード解決の成立性 (引数なしの型指定 `ShowAsync<TVm>()` とインスタンス版の分離など) は仕様化・実装時に検証し、成立しない形が見つかった場合は本決定を supersede して個別名を導入する

## Alternatives Considered

- **経路ごとに動詞を分ける (showFromModel 等の原典踏襲)** — 却下。原典の命名崩れをそのまま持ち込むことになり、既に show に集約済みの2経路 (インスタンス渡し・インライン factory) と非対称になる。「名前が経路を説明する」利点はあるが、FromModel という概念名の学習を利用者に強いる

## Consequences

- 正: 「表示する」は常に show という単純な心的モデルになり、コード補完で全経路が1つの動詞に集まる
- 正: 命名ポリシー (core/ADR-0002) の対称性優先が表示 API に一貫して適用され、以後の追加経路の命名判断が機械的になる
- 負: 原典 `ShowFromModelAsync` 系の利用者には対応先が自明でないため、移行対応表のドキュメント整備が必要
- 負: 1つの動詞にオーバーロードが集中するため、シグネチャ一覧の整理されたリファレンスがないと経路の全体像が掴みにくい
- 負: オーバーロド解決は言語ごとの細部に依存し、特に C# で成立しない形が後から見つかると supersede が必要になる

出典: kasane/roadmaps/library-foundation/phases/phase-6-model-binding-di/history.md (2026-08-24: show 系 API の命名統一) / AiForms.Maui.Dialogs リポジトリ: Dialog/Dialog.cs (原典 show 系の命名一覧)
