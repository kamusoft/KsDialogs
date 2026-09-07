# 配布モデルの設計 (packaging-model)

4形態 (Native iOS / Native Android / MAUI / KMP) の配布単位・リンク構造・バージョン整合を設計し、phase-5 の API 表面設計への入力を確定する research フェーズ。phase-4 縦串の実測 (KMP framework は static で Swift 実体を同梱しない = アプリが KMP と Swift パッケージの両方をリンクする) が出発点。

## 論点

(全論点解消済み。2026-08-16 に旧8論点を6論点 A〜F に統合し、2026-08-17 までに全件を決定事項へ昇格した)

## 決定事項

- **配布単位は「標準3チャネル + KMP 手動2点」(2026-08-16, 論点A①)**: 各形態ともエコシステム標準機構のみに乗せ、umbrella 等の独自成果物は作らない。Native iOS = SwiftPM 1点 / Native Android = Maven 1点 / MAUI = NuGet 1点 (binding は依存で自動) / KMP = Maven 1点 + iOS アプリ側 SwiftPM 1点 (Android Native は `api` 宣言の推移的依存で自動)。KMP iOS 側の手動1点は static framework が Swift 実体を同梱しない構造 (kmp/ADR-0002) の帰結
- **SwiftPM チャネルはルート配布マニフェストへの一本化 (2026-08-16, 論点A②)**: SwiftPM の git 配布はルート直下 Package.swift しか解決できないため、Package.swift をリポジトリルートへ移し、ソース・Tests は `path:` 参照で ios/ 配下に残す。`ios/Package.swift` は廃止 (2枚持ちの乖離防止)。モノレポの semver tag がそのまま SwiftPM バージョンになる。配布用ミラーリポジトリは同期機構のコストに見合わず不採用 (バイナリ配布が必要になったら再検討)。cross/ADR-0004「ルートに共通ビルドファイルなし」へは SwiftPM 配布制約による例外として改訂を追記する (→ cross/0008 ADR)
- **Kotlin SwiftPM 連携の配布時成立性は「成立」(2026-08-17, 論点C)**: PoC 全4項目成立 ([結果レポート](artifacts/poc-swiftpm-remote-distribution.md))。発行時は `swiftPackage(url(...), exact(...))` のリモート参照が必須 (`localSwiftPackage` は絶対パスが伝播して消費者ビルドが壊れる)。dev (ローカル参照) / publish (リモート参照) の切り替え機構と integrateLinkagePackage の利用者手順ドキュメントは phase-11 へ申し送り。SwiftPM import が Alpha であることは「KMP 形態の消費者ビルドのみ・ビルド時のみ」の影響 (実行時影響なし・代替手段なし) として見直し条件付きで受け入れ、cross/0008 に記録する
- **バージョン整合は全形態 lockstep 単一バージョン (2026-08-17, 論点B)**: 全 artifact (SwiftPM tag / Maven `ksdialogs`・`ksdialogs-kmp` / NuGet) を同一 x.y.z で一斉リリースし、モノレポ tag と一致させる。KMP→Swift は `exact(同版)`、KMP→Android Native も同版の厳密指定で機械的に強制し、版違いの組み合わせは非サポート (互換表なし)。deployment target (iOS 17) の配布物での担保は「Swift パッケージの platforms 宣言 + KMP metadata の iosMinimumDeploymentTarget」が正で、`-Xoverride-konan-properties` は手元でリンクする binary 用の内部ビルド詳細と整理 (→ cross/0009 ADR)
- **Swift 向け KMP 登録 API は Swift パッケージ側に置く (2026-08-17, 論点E)**: KMP 消費者は klib から自分の framework をビルドするため、klib 側の公開面は消費者の `export(...)` 設定を強要し ObjC 経由で型も劣化する。消費者アプリはすでに Swift パッケージをリンクしており (cross/0008 の「アプリ側 SwiftPM 1点」)、そこに利用者向け公開登録 API を置く。`KsDialogsInteropBridge` は KMP cinterop 委譲専用の機械面として残す。API の形の設計と Sample の差し替え (verify-001 ❌3 の解消) は phase-5 へ (→ kmp/0003 ADR)
- **MAUI NuGet は3パッケージ構成 (2026-08-17, 論点D)**: facade (`KsDialogs.Maui`) + 輸送層 binding 2件 (`KsDialogs.Binding.iOS` / `KsDialogs.Binding.Android`) を TFM 条件付き NuGet 依存で接続し、cross/0009 の lockstep で同版一斉発行。binding の pack は SDK 標準経路 (`IsPackable=true` のみで xcframework の resources.zip / aar 2件が同梱される) を使い、自作 MSBuild は足さない。PoC 全4項目成立 ([結果レポート](artifacts/poc-maui-nuget-packaging.md))。輸送層は Description で「直接参照しない」を明示 (→ maui/0004 ADR)
- **リポジトリ内生成物は現状の追跡状態を正として確定 (2026-08-17, 論点F)**: `kmp/.swiftpm-locks/` は KGP 生成の `.gitignore` に従う (合成パッケージのマニフェストはコミット・絶対パスを含む swiftPMCheckout/ は ignore 済み)。`samples/kmp/iosApp/KotlinMultiplatformLinkedPackage/` は JetBrains 公式指示どおりコミット (Xcode が参照する実体で、fresh clone の体験を守る)。コミット対象に絶対パスの残留がないことは検査済み。軽い決定のため ADR なし。phase-11 のリモート参照切り替えで合成パッケージの中身が変わる差分は phase-11 の変更に含める

## TODO

- [x] 論点の解消 (全6論点を決定事項へ昇格)
- [x] 論点A②の決着後、A①+A② をまとめた配布モデル ADR (cross) を proposed で起票する → [cross/0008](../../../../decisions/cross/0008-distribution-model-standard-channels.md)
- [ ] 蒸留時に cross/0004 の Consequences へルート Package.swift 例外 (cross/0008) を追記する
- [ ] phase-11 への申し送り (各決定事項に記載): dev/publish の Swift 参照切り替え機構・publish 配線の本実装・integrateLinkagePackage の利用者手順ドキュメント・サポート Kotlin バージョン範囲の宣言・nuget.org メタデータ要件・Release/trimming 検証・Android の NuGet 経由実行時確認。スパイクブランチ `spike/phase-10-packaging-poc` は参考実装として残置 (本実装は書き直す)
- [x] 調査結果のまとめ (決定事項 + artifacts/ の PoC レポート2本が本フェーズの成果)
- [x] ksn-roadmap で research 完了をマーク (2026-08-17)

## 調査結果 (2026-08-17 完了)

配布モデルの全論点 (6件) を決定事項へ昇格して research 完了。成果は決定事項セクション + ADR 4件 (cross/0008・cross/0009・kmp/0003・maui/0004、いずれも proposed) + PoC レポート2本 ([SwiftPM リモート配布](artifacts/poc-swiftpm-remote-distribution.md) / [MAUI NuGet packaging](artifacts/poc-maui-nuget-packaging.md))。

後続フェーズへの影響:
- **phase-5**: 登録 API の置き場所が Swift パッケージ側に確定 (kmp/0003)。API の形の設計と Sample の機械面直接利用の差し替え (verify-001 ❌3 解消) が phase-5 の作業になる
- **phase-11**: 配布モデルの実装課題一式を申し送り (TODO 参照)。PoC で pack・publish の成立は実証済みのため、phase-11 は「本配線 + メタデータ + 検証」に集中できる
