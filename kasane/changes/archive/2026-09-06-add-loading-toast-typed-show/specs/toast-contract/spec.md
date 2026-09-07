# toast-contract デルタ (add-loading-toast-typed-show)

Scenario ID は `TS-TY-<NN>` (Toast の型指定 show)。挙動系は Native 2 実装の同名テストで全量検証し、MAUI はパススルー検証 (core/ADR-0009・0016)。既存の Toast 契約 (archive add-toast の TS-* Requirement) は変えない — 本書は型指定経路の追加分のみ。

## ADDED Requirements

### Requirement: Toast レジストリの VM factory スロット

Toast 専用レジストリは、VM 型キーごとに View factory と VM factory の 2 スロットを持つ (SHALL)。VM factory の登録は既存の View factory の登録と同じキーに載り、再登録はスロット単位の後勝ち。show 時の解決は呼び出し時点のスナップショットによる。VM factory は表示のたびに呼ばれ、View factory と同じ UI スレッド保証で実行される。Dialog レジストリ・Loading レジストリとは独立で、同じ VM 型を別々に登録できる。

#### Scenario: [TS-TY-01] VM factory の再登録は View factory を保持する
- **GIVEN** ある VM 型に View factory と VM factory を登録したレジストリ
- **WHEN** 同じ型に別の VM factory を再登録する
- **THEN** 以後の型指定 show は新しい VM factory で VM を作り、View factory は登録時のものが使われる (逆も同様)

### Requirement: Toast の型指定 show

呼び出し側が VM の型と、任意の同期 configure・duration・置き場所を渡すと、ライブラリが VM factory で VM を生成し configure を適用してから、登録済みの View factory で中身を作って表示する (SHALL)。動詞は show 1 本 (core/ADR-0020)。show は既存どおり fire-and-forget (戻り値なし・終了を待つ手段なし — core/ADR-0031) で、duration・置き場所・多重表示の意味はインスタンス渡し show と同じ。実行順序は「VM 生成 → configure 完了 → View 生成 → 提示」で、configure が設定した状態を View の初期化が必ず読める。

失敗モデルは既存の 3 段階に次のように収まる: **VM factory 未登録**は「VM factory 未登録」の構成ミスとして**呼び出し時点で同期に失敗**する (「解決の失敗」の段階。View factory 未登録と同じ段階で種類は区別)。**VM factory と configure の例外**は、show が同期・任意スレッド呼び出しで VM factory / configure は UI スレッドで実行されるため呼び出し元へは返せず、**「受理後の失敗」**(警告ログを残してその表示 1 枚だけを破棄し、View factory は呼ばず、他の表示・後続の show に影響しない — core/ADR-0033 の View factory の失敗と同じ扱い) に分類する。

#### Scenario: [TS-TY-09] 型指定 show は任意スレッドから呼べ、VM factory と configure は UI スレッドで実行される
- **GIVEN** View factory と VM factory を登録した VM 型
- **WHEN** バックグラウンドスレッドから型指定 show を呼ぶ
- **THEN** 呼び出しは同期に戻り、VM factory と configure は UI スレッド上で受理順に実行され、表示される

#### Scenario: [TS-TY-02] 型指定 show が生成 → configure → 表示の一連で動く
- **GIVEN** View factory と VM factory を登録した VM 型
- **WHEN** configure で状態を設定して型指定 show を呼ぶ
- **THEN** VM factory の生成物に configure の状態が入った VM で View factory が呼ばれ、その中身で Toast が表示され、duration の経過で消える

#### Scenario: [TS-TY-03] VM factory 未登録の型指定 show は呼び出し時点で失敗する
- **GIVEN** View factory だけを登録した VM 型
- **WHEN** その型で型指定 show を呼ぶ
- **THEN** 「VM factory 未登録」の構成ミスとして呼び出し時点で失敗し、表示は行われない

#### Scenario: [TS-TY-04] configure の失敗は受理後の失敗としてその 1 枚だけを破棄する
- **GIVEN** 例外を投げる configure と、別に表示中の Toast
- **WHEN** 型指定 show を呼ぶ
- **THEN** 呼び出しは同期に戻り、警告ログが残り、View factory は呼ばれず、その 1 枚は表示されない。表示中の別の Toast と後続の show には影響しない

#### Scenario: [TS-TY-05] configure 省略の型指定 show は VM factory の生成物をそのまま表示する
- **GIVEN** View factory と VM factory を登録した VM 型
- **WHEN** configure なしで型指定 show を呼ぶ
- **THEN** VM factory の生成物で View factory が呼ばれて表示される

#### Scenario: [TS-TY-06] 型指定 show でも duration と置き場所の引数が効く
- **GIVEN** View factory と VM factory を登録した VM 型
- **WHEN** duration と置き場所を指定して型指定 show を呼ぶ
- **THEN** 指定した置き場所に表示され、指定した duration の経過で消える (インスタンス渡し show と同じ)

#### Scenario: [TS-TY-07] 型指定 show は呼び出し時点のエントリで View まで作る
- **GIVEN** View factory と VM factory を登録した VM 型と、停止できる UI スレッドの受理キュー
- **WHEN** 型指定 show を呼び、受理キューが処理する前に同じ型の VM factory と View factory を再登録し、そのあとキューを進める
- **THEN** 最初に取得した VM factory の生成物と View factory の組で表示される (UI スレッド上でレジストリを引き直さない)

#### Scenario: [TS-TY-08] VM factory の失敗は受理後の失敗としてその 1 枚だけを破棄する (失敗を表明できる形態)
- **GIVEN** 例外を投げる VM factory (Kotlin / C#。Swift の VM factory は Dialog と同じく失敗を表明できないため対象外)
- **WHEN** 型指定 show を呼ぶ
- **THEN** 呼び出しは同期に戻り、警告ログが残り、configure と View factory は呼ばれず表示されない。他の表示に影響しない
