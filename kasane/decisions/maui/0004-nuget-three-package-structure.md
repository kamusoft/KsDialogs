---
id: 0004
title: MAUI NuGet は facade + 輸送層 binding 2件の3パッケージ構成とし、SDK 標準の pack 経路で native 成果物を同梱する
status: accepted
date: 2026-09-08
---

## Context

MAUI 形態の配布は NuGet で、消費者の手数は「`KsDialogs.Maui` 1点」と決まっている (cross/ADR-0008)。native 成果物は binding 2件が生成する (iOS: XcodeProject アイテム経由の xcframework / Android: gradlew Exec 経由の aar 2件、maui/ADR-0003)。これらを NuGet にどう埋めるか、利用者側の要件 (MAUI 本体の版・最低 OS 版) をどう伝えるかが未決だった (両 binding は `IsPackable=false` で、facade は csproj に版を直書きしていた)。

配布モデル検討の PoC で 3パッケージ構成の pack と NuGet 消費を実測し (PoC 記録 poc-maui-nuget-packaging.md、出典参照)、パッケージング段階の議論で MAUI 本体の下限版と最低 OS 版の伝え方を決めた。姉妹ライブラリ KsSettingsView が同じ構成を先に実装しており、その配線を「コピー + 固有値の差し替え」で持ち込むことを前提にしている。

前提:
- MAUI テンプレートは `Microsoft.Maui.Controls` の版をリテラルで書かず、workload 同梱の既定値 (`$(MauiVersion)`) を使う。利用者の既定値は導入している workload set で決まる
- facade が使う MAUI の API は 10.0.x の途中で追加されたものを含まない (10.0.0 でビルドとユニットテストが通ることを実測)
- iOS の xcframework は最低 OS 版 (iOS 17) を目標に組まれるが、利用者アプリがそれより低い `SupportedOSPlatformVersion` のままでもリンクは通り、実行時に落ちる形で出荷できてしまう。Android は aar の manifest により merger が止めるがエラー文は要件を示さない
- .NET SDK 10.0.300 の pack は、binding resource の manifest に発行マシンの絶対パスを記録し、ProjectReference の依存を下限指定 (`>= x.y.z`) で書く。完全一致 (`[x.y.z]`) にする公開手段は無い
- .NET Android SDK は class library ごとに自 assembly 用 aar を作ることがあり、それが nupkg に入ると利用者の Android Release ビルドで `XA4301` (native ライブラリの重複) が出る。除外用の公開プロパティは無い

## Decision

### 3パッケージ構成と SDK 標準の pack 経路

facade (`KsDialogs.Maui`) + 輸送層 binding 2件 (`KsDialogs.Binding.iOS` / `KsDialogs.Binding.Android`) をそれぞれ NuGet パッケージとして発行し、facade が TFM 条件付き NuGet 依存で binding を参照する。消費者の手数は `KsDialogs.Maui` 1点のまま。

native 成果物の同梱は SDK 標準の pack 経路を使う。binding プロジェクトを `IsPackable=true` にするだけで、iOS は binding resource package (resources.zip 内に xcframework)、Android は aar 2件 (束縛対象 + `Bind=false` 同梱) が nupkg に入る。**pack の内部構造に依存する自作 MSBuild は足さない。** 例外は 1 つだけ認める — .NET Android SDK が生成する自 assembly 用 aar を nupkg から除く後処理を、SDK 内部ターゲット (`_IncludeAarInNuGetPackage`) の直後に接続して持つ。除外用の公開手段が SDK に無く、除かなければ利用者側で `XA4301` が出るためで、後処理は aar が存在するときだけ動き、中身が推移依存の native ライブラリ以外なら pack を失敗させる検査を伴う。この接続が SDK 更新で外れたことは、pack 検算で 3 パッケージに自 assembly 用 aar が無いことを毎回確かめて検出する。

- 共通メタデータ (名乗り・ライセンス・URL・アイコン・SourceLink・snupkg・`IsPackable` の既定 false・開発用の版の既定値) は `maui/Directory.Build.props` に 1 か所で持ち、csproj には版を直書きしない。発行版は CI が `-p:Version=` で注入する (cross/ADR-0009)
- facade の Package ID は明示 (`KsDialogs.Maui`)、binding はアセンブリ名の既定 (cross/ADR-0005 の写像表)。binding パッケージの Description に「直接参照しないでください」を明記し、輸送層であることを示す (maui/ADR-0001 の位置づけの表明)
- facade → binding の依存は SDK が書く下限指定のまま受け入れ、lockstep の同時発行と最小適用版解決で同版に揃える
- package README はルートの `README.md` を facade だけに同梱する (README はルート 2 枚のまま増やさない、cross/ADR-0012)。同梱される README の相対リンクは nuget.org から辿れないため、参照は public リポジトリの絶対 URL で書く
- バージョンは cross/ADR-0009 の lockstep に従い、3パッケージ同版で一斉発行する

