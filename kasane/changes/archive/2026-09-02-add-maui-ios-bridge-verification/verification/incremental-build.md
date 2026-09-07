# bridge 更新のビルド追随 (2026-09-02)

互換面 (bridge) の Swift を直したとき、Sample の iOS へ新しいネイティブ成果物が届くこと
(BV-MA-04) と、変更が無いときは従来どおりスキップされること (BV-MA-05) の実測記録。
依存した SDK の内部要素は、SDK 更新でここが変わったときの見直し材料として残す
(maui/ADR-0003 の Consequences「SDK 内部ターゲットへの依存を抱える」の増分)。

## 依存した SDK の内部要素

読解した SDK は .NET for iOS 26.5 (`Microsoft.iOS.Sdk.net10.0_26.5/26.5.10284`) と
26.1 (`…_26.1/26.1.10502`) の targets。両者で該当箇所は同じ。

| 手当ての場所 | ぶら下がる target | 補正する item | その item が効く先 |
|---|---|---|---|
| maui/macios/KsDialogs.Binding.iOS/KsDialogs.Binding.iOS.csproj | `_ExpandNativeReferences` (AfterTargets) | `_FileNativeReference` | `_CreateBindingResourcePackage` の Inputs |
| samples/maui/KsDialogs.Sample.Maui/KsDialogs.Sample.Maui.csproj | `_ComputeLinkNativeExecutableInputs` (AfterTargets) | `_LinkNativeExecutableInputs` | `_LinkNativeExecutable` の Inputs |

塞いだ穴は SDK 側の次の 2 点。

- `_CreateBindingResourcePackage` の Inputs は `@(_FrameworkNativeReference);@(_FileNativeReference)`
  までで、xcframework (`_XCFrameworkNativeReference`) を含まない。SDK 自身が
  「framework はディレクトリなので毎回未ビルドに見えてしまい、Inputs に使えない」と説明している。
  さらに binding プロジェクトでは `ResolveNativeReferences` が `IsBindingProject != true` の条件で
  走らないため、xcframework は実バイナリへ展開されない
- `_ComputeLinkNativeExecutableInputs` は `_LinkNativeExecutableInputs` に
  `@(_NativeExecutableObjectFiles)` `@(_XamarinMainLibraries)` `@(_FileNativeReference)` を入れるが、
  静的 framework の `@(_FrameworkNativeReference)` を入れない。`NativeLink.cache` のハッシュも
  パス文字列だけを見るため、中身が変わっても再リンクの契機にならない

前提としてもう 1 つ、Sample 側のフィルタ (`'%(Filename)' == 'KsDialogsMauiBridge'`) は
**`_FrameworkNativeReference` の項目が framework 内の実バイナリのパス**
(`…/KsDialogsMauiBridge.framework/KsDialogsMauiBridge`) であることに依存している。SDK は
`ResolveNativeReferences` の出力を `@(NativeReference -> '%(Identity)/%(Filename)')` として解決するため
現状はそうなっており、それゆえこの条件はディレクトリ判定にならない。SDK 更新でここがディレクトリ
(`…/KsDialogsMauiBridge.framework`) に変わると、条件は真のまま毎回再リンク (ビルドが遅くなるだけで
成功する) へ静かに転ぶため、SDK 更新時はこの item の解決結果を確認する。

どちらもディレクトリではなく**中の実バイナリ (Mach-O)** を足すことで、「変わったときだけ」の
判定になる (ディレクトリを足すと毎回未ビルド扱いで走り続ける)。

## 実行環境と実行コマンド

```
dotnet build samples/maui/KsDialogs.Sample.Maui/KsDialogs.Sample.Maui.csproj \
  -f net10.0-ios -p:RuntimeIdentifier=iossimulator-arm64 -p:ValidateXcodeVersion=false -v:n
```

- `-v:n` にすると `すべての出力ファイルが入力ファイルに対して最新なので、ターゲット "…" を
  省略します。` の行でスキップが読める。判定はこの行の有無で行った
