# Design: add-maui-nuget-distribution

## Context

MAUI の NuGet 配布は facade + binding 2 件の 3 パッケージ (maui/ADR-0004、2026-09-08 改訂で MAUI 本体の下限ルールと最低 OS 版ガードを追加、proposed)。姉妹ライブラリ KsSettingsView が同じ構成を 2026-09-02 に実装済み (`../KsSettingsView/kasane/changes/archive/2026-09-02-add-maui-nuget-distribution/`) で、その pack 配線・`buildTransitive/` ガード・消費者検証の形を「コピー + 固有値の差し替え」で持ち込む。KsDialogs 側で既に済んでいるもの: `maui/Directory.Packages.props` (CPM) と `maui/nuget.config` (nuget.org 固定 + packageSourceMapping、phase-4)、README の画像の絶対 URL 化 (phase-3)。名前空間は cross/ADR-0005 で既に素の `KsDialogs` で改名は不要。

小修正 2 件と Sample 通しの前提は phase-6 agenda の決定事項 (2026-09-08) と現行コードで確認済み: `BG8401` は `@JvmStatic` 付き companion 4 型で facade は outer 型の static 経由で呼ぶ / 1 行登録の `CreateView` (`maui/KsDialogs.Maui/Hosting/KsDialogsServiceCollectionExtensions.cs`) の `ActivatorUtilities` 失敗は Android で `onFailed(message)` を経由して型を失う (`Platforms/Android/PlatformDialogGateway.cs` の `ClosureListener.OnFailed`)、iOS は `BridgeContentSupply.CreateOrFail` + `BridgeContentFailure` (`Internals/BridgeContentSupply.cs`) で元例外を保持する / core/ADR-0033 (accepted) は MAUI 境界で両 OS が `BridgeContentSupply` を通すと定めているが Android の Dialog / Loading gateway は未配線 (`ContentProvider.CreateContent` が `request.CreateContent()` を直接呼ぶ)。

## Goals / Non-Goals

Goals: proposal.md の What Changes。Non-Goals: proposal.md の Non-Goals。

## Decisions

### Decision 1: 共通メタデータは `maui/Directory.Build.props`、同梱アイテムと後処理は `Directory.Build.targets` (翻案元 Decision 2 の踏襲)

**採用案:** `maui/Directory.Build.props` に Authors `kamusoft` / Company `kamusoft LLC` / Copyright `Copyright (c) kamusoft` / `PackageLicenseExpression` `MIT` / `PackageProjectUrl` = `RepositoryUrl` = `https://github.com/kamusoft/KsDialogs` / `RepositoryType` `git` / `PackageIcon` `icon.png` / `Version` `0.0.0-dev` / `IsPackable` `false` / `PublishRepositoryUrl` `EmbedUntrackedSources` `IncludeSymbols` `true` / `SymbolPackageFormat` `snupkg` を置き、同梱 props (Decision 4) を import する。`maui/Directory.Build.targets` を新設し、同梱 targets の import・`IsPackable=true` のプロジェクトだけに `assets/icon.png` を `None Pack="true" PackagePath=""` で同梱するアイテム・.NET Android SDK が生成する自 assembly 用 aar を nupkg から除く後処理 (`_IncludeAarInNuGetPackage` の `AfterTargets`。中身が推移依存の native ライブラリ以外なら失敗させる検査つき、翻案元と同一) を置く。facade csproj の `Version` 直書きは削除する。`assets/icon.png` は原典 AiForms.Maui.Dialogs の `images/icon.png` (300×300 PNG、著作権者同一) をコピーする。

**理由:** 3 プロジェクトに同じ値を書かず 1 か所に置く。`IsPackable=true` は csproj 本体で設定されるため props の評価時点では条件が成立せず、同梱アイテムは targets に置く (翻案元 deviation で実測)。自 assembly 用 aar は利用者の Android Release ビルドで `XA4301` を出す (翻案元実測) ため、公開前に写して除く。

