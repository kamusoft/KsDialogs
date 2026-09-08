# レビュー結果: add-verification-ci (002 回目)

**日付**: 2026-09-08
**判定**: APPROVED

## サマリー

review-001 (Major 2 / Minor 2) と second-opinion-code-001 の突き合わせで採用が確定した 6 件は、いずれも指摘の趣旨どおりに解消されている。修正は `maui/nuget.config` の新設・`ci.yml` の 5 job の `if:` の書き換え・`global.json` の `rollForward` 追加と handbook 反映・`local-development-setup.md` の版更新チェックリスト拡充・`verify-maui.yml` の件数検査 3 本のステップ条件化・`ci.yml` 冒頭コメントからの proposed ADR 参照除去に限定されており、範囲外への波及はない。

再実行した検証 (`dotnet restore --force` で NU1507 消失、`dotnet test` 155 件成功、`dotnet nuget list source` が `maui/` 配下で nuget.org 単一、`android` の `./gradlew test` BUILD SUCCESSFUL、lint 4 本 + 構造 lint exit 0、6 workflow の YAML パースと全 `run:` の `bash -n`) はすべて通過した。新規の Critical / Major は検出していない。残るのは Suggestion 2 件 (うち 1 件は前回 Minor 2 の修正が生んだ job 間の非対称) で、いずれも tasks 5.1 の実測後の判断で足りる。

前回の Suggestion 4 件は方針どおり未対応のまま指摘に含めていない。

## 照合した規約

| 文書 | 適用のきっかけ |
|---|---|
| cross/comment-policy.md | 常時。修正で増減したコメント (`ci.yml` 冒頭・5 job の `if:` の説明・`verify-maui.yml` の 3 検査の但し書き・`Directory.Packages.props` / Sample csproj) を規約本文から判定。`comment-policy-lint.py` も exit 0 |
| cross/local-development-setup.md | 本変更が同文書を改訂し、`global.json` の `rollForward` と版更新チェックリストを規定するため |
| cross/test-execution.md | 本変更が同文書を改訂し、CI が回す範囲を規定するため |
| cross/ADR-0004 (composite build)・ADR-0013 (外部 PR 不受理) | workflow から参照されている accepted な決定として、参照の妥当性を確認 |

以下は適用のきっかけに当たらないため本文まで読んでいない: sample-parity.md (Sample の csproj 変更は版プロパティのみで、デモ項目・文言・起動引数に触れない)、runtime-behavior-verification.md、user-skill-api-listing.md、user-skill-writing-style.md、docs-refresh-timing.md、diagnostic-message-language.md、aiforms-origin-reference.md。

`kasane/lessons/code-review.md` は不在 (重点観点・指摘しないことの登録なし)。

## 採用 6 件の確認

| # | 指摘 (review-001 / 突き合わせ) | 該当箇所 | 判定 |
|---|---|---|---|
| 1 | `maui/nuget.config` の新設 (`<clear />` + nuget.org 単一 + packageSourceMapping) | `maui/nuget.config` (新規) | 解消 |
| 2 | 5 job の `if:` を status 関数入りへ | `.github/workflows/ci.yml:128`・`134`・`140`・`146`・`152` | 解消 |
| 3 | `global.json` の `rollForward: disable` と handbook 反映 | `global.json:4`、`kasane/handbook/cross/local-development-setup.md:60-79` | 解消 |
| 4 | 版更新チェックリストに `samples/maui` の `MauiVersion` | `kasane/handbook/cross/local-development-setup.md:78` | 解消 |
| 5 | `verify-maui.yml` の件数検査 3 本をテストステップの `conclusion` で条件付け | `.github/workflows/verify-maui.yml:102`・`163`・`260` (対応する `id:` は `91`・`156`・`247`) | 解消 |
| 6 | `ci.yml` 冒頭コメントから proposed な cross/ADR-0016 の参照を除去 | `.github/workflows/ci.yml:1-14` | 解消 |

### 1. `maui/nuget.config`

`maui/nuget.config` はローカルの権限設定 (資格情報を持ち得るファイル名の保護) により**本文を直接読めなかった**ため、restore の実挙動で確認した。

- `dotnet nuget list source` を `maui/` 配下で実行すると `nuget.org` のみ。リポジトリルートでは `nuget.org` と私設フィード `kamusoft` の 2 件が見えるので、`maui/` 配下だけ継承が断たれている (`<clear />` 相当が効いている)
- `dotnet restore maui/KsDialogs.Maui.Tests/KsDialogs.Maui.Tests.csproj --force` を 2 回実行し、5 プロジェクトすべてで NU1507 / NU1605 を含む warning が 1 件も出ないことを確認 (review-001 時点では 5 プロジェクトすべてに NU1507 が出ていた)
- `dotnet test maui/KsDialogs.Maui.Tests/KsDialogs.Maui.Tests.csproj -c Release` = 155 件成功

