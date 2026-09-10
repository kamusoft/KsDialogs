# Design: add-release-workflow

## Context

翻案元は KsSettingsView の release workflow 一式 (`../KsSettingsView/.github/workflows/release.yml`、`../KsSettingsView/scripts/release/`、`../KsSettingsView/.github/release.yml`、`../KsSettingsView/kasane/handbook/cross/release-procedure.md`。change: `../KsSettingsView/kasane/changes/archive/2026-09-04-add-release-workflow/` と、初回リリース後の修正 `../KsSettingsView/kasane/changes/archive/2026-09-07-fix-release-central-validation-wait/`)。翻案元の design Decision 11 件のうち、dispatch 起動・tag は最後・version 注入・6 段構成・冪等な再実行・Environment `release`・Release ノート・固有値の `env` 集約は agenda の踏襲項目としてそのまま採り、本 design はそれを繰り返さない。翻案元の設計から**変える**箇所 (KMP 形態の追加に伴う順序と runner、README の version 置換の自動化) と、KsDialogs 固有の値 (Maven 3 座標・NuGet 3 ID・配信リポジトリ・必須 status check 10 件・初回 version) だけを Decision に立てる。

KMP 形態の制約 (add-kmp-maven-distribution の deviation と evidence/swiftpm-reference-derivation.txt で実測済み): 発行物の SwiftPM 連携メタデータには Swift 参照の URL と URL 末尾から導出される `packageName` が焼き込まれる / cinterop klib を伴う iOS publication の発行には配信リポジトリに同版 tag が実在する必要がある / 発行には Xcode が要る。消費者検証 (concepts cross/architecture/consumer-verification.md) の KMP 消費者は dry-run で kmp/ を job 内で `file://` 発行し直すため、package 段の KMP 成果物に消費者は居ない。

翻案元の初回リリース後に見つかった非対称 (新規 upload の経路にだけ検証の決着待ちが無く、`VALIDATING` のまま release して失敗) は、翻案時に最初から `wait-validated` を upload の直後に置く形で取り込む。

## Goals / Non-Goals

Goals: proposal.md の What Changes。Non-Goals: proposal.md の Non-Goals。

## Decisions

### Decision 1: publish は macOS 1 job で、SPM tag を KMP の Maven 発行より前に置く

**採用案:** publish job は `macos-26` (Xcode 選択 + JDK + Android SDK の準備は `verify-kmp.yml` から写す) の 1 job で直列に進む。順序は cross/ADR-0024 (proposed) の表のとおり: SPM スナップショット commit push → Android の Maven upload 保留 → `.asc` 検査 → validated 待ち → SPM tag push → KMP を `-Pversion=<version>` (URL 上書きなし = 既定の https + exact) で `publishAllPublicationsToMavenCentralRepository` 相当のタスク名指定で upload 保留 → validated 待ち → NuGet push → Maven release (Android → KMP) と published 待ち → monorepo tag + GitHub Release → README / Skill の version 置換 commit を `develop` へ push。SNAPSHOT ガードは名前を直接指定した発行タスクで診断だけを出す (集約タスク `publish` は使わない、phase-7 申し送り)。

**理由:** KMP artifact は https + exact で作らないと利用者に `file://` が届き、その発行は同版 tag が実在しないと失敗する。tag を「Android の deployment が validated になった後」に置けば、署名鍵・Portal 認証の失敗は tag より前に出て、tag が残る失敗経路は KMP のビルド / validation だけになる。翻案元 ADR-0020 の「取り消せる順で直列」のうち、配信リポジトリの tag だけは取り消せない操作の前に生まれる例外になる (削除はできるが clone 済みの利用者からは回収できない)。第一の受け皿は翻案元と同じ「同じ version で再実行して埋める」で、放棄するときは番号を欠番にして再利用しない。ロードマップのゴール「tag は publish 全成功後にのみ生まれる」は monorepo の tag を指すものとして読み替え、蒸留時に ksn-roadmap で 1 行直す。KMP の iOS publication に Xcode が要るため runner は macOS。drop と deployment ID の引き継ぎを 1 job に閉じるため job を割らない。

