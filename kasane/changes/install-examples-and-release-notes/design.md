# Design: install-examples-and-release-notes

## Context

姉妹リポジトリ KsSettingsView が同じ 2 つの契約を組み替え、`0.1.0-beta.3` のリリースまで通している
(`../KsSettingsView/kasane/changes/archive/2026-09-12-install-examples-and-release-notes/`)。
本 change はその最終形 (レビュー 2 周と deviation 7 件を織り込んだ状態) を翻案する。**設計の議論はやり直さず**、
向こうの Decision をそのまま引き継いだうえで、KsDialogs 固有の構造差分だけを新しい判断として置く。

こちらの現状は proposal の Why が示すとおり: 置換機構が `develop` へ書き戻すため既定ブランチ `main` が
1 リリースぶん古い version を示し続け、ラベル分類は集約 pull request 運用と噛み合わず、
prerelease 印が `/releases/latest` を 404 にしている。

## Goals / Non-Goals

**Goals**
- インストール例がリリースのたびに書き換わらない状態にし、最新版の案内を単一の場所 (GitHub Releases) に委ねる
- 契約 (プレースホルダ・`exact:`・案内・英日の同一構成) が崩れたら日常 CI で落ちる
- Release ノートが、人が書いた利用者向けの記述だけで構成される
- 不可逆な公開の後に Release 作成だけが失敗する経路を作らない

**Non-Goals** (proposal の Non-Goals に同じ)
- publish の待機・時間予算 (別 change)
- 既存の分類ラベルの削除、concepts の追随、`readme-example-lint.py` との統合

## Decisions

### Decision 1: インストール例のプレースホルダは `{version}`

**採用案:** 28 行の version を `{version}` に置換する。3 経路の形は次のとおり。

```
.package(url: "https://github.com/kamusoft/KsDialogs-SPM", exact: "{version}")
implementation("jp.kamusoft:ksdialogs-core:{version}")
implementation("jp.kamusoft:ksdialogs:{version}")
api("jp.kamusoft:ksdialogs-kmp:{version}")
<PackageReference Include="KsDialogs.Maui" Version="{version}" />
```

**理由:** 3 経路すべてで構文として合法でありながら、どこでも version として解決できない。
埋め忘れは依存解決の失敗として必ず露見する。

**代替案:**
- **A: `<version>`** — 却下。NuGet の例は XML 属性値なので山括弧が XML を壊す
- **B: `X.Y.Z`** — 却下。それ自体が version の形をしているため埋め忘れが見過ごされ、
  実在の version と誤認される余地が残る

### Decision 2: 散文中の version もプレースホルダにする (KsDialogs 固有)

**採用案:** コードブロック外の 4 行 (`skills/{en,ja}/ksdialogs-kmp/SKILL.md:69` と
同 `references/ios-host.md:7`) も `{version}` にする。lint はコードブロック内だけを走査するため
この 4 行は機械検査の対象外になるが、その限界を handbook に明記する。

**理由:** 検査できないことと契約から外すことは別。散文だけ具体 version が残ると、リリースのたびに
古い値を示し続ける状態が部分的に復活する。翻案元にはこの形の行が無く (向こうは宣言がすべてコードブロック内)、
向こうの handbook も「検出 0 件は適合の証明にならない — 散文は検査対象外」と限界を明記する線を既に引いている。

**代替案:**
- **A: 散文からは version を落とす (`jp.kamusoft:ksdialogs-kmp` だけにする)** — 却下。
  `SKILL.md:69` は「上記の Maven 依存」と直前のコードブロックを指しているので落としても通じるが、
  `ios-host.md:7` は手順の 1 番目でコードブロックを伴わないため、依存宣言の全体を示す必要がある。
  2 箇所で書き方を変えると、どちらが正しいのか読む側に判断させることになる
- **B: 散文も検査できるよう lint の走査をコードブロック外へ広げる** — 却下。
  対象外の文書の例示や引用まで拾い、除外規則が増える (翻案元が同じ理由で退けている)

### Decision 3: lint の対象表と構造の突合を `references/` まで広げる (KsDialogs 固有)

**採用案:** `TARGET_FILES` に `skills/{en,ja}/ksdialogs-kmp/references/android-host.md` と
同 `references/ios-host.md` を含める。実構成との突合も、`SKILL.md` だけでなく
**インストール宣言を持つファイル**を列挙して対象表と突き合わせる。

**理由:** 翻案元は `skills/{en,ja}/*/SKILL.md` だけにインストール宣言を持つため、構造の突合も
`SKILL.md` のパターンだけを見ればよかった。こちらは KMP の Skill が `references/` にホスト別の手順を分けており、
そこにも宣言がある。`SKILL.md` だけを見る突合では、`references/` に宣言が増えたことも、
対象表から漏れたことも検出できない。

