# レビュー結果: generalize-kmp-iosmain-throws-note (001 回目)

**日付**: 2026-09-05
**判定**: APPROVED

## サマリー

concept の注意点を「iosMain で `KsLoading` / `KsToast` を実装するとき」から「commonMain の interface が `@Throws` を宣言したメンバを iosMain で override するとき」へ一般化した文書のみの変更で、コード差分はない。一般化の主張はレビュー側で独立に再現して裏を取った — `KsDialogs` を iosMain で実装し `show` の override に `@Throws` を書くと `compileIosMainKotlinMetadata` が同じメッセージで失敗し、同じ実装を commonMain へ移すと成功する。concept・skills (en/ja)・manifest の 3 者は整合しており、機械検査 (構造 lint・docs-refresh 5 本・local-path / identity lint・planned-manifest 突き合わせ) と KMP 全件テストはすべて合格。指摘は探索メモの支持根拠 1 文の事実誤りが 1 件 (Minor) で、成果物として出す concept / skills の記述には影響しない。

## 照合した規約

| 文書 | 適用のきっかけ | 結果 |
|---|---|---|
| `kasane/handbook/cross/comment-policy.md` | 常時 (コメント構文を持つファイル) | 該当なし — コード差分ゼロ。レビュー用の一時 Kotlin ファイルは検証後に削除済み |
| `kasane/handbook/cross/user-skill-api-listing.md` | `skills/**` を更新するとき | 適合 — 節ごとに照合。「簡潔でも網羅」: 新出 API 名なし (`KsDialogs` / `KsLoading` / `KsToast` は既掲載)。「現行の除外リスト」: api-coverage-check が KMP × `kmp/api/ios-host-integration.md` で報告する 4 名 (`ConfirmContent` / `SharedConfirmViewModel` / `build.gradle.kts` / `localSwiftPackage`) は既存の確定除外で、本変更で増減なし。「コード例のコメント」: コードブロック差分なし (code-block-parity 合格) |
| `kasane/handbook/cross/test-execution.md` | 変更の完了判定・テスト結果の報告 | 適合 — 絞り込みなしの `kmp/ ./gradlew allTests --rerun-tasks` を実行し件数を確認 (下記) |
| `kasane/handbook/cross/local-development-setup.md` | worktree での Gradle ルート起動 | 適用 — 「git worktree で作業するとき」の手順どおり `kmp/` と `android/` へ `local.properties` を元 checkout から複製して `SDK location not found` を解消した (VCS 管理外のため `git status` には現れない) |
| `kasane/handbook/cross/sample-parity.md` / `runtime-behavior-verification.md` / `aiforms-origin-reference.md` | — | 非該当 (`samples/` を触らない・実行時挙動の不具合ではない・移植作業ではない) |

参照した長命層: `kasane/concepts/kmp/api/ios-host-integration.md` (変更対象)、`kasane/decisions/kmp/0001-swift-interop-plain-suspend.md` (`@Throws` による NSError 化と 2026-09-05 の現行照合行)。`kasane/lessons/code-review.md` は不在のため重点観点・指摘しないことの適用なし。

## 実行した検証

### ビルドとテスト

| 実行 | 結果 |
|---|---|
| `kmp/ ./gradlew :ksdialogs-kmp:compileIosMainKotlinMetadata` (作業ツリーそのまま) | 成功 (`e:` 行 0 件) |
| `kmp/ ./gradlew allTests --rerun-tasks` | BUILD SUCCESSFUL / **96 tests / 0 failures / 0 errors / 0 skipped** (iosSimulatorArm64Test 49 + testAndroidHostTest 47)。`iosX64Test` は host 側 arch 不一致で SKIPPED (既知・規約どおり) |

### 一般化の主張の再現 (レビュー側で独立実施)

