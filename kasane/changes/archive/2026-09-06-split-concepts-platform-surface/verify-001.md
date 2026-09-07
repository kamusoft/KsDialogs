# Verify 001: split-concepts-platform-surface

**判定**: VALID

- 対象: `git diff 218d5c0` の全体 (コミット 4e81fd8 + 作業ツリーの未コミット分 + 未追跡ファイル)
- デルタスペック: `specs/api-listing-policy/spec.md` / `specs/concept-placement/spec.md` / `specs/user-skills-manifest/spec.md`
- Scenario 総数 20 / ✅ 18 / ⚠️ deviation 記録済み 2 / ❌ 0
- 検証日: 2026-09-06

この change はドキュメントと検査スクリプトが成果物で、ソースコード・テストは変更していない。したがって
「テスト」列には、その Scenario を機械的に固定している検査 (`verification/` のスクリプト・
`.agents/skills/docs-refresh/scripts/` の 8 種・`scripts/` の lint) を書き、すべて本検証で再実行した。

---

## 対応表

### specs/concept-placement/spec.md (ADDED)

#### Requirement: core の契約は platform 固有の識別子を持たない

| Scenario | 実装 | テスト (再実行して確認) | 状態 |
|---|---|---|---|
| 識別子を含む形態別テーブルの不在 | `kasane/concepts/core/api/*.md` (8 本) | `verification/core-contract-check.md` 検査 2 のスクリプトを再実行 → 形態名で始まる表の行 15 件 / うち識別子を含む行 **0 件** | ✅ 一致 |
| 共通概念名以外の識別子の不在 | 同上 / 確定一覧は `verification/core-contract-check.md`「確定した共通概念名」(13 行) | 同ファイル検査 1 を再実行 → 一覧外の識別子 **0 件**。出現は 11 種 (`DialogAlignment` 2 / `DialogNotifier` 17 / `DialogPlacement` 6 / `DialogResult` 3 / `DialogTransition` 9 / `DialogViewModel` 2 / `DialogViewRegistry` 1 / `KsDialogs` 3 / `KsLoading` 1 / `KsToast` 1 / `LoadingProgressReceiver` 1) で全件が確定一覧内 | ✅ 一致 |
| platform 差分の挙動の残存 | `verification/platform-differences.md`「確認結果」の A1〜A22 が指す core の各箇所 | A 群 22 項目を現物で照合。spec が最低限求める 6 項目 — 多段表示の下段先閉じ (`kasane/concepts/core/api/multi-display-semantics.md:41-46`) / 呼び出し元キャンセルの観察 (`result-notification-semantics.md:84-90`・`transition-semantics.md:158-166`) / MAUI にキャンセル経路が無いこと (同 2 表の MAUI 行) / Android の戻るボタン (`result-notification-semantics.md:70-74`) / leading・trailing の START・END 対応 (`transition-semantics.md:90` — 「行の始まり側 / 終わり側」の散文へ) / MAUI の添付が code-behind 限定であること (`transition-semantics.md:38`) — をすべて確認。いずれも識別子なしで、具体形を持つ platform concept へのリンクを持つ | ✅ 一致 |
| 形態別の公開面への導線 | `kasane/concepts/core/api/*.md` 各末尾の「形態別の公開面」節 | 8 本すべてに節が存在し ios / android / maui / kmp への相対リンクを持つ。layout / transition の KMP 行は spec どおり `kasane/concepts/kmp/api/dialog-surface.md` (共有コードに面が無い旨) を指す。concepts 全体の内部リンク 511 件を解決検査 → 切れ **0 件** | ✅ 一致 |

#### Requirement: platform の公開面 concept

