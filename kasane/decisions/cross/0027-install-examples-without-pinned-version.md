---
id: 0027
title: インストール例は具体 version を持たず、プレースホルダ `{version}` と最新リリースの案内に委ねる (置換機構を撤去し、GitHub Release は prerelease 印を付けず最新として明示指定する)
status: accepted
date: 2026-09-13
---

## Context

ルート README 2 枚と利用者向け Skill 14 ファイルの 28 行に、貼ってそのまま使える依存宣言がある。これらの version は `scripts/release/set-readme-version.py` (711 行) が release のたびに置換し、publish の成功後に `develop` の先端へ commit して push していた。

2026-09-12 に既定ブランチを `main` へ切り替えた (cross/ADR-0025) ことで、この書き戻し先が収まらなくなった。GitHub でリポジトリを開いた利用者が読むのは `main` の README であり、書き戻しが `develop` にしか届かない以上、**`main` は常に 1 リリースぶん古い version を示し続ける**。手で書く方式より正確さが下がる。

維持しているもの (置換スクリプト 711 行・validate 段の一致検査・handbook の手順・`AGENTS.md` の例外規定) と、得ているもの (「README の行をそのままコピーできる」) が釣り合っていない。

最新版の案内を `https://github.com/kamusoft/KsDialogs/releases/latest` に委ねるには、この位置が解決する必要がある。GitHub Release は version にハイフンを含むかで prerelease 印を機械判定し、`/releases/latest` は prerelease を除外するため、0.x の beta だけを配信している間は 404 を返していた。

前提: 配布は 3 チャネル 5 経路 (SwiftPM / Maven の ksdialogs-core・ksdialogs・ksdialogs-kmp / NuGet)。0.x の beta を配信中で、正式版はまだ無い (cross/ADR-0009 の lockstep 単一 version)。

## Decision

インストール例は具体的な version を持たない。version の位置にはプレースホルダ `{version}` を置き、最新版の案内は GitHub Releases に委ねる。

| 要素 | 決定 |
|---|---|
| プレースホルダ | `{version}`。5 経路すべてで構文としては合法だが、どこでも version として解決できない。埋め忘れは依存解決の失敗として利用者に必ず露見する |
| 適用範囲 | コードブロック内の宣言 24 行に加え、コードブロック外の散文 4 行も同じプレースホルダにする。検査できないことと契約から外すことは別 |
| SwiftPM | 依存宣言は `exact:` で書く。`from:` は上限が次のメジャーまで開くため特定の版に固定されない |
| 最新版の案内 | 各ファイルに `https://github.com/kamusoft/KsDialogs/releases/latest` を置く |
| GitHub Release | semver の prerelease 表記を持つ version でも prerelease の印を付けず、作った Release を最新として**明示的に指定する** (印を外すだけでは最新の選別が自動判定に委ねられる)。既発行の Release の印も解除する |
| 置換機構 | `set-readme-version.py`・`release.yml` の呼び出し 2 箇所 (package 段の pack 前 / publish 後の `develop` への書き戻し)・validate 段の一致検査・`AGENTS.md` の例外規定を一括で撤去する。配布物に同梱される README もプレースホルダのままになる |
| 契約の検査 | リリースではなく日常の検証 CI が行う (cross/ADR-0029) |

守る形と検査の限界は handbook `cross/install-examples.md` が持つ。

## Alternatives Considered

- **プレースホルダを `<version>` にする** — 却下。NuGet の例は XML の属性値であり、山括弧が XML を壊す
- **プレースホルダを `X.Y.Z` にする** — 却下。それ自体が version の形をしているため埋め忘れが見過ごされ、実在の version と誤認される余地が残る
- **置換の書き戻し先を `main` にする** — 却下。`main` は branch protection で pull request と必須 status check を要求しており、CI に迂回させることになる。得られる結果は手作業と同じ
- **書き戻し先を `develop` のままにする (現状維持)** — 却下。既定ブランチ `main` が 1 リリースぶん古い version を示し続ける
- **散文の version だけ具体値のまま残す** — 却下。リリースのたびに古い値を示し続ける状態が部分的に復活する。散文から version を落とす案も、コードブロックを伴わない手順 (`references/ios-host.md`) では依存宣言の全体を示す必要があり、2 箇所で書き方が割れる

## Consequences

- 正: どのブランチで README を読んでも、インストール例が古い version を示すことがなくなる
- 正: リリースがリポジトリへ書き戻さなくなり、置換・lint・git 操作の失敗経路が publish から消える
- 正: `/releases/latest` が beta の版にも解決するため、最新版の案内が単一の位置に定まる
- 負: **破壊的変更** — インストール例をそのまま写した消費者は `{version}` を自分で埋める必要がある (埋め忘れは依存解決の失敗として必ず露見する)
- 負: 契約 (プレースホルダ・`exact:`・案内・英日の同一構成) が崩れても、リリースは止まらない。歯止めは日常 CI の検査だけになる
- 負: 散文 4 行は機械検査の外に残り、適合は人が読んで判定する
- 負: 正式版を配信した後に、正式版を最新に保ったまま候補版を出す必要が生じたら、prerelease 印の扱いを決め直す

## Revisit When

- 正式版 (1.0 以降) を配信し、prerelease を並行して出す必要が生じたとき
- 配布経路が増減して、プレースホルダが構文として合法でない経路が現れたとき
- 利用者から「version を埋める手間」に対する具体的な不満が届いたとき

---
出典: kasane/changes/archive/2026-09-13-install-examples-and-release-notes/proposal.md (Why・What Changes 1〜4) / 同 design.md (Decision 1・2、Migration Plan) / ../KsSettingsView/kasane/outbox/KsDialogs/2026-09-12-install-examples-and-release-notes-decisions.md (sibling の決定)
関連: cross/ADR-0025 (既定ブランチ main — 書き戻し先が収まらなくなった原因) / cross/ADR-0024 (publish の順序とインストール例の置換の時点。撤去に伴う改訂は cross/ADR-0030) / cross/ADR-0029 (契約の検査を lint job に加える) / cross/ADR-0028 (Release ノートの組み立て) / cross/ADR-0030 (release がインストール例を書き戻さない — 本決定に伴う 0024 の改訂)
