# Proposal: wait-for-host-appearance

## Why

提示先 (iOS は前面でアクティブなシーンの key window、Android は resumed な Activity) が無いときに呼ばれた表示の扱いが、機能と OS で揃っていない。

| 機能 | Android | iOS |
|---|---|---|
| Dialog | その場で失敗する (「提示先が無い」エラー) | 同左 |
| Loading | 処理は実行し、提示先が現れた時点で入りの演出から表示する | 処理は実行するが、表示しないまま終わる |
| Toast | 提示先の出現を待って表示する | 契約は同左。ただし起動直後の呼び出しで一度も表示されない |

「画面が出てから動かす」ごく普通の書き方 (最初の画面の表示時の処理。SwiftUI の `.task`) が、iOS ではシーンがアクティブになる前に走り、まだ提示先が無い。iOS Sample の実測では、この書き方で Loading が 3 回中 2 回、Toast が 5 回とも出ず、Dialog は 3 回とも失敗した (`kasane/changes/archive/2026-09-27-fix-ios-sample-demo-autoplay/evidence/autoplay-measurement.md`)。Loading・Toast はエラーにもならず黙って消えるので、利用者が気づきにくい。

iOS の Toast が待てない原因は、待ちが解ける合図 (window が key になった通知だけ) が提示先の条件 (シーンが前面でアクティブであること) の片方しか見ていないことだと見立てている (実測は実装の最初に行う)。

決定は探索で確定済み。経緯は [exploration.md](exploration.md)。

- [core/ADR-0039](../../decisions/core/0039-wait-for-host-appearance.md) (proposed): 提示先が無いまま呼ばれた Dialog・Loading・Toast は、全形態で失敗せず提示先の出現を待ち、待ちの上限は各機能の寿命に任せる
- [maui/ADR-0006](../../decisions/maui/0006-dialog-show-caller-cancellation.md) (proposed): MAUI の Dialog の show に呼び出し元の打ち切りを足す
- [core/ADR-0040](../../decisions/core/0040-content-created-after-host-secured.md) (proposed、core/ADR-0033 の amends): Toast・Loading の中身は全形態で提示先を確保してから作り、提示先が無いまま始まった Loading の生成失敗は表示だけを諦める (提案レビューでの指摘を受けてオーナーが改訂を選択)

## What Changes

- **契約 (core)**
  - Dialog: 提示先が無ければ失敗していたものを、失敗せず、提示先の出現を待って表示するように変える。待ちに上限は無く、呼び出し元が結果を待っている間は待ち続ける。待っている間に呼び出し元が打ち切ると、一度も表示されずに終わる (結果通知の既存ルール「呼び出し元の打ち切り」)。待っている Dialog が複数あれば、呼んだ順に出す (後から呼んだものが手前)
  - Loading: iOS も、提示先が現れた時点で表示が続いていれば入りの演出から表示する。OS 差の記述を無くす
  - Toast: 契約は変えない。iOS の実装を契約に合わせる
  - 例外: Native を持たない MAUI の素の .NET は、提示の仕組みそのものが無いため、今までどおりその場で失敗する
- **iOS Native**
  - 提示先の出現を待つ合図を、提示先を選ぶ規則と同じ場所に置く。window が key になったときと、シーンが前面でアクティブになったときの両方で見直す。Toast・Loading・Dialog で共用する
  - Toast の待ちをこの合図に置き換え、Loading と Dialog に待ちを足す
  - 公開 API から `DialogError.presentationHostUnavailable` を削除する
- **Android Native**
  - Dialog の提示面に、Loading・Toast が使っている resumed な Activity の入れ替わり購読を配線し、Dialog に待ちを足す
  - 公開 API から `DialogException.PresentationHostUnavailable` を削除する
- **KMP**: 振る舞いは両 OS の Native に従う。「提示先が無い」を KMP の失敗へ写す経路を外す
- **MAUI**
  - Dialog の show の全入口 (`ShowAsync` 7 本) に、呼び出し元の打ち切りを足す。両 OS のブリッジで Native の打ち切りへ中継する
  - MAUI 自前の提示先の判定 (MAUI の画面の文脈の解決。Dialog・Loading・Toast の各 gateway にある) を、失敗ではなく待ちに合わせる
  - `DialogException.PresentationHostUnavailable` は残し、素の .NET の失敗にだけ使う
- **テスト**
  - 全形態で、次のことを確かめる: 提示先が無い間は待ち、現れたら表示する / 待っている間に終われば表示しない (Toast の期限、Loading の終了、Dialog の打ち切り) / 待っている Dialog は呼んだ順に出る
  - 「その場で失敗する」を前提にしたテストと、それを別の検査の判定材料にしているテストを作り直す (KMP の DM-KM-01〜03・PB-KT-09・MB-KM-02、MAUI の一部を含む)
  - 直す前に、iOS の起動直後 (シーンがアクティブになる前) の呼び出しで 3 機能が出ないことを実測で再現し、証跡に残す。シーンが一時的に非アクティブになる場面 (システムの許可ダイアログなど) も観測する
