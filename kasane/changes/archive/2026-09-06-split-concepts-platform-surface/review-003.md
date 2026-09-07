# レビュー結果: split-concepts-platform-surface (003 回目)

**日付**: 2026-09-05
**判定**: APPROVED
**範囲**: 再レビュー — 修正サイクル 2 の確認 (review-002 の Minor 1 / Minor 2、second-opinion-code-002 の新規 Major)。tasks.md グループ 5〜7 (manifest 書き換え・Skill 再生成・除外リストの組み直し・仕上げ) は未着手のままなので、未実装の指摘はしていない

## サマリー

**指摘 3 件はすべて解消**していた。kmp 禁止集合の再分類 (152 → 136) は自前で独立に再導出して 1 件ずつ一致を確認し、外した 14 件の到達可能性も `ios/Sources/KsDialogs/Kmp/` の実物で裏を取った (baseline.md が挙げる確認先の行番号はすべて実測と一致)。境界事例 3 件を禁止に残した判断も doc comment の実文で妥当と確認した。`concepts/log.md` の構造 lint 件数の訂正も、lint を掛け直して事実と一致していた。

新規の指摘は 2 件で、いずれも Minor、いずれも**「集合の中身」ではなく「突き合わせの単位」と「オーナー判断との接続」**に関わる。禁止トークンの検査が何を 1 トークンと数えるか (バッククォート識別子か、spec Scenario が書く「ファイル全文」か) が成果物のどこにも書かれておらず、後者で数えると kmp では許可面 concept のコード例が `Dialog.shared.registry` に当たる。また外した 16 件のうち 3 件は `kasane/handbook/cross/user-skill-api-listing.md` にオーナー確定済みの除外行として載っており、その理由を覆す形になっている。どちらも task 6.1 に入る前に決めれば済み、修正サイクル 2 のやり直しは要らない。

## 照合した規約

domains 定義プロジェクトのため cross + 作業ドメイン (cross) の handbook / concepts index を対象にした。`always` の handbook 文書は無い (`kasane/handbook/index.md`: core / ios / android / maui / kmp はまだ規約なし、cross 7 件)。

| 規約・文書 | 適用のきっかけ |
|---|---|
| `kasane/handbook/cross/user-skill-api-listing.md` | 禁止トークン fixture の初期値の由来 (現行除外リスト)。今回は**除外基準の定義と KMP 行を節ごとに照合**した (`:22-32` 基準、`:34-36` 現行除外リストの位置づけ、`:70-124` KMP 行) |
| ksn-core `references/delta-spec.md` | 足場凍結・deviation = 合意済み差分 |
| ksn-core `references/doc-structure.md` | log.md が報告する構造 lint の適用対象と閾値 |
| ksn-core `references/paths.md` | 成果物に書くパスの形式 |
| ksn-core `references/concepts.md` | 長命層 (rules.md・log.md) の記述規約 |
| `kasane/concepts/rules.md` | 「KMP の公開面 = 3 側」の定義 (`:66`)。今回の許可面の定義と突き合わせた |
| `kasane/decisions/cross/0014` (proposed) | 分割の原則。proposed のため判定の根拠にはしていない |

`kasane/lessons/code-review.md` は不在 (lessons/ にあるのは `impl.md` / `process.md` / `spec-review.md` / `inbox/` / `details/`)。config.yaml の `skills.code-review` は空、cross に `domain-skills` の割り当てなし。

## 前回指摘の解消状況

`R2` = review-002、`SO2` = second-opinion-code-002 (末尾「突き合わせ結果」が採否)。

