# レビュー結果: add-page-layout-area (001 回目)

**日付**: 2026-09-26
**判定**: APPROVED

## サマリー

基準領域 `currentPage` を core 契約 (ケース表 C24〜C28) と iOS / Android / MAUI の 3 面に入れた実装は、デルタスペックの全 Scenario を満たす。deviation.md の 3 項 (Android の内部 API のオプトイン公開、Android の「同じ Activity のウィンドウ」の解釈、iOS 台帳の非表示除外) と付随修正 2 件は、記述と実装が 1 対 1 で一致する。計算部 (resolver) は `currentPage` の 1 分岐を足しただけで、ページの矩形は窓から見た 4 辺の inset として既存の手順に流れる。取得元の優先順位・提示先ウィンドウとの同一性・未解決時のフォールバックと診断は、3 面で同じ形に揃っている。全ルートのテストを独立に回し直して全件成功を確認した。MAUI の実配置テストホストも両 OS で 7/7 PASS だった。指摘は優先度の低い Minor 2 件と Suggestion 3 件だけ。一致検証 (Scenario ごとの対応表) は `verify-001.md` に分けて出力した (VALID)。

## 照合した規約

- comment-policy.md (always): `scripts/comment-policy-lint.py` で禁止 0 件。新規コメントは作業文書への参照を持たず、ADR は `core/ADR-0038` の ID 形式だけ
- test-execution.md (テストの実行・完了判定): 8 ルートを全件実行し件数を確認した (verify-001.md「テスト実行」)。instrumented は `:ksdialogs-core` と `:ksdialogs` の 2 つの XML を合算。Swift Testing / XCTest の 2 系統を確認
- sample-parity.md (samples/ を触る): 文言表の改訂 (基準領域の行・タブ名・Info タブ) と画面構成の表を ui/brief.md と突き合わせて一致。読み上げの規約の改訂 (基準領域の選択肢・タブ) と 6 面の証跡 (`evidence/panel-accessibility-*.txt`) あり
- diagnostic-message-language.md (ライブラリ本体に警告ログを追加): 未解決の診断は英語で、iOS と Android の書き出しの文 (`The current page could not be resolved, so the visible area is used instead.`) が同じ。MAUI の lead も同じ文。規約の日本語リテラル grep は出力なし
- ci-flaky-test-policy.md (Simulator / instrumented で状態遷移を観測するテストを書く): 指摘 2 を参照
- 関連する決定: core/ADR-0038 (proposed。実装は Decision 1〜5 と一致)、core/ADR-0030・0032 (Toast の自動検知の却下は据え置き。Toast には触れていない)、android/ADR-0001 (Compose の modifier は `:ksdialogs` に置き、`:ksdialogs-core` は Compose 非依存のまま。`verifyNoDeclarativeUiDependency` は成功)
- lessons/code-review.md の L-001: 下の推奨修正のうち、検査を足す推奨には「機構が無いと何が変わるか」を書いた

## 確認した観点

- 仕様充足: 全 Scenario に実装とテストがある (verify-001.md)。tasks.md に虚偽チェックなし。足場 (proposal / specs / brief / ADR-0038) の書き換えなし
- 堅牢性: provider の null・例外・別ウィンドウ・空の矩形・窓外は、どれも「見つからなかった」扱いで次の取得元へ進む。iOS は `throws`、Android は `Exception`、MAUI は `Exception` で捕まえ、表示は失敗しない。非有限の矩形は iOS では弾いている。Android はページ矩形の非有限を明示では弾いていないが、`getLocationOnScreen` + 整数の幅と高さから作るため非有限にはならない。器自身のウィンドウは Android では印 (`R.id.ksdialogs_container_window`) で外し、iOS では VC 走査で通り抜ける
- 状態とライフサイクル: iOS の台帳は View を弱参照で持ち、`didMoveToWindow` で出入りする。Compose は `Modifier.Node` の attach / detach で出入りし、detach 時に座標と host を手放す。Android の再問い合わせ `Runnable` は `onDetachedFromWindow` で外す。iOS の再問い合わせ `Task` は次の入力変化で取り消す
- 再配置: 再問い合わせのきっかけは従来どおり窓寸法と insets だけ (Non-Goal のページ遷移は加えていない)。同じきっかけで 1 回だけ問い合わせ直すのは、ページ側のレイアウトが遅れる状況 (回転・別ウィンドウ) への対処で、Android は収束パスの上限を共有していて無限ループにならない
- 公開面: iOS `DialogLayoutArea.currentPage` / `DialogCurrentPage.provider` / `View.ksDialogCurrentPage()`、Android `CURRENT_PAGE` / `DialogCurrentPage.provider` / `Modifier.ksDialogCurrentPage()`、MAUI `CurrentPage` / `DialogCurrentPage.Provider`。3 面とも正の公開面検査に載っている。`:ksdialogs` の compose-ui を `api` にしたのは、modifier が `Modifier` を公開面に出すためで妥当
- Sample: 4 ルートとも正規の登録口を使っている (iOS 系は各タブの content 枠に `.ksDialogCurrentPage()`、Android 系は Scaffold のバーの内側に modifier、MAUI は既定 provider に任せ登録なし)。36 枚の証跡を並べて見比べ、6 面で End/End・Start/Start・Info タブの位置関係が揃っていることを確かめた

