# Design: split-concepts-platform-surface

## Context

concepts の `core/api/` 8 本 (約 1,300 行) は、4 形態のダイアログ契約を 1 本文で書き、形態別テーブル (`| 形態 | ... |`) で公開名・署名・注意を並べている。利用者向け Skills は platform 単位 (cross/ADR-0011) なので、docs-refresh の API 名網羅検査は各 Skill に対して core の全形態の識別子を候補にし、除外リストが約 100 行に肥大した。`rules.md` は `<platform>/api/` (platform 固有の公開 API・利用例) をカテゴリとして持つが、`ios/`・`android/` に concept は無く、`maui/api/` 1 本 (`di-registration.md`)・`kmp/api/` 1 本 (`ios-host-integration.md`) だけが platform 側にある。

分割の原則は探索で決めた ([cross/ADR-0014](../../decisions/cross/0014-concepts-core-contract-platform-surface.md)、proposed): core = platform 非依存の契約、`<platform>/api/` = 公開面。検査スクリプトは KsSettingsView と同一のまま変えない。本 design はその原則を実行可能な単位まで落とす。

現状の Skill 構成 (manifest `targets`): 各 platform Skill は `SKILL.md` + `references/{dialogs,layout,loading,toast,transitions,view-models}.md` (MAUI は + `di-registration.md`、KMP は + `android-host.md` / `ios-host.md`)、移行 Skill は `SKILL.md` + `references/api-mapping.md`。

## Goals / Non-Goals

Goals: (1) API 名網羅検査の候補から他 platform の公開名が消える構造にする (承認済みの platform 差分の挙動は core に残す)。(2) 分割の前後で公開名を 1 つも落とさない。(3) Skill の構成を変えずに再生成で追従させる。(4) 除外リストを掲載可否の実判断だけに縮める。

Non-Goals: proposal.md の Non-Goals のとおり (検査器の改修・Skill 構成の変更・挙動の意味改訂・除外基準 5 つの改訂・KsSettingsView への逆流・生成物の品質改善)。

## Decisions

### Decision 1: 分割単位は「platform × 機能」で、Skill の references と 1:1 に対応させる

**採用案:** 各 platform ドメインの `api/` に、機能単位の公開面 concept を置く。ファイル名は 4 platform で揃える:

| concept | 対応する core の契約 | 対応する Skill references |
|---|---|---|
| `<platform>/api/dialog-surface.md` | registration-show / result-notification / multi-display / model-binding | `dialogs.md` + `view-models.md` |
| `<platform>/api/layout-surface.md` | layout | `layout.md` |
| `<platform>/api/transition-surface.md` | transition | `transitions.md` |
| `<platform>/api/loading-surface.md` | loading | `loading.md` |
| `<platform>/api/toast-surface.md` | toast | `toast.md` |

- iOS / Android / MAUI は 5 本ずつ新設。MAUI の既存 `di-registration.md` はそのまま (dialog-surface から参照)
- KMP は 3 本 (`dialog-surface` / `loading-surface` / `toast-surface`) を新設。commonMain には layout / transition の添付の面が無く (各 OS 側の View に対して Native の面で添付する)、その旨は `dialog-surface.md` の 1 節で書いて `ios/api/` `android/api/` の該当 concept へリンクする。既存 `ios-host-integration.md` はそのまま
- **KMP の Swift 向け公開面** (`Dialog.shared.kmp` / `Loading.shared.kmp` / `Toast.shared.kmp` の型付き登録入口・`result:` ラベルの結果型指定・`UIView` / SwiftUI factory のオーバーロード) は、既に「Swift 側で登録するもの」節を持つ `kmp/api/ios-host-integration.md` に集約する (core の形態別テーブルの「KMP (Swift 向け公開面)」行をそこへ移す)。コード自体は `ios/` に閉じるが、この面の読者は KMP 利用者であり rollout-user-docs で kmp ドメインに置いた既存 concept を割らないことを優先する。KMP Skill の `references/ios-host.md` の源泉は従来どおりこの 1 本
- 各 platform concept の書き方: 冒頭で対応する core 契約へリンクし、「この文書は `<platform>` の公開面 (名前・署名・コード例・framework 固有の注意) だけを扱い、挙動の契約は core が正」と宣言する。中身は core から外した形態別テーブルの自 platform 行・コード例・注意を、core の節順に並べ直したもの

