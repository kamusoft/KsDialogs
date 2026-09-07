# レビュー結果: rename-dialog-contract-singular (001 回目)

**日付**: 2026-09-06
**判定**: APPROVED

## サマリー

Dialog の表示契約 `KsDialogs` / `IKsDialogs` → `KsDialog` / `IKsDialog` の改名が 4 形態のライブラリ・テスト・公開 API 形状検査・Sample・concepts / handbook / skills / lint まで漏れなく揃っている。境界規則 (製品名接頭辞・モジュール名・パッケージ・namespace・NuGet ID は据え置き) も守られており、diff は型名の置換以外に挙動へ触る変更を含まない。4 ルートのテスト全件 (ios 251 / android 67 / kmp 96 / maui 133 + 30 + 6)、旧名の負のコンパイル検証 4 本、改名で本文が変わった既存の負の検査 10 本、4 Sample (5 ビルド)、docs-refresh の 8 検査、禁止トークン lint の fixture 両方向を自分で実行し、すべて期待どおりだった。Critical / Major はなく、指摘は timestamp の運用ずれ 1 件と将来の整理提案のみ。

## 照合した規約

- `kasane/handbook/cross/comment-policy.md` (always — コメント構文を持つファイルの変更あり。`scripts/comment-policy-lint.py` 923 ファイル検査で禁止 0 件)
- `kasane/handbook/cross/test-execution.md` (テストの実行・結果の報告・完了判定。4 ルートの全件実行と負の検査の診断表を本文で照合)
- `kasane/handbook/cross/local-development-setup.md` (4 形態の Sample のビルド。MAUI iOS の `ValidateXcodeVersion=false` を含む)
- `kasane/handbook/cross/user-skill-api-listing.md` / `user-skill-writing-style.md` (`skills/**` の更新。機能名・用語の表記規約と抵触なし)
- `kasane/decisions/core/0002-public-api-shape.md` (accepted。契約 interface + 既定エントリの両対応は不変)、`cross/0005-public-identifier-mapping.md` (製品名・配布識別子の据え置き)、`core/0034-...singular-feature.md` (proposed — 根拠としては扱わず所見のみ)
- `kasane/concepts/` は core/api/registration-show-semantics.md と各 platform の api/dialog-surface.md、kmp/api/ios-host-integration.md を本文まで照合
- `kasane/lessons/process.md` (L-001 姉妹面の照合)。`kasane/lessons/code-review.md` は不在

## 実行した検証 (すべて自分で実行)

