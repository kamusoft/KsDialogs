---
type: concept
title: 登録と表示の呼び出し面のルール (中身の書き方と show の呼び方)
description: ダイアログの中身を用意して表示するまでの core 契約 — 真偽値の結果なら結果型を書かずに済む省略形・中身を従来 View 系と宣言的 UI 系 (SwiftUI / Compose) のどちらで書いても挙動が同じであること・宣言的 UI での属性の添付・登録せずにその場で表示するインライン show
tags: [dialog, api, registration, contract]
timestamp: 2026-09-06
---

# 登録と表示の呼び出し面のルール (中身の書き方と show の呼び方)

この文書は、全形態 (iOS Native / Android Native / MAUI / KMP) 共通の「ダイアログの中身をどう用意して、どう表示するか」のルールを定める。読むと、結果が真偽値だけのときに結果型の記述を省ける条件、中身を従来の View で書くか宣言的 UI (SwiftUI / Jetpack Compose) で書くかで何が変わって何が変わらないか、宣言的 UI での属性の添付の考え方、登録せずにその場で表示する方法が分かる。

- 先に [結果通知のルール](result-notification-semantics.md) を読むと分かりやすい (show が何を返すか・結果型は ViewModel が宣言するという原則は同文書が定めている)
- ダイアログの大きさと位置を決める規則そのものは [レイアウトのルール](layout-semantics.md) の担当で、この文書では扱わない。扱うのは、その属性を**呼び出し面のどこにどう書くか**だけである
- 本文中の**移植元**は AiForms.Maui.Dialogs (本ライブラリの移植元) を指す。参照ルールは [移植元 AiForms.Maui.Dialogs の参照](../../../handbook/cross/aiforms-origin-reference.md)
- core は「全形態が共有する契約」の層 (層の区分は [concepts 配置ルール](../../rules.md))。公開名・署名・コード例は各形態の公開面が持つ (末尾の「形態別の公開面」)

**この文書が正であり、実装はここに合わせる**。根拠決定は [core/ADR-0010](../../../decisions/core/0010-dual-content-view-technology.md)・[core/ADR-0011](../../../decisions/core/0011-dual-content-registration-overloads.md)・[core/ADR-0012](../../../decisions/core/0012-default-result-type-bool.md)・[core/ADR-0013](../../../decisions/core/0013-inline-factory-show.md)・[kmp/ADR-0003](../../../decisions/kmp/0003-swift-facing-registration-in-swift-package.md)・[kmp/ADR-0004](../../../decisions/kmp/0004-swift-facing-typed-generic-facade.md)。これらは最初の実装での検証を経て確定する運用のためまだ proposed だが、確定の前後を通じて「何が成り立つか」の正はこの文書側にある (ADR が持つのは「なぜそう決めたか」)。

## 基本形: 登録してから show する

利用者は「ViewModel の型 → 中身を作る関数 (**factory**)」の紐付けを **register** で登録しておき、**show** に ViewModel のインスタンスを渡して表示し、結果を待つ。factory は show のたびに呼ばれ、ViewModel と `DialogNotifier` を受け取って中身を返す ([core/ADR-0004](../../../decisions/core/0004-di-view-factory-registry.md)・[core/ADR-0005](../../../decisions/core/0005-no-view-reuse-mechanism.md))。notifier 引数を省いた1引数形の factory と、ViewModel の型だけを渡す表示 (型指定 show) は [ViewModel 主導の呼び出しのルール](model-binding-semantics.md) が定める。

呼び出し先は2つに分かれる。**登録は `DialogViewRegistry` のメソッド**で、全ての入口が同じレジストリを共有する。**表示は表示エントリのメソッド**である。表示エントリとは、表示契約 `KsDialog` を実装したオブジェクトのことで、形態ごとに既定のシングルトンが用意されている。それを直接使ってもよいし、同じ契約を DI で注入したインスタンスを使ってもよい — どちらも同じレジストリを共有する ([core/ADR-0002](../../../decisions/core/0002-public-api-shape.md))。既定エントリの綴りは形態ごとに違うので、各形態の公開面を参照する。

show は ViewModel のほかに**置き場所 (`DialogPlacement`) を引数で受け取れる**。渡した値は中身に添付されている置き場所をオブジェクトまるごと置き換える (フィールド単位では混ぜない)。属性の供給経路と優先順位は [レイアウトのルール](layout-semantics.md) が定める。

この文書が定めるのは、この register / show の呼び出し面に関する4つのルール — **結果型の省略形**・**中身を書く技術の選択**・**宣言的 UI での属性の添付**・**登録しないインライン show** — である。

## 真偽値の結果は結果型を書かずに済む

