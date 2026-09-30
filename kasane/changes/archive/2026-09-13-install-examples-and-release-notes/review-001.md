# レビュー結果: install-examples-and-release-notes (001 回目)

**日付**: 2026-09-13
**判定**: CHANGES_REQUESTED

## サマリー

翻案元の設計を忠実に写したうえで、KsDialogs 固有の構造差分 (16 ファイル 28 行・Maven 3 座標・`references/` の宣言・散文 4 行) を設計どおりに扱えている。2 本の新規スクリプトは検出力を実測で確かめた限り堅く、workflow の組み替え (validate での確定・成果物の受け渡し・分岐の単一評価) にも穴は見つからなかった。

一方で、**merge 可能な状態になっていない** — design の Migration Plan が「案内を書く前に行う」と明示した既発行 Release の prerelease 印の解除が未実施で、いま 16 ファイルに書き込んだ `/releases/latest` は実測で 404 を返す。あわせて、lint job の step の形が本 change 自身の ADR-0029・handbook・既存 4 検査の形と食い違っていること、accepted の cross/ADR-0024 の一部を撤去したのに amends が起票されていないことを Major として挙げる。

## 照合した規約

| 文書 | 適用のきっかけ |
|---|---|
| `kasane/handbook/cross/verification-ci.md` | workflow の変更・変更の完了判定 |
| `kasane/handbook/cross/release-procedure.md` | release workflow の変更 |
| `kasane/handbook/cross/install-examples.md` (本 change で新設) | インストール手順の記述・対象表を触る |
| `kasane/handbook/cross/comment-policy.md` | 新規スクリプト 2 本のコメント |
| `kasane/handbook/cross/docs-refresh-timing.md` / `user-skill-api-listing.md` | `skills/**` の更新 |
| ksn-core `references/decisions.md` (改訂の型・改訂の権利) | accepted ADR との衝突判定 |
| cross/ADR-0016 / 0020 / 0021 / 0022 / 0024 / 0025 / 0026 | ブランチモデル・lint job の検査の系譜・publish の順序 |

`always` の handbook は無い。`kasane/lessons/code-review.md` は不在。

## 実行した検証

| 検証 | 結果 |
|---|---|
| `scripts/install-example-lint.py` (本検査 / `--selftest`) | 0 / 0 (16 ファイル 24 行) |
| `scripts/release/build-release-notes.py --selftest` | 0 (38 件すべて OK) |
| lint job の他 11 検査を手元で実行 | すべて 0 (`local-path` / `identity` / `comment-policy` / `scenario-id-coverage` / `ci-skip` / `readme-example` / `check-time-budget` / `check-publish-step-order`) |
| `scripts/doc-structure-lint.py` (新設 handbook 1 + ADR 3 + 改訂 2) | 違反なし |
| 変異注入 (build-release-notes 17 種 / install-example-lint 11 種) | 下記「検出力の実測」 |

ビルド対象のソース変更は無い (スクリプト・workflow・ドキュメントのみ)。

## 指摘事項

### [🟠 Major] 既発行 Release が prerelease のままで、書き込んだ案内先が 404 を返す

**該当箇所**: `kasane/changes/install-examples-and-release-notes/tasks.md:9` (1.1 未実施) / `README.md:37` ほか 16 ファイル

**問題点**: 実測で `GET /repos/kamusoft/KsDialogs/releases/latest` が 404、`gh release list` も `0.1.0-beta.1` を `Pre-release` と表示する。design の Migration Plan 2 は「既発行 Release の prerelease 印は、`/releases/latest` を案内先にする**前に**解除する (解除前に README へ案内を書くと、その間 404 になる)」と明示しており、tasks.md も 1.1 を「最初に行う」と書いているが、印は解除されないまま案内 16 ファイルぶんが先に入っている。この状態で `main` へ入ると、spec Scenario「最新版の案内から目的の版に着地できる」が成立しない — 利用者は `{version}` を埋める先を知る手段を失う。

**推奨修正**: `gh release edit 0.1.0-beta.1 --prerelease=false --latest` を実行し、`/releases/latest` が当該版に解決することを確かめてから tasks 1.1 に印を付ける。解除が済むまで本 change を `main` へ入れない。

### [🟠 Major] lint job の install-example-lint が 2 step に割れ、自己テストが本検査の後に走る

**該当箇所**: `.github/workflows/ci.yml:103-113`

