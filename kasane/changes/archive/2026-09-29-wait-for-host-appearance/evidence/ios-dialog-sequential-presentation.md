# 実 UIKit での待っていた Dialog の連続提示 (wait-for-host-appearance、tasks 5.4)

design の Risks「UIKit の連続提示」の確認。待っていた 2 枚が、実物の UIKit の提示機構の上で呼んだ順に提示され、後の 1 枚が手前に重なることを確かめた (dialog-contract PB-HW-06 の実 UIKit 版)。

## 手段

- MAUI の iOS ブリッジのテスト標的 (`maui/macios/native/KsDialogsMauiBridgeTests`) に、テスト `待っていた 2 枚は、提示先が現れると呼んだ順に提示され、後の 1 枚が手前に重なる` を足した (`maui/macios/native/KsDialogsMauiBridgeTests/MauiBridgeHostWaitTests.swift`)
  - この標的はホストアプリ (`KsDialogsMauiBridgeTestHost`) のシーンと key window の上で走るので、提示先の解決も提示も本番の既定の面 (`ApplicationKeyWindowProvider`・`UIKitDialogPresentationSurface`) を通る。Native ライブラリの単体テスト (`ios/`) はシーンを持たないテストランナーで走り、提示遷移が完走しないので、この確認には使えない
  - 提示先の不在は、ホストアプリの key window を隠して作る (前面でアクティブなシーンに key window が無くなる)。提示先の出現は、同じ window を `makeKeyAndVisible()` で key に戻して作る (window が key になった通知で合図が届く)
- 手順
  1. key window を隠し、提示先が無いことを確かめる
  2. 互換面 (`MauiDialogBridge`) で 2 枚 (A、B の順) を提示する
  3. 300 ms 待ち、失敗の通知・中身の供給・提示のどれも起きていないことを確かめる
  4. window を key に戻す
  5. ホストアプリの root から `presentedViewController` を辿り、1 段目の器に A の中身、2 段目の器に B の中身が載っていること、B の器の提示元が A の器であることを確かめる
  6. 2 枚を閉じ、それぞれの閉鎖の通知が `dismissed` で届くことを確かめる

## 結果

- 環境: iPhone 17 / iOS 26.5 の Simulator (この作業のために新しく作ったデバイス)、Xcode 26.5
- 実行: `maui/macios/native` で `xcodebuild test -project KsDialogsMauiBridge.xcodeproj -scheme KsDialogsMauiBridge -destination 'platform=iOS Simulator,name=<作業用デバイス>' -parallel-testing-enabled NO`
- 4 回実行し、4 回とも成功した (1 回目は `xcodebuild test`、2〜4 回目は同じビルドの `test-without-building`)。各回の件数行は `Test run with 10 tests in 6 suites passed` (XCTest 側は `Executed 0 tests`)。対象のテストは各回 0.87〜0.89 秒で通った
- 手順 5 の検査がすべて通ったので、待っていた 2 枚は実 UIKit の上で A、B の順に提示され、B が A の手前に重なった。手順 3 で、待っている間に失敗も中身の供給も起きないことも確かめた

## この確認が見ていないこと

- 提示先の出現を、シーンのアクティブ化の通知 (`UIScene.didActivateNotification`) で起こす経路は、この手段では作れない (ホストアプリのシーンは常に前面でアクティブ)。この経路は Native の単体テスト PB-HI-01 (通知で合図が届く) と、tasks 9.3 の Sample の起動直後の実測で押さえる
- 提示の途中に次の show が呼ばれる場合 (PB-HW-08) は、Native の単体テスト (提示面の完了通知を保留する差し替え) で確かめた。実 UIKit では再現していない

## 追記: show の入口で「呼んだ順」が崩れる穴 (2026-09-27)

上の 4 回の成功の後、同じテストが既定の並列実行で間欠的に落ちることが分かった。切り分けの結果、列 (`DialogHostWaitQueue`) と明け方は正しく、穴は列の入口より手前にあった。

