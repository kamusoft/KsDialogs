# Proposal: split-concepts-platform-surface

## Why

利用者向け Skills (`skills/{en,ja}/`、5 Skill) は platform 単位で分かれているのに、その源泉である concepts は `core/api/` の 8 本 (約 1,300 行) が 4 形態 (Native iOS / Native Android / .NET MAUI / KMP) の公開名・署名・コード例・framework 固有の注意を 1 本文に同居させている。`ios/`・`android/` ドメインには concept が 1 本も無く、`concepts/rules.md` が定めるカテゴリ `<platform>/api/` (platform 固有の公開 API・利用例) に中身が追いついていない。

この構造のため、docs-refresh の API 名網羅検査は全 Skill に対して `core/api/` 7 本を源泉として突き合わせ、候補の約 6 割が「他 platform の公開名」になる。初回生成後にオーナーが仕分けた除外リスト (`handbook/cross/user-skill-api-listing.md`) は約 100 行に達し、そのうち掲載可否の実判断が要る名前は 1 割程度。残りは検査器の限界をオーナー判断の形で抱え込んだもので、規範層の可読性を下げ、concept を足すたびに同じノイズが再生産される (rollout-user-docs の相方レビュー Suggestion 8)。姉妹ライブラリ KsSettingsView は同一の検査スクリプトで除外リストが 7 行に収まっており、差は検査器ではなく concepts の構造にある。

探索 ([exploration.md](exploration.md)) で検査器側の改修 4 案を却下し、concepts を「core = platform 非依存の契約 / `<platform>/api/` = 公開面」へ分割する方針を決めた ([cross/ADR-0014](../../decisions/cross/0014-concepts-core-contract-platform-surface.md)、proposed)。

## What Changes

- **concepts の再構成 (ksn-concept モード 3「作庭」の規律で実施)**
  - `core/api/` 8 本を、観察可能な挙動と保証だけを platform 非依存の文で書いた契約に書き直す。形態別テーブル (`| 形態 | ... |`)・platform 固有の識別子・コード例・framework 固有の注意 (Compose の遅延評価スコープ等) を本文から外し、末尾の「形態別の公開面」節から各 platform concept へリンクする
  - `ios/api/`・`android/api/` を新設し、`maui/api/`・`kmp/api/` を拡充する。各 platform に機能単位の公開面 concept (dialog / layout / transition / loading / toast の 5 本を基本形。design.md Decision 1) を置き、core から外した公開名・署名・コード例・注意をそこへ移す
  - layout の「共通ケース表と OS 差の統制」節 (cases.json の読み方・`approvedDiff` 統制) は利用者向けではなく検証機構の記述のため `core/architecture/` へ移し、既定どおり Skill の源泉から外す (design.md Decision 3)
  - 各ドメインの `index.md`・`concepts/log.md`・`rules.md` (配置判断への「契約 / 公開面」の基準追記) を更新する
- **manifest (`skills/.manifest.json`) の `targets` 組み替え**: 各 Skill ファイルの源泉を「core の契約 + 自 platform の api」に変える。33 キーの完成形は design.md Decision 6。KMP Skill の Android ホスト側 references は `android/api/` を、layout / transitions は各 OS 側の面として `ios/api/` `android/api/` の同名 surface を、iOS ホスト側は `kmp/api/ios-host-integration.md` (KMP の Swift 向け公開面を集約) を源泉にする。移行 Skill は cross/ADR-0011 の基準「対応表が触れるもの」の適用として core + `maui/api/` 6 本
- **Skills の再生成**: 源泉 concept が全件変わるため docs-refresh (`--all`) で 5 Skill × en/ja を再生成し、整合性チェック一式を通す。Skill の本数・構成 (references の並び) は変えない
- **除外リストの再仕分け** (ksn-concept の経路で handbook を書く): 分割後の API 名網羅検査の報告を基準に、`handbook/cross/user-skill-api-listing.md` の現行除外リストを組み直す。「対象 Skill 外・機械検査由来」の行は原則消え、残るのは内部層 / 低頻度 / 機械的に導出できる名前の実判断だけになる

影響する能力: concept の配置契約 (`concept-placement`) / 利用者向け Skills の源泉と manifest (`user-skills-manifest`) / Skill の API 掲載基準 (`api-listing-policy`)

## Non-Goals

- **API 名網羅検査スクリプトの改修**: 探索で却下済み (cross/ADR-0014 Alternatives)。KsSettingsView と同一を保つ
- **Skill の本数・分割軸の変更** (KMP Skill の分割、移行 Skill の統合など): cross/ADR-0011 の決定であり、本 change は源泉の構造だけを変える
- **concept が記述する挙動・保証の変更**: 再配置であって意味の改訂ではない。書き直し中に見つかった誤り・実装との乖離は deviation.md に記録して蒸留送りにする (別途 ksn-drift / 修正 change)
- **handbook の掲載除外基準 5 つの改訂**: 基準は維持し、リストの中身だけ組み直す。基準を増減したくなったらオーナー判断で別途
- **KsSettingsView への逆流**: あちらの concepts は既に契約 / 公開面の構造で、本 change に共有すべき道具の変更はない
- **`skills/` の内容改善** (レシピの追加・表現の見直し): 再生成は源泉の追従であり、生成物の品質改善は docs-refresh の通常運用と別 change で行う

## Impact

- 破壊的変更: なし (コード・公開 API は触らない)。利用者向け成果物 `skills/` は全面的に再生成されるが、5 Skill の構成と閉世界性は維持する
- 長命層への波及: concepts 8 本の書き直し + platform concept 18 本の新設 (ios 5 / android 5 / maui 5 / kmp 3。既存の maui 1 本・kmp 1 本は維持し、KMP の Swift 向け公開面は既存 `kmp/api/ios-host-integration.md` に集約) + `core/architecture/` 1 本の新設 / `rules.md`・各 index・log の更新 / handbook `user-skill-api-listing.md` の除外リスト組み直し / cross/ADR-0014 の accepted 昇格 (蒸留時)。cross/ADR-0011 の「残る concepts はすべてどれかの Skill の `targets` に載せる」は新設分にも適用し、`architecture/` 1 本は既定どおり `excluded` に理由つきで載せる
- リスク: (1) 分割中に契約の文と公開面の記述がずれる — 各 platform concept は core の対応節へリンクし、レビューで 1 挙動 = core 1 か所 + platform 4 か所の対応を確認する。(2) 書き直しで公開名を落とす — 分割前後で「全 concept の識別子集合」を採って platform 側に必ず着地したことを機械的に確かめる (specs の Scenario)。(3) Skill 再生成で英日ロックステップや閉世界性が崩れる — docs-refresh の整合性チェック 8 種で検出する
- 前提: rollout-user-docs の完了後に着手する (Skill 再生成の二重化を避ける)。fix-kmp-iosmain-throws-metadata / tidy-ios-presentation-internals は concepts に触れない (exploration.md のみ) ため衝突しない

## 級: L

複数能力横断 (concept 配置・manifest・handbook)・長命層の大規模再構成で覆すコストが高く、design.md の Decision (分割単位・core に残す範囲の判定基準・非 API token の扱い・除外リストの移行) が要るため。

domain: cross
