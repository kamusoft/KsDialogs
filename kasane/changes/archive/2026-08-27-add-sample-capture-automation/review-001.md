# レビュー結果: add-sample-capture-automation (001 回目)

**日付**: 2026-08-27
**判定**: CHANGES_REQUESTED

## サマリー

samples デルタスペック (CA-SA-01〜07) の要求は4ルート6アプリで実装されており、キー名・安定デモ ID 9件・異常系の倒れ方・one-shot 消費はコードを突き合わせた限り一致している。引数なし起動の不変も、受け口が null を返すだけの構造とメニュー系コードの差分の性質から成立している。6アプリすべてのビルドと全 lint (scenario-id-coverage / local-path / identity / comment-policy) が通ることを実行して確認した。

一方で、永続層の ADR cross/0010 が本 change で取り下げた方式 (撮影スクリプト2本構成・config.yaml をポインタへ縮約) を宣言したまま残っており、実装と真正面から矛盾する。加えて MAUI の自動再生だけが失敗を無言で捨てる経路になっており、撮影証跡の信頼性という本 change の目的に直接効く。証跡の静止画の置き場も ksn-core のホワイトリスト外にある。

## 確認した観点と実行結果

- **ビルド**: samples/android (`assembleDebug` BUILD SUCCESSFUL) / samples/kmp (`assembleDebug` + `:shared:linkDebugFrameworkIosSimulatorArm64` BUILD SUCCESSFUL) / samples/ios (`xcodebuild` BUILD SUCCEEDED) / samples/kmp/iosApp (BUILD SUCCEEDED) / samples/maui `net10.0-android` (0 警告 0 エラー) / samples/maui `net10.0-ios` (0 警告 0 エラー。既定 Xcode 26.5 では .NET for iOS 26.1 の版数制約で失敗するため `DEVELOPER_DIR=/Applications/Xcode-26.1.1.app/Contents/Developer` で実行 — 本 change と無関係の環境制約)
- **テスト**: 本 change はライブラリ本体に触れておらず、samples 配下に単体テストは存在しない (`samples/kmp/shared/src` は `commonMain` のみ)。`SamplePresenter` の primary constructor は既定引数の追加のみで既存呼び出しを壊さない
- **lint**: `scripts/scenario-id-coverage.py` (141/157・除外16件・未網羅なし) / `scripts/local-path-lint.py` / `scripts/identity-lint.py` / `scripts/comment-policy-lint.py` すべて exit 0
- **足場**: `specs/samples/spec.md` は未改変 (契約は凍結されたまま)。`specs/capture-tooling/spec.md` の削除と proposal / design / tasks の改訂はオーナー合意済みの取り下げに伴うもの
- **tasks.md**: `[x]` の 1.1〜1.5 / 4.1〜4.3 / 5.1 / 5.3 はいずれも実体を伴っており、虚偽チェックは見当たらない
- **証跡**: `verification/four-route-walkthrough/` の画像を実見し、MAUI iOS の9デモ自動再生・Android の回転で再発火しないことを確認 (証跡の内容そのものは spec の記述と整合)
- **deviation.md**: 2件とも記録済みの合意差分として扱い、違反としては指摘していない

## 指摘事項

### [🟠 Major] ADR cross/0010 が取り下げ済みの方式を宣言したまま残っている

**該当箇所**: `kasane/decisions/cross/0010-sample-capture-automation-two-part.md:3`, `:16`, `:22`, `:25` / `proposal.md:12`

**問題点**: 本 change は 2026-08-27 の改訂で capture-tooling (入口スクリプト) を取り下げ、config.yaml `ui.screenshot` も「ポインタへ縮約」ではなく手順の書き直しへ方針転換した。しかし永続層の ADR cross/0010 は、

- タイトル「…『デモ駆動モード + 単一入口スクリプト』の2本構成とする」
- Decision 2「**撮影スクリプト**: `scripts/capture/` に単一の入口スクリプトを置き…」
- 「config.yaml `ui.screenshot` は手順の書き写しをやめ『入口スクリプトの docstring が正』へのポインタ + …のみに縮約」

をそのまま宣言している。実装した config.yaml は逆に手順の書き写しを厚くしており、ADR の記述と実装が真逆になっている。ADR は status: proposed だが `kasane/decisions/` (長命層) の住人であり、レビュー観点「既存 ADR の決定に反していないか」に抵触する。さらに、この矛盾を蒸留時に拾うための記録が change 側のどこにも無い (deviation.md にも proposal の Impact にも無い) ため、ksn-distill が気づかなければそのまま accepted へ昇格しかねない。

