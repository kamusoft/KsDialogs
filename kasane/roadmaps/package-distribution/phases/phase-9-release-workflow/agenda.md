# release workflow と初回リリース (release-workflow)

KsSettingsView の `release.yml` をコピー + 固有値の差し替えで逆流させ、4 形態一斉の release workflow (validate → test ∥ package → dry-run → publish → 反映待ち → smoke) と外部設定 (Trusted Publisher / Environment / secrets / deploy key) を整備し、初回リリースまで到達する change フェーズ。

## 論点

(2026-09-10 に R1〜R7 をすべて決定事項へ移した。番号は決定事項の見出しに残る)


### KsSettingsView phase-8 からの申し送り (2026-09-04、library-foundation phase-11 から移送)

コピーして値を差し替える形で逆流する (共有 workflow 化はしない):

- `.github/workflows/release.yml` — 固有値 (Maven 座標・NuGet Package ID・配信リポジトリ名・artifact 名・Portal の URL) は冒頭の `env` に集約されている。段構成は validate → (test ∥ package) → dry-run (消費者検証に artifact を渡す) → publish (直列 1 job、`environment: release`) → 反映待ち → smoke
- `scripts/release/` — `central-portal.sh` (Central Portal Publisher API の status / release / wait-published / drop / published)、`set-readme-version.py`、`wait-for-registries.sh`、`check-signatures.sh`、`compare-maven-artifacts.sh` (再ビルドの同一性)、`check-distribution-tag.sh` (配信リポジトリの tag 判定)、`scripts/spm-snapshot/sync-snapshot.sh` (phase-5 で先に取り込む)
- `.github/release.yml` (Release ノートの分類) と `kasane/handbook/cross/release-procedure.md` (GitHub 設定とリリース手順の guide) を KsDialogs 側にも翻案する
- KsDialogs 側で別途作るもの: nuget.org の Trusted Publisher Policy (Repository `KsDialogs` / Workflow `release.yml` / Environment `release` / Glob `KsDialogs.*`、Scopes は push のみ)、GitHub Environment `release` (deployment branch policy = リリース対象ブランチ) と secrets 7 件 (`MAVEN_CENTRAL_USERNAME` / `MAVEN_CENTRAL_PASSWORD` = Central Portal の User Token、`SIGNING_KEY` / `SIGNING_KEY_ID` / `SIGNING_PASSWORD`、`NUGET_USER`、`SPM_DEPLOY_KEY`)、配信リポジトリの deploy key、branch protection
- 実測で分かった前提: Central Portal に「座標 + version が公開済みか」の API は無く `repo1.maven.org` の HEAD で判定する / drop 済み deployment の status は 404 / 初回リリースの所要は 39 分 (validate 9 秒 / test ∥ package 7 分 / consumer-maui dry-run 12 分 / publish 11 分 / smoke-maui 9 分)

### phase-4 からの申し送り: `main` の branch protection (2026-09-08)

`main` を作成した直後に branch protection を付ける (REST は実在するブランチにしか PUT できない)。内容は KsSettingsView の `main` と同じ形 (必須 status check・PR 経由必須 (承認数 0)・force-push 禁止・削除禁止・admin バイパスは緊急時の逃げ道として許容。`gh api -X PUT` は全体置換なので完全な payload を送る)。必須 status check は `{"context": ..., "app_id": 15368}` 形式で次の 10 件。`develop` には必須 check を付けない (cross/ADR-0028 翻案元の決定、phase-4 で確認済み)。

| 種別 | check 名 |
|---|---|
| lint | `lint` |
| 本体検証 5 本 (phase-4) | `ios / verify`、`android / verify`、`android-instrumented / verify`、`kmp / verify`、`maui / verify` |
| 消費者検証 4 本 (phase-8 で job 名確定) | `consumer-ios / verify`、`consumer-android / verify`、`consumer-maui / verify`、`consumer-kmp / verify` |

初回リリースの PR (`develop` → `main`) で、検証 CI の `main` 宛て PR に関する Scenario (pull_request トリガーでの起動・head 制限・status check 名の固定) を実動で確認する。phase-4 の change (add-verification-ci) では `main` が無いためステップ単体の確認までしか行えない (2026-09-08 申し送り)。

