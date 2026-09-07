# レビュー結果: add-vertical-slice (001 回目)

**日付**: 2026-08-15
**判定**: CHANGES_REQUESTED

## サマリー

4形態 (iOS Native / Android Native / MAUI / KMP) すべてで契約が貫通し、全ビルドルートのテストが緑 (ios 27 / android 23 / kmp 31 / maui 18)、負のコンパイル検証も再実行して期待どおり失敗すること、コメント規約 lint 0 件を確認した。契約の型設計 (VM が結果型を宣言 → notifier と show の戻り値を導出) は 4 形態で一貫しており、レジストリ・使い捨てモデル・型消去輸送の分離も spec / design の意図に沿っている。テストは差し替え点を内部の継ぎ目に絞った良質な構成で、手抜きや実質スキップは見当たらなかった。

一方で、**「ダイアログが画面から消えても show が結果を返さない」経路が iOS に実在し (MD-b の実測記録どおり)、これは concepts『多段表示のルール』の保証2および dialog-contract の Requirement「型付き結果の show」に反する**。加えて MAUI Android Bridge が構成エラー以外の例外を取りこぼして呼び出し元を hang させる非対称と、KMP 側で spec が要求する「型消去輸送からの復元失敗の報告」が実装されていない点がある。Major 3 件のため CHANGES_REQUESTED とする。うち 2 件は設計判断を含むため、修正方針の確定にオーナー判断が要る (各指摘に明記)。

## 指摘事項

### [🟠 Major] 器が閉じても結果が確定せず、show が永久に返らない経路がある (iOS)

**該当箇所**: `ios/Sources/KsDialogs/Presentation/UIKitDialogPresentationSurface.swift:22-26` / `ios/Sources/KsDialogs/Presentation/DialogContainerViewController.swift` / `ios/Sources/KsDialogs/Presentation/DialogPresenter.swift:29-38`

**問題点**:
`dismiss` は `container.presentingViewController?.dismiss(animated:)` を呼ぶ。UIKit ではこれで **その container より手前に積まれている器もまとめて閉じる**。結果として、下の 1 枚を先に結果確定させると上の器も画面から消えるが、上の `DialogResultChannel` は未確定のままで、**上の show は永久に完了しない**。

この挙動は推測ではなく実測記録がある — `common-spec-scenarios.md` の MD-b 実挙動記録 (iOS Native)「上の show はその後も完了せず、上のダイアログの器も戻ってこない (宙吊り) — 15 秒待っても結果表示は変わらなかった」、および `ui/brief.md` の多段表示表。同記録自身も「iOS の宙吊りは呼び出し側が await から戻れないという実害がある」と書いている。

これが「保証しない挙動」の範囲に収まらない理由:

- concepts [多段表示のルール] の**保証する挙動2**「重なっていても、各 show はそれぞれ独立に結果を返す」に反する。同文書が「保証しない」としているのは**閉じる順序**であって、結果が返らないことではない
- `specs/dialog-contract` の Requirement「型付き結果の show」は「completed(結果値) または cancelled のいずれかを**ちょうど1回返して完了する** SHALL」と無条件に定めている

同じ穴は他経路にもある。iOS は提示元 ViewController が別の理由 (画面遷移など) で消えたときも同様で、Android 側も `DialogContainer` が `setOnDismissListener` を持たないため、Activity 破棄などライブラリ外の要因でウィンドウが閉じると結果が未確定のまま残る (`android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/DialogContainer.kt:26-33`)。

**推奨修正**:
最低限「宙吊りにしない」手当てを入れる — 器が画面から外れた時点で結果が未確定なら cancelled で確定させる。

- iOS: `DialogContainerViewController.viewDidDisappear` で `resultChannel.settle(.cancelled)` を呼ぶ (確定済みなら no-op なので通常経路に影響しない)
- Android: `DialogContainer` に `setOnDismissListener` を足し、同様に未確定なら cancelled で確定させる

そのうえで「下から閉じたときに上をどう扱うか」(Android の挙動へ寄せるか / 上から順以外を明示的に禁止するか) は契約の確定を伴うため**オーナー判断が要る**。`common-spec-scenarios.md` は確定を蒸留へ申し送っているが、**申し送るのは「どちらの契約にするか」であって、「show が返らないままにする」ことではない**。契約確定を待つ場合でも、本変更の範囲で cancelled 確定の手当ては入れられる。

