---
type: concept
title: KMP の Dialog 公開面
description: KMP の共有コード (commonMain) からダイアログを使うときの公開名と署名 — 既定エントリと show・結果型を省略できないこと・型指定 show と VM factory の登録 (共有 Kotlin コード専用)・View factory の登録は各 OS 側で行うこと・共有コードに添付の面が無いこと・Android / iOS ホスト側の見え方・失敗とキャンセルの届き方と Swift 境界の @Throws
tags: [kmp, dialog, api, surface]
timestamp: 2026-09-07
---

# KMP の Dialog 公開面

この文書を読むと、KMP の共有コード (commonMain) からダイアログを表示するときに書く名前と署名、各 OS のホスト側で何を書くか、Swift から呼ぶときの失敗の届き方が分かる。

用語: **共有コード**は KMP モジュールの commonMain に書く Kotlin コード、**Native** は各 OS 単体で使える iOS / Android のライブラリを指す。「共有 Kotlin コード専用」は共有コードと同じ場所を指し、Swift から呼べないことを強調した言い方である。**View factory** は中身の View の作り方、**VM factory** は ViewModel の作り方を登録する関数である。

**この文書は KMP の公開面 (名前・署名・コード例・framework 固有の注意) だけを扱い、挙動の契約は core が正である。** 対応する契約は次の 4 本で、「何が起きるか」はそちらを読む:

- [登録と表示の呼び出し面のルール](../../core/api/registration-show-semantics.md) — register / show の基本形・結果型の省略形・中身の技術・インライン show
- [結果通知のルール](../../core/api/result-notification-semantics.md) — show が返すもの・キャンセル・構成ミス
- [多段表示のルール](../../core/api/multi-display-semantics.md) — 重ね出しの保証
- [ViewModel 主導の呼び出しのルール](../../core/api/model-binding-semantics.md) — 結果報告口の VM 供給・型指定 show・参照型限定・KMP での見え方

## 共有コード (commonMain) の公開面

| 用途 | 書く名前 |
|---|---|
| 既定の表示エントリ | `Dialog.instance` |
| DI で注入する表示契約 | `KsDialog` |
| レジストリのハンドル | `Dialog.instance.registry` (型は `DialogViewRegistry`) |
| 表示 (インスタンス渡し) | `show(viewModel, placement)` (`suspend` 関数) |
| 表示 (型指定) | `show(viewModelClass, placement, configure)` (`suspend` 関数。共有 Kotlin コード専用) |
| VM factory の登録 | `registry.registerViewModel(viewModelClass, factory)` (共有 Kotlin コード専用) |

`DialogViewRegistry` が共有コードで持つ登録の面は **VM factory だけ**で、その表は共有コード側が持つ。**View factory の登録 API はこの面に無い** — View の型が OS ごとに違うため、中身の登録は各 OS の Native API で行う。ハンドルは**どの入口から取得しても同じものが返る** (既定エントリからでも DI 注入したインスタンスからでも同一)。View factory の紐付けは OS ごとに Native ライブラリのレジストリ 1 個に集約され、純 Native 利用者と共有コード利用者が同じレジストリを使う。

### 結果型は省略できない

Native にある真偽値の省略形 (結果型を書かない ViewModel の別名) は共有コードには置いていない。したがって共有コードでは ViewModel の宣言で結果型を常に明示する (真偽値なら `DialogViewModel<Boolean>` と書く)。型指定 show の呼び出し側では結果型を書く必要はなく、`show(ConfirmViewModel::class)` の結果型は ViewModel の宣言から決まる。

### 型指定 show と VM factory (共有 Kotlin コード専用)

ViewModel のクラス参照 (`KClass`) を渡して表示する経路。共有コードのレジストリに登録した VM factory で ViewModel を作り、configure (省略可・`suspend`) の完了後に既存のインスタンス渡し show へ流す。VM factory は表示のたびに呼ばれ、同じ型への再登録は後勝ち、解決は呼び出し時点のスナップショットによる。

