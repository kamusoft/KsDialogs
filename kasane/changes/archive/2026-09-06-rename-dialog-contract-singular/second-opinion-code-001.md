# セカンドオピニオン: rename-dialog-contract-singular (code-001)
**相方**: codex / **label**: so-code-rename-dialog-contract-singular / **日付**: 2026-09-06 / **対象**: HEAD (20d2530) に対する作業ツリー全体 (staged rename 5 件・未 staged の修正・untracked の負の検査 4 本)
---
# レビュー結果: rename-dialog-contract-singular

**日付**: 2026-09-06  
**判定**: CHANGES_REQUESTED

## サマリー

契約型、参照箇所、Native bridge、API 形状検査、concepts、利用者向け Skills は一貫して新名へ更新されており、型名以外の契約変更や旧名の意図しない残存は確認できませんでした。  
ただし、完了判定に必要な Android instrumented テストの結果がなく、検証タスクを完了扱いにできません。Critical 0件、Major 1件、Minor 0件、Suggestion 0件です。

## 照合した規約

- `comment-policy.md` — always
- `test-execution.md` — テスト結果の報告・変更の完了判定
- `sample-parity.md` — `samples/kmp` の変更
- `local-development-setup.md` — Gradle と4形態の Sample ビルド
- `user-skill-api-listing.md` — `skills/**` の更新
- `user-skill-writing-style.md` — en/ja references の同期
- accepted ADR: core/ADR-0002、cross/ADR-0005、cross/ADR-0011、cross/ADR-0014
- core/ADR-0034 は proposed のため、決定ではなく変更意図の照合資料としてのみ参照

## 指摘事項

### [🟠 Major] Android instrumented テストの証跡なしで検証タスクが完了になっている

**該当箇所**: `kasane/changes/rename-dialog-contract-singular/tasks.md:15`、`kasane/handbook/cross/test-execution.md:23`

**問題点**: タスク 2.3 は handbook の手順によるライブラリテストを完了済みとしていますが、提示された結果には Android JVM テストの 67 件しかなく、別ルートである `connectedDebugAndroidTest` の結果がありません。handbook は instrumented 305件を全件実行対象として列挙し、`./gradlew test` ではこれらが1件も走らないことを明記しています。したがって、現状の証跡では完了条件を満たしていません。

**推奨修正**: `android/` で `./gradlew connectedDebugAndroidTest` を実行し、`:ksdialogs` と `:ksdialogs-compose` の結果 XMLから件数・failure・skipを合算して報告してください。APIレベル固有の skip を含めて全Scenarioを完了扱いにする場合は、handbookどおりAPI 29とAPI 30以上の双方で確認してください。

## アクションプラン

1. Android instrumented テストを必要なAPIレベルで実行する。
2. モジュール別の件数、failure、skipを報告へ追加する。
3. 全件成功を確認後、独立レビューを再実施する。


## 突き合わせ結果 (ホスト側 review-001.md との照合、2026-09-06)

| 相方の指摘 | ホスト側 | 採否 | 根拠 |
|---|---|---|---|
| [Major] Android instrumented テスト (`connectedDebugAndroidTest`) の証跡なしで 2.3 が完了扱い | 指摘なし (ホスト側の review / verify も JVM の 67 件のみ実行) | **採用** | `kasane/handbook/cross/test-execution.md:23` が instrumented を独立したビルドルートとして全件実行表に載せており、dialog-contract spec の Scenario「4 形態のライブラリテストが通る」は同 handbook の手順を参照している。ホスト側の見逃しとして扱い、instrumented の実行と証跡 (review / verify) の更新を修正サイクルに含める |

ホスト側のみの指摘 (Minor: concepts 2 本の `timestamp` 未更新 / Suggestion 3 件) は相方に対応する指摘がなく、ホスト側の判定のまま扱う。矛盾する指摘なし・未解決なし。
