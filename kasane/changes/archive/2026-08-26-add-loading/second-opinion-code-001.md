# セカンドオピニオン: add-loading (code-001)
**相方**: codex / **label**: so-code-add-loading / **日付**: 2026-08-26 / **対象**: 作業ツリーの未コミット変更全体 (Loading 全4形態 + 共有部品切り出し + samples + 検査基盤)
---
# 判定: CHANGES_REQUESTED

Critical 0 / Major 3 / Minor 1 / Suggestion 0 です。

## [Major] factory 失敗後に Android coordinator の利用状態が残る

**該当箇所:** `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/LoadingCoordinator.kt:168`

**問題点:** `activeCount = 1` などの状態を確定した後、`startDisplay()` から利用者の factory を呼んでいます（同ファイル `:247-266`）。factory が例外を投げると `beginUse()` は token を返さないため `endUse()` が呼ばれず、`activeCount` と host 購読が残ります。その後の Loading は失敗した世代へ合流し、最終利用が終了してもカウントがゼロになりません。

さらに提示先不在で開始した場合、factory は `onHostChanged()`（同ファイル `:318-321`）まで遅延されます。ここでの例外は `ResumedActivityTracker.kt:76-77` の Activity ライフサイクル通知まで伝播し、既に action が始まった後にメインスレッドをクラッシュさせ得ます。これは factory 失敗を fail-fast に扱う契約に反します。

**推奨修正:** factory の生成成功後に状態を commit するか、例外時にカウント・世代・コンテンツ・host 購読を完全に rollback してください。提示先不在からの遅延生成では、action 開始後に lifecycle callback から factory を無保護で呼ばない設計が必要です。throw する factory について「通常開始」「提示先不在→復帰」のテストを追加し、利用数ゼロ、後続 Loading の正常動作、未捕捉例外なしを確認してください。

## [Major] Android で beginUse 完了直前のキャンセルにより表示が閉じ残る

**該当箇所:** `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/LoadingCoordinator.kt:153`、`android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/Loading.kt:68`

**問題点:** `beginUse()` は `withContext(Dispatchers.Main.immediate)` 内で利用数と表示状態を変更します。メインスレッドから呼び出し元 dispatcher へ結果を戻す直前に caller がキャンセルされると、`withContext` の prompt cancellation により token の受け取りだけが失敗し、変更済み状態は残ります。

`NonCancellable` による終了処理は `runScope()` の `:108-120` にありますが、この競合では `runScope()` 自体へ到達しないため機能しません。任意スレッドからの呼び出しと「キャンセルも合流1件の終了として数える」という契約に違反します。

**推奨修正:** acquire の commit 後に token 配送がキャンセルされた場合も rollback/endUse できる所有構造に変更してください。メイン側で commit した直後、呼び出し元の再開前にキャンセルする決定的テストを追加し、action 未実行、利用数ゼロ、器撤去済みを確認してください。

## [Major] MAUI の最終進捗が action 完了時に失われる

**該当箇所:** `maui/KsDialogs.Maui/Platforms/iOS/PlatformLoadingGateway.cs:100`、`maui/KsDialogs.Maui/Platforms/Android/PlatformLoadingGateway.cs:154`

**問題点:** action へ渡している `Progress<double>` は、`Report()` の handler を捕捉した `SynchronizationContext` または ThreadPool へ非同期 dispatch します。一方、action 完了直後には iOS の `actionCompletion()`（`:108`）、Android の `completion.Run()`（`:164`）を同期的に呼んでいます。

したがって、次の通常の形でも完了通知が進捗 callback を追い越せます。

```csharp
progress.Report(1.0);
return Task.CompletedTask;
```

Native coordinator は終了後の報告を `activeCount == 0` として捨てるため、最後の `1.0` が既定表示やカスタム VM に届かない可能性があります。MAUI の進捗パススルー契約を満たしません。

**推奨修正:** bridge の `Report` を直接同期呼び出しする専用 `IProgress<double>` 実装を使用するか、発行済み callback を drain してから action 完了を通知してください。両 OS の MAUI bridge テストへ「報告直後に同期完了する action」を追加し、完了通知より先に `1.0` が届くことを検証してください。

## [Minor] 新規コメントがソースコメント規約に違反している

**該当箇所:** `scripts/scenario-id-coverage.py:86`

**問題点:** コメントの `add-loading design Decision 5` は、変更識別子と議論内 Decision 番号への参照です。`kasane/concepts/cross/conventions/comment-policy.md:39-41` で禁止されている参照形式に該当します。

**推奨修正:** 参照部分を削除し、「LD の挙動領域は両 Native のミラー対象」のような自己完結した現在形の説明にしてください。

合意済みの `deviation.md` 4件および UI brief の差分は指摘から除外しました。指定どおりビルド・テストは再実行せず、提示された全ルート通過結果を前提にした静的レビューです。

---

## 突き合わせ結果 (2026-08-26, ksn-orchestrator)

ホスト側 review-001.md (CHANGES_REQUESTED: Major 1 / Minor 3 / Suggestion 4) との突き合わせ。両者の指摘に重複はなく、相互補完の関係だった。

| # | 出典 | 指摘 | 採否 | 根拠 |
|---|---|---|---|---|
| 1 | 相方 Major | Android coordinator: factory 例外時に利用状態が rollback されない + 提示先不在→復帰経路の lifecycle クラッシュ | **採用** (Major) | 該当箇所特定・実害シナリオあり。ホスト側の見逃し。iOS 側の同型経路も修正時に点検する |
| 2 | 相方 Major | Android beginUse: prompt cancellation で token 未受領のまま状態が残り閉じ残る | **採用** (Major) | 同上 (withContext の cancellation 意味論に基づく具体的競合) |
| 3 | 相方 Major | MAUI: Progress<double> の非同期 dispatch により最終進捗が action 完了通知に追い越されて失われる | **採用** (Major) | 同上。ホスト Minor「bridge 区間のテスト不在」と対 — 修正とテスト追加を併せて行う |
| 4 | 相方 Minor | scenario-id-coverage.py:86 のコメントが規約違反 (変更識別子 + Decision 通番の裸参照) | **採用** (Minor) | 現物と comment-policy.md で確認。既存 lint の検出範囲外の形式だった |
| 5 | ホスト Major | PB-TR-23 不安定で全件実行が green にならない | 確定 (ホスト指摘) | 選択肢 (a) テストのみの付随修正で対処 (待ちなし観察→waitUntil 化)。deviation に記録 |
| 6 | ホスト Minor ×3 / Suggestion ×4 | (review-001.md 参照) | 確定 (ホスト指摘) | 修正サイクルで対処 (Suggestion は裁量で同梱) |

未解決・矛盾: なし。相方由来の採用 3 Major + 1 Minor は以後ホスト側指摘と同格に扱う。
