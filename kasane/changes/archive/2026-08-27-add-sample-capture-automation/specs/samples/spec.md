# samples デルタ (add-sample-capture-automation)

パリティ規約 (cross concepts sample-parity) 準拠。Sample 専用 Scenario は scenario-id-coverage の allow-missing に登録する (機械検証は手動通し + verification 証跡。割り付けは design.md Decision 5)。撮影支援機構は画面に出ないため、デモ項目の一致要件の枠外 (sample-parity.md への追記が本 change に含まれる)。引数の外部表現・ライフサイクル・ディスパッチの設計判断は design.md Decision 1〜3。

## ADDED Requirements

### Requirement: 撮影支援設定の起動引数受け口

4ルートの Sample は、起動時の引数から撮影支援設定を読み取る (SHALL)。キー名は4ルートで同一とする (SHALL): `demo` (自動再生するデモの安定 ID) と `loading-step-interval-ms` (Loading 進捗の刻み間隔のミリ秒)。外部表現は次のとおり (SHALL):

- iOS 系アプリ: launch arguments の `--demo <id> --loading-step-interval-ms <n>` (`--キー 値` の隣接トークンペア)
- Android 系アプリ: 同名キーの string extra (`--es demo <id> --es loading-step-interval-ms <n>`)

キーの直後に値がない・空文字・検証に通らない値の場合は当該キーを無視して既定動作とし、同じキーが複数回現れた場合は最初の1組を採用する (SHALL)。引数を渡さない起動では、画面・文言・挙動が従来と一致する (SHALL)。

#### Scenario: [CA-SA-01] 引数なしの通常起動は不変
- **GIVEN** 引数を何も渡さずに起動した Sample
- **WHEN** メニューから任意のデモを操作する
- **THEN** メニュー構成・文言・デモの挙動が従来と一致し、撮影支援機構の痕跡が画面に現れない

### Requirement: 指定デモの自動再生

`demo` キーに安定デモ ID を渡して起動すると、起動直後に該当メニュー項目のタップハンドラと同じコードパスが呼ばれ、下表の期待状態になる (SHALL)。以降の操作・結果表示はメニューからの手動起動と同一とする (SHALL)。定義外の ID は無視し、通常のメニュー表示のまま起動する (SHALL)。自動再生はプロセス起動につき1回だけ発火し、画面・Activity の再生成では再発火しない (SHALL)。

安定デモ ID は次の9件で、4ルートが同じ値を各自定義する:

| 安定デモ ID | メニュー項目文言 | 起動直後の期待状態 |
|---|---|---|
| `basic-dialog` | Basic Dialog | ダイアログが表示される |
| `declarative-dialog` | Declarative Dialog | ダイアログが表示される |
| `model-dialog` | Model Dialog | ダイアログが表示される |
| `text-input-dialog` | Text Input Dialog | ダイアログが表示される |
| `inline-dialog` | Inline Dialog | ダイアログが表示される |
| `transition-dialog` | Transition Dialog | 属性パネル画面が開く |
| `layout-dialog` | Layout Dialog | 属性パネル画面が開く |
| `default-loading` | Default Loading | ローディングが開始される |
| `custom-loading` | Custom Loading | ローディングが開始される |

#### Scenario: [CA-SA-02] 各デモ ID の自動再生
- **GIVEN** 9件の安定デモ ID のそれぞれを `demo` に渡して起動した Sample
- **WHEN** 起動完了を待つ
- **THEN** 手動操作なしで上表の期待状態 (該当メニュー項目をタップしたのと同じ状態) になり、以降の操作と結果表示は手動起動と同一になる

#### Scenario: [CA-SA-03] 定義外 ID の無視
- **GIVEN** `demo` に定義外の値を渡して起動した Sample
- **WHEN** 起動完了を待つ
- **THEN** 何も自動再生されず、通常のメニューが表示される

#### Scenario: [CA-SA-07] 画面再生成で再発火しない
- **GIVEN** `demo` に安定デモ ID を渡して起動し、自動再生が完了した Sample
- **WHEN** 画面・Activity が再生成される (例: Android の画面回転)
- **THEN** デモが再度自動再生されない

### Requirement: Loading 刻み間隔の起動時指定

`loading-step-interval-ms` に 1〜600000 の整数 (ミリ秒) を渡して起動すると、Default Loading / Custom Loading デモの進捗の刻み間隔がその値になる (SHALL)。渡さない場合、および受理範囲外・数値でない値の場合は従来の既定値で動作する (SHALL)。

#### Scenario: [CA-SA-04] 刻み間隔の延長
- **GIVEN** `loading-step-interval-ms` に `2000` を渡して起動した Sample
- **WHEN** `Default Loading` と `Custom Loading` をそれぞれ通す
- **THEN** どちらも進捗の各段階が延長された間隔で更新され、各段階を撮影で捉えられる

#### Scenario: [CA-SA-05] 不正値は既定値で動作
- **GIVEN** `loading-step-interval-ms` に数値でない値を渡して起動した Sample
- **WHEN** `Default Loading` を通す
- **THEN** 従来の既定の刻み間隔で完了まで動作する

### Requirement: 撮影支援設定の4ルートパリティ

4ルートの Sample は同じキー名・同じ値の起動引数に対して同じ挙動を示す (SHALL)。

#### Scenario: [CA-SA-06] 4ルートで同じ引数・同じ挙動
- **GIVEN** 4ルートの Sample
- **WHEN** それぞれを同じキー・同じ値 (例: `demo` = `default-loading`, `loading-step-interval-ms` = `2000`) で起動する
- **THEN** 4ルートすべてで同じデモが自動再生され、画面文言は従来どおり4ルートで一致する
