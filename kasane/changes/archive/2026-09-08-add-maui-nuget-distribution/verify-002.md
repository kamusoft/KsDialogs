# 検証: add-maui-nuget-distribution (verify-002)

- 対象: HEAD (`c844740`) に対する未コミットの作業ツリー全体
- デルタスペック: `kasane/changes/add-maui-nuget-distribution/specs/maui-binding/spec.md` (Requirement 3 / Scenario 8)、`kasane/changes/add-maui-nuget-distribution/specs/maui-nuget-distribution/spec.md` (Requirement 7 / Scenario 15) — 合計 **Requirement 10 / Scenario 23**
- 合意済み差分: `kasane/changes/add-maui-nuget-distribution/deviation.md` (2 件 + `[付随修正]` 1 件)
- 前回: `kasane/changes/add-maui-nuget-distribution/verify-001.md` (INVALID。❌ 1 件 + 注記 1 件)
- **判定: VALID** (❌ 0 件。verify-001 の ❌-1 と注記はいずれも証跡・規範文書の修正で解消)

---

## 1. 再検証の範囲と引き継ぎの根拠

本検証は verify-001 の ❌ 1 件と注記 1 件に対する修正の確認を主眼とし、verify-001 で ✅ / ⚠️ とした 22 件は引き継いだ。引き継ぎの前に、その根拠となる「ソース・テスト・足場が verify-001 の時点から変わっていないこと」を次の 3 つで確かめた。

1. **足場の逆流なし**: `proposal.md` / `design.md` / `specs/` は propose 段階の `1762a7f` 以降 commit がなく、`git status` で未コミット変更も 0 件。`tasks.md` の diff は 26 行の `[ ]` → `[x]` のみで本文の書き換え 0 行 (チェックボックス以外の差分行を抽出して 0 件を確認)
2. **mtime は判定に使えないことの確認**: `find -newer verify-001.md` は多数のソースを拾うが、実時刻を見ると git 追跡下の変更済みファイルが**すべて同一の 22:07:46** に固まっており、新規追加ファイル (`maui/Directory.Build.props` は 20:29:15、`.../MauiDialogLoadingContentSupplyTests.kt` は該当なし) は動いていない。個別編集ではなく一括 touch の痕跡であり、mtime は変更の指標にならないと判断した
3. **内容による引き継ぎ判定 (mtime の代替)**: verify-001 が根拠として挙げた行参照 15 か所を実物で引き当て、**すべて記載どおりの内容**であることを確認した (`DialogException.cs:59` = `public sealed class ViewCreationFailed`、`KsDialogsServiceCollectionExtensions.cs:213` = `CreateView` / `:228` = `catch (Exception thrown)`、`DialogDependencyInjectionTests.cs:109/142/169` = `MB_MA_11` / `MB_MA_13` / `MB_MA_16`、`LoadingDependencyInjectionTests.cs:92` = `MB_MA_11`、`ToastDependencyInjectionTests.cs:76` = `MB_MA_12`、`DiagnosticMessageTests.cs:20` = `DM_MA_01`、`MauiDialogBridge.kt:120` / `MauiLoadingBridge.kt:247` = `?: error("The MAUI side could not create the presentation content.")`、`MauiDialogLoadingContentSupplyTests.kt:59/82` = MB-MA-15 の 2 件、`buildTransitive/KsDialogs.Maui.targets:26` = `<Error Condition=...>`、`Directory.Packages.props:17` = `Microsoft.Maui.Controls 10.0.20`)。行番号ごと一致するため、内容は verify-001 の検査時点と同一

加えてテスト 2 ルートは本検証で自分で再実行した (下記 4.)。

---

## 2. verify-001 の ❌ と注記の解消確認

### ❌-1 (解消 → ✅): Android 互換面のテスト件数の記録

- **Scenario**: 「3 テストルートの全件実行」(`kasane/changes/add-maui-nuget-distribution/specs/maui-nuget-distribution/spec.md:107`)
- **本検証の実測**: `cd maui/android/native && ./gradlew :ksdialogs-maui-bridge:test --rerun-tasks` を `--rerun-tasks` 付きで再実行 (BUILD SUCCESSFUL、25 tasks executed)。`ksdialogs-maui-bridge/build/test-results/testDebugUnitTest/*.xml` 9 ファイルを自分で集計し **tests=34 / failures=0 / errors=0 / skipped=0**。クラス別は 2 / 6 / 4 / 2 / 5 / 4 / 6 / 1 / 4 = 34 で verify-001 の実測と一致
- **修正後の記録**:
  - `evidence/test-routes/README.md` の結果表が「Android 互換面 34 tests / 失敗 0」。内訳が「変更前 31 件 + MB-MA-15 の 2 件 + レビュー指摘 (second-opinion-code-001) で追加した DM-MA-04 の 2 件目 1 件 = 34」となり、**verify-001 が数え落としを指摘した DM-MA-04 の 1 件が内訳に入っている**。さらに「初回計測 (修正サイクル前) は 33 件で、修正サイクル後に再計測して更新 (2026-09-08)」と経緯が併記されている
  - `evidence/test-routes/test-routes.txt` の集計行が `JUnit XML 集計: tests=34 failures/errors=0 skipped=0 (修正サイクル後の再計測 2026-09-08。修正サイクル前は tests=33)`
