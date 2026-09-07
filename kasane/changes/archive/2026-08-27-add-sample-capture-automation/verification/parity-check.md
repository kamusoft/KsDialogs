# 撮影支援設定の4ルートパリティ突き合わせ (コードリーディング)

tasks.md 1.5 の突き合わせ結果。実機・シミュレータの通し検証ではなく、4ルートの実装コードを読んで
安定デモ ID・キー名・異常系挙動が同一かを確認したもの (実地の通し確認は 5.3 が受け持つ)。

対象コード (リポジトリルート相対):

| ルート | 引数の受け口 | デモ ID 定義 | 自動再生の入口 |
|---|---|---|---|
| android | `samples/android/app/src/main/kotlin/jp/kamusoft/ksdialogs/samples/android/SampleCaptureOptions.kt` | 同ディレクトリ `SampleDemoId.kt` | 同ディレクトリ `MainActivity.kt` |
| ios | `samples/ios/KsDialogsSample/SampleCaptureOptions.swift` | `samples/ios/KsDialogsSample/SampleDemoId.swift` | `samples/ios/KsDialogsSample/SampleCaptureAutoPlay.swift` + `SampleMenuScreen.swift` |
| maui | `samples/maui/KsDialogs.Sample.Maui/SampleCaptureOptions.cs` + `SampleCaptureArguments.cs` (+ `Platforms/iOS`・`Platforms/Android` の各 partial) | `samples/maui/KsDialogs.Sample.Maui/SampleDemoId.cs` | `samples/maui/KsDialogs.Sample.Maui/SampleCaptureAutoPlay.cs` + `SampleMenuPage.xaml.cs` |
| kmp | `samples/kmp/shared/src/commonMain/kotlin/jp/kamusoft/ksdialogs/samples/kmp/SampleCaptureOptions.kt` + `samples/kmp/iosApp/KsDialogsSampleKmp/SampleCaptureArguments.swift` / `samples/kmp/androidApp/src/main/kotlin/jp/kamusoft/ksdialogs/samples/kmp/android/MainActivity.kt` | `samples/kmp/shared/.../SampleDemoId.kt` | `samples/kmp/shared/.../SampleCaptureAutoPlay.kt` + `SamplePresenter.kt` + 各 OS UI 層 |

## 安定デモ ID (9件)

外部表現の文字列と、起動直後の期待状態に至る入口を突き合わせた。

| 安定デモ ID | android | ios | maui | kmp | 判定 |
|---|---|---|---|---|---|
| `basic-dialog` | `showBasicDialog()` | `model.showBasicDialog()` | `ShowBasicDialogAsync()` | 共有 Presenter `showBasicDialog()` | 一致 |
| `declarative-dialog` | `showDeclarativeDialog()` | `model.showDeclarativeDialog()` | `ShowDeclarativeDialogAsync()` | 共有 Presenter `showDeclarativeDialog()` | 一致 |
| `model-dialog` | `showModelDialog()` | `model.showModelDialog()` | `ShowModelDialogAsync()` | 共有 Presenter `showModelDialog()` | 一致 |
| `text-input-dialog` | `showTextInputDialog()` | `model.showTextInputDialog()` | `ShowTextInputDialogAsync()` | 共有 Presenter `showTextInputDialog()` | 一致 |
| `inline-dialog` | `showInlineDialog()` | `model.showInlineDialog()` | `ShowInlineDialogAsync()` | OS UI 層 `showInlineDialog()` (design Decision 3 どおり) | 一致 |
| `transition-dialog` | `openTransitionPanel()` | `showsTransitionPanel = true` | `OpenTransitionPanelAsync()` | OS UI 層のパネル起動 | 一致 (全ルート OS UI 層でパネルを開く) |
| `layout-dialog` | `openLayoutPanel()` | `showsLayoutPanel = true` | `OpenLayoutPanelAsync()` | OS UI 層のパネル起動 | 一致 (同上) |
| `default-loading` | `runDefaultLoading()` | `model.runDefaultLoading()` | `RunDefaultLoadingAsync()` | 共有 Presenter `runDefaultLoading()` | 一致 |
| `custom-loading` | `runCustomLoading()` | `model.runCustomLoading()` | `RunCustomLoadingAsync()` | 共有 Presenter `runCustomLoading()` | 一致 |

定義の並び順も4ルートで spec の表と同順。定義以外の ID を持つルートはない (各定義とも 9 件ちょうど)。

## キー名 (2件)

