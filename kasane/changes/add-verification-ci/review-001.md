# レビュー結果: add-verification-ci (001 回目)

**日付**: 2026-09-08
**判定**: CHANGES_REQUESTED

## サマリー

6 本の workflow・`global.json`・central package management の導入は、デルタスペックの 9 Requirement をほぼ全面的に満たしている。とくに「終了コードだけでは検証にならない」という本変更の主張が、5 job すべての件数検査・展開数の突き合わせ・期待 module のビルド構成からの導出として一貫して実装されており、負ケースの証跡も厚い。外部 action 5 種の SHA 固定は 4 種が翻案元と byte 一致、新規の `reactivecircus/android-emulator-runner@a421e43…` は v2.38.0 のタグ commit と一致、gitleaks の SHA-256 も公式 checksums と一致することを本レビューで独立に確認した。

一方で Major 2 件を検出した。(1) central package management を導入しながら翻案元が対で持っている `maui/nuget.config` を写しておらず、restore が開発者のユーザー設定 (複数ソース) に依存したまま NU1507 が新規に出るようになっている。(2) 本体検証 5 job が `needs: changes` で軽量 job に従属しており、`changes` が失敗すると 5 job が skip される — detect スクリプト自体は「判定不能はソース変更あり側へ倒す」と明示的に fail-safe に作られているのに、job グラフだけがその原則から外れており、Requirement「CI の起動条件」の「`main` 宛ての pull_request では常に実行する」が edge case で破れる。

## 照合した規約

| 文書 | 適用のきっかけ |
|---|---|
| cross/comment-policy.md | 常時 (workflow の日本語コメント約 600 行・csproj / props のコメントを規約本文から手で判定。禁止参照・禁止記述類型の該当なし) |
| cross/test-execution.md | 本変更が同文書を改訂し、CI の回す範囲を規定するため。既存の実行コマンド表・件数表と workflow の実行内容を突き合わせた |
| cross/local-development-setup.md | 本変更が同文書を改訂し、`global.json` による SDK / workload set の固定を規定するため |
| cross/ADR-0004 (composite build)・ADR-0013 (外部 PR 不受理)・ADR-0016 (ブランチモデル、`proposed`) | workflow のトリガー・head 制限・Gradle ルート構成の根拠として参照 |

以下は適用のきっかけに当たらないため本文まで読んでいない: sample-parity.md (デモ項目・文言・起動引数に触れていない。`samples/maui` の変更は csproj の版プロパティのみ)、runtime-behavior-verification.md、user-skill-api-listing.md、user-skill-writing-style.md、docs-refresh-timing.md、diagnostic-message-language.md、aiforms-origin-reference.md。

`kasane/lessons/code-review.md` は不在 (重点観点・指摘しないことの登録なし)。

## 実施した検証

- 全 6 workflow の YAML パース、全 41 `run:` ブロックの `bash -n` 構文検査 — すべて成功
- `ci.yml` の `Detect source changes` を workflow から抽出し、合成 git リポジトリで 5 ケース実行 — 記録だけ→`source=false` / 記録+ソース混在→`true` / before 全 0→`true` / before 到達不能→`true` / `pull_request`→`true`。ヒアドキュメント経由の `while` ループなので `emit_true` の `exit` が現シェルで効くことも確認
- `verify-android.yml` の件数検査を抽出し実リポジトリに対して実行 — 期待 module `:ksdialogs` / 68 件 / exit 0
- `dotnet --version` = 10.0.300、`dotnet workload list` = 「global.json で指定されたワークロード バージョン 10.0.300.3 を使用」/ maui manifest 10.0.20 (→ Scenario「手元のビルドが repo の設定で固定される」を独立に確認)
- `dotnet test maui/KsDialogs.Maui.Tests/KsDialogs.Maui.Tests.csproj -c Release` — 155 件成功 (CPM 移行後も restore・ビルド・テストが通る)。ただし NU1507 が新規に発生 (Major 1)
- `cd android && ./gradlew --console=plain test` — BUILD SUCCESSFUL、`:ksdialogs:verifyNoDeclarativeUiDependency` が同じ実行に乗ることを確認
- lint 4 本 (`local-path-lint` / `identity-lint` / `comment-policy-lint` / `scenario-id-coverage`) — すべて exit 0
- `doc-structure-lint.py` を改訂した handbook 2 本 + `cross/index.md` に対して実行 — 違反なし
- 外部 action の SHA 照合 (翻案元との突き合わせ + `gh api` でのタグ解決)、gitleaks 8.30.1 linux_x64 の SHA-256 を公式 checksums と照合
- `maui/macios/native/KsDialogsMauiBridge.xcodeproj` の scheme が `xcshareddata/xcschemes/` に置かれ追跡されていること (CI で `-scheme` が解決できること) を確認

