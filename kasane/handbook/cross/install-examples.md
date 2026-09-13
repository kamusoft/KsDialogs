---
kind: rule
applies-when:
  always: false
  paths: ["README.md", "README_ja.md", "skills/**"]
  tasks: [利用者向け Skill の追加, インストール手順の記述]
title: インストール例の契約
description: README 2 枚と利用者向け Skill のインストール例が守る形 — version のプレースホルダ・SwiftPM の `exact:`・最新リリースへの案内・英日の同一構成と、その機械検査と限界 (散文は検査の外)
timestamp: 2026-09-13
---

# インストール例の契約

この文書は、ルート README 2 枚 (`README.md` / `README_ja.md`) と利用者向け Skill (`skills/{en,ja}/ksdialogs-*/`) に載せる依存宣言が守る形を定める。読むと、インストール例を書くとき・新しい Skill や補助文書を足すときに何を満たせばよいかが分かる。

インストール例は具体的な version を持たない。リリースはこれらの行を書き換えず、最新版の案内は GitHub Releases に委ねる (cross/ADR-0027)。

## 守る形

- **version はプレースホルダ `{version}` で書く。** 5 経路すべてで構文としては合法だが、どこでも version として解決できない。埋め忘れは依存解決の失敗として利用者に必ず露見する
- **SwiftPM の依存宣言は `exact:` で書く。** `from:` は下限が prerelease であれば prerelease も解決するが、上限が次のメジャー version まで開くため特定の版に固定されない
- **最新リリースへの案内を各ファイルに置く。** 案内先は `https://github.com/kamusoft/KsDialogs/releases/latest` とする。この位置は常にその時点の最新リリースへ解決される
- **英語版と日本語版は同じ構成を持つ。** 同じ文書の集合を持ち、対応する文書は同じ種類・同じ本数のインストール宣言を持つ

宣言の種別は 5 つで、どのファイルがどれを持つかは決まっている。README 2 枚は 5 種すべてを 1 行ずつ持ち、Skill は自 platform の種だけを持つ。

| 種別 | 形 |
|---|---|
| SwiftPM | `.package(url: "https://github.com/kamusoft/KsDialogs-SPM", exact: "{version}")` |
| Maven (core) | `implementation("jp.kamusoft:ksdialogs-core:{version}")` |
| Maven (compose) | `implementation("jp.kamusoft:ksdialogs:{version}")` |
| Maven (kmp) | `api("jp.kamusoft:ksdialogs-kmp:{version}")` |
| NuGet | `<PackageReference Include="KsDialogs.Maui" Version="{version}" />` |

宣言は Skill の入口 (`SKILL.md`) だけに現れるとは限らない。KMP の Skill はホスト別の手順を `references/android-host.md` と `references/ios-host.md` に分けており、そこにも宣言がある。

## 新しい利用者向け Skill・補助文書を足すとき

`skills/{en,ja}/` に Skill を足したら、検査の対象表 (`scripts/install-example-lint.py` の `TARGET_FILES`) にも英日 2 行を登録する。**インストール宣言を持つ文書が対象表に無い状態は検査が失敗する** — 表への登録漏れをそこで捕まえるためである。

宣言を 1 つも持たない Skill は突合の対象に入らないため、その登録漏れは検出されない (英日のどちらかにしか無い Skill は、宣言の有無によらず検出される)。宣言を持たない Skill を対象表に載せるかどうかは書き手が判断する。

`references/` の補助文書も同じ扱いで、宣言を持つものは対象表に登録する。散文にしか宣言が無い文書 (`references/ios-host.md` のような形) は、コードブロックの期待本数を 0 として登録したうえで `PROSE_DECLARATIONS` にも書く。

## 機械検査

検査は `scripts/install-example-lint.py` 1 本で、検証 CI の lint job が走らせる (cross/ADR-0029)。

- **検査** (`python3 scripts/install-example-lint.py`) — 上の 4 つと、対象表と `skills/{en,ja}/` の実構成の突合を行う。違反はファイルと行を添えて出力する
- **自己テスト** (`--selftest`) — 各検査項目が違反を検出できることを確かめる。検査が退化して無音になっても平時は緑のままなので、検出力そのものを別に確かめる

契約の検査が見るのは**コードブロック内の宣言だけ**である。コードブロックの外に置いた依存宣言 4 行 (`skills/{en,ja}/ksdialogs-kmp/SKILL.md` と同 `references/ios-host.md`) にも契約は同じく適用されるが、走査が届かない。**検出 0 件は適合の証明にならない** — 散文の記述まで含めて契約を満たしているかは、書いた人とコードレビューが読んで判定する。

対象表と実構成の突合だけは散文の宣言も数える。宣言が散文の手順にしかない文書を、表への登録漏れとして取りこぼさないためである。

`PROSE_DECLARATIONS` に挙げた組も、コードブロックの外に宣言が実在することだけは検査する。値は見ない (走査の限界はそのまま) が、この一覧が実体を失うと「検査の外に何が残っているか」の記録そのものが実態と食い違うためである。

## 関連

- [リリース手順](release-procedure.md) — リリースはインストール例を書き換えない
- [利用者向け Skill の API 掲載基準](user-skill-api-listing.md) — `skills/` に何を載せるかの基準
- [検証 CI の範囲と実行条件](verification-ci.md) — lint job の検査の集合
