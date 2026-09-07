# MAUI パッケージング (maui-packaging)

MAUI の NuGet 3 パッケージ (facade + binding 2 件) を nuget.org へ出せる形に配線し、配布物で公開面を固める前に済ませる小修正 (`BG8401`・`DialogException` 合流) と MAUI iOS のビルド環境整合を同梱する change フェーズ。

## 論点

### library-foundation phase-11-packaging から移送

- パッケージメタデータ — Package ID (cross/ADR-0005 の写像表: `KsDialogs.Maui` / `KsDialogs.Binding.iOS` / `KsDialogs.Binding.Android`)、ライセンス・icon・README・説明 (binding は「直接参照しない」を明示)
- nuget.org 固有のメタデータ要件・Release / trimming 構成の検証・Android の NuGet 経由実行時確認 (phase-10 PoC の未検証領域)
- MAUI iOS のビルド環境制約の解消 (phase-6 からの申し送り、2026-08-25): .NET for iOS 26.1.10502 が Xcode 26.1 を要求し現行環境 (Xcode 26.5) でビルド不可のため、add-model-binding-di では MAUI ルートの通し確認が Android 面のみになった (deviation 9)。workload / Xcode の整合を確立し、未実施の MAUI iOS 面 Sample 通し (Model Dialog 含む) を回収する

### 簡易起票からの合流 (2026-09-02、phase-11 から移送)

**MAUI Android binding の `warning BG8401` 6 件** (旧 fix-maui-binding-bg8401-warnings)

- `MauiDialogBridge.Companion` 等、Kotlin の companion object 由来の入れ子型名の重複でバインディング生成がスキップされる。add-loading の 4 ルート通し検証中に発見。API に実害は未確認だが、一般公開ライブラリとしてビルド出力をきれいに保ちたい
- 論点: スキップされた 6 型が .NET 側から必要か。不要なら Metadata.xml で抑制、必要なら Kotlin 側の命名変更

**MAUI 1 行登録の View 生成失敗を `DialogException` に合流させる** (旧 wrap-maui-view-creation-failure)

- `RegisterForDialog<TView, TViewModel>` が生成する factory (`KsDialogsServiceCollectionExtensions.cs` の `CreateView`) は TView を `ActivatorUtilities.CreateInstance` 相当で生成し、依存が DI で解決できないと素の `InvalidOperationException` が呼び出し元へ届く
- 他の構成ミス失敗は `DialogException` に統一されており (add-model-binding-di design Decision 6)、この経路だけ例外型が揃っていない。出典: add-model-binding-di review-001 Suggestion
- 論点: 包み直し先を既存の `ViewFactoryNotRegistered` にするか、生成失敗専用の分類を新設するか (新設は公開 enum / 例外型の追加 = 公開面変更)。元例外を InnerException として保持するか

### KsDialogs 固有

- MAUI 本体の下限版と TFM (KsSettingsView は `net10.0-android36.0` / `net10.0-ios26.0` と API 版付きになり、下限は検証済み版 10.0.70)。KsDialogs は maui/ADR-0002 の net10 TFM を前提に着手時に実測
- 最低 OS 版のビルド時ガード (`buildTransitive/` の .targets、KsSettingsView 診断 ID `KSSV0001`) を KsDialogs でも持つか (iOS 17 / Android 24、cross/ADR-0002)

## 決定事項

踏襲 (解決済み論点)。出典は maui/ADR-0004 (3 パッケージ構成、KsSettingsView maui/ADR-0025 と同型)、KsSettingsView phase-6 / phase-8 の決定事項と実測 (`../KsSettingsView/kasane/roadmaps/package-distribution/phases/phase-6-maui-packaging/agenda.md`)。

- facade + binding 2 件の 3 パッケージ。利用者が書くのは facade 1 点、binding は TFM 条件付き依存で推移。binding の pack は SDK 標準経路 (`IsPackable=true` のみ) で自作 pack MSBuild は足さない。facade → binding の依存は下限指定 (lockstep + 最小適用版解決で同版に揃う)
- メタデータは `maui/Directory.Build.props`、版は `Directory.Packages.props` (CPM) に集約。SourceLink + snupkg。`PackageReadmeFile` は facade のみでルート README を同梱 (画像・リンクは絶対 URL 化)。`PackageIcon` の同梱アイテムは `Directory.Build.targets` へ (props では `IsPackable` 評価前)
- 名前空間は cross/ADR-0005 で既に素の `KsDialogs`。追加の rename は不要
- 実測で判明した対処 (踏襲): iOS binding resource の manifest に発行マシンの絶対パスが乗るため pack は CI で行う / .NET Android SDK の自 assembly 用 aar が nupkg に入り利用者側で `XA4301` → `_IncludeAarInNuGetPackage` の `AfterTargets` で除く (aar はクリーンビルドでは生成されず増分ビルドでのみ現れるため有無で条件分岐) / `NU1507` は `maui/nuget.config` (nuget.org のみ + packageSourceMapping) で消す
- 却下済み: 単一パッケージに全部同梱 (maui/ADR-0004) / Android だけ統合の 2 パッケージ / Package ID も `KsDialogs` (nuget.org で Native 版と紛れる)

## TODO

- [ ] 論点の解消 (Xcode / workload 整合・`BG8401` の 6 型の要否・`DialogException` の包み直し先・MAUI 下限版・最低 OS ガード)
- [ ] ksn-propose で変更提案を起こす
