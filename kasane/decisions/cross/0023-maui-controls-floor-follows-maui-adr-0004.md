---
id: 0023
title: toolchain 固定境界のうち MAUI 本体の版は maui/ADR-0004 の「workload set 同梱版」に従う (0018 を一部改訂)
status: accepted
date: 2026-09-09
amends: [cross/0018]
---

## Context

cross/ADR-0018 は検証に用いる toolchain の固定境界を表にし、MAUI 本体 (`Microsoft.Maui.Controls`) の行を「10.0.70」と書いた。この値は翻案元 KsSettingsView の実測値であり、同じ日に accepted になった maui/ADR-0004 は MAUI 本体の下限版を「リポジトリが固定する workload set が同梱する版 (現在 10.0.20)」と決め、10.0.70 を却下案として退けている。現行のコード (`maui/Directory.Packages.props`) は maui/ADR-0004 に従っている。

消費者検証の提案 (add-consumer-verification) は、当初この行を写して「ツールチェーンの再現性」の Requirement に 10.0.70 を書き、相方スペックレビューが現行コードと maui/ADR-0004 との衝突を Major として検出した。長命層の 2 つの accepted ADR が同じ対象について食い違う状態は、cross/ADR-0018 だけを開いた読み手 (`maui/Directory.Packages.props` のコメントは cross/ADR-0018 を指す) に古い値を現行として読ませる。

前提: cross/ADR-0018 の他の行 (ランナー・Xcode・JDK・.NET SDK と workload set・Sample・Emulator・外部 action) と、固定境界の置き場・`global.json` をルートに置く決定は、現行の workflow と `global.json` と一致している。

## Decision

cross/ADR-0018 の決定のうち固定境界の表の「MAUI 本体 (`Microsoft.Maui.Controls`)」の行を、**maui/ADR-0004 の決定「下限版は workload set 同梱の版とし、ビルド・テストする版もその版に揃える。`global.json` の workload set を上げるときは `maui/Directory.Packages.props` の版を同梱版に合わせる」で置き換える**。粒度 (central package management で一元宣言、restore 元は nuget.org 単一 + source mapping) と置き場 (`maui/Directory.Packages.props` / `maui/nuget.config`) は cross/ADR-0018 のまま維持し、値の決め方だけを maui/ADR-0004 に委ねる。他の決定は維持する。

## Alternatives Considered

- **cross/ADR-0018 を据え置き、footer の `関連:` 行で maui/ADR-0004 を指すだけにする** — 却下。本文の表に残る 10.0.70 は maui/ADR-0004 が却下した値であり、footer を読まない読み手 (コードコメントから直接開く読み手) が現行の決定として読む。一部の置き換えは amends の型で本文の読み手にも辿らせる
- 出典 (design の ADR 候補・相方スペックレビュー #1) に、上記以外の代替案の記載はない

## Consequences

- 正: MAUI 本体の版について cross/ADR-0018 と maui/ADR-0004 の読み手が同じ決定に辿り着く
- 正: 値の変更 (workload set の更新) が maui/ADR-0004 の手順 1 か所で説明される
- 負: cross/ADR-0018 の読み手は MAUI 本体の行だけ別ドメインの ADR を開く必要がある

## Revisit When

- maui/ADR-0004 が supersede されたとき (MAUI 本体の版の決め方が変わる)
- 前提 (Context) が崩れたとき

---
出典: kasane/changes/archive/2026-09-10-add-consumer-verification/design.md (ADR 候補: 既存 ADR 間の衝突) / 同 second-opinion-spec-001.md (#1 と突き合わせ結果) / 同 proposal.md (Impact) / kasane/decisions/maui/0004-nuget-three-package-structure.md (MAUI 本体の下限版は workload set 同梱の版)
関連: cross/ADR-0018 (toolchain の固定境界。本決定は MAUI 本体の行だけを置き換える) / maui/ADR-0004 (MAUI 本体の下限版の決め方)
