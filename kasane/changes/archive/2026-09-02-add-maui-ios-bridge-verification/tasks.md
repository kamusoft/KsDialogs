# Tasks: add-maui-ios-bridge-verification

実測の記録は `verification/` に残す (SDK 内部ターゲットへの依存箇所と、テスト実行体で提示先が得られたことの証跡)。生ログは handbook の規約どおり sanitize してから置く。

## 1. プローブ (先頭で実施 — 結果次第で以降の形が変わる)

- [x] 1.1 bridge xcodeproj にテスト用ホストアプリ target `KsDialogsMauiBridgeTestHost` (最小の UIKit アプリ。シーン 1 つ・空の root view controller) と、それを Host Application とするユニットテスト標的 `KsDialogsMauiBridgeTests` (Swift Testing) を追加し、scheme `KsDialogsMauiBridge` の TestAction に結線する。framework の scheme のビルド対象 (binding の `_BuildXcodeProjects` が呼ぶ側) にホストアプリを混ぜない。ios/ の Swift パッケージ (ローカルパッケージ参照) がテスト標的から解決でき、`xcodebuild test -scheme KsDialogsMauiBridge -destination 'platform=iOS Simulator,name=<機種名>'` で走ることを確認する。標的の成立確認用に最小のテスト 1 本を置き、Swift Testing の `Test run with N tests` 行で **1 件以上**が実行されたことを読む (0 件の偽 green を排除 — handbook/cross/test-execution) (→ Scenario BV-MA-06)
- [x] 1.2 提示先確保の再確認: ホストアプリ内で、`UIApplication.shared.connectedScenes` に前面アクティブなシーンがあり、ホストの key window (またはテストが作った window) を `ApplicationKeyWindowProvider` が返し、公開 init の `Dialog()` で実際に提示が起きる (提示先不在の通知にならない) ことを確認する。結果を `verification/presentation-host-probe.md` に追記する (提案段階のホストなしプローブは不成立 — 同ファイル)。**不成立なら以降のタスクに進まず、ユーザーに諮る** (deviation.md に記録) (→ BV-MA-01〜03・07 の GIVEN の成立条件)

## 2. bridge のテスト (BV-MA-01〜03)

- [x] 2.1 テスト支援: nil を返す供給関数と、通知を記録する受け口 (Dialog の閉鎖通知 / Loading の完了通知) を用意する。ios/ のテスト支援 (`DialogTestWaiting` 等) は別モジュールで参照できないため、必要最小限を bridge テスト側に置く (→ Requirement: 中身なしの供給は互換面の失敗として呼び出し側へ届く)
- [x] 2.2 `[BV-MA-01]` Dialog: nil 供給で提示されず、閉鎖通知がエラー (`contentUnavailable`) としてちょうど 1 回届く (→ Scenario BV-MA-01)
- [x] 2.3 `[BV-MA-02]` Loading 表示形 (`showContent:completion:`): nil 供給で表示されず、完了通知に `contentUnavailable` が理由として 1 回届く (→ Scenario BV-MA-02)
- [x] 2.3b `[BV-MA-07]` Loading スコープ形 (`startContent:action:completion:`): nil 供給で表示されず、渡した処理が呼ばれず、完了通知に `contentUnavailable` が 1 回届く (→ Scenario BV-MA-07)
- [x] 2.4 `[BV-MA-03]` Toast: 公開面 `MauiToastBridge.show` を通し、別の Toast (中身ありの供給) を表示中に nil 供給の表示を要求する。観測点はホストアプリの key window (提示先として解決される window) の view 階層 — Toast の器は key window に直接載る (`KeyWindowToastPresentationSurface`) ため、要求の前後で器の枚数が増えないこと (失敗分は追加されない) と、先に表示した Toast の器 (同一インスタンス) が残っていることを見る。MAUI Android の同役テストは「器が捕まえられる失敗になる」までしか見ていないので、iOS 側はこの器の観測まで踏み込む (→ Scenario BV-MA-03)
- [x] 2.5 テストの検出力: 供給失敗を投げない形に一時的に戻すと 2.2〜2.4 (2.3b 含む) が fail することを確認し、記録する (lessons/process L-001 の「修正前に fail すること」) (→ 同上)
- [x] 2.6 scenario-id-coverage: `DEFAULT_TEST_GLOBS` に bridge テスト標的の置き場 (`maui/macios/native/*Tests`) を追加し、BV-MA-04〜06 を理由付きで `DEFAULT_ALLOW_MISSING` に登録する。`--selftest` と既定実行で「未網羅なし」を確認 (→ Requirement: bridge のテスト標的は ios/ と同じ方式で全件実行できる)

## 3. Binding のビルド連携 (BV-MA-04 / 05)

- [x] 3.1 `maui/macios/KsDialogs.Binding.iOS/KsDialogs.Binding.iOS.csproj`: 資源パッケージ再生成 (`_CreateBindingResourcePackage`) の入力に、`_BuildXcodeProjects` が出力した xcframework の中の実バイナリを足すターゲットを追加する。既存の `_AdjustKsBridgeXcodeProjectInputs` と同じ流儀 (SDK の公開ターゲット名への AfterTargets/BeforeTargets と item 補正) で書き、SDK のどの target / item に依存したかをコメントに明記する (→ Requirement: bridge の更新は Sample の iOS ビルドに追随する)
- [x] 3.2 実測: bridge の Swift を変えたとき資源パッケージが再生成され、変えないときスキップされることをタイムスタンプ (`bin/.../<AssemblyName>.resources` と `.stamp`) で確認し `verification/incremental-build.md` に記録する (→ BV-MA-04 / 05 の Binding 側)

## 4. Sample のビルド連携 (BV-MA-04 / 05)

- [x] 4.1 `samples/maui/KsDialogs.Sample.Maui/KsDialogs.Sample.Maui.csproj`: ネイティブリンクの入力 (`_LinkNativeExecutableInputs`) に bridge の静的 framework の実バイナリを足すターゲットを iOS 条件付きで追加する。モノレポの ProjectReference 構成のための開発用配線であること (NuGet 利用者には不要) をコメントに明記する (→ Requirement: bridge の更新は Sample の iOS ビルドに追随する)
- [x] 4.2 実測 `[BV-MA-04]`: 成果物を捨てずに bridge の Swift だけを変えて Sample をインクリメンタルビルドし、`nativelibraries/<AppName>` が更新され、.app の実行ファイルに変更後のシンボル (`nm` / `strings` で確認できる mangled 名) が含まれることを記録する (→ Scenario BV-MA-04)
- [x] 4.3 実測 `[BV-MA-05]`: 変更なしで再ビルドし、資源パッケージ再生成とネイティブリンクがスキップされる (ビルド時間が従来と同等) ことを記録する (→ Scenario BV-MA-05)
- [x] 4.4 Android TFM (`net10.0-android`) のビルドが 4.1 の追加で壊れないことを確認する (→ 同上)

## 5. 仕上げ

- [x] 5.1 handbook の全件実行 (test-execution): ios/ / maui/ / maui/android/native/ と新設 bridge 標的を回し、件数を報告に併記する。負のコンパイル検証は本変更が公開面に触れないため対象外
- [x] 5.2 蒸留が handbook (test-execution 件数表・runtime-behavior-verification) と maui/ADR-0003 の現行照合へ写す値 (bridge 標的の実行コマンドと実測件数・依存した SDK target / item 名・.app シンボル確認の手順) を `verification/` に揃えておく。足場 (proposal / specs) は凍結のため書き換えない