**問題点**: 実装は `Install example lint` (本検査) と `Install example lint selftest` (自己テスト) の 2 step で、自己テストが後にある。これは本 change 自身が書いた 3 つの記述すべてと食い違う。

- `kasane/decisions/cross/0029-lint-job-includes-install-example-lint.md` の「数え方」行: 「本検査と自己テスト (`--selftest`) の 1 対で 1 検査と数える。**自己テストを本検査より先に同じ step で走らせる**」
- `kasane/handbook/cross/verification-ci.md:32`: 「自己テストを持つ検査は、自己テストを本検査より先に同じ step で走らせる (cross/ADR-0020) — その 1 対を 1 検査と数える」
- `kasane/handbook/cross/local-development-setup.md:213`: 手元手順も `--selftest && <本検査>` の順

同 job の既存 4 検査 (`CI skip allowlist lint` / `README example lint` / `Release time budget check` / `Publish step order check`) はいずれも 1 step 内で `--selftest` を先に走らせており、新設分だけが例外になっている。実害は 2 つ — (1) 検出器が退行したとき、ログ上は本検査の緑が先に出てから自己テストが落ちるため、ADR-0020 の理屈 (検出力を確かめてから緑を信じる) が成立しない、(2) step が 13 本になり「12 検査」の数え方が読み手から自明でなくなる。`deviation.md` にも記録が無い (「乖離: なし」)。

**推奨修正**: 既存 4 検査と同じ形の 1 step にまとめる。

```yaml
      - name: Install example lint
        run: |
          python3 scripts/install-example-lint.py --selftest
          python3 scripts/install-example-lint.py
```

### [🟠 Major] cross/ADR-0024 (accepted) の一部を撤去したのに amends が起票されていない

**該当箇所**: `kasane/decisions/cross/0027-install-examples-without-pinned-version.md` (frontmatter と footer) / `kasane/decisions/cross/0024-release-dispatch-serial-publish-spm-tag-before-kmp.md` / `kasane/decisions/cross/index.md:31`

**問題点**: cross/ADR-0024 は accepted で、次を**決定として**持つ。

- publish の順序表 順 8「README 2 枚と利用者向け Skill のインストール例の version を置き換えた commit を、lint を掛けてから `develop` へ push」
- 「tag と README の扱い」節の後半 (「インストール例の version は release workflow が書く — MAUI の pack の前に作業木で置き換えて nupkg に同梱し、publish 成功後に順 8 で `develop` へ commit する」「cross/ADR-0016 の決定のうち…の 1 文を本決定で置き換える」「publish job が push の前に検証 CI の lint job と同じ検査を掛ける」)
- Consequences の「正: README が指す version は常に公開済みの版で、更新忘れが起きない」「負: `develop` への push が競合したときは README の追従が次回のリリースまで遅れる」

本 change はこれらを一括で撤去した。ところが ADR-0027 は frontmatter に `amends:` を持たず、footer の `関連:` 行に「cross/ADR-0024 (publish の順序とインストール例の置換の時点。本決定で置換そのものを撤去)」と書くにとどめている。0024 側に `amended-by` は無く、`kasane/decisions/cross/index.md` の 0024 行にも「一部改訂」の注記が無い。

ksn-core `references/decisions.md` の footer 規約は「**決定の一部を置き換えるなら amends、置き換えずに補うだけならこの行**」と型を分けており、ここは前者に当たる。同じ運用規律が想定する害がそのまま起きる — `.github/workflows/release.yml:21-22` の冒頭コメントは今も `cross/ADR-0024` を指しており、そこから 0024 を開いた読み手は順 8 と「置換は workflow が行う」を現行として読む。同じ change 内で 0026→0029 は `amends` / `amended-by` / index 注記の 3 点セットを揃えており、0024 に対してだけ扱いが割れている。

**判定**: **amends を起票すべき**。ksn-core「改訂の権利」により、レビュー (実行系) は supersede / amends を自分で起票しないので指摘に留める。必要なのは次の 3 点。

1. ADR-0027 の frontmatter に `amends: [cross/0024]`、Decision 節に「cross/ADR-0024 の決定のうち インストール例の置換 (順序表の順 8 と『tag と README の扱い』の後半) を本決定で置き換える。他の決定 (起動と version・段構成・publish の順序 1〜7・再実行の範囲・tag の扱い) は維持する」の 1 文
2. cross/ADR-0024 の frontmatter に `amended-by: [cross/0027]` (本文は不変)
3. `kasane/decisions/cross/index.md` の 0024 行に「一部改訂: 0027 — インストール例の置換」

