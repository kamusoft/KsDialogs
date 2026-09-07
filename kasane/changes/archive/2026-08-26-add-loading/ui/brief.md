# UI Brief: add-loading

## 画面と状態

- **既定ローディング** (ライブラリ同梱の内蔵コンテンツ — core/ADR-0023):
  - 状態: メッセージのみ (進捗未報告) / 進捗あり (フォーマット関数の結果) / メッセージ更新後
  - 覆いは器メタ属性 overlayColor — 本体の器が描く。既定値は [レイアウトのルール](../../../concepts/core/api/layout-semantics.md) の DialogOptions 既定値を参照。既定ローディングでは Loading の設定プロパティ (DialogOptions 再利用) で変更可能
  - スタイル (インジケータ色・フォント・既定メッセージ・フォーマット) は LoadingStyle で一括設定可。モックは既定値の見え
- **Custom Loading デモ View** (Sample 側の参考実装):
  - 状態: 進捗 0% / 進捗反映中 (VM の進捗受け口経由)
  - SampleTheme トークン準拠

## リファレンス注釈

- 原典 (AiForms.Maui.Dialogs) の既定ローディング: 素のスピナー + 白テキストを覆いに直置き (カードなし)。README のスクリーンショットが参照元 (画像の持ち込みはなし)

## デザイントークン参照

- Sample デモ View: [Sample パリティ規約の SampleTheme](../../../concepts/cross/conventions/sample-parity.md) (primary / surface / on-surface / on-surface-muted / surface-variant)
- 既定ローディング: ライブラリ既定値 (LoadingStyle の既定。モックの値が実装の既定値の指標になる)

## 承認モック

- 既定ローディング: **mock/default-plain.html (案A: 原典踏襲) を採用** (approved.png、2026-08-25 オーナー承認)。案B (default-card.html) は不採用 — 原典の見えの継承を優先し、カード風はカスタム View で実現可能なため
- Sample の Custom Loading デモ View: **mock/sample-custom.html を採用** (approved-sample-custom.png、同日承認・修正指示なし)

## 実装で確定した既定値 (iOS Native / 既定ローディング)

`LoadingStyle` の既定値は、モックの見え + 原典 (AiForms.Maui.Dialogs の `DefaultLoading` / `LoadingConfig`) の値で確定した。

| 項目 | 既定値 | 根拠 |
|---|---|---|
| indicatorColor | 白 | モック (白スピナー) / 原典 `IndicatorColor = White` |
| messageFontSize | 14 | モックの本文サイズ / 原典 `FontSize = 14` |
| messageColor | 白 | モック / 原典 `FontColor = White` |
| defaultMessage | なし (nil) | 原典 `DefaultMessage` は未設定 |
| progressFormat | メッセージ + 改行 + 百分率 (小数なし) | モック状態2 / 原典 `ProgressMessageFormat = "{0}\n{1:P0}"` |
| インジケータとメッセージの間隔 | 20pt | 原典の制約 (ラベル上端 = スピナー下端 + 20) |
| インジケータの大きさ | `UIActivityIndicatorView` の large | 原典と同じ (モックのスピナー比率と同等) |
| メッセージの行間 | 8pt | オーナー指示 (下記の合意済み差分) |
| メッセージの太さ | bold | オーナー指示 (下記の合意済み差分) |

覆いの色 (40% 黒) は器メタ属性 `DialogOptions.overlayColor` の既定値で、この表の対象外。

**トークン候補**: 上記のうち間隔 20pt / large は `LoadingStyle` の設定項目ではなく内蔵コンテンツ側の固定値。デザイントークンの定義はまだ無いため生値のまま置いている。

## 照合結果 (既定ローディング / iOS Native)

