# レビュー結果: rollout-user-docs (005 回目)

**日付**: 2026-09-05
**判定**: CHANGES_REQUESTED

## サマリー

AiForms 移行 Skill は、旧 README と公開ソースから得た 78 件の既存 API 行に、新 Toast message 経路の代替 1 行を加えた構成になっており、Toast と `IReusableLoading.Hide()` の合意済み deviation、構造・frontmatter・閉世界性・英日ロックステップ、最小コードのコンパイルは確認できた。一方、予定 manifest のファイル単位の源泉完全性に 2 concept の欠落があり、さらに reusable Loading の終了とレイアウト・演出の非互換動作を安全に移行するための注記が足りないため、現状の判定は CHANGES_REQUESTED とする。

## 照合した規約

- `kasane/handbook/cross/aiforms-origin-reference.md` — 移植元 README を一次入口とし、記載差は公開ソースを優先する規約
- `kasane/handbook/cross/user-skill-api-listing.md` — 利用者が直接使う公開 API を簡潔かつ網羅的に掲載する規約
- `kasane/handbook/cross/test-execution.md` — 本 change の proposal で合意された全ビルドルート省略を尊重し、移行コードと MAUI 公開面だけを限定検査
- `specs/user-skills/spec.md` — 移行対応表、閉世界性、英日ロックステップ、ファイル単位の源泉完全性
- `kasane/decisions/cross/0011-user-skills-top-level-structure.md` — 移行 Skill の独立構成と Toast の扱い

## 検証証拠

**旧公開面**: 移植元は `../AiForms.Maui.Dialogs/README.md:438` 以降の API Reference と、同リポジトリの `Configurations`、Dialog、Loading、Toast、`ExtraView` の公開型を突き合わせた。README とコードが食い違う `IReusableLoading.Hide()` は、公開 interface の `Task Hide()` (`../AiForms.Maui.Dialogs/AiForms.Maui.Dialogs/Loading/IReusableLoading.cs:6`) を採る現行記述が正しい。

**対応行と Toast**: 対応表のデータ行は en / ja とも 79 行で、API コードトークンは行単位で一致した。内訳は旧公開面 78 行と、新 Toast message 代替 1 行である。旧 Toast に message overload が無いことと、新経路の差分 3 点は `skills/{en,ja}/ksdialogs-aiforms-migration/references/api-mapping.md:121` からの節に正しく反映されている。

**予定 manifest の機械検査**: concepts 網羅、見出し構造、コードブロック byte 一致、frontmatter、内部リンク解決はいずれも違反 0 件だった。API 名網羅検査はヒューリスティック候補を報告したため、対象 9 concept と現行 MAUI 公開実装・テストを個別に照合した。

**公開文書 lint**: 対象 4 ファイルの閉世界性、機械面名、Skill ルート外相対リンク、ローカル絶対パス、identity、配信識別子は違反 0 件だった。frontmatter は `license: MIT` と本リポジトリを指す `metadata.source` を持ち、ja の description には製品名、`.NET MAUI`、Dialog / Loading / Toast、API、IoC の英語 trigger がある。

**限定ビルドとテスト**: `skills/{en,ja}/ksdialogs-aiforms-migration/SKILL.md:37` の最小コードをそのまま使った一時 consumer を `net10.0` に限定してビルドし、警告 0・エラー 0 を確認した。`maui/KsDialogs.Maui.ApiSurfaceCheck/KsDialogs.Maui.ApiSurfaceCheck.csproj` も同 TFM でビルド成功、`maui/KsDialogs.Maui.Tests/KsDialogs.Maui.Tests.csproj` は 133 件成功・失敗 0・skip 0 だった。限定前の復元は未導入の iOS / Android workload を要求し、TFM の片側指定では MAUI XAML 生成が不足したが、`TargetFramework` と `TargetFrameworks` の両方を `net10.0` に固定した合意範囲の実行では成功した。

**UI・deviation・task**: UI brief は新規 UI を含まず README 用スクリーンショット選定だけを扱う。iOS Toast の撮影経路は `deviation.md` に記録済みで、AiForms 移行 Skill の内容には影響しない。`tasks.md` 6.1 が未完了なのも、独立レビュー群の途中状態と整合する。

## 指摘事項

### [🟠 Major] SKILL.md の内容が依拠する 2 concept が予定 source map に無い

**該当箇所**: `skills/en/ksdialogs-aiforms-migration/SKILL.md:3`、`skills/en/ksdialogs-aiforms-migration/SKILL.md:19`、`skills/en/ksdialogs-aiforms-migration/SKILL.md:21`、`skills/ja/ksdialogs-aiforms-migration/SKILL.md:3`、`skills/ja/ksdialogs-aiforms-migration/SKILL.md:19`、`skills/ja/ksdialogs-aiforms-migration/SKILL.md:21`

