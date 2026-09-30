# dialog-contract デルタ (fix-android-container-system-bar-appearance)

Scenario ID は既存の `PB-SB-<NN>` (提示挙動のうちシステムバー) の続き。本書の Scenario は Native 2 実装 (iOS / Android) の同名テストで検証する (core/ADR-0016)。PB-SB 領域は Android 固有の Scenario (android-native デルタの PB-SB-01〜07・09〜11) も持つため、網羅検査の両 Native ミラー対象 (`scripts/scenario-id-coverage.py` の `MIRROR_AREAS`) には足さない。本書の PB-SB-08 が両 Native に揃っていることはレビューで確かめる。決定の出典は core/ADR-0039。既存の Dialog 契約は変えない — 本書はシステムバーの指定の非干渉の追加分のみ。

明暗の Scenario が判定するのは、**ステータスバーの明暗を決める指定**であって、実際に描かれた文字色ではない (暗幕に合わせて OS が文字色を選ぶことは要件の外のため)。各 Native の観測のしかた:
- Android: 器のウィンドウの明暗の値が、テストが提示先に与えた指定 (明るい地向け) と同じであること。比べる相手は、テストが与えた指定から組み立てた期待値であり、提示先のウィンドウから OS が返す値ではない (OS が値を返さない指定のしかたがあるため。android-native デルタの PB-SB-09)
- iOS: UIKit がステータスバーの見えを尋ねる相手が提示先のままであること。Dialog の器は提示元の最前面の画面の上に全画面で重ねて表示し、制御を奪わない (`modalPresentationCapturesStatusBarAppearance` が false、明暗の委ね先 `childForStatusBarStyle` を持たない)。器が自分の指定で置き換える退行 (制御を奪う設定・委ね先の追加) はこの観測で検出できる。既存の `PB-IA-03` (`ios/Tests/KsDialogsTests/DialogStatusBarAppearanceTests.swift`) が表示/非表示について同じ形で確かめている

## ADDED Requirements

### Requirement: システムバーの指定の非干渉 (Dialog)

Dialog の器は、表示中も提示先の画面のシステムバーの指定を変えない (SHALL)。変えない指定は、ステータスバーのアイコンの明暗、バーの表示/非表示、隠れたバーの再表示の作法 (その概念を持つ OS のみ) である。覆いの色 (透明を含む) によらない。既存の約束「覆いに透明を指定しても、システムバーの見えは変わらない」はこの要件に含まれる。

暗幕 (覆い) がステータスバーの下を暗くしたときに、OS がそれに合わせてステータスバーの文字色を選ぶことは、この要件の外に置く。器は指定を変えておらず、見えの変化は OS の判断による。

この要件が定めるのは、表示を始めた時点の提示先の指定を変えないことである。表示中に提示先が指定を変えた場合の扱いは、この要件では定めない (Android は android-native デルタの PB-SB-06 / PB-SB-07 で「追随しない」と定めている)。

表示/非表示は既存の Scenario が検証している (Android は android-native デルタの PB-SB-01〜03、iOS は ios-native の PB-IA-03)。本書は明暗を足す。

#### Scenario: [PB-SB-08] 明るい地向けの明暗を指定した画面でダイアログを出しても、明暗の指定は変わらない
- **GIVEN** 提示先の画面が、ステータスバーのアイコンを明るい地向け (暗い色) に指定している
- **WHEN** 既定の覆いの色でダイアログを表示する
- **THEN** 表示中も、ステータスバーの明暗を決める指定は提示先の画面のもの (明るい地向け) のままで、器の指定に置き換わらない
