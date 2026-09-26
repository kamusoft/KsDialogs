# samples デルタスペック (add-page-layout-area)

## MODIFIED Requirements

### Requirement: レイアウトデモ項目 (属性調整パネル)

4 ルートの Sample はメニューの `Layout Dialog` から属性調整パネルの画面を開き、配置 (水平 / 垂直の Start・Center・End)・移動量 (OffsetX / OffsetY)・基準領域 (Window / Visible area / Current page の 3 択) を調整して `Show` でダイアログを出せること (SHALL)。パネルの画面は下部にタブバーを持ち、パネルのタブ (`Panel`) と `Info` タブを切り替えられること (SHALL)。パネルのタブは各 OS 標準のナビゲーションバー (ページの外側にあるバー) を持ち、戻る `‹` と `Show` はそこから操作できること (SHALL)。`Info` タブはタイトルバーを持たないページで、説明文とパネルの設定をそのまま使う `Show` を持つこと (SHALL)。パネルの初期値は契約の既定値 (中央配置・移動なし・可視領域基準) と一致すること (SHALL)。文言・初期値・タブ名は `ui/brief.md` の文言表が正で、4 ルート一字一句一致すること (SHALL)。現在ページの解決は各ルートの正規の登録口で行うこと (SHALL): iOS Native と KMP (iOS) は SwiftUI の `.ksDialogCurrentPage()` を各タブの content 枠に付ける、Android Native と KMP (Android) は Compose の modifier を各タブの content 枠に付ける、MAUI は MAUI 層の既定 provider。

#### Scenario: Current page で End/End がタブバーを避ける
- **GIVEN** パネルで基準領域を Current page、水平・垂直を End にした状態
- **WHEN** `Show` する
- **THEN** ダイアログはタブバーの上に、タブバーと重ならずに表示される (4 ルート同じ位置)

#### Scenario: Visible area で End/End はタブバーに重なる
- **GIVEN** パネルで基準領域を Visible area、水平・垂直を End にした状態
- **WHEN** `Show` する
- **THEN** ダイアログはタブバーに重なって表示され、Current page のときよりタブバーの高さぶん下に出る

#### Scenario: Current page で Start/Start はナビゲーションバーの下に出る
- **GIVEN** パネルで基準領域を Current page、水平・垂直を Start にした状態
- **WHEN** パネルのタブで `Show` する
- **THEN** ダイアログはナビゲーションバーの下端から dialogMargin 分内側に出て、バーと重ならない

#### Scenario: Info タブでは画面の上端に出る
- **GIVEN** 上と同じ設定
- **WHEN** `Info` タブへ切り替えて `Show` する
- **THEN** ダイアログは可視領域の上端 (ステータスバーの下) から dialogMargin 分内側に出る (タイトルバーが無いため、パネルのタブより上に出る)

#### Scenario: Info タブへ切り替えてもパネルの設定は保たれる
- **GIVEN** パネルで配置を End/End にした状態
- **WHEN** `Info` タブへ切り替えてからパネルのタブへ戻る
- **THEN** 配置・移動量・基準領域の設定は切り替え前のままである

#### Scenario: 既存のパネル操作は変わらない
- **GIVEN** 基準領域を Visible area のままにした初期状態
- **WHEN** 配置・移動量を変えて `Show` する
- **THEN** 従来 (トグル ON) と同じ位置にダイアログが出て、結果表示はパネル内とメニューの両方に出る