### [🟠 Major] handbook `cross/install-examples.md` が、実装に無い保証を書いている

**該当箇所**: `kasane/handbook/cross/install-examples.md:34`

**問題点**: 「**インストール宣言を持たない Skill であっても、対象表に無い Skill が実在する状態は検査が失敗する** — 表への登録漏れをそこで捕まえるためである」と書いているが、実測では失敗しない。宣言を 1 つも持たない Skill を `skills/{en,ja}/` 双方に足した木で `install-example-lint.py` は exit 0 を返す。

原因は `scripts/install-example-lint.py:303-327` の `check_structure`。`actual_skills()` の結果は**英日の対称性の検査にしか使っていない**(en だけ / ja だけの Skill)。対象表 (`registered_documents()`) と突き合わせているのは `declaring_documents()` — すなわち宣言を持つ文書だけで、宣言を持たない Skill はどちらの集合にも入らないため素通りする。

デルタスペックは突合の範囲を「**インストール宣言を持つ文書すべて**」と定めているので、**実装は spec どおり**である。誤っているのは規範層である handbook の記述で、読んだ人が「登録し忘れても CI が捕まえる」と信じる方向の誤りになっている。

**推奨修正**: どちらかを選ぶ。

- (a) 記述を実装に合わせる — 突合の対象は宣言を持つ文書であり、宣言を持たない Skill の登録漏れは検出されない旨に直す (spec を変えずに済む)
- (b) 記述どおりにする — `check_structure` で `actual_skills()` と `registered_documents()` の Skill 名集合も突き合わせる。spec の「対象に無い文書が存在する場合…失敗とする」を厳しくする方向なので spec 違反にはならない

なお cross/ADR-0029 の Consequences「対象表と実構成の突合により、新しい Skill や補助文書が表に登録されないまま増えた状態も検出される」も、宣言を持たない Skill については同じく過大な記述になっている (proposed なので本文の書き直しで足りる)。

### [🟡 Minor] 起点選択の自己テストが「最近接」と「一覧の末尾」を区別できない

**該当箇所**: `scripts/release/build-release-notes.py:500-540` (`selftest_selection`)

**問題点**: fixture は `published = [0.1.0-beta.1, 0.1.0-beta.2]` の 2 件で、古い順に並び、かつ 2 件が first-parent 上で隣接する。このため `select_base` の順位付けを「距離最小」から「一覧の最後を採る」へ退行させても、全ケースが緑のままになる (実測: `if best is None or candidate[0] < best[0]:` → `if True:` に置換して 38 件すべて OK)。GitHub の releases API は**新しい順**に返すため、この向きの退行は本番で「起点が最古の Release になり、前回以降でない項目までノートに載る」という静かな誤りになる。

**推奨修正**: 公開済み Release を 3 件以上 (うち 2 件以上が対象 commit の祖先) にし、一覧の並びを API と同じ新しい順にした fixture を 1 つ足す。起点が「一覧の先頭でも末尾でもない」状態を作れば、順位付けの退行がここで落ちる。

### [🟡 Minor] `PROSE_DECLARATIONS` が実体と突き合わされていない

**該当箇所**: `scripts/install-example-lint.py:137-142` と同 `418-422`

**問題点**: この一覧は成功メッセージの本数表示にしか使われておらず、実際の散文宣言と照合していない。実測で `skills/{en,ja}/ksdialogs-kmp/SKILL.md` の散文宣言を消しても、`「別に散文の宣言 4 行があり…」` と出たまま exit 0 になる (同ファイルはコードブロック側の宣言を保つため構造突合にも掛からない)。design Decision 2 と handbook は「検査できない限界を検査の側に記録する」ことを対策として挙げているが、その記録が腐っても誰も気づかない状態になっている。

**推奨修正**: `scan()` は既にコードブロック外の宣言も拾っているので、`PROSE_DECLARATIONS` の各 (ファイル, 種別) について `in_code == False` の Occurrence が 1 件以上あることを `lint()` に足す (数行)。自己テストにも「散文宣言を失った状態で落ちる」ケースを 1 つ。

### [🔵 Suggestion] `- none` の判定が行頭空白に厳しく、その厳しさが自己テストに現れていない

**該当箇所**: `scripts/release/build-release-notes.py:117-119`

**問題点**: `line = raw.rstrip()` の後に `line == NONE_MARKER` で比較するため、`  - none` のようにインデントすると「認識できない行」で失敗する。fail-closed なので害は小さいが、意図した厳しさなのか取りこぼしなのかが読み取れない (自己テストにこのケースが無く、`NONE_MARKER` の定義にも注記が無い)。