**理由:** Skill の references と 1:1 に対応させると manifest の `targets` が「core の契約 N 本 + 自 platform の同名 concept 1 本」という機械的な形になり、追従の逆引きが読みやすい。機能単位なら 1 本あたりの分量が doc-structure lint の警告閾値 (散文 10,000 字) に収まる。KsSettingsView の platform concept も機能・面単位 (`ios-swiftui.md` / `maui-cells.md` / `maui-styling.md` 等) で、粒度の先例に合う。

**代替案:**
- **A: platform ごとに 1 本 (`ios/api/public-surface.md`)** — core 8 本分の公開面が 1 本に集まり、iOS / Android で散文 10,000 字を超える見込み (現 core の layout だけで 13,700 字)。Skill の references 6 本すべてが同じ 1 concept を源泉にし、どの references を再生成すべきかの逆引きが効かなくなる。却下
- **B: platform ごとに 2 本 (dialog + layout + transition / loading + toast)** — A よりは小さいが、layout と transition は表・図が多く 1 本にすると lint 閾値に近い。references との対応も 1:1 にならない。却下
- **C: 機能ごとに 1 本で内部を platform 節に分ける (`core/api/layout-surfaces.md`)** — 4 platform の名前が再び 1 本文に同居し、検査候補のノイズが戻る。却下

### Decision 2: core に残す識別子は「4 つの Skill 範囲すべての公開面に存在する共通概念名」だけとし、platform 差分の挙動は識別子なしの散文で core に残す

