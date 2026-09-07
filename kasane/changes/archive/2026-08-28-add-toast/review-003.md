# レビュー結果: add-toast (3 回目)

**日付**: 2026-08-28
**判定**: CHANGES_REQUESTED

## サマリー

今回の主務であるスコープ追加分 (MAUI iOS の Loading / Dialog の factory 例外境界修正、および iOS native の
factory 閉包の `throws` 化) は、**iOS 側については設計・実装・検証のいずれも妥当**である。`BridgeContentSupply`
の 2 入口 (CreateOrDiscard / CreateOrFail) は面ごとの結末の違いを名前で表せていて、失敗の合流先も既存契約
(Loading = 開始そのものの失敗 / Dialog = 閉鎖の Failed) に正しく落ちている。`throws` 化の正常系の観察可能挙動は
不変で、既存の失敗系 (未登録・型不一致・提示先不在) の分岐も温存されている。ソース互換は呼び出し側について
実測で確認した (負の compile 検査 5 本が期待どおりの診断で失敗し続ける・全テスト green)。

一方で、今回明示的に検証を依頼された **「Android 側は既存の受け皿あり・修正不要」という判断は、Loading /
Dialog については正しいが、Toast については誤り**であることを実証的に確認した。.NET for Android が managed
例外を Java へ渡すときに使う `android.runtime.JavaProxyThrowable` は **`java.lang.Error` の派生**であり、
Android の `ToastCoordinator` の受け皿は `catch (Exception)` なので**素通りする**。MAUI Android のカスタム
Toast の factory が例外を投げると、iOS で修正したのと同じ「利用者 factory の例外がプロセス落ちになる」
経路が Android に残っている。これが Major 1 件。

指摘件数: Critical 0 / Major 1 / Minor 2 / Suggestion 3

## 実行した検証

| 対象 | 結果 |
|---|---|
| `ios/` `xcodebuild test -scheme KsDialogs` (iPhone 17 / iOS 26.5 Sim) | **`** TEST SUCCEEDED **`** / 失敗 0。今回追加の 2 スイート (`成立しなかった Loading の開始` = LoadingStartFailureTests、`VM 型キーによる View 解決と毎回生成` = DialogRegistryTests) を含め全 suite passed |
| `maui/` `dotnet test KsDialogs.Maui.Tests` | **129 / 0 failures** (前回 126 から +3。`BridgeContentSupplyTests` が 7 本 = 旧 ToastContentSupplyTests 4 本 + CreateOrFail 側 3 本) |
| `maui/macios/native/` `xcodebuild -scheme KsDialogsMauiBridge build` | **`** BUILD SUCCEEDED **`** |
| 生成 ObjC ヘッダ (`KsDialogsMauiBridge-Swift.h`) の nullability | `presentContentProvider:` / `MauiLoadingContent initWithContentProvider:` / `MauiToastContent initWithContentProvider:` の **3 つとも `KSDMauiDialogContent * _Nullable (^ _Nonnull)(void)`**。Swift の `MauiDialogContent?` が互換面まで届いている |
| `maui/` `dotnet build KsDialogs.Maui -f net10.0-ios` (binding 込み) | 成功 / 警告 0 / エラー 0。生成バインディング (`SupportDelegates.g.cs`) が `public delegate MauiDialogContent? MauiDialogContentProvider ()` / 同 `MauiLoadingContentProvider` を出しており、trampoline も `Runtime.RetainAndAutoreleaseNSObject(retval)` で null を扱える |
| iOS 負の compile 検査 5 本 (`LOADING_SHOW_OPTIONS` / `LOADING_SHOW_STYLE` / `RESULT_TYPE` / `NOTIFIER_VALUE` / `VM_ATTRIBUTE`) | **5 本すべてが期待した診断で TEST BUILD FAILED** (`extra argument 'options'` / `extra argument 'style'` / `cannot assign value of type 'DialogResult<Bool>' to 'DialogResult<String>'` / `cannot convert value of type 'String' to 'Bool'` / `has no member 'proportionalWidth'`)。Loading / Dialog の `throws` 化で公開面の締まりは落ちていない |
| `scripts/comment-policy-lint.py` / `local-path-lint.py` / `identity-lint.py` | いずれも違反 0 |
| `scripts/scenario-id-coverage.py --require-mirror` | 未網羅なし・両 Native ミラー OK |
| Swift のプロトコル準拠互換 (最小再現) | `func f(g: @escaping (Int) -> Int)` は `func f(g: @escaping (Int) throws -> Int)` の要件を**満たさない** (`type 'T' does not conform to protocol 'Q'`) ことを swiftc で確認。呼び出し側 (非 throwing クロージャを渡す) は互換 |
| `JavaProxyThrowable` の基底クラス | `mono.android.jar` / `java-interop.jar` を `javap` で確認: `android.runtime.JavaProxyThrowable extends java.lang.Error` / `net.dot.jni.internal.JavaProxyThrowable extends java.lang.Error` |
| Android バインディングの marshal method | `KsDialogs.Bridge.IMauiToastContentProvider.cs` 生成コードの `n_CreateContent` は `catch (System.Exception __e) { __r.OnUserUnhandledException(...) }` → `EndMarshalMethod` で Java 側へ throwable として送出する形 |

