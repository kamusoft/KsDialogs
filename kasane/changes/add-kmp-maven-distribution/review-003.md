# レビュー結果: add-kmp-maven-distribution (003 回目)

**日付**: 2026-09-09
**判定**: APPROVED

## サマリー

review-002 の唯一の Minor (`evidence/publish-to-maven-local.txt` に残っていた「URL の違いは SwiftPM 連携メタデータ以外の成果物に影響しない」という旧主張) は修正されている。書き直した段落は、実証手段 (root publication の POM / `.module` の突き合わせ) と対象外の面 (klib / aar は既定 URL で発行が完了せず未確認)、および本ファイルの検算結果が `file://` の tag 付きローカル clone による発行物のものであることを分けて書いており、`evidence/swiftpm-reference-derivation.txt` と `deviation.md` の記述と整合する。`kasane/lessons/process.md` L-002 の判定基準 (主張の直後に実証手段と対象外の面が書かれていること) を満たす。

evidence 7 本と deviation.md を L-002 の観点で通読した結果、残る食い違いは 1 点だけで、実証範囲の逸脱ではなく実証済みの面の中での言い方の精度 (`.module` を「一致」「影響しない」と書いた 2 箇所が、同じ証跡の「完全同一にはならない」と噛み合っていない) — 低優先度の Minor 1 件として記録する。ローカル絶対パス・個体特定値は 0 件。

## 照合した規約

- `kasane/handbook/cross/comment-policy.md` (always) — 本サイクルの修正はソースコメントを含まないため適用箇所なし
- ksn-core `references/paths.md` — 成果物内のパス表記 (change 相対 / リポジトリ相対、`<scratch>` `<repo>` プレースホルダ) の確認
- ksn-core `references/evidence.md` — 証跡の置き場 (`evidence/` 限定) とプレースホルダ語彙の確認
- `kasane/lessons/process.md` L-002 (互換・無影響の主張は実証範囲に限定し、カバーしない面を分けて明記する) を主要な判定軸として適用。`kasane/lessons/code-review.md` は不在
- 実装コード・テスト・ビルドの再検証は範囲外 (review-002 で解消確認済み)。今回はビルド / テストを走らせていない

## 指摘事項

### [🟡 Minor / 低優先度・判定はブロックしない] `.module` を「一致」「影響しない」と書いた 2 箇所が、証跡の実測 (完全同一にはならない) と噛み合っていない

**該当箇所**: `evidence/publish-to-maven-local.txt:12-14`、`deviation.md:6`

**問題点**: 書き直した段落は「URL の値が**他の成果物に影響しない**ことは root publication の POM / `.module` の突き合わせまでしか実証していない」と書き、`deviation.md` の該当項目も「実証したのは root の POM / `.module` の**一致**と、SwiftPM 連携メタデータの URL・`packageName` が URL に応じて変わること」と書いている。しかし参照先の `evidence/swiftpm-reference-derivation.txt:89-92` の実測は「POM: 差分なし」「`.module`: 差分は SwiftPM 連携メタデータのファイル項目 (`swiftPMDependenciesMetadata`) の size と 4 種のチェックサムだけで、他の variant とファイル項目は同一。URL 文字列の長さがメタデータのサイズに出るため、`.module` は完全同一にはならない」である。

つまり `.module` については「URL に影響されなかった」ではなく「URL 依存の差が SwiftPM 連携メタデータのファイル項目に限られていた」が実測で、2 箇所の要約は結論を一段強く言っている。実証範囲の逸脱 (L-002 が塞ぐ型) ではなく、実証した面の中での精度の問題であり、差の中身は「URL 依存であることを既に明示している SwiftPM 連携メタデータ」に閉じているため、下流の判断を誤らせる余地は小さい。ただし両ファイルとも蒸留・アーカイブされて phase-8 (smoke) の入力になり、phase-8 で公開 URL 発行物と `file://` 発行物の `.module` を突き合わせると「一致するはず」と読んだ側は差分に面食らう。

**推奨修正**: どちらも 1 節の追記で閉じる。`deviation.md:6` は「root の POM の一致と、`.module` の差が SwiftPM 連携メタデータのファイル項目 (size / チェックサム) に限られること」、`evidence/publish-to-maven-local.txt:12-14` は「URL の値の影響として実証できたのは root publication の POM (差分なし) と `.module` (差は SwiftPM 連携メタデータのファイル項目のみ) までである」といった形。蒸留時にまとめて直す扱いでも実害はない。

## アクションプラン

1. 上記 Minor は低優先度。単独で修正サイクルを回す必要はなく、蒸留 (ADR / 申し送りへの転記時) に合わせて 2 箇所の言い回しを実測に揃えれば足りる
2. review-002 の Suggestion 2 件 (`@Throws` 検査の型比較・configuration cache と `whenReady`) は対処しない方針の申し送りとして確定済み。本レビューでは指摘対象にしていない

## 確認して問題がなかった観点

