# Delta Spec: user-docs (rename-dialog-contract-singular)

対象: 契約型名を記載する開発者向け・利用者向け文書と、docs-refresh の禁止トークン lint。

確認先: `kasane/concepts/{core,ios,android,kmp,maui}/api/*.md` (型名の記載)、`README.md` / `README_ja.md`、`skills/{en,ja}/*/` (SKILL.md と references/)、`.agents/skills/docs-refresh/SKILL.md` (禁止トークンのパターン定義とその読み方の注記)。

## ADDED Requirements

### Requirement: 文書の契約型名が実装と一致する
concepts・handbook・README・skills に現れる Dialog の契約型名は、実装の公開名 (`KsDialog` / `IKsDialog`) と一致 SHALL する。経緯を記録する文書 (decisions の本文・log.md・changes/archive) は改名前の名前を残してよい。

#### Scenario: 現行文書に旧名の型参照が残らない
- **GIVEN** 改名後の concepts / handbook / README / skills
- **WHEN** 通常文・表セル・コード span を含む全 occurrence の `KsDialogs` / `IKsDialogs` を列挙し、製品名の用法 (モジュール名・パッケージ・namespace・NuGet ID・製品名接頭辞の型・リポジトリ名・製品としての言及) を allowlist で除外する
- **THEN** 契約型を指す旧名は 0 件であり、除外した occurrence の一覧が報告に添えられている

#### Scenario: skills の en / ja が同じ名前を掲載する
- **GIVEN** 改名後の skills/en と skills/ja
- **WHEN** 各 Skill の Dialog 契約の型名を突き合わせる
- **THEN** 両版とも `KsDialog` (MAUI Skill は `IKsDialog`) で一致する

### Requirement: 禁止トークン lint が改名後の正しい名前を弾かない
docs-refresh の禁止トークン検査は、単数形 `KsDialog` を誤表記として検出 SHALL NOT する。契約型の旧名が残っていないことの担保は本 change の残存検査 (上記 Scenario) で行い、lint に契約文脈の判定を持ち込まない。

#### Scenario: 単数形の契約名が lint を通り、残した禁止パターンは検出される
- **GIVEN** 許可すべき綴り (`KsDialog` / `IKsDialog` / `KsDialogs` / `KsDialogAttributes`) と、残す禁止パターンの各例 (`Ksdialogs` / `KSDialogs` / `ksDialogs` / `Ks Dialogs` / `ks-dialogs` / `com.kamusoft` / `jp.kamusoft.KsDialogs` / `KsDialogsMaui` / `KsDialogs.MAUI`) を並べた一時 fixture
- **WHEN** 改修後の禁止トークン検査を fixture と skills/ の両方に実行する
- **THEN** fixture では許可すべき綴りは 1 件も検出されず禁止例はそれぞれ 1 件以上検出され、skills/ では検出 0 件である
