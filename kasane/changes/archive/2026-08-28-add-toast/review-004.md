# レビュー結果: add-toast (4 回目)

**日付**: 2026-08-28
**判定**: CHANGES_REQUESTED

## サマリー

2 周目修正の 4 件 (Android Toast の中身供給の防護 / Loading・Dialog の元例外の預かり / Dialog の notifier
attach 順序 / TS-CO-07 のインライン経路テスト) は、**いずれもコードとしては正しく、指摘の意図どおりに解けている**。
特に (a) は「C# 側で例外を境界の内側に閉じ込め → null → Kotlin で通常の例外に変換 → coordinator の
`catch (Exception)` → その 1 枚だけ破棄」という合流が経路上で完結していることを、生成バインディングと
coordinator の全呼び出し口まで辿って確認した。(b) は元の型・メッセージ・スタックが呼び出し元へ届く形になり、
review-003 が指摘した Loading 側の `PresentationHostUnavailable` の平坦化も解消している。混線も無い。
新規の欠陥は見つからなかった。

残る問題は 1 点で、**コードの中身ではなく検証の穴**である。今サイクルで書き換えたのは
MAUI の binding / 互換面という、この 4 ルート構成で唯一ユニットテストの射程外にある層なのに、
実アプリ (Sample) を通した証跡が 1 枚も取り直されていない。`ui/verification/` の MAUI 系画像は
すべて 2 周目修正より前の時刻で止まっている。プロジェクトの
`concepts/cross/conventions/runtime-behavior-verification.md` は、実行時にしか症状の出ない不具合の
修正について実環境での確認と証跡を完了条件に置いており、(a) の直した症状 (managed 例外が
`JavaProxyThrowable` として境界を越えプロセスを落とす) はまさにその型である。

指摘件数: Critical 0 / Major 0 / Minor 1 / Suggestion 2

## 実行した検証

| 対象 | 結果 |
|---|---|
| `maui/` `dotnet test KsDialogs.Maui.Tests` | **133 / 0 failures** (前回 129 から +4。`BridgeContentSupplyTests` が 11 本へ増え、預かり口の型保存・混線しないこと・警告文言を固定) |
| `maui/android/native` `./gradlew :ksdialogs-maui-bridge:test --rerun-tasks` | **BUILD SUCCESSFUL** / 7 suite・失敗 0。新規 `MauiToastContentSupplyTests` 1 本を含む |
| `ios/` `xcodebuild test -scheme KsDialogs` (iPhone 17 / iOS 26.5 Sim) | **`** TEST SUCCEEDED **`** / **251 tests in 47 suites**・失敗 0。TS-CO-07 が 3 本 (型不一致 / インライン / 登録経路) に |
| `maui/macios/native/` `xcodebuild -scheme KsDialogsMauiBridge build` | **`** BUILD SUCCEEDED **`** |
| `maui/` `dotnet build KsDialogs.Maui -f net10.0-ios` | 成功 / **警告 0・エラー 0** (今サイクルで書き換えた `Platforms/iOS/*.cs` はこの TFM でしか compile されないため必須) |
| `maui/` `dotnet build KsDialogs.Maui -f net10.0-android` | 成功 / **警告 0・エラー 0**。aar 再生成後の生成 binding が `MauiDialogContent? CreateContent ()` を出しており、Kotlin の nullable が C# 側まで届いている |
| `scripts/comment-policy-lint.py` / `local-path-lint.py` / `identity-lint.py` | いずれも違反 0 (検査対象 907 ファイル) |
| `scripts/scenario-id-coverage.py --require-mirror` | 175/197 (除外 22)・未網羅なし・**両 Native ミラー OK** (TS-CO は mirror 対象領域) |
| Android instrumented (既存の実行結果を確認) | `android/ksdialogs` **268 / 0 failures**、`android/ksdialogs-compose` **37 / 0 failures**。実行時刻は 2 周目修正の後 (00:57 / 00:58 対 修正 00:37〜00:44) で、今サイクル分を反映している |

未実行 (今サイクルの diff が触れていないため意図的に省略): `kmp/` `./gradlew allTests`、android / kmp の
負の compile 検査、iOS の負の compile 検査。今サイクルの変更は MAUI 経路の C# / Kotlin / Swift と
iOS テスト 1 本のみで、公開面の形は変わっていない (両 TFM の警告 0 とミラー検査で裏取り)。

## 確認した観点 (指摘に至らなかったもの)

依頼された 4 点について、成立を確認した根拠を残す。

