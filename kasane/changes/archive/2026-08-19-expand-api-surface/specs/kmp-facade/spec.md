# kmp-facade デルタスペック (expand-api-surface)

改訂履歴: 2026-08-17 相方レビュー採用指摘の反映 — キャンセル契約 (#5)・機械面の可視性の言い換え (#6)・宣言表 compile Scenario (#1)。
2026-08-19 add-layout-spec 完了分の反映 — SwiftUI 添付 DSL の KMP 経路での有効性 Scenario を追加 (core/ADR-0015)。
2026-08-19 相方スペックレビュー (spec-002) 採用指摘の反映 — 共有コード show 引数の placement 優先 Scenario を追加。

## ADDED Requirements

### Requirement: 呼び出し元キャンセルでの閉鎖 (KMP Swift 面)

Swift 向け型付き show は、呼び出し元 Task のキャンセル時に当該ダイアログだけを閉じ、結果を cancelled としてちょうど1回確定すること (SHALL、core の結果通知契約への適合。design.md Decision 9)。

#### Scenario: Swift Task キャンセルで閉じる
- **GIVEN** Swift から型付き show を await して表示中の状態
- **WHEN** 呼び出し元 Task をキャンセルする
- **THEN** 当該ダイアログだけが閉じ、cancelled が1回だけ確定する (他の表示中ダイアログは影響を受けない)

### Requirement: Swift 向け型付き登録 (KMP)

Swift パッケージは KMP 共有 VM 向けの型付き公開登録 API を提供すること (SHALL)。結果型は型引数 (`result:` ラベル) から導出し、手動の enum 申告を公開面に出さないこと。省略時は真偽値とすること。UIView 版・SwiftUI 版の両形を提供すること。

#### Scenario: 型付き登録と show の一連
- **GIVEN** 共有 VM を `result:` 指定つきで型付き登録した状態
- **WHEN** 共有コードから show し、完了操作で閉じる
- **THEN** 共有コード側で型付き結果が得られる

#### Scenario: result 省略は真偽値
- **GIVEN** `result:` を省略して登録した共有 VM
- **WHEN** show し、真偽値つき完了操作で閉じる
- **THEN** completed(真偽値) が返る

#### Scenario: SwiftUI 添付 DSL は KMP 経路でも有効
- **GIVEN** SwiftUI 版で型付き登録した共有 VM のコンテンツに、添付 DSL (`.ksDialogPlacement(...)` 等) で属性を添付した状態
- **WHEN** 共有コードから show する
- **THEN** iOS Native 直接登録と同じ供給契約で添付値が採用される

#### Scenario: 共有コードの show 引数 placement は SwiftUI 添付 DSL より優先される
- **GIVEN** SwiftUI 版で型付き登録した共有 VM のコンテンツに、添付 DSL で placement を添付した状態
- **WHEN** 共有コード (commonMain) から別の placement を show 引数に渡して show する
- **THEN** show 引数の placement がオブジェクトまるごと採用される (供給優先順位は interop 境界を越えても不変)

### Requirement: Swift からの型付き show (KMP)

Swift パッケージは共有 VM を Swift から直接 show できる型付き入口を提供し、結果を Swift の型付き値として返すこと (SHALL)。

#### Scenario: Swift から await して型付き結果を得る
- **GIVEN** 型付き登録済みの共有 VM
- **WHEN** Swift コードから型付き show を await する
- **THEN** completed / cancelled が Swift の型付き値として返る (add-vertical-slice deviation で未実証だった経路の実証)

### Requirement: 型不一致の型付きエラー (KMP)

登録時の申告結果型と実際に報告された結果の型が一致しない場合、Swift の型付き入口は型付きエラーを throw すること (SHALL)。内部表現 (機械面の印) を公開面の結果に出さないこと。

#### Scenario: 誤申告の検出
- **GIVEN** 実際の報告型と異なる `result:` で登録した共有 VM
- **WHEN** show し、完了操作で閉じる
- **THEN** 型不一致を示すエラーが throw される (cancelled や不正な completed にならない)

### Requirement: 機械面の利用者非公開 (ABI は公開のまま)

`KsDialogsInteropBridge` は KMP cinterop 委譲専用の面として **Swift access level は public のまま維持**し (internal 化すると cinterop のリンクが成立しない)、ドキュメントコメントで「KMP cinterop 委譲専用・アプリコードから直接使用しない」ことを明示すること (SHALL)。利用者向けの登録・表示は型付き公開 API だけで完結すること。

#### Scenario: 公開経路だけで登録が完結する
- **GIVEN** KMP iOS 利用者アプリ
- **WHEN** 型付き公開 API のみで登録・表示・結果取得を行う
- **THEN** 機械面への直接参照なしで一連が成立する

#### Scenario: cinterop 委譲は引き続き成立する
- **GIVEN** KMP iosMain の cinterop 委譲経路
- **WHEN** 共有コードから show する
- **THEN** 機械面経由の委譲が従来どおり動作する (可視性の整理で KMP 側のリンクが壊れていない)

### Requirement: 宣言表どおりの Swift 公開面

Swift 向け公開面 (登録・show・エラー型) は design.md Decision 6 の宣言表どおりの形で提供すること (SHALL)。

#### Scenario: 宣言表どおりの利用コードがコンパイルできる
- **GIVEN** design.md Decision 6 の KMP Swift 宣言表の各形 (result 有無 × UIView/SwiftUI の登録、result 有無の show)
- **WHEN** 利用コード形式のコンパイル検査を書く
- **THEN** 型注釈の追加なしでコンパイルが通る