| # | 指摘 | 状態 | 確認したこと |
|---|---|---|---|
| SO2 Major (新規) | kmp 禁止集合に KMP 自身の Swift 入口から到達する名前 (`DialogError` / `cancelled` / `presentationHostUnavailable` / `viewFactoryNotRegistered`) が入っている | **解消** | 4 件とも `tokens` から消えている。到達可能性を実装で確認 — `DialogError` は KMP show の doc が明示 (`ios/Sources/KsDialogs/Kmp/KsDialogsKmp.swift:185`、`KsLoadingKmp.swift:67`、`KsToastKmp.swift:64`)、`cancelled` は戻り値 `DialogResult` の case (`KsDialogsKmp.swift:197`・`:214-215`)、`presentationHostUnavailable` は理由欠落時の既定 (`KsDialogsKmpError.swift:30`・`:34`)、`viewFactoryNotRegistered` は KMP の Loading / Toast 入口が実際に throw (`KmpLoadingViewModel.swift:33`・`:42`、`KmpToastViewModel.swift:26`・`:30`)。**指摘の 4 件を超えて基準を一般化した点も適切** — `KsDialogsKmpError.publicError(from:)` が素通しする残りの case、doc が設定先として案内する `Loading.shared.style` / `.options` / `Toast.shared.style`、`.kmp` の取り出し元 `Dialog.shared` / `Loading.shared` / `Toast.shared` まで同じ基準で拾えている |
| R2 Minor 1 | kmp 禁止集合に許可面の中にある 4 トークンが残っている (`KsDialogAttributes` / `SimpleDialogViewModel` / `ksDialogOptions` / `ksDialogPlacement`) | **解消** | 4 件とも消えている。`KsDialogAttributes` は `android/api/{dialog,layout,transition}-surface.md`、`SimpleDialogViewModel` は `android/api/dialog-surface.md` にのみ出現することを確認 (どちらも Decision 6 の `targets` 上で `ksdialogs-kmp/references/android-host.md` の源泉)。`concept-exempt` ではなく削除で処理されており、区分の使い分けも正しい (exempt は maui の 3 件のまま) |
| R2 Minor 2 | `concepts/log.md` の構造 lint 件数の記述が事実と食い違う | **解消** | `kasane/concepts/log.md:40` が「残る 11 件 — `maui/api/di-registration.md` 10 (`:13`-`:56` の既存本文。本 change の変更は末尾の関連節の追加だけ) / `cross/reference/reference-repositories.md` 1 (`:29`。本 change は未変更) — は既存本文からの持ち越し」に直っている。`scripts/doc-structure-lint.py` を scope モードで掛け直して **11 件 (di-registration 10 + reference-repositories 1)** を実測、違反行も `:13` `:14` `:15` `:33` `:35` `:36` `:43` `:44` `:54` `:56` で記述どおり。`git diff --stat` は di-registration.md が +10 行のみ (`## 関連` 配下への h3 2 つとリンク 5 本) で、「変更は末尾の関連節の追加だけ」も事実 |
| R2 Suggestion | doc-structure lint の掛け方 (scope / `--paths`) を tasks 4.4・7.2 に明記 | **未反映 (合意どおり)** | tasks.md `:37` (4.4) / `:56` (7.2) は変更なし。SO2 の突き合わせ結果が「標準装備側の課題として完了報告で扱う」と決めているので、指摘としては扱わない |

**未解消: 0 件。**

## 再実行した機械検査 (すべて成果物の報告と一致)

| 検査 | 結果 |
|---|---|
| `verification/identifier-landing.py check` | 台帳 108 行: 着地 89 / 意図して落とした 19 / 未説明 0 (exit 0) |
| 禁止トークン負の検査 (concept 側、自前で再実装) | ios 200 / android 178 / maui 154 (exempt 3) / kmp 136 / aiforms-migration 151 — 5 範囲すべて一致 0 件 |
| `verification/forbidden-tokens.json` の件数と `source` | kmp のみ 136 件・`source` に `kmp-targets-narrowing + kmp-entry-reachability` が加わり、他 4 範囲は件数・source とも不変 (baseline.md の記述どおり) |
| `scripts/doc-structure-lint.py` (scope モード) | `kasane/concepts` の違反は `maui/api/di-registration.md` 10 + `cross/reference/reference-repositories.md` 1 の 11 件のみ |
| `scripts/local-path-lint.py` / `identity-lint.py` | ともに exit 0 |
| concepts 内の相対リンク解決 | 全 39 ファイル走査・未解決 0 件 |
| `git diff --check` | 指摘なし |
| 足場の凍結 | `proposal.md` / `design.md` / `specs/` は未変更 (`git status` の modified に無い)。`tasks.md` の差分 22 行はチェックボックスの反転のみで本文の書き換え無し。グループ 5〜7 は未チェック |

### kmp 禁止集合の独立再導出 (136 件)

