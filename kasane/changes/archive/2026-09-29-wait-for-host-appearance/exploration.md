# Exploration: wait-for-host-appearance

旧 change-id: `fix-ios-toast-wait-host-at-launch` (2026-09-27 に改名。簡易起票時は iOS の Toast の起動直後の修正だったが、探索で Dialog・Loading・Toast の提示先の出現待ちを全形態で揃える変更に広がったため)

## 課題 / 動機

iOS ライブラリの Toast が、アプリの起動直後に show されたとき、提示先を待ったまま一度も表示されずに破棄される疑いがある。

- core の toast-semantics「提示環境の不在」行は「呼び出しは失敗せず、提示先の出現を待って表示する」と定める。起動直後の show が一度も出ないなら、この契約を満たしていない
- 観測: iOS Sample (`samples/ios/`) を `--demo default-toast` で起動すると、修正前の自動再生 (最初の画面の `.task` で、シーンが前面でアクティブになる前に show していた) で 0/5 回表示 (0.3 秒刻みで撮っても一度も写らない)。iPhone Air / iOS 26.5 の Simulator
- 仮説 (実装ワーカーとレビュアーの見立て。未実測): `ios/Sources/KsDialogs/Presentation/ToastCoordinator.swift` の提示先待ち (`startWaitingForHost`) は `UIWindow.didBecomeKeyNotification` だけを待つ。一方で提示先は「前面でアクティブなシーンの key window」に限られる (`ApplicationKeyWindowProvider.swift`)。起動直後はシーンが inactive のうちに window が先に key になり、その後シーンが active になっても key 化の通知は再び来ないため待ちが解けず、duration が満了して破棄される
- 影響: 起動直後の最初の画面で Toast を出す利用者アプリでも同じことが起きうる

発見の文脈: fix-ios-sample-demo-autoplay の実装時の修正前実測 (`kasane/changes/archive/2026-09-27-fix-ios-sample-demo-autoplay/evidence/autoplay-measurement.md`) と、同 change の review-001 の Minor 指摘。Sample 側は同 change で「シーンが前面でアクティブになってから再生」に直したため、Sample の自動再生では再現しなくなっている。

## 検討した選択肢 (却下案と理由を含む)

### 提示先が無いまま始まった Loading を iOS でどうするか (2026-09-27)

| 軸 | A: iOS も後から出す (この change で) | B: iOS は出さないのを OS 差として承認 | C: Toast だけ直し、Loading は別 change へ |
|---|---|---|---|
| 起動直後の Loading の見え方 | 提示先が現れた時点でまだ処理中なら出る。終わっていれば何も出ない (Android と同じ) | 処理は走るが一度も出ない | 当面は今のまま (出ない) |
| 形態間の揃い (KMP・MAUI) | 揃う。OS 差の記述が消える | 差が契約として残る | しばらく未承認の差のまま |
| 変更の重さ | M 級 (core の Loading 契約の改訂 + iOS の 2 機能) | S 級 (Toast の修正 + 文書の書き換え) | この change は S 級、別にもう 1 本 |
| 待ちの合図の作り | Toast と共用できて、同じずれを作り込まない | Toast 単独 | Loading を直すときに作り直すか、共用前提で先に作っておく |
| 気になる点 | 処理の終わり際に提示先が現れると Loading が一瞬だけ出る (Android では既にそう) | 承認の理由が「実装がそうだから」になる | 同じテーマをもう一度開く |

- **A を採用** (オーナー判断)
- B は却下: 差は OS の制約ではなく iOS の実装が待っていないだけで、承認の根拠が無い。KMP・MAUI の共有コードの同じ呼び出しで OS によって出たり出なかったりする
- C は却下: 同じテーマをもう一度開くことになる。待ちの合図を Toast と Loading で別々に作ると、今回の Toast のずれ (合図と提示先の条件の不一致) を Loading で繰り返す余地が残る

### 待ちの合図の作り (2026-09-27)

| 軸 | A: 条件は変えず、両方の変化で見直す | B: 非アクティブでも前面のシーンなら提示先にする | C: 待っている間、短い間隔で確かめ続ける |
|---|---|---|---|
| 表示されるタイミング | シーンがアクティブになった瞬間 | アクティブになる前から出せる | 次の確認のとき (少し遅れうる) |
| 提示先の定義への影響 | なし | 変わる。Dialog も同じ規則を使うので Dialog の提示先も変わる。iPad の複数ウィンドウで操作していない側に出うる | なし |
| 待っている間のコスト | 通知 2 種の購読 | なし | 確認が走り続ける。Loading は期限なしで待つので長く走りうる |
| 取りこぼしの余地 | 提示先の条件が増えたら合図も足す必要 (同じ場所に置いて気づきやすくする) | 減る | 無いが、ずれを直さず覆い隠す |

