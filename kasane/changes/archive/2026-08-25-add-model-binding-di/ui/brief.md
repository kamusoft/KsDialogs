# UI Brief: add-model-binding-di

## 画面と状態

UI の変更は Sample 4ルートへのデモ項目 `Model Dialog` の追加のみ。新規の画面・レイアウト・見た目は作らない。

- メニュー画面: 既存のデモ項目リストに `Model Dialog` の行を1行追加 (既存行と同じ行デザイン・遷移記号 `›` なし — ダイアログ直接表示型の既存項目 (Basic Dialog 等) と同じ扱い)
- ダイアログ: **既存 Basic Dialog と同一デザインの完全再利用** — メッセージ文言 (`ViewModel から表示しています`) だけが異なる。状態は表示中 / 完了 / キャンセルのみで、loading / empty / error 状態は持たない
- 結果表示: 既存の結果表示エリア (`直近の結果`) をそのまま使う。新規 UI なし

## リファレンス注釈

references/ なし (新規デザインが存在しないため)。見た目の正は各ルートの既存 Basic Dialog 実装。

## デザイントークン参照

SampleTheme (cross concepts sample-parity の色トークン表) に従う。本変更での追加・変更なし。

## 承認モック

新規の見た目が存在しない (既存 Basic Dialog デザインの完全再利用・差分なし) ため、mock を省略する — オーナー判断 2026-08-24 (second-opinion-spec-001 #8 の解決)。実装時の視覚照合は「Model Dialog のダイアログが各ルートの既存 Basic Dialog と同一に見えること」を基準とする。

## 照合結果

`verification/` の各画像 (`<ルート>-basic-dialog.png` / `<ルート>-model-dialog.png` の5組) を、
承認モックの代わりとなる基準 = 同じルートの既存 Basic Dialog と並べて照合した (2026-08-25)。
撮影は iOS Simulator (iPhone 17) と Android エミュレータ (Pixel 6) で行い、保存した画像を開いて
個人を特定する要素が写っていないことを確認済み (実機は使っていない)。

| ルート | 照合 | 結果 |
|---|---|---|
| ios | `ios-basic-dialog.png` ↔ `ios-model-dialog.png` | カードの左右端 (x=195〜1011) と上下端 (y=1164〜1541) が一致。差はメッセージ文言と、覆いの下に透けるメニュー行・結果表示だけ |
| android | `android-basic-dialog.png` ↔ `android-model-dialog.png` | 同上 (x=183〜897)。ボタン帯 (カード下半分) は画素単位で完全一致 |
| maui | `maui-basic-dialog.png` ↔ `maui-model-dialog.png` | 同上。ボタン帯は画素単位で完全一致 |
| kmp (iOS) | `kmp-ios-basic-dialog.png` ↔ `kmp-ios-model-dialog.png` | ios ルートと同じ結果 |
| kmp (Android) | `kmp-android-basic-dialog.png` ↔ `kmp-android-model-dialog.png` | android ルートと同じ結果。ボタン帯は画素単位で完全一致 |

構造 (メッセージ + 等幅2ボタン)・トークン (SampleTheme の primary / on-primary / surface /
on-surface / surface-variant)・意図 (完了操作を primary で強調) はすべて Basic Dialog と同一。
MAUI ルートの iOS 面だけは環境制約 (.NET for iOS 26.1.10502 が Xcode 26.1 を要求・現行 26.5) で
ビルドできず未撮影 — MAUI は Android 面で照合した。

視覚照合ループは1周で収束 (乖離ゼロ)。合意済み妥協は0件、トークン候補も0件 (生値の追加なし)。

## 未解決 (オーナー確認待ち) → 解決済み

下記はオーナー判断 2026-08-25 で解決: **`›` ありを承認** (既存行と同じ行デザインを正とする。「遷移記号 `›` なし」の記述は書き誤り)。

- 「画面と状態」の `遷移記号 ›  なし` は、実装されている既存メニュー行と食い違う — Basic Dialog を含む
  既存の全行が `›` を表示しており (`verification/*-basic-dialog.png` で確認可能)、
  sample-parity 規約も `›` をメニュー行の記号として4ルート一致で課している。
  同じ文の `既存行と同じ行デザイン` / `Basic Dialog 等と同じ扱い` に従い、
  Model Dialog の行も既存の行部品をそのまま使った (= `›` が付く)。
  `›` を落とす意図だったのなら、Basic Dialog 等の既存行も含めた変更になるため別途の判断が要る。
