# ios-native デルタスペック (expand-api-surface)

改訂履歴: 2026-08-17 相方レビュー採用指摘の反映 — ホスト所有・破棄の契約 (#4)・宣言表 compile Scenario (#1)。
2026-08-19 add-layout-spec 完了分の反映 — SwiftUI 添付 DSL の Requirement 追加 (core/ADR-0015 の申し送り)。
2026-08-19 相方スペックレビュー (spec-002) 採用指摘の反映 — preference 解決の終了状態 (design Decision 10) と非レイアウト属性の Scenario 化。

## ADDED Requirements

### Requirement: SwiftUI 登録オーバーロード (iOS)

登録 API は design.md Decision 6 の宣言どおり SwiftUI View を返す factory のオーバーロードを提供すること (SHALL)。ホスティングは内部で行い、利用者は SwiftUI 型をそのまま返せばよい。

#### Scenario: 宣言表どおりの利用コードがコンパイルできる
- **GIVEN** design.md Decision 6 の iOS 宣言表の各形 (SwiftUI 登録・UIView/SwiftUI インライン show)
- **WHEN** 利用コード形式のコンパイル検査を書く
- **THEN** 型注釈の追加なしでコンパイルが通る

#### Scenario: SwiftUI コンテンツの登録と表示
- **GIVEN** SwiftUI View を返す factory で VM を登録した状態
- **WHEN** show し、コンテンツ内の完了操作で閉じる
- **THEN** 型付き結果が返る

### Requirement: SwiftUI ホストの所有と破棄 (iOS)

SwiftUI コンテンツは hosting controller を提示コンテナの child containment に組み込んで保持し、全閉鎖経路 (完了 / キャンセル / 呼び出し元キャンセル / コンテナ破棄) で child から外して解放すること (SHALL)。

#### Scenario: 全閉鎖経路でホストが解放される
- **GIVEN** SwiftUI 登録のダイアログを表示した状態
- **WHEN** 完了・キャンセル・呼び出し元キャンセルのそれぞれで閉じる
- **THEN** いずれの経路でも hosting controller が親から外れ、解放される (リークしない)

### Requirement: SwiftUI 添付 DSL (iOS)

SwiftUI コンテンツ向けに、body ルートへ添付する `.ksDialogOptions(...)` / `.ksDialogPlacement(...)` modifier を提供すること (SHALL、core/ADR-0015。宣言は design.md Decision 6、実装方式は Decision 10)。添付値の採用はスナップショット契約 (初回ネイティブレイアウトパス完了時点の値) に従い、同一属性の重畳は外側勝ちとすること。preference 解決の終了状態は Decision 10 の3状態 (同期解決 / 追加レイアウトパス1回を上限とする遅延到達 / 上限到達で既定値 + 警告) で閉じること。

#### Scenario: 添付値が初回表示から反映される
- **GIVEN** body ルートに配置・器属性を添付した SwiftUI コンテンツで登録した VM
- **WHEN** show する
- **THEN** 初回表示から添付値どおりに配置され、共通ケース表の該当ケースに適合する

#### Scenario: 非レイアウト属性も添付 DSL で届く
- **GIVEN** body ルートに非既定の overlayColor と isCanceledOnTouchOutside=false を添付した SwiftUI コンテンツで登録した VM
- **WHEN** show し、外側をタップする
- **THEN** 覆いは添付した色で表示され、外側タップで閉じない

#### Scenario: 添付なしは契約既定値
- **GIVEN** 添付なしの SwiftUI コンテンツで登録した VM
- **WHEN** show する
- **THEN** 到達待ちによる提示遅延なしに、従来 View 系の添付なしと同じ契約既定値で表示される

#### Scenario: 到達上限後は既定値と警告
- **GIVEN** preference が上限 (追加レイアウトパス1回) までに到達しない状況
- **WHEN** show する
- **THEN** 契約既定値で提示され、警告ログが記録される (提示が停止しない)

#### Scenario: 同一属性の重畳は外側勝ち
- **GIVEN** 同一属性を内側と外側で二重に添付した SwiftUI コンテンツ
- **WHEN** show する
- **THEN** 外側の添付値が採用される

#### Scenario: 提示後の添付変更は反映されない
- **GIVEN** SwiftUI 添付 DSL つきのダイアログを表示中の状態
- **WHEN** コンテンツ側の状態変化で添付値 (placement・overlayColor・isCanceledOnTouchOutside) を変更する
- **THEN** 表示中の配置・覆いの色・外側タップ挙動のいずれも変化しない (スナップショット契約)

### Requirement: Result のデフォルト (iOS)

VM 契約の結果型はデフォルトで真偽値とし、宣言を省略した VM の notifier は真偽値に型付くこと (SHALL)。

#### Scenario: Result 宣言なしの VM
- **GIVEN** Result を宣言しない VM 型
- **WHEN** register の factory を書く
- **THEN** notifier は真偽値の通知役として型付いている (型注釈なしでコンパイルが通る)

### Requirement: インライン show (iOS)

UIView 版・SwiftUI 版の両形でインライン factory show を提供すること (SHALL)。

#### Scenario: SwiftUI のインライン表示
- **GIVEN** 未登録の VM
- **WHEN** SwiftUI factory を渡すインライン show を呼ぶ
- **THEN** 表示・完了操作・型付き結果の一連が成立する
