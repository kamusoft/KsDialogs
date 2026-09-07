# dialog-contract デルタ (add-toast)

Scenario ID は `TS-<領域>-<NN>` (design Decision 7)。挙動系は両 Native のテスト名に ID を含めて対応付ける (core/ADR-0016)。根拠決定: core/ADR-0028〜0032。

## ADDED Requirements

### Requirement: Toast の公開面と fire-and-forget

Toast の公開面は契約 interface + 既定シングルトンエントリで提供する (SHALL — core/ADR-0002、命名は原典踏襲)。呼び出し面は、デフォルト View で表示する `show(message, duration?, placement?)` と、カスタム View を表示する登録経路 (`show(viewModel, duration?, placement?)` — 型指定レジストリで解決) およびインライン経路 (`show(viewModel, duration?, placement?, factory)` — core/ADR-0013) を持つ (core/ADR-0028・0029)。カスタム View の factory は従来 View 系 + 宣言的 UI 系の技術別オーバーロード (core/ADR-0010・0011)。

show は fire-and-forget である (core/ADR-0031): 戻り値を持たず、表示終了を待つ手段を契約に設けない。hide / メッセージ更新 / スコープ形 / 進捗口は持たない。表示は受理順に UI スレッドへ直列化して開始し、呼び出しは任意スレッドから行える。

duration はミリ秒の整数で指定する。省略 (nil) 時は ToastStyle の既定 duration (初期値 1500)。0 以下の値は無効値として style の既定に丸め、警告ログを出す。上限クランプは設けない (原典 Android の実質 3.5 秒制限は継承しない)。

**失敗モデル** (段階別):
- **解決の失敗** (登録経路で未登録の ViewModel 型): show の呼び出し時点で構成ミスとして同期に fail-fast で失敗し、表示は行われない (Dialog / Loading と同じ)。message 入口とインライン経路に解決の失敗はない
- **受理後の失敗** (factory の例外・View の実体化失敗・器の取り付け失敗): show は既に戻っているため呼び出し元へは返せない。警告ログを出してその表示だけを破棄し、器・タイマー・factory 参照などの資源を解放する (表示リストに残さない)。他の表示には影響しない
- **提示環境の不在** (取り付け先ウィンドウ / resumed Activity が無い): 呼び出しは失敗せず、提示先の出現を待って表示する。計時は受理時点から消費しており、提示先が現れないまま duration が満了した表示は表示されずに破棄される

**duration の時間モデル**: 計時は show の受理時点から単調時計で開始し、実時間で消費する (アプリが背面にある間も進む)。duration の到達で出の演出を開始する (入りの演出の途中でも出へ移る)。器の撤去は出のフックの完了通知後に行う (core/ADR-0017 と同じ)。有効値域は正の整数で、0 以下の show 引数は style の既定へ、style の既定自体が 0 以下なら内蔵既定 (1500) へ丸め、いずれも警告ログを出す。

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
- **GIVEN** 入りの演出より短い duration を指定した show
- **WHEN** duration が到達する
- **THEN** 入りの完了を待たずに出へ移り、フック完了後に撤去される (表示が duration を大きく超えて残らない)

### Requirement: 完全非対話と非モーダル

Toast は表示中、いかなる入力も奪わない (SHALL — core/ADR-0031)。Toast の面へのタッチは背後のページ要素へ素通しされ、ページ要素は Toast の真下でも通常どおり操作できる。Toast はタッチで消えず、カスタム View 内に対話可能な部品を置いても反応しない。消滅の契機は duration の経過のみである。

#### Scenario: [TS-NM-01] Toast の真下の要素が操作できる
- **GIVEN** 操作可能な要素の真上に重なる位置に表示した Toast
- **WHEN** Toast の面 (要素と重なる位置) をタップする
- **THEN** 背後の要素が反応し、Toast は消えずに duration まで表示され続ける

