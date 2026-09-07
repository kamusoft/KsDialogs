# レビュー結果: add-presentation-behavior (002 回目)

**日付**: 2026-08-22
**判定**: APPROVED

## サマリー

1周目の突き合わせ表 #1〜#7 の対応を、成果物と deviation.md と自分で回した実測だけで確認した。**#1〜#5 はすべて解消、#6 / #7 は申し送りへ記録済み**で、対応漏れ・見せかけの対応 (テストだけ通して構造は直っていない類) は見つからなかった。特に #2 は、提示面を完了通知つきに変えただけでなく **テスト面が「閉鎖の要求」と「撤去の完了」を別々に進められる形になり、PB-TR-10 / PB-TR-13 が要求と完了の間の窓を実際に観察している** — 1周目に相方が指摘した「テスト surface の同期撤去で差が隠れる」構図は解消された。

副対象の退行確認も、iOS 130 / android 50 / kmp 51 / maui 62 / bridge 15 / 負の検査 4 本 / スクリプト 3 種をすべて自分で回して green だった (iOS は 129 → 130、増分は新設の閉鎖完了テスト 1 本と一致)。

指摘は Minor 2 / Suggestion 1 で、いずれも #2 の修正が作った新しい待ち合わせ窓の周辺にある**堅牢性とコメントの正確さ**の話。Scenario を破るものではないため APPROVED としたが、**M-1 のコメント訂正だけはアーカイブ前に入れてほしい** (実態と食い違う説明が長命層へ写るため)。

---

## 1周目の指摘への対応状況

| # | 指摘 | 判定 | 根拠 (自分で確認したこと) |
|---|---|---|---|
| 1 | iOS `DialogTransition.defaultDuration` の public 露出 | **解消** | 下記 |
| 2 | iOS の配送が UIKit の撤去完了を待たない | **解消** | 下記 |
| 3 | `scenario-id-coverage.py` がソース全文から ID を抽出 | **解消** | 下記 |
| 4 | handoff-distill.md の maui 件数 | **解消** | `handoff-distill.md` は 62、自分の実測も 62 (失敗 0 / スキップ 0) |
| 5 | パネル操作部の accessibility 証跡 | **解消** | 下記 |
| 6 | フック未完了警告の有効条件の非対称 (Suggestion) | **申し送り済み** | `handoff-distill.md` に「プリビルド xcframework 配布へ変える際は iOS 側の判定を見直す / transition-semantics.md の『デバッグビルド』の主語を明記」まで書かれている |
| 7 | Android の「ミリ秒に落とすと 0 になる duration」(Suggestion) | **申し送り済み** | 同上。transition-semantics.md への並記として記録 |

### #1 — 解消

`ios/Sources/KsDialogs/Contract/DialogTransition.swift` で、定数は `public extension` の外の素の `extension DialogTransition` (= internal) へ移り (:154-160)、プリセットの既定引数は `duration: TimeInterval = 0.25` のリテラルに戻っている。推奨修正の 2 案のうち後者で、design Decision 3 の完全シグネチャと字面が一致した。Android の `internal val DEFAULT_DURATION` / MAUI の `internal static readonly DefaultDuration` との非対称も解消。

コメント (「公開関数の既定引数式は呼び出し側へ展開されるため、内部の定数を参照できない」) が、なぜリテラル重複が必要かを単独で説明できている点も良い。

固定手段も入った: `ios/Tests/KsDialogsTests/DialogAttributeCompileChecks.swift:108-114` の `KSDIALOGS_NEGATIVE_CHECK_DEFAULT_DURATION`。このファイルは `@testable import` を使わない (冒頭にその理由のコメントあり) ため、利用者と同じ可視性で検査できている。自分で回した結果も期待どおり:

```
DialogAttributeCompileChecks.swift:112:30: error: 'defaultDuration' is inaccessible due to 'internal' protection level
** TEST BUILD FAILED **
```

### #2 — 解消

構造が3点とも直っている。

