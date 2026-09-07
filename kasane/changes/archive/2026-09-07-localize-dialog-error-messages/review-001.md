# レビュー結果: localize-dialog-error-messages (001 回目)

**日付**: 2026-09-07
**判定**: APPROVED

## サマリー

61 件の診断文言の英語化は、4 形態すべてでデルタスペックの対応表と機械照合して 1 文字違わず一致していた (対応表の 55 行を実装ソースへ突合し未検出 0 件、逆向きにライブラリ本体の長い文字列リテラルを走査して対応表に無い訳語 0 件)。公開契約 (case 名・例外型・引数・throw 条件) は文字列リテラル以外に差分が無く不変で、追加テスト 5 本はいずれも実物の値 (`errorDescription` / `message` / `Message` / 警告本文) を読んで完全一致または部分一致で検査しており、期待値を書き換えただけで通る作りにはなっていない。テストは 7 ルート中 6 ルートを再実行して全件成功・件数も handbook の更新値と一致した。指摘は Minor 1 件と Suggestion 1 件で、いずれも本務の正しさではなく検査の目減りと周辺の紛らわしさに関するもの。

## 照合した規約

| 文書 | 適用のきっかけ |
|---|---|
| `kasane/handbook/cross/comment-policy.md` | always (全ソースを触るため) |
| `kasane/handbook/cross/test-execution.md` | テストの実行・結果の報告・完了判定 (本 change の付随修正対象でもある) |
| `kasane/handbook/cross/user-skill-writing-style.md` | `skills/**` の references 改稿・ja → en 同期 |
| `kasane/handbook/cross/user-skill-api-listing.md` | `skills/**` の更新・parity 検査の仕分け |
| `kasane/decisions/cross/0015-diagnostic-messages-english-only.md` | 本 change の根拠 ADR (accepted) |
| `kasane/lessons/process.md` L-002 | 「破壊的変更なし」の主張の書き方 |
| `swift-ui-impl-skill` / `kotlin-impl-skill` | domain-skills の code-review 解決結果 |

## 実行した検証

| 検証 | 結果 |
|---|---|
| ios/ `xcodebuild test -scheme KsDialogs` | **277 tests / 50 suites 成功**。xcresult に `DM-IO-01` / `DM-IO-02` の実行を確認 |
| android/ `./gradlew test --rerun-tasks` | **68 tests / 0 failures** (`DialogExceptionMessageTests` を含む) |
| kmp/ `./gradlew allTests --rerun-tasks` | **96 tests / 0 failures** |
| maui/ `dotnet test` | **155 tests / 0 failures** (net10.0-android / net10.0-ios のビルドも通過 = tasks 4.8 の裏取り) |
| maui/android/native/ `./gradlew :ksdialogs-maui-bridge:test --rerun-tasks` | **31 tests / 0 failures** (`MauiBridgeDiagnosticMessageTests` を含む) |
| maui/macios/native/ `xcodebuild test` | **7 tests / 4 suites 成功**。`[DM-MA-03]` の実行を確認 |
| android/ (instrumented) | **再実行していない** — 別プロセスが同じ出力先へ書き込み中だったため (handbook の「android 系 Gradle を同時に走らせない」に従い回避)。ディスク上の 2026-09-07T04:50 の結果は `:ksdialogs` 294 / `:ksdialogs-compose` 39 / failures 0 で、handbook の 333 と一致 |
| `scripts/scenario-id-coverage.py --specs .../specs` | 終了コード 0 (10/14 網羅・除外 4・未網羅 0) |
| `scripts/scenario-id-coverage.py` (既定) | 未網羅 20 件はすべて `add-kmp-typed-show` 由来。DM-* は 0 件 = deviation の記述どおり |
| tasks 6.4 の日本語リテラル grep | 0 行 (証跡と同じ結果を再現) |
| `comment-policy-lint.py` / `local-path-lint.py` / `identity-lint.py` | 禁止 0 件 / 検出 0 件。要確認 (advisory) はすべて本 change 以前からの既存分と、テストクラスの doc コメント中の ADR 参照 (既存テスト多数と同じ書き方で、ライブラリ利用者から見える公開メンバーではない) |
| docs-refresh parity 4 本 | code-block byte 一致 / heading OK / frontmatter OK / link は既存の `kssettingsview-maui` 欠落のみ (本 change 外) |
| 対応表 ⇔ 実装の機械照合 | 対応表 55 行のプレースホルダ外断片が全件ソースに存在。逆向き走査で対応表に無い診断文言 0 件 |
| 対応表 ⇔ Skills の機械照合 | 24 ファイルの診断表のメッセージ列が全件対応表と一致。en / ja のメッセージ列は列順まで byte 一致。対応表の ja 文言 34 パターンの `skills/` 残存 0 件 |

## 指摘事項

### [🟡 Minor] 追随後の `Does.Contain("中身を作れません")` が、ライブラリの警告本文ではなくテスト自身が投げた例外の message にしか当たらなくなっている

**該当箇所**: `maui/KsDialogs.Maui.Tests/BridgeContentSupplyTests.cs:158` / `:185`

**問題点**: `BridgeContentSupply` の警告書式は `表示の中身を作れませんでした。{0}: {1}` → `Could not create the presentation content. {0}: {1}` に変わった。置き換え前は `Does.Contain("中身を作れません")` が**書式側の本文**にも当たっていた (「表示の中身を作れませんでした。」は「中身を作れません」を含む) が、置き換え後にこの部分文字列と一致するのは `{1}` に埋め込まれる**テスト自身が投げた** `new InvalidOperationException("中身を作れません")` の message だけになった。結果として、この 2 本のテストは「警告が何の失敗かを述べていること」を検証しなくなり、`Create` の書式本文を将来どう書き換えても通り続ける。

