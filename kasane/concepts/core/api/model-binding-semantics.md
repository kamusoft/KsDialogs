---
type: concept
title: ViewModel 主導の呼び出しのルール (notifier の VM 供給と型指定 show)
description: View を知らないコードから ViewModel を主役にダイアログを呼ぶための core 契約 — show 中の ViewModel から結果報告口 (notifier) を取得できる仕組み・ViewModel の型だけを渡して表示する型指定 show (Dialog / Loading / Toast の 3 機能で同型)・VM factory の登録と解決・ViewModel 契約が参照型 (class) 限定である理由と形態別の強制手段・「非破壊の追加」が指す範囲・KMP の共有コードでの見え方 (VM factory は共有コードで登録し、生成と configure は呼び出し元の文脈で行う)
tags: [dialog, loading, toast, viewmodel, notifier, di, contract]
timestamp: 2026-09-07
---

# ViewModel 主導の呼び出しのルール (notifier の VM 供給と型指定 show)

この文書は、全形態 (iOS Native / Android Native / MAUI / KMP) 共通の「ViewModel を主役にダイアログを呼ぶ」ためのルールを定める。読むと、ダイアログの中身や ViewModel 自身が結果報告口 (`DialogNotifier`) をどう手に入れるか、ViewModel の型だけを渡して表示する呼び方 (型指定 show — Dialog だけでなく Loading / Toast にも同型で提供される) がどう動くか、そのために ViewModel 契約が参照型 (class) に限定されている理由が分かる。

- 先に [結果通知のルール](result-notification-semantics.md) (show が何を返すか・結果報告口とは何か) と [登録と表示の呼び出し面のルール](registration-show-semantics.md) (register / show の基本形) を読むと分かりやすい
- core は「全形態が共有する契約」の層 (層の区分は [concepts 配置ルール](../../rules.md))。公開名・署名・コード例は各形態の公開面が持つ (末尾の「形態別の公開面」)

**この文書が正であり、実装はここに合わせる**。根拠決定は [core/ADR-0018](../../../decisions/core/0018-notifier-vm-injection-side-table.md) (結果報告口の VM 供給と参照型限定)・[core/ADR-0019](../../../decisions/core/0019-no-lifecycle-hooks-configure-closure.md) (ライフサイクルフック非採用と configure)・[core/ADR-0020](../../../decisions/core/0020-show-verb-unification.md) (動詞 show への統一)・[core/ADR-0021](../../../decisions/core/0021-vm-factory-registry-resolution.md) (VM factory による解決)・[core/ADR-0035](../../../decisions/core/0035-loading-toast-typed-show-vm-factory.md) (型指定 show の Loading / Toast への拡張)・[kmp/ADR-0006](../../../decisions/kmp/0006-common-vm-factory-typed-show.md) (KMP の共有コードでの型指定 show と VM factory の置き場)。MAUI 固有の DI 糖衣は [MAUI の DI 連携と登録糖衣](../../maui/api/di-registration.md) が定める。

## なぜこの呼び出し面があるか

共有層 (KMP の共有コードや、MAUI アプリの UI を知らない層) から「ViewModel を投げたらダイアログが出て、型付きの結果が返る」を成立させるのが目的である。従来の factory は「ViewModel と結果報告口を受け取って中身 (ダイアログに載る View) を返す」2引数形で、報告口は factory を書いた人しか受け取れなかった。この文書のルールにより、factory を書かない登録 (MAUI の1行登録など) や、ViewModel 自身が結果を報告する設計が成立する。

## 結果報告口を ViewModel から取得できるようにする (VM 供給)

show は**中身の生成より前に**結果報告口を ViewModel に紐付け、show 中は ViewModel から報告口を取得できる。ここでいう**空**とは「取得できない状態」で、綴りは形態ごとに違う (nil / null に相当する)。取得は各形態の慣用形 (拡張プロパティに相当する形) で書け、いずれも**宣言結果型** (ViewModel が契約の型引数で宣言する結果の型。省略時は真偽値 — [登録と表示の呼び出し面のルール](registration-show-semantics.md)) に型付いた `DialogNotifier` (または空) を返す。ViewModel の定義には何も足さない (契約はメンバーを持たないマーカーのまま)。