## 指摘事項

### 🟡 Minor 1. MAUI の実配置テストホストがどの自動実行・ソリューションにも入っていない
**該当箇所**: `maui/KsDialogs.Maui.PlacementHost/` (未追跡の新規プロジェクト)、`maui/KsDialogs.slnx`
**問題点**: maui-binding の「MAUI 層の既定 provider」の 5 Scenario と「provider の上書き」について、ダイアログが実際にその矩形を基準に置かれることを確かめる手段は、このテストホストだけである。単体テスト (`DialogCurrentPageLocatorTests`) が見るのは、どのページを選ぶかまで。ところがこのホストは `KsDialogs.slnx` に入っておらず、`dotnet test` にも CI にも載らない。起動方法 (iOS は `simctl launch --console-pty`、Android は logcat の `KSDPLACEMENT` 行) も handbook のどこにも書かれていない。このままだと MAUI の配置や写像を変えたときにホストがビルドごと壊れても気づけず、次の変更でも回されない。今回は両 OS で 7/7 PASS を確認済みなので、現時点の挙動は問題ない。
**推奨修正**: distill-handoff.md の「handbook 追記の候補」1 項目めを蒸留で採る。test-execution.md の maui/ 節に、実行手順と完了判定の読み方 (`SUMMARY|passed=N|failed=0`) を載せる。あわせて、少なくともビルドだけは検出できるようにしたい (slnx に足すと `dotnet test` が両 OS のアプリビルドを巻き込んで重くなるため、CI の maui job で `dotnet build` を 1 行足すなどの形がよい)。本 change の完了を止める指摘ではない。

### 🟡 Minor 2. 新しい instrumented テストの落ち着き待ちが共通プリミティブを使わずに書き起こされている
**該当箇所**: `android/ksdialogs-core/src/androidTest/kotlin/jp/kamusoft/ksdialogs/support/DialogCurrentPageStage.kt:171` (`pollUntil`)、`android/ksdialogs/src/androidTest/kotlin/jp/kamusoft/ksdialogs/compose/ComposeCurrentPageTests.kt:264` (`awaitSettled`)
**問題点**: handbook cross/ci-flaky-test-policy.md「観測は終端状態の合意を待つ」は、落ち着き待ちを共通プリミティブ (`InstrumentedStateSettling.awaitSettled` / `StateHistory`) に置き、テストごとに書き起こさないことと、時間切れの説明文に単調時計付きの観測履歴を添えることを求めている。新しい 2 つの待ちは、自前のポーリング (「2 回続けて同じ外形」「レイアウト要求が無い」) で安定を判定している。時間切れのときも `false` と固定文しか残らないため、CI で間欠的に落ちたときに、その回に何が起きていたかを後から読めない。今回の実行 (専用 AVD) では安定して通っており、判定の条件そのもの (レイアウト済み + 再要求なし + 外形の不変) は妥当。
**推奨修正**: `:ksdialogs-core` 側の `DialogCurrentPageStage` は `InstrumentedStateSettling.awaitSettled` に寄せ、外形の履歴を説明文に添える。`:ksdialogs` の Compose テストは `:ksdialogs-core` の androidTest の支援コードを参照できない。そのため同じ形にそろえるか、共有の置き場 (`android/layout-case-fixtures` のようなテスト支援の共有ソース) へ移すかを実装側で判断する。優先度は低い (CI で間欠失敗が出たら最初に直す箇所として記録しておく程度)。

