# レビュー結果: split-concepts-platform-surface (004 回目)

**日付**: 2026-09-06
**判定**: APPROVED

## サマリー

最終レビュー (tasks グループ 5.2〜7)。Skill 再生成物・manifest・禁止トークン fixture の是正・除外リストの書き直し・ADR-0014 の改訂を、成果物とスクリプトを自分の手で再実行して検証した。docs-refresh の整合性チェック 5 種・manifest の全不変条件 (キー集合 / SHA-256 / targets 33 キー / 網羅不変条件)・禁止トークンの負の検査 (5 範囲 0 件)・着地台帳 (未説明 0 件)・lint 一式はすべて再現し、除外リスト 22 行と API 名網羅検査の報告 38 名前は**過不足なく 1:1 で対応**していた (未分類 0 件・陳腐化した行 0 件)。再生成された Skill 本文は実装と照合した範囲すべてで正しく、他 platform 名の混入も独立の突き合わせで検出できなかった。

指摘は Critical / Major なし。Minor 2 件はいずれも検証記録・ADR 本文の一貫性に関するもので、検査結果と利用者向け成果物には影響しない。

## 照合した規約

- `kasane/handbook/cross/comment-policy.md` (always) — 新規スクリプト `verification/skill-forbidden-tokens.py` のコメント。lint 対象拡張子外だが規約本文で確認 (参照先は同一 change 内で archive されるため単独で辿れる)
- `kasane/handbook/cross/user-skill-api-listing.md` (適用のきっかけ: `skills/**` を生成・更新するとき / docs-refresh の API 名網羅検査を仕分けるとき) — 本 change が書き直した当の規約。方針節「簡潔でも網羅」・除外基準・してはいけないことの 3 節を、書き直し後の版で節ごとに照合
- `kasane/concepts/rules.md`「契約と公開面の振り分け」 — ADR-0014 の改訂が参照先として指す規則の存在と内容を確認
- `kasane/decisions/cross/0014-...` (proposed) — 決定ではないため、この ADR との齟齬は所見にとどめる方針で読んだ
- `kasane/lessons/code-review.md` — 不在 (`impl.md` / `process.md` / `spec-review.md` のみ)。重点観点・指摘しないことの持ち込みなし

## 実行した検証 (すべて自分で再実行)

| 検査 | 結果 |
|---|---|
| docs-refresh Step 6: concepts coverage / heading parity / code-block parity / frontmatter / link resolution | 5 種すべて OK |
| `planned-manifest.py` と `skills/.manifest.json` の全キー比較 | 完全一致 (concepts / targets / excluded / readmes / lastUpdatedFiles / version) |
| manifest 不変条件 (concept 集合 30・SHA-256 再計算・targets 33 キー ↔ 言語抜き 33 ファイル・網羅不変条件・excluded との排他) | 違反 0 |
| `targets` 33 キー ↔ design.md Decision 6 の表 | 全キー一致。ios / android / maui に他 platform の `api/` なし、kmp は `android-host.md` / `layout.md` / `transitions.md` に限り `android/api/` `ios/api/` を含む |
| `verification/skill-forbidden-tokens.py` | exit 0、検査 [1] [2] とも 5 範囲 0 件 (`verification/api-coverage-after.txt` の記録と同一出力) |
| 是正前 fixture (HEAD) での再現 | 44 件 (ios 22 / android 10 / maui 8 / aiforms 4) を完全に再現。全件が自 platform の実在名・見本名・文字列リテラル語の偽陽性であることを 1 件ずつ確認 |
| concept 側の負の検査の再現 | ios 192 / android 171 / maui 142 / kmp 142 / aiforms 144 に対し一致 0 件 (`forbidden-tokens-concepts.txt` 末尾の追記と一致) |
| `identifier-landing.py check` | 台帳 108 行 = 着地 89 / 意図して落とした 19 / 未説明 0 |
| 除外リスト 22 行 ↔ api-coverage-check.py の報告 23 行 (38 名前) | 未分類 0 件・報告されないのに残っている行 0 件 (5 Skill 範囲すべて) |
| 独立の他 platform 名混入検査 (自源泉に無く他 platform concept にある識別子を Skill 全文から探す) | 実 API の混入 0 件 (検出は `return` `var` `this` `Button` 等の言語キーワードと、実装に実在する自 platform 名のみ) |
| en/ja のバッククォート識別子の多重集合比較 (62 ファイル) | 差異 4 ファイル・4 語のみ (`suspend` / `0` / `AddKsDialogs` の裸形) で、いずれも散文表現の差。内容の同等性は保たれている |
| local-path lint / identity lint / doc-structure lint (書き直した handbook) / `git diff --check` | すべて exit 0。未追跡 2 ファイルは手作業で走査し、ローカル絶対パス・個体特定値 0 件 |
| Skill 本文と実装の照合 (抜粋) | `DialogOptions` / `DialogPlacement` / `DialogEdgeInsets` の既定値・綴り (iOS / Android)、MAUI の添付プロパティ 10 種と型 (`Thickness` / `Color?`)、`LoadingStyle` 5 項目と既定値 (MAUI / Android)、`ToastStyle` の既定値と `BUILTIN_*`、`DialogTransition` の preset 署名と既定 (250ms / `Easing.CubicInOut`)、`IKsLoading` の操作一覧と引数順、`KsDialogsKmp.register` の SwiftUI overload、MAUI の `DialogException` 派生型 — 照合した範囲すべて実装どおり |

