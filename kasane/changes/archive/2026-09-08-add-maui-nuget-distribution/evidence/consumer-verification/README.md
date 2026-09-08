# 消費者検証 (一時プロジェクト、リポジトリには残さない) (2026-09-08)

tasks 5.1〜5.6。ローカルフォルダフィードに置いた 3 パッケージを、素の MAUI アプリから
facade の PackageReference 1 行だけで導入し、restore → Release ビルド → 起動 →
ガードの発火 / 非発火 まで確認した記録。

このファイルはログの抜粋。全文は作業スクラッチ (リポジトリ外、手元保管) に残し、判定に要る行だけを
転記した。ローカル絶対パスは `<SCRATCH>` / `<DOTNET_ROOT>`、端末の識別子は
`<uuid>` / `<android-serial>` に置換している。

## 0. 検証環境

- 消費者プロジェクト置き場: `<SCRATCH>/consumer/` (リポジトリ外。`ConsumerApp` / `LibNet10` / `MultiTfm`)
- SDK / workload: 本リポジトリの `global.json` をそのまま複製

  ```
  { "sdk": { "version": "10.0.300", "rollForward": "disable", "workloadVersion": "10.0.300.3" } }
  ```

  `dotnet --version` => 10.0.300 / `dotnet workload list` => maui 10.0.20/10.0.100 (SDK 10.0.300)
- ローカルフォルダフィード `<SCRATCH>/consumer/feed` に、Release / `-p:Version=0.1.0-alpha.1` で
  pack した 3 nupkg + 3 snupkg を配置 (pack の検算は evidence/pack-verification)
- `nuget.config` (`<SCRATCH>/consumer/`): `<clear/>` でユーザー環境の既定ソースを外し、
  `local-ksdialogs` (上記フォルダ) と `nuget.org` の 2 件だけを列挙。
  `globalPackagesFolder` を `./pkgs` に固定。nuget.org を残す理由は「留意点」を参照
- 消費者アプリ `ConsumerApp`: `dotnet new maui` の生成物に対する変更は次の 5 点だけ

  | 変更 | 値 |
  |---|---|
  | `TargetFrameworks` | `net10.0-android;net10.0-ios` |
  | `RestorePackagesPath` | `<SCRATCH>/consumer/pkgs` (packages path の隔離) |
  | `WarningsAsErrors` | `$(WarningsAsErrors);NU1605;NU1608;NU1107` |
  | `SupportedOSPlatformVersion` | android = 24 / ios = 17.0 |
  | `PackageReference` | `KsDialogs.Maui` `0.1.0-alpha.1` (追加はこの 1 行のみ) |

  `Microsoft.Maui.Controls` の版はテンプレートのまま (`Version="$(MauiVersion)"`) で、
  csproj には数値を書いていない。
  Android Release では `PublishTrimmed` / `AndroidLinkTool=r8` / `RunAOTCompilation` を明示する
  (`AndroidLinkTool` は .NET Android SDK の既定が空 = Java コード縮小なし のため利用者側で書く)。
- 消費者コード: `MauiProgram` で `AddKsDialogs().RegisterForDialog<ConfirmCardView, ConfirmDialogViewModel>()`、
  `MainPage` の「Show dialog」で `Dialog.Instance.ShowAsync<ConfirmDialogViewModel>(...)` を呼び、
  結果を `Result:` ラベルに取り込む

## 1. tasks 5.1 — ローカルフィードからの restore

```
$ dotnet restore -v n   (ConsumerApp)
  ConsumerApp.csproj に対する 124 個のパッケージ
  ビルドに成功しました。
      0 個の警告
      0 エラー
```

`NU1605` / `NU1608` / `NU1107` を `WarningsAsErrors` に入れた状態で警告 0 件。

`project.assets.json` の取得元 (`project.restore.sources`):

```
<SCRATCH>/consumer/feed
<DOTNET_ROOT>/library-packs
https://api.nuget.org/v3/index.json
```

`project.assets.json` の `packageFolders`:

