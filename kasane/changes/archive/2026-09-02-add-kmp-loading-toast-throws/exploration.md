# Exploration: add-kmp-loading-toast-throws

## 課題 / 動機

KMP ライブラリの公開面で `@Throws` が宣言されているのは `KsDialogs.show` (と内部の `DialogGateway.show`) だけで、**`KsLoading` の suspend 関数 5 本 (`show` ×2 / `hide` / `setMessage` / `start` ×2) と `KsToast.show` 2 本には無い**。VM 経路は doc で `DialogException` を投げると明記し実装も投げる (`kmp/ksdialogs-kmp/src/iosMain/kotlin/jp/kamusoft/ksdialogs/kmp/IosToastGateway.kt` / `IosLoadingGateway.kt` がブリッジの NSError を `DialogException` に載せ替えて throw) が、宣言が無いため Swift から直接呼んで構成エラーが起きると、NSError にならず Kotlin/Native の未処理例外としてプロセスが abort する。

発見の文脈: fix-kmp-ios-unhandled-exception-crash の独立レビュー (review-001 Major 2、2026-09-02)。同 change は KMP Sample 側の `@Throws` 欠落を直したもので、ライブラリ側のこの穴は「同型・スコープ外」として起票に回した (オーナー承認)。

### 探索で確定した事実 (2026-09-02)

