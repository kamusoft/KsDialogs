# Delta Spec: repository-docs (ルート README・貢献導線・開発者向け知識の所在)

対象能力: repository-docs — 公開リポジトリの利用者向け入口 (ルート README) と貢献者向け入口 (`.github/`)、および開発者向け知識の置き場。

## ADDED Requirements

### Requirement: README の所在

公開ドキュメント面 (リポジトリルート直下と `skills/` `ios/` `android/` `maui/` `kmp/` `samples/` の配下) に置かれる README は次に限られる SHALL: ルート `README.md` (英語) と `README_ja.md` (日本語)、`skills/README.md` と `skills/README_ja.md` (Skill 索引)、`android/layout-case-fixtures/README.md` (テスト補助コードのディレクトリ注記)。`samples/` 直下と `samples/{ios,android,maui,kmp}/` 直下に README を置かない SHALL NOT。`kasane/` と `.claude/` `.agents/` は本要件の対象外とする SHALL。

#### Scenario: 公開ドキュメント面の README 集合

- **WHEN** 公開ドキュメント面から `README*.md` を列挙する (ビルド生成物と SwiftPM の checkout 配下を除く)
- **THEN** 上記 5 枚だけが得られる

#### Scenario: 廃止した README への参照の解消

- **WHEN** `kasane/changes/archive/` とロードマップの過去記録を除く全ファイルから `samples/README.md` および `samples/*/README.md` への参照を検索する
- **THEN** 該当パスへの Markdown リンクも文字列としての言及も存在しない (未解決のリンクも失敗とする)

### Requirement: ルート README の節構成

ルート README は英日とも次の節をこの順で持つ SHALL: 冒頭の配信準備中の表記 / 概要と主な特徴 / スクリーンショット / 対応プラットフォーム / インストール / 最小コード例 / Skills / リポジトリ構成 / 貢献 / ライセンス。開発者向けのビルド手順・環境セットアップ手順・ビルドルート表を持たない SHALL NOT。「リポジトリ構成」節はディレクトリの表 (`ios/` `android/` `maui/` `kmp/` `samples/` `skills/` `assets/` `kasane/`) と `AGENTS.md` / `kasane/concepts/` へのリンクだけを持ち、`samples/` 行は 1 行の説明のみでリンクを持たない SHALL。「Skills」節は英語 README から `skills/README.md`、日本語 README から `skills/README_ja.md` へリンクする SHALL。

#### Scenario: 開発者向け手順の不在

- **GIVEN** ルート README
- **WHEN** 節の内容を調べる
- **THEN** ビルドコマンド・Android SDK の設定手順・ビルドルート表に相当する記述がなく、`samples/` 配下の文書へのリンクがない

#### Scenario: 節の順序と Skills 導線

- **GIVEN** 英日のルート README
- **WHEN** 見出しを順に読む
- **THEN** 上記の順で並び、Skills 節が言語対応する索引を指している

### Requirement: 対応プラットフォーム表

「対応プラットフォーム」節は 4 形態 (iOS Native / Android Native / .NET MAUI / Kotlin Multiplatform) を行とし、「最小 OS」「ライブラリのビルドに使った toolchain」の列を持つ SHALL。表の値は docs-refresh 3d の取得元 (android version catalog・Gradle wrapper 2 本・`ios/Package.swift`・MAUI csproj) と一致する SHALL。表の下に、利用側の下限のうち取得元から機械的に読めるもの (minSdk / compileSdk は version catalog、MAUI 本体下限は csproj の `Microsoft.Maui.Controls` 版) と「表の toolchain 版はビルドに使った版であって利用側の最小ではない」の注記を持つ SHALL。利用側の Kotlin 最小版 (Android Native / KMP とも) は決定元が無いため値を推測して書かず、「確定前 (初回リリースまでに確定)」と明記する SHALL (KMP は phase-7、Android Native はそこで同時に扱う)。KMP の SwiftPM 連携が Kotlin 側で Alpha である旨を添える SHALL。

#### Scenario: 3d 取得元との一致

- **GIVEN** ルート README の対応プラットフォーム表
- **WHEN** docs-refresh 3d の手順で取得元 4 行を読み、表の値と突き合わせる
- **THEN** すべての値が一致する

#### Scenario: 注記の内容

- **GIVEN** 表の下の注記
- **WHEN** 内容を読む
- **THEN** minSdk / compileSdk / MAUI 本体下限の値、ビルド版と利用側最小の区別、Kotlin 最小版が確定前である旨 (推測値なし)、SwiftPM 連携が Alpha である旨が読める

### Requirement: インストールと最小コード例

