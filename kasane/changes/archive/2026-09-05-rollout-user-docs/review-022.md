# レビュー結果: rollout-user-docs (022 回目)

**日付**: 2026-09-05
**判定**: NEEDS_DISCUSSION

## サマリー

Toast の既定エントリは4形態の現行実装と一致し、更新履歴・timestamp・manifest hash も整合している。廃止した Sample README への active な参照は、現在記載された除外条件で 0 件になり、handbook の表境界と新規簡易起票の課題設定にも実体上の問題はない。

一方、レビュー対応の過程で承認済み proposal / delta spec と accepted ADR の既存本文・過去の現行照合が直接書き換えられている。これは Kasane の足場凍結・accepted ADR 不変規律に反し、レビュー側から修正文を処方できないため、正規の変更経路をオーナーと決める必要がある。

## 照合した規約

- `kasane/handbook/cross/test-execution.md` — 変更の完了判定と検査結果の報告
- `kasane/handbook/cross/sample-parity.md` — Sample 配下文書の廃止後も維持する4ルートの規範
- `kasane/handbook/cross/user-skill-api-listing.md` — docs-refresh の API 名網羅検査とオーナー仕分け
- ksn-core `references/delta-spec.md` — proposal / design / specs の実装中凍結と deviation の意味論
- ksn-core `references/decisions.md` — accepted ADR の不変性と改訂権限
- ksn-core `references/paths.md` — change 間参照を含む成果物内パスの形式

## 指摘事項

### [🟠 Major] 承認済み proposal と delta spec がレビュー対応で書き換えられている

**該当箇所**: `proposal.md:20`、`specs/repository-docs/spec.md:18`、`deviation.md:8`、`deviation.md:10`

**問題点**: Git 差分では、proposal に Toast concept の事実訂正を追加し、delta spec の残存検査を「archive とロードマップ過去記録を除外」から `kasane/changes/`・`kasane/roadmaps/`・`kasane/concepts/log.md`・`kasane/lessons/` 全体の除外へ変更している。どちらも実装・レビュー中は凍結する足場であり、後から判明した事実訂正と合意済み差分はすでに deviation に記録されているため、足場側へ逆流させてはならない。特に spec の変更は、承認時の契約と現在レビューしている契約を同じパスで上書きし、deviation が持つ差分証跡を曖昧にする。

**推奨修正**: 承認済み足場をどの版へ戻し、達成不能だった元 Scenario を deviation だけで扱うかをオーナーと決める。仕様そのものを改める必要があると判断する場合は、実装レビューから直接直さず、仕様策定の権限を持つフローへ戻す。

### [🟠 Major] accepted ADR の Decision と過去の現行照合を直接改変している

**該当箇所**: `kasane/decisions/cross/0006-samples-aggregated-consumer-boundary.md:14`、`kasane/decisions/cross/0006-samples-aggregated-consumer-boundary.md:38`、`kasane/decisions/cross/0007-sample-parity-demo-item-unit.md:20`、`kasane/decisions/cross/0007-sample-parity-demo-item-unit.md:37`、`kasane/decisions/cross/0007-sample-parity-demo-item-unit.md:39`

**問題点**: 2件とも `status: accepted` だが、既存 Decision から README の存在・写像関係を削り、過去日付の現行照合から当時存在した README への記録も削っている。2026-09-05 の現行照合を末尾へ追加すること自体は適切だが、accepted 後の本文と過去時点の記録は不変であり、`[付随修正]` や「決定内容は不変」という説明では既存行の変更権限を得られない。履歴として残る参照と現在有効な参照を区別する必要はあるが、その区別のために過去記録を現在形へ書き換えると、ADR の「その時点の判断の記録」という役割を壊す。

**推奨修正**: 既存 accepted 本文・過去の現行照合を保持したまま、現況を表す追記だけで済むかをまず判断する。Decision の関係自体を改める必要があるなら、ksn-explore / ksn-agenda / ksn-propose または長命層の改訂権限を持つフローで supersede の要否をオーナー判断に掛ける。

