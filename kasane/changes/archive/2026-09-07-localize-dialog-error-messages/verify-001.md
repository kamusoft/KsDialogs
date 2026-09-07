# verify-001: localize-dialog-error-messages

判定: **VALID**

検証日: 2026-09-07 / 対象: 作業ツリーの未コミット変更 66 ファイル (`git status --short`。実装 59 + `tasks.md` + 新規テスト 5 + `deviation.md` + `verification/`)
入力: `proposal.md` / `specs/{ios-native,android-native,kmp-facade,maui-binding,user-skills}/spec.md` / `tasks.md` / `deviation.md` / `verification/japanese-literal-grep/`

検証中に `review-001.md` / `review-002.md` / `second-opinion-code-001.md` が並行して作業ツリーへ現れたが、本検証はそれらを入力にしていない (ksn-verify は仕様と実装の一致だけを見る)。検証開始時と終了時で追跡ファイルの変更集合は同一で、実装は検証中に動いていない。

検証は 2 軸で行った。**軸 A** はデルタスペックの対応表 (現行 ja → 変更後 en) の各行が実装のリテラルと 1 文字違わず一致することの機械照合、**軸 B** は全 Scenario と実装・テストの対応。

---

## 軸 A: 対応表 → 実装 (61 箇所)

対応表の「変更後 (en)」列 (4 capability 合計 55 行、うち重複を除く固有文言) を抽出し、プレースホルダ (`{T}` `{expected}` `{actual}` `{N}` `{phase}` `{error}` `{0}` `{1}`) を任意文字列に読み替えた正規表現で、各 capability の実現経路のソース (`*.swift` / `*.kt` / `*.cs`、`/build/` 除外) を全走査した。**55 行すべてが実装に存在し、未検出 0 行**。逆に diff を全行読み、置き換えられたリテラルが対応表の行に対応しないものが無いことも確認した (**表に無い訳語の混入なし**)。

### iOS Native (26 箇所 / spec 表 22 行)