「インストール」節は 4 形態の依存宣言 (SwiftPM: `https://github.com/kamusoft/KsDialogs-SPM` の product `KsDialogs` / Maven: `jp.kamusoft:ksdialogs` と Compose 利用時の `ksdialogs-compose` / NuGet: `KsDialogs.Maui` / KMP: Maven `jp.kamusoft:ksdialogs-kmp` + iOS アプリ側の SwiftPM 1 点) と prerelease (`X.Y.Z-{alpha|beta|rc}.N`) の ecosystem ごとの指定方法だけを持ち、詳細な導入手順は `skills/` に委ねる SHALL。依存宣言は公開レジストリに存在する前提で書き、未配信を理由とした代替手順を持たない SHALL NOT。「最小コード例」節は 4 形態 (iOS / Android / .NET MAUI / KMP 共有コード) ごとに 1 例を置き、各例は対応する platform Skill の `SKILL.md` の最小コードブロックと byte 一致する SHALL (移行 Skill は対象外)。座標は README と各 `SKILL.md` の Setup で同一の値を示す SHALL。

#### Scenario: 導入手順の委譲

- **GIVEN** インストール節
- **WHEN** 内容を読む
- **THEN** 4 形態の依存宣言と prerelease の指定方法だけがあり、IDE での追加操作・module 構成・要件表は `skills/` を参照する案内になっている

#### Scenario: 最小コード例の一致

- **GIVEN** ルート README の最小コード例 4 つと `ksdialogs-{ios,android,maui,kmp}/SKILL.md` の最小コードブロック
- **WHEN** 形態ごとに対応する組を比較する
- **THEN** それぞれ byte 一致する

#### Scenario: 座標の文書間一致

- **GIVEN** ルート README のインストール節と各 `SKILL.md` の Setup
- **WHEN** 4 形態の座標を比較する
- **THEN** 同じ値である

### Requirement: スクリーンショットの提示

ルート README は、iOS と Android のそれぞれについて Dialog / Loading / Toast の 3 機能を示すスクリーンショット計 6 枚を主な特徴の直後に持つ SHALL。画像はルートの `assets/` に置き、英日 README は同一の画像ファイルを参照してキャプションのみ言語別とする SHALL。画像はシミュレータ / エミュレータで Sample のデモ駆動モードから撮り、端末を特定できる表示 (キャリア名・実機の時刻・バッテリー残量等) を含まない SHALL。.NET MAUI と KMP については画像を置かず、Native と同じ画面になる旨を文で示す SHALL。

#### Scenario: 6 枚の組み合わせの網羅

- **GIVEN** ルート README が参照するスクリーンショット
- **WHEN** platform (iOS / Android) と機能 (Dialog / Loading / Toast) の組み合わせを数える
- **THEN** 6 通りすべてが 1 枚ずつ `assets/` に存在し、英日 README が同一パスを参照している

#### Scenario: 端末固有情報の不在

- **GIVEN** 採用したスクリーンショット 6 枚
- **WHEN** ステータスバー領域を目視で確認する
- **THEN** 端末を特定できる表示がない

### Requirement: 配信準備中の状態表記

ルート README は冒頭に配信準備中である旨の表記を 1 箇所だけ持つ SHALL。API 安定性の表記 (0.x の間は破壊的変更があり得る旨) は状態表記と区別し、公開後も残る常設の記述として持つ SHALL。

#### Scenario: 解除箇所の単一性

- **GIVEN** ルート README
- **WHEN** 未配信・配信準備中を示す記述を探す
- **THEN** 冒頭の 1 箇所だけが該当し、インストール節には存在しない

### Requirement: 英日 README の翻訳ロックステップ

ルート README 2 枚は同一の節構成 (見出し階層の並び・コードブロックの数と順序) を持ち、コードブロックは byte 一致する SHALL。一方だけを更新した状態をコミットしない SHALL NOT。

#### Scenario: 構成の一致

- **GIVEN** 英語 README と日本語 README
- **WHEN** 節構成一致とコードブロック byte 一致を機械検査する
- **THEN** 違反 0 件

### Requirement: 開発者向け知識の所在

廃止する `samples/` 配下 README が持っていた規範と記述は次に置かれる SHALL: 撮影のための起動引数 (キー 2 つ・安定デモ ID 14 件・アプリ識別子・iOS / Android の外部表現) は `kasane/handbook/cross/sample-parity.md` の節、各ルートの参照方式とビルド・実行コマンドは `kasane/handbook/cross/local-development-setup.md` の「Sample のビルドと実行」節、KMP iOS アプリの 3 点リンクと `integrateLinkagePackage` の再生成手順は `kasane/concepts/kmp/api/ios-host-integration.md`。既出の内容 (パリティ写像・器の責務と演出の添付の注意・KMP の演出分担メモ) は移送せず、既出であることを確認して捨てる SHALL。cross/ADR-0010 の本文、`kasane/config.yaml` の `ui.screenshot`、sample-parity.md の「関連」節は `samples/README.md` を正として指さない SHALL NOT。

