---
id: 0025
title: 既定ブランチは main とし、リポジトリの入口が最新リリースの README を指すようにする
status: accepted
date: 2026-09-10
amends: [cross/0016]
---

## Context

cross/ADR-0016 はブランチを `develop` / `main` の 2 本にし、既定ブランチを `develop` と定めた。理由は開発者 1 人が `develop` へ直接 push する運用に合わせることだった。初回リリース `0.1.0-beta.1` を経て `main` が実在し、利用者がリポジトリを開くようになった。

既定ブランチはリポジトリのトップページと `git clone` 直後の checkout を決める。`develop` が既定だと、利用者が最初に見る README は公開版より進んだ開発中の内容になり、まだ公開していない API やインストール例を読むことになる。姉妹ライブラリ KsSettingsView は既定ブランチを `main` にし、README の絶対 URL も `main` を指している。

前提: `main` は最新リリース (またはリリース進行中のリリース候補) の先端であり、`develop` からの pull request だけが入る (cross/ADR-0016 の他の決定は維持)。

## Decision

cross/ADR-0016 の決定のうち「既定ブランチは `develop`」の 1 文を本決定で置き換え、**既定ブランチを `main`** にする。他の決定 (ブランチ 2 本・`develop` へ直 push・`main` は PR のみ・release は `main` からのみ起動) は維持する。

README 2 枚の絶対 URL (画像・リポジトリ構成表・AGENTS.md へのリンク) も `main` を指すように揃える。`main` は最新リリース時点の内容を持つため、利用者向けの入口とそこから辿る先が同じ時点になる。

## Alternatives Considered

| 案 | 却下理由 |
|---|---|
| 既定ブランチを `develop` のまま置く (ADR-0016 の形) | 利用者がリポジトリを開いたときに公開版より先の README を読む。KsSettingsView とも揃わない |
| 既定ブランチだけ `main` にし README の URL は `develop` のまま | `main` の README から `develop` の画像・リンク先を参照する形になり、入口と辿る先の時点がずれる |

## Consequences

- 正: リポジトリのトップと clone 直後が最新リリースの README になり、利用者が未公開の内容を読まない
- 正: `main` 宛ての pull request を作るときに base を明示しなくてよい
- 負: 開発者は clone 直後に `develop` へ切り替える手間が要る
- 負: README の URL は次のリリース PR で `main` に入るまで、`main` 側では旧 URL (`develop`) のまま残る

## Revisit When

- 前提 (Context) が崩れたとき。特にブランチモデル (cross/ADR-0016) を見直すとき

---
出典: オーナー判断 2026-09-10 (会話。「KsSettingsView は main になっているので揃える。利用者が develop を最初に見てしまうのはまずい」) / ../KsSettingsView/kasane/handbook/cross/release-procedure.md (default branch の切り替え手順)
現行照合: 2026-09-10 確認。`gh api repos/kamusoft/KsDialogs` の `default_branch` が `main`。README 2 枚の絶対 URL を `main` に置換。判定: 維持
関連: cross/ADR-0016 (ブランチモデル。既定ブランチの 1 文を本 ADR が一部改訂) / cross/ADR-0024 (release がインストール例を `develop` へ commit する決定。既定ブランチによらず成り立つ)
