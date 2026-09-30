# toast-contract デルタ (fix-android-startup-toast-under-splash)

Scenario ID は既存の `TS-CO-<NN>` と `TS-HW-<NN>` を引き継ぐ。挙動は Native 2 実装 (iOS / Android) の同名テストで全量を検証する (core/ADR-0016)。決定の出典は core/ADR-0043 (proposed)、実現経路は design Decision 2・3。

「提示先」は、iOS では前面でアクティブなシーンの key window、Android では resumed で、かつ描画された Activity を指す (Android の定義は core/ADR-0044 と android-native デルタで変える)。「前面の待ち」は、アプリが前面にいる (画面が背面に下がっていない) のに提示先が無い間を指す。前面の判定は ios-native・android-native デルタが定める。

「Toast の公開面と fire-and-forget」は add-toast の変更 (`kasane/changes/archive/2026-08-28-add-toast/specs/dialog-contract/spec.md`) で定めた要件で、以後書き換えられていない。本書ではこれを toast-contract の側で MODIFIED にする。

## MODIFIED Requirements

### Requirement: Toast の公開面と fire-and-forget

Toast の公開面は契約 interface + 既定シングルトンエントリで提供する (SHALL — core/ADR-0002、命名は原典踏襲)。呼び出し面は、デフォルト View で表示する `show(message, duration?, placement?)` と、カスタム View を表示する登録経路 (`show(viewModel, duration?, placement?)` — 型指定レジストリで解決) およびインライン経路 (`show(viewModel, duration?, placement?, factory)` — core/ADR-0013) を持つ (core/ADR-0028・0029)。カスタム View の factory は従来 View 系 + 宣言的 UI 系の技術別オーバーロード (core/ADR-0010・0011)。

show は fire-and-forget である (core/ADR-0031): 戻り値を持たず、表示終了を待つ手段を契約に設けない。hide / メッセージ更新 / スコープ形 / 進捗口は持たない。表示は受理順に UI スレッドへ直列化して開始し、呼び出しは任意スレッドから行える。

duration はミリ秒の整数で指定する。省略 (nil) 時は ToastStyle の既定 duration (初期値 1500)。0 以下の値は無効値として style の既定に丸め、警告ログを出す。上限クランプは設けない (原典 Android の実質 3.5 秒制限は継承しない)。

**失敗モデル** (段階別):
- **解決の失敗** (登録経路で未登録の ViewModel 型): show の呼び出し時点で構成ミスとして同期に fail-fast で失敗し、表示は行われない (Dialog / Loading と同じ)。message 入口とインライン経路に解決の失敗はない
- **受理後の失敗** (factory の例外・View の実体化失敗・器の取り付け失敗): show は既に戻っているため呼び出し元へは返せない。警告ログを出してその表示だけを破棄し、器・タイマー・factory 参照などの資源を解放する (表示リストに残さない)。他の表示には影響しない
- **提示環境の不在** (提示先が無い): 呼び出しは失敗せず、提示先の出現を待って表示する。アプリが背面にある間に受理した表示は、計時を受理時点から消費しており、提示先が現れないまま duration が満了した表示は表示されずに破棄される。前面の待ちの間に受理した表示は、提示先に載った時点から計時する (「前面の待ちの間に受け付けた Toast は、提示先に載った時点から計時する」)

**duration の時間モデル**: 計時は show の受理時点から単調時計で開始し、実時間で消費する (アプリが背面にある間も進む)。ただし、前面の待ちの間に受理された表示は、提示先に載った時点、または載る前にアプリが背面へ下がった時点から計時を開始する (core/ADR-0043)。duration の到達で出の演出を開始する (入りの演出の途中でも出へ移る)。器の撤去は出のフックの完了通知後に行う (core/ADR-0017 と同じ)。有効値域は正の整数で、0 以下の show 引数は style の既定へ、style の既定自体が 0 以下なら内蔵既定 (1500) へ丸め、いずれも警告ログを出す。

