# dialog-contract デルタスペック

全形態 (iOS Native / Android Native / MAUI / KMP) が共有するダイアログ契約。意味論の正は concepts ([結果通知のルール](../../../../concepts/core/api/result-notification-semantics.md) / [多段表示のルール](../../../../concepts/core/api/multi-display-semantics.md))。本スペックはこの変更で実装・検証する範囲の契約を定める。各 Requirement は4形態すべてに適用される。

## ADDED Requirements

### Requirement: 型付き結果の show

VM は結果型を宣言する契約 (`DialogViewModel<R>` 相当) に準拠する SHALL。show は VM の宣言から結果型を導出する非同期操作として公開され、completed(結果値) または cancelled のいずれかをちょうど1回返して完了する SHALL。cancelled は結果値を持たない SHALL。結果を報告する部品 `DialogNotifier` は完了(値) / キャンセルの2操作を持ち、VM の宣言結果型に固定される SHALL (呼び出し側が VM と異なる結果型を指定できる API 面を公開しない — 型不一致はコンパイル時に排除され、各形態のコンパイル検証で確認する)。

#### Scenario: 完了操作で completed が返る
- **GIVEN** 登録済みの VM で show を呼び出し、ダイアログが表示されている
- **WHEN** ダイアログ側で結果値つきの完了操作を行う
- **THEN** show の呼び出しは completed(その結果値) で完了する

#### Scenario: キャンセル操作で cancelled が返る
- **GIVEN** ダイアログが表示されている
- **WHEN** ダイアログ側でキャンセル操作を行う
- **THEN** show の呼び出しは cancelled で完了する

#### Scenario: 外側タップで cancelled が返る (既定)
- **GIVEN** 既定設定のままダイアログが表示されている
- **WHEN** ダイアログの外側をタップする
- **THEN** show の呼び出しは cancelled で完了する

### Requirement: 契約 interface と既定 singleton の両対応とレジストリ共有

各形態は契約 interface (protocol) と既定 singleton エントリの両方を公開する SHALL (core/ADR-0002)。両入口は同一のレジストリを共有する SHALL (core/ADR-0004)。

#### Scenario: 片方の入口の登録がもう片方から見える
- **GIVEN** 既定 singleton エントリ経由で VM 型に View factory を登録している
- **WHEN** 契約 interface 経由 (DI 注入等) の参照で同じ VM 型の show を呼び出す
- **THEN** 登録済み factory が解決され、ダイアログが表示される

### Requirement: 結果確定とダイアログの閉鎖

結果 (completed / cancelled) を最初に確定した操作は、その show が所有するダイアログだけを閉じる SHALL。show の呼び出しはダイアログの閉鎖開始後に完了する SHALL (閉鎖アニメーションの完了タイミングは保証しない — OS 依存)。プログラムからダイアログを閉じる公開 API は本変更では提供しない (閉鎖経路は DialogNotifier の2操作のみ)。

#### Scenario: 結果確定で自分のダイアログだけが閉じる
- **GIVEN** ダイアログが2枚重なって表示されている
- **WHEN** 手前のダイアログで完了操作を行う
- **THEN** 手前のダイアログだけが閉じ、下のダイアログは表示されたまま結果未確定である

### Requirement: 呼び出しコンテキストの契約

show は提示 host の引数を持たず、ライブラリが提示先を自動解決する SHALL。show は任意のスレッドから呼び出せ、提示処理は内部で UI スレッドへマーシャリングされる SHALL。アクティブな提示 host が存在しない場合、show は各形態の throw 系チャネルで即座に失敗し、キューイングしない SHALL。

#### Scenario: UI スレッド外からの show が成立する
- **GIVEN** 登録済みの VM がある
- **WHEN** UI スレッド以外のスレッド (ワーカー) から show を呼び出し、表示されたダイアログを完了操作で閉じる
- **THEN** ダイアログは正常に表示され、呼び出し元は completed(結果値) を受け取る

#### Scenario: 提示 host 不在の show は即失敗する
- **GIVEN** アクティブな提示先画面が存在しない状態である
- **WHEN** show を呼び出す
- **THEN** show は結果を返さず throw 系チャネルで失敗し、ダイアログは表示されない

### Requirement: 結果はちょうど1回だけ確定する

最初の「完了」または「キャンセル」の報告だけが有効であり、結果確定後の報告は何もしない (no-op) SHALL。

#### Scenario: 確定後の再報告は無効
- **GIVEN** ダイアログの結果が completed で確定している
- **WHEN** 続けて完了またはキャンセルの報告を行う
- **THEN** 何も起きず、呼び出し元が受け取る結果は最初の completed のまま変わらない

### Requirement: VM 型キーによる View 解決と毎回生成

show は VM の型をキーにレジストリから View factory を解決し、View を毎回新規に生成して表示する SHALL。未登録の VM 型で show した場合は、結果を返さず throw 系チャネル (Swift = throw / Kotlin = 例外 / MAUI = faulted Task。KMP → Swift 境界では NSError 変換) で即座に失敗し、View は生成・表示されない SHALL。

#### Scenario: 登録済み VM 型の show で View が表示される
- **GIVEN** VM 型に対して View factory がレジストリに登録されている
- **WHEN** その VM のインスタンスで show を呼び出す
- **THEN** factory が生成した View がダイアログとして表示される

#### Scenario: 未登録 VM 型の show は即エラー
- **GIVEN** レジストリにその VM 型の View factory が登録されていない
- **WHEN** その VM のインスタンスで show を呼び出す
- **THEN** show は結果 (completed / cancelled) を返さず、その形態の throw 系チャネルで失敗し、View は生成・表示されない

#### Scenario: show ごとに View は新規生成される
- **GIVEN** 同じ VM 型で show を2回呼び出す
- **WHEN** それぞれのダイアログが表示される
- **THEN** 2回の表示は互いに独立した View インスタンスである

#### Scenario: 同一 VM インスタンスの再 show は独立した重ね出しになる
- **GIVEN** ある VM インスタンスで show を呼び出し、ダイアログが表示されている
- **WHEN** 同じ VM インスタンスで再度 show を呼び出す
- **THEN** 2枚目が独立したダイアログとして重ねて表示され、各 show の結果はそれぞれ独立に確定する

### Requirement: 多段表示の基本保証

ダイアログ表示中でも show を呼び出せる SHALL。重なっている場合も各 show は独立に結果を返し、手前の1枚だけがユーザー操作を受ける SHALL。ライブラリは重なりの枚数・一覧を管理せず公開しない SHALL。

#### Scenario: MD-a 2枚重ねて上から順に閉じる
- **GIVEN** show を2回呼び出し、ダイアログが2枚重なって表示されている
- **WHEN** 手前の1枚を完了操作で閉じ、続けて残る1枚を完了操作で閉じる
- **THEN** 各 show の呼び出しはそれぞれ独立に、自分のダイアログの結果値で completed を受け取る

#### Scenario: MD-c 重ね出し中の外側タップは手前のみ
- **GIVEN** ダイアログが2枚重なって表示されている
- **WHEN** 外側をタップする
- **THEN** 手前の show だけが cancelled で完了し、下のダイアログは表示されたまま結果未確定である
