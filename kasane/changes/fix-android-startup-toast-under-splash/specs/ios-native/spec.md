# ios-native デルタ (fix-android-startup-toast-under-splash)

toast-contract (TS-HW-*) の挙動 Scenario は、同名テストで全量を検証する (core/ADR-0016)。本書は iOS に固有の差分だけを扱う。Scenario ID は既存の `PB-HI-<NN>` (iOS の提示先の合図) を引き継ぐ。決定の出典は core/ADR-0043 (proposed)、実現経路は design Decision 2・5。

実現経路:
- 前面の判定と前面を離れた合図は、提示先の規則と同じ供給元 (`ios/Sources/KsDialogs/Presentation/ApplicationKeyWindowProvider.swift`) に置く。シーンの写し (`Presentation/DialogWindowSceneSnapshot.swift`) に、前面かどうかを足す
- 提示先の定義 (前面でアクティブなシーンの key window) と、提示先の出現の合図 (`UIWindow.didBecomeKeyNotification` と `UIScene.didActivateNotification`) は変えない

## ADDED Requirements

### Requirement: iOS の前面の判定と、前面を離れた合図は、提示先の規則と同じ供給元に置く

アプリが前面にいるとは、activationState が foregroundActive か foregroundInactive のシーンが 1 つ以上あることとする SHALL (core/ADR-0043 の「前面」の iOS での判定)。シーンが 1 つもつながっていない間は背面とする。

- 前面を離れた合図は、シーンが背面へ入った通知 (`UIScene.didEnterBackgroundNotification`) から作る SHALL。通知を受けた時点で前面かどうかを読み直し、前面のシーンが 1 つも無くなっていたときだけ届ける。提示先の出現の合図とは別の口にする
- 前面の判定と前面を離れた合図は、提示先を選ぶ規則と同じ供給元に置く
- 前面を離れた合図の購読は、前面の待ちの Toast がある間だけ張り、無くなれば解除する

#### Scenario: [PB-HI-05] 前面でアクティブでないシーンだけがあるとき、前面の待ちと判定される
- **GIVEN** foregroundInactive のシーンだけがあり、foregroundActive のシーンが無い状態 (起動の途中・割り込みの最中)
- **WHEN** 前面かどうかと提示先を読む
- **THEN** 前面と判定され、提示先は無い

#### Scenario: [PB-HI-06] 背面のシーンだけがあるとき、またはシーンが無いときは、背面と判定される
- **GIVEN** background のシーンだけがある状態、またはシーンが 1 つもつながっていない状態
- **WHEN** 前面かどうかを読む
- **THEN** 背面と判定される

#### Scenario: [PB-HI-07] 唯一の前面のシーンが背面へ入った通知で、前面を離れた合図が届く
- **GIVEN** 前面を離れた合図を購読していて、前面のシーンが 1 つだけある状態
- **WHEN** そのシーンが背面へ入り、`UIScene.didEnterBackgroundNotification` が post される
- **THEN** 購読者に合図が UI スレッドで届く

#### Scenario: [PB-HI-09] 別のシーンが前面に残っていれば、1 つのシーンが背面へ入っても合図は届かない
- **GIVEN** 前面を離れた合図を購読していて、前面のシーンが 2 つある状態
- **WHEN** そのうち 1 つが背面へ入り、`UIScene.didEnterBackgroundNotification` が post される
- **THEN** 前面と判定されたままで、購読者に合図は届かない

#### Scenario: [PB-HI-08] 前面の待ちの Toast が無くなれば、前面を離れた合図の購読を解除する
- **GIVEN** 前面の待ちの Toast があり、前面を離れた合図を購読している状態
- **WHEN** その Toast が提示先に載る
- **THEN** 前面を離れた合図の購読が解除される