未実行 (今回の追加分が触れていないため意図的に省略): `android/` `./gradlew test` / `connectedDebugAndroidTest`、
`kmp/` `./gradlew allTests`、`maui/android/native/` の Kotlin テスト、android / kmp の負の compile 検査。
今サイクルの diff に Kotlin / KMP のソース変更は無く、review-002 で全件確認済み。

## 指摘事項

### [🟠 Major] MAUI Android のカスタム Toast の中身供給が未防護のまま — 「Android は受け皿あり」は Toast には成立しない

**該当箇所**:
- `maui/KsDialogs.Maui/Platforms/Android/PlatformToastGateway.cs:64-69`
- `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/ToastCoordinator.kt:162`
- (参考・正しく防護されている側) `maui/android/native/ksdialogs-maui-bridge/src/main/kotlin/jp/kamusoft/ksdialogs/maui/MauiLoadingBridge.kt:218`、同 `MauiDialogBridge.kt:91`

**問題点**:
deviation.md の「Android 側 (Loading / Dialog の Kotlin 受け皿) の確認も同梱範囲」という判断のうち、
**Loading / Dialog は正しい**。両者の受け皿は `catch (failure: Throwable)` で、factory 呼び出しはその try の
内側にある。.NET for Android は managed 例外を `android.runtime.JavaProxyThrowable` に包んで Java へ送出し、
これは `java.lang.Error` 派生なので `Throwable` の catch には掛かる — `javap` で基底クラスを、生成
バインディング `n_CreateContent` の `OnUserUnhandledException` 経路を、それぞれ実物で確認した。

**しかし Toast はこの受け皿を共有していない**。Android の Toast の中身生成の受け皿は
`ToastCoordinator.kt:162` の `catch (contentFailure: Exception)` であり、直前のコメントが
「実行の継続そのものが成り立たない致命的な失敗 (メモリ枯渇など) は隠さずそのまま伝える」と
`Exception` / `Error` の切り分けを**意図的に**行っている。ところが MAUI 経路では利用者 factory の
`System.Exception` が例外なく `Error` (JavaProxyThrowable) として届くため、**通常の失敗が全部「致命的」
側に分類されて素通りする**。素通りした先は `beginDisplay` を走らせている
`CoroutineScope(SupervisorJob() + Dispatchers.Main)` (`ToastCoordinator.kt:52-54`) で、
CoroutineExceptionHandler は付いていないため、既定のスレッド例外ハンドラ = **プロセス落ち**になる。

これは second-opinion-code-001 Major 2 (= 今回のスコープ追加の発端) と**同一の欠陥型**で、
iOS 側では `BridgeContentSupply.CreateOrDiscard` で塞がれているのに Android 側だけが空いている。
`PlatformToastGateway.cs` は本変更で新規追加されたファイルであり、Toast は本変更が新設した公開面なので、
「既存面の穴」ではなく**本変更が持ち込んだ穴**である。

なお、同じ理屈は Android の Loading / Dialog の `ContentProvider`
(`Platforms/Android/PlatformLoadingGateway.cs:110`、`Platforms/Android/PlatformDialogGateway.cs:52`) が
無防備であること自体には及ばない (Kotlin 側が `Throwable` を捕まえるため観察可能挙動は成立する)。
ただし、失敗の理由が `failure.message` として文字列でしか渡らない点は iOS と同じ制約で、
`BridgeContentSupply` を Android にも通しておけば 3 面の作りが揃い、Kotlin 側の
`Exception` / `Error` 方針に依存しなくなる。

**推奨修正**:
最小の修正は iOS と対称にすること — `Platforms/Android/PlatformToastGateway.cs` の `ContentProvider.CreateContent`
を `BridgeContentSupply.CreateOrDiscard(...)` で包む。`BridgeContentSupply` は `Internals/` にあり
TFM 条件が付いていないので Android からもそのまま使える。ただしこの包みは `null` を返すため、
受け側の表現も iOS と揃える必要がある:

- 望ましい形: Kotlin の `MauiToastContentProvider.createContent()` の戻りを `MauiDialogContent?` にし、
  `MauiToastViewModel.createContentView()` (`MauiToastBridge.kt:75-79`) が null を受けたら Kotlin の
  通常の例外を投げる (iOS の `MauiDialogBridgeError.contentUnavailable` と同じ役回り)。
  こうすると `ToastCoordinator.kt:162` の `catch (Exception)` に**設計どおり**掛かり、
  「1 枚だけ破棄」の既存契約に合流する。Kotlin 側の `Exception` / `Error` 方針も無傷で残る。