- iOS の `Dialog.show` は MainActor の付かない既定の非隔離 async 関数だったため、UI スレッドから呼んでも、いったん大域の実行器へ移ってから MainActor の提示処理へ戻る。A、B の順に呼んでも、戻る順が入れ替わって列に B、A で並ぶことがあった
- KMP の iOS 互換面 (`KsDialogsInteropBridge.show`) は同期関数の中で `Task { @MainActor in ... }` を作って提示処理へ入るため、列の順は Task の開始順だけで決まっていた

### 直したこと

- `Dialog` の show 群 (インスタンス渡し・インライン 2 種・型指定) と内部の `showInline`、`KsDialog` の要件 4 本と拡張 6 本を `nonisolated(nonsending)` にした。show は呼び出し元の実行文脈のまま始まり、UI スレッドから呼べば UI スレッドを離れずに列まで進む。公開 API の見た目 (`async throws`) と「任意のスレッドから呼べる」契約は変えていない
- 列に順番札を足した。札は任意のスレッドから同期で取れる (`DialogHostWaitQueue.reserve()`、Presenter の口は `DialogPresenter.reserveTurn(on:)`)。列は札の小さい順に並び、先に取られてまだ列に着いていない札がある間は、それより後の札の show を明けない。札を取らない呼び出し (Native の show) は、列に着いた時点で札を取る。札が列に着かずに終わった (未登録・同じ VM が表示中などで失敗した、札を捨てた) ときは、手放すか解放された時点で後ろが進む
- KMP の iOS 互換面は、Task を作る前に札を取り、提示処理へ渡すようにした
- 提示先があって列が空 (未着の札も無い) なら、札があっても今までどおり番を持たずにその場で提示する

### 互換性の実測 (公開プロトコルの要件の隔離を変えることの影響)

パッケージの外から準拠する最小の再現 (ライブラリ側のパッケージと、それに依存する別パッケージ) を macOS 上の `swift build` (Swift 6.3.2) で組んで確かめた。

- ライブラリ側: プロトコルの要件・拡張・具体型の実装を `nonisolated(nonsending)` にしたもの。`-enable-library-evolution` と `.swiftinterface` の出力でも通り、interface に `nonisolated(nonsending)` が出る
- 外部の準拠型 4 種 (既定の非隔離 async・`@concurrent`・`@MainActor` クラス・actor) を、3 つの言語設定 (Swift 6 言語モード、Swift 5 言語モード、`NonisolatedNonsendingByDefault` を有効にしたもの) でそれぞれ置き、すべて警告もエラーも無くコンパイルできた
- 入口の順序 (MainActor から Task を 8 本続けて作り、MainActor の関数へ入った順を記録する。500 回)

| ライブラリ側の指定 | 拡張経由 (`any P`) の順序崩れ | 具体型経由の順序崩れ | 入口が UI スレッド以外で走った回数 |
|---|---|---|---|
| 指定なし (修正前と同じ) | 342/500 | 202/500 | 8000/8000 |
| 実装と拡張だけ指定し、要件は指定なし | 197/500 | 0/500 | 4000/8000 |
| 要件・拡張・実装すべてに指定 (採用) | 0/500 | 0/500 | 0/8000 |

要件を変えないと、DI で `KsDialog` として受け取った呼び出しでは順序が保てない。同じことを iOS のテストでも確かめた (下の「要件だけ戻した版」)。

確かめていない面: iOS 17 の実行環境 (手元に iOS 17 の Simulator ランタイムが無い。iOS 18.6 と iOS 26.5 では下のテストが通った)、利用者が準拠型をバイナリ配布の framework として持つ場合の ABI (要件の呼び出し規約が変わるので、準拠型を含むバイナリは組み直しが要る)。

### テストと検出力

Native (`ios/Tests/KsDialogsTests`):