| 対応表の行 | 変更後 (en) | 実装 |
|---|---|---|
| `DialogError.viewFactoryNotRegistered` | `No View factory is registered for ViewModel type {T}.` | `ios/Sources/KsDialogs/Contract/DialogError.swift:27` |
| `DialogError.presentationHostUnavailable` | `No screen is available to present the Dialog.` | `ios/Sources/KsDialogs/Contract/DialogError.swift:29` |
| `DialogError.viewFactoryTypeMismatch` | `The registered View factory cannot accept ViewModel type {T}.` | `ios/Sources/KsDialogs/Contract/DialogError.swift:31` |
| `DialogError.resultTypeMismatch` | `The result value type does not match (expected: {expected} / actual: {actual}).` | `ios/Sources/KsDialogs/Contract/DialogError.swift:33` |
| `DialogError.viewModelFactoryNotRegistered` | `No ViewModel factory is registered for ViewModel type {T}.` | `ios/Sources/KsDialogs/Contract/DialogError.swift:35` |
| `DialogError.viewModelFactoryTypeMismatch` | `The registered ViewModel factory does not produce ViewModel type {T}.` | `ios/Sources/KsDialogs/Contract/DialogError.swift:37` |
| `DialogError.viewModelAlreadyShowing` | `This ViewModel instance of type {T} is already being shown.` | `ios/Sources/KsDialogs/Contract/DialogError.swift:39` |
| `KsDialogsKmpError.notRegistered` | (同上 View factory 未登録) | `ios/Sources/KsDialogs/Kmp/KsDialogsKmpError.swift:43` |
| `KsDialogsKmpError.resultTypeMismatch` | (同上 結果型不一致) | `ios/Sources/KsDialogs/Kmp/KsDialogsKmpError.swift:45` |
| View の `init?(coder:)` × 3 | `This View does not support instantiation from a storyboard.` | `ios/Sources/KsDialogs/SwiftUI/DialogSwiftUIContentView.swift:23` / `ios/Sources/KsDialogs/Presentation/LoadingDefaultContentView.swift:61` / `ios/Sources/KsDialogs/Presentation/ToastDefaultContentView.swift:88` |
| ViewController の `init?(coder:)` × 3 | `This ViewController does not support instantiation from a storyboard.` | `ios/Sources/KsDialogs/Presentation/DialogContainerViewController.swift:149` / `LoadingContainerViewController.swift:108` / `ToastContainerViewController.swift:90` |
| Toast duration 不正 (指定値) | `Toast duration must be a positive integer. Showing with the default duration.` | `ios/Sources/KsDialogs/Presentation/ToastCoordinator.swift:120` |
| Toast duration 不正 (ToastStyle 既定) | `The default duration of ToastStyle is not a positive integer. Showing with the built-in default.` | `ios/Sources/KsDialogs/Presentation/ToastCoordinator.swift:123` |
| Toast 中身生成失敗 | `Could not create the Toast content. This presentation is discarded: {error}` | `ios/Sources/KsDialogs/Presentation/ToastCoordinator.swift:150` |
| Dialog 添付値未達 | `No attachment values were received from the Dialog content; presenting with the contract defaults.` | `ios/Sources/KsDialogs/Presentation/DialogContainerViewController.swift:255` |
| Dialog 収束せず | `The Dialog attachments did not converge; using the values from layout pass {N}.` | `ios/Sources/KsDialogs/Presentation/DialogContainerViewController.swift:278` |
| Loading 添付値未達 | `No attachment values were received from the Loading content; presenting with the contract defaults.` | `ios/Sources/KsDialogs/Presentation/LoadingContainerViewController.swift:207` |
| Loading 収束せず | `The Loading attachments did not converge; using the values from layout pass {N}.` | `ios/Sources/KsDialogs/Presentation/LoadingContainerViewController.swift:225` |
| Toast 添付値未達 | `No attachment values were received from the Toast content; presenting with the default placement.` | `ios/Sources/KsDialogs/Presentation/ToastContainerViewController.swift:192` |
| Toast 収束せず | `The Toast attachments did not converge; using the values from layout pass {N}.` | `ios/Sources/KsDialogs/Presentation/ToastContainerViewController.swift:210` |
| フック失敗 | `The Dialog {phase} hook failed: {error}` | `ios/Sources/KsDialogs/Presentation/DialogTransitionRunner.swift:128` |
| フック未完了 | `The Dialog {phase} hook did not complete. Completing the hook is the caller's responsibility.` | `ios/Sources/KsDialogs/Presentation/DialogTransitionRunner.swift:140` |

内訳 26 件 = `DialogError` 7 + `KsDialogsKmpError` 2 + `fatalError` 6 + `Logger.warning` 11。proposal の件数と一致。

### Android Native (12 箇所 / spec 表 12 行)

| 対応表の行 | 変更後 (en) | 実装 |
|---|---|---|
| `ViewFactoryNotRegistered` | `No View factory is registered for ViewModel type {T}.` | `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/DialogException.kt:17` |
| `ViewModelFactoryNotRegistered` | `No ViewModel factory is registered for ViewModel type {T}.` | 同 `:26` |
| `ViewModelAlreadyShowing` | `This ViewModel instance of type {T} is already being shown.` | 同 `:38` |
| `ValueClassViewModel` | `ViewModel type {T} is a value class and cannot be used as a ViewModel.` | 同 `:51` |
| `PresentationHostUnavailable` | `No screen is available to present the Dialog.` | 同 `:56` |
| `DialogLayoutHost` 収束せず | `The attachments did not converge; using the values from layout pass {N}.` | `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/DialogLayoutHost.kt:78` |
| フック失敗 | `The Dialog {phase} hook failed.` | `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/DialogTransitionRunner.kt:156` |
| フック未完了 | `The Dialog {phase} hook did not complete. Completing the hook is the caller's responsibility.` | 同 `:170` |
| Toast 中身生成失敗 | `Could not create the Toast content. This presentation is discarded.` | `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/ToastCoordinator.kt:165` |
| Toast duration 不正 (指定値) | `Toast duration must be a positive integer. Showing with the default duration.` | 同 `:339` |
| Toast duration 不正 (ToastStyle 既定) | `The default duration of ToastStyle is not a positive integer. Showing with the built-in default.` | 同 `:344` |
| Loading 中身生成失敗 | `Could not create the Loading content. Nothing is presented.` | `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/LoadingCoordinator.kt:409` |

