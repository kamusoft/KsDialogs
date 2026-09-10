---
name: ksdialogs-kmp
description: KsDialogs の Kotlin Multiplatform (KMP) 向け。commonMain から Dialog・Loading・Toast を呼び、Android / iOS host で View を登録する。
license: MIT
metadata:
  language: ja
  source: https://github.com/kamusoft/KsDialogs
---

# Kotlin Multiplatform 向け KsDialogs

KsDialogs は、アプリのどこからでも Dialog を呼び出せる UI ライブラリ。中身は自分で書いた View を登録しておき、呼び出し側は表示を頼んで結果を待つだけでよい。表示できるのは 3 種類 — 利用者の応答を受け取る Dialog、処理中の操作をブロックする Loading、非対話の通知を出す Toast。この Skill が扱うのは Kotlin Multiplatform 版で、呼び出しは共有 Kotlin コードに書き、中身は各 host が Native の content として供給する。共有コードは `Dialog.instance`・`Loading.instance`・`Toast.instance` を呼ぶか、`KsDialog`・`KsLoading`・`KsToast` を DI で受け取る。

関わるのは 3 側である:

| 側 | そこに書くもの |
|---|---|
| 共有コード (`commonMain`) | ViewModel の class・結果型・`show` / `start` の呼び出し・呼び出しごとの `DialogPlacement` |
| Android host | 共有 ViewModel class に対する Android View / Compose の content、添付する option と transition |
| iOS host | Swift package の KMP 入口に登録する UIKit / SwiftUI の content、添付する option と transition |

content の型が OS ごとに違うため、content の登録は host 側にある。どちらの host も純 Native 利用者と同じ Native レジストリへ書き込むので、共有コードと Native コードは同じ factory を引く。共有コードのレジストリが持つのは ViewModel の作り方 (ViewModel factory) の登録だけである。既定エントリから呼んでも DI で受け取った契約から呼んでも、届く先はこの同じレジストリである。

## 能力マップ

| 目的 | どこに書くか | レシピ |
|---|---|---|
| 登録済みの Dialog を表示して結果を待つ | `DialogViewModel<R>`, `Dialog.instance.show`, `DialogResult` | [Dialog](references/dialogs.md) |
| 既定エントリの代わりに共有契約を注入する | `KsDialog`, `KsLoading`, `KsToast` | [ViewModel](references/view-models.md) |
| ViewModel の class を渡して表示し、instance の生成をライブラリに任せる | `registry.registerViewModel`, `show(VM::class)` | [ViewModel](references/view-models.md) |
| 1 回の呼び出しだけ配置を上書きする | `DialogPlacement`, `DialogAlignment` | [レイアウト](references/layout.md) |
| 大きさ・覆い・外側タップの option を content に添付する | host の `DialogOptions`, `ksDialogOptions`, `KsDialogAttributes` | [レイアウト](references/layout.md) |
| 出入りの演出を content に添付する | host の `DialogTransition`, `ksDialogTransition` | [トランジション](references/transitions.md) |
| 共有処理の実行中に操作をブロックする | `Loading.instance`, `LoadingViewModel`, `LoadingProgressReceiver` | [Loading](references/loading.md) |
| 非対話の通知を表示する | `Toast.instance`, `ToastViewModel` | [Toast](references/toast.md) |
| Android View / Compose の content を登録する | `Dialog.instance.registry`, `registerCompose` | [Android host](references/android-host.md) |
| UIKit / SwiftUI の content を登録する | `Dialog.shared.kmp`, `Loading.shared.kmp`, `Toast.shared.kmp` | [iOS host](references/ios-host.md) |

## セットアップ

現行 artifact のビルド環境は Kotlin 2.4.10 と Gradle 9.7.0。Android target は API 24 以降、iOS target は iOS 17 以降と Swift tools 6.3 が必要になる。利用側の Kotlin Gradle Plugin は同じ minor 系列 (2.4.x) をサポートし、動作確認済みの版は 2.4.10 である。iOS のリンク情報を運ぶ SwiftPM import は Kotlin 2.4 の Alpha 機能なので、これより広い範囲は約束しない。

`jp.kamusoft:ksdialogs-kmp` は Maven Central へ公開されており、iOS host がリンクする Swift package は配信リポジトリ `https://github.com/kamusoft/KsDialogs-SPM` (package identity は `KsDialogs-SPM`、product は `KsDialogs`) に公開されている。どちらも同じ 1 つの version 文字列で出るため、共有 module の Maven 依存と Xcode の package 参照には同じ version を書く。以下の例に載る version は公開済みのものである。

