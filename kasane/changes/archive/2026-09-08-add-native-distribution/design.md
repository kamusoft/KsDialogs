# Design: add-native-distribution

## Context

Native iOS / Android の配布方針は cross/ADR-0008 (SwiftPM は配信リポジトリ `KsDialogs-SPM` へのスナップショット、Android は Maven Central) と cross/ADR-0009 (lockstep 単一 version) で確定し、Android の座標名は cross/ADR-0019 (proposed: 本体 `ksdialogs-core`、Compose 側 `ksdialogs`) で改訂中。配信リポジトリは phase-3 で作成済み (誘導 README + LICENSE のみ)。翻案元 KsSettingsView の実装 (add-spm-distribution / add-android-maven-distribution) が初回リリースまで実証済みで、本 change はその「コピー + 固有値の差し替え」に KsDialogs 固有の 2 点 (2 artifact のリネーム・KMP を含む version 導出式) を足す。

## Goals / Non-Goals

- Goals: 発行設定と同期スクリプトを配線し、`publishToMavenLocal` の検算と配信リポジトリの https 解決確認まで通す。座標リネームと version の単一ソース化を同時に済ませる
- Non-Goals: proposal.md の Non-Goals に同じ (Central 実発行・deploy key・release workflow は phase-9、KMP の version 配線は phase-7、消費者検証は phase-8、文書追随は docs-refresh / 蒸留)

## Decisions

### Decision 1: 同期スクリプトの安全契約 (翻案元 Decision 1 の踏襲)

**採用案:** `scripts/spm-snapshot/sync-snapshot.sh` は破壊的操作 (`.git/` 以外の除去) の**前に**次を全件検証し、1 つでも失敗したら同期先を一切変更せず非ゼロ終了する:

1. コピー元 5 点 (`ios/Package.swift` / `ios/Sources/` / `ios/Tests/` / ルート `LICENSE` / `README.template.md`) がすべて存在する
2. 同期先は canonical path 化した上で git top-level ディレクトリである
3. 同期先の `origin` remote URL が配信リポジトリ `kamusoft/KsDialogs-SPM` を完全一致で指す
4. 同期先が monorepo 自身・その祖先ディレクトリでない

git metadata (HEAD / index / refs / remote 設定) には触れず、ネットワーク操作も行わない。commit / tag / push は呼び出し側 (本 change は手動、phase-9 は release workflow)。`.git/` 以外の除去は翻案元と同じ `rm -rf` で行う — 全体ルール「削除は `trash`」は人の操作環境向けで、CI ランナーで動くスクリプトは対象外 (オーナー判断 2026-09-08。`trash` は CI に無く phase-9 の経路が成立しない)。発火は 4 段の事前検証を通過した配信リポジトリの作業コピーに限る。

**理由:** 引数の誤指定で任意の作業ツリーを消去し得る構造を実行時検証で塞ぐ。翻案元でテスト済みの契約をそのまま持ち込み、KsDialogs 向けに変えるのは配信リポジトリ名とコメントだけ (agenda 決定事項)。

**代替案:**
- **A: 検証なしの単純同期 (rsync --delete 相当)** — 誤指定 1 回で回復困難なデータ消失。却下
- **B: 対話確認プロンプト** — phase-9 で CI から呼ぶ経路が成立しない。却下

### Decision 2: 検証用 tag のライフサイクル (翻案元 Decision 2 の踏襲)

**採用案:** https 解決確認は prerelease tag (`X.Y.Z-alpha.N` 形式) で行い、手順の終了時に成否を問わず tag を local と remote の両方から削除する (手順は tag 作成後の後始末を `trap` 相当で必ず走らせる形にし、削除対象は手順が作成した tag だけに限る)。証跡 (解決ログ・実行記録) は change の evidence/ に保存する。

**理由:** public リポジトリに tag が残ると iOS だけが外部から解決可能な公開版になり、lockstep (cross/ADR-0009) と「tag は publish 全成功後にのみ生まれる」原則と緊張関係になる。

**代替案:**
- **A: tag を残す** — 原則の例外が恒久化する。却下
- **B: 検証専用リポジトリで確認する** — リポジトリが増え、本番経路を検証した価値が下がる。却下
- **C: phase-9 の全形態 prerelease で検証する** — https 解決確認が最後まで遅延し、フェーズ分担の決定と矛盾する。却下

### Decision 3: version の導出式は「注入値があればそれ、無ければカタログの SNAPSHOT」