iOS と対になる 4 種の本文テンプレート一致 (spec が明示した同一化条件) を実測で確認した:

| 状況 | iOS 本文 | Android 本文 | 判定 |
|---|---|---|---|
| フック失敗 | `The Dialog {phase} hook failed: {error}` | `The Dialog {phase} hook failed.` | ✅ 動的値の前まで一致 (iOS は `{error}` 埋め込み、Android は throwable 引数) |
| フック未完了 | `... is the caller's responsibility.` | 同一文字列 | ✅ 完全一致 |
| duration 不正 (指定値 / ToastStyle 既定) | 2 文とも | 同一文字列 | ✅ 完全一致 |
| Toast 中身生成失敗 | `Could not create the Toast content. This presentation is discarded: {error}` | `Could not create the Toast content. This presentation is discarded.` | ✅ 本文テンプレート一致 |

同一化の対象外と spec が宣言した 2 件 (iOS だけの添付値未達 3 本 / Android の機能名を持たない収束警告) は、宣言どおり別文言のままである。

### KMP (3 箇所 / spec 表 3 行)

| 定数 | 変更後 (en) | 実装 |
|---|---|---|
| `IosDialogGateway.MISSING_RESULT_MESSAGE` | `No Dialog result was delivered.` | `kmp/ksdialogs-kmp/src/iosMain/kotlin/jp/kamusoft/ksdialogs/kmp/IosDialogGateway.kt:97` |
| `IosDialogGateway.UNKNOWN_FAILURE_MESSAGE` | `Failed to show the Dialog.` | 同 `:98` |
| `IosLoadingGateway.UNKNOWN_FAILURE_MESSAGE` | `Failed to show the Loading.` | `kmp/ksdialogs-kmp/src/iosMain/kotlin/jp/kamusoft/ksdialogs/kmp/IosLoadingGateway.kt:141` |

共有コードの `DialogException` が自前文言を持たず素通しする構造は変わっていない (`commonMain` に差分なし)。

### MAUI (20 箇所 / spec 表 18 行)

| 対応表の行 | 変更後 (en) | 実装 |
|---|---|---|
| `ViewFactoryNotRegistered` | `No View factory is registered for ViewModel type {T}.` | `maui/KsDialogs.Maui/Contract/DialogException.cs:24` |
| `ViewModelFactoryNotRegistered` | `No ViewModel factory is registered for ViewModel type {T}.` | 同 `:37` |
| `ViewModelAlreadyShowing` | `This ViewModel instance of type {T} is already being shown.` | 同 `:54` |
| `ValueTypeViewModel` | `ViewModel type {T} is a value type and cannot be used as a ViewModel.` | 同 `:73` |
| `ServiceProviderUnavailable` | `The app's IServiceProvider is not available yet.` | 同 `:91` |
| `PresentationHostUnavailable` | `No screen is available to present the Dialog.` | 同 `:99` |
| `ToastGateway` | `The Native library owns the content of the default Toast.` | `maui/KsDialogs.Maui/Internals/ToastGateway.cs:70` |
| `LoadingGateway` | `The Native library owns the content of the default Loading.` | `maui/KsDialogs.Maui/Internals/LoadingGateway.cs:99` |
| Platform Dialog gateway (両 OS) | `Could not present the Dialog.` | `maui/KsDialogs.Maui/Platforms/Android/PlatformDialogGateway.cs:78` / `maui/KsDialogs.Maui/Platforms/iOS/PlatformDialogGateway.cs:77` |
| Platform Loading gateway (両 OS) | `Could not show the Loading.` | `maui/KsDialogs.Maui/Platforms/Android/PlatformLoadingGateway.cs:159` / `maui/KsDialogs.Maui/Platforms/iOS/PlatformLoadingGateway.cs:103` |
| `MauiDialogBridgeError.unsupportedResult` | `Could not determine the Dialog result.` | `maui/macios/native/KsDialogsMauiBridge/MauiDialogBridgeError.swift:17` |
| `MauiDialogBridgeError.contentUnavailable` | `The MAUI side could not create the presentation content.` | 同 `:19` |
| `MauiToastBridge` の `error()` | (同上) | `maui/android/native/ksdialogs-maui-bridge/src/main/kotlin/jp/kamusoft/ksdialogs/maui/MauiToastBridge.kt:80` |
| 演出失敗 (書式) | `The Dialog transition failed. The Dialog result is not affected: {0}` | `maui/KsDialogs.Maui/Internals/DialogTransitionRunner.cs:85` |
| 演出失敗 (`{0}` の既定) | `cancelled` | 同 `:86` |
| 中身生成失敗 (書式) | `Could not create the presentation content. {0}: {1}` | `maui/KsDialogs.Maui/Internals/BridgeContentSupply.cs:73` |
| `CreateOrDiscard` の効果句 | `This presentation is discarded. Other presentations are not affected` | 同 `:36` |
| `CreateOrFail` の効果句 | `This call fails` | 同 `:52` |

