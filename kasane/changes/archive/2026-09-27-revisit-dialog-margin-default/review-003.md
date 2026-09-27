# レビュー結果: revisit-dialog-margin-default (003 回目)

**日付**: 2026-09-27
**判定**: APPROVED

## サマリー

修正サイクルの小さな再確認。対象は review-002 の Suggestion 1 件 (`evidence/test-run-summary.md` の Compose の行に件数が無い) だけで、ほかは review-002 で APPROVED 済みである。該当行は `N tests / M failures` の形で 3 回分の件数を持つようになり、出典の review-001 の実行結果表と件数・経緯のどちらも食い違わない。新たな指摘は無い。

## 照合した規約

- test-execution.md (テスト結果の報告)。冒頭の「実行件数 (`N tests / M failures`) を併記する」と「実行した件数は変更の証跡に残す」、android/ (instrumented) 節の「件数はモジュールごとに別の行で流れる」を照合した

## 前回指摘の解消確認

| # | 前回の指摘 (重要度) | 対応 | 確認結果 |
|---|---|---|---|
| 1 | 証跡のレビュー再実行の節で、Compose の失敗件数が数値になっていない (Suggestion、review-002) | `evidence/test-run-summary.md:30` に 3 回分の件数と経緯を記入 | **解消**。下の突き合わせのとおり |

### `evidence/test-run-summary.md:30` と review-001 の突き合わせ

| 項目 | 証跡の記載 | review-001 の記録 (レビュー側の実行結果表・instrumented の行) | 一致 |
|---|---|---|---|
| 総件数 | 52 | ksdialogs (Compose) 52 tests | 一致 |
| 1 回目 | 52 tests / 2 failures。起動直後のランチャーの ANR ダイアログが前面にあり、入力の注入が失敗 | 起動直後のランチャーの ANR ダイアログが前面にあり、入力の注入で 2 件失敗 (環境要因) | 一致 |
| 2 回目 | ANR を閉じた後 52 tests / 1 failure。`ComposeCurrentPageTests` の最初の 1 件が、配置が落ち着く前に 10 秒で時間切れ。1 回目では成功 | ANR を閉じた 2 回目は 1 件失敗 (ComposeCurrentPageTests の最初の 1 件が、配置が落ち着かないまま 10 秒で時間切れ)。1 回目では同じテストが成功 | 一致 |
| 単独の再実行 | `ComposeCurrentPageTests` 単独で 7 tests / 0 failures | 同クラスを単独で再実行すると 7/7 成功 | 一致。`android/ksdialogs/src/androidTest/kotlin/jp/kamusoft/ksdialogs/compose/ComposeCurrentPageTests.kt` の `@Test` も 7 件で、クラスの全件にあたる |

test-execution.md の書き方も満たしている。3 回とも `N tests / M failures` の形で書かれている。instrumented の件数はモジュール別に数えるという規約どおり、`:ksdialogs-core` の行 (`:29`) とは別の行になっている。失敗した回は、原因 (ANR・配置前の時間切れ) と、単独の再実行で成功したことまで読める。review-002 が指摘した、iOS の行 (`:27`) と書き方がそろっていない点も解消した。

## 指摘事項

なし。

## 確認した観点 (指摘なし)

- **範囲外への変更の持ち込み**: 依頼の範囲は該当行の 1 か所だけで、コード・足場は確認の対象外 (review-002 で APPROVED 済み)
- **証跡の個人情報・ローカルパス**: 該当行にローカル絶対パスや端末の個体識別子は無い。端末は「API 35 AVD」の粒度 (`:29`) にとどまる
- **テストの再実行**: 今回の差分は証跡の文言だけで、テストの結果は変わり得ない。そのため再実行していない

## アクションプラン

1. 対応は不要。蒸留時の申し送りは review-001 のアクションプラン 4 のとおり
