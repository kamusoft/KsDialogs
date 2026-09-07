# scout 調査記録: 移植元の結果通知・多段表示の観察可能な挙動

2026-08-14、ksn-scout による AiForms.Maui.Dialogs (移植元) の読み取り専用調査。core concepts (結果通知の意味論・多段表示の意味論) の書き下ろしの裏取りに使用した。パスは調査時点のローカル環境のもの (時限情報)。

---

## テーマ1: 結果通知の意味論

### 結論 (観察可能な挙動)

**戻り値の型**
- Dialog は 2 系統。真偽値系 `Task<bool>` (`ShowAsync` 各種 / `ShowFromModelAsync`) と、任意型系 `Task<TResult>` (`ShowResultAsync` 各種 / `ShowResultFromModelAsync`)。いずれも await して「閉じたときの結果」を受け取る。
- 再利用型ハンドル (IReusableDialog) は `Task<bool> ShowAsync()` と `Task<TResult> ShowResultAsync<TResult>()` の 2 メソッドのみ。`IDisposable`。
- Toast は `void Show(...)` のみ。await できず、結果もタップ検知も一切返らない。型自体が `[Obsolete("Toast will be deprecated.")]`。
- Loading は「結果」ではなく実行の入れ物。`void Show(message, isCurrentScope)` / `void Hide()` / `void SetMessage(string)` と、`Task StartAsync(Func<IProgress<double>,Task> action, ...)`。StartAsync は渡した処理を await し、終了後に自動で Hide する。進捗は `IProgress<double>` 経由。返るのは `Task` (非ジェネリック) で、処理の戻り値を運ぶ口はない。

**完了 / キャンセルの伝わり方**
- 伝達路は 1 つだけ: ビュー側が持つ `DialogNotifier` の `Complete()` / `Complete<T>(result)` / `Cancel()`。呼び出し元 (Show 側) がこの 2 イベントを購読して `TaskCompletionSource` を解決する。
- 肯定的クローズ: `Complete()` → `bool` 系は `true`、`Complete<T>(r)` → `TResult` 系は `r`。
- キャンセルは 3 経路すべてが同じ `Cancel()` に集約: (a) ビューの明示的 `Cancel()`、(b) ダイアログ外タップ (`IsCanceledOnTouchOutside` が true のとき。既定 true)、(c) Android の戻るボタン (キーボード表示中は無視)。iOS には戻るボタン相当の経路がなく、外タップと明示 Cancel のみ。
- キャンセル時の値: `bool` 系は `false`、`TResult` 系は `default(TResult)`。値型では「キャンセル」と「0/false を Complete した」が区別できない。
- 「閉じた理由」を表す列挙値やフラグは存在しない。
- ライフサイクルフック: 開く直前に `SetUp()`、閉じた直後に `TearDown()`。使い捨て呼び出しでは閉じた時点でビュー・ハンドラ・ViewModel (`IDialogViewModelDestroy.Destroy()`) まで自動破棄。再利用ハンドルでは Dispose するまで状態が保たれる。

**カスタム結果値の機構 (DialogNotifier)**
- `IDialogNotifier` は `Complete()` / `Complete<T>(T)` / `Cancel()` の 3 メソッド。結果値は `DialogNotifierArgs.Result` (object) に載り、受け側で `(TResult)` にキャスト。型検査はキャスト時のみで、`Complete<T>` の T と `ShowResultAsync<TResult>` の TResult が食い違えば実行時 InvalidCastException。
- Notifier はビュー側のバインダブルプロパティ (既定 OneWayToSource) で、ViewModel から Complete/Cancel を呼ぶ使い方が公式。

**二重呼び出し / 表示中の再呼び出し**
- Android の Dialog: 同一ハンドル再入は防がれている。表示中なら例外ではなく `false` / `default` を即返す。
- iOS の Dialog: 同種のガードが一切ない。二重 ShowAsync は二重購読になる。
- 結果の二重通知は防御されていない (`SetResult` であり `TrySetResult` ではない)。2 回通知されると InvalidOperationException になりうる — コード上の確認で、実機再現は未確認 (推測)。
- Loading (既定): 表示中の `Show()` は無視。Android は固定タグのフラグメント存在で判定 (プロセス全体で1つ)、iOS はインスタンス単位のフラグ判定という差。
- Loading (カスタム/再利用): iOS の `StartAsync` は前回の非表示を最大 5 回×250ms 待ち、間に合わなければ何もせず return (処理自体が実行されない)。Android は前回ダイアログの破棄完了を待ってから表示。
- Toast: 排他制御なし。連続呼び出しは Android では OS の Toast キューに、iOS では KeyWindow への重ね貼り。

