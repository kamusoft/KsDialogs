# ios-native デルタ (wait-for-host-appearance)

dialog-contract (PB-HW-*)・loading-contract (LD-HW-*)・toast-contract (TS-HW-*) の挙動 Scenario は、同名テストで全量を検証する (core/ADR-0016)。本書は iOS に固有の差分だけを扱う。Scenario ID は `PB-HI-<NN>` (iOS の提示先の出現の合図)。

実現経路 (design Decision 1)
- 提示先を選ぶ規則: `ApplicationKeyWindowProvider.selectKeyWindow` (`ios/Sources/KsDialogs/Presentation/ApplicationKeyWindowProvider.swift:23-28`)
- 購読を足す場所: `DialogKeyWindowProvider` (`DialogKeyWindowProvider.swift:6-8`) と、3 機能の提示面の protocol (`DialogPresentationSurface`・`LoadingPresentationSurface`・`ToastPresentationSurface`)
- 置き換える購読: `ToastCoordinator` の `UIWindow.didBecomeKeyNotification` の直接購読 (`ToastCoordinator.swift:224-235`)

## ADDED Requirements

### Requirement: 提示先の出現の合図は、提示先を選ぶ規則の隣で 2 つの通知から作る

提示先の出現の合図は、提示先を選ぶ規則を持つ provider が作る SHALL。provider は、window が key になった通知 (`UIWindow.didBecomeKeyNotification`) と、シーンがアクティブになった通知 (`UIScene.didActivateNotification`) の両方で、購読者に合図を送る。

- 合図は「提示先が現れたかもしれない」ことだけを知らせる。受け取った側は、提示先を選ぶ規則をもう一度読んで、表示するか待ち続けるかを決める
- 3 機能 (Dialog・Loading・Toast) の提示面は、この購読を同じ形の口 (購読を返し、解除できる) で中継する。各機能は、この口のほかに提示先の出現を知る手段を持たない
- 購読は、提示先を待っている表示がある間だけ張り、待っている表示が無くなったら解除する

#### Scenario: [PB-HI-01] key 化とシーンのアクティブ化の両方で、合図が届く
- **GIVEN** provider の合図を購読している購読者
- **WHEN** window が key になった通知を送る / シーンがアクティブになった通知を送る
- **THEN** どちらの通知でも、購読者に合図が 1 回ずつ届く。購読を解除したあとは届かない

#### Scenario: [PB-HI-02] 合図が来ても提示先の条件を満たさなければ待ち続け、満たした合図で表示する
- **GIVEN** 提示先の無い提示面と、その提示面で提示先を待っている Dialog・Loading (スコープ形)・Toast
- **WHEN** 提示先が無いまま合図を送り、そのあと提示先を用意してから合図を送る
- **THEN** 1 回目の合図では、どれも表示されず待ち続ける。2 回目の合図で 3 つとも表示される

#### Scenario: [PB-HI-03] 待っている表示が無くなると、購読が解除される
- **GIVEN** 提示先を待っている Toast と Loading
- **WHEN** Toast の期限が満了し、Loading の処理が終わる
- **THEN** 提示面の購読がすべて解除されている

### Requirement: 提示先が無いことを表す失敗は公開 API に無い

`DialogError` は、提示先が無いことを表す case を持たない SHALL。Dialog の show は提示先の不在で失敗しないため (dialog-contract デルタ)、この失敗を表す手段は要らない。KMP 向けの型付き入口 (`KsDialogsKmpError`) も、理由の欠けた失敗の代わりの値に、提示先が無いことを表す値を使わない。

#### Scenario: [PB-HI-04] `DialogError` の case に提示先の不在が無い
- **GIVEN** `DialogError` のすべての case を網羅する `switch` を含むテストのソース
- **WHEN** 提示先の不在を表す case を列挙せずにコンパイルする
- **THEN** コンパイルが通る (網羅性の検査で、case が無いことを確かめる)

## MODIFIED Requirements

### Requirement: iOS の失敗型メッセージは英語固定

`DialogError` と `KsDialogsKmpError` の `errorDescription` は、次の英語文言を返す (SHALL)。`{T}` `{expected}` `{actual}` は現行と同じ値 (型名の文字列) を埋め込む。文言を端末言語で切り替えない。

| case | 文言 (en) |
|---|---|
| `DialogError.viewFactoryNotRegistered(viewModelType:)` | `No View factory is registered for ViewModel type {T}.` |
| `DialogError.viewFactoryTypeMismatch(viewModelType:)` | `The registered View factory cannot accept ViewModel type {T}.` |
| `DialogError.resultTypeMismatch(expected:actual:)` | `The result value type does not match (expected: {expected} / actual: {actual}).` |
| `DialogError.viewModelFactoryNotRegistered(viewModelType:)` | `No ViewModel factory is registered for ViewModel type {T}.` |
| `DialogError.viewModelFactoryTypeMismatch(viewModelType:)` | `The registered ViewModel factory does not produce ViewModel type {T}.` |
| `DialogError.viewModelAlreadyShowing(viewModelType:)` | `This ViewModel instance of type {T} is already being shown.` |
| `KsDialogsKmpError.notRegistered(viewModelType:)` | `No View factory is registered for ViewModel type {T}.` |
| `KsDialogsKmpError.resultTypeMismatch(expected:actual:)` | `The result value type does not match (expected: {expected} / actual: {actual}).` |

#### Scenario: [DM-IO-01] DialogError の全 case が対応表の英語文言を返す
- **GIVEN** `DialogError` の 6 case それぞれに型名 (`{T}` / `{expected}` / `{actual}`) を与えたインスタンス
- **WHEN** `errorDescription` (`localizedDescription`) を読む
- **THEN** 対応表の英語文言に型名を埋め込んだ文字列と完全一致する

#### Scenario: [DM-IO-02] KsDialogsKmpError の全 case が対応表の英語文言を返す
- **GIVEN** `KsDialogsKmpError` の 2 case それぞれに型名を与えたインスタンス
- **WHEN** `errorDescription` を読む
- **THEN** 対応表の英語文言に型名を埋め込んだ文字列と完全一致する
