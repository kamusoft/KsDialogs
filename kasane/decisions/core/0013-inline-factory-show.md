---
id: 0013
title: 登録不要の単発表示はインライン factory 型の show で提供する
status: accepted
date: 2026-08-17
---

## Context

原典 AiForms.Maui.Dialogs には登録不要の表示系統 (`ShowAsync<TView>(vm)` = View 型指定で框架が生成 / `ShowAsync(view, vm)` = View インスタンス直接渡し) があり、1回しか使わないダイアログに登録の儀式を課さない。KsDialogs は登録経路 (VM 型キー → View factory、core/ADR-0004) しか持たず、この系統への回答が必要だった (add-vertical-slice design Decision 1 で非破壊追加は担保済み)。原典のインスタンス渡しは DialogView が notifier を自前生成し BindingContext で VM に配る MAUI 固有機構の上に成立していたが、KsDialogs は factory が (vm, notifier) を受け取る注入型であり、出来上がった View を後から渡す形では notifier を型安全に届ける場所がない。

## Decision

- 登録不要の単発表示は**インライン factory 型の show** で提供する: `show(vm) { vm, notifier in View }` — 登録経路と同じ factory 形 (型付き notifier・技術別オーバーロード core/ADR-0011・既定結果型 Bool core/ADR-0012) をその場で1回だけ使う
- インライン show は**レジストリを経由しない一時 factory 実行**とする: 渡された factory をその場で内部表現に変換して提示し、レジストリへの登録・削除を一切行わない。よって (a) 同じ VM 型の既存登録はインライン show の前後で不変 (b) 同じ VM 型の並行インライン show はそれぞれの factory / notifier / 結果が独立する — この2点を観察可能な契約とする
- 対象は Native 2実装 + MAUI。KMP 共有コード (commonMain) は View を供給できないため対象外
- 原典の View 型指定 (`ShowAsync<TView>`、框架が View を生成) は DI での View 解決と地続きのため、後続の1行登録・DI 糖衣の設計とセットで扱う
- VM 注入 (notifier を VM スロットへ注入する後続設計) が実現すれば、登録 factory と同形のためインライン形も `(vm) => View` へ自動的に縮む

## Alternatives Considered

- **原典同型の View インスタンス直接渡し (`show(view, vm)`)** — 却下。notifier の型安全な受け渡しが壊れる (自前生成 notifier は show の結果配線と繋がらない)。「View を値として運ぶ」形は core/ADR-0011 でも不採用にした方向
- **追加しない (登録経路のみ)** — 却下。1回きりのダイアログに登録の儀式が残り、原典の利便性を失う
- **一時登録 + show + 登録解除で実現する案** — 却下。キー衝突と削除タイミングの問題を持ち込み、同一 VM 型の並行 show や既存登録との衝突が発生する

## Consequences

- 正: 登録経路とインライン経路が同じ factory 形を共有し、学習コスト・実装の重複が増えない
- 正: 型付き notifier・両 View 技術・bool 既定の恩恵をインラインでもそのまま受ける
- 負: show のオーバーロードが増える (技術別 × インラインのマトリクス)。公開 API の説明責任が増える

出典: kasane/roadmaps/library-foundation/phases/phase-5-2-api-surface/history.md (2026-08-17: View 直接渡しの show 系統)
