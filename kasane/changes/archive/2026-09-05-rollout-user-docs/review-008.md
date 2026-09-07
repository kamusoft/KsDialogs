# レビュー結果: rollout-user-docs (008 回目)

**日付**: 2026-09-05
**判定**: CHANGES_REQUESTED

## サマリー

Android 利用者向け Skill の en / ja 一式を、planned manifest が割り当てる 8 concept、change の proposal / design / tasks / deviation / delta spec / UI brief、関連 handbook・accepted ADR、Android の公開実装・テスト・Sample と突き合わせた。構成、frontmatter (`license` / `metadata.source` を含む)、ja 版の発火語、閉世界性、en / ja の見出し構造とコード byte 一致、内部リンク、planned manifest の concept 網羅は合格した。ファイル単位の源泉割当にも不足は見つからなかった。

一方、Setup が配布 artifact の accepted な依存契約と Compose の依存規律に反し、公開 API 2 件が Skill のどこからも発見できない。利用者が Compose 用依存を正しく宣言できず、Toast の inline Compose 経路とダイアログの縦比率指定へ到達できないため、Major 3 件として修正を求める。

製品コード・テストに変更がないため、全ビルドルート実行は `proposal.md` の合意済み例外に従って省略した。今回の判定はビルド未実施を理由とするものではない。

## 照合した規約

- `kasane/handbook/cross/user-skill-api-listing.md` — 公開 API は簡潔でも網羅し、現行除外リストにない未掲載名を独断で除外しない
- `kasane/handbook/cross/test-execution.md` — Android の検査範囲と実行証跡の扱い
- `kasane/changes/rollout-user-docs/specs/user-skills/spec.md` — Skill 構成、完動レシピ、閉世界性、翻訳ロックステップ、manifest、4 層レビュー
- `kasane/changes/rollout-user-docs/design.md` Decision 3 — Android の各ファイルへ割り当てる 8 concept
- `kasane/decisions/android/0001-compose-api-separate-module.md` — Compose 消費者が手で宣言する KsDialogs artifact は `ksdialogs-compose` 1 点
- `kotlin-impl-skill` / `jetpack-compose-impl-skill` — Kotlin・Compose コード例、状態収集、副作用、依存宣言のレビュー規律

## 検査証拠

- planned manifest を使う concept 網羅検査: `concepts coverage OK`
- Android en / ja 7 ファイル組の見出し構造検査: `en/ja heading structure OK`
- Android en / ja 7 ファイル組のコードブロック比較: `code blocks byte-identical`
- frontmatter、内部リンク、local-path lint、identity lint、閉世界語検査: 違反 0 件
- planned manifest の Android target は、`SKILL.md` に 8 concept、各 reference に design Decision 3 の機能別 concept を割り当てており、現在の本文の源泉と一致

## 指摘事項

### [Major] Compose 利用時の依存宣言が artifact の推移契約と BOM 規律に反する

**該当箇所**: `skills/en/ksdialogs-android/SKILL.md:27-36`、`skills/ja/ksdialogs-android/SKILL.md:27-36`、`kasane/decisions/android/0001-compose-api-separate-module.md:14-15,28`、`android/ksdialogs-compose/build.gradle.kts:57-65`

**問題点**: Skill は Compose 利用者へ `ksdialogs` と `ksdialogs-compose` の両方を手で宣言させ、「Compose artifact は基本 artifact を置き換えない」と説明している。しかし accepted ADR は `ksdialogs-compose` が本体へ依存し、本体は推移的に自動解決されるため、Compose 利用者が手で書く KsDialogs 依存は `ksdialogs-compose` 1 点と明記している。実装も `api(project(":ksdialogs"))` でこの契約を実現している。現状の説明では View-only と Compose の最小依存が区別されず、公開後の利用者へ冗長で契約と異なる Setup を教える。加えて Foundation と Material 3 を個別バージョンで固定しており、Compose ライブラリを BOM で揃える `jetpack-compose-impl-skill` の依存規律にも合わない。

**推奨修正**: View-only は `jp.kamusoft:ksdialogs`、Compose は `jp.kamusoft:ksdialogs-compose` のみを手で宣言し、本体が推移解決されることが分かるよう Setup を分ける。Compose Foundation / Material 3 は適合する Compose BOM を platform 依存として宣言し、各 artifact はバージョンなしで揃える。en / ja を同時に更新する。

### [Major] Toast の inline Compose 公開入口 `KsToast.showCompose` が発見不能

**該当箇所**: `skills/en/ksdialogs-android/references/toast.md:48-99`、`skills/ja/ksdialogs-android/references/toast.md:48-99`、`android/ksdialogs-compose/src/main/kotlin/jp/kamusoft/ksdialogs/compose/ComposeToastShow.kt:21-29`、`kasane/concepts/core/api/toast-semantics.md:36-47`、`kasane/handbook/cross/user-skill-api-listing.md:16-20,100-104`

**問題点**: Toast recipe は登録経路では Compose を示すが、「登録せずにカスタムコンテンツを表示する」節は View factory だけを示し、公開 extension `KsToast.showCompose` の名前も呼び出し方も Android Skill 全体に一度も現れない。concept は inline factory と View / Compose の技術別オーバーロードを契約化し、実装もレジストリを変更しない Compose 専用入口を公開している。`showCompose` という文字列が Dialog / Loading の別レシピにあるだけでは Toast の入口を導出できず、「簡潔でも網羅」および機能入口から到達可能にする handbook 規約を満たさない。

**推奨修正**: Toast の inline 節に `jp.kamusoft.ksdialogs.compose.showCompose` と `Toast.instance.showCompose(...)` を明記し、登録済み Toast と同じ duration / placement / 非対話 / 多重表示の契約で、レジストリを変更しないことが分かる完動コードを追加する。en / ja のコードブロックは byte 一致させる。

### [Major] 公開レイアウト API `DialogOptions.proportionalHeight` が未掲載のまま owner 除外にもない

**該当箇所**: `skills/en/ksdialogs-android/references/layout.md:16-31,51-59`、`skills/ja/ksdialogs-android/references/layout.md:16-31,51-59`、`android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/DialogOptions.kt:19-30`、`kasane/concepts/core/api/layout-semantics.md:39-56`、`kasane/handbook/cross/user-skill-api-listing.md:18-20,34-56,102-104`

**問題点**: layout recipe は Compose / View の両例で `proportionalWidth` だけを掲載し、公開プロパティ `proportionalHeight` は Android Skill 全体に一度も現れない。concept と実装は幅・高さを対になる公開機能として定義している。現在の Android owner 除外リストにも `proportionalHeight` はなく、未掲載 API を低頻度などの理由で独断除外してはならない規約に該当する。利用者のエージェントは高さ比率によるサイズ指定の存在を発見できない。

**推奨修正**: layout のリード文または完動コードへ `proportionalHeight` と幅と同じ正規化規則を掲載する。掲載しない判断を行う場合は、先にオーナー判断と handbook の現行除外リスト更新を行う。

## アクションプラン

1. Android Setup を View-only / Compose の推移依存契約に合わせ、Compose 依存を BOM 管理へ直す。
2. Toast の inline Compose 入口と `DialogOptions.proportionalHeight` を en / ja にロックステップで掲載する（または後者について根拠付きのオーナー除外判断を記録する）。
3. planned manifest で機械検査一式と API 名網羅報告を再実行し、修正後の Android Skill を独立再レビューする。