**代替案:**
- **A: 各 csproj にメタデータを直書き (現状形)** — 3 か所に同じ値が並び URL 変更で 3 か所を触る。却下
- **B: リポジトリルートに `Directory.Build.props` を置く** — `samples/maui/` にも効いて Sample が pack / CPM 対象になり、ビルドルートの境界 (cross/ADR-0004) を破る。却下
- **C: 自 assembly 用 aar の除去は phase-8 の消費者検証で `XA4301` を観測してから** — 翻案元で原因と対処が確定済みで、観測を待つ理由がない。却下

### Decision 2: MAUI 本体の版は workload set 同梱の 10.0.20 に揃える (maui/ADR-0004 改訂)

**採用案:** `maui/Directory.Packages.props` の `Microsoft.Maui.Controls` を 10.0.70 → 10.0.20 (repo `global.json` の workload set 10.0.300.3 が同梱する MAUI SDK 版) に下げ、`samples/maui/KsDialogs.Sample.Maui.csproj` の `MauiVersion` も 10.0.20 にする (Sample は別ビルドルートのため直書き維持、コメントを「workload 同梱版に合わせる」に改める)。handbook `cross/local-development-setup.md` の「版を上げるときは…」の行を「`global.json` の workload set を上げるときは props の `Microsoft.Maui.Controls` を同梱版 (`$(MauiVersion)` の既定値) に合わせる」の 1 ルールに書き換える。facade → `Microsoft.Maui.Controls` の依存は CPM の版がそのまま下限 (`>= 10.0.20`) になる。

**理由:** agenda 論点 4 の実測 (10.0.0 で 3 TFM ビルドとテスト 155 件が通る、API 差分なし、テンプレート既定は workload 同梱値 = 10.0.20)。下限・ビルド版・同じ SDK の利用者の既定値が一致し、CI が下限を常時検証する。

**代替案:**
- **A: 10.0.70 のまま** — 同じ workload set の素の利用者が NU1605 で止まる。翻案元の理由 (画像解決の版差) は KsDialogs に該当しない。却下
- **B: 10.0.0 を下限と宣言しビルド版は 10.0.70** — 下限が一度の実測だけで常時検証されない。却下

### Decision 3: pack は SDK 標準経路のみ、facade の Package ID は明示、binding は既定 (翻案元 Decision 4 の踏襲)

**採用案:** facade / binding 2 件の csproj に `IsPackable=true`。facade: `PackageId` `KsDialogs.Maui` (既存)、`Description` "A dialog UI library for .NET MAUI that presents dialogs, loading indicators, and toasts from anywhere in an application, with content written as MAUI views, on iOS and Android. Successor to AiForms.Maui.Dialogs."、`PackageTags` `maui dialog dialogs loading toast ios android`、`PackageReadmeFile` `README.md` (ルート README を `None Pack="true" PackagePath=""` で同梱。facade 固有のため csproj に置く)。binding: `PackageId` は既定 (アセンブリ名 `KsDialogs.Binding.iOS` / `.Android` = cross/ADR-0005 の写像表)、`Description` "iOS native bridge binding for KsDialogs.Maui. Referenced transitively by KsDialogs.Maui; do not reference this package directly." (Android は読み替え)、`PackageTags` なし。翻案元 PoC で確認済みの SDK 挙動 3 点 (iOS manifest の絶対パス → pack は CI で行う / facade → binding の依存は下限指定 → lockstep で同版 / API 版付き TFM `net10.0-android36.0` `net10.0-ios26.0`) はそのまま受け入れる。

**理由:** maui/ADR-0004。翻案元で 3 パッケージが標準経路だけで成立している。

**代替案:**
- **A: facade → binding の依存を完全一致 `[x.y.z]` にする自作ターゲット** — lockstep (cross/ADR-0009) と最小適用版解決で同版になり、内部構造依存を足す価値がない。却下
- **B: iOS manifest の絶対パスを消す後処理** — 公開物には CI ランナーの汎用パスしか載らない。却下