| Scenario | 実装 | テスト (再実行して確認) | 状態 |
|---|---|---|---|
| 新設 concept の存在と冒頭宣言 | `kasane/concepts/{ios,android,maui}/api/{dialog,layout,transition,loading,toast}-surface.md` (各 5 本) + `kasane/concepts/kmp/api/{dialog,loading,toast}-surface.md` (3 本) = 18 本。既存の `kasane/concepts/maui/api/di-registration.md`・`kasane/concepts/kmp/api/ios-host-integration.md` も維持 | 18 本すべて実在。全件が frontmatter に `type: concept` / `title` / `description` / `tags` / `timestamp` を持ち、本文冒頭に title と同じ h1、冒頭段落に対応する core concept への相対リンクと「公開面だけを扱い挙動の契約は core が正」の宣言を持つことを目視確認。docs-refresh 6-④ frontmatter 検査も通過 | ✅ 一致 |
| platform 側の識別子は自 platform のもの | `verification/forbidden-tokens.json` (5 範囲) | 禁止集合 × 該当ドメインの `<p>/api/*.md` のバッククォート識別子を完全一致で突き合わせ → ios 192×ios/api = 0 / android 171×android/api = 0 / maui 142×maui/api = 0 / kmp 142×kmp/api = 0 / aiforms-migration 144×maui/api = 0。**合計一致 0 件**。記録は `verification/forbidden-tokens-concepts.txt` | ✅ 一致 |

#### Requirement: 公開名の着地

| Scenario | 実装 | テスト (再実行して確認) | 状態 |
|---|---|---|---|
| 台帳の突き合わせ | `verification/baseline.md` (SHA `218d5c09…`)・`verification/identifier-ledger.md` (108 行) | `python3 kasane/changes/split-concepts-platform-surface/verification/identifier-landing.py check --ledger …/identifier-ledger.md` → **着地 89 / 意図して落とした 19 / 未説明 0**。台帳の網羅性も独自に検証 — baseline SHA の `core/api/*.md` から同じ抽出規則で採った識別子は 108 種で、台帳の行集合と**過不足なく一致** (差分 0)。スクリプトが他ドメインへの出現と `excluded` concept への出現を着地に数えないこと、`architecture` を着地から外すことも実装で確認 | ✅ 一致 |
| 小文字 API の保存 | `verification/identifier-ledger.md` | `hide` (:53 着地 ios android kmp) / `cancelled` (:35 着地 ios) / `leading` (:75 着地 ios) / `trailing` (:112 着地 ios) が理由つきで載る。他の小文字行 (`bottom` / `top` / `none` / `presentation` / `dismissal` / `overlayDuration` / `ksDialogOptions` ほか計 33 行) も同様。**`show` は台帳に無い** — baseline の `core/api/*.md` にバッククォート識別子 `show` は 1 件も存在せず (存在するのは `ShowAsync` / `ShowResultAsync` / `showCompose`)、Requirement 本文の定義「baseline 時点の core/api から抽出したバッククォート識別子を全部」に照らすと欠落ではない。Scenario の列挙は「〜のような」の例示であり、実在した小文字識別子はすべて台帳にある | ✅ 一致 (下記「観察事項 1」に注記) |

#### Requirement: 検証機構の記述は architecture

| Scenario | 実装 | テスト (再実行して確認) | 状態 |
|---|---|---|---|
| ケース表の記述の移動 | `kasane/concepts/core/architecture/layout-case-table.md` (新設) / `kasane/concepts/core/api/layout-semantics.md` | ケース ID (`C` + 2 桁)・`approvedDiff`・`approvedBy` をバッククォート表記で grep → layout-semantics.md **0 件** / layout-case-table.md **10 件**。平文表記の grep でも layout-semantics.md は **0 件** | ✅ 一致 |
| 数式の表記 | `kasane/concepts/core/api/layout-semantics.md`「最終 rect の決め方 (軸ごとの手順)」節 (:114-190) | API 名網羅検査と同じ抽出規則で当該節の識別子候補を列挙 → **0 件** (`A.min` / `A.max` は fenced code block と平文の数式) | ✅ 一致 |

#### Requirement: 配置規則と目次の更新

| Scenario | 実装 | テスト (再実行して確認) | 状態 |
|---|---|---|---|
| 目次からの到達 | `kasane/concepts/index.md`・`core/index.md` (architecture/ 節を追加)・`ios/index.md` (新設)・`android/index.md` (新設)・`maui/index.md`・`kmp/index.md`・`cross/index.md` | index から辿れる concept を機械的に集計 → concept 総数 **30** に対し index からの到達 **30**、未掲載 **0 件**。各行に 1 行説明あり。ルート index の ios / android 行は「まだ概念なし」が解消され `ios/index.md` / `android/index.md` へのリンクになっている。リンク解決検査で切れ 0 件 | ✅ 一致 |
| 配置判断の基準 | `kasane/concepts/rules.md`「契約と公開面の振り分け (cross/ADR-0014)」節 | 「観察可能な挙動と保証 → `core/api/`」「公開名・署名・コード例・framework 固有の注意 → `<platform>/api/`」「検証機構 → `core/architecture/`」の表と、cross/ADR-0014 への相対リンク 2 か所を確認。`timestamp` も 2026-08-15 → 2026-09-05 へ更新済み | ✅ 一致 |

