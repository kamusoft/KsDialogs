---
kind: guide
applies-when:
  always: false
  tasks: [環境構築, worktree での作業開始, Gradle ルートのビルド・テスト, Sample のビルドと実行, MAUI iOS の Sample ビルド, .NET SDK の解決, 消費者検証を手元で回す, リリース用スクリプトの自己テスト]
title: ローカル開発環境の準備
description: Android SDK と Xcode のローカル環境を整え、repo 直下の global.json が固定する .NET SDK / workload set を確認し、4 形態の Sample が参照するライブラリとビルド・起動手順を確認するためのガイド。Gradle build root は本体・Sample の 5 つと消費者検証の 2 つ。消費者検証 (`verification/`) と `scripts/release/` の自己テストを手元で回す手順を含む
timestamp: 2026-09-10
---

# ローカル開発環境の準備

この文書は、clone 直後や git worktree を切った直後に Gradle ルートを動かすまでの準備と、MAUI のビルドが使うツールチェインの解決条件をまとめる。読むと、Android SDK の場所をどこに置けば 5 つの build root すべてが解決できるか、worktree で `SDK location not found` が出たとき何を複製すればよいか、`dotnet` がどの SDK と workload set を拾うか、`Swift tools version` の不一致でパッケージ解決が止まったとき何を指定すればよいかが分かる。テストの実行方法と完了判定は [テスト実行規約](test-execution.md) が正であり、本書は準備だけを扱う。先例 KsSettingsView の同名 guide を下敷きにし、このリポジトリで実測した範囲に絞っている。

## Android SDK ロケーション

MAUI の `dotnet build` は Android SDK を自身で解決するため、本節は Gradle を使うルートが対象である。このリポジトリの Gradle build root は 7 つあり、Sample までの 5 つは `includeBuild` で互いを巻き込む (`android/` は他の 4 つすべてから included build として使われる)。Android Gradle Plugin は build root ごとに `local.properties` を独立して解決するため、SDK は root ごとに見える状態にする。

| build root | 巻き込む included build | SDK の解決 |
|---|---|---|
| `android/` | なし | `ANDROID_HOME` または自身の `local.properties` |
| `kmp/` | `android/` | 同上 |
| `maui/android/native/` | `android/` | 同上 |
| `samples/android/` | `android/` | 同上 |
| `samples/kmp/` | `kmp/` と `android/` | 同上 |
| `verification/android/` | なし (配布物をローカル参照先から解決する) | `verification/lib/android-sdk.sh` |
| `verification/kmp/` | なし (同上) | `verification/lib/android-sdk.sh` |

消費者検証の 2 ルート (`verification/`) には `local.properties` を置かない。`verification/lib/android-sdk.sh` が `ANDROID_HOME` / `ANDROID_SDK_ROOT` を見て、どちらも無ければ `android/local.properties` の `sdk.dir` を読んで `ANDROID_HOME` として export する。本体 build root の設定だけで消費者検証が動くため、未追跡ファイルを増やす必要はない。どちらも無い環境ではフィード準備の前に失敗する。

### ANDROID_HOME を使う

`ANDROID_HOME` を設定すると全 build root を一度に解決できる。SDK の場所は環境に合わせて変える。

```bash
export ANDROID_HOME="$HOME/Library/Android/sdk"
```

### local.properties を使う

環境変数を使わない場合は、動かす build root と、それが巻き込む included build のそれぞれに同じ `sdk.dir` を置く。

```properties
sdk.dir=<Android SDK の絶対パス>
```

Android Studio が自動生成するのは開いた root 側だけで、included build 側は生成しない。`SDK location not found` のエラーは不足している root のパスを指すので、そのディレクトリへ置く。

## git worktree で作業するとき

`local.properties` は VCS 管理外 (`.gitignore`) のため、**worktree には引き継がれない**。`ANDROID_HOME` を使っていない環境では、worktree で Gradle ルートを動かす前に、動かす root と included build の `local.properties` を元の checkout から複製する。