内訳 20 件 = `DialogException` 6 + gateway 例外 6 + bridge 3 + 警告と部品 5。proposal の件数と一致。**4 形態合計 61 箇所**で proposal の総数と一致する。

---

## 軸 B: Scenario → 実装 / テスト

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| `[DM-IO-01]` DialogError の全 case が対応表の英語文言を返す | `ios/Sources/KsDialogs/Contract/DialogError.swift:25-41` | `ios/Tests/KsDialogsTests/DiagnosticMessageTests.swift:12`「[DM-IO-01] DialogError の全 case が英語の説明文を返す」(7 case を完全一致、`expectations.count == 7` で件数も固定) | ✅ 一致 |
| `[DM-IO-02]` KsDialogsKmpError の全 case が対応表の英語文言を返す | `ios/Sources/KsDialogs/Kmp/KsDialogsKmpError.swift:41-46` | 同ファイル `:56`「[DM-IO-02] KsDialogsKmpError の全 case が英語の説明文を返す」(2 case 完全一致) | ✅ 一致 |
| `[DM-IO-03]` iOS のライブラリ本体に日本語の文字列リテラルが残らない | `ios/Sources/` 全体 | 静的 grep (`verification/japanese-literal-grep/`) + `scripts/scenario-id-coverage.py:103` の除外登録 | ✅ 一致 (grep 0 件を再現) |
| `[DM-AN-01]` DialogException の全サブクラスが対応表の英語文言を持つ | `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/DialogException.kt:14-57` | `android/ksdialogs/src/test/kotlin/jp/kamusoft/ksdialogs/DialogExceptionMessageTests.kt:17` (5 サブクラス完全一致) | ✅ 一致 |
| `[DM-AN-02]` Android のライブラリ本体に日本語の文字列リテラルが残らない | `android/ksdialogs/src/main/`・`android/ksdialogs-compose/src/main/` | 静的 grep + `scripts/scenario-id-coverage.py:104` の除外登録 | ✅ 一致 (grep 0 件を再現) |
| `[DM-KM-01]` iOS 互換面の結果に Native の英語文言が載る | `kmp/ksdialogs-kmp/src/iosMain/.../IosDialogGateway.kt` 経路 (Native 素通し) | `kmp/ksdialogs-kmp/src/iosTest/kotlin/jp/kamusoft/ksdialogs/kmp/InteropBridgeContractTests.kt:78` (bridge 結果の `error.localizedDescription` を 2 経路で部分一致) | ✅ 一致 |
| `[DM-KM-02]` Android ホストで Native の英語文言が DialogException に届く | Android gateway の素通し (現行どおり) | `kmp/ksdialogs-kmp/src/androidHostTest/kotlin/jp/kamusoft/ksdialogs/kmp/AndroidDialogGatewayContractTests.kt:157` (`message` 完全一致 + `cause` の型) | ✅ 一致 |
| `[DM-KM-03]` iOS ホストで共有コードの DialogException に Native の英語文言が素通しで届く | 同上 | `InteropBridgeContractTests.kt:103` (提示先なし経路) と `:118` (未登録経路)。spec の「両経路で message を検証する」を 2 テストで満たす | ✅ 一致 |
| `[DM-KM-04]` KMP 共有コードのメイン側に日本語の文字列リテラルが残らない | `kmp/ksdialogs-kmp/src/{commonMain,androidMain,iosMain}/` | 静的 grep + `scripts/scenario-id-coverage.py:105` の除外登録 | ✅ 一致 (grep 0 件を再現) |
| `[DM-MA-01]` DialogException の全入れ子型が対応表の英語文言を持つ | `maui/KsDialogs.Maui/Contract/DialogException.cs:21-101` | `maui/KsDialogs.Maui.Tests/DiagnosticMessageTests.cs:20` (6 入れ子型完全一致) | ✅ 一致 |
| `[DM-MA-02]` 既定の中身を facade で作ろうとすると英語文言の InvalidOperationException になる | `maui/KsDialogs.Maui/Internals/ToastGateway.cs:70` / `LoadingGateway.cs:99` | `maui/KsDialogs.Maui.Tests/DiagnosticMessageTests.cs:49` (Toast / Loading の両経路を完全一致) | ✅ 一致 |
| `[DM-MA-03]` iOS bridge の Error が対応表の英語文言を返す | `maui/macios/native/KsDialogsMauiBridge/MauiDialogBridgeError.swift:14-21` | `maui/macios/native/KsDialogsMauiBridgeTests/MauiBridgeDiagnosticMessageTests.swift:13` (2 case 完全一致) | ✅ 一致 |
| `[DM-MA-04]` Android bridge で MAUI 側が中身を作れないと英語文言の IllegalStateException になる | `maui/android/native/ksdialogs-maui-bridge/src/main/kotlin/jp/kamusoft/ksdialogs/maui/MauiToastBridge.kt:80` | `maui/android/native/ksdialogs-maui-bridge/src/test/kotlin/jp/kamusoft/ksdialogs/maui/MauiBridgeDiagnosticMessageTests.kt:20` (`message` 完全一致) | ✅ 一致 |
| `[DM-MA-05]` MAUI のライブラリ本体に日本語の文字列リテラルが残らない | `maui/KsDialogs.Maui/`・`maui/macios/native/KsDialogsMauiBridge/`・`maui/android/native/ksdialogs-maui-bridge/src/main/` | 静的 grep + `scripts/scenario-id-coverage.py:106` の除外登録 | ✅ 一致 (grep 0 件を再現) |
| (user-skills) 診断表のメッセージ列が対応表の英語文言と一致する | `skills/{en,ja}/ksdialogs-{ios,android,maui,kmp}/references/{dialogs,loading,toast}.md` 24 ファイル | 検査 script (下記「Skills の検査」) — 12 ペア 42 セルすべてが対応表の英語と一致し、en / ja で byte 一致 | ✅ 一致 |
| (user-skills) Skills に日本語の例外メッセージの引用が残らない | 同上 | 検査 script — 対応表の現行 (ja) 文言 40 種を `skills/` 全ファイルに対して検索し 0 件 | ✅ 一致 |
| (user-skills) en / ja の parity 検査と lint を通る | 同上 | 下記「実行したコマンドと結果」の 6 本すべて exit 0 | ✅ 一致 |