```
<SCRATCH>/consumer/pkgs        (この 1 件のみ = 隔離済み)
```

facade からの推移解決 (`targets`。RID 別の inner target も同じ内容):

```
net10.0-android:
  KsDialogs.Maui/0.1.0-alpha.1
    -> KsDialogs.Binding.Android 0.1.0-alpha.1, Microsoft.Maui.Controls 10.0.20
  KsDialogs.Binding.Android/0.1.0-alpha.1
    -> Xamarin.Kotlin.StdLib 2.4.0.1, Xamarin.KotlinX.Coroutines.Android 1.11.0.1
net10.0-ios:
  KsDialogs.Maui/0.1.0-alpha.1
    -> KsDialogs.Binding.iOS 0.1.0-alpha.1, Microsoft.Maui.Controls 10.0.20
  KsDialogs.Binding.iOS/0.1.0-alpha.1
    -> (依存なし)
```

`Microsoft.Maui.Controls` の解決版は全 TFM で `10.0.20`。binding 2 件は facade と同じ
`0.1.0-alpha.1` で推移的に入る。

取得元 (`pkgs/<id>/0.1.0-alpha.1/.nupkg.metadata` の `source`):

```
ksdialogs.maui             <SCRATCH>/consumer/feed
ksdialogs.binding.android  <SCRATCH>/consumer/feed
ksdialogs.binding.ios      <SCRATCH>/consumer/feed
xamarin.kotlin.stdlib      https://api.nuget.org/v3/index.json   (対照。テンプレート依存は nuget.org 由来)
```

## 2. tasks 5.2 — facade の公開型を参照した Release ビルド

```
$ dotnet build -c Release -f net10.0-android
  ビルドに成功しました。
      0 個の警告
      0 エラー
  XA4301 の出現件数: 0
```

Android Release の有効な設定 (`-getProperty`):

```
PublishTrimmed      true
RunAOTCompilation   true
AndroidEnableProfiledAot  true
AndroidLinkTool     r8      (csproj で明示。SDK 既定は空)
```

R8 が走った証跡として `bin/Release/net10.0-android/mapping.txt` が生成される。

成果物 `com.companyname.consumerapp-Signed.apk`:

```
lib/arm64-v8a/libaot-KsDialogs.Maui.dll.so
lib/arm64-v8a/libaot-KsDialogs.Binding.Android.dll.so
lib/x86_64/libaot-KsDialogs.Maui.dll.so
lib/x86_64/libaot-KsDialogs.Binding.Android.dll.so
```

リンク後の managed assembly (`obj/Release/net10.0-android/android-arm64/linked/`、android-x64 も同じ):

```
KsDialogs.Maui.dll
KsDialogs.Binding.Android.dll
```

```
$ dotnet build -c Release -f net10.0-ios -p:RuntimeIdentifier=iossimulator-arm64
  ビルドに成功しました。
      0 個の警告
      0 エラー
```

成果物 `bin/Release/net10.0-ios/iossimulator-arm64/ConsumerApp.app/`:

```
KsDialogs.Maui.dll
KsDialogs.Binding.iOS.dll
KsDialogs.Maui.aotdata.arm64
KsDialogs.Binding.iOS.aotdata.arm64
```

両 OS とも Release ビルドが成功し、facade と binding のアセンブリが成果物に残る。
利用者側の `XA4301` は 0 件 (自 assembly 用 aar が nupkg に無いため、翻案元で出ていた
`.so` の重複警告が起きない)。

## 3. tasks 5.3 — パッケージ経由の起動確認

上記の Release ビルド成果物 (trimming + R8 + AOT を通した APK / simulator の .app) を
そのままインストールして起動した。証跡はこのディレクトリの png。

iOS Simulator (iPhone 17 / iOS 26.0、`<uuid>`。起動中のものは流用せず新規 boot):