**推奨修正**: 意図であれば `NONE_MARKER` の直上に「行頭の空白も許さない (箇条書きの入れ子に見える記載を黙って `- none` として扱わないため)」の 1 行を足し、自己テストに 1 ケース加える。

## 検出力の実測 (依頼のあった観点)

### `install-example-lint.py`

リポジトリの `README.md` / `README_ja.md` / `skills/` を一時ディレクトリへ写し、1 箇所ずつ壊して `lint()` を掛けた。

| 入れた誤り | 結果 |
|---|---|
| README の SwiftPM を `0.1.0-beta.1` へ逆戻り | 検出 (ファイル:行 と実際の値つき) |
| MAUI Skill の NuGet を具体 version へ | 検出 |
| SwiftPM を `from:` へ | 検出 (理由つき) |
| 最新版の案内を消す | 検出 |
| 対象表に無い Skill を英日に足す | 検出 |
| **対象表に無い `references/*.md` を英日に足す** | **検出** |
| **`references/ios-host.md` から宣言そのものを消す** | **検出**「対象表にあるがインストール宣言を持たない」 |
| 対象表にある Skill を消す | 検出 |
| 英語版だけ宣言を 1 行増やす | 検出 (本数差 + 英日差の 2 件) |
| `ios-host.md` の散文をコードブロックへ移す | 検出 (「持たないはずの種別」) |
| `ios-host.md` の散文を具体 version へ | 検出せず (設計どおりの限界) |
| `ksdialogs-kmp/SKILL.md` の散文を具体 version へ | 検出せず (設計どおりの限界) |
| 宣言を持たない Skill を英日に足す | 検出せず (→ Major 4) |

- **`references/` の 2 ファイルは構造突合で確かに見られている**。`declaring_documents()` が `skills/{en,ja}/` 配下を `os.walk` し、`SKILL.md` に限らず `.md` すべてを走査しているため、`references/` に宣言が増えた場合も対象表から漏れた場合も落ちる。
- **`ios-host.md` の期待本数 0 は「宣言を失っても気づけない穴」になっていない**。`declaring_documents()` だけは `in_code` で絞らずに散文の宣言も数えるので、宣言が消えれば「対象表にあるがインストール宣言を持たない」と「片言語だけが宣言を持つ」の 2 件で落ちる。コメント (`scripts/install-example-lint.py:257-263`) にも理由が書かれている。
- 残る穴は散文 4 行の**値**だけで、これは spec Scenario「散文の記述は検査を通る」と design Decision 2 が明示的に受け入れ、handbook `cross/install-examples.md` と `PROSE_DECLARATIONS` の両方に記録した限界なので指摘としない (記録の鮮度は Minor 2)。

### `build-release-notes.py`

本体に 17 種の退行を 1 つずつ注入して `--selftest` を回した。

| 注入した退行 | 結果 |
|---|---|
| **`paged` を 1 ページ打ち切りに退行** | **検出 (4 件 NG)** — `PagedGitHubApi` が `_get` だけを差し替えて辿る処理は本体を通しており、翻案元で Major になった「本体を写した別実装を検査する」形になっていない |
| 認識できない行を読み飛ばす | 検出 (2 件) |
| 不正な種別を `docs` に落とす | 検出 |
| セクション無しを空扱いにする | 検出 |
| 見出し 2 つを許す | 検出 |
| 空の説明を許す | 検出 |
| 空セクションを許す | 検出 |
| `- none` と項目の共存を許す | 検出 |
| 今回の version の tag も起点候補にする | 検出 |
| draft を除外しない | 検出 |
| 祖先判定を外す (距離不明を最優先) | 検出 |
| base ブランチで絞らない | 検出 |
| 種別見出しを空でも出す | 検出 |
| 比較の位置を出さない | 検出 |
| マージ順に並べない | 検出 |
| 起点を「一覧の最後」にする | 検出せず (→ Minor 1) |
| `commits_in_range` を `--first-parent` 限定にする | 検出せず (fixture の履歴が線形。本番挙動は変わるが、集約 pull request の merge commit は first-parent 上にあるため実害は小さい) |
| pull request の重複除去の `continue` を外す | 検出せず (`found` が dict なので等価な変異。重複除去は dict 自体が担保) |

## 確認して問題が無かった点

