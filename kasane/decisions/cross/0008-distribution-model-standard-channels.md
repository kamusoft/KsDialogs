---
id: 0008
title: 配布は標準3チャネルのみとし、SwiftPM は配信リポジトリ (KsDialogs-SPM) で配る
status: proposed
date: 2026-08-16
---

## Context

縦串実装 (add-vertical-slice) の実測で、KMP の iOS framework は static (`isStatic = true`) で Swift 実体を同梱せず、アプリが KMP と Swift パッケージの両方をリンクする構造が確定した (kmp/ADR-0002)。この構造を前提に、4形態 (Native iOS / Native Android / MAUI / KMP) の配布単位・チャネルを決める必要がある。

SwiftPM の git 配布はリポジトリルート直下の Package.swift しか解決できず (サブディレクトリ指定は未サポート)、現状の `ios/Package.swift` のままでは SwiftPM 配布が成立しない。SwiftPM には Maven Central / NuGet.org のような実用的な中央サーバーがなく、git 直接参照 + semver tag が事実上の唯一解である。

当初 (2026-08-16) は Package.swift をリポジトリルートへ移してモノレポを SwiftPM に直接解決させる案を採ったが、姉妹ライブラリ KsSettingsView が同案を翻案して public 化の準備を進めた際に次が判明し、SwiftPM 専用の配信リポジトリ方式へ転じた (KsSettingsView cross/ADR-0018、2026-08-21 改訂、2026-09-04 に初回リリースまで実証済み):

- SwiftPM 利用者はモノレポを履歴ごと full clone する。`kasane/changes/` に蓄積する検証証跡の媒体 (KsSettingsView は公開時点で約 180 MB、以後も変更ごとに増える) がそのまま利用者のコストになり、配信の都合で開発の足場 (証跡の保存方法) を制約しなければならなくなる
- `ios/Package.swift` は `ios/` 相対で解決するので、`ios/` 配下をそのまま別リポジトリのルートへ置けば無改変で解決できる
- 当初懸念した「同期 CI と tag の二重管理」は release CI が tag を自動で打つことで、「URL と issue 窓口の分裂」は配信リポジトリの Issues / PR を無効化して monorepo へ誘導することで解消できる

KsDialogs も同じ構図にあり (`ios/Package.swift` は target 1 本・`path:` 指定なし、`kasane/changes/archive/` に証跡が蓄積)、発行実装は未着手のため実績のある形へ乗り換えるコストは今が最小である。本 ADR は 2026-09-04 にこの経緯で SwiftPM チャネルの節を改訂した。

## Decision

**配布単位: 標準3チャネルのみ・独自成果物なし。**

| 形態 | 消費者が手で入れるもの | 自動で付いてくるもの |
|---|---|---|
| Native iOS | SwiftPM 依存 1点 (`KsDialogs`) | — |
| Native Android | Maven 依存 1点 (`jp.kamusoft:ksdialogs`) | — |
| Native Android (Compose コンテンツ利用時) | Maven 依存 1点 (`jp.kamusoft:ksdialogs-compose`) | 本体 `ksdialogs` は Maven の推移的依存で自動 |
| MAUI | NuGet 1点 (facade) | binding 2件は NuGet の依存関係で自動 |
| KMP | Maven 依存 1点 (`ksdialogs-kmp`) + iOS アプリ側に SwiftPM 依存 1点 | Android Native は `api` 宣言の Maven 推移的依存で自動 |

Android の Compose 系 API を別モジュール `ksdialogs-compose` として配る分離判断は android/ADR-0001。バージョンは lockstep (cross/ADR-0009) に含める。

KMP iOS 側の手動1点は、static framework が Swift 実体を同梱しない構造 (kmp/ADR-0002) の帰結として受け入れる。

**SwiftPM チャネル: 配信リポジトリへのスナップショット配布 (2026-09-04 改訂)。**

- SwiftPM 専用の公開配信リポジトリ **`KsDialogs-SPM`** (Package URL: `https://github.com/kamusoft/KsDialogs-SPM`) を別に持ち、release CI が `ios/Package.swift` と `ios/Sources/` `ios/Tests/` (と LICENSE) のスナップショットを配信リポジトリのルートへ commit し、同じ version の semver tag を push する。配信リポジトリの履歴はリリース回数分しか増えない
- モノレポのルートには Package.swift を置かない。`ios/Package.swift` が開発用かつ配信用の唯一のマニフェストであり (配信リポジトリ側はそのコピー)、2 枚持ちにはしない。cross/ADR-0004「ルートに共通ビルドファイルを置かない」への例外は不要
- 配信リポジトリは Issues / PR を無効化し、README で monorepo (ソース・Issue 窓口) へ誘導する。手で commit しない (CI のみが書く)
- 配信リポジトリ名の `-SPM` サフィックスは配信専用リポジトリの既存慣例 (`airbnb/lottie-spm`・`RevenueCat/purchases-ios-spm` 等) に倣い、KsSettingsView cross/ADR-0018 が姉妹ライブラリの展開形として先に定めた名前を採る。利用者が `Package.swift` に書く identity は `KsDialogs-SPM`、Xcode 上の表示は package 名の `KsDialogs`
- 配信リポジトリの tag は monorepo の semver tag と同じ値を持つ (cross/ADR-0009)。配信リポジトリへの push は SwiftPM の publish 工程である
- KMP の発行時 Swift 参照は配信リポジトリを指す: `swiftPackage(url("https://github.com/kamusoft/KsDialogs-SPM"), exact(x.y.z))`。開発時の `localSwiftPackage(../ios)` はそのまま維持する