- `-p:ValidateXcodeVersion=false` が要る理由: MAUI ワークロードが解決する .NET for iOS 26.1 は
  Xcode 26.1 を要求するが、iOS Native の Swift パッケージ (`ios/Package.swift`) は
  swift-tools 6.3 を宣言しており Xcode 26.1 の Swift 6.2.1 では
  `package 'ios' is using Swift tools version 6.3.0 but the installed version is 6.2.1` で
  パッケージ解決に失敗する。Xcode 26.5 に揃えたうえで SDK の Xcode 版数検査
  (`_ValidateXcodeVersion`。SDK が用意している opt-out) を外して 1 つのツールチェインで通した。
  この事情は本変更とは独立の環境条件で、Simulator 向けビルドでは他に影響は出ていない
- 観測点は 3 つ: `_BuildXcodeProjects` / `_CreateBindingResourcePackage` / `_LinkNativeExecutable` の
  実行かスキップか、`obj/…/iossimulator-arm64/nativelibraries/KsDialogs.Sample.Maui` のタイムスタンプ、
  `.app` の実行ファイルに一時変更のシンボルが載るか

## 一時変更 (観察可能なシンボル)

`maui/macios/native/KsDialogsMauiBridge/MauiToastBridge.swift` の公開クラスへ、確認のあいだだけ
次のメソッドを足した (確認後に取り除き、差分が残っていないことを `git status` で確認済み)。

```swift
@objc(ksBridgeIncrementalBuildProbe)
public func ksBridgeIncrementalBuildProbe() -> Int { 20260902 }
```

`.app` の実行ファイルでの見え方 (`nm -a <.app>/<実行ファイル>`):

```
_$s19KsDialogsMauiBridge0c5ToastD0C02ksD21IncrementalBuildProbeSiyF
_$s19KsDialogsMauiBridge0c5ToastD0C02ksD21IncrementalBuildProbeSiyFTo
```

`strings -a` では ObjC セレクタ名 `ksBridgeIncrementalBuildProbe` が見える。以下の表の
「シンボル」列は `nm -a … | grep -c IncrementalBuildProbe` の値 (0 = 修正前のまま / 2 = 追随した)。

## BV-MA-04: bridge を変えたら .app まで届く

手当てを入れた状態で、成果物を捨てずに順に実行した結果。

| # | 直前の操作 | Xcode | 資源パッケージ | ネイティブリンク | nativelibraries | シンボル | 総時間 |
|---|---|---|---|---|---|---|---|
| A1 | 一時変更を戻す | 実行 | 実行 | 実行 | 19:14:49 | 0 | 27.6 s |
| A2 | なし | スキップ | スキップ | スキップ | 19:14:49 | 0 | 3.3 s |
| A5 | 一時変更を入れる | 実行 | 実行 | 実行 | 19:16:07 | **2** | 31.6 s |
| A6 | なし | スキップ | スキップ | スキップ | 19:16:07 | 2 | 2.4 s |

bridge の Swift だけを直した A5 で、資源パッケージが作り直され、ネイティブリンクが再実行され、
`nativelibraries/KsDialogs.Sample.Maui` が更新され、`.app` の実行ファイルに変更後のシンボルが
載った。

## BV-MA-05: 変更が無ければ再リンクしない

A2 / A6 のほか、変更なしの連続ビルドを重ねても `_LinkNativeExecutable` は一度もスキップを外れず、
総時間も 2〜4 秒台にとどまった (bridge を作り直すビルドは 20〜40 秒台)。

```
_LinkNativeExecutable:
すべての出力ファイルが入力ファイルに対して最新なので、ターゲット "_LinkNativeExecutable" を省略します。
```

**注意 (SDK の既定挙動)**: `_CreateBindingResourcePackage` だけは、変更が無くても 1 ビルドおきに
実行される。スキップしたビルドでは出力の `…resources.stamp` が `FileWrites` に載らず
IncrementalClean に消されるため、次のビルドで出力なしと判定されるためである。手当てを外した
状態でも同じ周期 (実行 → スキップ → 実行) になることを確認したので、これは本変更が持ち込んだ
挙動ではない。この再実行は資源パッケージを同じ内容で作り直すだけで、ネイティブリンクの
スキップには影響しない (上表・下の B2 のとおり)。

