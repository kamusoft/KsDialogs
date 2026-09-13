---
kind: guide
applies-when:
  always: false
  tasks: [リリースの実施, release workflow の secrets / Environment の設定, リリースの再実行, リリースのリハーサル, 放棄した version の後片付け]
title: リリース手順
description: main ブランチと branch protection の用意、配信リポジトリの deploy key、Environment release と secrets 7 件、nuget.org の Trusted Publisher、事前確認からリリース PR (`## Changes` の記入)・起動・見守り・公開後の確認までの各段、validate 段と publish 段に分けた失敗時の再実行と 2 枠の deployment の後始末、反映待ちの失敗を分類から読み分ける手掛かり、放棄した version の扱い、dry-run によるリハーサル
timestamp: 2026-09-13
---

# リリース手順

この文書は、4 形態 (SwiftPM 配信リポジトリ / Maven Central の Android 2 座標 / Maven Central の KMP / nuget.org の 3 パッケージ) へ同じ version を 1 回の操作で公開するまでの手順をまとめる。公開は `.github/workflows/release.yml` を手で起動して行い、その前提となる GitHub 側の設定 (ブランチ・Environment・鍵) はオーナーが手作業で用意する。

コマンド例の `<version>` は `0.1.0` または `0.1.0-beta.1` の形の値に読み替える。リポジトリは `kamusoft/KsDialogs` (monorepo) と `kamusoft/KsDialogs-SPM` (SwiftPM 配信リポジトリ) の 2 つを扱う。

インストール例は具体的な version を持たず、リリースはその行を書き換えない (cross/ADR-0027)。README や Skill の version を人が手で直す手順も無い。守る形は [インストール例の契約](install-examples.md) が持つ。

## この手順書の使い方

この文書がリリース手順の正であり、1 回のリリースはここを上から順に読めば完了する。

1. **初回だけ行う設定** — 済んでいれば飛ばす
2. **リリースのたびに行うこと** — 事前確認から公開後の確認まで、書かれた順に実行する
3. **失敗したとき** — 実行が止まったときだけ開く
4. **リハーサル** — 配信先を変えずに経路を通したいときだけ開く

リリース用スキル (`.agents/skills/release/SKILL.md`) は、実行時にこの文書を読んで書かれた順に従う薄い層である。段の順序・節の並び・コマンドはいずれもこの文書が持ち、スキルの側には写しを持たない。手順を変えるときはこの文書だけを直せばよい。判断 — version 番号の決定、`## Changes` の最終的な文面、失敗したときに再実行するかどうか — は人が行い、スキルは代行しない。

## ブランチの役割

| ブランチ | 先端が表すもの |
|---|---|
| `develop` | 開発の最新。ローカルの作業ブランチをローカルでマージして直接 push する。push のたびに検証 CI (lint + 本体検証 5 job) が事後検証として走り、失敗は通知で拾う |
| `main` | 最新リリース、またはリリース進行中 (リリース PR のマージ後、publish 成功まで) のリリース候補。リポジトリの既定ブランチ (cross/ADR-0025) |

`main` へ入るのは `develop` からの pull request だけで、それ以外の head は CI の lint job が失敗させる。この pull request では lint と本体検証 5 job に加えて消費者検証 4 job が走り、10 件すべてが `main` の必須 status check になる。リリースの起動も `main` に限られる。

既定ブランチは `main` で、利用者がリポジトリを開いたときに最新リリースの README が見える (cross/ADR-0025)。`develop` には必須 status check も pull request の必須化も付けない (force-push 禁止と削除禁止だけ)。開発者 1 人が直接 push する運用に合わせた設定である。

## 初回だけ行う設定

### main の作成と保護

`develop` から `main` を作り、作った直後に保護を付ける。必須 status check は 10 件で、名前は `.github/workflows/ci.yml` の job 名 (再利用可能 workflow を呼ぶ job は「呼び出し側 / verify」) と一致させる。