### [🟡 Minor] 残存検査が active な lessons まで一括除外する

**該当箇所**: `specs/repository-docs/spec.md:18`、`tasks.md:58`

**問題点**: 現在残っている該当文字列は `kasane/lessons/inbox/` の過去観測だけなので、履歴除外後の走査は実測 0 件になる。しかし `kasane/lessons/` には inbox だけでなく、エージェントが現在従う昇格済みルール (`kasane/lessons/<scope>.md`) も置かれる。ディレクトリ全体を除外すると、将来 active な作業規律が廃止 README を指しても検査が成功する。

**推奨修正**: 残存検査の可変な実行条件では、履歴として許容する lessons の場所だけを絞って除外し、昇格済みルールは検査対象に残す。delta spec の扱いは Major 1 のオーナー判断に従う。

### [🟡 Minor] 簡易起票が別 change を同一 change 相対形式で参照している

**該当箇所**: `kasane/changes/refine-docs-refresh-api-token-extraction/exploration.md:11`

**問題点**: `../rollout-user-docs/second-opinion-code-001.md` は実体へ辿れるが、change 相対パスを使えるのは同じ change 内の成果物だけである。別 change の記録はリポジトリ相対で書く必要があり、archive 後の解決規則も現在の表記には適用できない。課題自体は実測で再現し、標準型・case ID・ファイル名を公開 API 候補として大量に報告するため、簡易起票の妥当性に問題はない。

**推奨修正**: 参照を `kasane/changes/rollout-user-docs/second-opinion-code-001.md` にする。参照先 change の archive 後は、Kasane の change-id 解決規則で追跡する。

## 確認済み事項

- 履歴・作業記録の除外をそのままコマンド化した残存検査は 0 件。全残存は `kasane/changes/`、`kasane/roadmaps/`、`kasane/concepts/log.md`、`kasane/lessons/inbox/`、archive 内の記録に限られる
- `samples/` 配下の `README*.md` は 0 件。公開ドキュメント面はルート2枚・skills索引2枚・維持対象の `android/layout-case-fixtures/README.md` の計5枚
- `Toast.shared` / `Toast.instance` / `Toast.Instance` / commonMain `Toast.instance` を現行 Swift・Kotlin・C# 実装と直接照合し、`toast-semantics.md:29-34` の表と一致
- `toast-semantics.md` と `user-skill-api-listing.md` の timestamp は 2026-09-05。`kasane/concepts/log.md` に事実訂正・除外リスト確定・ADR追随の追記がある
- `skills/.manifest.json` の concept 11件を SHA-256 再計算し 11/11 一致。Toast concept の hash も一致
- `user-skill-api-listing.md:142-144` は表の後に空行があり、後続段落が表へ吸収されない
- docs-refresh の concepts coverage・英日 heading parity・code block byte parity・frontmatter・内部リンク、local path / identity lint、対象長命文書の構造 lint、`git diff --check` はすべて成功
- `api-coverage-check.py` は `Bool`、`AnyObject`、`LazyColumn`、`build.gradle.kts`、`C05`、`approvedDiff` 等を未掲載 API 候補として実際に報告し、簡易起票の問題設定を再現できる
- 製品コード・テストの全 suite は proposal の合意済み例外と `deviation.md:11` に従い省略した。今回のレビューでは製品コード・テストを変更していない

## アクションプラン

1. proposal / delta spec の凍結版と、残存参照 Scenario の扱いをオーナー判断で確定する
2. cross/ADR-0006・0007 の既存行変更を取り消すか、正規の ADR 改訂フローへ送るかを決める
3. 残存検査の lessons 除外を履歴部分へ限定する
4. 簡易起票の別 change 参照をリポジトリ相対へ直す
5. 上記反映後、同じ機械検査と独立レビューを再実施する