`ui/verification/ios-default-loading-message.png` (状態1: メッセージのみ) と
`ui/verification/ios-default-loading-progress.png` (状態2: 進捗 45%) を `mock/approved.png` と4観点で照合し、
**1周で収束** (2026-08-26)。撮影は iOS Simulator (iPhone 17) の Sample 画面上で行い、個人要素の写り込みが
無いことを保存画像で確認済み。

| 観点 | 結果 |
|---|---|
| 構造 | 一致 — 覆いの中央にスピナー、その下にメッセージ。カード・枠なし |
| トークン | 一致 — 白スピナー / 白文字 14pt / 覆いは 40% 黒 |
| 状態 | 一致 — 状態1「Loading...」、状態2「Loading...」+ 改行 + 「45%」。メッセージ更新後はテキストのみ差し替わる (テストで担保) |
| 意図 | 一致 — 背景の操作を覆いで遮り、進捗はメッセージの従属行として二次的に置かれる |

残差 (許容範囲): モックの HTML はスピナーとテキストの間隔を 14px、実装は 20pt。原典の値を採り、
モックの図示比率とも整合するため乖離としない。

### 合意済み差分 (モックに対するオーナー指示の調整、2026-08-26)

照合画像を見たオーナーの指示で、モックの見えから次の2点を変えた。どちらも内蔵コンテンツ View 側の
描画で与える固定値で、`LoadingStyle` の設定項目 (インジケータ色・フォントの大きさと色・既定メッセージ・
進捗フォーマット) は増やしていない。

- **メッセージの行間: 8pt** — モックではメッセージとパーセントの行が詰まって1つの塊に見える。
  フォーマット文字列の改行を増やすのではなく、ラベルの段落設定 (`lineSpacing`) で行間を広げた。
  フォーマット関数は表示テキストの文言だけを決め、行間の見えは持たない
- **メッセージの太さ: bold** — モックは通常のウェイト。覆いの上での読み取りやすさを優先し、
  内蔵コンテンツの既定の見えとして太字を採る。大きさと色はこれまでどおりスタイルの指定に従う

この2点を反映した画像で `ui/verification/` の2枚 (メッセージのみ / 進捗 45%) を撮り直している。

**オーナーの最終承認: 取得済み** — verification/ の各画像 (調整後の2枚) と approved.png を並べて提示し、2026-08-26 最終承認。合意済み差分は上記2件 (行間 8pt / bold)。Android ミラー実装 (3.4) は同じ確定値 (白・14・bold・行間 8・間隔 20 相当) を指標とする。

## 照合結果 (既定ローディング / Android Native)

`ui/verification/android-default-loading-message.png` (状態1: メッセージのみ) と
`ui/verification/android-default-loading-progress.png` (状態2: 進捗 45%) を `mock/approved.png` および
iOS の照合画像と4観点で照合し、**1周で収束** (2026-08-26)。撮影は Pixel 4a 実機 (API 33) の Sample
メニュー画面上で行い、通知アイコンが写るステータスバーを切り落としてから保存した (個人要素なしを
保存画像で確認済み)。Sample にはまだ Loading のデモ項目が無い (tasks 6.1) ため、撮影のあいだだけ
Sample 側に既定ローディングを出す一時コードを入れ、撮影後に取り消している (Sample のソースは無変更)。

| 観点 | 結果 |
|---|---|
| 構造 | 一致 — 覆いの中央にインジケータ、その下にメッセージ。カード・枠なし |
| トークン | 一致 — 白インジケータ / 白文字 14sp bold / 行間 8dp / 間隔 20dp / 覆いは器の既定 |
| 状態 | 一致 — 状態1「Loading...」、状態2「Loading...」+ 改行 + 「45%」。メッセージ更新後はテキストのみ差し替わる (テストで担保) |
| 意図 | 一致 — 背景の操作を覆いで遮り、進捗はメッセージの従属行として二次的に置かれる |

