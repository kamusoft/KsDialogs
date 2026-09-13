# レビュー結果: install-examples-and-release-notes (002 回目)

**日付**: 2026-09-13
**判定**: APPROVED (ただし **merge の前にオーナー操作 1 件が要る** — 下記「持ち越しの既知項目」)

## サマリー

review-001 の Major 2・3・4 と Minor 1・2・Suggestion のすべてに修正が入り、いずれも**自分の手で退行を注入して検出力を確かめた**。Minor 1 (起点選択) は新しい fixture でのみ落ちる変異を実際に作れたこと、Minor 2 (`PROSE_DECLARATIONS`) は実リポジトリの散文宣言を 1 行消すと exit 1 になることを実測で確認した。review-001 で ✅ だった検出 (具体 version への逆戻り・`from:`・案内の欠落・`references/` の構造突合・`paged` の打ち切り・解析の各退行) はいずれも今も落ちる。

ADR-0030 の amends の切り方は ksn-core `references/decisions.md` の型に沿っており、置き換えた範囲と維持した範囲の線引きも実装 (publish が 順 7 で終わり `develop` へ書き戻さない) と一致する。ADR-0016 を追加で amends する必要は無いと判定した (理由は下記「特に確認を求められた点」4)。lint job の step は 12 本になり、ADR-0029・handbook の表・実際の step が 3 つとも 12 で一致している。

review-001 の Major 1 (既発行 Release `0.1.0-beta.1` の prerelease 印) は**未解決のまま**である。実測で `gh release list` は今も `Pre-release` を返し、`GET /repos/kamusoft/KsDialogs/releases/latest` は 404 を返す。依頼のとおりオーナー担当の既知項目として扱い、これ単独では CHANGES_REQUESTED としないが、**この操作が済むまで本 change は `main` へ入れられない** (16 ファイルに書いた案内先が解決しないため)。

## 照合した規約

| 文書 | 適用のきっかけ |
|---|---|
| `kasane/handbook/cross/verification-ci.md` | workflow の変更・変更の完了判定 |
| `kasane/handbook/cross/release-procedure.md` | release workflow の変更 |
| `kasane/handbook/cross/install-examples.md` | インストール手順の記述・対象表を触る |
| `kasane/handbook/cross/comment-policy.md` | スクリプト 2 本のコメント追加 |
| `kasane/handbook/cross/local-development-setup.md` | 手元の検査手順の改訂 |
| ksn-core `references/decisions.md` (改訂の型・amends の規律・改訂の権利) | accepted ADR への amends の起票 |
| cross/ADR-0016 / 0020 / 0021 / 0022 / 0024 / 0025 / 0026 | lint job の検査の系譜・publish の順序・ブランチモデル |

`always` の handbook は無い。`kasane/lessons/code-review.md` は不在。

## 実行した検証

| 検証 | 結果 |
|---|---|
| lint job の 12 検査を手元で実行 (本検査 + 自己テスト) | すべて exit 0 |
| `scripts/doc-structure-lint.py` (handbook 3 + ADR 4) | 違反なし |
| `install-example-lint.py` への変異注入 (今回分 1 + 退行確認 4 + handbook 記述の照合 2) | 下記 |
| `build-release-notes.py` への変異注入 (今回分 3 + 退行確認 9) | 下記 |
| `gh release list` / `GET /releases/latest` | `Pre-release` / 404 (Major 1 未解決の確認) |

ビルド対象のソース変更は無い (スクリプト・workflow・ドキュメントのみ)。

## 特に確認を求められた点

### 1. Minor 1・Minor 2 の修正は検出力を持つか — **持つ (実測)**

**Minor 2 (`check_prose_declarations`)**

| 入れた退行 | 結果 |
|---|---|
| `lint()` から `check_prose_declarations(root, problems)` の呼び出しを外す | `--selftest` が NG 2 件で落ちる |
| **実リポジトリの** `skills/ja/ksdialogs-kmp/SKILL.md:71` から散文の宣言を消す | 本検査が exit 1 (`Maven 座標 (ksdialogs-kmp) の宣言がコードブロックの外に無い`) |