deviation.md は MAUI 側の追随を「部分一致 2 件 (`Does.Contain("破棄")` / `Does.Contain("失敗として返")`)」と記録しているが、同じアサーション群の前半 2 行が旧文言と二重に噛み合っていた事実が拾えていない。`BridgeContentSupply` の警告本文には他の検査が無く (`maui/KsDialogs.Maui.Tests/` で `Warnings[0]` を見るのはこの 4 行だけ)、DM-MA-05 も静的 grep の Scenario なので文言の中身は見ない。

**推奨修正**: 2 箇所の `Does.Contain("中身を作れません")` を `Does.Contain("Could not create the presentation content")` に変え、テスト自身の投げる文言と検査対象を分離する (元の例外が警告に載ることも見たいなら、そのアサーションを 1 行足す)。deviation.md の MAUI 追随の内訳も「部分一致 4 箇所」に直す。

### [🔵 Suggestion] 共有コードのテストが、実装から消えた日本語文言を偽 gateway の失敗メッセージとして持ち続けている

**該当箇所**: `kmp/ksdialogs-kmp/src/commonTest/kotlin/jp/kamusoft/ksdialogs/kmp/DialogResultRouteTests.kt:55` / `:59`

```kotlin
val dialogs = GatewayKsDialog(TestDialogGateway.failing("View factory が登録されていません。"))
...
assertEquals("View factory が登録されていません。", failure.message)
```

**問題点**: テストが自分で用意する偽 gateway の文言なので proposal の Non-Goals (「テストが自分で投げる例外の文言」) の対象で、**違反ではない**。ただしこの文字列は旧実装の `DialogException.ViewFactoryNotRegistered` の文言そのもので、実装にはもう存在しない。共有コードの素通しを見るテストなので中身は任意でよく、実装文言を模したまま残ると「共有コードは日本語の説明を運ぶ」と読める。cross/ADR-0015 の後に KMP の失敗経路を読む人を迷わせる。

**推奨修正**: 素通しを見るだけなので、実装文言を模さない中立な文字列 (`gateway failure` など) に置き換える。本 change のスコープ外として残す判断なら、蒸留で「Non-Goals として意図的に残した箇所」として記録しておく。

## 確認して問題が無かった点 (指摘なし)

- **公開契約の不変**: 全 diff で変わったのは文字列リテラルのみ。`DialogError` の case・`DialogException` のサブクラス / 入れ子型・`MauiDialogBridgeError` の case・引数・throw 条件・例外型はいずれも差分なし
- **対になる警告ログの揃え**: iOS / Android のフック失敗・フック未完了・duration 不正 (2 種)・Toast の中身生成失敗の 5 対はすべて本文テンプレートが一致。差分は spec が明示した 2 点 (iOS は `{error}` を本文に埋め、Android は `Log.w` の throwable 引数に渡す) のみ。同一化の対象外とされた iOS の添付値未達 3 件と Android の `DialogLayoutHost` 収束警告 (機能名を持たない文言) も spec の除外どおり
- **コメントの日本語維持**: 触った 25 ソースのコメント・doc comment に英語化は 1 件も無い。`BridgeTestFailure.swift` のように定数だけ英語になりコメントが日本語のままの箇所も、コメントの記述内容と実体が食い違っていない
- **足場の凍結**: `proposal.md` / `specs/**` / `exploration.md` に差分なし。`tasks.md` の差分はチェックボックスの反転のみ (6.2 は未チェックのままで、理由が deviation にある)
- **deviation の付随修正 2 件**: `kasane/handbook/cross/test-execution.md` の 1 ファイルに閉じており、本 change の tasks 6 が完了判定の根拠として名指しする文書。公開 API・ADR に触れず、局所的で、記載された件数は 6 ルートを再実行して実測と一致した (ksn-core の同梱条件を満たす)
- **Skills の形**: 列構成・見出し・コードブロックは無変更 (code-block-parity が byte 一致で通過)。追加された「安定 API ではない」の 1 文は en / ja とも診断表の直後に置かれ、iOS だけ「case と throw される条件」、他 3 形態は「例外型と throw される条件」と対象に合わせて書き分けられている
- **`{TypeName}` プレースホルダ**: en / ja とも `{型名}` から `{TypeName}` へ揃っており、`resultTypeMismatch` の 2 スロットを両方 `{TypeName}` にしているのも現行 (`{型名}` 2 つ) と同じ形
- **scenario-id-coverage.py の除外登録**: DEFAULT_ALLOW_MISSING の 4 件と先頭コメントの理由列挙の両方に追記済み (tasks 6.0 の到達状態)

## アクションプラン

1. `maui/KsDialogs.Maui.Tests/BridgeContentSupplyTests.cs:158` / `:185` の部分一致を英語文言に差し替え、deviation.md の MAUI 追随の内訳を実測 (4 箇所) に直す — Minor
2. `DialogResultRouteTests.kt:55` / `:59` の偽 gateway 文言を中立な文字列に置き換える (スコープ外として残すなら蒸留で記録) — Suggestion
3. instrumented の全件実行だけは本レビューで再現していない。完了判定の前に、実行中のものが終わった時点で件数 (`:ksdialogs` 294 + `:ksdialogs-compose` 39) を確認する