### 根拠パス
- `AiForms.Maui.Dialogs/Dialog/DialogNotifier.cs:3-40` (IDialogNotifier / Result が object)
- `Dialog/IDialog.cs:5-14`、`Dialog/Dialog.cs:27-87`、`Dialog/IReusableDialog.cs:5-6`
- `Dialog/DialogView.cs:50-73` (DialogNotifier、既定 OneWayToSource)、`:5-18` (IsCanceledOnTouchOutside 既定 true)、`:67-68` (SetUp/TearDown)
- `Dialog/ReusableDialog.iOS.cs:135-172` (bool 系)、`:174-211` (TResult 系、cancel で `SetResult(default)`)、`:213-219` (外タップ→Cancel)、`:79-133` (Dispose)
- `Dialog/ReusableDialog.Android.cs:121-133` (再入ガード)、`:181-193` (同、TResult 系)、`:276-294` (外タップ→Cancel)、`:239-274` (Dispose で ViewModel の Destroy)
- `Native/Android/ExtraPlatformDialog.cs:80-90` (戻るボタン→Cancel、キーボード表示中は無視)
- `Toast/IToast.cs:5-8`、`Toast/Toast.cs:5` (Obsolete)、`Toast/Toast.iOS.cs:18-77`、`Toast/Toast.Android.cs:25-133`
- `Loading/ILoading.cs:10-13`、`Loading/IReusableLoading.cs:5-7`、`Loading/Loading.iOS.cs:44-58`、`Loading/Loading.Android.cs:59-103`、`Loading/ReusableLoading.iOS.cs:46-67`、`Loading/LoadingBase.iOS.cs:77-88` (5回×250ms 待ち)、`Loading/ReusableLoading.Android.cs:56-72`
- `IDialogViewModel.cs:4-22`
- README: `README.md:134-166` (「If canceled, ret is false」「Returns null in case of Cancel」)、`:446-452`、`:486-492`、`:498-500` (IToast)、`:506-525` (ILoading)、`:534-545` (IReusableLoading)、`:637-676` (DialogNotifier)、`:678-684` (SetUp/TearDown)

### README とコードの食い違い
1. README は ShowResultAsync のキャンセル時を「Returns null」と書くが、実装は `default(TResult)`。値型では 0/false になる。
2. README の IReusableLoading は「void Hide()」だが実装は `Task Hide()`。ILoading 側は void で一致。
3. Toast が Obsolete である旨は README に記載なし。
4. README は「表示中の再呼び出し」「二重呼び出し」に一切言及しない。Android のみの再入ガードは未文書。

---

## テーマ2: 多段表示の意味論

### 結論 (観察可能な挙動)

**重なり順・ブロック**
- Dialog の多段は明示的なスタック管理ではなく OS の提示機構への委譲で成立。ライブラリ内にスタック・深さカウンタに相当する状態は存在しない。
  - iOS: 表示のたびに「キーウィンドウの root から `PresentedViewController` を辿った最上位」を提示元として `PresentViewController`。モーダル連鎖として上に積まれる。
  - Android: ダイアログ 1 枚 = 独立した `DialogFragment` + 全画面透明 Window。GUID タグで個別管理。共通スタックなし。
- 操作ブロック: どちらもモーダル。下層はタッチを受けない。オーバーレイ色は既定で透明 (「暗転しないモーダル」が既定)。外タップは最上位ダイアログの Cancel。
- Loading とダイアログの共存:
  - Android: Loading も DialogFragment (固定タグ1つ、Cancelable=false・外タップ不可・戻るボタン無効)。
  - iOS: Loading はモーダルではなく KeyWindow (または現在の VC の View) へのオーバーレイ subview 貼付。`isCurrentScope` で親を切替 (iOS のみ有効と README 明記)。前後関係は貼付先とモーダルの前後関係次第で、プラットフォーム間で挙動が一致しない。
  - 既定 Loading とカスタム Loading は Android では同一インスタンス・同一タグを共有し実質同時1つ。iOS は独立オーバーレイで複数同時に出せてしまう。明確なクロスプラットフォーム差。
