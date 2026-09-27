# Tasks: revisit-dialog-margin-default

## 1. 契約の既定値を 0 にする

- [ ] 1.1 iOS: `ios/Sources/KsDialogs/Contract/DialogOptions.swift` の dialogMargin の既定値を全辺 0 にし、doc コメントと `ADR-0039` の参照を直す。NaN の辺のフォールバック (`ios/Sources/KsDialogs/Layout/DialogLayout.swift:75`) が `DialogOptions()` から引いたままで 0 になることを確認する (→ Requirement: メタ属性セットと既定値 / Scenario: 既定の options の余白は全辺 0・無効値の正規化)
- [ ] 1.2 Android: `android/ksdialogs-core/src/main/kotlin/jp/kamusoft/ksdialogs/DialogOptions.kt` の既定値と KDoc を全辺 0 にする。`DialogLayout.kt:49` の `DEFAULT_MARGIN` (契約とは別の 24 の二重定義) をやめ、iOS と同じく契約の既定値から引く (→ Requirement: メタ属性セットと既定値 / Scenario: 既定の options の余白は全辺 0・無効値の正規化)
- [ ] 1.3 MAUI: `maui/KsDialogs.Maui/Contract/DialogOptions.cs` の `DialogMargin` の既定値と XML doc を 0 にする。Android bridge `maui/android/native/ksdialogs-maui-bridge/src/main/kotlin/jp/kamusoft/ksdialogs/maui/MauiDialogAttributes.kt:51-60` の 4 辺の既定値を 0 にする。iOS bridge (`maui/macios/native/KsDialogsMauiBridge/MauiDialogAttributes.swift`) と添付プロパティ (`maui/KsDialogs.Maui/Presentation/DialogAttachedProperties.cs`) が契約の既定値から引いて追従することを確認する (→ Requirement: メタ属性セットと既定値 / Scenario: 既定の options の余白は全辺 0)

## 2. 既定 Toast のデフォルト View に余白 24 を持たせる

- [ ] 2.1 Android: `ToastCoordinator.kt` の `ToastContentRequest.Builtin` の分岐で作る `ToastDefaultContentView` に、dialogMargin 全辺 24 の options を添付する。値は `ToastPlacementDefault` と同じくデフォルト View の性質として 1 か所に定数で持ち、`ADR-0039` を参照する (→ Requirement: 配置属性と ToastStyle / Scenario: TS-AT-04・05・06)
- [ ] 2.2 iOS: `ToastCoordinator.swift` の `.builtin` の分岐で作る `ToastDefaultContentView` に同じ options を添付する (定数の置き方は 2.1 と同じ) (→ Requirement: 配置属性と ToastStyle / Scenario: TS-AT-04・05・06)
- [ ] 2.3 MAUI・KMP の既定 Toast が Native のデフォルト View を通ることを確認し、両者に変更を入れない (`maui/KsDialogs.Maui/Internals/ToastGateway.cs:70`、`kmp/ksdialogs-kmp/src/iosMain/kotlin/jp/kamusoft/ksdialogs/kmp/IosToastGateway.kt:26`、`kmp/ksdialogs-kmp/src/androidMain/kotlin/jp/kamusoft/ksdialogs/kmp/AndroidToastGateway.kt:15`) (→ Requirement: 配置属性と ToastStyle)

## 3. 共通ケース表

- [ ] 3.1 `core/layout-spec/cases.json`: `defaults` の説明を `dialogMargin=0全辺` にし、`comment` に改訂の行を足す (core/ADR-0039)。期待値を直す: C23 を y=510 に、C24 を x=120・y=530 に、C26 を x=120・y=590 にする。いずれも規則から再導出して検算する。C19 は中央配置なので不変。C19 と C24 の note の「margin24」を直し、C23 の note に「何も添付しないカスタム View の Toast」を書く (→ Requirement: メタ属性セットと既定値 / Scenario: 属性を指定しない呼び出しはコンパイル互換を保つ・余白を指定しない末尾寄せは基準領域の端に接する。Requirement: 配置属性と ToastStyle / Scenario: TS-AT-07)

## 4. テスト

