# Exploration: fix-release-published-wait

## 課題 / 動機

add-release-workflow の初回リリース `0.1.0-beta.1` (release run 34449418361) で、Android 枠の release 後の PUBLISHED 待ち (`scripts/release/central-portal.sh wait-published`、上限 `KSR_POLL_TIMEOUT_SECONDS` 既定 1800 秒) が上限に達して publish job が失敗し、同じ run の再実行で整合した (証跡: `../add-release-workflow/evidence/release-pr-and-dispatch.txt`)。上限の 30 分は翻案元 KsSettingsView の実測 (publish 11 分) を根拠にした値で、Central の同期の実測は Android 枠 約 60 分 / KMP 枠 27 分 29 秒。2 枠の待ちは直列 (Android release → 待ち → KMP release → 待ち) で、publish job の timeout は 120 分。

## 検討した選択肢 (却下案と理由を含む)

| 案 | 評価 |
|---|---|
| A: 上限を伸ばすだけ | 直列のまま伸ばすと最悪 2 枠分が足し算になり (90 分 × 2)、job timeout も 200 分級が要る。却下 |
| **B: 2 枠の release を Android → KMP の順に先に要求してから、2 枠の PUBLISHED を並行で待つ + 上限引き上げ** | 最悪でも遅い方 1 枠分。「monorepo の tag は publish 全成功後」の契約と再実行の分岐 (枠ごとの状態照会) は変わらない。step 2 つの組み替えで済む。**採用** |
| C: publish は release 要求の受理までで、公開待ちは反映待ち job (repo1 の POM) に任せる | tag / Release が Maven 未反映のうちに生まれ、同期失敗時に tag だけ残る。ロードマップのゴールと ADR-0024 の改訂が要る。却下 |

## 決定事項

- B を採用 (オーナー、2026-09-10)。release 要求の順序 (Android → KMP) は保ち、待ちだけを並行にする
- PUBLISHED 待ちの上限は 90 分 (今回の最大 60 分に余裕)、publish job の timeout は 150 分 (前段の upload / 検証 / NuGet 約 10 分 + KMP 再ビルド + 待ち 90 分)
- cross/ADR-0024 (proposed) の順 6「Maven release 2 件 (Android → KMP) → published 待ち」はこの形でも成立するため本文は触らない。add-release-workflow の spec「Maven Central の 2 枠の deployment」の「Android → KMP の順で行い、公開 (PUBLISHED) を待つ」は「要求の順序」として読み、待ちの並行化は蒸留時に concepts 側で記述する
- 同梱: handbook release-procedure「失敗したとき」に「Portal の表示名は 2 枠とも `jp.kamusoft-<version>` で見分けられない。枠の区別は Summarize の deployment ID で行う」を 1 文足す (add-release-workflow の申し送り)

## ADR 候補

なし (ADR-0024 の範囲内の運用値と step の組み替え)

## 未決の論点

なし

## UI 素材

なし

## 変更級の推奨: S

spec の改訂を持たず、workflow の step 2 つと運用値 2 つ、handbook 1 文の変更。独立レビューは必須 (S 級でも)。main は保護済みのため、変更は develop → リリース PR の経路で main に入る (次のリリースから効く)
