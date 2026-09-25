# dialog-contract デルタスペック (add-page-layout-area)

## ADDED Requirements

### Requirement: 基準領域「表示中のページ」

基準領域 `layoutArea` は `window` / `visibleArea` に加えて `currentPage` (表示中のページ) を選べること (SHALL)。`currentPage` の基準矩形は「表示中ページの矩形のうち、そのページ自身の safe area / システム insets の内側」とし、窓座標の 4 辺 inset に変換して既存の rect 決定手順 (基準 rect R → 有効領域 A → サイズ選択 → クランプ → anchor → offset) にそのまま流すこと (SHALL)。水平・垂直の両軸に効くこと (SHALL。原典の垂直のみは踏襲しない — core/ADR-0008・0038)。既定値は従来どおり `visibleArea` で、既存の指定の結果は変わらないこと (SHALL)。

#### Scenario: タブバーを持つページで End 配置がタブバーを避ける
- **GIVEN** 下部にタブバーを持つ画面の表示中ページと、`layoutArea = currentPage`・水平 End・垂直 End の添付
- **WHEN** show する
- **THEN** ダイアログの下端はページの矩形の下端 (タブバーの上端) から dialogMargin 分内側になり、タブバーと重ならない

#### Scenario: 同じ画面で visibleArea はタブバーに重なる
- **GIVEN** 同じ画面で `layoutArea = visibleArea`・水平 End・垂直 End の添付
- **WHEN** show する
- **THEN** ダイアログの下端は可視領域の下端から dialogMargin 分内側になり、タブバーの高さぶん `currentPage` の結果より下に出る

#### Scenario: 全画面のページでは可視領域と一致する
- **GIVEN** タブバー・ナビゲーションバーを持たない全画面のページと `layoutArea = currentPage`
- **WHEN** show する
- **THEN** 最終 rect は `layoutArea = visibleArea` のときと一致する

#### Scenario: 比率サイズもページの矩形を基準にする
- **GIVEN** タブバーを持つページと `layoutArea = currentPage`・proportionalHeight = 0.5
- **WHEN** show する
- **THEN** 高さはページの矩形 (タブバーを除く) の高さの 5 割になる

#### Scenario: バーの下まで伸びるページでも safe area の内側を採る
- **GIVEN** ページの view が半透明のバーの下まで伸びていて、ページ自身の safe area がバーの分だけ狭い画面と `layoutArea = currentPage`・垂直 End
- **WHEN** show する
- **THEN** ダイアログの下端はページの safe area の下端 (バーの上端) を基準にし、view の矩形の下端 (バーの下) を基準にしない

### Requirement: 現在ページは登録された provider から得る

器は表示中のページを自分で探索せず、アプリ (ホスト) が一度登録した「現在ページ provider」に問い合わせて矩形を得ること (SHALL)。provider は各表示の開始時と、窓寸法・insets の変化による再配置のたびに問い合わせること (SHALL)。登録の差し替えは次の表示から効き、表示中のダイアログには影響しないこと (SHALL)。provider の登録口は形態ごとに 1 つ — UIKit / Android View は「ページの View を返す関数」、SwiftUI / Compose は「付けた View / composable を現在ページとして名乗らせる modifier」— とし、「窓座標の矩形を返す provider」は内部の共通型として公開しないこと (SHALL)。既定 provider を持つ形態 (iOS Native / MAUI) でもアプリは上書きできること (SHALL)。取得元が複数あるときの優先順位は **modifier の台帳 > 登録した provider > 既定 provider** とし、上位の取得元が候補を持たない・`null` を返す・例外を投げる・空の矩形を返すときは次の取得元へ進むこと (SHALL)。provider が返す View と modifier の候補は、**今回ダイアログを出す提示先と同じ window (Android は同じ Activity のウィンドウ) に属するもの**に限り、別ウィンドウに属するものは候補にしないこと (SHALL)。

#### Scenario: 登録した provider の View が基準になる
- **GIVEN** ページの View を返す provider を登録したホストと `layoutArea = currentPage`
- **WHEN** show する
- **THEN** 最終 rect はその View の矩形 (safe area / insets の内側) を基準に決まる

#### Scenario: 登録の差し替えは次の表示から
- **GIVEN** 表示中のダイアログ
- **WHEN** provider を別の View を返すものに差し替える
- **THEN** 表示中のダイアログは動かず、次に show したダイアログから新しい View が基準になる

#### Scenario: 窓の寸法が変わると provider に問い合わせ直す
- **GIVEN** `layoutArea = currentPage` で表示中のダイアログ
- **WHEN** 画面を回転し、ページの矩形が変わる
- **THEN** ダイアログは回転後のページの矩形を基準に再配置される (`PB-WN-01` と同じ関門)

#### Scenario: modifier の台帳が登録 provider に勝つ
- **GIVEN** ページの View を返す provider を登録し、さらに別の枠に modifier を付けた画面
- **WHEN** `layoutArea = currentPage` で show する
- **THEN** modifier を付けた枠の矩形が基準になる

