# android-native デルタ (add-presentation-behavior)

## ADDED Requirements

### Requirement: トランジション添付面 (Android)

Android Native ライブラリは、従来 View 系 (`View.ksDialogTransition` 拡張プロパティ) と Compose 系 (`KsDialogAttributes` の `transition` 引数) の両方で DialogTransition を添付できる (SHALL)。フックは `suspend (View) -> Unit` で Main ディスパッチャで開始され、Compose コンテンツではホスト View (AbstractComposeView 派生) が渡る。Compose の添付は既存の属性宣言と同じ供給規律 (初回の組み立てで実行される位置に宣言) に従う。プリセット factory は `kotlin.time.Duration` と `android.view.animation.Interpolator` で引数を受け取る。公開 API の形は design Decision 3 の表に従う。

#### Scenario: [PB-AA-01] View 拡張プロパティでの添付が器で採用される
- **GIVEN** View コンテンツに `ksDialogTransition` で DialogTransition を設定する
- **WHEN** show で表示する
- **THEN** presentation フックがそのコンテンツのホスト View を引数に Main スレッドで呼ばれる

#### Scenario: [PB-AA-02] Compose 属性宣言での添付が器で採用される
- **GIVEN** Compose コンテンツの冒頭で `KsDialogAttributes(transition = ...)` を宣言する
- **WHEN** showCompose で表示する
- **THEN** presentation フックが Compose ホスト View を引数に呼ばれる

#### Scenario: [PB-AA-03] 呼び出し元キャンセル後も退出処理が完遂される
- **GIVEN** 完了までの間を制御できる dismissal フックを添付して表示中
- **WHEN** show を待つコルーチンをキャンセルする
- **THEN** 呼び出し元には `CancellationException` が伝播し、dismissal フックは中断されずに完了して器が撤去される

### Requirement: システムバー表示状態の引き継ぎ

ダイアログのウィンドウは、画面に載った時点で提示先の画面のシステムバー表示/非表示状態を引き継ぐ (SHALL)。API 30 以上では status / navigation それぞれの可視状態と systemBarsBehavior を、API 24〜29 では systemUiVisibility の丸ごとコピーにより可視状態を引き継ぐ。引き継ぎは表示時の1回で、表示中の提示先の状態変化には追随しない。

#### Scenario: [PB-SB-01] 全システムバー非表示の画面でダイアログを出してもバーが再出現しない (API 30+)
- **GIVEN** API 30 以上で提示先の画面が status / navigation 両方を非表示にしている
- **WHEN** ダイアログを表示する
- **THEN** 両バーは非表示のまま維持され、systemBarsBehavior も提示先と同じになる

#### Scenario: [PB-SB-02] ステータスバーのみ非表示の画面を引き継ぐ (API 30+)
- **GIVEN** API 30 以上で提示先の画面が status のみ非表示にしている
- **WHEN** ダイアログを表示する
- **THEN** status は非表示のまま、navigation は表示のまま維持される

#### Scenario: [PB-SB-03] ナビゲーションバーのみ非表示の画面を引き継ぐ (API 30+)
- **GIVEN** API 30 以上で提示先の画面が navigation のみ非表示にしている
- **WHEN** ダイアログを表示する
- **THEN** navigation は非表示のまま、status は表示のまま維持される

#### Scenario: [PB-SB-04] 旧経路 (API 24〜29) でも非表示状態が維持される
- **GIVEN** API 24〜29 で提示先の画面が immersive フラグでシステムバーを非表示にしている
- **WHEN** ダイアログを表示する
- **THEN** システムバーは非表示のまま維持される

#### Scenario: [PB-SB-05] 通常表示の画面では従来どおり
- **GIVEN** 提示先の画面がシステムバーを表示している
- **WHEN** ダイアログを表示する
- **THEN** システムバーの表示状態は変化しない

#### Scenario: [PB-SB-06] 表示後の提示先の可視状態の変更には追随しない
- **GIVEN** API 30 以上で提示先の画面がシステムバーを表示した状態でダイアログを表示中
- **WHEN** 提示先の画面がシステムバーを非表示に変更する
- **THEN** ダイアログのウィンドウの可視状態は表示時に採用した値 (表示) のまま変わらない

#### Scenario: [PB-SB-07] 表示後の提示先の behavior の変更には追随しない
- **GIVEN** API 30 以上で提示先の画面が既定の systemBarsBehavior でダイアログを表示中
- **WHEN** 提示先の画面が systemBarsBehavior を変更する
- **THEN** ダイアログのウィンドウの behavior は表示時に採用した値のまま変わらない
