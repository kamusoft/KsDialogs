# verification-ci デルタスペック

## ADDED Requirements

### Requirement: CI の起動条件
`develop` への push と、`main` を base とする pull_request で CI SHALL 起動し、lint job は起動のたびに必ず実行される (変更パスによる除外を持たない)。`develop` 宛ての pull_request は起動条件に持たない。`main` 宛ての pull_request では本体検証 5 job (ios / android / android-instrumented / kmp / maui) を常に実行する。`develop` への push では、ビルド・テストのいずれにも入力されないファイル (`kasane/**`・Issue テンプレート・貢献案内) だけの変更と判定されたとき本体検証 5 job をスキップし、それ以外では全 job を実行する。同じブランチ (または同じ PR) の連続する起動では、新しい起動が走行中の古い実行を打ち切る。

#### Scenario: develop への push で本体検証と lint が起動する
- **GIVEN** `develop` へ push された commit
- **WHEN** push が完了する
- **THEN** lint と本体検証 5 job がすべて実行される

#### Scenario: main 宛ての PR で起動する
- **GIVEN** `main` を base とする pull_request
- **WHEN** PR が作成または更新される
- **THEN** lint と本体検証 5 job がすべて実行される (消費者検証 job は本変更の範囲外)

#### Scenario: 記録だけの push では lint だけが走る
- **GIVEN** `kasane/` 配下のファイルだけを変更した commit
- **WHEN** `develop` へ push される
- **THEN** lint job は実行され、本体検証 5 job はスキップされる (スキップは成功扱いで、失敗ではない)

#### Scenario: 記録とソースが混ざった push では全 job が走る
- **GIVEN** `kasane/` 配下とソース (例: `ios/`) の両方を変更した commit
- **WHEN** `develop` へ push される
- **THEN** lint と本体検証 5 job がすべて実行される

#### Scenario: 連続する push で古い実行が打ち切られる
- **GIVEN** `develop` の検証が走行中
- **WHEN** 同じブランチへ次の push が行われる
- **THEN** 走行中の実行は打ち切られ、新しい push に対する実行だけが残る

### Requirement: main 宛て PR の head 制限
`main` を base とする pull_request は、head が自リポジトリの `develop` である場合にのみ lint job が成功 SHALL する。fork の同名ブランチや他のブランチからの PR は lint job が失敗する。

#### Scenario: develop 以外からの PR は失敗する
- **GIVEN** `main` を base とし、head が `develop` 以外 (または fork の `develop`) の pull_request
- **WHEN** lint job が実行される
- **THEN** lint job は head 制限の違反として失敗し、理由が出力で分かる

### Requirement: platform workflow の再利用契約
本体検証 5 job の各 workflow は `workflow_call` で呼び出し可能 SHALL である。他の workflow (release workflow を含む) から CI 入口を経由せず単独で呼び出せる。CI 入口から呼ぶ job の status check 名は「呼び出し側 job 名 / 呼ばれた側 job 名」の形 (`ios / verify`・`android / verify`・`android-instrumented / verify`・`kmp / verify`・`maui / verify`) と `lint` で固定する。

#### Scenario: 別 workflow からの呼び出し
- **GIVEN** platform 検証 workflow を `uses:` で参照する別の workflow
- **WHEN** その workflow が実行される
- **THEN** platform 検証 job が CI 入口経由と同じ内容で実行される

#### Scenario: status check 名が固定される
- **GIVEN** CI 入口からの起動
- **WHEN** 各 job が報告される
- **THEN** status check 名は上記 6 件で、workflow ファイルの変更なしには変わらない

### Requirement: iOS の検証
ios job は iOS Simulator 上でパッケージ全体のテストを実行 SHALL し、テストの失敗で job が失敗する。macOS ホスト上の `swift test` を成否判定に用いてはならない。実行件数は Swift Testing の件数行 (`Test run with N tests`) と XCTest の件数行 (`Executed N tests`) の 2 系統を合算 SHALL し、合算が 0 件、またはどちらの件数行も無ければ job は失敗する。

#### Scenario: Simulator 全件実行
- **GIVEN** ios job の実行
- **WHEN** テストが実行される
- **THEN** iOS Simulator destination で全テストターゲットが実行され、実行件数が job summary で確認できる

#### Scenario: 0 件実行の検出
- **GIVEN** 2 系統の合算が 0 件 (どちらの件数行も無い場合を含む) の状態
- **WHEN** 件数検査が走る
- **THEN** テスト自体が緑でも job は失敗として報告される

#### Scenario: Swift Testing だけの構成で件数が取れる
- **GIVEN** XCTest のテストが 0 件で Swift Testing のテストだけがある現行構成
- **WHEN** 件数検査が走る
- **THEN** Swift Testing の件数が合算に入り、job は成功する