**状態の共有**: レジストリと ToastStyle は 1 OS プロセス内で単一であり、Native 実装のすべての入口 (既定シングルトン・契約 interface から構築したインスタンス) が同じ状態を共有する。KMP はレジストリ実体を Native へ委譲するため Native と共有される (kmp/ADR-0002)。MAUI のレジストリは C# 層にあり Native レジストリとは層が別 (maui/ADR-0001 — 利用者の VM 型は Native レジストリに入らない)。インライン経路はレジストリの状態を一切変えない (core/ADR-0013)。

#### Scenario: [TS-CO-01] show で表示され duration 経過で自動的に消える
- **GIVEN** Toast が表示されていない状態
- **WHEN** duration を指定して show(message) を呼ぶ
- **THEN** デフォルト View の Toast が表示され、指定 duration の経過後に自動的に消える

#### Scenario: [TS-CO-02] duration 省略時は ToastStyle の既定が使われる
- **GIVEN** ToastStyle に既定 duration を設定した状態
- **WHEN** duration を省略して show(message) を呼ぶ
- **THEN** 設定した既定 duration で表示され、その経過後に消える

#### Scenario: [TS-CO-03] 0 以下の duration は style の既定に丸められる
- **GIVEN** Toast が表示されていない状態
- **WHEN** 0 以下の duration を指定して show を呼ぶ
- **THEN** 表示は行われ、style の既定 duration で消える (即時消滅・永続表示にならない)

#### Scenario: [TS-CO-04] 未登録の ViewModel 型は fail-fast
- **GIVEN** レジストリに登録のない ToastViewModel 型
- **WHEN** 登録経路の show を呼ぶ
- **THEN** 構成ミスとして失敗し、表示は行われない

#### Scenario: [TS-CO-05] インライン経路はレジストリの状態を変えない
- **GIVEN** ある ViewModel 型のレジストリ登録がある状態
- **WHEN** 同じ型でインライン経路の show を呼ぶ
- **THEN** インラインの factory が使われ (登録 factory は使われず)、呼び出し前後でレジストリの登録内容は変化しない

