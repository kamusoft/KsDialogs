# セカンドオピニオン: add-kmp-maven-distribution (code-001)
**相方**: codex / **label**: so-code-add-kmp-maven-distribution / **日付**: 2026-09-09 / **対象**: 未コミットの作業ツリー (kmp/build.gradle.kts、kmp/ksdialogs-kmp/build.gradle.kts、SwiftBoundaryThrowsTests.kt、samples/kmp/*、handbook cross/test-execution.md、evidence/)
---
Critical 0件、Major 1件、Minor 1件です。提示済みの 153 tests / 0 failures は受領済みとして、再実行していません。

## 指摘事項

### 🟠 Major: SNAPSHOT ガードが認証情報の検証より後に実行される

**該当箇所**: `kmp/build.gradle.kts:63`  
**関連証跡**: `kasane/changes/add-kmp-maven-distribution/evidence/snapshot-central-publish-guard.txt:26`

**問題点**: ガードを `doFirst` に置いているため、Gradle が Central タスクの必須 credentials を事前検証した後でなければ実行されません。証跡にも、認証情報がない場合はガード到達前に credentials 解決で失敗するため、ダミー認証情報を渡したと明記されています。

これは spec の「ネットワークアクセスや認証より前に SNAPSHOT 診断で失敗する」と、完了済みになっている `tasks.md:9` を満たしていません。公開事故自体は防げていますが、明示された安全契約と検証条件からの逸脱です。

**推奨修正**: Central 向けタスクが task graph に含まれた段階など、タスク入力検証より前に例外を投げるガードへ移してください。修正後は credentials を一切渡さず、`--offline` で各入口が SNAPSHOT 固有診断になることを再確認し、証跡を更新してください。

### 🟡 Minor: ソースコメントが未確定 ADR と非正規 ID を参照している

**該当箇所**: `kmp/ksdialogs-kmp/build.gradle.kts:144`、`kmp/ksdialogs-kmp/build.gradle.kts:150`

**問題点**: `cross/ADR-0008` と `cross/ADR-0009` はいずれも proposed です。また、150行目の `0009` は正式形 `<domain>/ADR-NNNN` でもありません。`comment-policy.md` は、コメントから参照できる ADR を確定した決定の正式 ID に限定しています。

**推奨修正**: 144行目は既に理由が自己完結しているため、未確定 ADR の括弧書きを削除してください。最低 iOS 17 の根拠は accepted の `cross/ADR-0002` だけで成立するため、150行目はそれだけを参照してください。

## 照合した規約

- `comment-policy.md`（always）
- `test-execution.md`（テスト結果・完了判定）
- `sample-parity.md`（`samples/` の変更）
- `kasane/lessons/impl.md`
- `kasane/lessons/process.md`
- KMP／cross の関連 accepted ADR

`deviation.md` 記録済みの差分は、依頼どおり指摘対象から除外しました。その他の version 導出、SwiftPM 参照分岐、publication 構成、署名、Sample の composite build、`@Throws` 反射検査には追加の Critical／Major は見つかりませんでした。

判定: CHANGES_REQUESTED


## 突き合わせ結果 (review-001 との照合、2026-09-09)

| 指摘 | 出典 | 採否 | 根拠 |
|---|---|---|---|
| SNAPSHOT ガードが credentials 検証より後に走る (`kmp/build.gradle.kts` の `doFirst`) | 相方のみ (Major) | **採用** (Major) | spec Requirement「version の単一ソースと注入 (kmp)」の「ネットワークアクセスや認証より前に」と Scenario の「認証不足を理由としない」に対し、evidence/snapshot-central-publish-guard.txt がダミー認証情報なしではガードに到達しないと明記。該当箇所・実害シナリオとも特定済み。翻案元 android/build.gradle.kts も同じ形のため付随修正として同梱 |
| ソースコメントが proposed の ADR (cross/0008・0009) と非正規 ID を参照 | 相方のみ (Minor) | **採用** (Minor) | handbook cross/comment-policy.md は ADR 参照を確定した決定の正式 ID に限定。蒸留時に accepted 化した ADR の参照を足す運用と整合 |
| review-001 Minor 1〜3 / Suggestion | ホストのみ | ホスト判定どおり | 相方は指摘せず (矛盾なし) |

未解決: 0 件。修正サイクル 1 周目へ。
