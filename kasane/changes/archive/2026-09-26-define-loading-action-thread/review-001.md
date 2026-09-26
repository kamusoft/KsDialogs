# レビュー結果: define-loading-action-thread (001 回目)

**日付**: 2026-09-25
**判定**: APPROVED

## サマリー

4 形態すべてで、スコープ形の action が始まるスレッドの契約が満たされている。既定は UI スレッドで、指定すると UI スレッド外で始まる。切り替えは、デルタスペックと design に書かれた場所 (Swift の isolation / `runScope` / `LoadingActionRunner` / 各 OS の gateway) に 1 か所ずつ入っている。Scenario 対応のテストもすべてある (LD-TH 8・LD-HI 2・LD-HA 2・LD-HM 6・LD-HK 4。LD-HS 2 件は Sample 通しの証跡で受け入れる)。Critical / Major はない。指摘は記録の不足 2 件 (Minor) と、堅牢性と蒸留時の補足の提案 3 件 (Suggestion)。実装の中で最も大きい変更は Android の報告の列を作り直したこと (`LoadingCoordinator`) で、正しさは確認できた。ただし design の前提から外れた変更なのに、deviation.md に記録が無い。

## 照合した規約

- comment-policy.md (always)
- test-execution.md (テストの実行・結果の報告・完了判定)
- sample-parity.md (`samples/` の文言とデモ動作の変更)
- diagnostic-message-language.md (MAUI Runner に例外経路を足しているため確認した。新しい文言は無く、該当なし)
- lessons/code-review.md の重点観点 L-001 (MAUI iOS の変更前後の証跡が、検証したい命題の真偽で結果が分かれるか)

## 確認した観点

- **仕様充足**
  - loading-contract の LD-TH-01〜08 は、iOS (`ios/Tests/KsDialogsTests/LoadingActionThreadTests.swift`) と Android (`android/ksdialogs-core/src/androidTest/.../LoadingActionThreadTests.kt`) に同じ ID のテストがある。`--require-mirror` の検査も通る (手元で再実行し、未網羅なし)
  - ios-native: protocol 要件 5 本・省略形 8 本・実装 5 本・`runScope` の action の型が `@MainActor` になっている。スレッドを指定する引数は足していない。LD-HI-01 は 13 本 × 3 通りの書き方をコンパイル検査で固定している。LD-HI-02 は実行時テストで確かめている
  - android-native: 4 本の `start` と `startCompose` に、`actionThread` が action の直前・既定値 `MAIN` で足されている。LD-HA-01 はコンパイル検査、LD-HA-02 は instrumented で確かめている
  - maui-binding: 10 本の `StartAsync` の最後に `actionThread` が足されている。Runner は差し替え可能な UI スレッドの口を通して振り分ける。Hostless はその場で実行する。LD-HM-01 は戻り値の型を明示した受けでオーバーロードの束縛を固定し、LD-HM-02〜06 は偽物の専用スレッドで振り分けを確かめている
  - kmp-facade: commonMain は指定を渡すだけ。Android は Native の型へ写し、iOS は gateway で `Dispatchers.Main.immediate` / `Default` を使って切り替える。ObjC ヘッダに `actionThread` が現れることも、テスト (`ObjCApiSurfaceTests`) と証跡で確かめている
  - samples: 4 ルートの `SampleText` に `結果: 処理中` が同じ値で入っている。Default Loading の action の最初の文で、UI スレッドへ明示的に移さずに結果表示を更新している。iOS Sample から「MainActor の外で動く」前提のコメントと値の先取りが外れている
- **tasks.md**
  - 1〜5 と 6.2・6.3 のチェックに、実体の伴わないものは無い。6.1 は未チェックのままで、実態と合っている
- **足場アーティファクト**
  - proposal / design / specs の差分は無い (tasks.md のチェックだけ)
- **付随修正**
  - deviation.md の 2 件 (MAUI の負の検査の説明文、`MIRROR_AREAS` への `("LD","TH")` の追加) は、どちらも本務と同じ能力の 1 ファイル・局所的な変更。公開 API に触れず、同梱条件に収まる。前者は説明文だけ、後者は `--require-mirror` の実行で効いていることを確かめた
- **堅牢性**
  - 失敗・キャンセルの経路で終了がちょうど 1 回数えられることを確かめた (Android の `runScope` の catch、KMP iOS の `NonCancellable`、MAUI Runner の `finally` と範囲外の値の扱い)。報告と終了の順序 (Android は列と区切り、iOS / KMP iOS は互換面の `LoadingReportQueue`、MAUI は同期転送) も確かめた
  - MAUI の `#if IOS || ANDROID` は、`DialogPresenter.OnUiThreadAsync` と同じ条件になっている。ターゲットは `net10.0;net10.0-ios;net10.0-android` で、漏れは無い
