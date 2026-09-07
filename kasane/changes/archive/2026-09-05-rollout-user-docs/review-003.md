# レビュー結果: rollout-user-docs (003 回目)

**日付**: 2026-09-05
**判定**: CHANGES_REQUESTED

## サマリー

.NET MAUI Skill は、規定のファイル構成、frontmatter、閉世界性、en / ja の見出し構造・コード byte 一致・意味等価、Setup の配布情報、主要 API の説明について概ね整合している。一方、型指定 show の非同期 configure レシピがそのままでは必ず構成ミスで失敗し、Loading の源泉 concept が定めるインライン factory 経路と進捗フォーマットが利用者から到達できないため、Major 2 件として修正を求める。

## 照合した規約

- `kasane/handbook/cross/user-skill-api-listing.md` — 対象が `skills/**` であり、公開 API の掲載範囲とコード例コメント規約を照合した
- `kasane/handbook/cross/test-execution.md` — MAUI / Native bridge の通常の検証ルートを確認した。今回は proposal の合意済み全ビルド省略を適用した

## 検証証拠

- `skills/{en,ja}/ksdialogs-maui/` は各言語とも `SKILL.md` + 規定の references 7 本だけで、相対パス構成は一致していた。
- task 2.6 の草案 manifest にあるファイル別源泉を全件照合した。`SKILL.md` は registration-show / loading / toast / MAUI DI、`dialogs.md` は multi-display / registration-show / result-notification、`view-models.md` は model-binding、残る機能別 references と `di-registration.md` は各同名 concept に対応し、内容が依拠する concept の未登載は認めなかった。
- `heading-parity-check.py` は `en/ja heading structure OK`、`code-block-parity-check.py` は `code blocks byte-identical`、`frontmatter-check.py` は `frontmatter OK`、`concepts-coverage-check.py` は `concepts coverage OK`、`link-resolution-check.py` は `All internal links resolve` で終了した。追加検査でも en / ja の `license: MIT` と `metadata.source: https://github.com/kamusoft/KsDialogs` は一致した。
- ja 版 description は日本語本文に `.NET MAUI`、`KsDialogs`、`API`、`Dialog`、`Loading`、`Toast`、`dependency injection`、`layout`、`transition` を併記しており、発火に使える英語キーワードを満たす。
- ローカル絶対パス、Skill 外への相対参照、`kasane/`、ADR 番号、change-id、interop 型名、コードコメントは対象 16 ファイルで 0 件だった。外部 URL は frontmatter の必須 `metadata.source` だけである。
- Setup の `KsDialogs.Maui` 0.1.0、.NET 10、iOS 17、Android API 24、`KsDialogs` namespace は `maui/KsDialogs.Maui/KsDialogs.Maui.csproj` と公開コードに一致する。Native bridge も Loading の style / placement と Toast の非対話経路を各プラットフォームへ委譲しており、Skill の該当説明と矛盾しない。
- Dialog / Loading / Toast / DI の公開実装、MAUI tests、Sample を静的に突き合わせた。全ビルド・全テストは、`proposal.md` Impact の「製品コード・テストに触れないため省略する」合意と task 9.1 に従い実行していない。
- `ui/brief.md` の対象は README 用スクリーンショットであり、この Skill の本文・コード例には UI 承認物との照合対象がない。`deviation.md` の 5 件にも、この Skill の説明と衝突する合意済み差分はない。

## 指摘事項

### [🟠 Major] 非同期 configure のレシピが必要な factory 登録を欠き、表示に到達しない

**該当箇所**: `skills/en/ksdialogs-maui/references/view-models.md:60`、`skills/ja/ksdialogs-maui/references/view-models.md:60`

**問題点**: レシピは `Dialog.Instance.ShowAsync<AccountViewModel>(...)` を呼ぶが、`AccountViewModel` の ViewModel factory と View factory をどこにも登録していない。公開実装は型指定 show で明示登録も fallback も無い場合に `DialogException.ViewModelFactoryNotRegistered` を投げる (`maui/KsDialogs.Maui/Internals/DialogResolution.cs:52`)。この失敗は `maui/KsDialogs.Maui.Tests/DialogTypedShowTests.cs:81` でも固定されている。直前のレシピが登録するのは別型 `ProfileViewModel` であり、前節に依存させても救えないため、user-skills spec の「完動コード」を満たさない。

