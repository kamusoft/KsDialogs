# レビュー結果: split-concepts-platform-surface (001 回目)

**日付**: 2026-09-05
**判定**: CHANGES_REQUESTED
**範囲**: 中間レビュー — tasks.md グループ 1〜4 完了時点。グループ 5〜7 (manifest 書き換え・Skill 再生成・除外リストの組み直し・仕上げ) は未着手として扱い、未実装の指摘はしていない

## サマリー

分割そのものは spec の Requirement / Scenario をよく満たしている。core 8 本の書き直しは挙動の主張を減らさずに識別子だけを剥がしており (removed 行を baseline と 1 本ずつ突き合わせて確認)、新設 18 本の公開面 concept は実装コードとの照合サンプルで綴り・署名がほぼ正確だった。着地台帳・禁止トークン fixture・platform 差分の棚卸しはいずれも自分の手で再現でき、報告された数値 (着地 89 / 意図して落とした 19 / 未説明 0、禁止トークン 0 件、リンク未解決 0 件) はすべて一致した。

一方で、**新規に書いた公開面 concept に実装と食い違う記述が 1 か所ずつ (ios / android の layout-surface) ある**。公開面 concept の存在意義は綴りと署名の正確さであり、この記述はグループ 5 の再生成でそのまま利用者向け Skill に載る位置にあるため Major とした。ほかに長命層に残る規約文の欠落 (rules.md) と目次の誤った識別子 (core/index.md)、書き直した項目に残った構造 lint 違反を指摘する。

## 照合した規約

domains 定義プロジェクトのため cross + 作業ドメイン (cross) の handbook / concepts index を対象にした。

| 規約・文書 | 適用のきっかけ |
|---|---|
| ksn-core `references/concepts.md` | concepts の階層・価値 lint・アンカー規約・用語規約・可読性規約・粒度と分割 |
| ksn-core `references/doc-structure.md` | 育つ文書 (concepts) への追記・書き直し。構造 lint の扱い |
| ksn-core `references/domain-axis.md` | domains 定義プロジェクトの concepts レイアウト・ドメイン間リンク・index の読み替え |
| ksn-core `references/paths.md` | 成果物に書くパスの形式・archive 解決規則 (deviation の付随修正がこれに当たる) |
| ksn-core `references/delta-spec.md` | deviation = 合意済み差分・足場凍結 |
| `kasane/concepts/rules.md` | 概念の配置判断 (本 change が追記した節を含む) |
| `kasane/handbook/cross/user-skill-api-listing.md` | 禁止トークン fixture の初期値の由来 |
| `kasane/decisions/cross/0014` (proposed) | 分割の原則。proposed のため判定の根拠にはしていない |

`kasane/lessons/code-review.md` は不在。config.yaml の `skills.code-review` は空、`domain-skills` に cross の割り当てなし (本 change は文書専用のため実装スキルの適用対象なし)。

## 再実行した機械検査 (すべてホスト報告と一致)

| 検査 | 結果 |
|---|---|
| `verification/identifier-landing.py check` | 台帳 108 行: 着地 89 / 意図して落とした 19 / 未説明 0 (exit 0) |
| 禁止トークン負の検査 (concept 側、自前で再実装) | ios / android / maui / kmp / aiforms-migration すべて一致 0 件 |
| 除外リスト「対象 Skill 外・機械検査由来」71 行 → トークン 169 件の fixture 収容 | 欠落 0 件 (baseline.md の「消してよいのは実装確認できた場合だけ」を満たす) |
| concepts 内の相対リンク解決 | 未解決 0 件 (全 38 ファイル走査) |
| `scripts/local-path-lint.py` / `identity-lint.py` | ともに exit 0 |
| frontmatter (type/title/description/tags/timestamp) と h1 一致 | 概念 30 本すべて適合 |
| core 8 本の「形態別の公開面」節 | 8 本すべてに存在し、ios/android/maui/kmp の 4 リンク (layout / transition は kmp のみ dialog-surface) が解決 |
| 新設 18 本の冒頭宣言 + core へのリンク | 18 本すべて適合 |
| `git diff --check` | 指摘なし |
| `scripts/doc-structure-lint.py --paths <全ファイル>` | 16 件 / 4 ファイル (baseline 218d5c0 は 43 件 / 7 ファイル。指摘 4 参照) |