- **Sample**: iOS Sample と KMP の iOS Sample の自動再生から、「シーンがアクティブになってから再生する」待ち (前回の change がライブラリの不具合を避けるために入れた回避) を外し、最初の画面の表示時の処理から呼ぶ形に戻す。デモ項目は変えない。自動再生が、直す前の再現と直した後の確認の観測点を兼ねる (design Decision 10、オーナー判断 2026-09-27)
- **ADR**: core/ADR-0039・maui/ADR-0006 (proposed) を、design と実装の結果に合わせて見直す
- **蒸留時に反映**:
  - concepts core/api/result-notification-semantics.md — 失敗のルール (出す先の画面が無い場合) と、呼び出し元の打ち切りの形態別の表 (MAUI の「経路なし」)
  - concepts core/api/loading-semantics.md・toast-semantics.md — 提示環境の不在の記述
  - concepts core/api/multi-display-semantics.md — 待っている Dialog の出る順番
  - concepts ios / android / maui / kmp の dialog-surface.md ほかの公開面 — 失敗の表と打ち切りの署名
  - handbook cross/test-execution.md — KMP の iosTest で提示先が常に無いことの説明と、提示先不在の文言の引用
  - core/ADR-0035 の現行照合 — 「Toast の型指定経路の VM factory・configure は iOS では受理時点で走る」という観測を、全形態で取り付けの時点に揃ったことへ更新する (決定の本文は変えない)
  - core/ADR-0039・maui/ADR-0006・core/ADR-0040 の昇格。ADR-0040 の昇格と同時に、core/ADR-0033 の frontmatter に `amended-by: 0040` を足し、index の ADR-0033 の行に「一部改訂: 0040」を書く

影響する能力: dialog-contract・loading-contract・toast-contract・ios-native・android-native・maui-binding・kmp-facade・samples

## Non-Goals

- **何を提示先とみなすかの定義の変更** — 別の設計判断が要る。定義を緩める案 (非アクティブでも前面のシーンなら提示先にする) は探索で却下済みで、core/ADR-0039 の範囲外
- **MAUI の Loading・Toast の入口への打ち切りの追加** — maui/ADR-0006 の範囲外。打ち切りを足す動機は Dialog の待ちに上限が無いことで、Loading・Toast はそれぞれの寿命 (処理の終了・表示時間) で終わる
- **Android の Toast・Loading の実装** — 変えない。待ちの合図 (resumed な Activity の入れ替わり) と提示先の条件がすでに一致している (探索のコード読解)。実機での確認は検証に含める
- **Sample のデモ項目** — 変えない。打ち切りなどを見せるデモは 4 ルートで揃える必要があり、この変更の契約の検証には要らない (自動再生の回避を外すことは What Changes に含める)
- **ViewModel なしで Dialog を出す新しい入口** — 別の change (add-viewmodel-less-dialog-show)。後から入る側が、提示先の出現待ちと MAUI の打ち切りに合わせる
- **利用者向け Skill (`skills/`) の追随** — docs-refresh 経由の手順で行う (CLAUDE.md の運用)

## Impact

- **破壊的変更あり**。公開 API はすべて 0.1.0-beta の範囲
  - 公開 API の削除: iOS の `DialogError.presentationHostUnavailable`、Android の `DialogException.PresentationHostUnavailable`。これを参照している利用者のコードはビルドが通らなくなる
  - 振る舞いの変更
    - Dialog: 全形態で、その場で失敗していた呼び出しが待つようになる。画面が現れない場所から呼ぶと返らない
    - iOS の Loading: 出なかった表示が、あとから出るようになる
    - iOS の Toast: 起動直後などで出なかった表示が出るようになる
  - MAUI: `ShowAsync` の全入口に引数が増える
- **リスク**
  - 画面が現れない場所から呼んだ Dialog は失敗せず返らないので、気づきにくい。呼び出し元の打ち切りで防ぐ
  - 長く背面にいたあとに、古い文脈の Dialog が表示されることがある
  - iOS の起動直後とシーンが一時的に非アクティブになる場面の順序は、まだ実測していない (見立て)。実装の最初に再現を撮り、見立てが外れたら design に戻る
  - iOS の Toast と Loading は、中身を受理・開始の時点で作る。MAUI の iOS では、MAUI の画面ができる前だと中身を作れず失敗しうる。中身をいつ作るかは design で決める
  - 「その場で失敗する」を前提にしたテストが全形態にあり、作り直しの量が多い

## 級: L

Dialog・Loading・Toast の 3 機能の契約を全形態で変え、公開 API の削除と追加を伴う破壊的変更 (オーナー確定 2026-09-27)。

domain: cross
