# レビュー結果: add-loading-toast-typed-show (001 回目)

**日付**: 2026-09-06
**判定**: APPROVED

## サマリー

デルタスペック 6 面 (loading-contract / toast-contract / ios-native / android-native / maui-binding / samples) の Requirement と Scenario は、3 形態の実装・テストへ過不足なく落ちている。レジストリの 2 スロット化は Dialog レジストリ (`DialogViewRegistry`) と同型で、型指定経路の「呼び出し時点のスナップショット解決 → VM 生成 → configure 完了 → View 生成 → 提示」の順序保証、Loading の合流との関係 (提示前の失敗は合流に数えない / 合流側では View factory を呼ばない)、Toast の失敗 2 分類 (VM factory 未登録 = 同期失敗 / VM factory・configure の例外 = 受理後の失敗 1 枚破棄) が、いずれも設計どおりに実装されている。足場 (proposal / specs) は無改変で、tasks.md の変更はチェックのみ。

Critical / Major は無い。指摘は Minor 2 件 (Android Toast の型指定経路での ViewModel 保持と kdoc の食い違い / 新規 Compose テストの後始末) と Suggestion 3 件。いずれも公開契約の意味を変えるものではないため、修正なしでアーカイブへ進めてよい範囲と判断する。

## 照合した規約

- `kasane/handbook/cross/comment-policy.md` (always — 全ソースファイル)
- `kasane/handbook/cross/test-execution.md` (テスト実行・完了判定・scenario-id-coverage・公開 API 形状の検証)
- `kasane/handbook/cross/sample-parity.md` (`samples/` を触るため)
- `kasane/handbook/cross/runtime-behavior-verification.md` — 適用外と判定 (実行時挙動の不具合調査ではなく、機能追加。Sample の通しは sample-parity と verification 証跡で担保)
- `kasane/handbook/cross/local-development-setup.md` (guide) / `aiforms-origin-reference.md` / `user-skill-api-listing.md` / `user-skill-writing-style.md` — 適用外と判定 (環境構築・未移植機能の移植・`skills/**` のいずれにも当たらない)
- ADR: core/ADR-0035 (本 change の決定)、参照として core/ADR-0013・0019〜0021・0024・0025・0029・0030・0031・0033、maui/ADR-0005
- lessons: `kasane/lessons/code-review.md` は不在 (重点観点・指摘しないことの適用なし)。`process.md` の L-001 (姉妹面の照合) をレビュー観点として適用した

## 実行した検査

本レビューは静的レビューで、全ルートのテスト再実行は行っていない (コンテキストパッケージで実測済みとして提示された値をそのまま前提にした)。このセッションで自分で回したのは次の 4 本:

- `python3 scripts/scenario-id-coverage.py --require-mirror` → 本 change の ID は未網羅 0・両 Native ミラー OK (残る未網羅 20 件はすべて別 change `add-kmp-typed-show` の ID)
- `python3 scripts/comment-policy-lint.py --advisory` → 禁止 0 件 (要確認 453 件はリポジトリ全体の既存分)
- `python3 scripts/local-path-lint.py` → 0 件
- `python3 scripts/identity-lint.py` → 0 件

証跡は `verification/sample-walkthrough/` の PNG を 2 枚 (android-04 / ios-02) 開いて notes.md の記述と一致することを確認した。

## 指摘事項

### [🟡 Minor] Android の Toast 型指定経路が生成した ViewModel を保持せず、`ToastDisplay` の kdoc と食い違う

**該当箇所**: `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/ToastDisplay.kt:26-36` (`ToastContentRequest.Typed`) と `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/ToastCoordinator.kt:202-205`

**問題点**: `ToastContentRequest.Typed` は `Custom` の下位ではなく `prepare` と `factory` だけを持つため、`prepare()` が作った ViewModel はどこにも保持されない (`createView(host, request.prepare())` に渡って捨てられる)。`ToastDisplay` の kdoc は「器・中身・**ViewModel**・factory・演出フックへの参照は撤去が完了するまでここが握る (fire-and-forget でも途中で解放しない)」と宣言しており、型指定経路ではこの不変条件が成立しない。iOS 側は `ToastResolvedContent.viewModel` → `ToastDisplay(viewModel:)` で保持しており、姉妹面で扱いが割れている (lessons L-001 の照合対象)。

現時点で観測可能な不具合は無い — Toast の ViewModel は View 生成後に読まれる契約を持たないため、参照が切れても表示は壊れない。ただし将来 Toast VM に読み取り後の役割 (アクセシビリティ再読み上げ・再取り付け時の再バインドなど) が入ると、Android だけ静かに壊れる形になっている。

**推奨修正**: `createContent` で `prepare()` の戻り値を `ToastDisplay` の可変フィールド (例: `var typedViewModel: Any?`) へ入れて `releaseResources()` で手放す (iOS と同じ保持期間にそろえる)。コード変更を避けるなら、`ToastDisplay` の kdoc から「ViewModel」を外し、型指定経路では中身の View が保持者であることを明記する。

### [🟡 Minor] 新規 Compose テストが 4 秒の Toast を残したまま終わる

**該当箇所**: `android/ksdialogs-compose/src/androidTest/kotlin/jp/kamusoft/ksdialogs/compose/ComposeTypedShowTests.kt:76-99` (`TS_YA_03`) と同ファイル `:118` (`DURATION_MILLIS = 4_000`)

