---
id: 0002
title: KMP は契約のみ commonMain に置き、レジストリ実体は各 Native lib へ全委譲する
status: accepted
date: 2026-08-14
amended-by: 0006
---

## Context

KMP 形態は Native 2実装を土台とする薄いファサード (core/ADR-0001)。公開 API は契約 interface + 既定 singleton の両対応 (core/ADR-0002)、DI 差し込みは VM 型キー → View factory の明示レジストリで singleton / DI はレジストリを共有する (core/ADR-0004)。expect/actual の境界をどこに引くか — 特にレジストリの実体を KMP 層と Native lib のどちらに置くか — が KMP 公開 API の構造を決める。

## Decision

- commonMain には純粋な契約のみを置く: `interface KsDialogs` + `sealed DialogResult` + レジストリ契約 (純 Kotlin、プラットフォーム依存なし)。テスト差し替え (FakeDialogs) は commonMain の interface で完結する
- expect/actual は既定 singleton エントリの取得と VM 契約 `DialogViewModel<R>` に絞る。`DialogViewModel<R>` は commonMain の expect interface とし、androidMain では Android Native lib の同名契約への actual typealias にする — Android Native のレジストリが自身の `DialogViewModel<R>` を型境界に要求するため、KMP 側で別型に包むとレジストリキーの同一性が壊れる
- 実体 (レジストリ含む) は各 Native lib へ全委譲する。レジストリ実体は OS ごとに Native lib 側の1個のみで、純 Native 利用者と KMP 利用者が同一レジストリを共有する
- androidMain actual は Android Native lib (Kotlin) へ直接委譲、iosMain actual は iOS Native lib (Swift) の `@objc` 互換面へ cinterop 経由で委譲する

## Alternatives Considered

- **KMP 層にレジストリ実体を持つ**: 却下。Native lib 側のレジストリと二重化し、登録漏れ・キー不一致の温床になる。core/ADR-0004 のレジストリ共有が崩れる
- **API 全体を expect class 化**: 却下。expect 面積が広く維持コストが高いうえ、expect class はモックが困難でテスト差し替えを損なう

## Consequences

- 正: KMP 層が最薄になり、core/ADR-0001 の「薄いラッパー」がそのまま成立する
- 正: レジストリが OS ごとに1個なので、純 Native と KMP の混在利用でも登録が1箇所で済む
- 正: KMP 層で検証すべき面が cinterop 委譲とキー同一性に限られ、挙動そのものの検証は Native 実装側に集約される
- 負: iosMain actual の cinterop 委譲は「commonMain VM の ObjC クラスが Swift lib のレジストリキーとして同一性を保つ」ことに依存する。この前提が崩れる構成では本 ADR の見直しが必要
- 負: KMP 単独では動作せず、常に Native lib の配布物とセットになる (core/ADR-0001 由来の制約を継承)
- 負: iOS の `@objc` 互換面はジェネリクスを表現できないため結果値を型消去で運ぶ。宣言結果型の検査は互換面が登録時に受け取る型情報に基づく自己申告であり、**誤った結果型を申告した登録そのものは検出できない**
- 負: 共有レジストリを Native lib 側に置く以上、commonMain の VM 契約は Native 契約と同一型でなければならず、KMP 層が独自の VM 契約を持つ余地はない

出典: kasane/roadmaps/library-foundation/phases/phase-4-vertical-slice/history.md (2026-08-14: KMP 公開 API の形 (b))

現行照合: 2026-08-15 確認。kmp/ksdialogs-kmp/src/androidMain/kotlin/jp/kamusoft/ksdialogs/kmp/DialogViewModel.android.kt の `actual typealias DialogViewModel<R> = jp.kamusoft.ksdialogs.DialogViewModel<R>` と iosMain/IosDialogGateway.kt の cinterop 委譲が、契約のみ commonMain・レジストリ実体は Native lib という構造を実装している。判定: 維持