review-001 の指摘どおり、以前は同じ操作で exit 0 のまま通っていた。値を見ない設計はそのままなので「検査の限界」の記述とも矛盾しない。

**Minor 1 (`select_base` の起点選択)**

`select_base` の順位付け `if best is None or candidate[0] < best[0]:` を 3 通りに壊した。

| 入れた退行 | 落ちたケース |
|---|---|
| `if True:` (常に一覧の最後を採る — review-001 が素通りを実測した形) | **`新しい順の一覧でも最も近い祖先が起点になる` の 1 件だけ** |
| `if best is None:` (常に一覧の最初を採る) | 既存 6 件 |
| `candidate[0] > best[0]` (距離最大を採る) | 7 件 |

新しい fixture が拾っているのは確かに「最後を採る」向きの退行だけで、そこが review-001 の穴だった。「最初を採る」向きは既存の `published` (古い順の 2 件) が拾っているので、2 つの fixture が**並びの向きを揃えていないことで**両方向を覆っている。この依存関係は下記 Suggestion 1 に書いた。

### 2. 既存の検出力を壊していないか — **壊していない (実測)**

`install-example-lint.py` — リポジトリの `README.md` / `README_ja.md` / `skills/` を複写した木に 1 箇所ずつ誤りを入れた。

| 入れた誤り | 結果 |
|---|---|
| README の SwiftPM を `0.1.0-beta.1` へ逆戻り | 検出 (`README.md:47` と実際の値つき) |
| README_ja の SwiftPM を `from:` へ | 検出 (理由つき) |
| MAUI Skill から最新版の案内を消す | 検出 |
| 対象表に無い `references/desktop-host.md` を英日に足す | 検出 (2 件) |

`build-release-notes.py` — 本体に退行を 1 つずつ注入して `--selftest` を回した。

| 入れた退行 | 結果 |
|---|---|
| `paged` を 1 ページ打ち切りに | 検出 (NG 4 件) |
| base ブランチで絞らない | 検出 (NG 2 件) |
| draft を除外しない / 今回の version も起点候補に | それぞれ検出 |
| 祖先判定を外す | 検出 (`KeyError` で落ちる — 静かには通らない) |
| 認識できない行を読み飛ばす | 検出 (NG 3 件) |
| 不正な種別を `docs` に落とす / 空セクションを許す / `- none` と項目の共存を許す | それぞれ検出 |
| `rstrip()` を `strip()` に緩める (字下げ `- none` を受理) | 検出 (Suggestion の修正が効いている) |

lint job の 12 検査は手元で全部緑。ci.yml の step 統合は `check-publish-step-order.py` にも影響していない (緑)。

### 3. ADR-0030 の amends の切り方 — **妥当**

| 観点 | 確認したこと |
|---|---|
| 型 | `amends: [cross/0024]` + Decision 節 1 文での範囲明示 + 0024 の `amended-by: [cross/0030]` + index の 0024 行「一部改訂: 0030 — インストール例の書き戻し」。ksn-core が要求する 3 点セットが揃う。0024 の本文は書き換えていない |
| 置き換えた範囲 | 順序表の 順 8 と「tag と README の扱い」節の後半。0024 の Decision 末尾段落と読み合わせ、対象が過不足なく特定できる |
| 維持した範囲 | 起動と version・段構成・publish の順序 1〜7・再実行の範囲・配信リポジトリの tag の例外と monorepo の tag の扱い |
| supersede を採らなかったこと | 0030 の Alternatives Considered に却下理由 (維持している決定の出所が失われる) があり、実態と合う |

維持した範囲は実装とも一致している。`.github/workflows/release.yml` の publish job は `Push snapshot commit` → Android upload → `Push distribution repository tag` → KMP → NuGet → `Release Maven Central deployments` → `Push monorepo tag` → `Create GitHub Release` (順 7) で終わり、`Update install examples on develop` に相当する step は無い。

`status: proposed` の段階で `amended-by` と index を書いている点は ksn-core の文面 (accepted 昇格時の作業) より前倒しだが、同 change 内の 0026→0029・0022→0026 が同じ形なので運用として一貫している。

### 4. ADR-0016 の扱い — **0027 も 0030 も 0016 を amends すべきではない**