- **修正箇所の実証範囲との整合 (確認事項 1)**: `evidence/publish-to-maven-local.txt:11-16` は (a) URL 上書きの理由 = 既定 URL では配信リポジトリに当該 tag が無く cinterop klib の生成が完了しないため、(b) 実証は root publication の POM / `.module` の突き合わせまで、(c) klib / aar は既定 URL で発行が完了せず未確認、(d) 本ファイルの検算はすべて `file://` の tag 付きローカル clone を指した発行物のもの、(e) 公開 URL での全 publication の同一性は phase-8 (smoke) に委ねる、を分けて書いている。(a) は `evidence/swiftpm-reference-derivation.txt:56-78`、(b)(c) は同 `:80-98`、(e) は同 `:97-98` と `deviation.md:6` に対応する。旧主張 (「SwiftPM 連携メタデータ以外の成果物に影響しない」) は残っていない
- **本ファイルの記述が実際にすべて `file://` ケースであること**: 1〜5 節の検算はいずれも冒頭の `case-4` (`-Pksdialogs.swiftPackageUrl=file://<scratch>/spm-clone`) の 1 回の発行から取られており、(d) の限定と一致する。既定 URL で発行できるのは root publication だけ (`swiftpm-reference-derivation.txt:56-78`) なので、この限定なしには成立しない節 (aar / klib を含む 1 節・3 節・5 節) を限定の内側に置けている
- **evidence 7 本の相互整合 (確認事項 2、上記 Minor 以外)**:
  - `snapshot-central-publish-guard.txt` の 10 経路 + 集約 `publish` + 除外タスク + ローカル発行の結果は `deviation.md:7`「残り」の記述 (集約 `publish` だけ SNAPSHOT 診断と認証情報未解決の同時報告) と一致し、`--offline`・認証情報プロパティなしという実行条件も本文に明記されている (ネットワーク不通を理由にした主張はない)
  - `publish-signing.txt` の「鍵なしで Sign タスク SKIPPED・`.asc` 0 件」は `publish-to-maven-local.txt` の SNAPSHOT ケースの記述および `version-injection.txt` の case-1 と矛盾しない。鍵ありのケースは検証用の一時鍵と明記され、鍵の本文・指紋・利用者 ID は写していないと断ってある
  - `kmp-test-counts.txt` の 153 件 (androidHostTest 78 + iosSimulatorArm64 75) と +2 件の内訳は `kasane/handbook/cross/test-execution.md` の更新値 (153 件・2026-09-09) と一致する
  - `version-injection.txt` の 2 節は beta.1 の実測を `publish-to-maven-local.txt` / `android-version-alignment.txt` に委譲している。委譲先が `file://` ケースである旨の限定は委譲先 (上記 (d)) が持っており、version 導出自体は `-Pversion` 由来で URL 上書きと独立、かつ既定 URL の case-3 (`swiftpm-reference-derivation.txt:35-49`) でも root publication の version が `0.1.0-beta.1` になることが取れているため、無限定の主張になってはいない
  - `android-version-alignment.txt` の POM 共通部 (url / inceptionYear / licenses / developers / scm) の一致は、URL 上書きで発行した kmp 側の POM を含むが、POM が URL に依存しないことは `swiftpm-reference-derivation.txt:89` (POM: 差分なし) で実測されているため、`file://` ケース由来であることが結論を弱めない
- **未実証の面の書き分け**: 「公開 URL + 実在 tag での全 publication の突き合わせは phase-8」「SPM tag を先に push してから Maven へ発行する順序は phase-9 の release workflow の要件」という 2 つの持ち越しが、`swiftpm-reference-derivation.txt:96-101` と `deviation.md:6` と `publish-to-maven-local.txt:16` で同じ内容に揃っている
- **ローカル絶対パス・個体特定値 (確認事項 3)**: `python3 scripts/local-path-lint.py` (追跡ファイル全体) に加え、evidence/ と deviation.md が未追跡のため `--paths` で当該ファイルを明示して再実行 — いずれも違反 0 件。`python3 scripts/identity-lint.py --paths kasane/changes/add-kmp-maven-distribution` も違反 0 件。目視でも発行先は `<scratch>/case-N`、SwiftPM のローカル参照は `<repo>/ios`、GnuPG の鍵の利用者 ID は `<検証用の一時鍵>` のプレースホルダになっており、実名パス・ホスト名・メールアドレスは現れない (`swiftpm-reference-derivation.txt:52` の `/Users/` `/Volumes/` `/private/tmp/` は「発行物にこれらが現れないことを確認した」という検査対象の記述で、実パスではない)
- **成果物パスの表記**: 証跡が参照するパスはすべてリポジトリ相対 (`kmp/build.gradle.kts`・`evidence/...`・`kasane/handbook/...`) で、ksn-core `references/paths.md` の規約に沿っている

## 申し送り

- `deviation.md:3` の「ルートへの `alias(libs.plugins.kotlinMultiplatform) apply false`」は依然「オーナー承認待ち」のまま (review-001 / 002 からの持ち越し)。蒸留前に承認の有無を確定させる必要がある
- SNAPSHOT ガードの診断文言が kmp (英語 + 日本語の混在) と android (全文日本語) で割れている件、および集約 `publish` 経由での同時報告の件は、review-002 の申し送りのまま有効
