# セカンドオピニオン: add-layout-spec (code-002)
**相方**: codex / **日付**: 2026-08-19 / **対象**: commit 5af1a1b 以降の working tree 全変更 (改訂実装)
---
# レビュー結果: add-layout-spec

**判定**: **CHANGES_REQUESTED**

**指摘件数**: Critical 0 / Major 2 / Minor 1 / Suggestion 0

提示された全ルート green の結果は前提とし、静的に契約・実装・テストを照合しました。正規化、オブジェクト単位置換、KMP の現在の公開面、旧 `DialogLayoutProviding` 系の撤去、共通ケース表の一致は確認できました。

## 指摘事項

### [🟠 Major] 添付値を初回レイアウト完了時点より前に固定している

**該当箇所**:

- `ios/Sources/KsDialogs/Presentation/DialogContainerViewController.swift:44`
- `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/DialogContainer.kt:33`
- `maui/KsDialogs.Maui/Internals/DialogGateway.cs:59`

**問題点**: 契約では、採用値は「初回ネイティブレイアウトパス完了時点の添付値」であり、show 後でもその時点までの変更は反映されます。しかし iOS はコンテナ初期化時、Android はコンストラクター実行時、MAUI は View 生成直後に値を固定しています。

そのため、初回レイアウト中に到達する添付値やバインディング更新を採用できません。既存テストは「初回レイアウト後の変更を無視すること」しか検証せず、完了前の変更が採用される契約を固定できていません。

**推奨修正**: 初回描画前のレイアウト境界で添付値を確定し、レイアウト中に値が変わった場合は提示前に再適用してから固定してください。少なくとも「View 生成後・初回レイアウト完了前に変更した値は採用され、完了後の変更は無視される」テストを iOS・Android・MAUI 経路へ追加してください。

### [🟠 Major] 公開 API のコンパイル検査が部分的な契約違反を見逃せる

**該当箇所**:

- `ios/Tests/KsDialogsTests/DialogAttributeCompileChecks.swift:5`
- `kmp/ksdialogs-kmp/src/commonTestNegativeCompileCheck/kotlin/jp/kamusoft/ksdialogs/kmp/DialogAttributeNegativeCompileChecks.kt:14`
- `android/ksdialogs/src/testNegativeCompileCheck/kotlin/jp/kamusoft/ksdialogs/DialogAttributeNegativeCompileChecks.kt:13`
- `maui/KsDialogs.Maui.Tests/NegativeCompileChecks/DialogAttributeNegativeCompileChecks.cs:15`
- `maui/KsDialogs.Maui/KsDialogs.Maui.csproj:39`

**問題点**: 正の検査は `@testable import`、Kotlin の test friend path、`InternalsVisibleTo` を通るため、公開型が誤って internal になっても成功し得ます。また、負の検査は複数の禁止 API を1回の「失敗すれば成功」ビルドにまとめています。

例えば KMP で誤って `DialogOptions` が公開されても、`show(options:)` が存在しなければ別のエラーでビルドは期待どおり FAILED となり、「KMP は `DialogPlacement` のみ」という違反を検出できません。提示された負コンパイル結果だけでは各禁止形状を個別に証明できません。

**推奨修正**: 各ルートに friend/test 可視性を持たない独立 consumer fixture を設けて公開面を検査してください。負の検査は「KMP の `DialogOptions`」「show の options 引数」「VM 属性」など、禁止事項ごとに別ビルドへ分割するか、期待するコンパイラー診断を個別に照合してください。

### [🟡 Minor] 戻る導線のアクセシブルな名前・役割が不足している

**該当箇所**:

- `samples/ios/KsDialogsSample/SampleLayoutPanelHeader.swift:12`
- `samples/kmp/iosApp/KsDialogsSampleKmp/SampleLayoutPanelHeader.swift:13`
- `samples/android/app/src/main/kotlin/jp/kamusoft/ksdialogs/samples/android/SampleLayoutPanelView.kt:100`
- `samples/maui/KsDialogs.Sample.Maui/SampleLayoutPanelPage.xaml:14`

**問題点**: 操作名が記号 `‹` だけで、MAUI/Android は汎用 Label/TextView をクリック可能にしています。スクリーンリーダーから「戻る」操作として理解しにくく、MAUI ではボタンの役割も明確ではありません。

**推奨修正**: 見た目の記号は維持しつつ、各ルートで「戻る」のアクセシブル名を付与してください。MAUI/Android は可能なら Button 相当のセマンティクスも設定してください。

## アクションプラン

1. 初回レイアウト完了時点のスナップショット契約を実装・テストする。
2. 公開 API 検査を実利用者境界かつ禁止事項単位へ分離する。
3. サンプルの戻る導線へアクセシビリティ情報を追加する。

## 突き合わせ結果 (2026-08-19)

ホスト側 review-002.md (CHANGES_REQUESTED: Major 1 / Minor 4 / Suggestion 2) との突き合わせ。

