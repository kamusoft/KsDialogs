# レビュー結果: add-maui-nuget-distribution (002 回目)

**日付**: 2026-09-08
**判定**: APPROVED

## サマリー

修正サイクル 1 周目で採用された相方指摘 2 件 (診断文言と元例外の検出力 / Sample 証跡の内訳) は、いずれも指摘の実害が消える形で解消されている。手元で再実行した facade `dotnet test` は 160 tests / 0 failures、Android 互換面は 34 tests / 0 failures / 0 skipped、lint 4 種 (comment-policy / identity / local-path / scenario-id-coverage) と `KsDialogs.Maui.ApiSurfaceCheck` のビルドもすべて通り、修正による退行は見つからなかった。足場 (proposal / design / specs) は今回も書き換えられていない。新規の指摘は Critical / Major なし、Suggestion 1 件のみ。

## 照合した規約

- `kasane/handbook/cross/comment-policy.md` (always)
- `kasane/handbook/cross/test-execution.md` — テストの実行と件数の報告 (修正後の全件再実行)
- `kasane/handbook/cross/diagnostic-message-language.md` — 「検査」節 (失敗型ごとの完全一致テスト `DM-MA-01`〜`04` 系列が英語文言の正しさを担う) が今回の修正の判定基準
- `kasane/handbook/cross/sample-parity.md` — 「安定デモ ID」表の「起動直後の状態」列が証跡の内訳訂正の照合先
- `kasane/lessons/impl.md` L-001 (証跡の説明文は保存実体と一致させる) / `kasane/lessons/process.md` L-001・L-002 (`kasane/lessons/code-review.md` は存在せず)

## 実行して確認したこと (自分の手元)

| 検査 | 結果 |
|---|---|
| `cd maui && dotnet test` | 160 tests / 0 failures / 0 skipped |
| `cd maui/android/native && ./gradlew :ksdialogs-maui-bridge:test --rerun-tasks` | tests=34 / failures=0 / errors=0 / skipped=0 (JUnit XML 9 ファイルの合算) |
| `dotnet build maui/KsDialogs.Maui.ApiSurfaceCheck/…` | 成功 / 0 警告 / 0 エラー (Scenario「公開例外面の compile 検査」) |
| `scripts/comment-policy-lint.py` | 禁止 0 件 (検査対象 968 ファイル)。`--advisory` の要確認は既存 448 件で、今回触れた 4 ファイルの該当行はいずれも変更前から在るクラス doc コメント |
| `scripts/identity-lint.py` / `scripts/local-path-lint.py` | 検出なし |
| `scripts/scenario-id-coverage.py` | 「結果: 未網羅なし」 |

## 採用指摘の解消確認

### 1. 新規失敗契約のテストが診断文言と元例外を固定していない → 解消

- `maui/KsDialogs.Maui.Tests/DiagnosticMessageTests.cs:17` の `DM_MA_01` が `ViewCreationFailed` を 7 種目として取り込み、`Message` の完全一致 (`Could not create the View SampleView registered for ViewModel type SampleViewModel.`)・`ViewTypeName` / `ViewModelTypeName` の両プロパティ・`InnerException` の `Is.SameAs` による同一性を固定している。`maui/KsDialogs.Maui/Contract/DialogException.cs` の `public sealed class` は 7 個で、7 個すべてがこのテストで検査されている (数え漏れなし。doc コメントの「7 種」も実体と一致)
- `maui/android/native/…/MauiBridgeDiagnosticMessageTests.kt` に `DM-MA-04` の 2 本目が加わり、`MauiDialogViewModel` / `MauiLoadingViewModel` の `createContentView()` が投げる `IllegalStateException` の `message` を Toast と同じ文言で完全一致検査している。既存の `DM-MA-04` (Toast) と合わせて 3 面が揃った
- `MB-MA-15` を接頭辞判定のまま残した判断は妥当と判断した。`maui/KsDialogs.Maui/Platforms/Android/PlatformDialogGateway.cs:94` / `PlatformLoadingGateway.cs:186` のとおり、互換面が `onFailed` / `onFailure` に載せて返すメッセージは預かり口 (`contentFailure.Cause`) が空のときだけ使うフォールバックであり、通常経路では利用者へ届かない。文言の正しさを `DM-MA-04` に集約し、`MB-MA-15` は経路の合流だけを見る分担になっている
- `MB-MA-11` は Dialog (`DialogDependencyInjectionTests.cs:108`) / Loading (`LoadingDependencyInjectionTests.cs:91`) の両方で、`InnerException` の型 (`InvalidOperationException`) に加えて、その本文が解決できなかった依存の型名と組み立てられなかった View の型名の両方を含むことを検査している。spec の Scenario「InnerException が DI の解決失敗の例外」を識別できる強さになった。姉妹面の Toast (`ToastDependencyInjectionTests.cs` の `MB-MA-12`) も警告本文に `ViewCreationFailed` と元の依存型名が残ることを見ており、3 面で検出力が揃っている (process L-001)

