---
id: 0003
title: 既存資産知識の取り込みはハイブリッド型 — 原則系は即時翻案、機構系はオンデマンド参照
status: accepted
date: 2026-08-13
---

## Context

KsDialogs は先例リポジトリ (KsSettingsView / KsAppKMP) に蓄積された ADR・concepts を参照しながら立ち上げる。これらをいつ・どう KsDialogs 自身の知識層 (decisions/ concepts/) に取り込むかの方針が必要。同じ課題に対する先例として KsAppKMP core/ADR-0006 (原則系即時宣言 + 機構系オンデマンド再決定) がある。

## Decision

ハイブリッド型を採用する:

1. **原則系は即時翻案** — プロジェクトの意思決定を縛る方針 (リブランド原則・パリティ規約等) は、決定に使った時点で KsDialogs 自身の ADR / concepts へ翻案して取り込む。実例: リブランド3原則 (cross/ADR-0001 ← KsSettingsView cross/ADR-0017 の翻案)
2. **機構系はオンデマンド参照** — モノレポ構成・Swift interop 構成・ビルド設定などの「作り方の知識」は、使うフェーズが来るまで取り込まず、reference-repositories.md 経由で「リポジトリ名 + ADR 番号」形式の参照に留める。吸収時は出典 ADR / concepts を読み、KsDialogs の文脈で設計し直して新 ADR に出典リンクを残す
3. **一括移植は行わない**

## Alternatives Considered

- **A. 全部いま一括取り込み** — 却下。大半の機構知識は scaffold〜縦串実装の段階まで使われず、翻訳コストを先払いした上で移植した瞬間から乖離が始まる
- **B. 全部オンデマンド** — 却下。原則系が明文化されないまま立ち上げ期の設計議論が進み、決定の前提がぶれる

## Consequences

- 正: 原則系だけが早期に固まり、立ち上げ期の設計議論が安定する
- 正: 機構知識は使う時点の最新を参照でき、鮮度が保たれる
- 正: 取り込みコストを必要分だけに抑えられる
- 負: 機構系の吸収のたびに出典を参照して設計し直す一手間が発生する
- 負: 原則だけでは機械的に決まらない判断が都度発生する

出典: kasane/roadmaps/library-foundation/phases/phase-1-architecture-research/history.md (2026-08-13: 既存資産知識の取り込み方針) / KsAppKMP core/ADR-0006 (判断型の踏襲元)
