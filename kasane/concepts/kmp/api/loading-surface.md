---
type: concept
title: KMP の Loading 公開面
description: KMP の共有コード (commonMain) から Loading を使うときの公開名と署名 — 既定エントリと 4 操作・型指定 show / start と VM factory の登録 (共有 Kotlin コード専用)・進捗報告口と進捗受け口・styling と器メタ属性がこの面に無いこと・カスタム View の登録は各 OS 側であること・Swift 境界の @Throws
tags: [kmp, loading, api, surface]
timestamp: 2026-09-07
---

# KMP の Loading 公開面

この文書を読むと、KMP の共有コード (commonMain) から Loading を表示するときに書く名前と署名、各 OS のホスト側で何を書くか、Swift から呼ぶときの失敗の届き方が分かる。

**この文書は KMP の公開面 (名前・署名・コード例・framework 固有の注意) だけを扱い、挙動の契約は core が正である。** 対応する契約は [Loading のルール](../../core/api/loading-semantics.md) で、合流の数え方・世代・器の性質・保証と禁止はそちらを読む。型指定の形の共通の決まりは [ViewModel 主導の呼び出しのルール](../../core/api/model-binding-semantics.md) が持つ。

## 共有コード (commonMain) の公開面

| 用途 | 書く名前 |
|---|---|
| 既定の表示エントリ | `Loading.instance` (`expect object Loading` の唯一のメンバ) |
| DI で注入する契約 | `KsLoading` |
| レジストリのハンドル | `Loading.instance.registry` (型は `LoadingViewRegistry`) |
| 既定ローディングの表示 | `show(message, placement)` (`suspend`) |
| 登録済みカスタム View の表示 (インスタンス渡し) | `show(viewModel, placement)` (`suspend`) |
| 登録済みカスタム View の表示 (型指定) | `show(viewModelClass, placement, configure)` (`suspend`。共有 Kotlin コード専用) |
| 閉鎖 | `hide()` (`suspend`) |
| メッセージの差し替え | `setMessage(message)` (`suspend`) |
| スコープ形 | `start(message, placement, action)` / `start(viewModel, placement, action)` / `start(viewModelClass, placement, configure, action)` (`suspend`。型指定版は共有 Kotlin コード専用) |
| VM factory の登録 | `registry.registerViewModel(viewModelClass, factory)` (共有 Kotlin コード専用) |

進捗報告口は `(Double) -> Unit` の関数で、スコープ形の処理に渡る。カスタム Loading の ViewModel は `LoadingViewModel` (expect interface)、進捗を受け取る面は `LoadingProgressReceiver` (expect interface。`onProgress(progress: Double)`) である。

```kotlin
val uploaded = Loading.instance.start(UploadViewModel()) { report ->
    upload(onProgress = report)
}
```

### 型指定 show / start と VM factory (共有 Kotlin コード専用)

ViewModel のクラス参照を渡す形。共有コードのレジストリに登録した VM factory で ViewModel を作り、configure (省略可・`suspend`) の完了後に既存のインスタンス渡しの show / start へ流す。引数の並びは Android Native の型指定 show / start と同じで、`start` は処理の戻り値をそのまま返す。

```kotlin
// 起動時 (共有コード)
Loading.instance.registry.registerViewModel(UploadViewModel::class) { UploadViewModel() }

// 呼び出し元 (共有コード)
val uploaded = Loading.instance.start(UploadViewModel::class, configure = { it.title = "アップロード中" }) { report ->
    upload(onProgress = report)
}
```

VM factory と configure は呼び出し元のコルーチン文脈で実行され、UI スレッドへは移らない。VM factory 未登録と、生成物が登録キーと違うクラスである型不一致は構成ミスとして `DialogException` で失敗し、表示にも合流にも進まない。VM factory / configure の例外もそのまま呼び出し元へ伝播し、スコープ形では処理ブロックを実行しない。合流の判定はインスタンス渡しと同じ時点で行われる ([Loading のルール](../../core/api/loading-semantics.md) の「型指定の形と合流」)。型指定 show / start と `registerViewModel` は Swift / ObjC から見えない。決まりの全体と根拠 ([kmp/ADR-0006](../../../decisions/kmp/0006-common-vm-factory-typed-show.md)) は [KMP の Dialog 公開面](dialog-surface.md) の「型指定 show と VM factory」の表と同じである。