#### Scenario: [TS-NM-02] カスタム View 内の対話部品は反応しない
- **GIVEN** タップ反応を記録する部品を含むカスタム Toast View の表示
- **WHEN** その部品をタップする
- **THEN** 部品は反応せず (タッチは背後へ素通し)、Toast は duration まで表示され続ける

### Requirement: 多重表示と表示の継続

Toast は多重起動できる (SHALL)。同時に複数の show が行われた場合、すべて表示され、重なり順は起動順 (後に起動したものが手前) である。重なりは z-order のみであり、同一の実効配置の Toast は同じ位置に重なる — 器が自動で位置をずらす可視スタックは行わない (2026-08-27 オーナー確定。core/ADR-0030 の「自前のスタック管理はしない」と整合)。各表示は自分の duration で独立に消え、他の表示の開始・消滅に影響されない (合流・置き換え・キューイングは行わない)。

Toast の表示はページ遷移をまたいで継続する (器は OS の提示スタックに参加しない — core/ADR-0030)。表示中のウィンドウ寸法・insets の変化 (回転を含む) では Dialog と同じ規則 (凍結済み実効値での再配置) に従って表示を継続し、残り duration は維持される (巻き戻らない)。

**機能間の前後関係**: Loading と Toast が同時に表示される場合、起動順によらず常に Loading が前面である (SHALL — core/ADR-0030)。Dialog と Toast の前後関係は契約で保証しない (core/ADR-0006 の線)。

#### Scenario: [TS-MX-01] 多重起動はすべて表示され起動順に重なる
- **GIVEN** Toast が表示されていない状態
- **WHEN** 異なるメッセージで show を3回続けて呼ぶ
- **THEN** 3枚すべてが表示され、後に起動したものが手前に重なる

#### Scenario: [TS-MX-02] 各表示は自分の duration で独立に消える
- **GIVEN** 異なる duration の2枚の Toast を表示した状態
- **WHEN** 短い方の duration が経過する
- **THEN** 短い方だけが消え、長い方は自分の duration まで表示され続ける

#### Scenario: [TS-MX-03] ページ遷移をまたいで表示が継続する
- **GIVEN** Toast を表示した画面
- **WHEN** 別の画面へ遷移する
- **THEN** Toast は遷移後も表示され続け、duration の経過で消える

#### Scenario: [TS-MX-04] 回転しても表示が継続し残り時間は維持される
- **GIVEN** Toast を表示した状態
- **WHEN** 画面を回転する
- **THEN** 表示は途切れず継続し (Android では器の作り直しをまたいで)、残り duration は巻き戻らずに消える

#### Scenario: [TS-MX-05] Loading は起動順によらず Toast より前面
- **GIVEN** Loading 表示中に Toast を出した状態と、Toast 表示中に Loading を出した状態のそれぞれ
- **WHEN** 両者が同時に表示されている
- **THEN** どちらの順でも Loading が Toast より前面に表示される (実提示での検証 — 両 OS)

### Requirement: 配置属性と ToastStyle

配置は DialogPlacement を Dialog と同じ意味で適用する (SHALL — core/ADR-0015)。実効値の優先順は「show の placement 引数 > (カスタム View の) 添付 > ToastStyle のアプリ既定配置 > Toast の契約既定値」であり、placement はオブジェクトまるごと置換で採用する。Toast の契約既定値は、可視領域基準の下部中央に契約既定の上方向オフセットを加えた位置である (Dialog の既定 = 中央からの意図的乖離 — core/ADR-0008 の線で明示。オフセットの値はレイアウト共通ケース表に定める)。

ToastStyle は一括設定の値オブジェクトである (core/ADR-0032)。項目の適用範囲は2種に分かれる: **視覚項目** (背景色・文字色・フォントサイズ・角丸) はデフォルト View にのみ効き、カスタム View には効かない。**既定値項目** (既定 duration・アプリ既定配置) はデフォルト・カスタムを問わずすべての Toast に効く (該当引数・添付の省略時の既定として)。表示 API にスタイル引数は設けず、器は各表示の開始時に style を読み、設定変更は次の表示から効く。色を含むため KMP 共有コード (commonMain) からは設定できず、各 OS 側で設定する (LoadingStyle と同じ非対称)。

