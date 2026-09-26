---
id: 0037
title: Loading のスコープ形の action は既定で UI スレッドで始め、UI スレッド外で始める指定を入口に持たせる
status: accepted
date: 2026-09-25
---

## Context

Loading のスコープ形 (処理 action を渡すと Loading を出して action を await し、終わったら閉じる入口) で、action がどのスレッドで始まるかは、契約 (concepts・ADR・公開 doc コメント) のどこにも定めが無かった。形態ごとに次のように食い違っていた (2026-09-25 コード確認)。

| 形態 | action が始まるスレッド |
|---|---|
| iOS Native | 常にメインスレッド外。コアの `start` と action の型に isolation の指定が無く、UI スレッドから呼んでも移る |
| Android Native / KMP | 呼び出し元のコルーチン文脈をそのまま使う |
| MAUI iOS | Swift のスレッドプール上で呼ばれる (メインスレッド外) |
| MAUI Android | ブリッジの scope が `Dispatchers.Main.immediate` のため常に UI スレッド |

action の実行スレッドを確かめるテストは全形態に無かった。

ColorAnalyzer が AiForms.Maui.Dialogs から KsDialogs.Maui へ移行したところ、iOS 実機 (Debug 構成) でだけ、action 内の UIKit 呼び出しが `UIKitThreadAccessException` になった。同じコードは Android では成功する。AiForms の Loading は「表示したあと、同じ async メソッドの中で処理を await する」使い方で、移行スキルは `Show` / `Hide` の組を `StartAsync` へ写すよう案内している。そのため、UI スレッド前提の処理がそのまま action に入りやすい。

`StartAsync` はほぼ UI スレッド (画面のコマンド) から呼ばれる。利用者が困っているのは、action の中が UI スレッドかどうかが書く側から分からないことである。一方で、action には UI スレッドで動かす必要のない処理もある。

利用者のコードを呼ぶほかの箇所は、UI スレッドで呼ぶと決めている。VM factory・configure (core/ADR-0035)、transition の hook、進捗の受け口とフォーマット関数 (core/ADR-0027) がそれにあたる。例外は KMP の共有コードの VM factory・configure で、こちらは呼び出し元の文脈で動かす (kmp/ADR-0006)。kmp/ADR-0006 の論拠は、共有コードが UI スレッドの概念を持たないこと (最薄のファサードと、Fake 差し替えのテスト容易性) にある。

前提: 実行スレッドを関数の型で宣言できるのは Swift だけで、Kotlin と C# は関数の型にスレッドを載せられない。

## Decision

- スコープ形の action は、**既定で UI スレッドで始まる**。全形態 (iOS Native / Android Native / MAUI / KMP) の全入口 (既定ローディング・インスタンス渡し・インライン・型指定) で同じとする
- スコープ形の入口で「UI スレッド外で始める」を指定できるようにする。指定すると、action は **UI スレッド外で始まる**
- 指定は**どちらの場合も、呼び出し元のスレッドに関係なく、始まるスレッドを保証する**。「どこで始まってもよい (保証なし)」にあたる選択肢は置かない
- 「始まる」は action の本体の最初の文が実行されるスレッドを指す。action の中で await した後の戻り先は各言語の規則に従い、この決定は保証しない
- 指定の表し方は、形態の言語機構に合わせる
  - Swift (iOS Native): action の型を `@MainActor` にし、UI スレッド外で始めるには利用者がクロージャに `@concurrent` を付ける。入口と引数は増やさない
  - Kotlin (Android Native / KMP) と C# (MAUI): 入口に列挙型 `LoadingActionThread` の引数を足す。既定値は UI スレッド側
- 切り替えは、各形態で action を最後に呼ぶ 1 か所に置く
  - KMP: 各 OS の gateway に置く (Android は Native へ指定を渡し、iOS は gateway 自身が切り替える)。共有コードは UI スレッドの概念を持たないままにする。kmp/ADR-0006 が呼び出し元の文脈で動かすと決めたのは VM factory・configure であり、action はその範囲に含めない
  - MAUI: managed 側で切り替え、両 OS に同じ仕組みで効かせる
- 保証の範囲には、次の 2 つの定めがある
  - 処理自身が実行スレッドを型で宣言している場合 (Swift の isolation) は、その宣言が優先される
  - 保証の対象は、UI スレッドを持つ実行環境 (iOS / Android) だけである。UI スレッドを持たない環境 (MAUI の素の .NET) では、指定に関係なくその場で実行する

## Alternatives Considered

### 既定と保証の形

- **A. 常に UI スレッドで始まる (指定なし)** — 却下
  - ColorAnalyzer の要望はこの形だった
  - UI に触らない重い処理も UI スレッドで始まり、UI スレッドから外すかどうかは利用者任せになる。外し忘れると、止まっている間は進捗やメッセージの更新が画面に出ない
- **B. 既定は保証なし + UI スレッドで始めるフラグ** — 却下
  - 既定のままでは、action の中が UI スレッドかどうかが書く側から分からないという困りごとが残る
  - フラグを付け忘れると iOS でだけ落ちる
