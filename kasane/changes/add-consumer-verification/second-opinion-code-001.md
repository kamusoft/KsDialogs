# セカンドオピニオン: add-consumer-verification (code-001)
**相方**: codex / **label**: so-code-add-consumer-verification / **日付**: 2026-09-09 / **対象**: 未 commit の実装 diff 全体 (verification/ 一式・scripts/readme-example-lint.py・.github/workflows/verify-consumer-*.yml・ci.yml・kasane/config.yaml・handbook 2 本・concepts/log.md)
---
# レビュー結果

**判定: CHANGES_REQUESTED**

Critical 0件 / Major 1件 / Minor 1件 / Suggestion 0件。

静的レビューのみ実施し、指定どおりビルド・テストは再実行していません。本体ソース (`ios/`、`android/`、`maui/`、`kmp/`、`samples/`、README) に変更はありません。

## 指摘事項

### Major: KMP 発行失敗時に追跡ファイルが汚れ、既存のローカル変更も失われる

**該当箇所**: [verification/lib/gradle-publish.sh:64](verification/lib/gradle-publish.sh:64)、[verification/lib/gradle-publish.sh:70](verification/lib/gradle-publish.sh:70)、[verification/lib/gradle-publish.sh:80](verification/lib/gradle-publish.sh:80)、[verification/kmp/prepare-feed.sh:90](verification/kmp/prepare-feed.sh:90)

**問題点**: `ksv_publish_kmp` は本体側の追跡済み Swift マニフェスト2本を書き換えますが、復元は発行成功後にしか呼ばれません。Gradle 発行や成果物確認が途中で失敗すると `set -e` により終了し、`file://` の絶対パスが作業ツリーへ残ります。

また、復元の `git checkout --` は対象が追跡済みかしか確認しておらず、実行前から存在した未ステージ変更を破棄します。Git 管理外では復元を黙って省略するため、その場合も変更が残ります。

[5.1 の証跡:112](kasane/changes/add-consumer-verification/evidence/verification/5.1-dry-run-no-args.txt:112) が示すのはクリーンな状態で成功した経路だけで、発行失敗時や既存変更がある状態を識別できません。

`deviation.md` に記録された「発行後に復元する」という合意自体への違反ではなく、その実装の失敗時・既存変更時の安全性が不足しています。

**根拠**:

- consumer-verification「追跡している生成物は実行で変化しない」
- `cross/ci-script-deletion.md`「破壊的な操作の前に対象を検証する」
- ksn-review「エラーハンドリング」「副作用・データ損失」

**推奨修正**: 発行前に Git 管理下であることと対象2ファイルが未変更であることを検査し、変更済みなら発行前に停止してください。そのうえで、発行開始前に `EXIT` trap を設定し、成功・失敗のどちらでも復元されるようにします。併せて次の負ケースを追加してください。

- 対象ファイルに既存変更がある場合、それを破棄せず早期失敗する
- KMP 発行を意図的に失敗させても2ファイルが実行前と同一になる

### Minor: README の節自体が重複すると検査を素通りする

**該当箇所**: [scripts/readme-example-lint.py:60](scripts/readme-example-lint.py:60)、[scripts/readme-example-lint.py:74](scripts/readme-example-lint.py:74)

**問題点**: 最初の `## Minimal examples` だけを見つけ、次の `##` で抽出を終了します。このため、README に `## Minimal examples` 節が2つ存在しても、最初の節が一致していれば後続の重複節を無視して成功します。

現在の自己テストは同じ節内のコードブロック重複だけを検査しており、この経路を捕捉しません。

**根拠**: consumer-verification「README 最小例との一致」— 対応するコードブロックの重複は失敗とする。

**推奨修正**: fence 内外を区別しながら文書全体の `## Minimal examples` 見出し数を数え、ちょうど1件でなければ失敗させてください。節が2つある負ケースも自己テストへ追加します。

## 照合した規約

- `cross/comment-policy.md`
- `cross/verification-ci.md`
- `cross/ci-script-deletion.md`
- `cross/local-development-setup.md`
- `cross/test-execution.md`
- `kasane/lessons/process.md`
- Swift / Kotlin の設定解決済みレビュー観点

## Scenario 対応要約

4形態の消費者構成、dry-run・smoke の参照分離、Release ビルド、Android・MAUI・KMP の依存検査、README 一致検査、CI の再利用配線は仕様との対応を確認できました。提示された成功・負ケースの証跡も実装と概ね整合しています。

一方、KMP の「追跡物を変化させない」は成功経路しか担保していません。また、[tasks.md:45](kasane/changes/add-consumer-verification/tasks.md:45) の実 Actions 起動と、[tasks.md:46](kasane/changes/add-consumer-verification/tasks.md:46) の artifact download 経路は未確認のままです。



---

## 突き合わせ結果 (ホスト側 review-001 との照合、2026-09-09)

| 指摘 | 出典 | 採否 | 扱い |
|---|---|---|---|
| KMP のフィード準備が失敗すると本体側の合成 Swift マニフェスト 2 本が `file://` のまま残る。復元が既存のローカル変更を破棄する | 双方一致 (host Minor 1 / 相方 Major) | **確定** (Major — 高い方) | 発行前に対象 2 本が未変更かを検査して変更済みなら停止、`EXIT` trap で成否によらず復元、復元は `git checkout HEAD --`。負ケース (既存変更で早期失敗 / 発行失敗でも同一) を証跡に追加 |
| Android 消費者のプラグイン版がカタログ共有の外にある (AGP / compose プラグインの直書き) | host のみ (Minor 2) | **確定** (Minor) | kmp 消費者と同じく `alias(libs.plugins.*)` へ寄せる |
| `Select Xcode` の実体パス解決が consumer-maui だけにある | host のみ (Minor 3) | **確定** (Minor) | ios / kmp にも揃える (無害) |
| README の `## Minimal examples` 節自体が重複すると検査を素通りする | 相方のみ (Minor) | **採用** (Minor) | 根拠が具体的 (該当行と抜け道の再現手順あり、修正コストが小さい)。見出し数を数えて 1 件以外を失敗にし、自己テストに負ケースを足す |
| `verification/lib/*.sh` の `# shellcheck shell=bash` 欠落 | host Suggestion | 対処 | 1 行追加 |
| MAUI 消費者の `<uses-sdk>` が最低対象 OS を三重に持つ | host Suggestion | 見送り | 翻案元と同形、挙動への影響なし |
| tasks 5.5 / 5.6 の CI 側がオーナー実施待ち | 双方 | 保留 | オーナーの draft PR push 後に実施 (完了報告に明記) |

未解決 (矛盾) なし。
