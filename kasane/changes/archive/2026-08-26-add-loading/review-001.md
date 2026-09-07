# レビュー結果: add-loading (001 回目)

**日付**: 2026-08-26
**判定**: CHANGES_REQUESTED

## サマリー

Loading の4形態実装は、デルタスペックの Requirement / Scenario を漏れなく実装・検証しており、実装品質は高い。共有部品の切り出し (`DialogTransitionRunner` / `DialogLayoutApplier` / `DialogLayoutSnapshot` の供給読み取りの外出し / maui `PlatformDialogContent`) はいずれも挙動不変の機械的な移送で、既存テスト全通過を確認できた。Scenario ID 網羅 (141/150・除外9)・両 Native ミラー検査・新規の負の API 形状検査6本・3種の lint もすべて期待どおり通る。

一方で **ios の全件実行が green にならない** (`PB-TR-23` が反復で 3/9 失敗)。同じテストは HEAD (本変更なし) でも 1/5 失敗し、本変更由来の回帰である証拠は得られなかったが、本変更は当のトランジション実行部を器から切り出しており、かつ「完了判定は絞り込みなしの全件実行」がプロジェクトの規約 (cross conventions テスト実行規約) であるため、赤いまま完了にはできない。この1点をブロッカーとし、他は Minor / Suggestion とする。

## 実行した検証

| ビルドルート | コマンド | 結果 |
|---|---|---|
| ios/ | `xcodebuild test -scheme KsDialogs -destination 'platform=iOS Simulator,name=iPhone 17'` | **204 tests / 36 suites — 1 issue** (`PB-TR-23`) |
| android/ | `./gradlew test --rerun-tasks` | 67 tests / 0 failures (`verifyNoDeclarativeUiDependency` 含む) |
| android/ (instrumented) | `ANDROID_SERIAL=<Pixel 4a> ./gradlew connectedDebugAndroidTest` | `:ksdialogs` 183 (skipped 1) + `:ksdialogs-compose` 35 = **218 / 0 failures** |
| kmp/ | `./gradlew allTests --rerun-tasks` | iosSimulatorArm64 42 + androidHostTest 38 = **80 / 0 failures** |
| maui/ | `dotnet test` | **105 / 0 failures** |
| maui/android/native/ | `./gradlew :ksdialogs-maui-bridge:test --rerun-tasks` | 15 / 0 failures |

- `python3 scripts/scenario-id-coverage.py` → 未網羅なし (141/150・除外 9)。`--require-mirror` → OK。`--selftest` → 全件 OK
- 新規の負の API 形状検査 6 本をすべて個別に実行し、期待どおりコンパイル失敗することを確認:
  - ios `KSDIALOGS_NEGATIVE_CHECK_LOADING_SHOW_STYLE` → `extra argument 'style' in call`
  - ios `KSDIALOGS_NEGATIVE_CHECK_LOADING_SHOW_OPTIONS` → `extra argument 'options' in call`
  - android `ksdialogs.negativeCheck.loadingShowStyle` / `.loadingShowOptions` → `No parameter with name '...' found.` (候補不適合の行を含め 2 件)
  - kmp `ksdialogs.negativeCheck.loadingStyleType` → `Unresolved reference 'LoadingStyle'.`
  - kmp `ksdialogs.negativeCheck.loadingStyleProperty` → `Unresolved reference 'style'.`
- `scripts/local-path-lint.py` / `scripts/identity-lint.py` / `scripts/comment-policy-lint.py` → いずれも 0 件

### 重点確認の依頼への回答 (巻き戻し→復元が起きた2ファイル)

`samples/ios/KsDialogsSample/SampleMenuModel.swift` と `samples/android/app/src/main/kotlin/jp/kamusoft/ksdialogs/samples/android/MainActivity.kt` は、**どちらも欠落なし**と判断した。