| コマ | 画像 | 実体 |
|---|---|---|
| 起動直後 | `ios-consumer-home-launch.png` | Home に「Show dialog」ボタンと `Result: (none)` |
| ダイアログ表示 | `ios-consumer-dialog-shown.png` | 覆いの上に白いカード。文言は "Delivered through the NuGet package." と Cancel / OK |
| 閉じた後 | `ios-consumer-home-result-completed.png` | カードと覆いが消え、ラベルが `Result: completed (True)` に変化 |

Android エミュレータ (AVD `Pixel_6`。system image は `android-31` / google_apis / arm64-v8a、`adb -s <android-serial>`):

| コマ | 画像 | 実体 |
|---|---|---|
| 起動直後 | `android-consumer-home-launch.png` | Home に「Show dialog」ボタンと `Result: (none)` |
| ダイアログ表示 | `android-consumer-dialog-shown.png` | 覆いの上に白いカード。文言は "Delivered through the NuGet package." と Cancel / OK |
| 閉じた後 | `android-consumer-home-result-completed.png` | カードと覆いが消え、ラベルが `Result: completed (True)` に変化 |

両 OS とも、パッケージ経由の 1 行登録で組み立てたダイアログが表示され、OK で閉じると
show の結果 (completed / True) が呼び出し元へ届く。6 枚の png は md5 がすべて相異なる。
撮影はシミュレータ / エミュレータのみで、個人を特定する要素は写っていない
(ステータスバーは時刻・Wi-Fi・電池のみ)。

## 4. tasks 5.4 — 要件未満・未設定でのガード (KSDLG0001)

### 4-a. 要件未満 (android = 21 / ios = 15.0)

```
$ dotnet build -c Release -f net10.0-android
  exit 1
  error KSDLG0001: KsDialogs requires SupportedOSPlatformVersion 24 or later for
  net10.0-android (minimum supported: Android API 24, iOS 17.0), but the current value
  is '21.0'. Add <SupportedOSPlatformVersion>24</SupportedOSPlatformVersion> to a
  PropertyGroup for this target framework in your project file.
  XAAMM* の出現件数: 0

$ dotnet build -c Release -f net10.0-ios -p:RuntimeIdentifier=iossimulator-arm64
  exit 1
  error KSDLG0001: KsDialogs requires SupportedOSPlatformVersion 17.0 or later for
  net10.0-ios (minimum supported: Android API 24, iOS 17.0), but the current value
  is '15.0'. Add <SupportedOSPlatformVersion>17.0</SupportedOSPlatformVersion> to a
  PropertyGroup for this target framework in your project file.
```

診断の出所は `<SCRATCH>/consumer/pkgs/ksdialogs.maui/0.1.0-alpha.1/buildTransitive/KsDialogs.Maui.targets(26,5)`
— 利用者側に同梱資産として展開されたものが動いている。

### 4-b. manifest merger より先に出ることの対照実験

ガードの要件定数だけを外から下げて (`-p:KsDialogsMinAndroidApi=1`)、同じ android = 21 の構成を
ビルドすると、ガードは沈黙し manifest merger が失敗する:

```
$ dotnet build -c Release -f net10.0-android -p:KsDialogsMinAndroidApi=1
  exit 1
  KSDLG0001 の出現件数: 0
  XAAMM0000: .../obj/Release/net10.0-android/AndroidManifest.xml:9:3-72 Error:
  XAAMM0000: 	uses-sdk:minSdkVersion 21 cannot be smaller than version 24 declared in
             library .../lp/165/jl/AndroidManifest.xml as the library might be using
             APIs not available in 21
  XAAMM0000: 	Suggestion: use a compatible library with a minSdk of at most 21,
  XAAMM0000: 		or increase this project's minSdk version to at least 24,
```

ガードが有効なときは `KSDLG0001` だけが出て `XAAMM` は 1 件も出ない (`CoreCompile` の前で
止まり manifest merger まで進まない)。ガードを外すと `XAAMM0000` が出る。
よってガードは manifest merger より先に働く。

### 4-c. 未設定 (csproj から android / ios の `SupportedOSPlatformVersion` 行を削除)

評価値 (`dotnet msbuild -getProperty:SupportedOSPlatformVersion`):

