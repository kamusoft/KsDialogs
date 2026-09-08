# Tasks: add-maui-nuget-distribution

前提: phase-5 (add-native-distribution) の Android 座標リネームが merge 済みで、Android binding の aar パスが `android/ksdialogs-core` を指していること。

## 1. 公開面を固める前の小修正

- [ ] 1.1 `maui/android/KsDialogs.Binding.Android/Transforms/Metadata.xml` に `MauiDialogBridge` / `MauiLoadingBridge` / `MauiToastBridge` / `MauiDialogContent` の `.Companion` 入れ子型と同名 static フィールドの `remove-node` を足し (design Decision 5)、binding の Release ビルド出力に `BG8401` が無いこと・facade の `net10.0-android` ビルドが通ることを確認する (→ Scenario: BG8401 の不在と static メンバーの可用性)
- [ ] 1.2 **修正前の再現** (handbook cross/runtime-behavior-verification.md): Android エミュレータで、依存を解決できない TView を 1 行登録した Sample (または一時消費者) を show し、現行の症状 (メッセージだけの `InvalidOperationException`、InnerException なし) を証跡に残す。同じアプリ・同じ操作を 1.7 の修正後確認に使う (→ Scenario: MB-MA-14)
- [ ] 1.3 `DialogException` に InnerException を取る private 基底コンストラクタと入れ子型 `ViewCreationFailed` (`ViewTypeName` / `ViewModelTypeName`、英語メッセージ) を追加し、`KsDialogsServiceCollectionExtensions.CreateView` の `ActivatorUtilities` 生成を包んで `DialogException` 以外の例外を `ViewCreationFailed` に包み直す。利用者コード (`Register` / インライン factory・`UseViewFallback` の resolver) は包まない (design Decision 6) (→ Requirement: View 生成失敗の構成ミスとしての報告)
- [ ] 1.4 facade テスト (`DialogDependencyInjectionTests` / `LoadingDependencyInjectionTests` / Toast 系) に MB-MA-11 (Dialog と Loading、両プロパティと InnerException の assertion) / MB-MA-12 (Toast の警告と後続継続) / MB-MA-13 (factory と resolver) / MB-MA-16 (既存経路の不変) を追加し、`KsDialogs.Maui.ApiSurfaceCheck` に `ViewCreationFailed` の型・両プロパティ・InnerException を参照する正の compile 検査を足して、`KsDialogs.Maui.Tests` の全件が通ることを確認する (→ Scenario: MB-MA-11 / MB-MA-12 / MB-MA-13 / MB-MA-16 / 公開例外面の compile 検査)
- [ ] 1.5 Kotlin 互換面の `MauiDialogContentProvider.createContent()` と `MauiLoadingContent.createContent()` の戻り値を `MauiDialogContent?` にし、`createContentView()` で null を `error(...)` により既存の失敗経路へ合流させる。bridge の Kotlin テストに MB-MA-15 (Dialog / Loading の中身なし → 失敗通知、例外の非漏出) を追加し `maui/android/native` のテスト全件が通ることを確認する (design Decision 6) (→ Scenario: MB-MA-15)
- [ ] 1.6 Android の `PlatformDialogGateway.ContentProvider` / `PlatformLoadingGateway.ContentProvider` を `BridgeContentSupply.CreateOrFail` + `BridgeContentFailure` 経由にし、`ClosureListener.OnFailed` / `CompletionListener.OnFailure` が預かった元例外を優先して投げるようにする。binding の生成コード (nullable 化した戻り値) に追随する (→ Requirement: Android の managed/native 境界での失敗の受け止め)
- [ ] 1.7 **修正後の確認**: 1.2 と同じアプリ・同じ操作を修正後のビルドで Android エミュレータと iOS Simulator で行い、`ViewCreationFailed` (InnerException 付き) が届くことを実測して 1.2 の証跡と並べて evidence/ に残す。MB-MA-14 は自動テストを持たない実機観測の Scenario なので `scripts/scenario-id-coverage.py` の `DEFAULT_ALLOW_MISSING` に理由付きで加える (MB-SM-* の先例) (→ Scenario: MB-MA-14)

