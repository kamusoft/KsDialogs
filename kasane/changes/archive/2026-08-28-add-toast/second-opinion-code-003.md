# セカンドオピニオン: add-toast (code-003)
**相方**: codex / **label**: so-code-add-toast (セッション継続 turn 2) / **日付**: 2026-08-28 / **対象**: スコープ追加分の差分のみ — MAUI iOS の Loading / Dialog factory 例外境界修正 (BridgeContentSupply / 3 gateway / ApiDefinition / Swift bridge 6ファイル / iOS native factory 契約の throws 化 / 追加テスト)
---
# 追加差分レビュー結果

**判定: CHANGES_REQUESTED**

Critical 0件、Major 1件、Minor 2件、Suggestion 1件です。throws 化自体は合意済み差分として扱い、ホスト側のテスト結果も有効な前提としています。

## 指摘事項

### [🟠 Major] Loading / Dialogで元のfactory例外が呼び出し元から失われる

**該当箇所**:

- `maui/KsDialogs.Maui/Internals/BridgeContentSupply.cs:58`
- `maui/KsDialogs.Maui/Platforms/iOS/PlatformLoadingGateway.cs:80`
- `maui/KsDialogs.Maui/Platforms/iOS/PlatformDialogGateway.cs:60`

**問題点**:

`CreateOrFail` は元の例外を `Trace` に記録しますが、例外自体は破棄して `null` を返します。Nativeから戻ったとき、Loading / Dialogの呼び出し元へ届くのは、新しく生成された汎用的な `InvalidOperationException` です。

これにより、利用者factoryが投げた以下の情報がiOS経路だけ失われます。

- 例外の具体型
- 元のメッセージ
- スタックトレース
- 独自例外に付随するプロパティ

既存テスト `maui/KsDialogs.Maui.Tests/DialogCallContextTests.cs:60` は、factoryの `InvalidOperationException` と元のメッセージがそのまま伝播することを固定しています。ログに残すだけでは、呼び出し元のcatch処理やエラー表示に対する退行を補えません。

**推奨修正**:

Loading / Dialogでは、interopへは引き続き `null` を返しつつ、呼び出し単位の失敗ホルダーへ元の `Exception` または `ExceptionDispatchInfo` を保持してください。Nativeから `contentUnavailable` が返ったとき、ホルダーに原因があればそれをTaskへ設定し、Native固有の失敗なら現在のNSError変換を使う構造が適切です。

Toastはfire-and-forgetなので、現在どおりログ記録と破棄で問題ありません。

### [🟡 Minor] Dialogのfactory失敗後もpresentationがnotifierを保持する

**該当箇所**:

- `maui/macios/native/KsDialogsMauiBridge/MauiDialogBridge.swift:20`
- `maui/macios/native/KsDialogsMauiBridge/MauiDialogViewModel.swift:32`
- `maui/macios/native/KsDialogsMauiBridge/MauiDialogPresentation.swift:30`

**問題点**:

Dialogのfactoryでは、最初に `presentation.attach(notifier)` した後で `makeContentView()` を呼びます。providerが `nil` を返してthrowした場合、提示は失敗しますが、`MauiDialogPresentation` 内のnotifierは解除されません。

通常終了ではC#側の `presentation.Dismiss()` がnotifierを外しますが、提示失敗ではC#の結果チャネルが確定しないため、この経路も実行されません。presentation handleの解放まで不要な結果チャネルが保持されます。

**推奨修正**:

`makeContentView()` が成功した後に `presentation.attach(notifier)` する順序へ変更してください。事前に `dismiss()` された場合も、既存の `isDismissRequested` により取りこぼしは発生しません。

### [🟡 Minor] TS-CO-07の追加テストがインライン経路を通っていない

**該当箇所**:

- `ios/Tests/KsDialogsTests/ToastContractTests.swift:182`
- 契約: `kasane/changes/add-toast/specs/dialog-contract/spec.md:54`

**問題点**:

TS-CO-07は「例外を投げるfactoryによるインライン経路」を規定していますが、追加テストはfactoryをレジストリへ登録し、登録経路の `show` を呼んでいます。

内部の実体化処理は共有されていますが、公開インラインAPIから `ToastViewFactory` への変換や `acceptInline` の配線は検証されません。Scenario IDの網羅検査は成功しても、GIVENに対応していない状態です。

**推奨修正**:

少なくとも1本は次の形でインライン公開面を直接通してください。

```swift
try harness.toast.show(
    ToastTestViewModel(),
    duration: Self.shortDuration
) { _ throws -> UIView in
    throw ToastTestContentFailure.cannotMakeContent
}
```

現在の登録経路テストは、MAUI bridgeが使う経路の回帰テストとして別途残せます。

### [🔵 Suggestion] nullable interop境界そのものの実行テストがない

**該当箇所**:

- `maui/macios/KsDialogs.Binding.iOS/ApiDefinition.cs:29`
- `maui/macios/KsDialogs.Binding.iOS/ApiDefinition.cs:193`
- `maui/macios/KsDialogs.Binding.iOS/ApiDefinition.cs:301`
- `maui/KsDialogs.Maui.Tests/BridgeContentSupplyTests.cs:46`

**問題点**:

追加テストはC# helperとSwift nativeの失敗経路を別々に検証していますが、生成されたbindingを通してC# providerが実際に `null` を返す経路は実行していません。ビルド成功はAPI形状を証明しますが、元の問題だったmanaged/native callbackの実行時マーシャリングまでは防護しません。

**推奨修正**:

可能ならiOS向け結合テストで、C# providerが `null` を返すケースをToast / Loading / Dialogそれぞれ1回ずつ実行し、クラッシュせず期待した破棄・失敗通知になることを確認してください。

## 確認できた点

- 3 providerすべてでnullable注釈、Swift optional戻り値、`guard` が対応しています。
- Loadingはfactory成功前に合流数を増やさないため、throw後に状態が残りません。
- Dialog nativeのnotifier side tableは `defer` で解除されます。
- Toastのfactory例外はcoordinatorの受理後失敗経路へ到達しています。

**判定: CHANGES_REQUESTED**

## 突き合わせ結果

ホスト側 (review-003.md: Major 1 / Minor 2 / Suggestion 3) との突き合わせ (2026-08-28)。

| 指摘 | 出典 | 採否 | 根拠 |
|---|---|---|---|
| Loading / Dialog で元の factory 例外が呼び出し元から失われる | **双方一致** (相方 Major / ホスト Minor) | **確定・Major** (高い方を採用) | 既存テストが素通し伝播を固定・ホストは型付き例外の平坦化の非対称まで特定。ExceptionDispatchInfo 退避で修正 |
| Android の Toast provider が BridgeContentSupply を通らず、JavaProxyThrowable (Error 派生) が Toast coordinator の catch (Exception) を素通りしてプロセス落ち | ホストのみ | 確定 (Major) | javap + 生成バインディングでの裏取りあり。修正サイクルへ |
| Dialog の factory 失敗後も presentation が notifier を保持 (attach 順序) | 相方のみ | **採用** (Minor) | 該当箇所と機構の特定あり。attach を makeContentView 成功後へ |
| TS-CO-07 テストがインライン経路 (spec の GIVEN) を通っていない | 相方のみ | **採用** (Minor) | spec 適合の具体指摘。インライン公開面を直接通すテストを追加 |
| deviation.md の「ソース互換」表記が呼び出し側限定 | ホストのみ | 確定 (Minor・記録修正) | swiftc 最小再現あり。オーケストレーターが記録を修正済み |
| nullable interop 境界の実行時マーシャリングの結合テスト | 相方のみ (ホストも示唆) | **降格** (申し送り) | iOS bridge にテストターゲット不在。両端は自動テストで固定済み。蒸留への申し送り |

矛盾: なし。確定・採用分は修正サイクル2周目で処理する。
