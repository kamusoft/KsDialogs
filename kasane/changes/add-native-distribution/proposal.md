# Proposal: add-native-distribution

## Why

Native iOS / Native Android の配布経路は決定済み (cross/ADR-0008: SwiftPM は配信リポジトリ `KsDialogs-SPM` へのスナップショット、Android は Maven Central へ 2 artifact) だが、発行の仕組みがまだ無い。配信リポジトリは phase-3 で誘導 README + LICENSE だけの状態で作成済み、Android は座標の宣言だけで発行の配線が無い。姉妹ライブラリ KsSettingsView の実装 (add-spm-distribution / add-android-maven-distribution) を「コピー + 固有値の差し替え」で持ち込み、あわせてフェーズ議論で決めた Android 座標のリネーム (cross/ADR-0019 proposed: View 系本体を `ksdialogs-core`、Compose 側を素の `ksdialogs`) とバージョンの単一ソース化を実装して、消費者検証 (phase-8) と release workflow (phase-9) が乗る土台を作る。

## What Changes

- **Android 座標のリネームと module 改名**: `android/ksdialogs` → `android/ksdialogs-core` (`:ksdialogs-core`、Maven `jp.kamusoft:ksdialogs-core`)、`android/ksdialogs-compose` → `android/ksdialogs` (`:ksdialogs`、Maven `jp.kamusoft:ksdialogs`)。`git mv` で履歴追跡。Kotlin パッケージ名 (`jp.kamusoft.ksdialogs` / `.compose`) と AGP namespace は不変。`settings.gradle.kts`・`api-surface-check`・`layout-case-fixtures` の project 参照と説明を追随
- **バージョンの単一ソースと注入**: カタログの `ksdialogs` を `0.1.0-SNAPSHOT` に改め、`android/build.gradle.kts` を新設して「`-Pversion=` の注入値があればそれ、無ければカタログ値」の導出式で subprojects の `group` / `version` を一括設定 (各 module の直書きを削除)
- **発行設定**: `com.vanniktech.maven.publish` 0.37.0 を `:ksdialogs-core` / `:ksdialogs` の 2 module にのみ導入。`publishToMavenCentral()` + `signAllPublications()`、release 単一 variant + sources jar + 空 javadoc jar、SNAPSHOT は Central へ発行しないガード、署名の必須/任意は鍵の有無に連動。POM の共通部 (url / MIT license / developers / scm / inceptionYear) はルートで一括、name / description は module ごと (文言は agenda 決定事項)。依存スコープは現状維持 (`api` に上げるべき `implementation` が無いことは走査済み)
- **monorepo 内消費者の追随**: `samples/android` と `samples/kmp` の dependencySubstitution とアプリ依存 (直接依存は Compose 系 `jp.kamusoft:ksdialogs` の 1 行に減らし、本体は推移で解決)、`kmp/ksdialogs-kmp` と `maui/android/native/ksdialogs-maui-bridge` の本体依存座標 (`jp.kamusoft:ksdialogs` → `ksdialogs-core`)、MAUI binding csproj の aar パス・gradlew task 名
- **SwiftPM 配信**: `scripts/spm-snapshot/` に同期スクリプト + テスト + 誘導 README テンプレートを翻案元から持ち込む (固有値は配信リポジトリ名とコメントのみ)。成果物を手動で commit・push し、検証用 prerelease tag でリポジトリ外の一時消費者から https 解決・ビルドを確認したうえで tag を削除する
- **発行検証**: `publishToMavenLocal` で発行物 (aar / sources jar / 空 javadoc jar / POM / `.module` の依存スコープ) を検算し、release aar の公開シグネチャを javap で走査して外部型の集合が `api` 宣言と一致することを確かめる。証跡は change の evidence/ に残す
- **規範と道具のパス・座標追随**: `kasane/handbook/cross/diagnostic-message-language.md` の検査パス、`test-execution.md` の module 名、`local-development-setup.md` の Sample 参照方式の記述 (座標・project 名・依存 1 行) を改名後に追随する (規約の内容は変えない)。docs-refresh 本体 (`.agents/skills/docs-refresh/SKILL.md`) の識別子表と配布座標の説明も新座標へ更新する (次回の docs-refresh が旧座標を正として扱う経路を残さない。CLAUDE.md の「構成の見直しは承認済み change で行う」に従う)