**(a) `JavaProxyThrowable` が境界を越えない構造か / null が「1 枚だけ破棄」へ合流するか — 成立**

- `maui/KsDialogs.Maui/Platforms/Android/PlatformToastGateway.cs:70-76`: 例外を投げうる 3 箇所
  (`request.CreateContent()` / `ResolveMauiContext()` の `?? throw` / `PlatformDialogContent.Create`) が
  すべて `BridgeContentSupply.CreateOrDiscard` のラムダの**内側**にあり、`Create` は `catch (Exception)` で
  受け止める。C# の例外はすべて `Exception` 派生なので、この面から Java へ抜ける経路は残っていない。
- 生成バインディング (`KsDialogs.Binding.Android` の `IMauiToastContentProvider.cs`) は
  `MauiDialogContent? CreateContent ()` を宣言し、`n_CreateContent` は
  `JNIEnv.ToLocalJniHandle(__this.CreateContent())` で null をそのまま Java の null にする。
  erased な JNI シグネチャは変わっていない (`()Ljp/kamusoft/ksdialogs/maui/MauiDialogContent;`) ため、
  成功系のマーシャリングにも変化が無い。
- Kotlin 側 (`maui/android/native/ksdialogs-maui-bridge/src/main/kotlin/jp/kamusoft/ksdialogs/maui/MauiToastBridge.kt`
  の `MauiToastViewModel.createContentView()`) が null を `error(...)` = `IllegalStateException` に変換。
  これは `Exception` 派生なので、`android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/ToastCoordinator.kt:164`
  の `catch (contentFailure: Exception)` に**設計どおり**掛かる。
- 合流先も確認した: catch → `discard(display)` (`ToastCoordinator.kt:221-226`) が
  `isFinishing = true` → `timerJob?.cancel()` → `removeDisplay` (資源解放 + リストから除去) を行い、
  `beginDisplay` は `isFinishing` を見てタイマーを張らない (`:129-132`)。`attachIfPossible` の呼び出し口は
  `beginDisplay` (`:125`) と提示先復帰 (`:275`) の 2 箇所だけで、**どちらも同じ try の内側**を通る。
  他の表示は display 単位で独立しており影響しない。
- 追加テスト (`MauiToastContentSupplyTests.kt`) は `catch (failure: Exception)` で受けているため、
  もし `Error` 側になれば test が落ちる。「Exception であること」を型名の assert ではなく catch の型で
  表現しているのは適切な設計。

**(b) 元例外が iOS 経路で呼び出し元へ届くか / 混線しないか / NSError 変換が保たれるか — 成立**

- 預かりは `BridgeContentFailure` のインスタンス単位で、`PlatformLoadingGateway.ShowAsync` / `RunAsync` と
  `PlatformDialogGateway.PresentAsync` がそれぞれ呼び出しごとに `new` して自分の `Settle` / `OnClosed` に
  だけ渡す。Swift 側も show / present ごとに ViewModel と completion が 1 対 1
  (`MauiLoadingBridge.report` は自分の completion にしか返さない) なので、呼び出し間で混ざる経路が無い。
  `HideAsync` は中身を作らないため `contentFailure: null` を明示している。
- `Settle` (`PlatformLoadingGateway.cs:86-107`) / `OnClosed` (`PlatformDialogGateway.cs:75-77`) は
  預かりがあるときだけ元例外を `TaskCompletionSource` に載せる。TCS に載せた例外は await 時に
  元のスタックを保ったまま再送出されるため、型・メッセージ・スタックが呼び出し元へ届く。
  `DialogCallContextTests.ShowFailsWhenContentViewCreationThrows` が facade 側で固定している契約と
  iOS gateway の挙動が揃った。
- 型付き例外: `ToBridgeContent` 内の `?? throw new DialogException.PresentationHostUnavailable()` が
  預かりに入るため、review-003 が指摘した「Loading だけ型が潰れる」非対称は解消。
  Dialog の `MauiDialogClosureKind.PresentationHostUnavailable` は従来どおり型付きのまま。
- Native 固有失敗: 中身の供給が成功していれば `Cause` は null のままなので、
  `error.LocalizedDescription` からの `InvalidOperationException` 変換がそのまま残る。
- スレッド: 供給の実行 (`makeContentView()` は `@MainActor`) と `Settle` / `OnClosed` の呼び出し
  (`report` / `Task { @MainActor }`) が同じスレッドなので、`captured ??=` に可視性・競合の問題は無い。

**(c) 事前 dismiss の取りこぼし / 正常系の不変性 — 成立**