- 暫定でも可の形: 上記のうち Kotlin 側の nullable 化だけ行わない場合、Java 実装が null を返すと
  Kotlin の null チェックで NPE になり結果的に `catch (Exception)` に掛かるが、**偶然に頼った経路**で
  ログにも実態と食い違う NPE が残るため推奨しない。

いずれの形でも、`ToastCoordinator.kt:162` 付近のコメント (「中身の作り手が投げる通常の失敗まで」) に
MAUI 経路の失敗がどう届くかを 1 行足すと、次に読む人が同じ罠を踏まない。
併せて、Android の Loading / Dialog の `ContentProvider` も `CreateOrFail` で揃えるかどうかは
オーナー判断でよい (今の形でも観察可能挙動は成立する)。

---

### [🟡 Minor] Loading / Dialog は呼び出し元が待っているのに、元の失敗が呼び出し元へ渡らない

**該当箇所**:
- `maui/KsDialogs.Maui/Internals/BridgeContentSupply.cs:58-63`
- `maui/KsDialogs.Maui/Platforms/iOS/PlatformLoadingGateway.cs:73-81` (`Settle`)
- `maui/KsDialogs.Maui/Platforms/iOS/PlatformDialogGateway.cs:60-62` (`Failed`)

**問題点**:
`BridgeContentSupply` のコメントが自ら書いているとおり、Loading / Dialog では呼び出し元に届くのは
互換面が用意した理由 (`InvalidOperationException("MAUI 側が表示の中身を作れませんでした。")`) だけで、
利用者 factory が投げた元の例外は `Trace.TraceWarning` にしか残らない。Toast (fire-and-forget) では
これが唯一の選択肢だが、**Loading / Dialog では呼び出し元がまだ同じ C# のスコープで待っている**ので、
元の例外を握っておいて `InnerException` として返すことができる。

しかも、その型紙は**同じファイルの隣**にある — `PlatformLoadingGateway.RunAsync`
(`Platforms/iOS/PlatformLoadingGateway.cs:36-56`) は `Exception? failure = null;` にアクションの失敗を
退避し、互換面の完了を待ってから `throw failure;` で呼び出し元へそのまま投げ直している。
今の形は、この確立済みのパターンが使える場面で診断情報を捨てている。

副作用として、`PlatformLoadingGateway.ToBridgeContent` (`:107-112`) では
`ResolveMauiContext() ?? throw new DialogException.PresentationHostUnavailable()` が `CreateOrFail` の
**内側**に入ったため、本当に提示先が無かった場合の**型付き例外 `DialogException.PresentationHostUnavailable`
が汎用の `InvalidOperationException` に潰れる**。Dialog 側 (`PlatformDialogGateway.cs:28-29`) は
文脈解決を供給の外に出しているので型が保たれており、2 つの面で非対称になっている。
(Loading は `ShowAsync` が UI スレッドへ乗る前に `ToBridgeContent` を呼ぶ構造なので、Dialog と同じ
「先に解決」には単純には寄せられない。したがって下の推奨修正はどちらの問題もまとめて解く形にしてある。)

**推奨修正**:
`CreateOrFail` に失敗の退避口を持たせ (例: `out Exception? failure` か、呼び出し側が握る
`Exception?` 変数へ書き込む形)、Loading / Dialog の gateway が互換面の失敗通知を受け取ったときに、
退避してある元の例外を `InnerException` に載せて (あるいは `DialogException` 系はそのまま) 投げ直す。
`RunAsync` の既存パターンをそのまま踏襲できる。`CreateOrDiscard` (Toast) は今のままでよい。

---

### [🟡 Minor] deviation.md の「ソース互換」は呼び出し側に限った話で、公開 protocol への外部準拠は壊れる

**該当箇所**: `kasane/changes/add-toast/deviation.md:8`
(対象: `ios/Sources/KsDialogs/Presentation/KsLoading.swift:45-102`、
`ios/Sources/KsDialogs/Presentation/KsDialogs.swift:30-41`)

**問題点**:
deviation.md は「ソース互換 (samples/ios・samples/kmp/iosApp 無改変ビルド成功で実証)」と書いているが、
samples が実証しているのは**呼び出し側 (非 throwing クロージャを渡す側) の互換**だけである。
`KsLoading` / `KsDialogs` は `public protocol` であり、その要件のクロージャ引数が `throws` になったため、
**旧シグネチャで準拠していた外部の型 (テストダブル・DI 差し替え用の実装) は準拠しなくなる**。
Swift のプロトコル witness マッチングは引数位置の関数型に反変の緩和を持たないことを最小再現で確認した:

