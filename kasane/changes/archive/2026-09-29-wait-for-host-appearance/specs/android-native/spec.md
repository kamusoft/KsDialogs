# android-native デルタ (wait-for-host-appearance)

dialog-contract (PB-HW-*)・loading-contract (LD-HW-*)・toast-contract (TS-HW-*) の挙動 Scenario は、同名テストで全量を検証する (core/ADR-0016)。本書は Android に固有の差分だけを扱う。Scenario ID は `PB-HA-<NN>` (Android の Dialog の提示先の出現待ち)。

実現経路 (design Decision 5)
- Loading・Toast の提示面が使っている入れ替わりの購読口 `observeHostChange` (`android/ksdialogs-core/src/main/kotlin/jp/kamusoft/ksdialogs/LoadingPresentationSurface.kt:26,40-51`・`ToastPresentationSurface.kt:25,39-50`) を、Dialog の提示面 (`DialogPresentationSurface.kt:12-18`・`ActivityDialogPresentationSurface.kt:9-12`) にも足す
- 通知の元は `ResumedActivityTracker` の入れ替わりの購読 (`ResumedActivityTracker.kt:52-57,59-78`)。通知は UI スレッドで、登録順に届く
- 待ちを置く場所: `DialogPresenter.presentWithFactory` の提示先の判定 (`DialogPresenter.kt:66-68`) を、紐付け (`:75-77`) の後ろへ移して待ちに置き換える

## ADDED Requirements

### Requirement: Dialog の提示面は、Loading・Toast と同じ入れ替わりの購読口を持つ

Dialog の提示面は、resumed な Activity の入れ替わりを購読する口を持つ SHALL。形は Loading・Toast の提示面の口と同じ (購読を返し、解除できる) にする。Dialog は、この口で提示先の出現を待つ。

- 提示の時点で提示先が無ければ、提示面は `DialogException` を投げない。Presenter が提示先を待ってから提示を求めるので、提示先の無い提示の要求は内部の不整合として扱う
- 購読は、提示先を待っている Dialog がある間だけ張り、待ちが終われば解除する

#### Scenario: [PB-HA-01] Activity が resume した時点で、待っていた Dialog が表示される
- **GIVEN** resumed な Activity が無い状態で show を呼び、待っている Dialog
- **WHEN** Activity が resume する
- **THEN** その Activity に Dialog が表示される。待ちの購読は解除されている

#### Scenario: [PB-HA-02] Activity が破棄されただけでは表示されず、次の Activity の resume で表示される
- **GIVEN** 提示先を待っている Dialog
- **WHEN** Activity の破棄の通知だけが届き (resumed な Activity は無いまま)、そのあと別の Activity が resume する
- **THEN** 破棄の通知では表示されず、次の Activity の resume で表示される

### Requirement: 提示先が無いことを表す例外は公開 API に無い

`DialogException` は、提示先が無いことを表すサブクラスを持たない SHALL。Dialog の show は提示先の不在で失敗しないため (dialog-contract デルタ)、この例外は要らない。

#### Scenario: [PB-HA-03] `DialogException` のサブクラスに提示先の不在が無い
- **GIVEN** `DialogException` のすべてのサブクラスを網羅する `when` 式を含むテストのソース
- **WHEN** 提示先の不在を表すサブクラスを列挙せずにコンパイルする
- **THEN** コンパイルが通る (網羅性の検査で、サブクラスが無いことを確かめる)

## MODIFIED Requirements

### Requirement: Android の失敗型メッセージは英語固定

`DialogException` の各サブクラスの `message` は、次の英語文言とする (SHALL)。`{T}` は現行と同じ値 (ViewModel 型名の文字列) を埋め込む。iOS と同じ状況の文言は ios-native デルタと同じ英語にする。

| 例外 | 文言 (en) |
|---|---|
| `DialogException.ViewFactoryNotRegistered` | `No View factory is registered for ViewModel type {T}.` |
| `DialogException.ViewModelFactoryNotRegistered` | `No ViewModel factory is registered for ViewModel type {T}.` |
| `DialogException.ViewModelAlreadyShowing` | `This ViewModel instance of type {T} is already being shown.` |
| `DialogException.ValueClassViewModel` | `ViewModel type {T} is a value class and cannot be used as a ViewModel.` |

#### Scenario: [DM-AN-01] DialogException の全サブクラスが対応表の英語文言を持つ
- **GIVEN** `DialogException` の 4 サブクラスそれぞれに型名を与えたインスタンス
- **WHEN** `message` を読む
- **THEN** 対応表の英語文言に型名を埋め込んだ文字列と完全一致する
