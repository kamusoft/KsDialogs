# Exploration: fix-android-instrumented-toast-ime-hide-flake

## 課題 / 動機

Android の instrumented テスト `android/ksdialogs/src/androidTest/kotlin/jp/kamusoft/ksdialogs/ToastSystemInputTests.kt` の「Toast 表示中でも IME を出し入れできる」が、検証 CI (`android-instrumented / verify`、API 36 の Emulator 1 台、`-no-window`) で間欠的に失敗する。目的は再実行で通す運用ではなく、**失敗の原因を特定して根本的に直す**こと。

発見の文脈: add-verification-ci の蒸留 commit (3cc036a、コメント変更のみでコードは無変更) の run 34187175131 attempt 1。同 job の実行 5 回のうち失敗は 1 回 (2d77ef1 / 1a96389 / 29b8973 / 同 run の attempt 2 は成功)。

頻度の追加実測 (2026-09-08、直近 12 run の android-instrumented job): 失敗 2 回 (3cc036a attempt 1・6519d94 attempt 1)、成功 10 回。どちらも同じ assertion (行 64)。CI ログに logcat は無く、失敗時の IME 状態は読めない。

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

### 原因仮説 (2026-09-08 時点の確からしさ順。出典: advisor (counterpart / codex) の評価をメインが照合)

| 仮説 | 評価 | 根拠 |
|---|---|---|
| H2: client の要求可視性と server の IME 状態のレース (show 直後の hide が pending / キャンセル分岐に入る) | 部分支持。取りこぼしの具体経路は未確定 | insets の可視観測は show の全処理完了を証明しない。API 36 の InsetsController には「要求上は既に hidden」なら hide をキャンセルする分岐 (`PHASE_CLIENT_ALREADY_HIDDEN`) がある |
| H1: IMM の served view / window token 照合に外れて hide が配送されない | 経路の存在は支持。Toast が誘発する説明は未成立 | `inputField.hasFocus()` はウィンドウフォーカスや IMM の served 状態の証明にならない |
| H3: Toast と無関係の Emulator / IME プロセス側の停滞 | 判定不能 | suite で IME を触るのはこのテストだけで、CI 履歴からは区別できない |
| H4: Toast の器が IME control target を奪う | 否定寄り | FLAG_NOT_FOCUSABLE 単独は `canBeImeTarget()` の候補から外れる (AOSP android-16.0.0_r1 の WindowState / DisplayContent で確認)。追加・撤去時の再計算までは否定できない |

### 切り分けの手段

- **hideSoftInputFromWindow の戻り値を assert する (却下)**: API 36 では `refactorInsetsController` 有効時に served view 照合後 InsetsController へ合流し、targetSdk 36 の互換変更では照合で止めても true を返す。H1 の検出器にならない (出典: advisor、AOSP InputMethodManager)
- **観測を足して CI の次の失敗を待つ (D 案)**: 頻度 2/12 なので数日で当たるが、その場で絞れない。advisor は「観測を足した現行テストを CI と同条件で集中反復」を推奨
- **観測の設計** (advisor 案をメインが採用): 単一の最終値ではなく単調時計付きの状態変化履歴を残す — show / hide の呼出時刻と戻り値、EditText の attached / view focus / window focus、View と decor の window token、`OnApplyWindowInsetsListener` での IME visible と animation の start / end、器の attach / 状態遷移 / dismiss 時刻と期限の残り。失敗時だけ `dumpsys input_method` / `dumpsys window` と直前の logcat (ImeTracker / InsetsController / InputMethodManager(Service) / WindowManager / IME プロセス) を取る。`IMM.isActive(view)` は内部で `checkFocus()` を呼び現象を変え得るので hide 前には入れない
- **再現実験の設計**: Toast の自然消滅を実験から外す (表示時間を上限より十分長くし後始末で撤去。待ち中に器が消えたら別の失敗にする)。同じ二往復 (show→hide→show→hide) を Toast なし / ありで交互に各 30 回程度。BACK / HOME テストとの前後順も明示する。失敗を成功扱いするリトライはしない

### 再現実験の実測 (2026-09-08、手元の headless Emulator)

環境: API 36 google_apis **arm64-v8a** (CI は x86_64。ホストが arm64 Mac のため揃えられない)、`sdk_gphone64_arm64` BE2A.250530.026.F3、Gboard 15.1.08、Emulator 35.6.11、`-no-window -gpu swiftshader_indirect -noaudio -no-boot-anim -no-snapshot`、animations 3 種 = 0。一時テスト (コミットしない) `ToastImeRepetitionProbe` を `am instrument` で直接回し、logcat (ImeTracker / InsetsController) と失敗時の dumpsys を回収した。