取得できる期間と同一性の規則は次の4点である。

### show 中だけ取得できる

show の前と、結果または例外が呼び出し元へ渡った後は空になる。除去は正常な結果配送だけでなく、**紐付け後に show が終わる全経路** (View factory の例外・提示の失敗・呼び出し元キャンセル・OS 発の器消失) で必ず行われる — 除去漏れがあると同じ ViewModel の次回 show が並行 show と誤判定されてしまうためである。

### 紐付けと取得はインスタンス同一性で行う

等価比較 (equals 相当) が一致する別インスタンスの ViewModel は互いに干渉しない。

### 登録形・呼び出し経路によらず常に紐付く

呼び出し経路とは、ViewModel のインスタンスを渡す従来の show (**インスタンス渡し show**)・登録せず factory を直接渡す**インライン show** ([登録と表示の呼び出し面のルール](registration-show-semantics.md))・後述の**型指定 show** の3つを指す。2引数 factory で登録した中身でも、VM から引いた報告口は factory 引数の報告口と同じ配送先を指し、インライン show でも同じに働く。

### 同一 ViewModel インスタンスの並行 show は構成ミスとして失敗する

紐付け先が1インスタンス1本のため2本目を張れない (cancelled 等の結果に化けない)。表示中の先行 show には影響しない。別インスタンスなら従来どおり独立に重ねられる ([結果通知のルール](result-notification-semantics.md) のルール6)。失敗の型・名前は形態ごとに違う。

## ViewModel 契約は参照型 (class) 限定

紐付けがインスタンス同一性を要求するため、コピーで同一性が失われる値型は ViewModel にできない。値型を許すと、値コピーや boxing のたびに別インスタンスになって報告口が常に空を返し、**結果を報告できないダイアログが黙って残る**。

強制手段は形態別で、**どの show 経路 (インスタンス渡し / インライン factory / 型指定) でも値型の ViewModel は提示に至らない**。参照型制約をコンパイル時に書ける形態はコンパイルエラーで、書けない形態は全 show 経路が通る共通の提示入口の実行時検査で拒否する。形態によっては両方を併用する。拒否の綴りと、どの経路がどちらで止まるかは各形態の公開面が持つ。

## 型指定 show (型を渡して表示する)

ViewModel の**インスタンスではなく型**を渡して表示する経路。ライブラリが ViewModel を生成し、configure クロージャ (省略可・非同期可) を適用してから表示する。動詞は他の経路と同じ show 1本で、経路の違いは引数の形だけで表す ([core/ADR-0020](../../../decisions/core/0020-show-verb-unification.md))。型指定 show でも置き場所を引数で渡せる。

この経路は 4 形態すべてにある。KMP の共有コード (commonMain) では VM factory の登録先と実行文脈が Native と違う (下記「KMP での見え方」)。各形態での書き方は公開面が持つ。

### Dialog / Loading / Toast で同型

型指定 show は Dialog だけの経路ではなく、**Loading と Toast にも同型で提供される** ([core/ADR-0035](../../../decisions/core/0035-loading-toast-typed-show-vm-factory.md))。3 機能で共通なのは、VM factory による解決 (次節)・スナップショット解決・「VM factory で生成 → configure 完了 → View factory → 提示」の順序保証・未登録は構成ミスとして失敗すること。Loading にはスコープ形 (処理ブロックを渡す形) の型指定版 (**型指定 start**) もある。機能の性質に由来する違いは次の 3 点で、合流や失敗モデルとの関係は [Loading のルール](loading-semantics.md) / [Toast のルール](toast-semantics.md) が持つ。