**推奨修正**: 次のいずれか。(a) ADR cross/0010 を「デモ駆動モード単独」の決定へ改訂し、スクリプト部分は Alternatives / Consequences へ移して取り下げ理由 (iOS シミュレータへの座標タップ注入 CLI が環境に無い) を残す。(b) 本 change 内では ADR を触らず、`deviation.md` または `proposal.md` の Impact に「ADR cross/0010 は本 change の取り下げを反映した改訂が必要 (蒸留時)」を明示的に記録する。どちらを採るかはオーナー判断だが、**記録が一切無い現状は選べない**。

### [🟡 Minor] MAUI の自動再生だけが失敗を無言で捨てる

**該当箇所**: `samples/maui/KsDialogs.Sample.Maui/SampleMenuPage.xaml.cs:43`

```csharp
Dispatcher.Dispatch(() => _ = PlayAsync(demo));
```

**問題点**: `PlayAsync` の返す Task を破棄しているため、自動再生の途中で例外が出ても誰も観測せず、**画面には何も起きないまま通常のメニューが残る**。同じデモを手動タップした場合は `async void` ハンドラ経由で例外が同期コンテキストへ再送出されてアプリが落ちるので、失敗の見え方が自動再生と手動で正反対になる。他3ルートは失敗が表面化する — android native / kmp-android は `scope.launch` の未捕捉例外がそのままクラッシュへ、ios / kmp-ios は `assertionFailure` で止まる。

本 change の目的は撮影証跡の収集であり、無言の no-op は「定義外 ID を渡したときの正常な結果」と見分けがつかない画像を生む。証跡が誤って成立してしまう型なので、4ルートで最も避けたい壊れ方がここだけ残っている。

**推奨修正**: `Dispatcher.Dispatch(async () => await PlayAsync(demo))` にして例外を Dispatcher へ再送出させる (手動タップと同じ倒れ方に揃う)。あるいは `PlayAsync` を try/catch で包み、他ルートの `assertionFailure` に相当する明示的な失敗 (`Debug.Fail` 等) を出す。

### [🟡 Minor] 動作証跡の静止画が規約外の置き場にある

**該当箇所**: `verification/four-route-walkthrough/*.png` (20 件)

**問題点**: ksn-core `references/ui-artifacts.md` の媒体ホワイトリストでは、実機・シミュレータの動作証跡の静止画を置いてよいのは `changes/<id>/evidence/` だけで、change 直下の `verification/` は定義されていない (`ui/verification/` は視覚照合ループ用で、本 change には `ui/` が無い)。同じ違反は add-model-binding-di の蒸留で「規約外の verification/ を evidence/kmp-model-binding/ へ移設のうえ archive」として一度是正された前例がある (`kasane/concepts/log.md:20`)。放置すると `distill.archive-media` の削除対象から外れて archive に媒体が残る。

**推奨修正**: 画像を `evidence/four-route-walkthrough/` へ移し、`notes.md` / `parity-check.md` からの参照パスを更新する (テキストの置き場も `evidence/` へ揃えるのが素直)。

### [🟡 Minor] design.md に取り下げ前提の記述が残っている

**該当箇所**: `design.md:9` (Goals) / `design.md:40`〜`:48` (Decision 2) / `design.md:104` (Migration Plan) / `design.md:11` (Non-Goals)

**問題点**: Decision 4・5 には「取り下げ (2026-08-27 改訂)」の改訂注記が入ったのに、同じく入口スクリプト前提の記述が注記なしで残っている:

- Goals の「**部分成功を成功扱いしない撮影契約**」— 失効した Decision 4 の Goal
- Decision 2 の見出し「**スクリプトは**毎回コールド起動し」と本文「**入口スクリプトは**起動前に対象アプリを必ず終了してから起動する」— コールド起動の契約自体は生きているが、担い手はスクリプトではなくエージェント手順 (config.yaml `ui.screenshot`) に変わっている
- Migration Plan の「ロールバックはデモ駆動モードの受け口と **`scripts/capture/` の削除**で完結する」— 存在しないディレクトリ
- Non-Goals の「scenario-id-coverage の Python 対応拡張 (…)」— proposal.md 側では対応する Non-Goal が削除済みで、片方だけ残っている

Decision 2 の内容 (毎回コールド起動 + one-shot 消費) は実装の正であり続けるため、**読み手がどれを信じるかで迷う**のがまずい。

**推奨修正**: Decision 2 に「担い手は入口スクリプトからエージェント手順へ変わったが、コールド起動を撮影側の契約とする判断は有効」の改訂注記を足し、Goals・Migration Plan・Non-Goals の失効箇所を改訂後の実体に合わせる。

### [🔵 Suggestion] kmp-android のディスパッチだけコンパイラが漏れを検出できない

**該当箇所**: `samples/kmp/androidApp/src/main/kotlin/jp/kamusoft/ksdialogs/samples/kmp/android/MainActivity.kt:73`