台帳の「移動先訂正」7 行 (`END` / `START` / `hide` / `hide()` / `ksDialogOptions` / `ksDialogPlacement` / `LoadingCoordinator`) は、備考が挙げる実装ファイルを開いて 7 行とも裏が取れた (`maui/KsDialogs.Maui/Contract/DialogTransitionEdge.cs` の `Start` / `End`、`android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/ViewDialogAttributes.kt` の `ksDialogOptions` / `ksDialogPlacement`、`maui/KsDialogs.Maui/Presentation/IKsLoading.cs` の `HideAsync`)。「意図して落とした」19 行は architecture への移動 9 件・Scenario ID 接頭辞などの非 API ラベル 9 件・数式断片 1 件で、architecture 行の 9 トークンは `kasane/concepts/core/architecture/layout-case-table.md` に実際に出現することを確認した。

## 実装コードとの照合 (サンプリング)

公開面 concept が挙げる名前・署名を次の範囲で実物と突き合わせた。

| 照合した concept | 読んだ実装 |
|---|---|
| `kasane/concepts/ios/api/dialog-surface.md` | `ios/Sources/KsDialogs/Contract/DialogError.swift`・`DialogResult.swift`・`Presentation/KsDialogs.swift`・`Presentation/Dialog.swift`・`Registry/DialogViewRegistry.swift`・`SwiftUI/DialogAttributeAttachment.swift` |
| `kasane/concepts/ios/api/layout-surface.md` | `ios/Sources/KsDialogs/Contract/DialogOptions.swift`・`DialogPlacement.swift`・`DialogEdgeInsets.swift`・`DialogLayoutArea.swift`・`DialogAlignment.swift` |
| `kasane/concepts/ios/api/loading-surface.md` | `ios/Sources/KsDialogs/Presentation/KsLoading.swift`・`Contract/LoadingStyle.swift`・`Registry/LoadingViewRegistry.swift` |
| `kasane/concepts/android/api/layout-surface.md`・`toast-surface.md` | `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/DialogOptions.kt`・`DialogPlacement.kt`・`DialogEdgeInsets.kt`・`DialogTransitionEdge.kt`・`KsToast.kt`・`ViewDialogAttributes.kt` |
| `kasane/concepts/maui/api/dialog-surface.md`・`layout-surface.md` | `maui/KsDialogs.Maui/Presentation/IKsDialogs.cs`・`IKsLoading.cs`・`Dialog.cs`・`Presentation/DialogAttachedProperties.cs`・`Contract/DialogResult.cs`・`DialogException.cs`・`DialogPlacement.cs`・`Registry/DialogViewRegistry.cs`・`Internals/DialogOptions.cs` |
| `kasane/concepts/kmp/api/dialog-surface.md`・`ios-host-integration.md` | `kmp/ksdialogs-kmp/src/commonMain/kotlin/jp/kamusoft/ksdialogs/kmp/`(一覧と `KsDialogs.kt`)・`ios/Sources/KsDialogs/Kmp/KsDialogsKmp.swift` |

一致しなかったのは指摘 1 の 2 か所だけで、それ以外 (`DialogError` の case 名、`DialogException` の入れ子クラス名、`ShowAsync` の `placement` 既定値 `null`、`Dialog.GetLayoutArea` / `SetLayoutArea` / `LayoutAreaProperty`、`LoadingStyle` の 5 プロパティと既定値、KMP の `notifier(for:result:)` / `show(_:result:placement:)` のラベル、`@Throws(DialogException::class, CancellationException::class)`) はすべて実装どおりだった。