- **肯定**: `kmp/ksdialogs-kmp/src/iosMain/.../TmpReviewIosThrows.kt` に `KsDialogs` 実装を置き、`show` の override に `@Throws(DialogException::class, CancellationException::class)` を書いて `compileIosMainKotlinMetadata --rerun-tasks` を実行 → `Member overrides different '@Throws' filter from 'interface KsDialogs : Any'.` で失敗。探索メモの記録と同一メッセージ
- **対照 (commonMain は対象外)**: 同じ実装を `commonMain` へ移して同タスクを実行 → 終了コード 0・`e:` 行 0 件。`public` クラスでも通ることを確認しており、concept の「commonMain の override は対象外で、`@Throws` を書いても通る」は library 内部の `internal class GatewayKsDialogs` (`kmp/ksdialogs-kmp/src/commonMain/kotlin/jp/kamusoft/ksdialogs/kmp/DialogGateway.kt:49`) だけに依存した主張ではない
- 一時ファイルは 2 件とも trash で削除済み。`git status` は対象 5 ファイルのみ

### 機械検査

| 検査 | 結果 |
|---|---|
| `scripts/doc-structure-lint.py --paths kasane/concepts/kmp/api/ios-host-integration.md` | 違反なし (追記した箇条書き 1 項目は 200 字上限内) |
| docs-refresh `concepts-coverage-check.py` / `heading-parity-check.py` / `code-block-parity-check.py` / `frontmatter-check.py` / `link-resolution-check.py` | 全件 OK |
| `scripts/local-path-lint.py` / `scripts/identity-lint.py` (対象 4 ファイル) | 違反なし |
| `planned-manifest.py` と `skills/.manifest.json` の突き合わせ | `version` / `concepts` / `targets` / `excluded` / `readmes` すべて一致。concept ハッシュ `06d2353c…8523` は現在のファイルの sha256 と一致 |

### 文書の内容確認

- **en/ja 一致**: 両版とも「iosMain で commonMain interface の `@Throws` 宣言メンバを override するとき」「3 契約いずれでも同じ」「ジェネリックな `KsDialogs.show` でも失敗」「commonMain の override は対象外」の 4 点を同じ順で述べており、過不足なし
- **閉世界性**: 変更後の `skills/{en,ja}/ksdialogs-kmp/references/ios-host.md` に `kasane/` 参照・ADR 番号・YouTrack URL・`KT-` 課題番号のいずれも含まれない (grep で確認)。Kotlin 版数と修正版数だけを述べる形になっており規律を守っている
- **残存する狭い表現**: `skills/` と `kasane/concepts/` を横断 grep したが、旧来の「`KsLoading` / `KsToast` 限定」の表現が残っている生きた文書はない (`kasane/concepts/log.md` の該当行は append-only の履歴、`kasane/decisions/kmp/0001-…md:41` の現行照合行は library の実在 override 2 件を述べた事実記述で、どちらも書き換え対象ではない)

## 指摘事項

### [🟡 Minor] 探索メモの支持根拠に事実誤り — `DialogGateway.present` は `@Throws` を宣言していない

**該当箇所**: `exploration.md:13`

**問題点**: 「`IosDialogGateway.present` は `@Throws` 宣言を持つ `DialogGateway.present` の override だが `@Throws` を書いていない (fix-kmp の回避と同形)」と書かれているが、`kmp/ksdialogs-kmp/src/commonMain/kotlin/jp/kamusoft/ksdialogs/kmp/DialogGateway.kt:21` の `suspend fun present` に `@Throws` は付いていない。kmp モジュール全体を grep しても `@Throws` は commonMain の `KsDialogs.show` / `KsLoading` 2 箇所 / `KsToast.show` / `GatewayKsDialogs.show` と androidMain の gateway 3 箇所だけで、`present` 系には 1 つもない。したがって `IosDialogGateway.present` が `@Throws` を書いていないのは KT-88548 の回避ではなく、継承すべき宣言がそもそも存在しないためであり、「fix-kmp の回避と同形」は成り立たない (`AndroidDialogGateway.present` も同じ理由で書いていない)。

この誤りは concept / skills の記述には波及していない (一般化の根拠は同メモ 15〜23 行の肯定再現であり、レビュー側でも再現できている) が、探索メモは蒸留時に読まれる証跡として archive に残る。Kotlin 2.5.0 で回避を撤回する段になったとき、この 1 文を根拠に `IosDialogGateway.present` へ `@Throws` を書き戻す判断が生まれると、継承元に filter がない override として別種のコンパイルエラーになる。