```bash
gh api -X POST repos/kamusoft/KsDialogs/git/refs \
  -f ref=refs/heads/main \
  -f sha="$(gh api repos/kamusoft/KsDialogs/git/ref/heads/develop --jq .object.sha)"
```

保護は完全な payload を PUT する (`gh api -X PUT` は部分更新にならず、書かなかった項目は消える)。

```bash
gh api -X PUT repos/kamusoft/KsDialogs/branches/main/protection --input - <<'JSON'
{
  "required_status_checks": {
    "strict": false,
    "checks": [
      { "context": "lint", "app_id": 15368 },
      { "context": "ios / verify", "app_id": 15368 },
      { "context": "android / verify", "app_id": 15368 },
      { "context": "android-instrumented / verify", "app_id": 15368 },
      { "context": "kmp / verify", "app_id": 15368 },
      { "context": "maui / verify", "app_id": 15368 },
      { "context": "consumer-ios / verify", "app_id": 15368 },
      { "context": "consumer-android / verify", "app_id": 15368 },
      { "context": "consumer-maui / verify", "app_id": 15368 },
      { "context": "consumer-kmp / verify", "app_id": 15368 }
    ]
  },
  "enforce_admins": false,
  "required_pull_request_reviews": {
    "dismiss_stale_reviews": false,
    "require_code_owner_reviews": false,
    "required_approving_review_count": 0
  },
  "restrictions": null,
  "allow_force_pushes": false,
  "allow_deletions": false
}
JSON
```

`app_id` 15368 は GitHub Actions を指す。これを省くと同名の check を出す他のアプリでも必須が満たせてしまう。設定後に `gh api repos/kamusoft/KsDialogs/branches/main/protection` を読み直して 10 件が並ぶことを確かめ、既定ブランチを `main` に切り替える (`gh api -X PATCH repos/kamusoft/KsDialogs -f default_branch=main`)。

### 配信リポジトリの deploy key

publish job は配信リポジトリへ commit と tag を push する。書き込み可の deploy key を作り、公開鍵を配信リポジトリへ、秘密鍵を monorepo の Environment secret へ置く。

```bash
ssh-keygen -t ed25519 -C "KsDialogs release" -N "" -f ./spm-deploy-key
gh repo deploy-key add ./spm-deploy-key.pub \
  --repo kamusoft/KsDialogs-SPM --title "release workflow" --allow-write
```

秘密鍵はこのあとの secret 登録に使い、登録が終わったら鍵ファイル 2 つを `trash` で消す。

### Environment release と secrets

secrets は Environment `release` にだけ置く。publish job だけがこの Environment を参照し、deployment branch policy が `main` 以外からの参照を拒む。required reviewers は付けない (起動そのものが手動のゲートになっている)。

GitHub の画面で Environment `release` を作り、Deployment branches を `Selected branches` にして `main` を追加してから、次の 7 件を登録する。

| secret | 中身 |
|---|---|
| `MAVEN_CENTRAL_USERNAME` | Central Portal の User Token のユーザー名 |
| `MAVEN_CENTRAL_PASSWORD` | Central Portal の User Token のパスワード |
| `SIGNING_KEY` | GPG 秘密鍵の armored export |
| `SIGNING_KEY_ID` | GPG 鍵の短い ID |
| `SIGNING_PASSWORD` | GPG 鍵のパスフレーズ |
| `NUGET_USER` | nuget.org のユーザー名 |
| `SPM_DEPLOY_KEY` | 配信リポジトリの deploy key の秘密鍵 |

`SPM_DEPLOY_KEY` 以外の 6 件は姉妹ライブラリ KsSettingsView と同じ値を使う (Central Portal のアカウント・GPG 鍵・nuget.org のユーザーが同じため)。`SPM_DEPLOY_KEY` だけは配信リポジトリごとに別の鍵を作る。

