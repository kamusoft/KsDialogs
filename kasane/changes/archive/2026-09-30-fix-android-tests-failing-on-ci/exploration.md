# Exploration: fix-android-tests-failing-on-ci

旧 change-id: fix-toast-test-harness-offthread-read (簡易起票)。0.1.0-beta.3 のリリースの事前確認で CI の赤 2 件を検出し、同じ原因の CME とモーダルの寸法の件を 1 つの change にまとめて改名した。

## 課題 / 動機

0.1.0-beta.3 のリリースの事前確認 (release-procedure 1.) で、`develop` の先端 `cfa0b73` の検証 CI (run 36687882779) が Android の 2 job で赤になった。どちらもテスト側の問題で、製品の不具合ではない。

### 1. Toast の状態をテストのスレッドから読んで CME (間欠)

- unit test: `AttachedHostRetentionTests`「描画済みの画面に載った Toast は、背面へ下がって戻っても外れない」が `ConcurrentModificationException at AttachedHostRetentionTests.kt:71` で落ちた (attempt 1)。失敗した job だけの再実行 (attempt 2) では通った
- instrumented (簡易起票時の観測): `ToastMultiDisplayTests.TS_MX_05_Loading_は起動順によらず_Toast_より前面` が間欠的に同じ例外で落ちる。スタックは `ToastCoordinator.getPresentedContainers` ← `ToastTestHarness.getContainers` ← `waitUntilPresenting`。観測環境は API 33 実機、`:ksdialogs-core` の `connectedDebugAndroidTest` 全件実行の 1 回。同じクラスを回し直すと成功した (12 件中 12 件)。発見の文脈は define-loading-action-thread の tasks 6.1 (記録は `kasane/changes/archive/2026-09-26-define-loading-action-thread/evidence/test-run-summary.md` の失敗 C)

原因の調べ (ksn-scout):

- `ToastCoordinator` (internal) の観察用プロパティ `presentedContainers` は、getter の中で内部の可変リスト `displays` を `mapNotNull` で走査してコピーを作る (`android/ksdialogs-core/src/main/kotlin/jp/kamusoft/ksdialogs/ToastCoordinator.kt` の「観察」節)。返り値はコピーだが、コピーを作る走査が UI スレッドの add / remove と重なると CME になる。`presentedContentViews` / `presentedDefaultContentViews` / `isPresenting` も同じ走査を経由する。`displayCount` は size だけなので CME にはならない
- Loading の `presentedContainer` は `@Volatile` の単一参照で CME は起きない。Dialog のテスト用 surface (`DialogTestPresentationSurface` / `RecordingDialogPresentationSurface`) は `synchronized` でコピー済みで安全
- unit test は Robolectric ではなく純 JVM。UI スレッドにあたるのは `DialogUiThreadTest` が `Dispatchers.setMain` に据える専用スレッドで、テスト本体 (`runBlocking`) とは別に回るため、instrumented と同じ競合が起こる
- 同じ読み方の箇所:
  - 集約点 `android/ksdialogs-core/src/androidTest/kotlin/jp/kamusoft/ksdialogs/support/ToastTestHarness.kt` の読み取り口 (`containers` / `contentViews` / `defaultContentViews` / `isPresenting` / `waitUntilPresenting`)。Toast の instrumented テスト十数クラスがこれを経由し、テストのスレッドから読んでいる
  - coordinator を直接読むもの: `ToastMultiDisplayTests` (`coordinator.isPresenting`)、`DrawnHostPresentationTests` (`toastCoordinator.isPresenting` と直接の読み)、unit の `AttachedHostRetentionTests` (`waitUntil { coordinator.presentedContainers.size == 1 }` と続く直接の読み)
  - waitUntil のラムダ内で harness の列を走査するもの: `ToastActivityRecreationTests`、`ToastSystemBarsTests`、`ToastSystemInputTests`、`ToastTransitionTests`
- 前例: UI スレッドで読む形は既にある — `InstrumentedStateSettling` (runOnMainSync)、`LoadingLayoutObservation.readOnMain`、`DialogSystemBarsTests.readOnMainThread` / `awaitCondition`
- handbook `kasane/handbook/cross/ci-flaky-test-policy.md` の「観測は終端状態の合意を待つ」節は待ち方の規約で、「UI スレッドが書き換える状態は UI スレッドで読む」という明文は無い

### 2. モーダルのウィンドウが CI 端末の画面からはみ出して押し戻される (毎回)

