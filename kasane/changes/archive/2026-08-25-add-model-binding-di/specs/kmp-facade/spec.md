# kmp-facade デルタ (add-model-binding-di)

commonMain の呼び出し面 (インスタンス渡し show) は変更しない (proposal の Non-Goals)。本書は KMP 経路の VM 供給と共有層検証の差分。

## ADDED Requirements

### Requirement: iOS KMP 面の VM 供給

iOS KMP 面の登録に VM 引数のみの factory (`(vm) → UIView`) を追加する (SHALL)。KMP の共有 VM は Swift の VM 契約 protocol に準拠しないため、notifier の取得は KMP 面の専用アクセサ (`notifier(for: vm, result:)` 相当、result 省略 = Bool) で提供する。取得の規則 (show 中のみ・インスタンス同一性・終端後は空) は dialog-contract の「notifier の VM 供給」と同一。`result:` に登録時の結果型と一致しない型を渡した場合は typed error とし (既存 KMP 面の型不一致エラーと対称)、「show 外のため空」とは区別できる。

#### Scenario: [MB-KM-01] KMP 面の1引数登録とアクセサで共有 VM の結果報告が回る
- **GIVEN** iOS 側で共有 VM 型を1引数 factory で登録した構成
- **WHEN** 共有コードから show し、Swift の View が KMP 面アクセサで取得した notifier で completed を報告する
- **THEN** 共有コードの show が宣言結果型の completed で完了する

#### Scenario: [MB-KM-04] アクセサの結果型不一致は typed error になる
- **GIVEN** Bool 以外の結果型で登録し表示中の共有 VM
- **WHEN** `result:` を省略 (= Bool) してアクセサで notifier を取得する
- **THEN** typed error が投げられ、show 外の「空」とは区別して観察できる

### Requirement: KMP Android 面の VM 供給

KMP Android 面では共有 VM が Native VM 契約の typealias であるため、Android Native の拡張プロパティ (`vm.notifier`) が追加実装なしでそのまま働く (SHALL)。

#### Scenario: [MB-KM-02] 共有 VM に対する Native 拡張の notifier で報告できる
- **GIVEN** Android 側で共有 VM 型を1引数 factory で登録した構成
- **WHEN** 共有コードから show し、View が `vm.notifier` (Native 拡張) で completed を報告する
- **THEN** 共有コードの show が宣言結果型の completed で完了する

### Requirement: 共有層からの疎結合呼び出しの実証

UI 層 (プラットフォームの View 型) を参照しない commonMain のコードから show を呼び、Native 側 View が VM 供給経由で報告した型付き結果を共有層が受け取れる (SHALL)。この経路が両 OS (Android / iOS) で成立することをテストと verification 証跡で実証する。

#### Scenario: [MB-KM-03] UI 層参照なしの共有コードが型付き結果を受け取る
- **GIVEN** プラットフォーム View 型を import しない共有 Presenter 相当のコード
- **WHEN** 共有 VM を show し、Native 側 View が VM 供給経由で結果を報告する
- **THEN** 共有コードが宣言結果型の DialogResult を受け取る (両 OS で成立)
