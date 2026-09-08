# KMP パッケージング (kmp-packaging)

KsSettingsView に実績のない KMP 形態の配信 (Maven Central へのマルチターゲット publication・dev / publish の Swift 参照切替・Kotlin サポート範囲・公開面の機械検査) と、KMP 消費者検証の形を議論して確定し実装する change フェーズ。本ロードマップで唯一の本格的な議論フェーズ。

## 論点


### 消費者検証の形 (実装は phase-8)


## 決定事項

### A1 発行は vanniktech を kmp にも適用し、composite build は維持 (2026-09-08)

`:ksdialogs-kmp` に `com.vanniktech.maven.publish` 0.37.0 を適用する (`KotlinMultiplatform` 構成、AGP 9 の `com.android.kotlin.multiplatform.library` 向けに `androidVariantsToPublish` は省略。`api-surface-check` には適用しない)。`includeBuild("../android")` はそのまま維持する。composite build の置換は解決時にだけ効き、POM には宣言どおりの `jp.kamusoft:ksdialogs-core:<版>` が載る (PoC 項目 1 の実測) ため、同版厳密指定 (cross/ADR-0009) は kmp/ の version と依存版を phase-5 の導出式 (カタログ + `-Pversion=`) から同じ値で取れば成立する。vanniktech は KGP の既存 publication を修飾するだけで作り直さないため、klib・cinterop klib・Module Metadata は素の `maven-publish` と同じものが出る。SwiftPM 連携メタデータ (`swiftpm-metadata.json` と専用バリアント) が発行物に含まれることは公式に明記がないので、`publishToMavenLocal` の dry-run で確認することを受け入れ条件にする。POM 共通部は kmp/ が別ビルドルート (cross/ADR-0004) のため `kmp/build.gradle.kts` を新設して android 側と同じ内容を書き、対応をコメントで明記する。

- 却下: PoC どおり素の `maven-publish` (SwiftPM メタデータの発行は実測済みで確実だが、署名と Central Portal への upload を kmp だけ別実装することになり、release CI の手順が分岐する)

### A2 Swift 参照は version から導出し、dry-run 用に URL だけ上書きできる (2026-09-08)

phase-5 の導出式で得た version が `-SNAPSHOT` なら `localSwiftPackage(../ios)`、それ以外 (リリース版の注入時) なら `swiftPackage(url, exact(<version>))` を宣言する。専用のモード切替スイッチは持たない。リリース版で local 参照を選べない形にすることで、PoC の判明事実 1 (`localSwiftPackage` のまま発行すると発行者マシンの絶対パスが伝播し、警告も出ない) を構造的に防ぎ、exact の値が version と同じ式から出るため KMP artifact x.y.z → SPM tag x.y.z の lockstep (cross/ADR-0009) を手で揃える箇所をなくす。URL は Gradle プロパティ 1 つで上書きでき (既定 `https://github.com/kamusoft/KsDialogs-SPM`)、dry-run (C1) はスナップショットを同期したローカル clone に commit + tag した `file://` URL を注入して `publishToMavenLocal` する (PoC が `file://` の bare clone + tag で成立を実証済み)。exact(version) は上書きしない。

- 帰結 (A3 解消): 日常はカタログの `0.1.0-SNAPSHOT` なので local 参照のまま。`../ios` のライブ編集も `samples/kmp/iosApp/KotlinMultiplatformLinkedPackage/` の中身も変わらず、差分は生じない
- 既知の限界: SNAPSHOT を `publishToMavenLocal` した成果物には local パスが載る (同一マシンでだけ動く)。SNAPSHOT の消費はリポジトリ内 Sample の composite build が担い、リポジトリ外の検証はリリース版で行う。文書に明記する
- 却下: Gradle プロパティで local / remote を明示切替 (フラグ忘れが残り、publish 時のガードを別途要する) / URL 上書きなしの version 導出のみ (dry-run のたびに配信リポジトリへ一時 tag を push する運用になる)

