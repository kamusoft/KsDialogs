# レビュー結果: proofread-user-skills-ja (1 回目)

**日付**: 2026-09-06
**判定**: CHANGES_REQUESTED

## サマリー

利用者向け Skill 64 ファイルの校正として、狙った型 (API 選択表 → 現実的なサンプル分割 → 構成ミスの表) は 4 platform の dialogs / loading / toast にきれいに揃っており、公開 overload の網羅・例外メッセージの逐語一致・既定値は実装と照合して高い精度で一致していた。機械検査 (ja/en コードブロック byte 一致、見出し構造、内部リンク解決、frontmatter、local-path / identity lint) もすべて差分ゼロで再現できた。

一方で、**実装に存在しない受け口を選択表が指している (KMP)**、**catch できない失敗を catch できる失敗の表に混ぜている (iOS)**、**同名 enum の方向解釈という誤読しやすい細則が 4 platform 中 1 つにしか書かれていない**の 3 点は、この change が最も価値を置いた「表から判断できる」性質を損なうため修正を要する。加えて、summary が決定事項として明記した表記統一 (「ダイアログ」→「Dialog」、「覆い」の語の統一) が MAUI と一部 ja ファイルで徹底されていない。

## 照合した規約

- `kasane/handbook/cross/user-skill-api-listing.md` (適用のきっかけ: `skills/**` を更新するとき) — 「簡潔でも網羅」方針、現行除外リスト、コード例のノーコメント規約と ja/en byte 一致
- `kasane/handbook/cross/comment-policy.md` (always) — `kasane/config.yaml` の `lint.comment-policy.exclude` に `skills` があり、本 change の対象ファイルは規約対象外。コード例にコメントが無いことは上記 user-skill-api-listing.md 側で確認
- `kasane/handbook/cross/test-execution.md` — 本 change はソース・テストを変更しないため、代わりに docs-refresh の検査群と標準 lint を実行 (下記)
- `kasane/concepts/core/api/{layout,loading,toast,multi-display}-semantics.md`、`kasane/concepts/{maui,ios,android,kmp}/api/*` — 記述の裏取りに使用
- `kasane/lessons/code-review.md` は存在しないため、重点観点・指摘しないことの参照なし

## 実行した検査 (すべて成功)

| 検査 | 結果 |
|---|---|
| `.agents/skills/docs-refresh/scripts/code-block-parity-check.py` | code blocks byte-identical |
| `.agents/skills/docs-refresh/scripts/heading-parity-check.py` | en/ja heading structure OK |
| `.agents/skills/docs-refresh/scripts/link-resolution-check.py` (`DOCS_REFRESH_TARGETS` = `find skills -name '*.md'` の 68 件) | All internal links resolve |
| `.agents/skills/docs-refresh/scripts/frontmatter-check.py` | frontmatter OK |
| `scripts/local-path-lint.py` / `scripts/identity-lint.py` | exit 0 |

`.agents/skills/docs-refresh/scripts/api-coverage-check.py` は summary の実施リストに無かったため本レビューで追加実行した (結果は 🔵 Suggestion 節)。

## 指摘事項

### [🟠 Major] KMP の登録表が、共有面に存在しない `Loading.instance.registry` / `Toast.instance.registry` を指している

**該当箇所**: `skills/ja/ksdialogs-kmp/references/view-models.md:75-78` (en 同行)

**問題点**: 「必要な登録」節の表が「レジストリ」列に `Dialog.instance.registry` / `Loading.instance.registry` / `Toast.instance.registry` を並べ、その各行が Android と iOS の両列にかかる構成になっている。しかし KMP 共有面で `registry` を持つのは `KsDialogs` だけで (`kmp/ksdialogs-kmp/src/commonMain/kotlin/jp/kamusoft/ksdialogs/kmp/KsDialogs.kt:14`)、`KsLoading.kt` / `KsToast.kt` には `registry` メンバが無い。`Loading.instance` / `Toast.instance` の型はそれぞれ `KsLoading` / `KsToast` (`Loading.android.kt:10`, `Toast.android.kt:10`, `Loading.ios.kt:14`, `Toast.ios.kt:14`)。この名前が成立するのは Android Native 側の `jp.kamusoft.ksdialogs.Loading` を import した場合だけで、iOS には対応する受け口が存在しない (登録は `Loading.shared.kmp`)。

