# Deviation: fix-ios-sample-demo-autoplay

- 決定事項「対象は iOS 系 Sample 3 つを実測して、崩れているものだけ直す」: 探索では崩れている Sample だけを直す範囲 → 指示により、崩れていない KMP iOS Sample (`samples/kmp/iosApp/KsDialogsSampleKmp/SampleMenuScreen.swift`) の待ち方も同梱で置き換える。KMP iOS は提示先 (前面アクティブのシーンの key window) を 50ms 刻み・上限 2 秒で確かめる待ちを持っており、探索で却下した案 C (Sample がライブラリの提示先探索を複製する) に当たるため、iOS Sample と同じ「シーンが前面でアクティブになってから再生」に揃える (待ちに続く `Task.yield()` も iOS Sample と同じく取り除く)。理由: 同じ目的の Sample 間で待ち方を揃え、却下案を利用者向けの見本として残さない (2026-09-27)
