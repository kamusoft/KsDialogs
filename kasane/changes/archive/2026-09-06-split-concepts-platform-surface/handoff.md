# Handoff: split-concepts-platform-surface (実装フェーズの引き継ぎ)

ksn-orchestrator のセッションをコンテキストクリアで区切るための引き継ぎメモ (足場。archive 時に change と共に移動する)。
再開時は `/ksn-orchestrator kasane/changes/split-concepts-platform-surface` を起動し、本ファイル → tasks.md → 最新の review / second-opinion を読む。

## 現在地 (2026-09-05 時点)

- 級 L / domain cross。tasks.md **グループ 1〜4 と 5.1 は完了** (チェック済み)。コミット `4e81fd8` に全部入っている (push は未実施)
- 中間の独立レビューは **収束済み**: review-001 (CHANGES_REQUESTED) → review-002 (APPROVED) → review-003 (APPROVED)、相方 codex は second-opinion-code-001 (CR) → 002 (CR) → 003 (CR) → **004 (APPROVED)**。各ファイル末尾の「突き合わせ結果」が採否の記録
- 修正サイクルは 3 周使った (上限 `orchestrator.max-review-cycles: 3`)。3 周目はオーケストレーターの直接修正 (fixture の 6 トークン復帰) で相方が確認
- 相方セッションの label: `so-code-split-concepts-platform-surface` (codex、`counterpart-bridge.sh send --label` で継続可。最終レビューでは新規 label で start し直してよい)

## 止まっている場所: tasks 5.2 = docs-refresh `--all` の Step 4 (更新方針の承認)

docs-refresh (`.agents/skills/docs-refresh/SKILL.md`) の Step 1〜3 は実行済みで結果は次のとおり。**Step 4 の承認 (オーナーの発話) を得てから Step 5 へ進む**:

- Step 1: 器 `ksn-implementer` 配置済み。concept 30 本、skills/en・ja 各 33 ファイル
- Step 2: manifest 検証 OK (version 3 / concepts 30 / targets 33 / excluded 2 / readmes 4)
- 3c 網羅検査: `concepts coverage OK`
- 3d コード正の突合: 全項目一致 (AGP 9.3.0 / Kotlin 2.4.10 / minSdk 24 / compileSdk 36 / Gradle 9.7.0 両ルート / Swift tools 6.3・iOS 17 / net10.0・SupportedOSPlatformVersion 17.0 / 24.0・Maui.Controls 10.0.1)。README 2 枚と Skill 導入節に差分なし
- 3e API 名網羅検査: 31 行の候補 (分割前の源泉で書かれた**現行 Skill** を分割後の源泉で照らした結果。再生成で大半が解消される見込み。全ファイル再生成なので個別承認は不要、再生成サブエージェントへ「変更理由の補足」として渡す。本仕分けは 6.1 / 6.2)
- 更新対象: Skill 33 × en/ja = 66 + README 4 = **70 ファイル**。Skill の構成 (references のファイル名・本数) は変えない

オーナーへ提示した選択肢: **A 全件進める (推奨)** / B 一部 (spec は全件再生成を SHALL としているので deviation 記録が要る) / C 中止。回答待ち。

## 承認後の進め方

1. docs-refresh Step 5: Skill 単位 (en/ja 同時) で `ksn-implementer` に委譲 (テンプレは `.agents/skills/docs-refresh/references/prompt-skill.md` / `prompt-readme.md`)。最大 3 並列のバッチ直列。5 Skill + README 4 (ペア 2)
2. Step 6: 整合性チェック 8 種 (`.agents/skills/docs-refresh/scripts/` の 6 本 + 閉世界性・表記ゆれ・ツール最低バージョン) を**予定 manifest** に対して実行 (`DOCS_REFRESH_MANIFEST` はコマンド行にインライン指定)。Step 7 で manifest を最後に書く (`generatedAt` / `lastUpdatedFiles` / ハッシュ最終化 = tasks 5.4)
3. tasks 5.3: 再生成前後の `skills/{en,ja}/` のファイル集合一致 (33 × 2)
4. グループ 6 (ksn-implementer に委譲。handbook の書き込みは ksn-concept の経路で index / log / timestamp / 構造 lint を伴う):
   - 6.1 Skill 側の負の検査: `verification/forbidden-tokens.json` を **全文** (バッククォート span + fenced code block から api-coverage-check.py と同じ識別子規則で抽出、完全一致) で突き合わせる。単位の定義と一致時の扱いは `verification/baseline.md` 末尾「Skill 側の負の検査 (task 6.1) の突き合わせ単位」が正。結果は `verification/api-coverage-after.txt`
   - 6.2 3e 候補の仕分け (掲載漏れ → docs-refresh の項目指定で Skill 修正 / 実判断の除外 → 旧リストの理由を引き継ぐ / 非 API token)。**オーナー判断が要る候補は ask-style で提示**
   - 6.3 `handbook/cross/user-skill-api-listing.md` の除外リストをゼロから書き直す (design Decision 4)。既知の申し送り: 除外行 :101 / :109 / :119 / :132 (`A.min` `C05` `C19` `approvedBy` `approvedDiff`) は architecture 移動で原則消える
   - 6.4 docs-refresh SKILL.md の 3e 注記 (誤検出源 ①) を「分割後は原則発生しない」旨に改める
