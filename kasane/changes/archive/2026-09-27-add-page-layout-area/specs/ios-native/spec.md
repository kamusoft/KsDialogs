# ios-native デルタスペック (add-page-layout-area)

## ADDED Requirements

### Requirement: currentPage と既定 provider (iOS)

iOS 実装は `DialogLayoutArea.currentPage` を公開し、既定の現在ページ provider を内蔵すること (SHALL)。既定 provider は提示先 window の rootViewController から presented → `UINavigationController.topViewController` → `UITabBarController.selectedViewController` を先端まで走査し、先端の view controller の view を現在ページとすること (SHALL。閉じる途中の presented は辿らない — 既存の提示先走査と同じ)。走査は **KsDialogs 自身の器 (Dialog / Loading / Toast のコンテナ view controller) を通り抜けて、その提示元へ戻ること** (SHALL) — 表示中の器や重ねて出した別の器を現在ページに選ばない。既定 provider が保証するのは UIKit のコンテナ (presented / navigation / tab) までで、`UIHostingController` の内側 (SwiftUI の `TabView` / `NavigationStack`) には保証を及ぼさない — SwiftUI では modifier が正規の経路 (次の Requirement)。基準矩形は現在ページの view の `safeAreaLayoutGuide.layoutFrame` を窓座標へ変換したものとすること (SHALL)。

#### Scenario: tab + navigation の階層で End 配置がタブバーを避ける
- **GIVEN** `UITabBarController` の selected タブが `UINavigationController` で、その top の view controller が表示中のテストホストと、`.currentPage`・End/End の添付
- **WHEN** show する
- **THEN** ダイアログの下端はタブバーの上端から dialogMargin 分内側にあり、上端側の計算はナビゲーションバーの下端を基準にしている

#### Scenario: presented された画面が現在ページになる
- **GIVEN** タブ画面の上に present された view controller が表示中のテストホスト
- **WHEN** `.currentPage` で show する
- **THEN** present された view controller の view の safe area が基準になる

#### Scenario: 表示中の器は現在ページにならない
- **GIVEN** タブ画面の上に KsDialogs のダイアログを 1 つ表示中の状態
- **WHEN** `.currentPage` で 2 つ目のダイアログを show する
- **THEN** 2 つ目の基準は 1 つ目の器ではなく、背後のタブ画面の先端の view の safe area になる (1 つ目と同じ矩形)

#### Scenario: 別ウィンドウの View は候補にならない
- **GIVEN** 提示先とは別の `UIWindow` に載った view を返す provider を登録した状態
- **WHEN** `.currentPage` で show する
- **THEN** その view は使われず、次の取得元 (既定 provider) の結果が基準になる

### Requirement: provider の上書き (iOS)

iOS 実装は、現在ページの `UIView?` を返す関数を一度登録する口を公開し、登録があれば既定 provider より優先すること (SHALL)。登録を外せば既定 provider に戻ること (SHALL)。

#### Scenario: 上書きした provider が既定より優先される
- **GIVEN** 既定 provider が届くタブ画面と、別の view を返す provider の登録
- **WHEN** `.currentPage` で show する
- **THEN** 登録した view の safe area が基準になる

#### Scenario: 登録を外すと既定に戻る
- **GIVEN** 上の状態
- **WHEN** 登録を外して show する
- **THEN** 既定 provider の結果 (タブ画面の先端) が基準になる

### Requirement: SwiftUI の modifier (iOS)

iOS 実装は、付けた View を現在ページとして名乗らせる SwiftUI の view modifier を公開し、dialog-contract の台帳規則に従うこと (SHALL)。台帳に候補があれば登録 provider・既定 provider より優先すること (SHALL)。SwiftUI の `TabView` / `NavigationStack` で組んだ画面では、この modifier を content 枠に付けることが現在ページを得る正規の経路である (SHALL)。

#### Scenario: SwiftUI の TabView + NavigationStack で content 枠の modifier が基準になる
- **GIVEN** SwiftUI の `TabView` の中の `NavigationStack` に載せた画面の content 枠に modifier を付け、`UIHostingController` で表示しているテストホスト
- **WHEN** `.currentPage`・End/End で show する
- **THEN** ダイアログの下端はタブバーの上端から dialogMargin 分内側にあり、上端側の計算はナビゲーションバーの下端を基準にしている

#### Scenario: modifier を付けた枠が既定 provider に勝つ
- **GIVEN** 既定 provider が届く UIKit の画面の中で、content 枠に modifier を付けた状態
- **WHEN** `.currentPage` で show する
- **THEN** modifier を付けた枠の矩形が基準になる

#### Scenario: 枠が画面から外れると既定 provider に戻る
- **GIVEN** 上の枠を持つ画面から別の画面へ遷移し、枠が detach した状態
- **WHEN** show する
- **THEN** 既定 provider の結果が基準になる

### Requirement: ケース表の追加ケースへの適合 (iOS)

iOS 実装は、`pageArea` を持つ追加ケースを含む共通ケース表の全ケースに、実 frame の測定で適合すること (SHALL)。テストは `pageArea` をページの view の safe area として再現するホストで実レイアウトさせること (SHALL)。

#### Scenario: 追加ケースの実 frame 検証が通る
- **GIVEN** 拡張後の共通ケース表
- **WHEN** シミュレータ上のテストがレイアウト完了後の実 frame を測る
- **THEN** 追加ケースを含む全ケースが許容誤差内で期待 rect に一致する
