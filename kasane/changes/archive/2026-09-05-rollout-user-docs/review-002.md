# レビュー結果: rollout-user-docs (002 回目)

**日付**: 2026-09-05
**判定**: CHANGES_REQUESTED

## サマリー

Android Skill の en / ja 14 ファイルを、割り当てられた 8 concept、Android / Compose の公開実装、公開面検査、Sample と突き合わせた。frontmatter、言語ペアの見出し構造、コードブロックの byte 一致、内部文書を前提にしない閉世界性は満たしているが、源泉 manifest、公開経路、完動レシピの依存、API 掲載範囲に Major が残るため承認できない。

製品コード・テストには変更がないことを `git status --porcelain=v1 -- android samples core kmp maui ios tests` で確認した。proposal.md の Impact にある合意済み例外を適用し、全ビルドルートは実行せず、ドキュメント検査と公開実装・依存グラフの静的照合を行った。

## 照合した規約

- 利用者向け Skill の API 掲載基準 (`skills/**` をレビューするため)
- テスト実行規約 (製品コード・テスト不変更時の合意済み全ビルド免除を判定するため)
- ksn-review の独立レビュー規律
- kotlin-impl-skill / jetpack-compose-impl-skill の公開 API、coroutine、Compose lifecycle・依存境界の観点

## 検証証拠

- 草案 manifest を指定した `concepts-coverage-check.py` は `concepts coverage OK`。
- 草案 manifest を指定した `frontmatter-check.py`、`heading-parity-check.py`、`code-block-parity-check.py` はそれぞれ `frontmatter OK`、`en/ja heading structure OK`、`code blocks byte-identical`。
- Android Skill の両 `SKILL.md` は同じ `name`、`license: MIT`、言語に合う `metadata.language`、`metadata.source: https://github.com/kamusoft/KsDialogs` を持つ。ja の description は `Android`、`Jetpack Compose`、`KsDialogs`、`Dialog`、`Loading`、`Toast` を含む。
- Android Skill 内の Markdown リンクは同じ Skill ルート内の 6 reference に閉じ、宛先はすべて実在する。ローカル絶対パス、`kasane/`、ADR 番号、interop 機械面の漏出は検出しなかった。
- en / ja の各段落を対応づけて意味を照合し、下記指摘以外の挙動説明は対象 concept と Android 公開実装に一致した。UI brief / deviation に記録されたスクリーンショット統制・合意済み差分とも矛盾しない。
- `api-coverage-check.py` は Android について `DialogViewRegistry` と `DialogException.ValueClassViewModel`、`DialogException.ViewModelFactoryNotRegistered`、`DialogException.ViewModelAlreadyShowing` を未掲載候補として報告した。さらに公開ソースとの手動照合で、concept の表中でバッククォート括りでないため同検査が拾わない `proportionalHeight` なども未掲載と確認した。

## 指摘事項

### [🟠 Major] `SKILL.md` のファイル単位 source が本文の依存先を覆っていない

**該当箇所**: `skills/en/ksdialogs-android/SKILL.md:12`、`skills/ja/ksdialogs-android/SKILL.md:12`、`design.md:78`

**問題点**: レビュー入力の草案 manifest は `targets["ksdialogs-android/SKILL.md"]` に `core/api/registration-show-semantics.md` だけを割り当てている。一方、同ファイルは型付き結果、Loading、Toast を導入文で要約し、能力マップとレシピ振り分けで model binding、layout、transition、multi-display、loading、toast の内容まで記述している (`SKILL.md:12-23,50-55`)。design.md:78 が合格条件とする「各ファイルの内容が依拠する concept が targets にすべて載ること」を満たさず、たとえば loading concept だけが変わった将来の docs-refresh で入口の能力マップが更新対象にならない。

**推奨修正**: 草案および最終 manifest の当該 target に、本文が依拠する `multi-display`、`result-notification`、`model-binding`、`layout`、`transition`、`loading`、`toast` の各 concept を追加する。修正後、ファイル単位の逆引きを再確認する。

### [🟠 Major] Android Compose の未登録 Dialog 表示経路がレシピから欠落している

**該当箇所**: `skills/en/ksdialogs-android/references/dialogs.md:38`、`skills/ja/ksdialogs-android/references/dialogs.md:38`、`kasane/concepts/core/api/registration-show-semantics.md:75`