- **判定**: 記録と実測が一致し、修正サイクル前の値だった経緯も残っている → **✅ 一致**

### 注記-1 (解消): `IsPackable` の括弧書き

- **修正後**: `evidence/pack-verification/README.md:131-132` が「`-getProperty:IsPackable` はいずれも `false` (テスト 2 プロジェクトは csproj で `IsPackable=false` を明示しており、`maui/Directory.Build.props` の既定値も `false`)」
- **本検証の実測**: `maui/KsDialogs.Maui.Tests/KsDialogs.Maui.Tests.csproj:9` と `maui/KsDialogs.Maui.ApiSurfaceCheck/KsDialogs.Maui.ApiSurfaceCheck.csproj:18` がともに `<IsPackable>false</IsPackable>` を持ち、`maui/Directory.Build.props:29` にも既定の `<IsPackable>false</IsPackable>` がある。**記述と実物が一致**

### 注記-2 (解消): handbook の実測表

- **修正後**: `kasane/handbook/cross/test-execution.md:25-26` が `maui/` = 160 tests / 0 failures (2026-09-08)、`maui/android/native/` = 34 tests / 0 failures (2026-09-08)。本文 `:147` も「この 34 件が MAUI Android 経路の…保証を持つ」に更新。diff は当該 3 行のみで、他ルートの実測値・実測日には触れていない
- **本検証の実測**: 160 / 34 とも自分の再実行で一致 (下記 4.)
- **記録**: `deviation.md` に `[付随修正]` 行として、同文書が「実態が変わったら実測で更新する」と定めることを理由に記録済み → **付随修正として記録あり、未記録乖離ではない**

---

## 3. 対応表 (verify-002 時点)

### 3.1 `specs/maui-binding/spec.md`

| Requirement | Scenario | 実装 | テスト / 証跡 | 状態 |
|---|---|---|---|---|
| View 生成失敗の構成ミスとしての報告 | [MB-MA-11] 依存を解決できない View は ViewCreationFailed で失敗する | `maui/KsDialogs.Maui/Hosting/KsDialogsServiceCollectionExtensions.cs:228` | `maui/KsDialogs.Maui.Tests/DialogDependencyInjectionTests.cs:109`、`maui/KsDialogs.Maui.Tests/LoadingDependencyInjectionTests.cs:92` | ✅ 一致 (verify-001 から引き継ぎ) |
| 同上 | [MB-MA-12] Toast の 1 行登録の生成失敗は警告に残して 1 枚だけ破棄する | 同上 (`BridgeContentSupply.CreateOrDiscard` へ合流) | `maui/KsDialogs.Maui.Tests/ToastDependencyInjectionTests.cs:76` | ✅ 一致 (引き継ぎ) |
| 同上 | [MB-MA-13] 利用者コードの例外は包まれない | `maui/KsDialogs.Maui/Hosting/KsDialogsServiceCollectionExtensions.cs:213` | `maui/KsDialogs.Maui.Tests/DialogDependencyInjectionTests.cs:142` | ✅ 一致 (引き継ぎ) |
| 同上 | 公開例外面の compile 検査 (ID なし) | `maui/KsDialogs.Maui/Contract/DialogException.cs:59` | `maui/KsDialogs.Maui.ApiSurfaceCheck/DialogExceptionApiSurfaceChecks.cs`。本検証の `dotnet test` のビルド成功で再確認 | ✅ 一致 |
| 同上 | [MB-MA-14] Android の実機経路でも同じ型が届く | 上記 + `maui/KsDialogs.Maui/Platforms/Android/PlatformDialogGateway.cs:64,95` | 自動テストなし (`scripts/scenario-id-coverage.py:114` の allow に登録)。証跡 `evidence/view-creation-failure/` の 3 枚 (verify-001 で内容確認済み) | ✅ 一致 (引き継ぎ) |
| Android の managed/native 境界での失敗の受け止め | [MB-MA-15] Kotlin 互換面は中身なしを失敗経路へ合流させる | `maui/android/native/ksdialogs-maui-bridge/src/main/kotlin/jp/kamusoft/ksdialogs/maui/MauiDialogBridge.kt:120`、`.../MauiLoadingBridge.kt:247` | `maui/android/native/ksdialogs-maui-bridge/src/test/kotlin/jp/kamusoft/ksdialogs/maui/MauiDialogLoadingContentSupplyTests.kt:59,82`。本検証の 34 件実行に含まれ成功 | ✅ 一致 |
| 同上 | [MB-MA-16] 既存の失敗経路は変わらない | 変更なし | `maui/KsDialogs.Maui.Tests/DialogDependencyInjectionTests.cs:169` | ✅ 一致 (引き継ぎ) |
| Android binding の生成出力 | BG8401 の不在と static メンバーの可用性 (ID なし) | `maui/android/KsDialogs.Binding.Android/Transforms/Metadata.xml:30-42` | 証跡 `evidence/binding-warnings/README.md`。verify-001 の Release 再ビルドで BG8401 0 件。本検証の `dotnet test` のログでも BG8401 は出ず、出るのは BG8605 / BG8606 / BG8A00 (`.Companion` の `remove-node` 4 種) のみ | ⚠️ deviation 記録済み (`deviation.md` 1 件目) |

