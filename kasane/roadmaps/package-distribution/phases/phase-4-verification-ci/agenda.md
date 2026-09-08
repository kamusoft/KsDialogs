# 検証 CI (verification-ci)

4 形態のビルド・テスト + lint を GitHub Actions で回す検証 CI を、KsSettingsView の reusable workflow 構成とブランチ役割別トリガーを踏襲して整備する change フェーズ。

## 論点

(2026-09-08 時点で空。論点 1〜4 は決定事項へ移した)

## 決定事項

踏襲 (解決済み論点)。出典は KsSettingsView cross/ADR-0025 (reusable workflow + 入口)・cross/ADR-0026 (CI の保証範囲)・cross/ADR-0028 (トリガーをブランチの役割で分ける) と同 phase-3 / phase-13 の決定事項。

- platform 別 reusable workflow (`workflow_call`) + 入口 workflow 1 本。release も同じ workflow を呼ぶ。変更パスによる job の絞り込みはしない (必須 check の素通り経路になる)。status check 名は「呼び出し側 job 名 / 呼ばれた側 job 名」で両側を固定
- 緑の意味は「ロジック全件通過 + native への配線がコンパイルできる」まで。実行ホスト起動 (E2E) は CI に載せない。iOS はホスト上の `swift test` を成否判定に使わず Simulator で実行。テストが 1 件も実行されなければ失敗
- lint job に gitleaks + local-path-lint + identity-lint + comment-policy-lint (KsDialogs の `scripts/` 既存分) を載せる
- トリガー整理は最初から織り込む: 開発ブランチへの push は lint + 本体検証、リリース対象ブランチ宛て PR はそれに消費者検証 (phase-8) を足す
  - 入口は 1 本のまま job に `if: github.event_name == 'pull_request'`。concurrency の group を `github.ref` にして cancel-in-progress
- JDK は Temurin 17 を `setup-java`、キャッシュは依存のみ、`macos-26` + Xcode 版を `env` で固定 (版は着手時に実測)
- 却下済み: 入口に手順直書き / paths 絞り込み / platform ごとの独立入口 / E2E を CI に載せる / 消費者検証を開発ブランチで毎回 (理由は cross/ADR-0025・0026・0028 の Alternatives)

KsDialogs 側で確定している前提 (phase-3 の結論と申し送り、2026-09-07):
- トリガーはブランチモデル `develop` / `main` (cross/ADR-0016 proposed) に従う。`develop` への push で lint + 本体検証、`main` 宛て PR で消費者検証を足す (cross/ADR-0028 をそのまま逆流、読み替え不要)
- 識別子 lint の検査範囲はソース 5 ルートを含む形に拡張済み (誤検出 0 件)。CI では拡張後の config をそのまま走らせる
- リポジトリは public のため macOS ランナーは無料

### KMP job の形と metadata compile (2026-09-08)

KMP の検証は **macOS ランナー 1 job** (`kmp / verify`) で、`allTests` (androidHostTest + iosSimulatorArm64Test) と階層化 source set の metadata compile (`compileIosMainKotlinMetadata` / `compileCommonMainKotlinMetadata`) を同じ Gradle 起動で回す。iOS ターゲット (Swift パッケージへの `localSwiftPackage` + framework リンク) は KMP にとっての「native への配線」であり、iOS Simulator のテストには ObjC 公開面の検査 (`ObjCApiSurfaceTests`) が含まれ、同じ実行で正の公開 API 形状検査 (`api-surface-check` の commonMain。負の検証はフラグ指定時のみで CI 対象外) もコンパイルされるため、Ubuntu だけでは保証範囲 (ロジック全件 + 配線のコンパイル) を満たせない。metadata compile を消費者検証 (phase-8) に寄せると `main` 宛て PR でしか走らず `develop` の push で KT-88548 型の失敗を見落とすため、ライブラリ側の job に直接載せる。所要時間は着手時に実測し timeout を置く。Kotlin 2.5.0 以降へ上げるときは iosMain の override に `@Throws` を書き戻せる (kmp/ADR-0001 の現行照合 2026-09-05)。撤回時は concept kmp/api/ios-host-integration.md の注意点と KMP Skill en / ja の同節も docs-refresh で追随させる。
- 却下: Ubuntu (Android host) + macOS (iOS) の 2 job (status check が非対称・Gradle 設定が 2 重) / Ubuntu のみ (iOS 配線・ObjC 面・負の検証が落ちる)

### MAUI job の toolchain 固定 (2026-09-08)

