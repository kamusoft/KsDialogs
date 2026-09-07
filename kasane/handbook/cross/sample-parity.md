---
kind: rule
applies-when:
  always: false
  paths: ["samples/**"]
  tasks: [Sample のデモ項目の追加・変更, Sample の文言・色トークンの変更, Sample の OS 操作 (戻る・回転) への反応の変更, Sample の撮影支援の起動引数の変更]
title: Sample パリティ規約
description: samples/ の4ルート (ios / android / maui / kmp) が一致させるデモ項目・文言・色トークンと、Sample を検証装置として保つための禁止事項
timestamp: 2026-09-04
---

# Sample パリティ規約

この文書を読むと、`samples/` に置かれた4つのサンプルアプリが**何をどこまで一致させるのか**、なぜ一致が要るのか、Sample を触るときに守ることが分かる。

## 前提となる言葉

| 語 | 意味 |
|---|---|
| **4ルート** | `samples/ios` / `samples/android` / `samples/maui` / `samples/kmp` の4つのサンプルアプリ。以下「ルート」はこの4つを指す |
| **本体** | `samples/` の外にある KsDialogs のライブラリ実体 (iOS Native / Android Native / MAUI / KMP の4形態)。Sample は本体を**利用者と同じ立場で使う側**に立つ |
| **デモ項目** | 一致の単位。メニューに並ぶ1行と、それを押して出るダイアログ、閉じた後の結果表示までをワンセットにしたもの |
| **SampleTheme** | 4ルートが共有する色トークンの集合 (値は下表)。定義そのものはルートごとに同じ値で書かれる |

## なぜ一致させるのか

Sample は**プラットフォーム間パリティの検証装置**である。4ルートの画面を並べて差を見るのが検証の手段なので、Sample 自体がばらつくと、出た差が **本体の仕様差 (= バグ)** なのか **Sample の書き方の差**なのか判別できなくなり、装置として機能しない。

そのため「各プラットフォームで idiomatic なサンプル」ではなく、一字一句同一を採る。使い方の見本としての価値は、同一のサンプルでも同じだけ果たせる。

## 一致の単位はデモ項目

デモ項目は次の3つをワンセットにしたもので、この3つをまとめて4ルート一致させる:

- メニューに並ぶ**デモ項目の文言**
- 表示されるダイアログの内容 (タイトル・本文・ボタン文言・デモデータ)
- 閉じた後の結果表示

デモ項目が専用の画面を伴う場合は、その画面の構成・文言・初期値も同じ一致の対象に含める。

ルート間の対応は**デモ項目の文言**で取る。ダイアログライブラリのサンプルには「画面」がほとんど存在しないため、画面タイトルでは対応が取れない。

## 文言の正

下表は **Sample が画面に出す文言の全体**であり、メニュー画面のタイトルも含む。4ルートの `SampleText` (`samples/<ルート>/.../SampleText.*`) がこの値を持つ。現在のデモ項目は14件 (項目が増えて縦の短い端末で収まらなくなったため、メニューは4ルートともスクロール可能):

