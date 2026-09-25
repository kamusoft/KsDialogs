---
id: 0037
title: Loading のスコープ形の action は既定で UI スレッドで始め、UI スレッド外で始める指定を入口に持たせる
status: proposed
date: 2026-09-25
---

## Context

Loading のスコープ形 (処理 action を渡すと Loading を出して action を await し、終わったら閉じる入口) で、action がどのスレッドで始まるかは、契約 (concepts・ADR・公開 doc コメント) のどこにも定めが無い。形態ごとに次のように食い違っている (2026-09-25 コード確認)。

| 形態 | action が始まるスレッド |
|---|---|
| iOS Native | 常にメインスレッド外。コアの `start` と action の型に isolation の指定が無く、UI スレッドから呼んでも移る |
| Android Native / KMP | 呼び出し元のコルーチン文脈をそのまま使う |
| MAUI iOS | Swift のスレッドプール上で呼ばれる (メインスレッド外) |
| MAUI Android | ブリッジの scope が `Dispatchers.Main.immediate` のため常に UI スレッド |

action の実行スレッドを確かめるテストは全形態に無い。

ColorAnalyzer が AiForms.Maui.Dialogs から KsDialogs.Maui へ移行したところ、iOS 実機 (Debug 構成) でだけ、action 内の UIKit 呼び出しが `UIKitThreadAccessException` になった。同じコードは Android では成功する。AiForms の Loading は「表示したあと、同じ async メソッドの中で処理を await する」使い方で、移行スキルは `Show` / `Hide` の組を `StartAsync` へ写すよう案内している。そのため、UI スレッド前提の処理がそのまま action に入りやすい。

`StartAsync` はほぼ UI スレッド (画面のコマンド) から呼ばれる。利用者が困っているのは、action の中が UI スレッドかどうかが書く側から分からないことである。一方で、action には UI スレッドで動かす必要のない処理もある。

利用者のコードを呼ぶほかの箇所は、UI スレッドで呼ぶと決めている。VM factory・configure (core/ADR-0035)、transition の hook、進捗の受け口とフォーマット関数 (core/ADR-0027) がそれにあたる。例外は KMP の共有コードの VM factory・configure で、こちらは呼び出し元の文脈で動かす (kmp/ADR-0006)。

## Decision

- スコープ形の action は、**既定で UI スレッドで始まる**。全形態 (iOS Native / Android Native / MAUI / KMP) で同じとする
- スコープ形の入口で「UI スレッド外で始める」を指定できるようにする。指定すると、action は **UI スレッド外で始まる**
- 指定は**どちらの場合も、始まるスレッドを保証する**。「どこで始まってもよい (保証なし)」にあたる選択肢は置かない
- 指定の表し方は、形態の言語機構に合わせる (change define-loading-action-thread の design で確定)
  - Swift (iOS Native): action の型を `@MainActor` にし、UI スレッド外で始めるには利用者がクロージャに `@concurrent` を付ける。引数は足さない
  - Kotlin (Android Native / KMP) と C# (MAUI): 入口に列挙型 `LoadingActionThread` の引数を足す。既定値は UI スレッド側
- 保証の範囲には、次の 2 つの定めがある
  - 処理自身が実行スレッドを型で宣言している場合 (Swift の isolation) は、その宣言が優先される。isolation の指定が無い async 関数を名前で渡すと、UI スレッド外で始まる
  - 保証の対象は、UI スレッドを持つ実行環境 (iOS / Android) だけである。UI スレッドを持たない環境 (MAUI の素の .NET) では、指定に関係なくその場で実行する

## Alternatives Considered

- **A. 常に UI スレッドで始まる (フラグなし)** — 却下
  - ColorAnalyzer の要望はこの形だった
  - UI に触らない重い処理も UI スレッドで始まり、UI スレッドから外すかどうかは利用者任せになる。外し忘れると、止まっている間は進捗やメッセージの更新が画面に出ない
- **B. 既定は保証なし + UI スレッドで始めるフラグ** — 却下
  - 既定のままでは、action の中が UI スレッドかどうかが書く側から分からないという困りごとが残る
  - フラグを付け忘れると iOS でだけ落ちる
- **C. 呼び出し元の文脈を引き継ぐ (フラグなし)** — 却下
  - `StartAsync` はほぼ UI スレッドから呼ばれる。「UI スレッドで動かす必要のない処理はバックグラウンドから呼ぶ」という逃げ道は、実際には使われない
  - スレッドが呼び出し元しだいで決まり、「分からない」を呼び出し側へ移すだけになる

## Consequences

- 正: action の中が UI スレッドかどうかが、呼び出しの 1 行を見れば分かる
- 正: AiForms から移行したコードは、何も付けなければ UI スレッドで始まるので、そのまま動く
- 正: 利用者のコードを UI スレッドで呼ぶ既存の箇所 (core/ADR-0027・0035) と規則が揃う
- 負: Kotlin と C# では、スコープ形の入口すべて (Android Native・Compose・KMP・MAUI) に引数が 1 つ増える
- 負: Swift では、isolation の指定が無い async 関数を名前で渡すと、その関数自身の isolation が優先され、UI スレッド外で始まる (言語規則)。ただし型の上でも UI スレッドではないと分かるため、中で UIKit に触るとコンパイルエラーになる
- 負: 次の形態では、既定の動きが変わる
  - iOS Native と MAUI iOS: 今はメインスレッド外で始まる
  - Android Native と KMP: 今はバックグラウンドから呼ぶと、そのスレッドで始まる
- 負: KMP の共有コードで UI スレッドを保証することが、kmp/ADR-0006 の整理 (共有コードは呼び出し元の文脈で動かす) と両立するか、確かめる必要がある

## Revisit When

- KMP の共有コードで UI スレッドを保証することが kmp/ADR-0006 の整理と両立しないと分かったとき

出典:
- `../ColorAnalyzer/kasane/outbox/KsDialogs/2026-09-25-loading-action-thread-undefined.md` (事象と、ColorAnalyzer の要望)
- `kasane/changes/define-loading-action-thread/exploration.md` (現状の調査・検討した選択肢・決定事項。2026-09-25 の探索でオーナーが D 案を採用)
- `kasane/decisions/core/0027-loading-process-coordinator-single-source.md`・`0035-loading-toast-typed-show-vm-factory.md`・`kasane/decisions/kmp/0006-common-vm-factory-typed-show.md` (利用者のコードを呼ぶスレッドの前例)
