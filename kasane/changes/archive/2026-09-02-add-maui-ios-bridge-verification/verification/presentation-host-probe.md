# プローブ: xctest ランナー内で実物の提示先が得られるか (2026-09-02、提案段階)

## 問い

bridge の互換面は公開 init (`Dialog()` / `Loading()` / `Toast()`) で実物の提示先解決 (`ios/Sources/KsDialogs/Presentation/ApplicationKeyWindowProvider.swift` — 前面アクティブなシーンの key window) を使う。bridge のテスト標的 (ホストアプリなしの iOS Unit Testing Bundle) からこの経路で実際に提示できるか。

## 方法

ios/ のテスト標的に一時テスト 1 本を置き (実行後に削除、コミットしない)、`xcodebuild test -scheme KsDialogs -destination 'platform=iOS Simulator,name=iPhone 17' -only-testing:...` で実行した。テストの内容:

1. `UIApplication.shared.connectedScenes` の件数と状態を記録
2. `UIWindow(frame:)` を作り `rootViewController` を置いて `makeKeyAndVisible()`、300 ms 待つ
3. `ApplicationKeyWindowProvider().keyWindow` が作った window を返すか
4. 公開 init の `Dialog()` に ViewModel 型を登録して `show` し、factory が呼ばれるか

## 結果 (要約。生ログは環境パスを含むため保存しない)

| 観測 | 値 |
|---|---|
| 接続シーン数 (window 作成前) | 0 |
| 作成した window のシーン付与 (`windowScene != nil`) | true |
| 作成した window が key か (`isKeyWindow`) | true |
| `ApplicationKeyWindowProvider().keyWindow` | **nil** (前面アクティブなシーンが無い) |
| `Dialog().show(...)` | **`DialogError.presentationHostUnavailable`** で失敗。factory は呼ばれない |
| Swift Testing 件数行 | `Test run with 1 test in 1 suite failed ... with 2 issues` |

## 結論

ホストアプリなしの Unit Testing Bundle では、window を作って key にしてもシーンは前面アクティブにならず、既定の提示先解決は成立しない。bridge の nil 供給経路 (BV-MA-01〜03・07) は「提示先が確保できた後」に通るため、**このままではテスト標的から到達できない**。実現経路の選択 (テストのホストアプリ / bridge への注入口 など) は提案の設計判断として確定してから実装する。

ios/ の既存テストは偽の提示面を内部 init で注入しており (`ios/Tests/KsDialogsTests/Support/DialogTestHarness.swift`)、この経路には当たらない。

---

# 再確認: テスト用ホストアプリ内で提示先が得られるか (2026-09-02、実装時)

## 問い

上のプローブで不成立だった経路を、テスト用ホストアプリ (最小の UIKit アプリ target) 付きの
テスト標的で走らせたとき、前面でアクティブなシーンと key window が得られ、公開 init の
`Dialog()` で実提示が起きるか。

## 方法

`maui/macios/native/KsDialogsMauiBridge.xcodeproj` にホストアプリ target
`KsDialogsMauiBridgeTestHost` (AppDelegate がシーン構成を返し、SceneDelegate が空の
root view controller を持つ window を key にする) と、それを Host Application とする
ユニットテスト標的 `KsDialogsMauiBridgeTests` (Swift Testing) を追加し、次で実行した。

```
cd maui/macios/native
xcodebuild test -project KsDialogsMauiBridge.xcodeproj -scheme KsDialogsMauiBridge \
  -destination 'platform=iOS Simulator,name=iPhone 17'
```

観測したのは `maui/macios/native/KsDialogsMauiBridgeTests/MauiBridgePresentationHostTests.swift`
の 2 本 (シーンと key window の有無 / 公開 init の互換面が実提示に入るか)。

## 結果 (要約)

| 観測 | 値 |
|---|---|
| 前面でアクティブな `UIWindowScene` | あり |
| key window (`isKeyWindow` かつ前面アクティブなシーン配下) | 得られる |
| key window の `rootViewController` | あり (提示の起点になる) |
| `MauiDialogBridge().present(...)` の中身の供給 | 呼ばれる (1 回) |
| 提示の結果 | 実提示される (提示先不在の通知にならない)。`dismiss()` で閉鎖の通知が `dismissed` として 1 回届く |
| Swift Testing 件数行 (この 2 本のみの時点) | `Test run with 2 tests in 1 suite passed` |

## 結論

**成立**。ホストアプリのプロセス内では既定の提示先解決が通り、BV-MA-01〜03・07 の GIVEN
(提示先が確保できる状態) をテストから作れる。提案段階で検討した bridge への注入口は不要。

ホストアプリは framework の scheme の BuildAction には入れず、TestAction からのみ組み立てる。
binding の xcframework 生成はこの scheme のビルドを呼ぶため、混ぜると配布物のビルドに
テスト用 target が巻き込まれる。
