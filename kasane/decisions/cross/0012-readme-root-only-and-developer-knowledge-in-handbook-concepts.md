---
id: 0012
title: README はルート 2 枚 (英語 + `README_ja`) に集約し、`samples/` 配下 README は廃止して開発者向け知識は handbook / concepts に一本化する
status: accepted
date: 2026-09-04
---

## Context

public 化の前に利用者向けドキュメントを揃えるにあたり (package-distribution ロードマップの実行順の制約: 公開履歴に旧文書を載せない)、README 群の役割と枚数を決める必要があった。

- 現行のルート README は開発者向け (ビルドルート表とビルドコマンド) の日本語混じり英語 1 枚で、インストール座標・最小コード例・Skills への導線・貢献方針を持たない。
- `samples/README.md` と `samples/{ios,android,maui,kmp}/README.md` の 5 本が「撮影用デモ ID・アプリ識別子」「各ルートの参照方式とビルド手順」「KMP iOS アプリのリンク構成」の正を握っており、cross/ADR-0010 と `kasane/config.yaml` の `ui.screenshot` がそれを正として参照していた。「知識の正は `kasane/concepts/` (規範は handbook) とコード・テスト」の原則 (cross/ADR-0003・0011) と逆転している。
- 利用者向けドキュメントは Agent Skills (`skills/{en,ja}/`) として提供し docs-refresh で追従させる形が確定済み (cross/ADR-0011)。README は利用者の入口であって手順書ではない。
- 姉妹ライブラリ KsSettingsView は同じ状況で「README はルート 2 枚、開発者向け知識は concepts へ」と決めている (`../KsSettingsView/kasane/decisions/cross/0023-readme-root-only-and-developer-knowledge-in-concepts.md`)。KsDialogs には対応する決定が無く、翻案元の規範を根拠なしに書けない状態だった。

前提: `skills/` が利用者向けの入口として存在し、docs-refresh がルート README 2 枚と `skills/` 索引 2 枚を追従対象に持つこと。

## Decision

- リポジトリの README は**ルートの 2 枚のみ**とする: 英語 `README.md` + 日本語 `README_ja.md`。`skills/README.md` / `skills/README_ja.md` (Skill 索引、cross/ADR-0011) は対象外で存置する。`android/layout-case-fixtures/README.md` (テスト fixture の説明) も対象外とする。
- `samples/README.md`・`samples/{ios,android,maui,kmp}/README.md` の 5 本は廃止し、内容は「規範は handbook・記述は concepts・既出は捨てる」で移す: 撮影のための起動引数 (キー・安定デモ ID・アプリ識別子・外部表現) は `kasane/handbook/cross/sample-parity.md`、各ルートの参照方式とビルド・実行手順は `kasane/handbook/cross/local-development-setup.md`、KMP iOS アプリの 3 点リンクと合成 Swift package の再生成手順は `kasane/concepts/kmp/api/ios-host-integration.md`。パリティ写像・器の責務と演出の添付・Sample 内部の演出分担メモは既出のため捨てる。
- 廃止した README を正として指していた参照 (cross/ADR-0010 本文、`kasane/config.yaml` `ui.screenshot`、sample-parity.md「関連」節) は handbook の節へ付け替える。
- **ルート README は利用者の入口に純化する**: 配信準備中の状態表記 1 行・概要と主な特徴・スクリーンショット・対応プラットフォーム・インストール座標・最小コード例・`skills/` への導線・リポジトリ構成・貢献・ライセンス。ビルドルート表・ビルドコマンドなど開発者向けの手順は載せない。インストールは座標だけを置き手順は Skills へ、最小コード例は対応する platform Skill の最小コードと逐語一致させる。
- ルート README 2 枚は**翻訳ロックステップ**で扱う (片方だけを更新しない。docs-refresh は 2 枚を 1 回で更新する)。
- 以後 platform / Sample 別の README を新設するには本 ADR の改訂を要する。

## Alternatives Considered

- **現状維持 (README を残し、利用者の入口にしないだけ)**: 移送作業が不要。しかし知識の正が README に滞留する逆転が残り、ADR と config が README を正として指し続ける。「ADR は記録であって規約ではなく、正は handbook へ寄せる」というオーナー判断で却下。
- **handbook に `sample-build.md` を新設して Sample の知識を集約する**: 移送先が 1 本で分かりやすい。しかし正の本数が増える。既存の `local-development-setup.md` と `sample-parity.md` に節を足せば足りるため却下。
- **撮影引数を config `ui.screenshot` へ吸収する**: 参照の付け替えが最小で済む。しかし「4 ルートが同じキーと ID を受け付ける」という規範が config に埋もれるため却下。

## Consequences

- 正: 開発者向け知識の正が handbook / concepts に一本化され、ADR・config → README の逆参照が解消する。
- 正: docs-refresh の追従対象がルート 2 枚 + 索引 2 枚の計 4 枚に閉じ、翻訳ロックステップのコストが上限 4 枚に収まる。
- 負: public 化後の contributor が Sample のビルド手順に辿り着く入口が `AGENTS.md` → handbook の 2 段になる (ルート README には載らない)。
- 負 (実装で判明): 廃止した README への参照を「リポジトリ全体で 0 件」にする検査は達成できない。append-only の履歴 (`kasane/concepts/log.md`・`kasane/lessons/inbox/`・進行中 change の足場・accepted ADR の本文と過去の現行照合) が移送元のパスを履歴として保持するため。合格条件は「active な decisions・handbook・concepts・実装・公開文書で残存参照 0 件」に倒した (deviation 記録)。
- 負 (実装で判明): 廃止した README を名指ししていた accepted ADR (cross/ADR-0006・0007) は本文不変のため書き換えられず、現行照合 footer で現在の所在を示すにとどまる。文書を廃止する決定は、その文書を名指しする accepted ADR の有無を先に確認する必要がある。

## Revisit When

- platform / Sample 別 README を必要とする読者 (エージェント・contributor) が実在すると分かったとき。
- docs-refresh の追従対象 (`readmes`) を変更フローで増減させるとき。

---
出典: kasane/roadmaps/package-distribution/phases/phase-2-docs-rollout/agenda.md (踏襲決定「README はルート英日 2 枚のみ」・決定事項「`samples/` 配下 README 5 本は廃止し、正を handbook / concepts へ移送する」) / kasane/roadmaps/package-distribution/phases/phase-2-docs-rollout/history.md (2026-09-04) / kasane/changes/archive/2026-09-05-rollout-user-docs/design.md (Decision 2・Decision 5) / kasane/changes/archive/2026-09-05-rollout-user-docs/deviation.md (Task 7.4) / ../KsSettingsView/kasane/decisions/cross/0023-readme-root-only-and-developer-knowledge-in-concepts.md (翻案元。踏襲部分の Decision・Consequences の原文)