### Decision 4: 最低 OS 版は facade 同梱の `buildTransitive/` で検査、要件の数値は同梱 props を単一の宣言元にする (翻案元 Decision 5 の踏襲、maui/ADR-0004 改訂)

**採用案:** facade に `buildTransitive/KsDialogs.Maui.props` (`KsDialogsMinAndroidApi=24` / `KsDialogsMinIOSVersion=17.0`、cross/ADR-0002) と `buildTransitive/KsDialogs.Maui.targets` を置き、両方を facade パッケージに同梱する。targets は `TargetFramework` が空でなく `TargetPlatformIdentifier` が `android` / `ios` のときだけ有効で、`SupportedOSPlatformVersion` が空または `VersionLessThan` で要件未満なら、要件と設定例を書いた `Error Code="KSDLG0001"` を `CoreCompile` の前に出す (Android では manifest merger より先)。リポジトリ側は `maui/Directory.Build.props` が同梱 props を import し、facade / binding 2 件の csproj は既存の TFM 条件を残して値だけを定数参照に置き換える。`maui/Directory.Build.targets` が同梱 targets を import して自分たちのビルドにも検査を当てる。Sample は別ビルドルートで直書き (24.0 / 17.0) を維持し、props のコメントで「変えるときは Sample も」と示す。

**理由:** agenda 論点 5。iOS は xcframework が iOS 17 目標でも利用者のテンプレート既定 (15.0) のままリンクが通り実行時に落ちる形で出荷できる。Android は manifest merger が止めるが要件を示さない。利用者ビルド資産の同梱は「pack 内部構造に依存する自作 MSBuild を足さない」の対象外 (ADR-0004 改訂で明文化)。

**代替案:**
- **A: README / skills の明記のみ** — iOS の誤出荷を防げない。却下
- **B: 数値を targets と csproj に直書き** — 2 か所の同期が要る。却下
- **C: TFM 別フォルダ (`buildTransitive/net10.0-android/` 等)** — 1 ファイルの Condition で足り、API 版付き TFM の照合を複雑にする。却下
- **D: `SupportedOSPlatformVersion` の代入を `Directory.Build.props` の TFM 条件で行う** — 単一 TFM の binding は `TargetFramework` を csproj 本体で定義するため props 側の条件が空振りする。却下

### Decision 5: `BG8401` は Metadata.xml で `Companion` 型と同名フィールドを除く

**採用案:** `maui/android/KsDialogs.Binding.Android/Transforms/Metadata.xml` に、`MauiDialogBridge` / `MauiLoadingBridge` / `MauiToastBridge` / `MauiDialogContent` の 4 型について `remove-node` を 2 行ずつ (入れ子クラス `<Outer>.Companion` と outer の static フィールド `Companion`) 足す。Kotlin 側と facade は変えない。public companion を新たに足したときは Metadata.xml にも足す旨をコメントに残す。

**理由:** 4 型とも中身は `@JvmStatic` メンバーだけで outer 型の static として公開されており、facade は `MauiDialogBridge.Shared` / `MauiDialogContent.ApplyAttributes` の形で呼ぶ (`Platforms/Android/*.cs`)。`Companion` 型は .NET 側で未使用で、輸送層から落としても公開契約に影響しない。

**代替案:**
- **A: Kotlin 側で companion を無くす (`shared` をトップレベルへ)** — 互換面の呼び方が変わり facade と bridge テストの追随が要る。却下
- **B: 警告のまま受け入れる** — 一般公開ライブラリのビルド出力として見栄えが悪い。却下

### Decision 6: View 生成失敗は `DialogException.ViewCreationFailed` で報告し、Android の Dialog / Loading gateway に core/ADR-0033 の預かり口を配線する