- **証跡 (L-001)**
  - `evidence/maui-ios-before-change-default-loading.log` は、変更前のライブラリで同じ操作をすると `UIKitThreadAccessException` で落ちる記録。変更後の画像 (`sample-maui-ios-default-loading-*.png`) では、処理中の結果表示が出たまま落ちていない。振り分けの機構が無ければ観測が変わる組になっており、判別力がある
- **コメント規約**
  - 公開 doc コメントに、今回新しく ADR ID や内部用語は入っていない。ADR 参照は、非公開の実装側 (`Loading.runScope`、`LoadingActionRunner`、`IosLoadingGateway.runScope`) に限られている。`comment-policy-lint.py --summary` は禁止 0 件 (手元で再実行)
- **テストの実行**
  - 依頼元がホスト側で回した全ルートの結果 (事実として受領) を判定に使った
  - 手元では `scenario-id-coverage.py` (通常 / `--require-mirror`) と comment-policy lint を再実行し、どちらも問題なし
  - Gradle・Simulator を使う実行は、同時実行と端末の占有を避けるために追加で回していない

## 指摘事項

### 🟡 Minor Android の報告の列を作り直した変更が deviation.md に記録されていない

**該当箇所**: `android/ksdialogs-core/src/main/kotlin/jp/kamusoft/ksdialogs/LoadingCoordinator.kt:279-322`、`android/ksdialogs-core/src/main/kotlin/jp/kamusoft/ksdialogs/Loading.kt:185-205`、`deviation.md`

**問題点**:
- design (`design.md:110`) の Decision 3 は、「切り替えを action の呼び出しの内側に入れれば、順序の保証をそのまま保てる」を前提にしている
- 実装では、Android でこの前提が成り立たなかった。既定の指定で UI スレッド外から報告し、すぐ戻ると、終了が報告を追い越す。対策として、次の 2 つを作り直している
  - `LoadingCoordinator.report` を、報告ごとの `scope.launch` から、1 本の `Channel` と 1 つの受理コルーチンに変えた
  - 終了の直前に区切り (`awaitAcceptedReports`) を積んで待つ形にした
- 変更自体は正しく、回帰テストもある (`既定の指定で_UI_スレッド外から報告して直後に戻っても…`)
- ただし、design の前提から外れた変更で、共有の報告経路 (coordinator) の構造も変えている。それなのに deviation.md には付随修正 2 件しか無い。このままでは、「なぜ報告は列と区切りなのか」という知識が蒸留で拾われない

**推奨修正**: deviation.md に乖離として 1 項を足す。次の 3 点を書く。
- design Decision 3 の「順序の保証をそのまま保てる」が、Android では成り立たなかったこと
- 追い越しの起き方
- 報告と区切りを 1 本の列にした対策と、それを固定するテスト名

### 🟡 Minor 完了確認の記録 (test-run-summary.md) が途中の実装の失敗のまま残っている

**該当箇所**: `evidence/test-run-summary.md:51-57`、`evidence/test-run-summary.md:9-17` (6.1 の表)

**問題点**:
- 失敗 B (LD-TH-07) の「実装の見立て」は、`endUseAfterPendingReports` が `withContext(NonCancellable + Dispatchers.Main)` で積み直す前提の、途中の実装を説明している。今の実装は、報告と同じ列に区切りを積む形である
- 記録は「実装側で直す対象」で終わっていて、その後に直したこと・再実行の結果が書かれていない
- 6.1 の表も、`:ksdialogs-core` の失敗を含む途中の件数のままになっている
- 依頼元の申し送りでは、修正後に Loading 系 110 件が全件成功し、LD-TH-07 と既定指定の順序テストが 3 台で各 50 回連続成功している。しかし証跡ファイルにはそれが無く、verify・蒸留の読み手が未解決の失敗と読み違える

**推奨修正**: 6.1 を閉じるときに、この記録を最終の実行に合わせて更新する。
- 失敗 B に「その後の修正」と「再実行の結果」を追記する (API 31 / 33 / 36 で各 50 回、Loading 系 10 クラス 110 件)
- 残る 5 件の失敗が変更前のコードでも同じ例外で落ちることの切り分けを、1 か所にまとめる

### 🔵 Suggestion 報告の受理コルーチンが例外で止まると、以後のスコープ形の終了が取り消しできない待ちになる