### 共有 module

共有 module の `build.gradle.kts` で `commonMain` へ Maven 依存を 1 点追加する。artifact metadata が iOS 向け Swift package のリンク情報を運ぶため、利用側の build で KsDialogs の `swiftPMDependencies` を再宣言しない。

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            api("jp.kamusoft:ksdialogs-kmp:0.1.0-beta.1")
        }
    }
}
```

`implementation` ではなく `api` で宣言する。共有 ViewModel は `DialogViewModel` を継承するため KsDialogs の型が共有 module 自身の公開面に現れ、Android host の登録コードからも見える必要がある。

### Android host

KMP artifact から Android View 系 artifact `jp.kamusoft:ksdialogs-core` が推移的に届くため、Android アプリは `Dialog.instance.registry`・`Loading.instance.registry`・`Toast.instance.registry` へ content を登録する。Compose 系 artifact `jp.kamusoft:ksdialogs` は推移では届かないため、content を Compose で書くなら Android アプリ側で足す。詳しくは [Android host](references/android-host.md) を読む。

### iOS host

前提: Xcode project から共有 module の framework をビルド・リンクする標準の KMP iOS 連携を済ませる。

1. 上記の Maven 依存 `jp.kamusoft:ksdialogs-kmp:0.1.0-beta.1` 1 点を共有 module に追加する。
2. Xcode project のパスを渡して `integrateLinkagePackage` を 1 回実行し、生成された `KotlinMultiplatformLinkedPackage/` を VCS に含める。
3. Xcode の Package Dependencies に `https://github.com/kamusoft/KsDialogs-SPM` を同じ version の exact 指定で追加し、product `KsDialogs` をアプリ target にリンクする。

```bash
XCODEPROJ_PATH="$PWD/iosApp/MyApp.xcodeproj" \
  ./gradlew :shared:integrateLinkagePackage
```

Gradle で SwiftPM 依存を再宣言しない。公開 Maven metadata がすでに推移情報を運ぶ。Xcode への直接追加は、Swift host コードから型付き登録 API を呼ぶためだけに必要になる。詳しくは [iOS host](references/ios-host.md) を読む。

Swift から呼ぶ共有コードの関数は、`suspend` でも非 `suspend` でも `@Throws` が要る。ライブラリ自身は ViewModel の解決で失敗しうる経路にだけ宣言している — Dialog の `show`、カスタム Loading の `show` と `start`、カスタム Toast の `show` で、message 経路には付かない。宣言がないと Kotlin/Native は例外を `NSError` へ変換せず、`suspend` の関数では未処理例外でプロセスが終了し、非 `suspend` の関数では Swift 側へ何も伝わらない。

## 最小例

```kotlin
import jp.kamusoft.ksdialogs.kmp.Dialog
import jp.kamusoft.ksdialogs.kmp.DialogException
import jp.kamusoft.ksdialogs.kmp.DialogResult
import jp.kamusoft.ksdialogs.kmp.DialogViewModel
import kotlin.coroutines.cancellation.CancellationException

class ConfirmViewModel(val message: String) : DialogViewModel<Boolean>

@Throws(DialogException::class, CancellationException::class)
suspend fun showConfirmation(message: String): DialogResult<Boolean> =
    Dialog.instance.show(ConfirmViewModel(message))
```

共有コードには、Native にある真偽値の省略形 (結果型を書かない ViewModel の別名) が無い。ViewModel の宣言では結果型を常に明示し、真偽値なら `DialogViewModel<Boolean>` と書く。`showConfirmation` を呼ぶ前に、各 host で `ConfirmViewModel` の content を 1 回登録する。

## レシピを選ぶ

| やりたいこと | 読むレシピ |
|---|---|
| 型付き結果・失敗・キャンセル・多段表示 | [Dialog](references/dialogs.md) |
| 契約の注入・ViewModel factory の登録・host 側の結果報告 | [ViewModel](references/view-models.md) |
| 呼び出しごとの配置と host 側の option 添付 | [レイアウト](references/layout.md) |
| 演出の選択を host へ運ぶ方法 | [トランジション](references/transitions.md) |
| 命令形・スコープ形の Loading と進捗 | [Loading](references/loading.md) |
| message 経路と登録済みカスタム経路 | [Toast](references/toast.md) |
| Android の Dialog / Loading / Toast の登録 | [Android host](references/android-host.md) |
| iOS の登録・結果型・Swift への例外変換 | [iOS host](references/ios-host.md) |
