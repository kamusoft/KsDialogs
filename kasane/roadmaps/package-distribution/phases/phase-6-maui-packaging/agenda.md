# MAUI パッケージング (maui-packaging)

MAUI の NuGet 3 パッケージ (facade + binding 2 件) を nuget.org へ出せる形に配線し、配布物で公開面を固める前に済ませる小修正 (`BG8401`・`DialogException` 合流) と MAUI iOS のビルド環境整合を同梱する change フェーズ。

## 論点

番号は議論開始時 (2026-09-08) に整理したもので、以後この番号で扱う。移送元 (library-foundation phase-11 / 簡易起票 2 件) の原文は history.md の 2026-09-08 の項に要約して残す。

(全 7 論点解消済み — 決定事項へ移動、2026-09-08)

## 決定事項

踏襲 (解決済み論点)。出典は maui/ADR-0004 (3 パッケージ構成、KsSettingsView maui/ADR-0025 と同型)、KsSettingsView phase-6 / phase-8 の決定事項と実測 (`../KsSettingsView/kasane/roadmaps/package-distribution/phases/phase-6-maui-packaging/agenda.md`)。

- facade + binding 2 件の 3 パッケージ。利用者が書くのは facade 1 点、binding は TFM 条件付き依存で推移。binding の pack は SDK 標準経路 (`IsPackable=true` のみ) で自作 pack MSBuild は足さない。facade → binding の依存は下限指定 (lockstep + 最小適用版解決で同版に揃う)
- メタデータの置き場と pack の細部は下表のとおり (KsSettingsView 実測の踏襲)

| 項目 | 踏襲する形 |
|---|---|
| 置き場 | メタデータは `maui/Directory.Build.props`、版は `Directory.Packages.props` (CPM)。`PackageIcon` の同梱アイテムは `Directory.Build.targets` (props では `IsPackable` 評価前) |
| SourceLink | SDK 同梱の SourceLink + snupkg |
| README | `PackageReadmeFile` は facade のみでルート README を同梱 (画像・リンクは絶対 URL 化) |

- 名前空間は cross/ADR-0005 で既に素の `KsDialogs`。追加の rename は不要
- 実測で判明した対処 (踏襲) は下表のとおり

| 事象 | 対処 |
|---|---|
| iOS binding resource の manifest に発行マシンの絶対パスが乗る | pack は CI で行う |
| .NET Android SDK の自 assembly 用 aar が nupkg に入り利用者側で `XA4301` | `_IncludeAarInNuGetPackage` の `AfterTargets` で除く。aar はクリーンビルドでは生成されず増分ビルドでのみ現れるため有無で条件分岐 |
| `NU1507` | `maui/nuget.config` (nuget.org のみ + packageSourceMapping) で消す |

- 却下済み: 単一パッケージに全部同梱 (maui/ADR-0004) / Android だけ統合の 2 パッケージ / Package ID も `KsDialogs` (nuget.org で Native 版と紛れる)

### MAUI iOS の環境整合は解消済み、iOS 面 Sample 通しは本 change の受け入れ条件に同梱 (2026-09-08)

移送時の申し送り (.NET for iOS が Xcode 26.1 を要求し Xcode 26.5 でビルド不可) は phase-4 の `global.json` 固定 (workload set 10.0.300.3 = .NET for iOS 26.5) で解消済みで、本 phase での環境作業はない。add-model-binding-di deviation 9 で未実施のまま残る MAUI iOS 面の Sample 通し (Model Dialog を含む全デモ項目) は、本 change の受け入れ条件として iOS Simulator で実施し証跡を change の evidence/ に残す。不具合が見つかれば同じ change で直す。

- 却下: いま先に通して結果だけ持ち込む (不具合の受け皿がない) / phase-8 の smoke に委ねる (phase-8 は解決 + Release ビルドまでで起動は範囲外)

### `BG8401` は Metadata.xml で `Companion` 型を輸送層から除いて消す (2026-09-08)

