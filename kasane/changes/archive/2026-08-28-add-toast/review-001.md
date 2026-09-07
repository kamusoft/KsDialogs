# レビュー結果: add-toast (1 回目)

**日付**: 2026-08-27
**判定**: CHANGES_REQUESTED

## サマリー

4形態 + samples の実装はデルタスペックの Requirement / Scenario をよく満たしており、テストの質が高い (実タッチ注入・実画素での重なり判定・ゲート付き演出フック・提示先不在の再現)。全ビルドルートを自分で回して green を確認し、新規の負の compile 検査 18 本がすべて期待どおり失敗すること、Scenario ID の網羅とミラーも確認した。

一方で、Android の「Loading 常時前面」の実現経路 (design Decision 5) に、**Loading の入りの演出中に Toast を出すと Loading の中身が透明のまま固まる**欠陥がある (design が約束した「見た目は連続する」を破る。現行の TS-MX-05 は覆いの暗さしか見ていないため検出できない)。加えて、デモ項目が5つ増えたことで **4ルートの Sample メニューから結果表示エリアが画面外へ押し出され**、Toast だけでなく既存デモの結果表示も見えなくなっている (証跡に記録済み・未決着)。

指摘件数: Critical 0 / Major 2 / Minor 1 / Suggestion 3

## 実行した検証

| 対象 | 結果 |
|---|---|
| `ios/` `xcodebuild test -scheme KsDialogs` (iPhone 17 Sim) | 246 tests / 47 suites passed (TEST SUCCEEDED) |
| `android/` `./gradlew test --rerun-tasks` | 67 tests / 0 failures (`verifyNoDeclarativeUiDependency` 込み) |
| `android/` `./gradlew connectedDebugAndroidTest` (実機 API 33) | 300 tests / 0 failures / 1 skipped (`PB_SB_04` = 規約どおりの API レベル skip)。内訳 `:ksdialogs` 263 + `:ksdialogs-compose` 37 |
| `kmp/` `./gradlew allTests --rerun-tasks` | 96 tests / 0 failures |
| `maui/` `dotnet test` | 122 tests / 0 failures |
| `maui/android/native/` `:ksdialogs-maui-bridge:test` | 29 tests / 0 failures |
| 負の compile 検査 (本変更で追加された 18 本) | android 5 / kmp 5 / ios 4 / maui 4 をフラグ個別に実行し、**全件が期待した診断で失敗**することを確認 |
| `scripts/scenario-id-coverage.py` (`--require-mirror` 込み) | 未網羅なし・ミラー OK (TS-CO 8 / NM 2 / MX 5 / AT 3 / TR 2 / AC 1 / IO 3 / AN 4 / MA 3 / KM 3、TS-SA 6 は allow-missing) |
| `scripts/comment-policy-lint.py` / `local-path-lint.py` / `identity-lint.py` | いずれも違反 0 |

## 指摘事項

### [🟠 Major] Loading の入りの演出中に Toast を出すと、Loading の中身が透明のまま固まる

**該当箇所**: `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/LoadingCoordinator.kt:340-348`
(呼び出し元 `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/ToastCoordinator.kt:170`、
帰結 `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/LoadingContainer.kt:161-171` / `:203`)

**問題点**: `bringToFrontWithoutPresentation()` は `container != null && dismissalJob == null` だけを見て、
**Loading が入りの演出の途中かどうかを見ずに**器を作り直す。経路をたどると:

1. `Loading.show()` → `attach(playsPresentation = true)` → `LoadingContainer.init` が
   `transitionRunner.concealContent()` を呼び中身の alpha を 0 にする (`DialogTransitionRunner.kt:63`)
2. 実効値が固まると `beginPresentation()` → `runPresentationPhase()` (`DialogTransitionRunner.kt:92`) が
   alpha を戻したうえで既定の fade フックを走らせる。fade の presentation は `view.alpha = 0f` から
   250 ms かけて 1 へ動かす (`DialogTransition.kt:46-58`)
3. この 250 ms の間に Toast が取り付くと `ToastCoordinator.kt:170` → `bringToFrontWithoutPresentation()` →
   `detachForReattach()` が `lifecycleJob?.cancel()` で**演出を途中で打ち切る**
