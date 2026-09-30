# samples デルタ (define-loading-action-thread)

パリティ規約 (handbook/cross/sample-parity) に従う。Sample 専用の Scenario は scenario-id-coverage の除外 ID に登録する (機械検証はせず、手で通して verification の証跡を残す)。デモ項目と安定デモ ID は追加しない。文言は 1 つ (`結果: 処理中`) 追加する。sample-parity の文言の表への追記は、規約層の更新なので蒸留時に行う。

## ADDED Requirements

### Requirement: Default Loading のデモで、action の中から結果表示を更新する

4 ルート (ios / android / maui / kmp) の `Default Loading` のデモは、action の最初の文で、メニューの結果表示を `結果: 処理中` に直接更新する (SHALL)。UI スレッドへ明示的に移す書き方 (`MainActor.run` / `MainThread` / `withContext` 等) は使わず、スコープ形の既定 (UI スレッドで始まる) に任せる。完了後は、今までどおり `結果: 完了` に変わる。

この更新は、新しい既定の使い方の見本であり、MAUI iOS の元の不具合 (action 内の UIKit 呼び出しがメインスレッド外で失敗する) を実際の MAUI ホストで確かめる観測点でもある。変更前の MAUI iOS では、Debug 構成で結果表示の更新が UIKit のスレッド検査に掛かって失敗する。

文言 `結果: 処理中` は 4 ルートの `SampleText` に同じ値で持つ。

#### Scenario: [LD-HS-02] Default Loading の処理中に、結果表示が action の中から更新される
- **GIVEN** メニューを表示した Sample (ios / android / maui / kmp の 4 ルート。MAUI は iOS と Android の両方)。Loading の刻み間隔を延ばす起動引数で起動している
- **WHEN** `Default Loading` をタップし、Loading の表示中に画面を撮る
- **THEN** Loading の表示中、結果表示は `結果: 処理中` になっている。アプリは落ちない。完了後、結果表示は `結果: 完了` に変わる

### Requirement: iOS Sample のスコープ形を新しい既定に合わせる

iOS Sample (`samples/ios/KsDialogsSample/SampleMenuModel.swift`) の次の書き方を外し、action の中から MainActor のプロパティを直接読む形に直す (SHALL)。
- 「進捗を報告するクロージャは MainActor の外で動く」という前提のコメント
- その前提のために、値を先に取り出しておく書き方

この Requirement での観察できる挙動 (表示・進捗の刻み・メッセージの差し替え・完了の結果表示) は変えない (処理中の結果表示は、上の Requirement が 4 ルートそろえて加える)。ほかの 3 ルートには、UI スレッド外で動くことを前提にした書き方が無いため、この Requirement の対象外。

#### Scenario: [LD-HS-01] iOS の Default Loading / Custom Loading の通し
- **GIVEN** メニューを表示した iOS Sample
- **WHEN** `Default Loading` と `Custom Loading` をそれぞれタップする
- **THEN** 変更前と同じく、Loading が表示されて進捗が刻まれ (Default Loading は途中でメッセージが差し替わる)、完了すると結果表示が `結果: 完了` に変わる