**採用案:**
- `DialogException` に `(string message, Exception innerException)` の private 基底コンストラクタを足し、入れ子型 `ViewCreationFailed` (`ViewTypeName` / `ViewModelTypeName` を持ち、元例外を InnerException に保持。メッセージは英語固定、cross/ADR-0015) を新設する
- `KsDialogsServiceCollectionExtensions.CreateView` の `ActivatorUtilities.CreateInstance` と、`DialogResolution.ResolveViewFactory` の View fallback 呼び出しを try / catch で包み、`DialogException` 以外の例外を `ViewCreationFailed` に包み直す (`DialogException` 派生はそのまま通す。`ServiceProviderUnavailable` が先に立つ経路を変えない)。利用者が書いた factory (`Register` / インライン show) は包まない
- Android の `PlatformDialogGateway.ContentProvider` / `PlatformLoadingGateway.ContentProvider` を iOS と同じ `BridgeContentSupply.CreateOrFail(…, contentFailure)` 経由にし、`ClosureListener.OnFailed` / `CompletionListener.OnFailure` は `contentFailure.Cause ?? new InvalidOperationException(message)` を投げる。Kotlin 互換面の `MauiDialogContentProvider.createContent()` と `MauiLoadingContent.createContent()` の戻り値を Toast と同じ `MauiDialogContent?` にし、`createContentView()` は null を `error(...)` で既存の失敗経路 (`reportClosure` / `reportLoadingCompletion` の catch → `onFailed` / `onFailure`) に合流させる
- 結果: 両 OS で `ViewCreationFailed` (InnerException 付き) が呼び出し元の Task の失敗として届く。ユニットテスト (fake gateway) では factory がその場で呼ばれ同じ型が届く

**理由:** agenda 論点 3。「factory は登録されているが View を組み立てられなかった」は「factory 未登録」と原因も直し方も違い、concepts の失敗種別表 (`maui/api/di-registration.md`) が 1 対 1 で読める形を保つ。Android の預かり口は core/ADR-0033 (accepted) の Decision そのもので、現状はその決定から乖離している (iOS のみ配線)。Kotlin の nullable 化は Toast (`MauiToastContent.createContent(): MauiDialogContent?`) と同じ形。

**代替案:**
- **A: 既存 `ViewFactoryNotRegistered` に包み直す** — 「未登録」と表示され原因を誤誘導する。却下
- **B: 型は新設するが Android の預かり口は配線しない** — Android では JNI 境界で型が落ち、新設した型が届かない (`onFailed(message)` しか残らない)。core/ADR-0033 との乖離も残る。却下
- **C: Android は Kotlin の `Throwable` を `JavaProxyThrowable` として C# へ戻して元例外を復元する** — 境界を生の例外が越える形で core/ADR-0033 の「例外を値に変えて渡す」に反し、Kotlin 側の catch-all と二重になる。却下

### Decision 7: package README はルート `README.md`、残る相対リンクを絶対 URL に改める (翻案元 Decision 6 の踏襲)

**採用案:** facade の csproj で `PackageReadmeFile=README.md` とし、ルート `README.md` を同梱する。`README.md` / `README_ja.md` に残る相対リンク (`skills/README*.md` / `skills/` / `assets/` / `LICENSE`、各 5 箇所) を `https://github.com/kamusoft/KsDialogs/blob/develop/<path>` (ディレクトリは `tree/develop/`) の絶対 URL に改める (両枚同時)。画像は phase-3 で `raw.githubusercontent.com/kamusoft/KsDialogs/develop/assets/` に絶対 URL 化済みで変更なし。既定ブランチ `develop` は削除・force-push 禁止 (cross/ADR-0016)。

**理由:** README を新設せず cross/ADR-0012 (README はルート 2 枚) と docs-refresh 対象に触れない。package README として同梱されると相対リンクは nuget.org から到達できない (翻案元 review-001 で検出)。

**代替案:**
- **A: facade 専用の package README を新設** — cross/ADR-0012 の改訂と docs-refresh 対象の追加が要る。却下
- **B: 同梱しない** — nuget.org で readme なし表示。却下

### Decision 8: 消費者検証は一時プロジェクトで pack → restore → Release ビルド → 起動まで、iOS 面 Sample 通しを同梱

