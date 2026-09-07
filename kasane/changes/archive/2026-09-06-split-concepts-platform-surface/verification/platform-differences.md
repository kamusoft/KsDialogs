# core にある platform 差分の記述 (baseline 時点の棚卸し)

`verification/baseline.md` の SHA 時点の `kasane/concepts/core/api/*.md` から、形態によって
違いがある記述を出典つきで拾ったもの。行番号は baseline 時点のもの。

2 つに分けてある:

- **A. 利用者が観察する挙動の差** — 承認済みの platform 差分の挙動。**core に残す**。
  ただし識別子を含まない散文か、識別子を含まない表で書き直し、識別子を伴う具体形を持つ
  platform concept の節へリンクする
- **B. 公開面の差 (core から platform へ移すもの)** — 名前・署名・型・添付の書き方・配布形の違い。
  挙動の差ではないため core には残さず、各 platform の公開面 concept へ移す

再構成後に A の各項目が core に残っていること (識別子なし・リンクあり) を確認する。

## A. 利用者が観察する挙動の差 (core に残す)

| # | 差分 | 出典 (concept / 節 / 行) | 現行の書き方に含まれる識別子 (外す対象) |
|---|---|---|---|
| A1 | **多段表示で下段を先に閉じたときの上段の扱い**。iOS は上下とも画面から消えて上段は cancelled で確定して返る。Android は下だけが閉じ、上段は表示されたまま残り後から通常どおり completed で返る | `multi-display-semantics.md` / 保証しないこと / 39-40 行 (形態表) | なし (表の中は散文) |
| A2 | **iOS で上段が消えるのは OS 発の器の消失**であり、上段に退出の演出を添付していても実行されず即座に cancelled が配送される | `multi-display-semantics.md` / 保証しないこと / 44 行 | なし |
| A3 | **呼び出し元をキャンセルしたときの観察のされ方**。Swift は show が結果として cancelled を返し (throw しない)、Kotlin (Android Native / KMP) はコルーチン規約どおりキャンセル例外が伝播する (内部の結果は cancelled で確定済み) | `result-notification-semantics.md` / 呼び出し元をキャンセルしたときの見え方 / 77-80 行、`transition-semantics.md` / 呼び出し元のキャンセルはどう観察されるか / 211-214 行 (同じ表が 2 か所にある) | `.cancelled` / `CancellationException` / `cancelled` |
| A4 | **MAUI にはこの経路がない** — show が CancellationToken を受け取らないため、呼び出し元キャンセルという観察自体が存在しない | 同上 (2 表の MAUI 行) | なし |
| A5 | **Kotlin ではキャンセル済みのコルーチンからでも退出の演出と撤去を最後まで完遂する** (キャンセルで演出が中途半端に残らない) | `transition-semantics.md` / 呼び出し元のキャンセルはどう観察されるか / 217 行、`result-notification-semantics.md` / 呼び出し元をキャンセルしたときの見え方 / 83 行 | なし (Scenario ID は `-` を含むため識別子として拾われない) |
| A6 | **Android の戻るボタンがキャンセル操作になる**。iOS には戻るボタンに相当するキャンセル経路を設けない | `result-notification-semantics.md` / どの操作がキャンセルになるか / 63 行・66 行 | なし |
| A7 | **Android でソフトキーボード表示中の 1 回目の戻るは OS がキーボードを閉じるのに使い、ダイアログには届かない** (2 回目で cancelled)。ライブラリは保証せず OS の既定に委ねる | `result-notification-semantics.md` / ソフトキーボード表示中の戻るボタン / 69-71 行、`multi-display-semantics.md` / 保証しないこと / 46 行 | なし |
| A8 | **プリセットの `leading` / `trailing` (Android・MAUI は `START` / `END`) はレイアウト方向に追随**し、右から左へ読む環境では左右が入れ替わる。`top` / `bottom` は物理方向で変わらない | `transition-semantics.md` / プリセット / 105 行 | `leading` / `trailing` / `START` / `END` / `top` / `bottom` |
| A9 | **MAUI の演出の添付は code-behind からに限られる** (クロージャは XAML に書けない)。MAUI には宣言的 UI 系の添付経路がなく、MAUI の View が唯一の中身の形 | `transition-semantics.md` / 演出は中身に添付する (第3の添付スロット) / 38 行 (添付の面の表 34-39 行の MAUI 行) | `Dialog.SetTransition` |
| A10 | **KMP の共有コードには演出の添付の面がない** (共有コードは演出を運ばない)。KMP から登録・表示した中身も各 OS では通常の View として組み立てられるため、その中身に対して各 OS の面で添付すれば同じように効く | `transition-semantics.md` / 演出は中身に添付する (第3の添付スロット) / 39 行 (KMP 行) | `UIView` |
| A11 | **KMP の共有コードから渡せる属性は placement だけ** (器の静的メタ属性は OS 共通コードの境界を渡らない)。commonMain には style / options の API も公開しない | `layout-semantics.md` / 属性の渡し方と優先順位 / 83 行、`loading-semantics.md` / 既定ローディング (内蔵コンテンツ) と styling / 66 行 | `DialogPlacement` / `DialogOptions` |
| A12 | **レイアウト計算の実体を持つのは iOS / Android の Native 2 実装だけ**で、MAUI / KMP は属性を無変換で渡すパススルーである (再実装しない) | `layout-semantics.md` / 冒頭の読み方 / 18 行、してはいけないこと / 190 行、用語 / 225 行 | なし |
| A13 | **同じ入力なら iOS と Android で同じ rect になる** (承認・記録された差異を除く)。差異の記録の仕組みは検証機構側 (`core/architecture/layout-case-table.md`) が持つ | `layout-semantics.md` / 保証すること / 182 行 | `approvedDiff` |
| A14 | **基準領域 visibleArea の中身が OS で違う** (iOS = safe area / Android = システムバーを除外した領域)。属性値の論理単位も iOS = pt / Android = dp | `layout-semantics.md` / 基準領域 (LayoutArea) / 111 行、器が持つメタ属性 / 37 行、用語 / 235・237 行 | なし |
| A15 | **overlayColor に透明を指定してもシステムバーの見えは変わらない** — Android の現行実装ではダイアログ表示中のシステムバーの見えが変化する | `layout-semantics.md` / 保証すること / 186 行 | なし |
| A16 | **offset の符号は配置によらず一定**で、end 配置でも +X は右へ動く (原典 Android の符号反転は仕様バグとして継承しない) | `layout-semantics.md` / 座標系と方向 / 147 行 | なし |
| A17 | **画面の回転で凍結済み実効値のまま再配置する** — 原典 iOS の「提示後はサイズを固定する」挙動は継承しない | `layout-semantics.md` / 表示中に画面のほうが変わったとき / 100 行 | なし (Scenario ID は識別子として拾われない) |
| A18 | **Toast の器は表示 1 枚ごとに専用のオーバーレイを持ち、その実現方式が OS で違う** (iOS = key window 直貼りの View / Android = 全画面透過 Window)。表示はページ遷移をまたいで継続する | `toast-semantics.md` / 多重表示と表示の継続 / 82 行 | なし |
| A19 | **Toast の duration に上限クランプは無い** — 原典の Android 実装は OS の Toast API に載っていて実質 3.5 秒に切り詰められていたが、自前の器ではこの制限を継承しない | `toast-semantics.md` / duration の時間モデル / 56 行 | なし |
| A20 | **Toast の重なり順は起動順** (後に受理されたものが手前)。原典の Android は OS の順番待ちだった | `toast-semantics.md` / 多重表示と表示の継続 / 79 行 | なし |
| A21 | **Loading は常にウィンドウ全体を覆う** — 原典で iOS のみ有効だった表示スコープ (現在のページだけを覆う指定) は持たない | `loading-semantics.md` / 持たない機能 / 78 行 | なし |
| A22 | **Toast / Loading とダイアログの重なり順は原典で platform 間が揃っておらず**、ここでのルール化は各機能の実装時に行う (未確定として core に残っている記述) | `multi-display-semantics.md` / 保証しないこと / 47 行 | なし |