対象は `MauiDialogBridge` / `MauiLoadingBridge` / `MauiToastBridge` / `MauiDialogContent` の `.Companion` 4 型 (移送時の 6 件から減少)。いずれも中身は `@JvmStatic` メンバーだけで outer 型の static として公開されており、facade は `MauiDialogBridge.Shared` / `MauiDialogContent.ApplyAttributes` の形で呼んで `Companion` 型を使っていない。Android binding の `Transforms/Metadata.xml` に `remove-node` で 4 型の入れ子クラスと同名 static フィールドを除き、Kotlin 側と facade は変えない。public companion を新たに足したときは Metadata.xml にも足す (private companion は生成対象外で警告が出ない)。

- 却下: Kotlin 側で companion を無くす (互換面の呼び方が変わり facade と bridge テストの追随が要る) / 警告のまま受け入れる (一般公開ライブラリのビルド出力として見栄えが悪い)

### View 生成失敗は `DialogException.ViewCreationFailed` を新設して報告し、元例外を InnerException に保持、Android も iOS と同じ預かり口で型を届ける (2026-09-08)

1 行登録でライブラリ自身が View を組み立てる経路 (`CreateView` の `ActivatorUtilities` 生成) の失敗を、生成失敗専用の入れ子型 `ViewCreationFailed` で報告する (「factory 未登録」とは原因も直し方も違うため既存型へ寄せない)。元の失敗 (どの依存が解決できなかったか) は InnerException に保持する (基底に message + inner のコンストラクタを足す)。利用者が書いたコード (`Register` / インライン show の factory、`UseViewFallback` の resolver) が投げた例外は包まずそのまま通す (2026-09-08 提案時の相方指摘で fallback を包む範囲から外した: resolver は利用者コードで、View の型名も知り得ない)。Toast は Show が戻り値を持たないため既存契約 (警告 + 1 枚破棄) のまま、警告に原因として残す。実機では View の生成は器に受理された後の UI スレッドで走り、iOS は預かり口 (`BridgeContentSupply`) が元例外を保持して呼び出し元へ返す一方、Android は Kotlin 側の `onFailed(message)` 経由で型が落ちてメッセージだけの `InvalidOperationException` になるため、Android の gateway にも同じ預かり口の形を入れて両 OS で同じ型が届くようにする。Native 側に対応する分類は無く (DI による生成が無い)、`ServiceProviderUnavailable` と同じ MAUI 固有の失敗として扱う。concepts `maui/api/dialog-surface.md` の失敗種別表への行追加は蒸留で行う。

- 却下: 既存 `ViewFactoryNotRegistered` に包み直す (「未登録」と表示され原因を誤誘導する) / 現状維持 (種別が無く Android では型も失う)

### MAUI 本体の下限は workload set 同梱の版 (10.0.20) とし、ビルド版も同じ版に揃える。API 版付き TFM は SDK 挙動を受け入れる (2026-09-08)

facade が使う MAUI の API は全て .NET 6〜8 世代からあるもので 10.0.x の途中で追加された API は無く、10.0.20〜10.0.70 はバグ修正のみ。実測 (scout、`maui/Directory.Packages.props` の版を書き換えた複製) で 10.0.0 でも 3 TFM のビルドと facade のユニットテスト 155 件が通った (実機の描画経路は未確認)。MAUI テンプレートは版をリテラルで書かず workload 同梱の既定値 `$(MauiVersion)` を使い、repo の `global.json` が固定する workload set 10.0.300.3 では 10.0.20。下限を 10.0.20 にして `maui/Directory.Packages.props` の `Microsoft.Maui.Controls` と Sample の `MauiVersion` も同じ版に下げ、「下限 = 実際にビルド・テストする版 = 同じ SDK の利用者の既定値」を揃えて CI が下限そのものを常時検証する形にする。以後 `global.json` を上げるときは props を workload 同梱の版に合わせる 1 ルールで追随する (handbook local-development-setup の該当行を change に同梱で改める)。KsSettingsView が 10.0.70 に留めた理由 (画像解決の版差、同 maui/ADR-0026) は KsDialogs には該当しない。README 互換表 (現状 10.0.1) の訂正は docs-refresh 依頼へ。SDK 10.0.300 既定の API 版付き TFM (`net10.0-android36.0` / `net10.0-ios26.0`) は SDK 挙動のまま受け入れ、古い TargetPlatformVersion を固定した利用者への影響は phase-8 の消費者検証と README 互換情報へ申し送る。