design.md Decision 6 の `targets` から許可面 (`kmp/api/*` 4 本 + `android/api/*` 5 本 + `ios/api/layout-surface.md` + `ios/api/transition-surface.md` = 11 本) と他 platform 面 (`maui/api/*` 6 本 + `ios/api/{dialog,loading,toast}-surface.md` = 9 本) を自分で組み、`identifier-landing.py` と同じ抽出規則 (`TOKEN` / `STOP`) で差集合を取った。

- 「他 platform 面 − 許可面」= **146 件**。`forbidden-tokens.json` の kmp 136 件と突き合わせると、**導出に出て集合に無いのはちょうど 15 件** — 言語・標準ライブラリの `AnyObject` / `Sendable` / `Result` 3 件と、到達可能として外した 12 件 (`Dialog.shared` / `Loading.shared` / `Toast.shared` / `Loading.shared.style` / `Loading.shared.options` / `Toast.shared.style` / `DialogError` / `cancelled` / `presentationHostUnavailable` / `viewFactoryNotRegistered` / `viewModelAlreadyShowing` / `viewModelFactoryNotRegistered`)。外した 14 件のうち残る `ksDialogOptions` / `ksDialogPlacement` は許可面側にあるため元から差集合に出ない。**取りこぼし・余剰はゼロ**
- **集合にあって導出に無いのは非 API トークン 5 件だけ** (`A.min` / `C05` / `C19` / `approvedBy` / `approvedDiff`)。修正サイクル 1 まで残っていた旧構成由来の 4 件は消えている
- **内訳の検算も一致**: iOS Native 固有 8 件 (`Dialog.shared.registry` / `Loading.shared.registry` / `Toast.shared.registry` / `LoadingStyle.ProgressFormat` / `LoadingStyle.defaultProgressFormat` / `ToastStyle.builtinBackgroundColor` / `ToastStyle.builtinDefaultDuration` / `UIHostingController`) + MAUI 固有 123 件 + 非 API 5 件 = 136 件で、3 つの区分は互いに素、どこにも属さないトークンは 0 件
- **バッククォート識別子での積集合は 0 件** — 許可面 11 本の識別子との積集合が空であることを確認 (R2 Minor 1 が求めた不変条件。ただし下記 Minor 1 のとおり、コード例まで数えると成立しない)

### 到達可能性の裏取り (baseline.md が挙げる確認先の実測)

`verification/baseline.md:141` の基準 4 つを、外した 14 件のうち 9 件について実物で確認した。**引用された行番号はすべて実測と一致**する。

| トークン | 基準 | 実測 |
|---|---|---|
| `Dialog.shared` | 1 (公開署名・入口) | `KsDialogsKmp.swift:13`・`:16` (doc のコード例)・`:22` (`Dialog.shared.kmp` の取り出し) |
| `Loading.shared.style` / `Loading.shared.options` | 4 (doc の案内) | `KsLoadingKmp.swift:17-18`「見た目のスタイルと既定ローディングの器メタ属性は Native の入口 (…) で設定する」 |
| `Toast.shared.style` | 4 | `KsToastKmp.swift:17` |
| `DialogError` | 3 (throw する失敗型) | `KsDialogsKmp.swift:185`、`KsLoadingKmp.swift:67`、`KsToastKmp.swift:64` |
| `cancelled` | 2 (戻り値の case) | `KsDialogsKmp.swift:197`・`:214-215` |
| `viewFactoryNotRegistered` | 3 | `KmpLoadingViewModel.swift:33`・`:42`、`KmpToastViewModel.swift:26`・`:30` (KMP 入口の `show` が呼ぶ `*ContentRequest.kmp` が実際に throw) |
| `presentationHostUnavailable` / `viewModelAlreadyShowing` / `viewModelFactoryNotRegistered` | 3 (素通し) | `KsDialogsKmpError.swift:26-34` の `publicError(from:)` が写し替えずに返す |
| `ksDialogOptions` / `ksDialogPlacement` | 1 (添付 API) | `KsDialogsKmp.swift:62`、`KsLoadingKmp.swift:49`、`KsToastKmp.swift:48` |

**外し過ぎは無い**。14 件はいずれも KMP 利用者が Swift ホスト側で通る経路にあり、他 platform 固有名 (MAUI 系・Android 固有の綴り) を許可した形跡は無い。除去は kmp 範囲だけに閉じており、同じ名前は android / maui / aiforms-migration の各集合に残っている (`Dialog.shared` `DialogError` `cancelled` ほか 12 件を確認)。

