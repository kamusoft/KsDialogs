# レビュー結果: add-page-layout-area (003 回目)

**日付**: 2026-09-26
**判定**: APPROVED

## サマリー

review-002 で修正対象にした 2 件は、どちらも解消している。Major 1 では、MAUI iOS 層でページが決まらないときの逃げ道が、提示先 window そのものから、Native が受け付けて safe area の内側が可視領域を覆う View に替わった。bridge テストは、VC 走査なら結果が変わるホスト構成に組み直され、その構成で差が出ることを前提条件として確かめている。Suggestion 2 では、iOS の空矩形シナリオを FlyoutPage 構成に替えた。MAUI 層の判定が無ければ Native の VC 走査が可視領域を選んで失敗する形になっている。

修正は UIKit の公開の構造 (root と present の連なり) だけを辿り、覆うかどうかは実行時に確かめるので、MAUI の内部構造には依存していない。新しい Critical / Major は無い。低優先度の Suggestion 2 件を残す。

## 照合した規約

- comment-policy.md (always): `scripts/comment-policy-lint.py` で禁止 0 件 (検査対象 1132 ファイル)。修正で書き直したコメント (`FindVisibleAreaView` の remarks、bridge テスト・シナリオの説明) は、外部文書の ID に頼らず単独で読める
- test-execution.md (テストの実行・完了判定): 下の「テスト実行」のとおり、今回動いた面 (MAUI iOS 層・bridge・実配置テストホスト iOS) を回した
- ci-flaky-test-policy.md (Simulator 上で状態遷移を観測するテスト): 新しい bridge テストの待ちは、共通の `BridgeTestWaiting.awaitSettled` (位置が 300ms 変わらないこと) と `waitUntil` を通る。後片付けも、器が閉じたことを待ってからタブを閉じる順になっている
- diagnostic-message-language.md (警告ログを足す・変える): 新しい `NoVisibleAreaViewLead` は英語で、実際の挙動 (VC 走査へ進む) と一致する文になった。逃げ道の View が見つかったときは、既存の `UnresolvedLead` (可視領域を使う) を出す。こちらも実際の挙動と一致する
- 関連する決定: core/ADR-0038 (proposed。MAUI は MAUI 層でページ木を辿り、VC 走査を使わない)
- lessons/code-review.md L-001: 解消の判断では、各テストについて「機構が無いと何が変わるか」を確かめた (解消確認表)

## テスト実行

作業用 Simulator「KsDialogs-add-page-layout-area」を起動して使い、終わったら停止した。他の端末は使っていない。Android 側は今回の修正で動いていないので回していない (review-002 の全件成功の記録を引き継ぐ)。

| ルート | 実行 | 全件 | 失敗 | skip |
|---|---|---|---|---|
| maui/ | `dotnet test` | 197 | 0 | 0 |
| maui/macios/native/ | `xcodebuild test -scheme KsDialogsMauiBridge -parallel-testing-enabled NO` | 9 (5 suites) | 0 | 0 |
| MAUI 実配置テストホスト iOS | `dotnet build -f net10.0-ios` → `simctl install` → `simctl launch --console-pty` | 8 シナリオ | 0 | — |

- 実配置テストホストのアプリは、修正後の `PlatformCurrentPage.cs` より新しい `KsDialogs.Maui.dll` を含むこと (ビルドが最新であること) を確かめてから入れた
- `provider-empty-falls-back` (iOS) の結果: `area=(0,116)-(402,840)|visible=(0,62)-(402,840)|barAbovePage=True|startPlaced=True`。基準は Detail の NavigationPage の先端ページ (ナビゲーションバーの下から) で、可視領域とは違う
- 新しい bridge テスト「登録した関数が root の view を返すと、view controller 階層の先端と違っても可視領域と同じ配置になる」は PASS。前提条件の `#require` (VC 走査が選ぶタブの safe area の下端が、可視領域の下端より上にある) も通った。つまりこの構成では、VC 走査の結果と可視領域が実際に食い違っている
- 実測 (補足): シナリオを流し終えた実配置テストホストに lldb で接続し、root VC (`Microsoft_Maui_Platform_PageViewController`) の view の `safeAreaLayoutGuide.layoutFrame` を読んだ。結果は `(0, 62, 402, 778)` で、可視領域 `(0,62)-(402,840)` と一致した。「MAUI の root の view の safe area の内側は可視領域と一致する」という前提は、実機のプロセスでも成り立っている
- lint: `local-path-lint.py` / `identity-lint.py` は違反なし。`ci-skip-lint.py` は review-002 の補足と同じ理由 (旧 `InstrumentedStateSettling.kt` の削除が index に載っていない) で `FileNotFoundError` になる。既知で、コミット時に削除を stage すれば解消する

## 解消確認表

