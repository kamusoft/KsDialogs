# セカンドオピニオン: add-maui-nuget-distribution (spec-001)
**相方**: codex / **label**: so-spec-add-maui-nuget-distribution / **日付**: 2026-09-08 / **対象**: kasane/changes/add-maui-nuget-distribution/ の proposal / design / specs 2 本 / tasks (提案一式、実装前)
---
# 独立スペックレビュー

静的照合のみ実施し、ビルド・テスト・書き込みは行っていません。

## 指摘事項

### [🟠 Major] Toast では `ViewCreationFailed` を呼び出し元へ返せない

**該当箇所**: [specs/maui-binding/spec.md:7](kasane/changes/add-maui-nuget-distribution/specs/maui-binding/spec.md:7)、[IKsToast.cs:13](maui/KsDialogs.Maui/Presentation/IKsToast.cs:13)、[core/ADR-0033:22](kasane/decisions/core/0033-user-factory-failure-boundary.md:22)

**問題点**: Requirement は `RegisterForToast` も対象に含め、「show が `ViewCreationFailed` で失敗」「実機経路でも呼び出し元へ同じ型が届く」と要求しています。しかし Toast の `Show` は `void` の fire-and-forget で、受理後の生成失敗は既存契約上「警告を残して1枚だけ破棄」です。呼び出し元へ例外を返すことは、公開契約を破壊しない限り不可能です。

**推奨修正**: Dialog / Loading と Toast の Requirement を分離してください。Toast は「`ViewCreationFailed` を原因として警告し、その1枚だけ破棄し、後続表示は継続する」という Scenario にする必要があります。

### [🟠 Major] View fallback の例外境界と `ViewTypeName` が成立しない

**該当箇所**: [specs/maui-binding/spec.md:7](kasane/changes/add-maui-nuget-distribution/specs/maui-binding/spec.md:7)、[design.md:72](kasane/changes/add-maui-nuget-distribution/design.md:72)、[proposal.md:28](kasane/changes/add-maui-nuget-distribution/proposal.md:28)、[KsDialogsOptions.cs:34](maui/KsDialogs.Maui/Hosting/KsDialogsOptions.cs:34)

**問題点**: `UseViewFallback` は利用者が渡す `Func<Type, IServiceProvider, View?>` です。それにもかかわらず、spec はその例外をライブラリ自身の生成失敗として包み、proposal は利用者 factory の例外を包まないとしています。また resolver が View を返す前に投げた場合、ライブラリが知っているのは ViewModel 型だけで、「生成しようとした View の型名」は取得できません。

**推奨修正**: 次のどちらかを明示決定してください。

- fallback の例外は利用者 factory として元のまま通し、`ViewCreationFailed` は型を把握できる1行登録だけに限定する。
- fallback も包むなら、`ViewTypeName` を nullable にするなど意味を変更し、非包み直しの Non-Goal と accepted ADR との関係も改訂する。

### [🟠 Major] 最低 OS ガードの「未設定」契約が相互矛盾している

**該当箇所**: [specs/maui-nuget-distribution/spec.md:61](kasane/changes/add-maui-nuget-distribution/specs/maui-nuget-distribution/spec.md:61)、[specs/maui-nuget-distribution/spec.md:77](kasane/changes/add-maui-nuget-distribution/specs/maui-nuget-distribution/spec.md:77)、[tasks.md:39](kasane/changes/add-maui-nuget-distribution/tasks.md:39)、[翻案元 deviation.md:9](../KsSettingsView/kasane/changes/archive/2026-09-02-add-maui-nuget-distribution/deviation.md:9)

**問題点**: spec は未設定なら両 platform でエラーとしていますが、tasks は iOS 未設定時に SDK 既定値が入り「発火しない」ことを受け入れています。翻案元でも同じ spec 逸脱が既知です。このままでは、実装開始前から spec と tasks のどちらかに必ず違反します。

**推奨修正**: 契約を「MSBuild 評価後の値が要件未満ならエラー」に直し、Android 未設定は既定 21.0 で失敗、iOS 未設定は SDK 既定値が17以上なら成功、と明記してください。明示設定自体を必須にしたいなら、SDK 既定値適用後の値比較とは別の検出機構が必要です。

### [🟠 Major] 下限依存では binding の同一版を保証できない

**該当箇所**: [design.md:38](kasane/changes/add-maui-nuget-distribution/design.md:38)、[design.md:43](kasane/changes/add-maui-nuget-distribution/design.md:43)、[specs/maui-nuget-distribution/spec.md:29](kasane/changes/add-maui-nuget-distribution/specs/maui-nuget-distribution/spec.md:29)