## 指摘事項

### [🟠 Major] 新設した layout-surface の「すべての引数に既定値がある」が実装と食い違う

**該当箇所**: `kasane/concepts/ios/api/layout-surface.md:25`、`kasane/concepts/android/api/layout-surface.md:25`

**問題点**:

iOS は「いずれも `struct` で、すべてのイニシャライザ引数に既定値がある」と書いているが、直前の表 (`:17-23`) が挙げる 5 型のうち `DialogLayoutArea` と `DialogAlignment` は `enum` であり (`ios/Sources/KsDialogs/Contract/DialogLayoutArea.swift:4`・`DialogAlignment.swift:7`)、`DialogEdgeInsets` の 4 辺イニシャライザは既定値を持たない (`ios/Sources/KsDialogs/Contract/DialogEdgeInsets.swift:12` — `init(top: Double, left: Double, bottom: Double, right: Double)`)。既定値があるのは `init(all:)` ではなく `DialogOptions.init` / `DialogPlacement.init` の側である。

Android も同じ文型で「コンストラクタ引数にはすべて既定値がある」と書いているが、`DialogEdgeInsets` の主コンストラクタ 4 引数に既定値はない (`android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/DialogEdgeInsets.kt:11-15`)。`DialogOptions` / `DialogPlacement` は全引数に既定値がある (`DialogOptions.kt:27-32`・`DialogPlacement.kt:19-22`) ので、主語を型ごとに分ければ正しくなる。

この 2 文は baseline (218d5c0) の core には存在せず、本 change で新規に書いた文である。公開面 concept は「名前・署名・コード例」を正とする文書で、しかもグループ 5 の docs-refresh 再生成でそのまま利用者向け Skill の源泉になる。読んだ利用者 (あるいはエージェント) が `DialogEdgeInsets()` と書けばコンパイルが通らない。proposal の Non-Goal「意味を改訂しない」ではなく、**新しく足した事実の誤り**なので deviation の合意済み差分にも当たらない。

**推奨修正**: 表の直後の 1 文を型ごとに分ける。例:

- iOS: 「`DialogOptions` / `DialogPlacement` / `DialogEdgeInsets` は `struct`、`DialogLayoutArea` / `DialogAlignment` は `enum`。`DialogOptions` と `DialogPlacement` はすべてのイニシャライザ引数に既定値があり、`DialogEdgeInsets` は 4 辺を明示するか `init(all:)` / `.zero` を使う。数値は `Double` (論理単位 pt)」
- Android: 同様に `data class` 3 種 / `enum class` 2 種を分け、既定値があるのは `DialogOptions` / `DialogPlacement` だけである旨に直す (`DialogEdgeInsets` は `DialogEdgeInsets(all)` / `DialogEdgeInsets.ZERO` を案内)

あわせて、他の surface に同型の包括表現がないかを 1 度 grep してほしい (`いずれも` / `すべて` で拾える。今回の走査では上記 2 か所以外に該当なし)。

### [🟡 Minor] rules.md の共通概念名の条件に「KMP の公開面 = 3 側」の定義が落ちている

**該当箇所**: `kasane/concepts/rules.md:64`

**問題点**:

追記された規約文は「4 つの Skill 範囲 (iOS / Android / MAUI / KMP) すべての公開面に同綴りで存在する」とだけ書いている。しかし design.md Decision 2 と `specs/concept-placement/spec.md` の Requirement は「iOS / Android / MAUI / **KMP の 3 側**」と書いており、`verification/core-contract-check.md` 冒頭の「公開面として読んだ場所」は KMP を **commonMain + Android ホスト側 + Swift 向け公開面**の 3 つと定義している。`DialogNotifier` / `DialogTransition` / `DialogTransitionEdge` の 3 つが core に残っているのは、この読み方があって初めて成立する (いずれも commonMain に宣言が無いことを私も `kmp/ksdialogs-kmp/src/commonMain/kotlin/jp/kamusoft/ksdialogs/kmp/` の一覧で確認した)。