user-skills の 3 Scenario は spec 前文の宣言どおり安定 ID を持たない (core/ADR-0016 の対象外)。本表と検査 script が受け入れの記録になる。

### Skills の検査 (詳細)

診断表 (ヘッダに `Message` / `メッセージ` を持つ表) の 2 列目を 24 ファイルから機械抽出し、(a) 各セルが対応表の英語文言と一致すること、(b) 同じ skill / 同じ文書の en と ja でセル列が byte 一致することを検査した。

| 文書 | メッセージ列のセル数 | en / ja 一致 | 対応表との一致 |
|---|---|---|---|
| ksdialogs-ios/dialogs.md | 4 | ✅ | ✅ |
| ksdialogs-ios/loading.md | 4 | ✅ | ✅ |
| ksdialogs-ios/toast.md | 2 | ✅ | ✅ |
| ksdialogs-android/dialogs.md | 5 | ✅ | ✅ |
| ksdialogs-android/loading.md | 3 | ✅ | ✅ |
| ksdialogs-android/toast.md | 3 | ✅ | ✅ |
| ksdialogs-maui/dialogs.md | 6 | ✅ | ✅ |
| ksdialogs-maui/loading.md | 5 | ✅ | ✅ |
| ksdialogs-maui/toast.md | 3 | ✅ | ✅ |
| ksdialogs-kmp/dialogs.md | 5 | ✅ | ✅ (注) |
| ksdialogs-kmp/loading.md | 1 | ✅ | ✅ |
| ksdialogs-kmp/toast.md | 1 | ✅ | ✅ |