| 対象 | 結果 |
|---|---|
| ios/ `xcodebuild test` | 251 tests / 47 suites passed (`Executed 0 tests` は XCTest 側で正常) |
| android/ `./gradlew test --rerun-tasks` | 67 tests / 0 failures (`verifyNoDeclarativeUiDependency` 含む) |
| kmp/ `./gradlew allTests --rerun-tasks` | 96 tests / 0 failures (iosSimulatorArm64 + androidHostTest) |
| maui/ `dotnet test` | 133 tests / 0 failures |
| maui/android/native/ `./gradlew :ksdialogs-maui-bridge:test` | 30 tests / 0 failures |
| maui/macios/native/ `xcodebuild test` | 6 tests / 3 suites passed |
| 新設した負の検査 4 本 | 4 形態すべてビルド失敗。診断は handbook の追記行と一致 (ios `cannot find type 'KsDialogs' in scope` 1 件 / android・kmp `Unresolved reference 'KsDialogs'.` 2 件 / maui CS0246) |
| 改名で本文が変わった既存の負の検査 10 本 | ios SHOW_OPTIONS / SHOW_TRANSITION / RESULT_TYPE、android resultType / showOptions / showTransition、kmp resultType / showOptions、maui ResultType / TypedShowContract / ShowOptions / ShowTransition / ValueTypeViewModel — すべて**改名前と同じ診断**で失敗 (別の理由で失敗して「効いた」と読み違える事故がないことを確認) |
| Sample ビルド | samples/ios ✅ / samples/android ✅ / samples/maui (android + ios) ✅ / samples/kmp (androidApp + iosApp) ✅ |
| 残存検査 (コード) | 型としての `KsDialogs` / `IKsDialogs` / `GatewayKsDialogs` / `FakeKsDialogs` は 0 件 (負の検査ソース 4 本の意図的な参照を除く)。製品名接頭辞 7 種はすべて現存 |
| 残存検査 (文書) | concepts / handbook / README 2 枚 / skills の全 occurrence を列挙し、製品名用法 (モジュール・SwiftPM product・Kotlin パッケージ・namespace・NuGet ID・リポジトリ URL・`AddKsDialogs` 等の接頭辞・負の検査の診断表) を除外すると契約型を指す旧名は 0 件 |
| skills en/ja | 5 Skill すべてで `KsDialog` / `IKsDialog` の出現数が en / ja 一致 (5/5, 8/8, 22/22, 12/12, 11/11) |
| docs-refresh 8 検査 | 6-① concepts 網羅 OK / 6-② 節構成 OK / 6-③ コードブロック byte 一致 OK / 6-④ frontmatter OK / 6-⑤ 閉世界性 0 件 / 6-⑥ 内部リンク全解決 / 6-⑦ local-path・identity lint 0 件 / 6-⑧ 配信識別子 grep 0 件。`skills/.manifest.json` は予定 manifest と完全一致 |
| 禁止トークン lint fixture | 許可綴り (`KsDialog` / `IKsDialog` / `KsDialogs` / `KsDialogAttributes`) 検出 0 件、残した禁止パターン 15 行すべて検出。参考: 撤去前のパターンを同じ対象へ当てると 114 件の偽陽性 (撤去の必要性を裏取り) |
| リポジトリ lint | `local-path-lint.py` / `identity-lint.py` / `comment-policy-lint.py` すべて違反 0 件、`scenario-id-coverage.py` 「未網羅なし」 |
| 足場の逆流 | proposal.md / specs/ に diff なし。tasks.md はチェックボックスの `[ ]` → `[x]` のみ |

## 指摘事項

### [🟡 Minor] 本文を書き換えた concepts の timestamp (最終検証日) が更新されていない

**該当箇所**: `kasane/concepts/core/api/registration-show-semantics.md:6` (`timestamp: 2026-08-25`)、`kasane/concepts/maui/api/dialog-surface.md:6` (`timestamp: 2026-09-05`)

**問題点**: concepts の `timestamp` は「最終検証日 — 内容が今も正しいと確認した日に更新する」と定義されており、ksn-drift の鮮度可視化の入力になる。本 change は両ファイルの本文 (表示エントリの定義・用語表・入口表) を 2026-09-06 に書き換えて実装と照合しているのに、この 2 本だけ日付が据え置かれている。同時に触った `ios/api/dialog-surface.md` / `android/api/dialog-surface.md` / `kmp/api/dialog-surface.md` / `kmp/api/ios-host-integration.md` はいずれも 2026-09-06、`kasane/handbook/cross/test-execution.md` は 2026-09-02 → 2026-09-06 へ更新済みで、扱いが不揃いになっている (前者 4 本は元から 2026-09-06 だったため結果的に正しい値になっているだけ)。

**推奨修正**: 2 ファイルの `timestamp` を `2026-09-06` に更新する。

### [🔵 Suggestion] 契約が単数になっても変数・パラメータ名は複数形 `dialogs` のまま

**該当箇所**: `samples/kmp/shared/src/commonMain/kotlin/jp/kamusoft/ksdialogs/samples/kmp/SamplePresenter.kt:29`、`maui/android/native/ksdialogs-maui-bridge/src/main/kotlin/jp/kamusoft/ksdialogs/maui/MauiDialogBridge.kt:22`、`maui/macios/native/KsDialogsMauiBridge/MauiDialogBridge.swift:15`、各 api-surface-check の `dialogs: KsDialog` 引数、`skills/**` の DI 例 (`IKsDialog dialogs` / `private let dialogs: any KsDialog`) ほか