**採用案:** 3 パッケージを Release で pack (Version 未指定と `-p:Version=0.1.0-alpha.1` の 2 回) してローカルフォルダフィードを作り、repo の `global.json` を複製した素の MAUI アプリ (`Microsoft.Maui.Controls` は workload 既定のまま = 10.0.20、`SupportedOSPlatformVersion` Android 24 / iOS 17.0) に `KsDialogs.Maui` の PackageReference 1 行を足す。`nuget.config` はローカルフィード + nuget.org 併記 (`<clear/>` + ローカルのみだとテンプレート依存が NU1101、翻案元実測) で `RestorePackagesPath` を隔離し、取得元を `.nupkg.metadata` で確認する。restore 警告 0 (`NU1605` / `NU1608` / `NU1107` を `WarningsAsErrors`)、両 OS Release ビルド (Android は trimming + R8 + AOT、iOS は Simulator)、`SupportedOSPlatformVersion` を Android 21 / iOS 15.0 に下げたときの `KSDLG0001`、`net10.0` のみのクラスライブラリと複数 TFM の outer build で無反応、Android エミュレータと iOS Simulator でダイアログを 1 回表示。加えて `samples/maui` を iOS Simulator で起動し、デモ駆動モード (cross/ADR-0010、config `ui.screenshot`) で全デモ項目 (Model Dialog 含む) を通す。証跡は evidence/ (ローカル絶対パスは置換)。

**理由:** agenda 論点 1・7。phase-8 は起動を含まないため Android の NuGet 経由実行時は本 change でしか見ない。maui/ADR-0004 の未検証 3 点を埋めて蒸留で accepted に上げる。

**代替案:**
- **A: 起動なし (ビルドまで)** — Android 実行時が以後どこでも検証されない。却下
- **B: 消費者側は phase-8 に任せ pack のみ** — Release / trimming とガードの発火確認が phase-8 まで残る。却下

## Risks / Trade-offs

- MAUI 本体を 10.0.20 に下げる回帰は facade テスト (155 件) と両 OS の Sample 通しで確認する。実機描画経路の版差は Sample 通しでしか見えない
- Android 預かり口の配線は Kotlin 互換面 (nullable 化) と C# gateway の両方を触る。bridge の Kotlin テストで null → 失敗経路の合流を、facade テストで `ViewCreationFailed` の型と InnerException を固定する。既存の失敗経路 (`PresentationHostUnavailable` / cancelled) は変えない
- `buildTransitive/` は推移的な全消費者に import される。platform inner build 以外で無効なことを消費者検証で確認する
- Kotlin 側で `Companion` を持つ public 型が増えると `BG8401` が再発する。Metadata.xml のコメントで示す (機械検査は持たない)
- README の URL はブランチ名 `develop` を含む。既定ブランチの改名で切れる (削除禁止済み)
- 消費者検証の所要 (翻案元は約 20 分) と、Simulator / エミュレータの準備 (起動中の Simulator は流用しない)

## Migration Plan

1. `BG8401` (Metadata.xml) と `ViewCreationFailed` + Android 預かり口 → facade テストと bridge テストで回帰確認
2. props / targets / icon / MAUI 版 10.0.20 → 全プロジェクトの restore とビルド、facade テスト
3. pack 設定 + ガード + README → ローカル pack と一時消費者で検証、evidence/
4. iOS 面 Sample 通し → evidence/
5. handbook の 1 ルール追記

利用者側の移行は不要 (未リリース)。

## Open Questions

- なし

## ADR 候補

- なし (新規)。maui/ADR-0004 (proposed、2026-09-08 改訂済み) が Decision 2・3・4 を既に含む。蒸留時に Consequences へ実装結果 (facade の `buildTransitive/`、自 assembly 用 aar の除去、消費者検証の `nuget.config` の形) を溶かして accepted へ昇格。core/ADR-0033 は Decision 6 で乖離が解消されるため現行照合 footer を更新 (蒸留時)
