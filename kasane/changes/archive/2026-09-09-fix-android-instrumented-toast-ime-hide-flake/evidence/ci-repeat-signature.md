# 反復 CI で取った失敗の署名 (Toast 表示中の IME hide)

`ToastSystemInputTests` の `Toast_表示中でも_IME_を出し入れできる` が検証 CI でだけ間欠的に落ちる件について、本物の suite を CI 上で並列反復して失敗の時系列を採取した記録。抜粋はすべて `python3 scripts/log-sanitize.py` を通したもので、置換対象は 0 件だった (端末個体・個人・秘密を特定する値は元から含まれない)。logcat の全文は CI の成果物 (`android-instrumented-ime-observation-<N>`) と手元保管にあり、ここには判別に要る行だけを載せる。

## 反復の条件

| 項目 | 値 |
|---|---|
| workflow | 一時的な `workflow_dispatch` から検証用の再利用 workflow を 8 回並列に呼ぶ |
| run | 34304021835 (本番の 8 run)。試走は 34303269469 |
| 回すもの | 本物の suite 全件 (`connectedDebugAndroidTest`、`:ksdialogs-core` 294 件 / `:ksdialogs` 39 件)。probe は持ち込まない |
| Emulator | API 36 `google_apis` x86_64、1 台、`-no-window -gpu swiftshader_indirect -noaudio -no-boot-anim -no-snapshot -camera-back none`、アニメーション無効 |
| system image | `system-images;android-36;google_apis;x86_64` revision 7 |
| build fingerprint | `google/sdk_gphone64_x86_64/emu64xa:16/BE2A.250530.026.F3/13894323:userdebug/dev-keys` |
| IME | Gboard (`com.google.android.inputmethod.latin`) |
| 採取 | `adb logcat -b all -v threadtime` を gradle の前から常時。失敗回は `dumpsys input_method` / `dumpsys window` も |

## 結果 (8 run)

| 採取 | 落ちたテスト |
|---|---|
| obs-7 | IME hide |
| obs-4 | IME hide、戻るとホーム |
| obs-2 | LD_WN_01、LD_CO_13、IME hide |
| obs-5 | LD_AT_04、IME hide |
| obs-6 | LD_AT_03 |
| obs-1 | LD_CO_13 |
| obs-3 | (成功) |
| obs-8 | (成功) |

IME hide の失敗は 8 run 中 4 回。4 回とも下記の署名が一致し、成功回では発生していない。obs-4 で同時に落ちた「戻るとホームが通る」は、前のテストが残した可視状態の IME が最初の BACK を食べた波及 (`HIDE_SOFT_INPUT_REQUEST_HIDE_WITH_CONTROL fromUser true`) で、独立した欠陥ではない。LD_* の失敗は Loading 側の別件。

## 署名

テストの流れは show#1 → hide#1 (Toast を出す前の基準) → Toast 表示 → show#2 → hide#2。失敗回では次の順に進む。

1. **hide#1 が show#1 の表示アニメーションの最中に出る。** x86_64 の Emulator では show の要求から `onShown` まで約 1 秒かかる一方、テストは `rootWindowInsets.isVisible(ime())` が true になった時点で先へ進むため、hide#1 は表示の途中に落ちる。この hide は `ImeInsetsSourceConsumer` の保留になる
2. **保留の適用で要求可視性が一瞬 hidden になる。** `InsetsController` が `requestedVisibleTypes` を `-9 (was -1)` に落とし、約 30 ms 後に `-1 (was -9)` へ戻す。テストの `awaitImeVisible(false)` はこの隙に「引っ込んだ」と**偽の合格**を出す (IME は実際には消えていない)
3. **本命の hide#2 が捨てられる。** 要求可視性が既に hidden の側にあるため `onCancelled at PHASE_CLIENT_ALREADY_HIDDEN` になり、直後に show#1 / show#2 が `onShown` して要求可視性が visible へ戻る
4. **誰も hide を再送しない。** 8 秒の待ちは無音のまま時間切れになる (失敗回のテスト窓には `onHidden` が 1 件も無い)

