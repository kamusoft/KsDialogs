---
id: 0024
title: release は dispatch 起動・取り消せる順で直列に publish し、SPM tag は KMP の Maven 発行より前に置く
status: accepted
date: 2026-09-10
amends: [cross/0016]
---

## Context

4 形態 (SwiftPM 配信リポジトリ / Maven `ksdialogs-core`・`ksdialogs`・`ksdialogs-kmp` / MAUI NuGet 3 パッケージ) は lockstep 単一バージョンで一斉リリースする (cross/ADR-0009)。release workflow は姉妹ライブラリ KsSettingsView の `release.yml` (同 cross/ADR-0020: dispatch 起動・tag は最後・version 注入) をコピーして固有値を差し替える形で逆流させる。

KsSettingsView には KMP 形態が無い。KsDialogs の KMP は発行物の SwiftPM 連携メタデータに Swift 参照の URL と URL 末尾から導出される `packageName` を焼き込み、cinterop klib を伴う iOS publication の発行は配信リポジトリ `KsDialogs-SPM` に同版の tag が実在しないと失敗する (KGP が発行時に SwiftPM パッケージを解決する)。発行には Xcode が要る。したがって Maven Central へ上げる KMP artifact は既定 URL (https) + exact(version) で、SPM tag の後にしか作れない。翻案元の順序「Maven upload 保留 → NuGet push → Maven release → SPM tag → monorepo tag」はそのままでは成立しない。

消費者検証 (concepts cross/architecture/consumer-verification.md) の KMP 消費者は job 内で kmp/ を `file://` のスナップショット clone で発行し直すため、package 段の KMP 成果物に消費者は居ない。

翻案元は「publish と同じ OS で再ビルドして比較する」ために Android の package job と publish job をともに ubuntu に置いていた。

前提: 配信リポジトリの tag は削除できる。Maven Central の deployment は release するまで drop できる。NuGet push と Maven release は取り消せない。

## Decision

**起動と version**: 起動は `workflow_dispatch` で、version 入力は `^(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)(-(alpha|beta|rc)\.(0|[1-9][0-9]*))?$` のみ通し、`dry-run` 入力で publish 以降を行わない。本番はリリース対象ブランチ `main` からのみ起動し、secrets は Environment `release` に置く。tag push はトリガーにしない。version の SSoT は dispatch 入力 (= tag) で、CI が `-Pversion=` / `-p:Version=` で注入し、リポジトリ内の値は開発用既定値のまま (bump コミットを積まない)。tag は接頭辞なし `X.Y.Z`。

**段構成**: validate → (test ∥ package) → dry-run (消費者検証に artifact を渡す) → publish → 反映待ち → smoke。package 段に KMP の job は置かない (コンパイルは本体検証 `kmp / verify` が担保)。Android の package job は publish job と同じ macOS で作る — publish が署名鍵つきで再ビルドした発行物を package 段のものと比較して「dry-run が見たものと外に出るものが同じ」ことを担保するため、比較の前提 (同じ OS・同じ JDK・同じ commit) を保つ。

**publish の順序**: macOS runner の 1 job で、取り消せる順に直列で行う。再実行は同じ version で「失敗した job から」行い、各ステップは存在検査で冪等化する。失敗時は保留 deployment を drop する。

| 順 | ステップ |
|---|---|
| 1 | SPM スナップショット commit push (差分なしなら skip) |
| 2 | Android の Maven upload 保留 → validated 待ち |
| 3 | SPM tag push (同 commit なら skip) |
| 4 | KMP を https + exact で発行 → upload 保留 → validated 待ち |
| 5 | NuGet push (Trusted Publishing / OIDC) |
| 6 | Maven release 2 件を Android → KMP の順に要求 → 2 枠の published をまとめて待つ |
| 7 | monorepo tag + GitHub Release |
| 8 | README 2 枚と利用者向け Skill のインストール例の version を置き換えた commit を、lint を掛けてから `develop` へ push (競合しても release は失敗にしない) |

**再実行の範囲**: 部分 publish の続行は同じ workflow run の再試行に限る。新規の dispatch で当該 version の外部状態 (配信リポジトリの tag・Maven Central の公開・nuget.org の存在) が既にあり monorepo の tag が起動 commit に無ければ失敗する (別の commit の binary に tag を打たない)。

**tag と README の扱い**: 配信リポジトリの tag は KMP の発行に先立って生まれるため、取り消せない操作 (NuGet push / Maven release) より前に存在する例外である。第一の受け皿は同じ version での再実行で、放棄するときはその番号を欠番にして再利用しない (tag の削除は任意の後片付けで、clone 済みの利用者からは回収できない。手順は handbook の release-procedure)。「tag は publish 全成功後にのみ生まれる」は monorepo の tag について成り立つ。インストール例の version は release workflow が書く — MAUI の pack の前に作業木で置き換えて nupkg に同梱し、publish 成功後に順 8 で `develop` へ commit する。人がリリース PR で version を書く手順は持たず、`main` は次のリリース PR で追従する。cross/ADR-0016 の決定のうち「README の version 置換はリリース PR の中の commit として行う」の 1 文を本決定で置き換える。他の決定 (ブランチ 2 本・`develop` へ直 push・`main` は PR のみ・release は `main` からのみ起動) は維持する。置き換えの commit は `GITHUB_TOKEN` の push のため検証 CI を起動せず、publish job が push の前に検証 CI の lint job と同じ検査を掛ける。

