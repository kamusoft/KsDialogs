# レビュー結果: add-native-distribution (001 回目)

**日付**: 2026-09-08
**判定**: APPROVED

## サマリー

座標リネーム・version の単一ソース化・vanniktech による発行設定・SwiftPM 同期スクリプトのいずれも、デルタスペックの Requirement / Scenario をレビュー側の再実行で満たすことを確認した。POM と `.module` の依存スコープは spec の列挙と一字一句一致し、注入値の形式検査 (空文字を含む) ・SNAPSHOT ガード・署名の鍵有無連動・`:api-surface-check` の非公開はいずれも実物で再現した。tasks.md に虚偽のチェックは見つからず、足場アーティファクトの書き換えもない。

指摘は Critical / Major なし。未実施の task 7 (配信リポジトリへの手動 push と https 解決) で使う `verify-https-resolution.sh` の事前検証に 1 か所の穴 (Minor) があり、task 7 に着手する前に塞ぐことを勧める。残りは記録の粒度に関する Minor 2 件と Suggestion 4 件。

## 実行した検証 (レビュー側で再実行)

| 対象 | 結果 |
|---|---|
| `android/` `./gradlew test --rerun-tasks` | BUILD SUCCESSFUL / `:ksdialogs-core` 68 tests・0 failures (handbook の基準値と一致) |
| `:ksdialogs-core:verifyNoDeclarativeUiDependency` の結線 | `test` のタスクグラフに乗ることを `--dry-run` で確認 |
| `:api-surface-check` 肯定ケース | `assembleDebug` 成功 |
| `:api-surface-check` 否定ケース (抜き取り 2/15) | `toastComposeFromCore` / `legacyContractName` とも期待どおりコンパイルエラーで失敗 |
| `-Pversion` の形式検査 | 空文字 / `v1.0.0` / `1.0.0-pre` / ` 1.0.0 ` はすべて設定段階で失敗、`0.1.0-beta.1` は通り `version` に反映 |
| `publishToMavenLocal` (鍵なし) | 成功。両 artifact に aar / sources jar / 空 javadoc jar / POM / `.module` が揃う。POM の name・description・url・MIT license・developers・scm・inceptionYear を実物で確認 |
| `.module` の variant 別依存 | core: api = `androidx.annotation:annotation` + kotlin-stdlib / runtime = +`kotlinx-coroutines-android`。compose 系: api = `jp.kamusoft:ksdialogs-core`(同版) + `androidx.compose.runtime:runtime` + kotlin-stdlib / runtime = +`compose.ui` / `lifecycle-runtime` / `savedstate`。テスト専用ライブラリはいずれの variant にも無し (spec の列挙と一致) |
| `publishToMavenLocal` (`ORG_GRADLE_PROJECT_signingInMemoryKey` あり) | Sign タスクが必須化されて鍵の不正で失敗 = 鍵の有無連動が環境変数経路でも効くことを確認 |
| `:api-surface-check:tasks --group publishing` | `No tasks` |
| `scripts/spm-snapshot/sync-snapshot-test.sh` | 41 判定すべて ok / 失敗 0 |
| `scripts/local-path-lint.py` / `identity-lint.py` / `comment-policy-lint.py` | いずれも違反 0 (comment-policy は 966 ファイル検査 / 禁止 0) |
| `scripts/scenario-id-coverage.py` (lint job の同伴検査) | 未網羅なし |
| `diagnostic-message-language.md` の改訂後 grep | 出力なし (期待どおり) |
| 配信リポジトリの README と `README.template.md` | 差分なし |
| `ios/Package.swift` | 無変更 |
| `kmp/` `./gradlew allTests --rerun-tasks` | BUILD SUCCESSFUL / 151 tests・0 failures (iosSimulatorArm64 75 + androidHostTest 76) |
| `verify-https-resolution.sh` が生成する消費者パッケージの骨格 | 配信リポジトリと同形のローカルコピーに差し替えて `xcodebuild build -destination 'generic/platform=iOS Simulator'` を実行 → BUILD SUCCEEDED (scheme 名・`.product(name: "KsDialogs", package: "KsDialogs-SPM")`・`DialogOptions` / `DialogAlignment` の参照・tools-version 5.9 から 6.3 パッケージへの依存がすべて成立) |

instrumented test・`ios/` の `xcodebuild test`・MAUI binding のビルド・samples 2 ルートの `assembleDebug` は evidence/ と実施済み結果で代替した。task 7.1〜7.3 (実リモートの push・検証 tag・後始末) は未実施として扱い、対応 Scenario の未検証は指摘対象にしていない。

## 照合した規約

