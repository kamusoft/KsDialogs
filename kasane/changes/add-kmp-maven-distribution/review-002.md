# レビュー結果: add-kmp-maven-distribution (002 回目)

**日付**: 2026-09-09
**判定**: CHANGES_REQUESTED

## サマリー

前回 (001) の Minor 3 件と相方レビューの Major / Minor はいずれも修正が入っており、修正で新たに入った差分 (2 段ガード・android 側の同型化・集合一致の `@Throws` 検査・置換コメント 2 本・証跡 2 本の書き直し) を新規の目で見ても、Critical / Major は無い。ガードは自分の手で再実行して裏付けた — 認証情報のプロパティを一切渡さず `--offline` で、名前を直接指定した Central 向け 5 経路がすべて設定段階で SNAPSHOT 診断のみにより失敗し、`dropMavenCentralDeployment` は素通り、集約 `publish` だけが deviation の記述どおり SNAPSHOT 診断と認証情報未解決を同時に報告する。version 導出・Swift 参照の 3 分岐 (SNAPSHOT / 既定 URL / URL 上書き)・不正注入値の拒否・`:api-surface-check` の非公開・kmp 153 件 (androidHostTest 78 + iosSimulatorArm64 75) / android 68 件の全件成功も再現できた。

残るのは Minor 1 件で、これは前回 Minor 3 (実証範囲を超えた無影響の主張) の取りこぼし — 主張を実証範囲へ絞る修正が `deviation.md` と `evidence/swiftpm-reference-derivation.txt` には入ったが、同じ主張を持つ 3 つ目の箇所 (`evidence/publish-to-maven-local.txt`) が旧主張のまま残り、書き直した側と食い違っている。1 文の修正で閉じる。

## 照合した規約

- `kasane/handbook/cross/comment-policy.md` (always) — 新規・改稿コメント全件。禁止参照 (作業文書・変更識別子・ローカル通番) と禁止記述類型を本文から確認し、`scripts/comment-policy-lint.py --advisory` も実行 (禁止 0 件、要確認は既存分のみで本変更のファイルは 0 件)。コメントが参照する ADR は `cross/ADR-0002 / 0004 / 0005 / 0006`・`core/ADR-0001`・`kmp/ADR-0002` で、いずれも `kasane/decisions/*/index.md` 上 accepted (相方指摘の proposed な 0008 / 0009 の参照は残っていない)
- `kasane/handbook/cross/test-execution.md` (テストの実行・結果の報告・完了判定) — kmp / android の全件コマンドと件数の得方 (`build/test-results/<ターゲット>/TEST-*.xml` の属性合算)、`scripts/scenario-id-coverage.py` (未網羅なし)
- `kasane/handbook/cross/sample-parity.md` (`samples/` を触る) — 変更はビルド定義の依存版と説明コメントのみで、デモ項目・文言・色トークン・OS 操作への反応に触れないためパリティ要件の対象なし
- `kasane/handbook/cross/diagnostic-message-language.md` — 適用外。対象はライブラリ本体 4 形態のソースで、本変更が足した診断は同文書が対象外に挙げる「リポジトリ内のビルドだけで動く診断」
- `kasane/handbook/cross/verification-ci.md` / `ci-flaky-test-policy.md` / `ci-script-deletion.md` / `user-skill-*.md` / `docs-refresh-timing.md` / `runtime-behavior-verification.md` / `local-development-setup.md` / `aiforms-origin-reference.md` — 適用外 (workflow・`skills/`・実行時挙動・移植機能のいずれにも触れない)
- `kasane/lessons/impl.md` L-001 / `process.md` L-001・L-002 / `spec-review.md` L-001〜L-003 を適用。`kasane/lessons/code-review.md` は不在

## 指摘事項

### [🟡 Minor] 実証範囲へ絞った主張の書き直しが 1 箇所取り残されている

**該当箇所**: `evidence/publish-to-maven-local.txt:11-13`

**問題点**: 当該箇所は「URL 上書きを使っている理由 … と、それが SwiftPM 連携メタデータ以外の成果物に影響しないことは evidence/swiftpm-reference-derivation.txt に記録した」と書いている。しかし参照先の `evidence/swiftpm-reference-derivation.txt` は本サイクルで書き直され、いま記録しているのは逆の内容である — 同 80 行「URL の違いが成果物へ与える影響として実証できたのは root publication までである」、同 90〜92 行「`.module` … 差分は SwiftPM 連携メタデータのファイル項目の size と 4 種のチェックサムだけ … 完全同一にはならない」、同 96〜98 行「klib と aar は … 比較対象が存在せず、URL の違いによる影響は未確認である」。`deviation.md` の該当項目も「全 publication の POM / `.module` / klib / aar が URL に依存しないことは実証していない」に直っている。

