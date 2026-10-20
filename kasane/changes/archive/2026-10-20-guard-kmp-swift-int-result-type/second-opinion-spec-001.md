# セカンドオピニオン: guard-kmp-swift-int-result-type (spec-001)
**相方**: codex / **label**: so-spec-guard-kmp-swift-int-result-type-001 / **日付**: 2026-10-20 / **対象**: 提案一式 (proposal.md / specs/kmp-facade/spec.md / tasks.md)
---
指摘は3件です。オーナー決定の「deprecated 警告＋show 時の型付き失敗」は前提として維持しています。

1. **Major — `kasane/changes/guard-kmp-swift-int-result-type/tasks.md:8`**

   **問題点:** show の先頭で登録型を検査しても、実際に使う factory と一致する保証がありません。既存経路は `KsDialogsInteropBridge.swift:98` で別 Task を作り、`DialogPresenter.swift:33` でレジストリを再解決します。この間に許可型から `Int` / `UInt` へ再登録されると、検査を通過して拒否対象の factory が呼ばれ、spec:28 の「表示せず、factory も呼ばない」を破ります。登録は MainActor に限定されていません。

   **推奨修正:** 検査した factory と提示に使う factory を同一のスナップショットにするか、Swift 面からの提示に限って実際に解決した factory を生成前に検査する設計を明記してください。再登録が検査と提示の間に起きる Scenario と検証タスクも追加してください。

2. **Minor — `kasane/changes/guard-kmp-swift-int-result-type/specs/kmp-facade/spec.md:28`**

   **問題点:** 指定型と登録型が異なる拒否対象の場合、エラーの内容が一意に決まりません。例えば登録型が `Int`、show の指定型が `UInt` の場合、`expected` / `actual` にどちらの組を返すか未定義です。IR-KM-03・04 もこの組み合わせを扱っていません。

   **推奨修正:** 「指定型を先に判定し、許可型の場合のみ登録型を判定する」など優先順位を定め、登録 `Int`／指定 `UInt` と、その逆の期待エラーを Scenario に追加してください。

3. **Minor — `kasane/changes/guard-kmp-swift-int-result-type/tasks.md:16`**

   **問題点:** 登録成功の検証として「その型の show が IR-KM-04 の失敗になる」だけを認めると、登録が何もしない実装でも合格できます。`show(result: Int.self)` は登録の有無によらず、指定型だけで同じ失敗になるためです。

   **推奨修正:** 4 種それぞれの登録直後にレジストリの宣言結果型を検査する方法へ統一するか、show で検証する場合は `Bool` 省略形など許可型を指定し、エラーが登録型に由来することを必須にしてください。

静的レビューのみ実施しました。ビルド・テスト実行、ファイル変更は行っていません。

## 突き合わせ結果

ホスト側の自己レビュー (ksn-propose Step 8 のチェックリスト) は 2 周で問題なしだった。相方の指摘はどれもホスト側の見逃し。

| # | 採否 | 扱い |
|---|---|---|
| 1 (Major: 検査と提示の間の再登録) | 採用 | 根拠強 (`ios/Sources/KsDialogs/Interop/KsDialogsInteropBridge.swift:98` で提示が別 Task になり、登録は MainActor 限定でない)。仕組みは足さず、spec の約束を「show を呼んだ時点の登録で判定し、その後の再登録は既存の結果復元時の照合に任せる」に絞った (判断の優先順 4 — 共有の提示経路に手を入れない) |
| 2 (Minor: 指定と登録が両方当たるときの内容) | 採用 | 「指定した結果型を先に判定する」を Requirement に書き、IR-KM-04 に `Int` 登録 × `UInt` 指定 (と逆) の AND を足した。tasks 1.2・2.3 も合わせた |
| 3 (Minor: 登録の確認が空実装でも通る) | 採用 | tasks 2.2 の確かめ方を、登録直後の `declaredResultType` か、結果型を省略した show の登録由来の失敗 (`actual: "Int"`) に限った |

確定 0 / 採用 3 / 降格 0 / 未解決 0