ios / kmp / android-instrumented / maui の platform TFM ビルドは所要時間の都合で再実行していない (コンテキストパッケージの手元実測結果を前提とする)。

## 指摘事項

### [🟠 Major] central package management の導入に `maui/nuget.config` が伴っておらず、restore 元が環境依存のまま (NU1507 の新規発生)

**該当箇所**: `maui/Directory.Packages.props:8` (影響範囲は `maui/**/*.csproj` の restore 全体)

**問題点**:
`ManagePackageVersionsCentrally` を有効にしたことで、複数パッケージソースが定義された環境の restore に NU1507 が新規に出るようになった。本レビューで `dotnet test maui/KsDialogs.Maui.Tests/KsDialogs.Maui.Tests.csproj -c Release` を実行したところ、5 プロジェクトすべてに次の警告が出る (CPM 無効時には出ない、本変更が持ち込んだ警告)。

```
warning NU1507: ... 中央パッケージ管理を使用する場合は、パッケージ ソース マッピング を使用して
パッケージ ソースをマップするか、単一のパッケージ ソースを指定してください: nuget.org, kamusoft
```

翻案元は同じ `Directory.Packages.props` と**対で** `../KsSettingsView/maui/nuget.config` を持っており、そのコメントは目的をこう書いている — 「環境側 (ユーザー設定) にローカルフィード等の追加ソースがあっても継承せず (clear)、packageSourceMapping で全パッケージを nuget.org へ割り当てる。複数ソース環境で出る NU1507 を警告の抑止ではなく原因ごと消し、ローカルフィードの混入で別物を掴む余地も断つ」。本変更は props だけを写しており、この対の片方が落ちている (tasks 1.2 は「KsSettingsView を参考に決め」と指示している)。

実害は 2 つある。

1. **Requirement「ツールチェーンの再現性」の趣旨に反する**。同 Requirement は「ランナーイメージの既定値や**親ディレクトリの設定に依存しない**」ことを求めている。版は `Directory.Packages.props` で固定されたが、その版の**取得元**は依然としてユーザープロファイルの `NuGet.Config` (ここでは `nuget.org` + 私設フィード `kamusoft`) に依存する。CI は nuget.org だけなので、手元だけが私設フィードの成果物を掴む / CI だけが解決できないという非対称が残る
2. 公開予定のリポジトリで、パッケージソースマッピングなしに私設フィードが解決経路に居る状態は dependency confusion の観点でも望ましくない

**推奨修正**: 翻案元と同じ `maui/nuget.config` (`<clear />` + nuget.org 単一 + `packageSourceMapping` で `*` → nuget.org) を置く。`samples/maui` は別ツリーなので対象外である点も同じ。置いたうえで `dotnet test` を再実行し NU1507 が消えることを確認する。

---

### [🟠 Major] `changes` job の失敗で本体検証 5 job が skip され、「PR では常に実行する」が破れる

**該当箇所**: `.github/workflows/ci.yml:119-120`、`125-126`、`131-132`、`137-138`、`143-144` (5 job の `needs: changes` + `if:`)

