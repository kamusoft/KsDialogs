# レビュー結果: fix-kmp-ios-unhandled-exception-crash (002 回目)

**日付**: 2026-09-02
**判定**: APPROVED

## サマリー

review-001 の Major 2 件はいずれも解消している — 提示先待ちの述語は `activationState == .foregroundActive` で絞られ、ライブラリの `ApplicationKeyWindowProvider.selectKeyWindow` と実質同じ判定になった。ライブラリ側の同型欠陥は別 change (`add-kmp-loading-toast-throws`) へ起票され、判断材料も引き継がれている。Minor 2 (識別力のある証跡) の代替 — 「`.task` 開始時点で提示先不在が 23/23」という実測 — は**妥当**と判断する。review-001 が勧めた「提示先不在を N ms 強制する A/B」は、強制がライブラリ側の判定にしか効かず Sample の `hasPresentationHost` は独立に `UIApplication` を見るため、Sample の待ちの効果を識別できない (Sample 側の述語も同時に偽装すれば「ポーリングがポーリングすること」の確認にしかならない)。窓の実在を同じ述語で直接測る今回の代替のほうが、証明したい命題に近い。

残る指摘は、この change が閉じたのと同じ abort 経路が `SamplePresenter` の**非 suspend** 関数 1 本に残っていること (合意スコープの決定文は「suspend 関数全件」なので、literal には範囲外) と、実測の生証跡が `evidence/` に無いこと。いずれも本 change の主目的の達成を妨げず、受け皿の change も既にあるため、判定は APPROVED とする。

### 実行した検証

| 対象 | コマンド | 結果 |
|---|---|---|
| KMP Sample 共有コード | `samples/kmp` で `./gradlew :shared:compileKotlinIosSimulatorArm64` | BUILD SUCCESSFUL |
| KMP iOS Sample アプリ | `samples/kmp/iosApp` で `xcodebuild build -scheme KsDialogsSampleKmp -destination 'platform=iOS Simulator,name=iPhone 17 Pro'` | ** BUILD SUCCEEDED ** |
| KMP ライブラリ (無変更だが依存先) | `kmp` で `./gradlew allTests --rerun-tasks` | 96 tests / 0 failures / 0 errors (handbook の実測値と一致) |
| 動作確認 (Dialog) | 新ビルドを install し `--demo basic-dialog` のコールド起動 4 回 (terminate → 1.5s → launch) | 4 回とも abort なし。うち 1 回はスクリーンショットで Basic Dialog の自動再生まで確認 |
| 動作確認 (Toast) | 同上 `--demo custom-toast` を 2 回 (0.9s 間隔の連続撮影で追跡) | abort なし。登録経路の `カスタムトースト` が起動 ~3 秒後に表示され、duration で消えるところまで確認 |
| lint | `comment-policy-lint.py samples/kmp` / `local-path-lint.py --paths <本 change と add-kmp-loading-toast-throws の成果物 + 変更ファイル>` / `identity-lint.py` | いずれも 0 件 (検査対象 96 ファイル) |

作業ツリーは clean で、「機構の確認」「提示先待ちの実測」で入れた一時変更 (ライブラリの提示判定の強制・Sample の待ちループのログ) の戻し漏れはない。`samples/kmp/shared` にテストソースは無く、Sample を対象にした自動テストは存在しない (前回同様)。

## 照合した規約

- `kasane/handbook/cross/comment-policy.md` (always) — 追加・改訂されたコメントに change / フェーズ・レビュー通番の裸参照はなく、ファイル単独で読める。lint も 0 件
- `kasane/handbook/cross/sample-parity.md` (`samples/**` を触る) — 撮影支援機構はデモ項目の一致要件の枠外。機構自体にかかる一致要件 (引数のキー名・安定デモ ID・不正値の倒れ方) は無変更で、提示先待ちはそのいずれにも当たらない。観測用の一時改変も戻っている
- `kasane/handbook/cross/runtime-behavior-verification.md` (実行時挙動が絡む不具合の調査・完了判定) — 症状の再現 (強制失敗による段階 1 の abort、evidence の 3 件と 35/35 フレーム一致) と修正後の解消 (段階 2)、証跡 (`.ips`) が揃っており 1〜3 を満たす。提示先待ち側の証跡は指摘 2 のとおり自己申告どまり
- `kasane/handbook/cross/test-execution.md` (テスト実行・完了判定) — kmp ルートの全件実行と件数確認を実施 (96 件)
- `kasane/decisions/kmp/0001-swift-interop-plain-suspend.md` — 素の suspend 直接公開は維持。`@Throws` は Sample 側の export 面にのみ足しており、ライブラリの公開面は無変更で ADR の範囲内
- `kasane/lessons/` に `code-review.md` は未作成のため、昇格済みの重点観点・除外観点はなし