| 機能 | configure | View factory の前に挟まる紐付け | VM factory / configure の例外 |
|---|---|---|---|
| Dialog | 非同期可 | 結果報告口 | 提示に進まず呼び出し元へ伝播する (結果に化けない) |
| Loading (型指定 show / 型指定 start) | 非同期可 | 進捗受け口。合流の判定は configure 完了後に入り、合流側になった呼び出しの VM は表示に使われない | 提示に進まず呼び出し元へ伝播し、合流 1 件に数えない。型指定 start は処理ブロックも実行しない |
| Toast | 同期のみ (show が fire-and-forget の同期呼び出しのため) | なし | 呼び出しは既に戻っているため伝播できず、受理後の失敗 (警告 + その 1 枚だけ破棄) になる。VM factory 未登録だけは呼び出し時点の同期失敗 |

Toast の行は Native (iOS / Android / MAUI) の挙動で、**KMP の共有コードの型指定 show だけは Toast でも VM factory / configure の例外が show から同期に呼び出し元へ伝播する** — 共有コードは生成と configure を呼び出しスレッドで済ませてから各 OS の経路へ渡すため、受理後の失敗にならない (下記「KMP での見え方」)。

### VM factory による解決

型指定 show の ViewModel は、**レジストリに登録した VM factory** で生成する ([core/ADR-0021](../../../decisions/core/0021-vm-factory-registry-resolution.md))。VM factory の登録 API を View factory の登録と同名にするか別名にするかは、言語のオーバーロード解決の都合で形態ごとに違う。

- **未登録の型指定 show は構成ミスとして失敗する**。暗黙の既定コンストラクタ生成で隠さない — DI 登録を忘れた依存未注入の ViewModel が黙って生まれる事故を防ぐ
- DI コンテナ連携は **VM factory の中身**として表現する。core 契約はコンテナを知らない
- レジストリのエントリは **View factory と VM factory の2スロット**を持ち、再登録は**スロット単位の後勝ち** — 該当スロットだけを置換し他方は保持する。Dialog / Loading / Toast の 3 つの専用レジストリはどれもこの構成で、互いに独立している (同じ VM 型を別々に登録できる)
- show 時の解決は**呼び出し時点のエントリのスナップショット**による — 表示中に再登録が起きても、出ているダイアログの結果型・報告口の取得は show 時点の登録で判定される。Loading / Toast でも同じで、表示中の再登録は出ている表示の中身と進捗転送先を変えない
- KMP の共有コードから型指定 show するときの VM factory は**共有コードのレジストリに登録する**。各 OS の Native レジストリの VM factory スロットとは互いに独立した表で、Native 側に登録した VM factory は共有コードの型指定 show から見えない (登録先を取り違えると未登録として失敗する — 黙って通らない)

MAUI だけは1行登録が VM factory の DI 配線まで自動化する (Dialog / Loading / Toast の 3 機能とも — [MAUI の DI 連携と登録糖衣](../../maui/api/di-registration.md))。

### configure の順序保証

実行順序は「VM factory で生成 → configure 完了 (非同期含む) → 報告口の紐付け → View factory → 提示」に固定される。したがって:

- **configure が設定した状態を、View の初期化が必ず読める** (View 生成は configure 完了より後)
- VM factory と configure は View factory と同じ UI スレッド保証で実行される (KMP の共有コードだけは例外 — 次の段落)
- VM factory・configure の例外 (キャンセル含む) は**提示に進まず呼び出し元へ伝播し、結果には化けない**

UI スレッド保証の例外は KMP の共有コードの型指定 show である。共有コードは UI スレッドの概念を持たないため、VM factory と configure を**呼び出し元の文脈** (呼び出し元のコルーチン文脈、Toast は呼び出しスレッド) で実行し、UI スレッドへ移さない — 利用者が VM を作ってからインスタンス渡し show するのと同じ形である ([kmp/ADR-0006](../../../decisions/kmp/0006-common-vm-factory-typed-show.md))。