成功回は hide#2 の要求行が保留の適用より先に出るため、show#2 を `PHASE_CLIENT_ANIMATION_CANCEL` で潰して最終的に `onHidden` へ到達する。差は数十ミリ秒の並びだけで、環境の速さがそのまま出る。

### 失敗回 (obs-7) の決定的な並び

```
09-09 02:47:38.518  5367  5367 I ImeTracker: …ksdialogs.test:2b7f1ce5: onRequestShow at ORIGIN_CLIENT reason SHOW_SOFT_INPUT fromUser false
09-09 02:47:38.521  5367  5367 D InsetsController: show(ime(), fromIme=false)
09-09 02:47:38.521  5367  5367 D InsetsController: Setting requestedVisibleTypes to -1 (was -9)
09-09 02:47:39.452  5367  5367 I ImeTracker: …ksdialogs.test:b5b6aef4: onRequestHide at ORIGIN_CLIENT reason HIDE_SOFT_INPUT fromUser false
09-09 02:47:39.453  5367  5367 D InsetsController: hide(ime(), fromIme=false)
09-09 02:47:39.453  5367  5367 D InsetsController: Setting requestedVisibleTypes to -9 (was -1)
09-09 02:47:39.454  5367  5367 I ImeTracker: …ksdialogs.test:2b7f1ce5: onCancelled at PHASE_CLIENT_ANIMATION_CANCEL
09-09 02:47:39.485  5367  5367 D InsetsController: Setting requestedVisibleTypes to -1 (was -9)
09-09 02:47:39.485  5367  5367 I ImeTracker: …ksdialogs.test:2b7f1ce5: onShown
09-09 02:47:39.499  5367  5367 I ImeTracker: …ksdialogs.test:cc95f673: onRequestShow at ORIGIN_CLIENT reason SHOW_SOFT_INPUT fromUser false
09-09 02:47:39.499  5367  5367 D InsetsController: show(ime(), fromIme=false)
09-09 02:47:39.499  5367  5367 I ImeTracker: …ksdialogs.test:b5b6aef4: onCancelled at PHASE_CLIENT_ANIMATION_CANCEL
09-09 02:47:39.519  5367  5367 D InsetsController: Setting requestedVisibleTypes to -9 (was -1)
09-09 02:47:39.531  5367  5367 I ImeTracker: …ksdialogs.test:a9f40944: onRequestHide at ORIGIN_CLIENT reason HIDE_SOFT_INPUT fromUser false
09-09 02:47:39.531  5367  5367 D InsetsController: hide(ime(), fromIme=false)
09-09 02:47:39.532  5367  5367 I ImeTracker: …ksdialogs.test:a9f40944: onCancelled at PHASE_CLIENT_ALREADY_HIDDEN
09-09 02:47:39.534  5367  5367 D InsetsController: Setting requestedVisibleTypes to -1 (was -9)
09-09 02:47:39.535  5367  5367 I ImeTracker: …ksdialogs.test:cc95f673: onShown
09-09 02:47:47.624   639  2856 I ImeTracker: system_server:a71501c5: onRequestHide at ORIGIN_SERVER reason IME_REQUESTED_CHANGED_LISTENER fromUser false
```

要求 ID の対応: `2b7f1ce5` = show#1、`b5b6aef4` = hide#1、`cc95f673` = show#2、`a9f40944` = hide#2。偽の合格が起きた窓は 02:47:39.453〜39.485 (要求可視性が `-9` になって戻るまでの約 32 ms)。最終行の 02:47:47.624 は 8 秒の待ちが尽きて Activity が畳まれ始めた時刻で、hide#2 に対応する `onHidden` は窓内に 1 件も無い。

### 成功回 (obs-3) との違い