## 指摘事項

### [🟡 Minor] 同じ abort 経路が `SamplePresenter` の非 suspend 関数 1 本に残っている

**該当箇所**: `samples/kmp/shared/src/commonMain/kotlin/jp/kamusoft/ksdialogs/samples/kmp/SamplePresenter.kt:251` (`showCustomToast`)

**問題点**: Kotlin/Native の ObjC export は、**非 suspend 関数でも** `@Throws` が無ければ Kotlin 例外を未処理例外として扱いプロセスを終了させる (この change が潰した経路と同一)。`SamplePresenter` の非 suspend 関数のうち `showCustomToast` は `KsToast.show(viewModel = ...)` を呼び、この経路は未登録の ViewModel 型で `DialogException` になる (`kmp/ksdialogs-kmp/src/iosMain/kotlin/jp/kamusoft/ksdialogs/kmp/IosToastGateway.kt:36-38`)。呼び出し側の `samples/kmp/iosApp/KsDialogsSampleKmp/SampleMenuModel.swift:121` は `try` を付けられない (ObjC シグネチャに `error:` が無い) ため、失敗すれば `assertionFailure` ではなく abort する。同じ関数の直後 (同ファイル 122-139 行) にある iOS Native 直呼びの経路は `try` / `catch` で受けており、Sample 内で非対称になっている。

他の非 suspend 関数 (`showDefaultToast` / `showToastStack` / `showToastPlacement`) はメッセージ経路のみで、`KsDialogsInteropToastBridge.show(message:duration:placement:)` が失敗を返さないため露出はない。露出は `showCustomToast` の 1 本に限られる。

**発火条件は狭い**: Toast の受理 (`ToastCoordinator.accept`) は提示先の有無を見ないため、今回の**コールド起動の窓では発火しない**。実際に投げるのは「カスタム Toast の View factory が未登録」という Sample の組み立て誤りのときだけで、現状の Sample は `KsDialogsSampleKmpApp.swift:7-9` の App 初期化で登録しているため競合もない。

**推奨修正**: 合意スコープの決定文は「Swift へ出る **suspend 関数**全件」なので、本 change で直すかは判断が要る。選択肢は 2 つ —
(a) ここで閉じる: `showCustomToast` に `@Throws(DialogException::class)` を足し、`SampleMenuModel.showCustomToast` を `try` / `catch` + `assertionFailure` にする (他の入口と同じ形。1 箇所ずつの追加で済む)
(b) `add-kmp-loading-toast-throws` の未決の論点へ移す: 同 change は `KsToast.show` 非 suspend 2 本の throws 化を扱うため、ライブラリ側が throws 化されれば Sample のこの関数も同時に触ることになる。移す場合は「Sample 側 (`SamplePresenter.showCustomToast`) の export 面も同じ穴を持つ」ことを起票に明記する

### [🟡 Minor] 提示先待ちの実測に生の証跡が残っておらず、識別力を上げる 1 点が未計測

**該当箇所**: `exploration.md`「提示先待ちの実測」節

**問題点**: 代替の方針自体は妥当 (サマリーの理由) だが、2 点足りない。

1. **生証跡が無い**: 一時ログはコミットせず除去済みで、`evidence/` には測定の出力が無い。`runtime-behavior-verification.md` の 3 (レビューと蒸留が主張を検証できる形で証跡を残す) に対し、23/23 は表の自己申告どまりになっている。`@Throws` 側が `.ips` を残しているのと非対称
2. **修正前コードが `show` を呼ぶ瞬間を測っていない**: 表の 2 列は「`.task` 開始時点」と「50ms 後」で、修正前の実行が実際に提示へ進む瞬間 = `await Task.yield()` の直後の提示先有無が測られていない。ここを同じ一時ログで拾えば、「従来の `Task.yield()` 1 回はこの窓をたまたま跨げていただけ」という説明の裏取りになり (跨げていれば余裕の実測、跨げない回があれば待ちの必要性の直接証拠)、待ちの効果に対する識別力がもう一段上がる

