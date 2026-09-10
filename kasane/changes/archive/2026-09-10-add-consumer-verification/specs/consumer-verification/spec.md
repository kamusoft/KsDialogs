# consumer-verification デルタスペック

## ADDED Requirements

### Requirement: 消費者プロジェクトの構成
リポジトリは、配布物を利用者と同じ経路で参照する消費者プロジェクトを `verification/` 配下に形態ごと (iOS / Android / MAUI / KMP) に持つ SHALL。iOS は SwiftPM パッケージ、Android は 2 つの application module (Compose 系 `jp.kamusoft:ksdialogs` 1 行だけを参照するものと、View 系本体 `jp.kamusoft:ksdialogs-core` 1 行だけを参照するもの) を持つ 1 つの Gradle プロジェクト、MAUI は facade `KsDialogs.Maui` 1 行だけを参照する MAUI アプリ、KMP は共有モジュールの commonMain に `jp.kamusoft:ksdialogs-kmp` 1 行だけを宣言する共有モジュール + Android アプリ + iOS アプリ (Xcode project) の 1 つの Gradle プロジェクトである。いずれも本体のソースを参照せず、パッケージ (SwiftPM product / Maven artifact / NuGet パッケージ) だけを参照する。各消費者は、ルート README (英語) の当該形態の最小コード例 (iOS / Android / MAUI / KMP の 4 コードブロック) をソースとして含む。Android の最小例は 2 module が共有する 1 つのソースディレクトリに置く。Android と KMP の消費者 (Gradle build root) は Android SDK の位置を環境変数 (`ANDROID_HOME` / `ANDROID_SDK_ROOT`) から、無ければ本体 build root の `android/local.properties` の `sdk.dir` から引き継ぎ、`verification/` 配下に `local.properties` を生成しない。

#### Scenario: 本体ソースへの参照を持たない
- **GIVEN** `verification/` 配下のいずれかの消費者プロジェクト
- **WHEN** その依存宣言を確認する
- **THEN** 本体 (`ios/` / `android/` / `maui/` / `kmp/`) へのローカルソース参照 (Local Swift Package・composite build・ProjectReference) は存在せず、公開座標 (product `KsDialogs` / `jp.kamusoft:ksdialogs` / `jp.kamusoft:ksdialogs-core` / `KsDialogs.Maui` / `jp.kamusoft:ksdialogs-kmp`) への参照だけがある

#### Scenario: README の最小例がそのままビルド対象になる
- **GIVEN** ルート README の 4 コードブロック
- **WHEN** 各消費者プロジェクトをビルドする
- **THEN** 最小例のコードがコンパイル対象に含まれ、例が壊れていればビルドが失敗する

#### Scenario: KMP のホスト側は利用者向け手順の 3 点だけを参照する
- **GIVEN** KMP 消費者の iOS アプリ
- **WHEN** その Xcode project の参照を確認する
- **THEN** 共有モジュールの static framework、発行 metadata から生成される linkage package、Swift 側の登録 API を呼ぶローカル Swift package の 3 点だけを参照し、`ios/` のソースや monorepo 内のパスを参照しない

#### Scenario: 追跡している生成物は実行で変化しない
- **GIVEN** 追跡されている KMP 消費者の linkage package と iOS アプリ側のローカル Swift package のマニフェスト (Swift 参照は配信リポジトリの https + exact)
- **WHEN** dry-run または smoke の消費者ビルドを実行する
- **THEN** 再生成は作業ディレクトリ内のコピーで行われ、リポジトリ内の `verification/kmp/` には差分が生じない

#### Scenario: 本体 build root の SDK 設定だけで消費者が動く
- **GIVEN** `ANDROID_HOME` / `ANDROID_SDK_ROOT` が未設定で、`android/local.properties` の `sdk.dir` だけで本体がビルドできる環境
- **WHEN** Android または KMP の消費者検証を実行する
- **THEN** 消費者の Gradle ビルドは同じ SDK を解決して成功し、`verification/` 配下に `local.properties` は作られない。どちらも無ければフィード準備の前に理由を出して失敗する

