# kmp-facade デルタスペック

KMP 形態での dialog-contract の貫通。契約の意味論は [dialog-contract](../dialog-contract/spec.md) に従う。構造判断は kmp/ADR-0001 (素の suspend 直接公開)・kmp/ADR-0002 (契約のみ commonMain・Native 全委譲) が正。

## ADDED Requirements

### Requirement: commonMain からの show 貫通

KMP は commonMain に契約 (interface + sealed 結果型 + レジストリ契約) を公開し、共有コードからの show 呼び出しが iOS / Android 両ターゲットで Native 実装に委譲されて動作する SHALL。

#### Scenario: 共有コードからの show が Android で動作する
- **GIVEN** commonMain の Presenter が契約 interface 経由で show を呼び出す構成で、Android 側で VM 型に View factory が登録されている
- **WHEN** Android ターゲットで show を呼び出し、ダイアログを完了操作で閉じる
- **THEN** Presenter は completed(結果値) を正しい型で受け取る

#### Scenario: 共有コードからの show が iOS で動作する
- **GIVEN** 同じ Presenter 構成で、iOS 側 (Swift) で VM 型に View factory が登録されている
- **WHEN** iOS ターゲットで show を呼び出し、ダイアログを完了操作で閉じる
- **THEN** Presenter は completed(結果値) を正しい型で受け取る

### Requirement: Swift 側登録とのキー同一性

commonMain で定義した VM クラスは Swift から ObjC クラスとして見え、Swift 側でそのクラスをキーに登録した View factory が、共有コードからの show で解決される SHALL (phase-1 申し送りの最優先疎通確認)。

#### Scenario: Swift 登録の View が共有コードの show で表示される
- **GIVEN** Swift アプリ起動時に commonMain 定義の VM クラスをキーとして Native View の factory を登録している
- **WHEN** 共有コードがその VM のインスタンスで show を呼び出す
- **THEN** Swift 側で登録した factory が解決され、その View がダイアログとして表示される

### Requirement: Swift async からの直接呼び出し

KMP facade の suspend な show は、Swift から async 呼び出しとして利用でき、型付き結果が正しい型・値で届く SHALL (kmp/ADR-0001。粗が受け入れ条件を割る場合のフォールバック判定を兼ねる)。iosMain actual は iOS Native の型消去輸送 (design Decision 12) から VM の宣言結果型への復元を担い、復元失敗と構成エラーは Kotlin 例外 → NSError 変換で Swift 側に届く SHALL。

#### Scenario: Swift から await した結果の型と値が正しい
- **GIVEN** Swift コードが KMP facade の show を await で呼び出す
- **WHEN** 表示されたダイアログを完了操作またはキャンセル操作で閉じる
- **THEN** completed の場合は結果値が、cancelled の場合はキャンセルが、Swift 側で判別可能な形で正しく得られる

### Requirement: テスト差し替え

commonMain の契約 interface は、Native 実装なしの fake 実装で差し替え可能である SHALL (kmp/ADR-0002 — Presenter の単体テストがダイアログ表示なしで書ける)。

#### Scenario: fake 実装で Presenter を単体テストできる
- **GIVEN** 契約 interface の fake 実装が completed(固定値) を返すよう構成されている
- **WHEN** commonTest で Presenter に fake を注入して show を経由するロジックを実行する
- **THEN** ダイアログを表示せずに Presenter の分岐が検証できる
