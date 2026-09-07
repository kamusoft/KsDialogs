# セカンドオピニオン: add-vertical-slice (code-001)

**相方**: codex (ksn-reviewer 器) / **日付**: 2026-08-15 / **対象**: working tree の HEAD (main) に対する全差分 (ios/ android/ kmp/ maui/ samples/ + BuildProbe 削除 + maui/ADR-0003)

---

CHANGES_REQUESTED

Critical 0件、Major 4件、Minor 2件です。正常系の green 結果は確認済みの前提として扱いましたが、型消去境界・host 解決・失敗経路に契約違反が残っています。

## [Major] KMP iOS の結果型復元が実装されていない

該当箇所: [DialogGateway.kt](kmp/ksdialogs-kmp/src/commonMain/kotlin/jp/kamusoft/ksdialogs/kmp/DialogGateway.kt:47)、[KsDialogsInteropNotifier.swift](ios/Sources/KsDialogs/Interop/KsDialogsInteropNotifier.swift:19)

問題点: `outcome.value as R` は消去された型パラメータへの unchecked cast で、`R` の実型を検査しません。一方、Swift 側の報告口は `complete(Any)` のため、Boolean VM に文字列等を報告してもコンパイルできます。誤型は `show` で `DialogException` にならず、利用側で値を取り出した時点まで流出します。

これは「iosMain が宣言結果型へ復元し、失敗は Kotlin 例外 → NSError」とする `design.md` Decision 12、および kmp-facade spec に反します。現在の負のコンパイル検証も、この Swift 登録面の誤報告を覆っていません。

推奨修正: VM または登録情報に実行時の結果型記述子／変換器を持たせ、iOS 境界で値を検証・変換してください。不一致は `DialogException` にし、Swift へ NSError として返す必要があります。誤型値と nullable 値を含む境界テストも追加してください。

## [Major] iOS が非アクティブ／非 key window を提示先として採用する

該当箇所: [ApplicationKeyWindowProvider.swift](ios/Sources/KsDialogs/Presentation/ApplicationKeyWindowProvider.swift:8)

問題点: `foregroundActive` シーンがなければ全 connected scene にフォールバックし、key window がなければ単に最初の window を返しています。そのためバックグラウンドシーンや非表示 window に root controller があるだけで `canPresent == true` になります。表示されない提示に進んだ場合、`DialogPresenter` は結果通知を永久に待ちます。

「アクティブな host がなければ即座に throw し、キューイングしない」という契約に反します。

推奨修正: `foregroundActive` なシーンの key window のみに限定し、存在しなければ `nil` を返してください。inactive/background scene、active だが key window なし、複数 scene のケースをテストしてください。

## [Major] MAUI Android の View 生成例外で ShowAsync が完了しない

該当箇所: [MauiDialogBridge.kt](maui/android/native/ksdialogs-maui-bridge/src/main/kotlin/jp/kamusoft/ksdialogs/maui/MauiDialogBridge.kt:50)

問題点: fire-and-forget の `scope.launch` が捕捉するのは `DialogException` だけです。`contentProvider.createContentView()` や MAUI View の platform 化が `IllegalArgumentException` 等を投げると、listener の失敗通知が呼ばれません。

C# 側は結果チャネルと failure Task のどちらかを待つため、未処理例外でプロセスが落ちるか、`ShowAsync` が未完了になります。検証記録にある MaterialButton のテーマ例外もこの種類です。

推奨修正: `CancellationException` は再 throw し、それ以外の `Throwable` は境界で `listener.onFailed(...)` に変換してください。通知の exactly-once も保証し、content provider が例外を投げるテストを追加してください。

## [Major] MAUI の MauiContext 解決が任意スレッド・active host 契約を満たさない

該当箇所: [Android PlatformDialogGateway.cs](maui/KsDialogs.Maui/Platforms/Android/PlatformDialogGateway.cs:25)、[iOS PlatformDialogGateway.cs](maui/KsDialogs.Maui/Platforms/iOS/PlatformDialogGateway.cs:27)

問題点: `PresentAsync` は最初の await より前に `Application.Current.Windows` を列挙します。ワーカースレッドから呼ばれた場合もUI状態をそのスレッドで参照します。また、選択基準が「最初の MauiContext」で、Native 層が後から選ぶ resumed Activity／foreground scene と一致する保証がありません。

複数 window では、非アクティブ画面の Activity・theme context で View を生成し、別のアクティブ画面へ提示する可能性があります。現在の worker-thread テストは fake gateway のみで、実 platform gateway を通っていません。

推奨修正: context 解決と View 実体化を MainThread/Dispatcher 上で行い、Native が実際に解決した提示 host と同じ window/activity の MauiContext を使ってください。worker-thread と複数 window の adapter テストが必要です。