### Requirement: モードと version の指定
各消費者の検証は、モード (`dry-run` / `smoke`) と version を外部から受け取って動作する SHALL。`dry-run` で version の指定がなければ、4 形態に共通の検証用 version (リリースしない値で、本体が注入値として受け付ける形式に適合する) で動作し、iOS はスナップショット参照のため version を持たない。`smoke` では version が必須である。許可値以外のモード、および `smoke` で version が無い場合は、フィード準備や依存解決に入る前に失敗する。消費者のソース (README 最小例を含む) はモードによって変わらない。

#### Scenario: 引数なしで dry-run が動く
- **GIVEN** 手元の作業環境で本体がビルドできる状態
- **WHEN** 引数を与えずに消費者検証を実行する
- **THEN** dry-run として、検証用 version の配布物をローカルの参照先から解決してビルドする

#### Scenario: version を与えると全形態に同じ文字列が流れる
- **GIVEN** version `X.Y.Z-rc.1` を指定した実行
- **WHEN** 各形態の消費者の参照設定が生成され (dry-run では続けて依存が解決され) る
- **THEN** iOS の生成した `Package.swift` の `exact:` (smoke 形)、Android と KMP の依存座標と KMP の Swift 参照の `exact`、MAUI の `PackageReference` のいずれも同じ `X.Y.Z-rc.1` を用い、dry-run の解決結果では KMP artifact が推移的に要求する本体 (`ksdialogs-core`) の version も同じ文字列である

#### Scenario: 不正な入力は早期に失敗する
- **GIVEN** モードに `dry-run` / `smoke` 以外の値を与えた、`smoke` で version を省いた、または `smoke` に準備済みの参照先を与えた実行
- **WHEN** 消費者検証を起動する
- **THEN** フィード準備・依存解決を行わずに失敗として報告され、理由が出力で分かる

### Requirement: dry-run の参照先
`dry-run` では、本リポジトリ由来の配布物をローカルの参照先だけから解決する SHALL。参照先は iOS がスナップショット (配信リポジトリのルートと同じファイル配置で、identity が `KsDialogs-SPM` になるディレクトリ) への `path:` 参照、Android と KMP が実行ごとに空から作る作業ディレクトリ内のローカル Maven リポジトリ、MAUI がローカルフォルダフィードである。KMP の Swift 参照 (発行 metadata と消費者の iOS アプリ側の依存) は、スナップショットを同期して commit し version と同名の tag を打ったローカル clone の `file://` URL + exact(version) であり、両者は同一の URL を指す。ローカル参照先は本リポジトリ由来の座標 (group `jp.kamusoft` / `KsDialogs.*` / product `KsDialogs`) について排他的であり、そこに無ければ公開レジストリやユーザー環境のキャッシュ (`~/.m2` を含む) へフォールバックせず失敗する。MAUI では実行ごとに空のパッケージ展開先を使い、既存のユーザーキャッシュを参照しない。本リポジトリ由来以外の依存 (AndroidX・Kotlin・MAUI 本体等) は通常の公開リポジトリから取得してよい。dry-run は配信リポジトリ・Maven Central・NuGet.org に対して書き込み (tag の push・upload・push) を行わない。

#### Scenario: 本リポジトリ由来の座標はローカル参照先からのみ取得される
- **GIVEN** dry-run の実行
- **WHEN** 依存解決が完了する
- **THEN** product `KsDialogs` / `jp.kamusoft:ksdialogs` / `jp.kamusoft:ksdialogs-core` / `jp.kamusoft:ksdialogs-kmp` / `KsDialogs.*` の取得元 (パッケージ単位の記録) はすべてローカル参照先であり、公開レジストリからは取得されていない

#### Scenario: ローカル参照先に無ければ公開済みの版でも失敗する
- **GIVEN** ローカル参照先に指定 version の配布物が置かれておらず、同じ version が公開レジストリまたはユーザー環境のキャッシュに存在する状態
- **WHEN** dry-run を実行する
- **THEN** 依存解決は失敗として報告され、公開レジストリやキャッシュの同名パッケージへフォールバックしない