**代替案:**
- **A: `references/` の宣言を `SKILL.md` へ集約する** — 却下。`references/` の分割は Skill の読み方
  (必要なホストの手順だけを読む) に沿った構成で、検査の都合で文書構成を変えることになる
- **B: `skills/**/*.md` を全走査して宣言を探す** — 却下。対象を明示しない走査は、
  新しい Skill が対象表に登録されないまま検査を通る状態を作る (Decision 6 と同じ理由)

### Decision 4: ノートの対象は「直前の Release の tag」から今回の commit までの、`main` を base とする pull request

**採用案:** 次の手順で決める。

1. 起点 tag を選ぶ — 今回の version でない / draft でなく公開済み / その tag が `main` の first-parent 上で
   対象 commit の祖先である、の全条件を満たす Release のうち、first-parent 上で対象 commit にもっとも近いもの。
   該当が無ければ履歴の最初から
2. 起点 tag と対象 commit の間の commit を得る
3. 各 commit に紐づく pull request を引き、base が `main` のものだけ残して pull request 番号で重複を除く

**理由:** 単純に「直前の tag」を起点にすると、Release を持たない tag を選びうる。今回の version を除くことで、
Release 作成だけが失敗した再実行でも起点がずれない。base を `main` に限るのは、commit から pull request を引く
API が base を問わないため — 限定しないと `develop` 宛ての個別 pull request が、集約 pull request の記載と二重に載る。

**代替案:**
- **A: 日時で絞る** — 却下。境界が commit ではなく時刻になり、再実行や tag の打ち直しで結果が変わる
- **B: 直前の tag を version 順で選ぶ** — 却下。Release を持たない tag を起点にしうる。version 順は履歴の前後と一致しない
- **C: Release の作成日時で「直前」を決める** — 却下。対象 commit の祖先でない別枝の Release を選びうる
- **D: 起動時に人が本文を貼る** — 却下。複数 pull request から集める以上、手作業の集約は現実的でない

### Decision 5: リハーサル (dry-run) は `main` から起動したときだけ収集する

**採用案:** `main` から起動した `dry-run` は収集・検査・整形・成果物の受け渡しまで本番と同じ経路を通る
(Release だけ作らない)。`main` 以外から起動した場合は収集も検査も行わず、その旨を出力に残す。

**理由:** `dry-run` は起動ブランチの制限が外れているため、`main` 以外だと pull request に紐づかない commit が
範囲に入り、正当なリハーサルが「記載が無い」で落ちる。一方で一律に省くと、API 認証・ページ送り・
commit と pull request の関連付け・成果物の受け渡しという中心の経路が、本番まで一度も実行されない。
取りこぼしは失敗ではなく「項目の足りないノート」として静かに成功しうる。

**代替案:**
- **A: `main` に open な pull request があるときだけ `dry-run` を許す** — 却下。起動ブランチの決定の改訂が要る
- **B: どのブランチでも一律に収集を省く** — 却下。実経路が本番まで未検証のまま残る

### Decision 6: `## Changes` の入力文法を閉じ、認識できない入力は失敗させる

**採用案:** 見出しはちょうど 1 つ。見出しの直後から次の見出しまたは本文末までが範囲。
範囲の空行でない行はすべて `- <種別>: <説明>` または `- none`。種別は breaking / feature / fix / docs の 4 つ。
説明は前後の空白を除いて非空。`- none` は単独でのみ許す。項目 0 個の空セクションは失敗。
出力は種別ごとにまとめ、項目の無い種別の見出しは出さない。

**理由:** 「`- ` で始まる行だけ読む」形にすると、記法違いや地の文が静かに無視され、ノートから項目が消える。
消えたことは誰も気づかない。

**代替案:**
- **A: 種別を小見出しで表す** — 却下。書く側が毎回 4 つの小見出しを用意する負担
- **B: 認識できない行を読み飛ばす / 不正な種別を「その他」に落とす** — 却下。いずれも静かに通り、項目が消える

### Decision 7: ノートは publish より前に確定させ、成果物として publish へ渡す

**採用案:** validate の段で対象 pull request の収集・検査・整形をすべて行い、確定したノート本文と
対象 pull request 番号・起点 tag・対象 commit を成果物として保存する。publish は pull request 本文を読み直さず、
成果物をそのまま Release 本文に使う。validate の `permissions` は `contents: read` と `pull-requests: read` の
**両方を書き切る**。

**理由:** 検査と組み立てを別の段で行うと、その間に pull request 本文が編集された場合に 2 つの壊れ方をする —
再取得した本文が不正になって**不可逆な公開の後に Release 作成だけ失敗する**か、検査したものと違う本文で
Release が作られるか。job レベルの `permissions` は列挙しなかった権限を `none` にするため、
`pull-requests: read` だけを足すと `contents: read` が失われ checkout と Release 取得が壊れる。

