# 実装乖離メモ: add-native-distribution

- [付随修正] `android/gradle/libs.versions.toml` のコメント 3 箇所と `android/api-surface-check` の doc コメント 2 ファイル (`ToastApiSurfaceChecks.kt` / `RejectsComposeToastApiFromCoreModule.kt`): 座標リネーム (Requirement: Android module の座標と構成) に伴い、旧座標 `jp.kamusoft:ksdialogs` (本体) / `jp.kamusoft:ksdialogs-compose` の字面が誤りになるため新座標へ置換。値・コードは不変 (2026-09-08)
- Scenario「instrumented test の無改変実行」(tasks 1.2): spec では API レベル 36 のエミュレータ (検証 CI の instrumented job と同じ条件) で実行 → 指示により API 36 の実機で実行し、件数 294 / 39 が改名前と一致・全件成功したことを完了条件とした (evidence/instrumented-test-counts.txt)。理由: 手元環境に API 36 のシステムイメージ・AVD が無く、エミュレータ条件は CI の instrumented job が push 時に担保する (2026-09-08)
