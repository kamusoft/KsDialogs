# セカンドオピニオン: rename-dialog-contract-singular (spec-001)
**相方**: codex / **label**: so-spec-rename-dialog-contract-singular / **日付**: 2026-09-06 / **対象**: kasane/changes/rename-dialog-contract-singular/ の proposal.md / specs/ / tasks.md / exploration.md と core/ADR-0034
---
# レビュー結果: rename-dialog-contract-singular

**日付**: 2026-09-06  
**判定**: **NEEDS_DISCUSSION**

## サマリー

改名方針そのものは既存 API と整合していますが、実装不能な KMP Scenario と、受け入れ条件を証明できない検証タスクがあります。実装前にスペックと tasks の修正が必要です。

件数: Critical 0 / Major 6 / Minor 3 / Suggestion 1

ビルド・テストは依頼どおり実行していません。レビュー結果ファイルも作成していません。

## 照合した規約

- `ksn-review`、`ksn-core` の delta-spec・domain-axis・decisions・paths
- `kasane/handbook/cross/comment-policy.md`（always）
- `kasane/handbook/cross/test-execution.md`
- `kasane/handbook/cross/sample-parity.md`
- `kasane/handbook/cross/local-development-setup.md`
- `kasane/handbook/cross/user-skill-api-listing.md`
- `.agents/skills/docs-refresh/SKILL.md` の 6-⑧、Step 7
- core/ADR-0002、core/ADR-0034、cross/ADR-0005

## 指摘事項

### [🟠 Major] proposal の domain が変更範囲と一致しない

**該当箇所**: `proposal.md:15`, `proposal.md:35`, `kasane/config.yaml:52`

**問題点**: iOS・Android・KMP・MAUI・cross 文書を横断する変更なのに `domain: core` です。Kasane の domain-axis では複数ドメイン変更は `cross` とする必要があります。このまま実装へ進むと、proposal の domain を正として解決される Swift・Kotlin・C#・MAUI の実装／レビュースキルが読み込まれません。

**推奨修正**: `domain: cross` とし、実際に触る `core / ios / android / kmp / maui` の domain-skills を結合対象として明記してください。

### [🟠 Major] KMP では `Dialog()` を構築できない

**該当箇所**: `specs/dialog-contract/spec.md:30`, `kmp/ksdialogs-kmp/src/commonMain/kotlin/jp/kamusoft/ksdialogs/kmp/Dialog.kt:10`

**問題点**: Scenario は「各形態」で `Dialog()` を作って注入できると読めますが、KMP の `Dialog` は `expect object` であり、利用できるのは `Dialog.instance` だけです。現仕様と直接矛盾しています。

**推奨修正**: Scenario を分割してください。

- iOS／Android／MAUI: 既定エントリと新規 `Dialog()` が共有レジストリを使う
- KMP: `Dialog.instance` を `KsDialog` として注入できる
- KMP の差し替え可能性: fake 実装を注入できることを別 Scenario で検証する

### [🟠 Major] 「旧名ではコンパイルできない」を grep だけで検証している

**該当箇所**: `specs/dialog-contract/spec.md:25`, `tasks.md:13`, `kasane/handbook/cross/test-execution.md:159`

**問題点**: Scenario の期待結果は利用者コードのコンパイルエラーですが、task はソースの残存 grep だけです。これは公開成果物から旧名を解決できないことを証明しません。既存 handbook も禁止公開面は負のコンパイル検証で確認すると定めています。

**推奨修正**: iOS・Android・KMP・MAUI の consumer 境界に、旧名を型として参照すると所定の診断で失敗する負のコンパイル検証を追加してください。既存のフラグ式 negative check と同じく、1フラグ1禁止形状にします。

### [🟠 Major] Sample の検証手順が参照先に存在しない

**該当箇所**: `specs/dialog-contract/spec.md:45`, `tasks.md:14`, `kasane/handbook/cross/test-execution.md:19`, `kasane/handbook/cross/local-development-setup.md:75`