この定義が書かれていないと、rules.md だけを読む後続の書き手は core にある `DialogNotifier` を規約違反と判断して剥がしにかかるか、逆に「公開面」を広く取って条件を満たさない名前を足してしまう。`verification/core-contract-check.md` は change と一緒に archive されるので、定義が長命層に残らない。

**推奨修正**: `rules.md` の当該段落に 1 文足す。例:「ここでいう KMP の公開面は、commonMain の公開宣言に加えて、KMP Skill の源泉に入る Android ホスト側と Swift 向け公開面 (`kmp/api/ios-host-integration.md`) の 3 側を指す (cross/ADR-0014)」。cross/ADR-0014 を accepted へ昇格させる際 (task 7.1) にも同じ定義が Decision 節に入っているか併せて確認するとよい。

**判定への影響**: cross/ADR-0014 は proposed なので、これを根拠に CHANGES_REQUESTED は出していない。rules.md (長命層) の記述と実際に適用した規則が食い違っている点だけを指摘している。

### [🟡 Minor] core/index.md が、乖離として記録済みの綴り `LayoutArea` をバッククォート付きで掲げている

**該当箇所**: `kasane/concepts/core/index.md:13`

**問題点**:

layout-semantics.md の 1 行説明が「基準領域 (`LayoutArea`)」になっている。`deviation.md:21` は「記述 `LayoutArea` → 実装は 4 形態とも `DialogLayoutArea`。core からは型名を外して散文にし、綴りは各 platform の layout-surface に置いた」と記録しており、本文からは実際に外れている (`grep -rn 'LayoutArea' kasane/concepts/` の結果、core 側の残存はこの index の 1 行だけ)。baseline では同じ箇所が平文 `LayoutArea` だったので、本 change は**存在しない綴りにバッククォートを付け足した**ことになる。

index.md は manifest の concept 集合から外れるため API 名網羅検査には出ないが、読者が最初に読む地図であり、本文と deviation の記録の両方と矛盾する。

**推奨修正**: 「基準領域 (`DialogLayoutArea`)」に直すか、本文に合わせて「基準領域」と平文にする。同じ 1 行説明にある「器のメタ属性」も baseline の `DialogOptions` / `DialogPlacement` 併記を落として整合させてあるので、後者のほうが揃う。

### [🟡 Minor] 書き直した項目に構造 lint 違反が残っている (5 件)

**該当箇所**: `kasane/concepts/core/api/transition-semantics.md:13`(229字)・`:17`(279字)・`:91`(335字)・`:149`(267字)、`kasane/concepts/core/api/layout-semantics.md:17`(219字)

**問題点**:

`scripts/doc-structure-lint.py --paths <find で全ファイル列挙>` の残存 16 件 / 4 ファイルのうち、11 件は本 change が触っていないファイル (`maui/api/di-registration.md` 10 件・`cross/reference/reference-repositories.md` 1 件) で、これは持ち越しとして妥当。ただし残る 5 件は**本 change で書き直した本文の中にある**:

- `transition-semantics.md:149` は Decision 2 が明示的に書き換えを指示した「呼び出し元のキャンセル」の項目で、baseline の同項目 (267 字) から文言を変えて同じ 267 字になっている
- `layout-semantics.md:17` は deviation の付随修正でリンクラベルを短縮したはずの出典行だが、baseline の 214 字から **219 字へ増えている**
- `:91` の duration の項目は baseline 400 字 → 335 字と縮んだが上限は超えたまま

ksn-core `references/doc-structure.md` は「触った節だけを基準で見直せばよい」と定めた上で「lint の違反を残したまま確定する」を禁止事項に挙げている。全体で 43 件 → 16 件と大きく改善している点は評価するが、書き直した項目そのものについては「着手時点からの持ち越し」では説明しきれない。なお tasks 4.4 は「lint を掛ける」としか書いておらず、チェック済みの記載自体は虚偽ではない。