```bash
cp <元の checkout>/android/local.properties android/
cp <元の checkout>/maui/android/native/local.properties maui/android/native/
```

- 複製先は動かす root による。`maui/android/native/` のテストなら上の 2 つ、`kmp/` なら `kmp/` と `android/`
- 複製した `local.properties` は worktree 側でも VCS 管理外のまま。`git status` には現れず、worktree を削除すれば消える
- 症状の見え方: `dotnet test` (maui/) は Native 側の Gradle ビルドを巻き込むため、この不足が「テストの失敗」として現れる。切り分けは [テスト実行規約](test-execution.md) の maui/ 節を参照する

2026-09-02 の実測: worktree で `maui/android/native/` のテストを回したところ `android/` と `maui/android/native/` の両方で `SDK location not found` になり、元の checkout から 2 ファイルを複製して解消した。

## .NET SDK と MAUI ワークロード

repo 直下の `global.json` が .NET SDK と workload set の版を固定する。`maui/` や `samples/maui/` で `dotnet` を実行すると、親ディレクトリに別の `global.json` があってもこの設定が使われる。

```json
{
  "sdk": {
    "version": "10.0.300",
    "rollForward": "disable",
    "workloadVersion": "10.0.300.3"
  }
}
```

- 解決できていることは `dotnet --version` が `10.0.300` を返し、`dotnet workload list` が repo の `global.json` の workload set を使う旨を表示することで確認する
- `rollForward` を `disable` にしているため、指定した SDK が手元に無ければ**ロールフォワードせずに失敗する** (近い patch を黙って拾うことはない)。表示された版を導入して揃える
- workload set を固定すると .NET for iOS の版も固定される (`10.0.300.3` は .NET for iOS 26.5)。ワークロード自体の導入は `dotnet workload install maui`
- **workload set を上げるときは、`maui/Directory.Packages.props` の `Microsoft.Maui.Controls` と Sample の `MauiVersion` を同梱版に合わせる**
- 同梱版は `$(MauiVersion)` の既定値。版を書いていないプロジェクトで評価して確かめる (`dotnet msbuild maui/KsDialogs.Maui/KsDialogs.Maui.csproj -getProperty:MauiVersion -p:TargetFramework=net10.0`。2026-09-08 の実測は `10.0.20`)
- 揃えると下限・ビルド版・版を書かない利用者の既定値が一致し、CI が下限を常時検証する
- Sample (`samples/maui/KsDialogs.Sample.Maui/KsDialogs.Sample.Maui.csproj`) は CI の検証対象ではないため、ずれても検査で気づけない

## MAUI iOS ビルドの Xcode 版数

MAUI ワークロードが解決する .NET for iOS SDK が要求する Xcode と、iOS Native の Swift パッケージ (`ios/Package.swift` の swift-tools 版数) が要求する Xcode は独立に決まり、食い違うことがある。現行の固定ではどちらも Xcode 26.5 を要求して一致しているため、**`DEVELOPER_DIR` の付け替えも版数検査の opt-out も既定では要らない**。

```bash
dotnet build samples/maui/KsDialogs.Sample.Maui/KsDialogs.Sample.Maui.csproj \
  -f net10.0-ios -p:RuntimeIdentifier=iossimulator-arm64
```

- 手元の既定の Xcode が 26.5 であることは `xcodebuild -version` で確認する。異なる版が選択されていれば `xcode-select` で切り替える
- 2026-09-08 の実測: 上のコマンドと facade の `net10.0-ios` ビルドが、既定の Xcode 26.5 のまま opt-out なしで成功する

### 版が食い違ったとき

パッケージ解決が `package 'ios' is using Swift tools version X but the installed version is Y` で止まったら、要求版が再び割れている。Xcode を新しい側に揃えたうえで、SDK が用意している版数検査の opt-out を付けて 1 つのツールチェインで通す。Simulator 向けのビルドとテストでは、この opt-out による他への影響は出ていない。

```bash
dotnet build ... -p:ValidateXcodeVersion=false
```