- 導線: iOS は `SampleMenuScreen.swift` に `defaultLoadingItem` / `customLoadingItem` の2行 + 区切りが入り `runDefaultLoading()` / `runCustomLoading()` を呼ぶ。Android は `SampleMenuView.kt` に同じ並び (Model Dialog の後ろに2件) で `onDefaultLoadingSelected` / `onCustomLoadingSelected` が配線されている
- 登録: iOS `KsDialogsSampleApp.swift` / Android `SampleApplication.kt` の双方に `SampleLoadingRegistration.register()` が入っている
- 文言: 両ルートの `SampleText` に `Default Loading` / `Loading...` / `Soon...` / `Custom Loading` / `カスタムローディング` / `結果: 完了` / `progressPercentage` がすべて揃い、samples デルタスペックの文言指定と一致する
- 中身: 進捗の刻み (`0..4`)・メッセージ差し替え (`step == 2` で `setMessage`)・完了後の結果表示が両ルートで同型。刻み定数もローカル定数として復元されている

## 指摘事項

### [🟠 Major] ios の全件実行が green にならない (`PB-TR-23` が不安定)

**該当箇所**: `ios/Tests/KsDialogsTests/DialogTransitionTests.swift:531`

**問題点**:
全件実行 (`xcodebuild test`) が `204 tests / 1 issue` で失敗する。失敗するのは `[PB-TR-23] 提示中の OS 発器消失ではフックをキャンセルして cancelled を配送する` の

```swift
#expect(probe.hasEvent(.cancelled(.presentation)))
```

で、直前の `#expect(try await stage.task.value == .cancelled)` は通っている。結果の配送が済んだ時点でフック側の `CancellationError` 記録がまだ入っていない競合に見える (`.started(.presentation)` は `waitUntil` で待っているのに、`.cancelled` は待ちなしで即時に観察している)。

切り分けの実測:

| 対象 | 単体反復の結果 |
|---|---|
| 本変更の作業ツリー | 9 回中 3 回失敗 |
| HEAD (本変更なしの一時 worktree) | 5 回中 1 回失敗 |

失敗率に有意差はなく、**本変更が持ち込んだ回帰である証拠は得られなかった** (既存の不安定さと判断する)。ただし本変更は task 1.1 でこのテストが見ているトランジション実行部そのものを器から `DialogTransitionRunner` へ切り出しており、経路が無関係とは言い切れない。加えて cross conventions のテスト実行規約は「完了判定には絞り込みなしの全件実行を使う」「結果を報告するときは実行件数を併記する」と定めているため、全件実行が赤いままアーカイブへは進めない。

**推奨修正**: 次のいずれかを選び、選んだ方を記録に残すこと。

- (a) 本変更内でテストのみの付随修正として直す — `#expect(probe.hasEvent(.cancelled(.presentation)))` を `await DialogTestWaiting.waitUntil { probe.hasEvent(.cancelled(.presentation)) }` 相当の待ちに変える (Loading 側の新規テストが同種の観察で使っている待ち方と揃う)。テスト専用の変更で本体挙動に触れないため、付随修正として `deviation.md` に1行足せば足りる
- (b) 本変更のスコープ外として別 change に切り出し、`deviation.md` に「既存の不安定テスト。HEAD でも再現。別 change で対処」と経緯を残したうえで、本変更の完了判定からは外す

いずれにせよ、**「全件実行が green」を無記録のまま飛ばさない**ことが要件。

---

### [🟡 Minor] MAUI 互換面 (Kotlin / Swift bridge) の Loading 経路に自動テストが1本もない

**該当箇所**:
- `maui/android/native/ksdialogs-maui-bridge/src/main/kotlin/jp/kamusoft/ksdialogs/maui/MauiLoadingStyle.kt` (`toLoadingStyle()`)
- `maui/macios/native/KsDialogsMauiBridge/MauiLoadingStyle.swift` (`LoadingStyle.init(_:)`)
- `maui/android/native/ksdialogs-maui-bridge/src/test/kotlin/jp/kamusoft/ksdialogs/maui/` (Loading 用のテストが無い)

**問題点**:
maui-binding デルタスペックの `[LD-MA-03]` は「placement の各値・スタイルの各値・進捗値が無変換で **Native 側に到達する**」を求めている。実装された `maui/KsDialogs.Maui.Tests/LoadingPassthroughTests.cs` は `TestLoadingGateway` を的にしており、検証できているのは **C# facade → `ILoadingGateway` の区間まで**である。その先の `PlatformLoadingGateway.ToBridgeStyle` → `MauiLoadingStyle` → Native `LoadingStyle` という写しの区間は、両 OS とも自動テストが無い。