**推奨修正**: 13 行目を実態に合わせる。たとえば「iOS の既定エントリ (`Dialog.ios.kt`) は commonMain の `GatewayKsDialogs` に iOS の委譲面 (`IosDialogGateway`) を差し込む構成で、委譲面の `DialogGateway.present` は `@Throws` を宣言していない (`@Throws` は公開契約側の `KsDialogs` / `KsLoading` / `KsToast` にだけ付く)」とし、「fix-kmp の回避と同形」の断りを削る。fix-kmp が回避を入れた `IosLoadingGateway` / `IosToastGateway` は `KsLoading` / `KsToast` の実装であって委譲面ではない点も、区別して書くと後段の混同を防げる。

### [🔵 Suggestion] 「interface の宣言が commonMain にあることが条件」は必要条件として未検証

**該当箇所**: `exploration.md:24`

**問題点**: 「`@Throws` 宣言を持つ commonMain interface メンバの iosMain override 全般に当たる (…interface の宣言が commonMain にあることが条件)」の括弧内は、iosMain で宣言した interface を iosMain で override する対照実験がないまま必要条件として書かれている。取れている証跡は「commonMain 宣言 × iosMain override は失敗する」「commonMain 宣言 × commonMain override は通る」の 2 点であり、宣言側が commonMain であることが失敗の必要条件かは示されていない。

concept 本文は適用範囲を絞る言い方 (「commonMain の interface が `@Throws` を宣言したメンバを iosMain で override するとき」) に留めており、この未検証の必要条件に依存していないため実害はない。

**推奨修正**: 括弧内を「本 change で確認したのは commonMain 宣言 × iosMain override の組み合わせ」といった観測範囲の記述へ弱める。または不要なら削る。

### [🔵 Suggestion] concept 側の「3 契約」に先行する定義がない

**該当箇所**: `kasane/concepts/kmp/api/ios-host-integration.md:106`

**問題点**: 追記した「これは 3 契約のどれを実装しても同じで」の「3 契約」は、直前の箇条書き (104 行) が `KsDialogs` / `KsLoading` / `KsToast` を例示として並べているだけで、文書内に「3 契約」という呼称の定義がない。また 104 行が適用範囲を「`@Throws` を宣言したメンバの override 全般」へ広げた直後に、106 行が「3 契約」と数を限定して読める形になっており、一般化の意図がわずかに後退して見える。

skills 側 (en: "with any of these contracts" / ja:「いずれの契約でも同じで」) は数を持ち出さない書き方になっており、この点では concept より明快である。

**推奨修正**: 106 行の「3 契約のどれを実装しても同じで」を「どの契約を実装しても同じで」などに揃える。文意は変わらず、104 行の一般化と整合する。

## アクションプラン

1. 🟡 `exploration.md:13` の `DialogGateway.present` に関する記述を実態へ修正する (蒸留前に直すのが望ましい — このメモが archive される証跡になるため)
2. 🔵 `exploration.md:24` の括弧内を観測範囲の記述へ弱める (1 と同時に対応できる)
3. 🔵 `kasane/concepts/kmp/api/ios-host-integration.md:106` の「3 契約」を数を持ち出さない表現へ揃える。ただし修正すると concept ハッシュが変わるため、`skills/.manifest.json` の `concepts` エントリ・`generatedAt` の更新まで含めて docs-refresh の経路で行う (skills 側は文言変更不要)

いずれも Critical / Major ではなく、成果物として利用者へ出る concept・skills・manifest の内容は現状のまま正しい。

## 備考

- 本レビューの検証で `kmp/local.properties` と `android/local.properties` を元 checkout から複製した (handbook `local-development-setup.md` の worktree 手順)。VCS 管理外のため `git status` には現れず、worktree 削除で消える
- 再現に使った一時 Kotlin ファイル 2 件は削除済みで、作業ツリーの差分は対象 5 ファイルのみ
