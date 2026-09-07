# UI Brief: add-presentation-behavior

## 画面と状態

1. **Sample メニュー画面**: expand-api-surface 承認モックの5項目に `Transition Dialog` を追加して6項目にする。結果表示エリアの挙動は既存踏襲 (初期非表示)
2. **Transition デモ画面 (新規)**: プリセット選択 (Fade / Slide 4方向 / Zoom / None) + 時間・イージングの調整 + カスタムフックの実演 + 表示ボタン。ダイアログを閉じると結果が結果エリアに表示される (既存と同形)
3. **デモで出すダイアログ**: 見た目は Basic Dialog と同一デザイン (差はトランジションのみ)。専用モックは作らない

## リファレンス注釈

- `references/api-surface-approved.png` (expand-api-surface の承認モック、2026-08-21 引き継ぎ): カード形状・ボタン形状・トークン・メニュー行48・結果エリアをすべて踏襲。本変更で上書きする要素はない
- `references/layout-spec-approved-panel.png` (add-layout-spec の属性調整パネル承認モック、2026-08-21 引き継ぎ): 「調整パネル + 表示ボタン」の構成パターンの先例として参照。Transition デモの操作部はこのパターンの系統で設計する

## デザイントークン参照

- expand-api-surface 承認モックのトークンセット (primary / on-primary / surface / surface-variant / on-surface / on-surface-muted / scrim / divider) をそのまま使用。新規トークンなし

## 動的モック (プリセットの動作イメージ)

- `mock/mock-preset-motion.html`: プリセット (fade / slide 4方向 / zoom / none) の動きをブラウザで再生して事前確認する動的モック。時間・イージングの変更も試せる。**動作イメージの事前合意用**であり、アニメーションの最終検証は実機確認 + 証跡記録 (design Decision 7)
- 動的モックが表現する契約: オーバーレイの出現/消滅はプリセットによらず常にライブラリが駆動し (design Decision 2)、コンテンツのホスト View だけがプリセットの対象になる。覆いとコンテンツは兄弟レイヤ (覆いのフェードがコンテンツの演出を巻き込まない)。覆いのフェード時間は本体のトランジションに揃う (イージングは覆いには標準)。**動的モックは参考であり受け入れ基準ではない** — 受け入れ基準は dialog-contract の Scenario (順序と完了待ち) と実機確認の証跡

## 文言表 (パリティの正 — 4ルート一字一句一致)

| 場所 | 文言 |
|---|---|
| メニュー項目6 | `Transition Dialog` |
| プリセット選択肢 | `Fade` / `Slide Up` / `Slide Down` / `Slide Start` / `Slide End` / `Zoom` / `None` |
| カスタム実演の選択肢 | `Custom Hook` |
| 調整ラベル | `時間` / `イージング` |
| イージング選択肢 | `Standard` / `Linear` / `Accelerate` / `Decelerate` |
| 表示ボタン | `表示` |
| ダイアログ本文 | `トランジションのデモです` |
| ボタン (ダイアログ内) | `キャンセル` / `OK` |
| 結果表示 | 既存規約どおり (`結果: completed(true)` / `結果: cancelled`) |

## 調整面の規範表 (パリティの正 — 4ルート同一)

| 項目 | 規範 |
|---|---|
| 初期選択 | プリセット `Fade` / 時間 250 ms / イージング `Standard` |
| 時間 | 100〜600 ms、刻み 10 ms。スライダ (案B)。表示は `<値> ms` |
| イージング写像 | `Standard` = iOS `.standard` (easeInOut) / Android `AccelerateDecelerateInterpolator` / MAUI `Easing.CubicInOut` — `Linear` = `.linear` / `LinearInterpolator` / `Easing.Linear` — `Accelerate` = `.easeIn` / `AccelerateInterpolator` / `Easing.CubicIn` — `Decelerate` = `.easeOut` / `DecelerateInterpolator` / `Easing.CubicOut` |
| 方向写像 | `Slide Up` = `from: bottom` (下から上へ入場) / `Slide Down` = `from: top` / `Slide Start` = `from: leading (START)` / `Slide End` = `from: trailing (END)` |
| `None` 選択時 | 時間・イージングの調整を無効表示 (操作不可、値は保持) |
| `Custom Hook` 選択時 | 調整値を使わない (固定の自作演出: 入場は上方向 80pt 移動 + フェード、退場はフェード。時間・イージングの調整は無効表示) |
| ダイアログ | Basic Dialog と同一デザイン。`OK` = completed(true) / `キャンセル` = cancelled |