---

### [🟠 Major] MAUI Android Bridge が構成エラー以外の例外を取りこぼし、show が hang する / プロセスが落ちる

**該当箇所**: `maui/android/native/ksdialogs-maui-bridge/src/main/kotlin/jp/kamusoft/ksdialogs/maui/MauiDialogBridge.kt:50-62`

**問題点**:
`scope.launch { ... }` の catch は `DialogException.PresentationHostUnavailable` と `DialogException` の 2 つだけ。中身の View 生成 (C# 側の `ToPlatform`) やその他の経路で `DialogException` 以外の例外が出ると:

1. `listener` の 4 メソッドがどれも呼ばれない → C# 側 `PlatformDialogGateway.PresentAsync` の `Task.WhenAny(resultChannel.Result, failure.Task)` が永久に待ち、**利用者の `ShowAsync` が完了しない**
2. `CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)` で起動した coroutine の未処理例外は既定のハンドラへ渡り、Android では**プロセスクラッシュ**になる

これは仮想の話ではなく、`verification-matrix.md` の実績メモに「Android = 中身の生成時に `MauiMaterialButton` のテーマ要求違反でプロセスごと落ちる」と同型の障害が記録されている。文脈解決の修正でその1件は解消したが、**取りこぼしの構造自体は残っている**。

iOS 側 Bridge (`maui/macios/native/KsDialogsMauiBridge/MauiDialogBridge.swift:57-59`) は `catch { boxedCompletion.value(MauiDialogClosure(error: error)) }` で全ての error を `.failed` として返しており、C# 側も `OnClosed` の `Failed` で faulted Task にできている。**同じ層の同じ責務で 2 OS の堅牢性が非対称**になっている。

**推奨修正**:
catch 節の末尾に catch-all を足し、iOS と同じく `onFailed` へ落とす。`CancellationException` は再スローして structured concurrency を壊さないこと。

```kotlin
} catch (cancellation: CancellationException) {
    throw cancellation
} catch (failure: Throwable) {
    listener.onFailed(failure.message)
}
```

---

### [🟠 Major] KMP で型消去輸送からの復元失敗が検出されず、spec の要求を満たしていない

**該当箇所**: `kmp/ksdialogs-kmp/src/commonMain/kotlin/jp/kamusoft/ksdialogs/kmp/DialogGateway.kt:47-50`

**問題点**:
`specs/kmp-facade` の Requirement「Swift async からの直接呼び出し」は「iosMain actual は iOS Native の型消去輸送から VM の宣言結果型への**復元を担い、復元失敗**と構成エラーは Kotlin 例外 → NSError 変換で Swift 側に届く SHALL」と定め、design Decision 12 も「復元失敗は Decision 2 のエラーチャネルで報告する」としている。

実装は `@Suppress("UNCHECKED_CAST") DialogResult.Completed(outcome.value as R)` のみで、`R` は実行時に消去されるため**復元失敗は検出されない**。不整合な値はそのまま `DialogResult.Completed<R>` に包まれて呼び出し元へ渡り、実際に値を使う箇所で無関係な `ClassCastException` になるか、Swift 側 (ジェネリクスが消えて `AnyObject?` で届く) では最後まで検出されない。

コメントは「結果値は ViewModel が宣言した結果型に固定された報告口からしか入らない」としているが、これは Android 経路の説明であって **iOS 経路には当てはまらない**。iOS 側の互換面 `KsDialogsInteropNotifier.complete(_ value: Any)` は型指定を持たず、Swift 側の factory は任意の型の値を報告できる (`ios/Sources/KsDialogs/Interop/KsDialogsInteropNotifier.swift:19-22`)。まさに design Decision 12 が想定した不整合経路である。

iOS Native 単体では `Dialog.show` が `value as? ViewModel.Result` を検査して `DialogError.resultTypeMismatch` を throw しており (`ios/Sources/KsDialogs/Presentation/Dialog.swift:35-40`)、**KMP 側だけが穴になっている**。

**推奨修正**:
実行時型情報を持たない `R` では素直な検査ができないため、方針の選択が要る (**オーナー判断が要る**):

- (a) `DialogViewModel<R>` 相当に結果値の検査手段を持たせ、iosMain actual で復元検査を行う
- (b) 検査責務を iOS Native 側の互換面へ移す (登録時に結果型を受け取り、`complete` の時点で検査する)
- (c) 「KMP iOS 経路では復元失敗を検出しない」ことを spec 側の事実に合わせて改める (足場は凍結されているため、spec を直すなら本レビューを起点にした合意が要る)

いずれにせよ現状は spec の SHALL に対する未充足であり、**そのまま完了扱いにはできない**。

---

### [🟡 Minor] iOS の show が Task キャンセルに応答しない (Android / KMP との非対称)

**該当箇所**: `ios/Sources/KsDialogs/Presentation/DialogPresenter.swift:29-38`

**問題点**:
`withCheckedContinuation` を使っているため、呼び出し元の Task がキャンセルされても continuation は resume されず、**show は戻らずダイアログも画面に残る**。Swift の構造化並行性では `.task` modifier による画面離脱時のキャンセルなど日常的に起きる操作である。

Android は `suspendCancellableCoroutine` + `finally { presented.dismiss() }` でキャンセル時にも器を残さない設計になっており (`android/.../DialogPresenter.kt:41-51`)、KMP iOS も `suspendCancellableCoroutine` を使い挙動をコメントで明示している (`kmp/.../IosDialogGateway.kt:32-42`)。**iOS Native だけが無防備かつ無記載**。

**推奨修正**:
`withTaskCancellationHandler` でキャンセル時に器を閉じて cancelled 確定させるか、少なくとも「Task キャンセルには応答しない」ことを `KsDialogs.show` の doc コメントに明記する。前者は上記 Major 1 の手当て (器が外れたら cancelled 確定) と同じ仕組みで実現できる。

---

### [🟡 Minor] 互換面テストが Swift 側の非 Optional 契約に反する値を渡している

**該当箇所**: `kmp/ksdialogs-kmp/src/iosTest/kotlin/jp/kamusoft/ksdialogs/kmp/InteropBridgeContractTests.kt:53`

**問題点**:
`factory = { _, _ -> null }` を渡しているが、Swift 側の受け口 `KsDialogsInteropBridge.registerViewFactory(forViewModelClass:factory:)` の factory 戻り値は**非 Optional の `UIView`** (`ios/Sources/KsDialogs/Interop/KsDialogsInteropBridge.swift:35-38`)。ObjC 境界の nullability が緩いため通っているだけで、現状 factory へ到達する前に提示先不在で失敗するから露見していない。

将来 UI シーンを持つ環境でこのテストを回すと、Swift の非 Optional に nil が入る未定義動作になる。テストの意図 (解決の成否だけを見る) は正しいので、返す値を実体のある `UIView` に替えるだけで済む。

**推奨修正**: `factory` が実際に呼ばれても壊れない値 (空の `UIView` 相当) を返すよう変更する。

---

### [🔵 Suggestion] Android Native の結果復元も unchecked cast のみ

**該当箇所**: `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/Dialog.kt:22-26`

Native 単体では結果値が型固定の `DialogNotifier<R>` からしか入らないため現状は安全で、コメントにもその前提が書かれている。iOS 側が明示検査を持つのに対する非対称なので、Major 3 の方針を決めるときに合わせて整理すると設計が揃う。

---

### [🔵 Suggestion] samples/kmp の生成物がコミット対象になっている

**該当箇所**: `samples/kmp/iosApp/KotlinMultiplatformLinkedPackage/` (`Package.swift` / `*.m` / `include/*.h`)

Kotlin の SwiftPM 連携が生成する中間物に見えるが、`.gitignore` に載っておらず untracked のまま残っている (`kmp/.swiftpm-locks/` は同梱の `.gitignore` で `swiftPMCheckout/` だけ除外している)。Xcode プロジェクトが参照するため意図的に追跡するのであれば `samples/kmp/README.md` に理由を1行残し、そうでなければ `.gitignore` へ追加するのが望ましい。

---

### [🔵 Suggestion] MAUI の `Register` だけ型引数を 2 つ書かせる

**該当箇所**: `maui/KsDialogs.Maui/Registry/DialogViewRegistry.cs:37-38`

`Register<TViewModel, TResult>(Func<TViewModel, DialogNotifier<TResult>, View>)` はラムダのパラメータ型を明示しないと推論できず、利用側は `Register((BasicDialogViewModel _, DialogNotifier<bool> n) => ...)` と書く必要がある。iOS (`register(VM.self) { vm, notifier in ... }`) / Android (`register(VM::class) { vm, notifier -> ... }`) と比べて書き味が落ちる。phase-5 の API 表面の突き合わせで、`IDialogViewModel<TResult>` から `TResult` を導く形 (VM 型だけを型引数に取るオーバーロード等) を検討する余地がある。

## 確認した観点 (指摘なし)

- **ビルドとテスト**: 4 ルート全件を実行し全て緑 — ios 27 (Swift Testing、`xcodebuild test` / iPhone 17 Pro Simulator) / android 23 (`./gradlew test --rerun-tasks`、XML の tests=23 failures=0) / kmp 31 (`allTests --rerun-tasks`、iosSimulatorArm64 16 + androidHostTest 15) / maui 18 (`dotnet test`)。`verification-matrix.md` 実績メモの件数と一致
- **負のコンパイル検証の再実行**: `dotnet build KsDialogs.Maui.Tests -p:KsDialogsNegativeCompileCheck=true` が CS0029 / CS1503 / CS0311 の 3 件で失敗することを実測。記録どおり検証手段として有効
- **コメント規約**: `python3 scripts/comment-policy-lint.py --summary` が 224 ファイル / 禁止 0 件、`--selftest` も全件 OK。ADR 参照は `<domain>/ADR-NNNN` 形式で統一されている
- **公開 API 面 (ライブラリの露出)**: BuildProbe 8 ファイルは 4 ルートすべてから削除済み。新規型の可視性は妥当で、内部専用の型 (`DialogOutcome` / `DialogResultChannel` / `DialogViewFactory` / `DialogViewModelKey` / `DialogContainer(ViewController)` / `DialogPresenter` / gateway 一式) はいずれも internal。cinterop のために public が必須な iOS 互換面は `KsDialogsInterop*` / `@objc(KSDInterop*)` で命名分離され、design Decision 12 の要求を満たしている。Android / KMP は `explicitApi()` を有効化済み
- **tasks.md のチェック**: 全 26 項目に対応する実装・記録の実体を確認し、虚偽のチェックは見当たらない。7.2〜7.5 も証跡 (`ui/verification/` の 34 枚)・シナリオ表の記録欄・検証対応表の実績メモで裏付けられている
- **足場アーティファクトの改変**: proposal / design / specs 6 件はいずれも未変更。変更されたのは実績欄を持つ tasks.md と ui/brief.md のみで、規約に沿っている
- **exactly-once の実装**: 4 形態すべてで結果チャネルがロック (または `TaskCompletionSource.TrySetResult`) で 1 回だけ確定し、ハンドラ登録前の報告も pending 保持で取りこぼさない構造になっている
- **使い捨てモデル**: notifier は show 1 回ごとに factory 引数として新規に渡され、同一 VM の重ね出しでも報告先が混ざらない (design Decision 1・11 と整合)
- **samples のパリティ**: 文言 (`こんにちは、KsDialogs!`) と SampleTheme の primary (`#2563EB` 相当) が 4 ルートで一致していることを抜き取りで確認。consumer 境界 (Local Swift Package / composite build / ProjectReference 1 本) も spec どおり
- **maui/ADR-0003**: Android 標準アイテム不成立の根拠が実測ログ (init script の Kotlin DSL コンパイル失敗) 付きで記録され、「モジュール構成とは無関係」の検証まで含んでいる。design Decision 7 の「結果込みで起票する」指示を満たしている

## アクションプラン

1. **Major 2 (MAUI Android Bridge の catch-all)** — 影響が大きく修正も局所的。最初に入れる
2. **Major 1 (器が閉じたときの cancelled 確定)** — iOS / Android の器に閉鎖検知を足し、宙吊りを解消する。「下から閉じたときに上をどう扱うか」の契約確定はオーナー判断を仰ぐ (手当てだけは先行して入れられる)
3. **Major 3 (KMP の復元失敗検出)** — (a)/(b)/(c) の方針をオーナーと決めてから実装。spec に手を入れる選択肢を含むため、実装着手前に合意が要る
4. **Minor 1 (iOS の Task キャンセル)** — Major 1 の手当てと同じ仕組みで解消できるため、まとめて対応するのが効率的
5. **Minor 2 (互換面テストの nil)** — 単独で即修正可能
6. Suggestion 3 件は本変更で対応せず、蒸留 / phase-5 への申し送りで足りる