**問題点**: `play()` の `when` が `INLINE_DIALOG` / `TRANSITION_DIALOG` / `LAYOUT_DIALOG` の3件だけ明示し、残りを `else -> scope.launch { presenter.autoPlay(demo) }` で流している。共有 `SampleDemoId` に10件目が足されたとき、android native (網羅 `when`) はコンパイルエラーで気づけるのに、kmp-android は `presenter.autoPlay` が null を返して**無言で何も起きない**。sample-parity 規約では安定デモ ID は4ルート一致が要件なので、これは design Decision 3 の代替案 B が避けようとした「片方だけ直す事故」と同じ性質の穴になる。

**推奨修正**: `else` をやめ、Presenter が受け持つ6件を明示列挙して網羅 `when` にする (Swift 側は Kotlin enum が Swift へクラスとして出るため `default` が不可避で、ここは Kotlin 側でしか塞げない)。

### [🔵 Suggestion] 刻み間隔のローカル退避が MAUI では理由を失っている

**該当箇所**: `samples/maui/KsDialogs.Sample.Maui/SampleMenuPage.xaml.cs:188`, `:217` / `samples/ios/KsDialogsSample/SampleMenuModel.swift:132`, `:156`

**問題点**: iOS 側の `let stepIntervalMilliseconds = loadingStepIntervalMilliseconds` は、`@MainActor` 隔離された `SampleMenuModel` のプロパティを非隔離のクロージャから読めないための必要な退避だが、**なぜ必要かのコメントが無い**。MAUI 側の `int stepIntervalMilliseconds = _loadingStepIntervalMilliseconds;` は同じ形をしているが、C# では readonly インスタンスフィールドをラムダから直接捕捉できるため退避に意味がなく、形だけが写った状態になっている (kmp の `SamplePresenter` はフィールドを直接使っており、そちらが素直)。

**推奨修正**: iOS 側に退避の理由を 1 行で残し (comment-policy の「コメントが単独で理解できる」に沿う)、MAUI 側はローカルを削って `_loadingStepIntervalMilliseconds` を直接使う。

### [🔵 Suggestion] `Task.yield()` のコメントが手段の効果を言い過ぎている

**該当箇所**: `samples/ios/KsDialogsSample/SampleMenuScreen.swift:73` / `samples/kmp/iosApp/KsDialogsSampleKmp/SampleMenuScreen.swift:82`

**問題点**: 「初回のレイアウトが済む前に画面状態を変えると全画面表示の提示を取りこぼすため、1 巡譲ってから再生する」とあるが、`Task.yield()` が保証するのは MainActor のターンを1回譲ることだけで、レイアウトの完了は保証しない。証跡 (6アプリ × 9デモ) では安定しているので実害の報告は無いが、コメントが根拠として提示している因果と実際の手段がずれているため、将来「足りないから2回にする」「レイアウト完了を待つ API に置き換える」のどちらの判断もできない。

**推奨修正**: コメントを実測ベースの表現へ (「初回表示と同じターンで画面状態を変えると提示を取りこぼしたため、1ターン譲ってから再生する — 6アプリ × 9デモで安定を確認」)、あるいはレイアウト完了を観測できる待ちへ置き換える。

### [🔵 Suggestion] deviation.md の1件目が「オーナー確認待ち」のまま

**該当箇所**: `deviation.md:3`

**問題点**: 指摘ではなく申し送り。spec の「同じキーが複数回現れた場合は最初の1組を採用する (SHALL)」は Android の Intent extra では観測不能で (adb / Bundle 段階で1値に畳まれる)、実装は妥当だが、記録は「暫定記録 — オーナー確認待ち」の状態にある。この SHALL は両 OS 共通で書かれているため、**spec 側の記述が実現不能**という足場の不備でもある。合意済み差分として扱う前提でレビューしたので違反としては挙げていないが、アーカイブ前にオーナー確認 (と、必要なら次の change での spec 文言の訂正) が要る。

**推奨修正**: オーナー確認を取り「暫定記録」を外す。CA-SA-06 (同じ引数 → 同じ挙動) の例外として明記しておくと、後続の drift で拾い直さずに済む。

## アクションプラン

1. **ADR cross/0010 の矛盾の始末を決める** (Major)。ADR 改訂か、change 側への蒸留申し送りの記録か、どちらかを実施する
2. **MAUI 自動再生の失敗を表面化させる** (Minor・優先)。撮影証跡の信頼性に直結する
3. **証跡画像を `evidence/` へ移設**し、`notes.md` / `parity-check.md` の参照を更新する (Minor)
4. **design.md の失効記述に改訂注記を入れる** (Minor)
5. kmp-android の `when` を網羅化、MAUI のローカル退避削除と iOS の理由コメント追加、`Task.yield()` のコメント修正 (Suggestion・まとめて実施可)
6. deviation.md 1件目のオーナー確認 (アーカイブ前の申し送り)