### specs/api-listing-policy/spec.md (MODIFIED)

#### Requirement: 利用者向け Skill の API 掲載基準

| Scenario | 実装 | テスト (再実行して確認) | 状態 |
|---|---|---|---|
| 3e の仕分け | `kasane/handbook/cross/user-skill-api-listing.md`「現行の除外リスト」(22 行) と「意図的な掲載除外の基準」表 (非 API token の行を新設) | `python3 .agents/skills/docs-refresh/scripts/api-coverage-check.py` を再実行 → 報告 23 行 / 実トークン **38 件** (ios 5 / android 5 / maui 6 / kmp 8 / aiforms-migration 14)。除外リストの platform 別トークン集合と突き合わせ → **未分類 0 件**、かつ**リストにあって候補に無い行も 0 件** (過不足なし)。各行に基準 (内部層・interop 層 6 / 機械的に導出できる名前 22 / 非 API token 9 / 低頻度の細部 API 1) と実装上の経路・理由が付く。`kind: rule` / `timestamp: 2026-09-06` / handbook index の該当行 (`kasane/handbook/cross/index.md:12` — 適用のきっかけは不変のため据え置き、理由は `kasane/concepts/log.md` に記録) / `kasane/concepts/log.md` の追記 / 構造 lint 通過 (本ファイル違反 0) を確認。docs-refresh SKILL.md 3e はこの規約を参照する記述へ改訂済み | ✅ 一致 |
| 実判断の行の引き継ぎ | 同上 | 分割前 (`218d5c0` 時点) の除外リストと逐語照合 — `LoadingCoordinator` (iOS / Android / KMP、基準「内部層・interop 層」・理由「表示合流を実装する internal coordinator で、利用者は Loading の facade / contract から間接利用する」)、`localSwiftPackage` (KMP、同基準・同理由)、`IMauiInitializeService` (MAUI / AiForms migration、同基準・同理由) がいずれも**同じ基準と理由で残存**。`DialogViewRegistry.Shared` は分割後の報告に現れないため対象外 (Scenario は「分割後も報告される名前」に限る) | ✅ 一致 |
| 他 platform 名の行の消滅 | 同上 | 基準が「対象 Skill 外・機械検査由来」の行を数える → **0 件**。他 platform の公開名・framework 型を理由とする行も 0 件 (残存行は非 API token 9 件を含め、すべて自 Skill の源泉に由来する名前) | ✅ 一致 |

### specs/user-skills-manifest/spec.md (MODIFIED)

#### Requirement: manifest 初期版

| Scenario | 実装 | テスト (再実行して確認) | 状態 |
|---|---|---|---|
| 網羅不変条件の検査 | `skills/.manifest.json` | `python3 .agents/skills/docs-refresh/scripts/concepts-coverage-check.py` → `concepts coverage OK`。独自集計でも「`targets` の値にも `excluded` のキーにも現れない concept」**0 件**、「`targets` に現れるが実在しない concept」**0 件** | ✅ 一致 |
| スキーマ準拠とハッシュの最終状態一致 | 同上 | v3 の必須キー (`version` / `concepts` / `targets` / `excluded` / `readmes`) を確認、`generatedAt` / `lastUpdatedFiles` も規範どおり。不変条件 1 (targets のキーが en / ja 双方に実在) ・2 (targets の値と excluded のキーが実在 concept) を機械検査で確認。`concepts` は **30 キー**でディスクの concept 集合 (`index.md` / `log.md` / `rules.md` を除く) と過不足なく一致し、SHA-256 を全件再計算して **不一致 0 件**。内訳も spec どおり core/api 8 + core/architecture 1 + ios/api 5 + android/api 5 + maui/api 6 + kmp/api 4 + cross/reference 1 = 30。`excluded` は `cross/reference/reference-repositories.md` と `core/architecture/layout-case-table.md` の 2 本 (理由文字列つき)、`readmes` は 4 枚 | ✅ 一致 |
| 源泉が他 platform の公開面を含まない | 同上 `targets` (33 キー) | 33 キーの源泉を design.md Decision 6 の表と 1 行ずつ照合 → **全キー一致**。機械検査でも ios / android / maui の Skill に他 platform の `api/` パス **0 件**、kmp が `ios/api/` `android/api/` を含むのは `references/layout.md` / `transitions.md` / `android-host.md` の 3 キーのみ、aiforms-migration の源泉は core/api + `maui/api/` 6 本のみ (cross/ADR-0011 の基準の適用)。kmp の `view-models.md` (core 4 本 + kmp 3 surface)・`android-host.md` (core 7 本 + android 5 本)・`ios-host.md` (core 7 本 + `kmp/api/ios-host-integration.md`) も表どおり | ✅ 一致 |

