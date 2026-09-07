# レビュー結果: split-concepts-platform-surface (002 回目)

**日付**: 2026-09-05
**判定**: APPROVED
**範囲**: 再レビュー — review-001 / second-opinion-code-001 の指摘に対する修正サイクル 1 の確認。tasks.md グループ 5〜7 (manifest 書き換え・Skill 再生成・除外リストの組み直し・仕上げ) は未着手のままなので、未実装の指摘はしていない

## サマリー

前回の指摘 **10 件すべてが解消**していた。実装コードとの食い違い 2 系統 (1 引数 factory のインライン show への拡張、Android の Compose 呼び分け) は宣言を読み直して裏を取り、いずれも実装どおりの記述に直っている。構造 lint の 5 件も表・段落への移し替えで消えており、内容の脱落は baseline (218d5c0) との突き合わせで無いことを確認した。KMP 禁止集合の再導出は自前で独立に再現し、加えた 19 件・意図して外した 3 件ともに導出どおりだった。

新規の問題は 2 件で、いずれも Minor。KMP 禁止集合に**許可面の中にある 4 トークンが残っている** (baseline.md が今回掲げた「許可面は targets の和集合ちょうど」という不変条件に反する。task 6.1 で偽陽性になる) ことと、`concepts/log.md` の構造 lint の件数の記述が事実と食い違うことである。どちらも残りのタスク (6.1〜6.3) の中で自然に直せる位置にあり、Major には当たらない。

## 照合した規約

domains 定義プロジェクトのため cross + 作業ドメイン (cross) の handbook / concepts index を対象にした。

| 規約・文書 | 適用のきっかけ |
|---|---|
| ksn-core `references/concepts.md` | concepts の階層・価値 lint・アンカー規約・用語規約・粒度と分割 |
| ksn-core `references/doc-structure.md` | 育つ文書 (concepts) の書き直し。構造 lint の適用対象と閾値 |
| ksn-core `references/delta-spec.md` | 足場凍結・deviation = 合意済み差分 |
| ksn-core `references/paths.md` | 成果物に書くパスの形式 |
| `kasane/concepts/rules.md` | 配置判断・共通概念名の条件 (本 change が追記し、修正サイクルで改訂した節) |
| `kasane/handbook/index.md` / `handbook/cross/index.md` | always 該当なし。cross 7 件のうち `user-skill-api-listing.md` (禁止トークン fixture の初期値の由来) のみ本 change に当たる |
| `kasane/decisions/cross/0014` (proposed) | 分割の原則。proposed のため判定の根拠にはしていない |

`kasane/lessons/code-review.md` は不在 (lessons/ にあるのは `impl.md` / `process.md` / `spec-review.md` / `inbox/` / `details/`)。config.yaml の `skills.code-review` は空、cross に `domain-skills` の割り当てなし。

## 前回指摘の解消状況

`SO` = second-opinion-code-001、`R1` = review-001。末尾「突き合わせ結果」のとおり全件採用されている。

