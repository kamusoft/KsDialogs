# CI ステップ単体の負ケース確認 (tasks 5.3〜5.8)

`develop` への実 push を伴わずに手元で確認できる範囲。各 workflow の該当ステップのシェル / Python を
**workflow ファイルから書き換えずに抜き出して** 単体実行した。入力は実ログ・実 XML を加工した合成データ、
または一時的なソース変更 (確認後にコピーから復元) で作った。実行日 2026-09-08。

抜き出しは `<scratch>/extract.py` (ヒアドキュメント `<<'PY'` の中身を dedent するだけ) と
`<scratch>/extract-run.py` (`run: |` ブロックを dedent するだけ) で機械的に行った。
`verify-ios.yml` の件数検査と `verify-maui.yml` の iOS 橋渡し件数検査は抜き出し結果が **byte 一致** のため、
iOS 側の結果がそのまま MAUI iOS 橋渡しの結果を兼ねる。

以下、`exit` は抜き出したステップの終了コード。出力は要点のみ。ローカル絶対パスは `<scratch>` に置き換えている。

## 5.3 件数検査の負ケース

### iOS / MAUI iOS 橋渡し (verify-ios.yml `Verify executed test count` = verify-maui.yml `Verify iOS bridge test count`)

入力は `ios/` の実行で得た実ログ (`xcodebuild test -scheme KsDialogs`、Swift Testing 277 件 / XCTest 0 件) を加工したもの。

| ケース | 入力の作り方 | exit | 出力の要点 |
|---|---|---:|---|
| Swift Testing のみ (現行構成) | 実ログそのまま | 0 | Swift Testing: 277 件 / XCTest: 0 件 / 合算 **277 件** |
| XCTest のみ | `Test run with` 行を除き `Executed` を 12 件へ | 0 | Swift Testing: - 件 / XCTest: 12 件 / 合算 **12 件** |
| 混在 | 上記 2 系統を両方載せる | 0 | 合算 **289 件** |
| 両方 0 件 | `Test run with 0 tests` + `Executed 0 tests` | 1 | `::error::… テストが 1 件も実行されていない (2 系統の合算が 0 件)` |
| 件数行の欠落 | 2 系統の件数行を両方除去 | 1 | `::error::… どちらの件数行もログに無い (出力形式が変わった可能性がある)` |
| ログファイル不在 | 存在しないパスを渡す | 1 | `::error::… テストログ … が無いため実行件数を確認できない` |

### MAUI facade (verify-maui.yml `Verify facade test count`)

入力は `dotnet test maui/KsDialogs.Maui.Tests/KsDialogs.Maui.Tests.csproj -c Release` の実 TRX (155 件) を加工したもの。

| ケース | exit | 出力の要点 |
|---|---:|---|
| 実 TRX そのまま | 0 | 合計 **155 件** (成功 155 / 失敗 0) |
| `Counters` を total=0 へ | 1 | `::error::facade のユニットテストが 1 件も実行されていない` |
| TRX が 1 つも無い | 1 | 同上 + `テスト結果 (TRX) が … に無い` |

### MAUI Android 橋渡し (verify-maui.yml `Verify Android bridge test count`)

入力は `maui/android/native/ksdialogs-maui-bridge` の実 XML (31 件) を加工したもの。

| ケース | exit | 出力の要点 |
|---|---:|---|
| 実 XML そのまま | 0 | 合計 **31 件** (失敗 0) |
| 結果 XML なし | 1 | `::error::maui/android/native/…/build/test-results/testDebugUnitTest にテスト結果 XML が無い` |
| `tests="0"` | 1 | `::error::Android 橋渡しのテストが 1 件も実行されていない` |

### Android JVM (verify-android.yml `Verify executed test count`)

入力は `android/settings.gradle.kts` の実物と `android/ksdialogs` の実 XML を合成ツリーへ写して加工したもの。

| ケース | exit | 出力の要点 |
|---|---:|---|
| 実リポジトリそのまま | 0 | 期待 module `:ksdialogs` / 合計 **68 件** |
| 結果 XML 欠落 | 1 | `::error:::ksdialogs: android/ksdialogs/build/test-results/testDebugUnitTest にテスト結果 XML が無い` |
| `tests="0"` | 1 | `::error:::ksdialogs: テストが 1 件も実行されていない` |
| 導出集合が空 (`src/test` を持つ module 無し) | 1 | `include (:api-surface-check, :ksdialogs, :ksdialogs-compose) から src/test を持つ module を 1 つも導出できなかった` |
| include を 1 つも導出できない | 1 | `android/settings.gradle.kts から module を 1 つも導出できなかった` |