注: `ksdialogs-kmp/dialogs.md` の結果型不一致の行は `The result value type does not match (expected: {TypeName} / actual: {TypeName}).` で、対応表の `{expected}` / `{actual}` が両方 `{TypeName}` になっている。これは user-skills spec の「型名の埋め込みは en / ja とも `{TypeName}` のプレースホルダで書く (現行の `{型名}` を置き換える)」の指示どおりで、現行版も両方 `{型名}` だった。乖離ではない。

- 診断表の外で本文に引用されている個別メッセージ (KMP dialogs.md の iOS 補足 3 件) も対応表の英語に置き換わっている。diff の追加行からバッククォート引用を全抽出して照合し、**対応表に無い引用 0 件**
- 「メッセージは現在の実装値で、安定 API ではない」の 1 文は 24 ファイルすべてに 1 回ずつ存在する (en `not a stable API` / ja `安定した API ではない` を各 12 ファイルで 1 件ずつ確認)
- `{型名}` の残存 0 件 / `{TypeName}` 76 件
- en と ja の diff 行数が 12 ペアすべてで一致 (例: kmp/dialogs は双方 8 追加 / 6 削除) — 片側だけの追随がない
- `skills/.manifest.json` は未変更 (`git status` に現れない)。tasks 5.4 の指示どおり

---

## 追加検査

### tasks.md の完了状態

- 変更は全行チェックボックスのみ (本文の書き換えなし)。**虚偽チェックなし** — 22 項目すべてを軸 A / 軸 B の対応表と実行結果で裏付けた
- **6.2 は未チェックのまま**。deviation.md が「既定実行ではなく `--specs` で本 change の specs に絞った実行の終了コード 0 を受け入れ条件とした」と記録しており、実測でも既定実行の終了コード 1 は本 change 着手前から未実装の別 change (`add-kmp-typed-show`) の spec 由来の未網羅 20 件が原因で、`DM-*` は 1 件も含まれない (下記コマンド結果)。**合意済み差分として受け入れ、乖離としない**。チェックを付けずに deviation で説明した形は虚偽より安全な側で、判定に影響しない

### 逆流検査 (足場アーティファクトの書き換え)

`git status --short` に `proposal.md` / `specs/**` は現れない。`tasks.md` の diff はチェックボックス 22 行のみ。**逆流なし**。

### 未記録乖離

対応表の ❌ は 0 件。diff の全 66 ファイルを Scenario / deviation と突き合わせ、**未記録の乖離なし**。

deviation.md の 4 件はいずれも実測と整合する:

| deviation | 検証結果 |
|---|---|
| 文言依存テストが MAUI にも 3 箇所あった | `maui/macios/native/KsDialogsMauiBridgeTests/Support/BridgeTestFailure.swift:9` の照合定数 1 件、`maui/KsDialogs.Maui.Tests/BridgeContentSupplyTests.cs:161,188` の部分一致 2 件を確認。いずれも旧文言のままなら Requirement 達成時に必ず失敗する assertion で、追随は必然。**他に見落としが無いこと**を確認するため、対応表の ja 文言 22 パターンをテストソース全体に検索した — 残った日本語リテラルはすべてテストが自分で投げる例外文言・assertion の失敗説明・テスト用 fake gateway の文言で、ライブラリの出力を照合するものは無い (proposal の Non-Goals に合致) |
| tasks 6.2 の判定方法 | 上記のとおり実測で再現 |
| [付随修正] `kasane/handbook/cross/test-execution.md` の件数表 5 行 | 実行結果 (下表) と一致 |
| [付随修正] 同ファイルの本文 3 種 | `iosSimulatorArm64Test` の節の引用が `No screen is available to present the Dialog.` になり、件数 2 箇所 (31 / 7) と `timestamp: 2026-09-07` が実測と一致 |

