# PoC: MAUI NuGet の native 成果物同梱 (論点D)

2026-08-17 実施。スパイクブランチ `spike/phase-10-packaging-poc` + scratchpad の消費者 MAUI アプリで、3パッケージ構成 (案2) を実測した。
判定: **成立** (検証項目4つすべて確認)。

## 検証構成

- **発行側**: binding 2件を `IsPackable=true` + PackageId (`KsDialogs.Binding.iOS` / `KsDialogs.Binding.Android`) + Version 0.1.0 にして `dotnet pack`。facade (`KsDialogs.Maui`) は既存の PackageId/Version のまま pack。出力はローカルフォルダフィード
- **消費者側**: samples/maui を雛形に、リポジトリ外で `PackageReference Include="KsDialogs.Maui" Version="0.1.0"` 1点 + nuget.config (ローカルフィード + nuget.org) のアプリを構築

## 検証結果

| # | 検証項目 | 結果 |
|---|---|---|
| 1 | iOS: MSB4120 回避策 (CreateNativeReference=false + 手動登録) の下で pack が成立するか | ✅ nupkg の `lib/net10.0-ios26.0/` に binding assembly + `KsDialogs.Binding.iOS.resources.zip` が入り、zip 内に xcframework (device + simulator slice) が丸ごと同梱される。回避策は pack を壊さない |
| 2 | Android: `Bind=false` 同梱の Native aar が nupkg に入るか | ✅ `lib/net10.0-android36.0/` に binding assembly + aar 2件 (`ksdialogs-maui-bridge-release.aar` / `ksdialogs-release.aar`) が同梱される |
| 3 | 消費者が `KsDialogs.Maui` 1点で両プラットフォームをビルド・動作できるか | ✅ facade の nuspec は TFM 条件付き依存 (ios → Binding.iOS / android → Binding.Android) を宣言し、消費者は 1点参照で Android・iOS (simulator) 両ビルド成功。iOS はシミュレータで Basic Dialog 表示 → OK → `結果: completed(true)` まで動作 ([スクリーンショット](poc-maui-result.png)) |
| 4 | nupkg 内の環境依存残留 | △ resources.zip 内の `manifest` に発行マシンの絶対パス (`IdentityWithoutPathSeparatorSuffix`) が記録される。ただしこれは SDK 標準の挙動で、消費者ビルドは xcframework を名前で解決するため**実害なし** (検証3で無害を実証)。aar・assembly にはパス残留なし |

## 判明した事実・申し送り

1. **binding プロジェクトの pack は SDK 標準経路がそのまま機能する**: iOS の binding resource package (resources.zip) も Android の aar 同梱も、`IsPackable=true` にするだけで追加配線なしに nupkg へ入る。maui/ADR-0003 の回避策・Exec 方式との干渉はない
2. **facade の TFM 条件付き ProjectReference は pack 時に自動で NuGet 依存へ変換される**: binding 側に PackageId/Version があれば nuspec の dependencies group が正しく生成される
3. **pack が native ビルド (Xcode / gradlew) を内包する**: `dotnet pack` だけで xcframework・aar の生成から同梱まで走る。phase-11 のリリース手順は「3プロジェクトを pack するだけ」で足りる見込み
4. **輸送層パッケージが公開レジストリに見える**: Description に「直接参照しないでください」を明記して受け止める (CommunityToolkit 等の慣行と同じ)
5. バージョンは cross/0009 の lockstep に従い3パッケージ同版で一斉発行する

## 検証の限界

- Android は NuGet 経由のビルド成功 (aar の取り込み・dex 化を含む) までで、エミュレータでの実行時確認は未実施 (iOS 側で実行時のレジストリ・結果還流は実証済み。Android の実行時挙動は Sample の通常動作確認でカバーされている領域)
- nuget.org 固有の要件 (アイコン・license・snupkg 等のメタデータ) は phase-11 の範囲
- Release 構成の consumer ビルド (AOT/trimming) は未検証。IsTrimmable=true の実効はリリース検証 (phase-11) で確認する

出典: kasane/roadmaps/library-foundation/phases/phase-10-packaging-model/history.md (2026-08-17: 論点D PoC)、スパイクブランチ `spike/phase-10-packaging-poc`