**問題点**: spec と task は `test-execution.md` の手順で Sample も検証するとしていますが、同文書が列挙するのはライブラリのテストルートです。Sample のビルド手順は `local-development-setup.md` にあります。また「samples のテスト」が何を意味するかも未定義です。

**推奨修正**: ライブラリ全テストと Sample ビルドを分け、実行対象を列挙してください。Sample に自動テストがないなら「4 Sample の対象プラットフォーム向けビルド成功」と明記します。

### [🟠 Major] 禁止トークンの退行検証に正の対照がない

**該当箇所**: `tasks.md:25`, `tasks.md:26`, `.agents/skills/docs-refresh/SKILL.md:503`

**問題点**: 改名済みの正常な skills に grep を実行しても、残した禁止パターンが本当に検出できるかは確認できません。極端には正規表現全体を壊しても「検出0件」で task 4.2 が成功します。

**推奨修正**: 一時 fixture または selftest を用意し、次を両方向で確認してください。

- 許可: `KsDialog`、`IKsDialog`、`KsDialogs`、`KsDialogAttributes`
- 禁止: `Ksdialogs`、`ks-dialogs`、`KsDialogsMaui` など残す全パターン

各禁止例が少なくとも1件検出されることまで受け入れ条件にします。

### [🟠 Major] manifest 更新前の整合性ゲートが半分欠けている

**該当箇所**: `tasks.md:22`, `.agents/skills/docs-refresh/SKILL.md:292`, `.agents/skills/docs-refresh/SKILL.md:520`

**問題点**: task 3.5 は 6-②・③・⑥・⑧だけを実行しますが、Step 7 は全整合性チェック通過後に manifest を更新する規律です。6-① concepts 網羅、6-④ frontmatter、6-⑤閉世界性、6-⑦ identity/local-path lint が欠落しています。また 6-⑧ は `scripts/` 内のスクリプトではなく inline grep です。

**推奨修正**: 予定 manifest を入力として8検査すべてを実行し、その予定 manifest と同一内容を最後に書き込むタスクへ直してください。

### [🟡 Minor] Native 契約を参照する wrapper ファイルがタスクに明記されていない

**該当箇所**: `tasks.md:6`, `tasks.md:7`, `maui/macios/native/KsDialogsMauiBridge/MauiDialogBridge.swift:15`, `maui/android/native/ksdialogs-maui-bridge/src/main/kotlin/jp/kamusoft/ksdialogs/maui/MauiDialogBridge.kt:8`, `kmp/ksdialogs-kmp/src/androidMain/kotlin/jp/kamusoft/ksdialogs/kmp/AndroidDialogGateway.kt:18`

**問題点**: iOS／Android タスクは各 Native の tests・samples までしか列挙していませんが、MAUI bridge と KMP gateway も Native 契約型を直接参照しています。ビルドで最終的には検出されるものの、既知の変更対象が tasks から漏れています。

**推奨修正**: wrapper／bridge の参照追随を 1.1・1.2、または独立タスクとして明記してください。

### [🟡 Minor] docs の残存検査がコード span 外を保証しない

**該当箇所**: `specs/user-docs/spec.md:12`, `tasks.md:21`

**問題点**: Requirement は文書全体の契約型名一致を要求しますが、Scenario と task はコード span 内だけを検索します。通常文や表セルに旧契約名が残っても合格できます。

**推奨修正**: Requirement をコード span に限定するか、全 occurrence を列挙して製品名用途を allowlist 判定する検査へ広げてください。

### [🟡 Minor] proposed ADR が新規 ADR の形式を満たしていない

**該当箇所**: `kasane/decisions/core/0034-contract-type-name-singular-feature.md:35`

**問題点**: 新規 ADR に必要な `## Revisit When` がありません。また出典が `2026-09-06 ksn-explore セッション` という辿れない表記で、リポジトリ相対パス規約を満たしていません。

