# レビュー結果: add-kmp-maven-distribution (001 回目)

**日付**: 2026-09-09
**判定**: CHANGES_REQUESTED

## サマリー

発行の配線・version の単一ソース・Swift 参照の version 導出・SNAPSHOT ガード・署名の連動は、いずれも spec の Requirement を満たしており、手元で再実行して裏付けが取れた (注入値 5 パターンの受理/拒否、Central 向け 11 タスクのガード発火と `dropMavenCentralDeployment` の非対象、`:api-surface-check` に発行タスクが無いこと、`testAndroidHostTest` 78 件 / 0 failures、`samples/kmp` の構成解決、標準 lint 3 本の違反 0)。証跡 7 本も、発行物の配置・`.module` の variant・POM 共通部・署名の 1 対 1 まで踏み込んでおり、リスク ①③ の解消が読み取れる水準にある。

一方で、Critical / Major はないものの、優先度の高い Minor が 3 件ある — ① 新設した `@Throws` 回帰検査が、同じ宣言に載っていて Swift 側の abort を防いでいる `CancellationException` を見ていない (本変更が塞ごうとした退行型がそのまま通る)、② 本変更が書き直した置換コメントの真下に、同じ理由で現在は偽になっているコメントが残っている、③ deviation と証跡が「URL の違いは SwiftPM 連携メタデータ以外に影響しない」と、保存した成果物では裏付けられない範囲まで主張している。いずれも局所の修正で閉じる。

## 照合した規約

- `kasane/handbook/cross/comment-policy.md` (always) — 全新規コメント。禁止参照・禁止記述類型を本文から確認し、`scripts/comment-policy-lint.py --advisory` も実行 (禁止 0 件 / 要確認は既存分のみ)
- `kasane/handbook/cross/test-execution.md` (テストの実行・結果の報告・完了判定) — kmp の全件実行コマンド、件数の得方 (`build/test-results/<ターゲット>/TEST-*.xml`)、`scripts/scenario-id-coverage.py` の実行 (未網羅なし)
- `kasane/handbook/cross/sample-parity.md` (samples/ を触る) — 本変更の samples/ への変更はビルド定義の依存版と説明コメントのみで、デモ項目・文言・色トークン・OS 操作への反応に触れないため、パリティ要件の対象なしと判定
- `kasane/handbook/cross/diagnostic-message-language.md` — 適用外と判定。`applies-when.paths` はライブラリ本体 4 形態のソースで、本変更が文言を足したのは `kmp/build.gradle.kts` / `kmp/ksdialogs-kmp/build.gradle.kts` のビルド定義。同文書が対象外に挙げる「リポジトリ内のビルドだけで動く診断 (パッケージに同梱されず利用者のビルドには届かない)」に当たる
- `kasane/handbook/cross/verification-ci.md` / `ci-flaky-test-policy.md` / `ci-script-deletion.md` / `user-skill-*.md` / `docs-refresh-timing.md` / `runtime-behavior-verification.md` / `local-development-setup.md` / `aiforms-origin-reference.md` — 適用外 (workflow・skills/・実行時挙動・移植機能のいずれにも触れない)
- `kasane/lessons/impl.md` L-001 / `process.md` L-001・L-002 / `spec-review.md` L-001〜L-003 を適用。`kasane/lessons/code-review.md` は不在

## 指摘事項

### [🟡 Minor] `@Throws` の回帰検査が `CancellationException` を見ておらず、Swift 側 abort の退行がすり抜ける

**該当箇所**: `kmp/ksdialogs-kmp/src/androidHostTest/kotlin/jp/kamusoft/ksdialogs/kmp/SwiftBoundaryThrowsTests.kt:89`

**問題点**: 肯定側の検査は `method.exceptionTypes.any { it == DialogException::class.java }` で、宣言に `DialogException` が含まれることしか見ていない。4 経路のうち suspend の 3 本は実際には 2 つの型を宣言している (`kmp/ksdialogs-kmp/src/commonMain/kotlin/jp/kamusoft/ksdialogs/kmp/KsDialog.kt:32` / `KsLoading.kt:50` / `KsLoading.kt:120` が `@Throws(DialogException::class, CancellationException::class)`。`KsToast.kt:58` は非 suspend で `DialogException` のみ)。コンパイル後の JVM throws 節にも両方が載ることを確認した:

```
public abstract <R> java.lang.Object show(...DialogViewModel<R>, ...DialogPlacement, ...Continuation<...>)
    throws jp.kamusoft.ksdialogs.kmp.DialogException, java.util.concurrent.CancellationException;
```

