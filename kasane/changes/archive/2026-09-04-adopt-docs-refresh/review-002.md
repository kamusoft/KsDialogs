# レビュー結果: adopt-docs-refresh (002 回目)

**日付**: 2026-09-04
**判定**: CHANGES_REQUESTED

## サマリー

前回サイクルで確定・採用された 3 件 (6-⑧ の `KsDialogAttributes` 誤検出 / 6-⑤ ① の相対パス形の取りこぼし / 6-⑤ ③ の参照形式リンク未解析) と Suggestion 2 件 (python 断片の堅牢化・`/tmp` 残骸) は、すべて fixture 実走で解消を確認した。翻案元の契約 (manifest 検証 4 ケース・承認前無変更・器 `ksn-implementer` 固定・最大 3 並列・`--all` / `--readme-only`・manifest を最後に書く・旧ハッシュ保持・自動発動禁止) は今回の編集でも欠落しておらず、`scripts/` 8 本は依然 byte 一致、標準 lint 3 本は exit 0。

ただし 6-⑤ ① の修正が、検出用の文字クラスを**否定クラスから正の許可リストへ置き換えた**ため、全角区切り (「」・（）・`**` 強調など) の直後に現れる `kasane/` を取りこぼすようになった。修正前のパターンはこれらを検出できていたので、指摘 (相対パス形の取りこぼし) を直す過程で別方向の穴を開けた**回帰**である。生成物の半分は `skills/ja/` であり、日本語文書で内部パスを「」や太字で囲むのは普通の書き方なので、Major として差し戻す。

## 照合した規約

- `kasane/handbook/cross/comment-policy.md` (always) — コメント構文を持つファイルは翻案元から無改変コピーした `.py` 8 本のみ。今回の編集 (`SKILL.md` のみ) は対象外。`config.yaml` の `lint.comment-policy.exclude` 変更後も `comment-policy-lint.py` は exit 0
- `kasane/handbook/cross/test-execution.md` (きっかけ: 変更の完了判定) — 全ビルドルート全件実行は proposal の Impact どおり合意済み例外。diff が製品コード・テストに触れないことを再確認して適用外とした
- 適用外と判定: `sample-parity.md` (`samples/` 無変更)・`runtime-behavior-verification.md` (実行時挙動に触れない)・`local-development-setup.md` (ビルド・環境構築を行わない)・`aiforms-origin-reference.md` (未移植機能の実装・調査でない)
- `kasane/lessons/code-review.md` は存在しない。`impl.md` L-001 (証跡実体の突き合わせ) / `process.md` L-001 (姉妹面の照合) を読み、本 change には該当なし (証跡画像なし・姉妹面実装なし) と判断した
- `deviation.md` の tasks 2.2 の差分は合意済み差分として扱い、指摘に含めない

## 前回指摘の解消状況

| 前回の指摘 | 出典 | 今回の確認 (fixture 実走) | 状態 |
|---|---|---|---|
| 6-⑧ `KsDialog([^s]\|$)` が `KsDialogAttributes` を誤検出 | review-001 Major (確定) | 現行 `KsDialog([^sA-Za-z0-9_]\|$)` で `KsDialogAttributes` / `jp.kamusoft.ksdialogs.compose.KsDialogAttributes` を含む正例ファイルは 0 件報告。誤例 9 種 (`Ksdialogs` `ks-dialogs` `com.kamusoft` `jp.kamusoft.KsDialogs` `KsDialogsMaui` `KsDialog ` 単数形 `KsDialogs.MAUI` `Ks_Dialogs` `KSDialogs`/`ksDialogs`/`KSdialogs`) はすべて検出 | ✅ 解消 |
| 6-⑤ ① が `./kasane/` `../kasane/` を取りこぼす | review-001 Minor / second-opinion Major (確定・Major) | `](../../../kasane/…)` `./kasane/index.md` `[source]: ../../../../kasane/…` を検出。`skills/README.md` (③ の対象外) でも検出。URL 中の `…/kasane/…` と `mykasane/` は素通り | ⚠️ 半解消 (下記 Major: 全角区切りで新たな取りこぼし) |
| 6-⑤ ③ が参照形式リンク `[label]: path` を解析しない | second-opinion Major (採用) | `[source]: ../../../../kasane/…` と `[o]: ../ksdialogs-android/SKILL.md` の 2 件をルート外リンクとして検出。同一 Skill 内の `[ref]: ../SKILL.md` は報告なし。相方が示した回避経路 (`[内部仕様][source]` + 参照定義) は塞がった | ✅ 解消 |
| python 断片の空リンク例外 (`raw.split()[0]`) | review-001 Suggestion | `href = (tokens[0] if tokens else "")` へ修正済み。`[]( )` を含む fixture で例外なく完走 (`SKILL.md:430`) | ✅ 解消 |
| 空チェックの挙動が隣のブロックと揃わない | review-001 Suggestion | `print(...)` + `raise SystemExit(0)` へ統一 (`SKILL.md:408-410`)。6-⑤ bash / 6-⑦ の空ガードと同じ標準出力 + 正常終了 | ✅ 解消 |
| `/tmp/docs-refresh-targets.txt` の残骸 | review-001 Suggestion | ファイルは存在しない | ✅ 解消 |
| tasks 2.2 の文言と実装の食い違い | review-001 Suggestion | `deviation.md` に合意済み差分として記録済み。指摘しない | ✅ 記録済み |

