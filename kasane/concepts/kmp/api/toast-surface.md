---
type: concept
title: KMP の Toast 公開面
description: KMP の共有コード (commonMain) から Toast を使うときの公開名と署名 — 既定エントリと 3 経路の show・durationMs 引数・型指定 show と VM factory の登録 (共有 Kotlin コード専用。失敗は同期に伝播)・一括設定がこの面に無いこと・カスタム View の登録は各 OS 側であること・Swift 境界の @Throws
tags: [kmp, toast, api, surface]
timestamp: 2026-09-07
---

# KMP の Toast 公開面

この文書を読むと、KMP の共有コード (commonMain) から Toast を表示するときに書く名前と署名、各 OS のホスト側で何を書くか、Swift から呼ぶときの失敗の届き方が分かる。

**この文書は KMP の公開面 (名前・署名・コード例・framework 固有の注意) だけを扱い、挙動の契約は core が正である。** 対応する契約は [Toast のルール](../../core/api/toast-semantics.md) で、duration の時間モデル・失敗モデル・多重表示・非対話の保証はそちらを読む。型指定経路の共通の決まりは [ViewModel 主導の呼び出しのルール](../../core/api/model-binding-semantics.md) が持つ。

## 共有コード (commonMain) の公開面

| 用途 | 書く名前 |
|---|---|
| 既定の表示エントリ | `Toast.instance` (`expect object Toast` の唯一のメンバ) |
| DI で注入する契約 | `KsToast` |
| レジストリのハンドル | `Toast.instance.registry` (型は `ToastViewRegistry`) |
| メッセージ入口 | `show(message, durationMs, placement)` |
| 登録経路 (インスタンス渡し) | `show(viewModel, durationMs, placement)` |
| 登録経路 (型指定) | `show(viewModelClass, durationMs, placement, configure)` (共有 Kotlin コード専用) |
| VM factory の登録 | `registry.registerViewModel(viewModelClass, factory)` (共有 Kotlin コード専用) |

**表示時間の引数名は `durationMs`** (`Int?`、ミリ秒) である。プラットフォーム固有の時間型を使わないのは、共有コードから呼べるようにするためである。同じ値を Swift 側の KMP 入口は `duration:` ラベルで受ける ([KMP 利用者の iOS ホスト統合](ios-host-integration.md))。`placement` は `DialogPlacement?`。show はどの経路も `suspend` ではない同期の関数で、戻り値を持たない。カスタム Toast の ViewModel は `ToastViewModel` (expect interface) である。

```kotlin
Toast.instance.show("保存しました", durationMs = 2000)
Toast.instance.show(NoticeViewModel("同期が完了しました"))
```

### 型指定 show と VM factory (共有 Kotlin コード専用)

ViewModel のクラス参照を渡す形。共有コードのレジストリに登録した VM factory で ViewModel を作り、configure (省略可・**同期のみ**) を適用してから既存のインスタンス渡し show へ流す。引数の並びは Android Native の型指定 show と同じである。

```kotlin
// 起動時 (共有コード)
Toast.instance.registry.registerViewModel(NoticeViewModel::class) { NoticeViewModel() }

// 呼び出し元 (共有コード)
Toast.instance.show(NoticeViewModel::class, durationMs = 2000) { it.text = "同期が完了しました" }
```

VM factory と configure は**呼び出しスレッドで実行される**ため、Native の Toast と違って **VM factory / configure の例外も show から同期に呼び出し元へ伝播する** (受理後の失敗にならない)。VM factory 未登録と型不一致 (生成物が登録キーと違うクラス) も同期の `DialogException` で、いずれも表示は行われない。型指定 show と `registerViewModel` は Swift / ObjC から見えない。決まりの全体と根拠 ([kmp/ADR-0006](../../../decisions/kmp/0006-common-vm-factory-typed-show.md)) は [KMP の Dialog 公開面](dialog-surface.md) の「型指定 show と VM factory」の表と同じである。

### この面に無いもの

- **一括設定 (見た目とアプリ既定配置) の API が無い** — 色型が OS 共通コードの境界を渡らないため、各 OS 側の設定プロパティで行う ([iOS の Toast 公開面](../../ios/api/toast-surface.md) / [Android の Toast 公開面](../../android/api/toast-surface.md))
- **インライン factory 版の表示と View factory の登録 API が無い** — 共有コードは View の型に触れないため、渡せるのは登録済みの ViewModel かそのクラス参照だけで、`ToastViewRegistry` が共有コードで持つのは VM factory の登録口だけである (次節)
- **`hide` に相当する操作が無い** — Loading と違い、閉じる操作もメッセージ更新もスコープ形も進捗の報告口も Toast の契約には無い ([KMP の Loading 公開面](loading-surface.md))

## カスタム View の登録は各 OS 側

共有コードで定義した ViewModel の型をキーに、中身の View を各 OS のホスト側で登録する。

- **Android ホスト側**: 共有 ViewModel は Android Native 型の typealias なので、Android Native の登録面がそのまま使える ([Android の Toast 公開面](../../android/api/toast-surface.md))
- **iOS ホスト側**: Swift 側の KMP 専用の型付き入口 `Toast.shared.kmp` で登録する。署名と手順は [KMP 利用者の iOS ホスト統合](ios-host-integration.md) の「Swift 側で登録するもの」が定める

Android Native のレジストリにも VM factory のスロットがあるが、それは Android Native の型指定 show 用で、共有コードの型指定 show からは見えない。共有コードから型指定 show する VM の factory は共有コードで登録する。

## 失敗の届き方と Swift 境界の `@Throws`

未登録の ViewModel 型で表示すると構成ミスとして `DialogException` が投げられ、表示は行われない。共有コードの `DialogException` はサブクラスを持たないので、原因は説明文 (英語) がメッセージとして届くだけである — 各 OS の Native ライブラリで起きた失敗は素通しで、共有コードの型指定経路の失敗 (VM factory 未登録・型不一致) は共有コードが説明文を組み立てる。

公開面は**失敗しうる経路にだけ `@Throws` を宣言している** — Toast では ViewModel を渡す `show` がそれに当たり、メッセージ入口には付かない。この宣言により、Swift から直接呼んだときに構成ミスが NSError として届く ([kmp/ADR-0001](../../../decisions/kmp/0001-swift-interop-plain-suspend.md))。インスタンス渡しの経路で受理後に起きた失敗 (View factory の例外など) は show が既に戻っているため呼び出し元へは返らない。型指定 show は Swift から見えないため `@Throws` を宣言しない。

共有モジュールの iosMain でこの契約を差し替えるときの注意 (override に `@Throws` を書かない) は [KMP 利用者の iOS ホスト統合](ios-host-integration.md) の「してはいけないこと」が定める。

## 関連

- [Toast のルール](../../core/api/toast-semantics.md) — duration・失敗モデル・多重表示・非対話 (契約の正)
- [ViewModel 主導の呼び出しのルール](../../core/api/model-binding-semantics.md) — 型指定 show の共通の決まりと KMP での見え方 (Toast の同期伝播)
- [KMP 利用者の iOS ホスト統合](ios-host-integration.md) — Swift 向けの型付き公開面と登録手順
- [KMP の Loading 公開面](loading-surface.md) — 操作の多い側との対比
- [KMP の Dialog 公開面](dialog-surface.md) — 共有コードの呼び出し面の基本形・型指定 show の決まりの表・添付の面が無いこと
- [kmp/ADR-0006](../../../decisions/kmp/0006-common-vm-factory-typed-show.md) — 決定 (共有コードの型指定 show と VM factory 表の置き場)