`CancellationException::class` だけが落ちた場合、Kotlin 側のビルドもこのテストも緑のままだが、Kotlin/Native は `@Throws` に挙がっていない例外が Swift / ObjC 境界を越えると「想定外」としてプロセスを終了させるため、Swift から呼んだ suspend 関数が取り消されるとアプリが abort する。これは proposal の Why が塞ぐと述べた「Kotlin 側は緑のまま、利用者の Swift コードが abort して初めて分かる退行型」そのもので、検査の網から外れている経路が残っている。

spec の Requirement は `DialogException` しか要求していないため spec 違反ではない (この指摘は Requirement の狙いに沿った拡張であり、spec に反するものではない)。取り込むかはオーナー判断でよいが、テスト側だけの数行で閉じる。

**推奨修正**: `ThrowingRoute` に期待する例外型の集合を持たせ、`any { }` ではなく集合の完全一致で検査する (suspend の 3 経路は `{DialogException, CancellationException}`、`KsToast.show` は `{DialogException}`)。否定側が既に「集合が空であること」の完全一致なので、肯定側も同じ形になり読み口も揃う。

### [🟡 Minor] 書き直した置換コメントの直下に、同じ理由で偽になったコメントが残っている

**該当箇所**: `samples/kmp/settings.gradle.kts:48`

**問題点**: 本変更は同ファイル 37〜40 行の KMP facade の置換コメントを「公開版への無音フォールバックを避けるため常にローカルソースへ置換する」に書き直した (発行設定を持った後も成立する説明にするため)。しかし 8 行下の `../../android` の置換に付いた「AGP のライブラリモジュールは Maven publication を生成せず自動置換が発火しないため、置換を明示する」はそのまま残っている。`android/` の `:ksdialogs-core` / `:ksdialogs` は phase-5 (`android/build.gradle.kts` の `plugins.withId("com.vanniktech.maven.publish")` 経路) で発行設定を持ったため、この理由付けは現在の実物と食い違う。comment-policy の「現在の仕様を現在形で書く」に照らして直す対象であり、本務で触るファイル内・局所・判断不要なので付随修正の同梱条件に収まる。

なお同一の文が `samples/android/settings.gradle.kts:38` にもある (diff 範囲外)。同じ姉妹面なので、同梱するか別途起票するかはオーナー判断。

**推奨修正**: 48 行のコメントを、37〜40 行と同じ「公開済み成果物への無音フォールバックを避けるため置換を明示する」という現在形の理由に揃える。

### [🟡 Minor] 「URL の違いは SwiftPM 連携メタデータ以外に影響しない」が、保存した成果物では裏付けられない

**該当箇所**: `deviation.md:6`、`evidence/swiftpm-reference-derivation.txt:78`

**問題点**: deviation は「全 publication の検証 (6.1〜6.3) は tag 付きローカル clone の `file://` URL で行い、URL の違いが SwiftPM 連携メタデータ以外 (POM / `.module` / klib / aar) に影響しないことを両ケースの突き合わせで確認済み」と書き、証跡も同じ主張を置いている。しかし既定 URL のケース (case-3) は同証跡 54 行が明記するとおり root publication だけを発行しており、android / iOS の POM・`.module`・klib・aar はそもそも生成されていない。突き合わせられるのは root の POM / `.module` までで、「klib / aar に影響しない」は比較対象が存在しないまま述べられている。

さらに同証跡 114 行は `packageName` が URL 末尾から導出されて両ケースで異なる (`KsDialogs-SPM` / `spm-clone`) ことを自ら記録しており、「URL は SwiftPM 連携メタデータの中身だけを変える」を無条件には言えない材料が同じ文書内にある。lessons `process.md` L-002 (互換・無影響の主張は実証した範囲に限定し、実証がカバーしない面を分けて明記する) に抵触する。

実害の大きさは小さい (公開座標での実解決は phase-8 の責務と Non-Goals にある) が、この記述は phase-8 / 9 が「URL を差し替えても成果物は同じ」と読む根拠になりうるため、記録の側を実証範囲に合わせる。

**推奨修正**: 主張を実証済みの範囲に絞る — 「既定 URL のケースは root publication のみ発行しており、root の POM / `.module` は両ケースで同一。それ以外の publication (POM / `.module` / klib / aar) は `file://` ケースでのみ検証しており、公開 URL での成果物の確認は phase-8 に残る」。`packageName` が URL 依存であることも同じ段落に併記する。

### [🔵 Suggestion] SNAPSHOT ガードの診断文言が android/ と kmp/ で割れている

**該当箇所**: `kmp/build.gradle.kts:65`、`android/build.gradle.kts:132`