実装完了後の蒸留で cross の sample-parity 規約 (文言表・規範表) へ反映する。

## 承認モック

- **Transition デモ画面: mock/mock-transition-b.html (案B: チップ選択型) を採用** (2026-08-21 オーナー承認)。案A (リスト選択型) は不採用。メニュー6項目化は mock-transition-a.html 左側の phone が正 (案A/B 共通部分)
- **プリセット動的モック: mock/mock-preset-motion.html** — 初版は fill 残留バグ (前回サイクルの最終状態が残り Zoom 以外が見えない) をオーナー指摘で検出し、サイクル開始/終了時のアニメーション cancel で修正 (2026-08-21)。さらに退場時にコンテンツの時間・イージングが反映されない不具合 (ダイアログが覆いの子要素で、覆いの固定フェードに巻き込まれていた) をオーナー指摘で検出し、覆いとコンテンツを兄弟レイヤに分離して修正 (2026-08-21、design Decision 2 の「別レイヤ」規定の根拠)。兄弟レイヤ版でオーナー確認「よくなった」(2026-08-21) + 追加要望「覆いのフェード時間を本体に揃える」を反映 (プリセットは同じ duration、none / カスタムは既定 250ms。design Decision 2・3 の `overlayDuration` に昇格)。**2026-08-21 オーナー最終承認** (覆いの時間を本体に揃えた版。覆いのイージングは標準固定のまま)。モック承認ゲート (静的: 案B / 動的: mock-preset-motion.html) はこれで閉じた
- `mock/approved.png`: 案B (mock-transition-b.html) のヘッドレス Chrome 撮影 (420x720、2026-08-21 保存)。メニュー6項目化の見た目は references/api-surface-approved.png の増分 (行追加のみ) のため別撮影なし
- 動的モックの動作イメージ承認: 2026-08-21 オーナー最終承認 (上記「承認モック」節のとおり)

## 実装で決めた値 (4ルート一致の対象)

brief の規範表に規定がなく、実装で決める必要があった値。**4ルート同一にする**ため、
先行して実装した iOS / Android の値をここに記録する (MAUI / KMP はこの値に合わせる)。

| 項目 | 値 | 決めた理由 |
|---|---|---|
| `Custom Hook` の時間 | 300 ms (入場・退場とも) | 規範表は移動量とフェードだけを定めている。既定 250 ms と見分けがつく長さにした |
| `Custom Hook` の移動量 | iOS 80pt / Android 80dp | 規範表の「上方向 80pt 移動」を各 OS の論理単位で表したもの |
| `Custom Hook` の覆いのフェード時間 | 既定 (250 ms、`overlayDuration` 未指定) | 動的モックの合意 (「none / カスタムは既定 250ms」) に合わせた |
| `Custom Hook` のイージング | 標準 (加速して減速する) | 自作演出は調整値を使わないため演出側で固定 |
| 調整部の無効表示 | 不透明度 0.4 + 操作不可 (値は保持) | 規範表は「無効表示」とだけ定めている。値が読める濃さを残した |

## モックにない要素 (brief 本文が要求するもの)

承認モック (案B) には描かれていないが、本 brief の「画面と状態」と cross の Sample パリティ規約が
要求するため実装した要素。**見た目は先行 change の承認モック (references/) の対応部分をそのまま踏襲**しており、
新しい見た目を起こしてはいない。

| 要素 | 出典 | 見た目の出どころ |
|---|---|---|
| 画面左上の戻る記号 `‹` (読み上げ名「戻る」) | Sample パリティ規約「パネルは画面左上の戻る記号でメニューへ戻る」 | references/layout-spec-approved-panel.png のヘッダ |
| 結果表示エリア (`直近の結果` + 結果文言) | brief「画面と状態」2「ダイアログを閉じると結果が結果エリアに表示される (既存と同形)」 | references/api-surface-approved.png の結果エリア。パネル内は Layout パネルと同じく surface-variant の帯にする |

承認モックの選択チップが `Slide Up` になっているのは選択状態の見本であり、初期選択は規範表どおり `Fade`。

## トークン候補

なし。案B の色はすべて既存のトークンセット (primary / on-primary / surface / surface-variant /
on-surface / on-surface-muted / divider) に収まり、生値のハードコードは残っていない。
チップの角丸 10 / 隙間 8 / 高さ 44 (Android は 48) は寸法であり、本プロジェクトのトークンは色のみを扱う。

