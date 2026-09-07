# レビュー結果: add-monorepo-scaffold (001 回目)

**日付**: 2026-08-14
**判定**: APPROVED

## サマリー

4ビルドルートの scaffold は cross/ADR-0002・0004・0005 の決定を過不足なく実体化しており、識別子・最低対象 OS の宣言はすべて写像表と ADR の値に一致する。ビルド・テストは iOS (`swift test` / `xcodebuild` iOS Simulator) と MAUI (`dotnet test`、3 TFM) を本レビューで再実行して成功を確認し、Android / KMP は Android SDK 未設定のため再実行できなかったが、残存するテスト結果 XML (android 1件・kmp の androidHostTest / iosSimulatorArm64Test 各1件、いずれも failures=0) と成果物 (aar / klib / metadata jar) で成功を確認した。コメント規約 lint も 0 件 (selftest 全件 OK)。

Critical / Major はなし。指摘は将来のフェーズで顕在化する種類の Minor 3件と Suggestion 4件で、いずれも本 change のブロッカーではない。

## 指摘事項

### [🟡 Minor] KMP が参照する Android Native の依存バージョンがハードコード重複している

**該当箇所**: `kmp/ksdialogs-kmp/build.gradle.kts:54` (`implementation("jp.kamusoft:ksdialogs:0.1.0")`) と `android/ksdialogs/build.gradle.kts:12` (`version = "0.1.0"`)

**問題点**: 同じバージョン値が2つの独立した Gradle ビルドに別々のリテラルとして存在する。composite build の依存置換は group:module のみで照合しバージョンを無視するため、android/ 側だけを 0.2.0 に上げても kmp/ のローカルビルドは成功し続け、齟齬に気づけない。一方 kmp/ を publish した際の POM は宣言文字列どおり `jp.kamusoft:ksdialogs:0.1.0` を要求するため、公開成果物だけが誤ったバージョンを指す。cross/ADR-0004 は「Kotlin / AGP の整合はバージョンカタログ共有で緩和する」と決めており、自プロジェクトのバージョンだけがその機構の外にある。

**推奨修正**: バージョンを共有バージョンカタログ (`android/gradle/libs.versions.toml`) の `[versions]` エントリか、両ビルドが読む共通の properties に一元化し、android/ の `version` と kmp/ の依存宣言の双方をそこから引く。パッケージング・配布はロードマップ非ゴールのため、実施は publish の配線を入れるフェーズでも構わない。

### [🟡 Minor] iOS の最低対象 OS が内部コンパイラフラグ依存で、壊れても無音になる

**該当箇所**: `kmp/ksdialogs-kmp/build.gradle.kts:35-49` (`-Xoverride-konan-properties` を `binaries.all` に適用)

**問題点**: 本レビューで `kmp/ksdialogs-kmp/build/bin/iosSimulatorArm64/debugTest/test.kexe` の `LC_BUILD_VERSION` が `minos 17.0` であることを確認したので、現時点でフラグは意図どおり効いている。ただし以下2点の弱さがある。(1) ライブラリモジュールの `binaries` には自動生成されるテスト実行体しか入らないため、この指定が届いているのは今のところテストバイナリだけで、公開物である klib には最低対象 OS の概念がなく実 framework を出すまで効果を確認できない。(2) `-X` 接頭辞のフラグは非公開・非保証であり、Kotlin 更新で無効化・改名されてもビルドエラーにはならず既定の deployment target へ黙って戻る。宣言値を守っていることを検査する仕組みが無いため、退行が誰にも見えない。

**推奨修正**: 実 framework を出力するフェーズで同じ指定が framework バイナリにも効いているかを再確認する。あわせて、生成バイナリの `LC_BUILD_VERSION` を照合する軽い検査 (ビルド後タスクまたはテスト) を足せば、フラグの失効を無音で見逃さずに済む。

### [🟡 Minor] .gitignore のパターンが過広で、将来のコミット対象を黙って取りこぼす

**該当箇所**: `.gitignore:12` (`*.xcodeproj/`)、`.gitignore:15` (`Package.resolved`)

**問題点**: `*.xcodeproj/` はリポジトリ全体に効くため、Sample アプリのように「生成物ではなく成果物として管理したい Xcode プロジェクト」が追加されたときに無音で無視される。無視されたファイルは `git status` にも出ないため、追加者が気づく契機がない。`Package.resolved` の無視はライブラリの慣行として妥当だが、ios/ が外部依存を持ちアプリ側の成果物 (Sample) が同居した時点で、依存解決の再現性が失われる側の判断になる。

**推奨修正**: `*.xcodeproj/` は無視したい生成物の場所へスコープを絞る (例: SwiftPM が生成する `ios/` 配下のみ) か、成果物として管理する Xcode プロジェクトが生まれる時点で否定パターンを足す方針をコメントで明示する。`Package.resolved` は現状維持でよいが、アプリ形態の成果物が同居した時点で再判断する。