**代替案:**
- **A: 翻案元の順序 (Maven release → SPM tag) のまま** — 却下。KMP の iOS publication が発行できず順序が成立しない (phase-7 C3 の却下前提が崩れた)
- **B: package 段でも `file://` で KMP を発行し publish 段で作り直す** — 却下。消費者が居ない成果物を作るだけで、URL が焼き込まれるため同一性検査も掛けられない
- **C: SPM tag を publish 段の先頭に置く** — 却下。署名鍵・認証の失敗でも tag が残る
- **D: publish を ubuntu と macOS の 2 job に割る** — 却下。drop と deployment ID の引き継ぎが job をまたぐ

### Decision 2: Maven Central の deployment は Android / KMP の 2 枠で持ち回り、各 upload の直後に validated を待つ

**採用案:** deployment を枠 (`android` / `kmp`) として扱い、upload ログからの ID 抽出 (`deployment id: <uuid>` を tail で 1 件)・ID の保存 (1 つの artifact `<KS_ARTIFACT_DEPLOYMENT_ID>` に `deployment-id-android` / `deployment-id-kmp` の 2 ファイル)・再実行時の状態分岐 (VALIDATED → upload skip / PUBLISHING → PUBLISHED 待ち / PUBLISHED → skip / FAILED → drop して再 upload / NOT_FOUND → 再 upload)・`wait-validated` (upload 直後、ID を artifact に保存した後)・`release` + `wait-published`・失敗時の `drop` を枠ごとに繰り返す。`central-portal.sh` は翻案元の契約 (ID 1 引数、`published <version> <artifactId>` の HEAD 判定) のまま座標だけ差し替える。再実行時の状態 → 動作の分岐 (VALIDATED / PUBLISHING / PUBLISHED / FAILED / NOT_FOUND / ID なし) は workflow に直書きせず `scripts/release/central-resume.sh` (仮称) に切り出し、`--selftest` で 6 状態すべての分岐を確かめられる形にする (実レジストリ無しで判別できる検証、lessons code-review L-001)。android/ と kmp/ の `publishToMavenCentral()` は `automaticRelease = false` を明示し、Gradle プロパティ `mavenCentralAutomaticPublishing` で自動公開に倒れる余地を消す。両枠の validated 待ちは NuGet push より前に済むので「両方 validated まで release しない」は順序で満たされ、release 段の状態確認は冪等化の分岐だけ。

**理由:** android/ と kmp/ は別ビルドで vanniktech の upload は Gradle ビルド単位に 1 deployment を作る。翻案元のステップを枠名を変えて 2 回書くだけで済み、スクリプトの契約を変えない。validated 待ちを upload 直後に置くのは翻案元の修正 (fix-release-central-validation-wait) の採用案 A と同じで、検証 FAILED のときに NuGet だけが出て lockstep が崩れる経路を塞ぐ。

**代替案:**
- **A: 2 つの Gradle 出力を 1 つの bundle にまとめて Portal API へ手で upload し deployment 1 件にする** — 却下。upload サブコマンドと署名済み bundle の組み立てが新たな失敗点になり、vanniktech の upload を使わない (phase-7 C3 が退けた形)
- **B: validated 待ちを release 直前にまとめて置く** — 却下。翻案元の修正が却下した案 B と同じ理由 (検証 FAILED 時に NuGet だけ公開される)

### Decision 3: package 段は iOS / Android / MAUI の 3 job、Android は publish と同じ macOS で作り同一性を比べる

