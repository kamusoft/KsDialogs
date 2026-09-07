# Delta Spec: docs-refresh (KsDialogs への導入)

対象能力: docs-refresh — 利用者向け Agent Skills (`skills/{en,ja}/<name>/`) と README 群を `kasane/concepts/` とコード・テストへ追従させるメンテナンススキル。KsDialogs にはこの能力がまだ無く、本デルタは翻案元 (`../KsSettingsView/.agents/skills/docs-refresh/`、契約は `../KsSettingsView/kasane/changes/archive/2026-08-26-retarget-docs-refresh-to-skills/specs/docs-refresh/spec.md`) を取り込んだうえで KsDialogs 固有に置き換わる契約を定義する。翻案元と同一の挙動 (manifest v3 差分検出・承認ゲート・Skill 単位委譲・実行フラグ・manifest 更新) は本デルタで再定義せず、「翻案元と同一」として Requirement「docs-refresh 一式の配置と起動経路」が担保する。

## ADDED Requirements

### Requirement: docs-refresh 一式の配置と起動経路

docs-refresh は `.agents/skills/docs-refresh/` に SKILL.md・`references/prompt-skill.md`・`references/prompt-readme.md`・`scripts/` 8 本 (targets-list / planned-manifest / concepts-coverage-check / api-coverage-check / heading-parity-check / code-block-parity-check / frontmatter-check / link-resolution-check) の一式として配置される SHALL。翻案元は KsSettingsView の commit `33ce94e` (2026-09-02) 時点の `.agents/skills/docs-refresh/` に固定し、以後の翻案元の変化は本契約に影響しない。`.claude/skills/docs-refresh` は同ディレクトリへの symlink である SHALL。`scripts/` 8 本は翻案元と同一内容 (無改変) である SHALL。SKILL.md は固有値の差し替え後も翻案元の次の契約を保持する SHALL: manifest の読み込みと検証 (不在 / parse エラー / version ≠ 3 / 必須キー欠落・型不正で停止)、承認前の無変更、Skill 単位の委譲と en/ja ペア同時生成 (最大 3 並列・器 `ksn-implementer` 固定・委譲不能時は起動時に停止)、`--all` / `--readme-only` の意味と同時指定エラー、`--readme-only` での concepts スナップショット非更新、API 名網羅検査 (3e) の報告のみの位置づけ、予定 manifest に対する整合性チェック、manifest を最後に書く規律と未処理 concept の旧ハッシュ保持 (部分承認・中断時の再検出)。スキルは自発的に自動発動しない SHALL NOT — 起動はユーザーの明示依頼のみとし、その旨を description と Guardrails の両方に保持する SHALL。

#### Scenario: 2 つの入口が同じ実体を指す

- **GIVEN** 配置済みの docs-refresh
- **WHEN** `.agents/skills/docs-refresh/SKILL.md` と `.claude/skills/docs-refresh/SKILL.md` をそれぞれ開く
- **THEN** 同一のファイルが読まれ、`scripts/` 配下の 8 本は翻案元の同名ファイルと byte 一致する

#### Scenario: 翻案元の契約の保持

- **GIVEN** 固有値を差し替えた後の SKILL.md
- **WHEN** 上記の契約項目を 1 件ずつ本文で探す
- **THEN** すべての項目が翻案元と同じ意味で残っており、固有値の差し替えで欠落・改変された項目がない

#### Scenario: concepts 更新後の非発動

- **GIVEN** concepts が更新された直後のセッション
- **WHEN** ユーザーが docs-refresh を明示的に依頼していない
- **THEN** スキルは発動せず、`skills/` と README 群は変更されない

### Requirement: 初期生成前の停止

`skills/.manifest.json` が存在しない・JSON として壊れている・version が 3 でない場合、スキルは `skills/` と README 群を一切書き換えず、初期生成 (承認済み change による `skills/` 一式 + manifest 初期版の作成) または manifest の修復が必要である旨を案内して停止する SHALL。本 change の完了から初期生成の完了までの間はこの状態が続く。

#### Scenario: 異常な manifest でも書き換えない

- **GIVEN** `skills/.manifest.json` が JSON として壊れている、`version` が 3 でない、または必須キーが欠落している
- **WHEN** docs-refresh を起動する
- **THEN** `skills/` と README 群は書き換えられず、理由 (parse エラー / version=N / 欠落キー名) を添えた停止案内で終わる (SKILL.md の manifest 検証手順に 4 ケースすべてが列挙されている)

