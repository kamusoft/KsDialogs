---
id: 0003
title: MAUI のビルド連携は iOS が標準 XcodeProject アイテム、Android は gradlew Exec + AndroidLibrary 束縛
status: accepted
date: 2026-08-15
---

## Context

MAUI facade は Native 2実装への薄い binding (core/ADR-0001) であり、iOS の Swift パッケージと Android の Gradle モジュールを .NET のビルドへ接続する必要がある。接続手段には .NET SDK の標準アイテム (`XcodeProject` / `AndroidGradleProject`) と、ビルドツールを直接呼ぶ手動方式がある。

先例 KsSettingsView は Android で標準アイテムを断念し gradlew の直接呼び出しへ倒しているが、その理由は「複数モジュール構成で SDK の出力ディレクトリ差し替えが破綻する」ことだった。KsDialogs の Android Native は単一モジュール (`:ksdialogs`) なので、この制約には非該当と見込まれた。標準アイテムが成立すれば SDK 内部ターゲットへの依存を避けられるため、実測して採否を決めることにした。

MAUI 形態は「MAUI 側で作った View を中身にしてダイアログを出す」ため、Native の公開 API (ViewModel 型からの View 解決) とは別の互換面を必要とする (maui/ADR-0001)。この互換面は Native のビルドルート (ios/ ・ android/) ではなく MAUI のビルドルート配下に置く。

## Decision

**iOS: 標準 `XcodeProject` アイテムで接続する (実測で成立)。**

- `maui/macios/native/KsDialogsMauiBridge.xcodeproj` — iOS Native の Swift パッケージをローカルパッケージ参照で取り込み、Swift の互換面と一緒に静的 framework (`MACH_O_TYPE = staticlib`) へまとめる
- `maui/macios/KsDialogs.Binding.iOS` — 上記を `XcodeProject` アイテムで参照し、生成された xcframework を束縛する
- MSBuild 18 のメタデータ自己参照バグ (MSB4120) により SDK 側の `NativeReference` 自動登録が壊れているため、`CreateNativeReference=false` とカスタムターゲットでの手動登録を併用する (先例と同じ回避策)
- 再ビルド判定の入力に iOS Native の Swift ソースを足す (SDK の既定は xcodeproj のあるディレクトリ配下しか見ないため、Native 側の変更で再ビルドされない)

**Android: 標準 `AndroidGradleProject` アイテムは使わず、gradlew の直接呼び出し + `AndroidLibrary` 束縛にする (実測で不成立)。**

- `maui/android/native/` — Kotlin の互換面モジュール (`:ksdialogs-maui-bridge`)。Android Native ビルドを composite build で取り込む (cross/ADR-0004 と同じ接続)
- `maui/android/KsDialogs.Binding.Android` — 互換面の aar を `AndroidLibrary`(`Bind=true`) で束縛し、Android Native ライブラリの aar は `Bind=false` で同梱する
- aar の生成は `Exec` で `./gradlew :ksdialogs:assembleRelease` と `./gradlew :ksdialogs-maui-bridge:assembleRelease` を呼ぶ。composite build は互換面のコンパイルに必要なクラスを解決するだけで、取り込まれた側の aar は作らないため、両方を明示的に呼ぶ

### 実測の記録 (Android 標準アイテムが不成立である根拠)

`AndroidGradleProject` アイテムを使ったビルドは、SDK が生成する init script のコンパイルで失敗する:

```
Initialization script '.../obj/Debug/net10.0-android/gradle/net.android.init.gradle.kts' line: 11
Script compilation error:
  Line 11: layout.buildDirectory.set(file(gradle.startParameter.projectProperties["netAndroidBuildDirOverride"]))
           ^ Argument type mismatch: actual type is 'String?', but 'Any' was expected.
```

- 原因は SDK の init script が Kotlin DSL の null 許容性に対応していないこと。`projectProperties` の要素型が `String?` として解決されるため、`Project.file(Any)` へ渡せない
- この init script は .NET Android SDK 35.0.105 / 36.1.2 / 36.1.53 のいずれでも同一で、SDK 側の更新では解消していない
- **モジュール構成とは無関係**であることを確認した: 同じ init script を単一モジュールの `android/` へ直接適用しても同じコンパイルエラーで失敗する。すなわち先例が挙げた「複数モジュール構成」以前の段階で、Gradle 9 系では標準アイテム自体が動かない
- 本プロジェクトの Gradle は 9.7 (cross/ADR-0002 の技術セット)。Android Native が AGP 9 を使う以上 Gradle 9 系は必須であり、Gradle を下げる回避はできない