- [ ] 4.1 既定値を直接確かめるテストを 0 にする: Android unit `DialogAttributeDefaultsTests.kt` (NaN と +Infinity の辺が 0 になる検査を含む)、iOS `DialogLayoutAttributeDefaultsTests.swift`、MAUI `DialogLayoutPassthroughTests.cs:120`。Android bridge の `MauiDialogLayoutPassthroughTests.kt:81` (橋渡しの既定 = 契約の既定) は 1.3 で通ることを確認する (→ Scenario: 既定の options の余白は全辺 0)
- [ ] 4.2 NaN の辺を既定値前提で計算しているテストを直す: iOS `DialogAttributeSupplyTests.swift:247-259` と Android instrumented `DialogAttributeSupplyTests.kt:117-139` (どちらも左辺 NaN → 0 で x を再計算。Android は x=72 → 60。説明コメントも直す) (→ Scenario: 無効値の正規化)
- [ ] 4.3 既定値 24 を前提に期待値を計算しているテストを直す: Android instrumented `DialogCurrentPageTests.kt`・`DialogPresentedWindowTests.kt`、Compose `ComposeCurrentPageTests.kt`、iOS `Support/DialogCurrentPageStage.swift`・`LoadingAttributeTests.swift`・`LoadingTypedShowTests.swift`・`ToastAttributeTests.swift`・`ToastTypedShowTests.swift`。MAUI の実機検証ホストの余白定数 (`maui/KsDialogs.Maui.PlacementHost/Platforms/{Android,iOS}/PlacementMeasurement.cs`) を 0 にする。直した後に、カスタム Loading・カスタム Toast・既定ローディングを placement 引数の末尾寄せで出して下端を測る検査が両 Native に無ければ足す (→ Scenario: 余白を指定しない末尾寄せは基準領域の端に接する・Loading とカスタム Toast も余白の既定値 0 で置かれる)
- [ ] 4.4 Android の Toast / Loading の instrumented テストのうち、数値 24 を書かずに既定値へ暗黙に依存しているものを、全テストの実行で洗い出して直す (→ Scenario: Loading とカスタム Toast も余白の既定値 0 で置かれる)
- [ ] 4.5 既定 Toast の Scenario テスト TS-AT-04〜07 を iOS と Android の両方に同じ ID で足す (両 Native 同名 — core/ADR-0016)。TS-AT-05 は、デフォルト View の最大幅の制限 (両 Native とも取り付け先の幅に対する比率 — `ToastDefaultContentView`) よりも余白の制約が先に効く狭い幅の可視領域を測定ハーネスで作り、左右の端がちょうど 24 内側にあることを測る。通常の幅では最大幅の制限が先に効き、余白の添付漏れを見分けられないため (→ Requirement: 配置属性と ToastStyle / Scenario: TS-AT-04・05・06・07)
- [ ] 4.6 `kasane/handbook/cross/test-execution.md` に従って全形態のテストを実行する (iOS / Android unit と instrumented / Compose / MAUI の 3 実行 / KMP)。あわせて `scripts/scenario-id-coverage.py` で TS-AT-04〜07 の網羅を確かめる (→ 全 Requirement)

## 5. Sample (UI 変更 — ksn-ui)

- [ ] 5.1 4 ルートの Layout Dialog パネルに `Margin` の行を承認モックどおりに足す。初期値は 0。パネルの状態を ViewModel に載せ、各ルートの `SampleDialogRegistration` で中身に添付する (全辺 0 の明示を、パネルの値の添付に置き換える)。対象は iOS (`samples/ios/KsDialogsSample/`)、Android (`samples/android/app/`)、KMP (`samples/kmp/shared/` の ViewModel・presenter と、`androidApp` / `iosApp` の登録)、MAUI (`samples/maui/KsDialogs.Sample.Maui/`) (→ Requirement: Layout Dialog のパネルで余白を選べる / Scenario: 初期値のまま出すと基準領域の端に接する・余白を変えると次の表示から効く)
- [ ] 5.2 余白の設定はタブの切り替えで保つ (配置・移動量・基準領域と同じ置き場) (→ Scenario: タブを切り替えても余白は保たれる)
- [ ] 5.3 余白の行の読み上げ (読み上げ名・役割・状態) を ui/brief.md の表どおりに 4 ルートで一致させ、accessibility tree で確かめる (→ Requirement: Layout Dialog のパネルで余白を選べる)
- [ ] 5.4 mock との視覚照合: 4 ルートでモックの 3 状態 (パネル初期 / Current page + End/End + 余白 0 / 同 余白 24) を撮り、承認モック (`ui/mock/approved.png`) と照合して `ui/verification/` と brief.md に記録する。撮影は config `ui.screenshot` の手順 (デモ駆動の起動引数 `layout-dialog`) で行う (→ Requirement: Layout Dialog のパネルで余白を選べる)