### 3.2 `specs/maui-nuget-distribution/spec.md`

| Requirement | Scenario | 実装 | テスト / 証跡 | 状態 |
|---|---|---|---|---|
| パッケージの共通メタデータと版の宣言元 | nuspec のメタデータと Version の既定・注入 | `maui/Directory.Build.props`、`maui/KsDialogs.Maui/KsDialogs.Maui.csproj:17-21` | 証跡 `evidence/pack-verification/README.md` (verify-001 で pack し直して実測) | ✅ 一致 (引き継ぎ) |
| 同上 | pack 対象の限定 | `maui/Directory.Build.props:29` の `IsPackable` 既定 false | 証跡 `evidence/pack-verification/README.md:131-132`。**本検証で記述の訂正を実物と突き合わせて確認** (上記 2. 注記-1) | ✅ 一致 |
| 同上 | MAUI 本体の版の引き下げ後のビルドとテスト | `maui/Directory.Packages.props:17` (10.0.20)、`samples/maui/KsDialogs.Sample.Maui/KsDialogs.Sample.Maui.csproj:24` | 本検証で `cd maui && dotnet test` を再実行し **160 件成功 / 失敗 0**。`Directory.Packages.props:17` の 10.0.20 も実物で確認 | ✅ 一致 |
| 3 パッケージの構成と内容 | 3 パッケージのローカル pack | 上記 csproj 3 件 | 証跡 `evidence/pack-verification/README.md` (verify-001 で同梱物を実測) | ⚠️ deviation 記録済み (`deviation.md` 2 件目) |
| 同上 | binding パッケージの同梱物と説明 | binding 2 件の csproj | 同上 | ⚠️ deviation 記録済み (`deviation.md` 2 件目) |
| 消費者からの導入 | ローカルフィードからの restore と Release ビルド | pack 設定一式 | 証跡 `evidence/consumer-verification/README.md` の 1.〜2. | ✅ 一致 (引き継ぎ) |
| 同上 | パッケージ経由の起動確認 | 同上 | 証跡 `evidence/consumer-verification/` の png 6 枚 (verify-001 で 2 枚を開いて README と照合) | ✅ 一致 (引き継ぎ) |
| 最低 OS 版のビルド時ガード | 要件未満の利用者アプリ | `maui/KsDialogs.Maui/buildTransitive/KsDialogs.Maui.targets:26` | 証跡 `evidence/consumer-verification/README.md` の 4-a / 4-b / 4-c | ✅ 一致 (引き継ぎ) |
| 同上 | 要件を満たす利用者アプリとリポジトリ内のビルド | 同上 | 証跡 `evidence/pack-verification/README.md` 末尾 と `evidence/consumer-verification/README.md` の 2.。本検証の `dotnet test` / Gradle ログにも `KSDLG` は出ていない | ✅ 一致 |
| 同上 | 非 platform TFM と outer build ではガードが動かない | `.../KsDialogs.Maui.targets:17` の Condition | 証跡 `evidence/consumer-verification/README.md` の 5-a / 5-b | ✅ 一致 (引き継ぎ) |
| 同上 | 要件の宣言元の一致 | facade / binding 2 件の csproj (すべて `$(KsDialogsMinAndroidApi)` / `$(KsDialogsMinIOSVersion)` 参照) | 証跡 `evidence/pack-verification/README.md` の評価値表 (Android 24.0 / iOS 17.0)。verify-001 で `git grep` により直書き 0 件を確認 | ✅ 一致 (引き継ぎ) |
| package README の表示 | README の同梱とリンク参照 | `maui/KsDialogs.Maui/KsDialogs.Maui.csproj:21,26`、`README.md` / `README_ja.md` | 証跡 `evidence/readme-links/README.md` (verify-001 で抽出スクリプト再実行、非 HTTP(S) 0 件) | ✅ 一致 (引き継ぎ) |
| MAUI Sample の両 OS 通しと MAUI テストルートの完了判定 | 両 OS の全デモ項目 | Sample のソース改変なし | 証跡 `evidence/sample-walkthrough/README.md` + png 各 25 枚 (verify-001 で枚数・md5 重複 0・1 枚の内容照合) | ✅ 一致 (引き継ぎ) |
| 同上 | 3 テストルートの全件実行 | — | 証跡 `evidence/test-routes/README.md` + `test-routes.txt`。**本検証で facade 160 / Android 互換面 34 を自分で再実測し記録と一致** (iOS 互換面 7 件は Swift 側に本変更の差分がないため証跡の記録で受け入れ) | ✅ 一致 (**verify-001 の ❌-1 が解消**) |
| MAUI 本体の版の追随ルール | 規範文書の記載 | `kasane/handbook/cross/local-development-setup.md:77-78` | verify-001 で本文を確認 (追随ルールと `-getProperty:MauiVersion` による確かめ方) | ✅ 一致 (引き継ぎ) |

