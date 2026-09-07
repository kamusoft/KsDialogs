# 提示挙動の完成

多段表示・アニメーションフック・キーボード/回転追随の提示挙動を実装し、OS 間挙動差を共通仕様テストで記録する change フェーズ (phase-5-dialog-completion から分割)。

## 論点

(なし — 全論点解消 2026-08-21)

## 決定事項

- **多段表示の OS 間挙動差の記録は Scenario テスト方式とする** (2026-08-21): Native 2実装への同名 Scenario テストで系列挙動 (show A → show B → A を先に報告 → B の観察 等) を検証し、期待値の正は concepts/core/api/multi-display-semantics.md の差分表に置く。cases.json + approvedDiff (core/ADR-0009) は流用しない — 挙動は系列でありマトリクスでないため、同 ADR 自身の切り分け「マトリクスでない検証は少数の Scenario テストが担う」に乗る。Android 側は ADR-0009 と同じ「instrumented test のローカル実行 + 証跡記録」運用。委譲の実装と器消失検知は phase-4 で配置済み (ADR-0006 現行照合 2026-08-15) のため、実装の残作業は挙動テスト追加が本体
- **MD-d (キーボード表示中の Android 戻るボタン) は契約化しない** (2026-08-21): 「1回目はキーボードのみ閉じる」は OS の既定処理 (IME が戻るを先に消費) の産物であり、ライブラリの約束にはしない。契約化すると OS 側の変化 (predictive back・IME の戻る消費の変更) 時に明示ガード実装の義務を負うため、ADR-0006 の哲学「契約は自分が制御できる観察可能な意味論のみ」に従い委譲側とする。保証しなくても「結果はちょうど1回」は崩れない。result-notification-semantics.md の「まだ決めていないこと」から外し「OS の既定処理に委ねる (保証しない)」へ移す文書更新を ksn-propose のスコープに含める。保証しないため ADR-0016 枠の Scenario テストにも載せない
- **キーボード・回転の継承範囲を確定** (2026-08-21): (1) キーボード — 原典同様「ダイアログを動かさない・リサイズしない」。原典 KeyboardListener は移植しない (唯一の実効果「キーボード中の戻る無視」は MD-d 決定で継承しないと確定済み)。(2) 回転 — 専用コードは足さず、既存の毎レイアウトパス再計算機構 (Android DialogLayoutHost.onMeasure / iOS viewWillLayoutSubviews、属性は ADR-0015 スナップショットで凍結のままウィンドウ寸法・インセットのみ再読) による追随を仕様とし、実機裏取り + Scenario テスト (ADR-0016 枠) をこのフェーズで実施。原典 iOS の「提示後サイズ固定」は継承せず再計算方式で上書きする意図的乖離。(3) AutoRotateForIOS (回転抑止スイッチ) は移植しない — deprecated 系譜 API・用途がニッチで、主用途 (Loading の回転ロック) は phase-7 の agenda へ申し送り。必要になれば ADR-0014 の器メタ属性として非破壊追加できる
- **immersive のシステムバー表示状態はダイアログウィンドウへ引き継ぐ** (2026-08-21): DialogWindowSystemBars の宣言済み目的「ダイアログのウィンドウがシステムバーの見えを変えない」の完成として、Android 11+ 経路にホストの表示/非表示状態 (rootWindowInsets の可視判定 + hide(systemBars) + systemBarsBehavior コピー) の引き継ぎを追加する。旧経路 (systemUiVisibility 丸ごとコピー) は暗黙に引き継ぐのに新経路は appearance のみという非対称の解消でもある。原典は immersive 未対応のため意図的乖離 (ADR-0008 の枠)。検証は instrumented が困難な見込みのため実機確認 + 証跡記録。iOS 側は提示機構の継承 (childViewControllerForStatusBarHidden 系) で足りている想定を提案時に1点確認する
- **Kotlin `IosDialogGateway` のキャンセル追随を本フェーズで実装する** (2026-08-21): `suspendCancellableCoroutine` の `invokeOnCancellation` から、機械面 `showViewModel` が返す `KSDInteropDialogShowHandle` の取り消しを引き、Swift 面 (kmp/ADR-0005) と同じ「当該ダイアログだけ閉鎖・cancelled でちょうど1回確定」の意味論に乗せる。これで4形態すべてが結果通知契約 (基本ルール4) に適合する。doc コメント「互換面は取り消しの操作を持たない」の訂正も同時に行う。ADR-0005 の後続実行であり新規 ADR は不要 (根拠: archive/2026-08-19-expand-api-surface/handoff-distill.md 5・6-2)
- **アニメーションは両フック (presentation / dismissal) 対応・完了通知つき・既定はライブラリ実装のクロスフェード** (2026-08-21、オーナー決定): 片側だけの対応は非対称で使いにくいため両フックを導入する (dismissal のみ案は却下)。原典の「引数なし void + 250ms 暗黙同期」の脆さは継承せず、完了通知つき (async/suspend) とし、器は dismissal フックの完了を待ってから閉鎖する (cancelled 経路含む)。既定トランジションは OS 既定を維持せずライブラリ実装のクロスフェード (原典 250ms 相当) に置き換える — iOS の OS 既定 (crossDissolve) は演出が見えるが Android は実質無演出のため。供給はコンテンツ添付方式 (ADR-0015 の枠)。運び方は**別添付スロット DialogTransition** で確定 (2026-08-21) — DialogOptions への内蔵は却下 (ADR-0015 の値オブジェクト契約・スナップショット凍結契約にクロージャを混ぜない)。各形態の書き味は既存添付の隣に1つ増える形 (Android View = ksDialogTransition 拡張プロパティ / Compose = KsDialogAttributes の transition 引数 / MAUI = Dialog.SetTransition 添付プロパティ (code-behind 供給、IDialogTransitionAware 糖衣は propose で検討) / KMP = kmp/ADR-0002 により共有コード側 API 増なし)。宣言的 UI 系のフック形状も統一形で確定 (下記)
- **フック形状は統一形 — 常にコンテンツのプラットフォーム View (宣言的 UI ではホスト View) を受け取る + よくあるアニメはプリセット提供** (2026-08-21、オーナー決定): Compose は DialogComposeContentView (AbstractComposeView 派生)、SwiftUI は UIHostingController の view と、宣言的 UI のコンテンツも器の中では必ずプラットフォーム View に包まれている (ADR-0011 の単一系統) ため、フックは4形態 × 2技術系すべて `suspend (View) -> Unit` / `(UIView) async -> Void` の1つの型で足りる。翻訳系プロパティ (translation / alpha / transform) は描画時プロパティでレイアウト解決・スナップショット凍結と干渉しない。あわせて fade / slide 等の定番アニメは DialogTransition のプリセット (factory) として一発呼び出しできるようにし、**時間・easing を引数で指定できる**形とする (easing は形態ごとのネイティブ表現 — Android = Interpolator / iOS = UIKit タイミング指定 / MAUI = Easing — をそのまま受け、形態横断の easing 抽象は作らない。具体のプリセット一覧と引数既定値は propose で確定)。B案 (phase 通知の慣用形 — CompositionLocal / Environment でフェーズを流し中身が完了報告) は「もっと綺麗にできる余地がある」というオーナー感触つきで将来の非破壊追加候補として記録 — 添付スロットと器の退場待ち機構は共通なので後から足せる

