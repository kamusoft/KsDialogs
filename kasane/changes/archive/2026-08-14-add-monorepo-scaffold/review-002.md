# レビュー結果: add-monorepo-scaffold (002 回目)

**日付**: 2026-08-14
**判定**: APPROVED

## サマリー

修正サイクル1周目で採用された6件 (Major 2 / Minor 4) は、**すべて実物で解消を確認した**。うち Major 2件は「実装がそう書かれている」だけでなく、`git check-ignore` の挙動と再ビルド成果物という**振る舞いのレベルで**解消を検証している。

本レビューで ios (`swift test`) と maui (`dotnet test`、3 TFM) を修正後の状態で再実行し、いずれも成功。android / kmp は検証環境に Android SDK が無く再実行できないが、成果物の更新時刻がソース修正 (15:58〜15:59) より後 (android 15:59:51 / kmp 16:02:25) で、テスト結果 XML は3件とも `failures="0" errors="0"` — 修正後のビルドが実際に完走している。コメント規約 lint は禁止 0 件 (検査対象は 14 → 13 ファイル、`NativeBridgeProbe.kt` 削除と整合)。

新規の Critical / Major はなし。修正の結果として新たに見えた事項が Suggestion 2件あるが、いずれも本 change のブロッカーではない。

## 採用指摘の解消確認

| # | 採用指摘 | 状態 | 確認した根拠 |
|---|---|---|---|
| 1 | 🟠 Major: `BuildProbe` が公開 API に混入 | **解消** | 下記「指摘1の詳細」参照 |
| 2 | 🟠 Major: `.gitignore` の `*.xcodeproj/` / `*.xcworkspace/` 包括除外 | **解消** | 下記「指摘2の詳細」参照 |
| 3 | 🟡 Minor: `isReturnDefaultValues` が失敗を隠す | **解消** | `kmp/ksdialogs-kmp/build.gradle.kts:24` が `withHostTestBuilder {}` のみになり `.configure { ... }` が消えた。修正後の `testAndroidHostTest` が 16:02 に `tests="1" failures="0"` で完走しており、設定除去でホストテストが壊れていないことも確認 |
| 4 | 🟡 Minor: README の `cd` 連鎖が逐次実行不可 | **解消** | `README.md:25-28` の4行すべてがサブシェル `(cd … && …)` 形式。同一シェルへ一括貼り付けしてもカレントディレクトリが持ち越されない。maui 行は `dotnet test` のみだが、`dotnet test` はビルドを含むため相方の推奨形と等価 |
| 5 | 🟡 Minor: Gradle distribution のチェックサム未固定 | **解消** | `android/gradle/wrapper/gradle-wrapper.properties:4` と `kmp/gradle/wrapper/gradle-wrapper.properties:4` に `distributionSha256Sum=84fbba…73ae` を追加。両ファイルは `diff` で完全一致。下記「指摘5の補足」参照 |
| 6 | 🟡 Minor (ホスト側): バージョン `0.1.0` の二重宣言 | **解消** | `android/gradle/libs.versions.toml:10-12` に `ksdialogs = "0.1.0"` を単一の情報源として新設し、`android/ksdialogs/build.gradle.kts:12` が `version = libs.versions.ksdialogs.get()`、`kmp/ksdialogs-kmp/build.gradle.kts:52` が `implementation("jp.kamusoft:ksdialogs:${libs.versions.ksdialogs.get()}")` で同じ値を引く。カタログは cross/ADR-0004 の共有機構に乗っているため、2つの Gradle ビルドで同一値が保証される |

### 指摘1の詳細 (公開 API への混入)

4ルートすべてで非公開化され、`NativeBridgeProbe` は削除された。

- `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/BuildProbe.kt:7` — `internal object BuildProbe`
- `kmp/ksdialogs-kmp/src/commonMain/kotlin/jp/kamusoft/ksdialogs/kmp/BuildProbe.kt:7` — `internal object BuildProbe`
- `maui/KsDialogs.Maui/BuildProbe.cs:7` — `internal static class BuildProbe` + `maui/KsDialogs.Maui/KsDialogs.Maui.csproj:29-31` の `<InternalsVisibleTo Include="KsDialogs.Maui.Tests" />`
- `ios/Sources/KsDialogs/BuildProbe.swift:3` — 既定の internal を明示化 (`internal enum`)、コメントも実態に更新
- `kmp/ksdialogs-kmp/src/androidMain/` — ディレクトリごと消滅 (`find kmp/ksdialogs-kmp/src -type f` の結果は commonMain / commonTest の2ファイルのみ)

