# セカンドオピニオン: define-loading-action-thread (spec-001)
**相方**: codex / **label**: so-spec-define-loading-action-thread / **日付**: 2026-09-25 / **対象**: 提案一式 (kasane/changes/define-loading-action-thread/ の proposal.md・design.md・specs/・tasks.md・exploration.md と kasane/decisions/core/0037-loading-action-starts-on-ui-thread.md)
---
**判定: NEEDS_DISCUSSION**（Major 3件、Minor 1件）。実装前に、保証の範囲と MAUI の受け入れ条件を確定する必要があります。既存コード・テストとの静的照合のみ行い、ビルド・テストとファイル変更はしていません。

### 🟠 Major — Swift の既定保証に例外がある

**該当箇所**: `kasane/changes/define-loading-action-thread/specs/loading-contract/spec.md:9`、`kasane/changes/define-loading-action-thread/specs/ios-native/spec.md:15`、`kasane/changes/define-loading-action-thread/design.md:64`  
**問題点**: 共通契約と ADR-0037 は「指定なしなら、呼び出し元に関係なく UI スレッドで始まる」としています。一方、設計自身が `start(work)` に非隔離の async 関数を渡すと UI スレッド外で始まると認めています。LD-TH-01〜08 をクロージャだけで通しても、この契約違反は残ります。  
**推奨修正**: 関数参照も含めて保証する API にするか、既定保証の対象を明確に狭めるかを決め、proposal・ADR・デルタスペックを揃えてください。関数参照の実行テストも受け入れ条件に入れてください。

### 🟠 Major — 表示先がない MAUI 経路に「UI スレッド」が存在しない

**該当箇所**: `kasane/changes/define-loading-action-thread/specs/maui-binding/spec.md:13`、`kasane/changes/define-loading-action-thread/specs/maui-binding/spec.md:47`  
**問題点**: `HostlessLoadingGateway` も `Main` の対象ですが、素の .NET 実行環境には UI スレッドがありません。`maui/KsDialogs.Maui/Internals/DialogPresenter.cs:119` もその場合はその場で実行すると定めています。偽物の専用スレッドを通す LD-HM-06 は、実環境での `Main` の意味を定められません。  
**推奨修正**: 表示先がない場合の `Main` の挙動と、共通契約の適用範囲を明記してください。既存の「提示先がなくても action を実行する」契約との整合も必要です。

### 🟠 Major — 元の MAUI iOS 不具合を判定する実行条件が未決

**該当箇所**: `kasane/changes/define-loading-action-thread/design.md:164`、`kasane/changes/define-loading-action-thread/tasks.md:49`  
**問題点**: Runner の偽物を使うテストは振り分けを証明しますが、実際の MAUI iOS で C# action がメインスレッドから始まることは証明しません。互換面のテストだけでも managed 側の移送は通りません。元の `UIKitThreadAccessException` を解消したと判断する条件が Open Questions に残っています。  
**推奨修正**: 実際の MAUI ホストで action の最初の文の `MainThread.IsMainThread` を、`Main` と `Background` で観測する受け入れ条件を tasks に定めてください。この移送が欠ければ失敗する観測が必要です。

### 🟡 Minor — KMP の Swift 公開面に検証がない

**該当箇所**: `kasane/changes/define-loading-action-thread/specs/kmp-facade/spec.md:24`、`kasane/changes/define-loading-action-thread/design.md:141`  
**問題点**: Swift に書き出される `start` の引数追加は破壊的変更ですが、LD-HK-01〜04 は共有コードと gateway だけを検査します。生成された Objective-C 面に想定どおり現れ、Swift 利用者が呼べるかは未検証です。  
**推奨修正**: 生成ヘッダの形状確認と、Swift から公開される各 `start` を新引数で呼ぶ正のコンパイル検査を追加してください。

優先順は、**Swift の保証範囲 → hostless の契約 → MAUI 実行時の受け入れ条件 → KMP Swift 公開面の検査**です。

## 突き合わせ結果

ホスト側の自己レビュー (2 周、チェックリスト通過) との突き合わせ。

| # | 指摘 | 重要度 | 採否 | 反映 |
|---|---|---|---|---|
| 1 | Swift で isolation を持つ関数を名前で渡すと、共通契約の「呼び出し元に関係なく UI スレッド」の例外になるのに、契約と ADR に書かれていない | Major | 採用 (相方のみ・根拠強。ホスト側は ios-native デルタと design にだけ書いていた) | loading-contract デルタ・ADR-0037 の Decision・proposal の What Changes に範囲の定めを追記。関数参照の実行テストは既存の LD-HI-02 |
| 2 | 表示先の無い MAUI 経路 (`HostlessLoadingGateway`、素の .NET でだけ使われる) には UI スレッドが存在せず、LD-HM-06 が実環境での意味を定めていない | Major | 採用 (相方のみ・根拠強。ホスト側の見逃し) | 契約の範囲を UI スレッドを持つ環境に限定し、素の .NET では指定に関係なくその場で実行すると定めた (loading-contract・maui-binding デルタ・ADR-0037・proposal・design)。LD-HM-06 と tasks 3.4 を差し替え |
| 3 | MAUI iOS の元の不具合を解消したと判定する、実際の MAUI ホストでの観測条件が未決 | Major | 確定 (双方一致。ホスト側は design の Open Questions に残していた) | 判断が要るためオーナーに提示 (NEEDS_DISCUSSION 相当) |
| 4 | KMP を Swift から呼ぶ面 (書き出される Objective-C ヘッダ) に検証が無い | Minor | 採用 (相方のみ。箇所が特定され、破壊的変更の確認漏れという実害があるため降格しない) | tasks 4.5 (ヘッダの確認と証跡) を追加 |

確定 1 件 (指摘 3) / 採用 3 件 (指摘 1・2・4) / 降格 0 件。未解決 1 件: 確定した指摘 3 の対応方法がオーナー判断待ち。

### 未解決の解消 (2026-09-25)

指摘 3 はオーナーが A 案を選んで解消した。4 ルートの Sample の `Default Loading` のデモで、action の最初の文から結果表示を `結果: 処理中` に直接更新し、MAUI iOS (Debug 構成) を含む実機 / Simulator で撮って確かめる (design Decision 6・samples デルタ LD-HS-02・tasks 5.2〜5.5)。