同じ Skill の `skills/ja/ksdialogs-kmp/references/loading.md:203` は「共有コードは …インライン factory の経路も Loading レジストリのハンドルも持たない」と書いており、表と地の文が矛盾している (`kasane/concepts/kmp/api/loading-surface.md` / `toast-surface.md` の「この面に無いもの」も同旨)。同じ名前でも `loading.md:98,153` と `toast.md:52,117` は「Android は …」と明示して限定しているので、表だけが無限定。

**推奨修正**: 「レジストリ」列を「Android Native のレジストリ」に改題する (この列は iOS 行には当たらないことを明示する)、または各セルを `jp.kamusoft.ksdialogs.Loading.instance.registry` の完全修飾で書く。ja / en 双方。`skills/ja/ksdialogs-kmp/SKILL.md:60` は「Android アプリは …」と限定できているので、表もその水準に揃える。

### [🟠 Major] iOS Toast の構成ミス表に、`show` から catch できない case が混ざっている

**該当箇所**: `skills/ja/ksdialogs-ios/references/toast.md:236-239` (en 同行)

**問題点**: 節の冒頭は「登録経路で ViewModel 型が未登録なら `show` の呼び出し時点で同期的に throw し」と正しく書き、続く表に `viewFactoryNotRegistered` と `viewFactoryTypeMismatch` の 2 行を並べ、そのあとに `catch DialogError.viewFactoryNotRegistered(...)` の例を置いている。表の体裁上、後者も `show` から catch できるように読める。

実装では、`show` が同期に検査するのは `resolveFactory` (= `viewFactoryNotRegistered`) だけで (`ios/Sources/KsDialogs/Presentation/ToastCoordinator.swift:96-98`)、`viewFactoryTypeMismatch` は `makeCustomContent` (`ToastCoordinator.swift:285-287`) が投げ、その呼び出し元 `beginDisplay` (`ToastCoordinator.swift:147-152`) が warning ログを出してその 1 枚を破棄するだけで呼び出し元へは返らない。同じ節の 1 段落上 (`toast.md:234`) が「受理されたあとの factory の失敗は呼び出し元へ返せない」と正しく書いているのに、表がそれを打ち消している。

同じ Skill の `skills/ja/ksdialogs-ios/references/dialogs.md` は型消去をまたぐ 3 case を表から外して注記に落としており、Toast だけ扱いが非対称。Loading (`LoadingCoordinator.swift:123` の `beginUse` から伝播) は表に載せて正しい。

**推奨修正**: `viewFactoryTypeMismatch` の行を表から外し、`toast.md:234` の段落に「型消去をまたぐ不整合 (`viewFactoryTypeMismatch`) もこの経路になり、`show` からは catch できない」を追記する。ja / en 双方。

### [🟠 Major] `DialogAlignment.Start` / `End` が RTL に追随しないことが 4 platform 中 1 つにしか書かれていない

**該当箇所**: `skills/ja/ksdialogs-maui/references/layout.md:14`、`skills/ja/ksdialogs-ios/references/layout.md:16`、`skills/ja/ksdialogs-android/references/layout.md:30` (en 同行)

**問題点**: 3 platform の layout.md は `DialogAlignment` (`Start` / `Center` / `End` / `Fill`) を列挙するだけで方向の解釈に触れていない。一方、同じ Skill の transitions.md は `DialogTransitionEdge` の `Start` / `End` について「layout 方向に追随する (右から左へ読む環境では左右が入れ替わる)」と 4 platform すべてで明示している (`skills/ja/ksdialogs-maui/references/transitions.md:30`、`skills/ja/ksdialogs-android/references/transitions.md:25` ほか)。