#### Scenario: KMP の Swift 参照がローカル clone を指す
- **GIVEN** KMP の dry-run の実行
- **WHEN** フィード準備と消費者ビルドが完了する
- **THEN** 参照先に置かれた KMP artifact の SwiftPM 連携 metadata の Swift 参照、再生成された linkage package の依存、消費者の iOS アプリ側のローカル Swift package の依存は、いずれも同じ `file://` URL と exact(version) を持ち、`xcodebuild` の解決結果に配信リポジトリの pin は 1 つだけ現れる

#### Scenario: 配信先へ副作用を残さない
- **GIVEN** dry-run の実行
- **WHEN** 実行が完了する
- **THEN** 配信リポジトリの tag 一覧・Maven Central の deployment・NuGet.org のパッケージ一覧は実行前と同一であり、dry-run の実行経路は配信先への書き込み権限・認証情報を持たない

### Requirement: smoke の参照先
`smoke` では、本リポジトリ由来の配布物を公開レジストリ (配信リポジトリ `KsDialogs-SPM` の tag、Maven Central、NuGet.org) から解決する SHALL。KMP の Swift 参照は発行 metadata の既定 (`https://github.com/kamusoft/KsDialogs-SPM` + exact(version)) と消費者の iOS アプリ側の依存が同一の URL を指す。消費者側の座標・identity の文字列は dry-run と同一である。公開レジストリから当該 version を解決してビルドが成功すること (smoke の正ケース) は本変更の受け入れ条件に含めず、配布物が公開される初回リリース (phase-9 の release workflow) の変更で Scenario として立てて実証する。

#### Scenario: 参照先が公開レジストリを指す
- **GIVEN** smoke で version を指定した実行
- **WHEN** 各消費者の依存解決の設定が生成される
- **THEN** iOS と KMP の Swift 参照は `https://github.com/kamusoft/KsDialogs-SPM` + 指定 version、Android と KMP の Maven は Maven Central、MAUI は nuget.org を指し、ローカル参照先を含まない

### Requirement: フィード準備と消費者ビルドの分離
消費者検証の実行手段は「フィード準備」(スナップショット配置 / ローカル Maven リポジトリへの発行 / NuGet の pack / KMP はスナップショット clone の tag と android/・kmp/ の発行) と「消費者ビルド」を分けて呼び出せる SHALL。消費者ビルドは、外部で準備済みの配布物 (手元のディレクトリ、または CI の artifact として渡された release workflow の package 段の成果物) を与えられて動作でき、その場合フィード準備を再実行しない。KMP では準備済みの配布物は Android 側 (`ksdialogs-core` / `ksdialogs`) の発行物であり、kmp/ の発行 (Swift 参照を `file://` に上書きしたもの) とスナップショット clone の tag は準備済みの配布物が与えられても行う。準備済みの配布物の配置 (形態ごとのルート構造) は design.md の表に従う。

#### Scenario: 外部で準備した配布物を消費者に渡す
- **GIVEN** 別の工程で pack / 発行 / 配置された配布物
- **WHEN** フィード準備を行わず、その配布物の場所 (または artifact) を指定して消費者ビルドだけを実行する
- **THEN** 消費者はその配布物を解決してビルドし、フィード準備を再実行しない (KMP は kmp/ の発行と clone の tag だけを行い、Android 側の発行を再実行しない)

### Requirement: 消費者ビルドの成立条件
各消費者は Release 構成でビルドが成功する SHALL。対象は iOS (iOS Simulator 向け)、Android (2 つの application の release variant)、MAUI (`net10.0-android` の Release と、`net10.0-ios` の Simulator RID を明示した Release)、KMP (Android アプリの release variant、共有モジュールの iOS Simulator 向け Release framework のリンク、iOS アプリの iOS Simulator 向け Release ビルド、の 3 段を順に) である。署名情報 (provisioning・証明書) を要求せず、Simulator / Emulator / 実機での起動、`dotnet publish`、実機向け署名は検証範囲に含めない。