| 役割 | 値 |
|---|---|
| メニュー画面のタイトル | `KsDialogs Sample` |
| デモ項目の文言 (対応を取る単位) | `Basic Dialog` / `Layout Dialog` / `Declarative Dialog` / `Text Input Dialog` / `Inline Dialog` / `Transition Dialog` / `Model Dialog` / `Default Loading` / `Custom Loading` / `Default Toast` / `Custom Toast` / `Toast Stack` / `Toast Placement` / `Toast Overlap` |
| Basic Dialog のメッセージ | `こんにちは、KsDialogs!` |
| Layout Dialog のメッセージ | `レイアウト確認` |
| Declarative Dialog のメッセージ | `こんにちは、KsDialogs!` (Basic と同文言。差は実装経路 — 宣言的 UI 登録 — のみ) |
| Text Input Dialog のメッセージ | `メッセージを入力してください` |
| Text Input Dialog の入力欄 | プレースホルダ `ここに入力`・初期入力値なし (空) |
| Inline Dialog のメッセージ | `インライン表示です` |
| Transition Dialog のメッセージ | `トランジションのデモです` |
| Model Dialog のメッセージ | `ViewModel から表示しています` (Basic と同一デザイン。差は実装経路 — ViewModel 主導の呼び出し — のみ) |
| Default Loading の開始メッセージ | `Loading...` |
| Default Loading の途中更新メッセージ | `Soon...` (setMessage による差し替えのデモ) |
| Custom Loading のカスタム View 内の見出し | `カスタムローディング` |
| Custom Loading の百分率表示 (書式) | `<進捗>%` (0〜100 の整数。進捗の帯とともに VM の進捗受け口経由で更新される) |
| Loading 完了の結果表示 | `結果: 完了` (Default / Custom 共通。Loading は結果値を持たないため completed 書式は使わない) |
| Default Toast のメッセージ | `Hello Toast!` |
| Custom Toast の登録経路のメッセージ | `カスタムトースト` |
| Custom Toast のインライン経路のメッセージ | `インライントースト` (1回のデモで登録経路とインライン経路の両方を表示する。インライン側は配置を下寄せにずらして重なりを避ける) |
| Toast Stack の各メッセージ | 1枚目 `Toast 1` / 2枚目 `Toast 2` / 3枚目 `Toast 3: 長いメッセージは複数行に折り返され、コンテンツの高さが確保されることを確認する` (長文は折り返しのデモを兼ねる) |
| Toast Placement のメッセージ | `Placed Toast` |
| Toast Overlap のメッセージ | `Overlap Toast` (Dialog / Loading / Toast を固定時系列で同時表示する機能間デモ) |
| Toast Overlap 完了の結果表示 | `結果: 完了` (Toast は結果値を持たないため固定文言) |
| 完了操作 | `OK` |
| キャンセル操作 | `キャンセル` |
| 結果表示エリアの見出し | `直近の結果` |
| キャンセルの結果表示 | `結果: cancelled` |
| 完了の結果表示 (書式) | `結果: completed(<結果値>)` — 真偽値は `結果: completed(true)`、文字列は `結果: completed("<入力値>")` (引用符付き) |

画面遷移を表す記号も文言と同じく一致の対象とする。`SampleText` ではなく各ルートの View 側の定数として持つ:

| 役割 | 値 |
|---|---|
| メニュー行の遷移記号 | `›` |
| 画面を離れて戻る記号 | `‹` |

戻る記号には、**読み上げ用の名前 `戻る` とボタンとしての役割**を4ルート一致で与える (画面に出るのは記号 `‹` のままで、スクリーンリーダーにだけ効く。付け方は OS 標準の手段でよい)。

### Layout Dialog の属性調整パネル

`Layout Dialog` はメニューから開く専用画面 (属性調整パネル) を持ち、そこで調整した配置でダイアログを出す。パネルの文言と初期値も一致の対象:

| 役割 | 値 |
|---|---|
| パネルの画面タイトル | `Layout Dialog` (デモ項目の文言と同一) |
| 配置を選ぶ行 | `Horizontal` / `Vertical` |
| 配置の選択肢 | `Start` / `Center` / `End` |
| 移動量を入れる行 | `OffsetX` / `OffsetY` (初期値 `0`) |
| 基準領域を切り替える行 | `Use visible area` (初期は ON = 可視領域基準) |
| 表示操作 | `Show` |

パネルの初期値は**契約の既定値** (中央配置・移動なし・可視領域基準) と一致させる。パネルは表示操作のあとも開いたままにし、配置を続けて試せるようにする。メニューへは画面左上の戻る記号で戻る。

結果は**メニューの結果表示エリアとパネル内の結果表示エリアの両方**に出す。どちらも一度も結果が出ていない初期状態では表示しない。

### Transition Dialog のデモ画面

`Transition Dialog` もメニューから開く専用画面を持ち、出入りの演出 ([トランジションのルール](../../concepts/core/api/transition-semantics.md)) を選んでからダイアログを出す。画面の文言と初期値も一致の対象:

| 役割 | 値 |
|---|---|
| デモ画面のタイトル | `Transition Dialog` (デモ項目の文言と同一) |
| 演出を選ぶ区画の見出し | `プリセット` |
| 演出の選択肢 | `Fade` / `Slide Up` / `Slide Down` / `Slide Start` / `Slide End` / `Zoom` / `None` / `Custom Hook` |
| 調整の区画の見出し | `調整` |
| 調整の行 | `時間` / `イージング` |
| 時間の表示 | `<値> ms` |
| イージングの選択肢 | `Standard` / `Linear` / `Accelerate` / `Decelerate` |
| 表示操作 | `表示` |

画面の規範 (4ルート同一):