4. 続く `attach(host, playsPresentation = false)` で作られた器は、`beginLifecycle()` の
   `if (!playsPresentation) { containerState = SHOWN; return }` (`LoadingContainer.kt:167-170`) により
   `runPresentationPhase()` を一切走らせない。復元されるのは覆いだけ (`LoadingContainer.kt:203` の
   `overlayView.alpha = 1f`) で、**中身の alpha を戻すコードがどこにも無い**

結果、Loading の中身 (インジケータ・メッセージ) は打ち切られた時点の alpha (最悪 0) のまま、その表示が
終わるまで固まる。実効値が固まる前に Toast が来た場合は alpha = 0 のままなので、**画面が暗くなり操作も
ブロックされるのにインジケータが見えない**状態になる。`Loading.show(); Toast.show("…")` や
`Loading.start { Toast.show("…"); … }` という自然な呼び出しで踏む。slide / zoom プリセットでは
translation / scale が途中値で固まる同型の壊れ方になる。

design Decision 5 は「中身と実効値は同じものを載せ替え、Loading の見た目は連続する」と明記しており、
この経路はその保証を崩している。

**テストで捕まらない理由**: `android/ksdialogs/src/androidTest/kotlin/jp/kamusoft/ksdialogs/ToastMultiDisplayTests.kt:153-190`
の `assertLoadingCoversToast(showsLoadingFirst = true)` は、`loadingCoordinator.isPresenting`
(= `container != null && dismissalJob == null`。演出中も true) を待った直後に Toast を出すため、
この欠陥をむしろ毎回踏んでいる。しかし判定は「Toast のピルが覆いで暗くなったか」だけで、覆いは
`overlayView.alpha = 1f` で復元されるため素通しになる。

**推奨修正** (いずれか):

- `bringToFrontWithoutPresentation()` を `containerState` で分岐させ、`PRESENTING` (および実効値が
  未固定の `CREATED` / `ATTACHED`) では入りの演出をやり直す形で載せ直す
- `LoadingContainer` の `!playsPresentation` 経路に「見えている状態から続ける」ための中身の復元
  (覆いと対称に、中身を最終状態へ戻す操作) を入れる
- 前面化を演出の完了後まで遅らせる

あわせて、テストは **Loading の覆いではなく中身の見え** (中身 View の alpha、または中身の矩形の画素) を
見るものを足してほしい。修正前に fail することを報告に含めてもらえると確実。

---

### [🟠 Major] Sample のメニューがスクロールしないため、結果表示エリアが画面外へ押し出される

**該当箇所**: `samples/android/app/src/main/kotlin/jp/kamusoft/ksdialogs/samples/android/SampleMenuView.kt:45`
(`LinearLayout` 直下・スクロールなし)、`samples/maui/KsDialogs.Sample.Maui/SampleMenuPage.xaml:11`
(`VerticalStackLayout` 直下)、`samples/kmp/androidApp/src/main/kotlin/jp/kamusoft/ksdialogs/samples/kmp/android/SampleMenuView.kt:30`、
`samples/ios/KsDialogsSample/SampleMenuScreen.swift:10` (`VStack`)。証跡: `ui/verification/android-10-overlap-result.png`

**問題点**: デモ項目が 9 → 14 に増えた結果、1080x2340 級の端末では結果表示エリアの見出し
`直近の結果` が下端で切れ、値 (`結果: 完了` 等) は画面外に出る。証跡画像でも見出しが切れているのが確認できる。
影響は Toast のデモに限らず、**既存の Dialog / Loading デモの結果表示 (PB-SM / LD-SA / MB-SM 系の
Sample 通しで確認していた表示) まで巻き添えで見えなくなる**。4ルートとも縦積みでスクロール容器を持たないため、
画面が小さい端末では同じことが起きる (iOS も iPhone SE 級では同様のはず)。

`ui/verification/sample-walkthrough.md` には現象と「本作業では同梱せず、扱いをオーナー判断に委ねる」ことが
記録されているが、`deviation.md` には無く、samples デルタスペックの
TS-SA-05 (`Toast の消滅後に 結果: 完了 が表示される`) / TS-SA-06 (`結果表示が4ルートで一致する`) に対する
未解消の非適合が残ったままになっている。