| # | 指摘 (出典) | 結果 | 根拠 |
|---|---|---|---|
| 1 | MAUI iOS でページが決まらないとき、逃げ道の `UIWindow` が Native に外され、内蔵の VC 走査へ進む (review-002 Major 1) | **解消** | `maui/KsDialogs.Maui/Platforms/iOS/PlatformCurrentPage.cs:71` は、window そのものではなく `FindVisibleAreaView` (`:91`) の結果を返す。`FindVisibleAreaView` は root から present の連なりを辿り、各 VC の view のうち、Native と同じ条件の `Judge` (`:134`) を通り、かつ safe area の内側が window の可視領域を覆うものを最初に 1 つ選ぶ。Native はページの矩形と可視領域の狭い方を基準にするので、覆う View なら結果は可視領域と一致する。全画面のモーダルで root の view が window から外れても、連なりの先のモーダルの view が拾われる。連なりの末尾には、問い合わせ中の Dialog の器自身 (`overFullScreen` で present され、Native はその view の safe area を可視領域にしている) がいるので、Dialog では見つからない状況は事実上起きない。bridge テスト `rootViewAsPageMatchesVisibleArea` (`maui/macios/native/KsDialogsMauiBridgeTests/MauiBridgeCurrentPageTests.swift:59`) は、root の上に `UITabBarController` を `overFullScreen` で出す。これで VC 走査は選択中のタブの view (タブバーの上まで) を選ぶようになり、その差を `#require` で前提として確かめる。機構が無いと何が変わるか: 逃げ道を window に戻すと、Native が外して VC 走査へ進み、ダイアログの下端がタブバーの上に来るので、このテストは失敗する。修正後は root の view が受け付けられ、可視領域と一致して成功する。コメント (`:48-54`, `:78-90`) と `evidence/distill-handoff.md:116` も、今の実装に合わせて書き直されている |
| 2 | iOS の空矩形シナリオが、MAUI 層の判定の有無を見分けられない可能性 (review-002 Suggestion 2) | **解消** | `ProviderEmptyFallsBackAsync` (`maui/KsDialogs.Maui.PlacementHost/PlacementScenario.cs:150`) は、iOS では FlyoutPage + Detail の NavigationPage で組むようになった。期待は `ExpectsBarAbovePage` (基準の上端がナビゲーションバーの下になる)。機構が無いと何が変わるか: iOS の `Judge` が空の safe area を外さずに Native へ渡すと、Native が外して VC 走査へ進む。走査は `UINavigationController` / `UITabBarController` しか降りず、FlyoutPage の root VC (phone では `UIViewController` の派生、iPad では split view) で止まる。その view の safe area は可視領域と同じなので、基準の上端は可視領域の上端 (62) になり、`barAbovePage` と `startPlaced` が偽になって失敗する。今回の結果は上端 116 で PASS している。TabbedPage では見分けられない理由も remarks に書かれている |

### deviation.md との照合

今回の修正で deviation.md の文面は変わっていない。各項は今の実装と 1 対 1 で照合できる。

| 項 | 内容 | 実装との照合 |
|---|---|---|
| 1 | Android の台帳の出入り口と印の型を `@KsDialogsInternalApi` (ERROR) つきの public にする | 一致 (今回の修正範囲外。review-002 の照合から変化なし) |
| 2 | Android の「同じ Activity のウィンドウ」の解釈。MAUI 層も同じ条件で判定し、外れたら次の取得元へ進む | 一致。今回の修正は iOS の「全取得元が外れた後」の逃げ道だけで、Android の判定と、iOS も含む「次の取得元へ進む」共通部分 (`DialogCurrentPageLocator`) には触れていない |
| 3 | iOS の台帳で、表示されていない印を除外する | 一致 (範囲外。変化なし) |
| 4 | [付随修正] Sample Android 2 ルートの移動量欄 | 一致 (範囲外。変化なし) |
| 5 | [付随修正] Sample Android 2 ルートのステータスバー | 一致 (範囲外。変化なし) |

逃げ道を「可視領域を覆う View」にしたこと自体は、spec (maui-binding の「VC 走査は MAUI では使わない」「未描画は dialog-contract の未解決規則に従う」) を満たすための実装手段で、spec からの逸脱ではない。そのため deviation.md に項が無いのは妥当である。

## 指摘事項

### 🔵 Suggestion 1. iOS の空矩形シナリオが見分けられることは、MAUI の FlyoutPage の VC 構成に依存していて、前提を確かめる仕掛けが無い
**該当箇所**: `maui/KsDialogs.Maui.PlacementHost/PlacementScenario.cs:150`
**問題点**: bridge テストは「VC 走査の結果と可視領域が違う」ことを `#require` で確かめてから本体を検査するので、構成が変わって見分けられなくなれば失敗として表に出る。一方、実配置シナリオ `provider-empty-falls-back` (iOS) が見分けられるのは、「MAUI の FlyoutPage の root VC が `UINavigationController` / `UITabBarController` ではない」という MAUI 側の構成のおかげで、それを確かめる仕掛けは無い。将来の MAUI で FlyoutPage の構成が変わり、VC 走査が既定ページと同じ矩形を選ぶようになると、シナリオは黙って見分ける力を失い、PASS のままになる。製品コードは MAUI の内部構造に依存していないので、影響はテストの強さだけである。
**推奨修正**: (任意) シナリオの確認に「VC 走査が選ぶはずの矩形 (FlyoutPage の root VC の view の safe area の内側) が、既定ページの矩形と違う」を前提条件として足し、成り立たなければ FAIL ではなく「見分けられない構成」として報告する。機構が無いと何が変わるか: 今は MAUI の構成が変わっても PASS し続ける。前提条件があれば、構成が変わった時点でその旨が出る。優先度は低く、蒸留時に test-execution.md へ実配置テストホストを載せる判断 (distill-handoff の申し送り) と一緒に扱ってもよい。

