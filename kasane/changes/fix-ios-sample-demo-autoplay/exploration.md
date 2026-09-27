# Exploration: fix-ios-sample-demo-autoplay

## 課題 / 動機

iOS Sample (`samples/ios/`) を撮影支援の起動引数 `--demo <デモID>` で起動すると、自動再生が正しく働かない。手でメニューをタップすれば正常に出る。

- `default-loading` / `custom-loading`: 処理は走って結果表示は変わるのに、Loading が一度も画面に出ない
- `basic-dialog`: `showBasicDialog` の `assertionFailure` で落ちる (提示先を取れていないと見られる)
- 変更前 (define-loading-action-thread 着手前) のビルドでも同じで、この change が原因ではない
- 観測環境: iPhone Air / iOS 26.5 の Simulator

撮影支援の起動引数は Sample の撮影手順 (`kasane/config.yaml` の `ui.screenshot`、`kasane/handbook/cross/sample-parity.md`「撮影支援の起動引数」節) の前提で、iOS で効かないと証跡の撮影がタップ頼みになる。

発見の文脈: define-loading-action-thread の tasks 5.4 / 5.5 (Sample の通しの撮影) で、自動再生では Loading が撮れず、メニューのタップで撮り直したとき。

### 原因 (コードから確定した範囲)

自動再生が走った時点で、ライブラリが提示先として探す「前面でアクティブなシーンの key window」(`ios/Sources/KsDialogs/Presentation/ApplicationKeyWindowProvider.swift` の `selectKeyWindow`) がまだ得られていない。3 機能の提示先不在時の振る舞いが症状と一致する:

| 機能 | 提示先が無いときの振る舞い | 自動再生での症状 |
|---|---|---|
| Dialog | 構成ミスとして `presentationHostUnavailable` を throw (`DialogPresenter.swift`) | Sample の `assertionFailure` で停止 |
| Loading | 器を作らず処理だけ実行し、あとで window が現れても出し直さない (`LoadingCoordinator.swift` の `startDisplay`。コメントに「提示環境の不在は構成ミスではない」) | 結果表示は変わるが Loading が出ない |
| Toast | 提示先の出現 (`UIWindow.didBecomeKeyNotification`) を待って表示 (core の toast-semantics「提示環境の不在」行) | 影響なしの見込み (未実測) |

自動再生は最初の画面の `.task` で `Task.yield()` を 1 回挟んでから再生している (`samples/ios/KsDialogsSample/SampleMenuScreen.swift`)。KMP iOS (`samples/kmp/iosApp/KsDialogsSampleKmp/SampleMenuScreen.swift`) は同じ `Task.yield()` の前に、前面アクティブのシーンの key window が得られるまで 50ms 刻み・上限 2 秒で確かめる待ちを持つ (探索時は `Task.yield()` の行だけを見て「同形」と誤認していた。実装時の実測と実物の確認で訂正)。`Task.yield()` は MainActor のターンを 1 回譲るだけで、シーンのアクティブ化もレイアウト完了も保証しない。この点は自動再生を入れた add-sample-capture-automation の review-001 で既に指摘され、当時は iPhone 17 / iOS 26.0 (2026-08-27、verify-002) で 6 アプリ × 9 デモが安定した実測を根拠にコメント修正で閉じていた。iPhone Air / iOS 26.5 でタイミング頼みが外れたと見る。

## 検討した選択肢 (却下案と理由を含む)

### 論点 1: この change の範囲

- A: Sample の自動再生だけ直し、ライブラリの提示先不在時の振る舞いは別 change として起票する — 採らず (オーナー判断)
- B: この change でライブラリ側も揃える (Dialog / Loading も提示先の出現を待つ) — 却下。core 契約の改訂で M〜L 級 (ADR 級・4 形態に波及) になり撮影の復旧が遅れる。決めること (Dialog の待ち時間・待機中のキャンセル・処理が先に走り表示が遅れて出る Loading の是非・Android で前面 Activity が無い同状況の実測) も多い
- **C: Sample の自動再生だけ直し、ライブラリの振る舞いは今のままで良しとする — 採用** (オーナー判断)

### 論点 2: Sample の待ち方

置き換え対象は現状の `Task.yield()` 1 回 (保証の無いタイミング頼み)。

