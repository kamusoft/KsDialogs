# Tasks: adopt-docs-refresh

## 1. 一式の配置

- [x] 1.1 `../KsSettingsView/.agents/skills/docs-refresh/` (commit `33ce94e`、2026-09-02。作業ツリーが clean であることを確認してからコピーする) の SKILL.md・`references/` 2 本・`scripts/` 8 本を `.agents/skills/docs-refresh/` へコピーする。`scripts/` は無改変。manifest 不在時の停止挙動は翻案元の SKILL.md / スクリプトごと持ち込む (→ Requirement: docs-refresh 一式の配置と起動経路 / 初期生成前の停止)
- [x] 1.2 `.claude/skills/docs-refresh` → `../../.agents/skills/docs-refresh` の symlink を作る (→ Requirement: docs-refresh 一式の配置と起動経路)

## 2. SKILL.md の KsDialogs 向け差し替え

- [x] 2.1 frontmatter (description) と冒頭の目的・対象定義のプロジェクト名を KsDialogs に置き換え、自動発動禁止の文言を維持する。cross/ADR-0022 へのリンクは「ロードマップ package-distribution phase-1 の決定事項 (archive 後は `kasane/roadmaps/archive/*-package-distribution/` で解決)」への参照に置き換え、蒸留時に翻案 ADR を起票したら差し替える旨を注記する (→ Requirement: docs-refresh 一式の配置と起動経路)
- [x] 2.2 追従対象の表を 5 Skill (`ksdialogs-{ios,android,maui,kmp,aiforms-migration}`) × 2 言語に書き換える。KMP の 1 本構成 (SKILL.md = 共有コード側 + 3 側 Setup + 最小コード、ホスト側レシピは references/) と移行 Skill の構成 (SKILL.md + references/api-mapping.md、Toast は対応先なし) を明記する。README 4 枚の表と「platform / Sample ディレクトリに README を置かない」の根拠参照 (cross/ADR-0023) を KsDialogs の phase-2 踏襲決定への参照に直す (→ Requirement: 追従対象の規範)
- [x] 2.3 移行 Skill の源泉規則 (targets は新 API 側 concepts のみ・旧 API 側は追従対象外・初期生成時の一度限りの書き起こしは reference-repositories.md で解決するローカル clone 前提) を manifest / 3b の節に書く (→ Requirement: 移行 Skill の源泉は新 API 側の concepts のみ)
- [x] 2.4 3c 節に excluded の初期値 (`cross/reference/reference-repositories.md` + 理由) と architecture カテゴリの既定 (除外候補として提示・自動除外しない) を書く (→ Requirement: excluded の初期値と architecture カテゴリの既定)
- [x] 2.5 3d の取得元表を 4 行 (version catalog / wrapper 2 本 + 食い違い報告 / ios/Package.swift / MAUI csproj の TFM・OS 下限・MAUI 本体下限) に置き換え、KMP の catalog 共有前提 (`kmp/settings.gradle.kts` の `from(files("../android/gradle/libs.versions.toml"))`) を先に確認し外れていたら報告して停止する行、「module の build.gradle.kts は取得元にしない」注記と Step 4 提示例の固有名 (concept パス・Skill 名・版の例) を KsDialogs のものに直す (→ Requirement: コード正の機械チェック (ツール最低バージョン) の取得元)
- [x] 2.6 3e の仕分け基準の参照先 (handbook `user-skill-api-listing.md`) を「phase-2 で KsDialogs 固有の内容として起こす」注記に置き換え、過去実績の件数 (iOS 33 件等) の記述を落とす (proposal の Non-Goals「3e の仕分け基準の handbook 起こし」に対応。Requirement の紐付けなし)
- [x] 2.7 6-⑤ を閉世界性・機械面の漏れ検査に書き換える: grep (`kasane/` 参照・ADR 番号・`KsDialogsInteropBridge`・`KsDialogsInteropResultType`) と、skills/ 配下の相対 markdown リンクが Skill ルートの外へ解決されないことを見る python 断片 (scripts/ には手を入れず SKILL.md 内の断片として置く。README は対象外・外部 URL は検査しない)。`docs/` / `openspec` の検査を外す。Guardrails の廃止概念行は「機械面 (`KsDialogsInteropBridge` / `KsDialogsInteropResultType`) は現存する public ABI だが利用者向け導線に載せない」に書き換え、「廃止」「復活させない」の表現は使わない (→ Requirement: 閉世界性と機械面の漏れ検査)
- [x] 2.8 6-⑧ の参照先を cross/ADR-0005 + android/ADR-0001 に、grep パターンを両 ADR の識別子 (SwiftPM `KsDialogs` / Maven `jp.kamusoft:ksdialogs`・`ksdialogs-compose`・`ksdialogs-kmp` / Kotlin `jp.kamusoft.ksdialogs`・`.compose`・`.kmp` / NuGet `KsDialogs.Maui` / namespace `KsDialogs`) から導いた KsDialogs 版 (ブランド名の大小文字・区切りの崩れ / `com.kamusoft` / Maven 座標へのブランド名混入 / NuGet ID の崩れ) に置き換え、Maven 座標の注記 (artifactId 3 本・公開未導入の旨) を直す (→ Requirement: 配信識別子の表記ゆれ検査)
- [x] 2.9 6-⑦ の注記 (`lint.identity.scope` に `skills` 登載済み) を確認したうえで、SKILL.md と prompt 2 本の全文に翻案元固有語の残留 grep (`KsSettingsView` / `kssettingsview` / `KsColor` / `KsFont` / `ADR-0022` / `ADR-0023` / `phase-1[0-2]` / `user-skill-api-listing` / `public-identifiers` / `docs/` / `openspec`) を掛け、許容箇所 (冒頭の翻案元出典の注記 1 箇所) 以外に残っていないことを確認する (→ Requirement: 追従対象の規範 / 規約記述と lint 範囲)