### MAUI 本体の下限版は workload set 同梱の版

MAUI 本体 (`Microsoft.Maui.Controls`) の下限版は、リポジトリが固定する workload set が同梱する版とし、ライブラリをビルド・テストする版もその版に揃える。下限・実際に検証する版・同じ SDK の利用者の既定値が一致し、検証 CI が下限そのものを常時検証する。`global.json` の workload set を上げるときは `maui/Directory.Packages.props` の版を同梱版に合わせる (手順は handbook cross/local-development-setup.md)。

### 最低 OS 版は facade 同梱のビルド時ガードで検査

最低 OS 版 (cross/ADR-0002) は facade パッケージに同梱する `buildTransitive/` の props + targets で利用者ビルド時に検査し、platform TFM の inner build で `SupportedOSPlatformVersion` の評価後の値が要件未満なら、要件と設定方法を書いたエラー (`KSDLG0001`) で止める。素の `net10.0` や複数 TFM プロジェクトの outer build では何もしない。要件の数値は同梱 props を単一の宣言元とし、リポジトリ内の facade / binding の `SupportedOSPlatformVersion` もそこから供給する。利用者ビルドの資産を同梱することは「pack 内部構造に依存する自作 MSBuild を足さない」の対象外である。

## Alternatives Considered

- **単一パッケージに全部同梱する**: 却下。facade の pack に他プロジェクトの成果物を寄せる自作 MSBuild (TargetsForTfmSpecificBuildOutput + binding resource の手動コピー) が必要になり、maui/ADR-0003 の回避策の上にさらに壊れやすい層を積む。「1個だけ見える」美しさより標準経路の保守性を優先した
- **facade → binding の依存を完全一致 `[x.y.z]` にする自作ターゲット**: 却下。SDK 内部アイテム (`_ProjectReferencesWithVersions`) を書き換える必要があり「自作 MSBuild を足さない」の対象そのもの。binding は直接参照しない前提で、消費者側の版一致は消費者検証の依存検査が見る
- **自 assembly 用 aar を除かず利用者に `XA4301` の回避を案内する**: 却下。利用者ごとに回避策が要り、公開初版から警告が出る
- **自 assembly 用 aar の除去は消費者検証で `XA4301` を観測してから足す**: 却下。先例で原因と対処が確定済みで、観測を待つ理由がない
- **MAUI 本体の下限を検証済みの最新版 (10.0.70) に置く**: 却下。同じ workload set の素の利用者がダウングレード (NU1605) で止まり、`MauiVersion` の明記を強いる。姉妹ライブラリ KsSettingsView がこの版に留めた理由 (画像解決の版差) は KsDialogs には該当しない
- **下限を実測できた最低版 (10.0.0) と宣言し、ビルド版は別に置く**: 却下。下限が一度の実測だけで常時検証されない
- **最低 OS 版は README / skills の明記だけにする**: 却下。iOS の「リンクは通り実行時に落ちる」出荷を文書では防げない
- **iOS だけガードする**: 却下。Android も merger の読みにくいエラーより先に要件を書いた文面で止めたい
- **最低 OS 版の数値を targets と csproj に直書きする**: 却下。2 か所の同期が要る
- **facade 専用の package README を新設する**: 却下。cross/ADR-0012 の改訂と docs-refresh 対象の追加が要る

## Consequences

### 正の影響

- 正: pack の配線が `IsPackable=true` + PackageId のみで済み、SDK の標準経路に乗る
- 正: `dotnet pack` が native ビルド (Xcode / gradlew) を内包するため、リリース手順は3プロジェクトの pack で足りる
- 正: 消費者は `KsDialogs.Maui` 1点で iOS / Android 両対応になる
- 正: 同じ workload set の利用者は `Microsoft.Maui.Controls` の版を書かずに導入でき、下限は CI が毎回ビルド・テストする
- 正: 最低 OS 版の要件未満は両 OS ともビルド時に要件を書いたエラーで止まり、iOS の実行時クラッシュとして出荷されない
### 負の影響