- **失敗条件は「未登録 VM / 型不一致」だけ**。iOS ブリッジ (`ios/Sources/KsDialogs/Interop/KsDialogsInteropLoadingBridge.swift` / `KsDialogsInteropToastBridge.swift`) が失敗を返すのは VM 経路のみで、message 経路 (`show(message)` / `start(message)`)・`hide`・`setMessage` は失敗を返す口が無い。提示先不在 (`presentationHostUnavailable`) を投げるのは Dialog 経路 (`DialogPresenter.swift`) だけで、Loading は表示なしで action 実行、Toast は提示先待ち→満了破棄 (それぞれの概念文書の失敗モデルどおり)。簡易起票時の「提示先不在」の記述は Dialog にしか当てはまらなかった
- **Swift 利用者が Kotlin の `KsLoading` / `KsToast` に届く経路**は、エクスポートされた framework の `Loading.shared.instance` / `Toast.shared.instance` を直接呼ぶときだけ。Swift 糖衣 (`ios/Sources/KsDialogs/Kmp/KsLoadingKmp.swift` / `KsToastKmp.swift`、`Loading.shared.kmp` 等) は Native の Coordinator を直接叩き、Kotlin を経由しない。KMP iOS Sample も共有 Kotlin の `SamplePresenter` 経由で、instance の直呼びはしていない
- **iOS Native の protocol** (`ios/Sources/KsDialogs/Presentation/KsLoading.swift` / `KsToast.swift`) は「message 経路は非 throwing、VM 経路・インライン経路は throws」。KMP 面をこれに揃えるだけで新しい設計判断は要らない
- **Kotlin/Native の公式規則** (https://kotlinlang.org/docs/native-objc-interop.html の Errors and exceptions): `@Throws` に列挙したクラス (とサブクラス) だけが NSError として伝播し、それ以外は未処理として終了する。非 suspend + `@Throws` 無しは例外を一切伝播しない。suspend + `@Throws` 無しは `CancellationException` だけを NSError 化する。生成 ObjC ヘッダは本ワークツリーに成果物が無く未確認だが、abort は規則からの帰結
- **同型の穴がもう1つ**: Sample の `samples/kmp/shared/src/commonMain/kotlin/jp/kamusoft/ksdialogs/samples/kmp/SamplePresenter.kt` の非 suspend Toast 関数 4 本 (`showDefaultToast` / `showCustomToast` / `showToastStack` / `showToastPlacement`) に `@Throws` が無い。前回の修正は suspend 10 本だけを覆っており、登録経路の関数は同じ abort リスクを持つ (実機での abort は未確認)

## 検討した選択肢 (却下案と理由を含む)

**論点 1: `KsToast.show(viewModel)` (非 suspend) の扱い**

- **A. `@Throws(DialogException::class)` を宣言して throws 化** — 採用。toast-semantics の失敗モデル「解決の失敗は同期に失敗 (Swift は throws)」と Native iOS protocol の VM 経路 `throws` にそのまま揃う。Swift 直呼びでは「abort する呼び出し」が「`try` が要る呼び出し」になるだけで、公開前のため影響先なし
- B. 失敗を値で返す — 却下。fire-and-forget・戻り値なし (core/ADR-0031。`kmp/api-surface-check` の `RejectsToastShowResult` が既に負のチェックとして存在) と衝突し、Native からも乖離する。契約変更のため ADR も要る
- C. 据え置き + 文書の注意書き — 却下。文書は「失敗する」なのに実物は abort、が公開面に残る

**論点 2: `KsLoading` の宣言範囲**

- **2-a. VM 経路の 2 本 (`show(viewModel)` / `start(viewModel)`) だけ** — 採用。実際に投げる関数だけが宣言を持ち、Native の「message 経路は非 throwing」と同形
- 2-b. suspend 5 本すべてに一律 (`KsDialogs.show` に倣う) — 却下。`hide` / `setMessage` / message 経路に投げない例外を宣言することになる。将来 message 経路が投げるようになっても suspend の Swift シグネチャは不変なので、そのとき足せばよい

**論点 3: Sample の非 suspend Toast 関数 4 本**

- **3-a. 本 change に同梱し、登録経路で投げうる関数だけに `@Throws(DialogException::class)` を付ける** — 採用。ライブラリと同型の欠陥で、Sample は利用者の手本になる場所。付随修正の同梱条件 (同じ能力・局所的・公開 API に触れない・判断分岐なし) に収まる
- 3-b. 非 suspend 4 本すべてに一律 — 却下。論点 2 の「投げる関数だけ宣言」の方針とぶれる
- 3-c. 別 change に起票 — 却下。起票の後続コストに見合わない

## 決定事項

- 2026-09-02: **`KsToast.show(viewModel)` は `@Throws(DialogException::class)` を宣言して throws 化する** (オーナー確定、案 A)。ADR は起票しない (kmp/ADR-0001 と既存概念の範囲内)
- 2026-09-02: **`KsLoading` は VM 経路の 2 本 (`show(viewModel)` / `start(viewModel)`) だけに `@Throws(DialogException::class, CancellationException::class)` を宣言する** (オーナー確定、案 2-a)。`CancellationException` は宣言が無くても NSError 化されるが、`KsDialogs.show` と同じ並記にして読み手に伝える
- 2026-09-02: **Sample の `SamplePresenter` の非 suspend Toast 関数は、登録経路で投げうるものだけに `@Throws(DialogException::class)` を付けて本 change に同梱する** (オーナー確定、案 3-a)。どの関数が投げうるかの切り分けは実装時にコードで確定させる。Swift 側の呼び出し箇所 (`samples/kmp/iosApp/KsDialogsSampleKmp/SampleMenuModel.swift`) は `try` に追随させる
- 実装時の留意: `@Throws` は interface の宣言と actual 側の override の両方で整合が要る (`KsDialogs.show` / `DialogGateway.show` の既存例に倣う)。Android 側 (`AndroidLoadingGateway` / `AndroidToastGateway`) は Kotlin 呼び出しのため挙動に影響しない

## ADR 候補 (作成済み: なし / 未起票: なし)

kmp/ADR-0001 (素の suspend 直接公開・`@Throws` で NSError 化) の範囲内の徹底。契約は toast-semantics / loading-semantics の失敗モデルが既に定めており、ADR 級の判断は含まない。蒸留時は kmp/ADR-0001 の現行照合に 1 行足す程度でよい

## 未決の論点

- 検証の形: 前回の change と同じ「未登録 VM を強制する A/B」で、非 suspend (`KsToast.show(viewModel)`) 経路でも abort → NSError (Swift の `catch`) に変わることを 1 回実測して `evidence/` に残す (handbook/cross/runtime-behavior-verification.md)。Swift から Kotlin の instance を直接呼ぶ経路は Sample に無いため、検証用の一時コードか、生成 ObjC ヘッダの `error:` 引数の有無で確認する — どちらにするかは実装時に決める
- 生成 ObjC ヘッダで `KsToast.show(viewModel)` の宣言が `error:` 付きに変わることの確認 (ビルド成果物が要る)
- MAUI / Native の同等面: Native iOS は上記のとおり揃っている。MAUI は C# 例外なので同型の問題は無い見込み (未確認だが、境界の性質上 `@Throws` 相当の機構が存在しない)
- phase-9-docs の注意書き論点 (共有コードの関数を Swift に出すときの `@Throws`) には、本 change の結果「非 suspend 関数も対象」を申し送る

## UI 素材 (ui/references/ の一覧と注釈)

なし

## 変更級の推奨: S

理由: ライブラリ側は commonMain の 2 ファイル (`KsLoading.kt` / `KsToast.kt`) + actual 側の override への追随で、公開面の変化は「Swift 直呼びで abort する呼び出しが `try` 必須になる」だけ。契約 (概念文書・Native protocol) が既に定めた形への追随であり、新しい設計判断を含まない。Sample の付随修正も同梱条件に収まる。可逆で局所的。独立レビューは必須 (S 級の規律)
