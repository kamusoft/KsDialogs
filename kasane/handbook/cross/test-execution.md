---
kind: rule
applies-when:
  always: false
  tasks: [テストの実行, テスト結果の報告, 変更の完了判定, CI の検証範囲の確認]
title: テスト実行規約
description: 全ビルドルート (ios / android / android instrumented / kmp / maui / MAUI 互換面の Android・iOS) のテストの正しい実行コマンドと件数の得方、黙って空振りする範囲 (kmp の `test` 曖昧エラー・Swift Testing と XCTest の件数2系統・単体指定の `()`・実機がないと1件も走らない instrumented と API レベル別 skip・JVM / KMP / MAUI では実提示まで見ないテスト・ホストアプリなしの iOS テスト標的では提示先が得られない・フラグなしでは走らない負のコンパイル検証)、Android 本体の Compose 非依存を固定する依存グラフ検査、仕様の Scenario ID とテスト名の網羅検査、CI が回す範囲と手元に残る範囲
timestamp: 2026-09-08
---

# テスト実行規約

この文書は、各ビルドルートのテストを「実際に全件走らせる」ための実行方法と、**実行や検証が黙って空振りする範囲**を定める。読むと、どのコマンドで何が実行され、件数をどこで確認するかが分かる。

テストが 1 件も実行されなくてもコマンド自体は成功で終わり得るため、終了コードだけでは検証したことにならない。**実行件数を確認するところまでが検証**であり、テスト結果を報告するときはビルドルートを問わず実行件数 (`N tests / M failures`) を併記する。反復中の絞り込み実行は使ってよいが、**完了判定には絞り込みなしの全件実行を使う**。

コマンドと件数は 2026-09-07 に全 7 ルートを実測した (instrumented は API 29 / 33 / 36 の 3 台で全件成功)。テスト構成が育って実態が変わったら本規約を実測で更新する。

| ビルドルート | 全件実行コマンド | 実測件数 (実測日は上記の但し書き) |
|---|---|---|
| ios/ | `xcodebuild test -scheme KsDialogs -destination 'platform=iOS Simulator,name=iPhone 17'` | 277 tests (2026-09-07) |
| android/ | `./gradlew test --rerun-tasks` | 68 tests / 0 failures (2026-09-07) |
| android/ (instrumented) | `./gradlew connectedDebugAndroidTest` | 333 tests / 0 failures (`:ksdialogs` 294 + `:ksdialogs-compose` 39。1 台分の件数。API レベルによる skip あり — 後述。2026-09-07) |
| kmp/ | `./gradlew allTests --rerun-tasks` | 151 tests / 0 failures (iosSimulatorArm64 75 + androidHostTest 76。2026-09-07) |
| maui/ | `dotnet test` | 155 tests / 0 failures (2026-09-07) |
| maui/android/native/ | `./gradlew :ksdialogs-maui-bridge:test --rerun-tasks` | 31 tests / 0 failures (2026-09-07) |
| maui/macios/native/ | `xcodebuild test -project KsDialogsMauiBridge.xcodeproj -scheme KsDialogsMauiBridge -destination 'platform=iOS Simulator,name=iPhone 17'` | 7 tests / 4 suites (2026-09-07) |

**android/ 系の Gradle ビルドを同時に走らせない**: `maui/android/native/` は `android/` を複合ビルドで巻き込むため、`android/` のタスク (`test` / `connectedDebugAndroidTest`) と同時に実行すると build ディレクトリの取り合いで双方が壊れる (2026-08-26 実測)。上表の実行は逐次で回す。

これに加えて、公開 API 形状の検証が 2 種類ある (後述の「公開 API 形状の検証」)。正の検査は上表の既定実行に含まれるが、**負の検査 62 本はフラグを付けないと 1 本も走らない**。

android/ にはもう 1 つ、実行ではなく依存グラフを見る検査がある (後述の「本体の Compose 非依存の検証」)。こちらは上表の `./gradlew test` に含まれる。

実行とは別軸で、**仕様の Scenario ID がテスト名に網羅されているか**を見る検査がある (後述の「仕様とテストの対応の検査」)。どのビルドルートのコマンドにも含まれず、CI の lint job と手元の実行で回す。

## CI が回す範囲と手元に残る範囲

`develop` への push と `main` 宛ての pull request で検証 CI (`.github/workflows/ci.yml`) が起動する (cross/ADR-0016)。CI は上表の実行を 5 つの job に分けて回し、どの job も終了コードだけでなく**実行件数を検査**して 0 件なら失敗させる。lint job は起動のたびに必ず走る。