### この面に無いもの

- **styling と器メタ属性の設定 API が無い** — 色型が境界を渡らないため、各 OS 側の設定プロパティで行う ([iOS](../../ios/api/loading-surface.md) / [Android](../../android/api/loading-surface.md) の Loading 公開面)
- **インライン factory 版の表示が無い** — 共有コードは View の型に触れないため、渡せるのは登録済みの ViewModel かそのクラス参照だけである
- **View factory の登録 API が無い** — `LoadingViewRegistry` が共有コードで持つのは VM factory の登録口だけで、中身の View の登録は各 OS 側で行う (次節)

## カスタム View の登録は各 OS 側

共有コードで定義した ViewModel の型をキーに、中身の View を各 OS のホスト側で登録する。

- **Android ホスト側**: 共有 ViewModel は Android Native 型の typealias なので、Android Native の登録面がそのまま使える ([Android の Loading 公開面](../../android/api/loading-surface.md))
- **iOS ホスト側**: Swift 側の KMP 専用の型付き入口 `Loading.shared.kmp` で登録する。署名と手順は [KMP 利用者の iOS ホスト統合](ios-host-integration.md) の「Swift 側で登録するもの」が定める

Android Native のレジストリにも VM factory のスロットがあるが、それは Android Native の型指定 show 用で、共有コードの型指定 show からは見えない。共有コードから型指定 show する VM の factory は共有コードで登録する。

## 失敗の届き方と Swift 境界の `@Throws`

未登録の ViewModel 型で表示すると構成ミスとして `DialogException` が投げられ、表示は行われない。共有コードの `DialogException` はサブクラスを持たないので、原因は説明文 (英語) がメッセージとして届くだけである — 各 OS の Native ライブラリで起きた失敗は素通しで、共有コードの型指定経路の失敗 (VM factory 未登録・型不一致) は共有コードが説明文を組み立てる。

公開面は**失敗しうる経路にだけ `@Throws` を宣言している** — Loading では ViewModel を渡す `show` と `start` がそれに当たり、既定ローディングの `show` / `hide` / `setMessage` と、処理だけを渡すスコープ形には付かない。この宣言により、Swift から直接呼んだときに構成ミスが NSError として届く ([kmp/ADR-0001](../../../decisions/kmp/0001-swift-interop-plain-suspend.md))。型指定 show / start は Swift から見えないため `@Throws` を宣言しない。

共有モジュールの iosMain でこの契約を差し替えるときの注意 (override に `@Throws` を書かない) は [KMP 利用者の iOS ホスト統合](ios-host-integration.md) の「してはいけないこと」が定める。

## 関連

- [Loading のルール](../../core/api/loading-semantics.md) — 合流・世代・器の性質・保証と禁止 (契約の正)
- [ViewModel 主導の呼び出しのルール](../../core/api/model-binding-semantics.md) — 型指定 show の共通の決まりと KMP での見え方
- [KMP 利用者の iOS ホスト統合](ios-host-integration.md) — Swift 向けの型付き公開面と登録手順
- [KMP の Dialog 公開面](dialog-surface.md) — 共有コードの呼び出し面の基本形・型指定 show の決まりの表・添付の面が無いこと
- [KMP の Toast 公開面](toast-surface.md) — もう 1 つの非ダイアログ表示の公開面
- [kmp/ADR-0002](../../../decisions/kmp/0002-thin-facade-native-registry.md) — 決定 (薄い facade と View レジストリの Native 委譲)
- [kmp/ADR-0006](../../../decisions/kmp/0006-common-vm-factory-typed-show.md) — 決定 (共有コードの型指定 show と VM factory 表の置き場)
