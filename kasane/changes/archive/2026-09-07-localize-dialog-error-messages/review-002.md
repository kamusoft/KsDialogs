# レビュー結果: localize-dialog-error-messages (002 回目)

**日付**: 2026-09-07
**判定**: APPROVED

## サマリー

修正サイクル 2 周目の差分 2 件 (`BridgeContentSupplyTests.cs` の検査力の回復・`tasks.md` のチェック状態) だけを見た。前者は 1 周目の Minor を実際に解消しており、期待値は maui-binding デルタの対応表から機械的に組み立てた文字列と byte 一致、変異検査 (書式・効果句をそれぞれ書き換える) で 2 本とも落ちることまで確認した。後者はチェックボックスの反転のみで文面に差分はなく、`tasks.md` の未チェックは 0 件になった。製品コードは対応表どおりの文字列リテラル置換だけで、非リテラル行の差分は 0 行。指摘は Suggestion 1 件のみ。

## 照合した規約

| 文書 | 適用のきっかけ |
|---|---|
| `kasane/handbook/cross/comment-policy.md` | always (テストコードにコメントを追加しているため) |
| `kasane/handbook/cross/test-execution.md` | テストの実行・結果の報告・完了判定 (MAUI は `dotnet test`) |
| `csharp-impl-skill` / `dotnet-test-skill` | `domain-skills.maui` の解決結果 (NUnit・`Assert.That`・アサーションの弱体化禁止) |
| `kasane/decisions/cross/0015-diagnostic-messages-english-only.md` | 本 change の根拠 ADR (accepted) |

`kasane/handbook/maui/` は未作成 (index は「まだ規約なし」)。`kasane/lessons/code-review.md` は存在しないため重点観点・除外観点なし。

## 実行した検証

| 検証 | 結果 |
|---|---|
| `maui/` `dotnet test KsDialogs.slnx` | **155 tests / 0 failures** (net10.0-android / net10.0-ios のビルドも通過)。件数は 1 周目と同じで、`test-execution.md` の 155 と一致 |
| 期待値 ⇔ 対応表の機械照合 | 対応表の書式 `Could not create the presentation content. {0}: {1}` に効果句 2 種を埋めて `{1}` の直前までを切り出した文字列と、テストの `Does.StartWith` の連結リテラル 2 本が完全一致 (前後の空白・コロンを含む) |
| 変異検査 (検査力の実証) | `BridgeContentSupply.cs` の書式本文と `CreateOrDiscard` の効果句を一時的に別文言へ書き換えると **2 本とも失敗** (153/155)。復元後は 155/155 で、ファイルの shasum が書き換え前と一致することを確認済み (ツリーに残留なし) |
| `tasks.md` の差分性質 | 削除 29 行 / 追加 29 行がすべて `- [ ] ` → `- [x] ` の置換だけで一致 (文面差分 0)。未チェック項目は 0 件 |
| 足場の凍結 | `proposal.md` / `specs/**` / `exploration.md` / `review-001.md` に差分なし (git status 上、変更は `tasks.md` のみ) |
| 製品コード | `maui/KsDialogs.Maui/` の差分は 9 ファイル 17 挿入 / 17 削除で、**文字列リテラルを含まない差分行は 0 行**。3 つの対応表の英語文言はすべて実装ソースに存在 |
| `scripts/comment-policy-lint.py --advisory` | **禁止 0 件**。要確認 453 件はすべて `samples/` 等の本 change 以前からの既存分で、今回追加した 2 行のコメントは非該当 |
| `scripts/scenario-id-coverage.py --specs .../specs` | 終了コード 0 (tasks 6.2 の受け入れ条件。読み替えは deviation.md に記録済み) |

## 指摘事項

### [🔵 Suggestion] deviation.md の MAUI 追随の内訳が、追随後の assertion の形を書いていない

**該当箇所**: `deviation.md` の 1 項目め (「文言に依存する既存テストの範囲」)

**問題点**: 「`BridgeContentSupplyTests.cs` の**警告本文を見ていた**部分一致 2 件 (`Does.Contain("破棄")` / `Does.Contain("失敗として返")`)」という内訳は、**追随前**にどの assertion が旧文言に依存していたかの説明としては正しく、「残る 2 箇所はテスト自身が投げる例外の文言」も現在のコードと一致している (現ツリーに残る日本語の部分一致は `Does.Contain("中身を作れません")` の 2 箇所)。一方でこの 2 件は最終的に部分一致ではなく、書式本文 + 効果句をまとめて見る `Does.StartWith` に置き換わっており、記録された assertion 名は現ツリーに存在しない。蒸留・アーカイブ後にこの記録から現物を辿る人が、名前で grep しても当たらない。

**推奨修正**: 内訳の文末に「追随後は書式本文 + 効果句を前方一致で見る 2 件になっている」の主旨を 1 句添える (件数と分類は現行のままでよい)。

## 確認して問題が無かった点 (指摘なし)

- **1 周目 Minor の解消**: 警告本文 (対応表の書式 + 効果句) を見る `Does.StartWith` が 2 本入り、「警告が何の失敗かを述べていること」の検査が回復した。実装の書式・効果句を書き換えると必ず落ちることを変異検査で実証済み
- **検査の粒度が過剰でない**: 前方一致なので `{1}` (投げられた例外の描画) から後ろの変動は許容される。例外の型名・スタックの体裁が変わっても落ちない
- **検査対象の分離**: 「警告として出ている文言そのもの」(`Does.StartWith`) と「テスト自身が投げた例外の説明が載ること」(`Does.Contain("中身を作れません")`) が別アサーションに分かれ、それぞれ日本語のコメントとアサーションメッセージで役割が明示されている。書式から `{1}` を落とせば後者だけが落ちるため、後者も空洞ではない
- **新しい訳語の混入なし**: 期待値の 2 文字列は対応表の 3 行 (書式・効果句 2 種) だけから構成され、対応表に無い語は含まれない
- **製品コードの不変**: `maui/KsDialogs.Maui/` の差分は対応表どおりの文字列リテラル置換のみ。今回のサイクルで検査力の回復に合わせて実装を動かした形跡はない (非リテラル差分 0 行)
- **コメント規約**: 追加した 2 行のコメントは日本語で、作業文書パス・change 名の裸参照・タスク通番・レビュー通番のいずれも含まず、そのファイルだけを読んで意味が通る
- **テストの流儀**: NUnit の `Assert.That` + 制約 API という既存の書き方を維持し、`Ignore` やアサーションの削除による握りつぶしはない (弱体化ではなく強化の方向の変更)
- **修正 B の範囲**: `tasks.md` はチェック状態のみの変更で、項目本文・Scenario 参照・6.4 の grep コマンドはいずれも無改変。6.2 に注記を足していないのも、判定方法の読み替えが deviation.md にある以上、足場の文面を書き換えない扱いとして妥当

## アクションプラン

1. deviation.md の内訳に、追随後の assertion の形 (前方一致 2 件) を 1 句添える — Suggestion (蒸留時の追記でも足りる)
