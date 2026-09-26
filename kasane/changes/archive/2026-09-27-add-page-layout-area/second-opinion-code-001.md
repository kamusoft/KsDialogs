# セカンドオピニオン: add-page-layout-area (code-001)
**相方**: codex / **label**: so-code-add-page-layout-area / **日付**: 2026-09-26 / **対象**: HEAD (c4936a8) からの作業ツリー差分と未追跡ファイル
---
# 独立コードレビュー: add-page-layout-area

**判定: CHANGES_REQUESTED** — Major 2件。`HEAD` からの作業ツリー差分と未追跡ファイルを静的に確認しました。ビルド・テストは依頼どおり実行せず、提示された実行結果を前提としました。`deviation.md` の合意済み差分は指摘に含めていません。

### 🟠 Major: MAUI で空矩形の上書き provider が既定ページへフォールバックしない

**該当箇所**: [maui/KsDialogs.Maui/Internals/DialogCurrentPageLocator.cs:150](maui/KsDialogs.Maui/Internals/DialogCurrentPageLocator.cs:150)

**問題点**: 上書き provider の `PlatformView` が取得でき、提示先ウィンドウに属していれば、幅・高さや safe area が空でも採用します。その後 Native 側が空矩形を棄却しても、MAUI の既定ページへは戻れません。Android では可視領域へ落ち、iOS では UIKit の既定探索へ進むため、仕様の「空の矩形なら次の取得元へ進む」と異なります。

**推奨修正**: MAUI の取得元選択中に platform view の有効矩形を検証し、空なら MAUI の既定ページを試してください。描画済みだが空の要素を返す回帰テストを加えてください。このテストは既定ページが選ばれなければ失敗する形にできます。

### 🟠 Major: 再問い合わせで未解決になると古いページ領域を使い続ける

**該当箇所**: [ios/Sources/KsDialogs/Layout/DialogLayoutApplier.swift:180](ios/Sources/KsDialogs/Layout/DialogLayoutApplier.swift:180)、[android/ksdialogs-core/src/main/kotlin/jp/kamusoft/ksdialogs/DialogLayoutHost.kt:223](android/ksdialogs-core/src/main/kotlin/jp/kamusoft/ksdialogs/DialogLayoutHost.kt:223)

**問題点**: 窓の変化後に行う2回目の問い合わせが未解決になると、両実装とも `nil` を無視して直前のページ inset を保持します。例えば provider が初回に有効な View、次回に `nil` を返す場合、次の窓変化まで古い位置が残ります。仕様は、全取得元が未解決なら可視領域へのフォールバックを要求しています。

**推奨修正**: 再問い合わせの未解決も配置結果として反映し、可視領域で再配置してください。連続する問い合わせで「有効 → 未解決」を返す provider のテストなら、古い配置が残る現状と修正後を区別できます。

**照合した主な規約**: `comment-policy.md`、`test-execution.md`、`sample-parity.md`、`diagnostic-message-language.md`、`ci-flaky-test-policy.md`、`runtime-behavior-verification.md`。既知の Android instrumented テストの間欠失敗は、提示された変更前の再現情報を踏まえ、本差分への指摘として数えていません。


## 突き合わせ結果

ホスト側: review-001.md (APPROVED、Minor 2 / Suggestion 3) / verify-001.md (VALID)。

| 指摘 | 出典 | 採否 | 根拠 |
|---|---|---|---|
| MAUI で空矩形の上書き provider が既定ページへフォールバックしない | 相方のみ (Major) | **採用** | `DialogCurrentPageLocator.ToHostedPlatformView` は platform view の有無と提示先ウィンドウだけを見て矩形の空を確かめない。Native が空矩形を棄却すると MAUI の既定ページを経ずに可視領域 (iOS は UIKit の VC 走査) へ進み、dialog-contract「空の矩形を返すときは次の取得元へ進む」に反する。同じ仕組み (MAUI 層の判定が Native の棄却条件より緩い) で、Android の KsDialogs 自身の器の中の要素を返した場合も既定ページを経ない — 修正に含める |
| 再問い合わせで未解決になると古いページ領域を使い続ける | 相方のみ (Major) | **降格** | 各きっかけの 1 回目の問い合わせは未解決なら可視領域へ落ちる (iOS `DialogLayoutApplier` / Android `DialogLayoutHost` とも)。直前の結果を保つのは同じきっかけに対して次の番に追加する 2 回目 (ページ側のレイアウト遅れの補正) だけで、そのきっかけの結果は 1 回目で spec どおり決まる。2 回目の未解決まで可視領域に落とすと回転中の一時的な null でちらつくおそれがある。起き得るのは窓変化から 1 拍の間にページが消える場合のみで実害シナリオが弱い。修正サイクルは回さない |
| 実配置テストホストが slnx / CI / handbook に無い | ホストのみ (Minor) | 確定・蒸留へ | evidence/distill-handoff.md の handbook 追記候補に記載済み |
| instrumented の落ち着き待ちが共通の待ち (`InstrumentedStateSettling.awaitSettled`) を使わない | ホストのみ (Minor) | 確定・修正 | handbook cross/ci-flaky-test-policy.md。2 箇所で局所的 |
| Suggestion 3 件 (オプトイン強制の負の検査 / Toast gateway の EnsureInstalled / material3 の version catalog) | ホストのみ | 見送り | Toast は Non-Goals、負の検査と catalog 化は本 change の要件外 |

未解決: なし。
