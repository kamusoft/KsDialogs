# dialog-contract デルタスペック (revisit-dialog-margin-default)

## MODIFIED Requirements

### Requirement: メタ属性セットと既定値

ダイアログ契約は core/ADR-0015 の写像表に定めるメタ属性を、静的メタ `DialogOptions` (layoutArea / dialogMargin / proportionalWidth / proportionalHeight / overlayColor / isCanceledOnTouchOutside) と動的メタ `DialogPlacement` (horizontalAlignment / verticalAlignment / offsetX / offsetY) の2つの値オブジェクトとして公開し、公開する形態間で同一の意味と既定値を持たせること (SHALL)。KMP 公開面は供給経路を持つ `DialogPlacement` のみを公開する (kmp-facade spec 参照)。VM 契約に属性を持たせないこと。

全フィールドは既定値を持つ。dialogMargin の既定値は全辺 0 とすること (SHALL — core/ADR-0039。core/ADR-0008 の全辺 24 を改訂)。この既定値は、中身に dialogMargin を添付していない Dialog、Loading (既定ローディングを含む)、Toast のカスタム View に同じく効くこと (SHALL)。MAUI の Native 側の橋渡しが持つ既定値も契約の既定値と一致させること (SHALL)。

無効値 (0 以下・1 超の比率、非有限値、負の Margin 辺) は写像表の正規化規則で丸めること (SHALL)。非有限値は当該フィールドの既定値として扱うので、dialogMargin の非有限の辺は 0 になる。

#### Scenario: 既定の options の余白は全辺 0
- **GIVEN** 引数を与えずに作った `DialogOptions` (iOS / Android / MAUI) と、MAUI の橋渡しの既定の options
- **WHEN** dialogMargin を読む
- **THEN** 4 辺とも 0 である

#### Scenario: 属性を指定しない呼び出しはコンパイル互換を保つ
- **GIVEN** メタ属性を一切指定していない既存の VM・登録・show 呼び出し
- **WHEN** 本変更適用後にビルドし show する
- **THEN** 変更なしでコンパイルが通り、表示は全属性既定値の規則適合位置 (ケース表 C19) になる

#### Scenario: 余白を指定しない末尾寄せは基準領域の端に接する
- **GIVEN** dialogMargin を添付していない中身と、水平・垂直とも末尾寄せの配置
- **WHEN** show する
- **THEN** ダイアログの右端と下端は基準領域の右端と下端に一致する (ケース表 C24 / C26)

#### Scenario: Loading とカスタム Toast も余白の既定値 0 で置かれる
- **GIVEN** dialogMargin を添付していないカスタム View の Loading とカスタム Toast、および一括設定の options で余白を設定していない既定ローディング
- **WHEN** それぞれに、垂直方向を末尾寄せ・移動量 0 とする配置を show の placement 引数で渡して表示する
- **THEN** いずれも下端が基準領域の下端に一致する

#### Scenario: 無効値の正規化
- **GIVEN** 非有限値 (NaN / ±Infinity) の数値フィールドや負の dialogMargin 辺を含む添付
- **WHEN** show する
- **THEN** 実効値は正規化規則 (非有限は当該フィールドの既定値 — dialogMargin の辺は 0、負の Margin 辺は 0、比率は 0 以下を未指定・1 超を 1 へ丸め) どおりになり、表示はその実効値での規則適合位置になる

### Requirement: 配置属性と ToastStyle

配置は DialogPlacement を Dialog と同じ意味で適用する (SHALL — core/ADR-0015)。実効値の優先順は「show の placement 引数 > (カスタム View の) 添付 > ToastStyle のアプリ既定配置 > Toast の契約既定値」であり、placement はオブジェクトまるごと置換で採用する。Toast の契約既定値は、可視領域基準の下部中央に契約既定の上方向オフセットを加えた位置である (Dialog の既定 = 中央からの意図的乖離 — core/ADR-0008 の線で明示。オフセットの値はレイアウト共通ケース表に定める)。

ToastStyle は一括設定の値オブジェクトである (core/ADR-0032)。項目の適用範囲は2種に分かれる: **視覚項目** (背景色・文字色・フォントサイズ・角丸) はデフォルト View にのみ効き、カスタム View には効かない。**既定値項目** (既定 duration・アプリ既定配置) はデフォルト・カスタムを問わずすべての Toast に効く (該当引数・添付の省略時の既定として)。表示 API にスタイル引数は設けず、器は各表示の開始時に style を読み、設定変更は次の表示から効く。色を含むため KMP 共有コード (commonMain) からは設定できず、各 OS 側で設定する (LoadingStyle と同じ非対称)。

メッセージの内容は制限しない: 空文字はそのまま (内容が空のデフォルト View として) 表示され、長文は複数行に折り返す。寸法は Dialog と同じレイアウト規則 (可視領域へのクランプ) に従い、スクロール・省略表示は行わない。

デフォルト View は、ライブラリがその中身に dialogMargin 全辺 24 を添付して持つこと (SHALL — core/ADR-0039)。この余白は配置と別に読まれるので、show の placement 引数・ToastStyle のアプリ既定配置・Toast の契約既定配置のいずれで置いても効くこと (SHALL)。デフォルト View の余白を利用者が変える口は設けない。カスタム View の余白は契約の既定値 (全辺 0) で、中身への添付で変えられる。

Toast は器メタ属性のうち覆い・外側タップに関わるもの (overlayColor / isCanceledOnTouchOutside) を持たない (覆いも対話も存在しないため — core/ADR-0031)。レイアウト系の共通ケース表 (`core/layout-spec/cases.json`) は Toast の器でも適合する (覆い・外側タップに関わる観察のみ対象外に読み替え、既定配置は何も添付しないカスタム View の Toast で契約既定値のケースとして検証する)。

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

#### Scenario: [TS-AT-04] デフォルト View は契約既定配置で余白 24 の内側に置かれる
- **GIVEN** アプリ既定配置を設定していない ToastStyle
- **WHEN** 配置を渡さずに show(message) を呼ぶ
- **THEN** デフォルト View の下端は、可視領域の下端から「余白 24 + 契約既定の上方向オフセット」だけ上にある

#### Scenario: [TS-AT-05] 長いメッセージでもデフォルト View の左右に余白 24 が残る
- **GIVEN** デフォルト View 自身の最大幅の制限よりも、左右の余白 24 を引いた幅のほうが狭くなる幅の可視領域と、1 行ではその幅に収まらない長さのメッセージ
- **WHEN** show(message) を呼ぶ
- **THEN** デフォルト View の左右の端は、可視領域の左右の端からそれぞれちょうど 24 内側にある (余白の添付が無ければ端まで広がる)

#### Scenario: [TS-AT-06] 配置を渡してもデフォルト View の余白は保たれる
- **GIVEN** 垂直方向に先頭寄せの配置
- **WHEN** その配置を show の placement 引数で渡して show(message) を呼ぶ場合と、ToastStyle のアプリ既定配置に設定して配置なしで show(message) を呼ぶ場合
- **THEN** どちらもデフォルト View の上端は可視領域の上端から 24 内側にある

#### Scenario: [TS-AT-07] 何も添付しないカスタム View は余白 0 で契約既定配置に置かれる
- **GIVEN** 余白も配置も添付していないカスタム Toast View と、アプリ既定配置を設定していない ToastStyle
- **WHEN** 配置を渡さずに表示する
- **THEN** 下端は可視領域の下端から契約既定の上方向オフセットだけ上にある (余白 0 — ケース表 C23)
