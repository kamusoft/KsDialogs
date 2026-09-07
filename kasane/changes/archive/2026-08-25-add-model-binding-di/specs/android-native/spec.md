# android-native デルタ (add-model-binding-di)

dialog-contract の挙動 Scenario (MB-NI-*/MB-TS-*) は同名テストで全量検証する (core/ADR-0016)。本書は Kotlin 公開面に固有の差分のみ。

## ADDED Requirements

### Requirement: Kotlin 公開面の VM 供給と型指定呼び出し

VM からの notifier 取得は拡張プロパティで提供し、宣言結果型で型付けされる (SHALL)。登録は VM 引数のみの factory (`Context.(VM) → View` / Compose 系統の `(VM) → Unit` コンポーザブル) を技術別オーバーロードの両系統に追加し、従来の2引数 factory は低水準 API として残る。VM factory の登録 API と型指定 show (suspend configure 対応) を追加する。VM factory と configure は Main dispatcher で実行される (dialog-contract の UI スレッド保証の Kotlin 表現)。コンストラクタ参照が factory 型に一致する場合、追加 API なしで1行登録として成立する。参照型限定の強制はコンパイル時に表現できないため、value class の VM は登録および型指定 show の時点で構成ミスとして拒否する (インスタンス同一性が boxing で崩れるため)。

#### Scenario: [MB-AN-01] 公開面の正の compile 検査
- **GIVEN** 利用者コードの立場の compile 検査ソース
- **WHEN** 1引数 factory 登録 (View / Compose 両系統)・VM factory 登録・`vm.notifier` の宣言結果型での参照・型指定 show (configure あり / なし・suspend configure)・コンストラクタ参照による登録 (`register(VM::class, ::View)`) を記述する
- **THEN** すべてコンパイルが通る

#### Scenario: [MB-AN-02] Compose 登録でも VM 供給と型指定呼び出しが同じに働く
- **GIVEN** Compose 系統の1引数 factory と VM factory を登録した VM 型
- **WHEN** 型指定 show を configure 付きで呼び、コンポーザブルが VM 経由の notifier で報告する
- **THEN** configure で設定した状態が表示に反映され、宣言結果型の結果が返る

#### Scenario: [MB-AN-03] value class の VM は構成ミスとして拒否される
- **GIVEN** VM 契約に準拠した value class
- **WHEN** その型を登録する (または型指定 show を呼ぶ)
- **THEN** 構成ミスとして失敗する (cancelled 等の結果に化けない)