| # | 指摘 | 状態 | 確認したこと |
|---|---|---|---|
| SO Major 1 | 1 引数 factory がインライン show でも使えるという新規の主張 (`core/api/model-binding-semantics.md`) | **解消** | 当該段落 (`:83`) が「登録のオーバーロードであり…登録済みの factory を解決して表示する経路 (インスタンス渡し show・型指定 show) であれば同じに働く」に限定され、インライン show への言及が消えた。1 引数 register のオーバーロードが 3 形態に実在することも確認 (`ios/Sources/KsDialogs/Registry/DialogViewRegistry.swift:54`・`:80`、`maui/KsDialogs.Maui/Registry/DialogViewRegistry.cs:81`・`:95`)。用語表 `:124` と保証 `:100` にも矛盾なし |
| SO Major 2 | Android の Compose 登録経路とインライン表示経路の混同 (`android/api/{dialog,loading,toast}-surface.md`) | **解消** | 3 本とも表が「登録 / 登録済みの表示 / インライン表示 (中身を直接渡す)」の 3 列に分かれ、`registerCompose` + `show` と `showCompose` / `startCompose` が別経路として書き分けられた。署名は実装宣言と一致 (`android/ksdialogs-compose/src/main/kotlin/jp/kamusoft/ksdialogs/compose/ComposeDialogShow.kt:23`・`ComposeLoadingShow.kt:20`/`:41`・`ComposeToastShow.kt:21`)。Toast のインライン列が `durationMs` を含む点も実装どおり |
| SO Major 3 | `rules.md` の `core/api/` カテゴリ定義「契約・公開 API・利用例」が振り分け規則と矛盾 | **解消** | `kasane/concepts/rules.md:31` が「全 platform が共有する観察可能な挙動と保証 (ダイアログ契約)。公開名・署名・コード例は持たない」になり、同ファイル `:62-64` の表・`core/index.md:5` と整合した |
| SO Major 4 | KMP 禁止集合が `ios/api/*.md` 全体を許可面に含めている | **解消** (残課題は下記 Minor 1) | `verification/baseline.md` の表が kmp の許可面を「`kmp/api/*` + `android/api/*` + `ios/api/layout-surface.md` + `ios/api/transition-surface.md`」に改められ、iOS Native 固有 19 件が追加された。自前で再導出して一致を確認 (下記「再実行した機械検査」) |
| R1 Major 1 / SO Minor 1 | ios / android の layout-surface で型種別と既定値の包括表現が実装と食い違う | **解消** | `ios/api/layout-surface.md:25` が struct 3 / enum 2 を書き分け、既定値の主語を `DialogOptions` / `DialogPlacement` に限定し、`DialogEdgeInsets` は 4 辺明示か `init(all:)` / `.zero` と案内する形になった。android も `data class` 3 / `enum class` 2 で同型に直っている。実装と一致することを確認 (`ios/Sources/KsDialogs/Contract/DialogEdgeInsets.swift:12`・`DialogLayoutArea.swift:4`・`DialogAlignment.swift:7`・`DialogOptions.swift:35`・`DialogPlacement.swift:22`、`android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/DialogEdgeInsets.kt:11-18`・`DialogOptions.kt:26-33`・`DialogPlacement.kt:18-23`)。同型の包括表現が他に無いことを `いずれも` / `すべて` / `全て` の grep で再確認し、残る 13 件はすべて実装どおり (Android の Loading 全 suspend / Toast 全非 suspend も `KsLoading.kt:41-127` / `KsToast.kt:39-69` で裏を取った) |
| R1 Minor 1 | `rules.md` に「KMP の公開面 = 3 側」の定義が無い | **解消** | `kasane/concepts/rules.md:66` に「commonMain の公開宣言に加えて、Android ホスト側 (`androidMain` の typealias が指す Android Native の型) と Swift 向け公開面 (`kmp/api/ios-host-integration.md` が扱う面) を合わせた 3 側」と明記された。`verification/core-contract-check.md` 冒頭の定義と一致しており、長命層だけを読む後続の書き手が `DialogNotifier` / `DialogTransition` の残置理由を再導出できる |
| R1 Minor 2 / SO Minor 2 | `core/index.md` の `LayoutArea` バッククォート | **解消** | `kasane/concepts/core/index.md:13` が「基準領域」の平文になった。concepts 全体で残る `LayoutArea` は `maui/api/layout-surface.md:42` (MAUI の実在プロパティ名。`maui/KsDialogs.Maui/Internals/DialogOptions.cs:27` で確認) と `log.md` の履歴記述だけ |
| R1 Minor 3 | 書き直した項目に残る構造 lint 違反 5 件 | **解消** | `transition-semantics.md` は冒頭の 2 項目と duration を段落へ、脱出口を 4 行の表へ移し、`layout-semantics.md` は冒頭の読み方を段落へ出した。baseline (218d5c0) と突き合わせて、脱出口の 3 分岐 (退出中 / 出現中 / 表示中) と Scenario ID・duration の全条件が保存されていることを確認。lint の残存も下記のとおり di-registration 10 + reference-repositories 1 のみ |
| R1 Suggestion 1 | `layout-semantics.md` の `approvedDiff` 統制の二重記述 | **解消** | `kasane/concepts/core/api/layout-semantics.md:216` がリンク 1 本に寄せられた。`:202` の保証 (同じ入力なら同じ rect) は core に残っており、review-001 の推奨どおり |
| R1 Suggestion 2 | concept 側禁止トークン検査の実効範囲の注記 | **解消** | `verification/forbidden-tokens-concepts.txt` の冒頭 8 行に、新設 surface 由来分が構成上 0 件になること・独立した信号は除外リスト由来の非 API トークンと kmp の追加分であること・Decision 7 の達成判定は task 6.1 で行うことが明記された |