**採用案:** package 段は `package-ios` (ubuntu)・`package-android` (**`macos-26`**)・`package-maui` (macos-26) の 3 job。KMP の package job は置かない。消費者 dry-run 4 job は `verify-consumer-{ios,android,maui,kmp}.yml` を `mode: dry-run` + `version` + `artifact` で呼び、`consumer-kmp` には Android の artifact (`jp/` ルート) を渡す (concepts「フィード準備と artifact の配置」の表)。publish job は Android を署名つきで再ビルドし、`compare-maven-artifacts.sh` で package 段の発行物と比べてから upload する (翻案元の「Android 成果物の同一性」)。KMP は publish 段でしか作らないため同一性検査の対象外で、その代わり dry-run が `file://` で解決した 5 publication と publish 段の https 発行物の差が URL 由来のメタデータに限ることを、初回リリースの smoke (https での実解決) で確認する。

**理由:** 翻案元は「publish と同じ OS で再ビルド比較する」ために package-android を ubuntu に置いた。publish が macOS に移る (Decision 1) ので、比較の前提を保つには package-android も macOS で作る。macOS ランナーは public リポジトリで無料。KMP を package 段に置かない理由は Decision 1 の代替案 B。

**代替案:**
- **A: package-android を ubuntu のまま、macOS の再ビルドと比べる** — 却下。OS 差で偽の差異が出たときに切り分けられず、同一性検査が本来止めたい差異 (ソースの取り違え) と区別できない
- **B: 同一性検査を外し、publish の再ビルドだけを信じる** — 却下。dry-run が見たものと外に出るものの一致を保証する手段が無くなる (concepts consumer-verification の前提)

### Decision 4: README / Skill の version 置換は release workflow が pack 前と publish 後に行い、`--check` は validate から外す

**採用案:** `scripts/release/set-readme-version.py` を翻案し、(1) 対象は README 2 枚 (SwiftPM `exact:` / Maven `ksdialogs-core` と `ksdialogs` / NuGet `KsDialogs.Maui` / KMP `ksdialogs-kmp` の各行) と Skill の導入例 (`skills/{en,ja}/ksdialogs-{ios,android,maui,kmp}/SKILL.md`、`ksdialogs-aiforms-migration/SKILL.md`、`ksdialogs-kmp/references/{ios,android}-host.md`。実装時に `grep` で行を列挙して `TARGET_FILES` を確定する)、(2) 対象行は行の形 (配信リポジトリ URL / Maven 座標 / NuGet ID) で見つけ、値が `<version>` プレースホルダでも実値でも新しい version に置く、(3) 各ファイルの期待行が揃わなければ何も書き換えずに失敗する (翻案元と同じ)、(4) `--check` は残す (手元の検査用) が validate job からは呼ばない。workflow は `package-maui` job の pack の前に作業木で置換を実行し (commit しない。dry-run でも行う)、publish job の最後に `develop` を別ディレクトリへ `--depth 1` で clone して置換 → 差分があれば **検証 CI の lint job と同じ検査 (`scripts/local-path-lint.py` / `identity-lint.py` を置換後のファイルに、`readme-example-lint.py` を clone に対して) を掛け** → commit → `git pull --rebase` → push する。push が拒否されたら release を失敗にせず Summarize に警告を出す。`GITHUB_TOKEN` の push は他 workflow を起動しないため、この commit は検証 CI を通らない — その代わりに publish job 内で同じ lint を掛けることで、cross/ADR-0017 / 0022 の「`develop` への push ごとに lint」の保証を実質的に保つ (次のオーナーの push で CI が走る範囲にも含まれる)。AGENTS.md の docs-refresh の行に「リリース時の version 置換だけは release workflow が行う」を足す。

