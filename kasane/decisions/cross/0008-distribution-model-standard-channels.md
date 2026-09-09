---
id: 0008
title: 配布は標準3チャネルのみとし、SwiftPM は配信リポジトリ (KsDialogs-SPM) で配り、KMP の Swift 参照は version から導出する
status: accepted
date: 2026-09-08
---

## Context

縦串実装 (add-vertical-slice) の実測で、KMP の iOS framework は static (`isStatic = true`) で Swift 実体を同梱せず、アプリが KMP と Swift パッケージの両方をリンクする構造が確定した (kmp/ADR-0002)。この構造を前提に、4形態 (Native iOS / Native Android / MAUI / KMP) の配布単位・チャネルを決める必要がある。

SwiftPM の git 配布はリポジトリルート直下の Package.swift しか解決できず (サブディレクトリ指定は未サポート)、`ios/Package.swift` のままでは SwiftPM 配布が成立しない。SwiftPM には Maven Central / NuGet.org のような実用的な中央サーバーがなく、git 直接参照 + semver tag が事実上の唯一解である。

姉妹ライブラリ KsSettingsView は、Package.swift をリポジトリルートへ移してモノレポを SwiftPM に直接解決させる案から出発して public 化の準備を進めた際に次を確かめ、SwiftPM 専用の配信リポジトリ方式へ転じて初回リリースまで実証した (KsSettingsView cross/ADR-0018):

- SwiftPM 利用者はモノレポを履歴ごと full clone する。`kasane/changes/` に蓄積する検証証跡の媒体 (KsSettingsView は公開時点で約 180 MB、以後も変更ごとに増える) がそのまま利用者のコストになり、配信の都合で開発の足場 (証跡の保存方法) を制約しなければならなくなる
- `ios/Package.swift` は `ios/` 相対で解決するので、`ios/` 配下をそのまま別リポジトリのルートへ置けば無改変で解決できる
- 「同期 CI と tag の二重管理」は release CI が tag を自動で打つことで、「URL と issue 窓口の分裂」は配信リポジトリの Issues / PR を無効化して monorepo へ誘導することで解消できる

KsDialogs も同じ構図にある (`ios/Package.swift` は target 1 本・`path:` 指定なし、`kasane/changes/archive/` に証跡が蓄積)。

KMP の発行時の Swift 参照には固有の制約がある。KGP の SwiftPM import (`swiftPMDependencies`) は Alpha 機能で、開発時は `localSwiftPackage(../ios)` を宣言している。PoC (library-foundation の packaging-model 検討) で、リモート参照 (`swiftPackage(url, exact)`) なら Kotlin SwiftPM 連携が消費者側まで成立する一方、`localSwiftPackage` のまま発行すると発行者マシンの絶対パスが metadata に乗って消費者ビルドが壊れ、警告も出ないことが実測された。また消費側 KGP の最低版と metadata 形式の互換は公式に記述がない (公式ページが挙げる版は試用の推奨版であり最低要件ではない)。

前提: 全形態を 1 つの release workflow が同一 version で一斉発行する (cross/ADR-0009)。リポジトリは public で、SwiftPM が git を直接解決できる。初回リリース前で、配布 URL を変えても利用者の移行が発生しない。

## Decision

**配布単位: 標準3チャネルのみ・独自成果物なし。**

| 形態 | 消費者が手で入れるもの | 自動で付いてくるもの |
|---|---|---|
| Native iOS | SwiftPM 依存 1点 (`KsDialogs`) | — |
| Native Android (View 系のみ) | Maven 依存 1点 (`jp.kamusoft:ksdialogs-core`) | — |
| Native Android (Compose コンテンツ利用時) | Maven 依存 1点 (`jp.kamusoft:ksdialogs`) | 本体 `ksdialogs-core` は Maven の推移的依存で自動 |
| MAUI | NuGet 1点 (facade) | binding 2件は NuGet の依存関係で自動 |
| KMP | Maven 依存 1点 (`jp.kamusoft:ksdialogs-kmp`) + iOS アプリ側に SwiftPM 依存 1点 | Android Native は `api` 宣言の Maven 推移的依存で自動 |

Android の Compose 系 API を別モジュールとして配る分離判断は android/ADR-0001、座標名 (本体 `ksdialogs-core` / Compose 側 `ksdialogs`) は cross/ADR-0019。バージョンは lockstep (cross/ADR-0009) に含める。KMP iOS 側の手動1点は、static framework が Swift 実体を同梱しない構造 (kmp/ADR-0002) の帰結として受け入れる。

**SwiftPM チャネル: 配信リポジトリへのスナップショット配布。**

SwiftPM 専用の公開配信リポジトリ **`KsDialogs-SPM`** (Package URL: `https://github.com/kamusoft/KsDialogs-SPM`) を別に持つ。release CI が `ios/Package.swift` と `ios/Sources/` `ios/Tests/` (と LICENSE) のスナップショットを配信リポジトリのルートへ commit し、同じ version の semver tag を push する。配信リポジトリの履歴はリリース回数分しか増えない。配信リポジトリの tag は monorepo の semver tag と同じ値を持ち (cross/ADR-0009)、配信リポジトリへの push は SwiftPM の publish 工程である。