- 却下: 10.0.70 のまま (同じ SDK の素の利用者が NU1605 で止まり `MauiVersion` の明記を強いる) / 10.0.0 を下限と宣言しビルド版は 10.0.70 (下限が常時検証されない)

### 最低 OS 版 (iOS 17 / Android 24) は facade 同梱のビルド時ガード `KSDLG0001` で両 OS を検査する (2026-09-08)

KsSettingsView と同型に、facade の NuGet に `buildTransitive/` の props (要件の数値) + targets (platform inner build でのみ `SupportedOSPlatformVersion` を `VersionLessThan` で検査し、要件を書いたエラーで止める) を同梱する。診断 ID は `KSDLG0001`。要件の数値は props に集約し、facade / binding の `SupportedOSPlatformVersion` の宣言元と 1 箇所にする (cross/ADR-0002 の変更はそこだけ)。iOS は xcframework が iOS 17 目標で組まれていても利用者がテンプレート既定 (15.0) のままリンクが通り iOS 15 / 16 端末で落ちる形で出荷できてしまうため、ビルド時検査でしか防げない。Android は manifest merger が止めるが読みにくいエラーになるため、先に要件を書いた文面を出す。maui/ADR-0004 の「自作 pack MSBuild を足さない」は pack 内部構造への依存回避が趣旨で利用者ビルド資産は対象外 (蒸留時に Consequences へ追記)。README / skills への明記は docs-refresh 依頼へ。

- 却下: 文書の明記のみ (iOS の誤出荷を防げない) / iOS だけガード (Android の要件未満も読める文面で止めたい)

### パッケージメタデータの固有値 (2026-09-08)

名乗り・ライセンス・icon の扱いは KsSettingsView のオーナー裁定を踏襲し、KsDialogs 固有の値だけ埋めた。

| 項目 | 値 | 根拠 |
|---|---|---|
| Authors / Company / Copyright | `kamusoft` / `kamusoft LLC` / `Copyright (c) kamusoft` | KsSettingsView 裁定の踏襲。LICENSE (`Copyright (c) 2026 kamusoft`) と同じ名乗り、法人正式名化は見送り済み |
| PackageLicenseExpression | `MIT` | LICENSE |
| PackageProjectUrl / RepositoryUrl / RepositoryType | `https://github.com/kamusoft/KsDialogs` / 同 / `git` | phase-3 の public 化先 |
| PackageIcon | 原典 AiForms.Maui.Dialogs の `images/icon.png` (300×300 PNG) を `assets/icon.png` にコピーし 3 パッケージ共通 (同梱アイテムは `Directory.Build.targets`) | 原典の継承 (著作権者同一、帰属表示不要) |
| Version 既定値 | `maui/Directory.Build.props` に `0.0.0-dev`、facade csproj 直書きの `0.1.0` は削除。CI が `-p:Version=` で注入 | phase-5 の version 注入と同じ流れ (cross/ADR-0009) |
| IsPackable | props 既定 `false`、facade / binding 2 件の csproj で `true`。facade は `PackageId` を明示、binding は既定 (アセンブリ名 = `KsDialogs.Binding.iOS` / `.Android`) | cross/ADR-0005 の写像表と一致 |
| Description (facade) | "A dialog UI library for .NET MAUI that presents dialogs, loading indicators, and toasts from anywhere in an application, with content written as MAUI views, on iOS and Android. Successor to AiForms.Maui.Dialogs." | phase-5 の Android POM 文言の翻案 + 原典への言及 |
| Description (binding) | "iOS native bridge binding for KsDialogs.Maui. Referenced transitively by KsDialogs.Maui; do not reference this package directly." (Android は読み替え) | maui/ADR-0004 の「直接参照しない」の明記 |
| PackageTags | facade のみ `maui dialog dialogs loading toast ios android`。binding には付けない | 検索で binding が先に出ないように |
| PackageReadmeFile | facade のみルート `README.md` (facade csproj に置く)。画像 (`assets/*.png` 6 枚) とリンクの絶対 URL 化は change に同梱 | 踏襲 (README の新設は cross/ADR-0012 と衝突) |