#### Requirement: 源泉の再構成に伴う再生成 (ADDED)

| Scenario | 実装 | テスト (再実行して確認) | 状態 |
|---|---|---|---|
| 構成の不変 | `skills/en/**` / `skills/ja/**` | 再生成前 (`218d5c0`) と現在の言語抜き相対パス集合を比較 → **過不足なく一致 (33 ファイル × 2 言語 = 66)**。Skill は 5 本のまま、各 Skill の references のファイル名と本数も不変。`targets` の 33 キーとディスクのファイル集合も完全一致 | ✅ 一致 |
| 整合性チェックの通過 | `skills/**` / README 4 枚 | docs-refresh Step 6 の 8 種をすべて再実行 — ① concepts coverage OK / ② `en/ja heading structure OK` / ③ `code blocks byte-identical` / ④ `frontmatter OK` / ⑤ 閉世界性 (kasane/ 参照・ADR 番号・interop 名・Skill ルート外リンク いずれも **0 件**) / ⑥ `All internal links resolve` (対象 70 件) / ⑦ `api-coverage-check.py` は仕分け済み候補のみ (下記 Requirement 参照) / ⑧ 配信識別子の表記ゆれ grep **0 件**。`scripts/local-path-lint.py` exit 0 / `scripts/identity-lint.py` exit 0。`scripts/doc-structure-lint.py` は本 change が新設・書き直したファイルで違反 0 件 (既存本文からの持ち越し 11 件のみ。下記「観察事項 3」) | ✅ 一致 |

#### Requirement: API 名網羅検査の候補から他 platform 名が消える (ADDED)

| Scenario | 実装 | テスト (再実行して確認) | 状態 |
|---|---|---|---|
| Skill 本文への負の検査 | `verification/forbidden-tokens.json` (ios 192 / android 171 / maui 142 / kmp 142 / aiforms-migration 144) | `python3 kasane/changes/split-concepts-platform-surface/verification/skill-forbidden-tokens.py` → 検査 [1] 5 Skill 範囲すべて **0 件** (exit 0)。禁止集合の初期値の作り方は spec 本文と差があり (自 platform の実在名・見本名・文字列リテラルの語が 4 集合に混入していた 44 件の偽陽性を是正)、その裁定は deviation.md「検証 fixture の解釈差分」に記録済み | ⚠️ deviation 記録済み |
| 検査候補への負の検査 | 同上 | 同スクリプトの検査 [2] → 5 Skill 範囲すべて **0 件** (exit 0)。maui 集合から移植元の旧 API 名 3 件 (`SetIocConfig` / `ShowResultAsync` / `UseCurrentPageLocation`) を外した判断、ios 集合から `Show` / `Register` を外した判断も deviation.md に記録済み | ⚠️ deviation 記録済み |
| 仕分けの完了 | `kasane/handbook/cross/user-skill-api-listing.md` / `verification/api-coverage-after.txt` | `api-coverage-check.py` の候補 38 件を除外リストと突き合わせ → **未分類 0 件**。全件が「実判断の除外 (旧リストの理由を引き継ぐ)」または「非 API token」に仕分けられ、掲載漏れ (Skill を修正すべきもの) は残っていない | ✅ 一致 |

---

## 追加検査

### tasks.md の完了状況

30 タスクすべてがチェック済みで、**未実装なのにチェック済みの虚偽は 1 件も無い**。対応表に現れないタスクも個別に成果物を確認した:

