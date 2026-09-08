# Native パッケージング (native-packaging)

Native iOS (SwiftPM 配信リポジトリ `KsDialogs-SPM` へのスナップショット) と Native Android (Maven Central へ `ksdialogs-core` / `ksdialogs` の 2 artifact、cross/ADR-0019) の発行機構を配線する change フェーズ。

## 論点

(出尽くした。2026-09-08)

## 決定事項

踏襲 (解決済み論点)。出典は cross/ADR-0008 (2026-09-04 改訂) (配信リポジトリ方式)、KsSettingsView cross/ADR-0018 と同 phase-4 / phase-5 の決定事項 (`../KsSettingsView/kasane/roadmaps/package-distribution/phases/phase-4-ios-packaging/agenda.md`・`phase-5-android-packaging/agenda.md`)。

- iOS: `scripts/spm-snapshot/sync-snapshot.sh` (+ テスト + README テンプレート) を踏襲。スナップショットはホワイトリスト (`ios/Package.swift` / `Sources/` / `Tests/` / ルート LICENSE / 誘導 README) のみ。スクリプトの責務は「配置まで」で commit / tag / push は呼び出し側。tag は接頭辞なし `X.Y.Z`。CI の書き込みは書き込み許可付き deploy key をリポジトリ単位 secret に (organization secret は姉妹ライブラリと鍵共有になるため不採用)
- Android: `com.vanniktech.maven.publish` + `publishToMavenCentral()` + `signAllPublications()`。release 単一 variant + sources jar + 空 javadoc jar (Dokka を持ち込まない)。`group` / `version` はルートで一括、`-SNAPSHOT` 中の Central 発行はガードで失敗させる
- Sample の composite build は明示 dependencySubstitution を安全装置として維持 (自動置換の不発時に公開版へ静かにフォールバックするのを防ぐ)
- Central Portal の `jp.kamusoft` 名前空間検証は完了済み (共用)。再実施不要
- library-foundation phase-10 の申し送りのうち「ルート Package.swift への一本化 (`ios/Package.swift` 廃止・maui binding の入力パス追随)」は cross/ADR-0008 (2026-09-04 改訂) で不要になった。`ios/Package.swift` はそのまま
- 却下済み: ルート Package.swift / 2 枚持ち / binary 配布 / 配信リポジトリ名 `-swift` `swift-` (cross/ADR-0008 (2026-09-04 改訂))。fat aar / bridge の公開 (KsSettingsView android/ADR-0016 のうち KsDialogs にも当てはまる部分)。Android の module 統合は翻案しない (android/ADR-0001 を維持)

### Android toolchain は据え置き、vanniktech maven publish 0.37.0 を採用 (2026-09-08)

KsDialogs の現行 toolchain (Gradle 9.7.0 / AGP 9.3.0 / Kotlin 2.4.10) は翻案元より新しく、追随 (上げ下げ) は不要。`com.vanniktech.maven.publish` は KsSettingsView と同じ **0.37.0** (Kotlin 2.4 で検証済みの唯一の版。最低要件 JDK 17 / Gradle 9.0 / AGP 8.13 / Kotlin 2.2 を満たし、検証済み範囲は AGP 9.2.1 / 9.3.0-rc01 / Kotlin 2.4.0 まで) を採用し、発行設定 (release 単一 variant + sources jar + 空 javadoc jar + SNAPSHOT ガード + 署名の必須/任意を鍵の有無に連動) を翻案元からコピーする。AGP 9.3.0 正式版での動作は検証済み一覧に無いため、`publishToMavenLocal` の dry-run と POM の検算を受け入れ条件に含める (発行検証の論点で扱う)。

- 却下: AGP を翻案元と同じ 8.13 系へ下げて揃える (KMP 側がカタログに宣言済みの `com.android.kotlin.multiplatform.library` は AGP 9 前提で phase-7 に響き、cross/ADR-0018 の toolchain 更新を別途起こす必要がある)