### A4 Kotlin サポート範囲は同 minor (2.4.x)、確認済み版はカタログ値を転記 (2026-09-08)

消費者向けに「本ライブラリと同じ minor (Kotlin 2.4.x) の KGP をサポートし、動作確認済みはリポジトリが固定する版 (`android/gradle/libs.versions.toml` の `kotlin`、現在 2.4.10)」と宣言する。SwiftPM import は Kotlin 2.4.0 で導入された Alpha 機能で、消費側 KGP の最低版とメタデータ形式の互換は公式に記述がない (公式ページの「2.4.20-RC3」は試用の推奨版であり最低要件ではない)。互換保証のない機能に広い範囲を約束せず、完全一致に絞って patch 更新のたびに非サポートにもしない中間として同 minor を採る。次の minor が出たら phase-8 の消費者検証を回して宣言を広げる。確認済み版は docs-refresh の追従対象とし、Skill (`skills/*/ksdialogs-kmp/SKILL.md`) の「利用側で対応する Kotlin の下限は未確定」の文をこの宣言で置き換える。cross/ADR-0008 の見直し条件 (Stable 化 / メタデータの破壊的変更) は据え置く。integrateLinkagePackage の利用者手順は concepts (kmp/api/ios-host-integration.md) と Skill の 3 手順 (Maven 1 点 → `XCODEPROJ_PATH` 付きで 1 回実行 → Xcode に `KsDialogs-SPM` を追加) が PoC と一致しており、phase-7 で変える要素はない。

- 却下: 下限 2.4.0 のみで上限なし (2.4.0〜確認済み版の間の互換を確認していないのに約束する) / 確認済み版との完全一致のみ (patch 更新ごとに非サポートになり実用に耐えない)

### A5 version の導出式は kmp/build.gradle.kts にも同じものを書き、発行物の比較で揃いを検算 (2026-09-08)

phase-5 design Decision 3 の導出式 (`-Pversion=` の注入値があればそれ、無ければカタログ `ksdialogs` の SNAPSHOT 値。注入値は `X.Y.Z` / `X.Y.Z-{alpha|beta|rc}.N` 以外を失敗) と SNAPSHOT 中の Central 発行ガードを、`kmp/build.gradle.kts` (A1 で新設) にもう 1 度書く。android 側との対応はコメントで明記する。この version 値が `:ksdialogs-kmp` の version・`jp.kamusoft:ksdialogs-core` への依存版・A2 の Swift 参照導出 (SNAPSHOT 判定と exact 値) の共通の入力になる。別ビルドルートで monorepo ルートに共通ビルドファイルを置かない方針 (cross/ADR-0004) のもと、3 行程度の式を共有する仕組みを足すより二重定義の方が軽く、ずれは dry-run で両ビルドの発行物の version と kmp POM の依存版が同一文字列であることを受け入れ条件にして検出する。Sample (`samples/kmp/shared`) の `ksdialogs-kmp:0.1.0` の直書きは置換されるため実害はないが、カタログ値に揃えて版の置き場を 1 箇所に保つ。

- 却下: `android/gradle/` 配下の共有スクリプトを両ルートから `apply(from=)` (phase-5 の design 改訂が要り、kts の `apply(from=)` は型安全でなく制約が多い) / 規約プラグイン用 included build の新設 (3 行の式に対して過剰、可逆性も低い)

### A6 Skill の依存宣言は公開座標で解決でき、スコープは `api` (2026-09-08)

`jp.kamusoft:ksdialogs-kmp:<version>` は A1 の vanniktech 経由で Maven Central へ発行され、消費者は既定の `mavenCentral()` だけで解決できる。Android Native (`ksdialogs-core`) は POM の推移的依存、iOS の Swift 参照は A2 の metadata で自動 (PoC 項目 1・2)。追加のリポジトリ宣言は不要で、設計変更はない。Skill の宣言は `api(...)` が正 (現状の `implementation` は誤り): 消費者の共有 ViewModel は `DialogViewModel` を継承するため、Android アプリ側の登録コードに KsDialogs の型が見える必要がある。phase-7 の受け入れ条件は `publishToMavenLocal` の発行物 (POM の座標・依存・Module Metadata の SwiftPM バリアント) まで、実解決は phase-8 (dry-run = mavenLocal / smoke = Central) が担う。Skill の追従 3 点 (依存スコープ・Kotlin 範囲 (A4)・「予定している公開 coordinate」の状態表記) は change 完了後に docs-refresh で行う (skills/ を change 内で直接編集しない)。