ダイアログの結果は「OK か否か」の真偽値であることが大半で、カスタムの結果型 (入力された文字列・選んだ項目など) が要るのは一部にとどまる。にもかかわらず全 ViewModel に結果型の宣言を課すと、多数派に少数派向けの記述を強要することになる。そこで **ViewModel が結果型を宣言しなければ真偽値**とする ([core/ADR-0012](../../../decisions/core/0012-default-result-type-bool.md))。

言語ごとに「省略できる場所」が違うため、**省略形の書き方は形態ごとに異なる**。契約の型引数に既定値を持てる言語は宣言そのものを省き、持てない言語は真偽値専用の別名か、型引数を取らない専用の公開型を用意する。KMP の共有コードだけは省略できず、結果型を常に明示する — 共有コードの契約は各 OS の実体へ解決される宣言 (expect 宣言) であり、その型引数に既定値を置けないためである。具体的な綴りは各形態の公開面を参照する。

show が返すのは `DialogResult` で、その完了状態 (completed) が結果値を運ぶ ([結果通知のルール](result-notification-semantics.md))。省略形で宣言した ViewModel なら戻り値も factory が受け取る `DialogNotifier` も真偽値に型付く。

省略形は**追加された書き方**であり、ViewModel 型と結果型の2つを明示する従来の宣言・登録の形はそのまま併存する。カスタム結果型を使う経路はどの形態でも変わらない。

## 中身は従来 View 系でも宣言的 UI 系でも書ける

ダイアログの中身は2系統の技術で書ける ([core/ADR-0010](../../../decisions/core/0010-dual-content-view-technology.md)) — 各 OS の**従来 View 系**と、**宣言的 UI 系** (SwiftUI / Jetpack Compose) である。どちらか一方に寄せることはしない。宣言的 UI で書けないとネイティブの消費者にとって不自然になり、従来 View 系がないと MAUI との連携が成り立たないためである。

**どちらで書いても、show の外から観察できる挙動は同じ**になる。同じとは、次のすべてを指す:

- 結果の返り方とキャンセルの返り方 ([結果通知のルール](result-notification-semantics.md))
- **全閉鎖経路**での中身の破棄 (下記)
- レイアウト規則の適用 — 宣言的 UI で書いた中身も [レイアウトのルール](layout-semantics.md) と、その期待値を固定する[共通ケース表](../architecture/layout-case-table.md) に適合する。中身が要求するサイズが、そのままレイアウト規則の入力となるサイズ (同文書のいう内容サイズ) として扱われる
- 属性を添付して供給したときの扱い — 優先順位 (show 引数 > 中身への添付 > 契約の既定値) と、初回レイアウトパス完了時点の値だけを採用する規則 (いずれも [レイアウトのルール](layout-semantics.md) が定める)

ここでいう**全閉鎖経路**とは、ダイアログが閉じる次の4経路すべてを指す: **完了** (中身が結果つきで報告した) / **キャンセル** (中身からのキャンセル報告・外側タップ・Android の戻る操作) / **呼び出し元キャンセル** (show を待っている呼び出し側が待機を打ち切った) / **画面破棄** (ダイアログを載せていた画面そのものが壊れた)。

技術ごとに提示・結果・レイアウトの経路を分けると、経路が技術数の分だけ倍になり、共通の検証も割れる。公開面だけを技術別に分け、内部では1つの表現に集約する ([core/ADR-0011](../../../decisions/core/0011-dual-content-registration-overloads.md))。

### 形態ごとの呼び分け

登録・表示の呼び分けの形は形態によって違う。**同名のオーバーロードで並べられる形態と、宣言的 UI 系だけ別名にする形態がある** — 言語のオーバーロード解決が中身の技術を区別できるかどうかで分かれるためである。Android は宣言的 UI 系を別名にし、配布も本体と別のモジュールに分けている (従来 View 系しか使わない消費者に宣言的 UI の依存を持ち込まないため)。MAUI には宣言的 UI 系の経路がなく、MAUI の View が唯一の中身の形である。

呼び分けの具体的な名前・オーバーロードの並びと、別名にした理由の詳細は各形態の公開面が持つ。

## 宣言的 UI での属性の添付

中身の定義に**添付**して供給できるものは3スロットある — **器の静的な属性** (大きさ・基準領域・背後の覆い・外側タップの扱い)・**置き場所**・**出入りの演出**である。宣言的 UI では添付の書き方だけが変わり、供給の優先順位と採用時点の規則は従来 View 系と同一である ([レイアウトのルール](layout-semantics.md))。添付の面の綴り (modifier・宣言関数・添付プロパティ) は形態と技術系ごとに違う。

出入りの演出は静的な属性・置き場所と並ぶ**第3の添付スロット**で、添付の書き方と採用時点はここに書いたとおり同じだが、その意味 (フックの形・既定の演出・結果が返る時点) は [トランジションのルール](transition-semantics.md) が定める。