**未解消: 0 件。**

## 再実行した機械検査 (すべて成果物の報告と一致)

| 検査 | 結果 |
|---|---|
| `verification/identifier-landing.py check` | 台帳 108 行: 着地 89 / 意図して落とした 19 / 未説明 0 (exit 0) |
| 禁止トークン負の検査 (concept 側、自前で再実装) | ios 200 / android 178 / maui 154 (exempt 3) / kmp 152 / aiforms-migration 151 — 5 範囲すべて一致 0 件 |
| core-contract-check 検査 1 (共通概念名以外の識別子) | 一覧外 0 件。出現 11 種の内訳も報告と完全一致 (`DialogNotifier` 17 / `DialogTransition` 9 / `DialogPlacement` 6 / …) |
| core-contract-check 検査 2 (形態別テーブル) | 形態名で始まる表の行 15 件 / 識別子を含む行 0 件 |
| core-contract-check 検査 4 (ケース表の移動) | `layout-semantics.md` はバッククォート・平文とも 0 件、`layout-case-table.md` 10 件 |
| `scripts/doc-structure-lint.py` (scope モード) | `kasane/concepts` の違反は `maui/api/di-registration.md` 10 件 + `cross/reference/reference-repositories.md` 1 件のみ (いずれも本 change が触っていないファイル)。書き直した 5 件は消えている |
| `scripts/local-path-lint.py` / `identity-lint.py` | ともに exit 0 |
| concepts 内の相対リンク解決 | 全 39 ファイル走査・未解決 0 件 |
| frontmatter (type/title/description/tags/timestamp) と h1 一致 | 概念 30 本すべて適合 |
| core 8 本の「形態別の公開面」節 / 公開面 18 本の冒頭宣言と core リンク | すべて存在し解決 |
| `git diff --check` | 指摘なし |
| 足場の凍結 | `proposal.md` / `design.md` / `specs/` は未変更。`tasks.md` の差分 22 行はすべてチェックボックスの反転のみ (本文の書き換えなし)。グループ 5〜7 は未チェックのまま |

### KMP 禁止集合の再導出 (独立再現)

design.md Decision 6 の `targets` から許可面 (`kmp/api/*` + `android/api/*` + `ios/api/layout-surface.md` + `ios/api/transition-surface.md`) と他 platform 面 (`maui/api/*` + `ios/api/{dialog,loading,toast}-surface.md`) を自分で組み、`identifier-landing.py` と同じ抽出規則 (`TOKEN` / `STOP`) で差集合を取った。

- 「他 platform 面 − 許可面」= **146 件**。`forbidden-tokens.json` の kmp 152 件との差分を取ると、**導出に出て集合に無いのはちょうど `AnyObject` / `Sendable` / `Result` の 3 件**で、baseline.md が意図して外したと書いている 3 件と一致した。取りこぼしはゼロ
- **この 3 件を外した判断は妥当**。`ios/Sources/KsDialogs/Kmp/KsDialogsKmp.swift:48`・`:72`・`:95` の `register<ViewModel: AnyObject, Result: Sendable>` のとおり、KMP の Swift 向け公開面の署名そのものに現れる Swift の言語・標準ライブラリの語である。禁止すると `references/ios-host.md` の正当な署名記述を落とす。同じ理屈で許可面に入るべき `UIView` は許可面 concept に出現するため自動的に除外されており、逆に許可面に出てこない `UIHostingController` は追加されている — 一貫している
- baseline.md が列挙する追加 19 件 (`Dialog.shared` 系・`DialogError` と case 4 種・`LoadingStyle.ProgressFormat` など) はいずれも `ios/api/{dialog,loading,toast}-surface.md` にのみ現れることを確認した

## 指摘事項

