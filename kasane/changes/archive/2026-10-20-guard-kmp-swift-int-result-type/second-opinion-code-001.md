# セカンドオピニオン: guard-kmp-swift-int-result-type (code-001)
**相方**: codex / **label**: so-code-guard-kmp-swift-int-result-type-001 / **日付**: 2026-10-20 / **対象**: 提案コミット f98ed41 に対する作業ツリーの差分 (ios/Sources/KsDialogs/Kmp/KsDialogsKmp.swift・KsDialogsKmpError.swift、ios/Tests/KsDialogsTests/KsDialogsKmpModelBindingTests.swift・KsDialogsKmpKotlinIntegerResultTests.swift)
---
# レビュー結果: guard-kmp-swift-int-result-type (001 回目)

**日付**: 2026-10-20
**判定**: APPROVED
**指摘件数**: Critical 0 / Major 0 / Minor 0 / Suggestion 0

## サマリー

指定された4ファイルの差分を静的レビューし、修正を要する問題は見つかりませんでした。Int／UInt の直接指定とジェネリック経由の指定に同じガードが適用され、判定順序・エラー内容・登録の維持がデルタスペックと一致しています。

ビルド・テストは依頼の制約に従って再実行せず、ホスト側の結果と `evidence/` の記録を確認しました。`deviation.md` は合意済み差分として扱い、別件の `strip-adr-ids-from-ios-public-docs` は対象外としました。

## 照合した規約

- `kasane/handbook/cross/comment-policy.md`: 追加・変更コメントの日本語、自己完結性、公開説明の内容。
- `kasane/handbook/cross/diagnostic-message-language.md`: deprecation 警告と型不一致の診断値。
- `kasane/handbook/cross/test-execution.md`: 実行ルート、件数、Scenario ID の確認。
- `kasane/handbook/cross/ci-flaky-test-policy.md`: MainActor 上の状態観測、表示待ち、skip の有無。
- `kasane/handbook/cross/runtime-behavior-verification.md`: 純粋な型判定と提示機構の不具合との適用区分。
- kmp/ADR-0003・0004・0005: Swift パッケージ側の公開面、型付き失敗、公開エラーの case 集合、キャンセル経路。
- core/ADR-0011・0012・0018、ios/ADR-0001: 登録オーバーロード、Bool 既定、報告口の紐付け、表示順序。

ロードしたスキル: `ksn-review`、`swift-ui-impl-skill`、`kotlin-impl-skill`。

## 確認した観点

- **仕様充足**: 6入口 × Int／UInt の12オーバーロードがあり、英語の案内文と実行時の委譲先が揃っています。
- **判定順序**: show は指定型を優先し、その型が対象外の場合だけ呼び出し時点の登録型を判定します。notifier は表示状態・登録型との比較より先に指定型を拒否します。
- **型の同一性**: 型名の文字列ではなく、既存の `DialogDeclaredResultType` で判定しています。
- **副作用**: 不適合な show は提示経路へ入る前に失敗し、register は従来どおり登録します。
- **設計品質**: 共通の非公開実装への委譲により、deprecated overload の自己再帰を避けています。公開エラーの case、ObjC 互換面、キャンセル処理は変更されていません。
- **既存契約**: 表示中の報告口は既存 binding の結果型を使い、再登録で判定型が変わらない構造を維持しています。
- **テスト品質**: 登録結果そのもの、factory 未呼び出し、指定型の優先順位、ジェネリック経由、表示中の報告口、対応整数型の completed を検証しています。既存 MB-KM-04 の Int64 への変更も元の検証意図を保っています。
- **スコープ**: tasks の完了記録とコード・証跡の対応を確認し、未記録の仕様逸脱や不要な付随変更は見つかりませんでした。

ホスト側の実行結果として確認した範囲:

| ルート | 結果 |
|---|---|
| ios/ | Swift Testing 379件成功、XCTest 0件 |
| android/ | 今回の提示結果なし |
| android/ instrumented | 今回の提示結果なし |
| kmp/ | iOS 86件・Android host 83件、計169件成功 |
| maui/ | 今回の提示結果なし |
| maui/android/native/ | 今回の提示結果なし |
| maui/macios/native/ | 今回の提示結果なし |

今回の全件成功の確認範囲は ios/・kmp/ です。加えて、IR-KM 7/7 の網羅と12入口の deprecation 警告証跡を確認しました。

## 指摘事項

なし。

## アクションプラン

レビュー指摘に伴う修正は不要です。

## 突き合わせ結果

| 指摘 | 出典 | 採否 | 扱い |
|---|---|---|---|
| review-001 Minor: 実装期間中の proposal.md (Impact) の書き換えが deviation.md に無い | ホスト側のみ | 採用 (ホスト側指摘) | 相方は作業ツリーの proposal.md を最新の足場として読んだため見ていない。根拠は強い (提案のコミットとの差分が未記録)。deviation.md に記録した |
| review-001 Suggestion: 完了報告では実行したルートを列挙し「全ルートで成功」と書かない | ホスト側 (相方の表も同じ範囲を示す) | 確定 | 完了報告と証跡でルートを列挙する。レビュアーが再実行した maui/macios/native/ (17 tests) を evidence/test-run-summary.md に足した |
| コードの指摘 | 双方なし | — | — |

確定 1 / 採用 1 / 降格 0 / 未解決 0
