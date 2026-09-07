# maui-binding デルタ (add-toast)

Native の挙動 Scenario (TS-CO/NM/MX/AT/TR) は Native 側で全量検証し、MAUI はパススルー検証とする (core/ADR-0009・0016 の流儀)。本書は C# 公開面と bridge に固有の差分のみ。

## ADDED Requirements

### Requirement: C# 公開面の Toast と bridge 委譲

契約 interface `IKsToast` + 既定シングルトン `Toast` (静的エントリ) を追加する (SHALL — core/ADR-0002)。表示の実体は持たず、bridge (Swift / Kotlin) 経由で Native 実装へ全委譲する (maui/ADR-0001・0002 の構成)。**MAUI のレジストリは C# 層に持ち** (VM 型 → MAUI View factory)、Native レジストリとは層が別である — Dialog と同じく互換面専用の Toast 用 VM 型を1つだけ Native 共有レジストリに登録し、show ごとに MAUI View を platform view へ実体化して供給する (maui/ADR-0001 の構造踏襲。利用者の VM 型は Native レジストリに入らない)。デフォルト View は「contentProvider を持たない要求」として Native 側の内蔵コンテンツを指す (Loading と同じ表現)。ToastStyle は C# の値オブジェクトで受け、bridge 経由で Native の style に反映する。

#### Scenario: [TS-MA-01] 公開面の正の compile 検査
- **GIVEN** 利用者コードの立場の compile 検査ソース
- **WHEN** Show(message) の各省略形・duration / placement 引数・登録 (DI チェーンの1行登録を含む)・インライン factory 表示・ToastStyle の一括設定を記述する
- **THEN** すべてコンパイルが通る

#### Scenario: [TS-MA-03] 公開面の負の compile 検査
- **GIVEN** 禁止形状ごとに独立してコンパイルを試みる検査ソース
- **WHEN** Hide 相当の呼び出し / Show の await・戻り値の受け取り / Show へのスタイル引数を記述する
- **THEN** いずれもコンパイルエラーになる

#### Scenario: [TS-MA-02] MAUI 経由の表示が Native 直接呼び出しと同じに観察される
- **GIVEN** MAUI Sample 相当の呼び出しコード
- **WHEN** message 入口とカスタム View (登録・インライン) で表示し、duration の経過で消えるまで待つ
- **THEN** 表示・重なり・配置・消滅の観察可能挙動が Native 直接呼び出しと一致する (パススルー検証)