### 🔵 Suggestion 3. `@KsDialogsInternalApi` のオプトインの強制を負の検査で固定する
**該当箇所**: `android/ksdialogs-core/src/main/kotlin/jp/kamusoft/ksdialogs/KsDialogsInternalApi.kt`、`android/api-surface-check/build.gradle.kts`
**問題点**: deviation 1 の合意の前提は「利用者はオプトインしない限りコンパイルエラーになる」ことである。これは `@RequiresOptIn(level = ERROR)` という言語の仕組みで保証されるが、注釈が外れたり、レベルが `WARNING` に下がったりしたことを検出する検査が無い。台帳の入口が警告だけで呼べる状態になっても、既定のビルドは通ってしまう。
**推奨修正**: `api-surface-check` の負の検査に 1 本足す (例: フラグ `ksdialogs.negativeCheck.internalLedger` で、オプトインなしに `DialogCurrentPageLedger.attach(...)` を呼ぶ)。期待する診断は opt-in 要求のエラーで、test-execution.md の負の検査の表にも 1 行足す。機構が無いと何が変わるか: `@KsDialogsInternalApi` を外すか `WARNING` にすると、この検査はビルドが成功し、検証失敗になる。注釈がある限りはビルドが失敗する。つまり機構の有無で結果が分かれる。

### 🔵 Suggestion 4. MAUI の Toast gateway だけ MAUI 層の provider を登録しない
**該当箇所**: `maui/KsDialogs.Maui/Platforms/iOS/PlatformToastGateway.cs`、`maui/KsDialogs.Maui/Platforms/Android/PlatformToastGateway.cs` (Dialog / Loading の gateway は生成時に `PlatformCurrentPage.EnsureInstalled()` を呼ぶ)
**問題点**: Toast に `currentPage` を渡したときの挙動は Non-Goal (「値を渡せば Dialog と同じ規則で解決する」以上を固定しない) なので、違反ではない。ただ Toast だけを使うアプリ (Dialog / Loading の gateway が一度も生成されない) では、MAUI 層の provider が Native に登録されない。その結果、iOS では maui-binding が「MAUI では使わない」とした Native 内蔵の VC 走査に落ち、Android では未登録として可視領域に落ちる。どの器から使い始めたかで解決経路が変わる。
**推奨修正**: Toast の gateway の生成時にも `EnsureInstalled()` を呼んで 3 つの器をそろえるか、Toast での `currentPage` は非対応であることを Loading / Toast の論点を扱う後続の change に申し送る。

### 🔵 Suggestion 5. Sample の Material 3 の版を直書きしている
**該当箇所**: `samples/android/app/build.gradle.kts:57`、`samples/kmp/androidApp/build.gradle.kts:58` (`"androidx.compose.material3:material3:1.3.1"`)
**問題点**: 同じファイルの他の依存は version catalog (`libs.versions.androidx.compose` / `libs.kotlinx.coroutines.android`) で版を引いており、material3 だけが直書きになっている。2 ルートで同じ版を別々に持つため、更新時に片方だけ上がる余地がある。
**推奨修正**: catalog に material3 の版を足し、2 ルートからそれを参照する。

## アクションプラン

1. (任意・本 change 内) Suggestion 4: Toast gateway の `EnsureInstalled()` をそろえるか、後続 change へ申し送る
2. (蒸留時) Minor 1: test-execution.md に MAUI 実配置テストホストの手順を載せる (distill-handoff.md の候補を採る)。CI でビルドだけ検出する形を検討する
3. (後続) Minor 2: 新しい instrumented テストの待ちを共通プリミティブへ寄せる。CI で間欠失敗が出たら最優先
4. (後続) Suggestion 3・5: 負の検査の追加、material3 の版の catalog 化