- instrumented: `DialogCurrentPageTests`「同じ_Activity_のモーダルのウィンドウに載った_View_は原点が違ってもその位置が基準になる」が、前提の assert (`DialogCurrentPageTests.kt:207`、「モーダルのウィンドウの原点が Activity のウィンドウとずれている (左端)」、Actual: 0) で落ちた。attempt 1 / 2 とも同じ失敗
- CI の端末 (API 36 google_apis x86_64、hardware profile 指定なし) の画面は既定の 320x640 / 160dpi、つまり幅 320dp (ログ `Setting display: 0 configuration to: 320x640, dpi: 160x160`)
- テストのモーダルは左 32dp・幅 320dp (`DialogCurrentPageTests.kt` の `MODAL_LEFT_DP` / `MODAL_WIDTH_DP`) で 352dp となり画面からはみ出す。ウィンドウ管理がはみ出したウィンドウを画面内へ押し戻すため左端が 0 になった。縦は 120 + 400 = 520dp で 640dp に収まるので上端のずれは保たれている (症状と一致)
- 同じ補助 `SameActivityModalWindow` を使う `DialogCurrentPageLedgerTests` は 40 + 240 = 280dp で収まり、CI でも通っている
- テストは add-page-layout-area (`c4b8c6b`) で入り、CI で走ったのは今回が初めて。手元の端末は画面が広く顕在化しなかった

## 検討した選択肢 (却下案と理由を含む)

### CME の直す場所

- **(a) テストの側で UI スレッドの上で読む形に揃える (採用)** — 製品のホットパスにテストのためだけの同期を入れずに済み、既存の `readOnMain` 系の前例と一貫する。読み口が `ToastTestHarness` に集約されているので、instrumented の大半は呼び出し側を変えずに済む
- (b) 製品の側で読み取りを排他にする (却下) — `presentedContainers` は既にコピーを返しており「スナップショットにする」だけでは直らない。`displays` の全変更箇所を lock で囲むか `CopyOnWriteArrayList` 等へ替える必要があり、テストのために製品へ同期を入れることになる。Container の状態 (`containerState`) が volatile でない同種の可視性の問題は残り、Loading / Dialog とも形が揃わない。なお `ToastCoordinator` は internal なので公開 API には触れない (却下理由は影響範囲と一貫性であって契約ではない)

### モーダルの寸法

- **A. モーダルの寸法を端末の画面の大きさから決める (採用)** — 影響はこのテストと補助だけで、画面の広さに依らず収まる
- B. 幅の固定値を小さくする (例: 240dp) (却下) — 320dp より狭い端末では再発する
- C. CI の端末を大きな画面の hardware profile にする (却下) — instrumented 全件の所要時間・安定性に波及し、「手元では通り CI では落ちる」寸法依存が別の形で残る

## 決定事項

- CME は (a): UI スレッドが書き換える Toast の状態を、テストは UI スレッドの上で読む
  - instrumented: `ToastTestHarness` の読み取り口 (`containers` / `contentViews` / `defaultContentViews` / `isPresenting`、必要なら `displayCount`) を UI スレッドで読む形にする。`waitUntilPresenting` のポーリングも各回 UI スレッドで読む
  - coordinator を直接読んでいる `ToastMultiDisplayTests` / `DrawnHostPresentationTests` は harness 経由か UI スレッドでの読みに直す
  - unit: `AttachedHostRetentionTests` の読みを `Dispatchers.Main` (テスト用 UI スレッド) の上で行う。待ち合わせの補助 (`DialogTestWaiting`) に UI スレッドで条件を読む形を足すかは実装で判断する
  - 製品コード (`ToastCoordinator` 等) は変更しない
- モーダルの寸法は A: `DialogCurrentPageTests` のモーダルの幅・高さを、端末の画面の大きさ (dp) からずらし分を引いて決め、左端・上端のずらしが必ず保たれるようにする (前提の assert はそのまま残す)
- 検証: 修正後、`develop` へ push して検証 CI (特に `android / verify` と `android-instrumented / verify`) が緑になることを確かめる。CME は間欠なので、手元では該当テストの繰り返し実行で再発しないことも見る
- 蒸留時に反映: `kasane/handbook/cross/ci-flaky-test-policy.md` — 「UI スレッドが書き換える状態は UI スレッドで読む (テストのスレッドから製品の可変コレクションを走査しない)」の規約を足す
- 蒸留時に反映: テストが作る画面上の状況 (ウィンドウの位置・大きさ) は CI 端末の画面 (320x640dp) に収まる寸法にするか画面から決める、を同じ handbook か test-execution.md に足すかを判断する

## ADR 候補 (作成済み: なし / 未起票: なし)

いずれもテストコード内の方針で、覆すコストが低く境界も越えないため ADR にはしない。

## 未決の論点

- unit の待ち合わせ補助に「UI スレッドで条件を読む」版を足すか、呼び出し側で `withContext(Dispatchers.Main)` を挟むか (実装で判断)
- Container の `containerState` を別スレッドから読んでいる箇所の可視性 (volatile でない) は CME とは別問題。今回は扱わない (顕在化したら別 change)

## UI 素材 (ui/references/ の一覧と注釈)

なし

## 変更級の推奨: S

テストコード (テスト補助とテスト数本) だけの修正で、製品コード・公開 API・利用者向けの挙動に触れない。可逆。