### Requirement: Android の検証と実行件数の担保
android job は JVM テストを持つ全 module のユニットテスト (`test` タスクが起動する variant — 現構成では debug のみ) を毎回実行 SHALL し、実行件数を検査 SHALL する。検査は、期待する module の集合を Gradle 構成 (`android/settings.gradle.kts` の include のうち JVM テストのソースセットを持つ module) から導出し、各 module についてテスト結果 XML の欠落または `tests` 属性合計 0 のいずれでも job を失敗とする。導出した集合が空なら検査自体を失敗とする (JVM テストを持つのは現行では `:ksdialogs` だけで、`:ksdialogs-compose` は instrumented のみ、`:api-surface-check` はテストを持たない)。実行件数の合計は job summary に表示する。本体の Compose 非依存の依存グラフ検査は同じ実行に含まれ、違反で job が失敗する。

#### Scenario: 全件実行と件数表示
- **GIVEN** android job の実行
- **WHEN** テストが完了する
- **THEN** JVM テストを持つ全 module のテストが実行され、合計実行件数が job summary に表示される

#### Scenario: 0 件実行の検出
- **GIVEN** 期待する module のいずれかで、テストが 1 件も実行されなかった (結果 XML の欠落を含む) 状態
- **WHEN** 件数検査が走る
- **THEN** テスト自体が緑でも job は失敗として報告される

#### Scenario: 依存グラフ違反の検出
- **GIVEN** Android 本体に宣言的 UI (Compose) への依存が (推移的に) 混入した変更
- **WHEN** android job が実行される
- **THEN** 依存グラフ検査が失敗し job は失敗として報告される

### Requirement: Android instrumented の検証
android-instrumented job は API 36 の Emulator 1 台で instrumented テストを持つ全 module のテスト (`connectedDebugAndroidTest`) を実行 SHALL し、module ごとに実行件数を検査 SHALL する。期待する module の集合は include のうち instrumented テストのソースセットを持つ module (現行は `:ksdialogs` と `:ksdialogs-compose`) から導出し、空なら検査自体を失敗とする。結果 XML の欠落、または実行数 (`tests` − `skipped`) が 0 の module があれば job を失敗とし、module ごとに tests / skipped / failures を分けて job summary に表示する。API 29 固有の旧経路の検証は本 job の対象外で、手元の完了条件のまま handbook に残る。

#### Scenario: Emulator で全件実行
- **GIVEN** android-instrumented job の実行
- **WHEN** Emulator が起動しテストが完了する
- **THEN** `:ksdialogs` と `:ksdialogs-compose` の instrumented テストが実行され、module ごとの件数が job summary に表示される

#### Scenario: 0 件実行の検出
- **GIVEN** いずれかの module で instrumented テストの実行数 (`tests` − `skipped`) が 0 (結果 XML の欠落、全件 skip を含む) の状態
- **WHEN** 件数検査が走る
- **THEN** テスト自体が緑でも job は失敗として報告される

### Requirement: KMP の検証
kmp job は 1 回の Gradle 起動で、全ターゲットのテスト (Android host / iOS Simulator) と階層化 source set の metadata compile (`compileCommonMainKotlinMetadata` / `compileIosMainKotlinMetadata`) を実行 SHALL する。iOS Simulator のテストには ObjC 公開面の検査が含まれ、公開 API 形状の検査モジュール (`api-surface-check`) のコンパイルも同じ実行に含まれる。実行件数はターゲットごとに検査し、いずれかのターゲットで結果 XML の欠落または実行 0 件なら job を失敗とする。ターゲットごとの件数は job summary に表示する。

#### Scenario: 両ターゲットの全件実行と metadata compile
- **GIVEN** kmp job の実行
- **WHEN** Gradle 起動が完了する
- **THEN** Android host と iOS Simulator の両ターゲットでテストが実行され、metadata compile が成功し、ターゲットごとの件数が job summary に表示される

#### Scenario: metadata compile の失敗を検出する
- **GIVEN** ターゲット本体のコンパイルは通るが階層化 source set の metadata compile だけが失敗する変更
- **WHEN** kmp job が実行される
- **THEN** job は失敗として報告される

#### Scenario: 0 件実行の検出
- **GIVEN** いずれかのターゲットでテストが 1 件も実行されなかった (結果 XML の欠落を含む) 状態
- **WHEN** 件数検査が走る
- **THEN** テスト自体が緑でも job は失敗として報告される