## Alternatives Considered

- **Android も標準 `AndroidGradleProject` を使う**: 採用不能。上記のとおりビルドが成立しない。SDK 側の init script が修正されたら再評価する (見直し条件: `net.android.init.gradle.kts` が null 許容の Kotlin DSL でコンパイルできるようになること)
- **互換面を作らず Android Native の Kotlin 公開 API を直接束縛して C# から呼ぶ**: 却下。`KClass` キーのレジストリ・レシーバつき関数型・suspend 関数を C# から扱うことになり、`IContinuation` の手実装まで必要になる。互換面を1枚挟むほうが表面積も壊れやすさも小さい
- **互換面を ios/ ・ android/ のビルドルートへ置く**: 却下。MAUI 形態のためだけの面であり、Native 利用者の公開 API に混ぜると「どれが利用者向けか」が曖昧になる。ビルドルートの分離 (cross/ADR-0004) にも反する
- **iOS も Exec 方式に揃える**: 却下。標準アイテムが成立している側をわざわざ手動方式へ落とす理由がない

## Consequences

- 正: iOS は SDK 標準の経路に乗り、xcframework の生成・登録・パッケージングを自前で書かずに済む
- 正: Android も互換面の追加は Gradle の通常のモジュール追加で済み、Exec が呼ぶのは公開されたタスク名だけになる (SDK 内部ターゲットへの依存は `BeforeTargets` の 2 つに限られる)
- 負: iOS 側は MSB4120 の回避策を抱え続ける。MSBuild 側が直ったら `CreateNativeReference=false` とカスタムターゲットを外せる
- 負: Android 側は再ビルド判定を自前の入力リストで持つため、モジュール構成を変えたらここも直す必要がある
- 負: 同梱するだけの aar (Android Native ライブラリ) は Java 型解決に使われないため、互換面の内部実装が参照する型について binding 生成時に BG8605 / BG8606 の警告が出る。束縛対象の公開面には Native ライブラリの型が現れないため生成結果に欠落はない
- 先例への申し送り: KsSettingsView の「標準 `AndroidGradleProject` は複数モジュール構成で使えない」という理由付けは、Gradle 9 系ではより手前の理由 (init script のコンパイル失敗) で不成立になる。同リポジトリの該当 ADR を見直す際の材料になる

出典: kasane/changes/add-vertical-slice/design.md (Decision 7) の実測指示に対する結果

現行照合: 2026-08-15 確認。maui/macios/KsDialogs.Binding.iOS が `XcodeProject` アイテムで maui/macios/native/KsDialogsMauiBridge.xcodeproj を参照し、maui/android/KsDialogs.Binding.Android は gradlew の `Exec` + `AndroidLibrary` 束縛で maui/android/native の aar を取り込んでいる。判定: 維持

現行照合: 2026-09-02 確認。iOS 側の SDK 内部ターゲットへの依存が 2 か所増えた (add-maui-ios-bridge-verification)。maui/macios/KsDialogs.Binding.iOS/KsDialogs.Binding.iOS.csproj は `_ExpandNativeReferences` の後で xcframework 内の実バイナリを `_FileNativeReference` に足し (`_CreateBindingResourcePackage` の Inputs へ効く)、samples/maui/KsDialogs.Sample.Maui/KsDialogs.Sample.Maui.csproj は `_ComputeLinkNativeExecutableInputs` の後で静的 framework の実バイナリを `_LinkNativeExecutableInputs` に足す (`_LinkNativeExecutable` の Inputs へ効く)。どちらも互換面の Swift だけを直したときに Sample へ新しいバイナリが届くようにする、モノレポの ProjectReference 構成のための手当て (NuGet 利用者には現れない)。SDK の該当箇所 (.NET for iOS 26.1 / 26.5 で同一) と、SDK 更新時に確認する前提 (`_FrameworkNativeReference` の項目が framework 内の実バイナリのパスであること) は kasane/changes/archive/2026-09-02-add-maui-ios-bridge-verification/verification/incremental-build.md。判定: 維持