1. **面の契約**: `DialogPresentationSurface.dismiss(_:completion:)` が `DialogRemovalCompletion` を取る形になり、doc コメントで「提示関係の解消と View の取り外しまで終わったら completion を呼ぶ / 既に画面から外れている器には待つ相手がいないのでその場で呼ぶ」と定義されている
2. **UIKit 実装**: `UIKitDialogPresentationSurface.dismiss` が `presenting.dismiss(animated: false) { … completion() }` を使う。`presentingViewController` が nil の (= 既に連なりから外れた) 場合の即時 completion も明示
3. **器**: `finishRemoval(waitsForHostRemoval:)` が撤去要求を出して完了通知を待ち、`completeRemoval()` で初めて `releaseContentHost()` → `settle` → 配送へ進む。相方が求めた「host-lost 経路だけは撤去済みとして即配送」も `handleHostLost()` → `finishRemoval(waitsForHostRemoval: false)` として入っている

テストも「即完了ダブルで素通し」にはなっていない。`Tests/Support/DialogTestPresentationSurface.swift` の `holdsDismissalCompletion` / `heldDismissals` / `completeHeldDismissals()` は**保留中は器の View を外さない**ので、要求と完了の間の状態が本当に作れる。PB-TR-10 はその窓で `heldDismissalCount == 1` かつ `presentedContainers.count == 1` かつ `container.isOutcomeDelivered == false` を観察し、`completeHeldDismissals()` の**直後に同期で** `isOutcomeDelivered` が立つことまで見ている (= 配送が完了通知の経路で起きている証拠になっている)。PB-TR-13 も同型。

### #3 — 解消

`scripts/scenario-id-coverage.py` の `extract_declaration_ids(text, ext)` が、宣言行と**その宣言に続く属性・注釈だけ**を見る形になり、`COMMENT_LINE_PATTERN` でコメント行を除外している。宣言の見分けが定義されていない拡張子は 0 件扱いにする (黙って通さない) 判断も妥当。自分で回した `--selftest` は 9 ケースを含む全件 OK で、要求されていた「コメントだけの ID は数えない」が Swift / C# / 本文の証跡ファイル名の 3 通りで確認できる。`verification/scenario-id-coverage/README.md` にも「数える場所はテストの宣言に限る」節と、限定しない場合の偽陽性 (`--require-mirror` が通ってしまう) の説明が入った。

実行結果は 58/61 (除外 3)・未網羅なし・`--require-mirror` OK で、限定によって取りこぼした ID は無い。

### #5 — 解消

`verification/panel-accessibility/` に 4ルート 6形態 × 2状態 (initial / changed) の accessibility tree と README。規約 (`sample-parity.md`「パネル操作部の読み上げ」) の各項を 6形態で並べた照合表があり、取得方法・限界 (`uiautomator dump` が `RangeInfo` を書き出さないこと) まで書かれている。先例 (`archive/2026-08-19-expand-api-surface/verification/panel-accessibility/`) の踏襲も明記。`ui/brief.md:176-186` からも参照されている。

MAUI iOS だけ調整部の無効状態が読み上げに乗らない件は、(a) 操作不可自体は成立することを別プローブで裏取り、(b) 原因を `IsEnabled` 伝播として特定、(c) 規約の状態列挙外なので違反とは断定せずオーナー判断へ、と処理されており、`handoff-distill.md` にも同内容が載っている。証跡の質としては先例より厚い。

---

## 「撤去完了の合図が 2 つ」の設計判断の評価

deviation.md「実装メモ」に追加された、**UIKit の `dismiss(animated:completion:)` の完了通知と、器自身の `viewDidDisappear` の 2 経路のうち先に届いた方で後始末へ進む**という判断について。

### 結論: 契約に照らして妥当。ただし 1 行の締めを推奨 (下記 S-1)

**(a) どちらの合図も契約の字義を満たす。** PB-TR-10 の THEN は「フック完了と器の撤去の後に completed が配送される」、PB-TR-13 は「器が撤去された後」、design Decision 5-1 は「dismissal フック完了 + オーバーレイのフェード完了 + 器の撤去の後」。`viewDidDisappear` は器の View がウィンドウ階層から外れ終わった後に呼ばれるので、「器の撤去の後」という条件は満たしている。配送の早すぎる発火にはならない。