## 指摘事項

### [🟡 Minor] 禁止集合の是正で確定した規則 (a) が kmp 集合だけに適用されていない

**該当箇所**: `verification/baseline.md`「追記: ios / android / maui / aiforms-migration 禁止集合の是正 (task 6.1)」の「外さなかったもの」節 (`kmp 集合は触っていない (142 件のまま)`) / `verification/forbidden-tokens.json` の `kmp.tokens`

**問題点**: 追記が定めた規則 (a) は「自 platform の源泉 concept (manifest `targets` の当該 Skill の和集合) の**全文** (span + コード例) に現れる名前は外す」である。この定義で 5 集合を機械的に検査すると、規則に反して残っているトークンは kmp の `A.min` 1 件だけだった (ほかの 4 集合は 0 件)。`A.min` は kmp の源泉である `kasane/concepts/core/api/layout-semantics.md` の rect 決定手順の fenced code block に現れるため、maui / aiforms-migration で同じ `A.min` を規則 (a) で外した判断と非対称になっている。追記自身も「そのうち実際に源泉 concept に現れる `A.min` だけを (a) で外した」と書いており、kmp を対象外にした理由が記録に無い (修正サイクル 2 で kmp の許可面を `kmp/api/` + `android/api/` + `ios/api/` の 2 本と定義し core/api を含めなかった旧定義が残っているためと読めるが、明記されていない)。

現時点の検査結果には影響しない (KMP Skill にこの数式は写っておらず一致 0 件)。実害は、KMP Skill が将来 layout 契約の数式を写した場合に「他 platform 名の混入」として偽陽性が出ること、および fixture の判断規則が記録上一貫して読めないことに限られる。

**推奨修正**: `verification/baseline.md` の当該節に、kmp の許可面が修正サイクル 2 の定義 (core/api を含めない) のままであること、および `A.min` / `C05` / `C19` / `approvedBy` / `approvedDiff` の 5 件を「architecture への移動漏れの回帰検出」として意図的に残していることを 1 行で書き足す (規則 (a) を kmp にも適用して `A.min` を外す選択でもよいが、その場合は移動漏れの検出力が 4 件に減ることを併記する)。fixture の再実行は不要 (どちらでも一致 0 件)。

### [🟡 Minor] ADR-0014 の共通概念名の候補列挙に、確定一覧で条件を満たさなかった名前が残っている

**該当箇所**: `kasane/decisions/cross/0014-concepts-core-contract-platform-surface.md` の Decision 第 2 項 (`DialogPlacement` / `DialogTransition` / `DialogViewModel` / `DialogException` / `KsDialogs` / `KsLoading` / `KsToast` / `LayoutArea` などが候補)

**問題点**: `verification/core-contract-check.md`「確定した共通概念名」(13 行) では `DialogException` は 4 形態の条件を満たさず (iOS は `DialogError`)、`LayoutArea` は「どの形態にも存在しない綴り (実装は `DialogLayoutArea`)」として deviation.md に契約と実装の乖離として記録され、core からは散文に落とされている。ADR は proposed であり列挙は「候補」の語つきだが、task 7.1 は「design Decision 1〜3 の内容が反映されていること」の確認であり、蒸留で accepted へ昇格すれば規範として読まれる。実装の結果すでに否定された 2 例が候補として残ると、次に core へ識別子を足す人が同じ判定を繰り返す。