**問題点**: 対象 concept は Android の宣言的 UI 系を `registerCompose(...)` / `showCompose(...)` と明示し、インライン Compose 表示にも別名 `showCompose` を使うと定めている (`registration-show-semantics.md:75,98-106`)。公開実装にも `KsDialogs.showCompose` が存在する (`android/ksdialogs-compose/src/main/kotlin/jp/kamusoft/ksdialogs/compose/ComposeDialogShow.kt:23`)。しかし dialogs recipe は Compose の登録経路と View のインライン `show` だけで、Compose のインライン経路を名前でもコードでも案内していない。target に紐づく concept の主要な公開経路が翻訳から落ちている。

**推奨修正**: `showCompose` による未登録 Dialog の利用場面と、レジストリを変更しない契約を en / ja の対応箇所へ追加する。コードを追加する場合は言語ペアで byte 一致を維持する。

### [🟠 Major] Setup の依存宣言だけでは掲載した Compose レシピをコンパイルできない

**該当箇所**: `skills/en/ksdialogs-android/SKILL.md:25`、`skills/en/ksdialogs-android/references/loading.md:71`、`skills/ja/ksdialogs-android/SKILL.md:25`、`skills/ja/ksdialogs-android/references/loading.md:71`

**問題点**: Setup が宣言するのは `ksdialogs` と `ksdialogs-compose` だけだが、recipes は `androidx.compose.material3.Button` / `Text` / `CircularProgressIndicator`、`androidx.lifecycle.compose.collectAsStateWithLifecycle`、`kotlinx.coroutines.async` / `coroutineScope` / Flow を直接 import する。`ksdialogs-compose` が利用者へ API 依存として公開するのは core と Compose runtime だけで、Compose UI・lifecycle runtime は implementation であり、Material3 と lifecycle-runtime-compose は依存自体にない (`android/ksdialogs-compose/build.gradle.kts:57-66`)。core の coroutines-android も implementation である (`android/ksdialogs/build.gradle.kts:66-72`)。実際の Android Sample も消費者側で Compose foundation と coroutines-android を別途宣言している (`samples/android/app/build.gradle.kts:46-57`)。そのため、利用者が Setup のとおりに構成して「完動コード」をコピーすると unresolved import になる。

**推奨修正**: recipes が必要とする消費者側依存と Compose 有効化条件を Setup に明記するか、Setup が提供する依存だけで成立するコードへ recipes を組み替える。Material3 と lifecycle-runtime-compose を残す場合は、それぞれの artifact 宣言を含め、en / ja を同じ構造で更新する。

### [🟠 Major] 公開 API 名の未掲載候補が未裁定のままである

**該当箇所**: `skills/en/ksdialogs-android/references/layout.md:18`、`skills/en/ksdialogs-android/references/view-models.md:22`、`skills/ja/ksdialogs-android/references/layout.md:18`、`skills/ja/ksdialogs-android/references/view-models.md:22`

**問題点**: 「利用者向け Skill の API 掲載基準」は、公開 API のプロパティ名・機能名が対応 Skill のどこかに少なくとも一度現れることを要求し、現行除外リストは未初期化である。機械検査で挙がった `DialogViewRegistry` と Android の `DialogException` 各型に加え、layout concept と公開 `DialogOptions` にある `proportionalHeight` (`kasane/concepts/core/api/layout-semantics.md:45`、`android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/DialogOptions.kt:30`) も Android Skill のどこにも現れない。ほかにも `LoadingViewRegistry`、Dialog / Toast の `showCompose` など、現行の公開面に未掲載候補が残る。除外リストに判断済みの名前がないため、これらを意図的な簡略化として扱う根拠はまだない。

**推奨修正**: tasks.md 6.3 のオーナー判断で Android の未掲載候補を公開実装上の利用経路ごとに仕分ける。掲載と決まった名前は能力マップまたは到達可能な recipe に追加し、除外と決まった名前だけを理由つきで handbook の現行除外リストへ記録する。その後 API 名検査と手動の公開面照合を再実行する。

## アクションプラン

1. Compose inline Dialog の欠落と、recipes を成立させる Setup の依存不足を修正する。
2. Android `SKILL.md` の内容に合わせて草案・最終 manifest の source 割当を完全にする。
3. tasks.md 6.3 として Android の未掲載公開 API 候補をオーナー判断し、掲載または除外記録へ反映する。
4. en / ja の frontmatter・見出し・コード byte・意味等価、閉世界性、concepts coverage、API 掲載を再検査し、Android Skill の独立レビューを再実施する。
