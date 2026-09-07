# レビュー結果: rollout-user-docs (004 回目)

**日付**: 2026-09-05
**判定**: CHANGES_REQUESTED

## サマリー

KMP Skill の構成、frontmatter、英日ロックステップ、iOS の前提 1 + 手順 3、`@Throws` の経路表、Dialog / Loading / Toast の Native View 登録は概ね仕様どおりであり、公開 API surface-check の Android / iOS コンパイルも成功した。一方、ViewModel 主導の結果報告と Android Compose 登録が閉世界の完動レシピになっておらず、多段表示の既知の OS 差も結果契約まで伝えていない。また、予定 manifest の `view-models.md` の源泉割当が本文に対して不足しているため、独立レビューの合格条件を満たさない。

## 照合した規約

- `kasane/handbook/cross/user-skill-api-listing.md` — `skills/**` の公開 API 掲載とコード例の規約
- `kasane/handbook/cross/test-execution.md` — 変更に応じた検査経路。製品コード・テスト不変更時の全ビルドルート免除は `proposal.md` の合意済み例外を優先
- `kasane/changes/rollout-user-docs/specs/user-skills/spec.md` — Skill 構成、閉世界性、レシピ形式、KMP Setup、`@Throws`、翻訳ロックステップ、レビュー 4 層
- `kasane/changes/rollout-user-docs/ui/brief.md` / `deviation.md` — KMP Skill 自体に画像・新規 UI はなく、承認済みスクリーンショット差分と本レビュー範囲の衝突なし
- Kotlin 実装規律 — null 安全、公開面、coroutine、テスト、hygiene の観点でコード例を確認

## 源泉 concept 照合

| 源泉 | 照合結果 |
|---|---|
| `core/api/registration-show-semantics.md` | Native View の3機能登録、既定 entry、型付き show は一致。Compose 登録は完動レシピ不足 (Major 2) |
| `core/api/result-notification-semantics.md` | `DialogResult`、notifier の一度だけの確定、呼び出し元キャンセルは一致 |
| `core/api/multi-display-semantics.md` | 重ね出し・後入れが手前・結果の独立は一致。下段先閉じの結果差が欠落 (Major 3) |
| `core/api/model-binding-semantics.md` | accessor 名と表示中だけ取得可能という説明は一致。実行可能な host レシピが欠落 (Major 1) |
| `core/api/layout-semantics.md` | placement の object 全体置換、符号、単位、静的属性の host 所有は一致。message 経路の案内漏れあり (Minor 1) |
| `core/api/transition-semantics.md` | commonMain は選択だけを運び、Native content へ添付し、撤去後に結果配送する説明は一致 |
| `core/api/loading-semantics.md` | show / hide / setMessage / start、合流、進捗、custom fail-fast は一致。Swift 境界用 wrapper 例に注釈不足 (Minor 3) |
| `core/api/toast-semantics.md` | fire-and-forget、duration、重なり、touch-through、custom 経路、Loading 前面は一致 |
| `kmp/api/ios-host-integration.md` | Maven 1 点、合成 package、直接 SwiftPM 1 点、3機能登録は一致。初回後の自動更新と再実行案内が不一致 (Minor 2) |

## 指摘事項

### [🟠 Major] ViewModel 主導の結果報告が閉世界の完動レシピになっていない

**該当箇所**: `skills/en/ksdialogs-kmp/references/view-models.md:38`、`skills/ja/ksdialogs-kmp/references/view-models.md:38`、`skills/en/ksdialogs-kmp/references/android-host.md:21`、`skills/en/ksdialogs-kmp/references/ios-host.md:35`

**問題点**: `view-models.md` は Android の `viewModel.notifier` と iOS の `Dialog.shared.kmp.notifier(for:result:)` を挙げた後、「登録と notifier のコードは host レシピに置く」と案内する。しかし両 host レシピにある Dialog 登録はすべて notifier を factory の第2引数で受ける形であり、1引数 factory から accessor を取得するコードはどこにもない。`view-models.md` の第2節自体にもコードがなく、「やりたいこと見出し + リード + 完動コード」という Requirement を満たさない。特に iOS accessor は `throws` かつ非 Boolean 結果では `result:` が必要なので、名前だけでは安全な呼び方を復元できない。

