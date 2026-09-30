# KMP のテスト実行 (wait-for-host-appearance、tasks 7.1〜7.4)

## 環境と手順

- 実行日: 2026-09-27
- `kmp/` で `./gradlew --init-script <一時 init script> allTests --rerun-tasks`
- iosSimulatorArm64Test は、この実行のために `xcrun simctl create` で作った専用デバイス (iPhone 17 Pro / iOS 26.5) で走らせ、実行後に `xcrun simctl delete` で削除した。起動中・既存の Simulator は使っていない
  - 向け先の指定は、ビルド定義を変えない一時的な init script で行った。`iosSimulatorArm64Test` タスク (`KotlinNativeSimulatorTest`) の `device` プロパティに専用デバイスの UDID を入れる。タスクは standalone の既定のまま、そのデバイスを boot して走らせ、終了後に shutdown する

```kotlin
allprojects {
    tasks.matching { it.name == "iosSimulatorArm64Test" }.configureEach {
        @Suppress("UNCHECKED_CAST")
        val device = property("device") as org.gradle.api.provider.Property<String>
        device.set("<専用デバイスの UDID>")
    }
}
```

## 件数 (`ksdialogs-kmp/build/test-results/<ターゲット>/TEST-*.xml` の合計)

| ターゲット | tests | failures + errors | skipped |
|---|---|---|---|
| iosSimulatorArm64Test | 86 | 0 | 0 |
| testAndroidHostTest | 83 | 0 | 0 |

## この change で書き直した・足したテストの出現

| ID | ターゲット | テスト |
|---|---|---|
| DM-KM-01 | iosSimulatorArm64Test | `InteropBridgeContractTests` の `DM-KM-01 Swift 側登録の View factory が共有コードの ViewModel で解決される` |
| DM-KM-02 | testAndroidHostTest | `AndroidDialogGatewayContractTests` の `DM-KM-02 共有コードの ViewModel が Native レジストリのキーとして通用する` |
| DM-KM-03 | iosSimulatorArm64Test | `InteropBridgeContractTests` の `DM-KM-03 既定エントリの show は互換面と同じレジストリを引く` / `DM-KM-03 未登録の ViewModel の show は結果を返さずに失敗する` |
| PB-KT-09 | 両方 | iOS: `InteropBridgeContractTests`・`InteropToastBridgeContractTests`・`InteropLoadingBridgeContractTests` の各 `PB-KT-09 …` / Android: `AndroidDialogGatewayContractTests` の `PB-KT-09 型を渡す show の VM が Native レジストリで解決され提示先の出現を待つ` (と既存の `PB-KT-09 型を渡す show の VM が Native のインスタンス渡し show へ渡る`) |
| MB-KM-02 | testAndroidHostTest | `KmpViewModelSupplyTests` の `MB-KM-02 共有 VM に対する Native 拡張の notifier で報告できる` |
| PB-KC-04 | 両方 | iOS: `InteropBridgeContractTests` / Android: `AndroidDialogGatewayContractTests` の `PB-KC-04 待っている Dialog を共有コードで打ち切ると表示されずにキャンセルが伝播する` |
| TS-KM-02 | iosSimulatorArm64Test | `InteropToastBridgeContractTests` の `TS-KM-02 …` 2 本 |
| LD-KM-03 | iosSimulatorArm64Test | `InteropLoadingBridgeContractTests` の `LD-KM-03 …` 2 本 |

## 待ちの判定が空振りしないことの確認

「待っていることを確かめてから打ち切る」判定の補助関数 (iosTest の `cancelWhileWaitingForHost`) に、未登録の ViewModel の show を一時的に渡して走らせ、「待たずに失敗した」として落ちることを確かめた (失敗は約 12 ms で届き、待ちと判定するまでの 500 ms に十分収まる)。確認用の一時テストは実行後に削除した。