```kotlin
// 起動時 (共有コード): VM factory の登録
Dialog.instance.registry.registerViewModel(ConfirmViewModel::class) { ConfirmViewModel() }

// 呼び出し元 (共有コード): 型指定 show
val result = Dialog.instance.show(ConfirmViewModel::class) { vm ->
    vm.message = "保存しますか?"
}
```

この経路で共有コードが持つ決まりは次のとおりで、根拠は [kmp/ADR-0006](../../../decisions/kmp/0006-common-vm-factory-typed-show.md)。

| 観点 | 共有コードでの扱い |
|---|---|
| VM factory と configure の実行文脈 | 呼び出し元のコルーチン文脈でそのまま実行し、UI スレッドへは移さない。UI スレッドが要る処理は各 OS 側の View factory で行う |
| VM factory の生成物 | 登録キーと同じクラスの ViewModel を返す。サブクラスを返すと型不一致の構成ミスとして `DialogException` で失敗し、表示は行われない |
| 未登録 | VM factory 未登録は構成ミスとして `DialogException` で失敗し、表示は行われない (説明文で型不一致と区別できる) |
| VM factory / configure の例外 | 提示に進まず、その例外がそのまま呼び出し元へ伝播する (`CancellationException` を含む) |
| 登録先 | 共有コードから型指定 show するなら VM factory は共有コードで登録する。Android Native の `registerViewModel` に登録したものは共有コードの型指定 show から見えない |
| Swift / ObjC からの可視性 | 型指定 show と `registerViewModel` は Swift / ObjC から見えない (framework の ObjC ヘッダに現れない)。インスタンス渡し show と `registry` プロパティは見える |

Swift 向け KMP 面 (`Dialog.shared.kmp`) に型指定 show は無い。iOS ホストから共有 VM を出すときは、Kotlin の VM を Swift 側で作ってインスタンス渡し show する ([KMP 利用者の iOS ホスト統合](ios-host-integration.md))。

### 共有コードには添付の面が無い

器 (ダイアログの外枠) の静的な属性・出入りの演出を中身に**添付する (View の定義側に結び付ける) 面は共有コードに存在しない** — 中身は各 OS 側の View として組み立てられるため、添付も各 OS の面で行う。共有コードから渡せるのは show の引数の `DialogPlacement` だけである。

添付の書き方は各 OS の公開面が持つ:

- レイアウト属性: [iOS のレイアウト公開面](../../ios/api/layout-surface.md) / [Android のレイアウト公開面](../../android/api/layout-surface.md)
- 出入りの演出: [iOS のトランジション公開面](../../ios/api/transition-surface.md) / [Android のトランジション公開面](../../android/api/transition-surface.md)

KMP から登録・表示した中身も各 OS では通常の View として組み立てられるので、その中身に対して各 OS の面で添付すれば同じように効く。

## Android ホスト側の見え方

共有 ViewModel は Android Native 型の typealias なので、**Android Native の面がそのまま使える**。View factory の登録も結果報告口の取得 (`vm.notifier`) も Android Native と同じ書き方で、KMP 専用の追加 API はない。詳しくは [Android の Dialog 公開面](../../android/api/dialog-surface.md) を読む。

Android Native のレジストリにも VM factory のスロットがあるが、それは Android Native の型指定 show 用で、共有コードの型指定 show は共有コード側の表だけを引く。共有コードから型指定 show する VM の factory は共有コードで登録する。

## iOS ホスト側の見え方

共有 ViewModel は iOS Native の ViewModel 契約に準拠しないため、iOS 側には KMP 専用の型付き入口 `Dialog.shared.kmp` がある。結果型は ViewModel から導出できないので `result:` ラベルの引数で受け取り、省略すると真偽値になる。登録・表示・結果報告口の取得の署名と手順は [KMP 利用者の iOS ホスト統合](ios-host-integration.md) の「Swift 側で登録するもの」が定める。

