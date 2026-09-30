# 検証 CI の結果 (完了判定の証跡)

対象: `develop` の `663d4cd` (この change の実装 commit)。検証 CI run 36698778660 (2026-09-30) は全 job 成功。

| job | 結果 | 件数 |
|---|---|---|
| lint | success | — |
| android / verify | success | `:ksdialogs-core:testDebugUnitTest` 109 件 (失敗 0) |
| android-instrumented / verify | success | `:ksdialogs` 52 件 (skipped 0)・`:ksdialogs-core` 383 件 (skipped 1)、実行数の合計 434 件・失敗 0 |
| ios / verify・kmp / verify・maui / verify | success | — |

変更前の同じ構成 (run 36687882779、`cfa0b73`) では次の 2 件が落ちていた。

- `android / verify`: `AttachedHostRetentionTests`「描画済みの画面に載った Toast は、背面へ下がって戻っても外れない」が `ConcurrentModificationException` (attempt 1。失敗 job の再実行 attempt 2 では通過 — 間欠)
- `android-instrumented / verify`: `DialogCurrentPageTests`「同じ_Activity_のモーダルのウィンドウに載った_View_は原点が違ってもその位置が基準になる」が前提の assert (左端のずれが 0) で失敗 (attempt 1 / 2 とも)。CI の AVD の画面は 320x640 / 160dpi (ログ `Setting display: 0 configuration to: 320x640, dpi: 160x160`)

CME の解消の根拠は、繰り返し実行の成功回数ではなく、(1) 書き換えるのと同じ UI スレッドで読む構造にしたこと、(2) テストのスレッドから Toast の列を走査する箇所が残っていないことの grep (review-001)、(3) 上の検証 CI の緑、の 3 点とする (修正前も大半の回で通っていたため、回数では効果を示せない)。