- **A を採用** (オーナー判断)
- B は却下: 提示先を選ぶ規則 (`ios/Sources/KsDialogs/Presentation/ApplicationKeyWindowProvider.swift`) の「非アクティブのシーンに出すと利用者から見えない場所に出る」という理由を覆し、Toast と Loading だけの話で済まなくなる
- C は却下: 待っている間ずっと確認が走る。合図と条件のずれを直さず覆い隠す

### Dialog も対象にするか (2026-09-27)

オーナーの問い「起動直後に提示先が無いときに出すことは一般用途ではほぼない。それでも直すべきか」への整理:

- 「画面が出てから動かす」書き方 (最初の画面の表示時の処理、SwiftUI の `.task`) が、iOS ではシーンがアクティブになる前に走る。前回の実測 (`kasane/changes/archive/2026-09-27-fix-ios-sample-demo-autoplay/evidence/autoplay-measurement.md`) で、起動直後の画面は「非アクティブ」で始まり、あとから「アクティブ」が届くこと、その書き方で Loading 2/3 回・Toast 5/5 回が出ず、Dialog は 3/3 回落ちたことが記録されている
- Loading・Toast はエラーにならず黙って消えるので、利用者が気づきにくい。Toast は契約 (出現を待つ) に反している
- 直さない場合の代わりは「iOS ではアクティブになってから呼ぶ」の案内で、Android・MAUI iOS では要らない注意を利用者が踏んでから知ることになる

これを受けて、オーナーが **Dialog も今回対応する** と判断した。

## 決定事項

- **提示先が無いまま始まった Loading は、iOS も Android に揃え、提示先が現れた時点で表示中なら入りの演出から表示する。この change で Toast の待ちの修正と一緒に行う** (2026-09-27、オーナー判断。core/ADR-0039 proposed)
- Dialog の提示先不在時の振る舞い (失敗を返す) は両 OS で揃っているので、この change の対象外
- 仮説の実測は、実装の最初の手順 (直す前に再現を撮る) に回す。条件が 2 つの状態 (window が key であること・シーンが前面でアクティブであること) で決まる以上、両方の変化で見直すしかなく、順序の細部で直し方の方向は変わらないため

- **待ちの合図は、提示先を選ぶ規則の隣に置き、window が key になったときとシーンが前面でアクティブになったときの両方で見直す。Toast と Loading (と Dialog) で共用する** (2026-09-27、オーナー判断)
- **Dialog も、この change で提示先の出現を待つ対象にする** (2026-09-27、オーナー判断)。Dialog の今の振る舞い (提示先が無ければ失敗) は両 OS で揃っているため、iOS だけでなく全形態の契約の改訂になる。詳細は未決の論点 5〜

- **Dialog の待ちに上限を設けない。提示先が現れるまで待ち、待ちを終わらせるのは呼び出し元の打ち切り** (2026-09-27、オーナー判断。論点 5)
  - 検討した案: A 上限なし (採用) / B 決まった時間だけ待ち過ぎたら失敗 (起動直後は一瞬・背面復帰は無制限で値の根拠を置きにくい。Dialog だけ時間の規則) / C 呼び出し元が時間を指定 (全形態の show に引数が増える) / D 前面にいる間だけ待ち背面なら失敗 (背面から戻っても失敗が残る。前面の定義を OS ごとに決める必要)
  - A の弱点: 画面が現れない場所から呼ぶと返らないまま待つ。長い背面のあとに古い Dialog が出る。どちらも呼び出し元の打ち切りで防ぐが、MAUI には手段が無い (論点 6)

- **MAUI の Dialog の show に呼び出し元の打ち切りを足す。この change で行う** (2026-09-27、オーナー判断。論点 6。maui/ADR-0006 proposed)
  - 検討した案: A この change で足す (採用) / B 止められないまま差を契約に残す (Dialog の待ちに上限が無い以上、MAUI でだけ止める手段が無い) / C この change は B とし別 change で足す (推奨していたが、オーナーが A を選択)
  - iOS・Android・KMP は既存の打ち切りをそのまま使う。待っている間に打ち切られた Dialog は一度も表示されず、結果通知の「呼び出し元の打ち切り」ルールどおりに終わる
  - 打ち切られたことの見え方 (結果か例外か) と引数の形は、C# の慣習と既存の契約の表に照らして設計 (ksn-propose) で決める