### [🔵 Suggestion] README の Building ブロックが逐次実行できない

**該当箇所**: `README.md:24-29`

**問題点**: `cd` が累積するため、4行を続けて実行すると2行目の `cd android` が ios/ 配下で解決されて失敗する。単一の `sh` コードブロックはコピー&ペースト前提で読まれる。

**推奨修正**: `(cd ios && swift test)` のようにサブシェルで括るか、`cd` をリポジトリルートからの絶対的な移動として各行に書く。

### [🔵 Suggestion] MAUI テストの namespace が写像表の方針と食い違い、`global::` 修飾を招いている

**該当箇所**: `maui/KsDialogs.Maui.Tests/BuildProbeTests.cs:3,13`

**問題点**: cross/ADR-0005 は「コード上の識別子は素の `KsDialogs`、`.Maui` 修飾は配布上の識別子 (NuGet ID) にのみ残す」と決めている。テストプロジェクトの namespace `KsDialogs.Maui.Tests` はコード上の識別子に `.Maui` を持ち込んでおり、その結果ライブラリ側の型を指すのに `global::KsDialogs.BuildProbe` という修飾が必要な見た目になっている (実際には C# の名前解決上 `global::` なしでも解決するため、修飾は防御的なもの)。テスト側の命名は今後書かれる全テストの雛形になるため、ずれは早いうちに揃えたほうが安い。

**推奨修正**: テストの namespace を `KsDialogs.Tests` 等、コード上は素の `KsDialogs` を根に持つ形に揃え、`global::` 修飾を外す。プロジェクト名 / アセンブリ名 `KsDialogs.Maui.Tests` は NuGet ID 側の系列なので変更不要。

### [🔵 Suggestion] `isReturnDefaultValues = true` は現時点で不要で、将来の失敗を黙らせる

**該当箇所**: `kmp/ksdialogs-kmp/build.gradle.kts:24-26`

**問題点**: この設定は android.jar のスタブメソッドを例外送出ではなく既定値返却に変える。現在の commonTest は Android API に一切触れないため効果がなく、一方で将来 androidMain のロジックをホスト JVM 上でテストするようになったとき、実際には呼べていない API 呼び出しが `null` / `0` を返して静かに通る事故を招く。

**推奨修正**: 必要になるまで `isReturnDefaultValues` の行を落とし、`withHostTestBuilder {}` のみ残す (commonTest のホスト実行はそれで成立する)。

### [🔵 Suggestion] android/ と kmp/ の gradle.properties が完全重複している

**該当箇所**: `android/gradle.properties` と `kmp/gradle.properties` (内容が完全一致)

**問題点**: cross/ADR-0004 は2つの Gradle ビルド間の整合をバージョンカタログのファイル共有で緩和すると決めたが、JVM 引数・キャッシュ設定・AndroidX 系フラグはその機構の外にあり、片方だけ更新されるとビルド挙動が静かに食い違う。

**推奨修正**: 共有したい設定が増えた時点で、カタログと同じくファイル共有 (kmp/ から android/ の properties を読む、または共通ファイルへ切り出す) を検討する。現状は2ファイル6行なので、当面は重複のまま運用してよい。

## アクションプラン

1. (任意・publish 配線時) Android Native の依存バージョンをカタログへ一元化する — Minor 1
2. (任意・phase-4) 実 framework 出力時に iOS deployment target が効いているか再確認し、検査を足す — Minor 2
3. (任意・Sample 導入時) `.gitignore` の `*.xcodeproj/` をスコープする — Minor 3
4. (任意) README の Building ブロック、MAUI テスト namespace、`isReturnDefaultValues` の3点を整える — Suggestion 1・2・3

いずれも本 change のマージを止める必要はない。

## 確認した観点 (指摘なし)

- 足場アーティファクト (proposal.md / spec.md) は実装開始前の更新時刻で止まっており、実装中の書き換えなし
- tasks.md に未実装の虚偽チェックなし (未完了の 2.3 は正直に未チェック)
- コメント規約 (cross/conventions のソースコメント規約): lint 0 件。全コメントが `<domain>/ADR-NNNN` 形式のみを外部参照として使い、変更 ID・文書パス・仕様構文キーワードの混入なし
- 公開識別子・最低対象 OS の宣言値がすべて cross/ADR-0005 / cross/ADR-0002 と一致
- リポジトリルートに共通ビルドファイルなし (cross/ADR-0004)
- `.gitignore` は既存エントリを削らずに4形態分を追記しており、退行なし
- Gradle wrapper (jar / gradlew / properties) が android/ と kmp/ で同一 (Gradle 9.7.0)
- 命名・コメントの粒度・スモークテストの構造が4ビルドルート間で対称
