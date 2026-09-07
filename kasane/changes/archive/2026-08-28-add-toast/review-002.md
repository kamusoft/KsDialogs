# レビュー結果: add-toast (2 回目)

**日付**: 2026-08-27
**判定**: APPROVED

## サマリー

修正サイクル1周目で、review-001 の Major 2 / Minor 1 / Suggestion 2 と、second-opinion-code-001 で採用された
Major 2 のすべてが解消していることを、コード経路の追跡と全ビルドルートの再実行で確認した。修正はいずれも
既存の構造に沿っており、新しい抽象・新しい機構・スコープ膨張は持ち込んでいない。特に「入りの演出中に器を
載せ替えると中身が固まる」修正は Loading と Toast の**両方の器へ対称に**入り、期限確認は両 Native の
受理直後と取り付け経路の**両方**に置かれていて、指摘された競合の順序依存が構造ごと消えている。
新規の回帰テストは修正前に fail する形 (門で止めた入りの演出 / UI スレッドを占有したまま期限超過) で
組まれており、テストの手抜きはない。

残る指摘は、UI 照合証跡の**判定行が「乖離なし・修正なしの1周で収束」のまま**で、同じファイルの脚注
(Android の落ち影の乖離を検出して修正した旨) と矛盾している点 1 件のみ (Minor・低優先度)。
これは review-001 の Minor「証跡の記述が実装と食い違っている」の**言い直し部分が半分だけ残ったもの**で、
実装には影響しない。アーカイブ前に 2 行直せば済む。

指摘件数: Critical 0 / Major 0 / Minor 1 / Suggestion 3

## 実行した検証

| 対象 | 結果 |
|---|---|
| `ios/` `xcodebuild test -scheme KsDialogs` (iPhone 17 Sim) | **248 tests / 47 suites passed** (TEST SUCCEEDED。前回 246 から +2 = 期限の回帰テスト) |
| `android/` `./gradlew test --rerun-tasks` | BUILD SUCCESSFUL / 失敗 0 (`verifyNoDeclarativeUiDependency` 込み) |
| `android/` `ANDROID_SERIAL=<Pixel 4a / API 33> ./gradlew connectedDebugAndroidTest` | **`:ksdialogs` 268 / 0 failures / 1 skipped** + **`:ksdialogs-compose` 37 / 0 failures** = 計 305 (前回 300 から +5。skip 1 は規約どおりの API レベル skip) |
| `kmp/` `./gradlew allTests --rerun-tasks` | BUILD SUCCESSFUL / 失敗 0 |
| `maui/` `dotnet test` | **126 / 0 failures** (前回 122 から +4 = `ToastContentSupplyTests`) |
| `maui/android/native/` `:ksdialogs-maui-bridge:test --rerun-tasks` | BUILD SUCCESSFUL / 失敗 0 |
| 負の compile 検査 (iOS 4 本) | 4 本すべてを個別に実行し、**全件が期待した診断で失敗** (`has no member 'hide'` / `cannot convert '()' to 'String'` / `extra argument 'style'` / `has no member 'options'`)。iOS の factory を `throws` 化した後も効力が落ちていない |
| 負の compile 検査 (MAUI 4 本) | 4 本すべてを個別に実行し、**全件が期待した診断で失敗** (CS1061 / CS4008 / CS0029 / CS1739) |
| `scripts/scenario-id-coverage.py --require-mirror` | 未網羅なし・ミラー OK (175/197、除外 22 は Sample / 撮影支援の allow-missing) |
| `scripts/comment-policy-lint.py` / `local-path-lint.py` / `identity-lint.py` | いずれも違反 0 |

未実行: android / kmp の負の compile 検査 10 本 (本サイクルの修正が触れていない領域。review-001 で全件確認済み)、
MAUI iOS の実機通し (`ui/verification/sample-walkthrough.md` に環境要因として記録済み)。

## 前回指摘の解消確認

