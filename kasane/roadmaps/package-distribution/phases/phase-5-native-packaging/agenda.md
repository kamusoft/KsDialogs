# Native パッケージング (native-packaging)

Native iOS (SwiftPM 配信リポジトリ `KsDialogs-SPM` へのスナップショット) と Native Android (Maven Central へ `ksdialogs` / `ksdialogs-compose` の 2 artifact) の発行機構を配線する change フェーズ。

## 論点

### library-foundation phase-11-packaging から移送

- パッケージメタデータの整備 — Maven の POM (名前・説明・ライセンス・SCM・developers)、Swift パッケージ側は Package.swift のまま
- 発行検証 — ローカル / CI での dry-run publish (`mavenLocal()` と配信リポジトリのスナップショットへの `path:` 参照) と、配布物からの解決検証 (消費者側は phase-8)
- samples の配布物参照への切り替え検証 — Local Swift Package / composite build を配布物参照に差し替えて consumer 境界を最終確認する (恒久切り替えか検証のみか)
- バージョン整合の機械的担保 — 単一バージョンソース。KsSettingsView は version の正を Gradle version catalog に置き、リリース version は CI が `-Pversion=` で注入する形 (cross/ADR-0020)

### KsDialogs 固有

- Android toolchain 版 (Gradle / AGP / Kotlin) の追随要否: KsSettingsView は phase-1 で Gradle 9.5.0 / AGP 8.13.2 / Kotlin 2.4.10 に上げた。KsDialogs の現行版と vanniktech plugin の要件を着手時に実測して決める
- 2 artifact (`ksdialogs` / `ksdialogs-compose`) の publication: compose → 本体は Maven の推移的依存 (`api`)。公開 ABI に露出する外部型の `api` スコープ仕分け (KsSettingsView で列挙漏れが 2 度検出された) を公開宣言の全走査で確定する
- 配信リポジトリのスナップショット: `ios/Package.swift` は `path:` 指定なし (既定の `Sources/` `Tests/`) で無改変で置ける。product は 1 本のため umbrella の問題はない。maui binding (`ios/binding`) が個別 product を参照していないか着手時に確認する

## 決定事項

踏襲 (解決済み論点)。出典は cross/ADR-0008 (2026-09-04 改訂) (配信リポジトリ方式)、KsSettingsView cross/ADR-0018 と同 phase-4 / phase-5 の決定事項 (`../KsSettingsView/kasane/roadmaps/package-distribution/phases/phase-4-ios-packaging/agenda.md`・`phase-5-android-packaging/agenda.md`)。

- iOS: `scripts/spm-snapshot/sync-snapshot.sh` (+ テスト + README テンプレート) を踏襲。スナップショットはホワイトリスト (`ios/Package.swift` / `Sources/` / `Tests/` / ルート LICENSE / 誘導 README) のみ。スクリプトの責務は「配置まで」で commit / tag / push は呼び出し側。tag は接頭辞なし `X.Y.Z`。CI の書き込みは書き込み許可付き deploy key をリポジトリ単位 secret に (organization secret は姉妹ライブラリと鍵共有になるため不採用)
- Android: `com.vanniktech.maven.publish` + `publishToMavenCentral()` + `signAllPublications()`。release 単一 variant + sources jar + 空 javadoc jar (Dokka を持ち込まない)。`group` / `version` はルートで一括、`-SNAPSHOT` 中の Central 発行はガードで失敗させる
- Sample の composite build は明示 dependencySubstitution を安全装置として維持 (自動置換の不発時に公開版へ静かにフォールバックするのを防ぐ)
- Central Portal の `jp.kamusoft` 名前空間検証は完了済み (共用)。再実施不要
- library-foundation phase-10 の申し送りのうち「ルート Package.swift への一本化 (`ios/Package.swift` 廃止・maui binding の入力パス追随)」は cross/ADR-0008 (2026-09-04 改訂) で不要になった。`ios/Package.swift` はそのまま
- 却下済み: ルート Package.swift / 2 枚持ち / binary 配布 / 配信リポジトリ名 `-swift` `swift-` (cross/ADR-0008 (2026-09-04 改訂))。fat aar / bridge の公開 (KsSettingsView android/ADR-0016 のうち KsDialogs にも当てはまる部分)。Android の module 統合は翻案しない (android/ADR-0001 を維持)

## TODO

- [ ] 論点の解消 (toolchain 追随・`api` 仕分け・samples の参照切替の扱い)
- [ ] ksn-propose で変更提案を起こす
