---
name: ksdialogs-android
description: Android View または Jetpack Compose で、型付き結果・レイアウト・トランジションを備えた KsDialogs の Dialog、Loading、Toast を実装する。
license: MIT
metadata:
  language: ja
  source: https://github.com/kamusoft/KsDialogs
---

# Android 向け KsDialogs

KsDialogs は、アプリのどこからでも Dialog を呼び出せる UI ライブラリ。中身は自分で書いた View を登録しておくか呼び出し時に渡し、呼び出し側は表示を頼んで結果を待つだけでよい。表示できるのは 3 種類 — 利用者の応答を受け取る Dialog、処理中の操作をブロックする Loading、非対話の通知を出す Toast。この Skill が扱うのは Android 版で、通常は既定エントリ (`Dialog.instance`、`Loading.instance`、`Toast.instance`) から呼ぶ。テストやアプリの dependency injection では、`Dialog()`、`Loading()`、`Toast()` を実体とする `KsDialog`、`KsLoading`、`KsToast` 契約を注入できる。

既定エントリから呼んでも契約を注入して呼んでも、届く先はプロセス内の同じ状態である。View の登録先は 1 種類につき 1 つで、`Dialog.instance.registry` は `DialogViewRegistry.shared`、`Loading.instance.registry` は `LoadingViewRegistry.shared`、`Toast.instance.registry` は `ToastViewRegistry.shared` を指す。既定エントリに設定した style や器の options も同じように共有される。content は Android View でも Jetpack Compose でも書けるが、登録先が UI 技術ごとに分かれることはなく、`register` と `registerCompose` はどちらも同じレジストリに入る。どのレジストリでも 1 つの ViewModel 型のエントリは View factory と ViewModel factory の 2 スロットからなり、3 種類とも ViewModel の型だけを渡す `show` を持つ。

## 能力マップ

| やりたいこと | API | レシピ |
|---|---|---|
| Dialog を登録して表示する | `DialogViewModel`、`register`、`registerCompose`、`Dialog.instance.show` | [Dialog](references/dialogs.md) |
| 登録漏れ・提示先不在から復帰する | `DialogException` | [Dialog](references/dialogs.md) |
| ViewModel から結果を報告する | `notifier` | [ViewModel](references/view-models.md) |
| 型だけを渡して ViewModel を生成させる | `registerViewModel`、型指定 `show` / `start` | [ViewModel](references/view-models.md) |
| サイズ・配置・覆い・外側タップを制御する | `DialogOptions`、`DialogPlacement`、View プロパティ、`KsDialogAttributes` | [レイアウト](references/layout.md) |
| 出現と退出を演出する | `DialogTransition`、`ksDialogTransition`、`KsDialogAttributes` | [トランジション](references/transitions.md) |
| 処理中の操作をブロックする | `Loading.instance`、`LoadingStyle`、`LoadingProgressReceiver` | [Loading](references/loading.md) |
| fire-and-forget の通知を表示する | `Toast.instance`、`ToastStyle`、`ToastViewRegistry` | [Toast](references/toast.md) |

## セットアップ

minSdk 24 以降の View-only Android アプリでは、`<version>` を release version に置き換え、基本 artifact だけを宣言する。

```kotlin
dependencies {
    implementation("jp.kamusoft:ksdialogs:<version>")
}
```

Compose アプリでは Compose artifact だけを宣言する。基本 artifact は推移依存で解決される。以下のレシピでは、ライブラリの Compose 1.8.1 系と一致する Compose BOM 2025.05.00、および Lifecycle 2.8.7 と Coroutines 1.11.0 を使う。

```kotlin
dependencies {
    implementation("jp.kamusoft:ksdialogs-compose:<version>")
    val composeBom = platform("androidx.compose:compose-bom:2025.05.00")
    implementation(composeBom)
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
}
```

Compose content を含むモジュールでは Compose を有効化し、Kotlin と同じバージョンの Compose compiler plugin を適用する。Kotlin 2.4.10 の場合、該当するビルド設定は次のとおりである。

```kotlin
plugins {
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.10"
}

android {
    buildFeatures {
        compose = true
    }
}
```

ライブラリが resumed `Activity` を自動追跡するため、アプリ側の初期化は不要である。API は `jp.kamusoft.ksdialogs` から、Compose 拡張は `jp.kamusoft.ksdialogs.compose` から import する。

## 最小例

```kotlin
import jp.kamusoft.ksdialogs.Toast

fun notifySaved() {
    Toast.instance.show("Saved")
}
```

## レシピを選ぶ

| やりたいこと | 読むレシピ |
|---|---|
| 登録、型付き結果、インラインコンテンツ、多段表示、`DialogException` | [Dialog](references/dialogs.md) |
| `notifier`、ViewModel factory、表示前の設定、3 機能で同型の型指定 `show` | [ViewModel](references/view-models.md) |
| 配置、余白、比率サイズ、覆い、外側タップキャンセル | [レイアウト](references/layout.md) |
| プリセット、独自の非同期フック、退出後の結果配送 | [トランジション](references/transitions.md) |
| 命令形・スコープ形 Loading、進捗、style、カスタムコンテンツ、型指定 `show` / `start` | [Loading](references/loading.md) |
| message、登録、インライン、型指定の Toast 経路 | [Toast](references/toast.md) |
