# セカンドオピニオン: deliver-swiftui-content-safe-area-ios (code-003)
**相方**: codex / **label**: so-code-deliver-swiftui-content-safe-area-ios-003 / **日付**: 2026-10-20 / **対象**: 62b4981 から作業ツリーまで (修正サイクル 2 の後)
---
# レビュー結果: deliver-swiftui-content-safe-area-ios (003 回目)

**日付**: 2026-10-20  
**判定**: APPROVED  
**保存予定先**: `kasane/changes/deliver-swiftui-content-safe-area-ios/review-003.md`

## サマリー

`git diff 62b4981` と対象の未追跡ファイルを静的レビューした結果、修正を要求する問題は見つからなかった。安全領域を渡す軸の選択、要求サイズからの安全領域の除外、表示中のサイズ変更への追従は、合意済みの差分を含む仕様と整合している。

ビルド・テストは依頼どおり実行していない。実行結果の評価は、ホスト提供の `evidence/test-run-summary.md` と `evidence/real-window-safe-area.md` に基づく。

## 照合した規約

- `kasane/handbook/cross/comment-policy.md` — 新規・変更コメントの自己完結性と参照形式。
- `kasane/handbook/cross/test-execution.md` — 実行ルート、件数、Scenario ID 網羅の報告。
- `kasane/handbook/cross/runtime-behavior-verification.md` — 修正前後の比較と Simulator の実測証跡。
- `kasane/handbook/cross/ci-flaky-test-policy.md` — MainActor 上での観測、履歴による否定確認、テストの無効化の有無。
- `kasane/handbook/cross/verification-ci.md` — 検証範囲と手元での確認の区別。
- core/ADR-0009・core/ADR-0011 — レイアウト検証の責務分担と SwiftUI の内部ホストへの集約。

ios/ADR-0002 は proposed のため、独立した違反判定の根拠には用いず、今回のデルタスペックを基準とした。

ロードしたスキル: `ksn-review`、`swift-ui-impl-skill`。

## 確認した観点

- **仕様充足**: 窓全体基準の端の軸だけで安全領域を渡し、中央の軸・窓全体以外の基準領域では無視する構成を確認した。キーボード領域は除外されている。
- **共通経路**: Dialog・Loading・Toast、および KMP の SwiftUI 登録経路が同じホストを使用することを確認した。
- **サイズ計算**: ホストへの幅の提案と安全領域の控除、配置幅での高さの再計算、理想サイズ変更時の提案幅の解除を確認した。
- **前回の確定指摘**: 長い本文の高さ潰れと、水平 fill の窓拡大後に高さが古い幅のまま残る問題について、対応する実装と修正前後の回帰テスト証跡を確認した。
- **既存挙動との区別**: 内容サイズに委ねた軸が窓拡大後も広がらない既存挙動は、指定された別 change の対象として今回の指摘から除外した。
- **スコープ**: proposal・spec の変更はなく、記録済みの deviation は合意済み差分として扱った。tasks の完了記載と証跡に矛盾は見つからなかった。
- **状態・所有関係**: MainActor 隔離、Observation、ホストと通知先の弱参照、KVO クロージャの循環参照回避を確認した。
- **テスト品質**: PB-IS-01〜19 の対応、矩形の継続観測、安全領域の履歴、動的サイズ変更、変更前コードとの比較を確認した。

テスト実行規約の全件実行表との照合結果は次のとおり。リポジトリ全ルートの実行済みとは扱っていない。

| ルート | 提供された実行結果 |
|---|---|
| ios/ | 395 tests passed |
| android/ | 未実行。今回の変更対象外 |
| android/ (instrumented) | 未実行。今回の変更対象外 |
| kmp/ | 169 tests、failures 0、errors 0、skipped 0 |
| maui/ | 未実行。変更した SwiftUI ホストを通らない |
| maui/android/native/ | 未実行。今回の変更対象外 |
| maui/macios/native/ | 17 tests passed |

Scenario ID 網羅は PB-IS 19/19、未網羅なしと報告されている。

## 指摘事項

なし。

Critical: 0 / Major: 0 / Minor: 0 / Suggestion: 0

## アクションプラン

本レビューによる追加修正要求はない。既存の幅追従問題は、`kasane/changes/fix-swiftui-window-area-first-layout-width/exploration.md` で継続して扱う。

## 突き合わせ結果

確定 0 / 採用 0 / 降格 0 / 未解決 0。相方・ホスト側 (review-003.md) とも APPROVED で、前回の確定指摘は解消した。ホスト側の Suggestion 1 件 (窓全体・上端の長い本文で、窓を縦長から横長に変える条件をテストで固定する) は任意で、今は見送る。その幅は起票済みの fix-swiftui-window-area-first-layout-width で変わる見込みがあるため。