#### Scenario: skills/ 未生成での起動

- **GIVEN** `skills/` ディレクトリも `skills/.manifest.json` も存在しない
- **WHEN** docs-refresh を起動する (または `scripts/concepts-coverage-check.py` を単独で実行する)
- **THEN** 何も書き換えられず、初期生成が必要である旨の案内 (スクリプト単独では manifest 不在のエラー終了) で終わる

### Requirement: 追従対象の規範

SKILL.md は追従対象を **5 Skill × 2 言語** と README 4 枚として定義する SHALL。Skill は `ksdialogs-ios` / `ksdialogs-android` / `ksdialogs-maui` / `ksdialogs-kmp` / `ksdialogs-aiforms-migration` の 5 本で、各 Skill は `SKILL.md` (能力マップ) + `references/` (レシピ) の構成をとる。`ksdialogs-kmp` は 1 本で共有コード側 (commonMain) と Android ホスト側・iOS ホスト側を扱い、`SKILL.md` は共有コード側 + 3 側の Setup + 最小コードに絞り、ホスト側の登録レシピは `references/` で振り分ける SHALL。`ksdialogs-aiforms-migration` は `SKILL.md` + `references/api-mapping.md` の構成で、移植元で Obsolete だった Toast は対応先なしと扱う SHALL。README 4 枚は `skills/README.md` / `skills/README_ja.md` / ルート `README.md` / `README_ja.md` で、manifest の `readmes` が正である。Skill の構成の見直し (新設・廃止・references の分割方針) は docs-refresh の守備範囲外である SHALL NOT。

#### Scenario: 追従対象の特定

- **GIVEN** 配置済みの SKILL.md
- **WHEN** 追従対象の節を読む
- **THEN** 5 Skill の名前と構成 (KMP は 1 本でホスト側は references/ 振り分け、移行は SKILL.md + api-mapping.md) と README 4 枚が特定でき、KsSettingsView の Skill 名 (`kssettingsview-*`) は本文に残っていない

### Requirement: 移行 Skill の源泉は新 API 側の concepts のみ

manifest の `targets` における `ksdialogs-aiforms-migration` 配下ファイルの源泉は新 API 側の concepts (core/api・maui/api) のみである SHALL。移植元 (AiForms.Maui.Dialogs) の旧 API 側は concepts に源泉を持たず、docs-refresh の差分検出・要追従判定の対象外である SHALL NOT。SKILL.md はこの規則と、旧 API 側の書き起こしが初期生成時の一度限りで移植元のローカル clone (`kasane/concepts/cross/reference/reference-repositories.md` で解決) を前提とする旨を明記する SHALL。

#### Scenario: 移植元の変更は要追従にならない

- **GIVEN** 移植元リポジトリの README が更新され、KsDialogs の concepts は変化していない
- **WHEN** docs-refresh の差分検出を実行する
- **THEN** 移行 Skill のファイルは要追従リストに載らない

#### Scenario: 新 API 側の変更で対応先が追従する

- **GIVEN** 移行 Skill の対応表が源泉とする concept (例: maui/api/di-registration.md) のハッシュが変化した
- **WHEN** 差分検出を実行する
- **THEN** `targets` の逆引きで `ksdialogs-aiforms-migration/references/api-mapping.md` が en/ja ペアとして要追従リストに載る

### Requirement: excluded の初期値と architecture カテゴリの既定

SKILL.md は初期 manifest の `excluded` を `cross/reference/reference-repositories.md` (理由: 開発環境のローカルパス対応表で利用者向け Skill の対象外) の 1 本と定め、それ以外の concepts (core/api 8 本・maui/api/di-registration.md) はいずれかの Skill の `targets` に載ることを規範とする SHALL。`architecture/` カテゴリ (core / cross) の concept は既定で除外候補として扱う SHALL — 網羅検査 (3c) が未参照・未除外として報告した時点で、除外候補の既定を添えてユーザーに提示し、理由つきで確定するまで manifest を変更しない SHALL NOT。既定は候補であって自動除外ではない。

#### Scenario: architecture 配下の新 concept

- **GIVEN** `kasane/concepts/cross/architecture/` に新しい concept が追加され、manifest の `targets` にも `excluded` にも現れない
- **WHEN** 網羅検査を実行する
- **THEN** 検査失敗として報告され、提示には「architecture カテゴリのため除外候補 (既定)」が添えられ、ユーザーが確定するまで manifest は変更されない