指摘の事実関係 (0016 の index 行が「一部改訂: 0024 — README のインストール例の version 置換の時点」のままで、置換自体は現存しない) は正しい。そのうえで、追加の amends は起票しない方がよいと判断する。

- **置き換える対象が 0016 に残っていない**。0016 の当該 1 文 (`0016-branch-model-develop-main.md` の Decision、「README の version 置換はこのリリース PR の中の commit として行う」) は 2026-09-10 に 0024 が既に置き換えている。amends は「旧 ADR の決定の**一部だけを置き換え、残りは生かす**」型なので、既に置き換え済みの文をもう一度置き換える関係は作れない。作れば 0016 から同じ 1 文に対して 2 本の改訂ポインタが出て、どちらが有効かが読めなくなる
- **辿る経路は規約どおり成立している**。ksn-core は「`amended-by` を持つ ADR を読むときは指す先も読む」と定めており、0016 → 0024 (`amended-by: [cross/0030]`) → 0030 の 2 ホップで現行 (置換の手順は存在しない) に到達する。0030 は Decision 節に「cross/ADR-0024 が cross/ADR-0016 の…を置き換えていた件は、置換そのものが cross/ADR-0027 で無くなったため、どちらの形も現行ではない」と明記し、footer の `関連:` でも 0016 を指しているので、0030 に着いた読み手はその場で分かる
- **index からも 2 行で追える**。0016 行の「一部改訂: 0024」と 0024 行の「一部改訂: 0030 — インストール例の書き戻し」が並んでいる

ただし 0016 行だけを見た読み手が 0024 を現行と読む余地はわずかに残るので、下記 Suggestion 3 に軽い手当てを書いた (`一部改訂: <id>` の形は増やさない — ksn-drift のクイックチェックが `amends` との対称性を見るため)。

### 5. lint job の検査数 — **一致している**

| 数え方 | 値 |
|---|---|
| `.github/workflows/ci.yml` の lint job の検査 step (`Checkout` / `Install gitleaks` / `Pull request head restriction` を除く) | 12 |
| `kasane/handbook/cross/verification-ci.md` の検査表の行数 | 12 |
| 同 job 行・冒頭文・`description` の記述 | 12 |
| cross/ADR-0029 の決定 | 12 |

step 統合によって `Install example lint` が 1 step になり、「本検査と自己テストの 1 対で 1 検査」という数え方が step の見た目とも一致した (統合前は step 13 本で数え方が自明でなかった)。既存 4 検査と同じ「`--selftest` を先に置く 2 行の `run`」の形になっており、`local-development-setup.md` の手元手順 (`--selftest && <本検査>`) とも順序が揃っている。

## 指摘事項

### [🟡 Minor] tasks 10.1〜10.4 が、満たされているのに未チェックのまま

**該当箇所**: `tasks.md:161-166`

**問題点**: 10.1 (`install-example-lint.py` の本検査と `--selftest`)・10.2 (`build-release-notes.py --selftest`)・10.4 (lint job の全検査) は本レビューで手元実行していずれも緑、10.3 (`render` の出力確認) も自己テストの整形群が同じ経路を通しているが、4 つとも `[ ]` のままになっている。1.1 / 10.5 / 10.6 はオーナー操作が要るので未チェックで正しいが、10.1〜10.4 が同じ見た目だと「どれが残っているか」が読み手に伝わらない。蒸留でアーカイブされる証跡でもあるので、実施済みの印は付けておく。

**推奨修正**: 10.1〜10.4 に印を付ける。残る未チェックを 1.1 / 10.5 / 10.6 の 3 つに絞る。

### [🔵 Suggestion] 起点選択の 2 つの fixture が、並びの向きで役割分担していることが書かれていない

**該当箇所**: `scripts/release/build-release-notes.py:508-511` (`published`) と同 `538-548` (`newest_first`)

**問題点**: 実測のとおり、「常に一覧の最後を採る」退行を落とすのは `newest_first` だけ、「常に一覧の最初を採る」退行を落とすのは `published` (古い順) だけである。`newest_first` の側には並びが意図的である旨のコメントが付いたが、`published` の側には無い。後から `published` を「API と同じ新しい順に揃えよう」と善意で並べ替えると、片方向の検出力が静かに消える。

