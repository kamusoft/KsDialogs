# セカンドオピニオン: add-model-binding-di (spec-001)
**相方**: codex / **label**: so-spec-add-model-binding-di / **日付**: 2026-08-24 / **対象**: kasane/changes/add-model-binding-di/ の提案一式 (proposal / design / specs 6件 / tasks)
---
# レビュー結果: add-model-binding-di

**日付**: 2026-08-24  
**判定**: **NEEDS_DISCUSSION**

## サマリー

中核となる C# 型指定 API が現状のままでは型安全に宣言できず、参照型制約、notifier の後始末、MAUI DI の生成規則にも仕様上の未決事項があります。このまま実装へ進むと ADR の supersede や公開 API の再設計が実装途中で必要になるため、足場を確定し直す必要があります。

指摘件数: Critical 1 / Major 7 / Minor 1 / Suggestion 0。指定どおり静的レビューのみで、ビルド・テスト・ファイル書き込みは行っていません。

## 指摘事項

### [🔴 Critical] C# の型指定 ShowAsync は宣言結果型を導出できない

**該当箇所**: `design.md:55`、`design.md:93`、`specs/maui-binding/spec.md:9`

**問題点**: 提示された `Task<DialogResult<TResult>> ShowAsync<TViewModel>(...)` では `TResult` がメソッド型引数として宣言されていません。仮に `where TViewModel : IDialogViewModel<TResult>` を付けても、C# は制約から未指定の型引数を導出できません。現行 API が結果型を推論できるのは、`maui/KsDialogs.Maui/Presentation/IKsDialogs.cs:39` のように `IDialogViewModel<TResult>` 型の値引数が存在するためです。

これは Open Question として実装時まで先送りできる細部ではなく、MAUI の型指定呼び出しという主要 Requirement の成立性そのものです。MB-MA-01 を実装後の停止ゲートにしても、仕様どおりの実装が不可能だと判明するだけです。

**推奨修正**: 実装前に次のいずれかを選び、公開シグネチャと compile Scenario を完全に確定してください。

- Bool の省略形だけ `ShowAsync<TViewModel>() where TViewModel : IDialogViewModel` とし、カスタム結果は `ShowAsync<TViewModel, TResult>()` にする
- カスタム結果を含め、常に ViewModel 型と結果型の2型引数を要求する
- 型指定経路だけ別の戻り値設計または別名にし、core/ADR-0020 を supersede する

### [🟠 Major] 「VM 契約は参照型限定」が C#／Kotlin では保証されない

**該当箇所**: `proposal.md:30`、`specs/dialog-contract/spec.md:9`、`specs/ios-native/spec.md:16`、`tasks.md:12`

**問題点**: Swift には AnyObject の負の compile 検査がありますが、C# の `struct` と Kotlin の value class も現在の interface 契約へ準拠できます。C# の `ConditionalWeakTable` や Kotlin の参照同一性テーブルへ値型を渡すと、boxing ごとに別オブジェクトとなり、`vm.Notifier` の取得や並行 show 判定が成立しません。

proposal は全形態の破壊的な class 限定化を宣言している一方、MAUI／Android のデルタスペックと tasks には、その制約を実現・検証する契約がありません。

**推奨修正**: C#／Kotlin での保証方法を明記してください。コンパイル時に限定できない場合は、登録・show・notifier 取得時の値型拒否を構成ミスとして仕様化し、C# の struct と Kotlin value class に対する負の compile または実行時 Scenario を追加してください。

### [🟠 Major] notifier の除去が正常な結果配送しか定義していない

**該当箇所**: `specs/dialog-contract/spec.md:9`、`specs/dialog-contract/spec.md:21`、`design.md:51`、`tasks.md:8`

**問題点**: 除去時点は「結果配送後」とされていますが、notifier 紐付け後には View factory、fallback resolver、DI 解決、提示処理が失敗し得ます。特に C#／Kotlin の factory は例外を投げられます。この経路では結果配送が発生しないため、サイドテーブルに古い紐付けが残り、同じ VM の次回 show が「並行 show」と誤判定される可能性があります。

呼び出し元キャンセルや OS 発の器消失についても、除去が呼び出し元への配送前なのか、内部撤去完了時なのかが明確ではありません。

**推奨修正**: 「紐付けに成功した全経路で、正常配送・例外・キャンセル・提示失敗を問わず必ず除去する」「呼び出し元へ結果／例外を渡す前に除去が観察可能である」といった terminal-path 契約を定めてください。factory 例外、提示失敗、キャンセル後に notifier が空となり、同じ VM を再利用できる Scenario も必要です。

