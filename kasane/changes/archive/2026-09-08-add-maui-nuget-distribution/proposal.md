# Proposal: add-maui-nuget-distribution

## Why

MAUI 形態の配布は NuGet 3 パッケージ (facade `KsDialogs.Maui` + 輸送層 binding `KsDialogs.Binding.iOS` / `KsDialogs.Binding.Android`、maui/ADR-0004) と決まっているが、binding 2 件は `IsPackable=false` のまま、facade は csproj に `Version` 0.1.0 を直書きし、メタデータも無い。姉妹ライブラリ KsSettingsView の実装 (add-maui-nuget-distribution、2026-09-02) を「コピー + 固有値の差し替え」で持ち込み、nuget.org へ出せる形に配線する。

あわせて、配布物で公開面を固める前に済ませる小修正 2 件 — Android binding の `BG8401` 警告 (`Companion` 4 型) と、1 行登録が組み立てる View の生成失敗を `DialogException` に合流させる (Android では core/ADR-0033 が定める境界の預かり口が未配線で、失敗が型を失ってメッセージだけの `InvalidOperationException` で届く) — と、add-model-binding-di で未実施のまま残る MAUI iOS 面の Sample 通しを同梱する。フェーズ議論で決めた MAUI 本体の下限版 (workload set 同梱の 10.0.20) と最低 OS 版のビルド時ガード (`KSDLG0001`) もここで実装する (maui/ADR-0004 改訂、proposed)。

## What Changes

- **共通メタデータ**: `maui/Directory.Build.props` を新設し、Authors / Company / Copyright / MIT / URL 群 / `Version` 既定値 `0.0.0-dev` (CI が `-p:Version=` で注入) / `IsPackable` 既定 false / SourceLink + snupkg / `PackageIcon` を集約する。`assets/icon.png` (原典 AiForms.Maui.Dialogs の 300×300) を取り込み、同梱アイテムは `maui/Directory.Build.targets` に置く。facade csproj の `Version` 直書きを廃止。既存の `Directory.Packages.props` (CPM) と `nuget.config` (phase-4 で導入済み) はそのまま使う
- **MAUI 本体の版**: `Directory.Packages.props` の `Microsoft.Maui.Controls` と Sample の `MauiVersion` を workload set 10.0.300.3 同梱の 10.0.20 に下げ、下限 = ビルド版 = 同じ SDK の利用者の既定値に揃える。handbook `cross/local-development-setup.md` に「props の版は workload 同梱版に合わせる」の 1 ルールを書く
- **pack 設定**: facade / binding 2 件に `IsPackable=true`、facade に `PackageId` / Description / PackageTags / `PackageReadmeFile` (ルート README)、binding に Description ("do not reference this package directly")。pack は SDK 標準経路で、.NET Android SDK の自 assembly 用 aar (`XA4301` の原因) を nupkg から除く後処理だけを意図的な例外 (SDK 内部ターゲットへの接続、見直し条件つき) として KsSettingsView から写す
- **最低 OS 版のビルド時ガード**: facade に `buildTransitive/KsDialogs.Maui.props` (iOS 17.0 / Android 24 の定数) と `.targets` (platform inner build でのみ `SupportedOSPlatformVersion` を検査し、要件未満なら `KSDLG0001` で止める) を同梱する。リポジトリ内の facade / binding / Sample の `SupportedOSPlatformVersion` は同梱 props の定数を参照する (Sample は別ビルドルートのため直書き維持)
- **`BG8401` の解消**: Android binding の `Transforms/Metadata.xml` に `remove-node` で `MauiDialogBridge` / `MauiLoadingBridge` / `MauiToastBridge` / `MauiDialogContent` の `.Companion` 入れ子型と同名 static フィールドを除く。Kotlin と facade は無変更
- **View 生成失敗の合流**: `DialogException.ViewCreationFailed` (InnerException 保持) を新設し、1 行登録 (`RegisterForDialog` / `RegisterForLoading` / `RegisterForToast`) の `CreateView` (`ActivatorUtilities` 生成) の失敗をこの型で報告する (Dialog / Loading は show の失敗として呼び出し元へ、Toast は既存契約どおり警告 + 1 枚破棄で原因として残す)。Android の Dialog / Loading gateway に iOS と同じ `BridgeContentSupply.CreateOrFail` + `BridgeContentFailure` の預かり口を配線し (core/ADR-0033 の決定どおり)、Kotlin 互換面の Dialog / Loading の `createContent()` を Toast と同じ nullable にして null を既存の失敗経路に合流させる。両 OS で同じ型が呼び出し元へ届く。修正前に Android で型が失われる現状を実環境で再現してから直す (handbook runtime-behavior-verification)
- **README の同梱**: ルート `README.md` を facade の package README とし、両 README に残る非 HTTP(S) の参照 (各 12 件: `skills/` / `assets/` / `LICENSE` / 4 形態のディレクトリ / `kasane/` / `AGENTS.md` / CONTRIBUTING) を public リポジトリの絶対 URL に改める (画像は public 化時に絶対 URL 化済み)
- **検証**: 3 パッケージのローカル pack の検算、ローカルフィードの facade 1 行を足した素の MAUI アプリでの restore 警告 0 と両 OS Release ビルド、ガードの発火 / 非発火、Android エミュレータと iOS Simulator でのダイアログ 1 回表示、MAUI の Sample 全デモ項目の両 OS 通し (iOS 面は未回収分の回収、Android 面は MAUI 本体の版引き下げの回帰確認)、最後に MAUI の 3 テストルート (facade / Android 互換面 / iOS 互換面) の全件実行。証跡は evidence/ に残す