**推奨修正**: `published` の直上に「この一覧は**古い順**のまま置く (`newest_first` と向きを違えることで、順位付けが一覧の端を採る退行を両方向とも落とす)」の 1 行を足す。

### [🔵 Suggestion] cross/ADR-0029 の Consequences に、直前の行で書いた例外と食い違う 1 行が残っている

**該当箇所**: `kasane/decisions/cross/0029-lint-job-includes-install-example-lint.md` Consequences の「負: 対象表は手で維持する…」

**問題点**: 「(登録漏れは突合が落とすので、静かには進まない)」と書いているが、直前の「正:」の行が「宣言を 1 つも持たない Skill は突合の対象に入らないため検出されない」と例外を明示している。宣言を持たない Skill の登録漏れは静かに進むので、この括弧書きだけを読むと Major 4 で直したのと同じ型の過大な保証になる。`status: proposed` なので本文の書き直しで足りる。

**推奨修正**: 括弧書きを「(宣言を持つ文書の登録漏れは突合が落とす)」程度に限定する。

### [🔵 Suggestion] cross/ADR-0016 の index 行から、置換そのものが消えたことが 1 ホップで読めない

**該当箇所**: `kasane/decisions/cross/index.md:22`

**問題点**: 上記「特に確認を求められた点」4 のとおり、追加の amends は起票すべきでない。ただし 0016 行の注記「一部改訂: 0024 — README のインストール例の version 置換の時点」だけを見た読み手は、その置換が今も 0024 の形で行われていると読みうる (0024 行まで目を移せば分かる)。

**推奨修正**: 0016 行の当該注記の末尾に「(置換そのものは 0030 で撤去)」を足す。**`一部改訂: 0030` の形にはしない** — ksn-drift のクイックチェックは `amends: X` を持つ ADR と index の「一部改訂: <id>」の対称性を見るので、`amends` の無い関係に同じ語を使うと構造整備の対象として誤検出される。

### [🔵 Suggestion] deviation.md の「レビュー指摘の修正」が、tasks.md に無い修正のうち 1 件を落としている

**該当箇所**: `deviation.md:15-20`

**問題点**: 記録されているのは ci.yml の step 統合と `check_prose_declarations` の 2 件で、どちらも「tasks.md に無い修正」と書かれている。同じ基準なら `kasane/decisions/cross/0030-release-does-not-write-back-install-examples.md` の新設も該当する (tasks 9.1〜9.3 が挙げる ADR は 0027 / 0028 / 0029 の 3 本で、0030 は含まれない)。他の修正 (handbook `cross/install-examples.md` の記述・ADR-0029 の Consequences・起点選択の fixture・`NONE_MARKER` の注記) はいずれも tasks 8.1 / 9.3 / 5.6 / 5.2 の範囲内なので記録は要らない。

**推奨修正**: 0030 の起票を 1 行足す (「review-001 Major 3 を受けて 0024 への amends を起票。tasks.md に無い成果物」)。

## 持ち越しの既知項目 (オーナー操作待ち)

### [🟠 Major (review-001 から継続)] 既発行 Release `0.1.0-beta.1` が prerelease のままで、案内先が 404 を返す

**該当箇所**: `tasks.md:9` (1.1 未実施) / `README.md:37` ほか 16 ファイル

**現状 (2026-09-13 実測)**:

```
$ gh release list --repo kamusoft/KsDialogs
0.1.0-beta.1  Pre-release  0.1.0-beta.1  2026-09-10T09:03:35Z
$ gh api repos/kamusoft/KsDialogs/releases/latest
{"message":"Not Found", ... "status":"404"}
```

依頼のとおり**これ単独を CHANGES_REQUESTED の理由にはしない**が、解消されるまでは spec Scenario「最新版の案内から目的の版に着地できる」が成立しない。オーナーが `gh release edit 0.1.0-beta.1 --prerelease=false --latest` を実行し、`/releases/latest` が当該版へ解決することを確かめてから tasks 1.1 に印を付け、その後に `main` へ入れる。

## 確認して問題が無かった点

