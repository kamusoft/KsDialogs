# android-native デルタスペック (expand-api-surface)

改訂履歴: 2026-08-17 相方レビュー採用指摘の反映 — Compose 別モジュール化 (design Decision 7)・ホスト lifecycle 契約 (#4)・宣言表 compile Scenario (#1)。
2026-08-19 add-layout-spec 完了分の反映 — Compose 添付 DSL の Requirement 追加 (core/ADR-0015 の申し送り)。
2026-08-19 相方スペックレビュー (spec-002) 採用指摘の反映 — 非レイアウト属性の Scenario 化。

## ADDED Requirements

### Requirement: Compose 登録 (Android)

`@Composable` コンテンツを取る登録 (`registerCompose`) を **`ksdialogs-compose` モジュール**の拡張として提供すること (SHALL、design.md Decision 6・7 の宣言表が正)。`ksdialogs` 本体は Compose に依存しないこと。ホスティングは内部で行い、利用者は Composable をそのまま書けばよい。

#### Scenario: 宣言表どおりの利用コードがコンパイルできる
- **GIVEN** design.md Decision 6 の Android 宣言表の各形 (registerCompose / インライン show / showCompose / SimpleDialogViewModel)
- **WHEN** 利用コード形式のコンパイル検査を書く
- **THEN** 型引数の明示なしでコンパイルが通る

#### Scenario: 本体は Compose 非依存のまま
- **GIVEN** `ksdialogs` 本体モジュールの依存グラフ
- **WHEN** 依存を確認する
- **THEN** compose 系 artifact への依存が存在しない

#### Scenario: Compose コンテンツの登録と表示
- **GIVEN** registerCompose で VM を登録した状態
- **WHEN** show し、コンテンツ内の完了操作で閉じる
- **THEN** 型付き結果が返る

### Requirement: Compose ホストの lifecycle と破棄 (Android)

Compose コンテンツのホストは LifecycleOwner / SavedStateRegistryOwner が伝播する View tree に載せ、全閉鎖経路 (完了 / キャンセル / 呼び出し元キャンセル / 画面破棄) で composition を dispose すること (SHALL)。

#### Scenario: 全閉鎖経路で composition が破棄される
- **GIVEN** Compose 登録のダイアログを表示した状態
- **WHEN** 完了・キャンセル・呼び出し元キャンセルのそれぞれで閉じる
- **THEN** いずれの経路でも composition が dispose され、リークしない

### Requirement: Compose 添付 DSL (Android)

composable 冒頭で宣言する `KsDialogAttributes(options, placement)` を `ksdialogs-compose` モジュールの一部として提供すること (SHALL、core/ADR-0015。宣言は design.md Decision 6、実装方式は Decision 11)。添付値の採用はスナップショット契約 (初回レイアウトパス完了時点の値) に従うこと。Lazy スコープ内に書かれた場合は初回表示に反映されないことを DSL の契約 (ドキュメントコメント) に明記すること。

#### Scenario: 添付値が初回表示から反映される
- **GIVEN** composable 冒頭で配置・器属性を宣言した Compose コンテンツで登録した VM
- **WHEN** show する
- **THEN** 初回表示から添付値どおりに配置され、共通ケース表の該当ケースに適合する

#### Scenario: 非レイアウト属性も添付 DSL で届く
- **GIVEN** 冒頭で非既定の overlayColor と isCanceledOnTouchOutside=false を宣言した Compose コンテンツで登録した VM
- **WHEN** show し、外側をタップする
- **THEN** 覆いは添付した色で表示され、外側タップで閉じない

#### Scenario: 添付なしは契約既定値
- **GIVEN** `KsDialogAttributes` を書かない Compose コンテンツで登録した VM
- **WHEN** show する
- **THEN** 従来 View 系の添付なしと同じ契約既定値で表示される

#### Scenario: 提示後の添付変更は反映されない
- **GIVEN** Compose 添付 DSL つきのダイアログを表示中の状態
- **WHEN** コンテンツ側の状態変化で添付値 (placement・overlayColor・isCanceledOnTouchOutside) を変更する
- **THEN** 表示中の配置・覆いの色・外側タップ挙動のいずれも変化しない (スナップショット契約)

### Requirement: bool 既定の VM 契約の顔 (Android)

真偽値結果の VM を型引数なしで宣言できる顔 (typealias 等) を提供すること (SHALL)。register の型推論は従来どおり機能すること。

#### Scenario: 型引数なしの VM 宣言
- **GIVEN** bool 既定の顔で宣言した VM 型
- **WHEN** register の factory を書く
- **THEN** notifier は真偽値の通知役として推論される (型引数の明示なしでコンパイルが通る)

### Requirement: インライン show (Android)

View 版・Compose 版の両形でインライン factory show を提供すること (SHALL)。

#### Scenario: Compose のインライン表示
- **GIVEN** 未登録の VM
- **WHEN** Composable factory を渡すインライン show を呼ぶ
- **THEN** 表示・完了操作・型付き結果の一連が成立する