影響する能力: MAUI の配布 (NuGet 3 パッケージ・メタデータ・pack 経路・利用者ビルド資産)、MAUI facade の構成ミス失敗契約 (`DialogException` の種別)、MAUI の managed/native 境界 (Android の預かり口)、Android binding の生成 (Metadata.xml)、MAUI ビルド入口 (MAUI 本体の版・最低 OS 版の宣言元)

## Non-Goals

- nuget.org への実発行・Trusted Publisher Policy・GitHub Environment `release`・`ContinuousIntegrationBuild` の注入・release workflow への組み込み — phase-9 の責務 (cross/ADR-0009)
- 配布物を参照する消費者プロジェクト (`verification/maui`) と dry-run / smoke workflow — phase-8 の責務。本変更の消費者検証は一時プロジェクトで行いリポジトリに残さない (`nuget.config` の形は phase-8 へ申し送る)
- `dotnet publish` (フル trimming) と実機での確認 — phase-8 の決定事項 (解決 + Release ビルドまで) と同じ線引き。本変更の起動確認は Simulator / エミュレータ
- README の互換表 (MAUI 10.0.20 以上・最低 OS 版・API 版付き TFM の SDK 要件) と `skills/` の追随 — docs-refresh の明示依頼で更新する (CLAUDE.md の運用宣言、handbook docs-refresh-timing)。concepts の追随 (`maui/api/dialog-surface.md` / `di-registration.md` の失敗種別表、binding 構成の記述) と maui/ADR-0004 の accepted 昇格は蒸留時
- 利用者が書いたコード (`Register` / インライン show の factory、`UseViewFallback` の resolver) が投げた例外の包み直し — 包むのは 1 行登録でライブラリ自身が組み立てる経路だけ (agenda 決定事項、相方スペックレビュー反映 2026-09-08)。利用者コードの例外は core/ADR-0033 の既存経路でそのまま届く
- facade → binding の NuGet 依存を完全一致 `[x.y.z]` にすること — SDK 10.0.300 の pack に標準手段が無く、自作 MSBuild は maui/ADR-0004 の却下案。lockstep (cross/ADR-0009) と最小適用版解決で同版になり、消費者側の版一致は phase-8 の依存検査が検出する (受容リスク、design Risks)
- Kotlin 側で companion object を無くす / Package ID や名前空間の変更 / 単一パッケージ化 — フェーズ議論・maui/ADR-0004 で却下済み
- Android 座標リネームに伴う binding の aar パス追随 — phase-5 の change (add-native-distribution) が担う。本変更は phase-5 実装後の状態を前提にする

## Impact

- 破壊的変更: 公開面は `DialogException.ViewCreationFailed` の追加のみ (公開前のため利用者への影響なし)。Kotlin 互換面の `createContent()` の戻り値が nullable になる (互換面は輸送層で公開契約ではない、maui/ADR-0001)
- 影響範囲: `maui/` 配下の全プロジェクト (props / CPM の版)、Android binding の生成コード (`Companion` 型の消失)、Android gateway の失敗経路、Sample の `MauiVersion`、README 2 枚、handbook 1 本
- リスク: MAUI 本体を 10.0.70 → 10.0.20 に下げるため、facade のテストと両 OS の Sample 通しで回帰を確認する / `buildTransitive/` は推移的な全消費者に import されるため platform inner build 以外で無効なことを消費者検証で確認する / Android の預かり口配線で Kotlin 互換面と C# の両方を触るため bridge のテスト (Kotlin) と facade のテストで既存の失敗経路 (PresentationHostUnavailable 等) が変わらないことを確認する

## 級: L

NuGet の配布構成 (発行後は覆しにくい)・公開 API の追加・Android binding と gateway・利用者ビルド資産と複数の能力を横断する。

domain: maui
roadmap: package-distribution/phase-6-maui-packaging