- 負: 輸送層パッケージ2件が公開レジストリに見える。Description の注意書きで受け止める (CommunityToolkit 等の慣行と同じ)
- 負: binding resource の manifest に発行マシンの絶対パスが記録される。消費者ビルドでは無害だが、公開物に汎用のランナーのパスだけが載るよう pack は CI で行う
- 負: ライブラリ自身のビルドは workload 同梱版に留まり、それより新しい 10.0.x のバグ修正を自分の検証では使わない (利用者は上げてよい)
- 負: facade パッケージに build 資産 (`buildTransitive/`) が入り、要件の数値を変えるときは同梱 props とガードの文面を併せて見直す。この資産は推移的な全消費者へ import されるため、platform inner build 以外で無反応であることを崩せない
- 負: 自 assembly 用 aar の除去は SDK 内部ターゲットへの接続として残り、SDK 更新でターゲット名が変われば外れる。外れたことは pack 検算の aar 不在で検出する
- 負: facade → binding の依存は下限指定で、binding を直接参照する利用者は facade と版がずれ得る (Description で直接参照しないよう示す)
- 負: README の絶対 URL はブランチ名を含み、既定ブランチの改名で切れる (既定ブランチは削除・force-push 禁止、cross/ADR-0016)

## Revisit When

- MAUI テンプレートが版をリテラルで書くようになる、または workload set と `Microsoft.Maui.Controls` の版が独立に決まるようになったとき (下限ルールの前提)
- facade が 10.0.x の途中で追加された MAUI API を使い始めたとき (下限を同梱版より上げる必要が出る)
- .NET SDK が利用者側の最低 OS 版を自前で検査するようになったとき (ガードが重複する)
- SDK が pack からの aar 除外の公開手段、または ProjectReference 依存の完全一致指定を持ったとき (内部ターゲットへの接続と下限依存の受容を見直す)
- `global.json` の SDK を上げたとき (自 assembly 用 aar の生成有無と内部ターゲット名を pack 検算で確かめる)

出典: kasane/roadmaps/archive/2026-09-04-library-foundation/phases/phase-10-packaging-model/history.md (2026-08-17: 論点D PoC・決定) / kasane/roadmaps/package-distribution/phases/phase-6-maui-packaging/history.md (2026-09-08: MAUI 本体の下限版・最低 OS 版のビルド時ガード・提案化と相方スペックレビューの反映) / kasane/changes/archive/2026-09-08-add-maui-nuget-distribution/design.md (Decision 1〜4・7・8 — 採用案・理由・代替案) / kasane/changes/archive/2026-09-08-add-maui-nuget-distribution/deviation.md (自 assembly 用 aar の生成有無)

現行照合: 2026-09-08 確認 (実装完了時)。3 パッケージの pack 設定は maui/Directory.Build.props (共通メタデータ・版の既定値 `0.0.0-dev`・`IsPackable` 既定 false)・maui/Directory.Build.targets (アイコンの同梱・自 assembly 用 aar の検査と除去)・maui/KsDialogs.Maui/KsDialogs.Maui.csproj (Package ID・Description・README・`buildTransitive/` の同梱)・binding 2 件の csproj (`IsPackable` と Description)、下限は maui/Directory.Packages.props の `Microsoft.Maui.Controls` 10.0.20、ガードは maui/KsDialogs.Maui/buildTransitive/KsDialogs.Maui.props (Android 24 / iOS 17.0) と同 .targets (`KSDLG0001`)。実装で確定した観測: (1) KsDialogs では .NET Android SDK が自 assembly 用 aar を生成せず (facade / Android binding の Release・Debug とも)、除去の後処理は `Exists` 条件が不成立で走らない — 翻案元 KsSettingsView と異なる。後処理は SDK 挙動が変わったときの保険として残し、pack 検算の aar 不在が毎回の確認になる (2) 消費者検証 (一時プロジェクト) で restore 警告 0・両 OS Release ビルド成功・両 OS の起動確認・`KSDLG0001` の発火 (要件未満・Android の未設定 = SDK 既定 21.0) と非発火 (`net10.0` / outer build / iOS の未設定 = SDK 既定値) を実測。消費者側の `nuget.config` はローカルフィード + nuget.org 併記でないとテンプレート依存が NU1101 になる (3) facade の nupkg に XML ドキュメントが `net10.0-android` の lib にだけ入る (SDK 既定の非対称。方針は初回発行前に決める — package-distribution phase-9 へ申し送り) (4) SDK 10.0.300 既定の API 版付き TFM (`net10.0-android36.0` / `net10.0-ios26.0`) をそのまま受け入れた。判定: 維持

関連: cross/ADR-0009 (lockstep 単一バージョンと `-p:Version=` 注入) / cross/ADR-0002 (最低対象 OS の数値) / cross/ADR-0005 (Package ID と namespace の写像表) / cross/ADR-0012 (README はルート 2 枚) / cross/ADR-0018 (toolchain の repo 内固定 — `global.json` と CPM) / maui/ADR-0003 (binding の native ビルド連携)