| 実験 | 内容 | 結果 |
|---|---|---|
| 反復 | 元テストと同じ順 (show→hide→[Toast]→show→hide) を Toast なし / ありで交互に 30 組 | 引っ込まない 0/60。初回 (ブート後の Gboard 初期化中) だけ「出ない」1 件 |
| suite | ksdialogs-core 全 294 件を 3 周 | Toast IME テスト 3/3 成功 (1 周目に Loading の別テスト 2 件が落ちた。LD_WN_01 / LD_AT_03、2 周目以降なし) |
| 密着 | show の直後 (0 / 30 / 100 ms) に hide | 0/30。server が pending の show を `PHASE_WM_ABORT_SHOW_IME_POST_LAYOUT` で中止して正しく非表示になる |
| churn | hide の直後に Toast のウィンドウを追加 / 撤去 (control の再配布を hide 中に起こす)。通常 + CPU 負荷 (busy loop ×4) | 0/60 |
| block | show → メインスレッドを 300 ms 塞ぐ → hide | 0/20 (client が show を server に報告する前に hide が出るため狙った順序にならず) |
| block-2 | show → 16 / 40 / 80 ms 待つ → メインを 300 ms 塞ぐ → hide (server の完了通知が hide の後ろに並ぶ順序を実現) | 0/24。hide の後に処理された `CONTROLS_CHANGED` は `PHASE_CLIENT_ON_CONTROLS_CHANGED` で取り消され、非表示が保たれる |

時系列から分かった機構 (いずれも logcat の `ImeTracker` で読める):

- **server 起源の IME 要求と自分の要求は交差する**。Activity 起動時の `HIDE_UNSPECIFIED_WINDOW` が非同期に届き、show の直後に届くと show を潰す (初回の「出ない」の実体)
- **Toast のウィンドウ追加は insets control の再配布を起こす**。取り付けの約 10 ms 後に Activity 宛てに `CONTROLS_CHANGED` の show 要求が届く (要求可視性が非表示なら `PHASE_CLIENT_ON_CONTROLS_CHANGED` で取り消され無害)。「Toast は IME に無関係」ではない
- suite 1 周目の本物のテストでは、最初の show が `PHASE_WM_POST_LAYOUT_NOTIFY_CONTROLS_CHANGED` で server に取り消され、server 起源の show に置き換わってから hide が走った (それでも通る)
- `hideSoftInputFromWindow` の戻り値は全観測で true (advisor の指摘どおり判別に使えない)
- 手元では自然再現 (60 反復 + suite 3 周) も強制再現 (5 種) も得られなかった。client 側の交差処理は堅牢と判断でき、残る差は CI 環境 (x86_64 + swiftshader の遅さ・IME プロセスの状態) にある
- AOSP 16 (`InsetsSourceConsumer.setControl`): hide 中に新しい control が届いても要求可視性が非表示なら hideTypes に振られる。`ImeInsetsSourceConsumer.requestHide`: show アニメーション中の hide は `mHasPendingRequest` として保留される

### 修正の向き (未決)

実測後の候補 (2026-09-08):

- (A) 観測の設置 + 交差型への耐性 (S 級・テストと CI workflow のみ): CI の instrumented job で logcat (`ImeTracker` / `InsetsController` / `InputMethodManager*`) を常時採取し失敗時に成果物として残す。テスト側は (1) Toast の表示時間を IME 待ちの上限から独立させ後始末で撤去、(2) hide の前に「server が IME の枠を報告した状態」(`getInsets(ime()).bottom > 0`) を待って要求可視性だけで先へ進まない、(3) 時間切れのメッセージに状態履歴を出す。根本原因の確定ではなく「次の失敗を読める化 + 既知の交差型の回避」と明記する
- (B) CI 上で再現実験: 一時的な workflow_dispatch job で probe (反復 60 × 数 run) と logcat を x86_64 Emulator で回し、`ImeTracker` の失敗署名を取る。原因確定への最短だが CI 分を消費し、一時 workflow の後始末が要る
- (C) B → A の順で両方。B の署名が取れれば A の (2) を署名に合わせた待ち条件に置き換える