### Android instrumented (verify-android-instrumented.yml `Verify executed test count`)

入力は `android/ksdialogs` の実 instrumented 結果 XML (tests 294 / skipped 1) を合成ツリーへ写して加工したもの。

| ケース | exit | 出力の要点 |
|---|---:|---|
| 両 module に結果あり | 0 | `:ksdialogs` 293 / `:ksdialogs-compose` 30 / 実行数合計 **323 件** |
| 片方の結果 XML 欠落 | 1 | `::error:::ksdialogs-compose: …/androidTest-results/connected にテスト結果 XML が無い` |
| 全件 skip (tests = skipped) | 1 | `::error:::ksdialogs: instrumented テストの実行数 (tests - skipped) が 0 (tests 294 / skipped 294)` |
| 導出集合が空 (`src/androidTest` を持つ module 無し) | 1 | `include (…) から src/androidTest を持つ module を 1 つも導出できなかった` |

### KMP (verify-kmp.yml `Verify executed test count`)

入力は `kmp/ksdialogs-kmp` の実 XML を合成ツリーへ写して加工したもの。

| ケース | exit | 出力の要点 |
|---|---:|---|
| 実リポジトリそのまま | 0 | testAndroidHostTest 76 / iosSimulatorArm64Test 75 / 合計 **151 件** |
| 片方の結果 XML 欠落 | 1 | `::error::iosSimulatorArm64Test: kmp/ksdialogs-kmp/build/test-results/iosSimulatorArm64Test にテスト結果 XML が無い` |
| 片方が `tests="0"` | 1 | `::error::iosSimulatorArm64Test: テストが 1 件も実行されていない` |
| 両ターゲットとも XML 欠落 | 1 | 2 件の `::error::` を両方出力 |

「導出集合が空」は kmp には無い — 検査対象は `TARGET_TASKS` の固定列挙で、settings からの導出を行わないため。

## 5.4 KMP metadata compile の失敗検出

`kmp/ksdialogs-kmp/src/iosMain/.../IosLoadingGateway.kt` の `override suspend fun hide()` に
`@Throws(IllegalStateException::class)` を一時的に付けた状態で確認した (確認後にコピーから復元)。

| 実行 | exit | 出力の要点 |
|---|---:|---|
| `./gradlew --no-daemon --console=plain allTests compileCommonMainKotlinMetadata compileIosMainKotlinMetadata` (job のコマンドそのまま) | 1 | `Task :ksdialogs-kmp:compileKotlinIosSimulatorArm64 FAILED` / `Member overrides different '@Throws' filter from 'interface LoadingGateway : Any'.` |
| `./gradlew compileIosMainKotlinMetadata` 単体 | 1 | `Task :ksdialogs-kmp:compileIosMainKotlinMetadata FAILED` / 同じ診断 |
| `./gradlew compileCommonMainKotlinMetadata` 単体 | 1 | 依存する `compileIosMainKotlinMetadata` が FAILED |

job のコマンドは失敗する。ただし現行の Kotlin ではターゲット本体 (`compileKotlinIosSimulatorArm64`) が
先に同じ違反を検出して停止するため、この投入では「ターゲット本体は通り metadata compile だけが失敗する」
状態にはならない。metadata compile が単体で同じ違反を検出することは 2 行目の実行で別途確認した。

## 5.5 lint の負ケース

ベースライン (違反投入なし) は 4 本とも exit 0。以下は 1 件ずつ投入して確認し、投入したファイルは
確認後に `trash` で削除した (足場・追跡ファイルは書き換えていない)。

| 検査 | 投入した違反 | exit | 出力の要点 |
|---|---|---:|---|
| local-path-lint | ルート直下の一時 `.md` にローカル絶対パス 1 件 | 1 | `ローカル絶対パスが含まれています` + 該当行 |
| identity-lint | `maui/macios/native/` の一時 `.txt` に `DEVELOPMENT_TEAM = <team-id>;` | 1 | `個体・個人・秘密を特定する値が含まれています` + `team-id: <team-id>` |
| comment-policy-lint | 一時 `.kt` のコメントに `kasane/` 配下パス参照 | 1 | `[禁止] 作業文書 (kasane/ 配下) のパス参照` (検査対象 966 ファイル) |
| scenario-id-coverage | 一時 change の spec に仕様だけの ID `ZZ-QQ-99` | 1 | `未網羅 (仕様にあるがテストに見つからない ID): ZZ-QQ-99` / `結果: 未網羅 1 件` |

`identity-lint` の最初の投入は拡張子 `.log` にしたため `.gitignore` の `*.log` で除外され検出されなかった。
これは fixture の作り方の誤りで、追跡対象になる拡張子へ変えたところ検出された (検査ロジック側の問題ではない)。