**推奨修正**: 5 件を段落・表の行・小節へ移す (`:149` と `:91` は内容が場合分けなので表に向く。`:13` / `:17` / layout の `:17` は冒頭の読み方の箇条書きなので、multi-display-semantics.md で採ったのと同じく段落へ出せば揃う)。手を入れないと判断するなら、その旨と根拠を deviation.md に「合意済み差分」として記録し、`di-registration.md` の 10 件と区別できる形で残すのが筋。

### [🔵 Suggestion] layout-semantics に `approvedDiff` 統制の禁止事項が二重に残っている

**該当箇所**: `kasane/concepts/core/api/layout-semantics.md:216`

**問題点**: 「してはいけないこと」の「共通ケース表の統制を迂回しない — 承認のない OS 差の期待値分岐も、実装に合わせた期待値の書き換えも認めない」は、`kasane/concepts/core/architecture/layout-case-table.md:73-74` の「してはいけないこと」2 項目と同じ規則である。design.md Decision 3 と spec Requirement「検証機構の記述は architecture」は、layout-semantics には「共通ケース表を単一の正とする旨の 1 文と当該 concept へのリンクだけ」を残すとしている。Scenario (ケース ID と `approvedDiff` の出現位置) は通っているので違反とはしないが、同じ規則が 2 文書にあると片方だけ改訂されたときに割れる。

**推奨修正**: `:216` はリンクだけに寄せる (例:「共通ケース表の統制は [レイアウト共通ケース表と OS 差の統制](../architecture/layout-case-table.md) が定める」)。`:202` の保証 (同じ入力なら同じ rect) は利用者が観察する契約なので core に残すのが正しく、こちらは触らなくてよい。

### [🔵 Suggestion] 禁止トークンの concept 側 0 件は、新設 surface 由来分については構成上必ず 0 件になる

**該当箇所**: `kasane/changes/split-concepts-platform-surface/verification/forbidden-tokens-concepts.txt`、`verification/baseline.md:83-107`

**問題点**: fixture の追加規則は「他 platform の surface concept の識別子のうち自 platform の公開面に無いもの」なので、追加分は定義上その platform の concept と交わらない。実測でも、ios の集合 200 件は「android ∪ maui ∪ kmp の識別子 − ios/api の識別子 − 共通概念名 14 種 − STOP 語」とちょうど一致し、独立した信号を持つのは残る 4 件 (`C05` / `C19` / `approvedBy` / `approvedDiff` — architecture への移動漏れの回帰検出) だけだった。android / maui / kmp / aiforms-migration も同じ構造である。

fixture 自体は妥当で (除外リスト由来 169 トークンの欠落 0 件を確認済み)、この検査は「架空の 0 件」ではなく「移動漏れが無い」ことの確認としては機能している。ただし報告で「concept 側 0 件」を「他 platform 名が混ざっていないことの証拠」として読むと過大評価になる。

**推奨修正**: `forbidden-tokens-concepts.txt` の冒頭に、この検査の実効範囲 (新設 surface 由来分は構成上 0 件で、独立した信号は除外リスト由来の非 API トークン) を 1〜2 行で書き添える。他 platform 名の排除を実質的に検証するのは task 6.1 の Skill 側の検査なので、そちらの結果をもって Decision 7 の Goal 達成と判断するとよい。

## 判断材料として書いておくこと (指摘ではない)

