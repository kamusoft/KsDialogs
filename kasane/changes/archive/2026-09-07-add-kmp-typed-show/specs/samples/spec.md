# samples デルタ (add-kmp-typed-show)

パリティ規約 (handbook/cross/sample-parity) 準拠。Sample 専用 Scenario は scenario-id-coverage の除外 ID に登録する。デモ項目・文言・安定デモ ID は追加しない。

## ADDED Requirements

### Requirement: kmp ルートの登録経路デモを共有コードの型指定 show にする

kmp ルートの共有コード (`SamplePresenter`) で、`Model Dialog` / `Custom Loading` / `Custom Toast` (登録経路) の呼び出しを VM インスタンス渡しから型指定 show (configure で表示文言を設定) に差し替え、VM factory を共有コードで登録する (SHALL)。View factory の登録は各 OS 側のまま。観察できる挙動は差し替え前後で同一で、Native 3 ルート (2026-09-06 に型指定経路へ差し替え済み — archive の add-loading-toast-typed-show) とも同一に保つ。

#### Scenario: [PB-KS-01] Model Dialog の通し (kmp・型指定経路)
- **GIVEN** メニューを表示した kmp Sample (Android / iOS)
- **WHEN** `Model Dialog` をタップし、結果を報告する
- **THEN** 従来と同じダイアログが表示され、結果表示も従来と同じ

#### Scenario: [LD-KS-01] Custom Loading の通し (kmp・型指定経路)
- **GIVEN** メニューを表示した kmp Sample (Android / iOS)
- **WHEN** `Custom Loading` をタップする
- **THEN** 従来と同じカスタム Loading が表示され進捗が刻まれる

#### Scenario: [TS-KS-01] Custom Toast の通し (kmp・型指定経路)
- **GIVEN** メニューを表示した kmp Sample (Android / iOS)
- **WHEN** `Custom Toast` をタップする
- **THEN** 従来と同じく `カスタムトースト` (登録経路) と `インライントースト` (インライン経路は OS 側実装のまま) の 2 枚が表示され、時間経過で消える