**問題点**: core/ADR-0034 の Consequences は「DI 登録・fake 差し替えのコードが機能間で対称になる」を効果に挙げているが、`loading: KsLoading` / `toast: KsToast` と並ぶ場面で Dialog だけ `dialogs: KsDialog` と複数形が残り、利用者が読むコード例でも非対称が見える (`SamplePresenter` のコンストラクタが典型)。

**推奨修正**: 本 change の境界規則 (tasks.md 冒頭) は改名対象を型名に限っており、識別子名は明示的にスコープ外なので**この change では直さない**。別 change として「契約を受ける変数・パラメータ名の単数化」を起票するかどうかをオーナーが判断する材料として記録する。

### [🔵 Suggestion] `FakeDialogsSubstitutionTests` のクラス名・ファイル名が複数形のまま

**該当箇所**: `kmp/ksdialogs-kmp/src/commonTest/kotlin/jp/kamusoft/ksdialogs/kmp/FakeDialogsSubstitutionTests.kt:16`

**問題点**: 中身の fake は `FakeKsDialog` へ追随済みだが、それを使うテストクラス名だけが `FakeDialogs...` と複数形で残る。spec の Scenario は `FakeKsDialogs` の不在だけを要求しており違反ではないが、`FakeKsDialog` を使うテストの名前が複数形という読み手の引っかかりが残る。

**推奨修正**: 上と同じく境界規則の外。直すなら別 change で `FakeDialogSubstitutionTests` へ揃える。

### [🔵 Suggestion] docs-refresh の lint 注記が proposed の ADR を根拠にしている

**該当箇所**: `.agents/skills/docs-refresh/SKILL.md:516`

**問題点**: 撤去した単数形パターンの理由として `kasane/decisions/core/0034-contract-type-name-singular-feature.md` を引いているが、この ADR は現在 `proposed` で、accepted 昇格は蒸留時のオーナー承認待ち (tasks.md 5.1)。昇格が見送られると、恒久的に残る lint の注記だけが未確定の決定を根拠に据えた状態になる。

**推奨修正**: 実装側で直すことはない (ADR の status を実装が動かすのは誤り)。蒸留で ADR-0034 を accepted へ昇格させるところまでを一続きの前提として扱う、という申し送りとして記録する。

## 確認したが指摘に至らなかった点

- **ios/ の全件実行 1 回目で 1 件失敗した**: `[TS-IO-02] SwiftUI 登録のカスタム Toast が UIKit 登録と同じに働く` が `ToastSwiftUIContentTests.swift:45` の `await harness.waitUntilPresenting()` で 22.2 秒後に失敗。cold boot 直後の Simulator での 1 回目のみで、単体再実行 (1.4 秒) と 2 回目の全件実行 (251/251) はいずれも成功した。Toast は本 change の対象外で diff にも一切現れないため、環境起因のフレークと判断した。ただし「起動直後の Simulator に対して待ちが脆い」可能性は残るので、退行を疑う材料として記録しておく。
- **3 機能の契約名を並べる正の検査は iOS にだけ新設された**: android / kmp / maui では `Dialog*ApiSurfaceChecks` / `Loading*` / `Toast*` が既定ビルドで各契約型を型名で参照しており、3 名がまとめて崩れないことは既に compile で固定されている。ファイルが 1 本に集約されていないだけで検査の穴ではない。
- **付随修正の同梱**: diff に Scenario と対応しない挙動変更は無く、deviation.md が不在であることと矛盾しない。
- **L-001 (姉妹面の照合)**: 4 形態のミラーを 1 面ずつ実物で確認した (契約宣言・実装の準拠・bridge の受け口・負の検査の置き場と診断)。片面だけの取りこぼしは見つからなかった。

## アクションプラン

1. (Minor) `kasane/concepts/core/api/registration-show-semantics.md` と `kasane/concepts/maui/api/dialog-surface.md` の `timestamp` を `2026-09-06` へ更新する
2. (Suggestion / 蒸留時) core/ADR-0034 を accepted へ昇格させる — docs-refresh の lint 注記がこの ADR を根拠にしているため
3. (Suggestion / 別 change の判断材料) 契約を受ける変数・パラメータ名の単数化と `FakeDialogsSubstitutionTests` の改名。本 change の境界規則の外なので、ここでは直さない