## 照合結果

`ui/verification/` の実装スクリーンショットと `mock/approved.png` を4観点で照合した (2026-08-21、**1周で収束**、
乖離による修正は0件)。撮影は iPhone 17 Simulator (iOS 26.5) と Pixel 6a 実機 (API 36)。

| 観点 | iOS | Android | 備考 |
|---|---|---|---|
| 構造 | 一致 | 一致 | タイトル帯 → `プリセット` 見出し → 2列チップ8個 (Fade / Slide Up / Slide Down / Slide Start / Slide End / Zoom / None / Custom Hook) → `調整` 見出し → 時間行 + スライダ → `イージング` 行 + 4チップ → 画面下端に固定した `表示` の順 |
| トークン | 一致 | 一致 | 選択チップ = primary / on-primary、非選択 = surface + divider 枠 + on-surface、見出しと時間の値 = on-surface-muted、区切りと枠 = divider、表示ボタン = primary |
| 状態 | 一致 | 一致 | 初期 (Fade / 250 ms / Standard)・調整の無効表示 (None・Custom Hook)・結果表示あり/なしの3状態を撮影。結果はデモ画面とメニューの両方に出る |
| 意図 | 一致 | 一致 | 「演出を選ぶ」が画面の主役として上半分を占め、調整は従、`表示` は常に手の届く位置に固定 |

**残差 (プラットフォームの流儀による差。要修正ではない)**

- チップの高さ下限は iOS 44pt / Android 48dp。モックは 44px だが、既存の Sample が
  当たり判定の下限を iOS 44 / Android 48 で書き分けている流儀に合わせた
- 無効表示の濃さが Android のほうがやや薄い。OS 標準の無効状態の描画 (スライダのつまみと塗り) が
  不透明度 0.4 に重なるため。どちらも「操作不可・値は保持」は満たしている
- 戻る記号の左余白は iOS が左寄せ、Android が 48dp 幅の中央寄せ。既存の Layout パネルと同じ差であり、
  本変更で新たに生じたものではない

**合意済み妥協: なし** (モック通りにできず相談が要る箇所は出なかった)

**オーナーの最終承認: 未取得** — 実装ワーカーによる照合までが済んだ状態。before/after の提示と承認は
4ルート揃った時点でまとめて行う想定。

演出そのものの確認 (連写による実機証跡と動的モックとの突き合わせ) は
`verification/sample-walkthrough/README.md` にある。

### MAUI / KMP の2ルート (2026-08-21 追記)

`ui/verification/` の `maui-*` / `kmp-*` と `mock/approved.png` を4観点で照合した (**1周で収束**、
乖離による修正は0件)。撮影は iPhone 17 Simulator (iOS 26.5) と Pixel 6a 実機 (API 36)。
先行2ルート (iOS / Android) の実装スクリーンショットも見本として並べて照合した。

| 観点 | MAUI iOS | MAUI Android | KMP iOS | KMP Android | 備考 |
|---|---|---|---|---|---|
| 構造 | 一致 | 一致 | 一致 | 一致 | タイトル帯 → `プリセット` 見出し → 2列チップ8個 → `調整` 見出し → 時間行 + スライダ → `イージング` 行 + 4チップ → 画面下端に固定した `表示` の順 |
| トークン | 一致 | 一致 | 一致 | 一致 | 選択チップ = primary / on-primary、非選択 = surface + divider 枠 + on-surface、見出しと時間の値 = on-surface-muted、区切りと枠 = divider、表示ボタン = primary |
| 状態 | 一致 | 一致 | 一致 | 一致 | 初期・無効表示・結果ありの3状態。MAUI iOS の未撮影分は本体修正後に撮り直して解消 (下記) |
| 意図 | 一致 | 一致 | 一致 | 一致 | 「演出を選ぶ」が画面の主役として上半分を占め、調整は従、`表示` は常に手の届く位置に固定 |

**残差 (プラットフォームの流儀による差。要修正ではない)**

- 先行2ルートと同じ残差 (チップの高さ下限 iOS 44 / Android 48、無効表示の濃さが Android のほうが薄い) が
  MAUI / KMP でもそのまま出ている。原因も同じ
- MAUI の戻る記号は左寄せ (幅 44 の Label の既定の文字揃え) で、Native Android の 48dp 中央寄せとは
  わずかに位置が違う。既存の MAUI Layout パネルと同じ書き方であり、本変更で新たに生じた差ではない