- `DialogHostWaitTests` の `[PB-HW-06] 待っている Dialog が複数あると…` を、A の後に列の待ちを挟まずに A、B を続けて呼ぶ形にした。表示の順は中身に付けた識別子 (ViewModel の message) で読む
- 同 `[PB-HW-08] …追い越さずに後ろに並ぶ` は、A の提示中 (完了を保留) に B、C を待ち合わせを挟まずに続けて呼び、A、B、C の順に重なることを見る形にした。A の提示中という前提を作るため、A と B の間の待ち合わせは残している
- 同 `[PB-HW-06] UI スレッドから続けて呼んだ show は、呼び出しから戻る前に呼んだ順で列に並ぶ` を足した (iOS 26 以上。`Task.immediate` で UI スレッドの上で同期的に始め、A は具体型、B は `KsDialog` 経由で呼ぶ)。呼び出しから戻った直後に待ちの数が 2 であることを見るので、確率に頼らずに入口の実行文脈を判定できる
- `KsDialogsKmpFacadeTests` に、KMP の iOS 互換面を UI スレッドから続けて 2 回呼んだ直後に待ちの数が 2 であり、提示先が現れると A、B の順に重なるテスト (`[PB-HW-06] 共有コードから続けて呼んだ show は…`)、提示先があって列が空なら前の提示の完了を待たずに表示するテスト、列に着かずに失敗した show が後ろを止めないテストを足した
- `DialogHostWaitTests` に、未着の札がある間は後の show が明けず、手放すと明けるテストと、捨てた札が解放で手放されるテストを足した

修正前の実装との比較は、`ios/` を作業用の場所へ写し、修正だけを外して (`nonisolated(nonsending)` を外し、互換面の札を取らない形に戻して) 走らせた。

| 対象 | 修正前 | 修正後 |
|---|---|---|
| `[PB-HW-06] 待っている Dialog が複数あると…` (300 回) | 16/300 で失敗 (B、A の順に表示) | 300/300 成功 |
| `[PB-HW-08] …追い越さずに後ろに並ぶ` (300 回) | 18/300 で失敗 (A、C、B の順に表示) | 300/300 成功 |
| `[PB-HW-06] UI スレッドから続けて呼んだ show は…` | 毎回失敗 (呼び出し直後の待ちの数が 0) | 成功 |
| `[PB-HW-06] 共有コードから続けて呼んだ show は…` | 毎回失敗 (呼び出し直後の待ちの数が 0) | 成功 |
| 要件だけ戻した版の `[PB-HW-06] UI スレッドから…` | 毎回失敗 (待ちの数が 1。`KsDialog` 経由の B だけが UI スレッドを離れる) | — |

300 回の実行は `xcodebuild test-without-building … -only-testing:<2 本> -test-iterations 300` (既定の並列)。

### MAUI の iOS ブリッジの連続提示 (5.4) の安定性

`MauiDialogBridge.swift` は触らずに、Native の修正だけで回した (`-test-iterations 15`、全 10 件を 15 回)。

| 実行 | 対象のテストの成功 | 件数行 |
|---|---|---|
| 既定の並列 | 15/15 | `Test run with 150 tests in 90 suites passed` |
| 直列 (`-parallel-testing-enabled NO`) | 15/15 | `Test run with 150 tests in 90 suites passed` |

ブリッジは今も `Task { @MainActor in … }` の開始順に頼っている (同じスレッドから続けて作った MainActor の Task は、この環境では作った順に始まっている)。札を使う形への書き換えはブリッジ側の作業で行う。

### 全件実行 (iPhone 17 / iOS 26.5 の作業用デバイス)

| ルート | 実行 | Swift Testing | XCTest |
|---|---|---|---|
| `ios/` | 既定の並列 | `Test run with 341 tests in 57 suites passed` | `Executed 0 tests, with 0 failures` |
| `ios/` | 直列 | `Test run with 341 tests in 57 suites passed` | `Executed 0 tests, with 0 failures` |
| `maui/macios/native` | 既定の並列 | `Test run with 10 tests in 6 suites passed` | `Executed 0 tests, with 0 failures` |
| `maui/macios/native` | 直列 | `Test run with 10 tests in 6 suites passed` | `Executed 0 tests, with 0 failures` |
| `kmp/` の `iosSimulatorArm64Test` (作業用デバイス。向け方は evidence/kmp-test-run.md と同じ) | — | tests 86 / failures 0 / errors 0 / skipped 0 (結果 XML の合計) | — |

iOS 18.6 の作業用デバイスでも `DialogHostWaitTests`・`KsDialogsKmpFacadeTests` の 25 件が通った (iOS 26 以上の 1 件は skip)。