| CI の job | 回す範囲 |
|---|---|
| ios | 上表 ios/ の全件。スイート同士の並列実行は止めて回す (後述「CI の ios job はスイートを直列で回す」) |
| android | 上表 android/ の全件 (後述の Compose 非依存の依存グラフ検査を含む) |
| android-instrumented | 上表 android/ (instrumented) の全件。API 36 の Emulator 1 台 |
| kmp | 上表 kmp/ の全件と、階層化 source set の metadata compile |
| maui | 上表 maui/ と maui/android/native/ と maui/macios/native/ の全件、および platform TFM と binding のビルド |
| lint | secret scan・ローカル絶対パス検査・個体情報検査・コメント規約検査・仕様とテストの対応の検査 |

CI に載らない検証は**手元の完了判定に残る**。変更の完了を判定するときは CI の緑だけでは足りず、該当するものを手で回す。

- **instrumented の API 29 (旧経路)** — CI が回すのは API 36 の 1 台だけ。API レベルで走り分ける Scenario (後述) の API 29 側は手元のエミュレータで回す
- **負のコンパイル検証 62 本** (後述) — 「成功したら失敗」の判定を要し、フラグごとに 1 ビルドが要るため CI には載せない
- **実行ホストを起動しての確認** — 判定手順は [実行時挙動の検証規約](runtime-behavior-verification.md) が持つ

### CI の ios job はスイートを直列で回す

CI の ios job は `xcodebuild test` に `-parallel-testing-enabled NO` を付け、スイート同士の並列実行を止めて回す。手元 (12 論理 CPU 級) では既定の並列で 277 件が安定するが、GitHub のランナー (`macos-26-arm64`) は CPU が少なく、並列に走るスイートが提示・待ち合わせのために MainActor を取り合って、提示待ちのテストが時間切れになる。**手元の実行条件は変えない** (並列のまま)。この差は実行機の容量差であってテストや実装の欠陥ではないため、待ち時間の延長や観測点の変更で吸収しない。

同じ落ち方かどうかは、失敗の形で見分ける。次の 2 つが揃えば並列スイートの飢餓であり、待ち不足として扱わない:

| 見るもの | 飢餓のとき | 待ち不足のとき |
|---|---|---|
| 失敗までの時間 | 待ちの上限 (`DialogTestWaiting` の既定 5 秒) を大きく超える (初回観測は 28〜57 秒)。待ちループ自体が実行機会を得ていない | 上限のすぐ後 (5 秒台) |
| 落ちるテスト | 回ごとに入れ替わり、Toast / Dialog / Loading / ModelBinding など無関係な領域に散る | 同じテストが同じ箇所で落ち続ける |

直列化を外してよいのは、ランナーの容量が上がるか、提示・待ち合わせの構造が変わって並列で 2 回以上連続して全件が通ることを実測で示したときだけ。戻すときは同じ表で失敗の形を見てから判断する。

## ios/

```
cd ios
xcodebuild test -scheme KsDialogs -destination 'platform=iOS Simulator,name=<機種名>'
```

- scheme は `KsDialogs` (パッケージ全体)。`<機種名>` は `xcrun simctl list devices available` から選ぶ
- `swift test` は macOS 上での実行になる。ダイアログ実装は `#if canImport(UIKit)` でガードされており、**そのガード下のテストは失敗ではなく最初から存在しないものとして扱われる** (先例 KsSettingsView の実測では `swift test` 88件 / Simulator 全件 338件)。**完了判定は Simulator 実行を使う**
- Swift Testing のテストを単体指定するときは**関数名に `()` を付ける** (`-only-testing:KsDialogsTests/<Suite>/<func>()`)。付けないと filter が空振りして「0 tests / TEST SUCCEEDED」になる (偽の green)。絞り込み実行のあとは件数が 1 以上であることを見る

**件数は2系統ある**: Swift Testing のテストは `Test run with N tests ... passed` 行、XCTest のテストは `Executed N tests, with M failures` 行に出る。現在のテストはすべて Swift Testing 製のため XCTest 側は `Executed 0 tests` と表示される — **`Executed` 行だけ見ると全件が 0 件に見える**。両方の行を確認して合算する

## android/

```
cd android
./gradlew test
```

