# レビュー結果: add-maui-nuget-distribution (001 回目)

**日付**: 2026-09-08
**判定**: APPROVED

## サマリー

デルタスペック 2 本の Requirement 8 件・Scenario 16 件はすべて実装または証跡で満たされており、tasks.md のチェックに虚偽は無く、足場 (proposal / design / specs) は書き換えられていない。手元で再実行した facade `dotnet test` 160 件 / Android 互換面 33 件はいずれも失敗 0、Android binding の Release ビルドで `BG8401` が 0 件であること、両 README の非 HTTP(S) 参照が 0 件であることも再現できた。指摘は Critical / Major なしで、蒸留・phase-8/9 への申し送りに属する Minor 3 件と Suggestion 2 件のみ。

## 照合した規約

- `kasane/handbook/cross/comment-policy.md` (always)
- `kasane/handbook/cross/test-execution.md` — テストの実行・結果の報告・完了判定
- `kasane/handbook/cross/runtime-behavior-verification.md` — 実行時挙動の A/B (tasks 1.2 / 1.7)
- `kasane/handbook/cross/diagnostic-message-language.md` — 失敗型 `ViewCreationFailed` と `KSDLG0001` の文言追加
- `kasane/handbook/cross/sample-parity.md` — `samples/maui` の `MauiVersion` 変更と安定デモ ID 14 件の通し
- `kasane/handbook/cross/local-development-setup.md` — 本変更が改訂する対象
- `kasane/handbook/cross/ci-script-deletion.md` — `scripts/scenario-id-coverage.py` の改訂 (削除操作なし、該当なし)
- `kasane/lessons/impl.md` L-001 / `kasane/lessons/process.md` L-001・L-002 (`kasane/lessons/code-review.md` は存在せず)

## 実行して確認したこと (自分の手元)

| 検査 | 結果 |
|---|---|
| `cd maui && dotnet test` | 160 tests / 0 failures (変更前 155 + 5) |
| `cd maui/android/native && ./gradlew :ksdialogs-maui-bridge:test --rerun-tasks` | tests=33 / failures+errors=0 / skipped=0 |
| `cd maui/android/KsDialogs.Binding.Android && dotnet build -c Release --no-incremental` | exit 0 / `BG8401` 0 件 / `BG8A00` 4 種 (deviation 記録どおり) |
| README の非 HTTP(S) 参照の機械抽出 | 両枚とも 0 件 |
| lint (local-path / identity / comment-policy / scenario-id-coverage) | すべて通過。`scenario-id-coverage` は「未網羅なし」 |
| `doc-structure-lint --paths kasane/handbook/cross/local-development-setup.md` | 違反なし (他の違反は roadmaps の既存分で本変更の範囲外) |
| 証跡 png の md5 重複 | 0 件 (lessons impl L-001) |
| 証跡内のローカル絶対パス | 0 件 (未追跡ディレクトリのため手動 grep で確認) |
| `evidence/view-creation-failure/01,02-android-*.png` の実体照合 | README の記述と一致 (時刻 19:53 / 20:08 で別観測回。修正前は `type=System.InvalidOperationException` / `inner=(none)`、修正後は `KsDialogs.DialogException+ViewCreationFailed` / `inner=System.InvalidOperationException` / view・vm あり) |

Requirement / Scenario と実装の突き合わせでは、両 OS の預かり口 (`BridgeContentSupply.CreateOrFail` + `BridgeContentFailure`) が Dialog / Loading で対称になっていること (`maui/KsDialogs.Maui/Platforms/Android/PlatformDialogGateway.cs:95` と `maui/KsDialogs.Maui/Platforms/iOS/PlatformDialogGateway.cs:76`、Loading も同形)、Kotlin 側の `createContentView()` が null を既存の失敗経路へ合流させること、`ServiceProviderUnavailable` が `CreateView` の try の外に残って経路が変わっていないこと (`maui/KsDialogs.Maui/Hosting/KsDialogsServiceCollectionExtensions.cs:215`) を確認した。`MauiDialogViewModel` / `MauiLoadingViewModel` の `private` → `internal` は、既存の `MauiToastViewModel` と同じ形 (module 内可視・生成対象外) で公開契約に影響せず、無断の逸脱としては扱わない。