**問題点**: このテストは共有シングルトン `Toast.instance` へ 4000 ms の表示を投入し、表題を観測した時点で終了する — 表示が消えるのを待たない。テストメソッドが戻った後もグローバルな `ToastCoordinator` に表示が 1 枚生き続けるため、同一プロセスで後続する Toast 系テスト (同モジュールの `ComposeToastCustomViewTests` は表示枚数と取り付け/取り外しを見る) に混ざり込む余地がある。同モジュールの既存テストは `DURATION_MILLIS = 600` で取り外しまで待っており、この 1 本だけ後始末の作法が抜けている。3 台での全件成功は事実だが、テスト順序に依存した潜在的な不安定さである。

**推奨修正**: 観測後に表示が消えるまで待つ (`awaitValue` と同じ形の待ちで表示枚数 0 を確認する) か、`DURATION_MILLIS` を既存テストと同じ短さにして消滅まで待つ。

### [🔵 Suggestion] 公開 doc コメントの ADR ID の扱いが、同じ change の中で MAUI と Android で割れている

**該当箇所**: `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/LoadingViewRegistry.kt:9` / `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/ToastViewRegistry.kt:9` (ADR ID を追加) と `maui/KsDialogs.Maui/Registry/ToastViewRegistry.cs:9` (ADR ID を削除)

**問題点**: comment-policy の「公開メンバーの doc コメントには内部用語 (ADR ID を含む) を一切使わない」に対し、同じ change の中で MAUI の Toast レジストリは `(core/ADR-0029)` を落とし、Android の 2 レジストリは `(core/ADR-0025)` → `(core/ADR-0025・0035)` と増やしている。lint は advisory 止まり (要確認 453 件) でリポジトリ全体の既存債務であり、この change に閉じた違反ではないが、触った行での方向がそろっていない。

**推奨修正**: この change で無理に一括整理する必要はない。方針 (公開 doc から ADR ID を落とすのか、当面の既存債務として据え置くのか) をオーナー判断でひとつに決め、決めた方向へ触った行だけをそろえる。据え置きを選ぶなら handbook 側の記述と実運用の食い違いとして drift へ回すのが素直。

### [🔵 Suggestion] Toast の型指定経路で「VM factory と configure が走る時点」が iOS と Android で違う

**該当箇所**: `ios/Sources/KsDialogs/Presentation/ToastCoordinator.swift:141-150` (`beginDisplay` が取り付け前に `makeContent` = `prepare()` を呼ぶ) と `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/ToastCoordinator.kt:147-168` (`attachIfPossible` が提示先を得てから `createContent` = `prepare()` を呼ぶ)

**問題点**: iOS は受理の待ち行列が回った時点で必ず VM 生成と configure を実行するのに対し、Android は提示先 (host Context) が確保できるまで実行を保留する。提示先が現れないまま duration を使い切った表示では、Android では VM factory も configure も一度も呼ばれない。デルタスペックは提示先不在のケースを規定していないため違反ではなく、また「中身の生成は Context を要する」という Android 固有の既存構造から派生した差である (インスタンス渡し経路でも View 生成の時点は同じくずれている)。ただし型指定経路では configure が利用者コードであるぶん、副作用の有無として観測されうる。

**推奨修正**: コード変更は不要と考える。蒸留時に concepts (toast-semantics / 各 platform の toast-surface) へ「configure の実行時点は提示先の確保に従属する (Android)」を 1 行残すか、逆に姉妹面をそろえる価値があるかをオーナーに諮る。

### [🔵 Suggestion] handbook の負のコンパイル検査表が実体より 2 本少ない (本 change の作りではない)

**該当箇所**: `kasane/handbook/cross/test-execution.md` の「負の検査 (フラグなしでは走らない) — 59 本」の節

**問題点**: 実体のフラグ数は ios 16 + android 15 + kmp 11 + maui 19 = 61 本で、表の maui 行に `KsDialogsNegativeCheckLoadingShowOptions` と `KsDialogsNegativeCheckLoadingShowStyle` が載っていない (`maui/KsDialogs.Maui.ApiSurfaceCheck/KsDialogs.Maui.ApiSurfaceCheck.csproj` が正)。本 change は負の検査を 1 本も足していないため、この差は add-loading 由来の既存の取りこぼしである。表は完了判定で 1 本ずつ回す手順の正なので、欠けたぶんは黙って未検証になる。

**推奨修正**: この change のスコープ外なので同梱しない。ksn-drift または ksn-concept で `test-execution.md` の表と本数を実体に合わせる (61 本 + maui の 2 行追加)。

## アクションプラン

1. (任意・優先) Minor 2 件のうち `ComposeTypedShowTests` の後始末は影響が閉じていて修正も小さいため、直すならこの change に同梱してよい (テストのみの変更で、同梱条件を満たす)
2. (任意) Minor 1 件目 (Android Toast の VM 保持) は、コードをそろえるか kdoc を実態に合わせるかのどちらかを選ぶ。どちらも本務のファイル内で完結する
3. Suggestion 3 件はいずれも本 change の外へ回す — 公開 doc の ADR ID 方針はオーナー判断、Toast の configure 実行時点は蒸留時の concepts 追記、handbook の負検査表は drift での棚卸し