phase-4 の実装結果 (2026-09-08): 入口 `ci.yml` は変更検出 job `changes` を持ち、7 本目の補助 check として報告される。必須 check は上表の 10 件のままで `changes` は含めない (cross/ADR-0017)。`develop` push の実動 (lint + 5 job の起動・記録だけの push でのスキップ・連続 push の打ち切り) は確認済みで、残るのは `main` 宛て PR 側だけ。

### phase-7 からの申し送り (2026-09-09)

KMP の発行設定は実装済み (`kasane/changes/archive/2026-09-09-add-kmp-maven-distribution/`、cross/ADR-0008 / 0009 は accepted)。phase-7 の C3 (release の 4 本目) に対して、実装で判明した制約を論点として足す。

**SPM tag の先行 (論点)**: KGP は発行時に SwiftPM パッケージを解決するため、既定 URL (`https://github.com/kamusoft/KsDialogs-SPM`) でリリース版の iOS publication (cinterop klib 付き) を `publishToMavenLocal` するには、配信リポジトリに同版の tag が実在する必要がある (未 push だと `no versions of 'ksdialogs-spm' match the requirement` で失敗。root publication だけなら通る)。C3 の package 段「kmp/ を `-Pversion=` で publishToMavenLocal (Swift 参照は既定の https + exact)」は publish 段より前では成立しない。どの形でも「SPM tag が KMP の Maven artifact より先に存在する」ことが必須になる。

| 取りうる形 | 影響 |
|---|---|
| (a) package 段の kmp は `file://` のスナップショット clone で発行し、publish 段で SPM tag push 後に既定 URL で発行し直す | 再ビルドの同一性検査 (`compare-maven-artifacts.sh`) の対象と、dry-run 用成果物との関係を決め直す |
| (b) publish 順序を SPM tag → Maven upload にする | C3 が却下した「Maven release が失敗すると配信リポジトリに tag だけが残る」を、tag の削除運用か prerelease tag で受ける |

| 項目 | 内容 |
|---|---|
| SNAPSHOT ガードの経路 | ガードは設定段階の要求タスク名照合 + task graph 確定時の 2 段で、名前を直接指定した Central 向けタスクは SNAPSHOT 診断のみで止まる。集約タスク `publish` 経由だけは認証情報未解決と同時に報告される。workflow は発行タスクを名前で直接呼ぶ |
| docs-refresh の KMP 分 | Skill `ksdialogs-kmp` の依存スコープを `implementation` から `api` へ / Kotlin サポート範囲 (同 minor 2.4.x、確認済み版はカタログの `kotlin`) / 「予定している公開 coordinate」の状態表記 / SNAPSHOT を Maven local へ発行した成果物は同一マシンでしか動かない旨。phase-5 / 6 分と同じ依頼にまとめる |
| 配布構成の concepts 化 (KMP 分) | 5 publication (root + android + iOS 3 ターゲット) の内容と SwiftPM 連携メタデータ・version 導出と Swift 参照導出・`@Throws` 回帰検査の位置づけは、「phase-6 からの申し送り」の配布構成 concepts 化の行に含めて置き場を決める |

### phase-8 からの申し送り (2026-09-10)

消費者検証は実装済み (`kasane/changes/archive/2026-09-10-add-consumer-verification/`、仕組みは concepts [消費者検証](../../../../concepts/cross/architecture/consumer-verification.md))。release からは `verify-consumer-{ios,android,maui,kmp}.yml` を dry-run (package 段の artifact を `artifact` 入力で渡す) と smoke (`version` 必須) で呼ぶ。

