# レビュー結果: revisit-dialog-margin-default (002 回目)

**日付**: 2026-09-27
**判定**: APPROVED

## サマリー

修正サイクルの再確認。review-001 と second-opinion-code-001 の指摘 4 件 (Minor 2・Suggestion 2) は、どれも指摘どおりに直っていた。新たな問題は持ち込まれていない。変更範囲は Sample 5 ファイルのコメント、`evidence/test-run-summary.md` の新規作成、MAUI の `DialogOptions.cs` の行コメントの位置、Android の `ToastDefaultContentView` の companion の可視性の 4 点で、ほかの差分は review-001 で APPROVED 済みである。残るのは証跡の書き方についての Suggestion 1 件だけ。

## 照合した規約

- comment-policy.md (always)。「禁止する記述類型」「公開メンバーの doc コメント」「書き換え時の判断基準」の各節を照合した
- test-execution.md (テスト結果の報告・変更の完了判定)。冒頭の件数と証跡の節、各ルートの件数の得方 (iOS の 2 系統、Android の XML 集計、instrumented の API レベル別 skip) を照合した
- lessons/code-review.md (「指摘しないこと」は無し)

## 前回指摘の解消確認

| # | 前回の指摘 (重要度) | 対応 | 確認結果 |
|---|---|---|---|
| 1 | Sample 5 ファイルのコメントに履歴記述「旧既定値」(Minor) | 5 ファイルとも「0 は契約の既定値、24 は既定 Toast のデフォルト View が自分に持つ余白と同じ値。」に書き換え | **解消**。`samples/ios/KsDialogsSample/SampleMarginChoice.swift:3`、`samples/kmp/iosApp/KsDialogsSampleKmp/SampleMarginChoice.swift:5`、`samples/android/app/src/main/kotlin/jp/kamusoft/ksdialogs/samples/android/SampleMarginChoice.kt:6`、`samples/kmp/androidApp/src/main/kotlin/jp/kamusoft/ksdialogs/samples/kmp/android/SampleMarginChoice.kt:8`、`samples/maui/KsDialogs.Sample.Maui/SampleMarginSegmentsView.xaml.cs:6` で文言が 5 ファイルとも一致する (Sample パリティ)。書かれているのは現在の意味だけで、24 の説明は実装 (`ToastDefaultContentView` の添付値 24) と一致する。差分のソース全体を「旧既定」「以前は」「変更前」で検索したところ、残りは作業文書 (`ui/brief.md`・レビュー記録) だけで、ソースコメントには無い |
| 2 | テストの実行件数が `evidence/` に無い (Minor) | `evidence/test-run-summary.md` を新規作成 | **解消**。test-execution.md の要求を満たしている: ルートごとの件数 (7 ルートすべて 0 でない)、iOS の 2 系統 (Swift Testing 321・XCTest 0) の明記、kmp の 2 ターゲットの内訳、instrumented の skip 1 件と失敗 5 件の理由 (HEAD でも同じ例外で落ちる環境要因であり、専用 AVD での再実行では 0 件)、この change で足したテストが結果に現れていること、機構を外すと失敗することの確認、Scenario ID 網羅検査の結果。記載値は review-001 の実行結果表と食い違わない |
| 3 | MAUI `DialogMargin` の XML doc と宣言の間に行コメント (Suggestion) | 行コメントを `<summary>` の上へ移動 | **解消**。`maui/KsDialogs.Maui/Contract/DialogOptions.cs:29-31` は、行コメント → `/// <summary>` → 宣言の順になった。iOS (`DialogOptions.swift:35`) と Android (`DialogOptions.kt:28`) と同じく、根拠の行コメントを宣言の前に置いている。ビルドで出力された `KsDialogs.Maui.xml` に `P:KsDialogs.DialogOptions.DialogMargin` の summary が出ていることを確かめた (doc の関連付けは壊れていない)。ADR ID は非公開の行コメントにあり、公開 doc には入っていない |
| 4 | Android `ToastDefaultContentView` の companion を internal に広げる必要がない (Suggestion) | `private companion object` のまま `attachedOptions` を足す形に戻した | **解消**。`git diff HEAD` で見ると、companion の宣言行 (`ToastDefaultContentView.kt:131`) は変更前と同じ `private companion object` で、差分は `attachedOptions` の追加 (`:132-138`)、`init` での添付 (`:46-48`)、クラス doc の 2 行 (`:22-23`) だけになった。`attachedOptions` を参照しているのはこのファイルだけ (android / maui/android / kmp を検索して確認) で、テストは 24 を自分の定数で持つため、可視性を狭めても影響は無い |