**採用案:** カタログ (`android/gradle/libs.versions.toml` の `ksdialogs`) を `0.1.0-SNAPSHOT` に改め、`android/build.gradle.kts` を新設して `providers.gradleProperty("version").orNull ?: libs.versions.ksdialogs.get()` で subprojects の `group = "jp.kamusoft"` / `version` を一括設定する (各 module の直書きは削除)。注入値は `X.Y.Z` または `X.Y.Z-{alpha|beta|rc}.N` (roadmap 前提の prerelease 形式) に限り、空文字・空白・形式外はビルド設定で失敗させる (空文字が「注入あり」と解釈されて SNAPSHOT ガードを迂回するのを防ぐ。phase-9 の dispatch 入力検査とは独立に、直接 Gradle を実行した場合も守る)。SNAPSHOT のあいだは名前に `MavenCentral` を含むタスク (`dropMavenCentralDeployment` を除く) を失敗させる。`kmp/` も同じ式で自分の version と本体依存版を導出する (配線は phase-7)。

**理由:** リリース CI の注入点が「両ビルドに `-Pversion=` を渡す」の 1 種類で済み、dispatch 入力 = 注入値 = tag = 各レジストリの version が同一文字列で流れる。KMP → 本体の厳密同版 (cross/ADR-0009) が同じ式から導出されて機械的に揃う。samples の composite build は座標で置換するため version の値に左右されない。

**代替案:**
- **A: カタログの値を CI が書き換える** — 作業木を汚し、tag との一致が書き換え工程の正しさに依存する。却下
- **B: version 専用ファイルの新設** — カタログが既に同じ役で冗長。却下
- **C: 各 module に直書き (現状形)** — group のような全体事項を module ごとに繰り返し、kmp の直書きとずれる余地が残る。却下

### Decision 4: POM の共通部はルートで一括、name / description は module ごと

**採用案:** `com.vanniktech.maven.publish` 0.37.0 を `:ksdialogs-core` / `:ksdialogs` にだけ適用する (`api-surface-check` は発行しない)。ルート `android/build.gradle.kts` の subprojects で `plugins.withId("com.vanniktech.maven.publish")` により共通設定 (release 単一 variant + sources jar + 空 javadoc jar / `publishToMavenCentral()` / `signAllPublications()` / 署名の必須は `signingInMemoryKey` の有無に連動 / POM の url・MIT license・developers・scm・inceptionYear 2026 / SNAPSHOT ガード) を 1 回だけ書き、各 module は POM の name / description だけを書く (文言は agenda 決定事項の表)。署名の配線は、検証用に一時生成した鍵をプロパティで渡したローカル発行で `.asc` の生成を確かめる (鍵なしで通ることだけでは `signAllPublications()` の欠落を検出できない)。

**理由:** 共通部を 2 箇所に複製すると URL 変更時に片方だけ取り残される (cross/ADR-0018 で版の直書き 3 箇所が実際に取り残された前例)。

**代替案:**
- **A: 各 module に POM 全文を複製** — 変更時に 2 箇所を手で揃える。却下

### Decision 5: 座標リネームは `git mv` でディレクトリ / project 名まで、Kotlin パッケージ名と namespace は不変 (cross/ADR-0019)

**採用案:** `android/ksdialogs` → `android/ksdialogs-core` (`:ksdialogs-core`)、`android/ksdialogs-compose` → `android/ksdialogs` (`:ksdialogs`) を `git mv` で改名し、`settings.gradle.kts` / `api-surface-check` / `layout-case-fixtures` / 消費者 (samples 2 ルート・kmp・maui bridge・MAUI binding csproj) の参照を追随する。Kotlin パッケージ (`jp.kamusoft.ksdialogs` / `.compose`) と AGP namespace は動かさない。本体の Compose 非依存を固定する検査タスク (`verifyNoDeclarativeUiDependency`) は `:ksdialogs-core` に付いたまま。

**理由:** ディレクトリと座標がねじれたまま残ると読み手を惑わせ続ける。パッケージ名は artifact 名と一致させる慣習がなく、動かすと利用者の import が変わる割に得がない (agenda 決定事項)。

**代替案:**
- **A: 座標だけ差し替えてディレクトリはそのまま** — `ksdialogs` ディレクトリが `ksdialogs-core` を発行する形が残る。却下
- **B: パッケージ名も座標に揃える** — 利用者の import とソース・テスト・文書の全体が書き換わる。却下

### Decision 6: 依存スコープは現状維持し、発行時に release aar を javap で検算する

**採用案:** 公開宣言の全走査 (agenda 決定事項) で、公開シグネチャに現れる外部の非プラットフォーム型は `androidx.annotation.ColorInt` と `androidx.compose.runtime.Composable` の 2 つだけで既に `api`。依存スコープは変えない。発行時に release aar の公開シグネチャを javap で走査し、外部型の集合が `api` 宣言と一致すること (`implementation` の型が露出していないこと) を検算して evidence/ に残す。発行メタデータは compile / runtime の両スコープを検査する — compile は公開 ABI 由来の依存だけ (Kotlin 標準ライブラリは対象外)、runtime は実装が要する依存 (core: coroutines-android、Compose 系: compose-ui / lifecycle-runtime / savedstate) を含むこと、テスト専用ライブラリが無いこと。sources jar は公開パッケージの `.kt` を含むことまで見る (存在確認だけでは空 jar を通す)。