鍵の中身はファイルに落とさず、標準入力へ直接流し込む。

```bash
gpg --armor --export-secret-keys <鍵 ID> | gh secret set SIGNING_KEY --env release
gh secret set SPM_DEPLOY_KEY --env release < ./spm-deploy-key
trash ./spm-deploy-key ./spm-deploy-key.pub
```

### nuget.org の Trusted Publisher

publish job は長期の API key を持たず、実行のたびに短命な key を OIDC で受け取る。nuget.org 側に、この monorepo の `release.yml` と Environment `release` を指す Trusted Publisher Policy を登録する。

| 項目 | 値 |
|---|---|
| Repository | `kamusoft/KsDialogs` |
| Workflow | `release.yml` |
| Environment | `release` |
| Package glob | `KsDialogs.*` |
| Scopes | push のみ |

## リリースのたびに行うこと

出す version を決めたら、次の 5 段を上から順に実行する。各段の見出しの下に、その段を終えた状態 (到達状態) を書く。到達していないまま次の段へ進まない。

### 1. 事前確認

到達状態: `develop` の検証 CI が緑で、利用者向けドキュメントの追随の要否が判断できている。

`develop` の先端に対する検証 CI が成功していることを確かめる。

```bash
gh run list --branch develop --workflow=ci.yml --limit 1 \
  --json conclusion,headSha,url --jq '.[0]'
```

失敗しているときは先へ進まない。原因を直して緑にしてから戻る。

`skills/` と README 群が現状から遅れていないかを見て、遅れていれば `docs-refresh` をオーナーが依頼する (このスキルは自発的に発動しない)。追随した更新はこの後のリリース PR に含める。

### 2. リリース PR

到達状態: `develop` → `main` の pull request が 10 件の check を通してマージされ、`main` の先端がリリース対象の commit になっている。

1. `## Changes` に書く材料を集める。範囲は**この pull request が `main` へ新しく持ち込む差分**だけで、前回のリリース以降の全変更ではない (既に `main` へ入った変更は、それを持ち込んだ pull request の記載が既に持っている。両方に書くとノートへ二重に載る)。

```bash
git fetch origin main develop
git log --no-merges --reverse origin/main..origin/develop --pretty=format:'%h %s'
```

2. pull request を `develop` → `main` で作り、本文の `## Changes` セクションに利用者向けの変更だけを書く
3. 10 件の check が通ったらマージする

本文は `.github/pull_request_template.md` の雛形から始める。書式と種別の一覧は雛形の「記入の仕方」が持つ (利用者向けの変更が無いときは `- none` を単独で置く)。**説明は英語で書く** — Release ページは閲覧者の言語圏を仮定しない利用者向けの公開物であり、種別の見出しと定型文言も英語で出る。**このセクションの項目が GitHub Release 本文の材料になる** — 書いた順や字面がそのまま出るのではなく、workflow が項目を種別別に再編し、種別の見出し・pull request 番号・前回の版との比較リンクを付けて整形する。

`## Changes` を持たない pull request、または認識できない行を含む pull request が範囲にあると、release は publish に入る前に止まる (cross/ADR-0028)。範囲に入るのは前回の公開済み Release 以降に `main` へマージされた pull request すべてなので、書き忘れは次のリリースのときに露見する。手元で先に確かめるなら、`main` からのリハーサル (下記) を 1 回回す。

### 3. 起動

到達状態: release workflow の run が始まり、その URL が分かる。

`main` の先端がリリース対象の commit になっていることを確かめてから起動する。

```bash
gh workflow run release.yml --ref main -f version=<version>
```

### 4. 見守り

到達状態: 全 job が成功し、tag 2 本・GitHub Release・4 形態の公開・smoke まで終わっている。

```bash
gh run watch "$(gh run list --workflow=release.yml --limit 1 --json databaseId --jq '.[0].databaseId')"
```