### 検証は pack + 消費者 Release ビルド + 両 OS の起動確認までを change の受け入れ条件にする (2026-09-08)

maui/ADR-0004 が未検証のまま送った 3 点 (nuget.org 要件・Release / trimming の消費者ビルド・Android の NuGet 経由実行時) を本 change で埋める。phase-8 の消費者検証は「解決 + Release ビルド」までで起動を含まないため、Android の NuGet 経由の実行時確認は本 phase で見ないとどこでも見ない。受け入れ条件: 3 パッケージの `dotnet pack` (nuspec のメタデータ・TFM 別依存・snupkg・binding の native 同梱を検算) / ローカルフィードの facade 1 行を足した素の MAUI アプリで restore 警告 0 (NU1605 / NU1608 / NU1107) と Android (trimming + R8 + AOT)・iOS (Simulator) の Release ビルド / ガード `KSDLG0001` の発火と非発火 / Android エミュレータと iOS Simulator でダイアログを 1 回表示する起動確認。証跡は change の evidence/ に残す。`dotnet publish` (フル trimming) と実機は範囲外。消費者アプリの `nuget.config` は nuget.org 併記 + 隔離 packages path (phase-8 の踏襲事項と同じ)。

- 却下: 起動なしでビルドまで (Android 実行時が以後どこでも検証されない) / pack のみで消費者側は phase-8 (Release / trimming とガードの発火確認が phase-8 まで残る)

### 文書追随は 3 段仕分け (2026-09-08)

| 段 | 対象 | 内容 |
|---|---|---|
| change 同梱 | handbook `cross/local-development-setup.md` | 「props の MAUI 版は workload set 同梱の版に合わせる」の 1 ルール |
| 同上 | README / README_ja の画像・リンク絶対 URL 化 | package README として nuget.org で表示するための付随修正 |
| 蒸留 (ksn-distill) | concepts `maui/api/dialog-surface.md` / `di-registration.md` | 失敗種別の表に `ViewCreationFailed` の行 |
| 同上 | concepts maui (binding 構成の記述先。現状は api/ のみで置き場は蒸留時に決める) | NuGet 3 パッケージの構成・pack 経路・最低 OS ガード・SDK 更新時の再検証箇所 (manifest 絶対パス、API 版付き TFM、XA4301) |
| 同上 | maui/ADR-0004 | buildTransitive ガードと CI での pack 前提を Consequences に追記し accepted へ昇格 |
| docs-refresh (蒸留後に明示依頼、handbook docs-refresh-timing) | README 互換表・導入節、skills `maui` / `aiforms-migration` | MAUI 10.0.20 以上と NU1605 の注意、最低 OS 版とガード `KSDLG0001`、API 版付き TFM の SDK 要件、`ViewCreationFailed` |

## TODO

- [x] 論点 1〜7 の解消 (2026-09-08 全件を決定事項へ)
- [ ] **docs-refresh の明示依頼** (蒸留後): 内容は決定事項「文書追随は 3 段仕分け」の docs-refresh 行。phase-5 の Android 座標分と 1 回の依頼にまとめる
- [ ] **phase-8 への申し送り**: API 版付き TFM (`net10.0-android36.0` / `net10.0-ios26.0`) を下回る消費者での解決要件の確認、MAUI 消費者の `nuget.config` (nuget.org 併記 + 隔離 packages path) は本 phase の消費者 PoC の形を写す
- [ ] ksn-propose で変更提案を起こす