例外が結果に化けない時点では報告口が未紐付けのため、後始末すべき紐付けが残らず、次回の show が並行 show と誤判定されることもない (VM factory が同一インスタンスを返す構成でも安全にやり直せる)。

configure は初期化パラメータの配達に限らない汎用のセットアップ地点 (表示前の非同期ロード・VM 参照の捕獲・コールバック配線) である。ViewModel 契約にライフサイクルフック (初期化・破棄のメンバー) は持ち込まない ([core/ADR-0019](../../../decisions/core/0019-no-lifecycle-hooks-configure-closure.md)) — 後始末は show を待ち終えたあとに呼び出し側が書く (キャンセル時も言語の後処理構文で拾える)。

## 1引数 factory

報告口が ViewModel から引けるようになったため、View factory は報告口の引数を省いた **「ViewModel だけを受け取って中身を返す」1引数形**でも登録できる。従来の2引数形は低水準 API として残る。技術別の呼び分け (宣言的 UI 系) も従来どおりである。

この形は**登録のオーバーロード**であり、型指定 show 専用の書き方ではない — 登録済みの factory を解決して表示する経路 (インスタンス渡し show・型指定 show) であれば同じに働く。

## KMP での見え方

### 共有コード (commonMain) の型指定 show

共有 ViewModel をインスタンス渡し show して型付き結果を受ける経路に加え、共有コードにも Dialog / Loading / Toast の型指定 show (Loading は型指定 start も) がある。結果の報告は Native 側の View が VM から引いた報告口で行う点は変わらない。共有コードの型指定 show は次の点で Native と違う ([kmp/ADR-0006](../../../decisions/kmp/0006-common-vm-factory-typed-show.md))。

| 観点 | 共有コードでの扱い |
|---|---|
| VM factory の登録先 | 共有コードのレジストリ (実体は共有コード側)。View factory の登録は引き続き各 OS 側で、Native レジストリの VM factory スロットとは独立 |
| 解決の流れ | 共有コードで VM factory から生成 → configure 完了 → 既存のインスタンス渡し show に流す。Native の型指定経路は使わない |
| 実行文脈 | VM factory と configure は呼び出し元の文脈で実行し、UI スレッドへ移さない (UI スレッドが要る処理は各 OS 側の View factory で行う) |
| 例外の伝播 | VM factory / configure の例外は 3 機能とも提示に進まず呼び出し元へ伝播する (Toast も同期に伝播し、受理後の失敗にならない) |
| 生成物の型 | VM factory は登録キーと同じクラスの VM を返さなければならず、違えば型不一致の構成ミスとして失敗する (Native はインスタンスの実行時クラスで View factory を引くため、サブクラスを返すと未登録に化けるのを防ぐ) |
| 失敗の型 | VM factory 未登録・型不一致とも共有コードの構成ミスの失敗型で、原因は説明文で区別する |
| Swift からの可視性 | 型指定 show と VM factory の登録口は共有 Kotlin コード専用で、Swift / ObjC からは見えない。Swift 向け KMP 面に型指定 show は無い (iOS ホストは Kotlin の VM を Swift で作ってインスタンス渡し show する) |

### 各 OS ホスト側

- **Android 面**: 共有 ViewModel は Android Native 型の別名なので、Android Native の取得の書き方がそのまま効く。追加 API なし
- **iOS 面**: KMP の ViewModel は iOS Native の ViewModel 契約に準拠しないため、KMP の Swift 向け公開面が専用のアクセサを持つ

iOS 面の専用アクセサは、**show 外では空を返し、宣言結果型と一致しない結果型を指定したときは失敗する** (エラーで失敗するので、show 外の「まだ無い」と型の書き間違いを取り違えない)。署名と使い方は [KMP 利用者の iOS ホスト統合](../../kmp/api/ios-host-integration.md) が定める。

## 保証すること