モノレポのルートには Package.swift を置かない。`ios/Package.swift` が開発用かつ配信用の唯一のマニフェストであり (配信リポジトリ側はそのコピー)、2 枚持ちにはしない。cross/ADR-0004「ルートに共通ビルドファイルを置かない」への例外は不要である。

配信リポジトリは Issues / PR を無効化し、README で monorepo (ソース・Issue 窓口) へ誘導する。手で commit しない (CI のみが書く)。

配信リポジトリ名の `-SPM` サフィックスは配信専用リポジトリの既存慣例 (`airbnb/lottie-spm`・`RevenueCat/purchases-ios-spm` 等) に倣い、KsSettingsView cross/ADR-0018 が姉妹ライブラリの展開形として先に定めた名前を採る。利用者が `Package.swift` に書く identity は `KsDialogs-SPM`、Xcode 上の表示は package 名の `KsDialogs` になる。

**KMP の Swift 参照: version から導出し、専用のモード切替スイッチは持たない。**

- version (cross/ADR-0009 の導出式) が `-SNAPSHOT` なら `localSwiftPackage(../ios)`、それ以外 (リリース版の注入時) なら `swiftPackage(url, exact(<version>))` を宣言する。リリース版で local 参照を選べない形にして、絶対パスの伝播を構造的に防ぐ
- exact の値は version と同じ導出式から出し、上書き手段を持たない
- URL だけは Gradle プロパティ `ksdialogs.swiftPackageUrl` 1 つで上書きできる (既定は配信リポジトリ)。publish 前の検証は、スナップショットを同期したローカル clone に commit + tag した `file://` URL を注入して行う。SNAPSHOT では local 参照に URL が無いためプロパティは無視される

**サポートする Kotlin 範囲: 同 minor。** 消費者向けには「本ライブラリと同じ minor の KGP をサポートし、動作確認済みはリポジトリが固定する版 (バージョンカタログの `kotlin`)」と宣言する。次の minor が出たら消費者検証を回してから宣言を広げる。確認済み版の転記は利用者向けドキュメントの追従 (docs-refresh) が担う。

## Alternatives Considered

### 配布単位

| 案 | 却下理由 |
|---|---|
| KMP 向け umbrella SwiftPM 配布を追加する | umbrella が効く「KMP 面を Swift だけから使う人」には Native iOS ライブラリが正解で、想定利用者が不在。独自 artifact の維持コストと二重リンクの危険だけが残る |
| KMP framework を dynamic 化して Swift 実体を同梱する | 純 Native 併用時に Swift 実体が二重化し、kmp/ADR-0002 の「純 Native 利用者と KMP 利用者が同一レジストリを共有する」構図が壊れる |
| binary 配布 (xcframework を GitHub Release に添付し、ルート Package.swift は binaryTarget のみ) | 毎リリースの xcframework 生成と checksum 管理が CI に乗り、Swift バージョン依存も生じる。source 配布で成立する現状では不要 |

### SwiftPM チャネル

| 案 | 却下理由 |
|---|---|
| Swift Package Registry | 仕様 (SE-0292) はあるが公開レジストリの実用例がほぼない |
| ルート Package.swift へ移設しモノレポを直接配信する | SwiftPM 利用者がモノレポ全体を履歴ごと full clone するため、検証証跡の媒体が利用者の clone コストになり、配信の都合で足場の運用を制約することになる。cross/ADR-0004 への例外も要る。KsSettingsView が同じ案から出発して実際に問題化し、配信リポジトリ方式へ転じた実績がある |
| 開発用 `ios/Package.swift` と配布用ルート Package.swift の 2 枚持ち | 2 つのマニフェストが乖離する事故の温床になる |
| 配信リポジトリ名を `KsDialogs-swift` / `swift-ksdialogs` にする | 前者は本体の Swift 実装がそこにあると読まれ Issues / PR 無効化の方針と衝突し、後者は配信専用であることが名前から伝わらない (KsSettingsView cross/ADR-0018 の検討を踏襲) |

### KMP の Swift 参照と Kotlin 範囲

| 案 | 却下理由 |
|---|---|
| Swift 参照を Gradle プロパティで local / remote に明示切替する | フラグ忘れが残り、publish 時に local 参照なら失敗させるガードを別途書くことになる |
| version 導出のみで URL 上書きを持たない | publish 前の検証のたびに配信リポジトリへ一時 tag を push し、検証後に削除する運用になる |
| Kotlin の下限 2.4.0 のみを宣言し上限を置かない | 下限から確認済み版までの互換を確認していないのに約束することになる |
| Kotlin は確認済み版との完全一致のみ | patch 更新のたびに非サポートになり実用に耐えない |

## Consequences