`packageSourceMapping` 節の有無だけは本文を読めないため未確認だが、Requirement「ツールチェーンの再現性」が求める「親ディレクトリの設定に依存しない」は単一ソース化の時点で満たされており、指摘の実害 (私設フィードが解決経路に居る) は解消している。本文の byte 単位の照合は、コミット後の diff を見られる立場 (オーナーまたは commit 済みの状態での再レビュー) で確認するのが確実。

### 2. `ci.yml` の 5 job の `if:`

`if: ${{ !cancelled() && (github.event_name == 'pull_request' || needs.changes.outputs.source != 'false') }}` が 5 job すべてに同一の式で入っている。

- `changes` 失敗時: `outputs.source` は空文字 → `'' != 'false'` が真 → 5 job は実行される。`Detect source changes` スクリプト側の「判定不能はソース変更ありへ倒す」と向きが揃った
- `changes` 成功 + `source=false` (記録だけの push): `false || 'false' != 'false'` → 偽 → スキップ。Scenario「記録だけの push では lint だけが走る」は維持されている
- `pull_request`: 第 1 項が真で常に実行。Requirement「`main` 宛ての pull_request では本体検証 5 job を常に実行する」が job グラフの側でも成立する
- `!cancelled()` の前置により、concurrency の打ち切りで macOS ランナーが起き直ることはない

`ci.yml:116-124` に判断理由 (暗黙の `success()` を失う代わりに式だけで判定されること・空文字を実行側へ倒すこと・`!cancelled()` の役割) が書かれており、コメント単独で理解できる。

### 3 / 4. `global.json` と handbook

`global.json` は `version` / `rollForward` / `workloadVersion` の 3 項目になり、handbook の「.NET SDK と MAUI ワークロード」節が同じ JSON を掲げたうえで「指定 SDK が無ければロールフォワードせずに失敗する」ことを明記している。版更新チェックリストは `global.json` の 2 行 + `maui/Directory.Packages.props` + `samples/maui/KsDialogs.Sample.Maui/KsDialogs.Sample.Maui.csproj` の `MauiVersion` の 3 か所に増え、「Sample は CI の検証対象ではないため、ずれても検査で気づけない」という注意も入っている。

手元で `dotnet --version` = `10.0.300`、`dotnet workload list` が repo の `global.json` の workload set 10.0.300.3 を使う旨を表示することを再確認した (Scenario「手元のビルドが repo の設定で固定される」)。改訂 2 文書 + `cross/index.md` に `doc-structure-lint.py` を掛けて違反なし。

### 5. `verify-maui.yml` の件数検査

3 本のテストステップに `id:` (`facade-tests` / `android-bridge-tests` / `ios-bridge-tests`) が付き、対応する検査が `if: ${{ !cancelled() && steps.<id>.conclusion != 'skipped' }}` になった。前段の環境準備 (`Select Xcode` / `Setup .NET` / `Install MAUI workload` / `Prepare Android SDK location`) が落ちた場合、テストステップは `skipped` になるため 3 本の検査も走らず、原因を指さない `::error::` が 3 本並ぶ状態は解消される。テストが**失敗**したときは `conclusion` が `failure` なので検査は走り、件数の内訳は従来どおり残る。3 本のコメントにも「前段が落ちてテスト自体が走らなかったときは報告しない」意図が書かれている。

### 6. proposed ADR 参照の除去

`.github/workflows/` 全体で `grep -n "ADR-00"` すると残る参照は `verify-kmp.yml:59` の cross/ADR-0004 と `ci.yml:177` の cross/ADR-0013 の 2 件のみで、どちらも `status: accepted`。cross/ADR-0016 への参照はソースコメントから消えている。

## 実施した検証

- `dotnet restore --force` (2 回) — NU1507 / NU1605 を含む warning 0 件
- `dotnet test maui/KsDialogs.Maui.Tests/KsDialogs.Maui.Tests.csproj -c Release` — 155 件成功 / 失敗 0
- `dotnet nuget list source` を `maui/` とリポジトリルートで比較 — `maui/` 配下は nuget.org 単一
- `dotnet --version` / `dotnet workload list` — 10.0.300 / repo の global.json の workload set 10.0.300.3
- `android` で `./gradlew --no-daemon --console=plain test` — BUILD SUCCESSFUL、`:ksdialogs:verifyNoDeclarativeUiDependency` が同じ実行に乗ることを再確認
- 6 workflow の YAML パース、全 `run:` ブロックの `bash -n` — 失敗 0
- `local-path-lint.py` / `identity-lint.py` / `comment-policy-lint.py` (検査対象 965 ファイル) / `scenario-id-coverage.py` — すべて exit 0 (「結果: 未網羅なし」)
- `doc-structure-lint.py --paths` を改訂 handbook 2 本 + `cross/index.md` に — 違反なし
- 外部 action 6 参照の SHA とバージョンコメントが review-001 の照合時から変わっていないこと、全 6 workflow の `permissions: contents: read` を再確認

ios / kmp / android-instrumented / maui の platform TFM ビルドは、今回の修正がこれらの job の内容を変えていないため再実行していない (`verify-maui.yml` の変更は件数検査の起動条件のみ)。