### secret scan (ci.yml `Secret scan (gitleaks)`)

ステップを丸ごと抜き出し、scratch に作った検証用の小さな git リポジトリ (本リポジトリの `.gitleaks.toml` を複製) で実行した。
手元の gitleaks は workflow の固定版と同じ 8.30.1。

| ケース | 作り方 | exit | 出力の要点 |
|---|---|---:|---|
| 秘密なし | 通常のファイルのみ | 0 | `走査対象: 3 ファイル (追跡: 3 ファイル)` / `no leaks found` |
| 検証用ダミーの秘密 | ランダム生成した形式一致の文字列を commit | 1 | `RuleID: github-pat` / `Secret: REDACTED` / `leaks found: 1` |
| 展開数 < 追跡数 | `.gitattributes` の `export-ignore` で `git archive` の出力を減らす | 1 | `::error::走査対象の展開に失敗している (追跡 4 に対し展開 2)` (gitleaks へ進む前に停止) |

同時に置いた AWS アクセスキー形式のダミーは gitleaks 既定ルールでは検出されなかった (上流ルールセットの
挙動であり、本 workflow の検査ロジックとは別)。

## 5.6 main 宛て PR の head 制限 (ci.yml `Pull request head restriction`)

ステップを抜き出し、`github.head_ref` / head リポジトリ相当の環境変数を差し替えて単体実行した。

| ケース | exit | 出力の要点 |
|---|---:|---|
| 自リポジトリの `develop` | 0 | `head は <owner>/<repo> の develop` |
| 自リポジトリの `develop` 以外 (`feature/x`) | 1 | `::error::main を base とする pull request の head は develop でなければならない (head: feature/x)` |
| fork の `develop` | 1 | `::error::main を base とする pull request の head は <owner>/<repo> のブランチでなければならない (head: <fork>/<repo>)` |
| fork の `main` | 1 | 同上 (リポジトリ検査が先に落ちる) |

ステップの `if:` 条件 (`github.event_name == 'pull_request' && github.base_ref == 'main'`) は
GitHub 側の評価なので手元では確認できない。実 PR での確認は phase-9 へ申し送り済み。

## 5.7 Xcode 選択 (verify-ios.yml / verify-kmp.yml / verify-maui.yml `Select Xcode`)

| ケース | exit | 出力の要点 |
|---|---:|---|
| ステップをこの Mac 上でそのまま実行 (`KS_XCODE_VERSION=99.9`) | 1 | `::error::Xcode 99.9 がランナーイメージに存在しない` + 一覧の `ls` (この Mac には `/Applications/Xcode_*.app` が無いため一覧は空) |
| ランナーイメージ相当の擬似ディレクトリ (検索ルートのみ差し替えた複製) で `99.9` | 1 | 同じ `::error::` + `Xcode_16.4.0.app` `Xcode_26.4.0.app` `Xcode_26.5.0.app` `Xcode_26.5.1.app` の一覧 |
| 同上で `26.5` | 0 | `DEVELOPER_DIR=…/Xcode_26.5.1.app/Contents/Developer` (パッチ最新が選ばれる) |

この Mac の Xcode は `Xcode-26.5.0.app` (ハイフン区切り) で、ステップが前提とする GitHub ランナーイメージの
`Xcode_26.5.app` (アンダースコア区切り) と命名が違う。ステップは CI でのみ走るので実害はないが、
「一覧が出力される」ことの確認には検索ルートを差し替えた複製が要った。

## 5.8 Android 依存グラフ検査 (verify-android.yml `Run unit tests`)

`android/ksdialogs/build.gradle.kts` の `dependencies` に `implementation("androidx.compose.ui:ui:1.7.0")` を
一時的に足した状態で `./gradlew --no-daemon --console=plain test` を実行した (確認後にコピーから復元し、
SHA-256 の一致を確認済み)。

- exit 1 / `Task :ksdialogs:verifyNoDeclarativeUiDependency FAILED`
- `debugCompileClasspath に Compose 系 artifact が混ざっています: androidx.compose.runtime:runtime, …, androidx.compose.ui:ui, …`

依存グラフ検査が `test` の実行に乗っていること (`check` を経由しなくても走ること) も同時に確認できた。

## 後始末

一時的なソース変更 (5.4 / 5.8) はいずれも変更前のコピーから復元し、`shasum -a 256 -c` で一致を確認した。
一時ファイル (5.5 の違反投入・5.4 の probe) は `trash` で削除した。作業後の `git status` は作業開始時と同一。
