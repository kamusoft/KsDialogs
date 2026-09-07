# レビュー結果: add-vertical-slice (002 回目)

**日付**: 2026-08-15
**判定**: APPROVED

## サマリー

修正サイクル1の対象 8 件 (review-001 Major 1 の iOS / Android 手当て、review-001 Major 2 = codex Major 3、codex Major 2、codex Major 4、review-001 Minor 1・2、codex Minor) をすべて再確認し、**いずれも指摘を実際に解消している**ことを確認した。単に例外を握り潰す・テストを緩めるといった見せかけの修正はなく、どの修正も「差し替え可能な継ぎ目を切り出してテストを足す」形で入っている (`ApplicationKeyWindowProvider.selectKeyWindow` / `reportClosure` / `DialogContainer.reportDetached` / `DialogContainerViewController.viewDidDisappear`)。特に iOS の宙吊りは、テスト用提示面が UIKit の連鎖 dismiss と出現状態の遷移を忠実に模した上で MD-b テストが `上の show == .cancelled` を主張するようになっており、実質的な検証になっている。

全ビルドルートを再実行して全件緑 (ios 34 / android 26 / kmp 32 / maui 19 / bridge 5)、負のコンパイル検証も期待どおり 3 件で失敗、コメント規約 lint 0 件、足場アーティファクト (proposal / design / specs 6 件) は未改変。保留扱いの指摘 (KMP 復元・契約確定・コミット方針・Suggestion 3 件) は指示どおりレビュー対象から外した。

Critical / Major は無し。新規に見つかったのは優先度の低い Minor 3 件と Suggestion 2 件のみのため APPROVED とする。ただし **Minor 2 (蒸留への申し送り文が修正前の事実のまま)** は長命層へ流れ込む文面なので、蒸留に入る前に直しておくのが望ましい。

## 各修正の再確認結果

| # | 指摘 | 判定 | 根拠 |
|---|---|---|---|
| 1 | review-001 Major 1 (iOS の宙吊り) | **解消** | `DialogContainerViewController.viewDidDisappear` で `resultChannel.settle(.cancelled)`。`DialogTestPresentationSurface.dismiss` が「自分と自分より上の器を外し、外した器へ出現状態の遷移を伝える」という UIKit の連鎖 dismiss を模しており、`MD_b_closeFromBottom` が `topTask.value == .cancelled` と器 0 枚まで主張する。`DialogContainerViewControllerTests` に確定済みなら変わらないことの検証もある |
| 2 | review-001 Major 1 (Android 同型) | **解消** (検証は Minor 3 参照) | `DialogContainer.init` に `setOnDismissListener { reportDetached() }` を追加。`外の要因で器が閉じた show は cancelled で完了する` で提示面からの外部離脱 → `DialogResult.Cancelled` を確認 |
| 3 | review-001 Major 2 / codex Major 3 (MAUI Android Bridge) | **解消** | `reportClosure` を `@JvmSynthetic internal suspend fun` として切り出し、`catch (cancellation: CancellationException) { throw }` → `catch (failure: Throwable) { listener.onFailed(...) }` の順で catch-all を追加。新設 5 件が完了 / キャンセル / 提示先不在 / 中身生成の例外 / コルーチンのキャンセルを覆う。キャンセルが通知に変換されないことも主張している |
| 4 | codex Major 2 (iOS の非アクティブ window 採用) | **解消** | `selectKeyWindow(from:)` を `DialogWindowSceneSnapshot` を引数に取る静的関数として切り出し、`filter(\.isForegroundActive).flatMap(\.windows).first(where: \.isKeyWindow)` へ限定。フォールバックは撤去済み。テスト 4 件 (アクティブの key window / 非アクティブ / key window 無し / シーン無し) |
| 5 | codex Major 4 (MauiContext 解決の任意スレッド契約) | **解消** | Android / iOS 両方の `PresentAsync` が `MainThread.InvokeOnMainThreadAsync` の中で文脈解決と `Present` を行う。文脈選択も Native 側が選ぶ host (Android = `ActivityStateManager.Default.GetCurrentActivity()` / iOS = foregroundActive シーンの key window) と一致する `Window` を優先する形に変わっている。`verification-matrix.md` に実機でのワーカースレッド show 実測記録あり |
| 6 | review-001 Minor 1 (iOS Task キャンセル) | **解消** | `withTaskCancellationHandler` の `onCancel` で `settle(.cancelled)`。`cancellingCallerSettlesCancelled` が `showTask.value == .cancelled` と器 0 枚を主張。unstructured `Task {}` はキャンセルを継承しないため閉鎖処理も走る |
| 7 | review-001 Minor 2 (KMP テストの nil factory) | **解消** | `newProbeViewHandle()` が実体の `UIView` を生成し `interpretObjCPointer` で handle 化、実体は `createdProbeViews` が保持。`互換面へ渡す View factory は実体のある View を返す` が ObjC クラス名 `UIView` まで主張する |
| 8 | codex Minor (Swift 5 モード) | **解消** | `project.pbxproj` の Debug / Release とも `SWIFT_VERSION = 6.0`。`xcodebuild -configuration Release` で **BUILD SUCCEEDED**、警告は library evolution の 3 件のみ (Swift 6 の分離・Sendable 警告なし) |