## B. 公開面の差 (core から platform concept へ移す)

| # | 差分 | 出典 (concept / 節 / 行) |
|---|---|---|
| B1 | 既定の表示エントリの綴りが 4 形態で違う (Swift / Kotlin / C# / KMP 共有コード) | `registration-show-semantics.md` / 基本形: 登録してから show する / 26 行 |
| B2 | 真偽値の結果の省略形が形態ごとに違う (Swift の associatedtype 既定値 / Kotlin の別名 / MAUI の型引数なし interface / KMP 共有コードは省略不可 / KMP の Swift 向け公開面は result ラベル省略) | `registration-show-semantics.md` / 真偽値の結果は結果型を書かずに済む / 38-44 行 (形態表) |
| B3 | 登録・表示の呼び分けが形態ごとに違う (技術別オーバーロード / Android だけ Compose 用の別名。理由は `@Composable` のオーバーロード解決) | `registration-show-semantics.md` / 形態ごとの呼び分け / 72-81 行 |
| B4 | Compose 系を別モジュールに分けている (Android の配布形) | `registration-show-semantics.md` / 形態ごとの呼び分け / 81 行 |
| B5 | 属性の添付の面が形態・技術系ごとに違う (extension プロパティ / SwiftUI modifier / Compose の宣言 / MAUI 添付プロパティ / KMP はなし) | `layout-semantics.md` / 属性の渡し方と優先順位 / 68-75 行 (形態表) |
| B6 | 演出の添付の面が形態・技術系ごとに違う | `transition-semantics.md` / 演出は中身に添付する (第3の添付スロット) / 34-39 行 (形態表) |
| B7 | 演出フックの型と渡ってくる View の型が形態ごとに違う | `transition-semantics.md` / フックの形は全形態で1つ (統一形) / 49-53 行 (形態表) |
| B8 | duration / easing のネイティブ表現が形態ごとに違う。成立しない duration の判定に MAUI 固有の上限がある | `transition-semantics.md` / プリセット / 104・106 行 |
| B9 | プリセット factory を iOS では MainActor 上で呼ぶ | `transition-semantics.md` / プリセット / 108 行 |
| B10 | show の戻りの形が形態ごとに違う (async throws + enum / suspend + sealed class / Task + 型付き結果。構成ミスの伝わり方も含む) | `result-notification-semantics.md` / 各形態での形 / 87-93 行 (形態表) |
| B11 | 構成ミスの例外の名前と粒度が形態ごとに違う (Swift の enum case / Kotlin・C# の詳細例外型)。value class と値型で例外名が非対称 | `model-binding-semantics.md` / notifier の VM 供給 / 30 行、ViewModel 契約は参照型 (class) 限定 / 34-42 行、VM factory による解決 / 59 行 |
| B12 | ViewModel の参照型限定の効き方が形態ごとに違う (Swift はコンパイルエラー / Kotlin は実行時検査 / MAUI は class 制約 + 実行時検査) | `model-binding-semantics.md` / ViewModel 契約は参照型 (class) 限定 / 36-40 行 (形態表) |
| B13 | notifier の取得の綴りが形態ごとに違う (小文字 / PascalCase / KMP iOS 面の専用アクセサ) | `model-binding-semantics.md` / KMP での見え方 / 79-81 行、用語 / 132 行 |
| B14 | Loading / Toast の契約 interface と既定シングルトンの綴りが形態ごとに違う | `loading-semantics.md` / 公開面 / 24 行、`toast-semantics.md` / 公開面 / 29-34 行 (形態表) |
| B15 | 進捗報告口の形が形態別のイディオムで違う | `loading-semantics.md` / 公開面 / 33 行 |
| B16 | KMP の契約が VM 経路にだけ `@Throws` を宣言する (Swift から直接呼ぶときの失敗の届き方) | `loading-semantics.md` / 合流のルール (多重利用) / 44 行、`result-notification-semantics.md` / ルール5 / 47 行 |
| B17 | iOS の Toast カスタム View factory 閉包が throws である理由 | `toast-semantics.md` / 公開面 / 46・49 行 |
| B18 | 属性の綴りが形態ごとの言語慣習に従う (MAUI は PascalCase)。overlayColor のネイティブ色型も形態ごとに違う | `layout-semantics.md` / DialogPlacement (動的メタ) / 58 行、DialogOptions (静的メタ) / 46 行 |
| B19 | 移植元 (原典) の API 名との対応 (表示 API の動詞分裂・基準領域の bool プロパティ) | `result-notification-semantics.md` / 各形態での形 / 96 行、`layout-semantics.md` / 基準領域 (LayoutArea) / 113 行 |
| B20 | 内部の Loading coordinator が iOS / Android にそれぞれ 1 つあること (どの入口から使っても同じ表示に合流する説明) | `loading-semantics.md` / 合流のルール (多重利用) / 39 行 |

## 確認結果 (再構成後、task 4.1b)

A 群 22 項目を分割後の core で 1 項目ずつ探し、(1) core に残っているか (2) 識別子を含まない形か
(3) 具体形を持つ platform concept へのリンクがあるかを確認した。**22 項目すべてが条件を満たす**。
行番号は再構成後の `kasane/concepts/core/api/*.md`。B 群 20 項目は core から外れており、
着地は着地台帳 (`identifier-ledger.md`) 側で検査している。

| # | 分割後の core の箇所 | 識別子 | リンク先 (具体形) |
|---|---|---|---|
| A1 | `multi-display-semantics.md:41-44` (保証しないこと / 閉じる順序と、下を先に閉じたときの見え方) の OS 表 | なし | 同節 `:46` → ios / android / maui / kmp の `dialog-surface.md`「失敗とキャンセルの形」 |
| A2 | `multi-display-semantics.md:50` (同節) | なし | 同行 → 「トランジションのルール」(core)、同節 `:46` → 4 形態の `dialog-surface.md` |
| A3 | `result-notification-semantics.md:84-88` (呼び出し元をキャンセルしたときの見え方) と `transition-semantics.md:158-162` (呼び出し元のキャンセルはどう観察されるか) の 2 表 | なし (`.cancelled` / `CancellationException` を外し「言語のキャンセル規約どおりキャンセルの通知が伝播する」と散文化) | `result-notification-semantics.md:90` / `transition-semantics.md:164` → 4 形態の `dialog-surface.md`「失敗とキャンセルの形」 |
| A4 | 同 2 表の MAUI 行 (`result-notification-semantics.md:88` / `transition-semantics.md:162`) | なし | 同上 (`maui/api/dialog-surface.md`) |
| A5 | `transition-semantics.md:166`、`result-notification-semantics.md:90` | なし | 同上 (Scenario ID `PB-AA-03` は識別子として拾われない) |
| A6 | `result-notification-semantics.md:70` (キャンセルになる操作の箇条書き) と `:74` (iOS には設けない) | なし | `:111-118`「形態別の公開面」→ 4 形態の `dialog-surface.md` |
| A7 | `result-notification-semantics.md:78` (ソフトキーボード表示中の戻るボタン)、`multi-display-semantics.md:58` | なし | `multi-display-semantics.md:58` → 「結果通知のルール」(core)、`:64-71`「形態別の公開面」→ 4 形態の `dialog-surface.md` |
| A8 | `transition-semantics.md:90` (プリセット) | なし (`leading` / `trailing` / `START` / `END` / `top` / `bottom` を「行の始まり側 / 終わり側」「上 / 下」へ散文化) | 同行 → ios / android / maui の `transition-surface.md`「プリセット factory」 |
| A9 | `transition-semantics.md:38` (演出は中身に添付する) | なし (`Dialog.SetTransition` を外した) | 同行 → `maui/api/transition-surface.md`「code-behind での添付」 |
| A10 | `transition-semantics.md:39` (同節) | なし (`UIView` を外した) | 同行 → `kmp/api/dialog-surface.md`「共有コードには添付の面が無い」 |
| A11 | `layout-semantics.md:78` (属性の渡し方と優先順位) と `:259` (形態別の公開面)、`loading-semantics.md:72` (styling) | `DialogPlacement` のみ (共通概念名。`DialogOptions` は散文の「静的メタ属性」へ) | `layout-semantics.md:78` / `:259` → `kmp/api/dialog-surface.md`、`loading-semantics.md:72` → `kmp/api/loading-surface.md` |
| A12 | `layout-semantics.md:22` (冒頭の読み方) | なし | 同行 → core/ADR-0001。具体形は `maui` / `kmp` の各 surface (`:252-259`) |
| A13 | `layout-semantics.md:202` (保証すること) | なし (`approvedDiff` は architecture へ) | 同行 → `core/architecture/layout-case-table.md` |
| A14 | `layout-semantics.md:110` (基準領域) と `:249` (用語)、`:41` (論理単位) | なし | `:252-259`「形態別の公開面」→ ios / android の `layout-surface.md` (色型・論理単位の綴り) |
| A15 | `layout-semantics.md:206` (保証すること) | なし | 同上 (`overlayColor` は静的メタ属性のフィールド名として散文中に平文で書く) |
| A16 | `layout-semantics.md:167` (座標系と方向) | なし | 同上 |
| A17 | `layout-semantics.md:95` (表示中に画面のほうが変わったとき) | なし (Scenario ID `PB-WN-01` は識別子として拾われない) | 同上 |
| A18 | `toast-semantics.md:76` (多重表示と表示の継続) | なし | `:134-141`「形態別の公開面」→ 4 形態の `toast-surface.md` |
| A19 | `toast-semantics.md:118` (持たない機能) | なし | 同上 |
| A20 | `toast-semantics.md:73` (多重表示と表示の継続) | なし | 同上 |
| A21 | `loading-semantics.md:84` (持たない機能) | なし | `:100-107`「形態別の公開面」→ 4 形態の `loading-surface.md` |
| A22 | `multi-display-semantics.md:62` (Toast / Loading とダイアログの前後関係) | なし | `:64-71`「形態別の公開面」。前後関係の確定は 「Toast のルール」`:81` (core) が持つ |

補足: A2・A5〜A7・A12・A14〜A22 は baseline 時点でも識別子を含まない挙動の記述であり、
その形態固有の具体形 (名前・署名) が存在しない。表の「リンク先」にはその項目を含む節から
届く公開面リンクを書いた (どの形態の綴りを引くかの導線が節内で閉じていることの確認)。