```
09-09 02:47:48.586  5484  5484 I ImeTracker: …ksdialogs.test:499153a7: onRequestShow at ORIGIN_CLIENT reason SHOW_SOFT_INPUT fromUser false
09-09 02:47:48.589  5484  5484 D InsetsController: Setting requestedVisibleTypes to -1 (was -9)
09-09 02:47:49.560  5484  5484 I ImeTracker: …ksdialogs.test:1a0e8a69: onRequestHide at ORIGIN_CLIENT reason HIDE_SOFT_INPUT fromUser false
09-09 02:47:49.562  5484  5484 D InsetsController: Setting requestedVisibleTypes to -9 (was -1)
09-09 02:47:49.562  5484  5484 I ImeTracker: …ksdialogs.test:499153a7: onCancelled at PHASE_CLIENT_ANIMATION_CANCEL
09-09 02:47:49.581  5484  5484 D InsetsController: Setting requestedVisibleTypes to -1 (was -9)
09-09 02:47:49.582  5484  5484 I ImeTracker: …ksdialogs.test:499153a7: onShown
09-09 02:47:49.590  5484  5484 I ImeTracker: …ksdialogs.test:9164455b: onRequestShow at ORIGIN_CLIENT reason SHOW_SOFT_INPUT fromUser false
09-09 02:47:49.591  5484  5484 I ImeTracker: …ksdialogs.test:1a0e8a69: onCancelled at PHASE_CLIENT_ANIMATION_CANCEL
09-09 02:47:49.615  5484  5484 I ImeTracker: …ksdialogs.test:6b73a6f6: onRequestHide at ORIGIN_CLIENT reason HIDE_SOFT_INPUT fromUser false
09-09 02:47:49.615  5484  5484 D InsetsController: Setting requestedVisibleTypes to -9 (was -1)
09-09 02:47:49.617  5484  5484 I ImeTracker: …ksdialogs.test:9164455b: onCancelled at PHASE_CLIENT_ANIMATION_CANCEL
09-09 02:47:49.618  5484  5484 D InsetsController: Setting requestedVisibleTypes to -1 (was -9)
09-09 02:47:49.619  5484  5484 I ImeTracker: …ksdialogs.test:9164455b: onShown
09-09 02:47:49.627  5484  5484 D InsetsController: Setting requestedVisibleTypes to -9 (was -1)
09-09 02:47:49.781  1493  1493 I ImeTracker: system_server:ee21bd5c: onHidden
```

要求 ID の対応: `499153a7` = show#1、`1a0e8a69` = hide#1、`9164455b` = show#2、`6b73a6f6` = hide#2。hide#2 (49.615) が保留の適用より先に出ているため `PHASE_CLIENT_ALREADY_HIDDEN` に落ちず、show#2 を `PHASE_CLIENT_ANIMATION_CANCEL` で潰して 49.781 に `onHidden` へ到達する。失敗回との差は数十ミリ秒の並びだけ。

## 仮説の判定

| 仮説 | 判定 | 根拠 |
|---|---|---|
| H1: IMM の served view / window token 照合に外れて hide が配送されない | 否定 | 失敗回でも hide#2 は `ORIGIN_CLIENT reason HIDE_SOFT_INPUT` として記録され、`InsetsController.hide(ime(), fromIme=false)` まで到達している。止まっているのはその先の要求可視性の判定 (`PHASE_CLIENT_ALREADY_HIDDEN`) |
| H2: client の要求可視性と server の IME 状態のレース | 支持 (確定) | 上記の署名そのもの。保留された hide#1 の適用で要求可視性が一瞬 hidden に振れ、その隙に読んだ偽の合格が hide#2 を無効化する |
| H3: Toast と無関係な Emulator / IME プロセス側の停滞 | 否定寄り | `Choreographer: Skipped frames` 等の負荷マーカーの出方は失敗回と成功回で偏りが無く、ANR も無い。失敗回でも要求と応答は数ミリ秒で往復している |
| H4: Toast の器が IME の control target を奪う | 否定 | Toast ウィンドウの追加と、それが誘発する `CONTROLS_CHANGED` は成功回にも同じく現れる。失敗の分岐は Toast を出す前の hide#1 の扱いで既に決まっている |