実装は逆の規則: `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/DialogAlignment.kt:6` が「`START` / `END` は物理方向 …書字方向 (RTL) には追随しない」、`kasane/concepts/core/api/layout-semantics.md:168` も「先頭寄せ / 末尾寄せは物理方向」。同名の `Start` / `End` が同じ Skill 内の 2 箇所で逆の意味を持つ意図的な非対称なのに、片方だけ説明されているため誤読を誘う。

`skills/ja/ksdialogs-kmp/references/layout.md:23` はこの規則を正しく書いており (「`START` と `END` は物理方向 … 書字方向には追随しない」)、残る 3 platform だけが落ちている。

**推奨修正**: kmp/layout.md:23 と同じ 1 行を maui / ios / android の layout.md の alignment 表の直後に足す。ja / en 双方。

### [🟠 Major] 決定した表記統一 (「ダイアログ」→「Dialog」) が MAUI で徹底されていない

**該当箇所**: `skills/ja/ksdialogs-maui/references/dialogs.md:259` (見出し)、同 `:7`、`:261`、`skills/ja/ksdialogs-maui/references/transitions.md:38`、`:116`、`:138`

**問題点**: summary の決定事項「プロダクト機能名は『Dialog』『Loading』『Toast』で表記し (「ダイアログ」を置換)」に対し、MAUI ja の 6 箇所が「ダイアログ」のまま残っている。特に `dialogs.md:259` は節見出し「## 独立したダイアログを重ねて表示する」であり、同じ位置の他 platform は ios `## 独立した Dialog を重ねて表示する` / android `## 独立した Dialog を重ねる` / kmp `## Dialog を独立して重ねて表示する` と「Dialog」を使っている。en 側は `Stack independent Dialogs` で既に統一済みなので、ja だけが取り残されている。

例外メッセージ中の「ダイアログを提示できる画面がありません。」は実装のリテラル引用なので対象外 (現状のままで正しい)。

**推奨修正**: 上記 6 箇所の地の文・見出しを「Dialog」に置換する。置換後は `heading-parity-check.py` が en と対応するので再実行して確認する。

### [🟡 Minor] 却下したはずの訳語「scrim」が ja 側に 3 箇所残っている

**該当箇所**: `skills/ja/ksdialogs-maui/references/dialogs.md:97`、`skills/ja/ksdialogs-maui/references/loading.md:178`、`skills/ja/ksdialogs-ios/references/dialogs.md:115`

**問題点**: いずれも「覆い (scrim) と配置はライブラリの器が受け持つため …」。summary は「ios ワーカーは『覆い』を scrim、android は overlay と訳して不揃いになったため、API 名 (`overlayColor` / `OverlayDuration`) に寄せて overlay に統一」と記録しており、en 側は同じ文で "The overlay and the placement are handled by …" と overlay に統一済み。ja の括弧書きだけが却下語のまま残っており、API 名 (`Dialog.OverlayColor` / `overlayColor`) を探す読者を迷わせる。他の 15 ファイル以上では「覆い」に括弧書きは付いていない。

**推奨修正**: 3 箇所の「(scrim)」を削除する (必要なら「(overlay)」に置換して API 名へ橋渡しする)。

### [🟡 Minor] 同じ概念が「器」/「host」/ en "container" / en "host" に分かれている

**該当箇所**: `skills/ja/ksdialogs-maui/references/dialogs.md:5`、`loading.md:9`、`toast.md:13`、`:237`、`transitions.md:139` (「host」) と `dialogs.md:97`、`loading.md:178` (「器」)

