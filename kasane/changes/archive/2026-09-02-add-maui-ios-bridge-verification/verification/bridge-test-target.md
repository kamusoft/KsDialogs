# bridge テスト標的の実行と検出力 (2026-09-02)

MAUI iOS の互換面 (bridge) に新設したテスト標的の、全件実行の方法・実測件数と、
テストが本当に退行を捕まえられるか (検出力) の確認記録。

## 全件実行

```
cd maui/macios/native
xcodebuild test -project KsDialogsMauiBridge.xcodeproj -scheme KsDialogsMauiBridge \
  -destination 'platform=iOS Simulator,name=iPhone 17'
```

- scheme は framework と共用の `KsDialogsMauiBridge`。テスト標的は TestAction にだけ結線してある
  (BuildAction は framework のみ — binding の xcframework 生成が呼ぶ経路と混ぜない)
- 機種名は `xcrun simctl list devices available` から選ぶ
- **件数は Swift Testing の行で読む**。XCTest 側は `Executed 0 tests, with 0 failures` と出るため、
  この行だけを見ると全件が 0 件に見える

実測 (2026-09-02):

| 行 | 値 |
|---|---|
| Swift Testing | `Test run with 6 tests in 3 suites passed` |
| XCTest | `Executed 0 tests, with 0 failures` (Swift Testing 製のみのため) |
| 終了 | `** TEST SUCCEEDED **` |

内訳は「互換面の提示先」2 件 (ホストアプリのシーンと key window / 公開 init での実提示) と
「中身なしの供給」4 件 (BV-MA-01 / 02 / 07 / 03)。suite 数の 3 は、この 2 つを束ねる
外側の直列化 suite を含む。

## Scenario と対応テスト

| Scenario | テスト |
|---|---|
| BV-MA-01 | `[BV-MA-01] Dialog は提示されず閉鎖の通知が失敗としてちょうど1回届く` (`BV_MA_01_dialogContentUnavailable`) |
| BV-MA-02 | `[BV-MA-02] Loading (表示形) は表示されず完了の通知に失敗が載って1回届く` (`BV_MA_02_loadingShowContentUnavailable`) |
| BV-MA-07 | `[BV-MA-07] Loading (スコープ形) は MAUI 側の処理も実行せずに失敗が1回届く` (`BV_MA_07_loadingStartContentUnavailable`) |
| BV-MA-03 | `[BV-MA-03] Toast はその1枚だけが破棄され、表示中の別の Toast は残る` (`BV_MA_03_toastContentUnavailable`) |

いずれも `maui/macios/native/KsDialogsMauiBridgeTests/MauiBridgeContentSupplyTests.swift`。

## Scenario ID の網羅検査

```
python3 scripts/scenario-id-coverage.py --selftest   # 自己テスト: 全件 OK
python3 scripts/scenario-id-coverage.py              # 結果: 未網羅なし (BV-MA 4/7・除外 3 件)
```

走査対象に `maui/macios/native/*Tests` を足し、BV-MA-04〜06 を理由つきの除外 ID として登録した
(判定の対象がテストの中ではなく、ビルド成果物とテスト標的そのものの実行可能性であるため)。

## 検出力の確認

互換面の 3 つの ViewModel (`MauiDialogViewModel` / `MauiLoadingViewModel` / `MauiToastViewModel`) の
`makeContentView` を、中身の供給が nil を返したときに失敗を投げず空の View を返す形へ**一時的に**
戻して実行した。結果は次のとおりで、4 本すべてが失敗した (確認後、3 ファイルとも元に戻してある)。

| テスト | 失敗した観測点 |
|---|---|
| BV-MA-01 | 閉鎖の通知が届かない (中身が空の View で提示されたまま残るため) |
| BV-MA-02 | 完了の通知に失敗の理由が載らない / Loading の器が key window に取り付いてしまう |
| BV-MA-07 | MAUI 側の処理が実行されてしまう / 完了の通知に失敗の理由が載らない |
| BV-MA-03 | 中身なしの 1 枚も器を作ってしまい、先に表示した器と一致しなくなる |

