# セカンドオピニオン: add-maui-ios-bridge-verification (spec-001)
**相方**: codex / **label**: so-spec-add-maui-ios-bridge-verification / **日付**: 2026-09-02 / **対象**: kasane/changes/add-maui-ios-bridge-verification/ の proposal.md / specs/maui-binding/spec.md / tasks.md (提案一式、実装前)
---
# レビュー結果: add-maui-ios-bridge-verification

**日付**: 2026-09-02  
**判定**: NEEDS_DISCUSSION

## サマリー

ビルド追随の受け入れ基準は概ね検証可能ですが、テスト側に未確定の実現経路と Scenario 網羅検査の矛盾があります。特に、提示先プローブが失敗した場合は承認済み spec をそのまま実装できず、代替設計についてオーナー判断が必要です。

静的レビューのみ実施し、ビルド・テストは実行していません。

## 照合した規約

- `kasane/handbook/cross/comment-policy.md`（always）
- `kasane/handbook/cross/test-execution.md`
- `kasane/handbook/cross/runtime-behavior-verification.md`
- `kasane/handbook/cross/sample-parity.md`
- `kasane/decisions/maui/0003-build-wiring-xcodeproject-and-gradle-exec.md`
- `kasane/decisions/cross/0006-samples-aggregated-consumer-boundary.md`
- `kasane/decisions/core/0033-user-factory-failure-boundary.md`
- `kasane/lessons/spec-review.md`

## 指摘事項

### [🟠 Major] 自動テストを持つ BV-MA-01〜03 まで coverage 除外する記述になっている

**該当箇所**: `kasane/changes/add-maui-ios-bridge-verification/specs/maui-binding/spec.md:3`

**問題点**: BV-MA-01〜03 を自動テストで固定すると述べながら、同じ行で BV-MA-01〜06 をすべて `scenario-id-coverage` の除外 ID としています。これは、テスト名を走査対象にする SHALL（同ファイル:42）および「BV-MA-04〜06 だけを除外する」tasks（`tasks.md:17`）と矛盾します。01〜03 も除外すると、将来テストが削除されても網羅検査が失敗しません。

**推奨修正**: 除外対象を BV-MA-04〜06 のみに限定し、BV-MA-01〜03 はテスト宣言による必須網羅と明記してください。

### [🟠 Major] 提示先確保が未実証のまま必須 Scenario になっている

**該当箇所**: `kasane/changes/add-maui-ios-bridge-verification/proposal.md:32`

**問題点**: 提示面の注入口を Non-Goal とする一方、実提示先が得られない場合は実装を停止して注入口追加を判断するとしています（同ファイル:39、`tasks.md:8`）。しかし spec の BV-MA-01〜03 は「提示先が確保できる」を確定済み GIVEN としており、プローブ不成立時には凍結された spec を満たす実装経路がありません。既存テストも `UIApplication.shared.connectedScenes` を通す実提示ではなく、選択ロジックへ snapshot を直接与えているだけです（`ios/Tests/KsDialogsTests/ApplicationKeyWindowProviderTests.swift:25`）。

**推奨修正**: 提案承認前にプローブを実施して実提示経路を成立済みにするか、テスト専用注入口を採用するかを先に決定してください。後者なら可視性、公開 API/ABI への影響、3面すべてが同じ seam を使うかを proposal と tasks に確定させる必要があります。

### [🟠 Major] Loading の独立した2入口のどちらを固定するか決まっていない

**該当箇所**: `kasane/changes/add-maui-ios-bridge-verification/specs/maui-binding/spec.md:16`

**問題点**: 「開始を要求する」だけでは `showContent:completion:` と `startContent:action:completion:` のどちらを指すか判定できません。実装では前者が `beginUse`、後者が `runScope` という別経路です（`maui/macios/native/KsDialogsMauiBridge/MauiLoadingBridge.swift:62`、同ファイル:82）。tasks もテスト1件しか指定していないため、一方だけを検証して BV-MA-02 を満たした扱いにできます。

**推奨修正**: 両入口を保証対象にして Scenario を分割するか、対象入口を明記し、対象外の入口を Non-Goal としてください。公開 MAUI API のどの呼び出しを代表するかも併記すると判定可能になります。

### [🟠 Major] Toast の受け入れ基準に対して、予定している観測点が弱い

**該当箇所**: `kasane/changes/add-maui-ios-bridge-verification/tasks.md:15`