## 指摘事項

### [🟡 Minor] core/ADR-0033 との関係が「乖離の解消」ではなく「却下案の採用」であり、蒸留の予定作業では ADR 本文が古いまま残る

**該当箇所**: `kasane/decisions/core/0033-user-factory-failure-boundary.md` の Alternatives 3 番目と Consequences の「派生」行 / `design.md` の Decision 6 と「ADR 候補」節

**問題点**: design は「core/ADR-0033 (accepted) は MAUI 境界で両 OS が `BridgeContentSupply` を通すと定めているが Android の Dialog / Loading gateway は未配線」「現状はその決定から乖離している」と書き、蒸留時の作業を「現行照合 footer を更新」としている。しかし ADR-0033 本文は、Android の Dialog / Loading への `BridgeContentSupply` 適用を **Alternatives で「採らず」**と明示し、その理由を「Kotlin 側の受け皿が `catch (Throwable)` であり observable 挙動は現状で成立している」と記録し、Consequences の「派生」行で「3 面の対称化は将来課題」としている。つまり本実装は乖離の解消ではなく、却下扱いだった案の採用にあたる。あわせて、その却下理由 (「観察可能挙動は現状で成立している」) は本変更の証跡 `evidence/view-creation-failure/01-android-before.png` が反証している (例外型が失われ `InvalidOperationException` のメッセージだけになる)。footer の更新だけでは、Alternatives の「採らず」と Consequences の「将来課題」が accepted な ADR に残り、次に同じ判断を参照する読み手を誤らせる。

**推奨修正**: 実装の変更は不要 (ADR-0033 の Decision 本文とは整合している)。蒸留時に ADR-0033 の Alternatives 3 番目と Consequences の「派生」行を、本変更で対称化が完了した旨と、却下理由が実測で反証された旨に改訂する (ADR の改訂は explore / propose / distill の権利のため、ここでは指摘に留める)。

### [🟡 Minor] facade パッケージの XML ドキュメントが TFM 間で非対称に入る (android TFM のみ)

**該当箇所**: `maui/Directory.Build.props` (`GenerateDocumentationFile` の指定なし) / `evidence/pack-verification/README.md` の「facade の同梱物」

**問題点**: 同梱物一覧は `lib/net10.0-android36.0/KsDialogs.Maui.xml` のみを含み、`net10.0` と `net10.0-ios26.0` には XML が無い。リポジトリ内のどこにも `GenerateDocumentationFile` / `DocumentationFile` の指定が無く (grep で確認)、`maui/KsDialogs.Maui/bin/*/net10.0-android/` にだけ `.xml` が出ているため、これは .NET Android SDK の既定に由来する偶発的な非対称である。結果として利用者の IntelliSense は TFM によって出たり出なかったりし、かつ出る側では日本語の doc コメントがそのまま公開パッケージに載る (doc コメントの日本語自体は comment-policy どおりで違反ではないが、載せるかどうかの判断は行われていない)。spec の Requirement「3 パッケージの構成と内容」は XML に触れていないため仕様違反ではない。

**推奨修正**: 初版発行 (phase-8 / 9) までに方針を決めて明示する — 全 TFM で `GenerateDocumentationFile=true` に揃えるか、`.xml` を pack から外す。決めた側を `maui/Directory.Build.props` に書き、SDK 既定への暗黙依存を残さない。

### [🟡 Minor] 利用者へ配る診断文言 `KSDLG0001` が diagnostic-message-language の対象範囲と静的 grep の外にある

**該当箇所**: `maui/KsDialogs.Maui/buildTransitive/KsDialogs.Maui.targets:26` (`Error Code="KSDLG0001"` の Text) / `kasane/handbook/cross/diagnostic-message-language.md` の frontmatter `paths` と「検査」節の grep

