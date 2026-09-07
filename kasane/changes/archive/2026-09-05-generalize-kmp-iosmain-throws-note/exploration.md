# Exploration: generalize-kmp-iosmain-throws-note

## 課題 / 動機

`rollout-user-docs` の KMP Skill 再レビュー (`../archive/2026-09-05-rollout-user-docs/review-024.md` の Minor) で、源泉 concept `kasane/concepts/kmp/api/ios-host-integration.md` に `fix-kmp-iosmain-throws-metadata` の蒸留で追記した注意点「共有モジュールの iosMain で `KsLoading` / `KsToast` を実装するとき override に `@Throws` を書かない」の適用範囲が、実際の事象より狭い可能性を指摘された。

- 事象 (Kotlin 2.4.x の native 系中間 source set の metadata compile が同一 filter を「異なる filter」と誤検出する、KT-88548) は `@Throws` 宣言を持つ interface メンバの override 全般に当たると見られる
- `KsDialogs.show` も commonMain の interface で `@Throws` 宣言を持ち、KMP Skill (`skills/{en,ja}/ksdialogs-kmp/SKILL.md`) は `KsDialogs` / `KsLoading` / `KsToast` の 3 契約を並べて差し替え可能と案内している。`KsDialogs` だけを iosMain で差し替える利用者は案内なしで同じ失敗に当たり得る
- 簡易起票時点 (2026-09-05) では `KsDialogs` の差し替えで失敗を再現した証跡がなく、オーナー判断で再現の裏取りなしに concept を一般化せず、`rollout-user-docs` は蒸留へ進めた

## 現状の把握 (2026-09-05 探索、Kotlin 2.4.10 / Gradle 9.7.0)

- library の iosMain には `KsDialogs` の直接実装はない。iOS の既定エントリ (`Dialog.ios.kt`) は commonMain の `GatewayKsDialogs` に iOS の委譲面 (`IosDialogGateway`) を差し込む構成で、委譲面の `DialogGateway.present` は `@Throws` を宣言しておらず (`@Throws` は `GatewayKsDialogs.show` 側にある)、iosMain の override も宣言を持たない (継承すべき宣言がないだけで、fix-kmp の回避とは別物。2.5.0 で書き戻す対象ではない)
- commonMain の `GatewayKsDialogs.show` は `@Throws` を書いた override だが通る。事象は native 系中間 source set (iosMain) の metadata compile に限られ、commonMain の override は対象外
- **再現 (肯定)**: library の iosMain に `KsDialogs` を実装する一時ファイルを置き、`show` の override に interface と同じ `@Throws(DialogException::class, CancellationException::class)` を書いて `kmp/` で `./gradlew :ksdialogs-kmp:compileIosMainKotlinMetadata --rerun-tasks` を実行すると、fix-kmp と同じメッセージで失敗する:

  ```text
  e: .../iosMain/kotlin/jp/kamusoft/ksdialogs/kmp/TmpKsDialogsThrowsRepro.kt:9:5 Member overrides different '@Throws' filter from 'interface KsDialogs : Any'.
  Execution failed for task ':ksdialogs-kmp:compileIosMainKotlinMetadata'
  BUILD FAILED in 14s
  ```

  一時ファイルを削除した対照実行は成功 (`e:` 行 0 件)。一時ファイルは探索終了時に削除済みで、ワークツリーに差分は残していない
- ジェネリック (`<R>`) を持つ suspend メンバでも Loading / Toast と同じ事象であり、`@Throws` 宣言を持つ commonMain interface メンバの iosMain override 全般に当たる (interface が commonMain にあることが必要条件と見られる。根拠は fix-kmp の切り分け「iosMain 内で閉じた interface の override は通る」で、本 change では `KsDialogs` についてその対照実験を再実施していない)

## 検討した選択肢

- **A: concept の注意点を「`@Throws` 宣言を持つ interface メンバの override 全般」に広げ、`KsDialogs` を明示する (推奨)**: 事象の実態に合う。利用者向け文書 (KMP Skill en / ja `references/ios-host.md` の `@Throws` 節) は docs-refresh の差分更新で追随する
- **B: `KsDialogs` を 3 つ目として列挙するだけ (一般化しない)**: 実態は override 全般なので、今後 interface が増えると再び漏れる。却下候補
- **C: 見送り (Loading / Toast 限定のまま)**: 再現が取れた以上、利用者が案内なしに当たる状態を残すことになる。却下候補

## 決定事項

- 2026-09-05 review-001 APPROVED (Minor 1: 本メモの委譲面に関する事実誤りを修正 / Suggestion 2: 「条件」表現を未検証と明記、concept の「3 契約」を数を持たない表現へ)

- 2026-09-05 オーナー決定: A 案 (override 全般へ一般化し、`KsDialogs` / `KsLoading` / `KsToast` の 3 契約を例示) を S 級で直接実装する。concept の主文は「commonMain の interface で `@Throws` を宣言したメンバを iosMain で override するとき」とし、3 契約を例示として添える
- 利用者向け文書 (KMP Skill en / ja) は docs-refresh の差分更新で追随する。独立レビューは S 級の規律どおり必須

## ADR 候補 (作成済み: なし / 未起票: なし)

fix-kmp-iosmain-throws-metadata と同じく Kotlin の既知バグに対する文書上の注意であり、公開契約 (kmp/ADR-0001 の `@Throws` による NSError 化) は不変。ADR 級の判断は含まない

## 未決の論点

- KMP Skill en / ja への追随は docs-refresh 経由 (`.agents/skills/docs-refresh/`、manifest の concept ハッシュ更新を含む)
- Kotlin 2.5.0 への更新時に注意点自体を撤回する手順は phase-4 agenda の申し送りと同期 (本 change では触らない)

## UI 素材

なし

## 変更級の推奨: S

理由: concept 1 項目の文言修正と、docs-refresh による KMP Skill 2 ファイル + manifest の追随のみ。コード変更なし・公開 API 変更なし・UI なし・可逆。再現証跡は本メモに記録済み (evidence ファイルは作らない)