### Requirement: MAUI の検証
maui job は facade のユニットテストを実行 SHALL し、実行件数を検査して合計 0 件なら job を失敗 SHALL とする。あわせて facade の platform TFM (net10.0-ios / net10.0-android) と binding 2 プロジェクトのビルドを成功 SHALL させ、橋渡し (native bridge) のテスト 2 本 — Android 橋渡しの JVM テストと iOS 橋渡しの Swift テスト (iOS Simulator で実行、件数は ios job と同じ 2 系統合算) — を実行 SHALL し、それぞれ実行 0 件なら job を失敗とする。3 つの件数は job summary に表示する。検証ホストの実行 (E2E) は行わない。

#### Scenario: facade テスト・橋渡しテスト・配線のコンパイル検証
- **GIVEN** maui job の実行
- **WHEN** テストとビルドが完了する
- **THEN** facade のユニットテストと橋渡しテスト 2 本が実行されて 3 つの件数が job summary で確認でき、platform TFM と binding のビルドがすべて成功している (テスト・ビルドの失敗、実行 0 件はいずれも job の失敗になる)

#### Scenario: 0 件実行の検出
- **GIVEN** facade・Android 橋渡し・iOS 橋渡しのいずれかでテストが 1 件も実行されなかった状態
- **WHEN** 件数検査が走る
- **THEN** テスト自体が緑でも job は失敗として報告される

### Requirement: lint の検証
lint job は secret scan (gitleaks、版と配布物の checksum を固定し、追跡中の内容を展開したディレクトリに対して実行)・ローカル絶対パス検査 (local-path-lint)・個体/個人/秘密情報検査 (identity-lint、検査範囲は `kasane/config.yaml` の現行 scope)・コメント規約検査 (comment-policy-lint)・仕様の Scenario ID とテスト名の網羅検査 (scenario-id-coverage) を実行 SHALL し、いずれかの違反で job が失敗する。secret scan は走査対象の展開数が追跡ファイル数を下回れば検査対象不足として失敗する。

#### Scenario: 違反の検出
- **GIVEN** 検査対象範囲に違反 (秘密情報・ローカル絶対パス・個体識別子・コメント規約違反・仕様にあってテストに無い Scenario ID のいずれか) を含む変更
- **WHEN** lint job が実行される
- **THEN** job は失敗として報告され、違反箇所が出力で特定できる

#### Scenario: ソースルート配下の識別子検出
- **GIVEN** `samples/` や `maui/macios/native` 配下に開発チーム識別子等の個体情報が書き込まれた変更 (Xcode の実機ビルドによる書き戻しを含む)
- **WHEN** lint job が実行される
- **THEN** identity-lint が検出し job は失敗として報告される

#### Scenario: secret scan の空振り検出
- **GIVEN** 走査対象の展開に失敗し、展開数が追跡ファイル数を下回った状態
- **WHEN** secret scan のステップが走る
- **THEN** 検出 0 件でも job は失敗として報告される

### Requirement: ツールチェーンの再現性
検証に用いるツールチェーンの版はリポジトリ内 (workflow 定義・repo 直下の `global.json`) で明示 SHALL され、ランナーイメージの既定値や親ディレクトリの設定に依存しない。固定境界は次のとおり: ランナーイメージは版指定 (`macos-26` / `ubuntu-24.04`)、Xcode はメジャー.マイナー (26.5。パッチはイメージ同梱内の変動を許容)、JDK はディストリビューション + メジャー (Temurin 17)、.NET SDK と workload set は `global.json` の完全指定 (10.0.300 / 10.0.300.3)、MAUI 本体 (`Microsoft.Maui.Controls`) は 10.0.70、Emulator の API レベルは 36。Xcode の選択は iOS を扱う全 job (ios / kmp / maui) に適用する。外部 action の参照は commit SHA で固定し、全 workflow の `permissions` は `contents: read` に限る。

#### Scenario: 版の変更が diff に現れる
- **GIVEN** ツールチェーンの版を固定境界の粒度で上げる必要
- **WHEN** 版を変更する
- **THEN** 変更は workflow 定義・`global.json`・csproj (または版の一元宣言) の diff として PR に現れ、固定境界の粒度で版が diff なしに変わることはない

#### Scenario: 手元のビルドが repo の設定で固定される
- **GIVEN** リポジトリを clone した開発環境 (親ディレクトリに別の `global.json` があってもよい)
- **WHEN** `maui/` で .NET SDK を解決する
- **THEN** repo 直下の `global.json` が示す SDK 10.0.300 / workload set 10.0.300.3 が使われ、既定の Xcode 26.5 で MAUI iOS がビルドできる

#### Scenario: 指定した Xcode が無ければ失敗する
- **GIVEN** ランナーイメージに固定したメジャー.マイナーの Xcode が存在しない
- **WHEN** Xcode を選択するステップが走る
- **THEN** job は失敗し、イメージ内の Xcode 一覧が出力される
