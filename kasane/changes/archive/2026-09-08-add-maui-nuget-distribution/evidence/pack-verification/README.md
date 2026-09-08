# 3 パッケージのローカル pack 検算 (2026-09-08)

`<OUT>` は作業ツリー外の一時出力ディレクトリ (`-o` で指定)。生ログは手元保管、ここは抜粋。

コマンド (binding 2 件 → facade の順に 2 回):

```
cd maui
dotnet pack android/KsDialogs.Binding.Android/KsDialogs.Binding.Android.csproj -c Release -o <OUT>
dotnet pack macios/KsDialogs.Binding.iOS/KsDialogs.Binding.iOS.csproj          -c Release -o <OUT>
dotnet pack KsDialogs.Maui/KsDialogs.Maui.csproj                                -c Release -o <OUT>
# 2 回目は上記に -p:Version=0.1.0-alpha.1 を足す
```

3 件とも exit 0。

## 生成物 (Version 未指定 / 注入の 2 回とも同じ 6 ファイル)

```
KsDialogs.Binding.Android.<VER>.nupkg / .snupkg
KsDialogs.Binding.iOS.<VER>.nupkg     / .snupkg
KsDialogs.Maui.<VER>.nupkg            / .snupkg
```

`<VER>` は Version 未指定で `0.0.0-dev` (props の既定値)、`-p:Version=0.1.0-alpha.1` で `0.1.0-alpha.1`。
`snupkg` は 3 パッケージとも併せて生成される (`SymbolPackageFormat=snupkg`)。

## nuspec のメタデータ (3 パッケージ共通)

```
<authors>kamusoft</authors>
<license type="expression">MIT</license>
<icon>icon.png</icon>
<projectUrl>https://github.com/kamusoft/KsDialogs</projectUrl>
<copyright>Copyright (c) kamusoft</copyright>
<repository type="git" url="https://github.com/kamusoft/KsDialogs" branch="refs/heads/develop" commit="<COMMIT>" />
```

`<readme>README.md</readme>` は facade の nuspec にだけある (binding 2 件の pack は
「パッケージに readme がありません」の案内が出るだけで成功する)。

## facade の nuspec (Version 注入時)

```
<id>KsDialogs.Maui</id>
<version>0.1.0-alpha.1</version>
<tags>maui dialog dialogs loading toast ios android</tags>
<description>A dialog UI library for .NET MAUI that presents dialogs, loading indicators, and toasts
 from anywhere in an application, with content written as MAUI views, on iOS and Android.
 Successor to AiForms.Maui.Dialogs.</description>
<dependencies>
  <group targetFramework="net10.0">
    <dependency id="Microsoft.Maui.Controls" version="10.0.20" exclude="Build,Analyzers" />
  </group>
  <group targetFramework="net10.0-android36.0">
    <dependency id="KsDialogs.Binding.Android" version="0.1.0-alpha.1" exclude="Build,Analyzers" />
    <dependency id="Microsoft.Maui.Controls" version="10.0.20" exclude="Build,Analyzers" />
  </group>
  <group targetFramework="net10.0-ios26.0">
    <dependency id="KsDialogs.Binding.iOS" version="0.1.0-alpha.1" exclude="Build,Analyzers" />
    <dependency id="Microsoft.Maui.Controls" version="10.0.20" exclude="Build,Analyzers" />
  </group>
</dependencies>
```

`net10.0` の group に binding 依存は無い。binding 2 件の version は facade と同じ値で、
Version 未指定の回では 3 パッケージとも `0.0.0-dev` になる。

## facade の同梱物

```
KsDialogs.Maui.nuspec
README.md
buildTransitive/KsDialogs.Maui.props
buildTransitive/KsDialogs.Maui.targets
icon.png
lib/net10.0/KsDialogs.Maui.dll
lib/net10.0-android36.0/KsDialogs.Maui.dll
lib/net10.0-android36.0/KsDialogs.Maui.xml
lib/net10.0-ios26.0/KsDialogs.Maui.dll
```