**問題点**: ja MAUI は同じ提示コンテナを 5 箇所で「host」、2 箇所で「器」と呼び分けている。ios / android / kmp の ja は一貫して「器」。en 側もこれを引き継いで MAUI だけ "the library's host" / "host removal"、他 3 platform は "the library's container" / "the container" になっている (`skills/en/ksdialogs-ios/references/dialogs.md:115` ほか)。summary は「『器』など分かりにくい語を平易化」を施術内容に挙げているが、結果は 1 platform 内でも 2 語が併存している。

**推奨修正**: MAUI ja を「器」に寄せる (他 3 platform に合わせる) か「host」に寄せるかを決めて 7 箇所を統一し、en もそれに合わせて "container" / "host" のどちらかに揃える。

### [🟡 Minor] ja の SKILL.md 見出しが platform 間で揃っていない (en は揃っている)

**該当箇所**: `skills/ja/ksdialogs-kmp/SKILL.md:38`、`:79`、`skills/ja/ksdialogs-android/SKILL.md:68`

**問題点**: 4 つの platform SKILL.md は en では `Setup` / `Minimal example` / `Choose a recipe` と完全に統一されているが、ja は
- セットアップ節: maui / ios / android が「## セットアップ」、kmp だけ「## Setup」(日本語文書に未訳の英語見出しが 1 つだけ残っている)
- 最小例節: maui / ios が「## 最小例」、android / kmp が「## 最小コード」

と 2 通りに割れている。この change は ja を正として書き直す方針なので、正であるはずの ja 側に統一漏れが残っている形になる。

**推奨修正**: kmp の「## Setup」→「## セットアップ」、「最小例」/「最小コード」のどちらかへ 4 platform を統一する。見出し文字列だけの変更なので `heading-parity-check.py` には影響しない。

### [🟡 Minor] iOS の `configure` サンプルが escaping closure で暗黙 `self` を使っておりコンパイルできない

**該当箇所**: `skills/ja/ksdialogs-ios/references/view-models.md:132` (en 同行、コードブロックは共通)

**問題点**: `viewModel.text = try await loadCurrentName()` の `loadCurrentName()` は `ItemScreenModel` のインスタンスメソッドで、`ItemScreenModel` は `final class` (`skills/ja/ksdialogs-ios/references/dialogs.md:179`)。`configure` の型は `@escaping @MainActor @Sendable (ViewModel) async throws -> Void` (`ios/Sources/KsDialogs/Presentation/KsDialogs.swift:95`) なので、escaping closure 内で参照型のメンバを暗黙 `self` で呼ぶことはできない (`Call to method ... requires explicit use of 'self'`)。同じ節の同期版 (`view-models.md:113-115`) は closure 内で `self` のメンバを呼んでいないため問題ない。

**推奨修正**: `try await self.loadCurrentName()` にする。コードブロックなので ja / en 双方を同時に直す (byte 一致を維持)。

### [🟡 Minor] iOS の選択表が factory の `throws` を落としており、factory の失敗の扱いがどこにも書かれていない

**該当箇所**: `skills/ja/ksdialogs-ios/references/dialogs.md:19-20` (en 同行)

**問題点**: 表は `factory: (ViewModel, DialogNotifier<Result>) -> some View` / `-> UIView` と書くが、実装は `@escaping @MainActor @Sendable (ViewModel, DialogNotifier<ViewModel.Result>) throws -> UIView` / `throws -> Content` (`ios/Sources/KsDialogs/Presentation/KsDialogs.swift:33,41`)。`DialogViewRegistry.register` の factory も同じく `throws` (`ios/Sources/KsDialogs/Registry/DialogViewRegistry.swift:27,42,56,67`)。`@escaping @MainActor @Sendable` を落とすのは表の可読性のための妥当な省略だが、`throws` は「factory が投げた失敗は show の失敗になり、Dialog は提示されない」という挙動そのものなので、落とすと利用者が factory 内の失敗経路を設計できない。この挙動は Skill のどこにも書かれていない。