**問題点**:
デルタスペック Requirement「CI の起動条件」は「`main` 宛ての pull_request では本体検証 5 job (ios / android / android-instrumented / kmp / maui) を**常に**実行する」と定めている。実装は `if: github.event_name == 'pull_request' || needs.changes.outputs.source == 'true'` で式のうえでは常に真になるが、job には `needs: changes` が付いている。GitHub Actions では、`needs` の暗黙の `success()` 要求は **status 関数 (`always()` / `!cancelled()` / `failure()`) を含まない `if:`** では解除されない。したがって `changes` job が失敗すると、pull_request であっても 5 job はすべて skip される。

これは本変更が塞ごうとしている穴と同じ形をしている。`Detect source changes` のスクリプト自身は、判定不能な状態 (before が空 / 全 0 / 到達不能 / 差分が空) をすべて「ソース変更あり」へ倒すよう明示的に設計され、コメントにも「判定不能を『変更なし』と読むと、検証されないまま緑になる push が生まれる」と書かれている。ところが**その判定を運ぶ job そのものが死んだ場合**は、job グラフが逆向き (skip = 検証しない) に倒れる。スクリプトの fail-safe と job グラフの fail-open が非対称になっている。

さらに、skip した job の status check は branch protection の必須 check 評価で成功扱いになり得る (再利用可能 workflow の呼び出し側が skip されたとき、ネストした `ios / verify` という名前の check が生成されるかは実挙動未確認なので、「マージがブロックされる」側に倒れる可能性も残る)。`changes` job 自体は Requirement「platform workflow の再利用契約」が固定する status check 名 6 件に含まれておらず、phase-9 で必須 check に登録される予定もない。つまり「マージ条件として保証する」経路に、必須 check ではない単一 job の生死がぶら下がっている。

なお翻案元 (`../KsSettingsView/.github/workflows/ci.yml`) はこの構造を持たない — あちらは `on.push.paths-ignore` で絞っており `changes` job が存在せず、5 job は `needs` を持たない。`paths-ignore` を捨てて `changes` job へ移した (本変更の正当な判断) 副作用として、翻案元には無かった従属関係が新しく生まれている。

**推奨修正**: 5 job の `if:` を status 関数入りにして、`changes` の失敗を「実行する」側へ倒す。例:

```yaml
    if: ${{ !cancelled() && (github.event_name == 'pull_request' || needs.changes.outputs.source != 'false') }}
```

(`changes` が失敗すると `outputs.source` は空文字になるため `!= 'false'` が真になり、スクリプト側の fail-safe と向きが揃う。`!cancelled()` により concurrency による打ち切りでは走らない。)

あわせて、`changes` を phase-9 の必須 status check 一覧へ足すかどうかを決め、決めた結果を phase-9 agenda の申し送りに反映する (現状の申し送りは 10 件で `changes` を含まない想定と読める)。

---

### [🟡 Minor] handbook の「版を上げるとき」チェックリストに `samples/maui` の `MauiVersion` が無い

**該当箇所**: `kasane/handbook/cross/local-development-setup.md:75`

**問題点**:
現在の記述は「版を上げるときは `global.json` の 2 行と `maui/Directory.Packages.props` の `Microsoft.Maui.Controls` を併せて見直す」となっている。しかし deviation.md で合意されたとおり、`samples/maui/KsDialogs.Sample.Maui/KsDialogs.Sample.Maui.csproj:24` にも `<MauiVersion>10.0.70</MauiVersion>` が直書きされ、MAUI 本体の版を持つ場所は 3 か所になった。

`samples/maui` は Non-Goals により CI で組まないので、`Directory.Packages.props` だけ上げて Sample を忘れても**どの自動検査にも引っかからない**。deviation の理由 (Sample が版を持たないと workload set 既定の 10.0.20 が入り NU1605 で restore が落ちる) がそのまま再発し、誰かが手元で Sample をビルドするまで気づけない。deviation は合意済みの乖離なのでそれ自体は指摘しないが、乖離が作った新しい同期義務が handbook に反映されていない点は本変更の追随漏れである。