```
protocol Q { func f(g: @escaping (Int) throws -> Int) }
struct T: Q { func f(g: @escaping (Int) -> Int) {} }
// → error: type 'T' does not conform to protocol 'Q'
```

実装を変えるべきという指摘ではない (`throws` 化自体は合意済みで、この破壊は不可避)。
**deviation.md の互換の主張が実証の範囲より広い**ことが問題で、公開ライブラリとして
リリースノートに書くべき破壊的変更を取りこぼす risk がある。

**推奨修正**:
deviation.md の該当行の「ソース互換」を「**呼び出し側**のソース互換 (…で実証)。ただし
`KsLoading` / `KsDialogs` に外部で準拠していた型は再準拠が必要」と限定して書き直す。
蒸留時に破壊的変更として拾えるようにしておく。

---

### [🔵 Suggestion] tasks.md にスコープ追加分のタスク行が無い

**該当箇所**: `kasane/changes/add-toast/tasks.md` (1〜7 節すべてが元の Toast のタスク)

オーナー決定で同梱された「MAUI iOS の Loading / Dialog の例外境界修正 + iOS native の factory 契約の
`throws` 化」に対応するタスク行が無く、tasks.md が本変更の作業の全体を表さなくなっている。
deviation.md に理由は残っているが、実行記録としての tasks.md と検証 (ksn-verify) の対応表が
片方だけを見ていると欠落に見える。8 節を 1 つ足して該当作業を [x] で記録することを勧める。

---

### [🔵 Suggestion] 「action は必ず実行される」に factory の失敗が加わったことを蒸留で concepts へ

**該当箇所**: `kasane/concepts/core/api/loading-semantics.md:43`

現行の記述は「渡された処理 (action) は表示状態によらず**必ず実行される**…構成ミス (未登録 VM 等) は
実行前に fail-fast で失敗してよい」。今回、**利用者 factory が投げた失敗**も action 実行前の fail-fast
に加わった (`Loading.start` は `beginInlineUse` で throw するので action は走らない)。
矛盾ではなく「構成ミス」の具体例が 1 つ増えた形だが、公開面の失敗源なので、
蒸留 (ksn-distill) で `未登録 VM 等` の括弧に factory の失敗を明記しておくと利用者向けの正が揃う。

---

### [🔵 Suggestion] bridge の nil 供給経路そのものには自動テストが無い

**該当箇所**: `maui/macios/native/KsDialogsMauiBridge/` (テスト標的が存在しない)

今回の追加テストは (a) C# 側の `BridgeContentSupply` が失敗を値に変えること、
(b) iOS native の factory 例外が show / start の失敗になり提示が起きないこと、の 2 点を押さえていて、
どちらも過不足ない。一方で両者を繋ぐ結び目 —
`MauiLoadingViewModel.makeContentView()` / `MauiDialogViewModel.makeContentView()` が nil を受けて
`contentUnavailable` を投げ、それが完了 / 閉鎖の通知として MAUI 側へ届くこと — は自動では見ていない。
bridge に XCTest 標的が無いのは本変更以前からの構造なので新規の負債ではないが、
`ui/verification/` の Sample 通しでもこの経路は踏まれていない。
将来 bridge にテスト標的を作るときの最初の 1 本として起票しておく価値がある (簡易起票で足りる)。

## アクションプラン

1. **[Major]** `Platforms/Android/PlatformToastGateway.cs` の中身供給を `BridgeContentSupply.CreateOrDiscard`
   で包み、Kotlin 側 (`MauiToastContentProvider` / `MauiToastViewModel.createContentView`) を nullable +
   例外送出へ揃えて、`ToastCoordinator` の「1 枚だけ破棄」に設計どおり合流させる。
   併せて `ToastCoordinator.kt:162` 付近のコメントに MAUI 経路の届き方を 1 行追記する
2. **[Minor]** `CreateOrFail` に失敗の退避口を足し、Loading / Dialog の gateway が元の例外を
   `InnerException` (または型付き例外そのまま) として呼び出し元へ返すようにする
   (`PlatformLoadingGateway.RunAsync` の既存パターンを踏襲)
3. **[Minor]** deviation.md の「ソース互換」の主張を呼び出し側に限定し、公開 protocol への
   外部準拠が壊れることを明記する
4. **[Suggestion]** tasks.md にスコープ追加分の節を足して [x] で記録する
5. **[Suggestion]** 蒸留時に `loading-semantics.md:43` の fail-fast の例へ factory の失敗を加える
6. **[Suggestion]** bridge の nil 供給経路のテストを簡易起票しておく

1 の修正後は、`maui/` の `dotnet test` と Android の Toast 系 androidTest
(`ToastContractTests` / `ToastAccessibilityTests` 等) の再実行で足りる
(iOS 側は今回の修正で触れないため再実行不要)。
