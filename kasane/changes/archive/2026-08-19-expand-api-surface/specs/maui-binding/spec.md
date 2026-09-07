# maui-binding デルタスペック (expand-api-surface)

改訂履歴: 2026-08-19 add-layout-spec 完了分の反映 — negative compile check の検査方式参照を新方式 (`KsDialogs.Maui.ApiSurfaceCheck`) へ更新 (旧 NegativeCompileChecks 方式は add-layout-spec で撤去済み)。

## ADDED Requirements

### Requirement: bool 既定の登録 (MAUI)

非ジェネリックの VM 契約 (真偽値結果) と、VM 型のみを型引数に取る Register オーバーロードを提供すること (SHALL)。既存の2型引数 Register と共存し、オーバーロード解決が曖昧にならないこと。

#### Scenario: 全呼び出し形式の compile 検査
- **GIVEN** 次の4形式の登録コード — (1) `Register<TViewModel, TResult>(...)` (2) `Register<TViewModel>(...)` (3) 明示型付きラムダによる型引数なしの `Register((TViewModel vm, DialogNotifier<bool> n) => ...)` (4) カスタム結果型の明示型付きラムダ推論呼び出し
- **WHEN** コンパイル検査を書く
- **THEN** 4形式すべてが曖昧エラーなしでコンパイルが通る

#### Scenario: 意図しない形式は negative compile check で拒否される
- **GIVEN** bool 既定オーバーロードにカスタム結果型 VM を渡す誤用コード
- **WHEN** negative compile check (公開 API 形状検査 `KsDialogs.Maui.ApiSurfaceCheck` の禁止形状別 `KsDialogsNegativeCheck*` フラグ方式。回し方は `concepts/cross/conventions/test-execution.md`) を書く
- **THEN** コンパイルエラーになることが検査で固定される

#### Scenario: 型引数1つの登録の動作
- **GIVEN** 非ジェネリック契約で宣言した VM 型を `Register<TViewModel>` で登録した状態
- **WHEN** show し、完了操作で閉じる
- **THEN** show の戻りは completed(真偽値) の型付き結果になる

### Requirement: インライン show (MAUI)

factory を直接渡すインライン ShowAsync を提供すること (SHALL)。

#### Scenario: 未登録 VM のインライン表示
- **GIVEN** 未登録の VM
- **WHEN** factory を渡すインライン ShowAsync を呼ぶ
- **THEN** 表示・完了操作・型付き結果の一連が成立する