**推奨修正**: `## Revisit When` を追加し、出典を `kasane/changes/rename-dialog-contract-singular/exploration.md` の該当節など、解決可能なパスへ直してください。

### [🔵 Suggestion] 「メンバ署名は不変」の差分検証を追加する

**該当箇所**: `specs/dialog-contract/spec.md:22`, `tasks.md:14`

**問題点**: 既存テストの成功は主要な利用形を守りますが、全メンバ署名が名前以外不変であることを直接比較しません。

**推奨修正**: 変更前後の公開 API 出力を型名だけ正規化して比較するか、4契約の完全な compile surface を固定してください。

## アクションプラン

1. `domain: cross` と実装対象ドメインを確定する。
2. KMP の構築不能な Scenario を形態別に分割する。
3. 旧名の負のコンパイル検証と禁止トークン fixture を tasks に追加する。
4. ライブラリ全テストと Sample ビルドの対象・参照手順を明記する。
5. docs-refresh の8検査すべてを manifest 更新前ゲートにする。
6. wrapper 参照、docs 全文検査、ADR 形式を補完した後、再レビューする。


## 突き合わせ結果 (2026-09-06、ホスト側自己レビュー 2 周との照合)

ホスト側の自己レビューでは manifest の更新漏れ (task 3.5 の新設) のみ検出しており、以下は相方のみの指摘。根拠を実物で確認して採否を決めた。

| # | 指摘 | 採否 | 根拠・反映先 |
|---|---|---|---|
| 1 | domain が core だが複数ドメイン横断 | **採用** (Major) | ksn-core domain-axis.md 52 行「複数該当なら cross」を確認。proposal.md を `domain: cross` + 触るドメインの列挙に修正 |
| 2 | KMP では `Dialog()` を構築できない | **採用** (Major) | `kmp/.../Dialog.kt` が `expect object` であることを確認。Scenario を iOS/Android/MAUI と KMP に分割 |
| 3 | 旧名のコンパイル不能を grep だけで検証 | **採用** (Major) | handbook/cross/test-execution.md「公開 API 形状の検証」に負の検査 (フラグ式 55 本) が規定済み。Scenario を負の検査に書き換え、task 2.2 を新設 |
| 4 | Sample の検証手順が参照先に無い | **採用** (Major) | test-execution.md はライブラリ 4 ルート、Sample は local-development-setup.md「Sample のビルドと実行」。Scenario を分割し task 2.3 / 2.4 に分離 |
| 5 | 禁止トークン検査に正の対照がない | **採用** (Major) | fixture による両方向確認を Scenario と task 4.2 に追加 |
| 6 | manifest 更新前のゲートが 8 検査中 4 つ | **採用** (Major) | docs-refresh SKILL.md Step 7 は全検査通過が前提。task 3.5 を 8 検査 + 予定 manifest 方式に修正 |
| 7 | wrapper (MAUI bridge / KMP androidMain) が tasks に無い | **採用** (Minor) | 3 ファイルの参照を確認。task 1.1 / 1.2 に明記、spec の確認先にも追加 |
| 8 | docs 残存検査がコード span 限定 | **採用** (Minor) | 全 occurrence + allowlist 方式に Scenario と task 3.4 を修正 |
| 9 | ADR に Revisit When が無く出典が辿れない | **採用** (Minor) | ksn-core decisions.md の雛形で Revisit When と出典パスは必須。ADR-0034 に追記 (既存 34 件の ADR には Revisit When が無いが、新規分は雛形に従う) |
| 10 | メンバ署名不変の差分検証 | **降格** (Suggestion) | 署名は触らず、既定ビルドの正の API 形状検査と既存テストの追随で担保される。追加の比較機構は本 change の重さに見合わない |

採用 9 / 降格 1 / 未解決 0。判定は NEEDS_DISCUSSION → 採用分を反映済み (設計判断を要する指摘なし)。
