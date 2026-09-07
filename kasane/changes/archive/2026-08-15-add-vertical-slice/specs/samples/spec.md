# samples デルタスペック

集約 samples/ 4ルートの最小 Sample。構造判断は cross/ADR-0006 (consumer 境界)・cross/ADR-0007 (parity) が正。文言・画面構成の実物は ui/ (brief + 承認モック) が正であり、本スペックには書かない。

## ADDED Requirements

### Requirement: 4ルートの Basic Dialog デモ項目

samples/ios・samples/android・samples/maui・samples/kmp の各 Sample アプリは、デモ項目「Basic Dialog」を持ち、起動 → ダイアログ表示 → 閉じる → 結果表示の一連の流れが動作する SHALL。実行セルは **6セル** — Native iOS / Native Android / MAUI iOS / MAUI Android / KMP iOS / KMP Android — であり、samples/maui と samples/kmp はそれぞれ iOS / Android 両 OS でこれを満たす SHALL。

#### Scenario: 完了操作の結果が画面に表示される
- **GIVEN** Sample アプリを起動しデモ項目一覧が表示されている
- **WHEN** Basic Dialog 項目を起動し、表示されたダイアログを結果値つきの完了操作で閉じる
- **THEN** 完了 (completed) とその結果値が Sample の画面上で確認できる

#### Scenario: キャンセルの結果が画面に表示される
- **GIVEN** Basic Dialog のダイアログが表示されている
- **WHEN** キャンセル操作 (または外側タップ) で閉じる
- **THEN** キャンセル (cancelled) されたことが Sample の画面上で確認できる

### Requirement: Sample の consumer 境界

各 Sample は公開 product を利用者アプリと同じ側から参照する SHALL (iOS = Local Swift Package、Android = composite build、MAUI = facade への ProjectReference 1本、KMP = shared モジュールが KMP facade を消費)。Sample から本体の内部実装を直接参照しない SHALL。

#### Scenario: Sample が公開 API だけでビルド・動作する
- **GIVEN** 各ルートの Sample プロジェクト
- **WHEN** Sample をビルド・実行する
- **THEN** 公開 product の参照だけで成立し、内部実装への直接参照なしで Basic Dialog デモが動作する

### Requirement: 4ルートのパリティ

4ルートの Sample は、デモ項目単位 (起動項目の文言 + ダイアログ内容 + 結果表示) で同一のメニュー構成・同一の文言を持つ SHALL (cross/ADR-0007。文言の実物は ui/ が正)。

#### Scenario: 4ルートのデモ項目が一致する
- **GIVEN** 4ルートの Sample アプリをそれぞれ起動する
- **WHEN** デモ項目一覧と Basic Dialog の表示・結果表示を突き合わせる
- **THEN** 起動項目の文言・ダイアログ内の文言・結果表示の文言が4ルートで一致している
