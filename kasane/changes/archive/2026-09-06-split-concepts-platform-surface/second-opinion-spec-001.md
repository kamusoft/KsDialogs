# セカンドオピニオン: split-concepts-platform-surface (spec-001)
**相方**: codex / **label**: so-spec-split-concepts-platform-surface / **日付**: 2026-09-05 / **対象**: kasane/changes/split-concepts-platform-surface/ の proposal.md / design.md / specs/ (3 能力) / tasks.md
---
# レビュー結果: split-concepts-platform-surface

**日付**: 2026-09-05  
**判定**: **NEEDS_DISCUSSION**

## サマリー

「core の契約と platform 公開面を分離する」という方向性は妥当ですが、現在の仕様では platform 固有の挙動差の置き場所、KMP の manifest 対応、既存 ADR との整合が確定していません。また、主要な受け入れ条件である識別子保存と他 platform 名除去は、現在指定された検査では証明できません。

**件数**: Critical 0 / Major 6 / Minor 2 / Suggestion 0

静的な提案レビューのため、ビルド・テストは実行していません。ファイル変更もありません。

## 照合した規約・実物

- `ksn-review` の判定基準・指摘形式
- ksn-core の delta-spec、handbook、domain-axis、paths
- `kasane/concepts/rules.md`
- 現行の core/api 8 concept、MAUI/KMP concept、各 index
- `kasane/decisions/cross/0011-user-docs-as-agent-skills.md`（accepted）
- `kasane/decisions/cross/0014-concepts-core-contract-platform-surface.md`（proposed、決定根拠には不使用）
- `kasane/handbook/cross/user-skill-api-listing.md`
- `skills/.manifest.json`
- `.agents/skills/docs-refresh/SKILL.md` と scripts 8 本

## 指摘事項

### [🟠 Major] platform 固有の観察可能な挙動を置く層が定義されていない

**該当箇所**: `design.md:43`、`specs/concept-placement/spec.md:7`、`specs/concept-placement/spec.md:29`、`tasks.md:17`

**問題点**: platform concept は「名前・署名・コード例・framework 固有の注意だけ」を扱い、挙動の契約は core が正とされています。一方、core は platform 非依存にするとされています。

しかし現行契約には、次のような意図的な platform 差があります。

- `kasane/concepts/core/api/multi-display-semantics.md:35`: 下段を先に閉じたとき、iOS は上段も消えて cancelled、Android は上段が残る
- `kasane/concepts/core/api/transition-semantics.md:207`: 呼び出し元キャンセルは Swift・Kotlin・MAUI で観察結果が異なる
- `tasks.md:17`: このキャンセル観察表を core から外す予定

これらは公開名でも framework の注意でもなく、利用者が観察する契約です。現仕様のままでは、core に残すと「platform 非依存」に反し、platform 側へ移すと「挙動の契約は core が正」に反します。Non-Goal の「意味を改訂しない」も保証できません。

**推奨修正**: 次のどちらかを設計判断として確定してください。

1. core は共通保証に加えて、承認済みの platform 差分表も保持できる。
2. platform concept が platform 固有の挙動契約も所有し、core は共通部分のみを所有する。

そのうえで、既存の platform 差分節すべてに移動先を割り当てる Scenario を追加してください。

### [🟠 Major] 「4形態すべてに同綴り」の許可リストが現行 API と矛盾する

**該当箇所**: `design.md:45`、`specs/concept-placement/spec.md:7`

**問題点**: 許可例には `DialogOptions`、`DialogTransition`、`LoadingStyle`、`ToastStyle`、`notifier` などが含まれていますが、現行 concept は次を明記しています。

- `kasane/concepts/core/api/layout-semantics.md:83`: KMP commonMain が公開するのは `DialogPlacement` のみで、`DialogOptions` は公開しない
- `kasane/concepts/core/api/transition-semantics.md:39`: KMP commonMain に transition の添付面はない
- `kasane/concepts/core/api/loading-semantics.md:66`: KMP commonMain に style/options は公開しない
- `kasane/concepts/core/api/model-binding-semantics.md:23`: C# は `Notifier` であり `notifier` と同綴りではない

したがって、列挙済みの名前自体が「4形態すべてに同綴りで存在する」という Requirement を満たしません。

**推奨修正**: 「全形態に同じ公開識別子として存在する名前」と「公開名が異なっても core 用語として共有する概念名」を分離してください。後者を許すなら、同綴り条件を外し、公開 API 網羅検査の対象にするかも別途定義する必要があります。

### [🟠 Major] KMP の manifest `targets` が矛盾し、全33キーを一意に構成できない

