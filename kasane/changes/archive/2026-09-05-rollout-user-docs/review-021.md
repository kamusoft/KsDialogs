# レビュー結果: rollout-user-docs (021 回目)

**日付**: 2026-09-05
**判定**: CHANGES_REQUESTED

## サマリー

指定された Skill・manifest は、現行の concepts / handbook・公開実装との内容整合、英日等価性、閉世界性、API 掲載基準を満たしている。配信準備中という package 状態の表記はルート README 冒頭だけにあり、KMP Skill の「予定している公開座標」と Kotlin 下限未確定の注記は proposal.md が要求する暫定値の識別として区別できる。一方、KMP の iOS metadata compile が現行ツリーでも 3 件の `@Throws` filter 不一致で失敗するため、ビルド失敗を見逃さないレビューゲートに従い承認できない。

## 照合した規約

- `kasane/handbook/cross/user-skill-api-listing.md` — `skills/**` の API 掲載範囲と確定済み除外
- `kasane/handbook/cross/test-execution.md` — テスト結果の報告と変更完了判定
- cross/ADR-0011 — 利用者向け文書の Agent Skills 化、英日 2 版、5 Skill、閉世界性
- android/ADR-0001 — Compose API の別 artifact 分離
- kmp/ADR-0001〜0005 — Swift interop、Native registry 委譲、Swift package 側の型付き KMP 面

## 確認結果

- `skills/.manifest.json` は version 3、concept 11 件、target 33 件、除外 1 件、README 4 件で、concept のキー集合と SHA-256 が現行ファイルに一致した。
- 指定範囲を含む生成物は、concept 網羅・英日見出し構造・コードブロック byte 一致・frontmatter・内部リンク・ローカル絶対パス・個体情報・配信識別子の検査に合格した。指定 Skill 内に `kasane/`、ADR 番号、`KsDialogsInteropBridge`、`KsDialogsInteropResultType`、Skill ルート外への相対リンクはない。
- Android Skill の座標・minSdk・Compose / Lifecycle / Coroutines / Kotlin の記載は現行 version catalog と公開 API に一致し、en / ja の意味差はない。
- KMP Skill は、共有コード・Android host・iOS host の三側、iOS Setup の前提 1 + 手順 3、型付き Swift 入口、`@Throws` の経路表を公開実装と正本に一致させており、en / ja の意味差はない。
- AiForms migration の対応表は移植元 clone の README・公開型と現行 MAUI 公開面を突き合わせ、合意済み deviation（旧 Toast に message overload が実在しないこと、`IReusableLoading.Hide()` の実署名が `Task` であること）を反映している。

## 指摘事項

### 🟠 Major: KMP の iosMain metadata compile が `@Throws` filter 不一致で失敗する

**該当箇所**: `kmp/ksdialogs-kmp/src/iosMain/kotlin/jp/kamusoft/ksdialogs/kmp/IosLoadingGateway.kt:34`、`kmp/ksdialogs-kmp/src/iosMain/kotlin/jp/kamusoft/ksdialogs/kmp/IosLoadingGateway.kt:57`、`kmp/ksdialogs-kmp/src/iosMain/kotlin/jp/kamusoft/ksdialogs/kmp/IosToastGateway.kt:30`

**問題点**: `kmp/` で `./gradlew :ksdialogs-kmp:compileIosMainKotlinMetadata --rerun-tasks` を実行すると、上記 3 箇所が `Member overrides different '@Throws' filter` で失敗する。KMP Skill は iOS host 統合と Swift boundary の `@Throws` 契約を利用者向け経路として提示しているが、その配布元 metadata を現行ソースから生成できない。別 Kasane change への簡易起票と deviation の記録は問題の所在を明確にしているものの、失敗中のビルドを成功へ変えるものではないため、ksn-review の完了ゲートは通過しない。

**推奨修正**: 簡易起票済みの `fix-kmp-iosmain-throws-metadata` で、公開契約に記載した例外集合を弱めずに interface と iOS override の `@Throws` filter をコンパイラ上も一致させる。上記 metadata compile と KMP Sample の `compileCommonMainKotlinMetadata` を再実行して成功を確認した後、本レビューを再実施する。

## アクションプラン

1. `fix-kmp-iosmain-throws-metadata` で 3 件の annotation filter 不一致を解消する。
2. KMP library の iosMain metadata compile と KMP Sample の commonMain metadata compile を成功させる。
3. 成功結果を入力に、KMP Skill を含む最終独立レビューを再実施する。