## Sample のビルドと実行

4 つの Sample は配布済み package ではなく、同じリポジトリの公開 product を利用者側から参照する。参照方式と実行できる OS は次のとおり。

| Sample | 本体の参照方式 | 実行 OS |
|---|---|---|
| `samples/ios` | `ios/` を Local Swift Package として参照 | iOS |
| `samples/android` | `android/` を Gradle composite build として参照 | Android |
| `samples/maui` | `maui/KsDialogs.Maui` への ProjectReference 1 本 | iOS / Android |
| `samples/kmp` | `kmp/` と `android/` を Gradle composite build として参照 | iOS / Android |

撮影支援の起動引数と安定デモ ID は [Sample パリティ規約](sample-parity.md#撮影支援の起動引数) を参照する。

### iOS Native

Xcode project は `ios/` を Local Swift Package として参照し、公開 product `KsDialogs` だけをリンクする。

```bash
cd samples/ios
xcodebuild -project KsDialogsSample.xcodeproj -scheme KsDialogsSample \
  -destination 'platform=iOS Simulator,name=<機種名>' \
  -derivedDataPath DerivedData CODE_SIGNING_ALLOWED=NO build
xcrun simctl install <UDID> DerivedData/Build/Products/Debug-iphonesimulator/KsDialogsSample.app
xcrun simctl launch <UDID> jp.kamusoft.ksdialogs.samples.ios
```

`<機種名>` と `<UDID>` は `xcrun simctl list devices available` から選ぶ。

### Android Native

`samples/android/settings.gradle.kts` は `includeBuild("../../android")` と `dependencySubstitution` を使い、Maven 座標 `jp.kamusoft:ksdialogs-core` / `jp.kamusoft:ksdialogs` を included build の `:ksdialogs-core` / `:ksdialogs` へ置き換える。Android Gradle Plugin の library module は Maven publication を自動生成しないため、GAV の自動置換に頼らず利用側で明示する。version catalog は `android/gradle/libs.versions.toml` を共有し、Sample だけ依存版がずれないようにする。

```bash
cd samples/android
./gradlew :app:assembleDebug
adb -s <device> install -r app/build/outputs/apk/debug/app-debug.apk
adb -s <device> shell am start -n jp.kamusoft.ksdialogs.samples.android/.MainActivity
```

### .NET MAUI

MAUI Sample は facade project `maui/KsDialogs.Maui` への ProjectReference 1 本だけを持ち、Native binding は推移参照で受け取る。target framework は `net10.0-ios` と `net10.0-android` の platform target だけで、テスト用の素の `net10.0` は含めない。

```bash
cd samples/maui/KsDialogs.Sample.Maui

dotnet build -f net10.0-ios -p:RuntimeIdentifier=iossimulator-arm64
xcrun simctl install <UDID> bin/Debug/net10.0-ios/iossimulator-arm64/KsDialogs.Sample.Maui.app
xcrun simctl launch <UDID> jp.kamusoft.ksdialogs.samples.maui

dotnet build -f net10.0-android -p:EmbedAssembliesIntoApk=true
adb -s <device> install -r bin/Debug/net10.0-android/jp.kamusoft.ksdialogs.samples.maui-Signed.apk
adb -s <device> shell am start -n "$(adb -s <device> shell cmd package resolve-activity --brief jp.kamusoft.ksdialogs.samples.maui | tr -d '\r')"
```

手動で APK を install する場合は `EmbedAssembliesIntoApk=true` を付ける。Fast Deployment の古い override assembly が端末に残っている場合は、再 install の前に対象 package を uninstall する。iOS 側の SDK と Swift tools の版が食い違ったときの対処は「MAUI iOS ビルドの Xcode 版数」節を参照する。

### Kotlin Multiplatform

`samples/kmp` は KMP facade を composite build で参照し、`shared` が共通の ViewModel と show 呼び出しを持つ。`androidApp` と `iosApp` は各 Native API を使って View だけを登録する。ルート `build.gradle.kts` の plugin 宣言は `apply false` のままにし、本体の included build と同じ classloader で Kotlin/Native の build service を解決させる。

```bash
cd samples/kmp
./gradlew :androidApp:assembleDebug
adb -s <device> install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk
adb -s <device> shell am start -n jp.kamusoft.ksdialogs.samples.kmp.android/.MainActivity

cd iosApp
xcodebuild -project KsDialogsSampleKmp.xcodeproj -scheme KsDialogsSampleKmp \
  -destination 'platform=iOS Simulator,name=<機種名>' \
  -derivedDataPath DerivedData CODE_SIGNING_ALLOWED=NO build
xcrun simctl install <UDID> DerivedData/Build/Products/Debug-iphonesimulator/KsDialogsSampleKmp.app
xcrun simctl launch <UDID> jp.kamusoft.ksdialogs.samples.kmp.ios
```

KMP iOS アプリがリンクする 3 点と `KotlinMultiplatformLinkedPackage` の再生成手順は [KMP 利用者の iOS ホスト統合](../../concepts/kmp/api/ios-host-integration.md#sample-で合成-package-を再生成する) を参照する。

## 消費者検証を手元で回す

配布物を利用者と同じ経路で解決する消費者検証 (`verification/`、仕組みは concepts の [消費者検証](../../concepts/cross/architecture/consumer-verification.md)) は、形態ごとの `build-consumer.sh` を引数なしで実行すると dry-run が通しで動く (フィード準備 → 消費者ビルド → 検査)。

```bash
verification/ios/build-consumer.sh
verification/android/build-consumer.sh
verification/maui/build-consumer.sh
verification/kmp/build-consumer.sh
```

- 作業ディレクトリは `${TMPDIR:-/tmp}/ksdialogs-verification/<形態>` (`--work` で変更可。リポジトリ内は拒否される)。フィード準備の出力を再利用するときは `prepare-feed.sh` の最終行が出す参照先を `--reference` に渡す
- `--version` を省くと合成 version `0.0.0-alpha.0` で発行・解決する。`--mode smoke` は version 必須で、公開レジストリを参照する (未公開の version では解決に失敗する)
- KMP はフィード準備が本体側の `kmp/.swiftpm-locks/` 配下の合成 Swift マニフェスト 2 本を書き換えて復元する。2 本に未 commit の変更があると発行前に失敗するので、先に commit するか戻してから回す
- 実行後は `git status` で `verification/` と `kmp/.swiftpm-locks/` に差分が無いことを確かめる。差分が出たら消費者検証の欠陥として扱う (追跡物は実行で変化しない契約)
- Android / KMP の SDK は「Android SDK ロケーション」節のとおり本体 build root の設定から引き継がれる。MAUI は repo 直下の `global.json` が固定する SDK をそのまま使う

## リリース用スクリプトの自己テストを回す

`scripts/release/` のスクリプトはリリースのときにしか実行されない。判定やパターンを触ったら、リポジトリルートで自己テストを回して全件通過を確かめる (ネットワークにも実レジストリにも出ない)。

```bash
for s in scripts/release/*.sh; do "$s" --selftest; done
python3 scripts/release/set-readme-version.py --selftest
```

`set-readme-version.py` の自己テストは実物の README と Skill を一時ディレクトリへ複写して置換を試すため、インストール例を書き換えたときの受け皿にもなる (対象行の形が変わって検出できなくなっていれば、ここで落ちる)。リリース手順そのものは [リリース手順](release-procedure.md) を参照する。

## 関連

- [テスト実行規約](test-execution.md) — 各 build root の全件実行コマンドと件数の確認
- [リリース手順](release-procedure.md) — 公開の起動と初回だけ行う GitHub 側の設定
- [Sample パリティ規約](sample-parity.md) — 4 ルートで一致させるデモと撮影支援の外部契約
- [KMP 利用者の iOS ホスト統合](../../concepts/kmp/api/ios-host-integration.md) — KMP iOS の依存経路と合成 package