**推奨修正**: host 側2ファイルの Dialog 登録例の少なくとも1つを1引数 factory にし、Android は `viewModel.notifier`、iOS は `try? Dialog.shared.kmp.notifier(for:result:)` を使って結果を報告する完動例にする。非 Boolean の `result:` 指定と accessor の結果型も一致させ、`view-models.md` から実在するそのレシピへ到達できる状態にする。機能別ファイルへ host 登録を移さないという分担は維持する。

### [🟠 Major] Android Compose 節が依存宣言だけで登録を実行できない

**該当箇所**: `skills/en/ksdialogs-kmp/references/android-host.md:55`、`skills/ja/ksdialogs-kmp/references/android-host.md:55`

**問題点**: 能力マップは「Android Views or Compose content を登録する」レシピとしてこのファイルを案内し、当該節も各 registry の `registerCompose` を使うよう求めるが、コードブロックは `ksdialogs-compose` の依存宣言だけである。実装上は Dialog が `(VM, DialogNotifier<R>)` または `(VM)`、Loading / Toast が `(VM)` という異なる overload を持ち、import も必要になるため、この Skill だけでは Compose content を登録できない。reference の各節を完動コードのレシピにする Requirement と、KMP Skill を閉世界にする Requirement に反する。

**推奨修正**: `jp.kamusoft.ksdialogs.compose.registerCompose` を import し、Dialog / Loading / Toast の3 registry に共有 ViewModel を登録する、コンパイル可能な Compose 例を追加する。レイアウト属性を扱うなら `KsDialogAttributes` も実 API に沿って示す。

### [🟠 Major] 下段 Dialog を先に閉じた場合の既知の結果差を「見え方」だけに縮退させている

**該当箇所**: `skills/en/ksdialogs-kmp/references/dialogs.md:24`、`skills/ja/ksdialogs-kmp/references/dialogs.md:24`

**問題点**: 文書は「host platform が見え方を決める」とだけ書くが、源泉 contract と同名の Native tests が固定する差は表示だけではない。アプリコードが保持した下段 notifier を先に確定した場合、iOS は提示の連なりごと外れて上段 show も `cancelled`、Android は下段だけ閉じて上段 show が表示中のまま残る。現在の閉世界 Skill では、このコード経路で上段の結果が platform 間で変わることを利用者が知れない。

**推奨修正**: この状況はユーザー操作では起こらず、保持した下段 notifier へのアプリコードからの報告でだけ起こることを明記した上で、iOS / Android の見え方と上段 show の結果を簡潔な表または2文で示す。

### [🟠 Major] `view-models.md` の manifest 源泉が本文の3契約を追跡しない

**該当箇所**: `skills/en/ksdialogs-kmp/references/view-models.md:1`、`skills/ja/ksdialogs-kmp/references/view-models.md:1`

**問題点**: 予定 manifest はこのファイルの源泉を `core/api/model-binding-semantics.md` 1本だけとしている。一方、本文は `KsDialogs` の既定 entry と契約注入、`KsLoading.start` と既定 entry、`KsToast.show` と既定 entry をレシピの中心にしており、それぞれ `registration-show-semantics.md`、`loading-semantics.md`、`toast-semantics.md` の公開面に依拠する。現状のまま最終 manifest を書くと、Loading / Toast または Dialog の入口契約が変わってもこのファイルが docs-refresh の要追従対象にならない。

**推奨修正**: 予定 manifest の `ksdialogs-kmp/references/view-models.md` に少なくとも `core/api/registration-show-semantics.md`、`core/api/loading-semantics.md`、`core/api/toast-semantics.md` を追加し、修正後の予定 manifest で網羅・構造・リンク検査を再実行する。

### [🟡 Minor] layout レシピが message Loading / Toast の placement 経路を除外して読める

**該当箇所**: `skills/en/ksdialogs-kmp/references/layout.md:3`、`skills/ja/ksdialogs-kmp/references/layout.md:3`

**問題点**: lead は `DialogPlacement` を渡せる対象を Dialog、custom Loading、custom Toast としているが、公開 KMP 面は `Loading.show(message, placement)` / `Loading.start(message, placement, action)` と `Toast.show(message, durationMs, placement)` にも placement を持つ。後者は `toast.md` の例に現れるものの、配置レシピ単体では内蔵 Loading / message Toast では使えないように読める。

**推奨修正**: 「Dialog と、message/custom を問わない Loading / Toast の placement 引数」に直し、host content 添付が存在しない message 経路では host の既定配置を上書きすることが分かる表現にする。