| 項目 | 規範 |
|---|---|
| 初期選択 | `Fade` / 250 ms / `Standard` (契約の既定の演出と同じ) |
| 時間 | 100〜600 ms、刻み 10 ms (スライダ) |
| 演出の選択肢と本体の対応 | `Slide Up` = 下辺から (`from: bottom`) / `Slide Down` = 上辺から (`top`) / `Slide Start` = 前端から (`leading` / `START`) / `Slide End` = 後端から (`trailing` / `END`)。`None` は中身の演出なし |
| イージングと本体の対応 | `Standard` = 加速して減速する標準の曲線 (iOS `.standard` / Android `AccelerateDecelerateInterpolator` / MAUI `Easing.CubicInOut`) — `Linear` = 等速 — `Accelerate` = 加速 (`.easeIn` / `AccelerateInterpolator` / `Easing.CubicIn`) — `Decelerate` = 減速 (`.easeOut` / `DecelerateInterpolator` / `Easing.CubicOut`) |
| `None` 選択時 | 時間・イージングの調整を無効表示 (操作不可、値は保持) |
| `Custom Hook` 選択時 | 調整値を使わず、自作の演出を固定で実演する — 入場は上方向へ 80 (論理単位) 移動しながらフェードイン、退場はフェードアウト、いずれも 300 ms・標準の曲線、覆いのフェードは既定 (250 ms)。時間・イージングの調整は無効表示 |
| 無効表示の見え方 | 不透明度 0.4 + 操作不可 (値が読める濃さを残す) |
| ダイアログ | Basic Dialog と同一デザイン。`OK` = completed(true) / `キャンセル` = cancelled |

結果の出し方と戻る導線は Layout Dialog のパネルと同じ (メニューと画面内の両方に出す・左上の戻る記号で戻る)。操作部の読み上げも「パネル操作部の読み上げ」の規約に従う。

表示操作の表記は、Layout Dialog のパネル (`Show`) と Transition Dialog のデモ画面 (`表示`) で画面ごとに異なる。この差は**画面ごとの表記として許容する** (オーナー判断 2026-08-22) — パリティの一致はルート間 (同じ画面を4ルートで並べたとき) に課すもので、画面をまたぐ表記の統一は課さない。ただし新しい画面を足すときは既存画面の同じ役割の表記を引いてから決める (意図して違える場合はその理由を brief に残す)。

#### パネル操作部の読み上げ

戻る記号の読み上げ規約と同型で、属性調整パネルの操作部 (配置の選択肢・移動量欄・基準領域トグル) にも**読み上げ用の名前・役割・状態**を4ルート一致で与える (画面の見た目は変えず、スクリーンリーダーにだけ効く):

- 読み上げ名は画面の文言と同一を基本とする
- 同名になる選択肢 (Horizontal / Vertical それぞれの `Start` / `Center` / `End`) だけは「所属行の文言 + 選択肢の文言」の複合名 (例 `Horizontal Start`) とする
- 状態 (選択肢の選択状態・トグルの ON/OFF・移動量欄の現在値) は OS 標準の役割・状態機構に載せる (読み上げ名への埋め込みで代用しない)
- 検証は accessibility tree の検査 (自動検査または実機スクリーンリーダー) で行い、4ルートの証跡を残す

## 色トークン (SampleTheme)

各ルートは下表の色トークンに従い、**同一の RGBA を各自の実装内に持つ**。ルート間で定義コードを共有はしない (理由は下の「利用者と同じ側から使う」):

| トークン | 値 |
|---|---|
| primary | `#2563EB` |
| on-primary | `#FFFFFF` |
| surface | `#FFFFFF` |
| on-surface | `#1F2937` |
| on-surface-muted | `#6B7280` |
| divider (区切り線) | `#E5E7EB` |
| surface-variant (無彩色ボタンの塗り) | `#F3F4F6` |

背景を暗くする覆い (scrim) は、ダイアログを載せる各 OS 側のコンテナとして**本体が描く**。Sample 側では色を持たない。

## 利用者と同じ側から使う

Sample は配布物ではなく、**本体を利用者と同じ立場で組み合わせた、実行できる参考実装**である。この立場から次が導かれる:

- 参照するのは、本体が**公開 API として配布するビルド成果物**だけ。本体の内部実装を直接参照しない
- 現状の参照は**ローカルのソース参照**であり、パッケージとして配布されたものを取得しているわけではない
- **ルート間でコードを共有しない**。共有すると「利用者と同じ側から組み立てられるか」を検証したことにならない。そのため `samples/kmp` が登録するネイティブの View は `samples/ios` ・ `samples/android` と重複するが、各ルートで各自書く