## 2. 共通メタデータと MAUI 本体の版

- [ ] 2.1 `assets/icon.png` を取り込む (AiForms.Maui.Dialogs の `images/icon.png`、300×300 PNG) (→ Requirement: パッケージの共通メタデータと版の宣言元)
- [ ] 2.2 `maui/Directory.Build.props` を作る (design Decision 1 の値、同梱 props の import) と `maui/Directory.Build.targets` (同梱 targets の import、icon の同梱アイテム、自 assembly 用 aar の検査と除去) を翻案元から写す。facade csproj の `Version` 直書きを削除する (→ Requirement: パッケージの共通メタデータと版の宣言元)
- [ ] 2.3 `maui/Directory.Packages.props` の `Microsoft.Maui.Controls` と Sample の `MauiVersion` を 10.0.20 に下げ、コメントを「workload 同梱版に合わせる」に改める (design Decision 2)。facade 3 TFM のビルド・`KsDialogs.Maui.Tests` (件数を併記、handbook cross/test-execution.md)・Sample 両 OS のビルドで `project.assets.json` の解決版 10.0.20 と全件成功を確認する (→ Scenario: MAUI 本体の版の引き下げ後のビルドとテスト)
- [ ] 2.4 テストと ApiSurfaceCheck に pack を実行して nupkg が生成されないことを確認する (→ Scenario: pack 対象の限定)
- [ ] 2.5 handbook `cross/local-development-setup.md` の「.NET SDK と MAUI ワークロード」節に、workload set を上げるときに props と Sample の版を同梱版に合わせる 1 ルールと同梱版の確かめ方を書く (→ Scenario: 規範文書の記載)

## 3. pack 設定と最低 OS 版ガード

- [ ] 3.1 facade csproj: `IsPackable=true`、Description / PackageTags (design Decision 3)、`PackageReadmeFile` にルート `README.md` を同梱。binding 2 件の csproj: `IsPackable=true` と Description (→ Requirement: 3 パッケージの構成と内容 / package README の表示)
- [ ] 3.2 facade に `buildTransitive/KsDialogs.Maui.props` (Android 24 / iOS 17.0 の定数) と `buildTransitive/KsDialogs.Maui.targets` (`KSDLG0001`、platform inner build 限定、`VersionLessThan`、`CoreCompile` の前) を追加して facade パッケージに同梱し、facade / binding 2 件の csproj の `SupportedOSPlatformVersion` を定数参照に置き換える (design Decision 4)。TFM ごとに `-getProperty:SupportedOSPlatformVersion` で評価値を確認する (→ Scenario: 要件の宣言元の一致)
- [ ] 3.3 binding 2 件と facade を Release で pack し (Version 未指定と `-p:Version=0.1.0-alpha.1` の 2 回)、3 パッケージの nuspec (version / authors / license / URL / icon / TFM 別依存と下限 10.0.20。readme は facade のみ) と同梱物 (facade に native 混入なし、Gradle 由来 aar 2 本のみ、xcframework 両スライス、snupkg、自 assembly 用 aar の不在) を検査して evidence/ に残す (→ Scenario: nuspec のメタデータと Version の既定・注入 / 3 パッケージのローカル pack / binding パッケージの同梱物と説明)
- [ ] 3.4 facade / binding 2 件 / Sample をビルドしてガード由来のエラー・警告が出ないことを確認する (→ Scenario: 要件を満たす利用者アプリとリポジトリ内のビルド)

## 4. README のリンク

- [ ] 4.1 `README.md` / `README_ja.md` から `](...)` の参照を機械抽出し (anchor と mailto を除く)、非 HTTP(S) の参照 (現状各 12 件: `skills/README*.md` / `skills/` / `assets/` / `LICENSE` / `ios/` / `android/` / `maui/` / `kmp/` / `kasane/` / `kasane/concepts/` / `AGENTS.md` / `.github/CONTRIBUTING*.md`) をすべて絶対 URL に改め (両枚同時、design Decision 7)、抽出結果 0 件・pack した facade の nupkg に README が入ること・全 URL の取得成功を確認して evidence/ に残す (→ Scenario: README の同梱とリンク参照)