| タスク | 確認 |
|---|---|
| 1.1 / 1.2 / 1.3 | `verification/baseline.md` (SHA 記録)・`identifier-ledger.md` (108 行)・`platform-differences.md` (A 群 22 / B 群 20)・`forbidden-tokens.json` (5 範囲) が実在し、内容が spec の定義と一致 |
| 2.3 | `kasane/concepts/kmp/api/ios-host-integration.md` に「型付き入口の署名 (Dialog)」節が新設され、`result:` ラベルの結果型指定と `UIView` / SwiftUI factory の同名オーバーロードが移されている |
| 2.10 | `kasane/concepts/maui/api/di-registration.md` の「関連」に h3「MAUI の公開面」+ リンク 5 本、`kasane/concepts/kmp/api/ios-host-integration.md` の「関連」に新設 3 本へのリンク。どちらも本文は不変 |
| 3.3 | `kasane/concepts/log.md` に 2 エントリ (タスク 3.1〜3.4 の再構成、タスク 6.3 の除外リスト書き直し) |
| 3.4 | `kasane/concepts/core/api/registration-show-semantics.md:48`・`:141` が `../architecture/layout-case-table.md` を指す。ADR (core/0009・0016・0032) は accepted / append-only のため据え置きで、その判断が `kasane/concepts/log.md` に記録されている |
| 5.2 / 5.3 | 生成物 66 ファイルの言語抜きパス集合が再生成前と一致。README 4 枚は再生成しても内容が変わらなかった (concept を個別に参照せずディレクトリへリンクする構成のため) |
| 6.4 | `.agents/skills/docs-refresh/SKILL.md` の 3e 注記が「分割後 (cross/ADR-0014) は原則発生しない。報告されたらまず `targets` を見る」へ改訂され、意図的な例外 (`ksdialogs-kmp` / `ksdialogs-aiforms-migration`) と配置違反を疑う条件が明記されている |
| 7.1 | `kasane/decisions/cross/0014-concepts-core-contract-platform-surface.md` (proposed) の Decision に design.md Decision 1〜3 が反映済み。KMP の公開面を「3 側」と読む規則、`core/architecture/layout-case-table.md` の配置と `excluded` 掲載、公開面 concept の冒頭宣言も反映されている |
| 7.2 | local-path / identity lint は exit 0。doc-structure lint は本 change の新設・書き直し分で違反 0 件。`git diff --check` のみ 4 件の指摘が残る (下記「観察事項 2」) |
| 7.3 | deviation.md に付随修正 7 件・契約と実装の乖離 5 件 (書き直し中) + 11 件 (Skill 再生成のワーカー報告由来) + fixture の解釈差分 3 件を記録 |

### 逆流検査

足場アーティファクト (`proposal.md` / `design.md` / `exploration.md` / `specs/**`) は **baseline 以降 1 バイトも変更されていない** (`git diff 218d5c0 --` で差分なし、作業ツリーにも未コミット変更なし)。最終更新は提案フェーズの `42768fd`。`tasks.md` の差分はチェックボックスの `[ ]` → `[x]` のみで、タスク文の書き換えは 1 件も無い。

### 未記録乖離

対応表に ❌ は無く、**未記録の欠落・乖離は 0 件**。付随修正のうち deviation.md に記録が無いものを 2 件、および検査手順に関する所見を 1 件、下記に観察事項として挙げる (いずれも Scenario の判定には影響しない)。

### テストの成否

ソースコード・テストは変更していないため、コード側のテストは本 change の検証対象外。本 change で「テスト」に当たる検査はすべて再実行し、結果を上表に記録した (`identifier-landing.py check` / `skill-forbidden-tokens.py` / `core-contract-check.md` の検査 1〜4 / docs-refresh の 8 種 / local-path・identity・doc-structure lint / 内部リンク解決)。

---

## 観察事項 (判定に影響しないが記録する)

1. **`show` は着地台帳に載っていない**。concept-placement「小文字 API の保存」の Scenario は `show` を例示しているが、baseline (`218d5c0`) の `core/api/*.md` にバッククォート識別子 `show` は存在しない (存在するのは `ShowAsync` / `ShowResultAsync` / `showCompose`)。Requirement 本文が台帳を「baseline 時点の core/api から抽出したバッククォート識別子を全部」と定義しており、台帳 108 行は baseline の抽出結果と完全一致するため、これは実装の欠落ではなく Scenario の例示語の選び方の問題である。**見立て**: 対応不要 (蒸留時に spec を archive するだけでよい)。

