# KMP 経路の VM 供給 — 実 framework 越しの検証の証跡

tasks 4.3 の確認記録 (2026-08-25)。対象は kmp-facade デルタスペックの
Requirement「iOS KMP 面の VM 供給」「共有層からの疎結合呼び出しの実証」。

## なぜ自動テストだけでは足りないか

KMP 向けの Swift 公開面 (`KsDialogsKmp`) は Swift のジェネリック型なので ObjC 面に出ない。
そのため Kotlin 側 (`kmp/ksdialogs-kmp/src/iosTest`) からは呼べず、逆に Swift のユニットテスト
(`ios/Tests`) は Kotlin の共有 VM の実インスタンスを作れない。両者の継ぎ目は
**実 framework を挟んだ成果物同士のビルドと、Sample の通し操作**でしか観察できない。

## 確認したこと

### 1. 共有 VM は ObjC のクラスとして framework に現れる

`evidence/kmp-framework-boundary.txt` (生成された ObjC ヘッダの抜粋)。
共有コードで定義した ViewModel は `SampleSharedBase` (NSObject 系) を継承した ObjC のクラスとして
公開される。Swift 側の紐付け表 (`DialogNotifierBindings`) がインスタンスの同一性で引き、
レジストリがクラスの同一性で引く前提が、この形で成立している。

同じ形の実例に対する挙動は Swift 側のテスト
`KsDialogsKmpModelBindingTests` の「ObjC のクラスとして見える共有 VM でも1引数 factory と
アクセサが同じに働く」が固定する。Kotlin 側からの解決 (共有 VM のクラスが Swift 側レジストリの
キーとして通用すること) は `InteropBridgeContractTests` が実 cinterop 越しに固定する。

### 2. 成果物同士がリンクする

- `cd kmp && ./gradlew :ksdialogs-kmp:linkDebugFrameworkIosSimulatorArm64` — 実 framework の生成
- `cd samples/kmp/iosApp && xcodebuild build -project KsDialogsSampleKmp.xcodeproj -scheme KsDialogsSampleKmp -destination 'platform=iOS Simulator,name=iPhone 17'` — 実 framework と Swift パッケージ (KsDialogs) を同時にリンクするアプリのビルド

結果は `evidence/kmp-model-binding-runs.txt`。

### 3. 共有層からの疎結合呼び出しが両 OS で成立する

Sample (KMP) の Basic Dialog を通す。呼び出しは共有コードの `SamplePresenter` に閉じており、
View の型を一切参照しない。View の登録だけが各 OS の Native API で行われる。

| OS | 端末 | 表示 | 結果 |
|---|---|---|---|
| iOS | Simulator (iPhone 17) | `evidence/kmp-sample-ios-basic-dialog-shown.png` | `evidence/kmp-sample-ios-result-completed.png` |
| Android | 実機 (`<android-serial>`、1080x2340) | `evidence/kmp-sample-android-basic-dialog-shown.png` | `evidence/kmp-sample-android-result-completed.png` |

どちらも結果表示エリアが `結果: completed(true)` になり、共有コードが宣言結果型 (Boolean) の
completed を受け取っている。

## Scenario との対応

| Scenario | 固定するもの | 置き場 |
|---|---|---|
| MB-KM-01 | 1引数 factory + KMP 面アクセサでの結果報告 | `ios/Tests/KsDialogsTests/KsDialogsKmpModelBindingTests.swift` |
| MB-KM-04 | アクセサの結果型不一致が型付きの失敗になること | 同上 |
| MB-KM-02 | 共有 VM に Native 拡張の `notifier` がそのまま効くこと | `kmp/ksdialogs-kmp/src/androidHostTest/kotlin/jp/kamusoft/ksdialogs/kmp/KmpViewModelSupplyTests.kt` |
| MB-KM-03 | UI 層参照なしの共有コードが型付き結果を受け取ること (両 OS) | `kmp/ksdialogs-kmp/src/commonTest/kotlin/jp/kamusoft/ksdialogs/kmp/SharedLayerCallTests.kt` + 本書「3.」の通し |

## 未確認として残ること

- ~~**Swift 側アクセサを Kotlin の実インスタンスに対して呼ぶ通し操作**は、Sample が KMP 面の
  1引数登録とアクセサを使うようになってから通せる (Sample のデモ追加は本変更の別タスク)。
  現時点で押さえているのは、同じ形の ObjC クラスに対する Swift 側テストと、上記のビルド・通しの成立まで~~
  → **解消 (tasks 5.4〜5.5、2026-08-25)**。KMP Sample の `Model Dialog` が、KMP 面の1引数登録
  (`Dialog.shared.kmp.register(ModelDialogViewModel.self) { viewModel in ... }`) と
  アクセサ (`Dialog.shared.kmp.notifier(for: viewModel)`) を、共有コードで生成された Kotlin の
  実インスタンスに対して使う。iOS Simulator での通し操作で completed / cancelled の両経路が
  共有 Presenter へ届くことを確認した (`evidence/kmp-ios-model-dialog-completed.png` /
  `evidence/kmp-ios-model-dialog-cancelled.png`)
- Android 側の「報告口で報告した結果が共有コードの show へ届く」経路は、提示先の画面を持てる
  テスト置き場が Native 側にしかないため、`KmpViewModelSupplyTests` では登録の解決と
  紐付けの不在までを見る。報告の配送そのものは Native の `DialogNotifierSupplyTests` が
  同じ契約の型で固定し、通しは上表の実機操作が受け持つ

## ファイル

| ファイル | 内容 |
|---|---|
| `evidence/kmp-framework-boundary.txt` | 生成された ObjC ヘッダの抜粋 (共有契約と共有 VM の実例) |
| `evidence/kmp-model-binding-runs.txt` | テスト実行とビルドの結果行 |
| `evidence/kmp-sample-ios-basic-dialog-shown.png` | iOS Simulator: 共有コードからの show で出たダイアログ |
| `evidence/kmp-sample-ios-result-completed.png` | iOS Simulator: 共有コードが受け取った結果 |
| `evidence/kmp-sample-android-basic-dialog-shown.png` | Android 実機: 共有コードからの show で出たダイアログ |
| `evidence/kmp-sample-android-result-completed.png` | Android 実機: 共有コードが受け取った結果 |
