# セカンドオピニオン: add-monorepo-scaffold (code-001)
**相方**: codex / **日付**: 2026-08-14 / **対象**: 実装 diff (未コミット新規ファイル一式)
---
# レビュー結果: add-monorepo-scaffold

**判定**: CHANGES_REQUESTED  
**指摘件数**: Critical 0 / Major 2 / Minor 3 / Suggestion 0

## サマリー

4ビルドルート、識別子、最低対象 OS、Gradle composite build、スモークテストはデルタスペックおよび関連 ADR と概ね一致しています。コメント規約 lint も禁止事項 0 件でした。

一方、スモークテスト用型が製品の公開 API に露出している点と、Xcode の追跡対象ファイルを `.gitignore` が隠す点は、今後の公開契約や実装を阻害するため修正が必要です。

ビルド・テストは依頼文記載の成功結果を前提とし、再実行していません。task 2.3 は判定対象外です。

## 指摘事項

### [🟠 Major] スモークテスト用マーカーが製品の公開 API に混入している

**該当箇所**:

- [android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/BuildProbe.kt](android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/BuildProbe.kt:7)
- [kmp/ksdialogs-kmp/src/commonMain/kotlin/jp/kamusoft/ksdialogs/kmp/BuildProbe.kt](kmp/ksdialogs-kmp/src/commonMain/kotlin/jp/kamusoft/ksdialogs/kmp/BuildProbe.kt:4)
- [maui/KsDialogs.Maui/BuildProbe.cs](maui/KsDialogs.Maui/BuildProbe.cs:6)

**問題点**: Kotlin の `object` は既定で public、C# は明示的に `public` です。このため `BuildProbe` が Maven/NuGet 成果物の公開 API になります。これは proposal の「API 設計・実装コードは Non-Goal」および tasks.md の「空ソース」というスコープに反します。後で削除すると不要な破壊的 API 変更にもなります。

**推奨修正**:

- Android/KMP の `BuildProbe` を `internal` にする。
- MAUI は `internal` とし、テストには `InternalsVisibleTo` を使用する。
- Android Native のシンボルを参照するためだけの `NativeBridgeProbe` は削除し、composite build の依存解決を Gradle のコンパイルまたは依存解決検証で確認する。

### [🟠 Major] Xcode のプロジェクト／ワークスペース全体が追跡対象外になる

**該当箇所**:

- [.gitignore](.gitignore:12)
- [.gitignore](.gitignore:13)

**問題点**: `*.xcodeproj/` と `*.xcworkspace/` は生成キャッシュとは限らず、通常はソース管理対象です。実際に `git check-ignore` でも `project.pbxproj` と `contents.xcworkspacedata` が除外されます。今後、iOS 実装や MAUI Native Binding で Xcode プロジェクトを追加した際、`git status` に現れず、CIで必要なファイルが欠落する危険があります。

**推奨修正**: この2規則を削除し、`xcuserdata/`、`DerivedData/`、`.swiftpm/` などユーザー状態・生成物だけを除外してください。

### [🟡 Minor] Android ホストテストが未モック API の誤使用を隠す

**該当箇所**: [kmp/ksdialogs-kmp/build.gradle.kts](kmp/ksdialogs-kmp/build.gradle.kts:25)

**問題点**: `isReturnDefaultValues = true` は、未モックの `android.jar` メソッドを例外にせず `0` や `null` で通します。将来の `commonTest` / Android host test が、Android API を誤って使用していても成功する可能性があります。[AGP公式DSL](https://developer.android.com/reference/tools/gradle-api/9.3/com/android/build/api/dsl/KotlinMultiplatformAndroidHostTest)もこの意味を明示しています。現在の定数比較テストには不要です。

**推奨修正**: `isReturnDefaultValues = true` を削除し、`withHostTestBuilder {}` だけを残してください。Android API が必要なテストは mock、Robolectric、または device test へ分離します。

### [🟡 Minor] README のビルドコマンドを連続実行できない

**該当箇所**: [README.md](README.md:25)

**問題点**: コードブロック全体を同一シェルへ貼り付けると、最初の `cd ios` 後もカレントディレクトリが `ios/` のままです。次の `cd android` は `ios/android` を探して失敗します。

**推奨修正**: 各行をサブシェル化してください。

```sh
(cd ios && swift test)
(cd android && ./gradlew build)
(cd kmp && ./gradlew build)
(cd maui && dotnet build KsDialogs.slnx && dotnet test)
```

### [🟡 Minor] Gradle distribution のチェックサムが固定されていない

**該当箇所**:

- [android/gradle/wrapper/gradle-wrapper.properties](android/gradle/wrapper/gradle-wrapper.properties:3)
- [kmp/gradle/wrapper/gradle-wrapper.properties](kmp/gradle/wrapper/gradle-wrapper.properties:3)

**問題点**: `distributionUrl` は固定されていますが、`distributionSha256Sum` がありません。初回ビルド時に取得する Gradle ZIP の改ざん・破損をハッシュで検出できません。Gradle の[セキュリティベストプラクティス](https://docs.gradle.org/current/userguide/best_practices_security.html)ではチェックサム設定が推奨されています。

**推奨修正**: Gradle公式の 9.7.0 `-bin.zip` 用 SHA-256 を確認し、両方のプロパティファイルへ同じ `distributionSha256Sum` を追加してください。

## アクションプラン

1. `BuildProbe` を公開 API から除外する。
2. `.xcodeproj` / `.xcworkspace` の包括的 ignore を削除する。
3. `isReturnDefaultValues` を外してテストの失敗検知を維持する。
4. README のコマンドを独立実行可能にする。
5. Gradle distribution の SHA-256 を固定する。
6. 修正後、既に実施済みの4ビルドルート検証を再実行する。

## 突き合わせ結果

ホスト側 review-001.md (APPROVED / Minor 3 / Suggestion 4) との突き合わせ。

| # | 相方の指摘 | 採否 | 根拠 |
|---|---|---|---|
| 1 | BuildProbe が公開 API に混入 (Major) | **採用** | 相方のみ + 根拠強。公開成果物 (aar / NuGet) の public API になり、除去時に破壊的変更化する。proposal の Non-Goal「API 設計」とも緊張。ホスト側の見逃し |
| 2 | .gitignore の *.xcodeproj/ 包括除外 (Major) | **確定** | ホスト Minor 3 と一致。重要度は高い方 (Major) を採る |
| 3 | isReturnDefaultValues が失敗を隠す (Minor) | **確定** | ホスト Suggestion と一致。高い方 (Minor) を採る |
| 4 | README の cd 連鎖が逐次実行不可 (Minor) | **確定** | ホスト Suggestion と一致 |
| 5 | Gradle distributionSha256Sum 未固定 (Minor) | **採用** | 相方のみ + 根拠妥当 (供給網検証)。修正コストほぼゼロ |

ホスト側のみの指摘: Minor 1 (バージョン 0.1.0 の二重宣言) は採用して同ラウンドで修正。Minor 2 (konan override の脆さ) は代替 DSL が存在せず記録のみ。Suggestion 群 (MAUI テスト namespace / gradle.properties 重複) は対処せず記録のみ。

**統合判定: CHANGES_REQUESTED** (採用 Major 2 / Minor 4) — 修正ラウンド1周を実施し、独立文脈で再確認する。