**問題点**: spec は facade と binding の同一 version を保証しますが、design は SDK pack が作る下限依存と NuGet の最低適用版解決に依存しています。NuGet の裸の `1.0` は `x >= 1.0` であり、直接依存や別依存の制約により上位 binding が選ばれ得ます。ローカルフィードに1版だけ置く現在の検証では、この不整合を検出できません。[NuGet version ranges](https://learn.microsoft.com/en-us/nuget/concepts/package-versioning)、[dependency resolution rules](https://learn.microsoft.com/en-us/nuget/concepts/dependency-resolution)

**推奨修正**: 版間互換を提供しないなら binding 依存を `[x.y.z]` の完全一致にしてください。下限依存を維持するなら「同一版保証」を撤回し、異版組み合わせの互換契約とテストが必要です。2版を置いたフィードと競合する直接参照を使う負の検証も追加してください。

### [🟠 Major] 「SDK 標準 pack のみ」と private target 後処理が矛盾している

**該当箇所**: [proposal.md:13](kasane/changes/add-maui-nuget-distribution/proposal.md:13)、[design.md:17](kasane/changes/add-maui-nuget-distribution/design.md:17)、[maui/ADR-0004:25](kasane/decisions/maui/0004-nuget-three-package-structure.md:25)

**問題点**: 提案と ADR は pack 内部構造に依存する自作 MSBuild を足さないとしながら、design は SDK の private target `_IncludeAarInNuGetPackage` に `AfterTargets` で接続して同梱物を除去します。これはまさに SDK 内部 pack 経路への依存です。

**推奨修正**: この後処理を意図的な例外として ADR/design に明記し、固定 SDK、壊れたことを検出する positive control、SDK 更新時の見直し条件を定めてください。例外を認めないなら、公開された拡張点だけで除外できる別方式が必要です。

### [🟠 Major] README の実際の相対リンク数とタスクの対象が合っていない

**該当箇所**: [tasks.md:32](kasane/changes/add-maui-nuget-distribution/tasks.md:32)、[specs/maui-nuget-distribution/spec.md:89](kasane/changes/add-maui-nuget-distribution/specs/maui-nuget-distribution/spec.md:89)、[README.md:160](README.md:160)

**問題点**: tasks は「各5箇所」を対象にしていますが、現状は両 README とも相対 Markdown リンクが12件あります。`ios/`、`android/`、`maui/`、`kmp/`、`kasane/`、`AGENTS.md`、`CONTRIBUTING` などが列挙から漏れており、tasks のままでは spec の「相対参照が残らない」を満たしません。

**推奨修正**: 固定件数ではなく、anchor・mailto を除く非 HTTP(S) 参照を機械的に全件抽出し、0件を受け入れ条件にしてください。

### [🟠 Major] Android の型消失不具合に修正前の実環境再現がない

**該当箇所**: [tasks.md:13](kasane/changes/add-maui-nuget-distribution/tasks.md:13)、[runtime-behavior-verification.md:17](kasane/handbook/cross/runtime-behavior-verification.md:17)

**問題点**: task 1.7 は修正後の観測だけです。今回の主張は「Android 実機経路で型が `InvalidOperationException` に失われる」という実行時不具合なので、handbook が要求する修正前再現→同一手順での修正後確認を満たしていません。コード読解だけでは実環境再現の代わりになりません。

**推奨修正**: 実装前に Android エミュレータで現行の `InvalidOperationException`・InnerException 消失を保存し、修正後に同じアプリ・操作で `ViewCreationFailed` と InnerException を確認する A/B タスクへ変更してください。

### [🟠 Major] MAUI 完了判定に必須の iOS 互換面テストが tasks にない

**該当箇所**: [tasks.md:10](kasane/changes/add-maui-nuget-distribution/tasks.md:10)、[test-execution.md:138](kasane/handbook/cross/test-execution.md:138)、[test-execution.md:163](kasane/handbook/cross/test-execution.md:163)

**問題点**: tasks は facade テストと Android 互換面テストを含みますが、iOS 互換面の全件実行を含みません。handbook は MAUI の完了条件として `dotnet test`・Android互換面・iOS互換面の3実行すべてを必須としています。

**推奨修正**: 全実装後の最終確認として3ルートを再実行し、それぞれ実行件数と失敗0を記録するタスクを追加してください。

### [🟠 Major] Sample 通しの Scenario が操作・期待結果を定義できていない

**該当箇所**: [specs/maui-nuget-distribution/spec.md:99](kasane/changes/add-maui-nuget-distribution/specs/maui-nuget-distribution/spec.md:99)、[tasks.md:45](kasane/changes/add-maui-nuget-distribution/tasks.md:45)、[sample-parity.md:224](kasane/handbook/cross/sample-parity.md:224)

**問題点**: Scenario は全14項目について「結果表示の変化」を要求しますが、通常の Toast 項目には結果表示がありません。また `layout-dialog` / `transition-dialog` の起動引数は専用画面を開くだけで、ダイアログ表示や結果到着には追加操作が必要です。さらに design は MAUI 10.0.20 回帰を「両 OS の Sample 通し」で見るとしていますが、tasks の全項目通しは iOS のみで、Android は基本 Dialog 1件だけです。

**推奨修正**: 安定デモ ID ごとに「起動後の追加操作」「期待する画面／結果」「証跡」を表で定義し、結果表示は該当項目だけに限定してください。10.0.20 の両 OS 回帰を主張するなら Android の全項目通しも追加し、追加しないなら design の検証範囲を狭めてください。

### [🟡 Minor] 新しい公開例外面の検査が不足している

**該当箇所**: [specs/maui-binding/spec.md:7](kasane/changes/add-maui-nuget-distribution/specs/maui-binding/spec.md:7)、[tasks.md:8](kasane/changes/add-maui-nuget-distribution/tasks.md:8)

**問題点**: Requirement は `ViewTypeName` と `ViewModelTypeName` の両方を公開するとしていますが、MB-MA-11 は `ViewModelTypeName` を検査せず、外部利用者可視性を確認する ApiSurfaceCheck の追加も tasks にありません。

**推奨修正**: 両プロパティ値の assertion と、非 friend の `KsDialogs.Maui.ApiSurfaceCheck` から型・InnerException・両プロパティを参照できる正のコンパイル検査を追加してください。

## 照合した規約

`cross/test-execution.md`、`cross/runtime-behavior-verification.md`、`cross/sample-parity.md`、`cross/diagnostic-message-language.md`、`cross/local-development-setup.md`、指定 ADR、および翻案元の実装・deviation・消費者証跡を照合しました。

## 総合判定

**NEEDS_DISCUSSION**

Critical 0件、Major 9件、Minor 1件です。特に Toast の失敗契約、fallback 例外の分類、NuGet binding の版固定は実装だけでは解決できず、公開契約または設計判断を先に確定する必要があります。



## 突き合わせ結果 (2026-09-08)

ホスト側の自己レビュー (ksn-propose Step 8 のチェックリスト 2 周、問題なし) と突き合わせ。相方のみの指摘は根拠で判定した。

| 指摘 | 採否 | 根拠と反映先 |
|---|---|---|
| Toast では `ViewCreationFailed` を返せない | 採用 (Major) | Toast の Show は戻り値なし (core/ADR-0031)。spec maui-binding の Requirement を Dialog / Loading と Toast で分け、MB-MA-12 を Toast の警告 + 破棄に差し替え。design Decision 6 |
| View fallback の例外境界と `ViewTypeName` | 採用 (Major、設計判断) | オーナー裁定 A: fallback は利用者コードとして包まない。spec MB-MA-13 に resolver を含め、agenda 論点 3 の決定事項を改訂。design Decision 6 代替案 A' |
| ガードの「未設定」契約の矛盾 | 採用 (Major) | 評価後の値で比較する契約に改め、Android 未設定は既定 21.0 で失敗・iOS 未設定は通ると明記。spec / design Decision 4 / tasks 5.4 |
| binding 依存の完全一致 | 降格 (Minor 相当、記録) | SDK 10.0.300 の pack targets と `NuGet.Build.Tasks.Pack.dll` に完全一致の標準手段が無く、実現は ADR-0004 の却下案 (内部アイテムの書き換え)。lockstep + 最小適用版解決で同版、消費者側の一致は phase-8 の依存検査。design Decision 3 / Risks / ADR-0004 Consequences に受容を明記 |
| 「SDK 標準 pack のみ」と内部ターゲット後処理の矛盾 | 採用 (Major) | 意図的な例外として design Decision 1 と ADR-0004 Consequences に明記、検出 (pack 検算の aar 不在) と見直し条件 (Revisit When) を追加 |
| README の相対リンク数 | 採用 (Major) | 機械抽出で各 12 件と確認。受け入れ条件を「非 HTTP(S) 参照 0 件」に改め、tasks 4.1 |
| Android の型消失に修正前再現がない | 採用 (Major) | handbook runtime-behavior-verification の A/B に従い tasks 1.2 (修正前) / 1.7 (修正後) に分割、MB-MA-14 を A/B に |
| iOS 互換面テストが tasks にない | 採用 (Major) | tasks 6.3 に 3 ルート全件実行を追加、spec に Scenario「3 テストルートの全件実行」 |
| Sample 通しの Scenario が粗い | 採用 (Major) | 項目別観測を sample-parity の表に紐付け、transition / layout の追加操作と Dialog 系だけの結果観測に限定。Android 面の全項目通しを追加 (tasks 6.2) |
| 公開例外面の検査不足 | 採用 (Minor) | MB-MA-11 に両プロパティの assertion、ApiSurfaceCheck の正の compile 検査を tasks 1.4 に追加 |

採用 9 / 降格 1 / 未解決 0。相方への再提示は不要 (矛盾なし)。