**代替案:**
- **A: publish の段で取得と組み立てを行う** — 却下。公開後に Release 作成だけ失敗する経路が残る
- **B: 両方の段で取得して一致を検査する** — 却下。一致しなかったときに publish 後で止まる問題は同じ

### Decision 8: 組み立て処理はスクリプトに切り出し、自己テストで検査する

**採用案:** ノートの組み立て (pull request 本文の集合 → ノート本文) を `scripts/release/` のスクリプトとして
実装し、自己テストを持たせる。workflow はそれを呼ぶ。API も git も使わない純粋な組み立てだけを
単独で実行できるサブコマンドも持たせる。

**理由:** `dry-run` では Release を作らないため、workflow に直書きすると次の本番まで一度も実行されない。
解析と整形は入力を与えれば検査できる。

**代替案:**
- **A: workflow の step に直接書く** — 却下。検証手段が本番のリリースだけになる

### Decision 9: インストール例の lint は対象を明示した表で持つ

**採用案:** `scripts/` に lint を 1 本置き、検査対象 (ファイル・宣言の種別・期待本数) を表として持つ。
撤去する `set-readme-version.py` が持っていた対象知識を引き継ぐ。検査項目 4 つに加えて、
利用者向け Skill の実構成と対象表を突き合わせる (Decision 3 の範囲で)。

**理由:** 対象を明示しないと検査が緩くなる。一方で明示表だけでは、新しい Skill が表に追加されなかったときに
その存在自体を検出できない。両方を持つ。

**代替案:**
- **A: 正規表現でリポジトリ全体を走査する** — 却下。対象外の文書の例まで拾い、除外規則が増え続ける

### Decision 10: リリース用スキルは handbook の単一の入口を読み、順序も handbook が所有する

**採用案:** handbook `cross/release-procedure.md` を「上から順に読めば 1 回のリリースが完了する」単一の入口として
整え、スキルは実行時にそれを読んで書かれた順に従う。スキルが自分で持つのは `## Changes` の下書き手順と、
判断を人へ返す境界の 2 つだけ。どの節をどの順に読むかは持たない。
下書きの範囲は「今回の pull request が `main` へ新しく持ち込む差分」(`origin/main..origin/develop` 相当) に限る。

**理由:** 「参照する節の順序」をスキルが持つと順序の複製そのものになり、handbook で節を足すたびにスキルを直すことになる。
下書きを前回リリース以降の全変更から作ると、既に `main` へ入った pull request の変更が両方に現れ、
ノートに二重に載る。

**代替案:**
- **A: スキルが参照節と順序を持つ** — 却下。順序の複製になり、追随の漏れが生じる
- **B: スキルが順序を所有し handbook は各段の詳細だけを持つ** — 却下。手順の正を移すことになり、
  乖離検査の対象も設計し直しになる
- **C: 下書きを前回リリース以降の全変更から作る** — 却下。二重掲載を人が目視で取り除くことになる

## Risks / Trade-offs

- pull request 本文は後から編集でき、ノートは release 実行時点の本文を読む。マージ済みでも本文を直せば反映される
- `## Changes` の必須化により、記載を欠く pull request が範囲にあると release が止まる (止まる位置は validate)
- リリース用スキルは handbook の更新に追随する必要があるが、漏れを検出する機構は持たない
- 利用者はインストール例の version を 1 箇所差し替える手間を負う
- 散文 4 行は機械検査の外に残る (Decision 2)

## Migration Plan

1. `.github/release.yml` の廃止と組み立て処理の導入は**同時**に行う (分類設定だけ先に消すと、
   次のリリースでノートが空になる)
2. 既発行 Release の prerelease 印は、`/releases/latest` を案内先にする**前に**解除する
   (解除前に README へ案内を書くと、その間 404 になる)
3. `set-readme-version.py` の削除は、インストール例の置換と lint の導入が済んでから行う
4. 遡っての追記は行わない (初回リリース `0.1.0-beta.1` の Release ノートは現状のまま)

## Open Questions

なし (時間予算・ラベル・散文の扱いはいずれも決定済み)。

## ADR 候補

- **Decision 1 + Decision 2** → ADR「インストール例は具体 version を持たない」。
  覆すコストが高く (置換機構の再構築)、利用者が読む公開物の契約を定める
- **Decision 4 + Decision 6 + Decision 7** → ADR「Release ノートは `main` 宛て pull request 本文から組み立てる」。
  pull request の書き方を将来にわたって制約する
- **Decision 9** → cross/ADR-0026 (lint job の検査の集合) の amends
- Decision 3 / 5 / 8 / 10 は上記 ADR の内側の実装判断として、本 design に留める