## 指摘事項

### [🔵 Suggestion] 件数検査のステップ条件化が maui だけで、他 4 job は `if: always()` のまま

**該当箇所**: `.github/workflows/verify-ios.yml:97`、`verify-android.yml:72`、`verify-kmp.yml:90`、`verify-android-instrumented.yml:91`

review-001 の Minor 2 は「maui は検査が 3 本あるぶん診断性の劣化が大きい」として maui を対象に書かれ、そのとおり修正された。結果として、同じ change の中に 2 つの流儀が並ぶ形になっている。

実害が読めるのは android-instrumented で、この job の主要な失敗モードは Emulator の起動失敗 (`Run instrumented tests on emulator` ステップの失敗) である。そのとき `if: always()` の件数検査が走り、`:ksdialogs` と `:ksdialogs-compose` の 2 件について「`…/androidTest-results/connected` にテスト結果 XML が無い」という、原因ではない `::error::` を出す。android / kmp / ios も `Prepare Android SDK location` や `Select Xcode` の失敗で同じ形になる (検査 1 本ぶん)。

4 本とも maui と同じ `if: ${{ !cancelled() && steps.<test-step-id>.conclusion != 'skipped' }}` に揃えると流儀が 1 つになる。job の成否は変わらない (診断性だけの話) ので、tasks 5.1 の初回実行で実際の annotation の見え方を確認してから判断してよい。

---

### [🔵 Suggestion] `maui/nuget.config` が NuGet キャッシュキーに入っていない

**該当箇所**: `.github/workflows/verify-maui.yml:63`

`key: nuget-${{ runner.os }}-${{ hashFiles('global.json', 'maui/Directory.Packages.props', 'maui/**/*.csproj') }}` は、restore の入力のうち**版を決めるファイル**は網羅しているが、**取得元を決めるファイル** (`maui/nuget.config`) を含んでいない。現状はソースが nuget.org 単一なので実害はない。将来ソース定義を変えたときに古いキャッシュがそのまま復元される形になるため、`hashFiles` の列に `maui/nuget.config` を足しておくと入力の集合と一致する。

---

### [🔵 所見] tasks 3.1 の本文が修正前の条件式のまま

**該当箇所**: `tasks.md:21`

tasks 3.1 は `if: github.event_name == 'pull_request' || needs.changes.outputs.source == 'true'` と書いており、review-001 の Major 2 で `!cancelled() && … != 'false'` へ変えた現行の実装と食い違う。Requirement は満たしているので仕様逸脱ではなく、tasks は足場なので凍結対象でもない。ただし蒸留時に「tasks の指示と実装が違う」と読める形なので、実装済みの形に合わせておくか、`deviation.md` の 4 件目として「レビュー指摘による条件式の変更」を残しておくと後から辿りやすい。

---

### [🔵 所見] handbook が proposed な cross/ADR-0016 を参照している

**該当箇所**: `kasane/handbook/cross/test-execution.md:39`

新設節「CI が回す範囲と手元に残る範囲」の冒頭が `(cross/ADR-0016)` を根拠として挙げている。突き合わせ (second-opinion-code-001 の #6) では「handbook 側の参照はソースコメントではないため対象外、ADR の accepted 昇格は蒸留の申し送り」と整理済みなので、本レビューでも指摘としては扱わない。長命層から proposed を指す状態が残る点だけ、蒸留時の申し送り (ADR-0016 の accepted 昇格) と併せて記録しておく。

---

## 足場・tasks の確認 (指摘なし)

- `proposal.md` と `specs/verification-ci/spec.md` は未変更 (`git status` で M が付かず、`git log` でも起票コミット `e3abbe1` 以降の変更なし)。実装期間中の逆流なし
- `tasks.md` は 1.1〜4.2 と 5.3〜5.8 がチェック済み、5.1 / 5.2 (オーナーの push を要する) は未チェックのまま。`evidence/ci-step-negative-cases.md` と突き合わせて虚偽チェックなし
- `deviation.md` の 3 件 (Sample の `MauiVersion` 明示 / metadata compile 単体の負ケース再現不能 / `changes` が 7 本目の check) 以外に、無断の仕様逸脱は見つからなかった。`maui/nuget.config` の新設は Requirement「ツールチェーンの再現性」の実現手段であり、乖離ではない
- 修正 6 件のいずれも、対応する Scenario の担保を弱めていない (詳細は `verify-001.md` の対応表)

## アクションプラン

1. なし (Critical / Major / 優先度の高い Minor は無し)
2. Suggestion 2 件と所見 2 件は tasks 5.1 / 5.2 の実測と蒸留の申し送りで扱えばよい。前回の Suggestion 4 件 (kmp のターゲット導出・`fetch-depth`・Xcode グロブ・comment-policy の対象拡張子) も同じ扱いのまま
3. `maui/nuget.config` の本文は本レビューでは読めていない。commit 後に diff で `<clear />` / `packageSourceMapping` の内容を一度目視しておくと、指摘 1 の閉じが完全になる