**理由:** 翻案元の「リリース PR で人が置換」は手作業が 1 つだけ残り、オーナーが不便と判断した (agenda R4)。cross/ADR-0016 (accepted) の「README の version 置換はリリース PR の中の commit として行う」はこの案で置き換わるため、cross/ADR-0024 が `amends: [cross/0016]` でその 1 文だけを改訂する (他の決定は維持)。README の意味を「最新の公開版」に変えれば、release が失敗しても README は嘘にならず、更新忘れは起きない。`develop` は PR 必須も必須 check も無く (cross/ADR-0016)、publish job が既に持つ `contents: write` で push できる。`GITHUB_TOKEN` の push は他 workflow を起動しないので CI は連鎖しない。facade の nupkg は pack 時点の README を同梱するため、pack の前に作業木で置換すれば nupkg の README は実値になる。

**代替案:**
- **A: 翻案元の形 (リリース PR で人が置換 + validate の `--check`)** — 却下。手作業が残る
- **B: リリース準備 workflow を別に dispatch し、置換 commit 付きの PR を自動生成** — 却下。dispatch が 2 回になり、release が失敗すると README が未公開の版を指す
- **C: workflow が `main` へ commit** — 却下。翻案元 ADR-0020 の却下理由 (push 権限と protection のバイパス) がそのまま当たる
- **D: PAT や GitHub App の資格情報で push して検証 CI を起動させる** — 却下。長期資格情報を secret に増やす。置換 commit は version 文字列だけの差分で、lint を publish job 内で掛ければ CI の保証は保てる

### Decision 5: MAUI の発行ガードは nupkg 名の検査 2 回、XML ドキュメントは facade だけ 3 TFM で同梱

**採用案:** `package-maui` は pack 後に `KsDialogs.Maui.<version>.nupkg` / `KsDialogs.Binding.iOS.<version>.nupkg` / `KsDialogs.Binding.Android.<version>.nupkg` と対の snupkg の存在を検査し、publish job は download 後・push 直前に同じ検査を行う。`maui/KsDialogs.Maui/KsDialogs.Maui.csproj` に `GenerateDocumentationFile=true` を、binding 2 つの csproj に `false` を明示する (`maui/Directory.Build.props` は共通既定を持たず、各 csproj で宣言する — facade だけ true の非対称を props の条件式で書くより読みやすい)。未記載メンバの警告 (CS1591) が出るかは実装で 1 回 pack して確かめ、出るなら facade の `NoWarn` に足さず、未記載メンバに doc コメントを書くか警告を残すかをオーナーに諮る。

**理由:** NuGet の push は MSBuild の外 (`dotnet nuget push`) にあり、Gradle の SNAPSHOT ガードと同じ場所に置けない。名前検査は `0.0.0-dev` を確実に弾き、artifact の取り違えも止める。XML ドキュメントは agenda R6 (日本語 doc コメント 431 か所を同梱、翻案元と同じ)。

**代替案:**
- **A: MSBuild の target で `0.0.0-dev` の pack を失敗させる** — 却下。手元 pack と消費者検証 (`-p:Version=` を渡さない経路) の運用を壊すだけで、push を止める役には立たない
- **B: XML ドキュメントを pack から外す** — 却下 (agenda R6)。IntelliSense の説明が無くなる
- **C: 英訳して同梱** — 却下 (agenda R6)。翻訳と維持のコスト、cross/ADR-0015 の改訂が要る

### Decision 6: 反映待ちは Maven 3 座標 + NuGet 3 ID、smoke は 4 本、`published` 判定は `ksdialogs-core` の POM

**採用案:** `wait-for-registries.sh <version>` は `repo1.maven.org` の POM を Android 2 座標 (`ksdialogs-core` / `ksdialogs`) と KMP の 5 publication (`ksdialogs-kmp` root / `ksdialogs-kmp-android` / iOS 3 ターゲットの artifactId。実装時に発行物一覧から確定) で、nuget.org の flat container index を `ksdialogs.maui` / `ksdialogs.binding.ios` / `ksdialogs.binding.android` で確認する (計 10 件、間隔 30 秒・上限 45 分)。KMP の root だけを待つと target publication の未反映で smoke が落ち、反映待ちの判別力が無くなる。配信リポジトリの tag は publish 段で push 済みなので待ち対象に入れない。smoke は `verify-consumer-{ios,android,maui,kmp}.yml` を `mode: smoke` + `version` で 4 本呼ぶ。`central-portal.sh published <version>` は `ksdialogs-core` の POM への HEAD で判定する (Android 枠の公開済み判定。KMP 枠は `ksdialogs-kmp` の POM を見る同じ関数を座標引数で使い分ける)。

