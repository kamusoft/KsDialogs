---
id: 0013
title: 貢献は Issue Forms で受け、外部からの Pull Request は受け付けない
status: accepted
date: 2026-09-04
---

## Context

public 化にあたり、外部からの貢献をどう受けるかを決める必要があった。姉妹ライブラリ KsSettingsView は「貢献は Issue、外部 PR なし、Issue Forms 3 本 (英語)、CONTRIBUTING は英日」と決めており (`../KsSettingsView/kasane/decisions/cross/0024-contributions-via-issues-no-external-pull-requests.md`)、KsDialogs はこれを踏襲したうえで、4 形態 (iOS Native / Android Native / .NET MAUI / KMP) に固有の Platform 選択肢だけを決めた。

- 動機は AI 生成の粗雑な提案 (AI スロップ) の流入防止と、レビュー負荷・コード品質の維持。開発は Kasane の change フローで回っており、外部 PR はそのフローの外から実装だけが飛び込む形になる。
- GitHub の Pull requests アクセス設定は完全無効化と collaborators only の 2 段階が選べる。オーナー自身の PR と PR トリガーの検証 CI は必要。
- KsDialogs は MAUI と KMP が 2 つのホスト OS で動くため、不具合の切り分けには「形態」と「ホスト OS」の両方が要る。

前提: 貢献の受け口が GitHub Issues であり、Pull requests の設定を public 化フェーズで操作できること。

## Decision

- **外部からの Pull Request は受け付けない**。GitHub の Pull requests 設定を **collaborators only** にする (public 化フェーズで実施)。完全無効化はオーナー自身の PR と PR トリガー CI が成立しなくなるため採らない。
- **貢献は Issue で受ける**。Issue テンプレートは用途別 3 本 (バグ報告 / 提案 / 質問) を GitHub Issue Forms で置き、blank issue は無効にする。**Discussions は開かない**。
- **AI スロップの抑止は「実際に動かした証拠」の必須化** (バージョン・platform・再現手順・実際の挙動) で効かせる。書式の厳密さでは効かせない。
- **Platform の選択肢は「形態 × ホスト OS」の 7 択**: `iOS` / `Android` / `.NET MAUI on iOS` / `.NET MAUI on Android` / `Kotlin Multiplatform on iOS` / `Kotlin Multiplatform on Android` / `Multiple platforms`。バグ報告と質問で同じ 7 択を使う。
- **言語**: Issue Forms は英語 1 セット、`CONTRIBUTING.md` / `CONTRIBUTING_ja.md` は英日 2 枚、投稿本文は英語・日本語どちらでもよい。方針の表明先はルート README の「貢献」節 (3〜4 行) + `.github/CONTRIBUTING.md`。

## Alternatives Considered

- **Platform を 5 択 (形態 4 + Multiple) にする**: 選択肢が短い。しかし MAUI / KMP の不具合はホスト OS ごとに切り分けが要り、質問の往復が増えるため却下。
- **形態とホスト OS を 2 つの dropdown に分ける**: 組み合わせを網羅できる。しかし Native iOS / Android では 2 つ目が冗長になり、無意味な組み合わせ (Native iOS × Android) を許してしまうため却下。
- 翻案元で却下済みの案 (Pull requests の完全無効化 / README・CONTRIBUTING での表明のみ / Kasane の生フォーマットを外部に埋めさせる / Issue Forms を英日 2 セットにする) は、KsDialogs でも同じ理由で採らない (却下理由は翻案元 ADR を参照)。

## Consequences

- 正: 外部からの実装が change フローを迂回して入ることがなくなり、レビュー負荷とコード品質を制御できる。
- 正: 形態 × ホスト OS の選択肢で、MAUI / KMP の不具合報告が最初からホスト OS を伴う。
- 負: コードで直接貢献したい人の道が塞がれ、OSS としての参加障壁は上がる。
- 負: Issue の巡回とオーナーによる起票 (change への転記) が滞ると貢献が放置される。巡回の運用が前提になる。
- 負: Pull requests の設定変更はリポジトリ設定の操作であり、public 化フェーズの実施手順に持ち越す。設定が済むまで `.github/` 一式だけが先に存在する。

## Revisit When

- 外部からのコード貢献を受ける判断をしたとき (change フローに外部実装を取り込む手順が要る)。
- 質問の Issue が増え、Issues 1 面では捌けなくなったとき (Discussions を後から開くことはできる。逆は既存スレッドが読めなくなる)。

---
出典: kasane/roadmaps/package-distribution/phases/phase-2-docs-rollout/agenda.md (踏襲決定「貢献は Issue のみ」・決定事項「Issue Forms の Platform は「形態 × ホスト OS」の 7 択」) / kasane/roadmaps/package-distribution/phases/phase-2-docs-rollout/history.md (2026-09-04) / kasane/changes/archive/2026-09-05-rollout-user-docs/design.md (Decision 6) / ../KsSettingsView/kasane/decisions/cross/0024-contributions-via-issues-no-external-pull-requests.md (翻案元。踏襲部分の Decision・Alternatives・Consequences の原文)
