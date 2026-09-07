# samples デルタ (add-loading-toast-typed-show)

パリティ規約 (handbook/cross/sample-parity) 準拠。Sample 専用 Scenario は scenario-id-coverage の除外 ID に登録する (機械検証は手動通し + verification 証跡)。デモ項目・文言・安定デモ ID は追加しない (既存の `Custom Loading` / `Custom Toast` の登録経路の実装を差し替えるだけ)。

## ADDED Requirements

### Requirement: Custom Loading / Custom Toast デモの登録経路を型指定 show にする

ios / android / maui の 3 ルートで、`Custom Loading` デモの登録経路の呼び出しと `Custom Toast` デモの登録経路 (`カスタムトースト`) の呼び出しを、VM インスタンス渡しから型指定 show (configure で表示文言を設定) に差し替える (SHALL)。登録は各ルートの主流儀 (ios / android: 手動登録に VM factory 登録を追加 / maui: 1 行登録のまま)。kmp ルートは本 change の対象外のためインスタンス渡しのまま。観察できる挙動は 4 ルートで同一に保ち、kmp ルートの基準は直近の archive (add-loading / add-toast) の verification 証跡を再利用する (再撮影しない)。インライン経路のデモは変えない。

#### Scenario: [LD-YS-01] Custom Loading の通し (型指定経路)
- **GIVEN** メニューを表示した Sample (ios / android / maui)
- **WHEN** `Custom Loading` をタップする
- **THEN** 従来と同じカスタム Loading が表示され進捗が刻まれる (差し替え前後で見た目・挙動が変わらない)

#### Scenario: [TS-YS-01] Custom Toast の通し (型指定経路)
- **GIVEN** メニューを表示した Sample (ios / android / maui)
- **WHEN** `Custom Toast` をタップする
- **THEN** 従来と同じく `カスタムトースト` と `インライントースト` の 2 枚が重なって表示され、時間経過で消える
