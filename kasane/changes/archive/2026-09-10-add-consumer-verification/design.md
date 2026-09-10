# Design: add-consumer-verification

## Context

4 形態のパッケージング (SwiftPM 配信リポジトリへのスナップショット同期 / Android と KMP の Maven 発行 / MAUI の NuGet pack) は整い、各フェーズで一時プロジェクトによる消費者検証を手で通した証跡がある。本変更はそれを `verification/` として永続化し、`main` 宛て PR の CI (dry-run) と phase-9 の release workflow (dry-run → publish → smoke) から同じ手段で呼べるようにする。フェーズ議論 ([agenda](../../roadmaps/package-distribution/phases/phase-8-consumer-verification/agenda.md)) で踏襲 7 項目 (検証範囲・参照先・スクリプト構成・workflow・release との接続・落とし穴) と決定 4 件 (KMP 消費者の置き方・Android の 2 モジュール・トリガーと timeout・lint の範囲) を決め、phase-7 の C1 / C2 / C3 が KMP の dry-run の参照形と検証段・release との接続を定めた。本書は翻案元 (KsSettingsView `kasane/changes/archive/2026-09-02-add-consumer-verification/design.md`) の Decision 1〜6 (参照先の排他性・MAUI の空の展開先と取得元検査・platform 別 workflow と artifact 入力・証跡・副作用の不在) を踏襲したうえで、KsDialogs 固有 (KMP 形態と Android の 2 artifact) の設計判断を Decision 形式で残す。挙動の契約はデルタスペック、作業は tasks.md にある。

翻案元から踏襲する Decision は次のとおり (根拠は翻案元 design と agenda 踏襲表。翻案先での確認は tasks 1.x):

| 翻案元 | 内容 | KsDialogs での読み替え |
|---|---|---|
| Decision 1 | dry-run の参照先は本リポジトリ由来の座標について排他的 (`exclusiveContent` / packageSourceMapping / `path:`) | 座標を `jp.kamusoft` (2 group 内 3 artifact) / `KsDialogs.*` (3 パッケージ) / product `KsDialogs` に読み替え |
| Decision 2 | MAUI は実行ごとに空の展開先を使い、取得元をパッケージ単位で検査 | 同じ。検査対象に platform TFM でのアセット実在を足す (Decision 6) |
| Decision 3 | platform 別 workflow、artifact 入力で publish 成果物を受け取る | 4 本に増やす。KMP の artifact の意味は Decision 3 |
| Decision 5 | 証跡は platform ごとに取れるものを取る | KMP を足す (Decision 5) |
| Decision 6 | 副作用の不在は前後比較と権限の不在で示す | 同じ。比較対象に Central の `ksdialogs-kmp` deployment と `KsDialogs.*` を足す |

翻案元 Decision 4 (dry-run の既定 version は platform ごとの開発用既定値) は KMP で成立しないため、Decision 1 で置き換える。

## Goals / Non-Goals

- Goals: 配布物を利用者と同じ経路で解決・Release ビルドできることを、4 形態の消費者プロジェクトと CI で再実行可能にする。dry-run が公開レジストリやユーザー環境のキャッシュへ静かにフォールバックしない。README の最小例 4 つがビルドされ続ける。KMP は発行 metadata の Swift 参照 (cross/ADR-0008) が消費者側で linkage package として再生成され、Swift 登録 API までリンクされることを、dry-run (`file://`) と smoke (https) の両方で確かめる。release workflow が publish 前 / 後に同じ workflow を呼べる
- Non-Goals: proposal.md の Non-Goals に同じ (release からの呼び出しと smoke 正ケース、`main` の必須 check、起動・publish・実機、README の構成変更と `README_ja`、旧座標の追随、推移依存の期待値照合、次 minor の Kotlin)

## Decisions

### Decision 1: dry-run の既定 version は検証用の合成 version 1 つに揃える

