# セカンドオピニオン: add-release-workflow (code-001)
**相方**: codex / **label**: so-code-add-release-workflow / **日付**: 2026-09-10 / **対象**: 作業木の未コミット変更 (.github/workflows/release.yml・.github/release.yml・scripts/release/・Gradle 2 本・MAUI csproj 3 本・handbook 4 本・AGENTS.md)
---
# レビュー結果: add-release-workflow

**日付**: 2026-09-10  
**指摘件数**: Critical 1 / Major 3 / Minor 0 / Suggestion 0

## サマリー

公開済み binary と source tag の対応を壊し得る再実行判定と、再実行に必要な deployment ID を失う経路があります。特に「monorepo tag があれば完了」とする仕様は、tag と GitHub Release が別ステップである実装と整合しないため、完了判定の設計判断が必要です。

指定済みのテスト・lint 結果は受領済みとして扱い、再実行していません。`deviation.md` の4件は合意済み差分として指摘していません。

## 照合した規約

- `kasane/handbook/cross/comment-policy.md` — always
- `kasane/handbook/cross/test-execution.md` — 完了判定
- `kasane/handbook/cross/verification-ci.md` — workflow の変更
- `kasane/handbook/cross/local-development-setup.md` — release script の自己テスト
- `kasane/handbook/cross/release-procedure.md` — リリースと再実行
- `kasane/handbook/cross/ci-script-deletion.md` — CI script 内の削除
- `kasane/lessons/code-review.md` — L-001、検査の識別力
- 関連する accepted ADR と consumer-verification concept
- `cross/ADR-0024` は proposed のため、確定規約ではなく変更設計の入力としてのみ参照

## 指摘事項

### [🔴 Critical] `run_attempt >= 2` だけでは外部状態が同じ run 由来だと証明できない

**該当箇所**: `scripts/release/check-resume-eligibility.sh:90`、`.github/workflows/release.yml:662`

**問題点**: 外部状態が既に存在する新規 dispatch は attempt 1 で拒否されますが、その同じ run を再実行すると、外部状態の出所を確認せず `resume` が許可されます。自己テストも attempt 1 の拒否直後に attempt 2 を許可する判定を明示的に固定しています。

したがって、別 commit から作られた Maven/NuGet が先に存在する場合でも、

1. 新規 dispatch が拒否される
2. 案内どおりその run を再実行する
3. 既存 binary を skip し、現在の `GITHUB_SHA` に monorepo tag を作る

という経路が成立します。「同じ run の再実行だけを許可する」という形式は満たしても、binary と source の対応は保証できません。

**推奨修正**: attempt 1 で外部状態が `none` と確認できた場合にだけ、version・`GITHUB_SHA`・run ID を含む run-scoped の eligibility marker artifact を保存してください。attempt 2 以降は、その marker が存在して内容が一致する場合だけ resume を許可します。

L-001 に従い、「既存外部状態で拒否された attempt 1 → marker なしの attempt 2」が引き続き失敗し、正規の attempt 1 が作った marker を与えた場合だけ成功する自己テストを追加してください。

### [🟠 Major] monorepo tag を完了印にすると GitHub Release の作成失敗を再開できない

**該当箇所**: `scripts/release/check-resume-eligibility.sh:78`、`.github/workflows/release.yml:1139`、`.github/workflows/release.yml:1156`

**問題点**: monorepo tag は GitHub Release より先に作られますが、同じ commit の tag があれば `skip-all` になります。tag push 後に Release 作成が失敗、cancel、timeout した場合、再実行は Release 作成ステップまで含めてすべて skip します。その後の registry 待ちと smoke は成功できるため、GitHub Release が無いまま workflow が緑になり得ます。

デルタスペックには「同じ commit の tag があれば完了済み」と「tag の後に GitHub Release を作る」の両方があり、現在の実装では同時に保証できません。

**推奨修正**: 完了印を次のどちらにするか決定してください。

- monorepo tag があっても、GitHub Release の存在・prerelease 属性を確認し、不足する後処理を実行する
- tag と Release を一体で作れる処理に変更し、その完了後に別の completion marker を残す

少なくとも `skip-all` を tag の存在だけで決めず、tag 後の必須ステップを冪等に再開できる状態へ分ける必要があります。

### [🟠 Major] artifact download の早期失敗で保存済み deployment ID を空に上書きする

**該当箇所**: `.github/workflows/release.yml:678`、`.github/workflows/release.yml:743`、`.github/workflows/release.yml:1232`、`.github/workflows/release.yml:1290`

**問題点**: 再実行時、前回の deployment ID artifact を取得する前に Android/MAUI artifact をdownloadしています。どちらかの download が失敗すると、失敗処理は deployment ID ファイルを空で新規作成し、その空ファイルで既存の deployment ID artifact を上書きします。

