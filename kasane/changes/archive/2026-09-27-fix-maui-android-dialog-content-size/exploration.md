# Exploration: fix-maui-android-dialog-content-size

## 課題 / 動機

MAUI の Android で、ダイアログの中身に置いた View の大きさ指定 (`WidthRequest` / `HeightRequest`) が効かず、中の子要素の大きさまで縮んで表示される。

- 観測: 中身に `WidthRequest=120`・`HeightRequest=80` の ContentView (中に Label 1 つ) を置いたダイアログが、iOS では 120×80pt で出るのに、Android では Label の大きさ (約 36×19dp) で出た
- 観測環境: Android エミュレータ (emulator-5554)、MAUI の実配置テストホスト `maui/KsDialogs.Maui.PlacementHost` のプローブ (`PlacementProbe.cs`) の 1 構成
- 発見の文脈: add-page-layout-area の tasks 4.4 (MAUI の実配置テスト)。配置の判定は矩形の位置で見るため、その change の合否には影響しなかった

### 原因 (コードで確認。一部は推論)

- Android の MAUI 層は、中身の MAUI View を platform view 化した結果 (`ToPlatform()`) をそのまま互換面へ渡している (`maui/KsDialogs.Maui/Platforms/Android/PlatformDialogContent.cs` の `Create`)。iOS は包みの View (`Platforms/iOS/PlatformDialogContent.cs` の `DialogContentView`) で MAUI の測り方 (`IView.Measure` / `Arrange`) を呼んでいる
- Android Native の器は中身の LayoutParams (無ければ WRAP_CONTENT) に従って測り、内容サイズに委ねる軸は上限つき (AT_MOST) で測る (`android/ksdialogs-core/src/main/kotlin/jp/kamusoft/ksdialogs/DialogContentHolder.kt`・`DialogLayoutHost.kt` の `contentHolderMeasureSpec`)。Android の慣習どおりで、器の側に誤りはない
- MAUI 本体 (dotnet/maui) では、ルート View 自身の明示サイズは「MAUI の親が測るとき」(handler の `GetDesiredSizeFromHandler` が EXACTLY の測定条件を作る) にしか効かない。ネイティブの親に直接測られると、ContentView / Border / TemplatedView 系のルートは中身の大きさ + 余白を返し、自分の明示サイズを返さない (`LayoutExtensions.MeasureContent`)
- 縮むのはルートが ContentView / Border 系のときに限られる。Grid / StackLayout 系は LayoutManager が自分の明示サイズを返すため縮まない (推論)。`MinimumWidthRequest` / `MinimumHeightRequest` は Android の最小サイズへ写されるため効く (推論)
- 併発: ルート自身の `Arrange` を呼ぶ親がいないため、ルートの Width / Height / SizeChanged が届かない (推論)。ライブラリのスライド演出は Android の実寸で距離を測るため影響を受けないが、利用者の演出フックがルートの Width / Height を読むと未設定値になる
- 比率指定 (ProportionalWidth / Height) を付けた軸は器が大きさを固定して測るため縮まない

### 影響範囲

- Dialog / Loading / Toast の 3 機能とも同じ共通部品 (`PlatformDialogContent.Create`) を通り、ネイティブ側も同じ測り方の器 (`DialogLayoutHost` → `DialogContentHolder`) に載るため、3 つとも同じ症状が出る。既定の Loading / Toast の View は Native の内蔵なので対象外
- samples/maui の中身はどれもルートが大きさ指定なしの ContentView で、大きさはその内側の Border に付いている (MAUI の親に測られるため効く)。verification/maui に大きさ指定は無い。実害が出ている既存の画面は見つかっていない
- 契約上は不具合: layout 契約は「View 自身が宣言する大きさは内容サイズとして扱う」と定めている (`kasane/concepts/core/api/layout-semantics.md` の rect 決定手順)。MAUI Android はこれを満たしていない

### テストの穴

- 120×80 を指定している中身は実配置テストのプローブだけで、症状が出る構成 (ContentView ルート) だが、判定は端の位置だけで大きさを見ていないため検出できない (`PlacementRunner.cs`)
- `maui/KsDialogs.Maui.Tests` は素の net10.0 の単体テストで platform view が無く、Android の実際の測り方は見られない。Android で中身の大きさを実測できるのは実配置テストホストだけ (実機 / シミュレータで手で走らせる。CI・`dotnet test` の対象外)

