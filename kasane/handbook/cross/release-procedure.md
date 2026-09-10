---
kind: guide
applies-when:
  always: false
  tasks: [リリースの実施, release workflow の secrets / Environment の設定, リリースの再実行, リリースのリハーサル, 放棄した version の後片付け]
title: リリース手順
description: main ブランチと branch protection の用意、配信リポジトリの deploy key、Environment release と secrets 7 件、nuget.org の Trusted Publisher、リリース PR と dispatch、失敗したときの再実行と 2 枠の deployment の後始末、放棄した version の扱い、dry-run によるリハーサル
timestamp: 2026-09-10
---

# リリース手順

この文書は、4 形態 (SwiftPM 配信リポジトリ / Maven Central の Android 2 座標 / Maven Central の KMP / nuget.org の 3 パッケージ) へ同じ version を 1 回の操作で公開するまでの手順をまとめる。公開は `.github/workflows/release.yml` を手で起動して行い、その前提となる GitHub 側の設定 (ブランチ・Environment・鍵) はオーナーが手作業で用意する。

コマンド例の `<version>` は `0.1.0` または `0.1.0-beta.1` の形の値に読み替える。リポジトリは `kamusoft/KsDialogs` (monorepo) と `kamusoft/KsDialogs-SPM` (SwiftPM 配信リポジトリ) の 2 つを扱う。

インストール例に書かれた version は release workflow が書き換える (cross/ADR-0024)。人が README や Skill の version を手で直す手順は無い。

## ブランチの役割

| ブランチ | 先端が表すもの |
|---|---|
| `develop` | 開発の最新。リポジトリの既定ブランチ。ローカルの作業ブランチをローカルでマージして直接 push する。push のたびに検証 CI (lint + 本体検証 5 job) が事後検証として走り、失敗は通知で拾う |
| `main` | 最新リリース、またはリリース進行中 (リリース PR のマージ後、publish 成功まで) のリリース候補 |

`main` へ入るのは `develop` からの pull request だけで、それ以外の head は CI の lint job が失敗させる。この pull request では lint と本体検証 5 job に加えて消費者検証 4 job が走り、10 件すべてが `main` の必須 status check になる。リリースの起動も `main` に限られる。

既定ブランチは `develop` のままにする (cross/ADR-0016)。`develop` には必須 status check も pull request の必須化も付けない (force-push 禁止と削除禁止だけ)。開発者 1 人が直接 push する運用に合わせた設定である。

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

`app_id` 15368 は GitHub Actions を指す。これを省くと同名の check を出す他のアプリでも必須が満たせてしまう。設定後に `gh api repos/kamusoft/KsDialogs/branches/main/protection` を読み直して 10 件が並ぶことを確かめる。既定ブランチは切り替えないので、`main` 宛ての pull request は base を明示して作る。

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

### リリース PR

1. `docs-refresh` をオーナーが依頼し、`skills/` と README 群を現状へ追随させる
2. 1 を含む pull request を `develop` → `main` で作り、10 件の check が通ったらマージする

インストール例の version は触らない。`main` の README が指す version は 1 つ前のリリースのままになるが、publish の成功後に workflow が `develop` へ書き込み、次のリリース PR で `main` へ入る。

### 起動

`main` の先端がリリース対象の commit になっていることを確かめてから起動する。

```bash
gh workflow run release.yml --ref main -f version=<version>
gh run watch "$(gh run list --workflow=release.yml --limit 1 --json databaseId --jq '.[0].databaseId')"
```

publish は 1 つの job で直列に進み、配信リポジトリへの commit → Android の Maven upload と検証待ち → 配信リポジトリの tag → KMP の発行と upload と検証待ち → nuget.org への push → Maven Central の release 2 件 (Android → KMP の順) を要求してから 2 枠の公開をまとめて待つ (上限 90 分) → monorepo の tag と Release → `develop` へのインストール例の反映、の順になる。検証待ち (上限 30 分) を通らなければ nuget.org へ push する前に止まる。

### 公開後の確認

- nuget.org の 3 パッケージのページ (README が表示されること)
- Maven Central の `jp.kamusoft:ksdialogs-core` / `jp.kamusoft:ksdialogs` / `jp.kamusoft:ksdialogs-kmp` の当該 version
- 配信リポジトリの tag と、monorepo の Release (prerelease の suffix を持つ version は prerelease として作られる)
- `develop` の README 2 枚と利用者向け Skill のインストール例が新しい version になっていること

Release 本文は自動生成ノートのままにし、手で補わない (利用者向けの案内は README が担う)。

## 失敗したとき

publish の各ステップは冪等なので、原因を取り除いてから **同じ version で「失敗した job から再実行」** する。GitHub の実行画面の `Re-run failed jobs` を使う。`Re-run all jobs` でも成立するが、成功済みの検証と配布物の生成をやり直すぶん余計にかかるので原則として使わない。 **完了済みの古い run は再実行しない** — monorepo の tag が起動 commit にある再実行は Release の作成とインストール例の反映を続けて行うため、新しい version を公開した後に古い run を再実行すると `develop` のインストール例がその古い version へ書き戻される。

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
| tag / Release | 同じ内容の tag は skip、別内容なら失敗する。monorepo の tag が起動 commit にある再実行は、レジストリと配信リポジトリへの publish だけを skip し、Release の作成とインストール例の反映は続けて行う (Release が既にあれば本文には触れない) |
| `develop` へのインストール例の反映 | この step の失敗は release を失敗にしない (置換・lint・git 操作のどれが落ちても要約に警告が出るだけ)。次のリリースで追いつく |

保留中の deployment は Android / KMP の 2 枠あり、失敗経路の後始末で削除できる状態 (VALIDATED / FAILED) のものが drop される。削除できない状態 (検証中か公開処理中) のときは何もせず理由が出て ID もそのまま残る (次の attempt がその状態を見て続きを行う)。状態は [Central Portal の deployment 一覧](https://central.sonatype.com/publishing/deployments) で見る。一覧では 2 枠の deployment がどちらも `jp.kamusoft-<version>` の名前で並び、表示名では見分けられない。枠 (Android / KMP) の区別は publish job の Summarize が出す deployment ID の行で行う。手で操作するときは次を使う。

```bash
export MAVEN_CENTRAL_USERNAME=... MAVEN_CENTRAL_PASSWORD=...
scripts/release/central-portal.sh status <deployment-id>
scripts/release/central-portal.sh drop <deployment-id>
```

公開レジストリへ一度出したものは取り消せない (nuget.org は unlist のみ、Maven Central は削除できない)。smoke が失敗しても tag と Release は残したまま、原因を次の version で直す。

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

## 関連

- [検証 CI の範囲と実行条件](verification-ci.md) — 必須 status check になる 10 job の中身
- [ローカル開発環境の準備](local-development-setup.md) — リリース用スクリプトの自己テスト
- cross/ADR-0009 (lockstep の単一 version) / cross/ADR-0016 (ブランチモデル) / cross/ADR-0024 (publish の順序とインストール例の置換)