Dialog では同じ区間に Kotlin 側のテストがある (`MauiDialogLayoutPassthroughTests` の「指定した静的メタ属性がそのまま `DialogOptions` になる」ほか計 15 本) ため、Loading だけが片側だけの担保になっている。

さらに悪いことに、この区間の取り違え (例: `indicatorColorArgb` と `messageColorArgb` の入れ替え) は **samples の手動通しでも見えない** — 4ルートの Sample はどこでも `LoadingStyle` を設定しておらず、既定値が両方とも白のため見た目に差が出ない。つまり現状、この写しは「読んで確かめた」以上の担保を持たない。

なお、写しの内容自体は読んだ限り正しい (今この瞬間の欠陥ではなく、回帰を捕まえる網が無いという指摘)。

**推奨修正**: `maui/android/native/ksdialogs-maui-bridge/src/test/kotlin/.../MauiLoadingPassthroughTests.kt` (仮) を足し、`MauiLoadingStyle` → `LoadingStyle` と `MauiDialogPlacement` → `DialogPlacement` の値保存、および `MauiLoadingCompletionListener` がちょうど1回呼ばれること (`MauiDialogClosureReportTests` の Loading 版) を見る。Swift 側に同型のものを置けない事情があるなら、その旨を記録に残す。

---

### [🟡 Minor] Android の回転再取り付けで、破棄済み Activity の Context を掴んだ View を持ち越す

**該当箇所**: `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/LoadingCoordinator.kt` (`onHostChanged` → `attach(host, playsPresentation = false)`)

**問題点**:
`onHostChanged` は器 (`LoadingContainer`) だけを作り直し、`contentView` は再生成せずに新しい Activity の Window へ載せ替える。中身の View が握っている `Context` は**破棄された前の Activity**のままなので、

- ローディングが閉じるまで破棄済み Activity が到達可能になる (LeakCanary が拾う型のリーク。長さは表示の寿命に限られ、蓄積はしない)
- 中身が構成修飾つきリソース (`values-land/` など) を読む場合、旧構成の値のままになる

中身を作り直さないこと自体は意図された設計で、`LD-WN-01` の instrumented テストが `assertSame("中身は作り直されない", ...)` で固定しているため、仕様の逸脱ではない。ただしライブラリが利用者アプリに持ち込む副作用としては記録に値する。

**推奨修正**: 少なくとも `LoadingCoordinator.onHostChanged` のコメントに「中身は前の画面の Context を保持したまま載せ替える」というトレードオフを明記する。作り直しへ倒す判断をするなら、coordinator が既に `contentRequest` / `contentFactory` / 固定済み実効値を保持しているので再生成は可能だが、`DialogLayoutSnapshot` の固定値の引き継ぎ方を含めて設計判断が要る (その場合は本変更のスコープ外)。

---

### [🟡 Minor] `waitForPendingDismissal` のコメントが実際の条件と食い違う

**該当箇所**: `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/LoadingCoordinator.kt` (`waitForPendingDismissal`)

**問題点**:

```kotlin
val pending = dismissalJob ?: return
pending.join()
// 後始末まで済んだ撤去はここで手放す。控えが残ったままなら次の撤去が始まっている
if (dismissalJob === pending) {
    return
}
```

コメントは (1)「ここで手放す」と言うが、この区間では何も手放していない (手放すのは `completeDismissal`)、(2)「控えが残ったままなら次の撤去が始まっている」と言うが、`dismissalJob === pending` は**同じ撤去の控えが残っている**状態であり、新しい撤去なら別のインスタンスになるので条件が逆に読める。実際の役割は「後始末が走らずに join が解けた場合に無限ループへ落ちないための脱出」で、KDoc の「別の撤去が始まっていたらもう一度待つ」を実現しているのは条件が **false** の側 (ループ継続) である。

`ksn-core` の観点「コメントが単独で理解できるか」に触れる。iOS 側の同名メソッドは単純な `while let` で済ませており、Android だけこの分岐が要る理由も読み取れない。

**推奨修正**: コメントを実際の条件に合わせる。例:

