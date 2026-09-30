# セカンドオピニオン: fix-android-startup-toast-under-splash (code-001)
**相方**: codex / **label**: so-code-fix-android-startup-toast-under-splash / **日付**: 2026-09-30 / **対象**: 作業ツリーの未コミットの変更すべて (基点 HEAD cbfbf00。android/ ios/ kmp/ maui/ samples/ scripts/)
---
**判定: CHANGES_REQUESTED**（Major 1件）

### 🟠 Major — decorView の生成待ちで最初の描画を取り逃す可能性

**該当箇所:** `android/ksdialogs-core/src/main/kotlin/jp/kamusoft/ksdialogs/ActivityDrawObserver.kt:69`

**問題点:** `peekDecorView()` が `null` の場合、描画監視の登録を次のメッセージ周回へ一度だけ延期しています。その間に最初の描画が済むと、監視は描画後に登録されます。`onActivityResumed` 時点でも decorView が無ければ `requestDraw` は何もせず、登録後にも再描画を促さないため、次の描画が起きない画面では提示先が確定しません。Dialog が待ち続け、Loading・Toast も表示されない経路です。追加された実機テストは `onCreate` で `setContentView` するため、この分岐を通りません。

**推奨修正:** 遅れて decorView を取得して監視を登録した際にも描画を促し、最初の描画が登録より先に終わる順序を検証してください。監視登録から提示先確定まで、後続の描画に偶然頼らない形にする必要があります。

仕様、合意済みの `deviation.md`、適用するコメント・テスト・Sample・実行時挙動の規約と差分を照合しました。ビルド・テストは依頼どおり実行せず、提示された成功結果を前提に静的レビューを行いました。


## 突き合わせ結果

ホスト側: review-001.md (NEEDS_DISCUSSION。Major 1 / Minor 2 / Suggestion 1)

| 指摘 | 出典 | 採否 | 重要度 |
|---|---|---|---|
| decorView が無い画面で描画の観測が始まらない・最初の描画を取り逃す (`ActivityDrawObserver.kt` の取り直しの経路。ホスト Minor 1 と同じ箇所・同じ帰結: 提示先が確定せず Dialog が返らない) | 双方 | 確定 | Major (相方の主張する高い方を採る) |
| 背面から戻るたびに Android の Loading・Toast の器が外れて載り直す | ホストのみ | NEEDS_DISCUSSION としてオーナーへ | Major |
| Dialog・Loading の提示先の説明が「resumed な Activity」のまま | ホストのみ | 確定 (ホスト側の指摘として修正) | Minor |
| iOS の TS-HW-06 の時間の余裕が Android より狭い | ホストのみ | 確定 (ホスト側の指摘として修正) | Suggestion |

採用 (相方のみ・根拠強): 0 件 / 降格: 0 件 / 未解決 (両者の矛盾): 0 件