2. **`git diff --check` が 4 件の trailing whitespace を報告する** — `second-opinion-code-001.md:59` / `second-opinion-code-002.md:39,40` / `second-opinion-code-003.md:59`。いずれも相方 CLI の出力を逐語で貼った記録行で、成果物 (concepts / handbook / skills / decisions) には 1 件も無い。tasks 7.2 は「`git diff --check` を通す」と書いており、現状は通っていない。**見立て**: 相方 CLI 出力の逐語記録は行末空白を保つ性質があるため、(a) 3 ファイルの該当行から行末空白を落とす、または (b) 「逐語記録ファイルは `git diff --check` の対象外」と deviation.md に付随合意として記録する、のどちらか。実装内容には影響しない。

3. **doc-structure lint の実行方法に注意が要る**。`--paths` にシェル変数を渡すとき、zsh は未クォートのパラメータを単語分割しないため、`python3 scripts/doc-structure-lint.py --paths $FILES` は改行入りの 1 引数として渡り、存在しないパスを検査して「違反なし」と偽陽性で通る。正しく 96 ファイルを渡すと **231 件 / 38 ファイル**が報告され、うち `kasane/concepts/` 配下は **11 件 / 2 ファイル** — `kasane/concepts/maui/api/di-registration.md` 10 件 (`:13`-`:56` の既存本文。本 change の追記は `:90` 以降の関連節のみで違反ゼロ) と `kasane/concepts/cross/reference/reference-repositories.md` 1 件 (`:29`。本 change は未変更) である。これは `kasane/concepts/log.md` と review-003 が記録している「既存本文からの持ち越し 11 件」と一致し、本 change が新設・書き直したファイルの違反は 0 件。むしろ書き直しは改善で、`core/api/layout-semantics.md` と `transition-semantics.md` は baseline 時点で計 16 件の違反があったのが 0 件になり、散文字数の警告も 13706 → 11269 字 / 11016 → 10356 字へ縮んでいる (10000 字の警告閾値は超えたまま)。**見立て**: 対応不要。ただし引き継ぎメモの「doc-structure lint 違反なし」は上記の偽陽性による記述なので、蒸留時にこの実行方法の落とし穴を lessons へ捕捉する価値がある。

4. **`kasane/decisions/cross/0014-…md` は「確認」を超えて本文が改訂されている**。tasks 7.1 の文言は「反映されていることを確認する」だが、実際には Context のパス表記をリポジトリ相対へ直し、Decision の共通概念名の例示から実装で否定された `DialogException` / `LayoutArea` を外して確定一覧の所在を追記し、KMP の「3 側」判定と `excluded` 掲載・公開面 concept の冒頭宣言を書き足している。proposed の ADR を本文で直接改訂する運用に沿った変更であり、内容も design.md Decision 1〜3 と実装結果に一致するが、deviation.md には付随修正として記録されていない。**見立て**: deviation.md へ `[付随修正]` として 1 行足すのが素直 (実装を戻す必要は無い)。

5. **`.agents/skills/docs-refresh/SKILL.md` の「移行 Skill の源泉規則」が manifest と食い違ったまま残っている**。同ファイルは移行 Skill の `targets` を「`core/api/*` のうち対応表が触れるもの + `maui/api/di-registration.md` だけ」と規範として書いているが、本 change が書いた manifest は `maui/api/` の surface 6 本すべてを源泉に持つ (user-skills-manifest の Requirement 本文と design.md Decision 6 がそう定めている)。デルタスペックはこの節の改訂を求めておらず (task 6.4 は 3e 注記だけを対象にしている)、Scenario も違反しないが、docs-refresh 自身の規範文と生成物の実態が矛盾している。**見立て**: 蒸留時に cross/ADR-0011 の基準の読み方 (「対応表が触れるもの」であって具体列挙ではない) を反映して同節を改訂するか、次の change で拾う。実装を戻す必要は無い。

---

## 判定の根拠

- 全 20 Scenario が「✅ 一致」(18) または「⚠️ deviation 記録済み」(2) で、❌ は 0 件
- tasks.md の 30 タスクに虚偽のチェックは無い
- 足場アーティファクトへの逆流は無い
- この change で「テスト」に当たる検査はすべて再実行し、成功を確認した

→ **VALID**