- **待っている Dialog が複数あるときは、呼んだ順に出す (後から呼んだものが手前)** (2026-09-27。論点 7。多段表示の契約「後から出したものが手前」「各 show は独立に結果を返す」から決まるので判断依頼にしなかった)
  - core/ADR-0006 (多段表示は OS へ委譲し、ライブラリはスタックを管理しない) との関係: 待っている Dialog はまだ OS に渡していない「提示前の待ち」で、提示後の重なりは OS に委譲したまま。「今何段出ているか」も持たず公開もしないので、決定とは衝突しない。ただし帰結の「ライブラリが状態を持たない」は、提示前の待ちの間だけ当てはまらなくなる (core/ADR-0039 の負の帰結に記載済み)

- **iOS・Android の「提示先が無い」エラー型 (iOS `DialogError.presentationHostUnavailable`、Android `DialogException.PresentationHostUnavailable`) は、ベータのうちに削除する** (2026-09-27、オーナー判断。論点 8)
  - 検討した案: A 非推奨の印を付けて残し次の破壊的変更で消す (推奨していた。警告で気づけてビルドは壊さない) / B そのまま残し文書だけ直す (出ない型が残り、catch が死んだまま気づけない) / C 今消す (採用。catch している利用者のビルドは通らなくなるが、公開 API がすぐに整う)
  - KMP は型を持たず文言だけで区別しているので、文言が出なくなるだけ。KMP 向けの Swift の型付き入口 (`KsDialogsKmpError`) は、もともとこの失敗を判別に含めていない
  - MAUI の `DialogException.PresentationHostUnavailable` は残す。意味を「この環境には提示の仕組みが無い」に狭め、素の .NET (`HostlessDialogGateway`) の失敗にだけ使う
  - MAUI のブリッジ層の公開物 (`MauiDialogClosureKind.PresentationHostUnavailable`・Android ブリッジの `onPresentationHostUnavailable`) の扱いは設計で決める
- **Native を持たない MAUI の素の .NET は、今までどおりその場で失敗する** (2026-09-27。提示の仕組みそのものが無く、待っても提示先が現れないため。core/ADR-0037 の素の .NET の扱いと同じ考え方。core/ADR-0039 の「範囲に含めないもの」に記載)

## ADR 候補 (作成済み: core/ADR-0039・maui/ADR-0006 (いずれも proposed) / 未起票: なし)

- core/ADR-0039: 提示先が無いまま呼ばれた Dialog・Loading・Toast は、全形態で失敗せず提示先の出現を待ち、待ちの上限は各機能の寿命に任せる (Dialog を対象に加えた時点で、Loading 単独の決定から書き直した)
- maui/ADR-0006: MAUI の Dialog の show に呼び出し元の打ち切りを足し、待っている Dialog と表示中の Dialog をコードから止められるようにする

## 未決の論点

探索は 2026-09-27 に方向まで決着。以下は実装・提案で扱う残りの確認事項。

- **仮説の実測** (実装の最初の手順へ回す。直す前に再現を撮る): 起動直後の show で、key 化の通知・シーンのアクティブ化の順序と待ちの解除の有無を観測する。再現には、シーンがアクティブになる前に Toast を show する最小の呼び出しが要る (修正後の iOS Sample の自動再生では再現しない)。既存テスト (`ios/Tests/KsDialogsTests/ToastContractTests.swift` の「取り付け先が無い間は表示を保留し、現れたら表示する」) は key 化の通知を手で送っており、実アプリの順序は検査できていない
- **影響範囲の見立て (未実測)**: 起動直後だけでなく、シーンが一時的に非アクティブになる場面 (コントロールセンター・システムの許可ダイアログ・背面からの復帰) の最中の show も、同じ理由で出ないまま期限切れになる可能性がある。実測の対象に含める。Dialog・Loading も同じ場面で観測する
- **Android との照合** (lessons/process.md L-001 の姉妹面照合): コード読解では、Android は待ちの合図 (resumed な Activity の入れ替わり、`ResumedActivityTracker`) と提示先の条件 (resumed な Activity) が同じもので、起動直後の show も構造上は待てている。Android の Dialog は今回変わるので、実機での確認は実装の検証に含める
- fix-ios-sample-demo-autoplay で「ライブラリの提示先不在時の振る舞いは現状のまま契約とする」と決めたのは Dialog・Loading・Toast の方針の不揃いについてであり、Toast が自身の契約 (出現を待つ) を満たせていない疑いはその判断の対象外。Dialog・Loading の方針は今回の探索で改めて決めた (上記決定事項)

### Dialog の現状 (2026-09-27 調査、ksn-scout)

