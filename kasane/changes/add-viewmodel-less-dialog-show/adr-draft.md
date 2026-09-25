---
id: 未採番 (decisions/ へ置くときに採番する)
title: VM を持たないダイアログは VM なしのインライン show と MAUI の View 型 show で出し、結果型は結果を報告する側が宣言する
status: proposed
date: 2026-09-25
amends: [0003, 0004, 0012]
---

## Context

移植元 AiForms.Maui.Dialogs には、ViewModel を持たずにダイアログを出す呼び方がある。呼び出し側は View の型だけを渡し (`ShowAsync<TView>(object viewModel = null)` / `ShowResultAsync<TView, TResult>`)、View は基底クラス `DialogView` が自動生成する Notifier で `Notifier.Complete(...)` と結果を報告する。

KsDialogs の Dialog の show 経路 (インスタンス渡し・インライン factory・型指定) は、どれも ViewModel を必須としている。core/ADR-0004 は Decision の最終項で「View 型を直接指定する呼び方 (原典 `ShowAsync<TView>` 相当) はレジストリ登録なしで使える」と定めたが、この呼び方はどの形態にも実装されていない。core/ADR-0013 は、移植元の View 型指定を「DI での View 解決と地続きのため、1 行登録・DI 糖衣の設計とセットで扱う」として先送りした。その後に設計した MAUI の 1 行登録 (`RegisterForDialog<TView, TViewModel>`) は ViewModel を前提としたため、VM を持たないダイアログの経路はどの形態にも用意されていない。現状の回避策は「空の ViewModel を作って登録し、View の中で `vm.Notifier` を読む」ことで、移植元に比べて手数が多い。

制約:

- SwiftUI の View は struct、Compose の中身は関数であり、「View の型」として受け渡すことも、基底クラスを継ぐこともできない
- KMP の共有コードは OS ごとの View の型を参照できない
- C# は型引数の一部だけを推論できない (結果型を型引数で明示するときは、View の型と結果型の 2 つを並べて書く)
- core/ADR-0003 が「結果型は ViewModel が宣言する」とした目的は、報告する側と受け取る側で結果型が食い違う書き方をコンパイルできなくすることにある。core/ADR-0012 は、カスタム結果型を持てるのを「結果型を宣言した ViewModel」だけに限っている
- core/ADR-0012 が却下した「View のコンストラクタで notifier を自前で生成する」案は、自前で生成した notifier がどの show とも結線されないために成立しなかった。本決定で View に渡す Notifier は、show が生成して結線済みのものをライブラリが渡すため、この却下理由は当てはまらない

前提: SwiftUI / Compose の中身を型として受け渡す手段がなく、KMP の共有コードが View の型を参照できないこと。

## Decision

VM を持たないダイアログは、次の 2 経路で出せるようにする。

**VM なしのインライン show** を、4 形態の基本経路とする。呼び出し側は「Notifier を受け取って中身を返す factory」だけを渡し、ViewModel も登録も要らない。core/ADR-0013 のインライン show と同じく、レジストリを経由しない一時 factory 実行とする。提供先は Dialog の全 Native 技術 (SwiftUI / UIKit / Compose / Android の従来 View) と MAUI で、KMP の共有コードは View を供給できないため対象外とする。

**MAUI の View 型 show** を、MAUI だけの上乗せとして提供する。呼び出し側は View の型だけを渡す (`ShowAsync<TView>()` / `ShowAsync<TView, TResult>()`)。TView は結果報告口を受け取るインターフェースを実装し、ライブラリは TView を生成したあと、提示する前に、そのプロパティへ Notifier を設定する。ライブラリの基底クラスの継承は求めない。結果型の食い違いは、型引数の制約 (TView がそのインターフェースを同じ結果型で実装していること) でコンパイル時に止める。

結果型は**結果を報告する側が宣言し**、show はその宣言から戻り値の型を導出する。報告する側とは、VM 経路では ViewModel、VM なしのインライン show では factory が受け取る Notifier の型、View 型 show では View が実装するインターフェースの型引数である。宣言を省略した場合は真偽値とする。