### 配布単位と SwiftPM チャネル

- 正: 全形態がエコシステム標準機構のみで配布され、独自成果物の維持コストがゼロになる
- 正: Android Native は KMP の Maven 推移的依存で自動解決され、消費者の手数が最小化される
- 正: SwiftPM 利用者の clone は配信リポジトリ (ライブラリ本体のみ) で済み、monorepo の証跡・他 3 形態を引かない。monorepo 側は証跡媒体の保存方法を配信の都合で制約されない
- 正: `ios/Package.swift` は無改変で配信リポジトリのルートに置ける。iOS 開発は従来どおり `ios/` を開く形のまま (cross/ADR-0004 のビルドルート分離が例外なしに保たれる)
- 正: KsSettingsView と同じ配布構造になり、release workflow・スナップショット生成・消費者検証の仕組みをそのまま逆流できる
- 負: KMP 利用者は iOS アプリ側の SwiftPM 依存追加という手動手順が1点残り、ドキュメントでの案内が必須になる
- 負: 配信リポジトリという 2 つ目のリポジトリと、release CI がそこへ書き込むための secret (deploy key) を持つ。利用者から見て「ソース・Issue は monorepo、Package URL は配信リポジトリ」の 2 URL 体制になる
- 負: リポジトリの public 化が配布の前提条件になる (SwiftPM が git を直接解決するため)

### KMP の Swift 参照と Kotlin 範囲

- 正: exact 値が version と同じ式から出るため、KMP artifact x.y.z → SPM tag x.y.z の lockstep (cross/ADR-0009) を手で揃える箇所がない
- 正: 日常開発は SNAPSHOT のまま local 参照で、`../ios` のライブ編集も Sample の合成 Swift package の中身も変わらない
- 負: SNAPSHOT を Maven local へ発行した成果物には local 参照 (発行者の絶対パス) が載り、同一マシンでだけ動く。SNAPSHOT の消費はリポジトリ内 Sample の composite build が担い、リポジトリ外の検証はリリース版で行う
- 負: KMP 形態の配布は KGP の SwiftPM import 機能 (Alpha, `@ExperimentalKotlinGradlePluginApi`) に依存する。影響は KMP 消費者のビルド時のみ (Native / MAUI 利用者と実行時成果物には及ばない) で、KMP がリンク時に Swift 実体へ委譲する代替手段が現状ないため、見直し条件付きで受け入れる
- 負: Kotlin の minor が上がるたびに消費者検証とサポート範囲の宣言の更新が要る

## Revisit When

- KGP の SwiftPM import が Stable 化したとき、または swiftpm-metadata 形式の破壊的変更で消費者ビルドの互換が切れたとき
- iOS の binary (xcframework) 配布の要望が出たとき
- Kotlin の次の minor が出たとき (サポート範囲の宣言を広げる判断)
- 前提 (Context) が崩れたとき。特に形態ごとに別々の release workflow で発行するようになったとき

---
出典: kasane/roadmaps/archive/2026-09-04-library-foundation/phases/phase-10-packaging-model/history.md (2026-08-16: 論点A①・A②、2026-08-17: 論点C) / ../KsSettingsView/kasane/decisions/cross/0018-distribution-public-channels-root-swiftpm-manifest.md / kasane/roadmaps/package-distribution/exploration.md (A) — オーナー判断「KsSettingsView ADR-0018 を採用する」 / kasane/roadmaps/package-distribution/phases/phase-7-kmp-packaging/agenda.md (決定事項 A2・A4) / kasane/changes/archive/2026-09-09-add-kmp-maven-distribution/design.md (Decision 3)
現行照合: 2026-09-09 確認。`kmp/ksdialogs-kmp/build.gradle.kts` の `swiftPMDependencies` (version の SNAPSHOT 判定で local / remote を分岐、`ksdialogs.swiftPackageUrl` の既定は配信リポジトリ、`iosMinimumDeploymentTarget` 17.0 は分岐の外)。発行検証 (同 change の evidence/swiftpm-reference-derivation.txt) で SNAPSHOT は local 参照、`-Pversion=` 注入時は https + exact、`file://` の上書きが通ることを実測。リモート参照時の Kotlin SwiftPM 連携の消費者側成立は PoC (2026-08-17、poc-swiftpm-remote-distribution.md) で全 4 項目確認。既定 URL でのリリース版発行は root publication のみで実証 — KGP は発行時に SwiftPM パッケージを解決するため、cinterop klib を伴う iOS publication の発行には配信リポジトリに同版の tag が実在する必要がある (release workflow の順序への入力)。判定: 維持
関連: cross/ADR-0009 (lockstep と版の導出式。exact 値の入力) / cross/ADR-0019 (Android の Maven 座標名) / android/ADR-0001 (Compose 系の別モジュール分離) / kmp/ADR-0002 (static framework と View レジストリの Native 委譲) / kmp/ADR-0003 (Swift 向け登録 API は Swift パッケージ側)