| 指摘 (出典) | 判定 | 根拠 |
|---|---|---|
| Loading 演出中の前面化で中身が固まる (review-001 Major 1) | **解消** | `DialogTransitionRunner.revealContentAsShown()` を新設し、`LoadingContainer.detachForReattach()` / `ToastContainer.detachForReattach()` が `playsPresentation && state ∈ {CREATED, ATTACHED, PRESENTING}` のときだけ中身を演出後の見えへ戻す。`finishRemoval()` (= `lifecycleJob.cancel()`) より**前**に呼ぶので、打ち切りとの競合がない。実効値が固まる前 (`CREATED`) の alpha=0 固着という最悪ケースも含む |
| 同上のテスト不足 | **解消** | `Loading_の入りの演出の途中で_Toast_を出しても中身の見えが固まらない` (`ToastMultiDisplayTests.kt:152`) は門で止めた入りのフック (`view.alpha = 0f` のまま await) を使うため、修正前は必ず alpha=0 で fail する構造。器ではなく**中身の View の alpha** を見ている。Toast 側の同型経路も `ToastTransitionTests` の `入りの演出の途中で提示先が入れ替わっても中身の見えが固まらない` で押さえられている。実機実行で両方 pass を確認 |
| Sample メニュー非スクロール (review-001 Major 2) | **解消** | 4ルートとも「見出し固定 + その下 (項目一覧 + 結果表示) をスクロール容器へ」で揃った (Android/KMP-Android = `ScrollView` + `LayoutParams(MATCH_PARENT, 0, 1f)`、MAUI = `Grid RowDefinitions="Auto,Auto,*"` の行3 に `ScrollView`、iOS/KMP-iOS = `ScrollView`)。オーナー決定として `deviation.md` に記録済み。証跡の連番 10 を5ルート撮り直し済みで、`ui/verification/android-10-overlap-result.png` に `直近の結果` / `結果: 完了` が完全に写っていることを画像で確認した |
| 期限切れ保留 Toast の取り付け競合 (second-opinion Major 1) | **解消** | Android は `ToastCoordinator.kt:117` (受理直後) と `:150` (`attachIfPossible` 先頭) の 2 か所、iOS は `ToastCoordinator.swift:140` と `:177` の 2 か所で単調時計の期限を確認し、到達済みなら**中身も器も作らずに**破棄する。ホスト復帰とタイマーの実行順に依存しない。回帰テストは UI スレッド / MainActor を占有したまま期限を越えてから提示先を戻す形で、順序を確定させている (両 Native で pass)。読み上げが流れないことまで見ている |
| MAUI iOS の factory 例外境界 (second-opinion Major 2) | **解消** | C# 側は `ToastContentSupply.Create` が `Exception` を捕捉して警告を残し `null` を返す。Swift 側は `MauiToastContentProvider` を `() -> MauiDialogContent?` (nullable) にし、`MauiToastViewModel.makeContentView()` が `throws` で `MauiDialogBridgeError.contentUnavailable` を投げ、Native の `beginDisplay` の `do/catch` が受理後の失敗として1枚だけ破棄する。binding 側も `[return: NullAllowed] delegate` で表現。回帰テスト 4 本 (`ToastContentSupplyTests.cs` — 例外が境界を越えない / null が返る / 警告が残る / 次の供給に影響しない) |
| Android の落ち影 (review-001 Minor) | **実装は解消・記録は一部残** | `ToastDefaultContentView.kt` に `outlineProvider = ViewOutlineProvider.BACKGROUND` + `elevation = dimension(ELEVATION_DP)` を追加。`ui/verification/android-01-default-toast.png` (撮り直し) にピルの輪郭に沿った影が写っていることを確認した。照合表の「影」行も両 OS の実現手段を書き分ける形へ直っている。ただし**判定行だけが古いまま** (下記 Minor) |
| 既定配線の検証 (review-001 Suggestion 1) | **解消** | `既定の前面化の配線でも_Loading_が_Toast_より前面に戻る` (`ToastMultiDisplayTests.kt:211`) が `loadingFrontKeeper` を差し替えずに `LoadingCoordinator.shared` を踏み、器が載せ直されることと中身の見え (alpha=1) を見ている |
| 前面化の間引き (review-001 Suggestion 2) | **対処済み (部分)** | 受理経路は `attachIfPossible` が実際に取り付けたときだけ前面化するようになり (`ToastCoordinator.kt:125-128`)、提示先の入れ替わり経路は「載せ直した枚数によらず1回」に畳まれた (`:271-280`)。回転時に枚数分繰り返す問題は消えている。Toast 1 枚ごとの前面化そのものは残るが、これは重なり順の要件から避けられない |
| 長命層の数値ずれ (review-001 Suggestion 3) | **申し送りのまま** (蒸留の担当範囲。対象が1件増える — 下記 Suggestion 3) |

## 指摘事項

### [🟡 Minor] 照合証跡の判定行が「乖離なし・修正なしの1周で収束」のままで、同じファイルの脚注と矛盾する

**該当箇所**: `ui/verification/default-view-mock-match.md:17`
(`**判定: 一致 (乖離なし)。修正なしの1周で収束。**`) と `ui/brief.md:48`
(`照合で**一致** (乖離ゼロ・修正なしの1周で収束。…)`)