添付は**初回表示までに評価される場所に書く**。宣言的 UI には遅延評価されるスコープがあり、その中に書くと初回の組み立てで実行されず、その表示には効かない。中身の状態変化で添付値を書き換えても、表示中のダイアログには追随しない。

SwiftUI で同じ属性を入れ子の内側と外側の両方に添付した場合は、**外側 (より上位の View) の値が勝つ**。

## 登録せずにその場で表示する (インライン show)

1回しか使わないダイアログのために登録の手間を課さない経路として、**factory を show に直接渡す**形を用意する ([core/ADR-0013](../../../decisions/core/0013-inline-factory-show.md))。factory の形は登録経路とまったく同じで、型付きの `DialogNotifier`・技術別の呼び分け・結果型の省略形をそのまま使える。

インライン show は**レジストリの状態を一切変えない** — 一時的にも登録せず、当然その解除も起こらない。ここから次が従う:

- 同じ ViewModel 型が登録済みでも、インライン show はその登録を使わず、渡された factory を使う。**登録の内容はインライン show の前後で変わらない**
- インライン show で表示したからといって、その ViewModel 型が登録済みになるわけではない。あとからレジストリ経由で show すれば未登録の失敗になる (失敗の扱いは [結果通知のルール](result-notification-semantics.md) の構成ミス)
- 同じ ViewModel 型のインライン show を並行して表示しても、factory・`DialogNotifier`・結果はそれぞれ独立する。片方を閉じても他方は表示されたまま未確定である
- show の placement 引数の意味は登録経由と同じ (添付された置き場所をオブジェクトまるごと置換する)

「一時的に登録して show して解除する」実装にすると、同じ型の並行表示でキーが衝突し、解除のタイミングが既存の登録を壊す。レジストリを触らないのはこの構造的な問題を避けるためである。

対象は iOS Native / Android Native / MAUI。KMP の共有コードは View を供給できないため対象外で、KMP でのインライン表示は各 OS 側の入口を使う。

## 保証すること

- **結果型を宣言しない ViewModel の結果は真偽値になる**。省略形で登録した ViewModel の show は真偽値の `DialogResult` を返し、factory が受け取る `DialogNotifier` も真偽値に型付く。2型引数を明示する従来の形と併存するため、真偽値の書き方が増えてもカスタム結果型の書き方は失われない
- **中身をどちらの技術で書いても、show の外から観察できる挙動は同じになる** (結果・キャンセル・破棄・レイアウト規則・添付した属性の扱い)。ここが割れると、技術の選択が利用者にとって挙動の選択になってしまう
- **宣言的 UI の中身も、全閉鎖経路で解放される**。宣言的 UI の中身を器へ載せるために内部で挟むホストは、完了・キャンセル・呼び出し元キャンセル・画面破棄のどの経路でも破棄される。画面から消えたのに生き残る状態を作らない
- **インライン show はレジストリを変えない**。既存の登録と共存し、同じ型の並行インライン表示どうしも独立する
- **この文書が定める書き方はすべて追加であり、従来の書き方を置き換えない**。従来の factory の形も、ViewModel 型と結果型の2型引数を明示する登録も、そのまま使い続けられる

## してはいけないこと

- **提示・結果・レイアウトの経路を中身の技術ごとに分けない**。公開面のオーバーロードだけを分け、内部では1つの表現に集約する
- **インライン show を「一時登録 → show → 登録解除」で実装しない**。キー衝突と解除タイミングの問題を持ち込むだけになる
- **出来上がった View のインスタンスを直接渡す show を作らない** (下記)
- **View 側で `DialogNotifier` を自前生成しない**。自前で作った通知役はどの show とも結線されておらず、報告しても結果は誰にも届かない
- **宣言的 UI の添付を、初回表示までに評価されない場所に書かない** (遅延評価されるスコープの中など)

`KsDialog` の factory は ViewModel と `DialogNotifier` を受け取る形であり、外で作られた View には show の結果と結線された `DialogNotifier` を型安全に渡す場所がない。移植元には出来上がった View を渡す形があるが、それは中身の View 自身に通知役を作らせて ViewModel へ配る別の仕組みの上に成立しており、その仕組みごと持ち込む選択はしていない。

## まだ決めていないこと (実物と一緒に決める)

- **Compose で同じ属性を二重に宣言したときの勝敗** — SwiftUI の入れ子の重畳 (外側が勝つ) にあたる規則を Compose 側では規定していない。二重宣言を避けて書く前提で、必要になったら決める