| 項目 | 内容 |
|---|---|
| artifact の配置 | package 段が upload するルート構造は concepts「フィード準備と artifact の配置」の表のとおり。KMP は Android 分のローカル Maven リポジトリだけを渡し、kmp/ の発行とスナップショット clone の tag は消費者 job 内で行う (package 段で作る kmp/ の成果物は Swift 参照が既定の https + exact で、tag が無い dry-run では解決できない) |
| 所要時間の実測 (2026-09-09、コールドキャッシュ) | 消費者 job: ios 46 秒 / android 2 分 50 秒 / kmp 9 分 20 秒 / maui 15 分 26 秒 (timeout 30 / 30 / 30 / 40 分)。artifact 入力時: kmp 8 分 10 秒 / maui 13 分 24 秒。`main` 宛て PR の壁時計は 19 分 46 秒で、macOS 6 job のうち本体の ios / maui が約 7 分 40 秒待った。release の dry-run 段の並走数はこの待ちを見込んで決める |
| `main` の作成 | 一時 `main` で `main` 宛て PR の Scenario (10 job の起動・status check 名・consumer job の dry-run) は確認済み。phase-9 で `main` を作るときは branch protection と必須 status check 10 件 (「phase-4 からの申し送り」の表) を併せて登録する |
| README の KMP ホスト側の例 | README に KMP のホスト側 (iOS / Android) の登録例を載せるかは初回リリース前の docs-refresh の論点に含める。載せるなら `scripts/readme-example-lint.py` の対応表に 2 行足す (cross/ADR-0022 の Revisit When) |
| MAUI の依存警告の負ケース | `WarningsAsErrors` (NU1605 / NU1608 / NU1107) を故意に起こす実行証跡は無い (宣言のみ)。smoke で警告が表面化する経路は実測済み。必要なら release の change で 1 ケース足す |

## 決定事項

踏襲 (解決済み論点)。出典は cross/ADR-0009 (lockstep)、KsSettingsView cross/ADR-0019・cross/ADR-0020 (dispatch 起動・tag は最後・version 注入) と同 phase-8 の決定事項 (`../KsSettingsView/kasane/roadmaps/package-distribution/phases/phase-8-release-workflow/agenda.md`)。

- 起動は `workflow_dispatch` (version 入力、正規表現 `^[0-9]+\.[0-9]+\.[0-9]+(-(alpha|beta|rc)\.[0-9]+)?$` のみ通す)。`dry-run` 入力で publish 以降を行わないリハーサル。本番はリリース対象ブランチからのみ、secrets は Environment `release`
- version の SSoT は dispatch 入力 (= tag)。CI が `-Pversion=` / `-p:Version=` で注入し、ファイルは開発用既定値のまま (bump コミットを積まない)。tag は接頭辞なし `X.Y.Z`
- publish は取り消せる順で直列: SPM commit push (deploy key の認証失敗を不可逆操作の前に出す) → Maven upload 保留 (`.asc` 生成確認) → NuGet push (Trusted Publishing / OIDC、API key を保管しない) → Maven release (Publisher API、deployment ID は plugin ログから抽出し artifact で attempt をまたいで引き継ぐ) → SPM tag → monorepo tag + GitHub Release。失敗時は `if: failure()` で保留 deployment を drop
- 再実行は同じ version で「失敗した job から再実行」。publish 段の全ステップを存在検査で冪等化 (commit 差分ゼロなら skip / `--skip-duplicate` / tag は同 commit なら skip・別 commit なら失敗 / Release は既存なら触らない)
- Release ノートは GitHub 自動生成 + `.github/release.yml` のラベル分類。CHANGELOG ファイルは持たない
- concurrency group は `release`、cancel-in-progress は false
- 却下済み: tag push トリガー / ファイルを version の正にして tag と照合 / `vX.Y.Z` 表記 / 開発ブランチから起動 / publish 済み version の再実行禁止 / smoke 成功後に tag / workflow が README をリリース対象ブランチに commit / 共有 workflow 化 (理由は cross/ADR-0020 と KsSettingsView phase-8 の Alternatives)

### R1 KMP の Maven 発行は publish 段の中で SPM tag push の直後に行い、publish job は macOS 1 本 (2026-09-10)

KMP の発行物 (SwiftPM 連携メタデータ) には Swift 参照の URL と URL 末尾から導出される `packageName` が焼き込まれ、cinterop klib を伴う iOS publication の発行は配信リポジトリに同版 tag が実在しないと失敗する (add-kmp-maven-distribution の deviation / evidence/swiftpm-reference-derivation.txt)。したがって Central へ上げる KMP artifact は https + exact で、SPM tag の後にしか作れない。phase-7 C3 が却下した「SPM tag を Maven release より前に push する」は前提 (package 段で KMP を先に作れる) が崩れたため改め、tag は「Android 分の deployment が validated になった後」に置く。署名鍵・Portal 認証の失敗は tag より前に出て、tag が残るのは KMP のビルド / validation 失敗だけになる。package 段に KMP の job は置かない (消費者検証は job 内で `file://` 発行し直す (phase-8) ので消費者が居ず、URL が違うため同一性検査 `compare-maven-artifacts.sh` も掛けられない。コンパイルは `kmp / verify` が担保)。iOS publication の発行に Xcode が要るため publish job は macOS runner 1 本で直列にし、drop と deployment ID の扱いを 1 job に閉じる。踏襲元 (KsSettingsView cross/ADR-0020) の「取り消せる順で直列」は tag が削除可能なので保たれ、cross/ADR-0008 / 0009 との衝突はない。

