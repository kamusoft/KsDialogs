# loading-contract デルタ (add-loading-toast-typed-show)

Scenario ID は `LD-TY-<NN>` (Loading の型指定 show)。挙動系は Native 2 実装の同名テストで全量検証し、MAUI はパススルー検証 (core/ADR-0009・0016)。既存の Loading 契約 (archive add-loading の LD-* Requirement) は変えない — 本書は型指定経路の追加分のみ。

## ADDED Requirements

### Requirement: Loading レジストリの VM factory スロット

Loading 専用レジストリは、VM 型キーごとに View factory と VM factory の 2 スロットを持つ (SHALL)。VM factory の登録は既存の View factory の登録と同じキーに載り、再登録はスロット単位の後勝ち (該当スロットだけを置換し他方は保持する)。show / start 時の解決は呼び出し時点のエントリのスナップショットによる。VM factory は表示のたびに呼ばれ、View factory と同じ UI スレッド保証で実行される。Dialog レジストリ・Toast レジストリとは独立で、同じ VM 型を別々に登録できる。

#### Scenario: [LD-TY-01] VM factory の再登録は View factory を保持する
- **GIVEN** ある VM 型に View factory と VM factory を登録したレジストリ
- **WHEN** 同じ型に別の VM factory を再登録する
- **THEN** 以後の型指定 show は新しい VM factory で VM を作り、View factory は登録時のものが使われる (逆に View factory を再登録しても VM factory は保持される)

#### Scenario: [LD-TY-02] 表示中の再登録は出ている Loading に影響しない
- **GIVEN** 型指定 show で表示中のカスタム Loading
- **WHEN** 同じ VM 型の VM factory と View factory を再登録する
- **THEN** 表示中の Loading の中身と進捗転送先は変わらず、次の型指定 show から新しい登録が使われる

### Requirement: Loading の型指定 show

呼び出し側が VM の型と、任意の configure (非同期可)・置き場所を渡すと、ライブラリが VM factory で VM を生成し、configure を完了させてから、登録済みの View factory で中身を作って表示し合流 1 件を開始する (SHALL)。動詞は show 1 本 (core/ADR-0020)。実行順序は「VM 生成 → configure 完了 → 進捗受け口の紐付け → View 生成 → 提示」で固定され、configure が設定した状態を View の初期化が必ず読める。合流・置き場所・進捗転送の意味はインスタンス渡し show と同じ。VM factory 未登録の型指定 show は「VM factory 未登録」の構成ミスとして失敗し、表示は行われない (View factory 未登録の失敗とは区別する)。VM factory と configure の例外 (キャンセル含む) は提示に進まず呼び出し元へ伝播し、合流 1 件として数えない。

**合流との関係**: 型指定経路は VM 生成と configure を先に済ませ、そのあとは**インスタンス渡し show とまったく同じ合流判定**に入る (合流判定は要求が coordinator に受理された時点、すなわち configure 完了後)。非同期 configure の間に別の開始が先に表示を確定していれば、この呼び出しは合流側になる。合流側になった場合、生成した VM は表示に使われず (中身は最初の開始のもの)、View factory は呼ばれない。進捗は既存契約どおり表示中のコンテンツへ受理順の後勝ちで届く。VM factory / configure の失敗は提示前の失敗なので合流数に数えない。

#### Scenario: [LD-TY-03] 型指定 show が生成 → configure → 表示の一連で動く
- **GIVEN** View factory と VM factory を登録した VM 型
- **WHEN** configure で状態を設定して型指定 show を呼ぶ
- **THEN** VM factory の生成物に configure の状態が入った VM で View factory が呼ばれ、その中身で Loading が表示され合流数が 1 になる

#### Scenario: [LD-TY-04] 非同期 configure の完了まで View 生成が始まらない
- **GIVEN** 完了を外から制御できる非同期 configure
- **WHEN** 型指定 show を呼び、configure が完了する前に View factory の呼び出し有無を観察する
- **THEN** configure 完了前は View factory が呼ばれず、完了後に呼ばれて表示される

#### Scenario: [LD-TY-05] VM factory 未登録の型指定 show は構成ミスとして失敗する
- **GIVEN** View factory だけを登録した VM 型
- **WHEN** その型で型指定 show を呼ぶ
- **THEN** 「VM factory 未登録」の構成ミスとして失敗し、表示は行われず合流数は変わらない

