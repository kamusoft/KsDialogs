# セカンドオピニオン: split-concepts-platform-surface (code-001)
**相方**: codex / **label**: so-code-split-concepts-platform-surface / **日付**: 2026-09-05 / **対象**: 中間レビュー (tasks グループ 1〜4 完了時点) — kasane/concepts/ の分割後の全 concept (core/api 8 本の書き直し・core/architecture 1 本と platform surface 18 本の新設・index / log / rules の更新) と kasane/changes/split-concepts-platform-surface/verification/ 一式
---
# 中間レビュー結果

**判定: CHANGES_REQUESTED**

Critical 0件、Major 4件、Minor 2件です。グループ5〜7の未着手事項は指摘対象から除外しています。

## Major

### 1. 1引数 factory がインライン show でも利用可能という、新規かつ実装と異なる契約が追加されている

- 該当箇所: `kasane/concepts/core/api/model-binding-semantics.md:83`
- 問題点: 「1引数 factory」がインスタンス渡し show・インライン showでも使えると記述されています。しかし1引数形は登録用 factory のオーバーロードであり、各実装のインライン show は結果報告口を含む2引数 factoryしか受けません。
  - Android: `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/KsDialogs.kt:48`
  - iOS: `ios/Sources/KsDialogs/Presentation/KsDialogs.swift:30`、`:38`
  - MAUI: `maui/KsDialogs.Maui/Presentation/IKsDialogs.cs:67`、`:89`
- 分割前の文書は「1引数形でも登録できる」とだけ述べており、今回の書き直しでインライン show へ主張が拡張されています。したがって、単なる言い換えに収めるという proposal の Non-Goals にも反します。
- 推奨修正: 83行目を削除するか、「1引数形で登録した factory は、インスタンス渡し show と型指定 show の登録経路で利用できる」と限定してください。インライン show 対応を意図するなら、別 change で公開APIの追加として扱う必要があります。

### 2. Android Compose の登録経路とインライン表示経路が混同されている

- 該当箇所:
  - `kasane/concepts/android/api/dialog-surface.md:37`
  - `kasane/concepts/android/api/loading-surface.md:44`
  - `kasane/concepts/android/api/toast-surface.md:45`
- 問題点: 表では、Compose Viewを `registerCompose` で登録した後の表示にも `showCompose` / `startCompose` を使うように読めます。しかし、これらは content を直接受け取る、登録を利用しないインライン専用APIです。
  - `android/ksdialogs-compose/src/main/kotlin/jp/kamusoft/ksdialogs/compose/ComposeDialogShow.kt:23`
  - `android/ksdialogs-compose/src/main/kotlin/jp/kamusoft/ksdialogs/compose/ComposeLoadingShow.kt:20`、`:41`
  - `android/ksdialogs-compose/src/main/kotlin/jp/kamusoft/ksdialogs/compose/ComposeToastShow.kt:21`
- 同じ文書内の例は、`registerCompose` 後に通常の `show` を呼んでおり、表・説明と例も矛盾しています。
- 推奨修正: 次の3経路を明確に分離してください。
  - Compose Viewの登録: `registerCompose`
  - 登録済みViewModelの表示: 通常の `show` / `start`
  - Compose contentを直接渡すインライン表示: `showCompose` / `startCompose`

### 3. 配置規則のカテゴリ定義が、新設した契約・公開面の分割規則と矛盾している

- 該当箇所: `kasane/concepts/rules.md:31`
- 問題点: `core/api/` の対象を「契約・公開 API・利用例」と定義しています。一方、同ファイルの `:60-64` は公開名・署名・コード例を `<platform>/api/` に置くと定め、`kasane/concepts/core/index.md:5` も同じ分割を説明しています。
- `rules.md` は今後の配置判断に使われるため、この矛盾を残すと公開APIや利用例が再び core に追加される可能性があります。
- 推奨修正: 31行目を「全 platform が共有する観察可能な挙動と保証」など、今回導入した定義と一致する表現へ変更してください。

### 4. KMP の禁止トークン集合が、参照しない iOS Native surface を許可してしまう

- 該当箇所:
  - `kasane/changes/split-concepts-platform-surface/verification/baseline.md:88-99`
  - `kasane/changes/split-concepts-platform-surface/verification/forbidden-tokens.json:556`
- 問題点: KMPの「自 platform の公開面」に `ios/api/*.md` 全体を含めています。しかし、完成形の targets でKMPが参照するiOS surfaceは、layoutとtransitionだけです。
  - `kasane/changes/split-concepts-platform-surface/design.md:109-114`
  - `kasane/changes/split-concepts-platform-surface/specs/user-skills-manifest/spec.md:21-25`
