# レビュー結果: rollout-user-docs (024 回目)

**日付**: 2026-09-05
**判定**: APPROVED

## サマリー

`review-021.md` の CHANGES_REQUESTED 理由 (KMP library の iosMain metadata compile が `@Throws` filter 不一致で失敗する) は解消している。KMP library の `compileIosMainKotlinMetadata`、KMP Sample の `:shared:compileCommonMainKotlinMetadata` をいずれも `--rerun-tasks` 付きで自分で実行して成功を確認し、`kmp/` の `allTests` も 96 tests / 0 failures で通した。生成 ObjC ヘッダを直接読み、`@Throws` を override から外しても ViewModel 経路 4 本の NSError 化契約が保たれていることも実物で確認した。源泉 concept に増えた注意点の KMP Skill への翻訳は、concept・現行実装・閉世界性・en / ja 等価性・manifest 規約のいずれも満たしている。Critical / Major はなく、注意点の適用範囲が源泉 concept ごと実機構より狭い点と deviation 記録の陳腐化を Minor として挙げる。

## 照合した規約

適用と判定した handbook 文書 (cross ドメイン + kmp ドメイン。kmp は文書なし):

- `cross/comment-policy.md` (**always**) — diff 範囲に Kotlin ソースの KDoc 追記があるため
- `cross/user-skill-api-listing.md` — `skills/**` を更新するため (掲載範囲・除外リスト・コード例のコメント規約)
- `cross/test-execution.md` — 変更の完了判定とテスト結果の報告を行うため
- `cross/local-development-setup.md` (guide) — worktree で Gradle ルートを動かすため

適用外と判定: `cross/sample-parity.md` (`samples/` の変更なし)、`cross/runtime-behavior-verification.md` (実行時挙動の不具合調査ではない)、`cross/aiforms-origin-reference.md` (Dialog / Loading の移植作業ではない)。

その他に照合した正:

- `.agents/skills/docs-refresh/SKILL.md` — manifest v3 スキーマと不変条件、Step 6 の 8 検査、Step 7 のハッシュ更新規則
- `.agents/skills/docs-refresh/references/prompt-skill.md` — 生成物の内容規約 ①〜⑧
- `specs/user-skills/spec.md` — Requirement「KMP Skill の共有コード側と iOS ホスト側」「閉世界性」「翻訳ロックステップ」「manifest 初期版」
- `design.md` Decision 1 / Decision 3 — `references/` の割り、`targets` 割当とファイル単位の源泉完全性
- `kasane/decisions/kmp/0001-*.md` の 2026-09-05 現行照合行
- `kasane/lessons/process.md` / `impl.md` (昇格済み)、`kasane/lessons/inbox/kmp-completion-must-run-hierarchical-metadata-compile.md` (未昇格の捕捉。本レビューの完了判定に metadata compile を含めた根拠)
- `deviation.md` の合意済み乖離 (Task 9.1 の全ビルド省略を含む)

## 確認済み事項

### ビルド・テスト (すべて自分で実行)

| 実行 | 結果 |
|---|---|
| `kmp/` `./gradlew :ksdialogs-kmp:compileIosMainKotlinMetadata --rerun-tasks` | BUILD SUCCESSFUL (26 tasks executed。`Member overrides different '@Throws' filter` は 0 件) |
| `samples/kmp/` `./gradlew :shared:compileCommonMainKotlinMetadata --rerun-tasks` | BUILD SUCCESSFUL (35 tasks executed) |
| `kmp/` `./gradlew allTests --rerun-tasks` | BUILD SUCCESSFUL / **96 tests / 0 failures / 0 errors** (iosSimulatorArm64Test + testAndroidHostTest。`iosX64Test` は arm64 ホストで SKIPPED) |
| `kmp/` `./gradlew :ksdialogs-kmp:linkDebugFrameworkIosSimulatorArm64` | BUILD SUCCESSFUL (下記ヘッダ確認のため) |

`allTests` は初回 `SDK location not found` で停止した。これは worktree に `local.properties` が引き継がれない環境事情であり (`cross/local-development-setup.md`「git worktree で作業するとき」)、`ANDROID_HOME` を与えて再実行し上記の結果を得た。成果物側の欠陥ではない。

### `@Throws` 契約が override 除去後も保たれること (実物確認)

生成 ObjC ヘッダ `KsDialogsKmp.framework/Headers/KsDialogsKmp.h` を読み、ViewModel 経路 4 本が引き続き失敗を NSError として運ぶことを確認した。

- `show(viewModel:placement:completionHandler:)` / `show(viewModel:placement:completionHandler_:)` / `start(viewModel:placement:action:completionHandler:)` は completion の第 2 引数に `NSError * _Nullable` を持つ
- 非 suspend の Toast 経路は `- (BOOL)showViewModel:... error:(NSError * _Nullable * _Nullable)error` で、ヘッダの doc に `This method converts instances of DialogException to errors.` が残る
- message 経路には error 引数が付かない (Skill の経路表「なし」と一致)