### 付随修正の網羅

diff にあって Scenario に対応しない変更は `kasane/handbook/cross/test-execution.md` のみで、deviation.md に `[付随修正]` として記録済み。`scripts/scenario-id-coverage.py` の除外登録は tasks 6.0 の本務であり付随修正ではない。

### UI 変更

なし (`ui/` アーティファクトを持たない change)。

---

## 実行したコマンドと結果

すべてリポジトリルート (本 change の作業ツリー) で実行した。

### 静的検査

| コマンド | 結果 |
|---|---|
| tasks 6.4 の日本語リテラル grep (パイプライン全文は `tasks.md:6.4`) | **0 行** — `verification/japanese-literal-grep/grep.log` の記録を再現 |
| `python3 scripts/scenario-id-coverage.py --specs kasane/changes/localize-dialog-error-messages/specs` | exit 0 / 「Scenario ID 14 件・検出 10 件・除外 4 件・未網羅なし」 |
| `python3 scripts/scenario-id-coverage.py` (既定) | exit 1 / 未網羅 20 件。**全件が `kasane/changes/add-kmp-typed-show/specs/` 由来** (`PB-KT-*` 14 / `LD-KT-02` / `TS-KT-01,02` / `PB-KS-01` / `LD-KS-01` / `TS-KS-01`)。`DM-*` は 0 件。deviation の説明と一致 |
| `python3 scripts/comment-policy-lint.py` | exit 0 / 「合計: 0 ファイル / 禁止 0 件 (検査対象 949 ファイル)」 |
| `python3 scripts/local-path-lint.py` | exit 0 |
| `python3 scripts/identity-lint.py` | exit 0 |
| `python3 .agents/skills/docs-refresh/scripts/code-block-parity-check.py` | exit 0 /「code blocks byte-identical」 |
| `python3 .agents/skills/docs-refresh/scripts/heading-parity-check.py` | exit 0 /「en/ja heading structure OK」 |
| `python3 .agents/skills/docs-refresh/scripts/link-resolution-check.py` | exit 0 /「All internal links resolve」 |
| `python3 .agents/skills/docs-refresh/scripts/frontmatter-check.py` | exit 0 /「frontmatter OK」 |

link-resolution-check は `DOCS_REFRESH_TARGETS` 未指定だと `/tmp/docs-refresh-targets.txt` の残骸 (別プロジェクトの `kssettingsview-*` を並べたもの) を読んで MISSING を並べる。`python3 .agents/skills/docs-refresh/scripts/targets-list.py` で本リポジトリの 70 件を生成し直して実行した結果が上記。**本 change に起因する未解決リンクは無い**。

### テスト (全 7 ルート、絞り込みなしの全件実行)

| ビルドルート | コマンド | 結果 | handbook の記載 |
|---|---|---|---|
| ios/ | `xcodebuild test -scheme KsDialogs -destination 'platform=iOS Simulator,id=<検証用に新規 boot した iPhone 17 / iOS 26.5>'` | **277 tests / 50 suites 成功** (`** TEST SUCCEEDED **`) | 277 ✅ |
| android/ | `./gradlew test --rerun-tasks` | **68 tests / 0 failures / 0 errors** (`BUILD SUCCESSFUL`) | 68 ✅ |
| android/ (instrumented) | `ANDROID_SERIAL=<Pixel 6a / API 36> ./gradlew connectedDebugAndroidTest --rerun-tasks` | **333 tests / 0 failures / 0 errors** (`:ksdialogs` 294 (skip 1) + `:ksdialogs-compose` 39。`BUILD SUCCESSFUL`) | 333 ✅ |
| kmp/ | `./gradlew allTests --rerun-tasks` | **96 tests / 0 failures / 0 errors** (`BUILD SUCCESSFUL`) | 96 ✅ |
| maui/ | `dotnet test` | **155 合格 / 0 失敗 / 0 スキップ** | 155 ✅ |
| maui/android/native/ | `./gradlew :ksdialogs-maui-bridge:test --rerun-tasks` | **31 tests / 0 failures / 0 errors** (`BUILD SUCCESSFUL`) | 31 ✅ |
| maui/macios/native/ | `xcodebuild test -project KsDialogsMauiBridge.xcodeproj -scheme KsDialogsMauiBridge -destination 'platform=iOS Simulator,id=<同上>'` | **7 tests / 4 suites 成功** (`** TEST SUCCEEDED **`) | 7 tests / 4 suites ✅ |