## [Minor] MAUI iOS Bridge だけ Swift 5 モードになっている

該当箇所: [project.pbxproj](maui/macios/native/KsDialogsMauiBridge.xcodeproj/project.pbxproj:197)

問題点: Native Swift Package は Swift tools 6.3かつ language mode 6ですが、MAUI bridge の Debug/Release はともに `SWIFT_VERSION = 5.0` です。Swift 6 の actor isolation／Sendable 検査をこの境界だけ回避しています。

推奨修正: Swift 6 language modeへ統一し、必要なら strict concurrency も明示してください。

## [Minor] untracked にローカル生成物と絶対パスが含まれている

該当箇所: [workspace-state.json](kmp/.swiftpm-locks/default/swiftPMCheckout/workspace-state.json:12)

問題点: `.swiftpm-locks` 配下にローカル checkout 状態、lock ファイル、開発者環境の絶対パスが含まれています。コミットすると非再現的で、環境依存情報も残ります。

推奨修正: `kmp/.swiftpm-locks/` を変更対象から除外し、適切な `.gitignore` に追加してください。

ビルド・テストは依頼どおり再実行していません。提示された4ルート green、負のコンパイル検証、6セルの実機証跡を前提にした静的レビューです。

---

## 突き合わせ結果 (ksn-orchestrator、2026-08-15)

ホスト側 review-001.md (CHANGES_REQUESTED / Major 3・Minor 2・Suggestion 3) との突き合わせ:

| # | 指摘 | 出典 | 採否 | 扱い |
|---|---|---|---|---|
| 1 | KMP 型消去復元の失敗が検出されない | 双方一致 (host M3 / codex M1) | **確定** (Major) | 方針 (a)/(b)/(c) の選択が契約・spec に触れるため **NEEDS_DISCUSSION** — 修正はオーナー判断後 |
| 2 | MAUI Android Bridge の catch が DialogException のみ | 双方一致 (host M2 / codex M3) | **確定** (Major) | 修正サイクルへ (catch-all + CancellationException 再 throw) |
| 3 | 器が閉じても結果未確定で show が宙吊り (iOS、Android 同型) | host のみ (M1)。MD-b 実測記録と一致 | **有効** (Major) | 手当て (器が外れたら未確定なら cancelled 確定) を修正サイクルへ。「下から閉じたときの契約確定」は NEEDS_DISCUSSION |
| 4 | iOS が非アクティブ/非 key window を提示先に採用 | codex のみ (M2) | **採用** (Major) — 該当箇所特定 + 実害シナリオ (表示されない提示で永久待ち) が具体的で、host 契約 (不在なら即 throw) への違反が明確 | 修正サイクルへ |
| 5 | MAUI MauiContext 解決が任意スレッド契約を満たさない | codex のみ (M4) | **採用** (Major) — 最初の await 前に呼び出しスレッドで UI 状態を列挙する点は「show は任意スレッドから呼べる」契約への明確な違反。複数 window の host 一致は縦串範囲では単一 window だが構造的指摘として妥当 | 修正サイクルへ (UI スレッドへのマーシャリング + Native 解決との整合) |
| 6 | iOS show が Task キャンセルに応答しない | host のみ (Minor 1) | **有効** (Minor) | #3 と同じ仕組みで修正サイクルへ |
| 7 | 互換面テストが非 Optional 契約に nil を渡す | host のみ (Minor 2) | **有効** (Minor) | 修正サイクルへ (即修正可) |
| 8 | MAUI iOS Bridge の Swift 5 モード | codex のみ (Minor) | **採用** (Minor) — 設定の非対称は実測済みで修正コスト極小 | 修正サイクルへ |
| 9 | .swiftpm-locks / samples/kmp 生成物のコミット可否 | 双方一致 (host S2 / codex Minor) | **確定** (Minor) | git 領域の方針判断のため **オーナー判断へ** (修正サイクルでは触らない) |
| 10 | Android Native の結果復元も unchecked cast (非対称) | host のみ (Suggestion) | 有効 (Suggestion) | #1 の方針決定と併せて整理 — 蒸留へ申し送り |
| 11 | MAUI Register の型引数 2 個の書き味 | host のみ (Suggestion) | 有効 (Suggestion) | phase-5 (API 表面の突き合わせ) へ申し送り |

- 矛盾する指摘: なし (再提示は不要)
- 降格: なし (相方指摘はいずれも該当箇所特定 + 実害シナリオ付きで根拠強)
- 修正サイクル対象: #2 #3(手当て) #4 #5 #6 #7 #8
- NEEDS_DISCUSSION (オーナー判断): #1 (KMP 復元方針)、#3 の契約確定 (下から閉じたときの上の扱い)、#9 (コミット方針)
