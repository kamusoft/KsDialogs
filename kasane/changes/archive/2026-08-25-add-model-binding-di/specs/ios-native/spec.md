# ios-native デルタ (add-model-binding-di)

dialog-contract の挙動 Scenario (MB-NI-*/MB-TS-*) は同名テストで全量検証する (core/ADR-0016)。本書は Swift 公開面に固有の差分のみ。

## ADDED Requirements

### Requirement: Swift 公開面の VM 供給と型指定呼び出し

VM 契約 protocol は参照型 (AnyObject) 制約を持つ (SHALL)。VM からの notifier 取得は protocol extension のプロパティで提供し、宣言結果型で型付けされる。登録は VM 引数のみの factory (`(vm) → UIView` / `(vm) → some View`) を技術別オーバーロード (core/ADR-0011) の両系統に追加し、従来の2引数 factory は低水準 API として残る。VM factory の登録 API と型指定 show (async configure 対応) を追加する。VM factory と configure は MainActor で実行される (dialog-contract の UI スレッド保証の Swift 表現)。

#### Scenario: [MB-IO-01] 公開面の正の compile 検査
- **GIVEN** 利用者コードの立場の compile 検査ソース
- **WHEN** 1引数 factory 登録 (UIKit / SwiftUI 両系統)・VM factory 登録・`vm.notifier` の宣言結果型での参照・型指定 show (configure あり / なし・async configure) を記述する
- **THEN** すべてコンパイルが通る

#### Scenario: [MB-IO-02] 値型 VM の準拠は拒否される
- **GIVEN** struct で VM 契約に準拠しようとする negative check ソース
- **WHEN** コンパイルする
- **THEN** AnyObject 制約違反としてコンパイルが拒否される

#### Scenario: [MB-IO-03] SwiftUI 登録でも VM 供給と型指定呼び出しが同じに働く
- **GIVEN** SwiftUI 系統の1引数 factory と VM factory を登録した VM 型
- **WHEN** 型指定 show を configure 付きで呼び、SwiftUI コンテンツが VM 経由の notifier で報告する
- **THEN** configure で設定した状態が表示に反映され、宣言結果型の結果が返る