#### Scenario: [TS-CO-06] Native 内の異なる入口が同じレジストリと style を共有する
- **GIVEN** 既定シングルトンでレジストリ登録と ToastStyle 設定を行った状態
- **WHEN** 契約 interface から構築した別インスタンスで show を呼ぶ
- **THEN** 同じ登録・同じ style が使われる (MAUI の C# 層レジストリはこの共有の対象外 — maui/ADR-0001)

#### Scenario: [TS-CO-07] 受理後の factory 失敗は破棄と資源解放
- **GIVEN** 例外を投げる factory によるインライン経路の show
- **WHEN** show を呼び、表示処理が factory に到達する
- **THEN** 呼び出しは失敗せず、その表示だけが破棄されて表示リスト・タイマーに残らず、後続の show は正常に表示される

#### Scenario: [TS-CO-08] 計時は受理時点から進み、入りの途中でも duration 到達で出へ移る
- **GIVEN** 提示先がある状態で、入りの演出より短い duration を指定した show
- **WHEN** duration が到達する
- **THEN** 入りの完了を待たずに出へ移り、フック完了後に撤去される (表示が duration を大きく超えて残らない)

### Requirement: Toast の中身は取り付けの時点で作る

Toast の中身 (View) は、提示先に取り付ける時点で作る SHALL。提示先があれば受理と同じ UI スレッド上の手番で、提示先が無ければ提示先が現れた時点で作る。型指定経路の VM factory と configure も、中身と同じ時点で実行する。

期限を過ぎた表示は、中身を作らずに破棄する SHALL。そのため、提示先が現れないまま満了した表示では、View factory も、型指定経路の VM factory と configure も、一度も呼ばれない。

登録経路の未登録の ViewModel 型は、今までどおり受理の時点で同期に失敗する (中身を作る時点とは関係しない)。

#### Scenario: [TS-HW-01] 背面で受理された Toast は、提示先が現れた時点で表示され、受理時点から数えた duration で消える
- **GIVEN** アプリが背面にあり提示先が無い状態で、十分に長い duration で受理された Toast
- **WHEN** 期限の前に、アプリが前面に戻って提示先が現れる
- **THEN** 提示先が無い間は View factory が呼ばれず、提示先が現れた時点で中身が作られて Toast が表示される。受理時点から数えた duration の到達で消える

#### Scenario: [TS-HW-02] 提示先が現れないまま満了した Toast は、中身が作られずに破棄される
- **GIVEN** アプリが背面にあり提示先が無い状態で、短い duration で受理された型指定経路の Toast
- **WHEN** 提示先が現れないまま duration が満了する
- **THEN** Toast は表示されず、表示リストから外れる。VM factory・configure・View factory はどれも一度も呼ばれない

#### Scenario: [TS-HW-03] 期限を過ぎた保留表示は、提示先の出現が期限の処理より先でも表示されない
- **GIVEN** アプリが背面にあり提示先が無い状態で受理され、期限を過ぎた Toast
- **WHEN** 期限の処理が走る前に、アプリが前面に戻って提示先が現れる
- **THEN** Toast は表示されず、中身も作られない。読み上げも起きない

## ADDED Requirements

### Requirement: 前面の待ちの間に受け付けた Toast は、提示先に載った時点から計時する

前面の待ちの間に受理された Toast は、受理の時点では期限を決めず、提示先に載った時点から duration を数える SHALL (core/ADR-0043)。前面の待ちは、画面を開いている途中 (起動・背面からの復帰・画面の切り替え・作り直し) と、割り込み (システムの許可ダイアログなど) で提示先を一時的に失っている間の両方を含む。

- 載る前にアプリが背面へ下がったら、下がった時点から duration を数える SHALL。その期限の前に提示先が現れれば、その期限まで表示する
- 「前面の待ちの間に受理された」かどうかは、その表示の開始処理を UI スレッドで行う手番の状態で判定する (受理順に UI スレッドで行う開始処理 — 「Toast の公開面と fire-and-forget」)。show を呼んでから開始処理までの間に前面・背面が変わったら、開始処理の時点の状態に従う
- 一度決めた期限は、載せ直し (作り直し・提示先の入れ替わり) をまたいでも動かさない
- 待っている間は中身を作らない (「Toast の中身は取り付けの時点で作る」)

#### Scenario: [TS-HW-04] 前面の待ちの間に受理された Toast は、duration より長く待っても破棄されず、載った時点から数えた duration で消える
- **GIVEN** アプリが前面にいて提示先が無い状態で、duration D で受理された Toast
- **WHEN** D より長く提示先が無いまま置き、そのあと提示先を出現させる
- **THEN** 待っている間は View factory が呼ばれず、Toast は破棄されない。提示先が現れた時点で中身が作られて表示され、載った時点から数えた D の到達で消える

#### Scenario: [TS-HW-05] 前面の待ちのまま背面へ下がった Toast は、下がった時点から数え始める
- **GIVEN** アプリが前面にいて提示先が無い状態で、duration D で受理された Toast
- **WHEN** D より長く前面の待ちのまま置いてから、提示先が現れないままアプリが背面へ下がり、さらに D が経過する
- **THEN** 背面へ下がるまでは破棄されず、下がった時点から数えた D の到達で、表示されずに破棄される。中身は一度も作られない

#### Scenario: [TS-HW-06] 背面へ下がった後、期限の前に提示先が現れたら、下がった時点から数えた期限まで表示される
- **GIVEN** 前面の待ちのまま背面へ下がった、duration D の Toast
- **WHEN** 下がってから D に達する前に、アプリが前面に戻って提示先が現れる
- **THEN** 提示先が現れた時点で表示され、背面へ下がった時点から数えた D の到達で消える