つまり 3 箇所あった同じ主張のうち 2 箇所は実証範囲に合わせて直り、この 1 箇所だけが旧主張のまま残って、参照先の記録内容を誤って要約している。証跡は蒸留・アーカイブされて phase-8 / 9 の入力になるため、「URL を差し替えても SwiftPM メタデータ以外の成果物は同じ」と読まれうる点は前回指摘と同じで、`kasane/lessons/process.md` L-002 (互換・無影響の主張は実証した範囲に限定し、カバーしない面を分けて明記する) にそのまま抵触する。

**推奨修正**: 当該の 1 文を、参照先の現在の記録と一致する形に直す — 例えば「URL 上書きを使っている理由 (既定 URL では配信リポジトリに当該 tag が無く cinterop klib の生成が完了しないため) と、URL の違いによる成果物への影響をどこまで実証できたか (root publication まで。klib / aar は未確認) は evidence/swiftpm-reference-derivation.txt に記録した」。本節が確認しているのは発行物の配置・`.module`・POM であり、それらが `file://` ケースの実測であること自体は変わらないので、修正はこの 1 文で閉じる。

### [🔵 Suggestion] `@Throws` 検査の型比較が単純名で行われている

**該当箇所**: `kmp/ksdialogs-kmp/src/androidHostTest/kotlin/jp/kamusoft/ksdialogs/kmp/SwiftBoundaryThrowsTests.kt:110-116`

**問題点**: 期待と実際の突き合わせを `Class` ではなく `simpleName` の並びで行っているため、同名別パッケージの例外型に差し替わっても検査は緑のままになる。実害の確率は低い (この 2 型は同一パッケージ空間で一意) が、この検査の値打ちは「境界の契約を型として固定すること」にあるので、比較の粒度が名前まで落ちているのは狙いとわずかにずれる。

**推奨修正**: 比較は `Set<Class<*>>` 同士 (または `name` の並び) で行い、`simpleName` は失敗メッセージの整形にだけ使う。読みやすい失敗メッセージは今のまま保てる。

### [🔵 Suggestion] 2 段ガードの 2 段目は configuration cache と両立しない

**該当箇所**: `kmp/build.gradle.kts:82`、`android/build.gradle.kts:82`

**問題点**: `gradle.taskGraph.whenReady { }` は Gradle の configuration cache では利用できない API で、有効化すると構成段階で弾かれる。現状のリポジトリは configuration cache を有効にしておらず (`gradle.properties` に設定なし)、ビルドが毎回出す「Consider enabling configuration cache」も助言にとどまるため、いま壊れているものは無い。ただし前実装 (`tasks.configureEach { doFirst { } }`) は両立していたので、将来 configuration cache を入れる判断をするときはこのガードが最初に当たる箇所になる。

**推奨修正**: 修正不要。configuration cache を検討する時点 (phase-9 の release workflow で速度が問題になるなら、そこ) の申し送りとして扱う。

## アクションプラン

1. `evidence/publish-to-maven-local.txt:11-13` の 1 文を、書き直し済みの `evidence/swiftpm-reference-derivation.txt` の記録内容と一致する形に直す (Minor)
2. Suggestion 2 件は任意。テストの型比較 (Suggestion 1) は数行で閉じるので同梱してもよい。configuration cache (Suggestion 2) は蒸留 / phase-9 への申し送り

## 確認して問題がなかった観点 (自分で再実行した検証)