**問題点**: 同じ規則の姉妹実装なのに、kmp は `Refusing to publish a SNAPSHOT version to Maven Central: <version>. リリース版の version は…` (英語 + 日本語の混在)、android は `SNAPSHOT (<version>) は Maven Central へ発行しない。…` (全文日本語) と文言が異なる。kmp 側は spec の Requirement が指定した文字列どおりなので本変更の欠陥ではなく、揃えるとすれば android 側 (本変更のスコープ外) を動かすことになる。

**推奨修正**: 修正不要。蒸留または phase-9 (release workflow がこの診断を読む側になる) で、両ビルドルートの文言を揃えるかを決める申し送りとして扱う。

## アクションプラン

1. `deviation.md:6` と `evidence/swiftpm-reference-derivation.txt:78` の無影響の主張を実証範囲に絞る (Minor 3)
2. `samples/kmp/settings.gradle.kts:48` のコメントを現在形の理由に直す。`samples/android/settings.gradle.kts:38` を同梱するかはオーナーに諮る (Minor 2)
3. `SwiftBoundaryThrowsTests.kt` の肯定側を期待例外型の集合の完全一致にする (Minor 1)。spec の要求を超える拡張なので、見送る判断をした場合は「`CancellationException` の脱落は検査の対象外」を deviation か phase-8 の申し送りに残す
4. SNAPSHOT ガードの文言の統一 (Suggestion) は蒸留 / phase-9 へ

## 確認して問題がなかった観点 (再実行した検証)

- 注入値の受理/拒否: `./gradlew help -Pversion=<値>` を 5 パターン (空文字 / `' 1.0.0 '` / `v1.0.0` / `1.0.0-pre` / `0.1.0-beta.1`) 実行。前 4 件が設定段階で同一診断により失敗、最後が成功
- SNAPSHOT ガード: `--offline` の `:ksdialogs-kmp:publishToMavenCentral` が `prepareMavenCentralPublishing` で `Refusing to publish a SNAPSHOT version to Maven Central: 0.1.0-SNAPSHOT` により失敗 (ネットワーク・認証を理由としない)。`dropMavenCentralDeployment` はタスク自身の入力未設定で落ち、ガードは掛かっていない
- `:api-surface-check` の非公開: `tasks --all` に現れる publish 系は AGP の `prepareLintJarForPublish` のみ
- テスト: `cd kmp && ./gradlew testAndroidHostTest --rerun-tasks` が BUILD SUCCESSFUL、結果 XML の合算で 78 tests / 0 failures / 0 skipped。`kasane/handbook/cross/test-execution.md` の 153 件 (androidHostTest 78 + iosSimulatorArm64 75) と整合
- `SwiftBoundaryThrowsTests` の網の張り方: `$default` 名と synthetic の除外が効いていること (`show$default` にも throws 節が付くため、除外がないと否定側が落ちる) と、4 経路が第 1 引数型で一意に決まること (typed show は `KClass` 始まりで衝突しない) を javap の出力で確認
- Sample: `cd samples/kmp && ./gradlew help` が成功 (カタログ参照の解決)。`samples/kmp/iosApp/KotlinMultiplatformLinkedPackage/` の追跡ファイルに差分なし
- 規約検査: `scripts/comment-policy-lint.py --advisory` 禁止 0 件、`scripts/local-path-lint.py` 違反 0 件、`scripts/identity-lint.py` 違反 0 件、`scripts/scenario-id-coverage.py` 未網羅なし
- tasks.md の 15 項目のチェックは、いずれも対応する実装または証跡が存在する (虚偽チェックなし)。足場アーティファクト (proposal / design / specs) に書き換えなし
- deviation の 4 件はいずれも記録済みの差分として扱った。`sourcesJar = SourcesJar.Sources()` の付随修正は同梱条件 (本務で触るファイル・公開 API 非該当・局所・既存テストで担保・判断不要) に収まっている
- POM 共通部の複製 (design Decision 1) は `android/build.gradle.kts` の url / license / developers / scm / inceptionYear と字面まで一致

## 申し送り

- `deviation.md:6` が挙げる「phase-9 の release workflow は SPM tag を push してから Maven へ発行する順序が必須」は、`:api-surface-check` に登録される umbrella SwiftPM パッケージの解決が cinterop klib 生成の前提になることから来ており、phase-9 の agenda に効く制約として妥当。証跡 (`evidence/swiftpm-reference-derivation.txt` の「発見」節) に失敗メッセージまで残っている
- `deviation.md:2` (ルートへの `alias(libs.plugins.kotlinMultiplatform) apply false`) は「オーナー承認待ち」のまま。蒸留の前に承認の有無を確定させる必要がある