かつてここにあった2項目 — 「登録の手間をさらに縮める形」(1行登録・DI からの解決) と「結果報告口を ViewModel へ渡す形」— は決着し、[ViewModel 主導の呼び出しのルール](model-binding-semantics.md) が定める。show 中の ViewModel から通知役を取得できるため、factory は notifier 引数を省いた1引数形でも登録できる (2引数形は低水準 API として併存)。MAUI の1行登録と DI からの解決は [MAUI の DI 連携と登録糖衣](../../maui/api/di-registration.md) が定める。

## 用語

| 用語 | 意味 |
|---|---|
| 形態 | 本ライブラリを利用できる4つの入口 (iOS Native / Android Native / MAUI / KMP) |
| 器 | ダイアログを画面に載せる表示コンテナ。中身を内側に持つ ([結果通知のルール](result-notification-semantics.md)) |
| 中身 | factory が返す、ダイアログの器の内側に載る表示物 |
| factory | ViewModel と `DialogNotifier` を受け取って中身を作る関数。show のたびに呼ばれ、中身を毎回新規に作る |
| `DialogNotifier` | 中身の側から結果を報告する部品 (「完了 (結果つき)」「キャンセル」の2操作)。show 1回ごとに新しいものが factory へ渡る ([結果通知のルール](result-notification-semantics.md)) |
| 従来 View 系 | 各 OS の従来からの View 型 (MAUI は MAUI の View) で中身を書く系統 |
| 宣言的 UI 系 | SwiftUI / Jetpack Compose で中身を書く系統 |
| 省略形 | 結果型を書かずに真偽値の ViewModel を宣言する形。書き方は形態ごとに違う |
| 全閉鎖経路 | ダイアログが閉じる4経路 (完了 / キャンセル / 呼び出し元キャンセル / 画面破棄) のすべて |
| インライン show | 登録せずに factory を show へ直接渡す表示 |
| 添付 | ダイアログの中身の定義に器の属性を結びつける供給経路 ([レイアウトのルール](layout-semantics.md)) |
| 表示契約 | 各機能の表示メソッドを定めた interface / protocol。型名は `Ks` + 機能名の単数形 (`KsDialog` / `KsLoading` / `KsToast`、MAUI は `I` 接頭辞)。複数形の `KsDialogs` は製品名 (モジュール・パッケージ・NuGet ID) であって型名ではない ([core/ADR-0034](../../../decisions/core/0034-contract-type-name-singular-feature.md)) |
| 表示エントリ | 表示契約 `KsDialog` を実装したオブジェクト。形態ごとに既定のシングルトンがあり、DI で注入したインスタンスでもよい |
| 移植元 | 本ライブラリの移植元である AiForms.Maui.Dialogs |

## 形態別の公開面

既定エントリの綴り・省略形の書き方・技術別の呼び分け・添付の面・インライン show の書き方とコード例は、各形態の公開面が持つ:

- [iOS の Dialog 公開面](../../ios/api/dialog-surface.md)
- [Android の Dialog 公開面](../../android/api/dialog-surface.md)
- [MAUI の Dialog 公開面](../../maui/api/dialog-surface.md)
- [KMP の Dialog 公開面](../../kmp/api/dialog-surface.md)

## 関連

- [結果通知のルール](result-notification-semantics.md) — show が何を返すか。結果型を ViewModel が宣言するという原則はこちらが定める
- [ViewModel 主導の呼び出しのルール](model-binding-semantics.md) — 1引数 factory・結果報告口の VM 供給・型指定 show・ViewModel 契約の参照型限定
- [レイアウトのルール](layout-semantics.md) — 属性の意味と優先順位。宣言的 UI の中身もこの規則の対象 (規則から導いた期待値を固定する表は [レイアウト共通ケース表と OS 差の統制](../architecture/layout-case-table.md))
- [多段表示のルール](multi-display-semantics.md) — 重なったときの保証
- [トランジションのルール](transition-semantics.md) — 第3の添付スロット (出入りの演出) の意味と、結果が返る時点
- [core/ADR-0010](../../../decisions/core/0010-dual-content-view-technology.md) — 決定 (従来 View 系と宣言的 UI 系の両対応を必須とする)
- [core/ADR-0011](../../../decisions/core/0011-dual-content-registration-overloads.md) — 決定 (公開面は技術別オーバーロード、内部は単一表現に集約)
- [core/ADR-0012](../../../decisions/core/0012-default-result-type-bool.md) — 決定 (結果型の既定を真偽値とする)
- [core/ADR-0013](../../../decisions/core/0013-inline-factory-show.md) — 決定 (登録不要の表示はインライン factory show)

KMP 利用者向けの登録・表示を Swift パッケージ側の型付き公開面に置く決定は [kmp/ADR-0003](../../../decisions/kmp/0003-swift-facing-registration-in-swift-package.md)・[kmp/ADR-0004](../../../decisions/kmp/0004-swift-facing-typed-generic-facade.md) が持つ。
