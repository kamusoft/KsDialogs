# maui-binding デルタスペック (add-page-layout-area)

## ADDED Requirements

### Requirement: CurrentPage のパススルー (MAUI)

MAUI binding は `DialogLayoutArea.CurrentPage` を公開し、添付プロパティと `DialogOptions` から Native (iOS / Android) の `currentPage` へ写像すること (SHALL)。既存の `Window` / `VisibleArea` の写像は変えないこと (SHALL)。

#### Scenario: CurrentPage が Native へ届く
- **GIVEN** `ksd:Dialog.LayoutArea="CurrentPage"` を添付したコンテンツ
- **WHEN** show する
- **THEN** Native の器が受け取る layoutArea は currentPage である (パススルーテスト)

### Requirement: MAUI 層の既定 provider

MAUI binding は既定の現在ページ provider を MAUI 層に持ち、両 OS で同じ辿り方をすること (SHALL): 提示先の `Window` から、モーダルスタックの先頭 (`Navigation.ModalStack` の最後) があればそれ、無ければ `Window.Page` を起点に、`Shell` → `CurrentPage` / `FlyoutPage` → `Detail` / `TabbedPage` → `CurrentPage` / `NavigationPage` → `CurrentPage` を容れ物でなくなるまで降り、先端ページの `Handler.PlatformView` を Native の View provider へ渡すこと (SHALL)。先端ページの `Handler` が無い (未描画) ときは未解決として dialog-contract の規則に従うこと (SHALL)。iOS Native の既定 provider (VC 走査) は MAUI では使わず、MAUI 層の結果を優先すること (SHALL)。

#### Scenario: TabbedPage の中の NavigationPage
- **GIVEN** `Window.Page` が `TabbedPage` で、選択中の子が `NavigationPage`、その先端が `ContentPage` のテストホストと `CurrentPage`・End/End
- **WHEN** show する
- **THEN** ダイアログはその `ContentPage` の PlatformView の矩形 (タブバー・ナビゲーションバーの内側) を基準に配置され、タブバーと重ならない (iOS / Android 両方)

#### Scenario: Shell
- **GIVEN** `Window.Page` が `Shell` で、`Shell.CurrentPage` が表示中のテストホスト
- **WHEN** `CurrentPage` で show する
- **THEN** `Shell.CurrentPage` の PlatformView の矩形が基準になる

#### Scenario: モーダルが優先される
- **GIVEN** `TabbedPage` の上にモーダルで `ContentPage` を出しているテストホスト
- **WHEN** `CurrentPage` で show する
- **THEN** モーダルの `ContentPage` の PlatformView の矩形が基準になる (下の `TabbedPage` の子ではない)

#### Scenario: FlyoutPage の Detail
- **GIVEN** `Window.Page` が `FlyoutPage` で `Detail` が `NavigationPage` のテストホスト
- **WHEN** `CurrentPage` で show する
- **THEN** `Detail` のスタック先端の PlatformView の矩形が基準になる

#### Scenario: 素の ContentPage は可視領域と一致する
- **GIVEN** `Window.Page` が素の `ContentPage` のテストホスト
- **WHEN** `CurrentPage` で show する
- **THEN** 最終 rect は `VisibleArea` のときと一致する

### Requirement: provider の上書き (MAUI)

MAUI binding は、現在ページの `Page` または `VisualElement` を返す関数を一度登録する口を公開し、登録があれば MAUI 層の既定 provider より優先すること (SHALL)。返された要素の `Handler.PlatformView` を既定 provider と同じ経路で Native へ渡すこと (SHALL)。

#### Scenario: 上書きした要素が基準になる
- **GIVEN** `TabbedPage` のテストホストと、ページ内の特定の `VisualElement` を返す provider の登録
- **WHEN** `CurrentPage` で show する
- **THEN** その要素の PlatformView の矩形が基準になる