## レビュー側の実行結果 (2026-09-27)

| 対象 | 件数 | 結果 |
|---|---|---|
| android/ `./gradlew :ksdialogs-core:compileDebugKotlin :ksdialogs-core:compileDebugAndroidTestKotlin test --rerun-tasks` | unit 75 tests / 0 failures (`TEST-*.xml` の集計) | 成功。2 つのコンパイルも通った |
| maui/ `dotnet test` | 197 / 0 failures | 成功。ビルドで doc 関連の警告 (CS1587 等) は出ていない |
| `comment-policy-lint.py --advisory --paths` (4 点の対象 7 ファイル) | — | 禁止 0 件。要確認 3 件は下の「確認した観点」を参照 |
| `local-path-lint.py` / `identity-lint.py` / `doc-structure-lint.py` | — | exit 0 (doc-structure は既存の concepts の分量についての所見だけで、この change とは関係しない) |

Sample 5 ファイルはコメントだけの変更なので、ビルドの再実行は省いた。instrumented / iOS / kmp / bridge は、今回の差分 (コメント・可視性・証跡) で結果が変わり得ないので再実行していない。review-001 の全件実行の結果を引き継ぐ。

## 指摘事項

### 🔵 Suggestion: 証跡のレビュー再実行の節で、Compose の失敗件数が数値になっていない
**該当箇所**: `evidence/test-run-summary.md:30`
**問題点**: test-execution.md は結果の報告に `N tests / M failures` を併記するよう定めている。この行は「環境要因の失敗 ... が出たが、単独の再実行で成功」とだけ書いてあり、何件落ちたかが読めない。review-001 の記録では、1 回目が 2 件、2 回目が 1 件、クラス単独の再実行が 7/7 成功である。iOS の行 (`:27`) は件数を書いているので、書き方もそろっていない。完了判定の根拠 (件数が 0 でないこと・足したテストが現れていること) は他の行で満たされているので、実害は無い。
**推奨修正**: 「52 tests。1 回目 2 件・2 回目 1 件が環境要因で失敗 (ランチャーの ANR・起動直後の時間切れ)。該当クラスの単独再実行で 7/7 成功」のように件数を入れる。直さなくても判定には影響しない。

## 確認した観点 (指摘なし)

- **comment-policy の advisory 3 件**: `ToastDefaultContentView.kt:136` の ADR 参照は `internal class` の `private companion object` の中にあるメンバーの doc で、公開メンバーではない。lint が可視性を推定するときに companion の `private` までは見ないための誤検知で、規約上は「非公開の実装側コメント」に当たり許容される。`DialogOptions.cs:11` と `:49` は今回の差分に含まれない既存の行である
- **新たな問題の持ち込み**: 4 点とも、指摘箇所以外に手が入っていない。Sample のコメントの書き換えは値や挙動に触れていない。MAUI は行の順序だけ、Android は可視性を戻しただけで、ロジックは review-001 で確認した状態のままである
- **足場の凍結**: proposal / specs は、この修正サイクルで変わっていない。tasks.md に、修正サイクルでついた虚偽のチェックは無い
- **証跡の個人情報・ローカルパス**: `evidence/test-run-summary.md` にローカル絶対パスや端末の個体識別子は無い (lint でも確認)。端末は API レベルとエミュレータの別だけで書いてある

## アクションプラン

1. (Suggestion・任意) `evidence/test-run-summary.md:30` の Compose の行に、失敗件数を数値で入れる
2. 蒸留時の申し送りは review-001 のアクションプラン 4 のとおり (sample-parity.md の書き換え、core/ADR-0039 の accepted 化と 0008 への amended-by)
