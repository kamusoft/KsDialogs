# セカンドオピニオン: fix-android-startup-toast-under-splash (code-002)
**相方**: codex / **label**: so-code-fix-android-startup-toast-under-splash / **日付**: 2026-09-30 / **対象**: 作業ツリーの未コミットの変更すべて (基点 HEAD cbfbf00。review-001 の確定指摘の修正後)
---
**判定: APPROVED**（Critical 0件、Major 0件、Minor 0件、Suggestion 0件）

前回指摘した描画の取りこぼしは解消されています。`ActivityDrawObserver.kt:88-92` で、遅れて decorView に監視を張った後に描画を促し、`ResumedActivityTracker.kt:110-117` で監視を始められていなければ張り直します。この経路を通す instrumented テストも追加されています。

合意済みの deviation を含めて実装差分を静的に再確認し、新たな指摘はありません。テストは依頼どおり実行していません。判定は提示された成功結果を前提とします。


## 突き合わせ結果

ホスト側: review-002.md (APPROVED。Minor 2)。相方: APPROVED (指摘 0)

| 指摘 | 出典 | 採否 | 扱い |
|---|---|---|---|
| 前回の確定指摘 4 件の解消 | 双方 | 確定 (解消) | — |
| deviation で決めた実測の場面 (Loading の表示中に背面へ下がって戻る) が tasks 7 に無い | ホストのみ | 確定 (Minor) | tasks.md は足場のため書き換えず、deviation.md の記録を正としてグループ 7 の実測の依頼に含める |
| 未破棄の画面 A から描画済みの別の画面 B へ器が移る経路の、判定を実物でつないだテストが無い | ホストのみ | 確定 (Minor) | テストを足す。確認は verify で行う |

採用 (相方のみ・根拠強): 0 件 / 降格: 0 件 / 未解決: 0 件