**推奨修正**: 候補列挙を確定一覧の名前 (`DialogPlacement` / `DialogTransition` / `DialogViewModel` / `DialogResult` / `KsDialogs` / `KsLoading` / `KsToast` など) に差し替えるか、列挙の後に「確定一覧は実装コードで確認して定める (本 change の `verification/core-contract-check.md` が初版)」の 1 文を足す。ADR は proposed のため本文の直接改訂でよい。

### [🔵 Suggestion] 除外リストの新基準「非 API token」の定義と、そこに収容した名前がずれている

**該当箇所**: `kasane/handbook/cross/user-skill-api-listing.md`「意図的な掲載除外の基準」表の `非 API token` 行と、現行除外リストの MAUI `SetIocConfig`、`ShowResultAsync`、`UseCurrentPageLocation` の行 / AiForms migration `CancellationToken`、`Easing.CubicInOut` の行

**問題点**: 基準の定義文は「そもそも API 名ではないのに識別子として拾われた語」だが、同じ行の説明が「移植元ライブラリの旧 API 名」を含み、実際に収容している 3 件は移植元 AiForms.Maui.Dialogs の実 API 名、`CancellationToken` / `Easing.CubicInOut` は .NET / MAUI の実型・実定数である。さらに AiForms migration では `Easing`・`TimeSpan`・`TimeSpan.MaxValue` を「機械的に導出できる名前」に、`Easing.CubicInOut` を「非 API token」に置いており、同じ型とそのメンバーが別基準に分かれている。

spec `specs/api-listing-policy/spec.md` の Scenario (3e の仕分け / 他 platform 名の行の消滅) は「各名前に基準が付いていること」と「『対象 Skill 外・機械検査由来』の行が 0 件」を求めており、どちらも満たしているため判定には影響しない。

**推奨修正**: 基準名を「対象 Skill の公開面ではない名前」のように収容内容に合わせるか、定義文を「対象 Skill の公開 API ではない語 (framework の標準型・標準定数、見本型名、移植元の旧 API 名、build 配線の DSL 名・ファイル名)」に改める。あわせて `Easing.CubicInOut` を `Easing` と同じ行へ寄せると、同一型の扱いが揃う。

### [🔵 Suggestion] AiForms migration の `Center` / `Fill` / `Top` を「機械的に導出できる名前」で除外している点 (所見)

**該当箇所**: `kasane/handbook/cross/user-skill-api-listing.md` の `| AiForms migration | Center、Fill、Top | 機械的に導出できる名前 |` の行

**問題点**: この 3 件は `DialogAlignment` / `DialogTransitionEdge` の実在する列挙子 (公開 API) で、理由欄は「対応表は型名のほうを掲載しており列挙子は型から導ける」としている。一方で同 Skill の `references/api-mapping.md` は移行例に `DialogTransitionEdge.Bottom` を実際に載せており、同じ enum の列挙子で掲載と除外が分かれている。掲載可否はオーナー判断の領分 (規約の「してはいけないこと」でも独断の除外を禁じている) なので指摘ではなく所見として残す。

**推奨対応**: 現状のままでも spec を満たす。次に除外リストを触るときに、この行を「低頻度の細部 API」へ移すか、`Top` を対応表へ掲載して行から外すかを検討するとよい。

## アクションプラン

1. (任意・低優先) `verification/baseline.md` に kmp 集合を規則 (a) の対象外にした理由の 1 行を追記する — Minor 1。fixture・検査の再実行は不要
2. (任意・低優先) cross/ADR-0014 の候補列挙を確定一覧に合わせて改訂する — Minor 2。proposed のため本文を直接改訂してよく、蒸留での accepted 昇格前に済ませると誤読が残らない
3. (任意) 除外リストの基準名・`Easing.CubicInOut` の行寄せ — Suggestion 1。handbook への書き込みは ksn-concept の経路 (timestamp 更新を伴う) なので、次に同ファイルを触るときにまとめるのでよい
4. (記録のみ) `verification/skill-forbidden-tokens.py` と `verification/api-coverage-after.txt` は現在未追跡。標準 lint は追跡ファイルしか走査しないため手作業で確認済み (違反 0) だが、commit 時に検証成果物として取り込まれることを確認する
