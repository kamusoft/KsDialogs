# Deviation: fix-release-published-wait

- exploration.md「決定事項」の「再実行の分岐 (枠ごとの状態照会) は変わらない」 → review-001 Major の修正で、再実行時に PUBLISHING を引き継ぐ枠は枠の step 内で待たず、NuGet push の後の「まとめて待つ」step へ回す形にした (待ちの位置が NuGet push の後ろへ移る)。帰結: 再実行で「一方が PUBLISHING・他方が再 upload」の組み合わせでは、引き継ぎ枠の PUBLISHED 確認より前に他方の release 要求 (取り消せない操作) が走る。初回経路 (2 枠の release を要求してから待つ) と同じ性質で、オーナーが B 案として決定済みのため露出の新規性は無い。理由: 再実行でも公開待ちを 1 本・並行・上限 90 分に揃えるため (review-002 Minor、2026-09-10)
