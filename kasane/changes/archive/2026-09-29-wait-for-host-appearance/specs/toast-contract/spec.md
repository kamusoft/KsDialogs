# toast-contract デルタ (wait-for-host-appearance)

Scenario ID は `TS-HW-<NN>` (提示先の出現待ち)。挙動は Native 2 実装 (iOS / Android) の同名テストで全量を検証する (core/ADR-0016)。決定の出典は core/ADR-0041 (proposed)、実現経路は design Decision 1・2。

Toast の「提示環境の不在」の契約 (呼び出しは失敗せず、提示先の出現を待って表示する。計時は受理時点から消費し、提示先が現れないまま満了した表示は表示されずに破棄される) は変えない。本書は、中身を作る時点の定めと、待ちの挙動を ID つきで固定する分の追加。

## ADDED Requirements

### Requirement: Toast の中身は取り付けの時点で作る

Toast の中身 (View) は、提示先に取り付ける時点で作る SHALL。提示先があれば受理と同じ UI スレッド上の手番で、提示先が無ければ提示先が現れた時点で作る。型指定経路の VM factory と configure も、中身と同じ時点で実行する。

期限を過ぎた表示は、中身を作らずに破棄する SHALL。そのため、提示先が現れないまま満了した表示では、View factory も、型指定経路の VM factory と configure も、一度も呼ばれない。

登録経路の未登録の ViewModel 型は、今までどおり受理の時点で同期に失敗する (中身を作る時点とは関係しない)。

#### Scenario: [TS-HW-01] 提示先が無いまま受理された Toast は、提示先が現れた時点で表示される
- **GIVEN** 提示先が無い状態で、十分に長い duration で受理された Toast
- **WHEN** 期限の前に提示先を出現させる
- **THEN** 提示先が無い間は View factory が呼ばれず、提示先が現れた時点で中身が作られて Toast が表示される。受理時点から数えた duration の到達で消える

#### Scenario: [TS-HW-02] 提示先が現れないまま満了した Toast は、中身が作られずに破棄される
- **GIVEN** 提示先が無い状態で、短い duration で受理された型指定経路の Toast
- **WHEN** 提示先が現れないまま duration が満了する
- **THEN** Toast は表示されず、表示リストから外れる。VM factory・configure・View factory はどれも一度も呼ばれない

#### Scenario: [TS-HW-03] 期限を過ぎた保留表示は、提示先の出現が期限の処理より先でも表示されない
- **GIVEN** 提示先が無い状態で受理され、期限を過ぎた Toast
- **WHEN** 期限の処理が走る前に、提示先を出現させる
- **THEN** Toast は表示されず、中身も作られない。読み上げも起きない
