# Exploration: rename-current-page-marker

## 課題 / 動機

基準領域「表示中のページ」のために、アプリのページを表示中のページとして名乗らせる印 (SwiftUI の `View.ksDialogCurrentPage()`、Compose の `Modifier.ksDialogCurrentPage()`) の名前を、`ks` を付けずに、何をするかが分かる名前にしたい (オーナー、2026-09-30)。今の名前は「ダイアログの現在ページ」を取ってくる関数のように読め、付けると何が起きるかが呼び出し側で読めない。

### 探索で確認した現状 (2026-09-30、コードが正)

- 定義は 2 か所: `ios/Sources/KsDialogs/SwiftUI/View+DialogCurrentPage.swift:19` と `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/compose/KsDialogCurrentPage.kt:35`。MAUI と KMP の共有層には無く、KMP の Sample は各 OS 側の印を直接使う
- 印は 2026-09-27 の add-page-layout-area で追加され、最新の配布 `0.1.0-beta.2` (2026-09-13) には入っていない。利用者はいないので旧名を残さずに改名できる
- 印の仲間に見える `ks` 付きの公開メンバーは 9 個 (UIKit の `UIView` と Android の `View` への後付けプロパティ `ksDialogOptions` / `ksDialogPlacement` / `ksDialogTransition` と、同名の SwiftUI の modifier 3 個)。すべて中身の View に属性を付けるもので、beta で公開済み
- `ks` を付ける理由は記録のどこにも無かった。2026-08-18〜19 の add-layout-spec で UIKit の後付けプロパティ名として初出し、SwiftUI の modifier は同名で core/ADR-0015 に載った。2026-09-06 の rename-swiftui-transition-modifier は綴りを揃えただけで、付けること自体の是非は問われていない。印も既存の形に合わせて `ks` 付きで入った
- アプリが関数で表示中のページを教える登録口 (`DialogCurrentPage.provider`、MAUI は `DialogCurrentPage.Provider`) は iOS・Android・MAUI の 3 形態にある

## 検討した選択肢 (却下案と理由を含む)

### 印の新しい名前

| 案 | 内容 | 判定 |
|---|---|---|
| A | `markAsCurrentPage()` | 却下。付けると何が起きるかは読め、基準領域の値 (`currentPage` / `CURRENT_PAGE`) と同じ語でつながるが、ダイアログ用だという手がかりが名前に残らず、他のライブラリとぶつかる余地を名前で抑えられない |
| B | `dialogCurrentPage()` (`ks` を外すだけ) | 却下。今と同じく何かを取ってくる関数に見える。SwiftUI 自身の `dialog` で始まる modifier (`.dialogIcon` など) と並び、Apple の標準に見えやすい |
| C | `markAsDialogCurrentPage()` | **採用**。付けると何が起きるかが読め、ダイアログ用だと名前で分かる。長さは、IDE の補完やエージェントが書くなら関係ない (オーナー) |

### `ks` 付きの仲間 (属性を付ける 9 個) の扱い

| 案 | 内容 | 判定 |
|---|---|---|
| A | 今のまま残し、付ける理由を規則として書き残す | **採用** → core/ADR-0045 |
| B | SwiftUI の modifier だけ `ks` を外す | 却下。SwiftUI 標準の `dialog` で始まる modifier と見分けにくい。2026-09-06 に揃えた UIKit との綴りがまた崩れる |
| C | すべての `ks` を外す | 却下。OS の View 型に接頭辞のない一般的な名前が生えてぶつかる余地が生まれる。公開済みのメンバーがすべて変わる大きな変更になる |

beta の利用者はほぼいないため、互換への影響は決め手にしない (オーナー判断)。

## 決定事項