**該当箇所**: `android/ksdialogs-core/src/main/kotlin/jp/kamusoft/ksdialogs/LoadingCoordinator.kt:312-322`、`android/ksdialogs-core/src/main/kotlin/jp/kamusoft/ksdialogs/Loading.kt:201-205`

**問題点**:
- 受理は 1 つの長寿命のコルーチンが担う。利用者が実装する `LoadingProgressReceiver.onProgress` が例外を投げると、そのコルーチンは終わる
- その後の `awaitAcceptedReports` は、区切りを積んでも誰も受理しないため、`NonCancellable` の中で永久に待つ
- 変更前は報告ごとに別のコルーチンだったので、1 件の失敗が他の報告や終了を巻き込まなかった
- Android の既定の未捕捉例外処理ではプロセスが落ちるので、実害が出るのは、未捕捉例外を握って続行するアプリに限られる

**推奨修正**: 受理のループで 1 件ごとの失敗を閉じ込めて、ループを生かし続ける。区切りは必ず完了させる。

```kotlin
// before
is ReportQueueEntry.Report -> acceptReport(entry.progress, entry.token)
// after (例)
is ReportQueueEntry.Report -> runCatching { acceptReport(entry.progress, entry.token) }
    .onFailure { failure -> /* 既存の警告ログの流儀で記録し、列の受理は続ける */ }
```

握り潰さずに再送出したい場合は、`CoroutineExceptionHandler` へ渡す形でもよい。

### 🔵 Suggestion 「isolation の指定が無い関数は UI スレッド外で始まる」は、利用者のビルド設定に依存する

**該当箇所**: `ios/Sources/KsDialogs/Presentation/KsLoading.swift:100`、`ios/Tests/KsDialogsTests/LoadingActionThreadTests.swift` (末尾の `loadingActionThreadTestReportsStartThread` の doc コメント)

**問題点**:
- 公開 doc コメントは「その関数自身の isolation が優先される」とだけ書いていて、正確である
- 一方、proposal Impact と ios-native デルタの例示「isolation の指定が無い async 関数は UI スレッド外で始まる」は、利用者のモジュールで `NonisolatedNonsendingByDefault` が無効な場合に限って成り立つ
  - 有効な場合 (Xcode 26 の Approachable Concurrency)、そのような関数は呼び出し側の isolation を引き継ぐ。そのため UI スレッドで始まる
  - 既定の actor 分離を MainActor にしたモジュールでは、関数自体が MainActor になる
- テスト側のコメントは、この前提を正しく書いている

**推奨修正**: コードの修正は不要。蒸留時に、ADR-0037 または ios の loading-surface 概念文書へ「関数を名前で渡した場合は、関数の実効的な isolation (モジュールの並行性設定を含む) で決まる」と書いておく。docs-refresh で `skills/` に移行の案内を書くときも同じ。

### 🔵 Suggestion Android の負の検査 `loadingShowOptions` の診断件数が handbook の記載と食い違っている (この change の範囲外)

**該当箇所**: `evidence/test-run-summary.md:30`、`kasane/handbook/cross/test-execution.md` の負の検査の表 (`ksdialogs.negativeCheck.loadingShowOptions` の行)

**問題点**:
- 実測では `None of the following candidates is applicable:` の 1 件だけが出た。handbook は 2 件と記載している
- この change は `show` の署名を変えていないので、コンパイラ版数など、変更前からあるずれと見られる
- 禁止形状が弾かれていることは確かめられているので、検証としては成立している

**推奨修正**: この change では直さない。変更前のコードでも 1 件になるかを確かめたうえで、handbook の期待する診断を実測に合わせる。対応は ksn-drift か別の簡易起票で行う。

## アクションプラン

1. (Minor) deviation.md に、Android の報告の列と区切りへの作り直しを乖離として記録する (design Decision 3 の前提が成り立たなかったこと・対策・固定するテスト)
2. (Minor) tasks 6.1 を閉じるときに `evidence/test-run-summary.md` を最終の実行結果に更新し、失敗 B の解消と再実行の件数を残す
3. (Suggestion) Android の受理ループで 1 件ごとの失敗を閉じ込め、区切りの待ちが永久に止まらないようにする
4. (Suggestion・蒸留時) Swift で関数を名前で渡す場合の始まるスレッドは、利用者モジュールの並行性設定に依存することを、ADR-0037 か concepts に書く
5. (Suggestion・範囲外) handbook test-execution の `loadingShowOptions` の期待する診断を、実測で確かめて直す