- **`DialogNotifier` / `DialogTransition` / `DialogTransitionEdge` を共通概念名に入れた判断は spec 適合である**。design.md Decision 2 と spec Requirement が「KMP の 3 側」と明記しており、`verification/core-contract-check.md` はその読み方・裏取り・見込みが外れた場合の撤退方針まで書いている。私の側でも見込みを確認した — KMP Skill の `references/transitions.md` の源泉に `ios/api/transition-surface.md` と `android/api/transition-surface.md` が、`references/android-host.md` の源泉に `android/api/` 5 本が入り、`DialogTransition` は前者に、`DialogNotifier` は後者に実際に出現する。`api-coverage-check.py` は Skill 単位で源泉と本文を突き合わせるため、task 6.1 でこれらが KMP Skill の候補に上がる可能性は低い。`DialogTransitionEdge` は core 本文で未使用なのでそもそも候補にならない。
- **task 3.4 の「据え置き」は妥当**。`kasane/decisions` / `kasane/handbook` を grep したところ `layout-semantics.md` を指すパス参照は 1 件も無く、付け替えるべき壊れた参照は存在しなかった。ADR は accepted 後に編集しない規律どおり。
- **task 2.10 の「本文は変えない」との差**: `kasane/concepts/kmp/api/ios-host-integration.md` には「型付き入口の署名 (Dialog)」節が増えているが、これは task 2.3 (KMP の Swift 向け公開面を同ファイルへ集約) の指示によるもので逸脱ではない。追記内容は `ios/Sources/KsDialogs/Kmp/KsDialogsKmp.swift:130-205` と一致することを確認した。
- **deviation.md の付随修正 7 件**はいずれも ksn-core の同梱条件に収まる。archive パスへの付け替えは `references/paths.md` の archive 解決規則の適用、注記行の追随はリンク解決の事実追随、構造 lint 是正の短縮と di-registration の節分割は本 change の書き込みが lint を発火させたことへの直接の対応で、どれも本 change の作業から生じている。契約と実装の乖離 5 件も、記載どおりの実装をすべて確認できた (`DialogLayoutArea` / Android の `@ColorInt Int` と MAUI の `Microsoft.Maui.Graphics.Color` / MAUI の `Start` `End` / Swift の `start(message:placement:_:)` / `durationMs` と `duration`)。
- **core の書き直しは「言い換え」に収まっている**。baseline との差分で消えた主張 (処理が必ず実行されること、hide が世代を終わらせること、外側タップが常に無効であること、configure の順序保証、同一インスタンス並行 show の失敗、duration の丸め、Compose を別モジュールにした理由の「MAUI Android がバインディング経由で取り込む経路」まで含む) を 1 つずつ追跡し、すべて core か対応する platform surface に残っていることを確認した。新たに増えた主張も見つからなかった (multi-display の「報告口も show 1 回ごとに新しい」は baseline の registration-show 用語節にあった記述の再掲)。

## アクションプラン

1. **[Major]** `kasane/concepts/ios/api/layout-surface.md:25` と `kasane/concepts/android/api/layout-surface.md:25` の 1 文を型ごとに分けて実装に合わせる。グループ 5 の再生成より前に直す (再生成後だと Skill の作り直しが要る)
2. **[Minor]** `kasane/concepts/rules.md:64` に「KMP の公開面 = commonMain + Android ホスト側 + Swift 向け公開面の 3 側」の定義を 1 文足す
3. **[Minor]** `kasane/concepts/core/index.md:13` の `LayoutArea` を `DialogLayoutArea` にするか平文へ落とす
4. **[Minor]** 書き直した 5 項目の構造 lint 違反を是正する。是正しない判断なら deviation.md に根拠つきで記録し、未着手ファイルの持ち越し 11 件と区別できる形にする
5. **[Suggestion]** `layout-semantics.md:216` の統制の禁止事項をリンクへ寄せる
6. **[Suggestion]** `forbidden-tokens-concepts.txt` に concept 側検査の実効範囲を注記し、Decision 7 の達成判定は task 6.1 の Skill 側検査で行う旨を明示する

1〜3 は数行の修正で、4 も表・段落への移し替えで済む。修正後は `identifier-landing.py check` と `doc-structure-lint.py --paths` の再実行だけで足りる (公開名を動かす修正ではないため台帳と禁止トークンの再採取は不要)。