残差 (許容範囲): インジケータの大きさが iOS (`UIActivityIndicatorView` の large) より相対的に大きい。
Android は各 OS の「大」の標準指定 (`progressBarStyleLarge`) を採ったためで、モックのスピナーと
メッセージの幅比にはむしろ近い。OS 標準コントロールの寸法差であり乖離としない。

**トークン候補** (iOS と共通): 間隔 20dp / 行間 8dp / インジケータの「大」指定は `LoadingStyle` の
設定項目ではなく内蔵コンテンツ側の固定値。デザイントークンの定義がまだ無いため生値のまま置いている。

**オーナーの最終承認: 取得済み** — verification/ の Android 2枚を approved.png / iOS 照合画像と並べて提示し、2026-08-26 最終承認。合意済み妥協は1件 (OS 標準の大インジケータの寸法差)。

## 照合結果 (Custom Loading デモ View / iOS Native・Android Native)

`ui/verification/ios-sample-custom-start.png` (状態1: 進捗 0%) / `ios-sample-custom-progress.png` (状態2: 進捗 50%) と
`ui/verification/android-sample-custom-start.png` (状態1) / `android-sample-custom-progress.png` (状態2: 進捗 50%) を
`mock/approved-sample-custom.png` と4観点で照合し、**1周で収束** (2026-08-26)。
撮影は iPhone 17 シミュレータ と Pixel 4a 実機で行い、Android はステータスバーを切り落としてから保存した
(個人要素なしを保存画像で確認済み)。

| 観点 | 結果 |
|---|---|
| 構造 | 一致 — 覆いの中央にカード。上から見出し / 進捗の帯 / 百分率の3段 |
| トークン | 一致 — カードは surface・角丸 14・内余白 20・段の間隔 12、見出し 14 太字 on-surface、帯は高さ 8・角丸 4 で地が surface-variant・塗りが primary、百分率 12 on-surface-muted |
| 状態 | 一致 — 状態1 は帯が空で `0%`、状態2 は VM の進捗受け口経由で帯と百分率が更新される |
| 意図 | 一致 — 進捗そのものが主役で、見出しは上の小さな添え、数値は帯の従属情報として下に置かれる |

残差 (許容範囲): モックの HTML はカードに影 (0 4px 16px rgba(0,0,0,.25)) を敷いているが、実装は影なし。
既存のデモ用カード (Basic / Model ほか) がいずれも影を持たないため、Sample 内の見えを揃える方を採った。

**トークン候補**: カード幅 210 / 角丸 14 / 内余白 20 / 段の間隔 12 / 帯の高さ 8・角丸 4 は SampleTheme (色トークン) に
無い寸法で、各ルートの View 側に同じ生値で置いている。Sample の寸法トークンの定義はまだ無い。

MAUI / KMP はパリティ実装として同じ構成・同じ値で書き、実機・シミュレータでの起動と通し
(0% → 進捗反映 → 完了で `結果: 完了`) を確認した (詳細照合は 6.3 の通しで補完)。
中間状態の撮影のため、iOS Native / KMP / MAUI では刻みの待ち時間の定数を一時的に伸ばしたビルドで撮影し、
撮影後に原本へ戻してチェックサムの一致を確認している (Sample のソースは無変更)。

**スピナー描画の確認 (前フェーズからの申し送り)**: 既定ローディングの中間フレームを iOS (シミュレータ) /
Android (実機) の両方で連続撮影して確認した結果、**描画不具合ではなく撮影の一瞬**だった。
iOS は `UIActivityIndicatorView` の放射状の羽根がそのまま写り、Android は Material の円弧インジケータが
掃引の位相によって短い円弧になる瞬間があり、静止画ではこれが白い小さな矩形のように見える。
出の演出中 (フェード) のフレームでも形は保たれている。

**オーナーの最終承認: 取得済み** — verification/ の Custom Loading 画像 (iOS / Android) を
approved-sample-custom.png と並べて提示し、2026-08-26 最終承認。合意済み妥協は1件 (カード影なし)。