**推奨修正**: 同行に `samples/maui/KsDialogs.Sample.Maui/KsDialogs.Sample.Maui.csproj` の `MauiVersion` を加え、CI で検査されないため手で揃える必要があることを 1 文添える。

---

### [🟡 Minor] `if: always()` の件数検査が、前段の失敗時に無関係な `::error::` を 3 本出す

**該当箇所**: `.github/workflows/verify-maui.yml:97-99`、`156-158`、`251-253`

**問題点**:
maui job は件数検査を 3 本持ち、いずれも `if: always()` で無条件に実行される。この job は前段に `Select Xcode`・`Setup .NET`・`Install MAUI workload`・`Prepare Android SDK location` といった環境準備ステップを持つため、それらが落ちるとテストが 1 本も走っていない状態で 3 本の検査がすべて動き、`facade のユニットテストが 1 件も実行されていない` / `… にテスト結果 XML が無い` / `どちらの件数行もログに無い` という**原因ではない** annotation が 3 本並ぶ。実際の原因 (Xcode 不在など) の annotation はその中に埋もれる。

`always()` の意図は「テストが失敗したときも件数の内訳を残す」ことであり (コメントにそう書かれている)、対応するテストステップが skip された場合まで走らせる必要はない。ios / android / kmp は件数検査が 1 本なので影響は小さいが、maui は 3 本あるぶん診断性の劣化が大きい。

**推奨修正**: 各テストステップに `id:` を付け、検査側を `if: ${{ !cancelled() && steps.<test-step-id>.conclusion != 'skipped' }}` にする。

---

### [🔵 Suggestion] kmp の検査対象ターゲットだけが固定列挙で、ビルド構成から導出されない

**該当箇所**: `.github/workflows/verify-kmp.yml:103`

android / android-instrumented は期待 module を `android/settings.gradle.kts` の include から導出し、「導出集合が空なら検査自体を失敗」という安全弁まで持つ。kmp だけは `TARGET_TASKS = ["testAndroidHostTest", "iosSimulatorArm64Test"]` の固定列挙で、コメントも「ターゲットが増えたらここへ足す」と手動更新を前提にしている。ターゲットが増えたとき、`allTests` は新ターゲットのテストを走らせるが件数検査からは黙って漏れる (0 件でも緑)。Requirement「KMP の検証」は「実行件数はターゲットごとに検査し」としか書いていないので仕様違反ではないが、他 3 job が採った「構成から導出する」規律との非対称は残る。

`kmp/ksdialogs-kmp/build/test-results/` 配下に実在するターゲットディレクトリを列挙し、固定列挙との差集合が空でなければ警告 (または失敗) する、といった突き合わせを足すと 3 job と揃う。

---

### [🔵 Suggestion] `changes` job が pull_request でも全履歴 clone する

**該当箇所**: `.github/workflows/ci.yml:54`

`fetch-depth: 0` は push の差分計算にだけ必要で、pull_request では `Detect source changes` の 1 行目で `emit_true` して終わる。`fetch-depth: ${{ github.event_name == 'push' && 0 || 1 }}` 相当にすると PR ごとの全履歴取得を避けられる。所要時間の実測 (tasks 5.1) の結果次第で判断すればよい。

---

### [🔵 Suggestion] Xcode 選択のグロブが前方一致で、将来のマイナー版で誤選択し得る

**該当箇所**: `.github/workflows/verify-ios.yml:39`、`verify-kmp.yml:36` 相当、`verify-maui.yml:39` 相当

`ls -d "/Applications/Xcode_${KS_XCODE_VERSION}"*.app` は `26.5` に対して `Xcode_26.5.app` / `Xcode_26.5.1.app` を拾うが、仮に `KS_XCODE_VERSION=26.1` としたときは `Xcode_26.10.app` も一致してしまう (`sort -V | tail -n 1` で後者が選ばれる)。現行値では実害がない。`"…_${KS_XCODE_VERSION}"[._]*.app` と `"…_${KS_XCODE_VERSION}.app"` の 2 パターンに分けると意図どおりになる。

---