- **SNAPSHOT ガード (前回 Major の修正点)**: `kmp/` で認証情報のプロパティを渡さず `--offline` で実行し、`publishToMavenCentral` / `publishAndReleaseToMavenCentral` / `publishAllPublicationsToMavenCentralRepository` / `prepareMavenCentralPublishing` / `enableAutomaticMavenCentralPublishing` の 5 経路が、いずれも `kmp/build.gradle.kts` 79 行の SNAPSHOT 診断だけを理由に設定段階で失敗することを確認した (ネットワーク不通・認証不足のメッセージは出ない)。`dropMavenCentralDeployment` はタスク自身の入力未設定で落ち、ガードは掛かっていない。集約 `publish` だけが SNAPSHOT 診断 + 認証情報未解決の同時報告で、deviation の「残り」の記述と実測が一致する。なお spec の Requirement / Scenario が対象としているのは「名前に `MavenCentral` を含むタスク」で、`publish` はその集合に入らない — 実装は spec より広く捕まえており、deviation の当該項目は保守側に倒した記録として読める
- **android 側の同型化 (付随修正)**: `android/` でも同じ 2 経路を実行し、既存文言 (`SNAPSHOT (0.1.0-SNAPSHOT) は Maven Central へ発行しない。…`) で同じ挙動になることを確認。`./gradlew test --rerun-tasks` は BUILD SUCCESSFUL、結果 XML の合算で 68 tests / 0 failures。同梱条件 (本務と同じ発行規律の姉妹面・公開 API 非該当・2 ファイル・既存テストで担保・判断不要) に収まっている
- **合成ビルドでの二重発火**: `kmp/` は `includeBuild("../android")` を持つため、SNAPSHOT で Central 向けタスクを指定すると両ルートのガードが対象と判定しうるが、実測では kmp 側が先に落ちて診断は 1 つ。`-Pversion=` は両ルートが同じコマンドラインから読むため、片方だけガードが残る組み合わせは生じない
- **version 導出と発行物**: 空の一時 repository へ `publishToMavenLocal` (注入なし) → 5 publication すべて `0.1.0-SNAPSHOT`、`ksdialogs-kmp-android` の POM の `ksdialogs-core` 依存も同じ文字列、`.asc` は 0 件。注入値 5 パターン (空文字 / `' 1.0.0 '` / `v1.0.0` / `1.0.0-pre` / `0.1.0-beta.1`) の拒否・受理も再現
- **Swift 参照の 3 分岐**: SNAPSHOT → `SwiftPMDependency.Local` + `packageName: ios` + `iosDeploymentVersion: 17.0`。`-Pversion=0.1.0-beta.1` (URL 上書きなし) → `Remote` + `https://github.com/kamusoft/KsDialogs-SPM` + `Exact 0.1.0-beta.1` + `17.0`。`-Pksdialogs.swiftPackageUrl=file://…` → 同形で URL だけが差し替わる。SNAPSHOT で URL プロパティを渡しても `Local` のままで無視される (spec の「SNAPSHOT のとき無視される」を実測)
- **`:api-surface-check` の非公開**: `tasks --all` に現れる publish 系は AGP の `prepareLintJarForPublish` のみ
- **テスト**: `cd kmp && ./gradlew allTests compileCommonMainKotlinMetadata compileIosMainKotlinMetadata --rerun-tasks` が BUILD SUCCESSFUL、結果 XML 合算で androidHostTest 78 / iosSimulatorArm64 75 = 153 tests / 0 failures / 0 skipped。`kasane/handbook/cross/test-execution.md` の更新値 (153 件・2026-09-09) と一致
- **`@Throws` 検査の拡張 (前回 Minor 1 の修正点)**: 肯定側が期待例外型の集合の完全一致になり、suspend 3 経路は `{DialogException, CancellationException}`、`KsToast.show` は `{DialogException}` を要求する形になっている。commonMain 側の宣言 (`KsDialog.kt:32` / `KsLoading.kt:50` / `KsLoading.kt:120` / `KsToast.kt:58`) と一致し、どちらの型が落ちても当該メソッド名を示して失敗する。4 経路の件数一致検査と否定側 (集合が空) はそのまま
- **置換コメント (前回 Minor 2 の修正点)**: `samples/kmp/settings.gradle.kts` の 2 箇所と `samples/android/settings.gradle.kts` の 1 箇所が、いずれも「公開済み成果物への無音フォールバックを避けるため置換を明示する」という現在形の理由に揃った。発行設定を持った現状と食い違う説明は残っていない
- **Sample の依存版**: `samples/kmp` で `:shared` の `commonMainApi` が `jp.kamusoft:ksdialogs-kmp:0.1.0-SNAPSHOT` (カタログ値) を宣言し、置換で included build へ向くことを確認。`samples/kmp/iosApp/KotlinMultiplatformLinkedPackage/` の追跡ファイルに差分なし
- **規約検査**: `scripts/comment-policy-lint.py --advisory` 禁止 0 件、`scripts/local-path-lint.py` / `scripts/identity-lint.py` 違反 0 件、`scripts/scenario-id-coverage.py` 未網羅なし
- **足場と tasks.md**: `proposal.md` / `design.md` / `specs/**` に書き換えなし。tasks.md の 15 項目のチェックはいずれも対応する実装または証跡があり、字面が deviation で変わったもの (1.1 の `doFirst`・2.1 の `sourcesJar = true`) は deviation.md に記録済み
- **証跡の書き直し**: `evidence/snapshot-central-publish-guard.txt` は「認証情報のプロパティを一切渡さずに」実行した結果に置き換わっており、私の再実行と一致する (10 経路 + 集約 `publish` + 除外タスク + ローカル発行)。`evidence/swiftpm-reference-derivation.txt` も実証範囲 (root publication まで・klib / aar は未確認・`packageName` は URL 依存) を分けて書く形に直っている

## 申し送り

- `deviation.md` 冒頭の「ルートへの `alias(libs.plugins.kotlinMultiplatform) apply false`」は依然「オーナー承認待ち」のまま。蒸留の前に承認の有無を確定させる必要がある (前回からの持ち越し)
- SNAPSHOT ガードの診断文言が kmp (英語 + 日本語の混在。spec が指定した文字列) と android (全文日本語) で割れている点は前回 Suggestion のまま未着手。2 段化で両ルートの構造は完全に揃ったので、文言を揃えるなら蒸留か phase-9 が好機
- 集約 `publish` 経由で SNAPSHOT 診断と認証情報未解決が同時に出る件は、phase-9 の release workflow が `publish` ではなく `publishToMavenCentral` 系を直接呼ぶ限り表に出ない。workflow の入口タスク選定時の材料として残す
