---
id: 0005
title: 一括解決糖衣は static 関数ペアではなくレジストリの fallback resolver とし、解決順序を仕様で規定する
status: accepted
date: 2026-08-24
---

## Context

core/ADR-0004 は「MAUI には原典互換の糖衣として SetIocConfig 相当 (関数ペアによる一括委譲) を残し、コンテナ連携は糖衣に載せる。原典の null 上書きの粗は修正する」と定めたが、その具体形は未決だった。原典の SetIocConfig は「VM 型 → View 型」「型 → インスタンス」の関数2本を static に一括差し込みする API で、後勝ち・null 上書きの粗があり、個別登録との優先順位も不明瞭。また viewResolver が View と VM の解決を暗黙に兼務していた。

1行登録 (VM factory の自動配線を含む — core/ADR-0021) が利用者向け主経路として成立したことで、この糖衣が個別登録の代替を担う役目は消え、価値の本体は「per-type 登録なしの規約ベース一括解決」(VM 名 → View 名の命名規約でまとめて解決する Prism 風の使い方) に絞られた。

## Decision

**一括解決糖衣は、レジストリの fallback resolver として提供する。static 関数ペアの差し込み口は設けない。**

- 解決順序を仕様として規定する: **明示レジストリ (Register / RegisterForDialog) → fallback resolver → 構成ミスとして失敗** (例外。cancelled に化けさせない — core/ADR-0004 の原則)。明示登録が常に勝つ
- View の fallback と VM の fallback を明示的に分離する。View fallback は「未登録 VM 型 → View インスタンス (null = 解決不能)」の関数、VM fallback は型指定呼び出し用で「IServiceProvider から引く」既定実装を用意する
- 設定は DI チェーン上の一箇所 (`AddKsDialogs(options => ...)` 相当) で行う。static 差し込み口が存在しないため、原典の null 上書き・後勝ちの粗は構造ごと消える
- 本糖衣は MAUI 限定 (リフレクション文化圏)。Native / KMP には持ち込まない (core/ADR-0004 のまま)
- 名前・シグネチャは仕様化で確定する (スケッチ: kasane/roadmaps/library-foundation/phases/phase-6-model-binding-di/artifacts/api-sketch-maui-fallback-resolver.md)

## Alternatives Considered

- **原典形の踏襲 (関数ペアの static 一括設定 + null 上書きだけ修正)** — 却下。レジストリの外に第2の解決経路が立ち、明示登録との優先順位の規定を別途負う。static 設定は既定 singleton と DI 登録インスタンスのレジストリ共有 (core/ADR-0002) にも乗りにくい。原典利用者の移行はほぼ無傷だが、粗の温床 (static・後勝ち) が残る

## Consequences

- 正: 解決順序 (明示 → fallback → 失敗) がレジストリの中で閉じ、原典で不明瞭だった優先順位が仕様になる
- 正: null 上書き・後勝ちの粗が構造ごと消える
- 正: View / VM の fallback 分離により、原典 viewResolver の暗黙の兼務が明示化される
- 負: 原典 SetIocConfig からは載せ場所と形が変わるため、移行対応表の文書化が必要 (概念は同じ「関数を渡す」)
- 負: fallback が返す View / VM は明示レジストリの型検査を通らないため、規約ミス (名前不一致・型不整合) の検出は実行時になる — 構成ミス失敗のエラーメッセージ品質が重要になる

出典: kasane/roadmaps/library-foundation/phases/phase-6-model-binding-di/history.md (2026-08-24: MAUI 一括解決糖衣の形) / artifacts/api-sketch-maui-fallback-resolver.md (完成系イメージ) / AiForms.Maui.Dialogs リポジトリ: Configurations・Dialog/Dialog.cs (SetIocConfig の原典実装)