**残った 136 件に KMP の Swift 向け公開面から到達可能な名前は残っていない**。`ios/Sources/KsDialogs/Kmp/*.swift` 全 8 本の本文と doc を 136 件で走査した結果、当たったのは境界事例として意図的に残した `Loading.shared.registry` / `Toast.shared.registry` (どちらも doc の対比のみ) と、散文中の語 `Task` だけである。`Loading.shared.hide()` / `Dialog.shared.kmp` / `dialogTransition` / `ToastStyle` / `DialogResult` / `completed` / `KsDialogsKmpError` / `notRegistered` / `resultTypeMismatch` / `viewFactoryTypeMismatch` / `viewModelFactoryTypeMismatch` / `UIView` はいずれも集合外で、素通し・戻り値・添付の各系統に取りこぼしは無い。

### 境界事例 3 件を残した判断

**妥当**。`KsLoadingKmp.swift:8` / `KsToastKmp.swift:8` は「Native の登録面 (`Loading.shared.registry`) では受けられない」という**否定の対比**としてのみ挙げており、KMP 利用者が通る経路ではない。`Dialog.shared.registry` は `ios/Sources/KsDialogs/Kmp/` に一度も現れない。`kasane/handbook/cross/user-skill-api-listing.md:30` の基準「対象 platform の実装経路を持たない名前に限る」にも合致する。`LoadingStyle.*` / `ToastStyle.*` 4 件・`UIHostingController` も Kmp/ の doc・公開署名に現れないことを走査で確認した (`ToastStyle` が `KsToastKmp.swift:67` に出るが集合外、という但し書きも実測どおり)。

## 指摘事項

### [🟡 Minor] 禁止トークンの突き合わせ単位が未定義で、`Dialog.shared.registry` は許可面 concept のコード例に出る

**該当箇所**: `verification/forbidden-tokens-concepts.txt:3-4`、`verification/baseline.md:183-187`・`:204-207`、`specs/user-skills-manifest/spec.md:49-53`

**問題点**:

concept 側の検査は「バッククォート識別子を完全一致で突き合わせる」と定義されている (`forbidden-tokens-concepts.txt:3-4`)。一方 spec の Scenario「Skill 本文への負の検査」は「各 Skill 範囲の**ファイル全文**を当該集合と突き合わせる → 一致が 0 件」で、コードフェンスも地の文も含む読み方になる。**どちらで数えるかがどこにも書かれておらず、結果が大きく変わる**。

現行の Skill (再生成前) で実測すると、単位の違いはそのまま件数の違いになる:

| 範囲 | インラインのバッククォート識別子 | ファイル全文の語一致 |
|---|---|---|
| kmp | 0 件 | 5 件 (`Register` / `Show` / `Start` / `double` / `with`) |
| ios | 2 件 | 12 件 |
| android | 2 件 | 10 件 |
| maui | 0 件 | 20 件 |
| aiforms-migration | 2 件 | 19 件 |

全文の読み方で当たる大半は `with` / `Show` / `Start` / `options` / `instance` / `shared` / `presentation` / `transition` のような**英文の語や casing 違い**で、これは fixture の初期値 (現行除外リスト由来) が短い一般語を含むことによる。baseline.md は「部分一致にしない」までは書いているが (`verification/baseline.md:75-77`)、「語として現れた場合」の扱いは決めていない。

kmp ではこれが**実害の形をとる**。禁止に残した `Dialog.shared.registry` は、許可面 concept である `kasane/concepts/ios/api/layout-surface.md:72` と `kasane/concepts/ios/api/transition-surface.md:71` の Swift コード例 (`Dialog.shared.registry.register(ConfirmViewModel.self) { … }`) に出現する。この 2 本は Decision 6 の `targets` で `ksdialogs-kmp/references/layout.md` / `transitions.md` の源泉になっており、docs-refresh はコードブロックを源泉から運ぶ (整合性チェックに code-block parity がある)。**つまり task 6.1 で、許可面から正当に運ばれたコード例が違反として報告される可能性が高い**。しかも `verification/baseline.md:187` の手当ては「『対比としての言及』か『登録先としての案内』かを本文で見分ける」で、この例は後者に読めてしまい、正当な例を削る方向へ誘導する — SO2 Major が指摘したのと同じ失敗の形である。`verification/baseline.md:204-207` の「許可面の識別子との積集合は 0 件」も、バッククォート識別子でのみ成立する。

