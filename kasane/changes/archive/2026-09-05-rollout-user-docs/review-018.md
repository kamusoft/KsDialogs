# レビュー結果: rollout-user-docs (018 回目)

**日付**: 2026-09-05
**判定**: APPROVED

## サマリー

AiForms migration Skill の英日 4 ファイルを、移植元 clone の README・公開ソースと現行 `KsDialogs.Maui` の公開実装・テスト・Sample、change の proposal / design / tasks / delta specs / deviation、planned manifest を正として fresh に照合した。移植元 README が案内する consumer API と、その公開型が実際に露出する追加メンバーは対応表に現れ、public `BindableProperty` は宣言 18 件すべてが型名つきの個別行で分類されている。合意済みの Toast と `IReusableLoading.Hide()` の deviation、英日ロックステップ、closed-world、planned source completeness、実行可能性はいずれも満たす。

対応表の `LoadingConfig.Opacity` は「個別の対応先なし」という分類自体は正しいが、提示した代替が旧実装の全体 alpha と同じ見えを保たない点を説明していない。表示の一部に限る局所的な移行注意であり、API の分類・コンパイル・主要挙動を損なわないため低優先度 Minor とし、判定は APPROVED とする。

## 照合した規約

- `cross/comment-policy.md` — `skills/**` のコード例は原則コメントレス。対象コードブロックにコメントなし
- `cross/local-development-setup.md` — MAUI の製品・Sample の参照方式と検証入口
- `cross/test-execution.md` — 製品コード・テスト無変更時の全 suite 免除と、変更範囲に対応した限定検証
- `cross/user-skill-api-listing.md` — 「簡潔でも網羅」と、3e 未掲載名の確定済み除外分類
- accepted ADR / concepts — `cross/ADR-0011`、core API 8 本、`maui/api/di-registration.md`
- planned manifest `/tmp/docs-refresh-ksdialogs-manifest-planned.json` — `ksdialogs-aiforms-migration/SKILL.md` に transition を含む core 7 本 + MAUI DI の計 8 source、`references/api-mapping.md` にそれら + multi-display の計 9 source が明示され、実内容と一致

## 指摘事項

### [🟡 Minor] `LoadingConfig.Opacity` の代替は旧来の全体 alpha を保存しない

**該当箇所**: `skills/en/ksdialogs-aiforms-migration/references/api-mapping.md:83`、`skills/ja/ksdialogs-aiforms-migration/references/api-mapping.md:83`

**問題点**: 対応表は旧 `LoadingConfig.Opacity` を `DialogOptions.OverlayColor` の alpha に含めるよう案内している。しかし移植元 Android は spinner と message を子に持つ `ContentView` 全体へ `Alpha` を設定する (`../AiForms.Maui.Dialogs/AiForms.Maui.Dialogs/Loading/DefaultLoading.Android.cs:134`、`:135`、`:153`)。iOS も spinner と message を子に持つ `OverlayView` 全体へ alpha を設定する (`../AiForms.Maui.Dialogs/AiForms.Maui.Dialogs/Loading/DefaultLoading.iOS.cs:64`、`:134`、`:149`)。現行側の `DialogOptions.OverlayColor` は背後の overlay 色だけで (`maui/KsDialogs.Maui/Internals/DialogOptions.cs:40`)、indicator と message の色は `LoadingStyle.IndicatorColor` / `MessageColor` として独立している (`maui/KsDialogs.Maui/Contract/LoadingStyle.cs:22`、`:28`)。したがって案内どおりでは背景だけが透過し、旧実装で同時に透過した indicator と message は不透明のままになる。

**推奨修正**: 「単一の同値な対応先はない」と明記し、旧来の見えに近づける場合は `DialogOptions.OverlayColor` に加えて `LoadingStyle.IndicatorColor` と `LoadingStyle.MessageColor` にも alpha を反映すること、または indicator / message の不透明度が変わる視覚差を受け入れることを英日で案内する。

## 検証証跡

- 移植元の `ExtraView` 11 件、`DialogView` 4 件、`LoadingView` 2 件、`ToastView` 1 件、計 18 件の public `BindableProperty` 宣言を抽出し、`ClassName.PropertyName` の完全修飾表記で 18 / 18 件を対応表に確認した。特に `BorderColorProperty` / `BorderWidthProperty` / `DialogNotifierProperty` / `DurationProperty` を含み、欠落は 0 件
- 英日 `api-mapping.md` は各 97 表行で、API 識別子集合と分類が一致。見出し構造とコードブロックも一致し、英日とも root + `references/api-mapping.md` 以外の余剰ファイルなし
- deviation の 2 点を現物確認: 旧公開面に message Toast overload はなく、generic / concrete View route は「対応先なし」、新 message route は新規代替として挙動差 3 点つき。`IReusableLoading.Hide()` は README の `void` 表記と異なり、公開 interface と両 host 実装では `Task`
- `DOCS_REFRESH_MANIFEST=/tmp/docs-refresh-ksdialogs-manifest-planned.json` を明示した `concepts-coverage-check.py` / `heading-parity-check.py` / `code-block-parity-check.py` / `frontmatter-check.py` はすべて成功。対象 4 ファイルで `link-resolution-check.py` も成功
- `scripts/local-path-lint.py` / `scripts/identity-lint.py` は違反なし。Skill 内に `kasane/`、ADR ID、内部 Bridge / Gateway / interop 名、Skill ルート外リンクは残っていない
- planned manifest を明示した `api-coverage-check.py` の migration 未掲載候補は、`cross/user-skill-api-listing.md:81`〜`:104` の確定済み「対象 Skill 外・機械検査由来」「内部層・interop 層」「機械的に導出できる名前」「低頻度の細部 API」に全件一致。未分類候補なし
- `dotnet build ../AiForms.Maui.Dialogs/AiForms.Maui.Dialogs/AiForms.Maui.Dialogs.csproj --no-restore -f net10.0`: 成功、警告 0、エラー 0
- `dotnet build maui/KsDialogs.Maui.ApiSurfaceCheck/KsDialogs.Maui.ApiSurfaceCheck.csproj --no-restore -f net10.0`: 成功、警告 0、エラー 0
- `dotnet test maui/KsDialogs.Maui.Tests/KsDialogs.Maui.Tests.csproj --no-restore -f net10.0 --filter 'FullyQualifiedName~DialogDependencyInjectionTests|FullyQualifiedName~DialogInlineShowTests|FullyQualifiedName~DialogTypedShowTests|FullyQualifiedName~DialogLayoutPassthroughTests|FullyQualifiedName~DialogTransitionPassthroughTests|FullyQualifiedName~LoadingFacadeTests|FullyQualifiedName~ToastFacadeTests'`: 61 件成功、失敗 0、スキップ 0
- 全ビルドルートの全 suite は、製品コード・テスト無変更について proposal Impact で合意済みの免除に従い省略した

## アクションプラン

1. 任意: `LoadingConfig.Opacity` の行を英日同時に補足し、背景だけへ alpha を移す場合の視覚差と、旧来の全体 alpha を近似する 3 色への反映を区別する。
