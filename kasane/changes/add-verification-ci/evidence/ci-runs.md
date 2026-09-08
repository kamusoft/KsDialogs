# CI の実動確認 (tasks 5.1 / 5.2)

GitHub Actions の run 番号と結果。ランナーは `macos-26-arm64` / `ubuntu-24.04`。

## 5.1 `develop` push で lint + 5 job が起動・成功する

| run | commit | 内容 | 結果 |
|---|---|---|---|
| 34183364275 | 2d77ef1 | 実装一式の push (ソース混在) | lint / changes / android / android-instrumented / kmp / maui 成功、**ios 失敗** (提示待ちの時間切れ 3 件。再実行では 9 件超に増え、回ごとに入れ替わり) |
| 34184817756 | 1a96389 | ios job のスイート並列を停止 (`-parallel-testing-enabled NO`) | **全 job 成功**。ios は 277 件 (Swift Testing) 実行 |

所要時間 (run 34184817756、キャッシュなし):

| job | 所要 | timeout (実測後) |
|---|---|---|
| lint | 8 秒 | — |
| changes | 6 秒 | — |
| ios / verify | 5 分 22 秒 | 20 分 |
| android / verify | 1 分 35 秒 | 10 分 |
| android-instrumented / verify | 10 分 1 秒 | 30 分 |
| kmp / verify | 6 分 47 秒 | 25 分 |
| maui / verify | 12 分 8 秒 (初回 run は 9 分 10 秒) | 35 分 |

android-instrumented は API 36 で `:ksdialogs` / `:ksdialogs-compose` とも全件通過 (手元の headless API 35 で落ちた `LD_WN_01` は CI では再現せず)。

## 5.2 起動条件

| Scenario | 確認 | 結果 |
|---|---|---|
| 記録だけの push では lint だけが走る | run 34185606795 (b3c6f7f、`kasane/` の tasks.md のみ) | lint / changes 成功、ios / android / android-instrumented / kmp / maui は skipped、run 全体は success |
| 記録とソースが混ざった push では全 job が走る | run 34184817756 (1a96389、workflow + kasane) | lint + 5 job がすべて実行され成功 |
| 連続する push で古い実行が打ち切られる | run 34184817756 を再実行中に b3c6f7f を push | 再実行は cancelled、新しい push の run だけが残った |

status check 名は `lint` / `ios / verify` / `android / verify` / `android-instrumented / verify` / `kmp / verify` / `maui / verify` の 6 件と補助の `changes` で報告された (`main` 宛て PR での確認は phase-9)。
