# レビュー結果: rollout-user-docs (016 回目)

**日付**: 2026-09-05
**判定**: CHANGES_REQUESTED

## サマリー

AiForms migration Skill は、現行 KsDialogs.Maui への対応内容、英日ロックステップ、閉世界性、リンク、最小コードの実行可能性を満たしている。一方、移植元の対応表が、対象としている公開 View 型の `BindableProperty` フィールド 18 件を一件も掲載しておらず、「旧公開 API の網羅」を満たさないため、Major 1 件として修正を求める。

## 照合した規約

- `kasane/handbook/cross/user-skill-api-listing.md` — `skills/**` の公開 API 掲載基準
- `kasane/handbook/cross/aiforms-origin-reference.md` — 移植元 README を先に、公開ソースを詳細の正として読む規則
- `kasane/handbook/cross/test-execution.md` — 限定ビルドの実行と結果報告
- `kasane/handbook/cross/local-development-setup.md` — MAUI ビルドルートの環境・実行手順
- `kasane/decisions/cross/0011-user-docs-as-agent-skills.md` — 移行 Skill の構成、閉世界性、源泉規則
- `specs/user-skills/spec.md` — 移行 Skill の対応表、翻訳ロックステップ、閉世界性、生成の内容規約
- `deviation.md` — 旧 Toast message overload 不在と `IReusableLoading.Hide()` の実署名を優先する合意済み差分

## 指摘事項

### [🟠 Major] 旧 View 型の public `BindableProperty` フィールド 18 件が対応表に無い

**該当箇所**: `skills/en/ksdialogs-aiforms-migration/references/api-mapping.md:3`、`skills/ja/ksdialogs-aiforms-migration/references/api-mapping.md:3`

**問題点**: 対応表は「README 掲載 API と、同じ公開型が追加で露出するメンバー」を対象とし、各旧メンバーが一度現れると宣言している。しかし、移植元の公開ソースにある次の `BindableProperty` フィールドは、英日いずれにも exact token で 0/18 件しか掲載されていない。

| 移植元 | 未掲載の public フィールド |
|---|---|
| `../AiForms.Maui.Dialogs/AiForms.Maui.Dialogs/ExtraView.cs:8` | `ProportionalWidthProperty`、`ProportionalHeightProperty`、`VerticalLayoutAlignmentProperty`、`HorizontalLayoutAlignmentProperty`、`OffsetXProperty`、`OffsetYProperty`、`CornerRadiusProperty`、`BorderColorProperty`、`BorderWidthProperty`、`AutoRotateForIOSProperty`、`DialogMarginProperty` |
| `../AiForms.Maui.Dialogs/AiForms.Maui.Dialogs/Dialog/DialogView.cs:5` | `IsCanceledOnTouchOutsideProperty`、`OverlayColorProperty`、`UseCurrentPageLocationProperty`、`DialogNotifierProperty` |
| `../AiForms.Maui.Dialogs/AiForms.Maui.Dialogs/Loading/LoadingView.cs:5` | `ProgressProperty`、`OverlayColorProperty` |
| `../AiForms.Maui.Dialogs/AiForms.Maui.Dialogs/Toast/ToastView.cs:5` | `DurationProperty` |

これらは XAML・style・コードから直接参照できる旧 public API であり、同じ型の通常プロパティや仮想メソッドをソースから補完している現行表からだけ外す根拠はない。現行の除外リストにも載っておらず、`user-skill-api-listing.md` の「簡潔でも網羅」と、`specs/user-skills/spec.md` の Scenario「旧公開 API の網羅」に反する。特に旧添付先を static フィールドで参照する利用コードは、通常プロパティの行だけでは新しい `Dialog.*Property` または対応先なしへ機械的に移せない。

**推奨修正**: 18 件を英日対応表へ exact token 付きで追加し、現行の `Dialog.*Property` に移るものと、View 自身の表現・ViewModel・呼び出し引数へ移って直接対応先が無いものを区別する。個別行を畳む場合も、18 名を列挙して「対応する通常プロパティの行と同じ移行先」という導出規則を明記し、英日を同時更新する。

## 検証結果

- 予定 manifest の対象は 70 ファイル。AiForms migration の `SKILL.md` は 8 concept、`api-mapping.md` は 9 concept を源泉に持ち、記述内容との不足は上記の旧 API 側以外に認めなかった。
- docs-refresh の見出し構造、コードブロック byte 一致、frontmatter、内部リンク検査はすべて成功した。
- 対象 4 ファイルの閉世界性・機械面・Skill ルート外リンク・ローカル絶対パス・識別情報の違反は 0 件だった。
- 英日でバッククォート識別子集合は一致した (`SKILL.md` 14 件、`api-mapping.md` 170 件)。表行数も一致した。
- `dotnet build maui/KsDialogs.Maui.ApiSurfaceCheck/KsDialogs.Maui.ApiSurfaceCheck.csproj -f net10.0 --no-restore` は成功した (警告 0、エラー 0)。最小 Loading コードと同じ `StartAsync(Func<IProgress<double>, Task>, message)` 公開形状は API surface check でコンパイルされた。
- 製品コード・tests 無変更のため、proposal の合意済み免除に従い全 suite は実行していない。

## アクションプラン

1. 旧 `BindableProperty` フィールド 18 件を対応表へ追加または明示的な導出規則で全件掲載する。
2. 英日ロックステップ検査と、移植元 public member の exact token 突合を再実行する。
3. 修正後の AiForms migration Skill を fresh に再レビューする。