- 全形態で、提示先が無ければ構成エラーとして即座に失敗する (iOS `DialogError.presentationHostUnavailable` / Android・MAUI `DialogException.PresentationHostUnavailable` / KMP は単一の `DialogException` で文言だけで区別)。型指定 show の VM factory と configure は、提示先の判定より前に走る
  - iOS: `ios/Sources/KsDialogs/Presentation/DialogPresenter.swift` の `canPresent` の判定。Android: `android/ksdialogs-core/src/main/kotlin/jp/kamusoft/ksdialogs/DialogPresenter.kt` と `ActivityDialogPresentationSurface.kt`
  - KMP は Native の判定をそのまま通す。MAUI は Native の判定に加えて**自前の判定** (MauiContext の解決。Android は MAUI の `ActivityStateManager`) が先に走る (`maui/KsDialogs.Maui/Platforms/*/PlatformDialogGateway.cs`)。Native を持たない素の .NET は `HostlessDialogGateway` が即座に失敗する
- 契約の記述: core は `kasane/concepts/core/api/result-notification-semantics.md` のルール 5「出す先の画面が存在しない場合は cancelled を返さず失敗する」だけ。「即座に・キューイングしない」はコードの doc コメントにのみある。**この振る舞いを決めた ADR は無い**
- キャンセル: iOS (Task キャンセル → `.cancelled`)・Android / KMP (コルーチンのキャンセル) は呼び出し元の打ち切りを受け付ける (result-notification-semantics のルール 4(b))。**MAUI は `CancellationToken` を持たず、打ち切れない**
- 多段表示 (core/ADR-0006、multi-display-semantics): ライブラリはスタックを管理せず OS に任せ、「後から出したものが手前」「枚数は数えない」を保証する。待っている Dialog を溜めると、出現時の順番をライブラリが決めることになる
- Android の Dialog の提示面は、Loading・Toast が使う resumed な Activity の入れ替わり購読をまだ使っていないが、同じモジュールの仕組みなので配線すれば使える
- テスト: 提示先不在の即失敗を検査するテストが全形態にあり、KMP の一部 (DM-KM-01〜03・PB-KT-09・MB-KM-02) と MAUI の一部は「即失敗」を factory 解決の成否の判定材料に使っている。待つ方式では返らなくなるので作り直しが要る
- 公開状況: `0.1.0-beta.1` (2026-09-10)・`0.1.0-beta.2` (2026-09-13) を公開済み。ベータ利用者から見ると振る舞いの変更になる

### Dialog の論点 (すべて決定済み)

- ~~論点 5: 待ちに上限を設けるか~~ → 決定済み (上限なし。決定事項を参照)
- ~~論点 6: 待っている間の打ち切り~~ → 決定済み (MAUI に打ち切りを足す。決定事項を参照)
- ~~論点 7: 待っている Dialog が複数あるときの出る順番~~ → 決定済み (呼んだ順。決定事項を参照)
- ~~論点 8: 提示先不在の失敗 (公開の型) の扱い~~ → 決定済み (iOS・Android の型は削除、MAUI の型は素の .NET 用に残す。決定事項を参照)

## UI 素材 (ui/references/ の一覧と注釈)

なし

## 変更級の推奨: L

判定材料 (ksn-core の S/M/L 判定):

- 複数能力の横断: Dialog・Loading・Toast の 3 機能の、提示先が無いときの契約を改訂する (core の結果通知・Loading・Toast の各契約。多段表示の契約とも接する)
- 公開 API の変更: iOS・Android の「提示先が無い」エラー型の削除 (破壊的)、MAUI の Dialog の show 全入口 (7 本) への打ち切りの引数の追加
- 覆すコストが高い: 公開済みベータ (`0.1.0-beta.1` / `beta.2`) の振る舞いが全形態で変わる。ADR 2 件 (core/ADR-0039・maui/ADR-0006)
- 実装の範囲: iOS の提示先の待ちの合図 (3 機能で共用)、iOS の Toast・Loading・Dialog、Android の Dialog、MAUI の自前の提示先判定と打ち切りのブリッジ中継、全形態の「即失敗」を前提にしたテストの作り直し (KMP の DM-KM-01〜03・PB-KT-09・MB-KM-02、MAUI の一部を含む)
- 1 変更に収める理由: 3 機能の待ちは同じ合図を共用し、ずれを作らないことがこの change の要点なので、機能ごとに分けると合図を二度作ることになる

## ほかの change との関係

- **add-viewmodel-less-dialog-show** (ViewModel なしで Dialog を出す新しい入口を足す): どちらが先に入っても、後から入る側が合わせる必要がある。新しい入口も提示先の出現を待ち、MAUI の新しい入口にも打ち切りの引数が要る。「提示先が無い」エラーを前提にした記述も持ち込まない
- **tidy-ios-presentation-internals** (iOS の Dialog の器の内部整理): iOS の Dialog の器 (`DialogContainerViewController.swift`) に触れる可能性があり、作業が重なりうる。契約上の衝突は無い
- **fix-toast-test-harness-offthread-read** (Android の Toast のテスト補助): Android の Toast のテストに触れる場合に同じファイルを触りうる。この change は Android の Toast の実装を変えない見込み
