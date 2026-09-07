---
id: 0004
title: DI 差し込みは VM 型キー → View factory の明示レジストリとする
status: accepted
date: 2026-08-13
---

## Context

移植元の DI 差し込みは `SetIocConfig(Func<Type,Type> viewTypeGetter, Func<Type,object> viewResolver = null)` — DI コンテナ非依存で「ViewModel 型 → View 型」「型 → インスタンス」の関数2本を static に差し込む設計 (null チェック漏れの粗あり)。この設計は C# のリフレクション文化 (`Type` を投げ回す) が前提で、リッチなリフレクションを持たない Swift と、プラットフォームの View 型を知らない KMP commonMain には持ち込めない。

KMP 形態の核心は「共有層の ViewModel と Native View の紐付け」であり、これをどの契約で表現するかが本決定の主眼。また core/ADR-0002 (公開 API 形状) が「既定 singleton と DI 登録インスタンスの同一性担保」を本決定に委ねている。

## Decision

core 契約を「**VM の型キー → View を作る factory の明示レジストリ**」とする:

- 登録: `register(<VM 型キー>) { vm -> View }`。キーは Kotlin / KMP = KClass、Swift = メタタイプ、MAUI = ジェネリック型引数 — いずれもリフレクション不要
- KMP では VM が commonMain 型のため KClass がそのままキーになり、factory の登録は各 platform 側 (アプリ起動時 / actual) で行う。共有 Presenter は View を知らず「VM を投げたら結果が返る」だけになる
- 既定 singleton エントリと DI 登録インスタンスは**同じレジストリを共有**し、どちらの入口から呼んでも同じ紐付けが引ける (core/ADR-0002 の残課題の解消)
- MAUI には原典互換の糖衣として `SetIocConfig` 相当 (関数ペアによる一括委譲) を残し、コンテナ連携 (Prism 等) は糖衣に載せる。原典の null 上書きの粗は修正する
- View 型を直接指定する呼び方 (原典 `ShowAsync<TView>` 相当) はレジストリ登録なしで使える

書き味の具体例は API スケッチ (kasane/roadmaps/library-foundation/phases/phase-1-architecture-research/artifacts/api-sketch-registry.md) を参照。

## Alternatives Considered

- **原典踏襲 (関数2本の static 差し込み)** — 却下。Type→Type 変換関数は Swift で非イディオム (リフレクション不足)、KMP commonMain では表現不能
- **DI コンテナ別 adapter の提供** — 対立案ではなく将来のオプション。レジストリの上に Koin 用拡張等を後日足せるため、core 契約には含めない (コンテナごとの保守コストを負わない)

## Consequences

- 正: 3形態すべてでリフレクション不要の同型な登録 API になり、契約が仕様として共有しやすい
- 正: KMP の「共有 VM と Native View の紐付け」が factory 登録として自然に表現される
- 正: singleton / DI 両入口の紐付け一貫性がレジストリ共有で構造的に保証される
- 負: 原典の SetIocConfig 直接互換ではないため、MAUI 糖衣の実装と文書化が必要
- 負: KMP iOS 経路のキー同一性は「commonMain の VM クラスが ObjC クラスとして Swift から見える」ことを前提とし、この前提が崩れるとレジストリ共有そのものが成立しない
- 負: KMP iOS のキー同一性は VM クラスの ObjC export 名に依存するため、export 名が自動生成される文脈 (テスト実行形式など) では成立せず、実 framework 越しでしか確認できない
- 負: 明示レジストリである以上、VM 型の登録漏れは実行時にしか現れない — 構成ミスとして失敗させる必要があり、結果 (cancelled 等) に化けさせてはならない

出典: kasane/roadmaps/library-foundation/phases/phase-1-architecture-research/history.md (2026-08-13: DI 差し込み方式) / artifacts/scout-origin-api-surface.md (SetIocConfig の実装詳細) / artifacts/api-sketch-registry.md (合意した書き味)

現行照合: 2026-08-15 確認。ios/Sources/KsDialogs/Registry/DialogViewRegistry.swift (キーは `DialogViewModelKey` = メタタイプ) と android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/DialogViewRegistry.kt (キーは `KClass`) が、リフレクション不要の VM 型キー → View factory レジストリを実装している。判定: 維持