**理由:** phase-7 C3 の smoke 行と phase-8 の実装 (smoke は `mavenCentral()` + https + exact) に従う。Central Portal に「座標 + version が公開済みか」の API は無く (翻案元の実測)、枠ごとに代表の POM を見るのが最小。

**代替案:**
- **A: 反映待ちに配信リポジトリの tag の存在確認を足す** — 却下。tag は publish job 内で push して存在検査済み。同じ実行で再確認する意味が無い
- **B: `published` 判定を枠共通で `ksdialogs-core` だけにする** — 却下。Android 枠だけ公開済みで KMP 枠が未公開の再実行で、KMP の upload を skip してしまう

### Decision 7: validate は翻案元の検査から README の `--check` を外し、初回リリースは `develop` からの dry-run で予行してから `main` を作る

**採用案:** validate は checkout より前に version 形式 (`^(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)(-(alpha|beta|rc)\.(0|[1-9][0-9]*))?$`) と起動ブランチ (dry-run 以外は `main`) を検査し、checkout 後に monorepo tag (無し / 同 commit → 続行、別 commit → 失敗) と配信リポジトリ tag (`check-distribution-tag.sh`: `absent` / `match` → 続行、差異 → 失敗) を見て、artifact 名 3 つを outputs に出す。**部分 publish の続行は同じ workflow run の再実行 (`github.run_attempt` が 2 以上) に限る**: publish job の外部状態の再検査で、`run_attempt` が 1 (新規 dispatch) なのに当該 version の外部状態 (配信リポジトリの tag、Central の公開済み、nuget.org の存在) のいずれかが既にあり、かつ monorepo tag が起動 commit に無ければ失敗し、「失敗した run を再実行する」よう案内する。別の commit から同じ version を新規 dispatch すると、公開済みの binary と monorepo tag が指す source が食い違う (skip が黙って通す) ため。README の `--check` は持たない (Decision 4)。初回リリースの進め方は、(1) release workflow を `develop` に載せた状態で `dry-run: true` を `develop` から起動して validate → 本体検証 → package → 消費者 dry-run まで通す (dry-run は起動ブランチ制限を免除)、(2) `main` を `develop` の先端から作り branch protection (必須 status check 10 件・PR 必須・force-push / 削除禁止) を付ける、(3) リリース PR (`develop` → `main`) を開いて `main` 宛て PR の検証 CI 10 job の実動を確認してマージする、(4) `main` から `0.1.0-beta.1` を dispatch する。

**理由:** dry-run が起動ブランチ制限を免除されている (踏襲) ので、`main` を作る前に workflow の大半を予行できる。`main` は「作った直後に protection を付ける」(phase-4 申し送り) が要るため、予行を先に済ませておけば `main` を作ってからリリースまでの期間が短くなる。

**代替案:**
- **A: `main` を先に作ってから予行する** — 却下。予行で workflow に直しが要ると、`develop` → `main` の PR を予行のたびに回すことになる
- **B: 新規 dispatch でも外部状態から続きを埋める (翻案元の形)** — 却下。source と binary の対応を保証できない。同じ run の再実行で続きを埋める経路は残る

### Decision 8: 手順書は翻案元の構成のまま KsDialogs の固有値に差し替え、「version を放棄したときの tag 削除」を足す