影響する能力: iOS の配布 (SwiftPM 解決経路)、Android の配布 (Maven 座標と発行経路)、Android ビルド入口 (module 構成と version)、MAUI binding と KMP のビルド入力、Sample のソース参照

## Non-Goals

- Maven Central への実発行・Central Portal トークンと GPG 鍵の secrets 登録・配信リポジトリの deploy key・release workflow への組み込み — phase-9 の責務 (tag は publish 全成功後にのみ生まれる、cross/ADR-0009)。本変更で打つ tag は検証用 prerelease のみで削除する
- `kmp/` の version 導出式の配線 (直書き `"0.1.0"` の置き換えと KMP 発行設定) — phase-7 の責務 (式の形は agenda 決定事項で確定済み。本変更は本体依存の座標追随のみ)
- 配布物を参照する消費者プロジェクト (`verification/`) と dry-run / smoke workflow — phase-8 の責務。samples はソース参照 (composite build / Local Swift Package) を維持する
- README 2 枚と `skills/` の座標表記 (`ksdialogs-compose` → `ksdialogs` / `ksdialogs` → `ksdialogs-core`) — docs-refresh の明示依頼で更新する (CLAUDE.md の運用宣言)。concepts (android/api の 5 枚ほか) の追随は蒸留時の定型作業
- roadmap のゴール文の座標表記 — ksn-roadmap の改訂 (agenda TODO)
- cross/ADR-0019 の accepted 昇格と cross/ADR-0005・android/ADR-0001 への `amended-by` 記入 — 蒸留時 (ksn-distill)
- Kotlin パッケージ名・namespace の変更、2 module の統合、binary (xcframework) 配布 — フェーズ議論で却下済み (agenda 決定事項、cross/ADR-0008 / 0019)

## Impact

- 破壊的変更: 開発用の Maven 座標 (`jp.kamusoft:ksdialogs` の指す先が本体から Compose 側へ) と module パスが変わるが、未リリースのため実利用者はゼロ。monorepo 内の消費者 (samples 2 ルート・KMP・MAUI binding) は本変更内で追随する
- リスク: ① vanniktech 0.37.0 の検証済み範囲は AGP 9.3.0-rc01 までで正式版 9.3.0 は未掲載 — `publishToMavenLocal` の検算を受け入れ条件にする。② カタログの SNAPSHOT 化で version 文字列が変わるが、composite build の置換は座標で行うため影響しない (置換の実効を Sample ビルドで確認する)。③ 改名で検証 CI (`verify-android.yml` / `verify-maui.yml`) の task 名・パスが該当すれば追随が要る (実装時に grep で確定)
- 外部リソース: 配信リポジトリ `KsDialogs-SPM` への初回 push (中身はホワイトリスト 5 点のみ)。新規リポジトリ・アカウント作業は無い

## 級: L

外部連携 (公開配信リポジトリへの実 push と https 解決) と複数能力横断 (Android ビルド・配布・MAUI binding・KMP・Sample) を含み、公開座標という覆すコストの高い決定の実装でもあるため L。設計判断はフェーズ議論と ADR (cross/0008 / 0009 / 0019) で確定済みで、design.md は agenda 決定事項の Decision 形式への転記 + 実装方式の確定 (同期スクリプトの安全契約・検証 tag のライフサイクル・version 導出式・POM の分担) に絞る。

domain: cross
roadmap: package-distribution/phase-5-native-packaging
