# レビュー結果: rollout-user-docs (015 回目)

**日付**: 2026-09-05
**判定**: APPROVED

## サマリー

`skills/{en,ja}/ksdialogs-maui/**` を、過去の MAUI レビューおよび実装報告を参照せず、change の proposal / design / tasks / delta specs、呼び出し元指定の planned manifest、関連する accepted ADR・concepts・handbook、現行の公開実装・tests・Sample から独立に照合した。Major / Minor finding はない。

MAUI Skill は、既定 facade と注入契約、Dialog の登録・型付き結果・Notifier・型指定/インライン/多段表示、layout、transition、Loading、Toast、MAUI DI の全利用経路を、公開 API と現在の挙動に一致する実行可能な recipe として閉じ込めている。英日版の構造とコードもロックステップである。

## 照合した規約

- `ksn-review` と `ksn-core` のレビュー規律、delta spec、handbook 適用規則、path / domain-axis 規約
- `csharp-impl-skill`、`maui-skill`、`maui-native-binding-skill` の C#、MAUI、iOS / Android binding 規律
- `kasane/changes/rollout-user-docs/{proposal.md,design.md,tasks.md,deviation.md,ui/brief.md}` と `specs/{user-skills,docs-refresh,repository-docs}/spec.md`
- `kasane/handbook/cross/{comment-policy,test-execution,local-development-setup,user-skill-api-listing}.md`
- `kasane/concepts/core/api/` の 8 concept と `kasane/concepts/maui/api/di-registration.md`
- accepted の MAUI ADR 0001 / 0002 / 0003 / 0005、および関連する cross ADR 0002 / 0005 / 0006 / 0011

## 指摘事項

なし。

## 検証証跡

### planned manifest と source completeness

- 判定の正にはリポジトリ内の bootstrap manifest ではなく、呼び出し元指定の `docs-refresh-ksdialogs-manifest-planned.json` を使用した。
- planned manifest の MAUI 部分は 8 target key であり、現行の en / ja 各 8 ファイル、合計 16 ファイルと過不足なく一致する。Skill ディレクトリにも規定外ファイルはない。
- root `SKILL.md` は core の 8 concept と MAUI DI concept の計 9 source を持つ。7 references は `dialogs.md` が multi-display / registration-show / result-notification の 3 source、残る 6 ファイルが model-binding / layout / transition / loading / toast / MAUI DI を各 1 source として持つ。各 source の利用者向け公開面と重要な挙動を、対応ファイル単位で現物照合した。
- `DOCS_REFRESH_MANIFEST=... concepts-coverage-check.py` は `concepts coverage OK`。全 concept が target または明示除外へ配置され、planned manifest 内の削除済み source もない。

### API / Setup / 挙動

- `maui/KsDialogs.Maui/KsDialogs.Maui.csproj` の package ID `KsDialogs.Maui`、version `0.1.0`、.NET 10、iOS 17、Android API 24 と Setup 記載が一致する。
- 公開 facade / contract / registry / attached property / transition / hosting extension の署名を全 recipe と照合した。特に `DialogResult<TResult>`、Notifier、typed / inline show、10 個の layout 添付名（`ProportionalHeight` を含む）、preset と custom transition、Loading の imperative / scoped / custom 経路、同期 fire-and-forget Toast、`AddKsDialogs` と 3 種の `RegisterFor*` が一致する。
- Android / iOS の platform gateway は MAUI View の変換と値の転送に限定され、提示先・多段表示・Loading 合流・Toast 多重表示を Native host へ委譲する。Skill の thread、結果配送、失敗、重なり、非対話の説明と矛盾しない。
- `samples/maui/KsDialogs.Sample.Maui` は `AddKsDialogs`、3 種の `RegisterFor*`、型指定/インライン Dialog、layout / transition、Loading、Toast の通常 consumer 経路を facade 参照だけで使用している。
- 公開 consumer 面の限定コンパイル:

  ```text
  dotnet build maui/KsDialogs.Maui.ApiSurfaceCheck/KsDialogs.Maui.ApiSurfaceCheck.csproj --no-restore -f net10.0
  成功: 警告 0、エラー 0
  ```

- 関連 15 test class を 2 回の filter 実行で限定検証した。Dialog の registry / typed / inline / DI / layout / transition / notifier / delivery、Loading の facade / DI / passthrough / action、Toast の facade / DI / passthroughを含み、合計 101 件が合格、失敗 0、skip 0 だった。製品コードと tests に変更がないため、proposal の明示免除に従い全 build root / 全 suite は実行していない。

### API-name coverage の exact 分類

- planned manifest を指定した `api-coverage-check.py` の MAUI 報告を、`user-skill-api-listing.md` の exact token と 1 件ずつ照合した。41 unique token は全件が判断済みである。
- 内部層 / interop 層は `LoadingCoordinator`、`IMauiInitializeService`、機械的に導出できる名前は `IServiceProvider`、`IServiceProvider.GetService`、`TimeSpan.MaxValue`、低頻度の細部 API は `DialogViewRegistry.Shared` である。
- 残る 35 token は別 platform、旧 AiForms、concept の数式・case ID・検証来歴に由来する対象 Skill 外の検出である。MAUI の正規公開面へ追加すべき未掲載 API はない。

### 構造 / 英日 / 閉世界

- `frontmatter-check.py`: `frontmatter OK`。許可 field、同一 name、language、MIT license、repository source URL を満たし、ja description に `KsDialogs`、`.NET MAUI`、API 名など発火用英語 keyword がある。
- `heading-parity-check.py`: `en/ja heading structure OK`。
- `code-block-parity-check.py`: `code blocks byte-identical`。
- `link-resolution-check.py`: `All internal links resolve`。
- `scripts/local-path-lint.py` と `scripts/identity-lint.py`: 違反 0 件。
- 16 ファイルに、Skill 外へのリンク（frontmatter の配布元 URL を除く）、local absolute path、Kasane / ADR / change-id、内部 gateway / bridge 名、旧ブランド名はない。コードブロック内コメントも 0 件である。

## アクションプラン

なし。