### 公開 ABI の `api` スコープは現状維持で確定、発行時に release aar を機械走査で検算 (2026-09-08)

両 module の公開宣言 (約 130 件) の全走査で、公開シグネチャに現れるモジュール外の非プラットフォーム型はカラー注釈 (`androidx.annotation.ColorInt`) と `androidx.compose.runtime.Composable` の 2 つだけで、どちらも既に `api`。coroutines の型は公開面に出ず (suspend 修飾子は stdlib で足りる)、compose 側の ui / lifecycle / savedstate を使う型は internal。KsSettingsView で漏れた 2 経路 (親クラス経由・プロパティ型経由) も KsDialogs には該当なし。依存スコープは変更しない。2 artifact の publication は compose → 本体を `api(project(":ksdialogs"))` のまま Maven の推移的依存として発行する。受け入れ条件として、発行時に release aar の公開シグネチャを機械走査 (javap) し外部型の集合を `api` 宣言と突き合わせる検算を 1 回通す (翻案元の 2 件目はこの経路で見つかった)。

- 却下: 静的読解の結果だけで確定 (発行物での確認がない) / 検算を Gradle 検査タスクとして常設 (外部型が 2 つの現状では投資に見合わない。既に explicit API mode と `api-surface-check` が逆向きの漏れを押さえている。外部型が増えたら見直す)

### Android の Maven 座標は本体を `ksdialogs-core`、Compose 側を `ksdialogs` にリネーム (2026-09-08)

Android 利用者には Compose が主な使い方なので、素の名前 `jp.kamusoft:ksdialogs` を Compose 側 (旧 `ksdialogs-compose`) に与え、View 系の本体を `jp.kamusoft:ksdialogs-core` にする。分離 (android/ADR-0001) は維持し、Compose 側 → 本体の `api` 推移依存もそのまま。MAUI binding と KMP は本体 (`ksdialogs-core`) を取り込む。iOS は 1 パッケージ `KsDialogs` に SwiftUI 対応まで入る「素の名前 = 全部入り」なので、Android も揃う。公開前のため利用者への影響はなく、コストは文書 (README 2・skills 12・concepts 5・handbook 2 ほか) と設定 (Gradle・MAUI binding の aar パス・KMP の依存宣言・samples の置換) の書き換え。決定は cross/ADR-0019 (proposed、cross/ADR-0005 と android/ADR-0001 の amends) に起票。roadmap のゴール文の座標表記は ksn-roadmap の改訂で追随する。

- 却下: 2 module の統合 (本体が Compose 本体に依存し、Compose を使わない MAUI / KMP / View 系の消費者に数 MB が届く。MAUI binding は Compose NuGet の追加と版合わせが要る) / 現状名の維持 (素の名前の中身が iOS とずれ、多数派の Compose 利用者が接尾辞付きを選ぶ)

### 座標リネームはディレクトリ / Gradle project 名まで、Kotlin パッケージ名と namespace は維持 (2026-09-08)

ディレクトリと Gradle project 名は座標に揃えて改名する (`android/ksdialogs` → `android/ksdialogs-core`、`android/ksdialogs-compose` → `android/ksdialogs`。project 参照は `:ksdialogs-core` / `:ksdialogs`)。ディレクトリと座標がねじれたまま残ると以後ずっと読み手を惑わせるため、公開前の今に揃える (翻案元 KsSettingsView も改名時に同じく揃えた)。Kotlin パッケージ名 (本体 `jp.kamusoft.ksdialogs` / Compose 側 `jp.kamusoft.ksdialogs.compose`) と AGP namespace は動かさない。artifact 名とパッケージ名を一致させる慣習はなく、動かすと利用者の import とソース・テスト・文書の全体が書き換わるのに利用者の得がない。追随箇所は MAUI binding の aar パスと gradlew の task 名、samples と kmp の依存置換、`api-surface-check` の project 参照。