### B `@Throws` 宣言は androidHostTest の反射テストで回帰から守る (2026-09-08)

commonMain 契約の失敗しうる 4 経路 (`KsDialog.show`・`KsLoading.show`・`KsLoading.start`・`KsToast.show`) について、JVM メソッドの throws 節に `DialogException` があることを肯定検査し、それ以外の公開関数 (suspend を含む) に throws 節がないことを否定検査するテストを `kmp/ksdialogs-kmp/src/androidHostTest` に 1 本置く (concepts core/api/result-notification-semantics.md ルール 5 の線引きをそのまま検査にする)。`@Throws` は commonMain の宣言 1 つで JVM では throws 節、Native では Swift の `throws` に変換されるため、JVM 側の throws 節の消失は共通宣言の欠落と同値。ホスト JVM で走り、本体検証 CI (`kmp / verify`) で退行を検出する。既存の ObjC ヘッダ検査 (`ObjCApiSurfaceTests`) は suspend 関数の completionHandler が `@Throws` の有無に関わらず `NSError` を持つため (生成ヘッダの実物で確認: `setMessage` と `show` が同形)、4 経路中 1 経路 (非 suspend の `KsToast.show` の `error:` 引数) しか区別できず採らない。

- 却下: `api-surface-check` への組み込み (コンパイル検査は `@Throws` の有無に反応しない) / 検査なし・evidence のみ (宣言が落ちても Kotlin のビルド・テストは緑のままで、利用者の Swift コードが abort して初めて分かる)

### C1 dry-run は Maven を mavenLocal、Swift は両参照を `file://` + exact に揃える (2026-09-08)

KMP 消費者 (Android app + iOS app、実装は phase-8) の dry-run で、Maven は翻案元どおり `mavenLocal()` を `exclusiveContent` で `jp.kamusoft` に排他割り当てし (smoke は `mavenCentral()`)、Swift 参照は `path:` ではなく「スナップショットのローカル clone への `file://` URL + exact(version)」を使う。iOS アプリの xcodeproj は mode で変えず、(a) 消費者の shared framework、(b) 生成される linkage package (固定パス)、(c) README の最小例を包むローカル Swift package `VerificationApp` (固定パス) の 3 つだけを参照する。(c) の `Package.swift` だけを翻案元 (KsSettingsView `verification/ios/Package.swift.template`) のテンプレート方式で生成し、依存 1 行を dry-run は `file://` + exact、smoke は `https://github.com/kamusoft/KsDialogs-SPM` + exact にする。dry-run のフィード準備は翻案元の prepare-feed (origin を設定した作業コピーへ同期) に commit + tag (push なし) を足し、その clone の URL を A2 の上書きプロパティで kmp/ に注入して `publishToMavenLocal` する。これで linkage package と (c) が同一 URL を指し、1 つの pin にデデュープされる (PoC 項目 4 の実測。PoC の擬似リモートも `file://` の bare clone)。

- 却下: アプリ側だけ翻案元どおり `path:` (git 種別と ローカル種別が同じ identity でぶつかる懸念があり未検証) / dry-run で配信リポジトリへ一時 tag を push して https で解決 (A2 で却下した運用と同じ)

### C2 検証範囲は「解決 + Release ビルド」3 段、起動・実機は含めない (2026-09-08)