```
net10.0-android => 21.0   (SDK 既定)
net10.0-ios     => 26.5   (SDK 既定)
```

```
$ dotnet build -c Release -f net10.0-android
  exit 1 / KSDLG0001 (current value is '21.0') / XAAMM* 0 件

$ dotnet build -c Release -f net10.0-ios -p:RuntimeIdentifier=iossimulator-arm64
  ビルドに成功しました。0 個の警告 / 0 エラー / KSDLG の出現件数 0
```

.NET SDK は platform TFM に必ず既定値を与えるため、行を消しても
`SupportedOSPlatformVersion` が空文字になる経路は無かった。Android は既定 21.0 が要件未満なので
ガードが働き、iOS は既定 26.5 が要件以上のため通る (要件を満たす場合は何も出さない、
という意味論どおり)。targets の空文字分岐は通常の csproj 記述では到達しない。

## 5. tasks 5.5 — 非 platform TFM と outer build

### 5-a. `net10.0` のみのクラスライブラリ (`LibNet10`)

facade を PackageReference し、`typeof(KsDialogs.DialogException)` を参照する。

```
$ dotnet build -c Release
  ビルドに成功しました。0 個の警告 / 0 エラー
  KSDLG の出現件数: 0
```

### 5-b. 複数 TFM プロジェクト (`MultiTfm`: `net10.0;net10.0-android;net10.0-ios`、最低 OS 版は未設定)

評価値: `net10.0-android` => 21.0 / `net10.0-ios` => 26.0 (どちらも SDK 既定)

```
$ dotnet build -c Release -f net10.0          => exit 0 / KSDLG0001 0 件
$ dotnet build -c Release -f net10.0-ios      => exit 0 / KSDLG0001 0 件 (26.0 が要件以上)
$ dotnet build -c Release -f net10.0-android  => exit 1 / KSDLG0001 2 件

$ dotnet build -c Release        (TFM 指定なし = outer build)
  exit 1
  KSDLG0001 の内訳 (診断行に付く ::TargetFramework= で分類):
    TargetFramework=net10.0-android : 2 件
    TargetFramework 表記のない行 (= outer build 由来) : 0 件
```

outer build 自身も `net10.0` の inner build もガードの診断を出さない。
platform (android) の inner build に入って初めて要件未満のエラーになる。

## 6. 留意点 (spec との差・申し送り)

### 6-1. `nuget.config` に nuget.org を残した

design Decision 8 の想定どおり。`<clear/>` + ローカルフィードだけでは `dotnet new maui` の
テンプレート依存 (`Xamarin.AndroidX.*` / `Microsoft.Extensions.*` 等) が `NU1101` で解決できず、
これらは workload の library-packs にも入らないため nuget.org を 1 件併記した。
KsDialogs の 3 パッケージがローカルフィードからだけ取得されたことは 1. の `.nupkg.metadata` の
`source` で確認済み。phase-8 の `verification/maui` へはこの形を申し送る。

### 6-2. Android の消費者ビルドに `XA4301` が出ない

翻案元 (KsSettingsView) では自 assembly 用 aar 由来の `.so` 重複で `XA4301` が 4 件出ていたが、
KsDialogs では自 assembly 用 aar が生成されず nupkg にも入らないため (deviation.md)、
消費者ビルドは Android / iOS とも 0 警告で通る。

### 6-3. `AndroidLinkTool` の既定

.NET Android SDK 36.1.2 では `AndroidLinkTool` の既定が空 (= Java コードの縮小をしない) で、
Release でも R8 は自動では有効にならない。「trimming + R8 + AOT」の確認のため消費者アプリ側で
明示的に `r8` を指定した。managed trimming (`PublishTrimmed`) と AOT (`RunAOTCompilation`) は
Release 既定で true。

### 6-4. 一時プロジェクトの後始末

`ConsumerApp` / `LibNet10` / `MultiTfm` / `feed` / `pkgs` はすべて作業スクラッチ (リポジトリ外) に
あり、リポジトリには残していない。