**推奨修正**: task 6.1 に入る前に、baseline.md に**突き合わせの単位**を書く。(a) 識別子表記 (インラインのバッククォートとコードフェンス内のトークン) に限る、(b) ファイル全文の語一致、のどちらかを選び、(b) を採るなら一般語トークンの扱い (集合から落とす / 既知の偽陽性として仕分け対象にする) も併記する。あわせて `Dialog.shared.registry` は「許可面 concept のコード例に出る」ことを baseline.md の境界事例の節に追記し、task 6.1 でこの名前が出たら**まず出典が許可面かどうかを見る**手当てにする (禁止から外すかは単位の決め方しだい)。

### [🟡 Minor] 外した 3 件は handbook の現行除外リストのオーナー確定行を覆している

**該当箇所**: `verification/baseline.md:156`・`:162-163`、`kasane/handbook/cross/user-skill-api-listing.md:73`・`:75`・`:120`

**問題点**:

外した 16 件のうち 7 件は `kasane/handbook/cross/user-skill-api-listing.md` の「現行の除外リスト」に KMP 行として載っている。同文書 `:36` はこの表を「オーナーが仕分けた確定済みの除外」と位置づけ、`:32` は「具体的な API 名を新たに除外するときは、実装上の利用経路と該当基準を示してオーナー判断を得る」と定めている (除外を**外す**方向も対称に判断が要る)。

7 件のうち 4 件 (`KsDialogAttributes` `:116` / `SimpleDialogViewModel` `:115` / `ksDialogOptions` `ksDialogPlacement` `:118`) は、Decision 6 で KMP Skill の源泉に `android/api/` 5 本と `ios/api/layout-surface.md` が入った結果として行が古くなったもので、**design が承認済みの構成変更の帰結**である (Migration Plan 7 の除外リスト組み直しで解消される)。問題は残る 3 件:

| トークン | handbook の行 | 基準 | オーナーが書いた理由 |
|---|---|---|---|
| `Loading.shared.options` | `:120` | 対象 Skill 外・機械検査由来 | iOS Native の singleton / host 設定名であり、この対象 Skill の正規公開面には存在しない |
| `viewModelAlreadyShowing` | `:73` | **内部層・interop 層** | Swift Native の詳細 enum case は KMP 公開面へ露出しない |
| `viewModelFactoryNotRegistered` | `:75` | **内部層・interop 層** | 同上 |

後ろ 2 件の基準は「対象 Skill 外」ではなく**「内部層・interop 層」** — 「どの platform の名前か」ではなく「利用者に見せる層か」というオーナーの層判断である。今回の到達可能性の基準 3 (「素通しするものを含む」) はこの層判断に答えていない (Native の失敗 case が素通しで届くことと、KMP Skill にその case 名を載せるべきかは別の問い)。

しかもこの 3 件は**外しても得るものが無い**。3 件とも許可面 concept に 1 件も出現せず、`kasane/concepts/kmp/api/ios-host-integration.md` にも出現しない (実測 0 件)。docs-refresh は源泉の閉世界で生成するので、再生成 Skill に正当に現れる経路が無い。したがって除去は偽陽性の予防にならず、**オーナー確定済みの検出だけが静かに外れる**。

**推奨修正**: この 3 件を kmp の `tokens` に戻す。到達可能性の基準のほうが層判断より優先すると考えるなら、fixture を先に動かすのではなく、**`verification/baseline.md` に「handbook `:73` / `:75` / `:120` のオーナー確定行と食い違う」と明記して task 7 の除外リスト組み直し (Decision 4・Migration Plan 7、handbook への書き込みは ksn-concept の経路) の判断材料に送る**。どちらを採るにせよ、覆した事実がどこにも残っていない現状は避けたい。

### [🔵 Suggestion] 許可面の定義が rules.md (長命層) と baseline.md (短命層) でずれている

**該当箇所**: `verification/baseline.md:99-101`・`:135-140`、`kasane/concepts/rules.md:66`