## 保証すること

- 4ルートの Sample は**常に同一のメニュー構成・文言・SampleTheme の色**を持つ (下の「例外枠」に置いたものを除く)。したがって画面に出た差は本体の仕様差として扱ってよい
- デモ項目はその文言で4ルート1対1に対応する。対応の取れないデモ項目は存在しない (同じく例外枠を除く)

## OS 操作への反応

デモ項目の一致に加えて、ダイアログや専用画面を表示している間に OS の操作が起きたときの結果も4ルートで一致させる。OS 操作への反応は「本体の仕様差」と「Sample の器の作り (Activity の再生成など)」が混ざって見える場所であり、Sample 側の差を残すと本体の差が判別できなくなるため:

| 操作 | 4ルート共通の結果 |
|---|---|
| 端末の戻る操作 (ダイアログ表示中) | ダイアログが閉じ、結果表示は `結果: cancelled`。本体の器が受けるので、Sample 側で戻るを横取りしない |
| 端末の戻る操作 (専用画面を開いている間) | 専用画面を閉じてメニューへ戻る (画面左上の `‹` と同じ結果) |
| 端末の戻る操作 (メニュー表示中) | OS 既定の動き (アプリを離れる) |
| 画面の回転 (ダイアログ表示中) | ダイアログは閉じず、新しい寸法で再配置される |
| 画面の回転 (専用画面を開いている間) | 専用画面は開いたまま再レイアウトされる |

Android 系のルート (android / kmp) では Activity の再生成がダイアログの器消失 (cancelled) になるため、`MainActivity` に MAUI Android Sample と同じ `configChanges` の集合 (orientation / screenSize / screenLayout / smallestScreenSize / uiMode / density) を持たせて再生成を避ける。戻るは予測型戻り (targetSdk 36 以降は既定で有効になり、旧 `onBackPressed` は呼ばれない) に対応した受け口 (`OnBackPressedCallback`) で受ける。この表の反応を変える・確かめるときは、実機で「表示中に操作する」手順を踏み、結果表示と回転後の画面を証跡に残す。

## 例外枠 (パリティの対象外)

次の3つは一致の対象から外す。いずれも**4ルートを見比べても本体の仕様差を検出できない**ものであり、パリティの対象にしても検証装置としての意味を持たないため:

- **プラットフォーム固有 API のデモ**: そのプラットフォームにしか存在しない機能は、比べる相手がない
- **技術検証用の画面**: 特定の仕組みが動くかを確かめるための画面であり、4ルートで見比べるために作られていない
- **撮影支援機構**: Sample の画面を機械的に撮るために置く仕掛け (起動引数の受け口・指定デモの自動再生・進捗の刻み間隔の可変化など)。**画面に何も出さず、引数を渡さない起動では挙動が従来と変わらない**ため、デモ項目の一致要件 (メニューの文言・ダイアログの内容・結果表示) の枠外に置く

撮影支援機構が枠外なのは「デモ項目としての一致を課さない」という意味であり、**4ルートで揃えなくてよいという意味ではない**。撮影は4ルートを同じ手順で撮って並べるために使うので、機構そのものには別の一致要件がかかる — 引数のキー名・デモを指す安定 ID・不正な値を渡したときの倒れ方を4ルートで同じにする (外部から見える契約は揃え、置き場や型の作りはルートごとに idiomatic でよい)。

## 撮影支援の起動引数

4ルートの Sample は、画面を機械的に撮るための設定を起動時に受け取る。引数を渡さない通常起動では、画面・文言・挙動を変えず、撮影支援に固有の表示も出さない。

| キー | 意味 | 受理する値 | 渡さない・受理しない場合 |
|---|---|---|---|
| `demo` | 起動直後に自動再生するデモ | 下表の安定デモ ID 14 件 | 自動再生せず通常メニューを表示する |
| `loading-step-interval-ms` | Default / Custom Loading の進捗刻み間隔 | 1〜600000 の整数 (ms) | 既定値 400 ms を使う |

キーの直後に値がない場合、空文字、範囲外、未定義のデモ ID は、そのキーだけを無視して既定動作へ倒す。起動そのものは失敗させない。自動再生はプロセス起動につき 1 回だけで、画面や Activity の再生成では繰り返さない。撮り直すときは、起動中のアプリへ再配達せず終了してから起動し直す。