#### Scenario: 4 形態の Release ビルド
- **GIVEN** dry-run または smoke で依存解決が完了した消費者
- **WHEN** Release 構成でビルドする
- **THEN** 4 形態すべてで署名情報なしにビルドが成功し、いずれかの失敗 (KMP の 3 段のいずれかを含む) は検証全体の失敗として報告される

#### Scenario: KMP の 3 段は順に通る
- **GIVEN** KMP 消費者の依存解決が完了した状態
- **WHEN** 消費者ビルドを実行する
- **THEN** Android アプリの release variant、共有モジュールの iOS Simulator 向け Release framework のリンク (linkage package の再生成を含む)、iOS アプリの Release ビルド (linkage package と登録 API の Swift package をリンク) の順に実行され、前段の失敗で後段には進まない

### Requirement: Android 消費者の依存検査
Android 消費者の検証は、Compose 系 app の release runtime classpath で `jp.kamusoft:ksdialogs-core` が `jp.kamusoft:ksdialogs` と同じ version で推移的に解決されたこと、および View 系本体だけの app の release runtime classpath に `androidx.compose` の座標が含まれないことを検査する SHALL。不一致・混入は失敗とする。

#### Scenario: 推移の本体が同版で届く
- **GIVEN** Compose 系 app の依存解決が完了した状態
- **WHEN** 依存検査が走る
- **THEN** `ksdialogs-core` の解決版が `ksdialogs` の解決版と一致し、両者の行が出力で確認できる

#### Scenario: 本体だけの消費者に Compose が届かない
- **GIVEN** View 系本体だけの app の依存解決が完了した状態
- **WHEN** 依存検査が走る
- **THEN** release runtime classpath に `androidx.compose` の座標は 1 つも無く、検査は成功として報告される

### Requirement: MAUI 消費者の依存検査
MAUI 消費者の restore は、ダウングレード (NU1605)・依存版範囲外 (NU1608)・版競合 (NU1107) の警告を失敗として扱う SHALL。あわせて、解決された `KsDialogs.Binding.iOS` / `KsDialogs.Binding.Android` の version が facade `KsDialogs.Maui` の version と一致すること、および platform TFM (`net10.0-android` / `net10.0-ios`) の解決結果に binding のアセンブリが platform 固有のアセットとして含まれる (platform 中立アセットへのフォールバックでない) ことを検査し、不一致・不在は失敗とする SHALL。ビルド警告全般 (XA4301 等) は失敗にしない。AndroidX 等の推移依存の解決版は検査しない。

#### Scenario: 依存警告で失敗する
- **GIVEN** 依存の版競合またはダウングレードが起きる構成
- **WHEN** MAUI 消費者を restore する
- **THEN** restore は失敗として報告される

#### Scenario: binding の version 不一致を検出する
- **GIVEN** 参照先に facade と異なる version の binding しか存在しない状態
- **WHEN** MAUI 消費者の依存検査が走る
- **THEN** 検査は失敗として報告され、facade と binding それぞれの解決版が出力で確認できる

#### Scenario: platform 中立アセットへのフォールバックを検出する
- **GIVEN** 消費者の `TargetPlatformVersion` がパッケージの API 版付き TFM を下回り、binding が platform 固有アセットとして入らない構成
- **WHEN** MAUI 消費者の依存検査が走る
- **THEN** 検査は失敗として報告され、binding のアセットが platform TFM に無いことが出力で分かる

#### Scenario: ビルド警告は失敗にしない
- **GIVEN** restore に警告がなく、ビルドで警告だけが出る状態
- **WHEN** MAUI 消費者を Release ビルドする
- **THEN** ビルドは成功として報告される