```kotlin
// 後始末 (completeDismissal) が済んでいれば控えは null になっているので、次の周回で抜ける。
// 別の撤去が始まっていれば控えが別の Job に変わるので、その完了まで待ち直す。
// 控えが同じ Job のまま残っているのは後始末を経ずに join が解けた場合で、無限ループを避けて抜ける
```

---

### [🔵 Suggestion] MAUI の設定プロパティは、保持値の更新と Native への反映が同一ロック下にない

**該当箇所**: `maui/KsDialogs.Maui/Internals/LoadingSettings.cs` (`SetStyle` / `SetOptions`)

**問題点**: 保持値の差し替えは `lock (_gate)` の中、`gateway.ApplyStyle(style)` はロックの外で行っている。2 スレッドが同時に設定すると「C# 側が読み返す値」と「Native に最後に届いた値」が食い違い得る (A→B の順で保持され、B→A の順で Native へ届く)。設定プロパティは実用上 startup で1回書くだけなので実害は考えにくく、ロック内で外部呼び出しを行う設計にも別のリスクがあるため、判断はどちらでもよい。

**推奨修正**: 現状のままとするなら、「Native への反映順は保持値の順と一致しない可能性がある (設定は起動時の一括設定を想定)」を XML doc に一言残す。

---

### [🔵 Suggestion] Sample のカスタム Loading の進捗描画に、幅が決まらない場合の再帰 post がある

**該当箇所**: `samples/android/app/src/main/kotlin/jp/kamusoft/ksdialogs/samples/android/CustomLoadingCardView.kt` (`render`)

**問題点**: `progressTrack.width == 0` のとき `progressTrack.post { render(progress) }` で自分を再投函する。帯の幅が最後まで 0 のままになる経路 (0 幅の器に載った等) では、表示が閉じるまで毎フレーム再投函が続く。Sample なので実害は小さいが、利用者が写す参考実装であることを踏まえると打ち切りが要る。

**推奨修正**: `doOnLayout` / `addOnLayoutChangeListener` で1回だけ待つ形に置き換えるか、再投函の回数に上限を設ける。

---

### [🔵 Suggestion] `samples/maui/README.md` の追記が tasks / deviation のどこにも紐づいていない

**該当箇所**: `samples/maui/README.md` (Fast Deployment 残骸による `InvalidCastException` の注記)

**問題点**: 検証中に得た有用な知見だが、tasks.md のどのタスクにも属さず `deviation.md` にも記録が無い。ドキュメントへの追記なので仕様逸脱ではないものの、蒸留時に「どの作業で入ったのか」が追えなくなる。

**推奨修正**: `deviation.md` に `[付随修正]` として1行足す (検証手順の実測知見であること・Sample の挙動には影響しないこと)。

---

### [🔵 Suggestion] concepts の「テスト実行規約」が本変更で全面的に陳腐化する (蒸留での更新対象)

**該当箇所**: `kasane/concepts/cross/conventions/test-execution.md`

**問題点**: 同文書は「テスト構成が育って実態が変わったら本規約を実測で更新する」と自ら定めている。本変更で実測値がすべて動いた:

| ルート | 規約の記載 (2026-08-25) | 本レビューの実測 |
|---|---|---|
| ios/ | 158 tests / 28 suites | 204 tests / 36 suites |
| android/ | 66 tests | 67 tests |
| android/ (instrumented) | 139 (105 + 34) | 218 (183 + 35) |
| kmp/ | 57 (31 + 26) | 80 (42 + 38) |
| maui/ | 88 tests | 105 tests |
| 負の検査 | 31 本 | 37 本 (Loading 6 本追加) |

**推奨修正**: 実装側で今すぐ直す必要はない (concepts の追随は `ksn-distill` の責務)。蒸留で取りこぼさないよう、件数表と負の検査のフラグ表 (ios 2 / android 2 / kmp 2 の追加分) の更新を申し送りとして残すこと。

## アクションプラン