| 順 | ステップ | 失敗したときに残るもの |
|---|---|---|
| 1 | SPM スナップショット commit push (差分なしなら skip) | なし |
| 2 | Android の Maven upload 保留 → `.asc` 検査 → validated 待ち | 保留 deployment (drop) |
| 3 | SPM tag push (同 commit なら skip) | tag |
| 4 | KMP を https + exact で発行 → upload 保留 → validated 待ち | tag + 保留 deployment 2 件 (drop) |
| 5 | NuGet push (OIDC) | 以降は取り消せない領域 (翻案元と同じ) |
| 6 | Maven release 2 件 (Android → KMP) + published 待ち | |
| 7 | monorepo tag + GitHub Release | |

- 残る tag の扱い: 同 version の再実行なら手順 3 が `match` で skip されるため何もしない。version を放棄するときだけ配信リポジトリの tag を手で消す手順を handbook (release-procedure) に置く
- 対価: KMP の https 発行は dry-run で予行できず本番でしか通らない (`file://` 発行と URL 以外は同じ経路)
- 却下: package 段でも `file://` で KMP を発行し publish 段で作り直す (消費者の居ない成果物を作るだけ) / SPM tag を publish 段の先頭に置く (署名鍵・認証の失敗でも tag が残る) / publish job を ubuntu と macOS の 2 本に割る (drop・deployment ID の引き継ぎが job をまたぐ)
- → [cross/ADR-0024](../../../../decisions/cross/0024-release-dispatch-serial-publish-spm-tag-before-kmp.md) (proposed。踏襲分と併せて 1 本)

### R2 Maven Central の deployment は Android / KMP の 2 枠で持ち回り、反映待ちと smoke に KMP を足す (2026-09-10)

android/ と kmp/ は別ビルドのため deployment は 2 件 (Android = `ksdialogs-core` + `ksdialogs`、KMP = 5 publication)。deployment を「枠 (Android / KMP)」として扱い、ID の保存 (1 つの artifact に枠名のファイル 2 つ)・状態分岐 (VALIDATED / PUBLISHING / PUBLISHED / FAILED / NOT_FOUND)・release + published 待ち・失敗時の drop を枠ごとに繰り返す (翻案元のステップの写し、または枠名を引数にした composite step)。`central-portal.sh` は ID を引数に取る現状のまま変えない。「両方 validated まで release しない」は R1 の順序 (両方の validated 待ちが NuGet push より前) で満たされ、release 段の状態確認は冪等化の分岐だけ。Android release → KMP release の間に数分の窓が開くが、利用者は GitHub Release 以後に導入するので実害はない。

- 反映待ち (`wait-for-registries.sh`): Maven の POM を `ksdialogs-core` / `ksdialogs` / `ksdialogs-kmp` の 3 座標で確認 (NuGet 3 ID は踏襲)。配信リポジトリの tag は publish 段で push 済みのため待ち対象に入れず、`check-distribution-tag.sh` は publish 段の存在検査だけに使う
- smoke: `verify-consumer-kmp.yml` を `mode: smoke` + `version` で呼ぶ 4 本目を足す (`mavenCentral()` + https + exact は phase-8 で実装済み)
- 却下: 2 つの Gradle 出力を 1 つの bundle にまとめ Portal API へ手で upload して deployment 1 件にする (upload サブコマンドと署名済み bundle の組み立てが要り、vanniktech の upload を使わない。phase-7 C3 が退けた形)

### R3 初回リリースは `0.1.0-beta.1`、GitHub Release は prerelease (2026-09-10)