**(b) 2 経路目はテスト都合の逃げではなく、構造上必要な保険になっている。** `finishRemoval` は撤去要求を出す**前**に `containerState = .removed` を立てる。この後に器が画面を失うと `handleHostLost()` は `guard containerState != .removed` で早期 return し、提示機構からの完了通知も (提示関係が壊れているので) 期待できない。この窓では `viewDidDisappear` だけが唯一の合図で、これが無いと `completeRemoval()` に到達せず **show が永久に返らない**。deviation.md はこの動機を「提示機構の完了通知が届かない状況で show が返らなくなるのを避けるため」と書いており、実測 (シーンを持たないテストランナー) を根拠として挙げているが、本質は上記のとおり production 側にもある宙吊り回避で、判断としては筋が通っている。

**(c) 「先着で 1 回」は実際に 1 回に切り詰められている。** `completeRemoval()` は `didCompleteRemoval` で、配送は `deliverPendingOutcomeIfPossible()` が `pendingDelivery` / `onDelivery` を nil に落とすことで、それぞれ二重実行が塞がれている。`releaseContentHost()` も `contentHost` を nil に落とす形で冪等。二重配送・二重解放の穴は無い。

**(d) 契約テストは骨抜きになっていない。** 2 経路目が発火するのは器の View が実際に外れたときだけで、`holdsDismissalCompletion` の保留中は View を外さない。したがって PB-TR-10 / PB-TR-13 は完了通知だけを観察しており、`viewDidDisappear` に迂回されていない。

**(e) 弱いのは 1 点だけ** — `.removed` 分岐が `presentingViewController` を見ずに即 `completeRemoval()` すること。これは S-1 として下に書く。

---

## 指摘事項

### [🟡 Minor] M-1: 配送が器の生存に依存するようになり、`viewDidDisappear` のコメントが実態と食い違う

**該当箇所**: `ios/Sources/KsDialogs/Presentation/DialogContainerViewController.swift:354-357` (コメント)、`ios/Sources/KsDialogs/Presentation/DialogPresenter.swift:64-71`

**問題点**:

本変更で配送経路が `resultChannel.onSettle` から `container.onDelivery` へ移った (DialogPresenter の diff で確認)。その結果、**器が生きていることが配送の前提になった**。器を強く握っているのは提示面だけ (production では UIKit の提示関係、テストでは `presentedContainers` / `heldDismissals`) で、配送に関わる参照はすべて weak:

- `DialogPresenter.onDismissRequest` は `[weak container]`
- `finishRemoval` が提示面へ渡す完了通知は `{ [weak self] in self?.completeRemoval() }`
- `viewDidDisappear` の後始末も `Task { @MainActor [weak self] … }`

したがって、**撤去要求から完了通知までの窓で器が解放されると、どの経路も no-op になり `withCheckedContinuation` が再開されない** — show が永久に返り、`CheckedContinuation` のリーク警告が出る。#2 の修正でこの窓が新設された分だけ、露出は 1 周目より広がっている。

さらに、`viewDidDisappear` に残っているコメントが現在の結線と食い違う:

```swift
guard let self else {
    // 器が解放されている = もう画面には戻らないので、待っている呼び出し元を解放する。
    resultChannel.settle(.cancelled, origin: .hostLost)
    return
}
```

本変更前はチャネルのハンドラが配送口だったのでこの説明は正しかったが、今は **`settle` はラッチであって配送ではない** (Decision 5-1 が明示的に分離した概念)。器が既に解放されているこの分岐では、`settle` しても `onSettle` の観察者は `[weak self]` で消えており、呼び出し元は解放されない。

到達性は低い (UIKit は通常、完了通知を呼ぶまで提示中の器を生かす) ため Minor とした。ただし症状は「show が返らない」で、`handoff-distill.md` の MAUI 側に同種の防御 (`DialogPresentationCompletion.Deliver()` の保険) が入っていることを思えば、iOS 側だけ無防備なのは非対称でもある。

**推奨修正**: 最低限、コメントを実態に合わせる (「ラッチだけ行う。この経路では呼び出し元は解放されない」等) こと。**これはアーカイブ前に入れてほしい** — 誤った説明のまま蒸留で長命層へ写ると、次に触る人が「チャネルの確定で呼び出し元が解放される」前提で設計を読む。

加えて、どちらかで穴自体を塞ぐことを検討:

1. 撤去の完了を待つ間だけ器を強く握る (`finishRemoval` が提示面へ渡す完了通知で strong self を捕まえる)。1 行で「器の寿命 ≧ 撤去の完了」を確定できる
2. 器が死んだときの最後の砦を結果チャネル側に戻す (presenter がチャネルへ配送のフォールバックを預ける)