## 手当てが効いていることの確認 (A/B)

### Sample 側 (ネイティブリンクの入力)

両方の手当てを一時的に無効化して同じ操作を行った。

| # | 直前の操作 | Xcode | 資源パッケージ | ネイティブリンク | nativelibraries | シンボル |
|---|---|---|---|---|---|---|
| B1 | 一時変更を戻す (手当てあり) | 実行 | 実行 | 実行 | 19:16:46 | 0 |
| B2 | 一時変更を入れる (**手当てなし**) | 実行 | 実行 | **スキップ** | 19:16:46 (据え置き) | **0** |

xcframework は作り直されているのに、ネイティブリンクがスキップされて
`nativelibraries/KsDialogs.Sample.Maui` は修正前のまま、`.app` にも新しいシンボルが入らない。
ビルド自体は「成功」で終わる — 報告されている症状そのものである。

### Binding 側 (資源パッケージ再生成の入力)

資源パッケージの再生成は上記の周期で 1 ビルドおきに走るため、Sample 側の表だけでは Binding 側の
要否を切り分けられない。binding プロジェクト単体
(`dotnet build maui/macios/KsDialogs.Binding.iOS/KsDialogs.Binding.iOS.csproj -p:ValidateXcodeVersion=false -v:n`)
で、直前のビルドが実行済み = **本来ならスキップになる番**の状態を作ってから bridge を変えた。

| # | 手当て | 直前の操作 | Xcode | 資源パッケージ | resources.zip |
|---|---|---|---|---|---|
| D1 | なし | bridge の Swift を変更 | 実行 | **スキップ** | 据え置き (古い中身のまま) |
| E4 | あり | bridge の Swift を変更 | 実行 | **実行** | 更新 |

手当てありの E4 のあと、`KsDialogs.Binding.iOS.resources.zip` の中の
`KsDialogsMauiBridgeiOS.xcframework/ios-arm64_x86_64-simulator/KsDialogsMauiBridge.framework/KsDialogsMauiBridge`
に変更後のシンボルが入っていることを確認した (`unzip -p … | strings -a | grep`)。

## Android TFM への影響

Sample 側の手当ては iOS の TFM に限った条件付きで、Android には入らない。

```
dotnet build samples/maui/KsDialogs.Sample.Maui/KsDialogs.Sample.Maui.csproj \
  -f net10.0-android -p:EmbedAssembliesIntoApk=true
```

結果: 成功 (エラー 0 / 警告 8。警告はすべて binding 生成時の既知の BG8401 で、
maui/ADR-0003 の Consequences に記載のあるもの)。

## テスト標的のソースは xcframework を作り直さない

同じ csproj の `_AdjustKsBridgeXcodeProjectInputs` から、テスト標的
(`maui/macios/native/KsDialogsMauiBridgeTests/`) とテスト用ホストアプリ
(`…/KsDialogsMauiBridgeTestHost/`) のソースを除外した。どちらも scheme のビルド対象には
入らないため、直しても xcframework は変わらない。

確認: 両ディレクトリの `*.swift` を `touch` してから binding プロジェクトをビルドしたところ、
`_BuildXcodeProjects` はスキップのままだった。

## 蒸留への申し送り値

| 写す先 | 値 |
|---|---|
| handbook/cross/test-execution の件数表 | 「bridge テスト標的の実行と検出力」(`verification/bridge-test-target.md`) の実行コマンドと 6 tests / 3 suites |
| handbook/cross/runtime-behavior-verification | bridge 更新後の入れ替わりは `.app` の実行ファイルのシンボル (`nm -a` の Swift mangled 名 / `strings -a` の ObjC セレクタ) で確かめられる |
| maui/ADR-0003 の Consequences 現行照合 | 本ファイル冒頭の「依存した SDK の内部要素」の表 (target 2 つ・item 3 つ) |

## 後片付け

- bridge の Swift への一時変更は取り除き済み (`git status` に差分なし)
- 手当ての一時無効化 (A/B 用) も元に戻し済み