- **Major 4 の修正が実装と一字一句合っている**。`kasane/handbook/cross/install-examples.md` の「宣言を 1 つも持たない Skill は突合の対象に入らないため、その登録漏れは検出されない (英日のどちらかにしか無い Skill は、宣言の有無によらず検出される)」を実測で照合した — 宣言を持たない Skill を**英日両方**に足すと exit 0、**en だけ**に足すと `英語版だけに存在する Skill (日本語版が無い)` で exit 1。括弧書きまで実装どおり。spec は変えていない ((a) 案どおり)
- **`check_prose_declarations` が値を見ない設計を保っている**。「走査の限界はそのまま」というコメントどおり、コードブロック外の宣言は存在だけを見る。散文 4 行 (`skills/{en,ja}/ksdialogs-kmp/SKILL.md` と同 `references/ios-host.md`) が英日対称に実在することを目視でも確認した
- **ci.yml の 1 step 化が既存 4 検査と同じ形**。`--selftest` を先に置いた 2 行の `run` で、GitHub Actions の既定シェル (`bash -e`) により自己テストが落ちれば本検査に到達しない。`Install example lint selftest` という別 step は残っていない
- **ADR-0030 の footer が ksn-core の形に沿う**。`出典:` に proposal / design / review-001 を、`関連:` に 0024 / 0027 / 0029 / 0016 を列挙している。Consequences の 4 行はいずれも Decision と Context から導ける (実装して初めて分かる観測が混じっていない)
- **amends の対称性が 3 組とも揃っている** — 0022↔0026 / 0026↔0029 / 0024↔0030 のいずれも frontmatter 双方向 + index 注記が揃う
- **件数の記述が実体と合う**。`kasane/decisions/index.md` の cross 30 件 = index の行数 30、`kasane/handbook/index.md` の cross 15 件 = `handbook/cross/index.md` の行数 15、列挙も 15 項目
- **`AGENTS.md` の例外規定が消え、`CLAUDE.md` (symlink) 側にも残っていない**ことを実ファイルで確認した
- **削除済み `set-readme-version.py` への生きた参照が無い**。残るのは `kasane/roadmaps/archive/` と `kasane/changes/archive/`、および本 change 自身の記録だけ (いずれも歴史の記録として正しい)
- **`kasane/relations/KsSettingsView.md` の新設と `kasane/outbox/KsSettingsView/…` の削除**は探索段のハーネス台帳の整理で、本 change の実装成果物ではない (指摘対象としない)
- **リリース用スキルと handbook の関係**が変わっていない。`.agents/skills/release/SKILL.md` は手順の写しを持たず `## Changes` の下書きだけを自分で持つ形のままで、`.claude/skills/release` の symlink も生きている
- **2 本のスクリプトが構造 lint・コメント規約 lint を通る**。今回足した `check_prose_declarations` の docstring と `NONE_MARKER` の注記は、いずれも外部文書の ID に依存せず単独で理由が読める

## アクションプラン

1. **オーナー**: `gh release edit 0.1.0-beta.1 --prerelease=false --latest` を実行し、`/releases/latest` の解決を確認してから tasks 1.1 に印を付ける。**これが済むまで `main` へ入れない**
2. tasks 10.1〜10.4 に印を付ける (Minor)
3. `published` fixture の並びが意図的であることを 1 行で書く (Suggestion 1)
4. cross/ADR-0029 の Consequences の括弧書きを限定する (Suggestion 2)
5. `decisions/cross/index.md` の 0016 行に「(置換そのものは 0030 で撤去)」を足す (Suggestion 3)
6. deviation.md に ADR-0030 の起票を 1 行足す (Suggestion 4)
7. 残る検証 tasks 10.5 (`main` からの `dry-run`) と 10.6 (消費者での `{version}` 差し替え) はオーナー実行
8. 蒸留への申し送り: cross/ADR-0027〜0030 はいずれも実装コードに `ADR-NNNN` コメントを持たない。accepted へ昇格させる時点で、`.github/workflows/release.yml` 冒頭 (0030) と `scripts/install-example-lint.py` / `.github/workflows/ci.yml` の該当箇所 (0027 / 0029)、`scripts/release/build-release-notes.py` (0028) に埋め込みの証拠を足す