**採用案:** 「platform 非依存」の意味を**識別子**で定義する。core/api の本文は platform 固有の識別子 (既定シングルトン `Dialog.shared` / `Dialog.instance` / `Dialog.Instance`、形態別の別名 `SimpleDialogViewModel` / `IDialogViewModel`、添付の面 `ksDialogOptions` / `KsDialogAttributes` / `ksd:Dialog.*`、例外の詳細型 `ValueClassViewModel` / `ValueTypeViewModel`、framework 型 `UIView` / `LazyColumn` / `Bool` / `TimeSpan`、コード例) を持たない。バッククォート表記してよいのは**共通概念名**に限る — 4 つの Skill 範囲 (iOS / Android / MAUI / KMP の 3 側) すべての公開面に同綴り (MAUI の interface 接頭辞 `I` は同綴り扱い。大文字小文字の違いは別綴り) で存在することを実装コードで確認した名前で、確定一覧は実装時に `verification/core-contract-check.md` に記録する。候補は `DialogPlacement` / `DialogTransition` / `DialogViewModel` / `DialogException` / `KsDialogs` / `KsLoading` / `KsToast` / `LayoutArea` など。条件を満たさない用語 (KMP commonMain が公開しない `DialogOptions` / `LoadingStyle`、C# で `Notifier` と綴る notifier など) は core ではバッククォートを付けずに散文で書き (「結果報告口 (notifier)」)、識別子としての綴りは platform 側に置く。

一方、**承認済みの platform 差分の挙動** (下段を先に閉じたとき iOS は上段も消えて cancelled になり Android は上段が残る / 呼び出し元キャンセルは Swift が結果として cancelled を返し Kotlin はコルーチン規約でキャンセル例外が伝播し MAUI にはこの経路がない / Android の戻るボタン / leading・trailing のレイアウト方向追随 など) は利用者が観察する契約なので **core に残す**。ただし識別子を含まない散文か、識別子を含まない表で書く (「形態 | 呼び出し元が観察するもの」の表は残してよいが、セルに `.cancelled` / `CancellationException` を書かず「結果として cancelled」「コルーチンのキャンセル例外」と書く)。識別子を伴う具体形 (`.cancelled` / `CancellationException` / `Func<VisualElement, Task>`) は platform concept の対応節に置き、core の差分記述からリンクする。core の本文が「形態ごとに公開面が違う」ことを述べる箇所は「各形態の公開面を参照」と書き、末尾に「形態別の公開面」節を置いて 4 platform の同名 concept へリンクする。

**理由:** ノイズの原因は識別子であって platform への言及ではない (API 名網羅検査はバッククォート識別子しか抽出しない)。platform 差分の挙動を platform 側へ出すと「挙動の契約は core が正」が崩れ、1 挙動の契約が 5 文書に散る。逆に共通概念名は公開契約そのものであり、Skill がその名前を一度も載せないなら本当の掲載漏れなので検査に残す価値がある。「全形態に存在」の条件を実装コードで確認するのは、用語節の名前が特定 platform (たとえば KMP commonMain) に無いまま core に残ると、その Skill だけ毎回候補に出るため。

**代替案:**
- **A: core からバッククォート表記を全廃し、識別子は platform 側だけに置く** — 共通概念名の掲載漏れ (例: Skill が `DialogPlacement` に一度も触れない) を検査できなくなる。core の用語節がコード上の名前と結びつかず、契約の読み手が名前を引けない。却下
- **B: 共通概念名に加えて既定シングルトンの 4 綴りも core に残す** — 「既定エントリはシングルトン」という契約は共通だが綴りは 4 種で、必ず 3 形態分のノイズになる。契約は文で書けば足りる。却下
- **C: platform 差分の挙動も platform concept に移し、core は共通部分だけを持つ** — 「挙動の契約は core が正」が崩れ、差分の有無を知るのに 4 platform を読み比べることになる。core の Non-Goal「意味を改訂しない」も保証しにくい。却下
- **D: core の用語は同綴り条件を外して「概念名」として無条件に残す** — KMP commonMain に無い `DialogOptions` などが KMP Skill の候補に毎回出て、除外リストに戻る。却下

### Decision 3: 検証機構の記述 (共通ケース表と OS 差の統制) は `core/architecture/` へ移し、Skill の源泉から外す

**採用案:** `core/api/layout-semantics.md` の「共通ケース表と OS 差の統制」節 (cases.json の形・ケース ID `C05` `C19`・`approvedDiff` / `approvedBy` / `reason` の統制) を `core/architecture/layout-case-table.md` として独立させる。これは利用者が使う契約ではなく、Native 2 実装のレイアウト検証機構の記述である。cross/ADR-0011 の既定 (`architecture/` カテゴリは除外候補) に従い manifest の `excluded` に理由つきで載せる。core/api の layout 契約は「規則と対になる共通ケース表を単一の正として持つ」の 1 文と当該 concept へのリンクだけを残す。rect 決定手順の数式 (`A.min` / `A.max − s` 等) はインライン code ではなく fenced code block で書き、識別子として拾われないようにする。

**理由:** 非 API token の大半がこの 1 節に由来する (`C05` / `C19` / `approvedBy` / `approvedDiff`)。検証機構の記述は `rules.md` の architecture カテゴリ (共通 architecture) に当てはまり、利用者向け Skill の源泉になる必要がない。数式は API 名ではないので code block 化は表現の是正であって検査逃れではない。

**代替案:**
- **A: 節を core/api に残し、除外リストの「機械検査由来」1 行で吸収する** — ノイズは毎回報告に出続け、layout 契約が検証機構の説明を抱えたままになる (散文 13,700 字の主因の一つ)。却下
- **B: 検査スクリプトにケース ID のパターン除外を足す** — 検査器を変えない決定 (cross/ADR-0014) に反する。却下

残る非 API token (`build.gradle.kts` / 例示型名 `ConfirmContent` `SharedConfirmViewModel` / `MauiProgram` / `localSwiftPackage`) は数が少なく platform concept 側に残るため、分割後の検査報告で従来どおりオーナーが仕分ける (除外リストに残ってよい)。

### Decision 4: 除外リストは分割後の検査報告から組み直す (差分編集しない)

**採用案:** 分割と Skill 再生成の後に API 名網羅検査を実行し、その報告を出発点に `handbook/cross/user-skill-api-listing.md` の現行除外リストを**ゼロから書き直す**。現行リストのうち「内部層・interop 層」「低頻度の細部 API」「機械的に導出できる名前」「可視性引き下げ候補」の行は、分割後も報告されるなら理由をそのまま引き継ぐ (再判断しない)。報告されなくなった行は落とす。「対象 Skill 外・機械検査由来」の行は原則すべて落ち、残るとすれば Decision 3 末尾の非 API token だけになる。新たに報告された名前 (分割で platform 側に着地した公開名が Skill に未掲載のもの) は掲載漏れとして Skill 側を直すか、オーナー判断で除外に加える。

**理由:** 現行リストの 9 割は分割で意味を失う行なので、行ごとの差分編集より書き直しのほうが漏れが少ない。実判断の行の理由を引き継ぐことで、オーナーの過去の判断を捨てない。

**代替案:**
- **A: 現行リストを残し、報告されなくなった行だけ削る** — 約 90 行を 1 行ずつ照合する作業で、消し漏れと理由の陳腐化 (「Android Native の例外型で、Swift 公開面には存在しない」の前提が core から消える) が起きやすい。却下
- **B: 除外リストを空に戻して全件をオーナーに再仕分けしてもらう** — 実判断 10 行の再判断はオーナーの時間の無駄。却下

### Decision 5: 公開名の取りこぼしは、着地台帳 (識別子 × 期待する移動先) の突き合わせで機械的に検出する

**採用案:** 着手時に baseline のコミット SHA を `verification/baseline.md` に記録し、その時点の `core/api/*.md` からバッククォート識別子を**すべて** (API 名網羅検査の `looks_api` で絞らず、STOP 語と数値だけを除く。`show` / `hide` / `cancelled` / `leading` などの小文字名を含む) 抽出して着地台帳 `verification/identifier-ledger.md` を作る。台帳の各行は「識別子 / 出典 concept と節 / 期待する移動先 (core = 共通概念名として残す / ios / android / maui / kmp / architecture のいずれか、複数可) / 判定」を持ち、移動先は形態別テーブルの行や節の platform 名から機械的に初期値を付け、実装者が確認する。分割後、各識別子が期待する移動先の concept に実際に出現することをスクリプト `verification/identifier-landing.py` で検査し、判定は「期待どおり着地」か「意図して落とした (理由つき)」の二択で、**未説明の差分は 0 件**を受け入れ条件とする。他 platform の concept や `excluded` の architecture concept への着地は「着地」と数えない。

**理由:** 1,300 行の書き直しで名前を 1 つ落としても人間のレビューでは気づきにくい。「どこかの concept にあればよい」では iOS の名前が Android concept に誤配置されても通ってしまい、`looks_api` で絞ると小文字の API (`show` / `hide`) の保存を検査できない。台帳に移動先を持たせることで、保存と正しい配置を同時に検査できる。

**代替案:**
- **A: レビューで各 concept を読み比べて確認する** — 網羅性を保証できない。却下
- **B: 分割前後の識別子集合 S₀ ⊆ S₁ だけを検査する** — 誤配置と除外 concept への着地を見逃す。`looks_api` を流用すると小文字 API を検査できない。却下
- **C: `scripts/` に常設の検査として置く** — 分割後は不要になる一度限りの検査で、常設にすると保守対象が増える。却下

### Decision 6: Skill の再生成は manifest の `targets` を手で組み替えてから docs-refresh `--all` で行う

**採用案:** manifest v3 の `concepts` (新設分を追加・移動分を差し替え)・`targets` (下表)・`excluded` (`layout-case-table.md` を追加) を本 change で書き換え、その後に docs-refresh を `--all` で起動して 5 Skill × en/ja と README 4 枚を再生成する。docs-refresh の Step 4 (更新方針の提示) の承認はオーナーが行う。再生成後の整合性チェック 8 種・API 名網羅検査・identity / local-path lint は docs-refresh の手順どおり実行する。

`targets` の完成形 (33 キー。`core/api/` は `core/` と略記):

| Skill ファイル | 源泉 |
|---|---|
| `ksdialogs-{ios,android,maui}/SKILL.md` | core 8 本 + 自 platform `api/` 全本 (maui は `di-registration.md` を含む 6 本) |
| `ksdialogs-{ios,android,maui}/references/dialogs.md` | core registration-show / result-notification / multi-display + `<p>/api/dialog-surface.md` |
| `ksdialogs-{ios,android,maui}/references/view-models.md` | core model-binding + `<p>/api/dialog-surface.md` |
| `ksdialogs-{ios,android,maui}/references/layout.md` | core layout + `<p>/api/layout-surface.md` |
| `ksdialogs-{ios,android,maui}/references/transitions.md` | core transition + `<p>/api/transition-surface.md` |
| `ksdialogs-{ios,android,maui}/references/loading.md` | core loading + `<p>/api/loading-surface.md` |
| `ksdialogs-{ios,android,maui}/references/toast.md` | core toast + `<p>/api/toast-surface.md` |
| `ksdialogs-maui/references/di-registration.md` | `maui/api/di-registration.md` (変更なし) |
| `ksdialogs-kmp/SKILL.md` | core 8 本 + `kmp/api/` 4 本 (dialog / loading / toast-surface + ios-host-integration) |
| `ksdialogs-kmp/references/dialogs.md` | core registration-show / result-notification / multi-display + `kmp/api/dialog-surface.md` |
| `ksdialogs-kmp/references/view-models.md` | core model-binding / registration-show / loading / toast (現行と同じ 4 本) + `kmp/api/dialog-surface.md` / `loading-surface.md` / `toast-surface.md` |
| `ksdialogs-kmp/references/layout.md` | core layout + `kmp/api/dialog-surface.md` (共有コードに添付の面が無い旨) + `ios/api/layout-surface.md` + `android/api/layout-surface.md` (各 OS 側で添付する面) |
| `ksdialogs-kmp/references/transitions.md` | core transition + `kmp/api/dialog-surface.md` + `ios/api/transition-surface.md` + `android/api/transition-surface.md` |
| `ksdialogs-kmp/references/loading.md` | core loading + `kmp/api/loading-surface.md` |
| `ksdialogs-kmp/references/toast.md` | core toast + `kmp/api/toast-surface.md` |
| `ksdialogs-kmp/references/android-host.md` | core 7 本 (現行と同じ、multi-display を除く) + `android/api/` 5 本 |
| `ksdialogs-kmp/references/ios-host.md` | core 7 本 (現行と同じ) + `kmp/api/ios-host-integration.md` |
| `ksdialogs-aiforms-migration/SKILL.md` | core 7 本 (現行と同じ) + `maui/api/` 6 本 |
| `ksdialogs-aiforms-migration/references/api-mapping.md` | core 8 本 (現行と同じ) + `maui/api/` 6 本 |

移行 Skill の源泉は cross/ADR-0011 の基準「新 API 側の concepts のうち対応表が触れるもの」を維持する。対応表は Dialog の登録・show (dialog-surface)・添付属性 (layout-surface)・演出 (transition-surface)・Loading (loading-surface)・Toast (toast-surface)・DI 連携 (di-registration) のすべてに触れるため、分割後は `maui/api/` 6 本すべてが「対応表が触れるもの」に当たる。ADR-0011 の Decision に書かれた具体列挙 (`maui/api/di-registration.md`) は起票時点で maui/api がその 1 本だけだった事実の反映であり、基準の改訂ではない (cross/ADR-0014 の Decision に注記する)。KMP の Android ホスト側が `android/api/` を源泉にするのは、ADR-0011 が Android ホスト側の内容重複を閉世界性の代償として認め「両 Skill から同じ源泉 concept を指して追従する」と決めているため。

**理由:** cross/ADR-0011 は「Skill 構成の見直しは変更フローの承認を通す」と定めており、源泉の組み替えは構成の見直しに当たるため manifest を本 change が書く。生成そのものは docs-refresh に任せることで「`skills/` を手で直接育てない」を守る。33 キーを個別に確定するのは、KMP の layout / transitions (共有コードに面が無く各 OS 側の面を案内する) と view-models (現行 4 源泉) が「core + 同名 surface」の規則に収まらないため。

**代替案:**
- **A: Skill を手で書き換える** — cross/ADR-0011 の「手で直接育てない」に反する。却下
- **B: manifest の `targets` を docs-refresh に推定させる** — docs-refresh は既存構成への追従に限定され、源泉の推定機能を持たない。却下
- **C: 移行 Skill の源泉を `maui/api/di-registration.md` + core に限定したまま (ADR-0011 の列挙を字義どおりに維持)** — 分割で MAUI の公開名が `maui/api/*-surface.md` に移るため、対応表が触れる MAUI の名前の変更が移行 Skill へ追従しなくなる。基準 (「対応表が触れるもの」) に反する。却下

### Decision 7: 他 platform 名の排除は platform 別の禁止トークン fixture で負の検査をする

**採用案:** `verification/forbidden-tokens.json` に Skill 範囲ごと (ios / android / maui / kmp / aiforms-migration) の禁止トークン集合を固定する。初期値は現行除外リストの基準「対象 Skill 外・機械検査由来」72 行に載る名前をその platform 列ごとに拾ったもの (他 platform の既定シングルトン・別名・添付の面・例外の詳細型・framework 型) とし、分割で新設した他 platform の surface concept の識別子のうち自 platform の公開面に無いものを加える。検査は 2 面で行う: (1) 自 platform の concept (`<p>/api/*.md`) に禁止トークンが出現しない、(2) 再生成した Skill (en / ja 両方) に禁止トークンが出現しない。API 名網羅検査 (源泉にあって Skill に無い名前を報告する) とは逆向きの検査であり、他 platform 名を Skill に写してしまう偽陽性を塞ぐ。

**理由:** API 名網羅検査は「源泉にあって Skill に無い」しか見ないため、他 platform 名を Skill 側に書いてしまえば候補から消えて通ってしまう。「他 platform 名が消える」を検証するには禁止集合を固定して concept と生成物の両方に負の検査を掛けるしかない。現行除外リストは分割前のノイズの完全な目録なので fixture の初期値に最適。

**代替案:**
- **A: API 名網羅検査の候補に例示の名前が出ないことだけを確認する** — 禁止集合が「等」で開いていて回帰判定にならず、Skill 側への写し込みを検出できない。却下
- **B: 候補の総数を baseline と比較する** — 総数は仕分けの内訳を示さず、減ればよい指標でもない。却下 (未仕分け 0 件だけを条件にする)

## Risks / Trade-offs

- 契約と公開面が 2 文書に分かれ、1 挙動を読むのに core と platform の 2 か所を辿る。core 末尾の「形態別の公開面」節と platform 冒頭の core リンクで往復を 1 クリックにする
- 書き直し中に契約の文が実装とずれる (意味の改訂になってしまう) 危険。実装者は core の文を「言い換え」に限定し、挙動の主張を増減させない。見つけた乖離は deviation.md に記録して蒸留送り
- 新設 concept 約 20 本の frontmatter・index・log の更新漏れ。doc-structure lint と docs-refresh の網羅検査 (未参照・未除外 0 件) で検出する
- 再生成で Skill の表現が変わる (源泉が同じでも生成は非決定的)。閉世界性・英日ロックステップ・表記ゆれの検査で品質を担保し、表現の好みは別 change

## Migration Plan

1. rollout-user-docs の完了 (archive) を待つ
2. 着手時点のコミット SHA を記録し、着地台帳を作る (Decision 5)
3. 機能単位で 5 巡 (dialog → layout → transition → loading → toast): core の書き直し + 4 platform の concept 新設を 1 巡で行う (1 巡ごとに doc-structure lint)
4. layout-case-table を `core/architecture/` へ移す。`rules.md` の配置判断に「契約 / 公開面」の基準を追記、各 index・log を更新
5. 着地台帳の突き合わせ (Decision 5) と禁止トークンの負の検査 (Decision 7、concept 側)
6. manifest を書き換え、docs-refresh `--all` を起動 (Decision 6)
7. 禁止トークンの負の検査 (Decision 7、Skill 側) と API 名網羅検査の報告から除外リストを組み直す (Decision 4。handbook の書き込みは ksn-concept の経路)
8. lint 一式 (local-path / identity / doc-structure / comment-policy 対象外) と `git diff --check`

## Open Questions

- なし (KMP の Swift 向け公開面は Decision 1 で既存の `kmp/api/ios-host-integration.md` に集約すると決めた)

## ADR 候補

- Decision 1・2・3 は cross/ADR-0014 (proposed) の Decision 節に取り込む (proposed のため本文を直接改訂する)。蒸留時に accepted へ昇格
- Decision 4〜7 は本 change 限りの手順で、ADR にしない