5. グループ 7: 7.1 cross/ADR-0014 (proposed) の Decision 節に design Decision 1〜3 が反映されているか確認 (**申し送り**: rules.md に書いた「KMP の公開面 = commonMain + Android ホスト側 + Swift 向け公開面の 3 側」の定義が ADR-0014 の Decision 節にも要るか確認。proposed なので本文を直接改訂してよい) / 7.2 lint 一式 + `git diff --check` / 7.3 deviation.md の最終確認
6. 最終レビュー (ksn-reviewer、新規番号 review-004〜) + 相方セカンドオピニオン (L 級。`second-opinion-code-005〜`) → verify (ksn-verifier、`verify-001.md`) → 完了報告 → ksn-distill を促す

## オーケストレーターの裁定 (ワーカーへ再度伝える必要があるもの)

- 共通概念名は `verification/core-contract-check.md` の確定 14 種 (KMP の公開面は 3 側の読み方)。core にはこれ以外の識別子を置かない
- Scenario ID 接頭辞 / 見本ラベルはバッククォートを外す。数式は fenced code block
- 契約と実装の乖離は直さず deviation.md に記録 (蒸留送り)。契約の穴 (未規定の挙動) も直さない
- 付随修正はワーカーの報告から orchestrator が deviation.md へ `[付随修正]` として転記する (これまで 6 件記録済み)
- `scripts/doc-structure-lint.py` は **`--paths` にディレクトリを渡すと違反を拾わない**。全ファイルを `find` で列挙して渡す (index / log / rules は SKIP 対象なので結果から除く)。scope モードとの件数差はこの挙動由来 (review-002 Suggestion)。local-path / identity lint は `git grep` ベースで未追跡ファイルを見ないため、新設ファイルは `--paths` で明示
- 構造 lint の残存 11 件 (`maui/api/di-registration.md` 10・`cross/reference/reference-repositories.md` 1) は既存本文由来で本 change では触らない

## 完了報告に載せる予定のスコープ外の発見 (オーナー判断待ち。実装フロー中は未起票)

- **契約の穴 3 件** (baseline から存在、layout-semantics): ① 非有限値の一般規則と表の個別規則が ±Infinity で衝突し適用順が未定 ② 「いつの値が使われるか」の 3 文が整合せず show 後・初回レイアウト前の書き換えの効果が確定しない ③ 中身の View 階層の複数箇所に添付が競合したときの勝敗が SwiftUI 以外で未規定。見立て: 簡易起票 (ksn-explore) または ksn-drift
- **iOS の SwiftUI modifier の命名が非対称** (`.ksDialogOptions` / `.ksDialogPlacement` に対して演出だけ `.dialogTransition`、`ios/Sources/KsDialogs/SwiftUI/DialogAttributeAttachment.swift:68`)。公開 API なので変更は破壊的。事実として ios/api/transition-surface.md に注記済み。見立て: オーナー確認
- **multi-display-semantics.md の Scenario ID** `PB-MD-01`〜`03` が 2 ケースに 3 ID で対応が読めない (baseline から)。見立て: 確認 → 対応表の追加を起票
- **house 用語 (面 / 器 / 中身 / スロット) の対応表**を core に 1 枚置く案 (レビュアー所感)。見立て: オーナー判断 (別 change)
- **`doc-structure-lint.py` の `--paths` が SKIP_NAMES を迂回する / ディレクトリ指定で違反を拾わない** (標準装備側)。見立て: ksn-update / Kasane 側へ
- **`concepts/log.md` が 26,000 字超** (lint 対象外に守られているだけ)。見立て: ksn-drift の棚卸し
- `RegisterForLoading` / `RegisterForToast` が既存 `maui/api/di-registration.md` に無かった (本 change では MAUI の loading / toast surface 側に書き、リンクで接続)

## lessons の捕捉状況

ksn-lesson の即時捕捉対象 (オーナーの指摘 / 的外れ却下 / spec 不備停止 / 進め方の評価) は本セッションでは発生なし。相方由来で採用した見逃し 2 件 (1 引数 factory の主張拡張・Compose 経路の混同) は「ホスト側レビューが言い換え判定で新規主張を取りこぼした」型として ksn-distill の絞り口で扱う候補。

## 完了 (2026-09-06)

上記の承認待ちはオーナーの「A で進めて」で解消し、tasks 5.2〜7.3 を完了した。最終レビュー review-004 (APPROVED) / second-opinion-code-005 (2 ターン目で APPROVED) / verify-001 (VALID)。以降は ksn-distill へ。本ファイルの「現在地」節は履歴として残す (「7.2 の構造 lint 違反なし」は zsh の偽陽性で、正しい結果は deviation.md 末尾を参照)。
