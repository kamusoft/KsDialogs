# Exploration: fix-android-startup-toast-under-splash

(簡易起票 2026-09-29。align-sample-autoplay-start の実機での確認で発見)

## 課題 / 動機

Android で、アプリの起動直後 (最初の画面の表示時の処理) に表示時間の短い Toast を出すと、画面に見えないまま終わる回が多い。

- 観測: Sample の自動再生から Sample 側の待ち (`post` / `Dispatch` で受理を最初の描画の後まで遅らせる処理) を外すと、Android 系 3 ルートで起動直後の Toast が出ない回が増えた。kmp (Android) の default-toast は待ちありで 5/5 → 待ちなしで 1/5、MAUI Android の登録経路の custom-toast は 5/5 → 1/5、Android のインライン経路の custom-toast は 3/5 → 0/5。Dialog と Loading は 4 ルートとも全回で表示され、iOS 系では起きていない。実測は `kasane/changes/archive/2026-09-29-align-sample-autoplay-start/evidence/autoplay-measurement.md`
- 見立て (未検証): Toast の表示時間は受理の時点から数える (core の toast-semantics、wait-for-host-appearance の TS-HW-01)。Android の提示先は resumed な Activity で、これは最初の描画と起動画面の退場より前にそろう。そのため Toast は起動画面の下で表示時間を使い切る。表示時間の短い Toast ほど、起動から最初の描画までが長い構成 (MAUI) ほど出ない
- Sample 側の待ちでも隠しきれない: 待ちを戻した版 (受理を最初の描画の後まで遅らせる) でも、android の default-toast (1500 ms) は 3/5 で、直す前の版も 3/5 だった。出なかった 2 回は起動から最初の描画までが 4.8 秒・6.9 秒と長い回で、最初の描画から起動画面の退場までの間に表示時間が尽きると見ている (同じ evidence の「3 回目の実測」節)
- 利用者への影響: Sample に限らず、利用者のアプリが Android の起動直後に Toast を出すと見えずに終わりうる。エラーにもならないので気づきにくい

## 考えられる方向 (未検討)

- Android の提示先の条件を、resumed に加えて最初の描画 / window のフォーカスまで含める
- Toast の表示時間を、受理の時点ではなく画面に載った (または見える) 時点から数える
- どちらも core/ADR-0041 と toast-contract (TS-HW-01「受理時点から数えた duration」) に手が入り、全形態の揃えに関わる。iOS の提示先の条件 (前面でアクティブなシーンの key window) との対称も見る

## スコープに含めるもの (オーナー指示 2026-09-29)

- ライブラリの振る舞いの見直し
- **Sample の修正**: Android 系 3 ルート (`samples/android`・`samples/kmp/androidApp`・`samples/maui` の Android) に残した「最初の描画の後に再生する」Sample 側の待ちを外し、iOS 系と同じく最初の画面の表示時の処理から呼ぶ形に 4 ルートを揃える (align-sample-autoplay-start で見送った部分)。外した後に Android 系で起動直後の Toast が表示されることを実機で確かめる

## 級: 未判定 (契約の見直しを含むので M 以上の見込み)

domain: cross