| 文書 | 適用のきっかけ |
|---|---|
| `kasane/handbook/cross/comment-policy.md` | always (ビルドスクリプト・シェル・Kotlin のコメントを新規に書いている) |
| `kasane/handbook/cross/test-execution.md` | テストを実行し完了を判定する / 本文書自体が diff の対象 |
| `kasane/handbook/cross/verification-ci.md` | `.github/workflows/ci.yml` を変更している / 本文書自体が diff の対象 |
| `kasane/handbook/cross/local-development-setup.md` | Gradle ルートのビルドと Sample の参照方式 / 本文書自体が diff の対象 |
| `kasane/handbook/cross/diagnostic-message-language.md` | 検査対象パスの追随 (本文書自体が diff の対象) |
| `kasane/lessons/process.md` | L-001 / L-002 |

`sample-parity.md` は `samples/` に触れるが、変更は依存宣言と置換設定だけでデモ項目・文言・色トークン・OS 操作への反応・撮影引数のいずれにも当たらないため適用外と判定した。

参照した決定: cross/ADR-0004 / 0005 / 0006 / 0015 / 0016 / 0017 / 0018、android/ADR-0001 (いずれも accepted)。cross/ADR-0008 / 0009 / 0019 は `proposed` のため、これらとの整合は判定根拠にしていない (所見も無し — 実装は 3 本の記述と矛盾しない)。

適用したスキル: kotlin-impl-skill (Gradle Kotlin DSL)、github-workflow-skill (`ci.yml` の差分)、maui-native-binding-skill / csharp-impl-skill (binding csproj)、swift-ui-impl-skill (消費者コードの参照のみ)。Kotlin 言語層の指摘は無し — 差分にアプリケーション Kotlin は含まれず、DSL 側は `val` 中心・`!!` 不使用・`tasks.configureEach` / `plugins.withId` の遅延 API 使用でイディオムに沿っている。

## 指摘事項

### [🟡 Minor] 検証用 tag を打つ前に「スナップショットが remote に届いているか」を確かめていない

**該当箇所**: `scripts/spm-snapshot/verify-https-resolution.sh:102`

**問題点**: 事前検証は作業コピーに commit が 1 つ以上あること (`git rev-parse --verify HEAD`) までしか見ておらず、その commit が origin の既定ブランチに到達しているかを確かめない。`git push origin refs/tags/<tag>` は tag が指すオブジェクトを一緒に送るため、**手動の `git push origin main` (task 7.1) が抜けていても https 解決と消費者ビルドは成功する**。その後の後始末で tag を消すと、配信リポジトリは phase-3 の状態 (README + LICENSE のみ) のまま残り、ログには「結果: 成功」だけが残る。spec の Scenario「実リモートからの依存解決とビルド」の GIVEN (スナップショットが push された配信リポジトリ) を、手順自身が確かめていない状態になる。task 7.1〜7.3 が未実施なのでこの経路はまだ一度も通っていない。

**推奨修正**: tag 作成の前に `git -C "${DESTINATION}" fetch origin` を行い、HEAD が remote の既定ブランチに含まれること (`git branch -r --contains HEAD` に `origin/<既定ブランチ>` が現れる、または `git rev-parse HEAD` と `git rev-parse origin/<既定ブランチ>` の一致) を事前検証に足す。満たさなければ tag を作らずに「スナップショットを push してから実行する」旨で終了する。

### [🟡 Minor] instrumented test の Scenario GIVEN の置き換えが成果物に残っていない

**該当箇所**: `tasks.md:178` / `deviation.md`

**問題点**: Scenario「instrumented test の無改変実行」の GIVEN は「API レベル 36 のエミュレータ (検証 CI の instrumented job と同じ条件)」だが、実際は API 36 の実機で実行されている (環境にエミュレータイメージが無いため)。件数 294 / 39 は一致しているが、**実行環境を置き換えたことが deviation.md にも evidence/ にも残っていない**ため、成果物だけを読む後続 (verify・蒸留・phase-8) からは「CI と同じ条件で通った」と読める。lessons L-002 の趣旨 (主張の範囲を実証した範囲に限り、実証手段を併記する) に照らして記録が要る。この change で最大のテスト面 (333 件) に evidence が 1 つも無い点も同じ理由で気になる。

**推奨修正**: deviation.md に付随ではなく実行条件の乖離として 1 行残す (例: 「Scenario の GIVEN をエミュレータから API 36 実機へ置き換えた。理由: 環境に API 36 エミュレータイメージ不在。件数 294 / 39 は改名前と一致」)。可能なら件数 XML の抜粋を evidence/ に足す。

### [🟡 Minor] 改訂した handbook の `timestamp` (最終検証日) が更新されていない

