# spm-distribution (delta)

## ADDED Requirements

### Requirement: スナップショット同期スクリプト

`scripts/spm-snapshot/` の同期スクリプトは、引数で渡されたチェックアウト済み配信リポジトリ作業コピーに対し、`.git/` 以外の既存内容を除去した上で、ホワイトリスト 5 点 — `ios/Package.swift` (ルートへ)・`ios/Sources/`・`ios/Tests/`・monorepo ルート `LICENSE` のコピー・誘導 README (テンプレートから) — のみを配置する (SHALL)。実行は冪等である (SHALL)。

破壊的操作の前に次を全件検証し、1 つでも失敗したら同期先を一切変更せず非ゼロ終了する (SHALL):

1. コピー元 5 点がすべて存在すること
2. 同期先が canonical path 化の上で git top-level ディレクトリであること
3. 同期先の `origin` remote URL が配信リポジトリ `kamusoft/KsDialogs-SPM` を指すこと
4. 同期先が monorepo 自身またはその祖先ディレクトリでないこと

スクリプトは git 操作 (commit / tag / push) を行わず、git metadata (HEAD / index / refs / remote 設定) を変更せず、ネットワーク操作を行わない (SHALL)。

#### Scenario: ホワイトリスト 5 点の配置
- **GIVEN** 検証を満たす空の作業コピー (`.git/` のみ)
- **WHEN** 同期スクリプトを実行する
- **THEN** 作業コピー直下に `Package.swift` / `Sources/` / `Tests/` / `LICENSE` / `README.md` の 5 点だけが存在する

#### Scenario: 列挙外ファイルの混入防止
- **GIVEN** 作業コピーに前回スナップショットの残骸や無関係なファイルが存在する
- **WHEN** 同期スクリプトを実行する
- **THEN** `.git/` 以外の列挙外ファイルはすべて除去され、ホワイトリスト 5 点だけが残る

#### Scenario: 冪等性
- **GIVEN** 同期スクリプトを一度実行した作業コピー
- **WHEN** 同じ入力でもう一度実行する
- **THEN** 作業コピーの内容は 1 回目の実行結果と同一である

#### Scenario: 同期先の誤指定の拒否
- **GIVEN** 同期先として git top-level でないディレクトリ、`origin` が配信リポジトリを指さない git リポジトリ、または monorepo 自身のいずれかを渡す
- **WHEN** 同期スクリプトを実行する
- **THEN** スクリプトは非ゼロ終了し、同期先の内容は一切変更されない

#### Scenario: コピー元不足時の無変更
- **GIVEN** コピー元 5 点のいずれかが存在しない状態
- **WHEN** 同期スクリプトを実行する
- **THEN** スクリプトは非ゼロ終了し、同期先の内容は一切変更されない

#### Scenario: git 非操作
- **GIVEN** コミット履歴を持つ作業コピー
- **WHEN** 同期スクリプトを実行する
- **THEN** `.git/` は保持され、新しい commit / tag は作られず、HEAD・index・remote 設定は実行前と同一である (変更は未コミットの working tree として残る)

### Requirement: 誘導 README の整合

同期スクリプトが配置する誘導 README は、monorepo (`https://github.com/kamusoft/KsDialogs`) をソース・インストール手順・Issue 窓口として案内する内容であり、配信リポジトリに置かれている現行の README と同一内容である (SHALL)。

#### Scenario: 初回 sync 後の README
- **GIVEN** 配信リポジトリの作業コピーに同期スクリプトを実行した状態
- **WHEN** 配置された `README.md` を配信リポジトリの現行 README と比較する
- **THEN** 差分がない

### Requirement: 配信リポジトリの https 解決

配信リポジトリ `KsDialogs-SPM` に push されたスナップショットは、消費者プロジェクトから `https://github.com/kamusoft/KsDialogs-SPM` の URL と semver tag 指定で SwiftPM 解決でき、product `KsDialogs` をリンクして module `KsDialogs` を import できる (SHALL)。検証に用いる prerelease tag (`X.Y.Z-alpha.N` 形式) は、検証の成否に関わらず手順の終了時に local と remote の両方から削除し、配信リポジトリに tag を残さない (SHALL)。削除するのは手順が作成した tag だけである (SHALL)。

#### Scenario: 実リモートからの依存解決とビルド
- **GIVEN** スナップショットと prerelease tag (`X.Y.Z-alpha.N` 形式) が push された配信リポジトリ
- **WHEN** 一時消費者プロジェクトが https URL + その tag を exact 指定で依存に追加し、`KsDialogs` の公開型を最低 1 つ参照するコードを iOS Simulator 向けにビルドする
- **THEN** 依存解決とビルドが成功する

#### Scenario: 検証用 tag の後始末 (成功時)
- **GIVEN** https 解決の検証が成功した配信リポジトリ
- **WHEN** remote と作業コピーの tag 一覧を照会する
- **THEN** 検証用 prerelease tag はどちらにも存在しない

#### Scenario: 検証用 tag の後始末 (失敗時)
- **GIVEN** 検証用 tag を push した後に一時消費者のビルドが失敗した状態
- **WHEN** 手順を中断する
- **THEN** 手順に組み込まれた後始末が走り、検証用 prerelease tag は remote と作業コピーの両方から削除され、失敗の記録だけが残る

### Requirement: monorepo 内の iOS 消費者の維持

`samples/ios` と maui の iOS binding (`maui/macios/native`) は、引き続き Local Swift Package 参照 (`ios/`) で product `KsDialogs` をリンクしてビルドできる (SHALL)。本 change は `ios/Package.swift` を変更しない (SHALL)。

#### Scenario: iOS package のテスト維持
- **GIVEN** 本 change 適用後の `ios/`
- **WHEN** iOS Simulator destination で package の全テストを実行する (検証 CI `verify-ios.yml` と同じ経路)
- **THEN** ビルドとテストが成功し、実行されたテスト件数は 1 件以上である

#### Scenario: iOS Sample と binding のビルド
- **GIVEN** 本 change 適用後の `samples/ios` と `maui/macios/native` の binding プロジェクト
- **WHEN** それぞれを iOS Simulator 向けにビルドする
- **THEN** Local Swift Package 参照で product `KsDialogs` が解決され、ビルドが成功する