#### Scenario: 台帳が空なら登録 provider へ進む
- **GIVEN** 上の状態から modifier を付けた枠が画面から外れた状態
- **WHEN** show する
- **THEN** 登録 provider が返す View の矩形が基準になる

#### Scenario: 登録 provider が null なら既定 provider へ進む
- **GIVEN** 既定 provider を持つ形態で、`null` を返す provider を登録した状態
- **WHEN** show する
- **THEN** 既定 provider の結果が基準になる (可視領域へ落ちるのは既定 provider も解決できないとき)

### Requirement: 未解決時は可視領域へ落ちる

すべての取得元を辿っても現在ページが得られないとき (未登録、`null`、例外、返した View が提示先の窓に載っていない、矩形が空) は、`layoutArea = visibleArea` と同じ結果で表示すること (SHALL)。表示を失敗させないこと (SHALL)。未解決の理由は診断ログに出すこと (SHALL。文言は handbook/cross/diagnostic-message-language.md の規約に従う)。

#### Scenario: 未登録なら可視領域
- **GIVEN** provider を登録していないホスト (既定 provider を持たない形態) と `layoutArea = currentPage`
- **WHEN** show する
- **THEN** 最終 rect は `visibleArea` のときと一致し、未登録を示す診断ログが 1 件出る

#### Scenario: provider が null を返す
- **GIVEN** 既定 provider を持たない形態で `null` を返す provider を登録したホストと `layoutArea = currentPage`
- **WHEN** show する
- **THEN** 最終 rect は `visibleArea` のときと一致する

#### Scenario: provider が例外を投げても表示は失敗しない
- **GIVEN** 既定 provider を持たない形態で例外を投げる provider を登録したホストと `layoutArea = currentPage`
- **WHEN** show する
- **THEN** ダイアログは `visibleArea` の位置に表示され、show の結果契約 (completed / cancelled) は通常どおり返る

### Requirement: modifier の台帳規則 (SwiftUI / Compose 共通)

宣言的 UI の登録口 (modifier) は、付けた View / composable が画面に attach されている間だけ台帳に載せること (SHALL)。現在ページの決定は次の順で行うこと (SHALL): (1) 提示先と別の窓に属するもの、および窓と重なっていない矩形を候補から外す、(2) 候補の矩形が入れ子なら内側 (小さい方) を採る、(3) それ以外は最後に配置されたものを採る。detach したものは台帳から外し、残りの候補で決め直すこと (SHALL)。台帳が空なら未解決として可視領域へ落ちること (SHALL)。

#### Scenario: 1 つだけ付けたとき
- **GIVEN** 画面の content 枠に modifier を 1 つ付けた画面と `layoutArea = currentPage`
- **WHEN** show する
- **THEN** その枠の矩形が基準になる

#### Scenario: 画面遷移で付けた画面が消えたら残りへ戻る
- **GIVEN** modifier を付けた画面 A から、付けていない画面 B へ遷移し、A が画面から外れた状態
- **WHEN** show する
- **THEN** 台帳は空になり、最終 rect は `visibleArea` のときと一致する

#### Scenario: 戻る遷移で再び付けた画面が基準になる
- **GIVEN** 上の状態から画面 A へ戻り、A が再び配置された状態
- **WHEN** show する
- **THEN** A の矩形が基準になる

#### Scenario: 入れ子は内側が勝つ
- **GIVEN** 外側の枠と、その内側の枠の両方に modifier を付けた画面
- **WHEN** show する
- **THEN** 内側の枠の矩形が基準になる

#### Scenario: 窓の外に配置されたものは候補にならない
- **GIVEN** 窓の外 (隣のページなど) に配置された modifier 付きの枠と、窓内の modifier 付きの枠が同時に attach している画面
- **WHEN** show する
- **THEN** 窓内の枠の矩形が基準になる

### Requirement: 共通ケース表の拡張

共通ケース表 `core/layout-spec/cases.json` は、ケースの入力として任意の基準矩形 (窓座標の `pageArea`) を表せること (SHALL)。`layoutArea = currentPage` のケースは `pageArea` を持ち、期待 rect は既存の軸別レイアウト規則から機械導出すること (SHALL)。`pageArea` を持たない既存ケースの期待値は変えないこと (SHALL)。追加ケースは少なくとも「タブバー相当の下部帯を除いたページで End/End」「ページの矩形を基準にした比率サイズ」「ページが全画面 (可視領域と一致)」を含むこと (SHALL)。

#### Scenario: 追加ケースが iOS / Android の実 frame 検証に載る
- **GIVEN** `pageArea` を持つ追加ケース
- **WHEN** iOS / Android のケース表テストがページの矩形を模したホストで実レイアウトさせる
- **THEN** 全ケースが許容誤差内で期待 rect に一致する

#### Scenario: 既存ケースは変わらない
- **GIVEN** `pageArea` を持たない既存ケース
- **WHEN** 拡張後のケース表テストを回す
- **THEN** 既存ケースの期待値・結果は拡張前と同じである