publish は 1 つの job で直列に進み、配信リポジトリへの commit → Android の Maven upload と検証待ち → 配信リポジトリの tag → KMP の発行と upload と検証待ち → nuget.org への push → Maven Central の release 2 件 (Android → KMP の順) を要求してから 2 枠の公開をまとめて待つ (上限 90 分) → monorepo の tag と Release、の順になる。検証待ち (上限 30 分) を通らなければ nuget.org へ push する前に止まる。

所要時間の目安は初回リリース (`0.1.0-beta.1`、2026-09-10) の実測で、dry-run 段 (validate → 本体検証 5 ∥ package 3 → 消費者 dry-run 4) まで約 18 分、publish は Central の公開待ちが支配的で Android 枠が約 60 分・KMP 枠が約 27 分、反映待ちと smoke 4 本で約 10 分、壁時計は再実行を含めて約 2 時間だった。publish job の timeout 150 分は本体の作業約 16 分と公開待ちの上限 90 分を足した実測ベースの予算で、待ちがすべて上限まで伸びる最悪ケースは job timeout で止めて再実行に回す。

Release の本文は validate の段で確定済みで、publish は pull request 本文を読み直さない。version が prerelease の表記 (`-beta.1` 等) を持っていても GitHub の prerelease 印は付けず、作った Release を最新として明示的に指定する (cross/ADR-0027)。

途中で止まったら「失敗したとき」へ。

### 5. 公開後の確認

到達状態: 4 形態の公開物と Release ページを実物で確認できている。

- nuget.org の 3 パッケージのページ (README が表示されること)
- Maven Central の `jp.kamusoft:ksdialogs-core` / `jp.kamusoft:ksdialogs` / `jp.kamusoft:ksdialogs-kmp` の当該 version
- 配信リポジトリの tag と、monorepo の Release 本文
- `https://github.com/kamusoft/KsDialogs/releases/latest` が今回の版に解決すること (README と利用者向け Skill のインストール例は具体 version を持たず、この案内に委ねている)

Release 本文は pull request の `## Changes` から組み立てられたものであり、手で補わない。文面を直したいときは次のリリースの `## Changes` の書き方を直す。

## 失敗したとき

### publish に入る前 (validate) の失敗

公開物には何も起きていない。原因を直してから同じ version で起動し直すか、`Re-run failed jobs` で再実行する。

| 失敗の理由 | 直し方 |
|---|---|
| 対象の pull request に `## Changes` が無い / 認識できない行がある | ログが原因の pull request と行を示す。その pull request の本文を直してから再実行する (ノートは実行時の本文を読むので、マージ済みでも直せば反映される) |
| version の形式が不正 | 入力した version を見直す |
| 同名の tag が別の commit を指す | その version は出し直せない。次の version で出す。同名の tag が**起動した commit を指している**場合は衝突ではなく、workflow は再実行として続行する |
| 配信リポジトリの同名 tag が別内容 | 同上。片側だけ公開された状態を作らないよう、不可逆な操作に入る前にここで止まる |

`## Changes` の記載の不備は、手元でも確かめられる。`render` は GitHub API も git も使わず、pull request 本文を模した JSON (`[{"number": 42, "body": "..."}]`) だけで組み立てを再現する。

```bash
python3 scripts/release/build-release-notes.py render \
    --pulls <本文を並べた JSON のパス> --repo kamusoft/KsDialogs --version <version>
```

### publish の途中での失敗

publish の各ステップは冪等なので、原因を取り除いてから **同じ version で「失敗した job から再実行」** する。GitHub の実行画面の `Re-run failed jobs` を使う。`Re-run all jobs` でも成立するが、成功済みの検証と配布物の生成をやり直すぶん余計にかかるので原則として使わない。