いずれも今回のサイクルで必須とは考えないので、申し送りでも構わない。

### [🟡 Minor] M-2: `UIKitDialogPresentationSurface.dismiss` の完了通知は実行体で検証されていない

**該当箇所**: `ios/Tests/KsDialogsTests/UIKitDialogPresentationSurfaceTests.swift:82-105`

**問題点**: 新設の `dismissCompletionArrivesForUnpresentedContainer` が検証しているのは「提示の連なりに載っていない器 = `presentingViewController` が nil の早期 return 側」だけで、**本命の `presenting.dismiss(animated: false) { … }` 経路は 1 本も通っていない**。テスト自身のコメントもそう断っている (「提示済みの器を閉じたときの完了は…この環境では届かない」)。

つまり #2 の修正の中核である「UIKit の撤去完了を受けてから配送する」結線は、自動検査では**器の側 (テスト面との組み合わせ) しか**確かめられておらず、提示面の側は実機・実アプリでの目視に依存している。`MainActor.assumeIsolated { completion() }` が実際に MainActor 上で呼ばれること (= 前提が崩れたらクラッシュする箇所) も同様に未検証。

`verification/sample-walkthrough/` に 4ルートの通し証跡はあるが、README を読む限り観察点は演出の見た目と結果表示で、「撤去完了 → 配送」の順序を直接見た記録ではない。