また、再現の試行の表 (10/10/5 = 25 回) と実測の表 (10/10/3 = 23 回) で shutdown → boot 条件の回数が 5 → 3 に減っているが、理由が書かれていない。

**推奨修正**: 再測定するなら、一時ログの出力 (sanitize 済み) を `evidence/` に置き、`Task.yield()` 直後の 1 点を列に足す。再測定しない場合は、exploration に「生ログは残していない (一時ログを除去したため)」と明記して、後から証跡を探す人が空振りしないようにする。

### [🔵 Suggestion] 二重に持つ述語に照合先の名指しがなく、厳密には差が残っている

**該当箇所**: `samples/kmp/iosApp/KsDialogsSampleKmp/SampleMenuScreen.swift:138-145`

**問題点**: review-001 の推奨のうち「コメントで照合先の型名を名指しする」部分が入っていない (レビュー対応表にも未実施の記載がない)。現在のコメントは「ライブラリの提示可否と同じ条件に揃える」とだけ書いており、どの型と揃えたのかがファイル単独では追えないため、ライブラリ側が変わったときにずれに気づけない。

厳密には述語にもまだ差がある。ライブラリは「前面アクティブなシーンの window を平坦化して**最初の** key window を採り、その `rootViewController`」を見る (`ios/Sources/KsDialogs/Presentation/ApplicationKeyWindowProvider.swift:23-28` + `ios/Sources/KsDialogs/Presentation/UIKitDialogPresentationSurface.swift:51`) のに対し、Sample は「`rootViewController` を持つ key window が**1 枚でもあるか**」を見る。前面アクティブなシーンに key window が複数ある構成では判定が割れうる (iPhone の単一シーンでは一致する)。

**推奨修正**: doc コメントで `ApplicationKeyWindowProvider` / `UIKitDialogPresentationSurface.canPresent` を名指しする (リポジトリ内のコード識別子への参照はコメント規約で許容)。述語を厳密に揃えるなら、「最初の key window の `rootViewController` が非 nil」に読み替える。

### [🔵 Suggestion] キャンセルされても自動再生には進む

**該当箇所**: `samples/kmp/iosApp/KsDialogsSampleKmp/SampleMenuScreen.swift:101-121, 127-136`

**問題点**: review-001 の Minor 1 は推奨どおり直っている (`Task.sleep` の失敗で待ちを打ち切る) が、`waitUntilPresentationHostIsReady` は戻り値を持たないため、呼び出し側は「準備できた」「上限まで待った」「キャンセルされた」を区別できない。`Task.yield()` はキャンセルで throw しないので、キャンセル後も `model.autoPlay(demo)` まで到達して提示を試みる。Sample の範囲では実害は小さい (この画面が消えるのは概ねアプリ終了時) が、待ちだけをキャンセルに従わせても呼び出し側が従っていない、という半端な状態ではある。

**推奨修正**: 待ちの後に `if Task.isCancelled { return }` を置くか、待ちが `Bool` を返して呼び出し側が分岐する。

### [🔵 Suggestion] レビュー対応表に未実施部分が書き分けられていない

**該当箇所**: `exploration.md`「レビュー対応」表

**問題点**: Major 1 の対応欄は「同じ述語にした」で完了と読めるが、推奨の後半 (照合先の名指し) は入っていない (上の Suggestion)。Suggestion「`@MainActor` の付け方が不揃い」の欄も「両方から外し」とあるが、`hasPresentationHost` には元から付いていない。次サイクルのレビューは差分と対応表を突き合わせるため、部分対応・見送りは「未実施 (理由)」と書き分けてあると、同じ点を再指摘する往復が減る。

**推奨修正**: 記録の書き換えは指示しない。次回以降の書き方として。

## アクションプラン

1. **指摘 1** の (a) か (b) をオーナーに諮る。(b) なら `add-kmp-loading-toast-throws` の未決の論点に 1 行足すだけで閉じられる
2. **指摘 2** の後段 (生ログを残さなかったことの明記、または再測定しての `evidence/` 追加) を蒸留前に処理する
3. 指摘 3・4 は任意。3 は 1 行のコメント追記で将来のずれ検知に効く
4. 指摘 5 は次回以降の運用として