### [🟡 Minor] `integrateLinkagePackage` の再実行案内が初回後の自動更新契約と食い違う

**該当箇所**: `skills/en/ksdialogs-kmp/references/ios-host.md:16`、`skills/ja/ksdialogs-kmp/references/ios-host.md:16`

**問題点**: 源泉 concept は消費者で `integrateLinkagePackage` を初回1回実行し、その後の依存変更では Gradle build が合成 package を自動更新すると定める。reference は KsDialogs linkage の変更時にも手順2を再実行するよう案内しており、手動再実行が必要な条件を増やしている。Sample の合成 package を明示的に作り直す command と、通常の消費者更新を混同している。

**推奨修正**: 通常の消費者は初回1回、その後は Gradle build が更新する、と源泉どおり明記する。後続 command は repository Sample の合成 package を明示的に再生成する場合だけの例として分離する。

### [🟡 Minor] 内蔵 Loading の公開 wrapper 例が同 Skill の Swift 境界規則を満たさない

**該当箇所**: `skills/en/ksdialogs-kmp/references/loading.md:8`、`skills/en/ksdialogs-kmp/references/loading.md:26`、`skills/ja/ksdialogs-kmp/references/loading.md:8`、`skills/ja/ksdialogs-kmp/references/loading.md:26`

**問題点**: `download()` と `synchronize()` は public になる top-level suspend wrapper だが `@Throws` がない。message 版の library route 自体に `DialogException` 宣言がないことと、処理・suspend 呼び出しから外へ出る `CancellationException` 等を Swift 向け wrapper で宣言することは別である。SKILL.md と iOS host reference は「Swift から呼び、例外が外へ出る共有関数は suspend / 非 suspend を問わず `@Throws` が必要」と説明しているため、コピー可能なコード例がその規則を自ら満たしていない。

**推奨修正**: Swift から呼ぶ recipe として示すなら、実際に外へ出しうる例外を `@Throws` に列挙する。commonMain 内部だけの helper を意図するなら可視性を下げるか、Swift 境界ではないことを明記して誤用を防ぐ。

## 実施した検査

- 予定 manifest を使った docs-refresh 検査: concepts coverage、en/ja 見出し構造、コードブロック byte 一致、frontmatter、内部リンクはすべて合格
- KMP Skill 限定の閉世界 grep、ローカル絶対パス lint、identity lint、配信識別子表記ゆれ grep: 違反 0 件。外部 URL は `metadata.source` と SwiftPM 配布座標のみ
- frontmatter 目視: en/ja とも `license: MIT`、`metadata.language`、`metadata.source` が適合。ja description に `KsDialogs`、`Kotlin Multiplatform (KMP)`、`commonMain`、`Android`、`iOS`、`Dialog`、`Loading`、`Toast` を確認
- en/ja 18ファイルを対で通読し、機械検査対象外の本文も意味等価であることを確認
- KMP commonMain 公開面、Android Native / Compose 登録実装、iOS KMP 登録実装、同名 API surface / behavior tests、KMP Sample を照合
- `kmp` で `:api-surface-check:compileKotlinIosSimulatorArm64` と `:api-surface-check:compileAndroidMain` を offline 実行: `BUILD SUCCESSFUL`。macOS arm64 で実行不能な `iosX64Test` の警告のみ
- 製品コード・テストの diff がないことを確認し、全製品ビルドルートと実機試験は `proposal.md` Impact の合意済み免除に従って省略
- API 名網羅検査の KMP 未掲載候補は task 6.3 のオーナー仕分け前であり、本レビューでは独断で除外しない。`KsDialogAttributes` / `DialogNotifier` / `DialogViewRegistry` / `LoadingStyle` / `ksDialogOptions` / `ksDialogPlacement` / `overlayDuration` 等の実 API 候補と、他 platform・内部型・fixture 名の誤検出候補が混在している

## アクションプラン

1. Major 1・2を直し、host recipe だけで ViewModel accessor と Compose の3機能登録を実行できるようにする。
2. Major 3の多段表示 OS 差を結果契約まで記載する。
3. Major 4の予定 manifest 源泉を補い、Minor 1〜3を英日同時に修正する。
4. docs-refresh の機械検査一式と API surface-check を再実行し、変更された KMP Skill を独立再レビューへ回す。