### [🟡 Minor] KMP 禁止集合に、許可面の中にある 4 トークンが残っている

**該当箇所**: `verification/forbidden-tokens.json` (kmp の `tokens`)、`verification/baseline.md:104-106`

**問題点**:

kmp の禁止集合 152 件のうち次の 4 件は、KMP Skill の許可面 (= design.md Decision 6 の `targets` の和集合) に含まれる concept に実際に出現する:

| トークン | 出現する concept | `targets` 上の経路 |
|---|---|---|
| `KsDialogAttributes` | `android/api/{dialog,layout,transition}-surface.md` | `ksdialogs-kmp/references/android-host.md` (`android/api/` 5 本)・`layout.md`・`transitions.md` |
| `SimpleDialogViewModel` | `android/api/dialog-surface.md` | 同上 (android-host) |
| `ksDialogOptions` | `android/api/layout-surface.md`・`ios/api/layout-surface.md` | `ksdialogs-kmp/references/layout.md` |
| `ksDialogPlacement` | 同上 | 同上 |

これらは現行除外リスト由来の初期値 (`source: current-exclusion-list + new-surface`) で、理由欄も「この対象 Skill の正規公開面には存在しない」と旧構成の判断のまま残っている。修正サイクルで足した新設 surface 由来分は「他 platform 面 − 許可面」で正しく導かれているので、**新旧が混ざった集合が baseline.md 自身の掲げる不変条件と食い違っている**状態である。baseline.md `:104-106` は「**許可面は targets の和集合ちょうどに揃える**」「そこを禁止すると Skill 側の検査が正当な記述を落としてしまう」と書いており、この 4 件はまさにその失敗側に当たる。

concept 側の検査は kmp の禁止集合を `kmp/api/*.md` とだけ突き合わせる (許可面の一部でしかない) ため 0 件で通り、検出できない。実害が出るのは task 6.1 の Skill 側の検査で、生成された KMP Skill の `references/android-host.md` / `layout.md` / `transitions.md` にこれらの名前が載れば違反として報告される。task 6.2 に仕分けの手当てはあるが、「禁止トークンが出た → Skill から消す」と処理すると Android ホスト側の正当な記述が落ちる。

**推奨修正**: この 4 件を kmp の `tokens` から外す (`concept-exempt` ではなく削除 — exempt は「Skill 本文にだけ禁止を効かせる」区分なので逆向きになる)。あわせて、他の Skill 範囲でも同じ取り違えが無いことを機械的に確かめておくとよい (各範囲の禁止集合と許可面 concept の識別子の積集合が空であること。ios / android / maui / aiforms-migration は許可面 = 検査対象ディレクトリなので既存の 0 件がそのまま保証になっており、kmp だけが許可面 ⊋ 検査対象という非対称を持つ)。task 6.1 に入る前に直しておくと、6.2 の仕分けで迷わずに済む。

### [🟡 Minor] `concepts/log.md` の構造 lint の件数の記述が事実と食い違う

**該当箇所**: `kasane/concepts/log.md:40`

**問題点**:

2026-09-05 の記録に「doc-structure / local-path / identity lint 通過 (残る構造違反 16 件は baseline から持ち越しで内訳は実装報告)」とある。この「16 件が持ち越し」は記録時点でも不正確で (review-001 が示したとおり 5 件は本 change で書き直した本文の中にあった)、修正サイクルでその 5 件が是正された今は件数も合わない。現在 `kasane/concepts` に残る構造違反は 11 件 (`maui/api/di-registration.md` 10 + `cross/reference/reference-repositories.md` 1) で、これは正しく持ち越しである。

`concepts/log.md` は長命層の append-only 履歴であり、後から「なぜ違反が残っているのか」を辿る入口になる。数と性格が両方ずれていると、次に棚卸しする側 (ksn-drift) が持ち越しの範囲を読み違える。

**推奨修正**: 当該行の該当部分を「残る構造違反 11 件 (`maui/api/di-registration.md` 10 / `cross/reference/reference-repositories.md` 1) は本 change が触っていないファイルからの持ち越し」に直す。本 change はまだ archive 前で log.md には task 6.3 でも追記する予定があるため、その機会に合わせて直せばよい。

