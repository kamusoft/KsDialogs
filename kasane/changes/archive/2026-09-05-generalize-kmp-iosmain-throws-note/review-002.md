# レビュー結果: generalize-kmp-iosmain-throws-note (002 回目)

**日付**: 2026-09-05
**判定**: APPROVED

## サマリー

review-001 (APPROVED) の Minor 1 件と Suggestion 2 件への対応の再確認のみを範囲とする限定レビュー。3 件とも指摘どおりに反映されており、事実関係・文書間の整合・manifest ハッシュ・機械検査のいずれも問題なし。新規の Critical / Major はなく、残るのは対応の副作用として生じた文言の過剰なヘッジが 1 件 (Suggestion) のみ。

## 照合した規約

限定再確認のため、対応 3 件に直接かかる規約に絞って照合した。

| 文書 | 適用のきっかけ | 結果 |
|---|---|---|
| `kasane/handbook/cross/user-skill-api-listing.md` | `skills/**` を含む差分 | 適合 — 本再確認で `skills/{en,ja}/ksdialogs-kmp/references/ios-host.md` の本文は変更されておらず (review-001 の対応 3 は「skills 側は文言変更不要」)、変更は `skills/.manifest.json` の `generatedAt` と concept ハッシュのみ |
| `ksn-core` 育つ文書の構造規約 (`scripts/doc-structure-lint.py`) | concept を書き換えたため | 適合 — 該当 concept は違反なし |

`kasane/lessons/code-review.md` は不在のため重点観点・指摘しないことの適用なし。コード差分ゼロのため `comment-policy.md` は該当なし、ビルド・テストは再実行不要の指示に従い未実施 (review-001 で 96 tests / 0 failures を実測済み、以後コード差分は発生していない)。

## 確認結果

### 1. review-001 Minor — `DialogGateway.present` の記述 (対応済み)

`exploration.md:13` は「委譲面の `DialogGateway.present` は `@Throws` を宣言しておらず (`@Throws` は `GatewayKsDialogs.show` 側にある)、iosMain の override も宣言を持たない (継承すべき宣言がないだけで、fix-kmp の回避とは別物。2.5.0 で書き戻す対象ではない)」に改まっている。実装と照合して全項目が事実と一致する。

- `kmp/ksdialogs-kmp/src/commonMain/kotlin/jp/kamusoft/ksdialogs/kmp/DialogGateway.kt:21` の `suspend fun present` に `@Throws` なし
- 同ファイル `:47` の `GatewayKsDialogs.show` に `@Throws(DialogException::class, CancellationException::class)` あり
- `kmp/ksdialogs-kmp/src/iosMain/kotlin/jp/kamusoft/ksdialogs/kmp/IosDialogGateway.kt` に `@Throws` なし、`Dialog.ios.kt:15` は `GatewayKsDialogs(IosDialogGateway(...))` の構成

「fix-kmp の回避とは別物。2.5.0 で書き戻す対象ではない」と明示されているため、2.5.0 での書き戻し対象と誤読される余地は解消されている。

### 2. review-001 Suggestion 1 — 「条件」表現 (対応済み)

`exploration.md:24` の括弧内は「…interface が commonMain にあることが必要条件と見られるが、対照実験としては未検証」となり、未検証である旨が明記された。併せて追加された支持根拠「fix-kmp の切り分けでは iosMain 内で閉じた interface の override は通っており」も裏を取り、事実と一致することを確認した (`kasane/changes/archive/2026-09-05-fix-kmp-iosmain-throws-metadata/exploration.md:16` および同 `evidence/metadata-compile-recovery.txt:14`)。ヘッジの強さについてのみ下記 Suggestion を残す。

### 3. review-001 Suggestion 2 — concept の「3 契約」(対応済み)