翻案元 (KsSettingsView) と同じ値と扱い。suffix 付きの version は GitHub Release に `--prerelease` を付ける (Maven Central は suffix を同格に扱うため README の prerelease 節が説明を担う)。理由: AiForms.Maui.Dialogs からの移行利用者を迎える前提で API を固めた宣言はまだ早く、試用版であることと 0.x の API 変更余地が伝わる。Issue テンプレートの例示 (`0.1.0-beta.1`) とカタログの開発既定値 (`0.1.0-SNAPSHOT`) に揃う。以後は `beta.2` → `rc.1` → `0.1.0` と刻める。

- 却下: `0.1.0` (正式版と読まれ prerelease の段が無い) / `1.0.0-beta.1` (1.0 が近いと読まれ、カタログとテンプレート両方の書き換えが要る)

### R4 README / Skill の version 置換は release workflow が行い、人の手作業をなくす (2026-09-10)

翻案元の「リリース PR の中でオーナーが手で `set-readme-version.py` を実行し validate が `--check` で止める」形は手作業が 1 つだけ残って不便なため採らず、release workflow が 2 箇所で書く。(1) package-maui job が pack の前に作業木で置換を実行する (commit しない) — facade の nupkg に同梱される README (`KsDialogs.Maui.csproj` の `PackageReadmeFile`) が実値になる。(2) publish job の最後 (GitHub Release 作成の後) に `develop` を checkout して置換を commit し push する — `develop` は force-push 禁止・削除禁止だけで PR 必須も必須 check も無く、publish job が持つ `contents: write` で push できる。push 前に rebase し、競合しても release は失敗にせず Summarize に警告を出す (次回の release で追いつく)。`main` は次のリリース PR で `develop` から追従する。dry-run では (2) を行わない。validate の `--check` は廃止し、README の意味は「最新の公開版」になる。`GITHUB_TOKEN` の push は他の workflow を起動しないため CI は連鎖しない。翻案元 ADR-0020 の却下案は「`main` に commit」(push 権限と protection のバイパスが要る) で、`develop` 宛てはその理由に当たらない。AGENTS.md には「リリース時の version 置換は release workflow が行い docs-refresh を経ない」の例外 1 行を足す。

- スクリプトは行の形 (配信リポジトリ URL / Maven 座標 / NuGet ID) で対象行を見つけ、値がプレースホルダ `<version>` でも実値でも新しい version に置く (初回の `<version>` を吸収)。対象は README 2 枚と Skill の導入例すべて (SKILL.md + `ksdialogs-kmp/references/{ios,android}-host.md`) を列挙し、期待した行が無ければ失敗。対象行の形は docs-refresh (R5) の後に確定する
- 却下: 手で実行 + validate の `--check` (元の踏襲案。手作業が残る) / 「リリース準備」workflow を別に dispatch して置換 commit 付きの PR を自動で作る (dispatch が 2 回になり、release が失敗すると README が未公開の版を指す) / workflow が `main` へ commit (ADR-0020 の却下理由のまま)
- 派生: KsSettingsView 側も phase-9 の実装後に同じ形を逆流させる (このロードマップの外で起票)。→ cross/ADR-0024 (proposed) の Decision に追記

### R5 docs-refresh は提案化の前と初回リリース後の蒸留の後の 2 回、README に KMP ホスト側の例は載せない (2026-09-10)

1 回目はこのフェーズ議論の締め (ksn-propose の前) に走らせる。出典は現行 concepts (Android の新座標は android/api/dialog-surface.md にある) と、concepts に無い分 (MAUI の下限版 10.0.20・ガード `KSDLG0001`・iOS 17 / Android 24・API 版付き TFM・`ViewCreationFailed`、KMP の依存スコープ `api`・Kotlin 同 minor と確認済み版・「予定している公開 coordinate」の状態表記・Maven local 発行物の可搬性) を phase-6 / 7 の申し送り表として docs-refresh の Input に添える。handbook「docs-refresh を走らせる時点」の例外規定 (途中で走らせたら蒸留後にもう一度) に沿う。提案化の前に済ませれば、release change の version 置換スクリプト (R4) が確定した README / Skill の行の形を見て書ける。2 回目は初回リリース後の phase-9 蒸留 (配布構成 concepts 化を含む) の後で、「未配信」の状態表記の解除と配布構成を反映し、manifest をその時点の concepts で確定する。