**採用案:** `kasane/handbook/cross/release-procedure.md` を翻案元の節構成 (ブランチの役割 / 初回だけ行う設定 (main の作成と保護・配信リポジトリの deploy key・Environment release と secrets) / リリースのたびに行うこと (リリース PR・起動・公開後の確認) / 失敗したとき / リハーサル / 関連) で書く。「リリース PR」から version 置換の手作業を外し、「失敗したとき」の表に KMP 枠 (SPM tag push 後の KMP upload / validation の失敗) の行と、version を放棄するときの規則 (番号は欠番にして再利用しない。配信リポジトリの tag の削除は任意の後片付けで、`git push --delete` の手順を添える) を足す。必須 status check は 10 件 (`lint`、`ios / verify`、`android / verify`、`android-instrumented / verify`、`kmp / verify`、`maui / verify`、`consumer-{ios,android,maui,kmp} / verify`)、`app_id: 15368`、`develop` には必須 check を付けない (cross/ADR-0016 / phase-4)。default branch は `develop` のまま (翻案元は `main` を default にしたが、KsDialogs は cross/ADR-0016 で `develop` と決めている)。

**理由:** 翻案元の手順書はオーナーが 2 回のリリースで使った実績があり、構成を変える理由が無い。KsDialogs で変わるのは version 置換の自動化・KMP 枠・default branch の 3 点。

**代替案:**
- **A: default branch を `main` にする (翻案元と同じ)** — 却下。cross/ADR-0016 が `develop` を既定ブランチと決めており、README の画像 URL も `develop` を指す (phase-3)

## Risks / Trade-offs

- KMP の https 発行は dry-run で予行できず、初回の本番 dispatch で初めて通る。失敗しても残るのは配信リポジトリの tag (再実行で `match` skip、放棄なら手で削除) と保留 deployment (drop) だけで、取り消せない操作 (NuGet push / Maven release) より前
- 外部設定 (secrets 7 件・Trusted Publisher・deploy key) は手元で検証できない。publish の先頭 (SPM commit push = deploy key、Android upload の署名検査 = 署名鍵と Portal の認証) が取り消せない操作より前に失敗を出す
- publish job が macOS になり、Android の Gradle ビルドと KMP の iOS publication の発行が加わるため、翻案元の publish 11 分より長くなる。timeout は翻案元の 120 分のまま置き、初回の実測で詰める (agenda 申し送りへ)
- `develop` への version 置換 commit がオーナーの push と競合する余地。release は失敗にせず、次回のリリースで追いつく
- 「互換」の主張はしない。本変更はライブラリの公開面に触れず、MAUI の csproj への XML ドキュメント指定だけがパッケージ内容を変える (nupkg に `.xml` が 3 TFM 分増える)

## Migration Plan

新設のみ。既存の `ci.yml` / `verify-*.yml` は変更しない。`main` ブランチの作成と branch protection、Environment、Trusted Publisher、deploy key はオーナーの手作業 (tasks 群 6)。初回リリース後、docs-refresh 2 回目と蒸留 (配布構成 concepts 化) が続く (proposal Non-Goals)。

## Open Questions

- facade の `GenerateDocumentationFile=true` で CS1591 (未記載メンバ) が出るか。出た場合の扱い (doc コメントを足す / 警告を残す) は実装時にオーナーに諮る (Decision 5)
- publish job の所要時間と、消費者 dry-run 4 job + package 2 job (macOS) の並走で同時実行上限 (5) の待ちがどれだけ出るか。実測して agenda の申し送りへ

## ADR 候補

- Decision 1 / 2 / 4 → cross/ADR-0024 (proposed、agenda で起票済み。cross/ADR-0016 の README 置換の 1 文を amends)。本 design と同内容。蒸留時に accepted へ昇格し、Consequences の導出可能性テストを通す。Decision 3 (package-android の runner) は ADR-0024 の Decision に 1 文足す候補 (「Android の package 段は publish と同じ macOS で作る」) — 蒸留時に判断
- Decision 5〜8 は局所的 (workflow と手順書の詳細) で ADR 化しない
