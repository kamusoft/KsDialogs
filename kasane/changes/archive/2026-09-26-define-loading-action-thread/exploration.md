# Exploration: define-loading-action-thread

## 課題 / 動機

ColorAnalyzer からの知らせ (`../ColorAnalyzer/kasane/outbox/KsDialogs/2026-09-25-loading-action-thread-undefined.md`、kind: bug) を起点とする。

Loading のスコープ形 (`IKsLoading.StartAsync(viewModel, action)`) で、action がどのスレッドで動くかが iOS と Android で食い違っている。

- iOS: メインスレッド外で動く。ColorAnalyzer では、AiForms.Maui.Dialogs から KsDialogs.Maui (`0.1.0-beta.2`) へ移したあと、iOS 実機の Debug 構成で action 内の UIKit 呼び出し (画像の縮小) が `UIKit.UIKitThreadAccessException` になった
- Android: メインスレッドで動く。同じコードが成功する
- 知らせが読んだ経路:
  - iOS は Swift の `MauiLoadingBridge.awaitAction` に `@MainActor` が付いていない。コアの `KsLoading.start` の action 引数にも isolation の指定が無い。C# の `PlatformLoadingGateway` → `LoadingActionRunner` にも `MainThread` へ戻す処理が無い
  - Android は Kotlin の `MauiLoadingBridge.kt` が `Dispatchers.Main.immediate` を使い、コアの `runScope` も Dispatcher を切り替えない
- action のスレッドは spec・ADR・concepts・XML ドキュメントコメント・配布スキル (`ksdialogs-maui` / `ksdialogs-aiforms-migration`) のどこにも定めが無い。ViewModel の生成と configure、View の生成、transition の hook、進捗の受け口は UI スレッドを保証している
- 移行スキルの対応表は AiForms の `Show` / `Hide` の組を `StartAsync` へ写すよう案内している。このため、UI スレッド前提の処理がそのまま action に入りやすい

### 現状 (2026-09-25 コード確認)

知らせの読みは 5 か所とも正しい。ただし食い違いは MAUI 固有ではなく、Native の iOS と Android の間にもある。

| 形態 | action が始まるスレッド | 根拠 |
|---|---|---|
| iOS Native | 常にメインスレッド外。コアの `start` と action の型に isolation の指定が無く、UI スレッドから呼んでも移る | `ios/Sources/KsDialogs/Presentation/KsLoading.swift`、`Loading.swift` |
| Android Native | 呼び出し元のコルーチン文脈をそのまま使う | `android/ksdialogs-core/.../KsLoading.kt`、`Loading.kt`、`LoadingCoordinator.kt` |
| KMP (iOS / Android) | 呼び出し元のコルーチン文脈をそのまま使う | `kmp/ksdialogs-kmp/src/{androidMain,iosMain}/.../*LoadingGateway.kt` |
| MAUI iOS | Swift のスレッドプール上で呼ばれる (メインスレッド外) | `maui/macios/native/KsDialogsMauiBridge/MauiLoadingBridge.swift`、`maui/KsDialogs.Maui/Internals/LoadingActionRunner.cs` |
| MAUI Android | ブリッジの scope が `Dispatchers.Main.immediate` のため常に UI スレッド | `maui/android/native/ksdialogs-maui-bridge/.../MauiLoadingBridge.kt` |

- 契約 (concepts / ADR / 公開 doc コメント) に、action のスレッドの定めは無い。唯一の明文は、MAUI Android ブリッジ内部の KDoc (「UI スレッドから呼ばれる」) だけ
- action の実行スレッドを確かめるテストは、全形態に無い
- action の中から呼ぶ周辺操作 (show / hide / メッセージ更新 / 進捗報告) は、全形態で任意スレッドから呼べる契約と実装になっている
- 利用者のコードを呼ぶほかの箇所 (VM factory・configure・transition の hook・進捗の受け口) は UI スレッドで呼ぶ。例外は KMP の共有コードの VM factory・configure で、呼び出し元の文脈で動かす (kmp/ADR-0006)

## 検討した選択肢 (却下案と理由を含む)

| 案 | 評価 |
|---|---|
| A. 常に UI スレッドで始まる (フラグなし。ColorAnalyzer の要望) | UI に触らない重い処理も UI スレッドで始まる。外し忘れると、止まっている間は進捗やメッセージの更新が画面に出ない。→ 却下 |
| B. 既定は保証なし + UI スレッドで始めるフラグ | 既定のままでは、action の中が UI スレッドかどうかが分からないという困りごとが残る。フラグを付け忘れると iOS でだけ落ちる。→ 却下 |
| C. 呼び出し元の文脈を引き継ぐ (フラグなし) | `StartAsync` はほぼ UI スレッドから呼ばれるため、「バックグラウンドから呼べばよい」という逃げ道は実際には使われない。スレッドが呼び出し元しだいになり、「分からない」を呼び出し側へ移すだけになる。→ 却下 (オーナー指摘) |
| D. 既定は UI スレッド + UI スレッド外で始めるフラグ | action の中のスレッドが、呼び出しの 1 行で分かる。移行したコードは、何も付けなければそのまま動く。重い処理はフラグ 1 つで UI スレッドから外せる。→ **採用** |

## 決定事項

- スコープ形の action は、既定で UI スレッドで始まる。フラグを指定すると UI スレッド外で始まる。どちらの値でも、始まるスレッドを保証する (core/ADR-0037)

## ADR 候補 (作成済み: core/ADR-0037 / 未起票: なし)

## 未決の論点

- フラグの型 (真偽値か列挙か) と名前
- iOS Native で UI スレッドから始める方法 (action の型に `@MainActor` を付けるか、コアの入口で UI スレッドへ移すか)。UI スレッド外で始める場合の方法も含め、コンパイルで確かめる
- KMP の共有コードで UI スレッドを保証することが、kmp/ADR-0006 の整理 (共有コードは呼び出し元の文脈で動かす) と両立するか
- 定めた契約を、公開 doc コメント・concepts (core の loading-semantics と各形態の loading-surface)・配布スキル (`ksdialogs-maui` の `loading.md`・移行スキルの対応表) に書く。スキルの追従は docs-refresh 経由
- 全形態で、action の実行スレッドを確かめるテストを足す (既定とフラグ指定の両方)

## UI 素材 (ui/references/ の一覧と注釈)

## 変更級の推奨: L (2026-09-25 オーナー確定)

能力は Loading の 1 つで、公開 API の追加も小さい。前例の `add-loading-toast-typed-show` は M 級だった。今回はそれと違う点が 3 つあり、どれも「覆すコストが高い」ことと、design.md が要ることにつながるため L 級とする。

- 既定の動きが変わる (非破壊の追加ではない)。iOS Native / MAUI iOS / Android Native / KMP で、今ある呼び出しの挙動が変わる。利用者への伝え方まで整理が要る
- 形態ごとの実現方法がまだ確かめられていない。iOS Native で UI スレッドから始める方法は公開する型の形に出るため、実装より前に決める
- KMP で kmp/ADR-0006 とぶつかる可能性があり、ADR の改訂が要るかもしれない

1 change に収まる規模で、ロードマップにはしない。見た目は変わらないため `ui/` は作らない。
