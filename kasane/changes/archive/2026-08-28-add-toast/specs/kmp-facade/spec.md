# kmp-facade デルタ (add-toast)

Native の挙動 Scenario は Native 側で全量検証し、KMP はパススルー検証とする。本書は commonMain 契約と gateway に固有の差分のみ。

## ADDED Requirements

### Requirement: commonMain 契約の Toast と Native 委譲

commonMain に Toast の契約 (`KsToast` interface + 既定エントリ) を追加する (SHALL — kmp/ADR-0002: 契約のみ commonMain、レジストリ・実体は各 Native lib へ全委譲)。共有コードから呼べるのは `show(message, duration?, placement?)` と、型キーによるカスタム表示 `show(viewModel, duration?, placement?)` である (core/ADR-0029 — View factory の登録は各 OS 側 / Swift 向け登録 API は Swift パッケージ側 — kmp/ADR-0003)。ToastStyle は色を含むため commonMain からは設定できない (LoadingStyle と同じ非対称)。duration はミリ秒の整数で、commonMain から指定できる。

#### Scenario: [TS-KM-01] 共有コードからの呼び出し面の compile 検査
- **GIVEN** commonMain の利用者コードの立場の compile 検査ソース
- **WHEN** show(message) の各省略形・duration / placement 引数・型キーによるカスタム表示を記述する
- **THEN** すべてコンパイルが通る (View factory・ToastStyle への参照は commonMain に現れない)

#### Scenario: [TS-KM-03] commonMain の負の compile 検査
- **GIVEN** 禁止形状ごとに独立してコンパイルを試みる commonMain の検査ソース
- **WHEN** ToastStyle への参照 / View factory の登録 / hide 相当の呼び出し / show の戻り値の受け取りを記述する
- **THEN** いずれもコンパイルエラーになる

#### Scenario: [TS-KM-02] commonMain の型キー表示が OS 側登録の factory で解決される
- **GIVEN** 各 OS 側で View factory を登録した共有 ToastViewModel 型
- **WHEN** commonMain のコードから型キーで show を呼ぶ
- **THEN** OS 側登録の factory によるカスタム Toast が表示される (パススルー検証)