## Alternatives Considered

- **KMP 向け umbrella SwiftPM 配布を追加する**: 却下。umbrella が効く「KMP 面を Swift だけから使う人」には Native iOS ライブラリが正解で、想定利用者が不在。独自 artifact の維持コストと二重リンクの危険だけが残る
- **KMP framework を dynamic 化して Swift 実体を同梱する**: 却下。純 Native 併用時に Swift 実体が二重化し、kmp/ADR-0002 の「純 Native 利用者と KMP 利用者が同一レジストリを共有する」構図が壊れる
- **Swift Package Registry**: 却下。仕様 (SE-0292) はあるが公開レジストリの実用例がほぼない
- **ルート Package.swift へ移設しモノレポを直接配信する (2026-08-16 の当初案)**: 却下 (2026-09-04)。SwiftPM 利用者がモノレポ全体を履歴ごと full clone するため、検証証跡の媒体が利用者の clone コストになり、配信の都合で足場の運用を制約することになる。cross/ADR-0004 への例外も要る。KsSettingsView が同じ案から出発して実際に問題化し、配信リポジトリ方式へ転じた実績がある
- **開発用 `ios/Package.swift` と配布用ルート Package.swift の 2 枚持ち**: 却下。2 つのマニフェストが乖離する事故の温床になる
- **binary 配布 (xcframework を GitHub Release に添付し、ルート Package.swift は binaryTarget のみ)**: 却下。毎リリースの xcframework 生成と checksum 管理が CI に乗り、Swift バージョン依存も生じる。source 配布で成立する現状では不要。将来 binary 配布が必要になった時点で再検討する
- **配信リポジトリ名を `KsDialogs-swift` / `swift-ksdialogs` にする**: 却下。前者は本体の Swift 実装がそこにあると読まれ Issues / PR 無効化の方針と衝突し、後者は配信専用であることが名前から伝わらない (KsSettingsView cross/ADR-0018 の検討を踏襲)

## Consequences

- 正: 全形態がエコシステム標準機構のみで配布され、独自成果物の維持コストがゼロになる
- 正: Android Native は KMP の Maven 推移的依存で自動解決され、消費者の手数が最小化される
- 正: SwiftPM 利用者の clone は配信リポジトリ (ライブラリ本体のみ) で済み、monorepo の証跡・他 3 形態を引かない。monorepo 側は証跡媒体の保存方法を配信の都合で制約されない
- 正: `ios/Package.swift` は無改変で配信リポジトリのルートに置ける。iOS 開発は従来どおり `ios/` を開く形のまま (cross/ADR-0004 のビルドルート分離が例外なしに保たれる)
- 正: KsSettingsView と同じ配布構造になり、release workflow・スナップショット生成・消費者検証の仕組みをそのまま逆流できる
- 負: KMP 利用者は iOS アプリ側の SwiftPM 依存追加という手動手順が1点残り、ドキュメントでの案内が必須になる
- 負: 配信リポジトリという 2 つ目のリポジトリと、release CI がそこへ書き込むための secret (deploy key) を持つ。利用者から見て「ソース・Issue は monorepo、Package URL は配信リポジトリ」の 2 URL 体制になる
- 負: リポジトリの public 化が配布の前提条件になる (SwiftPM が git を直接解決するため)。消費者検証の publish 前 dry-run は配信リポジトリの prerelease tag か `path:` 参照で行う必要がある
- 検証済み: リモート SwiftPM 参照での Kotlin SwiftPM 連携の配布時成立性は PoC で全4項目成立を確認した (2026-08-17。PoC 記録 poc-swiftpm-remote-distribution.md、出典参照)。発行時は `swiftPackage(url(...), exact(...))` のリモート参照が必須で、`localSwiftPackage` のまま発行すると発行者マシンの絶対パスが伝播して消費者ビルドが壊れる。dev (ローカル参照) / publish (リモート参照) の切り替え機構が発行実装に必要
- 負: KMP 形態の配布は KGP の SwiftPM import 機能 (Alpha, `@ExperimentalKotlinGradlePluginApi`) に依存する。影響は KMP 消費者のビルド時のみ (Native / MAUI 利用者と実行時成果物には及ばない) で、KMP がリンク時に Swift 実体へ委譲する代替手段が現状ないため、見直し条件付きで受け入れる。消費者向けにサポートする Kotlin バージョン範囲をドキュメントで宣言する。**見直し条件**: 機能の Stable 化、または swiftpm-metadata 形式の破壊的変更で消費者ビルドの互換が切れたとき

出典: kasane/roadmaps/archive/2026-09-04-library-foundation/phases/phase-10-packaging-model/history.md (2026-08-16: 論点A①・A②、2026-08-17: 論点C)
出典 (2026-09-04 SwiftPM 節の改訂): ../KsSettingsView/kasane/decisions/cross/0018-distribution-public-channels-root-swiftpm-manifest.md / kasane/roadmaps/package-distribution/exploration.md (A) — オーナー判断「KsSettingsView ADR-0018 を採用する。0008 は KsSettingsView 側で問題が発生して 0018 の形になったため」