## 検討した選択肢 (却下案と理由を含む)

### 直し方

- **採用: Android にも iOS と同じ包みを置き、MAUI の測り方で大きさを決める** — ルートの種類を問わず iOS と同じ大きさになり、最小・最大の指定も効く。初回のレイアウトパスの時点の値で測るため、表示前に ViewModel の初期状態が binding で大きさを変える場合も契約どおりになる。ルートの Width / Height・SizeChanged も届くようになる。触るのは MAUI 層の C# だけ (Native・互換面は無変更)。MAUI 本体も CollectionView のセル (`ItemContentView`) で同じやり方を使っている
- 却下: 作成時点の `WidthRequest` / `HeightRequest` だけを Android の大きさ指定 (LayoutParams) に写す — 変更は最小だが、作成後〜初回レイアウトパスの間の変更を取りこぼし、最大の指定は写らず、ルートの Width / Height も未設定のまま残る。iOS との差が残る
- 却下: ネイティブの器 (Kotlin 互換面) の側で MAUI 用に測る — 結果は採用案と同じになるのに、互換面から C# への呼び返しの新設と、測定のたびの言語境界の往復が増えるだけ

### テストの足し方

- **採用: Dialog / Loading / Toast のそれぞれで、中身が指定どおりの大きさで出ることを実配置テストホストで確かめる** — 直す包みは 3 機能の共通部品なので、1 機能の配線漏れも見張れる。iOS でも同じシナリオが走り、両 OS の揃いを見られる。ルートの種類は 2 つ並べる (縮んでいた ContentView と、元から縮まない Grid)
- 却下: Dialog だけ確かめる (既存プローブの判定に幅・高さを足す) — 手間は小さいが、Loading / Toast の配線漏れは目視の証跡頼みになる
- 却下: テストは足さず修正前後の証跡だけ残す — 次に壊れたときに気づく手段が無い

## 決定事項

- 直し方: Android の MAUI 層に、iOS の包み (`DialogContentView`) と同じ役割の包みを置き、中身の大きさを MAUI の測り方で決める。Native の器と互換面は変えない
- テスト: 実配置テストホスト (`maui/KsDialogs.Maui.PlacementHost`) に大きさのシナリオを足し、Dialog / Loading / Toast × ルートの種類 2 つ (ContentView・Grid) で、表示された中身の大きさが指定どおりかを判定する。ホストの説明 (いまは「表示中のページ」専用) を実配置全般へ広げる。両 OS で走らせる
- 上の 2 つは ADR にしない (単発のバグ修正で、MAUI 層の Android 側に閉じ、iOS 側の既存の形に揃えるだけで新しい方向を決めないため)

## ADR 候補 (作成済み: なし / 未起票: なし)

## 未決の論点

実装で扱う注意点 (申し送り):

- 包みを挟むと器が受け取る View が包みに変わるため、添付面の写し先 (`MauiDialogContent.ApplyAttributes` の対象)・演出の結び付け先 (互換面の中身の View)・初回レイアウトパスの節目の監視 (`ObserveFirstLayoutPass` の attach / LayoutChange) を包みの側に揃える。採用時点 (core/ADR-0015) の意味を変えない
- Toast の器はタッチ素通し (非対話) なので、包みがタップを受け止めないこと
- 修正後も `MinimumWidthRequest` / `MinimumHeightRequest` が効くこと、Grid ルートが二重に大きさを当てられないことを実機で確かめる
- 既存の位置のシナリオは中身が 120×80 に戻っても端の位置で判定するため、合否は変わらない見込み

## UI 素材 (ui/references/ の一覧と注釈)

なし

## 変更級の推奨: S (確定)

公開 API の変更なし・MAUI 層の Android 側の中身の受け渡し 1 か所に閉じる・契約 (layout 契約の内容サイズ) は既に定まっていてデルタスペックで足す要件が無い・可逆。見た目の設計の変更ではなく不具合の修正なので ui/ は作らない。正解 (iOS と同じ大きさ) が事前に決まっているため ksn-live ではなく直接実装で進める。独立レビューは必須。