`kasane/concepts/kmp/api/ios-host-integration.md:106` は「これはいずれの契約を実装しても同じで」に改まり、数を持つ表現は concept・skills の 3 ファイルすべてから消えた (`3 契約` / `三契約` の grep で 0 件)。skills 側の対応表現 (en: "with any of these contracts" / ja:「いずれの契約でも同じで」) と整合し、同 `:104` の一般化した適用範囲とも矛盾しない。

### 4. manifest の concept ハッシュ (一致)

`shasum -a 256 kasane/concepts/kmp/api/ios-host-integration.md` = `0c5b084ff121f9935a175a5f3dafb3e5e305b022dbfae97694b23aaa9db534ef` で、`skills/.manifest.json` の `concepts["kmp/api/ios-host-integration.md"]` と一致。`planned-manifest.py` の出力と `version` / `concepts` / `targets` / `excluded` / `readmes` すべて一致し、`generatedAt` は concept 再編集後の時刻 (`2026-09-05T09:12:17Z`) に更新済み。`lastUpdatedFiles` は本変更で更新した en / ja 2 ファイルを指しており正しい (前回値と同一のため差分には現れない)。

### 5. 機械検査と差分範囲 (問題なし)

| 検査 | 結果 |
|---|---|
| `scripts/doc-structure-lint.py --paths kasane/concepts/kmp/api/ios-host-integration.md` | 違反なし |
| docs-refresh `concepts-coverage-check` / `heading-parity-check` / `code-block-parity-check` / `frontmatter-check` / `link-resolution-check` | 全件 OK |
| `scripts/local-path-lint.py` / `scripts/identity-lint.py` (対象 4 ファイル) | exit 0・違反なし |
| `git status --porcelain` | `exploration.md` / `kasane/concepts/kmp/api/ios-host-integration.md` / `skills/.manifest.json` / `skills/en/ksdialogs-kmp/references/ios-host.md` / `skills/ja/ksdialogs-kmp/references/ios-host.md` の 5 件が modified、`review-001.md` が untracked。想定外のファイルなし (本ファイルを加えて 7 件) |

## 指摘事項

### [🔵 Suggestion] 「対照実験としては未検証」は自ら挙げた証跡と食い違う

**該当箇所**: `exploration.md:24`

**問題点**: 同じ括弧内で「fix-kmp の切り分けでは iosMain 内で閉じた interface の override は通っており」と証跡を挙げながら、直後に「対照実験としては未検証」と結んでいる。挙げられた fix-kmp の切り分け (アーカイブ済み evidence で確認) はまさに「宣言側が iosMain の場合」の対照実験そのものなので、「未検証」は取れている証跡より弱い言い方になっている。review-001 Suggestion 1 の趣旨 (未検証を明記) は満たしているが、将来の読者が済んでいる対照実験を再実施しかねない。

**推奨修正**: 「本 change では再現していないが、fix-kmp の切り分けで確認済み」といった、証跡の所在と本 change での未再現を分けて書く形へ。concept 本文はこの必要条件に依存していないため、成果物への影響はない。

## アクションプラン

1. 🔵 `exploration.md:24` のヘッジを証跡の所在に合わせて言い換える (任意。蒸留前にまとめて直せる)

review-001 の Minor 1 件・Suggestion 2 件はすべて解消済みで、利用者へ出る concept・skills・manifest の内容に残課題はない。

## 備考

- `exploration.md` の書き換えは review-001 の推奨修正とオーナー判断に基づくもので、無断の足場書き換えではない
- `scripts/doc-structure-lint.py` の scope (`kasane/config.yaml` の `lint.doc-structure.scope`) は `kasane/concepts` / `kasane/handbook` / `kasane/roadmaps` で `kasane/changes` を含まないため、`exploration.md` の長い箇条書き (`:13` 317 字ほか) は lint 違反として数えられない。リポジトリ全体の構造 lint は 262 件 / 43 ファイルの既知 backlog があり (ksn-drift へ見送り済み)、本変更で concept 側の違反は増えていない
- ビルド・テストは指示により再実行していない。review-001 以降の差分は文書 3 ファイルのみでコードに影響しない