**採用案:** version 未指定の dry-run では、`verification/lib/verification-args.sh` が持つ検証用の合成 version `0.0.0-alpha.0` を Android / MAUI / KMP のフィード準備と消費者に流す (iOS は `path:` 参照で version を持たない)。値は本体の注入値の形式検査 (`android/build.gradle.kts` / `kmp/build.gradle.kts` の `releaseVersionPattern` = `X.Y.Z` または `X.Y.Z-{alpha|beta|rc}.N`) に適合し、実在しうるリリース版と衝突しない (0.0.0 はリリースしない)。フィード準備は android/ と kmp/ に `-Pversion=0.0.0-alpha.0`、maui/ に `-p:Version=0.0.0-alpha.0` を注入する。署名鍵の無い環境でのリリース版発行は Sign タスクが skip される (`setRequired(signingInMemoryKey の有無)`、android/ 148 行・kmp/ 90 行) ため、dry-run は未署名のまま通る。
**理由:** KMP の dry-run は SNAPSHOT では成立しない。SNAPSHOT の Swift 参照は `localSwiftPackage(../ios)` で発行 metadata に発行者マシンの絶対パスが載り (concepts kmp/api/ios-host-integration.md)、消費者の linkage package が monorepo 内 `ios/` のソースを指してしまう — 「本体ソースへの参照を持たない」消費者にならず、リリース版で初めて現れる参照の導出 (cross/ADR-0008) を検証できない。phase-7 C1 も dry-run を「tag 付きローカル clone への `file://` + exact」と定めている。version は kmp POM の `ksdialogs-core` 依存版と同じ文字列でなければならず (cross/ADR-0009)、android/ も同じ合成 version で発行する必要がある。Android / MAUI の消費者だけ開発用既定値 (SNAPSHOT / `0.0.0-dev`) を残す理由は無く、揃えることで「version を与えると全 platform に同じ文字列が流れる」経路と「リリース版 × 署名鍵なし」の発行経路 (翻案元では release workflow が実装されるまで CI で踏まれなかった) が `main` 宛て PR の CI で毎回踏まれる。
**代替案:**
- **A: platform ごとの開発用既定値 (翻案元 Decision 4)** — KMP で成立しない (上記)。却下
- **B: KMP だけ合成 version、Android / MAUI は開発用既定値** — 既定値が 3 通りになり、フィード準備の引数解釈が platform で分岐する。Android の「リリース版 × 鍵なし」経路が KMP 経由でしか踏まれない。却下
- **C: 本体の開発用既定値 (カタログの `0.1.0-SNAPSHOT` / `Directory.Build.props` の `0.0.0-dev`) を変える** — 翻案元 Decision 4 の却下理由と同じ (消費者検証の都合で本体の宣言を動かす)。却下

### Decision 2: Maven のフィードは作業ディレクトリ内のローカルリポジトリに隔離する

**採用案:** Android と KMP のフィード準備は、`~/.m2/repository` ではなく作業ディレクトリ内のローカル Maven リポジトリ (`<work>/maven`) へ発行する。発行は既存の `publishToMavenLocal` に `-Dmaven.repo.local=<work>/maven` を与えて行い、消費者の `settings.gradle.kts` は dry-run でそのディレクトリを `maven { url }` として `exclusiveContent` で `jp.kamusoft` に排他割り当てする (smoke は `mavenCentral()`)。`--reference` (CI の `artifact`) で受け取る準備済みの参照先も同じ形のディレクトリで、区別なく扱う。
**理由:** KMP のフィードは android/ (`ksdialogs-core`・`ksdialogs`) と kmp/ (5 publication) の 2 ビルドの発行物で構成され、release の dry-run では android/ 分だけを package 段の artifact から受け取り kmp/ 分を job 内で発行する (Decision 3)。2 つの発行物を 1 つの参照先に集めるには、発行先をディレクトリで指定できる必要がある。`~/.m2` を使うと (1) 前回実行や別 change の残留物が解決に混ざる (翻案元は mavenLocal をキャッシュしない注意書きで避けていた)、(2) artifact と job 内発行の 2 か所を `exclusiveContent` に並べる形になり dry-run の証跡が「どちらから解決したか」を失う。作業ディレクトリ内なら実行ごとに空から始まり、取得元がディレクトリ 1 つに確定する。Gradle の `mavenLocal()` と `publishToMavenLocal` は Maven と同じ規則で位置を決め、システムプロパティ `maven.repo.local` が最優先 (Gradle の mavenLocal の解決規則。翻案先での確認は tasks 1.3)。
**代替案:**
- **A: `~/.m2/repository` をそのまま使う (翻案元)** — 上記 (1)(2)。KMP 単独なら成立するが、artifact 併用時に参照先が 2 つになる。却下
- **B: kmp/ の発行先を Gradle の `maven { url }` publication として新設する** — 本体の発行構成 (vanniktech の publication 群) に消費者検証専用のリポジトリ宣言を足すことになり、本体側の変更が要る。却下

### Decision 3: KMP の dry-run は Swift 参照を tag 付きローカル clone の `file://` で作り、artifact 入力は Android 分だけを置き換える

**採用案:** 準備済みの配布物 (`--reference` / CI の `artifact`) の配置は形態ごとに次のとおりで、release の package 段はこの構造で upload する。

