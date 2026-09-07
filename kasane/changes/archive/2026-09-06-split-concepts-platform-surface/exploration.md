# Exploration: split-concepts-platform-surface

(2026-09-05 に `refine-docs-refresh-api-token-extraction` から改名。rollout-user-docs の review-022 / 023 / second-opinion-code-001 は旧 ID で本 change を指す)

## 課題 / 動機

`rollout-user-docs` のクロスモデル最終レビューで、docs-refresh の API 名網羅検査が公開 API ではない token を多数候補へ含め、長命規約の除外リストを肥大させていることを発見した。

- framework / 標準型 (`Bool`、`AnyObject`、`TimeInterval`、`LazyColumn` など)
- ファイル名 (`build.gradle.kts`)
- concept 内の case ID・検証 metadata・数式断片 (`C05`、`C19`、`approvedBy`、`approvedDiff`、`A.min` など)
- 本当に判断が必要な公開名・内部名と同じ報告へ混在し、オーナー仕分けと長命除外リストの可読性を下げる
- 発見時の詳細は `kasane/changes/archive/2026-09-05-rollout-user-docs/second-opinion-code-001.md` にある

### 探索で分かった現状 (2026-09-05)

- 検査スクリプト `.agents/skills/docs-refresh/scripts/api-coverage-check.py` は KsSettingsView と完全同一。判定材料は concept 本文と Skill 本文だけで、platform も公開 API の実在も知らない
- 現行の候補 (約 250 token) の内訳: ① 他 platform の公開名 ≈ 6 割 / ② framework・標準型 ≈ 2 割強 / ③ 非 API token (case ID・数式・ファイル名・例示型名) ≈ 1 割弱 / ④ 実判断が要る名前 (`LoadingCoordinator`、`DialogViewRegistry.Shared`、`localSwiftPackage`、`IMauiInitializeService`) ≈ 1 割弱
- 除外リスト約 100 行のうち ④ は 10 行程度。KsSettingsView は同じ検査器で 7 行
- 根本原因は検査器ではなく concepts の構造: `core/api/` 8 本 (1,304 行) に 4 platform の公開名・コード例・framework 注意が同居し、`ios/`・`android/` に concept が無い。`rules.md` のカテゴリ定義 (`<platform>/api/`) には中身が追いついていない

## 検討した選択肢 (却下案と理由を含む)

検査器側の案:

- A: 抽出ルールに除外パターンを足す — ① に効かず framework 型は列挙が追いつかない。却下
- B: 対象 platform のソースの public 宣言と突き合わせる — 3 言語の宣言抽出で中規模、抽出漏れが公開 API の見逃しになる。concept 側の構造的ノイズ源は残る。却下 (当初の推奨だったが、オーナーの指摘で concept 構造の是正へ転換)
- C: concept の識別子に platform 印を付ける — 記述である concept に検査都合の印を入れる。却下
- D: 検査器は変えず除外リストだけ圧縮 — 報告のノイズは残る。却下

concept 側の案:

- a: core = 契約 (観察可能な挙動と保証)、`<platform>/api/` = 公開面 (名前・署名・コード例・framework 注意) — **採用**
- b: platform ごとに完全な写し、core は薄い索引 — 挙動の改訂が 4 か所へ波及。却下
- c: core はそのまま、`<platform>/api/` に名前の対応表だけ足す — 根本を変えない。却下

## 決定事項

- KsSettingsView と同じく concepts を「core = platform 非依存の契約」「`<platform>/api/` = 公開面」に分割する (案 a)。検査スクリプトは変えず KsSettingsView と同一を保つ
- `ios/api/`・`android/api/` を新設、`maui/api/`・`kmp/api/` を拡充。KMP は Native 委譲のため薄い concept でよい
- 順序は rollout-user-docs の完了後 (Skill 再生成の二重化を避ける)
- 論点 2: 独立 change として進める (ロードマップの新フェーズにも ksn-concept 単独作業にもしない)。phase-2-docs-rollout の agenda TODO に申し送りを追記済み (2026-09-05)

## ADR 候補

- 作成済み: cross/ADR-0014 (proposed、オーナーがドラフト確認済み 2026-09-05) — 上記の分割原則

## 未決の論点

- ③ の非 API token (`C05`・`approvedBy`・`A.min`・`build.gradle.kts`) は分割後も core / platform 側に残る。検査器の最小パターン追加で補うか、除外リストの「機械検査由来」1 行に畳むか
- 現行除外リストの移行方法 (分割後に再仕分けし、無効になった行を落とす)
- false positive 削減を固定する fixture / 回帰検査の要否 (検査器を変えないなら不要の可能性)
- handbook `user-skill-api-listing.md` の「除外リストを更新しても検査結果から名前が消えるわけではない」の記述は維持 (検査器を変えないため)

## UI 素材

なし

## 変更級の推奨: L

- 触る能力: concepts 8 本 (約 1,300 行) の再構成と ios / android concept の新設、manifest `targets` の組み替え、Skill 5 本 × en/ja の再生成、handbook 除外リストの再仕分け
- 公開 API 変更: なし (文書のみ)。ただし利用者向け成果物 `skills/` が全面的に書き換わる
- 可逆性: 分割自体は戻せるが、Skill 再生成と除外リスト再仕分けを伴うため実質高コスト
- UI: なし
- 迷ったら 1 段上の原則と、design.md の Decision 節 (分割単位・core に残す範囲の判定基準) が要ることから L