**推奨修正**: 表のシグネチャを `throws -> some View` にするか、「登録せずに content を表示する」節に「factory は `throws` にでき、投げた失敗は show の失敗になる (提示されない)」の 1 文を足す。Loading / Toast の factory も同様。

### [🟡 Minor] Android の記述漏れ 4 件 (他 platform では書かれている細則)

**該当箇所と内容**:

| 該当箇所 | 問題点 | 実装・他 platform |
|---|---|---|
| `skills/ja/ksdialogs-android/references/toast.md:24` | 「`durationMs` はミリ秒で、省略すると `ToastStyle.defaultDuration` を使う」だけで、0 以下の丸めに触れていない | `ToastCoordinator.kt:323-335` は 0 以下も警告ログ付きで `defaultDuration` へ、それも 0 以下なら `BUILTIN_DEFAULT_DURATION` (1500) へ丸める。`skills/ja/ksdialogs-maui/references/toast.md:9` は 3 段の fallback を明記済み |
| `skills/ja/ksdialogs-android/references/toast.md:24` | 契約既定配置を「下部中央」とだけ書く | `ToastPlacementDefault.kt:12-20` は下部中央 + `offsetY = -80.0` dp。`skills/ja/ksdialogs-maui/references/toast.md:66` は「下部中央から上方向へ論理単位 80」と書けている |
| `skills/ja/ksdialogs-android/references/dialogs.md:275` | 「`PresentationHostUnavailable` 以外の例外は、**解決できなかった** ViewModel の型名を `viewModelTypeName` から読める」 | `ViewModelAlreadyShowing` / `ValueClassViewModel` は解決失敗ではない (`DialogException.kt:34,46`)。同じ Skill の `loading.md:238` / `toast.md:182` は既に「対象の ViewModel 型名」と書いており、そちらが正 |
| `skills/ja/ksdialogs-android/references/loading.md:236`、`toast.md:180` | `ValueClassViewModel` を表示時の失敗としてだけ説明している | `LoadingViewRegistry.kt:32` / `ToastViewRegistry.kt:32` の `register` も `requireReferenceTypeViewModel()` を通すため登録時点でも投げる。同じ Skill の `view-models.md:59` は Dialog について「登録の時点と …すべての表示の時点で」と正しく書いている |

**推奨修正**: 各行を上表の「実装・他 platform」に合わせて補う。ja / en 双方。

### [🟡 Minor] en の意味ずれ 3 件 (KMP)

**該当箇所と内容**:

| 該当箇所 | ja | en | 問題点 |
|---|---|---|---|
| `skills/en/ksdialogs-kmp/SKILL.md:40` | 「現行 artifact のビルド環境は Kotlin 2.4.10 と Gradle 9.7.0。」(記述) | "Use Kotlin 2.4.10 and Gradle 9.7.0 for the current artifact." (命令) | 直後の "The supported consumer Kotlin range has not yet been finalized" と矛盾する。利用者に版を固定させる指示に読める |
| `skills/en/ksdialogs-kmp/references/ios-host.md:93` | 「`try?` はその失敗を `nil` に潰すため、**型が食い違うと**この button は結果を報告できなくなります」 | "…`try?` collapses that failure into `nil`, which leaves this button unable to report." | 限定「型が食い違うと」が落ち、直前のサンプル (型は一致している) の無条件の性質として読める |
| `skills/en/ksdialogs-kmp/references/transitions.md:124` | 「**閉鎖の演出が走る経路** — …ライブラリが実行するすべての閉鎖経路」 | "**The closures that run the exit transition** — every closure the library performs…" | 「経路」を "closure" と訳したため、直後の節が扱う利用者記述の hook (`suspend (View) -> Unit`) と主語が入れ替わる |

**推奨修正**: 順に "The current artifact is built with Kotlin 2.4.10 and Gradle 9.7.0."、"…so a type mismatch leaves this button unable to report."、"The routes that run the exit transition — every closing route the library performs…"。