## Alternatives Considered

| 案 | 却下理由 |
|---|---|
| SPM tag を Maven release の後に置く (翻案元の順序) | KMP の iOS publication は同版 tag が無いと発行できず、順序が成立しない |
| package 段でも `file://` のスナップショット clone で KMP を発行し、publish 段で作り直す | 消費者検証は job 内で発行し直すため消費者が居ず、URL が発行物に焼き込まれるため再ビルドの同一性検査も掛けられない |
| SPM tag を publish 段の先頭に置く | 署名鍵・Portal 認証の失敗でも tag が残る。Android 分の validated の後に置けば、tag が残るのは KMP のビルド / validation 失敗だけになる |
| publish job を ubuntu と macOS の 2 本に割る | 失敗時の drop と deployment ID の引き継ぎが job をまたぐ |
| Android の package job を ubuntu のまま、macOS の再ビルドと比べる | OS 差で偽の差異が出たときに切り分けられず、同一性検査が本来止めたい差異 (ソースの取り違え) と区別できない |
| Android の同一性検査を外し、publish の再ビルドだけを信じる | dry-run が見たものと外に出るものの一致を保証する手段が無くなる |
| README の version 置換をリリース PR の中でオーナーが手で行い、validate job が `--check` で止める (翻案元の形) | リリースの手順に手作業が 1 つだけ残る。翻案元が「workflow が README を commit する」を却下した理由は `main` への push 権限と protection のバイパスであり、既定ブランチ `develop` への commit にはその理由が当たらない |
| リリース準備 workflow を別に dispatch し、置換 commit 付きの PR を自動で作る | dispatch が 2 回になり、release が失敗すると README が未公開の版を指す |
| 踏襲分の却下案: tag push トリガー / ファイルを version の正にして tag と照合 / `vX.Y.Z` 表記 / 開発ブランチから起動 / publish 済み version の再実行禁止 / smoke 成功後に tag / workflow が README を `main` に commit / 共有 workflow 化 | KsSettingsView cross/ADR-0020 の Alternatives に理由がある |

## Consequences

- 正: 4 形態が 1 回の手動起動で同一 version で出て、monorepo の tag と GitHub Release は publish 全成功後にのみ生まれる
- 正: 取り消せない操作 (NuGet push・Maven release) より前に、署名・認証・KMP の発行の失敗が出る
- 正: README が指す version は常に公開済みの版で、更新忘れが起きない
- 負: KMP の https 発行は dry-run で予行できず、本番でしか通らない (`file://` 発行と URL 以外は同じ経路)
- 負: 配信リポジトリの tag が Maven release より前に生まれるため、KMP 以降で失敗して放棄した version は「iOS だけ解決できる tag」が残り、番号は欠番になる
- 負: publish job と Android の package job が macOS runner になる (KsSettingsView は ubuntu)
- 負: `develop` への push が競合したときは README の追従が次回のリリースまで遅れる

## Revisit When

- KGP が SwiftPM 参照の解決なしに iOS publication を発行できるようになったとき (SPM tag の先行が不要になる)
- 形態ごとに別々の release workflow で発行するようになったとき
- 前提 (Context) が崩れたとき

---
出典: kasane/roadmaps/package-distribution/phases/phase-9-release-workflow/agenda.md (決定事項 R1・R2・R4 と踏襲分) / 同 history.md (2026-09-10: R1) / kasane/roadmaps/package-distribution/phases/phase-7-kmp-packaging/agenda.md (C3 と実装結果の申し送り) / kasane/changes/archive/2026-09-10-add-release-workflow/design.md (Decision 1〜4) / kasane/changes/archive/2026-09-09-add-kmp-maven-distribution/deviation.md (リリース版参照の検証範囲) と evidence/swiftpm-reference-derivation.txt
現行照合: 2026-09-10 確認 (初回リリース `0.1.0-beta.1` の完了時)。`.github/workflows/release.yml` の publish job が順 1〜8 を 1 job で持ち、続行判定は `scripts/release/check-resume-eligibility.sh` (印 artifact = version・commit・run id) と `central-resume.sh` (枠ごとの状態分岐) に切り出されている。初回リリースでは順 6 の公開待ちが上限 30 分に達して失敗し、同じ run の再実行で整合した (Central の同期は Android 枠 約 60 分 / KMP 枠 27 分)。この観測から公開待ちを「2 枠の release を要求してから 1 本でまとめて待つ (上限 90 分)」に組み替えた (fix-release-published-wait、順 6 の表記はその形)。判定: 維持
関連: cross/ADR-0008 (配布モデル) / cross/ADR-0009 (lockstep と version 注入) / cross/ADR-0016 (ブランチモデル。README 置換の時点を本 ADR が一部改訂) / cross/ADR-0017 (検証 CI。`develop` への push ごとの lint の保証を publish job 内の同じ検査で保つ) / KsSettingsView cross/ADR-0020 (翻案元)
