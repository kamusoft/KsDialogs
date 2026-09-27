# Exploration: fix-ios-toast-wait-host-at-launch

## 課題 / 動機

iOS ライブラリの Toast が、アプリの起動直後に show されたとき、提示先を待ったまま一度も表示されずに破棄される疑いがある。

- core の toast-semantics「提示環境の不在」行は「呼び出しは失敗せず、提示先の出現を待って表示する」と定める。起動直後の show が一度も出ないなら、この契約を満たしていない
- 観測: iOS Sample (`samples/ios/`) を `--demo default-toast` で起動すると、修正前の自動再生 (最初の画面の `.task` で、シーンが前面でアクティブになる前に show していた) で 0/5 回表示 (0.3 秒刻みで撮っても一度も写らない)。iPhone Air / iOS 26.5 の Simulator
- 仮説 (実装ワーカーとレビュアーの見立て。未実測): `ios/Sources/KsDialogs/Presentation/ToastCoordinator.swift` の提示先待ち (`startWaitingForHost`) は `UIWindow.didBecomeKeyNotification` だけを待つ。一方で提示先は「前面でアクティブなシーンの key window」に限られる (`ApplicationKeyWindowProvider.swift`)。起動直後はシーンが inactive のうちに window が先に key になり、その後シーンが active になっても key 化の通知は再び来ないため待ちが解けず、duration が満了して破棄される
- 影響: 起動直後の最初の画面で Toast を出す利用者アプリでも同じことが起きうる

発見の文脈: fix-ios-sample-demo-autoplay の実装時の修正前実測 (`kasane/changes/archive/2026-09-27-fix-ios-sample-demo-autoplay/evidence/autoplay-measurement.md`) と、同 change の review-001 の Minor 指摘。Sample 側は同 change で「シーンが前面でアクティブになってから再生」に直したため、Sample の自動再生では再現しなくなっている。

## 検討した選択肢 (却下案と理由を含む)

## 決定事項

## ADR 候補 (作成済み: なし / 未起票: なし)

## 未決の論点

**未探索 (簡易起票)**。分かっている疑問点:

- 仮説の実測での切り分け (起動直後の show で、key 化の通知・シーンのアクティブ化の順序と待ちの解除有無を観測する)。再現には、シーンがアクティブになる前に Toast を show する最小の呼び出しが要る (修正後の iOS Sample の自動再生では再現しない)
- 直し方の候補の比較 (待ちの解除条件にシーンのアクティブ化も加える、など) は未検討
- Android で同じ状況 (前面の Activity がまだ無い起動直後の show) が契約どおり待てているかの照合 (lessons/process.md L-001 の姉妹面照合)
- **Loading も同じテーマの OS 差を持つ** (fix-ios-sample-demo-autoplay の蒸留時に発見、2026-09-27 追記): 開始時点で提示先が無かった表示を、Android は提示先が現れた時点で入りの演出から表示する (`android/ksdialogs-core/src/main/kotlin/jp/kamusoft/ksdialogs/LoadingCoordinator.kt` の `onHostChanged`) が、iOS は表示しないまま終わる (`ios/Sources/KsDialogs/Presentation/LoadingCoordinator.swift` の `startDisplay`)。core の loading-semantics には現状のとおり「承認済みの差ではない」OS 差として記述済み。iOS を Android に揃えて直すか、OS 差として承認するかをこの探索で決める
- fix-ios-sample-demo-autoplay で「ライブラリの提示先不在時の振る舞いは現状のまま契約とする」と決めたのは Dialog・Loading・Toast の方針の不揃いについてであり、Toast が自身の契約 (出現を待つ) を満たせていない疑いはその判断の対象外

## UI 素材 (ui/references/ の一覧と注釈)

なし

## 変更級の推奨: 未判定