- README の KMP ホスト側の例は載せない。README は最小例 4 ブロックのまま (cross/ADR-0012 の「ルート README は入口」)、ホスト側の登録手順は Skill `ksdialogs-kmp/references/{ios,android}-host.md` が持つ。`readme-example-lint.py` の対応表は動かさない (cross/ADR-0022 の Revisit When は発火しない)
- 却下: 1 回目を release change の実装後・リリース PR 直前にする (実装中の README が旧座標のままで、R4 のスクリプトを後から直す) / 配布構成の concepts 化を ksn-concept で先に済ませて 1 回にする (リリース後の状態表記解除で 2 回目がどのみち要り、置き場の議論を前倒しするだけ)

### R6 MAUI の発行ガードは workflow 側の nupkg 名検査 2 回、XML ドキュメントは facade の 3 TFM で同梱 (2026-09-10)

**発行ガード**: package-maui job は pack 後に 3 つの nupkg が `<ID>.<入力 version>.nupkg` の名前で存在することを検査し、publish job は push の直前に同じ検査をもう一度行う (artifact の取り違えを止める)。`0.0.0-dev` の nupkg は名前の時点で弾かれる。NuGet の push は MSBuild の外 (`dotnet nuget push`) にあり、Gradle の SNAPSHOT ガード (Central 向けタスクを止める) と同じ場所に置けない。MSBuild で pack 自体を止める形は手元 pack と消費者検証の運用を壊すだけで得がなく採らない。

**XML ドキュメント**: facade (`KsDialogs.Maui`、doc コメント 431 か所・日本語) は 3 TFM すべてで `GenerateDocumentationFile=true` を明示して nupkg に同梱し、binding 2 つ (doc コメントなし) は生成しないことを明示して SDK 既定への暗黙依存を消す。理由: 主な移行元 (AiForms.Maui.Dialogs) の利用者層には日本語の IntelliSense が実用になり、無いより公開 API の意図が伝わる。翻案元 KsSettingsView と同じ。英語圏の利用者に日本語が見える対価は、cross/ADR-0015 が doc コメントを英語化の対象外にしたときに受け入れ済み。実装時に未記載メンバの警告 (CS1591) の有無を実測する。

- 却下: pack から外す (説明なし、Skill 頼み) / 英訳して同梱 (431 か所の翻訳と維持、ADR-0015 の改訂が要る非ゴール範囲外の作業)

### R7 MAUI の依存警告を故意に起こす負ケースは release の change に足さない (2026-09-10)

消費者 MAUI の `WarningsAsErrors` (NU1605 / NU1608 / NU1107) の実効性の証跡は無いまま見送る。守っている経路 (README / Skill が古い MAUI の版を案内する) は phase-6 の申し送りと docs-refresh (R5) で入口を塞いでおり、NU1605 は .NET SDK が既定で error 扱いにする。負ケースには MAUI の版を意図的に食い違わせたフィードが要り、release の change (主題は workflow と外部設定) に消費者検証の話を混ぜて初回リリースを遅らせる価値がない。Skill / README が MAUI の版を書くようになったら簡易起票で別 change にする。

- 却下: release の change に 1 ケース足す (tasks が増え主題が混ざる) / 今すぐ簡易起票 (必要になる時点が来ていない)

### phase-5 からの申し送り (2026-09-08)

利用者向け成果物 (`skills/{en,ja}/ksdialogs-android/**`・`skills/{en,ja}/ksdialogs-kmp/references/android-host.md`・`README.md`・`README_ja.md`) に旧座標 `jp.kamusoft:ksdialogs-compose` と本体としての `jp.kamusoft:ksdialogs` が計 16 箇所残る。cross/ADR-0019 の新座標 (Compose 側 `ksdialogs` / 本体 `ksdialogs-core`) への追随は docs-refresh の責務で、初回リリースより前に走らせる (release が先だと存在しない座標を案内する README が公開される)。「docs-refresh のタイミング」の論点はこの残存を潰す前提で決める。

version の注入と SNAPSHOT ガードは android/ に配線済み (cross/ADR-0009 に記載)。release workflow は `-Pversion=` を android/ と kmp/ の両ビルドに渡す。SwiftPM のスナップショット同期は `scripts/spm-snapshot/sync-snapshot.sh` (配置まで。commit / tag / push は workflow 側)。検証用の `verify-https-resolution.sh` は手動検証専用で release から呼ばない。

### phase-6 からの申し送り (2026-09-08)