- iOS 系 2 ルートは、稼働中の Simulator を流用せず検証用に新規作成・boot した端末 (iPhone 17 / iOS 26.5) を id 指定で使った
- instrumented は tasks 6.1 の「1 台以上」に従い実機 1 台 (Pixel 6a / API 36) で実行した (handbook の 333 は 1 台分の件数)。**先に API 35 のエミュレータで試したときは `PB_SB_02_ステータスバーのみ非表示の画面を引き継ぐ` (`android/ksdialogs/src/androidTest/.../DialogSystemBarsTests.kt:94` — 提示先のホスト側ステータスバーが隠れたままにならない) が 1 件失敗した (294 中 1 failures、`:ksdialogs-compose` は未到達)**。本 change の変更範囲 (文言リテラルのみ) とこの検査に接点は無く、instrumented のテストソースにライブラリ文言を照合する assertion は 1 件も無い (下記の照合で確認)。API 35 は handbook `cross/test-execution.md` が実測として記録している API レベル (29 / 33 / 36) の外であり、環境依存の失敗として扱い、記録済みの API レベルである API 36 で全件成功を確認した。**API 35 での挙動差は本 change とは別件の観測**として残す
- 追加 Scenario のテストが実際に走ったことを結果ファイルで確認: `DM-AN-01` (`TEST-jp.kamusoft.ksdialogs.DialogExceptionMessageTests.xml`)、`DM-KM-01` / `DM-KM-02` / `DM-KM-03` × 2 (KMP の JUnit XML に 4 件)、`DM-MA-03` (macios bridge の実行ログ)、`DM-IO-01` / `DM-IO-02` / `DM-MA-01` / `DM-MA-02` / `DM-MA-04` (各ルートの成功件数と `scenario-id-coverage.py` のテスト宣言検出)
- tasks 4.8 (MAUI iOS 面のビルド): `dotnet test` の過程で `KsDialogs.Binding.iOS` と `KsDialogs.Maui` の `net10.0-ios` ターゲットがビルド成功しており、bridge の Swift 変更が binding に追随している
- 文言を照合する assertion の所在を全ルートのテストソースで洗い出した (対応表の英語 14 パターンを検索): `ios/Tests/KsDialogsTests/DiagnosticMessageTests.swift` 9 件、`android/ksdialogs/src/test/.../DialogExceptionMessageTests.kt` 5 件、`kmp` の iosTest 4 件 / androidHostTest 1 件、`maui/KsDialogs.Maui.Tests` の 2 ファイルで 10 件、macios bridge 3 件、android bridge 1 件。**instrumented (`androidTest`) と Compose のテストにはライブラリ文言を照合する assertion が 1 件も無い**

---

## 判定

**VALID**

- 対応表 61 箇所すべてが実装のリテラルと一致。対応表に無い訳語の混入なし
- 全 14 Scenario (ID 付き) + user-skills の 3 Scenario に実装とテスト (または合意済みの静的 grep 受け入れ) が対応。❌ 0 件
- tasks.md の虚偽チェックなし。未チェックの 6.2 は deviation で合意済みの判定方法に置き換わっており、実測でも本 change 起因でないことを確認
- 足場アーティファクト (proposal / specs) の逆流なし
- 未記録乖離なし。deviation.md の 4 件はすべて実測と整合
- 全 7 ビルドルートのテストが全件成功し、handbook `cross/test-execution.md` の更新後の件数 (277 / 68 / 333 / 96 / 155 / 31 / 7) と一致

補足 (判定には影響しないが記録する): instrumented を API 35 のエミュレータで試したときに `PB_SB_02` が 1 件失敗した。本 change の変更範囲と接点が無く、handbook が記録している API レベル (29 / 33 / 36) の外での観測なので、**別件の観測**として残す。追う場合はこの change ではなく独立した調査になる。