テストからの参照は4ルートとも維持されている (Kotlin は test/commonTest が friend、MAUI は `InternalsVisibleTo`、Swift は `@testable import`)。実際に修正後のテストが4ルートすべてで合格しているため、非公開化がテストの到達性を壊していないことを確認済み。

**`NativeBridgeProbe` 削除による composite 接続の証明の変化**: 従来は Native 側シンボルの実参照でコンパイル時に置換成立を示していたが、現在は依存宣言 (`kmp/ksdialogs-kmp/build.gradle.kts:50-53`) の**解決**が証明になる。デルタスペックの Scenario が要求しているのは「`jp.kamusoft:ksdialogs` が includeBuild によりローカルの android/ ビルドへ置換解決され」であり、要求そのものは解決レベルなので充足は維持されている。加えて (a) commonMain のソースは android ターゲットのコンパイル対象に含まれるため androidMain の compile classpath は実際に解決される、(b) リポジトリ宣言は google と mavenCentral のみで `mavenLocal()` は無く、`jp.kamusoft:ksdialogs` はどこにも公開されていない — したがって解決が成功した事実自体が composite 置換の成立を意味する。修正後の `testAndroidHostTest` が完走していること、およびオーケストレーター側の `dependencyInsight` が `-> project ':android:ksdialogs' (by composite build)` を示していることと整合する。

### 指摘2の詳細 (.gitignore)

`*.xcodeproj/` と `*.xcworkspace/` の2行が削除され、残るのは `.build/` `.swiftpm/` `DerivedData/` `xcuserdata/` `Package.resolved`。**実際の無視判定を `git check-ignore -v` で確認した**:

| パス | 判定 |
|---|---|
| `ios/Sample/Sample.xcodeproj/project.pbxproj` | 追跡可能 |
| `maui/Native/Foo.xcodeproj/project.pbxproj` | 追跡可能 |
| `ios/Sample/Sample.xcworkspace/contents.xcworkspacedata` | 追跡可能 |
| `ios/.swiftpm/xcode/package.xcworkspace/contents.xcworkspacedata` | 無視 (`.swiftpm/`) |
| `ios/DerivedData/x` | 無視 (`DerivedData/`) |
| `ios/.build/x` | 無視 (`.build/`) |
| `ios/Sample/Sample.xcodeproj/xcuserdata/u.xcuserdatad/x` | 無視 (`xcuserdata/`) |

ソース管理すべきプロジェクト定義は追跡対象に戻り、生成物とユーザー状態 (SwiftPM 生成ワークスペースを含む) は引き続き除外される。指摘が求めた分離が正確に実現している。

### 指摘5の補足 (チェックサム)

宣言値の正しさについて、検証環境からは独立に裏付けられなかった (ローカルの Gradle 配布キャッシュは展開後に zip が削除され `gradle-9.7.0-bin.zip.ok` のマーカーのみが残っており、公式チェックサムの取得もこの環境では実行できなかった)。ただし `distributionSha256Sum` は Gradle wrapper が起動時にダウンロード物と照合する値であり、**値が誤っていればビルドが失敗する**性質を持つ。オーケストレーター側がスクラッチの `GRADLE_USER_HOME` で再ダウンロードを伴う検証を通したという報告は、この照合が実際に成功したことを意味するため、値の正しさの根拠として十分と判断する。

## 指摘事項 (新規)

### [🔵 Suggestion] Kotlin の `internal` は JVM バイトコード上は public のまま残る

**該当箇所**: `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/BuildProbe.kt:7` (同じ性質は `kmp` 側の commonMain にも当てはまる)

**問題点**: 修正後の aar (`android/ksdialogs/build/outputs/aar/ksdialogs-release.aar`) から `classes.jar` を取り出して `javap` で確認したところ、次のとおり JVM レベルでは公開のままだった。