**問題点**: 本 change が長命層に書いた定義は「KMP の公開面 = commonMain + Android ホスト側 + Swift 向け公開面 (**`kmp/api/ios-host-integration.md` が扱う面**)」で、Swift 側を**概念文書**に錨を打っている。一方 baseline.md の許可面は「targets の和集合 + **`ios/Sources/KsDialogs/Kmp/` から到達可能な名前**」で、実装ディレクトリに錨を打つ。外した 14 件のうち 11 件は `kmp/api/ios-host-integration.md` に出現しない (実測: 出現するのは `Dialog.shared` / `Loading.shared` / `Toast.shared` の 3 件のみ) ため、2 つの定義は実際に別の集合を指す。

fixture は change と共に archive されるが `rules.md` は残るので、後続の書き手が「KMP の Swift 向け公開面」を再導出すると今回と違う答えになる。なお、これは概念側の記載漏れの疑いも同時に示している — `kmp/api/ios-host-integration.md` は失敗型・結果 case・SwiftUI 登録の添付 API を散文でしか触れておらず、KMP 利用者が Swift 側で捕まえる型の綴りが載っていない (指摘ではなく所見。当該 concept は review-001 / 002 で確認済みの範囲)。

**推奨修正**: baseline.md の再分類節に、rules.md `:66` の定義との対応 (実装到達可能性は同じ面を実装側から見たものである旨) を 1 行足す。concept 側の記載の要否は task 6.x 以降か別 change で扱えばよい。

## 判断材料として書いておくこと (指摘ではない)

- **修正サイクル 2 の書き込みは範囲に収まっている**。tracked 側の変更は concepts 16 本 + tasks.md、untracked 側は新設 concept 18 本と change 配下の deviation / review / second-opinion / verification のみ。コード・スクリプト・handbook・decisions への書き込みは無く、`verification/__pycache__/` は `.gitignore:71` で除外済み
- **deviation.md への追記は不要と判断した**。fixture の再分類は spec Requirement 本文 (`specs/user-skills-manifest/spec.md:47`) の「自 platform の公開面に無いもの」の解釈を精緻化したもので、`kasane/concepts/rules.md:66` が定義する KMP の 3 側の公開面と整合する。handbook `:30` の基準「対象 platform の実装経路を持たない名前に限る」にも沿う方向の修正であり、spec からの逸脱ではない (ただし上記 Minor 2 の 3 件は、この整合の外にある)
- **修正サイクル 2 が新しい主張を持ち込んでいないことを確認した**。concepts への書き込みは `log.md` の 1 行のみ (append-only の追記行の中の件数記述の訂正) で、他の concept 本文は触られていない (相対リンク 39 ファイル未解決 0 件・doc-structure lint の残存 11 件とも review-002 時点と同値)
- **SO2 が「同じ理由で追加した case 名も再確認せよ」と書いた点は正しく展開されている**。指摘された 4 件に留めず、素通し系・doc 案内系・入口取り出し系まで基準を通して 14 件にしたのは過不足の無い一般化だった (逆方向の取りこぼしが無いことは 136 件 × Kmp/*.swift の走査で確認)

## アクションプラン

1. **[Minor]** 禁止トークンの突き合わせ単位を `verification/baseline.md` に明記する (識別子表記に限るか、全文の語一致か)。全文を採るなら一般語トークンの扱いを併記し、`Dialog.shared.registry` が許可面 concept のコード例 (`kasane/concepts/ios/api/layout-surface.md:72`・`transition-surface.md:71`) に出ることを境界事例の節に追記する。**task 6.1 に入る前に**
2. **[Minor]** `Loading.shared.options` / `viewModelAlreadyShowing` / `viewModelFactoryNotRegistered` を kmp の `tokens` に戻すか、handbook `kasane/handbook/cross/user-skill-api-listing.md:73`・`:75`・`:120` のオーナー確定行と食い違う事実を baseline.md に明記して task 7 の組み直しへ送る。**task 6.1 に入る前に**
3. **[Suggestion]** baseline.md の許可面の定義に、`kasane/concepts/rules.md:66` の「KMP の公開面 = 3 側」との対応を 1 行足す

1 と 2 はどちらも fixture と baseline.md の中で完結し、公開名は動かないので着地台帳・禁止トークンの再採取は不要 (2 で `tokens` を戻す場合は concept 側の負の検査だけ再実行すれば足りる)。