- `MauiDialogPresentation.dismiss()` は notifier 未設定なら `isDismissRequested` を立てるだけで、
  `attach()` がそれを見て即 `complete(true)` する。attach を `makeContentView()` の後ろへ移しても、
  「dismiss が先に来た」窓が広がるだけで取りこぼしは発生しない。
- 中身の生成が失敗した場合は attach されないが、その提示は `show` の失敗として閉鎖の通知
  (`.Failed`) に載るため、通知はちょうど 1 回で確定する。notifier が宙に浮く経路は無い。
- 正常系: attach と `makeContentView()` はどちらも同じ factory closure の内側で走るため、
  器から見た観察順 (factory の呼び出し → View の受け取り) は不変。C# の中身が生成中に結果を
  確定させる書き方 (`ResultChannel` 即時確定 → `presentation.Dismiss()`) をしても、
  旧順序では「attach 済み notifier を即 complete」、新順序では「isDismissRequested → attach で即 complete」で
  結末は同じ。

**(d) TS-CO-07 の spec 適合 — 成立**

- 追加テスト (`ios/Tests/KsDialogsTests/ToastContractTests.swift:182-202`) がインライン公開面
  (`toast.show(_:duration:placement:content:)`) に throwing クロージャを直接渡しており、
  spec (`kasane/changes/add-toast/specs/dialog-contract/spec.md:54`) の GIVEN 「例外を投げる factory による
  インライン経路の show」に一致する。
- Android の対応テスト (`android/ksdialogs/src/androidTest/kotlin/jp/kamusoft/ksdialogs/ToastContractTests.kt:169-173`)
  は元からインライン経路なので、これで両 Native の GIVEN が揃った。
- 登録経路版 (`:204-`) の残置も妥当 — 互換面 (MAUI / KMP) が実際に通るのはこちらで、回帰として意味がある。

## 指摘事項

### [🟡 Minor] MAUI 経路を書き換えたのに、実アプリを通した証跡が 2 周目修正より前で止まっている

**該当箇所**:
- `kasane/changes/add-toast/ui/verification/` (`maui-android-*.png` = 2026-08-27 21:05 / 23:01、
  `maui-ios-*.png` = 同 23:53)
- 対象の変更: `maui/KsDialogs.Maui/Platforms/Android/PlatformToastGateway.cs`、
  `maui/android/native/ksdialogs-maui-bridge/src/main/kotlin/jp/kamusoft/ksdialogs/maui/MauiToastBridge.kt`、
  `maui/macios/native/KsDialogsMauiBridge/MauiDialogBridge.swift` (いずれも 2026-08-28 00:43 以降)
- 規約: `kasane/concepts/cross/conventions/runtime-behavior-verification.md`

**問題点**:

今サイクルで書き換えたのは MAUI の binding / 互換面、つまりこのリポジトリで
**ユニットテストの射程外にある唯一の層**である。実際に動かしたテストは境界の両端しか見ていない:

- `BridgeContentSupplyTests` (net10.0) — C# 側が失敗を値に変えるところまで。`Platforms/iOS` /
  `Platforms/Android` の gateway は当該 TFM でしか compile されず、テストプロジェクト (net10.0) からは
  見えないため、`Settle` / `OnClosed` の預かりの受け渡しは 1 行も実行されていない
- `MauiToastContentSupplyTests` (JVM) — Kotlin 側が null を例外に変えるところまで。
  .NET ↔ JNI のマーシャリングも `createContentView()` の成功系 (`applyAttributes` → View 返却) も通らない
- Android instrumented 268 件 — Native ライブラリ本体であって、MAUI binding 経由ではない

この 3 つが green でも、(a) が直した症状そのもの (managed 例外が `JavaProxyThrowable` = `Error` 派生として
境界を越えプロセスを落とす) は一度も観測されておらず、直った状態も観測されていない。
`runtime-behavior-verification.md` は「実行時挙動が絡む不具合の修正は、修正前の実環境再現 /
修正後の同一手順での解消確認 / 証跡の 3 点を満たすまで完了と報告しない」「コード読解だけで
原因仮説を真因と断定しない」と定めており、この症状は同規約の適用範囲 (ユニットテストで症状自体を
再現できない) にまっすぐ当てはまる。加えて同規約は「MAUI 経由の症状は MAUI の Sample で再現する」と
形態まで指定している。