1. **[Major]** `PB-TR-23` の扱いを決める — 本変更内でテストのみの付随修正として直す (推奨) か、別 change へ切り出して `deviation.md` に経緯を残すか。どちらでも、全件実行が green である状態を作ってから完了とすること
2. **[Minor]** MAUI 互換面の Loading 経路にテストを足す (`MauiLoadingStyle` → `LoadingStyle` の値保存、完了通知ちょうど1回)。Dialog 側の 15 本と対になる担保
3. **[Minor]** `LoadingCoordinator.waitForPendingDismissal` のコメントを実際の条件に合わせる
4. **[Minor]** Android の回転再取り付けで前の画面の Context を持ち越すトレードオフを `onHostChanged` に明記する
5. **[Suggestion]** `deviation.md` に `samples/maui/README.md` の追記を1行足す
6. **[Suggestion]** 蒸留への申し送りとして `test-execution.md` の件数・負の検査フラグの更新を残す
7. **[Suggestion]** MAUI `LoadingSettings` の反映順の注記 / Sample の `render` 再帰 post — 余力があれば

## 確認した観点 (指摘に至らなかったもの)

- **足場アーティファクトの改変**: `specs/` 6ファイル・`proposal.md` / `design.md` は未変更 (`git status` で `tasks.md` と `ui/brief.md` のみ変更。どちらも実装中に書き足してよい層)
- **tasks.md の虚偽チェック**: 7 グループ 25 項目すべてについて対応する実装・テスト・証跡を確認し、未実装のチェックは無かった
- **合意済み差分**: `deviation.md` の 4 件 (LD-CO-11 は進捗のみ遮断 / `ResumedActivityTracker` の購読追加 / maui `PlatformDialogContent` 切り出し / C# `DialogOptions` の public 化) と、`ui/brief.md` の合意済み差分 (行間 8・bold・カード影なし・OS 標準インジケータの寸法差) はいずれも実装と一致しており、違反として扱っていない。`DialogOptions` の public 化では interop 詳細の `OverlayColorArgb` が `internal` に落とされており、公開面の広がりが最小に抑えられている点も確認した
- **切り出しの挙動不変性**: iOS `DialogContainerViewController` / Android `DialogContainer` / maui `PlatformDialogGateway` の3件とも、移送先のコードが移送前と同一のロジック・同一の実行順序 (中身の可視化とフック呼び出しを同じ同期区間に置く不変条件を含む) を保っていることを diff で照合した。回帰ガードとしての既存テストも全通過
- **合流・世代の競合**: 撤去中の新規開始 (`waitForPendingDismissal`)・撤去タスクの控えと開始の順序 (iOS は MainActor の実行順、Android は `CoroutineStart.LAZY`)・旧世代トークンの締め出しを読み、いずれも二重表示や閉じ残りに落ちる経路を見つけられなかった
- **fail-fast の一貫性**: 未登録 VM は 4 形態すべてで `beginUse` / `LoadingPresenter` / `IosLoadingGateway` の開始時点で弾かれ、action は実行されない (`LD-CV-04`)。合流時も同じ検証を通す (`validateContentRequest` / `resolveFactory`)。KMP 経路は `LoadingContentRequest.kmp` が先に解決するため `.inline` を使っても fail-fast が抜けない
- **進捗の直列化**: 報告口は 4 形態とも任意スレッドから呼べ、受理は UI スレッドへ移してから世代照合・クランプ・非有限無視を行う。VM の受け口とフォーマット関数の呼び出しも UI スレッド上
- **設定の採用時点**: `style` / `options` は各表示の開始時に読まれ (`beginUse` の新世代分岐 / `makeContent`)、合流や表示中の変更では読み直さない (`LD-ST-01` / `LD-AT-05` と一致)
- **証跡の妥当性**: `verification/loading-front/` の 8 枚と `verification/sample-walkthrough/` の 43 枚を数枚開いて内容を確認し、notes.md の主張 (Loading が最前面・タップが遮られる・4ルートで文言一致) と画像が食い違わないことを確かめた。一時コードでの撮影と原本復元の経緯も notes.md に明記されている
- **セキュリティ / 機微情報**: 新規コードに認証・ネットワーク・永続化の面は無い。3 種の lint (ローカル絶対パス・個体/個人/秘密・コメント規約) はすべて 0 件
- **`second-opinion-code-001.md`**: 独立性を保つため本レビューでは読んでいない (突き合わせは呼び出し元の責務)