### POM は共通部をルートで一括、name / description だけ module ごと (2026-09-08)

vanniktech plugin は `ksdialogs-core` / `ksdialogs` の 2 module にだけ適用し (`api-surface-check` は発行しない)、POM の共通部 (url / MIT license / developers / scm。翻案元の値の URL を `kamusoft/KsDialogs` に差し替え、inceptionYear 2026) は `android/build.gradle.kts` の subprojects 側で 1 回だけ書く (複製すると URL 変更時に片方が取り残される)。artifact 固有の name / description は各 module に置く。Swift パッケージ側は Package.swift のままでメタデータの追加なし。

| 座標 | name | description |
|---|---|---|
| `jp.kamusoft:ksdialogs` | `KsDialogs` | A dialog UI library for Android that presents dialogs, loading indicators, and toasts from anywhere in an application, with content written in Jetpack Compose. |
| `jp.kamusoft:ksdialogs-core` | `KsDialogs Core` | The Compose-free core of KsDialogs for Android: typed dialogs, loading indicators, and toasts built with Android Views. Use this artifact directly only when you do not use Jetpack Compose. |

### 発行検証は翻案元と同じ範囲、samples はソース参照を維持 (2026-09-08)

KsSettingsView の実装 change (add-spm-distribution / add-android-maven-distribution) の検証範囲をそのまま写す。samples は配布物参照に切り替えず (恒久も一時も不要)、consumer 境界の検証は phase-8 の `verification/` が dry-run / smoke で担う。samples に二重の役目を持たせると日常開発で発行が要るようになる。

- Android: `publishToMavenLocal` で発行物を検算し証跡を change に残す (aar / sources jar / 空 javadoc jar / POM の内容 / POM と `.module` の依存スコープ: `ksdialogs` → `ksdialogs-core` が `api`、テスト専用ライブラリの不在)。release aar の javap 検算を同居させる。Central への実発行は phase-9
- iOS: スナップショットの成果物を手動で commit・push し、検証用 prerelease tag (`X.Y.Z-alpha.N`) でリポジトリ外の一時消費者から https + exact 指定の解決・ビルドを確認する。確認後に tag を削除し配信リポジトリに残さない (翻案元のオーナー裁定)
- samples: composite build の明示置換を新座標 (`ksdialogs-core` / `ksdialogs`) に追随させ、本体ソースの変更が反映されることで置換の実効を確認する。iOS の Local Swift Package 参照は変更不要

### SPM スナップショットの初回 push と申し送りの確認 (2026-09-08)

配信リポジトリ `kamusoft/KsDialogs-SPM` の初期設定は phase-3 で完了済み (public・default `main`・全機能 OFF・PR は collaborators only) で追加作業なし。置いてある誘導 README は KsSettingsView のテンプレートと名前差し替え以外は同一と確認したので、スクリプト側の `README.template.md` は名前だけ差し替えて置き、初回 sync で同一内容に上書きされて整合する。同期スクリプト (`scripts/spm-snapshot/sync-snapshot.sh` + テスト) で KsDialogs 向けに変わるのは配信リポジトリ名 (origin の完全一致検査にも使う) とコメントだけで、ホワイトリスト 5 点のパスは同じ。`ios/Package.swift` は `path:` 指定なし・product 1 本のため無改変で置ける。maui の iOS binding は `../../../ios` の Local Swift Package 参照で product `KsDialogs` 1 本のみを参照し、影響なし。初回 push はこの phase の change で手動 (commit・push → 検証用 tag → https 解決確認 → tag 削除)。

### バージョンの単一ソースとリリース version の注入 (2026-09-08)

