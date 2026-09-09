# 消費者検証 (consumer-verification)

配布物を参照する消費者プロジェクト (`verification/`、4 形態) と、それを dry-run / smoke の 2 モードで回す reusable workflow を KsSettingsView から踏襲して整備する change フェーズ。KMP の 4 本目は phase-7 の結論に従って実装する。

## 論点

- KMP 消費者 (phase-7 の結論の実装): Android app + iOS の linkage package の 2 面を 1 つの `verification/kmp/` に置くか、iOS 面だけ別に切るか。macOS ランナーの所要時間
- Android 消費者は Compose 系 `jp.kamusoft:ksdialogs` 1 行だけを参照して本体 `ksdialogs-core` が推移的に届くことを検証するか、View 系本体だけの消費者も別に置くか (座標は cross/ADR-0019。phase-5 の Sample は Compose 系 1 行 + 推移で本体を解決する形)
- MAUI 消費者の所要時間 (KsSettingsView は約 20 分、見込みを大幅超過) を踏まえた timeout と、消費者検証をどのトリガーに載せるか (phase-4 の結論)
- README の最小コード例 (4 形態 + KMP の iOS 側) を消費者プロジェクトに逐語一致で同梱し lint で検査する範囲

### phase-4 からの申し送り (2026-09-08)

入口 `.github/workflows/ci.yml` に消費者検証 job の枠は無い (存在しない workflow を `uses:` できないため)。`verify-consumer-<platform>.yml` を作るときに入口へ job を足し、`if: github.event_name == 'pull_request'` で `main` 宛て PR に限る。job 名は phase-9 の必須 check 名 (`consumer-<platform> / verify`) に合わせる。reusable workflow の書き方 (Xcode 選択・`global.json` 参照・件数検査・SHA 固定) は本体検証 5 本 (cross/ADR-0017 / 0018) を写す。

### phase-6 からの申し送り (2026-09-08)

MAUI 消費者 (`verification/maui`) は phase-6 の一時消費者 (`kasane/changes/archive/2026-09-08-add-maui-nuget-distribution/evidence/consumer-verification/README.md`) の形を写す。`dotnet new maui` の生成物に足すのは `TargetFrameworks` / `RestorePackagesPath` (隔離) / `WarningsAsErrors` (NU1605・NU1608・NU1107) / `SupportedOSPlatformVersion` (Android 24 / iOS 17.0) / facade の PackageReference 1 行だけで、`Microsoft.Maui.Controls` の版は書かない。`nuget.config` は `<clear/>` + ローカルフィード + nuget.org の併記 (ローカルだけだとテンプレート依存が NU1101)。3 パッケージがローカルフィードから取得されたことは `pkgs/<id>/<ver>/.nupkg.metadata` の `source` で確かめる。Android Release で R8 を効かせるには消費者側で `AndroidLinkTool=r8` を明示する (SDK 既定は空)。

確認事項: API 版付き TFM (`net10.0-android36.0` / `net10.0-ios26.0`) を下回る `TargetPlatformVersion` を固定した消費者では、警告なく platform 中立アセットにフォールバックし binding が入らない (既知の落とし穴)。`check-dependencies.py` で binding が facade と同版で解決されたことに加え、platform TFM で binding が実際に入ったことを検査対象にする。消費者ビルドで `XA4301` が 0 件であることも再確認する (phase-6 では自 assembly 用 aar が生成されず 0 件)。

### phase-7 からの申し送り (2026-09-09)

KMP の発行設定は実装済み (`kasane/changes/archive/2026-09-09-add-kmp-maven-distribution/`)。dry-run が前提にする機構は次のとおり確認済み: `-Pversion=` の注入で Swift 参照が `https://github.com/kamusoft/KsDialogs-SPM` + exact(version) のリモート参照になる / `-Pksdialogs.swiftPackageUrl=file:///<tag 付きローカル clone>` で `file://` URL + exact のリモート参照になる (C1 の前提、KGP 2.4.10 で実測) / `swiftpm-metadata.json` の deployment target は参照種別によらず `17.0`。

確認事項: 既定 URL でのリリース版発行は root publication 単独でしか実証していない (iOS publication の発行は配信リポジトリに同版 tag が要るため)。全 publication (root / android / iOS 3 ターゲット) の POM・`.module`・klib・aar が Swift 参照の URL に依存しないことは root の突き合わせまでの実証なので、dry-run (`file://`) と smoke (https) の両方で全 publication を実解決し、Android app の `assembleRelease` と iOS の framework リンクまで通す (C2)。消費者の KGP 版で走る段 (C2 (2)) は動作確認済み Kotlin 版 (カタログ値、現在 2.4.10) の実証を兼ねる。

## 決定事項

踏襲 (解決済み論点)。出典は KsSettingsView phase-7 の決定事項と実測 (`../KsSettingsView/kasane/roadmaps/package-distribution/phases/phase-7-consumer-verification/agenda.md`)。

- 検証範囲は「解決 + Release ビルド」まで (起動・`dotnet publish`・実機は含めない)。iOS = SwiftPM パッケージ (pbxproj を持たず `DEVELOPMENT_TEAM` 事故が構造的に起きない)、Android = `com.android.application` の app (manifest merger / R8 / dex が app でしか走らない)、MAUI = `dotnet new maui` 相当
- dry-run の参照先: iOS = スナップショットの `path:` (一時ディレクトリ名は実レジストリと同じ `KsDialogs-SPM` — ディレクトリ名から package identity が決まる) / Android = `mavenLocal()` を `exclusiveContent` で / MAUI = ローカルフォルダフィード + packageSourceMapping (nuget.org 併記、実行ごとに空の packages path)
- 1 構成 + 2 引数 (mode と version): 各 platform に `prepare-feed.sh` (フィード準備、最終行に参照先を出力) と `build-consumer.sh` (消費者ビルド、`--reference` で受け取る) の 2 段、共通引数解釈は `verification/lib/verification-args.sh`。MAUI は `check-dependencies.py` で binding が facade と同版で解決されたかを検査
- workflow は `verify-consumer-<platform>.yml` (`workflow_call`、入力 `mode` / `version` / `artifact`)。`permissions: contents: read` のみで secrets を受け取らない。入力検査は checkout より前
- release の dry-run 段では package 段の artifact を渡し「dry-run が見たものと外に出るものが一致する」形にする。smoke は公開レジストリを参照し version 必須、反映待ち job を挟む
- 落とし穴 (踏襲時に避ける): Gradle の `content { includeGroup }` は排他でない → `exclusiveContent` / NuGet の mapping は global packages folder 既存分に効かない / `<clear/>` + ローカルフィードのみだと MAUI テンプレート依存が NU1101 / Xcode パスがシンボリックリンクだと trimming 後の `install_name_tool` が失敗 (`DEVELOPER_DIR` を `pwd -P` で実体解決) / API 版付き TFM を下回る消費者では警告なく platform 中立アセットにフォールバックし binding が入らない。MAUI 消費者は NU1605 / NU1608 / NU1107 を `WarningsAsErrors`
- 却下済み: 起動・実機まで検証する / 消費者検証を開発ブランチで毎回 (cross/ADR-0028)

## TODO

- [ ] 論点の解消 (KMP 消費者の置き方・Android の compose 参照・timeout・最小例の範囲)
- [ ] ksn-propose で変更提案を起こす