### [🟠 Major] VM factory と View factory の2スロット登録規則が未定義

**該当箇所**: `design.md:42`、`specs/dialog-contract/spec.md:43`、`specs/maui-binding/spec.md:37`

**問題点**: レジストリエントリを「View factory + 任意の VM factory」に変更しますが、個別登録時のマージ規則がありません。

例えば次が決まっていません。

- VM factory 登録後に View factory を再登録すると、VM factory を保持するのか
- `RegisterForDialog` 後に低水準 Register で View だけ差し替えると、VM factory は残るのか
- View と VM の fallback 判定はスロットごとに独立するのか
- 同時登録・解決時にどの時点のペアをスナップショットとして使うのか

MB-MA-08 は「明示 View + VM fallback」だけを扱っており、逆方向や再登録を判定できません。

**推奨修正**: 再登録はスロット単位の後勝ちで他方を保存するのか、エントリ全体を置換するのかを明記してください。少なくとも「明示 VM factory + View fallback」「登録順を変えた再登録」「RegisterForDialog 後の片側差し替え」の Scenario を追加してください。

### [🟠 Major] configure の実行コンテキストと失敗規則が決まっていない

**該当箇所**: `design.md:49`、`specs/dialog-contract/spec.md:43`

**問題点**: 現行 show は任意スレッドから呼べますが、VM factory と configure をどの executor／actor で実行するかがありません。Swift の例示シグネチャにも `@MainActor` がなく、Android でも caller dispatcher と Main のどちらか決まりません。UI バインドされる VM の状態変更が形態ごとに異なるスレッドで走る可能性があります。

また Kotlin configure の例外・CancellationException、C# の faulted/cancelled Task をどう伝播し、View を生成しないことをどう保証するかも未規定です。

**推奨修正**: Swift MainActor、Android Main dispatcher、MAUI UI threadなど、形態別の実行保証を決めてください。呼び出し側コンテキストで実行する設計なら、その制約を公開契約へ明記してください。configure の例外・キャンセル時は提示せず、その失敗を伝播する Scenario も追加してください。

### [🟠 Major] RegisterForDialog の「1行登録」と DI 生成規則が曖昧

**該当箇所**: `specs/maui-binding/spec.md:21`、`specs/maui-binding/spec.md:25`、`specs/maui-binding/spec.md:30`、`design.md:61`

**問題点**: MB-MA-03 は `RegisterForDialog` だけで動く構成を要求しますが、TView／TViewModel を IServiceCollection に登録する主体と lifetime が決まっていません。一方 MB-MA-04 は TViewModel の別途サービス登録を前提としており、「1行登録」と整合していません。

さらに TView を通常の DI 解決で生成すると、TView のコンストラクタが TViewModel を受け取る一般的な MAUI 構成では、コンテナが別の VM を生成した後に `BindingContext` だけが show 対象 VM へ上書きされます。View 内部が保持する VM と BindingContext が別インスタンスになり得ます。

fallback View についても、現在の VM を BindingContext に設定する義務が明記されていません。

**推奨修正**: 次を公開契約として確定してください。

- RegisterForDialog が TView／TViewModel を自動登録するか
- 自動登録する場合の lifetime と既存登録に対する TryAdd／上書き規則
- TView の生成に現在の VM インスタンスを渡す方法、または View の VM コンストラクタ注入を禁止する制約
- fallback View に現在の VM を供給する方法
- provider holder の寿命、対象 registry、複数 ServiceProvider が存在した場合の扱い

同一 VM が View のコンストラクタ側と BindingContext 側へ届き、生成回数も1回であることを検証する Scenario が必要です。

### [🟠 Major] KMP iOS notifier アクセサの結果型不一致時の挙動がない

**該当箇所**: `specs/kmp-facade/spec.md:7`

**問題点**: `notifier(for: vm, result: R.self)` は利用者が結果型を自己申告しますが、登録時の結果型と異なる型を渡した場合の挙動がありません。単に nil を返す場合、「show 外なので取得不能」と「結果型不一致」を区別できず、View が報告できないまま show が終了しない可能性があります。

既存の KMP Swift 面は結果型不一致を typed error とする決定を持つため、ここだけ無言の nil にすると公開契約が非対称になります。

**推奨修正**: 結果型不一致を typed error、構成ミス、nil のいずれにするかを明記し、`result:` 省略時に Bool 以外で登録された VM を渡す場合も含めて Scenario を追加してください。

### [🟠 Major] UI を変更する L 級提案に ui/ 足場がない

**該当箇所**: `specs/samples/spec.md:7`