**新規の dispatch では続きを埋められない。** 同じ version を新しい run から起動すると、当該 version の外部状態が既にあり monorepo の tag が起動 commit に無い場合は publish が止まる (公開済みの binary と tag が指す source の対応を保証できないため)。必ず失敗した run を再実行する。

**続きを埋められるのは、部分 publish を行った run そのものの再実行だけ。** publish job は「当該 version の外部状態が 1 つも無い」ことを自分で確認した試行だけが印を残し、以後の試行はその印が今回の version・commit・run と一致するときにだけ続きを埋める。外部状態を理由に止まった run を再実行しても、印が無いため同じ理由で止まる (この場合の案内は「部分 publish を行った run そのものを再実行する」)。どの run も引き継げない状態になったら、その version は放棄して次の番号へ進む。

| 失敗した位置 | 再実行で起きること |
|---|---|
| 配信リポジトリへの commit push | 差分が無ければ commit を skip して先へ進む |
| Android の Maven upload | 枠の前回 deployment の状態で分岐する (検証済みなら upload せず release へ。削除済みなら upload をやり直す) |
| Android の検証待ち (上限超過) | 検証中の deployment は削除できないので ID が残り、次の attempt が同じ deployment の決着を待つところから続ける |
| 配信リポジトリの tag push | 内容が同じ tag は skip、別内容なら失敗する |
| KMP の発行 (ビルド) | 配信リポジトリの tag は残ったまま。原因を直して再実行すれば、tag は内容一致で skip され発行からやり直す |
| KMP の Maven upload / 検証 | Android 枠と同じ分岐。Android 枠の保留 deployment は失敗経路の後始末で drop される |
| nuget.org の push | 公開済みのパッケージは skip される |
| Maven の release | 枠ごとに保留中の deployment を release する。応答を取りこぼして PUBLISHING のまま残った枠は、公開の完了を待つだけで release を送り直さない |
| Maven の公開待ち (上限超過) | 公開処理中の deployment は削除できないので ID が残る。次の attempt は release を送り直さず、2 枠まとめて公開の完了を待つ step へ回す (待ちは再実行でも 1 本で、上限は同じ 90 分) |
| tag / Release | 同じ内容の tag は skip、別内容なら失敗する。monorepo の tag が起動 commit にある再実行は、レジストリと配信リポジトリへの publish だけを skip し、Release の作成は続けて行う (Release が既にあれば本文には触れない)。本文は validate が確定させた成果物から取るため、再実行でも同じ内容になる |

