# セカンドオピニオン: wait-for-host-appearance (code-002)
**相方**: codex / **label**: so-code-wait-for-host-appearance / **日付**: 2026-09-28 / **対象**: 修正サイクル 1 の後の、前回指摘 2 件の解消確認 (diff の範囲は code-001 と同じ)
---
## 再確認結果

1. **提示直前に提示先が消える問題: 部分的に解消**  
   `ios/Sources/KsDialogs/Presentation/UIKitDialogPresentationSurface.swift:36` は提示失敗を `completion(false)` で通知し、`ios/Sources/KsDialogs/Presentation/DialogPresenter.swift:125` は `cancelled` の確定と列の番の返却へ進みます。前回指摘した経路は解消しています。

2. **保留中の登録経路 Toast の factory: 解消**  
   `ios/Sources/KsDialogs/Presentation/ToastCoordinator.swift:95` で受理時の factory を要求へ保存し、同ファイル `:301` で解決済み要求へ変換しています。取り付け時は `:275` でその factory を使います。再登録を挟むテストも `ios/Tests/KsDialogsTests/ToastContractTests.swift:320` にあります。

### 🟠 Major — UIKit の遷移監視を登録できない場合、show が再び待ち続ける

**該当箇所**: `ios/Sources/KsDialogs/Presentation/UIKitDialogPresentationSurface.swift:58`  
**問題点**: `transitionCoordinator.animate(alongsideTransition:)` は監視を登録できたかを `Bool` で返しますが、戻り値を確認していません。登録できず、提示自体も拒否された場合は `latch.report(false)` にも UIKit の提示完了通知にも到達する保証がなく、show と列の番が残ります。追加テストは遷移中に監視できる経路を確認しており、この分岐は覆っていません。  
**推奨修正**: `false` の場合に提示関係を再確認し、載せられなければ `latch.report(false)` へ進めてください。その分岐で show の終了と次の番の解放を確認してください。

ホスト側のテスト成功は提示された結果として扱い、今回は実行していません。合意済みの `deviation.md` の結末に異議はありません。

**判定: CHANGES_REQUESTED**


## 突き合わせ結果

ホスト側: review-002.md (APPROVED、Suggestion 1。前回の対応 4 件はすべて解消)。相方: 上記 (CHANGES_REQUESTED、前回の 2 件は解消 / 部分的、新規 Major 1)。

- **確定 (Major)**: `ios/Sources/KsDialogs/Presentation/UIKitDialogPresentationSurface.swift` の、UIKit の遷移の完了を見張る登録 (`animate(alongsideTransition:completion:)`) の戻り値を見ていない。登録できず提示も拒否されると、show と列の番が残る。相方の新規 Major とホストの Suggestion (1) が一致。相方が高い重要度を主張するので Major とする
- **確定 (同じ箇所の保険として対応)**: 「載せられなかった」と報告した後に提示が遅れて結ばれると、片付け済みの器が画面に残る (ホストの Suggestion (2))
- **降格なし / 未解決なし**
- 収束の判断: 前回の確定 Major (提示面が器を載せられなかった show が終わらない) の修正で入った判定のうち、残った狭い分岐への指摘で、同じ指摘が 2 周続けて残ったものではない。修正サイクル 2 として対応する