- この作り方では、KMPが参照しないiOS Native固有名、例えば `Dialog.shared.registry`、`DialogError`、`Loading.shared.style`、`Toast.shared.style` がKMP Skillへ混入しても負の検査で検出できません。`Loading.shared.options`など一部は旧リスト由来で残っていますが、体系的な集合になっていません。
- 推奨修正: KMPの許可面を実際の targets の和集合から導出してください。概ね次の範囲です。
  - `kmp/api/*.md`
  - `android/api/*.md`
  - `ios/api/layout-surface.md`
  - `ios/api/transition-surface.md`

  そのうえで、上記に存在しないiOS Native固有識別子をKMPの禁止集合へ追加してください。

## Minor

### 1. iOSレイアウト型の種別が誤っている

- 該当箇所: `kasane/concepts/ios/api/layout-surface.md:25`
- 問題点: 表に挙げた型を「いずれも struct」としていますが、`DialogLayoutArea` と `DialogAlignment` は enum です。
  - `ios/Sources/KsDialogs/Contract/DialogLayoutArea.swift:4`
  - `ios/Sources/KsDialogs/Contract/DialogAlignment.swift:7`
- 「すべてのイニシャライザ引数に既定値がある」という後半もenumには適用できません。
- 推奨修正: 2つを enum、`DialogOptions`・`DialogPlacement`・`DialogEdgeInsets`を struct と分類し、イニシャライザの説明をstruct側だけに限定してください。

### 2. core目次に実在しない識別子が残っている

- 該当箇所: `kasane/concepts/core/index.md:13`
- 問題点: 基準領域をコード識別子として `` `LayoutArea` `` と記載していますが、公開面の実名はiOS/Androidの `DialogLayoutArea` であり、KMP commonMainを含む全形態共通の同綴り識別子ではありません。`verification/core-contract-check.md` の共通名判定とも整合しません。
- 推奨修正: coreの目次ではバッククォートを外し、単に「基準領域」と記載してください。`DialogLayoutArea` の具体名はplatform surfaceにだけ残すのが今回の配置規則に合います。

## 確認結果

以下は問題ありませんでした。

- `git diff --check`: 成功
- 着地台帳: 108行、着地89・意図して落とした19・未説明0
- 相対リンク、identity、local-path、concept側禁止トークン、doc-structureについては、提示された機械検査結果と矛盾する所見なし
- `deviation.md` に記録された合意済み差分は指摘していません
- ビルド・テストおよびSkill再生成後の検査は、この中間レビューの対象外としました

照合した規約は ksn-review の汎用観点・判定基準、および ksn-core の `concepts.md`、`doc-structure.md`、`delta-spec.md`、`paths.md`、`domain-axis.md` です。公開名と署名はSwift/Kotlin/Compose/C#/MAUIの実装宣言と突き合わせました。指定どおりレビュー文書ファイルは作成していません。


## 突き合わせ結果 (2026-09-05、ホスト側 review-001 との照合)

| # | 相方の指摘 | ホスト側 (review-001) | 採否 | 根拠・反映 |
|---|---|---|---|---|
| Major 1 | 1 引数 factory がインライン show でも使えるという主張の追加 (`core/api/model-binding-semantics.md:83`) | 未検出 (「新規に増えた主張なし」と判定) | **採用 (ホスト側の見逃し)** | baseline (218d5c0) の同節は「1引数形でも登録できる」のみ。各実装のインライン show は 2 引数 factory しか受けない。修正サイクルへ |
| Major 2 | Android の Compose 登録経路 (`registerCompose` + `show`) とインライン表示 (`showCompose`) の混同 (`android/api/{dialog,loading,toast}-surface.md` の表) | 未検出 | **採用 (ホスト側の見逃し)** | `android/ksdialogs-compose/.../ComposeDialogShow.kt:23` 等の宣言で確認。修正サイクルへ |
| Major 3 | `rules.md:31` の `core/api/` カテゴリ定義「契約・公開 API・利用例」が新設の振り分け規則と矛盾 | 未検出 | **採用** | 同ファイル内の矛盾で、今後の配置判断に効く。修正サイクルへ |
| Major 4 | KMP の禁止トークン集合が `ios/api/*.md` 全体を許可面に含めており、targets が参照しない iOS 固有名を検出できない | 関連指摘 (Suggestion: concept 側検査は構成上 0 件になる) | **採用** | design Decision 6 の targets (kmp が参照する ios surface は layout / transition のみ) から許可面を導くべき。修正サイクルへ (fixture の再導出) |
| Minor 1 | iOS layout-surface の型種別 (`DialogLayoutArea` / `DialogAlignment` は enum) | **一致** (ホスト側は Major。Android 側の同文と `DialogEdgeInsets` の既定値なしも指摘) | **確定 (Major)** | 修正サイクルへ |
| Minor 2 | `core/index.md:13` の `LayoutArea` バッククォート | **一致** (ホスト側 Minor 2) | **確定 (Minor)** | 修正サイクルへ |

降格: なし。未解決 (矛盾): なし。ホスト側のみの指摘 (Minor 1: rules.md に KMP 3 側の定義が無い / Minor 3: 書き直した項目に残る構造 lint 5 件 / Suggestion 2 件) は review-001 に従い同じ修正サイクルで処理する。