| 指摘 | 出典 | 採否 | 扱い |
|---|---|---|---|
| スナップショット時点が契約 (初回レイアウトパス完了時点) より早い | 双方一致 (相方 Major1 / ホスト Major) | **確定 (Major)** | 修正サイクル: iOS / Android / MAUI の3経路 + 完了前変更の採用テスト |
| 公開 API コンパイル検査が部分的違反を見逃す (friend 可視性・負検査の束ね) | 相方のみ (Major2)。根拠強 — KMP で DialogOptions 誤公開でも別エラーで FAILED になり検出不能という実害シナリオ | **採用 (Major)** | 修正サイクル。ホスト Minor「iOS 負検査は並列ビルドで空振り (-jobs 1 で顕在化)」と同根のため統合して対処 |
| 戻る導線 `‹` のアクセシブル名・役割不足 | 相方のみ (Minor) | **採用 (Minor)** | 修正サイクル (4ルートのサンプル) |
| iOS 外側タップの内外判別述語が未検証 | ホストのみ (Minor) | 確定 (Minor) | 修正サイクル |
| KMP Android の視覚照合が未実施のまま 6.3 完了扱い | ホストのみ (Minor) | 確定 (Minor) | 修正サイクル (補完証跡の取得) |
| test-execution.md の件数乖離・instrumented 手順不在 | ホストのみ (Minor) | 確定 (Minor) | 蒸留 (ksn-distill) へ申し送り — concepts の更新は蒸留の責務 |
| MAUI DialogOptions internal の根拠明記 / iOS 互換面の既定値パリティテスト | ホストのみ (Suggestion) | 裁量 | 修正サイクルで安価に対処できる範囲で実施 |

降格: なし / 未解決 (両者矛盾): なし

## 再検証 round 2 (2026-08-19)

修正反映後の相方再検証。判定: **CHANGES_REQUESTED** (Major 2 — いずれもスナップショット修正の詰め残し)。

- [Major] iOS Native: `prepareForPresentation()` 内で `settleLayoutSnapshot()` → `present()` の順のため、window 搭載前に固定している。layout-semantics.md の「器を画面に載せたあと、OS が最初にサイズと位置を確定させる処理が終わった瞬間」と不一致。追加テストは window 搭載 → prepare の逆順で穴を検出できない
- [Major] MAUI iOS: `DialogContentView.LayoutSubviews()` が 転送 → Arrange → 固定 の順のため、Arrange 中の添付変更は固定されるが Native 添付面へ転送されない (次パスは frozen で早期 return)
- 解消確認: 公開 API 検査 (正/負16本)・戻る導線 a11y・iOS 外側タップ述語・KMP Android 照合・KMP 公開面・旧経路撤去・正規化/優先順位/置換の4ルート整合

原文: ~/.kasane/counterpart-bridge/responses/so-code-add-layout-spec-2.md

## round 2 突き合わせ (2026-08-19)

review-003 (ホスト) は「スナップショット3経路解消」と判定したが、相方 round 2 は iOS Native / MAUI iOS の詰め残しを指摘し矛盾。オーケストレーターが該当行を直接確認して裁定:
- iOS `DialogPresenter.swift:34-35` — `prepareForPresentation()` (内部 `settleLayoutSnapshot()`) が `present()` より前 = window 搭載前固定を確認。**相方の指摘を確定 (Major)**
- MAUI iOS `PlatformDialogGateway.cs:203-207` — Refresh → Arrange → Freeze の順を確認。固定値の最終転送が無い。**相方の指摘を確定 (Major)**
- ホスト側の解消判定は、追加テストが「window 搭載 → prepare」の逆順だったための見逃し (相方が指摘)

review-003 の新規指摘の扱い: test-execution.md の旧フラグ残置 (実行すると検証ゼロで成功) = **確定 (Major、修正サイクルで文書更新 — 同文書自身の「実態が変わったら実測で更新」規則に従う)** / MAUI 配線の検証欠如 = 確定 (Minor、相方 Major2 と統合) / iOS 正検査の VM 出自 = 確定 (Minor) / 収束ループ可観測性 = 裁量 (Suggestion)

## 再検証 round 3 (2026-08-19)

判定: **APPROVED** (Critical 0 / Major 0 / Minor 0 / Suggestion 1)。round 2 の Major 2件はいずれも解消を確認。

- iOS Native: 暫定レイアウトと window 搭載後の固定の分離、実 window での契約境界テストを確認
- MAUI iOS: TransferBeforeLayout → Arrange → FreezeAndTransfer の順序と固定値の再転送、搭載前に固定しない制御を確認
- 付随修正 (NaN 比較・applyAttributes インスタンスメソッド化) も妥当と確認
- Suggestion: `ios/Tests/KsDialogsTests/Support/DialogLayoutMeasurement.swift:43` のコメントが旧動作 (prepare で固定) の説明のまま → 非ブロッキング、修正予定

原文: ~/.kasane/counterpart-bridge/responses/so-code-add-layout-spec-3.md