| キー | android | ios | maui | kmp | 判定 |
|---|---|---|---|---|---|
| `demo` | `DEMO_KEY = "demo"` | `demoKey = "demo"` | `DemoKey = "demo"` | `DEMO_KEY = "demo"` (共有側の公開定数を各 OS が使う) | 一致 |
| `loading-step-interval-ms` | `LOADING_STEP_INTERVAL_KEY` | `loadingStepIntervalKey` | `LoadingStepIntervalKey` | `LOADING_STEP_INTERVAL_KEY` (同上) | 一致 |

外部表現も design Decision 1 どおり (iOS 系は `--<キー> <値>` の隣接トークンペア、Android 系は同名キーの
string extra)。maui の Android 側は `Intent.Extras` の `GetString`、iOS 側は `NSProcessInfo.Arguments` の
`Array.IndexOf` 走査で、native / kmp と同型。

## 異常系 (4種)

| 異常系 | android | ios | maui | kmp | 判定 |
|---|---|---|---|---|---|
| 定義外の `demo` 値を無視 | `SampleDemoId.from()` が null → 自動再生なし | `SampleDemoId(rawValue:)` が nil → 同左 | `SampleDemoIds.From()` が null → 同左 | 共有 `SampleDemoId.from()` が null → 同左 | 一致 |
| `loading-step-interval-ms` の不正値で既定動作 | `toLongOrNull()` + 範囲 `1..600000`、外れたら null → 既定 400ms | `Int(_:)` + 範囲 `1...600000` → 既定 400ms | `int.TryParse` (符号のみ許可) + 範囲 `1..600000` → 既定 400ms | 共有側で `toLongOrNull()` + 範囲 `1..600000` → 既定 400ms | 一致 (下の「対処した食い違い」参照) |
| 値が無い・空文字はキーごと無視 | 空文字を `takeIf { isNotEmpty() }` で捨てる | キーが末尾なら nil、空文字も nil | `Value()` が空文字を null にする (両 OS 側とも) | 共有側で空文字を捨てる。iOS 側はキーが末尾なら nil | 一致 |
| 自動再生は起動につき1回 (one-shot) | 静的な消費フラグ + `savedInstanceState == null` の再生成検知 | `SampleCaptureAutoPlay.consumeDemo()` のプロセス内消費フラグ | `SampleCaptureAutoPlay.ConsumeDemo()` の静的消費フラグ | 共有 `SampleCaptureAutoPlay.consumeDemo()` のプロセス内消費フラグ + android 側は `savedInstanceState` 検知 | 一致 |

補足:

- 回転時の再発火: android / kmp-android は manifest に `configChanges` を持たないため Activity が再生成されるが、
  `savedInstanceState` 検知と消費フラグの二重で止まる。maui は `MainActivity` の `ConfigurationChanges` に
  `Orientation` を含むため再生成されず、加えて消費フラグで止まる。ios / kmp-ios は View 再構築でも消費フラグで止まる
- 刻み間隔の反映先: 4ルートとも Default / Custom の両 Loading で同じ値を使い、既定値は 400ms で一致
- 引数なし起動: 4ルートとも受け口が null を返すだけで画面・文言・挙動に痕跡が出ない

## 対処した食い違い

- **maui の数値解析が `+` 付きの値を弾いていた**: `int.TryParse` に `NumberStyles.None` を渡していたため、
  `+2000` のような先頭符号付きの値だけ maui が既定動作へ倒れ、他3ルート (Swift `Int(_:)` / Kotlin
  `toLongOrNull()` はいずれも先頭符号を受理する) と挙動が分かれていた。`NumberStyles.AllowLeadingSign` に
  変更して受理する文字列を揃えた (前後の空白・桁区切りを許さない点は変更前後で同じ)。
  対象: `samples/maui/KsDialogs.Sample.Maui/SampleCaptureOptions.cs`

## 既知の非対称 (合意済み)

- 同じキーが複数回現れたときの採用: iOS 系は最初の1組、Android 系は後勝ち。Android の Intent extra が
  Bundle 段階で1値に畳まれ、アプリから重複を観測できないプラットフォーム制約による (deviation.md に記録済み)

## 実装構造の差 (挙動には出ない)

- android native だけは one-shot の消費フラグを `MainActivity` の companion object に持ち、他3ルートのような
  `SampleCaptureAutoPlay` 型を持たない。挙動は同じ (プロセス内で1回) だが、置き場だけが非対称
- kmp は検証と設定への畳み込みを共有側 (`SampleCaptureOptions.from`) に置き、各 OS 側は文字列の取り出しだけを
  行う。native / maui は取り出しと検証を同じ型で行う。どちらも受理する値の集合は同じ