### [🔵 Suggestion] comment-policy-lint の対象拡張子に `.yml` が無く、新設 workflow のコメントが機械検査の外にある

**該当箇所**: `kasane/config.yaml` の `lint.comment-policy.ext` (現在 `[]`)、`scripts/comment-policy-lint.py:56` の既定列挙

cross/comment-policy.md は `always: true` で「コメント構文を持つ全ソースファイル (ビルドスクリプトを含む)」を対象としているが、既定の対象拡張子に `.yml` / `.yaml` は無い。本変更は日本語コメント約 600 行を含む workflow 6 本を新設したので、この差が実質的に効くようになった。本レビューでは規約本文から手で判定し、禁止参照 (作業文書パス・裸の変更識別子・レビュー通番) と禁止記述類型 (履歴記述・デルタスペック構文キーワード) の該当は無いことを確認している (`ci.yml` の `kasane/**` は設計根拠としての文書参照ではなく、直下の `case kasane/*)` が扱う入力パスそのものの説明であり、規約の趣旨に反しない)。

ただし `.yml` を対象に加えると、その `kasane/**` の記述が BLOCKING として検出される可能性が高い。対象拡張子を広げるか、workflow を規約の適用外と明示するかはオーナー判断であり、本変更の範囲外の config 変更になるため所見にとどめる。

---

### [🔵 所見] macOS ランナー上の Android SDK 依存 (tasks 5.1 の初回実行で確認したい点)

`verify-kmp.yml:78-86` と `verify-maui.yml:75-84` の `Prepare Android SDK location` は `ANDROID_HOME` 未設定を即 fail させる作りで、fail-safe としては正しい。ただし kmp job は composite build (`kmp/settings.gradle.kts` の `includeBuild("../android")`) を巻き込むのに `kmp/local.properties` しか置かず、取り込まれる `android/` 側は `ANDROID_HOME` フォールバックに依存する (`verify-android.yml` は `android/local.properties` を置くので非対称)。maui job も同様に `maui/android/native/local.properties` だけを置く。翻案元は macos-26 で `net10.0-android` をビルドしているので `ANDROID_HOME` は存在すると見てよいが、Gradle を macOS 上で回すのは本リポジトリが初めてなので、tasks 5.1 の初回 push でこの 2 job の SDK 解決を実ログで確認しておくとよい。

---

## 足場・tasks の確認 (指摘なし)

- `proposal.md` と `specs/verification-ci/spec.md` は実装中に書き換えられていない (`git status` で未変更)
- `tasks.md` のチェック状態に虚偽なし。`5.1` / `5.2` (GitHub 上の実挙動) は未チェックのままで、それ以外は `evidence/ci-step-negative-cases.md` に対応する記録がある
- `deviation.md` に記録された乖離 (Sample の `MauiVersion` 明示) 以外に、spec からの無断逸脱は見つからなかった
- Requirement 9 件 / Scenario 21 件のうち、実装で担保されていない (= GitHub 上でしか確認できない) のは「連続する push で古い実行が打ち切られる」「main 宛ての PR で起動する」「status check 名が固定される」の 3 件で、いずれも Non-Goals と tasks 5.1 / 5.2 / phase-9 申し送りに明記されている

## アクションプラン

1. **Major 1** — `maui/nuget.config` を翻案元と同じ内容で新設し、`dotnet test` で NU1507 が消えることを確認する
2. **Major 2** — `ci.yml` の 5 job の `if:` を `!cancelled() && (… || needs.changes.outputs.source != 'false')` へ変え、`changes` を phase-9 の必須 check 一覧に含めるかを決めて申し送りへ反映する
3. **Minor 1** — `local-development-setup.md:75` の版更新チェックリストに `samples/maui` の `MauiVersion` を足す
4. **Minor 2** — `verify-maui.yml` の件数検査 3 本を、対応するテストステップの `conclusion` で条件付ける
5. Suggestion 4 件は tasks 5.1 の実測結果を見てから採否を判断してよい (kmp のターゲット導出・`fetch-depth`・Xcode グロブ・comment-policy の対象拡張子)