| 項目 | 内容 |
|---|---|
| MAUI の発行版ガード | `maui/Directory.Build.props` の開発既定値 `0.0.0-dev` のまま pack した nupkg は成立する (pack 検算で実測)。release workflow 側で MAUI 3 パッケージの注入値の形式検査と `0.0.0-dev` 発行の禁止を持つ。android/ の SNAPSHOT ガード (cross/ADR-0009) に相当するものが MAUI 側には無い (review-001 Suggestion 2) |
| facade nupkg の XML ドキュメント | `GenerateDocumentationFile` の指定がリポジトリに無く、.NET Android SDK の既定で `lib/net10.0-android36.0/KsDialogs.Maui.xml` だけが入る非対称 (`net10.0` / `net10.0-ios26.0` には無い)。初回発行前に「全 TFM で生成して同梱する (日本語 doc コメントが公開物に載る)」か「pack から外す」かを決め、`maui/Directory.Build.props` に明示して SDK 既定への暗黙依存を消す (review-001 Minor 2) |
| docs-refresh の内容 (MAUI 分) | README 互換表と導入節、skills `ksdialogs-maui` / `ksdialogs-aiforms-migration` に、MAUI 10.0.20 以上 (同じ workload set なら版を書かなくてよい・古い版を書くと NU1605)・最低 OS 版 iOS 17 / Android 24 とガード `KSDLG0001`・API 版付き TFM の SDK 要件・失敗種別 `ViewCreationFailed` を反映する。phase-5 の Android 座標分と 1 回の依頼にまとめる |
| 配布構成の concepts 化 | MAUI の 3 パッケージ構成・pack 経路・最低 OS ガード・SDK 更新時の再検証箇所 (manifest 絶対パス・API 版付き TFM・自 assembly 用 aar の有無) は、phase-5 の Native 配布経路 (SwiftPM スナップショット・Maven 発行・版の導出) と併せて phase-9 の蒸留で置き場 (`<platform>/api/` に収めるか新カテゴリか) を決める。現時点の記述は maui/ADR-0004 の現行照合 footer と handbook local-development-setup.md が持つ |

### 実装結果と蒸留への申し送り (2026-09-10)

change add-release-workflow の実装で初回リリース `0.1.0-beta.1` まで到達した (release run 34449418361、試行 2 回で完了)。証跡は change の `evidence/` (dispatch-validate-and-dry-run.txt / main-branch-protection.txt / release-pr-and-dispatch.txt / central-portal-upload-probe.txt)。

**所要時間の実測** (macos-26 の同時実行上限 5 の待ちを含む):

| 段 | 所要 |
|---|---|
| dry-run 本番 (validate → 本体検証 5 ∥ package 3 → 消費者 dry-run 4) | 17 分 49 秒 (最長は maui / verify 17 分 31 秒) |
| main 宛て PR の検証 CI 10 job | 17 分 20 秒 |
| publish 試行 1 回目 (Android upload → tag → KMP 発行 → NuGet → Android release まで) | 38 分 (うち Android 枠の PUBLISHED 待ちが上限 30 分で失敗) |
| Central の同期 (release 要求 → repo1 に POM) | Android 枠 約 60 分 / KMP 枠 27 分 29 秒 |
| publish 試行 2 回目 (KMP 再ビルド + upload + 検証 約 6 分 + KMP release 待ち) | 34 分 |
| 反映待ち + smoke 4 本 | 10 分 (最長 smoke-maui 9 分 26 秒) |
| 壁時計の合計 (手動の同期待ち 14 分を含む) | 約 1 時間 54 分 |

**申し送り** (蒸留と後続 change):