core/ADR-0003 の決定のうち「結果型は表示する ViewModel が宣言し、show はその宣言から結果型を導出する」を、本決定の「結果を報告する側が宣言する」で置き換える。他の決定は維持する。core/ADR-0004 の決定のうち「View 型を直接指定する呼び方はレジストリ登録なしで使える」を、「View 型の直接指定は MAUI の View 型 show としてのみ提供し、他の形態は VM なしのインライン show で代える」で置き換える。他の決定は維持する。core/ADR-0012 の決定のうち「カスタム結果型は宣言した VM だけが持つ」を、「カスタム結果型は結果を報告する側が宣言する」で置き換える。真偽値を既定とする決定は維持する。

## Alternatives Considered

- **View の型を渡す show を全形態の基本経路にする (移植元と同じ形)** — 却下。SwiftUI と Compose では View を型として渡せず、Android の従来 View は生成に Context が要るため結局は登録が必要になり、KMP の共有コードからは使えない。MAUI の上乗せとしてのみ採る
- **結果型つきの「ダイアログ名」(状態を持たない識別子) を登録し、それを渡して show する** — 却下。空の ViewModel を作って登録する現状の回避策と手数がほぼ変わらず、VM がないことによる利点が残らない。KMP の共有コードから呼べる唯一の案だったが、移植元も MAUI 専用であり、共有コードから VM なしで出す需要は薄い
- **View 型 show で、ライブラリの基底クラスを継ぐと Notifier が使える形にする (移植元と同じ形)** — 却下。継承の枠を消費し (ContentView 以外を土台にできない)、core/ADR-0017 が基底クラス継承を却下したときと同じく、「基底を継いだ View」と「普通の View」の 2 系統を並べることになる
- **View 型 show で、Notifier をコンストラクタの引数として受け取る** — 却下。コンストラクタ内で Notifier を使える利点はあるが、結果型の食い違いが、合うコンストラクタが見つからないという実行時の失敗になるまで分からない
- **VM なしの経路だけを例外として、「呼び出し側が結果型を指定する」と書く** — 却下。「呼び出し側の指定では守れない」という既存ルールの文言と例外が並び、説明が 2 つに割れる。型の安全性は「報告する側が宣言する」と書いた場合と変わらない
- **VM なしの経路は真偽値の結果だけにする** — 却下。移植元の `ShowResultAsync<TView, TResult>` 相当が失われ、カスタム結果型のダイアログは ViewModel が必須のまま残る

## Consequences

- 正: ViewModel を持たずに View だけで完結するダイアログを、空の ViewModel と登録なしで出せる
- 正: MAUI で移植元を使っていた人は、`ShowAsync<TView>()` と `Notifier.Complete(...)` という移植元に近い書き方で移行できる
- 正: 報告する側と受け取る側の結果型の食い違いは、VM の有無によらず全経路でコンパイル時に止まる
- 負: show のオーバーロードがさらに増える (VM あり / なし × UI 技術 × 結果型の既定 / 明示)。公開 API を説明する範囲が広がる
- 負: View 型 show は MAUI だけにあるため、形態間の非対称が増える。KMP の共有コードからは VM なしの経路を使えない
- 負: View 型 show の View は、コンストラクタの中では Notifier を使えない (生成したあとに設定されるため)。また、インターフェースのプロパティを 1 行宣言する必要がある
- 負: 結果型を宣言する場所が 3 か所 (ViewModel・factory の Notifier・View のインターフェース) になり、結果通知のルールの説明が「VM が宣言する」の 1 文では済まなくなる

## Revisit When

前提 (Context) が崩れたとき。

出典: kasane/changes/add-viewmodel-less-dialog-show/exploration.md (検討した選択肢・決定事項)
関連: core/ADR-0013 (VM ありのインライン show。本決定の VM なしインライン show はその延長) / core/ADR-0017 (基底クラス継承の却下) / core/ADR-0018 (VM の基底クラス案の却下と、Notifier の VM への紐付け)
