# セカンドオピニオン: add-toast (code-001)
**相方**: codex / **label**: so-code-add-toast / **日付**: 2026-08-27 / **対象**: 未コミットの作業ツリー全体 (HEAD 5d446e2 の提案アーティキット以降の実装一式 — ios / android / kmp / maui / samples / core/layout-spec / scripts)
---
# 独立コードレビュー結果

Critical 0件、Major 2件、Minor 0件、Suggestion 0件です。合意済みの `deviation.md` は指摘対象から除外しています。

## Major 1: 期限切れの保留 Toast が表示される競合がある

該当箇所:

- `ios/Sources/KsDialogs/Presentation/ToastCoordinator.swift:168`
- `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/ToastCoordinator.kt:136`
- 契約: `kasane/changes/add-toast/specs/dialog-contract/spec.md:18`

問題点:

両実装とも、`attachIfPossible` が現在時刻と `deadline` を比較せず、器を取り付けています。また、タイマーの開始は取り付け処理より後です。

そのため、次の状況では期限切れ Toast が一度表示され得ます。

- UI スレッド／MainActorへの受理処理が duration より長く遅延した
- 提示先がないまま期限を迎えたが、タイマー処理より先にホスト復帰通知が実行された
- Androidでスリープやメインスレッド停止後、期限超過状態で処理が再開した

この場合、期限切れ Toast の View生成・取り付け・アクセシビリティ通知が行われ、その直後にタイマー側で消されます。「提示先が現れないまま duration が満了した表示は表示されずに破棄」という契約に反します。

推奨修正:

`beginDisplay` およびすべての `attachIfPossible` 経路で単調時計による期限確認を行い、期限到達済みなら器を作成・取り付けせず、その場で表示を破棄してください。ホスト復帰処理とタイマー処理の実行順に依存しない状態遷移にする必要があります。

回帰テストとして、「期限超過後、タイマー完了処理より先にホストを復帰させても、器の取り付けとアクセシビリティ通知が発生しない」ケースを両Nativeへ追加することを推奨します。

## Major 2: MAUI iOSのfactory例外が受理後失敗モデルを通らない

該当箇所:

- `maui/KsDialogs.Maui/Platforms/iOS/PlatformToastGateway.cs:41`
- `maui/macios/native/KsDialogsMauiBridge/MauiToastContent.swift:4`
- `maui/macios/native/KsDialogsMauiBridge/MauiToastBridge.swift:72`
- 契約: `kasane/changes/add-toast/specs/dialog-contract/spec.md:17`

問題点:

C#側の `MauiToastContent` provider 内では、次の処理が例外を送出できます。

- `request.CreateContent()`
- `PlatformDialogContent.Create(...)`
- `ResolveMauiContext()` 失敗時の明示的な `throw`

一方、Swift側では provider が非throwingな `() -> MauiDialogContent` として定義され、`makeContentView()` も非throwingです。したがって、C#の例外をSwiftの `Error` として `ToastCoordinator.beginDisplay` の `do/catch` へ伝える経路がありません。

managed/native callback境界を越えて例外が漏れ、プロセス終了などの未処理障害になる可能性があり、「警告ログを出し、その表示だけを破棄し、後続表示を継続する」という受理後失敗モデルを満たしません。

推奨修正:

C# callback内ですべての通常例外を捕捉し、ObjC互換の成功／失敗結果としてSwiftへ返してください。例えば、contentとNSError相当を持つ結果型、またはnullable contentと明示的な失敗情報を使用できます。Swift側ではその失敗をログ出力し、該当表示だけを破棄してください。

併せて、MAUI iOS bridgeについて次の回帰テストが必要です。

- View factoryが例外を投げてもプロセス障害にならない
- 失敗したToastが表示リストに残らない
- その後のToastが正常に表示される

依頼どおり、ファイル変更およびビルド・テストの再実行は行っていません。

**判定: CHANGES_REQUESTED**

## 突き合わせ結果

ホスト側 (review-001.md: Major 2 / Minor 1 / Suggestion 3) との突き合わせ。双方一致の指摘はなし (指摘領域が相補的)。

| 指摘 | 出典 | 採否 | 根拠 |
|---|---|---|---|
| 期限切れの保留 Toast が表示される競合 (Major 1) | 相方のみ | **採用** | 該当箇所特定 (両 Native の attach 経路に期限確認なし)・実害シナリオ具体的。契約「表示されずに破棄」(specs/dialog-contract/spec.md:18) への違反。ホスト側の見逃しとして修正サイクルへ |
| MAUI iOS の factory 例外が受理後失敗モデルを通らない (Major 2) | 相方のみ | **採用** | provider が非 throwing である構造的根拠と該当行の特定あり。managed/native 境界の未処理障害リスク。修正サイクルへ |
| Loading 演出中の前面化で中身が固まる (review-001 Major 1) | ホストのみ | 確定 (ホスト指摘として処理) | — |
| Sample メニュー非スクロール (review-001 Major 2) | ホストのみ | 確定 (オーナー裁定待ち) | — |

降格: なし / 未解決 (矛盾): なし。採用 2 件は review-001 の指摘と同格に修正サイクル 1 周目で処理する。
