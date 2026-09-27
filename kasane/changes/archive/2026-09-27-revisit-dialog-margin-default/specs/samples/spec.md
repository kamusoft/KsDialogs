# samples デルタスペック (revisit-dialog-margin-default)

## ADDED Requirements

### Requirement: Layout Dialog のパネルで余白を選べる

4 ルート (iOS Native / Android Native / MAUI / KMP) の Layout Dialog の属性調整パネルは、全辺そろえの余白 (dialogMargin) を選ぶ行を持つこと (SHALL)。パネルの `Show` (Panel タブ・Info タブとも) で出すダイアログには、選んだ余白を中身への添付として渡すこと (SHALL — 余白は show の引数では渡せない静的メタ属性のため、基準領域と同じく ViewModel から View factory の添付へ流す)。初期値は契約の既定値 (0) とし、タブを切り替えても保つこと (SHALL)。行の文言・選択肢・読み上げは ui/brief.md の文言表と承認モックに従い、4 ルートで一致させること (SHALL — handbook `kasane/handbook/cross/sample-parity.md`)。

#### Scenario: 初期値のまま出すと基準領域の端に接する
- **GIVEN** パネルを開いた直後 (余白は初期値の 0) に、基準領域を Current page、水平・垂直とも End に変えた状態
- **WHEN** `Show` を押す
- **THEN** ダイアログの下端はタブバーの上端に、右端は画面の右端に接する

#### Scenario: 余白を変えると次の表示から効く
- **GIVEN** 前の Scenario と同じ配置と基準領域で、余白を 24 に変えた状態
- **WHEN** `Show` を押す
- **THEN** ダイアログの下端はタブバーの上端から、右端は画面の右端から、それぞれ 24 内側に出る

#### Scenario: タブを切り替えても余白は保たれる
- **GIVEN** Panel タブで余白を 24 にし、基準領域を Current page、水平・垂直とも Start にした状態
- **WHEN** Info タブに切り替えて `Show` を押す
- **THEN** ダイアログは画面の上端 (ステータスバーの下) と左端から 24 内側に出て、Panel タブに戻っても余白は 24 のまま