### [🟡 Minor] 宣言のない型がサンプルに登場する (「後続の節で宣言する」という約束と食い違う)

**該当箇所**: `skills/ja/ksdialogs-maui/references/dialogs.md:252-254`、`skills/ja/ksdialogs-android/references/dialogs.md:176-178`

**問題点**: MAUI は `dialogs.md:29` で「使う型は後続の節で宣言する `ConfirmDialogViewModel` … と `ItemEditDialogViewModel` である」と明示したうえで、`:252-254` に一度も宣言されない `NoticeDialogViewModel` / `NoticeDialogView` を出している (`NoticeDialogViewModel` は `transitions.md:51` にはあるが同一ファイル内にはない)。Android は `:176-178` の `registerCompose(ItemEditViewModel::class) { viewModel -> ItemEditContent(viewModel) }` で `ItemEditContent` を使うが、同 Skill 内に定義が無い (同じ役割の `ConfirmContent` / `ConfirmCardView` / `NoticeViewModel` はいずれも宣言を伴っている)。

**推奨修正**: 宣言を足すか、「`ItemEditContent` は `ConfirmContent` と同じ形で利用者が書く composable」のような 1 文を添える。MAUI は `:29` の約束の側を「一部は利用者が書く型」に緩める手もある。

### [🟡 Minor] 多段表示の記述から契約側の限定が落ちている

**該当箇所**: `skills/ja/ksdialogs-android/references/dialogs.md:256`、`skills/ja/ksdialogs-maui/references/dialogs.md:261` (en 同行)

**問題点**: 「下の Dialog へ先に結果を報告すると下だけが閉じ、上はそのまま操作できる」と書くが、`kasane/concepts/core/api/multi-display-semantics.md` の PB-MD-04 節は「この状況はアプリコードが下の結果報告口 (`DialogNotifier`) を保持して先に報告した場合にのみ起きる。ユーザー操作 (完了 / キャンセル / 外側タップ / 戻るボタン) は常に手前の 1 枚にしか届かない」と限定している。記述自体は誤りではないが、限定が落ちると「ユーザー操作でも下が先に閉じうる」と読める。

**推奨修正**: 「(下の報告口をアプリ側が保持して報告した場合。ユーザー操作は常に手前の 1 枚にしか届かない)」を補う。

### [🟡 Minor] KMP loading.md の iOS 追加メッセージが 1 件欠けている

**該当箇所**: `skills/ja/ksdialogs-kmp/references/loading.md:151` (en 同行)

**問題点**: 「iOS ではこのほかに …`登録済みの View factory が ViewModel 型 {型名} を受け取れません。` がメッセージになる」とあるが、`kmp/ksdialogs-kmp/src/iosMain/kotlin/jp/kamusoft/ksdialogs/kmp/IosLoadingGateway.kt:141` は interop bridge が handle も error も返さなかった場合の fallback として `ローディングの表示に失敗しました。` も定義している。対になる `skills/ja/ksdialogs-kmp/references/dialogs.md:164` は同種の `ダイアログの表示に失敗しました。` を載せているので、loading だけ非対称。

**推奨修正**: loading.md:151 に同メッセージを追記する。

### [🔵 Suggestion] ja の文体が ios-host.md だけ です・ます調

**該当箇所**: `skills/ja/ksdialogs-kmp/references/ios-host.md` (:7-9、:18、:22、:58、:62、:71、:93、:97、:99、:109、:111、:141、:160、:162)

**問題点**: ja 32 ファイルのうち、この 1 ファイルだけが です・ます調で書かれている (他 31 ファイルは である調)。本 change はこのファイルにも 69 行を追加しており、追加分も です・ます調で書かれているため差は保存されたままになっている。文体差は既存の持ち込みで本 change が作ったものではないが、「ja を正として書き直す」対象に入っていたファイルなので、統一の機会だった。

**推奨修正**: である調へ揃える (別 change に送るのも可)。ここは判断を要するのでオーナーに委ねる。