### OS ごとの外部表現

| OS 系統 | 渡し方 | 例 |
|---|---|---|
| iOS (`ios` / MAUI iOS / KMP iOS) | launch arguments の `--<キー> <値>` を隣接トークンで渡す | `--demo default-loading --loading-step-interval-ms 2000` |
| Android (`android` / MAUI Android / KMP Android) | 同名キーの string extra (`--es`) で渡す。数値も string とし `--ei` は使わない | `--es demo default-loading --es loading-step-interval-ms 2000` |

### アプリ識別子

| ルート | iOS bundle id | Android package / activity |
|---|---|---|
| ios | `jp.kamusoft.ksdialogs.samples.ios` | — |
| android | — | `jp.kamusoft.ksdialogs.samples.android` / `.MainActivity` |
| maui | `jp.kamusoft.ksdialogs.samples.maui` | `jp.kamusoft.ksdialogs.samples.maui` / `adb shell cmd package resolve-activity --brief <package>` で activity を引く |
| kmp | `jp.kamusoft.ksdialogs.samples.kmp.ios` | `jp.kamusoft.ksdialogs.samples.kmp.android` / `.MainActivity` |

### 安定デモ ID

| 安定デモ ID | メニュー項目 | 起動直後の状態 |
|---|---|---|
| `basic-dialog` | `Basic Dialog` | ダイアログを表示 |
| `declarative-dialog` | `Declarative Dialog` | ダイアログを表示 |
| `model-dialog` | `Model Dialog` | ダイアログを表示 |
| `text-input-dialog` | `Text Input Dialog` | ダイアログを表示 |
| `inline-dialog` | `Inline Dialog` | ダイアログを表示 |
| `transition-dialog` | `Transition Dialog` | デモ画面を表示 |
| `layout-dialog` | `Layout Dialog` | 属性調整パネルを表示 |
| `default-loading` | `Default Loading` | Loading を開始 |
| `custom-loading` | `Custom Loading` | Loading を開始 |
| `default-toast` | `Default Toast` | Toast を表示 |
| `custom-toast` | `Custom Toast` | カスタム Toast を 2 枚表示 |
| `toast-stack` | `Toast Stack` | Toast を 3 枚表示 |
| `toast-placement` | `Toast Placement` | Toast を上部中央に表示 |
| `toast-overlap` | `Toast Overlap` | Toast・Dialog・Loading の時系列を開始 |

自動再生はメニュー項目をタップしたときと同じ入口を使う。自動再生後の操作と結果表示は、手動で起動した場合と同じである。

## してはいけないこと

- **デモ項目を1ルートだけに足して放置する**: 追加・変更は4ルート一斉が原則。追跡を残した一時的な片側先行は許容するが、追跡なしで放置するとパリティが崩れ、検証装置でなくなる
- **片側だけの改善**: 「このルートだけ見栄えを良くする」も、上と同じ理由で禁止
- **プラットフォーム固有の semantic color を使う**: OS ごとに色が変わると、色差の原因が本体にあるのか OS にあるのかを判別できない
- **観測用の一時改変を戻さないまま終える**: 挙動を観察するために2枚重ね表示や入力欄つきの検証用 View を足したときは、観測後に原本へ戻し、チェックサムの一致確認まで行う。戻し漏れはパリティという前提そのものを崩す
- **Sample を配布物として扱う / 挙動の正として扱う / 自動テストの代わりに使う**: 挙動の正は本体側の概念文書と各変更の仕様記述にあり、Sample はそれを踏んで動かしてみる参考実装にすぎない
- **ローカルのソース参照でビルドが通ったことを、「公開リポジトリからの配布が成立した」と説明する**: 配布の検証はしていない

## 出典

- [cross/ADR-0006](../../decisions/cross/0006-samples-aggregated-consumer-boundary.md) — Sample の集約配置と、ルートごとの参照方式
- [cross/ADR-0007](../../decisions/cross/0007-sample-parity-demo-item-unit.md) — 一致の単位をデモ項目とした決定
- [changes/archive/2026-09-04-fix-sample-android-back-and-rotation](../../changes/archive/2026-09-04-fix-sample-android-back-and-rotation/exploration.md) — OS 操作 (戻る・回転) への反応を一致対象に加えた探索と実機証跡
