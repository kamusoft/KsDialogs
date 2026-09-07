# レビュー結果: rollout-user-docs (020 回目)

**日付**: 2026-09-05
**判定**: APPROVED

## サマリー

`review-018.md` の `LoadingConfig.Opacity` に関する Minor と、修正後の英日 `api-mapping.md`、移植元 Android / iOS 実装、現行 `KsDialogs.Maui` 公開面を再照合した。修正は旧値が overlay・indicator・message 全体へ作用したこと、現行には単一の同値 API がないこと、3 色への alpha 反映は近似であり overlay だけでは他 2 要素が不透明になることを英日で等しく説明しており、指摘は解消している。

対象 4 ファイル全体も再確認し、公開 API 対応、英日ロックステップ、closed-world、planned manifest の source completeness、実行可能性に新しい問題はない。

## 照合した規約

- `cross/comment-policy.md` — 利用者向けコード例のコメントと自己完結性
- `cross/test-execution.md` — 限定検証の実行件数報告と、proposal で合意済みの全 suite 免除
- `cross/local-development-setup.md` — MAUI の検証入口
- `cross/user-skill-api-listing.md` — 「簡潔でも網羅」と 3e 候補の確定済み分類
- `cross/aiforms-origin-reference.md` — README を先に、挙動差は移植元コードを正として確認
- accepted `cross/ADR-0011`、core API concepts 8 本、`maui/api/di-registration.md`
- planned manifest `/tmp/docs-refresh-ksdialogs-manifest-planned.json` — root 8 source、`references/api-mapping.md` 9 source のファイル単位完全性

## 指摘事項

なし。`review-018.md:23` の Minor は解消済み。

### 解消確認: `LoadingConfig.Opacity`

- 修正箇所: `skills/en/ksdialogs-aiforms-migration/references/api-mapping.md:83`、`skills/ja/ksdialogs-aiforms-migration/references/api-mapping.md:83`
- 移植元 Android は spinner / message を子に持つ `ContentView` 全体へ alpha を設定する (`../AiForms.Maui.Dialogs/AiForms.Maui.Dialogs/Loading/DefaultLoading.Android.cs:134`、`:135`、`:153`)
- 移植元 iOS は spinner / message を子に持つ `OverlayView` 全体へ alpha を設定する (`../AiForms.Maui.Dialogs/AiForms.Maui.Dialogs/Loading/DefaultLoading.iOS.cs:64`、`:134`、`:149`)
- 現行側は overlay の `DialogOptions.OverlayColor` (`maui/KsDialogs.Maui/Internals/DialogOptions.cs:40`) と、`LoadingStyle.IndicatorColor` / `MessageColor` (`maui/KsDialogs.Maui/Contract/LoadingStyle.cs:22`、`:28`) を独立して公開する
- 修正文はこの差をそのまま説明し、「単一の対応先なし」と 3 色への近似を区別している。英日両行の inline-code 識別子集合は `LoadingConfig.Opacity` / `DialogOptions.OverlayColor` / `LoadingStyle.IndicatorColor` / `LoadingStyle.MessageColor` で完全一致

## 検証証跡

- 英日 `api-mapping.md` は各 97 API 対応行、inline-code token は各 271 件。見出し構造とコードブロックも一致
- `DOCS_REFRESH_MANIFEST=/tmp/docs-refresh-ksdialogs-manifest-planned.json` を明示した `concepts-coverage-check.py` / `heading-parity-check.py` / `code-block-parity-check.py` / `frontmatter-check.py`: 全件成功
- 対象 4 ファイルを明示した `link-resolution-check.py`: `All internal links resolve`
- `scripts/local-path-lint.py` / `scripts/identity-lint.py`: 違反なし
- `api-coverage-check.py` の migration 候補を `cross/user-skill-api-listing.md` の現行除外リストと再照合し、未分類候補なし
- `dotnet build ../AiForms.Maui.Dialogs/AiForms.Maui.Dialogs/AiForms.Maui.Dialogs.csproj --no-restore -f net10.0`: 成功、警告 0、エラー 0
- `dotnet build maui/KsDialogs.Maui.ApiSurfaceCheck/KsDialogs.Maui.ApiSurfaceCheck.csproj --no-restore -f net10.0`: 成功、警告 0、エラー 0
- `dotnet test maui/KsDialogs.Maui.Tests/KsDialogs.Maui.Tests.csproj --no-restore -f net10.0 --filter 'FullyQualifiedName~DialogDependencyInjectionTests|FullyQualifiedName~DialogInlineShowTests|FullyQualifiedName~DialogTypedShowTests|FullyQualifiedName~DialogLayoutPassthroughTests|FullyQualifiedName~DialogTransitionPassthroughTests|FullyQualifiedName~LoadingFacadeTests|FullyQualifiedName~ToastFacadeTests'`: 61 件成功、失敗 0、スキップ 0
- 全ビルドルートの全 suite は製品コード・テスト無変更のため、`proposal.md:43` の合意済み免除に従って省略

## アクションプラン

なし。
