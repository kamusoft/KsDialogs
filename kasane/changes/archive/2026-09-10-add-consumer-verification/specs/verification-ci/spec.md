# verification-ci デルタスペック

## ADDED Requirements

### Requirement: 消費者検証 workflow の再利用契約
消費者検証の workflow は形態ごと (ios / android / maui / kmp) の 4 本を `workflow_call` で呼び出し可能 SHALL とし、入力としてモード (`dry-run` / `smoke`、必須)、version (任意。`smoke` では必須)、外部で準備した配布物の artifact 名 (任意。`dry-run` 専用) を受け取る。artifact 名が与えられればその配布物を取得して消費者ビルドだけを行い (KMP は Android 側の発行物として扱い、kmp/ の発行とスナップショット clone の tag は job 内で行う)、無ければ job 内でフィード準備から行う。入力の検査は checkout より前に行い、`smoke` と artifact の同時指定は拒む。モードと artifact の組み合わせごとの動きは次のとおり: `dry-run` で artifact 無し = フィード準備をすべて job 内で行う / `dry-run` で artifact 有り = artifact を参照先にしてフィード準備を行わない (KMP だけはスナップショット clone の tag と kmp/ の `file://` 上書き付き発行を job 内で行う) / `smoke` = artifact を拒み、フィード準備を行わず公開レジストリから解決する。他の workflow (release workflow を含む) から CI 入口を経由せず呼び出せる。job の権限は `contents: read` だけで secrets を受け取らない。status check の名前は呼び出し側の job 名と呼ばれる側の job 名の双方で固定する (`consumer-ios / verify` / `consumer-android / verify` / `consumer-maui / verify` / `consumer-kmp / verify`)。ランナーは ios / maui / kmp が `macos-26`、android が `ubuntu-24.04`。timeout は ios / android / kmp が 30 分、maui が 40 分を初期値とする。

#### Scenario: モードと version を与えた呼び出し
- **GIVEN** 消費者検証 workflow を `uses:` で参照し `mode` と `version` を渡す別の workflow
- **WHEN** その workflow が実行される
- **THEN** 当該形態の消費者検証 job が指定のモードと version で実行される

#### Scenario: artifact を与えた呼び出し
- **GIVEN** 別 job が upload した配布物の artifact 名を `dry-run` の入力に渡す呼び出し
- **WHEN** 消費者検証 job が実行される
- **THEN** job はその artifact を download して参照先とし、フィード準備 (pack / Android 側の発行 / スナップショット配置) を行わない (KMP はスナップショット clone の tag と kmp/ の発行だけを行う)

#### Scenario: smoke はフィード準備を行わない
- **GIVEN** `smoke` で `version` を渡す呼び出し
- **WHEN** 消費者検証 job が実行される
- **THEN** job はフィード準備 (pack / 発行 / スナップショット配置 / clone の tag) を行わず、公開レジストリを参照先として消費者ビルドだけを行う

#### Scenario: 不正な入力で失敗する
- **GIVEN** `mode` に許可値以外を渡す、`smoke` で `version` を省く、または `smoke` に `artifact` を渡す呼び出し
- **WHEN** 消費者検証 job が実行される
- **THEN** job は checkout とフィード準備の前に失敗として報告される

#### Scenario: 配信先への書き込み経路を持たない
- **GIVEN** 消費者検証 workflow の定義
- **WHEN** 権限と secrets の宣言を確認する
- **THEN** `permissions` は `contents: read` だけで、secrets の受け取り (`secrets:` 入力・`inherit`) が無い

## MODIFIED Requirements

### Requirement: CI の起動条件
`develop` への push と、`main` を base とする pull_request で CI SHALL 起動し、lint job は起動のたびに必ず実行される (変更パスによる除外を持たない)。`develop` 宛ての pull_request は起動条件に持たない。`main` 宛ての pull_request では本体検証 5 job (ios / android / android-instrumented / kmp / maui) と消費者検証 4 job (consumer-ios / consumer-android / consumer-maui / consumer-kmp。`dry-run` モード、version と artifact は渡さない) を常に実行する。`develop` への push では消費者検証 4 job を起動せず、ビルド・テストのいずれにも入力されないファイル (`kasane/**`・Issue テンプレート・貢献案内) だけの変更と判定されたとき本体検証 5 job をスキップし、それ以外では本体検証 5 job を実行する。同じブランチ (または同じ PR) の連続する起動では、新しい起動が走行中の古い実行を打ち切る。

#### Scenario: develop への push で本体検証と lint が起動する
- **GIVEN** ビルド・テストの入力になるファイルを含む commit が `develop` へ push された
- **WHEN** push が完了する
- **THEN** lint と本体検証 5 job がすべて実行され、消費者検証 4 job は起動しない

#### Scenario: main 宛ての PR で起動する
- **GIVEN** `main` を base とする pull_request
- **WHEN** PR が作成または更新される
- **THEN** lint・本体検証 5 job・消費者検証 4 job がすべて実行される

