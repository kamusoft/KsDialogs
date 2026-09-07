# ライブラリ本体の日本語文字列リテラル grep — 実行の記録

tasks 6.4 の確認記録 (2026-09-07)。
デルタスペックの 4 Scenario `DM-IO-03` / `DM-AN-02` / `DM-KM-04` / `DM-MA-05` は
「ライブラリ本体に日本語の文字列リテラルが残らない」ことを判定対象にしており、
判定がソースツリー全体の静的検索であるため自動テストを持たない。
`scripts/scenario-id-coverage.py` の除外表に理由付きで登録し、本記録を受け入れ証跡とする。

## 実行したコマンドと結果

実行は本 change の作業ツリー (リポジトリルート) で行った。全文は [grep.log](grep.log)。

```
grep -rn --include='*.swift' --include='*.kt' --include='*.cs' -E '"[^"]*[ぁ-んァ-ヶ一-龠][^"]*"' \
    ios/Sources android/ksdialogs android/ksdialogs-compose kmp/ksdialogs-kmp/src \
    maui/KsDialogs.Maui maui/macios/native maui/android/native \
    | grep -v '/build/' \
    | grep -v -E '^[^:]+:[0-9]+:\s*(//|///|\*|/\*)' \
    | grep -v -E '/src/(test|androidTest|commonTest|iosTest|androidHostTest)/' \
    | grep -v 'Tests/'
```

**結果: 出力なし (0 行)** — 期待どおり。

## 何を見て、何を見ていないか

| 面 | 内容 |
|---|---|
| 検索対象 | 4 形態のライブラリ本体のソース (`*.swift` / `*.kt` / `*.cs`) |
| 検索する文字 | ダブルクォートで囲まれた中にひらがな・カタカナ・漢字を含む行 |
| 除外 | ビルド生成物 (`/build/`)、行頭がコメント記号の行、テストソース (`src/{test,androidTest,commonTest,iosTest,androidHostTest}/` と `*Tests/`) |

- **コメントは対象外**。プロジェクトのコメント規約は日本語であり、文言の置き換えで
  doc comment を英語化しない (cross/ADR-0015 の対象外)
- **テストと samples は対象外**。ライブラリが外へ出す文言ではない
- この検索は「日本語が残っていないこと」しか見ない。英語文言がデルタスペックの
  対応表どおりであることは、各形態の完全一致テスト
  (`DM-IO-01` / `DM-IO-02` / `DM-AN-01` / `DM-MA-01`〜`DM-MA-04`) とレビューが担う
- 行頭以外に置かれた行末コメント中の日本語は除外されないため、この検索が 0 件であることは
  「リテラルに日本語が無い」より強い条件を満たしている