- 実行されるのは `testDebugUnitTest` (release variant の単体テストタスクは現構成では走らない)
- Gradle は up-to-date なテストタスクをスキップするため、**差分なしの再実行は「テスト 0 件で BUILD SUCCESSFUL」になり得る**。全件を確実に回し直すときは `--rerun-tasks` を付ける
- 件数はコンソールに出ない。`ksdialogs/build/test-results/testDebugUnitTest/TEST-*.xml` の `tests` / `failures` 属性で確認する (unit test を持つのは `:ksdialogs` だけ。`:ksdialogs-compose` と `:api-surface-check` は `NO-SOURCE` になる)
- この実行には、本体の Compose 非依存の検査 `:ksdialogs:verifyNoDeclarativeUiDependency` も乗る (後述の「本体の Compose 非依存の検証」)
- SDK の場所は `android/local.properties` の `sdk.dir` で指定する (VCS 管理外)。未作成だと `SDK location not found` でビルド自体が失敗する

## android/ (instrumented)

```
cd android
adb devices          # 実機かエミュレータが1台以上 device 状態で並ぶことを先に確認する
./gradlew connectedDebugAndroidTest
```

- 実 View のレイアウト・実入力の注入・実ウィンドウの見えを測るテストはここにしかない。**`./gradlew test` では 1 件も走らない**
- 端末が 1 台も繋がっていないと `No connected devices!` でタスクが失敗する。複数台繋がっているときは `ANDROID_SERIAL=<serial>` で 1 台に絞る (指定しないと全台で回る)
- 件数はコンソールに `Tests N/M completed` として流れるが、**モジュールごとに別々の行として流れる**。確定値は次の2か所の `tests` / `failures` 属性を合算して確認する — 片方だけを見ると全体件数を取り違える
  - `ksdialogs/build/outputs/androidTest-results/connected/debug/TEST-*.xml` (従来 View 系)
  - `ksdialogs-compose/build/outputs/androidTest-results/connected/debug/TEST-*.xml` (宣言的 UI)
- 端末の画面がロックされていると入力注入系が落ちるため、解除した状態で回す

### API レベルで走る / 走らない Scenario

**API レベルで走る / 走らない Scenario がある**。旧経路 (`systemUiVisibility`) のシステムバー引き継ぎなど、特定の API レベルでしか成立しない挙動のテストは `assumeTrue` で自分を skip し、結果 XML では `skipped` に数えられる。全件を実行したと言えるのは**対象 API レベルをそれぞれ1台ずつ回したとき**で、手元の環境は API 29 のエミュレータ (`ksn_api29`、旧経路用) と API 30 以上の実機 / エミュレータの組み合わせ。件数表の 333 は 1 台分で、skip の内訳は XML の `skipped` 属性で確認する。API 依存の `assumeTrue` はすべて API 30 を境にしており、skipped は API 30 以上で 1 (IME の出し入れは API 30 以上でだけ判定できる — 逆向きの 1 本)・API 29 で 6 (2026-09-06 実測)

**戻る操作を数えるテスト用画面は 2 経路の受け皿を持つ**。テスト APK の targetSdk は compileSdk (36) に追随し、予測型バック (predictive back) が既定で有効な端末では `Activity.onBackPressed()` が呼ばれず、システム既定の戻るで画面が閉じる。戻るが届いたことを数える画面は、`onBackPressed()` に加えて API 33 以上で `OnBackInvokedDispatcher` にコールバックを登録する (`ToastInputTestActivity` が先例。同時に両方へ届くことはない)。片方だけだと API レベルによって「戻るが届かない」のではなく「画面が破棄される」形で落ちる

### JVM では走らない Scenario

**出入りの演出・撤去後の配送・システムバー・ウィンドウ変化の Scenario (ID が `PB-TR` / `PB-SB` / `PB-WN` / 実提示の `PB-MD`) は JVM では 1 本も走らない** (すべて instrumented)。`./gradlew test` の JVM テストには、器をウィンドウに載せずに配送を即時にするテスト用の提示面があり、「配送は撤去の後」という順序はそこでは観察できない。演出と配送順序の退行を見るなら instrumented まで回す

### 証跡の取り出し

**証跡 (テストが端末へ保存したスクリーンショット) を取り出すときは `adb shell am instrument` で直接回す**。`connectedDebugAndroidTest` は実行後にテスト APK をアンインストールし、テスト package の外部ファイル領域に保存した画像も一緒に消える。撮影はテストに計測用の引数 (`-e ksdialogsEvidence 1`) を与えたときだけ行われる:

```
cd android
./gradlew :ksdialogs:assembleDebugAndroidTest
adb -s <serial> install -r -t ksdialogs/build/outputs/apk/androidTest/debug/ksdialogs-debug-androidTest.apk
adb -s <serial> shell am instrument -w -e class <テストクラス> -e ksdialogsEvidence 1 \
  jp.kamusoft.ksdialogs.test/androidx.test.runner.AndroidJUnitRunner
adb -s <serial> pull /sdcard/Android/data/jp.kamusoft.ksdialogs.test/files/evidence/
```

## kmp/

```
cd kmp
./gradlew allTests
```

- **`./gradlew test` は使えない** (実測: `Task 'test' is ambiguous ... Candidates are: 'testAndroid', 'testAndroidHostTest'` で失敗する)。全ターゲット集約タスクは `allTests`
- 実行ターゲットは `iosSimulatorArm64Test` と `testAndroidHostTest` の2つ
- 件数は `ksdialogs-kmp/build/test-results/<ターゲット名>/TEST-*.xml` で確認する。up-to-date スキップと `--rerun-tasks` の注意は android/ と同じ
- composite build で android/ を巻き込むため、`kmp/local.properties` も必要 (android/ と同じ `sdk.dir`。未作成だと `SDK location not found` で失敗する)

### iosSimulatorArm64Test は framework の ObjC ヘッダも作る

**`iosSimulatorArm64Test` は debug framework のリンクを伴う**。Swift / ObjC から見える面の検査 (`ObjCApiSurfaceTests`) が、生成された ObjC ヘッダを読んで「共有 Kotlin コード専用の呼び出しが ObjC の面に出ていない」ことを見るためで、ヘッダの場所はビルド定義がテストへ環境変数で渡す (シミュレータで走るテストへ環境変数を届けるには `SIMCTL_CHILD_` の接頭辞が要る)。ヘッダを読めなければその検査は失敗する — 素通りにはならない

### iosSimulatorArm64Test では実提示が起きない

**`iosSimulatorArm64Test` では実際の提示が起きない**。テストの実行体は `UIApplicationMain` を通らず key window が無いため、提示先が常に不在になる (既存の `InteropBridgeContractTests` が `No screen is available to present the Dialog.` の失敗で解決の成否を判定しているのと同じ制約)。Kotlin 側のテストは委譲面の差し替えで配管 (取り消しがちょうど1回届く・`CancellationException` の伝播) だけを見ており、実際に閉じることは ios/ 側のテスト (`KsDialogsKmpCancellationTests` 等) が担保する。KMP 経由で「画面に出た」ことを見たければ ios/ のテストか Sample を使う

## maui/

```
cd maui
dotnet test
```

- 件数はコンソール出力の `合計: N、合格: n、失敗: m` (環境言語により英語表記) で確認する

### `dotnet test` が巻き込むもの

**`dotnet test` は Native 2実装のビルドを巻き込む**。facade は iOS / Android の Binding プロジェクトを ProjectReference しており、その先で **Xcode ビルド** (`maui/macios/native/KsDialogsMauiBridge.xcodeproj` → xcframework) と **Gradle ビルド** (`maui/android/native` の互換面 aar + Android Native の aar) が走る。素の `net10.0` のユニットテストだけを回すつもりでも初回は数分かかり、**Native 側のビルド失敗が「テストの失敗」として現れる** — エラーの出所がどちらかを先に切り分ける

### 互換面のテスト (Android / iOS)

互換面 (Kotlin) のテストは .NET 側からは走らない。**別ビルドルートとして明示的に実行する**:

```
cd maui/android/native
./gradlew :ksdialogs-maui-bridge:test --rerun-tasks
```

件数の確認と up-to-date スキップの注意は android/ と同じ。この 31 件が MAUI Android 経路の「結果がちょうど1つ届く」「演出の完了通知がちょうど1回届く」「factory の失敗が 1 枚だけの破棄に合流する」保証を持つ。

互換面 (Swift) のテストも同じく .NET 側からは走らない。bridge の Xcode プロジェクトを scheme 指定で回す:

```
cd maui/macios/native
xcodebuild test -project KsDialogsMauiBridge.xcodeproj -scheme KsDialogsMauiBridge \
  -destination 'platform=iOS Simulator,name=<機種名>'
```

- scheme は framework と共用の `KsDialogsMauiBridge`。テスト標的とテスト用ホストアプリは TestAction にだけ結線してあり、BuildAction (binding の xcframework 生成が呼ぶ側) には入らない
- 件数の 2 系統と単体指定の `()` は ios/ と同じ。現在のテストはすべて Swift Testing 製で、XCTest 側は `Executed 0 tests` になる
- この 7 件が MAUI iOS 経路の「中身なしの供給が互換面の失敗として届く」保証 (Scenario ID `BV-MA-`) を持つ