```
public final class jp.kamusoft.ksdialogs.BuildProbe {
  public static final jp.kamusoft.ksdialogs.BuildProbe INSTANCE;
  public static final java.lang.String PACKAGE_NAME;
```

Kotlin の `internal` はモジュール外の **Kotlin** コードからは見えなくする一方、バイトコード上は public として出力される (関数はシグネチャがマングルされるが、クラスと `const` プロパティは素の名前で残る)。したがって Java から aar を使う利用者や、Kotlin メタデータを見ないバイナリ検査ツールからは依然として到達できる。

指摘1が問題にした「Kotlin / Maven 成果物の公開契約に載り、除去時に破壊的変更になる」という核心は解消されている (Kotlin API としては非公開になり、Kotlin メタデータを尊重する API 検証ツールも非 API として扱う) ため、追加対応は必須ではない。

**推奨修正**: 対応するなら選択肢は2つ。(a) probe をテストソースセット (Android は `src/test`、KMP は commonTest) だけに置き、成果物から完全に外す — ただしその場合 main のソースが空になり、「main がコンパイルされる」という疎通確認は弱くなる。(b) 現状維持とし、実 API が入る phase-4 で probe ごと削除する。scaffold の寿命を踏まえると (b) で十分で、本 change での対応は不要と考える。

### [🔵 Suggestion] KMP モジュール自身のバージョンだけリテラルのまま残っている

**該当箇所**: `kmp/ksdialogs-kmp/build.gradle.kts:13` (`version = "0.1.0"`)

**問題点**: 指摘6の修正でカタログに新設された `ksdialogs` は、コメントどおり「Android Native ライブラリ `jp.kamusoft:ksdialogs` の発行バージョン」にスコープされており、KMP 自身の成果物 `jp.kamusoft:ksdialogs-kmp` のバージョンは対象外。別 artifact なので独立にバージョニングできる設計は妥当だが、結果として android 側はカタログ由来・kmp 側はリテラルという非対称が残り、両者がたまたま同じ `0.1.0` であることが読み手には偶然か意図か判別できない。

**推奨修正**: publish の配線を入れるフェーズで、KMP 側も `ksdialogs-kmp` エントリとしてカタログに載せるか、独立バージョニングが意図であることをコメントで明示する。現時点で実害はない。

## 前回からの残課題の状況

verify-001.md で「アーカイブ前に閉じること」と記録した **task 2.3 (swift-tools-version の cross/ADR-0002 への追記) は本ラウンドで完了**していた。`kasane/decisions/cross/0002-tech-stack-2026-08.md` は swift-tools-version 6.3 の確定値と実測環境 (Xcode 26.5 / Swift 6.3.2) を Decision に記載し、Consequences から「未確定のまま持ち越し」の行を削除している。実物 `ios/Package.swift:1` の `// swift-tools-version: 6.3` とも一致する。tasks.md も全 16 タスクがチェック済みとなり、**虚偽チェックは引き続きなし**。

## アクションプラン

なし。採用6件はすべて解消済みで、新規 Suggestion 2件はいずれも将来フェーズでの再判断に委ねてよい。蒸留・アーカイブへ進んで差し支えない。

## 確認した観点 (指摘なし)

- 修正後に4ルートすべてのビルド・テストが成功 (ios / maui は本レビューで再実行、android / kmp は修正後の成果物とテスト結果 XML で確認)
- 非公開化によりテストからの到達性が壊れていないこと (4ルートのテストが全件合格)
- 削除された `NativeBridgeProbe` の残骸なし (`kmp/ksdialogs-kmp/src` 配下は commonMain / commonTest の2ファイルのみ)
- コメント規約 lint 禁止 0 件。修正で追加されたコメント (「公開 API ではないため〜」等) も許容参照のみで、変更 ID・文書パス・仕様構文キーワードの混入なし
- `git check-ignore` の実測により、`.gitignore` の修正が生成物・ユーザー状態の除外を巻き添えにしていないこと
- 公開識別子・最低対象 OS の宣言値は修正前後で不変 (cross/ADR-0005 / cross/ADR-0002 と一致を維持)
- 記録のみとした前回の指摘 (konan override の脆さ / MAUI テスト namespace / gradle.properties 重複 / `Package.resolved` の無視) は本レビューでは再提起していない
