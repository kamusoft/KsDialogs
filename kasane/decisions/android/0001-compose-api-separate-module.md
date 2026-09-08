---
id: 0001
title: Compose 系 API は別モジュール ksdialogs-compose に分離し、本体は Compose 非依存を保つ
status: accepted
date: 2026-08-17
amended-by: [cross/0019]
---

## Context

core/ADR-0011 で Android の Compose 系 API (`registerCompose` / `showCompose` と ComposeView ホスティング) を公開面に加えることが確定した。これらは compose-ui への依存を要求するが、`ksdialogs` 本体は View 系だけを使う消費者にも配られる — 特に MAUI Android はバインディング経由で `ksdialogs` を取り込むため、本体に Compose 依存を持たせると MAUI アプリにまで compose-ui が推移する。Compose 系 API をどの配布単位に置くかは、公開 artifact・import・依存の推移性・KMP androidMain からの利用方法を変える設計判断であり、実装前の確定が必要だった (オーナー判断 2026-08-17)。

## Decision

- Android の Compose 系 API は新モジュール **`ksdialogs-compose`** (Maven: `jp.kamusoft:ksdialogs-compose`、`jp.kamusoft:ksdialogs` に依存) に置く。公開形は既存 interface への拡張関数とする
- `ksdialogs` 本体は Compose に依存しない。Compose でコンテンツを書く消費者だけがこのモジュールを依存に追加する
- KMP androidMain は本体のみに依存し続ける (Compose 登録はアプリ層で行う)
- 本体の Compose 非依存は Gradle の依存グラフ検査タスク `:ksdialogs:verifyNoDeclarativeUiDependency` (`test` / `check` に結線。消費者へ配られる classpath の推移的混入まで走査) で機械的に固定する

## Alternatives Considered

- **本体 `ksdialogs` に同梱する** — 却下。全 Android 消費者 (MAUI 含む) に compose-ui が推移的依存として届く

## Consequences

- 正: View 系だけを使う消費者と MAUI バインディング経路に Compose の推移的依存が入らない
- 正: 依存グラフ検査が既定のテスト実行に乗るため、本体への Compose 混入は無断では通らない
- 負: Android の配布物が1つ増え、配布一覧 (cross/ADR-0008) と lockstep バージョン (cross/ADR-0009) の管理対象が増える
- 負: Compose 利用者にはモジュール2分割の説明責任が生じる (本体は推移的依存で自動解決されるため、手で書く依存は `ksdialogs-compose` の1点)

出典: kasane/changes/archive/2026-08-19-expand-api-surface/design.md (Decision 7、オーナー判断 2026-08-17)