#### Scenario: 消費者検証は dry-run で動く
- **GIVEN** CI 入口からの消費者検証 job
- **WHEN** job が実行される
- **THEN** 配布物は検証用 version でローカル参照先から解決され、job は配信先への書き込み権限・認証情報を持たず、公開レジストリ・配信リポジトリへの書き込みは発生しない

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

### Requirement: platform workflow の再利用契約
本体検証 5 job の各 workflow は `workflow_call` で呼び出し可能 SHALL である。他の workflow (release workflow を含む) から CI 入口を経由せず単独で呼び出せる。CI 入口から呼ぶ job の status check 名は「呼び出し側 job 名 / 呼ばれた側 job 名」の形 (`ios / verify`・`android / verify`・`android-instrumented / verify`・`kmp / verify`・`maui / verify`、および消費者検証の `consumer-ios / verify`・`consumer-android / verify`・`consumer-maui / verify`・`consumer-kmp / verify`) と `lint` で固定する。

#### Scenario: 別 workflow からの呼び出し
- **GIVEN** platform 検証 workflow を `uses:` で参照する別の workflow
- **WHEN** その workflow が実行される
- **THEN** platform 検証 job が CI 入口経由と同じ内容で実行される

#### Scenario: status check 名が固定される
- **GIVEN** CI 入口からの起動
- **WHEN** 各 job が報告される
- **THEN** status check 名は上記 10 件で、workflow ファイルの変更なしには変わらない

### Requirement: lint の検証
lint job は secret scan (gitleaks、版と配布物の checksum を固定し、追跡中の内容を展開したディレクトリに対して実行)・ローカル絶対パス検査 (local-path-lint)・個体/個人/秘密情報検査 (identity-lint、検査範囲は `kasane/config.yaml` の現行 scope で `verification` を含む)・コメント規約検査 (comment-policy-lint)・仕様の Scenario ID とテスト名の網羅検査 (scenario-id-coverage)・CI 限定スキップの許可リスト検査 (ci-skip-lint、自己テストを含む)・SwiftPM スナップショット同期スクリプトの自己テスト・README 最小例と消費者ソースの一致検査 (readme-example-lint、自己テストを含む) の 8 検査を実行 SHALL し、いずれかの違反で job が失敗する。secret scan は走査対象の展開数が追跡ファイル数を下回れば検査対象不足として失敗する。

#### Scenario: 違反の検出
- **GIVEN** 検査対象範囲に違反 (秘密情報・ローカル絶対パス・個体識別子・コメント規約違反・仕様にあってテストに無い Scenario ID・承認の無い CI 限定スキップ・README 最小例の不一致のいずれか) を含む変更
- **WHEN** lint job が実行される
- **THEN** job は失敗として報告され、違反箇所が出力で特定できる

#### Scenario: ソースルート配下の識別子検出
- **GIVEN** `samples/`・`verification/`・`maui/macios/native` 配下に開発チーム識別子等の個体情報が書き込まれた変更 (Xcode の実機ビルドによる書き戻しを含む)
- **WHEN** lint job が実行される
- **THEN** identity-lint が検出し job は失敗として報告される

#### Scenario: secret scan の空振り検出
- **GIVEN** 走査対象の展開に失敗し、展開数が追跡ファイル数を下回った状態
- **WHEN** secret scan のステップが走る
- **THEN** 検出 0 件でも job は失敗として報告される

### Requirement: ツールチェーンの再現性
検証に用いるツールチェーンの版はリポジトリ内 (workflow 定義・repo 直下の `global.json`) で明示 SHALL され、ランナーイメージの既定値や親ディレクトリの設定に依存しない。固定境界は次のとおり: ランナーイメージは版指定 (`macos-26` / `ubuntu-24.04`)、Xcode はメジャー.マイナー (26.5。パッチはイメージ同梱内の変動を許容)、JDK はディストリビューション + メジャー (Temurin 17)、.NET SDK と workload set は `global.json` の完全指定 (10.0.300 / 10.0.300.3)、MAUI 本体 (`Microsoft.Maui.Controls`) は workload set が同梱する版 (現在 10.0.20。maui/ADR-0004) を `maui/Directory.Packages.props` で一元宣言、Emulator の API レベルは 36。Xcode の選択は iOS を扱う全 job (ios / kmp / maui と消費者検証の consumer-ios / consumer-maui / consumer-kmp) に適用する。消費者検証の Gradle (AGP / Kotlin) は本体のバージョンカタログ `android/gradle/libs.versions.toml` を共有し、MAUI 消費者は `Microsoft.Maui.Controls` の版を書かず workload の版に従う。外部 action の参照は commit SHA で固定し、全 workflow の `permissions` は `contents: read` に限る。

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

#### Scenario: 消費者の Kotlin 版が本体と一致する
- **GIVEN** 消費者検証 (android / kmp) の Gradle ビルド
- **WHEN** Kotlin Gradle Plugin の版を確認する
- **THEN** 本体のカタログの `kotlin` (動作確認済み版、現在 2.4.10) と同じ版が使われている