テスト標的は Host Application (`KsDialogsMauiBridgeTestHost`) 付きで走る。互換面は公開 init で実物の提示先解決 (前面アクティブなシーンの key window) を使うため、ホストアプリなしの Unit Testing Bundle では window を作って key にしてもシーンが前面アクティブにならず、提示先不在で 1 本も提示に入れない (2026-09-02 実測)。bridge のテストを増やすときはこの標的に足し、ホストなしの標的を別に作らない。

maui の完了判定には `dotnet test`・Android 互換面・iOS 互換面の **3 つの実行すべて**が要る。

### 演出の動きは検証できない

**MAUI のユニットテストは演出の「動き」を検証できない**。MAUI のアニメーション API (`TranslateToAsync` 等) はハンドラに載っていない View では `Unable to find IAnimationManager` で fault し、ライブラリ側のアダプタがそれを吸収するため、テストで見えるのは「フックが呼ばれ完了が返る」経路までである。プリセットが実際に動くことの確認は Sample の実機 / シミュレータ通しで行う

## 公開 API 形状の検証

利用者から見た公開面 (公開すべき型が見えるか・引数の形・結果型の導出) は、実行ではなく**コンパイルが通るかどうか**で検証する。検証の置き場は利用者と同じ可視性の境界にそろえてある — android / kmp は friend path を持たない別モジュール `api-surface-check`、maui は `InternalsVisibleTo` の外の別プロジェクト `KsDialogs.Maui.ApiSurfaceCheck`、ios はテストターゲット内の非 `@testable` なファイル。テストの friend 可視性を通してしまうと、公開すべき型が誤って internal になっても検査が成功してしまう。

### 正の検査 (既定の実行に含まれる)

「あるべき呼び出しが通る」側は既定のビルドに入っており、追加の操作は要らない。上表のコマンドを回せば次がコンパイルされる:

| ルート | 既定の実行が通す対象 |
|---|---|
| ios/ | テストビルドに同梱 (`ios/Tests/KsDialogsTests/DialogAttributeCompileChecks.swift` / `DialogTypedResultCompileChecks.swift`) |
| android/ | `./gradlew test` → `:api-surface-check:compileDebugKotlin` |
| kmp/ | `./gradlew allTests` → `:api-surface-check:compileKotlinIosSimulatorArm64` |
| maui/ | `dotnet test` → `KsDialogs.Maui.ApiSurfaceCheck` のビルド (テストプロジェクトが ProjectReference で巻き込む) |

### 負の検査 (フラグなしでは走らない) — 62 本

「あってはならない呼び出しが弾かれる」側は、**ビルドが失敗することが期待結果**の条件つきコンパイルで行う。検証用のソースは既定のビルドから除外されているため、**フラグを付けずに実行すると何も検証されないまま成功する**。

禁止形状ごとに別のフラグへ分けてあり、**1 回のビルドに入る誤りは 1 つだけ**である。62 本を 1 本ずつ順に回し、それぞれが下表の診断で失敗することを確認する。診断は原則 1 件だが、**呼び出し面がオーバーロードされていると候補不適合の行が先行して 2 件出る**ほか、**解決できない型を import と使用の両方で書いていると 2 件出る** — その場合は下表に両方を書いてある。まとめて回すと、別の誤りの診断で失敗しても「効いた」と読めてしまい、個別の禁止形状を証明できない。

ルートごとのコマンドの形:

```
cd ios
xcodebuild build-for-testing -scheme KsDialogs -destination 'platform=iOS Simulator,name=<機種名>' \
  OTHER_SWIFT_FLAGS='$(inherited) -D<フラグ>'

cd android
./gradlew :api-surface-check:compileDebugKotlin -P<フラグ> --rerun-tasks

cd kmp
./gradlew :api-surface-check:compileKotlinIosSimulatorArm64 -P<フラグ> --rerun-tasks

cd maui
dotnet build KsDialogs.Maui.ApiSurfaceCheck -p:<フラグ>=true
```