## TODO

- [x] 論点の解消 (2026-08-21 全6論点 + 派生2論点)
- [x] ksn-propose で変更提案を起こす (2026-08-21: changes/add-presentation-behavior — 級 L、相方スペックレビュー4ラウンド APPROVED、モック承認済み)
- [x] 完了条件: パリティ準拠の Sample 通し (2026-08-22: 4ルート × 6状態の静止画照合 + 演出の実機証跡、オーナー最終承認)

## 実装結果 (2026-08-22 反映)

- 実装完了・アーカイブ済み: [add-presentation-behavior](../../../../changes/archive/2026-08-22-add-presentation-behavior/proposal.md) — レビュー 5 周 (ホスト + 相方 codex、うち 2 周はオーナー差し戻しの iOS 入場ちらつきの修正分) APPROVED・verify-001〜004 VALID (61 Scenario、未記録乖離ゼロ)・UI 最終承認 (2026-08-22)。テスト: ios 137 / android 50 + instrumented 138 (API 29 / 33 / 36) / kmp 51 / maui 62 / bridge 15、負のコンパイル検査 27 本
- ADR: core/0016 (挙動 Scenario テスト — 安定 ID `PB-xx-NN`・網羅検査 `scripts/scenario-id-coverage.py`) と core/0017 (出入りの演出 — design Decision 2・5・10 と実装の帰結を統合) を accepted 昇格。kmp/0005 の負の帰結「Kotlin 経路未追随」は解消 (現行照合 footer に記録)
- 合意済み差分 (deviation): iOS プリセット factory の `@MainActor` 化 (オーナー承認)・iOS の created / attached 中の外側タップは受け付けて演出なしで cancelled (design 遷移表は「受け付けない」。製品経路では到達不能)・iOS の内部列挙に `backPress` なし・Android の覆いの階層位置・MAUI の中身に親 (`DialogContentHost`) を与える、ほか実装メモ。design Decision 10 の iOS 暫定差分 (alpha の同期復帰) はオーナー却下 → design どおりへ修正 (録画フレームで解消確認)
- オーナー判断 (蒸留時): Sample の表示操作の表記 `Show` (Layout) / `表示` (Transition) は**画面ごとの差として許容** (sample-parity.md に明記)。MAUI iOS の調整部の無効状態が読み上げに乗らない件は**起票しない** (既知の制限として受け入れ。2026-08-22)
- 申し送りのルーティング (蒸留 2026-08-22):
  - **iOS 器の内部整理 4 件** (提示前撤去の 2 機構一本化 / 覆いフェードの同期開始 / 回帰テストの実描画化 / 器の解放順序の任意テスト) → 簡易起票 [tidy-ios-presentation-internals](../../../../changes/tidy-ios-presentation-internals/exploration.md) (S 級候補)
  - **Loading / Toast への `DialogTransition` 適用** → [phase-7 agenda](../phase-7-loading/agenda.md) / [phase-8 agenda](../phase-8-toast-rebuild/agenda.md) の論点へ追記済み
  - **Reduce Motion の尊重** (design Open Question) → [phase-9 agenda](../phase-9-docs/agenda.md) の論点へ追記済み (利用者向け案内か自動縮退かをそこで決める)
  - **AutoRotateForIOS (回転抑止)** → [phase-7 agenda](../phase-7-loading/agenda.md) に記載済み (2026-08-21 申し送り)
  - **宣言的 UI の phase 通知形フック (B案)** → 見送り。core/ADR-0017 の Alternatives に「将来の非破壊追加候補」として記録済みで、需要が出るまで受け皿を作らない
  - **`Task.immediate` への置き換え** (最低対応 OS を iOS 26 へ上げる時点) → 見送り。core/ADR-0017 の Consequences に条件つきで記録済み (OS 下限を動かす変更が起点になる)
  - **Android Sample の回転時のダイアログ維持** → 簡易起票済み [keep-sample-dialog-across-rotation](../../../../changes/fix-sample-android-back-and-rotation/exploration.md) (実装フェーズで起票。2026-09-02 に fix-sample-android-back-and-rotation へ統合)
  - **未再現の観測 (MAUI iOS `None` の退場で 1 度だけ中身が残ったコマ)** → 見送り。契約違反と断定できず再現もしないため起票しない。再現したら `None` の退出と配送順序の結線を疑う (archive の `verification/sample-walkthrough/README.md` 所見4)

