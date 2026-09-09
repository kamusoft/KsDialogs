# セカンドオピニオン: add-kmp-maven-distribution (code-002)
**相方**: codex / **label**: so-code-add-kmp-maven-distribution (turn 2、同一セッションの継続) / **日付**: 2026-09-09 / **対象**: 修正サイクル 1 後の作業ツリー (kmp/build.gradle.kts、android/build.gradle.kts、kmp/ksdialogs-kmp/build.gradle.kts、evidence/snapshot-central-publish-guard.txt、deviation.md 末尾 4 項目)
---
## 前回指摘の再判定

- Major「SNAPSHOT ガードが credentials 検証より後」: **解消**
  - `kmp/build.gradle.kts:68`〜`86` で、直接指定された Central タスクを設定段階で拒否し、間接経路を task graph 確定時に捕捉する二段構成になっています。
  - `evidence/snapshot-central-publish-guard.txt:28`〜`52` には、認証情報なし・offline で直接指定した10経路が SNAPSHOT 診断だけで停止した証跡があります。
  - `publish` 経由の同時報告は `deviation.md:7` の合意済み差分として除外しました。
  - `android/build.gradle.kts:68`〜`86` にも同じ修正が適用され、両ビルドルートのガード方式は一致しています。

- Minor「proposed ADR の参照」: **解消**
  - `kmp/ksdialogs-kmp/build.gradle.kts:144` から `cross/ADR-0008` が削除され、理由は自己完結した説明になっています。
  - `kmp/ksdialogs-kmp/build.gradle.kts:150` は accepted かつ正式形式の `cross/ADR-0002` のみを参照しています。

修正で追加されたガード処理、Android 側への付随修正、証跡および `deviation.md` の末尾4項目に、新たな Critical／Major は見つかりませんでした。ビルド・テストは再実行せず、コードと保存済み証跡による静的再レビューです。

判定: APPROVED


## 突き合わせ結果

前回採用した 2 指摘は相方も「解消」と判定。新規の Critical / Major なし。ホスト側 review-002 の結果と照合してレビュー完了を判定する。
