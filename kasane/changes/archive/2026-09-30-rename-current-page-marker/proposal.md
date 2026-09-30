# Proposal: rename-current-page-marker

## Why

基準領域「表示中のページ」のために、アプリのページを表示中のページとして名乗らせる印 (SwiftUI の `View.ksDialogCurrentPage()`、Compose の `Modifier.ksDialogCurrentPage()`) は、「ダイアログの現在ページ」を取ってくる関数のように読める。付けると何が起きるかが呼び出し側で分からない。印はまだ配布していない (2026-09-27 に追加、`0.1.0-beta.2` には未収録)。利用者がいない今のうちに、何をするかが読める名前へ改める (exploration.md)。

## What Changes

- **印の改名** (iOS・Android): SwiftUI の `ksDialogCurrentPage()` と Compose の `Modifier.ksDialogCurrentPage()` を、どちらも `markAsDialogCurrentPage()` に改める。旧名は deprecated 別名も含めて残さない。台帳規則・優先順位などの挙動は変えない
- **名前を含む周辺の追随**: 説明コメント、印が見つからないときの診断メッセージ (iOS・Android の台帳)、Compose のデバッグ表示名 (inspector の name)、Compose 側のファイル名 (`KsDialogCurrentPage.kt`)、公開面の正の検査 (iOS の非 `@testable` ファイル、Android の `api-surface-check`)、テストと 4 ルートの Sample
- **変えないもの**: `ks` 付きの属性メンバー (core/ADR-0045 のとおり)、アプリが関数で表示中のページを教える登録口の型名 `DialogCurrentPage`
- 蒸留時に反映: decisions/core/0045 — accepted に昇格 (`ks` を付けるのは OS の View 型への後付け属性とその SwiftUI の対に限る)
- 蒸留時に反映: concepts/ios/api/layout-surface.md・concepts/android/api/layout-surface.md — 印の名前を新しい名前へ。ADR-0045 の `ks` の付け分けの規則を iOS / Android の公開面に書く
- 蒸留時に反映: handbook/cross/sample-parity.md — 「表示中のページの名乗り」の行の印の名前を新しい名前へ

影響する能力: ios-native、android-native、samples

## Non-Goals

- 旧名が解決できないことを負のコンパイル検査で固定すること — 設計判断: 旧名は一度も配布していないので、利用者のコードを守る検査にはならない。旧名の消し忘れは、現行のソース・テスト・Sample に旧名が残らないことの検索で確かめる (デルタスペックの Scenario)
- 登録口の型名 `DialogCurrentPage` の改名 — 探索で変えないと決定済み。新しい印の名前が型名をそのまま含んで揃っている
- `ks` 付きの属性メンバー 9 個の改名 — core/ADR-0045 で変えないと決定済み
- skills/ の追随 — 現時点で印に触れている Skill が無い

## Impact

- **破壊的変更**: 公開 API の改名。ただし印はまだ配布していないので、外部の利用者への影響はない
- **挙動変更なし**: 改名だけで、既存テストは名前の追随だけで通る想定
- **リスク**: 診断メッセージとコメントの中の旧名は、コンパイルでは検出されない → 旧名の残存検索で機械的に拾う

## 級: M

未配布の公開 API の改名が iOS・Android の 2 形態と Sample 4 ルートにまたがる (挙動・見た目の変更なし)。

domain: cross (実際に触るドメイン: ios / android — 各 domain-skills を結合)