**該当箇所**: `proposal.md:18`、`design.md:31`、`design.md:89`、`specs/user-skills-manifest/spec.md:7`

**問題点**:

- KMP には `layout-surface.md` と `transition-surface.md` を作らない一方、`design.md:96` と manifest spec は各 references を「core + 自 platform の同名 surface」としています。
- `proposal.md:18` は KMP の iOS host に `ios/api/` と `kmp/api/` を使うとしていますが、`design.md:99` は core 7本 + `kmp/api/ios-host-integration.md` だけです。
- 現行 manifest の `ksdialogs-kmp/references/view-models.md` は model-binding のほか registration/loading/toast を源泉にしています（`skills/.manifest.json:128`）。提案表では core model-binding + KMP dialog-surface だけになり、Loading/Toast 変更を逆引きできなくなります。
- KMP の layout/transitions と iOS host が参照すべき Native platform surface も未確定です。

**推奨修正**: 抽象的な規則だけでなく、33個すべての `targets` キーについて完成後の源泉配列を確定してください。少なくとも KMP の `SKILL.md`、`view-models.md`、`layout.md`、`transitions.md`、`android-host.md`、`ios-host.md` は個別規則が必要です。

### [🟠 Major] 移行 Skill の源泉拡大が accepted ADR と docs-refresh 規範に反する

**該当箇所**: `design.md:100`、`specs/user-skills-manifest/spec.md:7`

**問題点**: 提案は移行 Skill の源泉を `core/api + maui/api 全本` とします。しかし accepted の `kasane/decisions/cross/0011-user-docs-as-agent-skills.md:32` と `.agents/skills/docs-refresh/SKILL.md:137` は、次の限定を規範としています。

- 対応表が触れる core/api のみ
- MAUI 側は `maui/api/di-registration.md`

全 `maui/api/` への拡大は、移行 Skill に新 API 全般の未掲載候補を要求し、移行対応表に絞る既存判断を変えます。proposed の ADR-0014 は、この accepted 判断を明示的に改訂していません。

**推奨修正**: 対応表が実際に扱う platform concept だけをファイル単位で列挙するか、源泉方針を変更するなら ADR-0011 と docs-refresh の移行 Skill 規範の改訂を proposal の明示的なスコープに加えてください。

### [🟠 Major] 識別子着地検査が公開名の保存を証明できず、Requirement 自体も矛盾する

**該当箇所**: `design.md:75`、`specs/concept-placement/spec.md:43`、`tasks.md:7`

**問題点**:

- Requirement は「S₀ の各要素が S₁ に含まれる SHALL」としますが、Scenario は差分を理由つきで記録すれば合格としています。空集合が必須なのか、承認済み除外を許すのか不明です。
- 流用する `looks_api` は `.agents/skills/docs-refresh/scripts/api-coverage-check.py:24` の規則上、`show`、`hide`、`start`、`register`、`cancel`、`complete`、`notifier` のような単純な小文字 API を抽出しません。
- S₁ が「全 concept の集合」なので、Android 名を iOS concept や excluded の architecture concept に誤配置しても着地扱いになります。
- 着手時点の commit SHA を固定する要件がなく、採取時期の再現性もありません。

**推奨修正**: baseline commit SHA を先に記録し、platform・出典節・期待する移動先を持つ識別子台帳を作ってください。判定は「正しい platform の許可パスへ着地」または「明示承認された除外」の二択とし、未説明差分を0件にしてください。保存検査には API coverage 用ヒューリスティックとは別の抽出規則が必要です。

### [🟠 Major] 「他 platform 名が消える」という主要ゴールが偽陽性で通過できる

**該当箇所**: `specs/concept-placement/spec.md:37`、`specs/user-skills-manifest/spec.md:45`

**問題点**: 禁止トークンは一部の例と「等」「同様」でしか定義されておらず、完全な platform 所有集合がありません。

さらに `api-coverage-check.py` は、源泉 token が生成 Skill に「無い」場合だけ報告します（`.agents/skills/docs-refresh/scripts/api-coverage-check.py:44`）。誤った他 platform 名を Skill にコピーすれば候補から消えるため、Scenario は成功します。ドット付き名も末尾メンバーがどこかにあれば掲載済み扱いです（同 `:55`）。

「候補の総数」Scenario も、数値・上限・比較対象を持たず、単なる人手分類なので題名どおりの回帰判定になっていません。

**推奨修正**: platform ごとの禁止トークン集合を fixture として固定し、concept と生成 Skill の双方に対する負の検査を追加してください。候補数を品質指標にするなら、baseline、完了時上限、未分類0件を別々に規定してください。