- MAUI Android のスライダの摘みは、地の色と同じ塗りだと見えなくなるため platform 側で縁付きの摘みを
  与えている (Native Android と同じ見た目)。MAUI iOS は OS 既定の摘みがそのまま一致する

**合意済み妥協: なし**

**未取得だった MAUI iOS の3状態は取得済み (2026-08-21 追記)**

`transition-dialog` / `transition-panel-result` / `menu-result` は、SIGSEGV を起こしていた本体の
iOS ブリッジが直ったあとに撮り直して揃えた。撮影は同じ iPhone 17 Simulator (iOS 26.5)。
先行3ルートの同名スクリーンショットと並べて照合した結果、4観点とも一致で、**1周で収束**した
(乖離による修正は0件)。

| 状態 | 見本 (Native iOS) との照合 |
|---|---|
| `maui-ios-transition-dialog.png` | カード・本文 `トランジションのデモです`・`キャンセル` / `OK` の並びと覆いの濃さが `ios-transition-dialog.png` と一致 |
| `maui-ios-transition-panel-result.png` | 結果帯 (`直近の結果` + `結果: completed(true)`) の位置・色・書体が `ios-transition-panel-result.png` と一致 |
| `maui-ios-menu-result.png` | 6項目の下に出る結果帯 (`結果: cancelled`) が `ios-menu-result.png` と一致 |

これで4ルート × 6状態がすべて揃った。この3状態は静止した見た目だけを見る照合であり、
下記の「演出が動かない」所見 (`verification/sample-walkthrough/README.md` の所見3) の影響を受けない。

**オーナーの最終承認: 未取得** — 静止画の照合は4ルート揃ったが、MAUI iOS の演出そのものに
未解決の所見が残っているため、before/after の提示と承認は据え置いている。

### 演出の確認まで含めて4ルートが揃った (2026-08-21 追記)

上の「オーナーの最終承認: 未取得」の理由だった **MAUI iOS の演出の所見 (所見3) は本体側で解消済み**で、
その後に本 brief の「調整面の規範表」が定める**方向写像 (4方向) とイージング写像 (4種)**、および
`None` / `Custom Hook` の**調整の無効化 (操作不可・値は保持)** を MAUI iOS の実機で観察して記録した。
動的モックの動作イメージとの突き合わせも、以前 3 項目が不一致だったところがすべて一致に変わった。

証跡と読み取り方 (覆いの濃さを時計にして中身の位置・大きさを画素で測る手口) は
`verification/sample-walkthrough/README.md` の最終節、画像は同ディレクトリの
`maui-ios-50〜58-*.png` にある。

**据え置きが残っているのはオーナーの最終承認だけ** — 静止画の照合 (4観点 × 4ルート × 6状態) と
演出の実機確認は揃っており、before/after の提示待ちの状態にある。

### 操作部の読み上げ (accessibility tree の証跡、2026-08-21 追記)

見た目の4観点とは別軸で、新設した操作部 (戻る記号・プリセットチップ8個・`時間` スライダ・
イージングチップ4個・`表示`) の**読み上げ名 / 役割 / 選択状態 / 現在値**を、4ルート6形態の
accessibility tree を取り出して照合した。cross の Sample パリティ規約「パネル操作部の読み上げ」が
求める4ルートの証跡にあたる。

証跡と照合表は `verification/panel-accessibility/README.md`。
名前・役割・選択状態・現在値は6形態とも規約を満たす。所見2件を同 README に記録した
(MAUI iOS だけ調整部の無効状態が読み上げに乗らない — 操作不可自体は成立、要判断。
スライダの現在値の読み上げ表現がルートで異なる — 規約違反ではない)。

## 比較シート (オーナー最終承認用)

- `ui/verification/comparison-sheet.png`: 承認モック案B (`mock/approved.png`) と 6 形態 (ios / android / maui-ios / maui-android / kmp-ios / kmp-android) の Transition デモ画面初期状態を同じ高さで並べたもの (2026-08-22 オーケストレーターが生成)。モックは `Slide Up` 選択の見本で、実装の初期値は規範表どおり `Fade`
- **オーナー最終承認: 2026-08-22 承認** (比較シート + ui/verification/ 36 枚 + sample-walkthrough の演出証跡で確認。iOS の入場ちらつきはオーナーの実機確認で却下 → 修正 → 録画フレームで解消確認の上で承認)