| ルート | フラグ | 期待する診断 |
|---|---|---|
| ios/ | `KSDIALOGS_NEGATIVE_CHECK_VM_ATTRIBUTE` | `value of type 'ConsumerDialogViewModel' has no member 'proportionalWidth'` |
| ios/ | `KSDIALOGS_NEGATIVE_CHECK_SHOW_OPTIONS` | `extra argument 'options' in call` |
| ios/ | `KSDIALOGS_NEGATIVE_CHECK_RESULT_TYPE` | `cannot assign value of type 'DialogResult<…Result>' (aka 'DialogResult<Bool>') to type 'DialogResult<String>'` |
| ios/ | `KSDIALOGS_NEGATIVE_CHECK_NOTIFIER_VALUE` | `cannot convert value of type 'String' to expected argument type '…Result' (aka 'Bool')` |
| ios/ | `KSDIALOGS_NEGATIVE_CHECK_SHOW_TRANSITION` | `extra argument 'transition' in call` |
| ios/ | `KSDIALOGS_NEGATIVE_CHECK_OPTIONS_TRANSITION` | `value of type 'DialogOptions' has no member 'transition'` |
| ios/ | `KSDIALOGS_NEGATIVE_CHECK_NONE_ARGUMENT` | `argument passed to call that takes no arguments` |
| ios/ | `KSDIALOGS_NEGATIVE_CHECK_DEFAULT_DURATION` | `'defaultDuration' is inaccessible due to 'internal' protection level` |
| ios/ | `KSDIALOGS_NEGATIVE_CHECK_VALUE_TYPE_VIEW_MODEL` | `non-class type 'ConsumerValueDialogViewModel' cannot conform to class protocol 'DialogViewModel'` |
| ios/ | `KSDIALOGS_NEGATIVE_CHECK_LOADING_SHOW_STYLE` | `extra argument 'style' in call` |
| ios/ | `KSDIALOGS_NEGATIVE_CHECK_LOADING_SHOW_OPTIONS` | `extra argument 'options' in call` |
| ios/ | `KSDIALOGS_NEGATIVE_CHECK_TOAST_HIDE` | `value of type 'any KsToast' has no member 'hide'` |
| ios/ | `KSDIALOGS_NEGATIVE_CHECK_TOAST_SHOW_RESULT` | `cannot convert value of type '()' to specified type 'String'` |
| ios/ | `KSDIALOGS_NEGATIVE_CHECK_TOAST_SHOW_STYLE` | `extra argument 'style' in call` |
| ios/ | `KSDIALOGS_NEGATIVE_CHECK_TOAST_OPTIONS` | `value of type 'any KsToast' has no member 'options'` |
| ios/ | `KSDIALOGS_NEGATIVE_CHECK_LEGACY_CONTRACT_NAME` | `cannot find type 'KsDialogs' in scope` |
| android/ | `ksdialogs.negativeCheck.vmAttribute` | `Unresolved reference 'proportionalWidth'.` |
| android/ | `ksdialogs.negativeCheck.showOptions` | `None of the following candidates is applicable:` と `No parameter with name 'options' found.` (show がオーバーロードされているため 2 件出る) |
| android/ | `ksdialogs.negativeCheck.resultType` | `Return type mismatch: expected 'DialogResult<String>', actual 'DialogResult<Boolean>'.` |
| android/ | `ksdialogs.negativeCheck.notifierValue` | `Argument type mismatch: actual type is 'String', but 'Boolean' was expected.` |
| android/ | `ksdialogs.negativeCheck.showTransition` | `None of the following candidates is applicable:` と `No parameter with name 'transition' found.` (show がオーバーロードされているため 2 件出る) |
| android/ | `ksdialogs.negativeCheck.optionsTransition` | `Unresolved reference 'transition'.` |
| android/ | `ksdialogs.negativeCheck.noneArguments` | `Too many arguments for 'fun none(): DialogTransition'.` |
| android/ | `ksdialogs.negativeCheck.loadingShowStyle` | `None of the following candidates is applicable:` と `No parameter with name 'style' found.` (show がオーバーロードされているため 2 件出る) |
| android/ | `ksdialogs.negativeCheck.loadingShowOptions` | `None of the following candidates is applicable:` と `No parameter with name 'options' found.` (同上) |
| android/ | `ksdialogs.negativeCheck.toastHide` | `Unresolved reference 'hide'.` |
| android/ | `ksdialogs.negativeCheck.toastShowResult` | `Initializer type mismatch: expected 'String', actual 'Unit'.` |
| android/ | `ksdialogs.negativeCheck.toastShowStyle` | `None of the following candidates is applicable:` と `No parameter with name 'style' found.` (show がオーバーロードされているため 2 件出る) |
| android/ | `ksdialogs.negativeCheck.toastOptions` | `Unresolved reference 'options'.` |
| android/ | `ksdialogs.negativeCheck.toastComposeFromCore` | `Unresolved reference 'showCompose'.` (import と呼び出しの 2 件出る) |
| android/ | `ksdialogs.negativeCheck.legacyContractName` | `Unresolved reference 'KsDialogs'.` (import と引数の型の 2 件出る) |
| kmp/ | `ksdialogs.negativeCheck.optionsType` | `Unresolved reference 'DialogOptions'.` |
| kmp/ | `ksdialogs.negativeCheck.showOptions` | `No parameter with name 'options' found.` |
| kmp/ | `ksdialogs.negativeCheck.resultType` | `Return type mismatch: expected 'DialogResult<String>', actual 'DialogResult<Boolean>'.` |
| kmp/ | `ksdialogs.negativeCheck.loadingStyleType` | `Unresolved reference 'LoadingStyle'.` |
| kmp/ | `ksdialogs.negativeCheck.loadingStyleProperty` | `Unresolved reference 'style'.` |
| kmp/ | `ksdialogs.negativeCheck.toastStyleType` | `Unresolved reference 'ToastStyle'.` |
| kmp/ | `ksdialogs.negativeCheck.toastStyleProperty` | `Unresolved reference 'style'.` |
| kmp/ | `ksdialogs.negativeCheck.toastRegistration` | `Unresolved reference 'register'.` (レジストリに View factory の登録 API が無い) |
| kmp/ | `ksdialogs.negativeCheck.loadingRegistration` | `Unresolved reference 'register'.` (同上) |
| kmp/ | `ksdialogs.negativeCheck.toastHide` | `Unresolved reference 'hide'.` |
| kmp/ | `ksdialogs.negativeCheck.toastShowResult` | `Initializer type mismatch: expected 'DialogResult<Boolean>', actual 'Unit'.` |
| kmp/ | `ksdialogs.negativeCheck.legacyContractName` | `Unresolved reference 'KsDialogs'.` (import と引数の型の 2 件出る) |
| maui/ | `KsDialogsNegativeCheckVmAttribute` | CS1061 (`ConsumerDialogViewModel` に `ProportionalWidth` がない) |
| maui/ | `KsDialogsNegativeCheckShowOptions` | CS1739 (`options` という名前のパラメーターがない) |
| maui/ | `KsDialogsNegativeCheckResultType` | CS0029 (`DialogResult<bool>` → `DialogResult<string>` に変換できない) |
| maui/ | `KsDialogsNegativeCheckNotifierValue` | CS1503 (`string` → `bool` に変換できない) |
| maui/ | `KsDialogsNegativeCheckNotifierType` | CS0311 (`ConsumerDialogViewModel` → `IDialogViewModel<string>` の変換がない) |
| maui/ | `KsDialogsNegativeCheckSimpleRegister` | CS0311 (`ConsumerTextDialogViewModel` → `IDialogViewModel` の変換がない) |
| maui/ | `KsDialogsNegativeCheckShowTransition` | CS1739 (`ShowAsync` に `transition` という名前のパラメーターがない) |
| maui/ | `KsDialogsNegativeCheckOptionsTransition` | CS0117 (`Dialog` に `GetTransitionDuration` がない。MAUI の `DialogOptions` は internal で検査側から見えないため、演出を静的メタ属性へ畳み込んだ場合に生える添付スカラーを的にしている) |
| maui/ | `KsDialogsNegativeCheckNoneArguments` | CS1501 (引数 1 個の `None` オーバーロードがない) |
| maui/ | `KsDialogsNegativeCheckLoadingShowStyle` | CS1739 (`ShowAsync` に `style` という名前のパラメーターがない) |
| maui/ | `KsDialogsNegativeCheckLoadingShowOptions` | CS1739 (`ShowAsync` に `options` という名前のパラメーターがない) |
| maui/ | `KsDialogsNegativeCheckTypedShowContract` | CS0311 (`string` → `IDialogViewModel` の変換がない。契約外の型での型指定 show を弾く) |
| maui/ | `KsDialogsNegativeCheckNotifierResultType` | CS0029 (`DialogNotifier<bool>` → `DialogNotifier<string>` に変換できない) |
| maui/ | `KsDialogsNegativeCheckValueTypeViewModel` | CS0452 が 2 件 (値型 ViewModel を登録と型指定 show の両方で弾く — 検査ソースが 2 箇所あるため 2 件出る) |
| maui/ | `KsDialogsNegativeCheckToastHide` | CS1061 (`IKsToast` に `Hide` がない) |
| maui/ | `KsDialogsNegativeCheckToastShowAwait` | CS4008 (void は待機できない) |
| maui/ | `KsDialogsNegativeCheckToastShowReturn` | CS0029 (void を object に変換できない) |
| maui/ | `KsDialogsNegativeCheckToastShowStyle` | CS1739 (`style` という名前のパラメーターがない) |
| maui/ | `KsDialogsNegativeCheckLegacyContractName` | CS0246 (型または名前空間の名前 `IKsDialogs` が見つからない) |