### [🟡 Minor] handbook の書き込み経路がタスクに明示されていない

**該当箇所**: `proposal.md:13`、`tasks.md:48`

**問題点**: concepts は ksn-concept モード3で扱うと明記されていますが、handbook の除外リストは単に「ゼロから書き直す」とされています。ksn-core の handbook 規約では、handbook の書き込みは ksn-concept / ksn-lesson / ksn-distill のいずれかを通す必要があります。

**推奨修正**: task 6.3 を ksn-concept の規律で実行することと、frontmatter・index・log・規範 lint の扱いを明記してください。

### [🟡 Minor] `SHALL NOT` が日本語の否定文と二重否定になっている

**該当箇所**: `specs/concept-placement/spec.md:7`、`specs/user-skills-manifest/spec.md:31`

**問題点**: 「持たない SHALL NOT」「変えない SHALL NOT」は、形式上は二重否定に読めます。

**推奨修正**: 「持ってはならない（SHALL NOT have）」「変更してはならない（SHALL NOT change）」のように、禁止対象を肯定形で記述してください。

## アクションプラン

1. platform 固有挙動と core 用語の所有境界を決定する。
2. KMP の特殊ケースを含む、全33 manifest target の完成形を確定する。
3. 移行 Skill の源泉拡大について ADR-0011 を維持するか改訂するか決める。
4. 識別子保存と他 platform 名排除を、platform 別 fixture と固定 baseline で検証できる仕様へ変更する。
5. handbook 経路と Requirement の二重否定を修正後、再度独立スペックレビューを行う。


## 突き合わせ結果 (2026-09-05)

ホスト側の自己レビュー (2 周) はいずれの指摘も検出していない。全件が相方のみの指摘で、根拠は実物 (concept の行番号・manifest・スクリプト) に基づくため採否は根拠で判定した。

| # | 指摘 | 採否 | 反映 |
|---|---|---|---|
| Major 1 | platform 固有の観察可能な挙動を置く層が未定義 | **採用 (設計判断: オーナー確認済み 2026-09-05)** | design Decision 2 を「platform 非依存 = 識別子で定義。承認済み差分の挙動は識別子なしで core に残す」に改訂 (相方案 1 に相当)。spec に Requirement 名の変更と Scenario「platform 差分の挙動の残存」、tasks 1.2 / 4.1b を追加 |
| Major 2 | 「4 形態同綴り」の許可リストが現行 API と矛盾 | **採用** | Decision 2 の一覧を「Skill 範囲すべての公開面に存在することをコードで確認した名前 (実装時に確定)」に変更し、`DialogOptions` / `LoadingStyle` / `ToastStyle` / `notifier` を例から外して散文扱いへ。代替案 D を追加 |
| Major 3 | KMP の manifest `targets` が矛盾し 33 キーを構成できない | **採用** | Decision 6 に 33 キーの完成形の表を置き、KMP の layout / transitions / view-models / android-host / ios-host を個別に確定。proposal の KMP iOS ホスト側の記述を修正 |
| Major 4 | 移行 Skill の源泉拡大が accepted ADR-0011 に反する | **採用 (設計判断: オーナー確認済み 2026-09-05)** | ADR-0011 の基準「対応表が触れるもの」を維持し、分割後は `maui/api/` 6 本すべてが基準に当たることを Decision 6 に根拠つきで明記 (代替案 C として字義どおり維持案を却下)。ADR-0014 に注記を追加。ADR-0011 本文は改訂しない |
| Major 5 | 識別子着地検査が保存を証明できず Requirement と Scenario が矛盾 | **採用** | Decision 5 を着地台帳方式 (baseline SHA・全識別子・期待する移動先・未説明 0 件) に改訂。spec Requirement を書き直し、小文字 API の Scenario を追加 |
| Major 6 | 「他 platform 名が消える」が偽陽性で通過できる | **採用** | Decision 7 (禁止トークン fixture の負の検査、concept 側 + Skill 側) を新設。「候補の総数」Scenario を削除し「仕分けの完了」に置換。tasks 1.3 / 4.3 / 6.1 |
| Minor 1 | handbook の書き込み経路が未明示 | **採用** | tasks 6.3 と api-listing-policy spec に ksn-concept の経路 (timestamp・index・log・構造 lint) を明記 |
| Minor 2 | SHALL NOT の二重否定 | **採用** | 「〜を禁止する (SHALL NOT)」の形に統一 |

未解決: なし (Major 1・4 は 2026-09-05 にオーナーが推奨案で確定)。相方への再提示は行っていない (矛盾ではなく採用のため)。