- **A: アプリ (シーン) が前面でアクティブになってから再生する — 採用**。ライブラリが見る条件そのものを待つため、端末・OS の版・起動の速さに左右されない。SwiftUI の通常の書き方で、利用者向けの見本として自然
- B: 起動から一定時間 (例: 1 秒) 待ってから再生する — 却下。端末・OS 版に依存し、KMP iOS はコールド起動に 6〜9 秒かかることがあるので、どこまで待てば足りるかが端末次第。撮影のたびに固定の待ちが乗り、今回と同じ「たまたま足りていた」が再発しうる
- C: ライブラリが提示先を見つけられるまで短い間隔で確かめてから再生する — 却下。条件は正確だが、Sample がライブラリ内部の提示先の探し方 (前面アクティブのシーンの key window) を複製して持つことになり、利用者向けの見本として不自然

## 決定事項

- **範囲は Sample の自動再生だけ** (論点 1 = C)。ライブラリの提示先不在時の振る舞い (Dialog = 失敗を返す / Loading = 表示せず処理だけ実行 / Toast = 出現を待つ) は現状のまま契約とする。起動直後の最初の画面で show する利用者アプリが失敗・非表示になるのも現仕様として扱う (論点 4 もこれで決着)
- **待ち方は「アプリが前面でアクティブになってから再生」** (論点 2 = A)。プロセス起動につき 1 回だけ (one-shot) の性質は維持する。画面側で受け取るアクティブの知らせ (`scenePhase`) の到着と、ライブラリが見る条件 (前面アクティブのシーン + key window) が揃う時点のずれは、実装時に実測で確かめる
- **対象は iOS 系 Sample 3 つを実測して、崩れているものだけ直す** (論点 3)。`samples/ios`・`samples/kmp/iosApp`・`samples/maui` の iOS を iOS 26.5 の Simulator で `--demo` 起動して確かめる。KMP iOS は同じ待ち方なので直す見込みが高い。MAUI は別の待ち方 (画面表示時の `OnAppearing` からディスパッチ、`samples/maui/KsDialogs.Sample.Maui/SampleMenuPage.xaml.cs`) なので、崩れていた場合に同じ考え方 (アクティブになってから) を MAUI の手段で表す
- `samples/ios` の `Task.yield()` に付いた理由コメント (「9 デモの通し撮影で安定を確認済み」) は、新しい待ち方の理由に置き換える
- handbook (sample-parity「撮影支援の起動引数」) と `kasane/config.yaml` の `ui.screenshot` は書き換えない。「起動直後に自動再生する」という表現は待ち方を変えても成り立ち、撮影支援機構はデモ項目の一致要件の枠外 (同 handbook「例外枠」) なので、iOS 系だけ待ち方が違ってもパリティ違反にならない

## ADR 候補 (作成済み: なし / 未起票: なし)

- 論点 1 (ライブラリの振る舞いを現状維持) は既存の振る舞いを変えない判断で、別 change で覆すのも容易なため ADR にしない
- 論点 2 (待ち方) は Sample 内部の実装の選び方なので ADR にしない

## 未決の論点

- **蒸留時の concepts 追随候補**: Loading の「提示先が無いときは表示せず処理だけ実行し、あとから出し直さない」振る舞いが、core の loading-semantics にも ios の loading-surface にも書かれていない (コードのコメントにのみある)。現状維持を決めたので契約の一部として書き足す候補。Toast は toast-semantics に「提示環境の不在」の行がある
- **実装時の実測**: (1) `scenePhase` が active になった時点でライブラリの提示先が得られるか (2) KMP iOS・MAUI iOS の症状の有無 (3) 提示先を待つ設計の Toast 系デモ (`default-toast` など) が自動再生で正常か
- 「いつから効かなくなったか」は、iPhone 17 / iOS 26.0 (2026-08-27) では安定、iPhone Air / iOS 26.5 で崩れた、まで判明。端末差か OS 版差かは切り分けていないが、条件を待つ方式ならどちらにも効くため切り分けは不要とする

## UI 素材 (ui/references/ の一覧と注釈)

なし

## 変更級の推奨: S

- 触るのは Sample の撮影支援機構 (自動再生の開始時点) だけで、最大 3 ルート (iOS・KMP iOS・MAUI iOS)
- 公開 API・core 契約・ADR・handbook の変更なし。可逆
- 画面の見た目の変更なし (自動再生後の状態は手でタップした場合と同じ)
- 確認は Simulator での `--demo` 起動の実測 (Sample に自動テストは無い)