#### Scenario: 利用者に効く architecture concept は targets へ回せる

- **GIVEN** 前 Scenario の提示
- **WHEN** ユーザーが「Skill に載せる」を選ぶ
- **THEN** 該当 concept は `excluded` ではなく指定 Skill の `targets` に追記される (既定は選択を拘束しない)

### Requirement: コード正の機械チェック (ツール最低バージョン) の取得元

コードを正とする 3d の突合は次の 4 行の取得元から値を読む SHALL: ① AGP / Kotlin / minSdk / compileSdk は `android/gradle/libs.versions.toml` の `[versions]` (`agp` / `kotlin` / `android-minSdk` / `android-compileSdk`。KMP ルートは同じ catalog を共有するため単一取得元)、② Gradle は `android/gradle/wrapper/gradle-wrapper.properties` と `kmp/gradle/wrapper/gradle-wrapper.properties` の `distributionUrl` (2 本)、③ Swift tools / iOS Deployment Target は `ios/Package.swift`、④ .NET TFM / 対象 OS 下限 / MAUI 本体下限は `maui/KsDialogs.Maui/KsDialogs.Maui.csproj` の `<TargetFrameworks>` / `<SupportedOSPlatformVersion>` / `Microsoft.Maui.Controls` の `Version`。突合先はルート README 群の対応プラットフォーム表・開発環境要件と、該当記載を持つ各 `SKILL.md` の導入節である SHALL。Gradle wrapper 2 本が食い違う場合はその食い違い自体を報告する SHALL。各 module の `build.gradle.kts` は値を持たない (catalog 参照のみ) ため取得元にしない SHALL NOT。① の前提「KMP ルートが android の version catalog を共有している」は `kmp/settings.gradle.kts` の `versionCatalogs` が `../android/gradle/libs.versions.toml` を `from()` していることで成り立つため、3d はこの前提を先に確認し、外れていたら android 側の値を KMP の正として読まずに差異を報告して停止する SHALL。

#### Scenario: 取得元が実在して値を返す

- **GIVEN** 現行リポジトリ
- **WHEN** 4 行の取得元を SKILL.md の抽出方法どおりに読む
- **THEN** AGP・Kotlin・minSdk・compileSdk・Gradle 版 ×2・swift-tools-version・iOS Deployment Target・TFM・OS 下限 ×2・MAUI 本体版のすべてが空でない値として得られる

#### Scenario: KMP の catalog 共有が外れた

- **GIVEN** `kmp/settings.gradle.kts` が `../android/gradle/libs.versions.toml` を参照しなくなった
- **WHEN** 3d を実行する
- **THEN** KMP の AGP / Kotlin / minSdk / compileSdk を android 側の値として突合せず、前提が外れた旨が報告される

#### Scenario: version catalog の変更

- **GIVEN** `android/gradle/libs.versions.toml` の `agp` が README 群の記載と食い違う
- **WHEN** 差分検出を実行する
- **THEN** ルート README 群 (および該当記載を持つ SKILL.md) が要追従リストに載る

#### Scenario: Gradle wrapper の食い違い

- **GIVEN** android と kmp の wrapper の `distributionUrl` が異なる版を指す
- **WHEN** 差分検出を実行する
- **THEN** README 突合とは別に、2 本の食い違いが報告される

#### Scenario: MAUI 本体下限の変更

- **GIVEN** csproj の `Microsoft.Maui.Controls` の `Version` が README 群または maui Skill の導入節の記載と食い違う
- **WHEN** 差分検出を実行する
- **THEN** ルート README 群と `ksdialogs-maui/SKILL.md` (en/ja) が要追従リストに載る

### Requirement: 閉世界性と機械面の漏れ検査

整合性チェックの旧名残 grep (6-⑤) は、生成物に次が含まれる場合に該当ファイルを再修正対象へ追加する SHALL: ① リポジトリ内部用語 (`kasane/` 配下への参照・ADR 番号の参照)、② 利用者向けでない機械面の名前 (`KsDialogsInteropBridge` / `KsDialogsInteropResultType` — いずれも現存する public ABI だが KMP cinterop 委譲専用で、利用者向け導線から退いている。「廃止」ではない)、③ `skills/` 配下のファイルに限り、その Skill のルートディレクトリ (`skills/<lang>/<name>/`) の外へ解決される相対 markdown リンク (別 Skill・リポジトリ内の他ファイルへのリンク。同一 Skill 内の `../SKILL.md` 等は許容)。README 群には ③ を適用しない。外部 URL は配布座標 (SwiftPM package URL・frontmatter `metadata.source`) として正当に現れるため機械検査の対象にせず、文書としての外部参照の禁止は生成プロンプトの内容規約とレビューで担保する。翻案元の `docs/` / `openspec` 参照検査は KsDialogs に該当ディレクトリが無いため持ち込まない。生成プロンプトの内容規約は「`kasane/` 内部文書・ADR 番号・機械面・他の Skill やリポジトリ内ファイルへの参照を書かない (閉世界性。配布座標の URL は可)」を明記する SHALL。