- Toast: 完全に別経路。Android は OS 標準 Toast に自前 View (最前面・非モーダル・OS キュー管理)、iOS は KeyWindow へ直接 subview + タイマー消滅。どちらも非モーダル。iOS でモーダル表示との前後関係は保証されない (推測: 実機未確認)。
- 自動クローズ: Toast のみ (`Duration` ミリ秒、README 1–3500、既定 1500。Android は実質 3.5 秒にクランプ)。Dialog / Loading に自動クローズはない。

**dismiss の順序に関する制約**
- Android: GUID タグで個別に閉じるため閉じる順序は任意 (LIFO 制約なし)。
- iOS: 閉じる際の提示元 VC が固定参照ではなく毎回「現在の最上位 VC」を再計算するプロパティ。2 枚重なった状態で下を先に Complete/Cancel すると、実際に消えるのは上のダイアログになる示唆 — await 側は下の結果を受け取るのに消えるのは別、という不整合。iOS では実質 LIFO でしか安全に閉じられない (コード読解による強い示唆。実機未確認)。
- Android の Dismiss は 250ms フェード + 250ms スリープで、閉じてから次の表示まで約 0.5 秒のラグ。
- Loading の Hide は「ダイアログ生成完了を待ってから閉じる」待ち合わせが明示的に入っている。

### 根拠パス
- `Native/iOS/DialogHelpers.cs:17-26` (最上位 VC を辿る提示元解決)
- `Dialog/ReusableDialog.iOS.cs:16` (`RootViewController` プロパティ)、`:146`/`:152` (dismiss 時に再評価)、`:160` (present)
- `Native/iOS/DialogPresentationController.cs:18-25` (`UIModalPresentationStyle.Custom`)、`:38-54` (オーバーレイのフェード)
- `Dialog/ReusableDialog.Android.cs:38` (GUID)、`:158-164` (表示)、`:296-331` (Dismiss、250ms+250ms)、`Native/Android/DialogHelpers.cs:39-44`、`:126-137` (全画面透明ダイアログ)
- `Loading/Loading.Android.cs:8`, `:14` (`LoadingDialogTag`), `:18`, `:33` (インスタンス共有), `:97-103` (IsRunning 判定)
- `Native/Android/LoadingPlatformDialog.cs:33-35` (キャンセル不可)
- `Loading/LoadingBase.iOS.cs:49-75` (isCurrentScope で親切替)、`Loading/ReusableLoading.iOS.cs:118-156`
- `Toast/Toast.Android.cs:33`, `:92-103` (OS Toast + 3500ms クランプ)、`Toast/Toast.iOS.cs:79-91` (AddSubview)、`:54-76` (タイマー)
- README: `README.md:509` (isCurrentScope は iOS のみ)、`:643-645` (Toast Duration)

### README とコードの食い違い / 記載欠落
- README には多段表示に関する記述が一切ない (multiple / stack / at the same time で全文検索ヒットなし)。重なり順・dismiss 順序・Loading との共存可否はすべて実装依存の暗黙仕様。
- Loading の「iOS はオーバーレイ貼付、Android はモーダルダイアログ」という機構の非対称は README に記述なし。

---

## 推測と実測の区別

- **実測 (コード確認済み)**: 戻り値の型と 3 種の Cancel 経路、DialogNotifier の値受け渡し (object 経由 + キャスト)、Android の再入ガードと iOS でのその欠如、Loading の排他方式のプラットフォーム差、Toast の Obsolete 化、iOS の提示元 VC が動的プロパティである事実、Android のタグベース個別 dismiss。
- **推測 (コード読解による示唆。実機未確認)**: iOS で下層ダイアログを先に閉じると上層が消える件、結果の二重通知による InvalidOperationException、iOS で Toast がモーダルダイアログの下に隠れる可能性。
- ビルド・テストの実行は行っていない (読み取り専用調査)。移植元にテストプロジェクトは存在しない。