`.aar` / `.xcframework` / `KsDialogs.Binding.*.dll` / 自 assembly 用 `KsDialogs.Maui.aar` は無い。

## Android binding の同梱物と依存

```
lib/net10.0-android36.0/KsDialogs.Binding.Android.dll
lib/net10.0-android36.0/KsDialogs.Binding.Android.xml
lib/net10.0-android36.0/ksdialogs-core-release.aar
lib/net10.0-android36.0/ksdialogs-maui-bridge-release.aar
```

Gradle 由来の aar 2 本だけで、自 assembly 用 `KsDialogs.Binding.Android.aar` は無い。
nuspec の依存は `Xamarin.Kotlin.StdLib 2.4.0.1` と `Xamarin.KotlinX.Coroutines.Android 1.11.0.1`。
description に "do not reference this package directly" を含む (iOS 側も同文)。

### 自 assembly 用 aar について

このリポジトリでは .NET Android SDK が自 assembly 用 aar (`$(TargetName).aar`) を**生成しない**。
facade / Android binding とも Release / Debug の出力に `KsDialogs.Maui.aar` /
`KsDialogs.Binding.Android.aar` は現れず、pack の除去後処理 (`KsExcludeGeneratedAarFromPackage`) は
`Exists` 条件が成立せず走らない。同梱物の検査で不在を確認したのが上記 2 節。
(先例 KsSettingsView では推移依存の native ライブラリを抱えて生成され、除去が要った)

## iOS binding の同梱物

```
lib/net10.0-ios26.0/KsDialogs.Binding.iOS.dll
lib/net10.0-ios26.0/KsDialogs.Binding.iOS.resources.zip
```

resources.zip 内の `KsDialogsMauiBridgeiOS.xcframework` のスライス:

```
KsDialogsMauiBridgeiOS.xcframework/Info.plist
KsDialogsMauiBridgeiOS.xcframework/ios-arm64/KsDialogsMauiBridge.framework/KsDialogsMauiBridge
KsDialogsMauiBridgeiOS.xcframework/ios-arm64_x86_64-simulator/KsDialogsMauiBridge.framework/KsDialogsMauiBridge
```

device と simulator の両スライスがある。

## pack 対象の限定

```
cd maui
dotnet pack KsDialogs.Maui.Tests/KsDialogs.Maui.Tests.csproj -c Release
dotnet pack KsDialogs.Maui.ApiSurfaceCheck/KsDialogs.Maui.ApiSurfaceCheck.csproj -c Release
```

どちらも exit 0 で nupkg は 0 件。`-getProperty:IsPackable` はいずれも `false`
(テスト 2 プロジェクトは csproj で `IsPackable=false` を明示しており、`maui/Directory.Build.props` の既定値も `false`)。

## 最低 OS 版の宣言元の一致

```
dotnet msbuild <csproj> -getProperty:SupportedOSPlatformVersion -p:TargetFramework=<tfm>
```

| プロジェクト | TFM | 評価値 |
|---|---|---|
| KsDialogs.Maui | net10.0 | (空) |
| KsDialogs.Maui | net10.0-android | 24.0 |
| KsDialogs.Maui | net10.0-ios | 17.0 |
| KsDialogs.Binding.Android | net10.0-android | 24.0 |
| KsDialogs.Binding.iOS | net10.0-ios | 17.0 |

Android は SDK が Version 形式へ正規化するため `24` の宣言に対して `24.0` と評価される。
`maui/` 配下の csproj / props / targets に数値の直書きは無く、すべて
`$(KsDialogsMinAndroidApi)` / `$(KsDialogsMinIOSVersion)` 参照 (grep で確認)。

## ガード由来の診断が出ないこと

facade (3 TFM) / binding 2 件 / Sample 両 OS のビルドログに `KSDLG` の出現なし。
Sample は Android / iOS とも 0 警告 0 エラー。
