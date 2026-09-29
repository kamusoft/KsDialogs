# dialog-contract デルタ (wait-for-host-appearance)

Scenario ID は `PB-HW-<NN>` (提示先の出現待ち)。挙動は Native 2 実装 (iOS / Android) の同名テストで全量を検証する (core/ADR-0016)。KMP・MAUI は、Native の待ちに従うことと、各形態の打ち切りの経路を、それぞれの能力のデルタで検証する。決定の出典は core/ADR-0041 (proposed)、実現経路は design Decision 3・4。

「提示先」は、iOS では前面でアクティブなシーンの key window、Android では resumed な Activity を指す (定義は変えない)。

## MODIFIED Requirements

### Requirement: 呼び出しコンテキストの契約

show は提示 host の引数を持たず、ライブラリが提示先を自動解決する SHALL。show は任意のスレッドから呼び出せ、提示処理は内部で UI スレッドへマーシャリングされる SHALL。

アクティブな提示先が存在しない場合、show は失敗せず、提示先の出現を待ってから表示する SHALL。待ちに上限は無い。待ちの終わり方は「提示先の出現を待つ Dialog の結末」の要件が定める。

例外: 提示の仕組みそのものを持たない環境 (Native を持たない MAUI の素の .NET) では、待っても提示先は現れないため、show はその場で失敗する (maui-binding デルタ)。

#### Scenario: UI スレッド外からの show が成立する
- **GIVEN** 登録済みの VM がある
- **WHEN** UI スレッド以外のスレッド (ワーカー) から show を呼び出し、表示されたダイアログを完了操作で閉じる
- **THEN** ダイアログは正常に表示され、呼び出し元は completed(結果値) を受け取る

#### Scenario: [PB-HW-01] 提示先が無い間は待ち、現れたら表示する
- **GIVEN** アクティブな提示先が存在しない状態と、登録済みの VM
- **WHEN** show を呼び出し、しばらくしてから提示先を出現させる
- **THEN** 提示先が無い間、show は失敗も完了もせず、View factory は呼ばれない。提示先が現れた時点で View factory が呼ばれてダイアログが表示され、完了操作で閉じると呼び出し元は completed(結果値) を受け取る

## ADDED Requirements

### Requirement: 提示先の出現を待つ Dialog の結末

提示先の出現を待っている Dialog は、次のいずれかで待ちを終える SHALL。

- **提示先が現れた**: 提示先の条件を読み直し、満たしていれば中身 (View) を作って表示する。満たしていなければ待ち続ける
- **呼び出し元が打ち切った**: 一度も表示せずに終える。View factory は呼ばれない。呼び出し元への見え方は、結果通知の「呼び出し元の打ち切り」のルールに従う (iOS は cancelled を返し、Android・KMP はキャンセルの通知が伝播し、MAUI は maui-binding デルタが定める)
- **待っている間に結果が確定した** (VM が notifier で報告した): 表示せずに、確定した結果を返す

待ちの前後の順序は次のとおりとする SHALL。

- View factory の登録の解決は、待ちより前に行う。未登録の ViewModel 型は、提示先の有無にかかわらず待たずに失敗する
- 型指定 show の VM factory と configure は、待ちより前に実行する
- notifier の紐付けは、待ちより前に行う。そのため、待っている間に同じ VM インスタンスを再び show すると、既存の「表示中」の失敗になる
- 中身 (View) は、待ちの後に作る

提示先を待っている Dialog が複数あるとき、提示先が現れたら呼んだ順に 1 枚ずつ表示し、後から呼んだものが手前に重なる SHALL (多段表示の「後から出したものが手前」と同じ見え方)。前の 1 枚の提示が終わってから次の 1 枚を表示する。待っている Dialog があるうちに呼ばれた show は、提示先があってもその後ろに並び、先に待っている Dialog を追い越さない SHALL。

#### Scenario: [PB-HW-02] 待っている間に呼び出し元が打ち切ると、一度も表示されずに終わる
- **GIVEN** 提示先が無い状態で show を呼び、待っている Dialog
- **WHEN** 呼び出し元が打ち切る (iOS は Task のキャンセル、Android はコルーチンの打ち切り)。そのあと提示先を出現させる
- **THEN** iOS では show が cancelled を返し、Android ではキャンセルの通知が伝播する。View factory は一度も呼ばれず、提示先が現れてもダイアログは表示されない。VM の紐付けは解除されている

#### Scenario: [PB-HW-03] 待っている間に VM が結果を報告すると、表示されずにその結果が返る
- **GIVEN** 提示先が無い状態で show を呼び、待っている Dialog
- **WHEN** VM が notifier で completed(結果値) を報告し、そのあと提示先を出現させる
- **THEN** show は completed(結果値) を返し、View factory は一度も呼ばれず、ダイアログは表示されない

#### Scenario: [PB-HW-04] 未登録の ViewModel 型は、提示先が無くても待たずに失敗する
- **GIVEN** 提示先が無い状態と、View factory が未登録の ViewModel 型
- **WHEN** show を呼ぶ
- **THEN** show は待たずに、View factory 未登録の構成ミスとして失敗する

#### Scenario: [PB-HW-05] 待っている間の同じ VM インスタンスの再 show は「表示中」として失敗する
- **GIVEN** 提示先が無い状態で、ある VM インスタンスを show して待っている Dialog
- **WHEN** 同じ VM インスタンスで、もう一度 show を呼ぶ
- **THEN** 2 回目の show は、既存の「このインスタンスは既に表示中」の失敗になる。1 回目の待ちは続く

#### Scenario: [PB-HW-06] 待っている Dialog が複数あると、呼んだ順に表示され、後から呼んだものが手前になる
- **GIVEN** 提示先が無い状態で、異なる VM で順に show を呼んだ 2 つの Dialog (A、B の順)
- **WHEN** 提示先を出現させる
- **THEN** A、B の順に表示され、B が A の手前に重なる。それぞれが独立に結果を返す

#### Scenario: [PB-HW-08] 待っている Dialog があるうちに呼んだ show は、追い越さずに後ろに並ぶ
- **GIVEN** 提示先が現れて、待っていた Dialog A の提示が始まり、まだ終わっていない状態
- **WHEN** 別の VM で Dialog B の show を呼ぶ
- **THEN** B は A の提示が終わってから表示され、A の手前に重なる

#### Scenario: [PB-HW-07] 型指定 show の VM factory と configure は、待つ前に実行される
- **GIVEN** 提示先が無い状態と、VM factory と View factory を登録した ViewModel 型
- **WHEN** configure を付けて型指定 show を呼ぶ
- **THEN** 提示先が現れる前に、VM factory と configure が実行されている。View factory は、提示先が現れるまで呼ばれない