| 形態 | artifact のルート | 必須の内容 |
|---|---|---|
| iOS | 配信リポジトリのスナップショット (ディレクトリ名は `KsDialogs-SPM` に展開する) | `Package.swift` / `Sources/` / `Tests/` / `LICENSE` / `README.md` (同期スクリプトの出力 5 点) |
| Android | ローカル Maven リポジトリのルート (`jp/kamusoft/...` の階層) | `ksdialogs-core` と `ksdialogs` の指定 version の POM・aar・`.module` |
| MAUI | フォルダフィードのルート | `KsDialogs.Maui` / `KsDialogs.Binding.Android` / `KsDialogs.Binding.iOS` の指定 version の `.nupkg` |
| KMP | Android と同じローカル Maven リポジトリのルート | Android の内容と同じ (kmp/ の 5 publication は job 内で同じルートへ発行する) |

KMP のフィード準備 (dry-run) は次の 4 段で行う: (1) iOS 消費者と同じ手順でスナップショットを一時ディレクトリ `KsDialogs-SPM` (git init + origin を配信リポジトリに設定) に `scripts/spm-snapshot/sync-snapshot.sh` で同期し、commit して version と同名の tag を打つ (push はしない)。(2) android/ を `-Pversion=<version>` で Decision 2 のリポジトリへ発行する (`--reference` があればこの段を飛ばし、その内容を参照先にする)。(3) kmp/ を `-Pversion=<version> -Pksdialogs.swiftPackageUrl=file://<clone の絶対パス>` で同じリポジトリへ発行する (`--reference` があっても行う)。(4) 参照先 (Maven リポジトリのパス) と clone の `file://` URL を出力する。消費者ビルドは `VerificationApp` の `Package.swift` をテンプレートから生成し、依存 1 行を dry-run は同じ `file://` URL + `exact(<version>)`、smoke は `https://github.com/kamusoft/KsDialogs-SPM` + `exact(<version>)` にする。
**理由:** phase-7 C1 のとおり、linkage package (発行 metadata から再生成) と `VerificationApp` の依存が同じ URL を指せば SwiftPM が 1 つの pin にまとめる (PoC 項目 4 の実測)。artifact を Android 分に限るのは phase-7 C3 のとおり: package 段の kmp artifact は Swift 参照が既定の https + exact で、その tag は publish 段まで存在しないため dry-run では解決できない。既定 URL でのリリース版の iOS publication の発行自体も配信リポジトリに同版 tag が要る (phase-7 実装結果) ため、tag の無い時点で kmp/ を発行できるのは `file://` 上書き付きだけである。
**代替案:**
- **A: KMP の artifact も package 段のものを使う** — 上記のとおり tag 未存在で解決できない。phase-7 C3 で却下済み
- **B: dry-run で配信リポジトリへ一時 tag を push して https で解決する** — phase-7 A2 / C1 で却下済み (配信リポジトリへの副作用)
- **C: iOS 消費者 (`verification/ios`) も `file://` + exact に揃える** — agenda 踏襲 (dry-run の参照先は `path:`) と異なり、`path:` の identity がディレクトリ名から決まる前提の検証 (翻案元 tasks 1.1) を失う。KMP 側で `file://` を検証するのとは別の経路なので両方残す。却下

### Decision 4: 生成物は smoke 形で追跡し、消費者ビルドは作業コピーで再生成する

