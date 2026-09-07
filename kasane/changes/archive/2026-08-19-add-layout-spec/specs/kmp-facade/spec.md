# kmp-facade デルタスペック (add-layout-spec)

改訂履歴: 2026-08-18 全面改訂 — VM 契約経由のパススルーを撤回し、commonMain の値オブジェクト + show placement 引数へ (core/ADR-0015)。options は各 OS の View 定義側で完結するため KMP 公開面の対象外。

## ADDED Requirements

### Requirement: 共有コードからの placement 指定 (KMP)

KMP 公開面は `DialogPlacement` を commonMain の具象型として公開し、show の placement 引数で供給できること (SHALL)。共有コードで指定した placement は両 OS の Native 実装へ無変換で届くこと (enum・数値の型写像は layout-semantics の写像表に従う)。`DialogOptions` は KMP 公開面に公開しないこと (供給経路が存在せず、View 定義側で完結するため) (SHALL)。公開 API 形状はコンパイル検査で固定すること。

#### Scenario: 共有コードの placement が両 OS のネイティブへ届く
- **GIVEN** show の placement 引数 (End 配置 + Offset) を指定する共有コード
- **WHEN** Android / iOS それぞれで show する
- **THEN** 各 Native 側が受け取った placement は共有コードで設定した値と一致する

#### Scenario: placement 省略時は添付と既定値がそのまま効く
- **GIVEN** placement 引数を省略した共有コードの show と、View 側に options / placement を添付した各 OS の登録
- **WHEN** show する
- **THEN** 実効値は「コンテンツ添付 > 既定値」で決まり、共有コード側は何にも関与しない

#### Scenario: 既存の共有 VM の互換性
- **GIVEN** 既存の共有 VM と placement 引数のない既存の show 呼び出し
- **WHEN** 本変更適用後にビルドし実行する
- **THEN** 変更なしでコンパイルが通り、挙動は現行と一致する
