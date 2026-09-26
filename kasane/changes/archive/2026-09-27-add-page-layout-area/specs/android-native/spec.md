# android-native デルタスペック (add-page-layout-area)

## ADDED Requirements

### Requirement: CURRENT_PAGE と provider の登録 (Android)

Android 実装は `DialogLayoutArea.CURRENT_PAGE` を公開し、現在ページの `View?` を返す関数を一度登録する口を公開すること (SHALL)。既定 provider は持たず、未登録なら dialog-contract の未解決規則 (可視領域 + 診断ログ) に従うこと (SHALL)。基準矩形は返された View の窓内矩形 ∩ 可視領域 (システムバーを除く) とすること (SHALL)。器 (別ウィンドウ) と Activity のウィンドウは原点が異なり得るため、両者を**画面座標を共通原点**として突き合わせること (SHALL) — ページの View は画面上の位置 (`getLocationOnScreen` 相当)、器はその root の画面上の位置で測り、差を取って器の座標へ写す。Compose の `boundsInWindow()` は Activity ウィンドウの画面上の位置を足して同じ変換にかけること (SHALL)。候補の View は提示先の Activity のウィンドウに属するものに限ること (SHALL)。

#### Scenario: 登録した View で End 配置が下部バーを避ける
- **GIVEN** Activity の content に下部バーとページ領域の View を持つテストホストで、ページ領域の View を返す provider を登録した状態と、`CURRENT_PAGE`・End/End の添付
- **WHEN** show する
- **THEN** ダイアログの下端はページ領域の View の下端から dialogMargin 分内側にあり、下部バーと重ならない

#### Scenario: 未登録なら可視領域と診断ログ
- **GIVEN** provider を登録していない状態と `CURRENT_PAGE` の添付
- **WHEN** show する
- **THEN** 最終 rect は `VISIBLE_AREA` のときと一致し、未登録を示す診断ログが出る

#### Scenario: 器のウィンドウと Activity のウィンドウの原点が違っても同じ場所を指す
- **GIVEN** 器の root の画面上の位置が Activity のウィンドウの原点とずれているテストホスト (器のウィンドウに上側の余白を与える等で原点差を作る) と、ページ領域の View を返す provider
- **WHEN** `CURRENT_PAGE`・Start/Start で show する
- **THEN** ダイアログの上端は、画面上で見てページ領域の View の上端から dialogMargin 分内側にある (原点差のぶんずれない)

#### Scenario: edge-to-edge でもシステムバーを含まない
- **GIVEN** ページの View がステータスバーの下まで伸びるテストホストと、その View を返す provider
- **WHEN** `CURRENT_PAGE`・Start/Start で show する
- **THEN** ダイアログの上端はステータスバーの下端 (可視領域の上端) から dialogMargin 分内側になる

### Requirement: Compose の modifier (Android)

Android 実装は、付けた composable を現在ページとして名乗らせる `Modifier` を公開し、dialog-contract の台帳規則に従うこと (SHALL)。台帳に候補があれば登録済みの provider より優先すること (SHALL)。台帳の attach / detach は composable の配置と離脱に追随すること (SHALL)。

#### Scenario: Scaffold の content 枠に付けると下部バーを避ける
- **GIVEN** 下部ナビゲーションを持つ `Scaffold` の content 枠に modifier を付けたテストホストと `CURRENT_PAGE`・End/End
- **WHEN** show する
- **THEN** ダイアログの下端は content 枠の下端から dialogMargin 分内側にあり、下部ナビゲーションと重ならない

#### Scenario: 遷移で枠が離脱すると台帳から外れる
- **GIVEN** modifier を付けた画面から付けていない画面へ遷移して前の画面が composition から外れた状態
- **WHEN** show する
- **THEN** 最終 rect は `VISIBLE_AREA` のときと一致する

#### Scenario: 入れ子は内側が勝つ
- **GIVEN** 外側の枠と内側の枠の両方に modifier を付けたテストホスト
- **WHEN** show する
- **THEN** 内側の枠の矩形が基準になる

### Requirement: ケース表の追加ケースへの適合 (Android)

Android 実装は、`pageArea` を持つ追加ケースを含む共通ケース表の全ケースに、実 View の測定で適合すること (SHALL)。テストは `pageArea` を Activity 側のページ領域の View として再現するホストで実レイアウトさせること (SHALL)。

#### Scenario: 追加ケースの instrumented 検証が通る
- **GIVEN** 拡張後の共通ケース表
- **WHEN** instrumented テストがレイアウト完了後の実 View を測る
- **THEN** 追加ケースを含む全ケースが許容誤差内で期待 rect に一致する
