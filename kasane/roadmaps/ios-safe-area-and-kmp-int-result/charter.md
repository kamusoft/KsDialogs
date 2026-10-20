# 委任状: ios-safe-area-and-kmp-int-result

承認: 2026-10-20 オーナー

## 種類
機能追加 (既存の挙動と公開面の修正)

## 完了条件
| # | 条件 | 確かめ方 |
|---|---|---|
| 1 | 2 フェーズがどちらも蒸留済み | `roadmap.md` のフェーズ一覧で、2 行とも `completed`、Change 列が `changes/archive/...` |
| 2 | 統合ブランチで ios/ の全件テストが通る | `cd ios && xcodebuild test -scheme KsDialogs -destination 'platform=iOS Simulator,id=<レーンの Simulator>' -collect-test-diagnostics never` が成功し、`Test run with N tests` の N が 0 でない |
| 3 | 統合ブランチで kmp/ の全件テストが通る | `cd kmp && ./gradlew allTests --rerun-tasks` が成功し、`ksdialogs-kmp/build/test-results/` の件数が 0 でない |
| 4 | 仕様の Scenario ID がテスト名に網羅されている | `python3 scripts/scenario-id-coverage.py` が成功する |
| 5 | 窓全体基準で端に付けた SwiftUI の中身 (Dialog・Loading・Toast) から見える安全領域が、器と安全領域の重なりに等しく、既定の可視領域では 0 である | phase-1 の change で足したテストが 2 の結果に現れて通っている。Simulator での実測の記録が phase-1 の change の `evidence/` にある |
| 6 | Dialog の KMP 向け Swift 面で結果型に `Int` を指定すると、`Int32` / `Int64` を案内する診断でコンパイルエラーになる | phase-2 の change の `evidence/` に、負のコンパイル検査 (`build-for-testing` と検査フラグ) が期待した診断で失敗した記録がある。検査フラグが `kasane/handbook/cross/test-execution.md` の表にある |
| 7 | concepts が新しい挙動と型の対応を記述している | `kasane/concepts/ios/api/` に器の中の SwiftUI の中身から見える安全領域の記述があり、`kasane/concepts/kmp/api/ios-host-integration.md` に `Int32` / `Int64` の対応と、共有コードからの show では結果型の食い違いが検出されないことの記述がある |
| 8 | ios/ADR-0002 が実装と一致している | ADR が `accepted` で、本文が採った方向 (標準の安全領域として届ける) と一致している |

## 見た目の基準 (UI に触れるフェーズがあるときのみ)
承認済みモックは使わない。phase-1 の見た目の基準は ios/ADR-0002 の挙動そのもの: 既定の可視領域では見た目が変わらず、窓全体基準で端に付けた SwiftUI の中身だけが安全領域の内側に寄る (安全領域を無視する指定があれば寄らない)。これ以外の見た目の変更は「任せない判断」に当たる。
| フェーズ | 承認済みモック |
|---|---|
| phase-1-ios-swiftui-content-safe-area | なし (上の挙動が基準) |

## 判断の優先順
1. この委任状に書いた方針
2. accepted の ADR・handbook の規約・concepts の記述
3. (移行) 原本の挙動 — 該当なし
4. 元に戻しやすく、触るファイルが少ない案
5. 1〜4 で案が並んだら ksn-advise の助言を材料に決める

方針:
- phase-1 の方向は ios/ADR-0002 (proposed だがオーナーが採用済み)。窓全体基準で端に付けた SwiftUI の中身の見た目が変わることは承認済みの挙動変更として扱う
- phase-2 の、結果型に `Int` を指定するとコンパイルエラーになる変更は、承認済みの公開 API の破壊的変更として扱う (任せない判断の「公開 API の破壊的変更」に当てない)。PR の `## Changes` に書く `- breaking:` の英文の案を、phase-2 の change に残す
- 診断・エラー文は英語で書く (`kasane/handbook/cross/diagnostic-message-language.md`)
- テストの結果は実行件数まで確かめ、件数を change の `evidence/` に残す (`kasane/handbook/cross/test-execution.md`)

## 任せない判断
- ゴール / 非ゴール・前提の変更
- 取り消せない操作 (データの削除・本番環境への適用・外部への公開や送信・課金)
- 秘密情報 (認証情報・鍵・トークン) の作成と入力
- 公開 API・データ形式の破壊的変更 (方針に書いた phase-2 の変更を除く)
- accepted の ADR の改訂 (supersede・amends)
- 関連リポジトリ (`relations` の相手) への書き込み
- phase-1 の実測で器のサイズ計算が崩れ、独自の値で渡す案 (B) に倒す必要が出たとき: B は新しい公開 API の形を決めることになり、Android との対応も絡むため、実測の結果と崩れ方を `evidence/` に残して保留にする

## ロードマップを変えてよい範囲
- 可: フェーズの追加 (修正フェーズを含む)・分割・順序と依存の変更・完了条件に関わらないフェーズの取り下げ
- 不可: ゴール / 非ゴール・前提の変更、完了条件の変更、完了条件に関わるフェーズの取り下げ

## 検証手段と端末
| 用途 | 端末の種類 | 台数 | 使うフェーズ |
|---|---|---|---|
| ios/ と kmp/ のテスト・負のコンパイル検査・安全領域の実測 | iOS Simulator (ノッチのある iPhone。レーンごとに別の 1 台) | 2 | phase-1・phase-2 |
統合確認: ビルド `cd kmp && ./gradlew compileCommonMainKotlinMetadata compileIosMainKotlinMetadata` / テスト `cd ios && xcodebuild test -scheme KsDialogs -destination 'platform=iOS Simulator,id=<レーンの Simulator>' -collect-test-diagnostics never`、`cd kmp && ./gradlew allTests --rerun-tasks`、`python3 scripts/scenario-id-coverage.py`
worktree の準備: メインのチェックアウトの `android/local.properties` と `kmp/local.properties` を、新しい worktree の同じ位置へコピーする

## 予算
- ksn-conductor の起動回数の上限 (1 回の実行あたり): 6 (フェーズ担当・蒸留担当・修正フェーズ・再起動を含む)
- 並列数: 2
- 止める条件: 既定の 2 項目 (同じ担当の停止報告が同じ論点で 2 回続いたらそのフェーズを保留 / 統合確認で同じテストが 2 回続けて落ちたら全体を止める) のみ

## 禁止事項
- `skills/` と `README.md`・`README_ja.md` を変更しない (docs-refresh の担当)
- `samples/`・`android/`・`maui/` を変更しない (ロードマップの非ゴール)
- `kasane/config.yaml` の `lint.ci-skip.allow` に項目を足さない (オーナーだけが足す)
- `main` へのマージ・push、タグの作成、リリースの起動をしない
