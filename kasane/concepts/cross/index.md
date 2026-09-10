# cross concepts 目次

リポジトリ横断のメタ事項のうち、**記述** (今どうなっているか) にあたるもの。

横断的な開発規約 (コメント・テスト実行・実行時挙動の検証・Sample パリティ・移植元参照) は**規範**のため handbook へ移送した → [handbook: cross](../../handbook/cross/index.md)

## architecture/

- [distribution-artifacts.md](architecture/distribution-artifacts.md) — 配布物の構成 (4 形態): SwiftPM 配信リポジトリのスナップショット・Android Maven 2 座標・MAUI NuGet 3 パッケージ (README / XML ドキュメント / `buildTransitive` の `KSDLG0001` の同梱、MAUI 本体の下限)・KMP Maven 5 publication と Android ホストへの推移、version の単一ソースと注入、開発版を公開レジストリへ流さないガード
- [release-workflow.md](architecture/release-workflow.md) — release workflow (4 形態の一斉公開): 6 段の構成、取り消せる順に並べた publish の順序、Maven Central の 2 枠 deployment と ID の引き継ぎ、印 (marker) による再実行の続行判定、README / Skill のインストール例の自動置換、反映待ちと smoke
- [consumer-verification.md](architecture/consumer-verification.md) — 消費者検証 (`verification/`): 配布物を利用者と同じ経路で解決して Release ビルドする 4 形態の消費者プロジェクト、dry-run / smoke の参照先の排他、フィード準備と消費者ビルドの 2 段、KMP の dry-run の組み立て (合成 version・tag 付きローカル clone・smoke 形 fixture)、release workflow が渡す artifact の配置

## reference/

- [reference-repositories.md](reference/reference-repositories.md) — 外部参考リポジトリ (AiForms.Maui.Dialogs / KsSettingsView / KsAppKMP) のローカルパス対応表。パスは移動・リネームで腐る時限情報のため、この1ファイルに集約する