- 2026-09-30: 印を SwiftUI・Compose とも `markAsDialogCurrentPage()` に改名する。旧名は残さない (未リリースで利用者がいない)
- 2026-09-30: `ks` 付きの属性メンバー 9 個は変えない。`ks` を付けるのは OS の View 型への後付けプロパティと、その SwiftUI の対に限る (core/ADR-0045)。印はどちらにも当たらないので `ks` を付けない
- 2026-09-30: 登録口の型名 `DialogCurrentPage` は変えない。新しい印の名前が型名をそのまま含み、「印を付ける」と「関数で教える」が同じ語でつながるため
- 印の名前が出てくるところはすべて新しい名前に揃える: 説明コメント、「印が見つからない」ときの診断メッセージ、Compose のデバッグ表示名 (inspector の name)、Compose 側のファイル名 (`KsDialogCurrentPage.kt`)
- 蒸留時に反映: decisions/core/0045 — accepted に昇格
- 蒸留時に反映: concepts/ios/api/layout-surface.md・concepts/android/api/layout-surface.md — 印の名前を新しい名前へ。ADR-0045 の `ks` の付け分けの規則を iOS / Android の公開面に書く (置き場所は蒸留時に決める。今は concepts/ios/api/transition-surface.md に「`ks` 接頭辞で揃う」の注記がある)
- 蒸留時に反映: handbook/cross/sample-parity.md — 「表示中のページの名乗り」の行の印の名前を新しい名前へ
- skills/ は現時点で印に触れていないため、docs-refresh で追従するものはない

## ADR 候補 (作成済み: core/ADR-0045 (proposed) / 未起票: なし)

- core/ADR-0045「接頭辞 ks は OS の View 型に後付けする属性とその SwiftUI の対に限る」(proposed)
- 印の名前そのものは、1 つの API だけに効く決定なので ADR にしない (オーナー「限定的すぎる決定はいらない」)

## 未決の論点

- 旧名が解決できないことを公開面の検査に加えるか (2026-09-06 の契約の型名の改名では、4 形態に負のコンパイル検査を加えた)。→ 2026-09-30 の ksn-propose で決着: 足さない。旧名は一度も配布しておらず利用者のコードを守る検査にならないため、旧名の残存検索で確かめる (proposal.md の Non-Goals)

## 影響範囲 (追随先)

- iOS 本体: `ios/Sources/KsDialogs/SwiftUI/View+DialogCurrentPage.swift` (定義)、`Contract/DialogCurrentPage.swift`・`Contract/DialogLayoutArea.swift`・`CurrentPage/DialogCurrentPageMarkerView.swift` (説明コメント)、`CurrentPage/DialogCurrentPageLedger.swift` (診断メッセージと由来の文言)
- iOS テスト: `ios/Tests/KsDialogsTests/` の `DialogCurrentPageCompileChecks.swift`・`DialogCurrentPageSwiftUITests.swift`・`Support/CurrentPageSwiftUI*.swift` 5 本
- Android 本体: `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/compose/KsDialogCurrentPage.kt` (定義・inspector 名・ファイル名)、`android/ksdialogs-core/src/main/kotlin/jp/kamusoft/ksdialogs/` の `DialogCurrentPage.kt`・`DialogLayoutArea.kt` (説明コメント)、`DialogCurrentPageLedger.kt` (診断メッセージ)
- Android 検査・テスト: `android/api-surface-check/.../DialogCurrentPageApiSurfaceChecks.kt`、`android/ksdialogs/src/androidTest/.../compose/ComposeCurrentPageTests.kt`・`support/CurrentPageComposeTestActivity.kt`
- Sample 4 ルート: `samples/ios/KsDialogsSample/SampleLayoutPanelScreen.swift`、`samples/kmp/iosApp/KsDialogsSampleKmp/SampleLayoutPanelScreen.swift`、`samples/android/app/.../SampleLayoutPanelScreen.kt`、`samples/kmp/androidApp/.../SampleLayoutPanelScreen.kt`
- MAUI・KMP の共有層: 追随不要 (印を持たない)

## UI 素材 (ui/references/ の一覧と注釈)

なし (見た目は変わらない)

## 変更級の推奨: M (オーナー確定 2026-09-30)

公開 API の小変更 (未リリースの印の改名) で、iOS と Android の 2 形態と Sample 4 ルートにまたがる。挙動・見た目の変更はない。デルタスペックに新しい名前を書き、旧名の消し忘れを verify で機械的に突き合わせられるようにする。前例は、1 形態の SwiftUI 演出 modifier の改名 (S) と、4 形態の契約の型名の改名 (M、「S でも成立するが迷ったら一段上」)。