### Requirement: KMP 消費者の依存検査
KMP 消費者の検証は、検査の自己テストで各判定が負の入力 (publication の欠落・版の不一致・metadata の不一致・URL の不一致・pin の 0 件と 2 件) を失敗と判定することを示したうえで、共有モジュールの iOS 3 ターゲット (iosArm64 / iosSimulatorArm64 / iosX64) と Android アプリの依存解決で `jp.kamusoft:ksdialogs-kmp` の全 publication (root / android / iOS 3 ターゲット) が参照先から解決されたこと、Android アプリで推移の `jp.kamusoft:ksdialogs-core` が `ksdialogs-kmp` と同じ version であること、参照先に置かれた KMP artifact の SwiftPM 連携 metadata の Swift 参照がモードどおりの URL と version と同じ `exact` を持つことを検査する SHALL。不足・不一致は失敗とする。

#### Scenario: 全 publication が解決される
- **GIVEN** KMP 消費者の依存解決が完了した状態
- **WHEN** 依存検査が走る
- **THEN** Android アプリと iOS 3 ターゲットの解決結果に、それぞれ対応する publication の `ksdialogs-kmp` が指定 version で現れ、`ksdialogs-core` も同じ version である

#### Scenario: 検査は負の入力で失敗する
- **GIVEN** iOS publication を 1 件欠いた解決結果、`ksdialogs-core` が別版の解決結果、URL / exact / deployment target のいずれかが異なる metadata、URL が不一致な 2 つの `Package.swift`、pin が 0 件または 2 件の `xcodebuild` 出力のいずれか
- **WHEN** KMP の依存検査 (自己テスト) にそれを与える
- **THEN** 検査は失敗として報告し、どの判定が失敗したかが出力で分かる

#### Scenario: Swift 参照がモードと一致する
- **GIVEN** 参照先に置かれた KMP artifact
- **WHEN** SwiftPM 連携 metadata を検査する
- **THEN** Swift 参照の URL は dry-run ではローカル clone の `file://`、smoke では `https://github.com/kamusoft/KsDialogs-SPM` であり、`exact` は指定 version と同一で、deployment target は `17.0` である

### Requirement: 解決結果の証跡
各消費者の検証は、本リポジトリ由来の配布物について解決版と取得元 (ローカル参照先か公開レジストリか) を実行ログまたは出力ファイルとして残す SHALL。iOS は dry-run では生成したマニフェストと依存グラフの表示 (path 参照には version が無いことを含む)、smoke では `Package.resolved` の URL・revision・version。Android は 2 module の依存ツリーの `jp.kamusoft` 行と `-core` 側の `androidx.compose` 不在の判定。MAUI はパッケージ単位の解決版・取得元・platform TFM のアセット。KMP は Android アプリと iOS 3 ターゲットの解決行、metadata の Swift 参照、linkage package と iOS アプリ側の依存 URL、`xcodebuild` の解決結果の pin。CI では job summary で確認できる。

#### Scenario: 解決版と取得元が読める
- **GIVEN** 消費者検証の実行
- **WHEN** 実行が完了する
- **THEN** 4 形態について、本リポジトリ由来の配布物の解決版 (iOS dry-run は path と identity) と取得元が出力から特定できる

### Requirement: README 最小例との一致
ルート README (英語) の「Minimal examples」節の 4 コードブロック (iOS / Android / MAUI / KMP) と、`verification/` 配下の対応する 4 ソースファイルは完全一致する SHALL。一致は lint で検査し、不一致・対応するコードブロックの不在・重複は失敗とする。`README_ja`、README に無い KMP のホスト側 (iOS / Android) の登録コード、および `skills/` の例は対象外 (英日同期と Skill の追従は docs-refresh の責務)。

#### Scenario: 例の変更が消費者に追随していなければ失敗する
- **GIVEN** README の最小例と消費者のソースのどちらか一方だけが変更された状態
- **WHEN** lint を実行する
- **THEN** 不一致のコードブロックとファイルが出力され、lint は失敗として報告される

#### Scenario: 一致していれば通る
- **GIVEN** 4 コードブロックと 4 ファイルが一致している状態
- **WHEN** lint を実行する
- **THEN** lint は成功として報告される
