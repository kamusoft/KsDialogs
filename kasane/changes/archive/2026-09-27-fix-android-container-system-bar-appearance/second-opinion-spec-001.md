# セカンドオピニオン: fix-android-container-system-bar-appearance (spec-001)
**相方**: codex / **label**: so-spec-fix-android-container-system-bar-appearance / **日付**: 2026-09-27 / **対象**: 提案一式 (proposal.md / specs/{dialog-contract,loading-contract,toast-contract,android-native}/spec.md / tasks.md)
---
**判定: NEEDS_DISCUSSION**

仕様の期待結果と予定された観測方法に食い違いがあります。実装前に受け入れ条件を明確にする必要があります。静的レビューのみで、ビルド・テストは実行していません。

1. **Major — iOS の明暗 Scenario が何を合格とするか曖昧です。** 該当箇所: [dialog-contract/spec.md:21](kasane/changes/fix-android-container-system-bar-appearance/specs/dialog-contract/spec.md:21)、[tasks.md:30](kasane/changes/fix-android-container-system-bar-appearance/tasks.md:30)。Scenario は明るい地向けの明暗が「使われる」とする一方、要件は暗幕に応じて OS が文字色を変える場合を除外しています。予定された iOS テストは提示関係とプロパティを調べるだけで、明暗の期待結果を判定しません。**推奨修正:** 「画面が要求した指定を保持すること」と「実際に描かれた文字色」を区別して THEN を定義し、指定を器が奪う退行を検出できる観測条件を決めてください。

2. **Major — 値の直接比較では旧来フラグの修正を判定できません。** 該当箇所: [proposal.md:20](kasane/changes/fix-android-container-system-bar-appearance/proposal.md:20)、[android-native/spec.md:59](kasane/changes/fix-android-container-system-bar-appearance/specs/android-native/spec.md:59)、[tasks.md:24](kasane/changes/fix-android-container-system-bar-appearance/tasks.md:24)。探索記録では、旧来フラグだけを設定した提示先について OS の明暗取得値は 0 です。正しく補完した器の値をその取得値と直接比べると、修正が成功しても不一致になります。未指定の `systemBarsBehavior` も同じ問題を持ちます。**推奨修正:** 提示先の旧来フラグを含めて期待値を組み立て、器の値と比較する方法を明記してください。未指定の作法は、生の値の一致ではなく OS 既定との一致で判定してください。

3. **Major — スプラッシュ終了の待機条件が定義されていません。** 該当箇所: [android-native/spec.md:75](kasane/changes/fix-android-container-system-bar-appearance/specs/android-native/spec.md:75)、[tasks.md:25](kasane/changes/fix-android-container-system-bar-appearance/tasks.md:25)。共通の安定待ちを使う指定だけでは、静止したスプラッシュを「落ち着いた提示先」と誤認した元の問題を防げません。**推奨修正:** 提示先固有の表示が画面上に現れたことを独立に確認する終端条件を定め、その条件が成立しなければ測定を失敗させてください。

4. **Major — 旧来フラグのナビゲーションバー明暗が Scenario から抜けています。** 該当箇所: [android-native/spec.md:19](kasane/changes/fix-android-container-system-bar-appearance/specs/android-native/spec.md:19)、[android-native/spec.md:59](kasane/changes/fix-android-container-system-bar-appearance/specs/android-native/spec.md:59)。Requirement は旧来フラグによるステータスバーとナビゲーションバーの明暗を対象にしますが、PB-SB-09 の GIVEN・THEN はステータスバーだけです。ナビゲーションバーの読み取りを落としても Scenario は通ります。**推奨修正:** ナビゲーションバーを独立の Scenario、または明示的な追加アサーションで固定してください。

5. **Minor — 載せ替え確認の合格条件が弱いです。** 該当箇所: [tasks.md:16](kasane/changes/fix-android-container-system-bar-appearance/tasks.md:16)。既存の再取り付けテストが通っても、新しい Activity のシステムバー指定を読み直した証明にはなりません。**推奨修正:** 再生成前後で異なる指定を与え、新しい器が再生成後の指定を採用したことを確認してください。

6. **Major — 完了条件と API 29 の除外が衝突します。** 該当箇所: [proposal.md:31](kasane/changes/fix-android-container-system-bar-appearance/proposal.md:31)、[tasks.md:41](kasane/changes/fix-android-container-system-bar-appearance/tasks.md:41)。提案は API 29 以下を検証対象から外しますが、tasks は handbook に従う「全ルート」の完了判定を要求しています。[test-execution.md:84](kasane/handbook/cross/test-execution.md:84) は API 29 の旧経路も走らせて初めて全件実行と言える、としています。**推奨修正:** 今回の完了報告で実行済みと呼ぶ範囲と、API 29 で未実行になる既存 Scenario の扱いを tasks に明記してください。

## 突き合わせ結果

突き合わせの相手はホスト側の自己レビュー (ksn-propose Step 8 の 2 周。チェックリストはすべて通過し、修正は「PB-SB-11 の要件本文から実装の関数名を実現経路へ移す」1 件だけ)。相方の 6 件はどれもホスト側が見逃したもので、該当箇所と実害の場面が示されていたため、すべて採用した。矛盾・未解決は無い。

| # | 重要度 | 指摘 | 採否 | 反映先 |
|---|---|---|---|---|
| 1 | Major | iOS の明暗の Scenario の合格条件が曖昧 (指定と実際の文字色の区別、観測条件) | 採用 | core 3 デルタの冒頭に「判定するのは明暗を決める指定で、実際の文字色ではない」と OS ごとの観測のしかた (iOS は UIKit が尋ねる相手が提示先のまま) を明記。THEN を「器の指定に置き換わらない」に |
| 2 | Major | 提示先から OS が返す値との直接比較では、旧来のフラグ・作法の未指定の修正を判定できない | 採用 | android-native デルタに「テストの比べ方」(テストが与えた指定から組み立てた期待値と比べる。作法の未指定は OS の既定値) を追加。proposal・core 3 デルタ・tasks 3.2 の「直接比べる」を置き換え |
| 3 | Major | スプラッシュ終了の待機条件が未定義 | 採用 | PB-SB-11 の要件に、明るさとは独立の終端条件 (フォーカス・提示先自身の描画・起動時の表示のウィンドウが無い) と「成り立たなければ測らずに失敗」を明記。tasks 3.7 |
| 4 | Major | 旧来のフラグのナビゲーションバーの明暗が Scenario に無い | 採用 | PB-SB-09 の GIVEN / THEN を両バーに。tasks 3.3〜3.6 にナビゲーションバーの追加の確かめ |
| 5 | Minor | 載せ替えの確認の合格条件が弱い | 採用 | tasks 2.3 を「作り直しの前後で異なる指定を与え、作り直し後の指定を採用したことを見る」に |
| 6 | Major | 完了条件 (全件実行) と API 29 の除外の衝突 | 採用 | proposal の Non-Goals と tasks 6.1・3.8 に、完了報告の呼び方 (API 31 / 35 で全件、API 29 は対象外) と PB-SB-04 の未実行の明記を追加 |

確定 0 件 / 採用 6 件 / 降格 0 件 / 未解決 0 件