- **show 中の ViewModel からは、宣言結果型に型付いた報告口が必ず取得できる** (View factory 実行中を含む)。show の前後では取得できない — この境界が崩れると、報告してもどこにも届かない報告口や、前回の show へ誤配送する報告口が生まれる
- **紐付けの除去は全終端経路で行われ、失敗直後でも同じ ViewModel を再 show できる**
- **configure の完了は View 生成より前**。表示された View が configure 前の状態を観察することはない
- **型指定 show の解決は show 呼び出し時点のスナップショット**。表示中の再登録は出ているダイアログに影響しない
- **構成ミス (VM factory 未登録・同一インスタンス並行 show・値型 ViewModel) は失敗で即返り、cancelled 等の結果に化けない** ([結果通知のルール](result-notification-semantics.md) の原則の適用)
- **この文書の呼び出し面はすべて呼ぶ側に対して非破壊の追加**。2引数 factory・インスタンス渡し show・インライン show は従来どおり使える (範囲は次の小節)

### 「非破壊の追加」が指す範囲

ここでいう非破壊は**呼ぶ側のソース互換** — 既存の呼び出し (2引数 factory の登録・インスタンス渡し show・インライン show) がそのまま通ること — を指す。型指定 show は表示契約 (`KsDialog` / `KsLoading` / `KsToast` の protocol / interface) の抽象メンバーとして増えるため、契約を**自前で実装する側** (利用者のテストダブル・adapter) は再コンパイル時に新メンバーの実装を求められる。既定実装 (protocol extension / default interface method) で吸収して契約の形を変える案は採らない — Dialog の型指定 show の導入時も Loading / Toast への拡張時も、KMP の共有コードへの追加時も同じ扱いで、互換の主張を書くときは呼ぶ側の互換に限定して書く。

## してはいけないこと

- **値型を ViewModel にしない**。コンパイルまたは実行時の構成ミスとして拒否される。この拒否をどれかの show 経路だけ免除する実装もしない — 抜け穴が1つでもあると「報告できないダイアログが黙って残る」事故が復活する
- **報告口を show の外で読んで保持しない**。show 中に取得した報告口を中身が保持するのは正しいが、show 前に読んだ空の値を「後で埋まる」と期待してはいけない
- **型指定 show の未登録を暗黙の既定コンストラクタ生成で救済しない**。依存未注入の ViewModel が黙って生まれる
- **VM factory・configure の例外を結果 (cancelled) に変換しない**。構成・準備の失敗と利用者の操作は別物である
- **ViewModel 契約にライフサイクルフックを足さない**。初期化はコンストラクタか configure、後始末は show を待ち終えた呼び出し側が担う。破棄フックが将来必要になっても opt-in の非破壊追加で行う

## 用語

| 用語 | 意味 |
|---|---|
| 形態 | 本ライブラリを利用できる4つの入口 (iOS Native / Android Native / MAUI / KMP) |
| 中身 | factory が返す、ダイアログの器の内側に載る表示物 ([登録と表示の呼び出し面のルール](registration-show-semantics.md)) |
| 結果報告口 (`DialogNotifier`) | 中身の側から結果を報告する部品。定義は [結果通知のルール](result-notification-semantics.md) |
| 宣言結果型 | ViewModel が `DialogViewModel` の契約で宣言する結果の型。省略時は真偽値 ([登録と表示の呼び出し面のルール](registration-show-semantics.md) の省略形) |
| インスタンス渡し show | ViewModel のインスタンスを渡す従来の呼び出し経路 |
| インライン show | 登録せず factory を直接渡す呼び出し経路 ([登録と表示の呼び出し面のルール](registration-show-semantics.md)) |
| 型指定 show | ViewModel の型を渡し、ライブラリが VM factory で生成して表示する呼び出し経路 |
| VM factory | 型指定 show のために ViewModel を生成する登録済み関数。View factory と対でレジストリのエントリに載る |
| 型指定 start | Loading のスコープ形 (処理ブロックを渡す形) の型指定版。VM の生成・configure・失敗の扱いは型指定 show と同じ ([Loading のルール](loading-semantics.md)) |
| configure | 型指定 show が生成直後の ViewModel に適用するセットアップ用クロージャ (省略可・非同期可) |
| スナップショット解決 | show 呼び出し時点のレジストリエントリのコピーで以後の判定を行うこと |
| 1引数 factory | 報告口の引数を省いた「ViewModel だけを受け取って中身を返す」View factory |
| 共通の提示入口 | 形態内の全 show 経路が通る内部の合流点。値型拒否などの全経路検査はここに置く |