### [🔵 Suggestion] `doc-structure-lint.py` の `--paths` モードは index / log / rules の除外を迂回する

**該当箇所**: `scripts/doc-structure-lint.py:46-47`・`:184-186`、`kasane/changes/split-concepts-platform-surface/tasks.md` (4.4・7.2)

**問題点**: `SKIP_NAMES = {"index.md", "log.md", "rules.md"}` は `targets()` の walk 経路にしか効かず、`--paths` に明示したファイルはそのまま検査される。そのため `find kasane/concepts -name '*.md'` の結果を丸ごと `--paths` に渡すと、規約 (ksn-core `references/doc-structure.md` の「適用対象」表・config.yaml の `lint.doc-structure.scope` のコメント) が対象外と定めた 3 種まで拾い、`kasane/concepts` だけで **54 件 / 6 ファイル**になる (scope モードなら 11 件 / 2 ファイル)。差の 43 件はすべて index 4 ファイルと log.md である。

本 change の欠陥ではない (baseline でも同じ挙動) が、task 4.4 / 7.2 の「lint を掛ける」がどちらの掛け方かで結果が 5 倍変わるため、後任やレビュアーが再現できない。

**推奨修正**: 掛け方を scope モード (`python3 scripts/doc-structure-lint.py` 引数なし) に統一するか、`--paths` を使うなら一覧から index.md / log.md / rules.md を除いてから渡すことを tasks 側に書き添える。スクリプト自体を `--paths` でも `SKIP_NAMES` を効かせる形に直すのは標準装備の改修なので、本 change ではなく ksn-update / 別 change の範疇として扱うのが筋。

## 判断材料として書いておくこと (指摘ではない)

- **修正が新たな主張を持ち込んでいないことを baseline と突き合わせて確認した**。`transition-semantics.md` の脱出口は baseline `:198-203` の箇条書き 1 項目 (3 分岐) が 4 行の表になったもので、`PB-TR-24` / `-29` / `-28` と「表示中は通常の退出」がすべて保存されている。`layout-semantics.md` の冒頭も、原典・現行実装・ADR 列・パススルーの各主張が段落に移っただけで欠落なし。`model-binding-semantics.md` は 1 引数 factory の段落が縮んだ方向 (主張の削減) で、増えた主張は無い
- **`git status` 上の変更はすべて本 change の範囲に収まる**。tracked 側は concepts 16 本 + tasks.md、untracked 側は新設 concept 18 本 + change 配下の deviation / review / second-opinion / verification のみで、コード・スクリプト・handbook・decisions への書き込みは無い
- **deviation.md は修正サイクル後も整合している**。`LayoutArea` の乖離記録は `core/index.md` の修正で本文・目次とも一致し、付随修正 7 件はいずれも ksn-core の同梱条件内 (archive パスの追随・リンク解決の事実追随・自身の書き込みが発火させた構造 lint の是正)。構造 lint 5 件は「是正しない判断なら deviation に記録」ではなく実際に是正されたので、deviation への追記は不要
- **`concept-exempt` の使い分けは正しい**。maui の 3 件だけが exempt で、これは「concept には載るが利用者向け MAUI Skill には載せない」というオーナー判断の反映。上記 Minor 1 の 4 件はこれとは別の類型 (許可面そのもの) なので、exempt ではなく削除が適切

## アクションプラン

1. **[Minor]** `verification/forbidden-tokens.json` の kmp から `KsDialogAttributes` / `SimpleDialogViewModel` / `ksDialogOptions` / `ksDialogPlacement` を外し、件数を baseline.md に反映する。task 6.1 に入る前に
2. **[Minor]** `kasane/concepts/log.md:40` の構造違反の件数と性格を実態 (11 件・すべて持ち越し) に直す。task 6.3 で log.md を触るときに合わせて
3. **[Suggestion]** doc-structure lint の掛け方 (scope モード / `--paths` から index・log・rules を除く) を tasks 4.4・7.2 に明記する

1 は JSON から 4 キーを消すだけ、2 は 1 行の書き換えで済む。どちらも公開名を動かさないので、着地台帳・禁止トークンの再採取は不要 (1 のあと concept 側の負の検査だけ再実行すれば足りる)。