### 2. Sample 証跡の 14 件の内訳が計数と一致しない → 解消

`evidence/sample-walkthrough/README.md` の判定節は「ダイアログ表示 5 件 / デモ画面 1 件 / 属性調整パネル 1 件 / Loading 開始 2 件 / Toast 表示 4 件 / 時系列の開始 1 件」の排他分類になり、合計が 14 件で一致する。個別表の 14 行 (`basic`〜`inline` の 5 件 / `transition-dialog` / `layout-dialog` / `default`・`custom` の Loading 2 件 / Toast 4 件 / `toast-overlap`) と 1 対 1 で対応し、handbook `cross/sample-parity.md` の「安定デモ ID」表の「起動直後の状態」列とも語まで一致する。`transition-dialog` / `layout-dialog` の追加操作で出したダイアログは「上の内訳には数えていない」と別文に分けてあり、二重計上も起きていない (lessons impl L-001 の要求を満たす)。

## 指摘事項

### [🔵 Suggestion] `MB-MA-11` の原因検査が DI 実装のメッセージ書式に依存している

**該当箇所**: `maui/KsDialogs.Maui.Tests/DialogDependencyInjectionTests.cs:132` / `maui/KsDialogs.Maui.Tests/LoadingDependencyInjectionTests.cs:115`

**問題点**: 元例外の識別を `InnerException.Message` が 2 つの型名を含むことで行っているため、`Microsoft.Extensions.DependencyInjection` の `ActivatorUtilities` が出す例外文言の書式が SDK 更新で変わると、ライブラリ側に何の欠陥もないままこの 2 本が赤くなる。Scenario が「DI の解決失敗の例外」を要求している以上、実際に DI に投げさせる現在の作りは spec に忠実であり、この依存は今回の強化と引き換えの正当なコストである (今回の修正で入った検出力そのものは spec が求めるもので、後退させる提案ではない)。

**推奨修正**: 本変更での修正は不要。将来この 2 本が SDK 更新で落ちたときに「文言書式の変化」と切り分けられるよう、`Does.Contain` の assertion メッセージに「フレームワークの例外文言に依存する検査である」旨を 1 文足しておくと、次に触る人が実装の退行と誤認しない。あるいは、依存の登録済み factory 自体を例外で失敗させる補助テストを 1 本足して `Is.SameAs` で同一性を固定すれば、書式非依存の防波堤になる。

## 前回から持ち越す指摘 (今回の修正対象外・再確認済み)

`review-001.md` の以下は現状のまま残っており、判定は変わらない。いずれも本変更の実装で解決すべきものではない。

| 指摘 | 重要度 | 送り先 |
|---|---|---|
| core/ADR-0033 の Alternatives / Consequences が却下扱いのまま残る | 🟡 Minor | 蒸留 (ksn-distill) |
| facade nupkg の XML ドキュメントが android TFM のみ | 🟡 Minor | phase-8 / 9 |
| `KSDLG0001` が diagnostic-message-language の対象範囲と grep の外 | 🟡 Minor | 蒸留 (ksn-distill) |
| aar 検査ターゲットの一度きりの発火確認 | 🔵 Suggestion | 任意 (deviation.md 記載済み) |
| MAUI の `0.0.0-dev` 発行ガード | 🔵 Suggestion | phase-9 |

## アクションプラン

1. 本変更としての修正作業は無し。採用指摘 2 件は解消済みで、退行も無い。オーナーレビューへ進んでよい
2. 蒸留・phase-8/9 への申し送りは `review-001.md` の 5 件をそのまま引き継ぐ (上表)
3. 任意: `MB-MA-11` の原因検査がフレームワークの例外文言に依存する旨を assertion メッセージに残す (Suggestion 1)