**問題点**: 4ルートへ新しいメニュー項目とダイアログ内容を追加する UI 変更ですが、`kasane/changes/add-model-binding-di/ui/` が存在しません。ksn-core では UI に触れる変更は級に関係なく ui/ が必要で、L 級には S 級微調整の省略例外も適用されません。

既存 Basic Dialog と完全に同じ見た目を再利用する意図であっても、どの既存画面を見た目の正とするかが足場に記録されていません。

**推奨修正**: 少なくとも `ui/brief.md` に Model Dialog の構造、状態、既存デザインの再利用範囲を記録してください。新しい見た目がある場合は `ui/mock/` と `approved.png` を用意し、既存デザインの完全再利用なら、その参照と差分なしの判断を brief に明記してください。

### [🟡 Minor] 「View factory 呼び出し前」の注入順序を Scenario が検証できない

**該当箇所**: `specs/dialog-contract/spec.md:9`、`specs/dialog-contract/spec.md:11`、`specs/dialog-contract/spec.md:31`

**問題点**: Requirement は factory 呼び出し前の紐付けを要求し、design も factory 内から notifier を読めることを採用理由にしています。しかし MB-NI-01／05 は、生成後の View が操作時に notifier を読む実装でも通ります。factory が返った後に紐付ける誤実装を検出できません。

**推奨修正**: factory 実行中に `vm.notifier` を読み、非空であること、および2引数 factory の notifier と同じ結果チャネルへ配送されることを直接検証する Scenario を追加してください。

## アクションプラン

1. C# の型指定 show と参照型制約を先に決定し、必要なら core/ADR-0020 を supersede する。
2. notifier の全 terminal path における除去と、configure の実行・失敗契約を仕様化する。
3. レジストリの2スロット更新／fallback 規則を確定する。
4. MAUI のサービス登録、lifetime、同一 VM の View 供給、provider holder の寿命を確定する。
5. KMP iOS の結果型不一致契約と不足 Scenario を追加する。
6. Sample 変更用の UI 足場を作成した後、再レビューする。



## 突き合わせ結果 (2026-08-24)

ホスト側自己レビュー (2周・通過) との突き合わせ。9件すべて相方のみの指摘であり、いずれも該当箇所の特定と実害シナリオを伴う根拠強のため**全件採用** (降格・未解決なし)。ホスト側の見逃しとして扱う。

| # | 指摘 | 採否 | 解決方針 |
|---|---|---|---|
| 1 | C# 型指定 ShowAsync の結果型導出不能 (Critical) | 採用 | 既存 Register と同型の2型引数形 (bool 省略形 `ShowAsync<TVm>()` + `ShowAsync<TVm, TResult>()`)。動詞は ShowAsync のままで core/ADR-0020 の supersede 不要 |
| 2 | 参照型限定が C#/Kotlin で未保証 | 採用 | C# は generic の `class` 制約 (負の compile 検査追加)、Kotlin は登録・型指定 show で value class を構成ミス拒否 (実行時 Scenario 追加) |
| 3 | notifier 除去が正常配送のみ | 採用 | terminal-path 契約 (紐付け後の全終端経路で除去・呼び出し元への配送/例外伝播前に観察可能) + factory 例外後の再 show Scenario |
| 4 | 2スロット登録のマージ規則未定義 | 採用 | スロット単位の後勝ち (再登録は該当スロットのみ置換・他方保持)・fallback 判定はスロット独立・解決は show 時スナップショット + Scenario 追加 |
| 5 | configure の実行コンテキスト/失敗規則 | 採用 | VM factory と configure は View factory と同じ UI スレッド保証。例外・キャンセルは提示せず伝播 + Scenario 追加 |
| 6 | RegisterForDialog の DI 生成規則 | 採用 | TView/TViewModel を TryAdd で transient 自動登録・TView は現在の VM を引数に生成 (ctor 注入と BindingContext の同一性・生成1回)・fallback View の BindingContext はライブラリが設定・provider holder の寿命を明記 + Scenario 強化 |
| 7 | KMP アクセサの結果型不一致 | 採用 | 既存 KMP 面の決定と対称の typed error。show 外の nil と区別 + Scenario 追加 |
| 8 | ui/ 足場の欠落 | 採用 | ui/brief.md を追加 (既存 Basic Dialog デザインの完全再利用を記録)。mock の要否はオーナー判断 (推奨: 新規見た目なしのため省略) |
| 9 | 注入順序の検証不能 (Minor) | 採用 | MB-NI-01 を factory 実行中の notifier 非空を直接検証する形に強化 |

解決方針 #1 (API 形) と #8 (mock 省略) はオーナー確認を経て反映する。
