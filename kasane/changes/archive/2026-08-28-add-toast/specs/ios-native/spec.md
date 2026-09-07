# ios-native デルタ (add-toast)

dialog-contract の挙動 Scenario (TS-CO/NM/MX/AT/TR) は同名テストで全量検証する (core/ADR-0016)。レイアウト共通ケース表は Toast の器でも回す (design Decision 7)。本書は Swift 公開面に固有の差分のみ。

## ADDED Requirements

### Requirement: Swift 公開面の Toast

契約 protocol `KsToast` + 既定シングルトン `Toast.shared` を追加する (SHALL — core/ADR-0002)。show は同期 (非 async)・戻り値なしで、任意スレッドから呼べ内部で MainActor へ直列化する。カスタム View の登録・インライン factory は UIKit / SwiftUI の技術別オーバーロード (KsLoading と同じ流儀)。`Toast.shared` は設定プロパティ `style` (`ToastStyle` 値オブジェクト — アプリ既定配置・既定 duration を含む) と `registry` (`ToastViewRegistry` — Dialog / Loading のレジストリとは独立) を持つ。DialogOptions 相当の設定プロパティは持たない (design Decision 1)。

#### Scenario: [TS-IO-01] 公開面の正の compile 検査
- **GIVEN** 利用者コードの立場の compile 検査ソース
- **WHEN** show(message) の各省略形・duration / placement 引数・UIKit / SwiftUI 両系統の登録・インライン factory 表示・ToastStyle の一括設定 (アプリ既定配置含む) を記述する
- **THEN** すべてコンパイルが通る

#### Scenario: [TS-IO-03] 公開面の負の compile 検査
- **GIVEN** 禁止形状ごとに独立してコンパイルを試みる検査ソース
- **WHEN** hide 相当の呼び出し / show の await・戻り値の受け取り / show へのスタイル引数 / DialogOptions 相当の設定プロパティ参照を記述する
- **THEN** いずれもコンパイルエラーになる (Non-Goals の API が公開面に漏れていない)

#### Scenario: [TS-IO-02] SwiftUI 登録のカスタム Toast が UIKit 登録と同じに働く
- **GIVEN** SwiftUI 系統の factory で登録した ToastViewModel 型
- **WHEN** 表示し、duration の経過で消えるまで待つ
- **THEN** 表示・配置・消滅の観察可能挙動が UIKit 登録と一致する
