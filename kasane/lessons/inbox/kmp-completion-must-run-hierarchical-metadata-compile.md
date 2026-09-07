---
scope: impl
kind: pain
severity: normal
count: 1
first-seen: 2026-09-05
last-seen: 2026-09-05
evidence:
  - fix-kmp-iosmain-throws-metadata (add-kmp-loading-toast-throws の完了判定は target 本体の compile と `allTests` で行われ、階層化 source set の metadata compile (`compileIosMainKotlinMetadata`) を走らせていなかった。同 task だけが Kotlin 2.4.10 の既知バグ KT-88548 で失敗する状態が 3 日間残り、rollout-user-docs の review-012 が Sample consumer の `:shared:compileCommonMainKotlinMetadata` で発見した)
---

## ルール文

KMP の Kotlin ソースを変更した change の完了判定では、target 本体の compile と `allTests` に加えて、変更した中間 source set の metadata compile (`compileIosMainKotlinMetadata` / `compileNativeMainKotlinMetadata` 等) か、consumer 側 (`samples/kmp`) の `:shared:compileCommonMainKotlinMetadata` を `--rerun-tasks` 付きで実行し、結果を報告・証跡に載せる。metadata compile は target 本体とは別の compiler 経路 (commonizer / Native CLI) で走り、IDE import と consumer の `build` だけが通る道なので、target 単体の成功は代わりにならない。守れたかは、実装報告または evidence に当該 task の実行結果が書かれていることで判定する。

## 経緯

- 2026-09-05 fix-kmp-iosmain-throws-metadata: 直前の change で追加した `@Throws` の override が metadata compile だけを壊していた

target compile・全テスト・framework link・Sample の Android / iOS ビルドはすべて緑で、通常 consumer の metadata 経路を踏むレビューが無ければ公開後に利用者が最初に踏む不具合だった。CI への組み込みは package-distribution phase-4 (verification-ci) の論点に載せた。