翻案元と同じ範囲で、KMP では (1) Android app が `ksdialogs-kmp` を解決して `assembleRelease`、(2) 消費者の Gradle で `linkReleaseFrameworkIosSimulatorArm64` (metadata から linkage package が再生成される段、PoC 項目 2)、(3) `xcodebuild` の Release ビルド (`generic/platform=iOS Simulator`、署名なし) で linkage package と `VerificationApp` package をリンクし README の最小例 (登録 API 呼び出し) をコンパイル・リンクする、の 3 段を通す。主眼は (2)・(3) で、A2 の参照導出と A4 の Kotlin 範囲宣言はこの経路でしか検証できない ((2) は消費者の KGP 版で走るため確認済み版の実証を兼ねる)。挙動は本体の検証 CI と Sample が担う。

- 却下: Xcode を回さず (1)・(2) のみ (linkage package が生成されるだけでリンクされず主眼が抜ける) / シミュレータ起動まで (起動・撮影の仕組みが翻案元になく、消費者検証の目的を超える)

### C3 release の 4 本目 — phase-9 への申し送り (2026-09-08)

翻案元の段構成 (validate → test ∥ package → dry-run → publish → 反映待ち → smoke) に KMP を足すときの制約を phase-7 の結論として渡す。

| 段 | KMP の扱い |
|---|---|
| package | kmp/ を `-Pversion=<version>` で `publishToMavenLocal` した成果物 (Swift 参照は既定の https + exact) を artifact に上げる。android/ と kmp/ は別ビルドのため Central への upload は deployment 2 件 (`ksdialogs-core` + `ksdialogs` / `ksdialogs-kmp`)。vanniktech の upload は Gradle ビルド単位で 1 deployment を作り、1 件への同梱は手動 bundle upload が要るため、`central-portal.sh` を 2 件分回す。「両方 validated になるまでどちらも release しない」を publish 段の直列 job で守る |
| dry-run | `consumer-kmp` は package 段の kmp artifact をそのまま使えない (metadata の Swift 参照が https + exact で、その tag は publish 段まで存在しない)。dry-run job 内で kmp/ を A2 の URL 上書き (`file://` のスナップショット clone、C1) 付きで `publishToMavenLocal` し直す。Android 側は package 段の artifact を使う。再ビルドの同一性検査 (`compare-maven-artifacts.sh`) は package 段と publish 段の成果物を比べ、dry-run 用の成果物は対象に入れない |
| publish | 翻案元の順 (Maven upload 保留 → NuGet push → Maven release → SPM tag → monorepo tag) を維持。KMP artifact は SPM tag を exact で指すため Maven release から SPM tag push までの数分は tag 未存在の窓が開くが、利用者の導入は GitHub Release 以後で実害はなく、取り消せない操作を後ろに置く方を優先する |
| smoke | `consumer-kmp` を `mavenCentral()` + https + exact(version) で回す。反映待ちに SPM tag (`check-distribution-tag.sh`) と `ksdialogs-kmp` の `repo1.maven.org` HEAD を足す |

- 却下: SPM tag を Maven release より前に push する順序 (Maven release が失敗すると配信リポジトリに tag だけが残り、削除運用が要る)

前提として確定済みのもの: 配布単位は Maven 1 点 + iOS アプリ側 SwiftPM 1 点 (cross/ADR-0008)、Swift 向け登録 API は Swift パッケージ側 (kmp/ADR-0003・0004)、KMP → Swift は `exact(同版)` (cross/ADR-0009)、Swift 参照先は配信リポジトリ (cross/ADR-0008 (2026-09-04 改訂))。PoC (library-foundation phase-10 `artifacts/poc-swiftpm-remote-distribution.md`) でリモート参照時の Kotlin SwiftPM 連携は全 4 項目成立、スパイクブランチ `spike/phase-10-packaging-poc` は参考実装 (本実装は書き直す)。

## TODO

- [x] 論点の解消 (publication の形・参照切替・Kotlin 範囲・`@Throws` 検査・消費者検証の形)
- [ ] change 完了後に docs-refresh を明示依頼し、KMP Skill の依存スコープ (`api`)・Kotlin サポート範囲 (A4)・coordinate の状態表記を追従させる
- [ ] ksn-propose で変更提案を起こす