### 🔵 Suggestion 2. `evidence/distill-handoff.md` の 6.1 の実行記録が、レビューで直す前のコードでの件数のまま
**該当箇所**: `evidence/distill-handoff.md:3`, `evidence/distill-handoff.md:24-28`
**問題点**: 冒頭で「最終コードの作業ツリー」で回したと書いている。しかしその後のレビューで直した結果、`dotnet test` は 192 → 197 件になり、実配置テストホストは iOS 7 → 8 本、Android 7 → 9 本のシナリオになった。記録の件数とシナリオの一覧 (「両 OS とも … 7 本」) は古いままである。今回の修正で動いたのは同じファイルの 116 行目だけで、実行記録の節は触られていない。蒸留でこの記録を完了確認の根拠として読むと、後から足したシナリオ (`provider-empty-falls-back` / `provider-container-falls-back`) と判定の修正が確認に入っていないように見える。
**推奨修正**: 6.1 の記録の冒頭に「レビュー 001〜003 の修正後の再実行は各 review の『テスト実行』を参照」と 1 行足すか、最終の件数に更新する。

## 確認した観点

- 手段の妥当性 (過剰でないか): 候補の辿り方は UIKit の公開の構造 (root → presented) だけで、MAUI の内部の型名や階層は見ていない。「覆うか」は前提とせず、実行時に `Covers` で確かめる。`Judge` を候補にも掛けるのは、全画面モーダルの下で window から外れた root の view を Native が外すためで、Native の受け付け条件と揃えるのに必要な分に収まっている。review-002 が挙げた「root の view だけを返す」案では全画面モーダル中に外れるので、連なりを辿る分の複雑さには理由がある
- 器の view が候補に入り得る点: 候補に入るのは、Dialog の器 (`DialogContainerViewController`、`overFullScreen`) が present の連なりに載っているときだけである。Loading / Toast は key window に直接重ねるので、連なりに載らない。Dialog の器の view の safe area は、Native が可視領域そのものとして使っている値なので、器が選ばれても結果は可視領域と一致する。ios-native spec の「既定 provider (VC 走査) は器を現在ページに選ばない」は、ページを選ぶ規則である。ここは MAUI 層が「可視領域と同じ結果」を作るための逃げ道で、器を選んでも結果が可視領域から変わらないので、その規則の趣旨に反しない。前の候補 (root など) が先に選ばれるので、器まで進むのは root 側に safe area が足されている等の例外的な場合に限られる
- 残る VC 走査への道: `FindVisibleAreaView` が null を返す (提示先 window が無い、または全候補が外れる) と、Native は VC 走査へ進む。Dialog では上のとおり器自身が末尾の候補になるので、事実上起きない。Loading で root に safe area が足され、かつ present が無い場合などに限って起き得る。その場合も `NoVisibleAreaViewLead` で、VC 走査を使うことを正直に診断する。spec の SHALL を実質的に損なう経路ではないと判断した
- 回帰: 解決できた場合の経路 (`selection.PlatformView` を返す) は変わっていない。実配置の 8 シナリオはすべて PASS で、解決できる構成 (tabbed / shell / modal / flyout / plain / override) の結果は review-002 の記録と同じ座標である。旧 bridge テスト `presentationWindowAsPageMatchesVisibleArea` は新しいテストに置き換えられ、スイートの件数は 9 のまま
- 自動テストの届く範囲 (指摘ではない): C# 側の `FindVisibleAreaView` の選び方そのものは、iOS の実行時にしか動かない。そのため、単体テスト (`net10.0`) にも実配置シナリオ (MAUI 層で必ずページが決まる構成) にも入っていない。固定されているのは要となる前提、つまり「Native が root の view を受け付け、VC 走査と違っても可視領域になる」ことで、bridge テストと上の lldb の実測で確かめた。選び方のロジックは小さく読みで追えるので、ここで追加のテストは求めない
- review-001 / 002 で確定・解消済みの指摘、見送り・降格の判断 (実配置ホストの自動実行、2 回目の問い合わせの扱い等) は蒸し返さない。異論なし

## アクションプラン

1. (任意) Suggestion 2: distill-handoff の 6.1 の記録に、レビュー後の再実行の参照先を足す (蒸留前に済ませると読み違いが無い)
2. (任意・低) Suggestion 1: iOS の空矩形シナリオに、見分けられる構成であることの前提確認を足す
3. (コミット時・既知) 旧 `InstrumentedStateSettling.kt` の削除を stage して、`ci-skip-lint.py` が通ることを確かめる