## 指摘事項

### [🟡 Minor] iOS の器が「上に全画面提示が乗った」だけでも cancelled になる

**該当箇所**: `ios/Sources/KsDialogs/Presentation/DialogContainerViewController.swift:53-59`

**問題点**:
`viewDidDisappear` は「自分が閉じられた」ときだけでなく、**自分の上に `.fullScreen` の ViewController が提示されたとき**にも呼ばれる。コメントは自ライブラリの `.overFullScreen` 提示ではここを通らないことを正しく書いているが、覆っていないのは**ライブラリ外からの全画面提示**である。

ダイアログ表示中に利用者コードが全画面の画面 (既定が `.fullScreen` の `UIImagePickerController` など) を提示すると:

1. 器の `viewDidDisappear` → `settle(.cancelled)` で、まだ生きているダイアログが cancelled になる
2. 続けて `DialogPresenter` の閉鎖経路が走り、`container.presentingViewController?.dismiss(animated:)` が **利用者が出した全画面の画面ごと閉じる**

縦串の範囲 (ダイアログ 1 種・全画面提示なし) では踏まないため実害は出ていないが、手当てが持ち込んだ新しい経路ではある。

**推奨修正**:
`viewDidDisappear` で「実際に閉じられた」ことを確かめてから確定させる (例: `presentingViewController == nil` / `isBeingDismissed` を条件にする)。既存テストは提示元を持たない器で出現状態を遷移させているため、どちらの条件でも通る。

---

### [🟡 Minor] 蒸留への申し送り文が修正前の事実 (iOS の宙吊り) のまま残っている

**該当箇所**: `kasane/changes/add-vertical-slice/common-spec-scenarios.md:121-127`

**問題点**:
MD-b の記録欄 (96 行目) と期待値確定の判断欄 (99 行目) には「手当て後: 宙吊りは解消し、上の show は cancelled で返る」が正しく追記されている。一方、その下の **「tasks 7.4 でまとまった追記候補」** は修正前のままで、

- `iOS: 下の1枚へ先に結果報告すると上下とも器が消え、上の show は完了しないまま残る (宙吊り)`
- `片側 (iOS) に呼び出し側が await から戻れない実害があるため`

と書かれている。この節は **concepts [多段表示のルール] への追記候補**、つまり長命層へ流れ込む文面なので、このまま蒸留に入ると既に解消済みの事実が永続層へ入る。同じファイル内で 99 行目と矛盾してもいる。

**推奨修正**:
追記候補 1 を手当て後の事実へ更新する — 残る OS 差は「iOS は上も一緒に閉じて cancelled / Android は上が残って操作を続けられる」であり、ADR 級の判断が要るという結論自体は変わらない (実害の根拠が「宙吊り」から「意図しない cancelled」に変わる)。

---

### [🟡 Minor] Android 側の手当ての結線が自動テスト・実機のどちらでも未検証

**該当箇所**: `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/DialogContainer.kt:34`

**問題点**:
手当ての実体は `setOnDismissListener { reportDetached() }` の 1 行だが、この**結線そのもの**を通る検証がない。

- 自動テスト: `android/ksdialogs/build.gradle.kts:42` の `isReturnDefaultValues = true` により `android.app.Dialog.setOnDismissListener` はスタブで、リスナは発火しない。テスト用提示面 (`DialogTestPresentationSurface.detach`) も `container.reportDetached()` を**直接**呼ぶため、検証されているのは `reportDetached` の中身だけ
- 実機: `common-spec-scenarios.md:97` に「手当て後のエミュレータ再観測は未実施」と明記されている

iOS 側は `beginAppearanceTransition` / `endAppearanceTransition` で実装の `viewDidDisappear` が実際に走るため結線まで検証されており、**2 OS で検証の深さが非対称**になっている。

**推奨修正**:
次の手動確認の機会に、Activity 破棄などで器だけが消える経路を Android エミュレータで 1 回観測して記録する (`verification-matrix.md` は既に再観測を申し送っているので、その対象に本経路を含める)。ユニットテストで結線まで見るなら Robolectric の導入が要るため、実測での裏取りのほうが安い。

---

### [🔵 Suggestion] `reportClosure` の exactly-once は通知先自身が例外を投げた場合に破れる

**該当箇所**: `maui/android/native/ksdialogs-maui-bridge/src/main/kotlin/jp/kamusoft/ksdialogs/maui/MauiDialogBridge.kt:80-92`

