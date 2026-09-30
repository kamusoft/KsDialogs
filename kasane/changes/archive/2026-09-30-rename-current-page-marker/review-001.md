# レビュー結果: rename-current-page-marker (001 回目)

**日付**: 2026-09-30
**判定**: APPROVED

## サマリー

iOS・Android・Sample 4 ルートにわたる印の改名 (`ksDialogCurrentPage()` → `markAsDialogCurrentPage()`) は、デルタスペックどおりの純粋な名前の置き換えになっている。Android の印を定義するファイルの改名 (`compose/KsDialogCurrentPage.kt` → `compose/DialogCurrentPageModifier.kt`) も、旧ファイルとの中身の差分は関数名・KDoc の参照・inspector の `name` の 3 行だけで、挙動・可視性・構造は変わっていない。旧名は ios/・android/・samples/ から消えており、残存は proposal が蒸留時に直すと定めた長命層と経緯の記録だけ。Critical / Major / Minor の指摘はない。

## 照合した規約

- `kasane/handbook/cross/comment-policy.md` (常時 — Swift / Kotlin のコメントを書き換えている)
- `kasane/handbook/cross/test-execution.md` (テスト結果の報告・変更の完了判定)
- `kasane/handbook/cross/diagnostic-message-language.md` (ライブラリ本体の診断文言を変えている)
- `kasane/handbook/cross/sample-parity.md` (`samples/` を触っている)
- `kasane/lessons/code-review.md` の重点観点 L-001・L-002 (指摘しないことは「まだなし」)
- 関連 ADR: `kasane/decisions/core/0045-ks-prefix-limited-to-view-attachments.md` (proposed。決定ではないので照合は所見のみ)
- ドメインスキル: swift-ui-impl-skill (ios)、kotlin-impl-skill・jetpack-compose-impl-skill (android)

## 確認した観点と結果

### 仕様充足
- 足場の凍結: `git diff HEAD -- kasane/` で変わっているのは `tasks.md` のチェックだけ。proposal / specs / exploration は 077da71 のまま (逆流なし)
- deviation.md: なし。diff の全行がいずれかの Scenario に対応しており、付随修正の同梱もない
- tasks.md のチェック 0.1〜4.1: 対応する変更と証跡 (`evidence/`) がすべて存在し、虚偽チェックなし
- 旧名・deprecated 別名: iOS `ios/Sources/KsDialogs/SwiftUI/View+DialogCurrentPage.swift:19`、Android `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/compose/DialogCurrentPageModifier.kt:35` の 1 宣言ずつで、別名は残していない
- 診断文言: iOS `ios/Sources/KsDialogs/CurrentPage/DialogCurrentPageLedger.swift:22`・`:67`、Android `android/ksdialogs-core/src/main/kotlin/jp/kamusoft/ksdialogs/DialogCurrentPageLedger.kt:91` とも英文・文型は同じで、名指しだけが新しい名前 (diagnostic-message-language.md の英語固定に適合)
- inspector の表示名: `DialogCurrentPageModifier.kt:44` が `name = "markAsDialogCurrentPage"`
- Sample: 4 ルートとも変わったのは印の呼び出し (Swift 4 行・Kotlin 4 行) と Kotlin の import 2 行だけ。付ける位置は同じ。MAUI の Sample は無変更 (sample-parity.md の「表示中のページの名乗り」の行の MAUI 側の扱いと一致。同じ行の印の名前は proposal どおり蒸留で直す)

### 差分の形の独立確認
- HEAD の旧 `KsDialogCurrentPage.kt` と未追跡の `DialogCurrentPageModifier.kt` を `diff` で比べ、差が 35・37・44 行目の 3 行 (いずれも旧名 → 新名) だけであることを確かめた。パッケージ・可視性 (`public fun` / `private data object` / `private class`)・Node の attach / detach・`boundsInWindow` は同一
- それ以外の 22 ファイルは `git diff HEAD -U0` で全ハンクが 1 行対 1 行の置き換えで、各行の差は `ksDialogCurrentPage` → `markAsDialogCurrentPage` だけ