commonMain の宣言 (`KsDialogs.kt:28` / `KsLoading.kt:43,88` / `KsToast.kt:51`) は変更されておらず、iosMain の override 3 箇所からのみ `@Throws` が外れている。`androidMain` の override は `@Throws` を保持したままで、Android 側は影響を受けていない。

### KMP Skill の内容整合

- `skills/{en,ja}/ksdialogs-kmp/references/ios-host.md:115` の追記は、源泉 concept `kasane/concepts/kmp/api/ios-host-integration.md:104` + 直後の段落と意味が一致する。en / ja は語順以外の意味差なし。
- 追記位置は既存の「Add `@Throws` at the Swift boundary」節末で、見出しは増減していない。spec Requirement「KMP Skill の共有コード側と iOS ホスト側」が「経路ごとの宣言の有無の表など詳細は `references/ios-host.md` に置く」と定めており、SKILL.md 側を触っていないことは spec と整合する。
- 閉世界性: 追記に `kasane/`・ADR 番号・change-id・`KsDialogsInteropBridge` / `KsDialogsInteropResultType` は無く、concept が持つ YouTrack URL も落としてある (外部解説への送客をしない規約どおり)。Skill ルート外への相対リンクも追加していない。
- 経路表の内容は現行実装と一致 (`@Throws` は commonMain の 4 宣言のみ、message 経路は無宣言)。
- 追記が使う識別子 `KsLoading` / `KsToast` は同 Skill の SKILL.md 能力マップと `references/view-models.md` に既出で、初出の未説明語ではない。
- 版数の書き方「Kotlin 2.4.x (2.5.0 で修正)」は SKILL.md Setup の「Use Kotlin 2.4.10 … The supported consumer Kotlin range has not yet been finalized.」と矛盾しない。

### 機械検査 (自分で再実行)

| 検査 | 結果 |
|---|---|
| concepts 網羅 (6-①) | `concepts coverage OK` (UNCOVERED / DELETED なし) |
| en/ja 節構成一致 (6-②) | `en/ja heading structure OK` |
| コードブロック byte 一致 (6-③) | `code blocks byte-identical` |
| frontmatter (6-④) | `frontmatter OK` |
| 閉世界性・機械面 grep (6-⑤) | `kasane/` / `ADR-NNNN` / interop 名 いずれも 0 件。Skill ルート外相対リンク 0 件 |
| 内部リンク解決 (6-⑥) | `All internal links resolve` (対象 70 ファイル) |
| local-path-lint / identity-lint (6-⑦) | いずれも違反 0 件 |
| 配信識別子の表記ゆれ (6-⑧) | 0 件 |
| API 名網羅 (3e、報告のみ) | KMP の報告トークンはすべて `user-skill-api-listing.md` の現行除外リストに載っている既決分。concept 追記による新規未掲載名は発生していない |
| `doc-structure-lint.py` (concept) | `構造 lint: 違反なし` (箇条書き 1 項目 200 字以内・ネスト段数とも適合) |
| `comment-policy-lint.py` (kmp 2 ファイル) | 禁止 0 件 |

### manifest の整合

- `version: 3`、必須キー (`version` / `concepts` / `targets` / `excluded` / `readmes`) すべて存在、型も規範どおり。
- `concepts` 11 件のハッシュを実ファイルの SHA-256 と全件突き合わせ、不一致 0 件。`kmp/api/ios-host-integration.md` は `1fc2cd2d…f198ce` で現行ファイルと一致する。
- `targets` 33 件のキーは en / ja 双方に実在。`targets` の値と `excluded` のキーはすべて実在 concept を指し、逆に未参照かつ未除外の concept も 0 件。
- `readmes` は 4 枚のまま (追従対象の増減なし)。`excluded` は `cross/reference/reference-repositories.md` の 1 件のまま。
- `lastUpdatedFiles` は今回更新した 2 ファイルのみ、`generatedAt` は更新済み。docs-refresh Step 7 の規則どおり。
- `kmp/api/ios-host-integration.md` を `targets` に持つのは `ksdialogs-kmp/SKILL.md` と `ksdialogs-kmp/references/ios-host.md` の 2 件で、SKILL.md 側は未更新のままハッシュが前進している。ただし上記のとおり spec が「詳細は references へ」と定めており、SKILL.md に反映すべき差分が無いという判断は `tasks.md` に明記されている。Step 7 の「対象の全 Skill ファイルが (この実行で) 処理され検査を通過した concept」の条件は満たすと判定した。

### tasks.md

- 6.4 は `[ ]` のまま。本レビューの再実施待ちという記述と一致し、虚偽チェックはない。
- 追記された「レビュー最終状態」の記述内容 (機械検査の再実行結果・SKILL.md 無変更・manifest 更新項目) は、本レビューで再実行した結果と一致する。
- 足場アーティファクト (proposal / design / specs / ui) は diff 範囲で書き換えられていない。

### deviation

`deviation.md` に記録済みの乖離 (Task 9.1 の全ビルドルート省略を含む合計 8 件) は合意済みの差分として扱い、違反として扱っていない。`[付随修正]` 3 件はいずれも ksn-core の同梱条件 (本務で触る文書・公開 API に触れない・局所的) に収まっている。