**推奨修正**: ksn-core の付随修正の判定でいう「確認」に当たる (4ルートに手が入り設計判断を伴う) ため、
オーナーへ **同梱 / 起票 / 見送り** を諮り、決まった扱いを `deviation.md` に記録してほしい。
同梱するならメニューをスクロール可能な容器に入れる (Android = `ScrollView`、MAUI = `ScrollView`、
iOS / KMP iOS = `ScrollView`) のが最小の変更に見える。

---

### [🟡 Minor] Android のデフォルト View に承認モックの落ち影が無く、照合ノートは「一致」と記録している

**該当箇所**: `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/ToastDefaultContentView.kt:52-56`
(背景は `GradientDrawable` のみ。`elevation` / `outlineProvider` の指定なし) と
`ios/Sources/KsDialogs/Presentation/ToastDefaultContentView.swift:49-54`
(`shadowOpacity = 0.25` / `shadowRadius = 5` / `shadowOffset = (0, 2)`)。
モック `ui/mock/default-pill.html:28` は `box-shadow: 0 2px 10px rgba(0,0,0,.25)`。

**問題点**: 承認済みモックの「弱い落ち影」を iOS は実装しているが Android は実装していない。
デフォルト View の見えは公開 API 面 (core/ADR-0028) であり、レイアウト共通仕様が OS 差を挙動に
持ち込まない方針を採っている中で、見えの要素が片方だけ欠けている。
さらに `ui/verification/default-view-mock-match.md` の照合表は「影 | 弱い落ち影 | 同じ | ✓」と
両 OS 一致で記録しており、**証跡の記述が実装と食い違っている**。

**推奨修正**: Android 側に同等の落ち影を入れる (`elevation` + `outlineProvider`、または
`GradientDrawable` を包む形) か、影を「OS 差として許容する合意済み妥協」としてブリーフ / 照合ノートに
明記するかを決め、いずれにせよ照合ノートの「同じ ✓」を実態に合わせて直してほしい。

---

### [🔵 Suggestion] 差し替え口の「既定実装」が production 配線のまま検証されていない箇所が残っている

**該当箇所**: `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/ToastCoordinator.kt:43-44`
(既定の `ToastLoadingFrontKeeper` = `LoadingCoordinator.shared.bringToFrontWithoutPresentation()`)、
テスト側は `android/ksdialogs/src/androidTest/kotlin/jp/kamusoft/ksdialogs/support/ToastTestHarness.kt:52-53`
でテスト用の `LoadingCoordinator` を注入している。

**問題点**: 本変更で捕捉した教訓 (`kasane/lessons/inbox/sample-walkthrough-catches-bridge-defects.md` の
add-toast 分) がまさに「差し替え口の既定実装が無検査だった」型で、`SystemToastAccessibilityAnnouncer` は
その後テストが足された。同じ形の口が Toast にはもう1つあり、**Loading 表示中に Toast を出す経路は
production 配線 (`LoadingCoordinator.shared`) では一度も通っていない** — Sample の `Toast Overlap` も
Toast → Dialog → Loading の順なので、前面化の実効経路を踏まない。上の Major 1 を直すときに、
この配線ごと踏む検証を1本足しておくと同型の穴が閉じる。

---

### [🔵 Suggestion] Toast を出すたびに Loading の Window を作り直す

**該当箇所**: `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/ToastCoordinator.kt:170`

**問題点**: `attachIfPossible` は取り付けのたびに無条件で `bringLoadingToFront()` を呼ぶため、
Loading 表示中は Toast 1 枚ごとに Loading の Window が破棄・再生成される。画面の再生成 (回転) では
表示中の Toast の枚数だけ繰り返される。design の Risks に「間引く余地がある」と書かれているとおりで、
挙動契約は変わらないが、Loading が既に最前面なら何もしない・同一フレーム内では 1 回に畳む、といった
間引きを入れる余地がある (Major 1 の修正と同じ箇所を触るのでついでに検討する価値がある)。

---

### [🔵 Suggestion] 長命層の数値が実態とずれた

**該当箇所**: `kasane/concepts/cross/conventions/test-execution.md` (負の検査「37 本」の表と件数表)、
`kasane/config.yaml` の `ui.screenshot` (「安定デモ ID 9件」)

**問題点**: 本変更で負の検査は 37 → 55 本、安定デモ ID は 9 → 14 件、各ルートの実測件数も
上記「実行した検証」の表のとおり変わった。どちらの文書も「実態が変われば実測で更新する」「一覧の正は
samples/README.md」と書いてあるので破綻はしていないが、次の drift / distill で拾えるよう申し送りたい。
(蒸留の担当範囲なので本変更での修正は求めない)