### 残存の独立確認
- `git grep --untracked -n ksDialogCurrentPage` の結果、ios/・android/・samples/・kmp/・maui/・skills/・README 群は 0 件。残りは `kasane/changes/` (本 change とアーカイブ)、`kasane/decisions/core/0045-…`、`kasane/concepts/{ios,android}/api/layout-surface.md`、`kasane/handbook/cross/sample-parity.md` だけで、`evidence/diff-and-residue.md` の表と一致する
- `KsDialogCurrentPage` (旧ファイル名の綴り) も change 外に 0 件

### テスト (L-002: test-execution.md の全件実行表との行ごとの突き合わせ)

| ビルドルート | 証跡の件数 | 判定 |
|---|---|---|
| ios/ | 372 tests / 0 failures (Swift Testing)、XCTest 0 件 | 実行済み。基準 4 件すべて Passed |
| android/ | ksdialogs-core JVM 109 tests / 0 failures / 0 skipped。同じ実行で `:api-surface-check:compileDebugKotlin` 成功 | 実行済み |
| android/ (instrumented) | ksdialogs-core 383 / 0 failures / 1 skipped、ksdialogs 52 / 0 failures (API 36 エミュレータ) | 実行済み。基準 7 件すべて passed。skip 1 件は API 30 以上で自己 skip する既知のテストで本変更と無関係 |
| kmp/ | 未実行 | 本変更の要件外。kmp が依存する `jp.kamusoft:ksdialogs-core` の差分は文字列リテラルとコメントだけで、kmp 内に旧名・診断文言の参照は 0 件 |
| maui/ | 未実行 | 同上。MAUI 層に旧名・診断文言の参照は 0 件 |
| maui/android/native/ | 未実行 | 同上。依存は `ksdialogs-core` だけ (`ksdialogs-maui-bridge/build.gradle.kts:49`) |
| maui/macios/native/ | 未実行 | 同上。旧名の参照 0 件 |

デルタスペックの Scenario が名指す手順は ios/ と android/ (JVM・instrumented) の 2 ルートで、証跡はその全件実行と、改名前の基準一覧 (iOS 4 件・Android 7 件) との 1 件ずつの突き合わせを持つ。合計件数だけでの判定になっていない。

追加で、レビュー側でも次を実行して成功を確かめた (テスト本体の再実行はしていない):
- `android/` で `./gradlew :api-surface-check:compileDebugKotlin :ksdialogs:compileDebugAndroidTestKotlin` → 終了コード 0
- `ios/` で `xcodebuild build-for-testing -scheme KsDialogs -destination 'generic/platform=iOS Simulator'` → 終了コード 0。変更ファイルに由来する警告・エラーなし (公開面の正の検査 `DialogCurrentPageCompileChecks.swift` を含むテストターゲットがコンパイルされる)

### 設計品質
- 命名: core/ADR-0045 (proposed) の「`ks` は OS の View 型への後付け属性とその SwiftUI の対に限る」と整合する (印は後付けの属性ではないので `ks` を付けない)。proposed のため判定の根拠にはしていない
- Kotlin のファイル名 `DialogCurrentPageModifier.kt`: トップレベル宣言だけのファイルに中身を表す名前を付ける Kotlin の慣習に沿い、tasks 2.1 の例示とも一致する
- コメント: 書き換えたコメントは日本語のまま、外部 ID に依存しない (comment-policy.md に適合)
- 堅牢性・性能・リソース: 挙動のコードに差分がないため、新たな観点は生じない

## 指摘事項

なし。

## 所見 (指摘ではない)

- 蒸留時に `kasane/handbook/cross/sample-parity.md` の「表示中のページの名乗り」の行と `kasane/concepts/{ios,android}/api/layout-surface.md` の印の名前を新しい名前へ直すこと (proposal の「蒸留時に反映」どおり。handbook は規範層なので、蒸留まで handbook とコードの名前が食い違った状態が続く点だけ留意)
- 診断文言の本文を直接確かめるテストはどちらのプラットフォームにも無い。デルタスペックはこの点を差分の目視 (Scenario「差分は旧名から新名への置き換えだけ」) で判定すると定めており、本変更の要件は満たしている

## アクションプラン

なし (APPROVED)。蒸留へ進んでよい。