**成功したら検証は失敗**である。判定を誤らないよう、出た診断が上表と一致するところまで確認する。フラグ名の一覧の正は各検査モジュールのビルド定義 (`android/api-surface-check/build.gradle.kts` / `kmp/api-surface-check/build.gradle.kts` / `maui/KsDialogs.Maui.ApiSurfaceCheck/KsDialogs.Maui.ApiSurfaceCheck.csproj`) と、ios は検査ファイル冒頭の doc comment にある。

## 本体の Compose 非依存の検証

Android 本体 `jp.kamusoft:ksdialogs` は宣言的 UI (Compose) に依存しない。View 系だけを使う消費者 — 特にバインディング経由で本体を取り込む MAUI Android — へ compose-ui の推移的依存を持ち込まないための境界であり、**推移的な混入も含めて**依存グラフの走査で固定してある。

```
cd android
./gradlew :ksdialogs:verifyNoDeclarativeUiDependency
```

- 検査対象は消費者へ配られる 4 つの classpath (`debug` / `release` それぞれの compile / runtime)。テスト専用の classpath は対象外なので、テスト側が Compose を使うことは妨げない
- **成功が期待結果**である (負のコンパイル検査とは逆向き)。混入があると `<classpath 名> に Compose 系 artifact が混ざっています: <group:module>` で失敗する
- この検査は `check` だけでなく `test` にも結線してあり、上表の android/ の全件実行 `./gradlew test --rerun-tasks` に含まれる。個別に回す必要はない (タスクグラフに入っていることは `./gradlew test --dry-run` で確認できる)
- 検査が空振りしていないことは、本体へ一時的に compose 系依存を足すと `./gradlew test` が上記の診断で失敗することで確かめた (2026-08-19 実測)

