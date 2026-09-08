---
id: 0004
title: MAUI NuGet は facade + 輸送層 binding 2件の3パッケージ構成とし、SDK 標準の pack 経路で native 成果物を同梱する
status: proposed
date: 2026-09-08
---

## Context

MAUI 形態の配布は NuGet で、消費者の手数は「`KsDialogs.Maui` 1点」と決まっている (cross/ADR-0008)。native 成果物は binding 2件が生成する (iOS: XcodeProject アイテム経由の xcframework / Android: gradlew Exec 経由の aar 2件、maui/ADR-0003)。これらを NuGet にどう埋めるかが未決だった (両 binding は `IsPackable=false` の状態だった)。

配布モデル検討の PoC で、3パッケージ構成の pack と NuGet 消費を実測した (PoC 記録 poc-maui-nuget-packaging.md、出典参照)。パッケージング段階の議論で、利用者側の要件 (MAUI 本体の版・最低 OS 版) をどう伝えるかも併せて決めた。

前提:
- MAUI テンプレートは `Microsoft.Maui.Controls` の版をリテラルで書かず、workload 同梱の既定値 (`$(MauiVersion)`) を使う。利用者の既定値は導入している workload set で決まる
- facade が使う MAUI の API は 10.0.x の途中で追加されたものを含まない (10.0.0 でビルドとユニットテストが通ることを実測)
- iOS の xcframework は最低 OS 版 (iOS 17) を目標に組まれるが、利用者アプリがそれより低い `SupportedOSPlatformVersion` のままでもリンクは通り、実行時に落ちる形で出荷できてしまう。Android は aar の manifest により merger が止めるがエラー文は要件を示さない

## Decision

### 3パッケージ構成と SDK 標準の pack 経路

facade (`KsDialogs.Maui`) + 輸送層 binding 2件 (`KsDialogs.Binding.iOS` / `KsDialogs.Binding.Android`) をそれぞれ NuGet パッケージとして発行し、facade が TFM 条件付き NuGet 依存で binding を参照する。消費者の手数は `KsDialogs.Maui` 1点のまま。

native 成果物の同梱は SDK 標準の pack 経路を使う。binding プロジェクトを `IsPackable=true` にするだけで、iOS は binding resource package (resources.zip 内に xcframework)、Android は aar 2件 (束縛対象 + `Bind=false` 同梱) が nupkg に入る。pack の内部構造に依存する自作 MSBuild は足さない。

- binding パッケージの Description に「直接参照しないでください」を明記し、輸送層であることを示す (maui/ADR-0001 の位置づけの表明)
- バージョンは cross/ADR-0009 の lockstep に従い、3パッケージ同版で一斉発行する

### MAUI 本体の下限版は workload set 同梱の版

MAUI 本体 (`Microsoft.Maui.Controls`) の下限版は、リポジトリが固定する workload set が同梱する版とし、ライブラリをビルド・テストする版もその版に揃える。下限・実際に検証する版・同じ SDK の利用者の既定値が一致し、検証 CI が下限そのものを常時検証する。`global.json` の workload set を上げるときは `maui/Directory.Packages.props` の版を同梱版に合わせる。

### 最低 OS 版は facade 同梱のビルド時ガードで検査

最低 OS 版 (cross/ADR-0002) は facade パッケージに同梱する `buildTransitive/` の props + targets で利用者ビルド時に検査し、要件未満なら要件を書いたエラー (`KSDLG0001`) で止める。要件の数値は同梱 props を単一の宣言元とし、リポジトリ内の `SupportedOSPlatformVersion` もそこから供給する。利用者ビルドの資産を同梱することは「pack 内部構造に依存する自作 MSBuild を足さない」の対象外である。

## Alternatives Considered

- **単一パッケージに全部同梱する**: 却下。facade の pack に他プロジェクトの成果物を寄せる自作 MSBuild (TargetsForTfmSpecificBuildOutput + binding resource の手動コピー) が必要になり、maui/ADR-0003 の回避策の上にさらに壊れやすい層を積む。「1個だけ見える」美しさより標準経路の保守性を優先した
- **MAUI 本体の下限を検証済みの最新版 (10.0.70) に置く**: 却下。同じ workload set の素の利用者がダウングレード (NU1605) で止まり、`MauiVersion` の明記を強いる。姉妹ライブラリ KsSettingsView がこの版に留めた理由 (画像解決の版差) は KsDialogs には該当しない
- **下限を実測できた最低版 (10.0.0) と宣言し、ビルド版は別に置く**: 却下。下限が一度の実測だけで常時検証されない
- **最低 OS 版は README / skills の明記だけにする**: 却下。iOS の「リンクは通り実行時に落ちる」出荷を文書では防げない
- **iOS だけガードする**: 却下。Android も merger の読みにくいエラーより先に要件を書いた文面で止めたい

## Consequences

- 正: pack の配線が `IsPackable=true` + PackageId のみで済み、SDK の well-trodden path に乗る (PoC で MSB4120 回避策・Exec 方式と干渉しないことを実測済み)
- 正: `dotnet pack` が native ビルド (Xcode / gradlew) を内包するため、リリース手順は3プロジェクトの pack で足りる
- 正: 消費者は `KsDialogs.Maui` 1点で iOS / Android 両対応 (PoC で両ビルド + iOS 実行時動作を実証)
- 正: 同じ workload set の利用者は `Microsoft.Maui.Controls` の版を書かずに導入でき、下限は CI が毎回ビルド・テストする
- 正: 最低 OS 版の要件未満は両 OS ともビルド時に要件を書いたエラーで止まり、iOS の実行時クラッシュとして出荷されない
- 負: 輸送層パッケージ2件が公開レジストリに見える。Description の注意書きで受け止める (CommunityToolkit 等の慣行と同じ)
- 負: binding resource の manifest に発行マシンの絶対パスが記録される (SDK 標準挙動)。消費者ビルドでは無害だが、発行環境のパスが公開物に含まれることは認識しておく
- 負: ライブラリ自身のビルドは workload 同梱版に留まり、それより新しい 10.0.x のバグ修正を自分の検証では使わない (利用者は上げてよい)
- 負: facade パッケージに build 資産 (`buildTransitive/`) が入り、要件の数値を変えるときは同梱 props とガードの文面を併せて見直す

## Revisit When

- MAUI テンプレートが版をリテラルで書くようになる、または workload set と `Microsoft.Maui.Controls` の版が独立に決まるようになったとき (下限ルールの前提)
- facade が 10.0.x の途中で追加された MAUI API を使い始めたとき (下限を同梱版より上げる必要が出る)
- .NET SDK が利用者側の最低 OS 版を自前で検査するようになったとき (ガードが重複する)

出典: kasane/roadmaps/library-foundation/phases/phase-10-packaging-model/history.md (2026-08-17: 論点D PoC・決定) / kasane/roadmaps/package-distribution/phases/phase-6-maui-packaging/history.md (2026-09-08: 論点 4 MAUI 本体の下限版・論点 5 最低 OS 版のビルド時ガード)
