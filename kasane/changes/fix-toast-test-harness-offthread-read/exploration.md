# Exploration: fix-toast-test-harness-offthread-read

## 課題 / 動機

Android の instrumented テスト `ToastMultiDisplayTests.TS_MX_05_Loading_は起動順によらず_Toast_より前面` が間欠的に `ConcurrentModificationException` で落ちる。

- スタック: `ToastCoordinator.getPresentedContainers` ← `ToastTestHarness.getContainers` ← `waitUntilPresenting`
- テストの待ち合わせ (`InstrumentedDialogWaiting.waitUntil`) が `runBlocking` のスレッドから Toast の状態の列を読み、UI スレッドが同じ列を書き換えている間に走査が重なった
- 同じクラスを同じ端末で回し直すと成功した (12 件中 12 件)。手元で間欠的に落ちる
- 観測環境: API 33 実機、`:ksdialogs-core` の `connectedDebugAndroidTest` 全件実行の 1 回

発見の文脈: define-loading-action-thread の tasks 6.1 (全ルートの全件実行)。この change は Toast とテスト補助に触れていない。記録は `kasane/changes/define-loading-action-thread/evidence/test-run-summary.md` の失敗 C (archive 後は `archive/*-define-loading-action-thread/`)。

## 検討した選択肢 (却下案と理由を含む)

## 決定事項

## ADR 候補 (作成済み: なし / 未起票: なし)

## 未決の論点

**未探索 (簡易起票)**。分かっている疑問点:

- 直す場所はテスト補助 (UI スレッドで読む) か、`ToastCoordinator.presentedContainers` の公開のしかた (スナップショットを返す) か。後者なら製品コードの契約 (UI スレッド外から読んでよいか) に触れる
- 同じ読み方をしているテスト補助がほかにあるか (Dialog / Loading の待ち合わせを含む)。handbook cross/ci-flaky-test-policy.md の「観測の待ち方」の規約との関係
- CI でも出ているか (CI 限定 skip の許可リストに載っていないか) は未確認

## UI 素材 (ui/references/ の一覧と注釈)

なし

## 変更級の推奨: 未判定 (暫定 S — テスト補助の読み方の修正で閉じるなら)