- (A) テストの引っ込め方を `WindowInsetsController.hide(ime())` に変える: ウィンドウを明示できる利点。ただし API 36 では IMM の hide も同じ InsetsController に合流する経路があり、H2 / H3 なら解決しない。IMM 互換経路の検証範囲も減る
- (B) 待ち方を変える (1 フレーム待つ・`WindowInsetsAnimation.Callback.onEnd` を待つ): 診断用の時間差実験には有用だが、別プロセスの描画完了を保証しない。show 中の hide は一般利用でも起こるため、待ちで避けたら「根本修正」ではなく「試験対象を安定状態に限定した」と明記する
- (C) 本体の器の flags を変える (FLAG_ALT_FOCUSABLE_IM 等): `canBeImeTarget()` の判定を通る組み合わせに変わり、IME layering target の意味が変わる。無害な回避策ではない (却下寄り)
- 捕まった分岐だけを直す: served view 不一致なら (A) を評価、要求可視性の競合なら状態遷移に対する待機条件、Toast による target 移動なら本体側へ戻る

## 決定事項

- 2026-09-09 オーナー決定: (C) B → A の順で進める (advisor の推奨「観測を足して集中反復し、捕まった分岐だけ直す」と一致)
- B の形: 一時テスト (probe) を CI に持ち込まず、**本物の suite をそのまま反復する**。理由は (1) 失敗は suite の文脈でしか出ていない、(2) 一時ファイルを develop に載せずに済む、(3) logcat の `ImeTracker` があれば本物のテストの失敗だけで署名が取れる
- B の実装: 先に A の観測部分 (instrumented job で `adb logcat -v time` を gradle の前に背景で開始し、`if: always()` で logcat と結果 XML を成果物に、失敗時は `dumpsys input_method` / `dumpsys window` も) を verify-android-instrumented.yml に入れる。次に一時的な `workflow_dispatch` の workflow (matrix で同じ再利用 workflow を N 回並列に呼ぶ。既定 8 run) を置いて回す。署名が取れたら (または 16 run で出なければ) 一時 workflow は削除する
- A の残り (テストの硬化) は B の署名を見てから決める。署名が無くても入れる項目: Toast の表示時間を IME 待ちの上限から独立させ後始末で撤去する / 時間切れのメッセージに時計付き履歴を出す。「server が IME の枠を報告した状態を待つ」は署名がその型を示したときだけ入れる (advisor: 待ちで避けたら「試験対象を安定状態に限定した」と明記)
- 本体 (ToastContainer の window flags) には手を入れない。実測で本体起因の署名が出ていない

## ADR 候補 (作成済み: なし / 未起票: なし。本体の器の flags に手を入れる判断になったら候補)

## 未決の論点

探索中 (2026-09-08 開始)。分かっている疑問点:

- 手元の arm64 Emulator では自然再現も強制再現も得られていない (上表)。CI の x86_64 + swiftshader 固有の遅さが要るなら、次の観測は CI 側に置くしかない (logcat の ImeTracker / InsetsController を job の成果物に残す)
- API 36 指定だけでは実装を固定できない。CI の system image revision・build fingerprint・Emulator version・IME package を記録して AOSP の照合対象 (android-16.0.0_r1) と突き合わせる
- Toast の期限は受理時点から進むため、8 秒の IME 待ちを始めた時点で残りは 8 秒未満 (試験設計上の欠陥。hide 不達の説明にはならない)
- CI では撮影引数が無く `capture` は no-op。手元の証跡採取実行 (`waitForIdleSync` + 撮影) と show→hide 間の時間が違う

- 失敗時に IME がどの状態だったか (insets は true のまま / null / window token が別) を切り分ける観測が無い → 観測の設計は「検討した選択肢」へ (単一値ではなく履歴)
- `WindowInsetsController.hide(ime())` への切り替えで器の有無に左右されなくなるか → 修正の向き (A) へ。API 36 では IMM も同じ経路に合流するため単独では決め手にならない
- `-no-window` 固有か。`-no-window` / GPU / ABI / ホスト負荷 / 撮影の有無が同時に違うため「headless 固有」とはまだ言えない
- 再現率が低いため、修正の検出力は「修正前に fail することの確認」ではなく、失敗経路を一時的に強制する形 (`kasane/lessons/inbox/forced-failure-confirms-mechanism-when-repro-fails.md`) で担保することになりそう

## UI 素材 (ui/references/ の一覧と注釈)

なし

## 変更級の推奨: S (確定 2026-09-09。テストと CI workflow の局所修正。本体の器 (ToastContainer) に手を入れる根拠は実測で出ていない。CI 上の再現実験で本体起因の署名が出たら M へ)