**問題点**: 実際には「Android にモックの落ち影が無い」という乖離を検出し、実装を直して撮り直している。
そのことは同じファイルの脚注 (`※ 影の行は当初、Android 実装に落ち影が無いまま「同じ ✓」と記録していた
(review-001 の Minor 指摘)。2026-08-27 の修正で…`) に正しく書かれているのに、**その 4 行上の判定行が
「乖離なし・修正なしの1周で収束」のまま**で、読み手が最初に見る一文と脚注が真っ向から食い違っている。
`ui/brief.md` の要約行も同じ古い文言を持ち、こちらには脚注が無いため訂正の手掛かりすら無い。

これは review-001 の Minor が指摘した「証跡の記述が実装と食い違っている」型そのもので、表の1行は直った
一方で判定文が残った形。証跡は蒸留でそのままアーカイブされ、後から「この change では UI 乖離は
一度も出なかった」と読まれてしまうため、実害は記録の側にある。

**推奨修正**: 2 か所を実態に合わせる。例:
`default-view-mock-match.md:17` → 「判定: 一致 (Android の落ち影の乖離 1 件を修正して収束。詳細は下記 ※)」、
`brief.md:48` → 「照合で一致 (Android の落ち影の乖離 1 件を修正して収束)」。
どちらも足場ではない証跡ファイルなので、書き換えて差し支えない。

---

### [🔵 Suggestion] 前面化の載せ替えは、覆いと中身をまとめて最終状態へ飛ばす (入りの演出が途中で切れる)

**該当箇所**: `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/LoadingCoordinator.kt:341-350`
(`bringToFrontWithoutPresentation()`)、帰結は
`android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/LoadingContainer.kt:136-141` と `:221-224`

**問題点**: 今回の修正は review-001 が挙げた3案のうち「`!playsPresentation` 経路に中身の復元を入れる」を
採っている。欠陥 (中身が透明のまま固まる) は確実に消えたが、入りの演出の途中で Toast が出ると、
中身は `revealContentAsShown()` で、覆いは `overlayView.alpha = 1f` で、**どちらもその場で最終状態へ跳ぶ**。
design Decision 5 の「Loading の見た目は連続する」は「中身を失わない」という意味では満たされるが、
250 ms のフェードが途中で打ち切られて 1 フレームで完成形になる、という見え方にはなる。

さらに、`revealContentAsShown()` が戻せるのは器が動かす属性 (透明度・位置・倍率) だけなので、
プリセット以外の自作フックが `rotation` や子 View の属性を動かしている場合は、その分は途中の値で残る
(`DialogTransitionAnimator` を通す自作フックなら打ち切り時の `onProgress(1f)` で最終状態へ飛ぶので、
実際に残るのは独自に待ちを組んだフックに限られる)。コメントにもその射程が明示されている。

**推奨** (任意): review-001 の3案目「前面化を演出の完了後まで遅らせる」を後追いで足すと、演出の連続性まで
戻せる (Toast は覆いを持たないので、入りの 250 ms のあいだ Toast が前面にいても実害は小さい)。
本変更で直す必要はなく、design Risks の「ちらつきが出る場合は間引く余地がある」と同じ棚に置けば足りる。

---

### [🔵 Suggestion] `revealContentAsShown()` は中身の translation / scale を無条件で初期化する

**該当箇所**: `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/DialogTransitionRunner.kt:76-82`

**問題点**: 対になる `concealContent()` (`:63-66`) が控えるのは alpha だけで、translation / scale は控えていない。
一方 `revealContentAsShown()` は `translationX/Y = 0f` / `scaleX/Y = 1f` を無条件に書く。演出が動かした分を
戻すのが意図だが、**利用者が中身の View に自分で掛けている変形** (例: カスタム Loading の中身を
`scaleY` で調整している) も同時に踏む。器の載せ替え時にだけ起きるので発現は稀で、実害の報告はないが、
alpha と同じく「演出前の値を控えて戻す」形にすると意図と実装が一致する。

---

### [🔵 Suggestion] 長命層の申し送りに `sample-parity.md` を追加する

**該当箇所**: `kasane/concepts/cross/conventions/sample-parity.md:42`
(`現在のデモ項目は9件:` と、その下の文言の正の表)

**問題点**: review-001 Suggestion 3 は `test-execution.md` (負の検査 37 本) と `config.yaml`
(`ui.screenshot` の「安定デモ ID 9件」) を蒸留へ申し送っていたが、**同じ理由で `sample-parity.md` も
ずれている**。デモ項目は 9 → 14 に増え、`Hello Toast!` / `カスタムトースト` / `インライントースト` /
`Placed Toast` / `Overlap Toast` などが「Sample が画面に出す文言の全体」の表から抜けている。
`samples/README.md` は本変更で 14 件へ更新済みなので、concepts 側だけが取り残された形。

**推奨**: 蒸留 (ksn-distill) の concepts 追随で、`test-execution.md` / `config.yaml` と併せて更新する。
本変更での修正は求めない。

## 確認した観点 (問題なし)