さらに**成功系にも穴がある**。(a) は MAUI Android のカスタム Toast が中身を得る経路そのものを
(C# の戻り型・Kotlin の interface・再生成した aar と binding まで) 差し替えており、
(c) は MAUI iOS のダイアログ提示で notifier を結ぶ位置を動かしている。どちらも既存の Sample デモ
(`custom-toast` / ダイアログ系) が直接踏む経路なのに、その撮り直しが行われていない。
本 change の lessons 観測 (`kasane/lessons/inbox/sample-walkthrough-catches-bridge-defects.md`、count 2)
自体が「継ぎ目のテストが green でも Sample 通しがブリッジの欠陥を捕まえた」事例を 3 件記録しており、
このプロジェクトで最も費用対効果の高い検証手段を今回だけ省いた形になっている。

なお、これは「iOS bridge にテスト標的が無い」(second-opinion-code-003 で申し送りに降格) の
蒸し返しではない。そちらは**自動テスト**の話で、こちらは**既にある Sample デモを撮り直すだけ**の話である。

**推奨修正**:

1. (最小・低コスト) 成功系だけでも撮り直す。既存の起動引数でそのまま出せる:
   MAUI Android の `custom-toast` (と `toast-stack`)、MAUI iOS のダイアログ系デモ。
   `ui/verification/` の該当画像を差し替え、`sample-walkthrough.md` に「2 周目修正後の再取得」と
   1 行残す。これで (a) と (c) の**回帰**は塞がる
2. (失敗系の A/B) 中身の作り手が例外を投げる MAUI Android の Toast を実機で 1 回踏み、
   修正前ビルドでプロセスが落ち・修正後ビルドで Toast 1 枚だけが出ずに他が続くことを
   前後の状態で残す。これが規約の求める本来の形だが、Sample に「失敗する factory」のデモが無く
   一時的な追加が要るため、実施するかはオーナー判断でよい (実施しないなら、
   「症状は javap と生成コードの読解で特定し実環境では未再現」であることを
   `deviation.md` か蒸留の申し送りに明記して、後から追える形にしておく)

---

### [🔵 Suggestion] tasks.md にスコープ追加分の節が無い (review-003 から状態変わらず)

**該当箇所**: `kasane/changes/add-toast/tasks.md` (1〜7 節すべてが元の Toast のタスク)

review-003 の Suggestion 4 と同じ。オーナー決定で同梱された「MAUI iOS の Loading / Dialog の
例外境界修正 + iOS native の factory 契約の `throws` 化」と、今サイクルの「MAUI Android の
Toast 中身供給の防護」に対応するタスク行が無い。deviation.md に理由は残っているが、
実行記録としての tasks.md が本変更の作業の全体を表していない。8 節を 1 つ足して [x] で
記録することを勧める (蒸留と ksn-verify の対応表が片側だけを見ると欠落に見えるため)。

---

### [🔵 Suggestion] `ExceptionDispatchInfo` を使っているが、スタックを保っているのは TCS 側

**該当箇所**: `maui/KsDialogs.Maui/Internals/BridgeContentSupply.cs:99-108`

`BridgeContentFailure` は `ExceptionDispatchInfo.Capture(thrown)` で預かるが、公開しているのは
`SourceException` (= 元の例外オブジェクトそのもの) だけで、`Throw()` を呼ぶ経路は無い。
呼び出し元へスタックを保ったまま渡しているのは、受け側の
`TaskCompletionSource.TrySetException` / `DialogPresentationCompletion.Fail` が await 時に
`ExceptionDispatchInfo` 経由で再送出する仕組みのほうである。

つまり `Exception?` フィールドで持っても観察可能な差は出ない。動作は正しいので直す必要は無いが、
読み手が「ここで退避の細工をしている」と読んで `Throw()` を探しに行く。
`Exception?` に置き換えるか、`Throw()` を使わない (TCS へ載せる形で足りる) ことをコメントに
1 行足すと、次に触る人の探索が減る。

## アクションプラン

1. **[Minor]** MAUI の Sample を 2 周目修正後のビルドで通し直し、`custom-toast` (MAUI Android) と
   ダイアログ系 (MAUI iOS) の証跡を `ui/verification/` に差し替えて `sample-walkthrough.md` に追記する。
   失敗系の A/B まで取るかはオーナー判断 — 取らない場合は「実環境では未再現」を記録に残す
2. **[Suggestion]** tasks.md にスコープ追加分 + Android Toast 防護の節を足して [x] で記録する
3. **[Suggestion]** `BridgeContentFailure` の `ExceptionDispatchInfo` を `Exception?` に寄せるか、
   `Throw()` を使わない理由をコメントに 1 行足す

1 の撮り直しはコードを触らないため、完了後の再テストは不要 (本レビューで全ルートの
ビルドとテストが green であることを確認済み)。
