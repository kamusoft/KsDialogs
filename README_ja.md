# KsDialogs

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
| .NET MAUI | iOS 17 / Android 7.0 (API 24) | .NET 10 (`net10.0`), Microsoft.Maui.Controls 10.0.20 |
| Kotlin Multiplatform | iOS 17 / Android 7.0 (API 24) | Kotlin 2.4.10, AGP 9.3.0, Gradle 9.7.0, Swift 6.3 |

Android target は minSdk 24、compileSdk 36 です。Android Native と Kotlin Multiplatform は同じ minor 系列の Kotlin Gradle Plugin (Kotlin 2.4.x) に対応し、利用側ビルドで動作を確認しているのは 2.4.10 です。表の version はライブラリのビルドに使ったものであり、利用側の最小 version ではありません。KMP 統合における Kotlin 側の SwiftPM 連携は Alpha です。

## インストール

以下には package 座標と version の書き方だけを示します。例の version は現在の公開版で、リリースのたびに更新されます。prerelease は `X.Y.Z-alpha.N`、`X.Y.Z-beta.N`、`X.Y.Z-rc.N` の形で書きます。platform ごとのセットアップや IDE 固有の詳細は [Agent Skills](#agent-skills) を参照してください。

### iOS Native

Swift package を追加し、その `KsDialogs` product を利用します。

```swift
dependencies: [
    .package(
        url: "https://github.com/kamusoft/KsDialogs-SPM",
        exact: "0.1.0-beta.1"
    )
]
```

SwiftPM は `exact` で tag を固定した宣言でなければ prerelease を解決しないため、正式版でも prerelease でもこの形のまま使います。

### Android Native

View-only アプリでは core Maven artifact を追加します。

```kotlin
dependencies {
    implementation("jp.kamusoft:ksdialogs-core:0.1.0-beta.1")
}
```

Compose アプリでは `ksdialogs` だけを追加します。core artifact は推移依存で解決されます。

```kotlin
dependencies {
    implementation("jp.kamusoft:ksdialogs:0.1.0-beta.1")
}
```

prerelease も同じ version 文字列を座標にそのまま書きます。

### .NET MAUI

NuGet package を追加します。

```xml
<PackageReference Include="KsDialogs.Maui" Version="0.1.0-beta.1" />
```

prerelease も同じ version 文字列を `Version` 属性にそのまま書きます。

Native の binding package は iOS / Android の target framework へ推移依存で届きます。アプリ側から直接参照する必要はありません。

Microsoft.Maui.Controls は 10.0.20 以上が必要です。これはこのリポジトリが固定する .NET workload set に同梱される version と同じなので、同じ workload set を使うアプリでは MAUI 本体の version を書く必要はありません。10.0.20 未満を明示すると、NuGet のダウングレードエラー NU1605 でビルドが失敗します。

package の target は `net10.0`、`net10.0-ios`、`net10.0-android` で、iOS / Android の MAUI workload を入れた .NET 10 SDK が必要です。最低 OS 版は iOS 17 / Android 7.0 (API 24) で、`SupportedOSPlatformVersion` がそれ未満のアプリ (未設定の場合を含む) はビルド時にガード診断 `KSDLG0001` で停止します。

### Kotlin Multiplatform

Maven artifact を `commonMain` に追加します。

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            api("jp.kamusoft:ksdialogs-kmp:0.1.0-beta.1")
        }
    }
}
```

共有 ViewModel が `DialogViewModel` を継承するため、Android アプリ側から KsDialogs の型が見える必要があります。`api` はそのための宣言です。

Android アプリ側には Android Native の artifact `jp.kamusoft:ksdialogs-core` が推移依存で届きます。Compose 用の artifact `jp.kamusoft:ksdialogs` は届かないため、Android アプリで Compose の中身を書く場合は別途追加します。

iOS アプリ側では `https://github.com/kamusoft/KsDialogs-SPM` も追加し、その `KsDialogs` product を Maven artifact と同じ version に `exact` で固定して link します。

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
| [`samples/`](https://github.com/kamusoft/KsDialogs/tree/develop/samples) | 4 形態の Sample アプリケーション |
| [`skills/`](https://github.com/kamusoft/KsDialogs/tree/develop/skills) | 英語版と日本語版の Agent Skills |
| [`assets/`](https://github.com/kamusoft/KsDialogs/tree/develop/assets) | 公開ドキュメント用画像 |
| [`kasane/`](https://github.com/kamusoft/KsDialogs/tree/develop/kasane) | プロジェクト知識と変更記録 |

[AGENTS.md](https://github.com/kamusoft/KsDialogs/blob/develop/AGENTS.md) · [概念ドキュメント](https://github.com/kamusoft/KsDialogs/tree/develop/kasane/concepts)

## 貢献

このプロジェクトでは外部からの Pull Request を受け付けていません。不具合の報告、機能の提案、質問は Issue で受け付けます。必要な情報を含められるよう、内容に合った Issue template を選んでください。貢献方針と報告方法は [CONTRIBUTING_ja.md](https://github.com/kamusoft/KsDialogs/blob/develop/.github/CONTRIBUTING_ja.md) を参照してください。

## ライセンス

KsDialogs は [MIT License](https://github.com/kamusoft/KsDialogs/blob/develop/LICENSE) で提供されます。
