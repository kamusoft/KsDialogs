# Exploration: fix-android-instrumented-toast-ime-hide-flake

## 課題 / 動機

Android の instrumented テスト `android/ksdialogs/src/androidTest/kotlin/jp/kamusoft/ksdialogs/ToastSystemInputTests.kt` の「Toast 表示中でも IME を出し入れできる」が、検証 CI (`android-instrumented / verify`、API 36 の Emulator 1 台、`-no-window`) で間欠的に失敗する。目的は再実行で通す運用ではなく、**失敗の原因を特定して根本的に直す**こと。

発見の文脈: add-verification-ci の蒸留 commit (3cc036a、コメント変更のみでコードは無変更) の run 34187175131 attempt 1。同 job の実行 5 回のうち失敗は 1 回 (2d77ef1 / 1a96389 / 29b8973 / 同 run の attempt 2 は成功)。

失敗の形:

- 落ちた assertion は `ToastSystemInputTests.kt:64` の「Toast 表示中に IME が引っ込まない」。Toast を表示した状態で `InputMethodManager.hideSoftInputFromWindow` を呼んだあと、`rootWindowInsets.isVisible(ime())` が false になるのを 8 秒 (`IME_TIMEOUT_MILLIS`) 待って時間切れ
- 同じテスト内の直前の段 (Toast 表示前の IME の出し入れ・Toast 表示中の IME 表示) は通っている。つまり「Toast 表示中に IME を引っ込める」段だけが落ちた
- 他の 293 件は成功 (1 skipped)。同クラスの「戻るとホームが通る」は成功
- 待ち時間 8 秒に対して時間切れなので、遅延ではなく IME が引っ込む経路そのものが動かなかった疑いがある (Emulator 側の IME 状態、`hideSoftInputFromWindow` に渡す window token・フォーカスの取り違え、insets の更新が届かない、のいずれか)

関連:

- `kasane/changes/archive/2026-09-06-fix-android-instrumented-toast-back-loading-coalescing/` — 同クラスの「戻るとホームが通る」が API 36 で落ちた件の修正 (予測型バックへの追随)。review-001 Suggestion に「待ちの時間切れは素の AssertionError しか出ず逆引きが要る」の指摘
- `kasane/changes/archive/2026-08-28-add-toast/evidence/toast-system-input-android.md` — このテストの初回実測 (実機)
- `kasane/handbook/cross/verification-ci.md` — CI の instrumented job の範囲と実行条件
- `kasane/lessons/inbox/ci-runner-parallel-suites-starve-main-actor.md` — CI ランナーの容量差で iOS テストが落ちた件 (別原因だが、CI だけで落ちる型の先例)

## 検討した選択肢 (却下案と理由を含む)

## 決定事項

## ADR 候補 (作成済み: なし / 未起票: なし)

## 未決の論点

**未探索 (簡易起票)**。分かっている疑問点:

- 失敗時に IME がどの状態だったか (insets は true のまま / null / window token が別) を切り分ける観測が無い。次に落ちたときに読める形 (時間切れ時のメッセージに insets の実測値と `hasFocus` を含める) を先に入れるか
- `hideSoftInputFromWindow(inputField.windowToken, 0)` は Toast の器 (フォーカスを奪わない透過ウィンドウ) が上に載った状態で、入力欄のウィンドウが IME のターゲットのままかに依存する。`WindowInsetsController.hide(ime())` (API 30+) で引っ込める経路に変えれば器の有無に左右されないか
- `-no-window` の Emulator 固有か。手元 (ウィンドウあり・実機) で再現するかの実測が要る。CI では 5 回中 1 回
- 再現率が低いため、修正の検出力は「修正前に fail することの確認」ではなく、失敗経路を一時的に強制する形 (`kasane/lessons/inbox/forced-failure-confirms-mechanism-when-repro-fails.md`) で担保することになりそう

## UI 素材 (ui/references/ の一覧と注釈)

なし

## 変更級の推奨: 未判定 (暫定 S — テストと観測方法の局所修正で収まる見込み。本体の器に手を入れる必要が出たら M)
