# kmp-api-surface (delta)

## ADDED Requirements

### Requirement: Swift 境界の失敗経路の宣言検査

commonMain の公開 interface `KsDialog` / `KsLoading` / `KsToast` の宣言メソッドのうち失敗しうる 4 経路 — `KsDialog.show(viewModel, placement)`・`KsLoading.show(viewModel, placement)`・`KsLoading.start(viewModel, placement, action)`・`KsToast.show(viewModel, durationMs, placement)` — は `DialogException` を Swift 境界へ `throws` として届ける宣言 (`@Throws`) を持ち、同じ 3 interface のそれ以外の宣言メソッドはこの宣言を持たない (SHALL)。この線引きは `androidHostTest` のテストで検査され、4 経路のいずれかで宣言が落ちても、それ以外の宣言メソッドに宣言が付いても、テストが該当するメソッド名を示して失敗する (SHALL)。4 経路の特定が名前の綴り変更で素通りしないよう、列挙した経路が過不足なく見つかったことも検査する (SHALL)。検査対象は 3 interface の宣言メソッドに限り、registry 型や ViewModel 契約など他の公開型は対象外である。

#### Scenario: 失敗しうる 4 経路の宣言
- **GIVEN** 現行の commonMain 契約
- **WHEN** `./gradlew testAndroidHostTest` を実行する
- **THEN** 4 経路の宣言検査が成功し、4 経路が過不足なく見つかっている

#### Scenario: 失敗しない経路に宣言が無いこと
- **GIVEN** 現行の commonMain 契約
- **WHEN** `./gradlew testAndroidHostTest` を実行する
- **THEN** 3 interface の 4 経路以外の宣言メソッド (message 引数の overload・型指定 show / start・`hide`・`setMessage`・registry プロパティのアクセサ) に宣言が無いことの検査が成功する

#### Scenario: 宣言が落ちたときの検出
- **GIVEN** 4 経路のいずれか 1 つから `@Throws` を一時的に外した commonMain 契約 (確認後に復元し `git diff` が空であることを確認する)
- **WHEN** `./gradlew testAndroidHostTest` を実行する
- **THEN** 宣言検査がそのメソッド名を示して失敗し、他のテストは影響を受けない

#### Scenario: 宣言が余計に付いたときの検出
- **GIVEN** 4 経路以外の宣言メソッド 1 つ (例: `KsLoading.setMessage`) に `@Throws(DialogException::class)` を一時的に付けた commonMain 契約 (確認後に復元し `git diff` が空であることを確認する)
- **WHEN** `./gradlew testAndroidHostTest` を実行する
- **THEN** 宣言検査がそのメソッド名を示して失敗し、他のテストは影響を受けない