#### Scenario: 内部用語の漏れ

- **GIVEN** `kasane/concepts/...` へのリンク、または `ADR-0004` のような ADR 番号を含む Skill ファイル
- **WHEN** 整合性チェックを実行する
- **THEN** 該当ファイルが失敗として報告され、再修正対象に追加される

#### Scenario: 機械面の漏れ

- **GIVEN** `KsDialogsInteropBridge` を利用者コードとして示す Skill ファイル
- **WHEN** 整合性チェックを実行する
- **THEN** 該当ファイルが失敗として報告され、再修正対象に追加される

#### Scenario: Skill ルート外への相対リンク

- **GIVEN** `skills/en/ksdialogs-kmp/SKILL.md` から `../ksdialogs-android/SKILL.md` へ解決される相対リンクを含む生成物と、同じ Skill 内の `references/ios-host.md` → `../SKILL.md` のリンクを含む生成物
- **WHEN** 整合性チェックを実行する
- **THEN** 前者は失敗として報告され、後者は報告されない

### Requirement: 配信識別子の表記ゆれ検査

整合性チェックの表記ゆれ grep (6-⑧) は cross/ADR-0005 (公開識別子の写像表) と android/ADR-0001 (`jp.kamusoft:ksdialogs-compose` の分離) の 2 本を正とし、そこから導いた誤表記パターン (ブランド名の大小文字・区切りの崩れ、`com.kamusoft`、Maven 座標へのブランド名混入、NuGet ID の崩れ) を検出する SHALL。正しい識別子 (SwiftPM `KsDialogs`、Maven `jp.kamusoft:ksdialogs` / `ksdialogs-compose` / `ksdialogs-kmp`、Kotlin パッケージ `jp.kamusoft.ksdialogs` / `.compose` / `.kmp`、NuGet `KsDialogs.Maui`、.NET namespace `KsDialogs`) は検出しない SHALL NOT。

#### Scenario: 誤表記の検出

- **GIVEN** `Ksdialogs`・`ks-dialogs`・`com.kamusoft`・`jp.kamusoft.KsDialogs`・`KsDialogsMaui` のいずれかを含む Skill ファイル
- **WHEN** 整合性チェックを実行する
- **THEN** 該当ファイルが失敗として報告される

#### Scenario: 正しい識別子は素通り

- **GIVEN** 正しい識別子のみを含む Skill ファイル
- **WHEN** 整合性チェックを実行する
- **THEN** 6-⑧ は何も報告しない

### Requirement: 規約記述と lint 範囲

AGENTS.md と CLAUDE.md は同一の Kasane 運用宣言として「`skills/` は利用者向けドキュメント (Agent Skills) であり、エージェントは開発時の知識参照先にしない」「`skills/` と README 群の継続的な追従更新は docs-refresh 経由のみ (自動発動禁止)。初期生成・構成の見直しは承認済み change の実装として行う。スキル本体は `.agents/skills/docs-refresh/SKILL.md`」を持つ SHALL。ツールの手順は AGENTS.md に書かない SHALL NOT。`kasane/config.yaml` は `context` に同趣旨を持ち、`lint.identity.scope` に `skills` を含み、`lint.comment-policy.exclude` は `skills` を指す SHALL。

#### Scenario: 宣言の一致

- **GIVEN** 更新後の AGENTS.md と CLAUDE.md
- **WHEN** 開発ハーネス節を比較する
- **THEN** 両者の宣言が一致し、docs-refresh の実行手順 (コマンド・フラグ) は含まれていない

#### Scenario: skills/ が identity-lint の範囲に入る

- **GIVEN** 更新後の `kasane/config.yaml`
- **WHEN** `lint.identity.scope` を読む
- **THEN** `kasane` と `skills` の両方を含む