**理由:** 翻案元の列挙漏れ 2 件目は発行後 aar の走査で見つかった。同じ経路で検算すれば静的読解の見落としを発行物で堰き止められる。

**代替案:**
- **A: 静的読解の結果だけで確定** — 発行物での確認がない。却下
- **B: 検算を Gradle 検査タスクとして常設** — 外部型が 2 つの現状では投資に見合わない (explicit API mode と `api-surface-check` が逆向きの漏れを既に押さえている)。却下

### Decision 7: Sample の composite build は明示 dependencySubstitution を維持し、新座標へ追随する (翻案元 Decision 4 の踏襲)

**採用案:** `samples/android` / `samples/kmp` の `substitute(module(...)).using(project(...))` を新座標 (`jp.kamusoft:ksdialogs-core` → `:ksdialogs-core`、`jp.kamusoft:ksdialogs` → `:ksdialogs`) に更新して維持する。アプリ側の直接依存は Compose 系 `jp.kamusoft:ksdialogs` の 1 行に減らし (現状は本体と Compose 系の 2 行)、本体は推移的依存で解決させる — 利用者が書く依存 1 行 (cross/ADR-0008 / 0019) を Sample が体現する形。置換の座標は 2 本とも明示してよい (置換対象の列挙であって直接依存の数ではない)。samples は配布物参照に切り替えない。

**理由:** 初回リリース後は Central に実 artifact が存在するため、自動置換の不発時に公開版へ静かにフォールバックする。明示置換は壊れたら必ずビルドエラーになる安全装置。消費者としての検証は phase-8 の `verification/` が担う。

**代替案:**
- **A: 自動置換に任せて明示置換を削除** — 無音フォールバック経路が生まれる。却下
- **B: samples を配布物参照に恒久切替** — 日常開発で本体変更を映すのに発行が要る。却下

### Decision 8: iOS テストと https 消費者検証の実行経路 (翻案元 Decision 4 の踏襲)

**採用案:** iOS の受け入れテストは iOS Simulator destination の `xcodebuild test` (検証 CI `verify-ios.yml` と同じ経路) で実行件数 1 件以上を確認する。https 消費者検証も iOS Simulator 向けにビルドし、product `KsDialogs` の公開型を最低 1 つ参照するコードで配線を確認する。

**理由:** `swift test` は macOS ホスト実行で UIKit ガード内のテストが除外され空振りする (handbook cross/test-execution.md)。import 文だけの消費者コードでは product の配線を検証できない。

**代替案:**
- **A: `swift build` / `swift test` (macOS ホスト)** — UI 系テストが 0 件で成功し得る。却下

## Risks / Trade-offs

- vanniktech 0.37.0 の検証済み範囲は AGP 9.3.0-rc01 まで。正式版 9.3.0 での動作は `publishToMavenLocal` の検算で確認する (失敗したら AGP の rc との差分を切り分ける)
- `-Pversion=` はすべての project の `version` に効く Gradle の予約プロパティ。`api-surface-check` にも同じ値が入るが発行しないため無害
- 改名で検証 CI の task 名・パス (`verify-android.yml` / `verify-maui.yml`) が該当すれば追随が要る。実装時に grep で確定し、該当分は本 change に含める (パス文字列の追随)
- 手動の初回 push と検証用 tag は人の操作を含む。手順と証跡を evidence/ に残し、phase-9 で同じスクリプトを CI から呼ぶ

## Migration Plan

1. 座標リネーム (git mv + 参照追随) → `./gradlew test` と api-surface-check のビルドで退行なしを確認
2. version 導出式と発行設定 → `publishToMavenLocal` と javap の検算
3. 消費者 (samples・kmp・MAUI binding) の追随とビルド確認
4. 同期スクリプト + テスト → 手動 push → 検証 tag → https 解決 → tag 削除
5. handbook のパス追随

## Open Questions

なし (同期スクリプトの削除方式と同期テストの継続実行経路は 2026-09-08 のオーナー裁定で確定: Decision 1、および同期テストは検証 CI の lint job に step として足し、cross/ADR-0017 の「5 検査」は蒸留時に amends する)

## ADR 候補

- Decision 3 (version の導出式と注入): 全形態のリリース手順を制約する (cross/ADR-0009 の lockstep を機械的に実現する手段)。蒸留時に cross/ADR-0009 へ溶かすか新規 ADR にするかを決める (agenda 決定事項)
- Decision 5 (座標リネーム): cross/ADR-0019 として起票済み (proposed)。蒸留時に accepted へ昇格し、cross/ADR-0005 と android/ADR-0001 に `amended-by` を記入する
- 同期テストの lint job への追加: cross/ADR-0017 (accepted) の「lint job は 5 検査」を一部改訂する。蒸留時に amends のドラフトを起票する (検査の追加。他の決定は維持)
- Decision 1 / 2 / 4 / 6 / 7 / 8: なし (翻案元の踏襲、または局所的で可逆)
