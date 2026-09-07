---
id: 0004
title: MAUI NuGet は facade + 輸送層 binding 2件の3パッケージ構成とし、SDK 標準の pack 経路で native 成果物を同梱する
status: proposed
date: 2026-08-17
---

## Context

MAUI 形態の配布は NuGet で、消費者の手数は「`KsDialogs.Maui` 1点」と決まっている (cross/ADR-0008)。native 成果物は binding 2件が生成する (iOS: XcodeProject アイテム経由の xcframework / Android: gradlew Exec 経由の aar 2件、maui/ADR-0003)。これらを NuGet にどう埋めるかが未決だった (両 binding は `IsPackable=false` の状態だった)。

配布モデル検討の PoC で、3パッケージ構成の pack と NuGet 消費を実測した (PoC 記録 poc-maui-nuget-packaging.md、出典参照)。

## Decision

- **3パッケージ構成**: facade (`KsDialogs.Maui`) + 輸送層 binding 2件 (`KsDialogs.Binding.iOS` / `KsDialogs.Binding.Android`) をそれぞれ NuGet パッケージとして発行し、facade が TFM 条件付き NuGet 依存で binding を参照する。消費者の手数は `KsDialogs.Maui` 1点のまま
- **native 成果物の同梱は SDK 標準の pack 経路**を使う: binding プロジェクトを `IsPackable=true` にするだけで、iOS は binding resource package (resources.zip 内に xcframework)、Android は aar 2件 (束縛対象 + `Bind=false` 同梱) が nupkg に入る。自作の pack 用 MSBuild は足さない
- binding パッケージの Description に「直接参照しないでください」を明記し、輸送層であることを示す (maui/ADR-0001 の位置づけの表明)
- バージョンは cross/ADR-0009 の lockstep に従い、3パッケージ同版で一斉発行する

## Alternatives Considered

- **単一パッケージに全部同梱する**: 却下。facade の pack に他プロジェクトの成果物を寄せる自作 MSBuild (TargetsForTfmSpecificBuildOutput + binding resource の手動コピー) が必要になり、maui/ADR-0003 の回避策の上にさらに壊れやすい層を積む。「1個だけ見える」美しさより標準経路の保守性を優先した

## Consequences

- 正: pack の配線が `IsPackable=true` + PackageId のみで済み、SDK の well-trodden path に乗る (PoC で MSB4120 回避策・Exec 方式と干渉しないことを実測済み)
- 正: `dotnet pack` が native ビルド (Xcode / gradlew) を内包するため、リリース手順は3プロジェクトの pack で足りる
- 正: 消費者は `KsDialogs.Maui` 1点で iOS / Android 両対応 (PoC で両ビルド + iOS 実行時動作を実証)
- 負: 輸送層パッケージ2件が公開レジストリに見える。Description の注意書きで受け止める (CommunityToolkit 等の慣行と同じ)
- 負: binding resource の manifest に発行マシンの絶対パスが記録される (SDK 標準挙動)。消費者ビルドでは無害だが、発行環境のパスが公開物に含まれることは認識しておく
- 未検証のまま発行実装へ送る項目: nuget.org 固有のメタデータ要件 (license・icon 等)、Release / trimming 構成での消費者ビルド、Android の NuGet 経由実行時確認

出典: kasane/roadmaps/library-foundation/phases/phase-10-packaging-model/history.md (2026-08-17: 論点D PoC・決定)
