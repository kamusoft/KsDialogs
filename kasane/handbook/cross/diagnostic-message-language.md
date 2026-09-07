---
kind: rule
applies-when:
  always: false
  paths: ["ios/Sources/**", "android/ksdialogs/src/main/**", "android/ksdialogs-compose/src/main/**", "kmp/ksdialogs-kmp/src/commonMain/**", "kmp/ksdialogs-kmp/src/androidMain/**", "kmp/ksdialogs-kmp/src/iosMain/**", "maui/KsDialogs.Maui/**", "maui/macios/native/KsDialogsMauiBridge/**", "maui/android/native/ksdialogs-maui-bridge/src/main/**"]
  tasks: [失敗型の case・例外型の追加, 例外メッセージ・警告ログの追加や変更, 診断文言を引用する文書の更新]
title: ライブラリが外へ出す診断文言の言語
description: 失敗型のメッセージ・throw 箇所の文言・警告ログとその部品は英語固定でローカライズしない。文言は互換契約ではない。日本語リテラルの残存は静的 grep で検査する
timestamp: 2026-09-07
---

# ライブラリが外へ出す診断文言の言語

この文書は、ライブラリ本体 (4 形態 = iOS Native / Android Native / KMP 共有コード / MAUI バインディング) が実行時に外へ出す診断文言をどの言語で書くかと、その文言をどう扱うかを定める。読むと、失敗型の case や警告ログを足すときに文言をどう書くか、既存の文言を変えてよいか、残存を何で検査するかが分かる。根拠決定は [cross/ADR-0015](../../decisions/cross/0015-diagnostic-messages-english-only.md)。

## 対象

| 対象 | 例 |
|---|---|
| 失敗型のメッセージ | iOS `DialogError` / `KsDialogsKmpError` の `errorDescription`、Android / MAUI `DialogException` の message、MAUI iOS bridge の内部 Error |
| throw 箇所で直接渡す例外文言 | storyboard 非対応 `init?(coder:)` の `fatalError`、gateway / bridge の `InvalidOperationException` / `error()` |
| 警告ログとその部品 | `Logger.warning` / `Log.w` / `Trace` の本文、書式文字列とその部品となる定数 |
| KMP 共有コードが自前で組み立てる文言 | iOS ホスト gateway の失敗メッセージ定数、型指定 show の VM factory 未登録・型不一致 ([KMP の Dialog 公開面](../../concepts/kmp/api/dialog-surface.md)) |

gateway / bridge は KMP・MAUI が Native ライブラリを呼ぶ継ぎ目のコードを指す。

対象外: ソースコメント・doc コメント ([comment-policy.md](comment-policy.md) のとおり日本語)、`samples/` とテストコードの文言、エンドユーザーに表示する文字列 (ライブラリは現在持たない。持つようになったら言語方針を別途決める)。

## 書き方

- 上の対象はすべて**英語**で書き、ローカライズ機構 (`.strings` / strings.xml / `.resx`) を使わない。「例外は英語・ログは日本語」のような 2 段の書き分けはしない
- iOS と Android で対になる警告 (同じ状況を同じ機能で報告するもの) は、動的な値の前の**本文テンプレート**を同じ英語にする。動的なエラー説明の渡し方は各 OS のログ慣行に従ってよい (iOS は本文へ埋め込み、Android は `Log.w` の throwable 引数)。対応物が片側にしか無い文言は同一化の対象外
- 型名は補間で埋め、コード識別子を英訳しない。文体は主語を registry / factory などに置いた平叙文で、末尾にピリオドを付ける (既存の失敗型と揃える)

## 文言は互換契約ではない

契約は case 名・例外型・引数・throw 条件で、文言は現在の実装値である。文言の改訂は非破壊の修正として扱い、利用者向け Skills の診断表には「メッセージは現在の実装値で、安定 API ではない」を添える ([user-skill-writing-style.md](user-skill-writing-style.md))。文言の完全一致テストは、対応表どおりに置き換わったことの受け入れ基準であり、文言を安定 API に昇格させるものではない。

## 検査

対象範囲に日本語の文字列リテラルが残っていないことを、次の静的 grep で確かめる (出力なしが期待結果)。行頭がコメントの行とテストソースは除外している。走査はモジュール全体 (`android/ksdialogs` 等) に掛け、テストはフィルタで落とすため、frontmatter の `paths` より広い範囲を見て同じ結果になる。

```bash
grep -rn --include='*.swift' --include='*.kt' --include='*.cs' -E '"[^"]*[ぁ-んァ-ヶ一-龠][^"]*"' ios/Sources android/ksdialogs android/ksdialogs-compose kmp/ksdialogs-kmp/src maui/KsDialogs.Maui maui/macios/native maui/android/native | grep -v '/build/' | grep -v -E '^[^:]+:[0-9]+:\s*(//|///|\*|/\*)' | grep -v -E '/src/(test|androidTest|commonTest|iosTest|androidHostTest)/' | grep -v 'Tests/'
```

この検査は「日本語が残っていない」ことしか見ない。英語文言が意図どおりであることはレビューと、失敗型ごとの完全一致テスト (iOS / Android / MAUI の `DM-IO-01` / `DM-IO-02` / `DM-AN-01` / `DM-MA-01`〜`04` の系列。KMP 共有コードの自前文言は型指定 show のテスト `PB-KT-05` / `PB-KT-13` が部分一致で見る) が担う。文言を変えたら、その文言を部分一致で見ている既存テストを確認する — 部分一致 assertion が、実装の文言ではなくテストが自分で組み立てた文字列にだけ一致する状態になっていないか。

## 禁止事項

- 対象範囲への日本語の文言の追加 (1 件でも入ると上の検査が落ちる)
- 文言の書き換えを破壊的変更として扱うこと (文言は契約ではない)。逆に、case 名・例外型・throw 条件の変更は文言の変更に見えても契約の変更として扱う
- 利用者向け Skills の診断表での文言の翻訳・言い換え (実装の英語文言をそのまま引用する)

## 関連

- [cross/ADR-0015](../../decisions/cross/0015-diagnostic-messages-english-only.md) — 決定 (英語固定・ローカライズしない・代替案の却下理由)
- [comment-policy.md](comment-policy.md) — コメントは日本語 (本規約の対象外)
- [user-skill-writing-style.md](user-skill-writing-style.md) — Skills の診断表での引用の仕方
- 出典: `kasane/changes/archive/2026-09-07-localize-dialog-error-messages/` (proposal.md の Impact と各 capability のデルタスペックの対応表、verification/japanese-literal-grep/README.md)