## 確認した観点 (問題なし)

- **デルタスペック適合**: TS-CO/NM/MX/AT/TR/AC の全 Scenario に同名テストが両 Native に存在し、
  実提示・実タッチ・ゲート付きフックで検証している。失敗モデル (解決の fail-fast / 受理後の破棄と資源解放 /
  提示先不在での保留と満了破棄)、duration の時間モデル (受理時点起点・単調時計・入りの途中でも出へ移る)、
  配置の優先順 (show 引数 > 添付 > style 既定 > 契約既定) はいずれもコードと専用テストで押さえられている
- **足場アーティファクト**: `specs/` は無改変。変更されたのは `tasks.md` (チェック) と `ui/brief.md`
  (照合結果の追記 — ksn-ui が明示的に許可している追記) のみ
- **tasks.md の虚偽チェック**: 全項目に対応する実装・テスト・証跡を確認。6.3 の maui(iOS) 未実施は
  環境要因として `ui/verification/sample-walkthrough.md` に理由つきで記録されており、MAUI ルートは
  Android 側で通っている
- **deviation.md の同梱条件**: `[付随修正]` 1 件は本務ファイル内・局所・テスト 1 件で担保されており条件内
- **公開 API 面**: 新規 public は `KsToast` / `Toast` / `ToastStyle` / `ToastViewModel` /
  `ToastViewRegistry` (+ 各形態の対応物) に限られ、coordinator・器・提示面・announcer・factory は
  すべて internal。意図しない public 露出は無い。iOS / Android / MAUI / KMP の入口の形は既存の
  Dialog / Loading の流儀 (`Toast.shared` / `Toast.instance` / `Toast.Instance`) と揃っている
- **bridge の運搬**: `ToastStyle` の 6 項目 (背景色 / 文字色 / フォントサイズ / 角丸 / 既定 duration /
  アプリ既定配置) と duration・placement が、MAUI の C# → ObjC / Java 互換面 → Native まで
  1 対 1 で運ばれていることを ApiDefinition と Swift / Kotlin 両 bridge で突き合わせた (取りこぼしなし)
- **レジストリの層**: MAUI は C# 層レジストリ + 互換面専用 VM 1 型のみ Native 登録 (maui/ADR-0001)、
  KMP は Native レジストリへ委譲 (kmp/ADR-0002) と、それぞれ決定どおり
- **スレッド安全性**: レジストリと設定は両 Native ともロック付き。受理の直列化は iOS が
  `ToastAcceptanceQueue` の鎖、Android が `Dispatchers.Main` (immediate ではない) への post で、
  どちらも起動順を保つ
- **iOS の前面規則**: `insertSubview(belowSubview:)` で Loading の器の下へ入れる方式は Loading 側に
  一切触れないため、Major 1 のような副作用は無い (この非対称は iOS 側が安全な向き)
- **レイアウト**: 共通ケース表 20 ケース (新規 C23 = Toast の契約既定配置を含む) を Toast の器でも
  全件通しており、C23 は「添付なしでも同じ位置」の追加検証つき。C23 の期待値も式から手計算で一致を確認した
- **コメント規約**: `comment-policy-lint.py` が違反 0。コメントは ADR 番号だけに寄りかからず、
  単独で読める説明になっている
- **Sample パリティ**: 4ルートの `SampleText` / デモ ID / README の追記内容が完全に一致

## アクションプラン

1. **[Major 1]** `bringToFrontWithoutPresentation()` の演出中分岐を直し、Loading の中身の見えを見る
   テスト (修正前に fail するもの) を足す
2. **[Major 2]** Sample メニューのスクロールについてオーナーに 同梱 / 起票 / 見送り を諮り、
   決着を `deviation.md` に記録する
3. **[Minor]** Android デフォルト View の落ち影を入れるか、合意済み妥協として記録する。いずれにせよ
   `ui/verification/default-view-mock-match.md` の「影 … 同じ ✓」を実態に合わせる
4. **[Suggestion]** Major 1 の修正ついでに、既定の `ToastLoadingFrontKeeper` 配線を踏む検証と
   前面化の間引きを検討する
5. **[Suggestion]** `test-execution.md` / `config.yaml` の数値の更新を蒸留へ申し送る