- **足場凍結**: `specs/` (6 能力)・`proposal.md`・`design.md`・`ui/mock/` はいずれも無改変。1周目で
  変更されたのは `tasks.md` (チェック) と `ui/brief.md` (照合結果の追記) のみで、ksn-ui が許す範囲
- **deviation.md の同梱条件**: 4 件とも条件内。`[付随修正]` 2 件 (Android の既定 announce の
  `AccessibilityManager.isEnabled` ガード / Compose の観察テストの待ち条件) はどちらも本務ファイル内・
  局所・テスト 1 件で担保。iOS factory の `throws` 化は design のスケッチ差分として理由つきで記録済みで、
  spec の「受理後の失敗 = factory の例外」を iOS で到達可能にするために必要な変更。
  非 throwing なクロージャは `throws` 引数へそのまま渡せるためソース互換で、正の compile 検査
  (`ToastApiSurfaceCompileChecks.swift:103-127` の登録・インライン) が非 throwing のまま通っていることで実証されている。
  Sample メニューのスクロール化はオーナー決定として記録済み (「確認」ルートの正しい扱い)
- **期限確認の副作用**: Android の `discard()` は `onHostChanged` の `displays.toList()` 走査中に
  `displays` を縮めるが、走査はコピー上なので安全。`removeDisplay()` が購読中のコールバックの中から
  `hostRegistration.cancel()` を呼ぶ経路が新たに到達可能になったが、`ResumedActivityTracker.notifyResumedChange()`
  はロックの外でスナップショットを回すため、購読解除の入れ子も ConcurrentModification も起きない。
  iOS の `attachPendingDisplays()` も配列の値意味論で同じく安全
- **`revealContentAsShown()` と打ち切りの順序**: `detachForReattach()` は復元 → `finishRemoval()` (取り消し) の順で、
  どちらも UI スレッドの同一同期区間にある。`DialogTransitionAnimator` の打ち切り時 `onProgress(1f)` が
  後から上書きしても最終状態なので矛盾しない
- **`rollbackFailedStart()` 経路への波及**: 開始失敗の巻き戻しも `detachForReattach()` を通るため復元が走るが、
  利用者の View を「演出前の見え」で返すことになり、従来の alpha=0 のまま返すより素直。退行はない
- **Android のデフォルト View の影とレイアウト**: `elevation` は測定・配置に影響せず、レイアウト共通ケース表
  (C23 含む 20 ケース) と重なり判定 (画素の平均色) は実機で全件 green。C23 の期待値 (x=60 / y=486) も
  可視領域 → margin 24 → end 揃え → offset -80 の式から手計算で一致を確認した
- **KMP iOS の登録面**: `KsToastKmp.register` は Swift 側からの登録で非 throwing のまま。
  `KsDialogsInteropToastBridge.registerViewFactory` の非 throwing な `(Any) -> UIView` も
  既存の Dialog / Loading の interop 面と同形で、専用テストがある (MAUI iOS のような未捕捉の
  例外境界は KMP 経路には無い — factory は Swift 側で組み立てられる)
- **証跡の同一性**: `android-05/09/10` と `kmp-android-05/09/10` はバイト一致だが、両ルートの Sample は
  文言・テーマ・メニュー構成が完全に一致し、同一端末・同一状態 (10 はスクロール下端でクランプ) で撮られている。
  ステータスバーは切り落とされて時刻も写らないため、一致は想定内。`01/03/04/06/07/08` は差分があり、
  同じ画像の使い回しではないことも確認した
- **公開 API 面の増減**: 1周目の修正で増えた公開面は無い (`revealContentAsShown` は internal、
  `ToastContentSupply` は internal、`MauiDialogBridgeError.contentUnavailable` は互換面内部)。
  Sample のスクロール化はライブラリに触れていない
- **コメント**: 新規・変更コメントはいずれも ADR 番号だけに寄りかからず単独で読める
  (`revealContentAsShown` / `isBeforePresentationFinished` / `ToastContentSupply` / 期限確認のいずれも
  「なぜそうするか」を先に書いている)。`comment-policy-lint.py` 違反 0

## アクションプラン

1. **[Minor]** `ui/verification/default-view-mock-match.md:17` と `ui/brief.md:48` の判定文を、
   「Android の落ち影の乖離 1 件を修正して収束」へ直す (アーカイブ前に)
2. **[Suggestion]** 前面化の遅延 (演出完了まで待つ) と `revealContentAsShown()` の
   translation / scale の控え方は、必要が出たときの余地として design Risks / lessons に置く
3. **[Suggestion]** 蒸留で `test-execution.md` (負の検査 37 → 55) / `config.yaml` (デモ ID 9 → 14) に加えて
   `sample-parity.md` (デモ項目 9 → 14 件と文言の正の表) も更新する