## 3. prompt 2 本の差し替え

- [x] 3.1 `references/prompt-skill.md` のプロジェクト名・変更理由の例・規約⑤を KsDialogs 向け (閉世界性: `kasane/` 内部文書・ADR 番号・機械面への参照を書かない) に置き換える (→ Requirement: 閉世界性と機械面の漏れ検査)
- [x] 3.2 `references/prompt-readme.md` のプロジェクト名・検出差分の例・README 種別の確認事項・規約⑤を KsDialogs 向けに置き換える (→ Requirement: 閉世界性と機械面の漏れ検査)

## 4. 規約記述の更新

- [x] 4.1 AGENTS.md の開発ハーネス節に運用宣言 2 行 (skills/ は派生物で知識参照先にしない / 追従は docs-refresh 経由のみ・自動発動禁止・初期生成は承認済み change・本体は `.agents/skills/docs-refresh/SKILL.md`) を足し、CLAUDE.md を写しとして揃える。ツール手順は書かない (→ Requirement: 規約記述と lint 範囲)
- [x] 4.2 `kasane/config.yaml` の `context` に skills/ の位置づけ 1 文を足し、`lint.identity.scope` に `skills` を追加、`lint.comment-policy.exclude` の `docs` を `skills` に置き換える (→ Requirement: 規約記述と lint 範囲)

## 5. 検証 (テスト相当)

- [x] 5.1 `scripts/` 8 本が翻案元と byte 一致すること (`cmp`) と、2 つの入口が同じ実体を指すことを確認する (→ Requirement: docs-refresh 一式の配置と起動経路)
- [x] 5.2 manifest 不在の現状で `concepts-coverage-check.py` を実行し、書き換えなしでエラー終了することを確認する。SKILL.md の manifest 検証手順に 4 ケース (不在 / parse エラー / version ≠ 3 / 必須キー欠落・型不正) が列挙され、停止案内が「初期生成は承認済み change で」へ誘導することを本文照読で確認する (→ Requirement: 初期生成前の停止)
- [x] 5.3 3d の取得元 4 行を SKILL.md の抽出方法どおりに実際に読み、全項目が空でない値として得られること、catalog 共有前提の確認行が現状 (共有あり) で通ることを確認する。各取得元の変化がどの突合先 (README 群 / 各 SKILL.md 導入節) を要追従にするかの対応が本文に書かれていることを照読する (→ Requirement: コード正の機械チェック (ツール最低バージョン) の取得元)
- [x] 5.4 6-⑤ / 6-⑧ の grep パターンと相対リンク検査の断片を一時 fixture (scratchpad) の正例・負例で実走し、誤表記 (2 ADR の識別子ごとに誤例を用意)・内部用語・機械面・Skill ルート外への相対リンクが検出され、正しい識別子・同一 Skill 内の `../SKILL.md` リンク・配布座標の URL が素通りすることを確認する (→ Requirement: 閉世界性と機械面の漏れ検査 / 配信識別子の表記ゆれ検査)
- [x] 5.5 一時 fixture で `skills/{en,ja}/<name>/` 構成と manifest v3 を組み、8 検査スクリプトが KsDialogs の `kasane/concepts` に対して動く (網羅検査で `excluded` の初期値と 9 本の targets が整合する) ことを確認する。fixture は trash で片付ける (→ Requirement: excluded の初期値と architecture カテゴリの既定 / 移行 Skill の源泉は新 API 側の concepts のみ)
- [x] 5.6 更新後の AGENTS.md / CLAUDE.md の宣言が一致し、config.yaml が YAML として parse でき `lint.identity.scope` に `kasane` と `skills` を含むことを確認する。`python3 scripts/identity-lint.py` と `local-path-lint.py` を通常モードで実行し違反 0 件を確認する (→ Requirement: 規約記述と lint 範囲)
- [x] 5.7 phase-1 agenda の決定事項 5 件と、Requirement「docs-refresh 一式の配置と起動経路」に列挙した翻案元の契約項目を 1 件ずつ SKILL.md と照合し、反映漏れ・欠落がないことを確認する (→ Requirement: 追従対象の規範 / 移行 Skill の源泉は新 API 側の concepts のみ / excluded の初期値と architecture カテゴリの既定 / コード正の機械チェック (ツール最低バージョン) の取得元)

## 6. 完了判定

- [x] 6.1 テスト実行規約 (`kasane/handbook/cross/test-execution.md`) の全ビルドルート全件実行は、本 change が製品コード・テストに触れないため合意済み例外として省略する (proposal の Impact。オーナー確認済み 2026-09-04)。実装報告に「製品コード・テスト無変更のため全件実行は合意済み例外で省略」と明記する
