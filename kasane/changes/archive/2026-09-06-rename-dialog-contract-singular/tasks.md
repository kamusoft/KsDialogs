# Tasks: rename-dialog-contract-singular

境界規則 (全タスク共通): 改名するのは契約型 `KsDialogs` / `IKsDialogs` と、契約名を冠する実装・fake (`GatewayKsDialogs` / `FakeKsDialogs`) のみ。製品名を接頭辞に持つ識別子 (`KsDialogsOptions` / `KsDialogsKmp` / `AddKsDialogs` / `KsDialogsInterop*` / `KsDialogsMauiBridge` / `KsDialogsInstaller` / `KsDialogsInitializer` / target 名) とモジュール・パッケージ・namespace・NuGet ID は触らない。

## 1. 契約型の改名 (コード)
- [x] 1.1 iOS: `protocol KsDialogs` → `KsDialog` (ファイル名 `KsDialogs.swift` → `KsDialog.swift`)、`Dialog` の準拠と Tests / samples/ios の参照、および MAUI iOS bridge (`maui/macios/native/KsDialogsMauiBridge/MauiDialogBridge.swift`) の参照を追随 (→ Requirement: Dialog 契約の改名)
- [x] 1.2 Android: `interface KsDialogs` → `KsDialog` (ファイル名追随)、`Dialog` の実装と Tests / samples/android の参照、および MAUI Android bridge (`maui/android/native/ksdialogs-maui-bridge/.../MauiDialogBridge.kt`) と KMP androidMain (`AndroidDialogGateway.kt`) の参照を追随 (→ Requirement: Dialog 契約の改名)
- [x] 1.3 KMP: commonMain `interface KsDialogs` → `KsDialog` (ファイル名追随)、`GatewayKsDialogs` → `GatewayKsDialog`、commonTest `FakeKsDialogs` → `FakeKsDialog` (ファイル名追随)、androidMain / iosMain / Tests / samples/kmp の参照を追随 (→ Scenario: 契約名を冠する実装と fake も追随する)
- [x] 1.4 MAUI: `IKsDialogs` → `IKsDialog` (ファイル名追随)、`Dialog` の実装・DI 登録・ApiSurfaceCheck・Tests・samples/maui の参照を追随 (→ Requirement: Dialog 契約の改名)
- [x] 1.5 doc コメント内の契約名 (各形態の `Dialog` / Loading / Toast の doc コメントで `KsDialogs` を契約として参照している箇所) を追随 (→ Requirement: Dialog 契約の改名)

## 2. 検証 (コード)
- [x] 2.1 残存検査: 4 形態 + samples のソースで型名としての `KsDialogs` / `IKsDialogs` / `GatewayKsDialogs` / `FakeKsDialogs` が 0 件、製品名由来の識別子が改名前と同一であることを grep で確認し結果を報告に添える (→ Scenario: 旧名では解決できない / 製品名由来の識別子は変わらない)
- [x] 2.2 旧名の負のコンパイル検証を 4 形態に追加: 既存のフラグ式 negative check と同じ置き場 (ios 非 `@testable` ファイル / android・kmp `api-surface-check` / maui `ApiSurfaceCheck/NegativeChecks`) に「旧名 `KsDialogs` (`IKsDialogs`) を型として参照する」ソースを 1 フラグ 1 本で追加し、それぞれが型未解決の診断で失敗することを確認。handbook/cross/test-execution.md の負の検査表 (55 本) に 4 行追記し本数を更新 (→ Scenario: 旧名では解決できない)
- [x] 2.3 4 ルートのライブラリテスト (handbook/cross/test-execution.md。maui は `dotnet test` + Android 互換面 + iOS 互換面) を実行し、件数を報告 (→ Scenario: 4 形態のライブラリテストが通る / 既定エントリと自前構築の注入が同じレジストリを共有する / 既定エントリの注入と fake の差し替えができる)
- [x] 2.4 4 Sample のビルド (handbook/cross/local-development-setup.md「Sample のビルドと実行」: ios / android / maui / kmp Android + iOS) を実行し、成否を報告 (→ Scenario: 4 Sample がビルドできる)
- [x] 2.5 3 機能の契約名が規則どおり並ぶことを各形態の公開面で確認 (→ Scenario: 3 機能の契約名が同じ規則で読める)

## 3. docs の追随
- [x] 3.1 concepts: core/api/registration-show-semantics.md、{ios,android,kmp,maui}/api/dialog-surface.md、kmp/api/ios-host-integration.md の契約型名を追随。log.md に記録は残す (→ Requirement: 文書の契約型名が実装と一致する)
- [x] 3.2 handbook / README.md / README_ja.md に契約型としての旧名があれば追随 (→ 同上)
- [x] 3.3 skills/en・skills/ja (全 Skill の SKILL.md と references/) の契約型名を直接修正。**前提: proofread-user-skills-ja の skills/ 変更が commit 済みであること** (未 commit なら着手前にオーナーへ確認) (→ Scenario: skills の en / ja が同じ名前を掲載する)
- [x] 3.4 docs 残存検査: concepts / handbook / README / skills の全 occurrence (通常文・表・コード span) の旧名を列挙し、製品名用法を allowlist で除外して契約型を指す旧名 0 件と除外一覧を報告 (→ Scenario: 現行文書に旧名の型参照が残らない)
- [x] 3.5 skills の整合性チェックと manifest 更新: docs-refresh SKILL.md Step 3a で予定 manifest (`scripts/planned-manifest.py`) を作り、それを入力に **6-①〜⑧ の 8 検査すべて** (concepts 網羅 / en-ja 節構成 / コードブロック byte 一致 / frontmatter / 閉世界性・機械面 / 内部リンク / identity・local-path lint / 配信識別子 grep — 6-⑧ は SKILL.md 内の inline grep) を通し、合格後に Step 7 の手順で予定 manifest と同一内容を `skills/.manifest.json` に書き込む。docs-refresh 本体 (Step 4〜5 の更新フロー) は起動しない (→ Scenario: skills の en / ja が同じ名前を掲載する)

## 4. lint
- [x] 4.1 `.agents/skills/docs-refresh/SKILL.md` の禁止トークンから `KsDialog([^sA-Za-z0-9_]|$)` を外し、パターンの読み方の注記 (516 行付近) を更新 (→ Requirement: 禁止トークン lint が改名後の正しい名前を弾かない)
- [x] 4.2 一時 fixture (許可: `KsDialog` / `IKsDialog` / `KsDialogs` / `KsDialogAttributes`、禁止: 残す各パターンの例 1 件ずつ) を scratchpad に作り、改修後の 6-⑧ grep を fixture と skills/ に実行。fixture で許可側 0 件・禁止側それぞれ 1 件以上、skills/ で 0 件を確認して報告 (→ Scenario: 単数形の契約名が lint を通り、残した禁止パターンは検出される)

## 5. 記録
- [x] 5.1 core/ADR-0002 の現行照合 (2026-09-06 追記) を「改名完了」に更新。ADR-0034 の accepted 昇格は蒸留時にオーナー承認