**推奨修正**: 同じコードブロックに `AccountViewModel` の View factory と ViewModel factory の登録を追加するか、`RegisterForDialog` を含む完結した DI 構成へ置き換える。en / ja の対応コードブロックは byte 一致を保ち、非同期 configure 完了後に content factory が実行されることを読者が確認できる形にする。

### [🟠 Major] Loading の源泉にあるインライン factory 経路と進捗フォーマットが欠落している

**該当箇所**: `skills/en/ksdialogs-maui/references/loading.md:45`、`skills/en/ksdialogs-maui/references/loading.md:81`、`skills/ja/ksdialogs-maui/references/loading.md:45`、`skills/ja/ksdialogs-maui/references/loading.md:81`

**問題点**: 源泉 `kasane/concepts/core/api/loading-semantics.md:62` は `LoadingStyle` の公開項目として進捗フォーマット関数を定め、同 `:70` はカスタム View の show / start に登録経路とインライン factory 版の双方を定める。しかし reference は登録済み ViewModel の `StartAsync` と、`ProgressFormat` を除く Style 設定だけを示している。実装には `IKsLoading.ShowAsync<TViewModel>(..., factory, ...)` (`maui/KsDialogs.Maui/Presentation/IKsLoading.cs:78`)、インライン `StartAsync` 2 形 (`:159`、`:177`)、`LoadingStyle.ProgressFormat` (`maui/KsDialogs.Maui/Contract/LoadingStyle.cs:44`) があり、インライン経路が registry を変更しないことと formatter の Native 委譲も tests で固定されている。能力マップが案内する「custom content」「progress styling」から、この distinct な利用経路と設定へ到達できない。

**推奨修正**: `loading.md` に、登録を変更せずに custom Loading を show または start するインライン factory の完動レシピを追加し、built-in content の設定例に `ProgressFormat` の関数形と未報告時 nullable progress の扱いを含める。見出し構造とコードブロックは en / ja で同期する。

### [🟡 Minor] Loading / Toast の MAUI DI 糖衣が未掲載かつ未除外のままである

**該当箇所**: `skills/en/ksdialogs-maui/SKILL.md:24`、`skills/en/ksdialogs-maui/references/di-registration.md:1`、`skills/ja/ksdialogs-maui/SKILL.md:24`、`skills/ja/ksdialogs-maui/references/di-registration.md:1`

**問題点**: 公開実装には `RegisterForLoading<TView,TViewModel>` と `RegisterForToast<TView,TViewModel>` があり (`maui/KsDialogs.Maui/Hosting/KsDialogsServiceCollectionExtensions.cs:110`、`:145`)、専用 tests と MAUI Sample もこの入口を使用するが、Skill 内に両 API 名が一度も現れない。`user-skill-api-listing.md` の現行除外リストは未初期化であり、独断で除外できない。一方、現在の `maui/api/di-registration.md` は `RegisterForDialog` だけを規範化しているため、これは task 6.3 でオーナー判断へ送るべき公開面と concept のずれでもある。

**推奨修正**: task 6.3 で両 API を掲載対象か意図的除外か明示的に判断する。掲載する場合は DI 能力マップまたは `di-registration.md` から Loading / Toast の 1 行登録へ到達可能にし、除外する場合は根拠と各 API 名を handbook の現行除外リストへ記録する。

## アクションプラン

1. 非同期 configure レシピを、必要な View / ViewModel factory を含む完動コードへ修正する。
2. Loading reference にインライン factory 経路と `ProgressFormat` を追加する。
3. task 6.3 で `RegisterForLoading` / `RegisterForToast` の掲載・除外をオーナー判断し、結果を反映する。
4. en / ja の見出し・コード byte、frontmatter、閉世界性、内部リンクの検査を再実行し、修正後の .NET MAUI Skill を再レビューする。