VALIDATING/PUBLISHING 中の ID を失うと、次回は同じ deployment を引き継げず、重複 upload または Portal 上での手動復旧が必要になります。行740付近の「通信失敗をIDなしと読み替えない」という意図にも反します。

**推奨修正**:

- deployment ID artifact の取得を他の fallible download より前へ移す
- 「既存IDを正常に取得した」または「uploadログから新IDを回収した」場合だけ artifact を更新する
- IDを取得できなかった失敗経路では、既存artifactを上書きしない

既存ID付きの再実行で、package artifact download を強制失敗させてもIDが保持される検査を追加してください。

### [🟠 Major] nuget.org の照会失敗を「外部状態なし」と扱っている

**該当箇所**: `.github/workflows/release.yml:655`

**問題点**: `curl ... || true` により、timeout・DNS失敗・5xx・不正JSONのすべてが空応答となり、「そのversionは存在しない」と判定されます。この照会は既存binaryと現在のsourceを誤って結び付けないための安全ゲートなので、判定不能時の fail-open は危険です。

nuget.org に実際は同versionが存在していても、一時的な通信障害中に新規dispatchがpublishへ進み、`--skip-duplicate` で既存packageを受け入れた後、現在のcommitへtagを作成できます。

**推奨修正**: HTTP応答を次のように判別してください。

- `200`: JSONとして解析し、versions配列の完全一致で判定
- `404`: 未公開
- 通信失敗、その他のstatus、不正JSON: 判定不能としてworkflowを失敗

通信失敗や壊れたJSONで `external=none` にならない負ケースを自己テストへ追加してください。

## アクションプラン

1. run-scoped eligibility markerで再実行の出所を証明する
2. monorepo tag後の完了境界を決定し、GitHub Releaseを再開可能にする
3. deployment IDの取得・保存順を修正する
4. NuGet外部状態検査をfail-closedにする
5. 各修正について、機構を外すと失敗する識別力のある負ケースを追加する

**判定: NEEDS_DISCUSSION**



## 突き合わせ結果 (review-001 との照合、2026-09-10)

| 指摘 | 出典 | 採否 | 重要度 | 理由 |
|---|---|---|---|---|
| 早期失敗の再実行で deployment ID を空で上書きする (`Recover` / `Drop` / `Store` が `failure()` のみ) | 双方一致 (review-001 Major / 相方 Major) | **確定** | Major | 該当箇所・実害シナリオ一致。翻案元のガードが 2 枠化で落ちた |
| `run_attempt >= 2` だけでは外部状態の出所を証明できない (拒否された run の再実行で resume が通り、別 commit の binary に tag を打つ) | 相方のみ | **採用** | Critical | 経路が具体的 (拒否 → 案内どおり再実行 → resume)。attempt 1 で `none` を確認した run だけが marker を残す形で塞げる。自己テストの負ケースが必要 |
| monorepo tag を完了印にすると Release 作成失敗を再開できない (`skip-all` で Release / `develop` 更新も skip) | 相方のみ | **採用** | Major | spec「tag があれば完了済みとして全ステップを skip」の字面と「tag の後に Release」が両立しない実装。registry への publish だけを skip し、tag 以降の冪等 step (Release は既存なら触らない・README 置換) は再実行しても走らせる形で解消する (spec の字面からの逸脱は deviation に記録) |
| nuget.org の照会失敗を「外部状態なし」と扱う (`curl ... \|\| true` の fail-open) | 相方のみ | **採用** | Major | 安全ゲートの fail-open。200 / 404 / それ以外で判別し、判定不能は失敗にする |
| `develop` への反映 step の fetch / worktree / commit 失敗が警告に落ちない | review-001 のみ | 採用 | Minor | 全形態の公開後に job を赤くし smoke を skip させうる。`FETCH_HEAD` 利用と warn 化 |
| `automaticRelease = false` の証跡に判別力が無い | review-001 のみ | 採用 | Minor | lessons code-review L-001。証跡だけの問題 |
| 初回リリース後に README / Skill の散文 (「まだ公開していない」「`<version>` を置き換える」) が矛盾する | review-001 のみ | 降格 (申し送り) | Minor | proposal Non-Goals の docs-refresh 2 回目の範囲そのもの。本 change では直さない |
| 自己テストが CI に載らない | review-001 のみ | 降格 (申し送り) | Suggestion | tasks 2.5 の検討結果 (cross/ADR-0022 の一部改訂を蒸留へ申し送り) と同じ |

採用 / 確定: 6 件 (Critical 1 / Major 3 / Minor 2)。降格: 2 件。未解決: 0 件。相方の判定 NEEDS_DISCUSSION は「完了印の設計判断」を指すが、spec の意図 (重複 publish の禁止) を保ったまま冪等 step を再実行可能にする形で解消できるため、修正サイクルで扱う。