**該当箇所**: `kasane/handbook/cross/diagnostic-message-language.md:9`

**問題点**: 検査コマンドの走査パスを `android/ksdialogs android/ksdialogs-compose` から `android/ksdialogs-core android/ksdialogs` へ書き換えたのに、frontmatter は `timestamp: 2026-09-07` のまま。handbook の `timestamp` は最終検証日で「確認した日に更新する」(ksn-core references/handbook.md)。書き換えた検査が改名後の木で実際に無出力になることを確かめた日が文書から辿れない (本レビューで実行し無出力を確認済み)。他の 3 本 (`local-development-setup.md` / `test-execution.md` / `verification-ci.md`) は元から 2026-09-08 なので結果的に問題は出ていない。

**推奨修正**: `diagnostic-message-language.md` の `timestamp` を実確認日 (2026-09-08) に更新する。

### [🔵 Suggestion] tag 名の存在確認が固定文字列比較になっていない

**該当箇所**: `scripts/spm-snapshot/verify-https-resolution.sh:118`

**問題点**: `grep -qx "refs/tags/${TAG}"` は `-F` が無いため tag に含まれる `.` が任意 1 文字として効く。`0.1.0-alpha.1` を検査するときに `0X1Y0-alpha.1` のような別 tag が「同名あり」と判定され、後始末の安全確認 (他人の tag を消さないための中止) が誤って発火する。実害の確率は低いが、この分岐は「他人の tag を消さない」ための最後の砦なので厳密にしたい。

**推奨修正**: `grep -qxF "refs/tags/${TAG}"` にする。

### [🔵 Suggestion] ログ出力が process substitution 越しで、末尾 (後始末の記録) を取りこぼす余地がある

**該当箇所**: `scripts/spm-snapshot/verify-https-resolution.sh:68`

**問題点**: `exec > >(tee -a "${LOG_PATH}") 2>&1` は非同期の `tee` にパイプするため、EXIT trap の `cleanup` が最後に `exit` した時点で `tee` が書き切っている保証がない。task 7.3 は「後始末の記録」を evidence/ に保存することを求めており、欠けるとその記録がログに残らない。

**推奨修正**: `cleanup` の末尾で標準出力/標準エラーを閉じてから `tee` の完了を待つ (`exec 1>&- 2>&-; wait`)、または `tee` を使わず各出力を `>> "${LOG_PATH}"` へ明示的に追記する形にする。

### [🔵 Suggestion] コメントの「鍵が漏れて」が二義的

**該当箇所**: `android/build.gradle.kts:105`

**問題点**: 「本番発行で鍵が漏れていても Sonatype Central Portal が未署名を拒否するため静かには通らない」の「漏れて」は文脈上「指定が抜けて」の意だが、署名鍵の話題であるため「鍵が漏洩しても」とも読める。comment-policy は「そのファイルだけを読んでいる人にとって意味が通ること」を最低条件としており、ここは初見で意味が反転しうる。

**推奨修正**: 「本番発行で鍵の指定が抜けていても」等、欠落であることが一意に読める語にする。

### [🔵 Suggestion] 旧座標が残る利用者向け成果物を、リリース前に必ず docs-refresh へ通す

**該当箇所**: `skills/en/ksdialogs-android/**` / `skills/ja/ksdialogs-android/**` / `README.md` / `README_ja.md`

**問題点**: `ksdialogs-compose` の表記が 12 箇所残っている。proposal の Non-Goals どおり docs-refresh の責務であり本 change の違反ではないが、これらは公開される利用者向け成果物で、内容は「存在しない Maven 座標の案内」になっている。未発行のうちは実害ゼロだが、phase-9 のリリースがこの追随より先に走ると誤った座標が公開される。

**推奨修正**: 指摘ではなく引き継ぎ事項として、phase-9 の前提条件 (またはロードマップの TODO) に「docs-refresh で skills / README 2 枚の座標を追随させる」を明示的に積む。

## アクションプラン

1. **[Minor] `verify-https-resolution.sh` に remote 到達の事前検証を足す** — task 7.1〜7.3 に着手する前に。あわせて `grep -qxF` とログの取りこぼし対策 (Suggestion 2 件) も同じ編集でまとめると安い
2. **[Minor] deviation.md に instrumented の実行条件の置き換えを記録する** — verify / 蒸留の前に
3. **[Minor] `diagnostic-message-language.md` の `timestamp` を 2026-09-08 に更新する**
4. **[Suggestion] `android/build.gradle.kts:105` のコメント文言を直す**
5. **[Suggestion] skills / README の座標追随 (docs-refresh) を phase-9 の前提として積む** — 本 change の外
