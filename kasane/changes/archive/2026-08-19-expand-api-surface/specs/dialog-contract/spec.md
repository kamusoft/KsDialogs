# dialog-contract デルタスペック (expand-api-surface)

改訂履歴: 2026-08-17 相方スペックレビュー採用指摘の反映 — bool 既定の形態別意味の明確化 (#2)・インライン非干渉の強化 (#7)・宣言的 UI のレイアウト適合 (#4)。
2026-08-19 add-layout-spec 完了分の反映 — 添付 DSL の供給契約同一性を「技術別登録の挙動同一性」に追記 (core/ADR-0015)。
2026-08-19 相方スペックレビュー (spec-002) 採用指摘の反映 — 供給同一性の対象に非レイアウト属性を明記・placement 上書きのオブジェクト置換とインライン show での成立を Scenario 化。

## ADDED Requirements

### Requirement: 真偽値結果の省略形

各形態の公開面は、真偽値結果のダイアログを結果型の記述なしで扱える省略形を提供すること (SHALL)。形態別の意味は次のとおり (design.md Decision 6 の宣言表が正):

- iOS: VM 契約のデフォルト associatedtype (`Result = Bool`) — Result 宣言なしの VM は真偽値
- Android: 真偽値用の型別名 (`SimpleDialogViewModel` 相当) — 型引数なしで VM を宣言できる
- MAUI: 非ジェネリック `IDialogViewModel` + VM 単型引数の Register / ShowAsync オーバーロード
- KMP: **共有 VM (commonMain) は従来どおり `DialogViewModel<Boolean>` を明示する** (expect interface に型引数デフォルトは導入しない)。省略できるのは Swift 向け公開面の `result:` 引数のみ
- カスタム結果型の従来の宣言経路はすべての形態で不変であること

#### Scenario: 省略形 VM の結果は真偽値で返る
- **GIVEN** 省略形で宣言した VM を登録した状態
- **WHEN** show し、真偽値つきの完了操作で閉じる
- **THEN** show の戻りは completed(真偽値) として型付きで得られる

#### Scenario: カスタム型の宣言経路は従来どおり
- **GIVEN** 文字列を結果型として宣言した VM を登録した状態
- **WHEN** show し、文字列つきの完了操作で閉じる
- **THEN** show の戻りは completed(文字列) として型付きで得られる

### Requirement: 技術別登録の挙動同一性

登録 API は従来 View 系と宣言的 UI 系の両方の factory を受け付け、どちらで登録しても show の観察可能な挙動 (結果経路・キャンセル経路・全閉鎖経路での破棄・レイアウト規則の適用) は同一であること (SHALL)。宣言的 UI 経由のコンテンツも add-layout-spec のレイアウト規則・共通ケース表適合の対象とすること。宣言的 UI の添付 DSL (core/ADR-0015) で供給した属性値も、従来 View 系の添付と同じ供給契約 (優先順位: show 引数 > コンテンツ添付 > 契約既定値、採用はスナップショット契約) に従うこと。

#### Scenario: 宣言的 UI 登録のダイアログも同じ結果経路を通る
- **GIVEN** 宣言的 UI の factory で登録した VM
- **WHEN** show し、完了操作で閉じる
- **THEN** 従来 View 系で登録した場合と同じ形の型付き結果が返る

#### Scenario: 宣言的 UI にもレイアウト規則が適用される
- **GIVEN** 宣言的 UI の factory で登録した VM にレイアウト属性を指定した状態
- **WHEN** show する
- **THEN** 従来 View 系と同じレイアウト規則 (add-layout-spec) で配置される

#### Scenario: 添付 DSL と従来添付は同じ結果になる (非レイアウト属性を含む)
- **GIVEN** 同じ属性値 (placement・非既定 overlayColor・isCanceledOnTouchOutside=false) を、従来 View 系の添付 (extension プロパティ / 添付プロパティ) と宣言的 UI の添付 DSL でそれぞれ与えた同内容のダイアログ
- **WHEN** 共通ケース表の該当ケースを実行し、あわせて覆いの色と外側タップ挙動を確認する
- **THEN** 両者は同一の配置・覆い・外側タップ挙動になる

#### Scenario: show 引数の placement は添付 DSL より優先される
- **GIVEN** 添付 DSL で placement を添付した宣言的 UI コンテンツ
- **WHEN** show 引数に別の placement を渡して show する
- **THEN** show 引数の placement がオブジェクトまるごと採用される (フィールド単位の合成はしない。供給優先順位はホスティング経由でも不変)

#### Scenario: インライン show でも placement 上書きが成立する
- **GIVEN** 属性を添付したコンテンツを返す factory
- **WHEN** placement 引数つきのインライン show を呼ぶ
- **THEN** show 引数の placement がオブジェクトまるごと採用される (登録経由と同じ供給優先順位)

### Requirement: インライン factory show

登録なしで factory を直接渡して表示する show を提供すること (SHALL)。インライン show はレジストリの状態を変更せず、既存の登録とも並行するインライン show とも干渉しないこと。

#### Scenario: 未登録の VM をその場で表示できる
- **GIVEN** レジストリに登録していない VM
- **WHEN** factory を直接渡すインライン show を呼ぶ
- **THEN** ダイアログが表示され、完了操作で型付き結果が返る

#### Scenario: インライン show はレジストリを汚さない
- **GIVEN** インライン show で VM を表示・完了した後
- **WHEN** 同じ VM 型を通常の (レジストリ経由の) show で表示しようとする
- **THEN** 未登録エラーになる (インライン利用が登録として残らない)

#### Scenario: 既存登録と共存する
- **GIVEN** ある VM 型が factory A でレジストリ登録済みの状態
- **WHEN** 同じ VM 型を別の factory B でインライン show し、閉じた後にレジストリ経由で show する
- **THEN** インライン表示には B が使われ、レジストリ経由の表示には A が使われる (登録は前後で不変)

#### Scenario: 並行インライン show の独立
- **GIVEN** 同じ VM 型の2つのインライン show を並行して表示した状態
- **WHEN** 一方だけを完了操作で閉じる
- **THEN** それぞれの factory / notifier / 結果は独立しており、閉じた側だけが確定し他方は表示されたまま未確定である
