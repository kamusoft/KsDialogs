# Exploration: add-viewmodel-less-dialog-show

## 課題 / 動機

移植元 AiForms.Maui.Dialogs には、ViewModel を持たずにダイアログを出すルートがあった。

- 呼び出し側: `ShowAsync<SomeView>()` (結果型つきは `ShowResultAsync<TView, TResult>`)
- View 側: `class SomeView : DialogView` を継承し、基底クラスが自動生成する `Notifier` で `Notifier.Complete(...)` を呼ぶ

KsDialogs の Dialog の show 経路 (インスタンス渡し・インライン factory・型指定) は、どれも ViewModel が必須になっている。View が Notifier に届くのは「2 引数 factory の引数で受け取る」か「VM から `vm.notifier` を引く」かのどちらかだけで、VM を持たない View から報告する道はない。現状の回避策は、空の VM を作って登録することになる。

経緯: core/ADR-0004 の Decision 最終項は「View 型を直接指定する呼び方 (原典 `ShowAsync<TView>` 相当) はレジストリ登録なしで使える」と書いている。しかし core/ADR-0013 (2026-08-17) がこれを「DI 糖衣とセットで扱う」と先送りした。その後に設計した MAUI の 1 行登録 (`RegisterForDialog<TView, TViewModel>`) は VM を前提としたため、この宿題は拾われないままロードマップ library-foundation がアーカイブされた (記録は kasane/roadmaps/archive/2026-09-04-library-foundation/phases/phase-5-2-api-surface/history.md)。

## 検討した選択肢

### show のときに呼び出し側が何を渡すか

| 案 | 評価 |
|---|---|
| A. View の型を渡す (移植元と同じ形) | MAUI・UIKit・protocol に準拠した Swift の View では可能。Android の従来 View は Context が要るため登録が必要。Compose と KMP の共有コードでは不可。→ **MAUI の上乗せとしてのみ採用** |
| B. 結果型つきの「ダイアログ名」を登録して渡す | 4 形態すべてで使え、KMP の共有コードからも呼べる。ただし空の VM を作って登録する回避策と手数がほぼ同じで、VM がないことの利点が残らない。→ 却下 |
| C. VM を渡さないインライン show (`show { notifier -> View }`) | Native の 4 技術 + MAUI で使える。KMP の共有コードでは使えない。登録不要。→ **4 形態の基本経路として採用** |

### MAUI の View 型 show で、View が Notifier をどう受け取るか

| 案 | 評価 |
|---|---|
| 2a. 基底クラス `DialogView<TResult>` を継ぐ | 移植元に最も近い。ただし継承の枠を消費し、core/ADR-0017 が基底クラス継承を却下した理由 (基底を継いだ View と普通の View の 2 系統ができる) と一部ぶつかる。→ 却下 |
| 2b. コンストラクタの引数で受け取る | 継承不要で、コンストラクタの中でも使える。ただし結果型の食い違いが実行時まで分からない。→ 却下 |
| 2c. インターフェース `IDialogContent<TResult>` (仮称) を実装し、ライブラリがプロパティへ設定する | 継承不要で、コンパイル時の型制約で食い違いを止められる。呼び出す箇所は移植元と同じ `Notifier.Complete(...)`。コンストラクタの中では使えない。→ **採用** |

### VM がないとき、結果の型を誰が宣言するか

| 案 | 評価 |
|---|---|
| 3a. 「結果を報告する側が宣言する」に一般化し、省略時は真偽値 | 1 つの原則で説明でき、移植元の `ShowResultAsync<TView, TResult>` にも対応できる。→ **採用** |
| 3b. VM なしの経路だけ例外として「呼び出し側が指定する」と書く | 「呼び出し側の指定では守れない」という既存ルールの文言と例外が並び、説明が割れる。→ 却下 |
| 3c. VM なしの経路は真偽値だけにする | カスタム結果型のダイアログは VM が必須のまま残る。→ 却下 |

## 決定事項

- show のときに渡すもの: VM を渡さないインライン show を 4 形態の基本経路にし、View の型を渡す show は MAUI の上乗せにする
- MAUI の View 型 show では、View がインターフェースを実装し、ライブラリが生成後・提示前にそのプロパティへ Notifier を設定する (基底クラスは使わない)
- 結果型は結果を報告する側 (VM、または VM なし経路の中身) が宣言し、省略時は真偽値とする (core/ADR-0003 のルール 1 と core/ADR-0012 の一部改訂)
- core/ADR-0004 の最終項 (「View 型を直接指定する呼び方は登録なしで使える」) は、同じ ADR 下書きの amends で「MAUI の View 型 show としてのみ提供し、他の形態は VM なしのインライン show で代える」に置き換える。関連行を足すだけの案は、最終項の「全形態で使える」という読みが残るため採らない

## ADR 候補

- 下書き: [adr-draft.md](adr-draft.md) (proposed・未採番) — 上の 4 つの決定をまとめたもの。core/ADR-0003・0004・0012 の amends。オーナーの指示で decisions/ にはまだ置かず、この change の中で保管している

## 未決の論点

- Loading / Toast にも VM なしの経路を用意するか (今回の議論は Dialog だけが対象)
- VM なしのインライン show を、KMP の Swift 向け公開面 (iOS ホスト) にも出すか
- Kotlin で「型を省略すると真偽値」と「型を指定する」の同名オーバーロードを並べると、JVM 上で型引数が消えたあとのシグネチャが衝突しうる。命名や `@JvmName` での逃がし方は提案の段階で確かめる
- MAUI の View 型 show で、TView の生成方法 (DI を使う構成なら `ActivatorUtilities`、DI なしなら引数なしコンストラクタ) と、BindingContext を設定するかどうか
- 追従が必要な concepts: core/api/result-notification-semantics.md (ルール 1)・core/api/registration-show-semantics.md (インライン show)・各形態の dialog-surface.md・maui/api/di-registration.md
- 利用者向けの移行ガイド (skills/ の aiforms-migration) の追従。更新は docs-refresh 経由

## UI 素材

なし

## 変更級の推奨

L (オーナー確定 2026-09-25)。Dialog の公開 API を 4 形態にまたいで追加し、core 契約 (結果型の宣言者) を変え、accepted の ADR を 3 本 (0003・0004・0012) 一部改訂するため。対象は Dialog だけとし、Loading / Toast に広げるかは提案の段階で決める