**問題点**: 「Android の同役テストと揃える」とありますが、Android テストが確認するのは nil が器で捕捉可能な例外になるところまでです（`maui/android/native/ksdialogs-maui-bridge/src/test/kotlin/jp/kamusoft/ksdialogs/maui/MauiToastContentSupplyTests.kt:20`）。一方、BV-MA-03 は「別の Toast が表示されたまま」「対象の1枚だけ破棄」という、より強い coordinator 結果を要求します。iOS bridge の coordinator は private であり（`maui/macios/native/KsDialogsMauiBridge/MauiToastBridge.swift:16`）、単に `MauiToastViewModel.makeContentView()` の throw を確認するだけでは Scenario を満たしません。

**推奨修正**: 公開 `MauiToastBridge.show` を通し、既存 Toast の器または content view の同一性・表示継続と、失敗 Toast の非追加をどこで観測するかを tasks に明記してください。直接観測できない場合は、提示面と同様にテスト seam の設計判断が必要です。

### [🟡 Minor] テスト標的プローブが0件実行でも成功扱いになる

**該当箇所**: `kasane/changes/add-maui-ios-bridge-verification/tasks.md:7`

**問題点**: 「0件以上」は0件を許し、`xcodebuild test` がテストを発見していない偽の green を排除できません。テスト実行規約も、終了コードだけでなく実行件数の確認を要求しています。

**推奨修正**: 最小の probe テストを置いて「1件以上」とし、Swift Testing の `Test run with N tests` 行から件数を確認する条件にしてください。

## アクションプラン

1. 提示先プローブと、失敗時のテスト seam 方針を提案承認前に確定する。
2. Loading の `Show` / `Start` の保証範囲を決める。
3. BV-MA-01〜03 を coverage 除外から外す。
4. Toast の観測方法と、テスト標的プローブの最低件数を明文化する。

**指摘件数**: Critical 0 / Major 4 / Minor 1 / Suggestion 0

**総合判定: NEEDS_DISCUSSION**


## 突き合わせ結果 (2026-09-02、ホスト側自己レビュー 2 周との照合)

| # | 相方の指摘 | 採否 | 根拠・反映先 |
|---|---|---|---|
| 1 | [Major] BV-MA-01〜03 まで coverage 除外する記述 | **降格 → 表現修正として反映** | ホスト側の意図は 04〜06 のみ除外だったが、spec.md:3 の「いずれも」が 01〜06 全体に読める曖昧な文だった。除外対象を 04〜06 に限定し 01〜03 (07) を必須網羅と明記 (specs/maui-binding/spec.md) |
| 2 | [Major] 提示先確保が未実証のまま必須 Scenario | **採用 (相方のみ・根拠強)** | ホスト側は Impact でリスクとして挙げ tasks 先頭のプローブに回していたが、相方の「凍結 spec を満たす経路が無い」指摘どおり承認前に決めるべき論点だった。提案段階でプローブを実施し**不成立**を実測 (verification/presentation-host-probe.md)。オーナー決定 (2026-09-02): テスト用ホストアプリ方式 (A) を採用、注入口案 (B) と解決規則の緩和 (C) は却下 → proposal What Changes / Non-Goals / Impact、tasks 1.1 / 1.2 に反映 |
| 3 | [Major] Loading の 2 入口 (show / start) のどちらを固定するか未定 | **採用 (相方のみ・根拠強)** | 該当箇所特定あり (MauiLoadingBridge.swift の beginUse / runScope)。コードで確認: スコープ形は中身解決の失敗で処理を走らせず失敗通知のみ。BV-MA-02 を表示形、新設 BV-MA-07 をスコープ形に分割 (spec / tasks 2.3・2.3b) |
| 4 | [Major] Toast の観測点が Android 同役テストより弱い要求に対して不足 | **採用 (相方のみ・根拠強)** | coordinator は private で throw の確認では Scenario を満たさない、は正しい。Toast の器は key window に直接載る (`KeyWindowToastPresentationSurface`) ため、公開 `MauiToastBridge.show` を通し key window の view 階層で「器が増えない・既存の器が残る」を観測する形を tasks 2.4 に明記 |
| 5 | [Minor] テスト標的プローブが 0 件でも成功扱い | **採用** | handbook/cross/test-execution の「件数確認までが検証」と整合。tasks 1.1 を「最小テスト 1 本・1 件以上」に修正 |

確定: 0 / 採用: 4 / 降格 (表現修正として反映): 1 / 未解決: 0。オーナーによる的外れ却下なし (ksn-lesson の捕捉なし)。相方の総合判定 NEEDS_DISCUSSION の論点 (#2) はオーナー決定で解消。