## 失敗とキャンセルの届き方

show は `suspend` 関数で、結果は `DialogResult` の sealed interface として返る。構成ミスは結果ではなく `DialogException` で投げられる。共有コードの `DialogException` はサブクラスを持たないので、共有コード側で種別を型で判別することはできない — 原因は説明文 (英語) がメッセージとして届くだけである。各 OS の Native ライブラリで起きた失敗はその説明文が素通しで届き、共有コードの型指定 show の失敗 (VM factory 未登録・型不一致) は共有コードが説明文を組み立てる。コンストラクタは internal なので、利用者は catch はできるが自分で構築はできない (アプリ側の共有コードのテストでは `DialogException` を投げるダブルは書けない)。

呼び出し元のコルーチンをキャンセルしたときは、コルーチン規約どおり `CancellationException` が伝播する (内部の結果は cancelled で確定済み)。これは共有コードから iOS 上で表示した場合も同じである。

### Swift 境界の `@Throws`

構成ミスが Swift 側へ NSError で届くのは、ライブラリの公開面を Swift から直接呼ぶ場合である。公開面は失敗しうる経路にだけ `@Throws` を宣言している — Dialog ではインスタンス渡しの `KsDialog.show` がそれに当たる (KsDialogs の他機能である Loading / Toast の該当メンバは、それぞれの公開面が定める)。型指定 show は Swift から見えないため `@Throws` を宣言せず、Kotlin 内では例外がそのまま伝播する。

アプリ開発者が共有コードで show を包んだ関数を自分で書いて Swift から呼ぶときは、suspend でも非 suspend でも、その関数に同じ `@Throws` 宣言が要る。宣言が無いと Kotlin/Native は例外を NSError に変換しない。結末は 2 通りで、suspend の関数では未処理例外としてプロセスが終了し、非 suspend の関数では例外が Swift 側へ一切伝播しない。

根拠は [kmp/ADR-0001](../../../decisions/kmp/0001-swift-interop-plain-suspend.md)。KMP Sample で実際にこのクラッシュが起きたときの調査メモが [fix-kmp-ios-unhandled-exception-crash](../../../changes/archive/2026-09-02-fix-kmp-ios-unhandled-exception-crash/exploration.md)、ライブラリ側で宣言を徹底したときの調査メモが [add-kmp-loading-toast-throws](../../../changes/archive/2026-09-02-add-kmp-loading-toast-throws/exploration.md) にある (いずれも過去の変更の作業記録)。

共有モジュールの iosMain で commonMain の interface が `@Throws` を宣言したメンバを override するときの注意は [KMP 利用者の iOS ホスト統合](ios-host-integration.md) の「してはいけないこと」が定める。

## 関連

- [KMP 利用者の iOS ホスト統合](ios-host-integration.md) — Swift 向けの型付き公開面と iOS アプリ側の依存経路
- [Android の Dialog 公開面](../../android/api/dialog-surface.md) — Android ホスト側で書く名前
- [KMP の Loading 公開面](loading-surface.md) / [KMP の Toast 公開面](toast-surface.md) — 同じ形の型指定 show と VM factory の登録を持つ他機能
- [kmp/ADR-0002](../../../decisions/kmp/0002-thin-facade-native-registry.md) — 決定 (薄い facade と View レジストリの Native 委譲)
- [kmp/ADR-0006](../../../decisions/kmp/0006-common-vm-factory-typed-show.md) — 決定 (共有コードの型指定 show は共有コード側の VM factory 表で解決する。kmp/ADR-0002 の一部改訂)

Swift 向けの登録・表示を Swift パッケージ側の型付き公開面に置く決定は [kmp/ADR-0003](../../../decisions/kmp/0003-swift-facing-registration-in-swift-package.md)・[kmp/ADR-0004](../../../decisions/kmp/0004-swift-facing-typed-generic-facade.md) が持つ。