KsSettingsView 方式 (同 cross/ADR-0020) を踏襲する。開発用既定値はバージョンカタログ (`android/gradle/libs.versions.toml` の `ksdialogs`) に置き、値を `0.1.0-SNAPSHOT` に改める (現状の `0.1.0` は SNAPSHOT ガードが効かない)。`android/build.gradle.kts` を新設し、`-Pversion=` の注入値があればそれ、無ければカタログ値、という導出式で subprojects の `group` / `version` を一括設定する (各 module の直書きは消す)。`kmp/` も自分の version と本体 `jp.kamusoft:ksdialogs` への依存版を同じ導出式から取る (直書き `"0.1.0"` を廃止。配線は phase-7、式の形はここで確定)。リリース時は dispatch 入力 = `-Pversion=` の注入値 = tag = 各レジストリの version が同一文字列で流れ、初回リリースは KsSettingsView と同じく `0.1.0-beta.1` 形式の prerelease から始める。samples の composite build は座標で置換するため version の値に左右されず、SNAPSHOT 化の影響はない。ADR 化 (cross/ADR-0009 へ溶かすか新規か) は蒸留時に決める。

- 却下: カタログの値そのものを CI が書き換える方式 (作業木を汚し、tag との一致が書き換え工程の正しさに依存する) / version 専用ファイルの新設 (カタログが既に同じ役で冗長)

## 実装結果 (2026-09-08 反映)

change [add-native-distribution](../../../../changes/archive/2026-09-08-add-native-distribution/proposal.md) (L 級) で実装完了。レビュー 2 周 + 相方レビュー 2 周 APPROVED、verify-001 VALID (22 Scenario)。配信リポジトリ `KsDialogs-SPM` へスナップショット (49fc0a8) を初回 push し、検証用 tag `0.1.0-alpha.1` で https 解決と iOS Simulator 向けビルドを確認して tag を削除済み。

決定事項からの乖離は 2 点。instrumented test は API 36 のエミュレータではなく API 36 の実機で件数一致 (294 / 39) を確認した (手元にシステムイメージが無いため。エミュレータ条件は CI の instrumented job が担保)。同期スクリプトの `.git/` 以外の除去は `rm -rf` とした (オーナー裁定: 全体ルール「削除は trash」は人の操作環境向けで、CI スクリプトは対象外)。

蒸留では cross/ADR-0019 を accepted に昇格し (cross/ADR-0005・android/ADR-0001 に amended-by)、同期テストの lint job 追加を cross/ADR-0020 (cross/ADR-0017 の amends) として起票した。版の導出式は新規 ADR にせず cross/ADR-0009 (proposed) に溶かし、phase-7 / phase-9 の蒸留で昇格する。

### 申し送り

| 項目 | 受け皿 |
|---|---|
| `skills/` 12 箇所と README 2 枚の旧座標 (`jp.kamusoft:ksdialogs-compose` / 本体としての `jp.kamusoft:ksdialogs`) の追随は docs-refresh の責務。初回リリース前に必ず走らせる | [phase-9 agenda](../phase-9-release-workflow/agenda.md) の TODO に追記 |
| `verify-https-resolution.sh` は自動テストを持たない (`sync-snapshot.sh` の自己テストとの非対称。verify-001 の注記)。手動検証専用で release workflow から呼ばない | 見送り。phase-9 で同スクリプトを流用する判断が出たときに再考する |
| `kmp/` の version 導出式の配線 (cross/ADR-0009 の式を kmp/build.gradle.kts に写す) | [phase-7 agenda](../phase-7-kmp-packaging/agenda.md) 決定事項 A5 |
| phase-8 の Android 消費者の論点は旧座標で書かれていた | [phase-8 agenda](../phase-8-consumer-verification/agenda.md) の論点を新座標の表記に改めた |

## TODO

- [x] 論点の解消 (2026-09-08 出尽くし)
- [x] ksn-roadmap でゴール文の Android 座標表記を `ksdialogs-core` / `ksdialogs` に改訂する (2026-09-08 蒸留時に roadmap.md のゴール文を追随)
- [x] ksn-propose で変更提案を起こす (add-native-distribution、2026-09-08 完了)