`Test run with 6 tests in 3 suites failed ... with 8 issues` (提示先の probe 1 本も、
前段が閉じないダイアログを残したことで巻き込まれて失敗した)。

### 取り付けの履歴を見る形にしたあとの再確認 (2026-09-02)

器の観測を「今の顔ぶれ」から「観測開始後に一度でも取り付いたか」へ広げたあと、同じ一時変更で
再確認した。結果は `Test run with 6 tests in 3 suites failed ... with 11 issues` で、4 本すべてが
引き続き失敗する (提示先の probe も同じく巻き込まれる)。観測点は次のとおり増えている。

| テスト | 失敗した観測点 |
|---|---|
| BV-MA-01 | 閉鎖の通知が届かない |
| BV-MA-02 | 完了の通知に失敗の理由が載らない / Loading の器が一度取り付いている (`LoadingContainerRootView`) |
| BV-MA-07 | 中身の供給が呼ばれない / MAUI 側の処理が実行されてしまう / 完了の通知に失敗の理由が載らない |
| BV-MA-03 | 取り付いた器が 3 枚になる / 今残っている器の枚数と、先に表示した器の同一性も崩れる |

### 一瞬だけ取り付いて撤去される退行の検出 (2026-09-02)

一時変更で潰せるのは「取り付いて居座る」退行までで、「取り付いた直後に撤去される」退行は
互換面の側からは作れない。そこで BV-MA-02 のテストに、要求の直後へ View を 1 枚足してすぐ外す
数行を**一時的に**入れて確認した (確認後に取り除き、差分が残っていないことを `git status` で確認済み)。

```swift
let transient = UIView()
BridgeTestHost.keyWindow?.addSubview(transient)
transient.removeFromSuperview()
```

同じテストに「今の顔ぶれだけを見る」旧来の観測 (`overlays.added.isEmpty`) も並べて 1 本だけ実行した
結果、旧来の観測は素通りし、履歴を見る観測 (`overlays.attachedEver`) だけが失敗した
(`Test run with 1 test in 2 suites failed ... with 1 issue`)。単体指定は Swift Testing の作法どおり
関数名に `()` を付けて `-only-testing:KsDialogsMauiBridgeTests/MauiBridgeSuite/ContentSupplyTests/BV_MA_02_loadingShowContentUnavailable()`
とし、件数が 1 件であることを確認している (0 件の偽 green を排除)。

## 観測点についての補足

Toast / Loading の器は提示機構を通らず key window の直下に重なるため、器の枚数と同一性は
window の subview として観測する。ただし提示機構が作る中間 View (`UITransitionView`) が
一度できると window に居座るため、**観測を始めた時点の顔ぶれを控えて、その後に増えた分だけを見る**
(`BridgeTestOverlayObserver`)。器の型は Native ライブラリの内部型でテスト標的からは見えないため、
型名で選ぶ形は採っていない。

「表示されない」の判定は、今の顔ぶれではなく**観測開始後に一度でも取り付いたか**で見る。
今の顔ぶれだけでは、取り付いた直後に撤去される退行を「一度も取り付かなかった」と読み違えるため。
履歴はテスト用ホストアプリの window (`BridgeTestHostWindow`) が取り付けの通知を受けて控え、
テスト標的はテスト可能性を有効にした取り込み (`@testable import KsDialogsMauiBridgeTestHost`) で読む
(scheme の TestAction は Debug 固定で、Debug は `ENABLE_TESTABILITY = YES`)。
履歴を控える window が提示先として解決できないときは値の不在 (nil) になり、テスト側は
`#require` で弾く — 観測できていないことが「取り付けが無かった」と読み替えられないようにしてある。

また、Toast / Dialog のように 1 プロセスの共有状態を使うテストは、本体と後片付けを
`BridgeTestSharedHost.run` で組み立てる。本体が途中で失敗しても片付けを実行し、本体が通ったときは
片付けの完了そのものも `#require` で見るため、片付け漏れが後続テストの順序依存の失敗に化けない。
