# android-native デルタ (fix-android-startup-toast-under-splash)

dialog-contract (PB-HW-*)・loading-contract (LD-HW-*)・toast-contract (TS-HW-*) の挙動 Scenario は、同名テストで全量を検証する (core/ADR-0016)。本書は Android に固有の差分だけを扱う。Scenario ID は既存の `PB-HA-<NN>` (Android の提示先) を引き継ぐ。決定の出典は core/ADR-0044・core/ADR-0043 (どちらも proposed)、実現経路は design Decision 1・2・5。

実現経路:
- 提示先の判定と前面の判定は `ResumedActivityTracker` (`android/ksdialogs-core/src/main/kotlin/jp/kamusoft/ksdialogs/ResumedActivityTracker.kt`) に置く。3 機能の提示面 (`ActivityDialogPresentationSurface.kt:18-19`・`LoadingPresentationSurface.kt:40-52`・`ToastPresentationSurface.kt:39-51`) は、今までどおり追跡役の供給 (`ResumedActivityProvider.kt:10-13`) と入れ替わりの通知を読む
- 描画の合図は、decorView の `ViewTreeObserver.OnDrawListener` (API 16)。差し替えられる口の後ろに置き、unit テストは偽の実装で描画を送る

## MODIFIED Requirements

### Requirement: Dialog の提示面は、Loading・Toast と同じ入れ替わりの購読口を持つ

Dialog の提示面は、提示先 (resumed で、かつ描画された Activity) の入れ替わりを購読する口を持つ SHALL。形は Loading・Toast の提示面の口と同じ (購読を返し、解除できる) にする。Dialog は、この口で提示先の出現を待つ。

- 提示の時点で提示先が無ければ、提示面は `DialogException` を投げない。Presenter が提示先を待ってから提示を求めるので、提示先の無い提示の要求は内部の不整合として扱う
- 購読は、提示先を待っている Dialog がある間だけ張り、待ちが終われば解除する

#### Scenario: [PB-HA-01] Activity が resume して描画された時点で、待っていた Dialog が表示される
- **GIVEN** 提示先が無い状態で show を呼び、待っている Dialog
- **WHEN** Activity が resume し、そのあと描画される
- **THEN** resume の時点では表示されず、描画された時点でその Activity に Dialog が表示される。待ちの購読は解除されている

#### Scenario: [PB-HA-02] Activity が破棄されただけでは表示されず、次の Activity が resume して描画された時点で表示される
- **GIVEN** 提示先を待っている Dialog
- **WHEN** Activity の破棄の通知だけが届き (提示先は無いまま)、そのあと別の Activity が resume して描画される
- **THEN** 破棄の通知では表示されず、次の Activity が描画された時点で表示される

## ADDED Requirements

### Requirement: Android の提示先は、resumed で、かつ描画された Activity

Dialog・Loading・Toast の提示先は、最後に resume した Activity が、start の後に一度でも描画されていれば、その Activity とする SHALL (core/ADR-0044)。3 機能で同じ条件を使う。

- 描画は decorView の実際の描画を指す。描画の直前で描画が止められている間 (起動画面の延長など) は、描画に数えない
- 描画済みの印は、その Activity の stop か破棄で失う。pause では失わない。描画を受けてから印になるまでの間に stop・破棄した場合、その描画は印にしない
- 描画済みになった時点で、入れ替わりの購読者へ通知する
- resume の時点で描画済みでなければ、ライブラリが描画を促す

#### Scenario: [PB-HA-04] resumed だが未描画の Activity には、Dialog・Loading・Toast のどれも載らず、描画の後に載る
- **GIVEN** 最初の描画を止めた (描画の直前で止める)、resumed な Activity
- **WHEN** Dialog・Loading・Toast を show し、そのあと描画の停止を解く
- **THEN** 停止している間は 3 つともどれも載らず、最初の描画の後に 3 つとも載る

#### Scenario: [PB-HA-05] pause だけから復帰した Activity は、再描画を待たずに提示先に戻る
- **GIVEN** 描画済みで resumed な Activity
- **WHEN** pause し、stop せずに resume する (その間に描画は起きない)
- **THEN** resume の時点でその Activity が提示先に戻り、入れ替わりの購読者へ通知が届く

#### Scenario: [PB-HA-06] stop した Activity は、start の後に描画されるまで提示先にならない
- **GIVEN** 描画済みの Activity が stop した状態
- **WHEN** その Activity が start・resume し、そのあと描画される
- **THEN** 描画されるまでは提示先が無く、描画された時点で提示先になり、入れ替わりの購読者へ通知が届く

#### Scenario: [PB-HA-10] 描画が印になる前に stop した Activity は、その描画では提示先にならない
- **GIVEN** resumed な Activity が描画され、まだ描画済みの印になっていない状態
- **WHEN** 印になる前にその Activity が stop し、そのあと start・resume する (新しい描画はまだ起きない)
- **THEN** stop の前の描画は印にならず、提示先は無いまま。次の描画の後に提示先になる

### Requirement: Android の前面の判定

アプリが前面にいるとは、作成済みで、まだ stop も破棄もされていない Activity が 1 つ以上あることとする SHALL (core/ADR-0043 の「前面」の Android での判定)。Activity が 1 つも作られていない間は背面とする。前面の Activity が無くなった (背面へ下がった) 時点で、入れ替わりの購読者へ通知する。

背面の確定は、stop・破棄の通知の後の次のメッセージ周回で、前面の Activity が無いままのときに行う SHALL。構成の変更による作り直し (旧 Activity の stop・破棄と新 Activity の作成が続く間) は、背面とみなさない。

#### Scenario: [PB-HA-07] 作成の通知だけが届いた Activity があれば、前面の待ちと判定される
- **GIVEN** Activity が 1 つも無い状態 (背面と判定される)
- **WHEN** Activity の作成の通知だけが届く
- **THEN** 前面と判定され、提示先は無い (前面の待ち)

#### Scenario: [PB-HA-08] 描画済みの Activity が pause しただけなら、前面のまま提示先を失う
- **GIVEN** 描画済みで resumed な Activity
- **WHEN** pause の通知だけが届く (割り込み)
- **THEN** 提示先は無くなり、前面と判定される (前面の待ち)

#### Scenario: [PB-HA-09] 前面の Activity がすべて stop すると背面になり、通知が届く
- **GIVEN** 前面の Activity が 1 つだけある状態
- **WHEN** その Activity の stop の通知が届き、次のメッセージ周回まで新しい Activity が作られない
- **THEN** 背面と判定され、入れ替わりの購読者へ通知が届く

#### Scenario: [PB-HA-11] 作り直しの間は背面と判定されず、前面の待ちの Toast は数え始めない
- **GIVEN** 前面の Activity が 1 つだけあり、前面の待ちの Toast がある状態
- **WHEN** 構成の変更でその Activity が作り直される (旧 Activity の stop・破棄と、新 Activity の作成が同じメッセージの中で続く)
- **THEN** 背面と判定されず、Toast の期限は決まらない。新 Activity が描画された時点で Toast が載り、そこから数え始める