降格済み (外部 URL の意味レビュー工程・ルート README の identity-lint 範囲) は本レビューでも再掲しない。

## 指摘事項

### [🟠 Major] 6-⑤ ① の文字クラスを許可リスト化した副作用で、全角区切り・強調記号の直後の `kasane/` を検出できなくなった (回帰)

**該当箇所**: `.agents/skills/docs-refresh/SKILL.md:395` (パターン本体)、同 `:448` (① の注記)

**問題点**:

修正後のパターンは

```
(^|[[:space:]([<\"'`,|:=])(\.{1,2}/)*kasane/
```

で、`kasane/` の直前に来てよい文字を**許可リストで列挙**している (行頭・空白・`(` `[` `<` `"` `'` `` ` `` `,` `|` `:` `=`)。修正前は `(^|[^A-Za-z0-9_./-])kasane/` という否定クラスで、「英数字・`_`・`.`・`/`・`-` 以外の任意の文字」が直前に来ればよかった。この置き換えで、許可リストに載っていない文字 — とりわけ**マルチバイトの全角区切りと markdown の強調記号** — の直後が検出対象から落ちた。

fixture で実走した結果 (7 行中 6 行が現行パターンでは 0 件):

```
a **kasane/concepts/index.md** を参照      ← 現行: 未検出 / 修正前: 検出
b 「kasane/concepts/index.md」              ← 現行: 未検出 / 修正前: 検出
c （kasane/concepts/index.md）              ← 現行: 未検出 / 修正前: 検出
d *kasane/concepts/index.md*                ← 現行: 未検出 / 修正前: 検出
e “kasane/concepts/index.md”                ← 現行: 未検出 / 修正前: 検出
g 【kasane/concepts/index.md】              ← 現行: 未検出 / 修正前: 検出
```

実害の経路:

- 生成物の半分は `skills/ja/` (en/ja ペア同時生成が契約) で、日本語文書がパスを「」や（）で囲むのは自然な書き方。`「kasane/concepts/index.md」を参照` は ① をすり抜ける
- ③ (Skill ルート外の相対リンク検査) は救済にならない — ③ が見るのは markdown リンクの解決先で、地の文の言及は対象外。さらに ③ は `skills/` 配下の 4 階層以上にしか適用されないため、`skills/README_ja.md` には ① 以外の機械的な防波堤が存在しない
- デルタスペック Requirement「閉世界性と機械面の漏れ検査」の ① は「リポジトリ内部用語 (`kasane/` 配下への**参照**・ADR 番号の参照)」を無条件の SHALL として要求しており、リンク形に限定していない

**推奨修正**:

許可リストではなく「除きたい文字だけを除く」否定クラスへ戻したうえで、今回の目的 (URL 中の `…/kasane/…` を拾わない) を `/` の除外で果たす:

```
(^|[^A-Za-z0-9_/-])(\.{1,2}/)*kasane/
```

fixture で実走して確認済み — 上記 a〜g のうち `_kasane/` (下線強調。`_` は識別子構成文字なので `my_kasane/` の誤検出を避けるため意図的に除く) を除く全行を検出し、`https://example.com/kasane/spec.md` と `mykasane/foo` は引き続き素通りする。前サイクルで直した相対パス形 (`](../../kasane/…)` `./kasane/…` `[s]: ../../kasane/…`)・README での検出も維持される。

あわせて `:448` の ① の注記を、許可リストの列挙 (「行頭・空白・括弧・引用符・区切り記号 (`,` `|` `:` `=`) の直後」) から「直前が `/` (URL の途中) と英数字・`_`・`-` (単語内一致) のときだけ当たらない」という**除外の説明**へ書き直す (許可リストのままだと、次に触る人が同じ穴を再現する)。

## 指摘事項 (低優先)

### [🔵 Suggestion] `/tmp/docs-refresh-*` の固定パスが姉妹リポジトリと衝突する

**該当箇所**: `.agents/skills/docs-refresh/SKILL.md:320` / `:333` / `:391` / `:407` / `:465` (`/tmp/docs-refresh-manifest-planned.json` と `/tmp/docs-refresh-targets.txt` の生成・参照箇所。他に `:319` `:327` `:332` `:343` `:354` `:365` `:376` `:458` `:468-469` `:495` `:519`)

**問題点**:
翻案元と同じ固定パスをそのまま使っているため、同一マシンで KsSettingsView と KsDialogs の docs-refresh を回すと同じファイルを共有する。現に `/tmp/docs-refresh-manifest-planned.json` には KsSettingsView 側の予定 manifest が残っている (`targets` の先頭が `kssettingsview-aiforms-migration/SKILL.md`、更新時刻は本 change の作業開始前)。この状態で KsDialogs 側の 6-② / 6-③ / 6-④ を `DOCS_REFRESH_MANIFEST=/tmp/docs-refresh-manifest-planned.json` で叩くと、**別プロジェクトの manifest に対して検査が走って結果だけが返る**。Guardrails は環境変数がブロックを跨いで失われる事故を警戒しているが、リポジトリ間の衝突は想定に入っていない。

**推奨修正**:
本 change のスコープ (翻案元の無改変踏襲) を崩さない範囲では、Step 6 の冒頭に「予定 manifest / 対象一覧はこの実行で生成し直したものであることを確認する (別リポジトリの docs-refresh と同じ `/tmp` パスを共有する)」の一文を足すだけでも足りる。パス自体をプロジェクト別 (`/tmp/docs-refresh-ksdialogs-*` 等) に分けるのは翻案元との差分になるため、phase-2 または蒸留時の判断に回してよい。

## 確認したがこの版では問題なしと判断した観点

- 翻案元契約の欠落: 今回の編集領域 (6-⑤ / 6-⑧ と各注記) は KsDialogs 固有の置き換え部分のみで、Requirement が列挙する翻案元契約 11 項目は本文照読で全件残存を確認 (frontmatter の自動発動禁止・Guardrails の 17 項目を含む)
- 6-⑧ の別方向の回帰: `KsDialog([^sA-Za-z0-9_]|$)` は**否定**クラスなので日本語の直後 (`KsDialogを表示`) にも当たる。①のようなマルチバイト取りこぼしはない。検出力が落ちるのは `KsDialogView` のような「単数形 + 英字継続」だが、これは `KsDialogAttributes` と機械的に区別できず、`:507` の注記が意図として明記している
- ③ の python 断片: `ref_re` が拾うのは行頭 3 スペース以内の `[label]:` のみで、リンク記法として妥当。`<…>` 剥がし・フラグメント除去・`://` と `#` / `mailto:` の除外・`is_relative_to` の判定はいずれも fixture で期待どおりに動いた
- `:490` の追記 (`KsDialogAttributes` は正しい API 名) の事実性: `android/ksdialogs-compose/src/main/kotlin/jp/kamusoft/ksdialogs/compose/KsDialogAttributes.kt:30` に `public fun KsDialogAttributes(` が実在し、`kasane/concepts/core/api/` の layout / registration-show / transition の 3 本に記載があることを確認
- 3d の取得元 4 行: 全項目が実読で非空 (`agp=9.3.0` / `kotlin=2.4.10` / `minSdk=24` / `compileSdk=36` / Gradle `9.7.0` ×2 / `swift-tools-version: 6.3` / `.iOS(.v17)` / TFM `net10.0;net10.0-ios;net10.0-android` / OS 下限 `17.0`・`24.0` / `Microsoft.Maui.Controls 10.0.1`)。`kmp/settings.gradle.kts:32` の catalog 共有前提も成立
- 規約記述: `AGENTS.md:11-12` の 2 行に実行手順・フラグは含まれず、`CLAUDE.md` は `AGENTS.md` への symlink。`config.yaml` の `lint.identity.scope: [kasane, skills]` / `lint.comment-policy.exclude: [skills]` / `context` の 1 文をいずれも確認
- 足場: `proposal.md` / `specs/docs-refresh/spec.md` に変更なし (逆流なし)。fixture は scratchpad のみでリポジトリ内に残骸なし

## アクションプラン

1. **Major**: `SKILL.md:395` のパターンを `(^|[^A-Za-z0-9_/-])(\.{1,2}/)*kasane/` へ戻し (許可リスト → 否定クラス)、`:448` の注記を除外の説明へ書き直す。修正後、①「」・（）・`**` 強調で囲んだ `kasane/` が検出され、URL 中の `…/kasane/…` と `mykasane/` が引き続き素通りし、②前サイクルで直した相対パス形と README での検出が維持されることを fixture で再実走する
2. **Suggestion**: Step 6 冒頭に `/tmp` 共有の注意書きを足す (パス分離自体は phase-2 / 蒸留へ回してよい)
