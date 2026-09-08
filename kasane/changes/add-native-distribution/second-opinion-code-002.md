# セカンドオピニオン: add-native-distribution (code-002)
**相方**: codex / **label**: so-code-add-native-distribution (同セッション 2 ターン目) / **日付**: 2026-09-08 / **対象**: 修正後の作業ツリーの HEAD (aa71409) からの全差分 (tasks 7.1〜7.3 を除く)
---
# 再レビュー結果: add-native-distribution

**判定: APPROVED**

件数: **Critical 0 / Major 0 / Minor 2 / Suggestion 0**

前回の Major 2 件と、ホスト側で採用された指摘はいずれも解消されています。新規 Minor 2 件はありますが、現在の変更目的や安全性を阻害するものではないため、判定基準に従い APPROVED とします。

## 前回指摘の状態

| 前回の指摘 | 状態 | 確認結果 |
|---|---|---|
| tag 削除・照会失敗を検出できず偽成功する | **解消** | 操作着手前のフラグ設定、remote/local の事後照会、照会不能時の失敗化、成功処理後の cleanup 不成立時の exit 1 が実装されています。 |
| API 36 エミュレータを実機へ置き換えた乖離が未記録 | **解消** | `deviation.md:4` と `evidence/instrumented-test-counts.txt` に実行環境、理由、件数、オーナー合意が記録されています。合意済み差分として適切です。 |
| proposed／改訂前 ADR をコメントから参照 | **解消** | 対象3箇所から不適切な ADR 参照が除かれ、自己完結した説明へ変更されています。ただし置換後の文言について新規 Minor 1 件があります。 |
| remote 到達の事前検証 | **解消** | `fetch`、既定ブランチ特定、`merge-base --is-ancestor` により、HEAD が remote の既定ブランチへ到達していることを tag 作成前に検査しています。 |
| `grep -qxF` | **解消** | remote/local の対象 tag 判定が固定文字列完全一致になっています。 |
| ログ末尾の取りこぼし | **解消** | 本体を同期パイプラインとして実行し、親 shell が `tee` の終了を待つ構造になっています。 |
| handbook の `timestamp` | **解消** | `diagnostic-message-language.md` は `2026-09-08` に更新されています。 |
| 「鍵が漏れて」の曖昧なコメント | **解消** | 「鍵の指定が抜けて」に修正されています。 |

## 指摘事項

### [🟡 Minor][新規] remote 到達確認の `fetch` が比較対象外の local tag を増やし得る

**該当箇所**: `scripts/spm-snapshot/verify-https-resolution.sh:137`、`scripts/spm-snapshot/verify-https-resolution.sh:144`、`scripts/spm-snapshot/verify-https-resolution.sh:153`

**問題点**: remote/local の tag 一覧を保存した後で `git fetch origin` を実行しています。通常の `fetch` は取得対象の履歴を指す tagを自動追随するため、remote に存在する別の tag が local にまだ無い場合、その tag が local に追加されます。

その結果、検証用 tag を正しく削除しても、実行後の local tag 一覧が `tags_local_before` と一致せず、後始末失敗として終了します。対象 tag を取り残す危険ではありませんが、正当な作業コピーで検証が偽失敗する可能性があります。

**推奨修正**: remote 到達確認を tag 一覧の保存より前へ移すか、到達確認の fetch を `git fetch --quiet --no-tags origin` として、比較対象外の tag を変更しないようにしてください。

### [🟡 Minor][新規] Android の version 導出式が KMP まで統一するように読める

**該当箇所**: `android/build.gradle.kts:27`

**問題点**: コメントは、この導出式によって「KMP を含む全形態」の version が同じ文字列になると説明しています。しかし現在の KMP は `kmp/ksdialogs-kmp/build.gradle.kts:13` で `version = "0.1.0"` を独立指定しており、proposalでも KMP の version 配線は phase-7 の Non-Goal です。

元の不適切な ADR 参照は解消されていますが、置換後の説明が現在の実装範囲を超えています。

**推奨修正**: 「Android の両 artifact の version が同じ注入値から導出される」など、現時点でこのファイルが保証している範囲に限定してください。

## 補足

- tasks 7.1〜7.3と対応する実リモート Scenario は、指定どおり判定対象外です。
- README／利用者向け Skills の旧座標は proposal の明示的な Non-Goal であり、違反として扱っていません。
- `git diff --check HEAD` と全シェルスクリプトの `bash -n` は問題ありませんでした。
- ファイルへの書き込み、ビルド、テスト、ネットワーク操作は実施していません。


## 突き合わせ結果 (ホスト側 review-002.md との照合、2026-09-08)

| 指摘 | ホスト側 | 相方 | 採否 |
|---|---|---|---|
| remote 到達確認の `git fetch origin` が local tag のベースライン取得後に走り、tag 自動追従で後始末の同一性判定が偽失敗する | Minor | Minor | **確定 (Minor)** — fetch をベースライン取得より前へ移す |
| `android/build.gradle.kts` の version 導出式コメントが「KMP を含む全形態」と現時点の保証範囲 (Android 2 artifact) を超える | なし | Minor | **採用 (Minor)** — kmp/ の version 直書きが残る現状 (phase-7 の責務) を正確に指す文言へ |
| 内部フラグ `VERIFY_HTTPS_RESOLUTION_LOGGING` が外から立っていると `$3` 未定義で落ちる | Suggestion | なし | 同じ修正で対処 |

未解決 (矛盾) の指摘: なし。降格: なし。両者とも判定は APPROVED。
