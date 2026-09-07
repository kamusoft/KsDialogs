# セカンドオピニオン: add-presentation-behavior (code-004)
**相方**: codex (新セッション `so-code-add-presentation-behavior-r4`、器 ksn-reviewer) / **日付**: 2026-08-22 / **対象**: オーナー差し戻し (iOS 入場ちらつき) の修正。入力はホスト側 review-004 と同一
---
# レビュー結果: add-presentation-behavior（4回目）

**日付**: 2026-08-22  
**判定**: **CHANGES_REQUESTED**

## サマリー

`alpha` 復帰から presentation フックの最初の同期処理までを直接 `await` でつなぐ方針自体は妥当です。Swift では async 呼び出し先も同じ Task の一部として実行され、実際の suspension point までは現在の job が継続するため、今回確認された Slide / Zoom のちらつきは解消できています。[SE-0304](https://github.com/swiftlang/swift-evolution/blob/main/proposals/0304-structured-concurrency.md)

一方、`.presenting` への遷移だけが presentation 開始タスクより先行しており、その隙間の閉鎖を誤った状態として扱う問題が残っています。Major 1件、Minor 2件です。

## 指摘事項

### [🟠 Major] presentation が実際に始まる前から `.presenting` と判定される

**該当箇所**: `ios/Sources/KsDialogs/Presentation/DialogContainerViewController.swift:410`

**問題点**: `containerState = .presenting` は同期的に実行されますが、オーバーレイ・中身の表示・presentation フックは、411行目で作った Task が後でスケジュールされるまで始まりません。

この隙間で閉鎖信号が来ると、見た目はまだ attached のままなのに presenting の規則が適用されます。

- 通常の報告は `handleSettled()` が何もせず、不要な presentation を完走してから dismissal する。
- 呼び出し元キャンセルは `beginDismissal()` に入り、presentation フックが一度も始まっていないのに dismissal フックを実行する。
- 415行目のガードは presentation 側の表示を防ぎますが、既に起動された dismissal は防ぎません。

これは design Decision 10 の「`.presenting` への遷移と、覆い・表示・フック開始が同時」、および提示開始前の閉鎖は両フックを実行しないという規則に反します。

**推奨修正**: Task が実際に開始するまで状態を `.attached` に保ち、Task の最初の同期区間で次を順に行ってください。

```swift
guard !Task.isCancelled,
      containerState == .attached,
      !resultChannel.isResultSettled
else {
    finishRemoval()
    return
}

containerState = .presenting
await runPresentationPhase()
```

あわせて、`layoutInWindow` から戻った直後、まだ一度も `await` していない地点で `handleCallerCancellation()` を呼び、両フックが実行されないことを固定するテストを追加してください。これならタイミング待ちなしで問題の隙間を再現できます。

### [🟡 Minor] 覆いの開始は「同時」ではなく、別 child task のスケジュールに依存している

**該当箇所**: `ios/Sources/KsDialogs/Presentation/DialogContainerViewController.swift:434`

**問題点**: `async let` の initializer は別 child task として実行されるため、MainActor 上の `runOverlayFade` は親 Task が suspension point に達するまで開始できません。[SE-0317](https://github.com/swiftlang/swift-evolution/blob/main/proposals/0317-async-let.md)

Slide / Zoom は suspension 前に画面外位置や透明状態を設定するので、確認済み録画では実害がありません。しかし、次のような同期完了フックでは、親が `_ = await overlay` で譲った後まで覆いが未開始です。

- `none`
- duration 0・負値・非有限値
- suspension しないカスタムフック

MainActor の job は投入順の FIFO が保証されないため、コンテンツだけが先に描画されないことを構造的には保証できません。[SE-0306](https://github.com/swiftlang/swift-evolution/blob/main/proposals/0306-actors.md)

**推奨修正**: 覆いについては `UIViewPropertyAnimator` の生成・`startAnimation()` を親 Task 内で同期的に行い、完了待ちだけを非同期ハンドルとして保持してください。その後に alpha 復帰とフックの直接 `await` を行い、最後に覆いの完了を待つ形なら、開始順と並走の両方を保証できます。

少なくとも `none` と同期完了カスタムフックについて、最初の描画フレームで覆いが開始済みであることを追加確認すべきです。

### [🟡 Minor] 回帰テストが保証されない MainActor の FIFO 順を判定根拠にしている

**該当箇所**: `ios/Tests/KsDialogsTests/DialogTransitionTests.swift:109`

**問題点**: lifecycle Task と `didRunLaterWork` を立てる Task は独立した MainActor job です。Swift の actor executor は投入順での実行を保証しません。

そのため、このテストは以下の両方が起こり得ます。

- 正しい実装でも目印 Task が先行して false failure になる。
- alpha とフックの間に別 Task を復活させても、その Task が目印より先に選ばれて false success になる。

修正前コードへ戻したとき失敗したという negative check は有益ですが、検出力はその具体的な実装と現在のスケジューラ挙動に限定されます。

**推奨修正**: 現在のテストは smoke test として残しても構いませんが、主たる回帰保証にはしないでください。`CADisplayLink` 等で最初にカードが描画されたフレームを観察し、Slide なら transform が開始位置、Zoom なら縮小状態であることを確認する実描画テストへ置き換えるのが適切です。

## 指摘外の確認結果

- PB-TR-05 は、フック開始後の通常報告について従来どおり presentation 完走後に dismissal へ進みます。
- PB-TR-23 / 28 は lifecycle Task のキャンセルが直接 await 中のフックと `async let` child へ伝播するため、開始後の脱出経路は維持されています。
- PB-TR-19 の factory 内即報告と、既存 PB-TR-20 の created 状態でのキャンセルは維持されています。ただし Major 指摘の「attached と実際の開始の間」は未検査です。
- `DialogOutsideTapTests` 2本の待ち合わせ追加は検査を弱めていません。alpha 0 の attached 状態は意図的にヒットテスト対象外であり、テスト対象である「利用者が触れられる中身」まで進めてから同じ座標・hitTest・判別述語を検査しています。
- before / after の代表画像も確認し、Slide の最終位置フレーム消失と、Zoom の縮小開始状態を確認しました。
- ホスト実施の 134 tests / 25 suites / 0 failures、修正前 negative check、lint、MAUI bridge ビルド結果は有効な証跡として扱いました。依頼どおり再実行はしていません。

## アクションプラン

1. `.presenting` への遷移を lifecycle Task の開始時点へ移す。
2. Task 開始前キャンセル／報告の回帰テストを追加する。
3. 覆いを同期的に開始できる構造へ変更し、`none` と同期完了フックの初回フレームを確認する。
4. MainActor FIFO に依存する回帰テストを実描画ベースで補強する。


## 突き合わせ結果 (ホスト review-004.md との照合、2026-08-22)

| # | 指摘 | ホスト (review-004) | 相方 (codex) | 採否 | 根拠 |
|---|---|---|---|---|---|
| 1 | `containerState = .presenting` が lifecycle Task の開始前に同期で立ち、1 ターンの窓で来た閉鎖信号 (別スレッド発の報告 / 呼び出し元キャンセル) が presenting 規則で扱われる → 一度も表示されていない中身に dismissal フックが走る / 表示前に確定済みでも presentation を完走してから退出 (Decision 10「提示前の閉鎖は両フックとも実行しない」に反する) | 🟡 Minor | 🟠 Major | **確定 (Major)** | 双方一致 (重要度は高い方)。修正: `.presenting` への遷移を Task の最初の同期区間へ移し、`.attached` でなければ / 確定済み・取り消し済みなら演出なしで撤去。Task 開始前の呼び出し元キャンセルで両フック非実行を固定するテストを追加 |
| 2 | 覆いの開始が `async let` の child task 依存で、`none` / 同期完了フックでは最初の描画フレームで覆いが未開始になり得る | — (「覆いは初期 alpha 0 のため 1 ターン遅れは無害、PB-TR-21 維持」と評価) | 🟡 Minor | **降格** | 覆いは alpha 0 → 250ms フェードで、1 フレーム分 (約 4%) の遅れは視覚的に判別不能。構造的に揃えたい場合は後続 (覆いを `UIViewPropertyAnimator` で同期開始) — handoff に記録 |
| 3 | 回帰テストが MainActor の FIFO 順に依存 | — (5 連続実行で安定、直前の `alpha == 0` 期待が前提を見張るため空振り通過しない、フレーム級は実機 A/B が担保、と評価) | 🟡 Minor | **降格** | 実測で脆さ未確認。smoke test としての位置づけを handoff に記録 (実描画テストへの置き換えは後続候補) |
| 4 | 直接 `await` の同期実行に依存する成立条件 (フック型が `@MainActor`) をコメントと ADR に明文化 | 🔵 S-1 | — (同趣旨の背景説明あり) | 採用 (コメント 1 文) + 蒸留へ | 修正に同梱 |
| 5 | 回帰テストに Scenario ID が無いことの位置づけ | 🔵 S-2 | — | 蒸留へ | ADR 統合時に「ちらつき防止の固定は回帰テストが担う」を 1 行 |
| 6 | `waitUntilContentIsVisible` が「既定トランジションが中身を動かさない」ことに暗黙依存 | 🔵 S-3 | — | 採用 (`.shown` まで待つ) | 実行時間 +250ms × 2 本で済むため修正に同梱 |

- 判定: ホスト APPROVED (Minor 1) / 相方 CHANGES_REQUESTED (Major 1)。#1 を Major として修正サイクルへ。降格 2 件 (#2 #3)、採用 (ホストのみ) 2 件 (#4 #6)
- 収束シグナル: #1 は今回の修正 (alpha 復帰の移動) で新たに生じた窓で、同一指摘の 2 周連続残存には当たらない