- **`.github/release.yml` の削除と組み立て処理の導入が同時**: 同一の未コミット作業木に `.github/release.yml` の削除 (D) と `scripts/release/build-release-notes.py` の新設 (??) が同居しており、`--generate-notes` / `--prerelease` フラグの参照も `release.yml` から消えている (grep 0 件)。「分類設定だけ先に消えてノートが空になる」経路は生じない。
- **validate の `permissions` が完全形**: `.github/workflows/release.yml:96-98` が `contents: read` + `pull-requests: read` を書き切っている。トップレベルは `contents: read` のみなので、job レベルで列挙を欠くと checkout と Release 取得が壊れる Decision 7 の懸念に対応できている。`fetch-depth: 0` も維持されており、`select_base` の tag 解決と `rev-list` が成立する。
- **リハーサルの分岐が 1 箇所で評価されている**: `Decide release notes scope` (id: `notes-scope`) だけが条件式を持ち、`Build release notes` / `Upload release notes` / `Skip release notes` の 3 step はその出力 (`collect`) だけを見ている。
- **`Upload release notes` に `overwrite: true` と `if-no-files-found: error`** がある。
- **`Download release notes` に `if:` を置かない判断は妥当**: publish job 自体が `if: ${{ !inputs['dry-run'] }}` で守られており、非 dry-run では `collect` が常に `true` になるため成果物は必ず存在する。一方 `Create GitHub Release` は `publish-needed` で分岐しない (skip-all の再実行でも走る) ので、download を `publish-needed` で絞ると本文が無いまま Release 作成に達する。両 step が揃って無条件であることが整合している。step の位置も `Prepare deployment id files` より後で、`check-publish-step-order.py` が緑 (実測)。
- **Release 作成の冪等性**: `gh release view` で既存を検出したら本文に触れずに `exit 0`、確定ノートが空なら `::error::` で失敗 (spec Scenario「確定したノートが無ければ Release を作らない」)。
- **`.github/pull_request_template.md` の未編集状態が解析を通る**: `## Changes` 直下に `- none` だけを置き、種別一覧と記入例は次の見出し以降。`extract_section` が次の見出しで切るため範囲に入らない。自己テストがリポジトリの実ファイルを読んで検査している。
- **`CLAUDE.md` は `AGENTS.md` への symlink** なので、tasks 4.5 の「`CLAUDE.md` がプロジェクト側にも同文を持つなら同時に直す」は自動的に満たされている。
- **lint job の 12 検査の数え方**は `kasane/handbook/cross/verification-ci.md` の表 12 行と一致し、0020 (6) → 0021 (7) → 0022 (8) → 0026 (11) → 0029 (12) の系譜とも整合している (step の分割だけが Major 2)。
- `deviation.md` の付随修正 4 件はいずれも ksn-core の同梱条件 (参照が切れた記述の追随) に収まっており、内容も実ファイルと一致している。`kasane/handbook/index.md` の件数 15 件・列挙も実態どおり。
- `scripts/` 2 本ともコメント規約 lint と構造 lint を通り、コメントが外部 ID だけに依存せず単独で読める形になっている。

## アクションプラン

1. **`0.1.0-beta.1` の prerelease 印を解除**し、`/releases/latest` が解決することを確認して tasks 1.1 に印を付ける (Major 1)。これが済むまで `main` へ入れない
2. **`.github/workflows/ci.yml` の install-example-lint を 1 step にまとめ、`--selftest` を先にする** (Major 2)
3. **cross/ADR-0024 への amends を起票する** — ADR-0027 に `amends: [cross/0024]` と置き換え範囲の 1 文、0024 に `amended-by: [cross/0027]`、index の 0024 行に注記 (Major 3)。起票は改訂の権利を持つフロー (explore / propose / distill) が行う
4. **`kasane/handbook/cross/install-examples.md:34` の保証を実装に合わせるか、実装を記述に合わせる** (Major 4)。あわせて cross/ADR-0029 の Consequences も見直す
5. 起点選択の自己テストに、公開済み Release 3 件以上・新しい順の fixture を 1 つ足す (Minor 1)
6. `PROSE_DECLARATIONS` を実体と突き合わせる検査を足す (Minor 2)
7. `- none` の行頭空白の扱いを注記 + 自己テスト 1 ケース (Suggestion)
8. 残る検証 tasks 10.5 (`main` からの `dry-run`) と 10.6 (消費者での `{version}` 差し替え) はオーナー実行が要る。10.1〜10.4 は本レビューで手元実行し、いずれも緑であることを確認した