`listener.onDismissed()` / `onCancelled()` の呼び出しが `try` の内側にあるため、通知先 (JNI 越しの C# 実装) が例外を投げると catch-all が拾って `onFailed` を追加で呼び、テスト名が主張する「ちょうど1回」が破れる。現在の C# 側 `ClosureListener` は `TrySetResult` / `TrySetException` だけで実際には投げないため実害はない。気にするなら `show()` の結果を先に確定させてから try の外で 1 回だけ通知する形にすると構造として保証できる。

---

### [🔵 Suggestion] 修正サイクルの記録行が 2 件ぶん `verification-matrix.md` に無い

**該当箇所**: `kasane/changes/add-vertical-slice/verification-matrix.md:125-131` 付近

iOS の手当て・MAUI の文脈解決・Android 互換面の catch-all は実績メモに行が追加されているが、**KMP 互換面テストの factory 修正 (Minor 2)** と **MAUI iOS Bridge の Swift 6 化 (codex Minor)** の行がない。証跡の網羅性のため 1 行ずつ足しておくと、蒸留時に「何をどう直したか」が表だけで追える。

## 確認した観点 (指摘なし)

- **ビルドとテスト (全ルート再実行)**: ios 34 (`xcodebuild test` / iPhone 17 Pro Simulator、9 suites 全通過) / android 26 (`./gradlew test --rerun-tasks`、XML 集計 tests=26 failures=0) / kmp 32 (`allTests --rerun-tasks`、tests=32 failures=0) / maui 19 (`dotnet test`、失敗 0) / MAUI Android Bridge 5 (`maui/android/native && ./gradlew test --rerun-tasks`、tests=5 failures=0)。期待件数と完全一致
- **MAUI iOS Bridge の Swift 6 ビルド**: `xcodebuild -project KsDialogsMauiBridge.xcodeproj -scheme KsDialogsMauiBridge -destination 'generic/platform=iOS Simulator' -configuration Release build` が **BUILD SUCCEEDED**。actor 分離 / Sendable 由来の警告は 0 件で、`SWIFT_VERSION = 5.0` を外したことによる退行はない
- **負のコンパイル検証**: `dotnet build KsDialogs.Maui.Tests -p:KsDialogsNegativeCompileCheck=true` が CS0029 / CS1503 / CS0311 の 3 件で失敗することを再実測。修正で緩んでいない
- **コメント規約**: `python3 scripts/comment-policy-lint.py --summary` が 229 ファイル / 禁止 0 件 (前回の 224 ファイルから新規 5 ファイルぶん増、いずれも適合)
- **足場アーティファクトの改変**: `proposal.md` / `design.md` / `specs/` 6 件はいずれも review-001 以前の更新時刻のまま未改変。修正サイクルで書き換えられたのは実績欄を持つ `common-spec-scenarios.md` / `verification-matrix.md` のみで規約どおり
- **保留指摘の扱い**: KMP `DialogGateway.kt:47-50` の unchecked cast、iOS 互換面 `KsDialogsInteropNotifier.complete(_ value: Any)`、`.swiftpm-locks` / `samples/kmp` 生成物はいずれも未修正のまま。オーナー判断待ちの指示どおりで、勝手に別方針の実装が入ってもいない
- **exactly-once の維持**: iOS は `viewDidDisappear` / `onCancel` / notifier の 3 経路がすべて `DialogResultChannel.settle` のロック + `isSettled` を通り、確定済みなら no-op。Android も同型。修正で報告経路が増えたが二重確定は構造的に起きない
- **キャンセル時のリソース解放**: iOS は unstructured `Task {}` がキャンセルを継承しないため、キャンセル経路でも `presentationSurface.dismiss(container)` が確実に走る (テストが器 0 枚まで主張)。Android は従来どおり `finally { presented.dismiss() }`
- **MAUI 文脈解決の退行**: `ResolveMauiContext` は host 一致が取れないときだけ「文脈を持つ最初の画面」へ落ち、どちらも取れなければ `null` → `PresentationHostUnavailable` を投げて View を作らない。「提示先が無ければ即失敗し View を作らない」契約は維持されている
- **実挙動記録の誠実さ**: `common-spec-scenarios.md` の MD-b は手当て後の実機再観測を「未実施」と明記した上で、根拠を自動テスト名で示している。観測していないことを観測したことにしていない (この点は良い記録の仕方)

## アクションプラン

1. **Minor 2 (申し送り文の更新)** — 蒸留の入力になるため、蒸留着手前に直すのが望ましい。編集は数行
2. **Minor 1 (iOS の viewDidDisappear の条件付け)** — 縦串では踏まないが、修正コストが小さく退行リスクも低い。本変更で入れても phase-5 へ送っても可
3. **Minor 3 (Android 結線の実測)** — 次の手動確認の機会に MD-b 再観測と併せて 1 回観測する。`verification-matrix.md` の申し送りへ含める
4. Suggestion 2 件は任意。#5 (記録行の追加) は証跡の網羅性のため蒸留前に足しておくと安い

---

## Minor 修正の確認 (追記)

**日付**: 2026-08-15 / **判定**: APPROVED (据え置き — 冒頭の判定に変更なし)

Minor 1・Minor 2 の修正を独立に確認し、**いずれも解消**していることを確認した。iOS の自動テストは 35 件全通過 (`xcodebuild test` / iPhone 17 Pro Simulator、9 suites)。Minor 3 は指示どおり実機再観測の申し送りとして扱い、確認対象から外した。

### Minor 1 (全画面提示で cancelled 発火) — 解消

**該当箇所**: `ios/Sources/KsDialogs/Presentation/DialogContainerViewController.swift:53-70`

`viewDidDisappear` が「即座に確定」から「**次の main actor の機会に `presentingViewController == nil` を確かめてから確定**」へ変わった。器が提示の連なりに残っている (= 上に重なられただけ) 場合は確定させず、器が解放済みなら (`guard let self` の else) 呼び出し元を解放する。指摘した 2 つの実害 — 生きているダイアログの巻き添え cancelled と、利用者が出した全画面の画面まで閉じてしまう連鎖 dismiss — はどちらも起きなくなる。

**`isBeingDismissed` を使わない判断について: 妥当と評価する。** 理由は 2 点:

1. **併用できない**。`isBeingDismissed` は閉鎖遷移中にのみ true で、`viewDidDisappear` を抜けた後 (= 遅延させた次の機会) には既に false になっている。判定を遅延させる設計を採る以上、`isBeingDismissed` は判定材料として使えない
2. **覆う範囲が広い**。`isBeingDismissed` が拾えるのは「dismiss で閉じられた」経路だけで、review-001 Major 1 が名指しした「**提示元 ViewController が別の理由 (画面遷移など) で消えた**」経路は拾えない (この場合 `isBeingDismissed` は false のまま)。`presentingViewController == nil` は提示関係が解けたこと自体を見るため、こちらも同じ判定で拾える

つまり同期判定 (`isBeingDismissed`) にすると、指摘の穴は塞げても Major 1 の穴が一部戻る。遅延 + 提示関係の確認という選択は、その両方を 1 つの条件で満たす筋の通った判断になっている。コメント (58-60 行) にもこの意図が書かれている。

**テストは実質的**。新規の `上に別の画面が重なっただけでは結果は確定しない` は、**実際に `UIWindow` + root VC を用意して `present` した**上で `presentingViewController != nil` を `#require` で確認してから出現状態を遷移させ、300ms 待って**確定していないこと**を主張する。fake で条件を作らず実 UIKit の提示関係を通しており、判定条件そのものを検証している。既存 2 件も確定が非同期になったことに合わせて `waitUntil` で待つ形へ正しく直され、`確定済みの器が画面から外れても結果は変わらない` は「二重確定が起きないこと」を 300ms 待ってから主張する形になっている (待たずに `count == 1` を見るだけの見せかけの緑にはなっていない)。

**新たな問題**: 構造的な問題は見当たらない。`Task { @MainActor [weak self, resultChannel = ...] }` に循環参照はなく、覆われるたびに Task が 1 つ増えるだけで累積もしない。正常経路 (notifier 報告 → 閉鎖 → `viewDidDisappear`) は確定済みのため遅延判定が no-op になる順序も維持されている。残るのは下記の観測課題のみ。

### [🔵 Suggestion] 「閉じられた側」の判定は実 UIKit に対して未検証

**該当箇所**: `ios/Tests/KsDialogsTests/DialogContainerViewControllerTests.swift:60-88`

この修正は「閉鎖されたら次の main actor の機会までに `presentingViewController` が nil になっている」という UIKit の挙動に依存する。実運用ではそのとおりになるはずだが、テストで実 UIKit を通しているのは**重なられた側 (nil にならない)** だけで、**閉じられた側 (nil になる)** は提示関係を持たないテスト用提示面での検証にとどまる。仮にこの前提が崩れると、確定漏れ = review-001 Major 1 の宙吊りが戻る (今回の修正は「誤確定」と「確定漏れ」のトレードオフを後者寄りに動かしている)。

追加コストが小さいので、`coveringPresentationKeepsResultUnresolved` の末尾に続けて **実際に `dismiss(animated: false)` してから `waitUntil { resultChannel.isResultSettled }` を主張する**と、依存している前提そのものが固定できる。あるいは既に申し送り済みの MD-b の Simulator 再観測でこの経路を確認する (その場合、再観測は「あると望ましい」ではなく**この判定条件の唯一の実証**になるため、優先度を上げて扱うのが望ましい)。

### Minor 2 (蒸留申し送り文の陳腐化) — 解消

**該当箇所**: `kasane/changes/add-vertical-slice/common-spec-scenarios.md:121-133` / `94-99` / `verification-matrix.md:125`

追記候補 1 が手当て後の姿へ書き換えられ、指摘した矛盾は解消した。書き換えの質も良い:

- 「宙吊り」を「上の show は **cancelled で確定する** (利用者が触っていないダイアログが、下を閉じた巻き添えで cancelled になる)」へ差し替え、**手当て前の姿も括弧書きで残している**ため、経緯が読める
- 実害の根拠を「await から戻れない」から「同じ操作で観察結果が割れる」へ正しく置き換えたうえで、**ADR 級の判断という結論は維持**している (修正で結論まで薄めていない)
- 「上記の手当ては契約の先取りではなく『結果が返らない状態を作らない』ための最低限の措置」と明記されており、オーナー判断待ちであることが後から読んでも分かる

MD-b 記録欄 (96 行) と `verification-matrix.md` の実績メモ (125 行) も「上に別の画面が全画面で重なっただけの場合は確定させない」「判定は提示関係の解除が済む次の機会に行う」まで追随済みで、3 箇所の記述が揃っている。

**nit**: `verification-matrix.md:125` の「ios の自動テストは 27 件 → 34 件」は、本修正でテストが 1 件増えたため **35 件**が正しい。次に同ファイルへ追記する機会に合わせて直せば足りる (review-002 の Suggestion 5 で挙げた KMP factory 修正・Swift 6 化の記録行の追加と同時が効率的)。

---

## Android 手当て2周目の確認 (追記)

**日付**: 2026-08-15 / **判定**: APPROVED (据え置き — 冒頭の判定に変更なし)

Activity 破棄経路の手当てを独立に確認し、**構造・テストとも指摘を解消している**ことを確認した。再実行は android 34 / kmp 32 いずれも失敗 0 (`./gradlew test --rerun-tasks`)、コメント規約 lint も 231 ファイル / 禁止 0 件。Major は無し。記録の追随漏れが Minor 1 件、設計上の注意が Suggestion 2 件。

### (1) Activity 破棄経路で cancelled が確定する構造 — 解消

確定の契機が 1 経路から **3 経路**へ増え、`DialogResultChannel.settle` に集約されている。

| 契機 | 経路 | 拾う状況 |
|---|---|---|
| 閉鎖の通知 | `setOnDismissListener { reportDetached() }` | ライブラリ自身が閉じたとき |
| 画面破棄の購読 | `ResumedActivityTracker.onActivityDestroyed` → `takeObservations` → `container.closeOnHostDestroyed()` | 画面回転などの Activity 破棄 (今回の未解消経路) |
| ウィンドウの取り外し | `window.decorView` の `OnAttachStateChangeListener.onViewDetachedFromWindow` | 上記のどちらも通らずウィンドウだけが外れたとき |

**要は `closeOnHostDestroyed` の `finally`** (`DialogContainer.kt:90-98)。破棄後の `dismiss()` は `IllegalArgumentException` (View not attached to window manager) で失敗しうるが、`finally { reportDetached() }` により**閉鎖の成否と無関係に確定が起きる**。実測で見つかった「`Dialog.dismiss()` が呼ばれないので `setOnDismissListener` が発火しない」という原因に対して、通知に依存しない確定経路を足しており、対処が原因に対応している。`catch` の範囲も `IllegalArgumentException` に絞られていて広すぎない。

購読の解除も漏れがない — `DialogPresenter.present` の `finally { presented.dismiss() }` が結果・キャンセル・例外のいずれの終了でも走り、`PresentedDialog { registration.cancel(); container.dismiss() }` で解除される。破棄側は `takeObservations` が該当分を一覧から外し、**参照が切れた購読も同時に片付ける**ため取り残しも溜まらない。

### (2) 新たな問題 (リーク・二重発火・レース) — 構造的な問題なし

- **二重発火なし**: 3 経路すべてが `settle` を通り、`isSettled` によるロック付きの冪等確定。実際、破棄経路では `closeOnHostDestroyed` の `dismiss()` → 閉鎖通知 → decorView 取り外し → `finally` と最大 3 回 `settle` が走りうるが、有効なのは最初の 1 回だけで exactly-once は維持される
- **デッドロック・ConcurrentModificationException なし**: `takeObservations` はロック内で `filter` (コピー生成) と `removeAll` を済ませ、**通知はロックの外**で行う (`ResumedActivityTracker.kt:59-60` にその意図が明記)。`Dispatchers.Main.immediate` は UI スレッド上で `continuation.resume` を同期再開しうるため、通知の中から `registration.cancel()` が同一スタックで再入する経路が実在するが、その時点でロックは解放済みかつ反復対象は別リストなので、どちらの問題も起きない。**再入を意識した書き方になっている**
- **decorView リスナの解除**: 明示的な解除はないが、decorView は Dialog の window の所有物で Dialog と一緒に解放されるため残留しない
- **`show()` と購読登録の順序** (`ActivityDialogPresentationSurface.kt:24-26`): `container.show()` が購読登録より先。`show()` が例外で抜けた場合は購読も `presented.dismiss()` も走らないが、例外は `DialogPresenter` の try より前で呼び出し元へ伝播するため宙吊りにはならない。提示も lifecycle 通知も UI スレッドなので、この 2 行の間に破棄が割り込むこともない

### [🔵 Suggestion] 破棄購読が Activity を実質的に強参照している (KDoc の主張とずれる)

**該当箇所**: `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/ResumedActivityTracker.kt:8-14, 41-47, 89-92`

`DestroyObservation` は Activity を `WeakReference` で持つが、**`onDestroyed` のラムダは `container` を強参照**し、`DialogContainer` は `android.app.Dialog` として `context = activity` を強参照する。結果として

`ResumedActivityTracker.shared` → `destroyObservations` → ラムダ → `DialogContainer` → `Activity`

という強参照鎖ができ、クラス KDoc の「Activity は弱参照で保持し、追跡が画面の寿命を延ばさないようにする」は**この経路には当てはまらない**。

ただし解除が (a) show の終了時 (`finally` 経由) と (b) 当該 Activity の破棄時 (`takeObservations`) の両方で確実に行われるため、**実害のあるリークではない** (保持されるのはダイアログが出ている間だけで、これは元々 Activity が生きている期間)。KDoc に「破棄の購読は解除されるまで購読者ごと保持する」の一言を足すか、`DialogContainer` 側も弱参照で持つかは任意。指摘は正確性の問題であって修正必須ではない。

### [🔵 Suggestion] 購読解除を見ているつもりの surface テスト 1 件に弁別力がない

**該当箇所**: `android/ksdialogs/src/test/kotlin/jp/kamusoft/ksdialogs/ActivityDialogPresentationSurfaceTests.kt:67-78`

`結果を確定して閉じた後の画面破棄は結果を変えない` は、**購読解除が効いていなくても** `settle` の冪等性だけで通る (破棄通知が届いても結果は変わらないため)。名前が示す「解除が効いている」ことは実際には見ていない。

解除自体は `ResumedActivityTrackerTests` の `購読を解除すると画面の破棄は届かない` が **通知カウントの増減**で弁別的に検証しているため穴ではないが、この 1 件は他 3 件ほどの検証力を持たない。強めるなら surface 側でも通知回数を数える差し替えを噛ませる形になる。

### (3) テストの実質性 — 結線まで検証している

前回 Minor 3 で「結線が自動テスト・実機のどちらでも未検証」と指摘した点は、**破棄経路については解消**した。

- `DialogContainerTests` の新規 `提示先の画面が破棄された show は cancelled で完了する` は、**器の出し入れを差し替えず**に提示面 (`ActivityDialogPresentationSurface`) + 追跡役 (`ResumedActivityTracker`) + 器 (`DialogContainer`) の実物をつなぎ、`tracker.onActivityDestroyed` から `show` の戻り値まで通す。しかも `withTimeout(SHOW_COMPLETION_TIMEOUT_MILLIS)` を掛けているため、**確定漏れの退行はハングではなくテスト失敗になる** — 宙吊りを検出するテストとして正しい形
- `ActivityDialogPresentationSurfaceTests` 4 件は購読の登録から確定までを surface 単位で覆い、確定済みの保持と**別画面の破棄では確定しないこと** (購読の宛先照合) まで見ている
- `ResumedActivityTrackerTests` の +3 件は購読・解除・別画面の弁別をカウントで主張しており、解除の検証として有効

なお `closeOnHostDestroyed` の `dismiss()` はテスト環境ではスタブの no-op になるが、**確定は `finally` で無条件に起きる**設計なので、テストの緑が本番の保証をそのまま反映している (スタブに助けられた緑ではない)。

### [🟡 Minor] 2周目の修正に記録が追随していない (前回 Minor 2 と同型)

**該当箇所**: `kasane/changes/add-vertical-slice/verification-matrix.md:136` / `common-spec-scenarios.md:97`

修正内容そのものは `common-spec-scenarios.md:97` の「手当て後」段落に「閉鎖の通知・提示先の画面の破棄の購読・ウィンドウの取り外し検知の3経路から確定させる」と正しく反映されている。一方で、**今回の修正のきっかけになった観測記録が未修正のまま**で、同じファイル・同じ表の中で結論が食い違う。

1. `verification-matrix.md:136` の末尾 **「修正は行っていない (オーナー / phase 判断待ち)」は今回の修正で偽になった**。かつ 2 周目の修正を記録する行が追加されていない (136 行が最終データ行)
2. `common-spec-scenarios.md:97` の器の閉鎖通知の実機観測段落は **「この経路では宙吊りが残る」で終わっており**、手当て後の追記がない。同じセルの前段が 3 経路の手当てを説明しているため、読み手はどちらが現状か判断できない

**推奨修正**: 136 行の「修正は行っていない」を修正済みへ改め、2 周目の実績行 (破棄購読 + 取り外し検知の追加、android 26 件 → 34 件、根拠テスト名、**画面回転での実機再観測が未実施であること**) を 1 行足す。`common-spec-scenarios.md:97` にも「2 周目の手当てで解消 (根拠は自動テスト)。回転での実機再観測は未実施」を追記する。

**この Minor は特に優先度が高い**。今回の修正は「実機で見つかった宙吊り」への対処だが、**その修正自体の実機再観測 (画面回転のやり直し) はまだ行われていない**。自動テストは Android フレームワークのスタブ上で動くため、`onActivityDestroyed` が実機で期待どおりのタイミングで届くこと・`dismiss()` の失敗を `IllegalArgumentException` で拾えることは**実測されていない**。記録がこの未検証状態を明示していないと、次の担当者が「実機で直っている」と誤読する余地が残る。

### アクションプラン (追記分)

1. **Minor (記録の追随)** — `verification-matrix.md:136` の訂正 + 2 周目の実績行の追加、`common-spec-scenarios.md:97` への追記。蒸留前に必須
2. **画面回転での実機再観測** — 今回の修正の唯一の実証手段。次の手動確認の機会に、宙吊りが解消していることと `WindowLeaked` が出なくなったことを確認する
3. Suggestion 2 件 (KDoc の正確性 / surface テストの弁別力) は任意

---

## KMP 復元検査 (方式 b) の確認 (追記)

**日付**: 2026-08-15 / **判定**: APPROVED (据え置き — 冒頭の判定に変更なし)

review-001 Major 3 / codex Major 1 (KMP 型消去復元の失敗検出) が、オーナー選択の方式 (b) で**解消**していることを確認した。再実行は **ios 38 / kmp 33 いずれも失敗 0**、コメント規約 lint 233 ファイル / 禁止 0 件。これで review-001・second-opinion-code-001 の突き合わせ表で「修正サイクル対象」「NEEDS_DISCUSSION」に分類された指摘のうち、コードに関わるものはすべて解消した。Major は無し、新規は Suggestion 2 件。

### (1) spec の SHALL 充足 — 満たす

`specs/kmp-facade/spec.md:32` の「iosMain actual は … 復元を担い、**復元失敗**と構成エラーは Kotlin 例外 → NSError 変換で Swift 側に届く SHALL」に対し、経路が最後までつながっていることを確認した。

| 段 | 実装 | 状態 |
|---|---|---|
| 検査 | `KsDialogsInteropNotifier.complete` が登録時の `resultType.accepts(value)` で判定 | 不一致なら `KsDialogsInteropResultTypeMismatch` の印で `settle(.completed(...))` |
| 振替 | `KsDialogsInteropResult.init(outcome:)` が印を検出して `init(error:)` へ | `kind = .error` / `value = nil` / `error = NSError(DialogError.resultTypeMismatch)` |
| 変換 | `IosDialogGateway.outcomeOf` の `KSDInteropDialogResultKindError` 分岐 | `throw DialogException(error.localizedDescription)` |
| 送出 | `DialogGateway.show` の `@Throws(DialogException::class, CancellationException::class)` | Swift 側へ NSError として届く |

`DialogError` が `LocalizedError` に準拠しているため、`localizedDescription` に「結果値の型が一致しません (期待: X / 実際: Y)。」が載って Swift 側まで原因が読める。**「復元失敗が検出されない」という指摘の実体は無くなっている**。

方式 (b) の据え方も妥当で、検査に必要な「宣言結果型」を **ObjC 面に出せる形 (名前 + 判定手続きの組)** に落とし込んでおり、ジェネリクスを ObjC 境界に出せないという design Decision 12 の制約と衝突していない。

### (2) 契約整合 — 崩れなし

- **cancelled に化けない**: 不一致は `.cancelled` を一切通らず、`.completed(印)` → `.error` と流れる。`interopMismatchedResultValueReportsError` が `kind == .error` と `value == nil` の両方を主張しており、「不一致がキャンセル扱いになる」退行はテストで固定されている
- **ダイアログは閉じる**: 印も結果チャネルの確定なので `DialogPresenter` の閉鎖経路がそのまま走る。同テストが `waitForPresentedContainers(count: 0)` まで見ており、**不一致でダイアログが残る**退行も塞がれている。結果チャネルに値を通す (throw で割り込まない) という設計選択がここで効いている
- **exactly-once**: 印の確定も `DialogResultChannel.settle` の冪等確定を通る。`interopMismatchKeepsExactlyOnceGuarantee` が不一致確定後の `complete(true)` / `cancel()` を無効化することを 300ms 待って主張している
- **構成エラーとの弁別**: 未登録・提示先不在は `show` の `catch` 経由、不一致は outcome 経由で、どちらも `.error` に集まる。判別は同じだが `localizedDescription` で区別でき、KMP 側テストも実測ログ付きでこの区別を主張している。輸送区分を増やしていないぶん互換面が単純に保たれている
- **公開 API 不変**: `Contract/` 6 ファイル・`Registry/` 3 ファイル・`Presentation/Dialog.swift` はいずれも 08-14 の更新時刻のまま**未改変**で、iOS Native 単体の型付き経路 (`value as? ViewModel.Result` → `DialogError.resultTypeMismatch`) も `DialogNotifier<R>` も変わっていない。変更は `Interop/` 内に閉じており、`specs/kmp-facade/spec.md` も未改変。互換面の登録シグネチャ変更は破壊的だが `KSDInterop*` の内部境界であり、design Decision 12 の分離どおり。`samples/kmp` の登録側も追随済み

### (3) 境界テスト — 実質的

- `宣言結果型と合わない結果値の報告は失敗として返る`: 誤型 (`"文字列"`) を報告し、`error as? DialogError == .resultTypeMismatch(expected: "Bool", actual: "String")` と**期待値・実際値の中身まで**主張。error 判別が出たことだけを見る緩いテストになっていない
- `nil を包んだ結果値の報告も失敗として返る`: `Bool?` の nil を `Any` として報告し、`actual: "Optional<Bool>"` を主張。**nil が `$0 is Bool` を素通りしない**ことの直接の証拠になっており、ObjC 境界で最も漏れやすい形を突いている
- `型が合わない報告で確定した後の再報告は無効`: 上述のとおり exactly-once を待ち合わせ付きで固定
- KMP 側 `登録時に渡した宣言結果型が合う値と合わない値を境界越しに判別する`: `KSDInteropDialogResultType.acceptsValue` を **cinterop 越しに** Kotlin から叩き、`true`/`false` を受理し `"文字列"`/`1` を拒否することを確認。判定手続きが ObjC 境界を跨いでも機能することの実測になっている

**未検証の範囲も正しく申告されている**。KMP 側の end-to-end (不一致の報告 → 共有コードで `DialogException`) は、提示先の画面を持たないテストランナーでは結果報告まで到達できないため覆えていない。`verification-matrix.md:114` がこの分担 (「報告時点の検査そのものは iOS 側のテストが受け持つ」) を明記しており、覆えていない範囲を覆ったことにしていない。なお `.error` → `DialogException` の変換自体は既存の未登録 ViewModel のテストが同じ分岐を通るため、経路は 2 つの半分で覆われている。

### [🔵 Suggestion] 宣言結果型は登録側の自己申告で、共有コードの `R` と機械的に結びついていない

**該当箇所**: `ios/Sources/KsDialogs/Interop/KsDialogsInteropBridge.swift:37-51` / `samples/kmp/iosApp/KsDialogsSampleKmp/SampleDialogRegistration.swift:15-17`

方式 (b) は「登録側が宣言した結果型」を正として検査する。したがって **Swift 側が誤った型を宣言した場合 (共有コードの `DialogViewModel<String>` に対して `name: "Bool") { $0 is Bool }` と登録した場合)、誤った値が検査を通過し、`DialogGateway.kt` の `outcome.value as R` が元どおりの穴になる**。方式 (b) を選んだ以上これは構造的な限界であって実装の欠陥ではないが、**利用者が守るべき前提**が生まれている。

現状この前提はコードコメント (「その ViewModel の宣言結果型」) にしか書かれておらず、`verification-matrix.md:114` にも前提としては記載がない。一般公開予定のライブラリなので、蒸留のときに `kmp/ADR-0002` か concepts へ「互換面へ登録する結果型は共有コード側の宣言と一致させること。一致は機械的に検査されない」を 1 行残しておくのが望ましい。

### [🔵 Suggestion] 不一致の印が Swift 型付き入口へ漏れると原因が読めなくなる

**該当箇所**: `ios/Sources/KsDialogs/Interop/KsDialogsInteropResultTypeMismatch.swift` / `ios/Sources/KsDialogs/Presentation/Dialog.swift:34-41`

互換面と型付き入口は同じレジストリを共有する (既存テスト `ObjC 互換面経由の呼び出しが同一レジストリに到達する` が両入口からの解決を実際に通している)。そのため、**互換面で登録した factory を Swift の `Dialog.show` から出して不一致が起きると**、印が型付き入口へ渡り

`DialogError.resultTypeMismatch(expected: "<VM.Result>", actual: "KsDialogsInteropResultTypeMismatch")`

になる。失敗にはなるので宙吊りや誤成功にはならないが、**`actual` に内部の印の型名が出て、実際に何が報告されたのかが読めない**。`Dialog.show` 側でも印を先に判定し、印が持つ `expected` / `actual` でそのまま throw すれば原因が保たれる。縦串の Sample はこの組み合わせ (互換面登録 + Swift 型付き show) を踏まないため優先度は低く、phase-5 の API 表面の突き合わせで拾っても足りる。

### アクションプラン (追記分)

1. Suggestion 2 件はいずれも本変更で対応しなくてよい。**宣言結果型の前提** (1 件目) は蒸留で ADR / concepts へ 1 行残す候補、**印の漏れ** (2 件目) は phase-5 への申し送り候補
2. 既出の未解決事項に変更なし — 記録の追随 (Android 2 周目)、画面回転の実機再観測、「下から閉じたときの上の扱い」の契約確定、`.swiftpm-locks` / `samples/kmp` 生成物のコミット方針は引き続きオーナー判断・申し送り