**問題点**: このファイルは ViewModel / notifier / lifecycle hook と animation の移行を能力として明示しているが、レビュー入力の予定 manifest で `ksdialogs-aiforms-migration/SKILL.md` の源泉に `core/api/model-binding-semantics.md` と `core/api/transition-semantics.md` が含まれていない。本文が現在正しくても、これらの concept が将来変わった際に SKILL.md が追従対象にならず、`specs/user-skills/spec.md:145` のファイル単位の源泉完全性を満たさない。

**推奨修正**: 予定 source map の同 target に上記 2 concept を追加し、最終 manifest へ反映する。修正後に concepts 網羅だけでなく、ファイル単位の source 完全性を再レビューする。

### [🟠 Major] reusable Loading の `Hide()` をプロセス共有 `HideAsync()` へ置く危険な差が示されていない

**該当箇所**: `skills/en/ksdialogs-aiforms-migration/references/api-mapping.md:60`、`skills/en/ksdialogs-aiforms-migration/references/api-mapping.md:62`、`skills/ja/ksdialogs-aiforms-migration/references/api-mapping.md:60`、`skills/ja/ksdialogs-aiforms-migration/references/api-mapping.md:62`

**問題点**: 旧 `IReusableLoading.Hide()` はその reusable handle の表示を閉じる API だが、新 `IKsLoading.HideAsync()` は合流数によらずプロセス共有の現在世代を即座に閉じ、他の `StartAsync` action は継続する (`kasane/concepts/core/api/loading-semantics.md:29`、`:37`〜`:47`、現行公開契約 `maui/KsDialogs.Maui/Presentation/IKsLoading.cs:84`)。表は「reusable handle は無い」とは書くものの、`IReusableLoading.Hide()` の行では単に `HideAsync()` を await するよう案内しており、機械的置換すると別機能の Loading まで閉じ得ることを読み取れない。

**推奨修正**: `IReusableLoading.Hide()` を単純な同等 API として扱わず、表示所有権の直接対応先が無いこと、`HideAsync()` は共有表示全体を閉じても action を中断しないことを明記する。旧 handle の開始・終了を移す通常経路として、可能な場合は処理スコープ付き `StartAsync` を優先する案内も同じ節に置く。

### [🟠 Major] 移行で外観・完了タイミングを変える既知の非互換差が対応表から落ちている

**該当箇所**: `skills/en/ksdialogs-aiforms-migration/references/api-mapping.md:95`、`skills/en/ksdialogs-aiforms-migration/references/api-mapping.md:96`、`skills/en/ksdialogs-aiforms-migration/references/api-mapping.md:97`、`skills/en/ksdialogs-aiforms-migration/references/api-mapping.md:106`、`skills/en/ksdialogs-aiforms-migration/references/api-mapping.md:107`、および ja 同ファイルの同各行

**問題点**: 対応表はメンバー名の行き先だけを示し、移植元と意図的に異なる既定値 3 点 — overlay は Transparent から黒 40%、dialog margin は 0 から全辺 24、layout area は window から visible area — を伝えていない (`kasane/concepts/core/api/layout-semantics.md:60`)。特に `UseCurrentPageLocation` は true / false のどちらを `VisibleArea` / `Window` に写すかも明示されていない。また旧 animation hook は 250ms を超えると打ち切られ得た (`../AiForms.Maui.Dialogs/README.md:620`) のに対し、新 MAUI hook は `Func<VisualElement, Task>` で完了を待ち、暗黙 timeout がなく、show の結果配送も退出完了後になる (`kasane/concepts/core/api/transition-semantics.md:53`、`:59`、`:149`〜`:157`)。既定値を指定していなかった利用者や旧 hook を移す利用者に、コンパイルでは検出できない外観・制御フローの変化が生じる。

**推奨修正**: layout 節に 3 既定値の差と `UseCurrentPageLocation` の bool→enum 対応を明記する。animation 2 行または節リードに、新 hook の引数・Task 完了契約・timeout 廃止・結果は退出完了後に返ることを簡潔に追記する。en / ja を同時に更新して意味を揃える。

## アクションプラン

1. 予定 source map の `ksdialogs-aiforms-migration/SKILL.md` に model-binding と transition の 2 concept を追加する。
2. reusable Loading の所有権差と共有 `HideAsync()` の効果を、英日対応表へ明記する。
3. layout の既定値 3 差分・bool→enum 対応と、transition hook の非互換な完了契約を英日へ追記する。
4. 変更した 2 言語ペアで、見出し・コード byte・閉世界性・内部リンク・API 行数を再検査し、AiForms 移行 Skill の独立レビューを再実施する。