**推奨修正**: cross の `runtime-behavior-verification` 規約に沿って、Sample の通しで「結果を受け取った直後に次のダイアログを出す」操作を 1 つ足し、前のダイアログが残らないことを証跡に残す (これが #2 の実害シナリオそのもの)。実施しないなら、この経路が自動検査の外にあることを `handoff-distill.md` へ 1 行残し、配布形態やシーン構成が変わったときの見直し対象にしておく。

### [🔵 Suggestion] S-1: `viewDidDisappear` の `.removed` 分岐に提示関係の確認を足すと、2 経路目が完了通知に譲る

**該当箇所**: `ios/Sources/KsDialogs/Presentation/DialogContainerViewController.swift:359-364`

**問題点**: `.removed` 分岐は `presentingViewController` を見ずに `completeRemoval()` へ進む。UIKit は**撤去の完了通知より前に** `viewDidDisappear` を呼ぶため、提示関係の解除が終わりきる前にこの分岐へ入る可能性が残る。同じメソッドの直前のコメント (:352「提示関係の解除は画面から外れた直後に済むとは限らないため、判定は次の機会に行う」) が host-lost 側でこの性質を明示しているのに、`.removed` 側だけ同じ性質を考慮していない — 非対称としても目に付く。実際には `viewDidDisappear` 側が `Task { @MainActor }` で 1 ホップ譲るのに対し、`animated: false` の完了通知は同じ callout 内で呼ばれるのが通例なので完了通知が先着し、実害はまず出ない。ただし先着順が入れ替わった場合には、相方が #2 で挙げた実害シナリオ (結果を受けて直ちに次を出すと、`topmostViewController()` が `isBeingDismissed` で打ち切った提示元へ present してしまう) が顔を出しうる。

**推奨修正**: `.removed` 分岐にも `presentingViewController == nil` の条件を足す。上記「妥当性の評価 (b)」の宙吊り回避は、器が画面を失った窓では `presentingViewController` が nil になるため保たれたまま、通常の撤去では完了通知に譲れる。あわせて 2 経路が同義であることの説明 (今のコメント) に「提示関係が解けていることまで確認したうえで」を足せば、次に読む人が先着順の意味を取り違えない。

---

## 確認した観点 (指摘に至らなかったもの)

- **状態機械の退行**: `finishRemoval` の割り込みで `containerState = .removed` が撤去要求より前に立つ点は、`reportOutsideTap` (退出中・撤去後の入力無視)・`handleSettled` (removed で何もしない)・`handleHostLost` (removed で早期 return) のすべてで整合が取れている。`handleCallerCancellation` の `.dismissing → finishRemoval` (脱出口) も残っており、PB-TR-24 / PB-TR-25 が実際に「フックは完了していないが器は撤去される」を観察している
- **提示前の撤去 (PB-TR-19 / PB-TR-20)**: `scheduleImmediateRemoval` が 1 ホップ譲ってから `finishRemoval()` する形は、完了通知待ちを入れても壊れていない (提示が始まっていなければ UIKit 面は `presentingViewController == nil` で即 completion)。両 Scenario とも green
- **多段表示**: UIKit の `dismiss` が上に重なった器もまとめて外す挙動と、上の器が `viewDidDisappear` → `handleHostLost` で cancelled になる経路は従来どおり。テスト面の `removeFromPresentation` も「その器と上に重なっている器を一緒に外す」で対応関係を保っており、PB-MD-01〜05 が green
- **Android / MAUI の非対称**: Android の提示面は `PresentedDialog.onDelivery` 形で、撤去がその場で終わる (View の取り外し) ため完了通知の概念を持たない。iOS だけ completion を足したことは形態差として妥当で、doc コメントにも「配送は…器の撤去がすべて済んだあとに1回だけ」と同じ規約が書かれている
- **tasks.md**: 36 件すべてチェック済みで未チェックなし。1周目に実在を確認済みの項目 (1.5 のスクリプト・5.2 の実機証跡・8.4 の連写証跡) に加え、今回の修正で触れた 1.5 / 2.2 の実体も再確認した
- **足場**: proposal / design / specs に書き換えなし (diff は tasks.md と ui/brief.md のみ)
- **deviation.md**: 追加された「撤去完了の合図が2つ」以外に新しい乖離は見当たらない。追加分は公開面に影響せず、契約の字義を破らないので「実装メモ」への分類も妥当

## アクションプラン

1. **[Minor]** M-1 のコメント訂正 (`DialogContainerViewController.swift:354-357`) — **アーカイブ前に必須**。穴自体を塞ぐかは任意 (申し送りでも可)
2. **[Minor]** M-2 — Sample 通しに「結果直後に次を出す」観察点を足すか、自動検査の外にあることを `handoff-distill.md` へ記録
3. **[Suggestion]** S-1 — `.removed` 分岐へ `presentingViewController == nil` の条件を足す (1 行)

## 実行結果 (本レビューでの実測)

| ルート | コマンド | 結果 |
|---|---|---|
| ios/ | `xcodebuild test -scheme KsDialogs -destination 'platform=iOS Simulator,name=iPhone 17'` | **130 tests / 25 suites / 0 failures** (TEST SUCCEEDED) |
| iOS 負の検査 | `DEFAULT_DURATION` / `NONE_ARGUMENT` / `SHOW_TRANSITION` / `OPTIONS_TRANSITION` の 4 フラグ | 全件が期待診断で TEST BUILD FAILED。新フラグは `'defaultDuration' is inaccessible due to 'internal' protection level` |
| android/ | `./gradlew test` | 50 / 0 (xml 集計) |
| kmp/ | `./gradlew allTests` | 51 / 0 (xml 集計) |
| maui/ | `dotnet test` | **62 / 0 / スキップ 0** |
| maui/macios/native | `xcodebuild -project KsDialogsMauiBridge.xcodeproj -scheme KsDialogsMauiBridge build` | BUILD SUCCEEDED |
| maui/android/native | `./gradlew :ksdialogs-maui-bridge:test` | 15 / 0 (xml 集計) |
| スクリプト | `scenario-id-coverage.py` | 58/61 (除外 3)・未網羅なし |
| スクリプト | `scenario-id-coverage.py --selftest` | 全件 OK (宣言限定の 9 ケースを含む) |
| スクリプト | `scenario-id-coverage.py --require-mirror` | 対象領域の ID は iOS / Android 双方にあり |
| lint | `comment-policy-lint.py` | 禁止 0 件 / 540 ファイル |

Android instrumented (Pixel 6a) は本サイクルで Android 側の変更が無く、1周目で 138 / 0 / skipped 1 を確認済みのため回していない。

## 補足 (指摘ではない)

`ui/brief.md` の「オーナーの最終承認: 未取得」は 1周目と同じくレビュー対象外のゲートとして残っている。accessibility の所見1 (MAUI iOS の無効状態) も同じくオーナー判断待ちとして brief と handoff の両方に記録されており、レビューとしては「証跡と判断材料が揃っている」ところまでを確認した。