### [🔵 Suggestion] MAUI の C# コード例に `var` / 例の完全性の揺れ

**該当箇所**: `skills/ja/ksdialogs-maui/references/di-registration.md:75`、`:140`、`:71-84`

**問題点**:
- `:75` は `MauiAppBuilder builder = MauiApp.CreateBuilder();`、`:140` は `Type? viewType = …` と明示型だが、同じ Skill の他の `MauiProgram` 例 (`dialogs.md:153`、`loading.md:228`、`toast.md:80`) はすべて `var builder = …`。summary は「明示型は型推論に寄せる」を施術内容に挙げている
- `:71-84` の `MauiProgram` 例だけが `builder.UseMauiApp<App>();` を欠いており、他の 5 つの `MauiProgram` 例には入っている。登録に焦点を当てた抜粋としては許容範囲だが、同じ Skill 内で揃っていない

**推奨修正**: `var` に寄せ、`UseMauiApp<App>()` を足して他例と揃える。コードブロックなので ja / en 同時に直す。

### [🔵 Suggestion] API 名網羅検査が未実行 — `DialogError` (KMP) が除外リスト外の未掲載名として残っている

**該当箇所**: `skills/ja/ksdialogs-kmp/references/ios-host.md` (掲載先候補)

**問題点**: summary の検査リストに `api-coverage-check.py` が無かったため本レビューで実行したところ、報告 19 行のうち 18 行は `kasane/handbook/cross/user-skill-api-listing.md` の現行除外リストで説明がつくが、`ksdialogs-kmp <- kmp/api/ios-host-integration.md: DialogError` の 1 件だけがリストに無い。`kasane/concepts/kmp/api/ios-host-integration.md:108` が「未登録の共有 ViewModel 型はどちらも構成ミスとして `DialogError` を投げ」と書いているのに対し、KMP Skill には `DialogError` が 1 度も現れない (ja / en とも)。

ただしこれは **本 change 以前からの欠落**で (`git grep DialogError HEAD -- skills/*/ksdialogs-kmp` も 0 件)、本 change が作ったものではない。一方で本 change は ios-host.md に `@Throws` の失敗表を新設しており、掲載するならそこが自然な位置だった。

**推奨修正**: handbook の「してはいけないこと」に従い、エージェントの独断で除外せずオーナー判断へ送る。掲載するなら ios-host.md の `@Throws` 表の周辺へ 1 行。あわせて docs-refresh 実行時の検査セットに `api-coverage-check.py` を含めることを推奨する (今回は判定の材料として初めて回した)。

## アクションプラン

1. **Major の 3 件を先に直す** — KMP `view-models.md:75-78` のレジストリ列の限定、iOS `toast.md` の `viewFactoryTypeMismatch` の表からの除去と地の文への統合、maui / ios / android `layout.md` への `DialogAlignment` 物理方向の 1 行追加。いずれも ja / en 双方
2. **表記統一の徹底 (Major)** — MAUI ja の「ダイアログ」6 箇所を「Dialog」へ。あわせて「(scrim)」3 箇所の削除、「器」/「host」の統一、ja SKILL.md の「## Setup」「最小例 / 最小コード」の統一
3. **コード例の 2 件を直す (Minor)** — iOS `view-models.md:132` の `self.`、MAUI `di-registration.md` の `var` / `UseMauiApp`。コードブロックは ja / en を同時に直し、`code-block-parity-check.py` を再実行
4. **記述漏れ・en 意味ずれをまとめて反映 (Minor)** — Android 4 件、KMP en 3 件、KMP loading.md の iOS メッセージ 1 件、多段表示の限定 2 件、未宣言型 2 件
5. **修正後に検査を再走** — code-block-parity / heading-parity / link-resolution / frontmatter / local-path / identity、加えて `api-coverage-check.py`
6. **オーナー判断へ送る (Suggestion)** — ios-host.md の文体、`DialogError` の掲載可否
