---
kind: rule
applies-when:
  always: false
  paths: ["skills/**", "kasane/concepts/**"]
  tasks: [docs-refresh の実行, 変更の蒸留]
title: docs-refresh を走らせる時点
description: 利用者向け Skill の追従更新を変更フローのどの時点で行うか。manifest の concepts スナップショットが取りこぼしを起こさない順序
timestamp: 2026-09-07
---

# docs-refresh を走らせる時点

この文書は、利用者向け Agent Skills (`skills/`) の追従更新を変更フローのどの時点で行うかを定める。読むと、`skills/.manifest.json` の concepts スナップショットが実態より古くも新しくもならない順序で docs-refresh を回せる。

## 規約: 蒸留の完了後に走らせる

**docs-refresh は変更の蒸留 (ksn-distill) が完了し、`kasane/concepts/` への追随が確定してから走らせる。** 実装フェーズの途中で走らせない。

docs-refresh は最後に `skills/.manifest.json` を書き、そこへ concepts の sha256 スナップショットを記録する。このスナップショットは「ここまでは Skill へ追従済み」という主張であり、次回実行はこれとの差分だけを要追従とみなす。実装フェーズ中に走らせると、その後の蒸留で concepts に入る改訂が**スナップショットに含まれないまま「追従済み」として記録される**。

## なぜ順序が効くか

蒸留は concepts を書き換えるフェーズである。したがって concepts の確定は蒸留の完了時点であり、それより前に取ったスナップショットは必ず古い。

| 走らせる時点 | manifest に載る内容 | 結果 |
|---|---|---|
| 実装フェーズ中 | 実装時点の concepts | 蒸留で入った改訂が記録から漏れる |
| 蒸留の完了後 | 確定した concepts | 次回の差分検出が実態と一致する |

漏れ方には2通りある。**ハッシュが実態より古い**場合は、次回実行が「変更されていない concept」を差分として検出する — 追従の必要がないファイルが要追従リストに載るノイズだが、検出漏れではないので安全側に倒れる。危険なのは逆で、実装中に書いたハッシュが**たまたま蒸留後の値と一致した**場合や、蒸留の改訂が別の concept にしか及ばなかった場合に、追従すべき改訂が差分として二度と現れない。

## 例外を認める場合

変更フローの中で `skills/` 自体を触る必要があるとき (公開 API の改名を Skill の記述へ波及させる等) は、実装フェーズ中に docs-refresh を走らせてよい。ただしその場合は**蒸留の完了後にもう一度走らせる**。2 回目は 1 回目で追従済みのファイルを再度更新することはなく、蒸留で入った concepts の改訂だけを拾う。

## 検知の手がかり

manifest のスナップショットが実態とずれているかは、`git log` で照合できる。ある concept のハッシュがどのコミット時点とも一致しないなら、その値は作業ツリーの途中状態を記録したものであり、実装フェーズ中に manifest が書かれた痕跡である。

## 関連

- [利用者向け Skill の API 掲載基準](user-skill-api-listing.md) — `skills/` に公開 API をどこまで載せるか
- [利用者向け Skill の記述スタイル](user-skill-writing-style.md) — references の節の型と en/ja 同期
- docs-refresh (`.agents/skills/docs-refresh/SKILL.md`) — 差分検出・承認・追従更新の手順と manifest の規範スキーマ