MAUI job は **SDK 10.0.300 / workload set 10.0.300.3 (.NET for iOS 26.5.10284 / MAUI SDK 10.0.20) / Xcode 26.5** に揃え、repo 直下に `global.json` を新設して `setup-dotnet` の `global-json-file` から参照する。`Microsoft.Maui.Controls` は KsSettingsView と同じ **10.0.70** に固定する (現状は facade / Tests / ApiSurfaceCheck の 3 csproj に 10.0.1 直書き。一元化の形は KsSettingsView の `Directory.Packages.props` を参考に propose で決める。`samples/maui` は別ビルドルートで `MauiVersion` 直書きのまま)。
- 背景: library-foundation で「Xcode 26.5 でビルド不可」だったのは、親ディレクトリの `../global.json` (workload set 10.0.101) が .NET for iOS 26.1.10502 を含み Xcode 26.1 を要求していたため
- 背景: リポジトリ内に `global.json` が無く、他の clone では SDK 版が環境任せだった
- 理由: CI で緑の実績がある唯一の組で、iOS Native job の Xcode 26.5 と一致し verify-maui.yml を読み替えなしで逆流できる。手元の既定 Xcode 26.5 で `DEVELOPER_DIR` の付け替えが不要になる
- 却下: 10.0.101 のまま Xcode 26.1 固定 (`macos-26` に 26.1 が残る保証がなく、ios / maui で Xcode が 2 本立て) / `global.json` を置かずランナー任せ (再現性なし)

### 実行件数の下限と CI に載せるテストルートの範囲 (2026-09-08)

下限は踏襲し **0 件 = fail、固定数値は持たない**。検査の単位は Android = module × variant、KMP = ターゲット 2 つ (Android host / iOS Simulator) それぞれ、iOS と MAUI = 合計。数値の下限はテスト整理のたびに CI を直すことになるため採らず、単位を細かく切って「一部が丸ごと走らない」を捕まえる。

翻案元に無い KsDialogs 固有のテストルート 3 つの扱い:

| テストルート | 扱い | 理由 |
|---|---|---|
| MAUI の Android 橋渡し (`maui/android/native/`、JVM 31 件) | maui job に載せる | 自動で成否が決まるロジックのテスト (cross/ADR-0026 翻案元の全件実行の対象)。Gradle 1 手順 |
| MAUI の iOS 橋渡し (`maui/macios/native/`、XCTest 7 件) | maui job に載せる | 同上。binding ビルドが同じ xcodeproj を既にビルドしているので `xcodebuild test` の追加だけ |
| Android instrumented (`connectedDebugAndroidTest`、333 件 / 1 台分) | 別 job (`android-instrumented / verify`) で API 36 の Emulator 1 台 | JVM では走らない Scenario 群 (`PB-TR` / `PB-SB` / `PB-WN`) を CI が見る。Emulator 由来の不安定さを別 job に隔離。所要時間は着手時に実測し timeout を置く |

API 29 固有の旧経路 (6 本) は手元の完了条件のまま handbook cross/test-execution.md に残す。
- 却下: instrumented を載せない (333 件が手元頼みになる) / API 29 + 36 の 2 台 (所要時間が倍、API 29 のシステムイメージ選定が要る)

### `develop` の必須 status check は登録しない、`main` の必須 check 名 10 件を phase-9 へ申し送る (2026-09-08)

phase-3 の申し送り「`develop` の必須 status check 登録は phase-4 で行う」は取り下げる。翻案元 cross/ADR-0028 (KsSettingsView、accepted) が却下した案そのもので、却下理由 (1 人開発・直 push・enforce_admins off のため管理者の直 push は必須 check の有無によらず通り、効力の無い設定が見かけと運用の食い違いを生む。enforce_admins を on にすると直 push 自体が拒否され cross/ADR-0016 が成り立たない) が KsDialogs でも同じ前提で有効。申し送りの出どころは KsSettingsView phase-2 の ADR-0028 より前の記述だった。結果は KsSettingsView の現状と同じ形: `develop` は force-push 禁止 + 削除禁止のみで事後検証、`main` は必須 status check + PR 経由必須。

`main` は初回リリースの PR で作るため (cross/ADR-0016)、その branch protection は phase-9 で `main` 作成直後に付ける (REST の branch protection は実在するブランチにしか PUT できない。KsSettingsView も release-workflow のフェーズで付けた)。登録する必須 check 名は次の 10 件 (`{"context": ..., "app_id": 15368}` 形式):

| 種別 | check 名 |
|---|---|
| lint | `lint` |
| 本体検証 5 本 | `ios / verify`、`android / verify`、`android-instrumented / verify`、`kmp / verify`、`maui / verify` |
| 消費者検証 4 本 (phase-8 で job 名確定) | `consumer-ios / verify`、`consumer-android / verify`、`consumer-maui / verify`、`consumer-kmp / verify` (KMP 消費者の形は phase-7 / 8 の結論次第) |

- 却下: 申し送りどおり `develop` に登録 (効力なし) / enforce_admins を on にして `develop` も PR 運用 (ADR-0016 の改訂が要る)

## TODO

- [x] 論点 1: KMP job の形と metadata compile の載せ方 (2026-09-08)
- [x] 論点 2: MAUI の Xcode / workload 固定と `global.json` 新設、MAUI Controls 10.0.70 (2026-09-08)
- [x] 論点 3: 実行件数の下限と CI に載せるテストルートの範囲 (2026-09-08)
- [x] 論点 4: `develop` の必須 status check は登録しない、`main` の必須 check 名を phase-9 へ申し送り (2026-09-08)
- [x] phase-9 の agenda に `main` の branch protection の申し送りを追記 (2026-09-08)
- [ ] ksn-propose で変更提案を起こす
