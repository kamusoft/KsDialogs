---
id: 0024
title: release は dispatch 起動・取り消せる順で直列に publish し、SPM tag は KMP の Maven 発行より前に置く
status: proposed
date: 2026-09-10
---

## Context

4 形態 (SwiftPM 配信リポジトリ / Maven `ksdialogs-core`・`ksdialogs`・`ksdialogs-kmp` / MAUI NuGet 3 パッケージ) は lockstep 単一バージョンで一斉リリースする (cross/ADR-0009)。release workflow は姉妹ライブラリ KsSettingsView の `release.yml` (同 cross/ADR-0020: dispatch 起動・tag は最後・version 注入) をコピーして固有値を差し替える形で逆流させる。

KsSettingsView には KMP 形態が無い。KsDialogs の KMP は発行物の SwiftPM 連携メタデータに Swift 参照の URL と URL 末尾から導出される `packageName` を焼き込み、cinterop klib を伴う iOS publication の発行は配信リポジトリ `KsDialogs-SPM` に同版の tag が実在しないと失敗する (KGP が発行時に SwiftPM パッケージを解決する)。発行には Xcode が要る。したがって Maven Central へ上げる KMP artifact は既定 URL (https) + exact(version) で、SPM tag の後にしか作れない。翻案元の順序「Maven upload 保留 → NuGet push → Maven release → SPM tag → monorepo tag」はそのままでは成立しない。

消費者検証 (concepts cross/architecture/consumer-verification.md) の KMP 消費者は job 内で kmp/ を `file://` のスナップショット clone で発行し直すため、package 段の KMP 成果物に消費者は居ない。

前提: 配信リポジトリの tag は削除できる。Maven Central の deployment は release するまで drop できる。NuGet push と Maven release は取り消せない。

## Decision

**起動と version**: 起動は `workflow_dispatch` で、version 入力は `^[0-9]+\.[0-9]+\.[0-9]+(-(alpha|beta|rc)\.[0-9]+)?$` のみ通し、`dry-run` 入力で publish 以降を行わない。本番はリリース対象ブランチ `main` からのみ起動し、secrets は Environment `release` に置く。tag push はトリガーにしない。version の SSoT は dispatch 入力 (= tag) で、CI が `-Pversion=` / `-p:Version=` で注入し、リポジトリ内の値は開発用既定値のまま (bump コミットを積まない)。tag は接頭辞なし `X.Y.Z`。

**段構成**: validate → (test ∥ package) → dry-run (消費者検証に artifact を渡す) → publish → 反映待ち → smoke。package 段に KMP の job は置かない (コンパイルは本体検証 `kmp / verify` が担保)。

**publish の順序**: macOS runner の 1 job で、取り消せる順に直列で行う。再実行は同じ version で「失敗した job から」行い、各ステップは存在検査で冪等化する。失敗時は保留 deployment を drop する。

| 順 | ステップ |
|---|---|
| 1 | SPM スナップショット commit push (差分なしなら skip) |
| 2 | Android の Maven upload 保留 → validated 待ち |
| 3 | SPM tag push (同 commit なら skip) |
| 4 | KMP を https + exact で発行 → upload 保留 → validated 待ち |
| 5 | NuGet push (Trusted Publishing / OIDC) |
| 6 | Maven release 2 件 (Android → KMP) → published 待ち |
| 7 | monorepo tag + GitHub Release |
| 8 | README 2 枚と利用者向け Skill のインストール例の version を置き換えた commit を `develop` へ push (競合しても release は失敗にしない) |

**tag と README の扱い**: version を放棄するときだけ、配信リポジトリに残った tag を手で消す (手順は handbook の release-procedure)。インストール例の version は release workflow が書く — MAUI の pack の前に作業木で置き換えて nupkg に同梱し、publish 成功後に順 8 で `develop` へ commit する。人がリリース PR で version を書く手順は持たず、`main` は次のリリース PR で追従する。

## Alternatives Considered

| 案 | 却下理由 |
|---|---|
| SPM tag を Maven release の後に置く (翻案元の順序) | KMP の iOS publication は同版 tag が無いと発行できず、順序が成立しない |
| package 段でも `file://` のスナップショット clone で KMP を発行し、publish 段で作り直す | 消費者検証は job 内で発行し直すため消費者が居ず、URL が発行物に焼き込まれるため再ビルドの同一性検査も掛けられない |
| SPM tag を publish 段の先頭に置く | 署名鍵・Portal 認証の失敗でも tag が残る。Android 分の validated の後に置けば、tag が残るのは KMP のビルド / validation 失敗だけになる |
| publish job を ubuntu と macOS の 2 本に割る | 失敗時の drop と deployment ID の引き継ぎが job をまたぐ |
| README の version 置換をリリース PR の中でオーナーが手で行い、validate job が `--check` で止める (翻案元の形) | リリースの手順に手作業が 1 つだけ残る。翻案元が「workflow が README を commit する」を却下した理由は `main` への push 権限と protection のバイパスであり、既定ブランチ `develop` への commit にはその理由が当たらない |
| リリース準備 workflow を別に dispatch し、置換 commit 付きの PR を自動で作る | dispatch が 2 回になり、release が失敗すると README が未公開の版を指す |
| 踏襲分の却下案: tag push トリガー / ファイルを version の正にして tag と照合 / `vX.Y.Z` 表記 / 開発ブランチから起動 / publish 済み version の再実行禁止 / smoke 成功後に tag / workflow が README を `main` に commit / 共有 workflow 化 | KsSettingsView cross/ADR-0020 の Alternatives に理由がある |

## Consequences

- 正: 4 形態が 1 回の手動起動で同一 version で出て、tag は publish 全成功後にのみ生まれる
- 正: 取り消せない操作 (NuGet push・Maven release) より前に、署名・認証・KMP の発行の失敗が出る
- 負: KMP の https 発行は dry-run で予行できず、本番でしか通らない (`file://` 発行と URL 以外は同じ経路)
- 負: 配信リポジトリの tag が Maven release より前に生まれるため、version を放棄したときは tag の手動削除が要る
- 負: publish job が macOS runner になる (KsSettingsView は ubuntu)
- 正: README が指す version は常に公開済みの版で、更新忘れが起きない
- 負: `develop` への push が競合したときは README の追従が次回のリリースまで遅れる

## Revisit When

- KGP が SwiftPM 参照の解決なしに iOS publication を発行できるようになったとき (SPM tag の先行が不要になる)
- 形態ごとに別々の release workflow で発行するようになったとき
- 前提 (Context) が崩れたとき

---
出典: kasane/roadmaps/package-distribution/phases/phase-9-release-workflow/agenda.md (決定事項 R1 と踏襲分) / 同 history.md (2026-09-10: R1) / kasane/roadmaps/package-distribution/phases/phase-7-kmp-packaging/agenda.md (C3 と実装結果の申し送り) / kasane/changes/archive/2026-09-09-add-kmp-maven-distribution/deviation.md (リリース版参照の検証範囲) と evidence/swiftpm-reference-derivation.txt
関連: cross/ADR-0008 (配布モデル) / cross/ADR-0009 (lockstep と version 注入) / cross/ADR-0016 (ブランチモデル) / KsSettingsView cross/ADR-0020 (翻案元)