## 形態別の公開面

報告口の取得の綴り・参照型限定の強制手段・型指定 show と VM factory 登録の書き方・構成ミスの失敗の名前とコード例は、各形態の公開面が持つ:

- [iOS の Dialog 公開面](../../ios/api/dialog-surface.md)
- [Android の Dialog 公開面](../../android/api/dialog-surface.md)
- [MAUI の Dialog 公開面](../../maui/api/dialog-surface.md)
- [KMP の Dialog 公開面](../../kmp/api/dialog-surface.md) — 共有コード側の公開面と、各 OS ホストのどちらを読むかの案内
- [KMP 利用者の iOS ホスト統合](../../kmp/api/ios-host-integration.md) — KMP の Swift 向け公開面 (登録・表示・報告口アクセサの署名)

Loading / Toast の型指定 show と VM factory 登録の書き方は、各形態の Loading / Toast 公開面が持つ: [iOS の Loading 公開面](../../ios/api/loading-surface.md) / [iOS の Toast 公開面](../../ios/api/toast-surface.md)・[Android の Loading 公開面](../../android/api/loading-surface.md) / [Android の Toast 公開面](../../android/api/toast-surface.md)・[MAUI の Loading 公開面](../../maui/api/loading-surface.md) / [MAUI の Toast 公開面](../../maui/api/toast-surface.md)・[KMP の Loading 公開面](../../kmp/api/loading-surface.md) / [KMP の Toast 公開面](../../kmp/api/toast-surface.md) (共有コード側)。

## 関連

### 契約と公開面

- [結果通知のルール](result-notification-semantics.md) — show が返すもの・構成ミスは失敗で返す原則・重ね出し
- [登録と表示の呼び出し面のルール](registration-show-semantics.md) — register / show の基本形・技術別の呼び分け・インライン show
- [Loading のルール](loading-semantics.md) / [Toast のルール](toast-semantics.md) — 型指定 show と合流・失敗モデルの関係
- [MAUI の DI 連携と登録糖衣](../../maui/api/di-registration.md) — 1行登録と fallback resolver (MAUI 限定)
- [KMP 利用者の iOS ホスト統合](../../kmp/api/ios-host-integration.md) — KMP の Swift 向け公開面 (登録・報告口アクセサ)

### 決定

- [core/ADR-0018](../../../decisions/core/0018-notifier-vm-injection-side-table.md) — 決定 (報告口のサイドテーブル紐付けと参照型限定)
- [core/ADR-0019](../../../decisions/core/0019-no-lifecycle-hooks-configure-closure.md) — 決定 (ライフサイクルフック非採用・configure クロージャ)
- [core/ADR-0020](../../../decisions/core/0020-show-verb-unification.md) — 決定 (動詞 show 1本への統一)
- [core/ADR-0021](../../../decisions/core/0021-vm-factory-registry-resolution.md) — 決定 (VM factory による解決・暗黙生成の不採用)
- [core/ADR-0035](../../../decisions/core/0035-loading-toast-typed-show-vm-factory.md) — 決定 (Loading / Toast のレジストリへの VM factory スロット追加と型指定 show の同型提供)
- [kmp/ADR-0006](../../../decisions/kmp/0006-common-vm-factory-typed-show.md) — 決定 (KMP の共有コードの型指定 show は共有コード側の VM factory 表で解決し、呼び出し元の文脈で生成・configure する)