| 項目 | 内容 |
|---|---|
| cross/ADR-0024 の accepted 昇格 | design Decision 1 / 2 / 4 のとおり実装。Decision 3 の「package-android も publish と同じ macOS で作る」を 1 文追記 (蒸留時に判断) |
| PUBLISHED 待ちの上限 (要 change) | `central-portal.sh wait-published` の上限 30 分 (翻案元の実測 11 分が根拠) は初回公開の同期に足りず、Android 枠で失敗 → 同じ run の再実行で整合した (spec の Scenario どおり)。KMP 枠は 27 分 29 秒で上限の内側。直し方は「上限と job timeout (120 分) を伸ばす」か「2 枠の release を先に両方要求してから並行で待つ (壁時計が半分)」で、実測値を根拠に探索で決める。main 保護済みのため develop → リリース PR の経路で入る |
| Portal の表示名 | Android 枠も KMP 枠も deployment 名が `jp.kamusoft-<version>` (複数 publication の bundle は artifactId が落ちる) で一覧で見分けられない。handbook release-procedure「失敗したとき」に「枠の区別は Summarize の deployment ID で行う」を 1 文足す |
| 配布構成の concepts 化 | phase-6 / 7 の申し送りと docs-refresh 1 回目の drift 所見のとおり。release workflow の段構成・2 枠 deployment・marker による再実行判定・version 置換の自動化 (README は「最新の公開版」を指す) を含める |
| docs-refresh 2 回目 | README 2 枚と Skill 10 箇所の「まだ公開していない」「`<version>` を置き換える」という散文が、置換 commit 87b7cf7 でコード例が実値になったため矛盾している。「未配信」表記の解除と配布構成の反映 (R5) |
| 自己テストと actionlint の lint job 搭載 | `scripts/release/*.sh --selftest` と `set-readme-version.py --selftest` は数秒・ネットワーク不要。`actionlint` も CI から呼ばれておらず workflow の構文退行を CI で検出できない (fix-release-published-wait の発見)。cross/ADR-0022 の lint job の検査集合を一部改訂 (tasks 2.5 の検討結果、review-001 Suggestion) |
| monorepo tag の別 commit 検出 | `release.yml` の validate と publish の tag 照合だけが自己テストを持たない (verify-001 所見)。スクリプト化するなら上の change に同梱 |
| KsSettingsView への逆流 | R4 (workflow による version 置換) と、翻案で見つけた `check-signatures.sh` の自己テストの無言終了 (`$( ... \|\| true )` の形で `exit` がサブシェルを終える) と `.github/release.yml` のラベル 6 件が未作成な点 (ロードマップ外で起票) |
| 蒸留後の docs-refresh 2 回目と、phase-9 の完了 | roadmap のゴール「tag は publish 全成功後にのみ生まれる」は monorepo の tag について成り立ち、配信リポジトリの tag は KMP 発行の前に生まれる例外 (ADR-0024) として ksn-roadmap で 1 行直す |

## TODO

- [x] 論点の解消 (R1〜R7、2026-09-10 決定)
- [x] docs-refresh 1 回目 (2026-09-10 完了): 旧 Android 座標 16 箇所を新座標へ、MAUI 分 (10.0.20・`KSDLG0001`・`ViewCreationFailed`) と KMP 分 (`api` スコープ・Kotlin 同 minor) を反映。API 版付き TFM 名は利用者向け文書に書かない (SDK 更新で腐るため、オーナー判断)。drift 所見: KMP 消費者の Android ホストへ推移的に届くもの (`ksdialogs-core` は自動、Compose 系は別途) と共有コード側の `api` スコープの根拠、MAUI の下限版 / `KSDLG0001` は concepts に無い (skills だけが持つ状態) → 蒸留の配布構成 concepts 化に含める
- [ ] docs-refresh 2 回目 (初回リリース後の蒸留の後): 「未配信」表記の解除と配布構成 concepts の反映 (R5)
- [ ] MAUI の nupkg 名検査 (package / publish の 2 回) と XML ドキュメントの明示 (facade true / binding false) を release の change に含める (R6)
- [ ] ksn-propose で変更提案を起こす (docs-refresh 1 回目の後。cross/ADR-0024 proposed を design の Decision に反映)
- [ ] KsSettingsView 側へ R4 (release workflow による version 置換) を逆流させる作業を、このロードマップの外で起票する (phase-9 の実装後)
- [x] change add-release-workflow の実装と初回リリース `0.1.0-beta.1` (2026-09-10 完了。詳細は上の「実装結果と蒸留への申し送り」)
- [ ] 蒸留 (ksn-distill): cross/ADR-0024 の accepted、配布構成の concepts 化、lessons の昇格判定
- [x] PUBLISHED 待ちの上限の change: fix-release-published-wait (S 級、2026-09-10 完了。2 枠の公開待ちを 1 本・並行・上限 90 分に、job timeout 150 分。次のリリース PR で main へ)
- [ ] 蒸留後: docs-refresh 2 回目 (R5)