**採用案:** `verification/kmp/iosApp/KotlinMultiplatformLinkedPackage/` (発行 metadata から生成される linkage package) と `verification/kmp/iosApp/VerificationApp/Package.swift` は、Swift 参照が既定の https + exact になる形 (smoke 形。`exact` は検証用の合成 version) で追跡する。追跡している 2 つは**構造確認用の非解決 fixture** である — 合成 version の tag は配信リポジトリに公開されないため、そのままでは SwiftPM の解決は通らず、Xcode project を開いて参照構造を確認できるだけに留まる。消費者ビルドの前の再生成 (2 段目) は必須であり、追跡物を直接ビルドしない。追跡物は dry-run で生成された linkage package の Swift 参照 URL を既定の https に書き換えて作る (既定 URL でのリリース版発行は配信リポジトリに同版の tag が要るため、https 形は消費者側では生成できない — phase-7 実装結果)。Xcode project は実装時に `XCODEPROJ_PATH` 付きの `integrateLinkagePackage` を 1 回実行して両者を相対パスで参照する状態にし、その状態を追跡する。消費者ビルドは `verification/kmp/` を作業ディレクトリ (`--work`) へコピーしてから、その中で (2) `linkReleaseFrameworkIosSimulatorArm64` による linkage package の再生成とテンプレートからの `Package.swift` 生成を行い、(3) `xcodebuild` を走らせる。追跡している `verification/kmp/` は実行で変化しない。
**理由:** concepts `kmp/api/ios-host-integration.md` の禁止事項「`KotlinMultiplatformLinkedPackage/` を生成物として無視しない (Xcode project が参照する統合物で clone 後にも必要)」に従い、Sample と同じく追跡する。一方で linkage package の `Package.swift` には mode で変わる Swift 参照が書かれ、dry-run では `file://` の絶対パスになる。追跡ファイルの中で再生成するとローカル絶対パス lint に掛かる状態が作業ツリーに残り、うっかり commit すれば違反になる。作業コピーで再生成すれば追跡分は smoke 形のまま変わらず、clone 直後に利用者と同じ構造で Xcode project を開ける (解決はできない)。翻案元の iOS 消費者も `Sources/` を作業ディレクトリへコピーして建てており同型である。
**代替案:**
- **A: 生成物を追跡せず `.gitignore` に置く** — concepts の禁止事項に反し、消費者検証だけの例外を concepts に書くことになる。clone 直後は Gradle を回すまで Xcode project が開けない。却下 (オーナー判断 2026-09-09)
- **B: 追跡したまま `verification/kmp/` の中でそのまま再生成する** — dry-run 後に作業ツリーが `file://` の絶対パスで汚れ、commit しなければ lint は通るが事故の余地が残る。却下
- **C: Xcode project も毎回生成する (`integrateLinkagePackage` を消費者ビルドの中で実行)** — pbxproj を書き換える統合手順を毎回走らせることになり、冪等性の検証と Xcode project のテンプレート化の手間が大きい。却下

### Decision 5: KMP の検証 3 段と証跡

**採用案:** 消費者ビルドは (1) `:androidApp:assembleRelease`、(2) `:shared:linkReleaseFrameworkIosSimulatorArm64`、(3) `xcodebuild -project ... -scheme VerificationKmp -configuration Release -destination 'generic/platform=iOS Simulator' CODE_SIGNING_ALLOWED=NO build` を順に通し、いずれかの失敗を全体の失敗とする。証跡は (a) `:androidApp:dependencies --configuration releaseRuntimeClasspath` の `jp.kamusoft` 行 (`ksdialogs-kmp` と推移の `ksdialogs-core` が同版)、(b) `:shared` の iOS 3 ターゲット (iosArm64 / iosSimulatorArm64 / iosX64) の compile 用 configuration の依存解決に現れる `jp.kamusoft:ksdialogs-kmp` の iOS publication 3 件 (root と android と合わせて 5 publication)、(c) 参照先の Maven リポジトリに置かれた kmp artifact の SwiftPM 連携 metadata (`swiftpm-metadata.json`) の Swift 参照 (URL が mode どおり、`exact` が version と同じ、deployment target `17.0`)、(d) 再生成された linkage package の `Package.swift` と生成した `VerificationApp/Package.swift` の依存 URL が同一であること、(e) `xcodebuild` の解決ログに `KsDialogs-SPM` の pin が 1 つだけ現れること。
**理由:** phase-7 C2 のとおり主眼は (2)(3) で、A2 の参照導出と A4 の Kotlin 範囲宣言 (消費者の KGP はカタログの `kotlin` と同じ 2.4.10) はこの経路でしか検証できない。phase-7 の申し送り「全 publication の POM / `.module` / klib / aar が URL に依存しないことは root の突き合わせまで」を消費者側から埋めるには、(b) で 5 publication が実解決されたことを残す必要がある。(e) は C1 の「1 つの pin にデデュープされる」の消費者側での確認。
**代替案:**
- **A: (2) の後 `xcodebuild` を回さない** — phase-7 C2 で却下済み (linkage package が生成されるだけでリンクされない)
- **B: 3 ターゲットすべてを link する** — framework のリンクは simulatorArm64 1 つで主眼を満たし、実機向け (iosArm64) のリンクは署名を要しない static framework でも所要時間が倍になる。解決の証跡 (b) は link せずに取れる。却下

### Decision 6: Android の 2 モジュールと MAUI の検査項目