## 仕様とテストの対応の検査

挙動系の共通仕様は `<機能面>-<領域>-<NN>` の安定 ID (Dialog 系は `PB-`、Loading 系は `LD-`、Toast 系は `TS-`、MAUI 互換面の検証は `BV-` で始まる) を持つ Scenario として書かれ、同じ ID を名前に含むテストで固定される (core/ADR-0016)。その対応が漏れていないかは、実行ではなく**仕様の見出しとテストの宣言の突合**で検査する:

```
python3 scripts/scenario-id-coverage.py             # 既定の仕様置き場 (進行中 + archive) とテスト置き場で突合
python3 scripts/scenario-id-coverage.py --require-mirror  # 両 Native ミラー必須領域が片側にしか無い ID を検出
python3 scripts/scenario-id-coverage.py --selftest  # 正規化と判定が壊れていないことの確認
```

- 仕様にあってテストに無い ID があれば終了コード 1。テストにあって仕様に無い ID (名前の打ち間違い) は警告で、終了コードは変えない
- **ID を数えるのはテストの宣言 (関数名と直前の属性・注釈) だけ**。コメント・説明文・証跡ファイル名にしか ID が無いものは網羅と見なさない — ソース全文から拾うと、テストを 1 本も書かずにコメントへ ID を書くだけで「テストあり」と読めてしまう
- 言語ごとのテスト名の表記差 (Swift `[PB-MD-01] …` / Kotlin・C# `PB_MD_01_…` / Kotlin のバッククォート名 `PB-KC-01 …`) は検査側が正規化する
- 自動テストで受け止められない Scenario (Sample の通し確認など) は検査の除外 ID として登録してあり、除外は結果に理由つきで表示される
- **実行契機は CI の lint job と手元の 2 つ**。CI は起動のたびに回す。手元では Scenario テストを足した後と、変更のレビュー前に回す。実行件数と同じく、結果の「未網羅なし」まで見る

## 先例にある未実測の落とし穴

KsSettingsView リポジトリ: concepts「テスト実行規約」には、本リポジトリにまだ対応する実物がない実測記録がある — Robolectric legacy graphics での描画系アサーションの空振り (実 ellipsize・singleLine TextView の実描画位置) 等。**本規約へは書き写さない** (このリポジトリで実測していない手順は書かない)。同系のテストを書く段になったら先例を参照し、本リポジトリで実測してからここへ追記する。

## 関連

- [実行時挙動の検証規約](runtime-behavior-verification.md) — ユニットテストの green だけでは完了にならない不具合の完了判定