メッセージの内容は制限しない: 空文字はそのまま (内容が空のデフォルト View として) 表示され、長文は複数行に折り返す。寸法は Dialog と同じレイアウト規則 (可視領域へのクランプ) に従い、スクロール・省略表示は行わない。

Toast は器メタ属性のうち覆い・外側タップに関わるもの (overlayColor / isCanceledOnTouchOutside) を持たない (覆いも対話も存在しないため — core/ADR-0031)。レイアウト系の共通ケース表 (`core/layout-spec/cases.json`) は Toast の器でも適合する (覆い・外側タップに関わる観察のみ対象外に読み替え、既定配置は Toast の契約既定値のケースで検証する)。

#### Scenario: [TS-AT-01] placement 引数で配置が変わる
- **GIVEN** 契約既定値と異なる配置を持つ placement
- **WHEN** placement 引数付きで show(message) を呼ぶ
- **THEN** 表示位置がレイアウト規則どおりに変わる

#### Scenario: [TS-AT-02] 優先順は show 引数 > 添付 > style 既定 > 契約既定
- **GIVEN** placement を添付したカスタム Toast View と、アプリ既定配置を設定した ToastStyle
- **WHEN** 添付のみ / style のみ (デフォルト View) / show 引数付き、のそれぞれで表示する
- **THEN** 添付のみでは添付値、style のみでは style 値、引数付きでは引数値が (まるごと置換で) 採用される

#### Scenario: [TS-AT-03] style の変更は次の表示から効く
- **GIVEN** デフォルト View の Toast を表示中の状態
- **WHEN** ToastStyle を変更し、新しい Toast を表示する
- **THEN** 表示中の Toast は変わらず、新しい Toast にだけ変更後の style が反映される

### Requirement: 出入りの演出の適用

カスタム Toast View には DialogTransition の添付が Dialog / Loading と同じ意味で効く (SHALL — core/ADR-0017 のフック機構。ただし Toast は結果を持たないため結果のラッチと配送は対象外)。添付を省略したカスタム View とデフォルト View は器の既定演出で出入りする。デフォルト View に演出を選択する口は設けない (phase-8 決定事項)。

#### Scenario: [TS-TR-01] カスタム View の演出フックが両局面で呼ばれる
- **GIVEN** presentation / dismissal 両フックを添付したカスタム Toast View
- **WHEN** 表示し、duration の経過で消えるまで待つ
- **THEN** 入りでpresentation フック、出で dismissal フックがホスト View を渡されて呼ばれ、完了通知の後に撤去される

#### Scenario: [TS-TR-02] デフォルト View は器の既定演出で出入りする
- **GIVEN** デフォルト View の Toast
- **WHEN** 表示し、消えるまで待つ
- **THEN** 器の既定演出で出入りし、撤去まで完了する (演出起因で表示が残らない)

### Requirement: 支援技術への通知

デフォルト View の Toast は、表示時にメッセージを OS の支援技術 (VoiceOver / TalkBack) へ通知 (announce) する (SHALL)。Toast の表示は支援技術のフォーカスを移動させない (完全非対話 — core/ADR-0031 — と整合)。カスタム View の読み上げ内容・accessibility 属性は View を供給するアプリの責務であり、器は通知もフォーカス移動も行わない。

#### Scenario: [TS-AC-01] デフォルト View はメッセージを announce しフォーカスを奪わない
- **GIVEN** 支援技術の通知を観察できるテスト環境
- **WHEN** show(message) でデフォルト View の Toast を表示する
- **THEN** メッセージの announce イベントが発行され、accessibility フォーカスは移動しない