#### Scenario: [LD-TY-06] configure の失敗は提示に進まず伝播する
- **GIVEN** 例外を投げる configure
- **WHEN** 型指定 show を呼ぶ
- **THEN** その例外が呼び出し元へ伝播し、View factory は呼ばれず表示も合流もされない

#### Scenario: [LD-TY-07] configure 省略の型指定 show は VM factory の生成物をそのまま表示する
- **GIVEN** View factory と VM factory を登録した VM 型
- **WHEN** configure なしで型指定 show を呼ぶ
- **THEN** VM factory の生成物で View factory が呼ばれて表示される

#### Scenario: [LD-TY-08] 型指定 show で生成した VM にも進捗が転送される
- **GIVEN** 進捗受け口を実装する VM 型を View factory と VM factory で登録し、Loading を表示していない状態
- **WHEN** 型指定 start (自分が表示を開始する世代) の処理ブロックから進捗を報告する
- **THEN** 生成された VM の進捗受け口が UI スレッド上で値を受け取る

#### Scenario: [LD-TY-14] 表示中の型指定 start は既存の表示に合流し生成した VM は表示に使われない
- **GIVEN** 既定ローディングまたは登録済みカスタム Loading を表示中で、別の VM 型に View factory と VM factory を登録
- **WHEN** その型で型指定 start を呼ぶ
- **THEN** VM factory と configure は実行されるが View factory は呼ばれず、中身は最初の開始のまま合流数が 1 増え、処理ブロックの進捗は表示中のコンテンツに後勝ちで反映され、処理の終了で合流数が 1 減る

#### Scenario: [LD-TY-15] 非同期 configure の間に別の開始が表示を確定すると合流側になる
- **GIVEN** View factory と VM factory を登録した VM 型と、完了を外から制御できる非同期 configure
- **WHEN** 型指定 show を呼び、configure が完了する前に別のインスタンス渡し show が表示を開始し、そのあと configure を完了させる
- **THEN** 型指定 show 側は View factory を呼ばずに既存の表示へ合流し (合流数 2)、中身は先に開始した show のものである

### Requirement: Loading の型指定 start

処理ブロックを渡すスコープ形にも型指定版を設ける (SHALL)。VM の生成・configure・失敗の扱いは型指定 show と同じで、開始 → 処理実行 → 終了の対と戻り値の扱いは既存の start と同じ。VM factory / configure の失敗では処理ブロックを実行しない (fail-fast)。

#### Scenario: [LD-TY-09] 型指定 start が処理の戻り値を返し合流 1 件を対で数える
- **GIVEN** View factory と VM factory を登録した VM 型
- **WHEN** 型指定 start に値を返す処理ブロックを渡す
- **THEN** 処理中は Loading が表示され、処理の戻り値がそのまま返り、終了で合流 1 件が減る

#### Scenario: [LD-TY-10] 型指定 start の VM factory 未登録は処理を実行しない
- **GIVEN** View factory だけを登録した VM 型
- **WHEN** その型で型指定 start を呼ぶ
- **THEN** 「VM factory 未登録」の構成ミスとして失敗し、処理ブロックは実行されない

#### Scenario: [LD-TY-11] 型指定 show の置き場所引数が提示に渡る
- **GIVEN** View factory と VM factory を登録した VM 型
- **WHEN** 置き場所を指定して型指定 show を呼ぶ
- **THEN** 指定した置き場所で表示される (インスタンス渡し show と同じ)

#### Scenario: [LD-TY-12] 型指定 show は呼び出し時点のエントリで View まで作る
- **GIVEN** View factory と VM factory を登録した VM 型と、完了を外から制御できる非同期 configure
- **WHEN** 型指定 show を呼び、configure が完了する前に同じ型の VM factory と View factory を再登録し、そのあと configure を完了させる
- **THEN** 最初に取得した VM factory の生成物と View factory の組で表示される (再登録後の factory は使われない)

#### Scenario: [LD-TY-13] VM factory の失敗は提示に進まず伝播する (失敗を表明できる形態)
- **GIVEN** 例外を投げる VM factory (Kotlin / C#。Swift の VM factory は Dialog と同じく失敗を表明できないため対象外)
- **WHEN** 型指定 show を呼ぶ
- **THEN** その例外が呼び出し元へ伝播し、configure と View factory は呼ばれず表示も合流もされない