**採用案:** `verification/android/` は Compose 系 `jp.kamusoft:ksdialogs` 1 行の app と `jp.kamusoft:ksdialogs-core` 1 行の app の 2 モジュール (agenda 決定 2)。検査は `releaseRuntimeClasspath` の依存ツリーから、Compose 側で `ksdialogs-core` が `ksdialogs` と同版で推移解決されたこと、`-core` 側に `androidx.compose` の座標が 1 つも無いことを見る。MAUI は `check-dependencies.py` で `project.assets.json` の解決版 (binding 2 件 = facade) と `.nupkg.metadata` の取得元に加え、platform TFM (`net10.0-android` / `net10.0-ios`) の target に binding のアセンブリが platform 固有アセットとして含まれること (platform 中立アセットへのフォールバックでないこと) を検査する。
**理由:** Android の 2 モジュールの理由は agenda 決定 2 (2 artifact を維持する KsDialogs では利用者の書き方が 2 通りで、`-core` 単独の解決と Compose 非混入 (cross/ADR-0019 の却下案の裏返し) を直接確かめる)。MAUI のアセット実在は phase-6 の申し送り: API 版付き TFM を下回る `TargetPlatformVersion` を固定した消費者では警告なく platform 中立アセットにフォールバックし binding が入らないため、版の一致だけでは binding が効いていることを示せない。
**代替案:**
- **A: Android は Compose 系 1 行の app だけ (翻案元)** — agenda 論点 2 で却下済み
- **B: MAUI の検査は版の一致だけ (翻案元)** — phase-6 の申し送りのフォールバックを見逃す。却下

## Risks / Trade-offs

- Decision 2 の `-Dmaven.repo.local` による発行先の切り替えと、Decision 3 の `file://` URL からの linkage package 再生成は、phase-7 の発行検証 (`file://` 上書きでの `publishToMavenLocal`) と PoC (bare clone の `file://`) で個別に実証済みだが、消費者側からの実解決は本変更が初めて。tasks 1.x のスパイクで先に実測し、覆ったら Decision 2 / 3 の見直しをオーナーへ上げる
- 合成 version `0.0.0-alpha.0` (Decision 1) は作業ディレクトリ内のリポジトリにしか発行されない (Decision 2) ため `~/.m2` を汚さないが、KMP のスナップショット clone に同名の tag が打たれる。clone は作業ディレクトリ内で push しない
- KMP の消費者 job は macOS 1 job でフィード準備 (android/ と kmp/ の発行、iOS 3 ターゲットの cinterop を含む) から `xcodebuild` までを通すため、所要時間は消費者 4 job で最長になりうる (見込み 12〜18 分、timeout 30 分)。実測で詰める
- 消費者の `verification/android` と `verification/kmp` は本体のバージョンカタログを共有するため、本体の AGP / Kotlin を上げると消費者も同時に上がる。消費者の KGP が確認済み版 (A4) と一致することはこの共有が保証する
- README の最小例 4 つは現行 API と対応済みだが、実装中に壊れていると分かった場合の修正は docs-refresh 経由 (proposal Impact)

## Migration Plan

新規ディレクトリと workflow の追加で、既存のコード・Sample・CI job には影響しない。`ci.yml` の job 追加は `main` 宛て pull_request でだけ効き、`develop` への push では job が起動しない。`main` の必須 status check への登録は phase-9 で本体 5 job と併せて行う。

## Open Questions

- なし (release からの呼び出し・smoke 正ケース・`main` の保護・README のホスト側の例は phase-9 に申し送り済み)

## ADR 候補

- **lint job の検査を 7 から 8 へ (README 最小例と消費者ソースの一致検査の追加)**: cross/ADR-0021 (accepted) は lint job を 7 検査に固定し「検査を無断で足すことはできない」と Context に書く。8 検査目の追加は ADR-0021 の一部改訂 (amends) であり、cross/ADR-0022 をドラフト (proposed) として起票済み (オーナー判断 2026-09-09)。実装が merge されたら蒸留で accepted に昇格し、ADR-0021 に `amended-by` を書く
- **既存 ADR 間の衝突 (本変更の決定ではないが蒸留への申し送り)**: cross/ADR-0018 の MAUI 本体の行 (10.0.70) は、後発の maui/ADR-0004 (workload set 同梱版、10.0.70 を却下) と `maui/Directory.Packages.props` (10.0.20) に対して古い。本変更のスペック (ツールチェーンの再現性) は現行のコードと maui/ADR-0004 に合わせて書き、ADR-0018 の当該行の改訂 (amends) は蒸留時に起票する
- 上記以外の Decision 1〜6 はいずれも `verification/` と CI 定義に閉じた可逆な判断で、選別 3 基準に該当しない。配布チャネル・lockstep・CI の構成原則は既存の cross/ADR-0008 / 0009 / 0017 / 0018 / 0019 が担う。蒸留時は `verification/` の役割と KMP の dry-run の組み立て (合成 version・`file://` の tag 付き clone・生成物を smoke 形で追跡し作業コピーで再生成する理由) を concepts (cross の配布構成の記述、phase-9 の申し送りにある配布構成 concepts 化と同じ置き場) に記述する