## 5. 消費者検証 (一時プロジェクト、リポジトリに残さない)

- [ ] 5.1 3 パッケージのローカルフォルダフィードを作り、本リポジトリの `global.json` を複製して SDK / workload を固定し、隔離した `RestorePackagesPath` とローカルフィード + nuget.org 併記の `nuget.config` を用意した素の MAUI アプリ (`Microsoft.Maui.Controls` の版は書かない、Android 24 / iOS 17.0) に facade の PackageReference 1 行を足して restore し、警告 0 件 (`NU1605` / `NU1608` / `NU1107` を `WarningsAsErrors`)・`Microsoft.Maui.Controls` 10.0.20・binding 2 件の推移解決と取得元 (`.nupkg.metadata`) を確認する (design Decision 8) (→ Scenario: ローカルフィードからの restore と Release ビルド)
- [ ] 5.2 同アプリで facade の公開型を参照した状態の Android (trimming + R8 + AOT) / iOS (simulator) Release ビルドが成功し、成果物に facade と binding のアセンブリが残り `XA4301` が出ないことを確認する (→ Scenario: ローカルフィードからの restore と Release ビルド)
- [ ] 5.3 同アプリに 1 行登録とダイアログ表示の操作を足し、Android エミュレータと iOS Simulator で表示・閉鎖・結果の到着を確認して証跡を残す (→ Scenario: パッケージ経由の起動確認)
- [ ] 5.4 同アプリの `SupportedOSPlatformVersion` を Android 21 / iOS 15.0 に下げ、両 TFM のビルドが `KSDLG0001` (要件と設定方法の文面) で失敗し、Android では manifest merger より先に出ることを確認する。両 platform とも未設定にして、Android は SDK 既定 21.0 で同じエラー、iOS は SDK 既定値で通ることを確認する (→ Scenario: 要件未満の利用者アプリ)
- [ ] 5.5 facade を参照する `net10.0` のみのクラスライブラリと複数 TFM プロジェクト (最低 OS 版未設定) で、クラスライブラリ・outer build・`net10.0` inner build ではガードの診断が出ず platform inner build でだけエラーになることを確認する (→ Scenario: 非 platform TFM と outer build ではガードが動かない)
- [ ] 5.6 5.1〜5.5 の証跡 (restore / build ログの要点、assets の該当行、`.nupkg.metadata` の source、ガードのエラー文面、起動画面) を evidence/ に残す (ローカル絶対パスは置換、handbook cross/evidence 規約)

## 6. MAUI Sample の両 OS 通しと完了判定

- [ ] 6.1 `samples/maui` を iOS Simulator (起動中のものは流用せず別デバイスを boot) でビルド・起動し、handbook `cross/sample-parity.md` の安定デモ ID 14 件を順に起動引数で渡して通す。`transition-dialog` / `layout-dialog` は画面からダイアログを 1 回表示し、Dialog 系は閉じて結果表示の変化まで撮る。項目ごとの証跡を evidence/ に残す (config `ui.screenshot` の手順、lessons impl L-001 の証跡規律) (→ Scenario: 両 OS の全デモ項目)
- [ ] 6.2 同じ手順を Android エミュレータ (adb、デモ引数は string extra) で行い、MAUI 本体 10.0.20 での回帰が無いことを項目ごとの証跡で残す (→ Scenario: 両 OS の全デモ項目)
- [ ] 6.3 最終確認として MAUI の 3 テストルート (facade `dotnet test` / `maui/android/native` の Gradle テスト / `maui/macios/native` の `xcodebuild test`) を全件実行し、実行件数と失敗 0 を記録する (handbook cross/test-execution.md) (→ Scenario: 3 テストルートの全件実行)
