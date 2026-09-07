# KMP Kotlin 経路のキャンセル追随と、KMP 登録コンテンツへの添付 — 検証の証跡

tasks 6.1 / 6.2 / 4.5 の確認記録 (2026-08-21)。
対象は kmp-facade デルタスペックの Requirement「Kotlin 経路の呼び出し元キャンセル追随 (iOS gateway)」と
「KMP 登録コンテンツへのトランジション添付」。

## どこで何を見たか (層別)

design Decision 7 の「MAUI / KMP は添付のパススルー検証のみ」に従い、層ごとに見る対象を分けた。

| 層 | 見たもの | 置き場 |
|---|---|---|
| KMP 共有コード (Kotlin) | 呼び出し元キャンセルが「当該 show だけの取り消し」へ届くこと・`CancellationException` が伝播すること | `kmp/ksdialogs-kmp/src/iosTest/.../IosDialogGatewayCancellationTests.kt` |
| KMP 登録面 (Swift) | KMP の登録面で登録した中身への添付が器に採用され、退出フックの完了後に結果が配送されること | `ios/Tests/KsDialogsTests/KsDialogsKmpTransitionTests.swift` |
| 実提示でダイアログが閉じること | 取り消し操作を受けた器が実際に閉じ、結果が cancelled でちょうど1回確定すること | 既存の `ios/Tests/KsDialogsTests/KsDialogsKmpCancellationTests.swift` (機械面の show ハンドル経由) |

### KMP の Kotlin テストで実提示まで見ない理由

KMP の iOS テスト (`iosSimulatorArm64Test`) は `UIApplicationMain` を通らない素の実行体で走るため、
前面でアクティブなシーンの key window が存在せず、**提示先が常に不在**になる
(既存の `InteropBridgeContractTests` が「提示できる画面がありません」の失敗で解決の成否を判定しているのと同じ制約)。
そのため Kotlin 側では、委譲面 (`IosDialogShowSurface`) を差し替えて
「取り消し操作がその show にちょうど1回届くか」「呼び出し元に `CancellationException` が伝播するか」を見る。
取り消し操作を受け取った先で実際に閉じるところは、Swift 側 (機械面の show ハンドル) のテストが担保する。

Android 側の KMP 経路には対応するテストを置いていない。Kotlin の suspend 呼び出しをそのまま
Native ライブラリへ委譲する構造で、キャンセルは委譲先の suspend 関数へ素通りするため、
KMP 層に固有の配管がない (Android Native の器側の追随は PB-AA-03 が見ている)。

## Scenario との対応

| Scenario | テスト名 | ルート |
|---|---|---|
| PB-KC-01 | `PB-KC-01 表示中のコルーチンをキャンセルすると当該ダイアログだけが閉じ CancellationException が伝播する` | kmp/ (`iosSimulatorArm64Test`) |
| PB-KC-02 | `PB-KC-02 提示が始まる前に来たキャンセルでも取り消しが届く` | kmp/ (`iosSimulatorArm64Test`) |
| PB-KC-02 | `PB-KC-02 キャンセルの後に届いた結果は呼び出し元へ配送されない` | kmp/ (`iosSimulatorArm64Test`) |
| PB-KC-03 | `[PB-KC-03] KMP 登録コンテンツへの添付が型付き面の show で採用される` | ios/ (Simulator) |
| PB-KC-03 | `[PB-KC-03] KMP 登録コンテンツへの添付が共有コードからの show で採用される` | ios/ (Simulator) |

## 実行方法と結果

```
cd kmp
./gradlew allTests --rerun-tasks
```

結果: **51 tests / 0 failures** (iosSimulatorArm64 29 + androidHostTest 22)。
うち本件の3件は `kmp-cancellation-test-results.xml` (Gradle が出力した JUnit XML) に残してある。

```
cd ios
xcodebuild test -scheme KsDialogs -destination 'platform=iOS Simulator,name=iPhone 17' \
  -only-testing:KsDialogsTests/KsDialogsKmpTransitionTests
```

結果: **2 tests / 1 suite / 0 failures** (`xcodebuild-kmp-transition.log`)。
ios/ の全件実行も 129 tests / 25 suites / 0 failures で通る。

## テストが空振りしていないことの確認

`IosDialogGateway` の `invokeOnCancellation` の結び付けを一時的に無効化して同じ実行を行い、
PB-KC-01 と PB-KC-02 (提示前のキャンセル) の2件が失敗することを確認した
(`kmp-cancellation-negative-check.log`)。確認後に元へ戻してある。
3件目 (キャンセル後に届いた結果を配送しない) はコルーチン側の性質なので、この無効化では落ちない。

## ファイル

| ファイル | 内容 |
|---|---|
| `kmp-cancellation-test-results.xml` | kmp/ の3件の実行結果 (JUnit XML) |
| `kmp-cancellation-negative-check.log` | 追随を無効化したときに2件が失敗する記録 |
| `xcodebuild-kmp-transition.log` | ios/ の PB-KC-03 2件の実行ログ |