#### Scenario: 撮影用の起動引数の到達可能性

- **GIVEN** 廃止前の `samples/README.md` 「撮影のための起動引数」節
- **WHEN** `kasane/handbook/cross/sample-parity.md` を調べる
- **THEN** キー 2 つ・安定デモ ID 14 件・4 ルートのアプリ識別子・iOS / Android の外部表現が読める

#### Scenario: Sample のビルド手順の到達可能性

- **GIVEN** 廃止前の 4 ルートの README にあった参照方式とビルド・実行コマンド
- **WHEN** `kasane/handbook/cross/local-development-setup.md` を調べる
- **THEN** 4 ルートそれぞれの参照方式 (理由を含む) と起動手順が読める

#### Scenario: KMP iOS 統合の到達可能性

- **GIVEN** 廃止前の `samples/kmp/README.md` 「ビルドと実行 > iosApp」
- **WHEN** `kasane/concepts/kmp/api/ios-host-integration.md` を調べる
- **THEN** 利用者向けの統合手順 (Maven 依存 1 点・合成パッケージの生成・登録 API 用の SwiftPM 1 点) と Sample での再生成手順が読める

#### Scenario: 参照元の付け替え

- **GIVEN** cross/ADR-0010・`kasane/config.yaml`・sample-parity.md
- **WHEN** `samples/README` への言及を検索する
- **THEN** 存在せず、ADR-0010 と config は handbook の該当節を指している

### Requirement: 利用者向け Skill の API 掲載基準

`kasane/handbook/cross/user-skill-api-listing.md` (kind: rule) が、利用者向け Skill に公開 API をどこまで載せるかの方針 (簡潔でも網羅)、意図的な掲載除外の基準、KsDialogs の現行除外リスト (Skill 生成後の docs-refresh 3e の報告に対するオーナー判断で初期化)、コード例のコメント規約、してはいけないこと (除外リストに無い未掲載 API を独断で除外しない・除外 API を concepts から消さない) を持つ SHALL。docs-refresh SKILL.md の 3e はこの規約を参照する SHALL。

#### Scenario: 3e の仕分け

- **GIVEN** 生成後の skills/ に対する docs-refresh 3e の報告
- **WHEN** 報告された未掲載 API 名を規約に照らす
- **THEN** 各名前が「掲載する (Skill を修正)」か「除外リストに載っている」のどちらかに仕分けられ、除外リストの各行に基準が付いている

### Requirement: 貢献方針の表明

ルート README は英日とも貢献の節を持ち、外部からの Pull Request を受け付けず Issue で受けること、および Issue テンプレートの利用を案内する SHALL。`.github/CONTRIBUTING.md` (英語) と `.github/CONTRIBUTING_ja.md` (日本語) を持ち、方針の理由と Issue の書き方を示し相互リンクする SHALL。

#### Scenario: README だけを読む人への到達

- **GIVEN** ルート README しか読まない訪問者
- **WHEN** 貢献の節を読む
- **THEN** PR を受け付けないこと・Issue で受けること・テンプレートを使ってほしいことが分かり、CONTRIBUTING へのリンクがある

### Requirement: Issue テンプレートの必須項目

`.github/ISSUE_TEMPLATE/` に英語の Issue Forms 3 本 (バグ報告 / 提案 / 質問) を置く SHALL。バグ報告は Version / Platform / 再現手順 / 実際の挙動 / 期待した挙動を、提案は解決したい課題 / 現状の困りごと / 検討した代替案を、質問は Version / Platform / 試したこと / 参照した Skill・README 節を必須項目とする SHALL。Platform はバグ報告と質問で同じ単一選択の 7 択 (`iOS` / `Android` / `.NET MAUI on iOS` / `.NET MAUI on Android` / `Kotlin Multiplatform on iOS` / `Kotlin Multiplatform on Android` / `Multiple platforms`) とする SHALL。本文を英語・日本語のいずれで書いてもよいことを案内する SHALL。`config.yml` で `blank_issues_enabled: false` とする SHALL。

#### Scenario: Forms の静的検査

- **GIVEN** `.github/ISSUE_TEMPLATE/` の 4 ファイル
- **WHEN** YAML として読み、必須項目と Platform の選択肢を検査する
- **THEN** 3 本の必須項目がすべて `required: true` で存在し、Platform の dropdown が 2 本で同一の 7 択、`blank_issues_enabled` が false である
