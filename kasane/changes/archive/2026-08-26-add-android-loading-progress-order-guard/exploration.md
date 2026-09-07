# Exploration: add-android-loading-progress-order-guard

簡易起票 (2026-08-26、add-loading の蒸留時の申し送り。出典: kasane/changes/archive/2026-08-26-add-loading/review-003.md の Minor)

## 課題

「進捗報告の直後に処理が終了しても、最終進捗が終了に追い越されない」という保証の回帰ガードが iOS 側にしか無い。

- iOS はこの保証を `LoadingReportQueue` (受理の鎖 + drain) で機構化し、端から端までの契約テスト 2 本 (`ios/Tests/KsDialogsTests/LoadingProgressTests.swift` / `KsLoadingKmpTests.swift`) で固定している
- Android は現状この保証を満たしているが、その出所は `LoadingCoordinator.kt` のディスパッチャ選択 (`Dispatchers.Main.immediate`) の 1 行にある。この 1 行を `Dispatchers.Main` (immediate なし) に替えるだけで順序は静かに崩れ、既存の LD-PR 系 6 本はどれも落ちない (報告と終了を競わせるテストが無い)

## やること (案)

iOS の 2 本と同型の instrumented テストを Android に 1 本足す — 進捗受け口つき VM のカスタム Loading で `report(0.5)` / `report(1.0)` を呼んで即座に action を抜け、受け口の受領列が `[0.5, 1.0]` であることを見る。

## 級 (見込み)

S (テスト追加のみ、本体挙動の変更なし)

## クローズ (2026-08-26)

**対応済みのため実装不要と確定してクローズ。** 「やること (案)」のテストは add-loading の実装内で追加済み — `android/ksdialogs/src/androidTest/kotlin/jp/kamusoft/ksdialogs/LoadingProgressTests.kt` の「報告の直後に処理が戻っても最終進捗が終了に追い越されない」(進捗受け口つき VM で report(0.5) / report(1.0) 直後に action が同期的に戻り、受領列 [0.5, 1.0] を確認。テスト内 12 反復)。ディスパッチャを `Dispatchers.Main` (immediate なし) に差し替えると確実に失敗することの実証 (ミューテーション確認) も実施済み。本起票の出典であるレビュー指摘は、同日の修正サイクル (レビュー確定後の追加対応) で先に消化されており、蒸留時の申し送りが対応済みの事実を拾えていなかったもの。
