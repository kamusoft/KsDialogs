# セカンドオピニオン: split-concepts-platform-surface (code-002)
**相方**: codex / **label**: so-code-split-concepts-platform-surface (同一セッションの 2 ターン目) / **日付**: 2026-09-05 / **対象**: 修正サイクル 1 の確認 — review-001 / second-opinion-code-001 の指摘に対する修正 (kasane/concepts/ の該当 concept と kasane/changes/split-concepts-platform-surface/verification/ の禁止トークン fixture)
---
# 再レビュー結果

**最終判定: CHANGES_REQUESTED**

前回の6件はすべて元の問題を解消しています。ただし、KMP禁止集合の修正で新たなMajorが1件入りました。

## 前回指摘の再確認

| 前回指摘 | 判定 | 確認結果 |
|---|---|---|
| Major 1: 1引数factoryをインラインshowでも使えるとしていた | **解消** | `kasane/concepts/core/api/model-binding-semantics.md:81-83` は登録オーバーロードに限定され、登録済みfactoryを使うインスタンス渡し／型指定showだけに修正された。baselineの既存記述から導ける範囲で、実装とも一致する |
| Major 2: Android Composeの登録経路とインライン経路の混同 | **解消** | Dialog `:37-42`、Loading `:44-49`、Toast `:45-50` で「登録済み表示」と「インライン表示」が分離された。`showCompose` / `startCompose` の実装と一致する |
| Major 3: `rules.md` のcore/api定義が分割規則と矛盾 | **解消** | `kasane/concepts/rules.md:31` が「観察可能な挙動と保証」に修正され、`:60-68` と整合した。KMPの3側の定義も `:66` に明記された |
| Major 4: KMP許可面がiOS surface全体を含んでいた | **解消** | `verification/baseline.md:93-103` で、KMP許可面がtargetsの和集合へ絞られた。ただし下記の新規Majorあり |
| Minor 1: iOSレイアウト型をすべてstructとしていた | **解消** | `kasane/concepts/ios/api/layout-surface.md:25` でstructとenumを正しく分類。初期値の説明も実装と一致する。Android側 `layout-surface.md:25` の追随修正も正しい |
| Minor 2: core目次に実在しない `` `LayoutArea` `` があった | **解消** | `kasane/concepts/core/index.md:13` は識別子表記を外した「基準領域」へ修正された |

## 新たな指摘

### 🟠 Major: KMP自身のSwift公開面で使う名前まで禁止集合へ入っている

**該当箇所**:

- `kasane/changes/split-concepts-platform-surface/verification/forbidden-tokens.json:594`
- `kasane/changes/split-concepts-platform-surface/verification/forbidden-tokens.json:695`
- `kasane/changes/split-concepts-platform-surface/verification/forbidden-tokens.json:701`
- `kasane/changes/split-concepts-platform-surface/verification/forbidden-tokens.json:705`

**問題点**: targetsに含まれるconceptの和集合だけを「KMP公開面」とみなしたため、KMP専用Swift入口から実際に観察される名前まで他platform名として禁止されています。

少なくとも以下はKMP自身の公開経路です。

- `DialogError`: `ios/Sources/KsDialogs/Kmp/KsDialogsKmp.swift:185` がKMP向けshowから投げると明記
- `cancelled`: 同ファイル `:199-215` の戻り値 `DialogResult<Result>` が `.cancelled` を返す
- `presentationHostUnavailable`: `ios/Sources/KsDialogs/Kmp/KsDialogsKmpError.swift:30-34` がKMP入口からそのまま返す
- `viewFactoryNotRegistered`: KMP向けLoading／Toast入口が実際に `DialogError.viewFactoryNotRegistered` を投げる
  `ios/Sources/KsDialogs/Kmp/KmpLoadingViewModel.swift:33-42`
  `ios/Sources/KsDialogs/Kmp/KmpToastViewModel.swift:26-30`

このままでは、後続のSkill再生成でこれらの正当なKMP公開契約を記載すると、負の検査が失敗します。反対に、検査を通すために正当なAPI説明を落とす方向へ誘導します。

**推奨修正**: KMPの禁止集合はtargetsのパス差分だけで決めず、KMP専用Swift入口から到達可能な公開署名・戻り値・失敗型を除外してください。少なくとも上記4種を再分類し、同じ理由で追加したcase名も到達可能性を実装から再確認してください。

## AnyObject / Sendable / Result の判断

この3件をKMP禁止集合へ追加しなかった判断は**妥当**です。

`ios/Sources/KsDialogs/Kmp/KsDialogsKmp.swift:187-203` の公開署名が、実際に `ViewModel: AnyObject`、`Result: Sendable`、`Result.Type`を使用しています。したがって、これらは単なるiOS Nativeからの混入ではなく、KMP利用者がSwiftホスト側で直接見る公開面です。

ただし、この判断基準は上記の `DialogError` や `cancelled` にも同様に適用する必要があります。

## その他の確認

- baseline 218d5c0にない新しい挙動保証の追加: 修正対象では確認されず
- 公開署名と実装の不一致: 上記禁止集合の分類問題を除き確認されず
- 修正対象10ファイルの相対リンク検査: 未解決0件
- `git diff --check`: 成功
- 着地台帳: 108行、着地89・意図して落とした19・未説明0
- ビルド・テスト: 文書専用の中間レビューという指定に従い未実施
- ファイル書き込み: なし

残る指摘は **Major 1件、Minor 0件**です。


## 突き合わせ結果 (2026-09-05、ホスト側 review-002 との照合)

| # | 相方の指摘 | ホスト側 (review-002) | 採否 | 根拠・反映 |
|---|---|---|---|---|
| 前回 Major 1〜4 / Minor 1〜2 | 全件解消 | 全件解消 (10/10) | **確定 (解消)** | — |
| 新規 Major | KMP 禁止集合に KMP 自身の Swift 向け入口から到達する名前 (`DialogError` / `cancelled` / `presentationHostUnavailable` / `viewFactoryNotRegistered`) が入っている | 未検出 (別角度で Minor 1: 許可面の 4 トークン `KsDialogAttributes` / `SimpleDialogViewModel` / `ksDialogOptions` / `ksDialogPlacement` が残っている) | **採用 (ホスト側の見逃し)** | `ios/Sources/KsDialogs/Kmp/KsDialogsKmp.swift:185-215` 等で到達可能。task 6.1 の Skill 側検査で正当な記述を落とす誘導になるため fixture の分類基準に「KMP 専用 Swift 入口からの到達可能性」を加えて再分類。ホスト側 Minor 1 と同じ修正サイクルで処理 |
| `AnyObject` / `Sendable` / `Result` の除外判断 | 妥当 | 妥当 (差集合の再導出で 3 件ちょうど一致) | **確定** | 変更なし |

降格: なし。未解決 (矛盾): なし。ホスト側のみの指摘 (Minor 2: log.md の構造 lint 件数の記述 / Suggestion: doc-structure-lint の `--paths` が index / log / rules の SKIP を迂回する挙動) は review-002 に従い処理する (Suggestion は標準装備側の課題として完了報告で扱う)。