**集計: Scenario 23 件 = ✅ 20 / ⚠️ 3 / ❌ 0**

---

## 4. 追加検査

| 検査 | 結果 |
|---|---|
| tasks.md の全タスク完了 | 全 26 タスクが `[x]`。verify-001 で対応表と突き合わせて虚偽チェックなしを確認済み。本検証で diff を再取得し、変更はチェックボックス 26 行のみで本文の書き換え 0 行 |
| 逆流検査 (足場の書き換え) | **なし**。`proposal.md` / `design.md` / `specs/` は `1762a7f` (propose 段階) 以降 commit なし・未コミット変更なし |
| 未記録乖離 | **なし**。verify-001 の ❌-1 は証跡の記録誤りとして修正済み。`deviation.md` の 2 件は対応表で ⚠️ として突き合わせ済み |
| 付随修正 | `deviation.md` に `[付随修正]` 1 行 (handbook `cross/test-execution.md` の実測表と本文の 31 → 34 / 155 → 160 更新)。**diff の実物と一致** (当該 3 行のみ)。verify-001 が挙げた他の非 Scenario 変更 3 件 (ViewModel の `private` → `internal`、`DM_MA_01` の 7 型化、`DM-MA-04` テストの 2 件目) はいずれも本変更 / 既存 Requirement の網羅でスコープ外ではない |
| UI 変更 | 本変更に `ui/` はない — 対象外 |
| テスト全件実行 | **本検証で 2 ルートを再実行**: facade `cd maui && dotnet test` → 合計 160 / 合格 160 / 失敗 0 / スキップ 0 (exit 0)。Android 互換面 `cd maui/android/native && ./gradlew :ksdialogs-maui-bridge:test --rerun-tasks` → JUnit XML 集計 tests=34 / failures=0 / errors=0 / skipped=0 (exit 0)。iOS 互換面は Swift 側に本変更の差分がないため証跡 `evidence/test-routes/test-routes.txt` の記録 (7 tests / 4 suites、失敗 0) で受け入れ |
| Scenario ID 網羅 | `python3 scripts/scenario-id-coverage.py` → 「結果: 未網羅なし」(exit 0)。ID を持たない Scenario 2 件の警告は verify-001 と同じく設計どおり |
| handbook の実測表との整合 | `kasane/handbook/cross/test-execution.md:25-26` の 160 / 34 (2026-09-08) が本検証の実測と一致。本文 `:147` の「この 34 件」も追随済み |

---

## 5. 判定

**VALID**。

- Scenario 23 件すべてが ✅ 一致 (20 件) または ⚠️ deviation 記録済み (3 件)。❌ は 0 件
- verify-001 の ❌-1 (Android 互換面 33 → 34) は証跡 2 ファイルの訂正で解消し、修正サイクル前が 33 件だった経緯も併記されている。本検証の独立実測 34 件と一致
- verify-001 の注記 2 件 (`IsPackable` の括弧書き、handbook の実測表) も解消し、handbook の更新は `deviation.md` に `[付随修正]` として記録されている
- 虚偽チェックなし・逆流なし・テスト全件成功 (160 / 34、失敗 0)

蒸留 (ksn-distill) へ進んでよい状態。