**Toast は引き金ではない。** 失敗の分岐点 (hide#1 が表示アニメーション中に出て保留になる) は Toast を表示する前に起きており、本体 (`ToastContainer`) の窓の属性は関与しない。

## 手元での検出力の確認 (2026-09-09)

環境: AVD `ksn_api36_headless` (API 36 google_apis **arm64**、`google/sdk_gphone64_arm64/emu64a:16/BE2A.250530.026.F3/13894323:userdebug/dev-keys`、Gboard)、`-no-window -gpu swiftshader_indirect -noaudio -no-boot-anim -no-snapshot`、アニメーション 3 種 = 0。テストクラスを `am instrument` で直接反復した。

### 旧条件と硬化後の A/B

比べた 2 版の定義は次のとおり。**待ち条件と観測の取り付けの両方が同時に変わっている**ので、この A/B だけでは「待ち条件の硬化が効いた」と一意には切り分けられない (どちらが何を塞いだかは下の帰属で述べる)。

| 版 | 待ちの判定 | アニメーションの観測 | 最初の要求前の落ち着き待ち |
|---|---|---|---|
| 旧条件 | 要求可視性 (`isVisible(ime())`) の 1 回の読み | 取り付けない | 無し |
| 硬化後 | 要求可視性 + 枠の高さ + アニメーション非進行が 48 ms 続くこと | 取り付ける (`WindowInsetsAnimation.Callback`) | 無し |

| 版 | 反復 | 失敗 | 内訳 |
|---|---|---|---|
| 旧条件 | 20 | 5 | **「Toast 表示中に IME が引っ込まない」3 件** (CI と同じ署名)、「Toast を出す前から IME が出ない」2 件 |
| 硬化後 | 50 | 2 | 「Toast を出す前から IME が出ない」2 件のみ。引っ込まない側は 0 件 |

硬化後の 50 回は旧条件の 20 回を挟んで前 30 回・後 20 回に分かれており、端末が温まって失敗しやすくなった後 (旧条件が 5 件落ちた直後) の 20 回でも 0 件だった。1 回あたりの実行時間も 8.5 秒前後から 1.3 秒前後へ縮んでいる (Toast の期限切れを待たずに後始末で撤去するため)。

**この表の「硬化後」は、最初の要求前の落ち着き待ちを入れる前の版である。** 残った 2 件はその後に足した落ち着き待ちで塞がれ、[evidence/ci-flake-triage.md](ci-flake-triage.md)「版の定義」の版 B (落ち着き待ちを入れた版) では 30 回中 0 回になる。2 本の evidence で「対応後」の数字が違うのはこの版の差で、落ち着き待ちを入れる前は 2 件残る、が正しい読み方。

#### どの条件が何を塞いだか

| 条件 | 塞いだもの |
|---|---|
| アニメーション非進行を待つ | 署名そのもの。表示要求の走行が終わるまで次の非表示要求を出さないので、「表示途中の hide が保留される」分岐に入らない |
| 枠の高さ (`bottom`) | 単独では何も塞がない。不可視の種別に対して枠は 0 で返るため要求可視性とほぼ同値で、偽の合格の窓 (要求可視性が 32 ms だけ hidden へ振れる間) では両方が同時に偽になる。見えと枠が食い違ったまま先へ進まないことだけを担保する |
| 48 ms の継続 | 一瞬の振れを終端と読む経路。単独の読みで判定しないことが前提条件になる |
| 最初の要求前の落ち着き待ち | 下記の別モード。Activity 起動に伴う要求とテスト最初の要求の交差 |

残る「Toast を出す前から IME が出ない」は**別の機構**で、旧条件でも同じ率で起きる。Activity 起動に伴う server 起源の `HIDE_UNSPECIFIED_WINDOW` がテスト最初の show と交差して `PHASE_WM_ABORT_SHOW_IME_POST_LAYOUT` で show を潰す形で、今回の署名 (hide が届かない) とは向きが逆。硬化後の失敗メッセージにはその履歴 (`showSoftInput を呼んだ` の後に `isVisible=false bottom=0 animating=false` が続いたまま) が出る。

### 手元で再現した同じ署名 (旧条件・失敗回)

```
09-09 12:17:58.666  9510  9510 I ImeTracker: …ksdialogs.test:19741f1b: onRequestShow at ORIGIN_CLIENT reason SHOW_SOFT_INPUT fromUser false
09-09 12:17:58.668  9510  9510 D InsetsController: Setting requestedVisibleTypes to -1 (was -9)
09-09 12:17:58.735  9510  9510 I ImeTracker: …ksdialogs.test:1dd2cc4a: onRequestHide at ORIGIN_CLIENT reason HIDE_SOFT_INPUT fromUser false
09-09 12:17:58.736  9510  9510 D InsetsController: Setting requestedVisibleTypes to -9 (was -1)
09-09 12:17:58.736  9510  9510 I ImeTracker: …ksdialogs.test:19741f1b: onCancelled at PHASE_CLIENT_ANIMATION_CANCEL
09-09 12:17:58.753  9510  9510 D InsetsController: Setting requestedVisibleTypes to -1 (was -9)
09-09 12:17:58.753  9510  9510 I ImeTracker: …ksdialogs.test:19741f1b: onShown
09-09 12:17:58.765  9510  9510 I ImeTracker: …ksdialogs.test:5b529c28: onRequestShow at ORIGIN_CLIENT reason SHOW_SOFT_INPUT fromUser false
09-09 12:17:58.765  9510  9510 I ImeTracker: …ksdialogs.test:1dd2cc4a: onCancelled at PHASE_CLIENT_ANIMATION_CANCEL
09-09 12:17:58.794  9510  9510 D InsetsController: Setting requestedVisibleTypes to -9 (was -1)
09-09 12:17:58.808  9510  9510 I ImeTracker: …ksdialogs.test:1affb058: onRequestHide at ORIGIN_CLIENT reason HIDE_SOFT_INPUT fromUser false
09-09 12:17:58.809  9510  9510 I ImeTracker: …ksdialogs.test:1affb058: onCancelled at PHASE_CLIENT_ALREADY_HIDDEN
09-09 12:17:58.810  9510  9510 D InsetsController: Setting requestedVisibleTypes to -1 (was -9)
09-09 12:17:58.810  9510  9510 I ImeTracker: …ksdialogs.test:5b529c28: onShown
09-09 12:18:06.890   662   694 I ImeTracker: system_server:b423d1c0: onRequestShow at ORIGIN_SERVER reason CONTROLS_CHANGED fromUser false
```

要求 ID の対応: `19741f1b` = show#1、`1dd2cc4a` = hide#1、`5b529c28` = show#2、`1affb058` = hide#2。CI と同じく、偽の合格の窓 (58.736〜58.753) を挟んで hide#2 が `PHASE_CLIENT_ALREADY_HIDDEN` で捨てられ、`onHidden` に到達しないまま 8 秒後 (12:18:06) の時間切れへ進む。手元の arm64 では出ないとされていた署名が、テストクラスを連続で反復すると再現する。

### 強制再現の試み (一時 probe)

旧条件の偽の合格を狙って強制する probe (コミットしていない) を、show の直後 0 / 8 / 16 / 30 / 60 / 100 ms に hide を出す 6 条件 × 5 回で回し、`isVisible(ime())` が false の間に `getInsets(ime()).bottom > 0` になる標本を探した。結果は 30 回とも 0 件で、いずれも show 自体が `PHASE_WM_ABORT_SHOW_IME_POST_LAYOUT` で中止されて IME が一度も出ず (`bottom` は 0 のまま)、狙った「表示途中の hide」に入らなかった。**強制再現は得られていない**が、上の A/B と手元の自然再現が同じ機構を捕まえているため、判別はそちらで足りている。