## 指摘事項

### 🟡 Minor: 注意点の適用範囲が実機構より狭く、`KsDialogs` の差し替えが抜けている

**該当箇所**: `skills/en/ksdialogs-kmp/references/ios-host.md:115`、`skills/ja/ksdialogs-kmp/references/ios-host.md:115` (源泉は `kasane/concepts/kmp/api/ios-host-integration.md:104`)

**問題点**: 追記は「`iosMain` で `KsLoading` / `KsToast` を実装するとき」に限定して書かれているが、KT-88548 が誤検出するのは「`@Throws` 宣言を持つメンバを中間 source set で override すること」であり、契約 interface の種類には依らない。`KsDialogs.show` も `kmp/ksdialogs-kmp/src/commonMain/kotlin/jp/kamusoft/ksdialogs/kmp/KsDialogs.kt:28` で `@Throws(DialogException::class, CancellationException::class)` を宣言しており、同じ Skill が `skills/{en,ja}/ksdialogs-kmp/SKILL.md:12` と同 `:19` で「テストや DI では `KsDialogs`、`KsLoading`、`KsToast` を注入して差し替えられる」と 3 契約を並べて案内している。`KsDialogs` だけを iosMain で差し替える利用者は、この注意点が自分に当てはまらないと読み、同じ失敗に案内なしで当たる。

なお library 側でこの組み合わせが露見しなかったのは、`KsDialogs` の実装が commonMain の `GatewayKsDialogs` (`DialogGateway.kt:45`) 側にあり、そこでの `@Throws` 付き override は問題なくコンパイルされるため (source set の違いが分岐点であって interface の違いではない) である。

**推奨修正**: 対象を「`KsDialogs` / `KsLoading` / `KsToast` を実装するとき」あるいは「`@Throws` 宣言を持つメンバを override するとき」へ広げる。ただし正本は源泉 concept 側であり、Skill だけを直すと次回の docs-refresh で食い違いが再検出される。concept `kmp/api/ios-host-integration.md` の記述を先に広げてから Skill を追随させる (本 change のスコープ外と判断するなら簡易起票でよい)。実機構の一般性については、上記のとおり実装で source set が分岐点であることまでは確認したが、`KsDialogs` の iosMain 実装での再現そのものは読み取り専用レビューのため実測していない。

### 🟡 Minor: `deviation.md` の Task 9.1 記録が現状と食い違ったまま残っている

**該当箇所**: `kasane/changes/rollout-user-docs/deviation.md` の Task 9.1 の項 (末尾)

**問題点**: 「なお KMP の限定 metadata compile で発見した既存不具合は成功扱いせず、`fix-kmp-iosmain-throws-metadata` に簡易起票し、最新 `review-021.md` の CHANGES_REQUESTED を維持する」と書かれているが、当該 change は既に実装・蒸留・アーカイブ済みで、metadata compile は本レビューで成功を確認している。`tasks.md` は現状へ更新されたのに `deviation.md` だけが旧状態のまま残っており、このまま archive されると「未解決の不具合を抱えたまま完了した change」と読める記録が長命側へ渡る。

**推奨修正**: Task 9.1 の項の末尾を現状へ改める (省略したのは全ビルドルートの全 suite であること、KMP の metadata compile と `allTests` は別 change での修正後に実行して成功していること)。乖離そのもの (全ビルド省略) は合意済みなので、書き換えるのは事実関係の後段だけでよい。

### 🔵 Suggestion: 完了判定に使った metadata compile の実行結果が本 change の証跡に残っていない

**該当箇所**: `kasane/changes/rollout-user-docs/` (`evidence/` なし)

**問題点**: `kasane/lessons/inbox/kmp-completion-must-run-hierarchical-metadata-compile.md` のルール文は「実装報告または evidence に当該 task の実行結果が書かれていること」で遵守を判定すると定めている。修正自体は別 change (アーカイブ済み) の証跡に残っているが、本 change の完了判定でも同じ実行が必要だったことは `review-021.md` の経緯から読み取るしかない。

**推奨修正**: 本レビューの「確認済み事項」表を実行結果の記録として扱うか、`tasks.md` の 9.1 行に実行したコマンドと結果 (96 tests / 0 failures を含む) を 1 行足す。未昇格の捕捉に基づく提案なので、対応は任意。

## アクションプラン

1. (優先度: 中) `deviation.md` の Task 9.1 の後段を現状へ更新する — 誤った記録が archive へ渡るのを防ぐため、蒸留の前に行う。
2. (優先度: 中) 注意点の適用範囲を `KsDialogs` を含む形へ広げる。concept を先に直し、Skill (en / ja) と `skills/.manifest.json` の該当ハッシュを追随させる。本 change のスコープ外と判断する場合は簡易起票する。
3. (優先度: 低) 完了判定に使った KMP の metadata compile / `allTests` の結果を `tasks.md` 9.1 か証跡に 1 行残す。

いずれも本 change の APPROVED を妨げない。1 と 2 は蒸留 (`ksn-distill`) の前に処理することを勧める。