**問題点**: `KSDLG0001` は利用者のビルドに出る診断であり、規約が対象とする「ライブラリが外へ出す診断文言」の性質を持つ。現在の文面は英語で規約に適合しているが、規約の `paths` は `maui/KsDialogs.Maui/**` を含む一方で「検査」節の grep は `--include='*.swift' --include='*.kt' --include='*.cs'` に限られており、`.targets` / `.props` は 1 件も走査されない。同梱 MSBuild 資産に日本語の診断文言が入っても、規約の唯一の機械検査が無音で素通りする (同じファイル群にはリポジトリ内向けの日本語エラー `maui/Directory.Build.targets` の `KsCheckGeneratedAarEntries` も同居しており、両者を区別する基準も文書化されていない)。

**推奨修正**: 蒸留時に diagnostic-message-language.md の「対象」表と検査の grep に、利用者へ配る MSBuild 資産 (`maui/KsDialogs.Maui/buildTransitive/**`) を加え、リポジトリ内だけで動くビルドスクリプト (`maui/Directory.Build.targets` 等) は対象外であることを明記する。

### [🔵 Suggestion] 自 assembly 用 aar の検査・除去ターゲットは本リポジトリで一度も実行されない

**該当箇所**: `maui/Directory.Build.targets` の `KsCheckGeneratedAarEntries` / `KsExcludeGeneratedAarFromPackage`

**問題点**: deviation.md に記録済みのとおり、KsDialogs では自 assembly 用 aar が生成されず `Exists` 条件が成立しないため、この 2 ターゲットは走らない (プロパティの評価値は正しく `bin/Release/net10.0-android/KsDialogs.Maui.aar` になることは確認した)。将来 SDK 挙動が変わって初めて発火する経路であり、許可パターン `^jni/[^/]+/libandroidx\.graphics\.path\.so$` は KsDialogs では未評価 (deviation 記載) のため、発火した瞬間に pack が日本語文面のエラーで止まる可能性がある。`RoslynCodeTaskFactory` を使う `KsListArchiveEntries` も未実行のままである。

**推奨修正**: 一度だけ手で発火させて (例: 既存の Gradle 由来 aar を `-p:_KsGeneratedAarPath=` で指定して `KsCheckGeneratedAarEntries` を走らせる) タスク自体が動くことを確認し、その結果を deviation.md か evidence に 1 行残す。走らせない判断にするなら、その旨を deviation.md の当該項に足す。

### [🔵 Suggestion] MAUI 側に「開発既定値 `0.0.0-dev` のまま発行しない」歯止めが無い (phase-9 への申し送り)

**該当箇所**: `maui/Directory.Build.props` の `<Version>0.0.0-dev</Version>`

**問題点**: cross/ADR-0009 (proposed) は Gradle ビルドルートについて「version が `-SNAPSHOT` のあいだは Maven Central へ向く発行タスクを失敗させる」歯止めと、注入値の形式検査を定めている。MAUI 側には対応する仕組みが無く、`-p:Version=` を渡し忘れた nupkg が `0.0.0-dev` のまま出来上がる (実際に pack 検算でそうなっている)。実発行は phase-9 の責務 (proposal Non-Goals) なので本変更の欠陥ではない。

**推奨修正**: phase-9 の release workflow 側に、MAUI の 3 パッケージについて注入値の形式検査と `0.0.0-dev` 発行の禁止を持たせるよう申し送る。

## アクションプラン

1. 本変更としての修正作業は無し (Critical / Major なし、spec 違反なし)。このままオーナーレビューへ進んでよい
2. 蒸留時: core/ADR-0033 の Alternatives / Consequences を改訂する (Minor 1)。ADR-0033 の現行照合 footer 更新だけで済ませない
3. 蒸留時: diagnostic-message-language.md の対象範囲と grep に同梱 MSBuild 資産を加える (Minor 3)
4. phase-8 / 9 まで: facade の XML ドキュメントの方針を決めて `maui/Directory.Build.props` に明示する (Minor 2)、MAUI の発行版ガードを phase-9 に持たせる (Suggestion 2)
5. 任意: aar 検査ターゲットの一度きりの発火確認 (Suggestion 1)
