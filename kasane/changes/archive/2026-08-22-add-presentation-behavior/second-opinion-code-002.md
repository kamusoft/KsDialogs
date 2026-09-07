# セカンドオピニオン: add-presentation-behavior (code-002)
**相方**: codex (同一セッションへの追送、器 ksn-reviewer) / **日付**: 2026-08-22 / **対象**: 修正サイクル 1 周目 (#1〜#5) 後の作業ツリー。入力はホスト側 review-002 と同一 (修正範囲・deviation.md・テスト結果の要約)
---
# 再レビュー結果: add-presentation-behavior

**判定: CHANGES_REQUESTED**

前回の公開 API と Scenario 抽出の指摘は解消しています。撤去完了待ちも基本構造は改善されていますが、OS 発消失時に `show` が永久待機し得る新しい寿命問題があるため、承認にはできません。

## 前回指摘の解消状況

| 項目 | 判定 |
|---|---|
| Major #1 `defaultDuration` の公開露出 | **解消**。internal 化、公開 factory のリテラル既定値、負の検査が揃っています |
| Major #2 撤去前配送 | **部分解消**。completion ゲートは正しい一方、下記 Major の寿命問題があります |
| Minor Scenario ID 抽出 | **解消**。宣言行と直前の属性・注釈に限定され、コメント・証跡名の偽陽性テストもあります |
| MAUI 件数 62 | **解消** |
| accessibility tree 証跡 | **解消**。4ルート6形態×2状態が実在します |

## 指摘事項

### [🟠 Major] 器の解放時に配送口も失われ、OS 発消失で `show` が返らない可能性がある

**該当箇所**: [DialogContainerViewController.swift:353](<ios/Sources/KsDialogs/Presentation/DialogContainerViewController.swift:353>)、[DialogPresenter.swift:64](<ios/Sources/KsDialogs/Presentation/DialogPresenter.swift:64>)

**問題点**: `viewDidDisappear` 後の Task は `self` を弱参照しています。Task 実行前に UIKit が器を解放すると、`self == nil` 分岐は `resultChannel` を settle するだけです。しかし continuation を resume する唯一の `onDelivery` は器自身が所有しているため、器の解放と同時に失われます。「呼び出し元を解放する」というコメントに反して `show` は永久待機し得ます。

現在のテスト surface は `presentedContainers` と各テストのローカル変数が器を強保持するため、この経路を検出できません。

**推奨修正**: 次のいずれかで配送主体の寿命を保証してください。

- `viewDidDisappear` の1回分の Taskでは器を強く保持し、必ず `handleHostLost()`／`completeRemoval()` まで進める
- `DialogPresenter` の async フレームで配送完了まで器を明示的に延命する
- より堅牢には、continuation とラッチ済み結果を器から独立した completion coordinator に所有させる

外部の強参照を落としてから disappearance の MainActor ターンを進めても PB-TR-12/23 が配送されるテストも必要です。

### [🟡 Minor] `removed` が実際の撤去完了より前に設定される

**該当箇所**: [DialogContainerViewController.swift:484](<ios/Sources/KsDialogs/Presentation/DialogContainerViewController.swift:484>)

**問題点**: `finishRemoval()` は dismissal completion を待つ前に `containerState = .removed` とします。Decision 10 の `removed = 器を撤去し、outcome を配送` という定義と一致せず、PB-TR-10/13 の completion 保留中にも内部状態だけは撤去済みになります。

**推奨修正**: 撤去要求中は `dismissing` 等の現状態を維持し、別の private フラグで要求済みを表してください。`containerState = .removed` は `completeRemoval()` 内へ移し、テストにも completion 前は `.removed` でないことを追加してください。

`viewDidDisappear` を第2の完了合図にする判断自体は妥当です。自分から撤去要求済みで、実際に View が画面から外れたことを確認し、1回きりのゲートを通すなら PB-TR-10/13 の「画面から消えた後」を満たします。ただし、状態フラグと寿命問題の修正が前提です。

### [🟡 Minor] 新しい負の検査と最終 iOS 件数が申し送りへ反映されていない

**該当箇所**: [handoff-distill.md:5](<kasane/changes/archive/2026-08-22-add-presentation-behavior/handoff-distill.md:5>)、[handoff-distill.md:15](<kasane/changes/archive/2026-08-22-add-presentation-behavior/handoff-distill.md:15>)

**問題点**: iOS は現在、負の検査8本・通常テスト130本ですが、申し送りには7本・129本までしかありません。蒸留時に `test-execution.md` を古い件数へ更新する可能性があります。

**推奨修正**: `KSDIALOGS_NEGATIVE_CHECK_DEFAULT_DURATION` と期待診断を追加し、最終値を8本／130 tests・25 suitesと明記してください。

### [🟡 Minor] MAUI iOSだけ無効なチップが有効なボタンとして読み上げられる

**該当箇所**: [panel-accessibility/README.md:77](<kasane/changes/archive/2026-08-22-add-presentation-behavior/verification/panel-accessibility/README.md:77>)、[SampleChipsView.cs:101](<samples/maui/KsDialogs.Sample.Maui/SampleChipsView.cs:101>)

**問題点**: 追加証跡により、MAUI iOSでは操作不能なイージングチップに `Disabled` が付かず、VoiceOver利用者には押せるボタンとして見えることが判明しています。「操作部の名前・役割・状態を4ルート一致」とするパリティ規約から外れます。

**推奨修正**: `SampleChipsView` の有効状態を各 `Button` へ明示的に伝播し、iOS platform viewの accessibility enabled/disabled 状態も更新してください。

## サマリー

- Critical: 0
- Major: 1
- Minor: 3
- Suggestion: 0

ホスト提示のテスト結果を根拠として扱い、制約どおりビルド・テストは再実行していません。ファイルへの書き込みも行っていません。


## 突き合わせ結果 (ホスト review-002.md との照合、2026-08-22)

| # | 指摘 | ホスト (review-002) | 相方 (codex) | 採否 | 根拠 |
|---|---|---|---|---|---|
| 1 | 待ち合わせ窓 (撤去要求〜完了通知) の中で器が解放されると、配送口 `onDelivery` (器が所有・weak 参照のみ) が失われ show が永久待機し得る。`DialogContainerViewController.swift:354-357` の「待っている呼び出し元を解放する」コメントも現結線では不成立 (settle はラッチであって配送ではない) | 🟡 M-1 | 🟠 Major | **確定 (Major)** | 双方一致。重要度はホスト基準だが相方が高い方を主張するため Major。修正サイクル 2 周目 |
| 2 | `finishRemoval()` が撤去要求の前に `containerState = .removed` を立てる (Decision 10 の定義と不一致)。`.removed` 分岐に `presentingViewController == nil` を足せば通常撤去は完了通知に譲れる | 🔵 S-1 | 🟡 Minor | **確定 (Minor)** | 双方一致。#1 と同じ箇所の修正に含める |
| 3 | `UIKitDialogPresentationSurface` の本命経路 (`presenting.dismiss(animated:false){…}`) が自動検査を 1 本も通っていない | 🟡 M-2 | — | 採用 (ホストのみ) | 修正サイクル 2 周目で記録用の提示元 VC によるテストを追加 |
| 4 | MAUI iOS の無効なイージングチップが VoiceOver に有効ボタンとして見える (`SampleChipsView.cs:101`) | — (review-001 で「規約の状態列挙外」と判定) | 🟡 Minor | **降格** | 相方のみ + Minor。sample-parity.md が列挙する状態 (選択・ON/OFF・現在値) に無効状態は含まれず、規約違反と断定できない。Sample の platform 分岐追加になるためオーナー判断 (S 級 UI 微調整) として完了報告へ。handoff-distill.md に記録済み |
| 5 | handoff-distill.md に負の検査 8 本目と iOS 130 tests が未反映 | — | 🟡 Minor | 確定 | オーケストレーターが直接修正済み |

- 双方が「撤去完了の合図が 2 つ (UIKit completion / 器の `viewDidDisappear`、先着 1 回)」を**妥当**と評価 (ホスト: `.removed` を先に立てる窓では `viewDidDisappear` が唯一の合図で構造上必要な保険 / 相方: 自分から撤去要求済み + 実際に画面から外れた + 1 回きりのゲートなら PB-TR-10/13 を満たす)
- 降格: 1 件 (#4) / 未解決 (矛盾): 0 件 / 採用 (相方のみ): 0 件 / 確定: 3 件 (#1 #2 #5) / ホストのみ: 1 件 (#3)
- 収束シグナル: 1 周目の #2 (撤去完了待ち) 自体は解消し、#1 は修正が作った待ち合わせ窓に起因する派生指摘。同一指摘の 2 周連続残存には当たらない
