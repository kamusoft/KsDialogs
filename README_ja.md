# KsDialogs

> **配信準備中:** 各 package は初回の一般公開に向けて準備中です。

## 概要と主な特徴

KsDialogs は、現在の View 階層へ組み込まずにアプリケーションのどこからでも UI を表示できるダイアログライブラリです。iOS と Android の Native 実装を土台とし、.NET MAUI と Kotlin Multiplatform には薄い wrapper を提供します。

- 型付き Dialog を表示し、その結果を受け取れます。
- 進捗更新に対応した Loading で処理中の操作をブロックできます。
- タッチを素通しする非対話の Toast 通知を表示できます。
- Native View と、SwiftUI や Jetpack Compose などの宣言的 UI を利用できます。

version 0.x の間は、公開 API に破壊的変更が入る可能性があります。

## スクリーンショット

| iOS | Android |
|---|---|
| **Dialog**<br>![iOS Sample の基本ダイアログ](https://raw.githubusercontent.com/kamusoft/KsDialogs/develop/assets/ios-dialog.png) | **Dialog**<br>![Android Sample の基本ダイアログ](https://raw.githubusercontent.com/kamusoft/KsDialogs/develop/assets/android-dialog.png) |
| **Loading**<br>![iOS Sample の50パーセントの Loading](https://raw.githubusercontent.com/kamusoft/KsDialogs/develop/assets/ios-loading.png) | **Loading**<br>![Android Sample の50パーセントの Loading](https://raw.githubusercontent.com/kamusoft/KsDialogs/develop/assets/android-loading.png) |
| **Toast**<br>![iOS Sample の3枚の Toast 通知](https://raw.githubusercontent.com/kamusoft/KsDialogs/develop/assets/ios-toast.png) | **Toast**<br>![Android Sample の3枚の Toast 通知](https://raw.githubusercontent.com/kamusoft/KsDialogs/develop/assets/android-toast.png) |

.NET MAUI と Kotlin Multiplatform は Native 実装を wrap するため、同じ画面になります。

## 対応プラットフォーム

| 形態 | 最小 OS | ライブラリのビルドに使った toolchain |
|---|---|---|
| iOS Native | iOS 17 | Swift 6.3 |
| Android Native | Android 7.0 (API 24) | Kotlin 2.4.10, AGP 9.3.0, Gradle 9.7.0 |
| .NET MAUI | iOS 17 / Android 7.0 (API 24) | .NET 10 (`net10.0`), Microsoft.Maui.Controls 10.0.1 |
| Kotlin Multiplatform | iOS 17 / Android 7.0 (API 24) | Kotlin 2.4.10, AGP 9.3.0, Gradle 9.7.0, Swift 6.3 |

Android target は minSdk 24、compileSdk 36 です。.NET MAUI の利用側下限は Microsoft.Maui.Controls 10.0.1 です。Android Native と Kotlin Multiplatform の利用側 Kotlin 最小 version は確定前で、初回リリースまでに確定します。表の version はライブラリのビルドに使ったものであり、利用側の最小 version ではありません。KMP 統合における Kotlin 側の SwiftPM 連携は Alpha です。

## インストール

以下には package 座標と prerelease version の指定方法だけを示します。platform ごとのセットアップや IDE 固有の詳細は [Agent Skills](#agent-skills) を参照してください。

### iOS Native

Swift package を追加し、その `KsDialogs` product を利用します。

```swift
dependencies: [
    .package(
        url: "https://github.com/kamusoft/KsDialogs-SPM",
        exact: "<version>"
    )
]
```

prerelease では `<version>` を `X.Y.Z-alpha.N`、`X.Y.Z-beta.N`、`X.Y.Z-rc.N` などの正確な tag に置き換えます。

### Android Native

View-only アプリでは core Maven artifact を追加します。

```kotlin
dependencies {
    implementation("jp.kamusoft:ksdialogs:<version>")
}
```

Compose アプリでは `ksdialogs-compose` だけを追加します。core artifact は推移依存で解決されます。

```kotlin
dependencies {
    implementation("jp.kamusoft:ksdialogs-compose:<version>")
}
```

prerelease では `<version>` を `X.Y.Z-alpha.N`、`X.Y.Z-beta.N`、`X.Y.Z-rc.N` のいずれかに置き換えます。

### .NET MAUI

NuGet package を追加します。

```xml
<PackageReference Include="KsDialogs.Maui" Version="0.1.0" />
```

prerelease では `Version` の値を `X.Y.Z-alpha.N`、`X.Y.Z-beta.N`、`X.Y.Z-rc.N` のいずれかに置き換えます。

### Kotlin Multiplatform

Maven artifact を `commonMain` に追加します。

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("jp.kamusoft:ksdialogs-kmp:<version>")
        }
    }
}
```

iOS アプリ側では `https://github.com/kamusoft/KsDialogs-SPM` も追加し、その `KsDialogs` product を link します。prerelease では Maven artifact と Swift package tag に同じ `X.Y.Z-alpha.N`、`X.Y.Z-beta.N`、`X.Y.Z-rc.N` version を使います。

## 最小コード例

### iOS Native

```swift
import SwiftUI
import KsDialogs

struct ContentView: View {
    var body: some View {
        Button("Show toast") {
            Toast.shared.show(message: "Saved")
        }
    }
}
```

### Android Native

```kotlin
import jp.kamusoft.ksdialogs.Toast

fun notifySaved() {
    Toast.instance.show("Saved")
}
```

### .NET MAUI

```csharp
using KsDialogs;

namespace MyApp;

public static class Notifications
{
    public static void ShowSaved() => Toast.Instance.Show("Saved");
}
```

### Kotlin Multiplatform

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

## Agent Skills

[Agent Skills の索引](https://github.com/kamusoft/KsDialogs/blob/develop/skills/README_ja.md)では、Dialog、Loading、Toast、layout、transition、ViewModel の platform 別セットアップと完全なレシピ、および AiForms.Maui.Dialogs からの移行方法を案内しています。

## リポジトリ構成

| ディレクトリ | 入口 |
|---|---|
| [`ios/`](https://github.com/kamusoft/KsDialogs/tree/develop/ios) | Native iOS ライブラリ |
| [`android/`](https://github.com/kamusoft/KsDialogs/tree/develop/android) | Native Android ライブラリ |
| [`maui/`](https://github.com/kamusoft/KsDialogs/tree/develop/maui) | .NET MAUI wrapper |
| [`kmp/`](https://github.com/kamusoft/KsDialogs/tree/develop/kmp) | Kotlin Multiplatform wrapper |
| `samples/` | 4 形態の Sample アプリケーション |
| [`skills/`](https://github.com/kamusoft/KsDialogs/tree/develop/skills) | 英語版と日本語版の Agent Skills |
| [`assets/`](https://github.com/kamusoft/KsDialogs/tree/develop/assets) | 公開ドキュメント用画像 |
| [`kasane/`](https://github.com/kamusoft/KsDialogs/tree/develop/kasane) | プロジェクト知識と変更記録 |

[AGENTS.md](https://github.com/kamusoft/KsDialogs/blob/develop/AGENTS.md) · [概念ドキュメント](https://github.com/kamusoft/KsDialogs/tree/develop/kasane/concepts)

## 貢献

このプロジェクトでは外部からの Pull Request を受け付けていません。不具合の報告、機能の提案、質問は Issue で受け付けます。必要な情報を含められるよう、内容に合った Issue template を選んでください。貢献方針と報告方法は [CONTRIBUTING_ja.md](https://github.com/kamusoft/KsDialogs/blob/develop/.github/CONTRIBUTING_ja.md) を参照してください。

## ライセンス

KsDialogs は [MIT License](https://github.com/kamusoft/KsDialogs/blob/develop/LICENSE) で提供されます。