保留中の deployment は Android / KMP の 2 枠あり、失敗経路の後始末で削除できる状態 (VALIDATED / FAILED) のものが drop される。削除できない状態 (検証中か公開処理中) のときは何もせず理由が出て ID もそのまま残る (次の attempt がその状態を見て続きを行う)。状態は [Central Portal の deployment 一覧](https://central.sonatype.com/publishing/deployments) で見る。一覧では 2 枠の deployment がどちらも `jp.kamusoft-<version>` の名前で並び、表示名では見分けられない。枠 (Android / KMP) の区別は publish job の Summarize が出す deployment ID の行で行う。手で操作するときは次を使う。

```bash
export MAVEN_CENTRAL_USERNAME=... MAVEN_CENTRAL_PASSWORD=...
scripts/release/central-portal.sh status <deployment-id>
scripts/release/central-portal.sh drop <deployment-id>
```

公開レジストリへ一度出したものは取り消せない (nuget.org は unlist のみ、Maven Central は削除できない)。smoke が失敗しても tag と Release は残したまま、原因を次の version で直す。

### 反映待ちでの失敗

publish が終わった後、反映待ち job が上限 (45 分) まで待っても反映を確認できずに失敗することがある。公開そのものは済んでいるので作り直すものは無い。失敗のログは待ち対象 10 件それぞれについて、その時点で保持していた分類を出す。レジストリが遅いのか壊れているのかは、この分類で読み分ける。

| ログの分類 | 起きていること | 次の手 |
|---|---|---|
| 未反映 | 照会は届いていて、当該 version がミラーにまだ無い | 同期の遅れ。時間を置いてから `Re-run failed jobs` で反映待ちと smoke だけを回し直す |
| 判定不能 (通信そのものの失敗) | 要求が相手に届かない、または応答が返らない | runner 側かレジストリ側の障害。Central Portal と nuget.org の稼働状況を見てから回し直す |
| 判定不能 (応答が成功を示さない) | 2xx でも 404 でもない応答 (5xx など) が返っている | レジストリ側の不調。同上 |
| 判定不能 (応答を解釈できない) | 応答の形が想定と違い、version の有無を読み取れない | レジストリの応答仕様が変わった可能性。`scripts/release/wait-for-registries.sh` の判定を見直す (待ち直しでは直らない) |
| 未照会 | 上限に達するまで一度もその対象を照会しなかった | 手前の対象の照会が時間を使い切っている。判定不能の対象を先に疑う |

10 件すべてが未反映なら待てば解決する見込みで、判定不能が混ざっていれば待っても解決しない可能性がある。分類が分かれている (反映済みが含まれる) ときは、公開が届いていること自体は確認できている。

### version を放棄するとき

配信リポジトリの tag は KMP の発行より前に作られるため (cross/ADR-0024)、KMP 以降で失敗した version を再実行せずに放棄すると「iOS だけ解決できる tag」が残る。

- **放棄した番号は欠番にして再利用しない。** 公開済みの tag は削除しても clone 済みの利用者からは回収できないため、同じ番号で作り直すと利用者ごとに違う内容が解決される
- tag の削除は任意の後片付けにすぎない。行うなら次のとおり (monorepo の tag は publish の最後に作られるので、放棄した version には存在しない)

```bash
git clone --quiet git@github.com:kamusoft/KsDialogs-SPM.git /tmp/ksdialogs-spm
git -C /tmp/ksdialogs-spm push --delete origin <version>
```

## リハーサル

`dry-run` を true にすると publish 以降を行わず、起動ブランチの制限も外れる。配信先の状態は一切変わらないので、workflow を変更したときはこれで validate から消費者検証までを通しておく。

```bash
gh workflow run release.yml --ref <branch> -f version=<version> -f dry-run=true
```

`<version>` には実際に出す予定の値を与える。KMP の発行だけは dry-run に含まれない (発行物に配信リポジトリの https URL と tag が焼き込まれるため、tag を打つ本番でしか通せない)。`scripts/release/` の判定を触ったときは、あわせて自己テストを手元で回す ([ローカル開発環境の準備](local-development-setup.md#リリース用スクリプトの自己テストを回す))。

Release ノートの扱いは起動ブランチで変わる。

| 起動ブランチ | Release ノートの扱い |
|---|---|
| `main` | 対象の収集・検査・整形と成果物への受け渡しまでを本番と同じ経路で行う (Release だけ作らない)。リリース PR のマージ後に一度回すと、`## Changes` の不備で本番の validate が落ちる事態を避けられる |
| `main` 以外 | 収集も検査も行わない。`main` の pull request に紐づかない commit なので、記載が無いことを理由に失敗させない |

## 関連

- [検証 CI の範囲と実行条件](verification-ci.md) — 必須 status check になる 10 job の中身
- [インストール例の契約](install-examples.md) — リリースが書き換えない例の守る形
- [ローカル開発環境の準備](local-development-setup.md) — リリース用スクリプトの自己テスト
- 公開の枠組みの ADR: cross/ADR-0009 (lockstep の単一 version) / cross/ADR-0016 (ブランチモデル) / cross/ADR-0024 (publish の順序)
- 利用者から見える契約の ADR: cross/ADR-0027 (インストール例と最新リリースの指定) / cross/ADR-0028 (Release ノートの組み立て) / cross/ADR-0030 (release がインストール例を書き戻さない)