- **C. 呼び出し元の文脈を引き継ぐ (指定なし)** — 却下
  - `StartAsync` はほぼ UI スレッドから呼ばれる。「UI スレッドで動かす必要のない処理はバックグラウンドから呼ぶ」という逃げ道は、実際には使われない
  - スレッドが呼び出し元しだいで決まり、「分からない」を呼び出し側へ移すだけになる

### 指定の表し方

- **Swift で UI スレッド外用の入口を別に足す** — 却下。動作は正しいが、protocol 要件が 5 本から 10 本に倍増し、省略形も増える。`@concurrent` で同じことが表せる
- **Swift で action の型を `nonisolated(nonsending)` の 1 つにし、実行時のフラグで呼ぶ場所を切り替える** — 却下。実行時のスレッドは正しいが、UI スレッドで始まる場合でも型の上では UI スレッドだと分からず、中で MainActor に属する状態に触るとコンパイルエラーになる
- **Swift にも Kotlin / C# と同じ列挙型の引数を足す** — 却下。引数の値で action の型の isolation を変えられないため、上の 2 案のどちらかになる
- **真偽値の引数 (例: `runOnUiThread = true`)** — 却下。呼び出し側に `false` と書かれても意味が読めない
- **nullable の列挙型で、null を既定の意味にする** (`placement` と同じ慣習) — 却下。`placement` の null は「スタイルの既定に従う」という意味を持つが、スレッドの指定には「スタイル側の既定」が無く、null の意味を読む手間だけが増える
- **任意の dispatcher / executor を渡せる形** — 却下。2 つの値を保証する形で足り、それ以上の需要は確認できていない

### 切り替えの場所

- **KMP の共有コードで切り替える** — 却下。共有コードに coroutines の依存と UI スレッドの概念を持ち込むことになり、kmp/ADR-0006 の論拠とぶつかる。Android は Native に委譲しているので、共有コードで切り替えると 2 重になる
- **MAUI の両 OS のブリッジ (Swift / Kotlin) で切り替える** — 却下。C# の action を呼ぶスレッドを 2 か所で合わせることになる

## Consequences

- 正: action の中が UI スレッドかどうかが、呼び出しの 1 行を見れば分かる。Swift では型にも出て、UI スレッド外で始まる action の中で MainActor の状態に触るとコンパイラが止める
- 正: AiForms から移行したコードは、何も付けなければ UI スレッドで始まるので、そのまま動く
- 正: 利用者のコードを UI スレッドで呼ぶ既存の箇所 (core/ADR-0027・0035) と規則が揃う
- 正: KMP の共有コードは UI スレッドの概念を持たないままなので、kmp/ADR-0006 の論拠 (最薄のファサード・Fake 差し替えのテスト容易性) を損なわない
- 負: Kotlin と C# では、スコープ形の入口すべて (Android Native・Compose・KMP・MAUI) に引数が 1 つ増える
- 負: Swift では、isolation を持つ関数を名前で渡すと、その関数の isolation が優先され、UI スレッドで始まるとは限らない (言語規則)。ただし型の上でもそれが分かるため、UI スレッドではない関数の中で UIKit に触るとコンパイルエラーになる
- 負: Kotlin の既定値つき引数は Objective-C 経由で Swift に書き出すと既定値が消えるため、KMP を Swift から直接呼ぶ利用者は指定を明示で渡す必要がある
- 負: UI に触らない重い処理を action に同期で書いている利用者は、既定のままだと UI スレッドを塞ぐ。UI スレッド外で始める指定へ移す案内が要る
- 負: 次の形態では、既定の動きが変わる (破壊的変更)
  - iOS Native と MAUI iOS: メインスレッド外で始まっていたものが、UI スレッドで始まる
  - Android Native と KMP: バックグラウンドから呼ぶとそのスレッドで始まっていたものが、UI スレッドで始まる

## Revisit When

- UI スレッド / UI スレッド外の 2 つの値では足りず、任意の実行先 (dispatcher / executor) を渡したい需要が確認されたとき

出典:
- `../ColorAnalyzer/kasane/outbox/KsDialogs/2026-09-25-loading-action-thread-undefined.md` (事象と、ColorAnalyzer の要望)
- `kasane/changes/archive/2026-09-26-define-loading-action-thread/exploration.md` (現状の調査・検討した選択肢・決定事項。2026-09-25 の探索でオーナーが D 案を採用)
- `kasane/changes/archive/2026-09-26-define-loading-action-thread/design.md` (Decision 1〜4: 指定の表し方・切り替えの場所・「始まる」の定義と、それぞれの代替案) / `proposal.md` (Non-Goals・Impact)
- `kasane/decisions/core/0027-loading-process-coordinator-single-source.md`・`0035-loading-toast-typed-show-vm-factory.md`・`kasane/decisions/kmp/0006-common-vm-factory-typed-show.md` (利用者のコードを呼ぶスレッドの前例)

関連: kmp/ADR-0006 (共有コードの VM factory・configure は呼び出し元の文脈で動かす。本決定の action はその範囲に含めない)
